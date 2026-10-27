# Continue — 0.9 Architecture Hardening + Strength Engine 2.0

## Current repository and scope

- Branch: `feature/strength-engine-2-architecture-hardening`; **draft PR #14**:
  https://github.com/mmwiza1/Race-day-dreams/pull/14 . Do not merge before ALL scope passes.
- Live-main baseline fetched: `8b75755a0b74074047408b6444a11db3d6fef6f3` (0.8.2/code11).
  Main was not changed by this increment. Original local checkpoints183dae4/c1bfac4
  remain on `checkpoint/strength-engine-2-local-c1bfac4`; no work was discarded.
- Target **0.9.0-test1 / code12**. Current schema **10**, JSON **7**, raw archive **2**,
  complete package **1** unchanged. No unnecessary persistence bump for architecture.
- Latest fully verified phase: **Phase1 architecture hardening**. Phase2 generator fixes
  and Phase3 focus are committed; Phase4 Strength-first Today is implemented. The next
  Android run verifies their combined descendant after fixing two new UI test failures.
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

## Phases2–4 current checkpoint

Current local checkpoint2bf1aee59f322608ff42e5ea785f943fe1aae33c applies actual pinned
formatting to Phase4/metadata. Remote source083e8b05f01817ae2a36cdd946345bec21c54aaf
triggered40590307831. It built/linted but API28 aborted after11cases with
NoSuchMethodError:Stream.toList in StrengthTodaySummary. Fixed using Collectors/Collections;
an additional Android metadata-initialisation case checks all174 entries on eachAPI.
Expected Android count is now253 (252previous +1compatibilitycase), NOT claimed passed.
Publish the new compatibility correction and verify actual CI before accepting this gate.

Three modified files (do not discard):
- domain/src/main/java/org/racedaydreams/domain/strength/GymProgrammeGenerator.java
- domain/src/test/java/org/racedaydreams/domain/strength/GymProgrammeAllocationTestSuite.java
- app/src/main/java/org/racedaydreams/coach/application/GymProgrammeOperations.kt

Implemented: six start-anchored seven-day windows; explicit lower occurrence variants;
actual-exercise lower classification; same/before keyrun conflicts and softer after-run
penalties/context; truthful retained-exercise explanations and trunk-substitute tradeoffs.
No session removal, recurring availability change, schema/backup or progression change.
Full local suite **91,439checks/21suites** (new30,070) and43schema tests passed.
Formatter scope now includes generator/newSuite. An additional Android application test
checks retained-exercise classification and post-run recovery context (Android result
pending). Independent review reproduced all 91,439 checks / 21 suites and 43 schema tests.
Next: finish the combined format/Android CI, adopt fresh canonical captures and record
observed results. Do not claim Android pass yet.

Phase2 source4b58fb4 and pinned-format3709991 are committed. Phase3 source438ee5f is
committed. Phase3 adds19
domain checks (91,458 across22suites) and eight Android cases. Do not claim those Android
results until observed. The first Phase2 CI formatting job produced a real pinned-tool
patch; it was applied without weakening checks. Phase2 and Phase3 verification will run
on the combined descendant, with separate coherent source commits.
Typed PrimaryFocus uses the existing backed-up settings table; absent values migrate
logically from legacy starting.path. Strength enters the existing Strength dashboard
without a RaceGoal; Phase4 will then add a Strength-first Home. New Strength-only gym
setup defaults may be GENERAL/STRENGTH_FIRST; existing programme settings stay unchanged.
Focus changes only presentation. Preserve legacy race draft/resume and all destinations.
Phase4 now enters Strength-first Home on relaunch; Plan still opens the standalone gym
programme when no running profile exists. Both old and new typed settings round-trip.

Actual workflow40582975480 compiled/built/linted successfully, then ran249Android cases
on all APIs with TWO new UI failures. Their common cause was the focus-save snackbar
covering the next lower-page action; recreation re-showed the retained message. Focus
save now uses persistent Settings status, with a no-transient-message regression check.
All original assertions/timeouts remain. Rich fixture-only failure diagnostics were added.
The actual formatter patches from40582342809/40582975480 were applied. A new run is
required; do not describe this failed workflow as green.

Phase4: StrengthTodaySummary uses explicit retained actual/prescription/version links,
not date coincidence. Its domain tests add38checks. SnapshotReader prepares PB/coaching
history analysis on IO once per coherent refresh; Compose reads that projection. Home
shows today's gym work, independent logging, actual weekly completion, real PB evidence
and at most one evidence-backed recommendation. GymNow's full flow remains Phase5;
the current independent logging action works and no nonfunctional GymNow control ships.
New screenshot test requires actual StrengthHome light/dark/200% captures; all77old
captures remain required, plus3new =80. New2component cases+1capture case => expected252
Android tests, not yet claimed. Existing174guides/14routines/artwork remain unchanged.

Phase6 static metadata is implemented in3new pure-domain files, separately checkpointed:
ExerciseMetadata.java, ExerciseMetadataCatalog.java, ExerciseMetadataTestSuite.java.
Explicit174/174canonicalIDs, movement roles, broad primary/secondary areas, experience
suitability and OR-of-complete equipment setups; unknown/custom metadata stays unknown.
2,576newchecks. Existing catalogue, generator selection and measurements remain unchanged.
This is the metadata foundation, NOT completed browsing/custom exercises/substitutions.
Current local total94,072checks/24suites,43schema,9release,10formatterharness,7screenshot,
11Dreamsource,14artworkintegrity, strict174/174+14/14guidance pass.

## Next work

1. Run/fix actual pinned formatter + full API28/35/36 for current Phase4 descendant.
2. Adopt canonical80captures, inspect StrengthHome light/dark/200%, record actual results.
3. Implement Phase5 complete GymNow, using one typed reusable prescription/execution path;
   favourites/repeats/custom builder must work, no silent programme association. Plan new
   typed durability with the later Strength concepts before any schema/JSON bump.
4. Use reviewed Phase6 metadata for Phases8–13 exclusions/browse/custom/substitution;
   catalogue expansion is quality-gated, not a count target. Continue all remaining scope.

## Remaining full product scope (not implemented yet)

Phase5: all Gym Now paths without programme mutation. Phase3 focus/Phase4 Today are
implemented above; finish their verification rather than rebuilding them.
Phases7–11: quality-gated selective catalogue expansion, reversible exclusions/reasons,
hierarchical browse/search and typed
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

## Concrete next Gym Now design (read-only audit)

Do not use StrengthSession for an unfinished draft: it requires positive final duration
and actual sets, and CoachingStore immediately treats it as completed PB/coaching evidence.
Use one date-free immutable GymWorkoutDefinition with stable slots/frozen exercise+targets,
and typed GymExecution staging (PLANNED/QUICK/CUSTOM/REPEATED/FAVOURITE/GENERATED/FREE).
Repeat structure/reference evidence, never prior actual sets as newly completed sets.
Freeze exact programme version/session at start. Existing saveStrength(plannedDate) resolves
gym.current() at finish and is insufficient for live execution after a programme edit.
Finalise actual StrengthSession + retained explicit association + finished staging/events
atomically. Stable set IDs make retries idempotent; recovery must not invent elapsed gaps.

Proposed schema11 families (NOT implemented/published): strength_workout_versions/current,
strength_executions/sets/events, strength_exercise_exclusions,
strength_custom_exercise_metadata, strength_athlete_decisions. Dedicated typed codecs/repos,
not a kind-based bucket. Settle all durable families BEFORE distributing a schema11APK;
never silently extend a circulated schema version. JSON8 would require a validated
strengthEngine section while keeping reads1–7 and outertransactional restore. Current
8MiB envelope/depth/token bounds need conscious scale evaluation; raw2/complete1 stay.
Existing GymProgramme append-only versions remain supported (current56daylimit requires
explicit handling for later multi-block plans), templates can remain bundled, settings
can hold rest preferences; no speculative achievement table is needed for derived evidence.

Agent gym_execution_domain is preparing NEW pure-domain files as a separate Phase5 start.
Do not accidentally stage them into the Phase4 compatibility checkpoint. Preserve and
test them separately; models alone do not complete GymNow or justify visible controls.
