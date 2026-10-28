# Gym Now and durable gym workouts

Gym Now is a Strength feature. It does not need a running profile, race goal,
Health Connect permission or network connection. It uses the same gym execution
engine for planned and spontaneous training.

## Choosing a workout

- **Start planned workout** shows the retained programme's sessions. Starting one
  freezes its programme ID, version, session and prescription. If the displayed
  programme has changed, the athlete must review the refreshed session first.
- **Quick workout** asks for available time, equipment and upper/lower/full-body
  focus. It uses the current planner's general-fitness, beginner and conservative
  effort guidance. Review the proposed exercises before starting.
- **Build a workout** supports a name and ordered exercise slots, with add, edit,
  move, duplicate and remove controls. Targets may use repetitions, duration or
  distance as supported by the exercise; an open slot has no prescribed target.
  Optional RPE/RIR guidance, rest and load references remain intentions.
- **Repeat previous workout** starts from retained structure with no new actual
  sets. A manual session contributes its working exercise list; a recorded gym
  execution retains its intended structure, including unperformed targets.
- **Favourite workouts** start a selected frozen revision. Editing and saving
  creates a later revision; removing a favourite does not erase prior versions or
  workouts. The picker reads bounded pages.
- **Generate one for me** also lets the athlete choose goal, experience, training
  priority and RPE/RIR guidance. It delegates selection to the current gym planner
  and explains limitations. Its temporary selection block is never stored as the
  athlete's programme or added to the calendar.

An **open workout** is available for choosing exercises while training. Quick,
custom, repeated, favourite, generated and open workouts remain extra training.
They do not silently satisfy a same-day programme session. Only an explicitly
started planned execution carries its exact preserved programme association.

## Recording actual sets

The active screen displays the current exercise, target, retained optional
reference, recent exact-exercise working sets where available, saved actual sets
and the coordinator's active/recorded elapsed time. It presents actual input
fields appropriate to the exercise: load and repetitions, repetitions alone,
duration, distance or a supported loaded variant. Load fields are explicitly
labelled kilograms; prescription values never prefill actual fields.

Effort is optional. Repetition-based exercises can record RPE or RIR, and a
warm-up checkbox excludes that set from working-set totals and PB evidence.
Saving a set writes it locally immediately. Entered values clear only after the
repository acknowledges the exact evidence. Unfinished sets do not become a
completed StrengthSession or PB evidence until finalisation.

The athlete can select another exercise, add an exercise, pause, resume, finish
or explicitly discard. Finishing with prescribed working sets still missing
requires a confirmation. The result retains actual evidence and a partial flag;
it does not invent the omitted sets. An open workout can finish after at least
one actual set. Discarded stages do not count as completed training.

## Equipment busy and today-only swaps

The live exercise card offers **Equipment busy?** and **Swap exercise**. A prepared
choice shows up to four intent-compatible alternatives, with more available on request.
Reasons include busy/unavailable equipment, movement discomfort, easier/harder variation
and general alternatives. Each option explains movement/body-area overlap, its complete
equipment setup and any measurement change. A shared upper/lower-body region alone does
not qualify an alternative. Archived exercises are not normally recommended; favourites
and recent history are tie-breakers after training compatibility.

The context uses this execution's preserved programme goal, experience and equipment
when available. Otherwise it explicitly defaults to general-fitness, beginner-compatible
choices and asks the athlete to confirm equipment. A generic machines label does not
imply every machine; a cable station does not imply every attachment. The optional typed
capability picker applies to this swap only and does not change programme settings.

The athlete confirms **Use just for today** before the existing execution coordinator
records a replacement. The original prescription, already saved sets, programme and
favourites remain unchanged. Previous-exercise sets stay visible separately and do not
count toward the replacement's target. Incompatible targets and every load reference
are cleared by the domain engine. Unsaved set inputs clear only after the replacement
write is acknowledged, and actual loads are never copied. A failed write uses the same
visible exact-retry path as actual-set entry.

Movement-discomfort guidance advises avoiding movements that reproduce pain and states
that alternatives are not medical clearance. No alternative is described as safe for an
injury. Permanent programme substitutions remain a separate phase; this flow does not
offer a nonfunctional permanent-change control.

## Ownership and interruption handling

`GymNowViewModel` is shared by the activity's Gym Now, builder and active-workout
destinations. It owns feature orchestration, not timing rules. One
`GymWorkoutSessionCoordinator` serialises operations around `GymExecutionEngine`
and `SqliteGymExecutionRepository`. Repository/coordinator calls run on IO, and
the UI receives immutable state. Visible lifecycle-driven clock observations do
not write a database row every second.

A failed write retains its exact snapshot and expected revision. Other mutations
are blocked until the athlete retries that same operation or explicitly reloads
the saved evidence. Retry does not generate a new set ID, clock observation or
duplicate event. A pending write is visibly unacknowledged; a lost acknowledgement
is not treated as proof that storage failed.

After a new process, the app lists unfinished stages and waits for an explicit
review/recovery decision. Accepted sets remain stored; recovery preserves pause
state and records interruption gaps rather than claiming uninterrupted active
training. There is no background GPS behaviour in gym execution. Form values and
stable input IDs also use `SavedStateHandle` where Android supplies saved UI
state; the SQLite stage remains the durable source of accepted evidence.

Saved definitions, favourite preferences, execution stages, events, reviewed associations,
exclusions and accepted actuals use additive schemas 11–13 and the versioned `strengthEngine` section of JSON envelope
8. Complete-container and raw-recording archive versions are unchanged. See
[STRENGTH_STORAGE_2.md](STRENGTH_STORAGE_2.md) for validation, bounds, migration,
source-proof and transactional backup rules. Unsaved editor drafts are UI state,
not a saved favourite or completed workout.

## Retained 0.9 boundary

This release finishes Architecture Hardening + Standalone Strength + Gym Now + Exercise
Discovery, Exclusions and Smart Substitution. The one-off factory uses the current safe
planner rather than introducing a major programming redesign. Actual load entry remains
independent of optional references. Full custom programmes/templates/multiblock systems,
rest timers, generated warm-ups, supersets, expanded catalogue and advanced analytics,
progression and hybrid engines are deferred; they are not release blockers.

## Explicit completed-workout association

After a spontaneous native workout, nearby supported programme targets can be reviewed.
Keep as extra or Count toward session requires an explicit confirmation of the current
actual and frozen programme version. It never changes native origin or invents completed
sets. The same decision ID is retried after a failed acknowledgement; another choice is
blocked until retry or explicit reload. Completed native workouts can be reopened from
Gym Now's repeat/recent list to review or reverse a choice. Later actual edits preserve
original decision evidence and refresh the review used for reversal. Planned executions
retain their original exact association rather than offering a spontaneous substitute.

## Exercise exclusions

Exercise detail offers a reversible explicit exclusion with a non-medical reason. Browse
→ Excluded offers Restore; matching searches label excluded exercises. This is separate
from legacy archive/hide and does not erase historical evidence or favourites. New
programmes, generated one-offs and today-only alternatives consult exclusions. Existing
prescriptions and saved workout intentions remain frozen, and may still be deliberately
used. If no supported candidate fits, the app explains that selection cannot be generated.

The full API28/35/36 gate and authentic captures must accompany these changes. Physical
S24 recovery, accessibility and complete-backup acceptance remain unperformed until a
human records results in TEST_BUILD.md.

## Verified retained release boundary

Application/capture source f3a988a passed workflow40741473316 with 331 tests on each API28,
35 and36, full build/lint/format/data gates and91authentic captures. New exclusions, explicit
associations and concise busy alternatives were directly reviewed, alongside dark/200%
layouts. Real S24 acceptance remains unperformed. No further Strength2.0 feature phase
is required for this scope-controlled release; only final PR-head checks/review remain.
