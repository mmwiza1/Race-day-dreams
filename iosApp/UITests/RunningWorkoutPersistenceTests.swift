import CoreLocation
import XCTest

final class RunningWorkoutPersistenceTests: XCTestCase {
    override func setUpWithError() throws {
        continueAfterFailure = false
    }

    func testAppleHealthRequiresAnExplicitReadChoice() {
        let app = XCUIApplication()
        app.launch()
        tap("Settings", in: app)
        tap("Weight", in: app)

        let request = control("Request access and import", in: app)
        XCTAssertTrue(request.waitForExistence(timeout: 15))
        XCTAssertTrue(request.isEnabled)

        app.swipeDown()
        tap("Weight", in: app)
        XCTAssertTrue(request.exists)
        XCTAssertFalse(request.isEnabled)
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

        verifySavedRoute(in: app)
    }

    private func verifySavedRoute(in app: XCUIApplication) {
        tap("Start recording", in: app)
        XCTAssertTrue(control("Pause", in: app).waitForExistence(timeout: 20))
        app.swipeDown()
        app.swipeDown()

        addTeardownBlock { XCUIDevice.shared.location = nil }
        setSimulatedLocation(latitude: 51.5000)
        XCTAssertTrue(control("GPS ready", in: app).waitForExistence(timeout: 20))
        for index in 1...7 {
            // The fixture moves approximately 11 metres every two seconds.
            Thread.sleep(forTimeInterval: 2)
            setSimulatedLocation(latitude: 51.5000 + Double(index) * 0.0001)
        }
        let measuredDistance = app.descendants(matching: .any)
            .matching(NSPredicate(format: "label CONTAINS %@", "0.1 km")).firstMatch
        XCTAssertTrue(measuredDistance.waitForExistence(timeout: 20))
        tap("Finish & save", in: app)
        tap("Save recorded run", in: app)
        tap("View recorded route", in: app)
        XCTAssertTrue(control("Your recorded route", in: app).waitForExistence(timeout: 20))
        tap("Export recorded route (GPX)", in: app)
        let cancelExport = app.buttons["Cancel"].firstMatch
        XCTAssertTrue(cancelExport.waitForExistence(timeout: 20))
        cancelExport.tap()
        XCTAssertTrue(control("GPX export cancelled.", in: app).waitForExistence(timeout: 20))
    }

    private func setSimulatedLocation(latitude: CLLocationDegrees) {
        XCUIDevice.shared.location = XCUILocation(location: CLLocation(
            coordinate: CLLocationCoordinate2D(latitude: latitude, longitude: -0.1246),
            altitude: 12,
            horizontalAccuracy: 3,
            verticalAccuracy: 5,
            timestamp: Date()
        ))
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
