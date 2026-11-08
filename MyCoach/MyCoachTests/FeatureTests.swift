import XCTest
@testable import MyCoach

@MainActor
final class FeatureTests: XCTestCase {
    func runtime() throws -> AppRuntime {
        let config = try AppConfiguration.parse(["MyCoachAPIBaseURL": "http://localhost:8080", "MyCoachBackendMode": "local"])
        let session = URLSessionConfiguration.ephemeral; session.protocolClasses = [MockURLProtocol.self]
        return AppRuntime(configuration: config, testNamespace: "MyCoach.feature.\(UUID())", session: URLSession(configuration: session))
    }
    func profile(plan: String) -> ProfileDTO {
        ProfileDTO(id: UUID(), displayName: nil, ageConfirmedAt: "2026-11-08T00:00:00Z", aiConsentAt: "2026-11-08T00:00:00Z", goals: [], humorLevel: 50, defaultToneId: nil, plan: plan, timezone: "Europe/Istanbul", storeImages: false, analyticsOptIn: false)
    }
    override func tearDown() { MockURLProtocol.handler = nil }
    func testPlusGateUsesServerProfileAndPresentsPaywallForFree() throws {
        let runtime = try runtime(); var opened = false
        runtime.profile = profile(plan: "FREE"); runtime.requirePlus { opened = true }
        XCTAssertFalse(opened); XCTAssertTrue(runtime.showPaywall)
        runtime.showPaywall = false; runtime.profile = profile(plan: "PLUS"); runtime.requirePlus { opened = true }
        XCTAssertTrue(opened); XCTAssertFalse(runtime.showPaywall)
    }
    func testLocalBillingRefreshesProfileAndUsageFromServer() async throws {
        let runtime = try runtime()
        let plus = try JSONEncoder().encode(profile(plan: "PLUS"))
        MockURLProtocol.handler = { request in
            if request.url?.path == "/local/billing/entitlement" { XCTAssertEqual(request.httpMethod, "PUT"); return (200, plus) }
            XCTAssertEqual(request.url?.path, "/v1/me/usage")
            return (200, Data("{\"remainingToday\":99,\"dailyLimit\":100}".utf8))
        }
        try await runtime.billing.localEntitlement(true, runtime: runtime)
        XCTAssertEqual(runtime.profile?.plan, "PLUS"); XCTAssertEqual(runtime.usage?.dailyLimit, 100)
    }
    func result(count: Int = 5) -> GenerationResult {
        GenerationResult(generation: GenerationDTO(id: UUID(), type: "OPENER", toneId: UUID(), contactId: nil, threadId: UUID(), createdAt: "2026-11-08T00:00:00Z"), suggestions: (1...count).map { SuggestionDTO(id: UUID(), text: "Suggestion \($0): what made you smile?", rank: $0, isTopPick: $0 == 1, feedback: nil, feedbackReason: nil, sentStatus: "NOT_SENT", outcome: "NONE") }, usage: UsageDTO(remainingToday: 2, dailyLimit: 3, resetsAt: nil))
    }
    func testGenerationRetriesSameInputAndDecodesFiveSuggestions() async throws {
        let runtime = try runtime(), model = GenerationViewModel()
        model.toneID = UUID(); model.images = [Data([1])]
        let key = model.idempotencyKey
        MockURLProtocol.handler = { _ in (503, Data("{\"code\":\"AI_UNAVAILABLE\",\"message\":\"Unavailable\"}".utf8)) }
        await model.generate(runtime: runtime)?.value
        XCTAssertNil(model.result); XCTAssertFalse(model.isLoading); XCTAssertEqual(model.idempotencyKey, key)
        let data = try JSONEncoder().encode(result())
        MockURLProtocol.handler = { _ in (200, data) }
        await model.generate(runtime: runtime)?.value
        XCTAssertEqual(model.result?.suggestions.count, 5); XCTAssertEqual(runtime.usage?.remainingToday, 2); XCTAssertEqual(model.idempotencyKey, key)
    }
    func testMalformedGenerationAndCancelledRequestsNeverBecomeResults() async throws {
        let runtime = try runtime(), model = GenerationViewModel()
        model.toneID = UUID(); model.images = [Data([1])]
        let data = try JSONEncoder().encode(result(count: 4))
        MockURLProtocol.handler = { _ in (200, data) }
        await model.generate(runtime: runtime)?.value
        XCTAssertNil(model.result); XCTAssertNotNil(model.error)
        model.error = nil
        let task = model.generate(runtime: runtime); model.cancel(); await task?.value
        XCTAssertNil(model.result); XCTAssertNil(model.error); XCTAssertFalse(model.isLoading)
    }
    func testQuotaAndPlanErrorsPresentPaywallWithoutAnAlertConflict() throws {
        let runtime = try runtime()
        runtime.handle(APIError(code: "OUT_OF_CREDITS", message: "quota", requestId: "trace"))
        XCTAssertTrue(runtime.showPaywall); XCTAssertNil(runtime.errorMessage); XCTAssertNotNil(runtime.paywallReason)
    }
    func testDowngradeRemovesCustomTonesAndFallsBackToBuiltIn() throws {
        let runtime = try runtime()
        let builtIn = ToneDTO(id: UUID(), name: "Casual", emoji: nil, description: nil, builtIn: true, lengthLevel: 50, boldnessLevel: 50, humorLevel: 50)
        let custom = ToneDTO(id: UUID(), name: "Custom", emoji: nil, description: nil, builtIn: false, lengthLevel: 50, boldnessLevel: 50, humorLevel: 50)
        runtime.tones = [builtIn, custom]; runtime.profile = profile(plan: "PLUS"); runtime.profile?.defaultToneId = custom.id
        XCTAssertEqual(runtime.initialTone(preference: .casual), custom.id)
        runtime.profile?.plan = "FREE"
        XCTAssertEqual(runtime.selectableTones.map(\.id), [builtIn.id])
        XCTAssertEqual(runtime.initialTone(preference: .casual), builtIn.id)
    }
    func testLightAppearanceTextContrast() {
        for (foreground, background): (UInt, UInt) in [(0x201A2D,0xF6F3FA),(0x665D74,0xFFFFFF),(0xB91F50,0xFFFFFF),(0x4C267D,0xEEE8F5)] { XCTAssertGreaterThanOrEqual(Theme.contrast(foreground, background), 4.5) }
    }
    func testPublicClientConfigurationRejectsRevenueCatServerKeys() {
        XCTAssertThrowsError(try AppConfiguration.parse(["MyCoachAPIBaseURL":"http://localhost:8080", "MyCoachBackendMode":"local", "MyCoachRevenueCatPublicKey":"sk_server_secret"]))
    }

    func testAnalyticsRespectsConsentAndFirstGenerationIsOnce() throws {
        let recorder = AnalyticsRecorder()
        let runtime = AppRuntime(configuration: try runtime().configuration, testNamespace: "MyCoach.analytics.\(UUID())", analytics: recorder)
        runtime.profile = profile(plan: "FREE")
        runtime.track(.feedbackGiven); XCTAssertTrue(recorder.snapshot.isEmpty)
        runtime.profile?.analyticsOptIn = true
        runtime.generated(); runtime.generated(); runtime.track(.feedbackGiven)
        XCTAssertEqual(recorder.snapshot, [.firstGeneration, .feedbackGiven])
        runtime.profile?.analyticsOptIn = false
        runtime.track(.paywallViewed); XCTAssertEqual(recorder.snapshot.count, 2)
    }

}

final class AnalyticsRecorder: AnalyticsService, @unchecked Sendable {
    private let lock = NSLock()
    private var events: [AnalyticsEvent] = []
    func track(_ event: AnalyticsEvent) { lock.withLock { events.append(event) } }
    var snapshot: [AnalyticsEvent] { lock.withLock { events } }
}
