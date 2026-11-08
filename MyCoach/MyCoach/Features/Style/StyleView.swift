import SwiftUI

struct StyleView: View {
    @Environment(AppState.self) private var state
    @State private var samples = Array(repeating: "", count: 5)
    @State private var style: StyleDTO?
    @State private var busy = false
    @State private var deleting = false
    @State private var error: String?
    @State private var operation: Task<Void, Never>?
    var body: some View {
        Form {
            Section {
                Text("Sound more like yourself.").font(.title2.bold())
                Text("Add 5–10 messages you've written. Each should be 2–500 characters. Avoid sensitive information. Samples are sent to Google Gemini for analysis.").foregroundStyle(Theme.muted)
            }
            Section("Your examples") {
                ForEach(samples.indices, id: \.self) { i in TextField("Message \(i + 1)", text: $samples[i], axis: .vertical).lineLimit(2...5).accessibilityIdentifier("style.sample.\(i)") }
                if samples.count < 10 { Button("Add another example") { samples.append("") } }
                if samples.count > 5 { Button("Remove last example", role: .destructive) { samples.removeLast() } }
                Button("Save and analyze") { run { guard let api = state.runtime?.api else { return }; let _: StyleDTO = try await api.request("v1/style-profile", method: "PUT", body: ["sampleMessages": .a(samples.map(JSONValue.s))]); style = try await api.request("v1/style-profile/analyze", method: "POST") } }.disabled(!valid || busy).accessibilityIdentifier("style.analyze")
            }
            if let style {
                Section("Your style") {
                    if let summary = style.summary { ForEach(summary.keys.sorted(), id: \.self) { key in VStack(alignment: .leading, spacing: 6) { Text(key.replacingOccurrences(of: "_", with: " ").capitalized).font(.headline); Text(summary[key]?.display ?? "").accessibilityIdentifier("style.summary.\(key)") } } }
                    else { Text("Analyze your examples to create your style summary.") }
                    Toggle("Use my style in suggestions", isOn: Binding(get: { self.style?.enabled == true }, set: { enabled in run { if let api = state.runtime?.api { self.style = try await api.request("v1/style-profile", method: "PUT", body: ["enabled": .b(enabled)]) } } })).disabled(busy)
                    Button("Delete my style", role: .destructive) { deleting = true }
                }
            }
            if busy { ProgressView("Working on your style…") }
            if let error { Text(error).foregroundStyle(Theme.destructive) }
        }.featureListStyle().navigationTitle("My Style")
            .task { await load() }.onDisappear { operation?.cancel() }
            .confirmationDialog("Delete your style and all saved sample messages?", isPresented: $deleting, titleVisibility: .visible) { Button("Delete my style", role: .destructive) { run { try await state.runtime?.api.delete("v1/style-profile"); style = nil; samples = Array(repeating: "", count: 5) } } }
    }
    private var valid: Bool { (5...10).contains(samples.count) && samples.allSatisfy { (2...500).contains($0.trimmingCharacters(in: .whitespacesAndNewlines).utf16.count) } }
    private func load() async { do { if let api = state.runtime?.api { let result: StyleDTO = try await api.request("v1/style-profile"); style = result; if !result.sampleMessages.isEmpty { samples = result.sampleMessages } } } catch { if !Task.isCancelled { self.error = error.localizedDescription } } }
    private func run(_ action: @escaping @MainActor () async throws -> Void) {
        guard !busy else { return }; UIApplication.shared.sendAction(#selector(UIResponder.resignFirstResponder), to: nil, from: nil, for: nil); busy = true; error = nil
        operation = Task { defer { busy = false }; do { try await action() } catch { if !Task.isCancelled { self.error = error.localizedDescription } } }
    }
}
