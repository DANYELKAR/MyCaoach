import XCTest

@MainActor
final class MyCoachUITests: XCTestCase {
    private func launch(largeType: Bool = false) -> XCUIApplication {
        let app = XCUIApplication()
        app.launchArguments = ["-ui-testing"]
        if largeType { app.launchArguments += ["-UIPreferredContentSizeCategoryName", "UICTContentSizeCategoryAccessibilityXXXL"] }
        app.launch()
        return app
    }

    func testFiveTabsAndNavigation() throws {
        let app = launch()
        XCTAssertTrue(app.staticTexts["MyCoach"].waitForExistence(timeout: 10))
        for title in ["Home", "People", "History", "Saved", "Profile"] {
            let tab = app.tabBars.buttons[title]
            XCTAssertTrue(tab.exists, title)
            tab.tap()
            if title != "Home" { XCTAssertTrue(app.navigationBars[title].waitForExistence(timeout: 3)) }
        }
        app.tabBars.buttons["Home"].tap()
        app.buttons["home.people"].tap()
        XCTAssertTrue(app.navigationBars["People"].waitForExistence(timeout: 3))
    }

    func testToneSelectionUpdatesProfileAndGuideCanClose() throws {
        let app = launch()
        app.buttons["tone.playful"].tap()
        app.tabBars.buttons["Profile"].tap()
        XCTAssertTrue(app.buttons["profile.tone.playful"].isSelected)
        app.buttons["profile.tone.witty"].tap()
        app.tabBars.buttons["Home"].tap()
        XCTAssertTrue(app.staticTexts["A clever observation, without trying too hard."].exists)
        app.buttons["About message tones"].tap()
        XCTAssertTrue(app.navigationBars["Your tone"].waitForExistence(timeout: 3))
        app.buttons["Done"].tap()
        XCTAssertTrue(app.staticTexts["MyCoach"].exists)
        let screenshot = XCTAttachment(screenshot: app.screenshot())
        screenshot.name = "MyCoach Home"
        screenshot.lifetime = .keepAlways
        add(screenshot)
    }

    func testAccessibilityTextSizeKeepsTabsReachable() {
        let app = launch(largeType: true)
        XCTAssertTrue(app.tabBars.buttons["Profile"].waitForExistence(timeout: 10))
        app.tabBars.buttons["Profile"].tap()
        XCTAssertTrue(app.navigationBars["Profile"].exists)
        let screenshot = XCTAttachment(screenshot: app.screenshot())
        screenshot.name = "MyCoach Accessibility Text Size"
        screenshot.lifetime = .keepAlways
        add(screenshot)
    }
}
