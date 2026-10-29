# Continue Codex — 0.10.0-test1 Strength Programming 2.0

## Permanent recovery rules

Inspect actual status/diff/log, live main, feature branch, PR and CI before changing anything.
Preserve valid local/remote work. Do not reset, force-push, discard or recreate completed work.
Continue the active release below; do not revive earlier/deferred backlogs. Use a feature
branch and draft PR. Domain rules stay Android-independent; repositories share one database
and restore transaction. Prescriptions, actual sets, historical versions and athlete choices
remain distinct. Reuse Gym Now's one execution engine. No Internet permission, accounts,
backend, ads, analytics, subscriptions or runtime LLM. No physical-device claims without
actual human results. The app recommends. The athlete decides. The plan adapts.

## LIVE checkpoint — authored integration + verified substitution-test correction

- Branch feature/0.10-strength-programming; PR15 remains OPEN/DRAFT. Main c6299e2.
- Parent65340a86057bc647ca78ac746498fb286f25e160 is the canonical-capture bot from
  source52e2f9b/workflow40865813724: **364tests/API28,35,36 all green**, formatting,
  guidance and91canonical captures. The prior association race is fixed. These results
  certify that source, not the newer integration below.
- Implementation committed/pushed:5b80852d8c1f98de99f514c13552b9c5cea4250e.
  Workflow40927512770 completed370tests/API28,35,36: EACH had ONE failure in the new
  busy-equipment test (NPE at line129); all other369cases/API passed. Format/guidance/build/
  lint passed. Canonical capture publication was correctly skipped. Do NOT rerun this
  known-bad source unchanged or report it green.
  Preserve local/remote work; no reset/recreation. Final combined build
  authored-current-build.log PASSED :domain:check, app build, AndroidTest build and lint
  in6m55s.588,069domain checks/33suites (Today58checks),60schema,9release-note,
  11Dream source,14artwork,14formatter-harness,8capture-validator tests; pinned formatting
  59Kotlin/106Java; strict174canonical+14routine guides/177original assets all pass.
- Root cause was a test assumption: suggestions.first() need not use the same measurement
  profile. A bodyweight alternative legitimately clears the whole task, so dereferencing
  target.referenceLoadKg was invalid. Production engine behaviour is correct. The follow-up
  test explicitly selects Dumbbell Bench Press and asserts its complete target equals the
  original without the load reference (including non-null target and retained fractional
  RIR/rest). It then selects Push-Up, asserts a DIFFERENT measurement and a cleared task.
  Original prescription/source/current version/actual/history assertions remain. No timeout
  or assertion was weakened; both safety branches are now covered regardless of rank order.
- Correction source passed full domain/app/test APK/lint gate in2m21s
  (authored-swap-fix-build.log), pinned formatting59Kotlin/106Java, and the exact corrected
  real Android API28 case **1/1 PASSED53.513s** (authored-swap-fix-api28.log). Fresh emulator
  boot/package readiness verified first. App APK hash/size unchanged; test APK SHA256
  4d4280c65a6b742745f5ba2535c9862fba9d90159102300ef5773ac701179ca3.
  This follow-up must be committed/pushed and receive a NEW full matrix; the full0.10 scope
  remains unfinished. Inspect actual HEAD/CI on continuation before any changes.
- Gym Now now resolves typed current owner/revision, lists authored workouts, starts
  exact preserved nullable/RIR/reference targets through the SAME native coordinator,
  rejects/refreshed stale selection, and uses started authored settings for substitutions.
- CoachSnapshotReader injects typed current repository on IO. Authored StrengthTodaySummary
  preserves exact targets and explicit retained-version adherence/progression context.
  Strength Today, hybrid Home, read-only programme/block/workout details and calendar
  display authored programmes. No coercion into fake legacy integer-RPE prescriptions.
- New AuthoredGymNowScreenTest has6cases: exact planned actual60-vs-reference80, stale
  selection, preserved busy-equipment context/today-only swap, paused recreation without
  duplicate sets/auto-resume, normal Today/details, dark200% Today/details. All6passed.
- Focused local13selection reached **11passes** including all6new +2existingToday +3GymNow.
  Android SYSTEM aborted in existing openWorkoutSavesOnlyEnteredActualsAndPauseRequiresExplicitResume;
  final2cases incomplete. Do NOT claim13/13. Log authored-current-api28.log.
  Frozen prior-source103selection similarly aborted after101passes. Direct SwiftShader /
  Vulkan-disabled pinned37.2.12 had passed isolated5case class, but does NOT establish
  long-suite stability. Full accelerated API28/35/36 CI is required for this checkpoint.
  No test deadline/assertion/watchdog changed. Host OOM counters remain0. The first
  installation attempt found no package service after a system crash; emulator was
  restarted and boot/package readiness verified before the actual13case selection.
- APK0.10.0-test1/code13:53,635,997bytes, SHA256
  cc3ea418011088b52809c408a96530b418dfbb33b7870d726ceb2a10ee2ac843. Binary manifest has
  no INTERNET permission. Schema14/rootJSON9/strengthProgramming1/raw2/complete1 unchanged
  by this integration. No new dependency/artwork/catalogue/0.11 feature. No physical checks.
- Logs, build/runner scripts and recovery patch/new-file tar under /workspace/scratch/rdd010.
  The user repeated continuation while the final local cases were running; this was
  steering to keep working, NOT permission to discard/restart.

### Immediate next steps (authoritative)

1. Verify the pushed follow-up substitution-test correction and its NEW full workflow
   (370cases/API expected). Workflow40927512770 is a known370-case/one-failure matrix,
   corrected as above; do not blindly rerun that old source. Fix actual new failures. Keep PR15 DRAFT regardless of
   checkpoint green. Adopt canonical capture bot without discarding local feature work.
2. Continue feature-owned authoring orchestration + phone builder/template/generated preview,
   persisted drafts and explicit activation. Next files: new strength/StrengthProgrammeViewModel.kt,
   StrengthProgrammeBuilderScreen.kt, template/preview components and strengthGraph wiring.
   Use typed repository and SavedState only for draft IDs/small inputs, no Binder-sized JSON.
   Do NOT flatten generated dated/week-specific/hybrid prescriptions into one repeating
   draft week silently: StrengthProgrammeDraft has repeating Day definitions per block;
   preserve concrete generated previews when editing/confirming, explain structural changes.
3. Add safe future-only edit/regeneration, modest custom metadata UI (existing UUID custom
   definitions must retain history; no fake guidance), permanent-vs-today swaps with immutable
   versions/provenance and cleared incompatible references, and optional conservative modality
   target proposals. Review typed version-history presentation through the activated workflow.
4. Complete simulator quality issues honestly (beginner Power catalogue gap remains), full
   backup/migration/current+older/large history gates, authentic new builder/template/block/
   custom/permanent-swap captures, light/dark200%/TalkBack and release docs. No0.11 scope.
   Full agreed0.10 criteria are NOT done. Do not merge or mark ready early.

All following checkpoint sections are historical context; this live section overrides stale
pending/uncommitted/source claims below. Preserve the full remaining scope near the end.

## LIVE checkpoint — authored associations + production ticker fix; full CI required

- Latest code: `9c6d24165d0571d871744443e04a897a17403d2d` (exact authored Count/Keep).
- Preceding production fix: `95cefc05c188c8dc031da448b3fdb5d9fec61351` (terminal display
  ticker + deterministic original UI regression + PrimaryFocus backup9 assertion).
- Prior verified durability:974a349, handoff915f8b7. PR15 remains DRAFT. No merge.
- These two new code commits compile and have the results below. They are being pushed
  with this documentation checkpoint; inspect actual remote HEAD/CI, not stale notes.

### Why the prior matrix failed and what changed

Source915f8b7 workflow40856845725 FAILED after354cases/API: API28/36 each had one
PrimaryFocusRepositoryTest stale8-vs9 assertion. API35 also lost the second association
confirmation AFTER readiness/display, at helper127 assertIsEnabled.

Root code review identified a production race: openCompleted creates an empty coordinator
and keeps terminalView for display; a later coordinator.observe tick replaced that finished
projection with empty state, unmounting the review/dialog. GymNowViewModel.observeWorkoutTime
now only observes an unfinished displayed session under the existing mutex/busy checks.
The original GymAssociationScreenTest forces that REAL tick while each dialog is open.
All20sec/display/enabled/Count/Keep/history assertions remain. PrimaryFocus asserts9.
The earlier appearance-wait repair's green0483c96 run was not a permanent root-cause proof.

### New association implementation — preserve it

- Proposal preserves exactly one family: legacy Session OR authored Workout. The original
  six-argument constructor/readers remain; shared sessionId/date/title/count/review helpers
  preserve the exact frozen source. authoredProposals/acceptance require a nearby exact
  working task, correct current version and free target. No automatic association.
- GymAssociationJson writes authored decision2 and reads legacy1. Strict cross-family,
  future/unknown/coerced fields reject. StrengthProgrammingJson shares its frozen-workout
  codec instead of duplicating it. Root9/schema14/strengthEngine2 stay unchanged.
- SqliteGymAssociationRepository current/retained lookups, acceptance/reversal, SQL rollback,
  projection and backup validation support authored versions. Actual snapshots/history are
  never rewritten. Actual edits require fresh review while prior decisions remain frozen.
- GymAssociationCard displays exact nullable/fractional RIR/rest/load references using the
  shared target description. Existing legacy rendering/tags remain. New light and dark200%
  Compose reviews and6new repository/2additional JSON cases cover the path.

### Observed checks for9c6d241 (not full release acceptance)

- **588,049 domain assertions/33suites**, association suite658checks.
- Final domain/build/lint: authored-association-final-build.log, API28-safe fixture rebuild
  authored-association-api28-fixture-build.log both PASSED.
- Formatting57Kotlin/106Java,60schema and9release-note cases PASSED.
- `authored-association-api28.log`:103selected tests reached **101passes**. All new repo /
  JSON / light+dark200% review cases, PrimaryFocus correction and original association
  test with forced clock ticks passed. Android then aborted during GymNowScreenTest.
  openWorkoutSavesOnlyEnteredActualsAndPauseRequiresExplicitResume; final2cases incomplete.
  No test assertion failed before the abort. Do NOT claim103/103passed.
- Local emulator was aligned from37.3.6 to CI-pinned37.2.12/build16428233 (verified archive
  SHA1cd7362ea55dfb86a418958138dc396e74165dd01). Isolated5-case GymNow rerun passed3then
  hit the SAME system abort in that method, so binary alignment alone did not resolve it.
- Root diagnostic `pinned-system-crash.log` contains Android SYSTEM PROCESS stack:
  FinalizerWatchdogDaemon, SurfaceControl.finalize timed out10seconds in nativeRelease.
  Host cgroup has NO OOM. This is a system graphics-finalizer abort, not an app assertion;
  the underlying renderer/TCG reason is still not proven. Do not disable watchdogs or tests.
- Current emulator6017 uses pinned binary/swiftshader_indirect/TCG. Next: inspect actual
  processes, test a supported software-rendering fallback if useful, while requiring the
  full accelerated CI API28/35/36 matrix for the new head. CI renderer/matrix remains
  unchanged. Local103selection must be recorded honestly until completed.

Persistent logs/tools are under `/workspace/scratch/rdd010`: verify-authored-association.py,
verify-gym-now-pinned.py, authored-association-api28.log, gym-now-pinned.log,
pinned-system-crash.log and ci408568-summary.txt. Source backups also exist there.
No new runtime dependency/permission, no S24 checks. Existing79-case persistence source
974a349 DID complete before its code was committed (historical evidence below).

### Immediate next tasks

1. Fetch/check new pushed source and full CI. Fix actual assertions/root failures, never
   rerun known-bad915f8b7 expecting success. Keep PR15 draft regardless of checkpoint green.
2. Complete/localise the software-emulator graphics abort investigation where feasible;
   require actual accelerated API28/35/36 results. Preserve tests and final source.
3. Continue current-programme/Gym Now planned-start/source context/Today integration for
   authored programmes, then full builder/template/activation/regeneration/custom/permanent
   swaps and conservative modality controls below. No0.11 scope. The full0.10 UI is NOT done.

## Current recovery boundary — durability checkpoint, NOT release acceptance

- Branch: `feature/0.10-strength-programming`; PR **15 remains DRAFT**.
- Latest verified source checkpoint: `974a349d79dc0b5bf0f2929812fa4f3a6641facb`
  (`Persist versioned strength programmes and transactional authoring backups`).
- Implementation parent: `df4d9ffa5b9a693981da841899aef0074f8bfe1c` (91canonical captures
  from0483c96). Main remains `c6299e2401525197260f69c6918c89794cfa4c3d`.
- The interrupted schema14 / typed repository / backup9 work SURVIVED. No reset/discard
  occurred. The verified durability checkpoint is committed; inspect actual git log/current
  PR gates before proceeding. Do not recreate these source files.
- Version **0.10.0-test1 / code13**, schema **14**, JSON envelope **9** (reads1–8),
  strengthProgramming1. strengthEngine2 / strengthState2 / raw2 / complete1 unchanged.
- The environment restarted again during verification. Source, tools and build outputs
  survived, but /tmp logs/processes did not. Recovery copies and FINAL logs now persist
  under `/workspace/scratch/rdd010`. This source was rebuilt and instrumented afterwards.
- User-facing programme builder/template/activation/regeneration controls remain unfinished.
  No new activation controls have been exposed. Do NOT merge or describe0.10 as complete.

## Previously pushed gate / association-dialog failure — RESOLVED

`4180c28` run40784342654 passed API28/36 but API35 failed at the SECOND confirmation in
GymAssociationScreenTest: the dialog window/test tag had not registered before the click.
`57c46ec` waits for the actual dialog with the same20sec bound, then requires displayed +
enabled before clicking BOTH confirmations. All actual/history/association assertions remain.
Source `0483c96` **passed FULL workflow40786866429**, all API28/35/36 jobs, formatting,
source/domain/schema/guidance and canonical capture job. Bot `df4d9ff` adopted91captures.
No assertion/time limit was weakened and the failed test was not blindly rerun.
These results certify that pushed source, not the later persistence checkpoint.

## Completed committed0.10 foundation — preserve it

- StrengthProgramme immutable revisions, concrete dated blocks, nullable/RIR/fractional
  targets, frozen definitions; incomplete StrengthProgrammeDraft operations (edit/reorder/
  move/duplicate/delete workouts and blocks); CustomStrengthExercise metadata, no fake
  technique/progression. Authoring bounds52weeks/12blocks/12slots; generated previews26weeks.
- Nine original editable template structures: Full Body2/3, Upper/Lower4, PPL3/6,
  strength3, muscle4, running support2/3.
- Goal/experience-specific generator, real metadata/equipment/exclusion filters, favourite
  tie preferences, explained time budget and hybrid soft scheduling. Meaningful phases
  and conservative optional recovery/assessment. Short sessions reserve time for later
  required roles/pulling rather than exhausting the budget on early compounds.
- 2,430-configuration deterministic quality simulator; future-only protected regeneration
  and substitution proposals; substitutions clear load references and recalculate time.
- Exact authored adapter/source validator into the SAME GymExecutionEngine/source kind.
- Strict versioned programme/draft/custom JSON codec; six Android codec tests.
- StrengthTargetProgression: two recent comparable real sessions, recorded effort, exact
  task/load, enough working sets; optional load/rep/time/distance advice. Unknown effort,
  stale/future/warm-up/custom-only evidence never invents progression. Existing0.9 advice
  remains unchanged until new controls are connected.
- Original174canonical +14routine guidance untouched. Beginner power still has an honest
  catalogue gap (no POWER-primary beginner-friendly movement); do not relabel another
  movement or fake guidance just to satisfy a count. Resolve/document responsibly before
  final acceptance. No catalogue expansion for breadth.

## Verified persistence implementation — inspect actual source

New files:
- domain/.../strength/StrengthProgrammingRepository.java
- app/.../data/SqliteStrengthProgrammingRepository.java
- app/.../data/StrengthProgrammingArchive.java
- app/src/androidTest/.../data/StrengthProgrammingRepositoryTest.java (15 tests)

Changes:
- ProfileDatabase additive13→14: immutable revisions, normalized blocks/session rows,
  scalar current pointer, ordered decisions, split draft/header/block rows, custom metadata.
  Bounded UTF8 rows/FKs/unique dates and indexed lightweight summaries. No giant programme
  row or generic JSON catch-all. Existing schemas1–13 supported; no destructive fallback.
- Typed repository: draft CAS/idempotent retries, immutable explicit activation/stop,
  legacy transition/identity collision guards, fresh actual-linked protected sessions
  verified inside the write transaction, exact metadata/equipment/exclusion checks.
  customExercises is ONE payload query (N+1 removed). Normalized programme/draft/current
  reads use one transaction snapshot (included in final build and79-case Android pass).
  Completed-session lookup joins exact retained programme keys, not ambiguous prefixes;
  a real execution/activation/restore regression covers similarly named programme IDs.
- Custom metadata mirrors strength_exercises. Existing CoachingStore.saveExercise now
  updates typed metadata + logging definition atomically, rejecting measurement-task
  changes under the same typed ID. Otherwise the old rename UI could break backup
  consistency. Historical prescription snapshots are not renamed. Regression test added.
- Existing SqliteGymExecutionRepository/GymProgrammeStore now resolve exact authored and
  legacy sources/dates/finalisation links without coercing optional/RIR targets into fake
  legacy RPE. Competing current pointers rejected; old histories retained.
- Strict archive graph: lineage, activation ledger/current pointer, draft/custom refs,
  normalized row-size preflight, custom/logging agreement before replacement.
  SqliteBackupRepository includes strengthProgramming; BackupManager9 requires section,
  reads1–8; restore reconstruction uses the existing shared transaction. GymExportBudget
  includes new normalized rows;32MiB bound retained.
- Old-schema test fixtures drop14 children before simulating older databases.
- Added formatter targets and five schema tests. Docs updated for candidate14/9.
- Modern spontaneous Count/Keep has since been added in9c6d241 (see live checkpoint).
  Current-programme display/start and the builder still need complete UI integration.

## Final observed local verification — 2026-10-29 after environment recovery

All FINAL evidence is under `/workspace/scratch/rdd010`:
- `final-build.log`: **:domain:check, :app:assembleDebug, :app:assembleDebugAndroidTest,
  :app:lintDebug PASSED** after the final source edits. Domain33suites /587,615assertions.
- `api28-persistence.log`: **79/79 Android tests PASSED**,315.144seconds, software API28.
  Includes15new repository tests +64retained JSON/execution/backup/association/exclusion/
  CoachingStore/BackupManager cases. Runner verifies the exact installed APK SHA256.
- `schema.log`: **60passed**, including13→14 and all supported upgrade paths, late-DDL
  rollback, FK/uniqueness, UTF8 bounds, draft cascade and indexed scalar summaries.
- `format.log`: pinned Kotlin56 +Java105files pass. `release.log`:9cases pass.
- formatter harness14, Dream source11, artwork integrity14, capture validator8 pass.
  Bundled artwork integrity177assets; strict174canonical +14routine guidance unchanged.
- Final app APK SHA256: `0abaf5cb47c22851eeaf8442bad50940d02fe3c945884a7d22fb38d508b5d631`.
- No new runtime dependency or manifest permission. No S24 physical checks performed.

Failures were investigated and corrected, not hidden:
- New weighted-actual fixture initially selected a General/Beginner all-bodyweight workout.
  It now explicitly generates Strength/Intermediate, then a Muscle replacement; actual-set,
  fresh completed-session protection and historical assertions remain.
- The existing association backup test still expected8; it now asserts the new envelope9.
  All corruption/history/explicit Count/Keep assertions remain.79-case rerun passed.
- Lint flagged nested unbraced-loop indentation; braces fixed it without suppression.
- The old name editor could diverge typed metadata/logging mirror; writes are now atomic,
  with a regression preserving history and rejecting incompatible task changes.
- Local parallel build attempts caused OOM before final verification. Unused daemons were
  stopped, heap/workers constrained. The recovered final gate passed; do not count failed
  host ADB helper attempts or erased /tmp logs as current verification.

## Exact next tasks

1. Inspect the committed durability checkpoint and CURRENT full CI run after push. Require
   API28/35/36, build/lint/format/domain/schema/source/catalogue and canonical capture gates.
   The prior0483c96 green run does not certify this new schema14/backup9 source. Fix failures
   at root cause, keep PR15 draft, adopt the new capture bot without discarding feature work.
2. Continue application integration below: current-programme/Gym Now/Today display,
   feature-owned authoring
   orchestration, builder/template/preview/activation/regeneration and permanent-swap UI.
3. Keep this handoff updated at each verified phase. Do not start0.11, merge prematurely,
   relabel this checkpoint as a complete release or stop just because one gate is green.

## Remaining agreed0.10 scope (ALL still required where not completed above)

1. Current authored display/Gym Now planned start/source context/Today/calendar are integrated
   in the live checkpoint; finish full CI and review typed version/history navigation through activation. Exact explicit authored associations are implemented above. Preserve exact legacy
   versions and existing today-only substitutions; no incompatible target/load transfer.
2. Feature-scoped authoring coordinator/ViewModel and full phone-usable builder: name,
   goal, duration/blocks, frequency/days, equipment, workout names/notes, exercises/order,
   sets/reps or supported task, optional RPE/RIR/rest/reference, moves/duplicates/removal,
   draft save/reload, preview and explicit activation. No timing/business rules in Compose.
3. Editable template selection and meaningful generated preview: split/days/durations,
   exercises/targets, blocks, movement emphasis/trade-offs; edit/regenerate before accept.
   Future-only active edits/regeneration preserve actual-linked and historical sessions,
   old revisions and explanations; CAS prevents stale acceptance.
4. Modest custom-exercise metadata entry (name/body area/role/equipment/task/notes), logging
   and history, no fabricated technique or unsupported progression. Existing UUID custom
   exercises remain readable; new typed IDs use custom- prefix. Typed rename path is now
   protected but the UI still needs metadata controls and consistency review.
5. Permanent-vs-today swap UI with clear athlete choice; durable revision/provenance,
   exclusion/equipment/intent validation and cleared incompatible references. Preserve
   current quick equipment-busy/unavailable flows and original actual evidence.
6. Connect conservative modality progression to explicit future-target proposals without
   forcing increases; keep existing StartingLoad safeguards. Resolve goal/experience/
   power/support/time-budget quality gaps found by simulator, not cosmetic labels.
7. Realistic large strength-history/old backup/rollback coverage as needed; complete all
   supported API28/35/36 release gates, no Internet permission/dependency regression.
8. Authentic new builder/template/preview/blocks/custom/permanent-swap screenshots, light/
   dark/200%/TalkBack review, current canonical captures; update README/release/storage/
   architecture/TEST_BUILD/VERIFICATION docs to actual shipped behaviour. No mock UI.
9. Prepare PR15 for review only after full0.10 criteria/automated gates green. No stable
   production tag. Physical Galaxy S24 checks remain a separate honest manual checklist.

## Explicitly deferred0.11 — do NOT implement

Nutrition/protein, new height/weight intelligence, muscle-volume/PB dashboards,
achievements, intelligent deload/recovery, advanced analytics/hybrid, rest timer,
generated warm-up sets, supersets, major live-workout polish, catalogue expansion for breadth.

## Local environment recovery notes

SDK `/workspace/android-sdk`, full JDK17 `/workspace/jdk17`, Gradle cache
`/workspace/gradle-cache`, writable Android user dir `/workspace/android-user`.
local.properties (ignored) points to SDK. Default Java21 runtime lacks a javac binary;
standalone domain script uses its compiler module successfully. Builds need the full JDK.
Use JAVA_HOME=/workspace/jdk17, ANDROID_USER_HOME=/workspace/android-user,
GRADLE_USER_HOME=/workspace/gradle-cache, truststore `/etc/ssl/certs/java/cacerts` and the
configured proxy JVM properties; do not override HOME or disable TLS. For heavy local
builds use --no-daemon --max-workers=1, -Dorg.gradle.jvmargs='-Xmx1024m -Dfile.encoding=UTF-8'
and -Pkotlin.compiler.execution.strategy=in-process. Do not run multiple heavy builds.

No /dev/kvm. API28 AVD `/workspace/android-avd/recovery.avd`, software emulator launched
with ANDROID_I_WANT_MY_TCG=yes / -memory1024 / -accel off / -no-window. It needs network
sandbox permission for local sockets. Default adb cannot write ~/.android; local-only
Python adb_shell is installed under `/workspace/android-python` and speaks directly to
127.0.0.1:5555. Persistent runner `/workspace/scratch/rdd010/verify-persistence.py` prints APK SHA256 and streams
instrumentation results. Transport/read300s accommodates software package installation;
this is host tooling, not any relaxed Compose test deadline. Do not add these setup
packages or dependencies to the app. GitHub CI retains accelerated API28/35/36 coverage.

## Galaxy S24 checks — UNPERFORMED

Outdoor/locked/background/long GPS, poor GPS return, battery/reboot/process recovery;
real Health Connect providers/write/reimport/revocation; BLE HR/cadence hardware;
TTS/music/calls/audio focus; TalkBack/200% actual use; complete backup/restore on hardware.
