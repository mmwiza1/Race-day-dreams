# Architecture

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
