# Stage E parity and CI

Post-audit hardening for area D (`D-parity-ci-release.md`), the C-R02 flake
(`C-navigation-ux-a11y.md`) and the CI flakes recorded in `CONTINUE_CODEX.md`. Branch
`ccr-60f602a7-tfmok3-hard-e`, based on `origin/ccr-60f602a7-tfmok3-audit` (main aa6fa233 plus the audit docs).

No emulator, simulator or Xcode is available here. Android instrumentation, Swift and
`sharedUi/src/iosMain` changes were **not run** locally. The ready-PR and main CI matrix must
prove them.

## 1. Confirmed bugs

| ID | Sev | Root cause | Fix | Regression tests | Local verification | Commit |
|---|---|---|---|---|---|---|
| D-4 | P2 | The shared run comparison and feedback cards were wired only into Android. iOS gaps were not documented anywhere. | (a) The iOS run detail (`AppleHistoryScreen.kt`, `AppleRunComparisonAndFeedback`) now shows `SharedRunComparisonSection` and the read-only `SharedRunFeedbackCard`, with Android's inputs and order. (b) The new `docs/PLATFORM_PARITY.md` lists every known difference with its status and follow-up. It never claims complete parity. The table has a correction to the audit: Run now, unplanned runs and planned or structured execution already exist on iOS from a different entry point (Record page, `plannedId`, the `execute/` quick route), so they are Partial, not missing. Still Android-only: Health Intelligence (needs a HealthKit gateway), strength PB records and PB history (Android screens, not shared), the durability card, adjustment proposals and in-app updates. No shared strength PB history screen exists, so none was integrated. | The shared cards already have androidTest coverage (`SharedRunComparisonTest`, `SharedRunFeedbackCardTest`). The iOS XCUITest now asserts "Run Analysis" on the saved run's detail. | Format check passed. iOS code **not compiled** (no macOS). | 52ffe991 |
| D-5 | P2 | `RDD_FOCUSED_CLASSES` was maintained by hand with no drift check. | Added the 24 missing non-screenshot classes. `Release081ReliabilityTest` (JUnit 3, which the audit count missed) is included, so all 25 uncovered classes are now handled. `CoachingScreenshotTest` is excluded through an `RDD_FOCUSED_EXCLUDED` workflow comment with its reason (35 tests, about 14.5 min). The draft step timeout goes from 15 to 20 min: the old 134 classes took 12–14 min, and the added classes measured about 75 s on API 35 (main run 45243713104). | `scripts/test-android-gate.py` `DraftGateCoverageTest` discovers every JUnit 4 and JUnit 3 androidTest class. It fails on any class that is neither listed nor excluded with a reason, and on duplicate, stale or overlapping entries. It also pins the key D-5 classes and the 20-min draft timeout. | `test-android-gate.py`: 24 tests OK. | 2a5d211c |

## 2. Risks

| ID | Outcome | Evidence | Action |
|---|---|---|---|
| D-R1 (R-1) iOS GPS UI test | Confirmed and fixed | Job 137696416489: "GPS ready" was found after one fix at t=182 s. The test re-waited at t=186 s with no new fix and failed at line 159. `RunMetricsEngine` shows "GPS signal weak" once 5 s pass without a usable fix. Fixes stamped before Start are rejected as stale, so priming before Start would not help. The line 178 failure (job 136748283426) was the older Files "GPX file saved" wait, already hardened since. | A fresh fix every ~2 s while waiting (at most 45), plus one fresh fix right before the unchanged `XCTAssertTrue(gpsReady.waitForExistence(timeout: 20))`. Every assertion is kept. Not compiled here. (aa878595) |
| D-R2 (R-2) Android emulator | Confirmed and fixed (launcher ANR); remainder recommended | Jobs 137881465453 (API 35), 136748283333 and 136697098918 (API 36) all failed with `PLATFORM_FOCUS_BLOCKED` on the launcher3 ANR "Application does not have a focused window". This followed the emulator action's boot-time `KEYCODE_MENU`, before any app test ran. The "adb: device offline" lines are the action's own `sys.boot_completed` polling, and boot then succeeded. The target is the AOSP `default` image, which has no setup wizard. | `run-android-gate.sh` recovers from a launcher3 ANR exactly once: it reports `PLATFORM_LAUNCHER_ANR_RECOVERY`, force-stops launcher3, then proves Settings and launcher focus again. A SystemUI or any other ANR, or a repeated launcher ANR, still fails. Three new stubbed tests cover this. `android.yml` is unchanged. **Recommended:** the API 28 cascade (job 137881464401: a `RootViewWithoutFocusException`, then 23 or more `ComposeTimeoutException`s until the 45-min timeout) needs a fail-fast after N consecutive focus failures in `GateProgressListener` (androidTest, not owned by this stage). (cf9e491b) |
| C-R02 "5K" not displayed | Confirmed (test synchronisation) and fixed | Jobs 137397912915 and 137605545771. The node existed but layout had not settled after `recreate()`. | `waitForIdle()` plus a bounded `awaitCondition` that scrolls to the node and checks it is displayed. The original assertion is kept. `:app:compileDebugAndroidTestKotlin` passed; not run on a device. (c2a33676) |
| D-R3 (R-3) macOS runner loss | Partly mitigated | setup-gradle takes 38–82 s (run 45243713104). One earlier attempt stalled for 45 min. | Step `timeout-minutes: 10` on setup-gradle in apple-shared and apple-device. It can only fail a hung runner sooner. **Recommended only:** step timeouts on the Native test and link steps (10 and 8 min today; cold caches make a safe bound uncertain). (5192f2bb) |
| D-R5 (R-5) iOS back stack | Not fixed (not trivial), documented | `ApplePage` is a single `mutableStateOf` with fixed `onBack` targets. | Documented in `PLATFORM_PARITY.md` with a follow-up: a saveable page stack plus Back or edge-swipe. |
| R-6 physical acceptance | Needs a physical device | All hardware checks are UNPERFORMED. | Listed in `PLATFORM_PARITY.md`. Nothing is claimed. |

## 3. Notes for the coordinator
- Link `docs/PLATFORM_PARITY.md` from README or CONTINUE_CODEX; this stage does not edit either.
- Risks to the full CI run:
  - The iOS edits are not compiled here.
  - The added iOS "Run Analysis" assertion adds one bounded `reveal`.
  - The draft gate now runs 158 classes (about 15 min against a 20-min bound).
