# Authentic app screenshots

Canonical images are captured by `CoachingScreenshotTest` from the real
`CoachActivity`, navigation and production screen composables. Fixture builders
exist only in `app/src/androidTest`: neither test data nor a demo-seeding entry
point is compiled into the app APK. Setup/teardown deletes the isolated test
profile, cancels alarms and clears the test database.

The fixture fixes record IDs, exercise sets and training evidence. Dates are
relative to the capture day so the actual Today screen remains meaningful; the
canonical image is a dated build capture, not a daily-changing golden assertion.
Canonical captures use the CI API-35 emulator at 540×1200 pixels / 210 dpi (approximately 411×914 dp). Fonts use the Android defaults. No stock image, generated
mockup or manually composited UI is used.

Run locally with an emulator:

```sh
./gradlew :app:connectedDebugAndroidTest
adb pull /data/local/tmp/racedaydreams-screenshots/. docs/screenshots/
python3 scripts/screenshot-source.py > docs/screenshots/source.sha256
```

`UiAutomation.takeScreenshot` is provided by Android; no new screenshot dependency
is required. Captures are copied with the instrumentation shell identity to a
temporary emulator directory before AGP uninstalls test apps. That shell capability
is not available to or used by production code.

CI tests API 28/35/36, uploads each API's captures and the named APK **after tests
pass**, then publishes canonical API-35 PNGs only when all matrix jobs succeed.
The publisher is restricted to the same-repository
`codex/weekly-coach-and-simple-ux` PR branch and cannot write main. A changed
screenshot-source fingerprint (including rendering workflow settings) causes recapture; an unchanged digest avoids
commit loops. `source-commit.txt` identifies the tested source commit.

Review images after a material screen change. Other branches must supply an up-to-date source fingerprint before CI proceeds. The digest detects source changes,
not visual correctness; it is not pixel-difference approval or physical-device
validation. CI bot screenshot commits may not run automatically or may require GitHub workflow approval. A subsequent reviewed documentation/source commit triggers
full verification of the resulting branch head. No claim is made that the images
are always pixel-identical across Android releases or wall-clock dates.
