# Race Day Dreams 0.15.5-test1

A redesign and hardening build. Every main screen has a calmer, chart-first look, and this build
fixes the problems found in the November 2026 whole-app review: dates, units, Back behaviour,
onboarding and recording. It is also the first test build that is not debuggable. If you have
0.15.1-test1 or later, including any 0.15.4-test1 download, Settings › App updates can install this
version over it and keep your data.

## What's new

- Home shows today first: today's session sits in one clear hero with a single start action,
  then your next 7 days and what is coming up, with labelled days. Running + strength shows the
  gym session inside the same card.
- Progress is chart-first. Distance, time, pace and heart rate each have their own colour, over
  4 weeks, 12 weeks, 6 months or 1 year, and month labels show the year where it changes.
- Strength progression charts: exercise progression, training volume and muscle-group workload,
  with a labelled volume week and a quick return to this week.
- Plan and Calendar have a journey header, day panels, calendar marks and a compact key.

## Improvements

- History and run details are grouped into sections with splits, and times show in your local date
  and time instead of UTC.
- Gym Now, programmes, records, the exercise library and technique pages have compact headers.
  Filter chips fade at the edges, and equipment is written out as "Equipment: …".
- Recovery and training balance, weekly review, body and protein, health context, race day,
  bouldering and milestones match the new look. The race-day countdown reads "Today" on the race
  date, and milestones use discipline badges that screen readers announce with their date.
- Navigation: Back in the app bar and the system Back go to the same place, and screens no longer
  show a second Back button inside the content on Android. After choosing Strength focus, the bottom
  tabs appear straight away.
- Onboarding: goal and plan choices are full-width cards with a gold highlight when selected.
  Tapping one keeps your place on the screen and gives visible touch feedback.
- Accessibility: with very large text the bottom bar keeps its labels and the app title stays
  readable. App update status changes are announced to TalkBack, and "App updates" is a heading.
  The period selector on Strength and Bouldering shows the selection by more than colour.
- Units: if you choose miles or pounds, Home, the weekly review, race pacing, gym logging and
  warm-ups use them too, as do race prediction and strength analytics. Warm-up loads round to
  2.5 kg or 5 lb plate steps. Stored values are unchanged.
- Race day shows the closing pace that finishes on your target time.
- iPhone run details now show run comparison and run feedback, as on Android.

## Fixes

- Runs saved without a time-zone offset, such as Apple Health imports, now land on the same local
  day and week in Calendar, the weekly review, History and race prediction as on Home and Progress.
- The same run imported from two sources is counted once in the weekly review, the "Hold volume"
  limit and race prediction, as it already was in Progress. The next-7-days strip no longer counts
  it twice either.
- Android: "Finish & save" before the first GPS fix no longer leaves an unfinished run that blocks
  the next start.
- Android: opt-in auto-pause resumes reliably in town, where GPS accuracy is lower, and paused time
  is accounted for correctly.
- The Home race countdown no longer shows negative days after your race.
- Progress plan completion shows how many workouts you skipped.
- Back after saving a run no longer returns to the recorder.
- A bouldering timer left running for more than 24 hours stops at 24 hours and asks for the real
  time.
- iPhone: the recording clock keeps counting while a locked phone sleeps. This still needs a
  locked-screen check on a real iPhone.
- Release integrity: the downloadable Android build is no longer debuggable, so a computer connected
  over USB cannot read the app's private data with debugging tools. It has its own version number,
  so the in-app updater offers it, and CI now refuses to publish different app contents under a
  version number that has already been published.

## Known limitations

- The Samsung Galaxy S24 goal-selection retest is still waiting for a physical phone. The onboarding
  changes above address the most likely causes, but the original report stays open until that retest.
- Physical acceptance has not been performed: no checks on a Galaxy S24, Pixel, Xiaomi or iPhone,
  outdoor GPS, locked-screen recording, Health Connect, HealthKit, Bluetooth heart rate, audio,
  battery, rotation or a human TalkBack/VoiceOver pass. In-place updates are tested only on an
  emulator with throwaway test keys, not yet on a phone updating from 0.15.4-test1.
- This is the first non-debuggable build. Behaviour should be identical, but report anything that
  works differently from 0.15.4-test1.
- iPhone does not yet have every Android screen. Health trends, strength personal-best records and
  history, the long-run durability card and plan adjustment proposals are Android-only. Run now,
  unplanned runs and structured runs are only partly available. See PLATFORM_PARITY.md.
- Reference-load editors opened from the programme builder, programme generation and exercise swap
  still take kilograms; their field label says so.
- Run splits still leave out a kilometre that contains a pause.
- iPhone has no in-app updater; download new builds from the release page.

## Data, privacy and licensing

No new permissions, network access, accounts or analytics. SQLite schema 20, backup JSON 14, raw
recording archive 3 and complete backup package 1 are unchanged, and stored values stay in
kilometres, kilograms and seconds. Exported backups are still not encrypted, as Settings says; keep
them somewhere private. The Android build is signed with the same permanent release key as before.

## Verification

The exact final source must pass the Android API 28, 35 and 36 device matrix with the updated
screenshot set, the shared tests and the iPhone simulator, startup, UI and unsigned device builds.
Before publication, CI checks that the Android download is not debuggable, carries the release
signature and is a new version number.

## Previous release highlights

- 0.15.4-test1: a user-experience build: a first-run choice of focus, Progress that follows your
  focus, a simpler Today and grouped Settings.
- 0.15.3-test1: a quieter Android run notification, incremental live-route reading and faster
  dashboards with long histories; no change to GPS, heart rate or data.
- 0.15.1-test1: the first build signed with the permanent release key; later versions can be
  installed over it from Settings › App updates.
