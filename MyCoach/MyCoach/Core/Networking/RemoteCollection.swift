import Foundation
import Observation

@MainActor @Observable
final class RemoteCollection<Item: Decodable & Identifiable & Sendable> where Item.ID: Sendable {
    var items: [Item] = []
    var isLoading = false
    var error: String?
    var hasMore = false
    private var page = 0
    deinit {}
    func load(_ api: APIClient, path: String, reset: Bool = true) async {
        guard !isLoading else { return }
        isLoading = true; error = nil
        defer { isLoading = false }
        do {
            let nextPage = reset ? 0 : page
            let separator = path.contains("?") ? "&" : "?"
            let values: [Item] = try await api.request(path + "\(separator)page=\(nextPage)&size=50")
            try Task.checkCancellation()
            if reset { items = values } else { let known = Set(items.map(\.id)); items += values.filter { !known.contains($0.id) } }
            page = nextPage + 1; hasMore = values.count == 50
        } catch {
            if !Task.isCancelled { self.error = error.localizedDescription }
        }
    }
}
