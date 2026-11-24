# Race Day Dreams — November 2026 audit remediation

This records how the findings in [AUDIT_2026-11.md](AUDIT_2026-11.md) were handled on the
post-audit hardening branch (`ccr-60f602a7-tfmok3-hardening`, one PR into `main`). The original
audit and area reports are unchanged. The detailed per-stage notes, with root causes, tests and
file references, are:

| Stage | Scope | Notes |
| --- | --- | --- |
| A | Release integrity (D-1, D-2, D-3) | [REMEDIATION-A](audit-2026-11/REMEDIATION-A.md) |
| B | Recording, GPS and health-data dating (B-01, B-02, A-B1–A-B3) | [REMEDIATION-B](audit-2026-11/REMEDIATION-B.md) |
| C | Statistics, units and coaching display (B-03–B-07, C-09) | [REMEDIATION-C](audit-2026-11/REMEDIATION-C.md) |
| D | Navigation, onboarding and accessibility (C-01–C-08) | [REMEDIATION-D](audit-2026-11/REMEDIATION-D.md) |
| E | iOS parity and CI reliability (D-4, D-5, CI risks) | [REMEDIATION-E](audit-2026-11/REMEDIATION-E.md) |

**Verification status.** "Local" means checked in the development container: Kotlin/JVM unit
suites, Android compilation (main, androidTest and the release APK), lint, the format check and the
repository script tests. No emulator, simulator or Xcode is available there. Android
instrumentation, canonical screenshots and all iOS compilation and tests run only in the PR's CI
cycle; their result is recorded in the PR and in `CONTINUE_CODEX.md`, not assumed here. **No
physical-device testing was performed.**

**CI result (24 November 2026):**
- **PR #49, source `15dea009`:** 853/853 Android instrumentation tests passed on each of API 28,
  35 and 36. Canonical screenshots, apple-shared and apple-device passed.
- **Merged** as `main` `60b5201b`. The post-merge run 45348502595 passed after one infrastructure
  rerun.
- **Published:** 0.15.5-test1 / 28. The APK SHA-256 is `41ba4345…1c97`. It is not debuggable and is
  signed with certificate `e7af758a…929d`.

## 1. Confirmed bugs (24)

A bug is "Resolved" only with a code fix plus a regression test (or, for CI and release findings,
an objective automated check).

| ID | Sev | Root cause (short) | Fix | Regression evidence | Status | Commit(s) |
| --- | --- | --- | --- | --- | --- | --- |
| D-3 | P1 | Only the debug build existed; the publish job re-signed the debuggable `…-debug-api-35` APK | `release` build type (not debuggable, same package/version); CI builds it and publishes it; `assert-not-debuggable.sh` runs before and after re-signing; signing secrets, script and certificate pin unchanged | aapt check in CI build and publish jobs; local `assembleRelease` reports no `application-debuggable` | Resolved | f2e8bb0d |
| D-1 | P1 | Version never bumped; publication allowed an equal versionCode with different bytes | 0.15.5-test1 / 28 (Android and iOS); `check_not_downgrade` refuses a reused versionCode with a different SHA-256 | `test-development-assets.py` (function, CLI and version-source consistency) | Resolved | f2e8bb0d |
| D-2 | P1 | `RELEASE_0_15_4.md` never replaced | `docs/RELEASE_0_15_5.md` per the AGENTS.md contract, aligned with the integrated scope | `test-release-notes.py` | Resolved | f2e8bb0d, 73712904 |
| B-01 | P1 | Offset-less runs dated with `startOffset() ?: UTC` in Calendar, weekly review, History and prediction | Shared `ActivityDates` (run's own offset, else the device zone, DST-aware), used by Calendar, weekly review, History, prediction, milestones, best efforts and baseline; placeholder `Z` offsets from native/GPX treated as unknown | `ActivityLocalDatingTest` (UTC+10, UTC−8, NY and Sydney DST days, week edges, cross-screen agreement) | Resolved (see residual) | eb406aac |
| B-02 | P2 | Weekly review and prediction summed raw records | Both use `PhysicalRunSessions.counted`, as Progress does | `WeeklyReviewEvidenceTest` | Resolved | c0ea6d79 |
| A-B1 | P2 | Finish before 2 GPS points threw, stopped the service and stranded an ACTIVE run | `requestFinish()` decides first; recording continues with a message; a second tap within 10 s discards cleanly | `FinishBeforeGpsLockTest` | Resolved | f365fc43 |
| A-B2 | P2 | Auto-resume needed ≤ 8 m accuracy while recording accepts ≤ 35 m | Resume accepts ≤ 35 m with a 2× accuracy displacement guard; rejected fixes no longer reset resume | `AutoPauseReliabilityTest` | Resolved | f365fc43 |
| A-B3 | P3 | Pause/resume timestamps lagged detection; resume fixes dropped | Pause dated from the still window, resume from the first moving fix; confirming fixes replayed | `AutoPauseReliabilityTest` | Resolved | f365fc43 |
| B-03 | P2 | Signed day difference printed raw | Countdown says "Today · race day" and "N days ago" after the race | `HomeCountdownTest` | Resolved | 81a54c97 |
| B-04 | P2 | km hard-coded on Home, weekly review, race pacing and prediction reasons | Shared `DisplayUnits`; prediction reasons take the unit | `DistanceUnitsDisplayTest`, `racePredictionReasonsFollowTheImperialSetting` | Resolved | 7e59f7a3, d4ef52cc |
| B-05 | P2 | Mass unit applied only on dashboards | `MassText`; entry and display in lb, stored in kg at full precision; analytics hosts pass the unit | `GymMassEntryTest`, `MassTextTest` | Resolved (see residual) | d4839d7b, d4ef52cc |
| B-06 | P2 | Skipped workouts removed from the plan before counting | Removed skips counted on the decision date | `DashboardAnalyticsContractTest` case | Resolved | 77ce8c6c |
| B-07 | P3 | Strip used raw activities and per-day gym subtraction | Physical-run grouping and link-aware gym matching | `WeekAtAGlanceTest` | Resolved | 7ae02f32 |
| C-09 | P3 | Period selectors without a discipline fell back to colour-only chips | Strength and Bouldering pass `Discipline.STRENGTH`; fallback removed | `SharedDashboardScreenTest` assertion (CI) | Resolved | 3bab9059 |
| C-01 | P2 | Goal tap re-rendered the whole native form and scrolled to top | Goal step built once and updated in place | `OnboardingGoalCardsTest` (CI) | Resolved | 0ddb903a |
| C-02 | P2 | Options had no ripple/checked state | Full-width selectable cards with gold selection and check | `OnboardingGoalCardsTest`, `OnboardingPaletteTest` (CI) | Resolved | 0ddb903a |
| C-03 | P2 | App-bar Back bypassed screen `BackHandler`s | App-bar Back routed through the back dispatcher | `NavigationBackStackTest` (CI) | Resolved | 05176976 |
| C-04 | P2 | Five shared pages drew an in-content Back under the Android app bar | `appBarBack` parameter; Android hides it, iOS keeps it | Screen tests for Body, Recovery, Milestones, Storage, Custom exercise (CI) | Resolved | 3b2a0e6d |
| C-05 | P2 | Strength focus started outside the tabbed shell | Lands on Home with tabs; gym opens above Home | `PrimaryFocusOnboardingTest` (CI) | Resolved | 05176976 |
| C-06 | P2 | Labels and title dropped at font scale ≥ 1.5 | Labels and full title always shown, capped growth with ellipsis | `SharedCoachShellTest` (CI) | Resolved | 9bcc109f |
| C-07 | P2 | No live region or heading | Heading plus polite live region; stable download announcement | `AppUpdatesCardTest` (CI) | Resolved | c6dc5f63 |
| C-08 | P3 | Saved run pushed over the recorder | Saved activity replaces the recording route | `NavigationBackStackTest` (CI) | Resolved | 05176976 |
| D-4 | P2 | Shared run comparison/feedback wired only on Android; gaps undocumented | iOS run detail shows both; `docs/PLATFORM_PARITY.md` lists every remaining gap | iOS UI test checks "Run Analysis" (CI) | Resolved (documented gaps remain) | 52ffe991 |
| D-5 | P2 | Draft-gate class list maintained by hand | All classes listed or excluded with a reason; guard test fails on drift | `test-android-gate.py` coverage tests | Resolved | 2a5d211c, 4d5636be |

"(CI)" marks tests that compile locally but run first in the PR's emulator or simulator jobs.

**Known residuals (not hidden):**
- **B-01:** Home and Progress weekly figures still date runs through an `Instant → LocalDate`
  callback in the device zone. They now agree with Calendar and History for every run recorded in
  the phone's zone and every offset-less import (the audited defect). Only a run imported with an
  explicit offset different from the phone's (for example recorded while travelling) can still
  fall on a different day there. Fixing it needs the shared weekly aggregation to take records
  instead of instants, which was out of scope for this PR.
- **B-05:** reference-load editors opened from the programme builder, programme generation and
  exercise swap still take kg; their field label says "kg", so a pound value cannot be stored by
  mistake.
- **C-01/C-02 and the Galaxy S24 report (C-R01):** awaiting physical-device verification.

## 2. Risks and recommendations (35)

| ID | Pri | Outcome | Action / evidence | Commit |
| --- | --- | --- | --- | --- |
| A-R1 | P1 | Confirmed in code; fixed; needs physical device | iOS recording clock uses `CLOCK_MONOTONIC_RAW`, which keeps counting during sleep. Locked-screen iPhone check outstanding | 83d20edf |
| A-R2 | P2 | Confirmed; fixed | iOS location and heart-rate delegate callbacks guarded like Android | 83d20edf |
| A-R3 | P2 | Confirmed; deferred | Pause-aware splits need pause intervals stored with samples; noted in release notes | — |
| A-R4 | P2 | Confirmed; deferred (owner decision) | Automatic Health import after onboarding changes Health Connect/HealthKit behaviour | — |
| A-R5 | P3 | Not changed; needs measurement | iOS route tab cost to be measured on a device | — |
| A-R6 | P3 | Confirmed; fixed | A failed Start discards its empty staging | f365fc43 |
| A-R7 | P3 | Needs physical device | Battery items A-2, I-1, I-3 stay as recorded in the battery audit | — |
| A-R8 | P3 | Confirmed; fixed | Manifest comment and battery audit describe the update check | 76d8f8ce |
| A-R9 | P3 | Deferred enhancement | iOS auto-pause (policy is now shared) | — |
| B-R1 | P2 | Confirmed; deferred | Rebuild-safe plan completion needs a product decision on superseded sessions | — |
| B-R2 | P3 | Confirmed behaviour; documented | "+1 day" move never offered; training-logic decision for the owner | — |
| B-R3 | P3 | Confirmed; fixed (display) | Race day states the closing pace that finishes on target | 7e59f7a3 |
| B-R4 | P3 | Confirmed; fixed | "Hold" keeps planned volume without distance evidence | c0ea6d79 |
| B-R5 | P3 | Confirmed; fixed | Weekly review "today" boundary is local midnight | c0ea6d79 |
| B-R6 | P3 | Confirmed; fixed | Bouldering timer stops at 24 h and asks for the real time | 1cdbab3c |
| B-R7 | P3 | Not user-visible; documented | Weekly sends count not rendered anywhere | 1cdbab3c |
| B-R8 | P3 | Confirmed; fixed | Warm-ups round down to 2.5 kg / 5 lb plate steps | d4839d7b |
| B-R9 | P3 | Partly addressed | New WeekAtAGlance and countdown tests; zone dating covered by B-01 | 7ae02f32 |
| C-R01 | P1 | **Needs physical device** | Code contributors C-01/C-02 fixed; the ~4 dp busy-bar shift on resume remains. Galaxy S24 retest outstanding — not marked resolved | 0ddb903a |
| C-R02 | P2 | Confirmed (test sync); fixed | "5K" check waits for idle and scrolls in a bounded wait; assertion kept | c2a33676 |
| C-R03 | P2 | Confirmed; deferred | No active-run entry from Home / no Back confirmation on the recorder | — |
| C-R04 | P3 | Not confirmed | Back from Settings returns to the onboarding form | — |
| C-R05 | P3 | Confirmed; partly fixed | Glyph no longer spoken on Recovery and Storage disclosures; remaining "Optional warm-up", "Timer settings", "Calculation & sources" and `Explainer` glyphs are used as lookup text by iOS UI and screenshot tests and were left unchanged | 3b2a0e6d |
| C-R06 | P3 | Deferred | Calendar day cells ~44–46 dp at 360 dp width | — |
| C-R07 | P3 | Deferred enhancement | Run vs Strength week dots differ by hue (semantics correct) | — |
| C-R08 | P3 | Deferred | "Tap to show exercises" state description in strength progression | — |
| C-R09 | P3 | Not confirmed | Declined install permission leaves "Install update" able to ask again | — |
| D-R1 | P1 | Confirmed; fixed | iOS GPS UI test keeps fresh simulated fixes flowing; assertions kept | aa878595 |
| D-R2 | P1 | Confirmed; partly fixed | One-time recovery from the boot launcher3 ANR. The API 28 cascade after a focus failure still needs a fail-fast in `GateProgressListener` (recommended) | cf9e491b |
| D-R3 | P2 | Partly mitigated | 10-minute timeout on macOS setup-gradle | 5192f2bb |
| D-R4 | P2 | Confirmed; recommendation only | Move `RDD_ANDROID_*` secrets to a protected `release` environment (owner action; no signing change made) | — |
| D-R5 | P2 | Not fixed; documented | iOS has no back stack or saved navigation state; follow-up in `PLATFORM_PARITY.md` | 52ffe991 |
| D-R6 | P2 | Needs physical device | All physical acceptance is UNPERFORMED | — |
| D-R7 | P3 | Confirmed (theoretical); fixed | Installer hashes exactly the bytes written to the install session | b1420210 |
| D-R8 | P3 | Disclosed limitation; deferred | Backups unencrypted, as Settings and PRIVACY.md say | — |

## 3. Outstanding physical acceptance

None of the following has been performed: Galaxy S24 onboarding retest (C-R01); locked-screen
iPhone recording time (A-R1); outdoor GPS and auto-pause on Android (A-B2, A-B3); the first
in-place update from the debuggable 0.15.4-test1 to the non-debuggable 0.15.5-test1 on a phone;
Health Connect, HealthKit, Bluetooth heart rate, battery and human TalkBack/VoiceOver passes.
