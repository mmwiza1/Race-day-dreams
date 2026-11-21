# Race Day Dreams 0.15.4-test1

A user-experience build. Each screen now leads with what matters most: Today shows one clear way
to start, Progress follows your training focus, and Settings is grouped into sections. Every
feature, figure and setting is still there, and your data is unchanged. If you already have
0.15.1-test1 or later, Settings › App updates can install this version over it, keeping your data.

## What's new

- A new first-run screen with four clear choices: train for a race, record my runs, focus on
  strength, or running + strength.
- Progress follows your focus. Strength-focused training leads with workouts, working sets,
  records, the estimated 1RM trend and muscle-group workload. Hybrid training shows running and
  strength side by side, and running stays exactly as before for runners.

## Improvements

- Today: your planned session sits in one card with a single start button, the next sessions
  follow directly underneath with their distances, and Run now and Gym now sit side by side.
  Other shortcuts are grouped into "Your training" and "Log and adjust".
- Strength-focused Today names the two ways to train: "Start planned workout" and "Start
  unplanned workout". Unplanned workouts stay separate from your programme unless you link them.
- Onboarding asks about your goal first. Training strategy, long-run day, schedule style and
  units are optional "Change" and "More options" choices; the review step still lists every value.
- Settings is grouped into Training, Health and connected data, Reminders, Appearance and
  updates, Data and storage, and Privacy, safety and licences. The section you opened last
  reopens while the app stays open.
- Strength's Log workout and Gym programme sit directly under Gym now, and recent workouts come
  before the charts.
- Recent workouts show "Training volume (load × reps)" with thousands separators, for example
  "2,100 kg", so it is not mistaken for a weight lifted or a personal best.
- Average pace says how many runs it is based on when only some runs qualify.
- Chart keys and longer explanations open on request ("How to read this chart").

## Fixes

- The first-run choice no longer shows Back or Settings in its top bar.
- Settings no longer offers a Settings button in its own top bar.

## Known limitations

- These changes were checked with automated tests, Android emulators and the iPhone simulator, not
  yet with a human screen-reader pass or on a physical Samsung Galaxy phone or iPhone.
- iPhone onboarding uses its own screen and is unchanged in this build.
- Settings sections open with one extra tap, and they all start closed when the app is
  restarted.

## Data, privacy and licensing

Unchanged from 0.15.3-test1: no new permissions, network access, accounts or analytics. SQLite
schema 20, backup JSON 14, raw recording archive 3 and complete backup package 1 are unchanged.
Training calculations, GPS recording, Health Connect and HealthKit are unchanged.

## Verification

The exact final source must pass the Android API 28, 35 and 36 device matrix with the updated
screenshot set, the shared tests and the iPhone simulator, startup, UI and unsigned device builds.
The GitHub homepage now shows current emulator screenshots of this build.

## Previous release highlights

- 0.15.3-test1: a quieter Android run notification, incremental live-route reading and faster
  dashboards with long histories; no change to GPS, heart rate or data.
- 0.15.2-test1: weekly run counts no longer count the same run twice; onboarding review buttons
  stay usable; clearer race distance.
- 0.15.1-test1: the first build signed with the permanent release key; later versions can be
  installed over it from Settings › App updates.
