import XCTest
import UIKit
@testable import MyCoach

@MainActor
final class FoundationTests: XCTestCase {
    func testConfigurationReadsDebugBundleValues() throws {
        let config = try AppConfiguration.load()
        XCTAssertEqual(config.apiBaseURL.absoluteString, "http://localhost:8080")
        XCTAssertEqual(config.backendMode, .local)
        // Local.xcconfig may contain public cloud settings while Debug remains local.
        XCTAssertEqual(config.supabaseURL == nil, config.supabasePublishableKey == nil)
    }

    func testLocalAuthNeverCreatesACloudClientEvenWithSupabaseSettings() async throws {
        let config = try AppConfiguration.parse([
            "MyCoachBackendMode": "local", "MyCoachAPIBaseURL": "http://localhost:8080",
            "MyCoachSupabaseURL": "https://project.supabase.co",
            "MyCoachSupabasePublishableKey": "sb_publishable_fixture"
        ])
        let auth = AuthService(configuration: config, namespace: "MyCoach.isolation.\(UUID())")
        XCTAssertNil(auth.supabase)
        let localUser = UUID()
        auth.userID = localUser
        await auth.restore()
        XCTAssertEqual(auth.userID, localUser)
        do { try await auth.refresh(); XCTFail("Local sessions cannot refresh against Supabase") }
        catch { XCTAssertEqual((error as? APIError)?.code, "UNAUTHORIZED") }
    }

    func testHTTPIsLimitedToLocalDevelopmentLoopback() throws {
        for host in ["localhost", "127.0.0.1"] {
            let config = try AppConfiguration.parse(["MyCoachBackendMode": "local", "MyCoachAPIBaseURL": "http://\(host):8080"])
            XCTAssertEqual(config.backendMode, .local)
        }
        for (mode, url) in [("supabase", "http://api.example.test"), ("local", "http://192.168.1.2:8080"), ("local", "file:///tmp/api")] {
            XCTAssertThrowsError(try AppConfiguration.parse(["MyCoachBackendMode": mode, "MyCoachAPIBaseURL": url]))
        }
        XCTAssertNoThrow(try AppConfiguration.parse(["MyCoachBackendMode": "supabase", "MyCoachAPIBaseURL": "https://api.example.test"]))
    }

    func testConfigurationRejectsCredentialsAndUnknownMode() {
        XCTAssertThrowsError(try AppConfiguration.parse(["MyCoachBackendMode": "local", "MyCoachAPIBaseURL": "https://user:secret@api.example.test"]))
        XCTAssertThrowsError(try AppConfiguration.parse(["MyCoachBackendMode": "unknown", "MyCoachAPIBaseURL": "https://api.example.test"]))
        XCTAssertThrowsError(try AppConfiguration.parse(["MyCoachBackendMode": "local", "MyCoachAPIBaseURL": "https://api.example.test?token=secret"]))
    }

    func testSupabaseRequiresBothPublicConfigurationValues() throws {
        let base = ["MyCoachBackendMode": "supabase", "MyCoachAPIBaseURL": "https://api.example.test"]
        XCTAssertThrowsError(try AppConfiguration.parse(base.merging(["MyCoachSupabaseURL": "https://project.supabase.co"]) { _, b in b }))
        let valid = base.merging(["MyCoachSupabaseURL": "https://project.supabase.co", "MyCoachSupabasePublishableKey": "sb_publishable_fixture"]) { _, b in b }
        XCTAssertEqual(try AppConfiguration.parse(valid).supabaseURL?.host, "project.supabase.co")
    }

    func testServerKeysCannotBeEmbeddedAsClientKeys() throws {
        let base = ["MyCoachBackendMode": "supabase", "MyCoachAPIBaseURL": "https://api.example.test", "MyCoachSupabaseURL": "https://project.supabase.co"]
        XCTAssertThrowsError(try AppConfiguration.parse(base.merging(["MyCoachSupabasePublishableKey": "sb_secret_fixture"]) { _, b in b }))
        let payload = Data("{\"role\":\"service_role\"}".utf8).base64EncodedString().replacingOccurrences(of: "=", with: "")
        XCTAssertThrowsError(try AppConfiguration.parse(base.merging(["MyCoachSupabasePublishableKey": "header.\(payload).signature"]) { _, b in b }))
    }

    func testTonePreferencePersistsAndInvalidValueFallsBack() throws {
        let suite = "MyCoach.UnitTests.\(UUID().uuidString)"
        let preferences = try XCTUnwrap(UserDefaults(suiteName: suite))
        defer { preferences.removePersistentDomain(forName: suite) }
        let first = AppState(preferences: preferences)
        XCTAssertEqual(first.defaultTone, .casual)
        XCTAssertEqual(first.selectedTab, .home)
        first.defaultTone = .witty
        XCTAssertEqual(AppState(preferences: preferences).defaultTone, .witty)
        preferences.set("unknown-tone", forKey: "defaultTone")
        XCTAssertEqual(AppState(preferences: preferences).defaultTone, .casual)
    }

    func testSevenTonesAndFiveTabsHaveValidSystemSymbols() {
        XCTAssertEqual(BuiltInTone.allCases.count, 7)
        XCTAssertEqual(AppTab.allCases.map(\.title), ["Home", "People", "History", "Saved", "Profile"])
        for tone in BuiltInTone.allCases { XCTAssertNotNil(UIImage(systemName: tone.symbol), tone.title) }
        for tab in AppTab.allCases { XCTAssertNotNil(UIImage(systemName: tab.symbol), tab.title) }
    }

    func testActionAndBodyColorsMeetTextContrastRequirement() {
        for stop in Theme.actionStops { XCTAssertGreaterThanOrEqual(Theme.contrast(0xF5F3FA, stop), 4.5) }
        XCTAssertGreaterThanOrEqual(Theme.contrast(0xA09BB0, 0x1A1722), 4.5)
        XCTAssertGreaterThanOrEqual(Theme.contrast(0xF5F3FA, 0x0F0D14), 4.5)
    }

    func testModelLifetimeInsideTaskLocalScope() throws {
        let suite = "MyCoach.DeinitTests.\(UUID().uuidString)"
        let preferences = try XCTUnwrap(UserDefaults(suiteName: suite))
        defer { preferences.removePersistentDomain(forName: suite) }
        TestTaskContext.$marker.withValue(1) {
            for _ in 0..<25 {
                weak var released: AppState?
                autoreleasepool {
                    let model = AppState(preferences: preferences)
                    released = model
                    model.defaultTone = .bold
                }
                XCTAssertNil(released)
            }
        }
    }
}

private enum TestTaskContext {
    @TaskLocal static var marker = 0
}
