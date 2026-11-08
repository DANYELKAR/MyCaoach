import Foundation

enum JSONValue: Codable, Sendable, Equatable {
    case s(String), b(Bool), n(Double), a([JSONValue]), o([String: JSONValue]), null
    init(from decoder: Decoder) throws {
        let c = try decoder.singleValueContainer()
        if c.decodeNil() { self = .null }
        else if let v = try? c.decode(Bool.self) { self = .b(v) }
        else if let v = try? c.decode(Double.self) { self = .n(v) }
        else if let v = try? c.decode(String.self) { self = .s(v) }
        else if let v = try? c.decode([JSONValue].self) { self = .a(v) }
        else { self = .o(try c.decode([String: JSONValue].self)) }
    }
    func encode(to encoder: Encoder) throws {
        var c = encoder.singleValueContainer()
        switch self {
        case .s(let v): try c.encode(v)
        case .b(let v): try c.encode(v)
        case .n(let v): try c.encode(v)
        case .a(let v): try c.encode(v)
        case .o(let v): try c.encode(v)
        case .null: try c.encodeNil()
        }
    }
    var display: String {
        switch self {
        case .s(let v): v
        case .b(let v): String(v)
        case .n(let v): String(v)
        case .a(let v): v.map(\.display).joined(separator: ", ")
        case .o(let v): v.keys.sorted().map { "\($0): \(v[$0]!.display)" }.joined(separator: "\n")
        case .null: ""
        }
    }
}
struct ProfileDTO: Codable, Sendable {
    let id: UUID
    var displayName: String?
    var ageConfirmedAt: String?
    var aiConsentAt: String?
    var goals: [String]
    var humorLevel: Int
    var defaultToneId: UUID?
    var plan: String
    var timezone: String
    var storeImages: Bool
    var analyticsOptIn: Bool
    var isPlus: Bool { plan == "PLUS" }
}
struct UsageDTO: Codable, Sendable { let remainingToday: Int; let dailyLimit: Int; let resetsAt: String? }
struct ToneDTO: Codable, Identifiable, Hashable, Sendable {
    let id: UUID; var name: String; var emoji: String?; var description: String?; var builtIn: Bool
    var lengthLevel: Int; var boldnessLevel: Int; var humorLevel: Int
}
struct GenerationDTO: Codable, Identifiable, Sendable {
    let id: UUID; let type: String; let toneId: UUID; let contactId: UUID?; let threadId: UUID?; let createdAt: String
}
struct SuggestionDTO: Codable, Identifiable, Sendable {
    let id: UUID; let text: String; let rank: Int; let isTopPick: Bool
    var feedback: String?; var feedbackReason: String?; var sentStatus: String; var outcome: String
}
struct GenerationResult: Codable, Sendable { let generation: GenerationDTO; var suggestions: [SuggestionDTO]; let usage: UsageDTO }
struct ContactDTO: Codable, Identifiable, Sendable {
    let id: UUID; var name: String; var source: String?; var status: String; var interests: [String]; var notes: String?
}
struct ThreadDTO: Codable, Identifiable, Sendable { let id: UUID; let contactId: UUID?; let title: String?; let lastActivityAt: String }
struct TimelineDTO: Codable, Sendable { let thread: ThreadDTO; let generations: [GenerationResult] }
struct FolderDTO: Codable, Identifiable, Hashable, Sendable { let id: UUID; let name: String }
struct SavedDTO: Codable, Identifiable, Sendable {
    let id: UUID; let folderId: UUID?; let kind: String; let suggestionId: UUID?; let title: String?; let text: String; let tags: [String]
}
struct StyleDTO: Codable, Sendable { let sampleMessages: [String]; let summary: [String: JSONValue]?; let enabled: Bool }
struct EmptyResponse: Decodable, Sendable {}
