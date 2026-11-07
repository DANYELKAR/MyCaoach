import Foundation

enum BuiltInTone: String, CaseIterable, Codable, Identifiable {
    case casual, flirty, playful, witty, romantic, confident, bold

    var id: String { rawValue }
    var title: String {
        switch self {
        case .casual: String(localized: "Casual")
        case .flirty: String(localized: "Flirty")
        case .playful: String(localized: "Playful")
        case .witty: String(localized: "Witty")
        case .romantic: String(localized: "Romantic")
        case .confident: String(localized: "Confident")
        case .bold: String(localized: "Bold")
        }
    }
    var symbol: String {
        switch self {
        case .casual: "cup.and.saucer.fill"
        case .flirty: "heart.fill"
        case .playful: "balloon.fill"
        case .witty: "sparkles"
        case .romantic: "heart.circle.fill"
        case .confident: "bolt.fill"
        case .bold: "flame.fill"
        }
    }
    var detail: String {
        switch self {
        case .casual: String(localized: "Easygoing, warm, and naturally you.")
        case .flirty: String(localized: "A little charm. A respectful first step.")
        case .playful: String(localized: "Lighthearted with a little personality.")
        case .witty: String(localized: "A clever observation, without trying too hard.")
        case .romantic: String(localized: "Thoughtful, sincere, and a little sweet.")
        case .confident: String(localized: "Clear, self-assured, and pressure-free.")
        case .bold: String(localized: "Daring and memorable. Always respectful.")
        }
    }
}

enum AppTab: String, CaseIterable, Identifiable {
    case home, people, history, saved, profile
    var id: String { rawValue }
    var title: String { rawValue.capitalized }
    var symbol: String {
        switch self {
        case .home: "sparkles"
        case .people: "person.2"
        case .history: "clock.arrow.circlepath"
        case .saved: "heart"
        case .profile: "person.crop.circle"
        }
    }
}
