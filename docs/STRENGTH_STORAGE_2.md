# Strength Engine 2.0 — typed durability

This document describes the 0.9 data foundation. It is not a claim that the full Gym Now
or live gym interface has shipped. The latest completed supported-API gate is recorded in
VERIFICATION.md. Source77fd0b4 passed the complete supported-API gate with293Android
cases on each API28/35/36, including12repository and11backup cases. Physical Galaxy S24
acceptance remains unperformed. Subsequent uncommitted coordinator/UI/resource guards
require their own gate; they are not covered by that source's results.

## Ownership and version boundaries

`GymExecutionEngine` remains Android-independent and owns prescription/actual separation,
set identity, execution events, pause/resume and recorded timing. `GymExecutionRepository`
defines its typed storage contract. `SqliteGymExecutionRepository` owns SQL on the existing
shared `ProfileDatabase`, alongside the existing coaching and backup repositories. It
does not add another database or put new records in the miscellaneous release store.

The additive schema 10 → 11 migration introduces only these durable families:

| Table | Purpose |
| --- | --- |
| `strength_workout_versions` | Immutable workout revisions, frozen exercise definitions and retained source proof |
| `strength_workout_preferences` | Current saved revision and favourite choice |
| `strength_executions` | Execution identity, origin, state, revision, summary columns, bounded header and accepted current actual |
| `strength_execution_sets` | Ordered append-only entered sets with stable actual-set IDs |
| `strength_execution_events` | Ordered append-only execution events |

Foreign keys retain workout-revision relationships and cascade child staging rows only
when their execution is explicitly deleted. A unique partial index permits at most one
unfinished gym execution. History ordering has an index; unfinished-summary queries read
summary columns without decoding workouts, sets or events. Opening one execution loads
its own evidence, not the full gym history.

Schema 11 is a fixed contract once circulated. Exclusions, custom exercise metadata,
programme/block extensions and other later durable concepts must use explicit subsequent
migrations, beginning with schema 12 where needed. They must not be silently appended to
an already distributed schema 11. Existing gym programme versions, favourites, actual
sessions and PB evidence are retained by the additive migration.

## Atomic writes and retries

Starting an execution requires an empty active snapshot. Its frozen prescription and
source are retained before the initial stage commits. A second unfinished workout is
rejected; the existing stage is not overwritten.

Saving uses an expected repository revision. The execution header and newly appended
sets/events commit in one SQLite transaction. Existing actual-set and event prefixes
cannot change; a stale divergent writer is rejected. An identical retry after a lost
acknowledgement returns the retained result rather than appending duplicate sets or
distance/time evidence. The UI must report success only after persistence succeeds.

Finishing is a separate atomic operation: it writes the actual `StrengthSession`, the
exact explicitly planned association where applicable, and the terminal execution stage
together. Unfinished actuals do not enter the completed-training/PB store. A spontaneous,
quick, custom, generated, favourite or repeated workout does not complete a programme
session merely because it occurred on the same day.

The original prescription and original execution evidence remain frozen. If the athlete
later accepts an edit to the completed actual workout, the current accepted actual and
its edit timestamp are retained separately, in the same transaction as the actual edit.
PB calculation continues to use the current actual history; it does not award records
from superseded values in the original execution snapshot. Explicit deletion of a
completed actual removes its terminal stage, while retained repeat templates/source proof
remain available. Deletion cannot silently remove an unfinished execution.

## Provenance is more than an identity hash

Every execution origin uses the same domain state machine. A source reference identifies
an exact programme/session revision, saved workout revision or repeated actual source.
An identity hash alone does not prove that the frozen targets match that source.

`GymWorkoutSourceValidator` compares preserved targets, order and exercise IDs against the
retained programme, and compares saved frozen definitions in full. It never rebuilds old
definitions through the current exercise catalogue. Legacy programmes stored exercise
IDs and targets rather than complete old names, so their historical names cannot be
independently authenticated; the newly frozen definition remains preserved.

A repeat starts with fresh actuals. When its source is accepted, the repository validates
and retains the repeated template proof. Later deletion of the original actual does not
invalidate that already retained template or cause the repeated workout to be re-derived
from today's catalogue. A repeated partial execution can retain unperformed intentions;
those intentions still do not become completed sets.

## Bounded storage and recovery

The workout payload, execution header, each set/event row and accepted current actual
have a 256 KiB UTF-8 row bound. Sets and events are normalised into separate rows rather
than an ever-growing SQLite cursor cell. A record that exceeds a bound fails explicitly;
previously committed staging is retained. Domain limits also bound recorded time, active
time, actual-set count and event count. These are resource guards, not training targets.

Strict independently versioned JSON codecs reject unknown fields/future versions, missing
required fields, type coercions, invalid clocks and corrupt event replay. Missing metrics
remain explicit nulls. Frozen historical exercises are decoded from the retained data.

Timing uses a monotonic continuity identifier as well as wall-clock observations. Recovery
after interruption or reboot records a gap and preserves only retained timing and actual
sets. It must not imply uninterrupted exercise or silently restart a clock. Pause state,
prescription, prior actuals and execution events remain available to the recovery flow.

## Backup compatibility and replacement

JSON envelope 8 requires an independently versioned `strengthEngine.version=1` section
containing workout revisions/source proof, saved preferences and execution snapshots,
including unfinished staging and accepted actual edits. Readers for envelopes 1–7 remain
supported. Older apps must reject envelope 8 rather than silently discard its gym data.
Raw recording archive 2 and complete-backup container 1 are unchanged; the complete
container carries the new JSON envelope through its existing integrity workflow.

The complete strength graph is validated before destructive replacement: unique IDs,
workout/source references, exact prescription snapshots, event validity, completed actuals,
edit observations and explicit programme associations must agree. Restore uses the shared
outer SQLite transaction. A corrupt or unsupported document must leave current data
intact. Replacing local data is refused while a local unfinished gym execution exists;
the athlete must first finish or discard it. An unfinished stage from a valid backup
remains recoverable evidence rather than an automatically running workout.

The JSON reader is bounded to 32 MiB, depth 32, two million structural tokens and a
1 MiB serialised-string limit. Export fails before truncating a destination if the
supported size is exceeded. These limits accommodate larger strength histories without
claiming unbounded scale. Tests that measure heap before and after a fixture are not
measurements of peak allocation; no physical-memory or multi-year performance claim is
inferred from them.

The verified synthetic450workout/2250working-set export was19,102,019bytes on API35/36
and19,102,016 on28. Observed heap after export was164,180,704 /165,440,400 /187,560,424
bytes respectively. These are individual before/after samples, not peak measurements,
and do not establish that every32MiB document fits memory. The near-row fixture retained
238,630raw bytes and279,304escaped payload characters with2000actual sets. See the full
measurement table and limitations in VERIFICATION.md.

The current uncommitted `GymExportBudget` guard performs SQL aggregate UTF-8 byte counts
over the five gym storage families inside the export snapshot transaction, before JSON
construction. It returns counts rather than hydrating payloads through CursorWindow and
rejects a provable lower bound beyond the backup limit before any destination bytes.
Its new supported-API test is pending. Passing this lower-bound check does not guarantee
the final serialised size or bound peak heap: escaping, wrappers and other sections add
bytes. Complete streaming/resource hardening remains subsequent Phase35 work; the32MiB
reader limit must not be advertised as a universally verified memory capacity.

## Verification boundary

Required automated coverage includes all supported schema upgrades, strict codec
round trips/malformed inputs, revision conflicts, retry idempotence, append-only actuals,
partial recovery, exact source validation, actual edits/deletions, previous/current
backup reads, transactional rollback and a history fixture above the former 8 MiB limit.
The verified gate and measured fixtures are recorded in VERIFICATION.md. Subsequent
additions need new observed results. The data foundation does not substitute for the full Gym Now,
live set-entry, rest-timer, substitution or programme-builder acceptance gates.
