import Foundation
import Observation

@MainActor @Observable
final class AppState {
    var runtime: AppRuntime?
    var selectedTab: AppTab = .home
    var defaultTone: BuiltInTone {
        didSet { preferences.set(defaultTone.rawValue, forKey: "defaultTone") }
    }
    @ObservationIgnored private let preferences: UserDefaults

    // Avoid the Swift 6.2 implicit actor-deinit crash on iOS 26.2 and earlier.
    // Regression coverage: testModelLifetimeInsideTaskLocalScope.
    // https://github.com/swiftlang/swift/issues/88036
    deinit {}

    func chooseTone(_ tone: BuiltInTone) {
        guard runtime?.isUpdatingTone != true else { return }
        let previous = defaultTone
        defaultTone = tone
        guard let runtime, let id = runtime.tones.first(where: { $0.name.lowercased() == tone.rawValue })?.id else { return }
        runtime.isUpdatingTone = true
        Task {
            defer { runtime.isUpdatingTone = false }
            do { try await runtime.updateProfile(["defaultToneId": .s(id.uuidString)]) }
            catch { defaultTone = previous; runtime.handle(error) }
        }
    }
    init(preferences: UserDefaults = .standard) {
        self.preferences = preferences
        defaultTone = preferences.string(forKey: "defaultTone").flatMap(BuiltInTone.init(rawValue:)) ?? .casual
    }
}
