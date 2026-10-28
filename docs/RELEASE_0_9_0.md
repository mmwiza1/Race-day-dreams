# Race Day Dreams 0.9.0-test1

Architecture Hardening + Standalone Strength + Gym Now + Exercise Discovery, Exclusions
and Smart Substitution. Running and Strength can be used independently, with useful
context when an athlete chooses both.

## What's new

- Choose Running, Strength or Hybrid emphasis during welcome or later in Settings.
  Strength setup and Today work without a race goal or running profile. Switching focus
  preserves existing goals, programmes, actual workouts and unfinished drafts.
- Gym Now starts planned, quick, custom-built, favourite, repeated, generated or open
  workouts through one durable engine. Actual sets are entered independently and saved
  locally; frozen intentions never become fabricated actual performance.
- Browse all174canonical exercises by body area, movement and equipment, with relevance-first
  name/alias search and programme, recent and favourite shortcuts. Guidance remains offline.
- Explicitly exclude an exercise from new programme and alternative recommendations, then
  restore it from Exercises → Excluded. History, PBs, favourites and existing targets stay.
- Equipment busy/unavailable offers explained, intent-matched alternatives for today.
  Confirming a swap preserves the original programme and earlier actual sets and clears
  incompatible load references. Equipment and rationale details can be expanded.
- Review a completed spontaneous native workout as extra or explicitly count it toward a
  compatible preserved programme session. Reopen it to review/reverse the choice. Actuals,
  original prescriptions, reviewed version and spontaneous origin remain separate.

## Improvements

- Feature-owned application operations, scoped repositories and navigation retain one
  shared database and one coherent command/read model. The authoritative run service has
  testable recording, execution, sensor and cue coordinators.
- Typed gym staging, stable set IDs and revision checks support exact lost-acknowledgement
  retries. A new process requires explicit recovery, preserves pause state and records
  interruption gaps. Partial finish remains explicit; saved sets enter history only on finish.
- Exact-exercise starting references and conservative dated progression retain their
  evidence requirements. Missing effort, stale evidence and one success do not imply an increase.
- No new compulsory configuration for Running users. Hybrid Today can show both disciplines;
  Strength-focused Plan and Today stay gym-first when an older running profile is retained.
- Recorded route points, fitted path, genuine markers, pauses and Route Story publish as
  one prepared detail projection. Stored route evidence is preserved in full. City artwork
  readiness is published coherently; canonical captures observe its own semantics node.
- Pinned Kotlin/Java formatting is checked automatically. Existing174canonical and14routine
  illustrations, geographic Dream Routes, backup archives and offline behaviour remain intact.

## Fixes

- Six-week gym blocks use six complete programme weeks anchored to the actual start date;
  mid-week starts do not allocate a seventh calendar-week session.
- Upper/Lower uses distinct squat/hinge occurrences. Full Body lower work and demanding-run
  timing contribute truthful conflict context, including consecutive programme-week boundaries.
  Explanations describe exercises actually retained within the time budget.
- Actual edits and reviewed association history survive together; an exact retry cannot
  silently undo a later choice. Restore validates association sources and current links
  before replacement. Existing hide/archive choices are not reinterpreted as exclusions.
- API28 remains supported: association membership validation uses compatible collection
  operations rather than an API34 record-method reference.

## Known limitations

- Current-source release gates are recorded in VERIFICATION.md. Do not treat an earlier
  passing checkpoint as verification of later UI or make the draft ready before all gates pass.
- Real Galaxy S24 outdoor recording, lock/background/recovery, battery, Health Connect,
  Bluetooth HR, TTS/music/calls, TalkBack and complete backup acceptance remain unperformed.
- Generated one-offs use the retained safe planner. Full custom programme builders,
  templates/mesocycles, major programming redesign, rest timers, generated warm-up sets,
  supersets, catalogue expansion and advanced analytics/progression/hybrid engines are
  deliberately deferred beyond the scope-controlled0.9release.
- A verified approximately19.1MB gym-backup fixture is not a worst-case peak-memory guarantee.
  Provably oversized gym exports are rejected before JSON construction; retain original backups.

## Data, privacy and licensing

Version0.9.0-test1 / versionCode12. Additive schema13 retains supported1–12 upgrades.
JSON envelope8 reads1–7; strengthEngine section2 reads1, strengthState section2 reads1.
Raw recording archive2 and checksummed complete container1 are unchanged. Validation
precedes the shared transactional replacement; a local unfinished gym workout must be
finished/discarded first. Old programmes, PBs, actual edits, favourites, recording samples
and stable Dream Route assignments remain supported. No new runtime dependency, Internet
permission, account, backend, analytics, advertising, subscription or runtime LLM is added.
Exclusion reasons and reviewed choices remain private local data unless explicitly backed up.

## Verification

Latest adopted source5527cce/workflow40702322526 passed328instrumentation tests on EACH
API28/35/36 plus build/lint/format/guidance and canonical publication. Bot5b66454 supplied88
actual captures. The connected association UI, expanded91capture set and subsequent busy
selection refinements require their own final gate; consult VERIFICATION.md for current
observed results. Local219,527domainchecks/31suites,55schema,9release-note,8screenshot-validator,
11Dreamsource,14artwork-integrity and strict174/174canonical+14/14routine guidance pass.
No physical S24 result is claimed.
