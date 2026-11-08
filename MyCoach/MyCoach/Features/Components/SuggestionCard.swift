import SwiftUI

struct SuggestionCard: View {
    @Environment(AppState.self) private var state
    @State var suggestion: SuggestionDTO
    private var savedID: UUID? { state.runtime?.savedSuggestions[suggestion.id] }
    @State private var busy = false
    @State private var feedback = false
    @State private var report = false
    @State private var reason = ""
    @State private var details = ""
    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            if suggestion.isTopPick { Label("Top pick", systemImage: "sparkles").font(.caption.bold()).foregroundStyle(Theme.primary) }
            Text(suggestion.text).textSelection(.enabled).fixedSize(horizontal: false, vertical: true)
            ViewThatFits(in: .horizontal) {
                HStack { copy; save; feedbackButton }
                VStack(alignment: .leading) { copy; save; feedbackButton }
            }
            Menu {
                Button(suggestion.sentStatus == "SENT" ? "Mark as not sent" : "Mark as sent") { patch(["sentStatus": .s(suggestion.sentStatus == "SENT" ? "NOT_SENT" : "SENT")]) }
                Button("They replied") { patch(["outcome": .s("REPLIED")]) }
                Button("No reply") { patch(["outcome": .s("NO_REPLY")]) }
                Button("Clear outcome") { patch(["outcome": .s("NONE")]) }
                Button("Report", role: .destructive) { reason = ""; details = ""; report = true }
            } label: { Label(suggestion.sentStatus == "SENT" ? "Sent · \(suggestion.outcome.replacingOccurrences(of: "_", with: " ").lowercased())" : "Mark as sent / More", systemImage: "ellipsis.circle").font(.footnote).frame(minHeight: 44) }
        }.frame(maxWidth: .infinity, alignment: .leading).surfaceCard().disabled(busy)
            .sheet(isPresented: $feedback) {
                EditorShell(title: "Feedback", valid: details.utf16.count <= 500, save: { try await update(["feedback": .s(reason), "feedbackReason": .s(details)]) }) {
                    Picker("How was this suggestion?", selection: $reason) { Text("Like").tag("LIKE"); Text("Dislike").tag("DISLIKE") }.pickerStyle(.segmented)
                    TextField("Tell us why (optional)", text: $details, axis: .vertical).lineLimit(3...7)
                }
            }
            .sheet(isPresented: $report) {
                EditorShell(title: "Report suggestion", valid: !reason.isEmpty && details.utf16.count <= 2000, save: {
                    let _: [String: String]? = try await state.runtime?.api.request("v1/suggestions/\(suggestion.id)/report", method: "POST", body: ["reason": .s(reason), "details": .s(details)])
                    state.runtime?.toast = "Report received. Thank you."
                }) {
                    Picker("Reason", selection: $reason) { Text("Choose a reason").tag(""); ForEach(["Inappropriate", "Sensitive assumptions", "Harassment", "Other"], id: \.self) { Text($0).tag($0) } }
                    TextField("Details (optional)", text: $details, axis: .vertical).lineLimit(3...8)
                }
            }
    }
    private var copy: some View { Button { UIPasteboard.general.string = suggestion.text; state.runtime?.toast = "Copied to clipboard" } label: { Label("Copy", systemImage: "doc.on.doc").frame(minHeight: 44).contentShape(Rectangle()) }.buttonStyle(.plain) }
    private var save: some View { Button {
        state.runtime?.noteSavedMutation()
        busy = true
        Task { defer { busy = false }; do {
            guard let api = state.runtime?.api else { return }
            if let savedID { try await api.delete("v1/saved/\(savedID)"); state.runtime?.savedSuggestions[suggestion.id] = nil }
            else { let item: SavedDTO = try await api.request("v1/saved", method: "POST", body: ["kind": .s("SUGGESTION"), "suggestionId": .s(suggestion.id.uuidString)]); state.runtime?.savedSuggestions[suggestion.id] = item.id }
        } catch { state.runtime?.handle(error) } }
    } label: { Label(savedID == nil ? "Save" : "Saved", systemImage: savedID == nil ? "heart" : "heart.fill").frame(minHeight: 44).contentShape(Rectangle()) }.buttonStyle(.plain).accessibilityIdentifier("suggestion.save.\(suggestion.rank)") }
    private var feedbackButton: some View { Button { reason = suggestion.feedback ?? "LIKE"; details = suggestion.feedbackReason ?? ""; feedback = true } label: { Label("Feedback", systemImage: suggestion.feedback == "LIKE" ? "hand.thumbsup.fill" : "hand.thumbsup").frame(minHeight: 44).contentShape(Rectangle()) }.buttonStyle(.plain) }
    private func patch(_ body: [String: JSONValue]) { busy = true; Task { do { try await update(body) } catch { state.runtime?.handle(error) }; busy = false } }
    private func update(_ body: [String: JSONValue]) async throws { if let api = state.runtime?.api { suggestion = try await api.request("v1/suggestions/\(suggestion.id)", method: "PATCH", body: body); if body["feedback"] != nil { state.runtime?.track(.feedbackGiven) } } }
}
