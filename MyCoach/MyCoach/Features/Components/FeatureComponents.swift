import SwiftUI

struct CollectionFooter<Item: Decodable & Identifiable & Sendable>: View where Item.ID: Sendable {
    let collection: RemoteCollection<Item>
    let load: () async -> Void
    var body: some View {
        if collection.isLoading { ProgressView("Loading…") }
        if let error = collection.error { Text(error).foregroundStyle(Theme.destructive); Button("Try again") { Task { await load() } } }
        else if collection.hasMore { Button("Load more") { Task { await load() } }.frame(minHeight: 44) }
    }
}
struct EditorShell<Content: View>: View {
    @Environment(\.dismiss) private var dismiss
    let title: String
    let valid: Bool
    let save: () async throws -> Void
    @ViewBuilder let content: () -> Content
    @State private var busy = false
    @State private var error: String?
    var body: some View {
        NavigationStack {
            Form {
                content()
                if let error { Text(error).foregroundStyle(Theme.destructive) }
            }.scrollContentBackground(.hidden).background(Theme.background).navigationTitle(title).navigationBarTitleDisplayMode(.inline)
                .toolbar {
                    ToolbarItem(placement: .cancellationAction) { Button("Cancel") { dismiss() }.disabled(busy) }
                    ToolbarItem(placement: .confirmationAction) {
                        if busy { ProgressView() }
                        else { Button("Save") {
                            UIApplication.shared.sendAction(#selector(UIResponder.resignFirstResponder), to: nil, from: nil, for: nil)
                            busy = true
                            Task { do { try await save(); dismiss() } catch { self.error = error.localizedDescription }; busy = false }
                        }.disabled(!valid).accessibilityIdentifier("editor.save") }
                    }
                }.interactiveDismissDisabled(busy)
        }.tint(Theme.primary)
    }
}
struct PlanNotice: View {
    let text: String
    var body: some View { Label(text, systemImage: "info.circle").font(.footnote).foregroundStyle(Theme.muted).padding(.vertical, 8) }
}
extension View {
    func featureListStyle() -> some View { scrollContentBackground(.hidden).background(Theme.background).tint(Theme.primary) }
}
