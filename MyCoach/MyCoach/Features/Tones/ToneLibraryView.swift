import SwiftUI

struct ToneLibraryView: View {
    @Environment(AppState.self) private var state
    @State private var editing: ToneDTO?
    @State private var adding = false
    var body: some View {
        List {
            if let runtime = state.runtime {
                ForEach(runtime.tones) { tone in
                    VStack(alignment: .leading, spacing: 12) {
                        HStack { Text("\(tone.emoji ?? "") \(tone.name)").font(.headline); Spacer(); if !tone.builtIn { PlusBadge() } }
                        if let description = tone.description { Text(description).foregroundStyle(Theme.muted) }
                        Button(runtime.profile?.defaultToneId == tone.id ? "Default tone ✓" : "Set as default") {
                            Task { do { try await runtime.updateProfile(["defaultToneId": .s(tone.id.uuidString)]); if let builtIn = BuiltInTone(rawValue: tone.name.lowercased()) { state.defaultTone = builtIn } } catch { runtime.handle(error) } }
                        }.disabled(!tone.builtIn && runtime.profile?.isPlus != true)
                        if !tone.builtIn { Button("Edit") { runtime.requirePlus { editing = tone } } }
                    }.padding(.vertical, 8).swipeActions { if !tone.builtIn { Button("Delete", role: .destructive) { Task { do { try await runtime.api.delete("v1/tones/\(tone.id)"); try await runtime.reload() } catch { runtime.handle(error) } } } } }
                }
            }
        }.featureListStyle().navigationTitle("Tones")
            .toolbar { Button("Create tone", systemImage: "plus") { state.runtime?.requirePlus { adding = true } }.accessibilityIdentifier("tones.add") }
            .sheet(isPresented: $adding, onDismiss: reload) { ToneEditor() }
            .sheet(item: $editing, onDismiss: reload) { ToneEditor(tone: $0) }
            .refreshable { try? await state.runtime?.reload() }
    }
    private func reload() { Task { do { try await state.runtime?.reload() } catch { state.runtime?.handle(error) } } }
}

struct ToneEditor: View {
    @Environment(AppState.self) private var state
    let tone: ToneDTO?
    @State private var name: String
    @State private var emoji: String
    @State private var description: String
    @State private var instruction = ""
    @State private var length: Double
    @State private var boldness: Double
    @State private var humor: Double
    init(tone: ToneDTO? = nil) {
        self.tone = tone; _name = State(initialValue: tone?.name ?? ""); _emoji = State(initialValue: tone?.emoji ?? "✨"); _description = State(initialValue: tone?.description ?? "")
        _length = State(initialValue: Double(tone?.lengthLevel ?? 50)); _boldness = State(initialValue: Double(tone?.boldnessLevel ?? 50)); _humor = State(initialValue: Double(tone?.humorLevel ?? 50))
    }
    var body: some View {
        EditorShell(title: tone == nil ? "Create a tone" : "Edit tone", valid: valid, save: save) {
            Section("Your tone") {
                TextField("Name", text: $name).accessibilityIdentifier("toneEditor.name")
                TextField("Emoji", text: $emoji)
                TextField("Description", text: $description, axis: .vertical)
                TextField(tone == nil ? "Style instruction" : "New instruction (leave blank to keep)", text: $instruction, axis: .vertical).lineLimit(3...6).accessibilityIdentifier("toneEditor.instruction")
            }
            Section("Fine tune") { slider("Length", value: $length); slider("Boldness · PG-13", value: $boldness); slider("Humor", value: $humor) }
            Section("Illustrative preview") {
                Text(humor > 60 ? "That coffee looks like it has a better weekend planned than I do. What's your go-to order?" : length < 40 ? "That café looks lovely. What's your go-to order?" : "That café looks like a great place to spend a quiet afternoon. What's your go-to order?")
                Text("This preview is an example. Your instruction and sliders are applied when you generate a message.").font(.footnote).foregroundStyle(Theme.muted)
            }
        }
    }
    private var valid: Bool { !name.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty && name.utf16.count <= 40 && emoji.utf16.count <= 20 && description.utf16.count <= 300 && instruction.utf16.count <= 1000 && (tone != nil || !instruction.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty) }
    private func slider(_ title: String, value: Binding<Double>) -> some View { VStack(alignment: .leading) { Text("\(title): \(Int(value.wrappedValue))%"); Slider(value: value, in: 0...100, step: 1).accessibilityLabel(title) } }
    private func save() async throws {
        guard let api = state.runtime?.api else { return }
        var body: [String: JSONValue] = ["name": .s(name), "emoji": .s(emoji), "description": .s(description), "lengthLevel": .n(length), "boldnessLevel": .n(boldness), "humorLevel": .n(humor)]
        if !instruction.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty { body["styleInstruction"] = .s(instruction) }
        let _: ToneDTO = try await api.request("v1/tones" + (tone.map { "/\($0.id)" } ?? ""), method: tone == nil ? "POST" : "PATCH", body: body)
    }
}
