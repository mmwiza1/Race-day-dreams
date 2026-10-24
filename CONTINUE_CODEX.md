# Continue Race Day Dreams 0.8.1 — verified code, physical acceptance pending

Preserve this branch and every completed commit. Inspect actual git status/log/diff,
PR #10 and live CI before trusting these notes. Do not reset/recreate the work.

## Repository and delivery state

- Branch: `feature/0.8.1-design-guidance-reliability`.
- PR: https://github.com/mmwiza1/Race-day-dreams/pull/10 . **KEEP DRAFT** until the
  full definition of done, including required physical acceptance, is satisfied.
  No merge, permanent tag or release has been published.
- Main last fetched: `515fe538de1302dd83ee05fc9ca131c7afb7d520`.
- Version `0.8.1-test1`, versionCode10, schema10; JSON7/raw2/complete1.
- Final verified production source: **109394a1046db2bf6d4c9fd203b8b761f02edd01**.
- Exact canonical screenshot bot: **7ed12d8edc429671ea4ec1cf5e6a2329c424c48f**,
  fast-forwarded locally without replacing any previous work. A subsequent
  documentation-only checkpoint may be HEAD; inspect git log. Production unchanged.

## Completed — do not redo

- **174/174 canonical exercises and14/14 routine movements** have explicit,
  appropriate original illustrated guidance, movement-specific instructions,
  cues/mistakes/equipment/accessible descriptions. No missing canonical ID.
  **177 local WebPs /9,198,660bytes**; strict content/resource/provenance audit passes.
  Primitive canonical fallback removed. Actual RDL/Bench/Lunge/Ankle/static/cable
  and mobility captures reviewed against untouched owner references.
- Week-first Plan; programme metadata disclosed; compact Calendar key;
  summary-first Health/Shoes/WeeklyReview/RunNow; real weekly-distance chart;
  Live Coach range using existing smoothed evidence and authoritative engine.
- SQL keyset metadata history paging, schema10 additive indexes. Tests include
  2000activities/100000unreadable raw points to prove lists avoid GPS hydration;
  mixed IDs/date/type/alias filtering and preserved-plan categories are protected.
- Atomic staged recording start/origin/exact prescription; idempotent batch retry;
  bounded incremental flush; honest durable partial/paused recovery. No secret
  post-reboot tracking or fabricated continuity. Existing backup formats preserved.
- Version-tag and rolling-development GitHub prerelease workflows gate full matrix,
  guidance and current captures; debug signing is explicit, permanent releases not
  overwritten. Release notes/configuration tests pass. No premature publication.

## Dream Routes — five sourced geographic revisions and full offline presentation

Only future assignments select the current revisions. Old course IDs, coordinates,
preferences, offsets and saved assignments remain immutable/readable in backups.
- London historical MIT line,164positions; edition/survey accuracy unspecified.
  Publication2023 is not a claimed race edition.
- Berlin2014 MIT participant trace,191retained positions.
- Tokyo2026 CC0 participant trace,91retained original positions from10872.
- NYC2026 ODbL MARATHON TRIP road-centreline data,371XYpositions, documented UTM18
  frame/anchor; explicit data offer, retained source hash and full notices.
- Paris2024 Olympic course,328ODbL ThomasKrakow/Running-tomy Commons positions.
  **Primary name explicitly says2024Olympic course, NOT annual ParisMarathon.**
  Annual-course redistribution rights were not established; this is the documented
  safe geographic fallback, not hidden or mislabelled official geometry.

Original full-course/section/start/finish/5km/percentage/distance presentation now
includes original-styled sourced streets, water and green space.67frozen OpenFreeMap
source extracts /10,342,383bytes in development; only selected ODbL DATA ship.
OpenFreeMap expressly permits bounded offline downloads under its /tos/ policy;
3parallel requests and identifying User-Agent used. No downloaded styles/images,
fonts/icons, third-party software, runtime tile client/map SDK or Internet permission.
Original reader/projection/styling MIT, retained/projected databases separately ODbL.
Visible credit and full notices are available offline in Settings.

Source provenance/URLs/SHA256s/coordinates: docs/dream-route-data/basemap-*.json.
Bundled compact extracts: app/src/main/assets/dream-basemaps/*.rddmap,
**235,998positions /4,012,113bytes** before APK compression. Bounds cover the whole
fitted5:4 course-plus-landmark viewport +4%margin; earlier abrupt map edges are fixed.
15sourced landmark anchors (12WikidataCC0,3NYODbL), original city vignettes, correct
NYsuspension/cantilever silhouettes and NaturalEarth public-domain rivers retained.

DreamBasemap bounds bytes/features/points and rejects invalid format/frame/NaN/
unclosed polygons. DreamBasemapArtwork loads/parses onIO, prepares paths onDefault,
caches immutable data for five cities; redraw uses three cached context paths.
Latest label-placement fix separates Tokyo35km/40km text without moving markers.
Actual final Tokyo/London/dark/200% captures confirm the fix and clear map context.
The reviewed presentation follows the supplied design family; see VISUAL_REVIEW.

## Actual verification

**Workflow40042190130 is FULL GREEN**, source109394a:
https://github.com/mmwiza1/Race-day-dreams/actions/runs/40042190130
- Android build/lint/domain and **204instrumentation tests on EACH API28/35/36**.
- Jobs28/35/36:120715596416 /120715596443 /120715596294; actual Finished204 logs read.
- Strict174/174canonical +14/14routines and177asset checksums/provenance.
- **70 actual PNGs**, exact bot7ed12d8. Source fingerprint:
  `e7452da5a423bb8d660eaa0625543a4cb64030bce549d070366e543d99c5bb8b`
  matches production source. Original JPEG references unchanged/separate.
- Local **61,263domain checks /19suites**,43schema,5release-note and10source-format/
  projection tests; all5course +15landmark/river +5basemap reproductions;
  YAML/notices comparison/full branch whitespace checks pass.
- No local AndroidSDK build or physical-device result claimed: Android evidence
  comes from the actual GitHub matrix. Source manifest/dependency diff reviewed;
  binary APK permission list inspected: **NO INTERNET permission**.

## Verified install APK

Exact file: `build/distributions/Race-Day-Dreams-0.8.1-test1-debug.apk`.
API35artifact **12540415451**, run40042190130,
name `Race-Day-Dreams-0.8.1-test1-debug-api-35`.
**46,006,970bytes**, SHA256
`3f1a2d958f9421e8abf34fdb9b01e0ffe19c74abc2e47390391d5729177a2ff6`.
ZIPCRC and binary manifest verified: org.racedaydreams /0.8.1-test1 /code10.
Build sidecar `build/distributions/APK_VERIFICATION.json` records source/artifact.
Verified0.8.0/code9 baseline34,566,254bytes: increase11,440,716bytes /33.10%.
Debug/test signing only; preserve complete backup before any required uninstall.

## Remaining work / next action

1. Inspect latest documentation-only head's CI. It must preserve the fingerprint
   above and not rerender/replace exact verified PNGs unnecessarily. Diagnose any
   actual failure; no blind reruns, test removal or safeguard weakening.
2. **Physical GalaxyS24 acceptance is UNPERFORMED.** TEST_BUILD.md has the complete
   recording/locked/background/1km split/long-run/GPS loss/HC permissions/providers/
   BLE/TTS-over-music/audio-focus/battery/recovery/complete-backup/TalkBack/200% list.
   A meaningful real outdoor run is required before locking the Android baseline.
   The container/CI cannot perform these checks. Do not tick them from emulator tests.
3. Keep PR#10 draft while that acceptance remains open. After actual results arrive,
   address genuine defects and update verification/release notes, then follow the
   authorised review/CI/release workflow. Do not invent a physical pass or publish
   an incomplete permanent release. No further feature expansion is needed here.

## Useful tools / reproducibility

`sh scripts/test-domain.sh`; `python3 scripts/test-schema.py`;
`python3 scripts/test-release-notes.py`; `python3 scripts/test-dream-map-source.py`;
all generate-*-geometry.py, generate-dream-context.py, generate-dream-basemaps.py
with --check; verify-guidance-assets.py; strict ExerciseGuidanceAudit.java.
Development source refresh: prepare-dream-basemaps.py --cache PATH (offline unless
explicit --fetch). Existing cache /workspace/scratch/openfreemap-context. No network
source refresh belongs in the app or CI. Unlicensed/GPL geometry candidates excluded.

Git fetch/push work normally with scoped network permission. gh pr edit fails on
GitHub's deprecated classic projectCards; use REST PATCH with structured JSON.
Artifact download via gh can redirect403; github_download_workflow_artifact returns
signed downloadURL, then curl with proper singlequote/scoped network. Do not print
URLs/tokens or use download_file (FileService is not Sediment). Validate archive
bounds/CRC, manifest, bytes and hash. Verification-only manifest readers and downloaded
ZIPs are in /workspace/scratch, not production code. Never discard completed work.
