# 0.8.2-test1 verification — in progress

Baseline main ec6c3ef1e5e162f513ce7ace017a604863cec679 passed complete workflow40079947980
with204instrumentation tests EACH API28/35/36 and rolling development publication. PR10
and PR11are merged. BaselineAPI35APK46,402,613bytes.

Current feature version0.8.2-test1/code11;schema10,JSON7,raw2,complete1 unchanged.
Observed local:61,295domain checks/19suites,43schema,11Dreamsource/projection,
6release-note tests,20artassets/1,472,726bytes and14artwork-integrity fault tests pass.
Initial source30a619b workflow40085783355: Androidbuild/lint/instrumentation compilation
passed on allthree APIs;209instrumentation tests passed on API28 and35. API36 completed
209with one native-history selector failure. Logs exposed a cancelled-query readiness
race; a targeted fix and3controlled UI tests are pending the next fullrun, not a blindretry.
API35artifact12556114941 contains77realcaptures, fileintegrity verified and allfivecity/
dark/200%top+landmarkpanels visually reviewed. Review caught an oversized duplicate
Paris citytitle; presentation now keeps city short while retained course/edition stay
explicit. Fresh final-source captures are still required before acceptance/merge.
InitialAPI35APKartifact12556488666:47,960,610bytes(+1,557,997 /3.358%vsbaseline),
version0.8.2-test1/code11, ZIPCRC/binarymanifest checked; noINTERNETpermission.
Final-source APK size/CI/captures and postmerge publication remain pending.
No physicalGalaxyS24check is claimed.

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
