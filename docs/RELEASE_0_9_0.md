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
- Browse all 174 canonical exercises by body area, movement and equipment, with relevance-first
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
- Pinned Kotlin/Java formatting is checked automatically. Existing 174 canonical and 14 routine
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

- Real Galaxy S24 outdoor recording, lock/background/recovery, battery, Health Connect,
  Bluetooth HR, TTS/music/calls, TalkBack and complete backup acceptance remain unperformed.
- Generated one-offs use the retained safe planner. Full custom programme builders,
  templates/mesocycles, major programming redesign, rest timers, generated warm-up sets,
  supersets, catalogue expansion and advanced analytics/progression/hybrid engines are
  deliberately deferred beyond the scope-controlled 0.9 release.
- A verified approximately19.1MB gym-backup fixture is not a worst-case peak-memory guarantee.
  Provably oversized gym exports are rejected before JSON construction; retain original backups.

## Data, privacy and licensing

Version0.9.0-test1 / versionCode12. Additive schema13 retains supported1–12 upgrades.
JSON envelope8 reads1–7; strengthEngine section2 reads1, strengthState section2 reads1.
Raw recording archive2 and checksummed complete container1 are unchanged. Validation
precedes the shared transactional replacement; a local unfinished gym workout must be
finished/discarded first. Old programmes, PBs, actual edits, favourites, recording samples
and stable Dream Route assignments remain supported. The existing Compose family is aligned
with stable UI/runtime/foundation 1.8.3 and Material 3 1.3.2 (BOM 2025.07.00, Apache-2.0). No new service/SDK, Internet
permission, account, backend, analytics, advertising, subscription or runtime LLM is added.
Exclusion reasons and reviewed choices remain private local data unless explicitly backed up.

## Verification

Retained application/capture source f3a988a68f3d1b6dbb618422a80612b90cf4d92e passed workflow40741473316:
331 instrumentation tests on EACH API28/35/36, build/lint/format/domain/schema/source/guidance
and91 canonical captures. Botb768e0b captures were adopted; current source fingerprint
matches their metadata. Current RDL/busy/excluded/association and representative light/dark/
200% layouts were opened and reviewed, not merely byte-validated. The first alternative and
Choose action are visible without excessive context; normal feedback no longer covers guides.

Local219,527domain assertions/31suites,55schema,9release-note,14formatter,8capture-validator,
11Dreamsource and14artwork-integrity cases pass; strict174/174canonical+14/14routine guidance
passes. Current/old backup, corrupt restore and migration gates pass. API35 verified APK is
50,799,716bytes (artifact12840411651); actual captures artifact12839284580. The existing
Compose refresh adds3.10% with no new artwork. Final documentation-only required checks
remain mandatory before PR14 readiness; no physical S24 pass is claimed.
