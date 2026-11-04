# Race Day Dreams 0.10.0-test1

Strength Programming 2.0 adds editable programmes, original templates, meaningful training
blocks and reviewed future changes to the standalone Strength foundation. Prescriptions,
recorded sets and historical evidence remain separate. Every activation is your choice.

## What's new

- Build a programme from scratch: configure training days, equipment, blocks, workouts,
  exercises, sets, rep/time/distance targets, optional effort/rest and notes. Reorder, move
  or duplicate entries, save drafts, preview dated workouts and explicitly activate.
- Start with nine editable templates, including Full Body, Upper/Lower, Push/Pull/Legs,
  Strength, Muscle and Running Support. Goals, experience and availability produce distinct
  prescriptions and appropriate Foundation, Development and Intensification blocks, with
  optional Recovery/Assessment. Assessment never requires maximal testing.
- Create structured custom exercises with explicit body area, movement, equipment and
  measurement for programming and logging. Existing custom identities/history stay readable.
- Edit or regenerate future programming when circumstances change. Earlier, completed and
  active prescriptions remain protected; previous versions and actual sets are preserved.
- Review explained programme replacements for one dated workout or eligible future
  occurrences. Each phase keeps its targets, load references clear, and changed measurement
  tasks need fresh targets. Gym Now's today-only equipment alternatives remain separate.
- Review optional progression based on recent comparable working evidence and recorded
  effort. Save a dated proposal or keep the target; activation remains a separate choice.
- Browse read-only accepted programme revisions and their original dated prescriptions.

## Improvements

- Current authored programmes appear in Strength Today, hybrid Home, programme details,
  Calendar and planned Gym Now execution through the existing workout engine. Starting a
  prescription never fills in actual sets. Spontaneous sessions require explicit association.
- Previews preserve every generated week, target, phase, note and explanation. Programme
  trade-offs and evidence use disclosures; dates remain chronological and use UK conventions.
- Programme construction respects equipment, exclusions, experience, time and useful hybrid
  run context without changing running plans. Exact set/rep rules are original product
  heuristics, not individual physiological guarantees.
- Exercise discovery uses current authored inventory and the custom metadata supplied by
  the athlete. Existing favourites, exclusions, technique guides and PB evidence remain intact.
- Optional progression now presents the proposed target first, with readable light/dark
  styling, flowing controls and natural scrolling at 200% text.

## Fixes

- Exact reviewed draft/current-owner checks and fresh eligibility protect activation from
  stale reviews. Late exclusions or archived choices cannot silently enter a new programme.
- Forward activation waits for an unfinished native workout from that programme to finish
  or be explicitly discarded. Previewing future changes never stops a workout.
- Editing concrete targets recalculates the workout-duration estimate. Competing drafts stay
  distinct from accepted history and cannot overwrite it during restore.
- Reopening completed gym history no longer lets an empty active coordinator dismiss its
  association review. Display-clock ticks remain limited to unfinished workouts.
- City artwork readiness stays tied to the current city. Capture navigation now respects
  large-font accessibility labels, exact calendar-day identity, real alternative disclosures
  and departing-window lifecycle, retaining the original evidence assertions and deadlines.

## Known limitations

- Galaxy S24 outdoor recording, sensors/audio, real gym entry, backup/restore and human
  TalkBack acceptance remain unperformed. TEST_BUILD.md contains the real-device checklist.
- Beginner power previews explain the catalogue's lack of a beginner-classified ballistic
  movement instead of inventing power exposure or prescribing technical lifts.
- Custom movements get no invented technique, estimated 1RM or automatic progression.
  Progression reviews up to 20 recent logged sessions; the bounded window can conservatively
  report insufficient evidence. Numeric suggestions always remain optional.
- Generated previews support up to 26 weeks; manual authoring supports up to 52 weeks.
  These are product/resource bounds. Timers, supersets, generated warm-ups, analytics
  dashboards, achievements and deload intelligence remain outside this release.

## Data, privacy and licensing

- Schema 15 / JSON backup 9 retain typed programmes, drafts, decisions and custom metadata,
  adding concrete dated preview storage. The nested authoring section writes 2 and reads
  1–2; supported older root backups remain readable. Raw archive 2 and complete package 1
  are unchanged. Restore validates before transactional replacement.
- All 174 canonical exercise guides and 14 routine visuals remain unchanged. No artwork,
  runtime dependency, permission or third-party licence was added by the new controls.
- The app remains local-first, account-free and offline-capable. No Internet permission,
  backend, advertising, analytics, subscription or runtime LLM is introduced.

## Verification

- Source 1a7c683 / workflow 41912323900 passed 426 tests on EACH API 28/35/36, builds/lint,
  588,145 domain checks across 35 suites (including 2,430 programme configurations), 64 schema
  cases, nine release tests, formatting/source/provenance/guidance and 125 authentic captures.
  Schema 13→15 and supported old/current backup behaviour are covered by the SQLite suite.
- New replacement/progression/history screens were reviewed in light/dark/200% text. The
  first progression dialog failed visual review despite passing image tests; the refined
  proposal-first screen passed stronger entry/scroll assertions and actual visual review.
- See VERIFICATION.md for fingerprints, exact artifacts and following metadata CI. Emulator
  success does not imply physical Galaxy S24 acceptance or a stable production release.
