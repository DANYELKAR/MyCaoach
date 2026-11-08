import Foundation
import Observation

@MainActor @Observable
final class AppRuntime {
    let configuration: AppConfiguration
    let auth: AuthService
    let api: APIClient
    var profile: ProfileDTO?
    var usage: UsageDTO?
    var tones: [ToneDTO] = []
    private var started = false
    var isRestoring = true
    var hasSession = false
    var errorMessage: String?
    var showPaywall = false
    var toast: String?
    var onboardingComplete: Bool { profile?.ageConfirmedAt != nil && profile?.aiConsentAt != nil }
    deinit {}
    init(configuration: AppConfiguration, testNamespace: String? = nil) {
        self.configuration = configuration
        auth = AuthService(configuration: configuration, namespace: testNamespace ?? "com.danyelkar.mycoach.auth")
        api = APIClient(baseURL: configuration.apiBaseURL)
        api.token = { [weak auth] in try await auth?.accessToken() }
        api.refresh = { [weak auth] in try await auth?.refresh() }
        api.unauthorized = { [weak self] in self?.hasSession = false; self?.profile = nil; self?.auth.clearLocal() }
    }
    func start() async {
        guard !started else { return }; started = true
        await auth.restore()
        if auth.userID != nil {
            hasSession = true
            do { try await reload() } catch { handle(error) }
        }
        isRestoring = false
    }
    func reload() async throws {
        profile = try await api.request("v1/me")
        usage = try await api.request("v1/me/usage")
        tones = try await api.request("v1/tones")
    }
    func authenticated() async throws { hasSession = true; try await reload() }
    func updateProfile(_ body: [String: JSONValue]) async throws { profile = try await api.request("v1/me", method: "PATCH", body: body) }
    func handle(_ error: Error) {
        if error is CancellationError || (error as? URLError)?.code == .cancelled { return }
        if let error = error as? APIError, ["FORBIDDEN_PLAN", "OUT_OF_CREDITS"].contains(error.code) { showPaywall = true }
        errorMessage = (error as? URLError) != nil ? "You're offline or the server is unavailable. Check your connection and try again." : error.localizedDescription
    }
    func signOut() async { await auth.signOut(); reset() }
    func reset() { profile = nil; usage = nil; tones = []; hasSession = false; errorMessage = nil; toast = nil }
    func requirePlus(_ action: () -> Void) { if profile?.isPlus == true { action() } else { showPaywall = true } }
}
