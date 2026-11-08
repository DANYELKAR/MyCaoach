import Foundation
import Observation

@MainActor @Observable
final class AppRuntime {
    let configuration: AppConfiguration
    let auth: AuthService
    let analytics: any AnalyticsService
    private var didActivate = false
    @ObservationIgnored private var savedRevision = 0
    var savedSuggestions: [UUID: UUID] = [:]
    let billing = BillingService()
    let api: APIClient
    var profile: ProfileDTO?
    var usage: UsageDTO?
    var tones: [ToneDTO] = []
    private var started = false
    var isRestoring = true
    var hasSession = false
    var errorMessage: String?
    var isUpdatingTone = false
    var paywallReason: String?
    var showPaywall = false
    var toast: String?
    var selectableTones: [ToneDTO] { tones.filter { $0.builtIn || profile?.isPlus == true } }
    func initialTone(preference: BuiltInTone) -> UUID? {
        if let id = profile?.defaultToneId, selectableTones.contains(where: { $0.id == id }) { return id }
        return selectableTones.first(where: { $0.name.lowercased() == preference.rawValue })?.id ?? selectableTones.first?.id
    }
    var onboardingComplete: Bool { profile?.ageConfirmedAt != nil && profile?.aiConsentAt != nil }
    deinit {}
    init(configuration: AppConfiguration, testNamespace: String? = nil, session: URLSession = URLSession(configuration: .ephemeral), analytics: any AnalyticsService = NoOpAnalytics()) {
        self.configuration = configuration
        self.analytics = analytics
        auth = AuthService(configuration: configuration, namespace: testNamespace ?? "com.danyelkar.mycoach.auth")
        api = APIClient(baseURL: configuration.apiBaseURL, session: session)
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
        if let error = error as? APIError, ["FORBIDDEN_PLAN", "OUT_OF_CREDITS"].contains(error.code) { paywallReason = error.localizedDescription; showPaywall = true; return }
        errorMessage = (error as? URLError) != nil ? "You're offline or the server is unavailable. Check your connection and try again." : error.localizedDescription
    }
    func signOut() async { await billing.signOut(); await auth.signOut(); reset() }
    func reset() { savedRevision &+= 1; savedSuggestions = [:]; didActivate = false; showPaywall = false; paywallReason = nil; profile = nil; usage = nil; tones = []; hasSession = false; errorMessage = nil; toast = nil }
    func track(_ event: AnalyticsEvent) { if profile?.analyticsOptIn == true { analytics.track(event) } }
    func generated() { if !didActivate { track(.firstGeneration); didActivate = true } }
    func noteSavedMutation() { savedRevision &+= 1 }
    func loadSavedSuggestions() async {
        let revision = savedRevision
        let collection = RemoteCollection<SavedDTO>()
        await collection.load(api, path: "v1/saved")
        while collection.hasMore && collection.error == nil && !Task.isCancelled { await collection.load(api, path: "v1/saved", reset: false) }
        guard collection.error == nil, !Task.isCancelled, savedRevision == revision else { return }
        savedSuggestions = Dictionary(collection.items.compactMap { item in item.suggestionId.map { ($0, item.id) } }, uniquingKeysWith: { first, _ in first })
    }
    func requirePlus(_ action: () -> Void) { if profile?.isPlus == true { action() } else { paywallReason = nil; showPaywall = true } }
}
