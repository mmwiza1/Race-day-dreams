# Strength programming — retained policy and 0.10 integration

For the current0.10 model, templates, future revisions, permanent substitutions,
optional progression and history, see [Strength Programming2.0](STRENGTH_PROGRAMMING_0_10.md).
Historical implementation notes below are preserved; source-bound current verification
is in VERIFICATION.md and CONTINUE_CODEX.md.

# Original strength programming — 0.5

Race Day Dreams recommends; the runner decides. Programming is optional and local.
No programme tables, source code or illustrations have been copied from another app.

## Research reviewed on 22 October 2026

- [ACSM 2026 position stand, Currier et al., DOI 10.1249/MSS.0000000000003897](https://pmc.ncbi.nlm.nih.gov/articles/PMC12965823/): repeated major-muscle exposure, heavier loading for strength, sufficient weekly volume for hypertrophy, and moderate-load, low-volume power work. Many programme structures can work; failure training is not required. This updates the older 2009 position stand.
- [Schumann et al. 2022, DOI 10.1007/s40279-021-01587-7](https://pubmed.ncbi.nlm.nih.gov/34757594/): concurrent endurance and resistance training did not generally compromise maximal strength or hypertrophy in the pooled studies; explosive-strength development can be affected, particularly when both are performed in one session. This does not establish a universal recovery interval for an individual.
- [Llanos-Lagos et al. 2024, running economy review](https://pubmed.ncbi.nlm.nih.gov/38165636/): strength methods can support running economy, with variation in interventions and evidence quality. The app does not promise a performance benefit to every runner.

These are scientific references, not incorporated dependencies or copyrighted schedules.
The exact set counts, six-week blocks, scheduling penalties and progression thresholds
below are original product heuristics, not validated individual physiological models.

## Implemented rules

A runner chooses goal, priority, experience, available days, frequency (1–6), equipment,
30/45/60/75-minute budget and RPE/RIR display. Running-only/manual-strength users do
not have to create a programme. Disabling a generated programme preserves history.

One to three days uses full-body sessions; four uses upper/lower; five distributes
upper/lower/push/pull/lower; six repeats push/pull/lower. These structures distribute
volume rather than claiming split branding has special benefits. A one-day routine
is supported as a practical compromise, not an optimal universal dose.

Strength goals use lower rep ranges and longer rests; muscle goals use higher rep
ranges; running support includes unilateral, calf and trunk work with restrained
sets. Experience and priority limit working sets. Power preparation uses low reps
and an intentional fast-concentric/control cue for experienced users; automatic Olympic-lift prescription is deliberately absent.
No estimated 1RM is required for loading. The runner chooses manageable loads.

Exercise selection checks equipment, including bench/rack requirements and bodyweight
fallbacks. Sets/rest/setup consume an approximate time budget. That estimate is not
a stopwatch guarantee. Stable core movement selections repeat throughout the block.

The lightweight scheduler enumerates a small set of weekday assignments. It penalises
lower-body work next to key runs and after actual hard lower-body work; priority changes
the relative penalty. Unavoidable conflicts are disclosed rather than deleting a session.
Running prescriptions and recurring availability are never silently changed.

Two comparable sessions at the top of the rep range with suitable recorded RPE may
suggest a small load increase. Missing effort supports holding, not assuming easy
execution. Repeated difficulty supports a hold/reduction. Suggestions never apply
weights automatically. Accepting an optional reference creates a new prescribed version;
keeping it records a no-change decision. Both retain the evidence and before/after version
identities in backup. Raw RIR is preserved separately from an approximate coaching RPE.
Actual sets and immutable prescribed versions stay separate.

## Deliberate limits

The generator is a conservative test-build implementation. It is not a substitute for
individual technique coaching and does not claim an optimal hypertrophy dose for every
frequency/time budget. Advanced power methods and automatic task-specific time/distance
progression are not prescribed. All184canonical exercises have original illustrated technique guidance;
qualified review and physical-device inspection remain necessary.

## 0.10 manual authoring and structured custom exercises

Manual drafts persist through typed optimistic rows. The phone builder edits name, schedule,
block duration/phase, workout names/notes, exercises/order and precise optional targets.
Preview expands the exact dated prescriptions; explicit confirmation activates atomically
against the reviewed draft and programme pointer. This does not record an actual set.

Custom entry records a stable custom- identity, broad area, movement role, measurement,
complete equipment setup (or explicit equipment-free choice) and optional notes. It uses
schema14's typed custom repository and atomic logging mirror. Six measurement profiles and
weighted-bodyweight logging retain actual evidence. Canonical guidance is not imitated for
custom movements, estimated1RM is disabled, and automatic generated selection remains
canonical. Older UUID custom logging/history is retained; no renaming or transfer of history
is inferred from a similar name. Metadata participates in browsing/filtering/detail.

Editable generated/template previews, future regeneration, permanent replacements,
modality proposals and read-only revision history are connected. The supported 426-case/API
software gate and scoped light/dark/200% review passed. Physical S24 acceptance is separate;
see the current Strength Programming 2.0 document and VERIFICATION.md.

## Concrete generated previews — durability foundation

Schema 15 adds separate typed preview header, block and dated-workout tables. A saved
concrete preview preserves every generated week, target, phase, workout note and trade-off;
it never becomes a repeated first week and saving it does not activate programming.
The recurring manual builder remains supported unchanged.

Concrete preview activation verifies the exact reviewed saved revision and the current
programme in the same SQLite transaction, then uses the existing eligibility/completed-history
checks. A failed decision or restore rolls back the whole transaction. Deleting a preview
does not delete an accepted revision or actual evidence. Summary pages read indexed headers
without hydrating workout payloads.

JSON backup remains envelope 9. The nested strengthProgramming archive writes version 2
and reads versions 1–2; version 1 contains no concrete previews. New/future or malformed
sections are rejected before replacement. Raw recording archive 2 and complete package 1
are unchanged. Templates & generated programmes now opens from the programme builder. Choose one of the
nine starting structures or generate from preferences. Configure days, goal, experience,
priority, duration and real equipment, then review the saved dated blocks. Individual
workouts support target/exercise editing, ordering, removal and additions. Generating
another preview retains the previous saved draft. Accepted previews are read-only.
The engine considers retained demanding runs/race context for hybrid users; Strength-only
users need no running profile. The supported426-case/API gate and scoped capture review passed.
Future-only editing and regeneration now prepare a saved next revision from the current
programme. Completed and earlier workouts are read-only; activation independently rechecks
fresh completions and current ownership. Regeneration can change goal, days, equipment,
frequency and length while keeping the original start and exact retained prescriptions.
The original revision and native actuals remain separately readable. Competing unaccepted
proposals remain saved; after another is accepted they are superseded/read-only, and do not
replace accepted history during backup restore. Permanent-swap controls, optional progression and read-only accepted history are connected.
The supported426-case/API gate and scoped light/dark/200% visual review passed.

Activation of a forward revision waits while a native workout using that programme is
active, paused or awaiting recovery. The athlete can finish/discard that workout first;
then activation rechecks fresh completed evidence. Previewing changes never stops a workout.

Permanent-substitution domain operations and connected review controls can retain each phase's
own target across selected future occurrences, clear load references and record final
accepted-base→replacement provenance. A changed measurement needs an individually reviewed
target. SQL acceptance independently rechecks original intent, fresh exclusions/archived
choices and equipment. These operations create no actual sets or completion evidence.

## 0.10.1 coaching clarity

The optional progression review now leads with the actual task in plain English. Repetition,
time and distance instructions come from the preserved structured target. No effort or rest
is invented when absent. Perceived effort (RPE) and repetitions left before failure (RIR) are
separate explanations, never presented as interchangeable scales.

About the weight distinguishes the current optional prescription reference from the suggested
new reference. Neither is labelled as a lifted result. Only actual recorded sets in the supporting
workouts are shown as recorded weights. Advanced details starts collapsed, can be expanded by
touch/keyboard/accessibility action, and retains state through recreation. It exposes separate
current/proposed targets, exact supporting records and the existing progression safeguards.

Save this suggestion for review changes only the reviewed draft. Freshness and transactional
checks remain intact; activation still requires a separate athlete choice. Keep my current
target dismisses the review without saving. The underlying conservative calculations, retained
history and backup formats are unchanged. See VERIFICATION.md for the current gate status;
physical Galaxy S24 accessibility acceptance remains unperformed.

### Compact progression review (5November refinement)

Advanced now highlights only genuine target differences and lists unchanged sets, task,
effort and rest once. These are read-only comparisons of the preserved target and the
existing optional proposal, never values inferred from historical weights. A second
collapsed View supporting workouts control contains retained exact-exercise working sets,
original effort evidence, the full calculation reason and safeguards. At large font sizes,
comparison values and unchanged labels stack naturally. Both disclosures retain accessible
state and support keyboard/touch. Calculation, freshness checks and separate activation
remain unchanged. Final source-bound captures and visual acceptance are recorded separately.

When only time/distance grows, an unchanged optional weight reference is described as
unchanged rather than a new0kgload increase. This is presentation only; progression
calculations, actual logged weights and separate activation are unchanged.
