# Strength Coach — retained 0.9 scope

The app recommends. The athlete decides. The plan adapts.

0.9.0-test1 delivers Architecture Hardening + Standalone Strength + Gym Now + Exercise
Discovery, Exclusions and Smart Substitution. This document retains its historical filename;
it does not claim the original larger Strength Engine2.0 roadmap has shipped. Current
source-bound gates and remaining acceptance work are in VERIFICATION.md and CONTINUE_CODEX.md.

## Independent training focus

Running, Strength and Hybrid are changeable presentation priorities over one local database.
Strength setup needs no race goal or running profile. Switching focus preserves existing
goals, programme priorities/versions, actual history and unfinished workouts. Running users
are not required to configure Strength. Hybrid Today can show both same-day disciplines.

Strength-first Today leads with today's preserved gym session, Gym Now, actual completion,
PB evidence and at most one conservative coaching note. Programme association is explicit;
sharing a date never makes an actual workout complete a prescription. Summary preparation
runs off the main thread, and supporting detail remains available without crowding Today.

## Corrected existing programme rules

Six weeks are six seven-day programme windows from the actual start, including mid-week
starts. Frequency is allocated within each window, without a seventh calendar-week session.
Lower variations use their own occurrence count, preserving squat and hinge emphasis.
Scheduling classifies retained exercises: meaningful Full Body lower work counts; core-only
work does not. Same-day/before demanding runs have conflict context, the following day a
softer penalty. Consecutive lower sessions across week boundaries are considered, and
unavoidable spacing is explained rather than silently removing sessions. Explanations name
movements actually retained within the time budget. These are inspectable product heuristics.

The existing generator remains deliberately conservative. This release does not introduce
mesocycles or a major goal/experience redesign. Progression requires two recent comparable
sessions, intended recorded effort and proven loading; missing effort, stale evidence and
one success do not justify an increase. Starting references require exact-exercise recent
working evidence. Suggestions remain optional and never overwrite actual performance.

## Explicit exercise metadata and discovery

All174canonical IDs retain their original measurement definitions and illustrated guides.
Reviewed metadata identifies movement roles, broad primary/secondary body areas, experience
suitability and OR-of-complete-equipment setups. A barbell bench requires its complete
bar/bench/rack setup; one machine never implies all machines. Suitability describes movement
complexity, not medical safety or measured biological stimulus. Legacy custom exercises
remain loggable; unknown metadata is not invented and does not enter automatic selection.

Browse by supported area, role or equipment. Programme/recent/favourite shortcuts and exact
name/alias relevance reduce flat-library browsing. Existing favourites, hide/archive and
actual history remain intact. New canonical entries require guidance/metadata integrity;
this release keeps174complete entries and14routine movements rather than expanding count.

## Reversible exclusions

Exercise detail offers an explicit exclusion with a non-medical preference reason. Exercises
→ Excluded offers Restore. Default recommendations in generation, Gym Now and the retained
manual picker omit exclusions; explicit search keeps them discoverable. Excluded search
results are labelled, while original prescriptions and recorded actuals remain usable.
Restoring removes only the exclusion preference, preserving favourites, history and PBs.
Old archive/hide choices are not silently reinterpreted. If no supported candidate remains,
generation explains the conflict without ignoring preferences or filling unrelated roles.

## Gym Now and smart alternatives

Planned, quick, custom-workout, favourite, repeated, generated and open workouts share one
GymExecutionEngine and application session coordinator. Custom workouts are saved date-free
intentions, not a full custom programme builder. Repeats copy intentions, never actual sets.
Actual entry remains independent, each accepted set saves locally, and visible forms clear
only after acknowledgement. Pending snapshots retain exact IDs/revisions; retry cannot
duplicate sets or silently replace a later action. Explicit interrupted recovery preserves
pause state, records timing gaps and never secretly restarts a clock. Early finish retains
partial evidence. No unfinished stage is promoted to completed history or PB evidence.

Busy/unavailable alternatives match training role, goal, body area, measurement, complete
available setup, experience and exclusions before favourites/history. The initial list is
bounded; more alternatives and explanation detail are available. The reason/equipment
controls apply only to this swap. Confirmation preserves original targets and prior actual
sets, removing incompatible load references. Discomfort guidance is non-diagnostic and never
claims an alternative is medically appropriate for an injury. Structural programme editing
remains separate; no nonfunctional permanent-change control is presented in this flow.

Completed spontaneous native workouts remain extra unless the athlete reviews Count toward
session. Frozen actual/target/programme-version evidence is retained in an immutable decision.
Lost-ACK retries reuse the same ID, and reviewed Keep as extra reverses the current link
without altering origin, actuals or programme history. Reopening native recorded workouts
from Gym Now's recent/repeat list exposes this review. Association does not claim every
prescribed set was completed. See GYM_NOW.md and STRENGTH_STORAGE_2.md for detailed contracts.

## Durability, privacy and acceptance

Typed repositories retain one SQLite database and transactional restore. Additive schema13
reads supported1–12 upgrades. JSON8 reads1–7; strengthEngine2 reads1, strengthState2 reads1;
raw archive2 and complete package1 remain unchanged. Full graph validation precedes
replacement, and an active local gym workout must be finished/discarded first. No new
concept is placed in the generic release store. No Internet permission, backend, account,
analytics, advertising, subscription or runtime LLM is introduced.

Current full API28/35/36, screenshot and physical acceptance status is recorded with exact
source IDs in VERIFICATION.md. S24 outdoor/sensor/audio/battery/TalkBack and complete-backup
acceptance remain separate human checks; emulator success is not physical verification.

## Deliberately beyond 0.9

Full custom programme builder, templates, multi-block/mesocycles, major Strength/Muscle/Power
redesign, rest timer, generated warm-ups, supersets, catalogue expansion/new custom-exercise
features, advanced progression/weekly volume/PB redesign/achievements/deload and advanced
hybrid decisions are deferred. They are not unfinished release blockers. Existing stable
functionality remains supported; do not restart the larger roadmap as part of this release.
