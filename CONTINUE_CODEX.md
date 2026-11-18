# Race Day Dreams — Codex continuation and recovery

## One-line resume prompt

`Read CONTINUE_CODEX.md and continue from the current repo state`

This file is a durable recovery procedure, not a frozen task list. Live repository state, the owner's latest explicit instructions, and `AGENTS.md` take precedence over historical notes. Do not assume a prior session's plan is still unfinished.

## Mandatory first steps — every resumed session

1. Read `AGENTS.md`, this file, the current user request, and relevant architecture/test/release documentation.
2. Inspect `git status --short --branch`, `git diff`, staged diff, recent `git log`, current branch/upstream, and actual remote `main`. Fetch safely. Check existing PRs, reviews, and CI for the active branch.
3. Determine what is already implemented, committed, pushed, tested, merged, or published. Inspect source and tests rather than trusting a previous summary or old SHA.
4. Identify the **latest owner-authorised unfinished task** from the current request and repository evidence. Do not invent a new release, revive archived tasks, or repeat completed work.
5. Preserve valid uncommitted and remote work. Do not reset, clean, overwrite unrelated edits, force-push, or rewrite history without explicit owner authorisation. If work is ambiguous or conflicting, explain the blocker before destructive action.
6. Continue from the smallest safe unfinished step, completing coherent implementation and regression tests. Keep an accurate progress checkpoint in this file if work must stop again.

## Current verified baseline (12 November 2026)

- Repository: `mmwiza1/Race-day-dreams`; default branch `main`. **0.12.2-test1 / versionCode 18 is complete, merged and published**, not an unfinished feature branch.
- [PR #21](https://github.com/mmwiza1/Race-day-dreams/pull/21) — Material 3 spacing and exercise-library refinement — was squash-merged as **e4d5753513ef69c5c4c17560238be23e59caf923**. [Final PR-head CI 43256007882](https://github.com/mmwiza1/Race-day-dreams/actions/runs/43256007882) and [post-merge main CI 43262272308](https://github.com/mmwiza1/Race-day-dreams/actions/runs/43262272308) succeeded.
- The merged software gate passed **538 full Android instrumentation tests and 433 source-verified authentic screenshots on EACH API 28/35/36**, plus **700,108 domain checks / 47 suites**, 75 schema tests, backup/restore, lint, formatting, guidance, release and source/provenance checks. See `VERIFICATION.md` and `docs/VISUAL_REVIEW_0_12_2.md` for exact source and visual evidence.
- [Rolling Latest Development Build](https://github.com/mmwiza1/Race-day-dreams/releases/tag/development) publishes **Race-Day-Dreams-0.12.2-test1.apk** (55,643,562 bytes, SHA-256 **144bb095ce2447b1b193e4db2a2b62a01c5940ee28b57842bc337d71146b791d**) with its checksum, tied to the verified merged `main` release. Debug-signed test build, not a stable/Play Store release.
- SQLite schema **19**, root JSON backup **13**, raw recording archive **3** and complete backup package **1** remain unchanged from 0.12.1. No new permissions, dependencies, GPS/Health Connect/strength engines, or icons were introduced by 0.12.2.
- PRs #19 and #20, 0.11/PR #18, and earlier Strength/GPS work are completed history. **Do not revive their old draft/merge/publication checkpoints.** Before acting, always inspect the real GitHub state; later documentation-only commits may move `main` beyond the release commit without changing the APK/source.
- **Physical acceptance remains UNPERFORMED:** Galaxy S24/Pixel/Xiaomi, outdoor GPS accuracy, locked-screen/background recording, real Health Connect/BLE, audio/battery, OEM rotation and human TalkBack. Synthetic narrow-window platform navigation clipping remains documented. Emulator tests are not physical verification.

## Completed design direction

The owner-authorised Material You modernisation is delivered. Preserve the central dynamic/branded light/dark theme, type hierarchy, rounded tonal surfaces, accessible disclosures, responsive navigation, safe system insets and retained sleeping/dreaming female-runner icon.

Running Home prioritises today's session, goal and upcoming training. Live distance/time/pace/GPS are glanceable, controls stack at 200% and Finish requires confirmation. Existing Strength, history, calendar, health, recovery, Race Day, shoes and settings inherit the shared design. Native goal and privacy forms keep safe, scrollable actions.

Existing official Material 3 APIs were reused without upgrading runtime dependencies. Newer experimental Expressive motion/loading APIs are intentionally omitted. The documented synthetic short-wide blank platform navigation strip remains a hardware/rotation acceptance limitation; app navigation tests pass. No data, engine, icon or permission rewrite is required.

## Implementation and delivery policy

- Follow `AGENTS.md`: feature development on a dedicated branch and PR, not directly on `main`. Small expressly authorised maintenance/documentation changes may follow its exception.
- Make narrowly scoped, reviewable commits. Do not silently modify unrelated features, tests, release automation or project policy.
- Run the relevant domain tests, unit/instrumentation tests, lint, formatting, build, schema/backup checks, and UI screenshot/accessibility gates appropriate to changed code. Diagnose failures; never weaken tests to manufacture green CI.
- Use the repository's release-note template, generator and validation instructions. Never invent build artifacts, download URLs, checksums, test passes or physical-device verification.
- Follow the repository's owner-authorised release/merge convention only for an explicitly authorised implementation scope; a request to audit, prepare a prompt or maintain this file is **not** permission to implement or merge the next feature.
- Before declaring a release complete, verify exact PR head CI, actual merged `main`, post-merge CI and the matching development APK/version/commit/checksum. Do not create a stable production tag without specific approval.

## Completed release — 0.12.3-test1 (13 November 2026)

- **MERGED/PUBLISHED:** [PR22](https://github.com/mmwiza1/Race-day-dreams/pull/22) squashmerged as`0f866185beac51760a3459806ad725eeda10ac56`. FinalPRheadb80a1e0 passed [43402640853](https://github.com/mmwiza1/Race-day-dreams/actions/runs/43402640853); actualmergedtree equals verifiedPRtree. [Post-main43408133512](https://github.com/mmwiza1/Race-day-dreams/actions/runs/43408133512) and gatedpublisher SUCCESS. Do not reopenPR22, restart completed work, begin another release or iOS port without a new request. Owner CIoptimisationPR23 remains SEPARATE; do not edit/merge it here.
- **Delivered:** Task-first British-English exercise/running/gym/health/backup wording; retained exact targets, fractionalRPE/RIR, reference/actual/estimated distinctions and separate save/review/activation. README fivegroups/16realimages/adjacentdownloads/fullgallery/archive. Serial/live-main byte-verified aliaspublisher, own-filename checksums, olderasset preservation and four real commit-pinned releasepreviews. Version0.12.3-test1/code19; SQLite19/rootJSON13/raw3/package1 unchanged. No calculation/engine/history/permission/navigation/icon/dependency change.
- **Verification:** EACH API28/35/36 passed25guards/543unique fulltests/451authentic captures on implementation, finalPRhead and mergedmain; allcompletedlogs checked. Guards repeat withinfullsuite, not extraunique tests. Domain700,189checks/48suites, schema75, backup/restore/retention, build/lint, pinned125Kotlin/161Java, release10/publisher7/showcase5/runner13/capturevalidator23 PASS. All220exercise/14routine guides complete. No assertion weakening, arbitrary delays or blind reruns.
- **Visual acceptance:** Canonicala2b20ca matches all451API35artifact14024788542 PNGs byte-for-byte; fingerprint`e2cfd08d05185f27e7889a0ed82b4e11e92b6f11ec244c313ed4581d123e57a9`.70original light/dark/200% views plus fullnative200% reviewed. TEST-ONLY API28fullcompositor capture corrects legacycrop and awaits actual statusglyphcontrast, retaining genuineletterboxing. No productionActivity change/image editing/rescale. Details/hashes in VERIFICATION.md and docs/VISUAL_REVIEW_0_12_3.md.
- **Published byte proof at merge checkpoint:** MainAPI35artifact14028664266 ZIP29,797,604bytes/SHA2560819dd866f690affd779afe8f27601ef0339f3ac34d2467b3b58265eff80d195. BOTH publicversioned/stableAPKs55,659,946bytes/SHA256`a8c0a335839e63aceb831f80ac1a2b4f3db1515bf6090202ea3cc0a5d13a4e68`, identical to mainartifact; ownSHAfiles correct, code19/minAPI28/noINTERNET. Releasebody/tag/source/run agree; four pinnedpreviewPNGs match repositorybytes; valid0.12.2asset retained. No stableproductiontag.
- **Actual GitHub presentation accepted:** MainREADME and rollingrelease rendered at1280/390/320px using realTLS-verifiedGitHub responses.16READMEimages/4releasepreviews decode per mode, noarticleoverflow; introduction, featuregroups, adjacentdownloads, installation and previews visually reviewed. Values labelled syntheticdemonstrations. [StableAPK](https://github.com/mmwiza1/Race-day-dreams/releases/download/development/Race-Day-Dreams-latest-test.apk) and [SHA](https://github.com/mmwiza1/Race-day-dreams/releases/download/development/Race-Day-Dreams-latest-test.apk.sha256) follow verifieddevelopmentbuilds; versionedpairs remain.
- **Maintenance/recovery boundary:** This final delivery record is an owner-authorised documentation-only maintenance checkpoint under AGENTS.md's exception, on`docs/0.12.3-completed-release-checkpoint`. It may advance main beyond the verifiedmergeSHA and trigger a new debugsignature/checksum for identical appsource. Always inspect actualmainCI/currentreleaseassetidentity before claiming the newestmain passed; the hash above records the verifiedmerge checkpoint. No implementation remains. Verify the following maintenance-main workflow and publicartifact pair if resuming before its completion; do not restart the release.
- **Proofs/tools:** `/workspace/scratch/plain-language/main0f-api35-apk/verification.json`, `main0f-public/verification.json`, `main0f-release/release-identity.json`, `showcase-main-browser/verification.json`, `release0f-browser/verification.json`, `476-api{28,35,36}-captures/verification.json`, `476-review/`. Helpers`verify_artifact.py`, `verify_public_pair.py`, `verify_release_identity.py` and GitHubbrowserreviewers validate actualsource/digests/safeZIP/public4files/aapt/pinnedpreviews/rendering. Refresh expired artifactURLs from connectedGitHubtools; never expose SAS or screenshotbase64logs. No localKVM; runtimeverification is GitHubActions.
- **Physical checks UNPERFORMED:** GalaxyS24/Pixel/Xiaomi, humanTalkBack, outdoorGPS, realHealthConnect/BLE, OEM audio/background/battery/rotation. Known forced-narrow platformnavigationstrip remains a physical/rotation limitation. TEST_BUILD.md retains all hardwarechecklists. Software/emulator/browser success is not hardware acceptance.

## Active work — experimental shared Android/iOS application

The owner's 13 November request authorises Kotlin Multiplatform / Compose Multiplatform,
replacement of the Java domain implementation, shared existing UI, Health Connect / HealthKit,
platform storage/services and functioning Android AND iOS builds. This supersedes the earlier
no-iOS boundary. No version bump/publication until cross-platform ready. Do not merge this
incomplete migration merely because a focused checkpoint passes.

### Latest recovery pointer — checkpoint 235 iPhone acceptance instructions

- Added the experimental iPhone build instructions and outstanding
  HealthKit, GPS/background, BLE/audio, accessibility and Files/backup
  hardware checks in TEST_BUILD.md. Confirmed the documented arm64
  framework and Xcode settings exist. This Linux workspace cannot sign
  or run a physical iPhone build. No device acceptance is claimed.
- GPX run [44313049480](https://github.com/mmwiza1/Race-day-dreams/actions/runs/44313049480)
  for `c79232e` is active. Preserve it. Retrieve Apple job 134831874165's
  Kotlin/Native GPX and XCTest picker results when available. Batch
  this local documentation with the next necessary changes. PR #24
  remains draft and full release gates remain pending.

### Latest recovery pointer — checkpoint 234 verified saved route and Apple Health choices

- [Run 44305594496](https://github.com/mmwiza1/Race-day-dreams/actions/runs/44305594496)
  passed on `f4b3410`: 642 focused API 35 tests, Apple tests, build,
  standalone launch and both production XCUITests. Apple Health choice
  passed in 50.824 s. Full favourite/replay/recovery/simulated-route
  flow passed in 177.521 s. Result artifact 14431141319.
- Startup artifact 14430960274 was visually inspected and checksum
  validated. Synthetic source `5b0e300` has the exact PR tree `eb111d5`.
  The simulator test verifies a retained route and available GPX action.
  It does not verify physical GPS or a native HealthKit import.
- Extended the native flow to open the real GPX document picker and
  cancel it, and added a Kotlin/Native test round-tripping production
  GPX output through the Apple decoder. Asserted two segments, original
  times and coordinates, and missing/present elevations. Pinned
  formatting and diff passed locally. Native execution pending.
- Verify both new GPX checks on the next run. Then verify any remaining
  integration and full release gates. Keep PR #24 draft while incomplete.

### Latest recovery pointer — checkpoint 233 simulator location API minimum

- [Run 44300142251](https://github.com/mmwiza1/Race-day-dreams/actions/runs/44300142251)
  on `12a9447` passed focused Android, Apple shared tests, framework link,
  app build and standalone startup. The saved-route XCTest could not
  compile: XCUIDevice.location requires iOS 16.4, and the test target
  inherited the app's iOS 16.0 minimum. No saved-route result.
- Set iOS 16.4 on the UI-test target alone. The production app's iOS 16.0
  minimum remains in force. Parsed the project locally and checked
  both deployment targets and diff. Native runtime remains pending.
- Batch this fix with the Apple Health controls from checkpoint 232.
  Verify both XCTest cases and startup on the exact pushed head. Fix
  any demonstrated failure. HealthKit import, physical checks and
  full release gates remain pending. PR #24 stays draft.

### Latest recovery pointer — checkpoint 232 Apple Health choice semantics

- The iOS Apple Health selectors now expose their labels and checked
  state as a single accessible control with a 48 dp minimum row height.
  Tapping the text or checkbox changes the selected category. The
  separate explicit import action still controls the native request.
- Added a production XCTest for the weight choice enabling and disabling
  the import action. This exercises UI intent without asserting any
  native permission grant or actual HealthKit import. Pinned source
  formatting and diff checks passed. Native compilation/runtime pending.
- Local checkpoint only. Leave the existing saved-route run
  [44300142251](https://github.com/mmwiza1/Race-day-dreams/actions/runs/44300142251)
  on `12a9447` intact. Inspect its Apple job 134787427891 and batch this
  with any demonstrated route fix. Then verify Apple Health on simulator
  and applicable hardware, remaining parity, and full release gates.
  PR #24 stays draft.

### Latest recovery pointer — checkpoint 231 verified iOS replay and recovery

- [Run 44293098860](https://github.com/mmwiza1/Race-day-dreams/actions/runs/44293098860)
  passed on `f3d39f6`: 642 focused Android API 35 tests, Apple shared
  and UI-module tests, app build, startup and the extended production
  XCUITest. One XCTest passed in 115.970 s with zero failures. Result
  artifact 14425187336, startup capture 14424068304. Production app
  verified saving a favourite without location access, reopening it,
  granting location, replaying, pause/resume, and interrupted recovery
  and discard after relaunch.
- The next test extension uses documented XCUITest simulator location
  samples in a fresh recorded run. It checks GPS readiness and positive
  recorded distance, finishes via the production UI, opens the retained
  route and checks the GPX export action is available. API declarations
  and diff checked locally. Live simulator result is pending. This does
  not verify a physical GPS receiver or an actual exported GPX file.
- Collect the new native result and diagnose any genuine failure. Then
  verify HealthKit UI/native effects, remaining parity, and the full
  release gates before any merge or publication. PR #24 stays draft.

### Latest recovery pointer — checkpoint 230 verified workout persistence

- [Run 44288009528](https://github.com/mmwiza1/Race-day-dreams/actions/runs/44288009528)
  passed on `9ce9ba9` (642 focused API 35 Android tests, Apple shared
  tests, app build, standalone startup and production XCUITest).
  Test `testFavouriteSurvivesRelaunchWithoutLocationPermission`
  passed in 113.748 s, one test and zero failures. XCTest result
  artifact 14422625562. Screenshot 14422011392 showed genuine onboarding;
  SHA-256 `2025240e9328c01558462a740db047dd55c24a7a3ede5470747839a8458b7a8a`
  matched and source `9450234` had the exact PR tree `adc5e07`.
- Extended that real app flow to grant location through the system
  alert, start the saved prescription, pause/resume, relaunch while
  interrupted and explicitly discard through UI. No route is fabricated.
  Diff check passed locally. Swift/XCTest runs only on macOS CI here.
- Verify the new Apple test. If it passes, add evidence-backed saved-route
  and HealthKit checks without treating them as physical acceptance,
  confirm remaining parity and run the required release matrix.
  Keep PR #24 draft while the migration is incomplete.

### Latest recovery pointer — checkpoint 229 independent XCTest execution

- [Run 44282849055](https://github.com/mmwiza1/Race-day-dreams/actions/runs/44282849055)
  passed focused Android, Apple shared tests, framework linking and the
  simulator app build. The standalone startup step reached the schema
  wait then hit its 15-minute limit. The diagnostic archive 14420655147
  contained empty app output files. This does not establish another app
  crash. XCTest was skipped under the old step dependency.
- XCTest now runs whenever the app builds, even after a standalone
  startup failure. If the startup step did not verify a simulator,
  XCTest gets an isolated simulator of the available runtime and model.
  The standalone startup failure remains a failing gate. Local YAML,
  shell, diff and both stubbed simulator selection checks passed.
- Verify the two separate Apple gates on the next run. Collect XCTest
  results and fix proven harness/app issues. Release acceptance and
  physical checks remain open. Keep PR #24 draft.

### Latest recovery pointer — checkpoint 228 simulator shell recovery

- [Run 44276798053](https://github.com/mmwiza1/Race-day-dreams/actions/runs/44276798053)
  passed focused Android, Apple shared tests, framework linking and the
  corrected app build. Startup was blocked by a simulator SpringBoard
  invalid response, before an app PID was allocated. Artifact 14416518898
  contains the black frame and CoreSimulatorBridge error
  `FBSOpenApplicationServiceErrorDomain Code=5`. No app crash is shown.
  The production XCUITest was skipped.
- The smoke now waits for simulator readiness and retries exactly once
  for the observed SpringBoard rejection. All other launch errors,
  app exits, missing database or failed OCR still fail. Local YAML,
  shell, diff and stubbed retry/rejection checks passed.
- Verify the next Apple run through SQLite, onboarding and the production
  workout persistence XCUITest. Collect the actual XCTest result before
  adding native recording, replay and HealthKit coverage. No merge or
  publication. PR #24 stays draft.

### Latest recovery pointer — checkpoint 227 production iOS workout persistence test

- Run 44272542319 finished green on Android and Apple. The verified
  foreground startup evidence below is unchanged.
- Added a native XCUITest scheme/target and simulator gate. The test
  selects the real recording onboarding path, creates and saves a
  favourite without location permission, terminates and relaunches the
  app, then reopens the retained step through the actual workout library.
  The gate uploads the XCTest result bundle and retains the separate
  verified app and startup artifacts.
- Project object/reference checks, scheme XML, workflow YAML/shell syntax,
  and diff checks passed locally. This Linux workspace cannot compile
  Xcode test targets. Inspect the first Apple run for build and runtime
  results; fix real failures without replacing the production flow.
  Recording/replay, HealthKit, remaining parity and full release gates
  are still pending. PR #24 stays draft.

### Latest recovery pointer — checkpoint 226 verified iOS foreground startup

- [Run 44272542319](https://github.com/mmwiza1/Race-day-dreams/actions/runs/44272542319)
  on `a561f09` passed its focused Android job and the Apple simulator
  build, required packaged plist setting, SQLite schema-19 readiness
  and foreground onboarding OCR. Apple job finalization was pending.
- Artifact 14414508045 was downloaded and visually inspected. It shows
  the real "Make today yours" onboarding and its Running and Strength
  controls. PNG checksum
  `be645e60b7a667d984f34c540fe2c0a39ac534f2e9e811974e0d6c2326ffe17f`
  matches. The capture's synthetic merge `b640e2d` has PR parent
  `a561f09` and the identical tree `7cd9a43c7afc702340b6a01999179210c5a45107`.
  Local evidence: `/workspace/scratch/kmp225-startup/`.
- Treat launch as verified on this exact simulator revision. Continue
  native app interactions using a production UI harness, starting with
  choosing Record my runs, opening the custom workout editor without
  location permission, saving a favourite and confirming it survives
  relaunch. Then verify recording, replay, HealthKit and the remaining
  parity and full release gates. Keep PR #24 draft until those pass.
  This handoff is local and does not start redundant CI.

### Latest recovery pointer — checkpoint 225 verify the plist correction

- [Run 44267864437](https://github.com/mmwiza1/Race-day-dreams/actions/runs/44267864437)
  on `56045e6` passed focused Android, Apple tests, framework link and
  the native app build with the launcher catalog. Normal foreground
  startup failed. Artifact 14412692296 confirms LLDB attached at
  `_dyld_start` but timed out during continue, with no faulting
  frame. OS crash-report and app output capture supplied no stack.
- The required Compose plist setting is fixed in `58e3d41`. The next
  Apple run must verify it in the built app and then pass SQLite readiness
  and foreground OCR. Inspect its exact-head capture. If startup still
  fails, use the new evidence to distinguish the remaining crash and
  improve native stack capture if needed. No merge or publication.
  PR #24 remains draft.
- The earlier Android log confirms 642 API 35 instrumentation tests
  passed, including the new dark/200% shared editor test. The plist fix
  is now pushed on `a561f09`. Track
  [run 44272542319](https://github.com/mmwiza1/Race-day-dreams/actions/runs/44272542319)
  and Apple job 134692728408. Leave the run intact while it tests the
  corrected app. The 642 count applies to the earlier head.

### Latest recovery pointer — checkpoint 224 required Compose plist setting

- The source published for pinned Compose UI 1.8.2 confirms
  `enforceStrictPlistSanityCheck = true` by default and that its UIKit
  controller asynchronously throws unless
  `CADisableMinimumFrameDurationOnPhone` is true. The app plist omitted
  it. Added the required setting and asserted it on the compiled iOS
  app plist in CI. This corrects a demonstrated configuration error.
  Whether it accounts for the observed SIGSEGV remains unverified.
- Source plist parsing, workflow YAML/shell syntax and diff checks passed.
  This is a local checkpoint because
  [44267864437](https://github.com/mmwiza1/Race-day-dreams/actions/runs/44267864437)
  on `56045e6` is running. Inspect Apple job 134676768769 and its LLDB
  capture, then batch this fix with any stack-driven correction.
  Verify fresh startup, SQLite and the foreground screen on the new head.
  PR #24 remains draft.

### Latest recovery pointer — checkpoint 223 native debugger capture

- [Run 44264043283](https://github.com/mmwiza1/Race-day-dreams/actions/runs/44264043283)
  passed focused Android and Apple compilation/tests/build. Apple foreground
  launch failed again in scene creation with SIGSEGV. Artifact 14410608084
  has the failure frame, simulator log and image UUID but no crash report
  or app stdout/stderr. The faulting frame remains unknown.
- When the original launch process exits and no OS crash report is
  available, the gate now attempts one bounded reproduction under LLDB
  and uploads its stack/image list. The original failure status is
  preserved. YAML, shell and Python syntax, diff checks and stubbed
  debugger command/PID handoff passed. Live LLDB remains unverified.
- Push this and the launcher catalog checkpoint together on PR #24.
  Inspect the native capture and resolve the actual startup defect.
  Existing onboarding/schema assertions and final feature/release gates
  remain required. Keep the PR draft.

### Latest recovery pointer — checkpoint 222 iOS launcher artwork

- iOS now registers a universal 1024 px opaque launcher asset made from
  the existing approved illustration and blue background. The app icon
  generator writes the same asset; Android outputs are untouched.
  Local catalog structure, pixel dimensions/opacity, visual preview,
  shell syntax, YAML and diff checks passed. Native actool and simulator
  display are pending. Next Apple packaging gate requires Assets.car.
- This commit is local to avoid cancelling
  [44264043283](https://github.com/mmwiza1/Race-day-dreams/actions/runs/44264043283)
  on pushed head `a987f5b`. Both jobs were still running. Inspect the
  Apple crash report or foreground screenshot before pushing the next
  batch. The launcher change is independent of the startup crash.
  PR #24 remains draft.

### Latest recovery pointer — checkpoint 221 confirmed native startup crash

- [Run 44258765847](https://github.com/mmwiza1/Race-day-dreams/actions/runs/44258765847)
  on `476583a` passed Android, Apple shared tests, framework linking,
  Apple UI-module tests and the simulator build. Foreground startup failed.
  Artifact 14408389327 (`/workspace/scratch/kmp220-apple/`) confirms that
  PID 40775 entered UIKit/Compose scene creation and exited with SIGSEGV
  before database readiness. App stdout/stderr were empty and the system
  log did not contain a native stack. No verified foreground screenshot.
- The next smoke additionally captures fresh RaceDayDreams crash reports
  from host and simulator crash logs and records the app binary UUID.
  Existing schema and onboarding checks remain required. Workflow YAML,
  shell syntax and diff checks passed locally.
- Push checkpoints 217–221 together once run 44258765847 has completed.
  Retrieve the new crash report and use the faulting stack to diagnose
  the actual native startup failure. Continue the new Android editor
  guard, iOS production workflow verification and full release gates.
  PR #24 remains draft.

### Latest recovery pointer — checkpoint 220 workout library permissions

- iOS workout favourites can be browsed, edited and removed before
  granting location permission. Retained structured runs remain visible.
  Recording starts stay permission-gated, and interrupted run recovery
  still takes precedence. Touched ktfmt and diff checks passed. Native
  compilation and runtime verification remain pending.
- [Run 44258765847](https://github.com/mmwiza1/Race-day-dreams/actions/runs/44258765847)
  on pushed head `476583a` passed the focused Android job. The Apple job
  was still compiling/testing shared code. Keep checkpoints 217–220
  local until the Apple startup evidence is available. Verify the new
  permission flow alongside the existing authoring/replay flows. PR #24
  remains draft.

### Latest recovery pointer — checkpoint 219 shared workout library

- Android's existing running workout library now delegates favourite
  paging/save/removal and structured replay reads to the same typed
  SQLite repository as iOS. Android still scans up to 50 retained
  executions, and the Android screen still shows up to ten. Existing
  archive schema, screen and import validation remain in place.
- Both focused SQLite tests and Android instrumentation compilation
  passed (`/workspace/scratch/kmp219-shared-run-library.log`). Touched
  shared formatting and diff checks passed. Stubbed success and failure
  paths of the Apple startup shell verified artifact handling locally.
- Checkpoints 217–219 are local while [44258765847](https://github.com/mmwiza1/Race-day-dreams/actions/runs/44258765847)
  is still running. Collect Apple job 134646719566's foreground result
  or diagnostics before pushing. Then verify Android's existing
  Release08 tests, the new shared editor UI guard and iOS production
  flows on the new head. PR #24 remains draft.

### Latest recovery pointer — checkpoint 218 workout editor UI guard

- Added an Android instrumentation test of the production shared editor,
  dark theme and 200% text. With recording unavailable it duplicates a
  retained warm-up group, commits only on explicit save, and preserves
  the original in-memory prescription. The test compiles locally
  (`/workspace/scratch/kmp218-editor.log`), and is included in the next
  focused gate. Runtime remains unverified.
- Keep local checkpoints 217–218 unpushed while [44258765847](https://github.com/mmwiza1/Race-day-dreams/actions/runs/44258765847)
  on `476583a` finishes. Latest Android stage was API 35 instrumentation;
  Apple shared tests were running before framework linking. Formatting
  CI passed on the pushed head. Inspect the foreground capture or failure
  diagnostic and then batch any needed fix with these commits. PR #24
  remains draft.

### Latest recovery pointer — checkpoint 217 structured workout parity

- iOS quick run offers 3/5/10 km and 20/30/45/60 minute presets that
  populate the normal target fields. The explicit Start controls still
  own recording initiation. Touched pinned formatting and diff checks passed.
- Four shared recording archive tests passed (`/workspace/scratch/kmp217-archive.log`).
  The new test proves an authored manual warm-up advances only by explicit
  action and preserves both completion times and the custom origin.
- This checkpoint is local. [Run 44258765847](https://github.com/mmwiza1/Race-day-dreams/actions/runs/44258765847)
  for pushed head `476583a` was still running on Android and Apple. Do not
  cancel it with another push. Inspect Apple job 134646719566 and the
  source-bound screenshot or failure artifact before batching this commit.
  Actual iOS quick/custom workout flows, foreground startup and full
  release gates remain pending. PR #24 remains draft.

### Latest recovery pointer — checkpoint 216 foreground failure evidence

- Apple job 134631096266 on `2ea5c08` built the simulator app, booted the
  simulator and received PID 45419 from simctl. The run then failed opening
  the expected database before the screenshot check. The old shell exited
  before diagnostics, so the iOS launch failure's root cause remains open.
  Android focused CI passed on the same revision.
- The next smoke checks process liveness during database startup and
  preserves app stdout/stderr, simulator logs and a diagnostic screenshot
  on failure. The schema and onboarding screenshot assertions remain
  required. Workflow YAML, shell syntax and diff checks passed locally.
- Checkpoints 212–216 are batched for PR #24. Inspect the new Apple build
  and smoke. If it fails, download experimental-ios-startup-diagnostics
  and fix the demonstrated platform/app issue. If it passes, verify the
  screenshot source and exercise the new production workout flows. The
  full release gates remain pending, and the PR remains draft.

### Latest recovery pointer — checkpoint 215 location-independent authoring

- The iOS custom workout editor can open and save a favourite before the
  athlete grants location access. Starting the recorder remains gated by
  the existing location permission. Local shared Android compilation and
  all three recording archive tests passed (`/workspace/scratch/kmp215-archive.log`).
  Pinned touched formatting and diff checks passed.
- The local branch is ahead of PR #24 by checkpoints 212–215. Android
  focused CI passed on pushed head `2ea5c08`. Apple shared tests, framework
  linking and simulator build passed. The foreground startup step failed
  in [44253971258](https://github.com/mmwiza1/Race-day-dreams/actions/runs/44253971258),
  so no screenshot was published. GitHub was still finalizing the job and
  did not serve its log. When available, inspect job 134631096266 logs and
  diagnose the failing boot/launch/screenshot command before pushing the
  local batch. Next verify the exact new iOS build and
  permission, authoring and replay flows. Keep PR #24 draft.
- The next workflow checkpoint bounds the foreground step at 15 minutes
  and logs each boot, install, database, and screenshot phase. YAML, shell
  syntax and diff checks passed. The in-flight run still uses the earlier
  workflow and is unaffected by this local change.

### Latest recovery pointer — checkpoint 214 iOS workout authoring

- iOS can create and edit a structured running workout and start it through
  the existing recorder. The Compose editor supports distance, time and
  manual steps, pace/effort targets, grouped rounds, editing, reordering,
  duplicating and deleting groups. Favourites save and remove the existing
  version-1 definitions. Archived executions keep independent snapshots.
- The shared Android compilation, three recording archive tests and focused
  JDBC favourite save/remove/page test passed. Pinned touched formatting
  and diff checks passed. Log: `/workspace/scratch/kmp214-tests.log`.
- On pushed head `2ea5c08`, Android focused CI passed. Apple shared tests
  and framework link passed and the simulator build was running in
  [44253971258](https://github.com/mmwiza1/Race-day-dreams/actions/runs/44253971258).
  Inspect the actual foreground smoke result before the next push. If
  green, inspect the source-bound screenshot. If red, diagnose the failing
  step. Then push checkpoints 212–214 together with any correction and
  verify the new iOS compile, custom/replay/favourite flows and broader
  release gates. PR #24 remains draft.

### Latest recovery pointer — checkpoint 213 restored running favourites

- iOS Record a run can page through saved favourite running workouts
  restored from an existing backup, then start the exact grouped steps
  using the replay recorder. The shared typed reader decodes the original
  archives in stable, bounded pages. No schema or backup version changed.
- Focused JDBC test for stable pagination and repeat-group preservation
  passed. Pinned formatting and diff checks passed. Local logs:
  `/workspace/scratch/kmp213-storage.log` and `/workspace/scratch/kmp213-format.log`.
  iOS compile/runtime verification of checkpoints 212–213 remains pending.
- Push checkpoints 212–213 with the next Apple startup correction or
  coherent authoring batch, after collecting the in-flight foreground
  startup result on pushed head `2ea5c08`, run 44253971258. Finish the
  Android-equivalent custom step editor and favourite save/remove using
  the existing legacy definition shape. Preserve prior runs and actuals.
  Then run native production-flow and full release gates as in checkpoint
  212. PR #24 remains draft.

### Latest recovery pointer — checkpoint 212 replay retained runs

- iOS Record a run now offers the last ten saved structured runs using the
  same archive as Android. Race and unsaved sessions are excluded. Replay
  keeps the exact steps and origin, with fresh events and no automatic plan
  association or copied actuals. The existing recorder remains the owner.
- Three shared archive tests passed on the Android JVM target (distance
  crossing, duration/pause, and exact replay). One real JDBC SQLite test
  passed (filter, ordering, and unchanged history). Pinned formatting and
  diff checks passed. Logs: `/workspace/scratch/kmp212-{replay,storage,format-final}.log`.
- [Experiment 44253971258](https://github.com/mmwiza1/Race-day-dreams/actions/runs/44253971258)
  for pushed head `2ea5c08` was still running on both platforms. Keep that run
  for the first foreground startup evidence. Replay is a local checkpoint
  until the Apple smoke result is available to batch the next push.
- Next: review the foreground startup result and any failure diagnostics,
  then finish iOS custom/favourite run authoring and production-flow
  verification, remaining platform parity, and final release gates.
  PR #24 remains draft.

### Latest recovery pointer — checkpoint 211 iOS startup evidence

- PR #24 is draft. [Experiment 44216267843](https://github.com/mmwiza1/Race-day-dreams/actions/runs/44216267843)
  on head `71c2ba6` passed the Android API 35 focused job, Apple build and database
  smoke, and shared tests (1,187 JVM, 1,148 Apple, four native UI-module tests).
  [Formatting 44216267884](https://github.com/mmwiza1/Race-day-dreams/actions/runs/44216267884) passed.
  The simulator artifact and experimental Android APK were produced. The APK SHA-256 is
  `1bc83c63665351a75ddee2b2cc9e815d8db35cc345cd236833e67951789e33af`.
- The inspected, checksum-verified iOS screenshot was SpringBoard, not the app.
  Its synthetic PR merge `97716cc` has the same tree as `71c2ba6`. Database schema
  creation and simulator build are verified, but foreground rendering and quick-run
  runtime remain unverified. Local evidence: `/workspace/scratch/kmp211-evidence/`.
- The smoke now checks process liveness, requires two real first-run coaching labels
  in the screenshot using macOS Vision, and logs app/SpringBoard diagnostics on a
  failed capture. Workflow YAML, shell syntax, and diff checks passed locally. Verify
  the Apple run on this checkpoint. If it fails, inspect the diagnostics for the actual
  launch failure before changing app code. If it passes, inspect the source-bound
  screenshot, exercise real iOS flows including quick runs, and continue remaining
  parity and the full Android API 28/35/36 and iOS release gates before merge.

### Latest recovery pointer — checkpoint 210 quick runs and CI repairs

iOS can start a distance- or duration-target quick run using the shared
execution engine and the existing quick recording archive. It remains
separate from a planned run. Shared Android Kotlin and Android
instrumentation Kotlin compilation and touched formatting passed.
Native quick-run runtime verification is pending.

Experiment 44213596116 ended red. Android crashed in the 200% text
StrengthRecordsScreenTest due to concurrent Compose layout/draw. The test
now uses the established ActivityScenario/main-thread window recomposer
as in StrengthAnalyticsScreenTest, with every assertion preserved.
The Apple shared tests passed; framework compilation failed because the
exercise artwork callback already supplies a String ID. The lookup now
uses that ID directly. Both jobs finished, so push checkpoints 205–210
and verify both fixes, source-bound Apple startup and the remaining full
release gates. Keep PR #24 draft.

### Latest recovery pointer — checkpoint 209 training recovery review on iOS

Gym and Today now open the shared recovery and training-balance review.
The existing coordinator uses retained training evidence, leaves missing
health context unknown, and saves lighter-week/move options as drafts.
iOS only exposes the exact requested draft for review and activation.
Focused JVM recovery and concrete-preview suites passed. Touched
formatting and diff checks passed. Native runtime remains unverified.

PR #24 remains draft at fb2b85e. Experiment 44213596116 was still running
on Apple and Android. Preserve it and inspect the framework, decoder and
simulator results before pushing checkpoints 205–209. Continue remaining
platform parity and production flow/release verification.

### Latest recovery pointer — checkpoint 208 privacy-limited iOS diagnostics

iOS Settings can export a diagnostic JSON through the existing private
staging and system document picker. Its fixed allow-list contains app and
iOS versions, schema/backup versions, Health availability and location
access, with no athlete records. Added a native test for the exact key set.
Pinned touched formatting and diff checks passed. Native compilation, test
execution and picker verification remain pending.

PR #24 remains draft at fb2b85e. Experiment 44213596116 was still running
on both platforms. Preserve the jobs and inspect their conclusions before
pushing checkpoints 205–208. Verify the next source-bound simulator
capture and the genuine iOS settings/export flows before final gates.

### Latest recovery pointer — checkpoint 207 iOS privacy and bundled licences

iOS Settings now explains local storage, optional Apple Health access
and backup encryption, and makes the bundled MIT, Apache 2.0 and Dream
Route/ODbL notices readable offline. The Xcode target and artifact gate
include the original licence files. Pinned formatting, diff, workflow
YAML and resource-path checks passed. Native packaging and UI remain
unverified.

PR #24 remains draft at fb2b85e. Experiment 44213596116 still had both
jobs running. Preserve it and inspect actual results before pushing
checkpoints 205–207. Verify the next simulator package, independently
review the separate source-bound startup screenshot, and continue the
remaining native parity and full release gates.

### Latest recovery pointer — checkpoint 206 session planning preferences on iOS

iOS Settings now edits the existing session budget, session distribution and
one-off unavailable dates. It saves in the validated legacy backup format,
read by the existing explicit plan generator. Saving preferences does not
rebuild the plan. Eleven JDBC core backup tests passed, including invalid
replacement and round-trip restoration. Pinned formatting, diff checks
and 19 schema boundaries passed. Native UI verification is pending.

PR #24 remains draft at fb2b85e. Experiment 44213596116 was still running
Android and Apple. Preserve it, review the exact-head results and push
checkpoints 205–206 with any concrete fixes. Verify simulator startup,
source-bound screenshot and production settings, then continue parity
and full release gates.

### Latest recovery pointer — checkpoint 205 independently reviewable iOS startup capture

The Apple experiment now publishes the startup screenshot in its own
small artifact. Both that artifact and the simulator app archive include
the checked-out commit and their own SHA-256. Workflow YAML, the Apple
shell command syntax, artifact paths and diff checks passed locally.
No new runtime verification is claimed for this workflow change.

PR #24 remains draft at fb2b85e. The Android formatting workflow
44213596107 passed and experiment 44213596116 is still running. Preserve
it and inspect both conclusions before pushing this checkpoint. The
earlier experiment 44208542517 was cancelled in teardown after successful
Apple application build, simulator launch, schema 19 and artifact upload.
Its native UI XML was retrieved via the GitHub connector and shows all
three GPX decoder tests passed; no independent startup screenshot review
was performed. Continue exact-head production UI and release gates.

### Latest recovery pointer — checkpoint 204 simulator startup verified

Experiment 44208542517 on c3f0ec4 passed Android and every substantive
Apple step, including shared Native tests, Compose framework, decoder
tests, Xcode app build, simulator launch and local SQLite schema 19.
The app and startup screenshot were uploaded as artifact 14387815249.
Only runner teardown was pending at the last check. The test XML archive
is 14385434888; the local artifact download returned HTTP 403, so no
independent visual review or per-test count is claimed.

Push the queued 195–203 feature checkpoints with this record and verify
the new exact head. Native artwork, Dream Routes, baseline/assessment and
other newly queued changes were not part of c3f0ec4. Confirm their compile
and startup, verify core iOS user flows and finish platform parity/full
release gates before merging draft PR #24. The earlier formatter HTTP
429 remains to verify on the new head.

### Latest recovery pointer — checkpoint 203 shared completed assessment

Android and iOS now share the optional completed baseline assessment form.
iOS validates the local date/time, retains start and finish offsets, and
saves manually entered evidence through the existing baseline coordinator.
The app and shared Compose Android Kotlin compilation passed, along with
pinned formatting and diff checks. Native runtime remains unverified.

PR #24 remains draft at c3f0ec4. Experiment 44208542517 passed Android,
Apple shared tests, framework, native UI/decoder tests and the simulator
application build. Its simulator launch/database gate was still active.
Preserve the runner; inspect its result before pushing checkpoints
195–203. Continue native verification and remaining feature parity.

### Latest recovery pointer — checkpoint 202 shared manual running baseline

iOS Running baseline now opens the existing optional corrections form.
Android and iOS share presentation and input validation, and both save
through the existing baseline coordinator. Android debug compilation,
pinned formatting for changed scoped files, and diff checks passed.

PR #24 is draft at c3f0ec4. Experiment 44208542517 passed Android and the
Apple shared, framework, decoder and application build steps. Simulator
startup was still active at last check. Inspect the final launch and
artifact results, then push the queued checkpoints 195–202 with any
concrete fixes. Native manual baseline and optional assessment parity
remain open.

### Latest recovery pointer — checkpoint 201 extra-run review on iOS

Android and iOS now share the actual-run association and easier-workout
preview. iOS reads the retained recording mode and delegates confirmed
links and plan revisions to the existing coordinators. Android debug
Kotlin compilation and touched-file formatting/diff passed. Native
presentation and persistence testing remain pending.

PR #24 remains draft at c3f0ec4. Experiment 44208542517 passed Android
and the Apple shared tests. Apple framework linking was still active.
Inspect that step, decoder tests and simulator results before pushing
the queued checkpoints 195–201. Continue native parity and final gates.

### Latest recovery pointer — checkpoint 200 Dream Route finalisation regression

The shared recording contract now exercises an opted-in Dream Route from
run start through atomic finalisation. It verifies a later settings change
does not replace the frozen route, failed finalisation leaves no assignment,
and retry preserves the original route and recorded distance. All 12
focused JDBC recording inventory tests and pinned formatting passed.

PR #24 remains draft at c3f0ec4. Experiment 44208542517 passed Android and
the Apple shared tests, then entered Apple framework compilation.
Preserve the job and inspect framework, decoder, packaged assets and
simulator startup results. Push checkpoints 195–200 after that run
finishes, with any concrete compile fixes.

### Latest recovery pointer — checkpoint 199 offline Dream Route basemaps on iOS

iOS reads the original, bounded offline basemaps for saved course editions.
It renders roads, water and green areas in the same course frame, and
retains the course and equivalent section if the map is unavailable.
The target bundles the original notice and ODbL text. The Apple packaging
gate checks the map, notice and licence. The six maps reproduced exactly,
all 11 map-source tests passed, and formatting, YAML and diff checks passed.
Native compile, packaging and rendering are pending.

PR #24 is draft at c3f0ec4. Experiment 44208542517 passed Android and was
still running on Apple at last check. Preserve that job, inspect its
framework, native test and simulator results, then push checkpoints
195–199 together. Continue feature parity and full release verification.

### Latest recovery pointer — checkpoint 198 Dream Route city artwork

The retained course artwork metadata is now shared by the Android and iOS
presentations. iOS bundles the original five city skylines and landmark
posters, and selects labels from the saved course edition. The Apple
artifact gate checks that original artwork is packaged. Artwork provenance
verified all 20 assets and all 14 artwork tests passed. Android debug
Kotlin compilation, touched formatting, workflow YAML and diff checks
passed. Visual proof and basemap parity remain pending.

PR #24 is draft at c3f0ec4. Experiment 44208542517 was still running both
jobs. Preserve it and inspect the actual framework, decoder and simulator
results. Push checkpoints 195–198 together once the live run finishes,
then verify bundled artwork rendering, continue remaining feature parity
and complete release gates. No premature merge or publication.

### Latest recovery pointer — checkpoint 197 Dream Routes for new iOS runs

iOS Settings lets athletes opt in and choose eligible cities. Fresh iOS
settings default off; saved/restored settings retain the original backup
format. Recording freezes the chosen course geometry in the start origin;
existing finalisation saves the equivalent distance. The selected hilly
race strategy is now included in the frozen execution prescription as
well as live cues, correcting a mismatch found while tracing run start.

Ten focused JDBC core backup tests, shared UI Android Kotlin compilation,
pinned touched formatting and all 19 schema checks passed. Native
recording, Dream Route rendering and city artwork parity remain open.

PR #24 is draft at c3f0ec4. Experiment 44208542517 still had both jobs
running at last check. Preserve and inspect that run before pushing
checkpoints 195–197. Continue native compiler/test/simulator verification
and parity before full release gates. No merge or publication.

### Latest recovery pointer — checkpoint 196 saved Dream Routes on iOS

iOS run details read and display existing Dream Route assignments, including
restored assignments, with their versioned course geometry and equivalent
distance. They leave the assignment and actual GPS evidence untouched.
Nine focused JDBC core backup tests passed, including scoped read-only
assignment lookup. Pinned formatting, diff and 19 schema checks passed.
Native rendering, new Dream Route opt-in/assignment, and city artwork
parity remain open.

PR #24 remains draft at c3f0ec4. Experiment 44208542517 still had both jobs
running. Preserve it, inspect framework, decoder and simulator results,
then push local checkpoints 195–196 with any concrete fixes. Continue
remaining native feature parity and release gates before merge.

### Latest recovery pointer — checkpoint 195 iOS exercise artwork and routines

iOS exercise details and browse thumbnails use the same reviewed bundled
artwork and technique phase labels as Android, with text or initials when
the image is unavailable. Training trends opens the shared warm-up and
mobility library, reusing the completed routine storage path. All 223
original artwork files passed the provenance audit, and pinned formatting
and diff checks passed. Native build, rendering and runtime are pending.

PR #24 is draft at c3f0ec4. Experiment 44208542517 was running on Android
and Apple. Separate formatting workflow 44208542541 failed on Maven
Central HTTP 429 after the formatter tests passed. The installed pinned
formatter passed locally over 951 Kotlin and 95 Java files. Preserve the
running experiment, inspect native compiler/test/simulator results, then
push this checkpoint with any concrete fixes. Confirm iOS artwork renders
and routine completion persists, then continue remaining parity and gates.

### Latest recovery pointer — checkpoint 194 iOS routine completion and guidance artwork

iOS race warm-ups now save explicitly completed routines in the existing
backup-compatible coach_support table. The iOS target references the
existing WebP guidance directory and the routine uses its original artwork
and descriptions, with written guidance if artwork is missing. The eight
focused JDBC core backup tests passed, including completed routine restore
and rejection of unknown routine IDs. Formatting, workflow YAML, diff and
all 19 schema boundaries passed. Native UI and Xcode packaging are pending.

Experiment 44205729778 passed Android and Apple Native tests, then failed
while compiling the Apple framework because a Kotlin Objective-C subclass
cannot own companion fields. Those GPX parser constants now live on the
decoder object. Push checkpoints 190–194 and inspect the next framework,
decoder test, bundled artwork and simulator gates. Continue full native
feature parity and release gates. PR #24 must remain draft.

### Latest recovery pointer — checkpoint 193 persistent race-week checklist

Race Day uses a shared checklist on both platforms. iOS persists choices
under the existing race/date/item identity and release08 backup format.
The checklist waits for saved state, reports failures, and keeps the
previous choice if saving fails. The seven focused JDBC core backup tests
passed, including legacy checklist reads and app-backup replacement.
Android debug Kotlin compilation, formatting of changed shared files,
diff checks and all 19 schema boundaries passed. Native testing is pending.

PR #24 remains draft at 73026cc. Experiment 44205729778 was still running
both jobs. Preserve it, inspect native compile, decoder and simulator
results and then push checkpoints 190–193. Next complete routine
illustrations/completion parity and the rest of the platform and release
gates. Do not merge or publish before acceptance.

### Latest recovery pointer — checkpoint 192 shared Race Day

Android and iOS now share the existing race prediction, pacing strategy,
taper and preparation screen. iOS can open the warm-up instructions and
results, and pass the hilly-course choice into live race cues. The selected
plan version is checked before iOS starts the linked race. Android still
uses its existing race-week checklist and routine navigation. iOS checklist
persistence and full routine completion/illustration parity remain open.

Android app and instrumentation Kotlin compilation, pinned formatting and
diff checks passed. Native compilation and runtime remain unverified.
PR #24 is draft at 73026cc and experiment 44205729778 still has both jobs
running. Preserve it, inspect native compile, decoder and simulator
results, then push checkpoints 190–192 together. Continue race checklist
and routine parity alongside the remaining release gates.

### Latest recovery pointer — checkpoint 191 iOS entered-result management

The iOS results page shows the 20 most recent entered results by default,
with access to the full list. An explicit confirmation removes only an
entered result, never its linked run. Missing run links are explained.
The focused JDBC activity repository suite passed, including the new
linked-run and derived-evidence preservation case. Pinned touched-file
formatting and diff checks passed. Native UI testing is pending.

PR #24 remains draft at 73026cc. Experiment 44205729778 is still running
on Android and Apple. Preserve both jobs. Inspect the actual native compile,
decoder test and simulator outputs before pushing checkpoints 190–191.
Continue native feature parity and full gates before merge or publication.

### Latest recovery pointer — checkpoint 190 running personal bests on iOS

iOS recorded results now show the existing source-precedence rankings and
recorded personal-best progression by distance. Android and iOS share the
ranked row, source labels and ranking helper. Android app and instrumentation
Kotlin compilation passed; pinned formatting and diff checks passed.
Emulator and simulator runtime tests for this checkpoint remain pending.

PR #24 is draft at 73026cc. Experiment 44205729778 is running on Android
and Apple. The separate Android workflow 44205729789 passed on that head.
Preserve the ongoing experiment and inspect its actual results before
pushing this checkpoint. Next verify native framework compilation, GPX
decoder Native results and simulator startup, then continue
remaining feature parity and full release gates. No merge/publication.

### Latest recovery pointer — checkpoint 189 race results and Apple picker compile

The existing Android race-result editor is now shared. iOS Training trends
opens recorded results, with official/manual source labels, date, notes,
and optional run links. Both platforms persist through the existing
evidence coordinator. Android debug Kotlin compilation, formatting and
diff checks passed. Native race-result workflows remain unverified.

Experiment 44201909878 passed Android and the Apple shared tests, then
failed compiling the Compose framework because UIDocumentPickerModeImport
was used as a top-level UIKit constant. The picker now uses the enum member.
Push checkpoints 182–189 as one batch and verify the next exact-head native
compile, shared UI decoder tests, simulator launch and production document
flows. Continue remaining platform parity and full gates before merge.

### Latest recovery pointer — checkpoint 188 shared recorded-run summary

Android and iOS now use the same run summary and recorded heart-rate cards.
iOS shows elapsed average pace and separately retained moving time, and can
record shoe observations through the existing coordinator. Android keeps
its current page, formatters and instrumentation entry points. Android
app and instrumentation Kotlin compilation, touched formatting and diff
checks passed. Runtime UI tests for this commit are pending.

PR #24 is at 9bc0737. Experiment 44201909878 passed Android and Apple was
still running at last check. Preserve that job; inspect its result, then
push local checkpoints 182–188 together. Continue with the new Apple
compile, decoder and simulator gates, and verify the native GPX, shoes,
notes and summary flows. Keep the PR draft until full acceptance.

### Latest recovery pointer — checkpoint 187 shared run notes and effort

Android and iOS now use the same run notes and optional 0–10 effort form.
iOS saves via the existing running-evidence coordinator, showing refreshed
notes and operation feedback. Recorded GPS/timing data is untouched.
Android debug Kotlin compilation, pinned formatting and diff checks passed.
iOS runtime verification is pending.

Experiment 44201909878 on PR #24 head 9bc0737 passed the Android job.
Apple was still running at last check. Preserve that job; inspect its
result and push local checkpoints 182–187 together. Verify the next
native decoder tests and simulator launch, then check the GPX picker,
shoe assignment and notes in the running app. Resume remaining native
parity and final gates. Do not merge the draft prematurely.

### Latest recovery pointer — checkpoint 186 shared shoe management

Android and iOS now use the same shoe list, mileage cards and editor.
Android retains its existing purchase-date picker. iOS opens shoes from
Settings and can assign or clear shoes from recorded run details. Both
use the existing shared evidence coordinator and SQLite repositories.
Touched-file formatting, diff checks and Android debug Kotlin compilation
passed. iOS compilation, layout and runtime assignment remain pending.

PR #24 is still at 9bc0737. Experiment 44201909878 remained in progress
on both platforms at last check. Inspect the final results and then push
the queued checkpoints 182–186. Verify native GPX and shoe flows with the
next simulator build, then continue the remaining native parity and full
release gates. Keep PR #24 draft.

### Latest recovery pointer — checkpoint 185 GPX decoder fidelity

The Apple decoder preserves empty segments for consistent track identity
and rejects structural elements in unrelated XML namespaces, nested
measurement markup and partial results after a parser rejection. Added
native cases for those conditions. Touched-file formatting and diff
checks passed. Native tests require the macOS workflow.

PR #24 remains draft at 9bc0737. Experiment 44201909878 was still running
both Android and Apple jobs. Preserve it, inspect its conclusions and
push local checkpoints 182–185 as one batch. Check the new Apple decoder
test XML and simulator startup artifact, then exercise the document
picker and continue the remaining native parity and full release gates.

### Latest recovery pointer — checkpoint 184 GPX re-import and native test gate

Shared GPX import now finds exact retained coordinates and timestamps of
a previously recorded run within the existing ten-minute dedup window.
The original activity is reused. The lookup reads only finished native or
GPX runs with matching point counts. The focused JDBC suite passed 12 tests
after correcting the test fixture to use distinct GPX wall timestamps.
Pinned formatting, schema parity, diff and workflow YAML parsing passed.

The next macOS experiment runs the shared UI tests, including the Apple GPX
decoder tests, after linking the framework and before building the iOS app.
Native build, decoder and picker results are pending. Preserve experiment
44201909878 on remote head 9bc0737. Both jobs were still running at last
check. After inspecting their results, push checkpoints 182–184 together,
inspect the new Apple test and simulator artifacts, and continue platform
parity and full release gates. PR #24 stays draft.

### Latest recovery pointer — checkpoint 183 iOS GPX file import

iOS Settings opens the existing system document picker for GPX. The native
decoder caps input at 24 MiB, disallows DTDs/entities, and reads one
timestamped track with the existing point limit. GPX import uses the same
exclusive operation owner as backup/restore and retention, then refreshes
history. The focused JVM recording inventory suite passed 12 tests after
routing import through that owner. Pinned formatting, plist parse, diff
and all 19 shared schema checks passed. Native decoder tests were added;
they have not run on Linux.

PR #24 remains at 9bc0737. Preserve experiment 44201909878 while it runs.
After inspecting its result, push local checkpoints 182–183 together.
Verify native decoder compilation and tests with sharedUi iosX64Test (or
iosSimulatorArm64Test on ARM), then launch the app and exercise the picker.
Exact re-import of a native recorded run and cross-platform GPX behavior
remain to verify. Continue native parity and final gates before merging.

### Latest recovery pointer — checkpoint 182 shared GPX import storage

Shared SQLite can ingest timestamped GPX track segments through the
existing recording engine and atomic finalisation. It preserves segment
breaks, avoids inventing distance across gaps, leaves moving time unknown,
deduplicates identical imported tracks and protects unfinished recordings.
The focused JDBC recording inventory gate passed 12 tests. Pinned touched
formatting, diff checks and 19 shared schema boundaries passed.

PR #24 is at 9bc0737. Experiment 44201909878 is running Android and Apple.
Keep this run intact. Next wire a bounded iOS XML decoder and document
picker to the tested storage path. Confirm duplicate behavior for exported
native runs and verify Android/iOS interoperability before claiming GPX
import parity. Continue native simulator startup and full release checks.
This checkpoint does not yet expose GPX import on iOS.

### Latest recovery pointer — checkpoint 181 shared GPX export and Apple compile fix

The iOS recorded-route view can export retained GPX via the existing native
document picker. Android and iOS now use one writer for coordinates,
timestamps, elevation and segment boundaries. Removed or insufficient
detail cannot be exported. The focused GPX writer test and Android debug
Kotlin compilation passed. Native document picker behavior and GPX import
on iOS remain pending.

Experiment 44198176072 ended with Android success and Apple Native test
success, then failed compiling the Compose framework on the unresolved
AVSpeechBoundaryImmediate import. The call now uses the AVSpeechBoundary
enum member. Push the queued batch and check the new Apple compile before
claiming a fix. Continue simulator startup and the unfinished native
feature parity and full verification. Keep PR #24 draft.

### Latest recovery pointer — checkpoint 180 truthful iOS run history

Run details now read the stored recording-removal receipt alongside the
summary. Removed detail gives an explicit explanation in both route and
split views without calculating splits or rehydrating GPS samples.
The shared route reader recognizes receipts by activity even when no
finished session header is available.

The focused JVM retention and recording inventory gates passed 21 tests.
These include retained distance, empty samples and coordinates, and
explicit removal provenance after cleanup. Pinned formatting, diff checks
and 19 migration boundaries passed.

Remote PR #24 head is 7558809. Android completed successfully on run
44198176072. Apple was still compiling and testing at the last check.
Preserve this run and inspect its final result before pushing checkpoints
174–180. Native startup, backup picker, storage and retention dialogs still
need simulator verification. Continue platform parity and full gates
before merge or publication.

### Latest recovery pointer — checkpoint 179 iOS retention cleanup

iOS can separately confirm removal from the reviewed preview for a saved
retention choice. The application backup owner checks approval and delegates
to the existing per-recording transaction, yielding between recordings.
The storage panel refreshes totals and reports committed progress if a
later recording fails approval. Existing-user consent and active recording
guards remain in the domain and transaction. Native behavior is unverified.

The focused JDBC retention gate passed 10 tests, including stale approval,
backup overlap, idempotent retry and preserved summary evidence through the
actual iOS storage owner. Formatting, diff checks and 19 shared schema
boundaries passed.

Local checkpoints 174–179 await the existing run 44198176072 on PR #24 head
7558809. Android reached artifact upload and Apple was still compiling and
testing. Inspect their actual conclusions before pushing and fix any errors.
Next verify native simulator startup, complete backup restore and retention
dialogs, including cancellation, active recording and truthful removed-route
presentation. Remaining platform parity and full gates still block merge.

### Latest recovery pointer — checkpoint 178 iOS retention choice and preview

PR #24 remains at 7558809. Jobs in experiment 44198176072 were still
running at the last check. Preserve them and inspect the results before
pushing the queued local checkpoints.

iOS Settings now reads and explicitly saves the three existing retention
policies and offers a read-only eligibility preview. Backup, replacement,
policy saving and preview use the same operation owner. Saving a choice
leaves all recording evidence intact. The focused JVM gate passed 19
tests, including the new overlapping-export and unchanged-evidence
contract. Pinned formatting, diff checks and all 19 schema boundaries
passed. Native UI validation is pending.

Next verify the Apple build and actual simulator launch. Wire separately
confirmed cleanup through this same owner and the existing per-recording
transaction, preserving cancellation between batches and reporting any
partial progress. Verify stale previews, recording guards and removed
detail presentation before promoting the feature. No merge or publication.

### Latest recovery pointer — checkpoint 177 iOS recording storage

A read-only Recording storage disclosure in iOS Settings now uses the
existing SQLite inventory under the same operation owner as backup and
replacement. It reports retained summaries, points, events and logical
recording content bytes. It does not claim physical device usage or expose
cleanup. The focused JDBC inventory suite passed in 1m 20s; schema parity,
touched-file formatting and diff checks passed. Native rendering is pending.

PR #24 remains at 7558809 with experiment 44198176072 still running at
last check. Preserve the live jobs. After they finish, inspect their
results, correct any root cause and push checkpoints 174–177. On macOS,
verify simulator launch, backup picker confirmation and storage disclosure.
Next connect informed retention preferences and read-only preview through
the same owner. Require explicit guarded approval before exposing deletion.

### Latest recovery pointer — checkpoint 176 iOS complete backup import

PR #24 is at 7558809. The Android and Apple jobs on experiment run
44198176072 were still running. Check both before pushing the local batch.

The iOS document picker accepts JSON app backups and complete ZIP packages.
ZIP inspection validates and stages recording detail privately before the
explicit replacement confirmation. The existing full-package transaction
runs under the same owner as app restore and export. Staging closes on
cancellation and after restore. The JDBC app and complete-backup contracts
passed in 1m 21s. All 19 schema statements/boundaries match Android and the
touched-file formatting and diff checks passed. Linux cannot compile iOS.

Next check Apple compilation and simulator launch, then exercise invalid,
cancelled and confirmed JSON and ZIP imports in the native picker. Wire the
existing read-only storage inventory under the same operation owner before
enabling any retention changes. Cleanup and reset remain unexposed.

### Latest recovery pointer — checkpoint 175 iOS app backup import

PR #24 remains at 7558809. Experiment 44198176072 was still running Android
APK compilation and Apple Native tests. Preserve both useful jobs.
Local checkpoints 174–175 are queued.

175 adds the native JSON import picker and bounded 32 MiB read. The file
is validated before the app presents explicit replacement confirmation.
A confirmed restore uses the shared transaction and backup owner. On success
the iOS host reopens its session so the previous programme and workout
controllers cannot retain old state. No raw detail is deleted by JSON
replacement. Complete ZIP restore, retention controls and destructive reset
remain unavailable. Native picker and restart behavior require macOS testing.

Pinned touched-file formatting, diff and all 19 schema checks passed.
Next inspect 44198176072, fix actual build/launch failures and push the
queued batch after the live jobs finish. Verify cancel, invalid file, active
recording guards and successful restore on iOS, then continue remaining scope.

### Latest recovery pointer — checkpoint 174 guarded backup review

PR #24 is at 7558809. Experiment 44198176072 was running Android and Apple.
Formatting run 44198176127 failed downloading its pinned formatter with
HTTP 429. The same full check passed locally (936 Kotlin and 95 Java files).

174 unifies app-backup review, export and confirmed replacement under the
same application backup owner. JSON review is read-only and returns an
owner-bound validated snapshot. Replacement uses the existing transaction,
including the live recording and unfinished gym checks. A focused JDBC
contract verifies review leaves data intact, rejects a different owner and
overlapping export, and restores successfully after confirmation. The
JDBC suite passed in 1m 46s. All 19 schema checks passed.
Log: /workspace/scratch/kmp174-backup.log.

Next connect the iOS import picker and explicit confirmation to the reviewed
backup API. Replacement remains unavailable in the UI at this checkpoint.
Complete-package restore and cleanup still need application guard wiring.
Check live CI before pushing, verify macOS compile and actual simulator
startup, and continue the remaining routes and release gates.

### Latest recovery pointer — checkpoint 173 shared heart-rate trends

The draft PR remains at 25adcc6. Experiment 44195464653 has a completed
Apple compile failure, corrected locally as documented below. The Android
instrumentation job was still running when this batch began. Inspect the
live job before pushing the queued changes.

173 shares the existing heart-rate panel across Android and iOS. The new
iOS Trends metric shows retained averages and maxima for the chosen range
and preserves explicit missing readings and zone-evidence limits.
Shared metadata and Android debug Kotlin compilation passed in 1m 59s,
and pinned formatting, diff and all 19 schema checks passed.
Log: /workspace/scratch/kmp173-heart-rate-trends.log.

Next push the queued batch once preceding Android instrumentation ends,
then verify the new Apple compile, app build and simulator startup.
Continue data-operation safeguards and the remaining platform routes.

### Latest recovery pointer — Apple compiler corrections

The Apple job in experiment 44195464653 passed Native tests and failed at
shared UI compilation. Its logs identify two source issues, both corrected
locally: the calendar needed a named history callback after the signature
added a strength-only flag, and the exercise library lacked the coroutine
cancellation import. Pinned formatting and diff checks passed. A fresh
macOS compile is required. Android instrumentation was still active at the
last check. Keep the queued batch local until that useful run ends.

### Latest recovery pointer — checkpoint 172 shared running trends

PR #24 remains at 25adcc6. Experiment 44195464653 reached Apple framework
linking. Check its current result before pushing queued checkpoints 167–172.

172 shares the Android weekly chart and trend aggregation with iOS. The iOS
Today route opens measured weekly distance, elapsed pace and running time,
with date range and metric selection. Android retains its existing formatting
and history inputs. The iOS heart-rate trends panel remains to be wired.

Shared metadata and Android debug Kotlin compilation passed in 1m 58s.
Pinned formatting, diff and all 19 schema checks passed. Apple compilation
and app startup remain pending. Log: /workspace/scratch/kmp172-trends.log.

Next inspect 44195464653 and fix actual macOS or launch errors. Push the
queued commits after that useful run completes, then continue data-operation
safeguards, remaining native feature parity and full Android/iOS verification.

### Latest recovery pointer — checkpoint 171 recorded milestones

PR #24 remains at 25adcc6. Experiment 44195464653 was still running Android
instrumentation and Apple Native tests at the last check. Local 167–171
are queued so those jobs can complete.

171 mounts the shared achievements screen from iOS Today and Gym. It uses
retained running and strength evidence, shared recognition and celebration
settings, and links to the source workouts. Shared error handling now also
surfaces checked SQLite failures while preserving coroutine cancellation.
Common UI metadata compiled in 32s, and pinned touched-file formatting,
diff check and all 19 schema checks passed. Apple compilation and simulator
behavior await macOS CI. Log: /workspace/scratch/kmp171-achievements.log.

Next inspect the active jobs, address demonstrated issues and batch the
pending commits. Continue running trends, remaining native services and
data-operation safeguards before the full Android and iOS gates. Keep draft.

### Latest recovery pointer — checkpoint 170 Bluetooth heart rate on iOS

PR #24 remains at 25adcc6. Both experiment 44195464653 jobs were running at
the last check. Local checkpoints 167–170 are queued.

170 adds explicit, time-limited discovery of standard Bluetooth heart-rate
monitors. The chosen monitor connects during the run. Valid readings feed
the original metrics engine and live panel, and stale or disconnected
readings disappear. The recorder disconnects at finish/cancellation. Bluetooth
usage text is in Info.plist. Kotlin formatting, plist parsing, diff and all
19 schema checks passed. Apple SDK compilation and real monitor pairing,
reconnect and locked-screen behavior remain unverified.

Next inspect the active CI logs for actual build/startup failures, batch
the pending changes and verify native compilation on macOS. Continue the
remaining iOS routes, service parity and release gates. Keep PR #24 draft.

### Latest recovery pointer — checkpoint 169 iOS background location

PR #24 remains at 25adcc6. Android and Apple jobs in experiment 44195464653
were running at the last check. Local checkpoints 167–169 are queued.

169 configures the iOS location background mode and enables background GPS
delivery only within a user-started recording, with the existing When In Use
permission and location indicator. Stopping the source disables background
delivery. Batched fixes now keep their shared receipt clock throughout cue
publication, avoiding a backwards-time exception on the next fix in the batch.
Pinned touched formatting, plist parsing, diff and all 19 schema checks passed.
Native compilation and actual locked-screen recording remain unverified.

Next inspect the active CI result, correct demonstrated failures, then push
the queued batch. Continue BLE, remaining iOS routes and release verification.
Do not count simulator startup as physical recording acceptance.

### Latest recovery pointer — checkpoint 168 spoken Live Coach cues

PR #24 remains at 25adcc6. Both platform jobs in experiment 44195464653
were running at the last check. Local checkpoints 167 and 168 are queued.

168 adds optional on-device speech for the already-selected coaching cues.
Athletes opt in before each run. Pause, finish and recorder cancellation
stop speech, and busy speech does not queue stale cues. Pinned touched-file
formatting, diff check and all 19 schema boundaries passed. Apple SDK
compilation and speech behavior remain unverified.

Next inspect 44195464653 for actual compiler or simulator launch failures,
batch corrections with 167–168, and push once the useful run finishes.
Continue native recording lifecycle, BLE, remaining navigation and data
safeguards before full platform verification. PR #24 remains draft.

### Latest recovery pointer — checkpoint 167 visual Live Coach cues

PR #24 is at 25adcc6. Experimental run 44195464653 was still running
on Android and Apple. The separate formatting job failed on the one-line
AppleAppHost route predicate. The pinned formatter fixed that locally.
Preserve the running simulator verification before pushing.

167 connects the shared cue coordinator to iOS live recording. The panel
shows selected cues and race context, using the chosen distance units.
Pace events are retained with the recording. Audio and device verification
remain pending. Pinned touched-file formatting, diff check and all 19
schema boundaries passed. Native compilation awaits CI. Next inspect run
44195464653 for actual build or launch failures, batch any corrections and
push to the existing draft PR. Continue native audio and services and
the remaining release gates.

### Latest recovery pointer — checkpoints 157–166 delivered

Experiment 44189952813 at 2172096 finished successfully on both Android
and Apple, including Native tests, framework linking and the first actual
Xcode simulator application build. That run did not contain the later
simulator launch/database startup gate. Do not claim startup acceptance.

The ten local checkpoints 157–166 are now queued for one normal push.
They contain shared history, calendar, running plan, Gym Now and programme
authoring, exercise library, Today, body guidance, route/splits and planned
execution. The prior entries record their actual focused verification.
Inspect the new head CI before accepting Apple compilation of this batch.

Next fix actual new-head Native/Xcode failures, verify simulator launch and
startup screenshot, then continue missing native audio/BLE/background
recording, full navigation and data-operation safeguards. PR #24 stays draft.

### Latest recovery pointer — checkpoint 166 planned run execution

Draft PR #24 remains at 2172096. Experiment 44189952813 passed Android,
Apple Native tests and framework linking. Xcode app build was running at
the last check. Preserve it. Local checkpoints 157–166 are queued.

166 binds iOS planned runs to the existing WorkoutExecutionCoordinator
and retained plan version/workout prescription. Step state drives the
shared recording panel. Explicit complete-open, skip and finish-early
commands preserve the original step events in staged execution context
and the normalized event stream. The existing SQLite finaliser links
the saved actual. Audio cues and physical device behavior are pending.

Shared UI common metadata compiled in 47s (18 tasks). Pinned formatting,
diff and exact 19-schema boundaries passed. Log:
/workspace/scratch/kmp166-planned.log. Native runtime for this batch is pending.

Next inspect the actual Xcode result, fix build errors, push the local
batch after CI finishes and verify the simulator launch gate. Continue
native cues, background/BLE, full planned detail and remaining app routes,
data lifecycle safeguards, accessibility and final platform verification.
Keep PR draft.

### Latest recovery pointer — checkpoint 165 retained routes and splits

Draft PR #24 remains at 2172096. In experiment 44189952813 Android and
Apple Native tests passed. The Apple framework link was still running at
last check. Preserve it. Local checkpoints 157–165 are queued.

165 adds saved-route and split disclosures to iOS run details. The shared
repository uses the existing paged point reader and original coordinates.
It distinguishes explicit removal receipts from unavailable coordinates.
Split calculations use the existing evidence-qualified calculator and chosen
units. JVM SQLite contract asserts the retained coordinates round-trip
and unknown activities return no route. All 11 focused JDBC tests passed in
1m 18s. Formatting, diff and 19 schema boundaries passed. Log:
/workspace/scratch/kmp165-route.log. Apple runtime is still pending.

Next inspect the preceding Apple link and Xcode build results. Fix actual
errors, push the local batch and verify simulator startup. Continue the
structured running integration and remaining native features and acceptance.
Do not merge or publish yet.

### Latest recovery pointer — checkpoint 164 body guidance and simulator startup gate

Draft PR #24 remains at 2172096. Experiment 44189952813 passed Android and
Native tests. Apple framework linking was still running at the last check,
with the first Xcode build following. Preserve that run. Local checkpoints
157–164 are queued.

164 mounts the original shared body measurements and protein screen on iOS
with manual editing, retained sources and explicit usable-weight selection.
The body-only Apple Health action checks selected height/weight categories
and reads through the existing visible-only import pipeline without reading
workouts. It does not infer HealthKit grants. Add weight opens this screen.

The next Apple CI run now builds and installs the simulator application,
launches it, checks that the actual local SQLite database opens at schema
19, and captures a startup screenshot. Pinned Kotlin formatting, workflow
YAML/Bash syntax, diff and shared-schema checks passed locally. No simulator
launch or SDK runtime pass has been observed for this change.

Next inspect 44189952813 for actual SDK/Xcode compiler output, then batch
the local commits after that useful run completes. Fix demonstrated issues
and verify the new launch gate. Finish remaining iOS features and release
gates before merge. Physical Apple and Android acceptance remains unperformed.

### Latest recovery pointer — checkpoint 163 Running and Hybrid Today

Draft PR #24 remains at 2172096. Experiment 44189952813 Android passed;
Apple Native tests passed and framework linking was underway at the last
check. The Xcode app build follows. Preserve that useful run. Checkpoints
157–163 are local and should be batched after observing the real CI result.

163 mounts the shared Running/Hybrid Today dashboard using actual date, unit,
activity and programme bindings. Weekly review choices and baseline summary
use existing shared content and application actions. Apple defers routes for
structured Live Coach, running trends, achievements and add-weight until
their screens are connected. Android keeps all original actions by default.

Shared metadata and Android debug Kotlin compilation passed in 2m 2s (62
tasks). Pinned touched Kotlin formatting, diff check and all 19 schema
boundaries passed. Log: /workspace/scratch/kmp163-home.log. Native build for
the local batch remains pending. Next inspect 44189952813 for demonstrated
compile/build failures, batch the push, and verify actual simulator launch.
Continue remaining app routes and native services before full verification.

### Latest recovery pointer — checkpoint 162 strength overview and library

Draft PR #24 remains at 2172096. Experiment 44189952813 Android passed and
Apple was still running its Native test step at the last check. Preserve
the run. Local checkpoints 157–162 are queued.

162 mounts the shared Strength Today, exercise search and detail tabs, and
strength volume analytics. Actual history, exclusions, favourites, custom
metadata and source workout lookups use existing repositories and commands.
iOS displays the original written technique with a thumbnail placeholder
until the reviewed illustrations are ported. PBs are accessible per exercise.
There is no dedicated iOS PB index yet. Pinned touched Kotlin formatting,
diff check and exact 19-boundary schema check passed. Apple compile and runtime
remain pending.

Next check 44189952813 again. Correct actual Native/Xcode errors and push the
local batch when the useful run finishes. Verify an actual simulator launch.
Continue running/hybrid Today, full workout detail, remaining native services
and cross-platform acceptance before merge. No publication or hardware claim.

### Latest recovery pointer — checkpoint 161 strength programmes on iOS

Draft PR #24 remains at 2172096. Experiment 44189952813 Android passed;
Apple Native tests were still running at the last check. Preserve that run.
Checkpoints 157–161 are local.

161 mounts the existing manual builder, generated/template drafts and custom
exercise form from the iOS Gym screen. The coordinators use the same local
SQLite and existing version/activation safeguards. Private preferences retain
only the selected draft IDs. Explicit activation refreshes shared coaching
and Gym Now state. The Apple target text retains exact effort and optional
reference weights. Strict DD/MM/YYYY input uses the shared calendar validator.

Pinned touched Kotlin formatting, diff check and exact 19-boundary schema
check passed. Apple-only code awaits macOS compilation. Next inspect
44189952813, correct any actual build errors and push the coherent batch
after the previous gate completes. Verify iOS app build/launch. Continue
Strength Today, exercise browsing, progress, structured recording, remaining
native services and final Android/iOS verification. Keep PR draft.

### Latest recovery pointer — checkpoint 160 Gym Now on iOS

Draft PR #24 remains at 2172096. Experiment 44189952813 was still running
Android instrumentation and Apple Native tests at the last check.
Local checkpoints 157–160 are queued. Preserve the live run.

160 opens production shared Gym Now, workout builder and live workout screens
from the iOS host. It uses the same application SQLite, verified legacy
fingerprints, existing execution and association repositories, native monotonic
clock, and retains unsaved inputs separately in private preferences. The
rest timer and optional warm-up form are wired. Local rest notification
permission is explicit; iOS delivery, background timer and vibration behavior
are not verified. Programme authoring/generation, Strength Today and remaining
strength routes are still open. Android source is unchanged in this checkpoint.

Pinned touched-file formatting, diff check and exact 19-boundary schema check
passed. Apple-only source awaits the macOS gate. Next inspect 44189952813,
fix demonstrated compiler/build failures, and push the coherent batch when
the live run completes. Verify actual simulator build/launch and continue
shared navigation, remaining native services and final acceptance.

### Latest recovery pointer — checkpoint 159 running plan on iOS

Draft PR #24 remains at 2172096. Experiment 44189952813 was running
focused Android instrumentation and Apple Native tests at the last check.
Checkpoints 157–159 are local. Preserve the live run.

159 mounts SharedTrainingPlanScreen in the iOS host and connects the existing
generator, adjustment decisions, calendar prescription previews and optional
movement guidance. The iOS goal form uses the existing RaceGoal, preferences
and profile validation/persistence. Saving does not regenerate the plan
silently. Race and hybrid first-run paths lead to Plan. The native screen
still needs full planned detail editing/structured execution and durable
onboarding drafts for parity. Pinned formatter, diff check and all 19 schema
boundaries passed. This Apple-only checkpoint awaits macOS compilation.

Next read 44189952813 actual results and fix demonstrated errors. Then push
157–159 in one batch and verify the simulator app builds and launches.
Continue Gym Now/strength, running detail execution and native services.
No final acceptance, merge or publication yet.

### Latest recovery pointer — checkpoint 158 iOS Calendar

Draft PR #24 remains at 2172096. Experiment 44189952813 was still running
Android instrumentation and Apple Native tests at the last check.
Checkpoints 157 and 158 are local. Preserve CI before batching the next push.

158 mounts the existing shared Calendar with list/week/month navigation,
the SQLite-backed history list, device-local date handling and saved view
preference. Recorded rows open retained run details. Planned running and
strength rows open read-only prescription previews. Android keeps its
existing log-workout action and all calendar controls. Full planned-workout
editing, structured recording and Gym Now navigation are not yet wired on iOS.

Common metadata and Android debug compilation passed in 1m 58s, 62 tasks.
Pinned touched-file formatting, diff check and 19 schema boundaries passed.
Log: /workspace/scratch/kmp158-calendar.log. Apple runtime remains pending.

Next inspect the first Xcode build and fix actual Native or Xcode errors.
Launch the simulator application. Then continue wiring the existing running
plan, gym execution, health and activity detail flows. Full release gates and
physical verification remain open.

### Latest recovery pointer — checkpoint 157 shared activity history

Draft PR #24 remote head 2172096 has experiment 44189952813 running both
Android and Apple jobs at the last check. Preserve that run. It contains
the first Xcode app build gate. The latest changes below are local.

157 extracts the original bounded, paginated Android history list into
shared Compose. Android retains its date, distance, pace, controls and
instrumentation injection. The iOS host mounts the same list with actual
SQLite paging and viewport-independent activity detail reads. A saved run
opens its own retained summary. The first-run selector waits for the initial
snapshot. The Apple run detail is currently a summary, with remaining
route/split/detail actions still to be shared.

Common metadata and Android debug Kotlin compiled in 2m 1s (62 tasks).
Touched-file formatting, diff check and all 19 schema boundaries passed.
Log: /workspace/scratch/kmp157-history.log. Apple verification is pending.

Next inspect 44189952813 before pushing. Fix demonstrated Apple compile/build
errors, then launch the simulator app. Continue wiring running plan, Calendar,
strength, health and the remaining activity detail flows through existing
shared services. Full parity, native SDK coverage and release gates remain open.

### Latest recovery pointer — checkpoint 156 iOS application target

Experiment 44186678584 at a85fcf0 finished Android successfully. Apple Native
tests passed, then shared UI compilation failed on NSTimeZone.systemTimeZone
and the inherited read-only notification properties. The batched changes
replace the time-zone call with the existing Kotlinx calendar and use the
mutable notification setter methods. SDK verification of those fixes is pending.

156 adds iosApp/RaceDayDreams.xcodeproj, the SwiftUI entry, HealthKit entitlement,
usage strings and Compose AppleAppHost. The host opens one app-private SQLite
session and mounts shared first-run, recording/recovery and backup settings.
Explicit Apple Health selection and request/import are wired. The Apple CI job
now builds a simulator application after framework linking and archives the app
with executable permissions. Local plist/YAML parsing, formatting and diff checks
passed. Linux cannot verify xcodebuild. Check actual new CI before claiming success.

This is partial production wiring. The full shared navigation, Home, running
history/plan, strength and health screens, app lifecycle and remaining native
services are still unfinished. Maintain draft PR #24. Next diagnose the new
Apple compile/build results, launch the actual simulator app and continue the
existing screen integrations. Do not claim feature parity, release or hardware testing.

### Latest recovery pointer — checkpoint 155 iOS recording screen

Draft PR #24 is still at a85fcf0. Experiment 44186678584 had Android running
focused instrumentation and Apple running Native tests at the last check.
Preserve that run. Checkpoints 152–155 are local.

155 binds the existing SharedActiveRunPanel to AppleRunRecorder for ordinary
iOS runs. It uses stored units, explicit location permission, route geometry
prepared only for the route tab, and the shared finish confirmation. It also
adds interrupted save/discard UI backed by the recorder's single recovery flow.
No structured execution, BLE, background recording or Dream Route integration
is claimed for this screen. It needs an actual native navigation entry.
Pinned formatter, diff check and all 19 schema boundaries passed.
Apple compilation and runtime remain pending.

Next verify the running Apple gate, then push the coherent local batch and
fix actual SDK errors. Build the runnable Xcode target and navigation host
that mounts production shared screens including AppleRecordingScreen and
AppleSettingsScreen. Connect remaining platform SDK effects and complete
the original cross-platform acceptance gates. Do not publish this draft.

### Latest recovery pointer — checkpoint 154 shared settings cards

Draft PR #24 remote head a85fcf0 is still running experiment 44186678584
at the last check. Preserve the Android runtime and Apple test/link jobs.
Checkpoints 152–154 are local on experiment/android-ios-kmp-compose.

154 extracts the existing Android primary-focus and backup settings cards
into common Compose. Android keeps its actions, test tags, copy and layout.
AppleSettingsScreen uses those cards with the owned settings coordinator and
AppleBackupDocuments, including ordinary and complete export actions and
accessible result feedback. iOS still needs a navigation entry and app host.
Restore/reset/retention are not exposed by this screen.

Local shared UI metadata and Android debug Kotlin compilation passed in
2m 18s (62 tasks). Pinned touched-file formatting and diff checks passed.
Log: /workspace/scratch/kmp154-settings.log. Native compilation and runtime pending.
Next inspect Apple CI and fix any SDK issues, then push the coherent batch.
Continue the runnable Xcode app and shared navigation, including this settings
screen, then complete remaining services and full software/physical verification.

### Latest recovery pointer — checkpoint 153 Apple backup documents

Draft PR #24 remote head a85fcf0 remains under experiment 44186678584.
At the last check Apple was running Native tests and Android was compiling
the app and instrumentation APK. Preserve this run. Checkpoints 152 and 153
are local, with 152 at 804f119.

153 wires one AppleBackupDocuments instance into AppleCoachSession, sharing
the existing LocalBackupExports owner. It stages ordinary app JSON or a
streamed complete package in a private temporary directory, then presents
the native export-as-copy picker. Success, cancellation and staging errors
remove the temporary copy. No restoration, reset or retention control is
enabled. Pinned Kotlin formatter, git diff --check and exact 19-boundary
schema check passed. UIKit compilation and picker runtime remain pending.

Next inspect the preceding Apple compile output and fix SDK issues if found.
Integrate the export actions in the production iOS settings screen, compose
shared navigation and screens, and add the runnable Xcode target. Continue
remaining lifecycle and native services and full platform acceptance. The
export adapter has not been invoked by an actual iOS application yet.

### Latest recovery pointer — checkpoint 152 platform wiring

Draft PR #24 remote head a85fcf0 has checkpoints 149–151. Experiment
44186678584 was still running on both platforms at last check. Preserve it.
Checkpoint 152 is local and connects AppleCoachPlatform to AppleCoachSession.
Shared snapshot and settings callbacks now load and coalesce real reminder
work, use the owned HealthKit gateway, and use the device regional timezone
for gym actions and manual running times. Reminder errors are exposed for
the host. The session supplies one guarded backup-export facade to the
document adapter. It can stream complete backups and leaves destructive
controls unavailable. The actual document picker and app UI are still pending.

Pinned formatting and diff checks passed. Shared storage JVM compilation
passed in 41s, and all 19 schema migration boundaries match Android.
Log: /workspace/scratch/kmp152-platform.log. Apple source compilation is pending.

Next inspect 44186678584 and fix any actual Apple SDK compiler errors before
batching the next push. Implement the real document export adapter, production
screen/navigation composition and runnable Xcode app target. Reuse the owned
database and platform services. Full platform and physical gates remain open.

### Latest recovery pointer — checkpoint 151 Apple reminders

Checkpoint 148 at 86006bc passed Android and Apple experiment 44183141942.
Checkpoints 149–151 are delivered together to draft PR #24 on
experiment/android-ios-kmp-compose. Check live PR head and CI before pushing.

151 adds native User Notifications scheduling and preference storage on the
same SQLite connection as AppleCoachSession. The shared reminder calendar and
rules select actual versioned planned workouts. The Apple adapter replaces
only evening-run requests, checks actual authorization, and respects available
notification slots. Permission requests remain explicit. Android is untouched.
Pinned Kotlin formatting, datetime API inspection and exact schema check passed.
Apple compilation for the three new native checkpoints is pending CI.

Next wire the host snapshot and settings callbacks to AppleCoachReminders and
attach HealthKit, recording and existing screens to the runnable Xcode app.
Document IO, app-owned destructive operation guards, BLE, audio, app lifecycle
and full acceptance are still pending. Simulator/physical reminder delivery
has not been verified. Keep the PR draft until the original scope and gates pass.

### Latest recovery pointer — checkpoint 150 Apple application composition

Draft PR #24 remains at checkpoint 148 (86006bc). The Android job in
44183141942 passed. The Apple job reached framework linking after its
Native test step, and was still running at the last check. Preserve that run.
Local checkpoint 149 (a34ce07) contains the Apple recording bindings.

Checkpoint 150 adds AppleCoachSession, the lifecycle owner for the real
Apple SQLite database, shared coaching services, HealthKit gateway and
recording repository. It opens storage on IO, resolves interrupted recording
before its first snapshot, and cancels and joins owned work before closing
SQLite. It requires real platform bindings from the eventual host.
Pinned Kotlin formatter, diff check and exact 19-boundary schema check passed.
Apple compilation and runtime remain pending for local checkpoints 149–150.

Next: after the running Apple gate finishes, push the two local checkpoints
together on PR #24 and verify Apple compile errors against real SDK output.
Wire concrete host settings/reminders, document IO, navigation and shared
screens through AppleCoachSession. Add the runnable Xcode app target and
HealthKit/location configuration, then expand native services and run final
cross-platform gates. No runnable app target or public build exists yet.

### Latest recovery pointer — checkpoint 149 Apple recording bindings

Draft PR #24 remains on experiment/android-ios-kmp-compose. Remote checkpoint
148 at 86006bc passed the formatting workflow 44183142014. Experimental run
44183141942 was still running both platform jobs at the most recent check.
Keep that run in place while useful.

Checkpoint 149 is local. AppleRecordingLocation binds Core Location permission,
fixes and errors, preserving unknown sensor values and sending timestamped
observations through the existing delivery checks. AppleRunRecorder binds
the real SQLite staging repository and shared metrics engine to a host-owned
main scope. It adds periodic staging, pause/resume, and explicit interrupted
save/discard. Finish freezes the last time and retries with the retained header.
These are Apple-only files. Pinned formatter and git diff --check passed.
macOS compilation and runtime are pending, and no host currently invokes them.

Next verify 148 CI, then push 149 in a coherent batch. Continue actual iOS
application composition: existing AppleApplicationDatabase, SharedCoachApplicationServices,
real HealthKit controls and shared navigation/screens. Attach the recorder to
the app lifecycle, provide location usage strings and verify permission denial,
suspension and resume. Background recording capabilities, BLE, audio, native
document IO, app-owned data-operation guard and full platform acceptance remain
unfinished. Do not merge or publish until the actual iOS app and full gates pass.

### Latest recovery pointer — checkpoint 148 recording recovery

Checkpoint 145 at d00370e passed Android and Apple experiment 44178248333.
Completed Android logs show 641 unique matching starts and ends. Both CI jobs
succeeded, including Apple framework linking. This is the experimental gate.

Checkpoint 148 adds interrupted recording summaries and explicit save/discard
repository commands. Discard requires a matching RECOVERABLE header. It removes
only that recording and its staging context in one transaction. Recovery uses
the same retained event timeline as finalisation and preserves the last stored time.
Shared UI metadata compilation and 11 focused JDBC tests passed in 1m 56s.
All 19 schema boundaries match Android. Log: /workspace/scratch/kmp148-gate.log.

Checkpoints 146–148 are delivered together to the existing draft PR #24.
Verify the new head on Android and Apple CI. Next wire real Core Location
and monotonic clocks, native recovery UI, and the iOS app host. Physical
recording and the full app release gates are still outstanding.

### Latest recovery pointer — checkpoint 147 recording finalisation

The existing draft PR #24 still has checkpoint 145 at d00370e. Experiment
44178248333 was running on Android and Apple at the last check. Checkpoint
146 (ffa363b) and 147 are local. Push them together once the previous useful
run completes.

145 shared the recording coordinator. 146 added shared SQLite staging.
147 connects that coordinator contract to shared SQLite and finalises retained
points into the canonical activity, PBs, Dream Route assignment and structured
execution response in one transaction. A successful retry returns the same
activity. A late failure leaves staged evidence available for recovery.
The Android service and storage path still use their existing adapters.

Shared UI metadata compilation and all 10 targeted JDBC tests passed in 2m 7s. Log:
/workspace/scratch/kmp147-gate.log
Schema check confirms all 19 migration boundaries match Android.

Next: finish native recovery controls, bind the real Core Location and
monotonic clock effects, and wire the application host and remaining SDK
services. Verify real Apple SQLite finalisation and the full app gates before
merge. No new permissions or changes to the schema or backup formats.

### Latest recovery pointer — checkpoint 146 staging

Draft PR #24 on `experiment/android-ios-kmp-compose` has checkpoint 145 at
`d00370ee5b16676a7734b7b19f81cd0cd9465798`. [Experiment 44178248333](https://github.com/mmwiza1/Race-day-dreams/actions/runs/44178248333)
was running on both platforms at the last check. Formatting 44178248372 passed.

Checkpoint 146 adds shared SQLite session creation, point/event append with exact
retry checks, atomic context updates and recoverable state. It uses existing
normalized tables and point codec. The shared SQLite inventory contract now
tests duplicate retries and rollback of a new point after a late event conflict.
All 19 schema boundaries match Android. Focused JDBC suite passed 9 tests with no failures or skips. Log:
`/workspace/scratch/kmp146-staging-test.log`. Formatting applied to 904 Kotlin and 95 Java files.
Leave the prior CI run in place while it is useful.

Next: connect staging to the existing shared recording coordinator after implementing
real canonical finalisation, including activity/PB/association writes in one transaction.
Then bind native SDK effects and the iOS app host. Android continues to use its existing
storage/service. There is no native recording entry point yet.

### Latest recovery pointer — checkpoint 145 delivery

The workspace recovered with clean local commit `640c633`. Checkpoint 144
(`3e95c70`) passed both jobs in [experiment 44171902763](https://github.com/mmwiza1/Race-day-dreams/actions/runs/44171902763).
Completed logs confirm 640 unique Android API 35 cases and 700,189 domain checks
across 48 suites. ZIP and XML verification confirms JVM 490 domain, 25 health,
658 storage and Apple 449 domain, 27 health, 658 storage tests, all with zero
failures, errors or skips. The Apple simulator framework linked. APK artifact
14370465169 contains version 0.12.3-test1, code 19, min API 28, with no Internet
permission and SHA-256 `44814221de8798391091ccd677c537f1901eca61dfa192703774734ee8064166`.
Proof: `/workspace/scratch/kmp144-verified/verification.json`.

Push this documentation and checkpoint 145 together on the existing draft PR #24.
Checkpoint 145 passed local metadata compilation, app and test APK builds and lint.
Check the actual PR head and CI before repeating a push. The new wire regression
makes the expected focused Android total 641. These runtime results are pending.
Continue with typed recording staging and finalisation on shared SQLite, then
real iOS SDK and application integration. See the implementation boundary below.
The full Android matrix and actual iOS application verification remain required.

### Checkpoint 145 implementation details

Continue ORIGINAL experiment/android-ios-kmp-compose / DRAFT PR24. Through144 is NOW pushed
at3e95c70d4065f71c6166264d86b930a390d4351f;143349ddcd precedes it. Current experiment44171902763:
Android134352935583 SUCCESS, Apple134352935067 Native tests passed, framework LINK still running
at last check. Formatting44171902185 SUCCESS; actual logs18contracts/997files(902Kotlin95Java)
checked. Do not cancel useful Native linking by pushing an intermediate checkpoint.
142fully verified640/700189/48/JVM490-25-658/Native449-27-658 proofs below remain historical.
144Android exact counts/artifacts not yet independently parsed here; inspect completed logs.

145is applied and locally VERIFIED in THIS coherent commit, not yet pushed at commit time.
Original active running metrics+staging cursor coordinator now COMMON and USED by Android:
sharedUi/commonMain/org/racedaydreams/recording/SharedRecordingSessionCoordinator.kt adds typed
RecordingStageSession/RecordingStageBatch/RecordingStageRepository and same original coordinator.
Exactly ONE RunMetricsEngine, same flush mutex/captured deltas, Main-before-IO snapshot, both
cursors advance ONLY after successful append, same latest retained header/active-time finalisation.
Constructing it never reads SQL, starts sensors or resumes missing data. Real repo/IO/monotonic/
context snapshot callbacks required. Encoded execution context is EXISTING recording context
format, not a new generic data bucket or schema concept. Native backend/codec still must be real.

Android app recording/RecordingSessionCoordinator.kt retains original APIs/RecordingStore.Session,
actual RecordingRepository/SqliteRecordingRepository and EXACT org.json event codec. Only maps
immutable header/batches at the boundary; callback builds original context on caller/Main before
serial IO. Existing SQL store, finalisation, activity/PBs/associations and foreground service are
UNTOUCHED. No GPS/pace/distance/filter/timing/sensor changes, no second recorder/engine.
All original RecordingSessionCoordinatorTest assertions/cases retained. New legacy wire golden
checks integer/fraction/quote/newline payload, event replacement, source immutability and cursors.
This is a declared repository/codec fixture, NOT physical SDK recording acceptance.
FINAL145common metadata/debugAPK/testAPK/lint PASS5m29s/176tasks:
scratch/kmp145-recording-coordinator.log. Formatting998files(903Kotlin95Java) applied; final check/
schema/source validation at commit. New runtime expected641Android and unchanged Native449/27/658
plus framework are PENDING for145; cannot inherit144or142acceptance.

Next exact tasks: finish current144Native gate and parse actual Android640/domain/JVM/Native
artifacts, fixing any actual failure without weakened tests or blind reruns. Then ONE normal145
push to existing PR24 if not already there and inspect new-head gates; consult actual PR body/head
before repeating a push. No temporary branch or replacement PR needed. Do not reapply source port
scripts. Shared coordinator is now extracted; next native recording boundary is REAL typed staging
append/recovery/finalisation from app/data/RecordingStore.kt and existing shared archive/evidence/
activity/context codecs. Reuse RunMetricsEngine and archive/retention kernels; preserve exact
normalized observations, atomic context/point/event batches, idempotence, qualified PBs, planned
links, gaps and explicit recovery. Do not create a stub/fake native recorder or duplicate engine.

Remaining full scope: actual native Core Location/BLE/clocks/notifications/audio/documents,
retained input and SDK/application composition, iOS HOST/entitlements/usage strings, remaining
active Progress/history/settings/storage/running UI/assets/navigation. Strength/Gym/body/manual/
custom/templates/reviews and feature coordinators now already exist in common and are used by
Android; do not repeat them or COMPLETE0Java-domain/storage kernel ports. ONE real app-owned
operation guard across EVERY native backup/restore/cleanup path before destructive controls;
Native controls remain OFF. Final full Android28/35/36 and actual iOS APP/runtime/backup/migration/
authentic source-bound light/dark/200% visual gates mandatory when ready. All physical checks
UNPERFORMED. Version0.12.3-test1/code19/schema19/rootJSON13/raw3/package1 unchanged. Main85e1c39/
ownerPR23 untouched; no version bump, merge or public publication until full cross-platform ready.

### Latest recovery pointer — 17 November, through144 locally verified (historical)

Continue ORIGINAL experiment/android-ios-kmp-compose / DRAFT PR24.142ced0982 is pushed and BOTH
experiment44166903090 jobs SUCCESS.143manual/custom programme UI committed locally349ddcd;
144template/revision/review UI NOW applied and locally VERIFIED in this coherent checkpoint.
Ready for ONE combined143–144normal push; inspect actual PR head/CI before repeating it. No useful
142Native job remains running, so no cancellation needed. Main85e1c39/ownerPR23 untouched.

143shares original manual programme builder and custom-exercise editor, using SAME existing
shared coordinator. Strict UK date/weekday/number/exercise/target/back bindings stay real Android
platform effects. Same user fields, immutable drafts/blocks/duplicates/moves/supersets, explicit
preview/activation and modest custom metadata, no fake technique. Programme label mapping is
common.143initial compile55s ONLY SlotEditor mixed argument binding, fixed explicit fields;
FINAL metadata/debugAPK/testAPK/lint PASS6m39/176tasks, scratch/kmp143-programme-ui-corrected.log.

144shares ORIGINAL template/library/configuration/dated revision UI and actual swap/history/
progression DIALOGS. Approved SharedProgrammeProgressionSheet.kt CONTENT IS UNCHANGED. Common
SlotEditor/preview/configuration/history/progression widgets reused; original strict input dates,
actual today/UUID/back and exact labels supplied explicitly. Native date comparisons/sorts use
identical epochDays; no new generator/executor/state store/automatic activation. Android keeps
same public APIs, lifecycle, initial draft and source/evidence/freshness guards. New files are
SharedStrengthGenerationScreen.kt, SharedProgrammeSwapDialog.kt, SharedProgrammeReviewDialogs.kt;
Android wrappers +internal coordinator access only. DO NOT rerun applied scratch scripts
port_programme_ui143.py/port_generation_ui144.py/body141/apply.py/port_body_ui142.py.
FINAL144metadata/debugAPK/testAPK/lint PASS6m57/176tasks: scratch/kmp144-generation-ui.log.
Pinned997files(902Kotlin95Java) applied; final check/schema/source validation recorded at commit.
Runtime/Native for143–144 still PENDING, expected same640Android and Native449/27/658/framework.
Do NOT accept these new files using source142CI or mistake a framework for an iOS application.

ACTUALLY VERIFIED source142ced0982b64313f10277947315e126bd4c8245aa3:
experiment44166903090 BOTH jobs SUCCESS. Android134335277396 logs prove640UNIQUE matching
START/END cases,0fail/skips, including all3Gym cases and new body-only privacy case.48actual domain
suite outputs sum700189checks. JVM14367082101 ZIP187518bytes/SHA2fa258ad80f8a00ffcb9a03ca54a24110215842ab7217645369651b550667260
independently verified490domain25health658storage ALL0fail/errors/skips. Native14366587100 ZIP
178977bytes/SHA0768cd80964773ad8500e3d599fab9fc17fdfd19c736f3a570f894f1b80e84a0 independently
verified449domain27health658storage ALL0fail/errors/skips. Proofs scratch/kmp142-verified/
{verification.json,runtime-checks.json} and scratch/kmp142-native-verified/verification.json.
AndroidAPK14367716219 ZIP30828265bytes/SHAafc48f7183f973ad5c856b9cbe993423a80dc1635d460bc7d85c3989f1fa304c
verified APK58615110bytes/SHAf76c33bb5aadd852f2ec2ffbeaaae7cb3ee3a64501c5e8667a26e9054b2a748a,
0.12.3-test1/code19/min28/noINTERNET. Apple134335277843 actual framework linked;14367057053
metadata62683596bytes/SHA937bfd6cbe063c7b86b432ee34deafb3d2fd3c4f3616cd01101f892da1323469;
framework ZIP not downloaded, not an APP. Format44166903228 SUCCESS18contracts/991files logs
verified. Other public release/canonical jobs SKIPPED by experimental draft, not full matrix.

Next exact tasks: if143–144not yet pushed, one normal push to ORIGINAL PR24 and inspect new-head
Android/Apple results; fix ACTUAL failures without weakened tests or blind reruns. Consult PR body
for actual head/run after push. Then remaining ACTIVE Progress/history/settings/storage/running UI,
platform inputs/assets/navigation and REAL native SDK/application composition: recording/location,
BLE, notifications/audio/document IO, retained state and clocks, actual iOS host/entitlements/usage
strings. All Gym execution/body/manual/generation/custom/review UI and feature coordinators now
exist in common and ARE used by Android; do not port them or Java-domain/storage kernels again.
Native body typed repo and actual SDK body-only operation already exist; finish host wiring.
ONE real app-owned operation guard must cover EVERY native backup/restore/cleanup path before
destructive controls become available; they stay OFF. Existing storage/backup/retention kernels
are complete. Final full API28/35/36 + ACTUAL iOS APP/runtime, migration/backup, authentic source-
bound light/dark/200% visual gates remain mandatory once complete. All physical checks remain
UNPERFORMED. No version bump, merge or public publication until complete cross-platform readiness.
Version0.12.3-test1/code19/schema19/rootJSON13/raw3/package1 unchanged. No new permissions/backend/
account/analytics/Internet or unrelated feature work. Preserve all earlier checkpoints below.

### Latest recovery pointer — 17 November,142 pushed /143 locally verified (historical)

Original experiment/android-ios-kmp-compose / DRAFT PR24. Through142 NOW committed/pushed:
ced0982b64313f10277947315e126bd4c8245aa3, preceding532f64e/0e01af2. Remote experiment44166903090
is IN PROGRESS (Android134335277396 runtime; Apple134335277843 Native tests). Expected640Android. Native tests HAVE NOW passed; downloaded14366587100 independently
SHA/ZIP/XML verified449domain27health658storage ALL0fail/errors/skips.178977bytes/
SHA0768cd80964773ad8500e3d599fab9fc17fdfd19c736f3a570f894f1b80e84a0, sourceced0982.
Proof scratch/kmp142-native-verified/verification.json. Framework link still running when checked;
Android640runtime still pending. These results do NOT accept local143. Format44166903228 SUCCESS; actual completed
logs verify18formatter contracts/991files(896Kotlin95Java). Other release/canonical/publication
jobs SKIPPED on experimental draft; not full release acceptance. Current PR body has exact IDs.
Do not cancel useful Native CI with an intermediate push. Source142 worktree was clean after push.

143is NOW locally VERIFIED in this coherent commit: original manual programme builder and custom-exercise UI are now
common and used by Android, with SAME SharedStrengthProgrammeCoordinator. Android VM exposes
internal shared for UI only; original lifecycle/SavedState and25methods intact. Required
ProgrammeScreenPresentation supplies exact friendly/input date parsing, ISO weekday factory/
labels and exercise/target/number formatters; actual BackHandler supplied by Android. Same strict
UK parser, human values, field/reset/duplicate/move/block/draft/preview/activation/custom-save
flows. Existing programmeLabel enum mapping moved to SharedProgrammeLabels, locale-neutral ASCII
labels, same names. SharedGymSlotEditor reused, no second parser or programme engine.
Paths: app coach/strength/{StrengthProgrammeBuilderScreen,CustomStrengthExerciseScreen,
StrengthProgrammeViewModel}.kt; sharedUi common coach/strength/{SharedStrengthProgrammeBuilderScreen,
SharedCustomStrengthExerciseScreen,SharedProgrammeLabels}.kt. No durable data/schema changes.
143first metadata/Android compile FAILED55s ONLY mixed named/positional SlotEditor arguments;
fixed explicit slot/exercises/favourites/recent/archived/excluded/error bindings without defaults
or removed assertions. FINAL143metadata/debugAPK/testAPK/lint PASS6m39s/176tasks:
scratch/kmp143-programme-ui-corrected.log. Formatting994files(899Kotlin95Java) applied;
diffcheck/schema19/source3 PASS. Android/Native runtime for143 NOT yet run.

144helper scratch/port_generation_ui144.py PREPARED ONLY: existing template/revision screen and
swap/progression/history dialogs, required real date/today/identity/back and exact formatter
bindings. Approved SharedProgrammeProgressionSheet.kt content MUST NOT be overwritten; helper
creates separate SharedProgrammeReviewDialogs.kt and SharedProgrammeSwapDialog.kt. Inspect actual
files before applying ONCE, since scratch source scripts are not idempotent. After143gate inspect
result; complete144coherently, targeted compile/APKs/lint/source/schema/format, commit and wait
useful142Native CI before next combined push. Keep all prior tests/assertions. If142CIfails,
inspect actual completed logs and fix root cause before any rerun. No merging/publishing while
actual iOS host/services/full app acceptance remain unfinished. Other remaining scope below.

### Latest recovery pointer — 17 November, through142 locally verified (historical)

Continue ORIGINAL experiment/android-ios-kmp-compose / DRAFT PR24. All workspace edits137–138
survived and recovery-only5235f2f was safely fast-forwarded.137–138committed0e01af2;139–140committed
532f64e.141–142are locally verified in THIS coherent checkpoint. Ready for ONE combined normal
push; inspect actual PR head/run first if resuming, never repeat pushes or cancel useful Native CI.
Main85e1c39/ownerPR23 untouched; no replacement PR, version bump or public artifact publication.

Completed137–140: original Gym Now operations, entry/live/builder/slot-editor and authored programme
screens are COMMON and USED by Android. Same execution/association coordinators, one SQL pool,
IO mutex, retained inputs, exact-retry acknowledgements, timers/supersets/warm-ups/exclusions/swaps.
Android keeps real lifecycle/back/permission, process+monotonic/rest clocks/alerts, UK dates and
exact target/actual formatting. Original Home/plan/root/authoring/generation/recovery/body/milestone
operations were already shared; do not repeat these ports or the COMPLETE0Java-domain conversion.

Completed141–142: SqliteBodyMeasurements implements original typed body interface over EXISTING
height/context stores; Native can reuse manual history/preferences and the established visible-only
HealthKit import. Apple visible reads cannot replace retained windows. Existing shared+real Apple
SDK facades now offer body-only refresh through SAME import pipeline, never globally selected
workouts. New actual SQL/shared-gateway privacy case is registered. Original body manual/history/
protein UI is shared and USED by Android with required real dates/numbers/access-note bindings and
SDK provider-control slot. Android public API/lifecycle/tokens/copy retained. Empty Apple permission
identifiers remain UNKNOWN, never invented grants or inferred denial. Actual iOS host still missing.

Also corrected demonstrated exception mismatch: AppleSqliteDatabase throws StorageException,
which is an Exception rather than RuntimeException. Shared Gym command/panel/display handlers
now preserve cancellation and expose original pending-write retry after such failures. New third
real SQL Gym case injects that exact exception BEFORE a write, retaining input/no persisted set
until explicit retry. Original after-persist lost-ack and superset/timer cases/assertions remain.
New Gym test added to pinned formatting targets; all126prior focused test classes retained.

Actual LOCAL gates:138metadata/APKs/lint PASS6m08/176;139PASS6m48/176;140PASS6m18/176;
141targeted3JDBC+metadata/APKs/lint PASS9m03/180. XML JdbcBodyMeasurementsTest proves3tests,
0fail/errors/skips: same-store/explicit-basis/outer rollback; visible Apple no deletion/basis;
authoritative window scope +late weight failure rolls back heights, null metric preserves history.
Same3actual Apple SQLite tests registered, NOT tested on Linux.142first gate was TERMINATED by
workspace restart during shared compilation, no failure/completion result; process/session vanished.
Resumed ONLY interrupted final gate: scratch/kmp142-body-resumed.log PASS6m36/176tasks. First
incomplete log scratch/kmp142-body-final.log is NOT a pass/failing-test result. Formatting991files
(896Kotlin95Java) applied; format18contracts/schema19exact/source3/release10/diffcheck PASS.
Current debugAPK aapt:0.12.3-test1/code19/min28/noINTERNET. No physical/emulator runtime pass
is claimed by compilation. Expected next640focused Android and Native449domain/27health/658storage
plus real framework remain PENDING until exact new-head CI. A framework is NOT an iOS application.

Prior13664fa114 experiment44142035037 BOTH jobs SUCCESS636Android; independently SHA/ZIP/XML
verified JVM490/25/655 and Native449/27/655 all0fail/errors/skips. Proofs scratch/kmp136-verified/.
Recovery-only5235experiment44157873531 BOTH jobs SUCCESS. These are prior-source results only.

Next exact tasks: finish one coherent push through142 to existing PR24 if not already there;
inspect real Android/Apple jobs and new runtime cases, fix actual failures without weakened tests
or blind reruns. Then remaining active shared UI/navigation/platform input/assets and REAL native
application/SDK composition (recording/location, BLE, notifications/audio/documents, clocks and
retained input), iOS host/entitlements/usage strings. Body typed adapter/SDK body-only flow now
exist; finish their host wiring, not another kernel. ONE actual app-owned operation guard must
cover EVERY native backup/restore/cleanup path before destructive controls are enabled; they stay
OFF. Existing shared storage/backup/retention kernels are complete. Final full API28/35/36 and
actual iOS APP/runtime, backup/migrations, authentic source-bound light/dark/200% visual gates
remain mandatory when implementation ready. Physical device tests remain UNPERFORMED.
Version0.12.3-test1/code19/schema19/rootJSON13/raw3/package1 unchanged. No merge/publication
until complete cross-platform acceptance; do not mistake this locally verified checkpoint for it.

### Latest recovery pointer — 17 November, Gym UI through140 locally verified (historical)

Continue experiment/android-ios-kmp-compose / DRAFT PR24.137–138committed locally0e01af2,
recovering ALL surviving edits and safely fast-forwarding5235f2f.139–140now applied/locally verified:
actual live workout, one-off builder/slot editor and authored-programme overview/workout UI
are common and USED by Android. Android keeps original lifecycle START/STOP+back observer,
notification permission/alerts, UK dates and exact numeric/target/actual labels. Existing shared
association/substitution/warm-up/rest/preview controls reused; no second execution engine,
redesign, changed targets, data/schema/version/permission or automatic activation.

139metadata/debugAPK/testAPK/lint PASS6m48s/176tasks (scratch/kmp139-gym-ui-gate.log).
FINAL140metadata/debugAPK/testAPK/lint PASS6m18s/176tasks (scratch/kmp140-authored-final.log).
Formatting985files(890Kotlin95Java) applied; diffcheck/schema19/source3/release10 PASS.
All126prior focused test classes/assertions retained; only new SharedGymNowCoordinatorTest added
(2realSQL cases). Expected638Android/655Native still PENDING for137–140, not accepted by136CI.
Source136archives independently verified as below. Recovery5235experiment44157873531 BOTH jobs
SUCCESS. No running useful old Native job to cancel; next push should be coherent new source.

141is APPLIED/uncommitted, not just prepared: SqliteBodyMeasurements implements the existing
body interface using original heights/context stores, rejects visible-only Apple window replacement,
and keeps the existing HealthKit merge owner. Shared+real Apple SDK facades expose body-only sync
through the SAME import pipeline without reading globally selected workouts. New3actual JDBC
contracts PASS0fail/error/skip (XML JdbcBodyMeasurementsTest); same3Apple tests registered, NOT
run locally. New actual Android shared-gateway body-only privacy case added, runtime pending.
Current metadata/debugAPK/testAPK/lint command still running: scratch/kmp141-body-gate.log.
No claim of final141build/lint/Android/Native acceptance. Next expected639Android/658storage.

142body UI is APPLIED/uncommitted. Actual manual/history/protein content is shared with
required real date/number/access-note bindings and actual SDK provider-control slot. Android
public API/lifecycle/tokens/copy retained; no unknown Apple grant is labelled denied by shared UI.
Also fixes demonstrated platform exception mismatch: AppleSqliteDatabase throws StorageException
(checked Exception), while original shared Gym Now caught only RuntimeException. Command/panel/
display handlers now catch Exception with cancellation rethrow, retaining pending exact-retry.
New third Gym SQL case injects the actual StorageException class before saving and asserts
unchanged input/no persisted set until explicit retry; both old cases/assertions remain. Added
new Gym test to pinned formatter targets. Expected next640Android/658storage, NOT yet verified.
141FINAL targeted3JDBC+metadata/APKs/lint PASS9m03s/180tasks, scratch/kmp141-body-gate.log.
142FINAL metadata/APKs/lint still running/queued after formatter: scratch/kmp142-body-final.log.
Do NOT rerun applied scratch/port_body_ui142.py or body141/apply.py. Last formatting990files
before the added Gym test target; final991(896Kotlin95Java) pending confirmation. Format18/schema19/
source3/release10 PASS. Then coherent commit and ONE combined PR24 push; inspect exact-head CI.
Remaining full native host/SDK/navigation/UI/lifecycle guard/final app+matrix/visual gates remain
as recorded below. All physical acceptance UNPERFORMED. Version/formats remain19/19/13/3/1.

### Latest recovery pointer — 17 November,137–138 locally verified (historical)

Continue original experiment/android-ios-kmp-compose / DRAFT PR24. Workspace recovered; ALL six
local137–138 files survived. Safely fetched/fast-forwarded recovery-only5235f2f with source edits
preserved. SharedGymNowCoordinator and actual shared entry UI are now used by Android. Original
execution/association coordinators, one pool/IO mutex, pending exact-retry, retained inputs,
immutable actuals, exclusions/substitutions, builder/supersets/timers and actual clocks/alerts
remain. SharedPlainWorkoutTarget now renders original instructions with required exact-target
formatter; native date/back bindings stay required. No new engine/schema/permissions/version.

Recovered138log FAILED47s at missing common LocalDate.isBefore and Android-only PlainWorkoutTarget.
Fixed with exact epochDay comparisons and real shared target UI + original Android formatter,
not changed targets/assertions. FINAL metadata/debugAPK/testAPK/lint PASS6m08s/176tasks,
scratch/kmp138-gym-corrected.log. New2real-SQL tests COMPILE; runtime/Apple for137–138 pending638.
All previous focused classes/assertions remain. Schema19 exact/source3/release10 PASS. Final
format count and commit identity to inspect on resume. DO NOT rerun applied scratch port scripts.

136actual44142035037 BOTH jobs SUCCESS636Android. Downloaded source-bound archives independently
SHA/ZIP/XML verified NOW: JVM14356076893(186882bytes/SHA7c92b7fe78bdde7b90afd077bab7d98b87641e896c3ec45556d76ed379d9fcb8)
490domain/25health/655storage; Native14356821481(178563bytes/SHA46fafe61b1d8f8dd4b6e0471665a38e71fcc210dad16df1d00149ce42e6cd8da)
449domain/27health/655storage. ALL0fail/errors/skips. scratch/kmp136-verified/verification.json.
This is PRIOR136proof, NOT137–138acceptance. Recovery5235workflow44157873531 still running when
last inspected; no cancelling useful Native verification or early intermediate push.

Next coherent batch139: actual live workout and workout builder/slot editor UI sharing, retaining
Android lifecycle/back/permissions/numeric labels and all existing fields/actions. Scratch helper
port_gym_ui139.py is PREPARED ONLY at this pointer; inspect actual files before running it ONCE.
Then focused local metadata/APKs/lint/schema/source/format gates, coherent checkpoint and one
combined push to existing PR24; inspect actual exact-head Android/Apple runtime. Remaining native
SDK/body/app composition/host/full app and full final matrix scope below remains, not optional.
No merge/publication until complete Android+iOS APP readiness. Physical tests UNPERFORMED.

### Latest recovery pointer — 17 November, workspace outage during137–138 (historical)

Continue ORIGINAL experiment/android-ios-kmp-compose / DRAFT PR24. This is a recovery-only
remote documentation commit made because the executor became UNAVAILABLE. Implementation
through136 remains pushed at64fa114281123f07d63bf2afaf6ce359e41ca8d0. Preserve all surviving
uncommitted137–138 files; do NOT reset or reapply scratch migration scripts blindly.
The new remote documentation commit does NOT include or validate the local source changes.

**Confirmed136 CI:** experiment44142035037 BOTH Android134247098540 and Apple134247099136
completed SUCCESS. Actual Android completed logs say636tests,0failed/0skipped; Apple actual
Native-test and framework-link steps SUCCESS. Source136 artifacts were NOT independently
downloaded/XML/digest-verified in this session. Prior134 proofs below remain historical.
Formatting44142034932 was already SUCCESS. Framework is NOT an actual iOS application.

**Surviving local changes at last successful git status:**
- MODIFIED app/src/main/java/org/racedaydreams/coach/strength/GymNowViewModel.kt:
  original public methods now forward to internal val shared:SharedGymNowCoordinator.
  Actual same AndroidSqlConnection, original execution/association/programming/coaching/legacy
  stores, SavedState keys, viewModelScope, UUID/process clock, elapsedRealtime, Instant/offset,
  GymRestClock and GymRestAlerts bound. Shared retained List values converted to ArrayList in
  SavedStateHandle; no replacement service or engine.
- UNTRACKED sharedUi/src/commonMain/kotlin/org/racedaydreams/coach/strength/SharedGymNowCoordinator.kt:
  original GymNowState/GymSetDraft/GymBuilderDraft and original ~1100line operations moved.
  IDs/time are REQUIRED platform effects (no common fake clock or UUID); form/builder have
  required identity/time and internal newForm/newBuilderDraft factories. Constructor requires
  SAME SqlDatabase, GymExecutionRepository, GymAssociationRepository, programming/coaching,
  legacy current/version, favourite/recent/finished-ID readers, GymNowRetainedState, lifecycle
  scope, real execution TickSource/rest clock/alert effect, now/offset/ID and IO dispatcher.
  Uses existing SqliteGymTools/SqliteStrengthChoices/SqliteExerciseExclusions over same pool.
  One original mutex/command/ack callback/terminal projection/pending exact-retry protocol.
  Original retained builder codec uses FrozenGymWorkoutJson, original parser takes newId;
  Collections.swap becomes identical element swap. Initial compile found only visibility:
  Android wrapper could not call internal shared observeWorkoutTime; shared method made public
  suspend (Android wrapper remains internal). That correction is NOT yet compiled.
- MODIFIED app/src/main/java/org/racedaydreams/coach/strength/GymNowScreen.kt and UNTRACKED
  sharedUi/src/commonMain/kotlin/org/racedaydreams/coach/strength/SharedGymNowScreen.kt:
  original entry/one-off/planned/favourites/repeat/recovery UI moved without redesign.
  Android retains lifecycle collection, actual UK LocalDate/Instant date formatters and
  BackHandler. Shared UI takes model/state/today/date+recorded-date/back-handler bindings.
  Shared date sort uses epochDay; UK enum casing uses locale-independent existing ASCII enums.
- UNTRACKED app/src/androidTest/java/org/racedaydreams/coach/SharedGymNowCoordinatorTest.kt:
  two actual SQL tests, lost save acknowledgement after persistence/exact retry/no duplicate/
  retained input/no completed actual before finish; builder/superset saved-state identity,
  90sec rest→30sec elapsed→pause60sec→explicit resume→changed boot epoch INTERRUPTED, no fake
  actual. Original catalogue IDs bench-press/dumbbell-row and GymSlotInput minimumReps/
  maximumReps/restSeconds corrected; provider clocks/alerts declared controlled fixtures.
  Tests have NOT compiled or run yet; inspect all fixture assumptions rather than claiming pass.
- MODIFIED .github/workflows/multiplatform.yml: added only new SharedGymNowCoordinatorTest to
  existing focused list; all old classes/assertions retained. Next expected638, UNVERIFIED.

**Actual validation boundary:** first metadata+Android Kotlin compile in
scratch/kmp137-gym-compile.log FAILED1m45 ONLY Android wrapper access to internal shared
observeWorkoutTime; visibility corrected afterwards. Initial common metadata/shared Android
compilation passed before that access failure. Later complete metadata/debugAPK/testAPK/lint
command queued behind formatter in session67730. Last successful observation: formatter process
still running, scratch/kmp138-gym-gate.log did NOT EXIST YET. Do NOT claim formatter/final build/
test compilation/lint/runtime/Native results. Processes may have continued; inspect logs first.
Scratch helpers port_gym137.py and port_gym_home138.py already APPLIED; do NOT rerun on applied
files. Last successful git diff --check PASS; no implementation commits/pushes performed.

**Exact next action after executor returns:** inspect git status/diff and surviving files/logs.
Fetch this recovery-only remote commit and safely fast-forward with local changes preserved
(the remote changes only this handoff); do not discard source. Finish/review137–138, compile
common metadata/debug+test APK/lint, fix actual compile/test failures, format/schema/source
checks, add any missing targeted cases. Commit coherent SOURCE only when locally validated,
push once to PR24 and inspect actual Android/Apple gates. Existing GymNow integration cases
remain mandatory. If local files disappeared, reconstruct ONLY137–138 from the original
64fa114 GymNowViewModel/GymNowScreen with bindings above; do not restart136 or storage/domain.

Remaining scope unchanged: actual shared Gym workout/builder and remaining UI/navigation/
assets; Native body repository/SDK composition; actual iOS host/entitlements/usage strings and
real recording/BLE/notifications/audio/document effects; ONE real app-owned guard before native
destructive controls; final full Android28/35/36 + actual iOS APP/runtime/backup/visual gates.
Version0.12.3-test1/code19/schema19/root13/raw3/package1 unchanged. No merge/publication.
Physical tests all UNPERFORMED. OwnerPR23/main untouched.

### Latest recovery pointer — 17 November, locally verified136 (historical)

Continue experiment/android-ios-kmp-compose / DRAFT PR24. Preserve pushed134ac9a845 +local135
recoveryaeac08a and136optional body operations in this checkpoint. Ready for ONE combined normal
push135/136at commit time; inspect actual PR head/run on resume BEFORE repeating a push or
cancelling useful CI. Actual subsequent head/run recorded in PR body. Main85e1c39/owner PR23
unchanged.132recovery ref already safely removed after containment checks; no new PR/version.

134experiment44136116110 finished SUCCESS on BOTH jobs. Actual Android134225992836 proves632
unique matching START/END cases, including all6new authoring/generation cases. No failure/skip;
700189domain checks/48suites retained. SHA/ZIP/XML independently verified JVM490domain/25health/
655storage and Native449domain/27health/655storage ALL0failures/errors/skips. Proofs
scratch/kmp134-verified/{verification.json,runtime-checks.json,android-job.log}.
Native14353178203 178677bytes/SHA644a8e8e01b588e2957869949becb911273aba1f34a0d84937e39fd116c5b086;
JVM14352753816 187158bytes/SHA0d2a2606af166c76e435b1a34c4d397e2fe0a7b63e3daf022b3a1381ad7f43c2.
APK14352918983 verified58533190bytes/SHA52ba65631d79e4ad7b1f5f95f58c96f948bd2ad6a85625a25899a972d2ceecee,
code19/min28/noINTERNET. Actual Apple framework linked,14354332572(metadata62412009bytes/
SHA469a7ea67c4d0efe278cc4e5e30ec6b3093c616f6864d932f9ff3fdd69540d38; ZIP not downloaded).
134format44136116072 SUCCESS:18contracts/880Kotlin95Java checked. Earlier HTTP429fixed132 and
new-head CI validated; do not blindly rerun it or wait completed134again. Framework is NOT an app.

135SharedTrainingRecoveryCoordinator is committed/used by Android, SAME repositories/cache/
focus/lifecycle/real clock/offset/ID. Original4public methods, conservative unknown evidence,
no-write Keep, explicit reviewed preview with current running/gym histories preserved. Two real
SQL tests added for timezone rollover/missing evidence/no-write Keep and hybrid preview/stale
review rejection. Local metadata/APKs/lint PASS6m14s/176tasks, scratch/kmp135-recovery-final.log;
format977(882Kotlin95Java).135runtime/Native still PENDING, not accepted by prior134CI.

136SharedBodyMeasurementsCoordinator is NOW APPLIED/locally VERIFIED. Original8public methods,
actual typed repository, optional SDK availability/factual permission/sync effects and real
clock/offset/ID/lifecycle. Android retains original concrete Health Connect sync API, explicitly
checks same repository owner and calls original concrete instance. First compile2m03s found
interface/concrete mismatch, corrected ONLY SDK binding, no changed policy/assertion/API widening.
FINAL metadata/debugAPK/testAPK/lint PASS5m43s/176tasks, scratch/kmp136-body-corrected.log;
format979(884Kotlin95Java), schema19/source3 PASS. Initial log kmp136-body-final.log records fixed
compile error. Two actual Android SQL/shared-coordinator tests cover exact manual source/nanos/
offset, no implicit protein basis, explicit fresh weight/stale evidence, height paging and revoked
SDK read retaining history. SDK test fixture is declared, NOT physical coverage. Empty Apple
permission identifiers remain UNKNOWN; do not fabricate grant tokens. Native UI must retain
platform-specific HealthKit visibility wording. Next expected636Android/655Native still PENDING.

After coherent136commit, normal-push135–136ONCE to ORIGINAL PR24 and inspect exact-head CI. All
previous124focused classes/assertions remain,2added. Native app host/GymNow/full SDK/body adapters,
remaining active shared UI/navigation/assets and final full Android28/35/36/iOS APP/visual gates
remain unfinished. No empty host/prototype or domain/storage/root/authoring/generation/recovery/
body policy restart. Native destructive UI OFF until every operation uses one actual lifecycle
owner. Version0.12.3-test1/code19/schema19/root13/raw3/package1 unchanged. No merge/publication
until complete cross-platform readiness. All physical acceptance remains unperformed.

### Branch, baseline and completed checkpoints

- Continue `experiment/android-ios-kmp-compose`; DRAFT [PR #24](https://github.com/mmwiza1/Race-day-dreams/pull/24).
  Live main fetched this session: `85e1c3903b61f1d45e444d120ba1a701ad3a2c36`.
  Preserve separate owner CI PR #23. No resets, replacement PRs or force pushes.
- Version stays **0.12.3-test1 / code 19**; SQLite **19**, root JSON **13**, raw archive **3**,
  complete package **1** unchanged. No experimental artifacts have been publicly released.
- `040ea9f801117bb96d54acbe4427c26943d80142`: actual Android-used shared enums/units/pace policy;
  existing shared theme, typography, shapes, chips, pages, cards and disclosures. Kotlin 2.1.21 /
  Compose Multiplatform 1.8.2; notices mirrored. Existing Android dynamic colour/system bars remain.
  JVM facade collision fixed by naming common file `SharedCoachComponents.kt`.
  CI [43431989080](https://github.com/mmwiza1/Race-day-dreams/actions/runs/43431989080) passed Android
  and real Apple simulator shared tests/framework. A framework is NOT a complete iOS application.
- `afc8e4ea822f88b524f6d3709e1ea538d757fc53`: shared body-read staging, immutable provenance,
  pagination, permission checks, qualified identities; real Health Connect and HealthKit readers.
  Android retains its original lock/history-permission recheck/DTO validation/transaction,
  1000-page / 500-record limits and missing-weight-offset UTC fallback (height stays unknown).
  Apple authorization completion does NOT prove read permission: visible-only empty/nonempty
  HealthKit snapshots cannot replace windows or establish deletion of retained history.
- `78805ecbea2df8174a42bc70e541eaa33bf4ec1d`: use tested kotlinx-datetime Darwin timestamp
  converters, not unsupported NSDate constructors/category properties; two real Foundation tests.
  SourceIdentity / PerformanceDistance moved to common Kotlin, stable original IDs/whitespace,
  record accessors/equality/hash/text and immutable core order retained. Original/new JVM parity
  output matched. Both jobs in [43440356122](https://github.com/mmwiza1/Race-day-dreams/actions/runs/43440356122)
  succeeded, including compiling the real HealthKit adapter and framework. No provider/device pass.
- `4521bce59c3d6f51577e740c2d53dcc19daa7ec1`: complete existing ProteinPolicy and PrimaryFocus
  shared in Kotlin; JVM ProteinGuidance adapter retains the actual original BodyWeight evidence.
  Selection/conflict/source freshness/ranges remain intact (including null-focus 1.2–2.0,
  30–250 kg applicability and 90-day boundary). Existing Java focus assertions retain Optional at
  their persistence boundary. Domain now mixed Kotlin/JVM + Java, not a second business engine.
  **Actual CI [43442780709](https://github.com/mmwiza1/Race-day-dreams/actions/runs/43442780709) SUCCESS
  on both Android and Apple**, plus formatting. Android passed 28 focused existing UI/health tests.

### Fifth checkpoint — common timer, immutable evidence and more existing UI

- **Committed/pushed `e672d31f8ae318d4da512e45e61003429beee9ec`.** Six more Java originals removed/replaced: Immutable, PerformancePace, GymRestTimer,
  StrengthMeasurements, TrainingMetrics and PlanRegressionPolicy. **194 Java domain files remain.**
- `sharedDomain/.../coaching/Immutable.kt` is the original shallow immutable-list/set contract,
  with defensive copies/null rejection/no exposed mutation. Shared health snapshots reuse it,
  rather than retaining duplicate local snapshot implementations.
- Recorded-performance pace arithmetic now common `PacePolicy.performancePace`; the JVM
  `PerformancePace.kt` adapter retains java.time.Duration and required-null boundary checks.
  Preserve original multiply/divide evaluation order and Math.round saturation/half-boundaries.
  Verified original Java returns **7:59/mi at 479.5 seconds**, 8:00 at 479.6. Do not algebraically
  cancel ratios or restore floor(value + 0.5), which differs at sub-half and huge-Double edges.
  A JVM differential test checks **22,110** combinations against original Math.round/formatting;
  original-binary versus current-adapter output also `cmp` MATCH.
- `sharedDomain/.../strength/GymRestTimer.kt` owns the existing monotonic, immutable countdown
  and settings. Epoch changes/backwards clocks interrupt; only explicit resume adopts a new
  clock. Actuals/prescriptions remain separate. The optional prescribed rest is projected as
  nullable seconds by the two Android callers; no duplicate workout model or execution engine.
- `StrengthMeasurementPolicy` shares existing seconds parsing/limits/overflow errors/duration
  wording. JVM `StrengthMeasurements.kt` retains existing record/locale formatting. Differential
  tests include Unicode digits, signs, overflow and exact original errors; common tests also
  exercise Arabic/full-width digits on Native. Missing TrainingMetrics remain null, not zero.
- Existing semantic tiles/cards and production rest controls moved to `sharedUi/commonMain`:
  `SemanticTiles.kt`, `strength/SharedGymRestControls.kt`. Existing labels, shapes, colours,
  enabled states and disclosures retained. Android `GymRestCard` only binds existing ViewModel,
  notification permission and platform alert explanation. No redesigned/demo screen.
- Expanded experiment-only focused CI to include existing `GymNowScreenTest` and
  `GymToolsRepositoryTest`, in addition to prior 28 tests. Full release gate/default Android
  guards/assertions remain unchanged; no version, publication or final screenshot claim.
- Verified current fast gates: **28 shared-domain JVM tests**, **11 shared-health JVM tests**,
  all **700,189 legacy checks / 48 suites**, schema **75**, Android runner helper **15**,
  release notes **10**, formatter-tool **14**, pinned formatter **172 Kotlin / 156 Java**;
  `git diff --check` clean. **APK + instrumentation APK + lint BUILD SUCCESSFUL, 167 tasks /
  6m33s**, `/workspace/scratch/kmp-gym-final-build.log`. APK inspection confirms code19, minAPI28,
  no INTERNET permission. Earlier focused compile failure (Long != Int zero) fixed to 0L, not weakened tests.
  An attempted sandboxed Gradle daemon lacked socket permission; retry with task network access
  started normally. This was not a source/test failure.

- Actual CI [43449181442](https://github.com/mmwiza1/Race-day-dreams/actions/runs/43449181442)
  **SUCCESS on Android and Apple** for exact `e672d31`. Android logs confirm **41 focused tests**
  completed with no failures/skips; build/lint/domain/schema passed. Downloaded Apple artifact
  `14048494430` XML confirms **26 common-domain Native tests + 13 shared-health Native tests**,
  zero failures/errors; real shared UI framework compiled. Artifact proof retained at
  `/workspace/scratch/kmp-gym-apple-results.zip`. CLI artifact redirect was forbidden; connected
  GitHub artifact download succeeded. This is not an infrastructure/compilation blocker.

### Sixth checkpoint — shared training availability (`92ff09d`)

- `TrainingPreferences.java` replaced with actual common Kotlin. **193 Java domain files remain.**
  Existing independent/overlapping running and gym days, optional long-run day, null rejection,
  frequency validation, immutable snapshots, ISO EnumSet iteration order, equality/hash and
  record text retained. No new scheduling rules or replacement planning engine.
- `sharedDomain/.../time/DayOfWeek.kt` uses an expect enum with an actual JVM typealias to the
  EXISTING `java.time.DayOfWeek`; Native implements the same seven stored names/ISO values.
  This binding **compiled against every remaining Java domain caller**. It is a real production
  availability migration, not a throwaway prototype or a proven binding for all java.time APIs.
  Expected Kotlin expect/actual Beta warnings are visible, not suppressed or treated as failures.
- Four new common tests cover weekday identities/values, original ordering, frozen independent
  availability, missing/invalid data and structural equality/presentation. JVM shared-domain
  **32 tests pass** and all **700,189 original checks / 48 suites pass**. Pinned formatter checks
  **177 Kotlin / 156 Java** files. No schema/backup/version/permission change.
- `scripts/screenshot-source.py` now includes `sharedDomain/src/iosMain`, so the Native weekday
  implementation cannot be omitted from final screenshot/source provenance. No captures claimed.
- Local APK + instrumentation APK BUILD SUCCESSFUL, **114 tasks / 3m20s**,
  `/workspace/scratch/kmp-weekday-build.log`; all original suites passed. Capture-validator
  fault tests also pass (**23**); these are not new app screenshots.
  Actual workflow43451707710: Android and formatting SUCCESS at `92ff09d5502cf3509331c7c4d4edf7ffa1f9f0ee`;
  Apple stayed QUEUED in the arm64 runner pool. No Native pass is claimed for that queued run.

### Seventh checkpoint — shared running goals and immutable plans (`865a0e1`)

- COMMITTED/PUSHED `865a0e1a162d6e0b21c9a0af034ce08aa8c45fb6`. Ten Java originals removed: DomainChecks,
  RaceGoal, UserProfile, WorkoutSegment, TrainingPhase, PlannedWorkout, TrainingPlan, AdjustmentOption,
  RaceDates, OnboardingValidator. **183 Java domain files remain.** Schema/JSON formats unchanged.
- `domain.time.LocalDate` / `Duration` expect types use actual JVM aliases to the ORIGINAL
  java.time classes, preserving Java source/binary signatures and direct persistence callers.
  Native calendar retains ±999999999-year ISO identity; pinned kotlinx-datetime Native stops at
  ±999999 years, so narrowing it would reject previously supported data. Original integer
  Gregorian representation and canonical Long seconds/Int nanos retain range, fractions,
  sign, ISO text and overflow checks. These are temporal compatibility primitives, not new
  training rules. No whole java.time replacement or unneeded date API was introduced.
- Existing PaceCalculator checks now common, replacing its JVM-only implementation. RaceDates
  and OnboardingValidator retain thin original JVM Clock/zone boundaries and delegate to
  RaceCalendar / GoalValidation. Constructors still accept historical past goals; only onboarding
  applies the existing future-date rule. Nullable dates/pace/effort/distance remain unknown.
- Common records retain exact validation ordering/messages, names/IDs/ASCII trim, record text,
  structural equality and immutable snapshots. Programmes never create actual evidence. Original
  binary versus converted record/temporal/validation output **cmp MATCH** at
  `/workspace/scratch/kmp-{original,shared}-running-models.log` (22 lines).
- Final local gate `/workspace/scratch/kmp-running-final-build.log`: **BUILD SUCCESSFUL, 113 tasks /
  3m28s**, APK + instrumentation APK + **40 shared-domain JVM tests**, zero failures. Eight existing
  domain suites passed **33,026 checks** in `/workspace/scratch/kmp-running-focused-domain.log`;
  DomainTestSuite's151 were verified again after the final shared onboarding rule. Focused rather
  than repeated full release testing, as authorised. Full legacy suites still run in CI.
- Two differential JVM tests cover **139,704** full-range date/text fixtures plus invalid days,
  **22,154** duration-normalisation/text/overflow pairs, and checked unit scaling against JDK.
  Six common tests exercise the real platform bindings, fractional target rejection, unknown
  prescriptions, preserved histories, explicit choices, local-day horizons and onboarding.
  Pinned formatting **197 Kotlin / 156 Java**, release-note helper **10**, diff check PASS.
- New nullable Kotlin APIs exposed unsafe reads in four Android files: HomeScreen, PlanScreens,
  RaceScreen and ReviewAndWeightScreens. Checked local values preserve optional pace/date and
  explanations, not zero/fabricated targets; no redesign. A review caught and removed an unintended
  guard from the undated-goal card BEFORE final verification. Current goal card remains available.
- Actual compile fixes were tested; `/workspace/scratch/kmp-running-app-compile.log` records the
  initial nullable-call errors. Earlier compile overlapped formatting/new source and is NOT used
  as final proof; final stable-source build above supersedes it. Do not restart/repeat fixed work.
- Experimental Apple CI now uses official **macos-26-intel** to avoid the sustained arm64 queue;
  official runner-images README inspected. Existing host-architecture tests/framework/assertions
  retained. Actual CI43457885883 SUCCESS on BOTH Android and Apple; Intel job really compiled
  and ran Native tests/framework. Android logs confirm **77 tests**, zero skipped/failed, plus
  existing full legacy domain/schema/build/lint checks. Prior92ff Apple job was cancelled while
  queued by the newer push, with zero compiler steps; not a demonstrated code failure.
  Focused Android CI additionally includes CoachOnboardingTest / CoachingInteractionTest /
  CoachCompletionStateTest / SqliteUserPreferencesRepositoryTest / CoachingStoreTest, covering
  actual goal/plan interaction, profile persistence, current/legacy backup and rollback.

### Eighth checkpoint — exact body observations (`7e973c7`)

- COMMITTED/PUSHED `7e973c7be9d9eb729bbd7281daf2028bc9930fca`. BodyWeight.java / BodyHeight.java replaced with common
  Kotlin; **181 Java domain files remain**. Original source IDs/ASCII trim/validation/equality/text,
  weight limits, height limits and unknown height offset/import time preserved. Legacy weights
  retain their original permissive empty-provider/source behavior; no retroactive rejection.
- `domain.time.Instant` / `ZoneOffset` use original java.time aliases on JVM and exact Native
  representations. Full original ±1000000000-year instant bounds, nanoseconds, comparisons,
  canonical ISO fractions and second-resolution offsets retained. Native kotlinx-datetime0.6.2
  is limited to ±1000000 years and its fromEpochSeconds factory clamps beyond them (pinned
  upstream implementation inspected). Do not silently clamp retained historical timestamps.
  IsoCalendar gains a bounded inverse only for formatting these actual retained observations.
- `sharedHealth/.../BodyEvidence.kt` owns the original conversion, now used by the actual Android
  HealthConnectGateway. Height missing offset stays null; legacy weight UTC display fallback is
  unchanged and is NOT proof of a provider-supplied local offset. Local IDs still supplied by
  caller; provider/origin/source/timestamp/nanoseconds retained. Snapshot completeness, qualified
  identities, permission/history recheck, IO lock, limits and final transaction are unchanged.
  HealthKit can reuse the same mapper; it does not confer replacement/deletion authority.
  sharedHealth now API-depends on sharedDomain because public mapping returns typed body records.
- Final local `/workspace/scratch/kmp-body-final-build.log` **BUILD SUCCESSFUL, 114 tasks /2m25s**:
  app + instrumentation APK; **45 shared-domain JVM tests /14 shared-health JVM tests**, zero
  failures. Original BodyGuidanceTestSuite **1368** checks PASS; original versus converted body /
  timestamp / provenance / error output **cmp MATCH** (11 lines) in scratch `kmp-{original,shared}-body-models.log`.
  Native primitive references compare **70,056** instant/fraction fixtures and ALL **129,601**
  permitted second-resolution offsets against JDK; common tests exercise actual platform models.
  Pinned formatter **209 Kotlin /154 Java**, diff check PASS. No schema/backup/version change.
- Actual workflow **43461801823 SUCCESS on Android AND Apple** for exact7e973c7, including real
  Native timestamps/offsets/provider mapping and shared UI framework. Android retained focused
  UI/persistence/health and complete legacy domain/schema/build/lint checks. No app-host or
  physical-provider pass is implied. No publication or merge.

### Ninth checkpoint — shared body contracts/card (`a7bad35`)

- COMMITTED/PUSHED `a7bad359686f29b7a95df7ed200d7ad92dd6954c`. **180 Java domain files remain**: BodyMeasurementsRepository.java
  replaced with its common typed interface/preferences; previous Kotlin/JVM ProteinGuidance adapter
  replaced with common API. Java signatures retain java.time through actual aliases. Original
  preferences defaults, nullable selection, advice accessors/text and captured evidence retained.
- ProteinPolicy now consumes exact domain Instant instead of a narrower provider timestamp.
  Same strict90-day boundary, future/stale rejection, explicit selection, conflict detection,
  original30–250kg applicability, focus subranges and nearest5g rounding. Nanosecond age comparison
  avoids materialising a clamped threshold; no new guidance calculation. Advice returns the
  ORIGINAL BodyWeight instance. Existing six policy tests retain all assertions/boundaries;
  only timestamp construction changed to equivalent epochs. Three new common contract tests
  cover original-instance identity/defaults, nanosecond boundaries and extreme retained dates.
- Domain no longer depends on kotlinx-datetime; sharedHealth still needs/pins0.6.2 for actual
  platform adapters. Do NOT remove its licence/HealthKit NSDate conversions. No new dependencies.
- Existing BodyMeasurementsState moved intact into sharedUi. Existing production protein card,
  disclosure and source label moved to `sharedUi/.../body/ProteinGuidanceCard.kt` and are USED
  by Android. Same layout/labels/controls/actions; platform supplies original number/date
  formatting and known permission note. No fake permission state or alternative/demo screen.
  Rest of health screen/orchestration/storage is still Android-specific; iOS host still absent.
- Local proof: **48 shared-domain JVM tests +14 shared-health JVM tests**, zero failures;
  original BodyGuidanceTestSuite **1368 checks** PASS after the exact-time policy port. Final
  `/workspace/scratch/kmp-body-card-build.log` **BUILD SUCCESSFUL, 114tasks/2m49s**, both APKs.
  Formatting **213Kotlin /153Java**, diff check PASS. APK code19/minAPI28/noINTERNET confirmed.
- Earlier instrumentation compile found the screenshot fixture assuming a nullable preference
  basis. `requireNotNull(...).matches(weight)` now explicitly preserves/strengthens that SAME
  assertion; no capture/mock or assertion removal. Fixed instrumentation APK passed, then final
  card build passed. Source-bound screenshots remain a final release task, not a claimed pass.
- Actual workflow43466875693: BOTH Android and Apple SUCCESS, confirmed live (real shared
  contracts/state/card Native framework and tests, Android existing integration suites). Keep SAME draft PR24; no version/schema/backup change, merge or publication.

### Tenth checkpoint — shared activity/result/split models

- COMMITTED/PUSHED `c8bdab3407236ef3a4f9e240a5e292265ddbd9ae` on top of `a7bad35`. Eight Java originals
  removed/replaced: ActivityChecks, ActivitySources, ActivityRecord, ActivityLap, DistanceSample,
  RecordedResult, PersonalBestEntry and RunSplit. **172 Java domain files remain.** Previous
  Kotlin/JVM PerformancePace adapter now common; Java caller signatures remain unchanged.
- Common Duration gains only required comparison and exact between(Instant,Instant), retaining
  JVM java.time types and Native canonical seconds/nanos. Original moving<=elapsed validation,
  positive intervals/results/splits, optional offsets/HR/cadence/elevation/RPE/notes, IDs and
  frozen samples/laps retained. Recorded totals are NOT recalculated; discontinuities preserved.
  Actual PB evidence remains distinct from estimates and first PBs have no invented previous value.
- Shared ActivityChecks retains original rounding-to-nanoseconds, including Math.round negative
  halves, NaN and overflow saturation; regression oracle checks20,012 arbitrary Double/edge inputs.
  Six new common/JVM tests bring **54 shared-domain JVM tests**, zero failures. Five existing
  original suites passed **3218 checks**: ActivityDomain138, RunEngine1059, Refinement97,
  RecordingCoordination74 and LiveCoach1850. Original Java versus converted output **cmp MATCH**
  in scratch `kmp-{original,shared}-activity-models.log` (11 lines).
- Java domain and ALL existing Java test sources compile. Android compiler exposed one distance
  nullable read in RunComparisonScreen; now a checked local value, with the identical pace formula.
  No recording/GPS/split/PB algorithm redesign. Original failed compile log:
  `/workspace/scratch/kmp-activity-test-build.log`; that compile passed production code but found an instrumentation test assuming non-null
  distance. `requireNotNull` preserves the original >300 assertion. Final coherent gate
  `/workspace/scratch/kmp-activity-fixed-build.log` BUILD SUCCESSFUL,110tasks/58s, both APKs
  and54shared-domain JVM tests. Formatting223Kotlin/153Java and diff check PASS.
- Experiment CI additionally targets existing FeatureStoreTest / ScopedRepositoriesTest /
  RunSummaryPresentationTest / ActivityHistoryReadinessTest for actual persisted evidence,
  provenance, summaries and history. Full release suite/assertions remain intact.
- Actual CI43469946974: Android SUCCESS with114focused tests, zero failed/skipped, original
  complete domain/schema/build/lint; Apple FAILED compilation because PerformancePace
  relied on JVM default import for JvmStatic. Explicit kotlin.jvm.JvmStatic import is applied
  in the next local history batch; do not rerun unchanged failing source or claim Native passed.

### Eleventh checkpoint — personal-best/shoe policies and actual shared shoe card

- COMMITTED/PUSHED `bddce018614b52083e418b7df75b99b5ec281b42` on top of c8bdab3: thirteen more Java originals removed/replaced,
  now **159 Java domain files remain**. Common PB ordering/authority/daily-best rules, exact
  improvements, shoe assignment arithmetic/unknown mileage, BestEffort/analysis snapshots,
  RecordedWeek/Milestone summaries and typed activity/result/shoe/import contracts.
- Only original JVM Clock/zone binding remains in ShoeMetricsCalculator.kt; common ShoeMileage
  receives explicit local-today. Exact Duration.minus/overflow shared, retaining JVM alias.
  Current test count is **61 shared-domain JVM tests**, including60,147 JDK duration
  subtraction fixtures (20,049 pairs x3 nanosecond combinations). Shared-health14 tests pass.
  Five unchanged activity/recording/refinement/live suites passed3218 checks. Wrong guessed suite
  package paths returned class-not-found, corrected to domain.presentation / domain.execution;
  actual existing suites then passed. No assertion removed or weakened.
- Original/new history-policy output cmp MATCH (11lines) in scratch kmp-{original,shared}-history-policy.log.
  Actual production ShoeHistoryCard extracted into sharedUi, existing Android screen binds
  unit/date formatting, insights and edit action. No redesign/duplicate screen. Three new real
  Compose card tests cover light/dark/200%, disclosure, evidence and edit; runtime PENDING.
- Kotlin nullability exposes original missing-record contracts. UI checks generated week identity,
  unknown milestone text omitted/honestly labelled; recording/restore fixtures use requireNotNull
  to retain SAME assertions. No summaries/algorithms/schema/backup/permissions/version changed.
- Includes explicit PerformancePace JvmStatic import correcting actual c8 Apple compiler error.
  Final focused build `/workspace/scratch/kmp-history-fixed-build.log` BUILD SUCCESSFUL,
 117tasks/1m40s:61domain+14health JVM tests and both APKs. Earlier history compile logs
  found nullable original fields/fixtures, corrected; not final proof. Formatter240Kotlin/149Java,
  release-note10 and git diff check PASS. CI43472233795 Android SUCCESS:117focused runtime tests, including all three new card cases,
  original full legacy domain/schema/build/lint. Apple FAILED BEFORE compilation resolving
  unchanged Google AGP8.9.2 plugin. One targeted retry of Apple accepted after Android completed
  (earlier attempt403 was because the workflow was still running, not missing permissions).
  Actual rerun attempt now BOTH SUCCESS: Apple job132053598802 and retained successful
  Android117; confirmed compile/tests/framework. No assertion or compiler change for dependency retry.
- Next safe action: commit/push this coherent verified batch to SAME PR24. Verify real new Native/Android CI,
  then continue remaining conversion/application scope; do not stop simply at this checkpoint.
- A next-batch baseline ONLY (not migrated yet) is saved: StrengthEvidenceParity.java and
  kmp-original-strength-evidence.log (229lines incl all220 catalogue definitions, original sets).
  Do not overwrite original evidence before comparing the next strength port.

### Twelfth checkpoint — canonical catalogue and actual strength evidence

- COMMITTED/PUSHED `ac34e2e3c36b76382126aded358328d1a5e13814` on top of bddce01 replaces THREE Java originals: StrengthExercise,
  ExerciseCatalog and StrengthSession. **156 Java domain files remain**. No added exercises:
  all220 original definitions/IDs/order/aliases/measurement/estimation flags remain shared.
  Existing recorded definitions, actual sets, warm-up flags, RPE versus RIR, duration/distance,
  unique set identities, immutable snapshots and validation ordering remain intact.
- Common five new tests bring **66 shared-domain JVM tests**, zero failure/error/skip.
  Original vs shared output **cmp MATCH** at kmp-{original,shared}-strength-evidence.log:
 229lines including ALL220 catalogue entries and original actual-session/validation output.
  Eight unchanged focused strength suites passed **383,950 checks**: Release05Domain253,
  StrengthRecords34, GymExecution173, StrengthVolume685, TechniqueGuidance1410,
  StrengthProgramme373245, GymTools7909, GymWorkoutSessionCoordinator241.
- Production ProgrammeProgressionSheet only captures checked nullable duration/distance/RPE/RIR
  locals before displaying the SAME actual supporting evidence.32 existing test fixture lookups
  now requireNotNull for their actual known canonical entry; no assertion/capture weakened.
  Experiment CI additionally includes original ProgrammeProgressionClarityTest; full default
  release gates unchanged.
- Final `/workspace/scratch/kmp-strength-evidence-verified-build.log` BUILD SUCCESSFUL,
 108tasks/1m8s, both APKs. Earlier logs found nullable display/fixture calls, all corrected
  without changing assertions. Common66tests/383950legacy checks/parity remain valid;
  formatter244Kotlin/149Java passed BEFORE the final fixture assertion edit; CI43476442298
  correctly found one line requiring wrapping in GymProgrammeOperationsTest. That formatting
  correction is applied in the next batch; no assertion change. No version/schema/backup change.
- Actual CI43476442699 SUCCESS on BOTH Apple and Android: real Native tests/framework;
  Android128focused tests (including11existing progression-clarity tests), full legacy domain/
  schema/build/lint. Formatting remains red on ac34 until the pending wrapped-line fix is pushed.
  No Native application or release-ready claim.
- `/workspace/scratch/StrengthProgrammeModelParity.java` and original228-line
  `kmp-original-strength-programme-model.log` retain original programme and all220 canonical
  metadata outputs. Used successfully by the next local batch; preserve the original oracle.

### Thirteenth checkpoint — shared programme/metadata contracts and actual exercise search

- COMMITTED/PUSHED `35d989e62a2b6551d02e860fd2a9151b3bef4975`: four more Java originals removed/
  replaced (GymProgramme, ExerciseMetadata, ExerciseMetadataCatalog, ExerciseSearch).
  **152 Java domain files remain**. All original programme goals/experience/effort IDs,
  prescription/set/rep/rest/load-reference validation,56-day original model limit, immutable
  revision snapshots and no-actuals creation preserved. Empty remaining-programme versions stay
  supported. No generation rules or six-week scheduling redesign.
- All220 explicit canonical metadata classifications retained, with original60equipment IDs,
  primary/secondary areas, reviewed setups/alternatives and suitability. Enum snapshots retain
  original EnumSet ordinal iteration. Unknown/custom metadata remains unknown, not inferred.
  Existing Immutable extended with copyMap: copied lookup index, canonical retained iteration,
  no map/entry mutation escape. Actual schema/backup/permissions/versions unchanged.
- Programme+metadata local gate `/workspace/scratch/kmp-programme-metadata-build.log` SUCCESS,
 117tasks/4m8s, both APKs; **74shared-domain +14shared-health JVM tests**. Original/new228-line
  programme+ALL220metadata output cmp MATCH in kmp-shared-strength-programme-metadata.log.
  Six original suites **438329checks**: GymProgramme25847, Allocation30390,
  StrengthProgramme373245, Release05Domain253, StrengthVolume685, GymTools7909.
  Earlier programme-only five suites429557 also passed, not additional unique checks.
- Search now migrated locally; original ranking, aliases, exact/prefix/word relevance, favourite/
  recent tie-breaks, excluded filter and original exercise instances retained. ASCII Java trim/
  word boundaries preserved explicitly rather than adopting Native ICU whitespace. Common tests
  include Unicode root-case and NBSP boundaries. Baseline30query/filter outputs saved BEFORE
  port in kmp-original-exercise-search.log; shared30-case output cmp MATCH at kmp-shared-exercise-search.log.
- Includes ac34 formatting correction with identical assertion. Final CI-style formatter
  --check --patch-output PASS:398source files (251Kotlin/147Java); diff check PASS.
  Final `/workspace/scratch/kmp-shared-search-build.log` **BUILD SUCCESSFUL**,117tasks/3m48s,
  both APKs, **77shared-domain JVM +14shared-health JVM tests**, zero failures/skips/errors.
  Legacy harness javap confirms actual Kotlin classes (not stale Java records) were loaded.
- Four further original search/metadata/exclusion/substitution suites passed **138915checks**:
  Foundation470, Metadata3173, Exclusion18212, Substitution117060. Original Release05Domain253
  passed again; it is already included in438329above, not additional unique checks.
- Exact CI43479723201 **SUCCESS on BOTH Android and Apple**, confirmed on this resume.
  Formatting43479722823 also SUCCESS. This verifies real Native programme/metadata/search
  classes/tests/framework, not a complete iOS app. Existing Android release matrix remains a
  final readiness requirement, not replaced by focused development coverage.
  Then continue typed exercise-exclusion/workout-definition/programme-draft contracts and
  remaining engines/UI, cross-platform typed storage/backup/retention/services and actual iOS
  app host. No full iOS application exists yet. No release or merge until the full scope passes.

### Fourteenth checkpoint — authored builder model and actual shared preview (`62e9867`)

- COMMITTED/PUSHED `62e9867e88e46f44d10e3224ae3a2d66dccff36f` replaces SIX Java originals:
  GymWorkoutDefinition, StrengthProgramme, StrengthProgrammeDraft, CustomStrengthExercise,
  StrengthWorkoutBudget, ExerciseExclusionRepository. **146Java domain files remain**.
  Existing draft edit/duplicate/move/reorder/preview operations, concrete dated blocks, revision
  lineage, frozen definitions, superset validation, unknown optional targets/rest/effort,
  incompatible-reference safeguards and source IDs retained. Preview never creates actuals or
  activates a programme. No schema/backup/version/permissions change.
- LocalDate adds only original plusDays/plusWeeks/minusDays/getDayOfWeek and epoch-day factory
  needed by actual authored programming. JVM remains java.time aliases; Native keeps full ISO
  range and checked arithmetic. isJavaBlank retains original JDK whitespace boundary, including
  NBSP being nonblank, rather than widening validation through Kotlin isBlank.
- **87shared-domain JVM tests +14shared-health JVM tests PASS**:8new common authoring tests;
  2JVM differential tests cover65536UTF16 whitespace units and2006full-range dates with9day/week
  adjustments plus weekdays. Original Java/shared output **cmp MATCH**,23lines, in scratch
  kmp-{original,shared}-authoring-models.log (only unordered settings sets are normalised).
  Legacy javap confirms real Kotlin authoring classes loaded, not stale Java records.
- Seven original affected suites passed **399742checks**: StrengthProgramme373245,
  GymTools7909, GymWorkoutSource60, ExerciseExclusion18212, ConcreteDraft31,
  ConcreteSubstitution44, GymWorkoutSessionCoordinator241. Full unchanged legacy suites run
  in CI; no repeated full final platform gate at this partial migration stage.
- Existing production ProgrammePreviewContent and CustomExerciseMetadataSummary moved into
  sharedUi. Android binds original date/goal/target formatters; same text, disclosure,7-date
  pagination and actions. No redesigned/demo screen. Two production nullable reads now use
  checked locals. Instrumentation fixtures explicitly assert required target/effort fields;
  same numeric/null assertions retained, no fake values or image edits.
- Final `/workspace/scratch/kmp-authored-final-build.log` BUILD SUCCESSFUL,117tasks/56s,
  both APKs. Earlier authored-compile/ui-build logs found Java-platform nullable assumptions,
  fixed before final gate. Formatter --check --patch-output PASS403files (262Kotlin/141Java); release-note10 PASS.
  Logs:`/workspace/scratch/kmp-authored-{format-check,release-check}.log`; diff check clean.
- Experiment-only focused CI adds EXISTING builder8/custom-exercise4/repository15/JSON6/
  exclusion-repository5 tests, retaining prior128 and all default final gate assertions.
  Actual CI43516659530 **SUCCESS on BOTH Android and Apple**. Android logs confirm166focused
  tests including those38 new existing cases, zero skipped/failed, schema75/build/lint/legacy
  domain. Real Apple authoring/date/UI tests/framework passed; NOT an iOS application.
  Formatting43516659481 SUCCESS. All results confirmed at exact62e9867 before the next push.
- Next safe action: inspect formatter/release result; fix errors; commit/push coherent batch
  to SAME draftPR24, update PR description and check exact Native/Android results. Then continue
  remaining engines/shared UI/orchestration/native storage/services/real iOS app host. No merge,
  release or version increment while full parity remains unfinished.

### Fifteenth checkpoint — shared actual strength-record engine (`3b234d7`)

- COMMITTED/PUSHED `3b234d777b43e728f4aa4c9af3a72283fa9deb08`: StrengthRecords.java replaced with actual
  common engine/key/point models. **145Java domain files remain**. Original JVM StrengthAnalytics
  estimatedOneRepMax API delegates to new common StrengthRecordPolicy; its other existing logic
  remains Java (not counted as migrated). No duplicate estimate formula or prescription input.
- Measured singles/records stay distinct from bounded2–10rep Epley estimates, warm-ups excluded,
  same exercise/mode/measurement keys, six-decimal ties-to-even load keys and original1e-7
  improvement tolerance. Exact source set/workout/date retained; duplicates idempotent/conflicts
  rejected; future evidence filtered by explicit as-of. Source offset, not current timezone,
  defines recorded date. Unvalidated raw point/key DTO optional fields remain nullable.
- CalendarDates.atOffset uses original java.time on JVM and checked epoch-day conversion on
  Native, retaining full calendar ranges/offset-second semantics. No zone database or clock guess.
- Existing records-screen algorithm/text/actions unchanged: checked required keys/source actuals
  from progression() output, no fake missing values. Experiment CI adds existing three-case
  StrengthRecordsScreenTest (expected169focused total; actual result must be checked).
- **93shared-domain JVM +14shared-health JVM tests**, zero failures/errors/skips; five new common
  record-policy tests plus a JVM differential checking70063offset-date cases and10007bounded
  load-rounding fixtures. Original/new26-line records/estimate output **cmp MATCH** at scratch
  kmp-{original,shared}-strength-records.log. Original StrengthRecords34 and Release05Domain253
  checks PASS. Actual working sets/reference distinctions and retained exercise definitions stay.
- Final `/workspace/scratch/kmp-record-final-build.log` BUILD SUCCESSFUL,117tasks/2m17s,
  APK+testAPK. Earlier kmp-record-policy-build.log exposed nullable Java-interop UI reads, fixed
  before final verification. Formatter --check --patch-output PASS406files (266Kotlin/140Java),
  kmp-record-format-check.log. Git diff check clean.
- Exact CI43518706438 SUCCESS on BOTH Android and Apple; Android logs confirm169focused
  tests, zero failures/skips, plus schema75/full legacy domain/build/lint. Native record policy,
  stored-offset dates and shared framework passed. Formatting43518706458 SUCCESS. These are
  framework/development gates, not a complete iOS application or final release matrix.

### Sixteenth checkpoint — shared strength context and historical timelines (`94aa212`)

- COMMITTED/PUSHED `94aa2129c49de82ce492d166bff193c13c7b105c` follows3b234d7.
  THREE Java originals replaced: StrengthAnalytics, HistoricalStrengthRecord, StrengthProgression.
  **142Java domain files remain**. Original context, bounded estimates, legacy rep buckets,
  loaded-task comparisons, approximate/unknown date precision and manual provenance preserved.
  Missing effort stays unknown; warm-ups do not enter working-set context/PBs. Manual records
  never fabricate completed workouts. One common estimate policy remains authoritative.
- Historical records retain original validated measurement profiles, date-precision constraints,
  notes/identity and nullable dates. Timeline sorting keeps unknown dates last/outside charts;
  old best/tie/NaN/signed-zero behaviour retained. Existing source offsets, not current device
  timezone, determine recorded dates. Session-load math keeps toMillis truncation and evaluation
  order on validated <=12h sessions; missing session RPE is not inferred from set averages.
- **99shared-domain +14shared-health JVM tests PASS**, zero failures/errors/skips, including
  six new common context/history tests. Original Java/common11-line output **cmp MATCH** in
  scratch kmp-{original,shared}-strength-context.log. Seven original suites **30461checks**:
  Release05Domain253, StrengthTodaySummary58, Refinement97, Coaching29602, WeeklyCoaching96,
  TrainingRecovery321, StrengthRecords34. No assertions weakened or algorithms redesigned.
- Four Android UI/orchestration files now explicitly check known computed fields/filtered dates.
  Unknown historical dates still render as unknown and stay outside graphs. Original UI strings,
  chart calculations, callbacks, future-date guard and independent actual/manual history remain.
- Final `/workspace/scratch/kmp-strength-context-final-build.log` BUILD SUCCESSFUL,
 117tasks/2m19s, both APKs. Earlier context-build log found Java-nullability reads, fixed before
  final gate. Formatter --check --patch-output PASS410files (270Kotlin/140Java), release10 and
  git diff check PASS. APK confirms0.12.3-test1/code19 and no INTERNET permission. SQLite19 /
  JSON13 /raw3 /complete1 unchanged. No version increment/publication/merge.
- Experiment CI adds original WeeklyUxTest five cases (historical unknown PB, explicit-zero shoe
  data, body entry/calendar/settings), preserving prior169 and all default full gates. Expected
 Actual CI43578194651 SUCCESS on BOTH Android and Apple. Completed Android logs confirm
 174focused runtime tests with no failures/skips, schema75, full legacy/build/lint. Real Native
  context/historical-timeline tests/framework passed. Formatting43578194654 SUCCESS. Still no
  complete iOS app or final unfiltered release-matrix claim.
- Next exact action: inspect the source checkpoint's real Android/Apple/format CI and fix actual
  failures. Then continue remaining shared execution/coaching engines and actual UI/state,
  typed native storage/backup/retention, device services, full HealthKit integration and the real
  iOS app host. Continue SAME draftPR24; do not restart completed Java conversions or UI.

### Seventeenth checkpoint — shared weekly strength analysis and production screen (`a54b993`)

- This source checkpoint follows94aa212 (inspect current git log/PR head for its commit).
  StrengthVolume.java replaced with common Kotlin; **141Java domain files remain**.
  Monday windows, explicit empty weeks, as-of filtering, duplicate/conflict checks, actual
  working-set totals, primary fractional shares/separate secondary exposure, unknown/changed
  definition classification and exact exercise/task load totals preserve the original rules.
  No body-mass inference, mixed-modality equivalence, new physiology or prescription inputs.
- Week maps retain original EnumMap ordinal ordering and nullable raw map values. Existing
  Immutable gained an internal snapshot variant permitting null VALUES for this original DTO
  contract; normal copyMap still rejects nulls. No mutable map-entry escape. Working-set absence
  remains recorded absence, not proof the athlete did not train. Warm-up-only workouts preserved.
- Entire original production StrengthAnalyticsScreen now shared Compose: asynchronous projection,
  window filters, progressive disclosure, charts, semantics and strings retained. Android wrapper
  supplies original Instant/LocalDate clock, UK date/number/duration formatting and existing
  working-set explanation. No extra dependency or separate/demo iOS screen.
- **103shared-domain +14shared-health JVM tests PASS**, zero failures/errors/skips; four new
  common volume tests cover attribution, unknown metadata, source filtering/empty windows,
  null-value/ordinal immutable snapshots and real task totals. Original/new3-line volume output
  **cmp MATCH** in scratch kmp-{original,shared}-strength-volume.log. Existing volume685 and
  GymTools7909 checks PASS (8594total). No assertion weakened.
- `/workspace/scratch/kmp-strength-volume-build.log` BUILD SUCCESSFUL,117tasks/3m29s,
  APK + testAPK. CI-style formatting --check --patch-output PASS412files (273Kotlin/139Java),
  git diff check clean. Version0.12.3-test1/code19; SQLite19/JSON13/raw3/complete1 unchanged.
  Actual screenshot regeneration/full Android matrix/iOS application build remain final work.
- Current experiment focus already contains StrengthAnalyticsScreenTest, including real light/
  dark/200% evidence/disclosure cases; no test selector/assertion change for this batch.
  Actual exact-head experiment43579335061 SUCCESS on BOTH Android and Apple; formatting
 43579335041 SUCCESS. Shared tests, retained domain/schema/build/lint, real runtime checks and
 Native tests/framework passed. This does not establish final platform/application parity.
- Next exact action: inspect this checkpoint's exact PR24 CI, fix any actual failure; then continue
  remaining domain/execution/coaching engines and shared UI/state/orchestration, typed native
  SQLite/backup/retention and services, full HealthKit integration, real iOS app host. Preserve
  all existing Android Health Connect/GPS/history. No release/version bump/merge before full parity.

### Eighteenth checkpoint — common Gym Now execution, durability coordination and preparation

- This source checkpoint follows a54b993 (inspect git log for its final commit). EIGHT Java
  originals replaced with common Kotlin: GymExecution, GymExecutionSet, GymExecutionEngine,
  GymSupersets, GymWorkoutAdapter, GymExecutionRepository, GymWorkoutSessionCoordinator and
  GymWarmup. **133 Java domain files remain.** No second execution engine or duplicate database.
- Frozen prescriptions/provenance, original SHA-256 programme-session IDs, event replay and
  actual-set order remain intact. JVM uses the original system MessageDigest; Apple uses
  system CommonCrypto. Original UTF-16 length/UTF-8 replacement of isolated surrogates retained.
  Original enum order, wire format1, IDs and record text preserved. No schema/backup bump.
- Monotonic timing, explicit interrupted-clock recovery, paused versus active time, unknown
  downtime, partial completion, warm-up exclusion and independent swapped-exercise evidence
  are preserved. Stable-ID identical retries remain idempotent; conflicts never overwrite actuals.
  A failed command cannot replace the last good snapshot. Stored versus proposed evidence remains
  separate. Coordinator retries freeze the exact operation/snapshot/revision/clock observation.
- Typed durability contract is shared; existing Android SQLite remains authoritative. Accepted
  evidence is published only after a matching acknowledgement. Complete operations are serialized
  by a reentrant JVM monitor / Apple NSRecursiveLock, including repository IO; no UI-thread IO.
  Explicit nullable load/acknowledgement contracts retain missing/invalid evidence. Android callers
  now check preserved IDs/known rows explicitly; fixtures assert expected non-null evidence.
  No assertion removal, delay, training-rule or persistence-semantic change.
- Original conservative warm-up engine and real production preparation card now shared. Original
  freshness (including fractional 28-day boundary), exact exercise, effort, related movement,
  modality and reference cap rules preserved. Suggestions only stage the editable form, never
  actuals. Collapsed/evidence disclosures, copy, callbacks and semantics retained; Android supplies
  the original UK one-decimal formatter and still owns rest notifications/permission handling.
- **122 shared-domain JVM +14 shared-health JVM tests PASS**, no failures/errors/skips. Nineteen
  new tests cover replay corruption, clock/recovery gaps, partials, warm-ups/substitutions/rounds,
  source IDs, signed-zero retry effort, before/after-commit failures, wrong acknowledgements,
  reentrant lock release and 64 competing saves without lost sets/revisions. Original six suites
  **9085 checks PASS**: execution173/tools7909/coordinator241/source60/association658/substitution44.
  Original/new outputs **cmp MATCH**: execution7 +identity6 +warmup9 lines, scratch
  kmp-{original,shared}-gym-{execution,identity,warmup}.log. Offline harness uses actual Kotlin.
- Schema75/release-note10 tests PASS. CI-style formatter421 scoped files (290Kotlin/131Java)
  and diff check PASS. Initial compile surfaced Kotlin's newly explicit nullable row/source types;
  direct production checks and fixture non-null assertions correct those without weakening tests.
  Final kmp-gym-verified-build.log BUILD SUCCESSFUL:117tasks/1m12s, both APKs. Shared
 tests were up-to-date after the final fixture-only correction; all instrumentation sources compile.
- Experiment selector adds existing execution SQLite14/JSON12/backup12, engine2/coordinator1/
  source1 and substitution3/authored GymNow6 cases, retaining all prior/default final gates.
  Exact cff9be2 experiment43582331804 SUCCESS on BOTH Android and Apple:225 runtime tests,
  zero failures/skips, retained domain/schema/build/lint, real Apple tests/framework. Formatting
 43582331839 SUCCESS. No new full screenshots or final Android matrix claim.
- Version0.12.3-test1/code19, SQLite19/JSON13/raw3/complete1 unchanged. APK permission inspection
  confirms no INTERNET. No public artifact/version increment/merge. Full iOS application host,
  typed native SQLite/backup/retention/services and app-wide shared UI/orchestration remain open.

### Nineteenth checkpoint — common intent-ranked substitution and real busy-equipment dialog

- This source checkpoint follows cff9be2 (inspect git log for its final commit). Existing
  ExerciseSubstitutionEngine.java replaced with common Kotlin; **132 Java domain files remain**.
  Original primary-role/body-area/task/equipment/experience gates, reviewed easier/harder pairs,
  goal/equipment/role/measurement/favourite/history ranking and stable ID tie-breaker preserved.
  No clinical clearance, loading transfer, invented alternatives or recommendation changes.
- Exact context validation, frozen inputs, reason order and bounded results retained. The parity
  harness caught ordinal equipment iteration changing to hash order; fixed with an internal
  ordered immutable-set snapshot. Normal Immutable.copySet keeps its existing unordered semantics.
  No mutable iterator/entry escape and no global collection-order rewrite.
- Entire real GymSubstitutionSheet and GymSwapState moved to shared Compose. Android wrapper only
  collects existing lifecycle state and supplies original callbacks/exercise-context formatter.
  Same full-screen dialog, concise alternatives, equipment/reason disclosures, explanation
  expansion, empty/loading/error states, confirmation, scrolling and semantics. No fake iOS UI,
  duplicate decision engine or new navigation. Labels use the same fixed ASCII enum names.
  Today-only replacement/unsaved-form clearing still occurs only through existing explicit IO.
- **128 shared-domain JVM +14 shared-health JVM tests PASS**, no failures/errors/skips. Six new
  common tests include2640 canonical catalogue/experience/equipment/reason combinations, exact
  setup/task/role/exclusion gates, reviewed difficulty relationships, missing/frozen metadata,
  non-medical wording, load warnings and immutable original ordinal equipment snapshots.
  Original ExerciseSubstitutionEngine117060 +concrete substitution44 = **117104 checks PASS**.
- Original/new **39600-combination parity MATCH** for complete contexts, ranked IDs, explanations
  and task flags (all220 exercises ×5 goals ×3 experiences ×6 reasons ×2 equipment setups).
  SHA-25653c0110cb575c99a3e094bf5e68e726ae091f970e93b3ef7aede03f98662c13b.
  Scratch kmp-{original,shared}-substitution.log and SubstitutionParity.java retain the proof;
  original source/classes preserved outside git. No assertion weakened or golden output normalised.
- Final kmp-substitution-verified-build.log BUILD SUCCESSFUL,117tasks/3m17s, both APKs. CI-style
  formatting424 scoped files (294Kotlin/130Java) and diff check PASS. Existing cff9be2 already
  verified schema75/release10 and225 real Android cases including production swap/large-text/
  authored execution and actual SQLite/JSON/backup tests. Exact e287ddd experiment43583530747
  SUCCESS on BOTH Android/Apple:225 real runtime cases/no failures or skips, retained domain/
  schema/build/lint and Apple tests/framework. Formatting43583530752 SUCCESS. No borrowed pass.
- SQLite19/JSON13/raw3/complete1 and0.12.3-test1/code19 unchanged. No new runtime dependency,
  permission, schema, public release or merge. Full app parity and actual iOS host remain unfinished.

### Twentieth checkpoint — common source validation and explicit association protocol/card

- This source checkpoint follows e287ddd (inspect git log for its final commit). FOUR more Java
  originals replaced with common Kotlin: GymWorkoutSourceValidator, GymWorkoutAssociation,
  GymAssociationRepository and GymAssociationCoordinator. **128 Java domain files remain**.
- Preserved sources are revalidated against exact retained versions, not today's catalogue.
  Legacy sources authenticate stored exercise IDs/order/targets without pretending old names
  were stored; authored/saved sources compare the full frozen definition, nullable/fractional
  RIR targets and optional load references. Repeat sources do not copy actuals or load references.
- Association discovery remains limited to three stored local days and an exact working task;
  warm-up-only, occupied, not-yet-completed and already planned work cannot invent eligibility.
  Authored open slots do not invent comparable prescriptions. Exact actuals, old revisions,
  explicit choices, transaction-time revalidation, reversals and conflicting association guards
  retained. Source timestamps use exact seconds/nanoseconds, never floating-point elapsed time.
- Reviewed-choice protocol keeps the exact decision ID/proposal/choice/time through lost ACK,
  wrong ACK or post-commit read failure. Pending stays pending until a matching acknowledgement
  and successful read or explicit reload. Clock/ID suppliers are small common functional
  interfaces; Android injects the same UTC instant source. The single original Java clock fixture
  now passes its unchanged fixed clock via ::instant. No assertion or expected value changed.
  Existing calling IO serialization retained; no speculative new association locking/engine.
- Entire real association card/confirmation now shared Compose. Android supplies the original UK
  date and target formatters. Actual date still uses its retained offset; historical actuals,
  preserved prescriptions and association choices stay visibly distinct. Text, actions, tags,
  scrolling, disabled states and confirmation remain intact; no auto-completion or auto-linking.
- **136 shared-domain JVM +14 shared-health JVM tests PASS**, no failures/errors/skips. Eight new
  common tests cover retained-local-day boundaries, warm-ups, future completion, stale/occupied
  reviews, source validation, exact fractional RIR, unknown/open targets, frozen definitions,
  lost/wrong acknowledgements, post-commit read failure, reversal and explicit reload.
  Original five suites **1176 checks PASS**: association658/source60/coordinator241/execution173/
  concrete substitution44. Original/new association/discovery/decision output **336lines cmp MATCH**
  in scratch kmp-{original,shared}-association.log / AssociationParity.java. Original sources
  remain outside git for diagnosis. No assertion weakened.
- Initial test fixture incorrectly requested a calculated time budget for an open slot. Corrected
  to an explicit athlete-entered fixture duration, retaining the existing unknown-budget guard.
  Android's authored-family fixture now asserts its known non-null prescription; same assertions.
- Final kmp-association-verified-build.log BUILD SUCCESSFUL,117tasks/1m7s, both APKs; JVM tasks
  up-to-date after the final Android-fixture-only correction. CI-style formatting426 scoped files
  (300Kotlin/126Java), diff and APK version/permission inspection PASS (code19/min28/noINTERNET).
- Experiment selector adds original association SQLite7/authored-SQLite6/JSON5/UI3 tests,
  retaining all225 prior cases and all default final gates. Exact fa6cb1f experiment43585087542
  SUCCESS on BOTH Android/Apple:246 real API35 cases/no failures/skips,700189 retained checks/
 48suites, schema75/build/lint and Apple tests/framework. Formatting43585087561 SUCCESS.
- Version0.12.3-test1/code19; SQLite19/JSON13/raw3/complete1 unchanged. No schema migration,
  public artifact, version bump or merge. Full iOS app host/storage/services/integration and the
  remaining Java engines/shared UI/state remain unfinished; framework success is not an app.

### Twenty-first checkpoint — shared recommendations and approved progression experience

- This source checkpoint follows fa6cb1f (inspect git log for its final commit). FOUR more Java
  originals replaced: polish/StartingLoad, StrengthProgressionAdvisor, StrengthTargetProgression
  and presentation/StrengthPrescriptionText. **124 Java domain files remain.** No engine redesign.
- Starting references keep exact exercise/weighted mode/recent working sets/recorded effort,
  conservative range and unknown fallback. Progression preserves the two-comparable-session rule,
  each observation's28-day boundary including nanoseconds, intended effort, consistent load,
  original half-kg increment/order/ceilings, modality-specific rep/time/distance suggestions,
  optional references and stale/newer-reference safeguards. No prescription or actual mutation.
  Legacy advisor overload behaviour (including duplicate input IDs) and raw DTO null/option-list
  behaviour retained; authored task advice retains its separate duplicate-identity safeguard.
- Entire real approved ProgrammeProgressionContent and review DTO now shared Compose. Original
  guidance-first hierarchy, compact changed/unchanged comparisons, nested supporting-workout
  disclosure, collapsed defaults/restoration, keyboard/TalkBack semantics,200% stacking, canvas
  glyphs, styles, labels and separate save/activation remain intact. Android owns only lifecycle/
  dialog and original UK date formatter. Recorded dates use retained offsets, not device zone.
- Presentation instructions/comparisons are common. JVM decimal formatting keeps original
  BigDecimal exactly. Native expands the platform's round-trip Double text to plain decimal and
  uses a shared exact digit subtraction for non-negative reference-load text, without rounding or
  a precision cap. These strings never drive calculations or stored values. Platform shortest
  representations are not claimed byte-identical for every arbitrary Double. Common full-finite-
  range round-trip tests and JVM expansion/subtraction checks protect against lost precision.
- **151 shared-domain JVM +14 shared-health JVM tests PASS**, no failures/errors/skips (15 new
  tests). Original five suites **4723 checks PASS**: target152/text114/Release08173/today58/
  six-month4226. All three calendar-six-month athlete histories pass with decisions unchanged;
  these are deterministic evidence tests, not physiological outcome claims.
- Original/shared output **MATCH** across3840 recommendation configurations (all supported task
  families, effort/missing/warm-up/age/load/success/duplicate variants) and180 complete instruction/
  reference/comparison cases. Scratch RecommendationParity.java / PrescriptionTextParity.java and
  kmp-{original,shared}-{recommendation,prescription-text}.log retain proofs; originals outside git.
- Exact nullable optional-target/raw-advice contracts are now explicit in Kotlin. Android's computed
  advice consumers and known-success fixtures check their non-null values, retaining assertions.
  One initial shared UI compile surfaced the old Android date formatter; corrected to an injected
  UK date callback and common stored-offset conversion. No test weakening or altered target values.
- Final kmp-progression-verified-build.log BUILD SUCCESSFUL117tasks/1m9s, both APKs. Shared tasks
  up-to-date after the final Android-fixture-only correction. CI-style formatting435 scoped files
  (312Kotlin/123Java), diff and APK checks PASS (0.12.3-test1/code19/min28/noINTERNET).
- Existing11 progression clarity UI cases remain selected. Experiment adds original19 generation/
  progression-persistence cases, retaining all246 prior cases (265 expected), old source/backup/
  repository checks and default full gates. Exact fa0c88a experiment43603001097 SUCCESS on
  BOTH Android and Apple:265 runtime cases/no failures or skips,700189 retained checks/48suites,
  schema75/build/lint and actual Native tests/framework. Formatting43603001007 SUCCESS.
- SQLite19/JSON13/raw3/complete1 unchanged. No schema migration, new runtime dependency, version
  bump/public release/merge. Remaining engines, native storage/services and real iOS host are open.

### Twenty-second checkpoint — common programme revisions and typed authoring contract

- This source checkpoint follows fa0c88a (inspect git log for its final commit). TWO more Java
  originals replaced: StrengthProgrammeEditor and StrengthProgrammingRepository. **122 Java
  domain files remain.** Android's existing typed SQLite/transactions remain authoritative.
- Future regeneration preserves exact historical/completed workouts, protected evidence,
  immutable revisions, explicit activation, availability and source/provenance guards. Permanent
  alternatives retain context/task checks and clear incompatible references. No actuals are created.
- Regenerated collision IDs retain original UUID v3/UTF-8 semantics, including isolated-surrogate
  replacement and reserved-ID salt. JVM uses original MD5; Native system CommonCrypto. This is
  legacy identity compatibility, not cryptographic authentication. Existing SHA-256 execution
  identities reuse the same unchanged common UTF-8 helper. Dates compare epoch days on both
  platforms; no JVM-only LocalDate comparison leaks into common source.
- Repository missing rows/pointers are explicitly nullable; successful decisions/reviews are
  non-null. Original DTO validation, immutable decisions, raw summary nullability and compare-
  activate contracts retained. Android checks known metadata/source IDs; test fixtures explicitly
  assert known created IDs and expected stored programmes without removing any assertion.
- **159 shared-domain JVM +14 shared-health JVM tests PASS**, no failures/errors/skips. Eight new
  tests cover retained history, Unicode/surrogate collision identities, reserved IDs, protected/
  missing evidence, forward revisions, unavailable days, substitutions/reference clearing and
  immutable decision evidence. Original six suites **374190 checks PASS**: programme373245/
  concrete substitution44/concrete draft31/target152/association658/source60. Original/shared
  27line programme edit identity/target/decision output cmp MATCH; scratch ProgrammeEditParity.java
  and kmp-{original,shared}-programme-edit.log. No golden normalisation or assertion weakening.
- Final kmp-programme-revision-verified-final-build.log BUILD SUCCESSFUL117tasks/1m16s, both
  APKs. CI-style formatting438scoped files (317Kotlin/121Java) and diff check PASS. APK remains
  code19/min28/noINTERNET. Exact pushed-head Android/Apple runtime CI is still required; no final
  full matrix/screenshots claim. Shared tests up-to-date after Android-fixture-only fixes.
- Version0.12.3-test1/code19, SQLite19/JSON13/raw3/complete1 unchanged. No new permission, public
  artifact, version bump or merge. Native storage/services/real iOS host remain unfinished.

### Twenty-third checkpoint — common concrete previews, permanent swaps and history screen

- This source checkpoint follows53ab0fc (inspect git log for its final commit). THREE more Java
  originals replaced: ConcreteStrengthProgrammeDraft, ConcreteStrengthProgrammeEditor and its
  typed repository. **119 Java domain files remain.** Saving a preview still cannot activate it
  or create actuals; original Android SQLite/JSON/backup transactions remain authoritative.
- Every dated workout/phase target/explanation remains preserved. Explicit swaps retain original
  accepted intent across chained choices, replace only eligible future slots, honour exclusions/
  protected evidence, require fresh individual targets for changed measurements and clear load
  references. Reversing a pending swap removes its decision while keeping the reference cleared.
  Original checked revision overflow, lineage guards, complete-target and unique-slot validation
  retained. Raw summary unknowns stay unknown; known Android stored rows are checked explicitly.
- Entire real read-only programme-history screen is shared Compose. Android keeps lifecycle and
  the original dialog/date/target formatting. Same cards, paging, lazy lists, retained-version/
  workout navigation, disclosures, saved selection, reset-to-top and accessibility semantics.
  Dates sort by epoch day on Native/JVM. No historical activation action is introduced.
- **167 shared-domain JVM +14 shared-health JVM tests PASS**, no failures/errors/skips. Eight new
  common tests cover dated edits, protected history, immutable previews, raw summary unknowns,
  lineage/overflow, phase-specific all-occurrence targets, exclusions, changed tasks and reversal.
  Original six suites **374190 checks PASS**. Original/shared **3453line cmp MATCH** for complete
  preview/workout/decision output, ranked choices and reversals across all6 reasons/3 identities.
  Scratch ConcretePreviewParity.java / kmp-{original,shared}-concrete-preview.log preserve proof.
- Schema75 and release-note10 PASS. Formatting440scoped files (322Kotlin/118Java) and diff PASS;
  final import-only wrapper correction retains sorted imports. Initial main compile surfaced raw
  summary nullability and missing original date import; explicit checks/import fixed those without
  removing assertions. Final kmp-concrete-preview-final-build.log BUILD SUCCESSFUL108tasks/2m19s,
  both APKs. Shared JVM tests had passed before the final Android-only import correction.
- Focus adds14 existing concrete-draft SQLite/JSON/old-schema/backup/transaction/activation tests
  to all265 prior cases (279 verified in the following checkpoint; the initial13/278 estimate
  missed one wrapped Java method declaration); original19 generation UI cases include real historical
  read-only review. Exact new-head Android/Apple/format runtime gate remains pending after push.
- Version0.12.3-test1/code19, SQLite19/JSON13/raw3/complete1 unchanged. No public artifact/merge.
  Full platform UI/state, native persistence/services/HealthKit and real iOS host remain open.

### Twenty-fourth checkpoint — common programme construction, templates and real configuration

- This source checkpoint follows b75baba (inspect git log for its final commit). TWO more Java
  originals replaced: StrengthProgrammeGenerator and StrengthProgrammeTemplates. **117 Java
  domain files remain.** All nine original structures/copy and unvalidated raw-template unknowns
  retained. No load is inferred, programme activated or actual created by generation.
- Original goals, experience-aware roles/phases, strength/muscle/power targets, conservative
  beginner power limitations, time-budget volume reduction/reservations, canonical equipment/
  exclusion gates, favourite tie-breaks and stable selection retained. Weeks anchor to actual
  start dates; Upper/Lower variations remain distinct. Hybrid run penalties, race windows and
  bounded insertion-ordered explanations are unchanged. Dates compare exact epoch days.
- Entire actual programme-configuration form and existing generation/swap UI state now shared.
  Original remembered fields, equipment disclosure, semantics, keyboard choices, optional recovery
  checkbox, error/disabled state and explicit preview callback remain. Android supplies its exact
  strict UK date parser/formatter, local today, ISO weekday conversion/labels and original labels.
  No new default date, silently applied schedule or separate authoring engine is introduced.
- **175 shared-domain JVM +14 shared-health JVM tests PASS**, no failures/errors/skips. Eight new
  common tests run1080 combinations plus every start weekday/frequency, phases/experience/goals,
  actual metadata/equipment/exclusion/time bounds, power limitations, race/hybrid context, nine
  templates, immutable request inputs and invalid durations. Missing eligible roles remain honest
  limitations or the original explicit rejection; no invented balanced workout is returned.
- Original six suites **374190 checks PASS**. Complete original/shared generation output cmp MATCH
  across1080 configurations (all5goals/3experiences/3priorities/6frequencies/4time budgets, varying
  starts/lengths/templates/equipment/exclusions/favourites/focus/runs/races/recovery). Both80MB
  outputs SHA25621f62dae161821b529e5a84b08b029f6c83df192f0501d90f0497b41530b669b.
  Scratch ProgrammeGenerationParity.java / kmp-{original,shared}-programme-generation.log retain
  proof. No sorting away output differences, numeric normalisation or weakened assertion.
- Final kmp-programme-generation-final-build.log BUILD SUCCESSFUL117tasks/2m47s, both APKs.
  CI-style formatting443scoped files (327Kotlin/116Java) and diff check PASS. Known raw template
  strings are explicitly checked in Android/shared production forms; their optional/default
  behaviour is retained. No schema/backup change since successful schema75/release-note10.
- Exact99b9b26 experiment43606970897 SUCCESS on BOTH Android and Apple; formatting43606970889
  SUCCESS. Completed Android logs show279 unique START/END pairs, no failures/skips,700189 legacy
  checks/48suites, schema75/build/lint. Actual extra repository class contains14 tests, correcting
  the earlier13/278 estimate; no test/selector/assertion changed to alter the observed count.
- Digest-verified CI result ZIPs confirm175 domain+14 health JVM tests and162 domain+16 health
  Native tests, zero failures/errors/skips. All24 new revision/preview/generation common tests
  pass on Native. Real iosX64 tests and shared UI framework link executed, BUILD SUCCESSFUL6m53s.
  Artifacts14119350912/JVM and14118014340/Apple are tied to99b9b26. Framework success is not an app.
- CI APK artifact14118517512 is source-bound to99b9b26; ZIP SHA256d84e1a430370bac3f11aae6b8d017654f7d83258212a6e27ec20cb5b9ff884c8 verified.
  Actual APK56,791,735bytes/SHA25698937fcfba1e33c88cb725436fd01914149864068a8d5621de2cb91140796470;
  inspected0.12.3-test1/code19/min28/noINTERNET. Experimental Actions artifact only, no rolling
  publication. Full unfiltered API28/35/36/app screenshots and actual iOS application remain open.
- Version0.12.3-test1/code19, SQLite19/JSON13/raw3/complete1 unchanged. No public release or merge.
  A shared framework is not a complete iOS app; native app/storage/services parity remains open.

### Twenty-fifth checkpoint — common original guidance/artwork and real technique presentation

- This source checkpoint follows99eaa6a (inspect git log for its final commit). Its exact recovery
  head passed43608429805 Android/Apple and43608429838 formatting. ELEVEN Java originals now
  replaced by common Kotlin: TechniqueGuide, TechniqueIllustrations, ExpandedExerciseGuides and
  their eight original catalogue/delegation helpers. **106 Java domain files remain.**
- Original case tables, fallback delegation, variant-specific instructions, cues, mistakes,
  alternatives, asset IDs/descriptions and phase captions retained. No generic pose or metadata-
  inferred pattern replaces movement-specific content. All220 canonical guides and14 running
  routine illustrations remain complete;223 licensed packaged assets/11,634,030bytes pass exact
  provenance verification. Unknown/custom movements still have no invented guide/artwork.
- Original raw guide DTO nulls/list references and artwork validation/snapshot semantics retained;
  private pipe-list splitting keeps Java trailing-empty behaviour. Expanded identity set immutable.
  Source-based screenshot guard now reads ExpandedExerciseGuides.kt with the SAME exact identity/
  mode assertions; all23 tests PASS. No hardcoded replacement set or skipped capture guard.
- Actual TechniqueSection content and original GuidanceStyle surfaces now shared Compose. Android
  retains actual licensed image loading/TalkBack description, image geometry and original window/
  font-scale policy. Same numbered steps, phase captions, notes, alternatives and optional real
  guidance disclosure. Native still needs its real packaged-asset adapter and app integration.
- **183 shared-domain JVM +14 shared-health JVM tests PASS**, zero failures/errors/skips. Eight new
  common tests cover all220/46/14 identities, variants, loaded-vs-unloaded art, raw constructors,
  immutability, absent/custom evidence and private Java split semantics. Original guidance1410+
  catalogue240+Release05253 = **1903 checks PASS**; strict ExerciseGuidanceAudit220/220+14/14 PASS.
  Complete original/shared1211line lookup output cmp MATCH, SHA256
  8c49373d52dfc73143f7deadd5c3156f2af54dffd2711b5096f94b9023cdff89. Originals/snapshot remain in scratch.
- Android nullable screenshot/artwork fixture reads now explicitly assert known canonical lists,
  retaining all original assertions and clipping checks. Four new production-wrapper UI cases
  exercise light/dark/200%/unknown plus real disclosure expansion/collapse; existing2 packaged-
  artwork tests join all279 prior focused cases (285 expected). Runtime/Native verification pending
  the next coherent push; no final screenshot or full-matrix acceptance claim.
- Final kmp-technique-final-build.log BUILD SUCCESSFUL108tasks/55s, both APKs; shared JVM checks
  passed before Android-fixture-only corrections. CI-style formatting455scoped files (340Kotlin/
  115Java), diff and APK code19/noINTERNET inspection PASS; final format check log in scratch.
- Version0.12.3-test1/code19, SQLite19/JSON13/raw3/complete1 unchanged. No public release or merge.
  Next legacy planner/factory/editor sources and original1080-case GymPlanningParity.java baseline
  are prepared under /workspace/scratch/gym-planning-original; proposed Kotlin files are in scratch,
  NOT yet installed in repository at this source checkpoint. Continue implementation, not a restart.

### Twenty-sixth checkpoint — common legacy gym planner, one-off factory and future-session edits

- This source checkpoint follows2971409 (inspect git log for its final commit). THREE remaining
  Java strength-package originals replaced: GymProgrammeGenerator, GymNowWorkouts and GymPlanEditor.
  **103 Java domain files remain; domain.strength itself now contains NO Java implementation.**
  This is domain migration, not full platform/application parity or a completed iOS app.
- Original bounded six-week permutation search, start-anchored weeks, lower-session ordinal,
  priority-sensitive demanding-run/actual/planned adjacency penalties, pattern-sensitive favourite
  selection, equipment/exclusion fallbacks, time budgeting and explanation order retained. Core-
  only work remains distinct from meaningful lower-body work. No sessions silently disappear.
- One-off generation retains private fixed reference calendar, exact planner selection, honest
  missing-pulling/power/experience limitations and no invented starting load. Only the date-free
  definition is returned; programmes and actuals are never saved. All original overloads retained.
- Explicit move/skip/reduce/swap/reference actions create new versions; past/completed work cannot
  change. Original recurring days/other sessions preserved, swaps clear load references and reject
  incompatible tasks. Legacy version arithmetic/validation retained without a new progression rule.
- Presentation bridge keeps the exact existing UK java.time formatter on Android/JVM, including
  its platform locale data. Native uses explicit UK month/year formatting over the supported
  proleptic date range; stored date IDs and all date calculations stay common and exact.
- **191 shared-domain JVM +14 shared-health JVM tests PASS**, zero failures/errors/skips. Eight new
  common tests cover every start weekday/frequency,1080 equipment/goal/experience/priority/time
  cases, distinct lower variations, favourite intent, directional hybrid context, protected
  history, optional task/load references, one-off isolation and96 full-range date cases.
- Original six suites **77780 checks PASS**: gym25847/allocation30390/GymNow19720/guidance1410/
  catalogue240/Release08173. A temporary CLI launcher used the wrong Release08 class name; ran
  the actual polish.Release08TestSuite successfully without rerunning/weakening passed suites.
- Complete original/shared **14136line cmp MATCH**,1080 configurations/3240 one-off requests plus
  reduce/skip edits and96 date vectors. Both outputs SHA256
  32dde5a936e1d801886013a34d7096795d3fe1769ace4824b85bb91407bbec66. Scratch GymPlanningParity.java,
  gym-planning-original and kmp-{original,shared}-gym-planning.log retain exact proof.
- Final kmp-gym-planning-final-build.log BUILD SUCCESSFUL117tasks/3m40s, both APKs. Formatting459
  scoped files (346Kotlin/113Java), diff, schema75 and release-note10 PASS. Existing Android
  callers/test sources compile without altered assertions. No schema/backup/permission change.
- Exact e2fadac experiment43630748155 SUCCESS on BOTH Android and Apple. Completed Android
  logs confirm285 unique runtime cases/no failures or skips,700189 checks/48suites and schema75/
  build/lint. Actual iosX64 tests +shared UI framework link executed, BUILD SUCCESSFUL5m16s.
  Digest-verified results14128376837/JVM and14127787601/Apple confirm191+14 JVM and178+16 Native
  tests, zero failures/errors/skips; proof /workspace/scratch/e2-test-verification.json. Formatting
  run43630748164 is recorded separately; inspect its actual status. Full matrix/screenshots/app
  parity remain unfinished. No runtime failure was hidden or assertion weakened.
- Version0.12.3-test1/code19; SQLite19/JSON13/raw3/complete1 unchanged. No rolling publication,
  version bump or merge. Keep PR24 draft until the whole authorised migration is complete.

### Twenty-seventh checkpoint — common optional running routines and real support screens

- This source checkpoint follows e2fadac (inspect git log for its final commit). RunningRoutines
  Java replaced by common Kotlin; **102 Java domain files remain.** Original14 movements and13
  routines retain every ID/title/dose/instruction/tip/caution/pose, no planned workouts or actuals.
- Original adaptive coach quality/demanding classification moved to common WorkoutCategories;
  existing Java AdaptiveCoachingEngine delegates to it. Exact five quality categories, race/long-
  run distinction, null rejection and warm-up distance boundaries retained. No programming change.
- Original raw record unknowns/list references and library mutability contracts retained: global
  MOVEMENTS/routine movement lists immutable; each all() fixed-size list allows replacing its own
  element, not resizing or modifying the canonical library. A compiler out-projected-array error
  was corrected using a private list behind the same fixed-size facade; no fallback validation.
- Actual RoutineLibrary/RoutineScreen presentation now shared Compose: same content, progress,
  remembered index, Back/Next/Finish controls, scroll layout, guidance and unknown state. Android
  retains licensed assets, lifecycle/coroutine cancellation, existing completion transaction and
  related-workout ID. No common UI command creates training actuals or silently marks work done.
- **197 shared-domain JVM +14 shared-health JVM tests PASS**, zero failures/errors/skips. Six new
  common tests cover all14/13 entries/art links, exact category/distance selection, raw unknowns,
  fixed-size per-call mutation and canonical immutability. Original coaching29602/weekly96/
  Release05253 = **29951 checks PASS** against the shared classification/catalogue.
- Complete original/shared161line output cmp MATCH including full records, all12categories ×11
  race-distance values and per-call replacement semantics. SHA256
  43dcb07759b7f37d3d538c380ce153713a20daabe15449428ba1f3825d4ee1a2; originals/RunningRoutineParity.java
  and kmp-{original,shared}-running-routines.log in scratch. No output normalisation or test removal.
- Three new production-wrapper UI cases use the real SQLite completion path, explicit navigation,
  light/dark/200% text, unknown IDs and no gym-actual creation. Known movement strings in existing
  artwork/Live Coach fixtures are explicitly checked without changing assertions. Expect288
  focused Android cases after push, preserving all285 prior and default full release gates.
- New technique/routine wrappers/tests added to pinned formatting scope; formatting468scoped
  files (355Kotlin/113Java) and diff PASS. Final kmp-running-routines-final-build.log BUILD
  SUCCESSFUL117tasks/3m37s, both APKs; code19/min28/noINTERNET inspected. Native/UI runtime checks
  for this NEW head remain pending; e2fadac's green pass does not cover new routine work.
- Version0.12.3-test1/code19; SQLite19/JSON13/raw3/complete1 unchanged. No publication/merge. iOS
  host, native assets/storage/backup/retention/services and remaining domain/UI parity remain open.

### Twenty-eighth checkpoint — verified implementation and next running-execution boundary

- Latest IMPLEMENTATION31ac1eb4148af7e56a08e4a08a9566a9f6bee673 passed exact experiment43632534511
  on BOTH Android and Apple; formatting43632534522 SUCCESS. This following recovery-only commit
  may advance the branch without app-source changes; inspect its own CI before claiming it passed.
- Completed Android logs: **288 unique START/END pairs, no failures/skips**, including all3 actual
  routine SQLite completion/navigation/light/dark/200% cases. **700189 domain checks/48suites**,
  schema75/build/lint PASS. No weakened assertions, skipped original cases or blind rerun.
- Digest-verified CI result artifacts14128574397/JVM and14129560116/Apple: **197 domain+14 health
  JVM tests;184 domain+16 health Native tests**, zero failures/errors/skips. Real iosX64 domain/
  health tests and shared UI framework link executed, BUILD SUCCESSFUL12m4s. The longer Apple
  run completed successfully; its in-progress log endpoint returned404 until final logs existed.
  No Xcode-infrastructure blocker or failed Native assertion was demonstrated.
- CI APK artifact14128663983 tied to31ac1eb: ZIP SHA256
  92cda6734675e80cbefac20bd685aa05f02c101ee0287dfa74872e176dad7ebe verified. Actual APK56,840,887
  bytes/SHA25654a02bf330b64168c7bec4b5f10839abd9663125b14163c4177502ba4a74ff65; inspected
  0.12.3-test1/code19/min28/noINTERNET. Proof /workspace/scratch/routine-ci-verification.json.
  Actions test artifact only; no rolling release/version bump/merge. Full iOS APPLICATION is open.
- All three source checkpoints (2971409/e2fadac/31ac1eb) pushed; working tree clean at this verified
  implementation boundary. Entire domain.strength package is common;102 other domain Java files
  and native UI/state/data/services/app integration remain. Native image loading remains unfinished.
- Next pure-domain batch identified, NOT IMPLEMENTED: execution/WorkoutExecutionEngine +CoachCues,
  followed by coordinators as their dependencies become shared. These project accepted recorder
  metres/active time and select cues; do not change GPS acquisition/filtering/stored evidence.
- Preserve distance-over-estimated-duration precedence, latest-observation boundary interpolation,
  explicit open/skip/early actions, paused/monotonic metrics and immutable event snapshots. Original
  step-list constructor permits raw null elements; do not add silent new collection validation.
  Cue selection retains sustained evidence, grace/hysteresis, cooldown, freshness, priority/time/
  ID ordering and first-item ties. No new audio service or second authoritative engine.
- Common Duration currently lacks toMillis(). Add its original JVM method contract and Native
  exact implementation when required: Java truncates negative fractional milliseconds towards
  zero and throws on overflow (including cancellation near Long.MIN/MAX); never silently wrap
  or round away missing time. Existing seconds/nanoseconds/ISO date types stay authoritative.
- Prepared original source copies +RunningExecutionParity.java in scratch/running-execution-original
  and scratch root; kmp-original-running-execution.log196lines/SHA256
  7499eac8cd85e45b9ba6573252d61325249820b9f987bf75b6bc779100bd13f6. Includes mixed metric traces,
  pauses/explicit actions, cue arbitration and JDK duration boundary vectors. These are preparation
  only; no running-execution port or new shared Duration API exists yet. Use original LiveCoach/
  RecordingCoordination suites and targeted common tests before committing that batch.

### Twenty-ninth checkpoint — shared recorder execution and actual live-running panel

- Resumed c2d46bd on experiment/android-ios-kmp-compose; fetched main85e1c3 unchanged; PR24 OPEN
  DRAFT. Its experiment43634399391 Android/Apple and formatting43634399393 SUCCESS. Completed
  Android logs independently inspected:288 unique START/END pairs, zero failures; schema75 passed.
- Replaced seven original Java implementations with common Kotlin: WorkoutExecutionEngine,
  CoachCues, WorkoutExecutionCoordinator, RecordingCueCoordinator, RunMetricsEngine,
  RecordingTimeline, AutoPausePolicy. Entire domain.execution and domain.recording are now shared;
  95 other domain Java files remain. Original Java copies retained only under scratch for parity.
- This is an exact port, NOT another GPS correction: receipt clocks/qualification, stationary
  clusters, anchors, accepted observations, pause/gap boundaries, sensor freshness, active/elapsed
  semantics, smoothed pace, interpolation, cue grace/hysteresis/cooldown/order all retained. Raw
  nullable record fields and original mutable factory results/frozen engine snapshots preserved.
- Added exact Duration.toMillis API/Native implementation: truncates negative fractional millis
  towards zero, rejects overflow, handles Long.MIN/MAX cancellation. Added39 common/JVM tests:
  execution9, cue6, duration4+JDK oracle1, metrics13, coordinator6. Native execution pending CI.
- Production ActiveRunPanel and PaceRange now delegate to actual shared Compose implementations.
  Android retains existing authoritative service/route/Dream sources and exact number/time text.
  Shared panel never starts a sensor, clock or second recorder. Safe Finish confirmation, explicit
  step/pause/resume commands, route/stat selection and large-text behavior retained. Unknown raw
  status is honestly labelled unavailable; recorded values/targets are never fabricated.
- Fixed Kotlin-visible optional pace/elevation reads via checked local values in retained Android
  StructuredRunSection/RouteCard. No assertions or recording heuristics changed. New shared-panel
  Android tests3 added; experimental focus retains all288 old cases and adds running/recording/
  persistence flows. New Android runtime tests have NOT yet run; no local KVM.
- Local kmp-shared-running-final-build.log: BUILD SUCCESSFUL5m34s/166tasks,236 domain+14 health JVM
  tests, zero failures/errors/skips; app/debug instrumentation APKs and lint PASS. Formatting
 373Kotlin/109Java APPLY complete; final CHECK follows the next source batch. Code19/min28/no
  INTERNET inspected. Domain LiveCoach1850, RecordingCoordination74, GPS46311 checks PASS.
- Two independent original/shared JVM parity traces match byte-for-byte: execution196lines/SHA
 7499eac8cd85e45b9ba6573252d61325249820b9f987bf75b6bc779100bd13f6;
  recording1865lines/SHA61f806d2f1ff30d14bd450ca681e763e3b9c0e29d17040bffc96cf747a7d755c.
  Scratch originals/fixtures/logs remain under scratch/{running-execution-original,recording-original}.
- Next route batch PREPARED ONLY under scratch/kmp-recording-source: RoutePresentation,
  RouteRenderGeometry, common render tests8, SharedLiveRecordedRoute and Android evidence adapter.
  Not applied or verified yet. Valid original RouteParity.java fixture72lines/SHA
 9d00bc8fb34e775d249f10867d8c6f700f1cb455caa50b0b48083baf05a742d5;
  original copies scratch/route-original. Initial invalid longitude fixture was corrected BEFORE
  baseline capture, not a product failure or test weakening. Keep existing64-edge/segment/km
  bounds and no-bridge assertions. Do not recalculate or mutate stored route evidence.
- Version0.12.3-test1/code19, SQLite19/rootJSON13/raw3/complete1 unchanged. No new permissions,
  version bump, rolling publication or merge. Entire iOS APPLICATION and full migration remain open.

### Thirtieth checkpoint — shared retained route projection and production route surface

- Continues committed6ee10a0 without restarting it. Replaced original RoutePresentation and
  RouteRenderGeometry Java with common Kotlin; **93 domain Java files remain**. Original drawing
  projection, qualified kilometre interpolation/story, altitude gate, 64-edge bounded simplification,
  sharp turns, kilometre transitions and discontinuity endpoints retained. No GPS rule/storage edit.
- Actual production route surface/canvas now shared as SharedLiveRecordedRoute/PreparedLiveRoute.
  Android LiveRecordedRoute remains its unchanged lifecycle-aware saved-evidence collector, with
  IO reads/default-dispatcher preparation, cancellation and honest read-failure handling. No street
  map, fabricated coordinates, new sensor or second recording engine. Live panel, pace indicator
  and route drawing all use shared Compose; native data/service/app integration remains unfinished.
- Eight new common route tests exercise geometry, exact boundaries, unknown elevation, immutable
  results, turns/gaps and zero/positive tolerance. Original and shared72-line route trace match SHA
 9d00bc8fb34e775d249f10867d8c6f700f1cb455caa50b0b48083baf05a742d5. javap confirms original
  Java harness resolves the new Kotlin object, not stale Java classes. Retained rendering6323,
  LiveCoach1850 and GPS46311 checks passed; previous coordinator74 also passed on shared recorder.
- Local kmp-route-final-build.log: BUILD SUCCESSFUL6m47s/162tasks, **244 domain JVM tests**, zero
  failures/errors/skips, both Android APKs and lint PASS. Health14 JVM tests passed on preceding
  6ee10a0 batch; unchanged health will run again in pushed CI. Formatting CHECK485sources
  (377Kotlin/108Java), schema75, release-note10 and diff PASS. No weakened assertions/skipped tests.
- Actual local APK61,497,838bytes/SHA256
 1ce29d2488fe44bd9af83db800bfc09dee9dd2ec1ccffb1004ffebeea9b85747,
  version0.12.3-test1/code19/min28/noINTERNET inspected; scratch/kmp-route-local-apk-verification.json.
  Local debug output only, not a published release. CI APK identity will differ with runner signing.
- Experimental API35 focus is now **340 expected cases**, preserving all288 previous cases plus
  existing live-control/route, recording/sensor/staging/SQLite/backup tests and3 new shared-panel
  interaction/200% cases. Runtime and real Apple results for this NEW source are PENDING after push.
  Do not borrow c2d46bd's green pass. Expected Native domain230/health16 is not yet a test result.
- Prior source checkpoint c2d46bd actually passed43634399391 Android/Apple and43634399393 format;
  completed Android logs verified288 unique pairs/no failures. Both new source commits will be
  pushed together to avoid starting/cancelling redundant experimental CI. PR24 stays OPEN DRAFT.
- No migrations, version bump, rolling-release publication or merge. Full iOS application host,
  native typed persistence/backup/retention/assets/services and remaining domain/UI still OPEN.

### Thirty-first checkpoint — shared sample qualification, observed splits and rolling efforts

- Continued from pushed4916f1fac336d30da1bc514dc08b37afc2e65ddd/6ee10a0, preserving all prior work.
  Replaced original SampleSeries, SplitCalculator and BestEffortCalculator Java with common Kotlin.
  **90 domain Java files remain.** Existing Java callers retain their helper field/constant API;
  javap confirms SampleSeries resolves the Kotlin implementation. No duplicate Java shadow.
- Preserves the same500000-point/12m/s/30s quality gates, rejected/unknown evidence, prefix-indexed
  available sensor means, continuous segments, fixed-split elapsed semantics and both rolling-effort
  sweeps. Stationary plateaus use latest starts for efforts but first crossings for fixed splits;
  final stationary time remains in fixed splits. No PB across unsupported gaps/sparse evidence or
  materially conflicting totals. Recorded source offsets/UTC fallback retained; actuals unchanged.
- Original/shared full output matches672lines/112deterministic configurations, SHA256
 135a6f4757414e7ae3d934a3bc8aa584822fe482b28dd5d9ca5643165e652775.
  Originals/compiled oracle in scratch/activity-calculation-original; ActivityCalculationParity.java
  and kmp-{original,shared}-activity-calculation.log retained. Ten new common tests keep all assertions.
  One initial fixture was only400m (shorter than CORE1km), corrected to four valid observations over
 1km; original Java independently confirms UNCERTAIN/no automatic PB. No product rule was changed.
- Local kmp-activity-calculation-final-build.log: BUILD SUCCESSFUL1m47s/52tasks; **254 domain+14health
  JVM tests**, zero failures/errors/skips; retained Java suite compilation and actual Android Kotlin
  compilation PASS. The prepared offline classpath executed **700189checks/48suites** with -ea,
  all PASS (kmp-activity-all-legacy-domain.log). Activity138/GPS46311 included, not extra totals.
  Formatting CHECK489sources (381Kotlin/108Java) and diff PASS. No modified UI or migrations in
  this added calculation batch; both APK/lint PASS at preceding4916 route checkpoint, not rebuilt
  for this newer local source yet. Exact-head Android build/runtime and Native remain pending.
- Actual4916 Apple job132582788108 in experiment43639234658 SUCCESS7m55s: real iosX64 tests and
  shared UI framework link. Artifact14131781097 digest
 36790f83cfe959a090ae4e9cd7a76817b63b7c571e1675ea6ad5f5fe149552ac independently verified:
  **230domain+16health Native tests**, zero failures/errors/skips. Proof scratch/running-apple-
  verification.json. This verifies the nine recorder/execution/route ports, NOT the following
  three calculation classes (expected Native240domain is still pending).
- First4916 Android job132582788395 failed BEFORE compilation: pinned Maven Central Kotlin2.1.21
  POM requests returnedHTTP403. All three exact POMs independently returnedHTTP200 afterwards.
  No dependency/mirror/security/test change. GitHub rejected retry while parentApple was running;
  once it finished ONE targeted job retry was authorised/executed via connected GitHub tool.
  Attempt2 Android132586151728 is running; actual dependency/domain/schema steps passed and it
  reached Android build. Prior failure was investigated, not blindly rerun. Check actual result.
  Expected340runtime cases are still NOT a pass. Formatting43639234684 SUCCESS for4916.
- Hold the next source push until the running4916 Android attempt completes, so it is not needlessly
  cancelled. If it reveals a genuine integration failure, fix it with this next coherent source
  checkpoint and reverify. PR24 remains OPEN DRAFT; version/formats/publication/merge unchanged.

### Thirty-second checkpoint — shared comparisons, durability, history and actual comparison UI

- Pushed b787077f65671c0f7a976cb6d09099e6a09b4320; experiment43700834215 Android/Apple and
  formatting43700834175 SUCCESS. Completed Android logs confirm340unique START/END pairs with
  no failures/skips, original700189checks/48suites, schema75/build/lint. Digest-verified artifacts
  confirm254domain+14health JVM and240domain+16health Native, zero failures/errors/skips. Real
  iosX64 tests/shared UI framework link4m36s; no iOS APPLICATION exists yet.
- b787 artifact14156731462 ZIP6a48d8e22b67634522843730c07eef79e4f024bedd877e1c9c35b52fa4b6c578
  verified; actual CI APK56,890,039bytes/SHA256
  c0b27f6f3cfb3abbffc0f13327721212dcf5d23ce447189050869fce97646b13, version/code/min28/noINTERNET
  inspected. JVM14156611728 and Apple14156466669 test ZIP digests checked before XML counts.
  Proof scratch/calculation-b787-verified/verification.json. Actions artifact only, no publication.
- Current source replaces original ComparableRuns, LongRunDurability and WorkoutHistoryQuery Java
  with common Kotlin; **87 Java domain files remain**. Preserves ranking/ties, purpose/distance/
  duration/elevation/age gates, optional effort descriptions, equal-time halves and80%sensor
  coverage, recorded offsets/UTC fallback, history range/type/exercise-alias filtering and bounded
  pages. Raw nullable record fields and original mutable aliases remain supported. No new score,
  medical inference, fabricated completion, physiological precision or history mutation.
- Original/shared988-line output matches SHA256
  deebbd3a6613aa3ff6d0d15b7109d7b86ba996e02328c573ccdcb9e9bb1b7671:
 720history pages,12matching histories/240interpretations,12durability windows and4raw records.
  Originals/oracle in scratch/activity-insights-original and ActivityInsightsParity.java.
- Actual production RunComparisonSection delegates to SharedRunComparisonSection. Android retains
  plan/profile/date/unit sources; original collapse, manual choice, controls/disclosure/wording and
  metrics are shared. ActivityScreens uses a nullable evidence binding for optional pace fade,
  never a fabricated zero. Activity Detail really calls this wrapper (not a test-only component).
- Added11common contracts (coverage, ranking, dates, aliases, raw records) and3actual Compose
  interaction/dark/200% tests. New Android cases are compiled, NOT yet executed. Experimental
  focus retains all340 previous cases and adds3 (343expected, not yet a result).
- Local kmp-activity-insights-verified-build.log: BUILD SUCCESSFUL5m55s/166tasks; **265domain+
 14health JVM tests**, zero failures/errors/skips; both Android APKs and lint PASS. Original
 700189checks/48suites executed with -ea against prepared new Kotlin classes, all PASS
  (kmp-insights-all-legacy-domain.log). Formatting CHECK496sources (388Kotlin/108Java), diff PASS.
  New Native251domain/16health and Android343 results remain pending after push; b787 pass does
  not cover this new source. Existing required final unfiltered gates unchanged.
- Intermediate compile optional typing and canonical-exercise test fixture nullability were fixed
  at their actual call sites; no assertion removed/weakened, no arbitrary delay or blind rerun.
  Latest source has no known local failures. Branch/PR24 OPEN DRAFT and full migration incomplete.
  Version0.12.3-test1/code19; SQLite19/rootJSON13/raw3/complete1 unchanged, no new permissions or
  dependencies, no merge/publication/version bump. PR23 untouched. Real-device acceptance unrun.

### Thirty-third checkpoint — shared retained milestones and actual durability summary card

- Verified c651baddd19e5584aa94b63c93ee7cbd6d89a178 passed experiment43703461026 Android/Apple
  and formatting43703460995. Completed logs confirm **343unique Android runtime cases**, no
  failures/skips, including all3 real shared-comparison/manual-choice/200% tests. Digests verified
  for test archives14158435179/JVM and14157304729/Apple: **265domain+14health JVM;251domain+
 16health Native**, zero failures/errors/skips. Real iosX64/framework link6m48s. No iOS app yet.
- c651 CI APK artifact14157826263 ZIP48ec6821dce4378cb9fe3fec83175166de23022a6831438dfba5f10899803e40
  verified; actual APK56,906,423bytes/SHA256
 4df22f1155857c237815939d783173a1765827c26f9514fcab74082f925e459e, code19/min28/noINTERNET
  inspected. Proof scratch/insights-c651-verified/verification.json. No rolling publication.
- New current source replaces MilestoneEngine and DurabilityData Java with common Kotlin;
  **85domain Java files remain**. No new achievements or calculations: retained firsts, strict
  time thresholds, original source/input/result tie rules, recorded offsets and no lifetime-first
  claims preserved. Summary requires the same qualified detailed evidence; totals alone never
  become half-run paces or sensor context. Raw nullable summary fields remain supported.
- Actual Activity Detail durability card is now SharedRunDurabilityCard, reused in production
  with unchanged shape/text/disclosure and Android unit/number callbacks. No record/sensor/clock
  ownership moved to UI. Existing screenshot fixture still reloads actual stored activity and
  asserts sufficient durability; explicit requireNotNull bindings preserve/strengthen its checks,
  not a skipped assertion or manufactured capture. Final source screenshots remain a final gate.
- Original/shared30-line milestone/summary trace matches SHA256
 0669b23349df49d108f45168783a417a99bfe966155927dd2caf94b722d290b5. Original copies/compiled
  oracle scratch/running-summary-original, RunningSummaryParity.java and kmp-{original,shared}-
  running-summary.log. Added12common contracts and3actual-card UI tests; new UI tests compiled
  but NOT yet run locally (no KVM). Focus preserves all343 prior cases and adds3 (346expected).
- Local kmp-running-summary-build.log BUILD SUCCESSFUL7m6s/166tasks: **277domain+14health JVM
  tests**, zero failures/errors/skips, both Android APKs and lint PASS. Original **700189checks/
 48suites** executed with -ea against new Kotlin classes, all PASS; Activity138/Refinement97
  included, not additional totals. Formatting CHECK501sources (393Kotlin/108Java), diff PASS.
- New exact-head Android346 and Native263domain/16health results are PENDING after push, not
  inferred from c651. No known local failures. Version0.12.3-test1/code19; SQLite19/rootJSON13/
  raw3/complete1 unchanged; no permissions/dependencies/version bump/publication/merge. PR24 DRAFT.

### Thirty-fourth checkpoint — shared weekly analytics, baseline coaching and repository seams

- Recovered and verified 5fb47fd425d6f2f4b78ee541abaebc38886f0bba: experiment43705545478 and
  formatting43705545475 SUCCESS. Completed Android log132785713956 has **346unique runtime
  START/END cases**, including all3 durability-card cases, no failures/skips. Apple132785713710
  actually built/tested iosX64 and linked shared UI; this is a framework, not an iOS app.
  Digest-verified archives14158378502/Apple and14158249736/JVM confirm **263domain+16health
  Native;277domain+14health JVM**, zero failures/errors/skips. CI APK14158319533 ZIP digest
 886e2f3a21611818e794a8743c1e2d3dc1826477701e0e249be995d0773248ae; actual56,906,423bytes/
 SHA25621cf234f4538b4f82b842ed9e3d9aa0ed07b82bcc23bba1f2bb9fd01cd02c726,
  code19/min28/noINTERNET inspected. Proof scratch/summary-5fb-verified/verification.json.
- Recovered weekly port and continued a coherent batch: **18superseded Java domain files
  replaced;67remain**. Entire baseline package, RunnerDataRepository compatibility aggregate,
  SettingsRepository and PlannedWorkoutRepository are common Kotlin. Actual Android storage/
  shared SQLite transaction and atomic activity+assessment save remain in existing adapters.
- RecordedWeekAggregation is the single common weekly implementation. JVM ActivityAnalytics
  preserves its Java-callable ZoneId API and real display-zone/DST projection; native callers
  supply platform projection. Never substitute stored offsets or UTC for the display timezone.
  BaselineEngine deliberately retains its DIFFERENT original per-run recorded-offset/UTC rule.
- Baseline/assessment code preserves source deduplication, exact cross-provider mirrors without
  deleting originals, observed complete weeks, unknown distances, recency/coverage confidence,
  explicit result provenance, manual corrections/review timestamps, fractional assessment times,
  controlled-vs-best-effort interpretation and mixed-evidence earlier expiry. No training rules,
  prescriptions, measurements or historical records changed. Raw nullable DTO fields/entries,
  snapshot ownership and older Java constructors/accessors remain supported.
- Original/shared output identical: weekly261lines/SHA256
 b2994b96c66e2587cd1b46539d786306f9e3283a8d42c409f84034fc0bbd44f2;
  input boundaries122lines/8c0a6cfbabfa2d75632186df7c4c884aa4ef6d32c18982bdc25b7e6370795473;
  baseline/overlays/assessment/eligibility1078lines/
 c4f968beec51aa921f12b7738a158c571d5d9b0d25c9dc6ced5db59f13cb8582.
  Fixtures/original copies in scratch/activity-weeks-original, baseline-boundary-original,
  baseline-engine-original, ActivityWeeksParity.java, BaselineBoundaryParity.java and
  BaselineEngineParity.java. javap confirms actual Kotlin classes, not stale Java test copies.
- Production BaselineScreen now calls SharedBaselineSummaryCard, keeping original layout,
  disclosures, labels and platform number/unit/UTC-date formatting. Added3actual-card light/
  dark/manual/200% interaction tests; focus retains all346 previous cases (349expected after
  push, not yet a result). No Health Connect permissions or UI navigation changes. Assessment
  summary renders confidence only when present; it never substitutes an invented level.
- Confirmed Java iterator.remove mutation escape in the new baseline map and existing shared
  immutable-map helper. Replaced both exposed mutable iterators with read-only iterators,
  strengthened existing tests, and independently verified Java now rejects both operations.
  No assertion removed/weakened. Tests retain raw null-map support where original DTOs allowed it.
- Current local **312domain+14health JVM tests PASS**, zero failures/errors/skips; common30+
  JVM5new contracts cover timezone/DST, unknowns, assessment boundaries, confidence, evidence
  expiry, provenance and snapshots. Original **700189checks/48suites passed after the iterator fix** with -ea against the
  prepared Kotlin classes. Final post-fix build **BUILD SUCCESSFUL6m47s/166tasks**: Android
  debug APK, instrumentation APK and lint PASS.
  Formatting CHECK523sources (419Kotlin/104Java), diff PASS; schema75 and release-note10 PASS.
  Logs scratch/kmp-baseline-verified-{build,legacy,format}.log. Final local APK inspected for
  code19/min28/noINTERNET and new shared classes (scratch/baseline-local-apk-verification.json);
  local debug build only, not a rolling publication. New Native293domain+16health and Android349
  remain pending exact-head CI after push. No canonical screenshot capture yet for this batch.
- Corrected actual Kotlin integration errors rather than weakening validation: baseline repository
  returns its existing all-unknown ManualBaselineInput.empty when absent, with nullable timestamp;
  raw AssessmentSummary confidence remains optional at its actual display. No known JVM failures.
  PR24 stays DRAFT; main85e1c39/PR23 untouched. Version0.12.3-test1/code19 and formats19/13/3/1
  unchanged; no new permission/dependency/version, merge or publication. Migration still incomplete.

### Thirty-fifth checkpoint — running prescription snapshots, execution feedback and CI repair

- Inspected 066104d exact-head experiment43709777067: Apple132798603267 SUCCESS, real iosX64
  tests/framework build8m37s. Digest-verified JVM14160671230 and Apple14159449402 archives:
  **312domain+14health JVM;293domain+16health Native**, zero failures/errors/skips. Proof
  scratch/baseline-066-verified/verification.json. Formatting43709777026 passed.
- Android132798603437 built/linted successfully, then failed TWO new baseline-card assertions:
  retained time formatter displays0:25:00, not25:00; expanded WhyDetails is labelled Hide details,
  not its original About confidence title. Corrected exact expectations and ADDED expanded/
  collapsed semantics assertions. Production formatting/disclosure behaviour unchanged. No blind
  rerun, arbitrary delay, removed assertion or manufactured Android pass. New head must verify fixes.
- Current batch replaces9Java files with common Kotlin: PlanRequest, PlanVersion, RunExecution,
  RunFeedback, AdjustmentProposal, WorkoutDecision, PlanPreservation, TrainingLoadEvidence and
  RunScoreEngine. **58Java domain files remain**. Preserves old constructors/accessors/raw nullable
  fields, immutable collections, 500-step/1000-repetition bounds, explicit revision linkage,
  keep-original choice, completed-prescription protection, run-END vs strength-START windows,
  unknown effort and exact original score/rule3 calculations. No prescription becomes an actual.
- Original/shared **821line trace matches** SHA256
 a450b6464ef39af0c4b8da0365988a4217a3029b54b99e91be1d4c703f6133f2.
  Original copies/compiled oracle scratch/coaching-contracts-original and CoachingContractsParity.java;
  output kmp-{original,shared}-coaching-contracts.log. Includes categories, recorded effort,
  totals, samples, structured reliability and validation boundaries. Actual Kotlin implementation
  is used by retained Java tests; no duplicate old implementation shipped.
- Production RunFeedbackSection now renders SharedRunFeedbackCard. Android still loads preserved
  plan/actual/response and owns proposal actions. Existing wording/layout/disclosures retained;
  raw missing text omitted, never invented. StructuredRunSection binds its already-qualified
  optional pace endpoints explicitly; never replaces unknown pace with zero. Added3actual-card
  light/dark/200% tests. Focus retains all349 prior cases and adds3 (352EXPECTED, not yet run).
- **329domain+14health JVM tests PASS**, zero failures/errors/skips, including17new common
  snapshot/execution/score contracts. Original **700189checks/48suites PASS** with -ea. Final
  production build kmp-coaching-verified-build.log **BUILD SUCCESSFUL6m9s/166tasks**: both Android
  APKs/lint PASS. Strengthened test-only snapshot assertions then reran JVM tests: PASS40s.
  Formatting CHECK534sources (430Kotlin/104Java), diff PASS. Existing schema/backup gates retained
  in CI; no schema/backup edits. Local APK code19/min28/noINTERNET/new shared feedback classes
  verified (scratch/coaching-local-apk-verification.json); not publicly published.
- New exact-head Native310domain+16health and Android352 are pending after push. Latest old
  Android failure is explained above, not claimed fixed until runtime verifies. No known local
  failures. Version0.12.3-test1/code19; formats19/13/3/1 unchanged. PR24 remains DRAFT, no merge,
  version bump or publication; main85e1c39/PR23 untouched. Real iOS app is still incomplete.
- Next running-generator oracle already captured BEFORE replacement: scratch/running-generator-
  original, RunningGeneratorParity.java and kmp-original-running-generator.log: **422lines**/
 17863e2983260d6149776e0094755fbb379828d311843faadabcff386b15125b.
  scratch/ConstraintScheduler.kt is a prepared port, NOT yet applied/compiled. Preserve this prep.

### Thirty-sixth checkpoint — original running generator and constraint scheduler shared

- 311f72422a4bf458d316731ba8181fa6b3019026 passed experiment43715513040 and
  formatting43715512958. Completed Android132816068189 logs confirm **352unique START/END
  runtime cases**, none missing/failed/skipped: all3 baseline tests now pass, including both
  repaired assertions, and all3 new feedback-card tests actually execute. Apple132816068411
  tests/framework build SUCCESS6m30s. Framework success is still not an iOS app build.
- Digest-verified JVM14162048474 and Apple14161919269 archives confirm **329domain+14health
  JVM;310domain+16health Native**, all zero failures/errors/skips. APK14162542930 ZIP SHA256
 59cc0b10727839b0a3a518f30231f9e48948efd903a351ed234f28528bdadaf5;
  actual56,971,959bytes/SHA2560910a8b77500e4a3d7a725a24a446705d9fb84bdd7db43431988e2292e339eea,
  code19/min28/noINTERNET verified. Proof scratch/coaching-311-verified/verification.json.
- Continued without stopping at CI repair: replaces ConstraintScheduler and
  OriginalTrainingPlanGenerator Java with the actual common Kotlin implementation used by
  Android. **56Java domain files remain**. Same hard availability, exhaustive soft scoring/ties,
  prior-week spacing, actual lower-body context, frequencies, workload/long-run caps, conservative
  run/walk defaults, taper/race protection, distant phases, time preferences and distribution.
  No training rules or calculations intentionally changed; no source evidence rewritten.
- Reuses existing exact Java-compatible UUID helper by changing ONLY nameUuid visibility in
  StrengthProgrammeEditor from private to internal. Strength programming logic unchanged;
  original UTF-8, MD5 UUIDv3 bits, plan IDs, prescription IDs and rounding preserved. No new
  dependency, speculative clock/calendar service or parallel generator.
- Original/shared **422line trace matches** SHA256
 17863e2983260d6149776e0094755fbb379828d311843faadabcff386b15125b:
 168full plans across race/goal/strategy/frequency/evidence/distribution/horizon and254scheduler
  combinations. Original copies/compiled fixture scratch/running-generator-original and
  RunningGeneratorParity.java; shared output kmp-shared-running-generator.log. javap confirms
  OriginalTrainingPlanGenerator.kt, including original Java-callable static phaseAt API.
- Added12common tests for legacy UUID/horizon, missing/stale evidence, all start weekdays,
  race protection/frequency, phase partition, distant phases, time caps/distribution, demanding
  spacing, all127nonempty weekday subsets/both quality options, soft strength conflicts and
  factory-vs-raw Schedule snapshot semantics. **341domain+14health JVM PASS**, zero failures/
  errors/skips. Original **700189checks/48suites PASS** with assertions against prepared Kotlin.
- Focused compile kmp-running-generator-build.log passed3m8s. Final
  kmp-running-generator-verified-build.log **BUILD SUCCESSFUL4m55s/166tasks**: both APKs and lint
  PASS. Actual local APK/code19/min28/noINTERNET/new shared generator classes inspected
  (scratch/running-generator-local-apk-verification.json); local build only, not publication. Formatting CHECK537sources
  (433Kotlin/104Java), diff PASS. New exact-head Native322domain+16health and Android352 remain
  pending after push; 311's green run must not be borrowed for this new production source.
  No canonical final-source capture yet. No known local test/compile failures.
- Version0.12.3-test1/code19; formats19/13/3/1 unchanged. No permissions/dependencies, version
  increment, merge or public publication. PR24 remains DRAFT; main85e1c39/PR23 untouched.
  Actual shared root UI/navigation/state, native persistence/services/HealthKit and iOS app host
  are still unfinished. Physical acceptance remains unperformed.

### Thirty-seventh checkpoint — weekly/adaptive coaching and production review workflow

- 41971581afd54e92769a758327fdba51c8e27bd0 passed experiment43727718762 and formatting
 43727718724. Completed Android132853184965 log: **352unique START/END cases**, zero failures/
  skips/missing completions. Apple132853184678 actually built/tested iosX64/framework7m15s.
  Digest-verified archives14167337576/JVM and14167875345/Apple: **341domain+14health JVM;
 322domain+16health Native**, zero failures/errors/skips. Proof scratch/generator-419-verified/
  verification.json. Framework linking is not an iOS application build.
- CI APK14167945108 ZIPceac1fe7e21221f7c5251d44150743fd86b0fe85a88e34fd920ecb738854f54a
  verified; actual56,971,959bytes/SHA256
 627bf20bd6f92d9f36465ad6440c8ba09777e557d63587a79e455eea0b2b2ded,
  code19/min28/noINTERNET inspected. Actions artifact only, no rolling publication.
- Replaced all5remaining coaching Java files and WeeklyReviewData with Kotlin. **No Java remains
  in domain.coaching;50Java domain files remain overall.** One common AdaptiveCoaching engine;
  JVM AdaptiveCoachingEngine is ONLY the original Optional facade. Common CoachingRepository
  uses expect/actual CoachingTransaction: JVM typealiases Runnable; iOS has its own interface.
  javap and JVM ABI test confirm original transaction(java.lang.Runnable). Android CoachingStore
  retains actual shared SQLite transaction ownership. Native storage is still not implemented.
- Preserves review windows, earliest prescriptions, moved/completed/skipped evidence, raw-null
  DTOs, enum ordering, fatigue/strength/durability context, source authority, six CALENDAR month
  lookback, keep/preview/accept safeguards, completed protection, pace bounds and UUID identity.
  No training rule redesigned. Calendar clamping, ordered compensated DoubleStream sums and
  native one-decimal labels are checked against JDK references; JVM formatter unchanged.
- Original/shared **771line trace identical**, SHA256
 80999dd339c7e55a4f031fde57dbf7f73bdab72b6b6f882c7a604779e937c2e9.
  Fixtures/original compiled copies: scratch/coaching-review-original, CoachingReviewParity.java,
  kmp-{original,shared}-coaching-review.log. Covers72histories/choices/apply/briefings/proposals,
  performance thresholds and3explicit hard-lower-body cases. Earlier768line trace matched too;
  enlarged fixture now genuinely meets retained >=3working-set hard-lower criterion.
- Actual WeeklyReviewScreen delegates review/disclosures/preview/accept/cancel to shared Compose
  SharedWeeklyReviewContent. Android keeps formatting, current plan, stored decision, coordinator
  action and break form. PlanScreens explicitly binds its known factory briefing. Added3actual
  light/dark/200% UI tests; existing352 retained (355EXPECTED, not run locally).
- **362domain+14health JVM PASS**, zero failures/errors/skips;18new common and3JVM contracts.
  **700189checks/48retained suites PASS** with -ea against prepared Kotlin. CHECK550sources
  (446Kotlin/104Java), diff PASS. Initial2new tests mistakenly used1lower set; fixture corrected
  to3, rule/assertions unchanged. Both then passed TARGETED rerun53s before full362test run.
- kmp-review-engines-build.log passed1m22s. Full kmp-review-final-build.log passed JVM tests and
  app compilation/APK, then instrumentation compile found old LiveCoachStoreTest assuming
  current() non-null. Strengthened that backup test with requireNotNull, preserving its exact
  original-prescription assertion. Affected Android rebuild/lint **PASS2m12s/157tasks** in
  scratch/kmp-review-android-verified.log: debug APK, instrumentation APK and lint all PASS.
  Local APK/version/code/min28/noINTERNET/new shared classes verified (scratch/coaching-review-
  local-apk-verification.json); private local build only. No known local failures.
  New Native340domain+16health and Android355 need OWN exact-head CI after push. Canonical
  final-source captures remain a final migration gate, not completed here.
- Version0.12.3-test1/code19 and formats19/13/3/1 unchanged. No permission/dependency/version bump,
  merge or public publication. PR24 DRAFT; main85e1c39/PR23 untouched. Shared root UI/state,
  native persistence/services/full HealthKit and real iOS host remain incomplete. Hardware unrun.

### Thirty-eighth checkpoint — Home, Strength Today, calendar and weight projections

- Recovered clean e52af3907342ccd247644c545120086412597690 on PR24; remote main85e1c39 and
  separate PR23 unchanged. e52 experiment43731038795 and formatting43731038835 SUCCESS.
  Android132863298696 completed **355unique START/END cases**, zero missing completions/failures/
  skips. Digest-verified JVM14168777856 and Apple14169017056 reports: **362domain+14health JVM;
  340domain+16health Native**, zero failures/errors/skips. Proof scratch/review-e52-verified/
  verification.json. Real Apple tests/framework pass is NOT an iOS application build.
- e52 APK14168747924 ZIP6228730fc7559f4edc006eaa07a2af71cdbaeb84703e7a27c61f903db9377ba1;
  extracted56,988,284bytes/SHA2561265c1956c98efc97f0e9f88febc747da98a30335f6a6749e17a5fb8a497597f,
  code19/min28/noINTERNET verified. Actions artifact only, not public publication.
- This batch ports HomeCoachingSummary, StrengthTodaySummary, CalendarData,
  WeightGraphData and AthleteLanguage to common Kotlin and removes their5Java originals.
  **45Java domain files remain**. Preserves raw nullable DTO/alias contracts, actual-vs-planned
  completion, explicit retained revision associations, recorded offsets, PB recency/estimate
  exclusions, conservative existing progression, calendar states and genuine chart measurements.
  JVM raw constructor/accessor APIs retained; graph epoch-millisecond floor/overflow matches JDK.
- Production StrengthTodayScreen and Home coaching card now delegate to actual shared Compose
  content. Android retains clock/date formatting/prepared state/URI encoding/navigation. No layout,
  target, history, permission, health-read or recording engine redesign. Nullable raw model fields
  are explicitly bound at their existing qualified factory/callback boundaries, never zero-filled.
- **379domain+14health JVM tests PASS**, zero failures/errors/skips;14new common and3JVM
  presentation contracts. **700189checks/48retained suites PASS** with assertions against actual
  Kotlin classes (javap verifies no stale Java shadows). Original/shared **2671line trace matches**,
  SHA2566bb4ba9a7a79e0700cd67830064ed501fb58ddc31ebca3560c74db9be764abd9.
  Originals/oracle: scratch/presentation-original, PresentationParity.java,
  kmp-{original,shared}-presentation.log. Oracle caught and corrected one truncated time-trial
  sentence; exact original wording restored. No test assertion weakened.
- Added4actual shared Home/Strength Today Android UI tests: standalone use, explicit navigation/
  raw session identity, actual completion and 200% scrolling. All355prior focused cases retained;
  **359EXPECTED Android /354domain+16health Native**, not yet verified on this source.
- Formatting CHECK557sources and diff PASS. Final affected build/lint **PASS7m40s/166tasks** in
  scratch/kmp-presentation-verified-build.log: both Android APKs and lint PASS. First compile
  identified real nullable integration errors; fixed qualified consumers, no invented fallbacks.
  Actual local APK/code19/min28/noINTERNET/shared classes verified (scratch/presentation-local-
  apk-verification.json); private local build only. No canonical screenshots yet.
- Next health-context original oracle already captured without editing those production sources:
  scratch/health-context-original, HealthContextParity.java, kmp-original-health-context.log
  (1271lines). Draft Kotlin files in scratch/health-context-ports are PREPARATION ONLY, not applied
  or compiled. Preserve, inspect and finish them rather than assuming they are verified.
- Version0.12.3-test1/code19; formats19/13/3/1 unchanged. PR24 stays DRAFT; no merge, version bump
  or public publication. Shared application root/native storage/services/full HealthKit/real iOS
  host remain unfinished. Physical acceptance unperformed.

### Thirty-ninth checkpoint — source-specific health context and import safety

- Preserved/pushed presentation checkpoint **85a6d0d9fc7ba779807d3c2e8b4111f8237cbc68**.
  Formatting43742108377 PASS. Experiment43742108251 Android132897315132 FAILED BEFORE tests:
  Maven Central returned HTTP429 for Kotlin2.1.21 plugin/BOM/Compose compiler POMs during root
  configuration. Actual logs inspected; no test or compilation assertion failed. Apple132897315253
  then PASSED actual iosX64 tests/framework9m3s. Digest-verified Apple14173586423 reports
  **354domain+16health Native**, zero failures/errors/skips; proof scratch/presentation-85a-verified/
  verification.json. A single Android retry request was rejected403 while the run was active;
  it did NOT rerun. New health source will run all retained Android cases, so do not launch an
  obsolete-parent retry immediately before pushing it.85a has NO verified Android runtime pass.
- This batch ports HealthIntelligence, HealthReadPolicy, RecoveryCoaching,
  HealthImportPolicy and HealthContextSummary; their5Java originals removed. **40Java domain
  files remain; no Java remains in domain.health.** Typed health repository/Android SDK transaction
  ownership remains unchanged. No additional permission, background read or deletion authority.
- Preserves latest-source authority, one comparable observation/day, longest sleep observation,
  seven-day versus preceding28-day windows,4/14 measured-day gates, exact compensated averages,
  corroborated recovery rules, missing/stale evidence and race-goal protection. Import snapshots
  remain permission/caller facts; partial/visible-only HealthKit data cannot imply deletion.
  Deletions retain wholly-contained window boundaries, other providers, opaque tuple identity,
  upsert precedence and single atomic applyImport. JVM constructors/accessors/text retained.
- Existing health trend and recovery preview/keep/accept cards now shared Compose. Android keeps
  permission launchers, provider reads, prepared state, formatting and actual plan actions. No
  recommendation is automatically activated. Existing rounded cards/disclosures/copy retained.
  JVM health number formatting unchanged; Native canonical decimal/grouping verified against
  the UK formatter on8001values plus extremes/nonfinite/negative-zero cases.
- **395domain+14health JVM tests PASS**, zero failures/errors/skips;14new common and2JVM
  contracts. **700189checks/48retained suites PASS** with assertions against actual shared Kotlin
  (javap confirms HealthImportPolicy.kt/HealthIntelligence.kt, no old Java shadow).
  Original/shared **1280line trace identical**, SHA256
 bde8c16113a7d3ba86ff7a60796ce1d340912a356958fdafdbb2fc0cb2f24bc6.
  Originals/fixtures: scratch/health-context-original, HealthContextParity.java,
  kmp-{original,shared}-health-context.log. Initial1271line trace also matched before adding
 9explicit import cases; retain the enlarged1280line oracle.
- Added3actual shared health/recovery UI tests for unknown data/disclosure, keeping original,
  and explicit acceptance at200% text. Existing359 focused cases retained. Own new source expects
  **362Android /368domain+16health Native**, not yet run. Formatting CHECK568sources and diff PASS.
- Initial health core compile PASS1m24s. Full build passed all JVM tests then found ONE real
  Kotlin integration error: cannot star-import nested types from a Kotlin object. Replaced ONLY
  HealthIntelligenceGateway's wildcard with explicit Metric/Observation imports; provider logic
  unchanged. Affected Android APK/instrumentation/lint rebuild **PASS4m49s/157tasks** in
  scratch/kmp-health-context-android-verified.log. Both APKs/lint PASS; final formatting568/diff PASS.
  Actual local APK/code19/min28/noINTERNET/new shared health classes verified (scratch/health-
  context-local-apk-verification.json); private local artifact only. No canonical captures yet.
- Next Dream batch originals compiled/preserved without production edits:
  scratch/dream-original, DreamParity.java, kmp-original-dream.log **72415lines**/SHA256
 539826da12212bfbe852c1e1fc234906680296f82ae3a7dac293dffac5af634d.
  Covers16geometry revisions, exact vertex-bit hashes, clipped sections, all6real basemap assets,
  licences/attribution/landmarks/rivers and500seeded assignments/progress descriptions. No Dream
  code has been ported in this batch. Preserve genuine route evidence and safe bounded parser.
- Version0.12.3-test1/code19; formats19/13/3/1 unchanged. PR24 DRAFT; main85e1c39/PR23 untouched.
  No merge, version increment, public publication or physical-device acceptance claimed.

### Fortieth checkpoint — race predictions and preserved training-choice contracts

- Health checkpoint **ecc2aba6949451025f97c641edef6d18e0c8fa13** pushed. Formatting43743599734
  passed; experiment43743599758 Apple132901917819 SUCCESS. Digest-verified Apple14173589121:
  **368domain+16health Native**, zero failures/errors/skips, proof scratch/health-ecc-apple-verified/
  verification.json. Framework linking remains NOT a real iOS app. Android132901917892 then PASSED **362unique START/END runtime cases**, zero failures/skips/
  missing completions, including ALL7new Home/Strength Today/health/recovery cases. Experiment
 43743599758 SUCCESS; earlier85a Maven429 did not recur. Digest-verified JVM14172919484 confirms
  **395domain+14health**, all zero failures/errors/skips; full proof scratch/health-ecc-verified/
  verification.json. APK14172974642 ZIP1956510632da0fe5aa37dc6b197cb9bc94f52a3a4a25ad0dbf7aed68b9c63cb4;
  extracted57,020,993bytes/SHA256c13ed8ffb7c4c8ceb8ba377a74bf3d02ff67d23dff5d1a9cf693f7bb6a375cba,
  code19/min28/noINTERNET verified. Actions artifact only, no public publication.
- This batch replaces8Java files: CompletedWorkout, GymSession, PlanAdjustment,
  RacePrediction, RaceReadiness, RecoveryMetrics, RacePredictionEngine and RaceStrategyEngine.
  **32Java domain files remain; no Java remains in domain.race.** Kotlin constructors/accessors,
  raw optional observations, immutable evidence, valid choices/selections and kept-original
  option remain intact. RaceReadiness retains its old INTERNAL score contract, not a new health
  readiness score/probability. No new training metric or recommendation rule was introduced.
- Original race scaling/ageing/source independence/120day lookback/28day actual endurance gates,
  conflict widening and minute-rounded ranges are preserved. Target pacing remains separate
  from estimates; hills/missing targets retain effort-only guidance. Java default-locale source
  casing is preserved by a tiny JVM helper; native UI uses English source labels. Existing UK
  decimal helper reused; no new numerical formatter or prediction implementation duplicated.
- Android RaceScreen binds already-qualified optional range/pace values explicitly and retains
  its actual appearance/navigation. No UI redesign or fabricated substitute values. All original
  eight source files/compiled oracle retained outside repository in scratch/race-contracts-original;
  fixture RaceContractsParity.java. **855original/shared lines exactly match**, SHA256
 c8ae7df49d3970c56ad8ce15d215b7612a1dc56ee7e05e1da10562ff4a85b037.
  Includes120evidence scenarios/sixdistances, target/hill strategies and retained DTO/choice cases.
- **409domain+14health JVM tests PASS**, zero failures/errors/skips;12new common and2JVM
  contracts. **700189checks/48retained suites PASS** with assertions against actual Kotlin;
  javap confirms Kotlin classes, not stale Java shadows. Formatting CHECK580sources/diff PASS.
  Native380domain+16health and Android362 are EXPECTATIONS for this source, not verified yet.
- Core compile PASS1m23s. Initial scratch generator had missing whitespace before getter '='
  after generic '>'; corrected syntax before successful compile. Original fixture preparation
  used enum values() on the actual PerformanceDistance VALUE TYPE and missing rolling source
  links; corrected fixtures to real CORE distances and valid activity IDs BEFORE capturing
  the original855line oracle. No production rule or existing test assertion changed.
- Final affected build/lint **PASS7m13s/166tasks** in
  scratch/kmp-race-contracts-verified-build.log: both Android APKs and lint PASS. Actual local APK/
  code19/min28/noINTERNET/shared race classes inspected (scratch/race-contracts-local-apk-
  verification.json); private incremental artifact only, not public publication.
  Final source canonical screenshots/full platform gate still pending at migration readiness.
- Dream originals/72415line oracle from checkpoint39 remain preserved and UNAPPLIED. Safely
  complete the coupled geometry/cartography/progress port plus appropriate bounded stream/parser
  boundary in the next batch; don't recreate or discard known original vertices/licences.
- Version0.12.3-test1/code19; formats19/13/3/1 unchanged. PR24 remains DRAFT, main85e1c39 and
  owner PR23 untouched. No merge/publication/version increment/physical acceptance claimed.

### Forty-first checkpoint — retained Dream geometry and cross-platform equivalent progress

- Preserved/pushed race checkpoint **e679887be25f48e20d1f23b0b2f63feb27086063**. Experiment
 43745501715 and formatting43745501719 SUCCESS. Android132907782499: **362unique START/END
  cases**, zero failures/skips/missing completions. Digest-verified JVM14174074186 and
  Apple14174711315: **409domain+14health JVM /380domain+16health Native**, all zero failures/
  errors/skips. Proof scratch/race-e679-verified/verification.json. Framework is NOT an iOS app.
- CI APK14174164084 ZIP3427598bdf2b937a3f8b5ccae748302f473dc175dc9997d96fade8ea95bda39c;
  extracted57,020,993bytes/SHA256831db756141fba2112be2d73aed82fa6123113df81bf4e74851de4f1323d9fd4,
  code19/min28/noINTERNET verified. Actions artifact only, not public publication.
- This batch ports DreamRoutes, DreamCartography,6geographic data wrappers and
  DreamRouteProgress;9Java originals removed. **23Java domain files remain; presentation has
  no Java left.** DreamBasemap deliberately remains the original bounded JVM InputStream reader;
  native stream/parser/storage support is still unfinished, NOT claimed cross-platform complete.
- Keeps all16historical geometry identities,5stable city-preference IDs, every original projected
  vertex/landmark/river, approximate-versus-official limitations and MIT/CC0/ODbL attribution.
  Original immutable registry remains cached once; no GPS coordinates/history touched. Generated
  coordinate helpers use128-vertex parts to avoid JVM method-size limits without downsampling.
- Original/shared **72415line trace EXACTLY matches**, SHA256
 539826da12212bfbe852c1e1fc234906680296f82ae3a7dac293dffac5af634d.
  Originals/compiled oracle scratch/dream-original; DreamParity.java, DreamContractFixtures.java,
  kmp-{original,shared}-dream.log. Covers all6real basemap assets, exact vertex-bit hashes,
  clipped sections/metadata and500seeded assignment/progress cases. Raw geography remains
  decorative marathon equivalence, never physical-location evidence or an invented GPS route.
- JVM assign API keeps java.util.Random through expect/actual typealias. Native uses a separately
  implemented documented48-bit LCG plus NSLock, with reproducible seeded public factory for native
  callers. New JVM contracts compare sequences across seeds/bounds/rejection and exact old API
  descriptor. Existing JVM source signatures remain callable. No runtime/new-library dependency.
- Existing native whole/one-decimal label implementations now share FixedDecimalText with the
  new two-decimal Dream labels; original JVM UK formatters unchanged. Canonical decimal half-up,
  negative zero/nonfinite/large/tiny cases tested. Only label text is rounded, never stored values.
- All7source-generation entry points now emit shared Kotlin and retain STRICT complete-source
  --check comparisons. Original Python projections/source-count/licence/frame validation unchanged.
  New scripts/dream_kotlin.py reuses repository-pinned checksum-verified ktfmt for generated text;
  Java/warm formatter cache required by this BUILD TOOL, never an app dependency. Checks passed:
  Berlin191/Tokyo91/London164/NewYork371/Paris2026=1960/Paris2024=328vertices;18sourced landmarks.
- **422domain+14health JVM tests PASS**, zero failures/errors/skips;10new common and3JVM
  Dream contracts, including all6full vertex fingerprints, preserved IDs/cache, actual corners,
  overrun/distance/physical-location distinctions, exact RNG and0/1/2decimal compatibility.
  Latest public seeded-factory source reconfirmed by final build. **700189checks/48retained
  suites PASS** with assertions against prepared Kotlin classes; final72415line oracle still
  identical. No duplicate old Java implementation shipped.
- Core compile PASS1m24s. Full build passed422JVM tests then found2real nullable metadata bindings
  in DreamDrawing: canonical landmark names are known but raw Landmark DTOs permit null. Bound
  qualified labels explicitly without fake replacements. Also exposed seeded DreamRandoms factory
  publicly so actual native clients outside sharedDomain can create its RNG. Native constructor
  remains encapsulated; Java Random API unchanged. Final corrected build
  scratch/kmp-dream-final-build.log **PASS8m9s/166tasks**:422domain+14health tests, both Android
  APKs/lint PASS. Final formatting CHECK597sources/diff PASS. Actual local APK/version/code/min28/
  noINTERNET/shared classes verified (scratch/dream-local-apk-verification.json); private
  incremental artifact only. Initial failed log scratch/kmp-dream-verified-build.log preserved.
- Version0.12.3-test1/code19; formats19/13/3/1 unchanged. PR24 DRAFT; main85e1c39/PR23 untouched.
  No merge, version increment, public publication, canonical capture or physical acceptance claimed.

### Forty-second checkpoint — recovered route/custom-workout batch verified locally

- Resumed b49309f on experiment/android-ios-kmp-compose without resetting/discarding its nine
  uncommitted Java-to-Kotlin conversions. Live main still85e1c39; PR24 OPEN/DRAFT, PR23 untouched.
- b493 own experiment43749173197 and formatting43749173130 SUCCESS. Android132919066273:
  **362unique START/END cases**, no failures/skips/missing completions. Digest-verified artifacts
  confirm **422domain+14health JVM /390domain+16health Native**, all zero failures/errors/skips.
  Proof scratch/dream-b493-verified/verification.json. APK14176580597 ZIP6c9c3a6b0e4da0fb17ba484efbcee332b05078c39adb2e79685e35789e390144;
  extracted57,070,145bytes/SHA25665e2929d3172ace2bca73080ed23eec4bc44ff09a64ff6bddbe341183bf024bc,
  code19/min28/noINTERNET. Actual shared framework, NOT a functioning iOS application.
- This checkpoint ports TrackPoint, RecordedRoute, ActivityRecorder,
  StructuredWorkoutDelivery, CustomWorkout and TrainingInsights to common Kotlin. Three original
  Optional/stream interfaces now JVM Kotlin compatibility facades (UserPreferencesRepository,
  PrimaryFocusRepository/GpxCodec), not duplicated business logic. Nine Java originals removed;
  **14Java domain files remain**. JVM facade IO/Optional semantics intentionally remain platform
  specific; complete native storage/GPX remains unfinished.
- Preserves route coordinate/optional sensor evidence, within-segment ordering/independent segment
  boundaries, source identity, immutable snapshots; custom repeats/reordering/open-target totals;
  original28/56day insight gates, compensated sums, actual moving-time demand and noncausal shoe
  notes. No new recording/generation engine or automatic programme change.
- **700189checks/48retained assertion suites PASS**; original/shared **2295line trace matches**,
  SHA256533c1c8706b7a4987a58fb928910082adc8f2c88794488317e5e77f81a0e6df6.
  Scratch portability-original, PortabilityParity.java, kmp-{original,shared}-portability.log.
  javap confirms Kotlin-only classes and exact original Optional/IOException/stream signatures.
- Added12common and2JVM tests. First run436domain tests found2NEW ownership-test fixture errors:
  Kotlin MutableList casts reject read-only collections before reaching Java mutation methods.
  Corrected tests to invoke java.util.List directly, retaining UnsupportedOperationException
  assertions. Final corrected build scratch/kmp-portability-final-build.log **PASS5m25s/162tasks**:436domain
  JVM tests (zero failures/errors/skips), both Android APKs and lint PASS. Unchanged14health JVM
  tests independently verified at b493; own-source CI remains pending. First failed log
  kmp-portability-verified-build.log preserved. Formatting CHECK607sources, schema75/diff PASS.
  Actual local APK/code19/min28/noINTERNET/shared source identities verified in
  scratch/portability-local-apk-verification.json; private incremental artifact only.
- Added existing Release08UiTest3 + Release08StoreTest15 to experimental Android selection,
  retaining all362prior cases: **380EXPECTED Android /402domain+16health Native**, unverified on
  this source. No assertions weakened, no broad final matrix yet.
- Next achievement originals preserved/compiled OUTSIDE repository in scratch/milestones-original;
  TrainingMilestonesParity.java original4071lines/SHA256
  b3056a63af40ebfeb6d096ee6569ea6d24d31a4e46fb94c752400312a5a87359.
  Scratch TrainingMilestones.kt/TrainingMilestoneContractTest.kt PREPARATION ONLY, not applied.
  Fixture initially supplied missing StrengthSession offset; corrected to valid actual offsets
  before capturing oracle. Preserve original nullable Proof offset semantics.
- Version0.12.3-test1/code19/formats19/13/3/1 unchanged. No merge/publication/version increment,
  final canonical capture or physical acceptance claimed. Native root/storage/services/real iOS
  host and full platform acceptance remain unfinished. Next: preserve this verified local commit,
  complete the prepared achievements batch, then push coherent checkpoints together to avoid
  starting/cancelling duplicate expensive CI. No new-source Android/Native runtime pass claimed.

### Forty-third checkpoint — retained recognition and plan diagnostics verified locally

- Checkpoint42 committed locally as42acb48; not pushed yet, to combine coherent verified batches
  in one CI trigger. All9recovered conversions preserved. No reset/replacement PR/main development.
- This checkpoint ports TrainingMilestones and PlanRegression/PlanRegressionReport/
  PlanSnapshot;4Java originals removed, **10Java domain files remain**. Original award thresholds,
  working-set evidence, completed-run gate, duplicate/conflict rejection, recorded-offset Monday
  weeks and immutable earned-at evidence preserved. No streaks or new achievement rules.
- Plan diagnostics remain explicit engineering budgets, not new coaching rules. Missing distance
  stays UNVERIFIABLE/null, separate from ERROR, with unchanged weekly/race/taper/phase/hard-day
  checks. Original snapshot escaping, stable ordering, trailing-empty-line diff and60change bound
  preserved. Does not mutate/generate prescriptions or actuals.
- Original/shared **4071achievement lines match**, SHA256
  b3056a63af40ebfeb6d096ee6569ea6d24d31a4e46fb94c752400312a5a87359;
  **4546plan-diagnostic lines match**, SHA256
  65affaa76a9e7d19b1a26d5211364c0cd89910d5326c1fd6abef0f5ddc317368.
  Originals/compiled sources remain outside repo in scratch/{milestones,plan-diagnostics}-original;
  TrainingMilestonesParity.java/PlanDiagnosticsParity.java and kmp-{original,shared}-*.log.
- Actual existing AchievementsScreen now delegates to SharedAchievementsScreen, preserving copy,
  cards/disclosure/200% scrolling, optional celebrations, missing-source limitations and explicit
  callbacks. Android retains lifecycle, repository transactions and prepared local date labels.
  Added2actual shared UI tests, retaining all existing3light/dark/200% and6repository tests.
  Experimental selection now **391EXPECTED Android**; not executed on this source yet.
- Added14common+2JVM tests: award evidence, future/unknown/duplicate handling, recorded-offset
  weeks, retained snapshots, missing prescription measurements, context/phase diagnostics and
  escaped/ordered/bounded snapshots. **452domain+14health JVM PASS**, all0failures/errors/skips.
  Corrected full build scratch/kmp-recognition-final-build.log **PASS6m23s/166tasks**, both
  APKs/lint PASS. **700189checks/48retained suites PASS**; javap confirms Kotlin-only source.
  Formatting CHECK614sources and diff PASS; logs kmp-recognition-retained-suites.log and
  kmp-recognition-format-check.log. Actual local APK/code19/min28/noINTERNET/shared classes
  verified in scratch/recognition-local-apk-verification.json; private incremental artifact only.
- Initial scratch common-test fixture needed requireNotNull for known push-up catalogue entry;
  new plan-test source then had one extra closing parenthesis. Corrected syntax/fixture only,
  no assertions weakened. Failed logs kmp-milestones-core.log and kmp-recognition-verified-build.log
  preserved. Formatter now succeeds before build via fail-fast shell.
- Expected own-source Native **416domain+16health**; not claimed passed. Checkpoint42 Native402/
  Android380 never ran separately. Latest actually green remote source remains b493 as checkpoint42
  records. Version0.12.3-test1/code19/formats19/13/3/1 unchanged; PR24 remains DRAFT.
- Both coherent checkpoints are ready to push together; inspect their exact-head CI before
  claiming Native/Android runtime passes. Next retention originals/oracle preserved outside repo:
  scratch/retention-original, RetentionParity.java, kmp-original-retention.log2935lines/SHA256
  3c6a36737acb7a33974be5ff7811969a5bb09b9b1c772c266c118c69077cd4a7.
  Prepared DetailRetention.kt/RemovedRecordingDetail.kt and two test files remain SCRATCH ONLY,
  not yet applied/compiled. Preserve original informed consent, epoch/expiry/bounded transactions,
  exact counts and restoration fingerprints. Then continue remaining10Java
  (recovery, retention, reminders, bounded basemap IO) and actual native root/storage/services/host.
  No merge/publication/canonical screenshots/physical acceptance claimed.

### Forty-fourth checkpoint — shared retention consent and proof verified locally

- Checkpoints42acb48 andbe280213f256dc80235d79f4d32920f723a6a792 pushed together. PR24 OPEN/DRAFT
  description updated. be280 formatting43754601156 SUCCESS; experiment43754601167 RUNNING
  (Apple132935826740/Android132935826997); no own-source runtime pass claimed yet.
- This checkpoint applies DetailRetention/RemovedRecordingDetail common Kotlin,
  removes2Java originals: **8Java domain files remain**. Original selection/consent rules only;
  actual bounded SQLite transactions/archive IO remain platform-owned. No new deletion policy,
  permission, schema or backup format. Shared/native repository integration is still unfinished.
- UTC calendar-month cutoffs preserve original month-end/leap-year/nanosecond behaviour; reuses
  checkedAdd and existing calendar/instant/Java-whitespace compatibility. Preserves existing-user
  protection, strict age/app ownership, unknown counts, overflow rejection, duplicate conflicts,
  dataset epoch, chosen policy/identity,24hour preview expiry, active/recoverable/replacement
  protection,20row transaction bound and exact original restoration fingerprints.
- Original2935line oracle captured BEFORE edits in scratch/retention-original, RetentionParity.java,
  kmp-original-retention.log/SHA256
  3c6a36737acb7a33974be5ff7811969a5bb09b9b1c772c266c118c69077cd4a7.
  Actual Kotlin2935line output matches EXACTLY; javap confirms .kt implementations.
- Added8common+2JVM tests, including wide/negative-year calendar comparisons, protected ownership,
  unknown sample counts, consent, exact expiry/epoch/identity checks, repeated bounded batches,
  unchanged proofs and hashes. Added existing4StorageManagementScreenTest+11DetailRetentionRepository
  tests to focused Android selection, retaining all391previous cases: **406EXPECTED Android**.
  **462domain+14health JVM PASS**, all0failures/errors/skips. Native424domain+16health
  remains an unverified expectation for this source.
- Android achievement date labels now recompute per composition, preserving original behaviour
  when system zone changes without altering recognition/evidence timestamps; no cached date labels.
- Full build scratch/kmp-retention-verified-build.log **PASS7m45s/166tasks**, both APKs/lint.
  **700189checks/48retained suites PASS**; schema75, formatting CHECK616sources and diff PASS.
  Logs kmp-retention-retained-suites.log, kmp-retention-schema.log, kmp-retention-format-check.log.
  Actual private local APK/code19/min28/noINTERNET/new shared classes verified in
  scratch/retention-local-apk-verification.json. Own Android/Native runtime CI still pending.
- Recovery originals/compiled oracle preserved outside repo: scratch/recovery-original,
  RecoveryParity.java, kmp-original-training-recovery.log336lines/SHA256
  b602cacc8e0e634b6be245b5f9ce6a328cfe39462b8e069d84c37a6be9d3b8c3.
  Prepared TrainingRecovery.kt/RecoveryProgrammeProposal.kt and two test files remain scratch
  only until applied/compiled. Preserve existing future-only/protected choices and copy exactly.
- Remaining8Java: recovery2/reminders5/DreamBasemap1. Actual native app root/storage/services/host,
  final full platform/screenshot acceptance remain incomplete. Version/formats unchanged,
  no merge/publication/physical acceptance claimed.

### Forty-fifth checkpoint — shared recovery and explicit schedule proposals verified locally

- Retention checkpoint2478b47 committed locally, not yet pushed. be280 experiment43754601167
  and formatting43754601156 SUCCESS. Android132935826997: **391unique START/END cases**, no
  failures/skips/missing completions, including all new milestone/recorder/legacy-backup cases.
  Digest-verified JVM14177813393 and Apple14177674870 confirm **452domain+14health JVM /
  416domain+16health Native**, all0failures/errors/skips. Proof
  scratch/recognition-be280-verified/verification.json. Framework is still NOT an iOS app.
- be280 private CI APK14177768316 ZIP0baf1f852ae86314162958e0670b687f4dc762304c6a6bf68fe8d8a0b52f3c1d;
  extracted57,102,854bytes/SHA256d8a6dc9981c441aae20d57b791b7c1c1593840db777b113b372be52a86d3f109,
  code19/min28/noINTERNET verified. No public publication. Do not rerun verified old parents.
- This checkpoint ports TrainingRecovery and RecoveryProgrammeProposal to shared Kotlin;
  their2Java originals removed: **6Java domain files remain**. Existing health RecoveryCoaching
  stays preserved; this moves the distinct completed-training/scheduling evaluator, not a second
  replacement engine. No recovery score, diagnosis or automatic activation introduced.
- Keeps completed/recent working-set evidence, separate recorded RPE/RIR, duplicate conflicts,
  distinct recorded-offset dates, fresh corroborated health context, future/unknown exclusions,
  original programme priorities, same/before/after/race-week context, anchored-week alternatives,
  protected actuals and future-only immutable reviewed proposals. Reuses checked time arithmetic
  and checked integer revisions; original loads/tasks/rest/effort/later weeks remain unchanged.
- Existing TrainingRecoveryState and production recovery content now in sharedUi. Android retains
  lifecycle, real provider/repository calls, local date formatting and original preview/keep/refresh/
  move callbacks. Original public Android wrapper remains for current callers/tests. Qualified
  nullable raw DTO fields are explicitly bound at factory-driven consumers; no zero/empty fiction.
- Preserved original336line oracle in scratch/recovery-original, RecoveryParity.java and
  kmp-original-training-recovery.log/SHA256
  b602cacc8e0e634b6be245b5f9ce6a328cfe39462b8e069d84c37a6be9d3b8c3.
  Covers96histories/health/offset/reordering cases,3priorities, lighter-week targets/protection,
  all programme-workout conflict/date choices and explicit moves. Kotlin matches all336lines after canonicalising ONLY unordered days/equipment enum-set
  DISPLAY order. Raw original/shared logs preserved; first193review lines match without changes.
  Java Enum.hashCode is process-specific, so original raw Proposal.toString is not deterministic.
  scratch/canonical_recovery_trace.py validates every enum token then sorts those sets only;
  every member/duplicate and all workout/block/target/date/explanation order/content remain
  compared. Canonical SHA256708d19ed2bf97d0c52c570318234e14670199b61ccf4fea8adf6bc5f90fbb53e.
  No domain output or test assertion weakened.
- Added9common+1JVM contracts for missing/stale/future/duplicate actuals, RPE versus RIR, health
  corroboration limits, original priorities, immutable preview/protected dates/tasks, anchored
  alternatives and raw nullable record ownership. Existing4TrainingRecoveryScreenTest cases now
  exercise actual shared content. Focus retains all406prior cases: **410EXPECTED Android**.
  **472domain+14health JVM PASS**, all0failures/errors/skips; **700189checks/48retained suites
  PASS** against actual .kt classes, javap verified. Native433domain+16health remains EXPECTED,
  not yet tested on this source. Initial format CHECK620sources/diff PASS.
- First formatter rejected missing whitespace in scratch '!!==' expression; corrected to '!! =='.
  Fail-fast prevented starting Gradle on that invalid source. UI extraction guard caught a wrapped
  Java date expression before writing the shared screen; completed the exact formatter replacement
  safely. No assertions or training rules changed. Initial full build
  scratch/kmp-training-recovery-verified-build.log passed domain/sharedUI/app compilation then
  found ONE real Android-test nullable fixture error at TrainingRecoveryScreenTest:62. Wrapped
  its known explanation with requireNotNull, retaining the original assertDoesNotExist assertion.
  Corrected Android build scratch/kmp-training-recovery-android-final.log **PASS1m45s/157tasks**:
  both APKs/lint PASS. Final formatting CHECK620sources/diff PASS after the fixture guard fix.
  Actual local APK/code19/min28/noINTERNET/shared recovery/state/content classes verified in
  scratch/training-recovery-local-apk-verification.json; private incremental artifact only.
- **PUSHED**2478b47 and56bb65a5ae73f6c7172a01e58c42ed2118e61a49 together on
  experiment/android-ios-kmp-compose; PR24 remains OPEN/DRAFT and its description is current.
  Own experiment43757246519 is RUNNING (Apple132943915240/Android132943915590); formatting
 43757246520 SUCCESS. Last inspection: own CI domain/retained suites and schema steps SUCCESS,
  Android build and Apple tests/framework still in progress. Own410Android/433domain+16health
  Native remain EXPECTED until reports/logs confirm. No parent runtime pass borrowed.
- This final delivery-state handoff edit is DOCUMENTATION ONLY and intentionally left local after
  the verified source push, avoiding a duplicate expensive CI run. Preserve and include it in the
  next coherent source checkpoint; implementation commits and all source are already pushed.
- Next reminder originals/compiled oracle preserved WITHOUT changing their production sources:
  scratch/reminders-original, RemindersParity.java, kmp-original-reminders.log **9372lines**/
  SHA2568191a23ad96647f921cad715f4100a2ec4dd2a8c74fa8cedf9cea450938a61dc.
  Covers seven zones, five DST/dateline dates, spring gaps/autumn overlaps, five local times,
  all reminder modes/statuses, quiet hours, permission denial and exact2hour delivery boundaries.
  No shared reminder implementation has yet been applied. Preserve clock/zone/Optional JVM
  signatures through appropriate platform seams rather than duplicating scheduling rules.
- Remaining6Java: reminders5 and bounded DreamBasemap reader1. Native application root, typed native
  storage/services/full permissions/HealthKit and REAL iOS host remain unfinished. Version/formats
  unchanged, PR24 DRAFT; no merge/publication/canonical capture/physical acceptance claimed.

### Forty-sixth checkpoint — shared reminder decisions and regional calendar

- Resumed56bb65a preserving the uncommitted documentation-only handoff. Main still85e1c39;
  PR24 OPEN/DRAFT. Own56bb experiment43757246519 and formatting43757246520 SUCCESS.
  Android132943915590: **410unique START/END cases**, no failures/skips/missing completions.
  Digest-verified JVM14178832894/Apple14178723952: **472domain+14health JVM /
  433domain+16health Native**, all0failures/errors/skips. Proof
  scratch/recovery-56bb-verified/verification.json. Framework remains NOT an iOS app.
- Private CI APK14179191882 ZIPb35e77b27f6edabd8f33d05378b518692ca57db042d3dcc667d8b5389cd33630;
  extracted57,119,238bytes/SHA256b368b5db8aaff2539cb7a33a5febced9c2f6e1d19805f3ab3c089bc8332bc66c,
  code19/min28/noINTERNET verified. No public publication, no old-parent rerun required.
- This batch replaces all5Java reminder files. Shared EveningReminder/ReminderPlanEntry/repository
  and ReminderRules own original opt-in eligibility, content, statuses/revisions, quiet intervals,
  future scheduling and exact2hour delayed-delivery revalidation. **1Java domain file remains**:
  DreamBasemap. No new alarm engine or permission; actual platform notification delivery remains
  platform-owned. Shared rules accept a lazy clock and typed resolved local-calendar facts.
- JVM ReminderPreferences/EveningReminderPolicy are narrow Kotlin LocalTime/ZoneId/Clock/Optional
  compatibility adapters; quiet/default/decision logic delegates to common code. No duplicated
  business rules. Native callers use ReminderSettings and KotlinxReminderCalendar, placed with
  existing cross-platform integration adapters and existing datetime0.6.2 dependency. No new
  dependency. Native notification/permission/app-root wiring remains unfinished.
- Both JVM facade and actual cross-platform calendar JVM execution exactly match original
  **9372line oracle**, SHA2568191a23ad96647f921cad715f4100a2ec4dd2a8c74fa8cedf9cea450938a61dc.
  Scratch RemindersParity.java/CrossCalendarRemindersParity.java, reminders-original, and
  kmp-{original,shared,kotlinx}-reminders.log. Preserves gaps/overlaps, dateline skipping,
  seven zones, all modes/statuses, permissions, quiet-hour and2hour boundaries.
- Added8common domain tests and2common integration tests. Calendar tests compare175independently
  generated original java.time results with nanosecond precision; generator/CSV in scratch
  ReminderCalendarGold.java/reminder-calendar-gold.csv. **480domain+16health JVM PASS**, all0
  failures/errors/skips; **700189checks/48retained assertion suites PASS** against real Kotlin.
- Added existing8EveningRemindersTest Android tests while retaining410prior cases:
  **418Android /441domain+18health Native EXPECTED**, not yet tested on this source. One initial
  new-test fixture needed a Long literal to prevent Int nanosecond arithmetic; fixed the actual
  fixture, no assertion weakened. Failed log kmp-reminders-verified-build.log retained.
- Final build scratch/kmp-reminders-final-build.log **PASS6m/166tasks**, both APKs/lint.
  Formatting CHECK628sources/diff PASS; actual private local APK/code19/min28/noINTERNET/new
  Kotlin classes inspected in scratch/reminders-local-apk-verification.json. Initial core build
  kmp-reminders-core.log passed before the completed test additions; use final results above.
- Next basemap originals/compiled oracle captured WITHOUT changing its production source:
  scratch/basemap-original, BasemapParity.java, kmp-original-basemap.log63lines/SHA256
  7726d01d0ee23370328b75f3fc1428fc7141c607ddff92025f2cf0563a61a9b4.
  Covers all6real asset bit fingerprints plus malformed/header/line-ending/float/size limits.
  Prepared scratch/basemap-ports/*.kt NOT APPLIED or compiled. Preserve caller-owned stream and
  IOException JVM signature through typealias boundaries; no geometry downsampling/data mutation.
- Version0.12.3-test1/code19/formats19/13/3/1 unchanged. Commit this coherent verified batch,
  finish basemap and push checkpoints together to avoid duplicate expensive CI. Native application
  composition/storage/permissions/services/real iOS host and final platform/canonical capture
  acceptance remain unfinished; no merge/publication or physical acceptance claimed.

### Forty-seventh checkpoint — bounded basemap decoder; no Java domain sources remain

- Reminder checkpointff1c406 committed locally and preserved. This batch replaces the final Java
  domain file, DreamBasemap, with actual shared Kotlin. **0Java files remain under
  domain/src/main/java**. JVM Clock/Optional/stream/SDK compatibility adapters remain Kotlin,
  with common business rules; this is NOT completion of native storage/services/app integration.
- Common BasemapInput/BasemapReadException use JVM actual typealiases to the ORIGINAL InputStream
  and IOException. read(InputStream,String) throws IOException remains exactly Java-callable;
  Native actual byte input/errors implement the same protocol. Caller still owns/closes input.
  No second parser or copied DTO hierarchy; all decoding/validation/model snapshots are common.
- Preserves3MB/150000point/20000feature limits, header/frame checks, CR/LF/CRLF/trailing-line
  handling, coordinate finite/range checks, polygon closure/signed zero and all original vertices.
  No downsampling, projection, asset/licence change or stored GPS/history mutation. Only the known
  parsed feature kind is explicitly bound at DreamBasemapArtwork's existing path lookup.
- Original/shared **63line parser trace EXACTLY matches**, SHA256
  7726d01d0ee23370328b75f3fc1428fc7141c607ddff92025f2cf0563a61a9b4.
  Includes bit fingerprints for all6real bundled files, original valid numeric spellings, malformed
  tokens/headers/frames and resource limits. scratch/basemap-original, BasemapParity.java,
  kmp-{original,shared}-basemap.log. javap confirms .kt read signature with IOException.
- Added8common+2JVM decoder tests for paths, independent segments, source float formats, signed
  zero, missing/corrupt frames, exact limits, raw nullable metadata, immutable snapshots, IO failure
  propagation and caller-owned streams. **490domain+16health JVM PASS**, all0failures/errors/skips.
  **700189checks/48retained assertion suites PASS**; no old Java shadow or assertion weakening.
- Core build kmp-basemap-core.log PASS1m26s. Full build kmp-basemap-verified-build.log
  **PASS5m58s/165tasks**, both APKs/lint; format CHECK634sources, schema75/diff PASS. Actual private
  local APK/code19/min28/noINTERNET/new Kotlin classes verified in
  scratch/basemap-local-apk-verification.json. No public publication or canonical capture.
- Added existing9Dream asset/publication/readiness/presentation tests while retaining418prior
  focused cases: **427Android /449domain+18health Native EXPECTED**, not yet run on this source.
  Native tests include175original calendar gold cases and full decoder edge cases; inspect their
  real results before claiming native compatibility. A linked framework remains NOT an iOS app.
- **PUSHED**ff1c406 andf2bd55d91125d31e1041f633893d475ad5eb0bcb to existing draft PR24.
  Own experiment43761135855 and formatting43761135839 SUCCESS. Android132956005260:
  **427unique START/END cases**, no failures/skips/missing completions; all17new reminder/Dream
  cases ran. Apple132956005025 SUCCESS; actual Native calendar/float/stream/decoder contracts pass.
- Digest-verified JVM14180797976 and Apple14181170632 confirm **490domain+16health JVM /
  449domain+18health Native**, all0failures/errors/skips. Proof
  scratch/basemap-f2bd-verified/verification.json. Private CI APK14180464125
  ZIPf58e8baa2455ccfe47d7451d12a41cf8885678c63329fe0b3ef233264d62ea22;
  extracted57,135,681bytes/SHA256bd4827b15819a5c7bc870516dbd55db9e54efbab2c7d0f2e0af5b832c6def265,
  version0.12.3-test1/code19/min28/noINTERNET verified. Actual shared Native framework, NOT iOS app.
- PR24 body updated with exact successful results; remains OPEN/DRAFT. Main85e1c39/PR23 untouched.
  Version/formats19/13/3/1 unchanged. No merge/publication/physical acceptance claimed.
  This final verification-only handoff update is intentionally local to avoid another expensive
  docs-only CI; preserve/include it with the next coherent implementation checkpoint.
- Next priority after exact-head verification: actual common application composition/navigation,
  typed native SQLite/migrations/backup/retention, native permissions/HealthKit/recording/BLE/
  notifications and a REAL functioning iOS host. Preserve existing Android implementations,
  data formats and the already-shared UI; do not replace stable systems or add fake controls.

### Checkpoint 48 — Native SQLite persistence foundation (15 November 2026)

- Continuing existing branch/PR24; baseline f2bd55d exact CI confirmed successful again. Preserve
  its final verification-only local handoff, included with this implementation checkpoint.
- Added sharedStorage: exact generated Android schema1..19, checked against original migration
  literals by scripts/shared-schema.py --check. No schema/version/backup changes. Added real Apple
  system SQLite cinterop/serialised connection, bound UTF-16 text including NUL, exact Int64,
  distinct NULL/empty text/blob, foreign keys, nested savepoint transactions and close/error guards.
  No corruption fallback/deletion. Production Apple factory uses actual wall clock/UUID, protects
  existing consent, closes failed migrations and enables WAL. Not yet wired to an iOS app root.
- Shared typed settings/exclusion repositories and read-only retention preference reader use one
  connection. Existing Android repositories/helper remain unchanged. JDBC is TEST-ONLY SQLite
  oracle, not a new application dependency/second database. Exclusion restoration never deletes
  history. Native cleanup/archive/backup codecs are NOT implemented by this foundation.
- Schema creation/upgrades are atomic, reject future/unversioned nonempty DBs before schema writes,
  retain rollback, and apply NEW_INSTALL_DEFAULT only to truly fresh DBs. Canonical stored UTC
  parser retains original full Instant range/nanoseconds; 2048 deterministic round trips included.
- Local PASS: 15 real SQLite JVM tests (0 failures/errors/skips), including all18 historical upgrades,
  migration/consent failure rollback, protected retention, integer/null/text/blob preservation,
  nested transactions, file reopen, typed exclusion/settings behaviour and timestamp bounds.
  Logs scratch/kmp-storage-contract-final.log; prior sharedUi Android compilation PASS42s in
  kmp-storage-build.log. Existing schema75 PASS. Formatter/fingerprint/CI include new runtime module,
  system SQLite bindings and build inputs. CI now runs storage JVM/Apple tests and exports native
  storage with the existing shared framework. Expected17 storage Apple tests; NOT yet verified.
- Next: push coherent checkpoint and inspect REAL macOS cinterop/native tests/framework and focused
  Android CI; fix actual failures. Then complete typed native repositories/codecs/backup/retention,
  real common application root/native services and real iOS app host. No framework-only app claims,
  no version bump/publication/merge. Physical-device acceptance remains unperformed.

### Checkpoint 49 — Profile persistence and Apple binding correction (15 November 2026)

- Checkpoint48 pushed as4e35bb7c77eb30351531edff7bb612c34fea8804. Own experiment43764161239:
  Android shared JVM/domain/schema stages PASS, APK/lint/instrumentation still pending when checked.
  Apple132965404194 failed compiling custom SQLite bindings, before any storage native test ran.
  Exact errors: opaque sqlite3/sqlite3_stmt types live in cnames.structs, and prepare_v2's generated
  String wrapper does not accept a raw pointer. Corrected imports and added small C helper inside
  sqlite.def that validates a single SQL statement WHILE the temporary UTF-8 string remains alive.
  Never retain/read its tail pointer after cinterop frees it. Native re-verification is required.
- Added actual typed profile persistence over the SAME normalised Android tables. Snapshot reads
  report incomplete/corrupt profiles as errors; transactional replacements preserve unrelated
  activity history and roll back old profile/draft on failure. Running goal/date/target, independent
  running/gym availability and focus/legacy fallback retain existing meanings. Strength focus does
  not require a race or running profile. Existing Android repositories remain unchanged.
- Local updated PASS:19 real SQLite JVM tests (0failures/errors/skips), sharedUi Android compilation
  PASS53s (scratch/kmp-storage-profile-build.log). Includes failed profile replacement trigger,
  optional race data, exact availability, history survival and focus unknown/legacy handling.
  Native tests now also cover corrupt file byte preservation, production opener, SQL-tail/binding
  rejection, NaN rejection and resource close; EXPECTED23storage Apple tests, NOT verified yet.
- Tightened generator check to compare all19 migration BOUNDARIES as well as exact DDL. Runtime
  gradle.properties (cinterop commonisation) also included in screenshot source fingerprint.
- No schema/backup/version changes, public APK, canonical captures, merge or iOS-app claim.
  Next: inspect the next exact-head Apple compile/storage tests/framework and Android focused gate;
  fix actual errors. Then continue remaining typed storage/codecs/backups/retention and REAL shared
  application/native services/iOS host. Existing 449domain/18health native and427Android remain
  required; do not replace with storage-only tests. Physical acceptance remains unperformed.

### Checkpoint 50 — Native proof; typed height and milestone storage (15 November 2026)

- Checkpoint49 pushed e51c0c419372a522aad887a2753e68770a3bb568. Formatting43764761168 SUCCESS.
  Actual Apple job132967287219 /experiment43764761281 SUCCESS7m33s. Digest-verified artifact
  14181718656 ZIP410f1c30f4e65341bf2f737b8a50733d063008dbe5fdb3a02628e588e6313740:
  **449domain+18health+23storage Native**, all0failures/errors/skips. Actual23cases inspected:
  inherited migration/profile contracts ran, as did corruption byte preservation, SQL tail/binds,
  production open/reopen/identity/WAL, nested rollback, UTF16/NUL/Long/null/blob and full timestamps.
  Framework linked (artifact14181813447); still NOT an iOS application. Proof in
  scratch/storage-e51-native-verification.json and storage-e51-native.zip. Android job132967287502
  passed JVM/domain/schema/build/lint and is running focused instrumentation; inspect completion.
- Added shared typed SqliteBodyHeights and SqliteTrainingMilestones on existing tables: source
  conflicts reject without REPLACE, timestamp ordering uses exact seconds/nanos, nullable source
  offset/import time remain unknown, selected protein basis/disabled defaults retained. No weight
  owner/format change. Milestone awards/proof/recognition time are idempotent; failed batches and
  outer height/preferences transactions roll back together. No new achievement definitions/rules.
- Local latest PASS24storage JVM tests (0failures/errors/skips); preceding combined Android sharedUi
  compilation PASS45s, final new rollback test PASS20s. Logs kmp-storage-health-milestone-build.log,
  kmp-storage-typed-final.log. Native EXPECTED28storage tests on this later source, not yet verified.
  Generator also checks Android/current shared VERSION equality, not only DDL/migration boundaries.
- Still incomplete: shared JSON/history/programme/execution/backup codecs/repositories; full native
  retention/pruning/archive handling; actual common root/native recording/BLE/notifications/full
  health integrations and REAL iOS host. No app/merge/publication/version-bump claim.

### Checkpoint 51 — Portable shoe JSON/persistence (15 November 2026)

- Previous checkpoint50 committed locally9278ce1 (height/milestone repository batch). Parent e51c0c4
  experiment43764761281 FULL SUCCESS, formatting43764761168 SUCCESS. Android132967287502 inspected:
  **427unique START/END cases**, no failures/skips/missing completions; **700189checks/48retained
  suites** (Primary focus19 plus47TestSuites700170). Both APKs/lint/schema75 passed. Actual Native
  **449domain+18health+23storage**, digest/case proof in checkpoint50. No public publication.
- Added portable ShoeJson and typed SqliteShoes over existing payload/default columns. Preserves
  optional/zero mileage, escaped Unicode/NUL/notes, canonical full-range dates, actual default
  column authority, and non-destructive UPDATE so existing shoe assignments survive edits. Default
  changes roll back if insert fails. Original Android FeatureJson/repositories remain in place.
- Added official kotlinx-serialization-json1.7.3 DOM dependency for portable EXISTING JSON formats;
  no reflective serializers/compiler plugin/new format. Avoided a homemade JSON parser. Full pinned
  upstream licence included; root/bundled attribution inventories updated, including system SQLite
  (public domain) and TEST-ONLY JDBC3.46.1.3. No network feature or permission introduced.
- Local PASS30storage JVM tests, all0failures/errors/skips; sharedUi Android compilation PASS51s
  (kmp-storage-shoe-build.log). format CHECK654sources (559Kotlin/95Java); schema source/current
  version/boundaries CHECK passes. New Android SharedShoeCodecTest cross-reads ACTUAL FeatureJson
  outputs with common codec, including null/zero, Unicode/NUL, extended dates and coercion rejection.
  It is added to focused CI: EXPECTED429Android /34storage Native, NOT yet verified on this source.
- Local full APK/instrumentation APK/lint PASS5m17s; final source/assets/JVM30/build/lint
  recheck PASS1m52s in scratch/kmp-storage-shoe-final-build.log (160tasks). Final local APK contains
  pinned serialization licence/current attribution, version0.12.3-test1/code19/min28/noINTERNET;
  no JDBC native library bundled. This is PRIVATE local verification, not public publication.
- No schema/version/backup-format changes. Whole JSON13 backup compatibility, remaining codecs/
  repositories/native retention/archives and actual iOS app root/services/host are STILL incomplete.
  Canonical screenshots/full Android28/35/36/iOS APP/hardware acceptance are NOT claimed.

### Checkpoint 52 — Existing activity payload and exact time codecs (15 November 2026)

- Checkpoint51 pushed91d8489649aa08a2c2c2e36412f29d378ef7bc25 (9278ce1 also pushed).
  Own experiment43766955165: Apple132974134825 SUCCESS; framework artifact14182942392.
  Digest-verified Apple XML14183316062 ZIP3f4b2344ec84a5567cec9cef25dde5234dbddf5ae18156965c01ec6730f7326e
  proves **449domain+18health+34storage Native**, all0failures/errors/skips. Proof:
  scratch/storage-91d-native-verification.json and storage-91d-native.zip. Android132974134626
  still running focused429 instrumentation when last inspected; prior compile/domain/schema PASS.
- Continued next codec batch locally: ActivityJson matches existing FeatureJson activity payload
  fields, including unchanged totals, missing metrics/offsets, source IDs, sample discontinuities,
  laps and planned/shoe references. No GPS/pace/PB recalculation or invented routes/samples.
  Same200000sample/10000lap bounds reject excess rather than silently truncate.
- Shared explicit StoredJson value checks extracted from already-tested ShoeJson, preserving the
  existing null/zero/string/boolean distinction. Canonical StoredDuration uses exact integer/nano
  arithmetic across full original Long range; StoredOffset preserves second-resolution source
  offsets/unknown values. JVM DateTimeException discrepancy fixed by validating total offset
  bounds before platform factory, preserving assertions rather than weakening them.
- Local PASS38storage JVM tests (0failures/errors/skips); includes2048duration random/extreme SDK
  round trips, >2100source-offset cases, retained totals/gaps/summaries/nulls and oversize rejection.
  Actual Android compile of app/shared UI/instrumentation tests PASS2m17s/57tasks in
  scratch/kmp-storage-activity-android-compile.log. JVM logs kmp-storage-activity-verified.log.
  New SharedActivityCodecTest cross-reads actual Android FeatureJson in BOTH directions; compile
  passed, instrumentation NOT yet run. Format CHECK662sources (567Kotlin/95Java) and schema
  source/version/boundaries PASS. Next CI EXPECTED431Android /42storage Native (not verified).
- These are CANONICAL STORED-PAYLOAD readers, NOT a completed JSON13 backup restore pipeline.
  Before wiring backup input, retain all existing accepted older reads/normalisation, validate all
  sections/future/corrupt formats before replacement and restore atomically. No new format/version.
- Next exact task: inspect91d Android completion, finish/check/commit/push current activity codec
  batch, inspect actual next-head431Android/42storage Native/framework/format gates. Then port the
  real ActivityRepository including summaries/paging, annotations, raw hydration budgets/removed
  detail truth, source import authority and transactional result/PB updates. Remaining programme/
  execution/backup/archive/native services and REAL iOS app root/host still incomplete. No merge,
  public publication, canonical capture or physical acceptance is claimed.

### Final recovery boundary — pushed activity codec, next exact-head gate pending

- All implementation through checkpoint52 is committed AND PUSHED as
  **efba4256d2c4d1eb26d1a939762e3d0ac191e734**, existing branch/PR24 still OPEN/DRAFT. Remote
  main85e1c3903b61f1d45e444d120ba1a701ad3a2c36 fetched again; unchanged, separate PR23 untouched.
- Parent91d8489 experiment43766955165 FULL SUCCESS. Android132974134626 log inspected:
  **429unique START/END**, allcomplete/no failures/skips, both SharedShoeCodecTest cases ran.
  Domain **700189checks/48suites**. Digest-verified JVM14182938951
  ZIP76303637b32e7780aa98223c0fcee68912cdf36d896a3b71fcc8858280ca1bc1 confirms
  **490domain+16health+30storage**, all0failures/errors/skips. Native proof34 in checkpoint52.
  Private APK14183013770 and framework14182942392 exist; no public publication/IOS app claimed.
- NEW exact-head efba425 experiment **43768694789 RUNNING**, formatting **43768694910 SUCCESS**.
  Expected431Android/42storage Native; inspect actual logs/artifact XML before claiming results.
  Local38storage JVM + actual Android/instrumentation Kotlin compile PASS2m17s; format662 PASS.
- PR24 body updated with actual parent verification, current-head pending status and remaining
  full migration scope. Canonical stored-payload codecs are not a completed backup/restore engine.
- Next exact action: inspect43768694789 results, fix actual failures without weakening assertions;
  then port typed ActivityRepository using existing Android code/source import and hydration/PB
  rules, remaining JSON/history/programme/execution repositories, full backups/retention/native
  services and a REAL shared application/iOS app host. Do not restart completed domain/storage.
- This final CI/recovery-only note is intentionally local to avoid a docs-only expensive CI push;
  preserve/include it with the next coherent implementation checkpoint. No other uncommitted work.

### Checkpoint 53 — Real shared activity/result repositories (15 November 2026)

- Recovered existing branch/PR24 and preserved the previous local verification-only handoff.
  Live main85e1c39 unchanged. Parent **efba425** experiment43768694789 FULL SUCCESS;
  format43768694910 SUCCESS. Android132979512125 inspected: **431unique START/END** cases,
  allcomplete/no failures/skips, both actual SharedActivityCodecTest cases ran; **700189/48suites**.
  Digest-verified JVM14184310869 (8488ef657f2ebbc167e9c83b0da17327e0768df8546494dd36aec1f38cd07d64)
  proves490domain+16health+38storage; Native14183786475
  (95f784f4101273f1e5a9bc5523399a849e96a69b43d42de8d73fd9128320d479) proves449domain+18health+42storage,
  all0failures/errors/skips. Proofs scratch/activity-efba-{jvm,native}.{zip,json}. Framework is NOT app.
- Added actual SqliteActivities/SqliteResults implementing existing COMMON repository interfaces,
  ResultJson/RecordingPointJson, exact epoch-millisecond IO, read-only removal receipts. Same SQLite19
  tables/IDs/JSON payloads. No schema/version/backup changes and Android production stores unchanged.
- Activity source conflicts reject; upserts/deletions/checkpoints/annotations/qualified PB updates
  commit together using the EXISTING BestEffortCalculator, with injectable real clock. Manual PBs
  cannot overwrite derived identities; original distance-catalog rules/table priority retained.
  Default shoes assigned only on first eligible observation; provider refresh never overwrites
  athlete notes/RPE/shoes. Explicit deletion and provider deletion preserve their different semantics.
- Summary/cursor reads never hydrate native raw detail. Detail hydration uses500-row keyset pages,
  same FINISHED/rdd/gpx rules, original totals/laps/offsets and discontinuities. Coaching retains
  original12recent/28day/60000point budget. Missing/recoverable/pruned detail never creates samples.
  Removal provenance is read-only; NO native destructive cleanup was enabled.
- Local **61storage JVM tests PASS**, zero failures/errors/skips. Includes17real SQLite activity
  contracts plus file reopen, source/import/PB/annotation rollback, 60001point budget, raw corruption,
  stable paging/timestamps and receipt/history truth. Codecs test Long>2^53, null/zero, bounds and
  actual coordinate preservation. File evidence tests run on both JVM and Apple (Native pending).
- Added5 Android parity tests using original repositories/FeatureJson/RecordingStore and shared
  repositories on the SAME actual Android database, with test-only SQLite bridge. Outer rollback,
  actual PBs/annotations/shoes/imports and normalized raw hydration are compared directly. SDK36
  non-null edit-table annotations and JUnit's deprecated two-double overload corrected; no assertion
  weakening. Existing Android production helper/pool/IO checks remain unchanged.
- Local full **build/APK/instrumentation APK/lint PASS3m9s/160tasks**:
  scratch/kmp-activity-repository-release-checkpoint.log. Schema75 PASS, exact shared schema/version/
  boundaries PASS; format671sources scoped. Expected next CI **436Android /65storage Native**;
  not yet run on this source. No public publication/merge/physical or iOS-app acceptance claimed.
- Next: push this coherent checkpoint, inspect exact-head Apple/native and Android parity gates;
  fix actual failures. Then continue remaining baseline/coaching/programme/execution repositories,
  complete backup/retention/archive compatibility, actual common root/native services and REAL iOS
  host. Canonical codecs are not a full supported-backup reader; retain accepted older normalisation
  and validate all data before replacing user records. Do not restart completed domain/storage.

### Checkpoint 54 — Baseline references and atomic completed assessments (15 November 2026)

- Checkpoint53 pushed **daeceba9af1ac5cc65ce64a7ef4a6462e32118a4**, existing PR24 OPEN/DRAFT.
  Own experiment43771660620 running, format43771660568 SUCCESS. Actual Apple132988690044 SUCCESS;
  digest-verified14184906602 ZIP2be1210d385a251e5b6ac599cac615707992aacc094c19e539f46288ec4b62d4
  proves **449domain+18health+65storage Native**, all0failures/errors/skips. All65case names inspected
  in scratch/activity-daeceba-native.{zip,json}; framework14185111015 linked. Still NOT an iOS app.
  Android132988689829 passed compile/domain/schema stages and is running436focused cases; inspect
  actual completion including all5 original/shared Android repository parity cases before claims.
- Added BaselineJson/SqliteBaselines implementing the existing common baseline interface over the
  SAME baseline_state/assessment_results tables and original JSON payloads. Missing answers/date
  remain unknown; reads never generate a clock observation or recorded run. Explicit save retains
  actual observation time/review flag; restore can retain null original time. Completed assessment
  and its actual activity/PBs/annotations share one transaction and source/identity validation.
  The original user-activity source rule is shared with SqliteActivities. No training calculations.
- Local **66storage JVM PASS**, zero failures/errors/skips. Five more contracts cover missing/zero
  references, clock-free reads, actual assessment pairs, invalid linkage/provider, failed assessment
  rollback, whole-count JSON and fractional actual effort/time. Added2 real Android old/new baseline
  and assessment codec/repository parity cases (7new repository cases total; runtime NOT yet run).
- Actual app/shared UI/instrumentation Kotlin compile PASS2m41s/57tasks; full66JVM/build/bothAPKs/lint
  **PASS3m8s/160tasks** in scratch/kmp-baseline-repository-complete-build.log. Formatting673sources
  (578Kotlin/95Java) PASS; exact schema/current version/boundaries PASS. Existing schema75 PASS in
  this session. No database/backup/version/permissions/navigation changes or public publication.
- Expected next exact-head CI **438Android /70storage Native**; not verified yet. Canonical JSON
  stored-payload readers are NOT a complete supported backup reader; retain all supported older
  reads/normalisation and validate completely before replacing data. No destructive native cleanup.
- Next: verify parent436Android, commit/push this baseline checkpoint, inspect latest native70/
  Android438 (especially7 old/new repository parity cases). Then port planned-workout/coaching-plan
  storage/codecs, strength programme/execution repositories, complete backups/archives/retention,
  real common application root, full native services and functioning iOS host/app. Do not restart
  completed Java-domain conversion or the current typed persistence. Full Android28/35/36/iOS APP/
  canonical screenshots/hardware acceptance remain pending; no merge/publication until ready.

### Checkpoint 55 — Shared running-plan persistence and explicit decisions (15 November 2026)

- Preserved checkpoint54 and the local verification handoff; live main85e1c39 unchanged, PR24
  OPEN/DRAFT. Parent **b909442** experiment **43773014056 FULL SUCCESS**, formatting43773014059
  SUCCESS. Android132992905601 actual logs: **438unique START/END**, no missing cases/failures/skips;
  all7 original/shared activity/baseline repository cases ran. **700189checks/48suites** retained.
- Digest-verified JVM14185835757 ZIPf088207e9e149f44b65e16e68dcaa22d3605c1a3273ed0f19026b5ef25e0b33e
  proves **490domain+16health+66storage**; Native14185099720
  ZIP96ca6ec54c81677eb7ea2bb5cbc6d45005f85b6d43d051cec32bb2f25d52dcd8 proves
  **449domain+18health+70storage**, all0failures/errors/skips. Proofs scratch/baseline-b909-{jvm,native}
  .{zip,json}. Framework14185154436 is linked shared code, NOT an iOS app.
- Added ProfileJson/PlannedWorkoutJson/RunningCoachingJson and real SqlitePlannedWorkouts/
  SqliteRunningCoaching over existing SQLite19 tables. Immutable version/profile/phase/prescription
  snapshots remain separate from reminder mirror, responses and optional recommendations. Completed
  prescriptions cannot be rewritten. Decisions use the EXISTING AdaptiveCoaching implementation;
  KEEP does not consult a clock/generate a version. Decision/version/pointer/mirror commit together.
  No fake strength methods: this is the running half, to compose with the remaining strength store.
- Fixed actual old/shared interoperability: reminder revisions compare structured prescriptions
  instead of raw JSON number spelling/key order. Tiny original CoachingStore comparator delegates
  to shared value comparison; unchanged targets no longer produce false reminder revisions when
  alternating writers. Android integer -0 versus decimal -0.0 parsing matches original JSONTokener;
  exact Long step timing never goes through Double. No training or GPS calculation change.
- Added16 common actual-SQLite contracts (JVM/Apple wrappers) for rollback/foreign keys, completed
  history, stale/invalid/KEEP/REST choices, nested transactions, mirror revisions, duplicate rows,
  limits/null/optional/large integer evidence and legacy-number spelling. Added6 actual Android
  original/shared same-database codec/repository parity tests, including both writer directions.
- Local82storage JVM PASS and Android app/instrumentation Kotlin compile PASS2m33s/57tasks.
  Final formatted-source build/bothAPKs/lint PASS5m31s/160tasks in scratch/kmp-running-coaching-
  complete-build.log. Format682sources(587Kotlin/95Java) PASS; release-note10 PASS. Exact schema19
  boundaries PASS; existing75schema tests PASS. Native86 /Android444
  are EXPECTED next CI counts, not yet run on this source. No schema/version/backup bump,
  permissions/navigation change, merge or public publication. Full backup normalisation remains.
- Next: finish focused local build/format, commit/push this coherent batch; inspect its actual
  Native and Android results, especially6 new old/shared running-plan tests. Continue strength
  session/exercise/programme/execution codecs and repositories, full backups/archives/retention,
  common application composition, native services and REAL iOS app host. Preserve completed ports.

### Checkpoint 56 — Frozen strength evidence and retained exercise choices (15 November 2026)

- Checkpoint55 is committed locally as **cb95d1e**. Batching this next coherent storage work before
  one push, preserving b909442's fully green remote checkpoint and the existing draft PR24.
- Added StrengthEvidenceJson for original CoachingJson exercise/actual-session payloads. Frozen
  original definitions, actual load/repetitions/duration/distance, warm-up flags, fractional RPE
  versus RIR, unknown effort, source offsets and nanosecond timestamps round-trip. Legacy absent
  measurement/seconds/metres/RIR retain existing mode-specific defaults or unknown values. No
  catalogue lookup, generated technique, prescribed sets or invented maximum becomes an actual.
- Eight common tests cover all modalities/weighted bodyweight, frozen definitions, legacy reads,
  invalid types/effort/measurement conflicts, duplicate IDs/limits and a full10000actual-set workout.
  Two actual Android tests compare old/new codecs BOTH ways, including genuine malformed input.
- Added typed SqliteStrengthChoices over existing strength_user_state/pb_acknowledgements: original
  favourite/archive flags and stable idempotent acknowledgements. Exclusions remain their existing
  separate reversible repository; no PB/workout/achievement is created or removed by these choices.
  No new table, generic payload bucket or migration. Four additional real-SQLite JVM/Apple contracts
  cover independent flags/unknown identities, rollback, original length validation and UTF16/NUL
  identities. Three Android tests compare the original StrengthUserStore and shared storage on the
  SAME database, original-writer reads, acknowledgements and outer transaction rollback.
- Local **94storage JVM tests PASS**, zero failures/errors/skips. Earlier90test payload/app/
  instrumentation compile PASS3m5s/57tasks; final full build/bothAPKs/lint **PASS5m3s/160tasks**
  in scratch/kmp-strength-evidence-choices-build.log.
  Format **687sources(592Kotlin/95Java) PASS**; exact schema1..19 boundary/source VERSION check PASS.
  No changed training/GPS/recovery calculations, schema/backup/version/permissions/navigation.
- Expected combined next CI **449Android /98storage Native**, not yet run. Final source still needs
  real Apple execution and actual Android parity tests. No iOS app, canonical visual or physical
  acceptance claimed; no public publication/merge. Full supported-backup input/restore guards are
  still incomplete, distinct from these canonical stored-payload codecs.
- Next: record final local build result, commit and push checkpoints55–56 together; inspect actual
  Apple and Android gates, fix root failures without weakening assertions. Then port the strict
  GymJsonSupport input size/depth/token/duplicate-field guards and GymWorkoutJson frozen workout
  codec before execution/programme storage. These strict gym payloads are DISTINCT: exercise
  estimateOneRepMax and completed durationMillis differ from legacy CoachingJson estimate/duration.
  Do not reuse the wrong wire shape. Preserve supersets, source revisions, actual edit/
  association provenance, transactional completion and older/current backups; never enable partial
  native destructive history/cleanup commands. Common app root/native services/REAL iOS host remain.

### Checkpoint 57 — Strict frozen gym-workout codec (15 November 2026)

- Checkpoints55–56 committed/pushed together: **cb95d1e1259df7d89ea9dadb511c11346a666fd1** then
  **31101d7c61560d641d237e8333682521a4362d44**. Existing PR24 OPEN/DRAFT, body updated with actual
  completed/pending scope. Experiment **43776246774 RUNNING**, formatting43776246786 SUCCESS.
  Android133003085132 passed domain/schema/build and is running expected449focused cases;
  Apple133003085372 SUCCESS: digest-verified14186068645
  ZIP94258ccd7dea53b94e258cffad900d75554a090c3e159081406d94ec893a89ca proves
  **449domain+18health+98storage Native**, all0failures/errors/skips. Proofs scratch/coaching-311-
  native.{zip,json}. Framework14186707231 linked; NOT an iOS app. Android449 still pending.
- Added internal GymStoredJson preserving original strict gym envelope validation BEFORE allocating
  a DOM:16MiB UTF8,20nesting,1000000tokens, bounded strings, standard JSON/no comments/trailing data,
  duplicate keys including escaped names, exact fields/types/whole-integer tokens. Reuses existing
  shared JSON primitives and serialization dependency, no new library or separate training model.
- Added FrozenGymWorkoutJson for original v1 GymWorkoutJson payloads. Frozen exercise identities,
  optional targets/reference loads, all measurement profiles, RPE versus RIR, superset ordering and
  100slot limits round-trip through existing common domain constructors. This wire format remains
  DISTINCT from legacy actual-session CoachingJson (estimateOneRepMax versus estimate; execution
  completed-session durationMillis versus legacy duration). No actuals are generated from targets.
- Ten common tests exercise valid/failing formats, escaped duplicate keys, limits, exact clocks,
  supersets/modality/effort/reference meanings and maximum100slots. Three actual Android parity
  tests use original GymWorkoutJson BOTH ways, including rejected unknown/future/exponent fields.
- Regression found a real portability mismatch: JsonPrimitive.longOrNull accepts integral exponent
  tokens such as3e0, whereas original Android requiredLong/gym whole rejects Double tokens. Shared
  numeric parsing now tests the literal content with String.toLongOrNull; numeric Double reads
  still use proper Double parsing for exponent tokens. Integer -0 versus decimal -0.0 and exact
  large Long evidence remain covered. Focused failing case PASS25s after correction; full
  **104storage JVM tests PASS**,0failures/errors/skips. No assertion weakening. Initial Kotlin object
  wildcard-import/nullable enum inference errors corrected with explicit imports/type arguments.
- Formatting **691sources(596Kotlin/95Java) PASS**. Final source build/bothAPKs/lint PASS5m24s/160tasks in
  scratch/kmp-frozen-workout-complete-build.log. Expected next
  source **452Android/108storage Native** remains unverified. No schema/version/backup/permission/
  navigation/engine change, native destructive commands, merge or public experimental publication.
- Next exact action: inspect the current local build and parent31101d7 CI, fix actual failures;
  commit/push this coherent strict-codec checkpoint when locally verified and inspect its real
  Apple/Android gates. Then continue GymExecutionJson, GymProgramme/StrengthProgramming/concrete
  draft/association/accepted-actual/tools codecs and typed repositories, preserving exact payload
  guards, retained source revision validation, actual edits and transaction boundaries. Complete
  full older/current backup normalisation/archives/retention before wiring native destructive
  operations. Real common app root, native services and iOS app host/app build remain unfinished.

### Checkpoint 58 — Gym Now execution snapshot wire format (16 November 2026)

- Checkpoint57 committed/pushed **a79cd9889cf55edb6d0d693ea681aed8f0b73e8b**, same draft PR24.
  Experiment **43777437866 RUNNING**, formatting43777437837 SUCCESS. Expected452Android/
  Android452 remains pending. Apple133006823423 SUCCESS: digest-verified14186508735
  ZIP79e2add9eef11a4b4f385a63940f6bd79abade07f51eb3c95a2361ada15b44f4 proves
  **449domain+18health+108storage Native**, all0failures/errors/skips; scratch/frozen-a79-native
  .{zip,json}. Framework14185864989 linked. No iOS app host has been implemented yet.
- Parent **31101d7 experiment43776246774 FULL SUCCESS**, format43776246786 SUCCESS. Actual Android
  133003085132: **449unique START/END**, allcomplete/no failures/skips; all11new running-plan/
  strength-codec/choice parity tests ran. **700189checks/48suites** retained. Digest-verified
  JVM14187200643 ZIPa87d39b0b9db021c9fd6fe080a36a2bc17f0932fd08f8f70692e648af7c00e3c proves
  **490domain+16health+94storage**. Native98 proof above; files scratch/coaching-311-{jvm,native}
  .{zip,json}. Actual linked framework is not an application; APK14186991659 is private CI only.
- Added GymExecutionSnapshotJson preserving original strict v1 GymExecutionJson envelope, original
  and today-only slots, source/revision identity, exact monotonic clock, actual exercise/set records,
  events, optional completed session and partial status. Uses the SAME GymExecution constructors
  to replay event/state invariants. No second execution engine; native execution writes/finalisation are not wired.
  Repository/source-content validation is still required separately from source identity checks.
- Six common tests cover active/paused/finished/free states, warm-ups, fractional RIR, optional
  effort, source provenance, exact clocks above2^53, honest process recovery, superset changes and
  original/replaced actual exercise separation. Forged events/timing/completion, unknown fields,
  exponent clocks and collection limits reject. Two Android tests compare original/shared readers
  BOTH ways, real lifecycle/recovery/substitution and forged payload rejection.
- Focused6tests PASS38s after correcting a test syntax error and binding the planned fixture to its
  genuine domain-required source identity/revision; no invariant/assertion weakening. Full local
  **110storage JVM PASS**,0failures/errors/skips. Final build/bothAPKs/lint **PASS1m55s/160tasks**:
  scratch/kmp-gym-execution-snapshot-build-fixed.log. Original attempt identified a nullable Double
  in the Android delta assertion; explicit non-null unboxing retains the exact assertion and fixes
  compilation. Format **694sources(599Kotlin/95Java) PASS**, exact schema1..19 boundaries PASS.
  Expected114storage Native/454Android still require actual CI; never assumed passed.
- Next: complete local gates, inspect checkpoint57 CI; commit/push this batch only once locally
  verified, preserving parent gate evidence without blind reruns. Then port remaining programme/
  concrete draft/association/accepted-actual/tools/backup codecs and real typed repositories.
  Execution persistence/finalisation MUST preserve retained source content, atomic actual/session/
  association commits, deliberate actual-edit provenance and corruption/rollback guards. Native
  destructive commands remain disabled until full retention/restore safeguards are complete.
- Version0.12.3-test1/code19/SQLite19/rootJSON13/raw3/package1 unchanged. No recording/domain-rule/
  permission/navigation change, public publication or merge. Real common app composition, native
  services, full supported backup normalization and functioning iOS app host/app gates remain.

### Checkpoint 59 — Strict accepted actuals without breaking legacy reads (16 November 2026)

- Checkpoint58 committed locally **ddf09b33806098419ef93eceb4d563187890da0e**; batching this small
  related accepted-actual guard before one push, avoiding cancellation of checkpoint57's gate.
- **a79cd98 experiment43777437866 FULL SUCCESS**, formatting43777437837 SUCCESS. Android133006823601
  actual logs: **452unique START/END**, allcomplete/no failures/skips; all3new original/shared frozen
  workout parity cases ran. **700189checks/48suites**. Digest-verified JVM14187196913
  ZIPa05b144c9dc951ec71a3dc75562d3bd7528a9d361811dbd88ea00ec4a1a99434 proves
  **490domain+16health+104storage**; Native108 proof above. Proofs scratch/frozen-a79-{jvm,native}
  .{zip,json}. Private APK14186763946 exists; framework14185864989 is NOT an iOS app.
- Added StrengthEvidenceJson.decodeAcceptedSession/encodeAcceptedSession and internal row reader
  matching original GymAcceptedActualJson: strict complete field sets/types/limits/whole tokens
  BEFORE existing actual-domain construction. Accepted encoding validates representability before
  any future storage write. Legacy decodeSession still preserves its supported missing-field and
  whole-numeric repetition defaults. These are deliberately separate formats/read contracts.
- Four more common tests cover accepted actuals across modalities, missing/unknown field rejection,
  exact integer tokens, representability and preserved legacy defaults. Two Android parity tests
  compare original strict and legacy readers. Specifically8e0 repetitions remain valid under old
  coaching integer rules but reject in the strict accepted section, matching original Android.
- Local **114storage JVM tests PASS**,0failures/errors/skips; actual app/shared UI/instrumentation
  Kotlin compile **PASS2m37s/57tasks** in scratch/kmp-accepted-actual-compile.log. Latest3changed
  files pass targeted pinned format CHECK; other691source files are unchanged from the full694check.
  Previous execution batch bothAPKs/lint PASS1m55s/160tasks; latest child source APK/lint runtime
  verification belongs to the next exact-head CI, not claimed passed. Schema/version unchanged.
- Next: commit and push checkpoints58–59 together; inspect actual CI expected **456Android /
  118storage Native**, never assume the counts. Then port StrengthProgrammingJson (including exact
  authored Workout/settings/custom definitions) and legacy programme/concrete drafts/tool payloads.
  GymAssociationJson MUST retain BOTH legacyv1 and authoredv2; do not approximate authored workouts
  as legacy sessions or silently drop a variant. Wire real execution/programme/association
  repositories only with source-content validation, atomic finalisation and actual-edit provenance.
- Complete full supported older/current backup/archives/retention/restore safeguards and actual
  common application root/native services/iOS host/app build. All original Android logging,
  recording, Health Connect and cleanup remain intact; only unimplemented native destructive
  commands are disabled. No app version/schema/backup bump, public publication or merge.

### Final recovery boundary — checkpoints58–59 pushed; latest gate pending

- Current branch experiment/android-ios-kmp-compose; HEAD/remote
  **610cf0b0619a0f16efe8a7bcbd7904e014699a56**. Checkpoint58ddf09b3 and59 were pushed together.
  GitHub confirms existing PR24 OPEN/DRAFT at this head; body rewritten with completed work and
  complete remaining migration scope. Main85e1c39 /separate owner PR23 preserved. No merge,
  version bump, public experimental publication or iOS app acceptance.
- Latest experiment **43779276337 RUNNING**, formatting **43779276309 SUCCESS**.
  Inspect actual results before relying on expected456Android/118storage Native. Latest local
  **114storage JVM PASS** and actual Android app/shared UI/instrumentation compile PASS2m37s;
  pinned targeted3files CHECK PASS. Earlier full694format and execution-batch bothAPKs/lint PASS;
  latest child APK/lint/runtime are pending exact-head CI, not falsely reported as passed.
- Parenta79cd98 experiment43777437866 FULL SUCCESS; format43777437837 SUCCESS. Actual Android
  133006823601 **452unique START/END**, allcomplete/no failures/skips; all3frozen-workout parity
  cases ran. **700189checks/48suites**, digest-verified490domain+16health+104storage JVM /
  449domain+18health+108storage Native. Proofs/digests/artifacts in checkpoints58–59 above.
- Next exact action: inspect43779276337 (format43779276309 passed); verify actual Android class cases and
  digest-verified Native/JVM XML, fix actual failures without weakening assertions. Then port
  StrengthProgrammingJson (programme/draft/settings/custom definitions including exact authored
  Workout), legacy GymProgramme/concrete-draft/tool codecs, BOTH associationv1/v2 and real typed
  execution/programme/association repositories with source-content/actual-edit/atomicity safeguards.
  Continue complete supported backups/archives/retention, real common application composition,
  native OS services and REAL iOS app host. Preserve completed Java-domain conversion and UI.
- This final verification/recovery-only note is intentionally local to avoid another expensive
  docs-only CI push. Preserve/include with the next coherent implementation commit. No other
  uncommitted work remains. Current working branch has a functioning Android compile, not a
  functioning iOS application; actual macOS CI works and must not be described as unavailable.

### Checkpoint 60 — Authored strength payloads and portable execution fixture (16 November 2026)

- Recovered branch610cf0b /existing OPEN DRAFT PR24, fetched live main85e1c39 unchanged, preserved
  the local verification-only handoff. No reset/restart/force push or replacement PR.
- Actual610cf0b experiment43779276337: Apple133012552771 FAILED compiling common TEST source,
  GymExecutionSnapshotJsonTest.kt:15 unresolved plusSeconds. JVM java.time actual exposed that
  member, but the shared Instant contract does not. Replaced fixture arithmetic with existing
  Instants.ofEpochSecond + original seconds/nanoseconds, preserving the same wall/monotonic values.
  No production time API, clock semantics, recording or domain calculation changed. Native fix
  still requires actual Apple CI; no blind rerun of unchanged failing source. Android133012552962
  was still running456cases when last inspected; inspect completion before claiming its result.
- Added StrengthProgrammingPayloadJson matching original v1 StrengthProgrammingJson: immutable
  programmes/revision lineage/training blocks, exact authored Workouts, editable/incomplete drafts,
  custom exercise metadata/equipment alternatives, settings and trade-offs. Reuses strict bounded
  gym input/frozen-workout codecs and existing domain constructors. No catalogue lookup while
  decoding history, inferred technique, generated actuals, changed programme or auto-activation.
  Wire enum names sort alphabetically as before; enum sets reconstruct ordinal input before the
  EXISTING domain immutable-set semantics. No domain collection contract changed.
- Nine common tests cover actual existing generator5goals/3experience +all9templates, frozen names/
  fractional effort/reference meanings, incomplete drafts, six custom measurement profiles,
  enum/duplicate/field/kind/future/coercion/date/collection limits and maximum12blocks. One new test
  initially assumed ordinal iteration from Settings; original Immutable.copySet uses HashSet.
  Corrected the expectation to the original Settings snapshot/set contract, retaining explicit
  wire-order, set-membership and source-snapshot iteration assertions. Focused case PASS26s.
- Four real Android parity tests compare original/shared programme/draft/custom codecs BOTH ways,
  all goals/experience/templates, frozen names, actual original set iteration and invalid payloads.
  Local **123storage JVM tests PASS**,0failures/errors/skips. Actual Android app/shared UI/test
  compilation **PASS2m23s/57tasks**, scratch/kmp-strength-programming-payload-android-compile.log.
  Full format **697sources(602Kotlin/95Java) PASS**, exact schema1..19 boundaries +existing75schema
  tests PASS. Latest APK/lint/runtime/native pending exact-head CI, not falsely claimed passed.
- Expected next CI460Android/127storage Native must be verified; no schema/version/backup bump,
  public publication, merge, permissions or user-visible feature redesign. Preserve all existing
  original Android stores and functionality; full native execution finalisation stays unwired.
- Next: commit/push this coherent payload +fixture fix, inspect actual native compile/runtime and
  Android cases; fix genuine failures. Continue BOTH GymAssociationJson legacyv1/authoredv2 using
  exact authored Workout codec and strict accepted-actual reader, then remaining legacy programme/
  concrete draft/tools/archive codecs and typed execution/programme/association repositories.
  Retained source-content/actual-edit/atomicity safeguards, full supported backups/retention,
  common application root/native services/REAL iOS app host and final full gates remain required.

### Checkpoint 61 — Both association decision families (16 November 2026)

- Checkpoint60 pushed **fd5ac1e204f2bb392dd18ea58965e033247a8de3**. Experiment43780857820 RUNNING,
  formatting43780857827 SUCCESS. Apple133017467014 SUCCESS: the JVM-only test-clock defect is fixed
  on actual Native. Digest-verified14188097622 ZIP5a1920b1d0e980e4a226ecaa29fbd87a392fefcbf423dc601e30a4b52527b296
  proves **449domain+18health+127storage Native**,all0failures/errors/skips. All source-case names
  inspected; scratch/programming-fd5-native.{zip,json}. Framework14188596595 linked, NOT an iOS app.
  Android133017466871 running expected460 cases; do not claim the runtime result yet.
- Added GymAssociationDecisionJson preserving BOTH original legacyv1 and authoredv2 formats. Exact
  authored Workout/fractional RIR/optional load references are not approximated as legacy Session.
  Frozen actuals use the strict accepted reader; metadata/source/review/decision times and both
  KEEP_EXTRA/COUNT_TOWARD_SESSION choices survive. Encoding revalidates representability before a
  future storage write. Reading creates NO programme link, applied revision or actual workout.
- Seven common tests cover both versions/choices, reference82.25 versus recorded80, warm-ups,
  authored RIR/unknown reference, invalid family/field/version/integer/date/collection input,
  incomplete actual evidence and review/completion ordering. Three actual Android old/shared
  parity tests compare both readers/directions, exact family meanings and malformed decisions.
- Local **130storage JVM PASS**,0failures/errors/skips. Full actual app/debug APK/instrumentation
  APK/lint **PASS5m18s/160tasks**, scratch/kmp-gym-association-codec-build.log. Full formatting
  **700sources(605Kotlin/95Java) PASS**. Existing75schema tests and exact1..19 boundary checker PASS
  this session, no schema/version/backup/permissions/navigation or domain calculation changes.
- Expected next source463Android/134storage Native NOT yet run. Next commit/push and verify actual
  gates, then legacy GymProgramme/decision storage, concrete draft/tools/archive payloads and real
  typed execution/programme/association repositories. Preserve all original Android stores/flows;
  full native finalisation/source-content/actual-edit/retention/restore safeguards remain mandatory.
  Common app root/native services/REAL iOS host and final full gates remain unfinished.

### Checkpoint 62 — Typed legacy gym programme history and decisions (16 November 2026)

- Checkpoint61 committed locally **736e52d**; batch its association codec with this related legacy
  persistence work before a single push. Parentfd5ac1e experiment43780857820 FULL SUCCESS and
  formatting43780857827 SUCCESS. Actual Android133017466871: **460unique START/END**, allcomplete/
  no failures/skips; all4 original/shared authoring codec cases ran. **700189checks/48suites**.
  Digest-verified JVM14189255105 ZIP1deeb8b665fe1daa3b666740d721514e85b81b15adc39d7a804fcd0a98a0b792
  proves **490domain+16health+123storage**; Native127 proof in checkpoint61. Files scratch/programming-
  fd5-{jvm,native}.{zip,json}. Private APK14187859974 exists; framework is NOT an iOS application.
- Added LegacyGymProgrammePayloadJson preserving original GymProgrammeStore snapshots and explicit
  KEEP/INCREASE decision payloads. Legacy whole-number/default/optional reference rules remain
  distinct from strict association records. Shared legacy session encoding is reused; no generic
  JSON bucket or independent programming model. Its version field remains a programme revision.
- Added actual SqliteLegacyGymProgrammes over existing gym_plan_versions/gym_current/
  gym_progression_decisions. Versions append; disable removes only the current pointer. Explicit
  version/decision/pointer updates are atomic and join outer transactions. Active authored
  programming blocks legacy activation. No prescribed set becomes an actual and no PB/history is
  deleted. Reads validate indexed identity; native writes reject unreadable metadata before insert.
- Reused the EXISTING JDK-compatible isJavaBlank helper by exposing its unchanged implementation
  across modules (visibility only); no new whitespace algorithm or training rule. NBSP identities
  remain valid, Java-blank EM SPACE rejects. Exact UTF16/NUL identities and legacy keys retained.
- Ten common real-SQLite contracts run on JVM/Apple wrappers: append/disable/history, stale/skipped/
  duplicate versions, KEEP/INCREASE history, invalid metadata, trigger/outer rollback, exact/corrupt
  indexed identities, legacy optional/numeric reads, limits and modern-current protection. Three
  actual Android tests compare original/shared codecs/repositories on the SAME database, alternating
  writers, explicit decisions, outer rollback and original unusual-ID rules.
- Local **140storage JVM PASS**,0failures/errors/skips. Full actual app/debug APK/instrumentation
  APK/lint **PASS5m4s/160tasks** in scratch/kmp-legacy-gym-repository-build.log. Full formatting
  **706sources(611Kotlin/95Java) PASS**. Exact schema1..19/current VERSION and existing75schema tests
  PASS this session; no schema/version/backup/permission/navigation changes or public publication.
- Expected combined next head **466Android /144storage Native** pending; verify actual logs/XML.
  Next commit/push checkpoints61–62, inspect Apple and Android results, fix real failures. Then
  remaining concrete-draft/tools/archive codecs and real authored programme/execution/association/
  strength-actual repositories; preserve source-content checks, finalisation/actual-edit provenance
  and atomicity. Full supported backups/retention, common app composition/native services/REAL iOS
  host/app build and full release/visual/hardware gates remain unfinished. Keep PR24 draft.

### Checkpoint 63 — Concrete draft previews and retained substitution provenance (16 November 2026)

- Checkpoints61–62 pushed together: **736e52d**, then
  **a7693750d11b9f5224fd3a7b16871dd4e2aff978**. Existing PR24 remains OPEN/DRAFT. Its experiment
  **43783654594 RUNNING**, formatting43783654586 SUCCESS. Apple133026354939 and Android133026355179
  are running expected144storage Native/466Android; verify actual results, do not assume counts.
- Added ConcreteStrengthDraftPayloadJson matching original independently versioned v1
  ConcreteStrengthProgrammeJson. Reuses exact authored programme codec, typed LEGACY/AUTHORED
  base references and bounded substitution metadata. Draft revision remains distinct from candidate
  programme revision and accepted base revision. No acceptance, activation or actual evidence is
  inferred on read. Full preserved-base validation remains the separate existing validateAgainst.
- Five common tests cover initial/legacy-replacement previews, forward revision/protected unchanged
  workout preservation, fractional RIR and cleared incompatible load reference, malformed/future/
  missing/unknown/coerced fields, wrong-family/stale base references, duplicate/oversize changes and
  duplicate JSON. Two actual Android old/shared parity tests compare both directions, valid retained
  proposals and rejected input against the original codec/domain, without mutating active state.
- Local **145storage JVM PASS**,0failures/errors/skips; Android app/shared UI/instrumentation
  compile **PASS2m37s/57tasks**, scratch/kmp-concrete-draft-payload-android-compile.log. Full format
  **709sources(614Kotlin/95Java) PASS**. Latest child APK/lint/runtime/native pending next exact-head
  CI; preceding checkpoint62 full bothAPKs/lint PASS5m4s/160tasks. No schema/version/backup bump or
  permission/navigation/engine change, native destructive command, public publication or merge.
- Expected next source468Android/149storage Native is unverified. Next commit this preview codec,
  inspect a769375 gate and fix actual failures; push the next coherent batch. Then shared gym tools
  preferences/timer persistence and strict archive validation (original SqliteGymToolsRepository
  lines148–265), programming decision/archive records, and real authored programme/concrete draft/
  execution/association/actual-edit repositories. Preserve actual source-content validation,
  explicit acceptance, single DB transactions and backup/retention guards before native deletion.
  Full supported backups/archives, real common app root/native OS services/iOS app host and final
  full Android28/35/36 +iOS APP/source-bound visual/hardware gates remain incomplete. Keep PR24 draft.

### Checkpoint 64 — Shared gym tools and complete authoring graph guards (16 November 2026)

- Continue branch `experiment/android-ios-kmp-compose`, existing OPEN/DRAFT PR24. Preserved the
  previous local recovery note and all implementation. Main remains85e1c39; owner PR23 untouched.
- Recovered checkpoint63 `b1b3d91`: experiment43785510489 and formatting43785510508 both SUCCESS.
  Android133032115530 actual logs prove **468unique START/END**, both concrete-draft cases ran,
  **700189checks/48suites**. Digest-verified JVM14189854929 proves490domain+16health+145storage,
  ZIP1a39177d422f594c08439828e3b3f62ad54f4ecfaf9ac388fdd53fe13975994e.
  Native14190028283 proves449domain+18health+149storage,
  ZIP1c8074d2dd045bc9aaf4333de3eecab5165dbb0c081bc9bf98aae01db57f4ede.
  All XML failure/error/skip counts0. Proofs scratch/concrete-b1-{jvm,native}.{zip,json}.
  Apple133032115202 linked the framework; framework is NOT an iOS app. Private APK14191125437.
- Added `SqliteGymTools` over existing gym_rest_preferences/gym_rest_timers/strength_executions.
  Reuses the SAME common GymRestTimer, clocks, defaults and transitions. Commands/once-only due
  acknowledgements validate unfinished workout ownership and stable trigger inside existing SQL
  transactions. Epoch interruptions never invent elapsed rest. Snapshot reads do not modify
  prescriptions, sets or accepted actuals. No second timer/execution engine or Android wiring change.
- `GymToolsPayloadJson` preserves original v1 envelope/preferences/exact Long clocks, strict field/
  version/type/bounds/unique timer IDs and required known execution references. Missing old payload
  defaults remain supported. No partial destructive native restore exposed.
- `StrengthProgrammingDecisionJson` preserves exact original v1 before/after owners, date nanos,
  protected IDs and substitution provenance; uses existing Java-compatible Unicode blank contract.
- `StrengthProgrammingArchiveJson` preserves v1/v2 complete authoring graph validation: normalized
  row bounds, legacy/authored collisions, known custom metadata, incomplete drafts, retained base
  revisions, independent concrete previews, activation chronology/lineage, decision chain, protected
  completed prescriptions, no orphan/duplicate activations and single current pointer. Custom
  authoring/logging definitions are checked separately before enclosing backup replacement. The
  enclosing legacy archive must be validated before supplying typed legacy context. Competing
  previews NEVER replace accepted history. No restore/activation added by these codecs.
- Added10 common realSQLite timer tests (JDBC +Apple wrappers),12 common authoring archive tests,
  and6 Android sameDB/original-codec parity tests in SharedGymToolsTest/SharedProgrammingArchiveTest.
  New Android cases included in focused experiment matrix and formatting targets. Existing tests
  unchanged. Intermediate new-test compilation errors (nullable Stored access and title argument)
  fixed directly; no domain/engine/assertion weakened.
- Local JVM **167storage tests PASS**,0failures/errors/skips, including all22new cases. Both Android
  APKs, instrumentation Kotlin compile and lint PASS (**5m31s/160tasks**). Full formatting
  **719sources (624Kotlin/95Java) PASS**; exact schema1–19 guard and75schema tests PASS.
  Final snapshot export uses one ordered timer query, preserving the original export cost.
  Logs: scratch/kmp-tools-archive-verified.log, kmp-tools-archive-format.log,
  kmp-tools-archive-schema.log. Exact pushed child runtime CI must still be inspected.
  Full root13 backup/archives/retention protections, typed authored/concrete/execution/association/
  strength-actual repositories, real common application root/native OS services and actual iOS app
  remain unfinished. Keep PR24 DRAFT; no version/schema/backup bump, merge or public publication.

### Checkpoint 65 — Normalised authored programme readers (16 November 2026)

- Checkpoint64 committed/pushed **6dc4a7a9e595275d798db4c0783f05793573ef58**. PR24 remains
  OPEN/DRAFT, accurate body updated. Formatting43816665144 SUCCESS. Experiment43816664754
  RUNNING: Android133130162731 pending. **Apple133130162793 SUCCESS**; digest-verified
  artifact14202873165 ZIP34457de5ef4881ea83fe4e07e49e2e6f67c2fe5527da22b24c0bf75f2b14da8f
  proves **449domain+18health+171storage Native**,0failures/errors/skips, all22new timer/archive
  cases ran. Proofs scratch/tools-6dc-native.{zip,json}. Framework14203426346 linked, NOT an iOS
  app. Expected474Android remains pending, not claimed passed. Final local64 build bothAPKs/lint PASS5m31s/160tasks,167storage JVM PASS,
  format719(624K/95J),schema75+exactsource guard PASS. APK0.12.3-test1/code19,min28,noINTERNET.
- Implemented records batch (include all files in this checkpoint commit): `SqliteStrengthProgrammingRecords.kt`, common
  `ProgrammingRecordsRepositoryContract.kt`, JDBC/Apple wrappers and3 additional Android cases
  appended to SharedProgrammingArchiveTest.kt (its preceding3 codec tests preserved).
- Reader composes existing SQLite19 tables: current legacy/authored reference, exact programme
  revision with indexed header/block/workout consistency, weekly draft, concrete draft with separate
  draft/programme revisions, bounded summaries, custom metadata and ordered decisions. Multi-query
  hydration shares one transaction. Read-only; no automatic activation, actual editing or restore.
  Use this reader in forthcoming command repositories rather than duplicating normalized loading.
- Added8 common realSQLite cases +3 Android parity cases using original normalized restore/writers.
  Initial local175tests had2 TEST FIXTURE failures: invalid SQL parent_revision9 and decision
  position0 violated existing CHECKs before reader assertions. Fixed to corrupt indexed start_date
  (legal SQL, invalid header identity) and original1-based decisions. No engine/schema/assertions
  weakened. Focused8 rerun PASS +Android instrumentation compile PASS (**2m40s/57tasks**),
  scratch/kmp-programming-records-focused.log. Full sharedStorage JVM **175PASS**,0failure/error/
  skip. Formatting **723sources (628Kotlin/95Java) PASS**, scratch/kmp-programming-records-format.log.
  Both APKs +lint PASS (**2m55s/160tasks**), scratch/kmp-programming-records-final.log.
- Next safe action: inspect checkpoint64 Android result/new6 actual instrumentation cases and
  digest-verified JVM XML. All65local gates PASS; commit/push the coherent reader batch without
  needlessly cancelling the almost-complete preceding runtime gate, then inspect its exact
  Native/JVM/Android results. Do not discard or redo checkpoint64.
  Full reviewed authored/concrete programme commands and source-validated execution/association/
  actual repositories, supported backups/retention, real app/native services/iOS host and final
  platform gates remain. Readers intentionally expose no mutation/activation/restore; future command
  repositories must compose these same normalized readers and recheck fresh evidence atomically.

### Checkpoint 66 — Draft commands, frozen repeat receipts and saved library

- Continuing `experiment/android-ios-kmp-compose` /existing OPEN/DRAFT PR24. Preserved all old
  source and the local verification note. HEAD/remote3405ed9; main85e1c39 unchanged. No version,
  schema, backup bump, merge or public experimental publication.
- Recovered65 **43818517597 FULL SUCCESS**, formatting43818517495 SUCCESS. Actual Android
  133135954441 logs: **477unique START/END**, allcomplete, all3 new normalized-record cases ran;
  **700189checks/48suites**. Digest-verified JVM14204490264
  ZIPb202f4d811677aee6ac7f5380a284bb11644d4013b77c13a6993f07881fc8873 proves
  **490domain+16health+175storage**. Native14204066359
  ZIPdff73862861f531da46f42eebd1d7f41e506f4405645d6b466b22e7a88d33c45 proves
  **449domain+18health+179storage**, all0failures/errors/skips. Proofs scratch/records-3405-
  {jvm,native}.{zip,json}. Framework14203449830 linked, NOT an iOS app; private APK14204305681.
- New `SqliteStrengthDrafts` commands compose completed normalized readers over SAME DB19:
  weekly save/delete with original forward-revision/lost-ack rules; custom save/logging edits keep
  typed metadata and legacy logging mirror atomic, reject stale/changed measurement tasks; concrete
  previews use independent sequential draft revisions, stable base/candidate identity, known
  legacy/authored/custom references, protected historical prescriptions, quota1000, row bounds and
  exact deletion guards. Child replacement/outer transaction/trigger failure rolls back fully.
  No acceptance/activation or actual set mutation. Existing Android repositories remain wired.
- `FrozenGymWorkoutRecordJson` ports original definition/source/sourceSnapshot row, ACTUAL vs
  EXECUTION repeat receipt, exact original/current fingerprint STRINGS, explicit edit/deletion
  dates, original slots and cleared load-reference rules. Source helper visibility only private→
  internal in GymExecutionSnapshotJson; the algorithms are unchanged. Archive concrete row-size
  validation extracted unchanged for reuse by command writer.
- CRITICAL: frozen receipt parsing is structural, NOT linked source approval. Do not enable native
  execution/finalization from this codec alone. Actual/programme source content still needs full
  transaction validation. New/edited native repeat fingerprints must retain the original Android
  CoachingJson UTF8 hash-input contract; semantically equivalent JSON is not permission to rehash
  retained strings with another serializer. No new native repeat proof generation/acknowledgement
  implemented here. Android parity tests re-encode retained receipts, then call ORIGINAL source
  validation before/after explicit actual edits/deletion.
- `SqliteGymWorkouts` reads indexed frozen rows/definitions, bounded favourites and preferences,
  and saves explicit preferences. Does not create workouts or executions or approve linked sources.
  Preference INSERT-vs-UPDATE semantics match original (avoid REPLACE for existing preferences).
- Added19 common realSQLite command tests,6 common frozen-receipt codec tests,6 common realSQLite
  library tests; JDBC/Apple wrappers;9 Android original-store/parity cases. Focused experiment and
  formatter targets include both new Android classes. Existing assertions/engines preserved.
  One transition-time library rollback assertion caught the initial upsert mismatch; UPDATE path
  and matching failed-update trigger now pass all6 tests. No weakening of rollback assertions.
- Latest local sharedStorage **206tests PASS**,0failures/errors/skips. Draft-only APKs/lint PASS
  5m33s/160tasks; frozen-codec source also bothAPKs/lint PASS5m33s/158tasks. FINAL source including
  library **bothAPKs +lint PASS6m28s/160tasks**, scratch/kmp-gym-durability-verified.log. Formatting
  **735sources (640Kotlin/95Java) PASS**, scratch/kmp-gym-durability-format.log. Exact schema
  guard +75tests PASS, scratch/kmp-drafts-frozen-record-schema.log. No known current JVM failure.
- All local checks PASS. Next: commit/push this coherent batch and update PR24 body; inspect its exact Android/Apple
  runtime CI and digest-verified XML (expected486Android/210storage Native NOT yet passed).
  Keep PR DRAFT. Continue reviewed authored programme activation and full source-validated gym
  execution/association/actual storage; then full supported backups/retention and actual common
  application/native services/iOS app. Do not enable partial native destructive restore/finalizer.

### Checkpoint 67 — Programme revision review snapshots

- Checkpoint66 committed/pushed **32929fb5f0a932d581dc75a70a6273c6e343d208**, PR24 OPEN/DRAFT
  confirmed; body updated. Experiment43833882273 RUNNING (Android133185561250 /
  Apple133185561688), formatting43833882276 SUCCESS. **Apple SUCCESS**: digest-verified
  artifact14210906075 ZIPa46144523653ff3ec3350319ca1aaa8b02b6ebcf7468363e96e8eaf5d6e44b35
  proves **449domain+18health+210storage Native**,0failures/errors/skips, all31new cases ran.
  Proofs scratch/durability-32929-native.{zip,json}; framework14210687440 linked, NOT an iOS app.
  Expected486Android remains pending, NOT claimed passed. All66local gates206storage JVM,735format,bothAPKs/lint,schema75 PASS.
- Implemented review batch in3 existing files: SqliteStrengthProgrammingRecords.reviewRevision,
  ProgrammingRecordsRepositoryContract (+3 common cases), SharedProgrammingArchiveTest (+1
  original-store Android case). Common reader returns one transactional programme/completion
  snapshot; it considers retained actuals linked across programme versions, intersects with known
  workouts, ignores dangling projections and refreshes after actual deletion. Historical snapshots
  remain immutable. Requires known AUTHORED reference. Does NOT activate/change programming.
- Uses exact original repository query and existing RevisionReview model. Acceptance must still
  recheck fresh completions inside its own write transaction; this snapshot is never authorisation.
- Local final **209storage JVM tests PASS**,0failures/errors/skips, bothAPKs+lint PASS
  **6m4s/160tasks**, scratch/kmp-programme-review-verified.log. Full formatting735sources
  (640Kotlin/95Java) PASS, scratch/kmp-programme-review-format.log. Expected213storage Native/
  487Android remain unverified until exact pushed child CI.
- Next safe action: inspect66 actual Android logs and digest-verified JVM XML; Apple verified
  above. All67local gates PASS; commit/push67 coherently without cancelling nearly-complete
  preceding CI. Preserve all local files/note. Continue full reviewed activation and source-validated
  execution/association/actual storage, backups/retention, real common app/native OS services and
  actual iOS app. No version/schema/backup bump, merge or public publication until full readiness.

### Final recovery boundary — checkpoint67 pushed; latest runtime gate pending

- Branch `experiment/android-ios-kmp-compose`, HEAD/remote
  **cb1b3ab9ce90e08ff52d67a43eb6935a77d935a8**. All source66/67 committed/pushed. PR24 remains
  OPEN/DRAFT at that exact head, body updated. No merge/public experimental publication/version/
  schema/backup change. Main85e1c39 /separate owner PR23 preserved.
- Latest experiment **43836146708 RUNNING**, Apple133193141940 /Android133193142314.
  Formatting **43836146806 SUCCESS**. Expected487Android/213storage Native are NOT verified.
  Latest local **209storage JVM tests PASS**,0failures/errors/skips, bothAPKs+lint PASS
  **6m4s/160tasks**,735sources(640Kotlin/95Java) formatting PASS. Logs scratch/kmp-programme-
  review-{verified,format}.log. No unresolved local compile/test failure; source66 schema75+
  exact1–19guard passed unchanged schema. Built APK0.12.3-test1/code19/noINTERNET. Do not publish
  a local/pre-commit binary as a verified current-source release.
- Parent **32929fb experiment43833882273 FULL SUCCESS**, formatting43833882276 SUCCESS.
  Android133185561250 logs prove **486unique START/END**, allcomplete; all9 new command/receipt/
  library parity cases ran (including original-source validation after shared encoding and explicit
  actual edits/deletion). **700189checks/48suites**. Actual CI builds/lint/schema passed.
  Digest-verified JVM14211165767
  ZIP2de5acff79c7b1b589543c1cc00cdc1a55687eed1c8e701c58efe4ba3c639665 proves
  **490domain+16health+206storage**. Native14210906075
  ZIPa46144523653ff3ec3350319ca1aaa8b02b6ebcf7468363e96e8eaf5d6e44b35 proves
  **449domain+18health+210storage**; all31 new common command/receipt/library cases ran.
  All XML failure/error/skip counts0. Proofs scratch/durability-32929-{jvm,native}.{zip,json}.
  Framework14210687440 linked, NOT an iOS app. Private APK14210264273, no public distribution.
- Next exact action: inspect43836146708 actual Android new revision-review case and digest-
  verified Native/JVM XML, fix real failures without weakening assertions. Then continue full
  reviewed authored/concrete activation and source-validated execution/association/actual stores.
  Compose existing SqliteStrengthDrafts/SqliteGymWorkouts/SqliteStrengthProgrammingRecords,
  source codecs and original domain engines; do not duplicate or restart these completed ports.
- Keep original source-content/actual-edit/frozen-repeat/atomic-finalization protections. Structural
  frozen receipt decode is NOT source approval. Retain original/current fingerprint strings exactly;
  native proof creation/edit acknowledgement still needs original Android hash-input compatibility,
  never a silent rehash with differently formatted JSON. Review snapshots must be rechecked inside
  the accepting write transaction together with current pointer and live-execution safeguards.
- Full root13/older backups, raw3/package1 and restore/retention safeguards remain; then real common
  application root/navigation/state/native OS services/permissions and REAL iOS host/app build.
  Full unfiltered Android28/35/36/iOSAPP/source-bound visuals/hardware remain pending. Native
  CI/macOS/Xcode WORKS; an unfinished app is not an infrastructure failure. No premature merge,
  publication, version bump, native destructive restore or partial finalizer.
- This final verification-only note is intentionally LOCAL to avoid another expensive docs-only CI
  push. Preserve/include with next coherent source checkpoint. All implementation is committed/
  pushed; only this recovery doc update is uncommitted.

### Checkpoint 68 — Reviewed activation and normalized execution reads

- Recovered/preserved all local work and fetched main85e1c39; existing PR24 OPEN/DRAFT,
  head cb1b3ab before this coherent source checkpoint. No replacement PR/reset/public artifact.
- Checkpoint67 experiment43836146708 and formatting43836146806 FULL SUCCESS, verified
  from GitHub. Android133193142314: **487 unique START/END**, including new revision-review
  parity case; retained **700189 checks/48 suites**. Digest-verified JVM14211123021
  SHA25614280f1072696812cdba7949fbc41e2b970aedfcf5ff0a22ab7dca3fc6779451 proves
  **490domain+16health+209storage**. Native14210973618
  SHA256828c36c09cda22774ec911c824df9fdf0780479381d52cad9e34e0d666e3582f proves
  **449domain+18health+213storage**. All XML failures/errors/skips0. Proofs
  scratch/review-cb1-{jvm,native}.{zip,json}. Framework14211731270 NOT an iOS app.
- New SqliteStrengthProgramming implements the COMPLETE existing interface, including reviewed
  weekly activation/forward revisions/stops, original eligibility/substitution engines, fresh
  completed-history review and live programme execution guard INSIDE the write transaction.
  Stable decision retries, normalized immutable programme children, decision/current pointer commit
  atomically. SqliteConcreteStrengthProgrammes composes that SAME acceptance transaction for
  saved concrete previews. Existing Android repositories remain wired, no actuals modified.
- New SqliteGymExecutionRecords reconstructs original normalized headers/sets/events, replay-validates
  them, verifies indexed summary/count/order, and keeps accepted edited actuals separate from
  original completion. Indexed unfinished/bounded finished-id queries avoid hydrating unrelated
  histories. No execution mutation/finalization/source approval implemented here.
- Added26 common realSQLite cases (10execution/16programme commands), JDBC/Apple wrappers,
  8original-store Android parity cases. Covers fresh completions, protected history, active-source
  guards, stale previews/custom metadata/exclusions, explicit actual edits, substitutions, decision
  retries, trigger/outer rollback and no-history rewriting. No engine/assertion weakening.
- Local **235 sharedStorage JVM tests PASS**, failures/errors/skips0. Both APKs +lint PASS
  **6m2s/160tasks**, scratch/kmp-strength-acceptance-verified.log. Full formatting **746sources
  (651Kotlin/95Java) PASS**, scratch/kmp-strength-acceptance-format.log. Exact schema1..19 guard
  +75schema tests PASS, scratch/kmp-strength-acceptance-schema.log. New runtime CI pending push:
  expected495Android/239storage Native are NOT claimed passed. Schema19/root13/raw3/package1,
  version0.12.3-test1/code19 unchanged. No UI/permission/dependency changes.
- Next: inspect this pushed checkpoint's actual Android/Apple runtime and digest-verified XML.
  Continue full source-validated execution commands/associations/accepted-actual stores, then
  supported backups/retention and actual common application/native services/iOS host/app. Shared
  programme activation is now implemented; do not restart it. Native actual finalization remains
  disabled until complete protections exist. Keep original Android CoachingJson UTF8 fingerprint
  input contract; structural receipts or copied hashes never authorize differently serialized proof.

### Checkpoint 69 — Explicit associations and archive graph validation

- Checkpoint68 committed/pushed **9323e363c0c3f0b5edac81d5dc7edc923e787609**. PR24 updated,
  remains OPEN/DRAFT. Formatting43842272255 SUCCESS. Experiment43842272316: Android
  133213618830 SUCCESS, **495unique START/END**, all8new parity cases ran; retained700189checks/
  48suites. JVM14214996067 ZIPa2622dcbde632a975494c64f575080a0202d08b1c4876ac5057eec5330ce5a44
  digest-verified **490domain+16health+235storage**,0failures/errors/skips; proof
  scratch/acceptance-9323-jvm.{zip,json}. PrivateAPK14214198054, not publicly released.
  Apple133213618440 FAILED common compilation at two
  LocalDate.isBefore calls (JVM-only java.time members). Root cause fixed by equivalent common
  toEpochDay comparisons in SqliteStrengthProgramming; no changed date bounds/calculations.
  Do not rerun failing source or claim checkpoint68 Native239 passed; validate corrected child.
- New SqliteGymAssociations implements COMPLETE existing GymAssociationRepository: exact
  legacy/authored discovery and review, explicit COUNT/KEEP choices, current-programme and fresh
  actual checks, occupied-target protection, stable retries, immutable choices and atomic coaching/
  indexed projections. Original completed executions/sets/programmes are never modified.
- GymAssociationArchiveJson ports original version1 association graph guards and canonical export
  shape: frozen historical actuals/prescriptions remain separate from current edits; complete current
  choices, unique target occupancy, exact links, row/root bounds and original domain comparability
  checks required. SqliteGymAssociations.archive is read-only and never invents a decision from a
  legacy projection. No destructive/native replacement or partial actual finalizer enabled.
- Added18 common realSQLite command/archive cases and JDBC/Apple wrappers;5 Android cases alternate
  original/shared commands, test stale/competing/edited actuals, rollback, and original backup roundtrip
  for both families. Existing assertions and engines preserved; focused workflow adds the new class.
- Focused18 JVM cases PASS (0failure/error/skip), scratch/kmp-gym-association-archive-focused.log.
  Initial new fixture violated existing FINISHED/accepted_actual CHECK when simulating unfinished
  data; replaced with a real domain-generated ACTIVE fixture, retaining assertions. New archive
  test generic inference error fixed explicitly. Final **253storage JVM tests PASS**,0failures/errors/
  skips. Common metadata +bothAPKs +lint PASS **5m37s/167tasks**, scratch/kmp-gym-association-
  verified.log. Full formatting **752sources(657Kotlin/95Java) PASS**, scratch/kmp-gym-association-
  format.log. Exact schema1..19 guard +75schema tests PASS, release-note10tests PASS. Built APK
  code19/version0.12.3-test1/min28/noINTERNET verified. No UI/engine/permission/dependency change.
  Common storage metadata compilation added to the existing JVM CI step to catch JVM-only API
  calls earlier; all existing focused Android classes retained, exactly one new class added.
- Next exact: finish local verification, commit/push69 together with portability fix and this note;
  inspect corrected child Android/Native runtime + digest-verified
  XML. Expected child500Android/257storage Native are NOT passed until actual proof. Continue complete
  source-validated execution commands and actual edit/deletion/frozen-repeat receipts; preserve
  original CoachingJson UTF8 fingerprint contract. Then full root13/older backups/raw3/package1,
  retention and real common root/native services/REAL iOS app. No public experimental release,
  schema/version bump, premature merge or native destructive restore/finalizer.

### Latest recovery boundary — checkpoint69 pushed, corrected runtime verification pending

- Branch `experiment/android-ios-kmp-compose`; HEAD/remote **a42e5ee2f6089ec6903c95e4c1321a68831735d2**.
  Checkpoint68/69 source is committed/pushed, PR24 OPEN/DRAFT confirmed at this exact head and body
  updated. No implementation remains only locally. This verification note is intentionally local
  for the next coherent source checkpoint, avoiding another expensive docs-only CI push.
- Corrected experiment **43845048753 RUNNING**: Android133222990921 /Apple133222991304.
  Formatting **43845048657 SUCCESS**. Actual current Android CI passed common metadata +JVM/
  retained domain suites +schema gates; build/runtime pending. Apple simulator Native/framework
  step running, not claimed passed. Expected500Android/257storage Native need actual logs and
  digest-verified XML. Root68 Apple compile failure was fixed in a42e5ee, no blind rerun.
- Local current-source verification COMPLETE:253storage JVM tests, common metadata, bothAPKs/lint
  (5m37s/167tasks),752formatting sources,75schema tests/exact schema1..19,10release-note tests PASS.
  APK version0.12.3-test1/code19/min28/noINTERNET. Preceding9323e36 Android495unique START/END/
  700189checks/48suites and235storage JVM verified; Native failed compilation, never claimed passed.
- Next exact action: inspect43845048753 jobs/new Android parity/backup cases and digest-verified
  JVM/Native results, fix real failures without weakening safeguards. Continue FULL source-validated
  execution start/save/atomic finish and accepted-actual edit/deletion storage, preserving original
  CoachingJson UTF8 fingerprint/hash-input compatibility and explicit repeat edit/deletion receipts.
  Associations, programme activation, normalized readers and association archive guards are done;
  do not restart them. All original Android stores remain wired until full replacements are safe.
- Then complete root13 supported older/current backups/raw3/package1 and retention/pruning guards,
  common root/navigation/state/services, actual native permissions/HealthKit/recording/BLE/
  notifications and REAL iOS app host/build/runtime. Native framework is NOT an app. Working
  GitHub macOS/Xcode is available; incomplete app code is not an infrastructure blocker. Full
  Android28/35/36/iOS app/canonical visuals and physical acceptance remain pending. No version or
  schema/backup bump, public artifact publication, premature merge or partial native finalizer.

### Checkpoint 70 — Preserve nested large association history

- New LOCAL source after pushed a42e5ee: GymAssociationArchiveJson, GymAssociationCommandsContract,
  SharedGymAssociationCommandsTest andsharedStorage/build.gradle plus this recovery note. All
  local checks complete; commit/push this coherent checkpoint before native runtime verification.
- Focused review demonstrated a boundary mismatch: original full BackupManager allows32MiB,
  while new association graph validation serialized its entire nested archive through the standalone
  GymStoredJson16MiB reader. This could reject valid larger retained choice histories before native
  restore is enabled. Original Android behaviour has NOT changed.
- Added element(Archive)/decode(JsonObject) for embedding only inside an enclosing syntax/size-
  validated backup; typed graph validation retains all100000row/524288byte decision bounds and
  exact evidence/link/occupancy checks without a second standalone aggregate cap. Standalone String
  encode/decode keeps its existing16MiB guard. No complete parent-backup validator or native restore
  is claimed; no destructive command enabled.
- Added common1800decision synthetic history >16MiB/<32MiB, exact all-choice decode/graph preservation,
  standalone rejection and retained per-field limits. Focused single JVM case PASS,0failures/errors/
  skips, scratch/kmp-large-association-focused.log (54s build,2.152s test). Added Android parity case
  passes that same large nested DOM to ORIGINAL association backup validator; runtime still pending.
- Current-source254storage JVM tests PASS,0failures/errors/skips; full752-source formatting PASS.
  First full build hit a public-API classpath error: new JsonObject parent entry point exposed an
  existing implementation-only serialization dependency to Android callers. Changed its SAME1.7.3
  dependency to api (no new library/version/runtime dependency). Corrected common metadata/bothAPKs/
  lint PASS **5m40s/167tasks**, scratch/kmp-large-association-api-verified.log; preceding failed
  log retained at scratch/kmp-large-association-verified.log. Full254storage JVM0fail/error/skip
  verified from XML.752format PASS; schema75/exact1..19 andrelease10 PASS unchanged from69.
  Native258/Android501 remain pending until corrected pushed-head CI, not claimed passed.
- Checkpoint69 a42e5ee experiment43845048753 and formatting43845048657 FULL SUCCESS. Android
  133222990921:500unique START/END, all5new association/backup parity cases ran,700189checks/48suites.
  JVM14216475216 SHA2564b2b01f05b0752dc449ed5b302657f3a033ac6c0a19a480d9e501d52bd7500bb:
  490domain+16health+253storage. Native14216015518
  SHA2563393ca17c3ec6d53c84f5c7281fcb701ac9e1cea8f78d1dff3be90cb8ccd77b8:449domain+18health+
  257storage, all18new association and26activation/execution cases ran. AllXML0failure/error/skip.
  Proofs scratch/associations-a42-jvm/verification.json and associations-a42-native.{zip,json}.
  Framework14215980640 NOT iOS app; privateAPK14216465364. Root68 native date issue VERIFIED fixed.
- Next exact: all70 local checks PASS; commit/push coherently, preserve true69 green results, then inspect its exact Native/
  Android cases and artifact digests. Preserve previous local verification note with next commit.
  Keep PR24 DRAFT and update its body. No version/schema/backup/public release change.

### Final recovery boundary — checkpoint70 pushed; exact-head runtime in progress

- Branch `experiment/android-ios-kmp-compose`, HEAD/remote
  **2803c741d86a10d7d4008f22606a1a95c18534cb**. Checkpoints68/69/70 source ALL committed/pushed;
  PR24 OPEN/DRAFT confirmed at this exact head and body updated. Main85e1c39 /owner PR23 untouched.
  No public experimental release, version/schema/backup change or merge. Only this final CI/status
  note is intentionally local; include it with next meaningful source checkpoint, avoid docs-only CI.
- Latest experiment **43848459269 RUNNING**: Android133234562163 /Apple133234562359.
  Formatting **43848459362 SUCCESS**. Expected501focused Android/258storage Native remain pending,
  NOT claimed passed. Inspect actual logs/artifacts before continuing; diagnose real failures.
- All70 local gates COMPLETE:254storage JVM0failures/errors/skips, common metadata +bothAPKs/lint
  **5m40s/167tasks** (scratch/kmp-large-association-api-verified.log),752source formatting PASS
  (657Kotlin/95Java). Schema75/exact1..19 andrelease10 passed69 and remain unchanged. The initial
  70Android JsonObject classpath failure is fixed via SAME serialization1.7.3 implementation→api;
  corrected APKs/instrumentation compilation/lint passed. No extra runtime library added.
- Parent a42e5ee experiment43845048753 and format43845048657 FULL SUCCESS:500unique Android
  START/END, all5new association/backup cases ran;700189checks/48suites. Digest-verified JVM
  14216475216 SHA2564b2b01f05b0752dc449ed5b302657f3a033ac6c0a19a480d9e501d52bd7500bb:
  490domain+16health+253storage. Native14216015518
  SHA2563393ca17c3ec6d53c84f5c7281fcb701ac9e1cea8f78d1dff3be90cb8ccd77b8:449domain+18health+
  257storage. All0failure/error/skip. All18association/10execution-read/16programme-command cases
  explicitly matched against Native testcase names. Proofs scratch/associations-a42-jvm/
  verification.json and associations-a42-native.{zip,json}. Framework14215980640 NOT iOS app;
  private APK14216465364. Original68 Apple date compilation failure is VERIFIED FIXED in69.
- Next exact actions: inspect43848459269 Android large-history ORIGINAL-validator parity case and
  new Native1800decision case plus digest-verified JVM/Native XML. Preserve completed source and
  standalone-vs-parent backup limits. Native actual finalizer/destructive restore still disabled.
  Continue FULL retained-source validated execution start/save/atomic finish, actual edit/deletion
  and repeat receipts; original Android CoachingJson UTF8 fingerprint/hash-input must match exactly,
  never rehash retained receipts using a semantically equivalent differently formatted serializer.
- Then complete supported root13/older/current backups/raw3/package1, parent syntax/export budgets,
  retention/pruning restore guards. When wiring full backup normalization, use validated-parent DOM
  entry points for nested archives; review the older programming-archive standalone16MiB path too,
  rather than imposing it on a supported32MiB whole backup. Do not narrow historical compatibility.
  Programme activation, association commands/graphs and normalized readers are completed; reuse them.
- Remaining full scope: real common root/navigation/state/data composition, native permissions/full
  HealthKit/recording/BLE/notifications, REAL iOS app host/entitlements/app build/runtime; final full
  Android28/35/36/iOS APP/source-bound captures/light/dark/200% gates. Native/macOS/Xcode works;
  unfinished app code is not an infrastructure blocker. No new version/publication/merge until all
  ready. Physical HealthKit/Health Connect/GPS/BLE/audio/locked-screen/OEM/TalkBack unperformed.

### Checkpoint 71 — Shared execution commands and source-validated actual transactions

- Coherent source batch: SqliteGymExecutions.kt, SqliteGymSources.kt, common
  GymExecutionCommandsContract.kt +JDBC/Apple wrappers, SharedGymExecutionCommandsTest.kt,
  formatting/workflow entries and this handoff. Prior source2803c74 remains committed/pushed.
- SqliteGymExecutions implements complete existing GymExecutionRepository, composes original
  readers/library and SAME SQLite19 transaction: start/retry/one-unfinished, append-only state/
  sets/events/revision guards, source revalidation, atomic finished actual +exact planned link,
  preferences, indexed recent actuals, explicit accepted-actual edits/deletion and repeat receipts.
  All original Android stores remain wired. No native finalizer/UI is enabled yet.
- SqliteGymSources validates exact retained legacy/authored/saved/actual/execution sources via
  ORIGINAL shared domain validators; immutable frozen receipts retain original slots/fingerprints,
  clear load references, and acknowledge explicit edit/deletion. Source removal never fabricates
  past sets; frozen repeat remains usable. Reused deleted IDs require a validated restore.
- REQUIRED LegacyStrengthFingerprint constructor dependency has NO default/fallback: it must hash
  ORIGINAL Android CoachingJson.strength(actual).toString UTF8 bytes, not the shared JSON codec's
  different number/escaping spelling. Android parity uses REAL original serializer/SHA256.
  Common/JDBC/Native command tests use an explicitly test-only registered-fixture lookup, failing
  closed for unknown fixtures. That does NOT verify a native production hash implementation.
  A real compatible native provider is STILL REQUIRED before enabling iOS execution; do not wire
  fixture hashes or generic JsonObject.toString into a host. No claimed native end-to-end readiness.
- Added21 common transaction/source/rollback cases +6 Android original-store/source/backup cases.
  All21 new cases PASS; full **275storage JVM tests PASS**,0failures/errors/skips. Final common
  metadata/bothAPKs/lint PASS **6m12s/167tasks**, scratch/kmp-execution-commands-verified.log.
  Full **758sources(663Kotlin/95Java) formatting PASS**, scratch/kmp-execution-commands-format.log;
  exact schema1..19 guard +75schema tests PASS, scratch/kmp-execution-commands-schema.log.
  Earlier compile/common-metadata/Android-test compilation PASS3m12s/64tasks after20case pass.
  No known local build/test failure remains; new Native/runtime cases still need pushed CI.
  Initial new fixture deletion violated original legacy-pointer FK; disable pointer before removal.
  Reused-deleted-source fixture now asserts fail-closed rather than writing an inconsistent receipt.
  Guards also reject ignored UPDATEs and roll back accepted actual/receipt together.
- Checkpoint70 **FULL SUCCESS**: experiment43848459269 /format43848459362. Android133234562163:
  **501unique START/END**, new >16MiB ORIGINAL-validator case ran;700189checks/48suites.
  Digest-verified JVM14217277917 ZIPdd33ef542f35c8d2b499515f5ebe797beadd46892549e25d94648fe46be33f90:
  490domain+16health+254storage. Native14217238381
  ZIPcb1e663c93c9734988541b002a31488503bc393de7af75a2efa454a49b5a53bd:
  449domain+18health+258storage; Native1800decision case explicitly matched against XML. All0fail/
  error/skip. Proofs scratch/large-history-2803-results/{verification.json,*test-results.zip}.
  Framework14217078806 is NOT an iOS app; privateAPK14217550824, no public build.
- Next exact: all71 local checks COMPLETE/PASS; commit/push this coherent batch including prior
  local handoff, PR24 DRAFT, inspect actual new runtime cases
  (expected507Android/279storage Native NOT verified). Then native legacy fingerprint compatibility,
  full backup/retention commands and real common application/native services/iOS host/app. No version/
  schema/backup bump, public publication or merge. Preserve0Java-domain completion and shared UI.

### Checkpoint 72 — Parent-budget programme archive entry points

- Checkpoint71 committed/pushed **a5d2cf4f79f0dbb754469832af675865f1c537ec**; PR24 OPEN/DRAFT
  confirmed and body updated. Experiment43852648767 and format43852648918 FULL SUCCESS.
  Android133248865390:507unique START/END, all6new source/finalization/edit/delete/backup cases ran
  with REAL original Android serializer/SHA256;700189checks/48suites. JVM14219368344
  ZIP348c06c5c7e45c53e080568cfb5ffc05a43db4fdf5edff1085919540cd476da4:490domain+16health+275storage.
  Native14220336386 ZIP2ce4507be7eef8fe58044781d365ba6e7737d9939f99c501710c0def76ec6888:
  449domain+18health+279storage; all21 new command cases matched against Native XML. All0fail/
  error/skip. Native tests use TEST-ONLY fingerprint lookup, NOT native production hash proof.
  Proofs scratch/execution-a5d2-{jvm,native}/verification.json. Framework14221220755 is NOT an
  iOS app; privateAPK14219782764. No public experimental publication.
- Coherent source72: StrengthProgrammingArchiveJson.kt, StrengthProgrammingArchiveJsonTest.kt,
  SharedProgrammingArchiveTest.kt, plus recovered status notes. Preserve all work; do not restart71.
- Existing String decode delegates to SAME full programme graph guards through new JsonObject
  overload. New element(Snapshot) permits nesting under an already syntax/size-validated parent
  backup without a second16MiB cap. Standalone encode/decode remains16MiB bounded; all per-row,
  version, legacy context, lineage, current-pointer and history checks unchanged. No native restore
  or partial parent-backup validator enabled; this is a required piece of future complete backups.
- Added2 common cases: synthetic800retained programmes/800explicit decisions/8000workouts >16MiB
  and<32MiB, all preserved, plus nested invalid-version/orphan/current/unknown-field rejection.
  Added Android same-large-DOM parity with ORIGINAL StrengthProgrammingArchive validator.
- Focused programming archive suite PASS, scratch/kmp-nested-programming-focused.log (56s build).
  Current **277storage JVM tests PASS**,0failures/errors/skips. Common metadata/bothAPKs/lint PASS
  **6m1s/167tasks**, scratch/kmp-nested-programming-verified.log. **758-source formatting PASS** (663Kotlin/95Java),
  scratch/kmp-nested-programming-format.log. No schema/backup/version/dependency/UI/engine change.
- Next exact: all local72 gates PASS and71 CI fully verified. Commit/push72 coherently; inspect
  its exact Android/Native runtime and digest-verified artifacts, diagnose any real failures. Expected
  child508focused Android/281storage Native are not passed until proof. Keep PR24 DRAFT.
- Native compatible LegacyStrengthFingerprint provider and host wiring STILL REQUIRED; fixture
  lookup in common tests must never be shipped. No native end-to-end finalization claimed. Complete
  remaining full root13/older backups/raw3/package1/retention safeguards and actual common app/
  OS services/iOS host/build afterwards; no premature merge, public release or version bump.

### Final recovery boundary — checkpoint72 pushed; exact-head runtime pending

- Continue branch `experiment/android-ios-kmp-compose`, HEAD/remote
  **8941d64a5371932bf0bba35d9b1d15fb74306d1d**. Source71(a5d2cf4) and72 ALL committed/pushed;
  PR24 OPEN/DRAFT confirmed at this exact head, body updated. Main85e1c39 /owner PR23 preserved.
  No public experimental publication, merge or version/schema/backup bump. Only this final CI/status
  note intentionally remains local for next coherent source checkpoint; avoid docs-only CI push.
- Latest experiment **43855254049 RUNNING**; formatting **43855254014 SUCCESS**.
  Expected508focused Android/281storage Native NOT passed until actual logs/digest-verified XML.
  Next exact: inspect these jobs, including new Android800programme ORIGINAL-validator case and
  both new Native programme-archive cases; fix real failures without weakening safeguards/reruns.
- All current72 local checks PASS:277storage JVM0failure/error/skip, common metadata +bothAPKs +lint
  **6m1s/167tasks** (scratch/kmp-nested-programming-verified.log),758source formatting
  (663Kotlin/95Java), focused14programme-archive tests including1.744s large-history test. Schema75/
  exact1..19 passed71 and unchanged. Built APK0.12.3-test1/code19/min28/target35/noINTERNET verified.
- Parent71 **FULL SUCCESS**, experiment43852648767 /format43852648918.507unique Android START/
  END, all6new real-original-hash source/staging/finalization/edit/delete/rollback/backup cases ran;
  700189checks/48suites. Digest-verified JVM14219368344
  SHA256348c06c5c7e45c53e080568cfb5ffc05a43db4fdf5edff1085919540cd476da4:
  490domain+16health+275storage. Native14220336386
  SHA2562ce4507be7eef8fe58044781d365ba6e7737d9939f99c501710c0def76ec6888:
  449domain+18health+279storage; all21 command cases matched against XML. All0failure/error/skip.
  Proofs scratch/execution-a5d2-{jvm,native}/verification.json. Native used TEST-ONLY known-fixture
  fingerprint provider for transaction logic; this is NOT native production hashing verification.
  Framework14221220755 NOT iOS app; privateAPK14219782764. No public experimental artifacts.
- Completed72 extends only nested programme archive entry points; existing String16MiB bound,
  all per-row/graph/current/context/lineage checks and immutable versions remain. Parent32MiB
  budget belongs to enclosing validated backup. Full root13/older external backup normalization,
  raw3/package1, restore/retention/pruning safety remains incomplete; no native destructive command.
- Next implementation: implement/verify a REAL compatible Native LegacyStrengthFingerprint provider,
  then wire the completed SqliteGymExecutions/source/actual commands. It must hash ORIGINAL Android
  CoachingJson.strength(actual).toString UTF8 bytes, including number spelling, field order, escaping,
  nulls and Java UTF8 handling; do not hash another serializer or ship fixture/constant hashes.
  All Android production stores remain wired until safe replacements are complete. Shared execution
  transactions, source validation, actual edits/deletions, repeat receipts, programme activation,
  associations/readers/graphs are DONE; reuse them rather than restarting their implementation.
- Remaining full scope: full backup/retention safeguards and common application root/navigation/
  state/data composition, native permissions/full HealthKit/recording/BLE/notifications, REAL iOS
  app host/entitlements/build/runtime. Then final full Android28/35/36/iOS APP/source-bound visuals/
  light/dark/200% gates. Mac/Xcode CI works; an unfinished app is not an infrastructure blocker.
  Physical HealthKit/Health Connect/GPS/BLE/audio/locked-screen/OEM/TalkBack still unperformed.

### Checkpoint 73 — Real portable legacy strength evidence fingerprints

- Preserved all checkpoint72 work and the local final-status note. Branch remains
  `experiment/android-ios-kmp-compose`, existing PR24 DRAFT; live main85e1c39 and owner PR23 untouched.
- Added PortableLegacyStrengthFingerprint: ORIGINAL CoachingJson field order, Android JSON escaping,
  nulls, integer/negative-zero spelling and Java UTF8 isolated-surrogate replacement. JVM uses system
  MessageDigest; Apple uses system CoreCrypto CC_SHA256. No runtime dependency/schema/backup bump.
- LegacyStrengthNumbers implements bounded exact-binary decimal rendering for actual measurements,
  avoiding Native Double.toString differences. Four JVM oracle cases compare against independent
  java.lang.Double formatting, including100000 everyday values,2048 subnormals, all supported binary
  exponent neighbours and seeded bit patterns. Native corpus golden comes from that independent
  Java oracle, not the provider. No measurements are rounded or rewritten.
- Six common provider cases cover real SHA standard vectors, all modalities, fractional RPE/RIR,
  nulls, precise clocks, escaping/Unicode and legacy numeric corpus. ALL21 existing execution
  transaction/source/edit/delete/rollback cases now use the real production provider instead of
  test-only fixture lookup. New Android SharedLegacyFingerprintTest has3 cases comparing byte input
  and hashes DIRECTLY with original CoachingJson, numeric round-trip bits and immutable receipt
  strings; all6 SharedGymExecutionCommandsTest cases now use the real provider too.
- Current full287storage JVM tests PASS,0failure/error/skip. Formatting766sources(671Kotlin/95Java)
  PASS. Exact schema1..19 +75schema tests PASS. Common metadata/bothAPKs/lint PASS
  **6m24s/167tasks**, scratch/kmp-legacy-provider-verified.log. APKcode19/min28/target35/noINTERNET checked.
  New Android/Native runtime checks await coherent push; expected511Android/287storage Native are
  NOT yet verified. Original Android stores remain wired; no native host/finalizer UI enabled.
- Checkpoint72 experiment43855254049 +format43855254014 FULL SUCCESS:508unique Android START/END,
  new800programme ORIGINAL-validator parity case ran;700189checks/48suites. Digest-verified JVM
  14220304883 SHA256d2973621d263eb4350201aec85cf8ec29614b73639147bf0b4337ba9b8372b58:
  490domain+16health+277storage. Native14221776525
  SHA2566adeec14a9730a3a3342bc2cd5d491090ca956b6f7bccabf155bc3aa500d9dbd:
  449domain+18health+281storage; both new nested-programme cases matched against Native XML.
  All0failure/error/skip. Proof scratch/programming-8941-results/verification.json.
  Framework14221751496 NOT iOS app; privateAPK14221364430. No public publication/merge.
- Next exact: all local73 gates PASS. Commit/push coherent batch including prior local handoff,
  keep PR24 DRAFT. Verify real Android serializer parity +Native system SHA/goldens and all21
  execution cases against exact-head logs/digest-verified XML. Fix genuine differences, never
  substitute fixture hashes. Then continue full root13/older/current backup/raw3/package1 and
  retention safeguards; real common composition/native services/iOS app remain unfinished.

### Checkpoint 74 — Retention consent and removed-detail backup metadata

- Checkpoint73 committed/pushed **74ad42cb1632595f158a06ad7d9ed12c5f6ba7f9**; PR24 OPEN/DRAFT
  body updated. Exact-head experiment43860039026 +format43860039120 FULL SUCCESS.
  Android133274150702:511unique START/END, all3new original serializer/number/receipt cases and
  all6real-provider execution cases ran;700189checks/48suites. JVM14224670719
  SHA256612794c8d7a6aa79853d561344d19973b5d20780c85fcc7bcdd7455db83eddfe:
  490domain+16health+287storage,0failure/error/skip, digest verified.
  Proof scratch/fingerprint-74ad-jvm/verification.json; privateAPK14223283352.
  Native14223347708 SHA256d0fc8c2a0505a27100ab844038bbc8ea93495da65be617b74ea76bb70d793518
  digest-verified449domain+18health+287storage,0failure/error/skip. All6 REAL provider goldens and
  all21 execution command cases matched against Native iosX64 XML. Proof scratch/fingerprint-74ad-native/
  verification.json. Framework14222909455 NOT iOS app. Main85e1c39/owner PR23 unchanged.
- Added DetailRetentionArchiveJson: original retention v1 fields, all three policies and choice
  identities, null historic consent, exact monotonic Longs/times and original removal hashes.
  Missing older sections return EXISTING_UNREVIEWED/KEEP_FOREVER without fabricated consent.
  Immutable snapshots reject duplicate session/activity IDs and>100000receipts, never truncate.
  Parent backup owns its total32MiB syntax/export budget; no second16MiB gym limit is imposed.
- SqliteRecordingDetailReceipts.snapshot reads consent and ordered removal receipts under SAME
  SQLite transaction with100001-row overflow detection. Existing single-receipt reader reused.
  Export changes no summaries, PBs, detailed evidence or preferences. No native prune/restore API
  or consent mutation was introduced. Original Android stores remain wired and unchanged.
- Added7 common archive guard/roundtrip cases +2 real SQLite history/no-write/fail-closed cases
  in ActivityRepositoryContract (run on JVM and Apple), plus2 Android ORIGINAL-validator/export
  parity cases. Android cases include every policy, immutable historic hashes, invalid metadata
  and protected older consent. Enclosing full backup must STILL validate activity/source references
  and compatible raw evidence before transactional restore; section decoding alone is not approval.
- Focused7archive tests and25JDBCactivity cases PASS,0failures/errors/skips; Android instrumentation
  compilation PASS **3m26s/57tasks**, scratch/kmp-retention-archive-corrected-focused.log.
  Initial test compilation used nonexistent saveActivity; corrected to existing upsertManualActivity
  API and reran affected cases. No production API or assertion was weakened.
  Formatting **769sources(674Kotlin/95Java) PASS**, scratch/kmp-retention-archive-format.log.
  Full **296storage JVM tests PASS**,0failure/error/skip. Common metadata/bothAPKs/lint PASS
  **2m49s/167tasks**, scratch/kmp-retention-archive-final.log. Release-note10 PASS.
  Source provenance now includes new sharedStorage/src/jvmMain crypto implementation;3new
  coverage checks verify ALL actual shared production source sets/builds and exact-byte fingerprint
  inputs. Added to experiment CI. No screenshots have been regenerated or claimed verified.
- Next exact: all74 local gates PASS;73 Android/Native/JVM runtime fully verified.
  Commit/push74 coherently, preserving all
  earlier source. Expected296storage Native and513focused Android remain pending until actual CI;
  current296storage JVM are verified locally. Keep PR24 DRAFT; no merge/version/schema/backup/
  public release change. Continue full root13/older/current backup/raw3/package1 restore/retention
  safeguards, then common app composition/native services/actual iOS host/app and final gates.

### Checkpoint 75 — Portable supported root-backup envelope safety

- Checkpoint74 committed/pushed **fb758eaa3e62487f713c0a7127c6489b3bf3c7ca**; PR24 OPEN/DRAFT
  body updated. Experiment43862290924 RUNNING: Android133281900366 /Apple133281899719.
  Format43862290879 SUCCESS. Android remains pending. Native14225240067
  SHA256109d8e1fa1f6c94fb3ada0039ca376e3750feb5a7384fb76bc73b7e04a8373b5:
  digest-verified449domain+18health+296storage,0failure/error/skip. All9 new retention cases
  matched against Native XML. Proof scratch/retention-fb758-native/verification.json.
  Framework14224627987 NOT iOS app. Expected513focused Android NOT yet verified.
  Checkpoint73 FULL SUCCESS511Android/287storage JVM/287storage Native as documented above.
- Added BackupEnvelopeJson with supported root1–13 format/version/date/required-section checks,
  strict UTF8 (including original leading BOM support), one complete document and duplicate-key
  rejection. Original root32MiB/depth32/2million structural-token/1MiB serialized-text budgets
  remain distinct from standalone gym16MiB/depth20/1million-value/100000-text limits.
  Existing strict scanner extracted to StoredJsonSyntax; original gym defaults/errors preserved.
- Both inspection and export REQUIRE a caller-supplied COMPLETE snapshot validator; there is no
  partial-validation default, replacement operation or native destructive restore. Complete shared
  parent data/graph normalization and raw3/package1/retention reconciliation STILL remain needed.
  Android production BackupManager/stores remain wired; original full validator is used only as
  actual parity oracle here. This does not claim a complete native backup/restore implementation.
- Added11 common envelope cases: all13read versions, section introduction boundaries, required
  caller validation/fail-closed export, precise Longs and text, corrupt/future/duplicate/trailing/
  malformed UTF8 rejection, BOM, original allocation limits and>16MiB/<32MiB syntax without loss.
  Synthetic envelope-only data is explicitly NOT a substitute for complete typed row validation.
  Added2 Android cases against REAL original BackupManager/SqliteBackupRepository across versions
  1–13, both export directions, BOM and corrupt full-row failures without database replacement.
- Focused11envelope +10existing frozen-workout JSON tests PASS,0failure/error/skip; Android test
  compilation PASS **3m19s/57tasks**, scratch/kmp-backup-envelope-focused.log.
  Full307storage JVM tests PASS,0failure/error/skip. Common metadata/bothAPKs/lint PASS
  **6m21s/167tasks**, scratch/kmp-backup-envelope-final.log. Exact schema1..19,75schema tests,
  release-note10 and3source-provenance tests PASS, scratch/kmp-backup-envelope-safety.log.
  Formatting773sources(678Kotlin/95Java)
  and3provenance checks PASS, scratch/kmp-backup-envelope-{format-final,provenance}.log.
- Next exact: all75 local gates PASS. Inspect74 exact-head Android receipt/consent cases and
  digest-verified JVM XML before a new push (avoid cancelling its running gate); Native is verified.
  Commit/push75 coherent batch, keep PR24 DRAFT and inspect its exact-head Native/Android root
  backup cases. No version/schema/backup format/publication/merge. Then complete full parent
  snapshot normalization/transactional restore/raw archives/retention guards. Port the COMPLETE
  SqliteBackupRepository.validated graph plus CoachingStore/DevelopmentStore/SupportStore/Release08
  validation using completed shared codecs; never enable native replacement with a partial callback.
  Common root/navigation/state/native services/actual iOS app and final gates remain unfinished.

### Final recovery boundary — checkpoint75 pushed; exact-head runtime pending

- Continue `experiment/android-ios-kmp-compose`, HEAD/remote
  **55d6f160947cdf2fa06254d2768feebd58bb5b94**. Checkpoints73(74ad42c),74(fb758ea),75 ALL
  committed/pushed; PR24 OPEN/DRAFT confirmed at55d6f16, body updated. Main85e1c39/owner PR23
  untouched. No version/schema/backup bump, merge, stable tag or public experimental publication.
  Only this final CI/status note remains intentionally local; include with next source checkpoint,
  avoiding another docs-only CI push. Preserve it if the workspace survives.
- Latest exact-head PR experiment **43865551237 RUNNING**: Android133293046630 /Apple133293047353.
  Formatting43865551231 SUCCESS. Expected515focused Android/307storage Native NOT verified yet.
  Connector briefly returned no runs after push; manual43865680449 was dispatched, then explicitly
  CANCELLED as soon as the real PR run appeared, to avoid duplicate work. This was not a failed-test
  rerun. Inspect the actual PR run43865551237, not the cancelled duplicate; never claim pending tests.
- All75 local gates PASS:307storage JVM0failure/error/skip,11envelope+10frozen-workout focused cases,
  common metadata/bothAPKs/lint **6m21s/167tasks**,773source formatting(678Kotlin/95Java), exact
  schema1..19/75schema tests/release-note10/provenance3. Logs scratch/kmp-backup-envelope-
  {focused,final,format-final,safety,provenance}.log. APK0.12.3-test1/code19/min28/target35/noINTERNET
  checked. No source/UI regression is known locally; new Native/Android envelope runtime is pending.
- Parent74 experiment43862290924 /format43862290879 FULL SUCCESS:513unique Android START/END,
  both new ORIGINAL retention-validator/export cases ran;700189checks/48suites and3source-provenance
  guards passed. Digest-verified JVM14225221225
  SHA256b5687dcc237ac5127a349a3d4d6d47743056bd6c7119e2aa1db1e63333c57a9d:
  490domain+16health+296storage. Native14225240067
  SHA256109d8e1fa1f6c94fb3ada0039ca376e3750feb5a7384fb76bc73b7e04a8373b5:
  449domain+18health+296storage. All0failure/error/skip; all9retention cases matched against Native
  XML. Proofs scratch/retention-fb758-{jvm,native}/verification.json. Framework14224627987 NOT
  iOS app; privateAPK14225281224. Real original fingerprint provider73 is fully verified too.
- Next exact: inspect75 PR CI, including both Android SharedBackupEnvelopeTest cases using REAL
  original whole-data validation and all11 Native envelope cases; digest-verify JVM/Native ZIP/XML.
  Diagnose real failures without weakening assertions or blindly rerunning. Preserve all completed
  Java-domain conversion, UI, typed storage, source validation, associations, execution and hashes.
- Then finish COMPLETE parent user-data/graph normalization from SqliteBackupRepository.validated
  and CoachingStore/DevelopmentStore/SupportStore/Release08 validators using existing shared codecs.
  The mandatory callback in BackupEnvelopeJson is NOT a supplied native full validator. Full raw3/
  package1 export/restore, active/recoverable recording protection, compatible archived-detail and
  retention/pruning transaction safeguards remain unfinished; native destructive restore stays disabled.
- Remaining full product work: real common app composition/root/navigation/state/data wiring, native
  permissions/full HealthKit/recording/BLE/notifications, REAL iOS host/entitlements/app build/runtime,
  final unfiltered Android28/35/36 +iOS APP and source-bound light/dark/200% visuals. Working macOS/
  Xcode CI is available; incomplete app code is not an infrastructure blocker. Physical HealthKit/
  Health Connect/GPS/BLE/audio/locked-screen/OEM/human TalkBack remains unperformed.

### Checkpoint 76 — Coaching archive graph and bounded SQLite export

- Preserve experiment/android-ios-kmp-compose /DRAFT PR24 and all75 source/local final recovery notes.
  Main85e1c39 /owner PR23 unchanged; no version/schema/backup/publication change.
- Added CoachingArchiveJson: original coaching-v1 table/column rules and graph references across
  plan versions/current, actual strength sessions/exercises, responses, proposals/decisions and
  feedback. Duplicate identities/numbers, missing prescriptions/versions, mismatched metadata,
  invalid decisions and unsupported feedback rule/score bounds fail before use.
- Immutable typed views preserve EXACT original SQL payload strings and NULLs, including legacy
  feedback components/extra fields. No score recalculation, catalogue reconstruction or activation.
  No second16MiB gym limit; the validated parent retains its32MiB budget.
- SqliteCoachingArchive snapshots all7tables in SAME transaction/original rowid order. Counts and
  raw text-byte lower bounds (including NULs) reject impossible32MiB exports/>100000rows BEFORE
  loading payloads, without truncation/writes. No native destructive restore command enabled;
  original Android stores remain authoritative.
- Added8 common archive graph/immutability/roundtrip cases and2 real SQLite cases (JVM+Apple) in
  RunningPlanRepositoryContract, including a real>32MiB SQL payload proving preflight before load.
  Added2 Android REAL original CoachingStore validator/export parity cases to the existing focused
  SharedRunningCoachingTest class. All earlier cases retained; no CI class-list change needed.
- Focused8archive+18JDBC running-plan tests PASS. Initial test parenthesis typo and nullable
  original.current access corrected without weakening assertions. Current317storage JVM PASS,
  0failure/error/skip. Common metadata/bothAPKs/lint PASS **4m31s/167tasks**,
  scratch/kmp-coaching-archive-final.log. Formatting777sources(682Kotlin/95Java), exact schema1..19,
  75schema tests and3source-provenance checks PASS (scratch/kmp-coaching-archive-{format,safety}.log).
  New Native/runtime cases NOT verified yet.
- Parent75 experiment43865551237 /format43865551231 FULL SUCCESS:515unique Android START/END,
  both original whole-data BackupEnvelope parity cases ran;700189checks/48suites. Digest-verified
  JVM14225822839 SHA2569fc3e5e888c7cfa49281f3dc37261e92fce3065bc88213791f2eb86c4986f9b9:
  490domain+16health+307storage. Native14225453053
  SHA256ed1ffe4c412ce75a733aa5ca43df745ab8396689e031b1082f0f2012be7ca997:
  449domain+18health+307storage. All0failure/error/skip; all11Native envelope cases matched XML.
  Proofs scratch/envelope-55d6-{jvm,native}/verification.json. Framework14225752358 NOT iOS app;
  privateAPK14226746613. Cancelled manual duplicate43865680449 is NOT the verification run.
- Next exact: all76local gates PASS. Commit/push coherent source+handoff and update PR24
  DRAFT. Inspect actual new Android/Native graph/source/preflight cases plus digest-verified XML
  (expected517focused Android/317storage Native NOT passed until proof). No merge/publication/bump.
- Then complete remaining parent graph normalization using this coaching-v1 section and existing
  codecs; port DevelopmentStore/SupportStore/Release08/other root sections and full transactional
  raw3/package1/retention reconciliation. NEVER use coaching-only validation as the complete
  BackupEnvelopeJson validator. Real common root/navigation/state, native services/full HealthKit/
  recording/BLE/permissions/notifications, REAL iOS host/app/runtime and final full release/visual
  gates remain unfinished. Mac/Xcode CI works; physical acceptance remains unperformed.

### Checkpoint 77 — Core user-data backup graph and retained-source cross-checks

- Checkpoint76 committed/pushed6999e80324f4113e63aeca7a8ca491ae6fa8b844. PR24 OPEN/DRAFT body
  updated. Parent experiment43869009332 /format43869009125 FULL SUCCESS:517unique Android START/
  END, both new original CoachingStore parity cases ran;700189checks/48suites. Digest-verified
  JVM14228765023 SHA2567f6c3c79e8200fcba5403449c372ddd50d90be414422da6e29acec8fbf7376d3:
  490domain+16health+317storage. Native14227143042
  SHA2566be24ef83fe961ed452b05e888e9b8380c5ccccb7ef6a7ef892134fb4a3895a6:
  449domain+18health+317storage. All0failure/error/skip; all8archive +2SQLite cases matched Native
  XML (normalize only platform suffixes [jvm]/[iosX64] for method-name comparison).
  Proofs scratch/coaching-6999-{jvm,native}/verification.json. Framework14227841914 NOT iOS app;
  privateAPK14227622360. No public experimental release, main merge or version/schema/backup bump.
- Added CoreBackupDataJson: original store-v1 profile/draft/manual baseline, user-owned activity,
  shoe, manual result, retained rolling effort/provenance, annotation, settings, reminder revision,
  assessment and legacy opaque-row guards. Reuses existing codecs and original limits/tolerance.
  Duplicate IDs/sources, imported health activities, missing shoes/annotation notes, conflicting
  distance codes, malformed settings/revisions and missing retained-source provenance reject.
- Original activity totals/results remain actual evidence, never recalculated. Unknown optional
  values stay null. Immutable typed snapshots preserve precise Long revisions and original source
  identity; retained opaque legacy rows are copied. Matching rolling PB provenance can support
  historical source references even without re-imported external activity, as in original validation.
- Added explicit current-running reminder-mirror and removed-detail source cross-checks using the
  completed coaching and retention archive snapshots. This is ONLY the core portion of parent
  validation; feature archives/remaining cross-references/transactional restore are still required.
  NEVER supply this component alone as the complete BackupEnvelopeJson validator. No native data
  replacement/pruning command enabled; Android production stores remain wired and unchanged.
- Added7 common core graph cases plus1 Android case in existing SharedBackupEnvelopeTest comparing
  actual retained activity/results and corrupt source/annotation/settings rejection against REAL
  original full SqliteBackupRepository validation. No class-list/format-config change needed.
- All current77 local gates PASS:324storage JVM0failure/error/skip; common metadata/bothAPKs/lint
  **6m42s/167tasks**, scratch/kmp-core-backup-final.log. Focused7core tests PASS58s build. Formatting
  **779sources(684Kotlin/95Java) PASS**, scratch/kmp-core-backup-format.log. Exact schema1..19,
  75schema tests and3source-provenance checks PASS, scratch/kmp-core-backup-safety.log. No known
  source/build failure remains. New Native/Android cases await pushed CI; expected518focused
  Android/324storage Native are NOT yet verified. Physical acceptance remains unperformed.
- Next exact: commit/push coherent77 source+handoff to existing draft PR24 and inspect new exact-head
  runtime/logs/digest-verified artifacts. Reuse completed core/coaching data guards; finish remaining
  parent feature archives (DevelopmentStore/SupportStore/Release08, strength choices/programming/
  execution links, tool/body/achievement sections) and full raw3/package1/retention restoration.
  Native destructive operations stay disabled until COMPLETE validation and transaction/consent/
  active-recording safeguards exist. Then common root/navigation/state/native services/full HealthKit/
  recording/BLE/notifications and REAL iOS host/app/runtime plus final unfiltered API28/35/36/iOS APP/
  source-bound light/dark/200% visuals. Do not start another release or merge/publish prematurely.

### Final recovery boundary — checkpoint77 pushed; exact-head runtime pending

- Continue experiment/android-ios-kmp-compose, HEAD/remote
  **4d40365ae69a2655d8cb375f43524f43a557b629**.76(6999e80) and77 ALL source committed/pushed;
  PR24 OPEN/DRAFT confirmed at4d40365, body updated. Main85e1c39 /owner PR23 untouched. No
  version/schema/backup bump, merge or public experimental publication. Only this final status note
  intentionally local; include it with the next coherent source checkpoint, avoid docs-only CI push.
- Latest PR experiment **43871575805 RUNNING**: Android133313857245 /Apple133313856795.
  Formatting43871575783 SUCCESS. Expected518focused Android/324storage Native NOT verified
  until completed logs/digest-verified XML. No duplicate manual workflow was dispatched this time.
- All77 local checks COMPLETE/PASS:324storage JVM0failure/error/skip, common metadata/bothAPKs/lint
  **6m42s/167tasks**,779source formatting(684Kotlin/95Java), focused7core tests, exact schema1..19/
  75schema tests/source-provenance3. Logs scratch/kmp-core-backup-{focused,final,format,safety}.log.
  No known local source/build failure. All76 local checks also passed317JVM/777format/APKs/lint.
- Parent76 experiment43869009332 /format43869009125 FULL SUCCESS:517unique Android START/END,
  both original CoachingStore archive parity cases ran;700189checks/48suites. Digest-verified JVM
  14228765023 SHA2567f6c3c79e8200fcba5403449c372ddd50d90be414422da6e29acec8fbf7376d3:
  490domain+16health+317storage. Native14227143042
  SHA2566be24ef83fe961ed452b05e888e9b8380c5ccccb7ef6a7ef892134fb4a3895a6:
  449domain+18health+317storage. All0failure/error/skip. All8coaching archive methods matched against
  current common JVM methods after removing ONLY [jvm]/[iosX64] platform suffixes; both new Native
  SQLite source/no-write/>32MiB-preflight cases ran. Proofs scratch/coaching-6999-{jvm,native}/
  verification.json. Framework14227841914 NOT iOS app; privateAPK14227622360.75 is fully verified too.
- Next exact: inspect77 actual Native seven CoreBackupDataJsonTest cases and Android new
  SharedBackupEnvelopeTest#sharedCoreGuardsMatchOriginalWholeDataActivityResultAndAnnotationRules;
  verify total518Android/324Native and JVM/Native artifact digests/XML, fix genuine failures without
  weakening tests or blindly rerunning. Preserve all completed source, including full Java-domain
  conversion and prior shared UI/repos/commands/fingerprint/retention/envelope/coaching/core guards.
- Then complete parent feature archive normalization and cross-references. CoreBackupDataJson77
  and CoachingArchiveJson76 are COMPONENTS, not the complete BackupEnvelopeJson validator. Reuse
  their typed source indexes, reminder/retention checks and exact payloads; implement remaining
  DevelopmentStore/SupportStore/Release08, strength choices/legacy programme/authored/execution graph
  links, tool/body/achievement sections. Finish complete same-database transactional restore/raw3/
  package1/compatible archive-detail/retention consent/preview/epoch/active-recording safeguards.
  Native destructive replacement/pruning stays disabled until ALL safeguards are implemented/tested.
- Full product still needs common application root/navigation/state/data composition, native full
  permissions/HealthKit/recording/BLE/notifications, REAL iOS host/entitlements/app build/runtime,
  final unfiltered API28/35/36 +iOS APP/source-bound screenshots/light/dark/200% gates. Mac/Xcode CI
  works; incomplete app is not an infrastructure blocker. Physical Galaxy S24/HealthKit/Health
  Connect/GPS/BLE/audio/locked-screen/OEM/human TalkBack remain unperformed. No premature bump/merge/
  publication; do not start another release or reimplement already completed domain/UI/storage work.

### Checkpoint 78 — Typed legacy evidence archive and weight-export privacy

- Preserved77source and its local final recovery note. Branch experiment/android-ios-kmp-compose /
  DRAFT PR24; main85e1c39 /owner PR23 unchanged. No version/schema/backup/publication changes.
- Added DevelopmentArchiveJson for original development-v1 records: WEIGHT, HISTORICAL_PB,
  INTERRUPTION, SESSION_DECISION, STRENGTH_LINK, REVIEW_DECISION and WEEKLY_REVIEW. Original kind/
  identity, domain measurement/date/effort/decision and paired gym-reference checks are reused.
  Unknown/future kinds/versions, duplicate kind+ID, mismatched identity and invalid records reject.
- Immutable typed evidence views retain private DOM copies of original payloads/unknown legacy
  fields. Historical PBs keep measurement profile/date precision and remain separate from actual
  workouts. Weekly summaries retain old channel/choice shapes without invented new fields or scores;
  completed counts may exceed planned as before. Reading/encoding never applies decisions.
- Older imported WEIGHT records remain readable. Explicit userOwnedElement omits non-manual weights
  exactly like original export, without deleting or changing imported observations. This is an
  archive component, NOT a complete parent validator/native restore/SQL export implementation.
  Future DB export must filter imported cached weights BEFORE parent row/size limits, as Android
  does; do not load an unbounded cache into this already-bounded file-archive decoder.
- Added8 common cases covering all7kinds, manual/imported privacy, identity/size/future guards,
  weight provenance, all historical measurement profiles/unknown dates, pairing rules, sparse legacy
  reviews and immutable payloads. Added2 Android cases in existing SharedBackupEnvelopeTest using
  REAL DevelopmentStore writes/export/validator, all7kinds, older imported weights, no-deletion and
  corrupt-evidence rejection. Existing class already in focused CI/format configuration.
- All78local gates PASS:332storage JVM0failure/error/skip; common metadata/bothAPKs/lint
  **7m34s/167tasks**, scratch/kmp-development-archive-final.log; focused8cases PASS1m2s build;
  formatting781sources(686Kotlin/95Java) PASS, scratch/kmp-development-archive-format.log; exact
  schema1..19/75schema tests/source-provenance3 PASS, scratch/kmp-development-archive-safety.log.
  No known local source/build failure. New Android/Native runtime cases still require pushed CI.
- Parent77 experiment43871575805 /format43871575783 FULL SUCCESS:518unique Android START/END,
  new REAL whole-data core guard parity case ran;700189checks/48suites. Digest-verified JVM14227529786
  SHA25673250ec23fa6114366d3e12d6b71696fd003a16e156d433539ddc13d231b9fa6:
  490domain+16health+324storage. Native14229760530
  SHA256511bf084296b0e790d7a4fe1ce19503690c1d6e6aa50ce9f35f147e43235588e:
  449domain+18health+324storage. All0failure/error/skip; all7CoreBackupDataJsonTest Native methods
  matched common JVM names, ignoring ONLY [jvm]/[iosX64] suffixes. Proofs scratch/core-4d403-
  {jvm,native}/verification.json. Framework14229810475 NOT iOS app; privateAPK14227114823.
- Next exact: commit/push78 coherent source+handoff, keep PR24 DRAFT, inspect exact new Native8cases
  and Android2cases/logs/digest-verified XML (expected520focused Android/332storage Native NOT yet
  verified). Reuse completed core/coaching/development guards, finish SupportStore/Release08 and
  other parent feature sections/graph links, full transactional/raw3/package1/retention restoration.
  Native destructive operations remain disabled until COMPLETE validation/consent/epoch/preview/
  active-recording protections exist. Real common root/navigation/state/native services/full HealthKit/
  recording/BLE/notifications, REAL iOS app host/runtime and final full matrix/visual gates remain
  unfinished. Mac/Xcode CI works; physical checks unperformed. No premature merge/bump/publication.

### Final recovery boundary — checkpoint78 pushed; exact-head runtime pending

- Continue experiment/android-ios-kmp-compose, HEAD/remote
  **64a52ce20dade3a81a239dd0e6a79232166727e9**.78 source ALL committed/pushed; PR24 OPEN/DRAFT
  confirmed at this head and body updated. Main85e1c39 /owner PR23 untouched; version0.12.3-test1/
  code19 and formats SQLite19/root13/raw3/package1 unchanged. No merge/public experimental artifacts.
  Only this final status note intentionally local; include with next source checkpoint, avoid a
  docs-only CI push. Preserve it if workspace survives.
- Latest exact-head PR experiment **43875256234 RUNNING**: Android133326511896 /Apple133326512273.
  Formatting43875256240 SUCCESS. Expected520focused Android/332storage Native NOT yet verified.
  Next exact: inspect all8 DevelopmentArchiveJsonTest Native cases and both new Android development
  archive cases in SharedBackupEnvelopeTest; verify completed START/END and artifact ZIP digests/XML.
  Fix genuine failures without weakening assertions or blindly rerunning.
- All78local gates COMPLETE/PASS:332storage JVM0failure/error/skip; focused8archive cases1m2s build;
  metadata/bothAPKs/lint **7m34s/167tasks**, scratch/kmp-development-archive-final.log;781source
  formatting(686Kotlin/95Java), exact schema1..19/75schema tests/source-provenance3 PASS. Logs
  scratch/kmp-development-archive-{focused,final,format,safety}.log. No known local source/build failure.
- Parent77 experiment43871575805 /format43871575783 FULL SUCCESS:518unique Android START/END,
  original full-data core guard parity case ran;700189checks/48suites. Digest-verified JVM14227529786
  SHA25673250ec23fa6114366d3e12d6b71696fd003a16e156d433539ddc13d231b9fa6:
  490domain+16health+324storage. Native14229760530
  SHA256511bf084296b0e790d7a4fe1ce19503690c1d6e6aa50ce9f35f147e43235588e:
  449domain+18health+324storage. All0failure/error/skip. All7 Native core methods matched current
  common JVM names, ignoring ONLY platform suffixes [jvm]/[iosX64]. Proofs scratch/core-4d403-
  {jvm,native}/verification.json. Framework14229810475 NOT iOS app; privateAPK14227114823.
- Completed component guards: root envelopes75, coaching76, core77 and development evidence78.
  Preserve/reuse these and all earlier source (0Java-domain completion/shared UI/repos/commands/
  hashes/programme/association/retention work). DevelopmentArchiveJson retains all7original evidence
  kinds and older imported weights; userOwnedElement omits imported weights without deletion.
  This is NOT a native DB export/restore implementation or a complete parent snapshot validator.
  DB capture must filter imported weights BEFORE row/size limits, matching original privacy rules.
- Next implementation: SupportStore/Release08 and remaining feature archive normalization/graph
  links (strength choices/legacy programmes/authored/execution/tool/body/achievement data). Compose
  COMPLETE parent validation with existing core/coaching/development and retained-source guards.
  Full same-database transactional replacement/raw3/package1/archive reconciliation/retention consent/
  preview/epoch/active-recording safeguards remain incomplete. Native destructive operations stay
  disabled; never pass a component decoder as the complete BackupEnvelopeJson validator.
- Then real common root/navigation/state/data composition, native full permissions/HealthKit/
  recording/BLE/notifications, REAL iOS host/entitlements/app build/runtime; final unfiltered API28/
  35/36 +iOS APP/source-bound light/dark/200% gates. Working Mac/Xcode CI is available; incomplete
  software is not an infrastructure blocker. Physical HealthKit/Health Connect/GPS/BLE/audio/locked-
  screen/OEM/human TalkBack unperformed. No premature bump/merge/publication or new release scope.

### Checkpoint 79 — Running-support and exercise-choice archive portability

- Continue experiment/android-ios-kmp-compose /DRAFT PR24. Preserved uncommitted support/0.8
  readers and78final note; main85e1c39 /owner PR23 untouched. No version/schema/backup bump,
  merge, destructive Native operation or public experimental publication.
- Added SupportArchiveJson and Release08ArchiveJson with typed original Dream Route assignments,
  routine evidence, saved running steps, and grouped custom workouts. Exact original payloads and
  unknown legacy fields remain retained. Interrupted/skipped/unfinished executions do not become
  completed activities; absent load/effort/time/distance targets stay unknown. Original event order,
  terminal-step index, step/group limits, mode/route/config identities and preferences are checked.
- Confined original getDouble/getLong/getInt/getBoolean coercions to SupportPayloadJson, preserving
  existing stored numeric-string steps and preferences. Other strict backup codecs remain strict.
- SqliteRunningSupportArchives captures both original tables in one read transaction, with count/raw
  BLOB-byte preflight before payload loading, no truncation/writes/assignment/activation. This is a
  component snapshot, NOT complete parent validation/restore. No second database or new JSON bucket.
- Continued next missing section: StrengthChoicesArchiveJson retains original v1/v2 favourites,
  archive choices, PB acknowledgements and reversible exclusions/reasons/timestamps. Current strict
  fields/booleans, duplicate identities, limits and older optional fields follow original validation.
  SqliteStrengthChoices.archiveSnapshot reads all three existing tables consistently with bounded
  preflight. No workout/PB/history evidence is created/deleted or choice automatically applied.
- Added19 common codec cases,4 real SQLite contract cases (JVM AND Apple),3 Android parity cases in
  existing SharedBackupEnvelopeTest using REAL original writes/exports/validators and shared SQLite
  snapshots. Cases cover original/custom prescription distinctions, unknown targets, old fields,
  numeric conversions, current/older preferences, reversible exclusions and no-write/oversize guards.
- All current79 local gates COMPLETE/PASS:355storage JVM0failure/error/skip; common metadata,
  bothAPKs and lint PASS **7m15s/167tasks**, scratch/kmp-legacy-archives-final.log. Formatting
  **789sources(694Kotlin/95Java)**, exact schema1..19/75schema tests/source-provenance3 PASS, logs
  scratch/kmp-legacy-archives-{format,safety}.log. Local APK verified0.12.3-test1/code19/min28/noINTERNET.
  Earlier focused12cases PASS1m3s, support-only347JVM/APKs/lint PASS7m21s. No known local source/build
  failure. New expected355storage Native/523focused Android are NOT verified until pushed CI.
- Parent78 experiment43875256234 /format43875256240 FULL SUCCESS.520unique Android START/END
  matched, both development archive cases ran;700189checks/48suites. Digest-verified JVM14230945994
  SHA2568586e1d52184216ef39aae262fab0f7d2e6346bf9f231aaf5de24377eb337c95:
  490domain+16health+332storage. Native14229459575
  SHA2561cf4d3d012a4e791f4c291057032f24388c290e8be3866f7582a13ec7272562f:
  449domain+18health+332storage. All0failure/error/skip. All8 Native DevelopmentArchiveJsonTest methods
  matched current common names ignoring ONLY [jvm]/[iosX64] suffixes. Proofs
  scratch/development-64a52-{jvm,native}/verification.json. Framework14231201551 NOT iOS app;
  privateAPK14231445395. Completed work must not be reimplemented.
- Next exact: commit/push coherent79 source+handoff to EXISTING draft PR24, inspect exact new
  Android3/Native23 cases and
  digest-verified XML. Then finish remaining parent feature archives/graph links (legacy programme,
  authored/execution/tool/body/achievement sections), COMPLETE parent validation, raw3/package1 and
  same-database transactional restore/retention safeguards. Native replacement/pruning stays disabled.
- Full common app root/navigation/state/data, native permissions/HealthKit/recording/BLE/notifications,
  REAL iOS host/entitlements/app/runtime and final full API28/35/36/iOS APP/source-bound visuals remain
  unfinished. Working Mac/Xcode CI exists. Physical HealthKit/HC/GPS/BLE/audio/locked-screen/OEM/
  human TalkBack unperformed. No premature bump/merge/publication or unrelated release.

### Checkpoint80 — Body-measurement and achievement archives (locally verified; next push pending)

- Checkpoint79 ALL source committed/pushed as **4e4d336b4b792ef14775ac0b86f30c2daca50519**;
  PR24 OPEN/DRAFT confirmed, description updated. Main85e1c39 /PR23 untouched. Exact-head experiment
  **43880437540 RUNNING**, Android133344566099 /Apple133344566645; formatting43880437522 SUCCESS.
  Expected523Android/355Native remain unverified until completed logs/digest-verified XML.
- Continued next original sections without restarting: BodyMeasurementsArchiveJson,
  AchievementsArchiveJson, BodyAndAchievementsArchiveTest and2 additional parity methods in existing
  SharedBackupEnvelopeTest. These4 source files are included in coherent80 local commit; next push
  waits for79 runtime to avoid cancelling its still-running Android gate. Existing typed body/milestone models
  reused; measurement identities/nullable provenance/reference basis, strict fields/versions/limits,
  duplicate sources, actual milestone proof/recognition chronology and old missing-section defaults
  retained. No guidance/celebration activation, reawarding, DB replacement or new SQL tables.
- Added9 common cases plus2 REAL original Android export/validator parity cases. Focused9 PASS
  **1m2s**, scratch/kmp-body-awards-focused.log, after fixing prohibited Kotlin object wildcard
  imports with explicit imports (initial compile failure resolved). Full storage JVM now364tests /
  0failure/error/skip. Common metadata AND Android test compilation PASS **3m16s/64tasks**,
  scratch/kmp-body-awards-final.log. Formatter792sources(697Kotlin/95Java), exact schema1..19 /
  75schema tests/source-provenance3 PASS, scratch/kmp-body-awards-{format,safety}.log.
- Next exact: inspect79CI before any next push to avoid cancellation. All80 relevant local checks
  now pass; push coherent80 once79 runtime finishes;
  keep PR24draft, record actual runtime counts/digests. No public experimental artifacts or version/
  schema/backup changes. Parent78 FULL SUCCESS is recorded above;79local355/APKs/lint all passed.
- Parent79 Apple artifact14233291239 digest VERIFIED:
  eee49e4f4d6c9cbb06b57c1b1d9c4bbf919a41120879b4f433fbb34e518326bc;449domain+18health+355storage,
  all0failure/error/skip. All19 new archive codec methods and4SQLite contract methods matched exact
  expected Native names, ignoring ONLY platform suffixes. Proof scratch/support-4e4d336-native/
  verification.json; framework14233286286 NOT iOS app. Android523 still pending.
- Then remaining legacy programme/authored/execution/tool archive composition/graph links, COMPLETE
  parent backup validation, raw3/package1 and transactional restore/retention safeguards, common app
  composition/native full services/REAL iOS app/final matrix remain as below. Native destructive
  operations stay disabled. Physical checks unperformed; actual Mac/Xcode CI is available.

### Final recovery boundary — checkpoint80 pushed; exact-head runtime pending

- Branch experiment/android-ios-kmp-compose, HEAD/remote
  **854a9f6a43a4853e0a2dff57a8613c910afc2953**.79(4e4d336) AND80 ALL source committed/pushed;
  GitHub branch ref/PR24 OPEN/DRAFT confirmed at854a9f6; description updated. Main85e1c39 /owner PR23
  untouched. Version0.12.3-test1/code19, SQLite19/root13/raw3/package1 unchanged. No merge or public
  experimental publication. Only this final recovery note intentionally local; include with next
  coherent source checkpoint, avoid documentation-only CI push. Preserve it if workspace survives.
- Latest exact-head experiment **43883242494 RUNNING**: Android133354410670 /Apple133354410319.
  Formatting **43883242583 SUCCESS**. Expected525focused Android/364storage Native NOT verified.
  Next exact: inspect all9 BodyAndAchievementsArchiveTest Native methods and BOTH new Android cases
  sharedBodyArchiveMatchesOriginalSourceIdentityAndOptionalGuidancePreferences /
  sharedAchievementsArchiveRetainsOriginalProofWithoutReawardingOrChangingCelebrations in existing
  SharedBackupEnvelopeTest. Verify completed START/END sets and artifact digests/XML, not test counts
  alone. Fix actual failures without weakening tests or blindly rerunning jobs.
- All80 relevant local gates COMPLETE/PASS: focused9 **1m2s**, full364storage JVM0failure/error/skip,
  common metadata AND Android test compilation **3m16s/64tasks**.792source formatting(697Kotlin/
  95Java), exact schema1..19/75schema tests/source-provenance3 PASS. Logs scratch/kmp-body-awards-
  {focused,final,format,safety}.log. Initial Kotlin object-wildcard import compile error fixed with
  explicit imports; no known current source/build failure.79 bothAPKs/lint PASS7m15s/167tasks and
  local APK version/min28/noINTERNET verified. No full current80 APK/lint/full matrix claim.
- Parent79 experiment **43880437540** /format43880437522 FULL SUCCESS:523unique Android START/END
  matched, all3 REAL support/0.8/choices-store parity cases ran;700189checks/48retained domain suites.
  Digest-verified JVM14233490851
  SHA25656d9d0a58a0dd5e5bf68db0122ab374983f820cbe422f67700d829346a0db944:
  490domain+16health+355storage. Native14233291239
  SHA256eee49e4f4d6c9cbb06b57c1b1d9c4bbf919a41120879b4f433fbb34e518326bc:
  449domain+18health+355storage. All0failure/error/skip; all19 new archive methods and4 new Native
  SQLite contract methods matched exact common names ignoring ONLY [jvm]/[iosX64] suffixes.
  Proofs scratch/support-4e4d336-{jvm,native}/verification.json. Framework14233286286 NOT iOS app;
  privateAPK14233680655. Parent78 is fully verified too. Do not redo completed source/tests blindly.
- Push80 initially stalled without a remote ref update; terminated ONLY that push's process tree
  after checking GitHub ref. Normal non-force retry with per-command HTTP/1.1 and bounded low-speed
  timeout succeeded; actual remote SHA854a9f6 confirmed. No history rewrite/config weakening.
- Completed component readers now include coaching/core/development/support/0.8/exercise choices/
  body measurements/achievements/retention. Body80 keeps manual/imported height source/timestamp/
  nullable-offset/import-time and optional selected weight reference distinct from actual records;
  achievement80 preserves original proof and recognition chronology, never awards/celebrates by
  reading. Reuse existing domain eligibility checks and typed models; no calculation changes.
  These components are NOT a complete parent BackupEnvelopeJson validator or native restore.
- Next implementation after80 CI: normalize original GymProgrammeStore v1 archive/current/optional
  decisions and validate STRENGTH_LINK against typed coaching actuals + legacy/authored prescriptions.
  Reuse LegacyGymProgrammePayloadJson and StrengthProgrammingArchiveJson; add DOM entry points where
  needed without imposing standalone16MiB gym limits on an already bounded32MiB parent. Compose
  remaining frozen-workout/execution/actual/edit/source/association archive graph and tool/timer
  ownership guards from completed codecs/commands. No catalogue reconstruction of historic records.
- Compose COMPLETE parent validation with core reminder mirror, retained-source provenance and all
  sections, then same-database transactional replacement/raw3/package1/archive reconciliation and
  retention consent/preview/epoch/active+recoverable-recording protection. Native destructive replace/
  prune stays disabled until ALL guards are implemented and tested. DB development export must
  filter imported cached WEIGHT rows BEFORE user-owned row/size caps, matching original privacy;
  do not feed an unbounded provider cache into the bounded external-archive component decoder.
- Full common app root/navigation/state/data, full native permissions/HealthKit/recording/BLE/
  notifications, REAL iOS host/entitlements/app build/runtime and final unfiltered API28/35/36 /
  iOS APP/source-bound light/dark/200% screenshots remain unfinished. Working Mac/Xcode CI exists;
  incomplete app is a software task, not infrastructure unavailability. Physical HealthKit/HC/GPS/
  BLE/audio/locked-screen/OEM/human TalkBack UNPERFORMED. No premature bump/merge/publication.

### Checkpoint81 — Legacy programme graph, bounded snapshots and parent timer archive

- Continue experiment/android-ios-kmp-compose /DRAFT PR24; preserve80final recovery note and all
  prior source. Main85e1c39 /owner PR23 untouched. Version0.12.3-test1/code19, SQLite19/root13/raw3/
  package1 unchanged. No new dependencies/permissions, destructive Native restore/prune or public
  experimental publication. Legacy Android stores remain authoritative while full app wiring waits.
- Added LegacyGymProgrammeArchiveJson: typed immutable revisions/current pointer/progression
  decisions; original optional absent decisions and unknown legacy fields preserved via private DOM.
  Duplicate versions/decisions, missing current/base/result revisions, future formats and row limits
  reject. Older unusual valid decision details are retained, never reinterpreted as new proposals.
- Shared legacy prescription/decision codecs now offer DOM entries and element builders, reusing
  unchanged domain checks. validateLinks checks original STRENGTH_LINK actual identity and exact
  legacy/authored prescribed session/date; unassociated old evidence remains legal. No progression
  activation, catalogue reconstruction, starting-load transfer or fabricated actual sets.
- SqliteLegacyGymProgrammes.archiveSnapshot captures original versions/current/decisions in ONE
  existing DB transaction. Count/raw BLOB byte preflight before loading, exact SQL/payload identity
  checks and full graph validation; no truncation/writes. Disabling the pointer retains history.
- GymToolsPayloadJson now offers a parent DOM reader/builder. Existing standalone string reader /
  encoder retain16MiB limits; an already-bounded32MiB parent can contain a valid larger timer section.
  Original known-execution ownership, uniqueness/preferences/version/clock guards remain mandatory.
  New40,000-timer format-boundary fixture proves >16MiB/<32MiB DOM round-trip and standalone rejection;
  this is synthetic format stress evidence, not a claim about a real athlete's workout history.
- Added8 common programme archive cases,2 real SQLite contracts (JVM AND Apple),2 timer DOM cases,
  plus2 original Android store/export/validator cases in existing SharedLegacyGymProgrammeTest.
  Recorded50kg stays separate from82.25kg prescription reference. No existing assertions weakened.
- All81 local gates PASS: focused20 legacy/archive/repository cases **1m10s**, full376storage JVM /
  0failure/error/skip, common metadata AND Android test compilation **4m13s/64tasks**. Timestamp-fix
  full376JVM rerun PASS52s.796source formatting(701Kotlin/95Java), exact schema1..19/75schema tests /
  source-provenance3 PASS. Logs scratch/kmp-{legacy-gym-archive-focused,gym-parent-final,
  gym-parent-time-fixed,gym-parent-format-final,gym-parent-safety}.log. No known current local failure.
- Parent80 experiment43883242494 finished with Android SUCCESS, Apple FAILURE. Android525unique
  START/END matched; both new body/achievement original-store cases ran;700189checks/48suites.
  Digest-verified JVM14235006212 SHA2565724db9a7825782a5801ff15b7be254a8f1ef8a40d25ae5cb62e5b59fd5b7513:
  490domain+16health+364storage, all0failure/error/skip. Proof scratch/body-854a9f6-jvm/verification.json.
  PrivateAPK14233418488; formatting43883242583 SUCCESS. NO80Native pass/framework claim.
- Actual Apple80 root cause: commonTest BodyAndAchievementsArchiveTest line199 used JVM-only
  Instant.plusSeconds. Replaced ONLY fixture timestamp construction with existing portable
  Instants.ofEpochSecond(seconds+delta,nanos), preserving the exact timestamps and all assertions.
  Production time/domain/recording APIs unchanged. Do not blindly rerun failing80: verify corrected
  source in next CI. Last fully verified Native checkpoint remains79(355storage), recorded above.
- Next exact: commit/push coherent81 source+handoff to EXISTING draft PR24. Verify fresh Native test
  compilation, all9 body/achievement methods plus12new archive/SQLite/timer methods and Android2new
  cases; expected376Native storage /527focused Android remain UNVERIFIED until real CI artifacts /
  completed logs. Keep original safeguards; fix actual failures, no blind reruns/test weakening.
- Then compose remaining frozen-workout/execution/accepted-actual/edit/source/association archive
  graph using completed codecs/source guards, followed by COMPLETE parent validator using all
  sections and tool/timer ownership. Legacy programme/current/decision/link and timer DOM components
  now exist; do not redo them. Components alone must NEVER be used as complete BackupEnvelopeJson
  validation. Native transactional replacement/raw3/package1/retention consent/preview/epoch/active+
  recoverable-recording protections stay disabled until all required checks exist and are tested.
- Full common app root/navigation/state/native services, full HealthKit/permissions/recording/BLE/
  notifications, REAL iOS host/app/runtime, final unfiltered28/35/36/iOS APP and source-bound visuals
  remain unfinished. Working Mac/Xcode CI exists. Physical acceptance remains UNPERFORMED. No
  premature version bump, merge/publication or unrelated feature release.

### Final recovery boundary — checkpoint81 ALL source pushed; Android AND Apple verified

- Continue experiment/android-ios-kmp-compose, HEAD/remote
  **befb74dc9d729e913e94acef74f1a77c0d265c38**.81 ALL source committed/pushed; PR24 OPEN/DRAFT
  confirmed at this SHA and description updated with actual verification. Main85e1c39 /owner PR23
  untouched. No version/schema/backup bump, merge or public experimental publication. Only this
  final status note intentionally local; include with next source checkpoint, avoid docs-only CI
  push. Preserve it if workspace survives. This boundary supersedes all prior pending81/80 notes.
- Exact-head experiment **43886908160 FULL SUCCESS**: Android133367412273 /Apple133367411585.
  Formatting **43886908141 SUCCESS**.527unique Android START/END sets matched; both new original
  legacy gym archive/link parity cases ran.700189checks/48retained domain suites passed.
- Digest-verified JVM14235428645
  SHA256470ca90dd886a1f70ffe1cc180b8be33b56a69e2fdb012937dd9308b009a0b6b:
  490domain+16health+376storage. Native14235627791
  SHA256c63c40c2304dfaf7695ebd12a48f75865cdd5b2ce831685f49edfef0bedc6ab7:
  449domain+18health+376storage. All0failure/error/skip. All9 BodyAndAchievementsArchiveTest,8
  LegacyGymProgrammeArchiveTest and2 GymToolsArchiveElementTest methods matched exact common names,
  ignoring ONLY [jvm]/[iosX64] suffixes. Both new Native SQLite archive contracts ran too. Proofs
  scratch/gym-parent-befb-{jvm,native}/verification.json. Framework14235323013 NOT iOS app;
  privateAPK14235023964. No claim of final full API28/35/36/iOS APP/visual gates.
- All81 relevant local gates COMPLETE/PASS: focused20legacy/archive/repository cases1m10s;
  full376storage JVM0failure/error/skip; metadata +Android test compilation **4m13s/64tasks**;
  full376 rerun after timestamp fixture correction PASS52s.796format sources(701Kotlin/95Java),
  exact schema1..19/75schema tests/source-provenance3 PASS. Logs scratch/kmp-gym-parent-
  {final,time-fixed,format-final,safety}.log and kmp-legacy-gym-archive-focused.log.
- Actual80 Apple failure is now RESOLVED on corrected81 source: replaced test-only JVM
  Instant.plusSeconds with existing portable Instants.ofEpochSecond preserving seconds/nanos.
  All9 body/achievement tests now compiled and ran on Native. Production time/recording/domain
  unchanged; no assertions removed or weakened and no blind rerun of failing80 source.
- Completed81: original legacy programme/current/optional decision archive guards + typed links
  to exact legacy/authored sessions and real retained actual IDs; bounded consistent SQLite
  snapshot with source-column checks; parent timer DOM entry retaining existing standalone16MiB
  guards/mandatory execution ownership. Valid40,000-timer >16MiB/<32MiB stress passed on Native.
  Private original payloads/unknown legacy fields, disabled programme history, references versus
  entered loads and unassociated old actuals stay intact. No new durable bucket/table or commands.
- NEXT EXACT IMPLEMENTATION: full strength-engine archive composition/validation from original
  SqliteGymExecutionRepository.validate. Reuse FrozenGymWorkoutRecordJson, GymExecutionSnapshotJson,
  StrengthEvidenceJson accepted actuals, SqliteGymSources source/receipt checks, pure domain source
  validators and GymAssociationArchiveJson. Add parent DOM entries where needed without replacing
  whole-parent32MiB with standalone16MiB. Preserve original256KiB normalized SQL row bounds,
  exact frozen source/prescription/accepted-actual/edit receipts, one unfinished execution,
  association provenance and original compatible fingerprint contract. Compare against REAL
  original validator, including long rows/escaping/unknown evidence; do not reconstruct history
  from the catalogue. Legacy81 snapshot/link +timer DOM guard already exist; do not redo them.
- Then compose COMPLETE parent data validator with coaching/core/development/choices/support/0.8/
  legacy/authored/execution/tool/body/achievement/retention sections, reminder mirror and retained
  source guards. Components alone must NEVER be passed as complete BackupEnvelopeJson validation.
  Finish SAME-DB transactional replacement, raw3/package1/compatible archived-detail reconciliation,
  consent/preview/dataset epoch/active+paused+recoverable-recording safeguards. Native destructive
  replacement/pruning remains disabled until complete validation and all safeguards are tested.
  Development DB export must omit imported WEIGHT caches BEFORE owned-row/size caps, as original.
- Full common root/navigation/state/data composition, native full permissions/HealthKit/recording/
  BLE/notifications, REAL iOS host/entitlements/app build/runtime, final unfiltered Android28/35/36
  +iOS APP/source-bound light/dark/200% visuals still unfinished. Working Mac/Xcode CI exists;
  incomplete software is not an infrastructure blocker. Physical GalaxyS24/HealthKit/HC/GPS/BLE/
  audio/locked-screen/OEM/human TalkBack UNPERFORMED. Keep PR24 DRAFT; no premature bump/merge/
  public publication or unrelated release.

### Checkpoint82 — Complete parent backup and immutable gym archive graph

- Continue `experiment/android-ios-kmp-compose` /DRAFT PR24. All prior valid81 source and its
  final verified recovery note are preserved. Main85e1c39 /owner PR23 untouched. No version,
  schema, backup format, permissions, dependencies, merge or public publication change.
- Added GymExecutionArchiveJson: original v1/v2 frozen revisions/preferences/executions/accepted
  actuals/edit receipts/source fingerprints/association graph, exact Long revisions and original
  256KiB normalized SQL row bounds. LegacyGymRowJson reuses original JSONObject-compatible
  escaping, number spelling and Java UTF8; compact JSON cannot bypass escaped slash row limits.
  Shared DOM entries retain standalone16MiB guards and whole-parent32MiB support. Repeat-source
  guards extracted unchanged from SqliteGymSources and use the REAL portable fingerprint provider.
- Added UserBackupDataJson: COMPLETE original store-v1 parent validator composing EVERY existing
  feature archive, running reminder mirror, actual/programme/link/source cross-references, timer
  execution ownership and removed-detail provenance. `inspect` uses this mandatory validator for
  root1..13, not an isolated section callback. Current root13 export fills only missing old empty
  defaults, preserves unknown original metadata/history, retains protected existing-user retention
  and omits imported WEIGHT caches without mutating an inspected older archive. No restore/prune.
- Added10 common gym graph cases,3 canonical row cases,10 complete parent cases and3 original
  Android SQL/export/validator parity cases in existing SharedGymExecutionCommandsTest. Existing
  SharedBackupEnvelopeTest root1..13 callback now ALSO checks the COMPLETE shared parent, alongside
  the REAL original validator. Keeps actual50kg distinct from reference82.25kg and preserves exact
  source hashes, accepted edits/deletion receipts and null older unassociated links. No assertions
  weakened. Initial fixture incorrectly requested association review before workout completion;
  fixed its timestamp and retained an assertion that premature review yields no proposal.
- Local section gate:389storage JVM0failure/error/skip, common metadata+Android test compilation
  PASS3m39s/64tasks. Full parent pre-privacy-final gate:398storage JVM0failure/error/skip; metadata,
  APK, instrumentation APK and lint PASS7m12s/167tasks. FINAL privacy-projection source then passed
 399storage JVM0failure/error/skip +metadata/APKs/lint8m15s/167tasks. Logs scratch/kmp-complete-backup-
  {final,owned-final}.log. This last build predates the following raw archive addition; final raw
  compile/tests are RUNNING separately. Do not claim the older APK/lint includes later raw source.
- Next in-order implementation within82: RecordingArchiveRows streams original raw1..3 row-graph
  validation, keeping only bounded per-session metadata, not dense point arrays. Retains exact
  Long clocks, contiguous point/event sequences, accepted monotonic/distance evidence, contexts,
  owned finished summaries and removal receipts. Never fills GPS gaps/recalculates summary totals;
  mixed removed/raw detail rejects. Failed row poisons the reader; malformed/truncated/duplicate/
  future/over-budget rows cannot return a valid graph. Retains original versioned session/point
  limits,8MiB line/512MiB dataset bounds and BOM compatibility. This is NOT ZIP/manifest/checksum
  verification or transactional raw restore; platform framing/digest adapters must still be done.
- Added10 common streamed-row cases and2 original Android export/parity cases in existing
  SharedDetailRetentionArchiveTest (gaps+recovery+oldformats and truthful removal provenance).
  Real original archive bytes/checksum are checked in the fixture; original idempotent restore
  must preserve exact coordinates/summary/receipt. No production recording/filter changes.
- Formatting805sources(710Kotlin95Java) PASS; exact schema1..19 /75schema tests /3source-provenance
  checks PASS. Final409storage JVM0failure/error/skip, shared metadata and Android test compilation
  PASS4m49s/64tasks, log scratch/kmp-raw-parent-final.log. No new Native/runtime result yet; expected409storage and532focused Android must be
  verified from NEW exact-head CI after coherent commit/push. Last fully verified Native remains
  checkpoint81(376storage), Android527. No current failing source assertions known.
- NEXT EXACT: local409JVM/metadata/Android test compilation, XML and format are verified.
  Commit/push coherent82 to EXISTING draft PR24. Exact-head CI builds/lints FULL current source,
  including the later raw guard. Verify original backup root1..13 and5new original-store parity
  methods; verify all33new common methods on Native (10gym+3row+10parent+10raw), not just successful
  framework linkage. Update PR description with actual results/pending status.
- Next implementation: SAME-DB bounded transactional user-data replacement + raw3/package1 archive
  guards/reconciliation, retention consent/preview/dataset epoch and active/paused/recoverable run
  protection. Native destructive restore/prune REMAINS DISABLED until those safeguards and tests
  exist. DB development export must filter imported cached WEIGHT rows BEFORE owned-row/size caps;
  the file projection implemented here does not excuse unbounded provider-cache capture.
- Full common app root/navigation/state/native full services/HealthKit/permissions/recording/BLE/
  notifications and REAL iOS app host/runtime remain incomplete. Working Mac/Xcode CI is available.
  Final unfiltered28/35/36+iOS APP and source-bound light/dark/200% visuals remain final-readiness
  work. Physical HealthKit/HC/GPS/BLE/audio/locked-screen/OEM/human TalkBack UNPERFORMED. Keep draft;
  no premature version bump, merge/publication or unrelated feature release.

### Checkpoint83 — raw archive byte framing and real streaming digests

- HEAD/remote82 `b68d71fac96e8bfbd0dccc0f2cbf8fa0155dbadb` pushed successfully to existing draft
  PR24.82 experiment43903195370 FULL SUCCESS, Android133424989147 /Apple133424988141;
  format43903195580 SUCCESS.532unique Android START/END matched, including ALL5new parity cases
  and original root1..13 whole backup test.700189checks/48retained domain suites passed.
- Digest-verified82 JVM14243834289 SHA256d35335a9f75c481e451ec4eac2c3b5da87e6929f6ef8568439115f38f812f13f:
  490domain+16health+409storage. Native14243788358
  SHA256ede90f01a8fd662ea213a38fb5e580ef9ae551a8533c20aa8668cda6f2d01d7e:
  449domain+18health+409storage. ALL0failure/error/skip. All33new common methods matched exact names
  against JVM XML, ignoring ONLY [jvm]/[iosX64] suffix. Proofs scratch/backup-b68-{jvm,native}/
  verification.json, backup-b68-native/new-case-proof.json and backup-b68-runtime-proof.json.
  Real Xcode26.6 Native tests/framework completed8m55s; framework14244337466 is NOT an iOS app.
  PrivateAPK14243769020; no public experimental release.82 source includes raw-row guard, build/lint
  passed remotely, not just the older399 local APK.
- Valid LOCAL83 source is not yet committed: RecordingArchiveReader.kt /RecordingArchiveReaderTest.kt,
  ArchiveSha256.jvm.kt /ArchiveSha256.ios.kt, and updated original raw-export Android helper in
  SharedDetailRetentionArchiveTest. Also ActivityJson/RecordingPointJson DOM entries and their
  existing common parity tests, plus RecordingArchiveRows calls them directly: avoids serialising/
  reparsing every already-parsed point/summary in potentially512MiB archives. Same original domain
  validation/coordinates/totals, no smoothing/training changes. Preserve all. No schema/version/
  backup/permission/library change.
- Adds bounded original8KiB manifest1..3 guard,512MiB exact streamed byte count, UTF8 decoding across
  arbitrary chunk boundaries, original final-newline framing, row graph82 and checksum before any
  valid snapshot is returned. Streams real JVM MessageDigest /Apple CoreCrypto SHA256, including
  explicit cancellation/error/finish cleanup. Does NOT implement ZIP extraction/package1 or grant
  database restore/pruning permission. File adapters still need exact ZIP names/duplicates/expanded
  limits and same-DB transaction/retention/recoverable-run safeguards.
- Added10 common framing/digest cases: published empty/abc vectors, byte-wise/multibyte/CRLF/BOM,
  truncated/invalidUTF8/incorrect hash/length, strict manifests and closed/cancelled reader behaviour.
  Existing2 original raw-export Android cases now additionally stream original archive bytes in
  seven-byte chunks against their REAL original manifest/checksum and compare complete row graphs.
-83 first local gate PASS:419storage JVM0failure/error/skip +metadata+Android test compilation
  4m56s/64tasks, scratch/kmp-raw-stream-final.log. FINAL DOM optimisation gate also PASS4m43s/64tasks:
 419JVM0failure/error/skip, shared metadata and Android test compilation, scratch/kmp-raw-stream-
  dom-final.log. No known current local failure. Formatting809sources(714Kotlin95Java) PASS;3source-provenance
  tests PASS. Native streaming crypto is not yet compiled/tested; next exact-head macOS CI must
  verify it. Do not describe untested new native source as working iOS code.
- NEXT:83 local FINAL419/metadata/Android test compile/809format is verified;82 CI is also
  FULLY verified. Commit/push coherent83+handoff now to EXISTING draft PR24; verify new
  Native419/Android532/format/build/lint results and all10new common methods using downloaded XML.
  Continue full raw3/package1 container/transactional replacement/retention protection, then actual
  common application/native services/iOS host/runtime. No early merge/publication; PR24 stays draft.

### Checkpoint84 — original complete-package v1 file integrity

- HEAD/remote83 `3ce0116db5fa65fc0e20a97d1ee3cb4868d1adf9` committed/pushed to existing DRAFT
  PR24.83 experiment43906545019 FULL SUCCESS, Android133436807431 /Apple133436806431;
  format43906545248 SUCCESS.532unique Android START/END matched, all5parity methods plus root1..13
  whole-parent case confirmed.700189checks/48domain suites passed. PR description needs latest84
  source/pending status after push; do not leave it claiming83 is still running.
-83 digest-verified Native14248075176 SHA256733d7667abe76357b17358d83acefffa5663f443b2929a469956ef2fe2786665:
  449domain+18health+419storage. JVM14245899887
  SHA256b6bb154b11164717512fef31a89547f3f8c13ad5fa5a6d97a041851e9f4806c5:
  490domain+16health+419storage. ALL0failure/error/skip. ALL10new streaming digest/framing tests
  matched exact JVM/Native method names, along with previous33 and9existing Activity/point DOM
  tests; real Apple CoreCrypto incremental context/cleanup is now VERIFIED. Proofs scratch/
  raw-stream-3ce-results/{verification,case-proof,runtime-proof}.json. Framework14247935170 NOT an
  iOS app; privateAPK14247560270. No public experimental build.
-84 source committed locally as2685205 (not reset/amended): CompleteBackupManifest.kt /CompleteBackupManifestTest.kt and1new
  original Android export parity method in SharedBackupEnvelopeTest. Preserve all. No bump to
  version/code/schema/root13/raw3/package1; no dependencies/permissions, writes or publication.
- New v1 manifest checks original8KiB declaration limit and distinct app32MiB/rawZIP513MiB bounds,
  exact integer lengths and SHA256 identities. Actual extracted payload receipts remain a list:
  missing/duplicate/unknown names cannot disappear through a map overwrite. Private immutable
  expected graph; harmless unknown original manifest metadata stays readable. Validation copies
  receipt input and never replaces app data. BOTH inner documents +ZIP container validation remain
  required; this is not a complete restore/ZIP extractor.
- Added5 common cases and1 original Android CompleteBackup.exportTo parity case. Fixture measures
  REAL extracted bytes/JVM SHA256, verifies original names/checksums, inspects actual app.json with
  the complete parent validator, rejects duplicate receipts and asserts database export unchanged.
- Local5focused common tests0failure/error/skip +shared metadata +Android test compilation
  PASS4m33s/64tasks, scratch/kmp-package-manifest-focused.log. Formatting811sources(716Kotlin95Java)
  and3source-provenance tests PASS. Expected full424storage /533focused Android on next CI remain
  UNVERIFIED.83 already passed local419/metadata/test compile4m43s;82 FULL remote verified above.
- NEXT EXACT:84 source2685205 is stable; focused5/metadata/test compile4m33s/811format verified.
 83 FULL remote419/532/700189/48 plus Native streaming crypto10cases is now VERIFIED. Push84 and
  this bundled recovery update ONCE to existing draft24, then verify new Native424/Android533 plus
  actual complete-package original-export case and5common manifest cases from artifacts/logs.
- NEXT IMPLEMENTATION after84: real platform ZIP extraction with exact allowed entries/duplicates/
  bounds/CRC and raw1..3 Reader83 +complete-package1 guards84; SAME SQLite19 transactional app
  replacement/raw reconciliation and retention consent/preview/dataset epoch/active+paused+
  recoverable-run protection. Source DB export must filter imported WEIGHT caches BEFORE owned
  caps. Existing Android remains authoritative; native destructive replacement/pruning stays OFF.
- Whole common app root/navigation/state/data wiring, full HealthKit/native permissions/recording/
  BLE/notifications and REAL iOS host/app/runtime remain incomplete. Mac/Xcode CI DOES work; no
  infrastructure excuse or framework-as-app claim. Final unfiltered28/35/36+iOS APP/visual gates
  and physical HealthKit/HC/GPS/BLE/audio/locked-screen/OEM/human TalkBack remain unperformed.

### Checkpoint85 — bounded user-owned development-evidence database capture

- Remote84 `297b95696963fbb297db82023a4fd5b11ba0748d` (implementation2685205 plus bundled recovery)
  FULL SUCCESS in43910500616, Apple133450781246 /Android133450781759; format43910500700 SUCCESS.
 533unique Android START/END matched, INCLUDING actual original complete-package exporter method,
  previous5parity cases and root1..13.700189checks/48domain suites passed. No current CI failure.
-84 digest-verified JVM14248961446 SHA2562d14a3cd54e632dde170927921fab11c3a2741fce20d42389af9ea91c63d7928:
  490domain+16health+424storage. Native14247414688
  SHA2569e6f3f51852306d785b3764cb3b452bb0418184a291f6b31427062b10958f9bc:
  449domain+18health+424storage. ALL0failure/error/skip. ALL5new complete-manifest methods matched
  exact JVM/Native names. Proofs scratch/package-297-results/{verification,runtime-case-proof}.json.
  Framework14247329830 NOT an iOS app; privateAPK14247438935. No public experimental publication.
-85 first source committed locally as97d527e; stable batching/parity follow-up also ready to commit: SqliteDevelopmentArchive.kt, DevelopmentArchiveRepositoryContract.kt,
  JdbcDevelopmentArchiveTest.kt /AppleDevelopmentArchiveTest.kt and original development export
  parity extension in SharedBackupEnvelopeTest. No DB/version/backup/permission/dependency change.
- Snapshot scans original coach_evidence in bounded512-row keyset pages within ONE existing DB
  transaction. Payload queries batch up to1MiB, with a larger supported row isolated; <=512bound
  parameters stay below API28SQLite limits. Existing composite-index tuple keysets/row IDs avoid
  per-record lookups and preserve kind/id ordering. Source rows individually preflight against supported whole-file32MiB input size /
  original JSON structural limits; aggregate imported-cache size/count is NEVER an owned limit.
  Imported WEIGHT provider filtering happens BEFORE owned100000-row/32MiB budget and typed external
  archive decoding. Matches original optString(provider) export semantics, including omission of
  incomplete cached payloads. Only owned rows receive typed validation; caches are NOT deleted.
- Preserves original SQL kind/id order, exact identities, manual measurements, unknown legacy
  payload fields, historical PBs/links/decisions and retained-source semantics. No new association,
  actual sets, programme activation, stale-evidence interpretation or source rewrite by reading.
- Added3 REAL SQLite contracts shared by JVM/Apple. Synthetic100001imported-weight cache exceeds
  BOTH owned count and aggregate-byte caps, yet manual export succeeds and ALL100002rows remain.
  Corrupt manual data rejects without writes; incomplete imported cached measurements omit exactly
  as original; legacy optional links and unknown metadata/order remain intact. Original Android
  seven-kind export test ALSO compares actual shared DB snapshots with original JSON; adds513
  cached rows to exercise Android multi-page tuple SQL, keeps original2weights assertion first,
  then asserts all515weights remain and exported owned data is unchanged.3common contracts also
  cover514records across a kind/page boundary. No assertions removed or weakened.
- Local focused3JDBC cases PASS0failure/error/skip (6.281s runtime), shared metadata+Android test
  compilation PASS5m7s/64tasks, scratch/kmp-owned-evidence-focused.log. FINAL batching gate PASS:
  3JDBC0failure/error/skip(2.897s), metadata+Android test compilation4m25s/64tasks in
  scratch/kmp-owned-evidence-batch-focused.log. Final Android multi-page assertion compile PASS
  1m30s/55tasks in kmp-owned-evidence-android-page-compile.log. Formatting815sources(720Kotlin95Java),
  exact schema1..19 and3source-provenance tests PASS. No new schema/version. Final release matrix
  remains final-readiness work; no need to rerun unchanged phase84.
- NEXT EXACT:84 is now FULLY verified424storage/533Android/package cases. Commit stable85 batched
  source+final handoff (preserve97d527e), then push BOTH85 commits ONCE to same draft24. New expected
  427storage per platform /533Android remains UNVERIFIED until fresh CI. Verify actual
  Native3SQLite contract methods (including large cache fixture) plus existing Android7-kind parity
  extension from fresh completed artifacts/logs. Fix real failures; no assertions weakened/reruns.
- Next implementation: real ZIP extraction/container guards and same-DB app/raw replacement with
  retention consent/preview/dataset epoch/active+paused+recoverable protection. Use COMPLETE parent82,
  byte/hash/row reader83 and package manifest84; owned development capture85 now exists, do not redo
  or feed unbounded caches into a bounded file decoder. Native destructive restore/pruning stays OFF.
- Full common app root/navigation/state/data, full HealthKit/native permissions/recording/BLE/
  notifications and REAL iOS host/app/runtime remain incomplete. Working Mac/Xcode CI is available.
  Final unfiltered28/35/36+iOS APP +source-bound light/dark/200% gates and physical HK/HC/GPS/BLE/
  audio/locked-screen/OEM/TalkBack acceptance remain unperformed. No early bump, merge/publication.

### Checkpoint86 — recover Android parity assertion placement after workspace interruption

- Current branch experiment/android-ios-kmp-compose, remote85 HEAD23c92bc6c3c55cfc76dd6ac8e8304ee35c83ab79;
  existing PR24 OPEN/DRAFT. Preserve ALL82–85 source. Main85e1c39 /owner PR23 untouched. No reset,
  amend, replacement PR, version/schema/format/permission change or experimental public release.
-85 experiment43917103613: Apple133474093470 SUCCESS; Android133474093999 FAILURE. Formatting
 43917103264 SUCCESS. Android completed533unique START/END cases; exactly ONE failed method:
  SharedBackupEnvelopeTest#sharedSupportArchiveMatchesOriginalSavedRunningEvidenceWithoutSideEffects.
  Do NOT describe85 as full Android success.700189checks/48domain suites and build/lint/schema passed.
- Confirmed root cause is TEST PLACEMENT, not production data loss: new SqliteDevelopmentArchive
  comparison was accidentally inserted after the SUPPORT fixture's7record assertion, comparing
  support-v1 rows with empty development-v1 records. Actual CI expected-support/actual-development
  JSON and source lines374–382 demonstrate it. Corrected by moving the IDENTICAL8-line comparison
  into developmentArchiveMatchesOriginalKindsAndManualWeightExportFilter immediately after its
  DevelopmentArchiveJson decode/7record check. ALL original/new assertions remain;513-cache paging
  and515retained-weight checks stay. No production logic/calculations/limits or test counts changed.
- Local relocation survived environment restart. Final Android test compilation PASS1m31s/55tasks,
  scratch/kmp-owned-evidence-parity-fix-compile.log;815format sources(720Kotlin95Java) PASS and
  git diff --check clean. No local KVM, so no claim of local instrumentation. Fix is supported by
  actual failed runtime fixture +exact relocation diff; fresh corrected-head CI must run methods.
-85 digest-verified JVM14253956408 SHA256db4a5d4a99b255b4e32e72cffc53d5528e194ec497028d199ceeafe77a051b2a:
  490domain+16health+427storage. Native14252242984
  SHA2569ca675aa9f16de295bcb1d989e1a17a384dcada8be8fc568a7b49d9a3772d72b:
  449domain+18health+427storage. ALL0failure/error/skip. ALL3new REAL Apple SQLite contract names
  matched local JVM XML, including100001-cache /514cross-kind paging fixture; Native runtime17.891s.
  Proofs scratch/owned-23c-{jvm,native}/verification.json, owned-23c-native/cache-case-proof.json.
  Completed failure log scratch/owned-23c-android-job.log. Framework14252302847 NOT an iOS app.
- NEXT EXACT: commit relocation+this recovery note, push existing draft24 normally; verify exact
  new Android533 INCLUDING support/development methods and Native427/JVM427/format/build/lint.
  Do not blindly rerun old23c source or weaken assertions. Update PR description to actual current
  pending/failure/result state.84 remains last FULL remote until corrected86 CI passes.
- Then continue ZIP container/file adapters and same-DB transactional restore/raw reconciliation /
  consent/preview/epoch/active+paused+recoverable safeguards. Complete parent82, raw rows/real streamed
  hash/framing83, package-v1 manifest84 and bounded owned development capture85 are implemented;
  preserve/reuse them. Native destructive restore/prune stays OFF. Full common app root/navigation/
  native services/HealthKit/recording/BLE/notifications and actual iOS host/app/runtime still pending.
  Working Mac/Xcode CI exists; no infrastructure excuse. Final unfiltered28/35/36+iOS APP/source-bound
  visuals and physical HK/HC/GPS/BLE/audio/locked-screen/OEM/TalkBack remain unperformed. Keep draft;
  no premature merge, version bump, public APK or stable tag.

### Checkpoint87 — shared bounded ZIP extraction and original exporter parity

- Current pushed86 `dcf2fb222e20351e9028afabcc8c3bbfa4bb63c7`; experiment43934876478 Android
 133536725255 SUCCESS, Apple133536724689 still RUNNING at this checkpoint. Formatting43934876402
 SUCCESS. Corrected Android533unique START/END matched, BOTH support/development fixture methods
 ran successfully;700189checks/48domain suites. No remaining Android failure demonstrated.
-86 digest-verified JVM14260644377 SHA2562ef10a81092ef0a94b6ec939fa9188af3d47f70ac23f87c48b0ded71b6c266a5:
 490domain+16health+427storage, ALL0failure/error/skip. Proof scratch/owned-dcf-jvm/verification.json;
 actual corrected runtime log scratch/owned-dcf-android-job.log. Apple86 needs actual confirmation.
-87 adds BackupZipArchive + real JVM Inflater/Apple zlib raw-DEFLATE implementations and10common
 regression cases. Exact known entries/counts, bounded physical/expanded lengths, duplicate/path/
 encrypted/unsupported rejection, central/local/descriptor agreement, overlap protection, CRC32,
 stored/deflated/ZIP64 support, short source reads, large-output draining, cancellation cleanup and
 changed/truncated source rejection. Extraction emits fresh <=32KiB blocks ONLY into private staging;
 it NEVER authorises database replacement. Existing parent/raw/package validators stay mandatory.
- Android existing actual original complete-package/raw archive exporter tests now additionally
 validate/extract through shared ZIP reader and compare bytes/complete raw session graphs with
 original readers. Original assertions remain. Android533expected count unchanged; runtime pending87.
- Local final PASS437storage JVM0failure/error/skip +shared metadata +Android test compilation.
 Workspace restart interrupted first complete Gradle process after tests; recovered run passed
 2m10s/64tasks in scratch/kmp-zip-container-recovered-final.log.10ZIP cases verified in XML.
 Formatting820sources(725Kotlin95Java), exact schema1..19 and3source-provenance tests PASS.
 Initial9/10ZIP run found a test mutation string was15bytes instead of required16; corrected fixture
 name ../bad-files.bin, retaining length assertion. No production assertion weakened.
- IMPORTANT Apple zlib source is NOT yet compiled/tested. Commit/push87 coherently after86 finishes;
 verify real Native437, all10common ZIP cases and533Android original-export parity. Do NOT describe
 this as completed restore/iOS application. No version/schema/backup/dependency/permission change.
- NEXT: private platform staging-file adapters and real container export/validation composition;
 same-DB transactional app/raw restore/reconciliation, consent/preview/epoch and active/paused/
 recoverable safeguards. Native destructive restore/prune remains OFF. Original Android stores
 remain authoritative. Full common app/native services/actual iOS host and final gates remain below.

### Checkpoint88 — private platform staging and complete-package inspection

-87 committed locally as1027712; NOT pushed separately (batch with88 to avoid duplicate costly CI).
 86dcf2fb2 is now FULL SUCCESS in43934876478; format43934876402 SUCCESS. Android133536725255 /
 Apple133536724689 succeeded.533unique Android cases incl BOTH corrected fixtures,700189checks/
 48domain suites. JVM14260644377:490domain+16health+427storage; Apple14262287373:449domain+
 18health+427storage; ALL0failure/error/skip. Apple artifact SHA256
 0cd5dfcc2b040f37d152973583d30711f47845a8e19ad5fd1dfb474e908c998b. Proofs scratch/owned-dcf-
 {jvm,native}/verification.json;11development archive/SQLite method names matched across platforms.
 Framework14260694984 NOT an iOS app; privateAPK14261806624. No public experimental publication.
-88 in-progress files: BackupStagingFile.kt, BackupScratch.jvm.kt /BackupScratch.ios.kt,
 InspectedBackupPackage.kt, BackupStagingFileContract.kt, CompletePackageFixtures.kt and real
 JvmBackupStagingFileTest /AppleBackupStagingFileTest; original Android complete-export case extended.
 Sources preserved locally. No version/schema/format/permission/dependency bump or native data writes.
- Private app-directory files use exclusive creation and immediate unlink with open handles, so
 cancellation/crashes cannot leave named scratch backups. Common wrapper bounds32KiB blocks and
 supported format bytes, seals before reads, prevents later writes, closes on failure and handles
 repeated close. JVM uses original Android-compatible RandomAccessFile; Apple uses real POSIX IO.
 Caller must supply app-private directory and serialize operations. These are temporary scratch,
 NOT durable/exported backups and never database files.
- Complete inspection composes original outer ZIP/manifest +ACTUALLY measured SHA256/lengths,
 complete app parent1..13 validation and nested raw ZIP1..3/manifest/hash/session-graph validation.
 App payload alone has bounded32MiB allocation; raw ZIP remains in bounded private scratch until
 close. Both inner validators must succeed before returning any checked object. No replacement,
 raw merge, retention cleanup or activation is enabled. All original Android parity assertions stay.
- Focused first6real JVM staging contracts PASS0failure/error/skip, metadata PASS1m45s/13tasks,
 scratch/kmp-backup-staging-focused.log. Added5complete-package contracts with independently measured
 Python standard-library ZIP/hash fixtures: valid original root1/recoverable session, wrong outer
 hashes/lengths, invalid app graph/future14, wrong inner raw hash/invalid graph, failure cleanup and
 later valid inspect. Android existing original COMPLETE exporter also validates whole composition.
- Final local448storage JVM0failure/error/skip +metadata +Android test compilation PASS5m30s/64tasks in
 scratch/kmp-backup-package-staging-final.log. Formatting828sources(733Kotlin95Java), schema1..19 /
 3source-provenance PASS. Native ZIP zlib AND scratch implementation still UNVERIFIED until next
 actual Apple CI. Do not call these an actual functioning iOS app or completed restore.
- NEXT EXACT: local final run is verified448/metadata/Android compilation/828format;
 commit88+this recovery and push87+88 ONCE to existing DRAFT24. Verify448storage on JVM/Apple,
 all10ZIP+11real staging/package cases and533Android original-export parity. Update PR body honestly.
 Then complete container export and same-DB transactional restore/raw reconciliation with retention
 consent/preview/epoch/active+paused+recoverable protections. Native destructive operations OFF.
 Full common app/services/actual iOS host/build/runtime and final28/35/36+iOS APP/visual gates remain.

### Checkpoint89 — portable streaming ZIP container export (in progress)

-87/88 pushed together normally; current remote88 is5b2c3715432f9ffae9335d7d64223d17fc34f854;
 PR24 still OPEN/DRAFT and body updated with actual86 pass/local88/pending Native status.
 88experiment43940777101 RUNNING (Android133557392264 /Apple133557393362);
 format43940776911 SUCCESS. Do NOT cancel this expensive Native verification just for89.
 Expected88:448storage JVM/Native,533Android; exact real runtime results still pending.
-88 final local gate actually PASSED5m30s/64tasks:448JVM0failure/error/skip, shared metadata,
 Android test compilation.828formatted sources, schema1..19 /3source-provenance PASS.86 full remote
 533Android/427JVM+Native/700189checks48domain suites remains last completely verified remote.
-89 local new BackupZipWriter.kt +BackupZipWriterTest.kt, shared limits helper extracted from
 BackupZipArchive.kt, original Android complete-export test extended. Preserve ALL changes.
 Streaming STORED ZIP output uses original names and size budgets, bounded32KiB fresh blocks,
 stable source-size/CRC checks across both passes, original local/central/EOCD headers and no new
 dependencies. ZIP64 output is unnecessary under existing <600MiB budgets; ZIP64 input remains
 supported87. Caller output remains private until all payload/document validation succeeds.
-5common cases test exact original raw/manifest byte preservation, complete nested payload
 preservation, missing/duplicate/unknown/oversize rejection before output, truncation/overproduction,
 source mutation and immediate sink cancellation. Original Android exporter payloads are repacked
 by shared writer then independently read using java.util.zip.ZipInputStream, compared byte-for-
 byte and revalidated through complete inspection. No original assertions/counts removed.
- FINAL local453storage JVM0failure/error/skip +metadata +Android test compilation PASS5m31s/64tasks in
 scratch/kmp-backup-zip-writer-final.log. Formatting830sources(735Kotlin95Java), exact schema1..19
 and3source-provenance PASS, scratch/kmp-backup-writer-{format,safety}.log. Do not claim pending
 writer tests or new Native ZIP/POSIX runtime passed. No destructive replacement/prune enabled.
- NEXT: check actual453/local final result and88CI; fix real failures. Commit89 coherently with
 this recovery note when local gates pass. Push once88actual Native/Android gate completes so it
 is not cancelled. Verify new453JVM/Native, all writer cases and533Android original-reader parity.
 Then continue bounded DB raw export, same-DB transactional application/raw replacement and
 compatible-detail merge/retention consent/preview/epoch/unfinished recording protections. Existing
 Android stores remain authoritative. Full app/native services/real iOS host and final gates remain.

### Checkpoint90 — correct real Apple zlib C-string binding failure

-89source committed locally as9c2e628295c578cb23a06cdb654b393a255f49db (not pushed yet, so88runtime
 is not cancelled). FINAL writer gate PASS5m31s/64tasks:453storage JVM0failure/error/skip, shared
 metadata and Android test compilation.830format sources(735Kotlin95Java), schema1..19 /
 3source-provenance PASS. No JVM/Android production change after this successful gate.
-88experiment43940777101 Apple133557393362 FAILED at native compilation; Android133557392264
 is still RUNNING. Do NOT call88green. Actual log scratch/backup-5b-native-job.log confirms ONE
 reported source error at BackupInflate.ios.kt:16: zlibVersion() returns CPointer<ByteVar>?, but
 inflateInit2_ expects String?. No Native storage runtime tests have passed for87–89 yet.
-90 source correction is COMMITTED LOCALLY as3e11089 (89writer9c2e628 is also committed locally).
 Remote remains88HEAD5b2c371; do not lose these unpushed commits. Working tree source is clean.
 Focused source correction: requireNotNull(zlibVersion()).toKString() before passing the version
 string to inflateInit2_. This is the actual SDK binding conversion, not a filtering/test/algorithm
 change. Decompression, limits, CRC/byte checks, cleanup and all assertions remain unchanged.
 Existing JVM453/metadata/Android compile results remain valid; real corrected Native compilation/
 runtime still requires fresh CI. Formatter/source checks are recorded in scratch/
 kmp-backup-zlib-version-format.log:830sources (735Kotlin95Java) PASS.
- NEXT EXACT: formatter PASS and source correction committed. Let actual
 Android88 job finish and inspect its results/logs. Fix any genuine additional failure. Push local
 89+90 ONCE to same DRAFT24 normally, then verify corrected Apple453storage (10ZIP+11staging/package+
 5writer cases), JVM453/Android533/original Java reader parity/domain/build/lint/format. This is a
 targeted response to a demonstrated compiler failure; do NOT blindly rerun5b2c371 unchanged.
- After verified IO, continue bounded owned raw export and same-DB transactional restore/raw merge,
 retention consent/preview/epoch/active+paused+recoverable safeguards, real common app composition /
 native services /actual iOS host/build/runtime, final unfiltered28/35/36+iOS APP and genuine visuals.
 No version/schema/backup/permission change, destructive native restore/pruning, merge or publication.

### Checkpoint91 — bounded shared raw-recording database export (in progress)

- All saved89/90/recovery commits pushed normally; current remote7e8cdaaec840b80f3886540e282e2ee62a7487de.
 PR24 remains OPEN/DRAFT.90corrected experiment43943796128 RUNNING, Android133567945066 /
 Apple133567945467; format43943795945 SUCCESS. Native453 and new writer interoperability runtime
 remain UNVERIFIED until actual completion. Do not cancel this run for91.
-88 completed Android133557392264 SUCCESS:533unique START/END matched,700189checks/48domain suites;
 original raw/complete exporter, full private-package inspection and original no-write assertions
 passed. Actual log scratch/backup-5b-android-job.log.88overall remains FAILURE because native zlib
 version binding did not compile;90fix is now pushed, not merely local.86 last FULL green remote.
-90Android133567945066 is now SUCCESS:533unique START/END matched, original complete writer/Java
 reader/full-inspection method ran,700189checks/48domain suites, build/lint/schema PASS. Actual log
 scratch/backup-7e-android-job.log. Digest-verified JVM14265227864 SHA256
 bb3c24b7b0b74e53ecac51712e960d9f6067ca79d75c34c06b30a6714d76aba6:490domain+16health+453storage,
 ALL0failure/error/skip. Proof scratch/backup-7e-jvm/verification.json. Apple90 still pending;
 do not infer Native success from Android/JVM.91 source committed locally as51fa65a (not pushed
 while90Native runs). Local final458/metadata/Android compilation actually PASS3m39s/64tasks.
-91 new local files: SqliteRecordingArchive.kt, RecordingArchiveRepositoryContract.kt,
 JdbcRecordingArchiveTest.kt /AppleRecordingArchiveTest.kt. Original Android SharedDetailRetention-
 ArchiveTest helper also compares complete shared DB-export rows with original exporter JSON and
 verifies actual shared manifest/hash/graph and unchanged SQL total_changes. All old assertions stay.
- Shared raw-v3 export captures ONE existing SQLite19 transaction,512-session keysets and512-row
 dense metadata pages. Payload batches bound1MiB (one larger supported row alone), <=513SQL bind
 parameters. Preflight payload lengths before materializing text. Strict existing row/graph/byte/
 source/clock limits and real platform SHA256 precede sealed private STORED ZIP output. Unknown
 payload fields, exact Long clocks, accepted coordinates, events, summary totals and removal proofs
 remain; active/paused/recoverable states are NOT silently completed or coerced. No data writes,
 pruning, replacement, new consent, schema/version/format/dependency/permission change.
-5focused REAL JDBC contracts PASS0failure/error/skip (1.169s runtime), shared metadata PASS51s/
 13tasks in scratch/kmp-recording-export-focused-fixed.log. First compile found test fixture omitted
 existing required SqliteActivities clock; injected deterministic { at }, preserving all assertions.
 Focused fixtures cover514sessions/514points/514events crossing count AND1MiB batch boundaries,
 unknown metadata, read-only transaction coverage, unfinished states/context, removal receipts,
 invalid/deep/oversized payload rejection without mutation, recovery after rejection, empty archive
 and rollback of caller's uncommitted outer transaction. Actual Apple counterparts await next CI.
- FINAL local458storage JVM0failure/error/skip +metadata +Android test compilation PASS3m39s/64tasks in scratch/
 kmp-recording-export-final.log. Formatting834sources(739Kotlin95Java), exact schema1..19 and
 3source-provenance tests PASS; scratch/kmp-recording-export-{format,safety}.log. Brief executor
 disconnect recovered; source persisted and focused run actually completed, no guessed test pass.
- NEXT EXACT: inspect final458/local compilation result and current90CI. Verify actual453Native/
 JVM plus533Android and all10ZIP+11staging/package+5writer methods using artifact XML/digests.
 Helper scratch/verify_backup_io_cases.py checks exact JVM/Native method names. Fix genuine failures.
 Commit91+this handoff after local gates pass, push ONCE after90 finishes (do not cancel it), verify
 new Native/JVM458 and original Android database-export parity. Then continue COMPLETE app snapshot
 capture/export and same-DB transactional app/raw replacement/reconciliation with consent/preview/
 dataset epoch and active+paused+recoverable protections. Native destructive operations remain OFF.
 Full common app composition/native services/REAL iOS host/runtime and final gates remain below.

### Checkpoint92 — verified real Apple backup IO; queue91 database-export runtime

-90HEAD7e8cdaaec840b80f3886540e282e2ee62a7487de FULL SUCCESS in43943796128,
 Android133567945066 /Apple133567945467; format43943795945 SUCCESS.533unique Android cases,
 700189checks/48domain suites, build/lint/schema passed. Actual original Java ZIP reader consumed
 shared writer output and complete private-package validation; original assertions remain intact.
- Digest-verified JVM14265227864 SHA256bb3c24b7b0b74e53ecac51712e960d9f6067ca79d75c34c06b30a6714d76aba6:
 490domain+16health+453storage. Apple14264928564
 SHA256bd9d8331044dcddf595893d6a10dd1b94f866fbd67179fb58d2aed7a194ed1d3:
 449domain+18health+453storage. ALL0failure/error/skip. ALL26new common IO cases matched exact
 JVM/Native method names:10ZIP+11real private-staging/package+5writer. Real Apple zlib and POSIX
 IO now VERIFIED, not just compiled source. Proofs scratch/backup-7e-{jvm,native}/verification.json
 and native/io-case-proof.json. A framework14263444345 is still NOT an iOS application.
- PrivateAPK14265247742 ZIPdigest d7367c047789f28a6741d38d9577d51b7780a9b1040b217756825a29fa954393
 verified; APK58123531bytes/SHA2566e1466567fca41414d468323445c908571f44496fafbe380499ff42d9e8a4616,
 code19/version0.12.3-test1/minAPI28/noINTERNET. Proof scratch/backup-7e-apk/verification.json.
 No experimental public publication or complete-iOS-app claim.
-91read-only raw database export is committed51fa65a. Local5new JDBC cases/meta PASS51s with1.169s
 test runtime; full458storage0failure/error/skip +metadata+Android test compilation PASS3m39s/64tasks.
 Formatting834sources(739Kotlin95Java), exact schema1..19 and3source-provenance PASS. Original Android
 database raw-export parity extension is compiled, not yet run. Expected next Native/JVM458 and
 Android533 need actual fresh CI; do not confuse them with verified90Native453.
- NEXT: push91+this recovery ONCE to existing draft24; update PR with90proof/new91pending state.
 Verify5new actual Apple SQLite export contracts and original Android raw export parity from fresh
 artifacts/logs. Do not rerun old7e unnecessarily. No destructive restore/pruning is enabled.
- Next implementation after91: complete application snapshot capture/export over same SQLite19,
 then transactional app/raw replacement/compatible detail merge +consent/preview/epoch/unfinished
 protections. Original capture reference is app/src/main/java/org/racedaydreams/data/
 SqliteBackupRepository.java#exportUserData; parent decoder is UserBackupDataJson82. Filter owned
 activities BEFORE source budgets/hydration (do not accidentally count imported health caches);
 preserve original rdd/gpx app-JSON sample stripping, laps/PBs, annotations and planned links.
 Raw detail is preserved separately by91. No duplicate database, generic bucket or invented data.
- Full common app composition/native services/permissions/HealthKit/recording/BLE/notifications /
 actual iOS host/entitlements/build/runtime and final unfiltered28/35/36+iOS APP/source-bound visual
 gates remain incomplete. Physical HK/HC/GPS/BLE/locked-screen/audio/TalkBack remain unperformed.
 No version/schema/backup/permission/dependency bump, premature merge or public development build.

### Checkpoint93 — actual raw-export platform results and original-reader parity correction

-92/91HEADa1c66d111e6dceae3c65b258948e04e3586b7ac7 experiment43946970369 is OVERALL FAILURE:
 Apple133579093220 SUCCESS; Android133579093429 completed533unique cases with ONE failed method,
 SharedDetailRetentionArchiveTest#sharedRawRowsKeepOriginalRemovedDetailProvenanceWithoutInventingRouteEvidence.
 Formatting43946970454 SUCCESS.700189checks/48domain suites/build/lint/schema PASS. No public APK.
- Actual expected/actual row arrays differ only in numeric spelling: original JSONObject emits
 distanceMetres:100 while the stored Kotlin JSON payload remains100.0. JsonPrimitive equality
 compares token text. Both represent the same measurement to the original reader; stored points,
 source clocks, receipts, totals and full row graphs are unchanged. This is a parity-test contract
 mismatch, NOT a demonstrated training-data/GPS calculation error. Log scratch/recording-a1-android-job.log.
- Fix test comparison by passing BOTH full row arrays through the ACTUAL original JSONObject reader
 and round-trip output; keep complete field/order/count/type comparison and every prior graph /
 no-mutation assertion. Two reader passes stabilize legacy -0 output becoming integral0 on read.
 New explicit Android guard proves numeric100/100.0 equivalence while changed measurement
 100.00000000000001, quoted100, exact clock9007199254740992 vs9007199254740993 and extra:null
 remain distinguishable. Also checks -0/-0.0. No production raw-export numbers or assertions about
 retained activity/point/removal evidence changed. New expected unique Android count534.
- Digest-verified92/91 JVM14266851323 SHA256b33f63763cd763cdc8b727692f30721049ec23c24716be7e008a95883799e523:
 490domain+16health+458storage. Apple14266251569
 SHA256ead41fa5dfc90800f5a164fc31cb3cc67beff6d8f0012584017a25a3bd9fee17:
 449domain+18health+458storage. ALL0failure/error/skip. ALL5new actual Apple raw-export SQLite
 contracts matched JDBC names, including514sessions/dense payload paging, removal provenance,
 unfinished states and rollback. Proofs scratch/recording-a1-{jvm,native}/verification.json and
 native/sql-case-proof.json. Framework14266621203 is NOT an iOS app.
-93parity correction is compiled in the combined local core gate:464storage0failure/error/skip,
 metadata+Android test compilation PASS4m37s/64tasks, scratch/kmp-core-capture-final.log. No local
 KVM: new Android guard/fixed comparison runtime still require fresh CI. No blind a1 rerun.
-94 core source is local and tested, preserve it; commit93test correction first, then core94 with
 complete handoff; push BOTH together once. Main85e1c39 /owner PR23 untouched; PR24 remains draft.

### Checkpoint94 — typed read-only core database capture

-93parity test fix committed7feaf5a.94source files: SqliteCoreBackup.kt, CoreBackupRepository-
 Contract.kt, JdbcCoreBackupTest.kt /AppleCoreBackupTest.kt; SqliteActivities summary projection
 extracted verbatim into readAnnotatedActivity, preserving notes/RPE/shoe/planned-link semantics.
 Original Android core-guard/old-current tests additionally compare ALL15core fields through the
 actual original exporter/JSONObject reader and assert SQL total_changes unchanged. No SDK/IO,
 engine, profile, history, actuals, settings or consent writes were introduced.
- Core capture reads the SAME SQLite19 connection inside one transaction. Imported activity
 providers are filtered BEFORE COUNT/payload load/owned byte checks.512-row keyset metadata pages,
 byte-bounded payload batches and <=512parameters; source syntax/individual supported32MiB bounds
 precede decoding. Whole-core output obeys original32MiB/100000array/depth/string budgets.
- Source data is captured faithfully: optional profile/draft/baseline timestamps, original annotation
 overrides and row ordering, normalized default-shoe authority, manual results, live/preserved PB
 precedence and source identity, exact revisions, settings, assessments, opaque plan/adjustment
 metadata. rdd/gpx inline samples are stripped ONLY from app-JSON summaries as Android already does;
 raw detail remains in original source and91companion export. Legacy inline source aggregates may
 exceed32MiB if compact summaries fit; no premature aggregate source cap. NULL remains unknown.
- Annotation source-pair ordering uses original SQLite ORDER BY, not a concatenated key (embedded
 NULs can collide). Activity date sorting is stable over SQLite ID order, preserving astral/BMP
 ordering rather than Kotlin UTF-16 tie sorting. Validates both on real JDBC/Apple contracts.
-6focused JDBC contracts PASS0failure/error/skip (2.493s initial runtime), metadata PASS1m32s/
 13tasks, scratch/kmp-core-capture-focused.log. Tests: Strength-only absent running profile/read-only
 transaction;100001import caches over owned row+byte budgets retained; legacy inline routes >32MiB
 but compact valid export; actual annotations/default shoes/PB precedence; Unicode/NUL identities;
 corrupt/oversized owned data fail without mutation and subsequent valid capture succeeds.
- FINAL local464storage JVM0failure/error/skip +shared metadata +Android test compilation
 PASS4m37s/64tasks, scratch/kmp-core-capture-final.log.838format sources(743Kotlin95Java), exact
 schema1..19 and3source-provenance PASS; scratch/kmp-core-capture-format-ready.log and safety-ready.log.
 No new canonical screenshots required DURING this read-only storage development; final-source full
 app screenshots/iOS APP gates still mandatory before acceptance. New Native6/Android534 pending.
- IMPORTANT this captures CORE ONLY. Do NOT pass core.data to currentElement as a complete new
 database export: old-backup default filling would invent empty missing feature sections. Capture
 ALL existing feature sections in the same transaction, then run UserBackupDataJson82/parent13
 validation. Native destructive restore/pruning remains OFF, Android stores remain authoritative.
- NEXT EXACT: commit94+this recovery; push93+94 ONCE to existing DRAFT24 (prior91gate is completed,
 Apple458pass, Android one numeric-token parity mismatch). Verify fresh Native/JVM464 and actual
 Android534 including corrected raw original-reader parity and both new core-capture comparisons.
 Fix genuine failures; do not blindly rerun unchanged a1. Then compose COMPLETE typed application
 snapshot/export and implement protected transactional restore/raw reconciliation before full
 native application/root/services/iOS host/runtime and final unfiltered28/35/36+iOS APP/visual gates.
 No version/schema/root13/raw3/package1/dependency/permission change, merge or publication.

### Checkpoint95 — complete current application snapshot composition (local verification)

-94pushed3fdba944d66f22d53d426f0fe2f5a69f052ffaa0 is FULL SUCCESS in experiment43951817449,
 Android133595983385 /Apple133595983878; formatting43951817321 SUCCESS.534unique Android
 START/END matched, including fixed raw JSON parity, new exact-clock/type/measurement guard and
 both actual core-capture comparisons.700189checks/48domain suites +build/lint/schema PASS.
-94 digest-verified JVM14268818911 SHA25630d54cd347344baa7d59c8122baf897be15d83369dfdc466787d10f3237bbb6d:
 490domain+16health+464storage. Apple14272306468
 SHA256c1dcbe1117e63cc319801cade1be6ec04a98c9bd860924d11a9e93d63f7f86c7:
 449domain+18health+464storage. ALL0failure/error/skip. All37new common IO/SQL names match across
 platforms:26ZIP/staging/writer +5raw SQL +6core SQL. Includes actual Apple100001-cache, >32MiB
 legacy inline-route source and Unicode/NUL identity cases. Proofs scratch/core-3fd-{jvm,native}/
 verification.json, native/all-io-case-proof.json; Android log scratch/core-3fd-android-job.log.
 Framework14271226751 NOT an iOS app; privateAPK14272391191. No public experimental release.
-95 local source: SqliteApplicationBackup.kt, ApplicationBackupRepositoryContract.kt and real
 JdbcApplicationBackupTest.kt /AppleApplicationBackupTest.kt; Android SharedBackupEnvelopeTest
 adds full-original-export comparison after core and body/achievement/choice fixtures. Preserve ALL.
- Explicitly captures core AND every existing section in ONE existing SQLite19 transaction:
 coaching, owned development, choices/exclusions, immutable legacy and authored programming/drafts/
 custom definitions/decisions/current references, frozen workouts/executions/actual edit metadata /
 associations, running support/0.8 records, timers, achievements, body measurements and retention.
 All current sections exist BEFORE full UserBackupDataJson graph validation; old-file empty default
 filling is NOT used to omit current data. No read creates programme IDs/timestamps, awards or
 activations. Source read callbacks deliberately reject attempts to use command-only clock/UUID.
- Original programming lower-bound/row-count budgets precede hydration. Per-feature actual bytes
 and final whole-parent32MiB/syntax limits apply. Imported weight/activity caches remain omitted
 without deletion; raw detail stays in91companion. encode(createdAt) emits original parent13 only
 after complete validation; this is still NOT replacement, cleanup, or a complete ZIP package export.
-6focused REAL JDBC tests PASS0failure/error/skip (0.685s runtime), metadata PASS48s/13tasks in
 scratch/kmp-application-capture-graph-focused.log. Cases cover all explicit sections/current13
 roundtrip, real finished sets+legacy+exclusions+height+awards, authored revision+incomplete draft,
 unaccepted valid actual replacement, early oversized gym rejection, read-only outer rollback.
 First test compile corrected prohibited object wildcard import. Initial graph rejection fixture
 wrongly expected removal of canonical definition cache to fail; original catalogue fallback is
 valid. It now explicitly ACCEPTS that supported case, then rejects a valid-but-unaccepted changed
 actual which disagrees with retained execution evidence. No production guard was weakened/tightened.
- FINAL local470storage JVM0failure/error/skip +metadata +Android test compilation PASS3m51s/64tasks in
 scratch/kmp-application-capture-final.log.842format sources(747Kotlin95Java) PASS in
 scratch/kmp-application-capture-format.log. New actual Apple6 and application-wide original Android
 parity are UNVERIFIED until fresh CI. No change to version/code/schema/root13/raw3/package1 /
 dependencies/permissions; native destructive restore/prune OFF. Current PR24 still draft.
- NEXT EXACT: inspect final local470/meta/Android compile log for actual result; finish schema/source
 checks. Fix real failures, commit95+this recovery and push ONCE (94run already finished and verified).
 Verify fresh Native/JVM470, new6application cases and Android534 full app-source parity; no blind
 reruns. Then compose COMPLETE ZIP export using typed app capture+91raw capture under the SAME outer
 transaction and original unfinished-recording guards. Continue protected transactional app/raw
 restore/reconciliation+consent/preview/epoch protections, real common app/native services/iOS host
 and final unfiltered28/35/36+iOS APP/source-bound visual gates. No early merge/publication.

### Checkpoint96 — complete package export from one shared database snapshot (local)

-95typed app capture committed/pushed0ae10b4ce3b99a941704ad9964887c8f84dda2e6; existing DRAFT24.
 Current experiment43956465891 RUNNING, Android133612208709 /Apple133612209451. Formatting
 43956465677 FAILED during PINNED TOOL DOWNLOAD: actual Maven HTTP429 Too Many Requests, not a
 source-formatting diff. All13formatter self-tests passed before fetch. Original local842format
 passed; failure log scratch/application-0ae-format-job.log. Required formatting still needs a
 clean current-head run; do not mark failed CI green or bypass its hash/content lock. No workflow /
 owner PR23 changes or blind rerun. Next changed-source head gets a normal fresh required gate.
-95experiment43956465891 is now SUCCESS: Android133612208709 andApple133612209451.534unique
 Android START/END matched, all full-original app capture comparisons (empty/core/body/awards/
 choices) ran,700189checks/48domain suites/build/lint/schema PASS. Formatter HTTP429 remains a
 separate REQUIRED failed check, so do NOT call95all-required-green. Actual Android log
 scratch/application-0ae-android-job.log.
-95digest-verified JVM14271899714 SHA2562a78c6f7177ddfbd2efc115e76a393a67a82acc8a8ac6b6bb0ea646977d748f8:
 490domain+16health+470storage. Apple14271184309
 SHA2565cf9ed40a725c4218fa52507c378c60120f6580bd8176572592be300ba7be880:
 449domain+18health+470storage. ALL0failure/error/skip. All43new IO/SQLite case names match across
 platforms, including6complete app capture cases. Proofs scratch/application-0ae-{jvm,native}/
 verification.json and native/all-case-proof.json. Framework14272049066 NOT an iOS app;
 privateAPK14272544480. No public experimental publication.
-96new files: SqliteCompleteBackup.kt, CompleteBackupRepositoryContract.kt and real
 JdbcCompleteBackupTest.kt /AppleCompleteBackupTest.kt. Android original complete-package method
 now also reads actual shared DATABASE package export via java.util.zip, verifies real byte lengths
 /Java SHA256, validates app.json through ORIGINAL BackupManager.inspect, compares original app data,
 then inspects the full shared package. All previous original-export assertions remain.
- Package1 output explicitly captures95parent13 and91raw3 under ONE outer existing SQLite19
 transaction, with original finish/recover ACTIVE/PAUSED/RECOVERABLE guard. Ordinary JSON/raw
 exports retain independent behaviour. Real SHA256 measures BOTH payloads; original manifest1,
 bounded stored ZIP and sealed private files. Streaming IO uses <=32KiB blocks, closes scratch
 handles on failure/complete/cancellation, and never publishes destinations or changes source data.
 No restore/pruning/gym activation/recording engine, version/schema/format/permission change.
-4focused REAL JDBC package cases PASS0failure/error/skip +metadata1m24s/13tasks in
 scratch/kmp-complete-export-focused.log. Actual nested complete inspector validates produced
 payloads and exact coordinates/Long clocks, truthful summary-vs-raw separation, unfinished run
 blocking with standalone raw still usable, one read-only snapshot/caller rollback and recovery
 after failed private staging. New actual Apple4 and Android original-package parity pending.
- FINAL local474storage JVM0failure/error/skip +metadata +Android test compilation PASS3m7s/64tasks in scratch/kmp-complete-export-
 final.log; local formatting846sources(751Kotlin95Java) and schema/source checks PASS, recorded
 in scratch/kmp-complete-export-{format,safety}.log. Do not claim pending tests as passed.
- NEXT EXACT: inspect final local log and95actualCI before next push. Verify current Native/JVM470
 and Android534/full application parity; inspect any failure root cause. Then commit96+handoff and
 push once (avoid cancelling prior expensive run). Verify fresh Native/JVM474, new4real SQLite
 package cases and original Android package reader parity; mandatory formatting must pass.
- After complete export verification, implement transactional application replacement/compatible raw
 restoration and retention consent/preview/dataset epoch/active+paused+recoverable protections.
 Native destructive restore/prune remains OFF until complete safeguards/test coverage. Then finish
 real common app composition/native services/actual iOS host/runtime and final unfiltered28/35/36 /
 iOS APP/source-bound visuals. No reset, early merge/publication or stable tag.

### Checkpoint97 — protected restore population and exact retained-detail reconciliation

- Resumed actual branch `experiment/android-ios-kmp-compose` / DRAFT24 from clean pushed
  `91430d339dad7a0d6c40302169f0f1da557a76d5`; live main remains85e1c3903b61f1d45e444d120ba1a701ad3a2c36.
  No reset/replacement PR/version bump. Separate owner PR23 untouched.
-96 complete-package export is now VERIFIED: experiment43959621139 SUCCESS on Android133623204202
  andApple133623204640; formatting43959621153 SUCCESS. Prior95 Maven429 does not apply to this head;
  no skipped check/retry workaround.534unique Android START/END match, actual original Java ZIP /
  SHA256 /BackupManager package parity ran;700189checks/48domain suites, build/lint/schema PASS.
-96 digest-verified JVM14274852479 SHA2569656e1d95589fdd5cb44dc74dbafe2ab73968543afdc1fa5f3f5d5cb72d6c5fd:
  490domain+16health+474storage. Apple14273872365
  SHA25647efc4da6cc470568b73fb51d3e4c456640970a11d85de8cc5fa428a350ffa6f:
  449domain+18health+474storage. ALL0failure/error/skip; all47new IO/SQL case names match across platforms,
  including4actual complete-package cases. Proofs scratch/complete-914-{jvm,native}/verification.json,
  native/all-case-proof.json and scratch/complete-914-android-job.log. Framework14274771984 NOT APP.
-96 private APK14274987311 digest-verified ZIP1c015e6cbbf3648b2a5ee071536f97fcd069e31939b8066a99ebad9af4a968a5;
  actual APK58172683bytes/SHA2561d3b0474ba2d5092a5e91bcb7f93e0ee66e9bed0c27c04fa27d26e3834711068,
  code19/version0.12.3-test1/min28/noINTERNET, scratch/complete-914-apk/verification.json. Not published.
-97 source adds SqliteRecordingEvidence.kt: original SHA256(sequence+":"+EXACT stored payload+newline),
  no JSON normalization, clock coercion or route recalculation.512-key metadata pages and <=1MiB payload
  batches (larger supported row alone), <=32KiB digest updates, count+hash comparison for BOTH tables.
  Unsupported huge rows reject before payload hydration. Public operation is read-only verification.
- SqliteRetentionRestore.kt is INTERNAL to future full replacement. One transaction checks active /
  paused/recoverable runs AND unfinished gym recovery, every retained activity/source, and every
  available raw count/hash BEFORE writes. Matching available detail removes only its receipt;
  missing detail stays explicitly removed. No raw sample/event changes. Preferences preserve actual
  backed-up choice, but fresh caller-supplied real dataset identity expires ALL old previews/approvals.
  Missing singleton, reused epoch, source conflict and late failure reject/rollback. No cleanup enabled.
- SqliteCoreBackupRestore.kt is INTERNAL population into ALL empty prepared core tables, never a
  partial public restore or a clear command. Restores original profile/nullable baseline times/draft,
  shoes, actual summaries/annotations, manual results and explicit preserved PBs, exact Long reminder
  revisions, assessments and opaque historical plans/decisions. Never recalculates PBs, assigns today's
  default shoe, imports health caches or generates observation clocks/IDs. Retained raw tables untouched.
- SqliteArchiveBackupRestore.kt internally populates validated original coaching/evidence/choices /
  exclusions/PB acknowledgements/legacy gym/support/0.8 archives; original FK table dependency order,
  verbatim coaching payload strings, actual sets and original selections/decisions. All target tables
  must be empty BEFORE writes. Old missing sections stay empty; supported older imported weight rows
  are retained if present in a validated backup, but user-owned exports still omit those caches.
- New contracts: RetentionRestoreRepositoryContract(8), CoreBackupRestoreRepositoryContract(5),
  ArchiveBackupRestoreRepositoryContract(5), with real JDBC +Apple SQLite wrappers. Existing Android
  SharedDetailRetentionArchiveTest's real GPS/gap fixture now verifies shared hashes against ORIGINAL
  SqliteDetailRetentionRepository.evidenceHash/requireMatches, including rejection of changed stored
  JSON bytes even when numerical evidence agrees. Original recording/archive assertions remain.
- Initial test fixture missing-return and JUnit non-void inference errors fixed locally;13focused
  retention/core cases +metadata PASS1m44s/13tasks in scratch/kmp-protected-restore-focused.log.
  First4archive cases +metadata PASS1m26s/13tasks in scratch/kmp-archive-restore-focused.log. Added a fifth
  old imported-weight case before final gate. NO source assertion was weakened.
- FINAL LOCAL492storage JVM0failure/error/skip /metadata /Android test compilation PASS3m21s/64tasks in
  scratch/kmp-protected-restore-final.log. Formatting859sources(764Kotlin95Java) PASS in
  scratch/kmp-protected-restore-format-final.log. Exact schema1..19 and3source-provenance tests PASS.
  Fresh Apple18 cases and extended Android runtime verification are PENDING a new pushed head.
- Full restore is still NOT enabled: normalized authored/draft/concrete/custom programming, frozen
  executions/actual edit receipts/associations/tools/body/awards writers, outer replacement guard /
  clear order, compatible raw merge and final retained-sample/epoch reconciliation remain. Reuse these
  internal population steps only in the complete validated outer transaction; do not expose them as
  a user-facing partial restore. Native pruning remains OFF. Version/schema/formats unchanged.

### Checkpoint98 — retained strength programming, body observations and awards

-97 committed/pushed086e6315458dd9d8f6d0badb622334b90973605b; existing DRAFT24. Experiment43963718706
  is RUNNING; formatting43963718680 SUCCESS. Do not cancel a pending gate with an unnecessary push.
  Native492/18new restore cases and Android534/original stored-byte hash parity remain unverified.
-97 Apple job is now SUCCESS. Digest-verified492storage +449domain +18health, ALL0failure/error/skip,
  artifact14277870370 SHA256d3a4fabe061fc1881ab36815b4be4c9bd8104625e6a00d1079fb92ff3df196c1,
  scratch/restore-086-native/verification.json. Framework14277550927 NOT APP. Android/JVM pending.
-97 is now FULL SUCCESS: experiment43963718706, Android133637527670 /Apple133637528168;
  format43963718680 SUCCESS.534unique Android START/END match; extended original stored-byte hash
  parity in the GPS/gap archive fixture ran.700189checks/48domain suites, build/lint/schema PASS.
  Digest-verified JVM14277341771 SHA2563c662c0e089535dc002c19ec28619520d9fccae58eecea00e8cf5df8b1bc3db3:
  490domain+16health+492storage; Native counts/digest above. ALL0failure/error/skip. All65new IO/SQL /
  restore case names match across both artifacts, including18new restore cases. Proofs scratch /
  restore-086-{jvm,native}/verification.json, native/all-case-proof.json, restore-086-android-job.log.
  PrivateAPK14277716297 exists but was NOT byte-inspected this checkpoint; last byte proof is96.
-98 adds INTERNAL SqliteStrengthProgramming.populateArchive(parent). A fully validated parent and
  empty normalized tables are required BEFORE writes. Existing private insertion/decision functions
  restore immutable revisions, blocks, dated sessions, original decision IDs/times/order and selected
  revision, without calling activate/stop or current clock/date functions. Old absent programming
  remains absent and retains the restored legacy selection. Restored stopped programming stays stopped.
- SqliteStrengthDrafts reuses its original normalized weekly-draft SQL through extracted writeDraft;
  normal save guards/order unchanged. Internal transactional population preserves custom metadata /
  logging definitions, incomplete weekly drafts and concrete previews with independent saved revisions.
  It bypasses normal save/activation gates only for validated history, which can legitimately retain
  revision4 previews of an already activated programme. All target draft tables must be empty first.
- New SqliteBodyAwardsBackupRestore internally restores validated height provenance/offset/nanos /
  import time, retained optional guidance/unit/weight-basis preferences and original milestone proof /
  per-award recognition dates. No health import, new measurement/award calculation or alerts. Reuses
  repository writes with ORIGINAL recognition dates; missing older sections keep disabled defaults.
  Late failure/caller rollback is atomic; occupied target rows reject before any write. No native
  destructive replacement/pruning command is enabled.
- New ProgrammingBackupRestoreRepositoryContract(5) and BodyAwardsBackupRestoreRepositoryContract(4),
  with real JDBC and Apple wrappers. Tests cover two authored revisions and original decisions, draft3 /
  concrete4, stopped history, old legacy selection, source metadata/unknown offsets, exact recognition
  dates, old disabled defaults, occupied targets and late/outer rollback. First fixture compile used
  nonexistent Region.CORE; fixed to existing Region.FULL with explicit BodyArea.TRUNK. No enum/model change.
- Focused programming +existing draft/activation commands PASS40tests (5new +19draft +16activation),
  metadata PASS1m21s/13tasks in scratch/kmp-programming-restore-focused.log. Focused body/awards4cases /
  metadata PASS1m21s/13tasks in scratch/kmp-body-awards-restore-focused.log. No assertion weakening.
- FINAL local501storage tests0failure/error/skip +metadata +Android test compilation PASS2m58s/64tasks
  in scratch/kmp-programming-body-restore-final.log. Pinned866format
  sources(771Kotlin95Java), exact schema1..19 and3source-provenance tests PASS. All75schema tests PASS in
  scratch/kmp-programming-body-restore-schema.log. Native9cases pending new CI.
-98local final gate is complete;97remote is fully verified. This source+recovery checkpoint is ready
  for one push; update DRAFT24 with its actual head/run IDs, then verify fresh Native/JVM501 and the9new
  programming/body/award cases plus Android534 regressions. Pending tests are not passes. Keep all scope.
- Remaining persistence: frozen workout/execution/set/event/accepted-actual edit receipts and association
  restoration, gym-tool/timer metadata; full protected outer replacement/clear order; compatible raw
  merge and retained-sample/receipt/dataset-epoch reconciliation; retention consent/preview/pruning.
  Raw merge MUST test original JSONObject number/whitespace serialization against exact receipt
  bytes; equivalent JSON is not identical evidence. Never normalize/recalculate persisted receipt
  hashes to force a match. Current verifiers safely reject differing stored bytes.
  Use97core/archive/retention +98programming/body/awards writers only inside complete validated outer
  transaction. Original Android remains authoritative. Then common application/native service wiring,
  actual iOS app host/runtime, and final unfiltered28/35/36+iOS APP/source-bound visual gates.
  No version/schema/format/permission change, early merge or publication. Hardware checks unperformed.

### Live recovery note after checkpoint98 push

- Implementation98 committed/pushed as cfe5b383f1ebeb328cb357f779f9c4bb80413fa6; PR24 remains OPEN/DRAFT.
  Fresh experiment43967075701 and formatting43967075932 are RUNNING on that exact source. No pass claimed.
  Source tree is preserved; this live-status note is a local documentation update so another push does
  not cancel/restart the just-launched expensive gate. Update/commit it with the next coherent batch.
- NEXT EXACT: inspect those two actual runs; require Native/JVM501/9new cases and Android534. Last all-
  green source is97/086e631 as documented above. After verification continue frozen execution/association /
  timer restore, guarded app/raw replacement and retention safeguards, then real shared app/native
  service integration/iOS host. No reset, replacement PR, premature merge/version/publication.

### Checkpoint99 — gym history population and guarded whole-app JSON replacement (local)

- Continuing experiment/android-ios-kmp-compose /DRAFT24. Live main85e1c3903b61f1d45e444d120ba1a701ad3a2c36
  unchanged; source work and the prior local status note are preserved. No reset/force/new PR/version.
-98headcfe5b383f1ebeb328cb357f779f9c4bb80413fa6 is now FULL SUCCESS: experiment43967075701,
  Apple133649202666 /Android133649203156; formatting43967075932 SUCCESS.534unique Android START/END
  match;700189checks/48domain suites/build/lint/schema PASS. All74 IO/SQL/restore case names match
  across actual downloaded test artifacts, including9new programming/body/award cases.
-98digest-verified JVM14276598479 SHA2567876b7e7fe6fdb79f72eda819cffc6378f4d44e262b30f63f2e0aa413b3fdd8b:
  490domain+16health+501storage. Native14276933304
  SHA25628e25ad3fe5cd6dda036cd8878bdc2b3e9616ae030dcdcfd3cfa9bd290396029:
  449domain+18health+501storage. ALL0failure/error/skip. Proofs scratch/programming-cfe-{jvm,native}/
  verification.json, native/all-case-proof.json, programming-cfe-android-job.log. Framework14277303093
  NOT APP. PrivateAPK14278130989 byte-verified ZIP2e79e60d1247914409461c0262f0a462d8244eb8f3d89d5a684ff5d760e03b6b;
  APK58205451bytes/SHA25605af700d5801a35983bd4158703af9237d8372172f6457069ba2e8ef977da40c,
  version0.12.3-test1/code19/min28/noINTERNET. Private only; no public experimental release.
-99 adds INTERNAL SqliteGymExecutions.populateArchive(parent) using existing parts/summary/append.
  Frozen workout/source/edit/deletion receipts, exact saved revision/preferences, original execution
  sets/events, accepted actual +actualEditedAt are copied without start/finish/saveActual or source
  revalidation/fingerprint generation. All engine/association/timer target tables must be empty first.
- Internal SqliteGymAssociations.populateArchive copies legacy link projections and original explicit
  choice history/selected target, never decides again or regenerates coaching evidence. KEEP_EXTRA /
  COUNT_TOWARD_SESSION remain distinct. SqliteGymTools.populateArchive copies exact timer epoch/phase /
  trigger and preferences; changing device epochs remains an interruption, not fabricated elapsed rest.
- GymBackupRestoreRepositoryContract8 real JDBC cases PASS1m17s/13tasks +metadata in
  scratch/kmp-gym-backup-restore-focused.log. Real Apple wrapper added. Cases include edited accepted
  actual vs original, warm-up/superset order, Long revision>2^53, deleted repeat source/fingerprint,
  all unfinished states, timer epoch, planned projection/no new choice, both explicit choices,
  old missing engine, occupied late table, final failure and outer rollback.
- New INTERNAL SqliteApplicationBackupRestore validates the complete root1..13 file BEFORE ANY DB
  access, protects outgoing run/gym recovery, validates owned recording identity/measurements and
  receipt/raw-count/hash compatibility BEFORE clearing. All typed core/archive/programming/body /
  retention/gym population executes in ONE existing SQLite19 transaction with final FK check.
  Retention runs before loading a legitimately unfinished incoming gym, and a fresh caller-supplied
  dataset identity invalidates old approvals. Late failure or rejected caller scope rolls back ALL.
- SqliteOwnedActivityRestore retains existing rdd/gpx activity rows in place, preserving dense legacy
  inline samples and normalized recording tables without whole-history staging or recalculation.
  Bounded512-key metadata pages, <=32MiB single-source rows and small indexed identities; imported
  caches are not hydrated. Only annotations/links can differ on retained measured records; conflicting
  IDs/measurements reject rather than overwriting evidence. Old inline backups restore into empty DBs.
  Known removal receipts survive older JSONs missing that later provenance; conflicting proofs reject.
- SqliteCoreBackupRestore default empty-table path remains intact; optional internal retained-owned
  path rechecks all owned evidence first. Settings restore now reuses SqliteSettings.putSetting,
  matching ORIGINAL Android legacy-provider normalisation to HEALTH_CONNECT; all13root versions test it.
- ApplicationBackupRestoreRepositoryContract now11 cases +Apple/JDBC wrappers: whole current roundtrip
  with running/strength revisions/drafts/sets/exclusions/body/awards/timer, genuine old-section reads
  for roots1..13, corrupt/future/incomplete rejection before DB access, all outgoing recovery guards,
  owned omission/source/measurement conflicts, retained coordinates/summary/PB, inline preservation,
  missing/conflicting removal provenance, late failure and caller rollback/unfinished imported gym.
- Initial15focused app/core cases PASS1m22s/13tasks. Preliminary519storage/meta/Android compile PASS /
  4m14s/64tasks, all0failure/error/skip, before the final additional compatibility case. A PB fixture
  constructor argument order was corrected, without changing assertions. Schema75/source3 PASS.
- Added a RED regression proving original-reader -0.0 ->0 summary spelling falsely conflicted under
  record equality. Fixed comparison ONLY for numerically identical signed-zero summary values;
  other differences (including100.00000000000001 vs100) still reject; raw bytes/hashes never normalize.
  A development attempt used the older compiled binary while source was edited and is not verification.
  Stable-source16focused app/core +metadata PASS1m22s/13tasks in scratch/kmp-application-replacement-
  focused.log. FINAL520storage JVM0failure/error/skip +metadata +Android test compilation PASS2m58s /
  64tasks in scratch/kmp-composed-restore-final.log. Pinned874sources(779Kotlin95Java), exact schema19,
  source3 and schema75 PASS in scratch/kmp-composed-restore-{format,schema}.log. New Native19cases /
  original Android regressions pending a fresh pushed-head gate.
- NATIVE DESTRUCTIVE UI REMAINS OFF. Whole JSON kernel is internal, not a complete backup-package /
  raw merge command or cleanup permission. Next: compatible streaming raw restore with exact receipts /
  source/timing/count/conflict checks, same-transaction complete package coordination, full retention
  consent/preview/epoch/pruning protections. Reuse RecordingArchiveRows/Reader and sealed private
  inspected ZIP inputs; never normalize receipt hashes to force a match. Original Android stores
  remain authoritative. Then full shared app/native services/iOS host and final full gates.
- No schema19/root13/raw3/package1/version/code/dependency/permission change. No early merge/publication.

### Checkpoint100 — recorded context preservation, staged raw replay and atomic complete restore

- Recovered clean d772bc6 checkpoint99; fetched main unchanged85e1c39 and preserved DRAFT PR24 /
  separate owner PR23.99 experiment43973710998 and format43973711057 are BOTH SUCCESS. Actual
  Android job133672180700 has534 matching unique START/END tests,700,189checks/48suites, build/lint /
  schema75 PASS. Digest-verified JVM14280972167:490domain+16health+520storage; Apple14280891388:
  449domain+18health+520storage, ALL0failure/error/skip. ALL93 new IO/SQL/restore names match.
  Proofs scratch/restore-d772-{jvm,native}/verification.json +native/all-case-proof.json and
  scratch/restore-d772-android-job.log. No blind rerun or assertion weakening.
- Actual99 private APK14281216044 ZIP30692786bytes SHA256
  dfe11d8a2295e2b17719b58707b105bb55d2182c7dd381c5bbf19327a641cfe0; APK58238219bytes SHA256
  5647a573ff7686d078ff88d39f9157afc2d8807a5707e921beda8673b85df888. Code19/version0.12.3-test1 /
  min28/no INTERNET. scratch/restore-d772-apk/verification.json. Framework14281180095 is NOT APP.
- New internal SqliteRecordedContextRestore preserves exact original execution-context bytes for
  retained recording sessions when older JSON lacks support; incompatible steps OR unknown fields
  reject BEFORE clearing. Bounded metadata/payload reads, original validation, caller rollback.
  Five new real common application restore cases (16total) +Apple/JDBC inheritance.
- InspectedRecordingArchive copies external raw ZIPs into bounded sealed private staging, validates
  actual ZIP/manifest/UTF8/framing/graph/SHA before exposure, and internally replays that SAME file.
  No provider re-read, route-wide buffering or second dense copy for complete packages. Existing
  RecordingArchiveReader public read-only constructor preserved; internal replay delivers only
  graph-validated lines and still requires successful final hash/EOF. Consumer failures close the
  reader. ALL database replay callbacks/finish must remain in one outer transaction.
- InspectedBackupPackage reuses the sealed raw inspection and cross-checks BOTH valid documents:
  immutable measured summary/source, current support/context presence and original instructions,
  and matching removal provenance. Correct individual hashes do not legitimise contradictory files.
  Old root5 missing support can still recover real original context from its raw companion.
- Internal SqliteRecordingArchiveRestore merges only inspected private sources. Preflight headers,
  original source indexes/measurements, exact samples and coverage; source context remains immutable.
  Incoming unfinished records become RECOVERABLE, never fabricated completed history. Known removed
  detail is rehydrated only after exact original timing/count/hash verification, then only its
  receipt is removed. Imported receipts cannot hide available data. Existing summaries/annotations /
  PBs stay intact; only genuinely new activities derive new qualified PBs from real samples.
  Late mismatch/failure/caller rejection rolls back rows, receipts and approval identity together.
- Internal SqliteCompleteBackupRestore composes validated whole JSON +raw merge under ONE existing
  SQLite19 outer transaction. One fresh identity, not two independent commits/approvals. Protected
  outgoing recovery, closed private sources and missing existing owned history reject safely.
  Parent changes roll back even when the final raw write fails. No native destructive UI enabled.
- Focused context16 +metadata PASS1m17s. Initial replay run exposed a JUnit signature inference
  error (Throwable instead of Unit) in a NEW test; corrected explicit Unit without changing assertions.
  Stable raw batch55focused +metadata PASS1m19s; final composed batch67focused +metadata PASS1m18s:
  app16, raw-archive10, raw-restore14, complete15, reader12, ALL0failure/error/skip. Logs:
  scratch/kmp-{recorded-context,context-replay,raw-restore,complete-restore}-focused.log.
- Preliminary557storage/meta/Android compilation PASS2m47s/64tasks; raw1/2/3 merge and closed-
  source guards then559storage/meta/Android PASS1m33s/64tasks. Review added TWO RED provenance
  tests proving provider/activity-end conflicts could pass despite matching raw hashes; both
  actually FAILED in scratch/kmp-raw-provenance-red.log (42s), not hypothetical defects.
  Fixed incoming RecordingArchiveRows graph validation using only bounded per-session source/end
  metadata; existing receipts must also match actual indexed activity and FINISHED session metadata
  before they can be consumed/reconciled. Hashes and original source bytes remain exact.
- Initial561 run caught a NEW test fixture referencing a nonexistent activity, correctly rejected
  by the existing SQLite FK. Fixed fixture by creating the separate real retained activity, keeping
  FK enforcement and all assertions. FINAL current561storage JVM0failure/error/skip +metadata +
  Android test compilation PASS1m31s/64tasks, scratch/kmp-raw-complete-restore-verified.log.
  Current17raw restore cases include source/index/receipt conflicts, exact archive1/2/3 merging,
  and closed-source rejection before ANY DB access. Total41new storage cases since99.
- Pinned881sources(786Kotlin95Java), schema75/exact19/source3 PASS. This handoff is committed with
  checkpoint100 source; resolve actual commit from git, then push ONCE. New Native561 /41cases and
  Android534 results require fresh exact-head CI and are NOT yet passed. Expected new IO/SQL/restore
  name proof131cases (prior93 +5app +5raw-export +11complete +17raw-restore); reader/graph cases
  additionally run in the full561. DRAFT PR24 remains open. No new-source APK byte claim yet.
- Native restore/cleanup UI remains OFF: full operation serialization, retention consent/preview /
  epoch/bounded pruning safeguards and original Android archived-byte parity remain acceptance
  work. Exact receipt conflict rejects safely; never normalize old raw payloads/hashes to force a
  match. Final app composition/navigation/state/native services and REAL iOS host remain incomplete.
- No schema19/root13/raw3/package1/version/code/dependency/permission change. No merge/publication.

### Checkpoint101 — original Android receipt end-bound compatibility

- 100179fcc8750f51c19dee32813e73960fc009770e1 is committed/pushed. Experiment43980336373 and
  format43980336386 BOTH SUCCESS. Actual Android133694861142:534matched unique tests,700,189 /
  48domain suites, build/lint/schema75 PASS. Digest-verified JVM14283945992:490domain+16health /
  561storage; Apple14284680190:449domain+18health+561storage; ALL0failure/error/skip. ALL131
  IO/SQL/restore case names match. Proofs scratch/restore-179-{jvm,native}/verification.json,
  native/all-case-proof.json, scratch/restore-179-android-job.log. This does NOT verify101.
- 100 private APK14283921037 ZIP30710049bytes SHA256
  990f277997613e9601b648319f663096d77b40ff90a07703fdf93200a82c5023; APK58270987bytes SHA256
  d436ff3a0ce017ba2b8e442de0e6bf351e2ef02e8dda15eb36a128e5ecc3a92d. Code19/version0.12.3-test1 /
  min28/noINTERNET. scratch/restore-179-apk/verification.json. Framework14284361021 is NOT APP.
- Inspected ORIGINAL active Android SqliteRecordingInventoryRepository.java: inspect()/recording()
  uses Instant.ofEpochMilli(end_millis).plusMillis(1), a conservative upper bound. ORIGINAL
  SqliteDetailRetentionRepository.cleanOne stores row.endedAt() in RemovedRecordingDetail.
  Therefore100's strict receipt.end == actual.end would reject legitimate ORIGINAL Android receipts.
  TWO new RED regression tests actually FAILED42s in scratch/kmp-original-retention-ceiling-red.log.
- Local RemovedDetailEnd.kt now accepts ONLY exact actual end OR the original indexed +1ms bound,
  using integer seconds/nanoseconds (including negative seconds and second carry). Incoming graph /
  existing receipt reconciliation both reuse it. Provider/activity/header checks, exact byte hashes
  and immutable actual timestamps remain unchanged. Arbitrary nearby times still reject.
- Three new common tests cover original upper-bound graph acceptance/rejection, exact fractional /
  negative/second-carry/Long>2^53 clock comparisons, and SQL-derived original end_millis+1 receipt
  rehydration. Existing removed-session/complete recovery fixtures also use original ceiling proof.
- Added real Android regression actualOriginalCleanupUpperBoundRemainsReadableBySharedArchiveInspection:
  original RecordingStore -> actual original acknowledge/preview/approve/cleanOne -> original ZIP ->
  common graph/export/private inspection, preserving summary and honest removed-detail status.
  This is production-code evidence, not merely a manually constructed receipt. Not runtime-verified
  until new-head Android CI; expected535 targeted tests, not100's534.
- Workspace restarted while the first focused Gradle run was compiling; log lacked completion and
  no process survived. ALL files survived, no reset/discard. Resumed focused46JVM (Rows13/raw18 /
  complete15) +metadata +Android test compilation PASS2m45s/64tasks in scratch/kmp-original-
  retention-ceiling-recovered.log. FINAL564storage JVM0failure/error/skip +metadata +Android
  test compilation PASS1m2s/64tasks, scratch/kmp-original-retention-ceiling-final.log.
- Pinned882sources(787Kotlin95Java), exact schema19, schema75 and source3 PASS; logs scratch/kmp-
  original-retention-ceiling-{format,schema}.log. This source and handoff are committed together;
  resolve actual HEAD and push ONCE if not already pushed. Fresh Native564/new535Android runtime
  gates are pending, NOT passed. UI remains OFF. Next finish native inventory/retention/operation
  safeguards on the SAME SQLite19/typed policy and then real common app/native host.
- Full retention/operation safeguards, common app/root/navigation/state, native services and REAL
  iOS app host still unfinished. No schema/version/format change, merge or publication.

### Checkpoint102 — shared inventory and non-destructive retention policy workflow

- 10139a58c807dce7d3a1c32a2e8c0cbe015b7721dc2 COMMITTED/PUSHED. Experiment44020784624 and
  format44020784727 BOTH SUCCESS. Android133829507771 has535matched unique START/END tests,
  including the actual original cleanup/end-bound regression;700,189checks/48suites, build/lint /
  schema75 PASS. Digest-verified JVM14301058867:490domain+16health+564storage; Apple14300974176:
  449domain+18health+564storage; ALL0failure/error/skip. ALL132 IO/SQL/restore names match.
  Proofs scratch/retention-39a-{jvm,native}/verification.json, native/all-case-proof.json and
  scratch/retention-39a-android-job.log. This does NOT verify102's new services.
- 101 private APK14301472930 ZIP30710655bytes SHA256
  3cb108f2192df94472c73e50dd48b90e39e4cae0a8914e4a6aaa9f97a04bae6c; APK58270987bytes SHA256
  1db000e4d2495c391dea9451e0a709587f88fa34cab9ee8fbbeea2fcafc5a4ed. Code19/version0.12.3-test1 /
  min28/noINTERNET, scratch/retention-39a-apk/verification.json. Framework14300949202 is NOT APP.
- New SqliteRecordingInventory ports the original read-only normalized-stream inventory, without
  decoding route/summary payloads or recalculating evidence. Pre-join point/event aggregates avoid
  cross-products; fixed query count, bounded100k headers/32MiB metadata and explicit overflow checks.
  SQL UTF8 payload bytes are distinct from optional approximate physical DB/WAL/SHM observation.
  Missing physical data stays null, never false zero/reclaimed-space claims. Old inline activity
  samples are retained and never eligible for removal through this normalized-stream boundary.
- Original indexed end_millis +1ms remains an ELIGIBILITY upper bound, not an actual end timestamp.
  Integer quotient/remainder preserves negative milliseconds and Long.MIN/MAX without overflow /
  Double rounding. Internal single-session recheck matches the same inventory projection. Unknown
  source/end/recovery states remain ineligible or explicitly fail, never assumed safe/finished.
- Eight new common real SQLite inventory cases +Apple/JDBC wrappers cover aggregate counts/UTF8,
  unknowns/active/paused/recovery, full Long bounds, fixed scoped read-only queries, physical unknown /
  failure, header/metadata allocation budgets, overflow and future states. First run caught NEW
  negative-time fixture violating existing end>=start CHECK; fixed valid start, never disabled FK /
  CHECK/assertions. Eight +metadata +Android compilation PASS2m34s/64tasks. Intermediate572storage /
  metadata/Android PASS57s/64tasks in scratch/kmp-recording-inventory-{focused,android-focused,final}.log.
- Added actual Android original-inventory comparison, reusing the production recording fixture:
  shared records/counts/clocks/source/logical bytes equal SqliteRecordingInventoryRepository output;
  no SQL mutation and unknown physical observation stays unknown. Expected536 targeted tests on new
  head; that NEW runtime case is not yet verified. Prior535 is101 only.
- New SqliteRetentionPolicy ports informed acknowledgement, read-only consequence preview and
  domain approval over one SQLite snapshot. Required real replacement-state callback, original
  policy/clock/deadline/recovery/epoch rules; fresh choice IDs invalidate old decision identity.
  Existing users may preview but cannot approve until acknowledgement. KEEP_FOREVER never approves
  deletion. Current backup13 records retain exact chosen time/policy/choice ID; no new JSON bucket.
- Eight real common policy cases +Apple/JDBC wrappers: grandfathering/all3choices/current backup,
  duplicate/invalid IDs before writes,24h/backwards-clock/restored epoch, all recovery/replacement
  guards, snapshot scope, failed SQL write and caller rollback. NO CLEANUP COMMAND EXISTS in this
  public policy service; approval alone cannot delete evidence. Full operation serialization and
  bounded deletion kernel are still required before exposing native destructive UI.
- Focused16 inventory/policy +metadata +Android compilation PASS3m48s/64tasks. FINAL580storage JVM /
  0failure/error/skip +metadata +Android test compilation PASS4m36s/64tasks after final metadata-byte
  budget; scratch/kmp-{retention-policy-focused,inventory-policy-final}.log. Pinned890sources
  (795Kotlin95Java), exact schema19, schema75 and source3 PASS; scratch/kmp-inventory-policy-
  {format,schema}.log. This source/handoff are committed together; resolve actual HEAD and push
  ONCE if needed. New Native580/16cases +Android536 are pending. Expected IO/SQL proof148names.
- No schema19/root13/raw3/package1/version/code/dependency/permission change. Native destructive
  UI OFF. Full operation/pruning safeguards and actual common app/root/native services/iOS APP
  remain required. No merge, public publication or actual-device acceptance claim.

### Checkpoint103 — internal bounded raw cleanup and exclusive data-operation guard

- 102616b1fb17496653f678c3954f8c490619524b8ff is COMMITTED/PUSHED. Experiment44025235154 and
  format44025235229 BOTH SUCCESS. Android133843845599 has536matched unique START/END tests,
  including actual original-inventory comparison;700,189checks/48suites, build/lint/schema75 PASS.
  Digest-verified JVM14303222660:490domain+16health+580storage; Apple14303113250:449domain /
  18health+580storage; ALL0failure/error/skip. ALL148 IO/SQL/restore case names match. Proofs
  scratch/inventory-616-{jvm,native}/verification.json, native/all-case-proof.json and
  scratch/inventory-616-android-job.log. These results do NOT verify103.
- 102 private APK14303019484 ZIP30713805bytes SHA256
  1abd5957f43e91d4394cc6226de018eb0d8f27dac956aac9d475c8fc43c54ee4; APK58287371bytes SHA256
  a6d1aa60c2ac6ae95fcca8826b0eb9a4780df51c54a93588ad58e175175bdff0. Code19/version0.12.3-test1 /
  min28/noINTERNET, scratch/inventory-616-apk/verification.json. Framework14302934103 is NOT APP.
- New INTERNAL SqliteRecordingDetailCleanup ports original one-session raw-only cleanup. Required
  application-exclusive operation boundary, no idle default. Fresh consent/choice/epoch/clock /
  recovery/replacement checks run inside the transaction, including before an idempotent no-op.
  Current indexed row must match approved ownership/end/counts. Original sequence:payload:newline
  SHA/count proofs are retained; only recording_points/events for that session are deleted.
  Unexpected delete counts, late SQL failure and rejected caller scopes roll back rows AND receipt.
- Existing receipts cannot be silently overwritten or treated as successful retries with changed
  source/policy/counts. Source identity/header/actual end checks reuse extracted internal
  SqliteRecordingReceiptIdentity in both cleanup and raw restoration, including original indexed
  upper-bound compatibility. Actual payload ID must match its indexed activity too. Bounded
  source-payload metadata checks precede allocation. No summary/PB/actual/history recalculation.
- Nineteen common real SQLite cleanup cases +Apple/JDBC wrappers cover all-table preservation /
  exact hashes/PBs, stale and expired decisions, all recovery states, source/index/payload conflicts,
  unapproved sessions, existing receipts, late failure/retry, count mismatch, outer rollback,
  oversized-payload rejection before read, preserved legacy inline samples, truthful JSON/package
  backups and exact compatible raw rehydration with old-approval invalidation. SQL spy permits ONLY
  raw-session deletes and removal-receipt inserts. Other tables remain byte-for-byte unchanged.
- LocalDataOperations provides the real non-queuing exclusive guard: JVM ReentrantLock /Apple
  NSRecursiveLock, with synchronized state inspection and rejection of nested or cross-thread
  backup/replacement/cleanup overlaps. Failure releases state/lock; rejected actions never replay.
  THREE common guard tests include actual concurrent workers (JVM latch/thread /Apple GCD semaphore,
  bounded failure timeouts, no sleeps). Cleanup integration also uses this production guard.
- Actual application composition MUST own ONE guard and wrap ALL backup/export/restore paths with
  it; those host bindings are not implemented/claimed. Guard/kernel are internal, Native destructive
  UI remains OFF. Do not confuse existence of the kernel with complete platform operation wiring.
- Focused36cleanup/restore +metadata PASS1m28s/13tasks; intermediate598storage/meta/Android compile
  PASS3m/64tasks. Guard/integration22focused +metadata PASS1m24s/13tasks. FINAL602storage JVM /
  0failure/error/skip +metadata +Android test compilation PASS3m3s/64tasks, scratch/kmp-cleanup-
  operations-final.log. Pinned901sources(806Kotlin95Java), exact schema19, schema75 and source3
  PASS in scratch/kmp-cleanup-operations-{format,schema}.log. Source/handoff committed together;
  resolve actual HEAD and push ONCE if needed. Fresh Native602 /cross-thread Darwin test and
  Android536 runtime gates are PENDING. Expected IO/SQL/guard name proof170 (148+19cleanup+3guard).
- Next bind the same guard into actual app data composition and all mutation/backup paths, preserve
  remaining original archive-byte safeguards, then real common application root/navigation/state /
  platform services and REAL iOS app host. No premature Native destructive UI, merge or publication.
- Schema19/root13/raw3/package1/version/code/dependencies/permissions unchanged. All physical-device
  HealthKit/Health Connect/GPS/BLE/audio/locked-screen/OEM/TalkBack acceptance remains unperformed.

### Checkpoint104 — shared application data-lifecycle owner

- Continue the SAME experiment branch / DRAFT PR24, no reset or release/version change.
- Checkpoint103 `e502e74a51821a071dced968a701a7df706f9b99` is now actually green:
  [44034693799](https://github.com/mmwiza1/Race-day-dreams/actions/runs/44034693799), Android
  133874631520 / Apple133874631883 SUCCESS; formatting44034693749 SUCCESS. Downloaded/digest-
  verified JVM14307542675 has490domain/16health/602storage; Native14306633637 has449/18/602,
  all0failure/error/skip. All170 IO/SQL/guard names match across platforms, INCLUDING real Darwin
  concurrent-worker test. Actual Android logs have536 unique matching START/END,700189checks /
  48suites,3successful builds. Proofs scratch/cleanup-e502-{jvm,native}/verification.json,
  native/all-case-proof.json, cleanup-e502-android-proof.json. PrivateAPK14308030308 ZIP30711670 /
  SHA b4256c0bdde258d3b54bc3b399819661e0568f7f3a361670a73774d52fa3c260; APK58303755 /
  SHA c0974c90ff032750e9dac1a6847f256940b85cdffcc1d8d35772c9dc810d0632,code19/min28/noINTERNET.
  Framework14306693488 remains NOT an actual app. No public experimental publication.
- New SqliteDataLifecycle composes the existing inspected JSON/raw/complete export+restore and
  informed retention workflow using ONE private LocalDataOperations guard. Review scopes see
  themselves as non-replacement while excluding concurrent/nested operations. Read inventory /
  preferences also share that gate. Host must create ONE owner per DB and route ALL relevant
  operations through it, on its IO executor; actual platform/root bindings remain unfinished.
- Scoped raw/complete backup consumers finish copying before return; staged sources close on
  normal return or failure. Same guard covers inspection AND transactional restore. Constructor
  never accesses database; corrupt imports validate before ANY DB access. Existing format/source/
  identity/rollback protections reused, no alternate DB or training engine. cleanOne stays INTERNAL;
  approval never deletes. Native destructive UI remains OFF pending host wiring and UI acceptance.
- EIGHT real SQLite contract cases on JVM/Apple wrappers: choice/preview/approval do not delete;
  backup scope rejects nested review/read/mutation/restore; complete/raw callbacks close escaped
  sources; consumer failure releases guard; actual complete roundtrip preserves data/fresh epoch;
  malformed imports leave state intact; cleanup cannot overlap backup; restore invalidates old
  approval; construction/invalid import never touches SQL. Existing nonempty source activity and
  point are retained. Existing kernels' detailed transaction suites remain intact.
- Local focused10 tests PASS1m11s (7initial lifecycle+3guard); final610storage JVM,0failure/error/
  skip +common metadata +Android instrumentation compilation PASS4m23s/64tasks,
  scratch/kmp-lifecycle-final.log. Initial test import compilation error corrected to existing
  storage StoredInstant (not a weakened assertion). Format905sources (810Kotlin/95Java) and exact
  schema1..19 PASS. Fresh Native610/runtime validation is PENDING next coherent push.
- Next batch already being implemented locally: move existing app chrome/navigation icons to common
  Compose, preserving actual Android routes/lifecycle/graphs. Inspect git status/diff; finish its
  focused compilation/UI tests before claiming it verified. Do not overwrite those local files.

### Checkpoint105 — original application shell now shared Compose

- COMMITTED with104storage owner; inspect actual HEAD/upstream and push these coherent commits
  ONCE if needed. SAME experiment branch / DRAFT PR24; no version/schema/publication changes.
- Actual production CoachActivity now delegates its EXISTING top bar, five bottom destinations,
  adaptive navigation rail, snackbar host and inset content slot to common CoachShell. Original
  routing/NavHost/feature graphs, model lifetime, Android permissions/document pickers, start
  destination, gym-workout back handling and running service remain authoritative in the host.
  This is a real Android-used extraction, NOT a new app/navigation engine or completed iOS root.
- CoachDestinationIcon shares the five exact original RDD vector path strings,24dp viewport and
  even-odd fill. Dynamic tint/labels/order/large-font semantics remain unchanged; no icon library,
  replacement branding or permissions. Original Android assets remain for notification/compatibility.
- FIVE new Android interaction tests cover destination order/selection/callbacks; detail Back /
  Settings delegation; dark200% accessible destination controls; dated Strength rail selection;
  exact pixel comparison of ALL5 shared icons against real original Android drawable resources.
  FeatureNavigationTest's2 existing actual graph/reactive snapshot tests now join focused CI too.
  Focused runtime expectation543 unique (prior536+5+2), NOT a final unfiltered release matrix.
- Local metadata+Android production/test compilation PASS2m51s/62tasks,
  scratch/kmp-shell-compile.log. ALL610storage JVM/0failure/error/skip passed in104. Full pinned
  formatter907sources(812Kotlin/95Java), schema75, exact schema1..19 and source3 PASS;
  scratch/kmp-shell-format.log, kmp-composition-{schema,source}.log. git diff --check clean.
- Fresh Apple610storage tests +real common shell/framework compilation and Android543 runtime /
  icon pixel test are PENDING the next exact-head CI run. Diagnose real failures; no weaker pixel
  or navigation assertions, arbitrary delay or blind retry. Native destructive UI stays OFF.
- Next finish actual application owner/root/navigation/state bindings and native services/iOS host,
  including all mutation/backup operation paths. Framework success never equals actual iOS app.
  Preserve remaining original archived-payload/receipt byte safeguards; do not normalize old hashes.
  No early merge/publication; complete final app/runtime/accessibility/matrix gates only when ready.

### Checkpoint106 — exact archived sample bytes and deterministic icon comparison

- SAME experiment branch / DRAFT PR24. Resolve actual HEAD/upstream; source+handoff committed
  together and push ONCE if ahead. No reset, format/version/schema change, merge or publication.
- Checkpoint105df52ab24b8f5602507a0f29b874e10df6bbfc581: experiment44038551289 completed
  FAILURE only in new Android icon pixel test; formatting44038551313 SUCCESS. Apple133887248589
  SUCCESS, real shared shell/framework compiled. Downloaded/digest-verified Native14307969295 /
  ZIP169758 / SHA ee8807853c60a0071d44c73336ba0fce81b2452e0c5a89cb3b18ef5debe78489 has449domain /
  18health /610storage; JVM14309590097 /ZIP179233 /SHA5e8112d4347e8fa091ea9ffce99ad31c86a33b85a6874282c5968037f53eb0e4
  has490/16/610, ALL0failure/error/skip. All178IO/SQL/guard/lifecycle case names match. Proofs
  scratch/composition-df52-{jvm,native}/verification.json and native/all-case-proof.json.
- Actual Android133887248359:543 matching START/END,542pass/ONEfailure, no skipped tests. Four
  shell navigation/light/dark200%/rail tests and both original graph tests PASS. Icon comparison
  failed at home pixel20,5: red248 versus247 (green/blue/alpha unchanged), at DIFFERENT device
  coordinates despite exact same source paths. Fixture now captures original/shared sequentially
  at IDENTICAL bounds to remove translated GPU antialias rounding from the comparison. All5icons
  still require EXACT every-pixel equality, no tolerance, deleted assertion, sleep or blind rerun.
  Actual runtime proof of this correction is PENDING. Log scratch/composition-df52-android-job.log.
  No df52 APK published/generated as verified; prior e502 privateAPK remains last byte-checked.
- Demonstrated NEW archive defect before fixing: valid stored point/event payload with original
  escaping/interior whitespace rejected its OWN exported archive at SqliteRecordingArchiveRestore
  old==payload.toString(), with original receipt hashes intact. One focused test FAILED at that
  exact conflict, scratch/kmp-archive-escape-confirmed-failure.log. Cause is JSON reserialization,
  not GPS/filtering/coordinates/summary arithmetic. No historical hash normalization is allowed.
- Export now embeds validated point/event object text VERBATIM in existing raw3 JSONL. Restore
  uses a bounded exact root-field span from the EXISTING strict scanner, preserving original
  escapes, numeric spelling and interior spacing. Entire document must validate before returning
  that value; duplicate/escaped duplicate names, malformed suffixes and original depth/token /
  string budgets remain enforced. Nested payload names cannot shadow the row's root payload.
- JSONL cannot preserve literal CR/LF or outside-value whitespace in a stored source object;
  export now rejects those safely instead of producing misleading proof-incompatible detail.
  Original rows remain unchanged. No extra wire fields, version bump, route recalculation or
  weakened receipt check. Already incompatible legacy archives still fail honestly; missing bytes
  are never invented. Native destructive UI remains OFF.
- TWO new real SQL cleanup/archive cases: unchanged retry then exact receipt rehydration with
  unknown/nested escaped fields; unrepresentable source rejects export without any DB changes.
  FOUR scanner cases verify exact spans, escaped root names, suffix/duplicate validation and
  existing allocation budgets. ONE new Android test exercises ACTUAL original JSONObject-built
  raw payloads (slashes/non-ASCII/nested names) through old→shared and shared→old archive readers,
  preserving exact SQL evidence. This Android case remains PENDING runtime verification.
- Local corrected repro PASS1m11s. Focused66cleanup/restore/complete/lifecycle/scanner tests +common
  metadata +Android test compilation PASS3m49s/64tasks, scratch/kmp-archive-bytes-focused.log.
  FINAL616storage JVM,0failure/error/skip +metadata PASS1m25s/13tasks, kmp-archive-bytes-final.log.
  Icon fixture Android test compilation PASS54s/55tasks, kmp-icon-fixture-compile.log. Pinned908
  sources(813Kotlin/95Java), exact schema1..19 and source3 PASS; schema75 passed unchanged earlier
  this batch. New exact-head Native616 /184IO/SQL/lifecycle/scanner names /Android544 are PENDING.
- Next verify those exact-head gates, diagnose any real icon/old-reader failure, then continue
  actual application state/root/platform bindings and iOS host. CoachUiState currently references
  three Android store DTOs, HealthAvailability and JVM ReminderPreferences/ZoneId defaults; reuse
  existing shared projection/ReminderSettings types when migrating, do not fork a second app state
  or engine. Platform IO/kernel adapters are not a complete iOS application. All physical checks
  remain unperformed. No premature release/final screenshot claim.

### Checkpoint107 — staged read-only HealthKit running-workout imports

- Continue SAME experiment branch / DRAFT PR24. Commit source+handoff together, push ONCE if
  needed after inspecting upstream. No new version/schema/backup/permission/dependency at runtime.
- Checkpoint106 `536ee94e704d43a4efb84525e374d44372d07427` is now FULLY GREEN for its targeted gate:
  [44041687509](https://github.com/mmwiza1/Race-day-dreams/actions/runs/44041687509), Android
  133897522972 /Apple133897523471 SUCCESS, formatting44041687519 SUCCESS. Actual Android logs
  prove544unique matching START/END including ALL5 exact icon pixel comparisons at identical
  coordinates AND original→shared→original escaped-archive interop. Original543failure fixed by
  matching capture geometry, not tolerance/removed assertion/changed icon.700189domain/48suites,
  3successful builds/schema/lint PASS; scratch/archive-536-android-{job.log,proof.json}.
- Downloaded/digest-verified JVM14310686519 ZIP180122/SHA
  febc436918dd10ce885d6250b24e20ebfd70cbd2276c15b72de8a3d3badf412a has490domain/16health/616storage;
  Native14309548258 ZIP170822/SHAed74abb21a3ec53f7caa74532b99326314c2f43220882e90efd6d2c6cf798e8a
  has449/18/616, ALL0failure/error/skip. ALL184IO/SQL/lifecycle/scanner case names match;
  scratch/archive-536-{jvm,native}/verification.json, native/all-case-proof.json. Framework14310311429
  is still NOT an actual iOS app. No canonical final screenshots or hardware acceptance claimed.
- PrivateAPK14311210389 ZIP30726531/SHA6caa105d685fe730881698c86a88d0fd1d5295c394d4a4f7fc6c7c1e6fe1329b;
  APK58320139/SHA9988a90363c199b5b34678fb30ed43eecb2fd8a210447a8b7ab925641cccb393, code19/min28/
  noINTERNET. scratch/archive-536-apk/verification.json. No experimental public publication.
- New actual iOS SDK HealthKitRunningReader requests ONLY workout read access (no write types).
  Apple's authorization completion never proves read permission. Query runs on Main, stops on
  cancellation, bounds the SDK result at10001 to reject >10000 rather than truncate, filters actual
  running workouts, app-owned source, read-window boundaries and unfinished/future endings.
  Real SDK UUID/source bundle/start/end/optional totalDistance only; no GPS/heart rate/moving-time/
  offsets/effort fabricated. Existing HealthKit body reader and Android Health Connect untouched.
- HealthKitRunningRead freezes SDK observations and is ALWAYS VISIBLE_RECORDS_ONLY. It returns no
  database writes or permission/coverage claim. Host must prepare its batch using CURRENT local
  records/checkpoint INSIDE the app's serial import/SQL scope, not a snapshot retained across the
  asynchronous SDK query. HealthKitRunningImport uses the EXISTING HealthImportPolicy.Batch /
  commit policy with completeSnapshot=false, no inferred deletions and unchanged retained token.
- Exact source/time/origin conflicts reject rather than overwrite historic measurements. Own app
  workouts aren't reimported. Repeated identical observations deduplicate; conflicting UUIDs reject.
  Missing distance stays null (genuine0 remains0); missing optional fields preserve compatible prior
  evidence/opaque app ID/notes/shoe association. Existing recorded sets/PBs/programmes unchanged.
- NINE common health cases cover observations/unknowns/source/nanoseconds, hidden/empty no-delete,
  checkpoint preservation, existing optional evidence, self/outside/future filters, duplicate and
  identity conflicts, invalid/oversized reads, immutable staging and fresh commit evidence. FOUR
  real JDBC/Apple SQLite contracts prove repeated import, retained history/notes/checkpoint with
  withheld data, caller rollback and rejection before SQL mutation. sharedStorage adds ONLY a TEST
  dependency on existing sharedHealth, no production/runtime library or second database.
- Focused9health PASS27s; full25health+4focused SQL PASS48s/10tasks. FINAL25health JVM +620storage
  JVM, ALL0failure/error/skip +health common metadata +Android test compilation PASS3m8s/68tasks,
  scratch/kmp-healthkit-running-final.log. Formatter914sources(819Kotlin/95Java), exact schema19 /
  source3 PASS; schema75 last passed in preceding unchanged-SQL batch. No test weakening or blind
  rerun. Fresh Native27health/620storage/SDK API compilation and Android544 remain PENDING.
- This does NOT complete HealthKit/iOS app integration: native host/entitlements/usage strings /
  permission UI/import-owner wiring, optional HR/route import and full service parity remain.
  Next verify actual macOS compiler/API diagnostics, then bind real common app state/root and
  native services/iOS host. ONE application-owned data-operation boundary must cover mutations /
  backup paths; native destructive UI stays OFF. All physical provider/GPS/BLE/audio/TalkBack /
  locked-screen checks remain UNPERFORMED. No premature merge/release.

### Checkpoint108 — ONE shared application read-state and command runner

- SAME experiment branch / DRAFT PR24; main refetched unchanged85e1c3903b61f1d45e444d120ba1a701ad3a2c36.
  Commit source+handoff together and push ONCE if ahead. All work stays experimental/unpublished.
- Checkpoint107 7d79b25c56f769165cc38422291dc62a763aeda9 is ACTUALLY GREEN: experiment44044723070,
  Android133907473548 /Apple133907474019 SUCCESS; formatting44044723086 SUCCESS. Actual native
  HealthKit SDK adapter compiled and linked. This proves API compilation, not authorised real reads.
  Android544unique matching START/END,700189domain/48suites,3successful builds/schema/lint PASS.
- Digest-verified JVM14311522787 ZIP182483/SHA694a29c0960bdaac161df5faba3553c46e0b64746242c316c537390378dd30b8
  has490domain/25health/620storage; Native14310779772 ZIP173708 /
  SHA5c1d027f1fbf3bf45a577ac3953af325fe50de47fef3985a521239336dbcad12 has449/27/620;
  ALL0failure/error/skip. ALL197 IO/SQL/lifecycle/HealthKit case names match across platforms,
  including actual SQLite source import/rollback cases. scratch/healthkit-7d79-{jvm,native}/
  verification.json, native/all-case-proof.json, healthkit-7d79-android-{job.log,proof.json}.
- PrivateAPK14311956887 ZIP30723789/SHA71bc7c90b39ce81cce86af1680b3736ccd6c4f2d73898c47fab49a0e65ab2b85;
  APK58336582/SHA5f6e25efdd80febf6a35be49a67b30b2eb0933c3064225c82366d71980093656,code19/min28/
  noINTERNET, scratch/healthkit-7d79-apk/verification.json. Framework14311921825 is NOT an iOS app.
- Actual CoachUiState moved from app to sharedUi/commonMain, SAME package/name and coherent model,
  not a second/native-only state. Existing common StrengthTodaySummary.Association, typed legacy
  gym-decision/choice DTOs and ReminderSettings replace Android store/Java-time wrapper fields.
  Android snapshot reader only copies original values into those existing models. Field values,
  order, source IDs, favourites/exclusions, version/association provenance and nullable metrics stay
  intact. Empty baseline retains original84-day/no-observation semantics with real local clock;
  no recorded measurement/eligibility/coaching calculation is invented or changed.
- Existing narrow JVM ReminderPreferences adapter now exposes its existing shared() conversion and
  fromShared() reverse binding. Mode, exact nanoseconds/quiet bounds and zone ID are retained;
  no scheduling changes. Android Settings still uses its original platform form/notification API.
  Provider HealthAvailability enum moved unchanged to sharedHealth; AVAILABLE is NOT read authority.
- Actual CoachOperationRunner moved VERBATIM to common UI/application ownership (same package/API,
  mutex/command/callback/retained-state rules), except its default dispatcher uses a tiny required
  expect/actual factory. Android retains Dispatchers.IO; Native imports its documented IO extension.
  kotlinx-coroutines-core1.9.0 is explicitly api in sharedUi because public signatures expose it;
  this reuses the ALREADY present version/library, not a new runtime dependency or second engine.
  New Android/iOS runtime source sets are covered by formatter and source fingerprint guards.
- Cross-module public properties cannot be smart-cast: RunFeedbackSection now captures its current
  plan ONCE for both list and callback, preserving coherent version identity. Failed compilation
  diagnosed/corrected at these references, not by weakening tests. Common Dispatchers.IO absence
  corrected with platform factories; exact coroutines1.9 source confirms public Native extension.
- TWO Android binding tests cover every reminder mode, four zones, optional quiet periods, exact
  subsecond values, original defaults and unknown training evidence. Two existing exercise-browser
  fixtures now supply the same values in common choice DTOs, with assertions unchanged. THREE
  EXISTING command-runner concurrency/failure/callback tests also join focused CI (not new tests).
- FINAL local25health /620storage JVM,0failure/error/skip +700189original domainchecks/48suites +
  common UI metadata +Android production/test compilation PASS4m48s/82tasks,
  scratch/kmp-shared-application-final.log. Formatter917sources(822Kotlin/95Java), exact schema19 /
  source3 PASS; schema75 remains unchanged and green in107CI. git diff --check clean.
- New actual Native state/runner/framework compilation and Android549focused runtime (544+2new
  bindings+3existing runner tests) are PENDING next exact-head CI. No final screenshots/whole app
  acceptance/physical pass claimed. Legacy Android state/runner originals removed, not duplicated.
- NEXT real software gap: shared operational CoachingRepository commands. SqliteCoachingArchive
  currently snapshots/validates; it does NOT implement CoachingRepository. Reuse actual original
  CoachingStore.java rules and existing common codecs/execution edit provenance, then bind shared
  application snapshot/services/root, platform providers and actual iOS host. Original snapshot
  reader also appends optional coaching proposals/reviews and reconciles reminders; preserve those
  explicit-version rules when moving it, not a fake read-only/prototype replacement. No destructive
  native UI until ONE data owner covers every operation path. All physical checks unperformed.

### Checkpoint109 — full shared coaching repository now used by Android

- SAME experiment branch / DRAFT PR24, all completed work retained. Commit source+handoff together,
  inspect upstream and push ONCE if ahead. No schema/version/backup/permission/runtime dependency
  change. Existing main85e1c3903b61f1d45e444d120ba1a701ad3a2c36 /owner PR23 remain untouched.
- Checkpoint108 ce74dcaadfc534d493b5f8d7f697652e8dac2a7b ACTUALLY GREEN: experiment44049185240,
  Apple133922402424 /Android133922403181 SUCCESS; formatting44049185186 SUCCESS. Actual common
  state/runner, Native IO extension and framework compiled. Android549unique matching START/END,
  including exact reminder mode/zone/nanosecond roundtrips, original3command-runner contracts,
  icon equality and old/shared archive interop;700189checks/48suites/3successful builds/schema/lint.
- Digest-verified Native14314116050 ZIP173366/SHAa5029c4720b56ec9cd53717893ee245715d52c346fadc5741d677bcdf1aa498b
  has449domain/27health/620storage; JVM14313153551 ZIP181906 /
  SHAa4347b1ce2e5a72872f91c45a18b6ed54b35ff9f083a6c4653ad0bbb57a5d6fd has490/25/620, ALL0f/e/skip.
  ALL197IO/SQL/HealthKit names match; scratch/application-ce74-{jvm,native}/verification.json,
  native/all-case-proof.json, application-ce74-android-{job.log,proof.json}. Framework14314375278
  is NOT an app. Physical HealthKit/recording/provider acceptance remains unperformed.
- PrivateAPK14313018959 ZIP30737739/SHAab67851b6ddab0d3533559ab74ee275b6aa7cf878e5442ab16c00c6d936882bb;
  APK58352966/SHA436a84e4b70f5fecb902fefcfe9569e08510d735aec394a3f3f00e1871c2ec4a,code19/min28/
  noINTERNET, scratch/application-ce74-apk/verification.json. No public experimental publication.
- NEW SqliteCoachingRepository implements the ENTIRE EXISTING CoachingRepository interface.
  It COMPOSES already-implemented SqliteRunningCoaching +SqliteGymExecutions +SqliteStrengthDrafts.
  Do NOT reimplement those commands or confuse SqliteCoachingArchive with missing command support.
  Original running decisions/history, explicit actual edit/delete provenance, unfinished guards,
  custom metadata/measurement compatibility and required legacy fingerprint provider are reused.
  Reads preserve original canonical/override order and full actual history; no fake strength stubs,
  replacement database, parallel adaptive/execution engine or historical recalculation.
- PRODUCTION Android CoachingStore now delegates its commands to that shared implementation;
  roughly270 old operational Java lines removed. Its Android helper/IO guard and independent legacy
  backup export/validate/clear/restore helpers remain. Construction opens NO database: a lazy
  source supplies the SAME ProfileDatabase writable pool only when an operation executes.
- New AndroidSqlConnection reuses established typed cursor/bind behavior, preserving SQL NULL /
  exact Long/Double/text/blob. Transactions share the existing Android session; nested/caller-
  owned scopes use unique atomic savepoints. Borrowed close() does NOT close/open the app pool;
  ProfileDatabase still owns its lifetime. Original test bridge stays independent for comparison.
- EIGHT real JDBC/Apple contract cases verify full-interface canonical overrides/frozen actual
  definitions; edits/deletion without new prescriptions; future-actual rejection; optional proposal /
  keep/no extra version; completed-prescription protection; whole running+strength rollback;
  custom metadata/measurement guards; unfinished execution cannot be replaced/deleted. Existing
  verified kernel suites still cover full execution/association/backup behavior.
- FOUR new Android driver cases cover lazy borrowed lifetime, exact >2^53 integers/NULL/unicode /
  real/blob, cross-wrapper nested rollback, and caller-owned Android transaction rollback. Existing
  CoachingStore tests remain unchanged and will now exercise the real shared backend. SEVEN existing
  ExerciseBrowseScreenTest cases join focused CI because108migrated their choice/state types;
  all assertions are retained (only two fixtures use the equivalent common DTO).
- Focused6new SQL cases PASS1m18s; metadata+6+Android compile PASS2m41s/66tasks. FINAL628storage
  JVM/0failure/error/skip +metadata +Android production/test compile +lint PASS3m32s/126tasks,
  scratch/kmp-coaching-binding-final.log. Pinned923sources(828Kotlin/95Java), formatter-tool14,
  exact schema19/source3 PASS; schema75 remains unchanged/green in108CI. Diff checks clean.
- New Native628 /205matching IO/SQL/HealthKit/coaching names and Android560runtime (549+4new
  driver+7existing browser) are PENDING exact-head CI. This is an actual Android persistence path
  change: inspect real failures before continuing, never weaken assertions or blindly rerun.
- NEXT: bind the shared operational repositories/state/runner into the actual shared application
  snapshot/services/root/navigation and native host, retaining side-effect ordering for optional
  proposals/reviews/reminder reconciliation. Current Android CoachSnapshotReader remains host-owned.
  Native permissions/full HealthKit import owner/HR/route integration, GPS/BLE/notifications/audio,
  actual iOS host/entitlements/build/runtime and final unfiltered Android28/35/36/iOS APP/screenshots
  remain unfinished. ONE data-operation owner must guard ALL app/service mutation and backup paths.
  Native destructive UI stays OFF; no early merge/version bump/publication or next release.

### Checkpoint110 — shared coherent snapshot and confirmed Native test fix (17 November)

- Preserved109b69b886 and all earlier checkpoints. Experiment44057707591 completed MIXED:
  Android133951222121 SUCCESS with560unique START/END tests,700,189domain/48suites, schema75,
  all3builds/lint; Apple133951222515 failed compiling ONLY the new common rollback test at
  CoachingRepositoryContract.kt:130. JVM accepts a lambda through Runnable; Native's actual
  CoachingTransaction interface does not. Corrected the SAME rollback test to an explicit
  object implementing run(); no transaction API, implementation or assertion weakened.
- Independently verified109JVM artifact14317916693 ZIP183,913bytes/SHA256
  d9abc285f1a8cf3dda8d67c99ca32531b980de7344cccb3342dc2e055b425254:
  490domain/25health/628storage,0fail/error/skip. PrivateAPK14317572059 ZIP30,741,210bytes /
  SHA95a024d0514e2a9fb601d0205fc273bc1fcfb5a130ec197f5d13f6373c36d8e6; APK58,352,966bytes /
  SHA7561a9db169a68a5003271d885b2613da713307721b9084204974ec695af2cff. aaptcode19/min28/noINTERNET.
  Proofs scratch/coaching-b69-{jvm,apk}/verification.json and coaching-b69-android-{job.log,proof.json}.
  This is NOT a green Apple checkpoint and NOT a publishable complete migration.
- Extracted the ACTUAL Android CoachSnapshotReader orchestration into common SharedCoachSnapshotReader.
  Same baseline/manual-overlay/eligibility calculations; same actual evidence, optional adaptive
  proposal/weekly-review deduplication; same reminder reconciliation order; same authored-versus-
  legacy Strength Today selection; same stored-input priority, choices/exclusions/links and settings.
  Uses the existing common AdaptiveCoaching kernel (JVM Optional wrapper removed from caller),
  injected real clock/local date and platform IO dispatcher. No training/recording calculation changed.
- CoachSnapshotSources is a REQUIRED typed host boundary, without empty/default implementations:
  actual permission reads, imported coverage, profile/draft, retained development/gym/user evidence
  and reminder delivery stay explicitly host-bound. Existing Android CoachSnapshotReader constructor
  stays source-compatible and now only binds these real Android stores/services to the shared reader.
  Android continues using the shared reader through the existing CoachApplicationServices/serial runner.
- SIX new real SQLite Android tests cover Strength-only/no race/no health setup without invented
  evidence/SQL writes; permission revocation as unknown; granted permission without fabricated
  activity/eligibility; recent-versus84day import coverage with exact nanoseconds; saved-input/units/
  choices/exclusions/provenance; repeated optional weekly review stored once BEFORE reminder
  reconciliation without plan activation. Existing completion/onboarding/interaction tests retained.
  Added this class to focused CI: new expected566Android (560+6). Native should run corrected
  628storage with205matching IO/SQL/health/coaching names and compile the ACTUAL shared snapshot.
- Local common metadata, corrected8coaching JVM tests and Android production/test compilation PASS
  2m32s/77tasks before final formatting. FINAL formatted8coaching JVM tests/common metadata/Android
  production+test compile/lint PASS4m36s/137tasks, scratch/kmp-shared-snapshot-final.log. Pinned926
  sources(831Kotlin/95Java), exact19migration boundaries and75schema tests PASS; diff checks clean.
- NEXT: inspect this new exact-head CI, fix actual failures before changing persistence further.
  Then bind shared snapshot sources to real shared repositories (operational weekly/development
  context is still Android-hosted), migrate remaining application services/root/navigation and
  implement actual iOS app host/services/permissions. No new fake empty native bindings; no separate
  product/demo. Full native HealthKit owner/HR/route, GPS/BLE/notifications/audio/lifecycle and ONE
  all-mutation/backup operation owner remain unfinished. Native destructive UI OFF. Full final gates,
  screenshots, physical checks, no early bump/merge/publication remain as below.

### Checkpoint111 — operational coaching context and weekly decisions (17 November)

- Preserved1102c08a7c and its shared snapshot. Added SqliteCoachingContext over the SAME schema19
  coach_evidence rows, using the established DevelopmentArchiveJson typed decoder. Imported
  weights stay available LOCALLY; the unchanged backup exporter still excludes provider caches.
  Historical PBs retain unknown dates/measurement identity; generic strength dates and explicit
  gym associations remain distinct. Existing row/time ordering, exact nanoseconds and optional
  values are preserved; reads never create actual workouts or rewrite retained payloads.
- Existing session decisions/interruption/review writes and explicit weekly choices now use this
  common implementation. The SAME WeeklyCoaching.apply and full shared CoachingRepository enforce
  base/choice/completed-prescription guards. Only an explicit nonempty replacement creates a new
  immutable revision. Keep creates only a decision receipt. Receipt and revision share the original
  DB/outer transaction; enclosing rejection rolls both back. Existing week/recommendation/ordered-
  observations matching is retained; no freshness score or new coaching engine/calculation.
- Production Android DevelopmentStore delegates these reads/commands through lazy borrowed
  AndroidSqlConnection over its original ProfileDatabase. Roughly180duplicate operational Java
  lines removed. Original independent backup export/validate/restore and remaining manual-weight /
  historical/link writers are retained; legacy association projections remain authoritative.
- EIGHT new JDBC/Apple contracts cover local imported/manual weight order and export distinction;
  unknown historical dates/generic vs gym links without fabricated actuals; retained interruption /
  decision times; complete optional review context without activation; keep/double choice/context
  matching; one explicitly accepted revision retaining completed prescription/response; stale /
  unknown/completed rejection before writes; whole-operation rollback of revision AND receipt.
- TWO new real Android tests use the UNCHANGED original DevelopmentStore exporter/validator/
  restorer against common reviews/decisions and imported/manual weight rows. Complete review
  observations/channels/replacements/nullable longest value and chosen immutable revision must
  remain readable; export/restore preserves the decision and original programme history.
  Added to focused CI: expected568Android (566+2), new636storage JVM/Native and213matching
  IO/SQL/health/coaching/context names. No schema/backup/permissions/dependency/version change.
- Focused8new JVM tests +metadata/Android production+test compilation PASS4m6s/66tasks; earlier
  metadata/Android production compilation PASS2m30s/51tasks. FINAL636storage JVM, zero fail/error/
  skip, common metadata/Android compilation/lint PASS4m12s/137tasks,
  scratch/kmp-coaching-context-final.log.
  Pinned931sources(836Kotlin/95Java), formatter14/source3/exact19migration boundaries PASS. No
  new Android/Native runtime result claimed before exact-head CI.
- NEXT: bind the shared snapshot inputs to common profile/draft, gym, preferences and this coaching
  context through the real application owner. Then remaining app services/root/navigation and
  actual iOS host/platform services. Preserve the existing Android permission/reminder/Health
  Connect/recording adapters. No fake native defaults or demo replacing the agreed full app.
  Native all-mutation/backup owner and full host/permissions/HealthKit/GPS/BLE/audio/notifications,
  actual iOS APP runtime and final full gates remain unfinished; destructive native UI stays OFF.

### Checkpoint112 — correct Android snapshot fixtures without relaxing migration safety

-110experiment44061576597: Android133964517505 finished with6failures, ALL the new snapshot
  tests failing BEFORE their assertions with "Unversioned existing database; files retained."
  Existing560cases still passed. Completed log scratch/snapshot-2c08-android-job.log inspected.
  Cause: Android SQLiteDatabase.create(null) creates android_metadata, so it is NOT an empty
  portable SQLite connection. ApplicationSchema's guard correctly rejects unversioned tables.
  Do NOT weaken that production guard or remove domain/assertion coverage to force a pass.
- New test-only createAndroidSchemaFixture requires a NEW :memory:/version0database, then calls
  actual ProfileDatabase.onConfigure/onCreate and sets the ORIGINAL current version transactionally.
  The helper never opens its own named database or changes the shared application helper; its
  borrowed fixture is still closed by each test. Subsequent ApplicationSchema.open must find the
  versioned schema and must NOT call the fresh-install callback. No dropping metadata, no special
  production migration exception, and no test assertion weakening.
- Applied to the SIX existing snapshot tests and TWO new context/old-backup interoperability
  tests (the latter would have hit the same fixture issue). Production code is unchanged from111.
  Pinned932sources(837Kotlin/95Java) PASS. Corrected Android test compilation/lint PASS2m31s/
  115tasks, scratch/kmp-android-schema-fixture-final.log. Actual runtime verification of these8
  cases remains pending.111's636JVM storage/meta/Android compile/
  lint result is valid; it does not establish Android fixture/runtime success.
-1117212d4a is committed locally, not yet pushed.110Apple133964517015 is STILL RUNNING; let this
  useful corrected Native transaction/snapshot compilation/test gate finish rather than cancelling
  it with an immediate branch push. Then commit/push this coherent fixture correction with111,
  inspect the next actual Native636/Android568result and fix actual failures before further binding.
  Keep24DRAFT. No merge/version bump/public APK. All full cross-platform scope remains below.

### Checkpoint113 — actual common snapshot repository binding

-1117212d4a/context and11216e8c65/Android-fixture correction are LOCALLY COMMITTED, not pushed;
  remote remains1102c08a7c.110Android failed ONLY6new unversioned-memory fixtures;112retains the
  safeguard/assertions and uses original Android schema owner.110Apple133964517015 still running;
  let it finish before pushing, rather than cancelling useful Native compilation/tests.
- Surviving113local work: common SqliteCoachSnapshotSources binds all profile/draft/focus,
  coaching context, legacy gym, choices/exclusions/PB acknowledgement and shoe-feedback inputs
  over ONE borrowed database. CoachSnapshotPlatform REQUIRES the six actual health/import/reminder
  hooks; no empty fake native defaults. Android CoachApplicationServices now uses this common
  source and shared reader directly, retaining real HealthConnectGateway/EveningReminders hooks.
  Superseded Android CoachSnapshotReader wrapper is deleted; do not reconstruct it.
- AndroidSqlConnection.forApplication is a lazy IO-checked view of the SAME ProfileDatabase
  pool, no new database/ownership/permission. SqliteAthleteProfile now has exact-byte draft
  load/save/clear; SqliteRunningSupportArchives has a targeted typed shoe-feedback read which
  never hydrates unrelated routes/workouts. Existing schema/backup versions and source guards stay.
- Three real JDBC/Apple cases cover exact/invalid/rollback draft input and independent shoe
  preference reads; two actual SQL snapshot tests cover full stored-input projection and persisted
  optional weekly-review deduplication without activation; one Android factory case guards UI-
  thread SQL. Expected next CI639storage/571Android (not yet runtime-verified).
- Initial common metadata/Android production compile PASS2m33s/62tasks; focused32StorageTest /
  common metadata/Android production+test compile PASS4m26s/77tasks. FINAL639storage JVM with0
  failure/error/skip +common metadata/Android production+test compilation/lint PASS3m39s/137tasks,
  scratch/kmp-snapshot-binding-final.log. Pinned932sources(837Kotlin/95Java), exact19migration
  boundaries and source3tests PASS; diff checks clean. No new native/Android runtime pass claimed.
-113a8849cd is committed locally with1117212d4a and11216e8c65; remote remains1102c08a7c.
 110Apple133964517015/experiment44061576597 remains in progress beyond its preceding13minute
  successful job; live log download returned404/BlobNotFound, so the actual cause is unknown.
  Inspect actual output before any retry. Then push this coherent111–113batch ONCE, keep24DRAFT,
  and verify new Native639/framework and Android571. No weaker assertion/migration guard, blind
  retry, early version bump, merge or publication. Full native app and final gates remain pending.

### Checkpoint114 — inspect slow Apple run and expose Native verification stages

- The already-failing110workflow44061576597 was cancelled for DIAGNOSIS after about40minutes
  (previous complete Apple run13minutes), when GitHub's live-log endpoint returned404. GitHub now
  confirms CANCELLED, not successful. Completed Apple133964517015 output is saved at
  scratch/snapshot-2c08-apple-job.log. No blind retry or claimed iOS-app/Native-storage pass.
- Actual evidence: cold Gradle cache and LLVM/libffi downloads; all common storage/test/UI Kotlin
  compilation completed (including the corrected transaction-interface test). Native DOMAIN449
  and HEALTH27tests completed0fail/error/skip. Storage test execution and Compose framework link
  were still incomplete at cancellation; no test failure/application cause was reported. The
  output shows overlapping heavy framework codegen and simulator work, but contention/deadlock is
  NOT established as a confirmed product defect. Do not speculate or claim a complete Native gate.
- Downloaded actual partial Native artifact14320987624:80,892ZIPbytes/SHA256
  784ec941b6cc92564290e08cde05bb9a4d6dfa6f084cdca6a78a6e835ac7f3d8. Only449domain/27health XML
  is present, NOT storage results. Proof scratch/snapshot-2c08-native/partial-results.json.
  Actual JVM14318853705:183,656ZIPbytes/SHAa31804cce0b5d4a65134b0590575f9c5015fadd3847810d8b5eecce915304641,
  490domain/25health/628storage all0fail/error/skip. Android566unique START/END /560passed and6new
  fixture failures BEFORE assertions independently checked; original-sourceproofs in
  scratch/snapshot-2c08-{jvm,android-proof.json}. No experimental public APK published.
- TARGETED diagnostic workflow correction: run the SAME Native domain/health/storage suites
  with max2workers/no parallel projects/info; upload completed XML BEFORE a separate framework
  link step. Both iOSX64 and simulatorArm64 targets and framework builds remain; tests/assertions
  are not removed/filtered, failure does not permit framework/publication.60minute Apple job bound
  avoids the default6hour invisible wait. No cache-policy/Android/full-release-gate bypass.
  Parsed YAML checks confirm all6test targets, both framework targets, strict stage order and
  early XMLartifact preservation. Full exact-head runtime verification is still REQUIRED.
- NEXT: commit114workflow/recovery, push the complete111–114batch ONCE, update DRAFT24 and inspect
  the new actual Native639storage/framework and Android571results (including original Android-
  schema fixture correction). Fix any true failure before another retry. Local639storage/meta /
  Android compile/lint, format932/exact-schema/source3 remain green from113. Next actual migration
  work is remaining application services/root/navigation/native host, not repeated kernel ports.

### Checkpoint114 verified — actual shared snapshot/backend/source gate GREEN

- Experiment44068485578 and format44068485539 completed SUCCESS at378d1354d6575cef041e8bd8be8d95bdd116f6ed.
  Android133988290735 passed571unique START/END cases, including ALL corrected schema fixtures /
  shared SQL snapshot sources/weekly context/IO factory and existing coaching/browser/recording
  tests.700,189domain/48suites, schema75, all3builds/lint pass. Completed log and exact-count proof:
  scratch/snapshot-378-android-{job.log,proof.json}. No new-head Browser115runtime result implied.
- Apple133988291172 passed actual Native suites and linked real Compose framework in SEPARATE
  stages: tests BUILD SUCCESS12m35s; framework BUILD SUCCESS5m27s. New workflow preserves all
  tests and makes each stage observable; no confirmed native application defect was found in
  the previous diagnostic cancellation. Whole new workflow is GREEN, not merely a compile claim.
- Byte/digest/commit-verified artifacts: JVM14321339653 ZIP185,244bytes/SHA256
  96bdb61220c3528d63fabea8865b9c3ae7785671a73e3ef9c512fa7f6fddd546:
  490domain/25health/639storage. Native14323285200 ZIP176,015/SHA
  6aea817c07ec7d9c62b1d25220e2abfecb3e4635c87b2d9c83c3bbaf0c871564:
  449domain/27health/639storage. ALL0failure/error/skip.213matching IO/SQL/health/coaching/context
  names plus3new exact matching draft/shoe preference cases independently compared. Proofs in
  scratch/snapshot-378-{jvm,native}/verification.json and native/{all-case-proof,snapshot-preference-case-proof}.json.
- PrivateAPK14321204971 ZIP30,747,897bytes/SHA78b7ffe26235feb7826a6de94a33cb4238918e3180f4b851c115fc688c2c10d4;
  APK58,369,350bytes/SHA279fd441907f65831ebab0c958a6363b4db617e09f4ecfb0e8cb50ac5735942a,
  aapt0.12.3/code19/min28/noINTERNET, scratch/snapshot-378-apk/verification.json.
  Framework14322084328 linked successfully; it is NOT an iOS app. No public experimental release.

### Checkpoint115 — actual shared Exercise Library UI

- Latest local115work moves the ACTUAL850line ExerciseBrowseScreen into common
  SharedExerciseBrowseScreen, retaining all search/alias/ranking, personal/category/equipment /
  movement filters, exclusions/restoration, cards, disclosures, custom metadata, dialog, semantics
  and200% layout. Existing Android API delegates with real native thumbnails and UUID source.
  No duplicated screen, UI redesign, storage/domain/recording calculation or new feature.
- Library search field and EXACT existing Canvas search/chevron/movement icons now common in
  ExerciseLibraryControls. Android ExerciseLibraryComponents retains its actual BitmapFactory /
  resource/bounded thumbnail cache. Common browser REQUIRES composable thumbnail and ID providers;
  no fake native images/identities. Controlled ASCII equipment enum labels retain their English
  casing; special programme GOAL labels remain untouched. Existing custom metadata detail helper
  is public across modules; no duplicate formula/classification implementation.
- TWO direct shared-browser Android tests verify real custom controls in dark200% mode: host ID
  requested ONLY after explicit save; measurement preserved/no unsupported estimated1RM enabled;
  classified custom action remains with host and never creates an unclassified exercise. Seven
  original browser and existing custom-exercise cases remain unchanged. Next focused count573
  (571+2); storage639 unchanged. No runtime/screenshot claim for these new cases before CI.
- Initial common metadata/Android prod+test compile PASS2m41s/73tasks. FINAL formatted common
  metadata/Android production+test compile/lint PASS5m8s/133tasks,
  scratch/kmp-exercise-browser-final.log. Formatting935sources(840Kotlin/95Java), formatter14 /
  source3tests PASS; diff checks clean. Storage/domain/backup behavior unchanged;114actual639
  Native/JVM result remains valid for those unchanged sources. New browser Native compilation /
  Android runtime must be checked on next candidate. Full final screenshots/readiness still pending.
- Exact next: commit115coherently; continue remaining common application services/root/active UI
  and actual native host. Coherent batches/targeted tests, then one push/new Native639/framework /
  Android573gate.114CI is complete, so there is no running old gate to cancel now. No early
  version bump, merge, public artifact or destructive native UI. Keep24DRAFT/full scope intact.

### Checkpoint116 — shared profile application service

-1157ae6d68(shared full Exercise Library) is locally committed, not pushed; remote114378d135
  remains FULLY GREEN as byte-verified above (Android571/Native639/framework). Preserve all code.
- Surviving116local work MOVES actual ProfileOperations into sharedUi/commonMain, changing only
  its concrete repository binding from JVM Optional/Android to existing SqliteAthleteProfile.
  SAME explicit Performance eligibility/retained-strategy rules, same saved eligibility/goal,
  draft lifecycle and baseline.reviewed effect. No training calculation change. Old Android class
  is deleted, not duplicated. Android CoachApplicationServices uses the common service and SAME
  lazy, IO-checked borrowed database view as common snapshot sources; legacy profile adapter
  remains for other unmigrated Android operations/reset compatibility.
- THREE real Android SQL tests cover new unsupported Performance choice retaining goal/draft;
  supported choice and explicit retention without fabricated eligibility/history/automatic plan;
  injected profile replacement failure rolling back prior profile/draft without false review flag.
  Existing profile/onboarding/interaction cases retained. Next combined focused count576:
  last verified571 +115's2browser +116's3profile. Domain/storage639 sources are unchanged.
- FINAL common metadata/debugAPK/instrumentationAPK/lint PASS6m20s/176tasks,
  scratch/kmp-profile-service-final.log. Formatting936sources(841Kotlin/95Java), exact schema19 /
  source3 checks PASS; diff checks clean. No new profile/browser runtime/Native compilation pass
  claimed before exact-head CI. Unchanged storage639/domain sources retain114actual verification.
- Exact next: commit116and push115+116ONCE (114CI already finished), update24DRAFT with actual
  next-head CI. Verify new Native
 639/framework and Android576 including all five new UI/policy cases; never blindly rerun/fakepass.
  Then remaining application services/active UI/root/navigation/actual native host/services. Native
  destructive UI OFF, no early bump/merge/publication. This is still the FULL agreed migration.

### Checkpoint116 actual remote verification; checkpoint117 surviving local work

-116 was committed/pushed as9cd4adf2c54a10bddb42ebca039ea4705b2d86f5, including1157ae6d68.
  Both experiment44074597919 jobs and formatting44074597892 are SUCCESS. Android job134009270049
  has576 matching unique START/END cases, including all2shared-browser and3profile SQL tests.
  Apple134009269492 passed actual Native tests AND separate framework linkage. No profile fixture
  issue was demonstrated; do not change valid tests based on an earlier unverified suspicion.
- Independently downloaded/digest-verified result ZIPs: Native14326021083 (176032bytes,
  SHA280ca031983127222097edb057cdba7705427e81bf047d983302aade4c7d670a):449domain/27health/
  639storage; JVM14324918435 (184937bytes,SHA882765a91b357a873bab972e5dd22ab220abaf21a083b08a9943500b31f4f332):
  490domain/25health/639storage. ALL zero failures/errors/skips. Exact source116. Proof
  /workspace/scratch/kmp116-results/verification.json and kmp116-android-job.log.
-117 uncommitted work moves the ACTUAL existing four-tab Exercise Detail into common Compose,
  with required original host date/number/set formatters, technique/artwork and explicit actions.
  Android signature delegates; no altered PB/history/progression formulas. Exclusion control moves
  to common sameFQN, saved reason uses enum name for portability. Plain text/decimal input is common;
  Android date-picker path retained. Three new direct production UI tests cover actual/estimated
  PB distinction/source navigation, dark200custom rename/restore with frozen history, timed PB
  with unknown date and no invented estimated lift. Existing2exclusion tests added to focused gate.
  Next expected Android581 (576+3+2), storage639 unchanged.
- Initial local117 compile caught a malformed extracted function signature; corrected before the
  current full metadata/debugAPK/testAPK/lint build. CURRENT outcome is SUCCESS; log
  /workspace/scratch/kmp-exercise-detail-final.log. Never infer a pass from116 or the old failed log.
  New files and intentional old exclusion deletion must be preserved. Formatting938sources (843Kotlin/95Java), exact schema19, source3 and formatter14 tests PASS. No117runtime/screenshot claim.

### Checkpoint118 local — common gym programme preview operations

-11752f511c is committed locally, not yet pushed: metadata/debugAPK/testAPK/lint SUCCESS6m11s/
  176tasks; format938 (843Kotlin/95Java), exact schema19/source3/formatter14 PASS. Original detail,
  history/PBs/exclusions/host technique preserved. Runtime expected581 remains pending.
-118 local work moves actual generate/move/reduce/skip/swap/alternative/conflict operations into
  SharedGymProgrammeOperations. Original Android API delegates and supplies ONE real clock/zone
  per action, default zone resolved per action so retained coordinators observe timezone changes.
  Explicit fixed test clocks and real UUID generation are retained. Common operations require
  clock/ID bindings; no fake defaults, storage writes, auto-activation or new training formula.
- Existing SEVEN GymProgrammeOperationsTest cases are unchanged and added to focused gate:
  immutable regeneration, targeted edits, completed-history protection, incompatible-load removal,
  equipment/measurement alternatives, actual lower-body conflicts, retained-timezone behavior.
  Next batch expected588Android (581+7), storage639 unchanged. FINAL metadata/Android prod+test
  compile/lint PASS5m44s/133tasks, formatting939(844Kotlin/95Java), diff clean; log scratch/kmp-gym-preview-final.log. New Native/framework/runtime
  results cannot be inferred from116 success. Continue remaining shared calendar presentation next.

### Checkpoint119 local — existing training calendar in common Compose

-11752f511c and118889452c are committed LOCALLY, not yet pushed.118final metadata/Android prod+test
  compile/lint passed5m44s/133tasks; formatting939(844Kotlin/95Java). Original SEVEN preview tests
  are retained unchanged and included in next actual runtime gate.116 remains latest pushed green.
-119 preserves the ACTUAL full calendar: entry assembly and authored/legacy associations, exact
  category/status marker Canvas, legend/disclosure, persistent List/Week/Month selection,
  previous/today/next, six-row month and selected-day sheet, original workout/actual/logging links.
  Android signature delegates and retains original UK labels/system-zone date arithmetic/units
  and real ActivityHistoryScreen. Required host calendar/label/history hooks have no fake defaults.
  Original bottom navigation and all plan/history calculations remain untouched.
- Added TWO direct production common-calendar tests: dark200week/month prescription and original
  navigation identity; List uses the required host content and returns view choices to its owner.
  Existing TWO CalendarAccessibilityTest cases added unchanged, existing WeeklyUxTest retained.
  Combined expected Android592; domain/storage639 unchanged. Native compile/full APP/screenshots
  are NOT inferred from an Android compile or the prior116 framework.
- Initial119 common compilation found2JVM-only Duration.toMinutes calls. Replaced with the
  EXACT original whole-seconds/60 conversion; no elapsed/timing or training rule change. Corrected
  metadata/debugAPK/testAPK/lint PASS6m23s/176tasks in scratch/kmp-calendar-corrected-final.log;
  formatting942(847Kotlin/95Java) passed before the two-call fix; final formatter recheck PASS942sources
  in scratch/kmp-calendar-corrected-format.log. Source3/exact schema19 PASS, focused
  YAML list parses and contains110distinct classes. A script stopped at an assertion while adding
  the final formatting target (old last target had no comma); safely finished through parsed JSON.
  All source files survive; no discarded/restarted implementation and no runtime pass claimed.
- Exact next: inspect CURRENT119 compile/lint/format, fix genuine failures; commit119 +updated
  recovery and push117–119 ONCE to existing DRAFT24. Last116 CI is complete. Verify exact new
  Android592/Native639/framework gates and new detail/calendar/preview cases before claiming pass.
  Then continue remaining original application services/active UI/root/navigation and native host/
  platform bindings. No version bump, merge, public build or native destructive controls yet.

### Checkpoint120 local — typed body-weight commands and common progress application service

-119bec8ea0 is committed locally: corrected metadata/debugAPK/testAPK/lint6m23s/176tasks PASS,
  formatter942(847Kotlin/95Java), source3/schema19 PASS.117–119 are NOT yet pushed; preserve them.
-120 moves exact manual weight save/delete and authoritative half-open provider-window replacement
  into existing SqliteCoachingContext over SAME coach_evidence rows/SQLite pool. DevelopmentStore
  delegates, retaining independent old backup helpers. No new schema, fields or inferred history.
  Visible-only Apple reads MUST keep their existing qualified merge, never window replacement.
- Actual ProgressCoordinator moves common (same FQN): same future-date guard, current unit
  conversion, original identity/edit semantics, manual provenance and serial runner. Android root
  supplies real per-action system-zone local noon/today and UUID over its existing lazy IO-checked
  database. No cached timezone, fake provider data, new DB or parallel coaching engine.
- TWO new common SQL contracts (both inherited by JDBC/Apple) cover manual/edit/delete provenance,
  unchanged imported bytes/user-owned export/no invented training, inclusive-start/exclusive-end
  provider scope, late mixed-provider failure rollback and repeatable replacement. Expected storage
  641 (639+2). Existing Android WeeklyUx/body/health/store/backup cases remain in focused runtime.
- FINAL JVM641storage (0fail/error/skip) +common metadata/debugAPK/testAPK/lint PASS,
  scratch/kmp-weight-commands-final.log. Formatter942/exact schema19/source3 PASS. Local APK
  inspection:0.12.3-test1/code19/min28/noINTERNET; no public upload. No120Android runtime/Native
  pass inferred from this local result; actual new checkpoint CI is required.
- Exact next: inspect this CURRENT120 build and XML, fix real failures; commit120+recovery and push
 117–120 ONCE to existing DRAFT24. Prior116 is fully green, no running gate to cancel. Inspect exact
  new Android592/Native641/framework and all newly included UI/preview/weight contracts. Then
  remaining application services/active UI/root/navigation/full native permissions and real iOS
  app host. Destructive native UI remains OFF; no premature version bump/merge/publication.

### Checkpoint121 local — baseline assessment application service; exact120CI pending

-11752f511c,118889452c,119bec8ea0 and1206d72b94042b7f4401c903e174bd709e7011bdc0c were pushed
  together normally. PR24 remains DRAFT, description updated. No running gate cancelled.
  Experiment44083171900 currently RUNNING (Android134038690212 /Apple134038689805);
  formatting44083171002 SUCCESS. No592/Native641/framework pass claimed before actual results.
-120final local641storage JVM (0fail/error/skip), metadata/debugAPK/testAPK/lint9m21s/180tasks,
  format942/schema19/source3 passed. Local APKcode19/min28/noINTERNET; unpublished. Previous
  full116runtime and byte-verified artifacts remain documented; do not treat them as new-head proof.
-121 extracts original assessment/manual-baseline/review orchestration to SharedBaselineCoordinator.
  Android API is a thin real timezone/UUID/wall-clock binding. Same 1–5K/future-start/end guards,
  explicit manual provenance, unknown effort, callback messages and atomic activity+assessment
  save. Required timing projection keeps end conversion lazy until original future-start check;
  mismatched host duration is rejected rather than persisting contradictory actuals. No new formula.
- THREE new direct common-service/real-runner/SQLite tests cover Europe/London DST start/end offsets,
  manual evidence and unknown effort/no automatic plan/review, future check before end/ID allocation,
  host timing mismatch, late assessment insertion rollback retaining prior manual baseline. Existing
  baseline/onboarding cases retained. Next expected Android595 (592+3), storage641 unchanged.
- Source review also CONFIRMED117new test used wrong oracle: actual StrengthAnalytics emits
  category `Load × 6`, displayed as "Best weight for 6 repetitions", not literal"6RM". Same logged
  set legitimately appears in TWO actual categories (Load and Load×6), so its number is not unique.
  Corrected new test to assert exact natural label and exactly2actual values, preserving estimated
  separation/source navigation. NO production/PB calculation change or weakened assertion.120CI
  may report this old assertion failure; inspect actual completed log, do not blindly rerun it.
-121metadata/debugAPK/testAPK/lint PASS6m29s/176tasks, scratch/kmp-baseline-service-final.log.
  FINAL corrected-input testAPK/lint PASS45s/140tasks, kmp-baseline-service-exact-final.log;
  formatting944(849Kotlin/95Java), exact schema19/source3 PASS. Test-only oracle fix is committed
  LOCALLY as992bcba, not pushed. No121runtime/Native/screenshot acceptance claim.
-120Native artifact14328303292 is independently downloaded/digest-verified:176312bytes,
  SHA3a5e9f850bc9aec6bb7d4f1977721859e1dab02060f4c55900e0e0c3dc2d0d95,
  449domain/27health/641storage with0fail/error/skip. Proof scratch/kmp120-native/verification.json.
  Actual new weight SQL cases passed on Apple. Framework linkage/Android runtime remain pending.
- Exact next: finish121current build, fix real compile failures; inspect120actual CI (including new
  detail/calendar/preview cases and Native641XML). If assertion failure matches proven oracle bug,
  retain correction and verify new candidate once prior useful CI completes. Run final current
  testAPK/lint/format, commit121+handoff and push ONCE, expected595Android/Native641/framework.
  Then remaining services/root/active UI and real iOS host; never merge incomplete migration.

### Checkpoint120 final CI outcome /121 ready to push

- Experiment44083171900 COMPLETED on6d72b94. Apple134038689805 SUCCESS: Native tests15m14s and
  real common UI framework linkage4m45s. Framework artifact14328624574 (metadata ZIP61978408bytes,
  SHA49222470c7aecd2ba18fbc33a5c8d82ebeb011dc941487a076db26ed499b7d02); not an iOS app.
- Android134038690212 executed592distinct matching START/END tests; exactly ONE failed:
  SharedExerciseDetailTest.recordedAndEstimatedRecordsStayDistinctAndOpenTheActualSourceWorkout
  expected literal6RM (no such label). Other591passed, including BOTH new calendar tests, existing
  calendar accessibility, seven preview cases, exclusions and remaining detail tests. Actual log
  scratch/kmp120-android-job.log. No product/PB engine defect asserted or masked; do not rerun bad head.
  Fix992bcba already uses the actual label and exactly2actual value nodes (stronger correct oracle).
- Independently downloaded/digest-verified JVM artifact14329965774:185441bytes,
  SHA1df147a211c98299dde9f538e3ee76535934e65a422c8bdbf2f3725d7f3385e1,
  490domain/25health/641storage ALL0fail/error/skip. Native14328303292 proof above also641PASS.
  scratch/kmp120-jvm/verification.json; Native kmp120-native/verification.json. No APK artifact
  published/uploaded from failing Android checkpoint; no public build or merge.
-121baseline source3eb93a5 and test correction992bcba are committed locally and fully compiled:
  metadata/debugAPK/testAPK/lint6m29s/176tasks, final exact corrected-input testAPK/lint45s/140tasks,
  format944(849Kotlin/95Java)/schema19/source3 PASS. Native/runtime of121pending. Preserve both.
- Next: push these local commits +this coherent recovery ONCE to existing DRAFT24 (old CI now
  complete, no cancellation). New expected595Android/Native641/framework. Inspect exact result,
  fix real failures; continue original remaining application services/active UI/root/native app.

### Checkpoint122 local — actual running-plan orchestration;121 external dependency failure

-1213eb93a5baseline +992bcbadetail-test fix +recovery7e19a63 were pushed normally. PR24DRAFT,
  latest remote7e19a63e2a6d133522f3d29ae2b24ec47977b6d8. Format44086846391 SUCCESS.
- Actual121experiment44086846386 Android134051388442 FAILED BEFORE ANY TESTS during root
  classpath configuration: repo.maven.apache.org returnedHTTP429 for pinned Kotlin2.1.21 Gradle
  plugin/BOM/Compose-plugin POMs. No JVM results/APKs/runtime tests were produced. Exact log
  scratch/kmp121-android-job.log. This is a confirmed dependency-download failure, NOT a test or
  compilation assertion failure. No repository/dependency pin/TLS/check changes or blind reruns.
  Apple134051387693 continues usefully (Native tests passed; framework linking as last checked).
  Let it finish; next coherent candidate may retry resolution after the actual cooldown once.
-122moves the ACTUAL RunningPlanCoordinator into common sameFQN. Same completed-response/legacy
  actual preservation on regeneration, explicit weekly choices/interruption/easy/move/skip rules,
  race/occupied-day/maximum-weekly-frequency guards and frozen source/goal/evidence/version data.
  Same atomic revision+decision transaction uses explicit cross-platform CoachingTransaction,
  no Native-unsupported SAM. Date ordering/Monday derive from exact ISO epoch-day/weekday; UK
  evidence labels/today/wall-clock/real UUID remain required actual Android bindings. No formula change.
- Original schedule has a targeted typed projection over existing release08config row using the
  already-reviewed codec. Shoe feedback reuses the same targeted reader; unrelated dense/custom
  payloads are not hydrated. No rewrite/new legacy bucket data/database/schema. Shared profile/
  context objects in Android root reuse ONE original lazy IO-checked database.
- TWO common JDBC/Apple SQL contracts cover absent schedule, untouched bytes/unknown metadata,
  duplicate unavailable-day semantics/unrelated malformed routes, future/missing/invalid prefs
  rejection without invented defaults. Review found new reader's lexical.int would reject old
  JSONObject.getInt-compatible encodings; corrected to EXISTING SupportPayloadJson.integer and
  added real persisted decimal/quoted/Long-wrap variants asserting45 and byte preservation.
- THREE new real SQLite/runner application tests cover stored schedule with immutable completed
  prescription/actual/source/draft, explicit move/revision/provenance plus completed guard, and
  late decision insertion rollback retaining original plan/actual/response/draft. Existing tests
  unchanged. Next expected598Android (595+3) /643storage (641+2).
- Initial122 JVM643(0fail/error/skip)/common metadata/debugAPK/testAPK/lint PASS8m17s/180tasks.
  FINAL numeric-compatibility-corrected source PASS5m5s/180tasks, JVM643ALL0fail/error/skip,
  scratch/kmp-running-plan-corrected-final.log; format945(850Kotlin/95Java), schema19/source3
  PASS, kmp-running-plan-corrected-format.log. No new runtime/Native acceptance inferred.
- Exact next: inspect final122 build/XML/formatter, fix real failures; inspect121Apple final
  result without cancellation. Commit122+handoff when local gates pass, then one normal push
  once old useful Apple finishes. Verify new598Android/643Native/framework and corrected detail/
  baseline/running-plan tests. If Maven429 recurs, record actual failure and avoid repeated blind
  retry or repository/verification bypass. Continue remaining RunningEvidence/Strength/Health/
  Settings application services, active UI/shared root/navigation and REAL iOS host/platform
  bindings. Native destructive controls remain OFF; no premature merge/version/publication.

### Checkpoint122 final local verification and121 actual Apple result

- FINAL122 corrected local source passes643storage JVM (ALL0fail/error/skip), common metadata,
  debugAPK/testAPK/lint5m5s/180tasks. This includes numeric strings/decimals/Long-wrap compatibility
  and all unchanged repository/backup/retention contracts. Source3/exact schema19 and formatting945
  (850Kotlin/95Java) PASS. Initial pre-correction build8m17s also passed; do not confuse its result
  with final corrected source. No new Android runtime/Native/visual screenshot pass claimed.
-121Apple134051387693 SUCCESS on7e19a63: real Native tests/framework. Independently verified
  Native artifact14331556396 (177169bytes,SHAfded762ca458221e98f993a2062239ad7938d179a0e002c34cd8e111e66f746f):
  449domain/27health/641storage,0fail/error/skip. scratch/kmp121-native/verification.json. Framework
  artifact14331632496 metadataZIP61988191/SHA00584411fbc000c17d363bfa03bdbd15cb48963e16bd81528cc11642d2714e96;
  compiled and linked, still NOT an iOS application. No false infra/Xcode blocker.
-121Android remains UNVERIFIED (before all tests) due confirmed Maven429 at12:51. Current13:20+
  is a real cooldown; no dependency/repository/TLS/cache-policy change, assertion reduction or
  blind rerun. The next coherent source candidate retries normal pinned resolution once. If429
  persists, record exact external limitation, preserve work and avoid endless retries.
- Exact next: commit122+this recovery, push once to existing DRAFT24 (121useful Apple has finished,
  so no cancellation). Expected598Android /Native643/framework. Inspect all corrected detail,
  baseline and running-plan tests, compare exact new SQL cases on JDBC/Apple and preserve final
  root/backup checks. Continue remaining RunningEvidence/Strength/Health/Settings services,
  active UI/root/navigation/native permissions and REAL iOS application, no fake empty host.

### Exact recovery after122 push — repeated Maven outage, no Android tests executed

- Source1221e2d6ff9adc09470e9c4c1dd67e4ab5ac052e8f2 is COMMITTED/PUSHED; PR24 still OPEN/DRAFT.
  All valid source, migrations, archive kernels and prior ports preserved. No resets/force pushes.
  No version/schema/format/permission change or public release. Latest README/PR description must
  distinguish whole-app remaining scope from actual Native framework success.
- New experiment44091963612 Android134069104331 FAILED at13:23:25 BEFORE tests/configuration
  completed: same repo.maven.apache.orgHTTP429 Kotlin Gradle plugin2.1.21, plugins BOM and Compose
  compiler-plugin POMs. Previous121failed12:51:30; actual32-minute cooldown did NOT restore service.
  Log scratch/kmp122-android-job.log. This is a confirmed external dependency-resolution blocker,
  not a new assertion/compilation failure. No598runtime/domain/JVM CI pass for current1e inferred.
- Apple134069104897 is STILL RUNNING (last observed Native test stage). Preserve it; no cancellation
  or whole-workflow rerun. Inspect actual conclusion/XML/framework later. Expected Native643 but
  not claimed until verified. Current formatting run44091963522 is SUCCESS.
- FINAL local current1e source PASS:643sharedStorage JVM,0fail/error/skip; common metadata/debugAPK/
  testAPK/lint5m5s/180tasks; format945(850Kotlin/95Java), exact schema19/source3. Numeric schedule
  compatibility and original Android JSON coercions are tested; no engine/prescription change.
  New common running-plan3tests and baseline3tests await actual Android runtime. Detail-test fix
  asserts actual supported label and exactly2actual record categories; do not reintroduce literal6RM.
- Prior121exact7e19a63 Apple/framework passed. Independently verified artifact14331556396 Native
  449domain/27health/641storage ALL0fail/error/skip; scratch/kmp121-native/verification.json.
  This is previous-source proof, NOT current643 proof or iOS application acceptance.
- Immediate next: inspect existing44091963612Apple result;44091963522format is SUCCESS. Once dependency
  service is demonstrably available/adequately cooled down, use ONE bounded failed-Android-job
  retry for exact1e, not repeated blind full workflows. If429 persists, preserve/report exact
  external limitation; do not substitute an insecure/unverified repository or weaken checks.
  When Android runs, inspect all598cases and fix real failures; verify new643JDBC/Apple XML and
  actual Native framework. Then remaining RunningEvidence/Strength/Health/Settings orchestration,
  active UI/shared root/navigation, native permissions/HealthKit/recording/BLE/notifications and
  REAL iOS app host/entitlements/build/runtime. Full Android28/35/36 +actual iOSAPP/canonical visual
  gates only at readiness. Native destructive controls OFF until one actual application operation
  owner binds every path. Physical S24/iPhone/HK/HC/GPS/BLE/audio/TalkBack remains unperformed.
- This last recovery note is intentionally LOCAL/uncommitted while useful Apple CI runs. A
  documentation-only push would cancel it under existing concurrency. Include the note with the
  next coherent implementation checkpoint; all IMPLEMENTATION is already safely remote at1e.

### Checkpoint123 local — full shared strength orchestration; diagnosed Android retry running

- Recovered exact1e2d6ff branch and surviving uncommitted recovery note; main85e1c39 unchanged,
  PR24OPEN/DRAFT, no source discarded or replacement PR. Read AGENTS/development workflow.
-122Apple SUCCESS: independently verified artifact14332373621,176074bytes,
  SHA4b4e3b757c042dea23cd3f966e9d18cb9d348cbc609e0a56cfa5bcb60abc0811;
  Native449domain/27health/643storage ALL0fail/error/skip. kmp122-native/verification.json.
  Actual framework linked (artifact14332818275 metadata62025757bytes,
  SHA59fb539108ac092003b28c71cb747a02feca8a38af789c11b7be92313e1c4f3a), not iOS app.
- Maven pinned-plugin POM HEAD returnedHTTP200 from workspace at13:37 (not a guarantee for GHA).
  Once original Apple job finished, ONE failed-Android-only rerun at13:45 after actual cooldown:
  run44091963612attempt2, Android134082808137. Resolution +JVM/domain/schema/build stages now PASS,
  actual instrumentation running. No dependencies/repositories/TLS/cache policy/check changes.
  Preserve this useful runtime job and inspect actual598 cases before claiming a pass.
-123moves ACTUAL StrengthCoordinator common, same FQN: same preview/explicit progression and load
  safeguards, exclusions vs favourites/archive/acknowledgements, actual save/delete, frozen custom
  definitions and historical PB future/unknown-date rules. Android root binds existing typed
  legacy gym/choices/exclusions/context over SAME borrowed DB and original real clock/ID adapter.
  Existing GymProgrammeOperations.shared becomes internal solely to reuse its genuine binding;
  no fake defaults/second engine. Cross-platform transactions use explicit CoachingTransaction.
- Added common typed Historical PB save/delete and legacy generic/gym link commands to existing
  SqliteCoachingContext; original DevelopmentStore delegates, independent backup helpers remain.
  Legacy row+normalized projection are ONE transaction; existing explicit decisions cannot be
  changed/removed by implicit link writes. Original nullable generic-target semantics retained.
  Historical exercise definitions/date precision/modalities retained, never fabricated actuals.
- THREE new JDBC/Apple SQL contracts verify frozen timed/weighted unknown-date PBs and caller
  rollback/no actual creation; generic/gym projection and explicit-decision protection; injected
  projection failure/outer rollback/idempotent deletion preserving exact bytes. Existing contracts
  unchanged. THREE new actual SQL/runner StrengthCoordinator tests verify save/link/rename/archive/
  exclusion/restore preserving actual/PB history; late association failure rolls back actual+link;
  historical future guard and unknown dates do not create workouts. Next expected601Android,
  storage646(643+3), no current-source Native or Android runtime acceptance inferred.
- FINAL local JVM646/common metadata/debugAPK/testAPK/lint PASS9m23s/180tasks,
  scratch/kmp-strength-coordinator-final.log. JVM646 ALL0fail/error/skip. Formatting946(851Kotlin/95Java), exact schema19 /
  source3 PASS, diff checks clean. No schema/backup/version/permission/publication changes.
- Exact next: finish local123 tasks and inspect actual current Android retry598. Diagnose/fix
  genuine failures without weakened assertions or speculative dependency workarounds. Commit123
  +updated handoff once local checks pass; push once prior useful retry completes. Verify new
 601Android/646JVM/Apple/framework, exact new SQL cases and source identity. Continue remaining
  RunningEvidence/platform health/settings/application root/active UI/navigation and genuine iOS
  host/services. Native destructive controls remain OFF until full operation-owner bindings.

### Checkpoint124 — actual shared running evidence and optional health orchestration

- Original RunningEvidenceCoordinator now common, same FQN and business rules. Results, shoes,
  notes, explicit associations, completion guards and no-invented-splits/effort remain unchanged.
  Required actual platform callbacks supply clock/today/IDs and local date/time+duration conversion
  with BOTH original offsets, including DST and nanoseconds. Existing ViewModel LocalTime API
  retained; direct shared calls pass canonical time text (including old completion test).
- Original release08 shoe-feedback and run-association commands have typed shared entrypoints
  through the existing codec. UPDATE/INSERT join the original outer coaching transaction,
  preserving actual/response/receipt/feedback atomicity. No new JSON bucket, schema or engine.
- TWO new real JDBC/Apple contracts verify exact association identity/time, sorted shoe choices,
  no actual creation, before-write validation, caller rollback and late receipt insertion failure.
  THREE actual Android SQL/runner cases cover DST/nanoseconds/manual-source/default shoe/unknown
  effort/no invented samples, immutable original targets, duplicate/future guards, imported
  provenance +raw-payload preservation, explicit receipts and feedback-failure rollback. Callback
  observation uses Main idle barrier, not arbitrary delays.
- Actual HealthGateway/result types and HealthCoordinator now common; original Health Connect
  SDK/permission/body-reader work remains Android, supplied through required boundaries. Original
  empty/partial/revoked permissions and reviewed-baseline auto-import rules remain unchanged.
  THREE new Android cases verify optional manual fallback, selective body sync, fresh permission
  checks and availability/review gating; no HealthKit read grant is fabricated.
- Final local health25/storage648 JVM ALL0fail/error/skip, metadata/debugAPK/testAPK/lint PASS
  8m24s/182tasks, scratch/kmp-running-health-final.log. Format949(854Kotlin/95Java), exact schema19 /
  source3 PASS. Initial test build identified direct old LocalTime call; adapted SAME time value,
  assertions untouched. Corrected running-only testAPK/lint passed2m32s before health batch.
- Remote12302efa0c experiment44100154646 is now SUCCESS:601unique Android0fail/skips,
  700189domain checks; JVM490/25/646 and Native449/27/646 independently digest-verified. JVM
  artifact14337745181 SHA6b1a148e3c5370d90ab16c6064741c2cf4dc100614af526b79e5138365ca5ff5;
  Native14335498585 SHAaef6c8854a5e2d811ee05dc95b87ee92c8e0116e8dafc4263aeaa81b6c93d53d.
  Framework14337046422 linked (metadata62068122/SHA0c4ccc6a8fa350afeb1bce005fb335dde2a6ef080af638ef25e4c81f42c603c5),
  NOT iOS app. APK14337590293 verified58418502bytes/SHA342af9fc08817215a1398e5170e57d34825fa2a1b298bab9e5ba6954884fd985,
  code19/min28/noINTERNET. Proof scratch/kmp123-{android,native}/verification.json.
-124source is committed locally as9d4554f (not pushed).125settings is now ALREADY APPLIED below;
  scratch preparation scripts must NOT be reapplied. Current combined candidate expects610Android /
  648Native; never infer that result from prior123 proof.

### Checkpoint125 — shared settings and original Android document/alarm owner

- Actual SettingsCoordinator now common, generic only for actual native selected-file identity.
  Original allowlist, focus/starting-path/milestone decisions, reset review and command callbacks
  are retained. Existing common SqlitePrimaryFocus uses SAME settings rows; original JVM Optional
  repository remains available unchanged. No new preferences or automatic programme changes.
- AndroidSettingsPlatform retains original BackupManager/URI IO, prepares bytes BEFORE truncating
  a selected document, cancels alarms before restore/reset and reconciles in finally even after
  rejection. No second backup/reset engine or Native destructive controls have been introduced.
- Common coordinator uses EXISTING ReminderSettings. Existing ViewModel ReminderPreferences API
  remains; native Android adapter converts via existing fromShared with exact nanos/quiet/zone.
  Actual settings UI calls that existing ViewModel wrapper; permission-declined disabling retained.
- THREE new Android cases cover actual typed focus/allowlisted storage and invalid command
  retention, selected-document forwarding and restore success/failure callbacks, active recording
  reset REVIEW guard plus exact reminder settings/milestone toggle. No arbitrary delays; success
  callback observation uses Main idle barrier. These await actual runtime verification.
- FINAL125 local metadata/debugAPK/testAPK/lint PASS7m35s/176tasks,
  scratch/kmp-settings-final.log. Format951(856Kotlin/95Java) PASS, final changed settings test
  individually pinned ktfmt PASS. Health25/storage648 JVM from124passed unchanged; no SQL/domain
  change requires repeat local runs. Exact schema19/source3 PASS. APK remains0.12.3-test1/code19/
  minAPI28/noINTERNET. Expected610Android/Native648 pending; do not claim runtime/Native acceptance.
  Commit125+handoff and push124+125once. DO NOT rerun already-applied scratch scripts.

### Checkpoint126 — ONE actual shared application composition

- SharedCoachApplicationServices composes the existing snapshot reader, ONE CoachOperationRunner,
  typed profiles/context and ALL existing feature coordinators. Required CoachRepositories borrow
  the original same-database interfaces; required CoachPlatformBindings provide actual health,
  document/reminder, current clock/IDs, per-action gym clock, date formatting and timezone effects.
  No fake defaults, new engine, background service, automatic IO or second data owner.
- Actual Android CoachApplicationServices now uses that common root, passing the SAME original
  FeatureStore/CoachingStore/programming repositories and Android SDK callbacks. Existing ViewModel
  and screen properties remain. Original BaselineCoordinator constructor/API/timezone adapter
  remains; an internal binding wraps common root's actual SharedBaselineCoordinator instead of
  making a second coordinator. Native app still needs REAL host/effects, not an empty scaffold.
- THREE new actual SQLite/root tests verify no database opening/reads during Main construction,
  real coherent first snapshot without invented training/health grants, serial cross-feature
  Strength-only weight/focus state BEFORE success callback, and failed-setting state retention /
  subsequent strength/weight operation. Tests use actual common repositories and original snapshot;
  test SDK/document boundaries throw on unexpected effects, never pretend unimplemented actions
  were performed. Final expected613Android, storage648 unchanged.
- FINAL126 local common metadata/debugAPK/testAPK/lint PASS7m14s/176tasks,
  scratch/kmp-application-root-final.log. Format953(858Kotlin/95Java), final changed root test
  individually pinned ktfmt, exact schema19/source3 PASS. Existing unchanged health25/storage648
  JVM were verified124; no repeated domain/storage implementation or test reduction. New root
  runtime/Native verification is still pending, expected613Android/648Native/framework.
- Preserve/commit common root +Android bindings +actual tests. DO NOT rerun old preparation scripts.
 12561e038d is safely remote and useful610/648 CI44106763790 must finish before126push. Current
  formatting44106763460 SUCCESS. No schema/backup/version/permission/publication changes; Native
  destructive UI remains disabled. Actual app/platform/navigation/SDK integration remains next.

### Current remote125 proof /126 exact recovery

-125Android134121100783 SUCCESS on61e038d: completed log has610unique matching START/END cases,
  all three new running-evidence/health/settings cases +existing running-plan/strength cases3each,
  zero failure/skips,700189domain checks. scratch/kmp125-android-job.log. Apple134121100041 is still
  usefully RUNNING Native tests as last observed15:06; no current648Native/framework pass claimed.
- Byte/digest-verified125 JVM490domain/25health/648storage ALL0fail/error/skip:
  artifact14339821998,185995bytes,SHA043075c2fd035858bbf8ad2f7c8a4cabf868719b2eb3267fc29570fdb2735028.
  APK14340880308 ZIP30770328/SHA8159b83b47faeb752d059c83d78aab4ec06cbf27667ba8bb96bbd0126c479e6b;
  APK58434886bytes/SHA2304f167ba6fd34b66a599e1b2940bac376778a35ccf6207864b774618285efb,
  0.12.3-test1/code19/min28/noINTERNET. Proof scratch/kmp125-android/verification.json.
-126e9a393b common root +actual Android use +3SQL/root tests is COMMITTED and backed up remotely
  on checkpoint/android-ios-kmp-compose-126 (verified initial head a172b905f7c2d3d8cb6e11b7caf237eb8edf2e0d;
  recovery-only docs may advance it). Original PR24 branch remains61e038d until useful125Apple ends.
  No replacement PR, force push, tag or workflow/policy bypass. We have NOT switched local branches.
  Local final metadata/debug+testAPK/lint7m14s/176tasks PASS; format953+final root test/schema/source
  PASS. Additional recovery/architecture documentation has NO app-source/fixture changes.
- At15:03 shell gh API returned401. Connected GitHub tools still work; normal Git ls-remote and
  push --dry-run both SUCCESS, without ref mutation. Managed runtime guidance/status inspected:
  environment ready, no credential values exposed/rewritten. No owner reconnect required for
  Git transport; use connector fetch_workflow_run_jobs/artifacts/logs instead of expired gh API.

### Checkpoint127 in progress — actual Apple SDK gateway and immutable visible-body import

- Existing native HealthKitRunningReader/HealthKitBodyReader are reused by new thin native
  AppleHealthGateway; real shared provider orchestration lives in SharedAppleHealthGateway.
  Athlete-selected workouts/body categories only are read. No SDK access sheet is launched by
  sync; request methods remain explicit. grantedPermissions returns EMPTY/UNKNOWN, never a fake
  Apple read grant. Native root must use explicit request+sync actions, not Android's Set-based
  permissionResult handler or fabricated permission tokens. Selected body reads are part of sync.
- ALL selected SDK evidence is staged before SQL. Common gateway checks exact body scope/provider/
  unknown-offset semantics; qualified workouts resolve against CURRENT repository data inside
  SAME transaction as body. Different repository owner is rejected. Visible-only results never
  imply deletion or authorised coverage; HealthSyncResult windowStart/windowEnd remain null.
  Unselected categories are omitted from result counts, not presented as zero training evidence.
- SqliteHealthKitBodyImport uses original typed heights/context tables. New cache entries retain
  source UUID/origin/nanoseconds; repeat/empty reads never delete/rewrite history or choose a
  protein basis. Compatible old IDs, first import dates and legitimate older offset metadata
  stay byte-identical. Missing origin stays unknown; conflicting KNOWN origins/value/time reject.
  Late failures roll back workouts/body/checkpoint, and unselected weight payloads are not loaded.
- Seven real JDBC/Apple contracts verify source/precision/manual/basis protection, repeats and
  empty-read byte identity, immutable conflicts/manual ID collision, duplicate/limit handling,
  outer workout+body rollback, caller rollback/unrelated malformed weight, older unknown origin.
  Three actual Android SQL/shared-gateway cases verify body-only privacy/no grants/coverage/basis,
  SDK staging failure +late-body rollback, and unavailable/unselected/mismatched/foreign-owner
  rejection. Expected616Android /655storage Native. No existing assertion removed or weakened.
- INITIAL six-case kernel JVM654ALL0fail/error/skip +common metadata/debug+testAPK/lint PASS
  8m36s/180tasks,scratch/kmp-apple-health-import-final.log. FINAL seven-case/selection-aware shared
  gateway JVM655ALL0fail/error/skip +metadata/debug+testAPK/lint PASS9m38s/180tasks,
  scratch/kmp-apple-health-gateway-final.log. Format960(865Kotlin/95Java), schema19/source3 PASS.
  Native SDK facade has NOT been compiled/tested locally (Linux); upcoming real Apple gate is
  mandatory. Actual iOS host/selection UI/entitlements remain unfinished, no false app acceptance.
-12561e038d finished BOTH jobs successfully: Native artifact14342256195,178284bytes,
  SHA7fa4662d229ffdce4dfa27317c1edae6dc24abf1031593d4f3ab061ecc7db501 independently verified
  449domain/27health/648storage ALL0fail/error/skip. scratch/kmp125-native/verification.json.
  Framework14342368146 linked, metadata62140355bytes/SHAe1157be705d1e5888a2dd3f937918333fc921cffdcdf6f4a94f6aef4c370478c.
  This is prior-source proof, not new127655/616 acceptance. No further wait/cancellation needed.

### Checkpoint128 — existing Home and primary-focus UI are shared

- SharedHomeScreen contains the actual original Today hierarchy and navigation, with required
  real platform date/unit/pace/route-encoding bindings and actual existing Strength Today content.
  Android Home uses its same formatters and route callbacks; no recording/service ownership moves.
  SharedStartingPath retains all four original choices; Android still saves focus/path and routes
  only after the original successful operation. No fake default callbacks were added to common UI.
- Five new UI cases cover original target/encoded route vs actual evidence, completed runs and
  independent recording at dark200%, Strength-only no-race Home, all first-run choices/no implicit
  action and dark200% choice accessibility. Registered alongside every retained focused test.
- FINAL common metadata/debugAPK/testAPK/lint PASS6m44s/176tasks,
  scratch/kmp-home-start-corrected-final.log. Formatter964(869Kotlin/95Java), exact schema19/source3
  PASS. Initial JVM-only date comparison compile error fixed by epoch-day sort with identical ISO
  ordering; no broadened expect ABI, changed training values or weakened assertion. Runtime/Native
  and authentic source-bound visual acceptance remain pending; expected621Android/655Native.

### Checkpoint129 — actual running-plan presentation and explicit athlete choice

- SharedTrainingPlanScreen contains original weekly hierarchy/date clamping, existing common
  calendar states, run/strength destinations, optional warm-up/cool-down/mobility slots, retained
  version/evidence details and original rebuild dialog. Android binds same UK date/units labels,
  real routine-history content and original RunningPlanCoordinator commands. AdjustmentCard also
  shared unchanged; no second adaptive engine, new prescription or automatic activation.
- Three new focused cases cover no-plan/no-automatic-generation, dark200% week navigation and
  original workout/routine identities, keep/rebuild confirmation without actual/history mutation,
  and explicit proposal choice. Existing integration and assertions remain; next expected624 cases.
- Common metadata/debugAPK/testAPK/lint PASS7m27s/176tasks,
  scratch/kmp129-plan-corrected.log; format966(871Kotlin/95Java) PASS. Initial1min compile failure
  used JVM facade name; changed to its existing common AdaptiveCoaching delegate, same rules.
  Runtime/Native/current-source visual acceptance still pending. No storage/domain/schema change.

### Checkpoint130 — existing milestone recognition orchestration

- Existing AchievementsViewModel operation/state moved to SharedAchievementsCoordinator, with
  required real clock, lifecycle scope, original coaching/activity repositories and same schema19
  SqliteTrainingMilestones. Android wrapper retains automatic initial refresh, original actions,
  idempotent recognition, cancellation/failure wording, notice/toggle behaviour and local dates.
  No new eligibility, achievement, route hydration, database, engine or historical rewrite.
- Two new real SQLite/common-operation tests check lazy construction/no Main SQL, summary-only
  reads, exact original actual/recognition preservation over repeat reads, optional celebrations
  and source failure/known-state retention/explicit retry. Existing three light/dark/200% screen
  tests and all previous116focused classes remain. Expected next626Android/655Native/framework.
- FINAL metadata/debugAPK/testAPK/lint PASS7m23s/176tasks, scratch/kmp130-achievements-final.log;
  formatter968(873Kotlin/95Java), exact schema19/source3 PASS. APK code19/version0.12.3-test1/min28/
  noINTERNET verified. Runtime/Native remain pending; do not reapply already-applied scratch scripts.129gate PASS7m27s;128gate PASS6m44s;127JVM655all0failure/error/skip.

### Checkpoints131–132 in progress — actual workout UI and demonstrated CI download failure

-131moves original workout summary/detail, exact effort/pace instructions, explicit move/skip,
  completed-activity links, manual actual form and source identities into common Compose. Required
  original coordinators and real OS date/time/input/recovery/routine bindings remain; Android
  wrappers invoke SAME commands. Existing actual metric/imperial/history integration assertions
  are retained, not duplicated/weakened. PlanScreens bindings now pinned-format scoped. Local
  metadata/APKs/lint PASS6m30s/176tasks at scratch/kmp131-workout-final.log; format970/source3PASS.
-132fixes actual130format-job HTTP429 before source check, using bounded server-aware retry under
  original60s budget. HTTP dates/numeric delays, exhaustion, permanent responses, budget/size/time
  rejection tested; pins/JAR/cache/proxy/CA/symlink/check-mode protections remain. All18offline
  formatter contracts PASS, scratch/kmp131-format-tests.log. CI fix still unpushed/unverified.
-130source cc180117 is remote, experiment44128266830 still useful on actual Android/Apple; expected
  626/655 not claimed passed. Formatting44128267170 FAILED (download429), no blind rerun. Do not
  cancel this run by pushing131early. Earlier125610/648 proof remains historical, not new source.

### Checkpoint133 — original manual programme authoring operations are shared

- SharedStrengthProgrammeCoordinator preserves original durable drafts/pagination/custom metadata,
  explicit preview/review/activation/stop, supersets and IDs, exclusion/archive handling, freshness/
  CAS and retained-state publishing. Constructor requires actual borrowed database/repositories,
  scope, retained-ID effects, clock/date/ID and recent actual reader; no IO during construction.
  Android ViewModel keeps original public API, SavedState key and lifecycle, real original stores.
- GymSlotInput moved unchanged; one common parser retains exact original values/errors/modality/
  effort/reference handling with required real ID supplier. Original Android3arg parser delegates
  with UUID, avoiding JVM file-facade collision and a second parser. Gym Now engine unchanged.
- Three new real SQL/coordinator cases verify no Main IO, callback/retained ordering, incomplete
  draft retention/reopen, stale review/fresh exclusion rejection and unchanged actuals, fractional
  RIR vs RPE and optional bodyweight-reference separation. All old builder/custom/gym tests remain.
- Metadata/debugAPK/testAPK/lint PASS6m45s/176tasks, scratch/kmp133-authoring-final.log;
  format973(878Kotlin/95Java) PASS. Expected629Android/655Native/framework unverified for new133.

### Checkpoint134 — original generated/revised strength application operations

- SharedStrengthGenerationCoordinator borrows original concrete/programming/coaching/execution
  repositories and real recent-actual reader, lifecycle/retained-ID/clock/date/identity callbacks.
  Actual Android ViewModel delegates unchanged public API and effects. No second generator,
  executor, state store or automatic activation. Fresh completed/unfinished protection, optional
  evidence progression, explicit swaps and immutable previous revisions remain unchanged.
- Three new real SQL tests cover standalone/no-race generation +retained ID before callback,
  draft-only future target editing/stale activation rejection/v1 preservation, and missing working
  evidence cannot invent or save progression. All original generation/progression/history tests
  and assertions remain. Expected632focused Android cases; storage655unchanged.
- FINAL common metadata/debugAPK/testAPK/lint PASS6m45s/176tasks, scratch/kmp134-generation-final.log.
  Formatter975(880Kotlin/95Java), schema19/source3 PASS;132formatter18offline contracts reconfirmed PASS.
  Previous133metadata/APKs/lint PASS6m45s/176tasks, format973; no new runtime/Native claims.

### Checkpoint135 — original recovery/hybrid review and explicit preview operations

- SharedTrainingRecoveryCoordinator borrows original typed programming/draft/gym/actual/activity
  repositories and required real health-cache/focus/lifecycle/clock/offset/ID callbacks. Android
  wrapper retains actual SDK/cache effects and4public methods. Original TrainingRecovery and
  RecoveryProgrammeProposal rules are reused, no readiness score/new engine/automatic activation.
- Two real SQL tests cover missing evidence/local-date rollover/no-write Keep/unsupported deload,
  genuine hybrid lower-conflict preview with preserved running and active strength programmes,
  and fresh interleaved programme changes rejecting stale review before another write. Existing
  light/dark/200% recovery UI and domain/backup assertions remain unchanged. Expected634Android.
- Metadata/debugAPK/testAPK/lint PASS6m14s/176tasks, scratch/kmp135-recovery-final.log;
  format977(882Kotlin/95Java) PASS. Runtime/Native for135 still PENDING; current134CI useful.

### Checkpoint136 — existing optional body/measurement guidance operations

- SharedBodyMeasurementsCoordinator preserves original manual source IDs/timestamps/units,
  explicit protein preference/basis, stale/missing evidence, paged heights and provider failure
  state. Host must provide actual typed repository, availability/factual permission/sync and
  clock/offset/ID/lifecycle effects. Apple permission identifiers can be unknown, never invented.
  Android wrapper keeps8public methods and original concrete Health Connect API/owned instance.
- Two real existing Android SQL/coordinator cases verify source/nanoseconds/offsets, height cannot
  create protein guidance, weight is chosen explicitly, stale evidence has no numeric range,
  independent height pagination and revoked-read failure preserves manual history/perm facts.
  Provider fixture explicitly not SDK/hardware acceptance. All old body/HC/protein assertions stay.
- First compile2m03s interface/concrete SDK mismatch fixed in required Android binding only.
  FINAL metadata/debugAPK/testAPK/lint PASS5m43s/176tasks, scratch/kmp136-body-corrected.log;
  format979(884Kotlin95Java), schema19/source3 PASS. Expected636Android/655Native/framework pending.

### Exact next safe actions and remaining full scope

1.135aeac08a and136source are locally verified. Normal-push combined135/136ONCE to ORIGINAL PR24
   if not already there; inspect actual head/CI first and preserve useful running Native work.
  13444136116110 BOTH jobs SUCCESS:632Android, JVM490/25/655, Native449/27/655/framework; format
  18contracts/975files SUCCESS. No need to wait/rerun it. New636Android/655Native/framework and
   current formatting require exact new-head verification. Fix actual failures without weakening
   assertions or blind reruns. Then remaining actual GymNow orchestration/active UI/platform
   inputs/assets/full Native services and actual iOS app host. No empty/throwaway host, duplicated
   domain/storage/root or already-ported feature operations, early destructive controls or merge.

2. Java domain conversion is COMPLETE (0Java sources). Preserve narrow JVM Kotlin SDK/IO adapters
   and actual shared UI. Do not restart domain ports or mistake framework linkage for an iOS app.
3. SharedStorage typed repositories/codecs/backup/retention kernels are COMPLETE over the SAME SQLite19 model; finish actual app binding, not another kernel restart.
   Existing native/schema/settings/exclusions/profile/focus/height/milestone/shoe/baseline/retention reader are
   implemented as above; preserve them. Running/actual/frozen workout/execution codecs are now shared
   as recorded in checkpoints55–59; preserve them. Programme, both association and concrete-preview codecs plus typed legacy gym history now exist
   as recorded in checkpoints60–63. Tools/timer storage and authoring decision/archive graph guards are implemented in checkpoint64.
   Normalized authored/draft/concrete/custom/decision readers are implemented in checkpoint65.
   Draft/custom/concrete-save commands and saved-workout library are implemented in checkpoint66;
   transactional revision-review snapshots are implemented in checkpoint67.
   Reviewed authored/concrete activation and normalized execution readers are implemented in checkpoint68.
   Explicit associations and archive graph guards are implemented in checkpoint69.
   Source-validated execution/actual commands are implemented in checkpoint71, with a REQUIRED
   compatible legacy fingerprint provider. Real provider73 is VERIFIED on Android and Native,
   including byte/number goldens and all21execution cases with system crypto; host wiring remains.
   No fixture lookup remains in these tests. Retention metadata snapshots/codec74 and required-
   validator root1–13 envelope75 exist; COMPLETE parent data/graph normalization is implemented82;
   raw1–3/package1 inspection/export/transactional restore and protected internal pruning now exist (103–109); remaining work is full host/operation/UI binding and final acceptance, not restarting kernels. Coaching76/core77/development78 /79 support/0.8/choices /80 body/achievements
   archive components now exist; preserve their exact payloads/source indexes and privacy rules.
   The COMPLETE composed parent validator is UserBackupDataJson82. Reuse71/73/74/75/76/77/78. Activity/result repositories are implemented in checkpoint53;
   preserve them. scripts/shared-schema.py --check
   proves exact Android schema1..19 statements/BOUNDARIES/current VERSION. Fresh retention identity
   uses real clock/UUID; upgrades remain protected/unreviewed. No native pruning until complete
   consent/preview/transaction/restore safeguards exist. Existing Android stores remain authoritative; checkpoint55 adds only a semantic reminder comparator for cross-codec compatibility.
4. State/runner, operational coaching/context, snapshot/source bindings and application root are
   COMMON and USED by Android (108–126); do not repeat them. Milestone orchestration is common130.
   Remaining feature-owned Gym Now/programme-generation/authoring/body/recovery orchestration,
   active UI/navigation and native app/SDK effects still need completion, including full permissions/
   HealthKit, native recording/BLE/notifications and real iOS app host/entitlements/usage strings,
   preserving Android Health Connect and all existing features. Visible-only health snapshots
   cannot infer deletion. Use actual shared screens and domain, not a separate product/prototype.
5. If adding a storage/platform module, include its runtime sources/build inputs in automated
   formatting and screenshot-source.py fingerprint coverage, and test its actual native storage
   on working macOS CI. Preserve foreign keys, one DB/transaction boundary, rollback/corrupt/future
   rejection, actual/history/source identities and current/older backup compatibility.
6. Final full Android28/35/36/iOS APP/regression/canonical screenshot/accessibility gates only
   when implementation ready. No premature version bump, merge or public publication.
   Physical HealthKit/Health Connect/GPS/BLE/audio/locked-screen/OEM/TalkBack remains unperformed.

### Reproducible local tooling / Apple verification

- Linux has JDK `/workspace/jdk17`, SDK `/workspace/android-sdk`, Gradle cache
  `/workspace/gradle-cache`, Android user home `/workspace/android-user`, Konan `/workspace/konan`.
  Use `JAVA_HOME`, `ANDROID_HOME`, `GRADLE_USER_HOME`, `ANDROID_USER_HOME`, `KONAN_DATA_DIR` with
  those paths; `./gradlew --no-daemon --project-cache-dir /workspace/scratch/kmp-gradle-project-cache
  -Dorg.gradle.workers.max=2 -Pkotlin.compiler.execution.strategy=in-process ...`.
- Local Gradle cache properties use inherited proxy/system CA trust; do not commit/expose them.
  Gradle needs socket/network access even for its daemon. No local Xcode/KVM, but actual GitHub
  **macOS 26 Intel / Xcode 26.6 works**. Do NOT claim unavailable macOS infrastructure: workflow
  `multiplatform.yml` already compiled/tested real Native modules and shared UI frameworks.
- `scripts/test-domain.sh` uses Gradle `:domain:prepareOfflineTests` to retain the original Java
  main-suite/audit harness with shared Kotlin dependencies, not a weakened substitute.
  Shared sources/build inputs are included in formatting and screenshot source fingerprints.

## End-of-session handoff

Summarise what changed, file paths, branch/PR/commit, tests actually run and their results, remaining work, and any real-device checks still needed. If interrupted by a usage limit, leave a concise accurate checkpoint above so the one-line resume prompt works without guessing.
