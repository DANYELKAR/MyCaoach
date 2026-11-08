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
}
