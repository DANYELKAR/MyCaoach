import SwiftUI

@main
struct MyCoachApp: App {
    @State private var state: AppState

    init() {
        #if DEBUG
        if ProcessInfo.processInfo.arguments.contains("-ui-testing") {
            let preferences = UserDefaults(suiteName: "MyCoach.UITests")!
            preferences.removePersistentDomain(forName: "MyCoach.UITests")
            _state = State(initialValue: AppState(preferences: preferences))
            return
        }
        #endif
        _state = State(initialValue: AppState())
    }

    var body: some Scene {
        WindowGroup {
            RootView().environment(state)
        }
    }
}
