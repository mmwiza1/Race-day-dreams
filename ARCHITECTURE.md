# 0.10 in-progress programme authoring boundary

New Android-independent StrengthProgramme/Draft models preserve dated blocks, frozen
GymWorkoutDefinition targets, future-only revision proposals and exact source adaptation
into the existing GymExecutionEngine. Original generation/templates and task progression
remain preview policies. StrengthProgrammingJson reuses the strict frozen-workout codec.
Schema14 / JSON backup9 add a typed authoring repository over the shared helper. Programme
headers, blocks and bounded session rows are separate; indexed summaries do not hydrate
session payloads. Normalized detail reads share a transaction snapshot. Draft CAS, explicit
activation/stop decisions, fresh completed-session protection and immutable versions share
one write transaction. Legacy and authored planned executions use the same source validator
and GymExecutionEngine. Logging-name edits keep typed custom metadata and its mirror atomic.
Backup graph validation precedes transactional replacement; envelopes1–8 remain readable,
raw2/complete1 unchanged. Reviewed modern spontaneous Count/Keep uses exact authored snapshots and decision payload2,
with legacy1 readers. Builder/activation and current-programme UI integration remain
unfinished. Completed history is a terminal projection; unfinished-only clock observations
must not replace it with an empty coordinator view.
See docs/STRENGTH_PROGRAMMING_0_10.md and CONTINUE_CODEX.md for current scope and gates.

The following sections record the preserved completed0.9 architecture.

# Current retained 0.9 application status

Feature-owned orchestration, standalone training focus, Gym Now, typed association and
exclusion persistence, exercise discovery and today-only substitution are implemented.
Application/capture source f3a988a passed workflow40741473316:331tests per API28/35/36 with
build/lint/format/migration/backup/guidance and91authentic captures. See VERIFICATION.md for
source-bound results and S24 limitations. The final documentation commit adds no app or
fixture inputs. The original larger Strength Engine2.0 roadmap is explicitly deferred.
Existing Compose family is aligned on UI/runtime/foundation1.8.3/Material3 1.3.2 (Apache2)
for framework layout/focus stability; this does not change the data or coaching contracts.

# Architecture

## 0.9 application boundaries — behaviour-preserving checkpoint

Feature-owned application coordinators now orchestrate running plans/evidence, Strength,
progress, baseline, health and settings/portability. `CoachViewModel` retains lifecycle,
SavedStateHandle draft protection and compatibility entry points. `CoachOperationRunner`
serialises commands and publishes one coherent `CoachUiState` through `CoachSnapshotReader`;
features do not create competing snapshots or duplicate core training rules.

```text
CoachActivity + feature destination registrations + presentation screens
  -> feature application coordinators / one command runner + coherent read model
  -> Android-independent domain policies and narrow repository contracts
  -> scoped SQL adapters / shared ProfileDatabase / one transaction pool
```

`FeatureStore` remains a compatibility composition facade. Activity, result, shoe,
baseline, settings, planned-workout and backup adapters own their actual SQL. Existing
recording import hooks and whole-backup operations retain the shared transaction.
No schema or backup-format change is required for these extractions.

Navigation ownership is split by Home, running, Strength, Progress and Settings while
retaining the original flat routes and Up/pop behaviour. Graph registrations accept
state providers and read them inside destination composition; caching a plain snapshot
would freeze later updates. RunNow snapshot selection is deferred for the same reason.
Each command publishes and delivers its success callback before releasing the shared
operation lock, preventing a queued feature from replacing that callback's snapshot.
Native recording keeps one
foreground service; execution events, cue arbitration and optional sensor wiring have
injected/testable internal coordinators. The extraction preserves Main-thread engine
mutation and serial-IO incremental atomic staging, with cursors advancing after commit.

These are implementation boundaries, not a claim that Strength Engine 2.0 product phases
are delivered. See CONTINUE_CODEX.md for the current gate and exact remaining scope.

## Typed gym durability — 0.9 data foundation

`GymExecutionEngine` owns the Android-independent execution state; a narrow
`GymExecutionRepository` and `SqliteGymExecutionRepository` persist it through the existing
shared database. The additive schema 11 contract has five families: immutable workout
revisions, saved preferences, execution summaries/headers, entered set rows and event
rows. Revision-checked writes append actual evidence atomically, and finalisation commits
the actual workout and its exact explicit programme association with the terminal stage.
An unfinished stage never becomes completed training/PB evidence. Summary reads do not
hydrate set/event histories; rows have an explicit 256 KiB bound.

Original prescriptions and execution evidence stay frozen. An accepted later edit retains
its current actual and timestamp separately; PBs use current actual history. Exact source
validation never resolves historical definitions through today's exercise catalogue.
Recovery retains observed time and records interruption gaps. Envelope 8 adds independently
versioned strength data while reading envelopes 1–7; validation precedes the existing
outer restore transaction. Raw archive 2 and complete container 1 remain unchanged.

This increment is a data foundation with its own pending verification gate, not a shipped
Gym Now/live gym interface. Later durable Strength concepts require explicit subsequent
migrations; schema 11 must not be silently extended after distribution. See
[Strength storage](docs/STRENGTH_STORAGE_2.md) for the complete contract and limits.

## Architecture history and retained contracts

## Incremental modernization decision

The original branch used Java 17, programmatic platform Views, a small IO
controller and SQLiteOpenHelper. Its independent domain module and tested
onboarding/persistence behavior remain valuable and are retained.

New application presentation uses Kotlin, Compose, Material 3, Navigation,
ViewModel, SavedStateHandle and StateFlow. The existing goal form is embedded
with AndroidView, so adopting Compose does not require rewriting working goal
validation or date/pace handling. Derived eligibility is passed explicitly into
that form; its legacy stored Boolean is a compatibility field, not authority.

Google Now in Android's Architecture Learning Journey was used as an engineering
reference for unidirectional data flow and layer boundaries. This app does not
copy its module graph, network architecture, synchronization framework or code.

```text
Compose screens / retained onboarding View
                   ↓ user actions
CoachViewModel + immutable StateFlow + SavedStateHandle
                   ↓
Android-independent Java models, evidence/quality policies and calculators
                   ↓ repository contracts
SQLite feature/profile repositories ← provider-neutral Health Connect adapter
                   ↓
application-private local database
```

The domain contains no Android, Compose, Health Connect or JSON imports. Kotlin
is introduced at Android integration/presentation boundaries. Java domain logic
is not rewritten for style. Disk work and provider reads run off the main thread;
ViewModels own operation state across rotation. No training rule lives in a UI.

## Coaching test-build layers

`domain.coaching` now implements original deterministic generation, bounded
constraint scheduling, raw-evidence adaptive proposals, execution feedback and
session-RPE context. `domain.strength` holds extensible exercises, ordered session
sets, explainable classification and reproducible full PB progression. No Android
or storage dependency enters these rules.

`CoachingStore` implements the Android-independent `CoachingRepository` boundary and shares the existing application-private helper and transaction
boundary. Plan saves atomically insert an immutable version, update the one
current pointer and mirror prescriptions for existing reminders. Accepted
proposals store the decision and resulting version atomically. Rejection preserves
the current version. `RunExecution` references the preserved prescription version;
provider activity payloads are not overwritten to add that link. Health Connect
imports and source deletions remain governed by the existing repository.

`CoachViewModel` coordinates IO and publishes actual state. Explicit user actions
create/rebuild plans or resolve proposals. Read-only loading may record a proposal
but never applies it. UI screens show near/distant certainty and the actual reasons
computed by the engine. Plan, Strength and feedback screens extend the existing
navigation without replacing the working onboarding/data features.

Backup's optional `coaching.version=1` section is typed and graph-validated before
any destructive writes. Restore uses the existing transaction, including all
coaching tables, and checks that reminder prescriptions match the current plan.
Older backups without coaching remain supported. Imported activity references
can await re-import; raw provider measurements are still excluded from export.

## Persistence decision and migration

Room was evaluated as the eventual structured-store adapter. It is deliberately
not introduced during this simultaneous UI and data-model expansion. Reusing the
existing SQLite store keeps one transaction boundary for profile, activity,
backup/restore and user annotations and preserves its tested non-destructive
corruption handling. Repository contracts permit a later Room migration without
changing evidence or training calculations. A Room change needs its own schema
comparison and data-migration tests; it must not use destructive fallback.

Schema 1 → 2 is explicit and additive; schema 2 → 3 transactionally copies the
annotation table to allow absent note overrides. Schema 3 → 4 adds immutable coaching plan versions, one current pointer, proposals/decisions, strength definitions/sessions, run responses and feedback snapshots. Both previously published
draft schema-2 variants upgrade without a reset. Stored null, empty and text
notes, RPE and shoe links are preserved; ambiguous old empty notes are never
reinterpreted by guesswork. Unsupported upgrade paths fail before any changes. Existing profile, goal, availability and
unfinished draft rows are retained. New tables represent activity payloads,
source identities, annotations, shoes, results, derived efforts, baseline state,
assessment records, checkpoints, settings, planned workouts and future
plan/decision metadata. Variable sample/lap payloads are versioned by their
containing codec rather than coupled to Health Connect classes. Distance codes
and metres are stored independently, allowing additional PB distances.

Repositories share an application-scoped helper. Transactions stage complete
imports, source deletions, derived efforts and the next checkpoint together.
Provider updates retain manual annotations and shoe assignments. Annotation notes
use null for no app override and empty text for an intentional clearing, separate
from provider payload. A source delete
invalidates its derived efforts; manually entered results remain user-owned.

Unreadable files are retained by an explicit corruption handler. Only confirmed
local-data reset closes the shared pool and deletes the database/journals.
Migration, malformed data or failed writes never silently create a new profile.

## Evidence and analysis

ActivityRecord stores explicit Instants and optional reported offsets, nullable
metrics, provenance, cumulative samples, actual laps and optional planned/shoe
links. Moving duration is distinct from elapsed time. PlannedWorkout and actual
activity remain separate. A future Wear OS companion must use these shared
prescriptions rather than duplicate phone training logic.

BaselineEngine reports what available records support, with per-field provenance,
recorded gaps and confidence explanations. Manual corrections preserve their
explicit review timestamp. Performance eligibility is recomputed from current
evidence and ages out; neither reload nor restore refreshes old manual evidence.
Session-average pace is not labelled easy without effort evidence.

Split and rolling-effort calculators require time/distance samples. They reject
invalid ordering and impossible jumps and do not cross long gaps or pauses.
Uncertain candidates are not automatically awarded as PBs. Calculations are
linear in sample count for each requested distance; PB history is reproduced
from retained results rather than discarding former records.

## Portability, reminders and future boundaries

Backups are user-chosen JSON files with a versioned, validated envelope. Replace
restore validates the complete document before a single database transaction;
unsupported merge semantics are rejected. Raw imported Health Connect records,
samples, heart rate and live checkpoints are excluded. App-owned annotations,
manual records and derived PB metadata can survive independently of reimport.

Evening reminders use deterministic local-time policy plus an inexact Android
alarm adapter. Delivery rereads the current planned workout, revision and
completion state. Moves, replacements, cancellations, permission changes and
quiet hours invalidate stale messages. Preferences default to disabled.

Native recording and GPX are implemented in the Prompt 1 increment described below.
Route maps and Wear OS remain future boundaries. Race predictions are described below. Original
production plan generation, single-workout proposals and bounded weekly/multi-week review rules are implemented. A
boundary is not an operational feature. The training engine and whole-plan
regression harness must grow together; no pretend generated plan is displayed.

Backup envelope version 5 retains coaching and optional strength-programming data and prevents old app versions
from silently discarding it. This reader also restores original version-1 and version-2
backups. Core store format 1 and coaching section version 1 are independently
validated. Export retains the existing 8 MiB/complexity limits: exceeding them
fails clearly before truncating a destination, without deleting local history.


## Schema five / 0.4 increment

`WeeklyCoaching` and `LongRunDurability` retain Android-independent deterministic
rules. `DevelopmentStore` adds a typed, kind-keyed evidence table for manual and
optional provider weights, historical strength records, weekly reviews/decisions,
interruption state, routine strength links and explicit session decisions. The
4→5 migration is additive; all supported 1–4 installations upgrade explicitly.
`development.version=1` is validated before transactional replacement, alongside
the retained coaching graph. Envelope 3 prevents old readers silently dropping
new user-owned evidence; readers still accept envelopes 1/2. Provider weight
payloads are omitted from export and can be re-read.

Compose uses the existing ViewModel snapshot and IO mutex; weekly evidence is
recomputed on evidence refresh, not every destination composition. Calendar view,
mass unit persists locally; obsolete provider preferences normalize to Health Connect. The retained tested native
onboarding wizard now uses compact weekday selections/card choices, disclosure
and a date picker; it is intentionally not rewritten merely to use Compose.
The final explicit Create my programme action saves the goal then generates.
Manual move/skip and review acceptance create immutable plan versions and update
reminder mirrors transactionally. Weight has no input path into coaching.

Health Connect is the supported adapter; unused enhanced-provider dispatch has been removed. Legacy provider preferences normalize on upgrade/restore.

## 0.5 strength and optional programming

Schema 6 adds exercise choices, PB acknowledgements, append-only gym versions and a
current pointer. Forward migrations retain old data and normalize removed provider
preferences. Backup envelope 4 includes these choices/plans and reads versions 1–3.
Restore validates a captured typed snapshot before a transactional replacement.

Measurement profiles distinguish load/reps, reps, duration, load/duration, distance
and load/distance. Canonical kg/metres/seconds survive display changes. Old JSON
without measurement fields decodes with original repetition semantics. Sessions and
manual PBs retain their recorded definitions and source/date precision.

GymProgrammeGenerator, GymPlanEditor, StrengthProgressionAdvisor, ComparableRuns,
WorkoutHistoryQuery and HomeCoachingSummary are Android-independent. Compose shows
these decisions; explicit runner action alone persists a changed prescription.

## Prompt 1: Health Intelligence and native recording

Schema 7 is additive from every supported 1–6 version. HealthIntelligence exposes
source-specific, coverage-aware trends; its optional HC cache is separate from goals
and coaching decisions. RunMetricsEngine owns deterministic trace/time rules. The
foreground service owns sensors and serialized five-second staging batches, never
Compose. RecordingStore finalizes canonical activity/PB data in the same transaction
as staging state. Raw points/events remain retained; process recovery finalizes only
observed time. Standard BLE and step detector adapters implement sensor boundaries.
GPX uses Android XML infrastructure. Explicit optional HC totals write-back uses
stable client IDs and excludes own-origin import. See docs/RUN_ENGINE.md for limits.

JSON backup envelope 5 still reads 1–4. The companion version-one ZIP archive streams
user-owned raw routes/sensors/events with SHA-256 verification and transactional merge.
This is explicitly a two-file backup workflow; a unified whole-app container is not
claimed. Restore refuses removing retained user-owned recordings or an active stage.

## Prompt 2: one recorder, service-owned execution

WorkoutExecutionEngine and CoachCues are Android-independent projections over accepted
metres/active time from RunMetricsEngine. Plan segments are preserved/flattened; the
executor never generates a training plan. The foreground service owns execution and
publishes immutable UI state. CoachAudio implements an injectable cue-sink boundary
using local platform TTS, temporary audio-focus ducking and optional vibration.
No second race/free/interval location implementation exists.

Schema 8 adds only coach_support (execution snapshots, routine completions, stable
Dream Route assignments). Forward paths from 1–7 retain existing tables. Staged
points/events and execution snapshots flush in the same transaction. Finalization
links the exact plan version and retains typed step evidence in RunExecution; later
plan changes do not reinterpret targets. Run Score compares qualified work segments
against their targets, excluding recovery-inclusive overall averages.

History uses metadata summaries, one activity-detail query and at most 12 recent runs /
60,000 native points for coaching refresh. Excluded detail means insufficient evidence,
not invented durability. Whole-history database paging remains future work.

JSON envelope 6 carries support records and reads 1–5. Raw archive 2 includes execution
snapshots and reads format 1. CompleteBackup format 1 nests app.json/recordings.zip
with SHA-256/length manifest, bounded extraction to private temporary files and one
outer SQLite transaction. A conflict or invalid inner document rolls back both stores.

## 0.8 presentation and app-owned data

SemanticTiles supplies restrained light/dark surfaces. Today and Run Now sit over the
existing service and WorkoutExecutionEngine, including explicit open-step completion.
CustomWorkout, StartingLoad, RoutePresentation and TrainingInsights are independent Java
rules. Release08Store stores typed, versioned definitions, origins, decisions, preferences
and checklist rows. Schema9 is additive. JSON7 reads1–6; complete package1 and recording
archive2 remain. DiagnosticReport has a small allow-list and no database dependency.
Route rendering loads selected activity samples on IO and projects off-main; visual
sampling never changes raw observations. No new runtime dependency or Internet permission.

## 0.8.1 history and durable checkpoint changes

Schema10 adds compound run-history and computed strength-time indexes; no user
records or backup-envelope fields change. WorkoutHistoryStore seeks by numeric
time/ID/kind in bounded SQL batches, filters local dates and exercise aliases on IO,
and returns light row projections. A preserved plan-version link controls workout
type classification. Numeric cursor bounds are explicitly cast for API28 SQLite
UNION affinity. Calendar List separately seeks30 recorded-run summaries at a time;
full raw points are not queried for list cards. Aggregate coaching/strength models
remain separate and are not incorrectly truncated to the visible history page.

Recorder start commits staging/origin/preserved execution atomically. Incremental
flushes copy only newly accepted points/events; identical committed retries are
idempotent, conflicts/sequence gaps/reversed clocks fail and roll back. Finished
stages cannot be revived. Recovery reports the durable cutoff/active time/pause and
saved prescription; it still finalises only the retained portion, not unobserved
movement or seamless cross-reboot tracking. The last unflushed batch remains a
physical reliability limit; automatic secret GPS restart is deliberately absent.

## 0.8.1 guidance and geographic content

All174canonical exercises resolve through one guide/artwork registry and shared
responsive screen; the strict release audit rejects missing text/art/accessibility
content. Bundled artwork is decoded only for the displayed guide. There is no old
schematic fallback for canonical exercises. Content and resource changes need no
schema/backup bump.

DreamRoutes retains immutable geometry IDs for every old assignment. Stable city
preferences choose currentGeometry only when assigning a new activity. London,
Berlin, Tokyo, New York and Paris have separately sourced geographic revisions;
Paris now uses the annual Marathon de Paris2026 OSM street reconstruction; the old Olympic ID is retained only for saved assignments. Generated projection scripts retain
source coordinates and licensing; all run offline. DreamCartography projects sourced
landmark/river context in the same frozen course frame. River geometry is clipped
in the renderer, never used to shrink the full course or fabricate streets. Original bundled city/poster illustrations are supplemental to sourced geographic
context and accessible course/progress labels.
ODbL coordinate databases remain separately licensed from MIT application code.
Neither geography nor presentation modifies actual GPS, stored equivalent offsets,
runner decisions, training load or the authoritative recording/execution engines.

### 0.8.1 offline city context

DreamBasemap is a bounded Android-independent parser for bundled ODbL geographic
extracts, separate from run samples and the immutable DreamRoutes course registry.
DreamBasemapArtwork loads only the selected frozen frame off-main, retains prepared paths
for at most three requested cities; raw decoded coordinate objects are released. DreamDrawing
renders original water/green/road styling plus preserved course/progress markers.
No runtime tile client, network SDK or user GPS query is involved. Failed/mismatched
context cannot alter a course assignment, recording or actual distance. Data-notice
links and attribution remain readable offline; source preparation is a development
tool, never part of the Android runtime.

## 0.8.2 Dream Routes presentation

DreamRouteExperience consumes the existing saved Assignment and immutable Route without
changing either. DreamRouteProgress formats exact42195m completed/remaining/total values.
DreamRouteArtwork resolves identity from the retained geometryID (including historical
editions), decodes only displayed local WebPs on IO and uses a bounded8MiB image cache.
DreamDrawing prepares preserved geographic paths off-main and draws a soft overview
with distinct full-course pins and blue equivalent-section rings. No schema, backup,
assignment, recorder, sample, provider or network architecture change is involved.
# Strength-first Today projection (0.9 checkpoint)

PrimaryFocus is a typed view over existing backed-up settings. Legacy starting paths
resolve without rewriting records; focus changes never edit programme priorities or goals.
StrengthTodaySummary is Android-independent and uses explicit actual/session/version
associations. CoachSnapshotReader computes its PB and coaching projection on IO with the
same observations used in CoachUiState, so navigation sees one coherent snapshot and
Compose does not repeatedly analyse retained history. The screen shares existing gym
logging/programme routes. Full Gym Now execution is a later checkpoint.

## Reviewed gym associations (0.9 retained scope)

Schema 12 adds typed `GymAssociationRepository` / `SqliteGymAssociationRepository` over
the same database. Reviewed immutable decisions and an indexed target projection update
atomically with the existing coaching association. No source execution, actual set or
programme version is rewritten. Backup strengthEngine section 2 reads older section 1;
root JSON 8/raw 2/complete 1 remain unchanged. No new generic release-store records.

## Reduced0.9 exclusions and association application protocol

Schema13 adds typed reversible exercise exclusions, distinct from legacy visibility state;
strengthState2 reads1. Generation/recommendation consults this policy without rewriting
existing intentions or actual evidence. `GymAssociationCoordinator` owns exact pending
reviewed decisions, retries and explicit reload; the feature VM serialises repository
calls on IO and publishes immutable state. Completed native membership is queried in one
bounded SQL lookup, not by hydrating every recent execution. Final source/gates are in
CONTINUE_CODEX.md and VERIFICATION.md; the original larger Strength backlog is deferred.
