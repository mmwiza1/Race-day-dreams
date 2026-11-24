# Area D — Platform parity, stability, security, CI and release readiness

Audited `main` aa6fa233 (read-only). The post-merge run 45243713104 for aa6fa233 completed **success on
attempt 1**, and the `development` tag now points at aa6fa233.

## 1. Scope

**Inspected**
- `app/build.gradle`, the merged debug manifest and `AndroidManifest.xml`, and `data_extraction_rules.xml`.
- The update path: `app/.../update/*.kt` and `scripts/development-assets.py` (manifest and downgrade rules).
- Signing: `scripts/sign-distribution-apk.sh` and `.github/workflows/signing-identity.yml`.
- Workflows: all three in `.github/workflows` (`android.yml`, `multiplatform.yml`, `signing-identity.yml`).
- The draft-gate list `RDD_FOCUSED_CLASSES` (`multiplatform.yml:62`), compared with every `app/src/androidTest` class (`@Test`, `AndroidTestCase`, Kotlin and Java).
- iOS sources: `sharedUi/src/iosMain` (`AppleAppHost` page model and navigation) and `iosApp`, compared with Android `coach/navigation/*`.
- Logging calls, `CoachActivity` and `CoachViewModel` lifecycle, the iOS dispatcher, and iOS `Info.plist` versions.
- Release material: `docs/RELEASE_0_15_4.md`, `CONTINUE_CODEX.md` (baseline, flakes, UNPERFORMED lists) and the live `development` release assets.
- CI history: main runs from 19–23 Nov via `gh api`, with failed job logs for jobs 137881464401, 137881465453, 137605545771, 137397912915, 136748283333, 136697098918, 136488191809, 137696416489 and 136748283426.

**Could NOT verify**
- Anything on a physical device: Galaxy S24/Pixel/Xiaomi, a real iPhone, outdoor GPS, locked-screen recording, real Health Connect/HealthKit/BLE, OEM battery and audio behaviour, human TalkBack/VoiceOver, and the in-place update from 0.15.3-test1.
- Runtime main-thread or IO behaviour, which needs StrictMode or a profiler run (no builds were allowed).
- Jobs of the feature-branch PR runs from 23 Nov: the API returns 0 jobs for them, so the branch run history is not inspectable.

## 2. Confirmed bugs

| ID | Sev | Area | Files | Evidence | Impact | Proposed fix |
|---|---|---|---|---|---|---|
| D-1 | P1 | Release / in-app updates | `app/build.gradle:15-16`; `scripts/development-assets.py:259-265`; `update/ApkVerifier.kt` (`longVersionCode <= installed` rejected) | Six feature merges since the 0.15.4 merge (`9cf6774c`) kept `0.15.4-test1` / versionCode 27: #36 Phase B, #37 V1, #38 V3, #39 W1, #47 W2/W3/W4a/b/d and #44 W4c. Each green main run republished `Race-Day-Dreams-0.15.4-test1.apk` (`--clobber`) and `latest-update.json` at the same versionCode 27. The manifest code says "equal codes are never offered", and the verifier refuses an APK that is not newer. | A tester who installed 0.15.4-test1 on 21 Nov is never offered the redesign builds in Settings › App updates. The file name `0.15.4-test1` now names different bytes over time, which breaks the version-to-checksum traceability promised in AGENTS.md. | Before the next publication, bump to 0.15.5-test1 / 28 (iOS 0.15.5 / 28) with its own release note. Add a CI guard that refuses a main publication whose versionCode equals the live manifest but whose APK SHA differs. |
| D-2 | P1 | Release notes accuracy | `docs/RELEASE_0_15_4.md` (unchanged since `9cf6774c`; `git diff 9cf6774c HEAD` is empty) | The live `development` release body is rendered from this file. It describes only the 21 Nov UX build. It says nothing about the shipped chart-first Progress, rolling 7-day Home, running trends, Strength progression charts or the W1–W4 visual modernisation. | Testers get notes that do not describe the build they download, against the AGENTS.md rule "do not claim … / write for testers". | Write `docs/RELEASE_0_15_5.md` covering Phase B, V1, V3 and W1–W4, together with the D-1 bump. |
| D-3 | P1 | Security / privacy (Android) | `.github/workflows/android.yml:66` (`:app:assembleDebug`), lines 286-288 (publishes the `…-debug-api-35` artifact); `app/build.gradle` (no `buildTypes`); `app/build/intermediates/merged_manifest/debug/.../AndroidManifest.xml:67` `android:debuggable="true"` | The publish job re-signs the verified **debug** APK with the permanent release key. Re-signing does not change the manifest, so every published, in-place-updatable build is debuggable. | On any phone with USB debugging enabled, `adb shell run-as org.racedaydreams` (or a JDWP attach) can read the SQLite database: GPS routes, heart rate, sleep, HRV, weight and training history. `allowBackup=false` and the data-extraction exclusions do not stop this. Debug builds also skip R8 and run slower, which skews battery and performance testing. | Add a `release` build type (non-debuggable; R8 optional at first), gate and publish it, and keep the debug APK for instrumentation only. Add a CI assertion with `aapt dump badging` / `apkanalyzer` that the published APK is not debuggable. Needs owner approval because it changes the signing and publication path. |
| D-4 | P2 | Platform parity (iOS missing screens) | Android: `coach/RunFeedbackScreen.kt`, `coach/RunComparisonScreen.kt`, `coach/HealthIntelligenceScreen.kt`. Shared: `sharedUi/.../SharedRunFeedbackCard.kt`, `SharedRunComparisonSection.kt`, `SharedHealthTrendCard.kt` | These shared composables are called only from `app/`; there are no call sites in `sharedUi/src/iosMain`. `ApplePage` (`AppleAppHost.kt:166-201`) has no equivalent of Android routes `health`, `run` (Run now), `spontaneous`, `execute/{version}/{workout}`, `strength/pbs/history` or `strength/source/{id}`. | iPhone users cannot give post-run feedback, compare runs, or see the Health Connect/HealthKit recovery trends that Android shows. Release notes only mention "iPhone onboarding uses its own screen", so this gap is undocumented. | Either wire the existing shared cards into `AppleRecordedResults` / `AppleAppHost`, or list the gaps under Known limitations. |
| D-5 | P2 | CI — draft gate coverage | `.github/workflows/multiplatform.yml:62` | 24 instrumentation classes are not in `RDD_FOCUSED_CLASSES`: AppPrivacyTest, LauncherIconTest, RefinementCompatibilityTest, CoachingScreenshotTest, ExerciseLibraryDesignTest, PrimaryFocusOnboardingTest, RecordedWorkoutPresentationTest, RecordingAvailabilityTest, RefinementUiTest, Release05UiTest, RunningPerformancePresentationTest, SettingsRefinementTest, StrengthTodayScreenTest, TrainingTrendsScreenTest, DevelopmentStoreTest, ExerciseMetadataAndroidTest, GymNowWorkoutsAndroidTest, PrimaryFocusRepositoryTest, RecordingInventoryRepositoryTest, StrengthDraftActivationRepositoryTest, StrengthReleaseStoreTest, OnboardingDraftTest, OnboardingPaletteTest and OnboardingReviewTest. Every class in the list exists, so there are no stale entries. | Privacy, onboarding (the Galaxy S24 goal-selection area), primary-focus, Today and trends regressions surface only at the expensive ready-PR matrix, which makes late red runs and reruns more likely. | Add the cheap non-screenshot classes, at least AppPrivacyTest, PrimaryFocus*, Onboarding*, StrengthTodayScreenTest, TrainingTrendsScreenTest and the `data.*` stores. Add a `test-android-gate.py` check that every androidTest class is either listed or explicitly excluded. |

## 3. Risks and recommendations

| ID | Pri | Area | Files | Evidence | Impact | Proposed fix |
|---|---|---|---|---|---|---|
| R-1 | P1 | CI flake — iOS GPS UI test | `iosApp/UITests/RunningWorkoutPersistenceTests.swift:159,178` | `testFavouriteSurvivesRelaunchWithoutLocationPermission` failed on main runs 44624667007, 44855647354 (×2), 44871969884 (×2) and 45175748454, with XCTAssertTrue at lines 159 (gpsReady) and 178 (Pause). Each failure took 223–275 s. | It is the most frequent main-publication blocker, and reruns have become routine, against the AGENTS.md rule. | Inject a deterministic location provider via a launch argument instead of simulator location. Split the "without permission" relaunch check from the GPS-fix check. |
| R-2 | P1 | CI — Android emulator instability | Jobs 137881464401 (API 28), 137881465453 and 137605545771 (API 35), 136748283333, 136697098918 and 136488191809 (API 36) | API 28 had a cascade: `RootViewWithoutFocusException` in GymSubstitutionScreenTest, then 23+ CoachingScreenshotTest `ComposeTimeoutException`s, then the 45 min timeout. API 35/36 showed `adb: device offline` and launcher3 ANRs ("Input dispatching timed out"). API 35 also failed `CoachOnboardingTest#canContinueWithoutHealthAndSaveGoalThroughExistingForm` with "component is not displayed" (twice). | Main-run 471d5290 never went green: attempt 1 failed on API 28/35 and attempt 2 was cancelled by aa6fa233. It is covered only transitively by aa6fa233's green run. | Fail fast on the first focus loss, with a per-class timeout and an early abort after N consecutive focus failures. Make the onboarding "5K" assertion wait with `waitUntil` plus scroll. Consider sharding CoachingScreenshotTest away from functional tests. |
| R-3 | P2 | CI — macOS runner loss | `multiplatform.yml` apple-shared (`timeout-minutes: 72`) | CONTINUE_CODEX records apple-shared attempt 1 of 44607724201 stalled 45 min in setup-gradle with no log. The XCUITest step also caused a job timeout (flake list). | Wasted macOS minutes and a delayed publication. | Add a step-level `timeout-minutes` on setup-gradle and on the Native test steps, so a hung runner fails in minutes. |
| R-4 | P2 | Signing-key exposure surface | `android.yml:304-312`, `signing-identity.yml` | Secrets are repository-level and the publish job has no `environment:` with protection rules. AGENTS.md notes that server-side branch protection may be unavailable. Secret handling itself is sound: env-var passwords, no echo, umask 077. | Anyone who can push to `main`, or push a branch with a new `on: push` workflow, can sign arbitrary code with the update key. The in-app updater trusts that key absolutely (`ApkVerifier.kt` pin). | Move the four `RDD_ANDROID_*` secrets into a protected `release` environment restricted to `main` and `v*` tags, ideally with a required reviewer. Enable branch protection or rulesets if the plan allows. |
| R-5 | P2 | iOS navigation / Back | `AppleAppHost.kt:166-240, 821-850` | iOS navigation is a single `remember { mutableStateOf(ApplePage.START) }` with hard-coded `onBack` targets (lines 413, 535, 548, 556, 567, 574). There is no back stack, no swipe-back gesture and no `rememberSaveable`. Android uses navigation-compose with system Back and predictive back (`enableOnBackInvokedCallback`). | A screen reached from Calendar can return to a different parent. Off-tab pages leave no tab selected, and state is lost when iOS evicts the app. | Introduce a small page stack (a list in `rememberSaveable`) with an edge-swipe or `BackHandler` equivalent, and test Back from Calendar → dated Gym → detail. |
| R-6 | P2 | Physical acceptance | `CONTINUE_CODEX.md:83-87, 217-222, 175-179` | All hardware checks remain UNPERFORMED: S24/Pixel/Xiaomi and iPhone installs, outdoor GPS, locked-screen recording, Health Connect/HealthKit/BLE, audio, battery, OEM background behaviour, rotation, human TalkBack/VoiceOver, the in-place update from 0.15.3, and the S24 goal-selection retest. | No stable release can be claimed. | Run the TEST_BUILD.md checklists on the S24 against the D-1 bumped build. |
| R-7 | P3 | Update path | `update/UpdateInstaller.kt` | The APK is verified (size, SHA, package, versionCode, single signer equal to installed and pinned cert) and then re-read for the install session. It stays in private storage, so the time-of-check/time-of-use gap is theoretical. Redirects are followed, but HTTPS-only applies and cleartext is blocked. | Low. | Optionally hash the stream while writing it to the session and compare once more before `commit`. |
| R-8 | P3 | Privacy disclosure | `SettingsScreen.kt:156`, `PRIVACY.md` | Exported backups are unencrypted; this is disclosed in-app. Logging is minimal: only two `Log.w` calls, with no personal data. | Low. | Consider optional passphrase encryption for exports (see section 4). |

**Positive checks with no issue found**
- The manifest exports only the launcher activity and the Health Connect rationale activity/alias; the alias is permission-guarded. Receivers and the recording service are not exported, and there is no FileProvider.
- `allowBackup=false` and the extraction rules exclude every domain, and `usesCleartextTraffic=false`.
- Update checks are limited to release-signed builds.
- `flushDraft` writes asynchronously via `viewModelScope` and `SavedStateHandle`.
- The iOS IO dispatcher is `Dispatchers.IO`, and there is no `GlobalScope` or `runBlocking` in production sources.

## 4. Release readiness summary

- **Versioning and notes:** Android `0.15.4-test1` / 27 and iOS 0.15.4 / 27 are consistent (`app/build.gradle`, `iosApp/Info.plist`), but stale against the content (D-1, D-2).
- **Release-notes contract:** the structure of `RELEASE_0_15_4.md` is valid (H1 and section order), but its content is outdated.
- **Publication workflow:** robust. It re-checks the live main SHA before each upload stage, verifies by download, never downgrades, and switches the aliases last.
- **Before a stable release:**
  1. Fix D-3 so a non-debuggable release build is published.
  2. Bump the version and write the release note (D-1, D-2).
  3. Harden R-1 and R-2 so main is green without reruns.
  4. Close or document the iOS parity gaps (D-4, R-5).
  5. Carry out the physical acceptance in R-6.
  6. Get explicit owner authorisation for a stable tag; none exists yet.

## 5. Future ideas
- Optional encrypted backup export (passphrase, AES-GCM), shared through `sharedStorage`.
- CI "flake ledger": automatically annotate known-flaky test names in the failure-explainer step.
- An iOS update notice that links to the release page, since iOS has no updater; for parity of awareness only.
- A shared navigation model in `sharedUi` (common back stack) so Android and iOS cannot drift.
