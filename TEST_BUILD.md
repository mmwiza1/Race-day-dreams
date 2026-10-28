# Galaxy S24 evaluation — 0.8.2-test1

0.8.2 adds illustrated city heroes, landmark posters and clearer equivalent-distance
progress for all five Dream Routes while preserving the merged 0.8.1 engine. Its 20
bundled WebPs total 1,472,726 bytes. Schema/backup formats, runtime dependencies and
permissions are unchanged; the app still has no Internet permission. PRs #10 and #11
are complete. Consult VERIFICATION.md for current source, CI and APK evidence. Source
automation and visual review passed; final-head/main publication is checked separately.
Physical checks remain unperformed.

Download `Race-Day-Dreams-0.8.2-test1-debug.apk` from a successful intended-commit API-35
artifact, or `Race-Day-Dreams-0.8.2-test1.apk` from the rolling development release after
publication. Export a complete backup first. Debug signing keys may differ; preserve
backup before any reinstall. The initial 0.8.2 APK measured 47,960,610 bytes (+3.358%);
the verified source APK size is 47,976,994 bytes (+3.393%). The rolling main release
supplies its final download checksum after publication. No physical test below has been
performed by Codex.

## 0.8.2 focused visual and compatibility checks

Record device/Android/One UI version, APK commit, date and observations. In airplane mode:

- [ ] Open London, Berlin, Paris, Tokyo and New York: illustrated hero and landmark cards
  load locally; full course remains visible; blue equivalent section is immediately clear.
- [ ] Verify Paris names the annual Marathon de Paris 2026. Check older activities retain
  their original route ID, start offset, distance and equivalent section after upgrade.
- [ ] Check completed/remaining/total distances and percentage, including a marathon-length
  or longer test activity. Display rounding must not alter recorded GPS or stored values.
- [ ] Inspect light/dark mode and 200% font: important text wraps, landmark cards can stack,
  map remains useful, controls retain touch targets, and progress is clear without colour.
- [ ] Use TalkBack: city artwork is clearly decorative/atmospheric rather than a geographic
  landmark-position claim; map, progress, summary and landmark cards have useful alternatives.
- [ ] Reopen several activities and scroll repeatedly; observe responsiveness and memory use.
  Only the selected city's artwork should be needed. Check the size of the installed update.
- [ ] Complete-backup/restore an expendable profile containing old and new assignments;
  verify geometry and actual GPS samples survive unchanged. Do not discard the original backup.
- [ ] Compare the installed screens to the owner-supplied four-city premium 0.8.2 reference.
  Recognisable, atmospheric artwork and hierarchy matter; merely displaying all fields does not pass.

These checks remain **UNPERFORMED** until a human records the result. The existing
recording/Health Connect/BLE/audio/long-run checklist below also remains unverified.

## Phone checklist

1. Upgrade from 0.7 (or an older supported version) with existing data; check goals, plans, runs, shoes, notes,
   strength history and reminder preferences. Export/restore the new version-seven JSON backup and complete package1 and verify manual historical PBs, weights, breaks and decisions survive.
2. Continue without Health Connect. Review a concise baseline, optional missing
   fields, goal cards, weekday selections, date picker and final review. The
   explicit Create my programme action saves your goal and creates a programme.
   Edit/help/back/rotation/process recreation must retain intended inputs.
3. Connect Health Connect with denied, partial and revoked permissions. Check
   Samsung Health has actually synchronised its runs. Optional weight permission
   is requested separately and must not be needed for running or manual weights.
4. Schedule a Sunday race with Tue/Thu/Sat normal availability. Try six-week,
   normal, one-year and three-year race horizons. Review the early foundation,
   race phase and taper. Far-away races must not produce months of hard work.
5. Inspect Weekly coaching review from Home. It should often suggest no change.
   Review raw evidence and previews before accepting a bounded hold/reduction or
   quality-pace review. Keep original must preserve the plan and goal. Report a
   training break and inspect the optional easy bridge; this is not medical advice.
6. Compare a controlled easy run with a harder-than-planned run, and a deliberate
   race/time trial at RPE 9–10. Inspect score components separately from RPE.
   Unplanned runs must show analysis rather than invented plan adherence.
7. Use Calendar List/Week/Month; check remembered view, multiple sessions,
   run+strength, race, completed/moved/skipped sessions, labelled day details and
   expandable Calendar key. Move/skip a future session, check version history/reminders,
   and verify missed mileage is not added elsewhere.
8. Use the Strength primary tab. Log separate load/reps/RPE fields, add/duplicate/
   remove/edit sets, multiple/custom exercises and an unplanned session. Open a
   routine strength date in Plan/Calendar and verify the actual log stays linked.
   Switch kg/lb and compare unchanged canonical loads. Delete source evidence and
   confirm automatic PBs are recalculated.
9. Add historical 1/5/10-rep PBs with exact, approximate and unknown dates. These
   are explicitly historical results, not fabricated completed sessions. Log a
   better recorded set and retain both histories. Estimated 1RM must remain
   labelled approximate and exclude unsuitable/high-rep/bodyweight sets.
10. Enter/edit/delete kg/lb body weights and inspect Week/Month/3 Months/Year/All.
    Sparse measurements remain points; connecting lines are not daily estimates.
    Health Connect import is bounded by readable history (29 days without extended
    history permission, 84 with it), handles updates/deletions in that window and
    never overwrites manual records. Older provider measurements are retained.
11. Detailed splits/durability require real samples. Health Connect totals alone
    must not create them. Only Health Connect is supported; no direct provider setting remains.

No demo records are installed by the production APK. Canonical screenshots are
rendered by instrumentation-only fixtures. No account, analytics, backend,
Internet permission, map/Wear feature or runtime LLM is introduced. Native GPS is explicit.

## Refinement checks on the Galaxy S24

- Compare List/Week/Month and restart the app to confirm the selected view persists.
  Check E/Q/L/S/R/– category labels, outlined planned/filled completed markers,
  moved/skipped labels, and run + strength on one date. Blank dates outside a plan
  must not gain Rest markers. Selected/today/outside-month dates must remain distinct.
- Inspect all weight ranges, tap points, compare textual values and switch kg/lb.
  No measurements should appear on missing days. Lines only connect retained points.
- Open an exercise progression: exact/approximate dates retain labels, unknown dates
  stay outside chronology, and current best may come from either source. Delete
  evidence and confirm the derived best recalculates. Estimated 1RM remains approximate.
- Add/duplicate/edit/remove sets, switch exercises, review blocks, then save a session.
- Review weekly positives/attention and no-change outcomes. For a meaningful change,
  inspect both prescriptions and dates before accepting; cancel must leave the plan alone.
- Check normal/200% fonts, dark mode, portrait scrolling, keyboard/date-picker behaviour,
  TalkBack labels and vector navigation on the physical phone. CI does not prove these.
- Direct Samsung scaffolding is removed; Health Connect remains supported. No direct-provider setup
  or proprietary components are introduced.

## 0.5 phone checks

Export a 0.4.1 backup before upgrading. Check retained goals, workouts, shoes, manual
PB dates and weights. Try RDL/OHP/BSS aliases, favourites, timed planks and carries;
inspect units and previous sets. Generate a programme using your actual equipment
and key runs; inspect/reject/accept changes and stop generation without losing
actual sessions. Review diagrams with a qualified coach. Test TalkBack, 200% fonts,
dark mode, keyboards, scrolling and reminders on the Galaxy S24. CI cannot prove
real coaching efficacy, OEM reminder timing or Health Connect synchronisation.

CI debug builds may use a different debug signing certificate from an older download.
If Android rejects an in-place installation, retain/export the old app's backup before
considering uninstall/reinstall. Restore the backup and re-import Health Connect data.
No production signing key or credential is bundled in this repository.

## 0.6 Prompt 1 — Galaxy S24 health/recording evaluation

- Samsung Health → Health Connect → RDD: allow sleep/RHR/HRV/steps selectively. Check
  actual dates/origins, source changes, missing HRV and mirrored providers. Deny/revoke
  each permission; manual plans/runs/strength must remain usable. Compare sleep-session
  duration cautiously (it may contain awake time). No readiness number should appear.
- Outdoors, start GPS recording explicitly; check notification, precise-location denial,
  screen lock, opening another app, a 2–4 hour run and battery usage. No tracking should
  occur after finish. Check smoothed pace and compare measured distance with a known route.
- Pause for several minutes, move while paused, resume: elapsed/active time differ and
  distance does not bridge the pause. Test a tunnel/GPS off/on, poor fixes and return.
  Check genuine interpolated splits after 1 km, save, restart and inspect history/samples.
- Keep auto-pause disabled initially. If testing it, check long stationary stops, hysteresis,
  slow climbing/recovery jogging/crowds and degraded GPS. Manual pause must not auto-resume.
- Kill the process or reboot during a staged run where practical. Recover only retained
  evidence; downtime must not appear as recorded movement. Test explicit discard separately.
- Pair a standard BLE HR monitor if available; disconnect/reconnect it during a run. Check
  events, missing HR and uninterrupted GPS. Deny sensor permissions; cadence remains absent
  without a supported step sensor. Do not assume a phone carried differently counts every step.
- Export GPX, reimport the unchanged file, check exact-route duplicate handling and samples/
  splits; malformed/no-time/DTD files must fail. Routes must not gain fabricated HR or accuracy.
- Export BOTH normal app JSON and raw recordings ZIP. Test restore on a spare/empty install,
  checksum corruption, conflicting merge, interrupted staging and retained sensor/events.
- Explicitly write a native run's totals to Health Connect after separate consent. Retry;
  verify one session/distance record and no duplicate reimport. No automatic location/health
  upload and no sleep write permission. Device tests must confirm platform client-ID behaviour.

CI/synthetic traces do not prove GNSS accuracy, OEM background survival, physical BLE,
provider interoperability, battery efficiency or coaching efficacy.

## Prompt 2 / 0.7 physical checklist

- Start a planned workout with Live Coach. Check the preserved structure, timer and
  genuine distance transitions; lock the screen/background UI, pause/resume, skip a
  step, finish early and complete normally. Verify recorded reps/recoveries and RPE
  remain distinct. GPS loss must suppress pace alerts, not invent distance.
- Enable spoken cues with an installed offline voice. Test phone speaker, wired/BT
  headphones, music ducking/restoration, denied focus/call mode, TTS disabled/missing
  language and haptics off/on. No stale backlog or continuous pace nagging should occur.
- Connect/disconnect/reconnect an HR strap during live execution. GPS/workout timing
  must continue without HR. Revoke location/BLE/health/notification permissions and
  review clear degradation. No signal may become fabricated fallback data.
- Inspect race countdown, separate target/range/confidence, hilly effort strategy and
  explicit race start. Check elapsed timing through pauses, half/remaining cues,
  genuine splits, manual finish, post-race snapshot and separate official chip result.
  Short-distance-only evidence must not create an unsupported marathon estimate.
- Compare normal/missing/low health combinations. A lone low sleep/HRV signal must
  not cancel training. Preview/reject/accept an easy alternative; only that future
  session changes, goal/availability stay unchanged and the earlier version remains.
- Open attached warm-ups/cool-downs and mobility. Check line figures, written cues,
  completed routine persistence and unchanged Calendar/adherence/load. Try Marathon
  vs 5K warm-up; no injury prevention or treatment is promised.
- Save several runs, inspect varied offline city-inspired Dream schematics, reopen
  and confirm stable assignments. They are not surveyed routes or a claim of location.
- Export complete backup ZIP (finish/recover active stage first); restore on a spare
  installation. Verify native samples, execution snapshots, official results, routines
  and Dream assignments. Corruption/conflicts must leave all previous data intact.
  Retain old JSON/raw archives until the new package restore has been checked.
- Test 2–4-hour screen-off/battery-saver runs, process recreation, notification controls
  and reboot recovery. Recovery saves only retained partial evidence and must not
  auto-start tracking after reboot. Synthetic replay does not prove physical battery
  behaviour or endurance. Last unflushed staging batch may still be lost.
- Check TalkBack focus/labels, 200% fonts, dark/light contrast, landscape and sunlight.
  Pause/resume/finish remain in a fixed footer; secondary details may scroll.

## 0.8 additional Galaxy S24 checks — not yet physically performed

- Export a complete0.7 backup; upgrade; verify all histories/assignments; restore both
  previous JSON and complete package. Export/restore0.8 workouts/preferences/checklists.
- Choose Record my runs on a fresh installation. Start free/quick/custom without a
  profile or Health Connect. Grant/deny location. Verify one recorder and foreground
  notification; lock/background for10–20min, pause/resume, GPS loss, finish/save.
- Author distance/time/open steps and repeats. Edit/reorder/duplicate/delete; save,
  reopen, repeat; manually complete open steps. Check TTS over music/calls and HR loss.
- Record an extra run beside planned training. Confirm no automatic completion; test
  explicit link and easy-alternative preview/keep/accept with original versions retained.
- Inspect actual route shape/aspect, km markers, pauses versus gaps, qualified elevation
  and Route Story. GPX round-trip and Health Connect write/re-import must not duplicate.
- Dream enabled/disabled/one/multiple/no cities; change settings and reopen old runs;
  old assignments stay fixed. Live Dream panel must not obscure Pause/Finish. Test offline.
- Favourite/unfavourite and search; inspect RDL/split-squat figures;6RM recalculation;
  load references must not change actual logged loads. Test low/missing effort.
- Race-week checklist persists; strategy still uses your target. Review scheduling
  preferences/trade-offs and unavailable race date; no hidden goal/availability edits.
- Export diagnostics; inspect exclusions. Check Today/navigation at normal and200% text,
  light/dark, TalkBack, rotation, keyboard and touch targets.
- Hardware/OEM: BLE disconnect/reconnect, cadence absence, battery saver,2–4hour run,
  screen off, process termination and reboot recovery. Automation does not prove these.

## 0.8.1 Galaxy S24 acceptance gate — not yet performed

Record date, Android/One UI version, APK commit, result and observations for each
check below. CI/emulators do not pass these checks. At least one meaningful real
outdoor run is required before calling the Android baseline locked.

- [ ] Back up the existing install with **complete backup**; upgrade/reinstall as
  required by the debug signing key. Restore and compare plans, PBs, shoes, strength,
  old Dream assignments and retained GPS samples.
- [ ] Outdoors: acquire GPS, explicitly start, record10–20min, lock screen, open
  another app, inspect foreground notification, pause/resume, cross a genuine1km
  boundary, finish/save, reopen route/splits. Check for duplicate distance/samples.
- [ ] GPS degradation/return: no invented connecting line or inflated distance;
  pace cues stop when evidence is unreliable. Do not deliberately enter danger.
- [ ] Unexpected closure/reboot where practical: retained run is offered with
  durable elapsed/active time and pause context; GPS does not restart secretly.
  Recover only the retained portion, verify original structured prescription and
  events. Discard only an expendable test run.
- [ ] A longer outdoor run: inspect battery, screen-off continuity and sample
  retention; separately evaluate a2–4hour run before relying on marathon recording.
- [ ] Health Connect actual available exercise/steps/sleep/HR/RHR/HRV/weight paths;
  deny and revoke permissions. Missing provider metrics stay unavailable. Confirm
  optional native write-back/re-import does not duplicate the run.
- [ ] Live Coach through phone speaker and Bluetooth headphones, with music playing:
  transitions, sustained pace cue, audio ducking, pause/resume, early finish and
  incoming-call behaviour. Missing TTS still leaves useful visual controls.
- [ ] Compatible HR strap if available: explicit connection, disconnect/reconnect,
  missing HR while the run continues. Mark genuinely unverified if no hardware.
- [ ] GPX export/re-import retains samples/splits/provenance and complete backup
  restores them. Diagnostic export excludes routes, health values, weight and notes.
- [ ] TalkBack focus/labels, light/dark mode and200% text on Plan, Calendar key,
  Live Coach controls, health summaries, shoes, chart and exercise/routine guidance.
  Check sunlight legibility, scrolling and all48dp+ actions.
- [ ] Compare **actual APK** Plan/Dream/RDL/Bench/Lunge/Ankle screens against the
  owner references. No schematic fallback counts as accepted exercise artwork.

Physical acceptance status: **UNVERIFIED**. Do not tick boxes from synthetic replay,
a screenshot, an interface existing, or a green GitHub Actions result.

### 0.8.1 guidance and geographic context additions

- [ ] Offline: browse several guides from each equipment class, timed holds and
  carries. Check whole figures/equipment, written cues and common mistakes. All174
  assets resolve automatically, but that is not an individual form assessment.
- [ ] Dream Routes: generate a new activity for each eligible city and reopen it.
  Check visible progress/distance, full route, section markers and numbered landmarks.
  Existing assignments must retain their old geometry. New Paris assignments must say Marathon de Paris2026 and follow the annual course via Vincennes, not the Olympic Versailles course.
- [ ] Open each geographic licence/source notice in Settings; check dark/200%text
  and TalkBack's route summary. No remote maps or image downloads should occur.

No box above has been ticked by Codex. Record actual device observations before
calling the physical baseline accepted.

### Offline geographic context checks

- In airplane mode reopen each city card: full course, highlighted equivalent
  section, source street/water/green-space context,5km ticks, start/finish and
  attribution should remain visible. New Paris assignments must use the annual Marathon de Paris2026; legacy assignments must retain their old identity.
- At200%text and in dark mode check complete landmark words, route contrast,
  progress/distance and readable source credit. This context is an overview,
  not navigation, not your actual GPS and not a claim of current certified routes.
- Changing city preferences affects future assignments only; reopen an existing
  run and complete-backup/restore it to verify its original assignment is retained.

These physical checks remain UNPERFORMED until a human records results.
# Strength Engine 2.0 focus checkpoint — manual acceptance pending

On Galaxy S24, test a fresh install with **Focus on strength**: create a programme,
log actual sets, rotate, close and relaunch, and open Plan without creating a race goal.
In Settings, switch Running / Strength / Hybrid and verify existing race goals, running
plans, gym versions, history and unfinished goal drafts remain intact. Export a complete
backup, change focus and restore it; check the original focus and programme return.
Running-only users must be able to record or set a race goal without gym setup.
These physical checks remain unperformed; CI emulator results are recorded separately.

## Strength Engine 2.0 durability — Galaxy S24 acceptance pending

All checks below are **UNPERFORMED**. Record the intended APK commit, Android/One UI
version, date, result and observations. The current branch connects Gym Now and live
actual-set recording to the typed storage foundation; its own automated UI/API gate is
still pending an observed result. Use the specific APK under test rather than assuming
the main development download contains this draft work. A repository test or green
emulator run is not a device result. See [Gym Now](docs/GYM_NOW.md) for implemented
controls and the remaining Strength Engine 2.0 phases.

- [ ] Back up the existing install before upgrading from schema 10/JSON envelope 7.
  Compare retained gym programmes, actual sets, favourites and actual rep PBs after
  migration. Restore an older supported backup on an expendable installation.
- [ ] Start a planned workout, enter actual sets different from its targets, and close
  and reopen the UI. Targets and original programme revision stay unchanged; the saved
  loads/reps/effort are exactly those entered. A warm-up is not working-volume/PB evidence.
- [ ] Exercise all six Gym Now paths: planned, quick, build, repeat, favourite and
  generate. Also start an open workout. Review generated targets before starting,
  and verify no race goal, Health Connect permission or network connection is required.
- [ ] Start spontaneous/quick/custom/repeated/favourite/generated workouts.
  None completes today's programme merely by sharing its date. Repeats begin with empty
  actual sets and retain the accepted structure rather than copying prior performance.
- [ ] Save a set, background/lock the phone and recreate the activity. Reopen the same
  execution: no duplicate set, invented reps, lost committed actual or duplicate completion.
  Confirm a second unfinished workout cannot silently replace the first.
- [ ] Enter a load/reps or time/distance value different from the target. After Save,
  confirm the exact actual is visible and the input clears only after acknowledgement.
  If a save error is encountered, use the exact Retry action; values and set identity
  must be retained, and later changes must remain blocked until the pending write is
  resolved or saved evidence is explicitly reloaded. Do not fake a storage fault on
  irreplaceable training data.
- [ ] Pause, close unexpectedly and reboot where practical. Recovery explains retained
  timing and an interruption gap; it does not restart a clock secretly or claim the
  missing period was continuous training. Confirm safe partial finish and discard on
  separate expendable workouts.
- [ ] Finish a planned workout, then change the current programme. The actual still
  refers to the revision/session used at start. Spontaneous evidence remains independent
  unless an explicit supported association is accepted.
- [ ] On an expendable programme version 1, start a planned session, then accept a
  version 2 programme change through the existing edit flow. Return to the active
  workout and finish it: its source must remain version 1 and the original session.
  Separately, change a displayed programme before starting its session; the stale
  start must request a review instead of silently starting different targets.
- [ ] Recover a paused stage after unexpected closure/reboot where practical. It
  must remain paused until Resume is chosen. Check previously accepted sets, any
  Android-restored unsaved input, no duplicate sets and no invented active time.
- [ ] Finish a prescribed workout with missing working sets. Confirm the early-finish
  choice, save only actual performed sets and check the partial-completion indication.
  On a separate test choose Keep training. A spontaneous workout must not complete a
  planned session without an explicit supported association decision.
- [ ] Accept an edit to a completed actual, check PB recalculation, then complete a backup
  round trip. The edited actual is current, while the original execution evidence and
  prescription are preserved. Deleting an expendable completed actual must not erase a
  previously retained repeat template or unrelated PB/history evidence.
- [ ] Export current JSON envelope 8 and complete container 1; restore on an expendable
  installation and compare saved revisions, favourite choices, sources, completed actuals,
  edits and unfinished-stage recovery. Keep the original backup until comparison succeeds.
- [ ] Try replacement restore with a local unfinished gym workout. It must explain the
  required finish/discard action and retain that stage. Use a deliberately corrupted copy
  of a backup: failed validation must leave all current records intact.
- [ ] On a substantial history, scroll summaries and open individual workouts repeatedly;
  note responsiveness and any memory/process problems. Do not infer a peak-memory or
  multi-year performance pass from one fixture or emulator result.
- [ ] Check light/dark mode, 200% text, TalkBack focus and useful save/recovery error
  announcements. No load suggestion may replace an actual entered value, and no recovery
  action may require network access.

Schema 11 / envelope 8 repository and backup results are recorded in VERIFICATION.md;
the connected Gym Now UI checkpoint needs its own observed automated API28/35/36 gate.
Outdoor recording, Health Connect, Bluetooth HR,
TTS/music/calls, battery and other earlier S24 checks remain separately unperformed.

## Reduced 0.9 retained feature acceptance — Galaxy S24 UNPERFORMED

- [ ] Choose Strength without any running/race setup. Open Today → Gym Now, save an open
  workout and verify actual load/reps remain exactly entered. Check Running and Hybrid
  remain available without replacing existing data.
- [ ] Browse/search an exercise, star it, then explicitly exclude it. Check Excluded,
  reason text and search badge; restore it. Favourite, history, technique and PB evidence
  must remain. Old Hide from My Exercises must not silently become an exclusion.
- [ ] Generate a new programme/one-off with an excluded movement; it must not be selected.
  Existing frozen workouts remain unchanged. Unsatisfied choices must explain the conflict.
- [ ] Use Equipment busy/unavailable during a live workout. Review the explained 2–4
  alternatives, confirm Just for today, verify reference load/input clears only after save,
  and that earlier actual sets keep their exercise while the programme stays unchanged.
- [ ] Finish a spontaneous workout near a compatible programme session. Before any choice,
  verify it is extra. Review Count toward session and its exact programme version/actual;
  confirm, reopen from Gym Now's recent/repeat list and explicitly Keep as extra instead.
  Actual values, original prescription and spontaneous source must remain unchanged.
- [ ] On expendable data, edit a counted actual and change the current programme version.
  Reopen the workout, refresh evidence and reverse its association; retained decisions
  and old programme versions must survive. Never perform fault injection on valuable logs.
- [ ] Back up and restore current data on an expendable installation, comparing exclusions,
  favourite exercises/workouts, explicit association choices, actual edits and programme
  history. Confirm old supported backups restore and unsupported/corrupt copies fail safely.
- [ ] Verify this flow in dark mode, 200% text and TalkBack. Continue the existing outdoor
  recording/locked-screen/recovery/HC/BLE/TTS/battery checks above. No physical result is
  inferred from synthetic traces or emulator/API tests.
