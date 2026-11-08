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

    init(preferences: UserDefaults = .standard) {
        self.preferences = preferences
        defaultTone = preferences.string(forKey: "defaultTone").flatMap(BuiltInTone.init(rawValue:)) ?? .casual
    }
}
