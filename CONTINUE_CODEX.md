# Continue — 0.9 Architecture Hardening + Strength Engine 2.0

## Current repository and scope

- Branch: `feature/strength-engine-2-architecture-hardening`; **draft PR #14**:
  https://github.com/mmwiza1/Race-day-dreams/pull/14 . Do not merge before ALL scope passes.
- Live-main baseline fetched: `8b75755a0b74074047408b6444a11db3d6fef6f3` (0.8.2/code11).
  Main was not changed by this increment. Original local checkpoints183dae4/c1bfac4
  remain on `checkpoint/strength-engine-2-local-c1bfac4`; no work was discarded.
- Target **0.9.0-test1 / code12**. Current schema **10**, JSON **7**, raw archive **2**,
  complete package **1** unchanged. No unnecessary persistence bump for architecture.
- Latest completed phase: **Phase1 architecture hardening, verified**. Phase2 generator
  fixes are implemented/tested locally and awaiting their own commit/Android gate.
- The full Strength standalone/programming/execution release is NOT complete. Do not
  confuse a green architecture checkpoint with completion of the requested product.

## Completed Phase1 checkpoint

Source `38fcc591024d1de858be16cd4c7d9860abb5ed34` passed complete workflow
[40578388101](https://github.com/mmwiza1/Race-day-dreams/actions/runs/40578388101):
**240 instrumentation tests EACH API28/35/36**, build/lint/test compilation, domain,
schema, source/provenance, strict174/174canonical+14/14routine guidance, real formatting
and77authentic captures. Exact Finished240lines inspected in jobs122451503560(API28),
122451503092(API35),122451503859(API36). Canonical bot2ea80d89b3dd695d01026a63e87fbde1a28b0932
was fast-forwarded locally. All77PNGstructures and current source fingerprint match:
`5b413ac959a755c6d514952d0f7feb67ea5343dca4d59c0f50ff650c3e069fde`.

Architecture decisions:
- CoachViewModel is lifecycle/draft shell; feature coordinators own commands/repositories.
  One coherent read model and operation runner remain intentionally shared.
- Success callback/message stays inside its serial publication Mutex. A controlled test
  proved unlock-before-callback allowed later snapshots to overtake it; original assertions
  remain. Destinations query state providers INSIDE composition, not cached graph creation;
  RunNow's nullable snapshot is deferred too. A realHomegraph5K->10K test guards reactivity.
- Seven SQL-owning repositories retain one ProfileDatabase and outer restore transaction.
  FeatureStore forwarders remain compatible; existing query/backup semantics retained.
- One recording service delegates session/solemetricsengine/atomicstaging, execution,
  cues and sensors. Capture batches/deepJSON on Main; serialIO only; cursors advance after
  commit. Fake/realSQLite tests cover retry/lostack/inflight/paused/partialexecution/backup.
- 36flat route contracts remain; no nested back-stack change. No unrelated UI redesign.
- Real ktfmt0.54 andgoogle-java-format1.24.0 AOSP check57scoped sources (33KT+24Java).
  Exact observed SHA256 locks are in config/formatting.json; cache metadata cannot override
  them. Temporary Maven checksum diagnostic was removed. Tools are build-only Apache2.0.

Before edits, baseline61,295checks/19suites +43schema +9release-note +11Dream-source,
14artwork-integrity and6screenshot-integrity tests passed; baseline mainCI40330082255green.
Phase1adds74purechecks(61,369/20suites),27Androidcases and10formatter-harness tests.
Actual newcaptures reviewed:Home,Plan,Strength,LiveCoach,Paris,Dream plusdark/200%Home;
no missingart, harmfulclipping or hierarchy regression found. See docs/VISUAL_REVIEW_0_9_0.md.

API35 architecture APK artifact12768139397,48,075,565bytes; captureartifact12768114597.
These are checkpoint artifacts, NOT the final complete0.9 release. Main's rolling release
remains0.8.2 until allStrengthEngine2work is complete/merged. No stabletag.

## Phase2 now ready for checkpoint

Three modified files (do not discard):
- domain/src/main/java/org/racedaydreams/domain/strength/GymProgrammeGenerator.java
- domain/src/test/java/org/racedaydreams/domain/strength/GymProgrammeAllocationTestSuite.java
- app/src/main/java/org/racedaydreams/coach/application/GymProgrammeOperations.kt

Implemented: six start-anchored seven-day windows; explicit lower occurrence variants;
actual-exercise lower classification; same/before keyrun conflicts and softer after-run
penalties/context; truthful retained-exercise explanations and trunk-substitute tradeoffs.
No session removal, recurring availability change, schema/backup or progression change.
Full local suite **91,439checks/21suites** (new30,070) and43schema tests passed.
Next: add generator/newSuite to formatter scope, commit separatePhase2, run/fix CI,
adopt fresh canonical captures and record observed results. Do not claim Android pass yet.

## Next work

1. Protect this documentedPhase1 checkpoint separately from Phase2 behaviour fixes.
2. Complete Phase2 format/build/affectedinstrumentation gate and checkpoint.
3. Phase3 typedtrainingfocus/onboarding/Settings: Running/Strength/Hybrid presentation,
   StrengthneedsnoRaceGoal, allfeaturesremain. Existing settingsbackups preserveALLstring
   keys, so a typed focusadapter can reuse that table without schema/envelopebump. Preserve
   record/race/hybrid legacydraft flows; no automatic programme priority changes.
4. Then Phase4 Strength-firstToday andPhase5 genuineGymNow, in coherent tested checkpoints.

## Remaining full product scope (not implemented yet)

Phases3–5: selectable Running / Strength / Hybrid priority, race-free Strength onboarding,
Strength-first Today and all Gym Now paths without programme mutation.
Phases6–11: exercise roles/body/equipment/experience metadata, quality-gated selective
catalogue expansion, reversible exclusions/reasons, hierarchical browse/search and typed
custom exercise metadata/opt-in generation.
Phases12–16: ranked intent-preserving substitution and busy-equipment choices; today-only
vs immutable programme versions; custom programmes and original editable templates.
Phases17–21: goal/experience-specific multi-block plans, volume balance, real power work,
preview/trade-offs and safe remaining-programme regeneration.
Phases22–25: live gym actual-set persistence, optional rest timer, evidence-backed optional
warm-up ramps and conservative supersets.
Phases26–30: measurement-specific optional progression, existing exact-exercise StartingLoad
safeguards, evidence-based optional recovery and today-only fatigue vs structural changes,
independent/priority-aware hybrid context. Audit dated progression overload: currently it
checks only latest evidence age, so stale second/future sessions may wrongly qualify.
Phases31–34: weekly actual muscle-group exposure, PB/history visuals, restrained real-evidence
achievements, programme/recent/favourite-first logging selection.
Phase35: typed/versioned additive persistence plus older/current transactional backups for
new user-owned concepts. Plan single justified schema/envelope bump after model design.
Phase36: thousands of deterministic programme-quality combinations across all preferences,
equipment/roles/time/frequency/start weekdays/favourites/exclusions/hybrid schedules.
Phases37–39: accessible real UX/captures, complete research/heuristic/architecture docs,
full build/lint/format/domain/schema/API28/35/36/backup gates, PR review and final merge
ONLY after full scope complete and green. Rolling test release; no stable production tag.


## Tooling / continuity

Working read-only `git fetch origin <branch> --no-tags` with sandboxed network access
successfully syncs bot captures. LocalSDK/adb is absent; default Gradle wrapper download
failed with JavaConnectException before compilation. Network-enabledcurl/gitpush tool
requests were interrupted before execution after long waits; do not repeat indefinitely.
Use connectedGitHub create_tree/create_commit/update_ref for publication with expectedhead
lease and exact treeSHA equality. Import remotecommit through
`/workspace/scratch/rdd-github-commit-import.py` and fast-forward refs without reset/discard.
Remote branch creation requires github_create_branch; update_ref does not create it.

The approvedvisual assets/courses/0.8.2engine remain intact. New durable Strengthconcepts
must use typed/versioned persistence, not Release08Store. Preserve all olderbackup reads;
plan justified additive schema/JSON bump only when new models require it. Later audit
StrengthProgressionAdvisor's datedoverload (stale second/future sessions currentlymay
qualify): fix inPhase26 with proper regressions, not silently during architecture.

All S24physical GNSS/BLE/TTS/music/calls/battery/recovery/backup/TalkBack/sunlight checks
remain **UNPERFORMED**. NoInternetpermission/backend/account/analytics/ads/subscriptions/
LLM dependency added. The app recommends. The athlete decides. The plan adapts.
