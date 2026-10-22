# Galaxy S24 evaluation — 0.3.0-test1

Branch: `codex/production-coach-test-build`. Draft PR #2. **Do not merge.**
Base main: `7624a662c5a52b6cd0ab427c09e17b354b50c62e`.

## What to try

1. Export any existing app data first. Install the named debug APK from a successful
   workflow's API-35 artifact. If an older debug installation has a different
   signing key, Android may require uninstalling it: preserve your backup first.
2. Continue without Health Connect. Review optional manual baseline details, choose
   a goal/date/mode/strategy and realistic running/gym availability. Alternatively
   allow the minimal Health Connect running permissions and review imported evidence.
   Verify Samsung Health has actually synced runs into Health Connect.
3. In Plan choose Generate my programme. Inspect this week, future weeks and all
   phases. Open a workout and check purpose, structure, effort and explanation.
   Your chosen goal is preserved. Future sessions must not appear as completed.
4. Compare plans for a beginner 5K, a short-horizon marathon and general aerobic
   fitness. The short marathon should explain limited preparation rather than
   rapidly increasing distance. Do not treat generation as confirmation you can
   safely complete the race.
5. Record a past/today planned run with its **actual** distance, start/date,
   duration and optional RPE. No GPS, HR, splits or laps are invented. Prefer
   linking an imported run in its detail page rather than creating a duplicate.
   Review Run Score components and feedback. A controlled easy run should score
   well; going harder should not earn extra credit. Without RPE/splits there may
   be insufficient evidence for a numeric execution score.
6. In Progress open optional Strength. Add multiple exercises and sets, a custom
   exercise, different rep ranges and added-load bodyweight work. Check chronological
   PB history and approximate 1RM for suitable weighted sets. Delete a session and
   confirm unsupported records disappear. Runners with no strength data should
   see only a secondary optional entry.
7. Log a hard lower-body workout before an upcoming quality run. Review any
   proposal in Plan. Keep original, choose an easier session, rest or use a feasible
   move. The goal and unaffected workouts must stay unchanged. A rejected proposal
   should not return for identical evidence. No recommendation prevents running.
8. Change goal/availability. Confirm the saved programme remains unchanged until
   you explicitly approve Rebuild remaining programme. Old linked prescriptions
   should remain in completed run history. Rebuild once on the same day after
   recording a session: the completed prescription must keep its original targets.
9. Enable evening reminders and Android notifications. Move/rest a workout through
   a proposal and confirm the reminder is updated/cancelled. Test actual delivery
   under Samsung battery restrictions, reboot, time-zone and daylight-saving changes.
   Timing is approximate; this is not an exact-alarm promise.
10. Export to a user-selected JSON document. Restore on a clean installation and
    verify goal, strength, plan history, responses, shoes and manual runs survive.
    Health Connect source records require re-import; app-specific annotations/links
    are preserved. Try a damaged copy: validation should leave existing data intact.
11. Try Android dark mode, largest font/display size, TalkBack, rotation, app process
    recreation and system Back while entering data. Check readable labels, scrolling,
    keyboard visibility and touch targets. Report any form draft loss.

## Known boundaries

- This first deterministic coach is conservative and heuristic; no clinical or
  real-world performance efficacy has been demonstrated. Thresholds/weights are
  documented, not scientific assurances.
- The generator currently uses workload/endurance/consistency and optional target
  pace; sophisticated multi-PB pace modelling, readiness and race prediction remain
  pending. Maximum Performance uses evidence-supported controlled work without
  pretending to know the runner's maximum race capability.
- Adaptation initially modifies one upcoming quality workout for specific supported
  evidence. Automatic multi-week rebuilding for interruption/fitness changes is
  pending; an explicit rebuild is available. Manual move/customise-any-session
  editing is not complete. No missed-session catch-up or automatic goal changes.
- Imported runs must be explicitly linked to a preserved prescription for adherence
  scoring. Rep/recovery response fields exist; the completion UI does not yet enter
  detailed rep results or map provider laps to prescribed intervals automatically.
- Strength session/default-definition editing and kg/lb display conversion are
  pending UI work. Current strength entry uses clearly labelled kg. Custom exercises
  and session deletion work; existing session definitions are immutable snapshots.
- Calendar remains an actual-activity list; Plan provides the generated weekly
  view. Month-grid calendar, maps, GPS recording, GPX runtime, Wear OS and NHS C25K
  are outside this iteration.
- No new Health Connect permission for gym is requested. Strength logging is manual
  and works without Health Connect. Missing HR/sleep is not a blocker.
- Instrumentation captures demonstrate rendered code on emulators. They do not prove
  Galaxy S24 accessibility, Samsung Health interoperability, OEM notification timing,
  real GPS behaviour or coaching effectiveness.

## Report a problem

Include APK source commit, Android/One UI version, steps, expected/actual behaviour,
and whether Health Connect was enabled. Avoid posting identifiable health records
in public issues. A backup contains personal information; do not attach it publicly.

Backup envelope version 2 includes coaching data and prevents old app versions
from silently discarding it. This reader also restores original version-1
backups. Core store format 1 and coaching section version 1 are independently
validated. Export retains the existing 8 MiB/complexity limits: exceeding them
fails clearly before truncating a destination, without deleting local history.
