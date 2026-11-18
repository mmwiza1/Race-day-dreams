import XCTest

final class RunningWorkoutPersistenceTests: XCTestCase {
    override func setUpWithError() throws {
        continueAfterFailure = false
    }

    func testFavouriteSurvivesRelaunchWithoutLocationPermission() {
        let app = XCUIApplication()
        app.launch()
        tap("Record my runs", in: app)
        tap("Create custom workout", in: app)
        XCTAssertFalse(control("Start custom workout", in: app).exists)
        tap("Add new group", in: app)
        tap("Save step", in: app)
        tap("Save favourite workout", in: app)

        XCTAssertTrue(control("Create custom workout", in: app).waitForExistence(timeout: 15))
        app.terminate()
        app.launch()

        tap("Record", in: app)
        tap("Favourite workouts", in: app)
        tap("Edit My workout", in: app)
        XCTAssertTrue(control("Edit step 1 · run", in: app).waitForExistence(timeout: 15))
        XCTAssertFalse(control("Start custom workout", in: app).exists)

        tap("Close editor", in: app)
        app.swipeDown()
        app.swipeDown()
        tap("Allow location access", in: app)
        let permission = XCUIApplication(bundleIdentifier: "com.apple.springboard")
            .buttons["Allow While Using App"]
        XCTAssertTrue(permission.waitForExistence(timeout: 20))
        permission.tap()
        tap("Start My workout · 1 steps", in: app)
        tap("Pause", in: app)
        tap("Resume", in: app)
        XCTAssertTrue(control("Pause", in: app).waitForExistence(timeout: 15))

        // Do not fabricate a route just to make an interrupted run saveable.
        app.terminate()
        app.launch()
        tap("Record", in: app)
        XCTAssertTrue(control("Interrupted recording", in: app).waitForExistence(timeout: 20))
        tap("Discard interrupted recording", in: app)
        tap("Discard", in: app)
        XCTAssertTrue(control("Create custom workout", in: app).waitForExistence(timeout: 15))
    }

    private func control(_ label: String, in app: XCUIApplication) -> XCUIElement {
        app.descendants(matching: .any).matching(NSPredicate(format: "label == %@", label)).firstMatch
    }

    private func tap(
        _ label: String,
        in app: XCUIApplication,
        file: StaticString = #filePath,
        line: UInt = #line
    ) {
        let element = control(label, in: app)
        for _ in 0..<8 {
            if element.waitForExistence(timeout: 2), element.isHittable {
                element.tap()
                return
            }
            app.swipeUp()
        }
        XCTFail("Could not reach \(label)", file: file, line: line)
    }
}
