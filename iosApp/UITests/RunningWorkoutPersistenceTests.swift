import CoreLocation
import XCTest

final class RunningWorkoutPersistenceTests: XCTestCase {
    override func setUpWithError() throws {
        continueAfterFailure = false
        dismissLeftoverLocationPrompt()
    }

    /// A system location prompt that appears late on a loaded simulator can outlive the test that
    /// asked for it and cover the next test's taps. Answer any such leftover prompt first.
    private func dismissLeftoverLocationPrompt() {
        let springboard = XCUIApplication(bundleIdentifier: "com.apple.springboard")
        for answer in ["Allow While Using App", "Don't Allow"] {
            let button = springboard.buttons[answer]
            if button.waitForExistence(timeout: 1) {
                button.tap()
                return
            }
        }
    }

    func testAppleHealthRequiresAnExplicitReadChoice() {
        let app = XCUIApplication()
        app.launch()
        // A fresh install opens on the first-run choice, which has no Settings action; any tab
        // leaves it, after which Settings is in the top bar.
        tap("Progress", in: app)
        tap("Settings", in: app)
        // Settings groups its controls; Apple Health lives under Health and connected data.
        tap("Health and connected data", in: app)
        tap("Weight", in: app)

        let request = control("Request access and import", in: app)
        XCTAssertTrue(request.waitForExistence(timeout: 15))
        XCTAssertTrue(request.isEnabled)

        app.swipeDown()
        tap("Weight", in: app)
        XCTAssertTrue(request.exists)
        XCTAssertFalse(request.isEnabled)
    }

    func testGymNowExposesSharedRestAndWarmupControls() {
        let app = XCUIApplication()
        app.launch()
        let strengthTab = app.buttons["Strength"].firstMatch
        XCTAssertTrue(strengthTab.waitForExistence(timeout: 15))
        strengthTab.tap()
        // The Strength tab opens the shared dashboard; Gym Now is its primary action at the top.
        // iOS exposes only on-screen Compose elements, so wait for the first visible control.
        XCTAssertTrue(control("Gym now", in: app).waitForExistence(timeout: 15))
        attachScreenshot("ios-strength-dashboard", app)
        app.swipeUp()
        settle()
        attachScreenshot("ios-strength-dashboard-scrolled", app)
        app.swipeDown()
        app.swipeDown()
        settle()
        tap("Gym now", in: app)
        tap("Start open workout", in: app)
        tap("Add exercise", in: app)

        tap("Bench Press", in: app)

        tap("Timer settings ▾", in: app)
        XCTAssertTrue(reveal("Default rest when no target is prescribed", in: app))
        tap("Optional warm-up ▾", in: app)
        XCTAssertTrue(reveal("Enter my own warm-up", in: app))

        tap("Discard unfinished workout", in: app)
        tap("Discard workout", in: app)
        XCTAssertTrue(control("Workout discarded", in: app).waitForExistence(timeout: 15))
    }

    func testSharedNavigationOpensTrainingProgress() {
        let app = XCUIApplication()
        app.launch()
        tap("Progress", in: app)

        XCTAssertTrue(control("Distance this week", in: app).waitForExistence(timeout: 15))
        // Chart-first Progress: the selected week's runs follow the chart.
        XCTAssertTrue(reveal("Runs this week", in: app))
        attachScreenshot("ios-progress-dashboard", app)
        app.swipeUp()
        settle()
        attachScreenshot("ios-progress-dashboard-scrolled", app)
        tap("Running PBs", in: app)
        XCTAssertTrue(control("Record a result", in: app).waitForExistence(timeout: 15))

        tap("Progress", in: app)
        XCTAssertTrue(control("Distance this week", in: app).waitForExistence(timeout: 15))
        XCTAssertTrue(control("Record", in: app).isHittable)
        XCTAssertTrue(control("Settings", in: app).isHittable)
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
        // The system prompt can take well over 20 seconds to appear on a loaded CI simulator.
        XCTAssertTrue(permission.waitForExistence(timeout: 60))
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

    func testLiveCoachAdvancesATimedRecoveryWithoutGPSDistance() {
        let app = XCUIApplication()
        app.launch()
        if control("Record my runs", in: app).exists {
            tap("Record my runs", in: app)
        } else {
            tap("Record", in: app)
        }

        let permissionAction = control("Allow location access", in: app)
        if permissionAction.exists {
            permissionAction.tap()
            let allow = XCUIApplication(bundleIdentifier: "com.apple.springboard")
                .buttons["Allow While Using App"]
            XCTAssertTrue(allow.waitForExistence(timeout: 60))
            allow.tap()
        }
        tap("Create custom workout", in: app)
        tap("Add new group", in: app)
        tap("Save step", in: app)
        tap("Add step to group", in: app)
        tap("Recovery", in: app)
        // Compose exposes the outlined input's visible label in this accessibility tree.
        // Tap that label inside the input and send keys to the focused Compose editor.
        tap("Time (seconds)", in: app)
        app.typeText("30")
        tap("Save step", in: app)
        tap("Add step to group", in: app)
        tap("Save step", in: app)
        tap("Start custom workout", in: app)
        XCTAssertTrue(control("Step 1 of 3", in: app).waitForExistence(timeout: 20))
        tap("Complete open step", in: app)
        let recoveryStep = control("Step 2 of 3", in: app)
        // Completing an open step may leave the scroll position near the bottom of the panel.
        if !recoveryStep.exists {
            app.swipeDown()
            app.swipeDown()
        }
        XCTAssertTrue(recoveryStep.waitForExistence(timeout: 15), app.debugDescription)
        // With no simulated movement, advancing to the third step requires the active-time target.
        XCTAssertTrue(control("Step 3 of 3", in: app).waitForExistence(timeout: 60), app.debugDescription)
        tap("Pause", in: app)
        XCTAssertTrue(control("Resume", in: app).waitForExistence(timeout: 15))
        app.terminate()
        app.launch()
        tap("Record", in: app)
        XCTAssertTrue(control("Interrupted recording", in: app).waitForExistence(timeout: 20))
        tap("Discard interrupted recording", in: app)
        tap("Discard", in: app)
        XCTAssertTrue(control("Create custom workout", in: app).waitForExistence(timeout: 15))
    }

    private func verifySavedRoute(in app: XCUIApplication) {
        tap("Start recording", in: app)
        XCTAssertTrue(control("Pause", in: app).waitForExistence(timeout: 20))
        app.swipeDown()
        app.swipeDown()

        addTeardownBlock { XCUIDevice.shared.location = nil }
        // A loaded CI simulator can drop or delay the first simulated fix, for example when it is
        // set before location updates have started. "GPS ready" is also transient: the shared
        // RunMetricsEngine shows "GPS signal weak" once no usable fix has arrived for 5 seconds,
        // and a fix stamped before recording started is rejected as stale, so a fix cannot be
        // primed before Start. Main job 137696416489 saw "GPS ready" after one fix, then
        // re-waited 4 seconds later with no new fix and timed out on "GPS signal weak". Keep
        // fresh fixes arriving well inside that window (at most 45 fixes, about 2 minutes) while
        // waiting, and give one more fresh fix right before the unchanged readiness assertion.
        let gpsReady = control("GPS ready", in: app)
        var fixes = 0
        repeat {
            setSimulatedLocation(latitude: 51.5000)
            fixes += 1
        } while !gpsReady.waitForExistence(timeout: 2) && fixes < 45
        setSimulatedLocation(latitude: 51.5000)
        XCTAssertTrue(gpsReady.waitForExistence(timeout: 20))
        let measuredDistance = app.descendants(matching: .any)
            .matching(NSPredicate(format: "label CONTAINS %@", "0.1 km")).firstMatch
        // The fixture walks about 11 metres every two seconds until the recorded distance shows
        // 0.1 km. A loaded CI simulator can deliver location updates late, so the walk continues
        // (up to 30 steps) instead of assuming exactly seven updates are enough.
        var index = 1
        while !measuredDistance.exists && index <= 30 {
            Thread.sleep(forTimeInterval: 2)
            setSimulatedLocation(latitude: 51.5000 + Double(index) * 0.0001)
            index += 1
        }
        XCTAssertTrue(measuredDistance.waitForExistence(timeout: 20))
        tap("Finish & save", in: app)
        tap("Save recorded run", in: app)
        // D-4: the shared run analysis card is on the iOS run detail (an unplanned run).
        XCTAssertTrue(reveal("Run Analysis", in: app))
        tap("View recorded route", in: app)
        XCTAssertTrue(control("Your recorded route", in: app).waitForExistence(timeout: 20))
        tap("Export recorded route (GPX)", in: app)
        guard waitForFilesSave(in: app) != nil else { return }
        let cancel = control("Cancel", in: app)
        if cancel.exists && cancel.frame.width > 1 && cancel.isHittable {
            cancel.tap()
        } else {
            // Files can open directly in On My iPhone, with Save but no Cancel.
            let sheetHeader = app.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.09))
            let belowSheet = app.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.84))
            sheetHeader.press(forDuration: 0.05, thenDragTo: belowSheet)
        }
        XCTAssertTrue(control("GPX export cancelled.", in: app).waitForExistence(timeout: 20))

        tap("Export recorded route (GPX)", in: app)
        guard let save = waitForFilesSave(in: app) else { return }
        save.tap()
        // On a loaded CI runner the Files picker can still be settling when Save is tapped, so the
        // tap is ignored and the picker stays open. Tap Save again only while it is still present;
        // the saved confirmation itself is still required.
        let saved = control("GPX file saved.", in: app)
        var retries = 0
        while !saved.waitForExistence(timeout: 20) && retries < 2 {
            guard save.exists && save.frame.width > 1 && save.isHittable else { break }
            save.tap()
            retries += 1
        }
        XCTAssertTrue(saved.waitForExistence(timeout: 20))
    }

    private func waitForFilesSave(in app: XCUIApplication) -> XCUIElement? {
        // Wait for a real Files control after the provider has populated the picker.
        let action = control("Save", in: app)
        let ready = XCTNSPredicateExpectation(
            predicate: NSPredicate { _, _ in
                action.exists && action.frame.width > 1 && action.isHittable
            },
            object: nil
        )
        // The system Files picker can take well over a minute to populate on a loaded CI runner.
        guard XCTWaiter.wait(for: [ready], timeout: 120) == .completed else {
            XCTFail("The native Files picker did not expose an actionable Save control.")
            return nil
        }
        return action
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

    /// Review evidence for the shared dashboards; synthetic simulator data only.
    private func attachScreenshot(_ name: String, _ app: XCUIApplication) {
        let attachment = XCTAttachment(screenshot: app.screenshot())
        attachment.name = name
        attachment.lifetime = .keepAlways
        add(attachment)
    }

    /// Expanded content can open below the visible area, and iOS exposes only on-screen Compose
    /// elements, so scroll gently until the control appears.
    private func reveal(_ label: String, in app: XCUIApplication) -> Bool {
        let element = control(label, in: app)
        if element.waitForExistence(timeout: 15) { return true }
        for _ in 0..<4 {
            app.swipeUp()
            settle()
            if element.waitForExistence(timeout: 3) { return true }
        }
        return false
    }

    /// A tap during a Compose scroll fling only stops the fling, so let a swipe finish first.
    private func settle() {
        Thread.sleep(forTimeInterval: 1.0)
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
        for attempt in 0..<8 {
            // On a loaded CI simulator the screen opened by the previous tap can take several
            // seconds to appear. Scrolling before it does swipes the old screen, or scrolls the
            // new screen's control out of view, so allow the first look to wait for the transition.
            if element.waitForExistence(timeout: attempt == 0 ? 10 : 2), element.isHittable {
                element.tap()
                return
            }
            app.swipeUp()
            settle()
        }
        XCTFail("Could not reach \(label)", file: file, line: line)
    }
}
