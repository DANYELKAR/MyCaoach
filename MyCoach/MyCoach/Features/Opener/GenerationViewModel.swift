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
    deinit {}
    init(reply: Bool = false, contactID: UUID? = nil) { self.reply = reply; self.contactID = contactID }
    var canGenerate: Bool { toneID != nil && (reply ? (!text.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty != !images.isEmpty) : images.count == 1) && images.count <= (reply ? 3 : 1) && note.utf16.count <= 1000 && text.utf16.count <= 10000 && !isLoading }
    private func resetKey() { idempotencyKey = UUID().uuidString; result = nil }
    func generate(runtime: AppRuntime) {
        guard canGenerate, let toneID else { return }
        isLoading = true; error = nil
        task = Task {
            defer { isLoading = false }
            do {
                let value = try await runtime.api.generate(reply: reply, toneID: toneID, note: note, text: text, contactID: contactID, key: idempotencyKey, images: images)
                try Task.checkCancellation()
                result = value; runtime.usage = value.usage
            } catch {
                if !Task.isCancelled { self.error = error.localizedDescription; runtime.handle(error) }
            }
        }
    }
    func cancel() { task?.cancel(); isLoading = false }
    func startAgain() { result = nil; resetKey() }
}
