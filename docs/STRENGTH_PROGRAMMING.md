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
progression are not prescribed. All174canonical exercises have original illustrated technique guidance;
qualified review and physical-device inspection remain necessary.

## 0.10 manual authoring and structured custom exercises — verification in progress

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

The new UI/device cases and captures must pass before release. Editable generated/template
previews, future regeneration, permanent swap UI and modality proposal controls remain
unfinished; domain support is not a claim of finished user workflows.
