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

Saved definitions, favourite preferences, execution stages, events and accepted
actuals use schema 11 and the versioned `strengthEngine` section of JSON envelope
8. Complete-container and raw-recording archive versions are unchanged. See
[STRENGTH_STORAGE_2.md](STRENGTH_STORAGE_2.md) for validation, bounds, migration,
source-proof and transactional backup rules. Unsaved editor drafts are UI state,
not a saved favourite or completed workout.

## Current checkpoint and remaining Strength Engine 2.0 work

This document describes the implemented Gym Now integration, whose Android build
and UI regression gate must be recorded in `VERIFICATION.md`; it is not evidence
that the full Strength Engine 2.0 release or physical acceptance has passed.

The one-off factory still uses the current planner's heuristics. The requested
goal/experience-specific multi-block generator, stronger power programming,
explicit spontaneous-to-programme association choice, permanent programme
substitutions, exclusions/custom exercise controls, optional rest timer and
notifications, evidence-based warm-up ramps, superset execution and wider
measurement-specific progression remain later phases. Preserved optional load
references are shown; actual load entry remains independent.

The full Android API 28/35/36 gate, authentic captures and real-device checklist
must accompany the integrated checkpoint. Galaxy S24 backgrounding, process
interruption, recovery, accessibility and complete backup/restore checks remain
unperformed until a human records their results in `TEST_BUILD.md`.
