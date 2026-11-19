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

### Latest recovery pointer — checkpoint 259 iOS publication verified; analytics dashboards DRAFT (Claude)

- PR #23 squash-merged as `146cde4` (tree == verified head 367200b). main run 44439687824
  attempt 2 (re-ran formatting after a transient Maven 404 and apple-shared after a lost Mac
  runner) published, from ONE commit: 0.13.0-test1 APK (SHA-256 02d3b128…), universal unsigned
  iOS Simulator ZIP (37aeff0d…, x86_64+arm64, iPhoneSimulator), unsigned iPhone ZIP (637b54d4…,
  arm64, iPhoneOS), latest-* aliases, .sha256 files and build-info. All downloaded and checked:
  checksums, commit 146cde4, Info.plist 0.13.0/20, no code signature, all licence notices;
  APK 0.13.0-test1/20/min 28/no INTERNET; 0.12.2/0.12.3 assets preserved.
- Owner-authorised analytics work on `feature/analytics-dashboards` (DRAFT; must NOT merge
  without explicit owner approval; owner reviews visuals). Reference designs: owner's Progress,
  overview and Strength images. Implemented:
  - `sharedDomain/.../presentation/DashboardAnalytics.kt` reusing RecordedWeekAggregation,
    StrengthVolume, StrengthRecords and StrengthRecordPolicy (Epley); plan completion uses the
    calendar's plannedWorkoutId link; nulls for missing evidence; 6 commonTest contracts.
  - `sharedUi/.../SharedDashboards.kt`: SharedProgressDashboard (week hero, KPI tiles, existing
    SharedWeeklyRunningChart, recent runs, all drill-downs) and SharedStrengthDashboard (Gym now,
    KPI tiles, estimated-1RM trend, muscle-group donut, recent workouts, all drill-downs).
  - Android Progress/Strength tabs and iOS PROGRESS + new STRENGTH_HOME page use them; five-tab
    navigation unchanged. Tests scroll to links on the longer screens; 6 new required captures
    (457 total); iOS XCUITests attach dashboard screenshots exported as `ios-review-screenshots`.
  - Today is unchanged (asked before touching it). Bouldering excluded.
  - Draft run 44456340230 on 09bab8b: iOS framework/app/startup OK; XCUITest navigation (Progress
    dashboard) passed; Gym Now test failed waiting for an off-screen label (iOS exposes only
    on-screen Compose nodes) -> now waits for the top "Gym now" control. Android focused gate
    642/644: WeeklyUxTest tapped "PBs & progression"/"Body weight" without scrolling on the longer
    dashboards -> scroll added there, in Release05UiTest and three screenshot-test taps.
  - Checkpoint 260 (owner's "Final Analytics Dashboard Requirements"): PR #25 returned to DRAFT.
    Full gate on f2adea7: Android all green (bot screenshot commit 3a1cbc1); iOS 2/4 XCUITests
    failed because taps landed during a Compose scroll fling (tap only stops the fling) -> the
    test tap helper now waits for the element frame to settle. Implemented: reporting periods
    (4 wk/12 wk/6 mo=26 wk/1 yr=52 wk, whole weeks ending today); like-for-like comparisons
    (week-to-date vs same weekdays last week; period vs same-length earlier window, only when
    history covers it); moving pace kept labelled "moving"; plan completion by calendar rules
    (link, latest skip/move decision); gym programme completion by calendar gym-link rules;
    e1RM exercise selector (eligible exercises only), tappable/accessible points with source
    workout + "Heaviest recorded set" (actual, separate from estimate); muscle workload with
    primary shares, separate secondary involvement (once per set/group) and visible Unclassified
    segment; recent runs show source (ActivitySources.friendly) and open run details; recent
    workouts open the recorded session (Android strength/source/{id}, iOS STRENGTH_RECORD with
    return page); compact grouped "More detail"; 2-up KPI grid (1-up at 200% text, 4-up >=600dp);
    explanations behind toggles. New records = distinct (record type, workout) improvements;
    tile text says "Recorded bests improved · each record counted once per workout" (owner asked
    that the wording not imply a count of workouts).
    Draft run on cbefb4c: Android focused suite incl. SharedDashboardScreenTest GREEN; apple-shared
    failed in the ios_scope step because `gh pr diff` refuses PRs with >300 files (the canonical
    screenshot bot commit makes PR #25 475 files) -> now uses the paginated pulls/files API and
    runs the UI suite if the list cannot be read.
    12 domain contracts, SharedDashboardScreenTest (in draft focused classes), empty-state
    captures progress-/strength-dashboard-empty (459 required).

### Latest recovery pointer — checkpoint 258 PR #24 merged; PR #23 staged CI + unsigned iOS releases (Claude)

- PR #24 squash-merged as `f7be1ab` (tree == verified head 529d310 / source 039044a); main run
  44427299471 then verifies and publishes the 0.13.0-test1 APK.
- PR #23 (`ci/staged-pr-verification`, owner-authorised to merge after checks): main merged in
  (no force-push). Draft/ready/main staging per owner table; iOS UI suite on drafts only for native
  iOS paths or the `ios-ui-tests` label. Owner then asked to publish unsigned iOS builds:
  - `android.yml` `apple` job calls `multiplatform.yml` (now `workflow_call`) on push, and
    `publish-test-release` needs [build, guidance-completeness, formatting, apple]: one run, one
    commit for APK + `-ios-simulator.zip` (universal arm64+x86_64, built in apple-device) +
    `-iphoneos-unsigned.zip`, `latest-*` aliases, `.sha256` files and `-build-info.txt`.
  - `development-assets.py --prepare-ios/--verify-ios` refuse wrong commit, bundle version,
    build number or checksum; versioned assets upload before `latest-*` aliases; tag releases
    stay Android-only. Release notes, README (stable links + unsigned labels) and TEST_BUILD.md.
  - Skia/Skiko licence inventory COMPLETE: 19 static libs in Maven-verified
    skiko-iosarm64-0.9.4.2.klib; exact pinned texts (Skia m132 a00c390e98 DEPS) in
    `iosApp/licences/ios-native-NOTICES.txt`, bundled, listed in-app and asserted in CI.
- After merge: verify main run (Android + Apple jobs + publisher), the Releases page, README links,
  download every asset and check SHA-256, commit and versions. Then start the analytics DRAFT PR
  (owner must approve its merge).

### Latest recovery pointer — checkpoint 257 merge authorisation and backup-text regression (Claude)

- Owner explicitly authorised merging PR #24 then PR #23 once mandatory checks pass, then a
  new DRAFT analytics PR that must NOT merge without owner approval. Order chosen: PR #24
  first (verified; PR #23 predates its workflow and must be rebuilt on the merged main,
  and must also run the Apple workflow for all PRs and main, which today only targets
  the experiment branch).
- Pre-merge visual review: pixel-diffed all 451 canonical captures against main. 389 are
  effectively identical; date-relative fixture changes explain the rest except ONE genuine
  migration regression: `AthleteLanguage.BACKUP` lost its last concatenated Java lines
  ("copied; reading them again depends on the provider and your permissions. Keep your
  backup somewhere you trust."). A scan of 1,342 long Java string literals found no other
  truncation. Restored text; `PresentationProjectionContractTest` now asserts the full ending
  (fails on the truncated text, 14/14 pass fixed; local JDK 17 sharedDomain:jvmTest).
- Session environment `Race-day-dreams` allows dl.google.com for local Gradle; install
  `openjdk-17-jdk-headless` and pass `-Porg.gradle.java.installations.paths=<jdk17>`.

### Latest recovery pointer — checkpoint 256 full software gate GREEN on 6186f59 (Claude)

- Exact source `6186f59` (0.13.0-test1 / code 20) passed EVERY job:
  - android.yml [44407108219](https://github.com/mmwiza1/Race-day-dreams/actions/runs/44407108219):
    guidance-completeness, formatting, build API 28/35/36 (full unfiltered instrumentation,
    lint, domain/schema/release/showcase/geometry checks; 451 captures uploaded per API,
    API 28 APK 58,943,010 bytes), canonical-screenshots (verified 451 and committed them as
    bot commit `2c8561a`, `docs/screenshots/source-commit.txt` = 6186f59).
  - multiplatform.yml [44407108265](https://github.com/mmwiza1/Race-day-dreams/actions/runs/44407108265):
    android-shared; apple-shared (Native tests, framework link incl. new AppleCoachSpeech,
    UI/GPX decoder, simulator build, startup at schema 19, XCTest `Executed 4 tests, with 0
    failures`, `** TEST EXECUTE SUCCEEDED **`, result 14478168044); apple-device unsigned
    iosArm64 build SUCCESS (first ever run).
- Heads after 6186f59 (`2c8561a` screenshots, this checkpoint `[skip ci]`) change only
  `docs/` and recovery/verification text; `git diff 6186f59 HEAD` contains no app, test,
  build or workflow change. Owner may still require a fresh run on the exact merge head.
- PR #24 is READY, NOT merged, NOT published. Merge needs explicit owner approval; after merge,
  verify main CI and the 0.13.0-test1 development APK/version/commit/checksum per AGENTS.md.
- Physical device acceptance (Android + iPhone, TEST_BUILD.md) remains UNPERFORMED. No
  signed iOS distribution exists.

### Latest recovery pointer — checkpoint 255 audit fixes and 0.13.0-test1 (Claude)

- Owner approved implementing the independent audit's recommendations (18 November).
  Changes, batched into ONE push after the c29f370 full gate finished:
  - A1 `AppleCoachSpeech`: AVAudioSession Playback/VoicePrompt with DuckOthers +
    InterruptSpokenAudioAndMixWithOthers only while speaking; released (notify others) on the
    next 1 s recording tick via `settle()` or on stop; cues skipped during a CallKit call with
    vibration for haptic cues. `Info.plist` adds `audio` background mode.
  - A5 `Info.plist` adds `bluetooth-central` background mode for locked-screen HR straps.
  - A2 iOS bundles and lists every Android notice (third_party.md, Kotlin/coroutines/
    serialization, route MIT/CC0/NOTICE files, transitive notices) plus the upstream Skia
    BSD licence (`iosApp/licences/skia-LICENSE.txt`); multiplatform.yml asserts each file is
    in the built .app and that the bundled inventory equals THIRD_PARTY.md. THIRD_PARTY.md
    and its asset copy gain an identical Skiko/Skia row (Skia sub-components still need a
    full inventory before public iOS distribution).
  - A3 PRIVACY.md: iPhone section; Android-only background statement qualified.
  - A4 versionName 0.13.0-test1 / versionCode 20 (iOS 0.13.0 / 20), android.yml artifact
    names, `docs/RELEASE_0_13_0.md` (release-note tests 10/10 pass).
  - A6/A7 documented as known limitations (HealthKit scope; no iOS in-app data deletion).
  - A8 README iOS statement; A9 checkpoints 239 and earlier moved verbatim to
    `docs/CONTINUE_CODEX_ARCHIVE.md`. A10/A11 (CI cost, Node 20 actions) left to PR #23.
- Full gate c29f370 (android.yml 44398563175): `guidance-completeness` job 135135023977 failed
  in 14 s resolving AGP dependencies ("Could not find" jsr305, gson, asm...). Root cause: the
  migration made `scripts/test-domain.sh` use Gradle, but this ready-only job never ran on
  the draft branch and lacked the SDK + setup-gradle cache steps the build jobs have. Fixed
  by adding the same setup steps; no test or audit content changed.
- Gate on 42ac840: android.yml 44400715723 guidance-completeness, formatting and build
  API 35/36 SUCCESS, API 28 tests passed (artifact upload in progress when superseded).
  multiplatform.yml 44400715164: android-shared SUCCESS; apple-shared Native tests passed but
  framework link and apple-device failed compiling `AppleCoachSpeech.kt`: "Unresolved
  reference 'setActive'" (lines 64, 71). In Kotlin/Native `AVAudioSession.setActive` is a
  category extension and needs `import platform.AVFAudio.setActive`; restored. All other new
  AVAudioSession/CallKit/AudioToolbox references compiled.
- Local: pinned formatting applied (only AppleCoachSpeech comment rewrapped), release-note,
  showcase and screenshot-source tests pass. iOS Kotlin/Native compilation cannot run on
  Linux: the CallKit/AudioToolbox/AVAudioSession calls are verified only by the Apple CI job.
- Physical iPhone checks required for A1/A5: music ducking, podcast pause, call suppression,
  locked-screen cues, BLE strap while locked.

### Latest recovery pointer — checkpoint 254 owner-approved ready stage (Claude)

- Owner explicitly approved marking PR #24 ready for review on 18 November 2026 to run the
  full release gates; merge and publication still require separate explicit owner approval.
- Marking ready on `5e6398f` started NO workflow: GitHub skips `pull_request` workflows,
  including `ready_for_review`, when the head commit message contains `[skip ci]`. Never
  leave a `[skip ci]` commit as head before a ready transition or final gate. This commit
  (no skip marker) is the exact head for the full API 28/35/36, canonical screenshot,
  release/licence/format and unsigned iosArm64 gates. Record their run IDs and results here.
- A read-only independent audit runs in parallel and makes no source changes.

### Latest recovery pointer — checkpoint 253 green focused gate including Gym Now (Claude)

- Exact `13b5e75` run [44385737143](https://github.com/mmwiza1/Race-day-dreams/actions/runs/44385737143)
  completed SUCCESS on both focused jobs. Android job 135089668569: shared JVM/domain,
  schema/backup, APK/lint, then API 35 emulator `644/644 completed (0 skipped) (0 failed)`;
  APK artifact 14465549126. Apple job 135089669145: Native tests, framework link, UI/GPX
  decoder tests, simulator app build, standalone startup with schema 19, XCTest build, then
  `Executed 4 tests, with 0 failures` and `** TEST EXECUTE SUCCEEDED **` (566 s). The new
  `testGymNowExposesSharedRestAndWarmupControls` passed (125 s): Strength → Gym Now → Bench
  Press suggestion → Timer settings → Optional warm-up → discard. Result bundle 14470552127.
  Earlier 9beae45 Android failure was an SDK system-image ZIP read error (infrastructure;
  no tests ran) and did not recur.
- Read-only data-safety review (no code change needed): iOS composes ONE
  `LocalBackupOperations` per session for backup, GPX import, restore and retention cleanup.
  Restore rejects ACTIVE/PAUSED/RECOVERABLE runs and ACTIVE/PAUSED/RECOVERY_REQUIRED gym
  executions inside its transaction; complete restore reuses that check; GPX import rejects
  an unfinished recording; cleanup rechecks consent, epoch, clock, open runs and replacement
  state per batch. AppleAppHost runs restore on the IO dispatcher via
  `serialiseDraftWrite`, then closes and reopens the session. Existing JDBC + Apple SQLite
  contracts cover overlap, rollback, stale approval and unfinished-state rejection.
  Old "destructive controls disabled" wording in ARCHITECTURE.md corrected.
- Known iOS parity gap for audit (not a blocker, owner decision): no in-app
  "Delete local app data"; `reset()` is not exposed on iOS.
- Next: owner approval to mark PR #24 ready, which triggers the full Android API 28/35/36
  matrix, canonical screenshots and the unsigned iosArm64 device build on the exact head.
  Do NOT mark ready, merge or publish without explicit owner approval. Documentation-only
  checkpoint commits may use `[skip ci]` mid-development only (see checkpoint 254).

### Latest recovery pointer — checkpoint 252 native gym selector and startup budget

- On `9beae45`, [44374630360](https://github.com/mmwiza1/Race-day-dreams/actions/runs/44374630360)
  successfully built the Apple simulator app and the separate XCTest target.
  Apple Health choice, saved run/GPX and shared navigation passed. New Gym
  Now test stopped before rest assertions: XCTest queried TextField, while
  result artifact 14465106389 shows the Compose search is a TextView and
  the Bench Press suggestion is an enabled button. Select that button;
  keep rest, warm-up and discard assertions.
- The standalone step reached SQLite readiness and wrote its screenshot
  but hit the wall deadline while finalising. Its local Swift verifier
  compilation had taken roughly four minutes. Compile the verifier in
  its own step and use a bounded 19-minute startup / 72-minute job.
- Push together, verify exact new native results, and check Android's
  new accelerated job. Full gates remain pending. PR #24 stays draft.

### Latest recovery pointer — checkpoint 251 separate XCTest build and launch

- Exact `1c6812e` run [44363683776](https://github.com/mmwiza1/Race-day-dreams/actions/runs/44363683776)
  passed Apple framework, simulator build and standalone startup. XCTest
  then timed out launching the app and hit the 16-minute deadline with
  an incomplete result bundle. The Gym Now test did not run.
- The workflow now explicitly terminates the standalone instance and
  separates build-for-testing from test-without-building, retaining the
  same simulator, result evidence and assertions. YAML, shell syntax
  and diff checks passed. Verify the new head on Apple. The Android BOM
  failure gets a fresh job with this push instead of a separate retry.
- On `9beae45`, [44374630360](https://github.com/mmwiza1/Race-day-dreams/actions/runs/44374630360)
  has passed the Android Gradle configuration and shared JVM stage. Pinned
  formatting passed in workflow 44374630412. A local stub check confirmed
  both XCTest commands use the same simulator and derived-data path, and
  only execution writes the result bundle. Await real native execution.
- Android job 135050352154 passed shared tests, schema and APK/lint
  build. The emulator binary downloaded successfully, but SDK Manager
  could not read the API 35 system-image ZIP from a SeekableByteChannel.
  No emulator boot or instrumentation ran. Preserve Apple job
  135050352500. After it finishes, retry only Android if no source
  fix is needed. If the ZIP failure recurs, check SDK package delivery
  and runner disk logs before changing the emulator or test settings.
- Independently downloaded Google's API 35 default x86_64 revision 2
  image. Its 782,404,023 bytes, official SHA-1 and full ZIP validation
  matched. The image booted locally in software mode and accepted both
  APKs, but the app hit a process-start ANR before any targeted tests
  ran. The guest also logged Bluetooth stack crashes. No local API 35
  pass. Logs: `/tmp/rdd251-api35-{emulator,tests,crash,main}.log`.
  Use the accelerated CI runner for the Android-only retry.
- Keep PR #24 draft. Full software gates and unsigned device build
  remain pending.

### Latest recovery pointer — checkpoint 250 exact-head local verification

- Android job 135011582342 on `1c6812e` stopped during Gradle configuration
  because it could not resolve Kotlin Gradle plugins BOM 2.1.21. No CI
  instrumentation ran. Maven Central returned HTTP 200 for the same POM
  from the workspace. After Apple job 135011581845 completes, retry only
  Android once unless an Apple fix needs a new push.
- Exact-head offline Gradle passed 700,189 retained checks across 48
  suites, all shared JVM tasks, storage metadata compilation, both
  Android APK builds and lint. All 75 schema tests, 19 migration-boundary
  comparisons and 3 screenshot-source tests passed. Logs:
  `/tmp/rdd249-local.log` and `/tmp/rdd249-schema.log`.
- Preserve the [active run 44363683776](https://github.com/mmwiza1/Race-day-dreams/actions/runs/44363683776).
  The new Gym Now XCTest and final full matrix/device build remain
  pending. Keep PR #24 draft.

### Latest recovery pointer — checkpoint 249 green focused cross-platform CI

- [Run 44352975227](https://github.com/mmwiza1/Race-day-dreams/actions/runs/44352975227)
  on `28d4e8f` completed green. Android passed 644 focused API 35 tests.
  iOS passed Native/GPX, framework, build, startup, and all 3 XCUITests.
  XCTest explicitly reported TEST SUCCEEDED, 0 failures, 387.550 seconds.
  Startup screenshot 14454640518, simulator package 14454595540.
- Pushed the shared Gym Now rest/warm-up changes and native smoke test as
  `1c6812e`. [Exact-head run 44363683776](https://github.com/mmwiza1/Race-day-dreams/actions/runs/44363683776)
  is active. Verify the new exact head, including the additional XCTest.
  Full Android API 28/35/36 and unsigned iPhone build gates remain pending.
  Keep PR #24 draft until ready for the full release stage.

### Latest recovery pointer — checkpoint 248 native gym preparation smoke test

- Added an iOS XCTest that opens Gym Now from Strength, adds Bench Press,
  checks the shared timer settings and warm-up disclosure, then
  deliberately discards the disposable unfinished workout. The Strength
  selector is restricted to buttons so the onboarding heading cannot
  intercept the tap. The prior iOS Progress-tab log confirms the shared
  bar exposes its tabs as buttons. Source
  labels and diff checks passed. Native compilation and execution
  remain pending.
- Preserve exact-head [44352975227](https://github.com/mmwiza1/Race-day-dreams/actions/runs/44352975227)
  for `28d4e8f`. The Android job passed all 644 focused API 35 tests.
  Apple passed shared Native tests and is linking the framework.
  After Apple reports, batch this test with the local
  rest-control and shared-preparation commits and fix any demonstrated
  failure before the full platform gates. Keep PR #24 draft.

### Latest recovery pointer — checkpoint 247 shared gym preparation

- iOS Gym Now now uses the same Compose rest settings and warm-up
  guidance as Android. It includes default duration, suggestion toggle,
  interrupted-timer recovery, timer controls and warm-up review.
  iOS still owns its notification permission and suppresses the separate
  vibration preference, which its notification adapter cannot control.
- Both Android APKs compiled locally. Both targeted API 28 gym preparation
  tests passed, including dark mode at 200% text. Native UI verification remains
  pending. Keep this checkpoint and `ba1dc38` local until the Apple and
  Android jobs on [44352975227](https://github.com/mmwiza1/Race-day-dreams/actions/runs/44352975227)
  have reported, then verify the batched head. PR #24 stays draft.

### Latest recovery pointer — checkpoint 246 iOS rest controls

- iOS Gym Now now exposes the shared coordinator's subtract-30-seconds
  and reset actions in the existing active/paused rest panel. It uses
  the same persistence and notification rescheduling path as the other
  timer controls. Paused timers show the remaining time. Targeted JVM
  timer tests and pinned formatting passed. Native UI verification remains pending.
- Keep this iOS-only change local while [44352975227](https://github.com/mmwiza1/Race-day-dreams/actions/runs/44352975227)
  verifies `28d4e8f`. Then inspect its exact Apple and Android results
  and batch any necessary follow-up. PR #24 stays draft until the
  focused implementation can move into all final verification gates.

### Latest recovery pointer — checkpoint 245 completed iOS tests, timed-out CI

- [Run 44342650745](https://github.com/mmwiza1/Race-day-dreams/actions/runs/44342650745)
  on `9f29d20` passed 642 focused Android API 35 tests, Apple Native/GPX,
  framework, simulator build and startup. Startup screenshot artifact
  14447897062 was visually reviewed and checksum verified. Synthetic
  source `3a48491` has the same tree as `9f29d20`.
- Apple log records all three XCUITests passing (Health choice, saved
  run with Files cancel/save, shared navigation), zero failures, 393.7 s
  executed. The runner then hit the step's 12-minute wall deadline
  during result finalisation, before completing the job. Expanded the
  step to 16 minutes and job to 65. Test assertions are unchanged.
- Pushed `28d4e8f` with the timeout adjustment, dated Gym Now, calendar
  and return navigation. [Exact-head workflow 44352975227](https://github.com/mmwiza1/Race-day-dreams/actions/runs/44352975227)
  is active. Verify native workflow completion and the dated iOS path. Full platform gates remain pending.
  Keep PR #24 draft while scope or focused checks remain pending.

### Latest recovery pointer — checkpoint 244 calendar to dated Gym Now

- iOS calendar strength detail now offers a logging action when a
  programme has a session on that date. It opens the same date-filtered
  Gym Now picker as the shared Today action. No execution begins until
  the athlete selects a session. Back from dated Gym Now returns to the
  originating Today or the same dated Calendar detail. Existing calendar
  review remains accessible. Pinned formatting of both affected files and diff checks
  passed locally. Native execution of this route remains pending.
- [Run 44342650745](https://github.com/mmwiza1/Race-day-dreams/actions/runs/44342650745)
  on `9f29d20` passed all 642 targeted Android API 35 tests. Its Apple
  job passed the shared Native tests and is linking the framework.
  Preserve this run. Keep the dated strength commits local until the
  native compile and Files results can be inspected. Then batch any
  demonstrated fix with them and verify the resulting head.
- PR #24 remains draft. Native export cancel/save, shared iOS navigation,
  the new dated logging path and full release gates remain pending.

### Latest recovery pointer — checkpoint 243 dated strength logging

- The iOS Today action “Log today's strength workout” used to open
  generic Gym Now and ignore the date. It now opens the shared planned
  workout picker for the specific date. Starting remains an explicit
  action, and another day's prescription cannot be selected from that
  view. A normal Gym Now entry still opens its overview.
- Both Android APKs built locally and formatting passed. All eight
  AuthoredGymNowScreenTest cases passed on API 28. The two new cases cover
  exact-date execution with empty actuals and the no-session case.
  iOS host execution remains pending. Keep this checkpoint local while
  [run 44342650745](https://github.com/mmwiza1/Race-day-dreams/actions/runs/44342650745)
  verifies the UIKit import, Files and shared navigation.
- Next, inspect that exact Apple result, batch any demonstrated fix
  with this dated-entry work, then verify the resulting head. PR #24
  stays draft while scope or release checks remain pending.

### Latest recovery pointer — checkpoint 242 UIKit category import

- Apple job 134923933698 on `946c7ca` passed shared Native tests but
  failed linking the framework at the export delegate registration:
  Kotlin could not resolve `presentationController`. Added the explicit
  Kotlin/Native UIKit category-extension import. This needs Apple
  compilation, followed by the unchanged Files cancellation and save
  assertions. There was no XCTest result for this head.
- The independent Android API 35 job 134923932908 finished green.
  Pushed `9f29d20` with the import fix, shared navigation and release
  gates. Exact-head [run 44342650745](https://github.com/mmwiza1/Race-day-dreams/actions/runs/44342650745)
  is in progress. Verify native compilation, export and navigation.
  The iPhone device job belongs to the full release stage. Keep the
  PR draft for focused work.

### Latest recovery pointer — checkpoint 241 iPhone device build gate

- Added an unsigned arm64 iPhone build job for the non-draft migration
  PR. It links the iosArm64 framework, compiles the app for iphoneos,
  checks the binary's architecture and platform, and uploads an archive
  with its source SHA and SHA-256 checksum. The normal draft workflow
  keeps its focused simulator and Android jobs. Marking the migration
  PR ready now also starts the established full Android matrix for that
  same commit. Manual dispatch can
  opt in once the workflow is available on the default branch.
- All 15 Android gate script tests, YAML and Bash syntax checks passed. A stubbed run checked the device
  target arguments, artifact naming, source stamp and checksum. Actual
  Xcode execution remains pending. Physical install and HealthKit
  verification still require the signing and device steps in TEST_BUILD.md.
- Keep this commit and `b2f78d7` local while exact Files run
  [44339045211](https://github.com/mmwiza1/Race-day-dreams/actions/runs/44339045211)
  is active. Then batch with any Files fix. Do not mark PR #24 ready
  before the retained implementation is ready for full-stage verification.

### Latest recovery pointer — checkpoint 240 shared navigation parity

- iOS now uses the same Compose bottom navigation, icon paths,
  destination ordering and large-text semantics as Android. The native
  host still owns routing. It keeps Record and Settings as top actions
  and keeps bottom tabs reachable from iOS detail pages. Progress links
  to existing trends, recorded results, strength analytics, achievements
  and body measurements.
- Added a native XCTest that opens Progress and recorded results, returns
  via the Progress tab, and checks the Record and Settings actions.
  Local Android app and instrumentation APK assembly passed. Pinned
  formatting passed for 966 Kotlin and 95 Java files. All 5 shared
  navigation instrumentation tests passed on local API 28, including
  the original icon comparison and large-text checks. Native execution
  and iOS visual/accessibility verification are pending.
- Keep this batch local until the exact `946c7ca` Files verification
  in [44339045211](https://github.com/mmwiza1/Race-day-dreams/actions/runs/44339045211)
  reports. If Files still fails, inspect actual logs/video before pushing
  a coherent batch. Then verify the new navigation XCTest and capture
  the revised iOS home screen. Full platform gates remain pending.

### Archived checkpoints

Checkpoints 239 and earlier (including the original domain, storage, UI and iOS host
history) were moved verbatim to [docs/CONTINUE_CODEX_ARCHIVE.md](docs/CONTINUE_CODEX_ARCHIVE.md).
Consult it only when an older decision or SHA is needed; it is not an active task list.

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
