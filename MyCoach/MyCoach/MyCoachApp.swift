import SwiftUI
import GoogleSignIn

@main
struct MyCoachApp: App {
    @State private var state: AppState
    private var configurationFailure: String?

    init() {
        #if DEBUG
        if ProcessInfo.processInfo.arguments.contains("-ui-testing") {
            let preferences = UserDefaults(suiteName: "MyCoach.UITests")!
            preferences.removePersistentDomain(forName: "MyCoach.UITests")
            _state = State(initialValue: AppState(preferences: preferences))
            return
        }
        #endif
        let value = AppState()
        do { value.runtime = AppRuntime(configuration: try AppConfiguration.load(), testNamespace: ProcessInfo.processInfo.arguments.contains("-e2e") ? "com.danyelkar.mycoach.e2e" : nil)
            #if DEBUG
            if ProcessInfo.processInfo.arguments.contains("-e2e-reset") { value.runtime?.auth.clearLocal() }
            #endif
        }
        catch { configurationFailure = error.localizedDescription }
        _state = State(initialValue: value)
    }

    var body: some Scene {
        WindowGroup {
            Group {
                if let configurationFailure { ContentUnavailableView("Configuration required", systemImage: "gear.badge.xmark", description: Text(configurationFailure)) }
                else { RootView().environment(state) }
            }.onOpenURL { _ = GIDSignIn.sharedInstance.handle($0) }
        }
    }
}
