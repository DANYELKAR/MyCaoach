import SwiftUI
import PhotosUI
import AVFoundation

struct GenerationView: View {
    @Environment(AppState.self) private var state
    @State private var model: GenerationViewModel
    @State private var selectedPhotos: [PhotosPickerItem] = []
    @State private var loadingImage = false
    @State private var showCamera = false
    @State private var inputMode = "Photos"
    @State private var contacts: [ContactDTO] = []
    init(reply: Bool = false, contactID: UUID? = nil) { _model = State(initialValue: GenerationViewModel(reply: reply, contactID: contactID)) }
    var body: some View {
        if let runtime = state.runtime {
            @Bindable var model = model
            ScrollViewReader { proxy in
            ScrollView {
                VStack(alignment: .leading, spacing: 24) {
                    if let result = model.result {
                        if model.reply {
                            DisclosureGroup("Conversation context") {
                                if model.text.isEmpty { Text("\(model.images.count) selected screenshots").font(.footnote) }
                                else { Text(model.text).font(.footnote).textSelection(.enabled) }
                            }.surfaceCard()
                        }
                        ResultsView(result: result)
                        SecondaryButton(title: "Start another") { model.startAgain() }
                    } else {
                        Text(model.reply ? "Keep the conversation going." : "Start with something real.").font(.title.bold())
                        if model.reply { Picker("Conversation input", selection: $inputMode) { Text("Screenshots").tag("Photos"); Text("Paste text").tag("Text") }.pickerStyle(.segmented).onChange(of: inputMode) { _, mode in if mode == "Text" { model.images = [] } else { model.text = "" } } }
                        if inputMode == "Text" {
                            TextField("Paste the conversation", text: $model.text, axis: .vertical).lineLimit(5...12).accessibilityIdentifier("generation.text")
                        } else {
                            PhotosPicker(selection: $selectedPhotos, maxSelectionCount: model.reply ? 3 : 1, matching: .images) {
                                Label(model.images.isEmpty ? (model.reply ? "Choose up to 3 screenshots" : "Choose a photo") : "Change photos", systemImage: "photo.on.rectangle").frame(maxWidth: .infinity, minHeight: 100).padding(20).background(Theme.surface, in: RoundedRectangle(cornerRadius: 20))
                            }.disabled(model.isLoading || loadingImage).accessibilityIdentifier("generation.photos")
                            if loadingImage { ProgressView("Preparing photos…") }
                            ForEach(Array(model.images.enumerated()), id: \.offset) { index, bytes in
                                if let image = UIImage(data: bytes) { Image(uiImage: image).resizable().scaledToFit().frame(maxHeight: 240).clipShape(RoundedRectangle(cornerRadius: 16)).accessibilityLabel("Selected photo \(index + 1)") }
                            }
                            if !model.reply && UIImagePickerController.isSourceTypeAvailable(.camera) {
                                Button("Take a photo", systemImage: "camera") { Task { if await AVCaptureDevice.requestAccess(for: .video) { showCamera = true } else { runtime.errorMessage = "Camera access is off. Enable it in Settings or choose a photo." } } }
                            }
                            #if DEBUG
                            if ProcessInfo.processInfo.arguments.contains("-e2e") { Button("Use test photo") { Task { let renderer = UIGraphicsImageRenderer(size: CGSize(width: 100, height: 100)); let data = renderer.jpegData(withCompressionQuality: 0.8) { c in UIColor.green.setFill(); c.fill(CGRect(x: 0, y: 0, width: 100, height: 100)) }; model.images = [data] } }.accessibilityIdentifier("generation.fixture") }
                            #endif
                        }
                        Picker("Tone", selection: $model.toneID) {
                            Text("Choose a tone").tag(nil as UUID?)
                            ForEach(runtime.tones) { tone in Text("\(tone.emoji ?? "") \(tone.name)").tag(Optional(tone.id)) }
                        }.accessibilityIdentifier("generation.tone")
                        Picker("Person (optional)", selection: $model.contactID) {
                            Text("No person").tag(nil as UUID?)
                            ForEach(contacts) { Text($0.name).tag(Optional($0.id)) }
                        }
                        TextField("Add a detail or personal note (optional)", text: $model.note, axis: .vertical).lineLimit(3...6)
                        Text("Selected content is sent to our backend and Google Gemini. Keep it respectful and don't share sensitive information.").font(.footnote).foregroundStyle(Theme.muted)
                        if runtime.auth.isLocal { Label("Test AI · local development", systemImage: "hammer").font(.footnote).foregroundStyle(Theme.warning) }
                        if let error = model.error { Text(error).foregroundStyle(Theme.destructive) }
                        PrimaryButton(title: model.isLoading ? "Finding the right words…" : "Generate suggestions", symbol: "sparkles", isLoading: model.isLoading) { model.generate(runtime: runtime) }.disabled(!model.canGenerate || loadingImage).accessibilityIdentifier("generation.submit")
                    }
                }.padding(20).foregroundStyle(Theme.label)
            }.onChange(of: model.result?.generation.id) { _, id in if id != nil { proxy.scrollTo("generation.top", anchor: .top) } }.background(Theme.background).navigationTitle(model.reply ? "Continue Chat" : "New opener").navigationBarTitleDisplayMode(.inline).toolbar(.visible, for: .navigationBar)
                .task {
                    model.toneID = runtime.profile?.defaultToneId ?? runtime.tones.first(where: { $0.name.lowercased() == state.defaultTone.rawValue })?.id ?? runtime.tones.first?.id
                    do { contacts = try await runtime.api.request("v1/contacts") } catch { runtime.handle(error) }
                }
                .onChange(of: selectedPhotos) { _, items in
                    loadingImage = true
                    Task {
                        do { var images: [Data] = []; for item in items { guard let data = try await item.loadTransferable(type: Data.self) else { throw ImageProcessor.ImageError.invalid }; images.append(try await ImageProcessor.jpeg(data)) }; model.images = images }
                        catch { runtime.handle(error) }; loadingImage = false
                    }
                }
                .onDisappear { model.cancel() }
                .sheet(isPresented: $showCamera) { CameraPicker { data in Task { do { model.images = [try await ImageProcessor.jpeg(data)] } catch { runtime.handle(error) } } } }
            }
        }
    }
}

struct CameraPicker: UIViewControllerRepresentable {
    @Environment(\.dismiss) private var dismiss
    let picked: (Data) -> Void
    func makeCoordinator() -> Coordinator { Coordinator(parent: self) }
    func makeUIViewController(context: Context) -> UIImagePickerController {
        let controller = UIImagePickerController(); controller.sourceType = .camera; controller.delegate = context.coordinator; return controller
    }
    func updateUIViewController(_ uiViewController: UIImagePickerController, context: Context) {}
    final class Coordinator: NSObject, UIImagePickerControllerDelegate, UINavigationControllerDelegate {
        let parent: CameraPicker
        init(parent: CameraPicker) { self.parent = parent }
        func imagePickerControllerDidCancel(_ picker: UIImagePickerController) { parent.dismiss() }
        func imagePickerController(_ picker: UIImagePickerController, didFinishPickingMediaWithInfo info: [UIImagePickerController.InfoKey: Any]) {
            if let image = info[.originalImage] as? UIImage, let data = image.jpegData(compressionQuality: 1) { parent.picked(data) }; parent.dismiss()
        }
    }
}

struct ResultsView: View {
    let result: GenerationResult
    var body: some View {
        VStack(alignment: .leading, spacing: 20) {
            Text("Make it yours.").font(.title.bold()).id("generation.top")
            Text("Review before sending. A suggestion is a starting point.").foregroundStyle(Theme.muted)
            ForEach(result.suggestions) { SuggestionCard(suggestion: $0) }
        }
    }
}
