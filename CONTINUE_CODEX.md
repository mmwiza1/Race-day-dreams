# Continue — 0.9.0-test1 (reduced scope is authoritative)

## Release boundary

Headline: Architecture Hardening + Standalone Strength + Gym Now + Exercise Discovery,
Exclusions and Smart Substitution.

Repository /workspace/Race-day-dreams, branch feature/strength-engine-2-architecture-hardening,
draft PR14. Keep draft until ALL retained scope and automated gates are complete. Do not
restart/reset from main. Main remains 8b75755a /0.8.2. Feature0.9.0-test1/code12.
Inspect actual git status/HEAD/remote before continuing; preserve all valid work.

## Retained 0.9 scope — finish these only

- Architecture hardening already implemented; feature ownership, one shared SQLite DB,
  one authoritative run recorder and deterministic gym execution.
- Existing gym allocation/variation/fatigue/explanation bug fixes.
- Running/Strength/Hybrid focus and Strength-first Today without a running goal.
- Gym Now six entry paths plus open workouts; existing live actual-set execution,
  saved workout definitions, favourites/repeat, actual/prescribed separation and recovery.
- Existing174complete exercise guides/metadata, discovery/search/personal shortcuts.
- Reversible explicit exercise exclusions, honoured by generation and recommendations,
  preserved in backups without losing favourites/history/PBs.
- Explained busy/unavailable equipment alternatives and today-only swaps; complete only
  already-active permanent substitution/association storage needed for correctness.
- Current conservative progression and exact-exercise starting-load safeguards.
- Required typed storage/migrations/backups, Route Story/API28 reliability, accessibility,
  authentic current captures, documentation and automated release/CI gates.
- Preserve stable completed functionality, including legacy custom-exercise logging.
  Do not expand it or make users depend on unfinished deferred features.

## Explicitly deferred (not release blockers and not active implementation tasks)

Full custom programme builder; large template library; multi-block/mesocycle system;
major Strength/Hypertrophy/Power engine redesign; advanced experience differentiation;
rest timer; generated warm-up ramps; supersets; catalogue growth toward220–250;
new custom-exercise features; advanced progression; weekly muscle-volume analytics;
major PB redesign; achievements; intelligent deloads; advanced hybrid decision engine.
Do not begin these after retained0.9acceptance. Earlier larger-backlog instructions are
superseded by the owner's scope reduction. Existing completed stable pieces stay intact.

## Actual source and verification

Route projection source bce6486 publishes recorded points/route/pauses/story as one
prepared detail projection. Prior5993a2d batches city artwork and readiness atomically,
with a real15-city-switch regression. No stored GPS points are discarded or altered.

Latest FULL GREEN source f21af7b/workflow40684717270:311tests EACH API28/35/36 plus
build/lint/format/guidance/canonical publication. Canonical bot4fdf7d1 supplies86captures,
fingerprint2f827237087c2a1b78b8342e410a3e419c893df26973b1b679b9684f15641ba6.
API35APK12813944987:49,140,525bytes. Do not call later failed workflows fully green.

Subsequent gates:
-231b93d/40689616914:API35/36 pass314,API28 artwork-readiness timeout.
-5993a2d/40691943309:API28/35 pass315;API36 missing RouteStory despite1801retained
 points and valid durability. Original20s/assertions retained.
-bce6486/40694457701:API28/36 pass315;API35 artwork-ready timeout. cf76ea6
 observes owning unmerged readiness semantics (same20s/assertions), pending new gate.
Required current capture set is88, including new exercise-browse and busy-equipment
screens. New set must be fully captured, adopted and visually reviewed before acceptance.
All physical GalaxyS24 checks remain UNPERFORMED.

## Association storage checkpoint — preserve and finish

Backend and documentation are being committed together; inspect current HEAD/status.
Schema12 adds strength_association_decisions + indexed strength_association_targets.
Typed SqliteGymAssociationRepository implements stable-ID retries, explicit keep/count/
reversal, retained provenance, immutable reviewed actual/target snapshots, atomic coaching
projection, legacy-link migration, deliberate actual deletion and history-safe actual edits.
Domain GymAssociationRepository + GymWorkoutAssociation exist; strict GymAssociationJson1
has3Android tests. Association UI is not enabled yet.

StrengthEngine section2 exports decisions/current choices and reads old section1 under
unchanged JSON envelope8. Raw archive2/complete container1 unchanged. Full graph validation
precedes destructive replacement. No actual-row cascade FK may silently erase decisions.
Seven new real Android repository/migration/backup cases added; NEW gate pending.
Local domain201,295checks/30suites and53schema cases passed. New backend Android is NOT
verified yet. All174/174canonical+14/14routine guides remain intact.

## Next exact tasks in order

1. Confirm cf76ea6 owning-semantics readiness fix on APIs28/35/36 with the backend;
   original assertions/timeouts and stored points are retained. No blind retries.
2. Validate/commit/push current typed association backend coherently with documentation.
   Run build/lint/format/domain/schema and real migration/backup/rollback/retry tests.
   Current expected test count322=315existing+7new, not an observed pass.
3. Complete only the already-active association feature: saved spontaneous workout offers
   Keep as extra / Count toward reviewed prescription, exact pending retries and source
   snapshots; edits refresh actual evidence; no automatic date-based completion.
   UI must be enabled only after backend/restore gate passes.
4. Implement reversible explicit exclusions separately from legacy archive/hide choices;
   enforce generation and substitution eligibility, preserve PB/history/favourites, add
   typed/versioned persistence and older backup readers. Keep actual source IDs intact.
5. Harden retained features and accessibility; capture/review all88current authentic
   screenshots plus exclusions if added. Update release notes/README/TEST_BUILD/VERIFICATION.
6. Run complete supported matrix, old/current backup round-trips and no-INTERNET check.
   Only then make PR14ready and prepare merge per owner instruction. List only genuine
   remaining S24hardware checks. No stable production tag; no deferred0.10feature work.

## Tooling

No verified local Android SDK/adb. Actual Android evidence is GitHub CI. Local pinned
formatters work (scripts/format-code.py); offline domain/schema/release scripts work.
Git fetch/push with sandbox network permission work; never force/reset unrelated changes.
Feature CI publishes matching canonical screenshots through a bot; fetch/merge them before
pushing if branch advances. Runtime app has no INTERNET/account/backend/analytics/LLM.
