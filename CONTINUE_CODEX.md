# Continue — Architecture Hardening + Strength Engine 2.0

## Repository and release boundary

- Repository: mmwiza1/Race-day-dreams, /workspace/Race-day-dreams.
- Branch: feature/strength-engine-2-architecture-hardening; draft PR #14:
  https://github.com/mmwiza1/Race-day-dreams/pull/14 . Do not merge before ALL scope passes.
- Live-main baseline8b75755a0b74074047408b6444a11db3d6fef6f3 remains0.8.2/code11.
  Current target0.9.0-test1/code12. Do not restart or reset to main on continuation.
- Schema10 / JSON7 / raw2 / complete1 unchanged. No Internet/runtime dependencies added.
- Last fully verified SOURCE: 171a2108197f8ce2dc3a68a8a6bb7e4df633c997.
  Canonical capture bot: 5220799a9efd53a97e90b546a2d44a04e26aead5. Inspect actual HEAD
  and fast-forward status before editing; preserve subsequent uncommitted phase work.
- Phases1–3 verified; Phase4 Strength-first Today implemented/verified (full GymNow is
  Phase5). Phase6 canonical metadata implemented/verified. Full release NOT complete.

## Last complete gate and visual review

Workflow40595241800 SUCCESS:
https://github.com/mmwiza1/Race-day-dreams/actions/runs/40595241800
- 255 instrumentation tests EACH API28/35/36. Exact Finished255 logs inspected in
  jobs122509582203 /122509582007 /122509582052.
- Build, lint, domain/schema/source/guidance and actual pinned formatting all passed.
  Formatting job122509581631 checked36Kotlin+44Java (80sources), both actual modes,
  including bounded byte-stable formatter convergence; all14harness cases passed.
- 80authentic captures (all77old + StrengthHome light/dark/200%) published by the bot
  and fast-forwarded. StrengthHome light/200% were opened; controls/wrapping retained.
  VISUAL FAILURE FOUND: home.png captured global Loading despite green tests. New
  current-scenario loaded/content guards reject this; fresh capture/review is REQUIRED.
  Do not claim the 171 screenshot set passed the visual gate. Source digest:
  6be033098cdaad7b0808cfd2711375958c6ae4ab771841838afaadd0af3a7e6e.
- Previous sourcee9768ff/workflow40592585867 passed253cases each and its80captures were
  reviewed: StrengthHome light/dark/200%, RunningHome, Plan and annualParis2026 showed
  no harmful clipping, missing art or hierarchy regression. See docs/VISUAL_REVIEW_0_9_0.md.
- API35 checkpoint APK artifact12777070406:48,239,405bytes, screenshots12775783471.
  These are draft0.9 checkpoint artifacts, NOT a complete StrengthEngine2release.
- All S24 physical outdoor/BLE/TTS/music/calls/battery/recovery/backup/TalkBack checks
  remain UNPERFORMED. Rolling main release stays0.8.2; no stable tag or merge.

Verified Phase5 foundation:94,241checks/25domain suites;43schema;9release notes;
14formatter harness;11Dream source;14artwork integrity;7screenshot-validator tests.
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

## Phase5 domain checkpoint — verified, NOT full GymNow

Seven files now checkpointed as Phase5 domain foundation (preserve them):
- domain/.../strength/GymWorkoutDefinition.java
- GymExecution.java, GymExecutionSet.java, GymExecutionEngine.java, GymWorkoutAdapter.java
- domain/.../strength/GymExecutionTestSuite.java
- app/src/androidTest/.../data/GymExecutionAndroidTest.java (2cases, passed all supported APIs)

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
original harness cases plus4new ones =14pass locally; actual pinned-tool CI passed above.
This prevents repeated real google-java-format long-string normalisation patch rounds.

NEXT EXACT TASKS:
1. Preserve the uncommitted cross-week lower-body scheduling correction in
   GymProgrammeGenerator.java and GymProgrammeAllocationTestSuite.java. The previous
   search considered within-week spacing but could allocate Monday then Tuesday across
   programme windows although Wednesday was available. The fix retains requested
   sessions, penalises the previous planned day separately from completed evidence, and
   explains unavoidable consecutive planned dates. Its320newchecks passed locally in the
   preceding work (94,561total/25suites); the resumed run is rechecking them. It has not
   passed a new full CI gate yet. Commit and verify this correction separately.
2. Strict v1 GymWorkoutJson/GymExecutionJson codecs and Android malformed/round-trip tests
   are in progress. Do not stage unfinished codec files with the scheduling correction.
   These codecs do not yet provide a repository, migration, backup or UI feature.
3. StrengthProgressionAdvisor now rejects stale second and future supporting sessions;
   inclusive28day boundary, effort/optionalincrease/actualimmutability have20newchecks.
4. Strict codec files are complete with12newAndroid cases; source validator has60domain
   checks and1Android case. Hybrid Today/Strength Plan coherence adds2Android UI cases.
   Local aggregate94,641checks/26suites; expected270Android cases awaits actual CI.
   Obtain pinned formatting, full supported-API gate and fresh80captures. Inspect Home
   specifically for the loading-screen race; retain all original screenshot requirements.
5. Implement full Phase5 typed durability/coordinators/GymNow screens with all six paths,
   then continue ALL remaining product phases below. No fake buttons or copied actuals.

## Concrete GymNow durability design — not implemented

StrengthSession requires positive final duration and actual sets; CoachingStore treats
it as completed PB/coaching evidence. Do not put unfinished/empty drafts there. Use typed
GymExecution staging. Origins PLANNED/QUICK/CUSTOM/REPEATED/FAVOURITE/GENERATED/FREE converge
on one engine. Finish actual StrengthSession + retained exact association + finished
staging/events atomically; stable set IDs support retries. Existing saveStrength(plannedDate)
resolves gym.current() at finish, so it is insufficient for frozen live-source associations.
Recovery must explain recorded timing/gaps, never pretend uninterrupted exercise.

Typed staging implementation is now in progress but NOT committed/published: schema11
settles workout_versions/preferences and executions/sets/events only. Later durable
families receive explicit schema12+ migrations; NEVER silently extend a circulated11.
Do not invent future table layouts merely to reserve them. Dedicated typed repositories/
codecs, not Release08Store. Normalised append-only bounded rows avoid CursorWindow limits;
summary queries exclude actual/event payloads. Exact programme source and stable IDs are
required for atomic finalisation. Retain repeat-source evidence independently of deleted
original actuals. No unfinished draft is completed coaching/PB evidence.
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
