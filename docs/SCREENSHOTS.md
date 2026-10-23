# Authentic app screenshots

Canonical images are captured by `CoachingScreenshotTest` from the real
`CoachActivity`, navigation and production screen composables. Fixture builders
exist only in `app/src/androidTest`: neither test data nor a demo-seeding entry
point is compiled into the app APK. Setup/teardown deletes the isolated test
profile, cancels alarms and clears the test database.

The fixture fixes record IDs, exercise sets and training evidence. Dates are
relative to the capture day so the actual Today screen remains meaningful; the
canonical image is a dated build capture, not a daily-changing golden assertion.
Canonical captures use the CI API-35 emulator at 540×1200 pixels / 210 dpi (approximately 411×914 dp). Normal captures use the Android defaults; `home-dark.png` and `home-large-font.png` additionally verify actual dark mode and 200% font scale. No stock image, generated
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
`feature/0.8.1-design-guidance-reliability` PR branch and cannot write main. A changed
screenshot-source fingerprint (including rendering workflow settings) causes recapture; an unchanged digest avoids
commit loops. `source-commit.txt` identifies the tested source commit.

Review images after a material screen change. Other branches must supply an up-to-date source fingerprint before CI proceeds. The digest detects source changes,
not visual correctness; it is not pixel-difference approval or physical-device
validation. CI bot screenshot commits may not run automatically or may require GitHub workflow approval. A subsequent reviewed documentation/source commit triggers
full verification of the resulting branch head. No claim is made that the images
are always pixel-identical across Android releases or wall-clock dates.


## 0.5 canonical capture set

34 real-build captures cover Home (normal/dark/200% font), Plan/workout detail,
Calendar List/Week/Month, activity detail/comparison/feedback, Progress, running PBs,
training trends, body weight, Strength, PB progression, exercise search, RDL detail
and technique, Bulgarian Split Squat detail, previous/timed/carry logging, gym setup,
running-support and strength-first blocks, gym workout detail, filtered history,
onboarding, settings/Health Connect and shoes. File names are listed by the capture
fixture and in `docs/screenshots`; `source-commit.txt` records their verified source.

The 0.6 fixture includes `health-context.png` and `record-run.png`. Health observations are explicitly test-only cache fixtures; the recording capture shows the ready screen, not fabricated live GNSS.

0.7 captures add warm-up, optional mobility, race preparation, a stored Dream equivalent
and the actual active-run panel. The latter receives a deterministic execution-engine
trace through test-only StateFlows; no such seeding path is compiled into production.
These captures do not establish physical GNSS, BLE, TTS or music behaviour. Test teardown
always clears injected recording state, including when a capture fails.

## 0.8 canonical set

43 authentic captures include Run Now and native post-run route presentation. README
curates eight current-build images; QA captures retain Calendar, dark/200%, guides,
gym and settings. Public Live Coach uses neutral fixture wording; its production UI
is unchanged. Actual native trace analysis uses deterministic androidTest-only input,
not a mock route widget. No test data enters the installable app.

## 0.8.1 design fidelity gate (in progress)

Owner-supplied reference JPEGs are preserved in `docs/design-reference/`. They are
never bundled as app UI or presented as actual screenshots. Required actual captures
include Plan, Dream, RDL, Bench, Walking Lunge, Ankle Rocks, a static stretch,
machine/cable guide, mobility, Live Coach, health, shoes, trends, Run Now, Calendar,
dark mode and200% text. Keep all images from the exact verified production source.

Compare actual renders side by side with references: week-first density, meaningful
route geography/highlight/progress, human illustration proportions/equipment/phases,
blue/navy hierarchy, mint tips/pink mistakes, touch targets, dark contrast and large
text. Document specific mismatches and refine. Hash equality/regression consistency
alone does not pass this gate. Current committed PNGs still describe0.8 until the
0.8.1 capture/review/adoption work completes; do not market them as the final redesign.

## 0.8.1 observed checkpoint

Source8aadbf9e3c677f478cf9196244fe7d78323998d2 /workflow39986988491 produced50
actual build PNGs. They are retained byte-for-byte with the source fingerprint.
Plan, Bench, Ankle Rocks, mobility-dark and Plan200%-dark were visually inspected.
Routine layout and recognisable human artwork now match the reference family;
Plan density and complete catalogue/Dream geography acceptance still remain open.
These captures predate the later seven canonical assets/geographic Berlin changes.
The next fixture requests53captures including BSSHowTo, cable row and plank.
No design JPEG is presented as an actual app capture or substituted for one.

Sourcef55f346404d02ed5a30aa44f108b894b62816c33 subsequently passed all198tests/API
and produced53captures (workflow40001032343). Those exact PNGs are now adopted.
Reviewed compact Plan, geographicBerlin and cable-row guide; long phase captions
needed explicit spacing, now fixed for next capture. The Berlin source is geographic,
not final visual-reference approval. New four canonical assets also need fresh capture.

A concrete reference-versus-build review is recorded in
[0.8.1 visual review](VISUAL_REVIEW_0_8_1.md). Open gates remain explicit there.

## Continued guidance capture request

The previous follow-up workflow40005151891 produced53captures adopted byte-for-byte
from bota8cbf6b; source c04143d. Current source adds five actual navigation captures:
Front Squat, Leg Press, Lat Pulldown, Seated Leg Curl and Farmer’s Carry (58total).
These will verify machine/squat/distance guide presentation in addition to the existing
RDL/Bench/BSS/routine/light/dark/200% family. They must be regenerated and visually
reviewed, not substituted with generated artwork or the design mockups. Full approved
reference fidelity remains an open mandatory gate; catalogue coverage is still47/174.
