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

### Seventeenth checkpoint — shared weekly strength analysis and production screen

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
  Exact new Android/Apple/format verification pending. Do not borrow94aa212 pass.
- Next exact action: inspect this checkpoint's exact PR24 CI, fix any actual failure; then continue
  remaining domain/execution/coaching engines and shared UI/state/orchestration, typed native
  SQLite/backup/retention and services, full HealthKit integration, real iOS app host. Preserve
  all existing Android Health Connect/GPS/history. No release/version bump/merge before full parity.

### Exact next safe actions and remaining full scope

1. Inspect branch/status/log and PR24. Preserve all valid work. The current weekly strength
   analysis/shared-screen checkpoint follows94aa212. Its local tests/build/formatting passed;
   inspect exact new PR-head CI.94aa212 passed Android174, Apple tests/framework and formatting.
2. Inspect exact new Android/Apple CI, correcting real failures. Continue remaining shared
   application work: portable activity/strength models/engines, actual shared screens and
   orchestration, platform storage/services and real iOS app host. Preserve known/unknown
   HealthKit read visibility; never use a visible-only snapshot for window replacement.
3. Continue remaining Java-domain conversion and actual shared screens/navigation/state/data
   sources, cross-platform typed SQLite/backup/retention, permissions, full HealthKit integration,
   recording/BLE/notifications and an actual iOS app host with entitlements/usage strings.
   Full Android/iOS parity and actual app builds are still incomplete. Framework-only success
   must never be described as an iOS application or grounds for merging.
4. Final required platform/regression/screenshot gates only once implementation is ready.
   No premature version increment, publication or merge. Physical HealthKit, Health Connect,
   GPS/BLE/audio/locked-screen/OEM/TalkBack acceptance remains unperformed.

### Reproducible local tooling / Apple verification

- Linux has JDK `/workspace/jdk17`, SDK `/workspace/android-sdk`, Gradle cache
  `/workspace/gradle-cache`, Android user home `/workspace/android-user`, Konan `/workspace/konan`.
  Use `JAVA_HOME`, `ANDROID_HOME`, `GRADLE_USER_HOME`, `ANDROID_USER_HOME`, `KONAN_DATA_DIR` with
  those paths; `./gradlew --no-daemon --project-cache-dir /workspace/scratch/kmp-gradle-project-cache
  -Dorg.gradle.workers.max=2 -Pkotlin.compiler.execution.strategy=in-process ...`.
- Local Gradle cache properties use inherited proxy/system CA trust; do not commit/expose them.
  Gradle needs socket/network access even for its daemon. No local Xcode/KVM, but actual GitHub
  **macOS / Xcode 26.6 / arm64 works**. Do NOT claim unavailable macOS infrastructure: workflow
  `multiplatform.yml` already compiled/tested real Native modules and shared UI frameworks.
- `scripts/test-domain.sh` uses Gradle `:domain:prepareOfflineTests` to retain the original Java
  main-suite/audit harness with shared Kotlin dependencies, not a weakened substitute.
  Shared sources/build inputs are included in formatting and screenshot source fingerprints.

## End-of-session handoff

Summarise what changed, file paths, branch/PR/commit, tests actually run and their results, remaining work, and any real-device checks still needed. If interrupted by a usage limit, leave a concise accurate checkpoint above so the one-line resume prompt works without guessing.
