import Foundation
import Observation

@MainActor @Observable
final class GenerationViewModel {
    var images: [Data] = [] { didSet { resetKey() } }
    var text = "" { didSet { resetKey() } }
    var note = "" { didSet { resetKey() } }
    var toneID: UUID? { didSet { if oldValue != toneID { resetKey() } } }
    var contactID: UUID? { didSet { if oldValue != contactID { resetKey() } } }
    let reply: Bool
    var isLoading = false
    var result: GenerationResult?
    var error: String?
    private(set) var idempotencyKey = UUID().uuidString
    private var task: Task<Void, Never>?
    private var activeRun = UUID()
    deinit {}
    init(reply: Bool = false, contactID: UUID? = nil) { self.reply = reply; self.contactID = contactID }
    var canGenerate: Bool { toneID != nil && (reply ? (!text.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty != !images.isEmpty) : images.count == 1) && images.count <= (reply ? 3 : 1) && note.utf16.count <= 1000 && text.utf16.count <= 10000 && !isLoading }
    private func resetKey() { cancel(); idempotencyKey = UUID().uuidString; result = nil }
    @discardableResult func generate(runtime: AppRuntime) -> Task<Void, Never>? {
        guard canGenerate, let toneID else { return nil }
        let run = UUID(); activeRun = run
        let key = idempotencyKey, images = images, text = text, note = note, contact = contactID
        isLoading = true; error = nil
        task = Task {
            defer { if activeRun == run { isLoading = false } }
            do {
                let value = try await runtime.api.generate(reply: reply, toneID: toneID, note: note, text: text, contactID: contact, key: key, images: images)
                try Task.checkCancellation()
                let expected = reply ? 4 : 5
                guard value.suggestions.count == expected, Set(value.suggestions.map(\.id)).count == expected,
                      Set(value.suggestions.map { $0.text.trimmingCharacters(in: .whitespacesAndNewlines).lowercased() }).count == expected,
                      value.suggestions.allSatisfy({ !$0.text.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty && $0.text.utf16.count <= 220 }) else {
                    throw APIError(code: "INVALID_RESPONSE", message: "Suggestions couldn't be loaded. Try again or check History.", requestId: nil)
                }
                guard activeRun == run, idempotencyKey == key else { return }
                result = value; runtime.usage = value.usage; runtime.generated()
            } catch {
                if !Task.isCancelled { self.error = error.localizedDescription; runtime.handle(error) }
            }
        }
        return task
    }
    func cancel() { task?.cancel(); activeRun = UUID(); isLoading = false }
    func startAgain() { result = nil; resetKey() }
}
