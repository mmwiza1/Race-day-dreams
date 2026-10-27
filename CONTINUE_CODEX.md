# Continue — Architecture Hardening + Strength Engine 2.0

## Repository and release boundary

- Repository: mmwiza1/Race-day-dreams, /workspace/Race-day-dreams.
- Branch: feature/strength-engine-2-architecture-hardening; draft PR #14:
  https://github.com/mmwiza1/Race-day-dreams/pull/14 . Do not merge before ALL scope passes.
- Live-main baseline8b75755a0b74074047408b6444a11db3d6fef6f3 remains0.8.2/code11.
  Current target0.9.0-test1/code12. Do not restart or reset to main on continuation.
- Schema10 / JSON7 / raw2 / complete1 unchanged. No Internet/runtime dependencies added.
- Last fully verified SOURCE: e9768ff3c13800dfdb8915f3b2ad273d5643f30b.
  Canonical capture bot c1fa18dd9d3319857764a1c1b7c43419f1d49a47 was fast-forwarded locally.
  The next checkpoint records documentation/formatter work above that bot commit.
- Phases1–3 verified; Phase4 Strength-first Today implemented/verified (full GymNow is
  Phase5). Phase6 canonical metadata implemented/verified. Full release NOT complete.

## Last complete gate and visual review

Workflow40592585867 SUCCESS:
https://github.com/mmwiza1/Race-day-dreams/actions/runs/40592585867
- 253 instrumentation tests EACH API28/35/36. Exact Finished253 logs inspected in
  jobs122500333538 /122500333740 /122500333523.
- Build, lint, domain/schema/source/guidance and actual pinned formatting all passed.
  Formatting job122500333497 checked36Kotlin+37Java (73sources), both actual modes.
- 80authentic captures (all77old + StrengthHome light/dark/200%). Integrity passes.
  Fingerprint0c67ec5c9cef6acac61f86829ad1a1b6e0fa5c4cf7aa37707e1867fd3b667d5b.
- Opened StrengthHome light/dark/200%, RunningHome, Plan, annualParis2026. Primary
  Strength logging action remains visible at200%; lower details scroll normally; no
  harmful clipping, missing art or hierarchy regression. See docs/VISUAL_REVIEW_0_9_0.md.
- API35 checkpoint APK artifact12774623099:48,190,253bytes, screenshots12774418148.
  These are draft0.9 checkpoint artifacts, NOT a complete StrengthEngine2release.
- All S24 physical outdoor/BLE/TTS/music/calls/battery/recovery/backup/TalkBack checks
  remain UNPERFORMED. Rolling main release stays0.8.2; no stable tag or merge.

Local verified Phase4/metadata:94,072checks/24domain suites;43schema;9release notes;
10formatter harness;11Dream source;14artwork integrity;7screenshot-validator tests.
All geographic generation checks reproduce. Strict174/174guides +14/14routines and
177exercise/20Dream assets remain intact. Existing tests/assertions were not removed.

## Architecture and behaviour already completed — preserve

- CoachViewModel is lifecycle/draft shell with feature coordinators/application services;
  one coherent read model + one serial operation runner remain deliberately shared.
- Callback and message publication stay inside the same Mutex after snapshot publication.
  Cached NavHost destinations read state providers INSIDE destination composition;
  RunNow snapshot is deferred too. Existing route contracts/back-stack retained.
- Seven SQL-owning scoped repositories share ProfileDatabase and outer backup transaction;
  FeatureStore compatibility forwarders remain. No ORM/database rewrite.
- ONE authoritative recording service delegates session/metrics/staging, execution, cues
  and sensor coordinators. Capture immutable batches on Main; serial IO only; advance
  point/event cursors after atomic commit. Lost acknowledgements/inflight samples/recovery
  tests retain real evidence. No automatic tracking restart after reboot.
- Default gym clock resolves device timezone at each operation; injected clocks stay fixed.
- Phase2: six start-anchored seven-day programme weeks; explicit lower-session occurrence
  variation (squat/hinge); actual retained lower exercises drive fatigue context; same/before
  demanding run conflict and softer after-run penalty; truthful retained-work explanations.
- Phase3: typed PrimaryFocus RUNNING/STRENGTH/HYBRID over existing backed-up settings.
  Missing preference resolves legacy starting.path without rewriting it. Focus changes
  preserve goals, drafts, plans and history. Strength onboarding needs no RaceGoal/profile.
  New standalone gym setup defaults GENERAL/STRENGTH_FIRST; stored settings never change.
- Phase4: Strength Home and race-free Plan access; explicit actual/session/version links
  for adherence; no date-coincidence completion; PB/coach projection prepared on IO.
  Existing independent logging works. Full GymNow action not shipped before its flow works.
- Phase6: ExerciseMetadata + explicit version1 registry for all174canonical IDs: primary/
  other movement roles, broad primary/secondary areas, experience suitability and OR-of-
  complete equipment setups. Unknown/custom metadata stays unknown. Generator selection,
  catalogue IDs, historical measurements and all original artwork remain unchanged.

## Diagnosed failures (fixed, never weaken tests)

- Earlier architecture callback/navigation snapshot races fixed; source38fcc59 passed
  240tests EACH API in40578388101. Incorrect imports/checksum-sidecar issue fixed earlier.
- Phase3 workflow40582975480 ran249tests but2newUI cases failed: focus confirmation
  snackbar covered the next action; rotation replayed it. Persistent Settings focus status
  replaces the snackbar. Original assertions/20s waits retained; fixture diagnostics added.
- Phase4 workflow40590307831 passed252cases API35/36 but API28 aborted after11cases:
  Stream.toList is unavailable there. Replaced with Collectors/Collections; new Android
  registry-initialisation case covers all174entries. Subsequent253gate above passed allAPIs.
- Production domain must use API28-safe Immutable/Collections/Collectors. No Java9
  List.of/Set.of/Map.of/copyOf or Java16 Stream.toList. JVM tests alone do not prove this.

## Current next checkpoint — Phase5 domain, NOT full GymNow

Seven NEW files prepared by gym_execution_domain (preserve them):
- domain/.../strength/GymWorkoutDefinition.java
- GymExecution.java, GymExecutionSet.java, GymExecutionEngine.java, GymWorkoutAdapter.java
- domain/.../strength/GymExecutionTestSuite.java
- app/src/androidTest/.../data/GymExecutionAndroidTest.java (2cases, Android pending)

169newchecks; aggregate94,241checks/25suites and43schema tests passed locally.
Definitions are date-free immutable revisions/stable slots/frozen exercises+targets.
Engine owns actual sets separately, idempotent saves/finish, manual changes, monotonic
pause/resume, explicit interruption gaps and partial completion. Programme source binds
exact identity/revision/session; repeats never copy old actuals. Corrupt event replay and
completion state are validated. Source codecs must also cross-check retained prescriptions.
NO storage/coordinator/UI wiring yet: do not describe models as a working GymNow flow.

Formatter follow-up is in ONLY scripts/format-code.py, scripts/test-format-code.py,
docs/FORMATTING.md. Actual pinned --apply and isolated patch generation require byte-stable
output within4passes, otherwise fail. Plain --check and checksum locks unchanged. All10
original harness cases plus4new ones =14pass locally; actual CI verification pending.
This prevents repeated real google-java-format long-string normalisation patch rounds.

NEXT EXACT TASKS:
1. Commit formatter/docs checkpoint separately above c1fa18d. Add all7new gym files to
   config/formatting.json, checkpoint Phase5 domain separately, publish using safe protocol.
2. Run actual formatter (apply bounded stable patch), compile + supported API gate for
   new engine smoke tests (expected255Android cases). Do not claim a pass before observed.
3. Adopt matching80canonical captures; preserve all previous screenshot requirements.
4. Implement full Phase5 typed durability/coordinators/GymNow screens with all six paths,
   then continue ALL remaining product phases below. No fake buttons or copied actuals.

## Concrete GymNow durability design — not implemented

StrengthSession requires positive final duration and actual sets; CoachingStore treats
it as completed PB/coaching evidence. Do not put unfinished/empty drafts there. Use typed
GymExecution staging. Origins PLANNED/QUICK/CUSTOM/REPEATED/FAVOURITE/GENERATED/FREE converge
on one engine. Finish actual StrengthSession + retained exact association + finished
staging/events atomically; stable set IDs support retries. Existing saveStrength(plannedDate)
resolves gym.current() at finish, so it is insufficient for frozen live-source associations.
Recovery must explain recorded timing/gaps, never pretend uninterrupted exercise.

Proposed schema11 families (NOT published): strength_workout_versions/current,
strength_executions/sets/events, strength_exercise_exclusions,
strength_custom_exercise_metadata, strength_athlete_decisions. Settle programme/block
persistence and ALL durable families before circulating a schema11APK; never silently
extend a circulated schema version. Dedicated typed repositories/codecs, not Release08Store.
JSON8 would require validated strengthEngine data while retaining reads1–7 and outer
transactional restore. Existing8MiB/depth/token bounds need conscious scale evaluation.
Keep raw2/complete1 unless a justified independently versioned change is required.
Legacy GymProgramme versions remain supported (current56day limit needs explicit handling
for multi-block plans); templates can be bundled; rest preferences can use typed settings;
derived achievements need no speculative table. No durable change has been made yet.

## Remaining complete user scope (do not silently reduce)

Phase5: full GymNow planned/quick/build/repeat/favourite/generate flows; typed persistence,
backup, real execution UI and explicit optional plan association. Phase4 hybrid Today
also needs both same-day disciplines surfaced; Strength-first Plan behaviour with a
retained running profile should be made coherent without losing Running access.
Phases7–11: quality-gated selective catalogue growth (fewer if quality cannot be met),
reversible exclusions/reasons, hierarchical browsing/search, typed custom exercise
metadata/explicit opt-in generation. Do not redo the174covered metadata or artwork.
Phases12–16: intent-ranked substitution/busy2–4options/nonmedical discomfort language;
today-only vs immutable programme changes; custom programmes and editable original templates.
Phases17–21: goal/experience-specific multi-block programming, muscle-volume balance,
genuine power work, running support, previews/trade-offs, safe remaining-programme regeneration.
Phases22–25: durable live gym actual-set entry, optional rest timer/+30/skip/audio/haptics,
optional evidence-backed warm-up ramps, conservative supersets.
Phases26–30: conservative measurement-specific progression, exact recent StartingLoad,
optional deload/recovery, today-only fatigue vs structural changes, priority-aware hybrid.
Known dated StrengthProgressionAdvisor overload checks only latest evidence age: reject
stale second and future sessions with proper regressions. Summary currently prefilters both.
Phases31–34: actual muscle-group working-set analytics, actual1/3/5/6/8/10RM vs estimated1RM,
restrained real-evidence achievements, programme/recent/favourite-first logging.
Phase35: typed/versioned additive migrations and older/current complete backup tests for
all new user-owned data. Phase36: thousands of deterministic quality combinations across
roles/equipment/goals/experience/time/frequency/startdays/preferences/hybrid, no pathologicalplans.
Phases37–39: accessible full UX, authentic new Strength screens, research-vs-original
heuristic docs, full build/lint/format/domain/schema/API28/35/36/backup gates, review then
merge ONLY when entire scope is complete/green. Verify main rolling test build afterwards.

## Tooling and publication

No local Android SDK/adb; wrapper download fails before local compile. Android results are
observed GitHub CI, not physical tests. Read-only git fetch with network additionalpermission
works (last0.86s), followed by ordinary ff-only merge; preserve uncommitted new files.
Network curl/gitpush requests previously stalled before execution; do not repeat indefinitely.
Connected GitHub create_tree/create_commit/update_ref works. Stage intended files ONLY,
assert remote treeSHA == local git write-tree, expected-head lease update, import exact
public commit via /workspace/scratch/rdd-github-commit-import.py then fast-forward refs.
Never reset/discard other phase work. Remote branch creation needs github_create_branch.

All physical S24 checks remain UNPERFORMED. No account/backend/ads/analytics/subscription/
LLM/Internet permission. The app recommends. The athlete decides. The plan adapts.
