# Explicit gym workout association — implementation boundary

The current app keeps spontaneous workouts separate from programme sessions.
Only an explicitly started planned workout currently carries its preserved association.
The post-workout association UI is **not implemented yet**.

## Implemented rules and payload

`GymWorkoutAssociation` supplies deterministic proposal and decision rules. A proposal
retains the exact reviewed actual, original origin, programme revision and prescribed
session. Discovery requires a working set with the exact exercise identity and matching
repetition/duration/distance task within three local dates. Warm-up-only sessions and
future actuals do not qualify. Missing evidence does not create a match.

`KEEP_EXTRA` and `COUNT_TOWARD_SESSION` are explicit choices; constructing a proposal or
serialising a decision never creates a link or alters a prescription. A count decision
means the athlete chose an association, not that every prescribed target was performed.

Acceptance must revalidate the current actual, retained recording origin, exact current
programme version, preserved target and occupied links in one storage transaction.
A caller-supplied origin cannot override the recording's actual provenance. Stale actuals,
changed programmes and occupied targets require review again.

`GymAssociationJson` has independent format version1 and strict fields/types, bounded
standard JSON, frozen actual/target encoding and future-version rejection. It neither
consults today's exercise catalogue nor writes a database. Three Android codec cases
cover both choices, historical display names, distinct actual/reference loads and
malformed inputs. Their observed CI result belongs in VERIFICATION.md.

## Remaining integration — do not enable a partial association UI

1. Add typed decision history and current-association projection through an explicit
   schema12+ migration. Circulated schema11 must remain unchanged.
2. Preserve old planned/manual associations. Maintain a normalised indexed target lookup
   rather than scanning/decoding all historical coach-evidence JSON for each decision.
3. Atomically validate, append the explicit decision and update the existing coaching
   association projection. Stable decision IDs must make lost acknowledgements harmless;
   conflicting retries must fail. Never overwrite the original workout origin.
4. Keep decisions safe across actual edits. Existing SQLite replacement writes can delete
   a parent row: a cascading actual foreign key would silently erase decision history.
   Explicit actual deletion and later association correction need deliberate tested rules.
5. Extend the independently versioned strength backup section, retain old readers and
   validate the entire decision/source/link graph before transactional replacement.
   Unsupported/corrupt data must preserve current local data.
6. Add the saved-workout choice UI only after storage, migration and restore tests pass.
   Show the actual workout and exact prescribed target. Keep-extra must remain available;
   no date-based automatic completion or programme rewrite is permitted.
7. Test every supported schema, old/current complete backups, source edits/deletions,
   concurrent plan changes, occupied targets, retries and rollback on API28/35/36.

No schema, backup envelope, raw archive or permissions changed for the domain/codec
foundation. Full remaining Strength Engine2 scope is retained in CONTINUE_CODEX.md.
