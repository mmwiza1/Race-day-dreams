# Continue — Architecture Hardening + Strength Engine 2.0

## Repository and release boundary

- Repository: mmwiza1/Race-day-dreams, /workspace/Race-day-dreams.
- Branch: feature/strength-engine-2-architecture-hardening; draft PR #14:
  https://github.com/mmwiza1/Race-day-dreams/pull/14 . Keep draft and do not merge until
  the ENTIRE requested Strength Engine 2.0 scope and all delivery gates are complete.
- Main baseline remains 8b75755a0b74074047408b6444a11db3d6fef6f3, version0.8.2/code11.
  Feature target:0.9.0-test1/code12. Do not restart or reset to main on continuation.
- Last fully verified SOURCE:35664996a188653d06a2eb573004a50c33430c92.
  Canonical bot7997dca03351b46e5598922b2ee6d3df7176fbca was fast-forwarded locally.
- Latest typed durability source checkpoint:7f0738cd0a6b710318dc7f14120e2579eb5c2583.
  It is committed locally but awaiting publication/format/full CI at this handoff edit.
  Inspect actual HEAD, status and remote before proceeding; preserve all completed work.
- Current source schema11 / JSON envelope8 / strengthEngine section1 / raw archive2 /
  complete container1. The last fully verified source above still used schema10/JSON7;
  the new durability formats require their own observed supported-API gate.
- Phases1–3, Strength-first Today/Hybrid coherence and canonical metadata are verified.
  Phase5 domain/codecs/source validation are verified; typed repository/backup code is
  implemented but its Android gate is pending. Full Gym Now UI is NOT implemented.
- No Internet permission/runtime dependency/account/backend/analytics/ads/LLM added.

## Last full gate and actual visual review

Workflow40598574304 SUCCESS:
https://github.com/mmwiza1/Race-day-dreams/actions/runs/40598574304
- 270 instrumentation tests passed on EACH API28/35/36. Exact Finished270 lines were
  inspected in jobs122521183424 /122521183354 /122521183323 respectively.
- Build, lint, domain/schema/source/guidance and actual pinned formatting passed.
  Bounded formatter convergence retains the pinned tools/checksums;14harness cases pass.
- All80authentic captures were adopted from bot7997dca and passed local integrity.
  Source fingerprint72e991bb52b062ddcd93a717aaa46a1f1e19b06dccdce9bc79465027db32ab96.
- Opened actual Home, StrengthHome light/dark/200% and annualParis2026 images. Home is
  fully loaded, correcting the earlier171a210 Loading capture defect. Strength controls
  and wrapping remain readable, lower details scroll, and no harmful clipping, missing
  artwork or hierarchy regression was observed. A partial multiplexed image display
  was a viewer-output issue, not corruption in the stored PNG. See visual review docs.
- API35 APK artifact12778150145:48,272,173bytes; screenshots artifact12776884991.
  These are draft feature checkpoint artifacts, NOT a complete StrengthEngine2release.
- All GalaxyS24 outdoor/BLE/TTS/music/calls/battery/recovery/backup/TalkBack checks remain
  UNPERFORMED. Main rolling release stays0.8.2; no stable tag or merge.

Local current checks:94,641domain checks/26suites;49schema;9release notes;
14formatter harness;7screenshot-validator;11Dream source;14artwork integrity cases;
strict174/174canonical+14/14routine guides. All177exercise/20Dreamassets remain intact;
manifest/attribution checks match. No original test/assertion was removed.

## Completed architecture and behaviour — preserve

- CoachViewModel is a lifecycle/draft shell with feature application coordinators;
  one coherent read model and one serial operation runner remain deliberately shared.
- Callback/message publication stays inside the same Mutex after snapshot publication.
  Cached graph destinations read providers INSIDE composition; RunNow selection is
  deferred too. Existing route/back-stack contracts remain.
- Seven scoped SQL repositories share ProfileDatabase and the outer backup transaction;
  FeatureStore compatibility forwarders remain. No ORM or database replacement.
- ONE authoritative run service delegates session/metrics/staging, execution, cues and
  sensor coordinators. Main captures immutable batches; serial IO commits them atomically;
  point/event cursors advance only after commit. Lost acknowledgements/inflight samples
  retain evidence. No automatic GPS restart after reboot.
- Default gym Clock resolves device timezone per operation; injected clocks stay fixed.
- Programme allocation uses six start-anchored7day windows and distinct lower-session
  squat/hinge occurrences. Actual retained lower-body exercises define fatigue context;
  same/before demanding runs conflict, after-run context is softer. Explanations describe
  retained work. Planned lower-body spacing now crosses week boundaries; unavoidable
  consecutive planned dates are explained separately from completed evidence.
- Typed PrimaryFocus RUNNING/STRENGTH/HYBRID uses existing backed-up settings and legacy
  starting.path interpretation. Focus preserves goals, programmes, history and drafts.
  Strength setup needs no running profile/RaceGoal. New standalone setup defaults to
  GENERAL/STRENGTH_FIRST only when no current programme exists; old settings stay intact.
- Strength Today uses explicit actual/session/version links, not date coincidence, and
  IO-prepared PB/adherence/coaching projections. Independent logging works. Hybrid Today
  now surfaces both same-day disciplines; Strength Plan stays gym-first even with a
  retained running profile. Running remains accessible.
- ExerciseMetadataCatalog v1 explicitly covers all174canonical IDs: roles, broad areas,
  suitability and OR-of-complete-AND equipment setups. Unknown custom metadata remains
  unknown. Catalogue IDs, historical measurements, original artwork and current generator
  selection remain intact.
- Dated StrengthProgressionAdvisor now requires BOTH supporting comparable sessions
  recent and nonfuture, with an inclusive28day boundary. Existing exact exercise/load,
  top-rep, known-effort, two-session and optional-small-increase safeguards remain.

## Verified Phase5 foundation — not the complete Gym Now product

GymWorkoutDefinition, GymExecution, GymExecutionSet, GymExecutionEngine and
GymWorkoutAdapter preserve date-free immutable revisions, stable slots, frozen exercise
and target definitions, actuals independent of prescriptions, idempotent stable IDs,
manual changes, monotonic pause/recovery and partial-completion evidence. Repeats begin
with empty actuals. All origins converge on one engine; only an explicitly started PLANNED
execution can propose its preserved programme association.

Strict v1 GymWorkoutJson/GymExecutionJson/GymJsonSupport decode exact fields and types,
explicit nulls, bounded arrays/structure, valid event replay and frozen historical exercise
definitions without current-catalogue reconstruction.12Android codec cases and a source
validator case passed in the270gate. GymWorkoutSourceValidator has60domain checks and
validates retained programme/saved/repeated sources; an identity hash alone is insufficient.
Legacy programme IDs/targets can be proven, but old exercise names were not stored and
cannot be independently authenticated. The newly frozen definition remains preserved.

## Typed durability checkpoint — implemented, supported-API gate pending

Source7f0738c adds domain GymExecutionRepository and SqliteGymExecutionRepository, strict
GymAcceptedActualJson, schema/backup integration and real Android fixtures/tests. It has
not passed its new full CI gate yet. Expected293Android cases =270existing +12repository
+11backup cases; never report this expectation as an observed pass.

Schema11 has exactly five families: strength_workout_versions,
strength_workout_preferences, strength_executions, strength_execution_sets and
strength_execution_events. Future exclusions/custom metadata/programme-block concepts
must use explicit schema12+ migrations; never silently extend a circulated11.

Repository semantics:
- One unfinished gym stage, revision-checked atomic saves and identical lost-ack retries.
- Normalised append-only sets/events;256KiB UTF-8 bound per workout/header/set/event/
  accepted-actual row. Summary queries do not hydrate actual/event histories.
- Atomic final actual StrengthSession + exact original PLANNED association + terminal
  staging. Unfinished stages never count as completed coaching/PB evidence.
- Original prescription and execution evidence remain immutable. An explicit later actual
  edit keeps accepted current actual/editedAt separately; PBs use current actual history.
  Explicit actual deletion removes its terminal stage, retaining prior repeat-template
  proof. It cannot silently delete an unfinished execution.
- Repeat-source proof remains independently retained after the original actual is deleted.
  Historical sources are validated without reconstructing through today's catalogue.
- Recovery preserves retained timing/actuals and records gaps; never claim uninterrupted
  exercise or resume secretly after reboot.

Envelope8 requires independently versioned strengthEngine1, retaining reads1–7. Validate
all source/actual/edit/association graphs before the shared outer replacement transaction;
refuse replacement while a local unfinished gym stage exists. Complete container1 and raw2
remain unchanged. Bounds are32MiB/depth32/2milliontokens/1MiBserialisedstring. Export must
fail clearly before destination truncation. The450workout/2250set fixture above the old
8MiB limit is an Android test awaiting execution; no peak-heap measurement is claimed.
See docs/STRENGTH_STORAGE_2.md for the detailed data contract and TEST_BUILD.md for the
unchecked physical acceptance list. There is still NO full GymNow/live gym coordinator/UI.

## Diagnosed failures — fixed without weakening tests

- Architecture callback/navigation snapshot races and early import/checksum errors were
  fixed; source38fcc59 passed240cases each in40578388101.
- Focus-confirmation snackbar covered the next action/replayed on rotation in40582975480.
  Persistent Settings status replaced it; original assertions/20s waits remain.
- Stream.toList caused API28 NoSuchMethodError in40590307831. API-safe collections and a
  real174-entry registry runtime case fixed it; later supported-API gates all passed.
  Production code must avoid List.of/Set.of/Map.of/copyOf/Stream.toList on API28.
- The171a210 screenshot set passed PNG checks but Home captured global Loading. Current
  activity/content readiness and negative Loading guards fixed this;3566499's fresh actual
  Home PNG was opened and is fully loaded. Integrity alone is not visual acceptance.
- Actual pinned formatting can normalise long strings over multiple passes. Apply/patch
  generation now requires byte-stability within4passes;14harness cases and real CI pass.

## NEXT EXACT TASKS

1. Inspect HEAD/status/remote; preserve7f0738c and all documentation work. Publish using
   the expected-head lease, obtain actual pinned formatting if needed, and run the new
   repository/backup/schema/build/lint/API28/35/36 gate. Expected293 is pending, not passed.
2. Inspect every failure and fix root causes. Validate old envelopes1–7/current8,
   all schema1–10 upgrades, current full-backup rollback and real actual edit/delete
   semantics. Review the measured large-fixture result without inventing peak heap.
3. Adopt matching80canonical captures and review affected screens; update actual gate/
   artifacts/counts in this handoff and VERIFICATION.md. Keep PR14draft/fullscope boundary.
4. Wire typed repository through application composition/coordinators and implement the
   full Phase5 GymNow planned/quick/build/repeat/favourite/generate entry flows and one
   live actual-set execution UI. Existing saveStrength(plannedDate) resolves gym.current()
   at finish; use the new preserved source for live finalisation instead. Success follows
   committed writes; prescription values must never prefill themselves as completed sets.
5. Continue ALL remaining user phases below; do not stop at storage/interfaces or treat a
   green foundation checkpoint as the completed release.

## Remaining full user scope — do not silently reduce

Phase5: finish all six GymNow entry paths, custom/favourite/repeated definitions, explicit
optional plan association, durable execution/recovery and fully working UI. No fakebuttons.
Phases7–11: selective quality-gated exercise growth (ship fewer rather than incomplete
art/guidance), reversible exclusions/reasons without PB/history loss; hierarchical body/
movement/equipment browsing and favourites/recent/programme/excluded shortcuts; alias-
relevance search/filters; custom exercises with measurement metadata and explicit generator
opt-in. Preserve all174existing complete guides and14routine movements.
Phases12–16: intent-ranked substitutions with reasons/explanations/equipment/experience/
exclusions/favourites; busy-equipment2–4options; cautious discomfort wording; today-only
versus immutable-programme substitutions with incompatible loads cleared; custom programmes
and editable original FullBody/UpperLower/PPL/Strength/Muscle/RunningSupport templates.
Phases17–21: stable goal/experience-specific multi-block programming, proper strength reps/
rest, approximate muscle working-set balance, genuine eligible power work, running-support
coverage, accessible general default, preview/trade-offs and safe remaining-programme
regeneration. Completed prescriptions/actuals remain immutable; current legacy56day limit
needs an explicit new-model/adapter decision for multi-block programmes.
Phases22–25: live actual load/reps/time/distance/RPE/RIR entry with previous evidence and
optional starting references; immediate local saves; optional rest timer/+30/skip/manual/
audio/haptics; evidence-backed editable/skippable warm-up ramps excluded from PB/working
volume; conservative accessory supersets with execution/rest grouping.
Phases26–30: measurement-specific conservative progression while preserving exact recent
StartingLoad safeguards; optional evidence-led recovery/deload without mandatoryweek6;
today-only fatigue versus accepted structural versions; RunningFirst/Balanced/StrengthFirst
hybrid recommendations without one discipline silently overriding the other.
Phases31–34: weekly actual working-set muscle analysis/comparisons; actual1/3/5/6/8/10RM
separate fromestimated1RM; history/progression charts; restrained real-evidence achievements;
programme/recent/favourite-first logging selection.
Phase35: extend typed persistence through explicit later migrations as features require,
retain all supported backup readers and transactional full-backup graph validation.
Phase36: thousands of deterministic programme-quality combinations across goal/experience/
priority/frequency/time/equipment/days/running/exclusions/favourites, enforcing role/volume/
power/complexity/measurement/timebudget/equipment/raceweek invariants and no pathologicalplans.
Phases37–39: accessible complete Strength UX/authentic new screens; original-heuristic vs
research documentation; all format/lint/domain/schema/API28/35/36/source/artwork/backup gates;
verify Strength-only requires no race, Running-only no gym and Hybrid coherent context.
Review and merge ONLY after fullscope complete/green; then verify main and rolling testAPK.
No permanent production/stable tag and no unperformed S24 pass.

## Tooling and safe publication

No local Android SDK/adb; wrapper download fails before local compile. Android results
are actual GitHub CI, not physical. Read-only git fetch with network additionalpermission
works, followed by ordinary ff-only merge; preserve uncommitted files. Network curl/gitpush
requests previously stalled before execution; do not retry indefinitely.
Connected GitHub create_tree/create_commit/update_ref works. Stage intended files ONLY,
assert remote treeSHA equals local git write-tree, expected-head lease update, import exact
public commit with /workspace/scratch/rdd-github-commit-import.py, then fast-forward refs.
Never reset/discard another phase. Branch creation needs github_create_branch.

The app recommends. The athlete decides. The plan adapts.
