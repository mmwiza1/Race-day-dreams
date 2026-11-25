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

## Current verified release — Piece 5B, 0.16.0-test1

- Owner-authorised [PR #51](https://github.com/mmwiza1/Race-day-dreams/pull/51)
  was squash-merged on 25 November 2026 at 07:20 UTC. Main commit
  `7823372c81d3fe09c9485ed6de5ee461d546f593`, tree
  `3de75a7501980dfa0ba5432ec92aeceadde1efe3`, exactly matched the verified head
  `205d2918e1be7c8d1abc2030e8de056551dbdb37`.
- Android `0.16.0-test1` / 29, iOS `0.16.0` / 29. SQLite 20, JSON backup 14,
  raw archive 3 and complete ZIP 1 are unchanged. Preserve the public release signer.
- [Android](https://github.com/mmwiza1/Race-day-dreams/actions/runs/45559510034)
  passed 857 instrumented tests per API 28/35/36, same-signer upgrade/wrong-key
  and downgrade guards, 524 captures per API and canonical-source verification.
  [Apple](https://github.com/mmwiza1/Race-day-dreams/actions/runs/45559510015)
  passed 1,320 shared Native tests, simulator startup, all five native UI
  cases, and unsigned iPhone/universal simulator builds. The final timed
  recovery case operated the production keypad and workout disclosure and kept
  the original playback assertions. [Detailed handoff](docs/HANDOFF_5A_TO_5B.md)
- Local API 28 probes verified published-base data preservation (temporary
  emulator signing), the actual foreground interval with a pause and exact
  plan linkage, legacy review decision, and new review complete-backup
  round-trip. The 13-profile probe passed 65 weekly reviews and 324 checks.
- Independent post-merge audit documentation is on
  `docs/2026-11-post-5b-audits` in `/workspace/scratch/rdd-piece5b`.
  Results are in the [running](docs/AUDIT_2026-11_RUNNING_PROGRAMMES.md),
  [gym](docs/AUDIT_2026-11_GYM_PROGRAMMES.md) and [full-app](docs/AUDIT_2026-11_FULL_APP.md)
  reports. Proposed remediation requires owner approval. No application changes
  or extra release are part of the audit PR.
- [Post-merge main CI](https://github.com/mmwiza1/Race-day-dreams/actions/runs/45567338048)
  and the rolling [development publication](https://github.com/mmwiza1/Race-day-dreams/releases/tag/development)
  passed. Verified APK bytes from versioned and rolling downloads, package
  `org.racedaydreams`, version 0.16.0-test1 / 29, non-debuggable build,
  manifest and exact source commit. APK SHA-256
  `4004fa1c40d1a12119acfc0ff6a7a617663796313acf645937d03b742848a483`.
  Release certificate SHA-256 remains
  `e7af758a8ff6732ee5fd2e19ab34a85bad1d283ec8b277d83813d7c66afd929d`.
  Independently downloaded and verified all three unsigned iOS pairs and build
  information. [Verification and downloads](docs/AUDIT_2026-11_FULL_APP.md#publication-and-physical-acceptance)
- Next potential implementation is an owner-approved, separate remediation PR.
  R-01 (conflicting marathon readiness copy) is the first priority. No
  unapproved implementation is active. Physical GPS, connected health,
  reminders/audio and screen-reader tests remain outstanding.
- The primary checkout contains unrelated experimental history. Preserve it
  and leave parked PR #50 untouched.

## Historical verified baseline (20 November 2026)

- **Latest: `main` = `e46d884d` (0.15.2-test1 / 25, iOS 0.15.2 / 25), PR #30 merged (checkpoint
  269).** Published and verified by download on 21 November 2026: APK SHA-256 `8f484d61…b6d2`,
  signer cert `e7af758a…929d`, versionCode 25, `latest-update.json` versionCode 25, unsigned `.ipa`
  `4348bc50…8878`. Battery/performance work (0.15.3-test1) is in progress below.
- **Earlier: `main` = `66e963fb` (0.15.1-test1 / 24, iOS 0.15.1 / 24), the first release-signed
  publication (checkpoint 268).** The 0.14.0 details below are kept as the earlier verified state.
- **`main` = `44987a3bd9e0a03686a5caad64524ed7832de908` (0.14.0-test1 / versionCode 22; iOS 0.14.0 /
  build 22).** Verified live on 20 November 2026 (git, PR, Actions and the `development` release).
- [PR #26](https://github.com/mmwiza1/Race-day-dreams/pull/26) (bouldering, battery audit, handover)
  was squash-merged by owner approval ("please merge") as `44987a3` at 22:02 UTC on 19 November.
  Merged tree == verified head `b51f8b1` (canonical screenshots only) / source `159da63`. The PR-head
  gate on `159da63`: Android run 44607724202 and shared run 44607724201 SUCCESS (apple-shared
  attempt 1 stalled 45 min in setup-gradle with no log, an infrastructure loss; attempt 2 passed).
- Post-merge `main` run [44624667007](https://github.com/mmwiza1/Race-day-dreams/actions/runs/44624667007)
  attempt 2 SUCCESS, including publish-test-release. Attempt 1 failed only in the iOS XCUITest
  `testFavouriteSurvivesRelaunchWithoutLocationPermission` ("The native Files picker did not expose an
  actionable Save control" after 60 s; iOS system UI, GPX export untouched by PR #26, passed on every
  PR run); one rerun of the failed job passed. Treat a repeat as a flaky test to harden, not rerun.
- **Published and checked by download:** `Race-Day-Dreams-0.14.0-test1.apk` 59,549,417 bytes, SHA-256
  `d8cf2fb37343bcfc5bc9f85cdcf482c0ead4d5c4b2f11e7128e4db817feb7e18`; `.sha256`, `latest-test.apk`
  alias and build-info all match; `development` tag → `44987a3`; build-info records iOS 0.14.0 / 22
  (simulator `2b0f9e50…`, iPhone unsigned `800bdd5d…`).
- Data formats: SQLite schema **20**, root JSON backup **14** (adds `climbing`), raw recording archive
  **3**, complete backup package **1**. Still no INTERNET permission in 0.14.0.
- **Signing finding (20 November):** every published APK is debug-signed with a *different* throwaway
  "CN=Android Debug" certificate, because `app/build.gradle` has no signing config and each CI runner
  creates a fresh debug keystore. Certificate SHA-256: 0.12.2 `1b241e16…`, 0.12.3 `01f3438b…`,
  0.13.0 `b3837765…`, 0.13.1 `1bbbdccc…`, 0.14.0 `56695050…`. **No existing installation can be
  updated in place by any other published build**; users have always had to back up, uninstall and
  restore. PR #27 addresses this.

## Previous verified baseline (19 November 2026 — 0.13.1; kept for history)

- Repository `mmwiza1/Race-day-dreams`, default branch `main`. **`main` = `2196212f0315da0cad5c9e6f1ed93c8da499d463`
  (0.13.1-test1 / versionCode 21; iOS 0.13.1 / build 21).** Verified live on 19 November 2026
  (git log, GitHub PR list, Actions and the `development` release), not copied from a summary.
- Merged and verified, in merge order (all squash-merged; each merged tree equals its verified PR head):

  | PR | Scope | Merge commit | Verified PR head / source | Post-merge `main` CI |
  | --- | --- | --- | --- | --- |
  | [#24](https://github.com/mmwiza1/Race-day-dreams/pull/24) | 0.13.0-test1 shared Kotlin Multiplatform / Compose Multiplatform Android + experimental unsigned iPhone app | `f7be1ab` (00:13 UTC) | head `529d310` (canonical screenshots) / source `039044a` | [44427299471](https://github.com/mmwiza1/Race-day-dreams/actions/runs/44427299471) SUCCESS |
  | [#23](https://github.com/mmwiza1/Race-day-dreams/pull/23) | Staged draft/ready/main CI; unsigned iOS Simulator + iPhone ZIPs published with the APK from one commit; Skia/Skiko licence inventory | `146cde4` (01:48 UTC) | head `367200b` / source `a392ff9` | [44439687824](https://github.com/mmwiza1/Race-day-dreams/actions/runs/44439687824) attempt 2 SUCCESS (re-ran only jobs lost to a Maven 404 and a lost Mac runner) |
  | [#25](https://github.com/mmwiza1/Race-day-dreams/pull/25) | 0.13.1-test1 Progress and Strength analytics dashboards | `2196212` (12:55 UTC) | head `456af32` (canonical screenshots only) / source `cfcd163` | [44528431552](https://github.com/mmwiza1/Race-day-dreams/actions/runs/44528431552) SUCCESS first attempt |

- **Latest verified development build:** the rolling [Latest Development Build](https://github.com/mmwiza1/Race-day-dreams/releases/tag/development)
  was published by run 44528431552 from commit `2196212` and checked by downloading every asset:
  `Race-Day-Dreams-0.13.1-test1.apk` 59,172,526 bytes, SHA-256
  `01703354747640b5081f9746fb4e7d97ea8f94360e7ab52bf32a05cd4788619d` (aapt: org.racedaydreams,
  code 21, 0.13.1-test1, no INTERNET permission); `-ios-simulator.zip` 67,003,022 bytes, SHA-256
  `8037b8451695b5437a001937261e815f97aa05df06c480a981ecf2a833b408f5` (Info.plist 0.13.1/21,
  iPhoneSimulator, unsigned); `-iphoneos-unsigned.zip` 41,620,321 bytes, SHA-256
  `5fd7d9ecd3c1b3991598c78aec0c02289708cb928f3f0d4ac93e87f53bc86519` (0.13.1/21, iPhoneOS,
  unsigned). `latest-*` aliases are byte-identical; `.sha256` files and build-info match; older
  0.12.2, 0.12.3 and 0.13.0 assets remain. Debug-signed / unsigned test builds only: no stable
  tag, Play Store, TestFlight or App Store distribution exists.
- Data formats unchanged since 0.12.1: SQLite schema **19**, root JSON backup **13**, raw recording
  archive **3**, complete backup package **1**. Android permissions unchanged; no networking.
- Last full software gate (PR #25 source `cfcd163`, PR-head runs
  [44520369536](https://github.com/mmwiza1/Race-day-dreams/actions/runs/44520369536) and
  [44520369623](https://github.com/mmwiza1/Race-day-dreams/actions/runs/44520369623), repeated on
  `main`): Android API 28/35/36 full instrumentation with 459 source-bound captures, lint,
  formatting, guidance, domain/schema/backup/release checks; iOS Native tests, framework link,
  simulator build and SQLite-19 startup, 4/4 XCUITests and unsigned iPhone build.
- **Outstanding physical acceptance (UNPERFORMED; software/emulator/simulator success is not
  hardware acceptance):** Galaxy S24/Pixel/Xiaomi and a real iPhone; outdoor GPS accuracy;
  locked-screen/background recording; real Health Connect, HealthKit and BLE heart-rate monitors;
  audio cues, battery and OEM background behaviour; rotation; human TalkBack and VoiceOver. The
  iPhone app needs the user's own Apple signing. Checklists live in `TEST_BUILD.md`.

## Active work — reliable Android app updates (PR #27, owner request, 20 November 2026)

Branch `feature/android-app-updates` from `main` `44987a3`; draft PR targeting `main`. Scope:
consistent APK signing via protected CI secrets (never committed), safe in-place upgrades preserving
all data, in-app update check against the official GitHub `development` release, verified download
(size, SHA-256, package, newer versionCode, same signing certificate as the installed app) and
Android's user-approved installation, a compact Settings "App updates" card with an optional
notification, release/CI verification, documentation and tests. Classified **major /
security-sensitive**. Android only: no iOS updater, TestFlight or App Store work. No battery
optimisations here.

Hard constraints: **do not merge without the owner's explicit approval; do not generate, publish or
transition a production signing identity or expose credentials without explicit authorisation.**
If signing needs the owner, document the exact setup and stop only the dependent signing/publication
steps. Never uninstall apps or delete user data automatically; never claim seamless updates for
installations that cannot support them (all existing debug-signed installs).

## Standing owner instructions (20 November 2026)

- Every iOS download section (release notes rendered by `scripts/release-notes.py`, README,
  TEST_BUILD) includes the **unsigned iPhone .ipa** link (`Race-Day-Dreams-<version>-unsigned.ipa`,
  `Race-Day-Dreams-latest-unsigned.ipa`), clearly labelled unsigned / re-sign before installing.
- Every progress update to the owner states an ETA (UTC) for the next result. Poll CI no more often
  than every 15 minutes (a background until-loop that stops on the first failed job).

## Active work — 2026 product experience redesign (owner request, 21 November 2026)

- **Plan:** a phased presentation-layer redesign, described in [docs/DESIGN_2026.md](docs/DESIGN_2026.md): benchmark, inventory, design system and roadmap C1–C8.
- **Phase B:** a PR on branch `ccr-60f602a7-tfmok3` (from `main` `9cf6774c`) demonstrates the design system on running Today, strength Today and Progress.
  - The new components are in `sharedUi/.../RddDesignSystem.kt` and `WeekAtAGlance.kt`.
  - Before images are in `docs/design-2026/before/`.
- **Status (22 November 2026):**
  - The owner approved Phase B and authorised merging PR #36 after one correction: the distance-only "Same days last week" comparison now shows in Distance mode only.
  - No version bump. The `main` workflow republishes the rolling `development` build as usual.
  - Next phases (V1 Running trends, V3 Strength progression, then C1–C8) each need separate owner authorisation. See `docs/DESIGN_2026.md` §3c, §5 and §6.
- **Phase B post-merge verification (22 November 2026):** PR #36 was squash-merged as `main` `730fd97d`, with a tree identical to the verified PR head.
  - The `main` run [45032992162](https://github.com/mmwiza1/Race-day-dreams/actions/runs/45032992162) passed. It covered Android API 28/35/36, Apple shared and device, formatting and guidance, and republished the rolling `development` release. The version is unchanged at 0.15.4-test1 / 27.
  - Verified by download:
    - the build info names `730fd97d` and run 45032992162;
    - the APK SHA-256 `01e89594…5ed2` matches its `.sha256` and the `latest-test.apk` alias;
    - apksigner reports certificate `e7af758a…929d`;
    - aapt reports `org.racedaydreams` / 27 / `0.15.4-test1`;
    - `latest-update.json` has versionCode 27, commit `730fd97d` and the same APK SHA and certificate;
    - the unsigned .ipa SHA-256 `e4a40636…00be` matches its `.sha256`.
  - On `main`: `RddDesignSystem.kt`, `WeekAtAGlance.kt`, `RunningTrendExplorer.kt` and `docs/DESIGN_2026.md`, with V1 and V3 marked as major priorities.
- **V1 Running trends (owner-authorised 22 November 2026):**
  - A draft PR from `ccr-60f602a7-tfmok3`, which merges `main` 730fd97d.
  - It moves the Training trends screen onto the shared explorer (§3d), adds heart rate as a fourth measure, and adds long-range month labels, steppers and a missing-data key.
  - Do not merge or release without explicit owner approval.
  - Do not start V2, V3, V4 or C1–C8.
- **V1 merged:** PR #37 was squash-merged as `main` `dbdfac0a`.
  - The post-merge run 45083875183 passed. API 35 needed one rerun after an onboarding "5K" visibility timing flake.
  - It republished 0.15.4-test1 / 27 with APK SHA-256 `44f59e31…3f2d` (matching its `.sha256` and the alias), certificate `e7af758a…929d`, manifest commit `dbdfac0a`, and .ipa SHA-256 `ca123db2…d3ee`.
- **Expressive tab composition:** see `docs/DESIGN_2026.md` §3f.
  - Applied to Strength and Progress → Strength training in PR #38.
  - The owner approved the direction app-wide on 23 November 2026, applied per screen purpose rather than copied: V3 final refinements are in PR #38, and each later screen group gets its own draft PR (see `docs/DESIGN_2026.md` §3g). No merge without explicit approval.
  - Workstream branches are stacked on V3 (`ccr-60f602a7-tfmok3`) while PR #38 is unmerged. The Home PR targets the V3 branch, so its diff holds only Home changes. Retarget it to `main` once V3 merges. Draft checks for a stacked PR are started by `workflow_dispatch` of "Shared Android and iOS checks" on its branch, because that workflow's `pull_request` trigger only covers PRs into `main`.
  - Planned order: W1 Home → W2 Plan and Calendar → W3 Progress Running/Overview → W4 activity, live, Gym Now and race/recovery screens → W5 onboarding (with the Galaxy S24 goal-selection retest), library, settings, bouldering and empty states.
- **V3 merged and published (23 November 2026):** PR #38 was squash-merged as `main` `2e2345a7`, with a tree identical to the verified head `2c796b06`.
  - Post-merge run 45149065398 passed. API 35 needed one rerun after the known onboarding "5K" timing flake.
  - It published 0.15.4-test1 / 27 from commit 2e2345a7.
    - APK SHA-256 `db552154…71b5f`, matching its `.sha256` and the `latest` alias.
    - `apksigner` certificate `e7af758a…929d`.
    - Unsigned .ipa SHA-256 `d80f6217…3efee`.
- **W1 Home merged (23 November 2026):** PR #39 was squash-merged as `main` `95305f41`, with a tree identical to the verified head `f7d4ff29` (source `8926dc51`: Android API 28/35/36, canonical screenshots, iOS shared and device all green).
  - Post-merge run 45175748454 passed. iOS apple-shared needed one rerun after a simulated GPS fix did not arrive in time.
  - It published 0.15.4-test1 / 27 from commit 95305f41.
    - APK SHA-256 `19fe06d6…18e8`, matching its `.sha256`, the `latest` alias and the update manifest.
    - `apksigner` certificate `e7af758a…929d`.
    - Unsigned .ipa SHA-256 `07616f5c…7080`.
- **Visual modernisation W2–W4d merged (23 November 2026):** integration PR #47 was squash-merged as `main` `471d5290`. The tree is identical to its verified head `8b3e7cb8`: source `eadf2de3` passed Android API 28/35/36, iOS shared and device, and one canonical screenshot regeneration of 524 captures.
  - It supersedes #42 (W2), #40 (W3), #41 (W4a), #43 (W4b), #45 (W4d) and #46 (docs). The owner chose one integrated validation over six separate runs. Those PRs are closed with comments naming their final heads, which went in unchanged.
- **W4c merged (23 November 2026, owner's conditional approval):** PR #44 was squash-merged as `main` `aa6fa233`. The tree is identical to its verified head `58cbe2f0` (source `b3812f81` = `main` `471d5290` + W4c). It includes the three owner-required fixes:
  - Android bouldering has a single app-bar Back, and iOS keeps its in-content Back.
  - The race-date countdown reads "Today".
  - Milestone titles have no ✓ and are announced with "Recognised <date>".
- **Final `main` verified:** run 45243713104 on `aa6fa233` was green on attempt 1.
  - It published 0.15.4-test1 / 27 from `aa6fa233`.
    - APK SHA-256 `5bbf7ed9…`, matching its `.sha256`, the `latest` alias and the manifest.
    - `apksigner` certificate `e7af758a…929d`.
    - Unsigned .ipa SHA-256 `a44de976…`.
  - The earlier `471d5290` run was cancelled by the next push to `main` before its API 28 and 35 infra reruns finished; `aa6fa233` covers that tree.
- **Whole-application audit (report only, 23 November 2026):** `docs/AUDIT_2026-11.md`, with area reports in `docs/audit-2026-11/`.
  - 24 confirmed bugs (P1 4, P2 16, P3 4) and 35 risks; no P0.
  - Top items:
    - D-3: the published APK is debuggable;
    - D-1 and D-2: version not bumped and stale release notes;
    - B-01: HealthKit runs dated in UTC in Calendar and review;
    - A-B1 and A-B2: Android recording edge cases.
  - Nothing is implemented. Fix-forward priorities wait for owner approval.
  - **Correction:** the API 35 "5K not displayed" flake is Home's "Current goal" text after recreation (a test-timing issue), not the onboarding goal control. It is not evidence for the Galaxy S24 issue.
- **Recurring CI flakes, for the reliability review (not visual work):**
  - **iOS `RunningWorkoutPersistenceTests.testFavouriteSurvivesRelaunchWithoutLocationPermission`:** three failures on loaded macOS runners on 23 November 2026, at "Could not reach Pause" and at the `gpsReady` wait on line 159. It also caused a job timeout. The simulated location fix is unreliable under load.
  - **Android API 35 onboarding "5K" not displayed:** seen after the V1 and V3 merges.
  - **Android Compose test-thread errors:** "multithreaded access to SnapshotStateObserver" and "performMeasureAndLayout called during measure layout", in tests unrelated to the change under test.
  - **Infrastructure:** `adb: device offline` and HTTP 429 download limits.
- **Remaining visual modernisation, owner-authorised 23 November 2026:** develop it concurrently, one draft PR per workstream into `main`.
  - **Workstreams:**
    - W2: Plan and Calendar, plus the hybrid same-day Home evidence.
    - W3: Progress Running and Overview.
    - W4: supporting screens, split into W4a (history and activity details), W4b (gym, exercise library and records) and W4c (recovery, race day, bouldering and dream routes).
    - W5: onboarding, settings and empty states; see `docs/design-2026/W5-plan.md`.
  - **Coordination rules:**
    - The shared design-system files are edited only by the coordinator.
    - Each workstream writes its notes to `docs/design-2026/<W>.md`.
    - There is one screenshot cycle per PR, run serially because canonical screenshot commits conflict between branches. After a merge, the other branches merge `main` and keep their own source.
  - **Approval:** no later workstream is merged without owner approval.
- **Outstanding issue to retest (owner-reported):** the running-goal selection buttons were unresponsive on the owner's physical Galaxy S24.
  - The agreed fix is in `docs/design-2026/W5-plan.md`: reuse the working selectable cards from `SharedStartingPath`, remove the goal checkboxes, and highlight the selection in gold. It is deferred to the final hardening stage.
  - Retest it on that device after the UI modernisation.
  - Do not mark it resolved on emulator evidence alone.
  - Not in V3 scope unless authorised.
- **V3 Strength progression (owner-authorised 22 November 2026):** see `docs/DESIGN_2026.md` §3e.
  - Draft PR only: no merge or release without explicit approval.
  - No further roadmap phase after V3.
- **Proportional testing (standing owner rule, 22 November 2026):**
  - **SMALL:** focused tests and targeted validation (for example `:sharedUi:testDebugUnitTest --tests …` locally).
  - **MEDIUM:** the relevant module and integration tests.
  - **MAJOR:** progressive testing with broader verification.
  - **RELEASE:** full mandatory release and signing verification.
  - Run a full Android/iOS cycle only when the repository requires it (ready-PR canonical screenshots, merge gates), once, after all changes. Never repeat it for small visual tweaks.
- **CI monitoring (standing owner rule):** check running CI every 15 minutes and also at each job's estimated completion time. Schedule both whenever a run starts.
- **CI note:** API 35's "persistence and draft tests" step now runs close to its 30-minute limit (27 min before Phase B's added captures). A timeout with 0 failures is a time-limit issue; rerun once. Raising the limit needs owner agreement.
- **Rules:**
  - **Do not merge, auto-merge or release without explicit owner approval.**
  - Do not start the C1–C8 phases before approval.
  - No algorithm, schema, integration, recording, signing or update change.

## Release 0.15.4-test1 — UX redesign (owner-authorised, 21 November 2026)

- **Approval:** the owner approved PR #34 (UX simplification, refinements, README showcase) and authorised the merge plus the next verified development release.
- **Versions:**
  - Android `0.15.4-test1` / versionCode 27 (was 0.15.3-test1 / 26).
  - iOS 0.15.4 / build 27.
  - Release notes: `docs/RELEASE_0_15_4.md`.
- **Unchanged:**
  - signing (permanent release certificate `e7af758a…929d`);
  - the update manifest format, package ID, schemas and backups;
  - Health Connect/HealthKit and GPS recording;
  - the five bottom destinations.
- **Merge and publication:**
  - Squash merge of PR #34 after the exact-head checks on the version-bump commit and its canonical-screenshot commit.
  - The trusted `main` run publishes the rolling `development` release.
  - Its `Race-Day-Dreams-latest-build-info.txt` records the exact merged commit, CI run and every artifact SHA-256. That is the authoritative post-merge evidence; no docs-only commit is made after release.
- **Verify by download:**
  - apksigner certificate equals `e7af758a8ff6732ee5fd2e19ab34a85bad1d283ec8b277d83813d7c66afd929d`;
  - aapt reports `org.racedaydreams`, 27, `0.15.4-test1`;
  - the APK SHA-256 equals its `.sha256` and the `latest-test.apk` alias;
  - `Race-Day-Dreams-latest-update.json` has versionCode 27 with the same SHA and certificate;
  - the unsigned `.ipa` and the simulator and iPhone ZIPs are present with matching SHA files.
- **Outstanding physical checks:**
  - an in-place update from 0.15.3-test1 on a Galaxy S24;
  - a human TalkBack and VoiceOver pass of the redesigned screens;
  - an iPhone install of the unsigned `.ipa` with the tester's own signing;
  - the earlier GPS, battery and BLE checklists in `TEST_BUILD.md`.

## Completed — UX simplification and content hierarchy (owner request, 21 November 2026)

Owner-requested redesign of Home, onboarding, Progress, Strength and Settings for review with
ChatGPT. **Do not merge, do not enable auto-merge and do not treat green CI as approval: merge only
on the owner's explicit authorisation.** Branch `ccr-60f602a7-tfmok3` (merged up to `main`
`b1e99333`), PR targeting `main`. No version bump or release unless the owner asks. Scope, design
rationale, before/after captures and deferred items: [docs/UX_REVIEW.md](docs/UX_REVIEW.md).
Training algorithms, schema, Health Connect/HealthKit, recording and bottom navigation are
untouched. Tests changed only to open the new Settings sections or select a button by its click
action; no assertion was removed.

- **Round 2 (owner review 1, 21 November 2026):**
  - Direction approved; six refinements requested and implemented:
    - strength-first Progress, and a balanced hybrid Progress;
    - Home: Coming up promoted, and its actions grouped;
    - Strength Today: planned and unplanned workouts named apart;
    - Settings: no Settings action in its own top bar, and the last opened section remembered for the session (in memory only);
    - pace averages state their sample;
    - a real first-run capture.
  - Round 1 images are kept in `docs/ux-review/round1/`. Still **do not merge**; awaiting the next owner review.
- **Round 3 (owner review 2):**
  - The first-run choice has no Back or Settings (Android and iPhone).
  - Training volume is labelled "Training volume (load × reps)" with thousands separators.
  - The README showcase is refreshed with 14 current captures under six feature headings, with one download link after the gallery. `scripts/test-showcase.py` is updated to match.
  - Round 2 images are kept in `docs/ux-review/round2/`.
  - Still **do not merge** and do not release.
- 0.15.3-test1 (battery/performance, PRs #31–#33) is published and verified by download on
  21 November 2026: APK `bbdd2224…6459`, versionCode 26, signer `e7af758a…929d`, update manifest
  commit `b1e99333`, unsigned `.ipa` `45944e77…9b2a`, main run 44886264006 all green.

## Completed — battery and performance optimisation (0.15.3-test1, owner request, 21 November 2026)

Owner-authorised implementation of `docs/BATTERY_EFFICIENCY_AUDIT.md`, with automatic merge "once
implementation, regression testing and required CI checks have passed" (this work only; do not merge
unrelated PRs). Branch `perf/battery-efficiency` from `e46d884d`. Never trade GPS accuracy, recording
reliability, heart rate or data integrity for battery, and claim no battery percentage without
physical measurements. Do not touch signing credentials or the signing-activation workflow.

- **Implemented:**
  - A-1: notification posted only on change (`NotificationUpdateGate`, cached `PendingIntent`s,
    delete intent re-shows).
  - S-1: incremental live route (`IncrementalRoutePoints` + `RecordingStore.pointsFrom`, gap-checked,
    reset on failure).
  - S-2: linear week bucketing (`RecordedWeekAggregation.byWeek`/`weeklyCounted`, trend points) and
    linear `PhysicalRunSessions.sessions`.
  - A-8 (partial): plain refreshes merged while queued (`CoachOperationRunner.refresh(merge = true)`,
    used by `CoachViewModel.refresh(autoImport = false)`).
  - I-5: iOS gym clock and checkpoint driven by app background/foreground (`AppleAppVisibility`).
- **Deferred with reasons:**
  - A-8 skip-unchanged (time-dependent derivations, side-effect writes, no change counter without a
    schema change).
  - I-1 (no demonstrably redundant commit; `last_mono` is the recovery end time).
  - A-2 (needs Battery Historian).
  - I-3 (needs a real strap).
- **Evidence:** Section 0 of the audit doc. JVM benchmarks are in `DashboardHistoryPerformanceTest`
  (its output is in the jvmTest XML).
- **Checkpoint 270 (21 November 2026, Claude):**
  - [PR #31](https://github.com/mmwiza1/Race-day-dreams/pull/31) was merged as `3b4c06b5`. Its
    tree equals the verified head `aeb8dca2` (screenshots only), whose source is `c94071ea`. All
    PR-head jobs passed.
  - Post-merge run 44855647354 attempt 1 failed in two places:
    - API 36: a launcher ANR before any test ran.
    - `apple-shared`: `testAppleHealthRequiresAnExplicitReadChoice` could not reach "Weight".
  - Attempt 2 passed API 36, but `apple-shared` failed a different test:
    `testFavouriteSurvivesRelaunchWithoutLocationPermission` could not reach "Pause" after Start.
  - Root cause: the XCUITest `tap` helper waited only 2 s before swiping. On the loaded Mac runner
    (17 s app launch), the next screen had not appeared yet, so the swipe hit the old screen or
    scrolled the new control away.
  - Fix PR `fix/ios-ui-test-transition-waits` lets the first look wait 10 s for the transition. It
    does not change assertions or the swipe count. Publication of 0.15.3-test1 waits on that PR.
  - PR #32's first run passed both iOS tests, but API 28 timed out in the new
    `CoachOperationRunnerTest.refreshesThatImportOrAreNotMergeableAreNeverDropped`. This was a test
    race: every read published an equal state, and `StateFlow` does not re-emit an equal value, so
    the waiting collector could miss the final read. Each test read now publishes a distinct
    `startingPath`. Runner code is unchanged.
- **Checkpoint 271 (21 November 2026, Claude):**
  - [PR #32](https://github.com/mmwiza1/Race-day-dreams/pull/32) was merged as `1e8f09f6`.
  - Post-merge run 44871969884 attempt 1 failed in two places:
    - API 36: another launcher ANR before any test ran. The gate fails closed on purpose and is
      unchanged.
    - `apple-shared`: the Pause step now passed, but `GPX file saved.` did not appear within 20 s
      of tapping the system Files picker's Save.
  - Attempt 2 passed API 36. `apple-shared` failed the same test earlier: one simulated fix was set
    and "GPS ready" never appeared within 20 s.
  - Both are single-shot system interactions on the loaded Mac runner. They have nothing to do with
    the battery work, and the iPhone app and recording code are unchanged.
  - Follow-up PR (branch `ccr-60f602a7-tfmok3`) makes the test:
    - resend the same start fix (up to 10 times) until GPS is ready;
    - tap Save again (at most twice) only while the picker still shows it.
  - Both assertions are still required.
  - 0.15.3-test1 is published only after main passes.
- **Local results:**
  - `:sharedDomain:jvmTest` 548/548;
  - app and androidTest compile;
  - formatting check;
  - release-notes tests;
  - iOS code (`AppleAppVisibility`, `AppleGymScreen`, iosTest) is compiled and tested only on CI
    Mac runners.

## Active work — onboarding and running-analytics bug fixes (PR #30, owner request, 20 November 2026)

Focused bug-fix PR (branch `fix/onboarding-and-weekly-run-count`, 0.15.2-test1 / 25, iOS 0.15.2 /
25). Owner reports from a Samsung Galaxy S24; owner-authorised: "Open a PR for my review … Merge
following completion." Do not touch signing credentials or the signing-activation workflow.

- **Bug 1, onboarding review buttons.** Cause: `GoalFormScreen` mapped the app-wide
  `CoachUiState.busy` to `OnboardingView.setSaving`. Every `onResume` calls `model.refresh()`
  (and the first refresh may import Health Connect), so returning to the app disabled the whole form,
  relabelled Create my programme "Saving…", blocked Back and re-rendered with `scrollTo(0, 0)`.
  Save errors went only to a snackbar. Fix: the form disables only for its own save
  (`setSaving(true)` in `onSave`, cleared by the new `saveFailed(message)`; success navigates away),
  re-renders keep the scroll position, buttons are drawn explicitly as Material 3 filled/outlined
  shapes (the before-capture showed an *enabled* Create my programme drawn disabled-grey: the
  platform drawable ignored `setBackgroundTintList` under the app theme; a stateful
  `ColorStateList` on the replacement `GradientDrawable` then stayed disabled-grey on the emulator,
  so colours are now plain values from `isEnabled()` and the test samples drawn pixels), save failures are shown in the form with answers kept, the
  primary button is identified by tag (not label text), and actions have a 12 dp gap.
  `CoachOperationRunner.submit`'s `onFailure` now receives the shown message.
- **Bug 2, weekly run overcount.** Re-importing one Health Connect record is idempotent (keyed by
  provider + record ID). The overcount is cross-source: the same physical run arrives as several
  records (Samsung Health plus another app writing into Health Connect, or an import plus a manual
  entry or native recording), and every count/sum treated each record as a run. Fix: shared
  `PhysicalRunSessions` groups records from *different* sources (provider + origin package) that
  overlap for at least half of the shorter record; one counted record per run (planned-workout link,
  then rdd > gpx > health platforms > assessment > manual, then has distance). Used by
  `RecordedWeekAggregation`, `DashboardAnalytics.running`, trend points, `CalendarData.day` and
  achievements eligibility. Read-only: no stored record is changed, merged or deleted, and no
  migration is needed. The weekly chart has "Show the runs in this week", listing counted runs,
  their source and uncounted copies.
- **Bug 3, race distance.** Pace and programme always used the selected distance (`goal.distance()`),
  and the 10K pace was correct for 10K. Nothing compared the distance with the free-text race name,
  and the review said only "Goal: 10K". Fix: the review says "Race distance: 10K (10 km)" and "Target
  race pace: … over …". `RaceNameDistanceCheck` warns (never infers) when the name clearly names
  another distance, with a "Change race distance" action. 1:55:00 for a half marathon is 5:27/km.
- **Two to three runs a week:** no automatic frequency change exists (only the runner's form choice).
  Editing goals rebuilds the remaining programme from today, keeps every activity, total and older
  plan version (`PlanPreservation.retainCompleted`); new plan IDs mean earlier weeks' planned
  workouts are not carried into the new version. A future enhancement could carry them forward.
- **Before and after screenshots:** commit `0dc93f17` adds only the capture scenario
  (`progress-weekly-run-sources`, `onboarding-review-distance`, `onboarding-review-actions`; 474
  required frames) so CI captures the old behaviour; the fix commit recaptures the same steps.

### Recovery pointer — checkpoint 268 PR #29 merged; first release-signed 0.15.1-test1 published (Claude)

- PR #29 merged as `66e963fb` (verified head `9a613ed4`). Post-merge run 44786672669 attempt 3
  SUCCESS (API 36 reruns: formatter HTTP 429, then a systemui ANR before tests).
- Downloaded and checked: `Race-Day-Dreams-0.15.1-test1.apk` signer `CN=Race Day Dreams`, cert
  `e7af758a…929d`, versionCode 24, SHA-256 `3873f010…337b` == `.sha256` == latest alias; embedded
  pin matches; `Race-Day-Dreams-latest-update.json` matches; unsigned `.ipa` `38224af9…2022`
  published with checksum and linked in the release notes.

### Recovery pointer — checkpoint 267 activation merged; first signed publish refused by parser (Claude)

- PR #28 (activation: fingerprint `e7af758a…929d`, 0.15.1-test1 / 24, whitespace-tolerant decode)
  merged by owner authorisation as `eeccb89` (tree == verified head `70824dd`; source `34c891c6`
  gate API 28/35/36 799 each after infra reruns: API 28 emulator death, apple-device 54-min stall).
- Post-merge run 44748666246: every build/test job green. publish-test-release decoded the keystore
  and signed, then `certificate_sha256` refused: "Exactly one APK signer is required" (failed
  closed; **nothing published; development release still 0.14.0-test1; no release-signed APK
  exists yet**). Local apksigner 35.0.0/36.0.0 with 2048/4096-bit keys print `Signer #1 …`; the
  runner picks its newest build-tools, whose format is unknown. Fix in PR `feature/ios-ipa-and-ci-reliability`:
  pin build-tools 35.0.0 in the publish job, accept any `Signer … certificate SHA-256` line naming
  one certificate, print only public apksigner lines on refusal. Same PR: unsigned .ipa
  publication + links, flaky iOS GPS/Files waits hardened.
- **Next:** after that PR merges, verify the post-merge publish: APK cert == `e7af758a…`,
  versionCode 24, checksums, build-info, `Race-Day-Dreams-latest-update.json`, `.ipa` assets.

### Recovery pointer — checkpoint 266 PR #27 merged; signing identity verified; activation PR (Claude)

- PR #27 squash-merged by owner approval as `a064779f721840edc42f5cf8e46de61eb745054e` (tree ==
  verified head `4e2b104`; source `67833481` gate: API 28/35/36 799/799 each, upgrade script, iOS).
- Post-merge run 44714113936: attempt 1 build (36) failed on a 404 downloading the Kotlin formatter
  jar (infra) and apple-shared on the flaky XCUITest `testFavouriteSurvivesRelaunchWithoutLocationPermission`
  (line 130, simulated GPS distance; its second post-merge flake after PR #26 → harden in a small
  PR, do not loop reruns). Attempt 2: every build/test job green; publish failed at the keystore
  decode (`base64: invalid input`) although the secret had been fixed — a re-run appears to reuse
  the original run's secret snapshot. Attempt 3 (publish only) failed earlier, on Maven Central HTTP
  429 while resolving dependencies (infra), so it was not retried further: 0.15.0-test1 is NOT
  published; the development release stays 0.14.0-test1 until the activation PR's fresh post-merge
  run (current secrets) publishes 0.15.1-test1.
- Signing identity: secret first failed to decode (owner re-saved it). Main-only workflow run
  44727056097 passed: 4 secrets present, keystore/alias/both passwords OK,
  `CN=Race Day Dreams, O=Race Day Dreams`, SHA-256
  `e7af758a8ff6732ee5fd2e19ab34a85bad1d283ec8b277d83813d7c66afd929d`. **Owner confirmed it matches
  their keystore backup and authorised activation.**
- Activation branch `feature/android-signing-activation`: fingerprint file, 0.15.1-test1 / 24,
  `docs/RELEASE_0_15_1.md`; decoders now ignore whitespace/CRLF in the secret. After its gate and
  merge, verify the published APK's certificate == fingerprint, versionCode 24, checksum, build-info
  and `Race-Day-Dreams-latest-update.json` before calling it the first release-signed build.

### Recovery pointer — checkpoint 265 PR #27 completion and signing activation plan (Claude)

- 20 Nov: owner reports the four `RDD_ANDROID_*` secrets are configured (not readable via this
  session's API proxy; never exposed to PR workflows). No fingerprint committed; **no release-signed
  APK published.** Full gate on `f55090d` was green on API 28/35/36 (798/798 each), shared Android/iOS
  and apple-device; bot screenshots `420137d`; `verify-android-upgrade.sh` on API 35: 9000→9001 in
  place with identical data fingerprint, verifier accepted 9002 / rejected key B, Android refused key
  B (`INSTALL_FAILED_UPDATE_INCOMPATIBLE`) and 9000 (`INSTALL_FAILED_VERSION_DOWNGRADE`).
- Earlier gate fixes: API 28 archives lack `signingInfo` → `GET_SIGNATURES` fallback (one signer
  still required); `AppPrivacyTest` now pins INTERNET + REQUEST_INSTALL_PACKAGES only, cleartext off,
  receiver unexported; FakeConnection streams from disk (two 60 MB heap copies broke the UI test).
- This completion commit: secrets-without-fingerprint is now an **identity check** in the main-only
  publish job (real key signs a scratch copy; public fingerprint in log/summary; test-signed APK
  still published, no manifest) instead of a refusal, so merging PR #27 cannot publish the first
  signed release and post-merge CI stays green. New main-only `workflow_dispatch`
  `signing-identity.yml` (public outputs only). Fixture labels derived from the real versionCode
  (older install = 0.14.0-test1/22, newer = 0.16.0-test1/24). New installer hand-off test.
- Gate on `f0fe326`: API 28 799/799 and iOS green; API 35/36 crashed in the new test because
  `appops set … REQUEST_INSTALL_PACKAGES deny` kills the app on Android 12+ (7 tests then unrun).
  The test now never toggles that access (default state + direct real PackageInstaller commit).
- **Next after owner approval to merge:** verify post-merge run and the identity-check fingerprint;
  then an activation PR committing `config/android-signing-certificate.sha256` + versionCode 24 +
  release note, merged only with owner authorisation → first release-signed APK and manifest;
  verify by download (cert, versionCode, checksum, manifest). Never claim it before that.

### Recovery pointer — checkpoint 264 Android updates implemented; draft PR #27 CI next (Claude)

- **Design:** CI re-signs the exact verified API35 APK in the publish job only
  (`scripts/sign-distribution-apk.sh`; secrets `RDD_ANDROID_KEYSTORE_BASE64`,
  `RDD_ANDROID_KEYSTORE_PASSWORD`, `RDD_ANDROID_KEY_ALIAS`, `RDD_ANDROID_KEY_PASSWORD`), requires the
  committed public fingerprint `config/android-signing-certificate.sha256`, one v2/v3 signer and
  identical contents, then publishes `Race-Day-Dreams-latest-update.json` (uploaded last, re-verified,
  no versionCode downgrade). Neither secrets nor fingerprint → unchanged test-signed publication, no
  manifest; only one of them → publication refused. **Signing is NOT configured; no production key
  was generated (owner authorisation required).** Setup steps: `docs/ANDROID_UPDATES.md`.
- App: rules `domain/update/AppUpdates.kt`, codec `storage/AppUpdateManifestJson.kt`, Android
  `update/` (client, `ApkVerifier`, `UpdateInstaller` PackageInstaller + unexported receiver,
  `AppUpdateCoordinator` singleton), `data/ActiveWork.kt` (restore's recording/gym/bouldering guards),
  Settings `AppUpdatesCard` after Appearance. Embedded fingerprint via manifest placeholder; only
  builds whose own cert matches it auto-check (daily, on app start, never during active work), so CI
  test builds never touch the network. New permissions INTERNET + REQUEST_INSTALL_PACKAGES only.
- Tests: 8 domain + 3 codec (JVM, pass locally: 519/683), `AppUpdateTest`, `AppUpdatesCardTest`,
  `UpgradePathTest` (focused draft classes), screenshots `settings-app-updates-*` (471 required),
  `scripts/verify-android-upgrade.sh` after the API35 gate (throwaway keys; restores gate APK),
  Python asset/release-note tests (17/12 pass). Signing script exercised locally with a throwaway key.
- Version 0.15.0-test1 / 23 (Android, iOS Info.plist, CI APK names), `docs/RELEASE_0_15_0.md`.
  Docs: `docs/ANDROID_UPDATES.md`, PRIVACY (first network access, Android only), README, TEST_BUILD
  (physical checklist UNPERFORMED), ARCHITECTURE. Draft PR must not merge without owner approval.

## Completed work — battery audit and bouldering (PR #26, owner request, 19 November 2026)

Branch `feature/bouldering-and-battery-audit` (from `main` 2196212). One branch, three ordered,
logically separate deliverables; the continuation update is part of this branch (no standalone
documentation PR):

1. **Phase 0 — handover (this update).** Done when committed on the branch.
2. **Phase 1 — battery efficiency audit (ASSESSMENT ONLY).** Report: `docs/BATTERY_EFFICIENCY_AUDIT.md`.
   Do **not** implement any battery optimisation until the owner and their senior engineer approve
   specific recommendations. Never trade GPS accuracy, background recording reliability, BLE heart
   rate or workout data integrity for battery. Do not invent measured figures; no physical-device
   profiling has been performed.
3. **Phase 2 — bouldering as a first-class Strength activity** (separate from weightlifting): sessions
   independent of programmes, problems/attempts/sends/flashes/incomplete climbs, V-scale and
   Fontainebleau with the original grade retained and conversions labelled approximate, preferred
   grading system, analytics using the PR #25 reporting periods, history/edit/delete, safe handling
   of unsaved/interrupted sessions, versioned backup/restore compatible with older formats. Must not
   create working sets, e1RM, lifting PBs or muscle-group workload. Five-tab navigation unchanged.
   Draft PR; **must NOT merge without the owner's explicit approval.** Full Android/iOS gate and
   actual-build screenshots before proposing merge.

Progress checkpoints for this work are recorded under "Latest recovery pointer — checkpoint 262+".

### Latest recovery pointer — checkpoint 262 battery audit COMPLETE; bouldering next (Claude)

- Phase 0 handover committed (`29b1e3d`). Phase 1 audit committed as `9412a4c`:
  [docs/BATTERY_EFFICIENCY_AUDIT.md](docs/BATTERY_EFFICIENCY_AUDIT.md), pinned copy
  https://github.com/mmwiza1/Race-day-dreams/blob/9412a4ce1ffcb22178b0c5892af15ce907b70156/docs/BATTERY_EFFICIENCY_AUDIT.md
- Static code audit only; no physical-device or profiler measurements were possible here and no
  figures are claimed. No P0. P1: A-1 Android run notification re-posted ~2x/s (post only on
  change); S-1 live route reloads every point every 5 s while visible (load incrementally).
  P2: S-2 dashboards recompute over full history on the main thread; A-8 full snapshot reload on
  every resume; I-1 iOS flushes SQLite every 1 s vs Android 5 s (integrity trade-off, owner
  decision); A-2 wake lock + 1 Hz GNSS during manual pause (measure first). P3 reliability notes:
  iOS never reconnects a dropped HR strap; iOS gym screen does not checkpoint on backgrounding.
- **Do NOT implement any of these until the owner/senior engineer approve specific items.** Keep
  low-risk (A-1, S-1, S-2) PRs separate from recording-pipeline changes (I-1, A-2).
- Next: Phase 2 bouldering on this branch (draft PR; no merge without explicit approval).

### Latest recovery pointer — checkpoint 263 bouldering 0.14.0-test1 approved for merge; final gate on PR #26 (Claude)

- `e02043e` domain + storage: `domain/climbing/Climbing.kt` (V-scale 19 / Font 24 grades, original
  grade kept, `approximately()` display-only; FLASH requires 1 attempt; sessions 1 min–24 h, ≤300
  problems; resumable `ClimbingDraft`), `ClimbingAnalytics` (PR #25 periods; distribution and
  weekly progression in the chosen system only; other-system/ungraded counted, never converted).
  Schema 20 (`ProfileDatabase` V20 markers → regenerated `SchemaStatements`; API-28-safe SQL, no
  UPSERT/json_extract): `climbing_sessions`, `climbing_session_draft` (id=1), `climbing_preferences`.
  Root JSON 14 `climbing` v1 on shared (`ClimbingArchiveJson`, `SqliteApplicationBackup*`) and
  Android (`AndroidClimbingBackup`, `SqliteBackupRepository`, `BackupManager.VERSION=14`). Roots
  1–13 restore empty climbing; unsaved draft never exported and blocks restore. Tests: 8 domain,
  6 repository (JVM + iOS), backup contract round-trip/blocks, schema 3 new (81 pass), Android
  fixtures drop v20 tables, `SharedBackupEnvelopeTest` Android==shared climbing equality.
- UI: `SharedBoulderingCoordinator` + `SharedBouldering{Home,Session,Review,Detail,History}`;
  Strength "More detail › Train › Bouldering" for everyone and a compact card only for climbers;
  Android routes `bouldering…` (activity-scoped `BoulderingViewModel`); iOS pages BOULDERING_* with
  Strength tab highlighted. `SharedBoulderingScreenTest` (flow incl. interruption/resume, review,
  correction, delete; dashboard separation) in draft focused classes; 6 new canonical captures
  `bouldering-*` (465 required). Docs: ARCHITECTURE, PRIVACY, TEST_BUILD physical checklist.
- PR #26 CI lessons (19 Nov): `94fb7ac` fixed 10 stale Android "backup root 13" asserts and the
  card's nested merged semantics. `468b408` made the iOS first-run launch check read
  `ApplicationSchema.VERSION` instead of a hard-coded 19; draft CI then went fully green (Android
  focused, iOS compile/native/launch/XCUITests). The first ready gate found a real race: the home
  screen's async `refresh()` could overwrite a just-started session, and "Start" before the first
  read could `INSERT OR REPLACE` over an interrupted draft. The coordinator now refuses to start
  while loading, joins pending draft writes before reading and re-reads if a local change happened
  meanwhile (regression test `startingBeforeTheDeviceIsReadNeverReplacesAnInterruptedSession`).
  The large-font capture now waits for "How am I doing?" (navigation labels hide at 2.0×).
- `6fb6fb0` full gate green (782 instrumentation tests on each API 28/35/36, shared Android/iOS,
  apple-device); the API 28 flow failure was an unscrolled tap on Start. Bot commit `35aadb7`
  added the 6 `bouldering-*` captures.
- 19 Nov: the owner approved the merge ("please merge"). Prepared as **0.14.0-test1 /
  versionCode 22** (`app/build.gradle`, `iosApp/Info.plist` 0.14.0/22, APK names in
  `android.yml`) with `docs/RELEASE_0_14_0.md`; the analytics capture now scrolls to "Hardest send
  by week" (it previously duplicated `bouldering-home`). Merge only after the full gate is green on
  the exact final head; then verify main, post-merge CI and the rolling development APK.

## Earlier baseline (12 November 2026 — superseded by the section above; kept for history)

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

## Completed work — shared Android/iOS application, CI staging and analytics (PRs #23–#25)

**Status: COMPLETE, merged and published (see "Current verified baseline").** The checkpoints below
are the historical record of how it was built and verified; they are not an active task list.
Original scope note, kept for context:

The owner's 13 November request authorises Kotlin Multiplatform / Compose Multiplatform,
replacement of the Java domain implementation, shared existing UI, Health Connect / HealthKit,
platform storage/services and functioning Android AND iOS builds. This supersedes the earlier
no-iOS boundary. No version bump/publication until cross-platform ready. Do not merge this
incomplete migration merely because a focused checkpoint passes.

### Latest recovery pointer — checkpoint 261 PR #25 merged; 0.13.1-test1 published (Claude)

- Owner authorised "merge once you feel it is ready". PR #25 final source `cfcd163` passed the full
  ready gate (both workflows SUCCESS); the bot's canonical-screenshot commit `456af32` changes only
  `docs/screenshots/` (GitHub does not run workflows for GITHUB_TOKEN commits, so its runs show
  `action_required`; identical to the PR #24 convention) and the real captures were reviewed
  visually before merge. Squash-merged with an expected-head pin as `2196212`; merged tree ==
  `456af32`.
- Post-merge `main` run 44528431552 SUCCESS (all jobs incl. "Require current canonical screenshots
  on main", Apple shared/device and publish-test-release). Release assets downloaded and verified as
  recorded in "Current verified baseline".
- Lessons kept for future work: keep "More detail" group names different from tab names (tests and
  TalkBack see duplicates); iOS XCUITest sees only on-screen Compose nodes, so scroll/reveal before
  asserting and pause ~1 s after swipes (a tap during a fling only stops it); avoid per-tap frame
  polling (slow on the Intel Mac runner); the draft iOS scope step uses the paginated
  `pulls/{n}/files` API because `gh pr diff` refuses PRs with >300 files.
- Physical-device checks for the dashboards remain unperformed (TalkBack/VoiceOver by a person,
  real phones).

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
    Run on 170fc1d: iOS compile/framework/app/launch GREEN; XCUITests timed out (16 min): the
    frame-polling settle in tap() costs ~1 s per frame read on the Intel runner and disturbed the
    unrelated favourite/GPX test -> replaced by a fixed 1 s pause only after swipes.
    Draft run on 8ea4378 GREEN (Android focused + iOS 4/4 XCUITests). Owner then said "merge once
    you feel it is ready": version bumped to 0.13.1-test1 / 21 (app/build.gradle, Info.plist
    0.13.1/21, android.yml artifact names) with docs/RELEASE_0_13_1.md. Merge only after the full
    ready gate is green on the exact final head (incl. the canonical-screenshot bot commit), then
    verify main, post-merge CI and the rolling Latest Development Build (APK + iOS assets).
    Full gate on 8b15fa3: iOS all green; Android 28/35/36 failed only CoachingScreenshotTest
    click("Strength") (2 nodes) because More detail group headings "Strength"/"Progress" equalled
    tab names -> renamed "Strength training" / "Records & trends" (keep group names != tab names).
    Full gate on b0da811: API 35/36, guidance, formatting, apple-device GREEN; API 28 failed only
    the new empty-state capture (tapped "Progress" before the slow emulator showed tabs -> await
    first); iOS Gym Now flaked at "Default rest…" (expanded below viewport; iOS sees only on-screen
    nodes) -> reveal() scrolls until visible. Same assertions.
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

### Historical migration next actions (completed by PR #24; context only, not an active task list)

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
