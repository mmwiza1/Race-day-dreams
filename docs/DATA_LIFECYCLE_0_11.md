# Recording detail and permanent training history

## What stays

Storage cleanup never deletes workout summaries, actual gym sets, qualified/preserved PBs, race
results, programme versions, coaching decisions, milestones, shoes or ordinary body observations.
It never calls the activity upsert/recalculation path. It never deletes external Health Connect
records. Existing activity totals are not recalculated when their raw samples are removed.

## Choose, preview, confirm

Settings → Storage & recording detail offers 24 calendar months, five calendar years and Keep
forever. UTC calendar arithmetic is used, not an invented fixed number of days. Truly new databases
start at 24 months. Upgrades start at Keep forever / existing unreviewed, protecting every existing
route until the athlete acknowledges the consequences. An older backup without this feature also
restores protected defaults. Saving a policy never starts deletion.

Cleanup is foreground, explicit and advisory: save an informed choice, review eligible counts,
then use the separate confirmation. There is no scheduled/background expiry worker. The page
explains loss of routes and precise sample-based detail and recommends complete backup first.
A missing backup cannot be reconstructed from totals.

Previews/approval are immutable in-memory values. They expire after 24 hours, including resumed
batches. The current policy choice identity and install-local dataset epoch are rechecked in each
transaction. JSON replacement and raw restore invalidate the epoch atomically; failed restores
roll back that invalidation too. Reboot/process death does not resume a destructive operation.

## One recording per atomic batch

Only finished app-owned RDD/GPX sessions with matching activity/source identity, known old end
and actual dense rows are eligible. Indexed summary end time is rounded UP by one millisecond
when selecting candidates, so sub-millisecond source evidence cannot expire early. Unknown,
orphan or foreign evidence is not made eligible. Active, paused and recoverable runs block cleanup.

Each batch rechecks one indexed recording/count proof; it does not hydrate whole routes or recount
the entire history. SHA-256 fingerprints cover original ordered sequence/payload rows. A normalized
receipt and deletion of that session's recording_points/recording_events commit together. A failed
SQL statement/process interruption rolls the whole session back; earlier committed sessions stay
removed. Identical retries are idempotent. Leaving the page stops between batches; refresh/review
remaining candidates after interruption. The pending preview is never automatically restored.

No summary, activity annotation, lap, actual strength set or PB row is modified during cleanup.
The receipt records source, original timing, counts, fingerprints, removal time and selected policy.
Activities display an explicit removed-detail explanation instead of invented maps/splits. GPX
export refuses missing detail rather than creating coordinates from summary totals.

## Storage numbers

The inventory returns lightweight counts/metadata and SQL aggregate UTF-8 payload lengths. It
never reads GPS payload cells merely to count them. Payload bytes exclude SQLite indexes and row
allocation overhead. Approximate physical database bytes include DB/WAL/SHM, not cache, bundled
artwork or exported files managed by other apps. SQLite row deletion does not imply an immediate
reduction of device file size. No VACUUM/reclaimed-space promise is made.

## Backups and original-detail restoration

Additive schema19 stores typed choices, a local epoch and removed-detail receipts. Root JSON13
adds detailRetention v1 and reads supported older roots1–12. It validates receipt/source ownership
before replacement; invalid/future/corrupt documents fail non-destructively. The epoch is not
exported. JSON alone does not contain raw GPS coordinates.

Raw archive3 adds explicit removed-detail entries and retains readers1–2. Version3 is bounded at
100000session headers,500000points per session and512MiB uncompressed data; older formats retain
their original1000/100000limits. Limits fail explicitly, never silently truncate. Root JSON retains
its32MiB bound. Complete package1 retains the existing shared outer SQLite transaction for both
JSON and raw parts; active/unfinished training protection remains.

A compatible earlier raw archive can explicitly restore a removed session only when original
session timing/counts and fingerprints agree. Different coordinates, coverage, summary totals or
source identity reject the restore transaction. Available matching raw detail is never deleted
by importing an older pruned archive. Compatible raw import does not replace retained summary
values or newer annotations. Successful restoration clears the current unavailable receipt.
If the original receipt is missing, do not guess that arbitrary conflicting evidence is compatible.

## Verification boundary

Domain/schema/local compilation and dedicated repository/UI tests are included. Actual API28/35/
36 execution, authentic215capture review and final0.11 acceptance must be recorded from the latest
workflow in VERIFICATION.md. This document does not claim an unobserved emulator or S24 pass.
The larger history fixture is a backup test, not one of the three six-month athlete simulations.
