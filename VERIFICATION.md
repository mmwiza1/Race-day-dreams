# 0.9 architecture checkpoint — in progress

Live baseline main `8b75755a0b74074047408b6444a11db3d6fef6f3` was fetched before edits;
feature branch is `feature/strength-engine-2-architecture-hardening`. Baseline workflow
40330082255 is green on API28/35/36. Local baseline:61,295domain checks/19suites,
43schema,9release-note,11Dream-source,14artwork-integrity and6screenshot-integrity tests
passed; strict174/174canonical+14/14routine resources and committed capture hash matched.

Phase1 implementation adds feature coordinators/shared operation runner, scoped SQL
repositories, flat feature navigation and internal recording coordination. Local checks
now pass61,369domain checks/20suites (74new recording/cue checks),43schema and9release-note
tests. Nine offline formatter-harness tests pass; actual formatter execution is pending
CI. Independent review found/fixed one timezone-capture regression and found no remaining
concrete semantic blocker; review does not replace Android build/instrumentation.

Local Gradle failed before compilation while downloading its distribution (Java connection
refused). No Android SDK/adb exists. One network-enabled tool capability probe was
interrupted before execution; no SDK download or TLS workaround occurred. Android
compile/lint/API28/35/36 tests and fresh captures must run in GitHub CI. No unperformed
Android or physical result is claimed. All GalaxyS24 acceptance remains UNPERFORMED.

Target0.9.0-test1/code12;schema10/JSON7/raw2/complete1 unchanged in this phase.
The full Strength Engine2.0 product phases are pending, listed in CONTINUE_CODEX.md.
Do not merge the draft release before complete scope and all verification gates pass.

---

# Historical 0.8.2 verification

# Verification — Race Day Dreams 0.8.2-test1

Version **0.8.2-test1 / versionCode 11**. Schema **10**, JSON envelope **7**, raw
recording archive **2**, complete-backup package **1**: unchanged. No dependency or
manifest permission was added. This remains a debug-signed test/development build.

## Verified source and CI

Verified follow-up source: `57c1044ea5eb372090f4c300ee6f660732ec7deb`.
[Complete workflow 40176513799](https://github.com/mmwiza1/Race-day-dreams/actions/runs/40176513799)
passed Android build, lint, instrumentation compilation, all supported emulator APIs,
strict instructional coverage and canonical screenshot publication.

| API | Instrumentation result | Job |
| --- | --- | --- |
| 28 | 213 passed, zero failed/skipped | 121118246169 |
| 35 | 213 passed, zero failed/skipped | 121118246131 |
| 36 | 213 passed, zero failed/skipped | 121118246110 |

Exact `Finished 213 tests` and successful-build log lines were inspected for all three.
These include migrations, complete-backup round trips/rollback, retained current and
legacy Dream assignments, history paging/recovery, new resources/edition identity,
controlled cancelled-query/retry/cursor tests and post-snapshot completion callbacks.
No original test or assertion was removed; no timeout was increased.

Observed local checks: **61,295 domain checks across 19 suites**, **43 schema tests**,
**11 Dream source/provenance/projection tests**, all course/context/basemap reproducers,
**6 release-note tests**, **14 artwork-integrity tests**, **6 PNG/capture-integrity tests**.
Strict guidance remains **174/174 canonical + 14/14 routine movements**, with all 177
existing assets intact. The 20 new city assets total **1,472,726 bytes**.

## Authentic screenshots and design gate

API-35 artifact **12593011391** contains **77 real PNGs**, adopted exactly by bot
`22de99c91b4483ffb681d71151b9a85c0a65a6b9`. Stored source commit is 57c1044; fingerprint:
`db25ccc7a930e1f40a3885ef24091b905973641850d4784ced9888b1657835b0`.
The current source hash matches. All 77 required PNG structures/checksums/pixel streams
validate. Five city top/landmark pairs plus dark and 200% pairs are separate actual
captures, not composites. Reference comparison is recorded in
[VISUAL_REVIEW_0_8_2.md](docs/VISUAL_REVIEW_0_8_2.md); the approved reference is not replaced
with production captures. Physical human/device acceptance is a separate gate.

## APK and privacy

Verified source API-35 artifact **12592851563**:
`Race-Day-Dreams-0.8.2-test1-debug-api-35`, containing
`Race-Day-Dreams-0.8.2-test1-debug.apk`. Build log size: **47,976,994 bytes**.
Merged 0.8.1 baseline: **46,402,613 bytes**; increase **1,574,381 bytes / 3.393%**.
Twenty bounded WebPs account for 1,472,726 bytes. Maps remain vector coordinates.

The manifest and resolved dependencies are unchanged apart from app version; the
independently inspected initial APK binary had no INTERNET permission. Source checks
confirm no network permission, map SDK, analytics/backend/account or remote artwork
path was introduced. Final-main publication supplies its own APK/checksum and CI URL.
No permanent stable/production tag is created by this update.

## Reliability findings resolved during verification

Initial run 40085783355 found cancelled history IO could mark a replacement page ready.
The targeted readiness fix retains summary-only keyset queries. Follow-up 40086946911
passed the original city capture on all APIs, but exposed two new test-harness errors
(frame clock pumping and offscreen lazy scrolling) and an API-28 completion-feedback
wait. Harness operations were corrected without weakening assertions. Manual completion
now delivers navigation after its coherent snapshot; a real ViewModel regression test
verifies Main-thread callback, idle state, actual, response and preserved prescription.
The complete 213-test matrix above passed after these corrections, without blind retries.

## Post-merge capture follow-up

PR #12 merged at `5ffa63029897365c84aeacd92b75f3339a935462` after final-head
workflow 40138918269 passed 213 tests on each API. Main workflow 40139578803 passed
API 28/35, but API 36 timed out waiting for asynchronous Route story evidence;
publication correctly remained blocked. PR #13 explicitly waits for and scrolls to
Route story and Long-run durability before the same two 20-second evidence assertions.
It verifies retained sample/story/durability preconditions and adds failure diagnostics.
No assertion, timeout, production code, geometry or data format was weakened or changed.
The full follow-up matrix above passed without rerunning a failed job. All 77 captures
were freshly adopted and all 14 Dream panels reviewed again, including dark/200% modes.

## Delivery and physical status

PR #12 contains the release; PR #13 contains only this capture synchronisation follow-up
and refreshed evidence. Require complete final PR-head CI and successful post-merge
main publication before claiming a downloadable 0.8.2 development release. The existing rolling
[Latest Development Build](https://github.com/mmwiza1/Race-day-dreams/releases/tag/development)
updates only after main verification. Its notes identify the actual merged commit.

Galaxy S24 outdoor GNSS/splits, long locked/background runs, real Health Connect data,
BLE disconnect/reconnect, TTS/music/calls, battery, sunlight, TalkBack and full physical
backup/restore acceptance remain **UNPERFORMED**. TEST_BUILD.md is the human checklist.
Local Android execution was unavailable: default Gradle cache was unwritable; a
workspace-cache attempt reached wrapper download but Java network access was refused,
and no Android SDK is installed. Android results above are observed GitHub CI results.

---

The following records are HISTORICAL0.8.1 results, not0.8.2 verification.

---

# Final0.8.1 native-capture follow-up —24October2026

PR10 merged at3fe6c6d93d261cd024fa71573553cfaf7c32d502. Postmerge workflow40077045374
passed API28/35, while API36 completed204tests with one native city-capture wait timeout.
The test selected a generic Recordedrun card shared by multiple fixtures. Publication
was correctly blocked. PR11selects the unique native recording-source card and asserts
its provenance before both existing analysis waits. No production code or timeout/
assertion was removed or altered; all existing gates remain required.

Source04cd097535e0ec80040c82799db4b87a286c2ab3 passed complete workflow40078273456:
**204instrumentation tests EACH API28/35/36**, build/lint/domain and strict174/174canonical
plus14/14routine guidance. Exact Finished204lines inspected in jobs120826251504,
120826251535,120826251368. All70actualAPI35captures adopted via bot0cd77bd; fingerprint
`985d5b81c46f8aef4a8bd696ac8912bec3f4916c681a80b493c449adec8ad894` matches source.
The refreshed Paris screenshot was inspected: annualcourse, equivalent section, markers,
percentage/distance and geographic context remain clear, without label overlap.

The runtime source/data are unchanged from PR10. Version0.8.1-test1/code10,
schema10/JSON7/raw2/complete1; no dependency or permission added. No physicalGalaxyS24
checks are claimed. Finalmainworkflow40079947980and rolling development publication succeeded. This remains a test/development build.

## Detailed Paris/source verification history

# Verification — 0.8.1-test1, 24 October 2026

Version **0.8.1-test1 / versionCode 10**. Schema **10**, JSON envelope **7**, raw
recording archive **2**, complete-backup package **1**. The Paris correction adds
no schema/backup change, runtime dependency or manifest permission.

## Annual Paris course correction

New assignments use `paris-2026-geo-v1`: **Schneider Electric Marathon de Paris,
12 April 2026**, the annual race. Factual streets come from police order2026-00388,
3April2026, articles16,18–20,22. The ASO2026guide corroborates the start near Rue de
Marignan and finish near Rue Spontini on Avenue Foch. No organiser map/stroke copied.

Geometry uses the Geofabrik22October2026 Île-de-France OSM extract (ODbL-1.0).
All1960positions are actual retained OSM nodes;550source ways retain IDs/versions,
tags, adjacency and course order. URLs, SHA256s, checked date, transformation and
limitations are retained in docs/dream-route-data/paris-2026*.json. Approximate
street/junction line43.22km is distinct from42195m marathon-equivalent progress;
not a certified racing line or navigation. The2027course was not yet published.

The former Olympic ID, its328positions, original frame/context/notice and old v2
schematic remain immutable solely for already-saved assignments. New Paris runs
never select that event. Complete-backup tests cover new annual and both legacy
assignments without rerolling offsets or relabelling history.

## Automated verification

Production correction source **e5a089b27f7eb2b6281d3fa8e53259422f75bd00**:
[workflow40046791736](https://github.com/mmwiza1/Race-day-dreams/actions/runs/40046791736).
**Complete workflow passed on API28,35,36**, including domain/build/lint,
instrumentation compilation/execution, strict guidance and canonical publication.
**204 instrumentation tests passed on each API28/35/36**; exact Finished204lines
were inspected in jobs120733014624/120733015682/120733016173.
Attempt1API28 failed before executing any test: offline emulator / APK install-write
failure, Finished0tests. After log diagnosis, only failed jobs/dependants were retried
on a fresh runner; attempt2passed. No test/assertion was removed or bypassed.

Observed local results:

- **61,265 domain checks across19 suites**.
- **43 schema/migration tests**, including supported upgrade paths/rollback.
- **11 source-format/projection/provenance tests**, including every annual Paris
  coordinate and consecutive edge matching retained OSM source nodes/ways.
- **5 release-note tests**, all course projection checks (including legacy Paris),
  **18 retained landmark anchors** and **6 offline basemap reproductions**.
- **174/174 canonical exercises**, **14/14 routine movements**, **177 original
  artwork assets /9,198,660bytes**; hashes/content/provenance checks pass.
- Workflow/release YAML parses, bundled notices match, full diff whitespace passes.

Android checks run in GitHub CI: :domain:check, :app:assembleDebug, :app:lintDebug,
:app:assembleDebugAndroidTest and :app:connectedDebugAndroidTest. No local Android
SDK or physical-device build/test execution is claimed.

## Actual-build captures and review

**70 authentic API35 PNGs**, source fingerprint
`6be17eb492113a693980488aff8fadf463e8a69093571d21489a878d51d9fc5f`.
Exact canonical bot commit **4916db3ed0b5488257ea2cfa0b7258ea94f59172** was fast-forwarded
locally. All70PNG SHA256s match the API35 artifact12542101120 byte-for-byte; the
fingerprint above equals the current production-source fingerprint.

Paris was visually inspected: the annual course covers Opéra/Nation/Vincennes,
Seine and the western return, rather than Versailles. Full course, highlighted
section, start/finish,5km labels, context,17% and7.19km/42.20km are visible.
No clipping/label overlap was observed. London/Berlin/Tokyo/NewYork and representative
dark/200%city captures were also inspected. The approved JPEG references remain
untouched and separate; no design image is substituted for a build capture.

The accepted illustrated guides/Plan/LiveCoach/health/shoes/trends/review/RunNow/
Calendar/history/recorder implementation was not redesigned by this correction.
Strict coverage remains complete. Visual review is not physical GNSS, sunlight,
TalkBack, medical/form assessment or sensor verification.

## Verified API35 correction APK

Artifact **12541633836**, `Race-Day-Dreams-0.8.1-test1-debug-api-35`, workflow40046791736.
Local file: `build/distributions/Race-Day-Dreams-0.8.1-test1-debug.apk`.
**46,402,613bytes**, SHA256
`c22a9017f6806db892ab59e7acb058b6f95066a05c0333e633b94dfd14bffbc4`.
ZIPCRC, binary manifest identity and permissions inspected: org.racedaydreams,
0.8.1-test1/code10, **no INTERNET permission**. Debug/test signing, not production.
Increase versus accepted pre-correction0.8.1APK:395,643bytes (0.86%).

A successful main workflow publishes its freshly built API35 APK through the existing
rolling [Latest Development Build](https://github.com/mmwiza1/Race-day-dreams/releases/tag/development).
That main artifact/release is canonical after merge; independently built/debug-signed
APKs can have different binary checksums. No permanent version/stable tag is created
solely by this merge. Final main workflow/SHA/publication must be inspected directly.

## Physical Galaxy S24 status

**Unperformed.** TEST_BUILD.md retains outdoor GPS/1km genuine split, locked screen,
backgrounding, long run/battery, pause/resume, GPS loss/return, interruption recovery,
real HealthConnect providers/denial/revocation, BLE if hardware exists, TTS over music/
audio focus/calls, GPX, complete backup/restore, TalkBack and200%font acceptance.
The owner explicitly authorises merge after the complete automated correction gate;
physical acceptance is not a claimed pass or a merge blocker. This remains a test/
development build until that real-device acceptance is completed.

Prior accepted verification history is retained separately in
[the pre-correction report](docs/verification-history/0.8.1-pre-paris-correction.md).
