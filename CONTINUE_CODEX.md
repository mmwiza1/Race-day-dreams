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

The owner's13November request authorises KotlinMultiplatform/ComposeMultiplatform migration,
Java-domain replacement, sharedexistingUI, HealthConnect/HealthKit, platformstorage/services,
functioningAndroid+iOS builds and eventual gatedmerge/testpublication. This supersedes the old
"no iOS without new request" boundary. No versionbump or publication until cross-platformready.
Do NOT merge this incomplete migration, even if its focused checkpoint checks pass.

- Branch`experiment/android-ios-kmp-compose`, from latestmain85e1c3903b61f1d45e444d120ba1a701ad3a2c36.
  First verifiedbatch040ea9f801117bb96d54acbe4427c26943d80142 is committed/pushed; DRAFT [PR24](https://github.com/mmwiza1/Race-day-dreams/pull/24). Separate ownerCI PR23 untouched.
  Versions0.12.3-test1/code19 and dataformats19/13/3/1 remain unchanged. No artifacts published.
- Implemented actual Android-used sharedDomain/commonMain:14existing enums/bodyunit conversions
  plus deterministicPacePolicy; original15Javafiles removed. ExistingJavaAPI PaceCalculator is
  now a Kotlin/JVM timeadapter delegating only to commonpolicy.204Java domainfiles remain.
- SharedUi/commonMain owns the EXISTING spacing/chips/tabs, CoachPage/cards/buttons/headings,
  WhyDetails and brandedtheme/typography/shapes. Androidconsumes it directly, retainingAndroid
  dynamiccolour/preferences/systembarintegration. No redesigned or alternative demoapp created.
  Kotlin2.1.21/ComposeMultiplatform1.8.2 pinned; AGP8.9.2 retained. Formatting/sourcefingerprint
  includes sharedpaths/buildinputs; third-party notices mirrored. No missing legacytest assertions.
- Verified: baseline700,189domainchecks/48suites and schema75; afterconversion all700,189legacy
  checks pass unchanged,3newcommonJVMtests pass. Final facadefix APK/testAPK/lint BUILD SUCCESSFUL
  122tasks/5m32s (`/workspace/scratch/kmp-facade-build.log`); APK58,836,422bytes/noINTERNET.
  Earlyrootpluginresolution failure was localGradle ignoring inheritedproxy; publicPOM200proved
  available, localGRADLE_USER_HOME properties now use inheritedproxy/systemtrust (not committed).
  Next compile exposed top-levelJVMfacadecollision: renamed commonfileSharedCoachComponents.kt;
  instrumentation now compiles, no source/test functionality removed. RuntimeUI stillpendingCI.
- Focused CI: new multiplatform.yml runs commonJVM/legacydomain/schema/Androidcompile/lint and
  existingMaterialTheme/PlainEnglish/StrengthAnalytics API35tests; macOSrunner attempts actual
  sharedNative simulator tests/framework for hostarchitecture. This is NOT an iOSapplication build.
  ExperimentalDRAFT alone skips oldfullAndroidmatrix; READY PR/main retain fullgate. Existing
  fullAndroid default guards/assertions/timeouts stay unchanged. OptionalRDD_FOCUSED_CLASSES is
  used ONLY by experimentaljob;15runnerhelpertests pass including cleanup/exit/defaultfullflow.
  Existing release10/capture23 pass. No final screenshots or hardware pass claimed.
- First checkpointCI43431989080 SUCCESS on actualGitHubmacOS/Android: Xcode26.6/arm64 compiled
  sharedUi iOSsimulator framework and ran sharedDomainNative tests; Android9focusedexisting
  MaterialTheme/PlainEnglish/StrengthAnalytics instrumentation tests passed. Nativeframework is
  NOT a completeiOSapp. No macOSinfrastructureblocker currently. Fullreleasegates not run yet.
- Second batch COMMITTED/PUSHED afc8e4ea822f88b524f6d3709e1ea538d757fc53: newsharedHealth KMPmodule, kotlinx-datetime0.6.2 exacttimestamp
  bridges, sharedtypedstaging/duplicate/conflict/authorization-window rules and11commonJVMtests.
  AndroidHeight/Weightreading now delegates to realsharedHealthAndroidreader; existinggateway
  retainslocking/historypermissionrecheck/DTOvalidation/transaction andweightmissingoffsetUTC.
  NativeHealthKitBodyReader uses realreadonlyHKqueries, cancellableboundedreads andauthorization
  sheet. AppleCOMPLETED doesnotmeanreadgranted; visible-onlyempty/nonempty snapshots CANNOT
  replace/import-delete retainedwindows. NativeHealthKitcode has NOT compiled/beenprovider-tested
  yet. No iOSapphost/entitlements/permissionusage strings yet. Preserve all uncommitted files.
- Currentverification: final `/workspace/scratch/kmp-health-policy-build.log` PASS160tasks/4m48s:
  11sharedHealthJVMtests, APK/testAPK/lint. Sixsnapshot and5pagedpermission/revocation/failure/
  bounded-read tests preserve existingpartialgrant behavior; SDKdriver delegates to commonpolicy.
  Body imports retain original1000pages/500records perpage limits, qualifiedsource IDs andUTC
  weightfallback. HC historypermission stillrechecked in originalgateway beforetransaction.
  FocusedAndroidCI nowincludes realBodyMeasurementsRepository/HealthConnectGateway/BodyMeasurements
  UI tests alongside prior9UIchecks. NativeHealthKitcode is NOT yet compiled; commit/push the
  coherent healthbatch toSAMEPR24 after fastchecks and inspect that exactCI, correcting real
  AppleAPIbinding errors rather than skips. Priorbaseline/domain/sharedUI checks remainvalid.
  Then continue remainingfullscope; no feasibility/demo substitute or prematuremerge/publication.
- Latest actualCI43435064307: AndroidPASS28focusedtests inclbodyrepository/healthgateway/UI;
  AppleFAIL only HealthKitBodyReaderNSDate constructors/category-propertybindings atlines131–137.
  AllApplehealthquery APIs compiled up to that error; no infrastructureblocker. LOCALfix now
  reuses existing kotlinx-datetimeDarwin toNSDate/toKotlinInstant instead of bespokeFoundation
  calls;2realiosDateinteroperability tests added. Do not rerun unchangedafc8 or weaken tests.
- Third batch LOCAL: SourceIdentity/PerformanceDistance converted to commonKotlin,2Java originals
  removed (202remaining). Retains Javaaccessors/constants, originalrecordequality/hash/text,
  exactwhitespace (validationdoesnotnormaliseIDs), immutableCORE order. OriginalJava versus
  convertedJVM10line identity/distance parity output cmpMATCH; originalhashes testedincommon.
  `/workspace/scratch/kmp-identities-build.log` PASS113tasks/2m47s:6sharedDomainJVMtests, all700,189
  legacydomainchecks, APK/testAPKcompile. Three newidentity tests retain invalidinput assertions.
  No lint/runtime/Nativepass yet for thisbatch. Preserve alluncommitted files.
- Exactnext: review/commit the NativeDatefix+realidentityconversion to SAMEPR24 after localformat
  checks; push and inspect new macOS/AndroidCI. UpdatePRbodyactualresults. Keepdraft until full
  agreedappscope complete; sharedframework alone is not iOSapp. Continue actualdomain/UI/
  storage/native-service migration after these concretecompilation failures are resolved.
- Remaining fullscope:202Javafiles to sharedKotlin; remaining Android-bound screens/ViewModels/
  navigation/data sources into one sharedapp; HealthKit and retainedHealthConnect adapters,
  cross-platformSQLite/backup/retention/permissions/recording/BLE/notifications; iOSapphost,
  genuine end-to-end feature parity and Android+iOSbuilds; final fullplatform/regression/visual
  gates before any version/release/merge. Current foundation is incomplete, not ready to merge.

## End-of-session handoff

Summarise what changed, file paths, branch/PR/commit, tests actually run and their results, remaining work, and any real-device checks still needed. If interrupted by a usage limit, leave a concise accurate checkpoint above so the one-line resume prompt works without guessing.
