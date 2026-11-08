import XCTest

@MainActor
final class LocalEndToEndTests: XCTestCase {
    private var email = ""
    private let password = "MyCoach-test-2026!"
    private var app: XCUIApplication!

    override func setUpWithError() throws {
        continueAfterFailure = true
        guard ProcessInfo.processInfo.environment["MYCOACH_E2E"] == "1" else { throw XCTSkip("Opt-in local Docker integration: TEST_RUNNER_MYCOACH_E2E=1") }
        email = "ios-\(UUID().uuidString.lowercased())@example.test"
    }
    override func tearDown() async throws {
        app?.terminate()
        guard !email.isEmpty else { return }
        // Clean up only the account created by this test, also when a later UI assertion fails.
        var login = URLRequest(url: URL(string: "http://localhost:8080/local/auth/login")!)
        login.httpMethod = "POST"; login.setValue("application/json", forHTTPHeaderField: "Content-Type")
        login.httpBody = try JSONSerialization.data(withJSONObject: ["email": email, "password": password])
        if let (data, _) = try? await URLSession.shared.data(for: login), let token = (try? JSONSerialization.jsonObject(with: data) as? [String: Any])?["accessToken"] as? String {
            var remove = URLRequest(url: URL(string: "http://localhost:8080/v1/me")!)
            remove.httpMethod = "DELETE"; remove.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
            _ = try? await URLSession.shared.data(for: remove)
        }
    }
    private func onboard() {
        app = XCUIApplication(); app.launchArguments = ["-e2e", "-e2e-reset"]; app.launch()
        let next = app.buttons["onboarding.continue"]
        XCTAssertTrue(next.waitForExistence(timeout: 15))
        for _ in 0..<4 { next.tap() }
        app.switches["consent.age"].tap(); app.switches["consent.ai"].tap()
        app.buttons["onboarding.consentContinue"].tap()
        app.textFields["auth.email"].tap(); app.textFields["auth.email"].typeText(email)
        app.secureTextFields["auth.password"].tap(); app.secureTextFields["auth.password"].typeText(password)
        app.buttons["auth.submit"].tap()
        XCTAssertTrue(app.buttons["home.opener"].waitForExistence(timeout: 15))
        // iOS can offer to save the local development password; this is system UI.
        for label in ["Not Now", "Later", "Sonra", "Şimdi Değil"] {
            let button = app.sheets.buttons[label]
            if button.exists { button.tap(); XCTAssertTrue(button.waitForNonExistence(timeout: 5)); Thread.sleep(forTimeInterval: 1); break }
        }
    }
    func testOnboardingConsentAndOpenerAgainstDocker() {
        onboard()
        let before = XCTAttachment(screenshot: app.screenshot()); before.name = "Home before opener"; before.lifetime = .keepAlways; add(before)
        app.buttons["home.opener"].tap()
        if !app.buttons["generation.fixture"].waitForExistence(timeout: 5) { print("Opener navigation state: \(String(describing: app.buttons["home.opener"].value))"); print(app.debugDescription); XCTFail("Opener did not open"); return }
        app.buttons["generation.fixture"].tap()
        app.swipeUp()
        app.buttons["generation.submit"].tap()
        XCTAssertTrue(app.staticTexts["Make it yours."].waitForExistence(timeout: 30))
        app.buttons["Copy"].firstMatch.tap()
        let image = XCTAttachment(screenshot: app.screenshot()); image.name = "Live local backend opener"; image.lifetime = .keepAlways; add(image)
    }
}
