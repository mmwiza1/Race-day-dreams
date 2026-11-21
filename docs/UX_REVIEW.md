# UX simplification and content hierarchy review

Proposed for owner review. **Not merged.** Base: `main` at `b1e99333` (0.15.3-test1). Before images in
[`ux-review/before/`](ux-review/before/) are the canonical captures of that base, taken from source
`c94071ea`. After images in [`screenshots/`](screenshots/) are captured by CI on the
API 35 emulator from this branch's exact head, using the same fixture data. Nothing on either side
is a mock-up.

## Usability problems found

1. **Home had no single next step.** Running Today showed up to four filled buttons: Start with Live
   Coach, Log strength, Start strength workout and View strength session. It also showed eight more
   outlined or text actions, two of them duplicating bottom tabs. The race card, the Run now card
   (its heading repeated its own button) and the Recent progress card all had equal weight.
2. **Strength Today repeated its own actions.** Start workout and Log today's workout were both
   filled. "Gym now" appeared as a heading and again as a filled button in a full-width explanation
   card.
3. **The first-run choice had four competing filled buttons** in three tinted cards. The Hybrid card
   used the race colour.
4. **Onboarding front-loaded concepts.** Before the first question, step 1 showed a philosophy line,
   a full-width "Why? / Learn more" button and a General Fitness explanation, even when that goal
   was not selected. Step 2 asked every new runner to choose a training strategy. Step 3 asked for a
   long-run day, schedule style and units next to the three essential questions.
5. **Progress put small print in permanent view.** The weekly hero showed three secondary lines. The
   chart added an axis sentence, a legend sentence and a "tap a bar" hint, and repeated the selected
   week in prose under the selected-week panel. Strength-focused people saw only running first.
6. **Settings was a single page about 14 screens long.** Health Connect, backups, updates,
   reminders, privacy and 13 licence buttons were all permanently expanded in one column, with
   filled buttons for routine navigation.

## Principles applied

- **One primary action per screen region.** Filled buttons are kept only for the day's planned start
  (or create a programme when there is none). Spontaneous starts are tonal. Navigation is plain rows.
- **Progressive disclosure, never removal.** Explanations, methodology, advanced choices and
  licences are one labelled tap away. Every control, figure and disclosure on the base still exists.
- **Today answers three questions in order:** what am I doing, how do I start, and what do I need to
  know. Anything else follows.
- **Context by focus.** Strength-first people see lifting first in Progress, hybrid people see both
  summaries before running detail, and running-first people are unchanged.
- **Existing Material 3 tokens only:** the colour roles, 4/8 dp spacing, shapes and type scale
  already used. No new colours or decoration. Touch targets are 48 dp minimum, rows 56 dp and
  section headers 64 dp.

## Screen by screen

| Screen | Before | After | What changed |
| --- | --- | --- | --- |
| Home (running) | ![](ux-review/before/home.png) | ![](screenshots/home.png) | Today card: a "Today's run" label, workout name and one fact line (distance · effort · pace range), then **one** filled Start with Live Coach. "Your planned session" removed. The race becomes one compact tappable row. |
| Home, scrolled | — | ![](screenshots/today-scrolled.png) ![](screenshots/today-more.png) | "Start something else": **Run now** and **Gym now** as equal tonal buttons. Insights only when relevant. Coming up and More are plain rows. View plan removed (it duplicated the Plan tab). |
| Home dark / 200% | ![](ux-review/before/home-dark.png) ![](ux-review/before/home-large-font.png) | ![](screenshots/home-dark.png) ![](screenshots/home-large-font.png) | At 200%, Run now and Gym now stack instead of squeezing. |
| Strength Today | ![](ux-review/before/strength-home.png) | ![](screenshots/strength-home.png) ![](screenshots/strength-today-scrolled.png) | Start workout is the only filled action; Log today's workout is outlined. Three exercises, then "+ n more". The Gym now card becomes one tonal button plus one sentence. Coach advice now sits above This week. |
| First run | (not previously captured) | (covered by `SharedStartingPathTest`) | Four equal choice cards (title, one line, chevron) replace four filled buttons. Labels, order of actions and routing are unchanged. |
| Onboarding step 1 | ![](ux-review/before/onboarding.png) | ![](screenshots/onboarding-material-light.png) | The goal is the first question. The privacy and safety lines become small notes. The Zone 2 explanation appears only when General Fitness is selected. Disclosures are text buttons. |
| Onboarding step 2 | — | ![](screenshots/onboarding-approach-light.png) | Goal mode and target time stay visible. Training strategy shows the suggested choice with "Change training strategy". It opens automatically when the saved strategy needs review. |
| Onboarding step 3 | — | ![](screenshots/onboarding-week-light.png) ![](screenshots/onboarding-week-options-light.png) | Runs per week, running days and gym days stay visible. Long-run day, schedule style and units are under "More options". Review still lists every value. |
| Progress (running) | ![](ux-review/before/progress.png) ![](ux-review/before/progress-dashboard-trends.png) | ![](screenshots/progress.png) ![](screenshots/progress-dashboard-trends.png) | The hero card has two lines under the number. The chart key, axis sentence and selected-week prose move into "How to read this chart". The selected-week panel announces changes as a live region. |
| Progress (strength focus) | ![](ux-review/before/progress-dashboard-empty.png) | ![](screenshots/progress-strength-focus.png) | A strength summary (workouts, working sets and new records for the selected period) comes first and opens Strength analytics. Hybrid people see it after the running week. |
| Strength tab | ![](ux-review/before/strength.png) ![](ux-review/before/strength-dashboard-analytics.png) | ![](screenshots/strength.png) ![](screenshots/strength-dashboard-analytics.png) | Gym now stays filled, with Log workout and Gym programme as tonal buttons directly under it. Recent workouts move above the 1RM and muscle charts. The New records tile uses shorter text. Bouldering is still shown only to people who climb, plus a More detail row. |
| Settings | ![](ux-review/before/settings.png) ![](ux-review/before/data-sources.png) | ![](screenshots/settings.png) ![](screenshots/settings-training-open.png) ![](screenshots/data-sources.png) | Six collapsible sections: **Training**; **Health and connected data**; **Reminders**; **Appearance and updates**; **Data and storage**; and **Privacy, safety and licences**. Each header states what is inside. Licence and source buttons sit behind "Third-party licences and route sources". |

Dark and 200% captures for the changed dashboards and onboarding steps are in
[`screenshots/`](screenshots/): `*-dark.png` and `*-large-font.png`.

## Where advanced functionality moved

| Item | Was | Now |
| --- | --- | --- |
| Training strategy (onboarding) | Always shown, step 2 | Step 2 → Change training strategy (auto-open if review needed) |
| Long-run day, schedule style, units | Step 3 | Step 3 → More options |
| Chart axis range and legend | Always shown under each weekly chart | "How to read this chart" under the chart |
| Extra coaching observations (Home) | Up to three lines | First shown; others under "More from this week" |
| View plan (Home) | Button | Removed; the Plan tab is one tap away |
| Activity data source, permissions, weight | Settings page | Settings → Health and connected data |
| Primary focus, race goal, baseline, shoes, health context | Settings page | Settings → Training |
| Appearance, app updates | Settings page | Settings → Appearance and updates (installer hand-offs stay active while it is closed) |
| Backup, storage, diagnostics, delete data | Settings page | Settings → Data and storage |
| Privacy, fitness guidance, licences | Settings page | Settings → Privacy, safety and licences → Third-party licences and route sources |
| Log workout, Gym programme (Strength) | "More detail" list | Tonal buttons under Gym now |

## Unchanged by design

- The five bottom destinations, icons, order and labels, plus the branding, launcher icon and
  Material 3 theme.
- Training algorithms and calculations, the database schema (20), backup formats, Health Connect and
  HealthKit integration, recording and Dream routes.
- Onboarding draft recovery, validation, step order and every saved field.
- The calendar, plan, live recording and run detail screens.

## Accessibility

- Every primary and secondary action is still a real button with its full text label. iOS UI tests
  tap the same labels.
- Section headers are headings with Expanded/Collapsed state descriptions. Rows merge their text for
  TalkBack and VoiceOver, and decorative chevrons and separators are hidden from both.
- Large text: Today's quick starts stack at 1.5× and above. KPI tiles already went to one column.
  Rows have no fixed heights.
- Contrast uses the existing colour roles only. Secondary text uses `onSurfaceVariant` on
  surface-family containers.
- Not done: no human TalkBack or VoiceOver pass and no physical-device review.

## Compromises and concerns for review

- **More taps in Settings.** Every control is one extra tap away. Settings opens with all sections
  collapsed, and a section only stays open while you remain on the Settings screen; leaving it
  closes them all again.
- **Home → More is a list of eight rows** at the end of the page. It is low emphasis but still
  long; splitting it into Progress and Tools groups is an option.
- **The first-run screen has no screenshot**, because the fixture-based capture suite always starts
  with a profile. `SharedStartingPathTest` covers every choice in light, dark and 200%.
- **iOS** shares the Home, Strength Today, first-run, Progress and Strength changes, and Settings
  gets the same sections. iOS onboarding is a separate screen and is unchanged.
- **No version bump or release.** If approved, decide whether to ship it as a new test build
  (versionCode 27) or together with other work.

## Deferred (separate PRs)

- Replacing text chevrons and "▾/▴" with Material Symbols, which would need a new icon dependency
  and a licence review.
- A real settings search, and remembering which Settings sections are open between visits.
- Native iOS onboarding parity with the simplified Android steps.
- Turning the Android onboarding (a native View form) into Compose. It is large and risky, and is
  not needed for this simplification.
