# Race Day Dreams — 2026 product experience redesign

Status: **Phase A specification + Phase B demonstration (draft PR, not merged).**
Base: `main` at `9cf6774c` (0.15.4-test1). This is a presentation-layer initiative. No
algorithm, metric, schema, integration, recording, signing or update change is proposed.

## 1. Competitive UX benchmark

**Confirmed** means a public help article, official post or established review says so.
**Inferred** means a design pattern drawn from those sources or common practice, not a claim
about a specific screen. Nothing here copies another app's layout.

| App | Confirmed | What Race Day Dreams adopts (principle, not layout) | Screens that benefit |
| --- | --- | --- | --- |
| Runna | Training Calendar showing completed, upcoming and changed sessions; drag-and-drop rescheduling within the current or next week ([help centre](https://intercom.help/runna/en/articles/10137793-how-to-use-and-manage-your-training-calendar)). A Home tab with "next run and plan", and redesigned Record and Progress, are announced without dates ([Wareable](https://www.wareable.com/running/inside-runna-app-plan-changes-new-features-academic-review-loughborough-stanford-steph-kessell)). Plan safety changes from 12 Nov 2026 ([the5krunner](https://the5krunner.com/2026/11/12/quick-read-runna-plan-updates/)). | **The week's structure and completion at a glance** before any detail; the next session as the hero. | Today (all focuses), Plan, Calendar, planned workout |
| Hevy | A PREVIOUS column with last-time values inline while logging; per-exercise History tab; exercise stats with heaviest weight, projected 1RM, best set, session volume and set records ([help centre](https://help.hevyapp.com/hc/en-us/articles/36011896355479), [features](https://www.hevyapp.com/features/exercise-performance/)); sets per muscle group per week ([features](https://www.hevyapp.com/features/sets-per-muscle-group-per-week/)). | **Previous performance beside the current input**, a compact set table instead of stacked form fields, and progression charts one tap from an exercise. Race Day Dreams already has the data (Last time, History, PBs, 1RM estimate with its recorded/estimated distinction). | Gym workout, Log strength, Exercise detail, Strength records |
| Garmin Connect | The 2024 redesign split Home into richer sections: an "In Focus" area of up to five chosen panels, "At a Glance", and "Edit Home" to reorder or remove cards ([Garmin](https://www.garmin.com/en-US/blog/fitness/new-garmin-connect-mobile/), [DC Rainmaker](https://dcrainmaker.com/2024/01/garmin-connect-through.html)). Reception was mixed: richer cards, but some missed the compact view. | **Prioritise by relevance and keep it compact.** Show what the person's focus needs first (already started in 0.15.4), avoid card walls, and treat personalisation as a later option rather than a requirement. | Today, Progress |
| Strava | Progress summary chart at the top of You › Progress with 1 week to 6 months ([support](https://support.strava.com/hc/tl/articles/28437860016141-Chart-na-Buod-ng-Progress)); sport-specific progress buttons for the athlete's top sports ([BikeRadar](https://www.bikeradar.com/news/strava-personalised-progress-chart/)); Weekly Snapshot criticised for combining sports' distances ([road.cc](https://road.cc/content/tech-news/stravas-weekly-snapshot-makes-me-feel-bad-307315)). | **One discipline at a time, with a period switch and a drill-down to the activities behind each figure**, and never summing running distance with strength. Race Day Dreams' dashboards already drill down; the redesign adds discipline tabs. | Progress, History, Activity detail |
| Material 3 Expressive | Variable type, a shape system and token-driven spring motion; new and updated components such as button groups and split buttons ([Android Developers](https://developer.android.com/design/ui/wear/guides/get-started/design-language), [M3 Expressive codelab](https://codelabs.developers.google.com/codelabs/mdc-102-objc)). The sources found are mostly about Wear OS; mobile specifics are inferred from the same system. | Emphasised type for the key figure, larger containers for the one hero per screen, connected button groups for choices, and restrained motion only where it explains a change. | Design system (all screens) |

## 2. Inventory and screen-by-screen recommendations

Priority: **P1** high usability impact and used daily; **P2** weekly; **P3** occasional or specialist.

| Journey / screen | Primary goal | Today (from current captures) | Recommendation (presentation only) | Reuse | Priority |
| --- | --- | --- | --- | --- | --- |
| Running Today | Know and start today's run | One card and one action since 0.15.4, but the week's shape is invisible and the figures are a text line | **Week strip** (done/planned/missed per day); a hero with three large figures (distance, effort, pace); dated Coming up rows; race countdown as a number | Plan, activities, run responses, CountdownCard | **P1 · Phase B** |
| Strength Today | Start the planned workout or train freely | Sentences for the weekly status; exercises as plain lines | Week strip; strength hero with figures (exercises, duration) and a numbered preview; "Your week so far" as figures | StrengthTodaySummary, gym schedule | **P1 · Phase B** |
| Progress (running, strength, hybrid) | See whether training is moving the right way | Focus-aware since 0.15.4, but one long page; small KPI values | **Discipline tabs** (Overview / Running / Strength training) opened at the person's focus; discipline-tinted heroes; KPI figures at headline size; every analytic unchanged | DashboardAnalytics, StrengthKpis, charts | **P1 · Phase B** |
| Plan | Understand the week and the block | Day cards with text status | Week header with completion ring or segments; session rows with type colour and duration; the block/phase as a quiet progress bar | WeekStrip, ScheduleRow | P1 |
| Calendar (week/month/list) | Find and reschedule sessions | Month dots are good; week and list are text-heavy, and the key is hidden behind "Calendar key" | One type-and-status mark language shared with the week strip (filled = done, ring = planned, muted = missed); always-visible mini legend; list rows with date badges | SessionMark, DateBadge | P1 |
| Planned workout | Know what to do and why | Long text; structure as a numbered list | **Interval bar** (segment widths by duration or distance, labelled), then steps; purpose and cues behind disclosure | Workout segments (existing data) | P1 |
| Live recording | Run without fiddling | Strong elapsed-time hero; the remaining-distance card is a pink block; explanations sit under the figures | Keep the layout. Apply type tokens (tabular figures) and the discipline accent to the progress bar, and keep large-text and paused states as they are | StatBlock | P2 (low risk; recording logic untouched) |
| Activity detail / run summary | Review what happened | Text explanations directly under each figure; the route is below the fold | Hero figures first, route second, then splits; methodology moved to "About these figures" | StatRow, existing route/split views | P1 |
| Splits | Spot pacing changes | Accurate cards with small bars | One horizontal pace-per-split bar chart with the selected split detailed; the same data and partial-split rules | Existing split model | P2 |
| History | Find a past session | Plain text list | Rows with discipline badge, date badge, the key figure and source; filters as chips | ScheduleRow pattern | P2 |
| Gym workout / Log strength | Log sets quickly and correctly | Stacked form fields; "Last time" as a separate block above | **Set table** (set · previous · weight · reps · RPE), with the previous value from existing "Last time" data inline; the prescription collapsed under the exercise name | Existing logging model, Last time | **P1** |
| Exercise library / detail | Choose and learn a movement | Good structure and tabs; the overview is text-heavy | Hero image and key facts; the progression chart first on the History tab; technique steps as a numbered list | Technique guides, records | P2 |
| Strength records / PBs | See progression | Cards correctly separate recorded from estimated | One chart per exercise with recorded points and estimated values visually distinct (filled vs hollow), keeping the labels | StrengthRecords | P2 |
| Programme builder / generator | Shape a programme | Many small text actions per block | Block cards with one overflow menu for edit, duplicate and remove; weeks as a timeline | Existing builder | P2 |
| Bouldering | Log climbs and see grades | Functional; grade distribution bars | Strength family colours with a grade ramp; the session log as quick-add chips per grade. Shown only to climbers, as now | ClimbingAnalytics | P3 |
| Recovery & health | Decide whether to adjust | Clear but long; options are tonal buttons | Lead with "what this means" and a confidence label (the evidence already carries it); supporting evidence collapsed | Recovery evidence | P2 |
| Dream routes | Feel progress towards a dream race | Distinctive and rewarding: map, landmarks, % complete | Keep it. Align the progress bar and figures with the type tokens; a larger milestone moment when a landmark is passed (static and accessible, no confetti) | Existing artwork | P3 |
| Milestones | Recognise real achievements | A list of check cards | A badge grid grouped by type, with evidence one tap away | TrainingMilestones | P3 |
| Weekly review | Accept or keep the plan | Several collapsible sections | Decision first (recommendation plus two actions), evidence below | Existing review | P2 |
| Race day | Execute the plan | Clear pacing card | Pacing as a split-band table; checklist as checkboxes | Existing pacing | P3 |
| Onboarding / first run | Get started | Simplified in 0.15.4 | Step indicator as a progress bar; choices as cards (as on first run); consider moving the Android native form to Compose later (large) | Starting path | P2 |
| Settings, backup, updates | Change something rarely | Grouped in 0.15.4 | Section icons and a search later; no structural change needed now | ExpandableSection | P3 |
| Empty / loading / offline / permission / error | Know what to do next | Mostly sentences | One **EmptyState** pattern (title, one sentence, one action); skeletons instead of "Loading…" text where content shape is known; permission rationale as the existing native page | New shared composable | P2 (with each screen) |

## 3. Design system (implemented in `RddDesignSystem.kt` for Phase B)

- **Foundation:** Material 3 colour roles, type scale and shapes only, so device colours (Material
  You), dark mode and 200% text keep working on both platforms. No new dependency.
- **Discipline accents:**
  - Running → primary roles; Strength → tertiary; Recovery → secondary.
  - Bouldering stays in the strength family; a fixed grade ramp is planned for its phase.
  - A discipline is never signalled by colour alone: it always has a label, and its state has a
    shape.
- **Type hierarchy:**
  - Key figures use `displayMedium/Small`, with the unit at title size in the same text, so
    screen readers read "12.8 km" once.
  - Titles `headlineLarge`, section titles `titleMedium` (as headings), labels `labelLarge`,
    context `bodyMedium` in `onSurfaceVariant`.
- **Containment:**
  - One **HeroCard** per screen region (discipline container, `extraLarge` shape, 20 dp padding).
  - Everything else is a flat `surfaceContainerLow` surface, an outlined group of rows, or plain
    space. No nested cards.
- **Components (shared, Phase B):**
  - `HeroCard`, `DisciplineLabel`.
  - `StatBlock` / `StatRow`: up to three figures, stacked at ≥150% text.
  - `WeekStrip`: done = filled dot, planned = ring, missed = muted ring. Every day has a
    spoken description.
  - `DateBadge` / `ScheduleRow`, `CountdownCard`, `SectionTitle`.
  - Restyled `KpiTile`, plus `ProgressTabs`.
  - Existing: `ActionGroup` / `ActionRow`, `QuickStartRow`, `ExpandableSection`, `Explainer`.
- **Charts:** existing charts keep their data and drill-downs. Later phases add the discipline
  accent and visibly distinguish recorded from estimated points (filled vs hollow).
- **Motion:** none added in Phase B. Later phases add expand/collapse animations and tab content
  cross-fade only, and respect the platform's reduced-motion setting.
- **Accessibility:**
  - Touch targets ≥ 48 dp (rows ≥ 56–64 dp).
  - Headings marked; decorative marks hidden from screen readers.
  - Large-text layouts stack rather than truncate.
  - Tab labels never repeat a bottom-navigation label (hence "Strength training").

## 3a. Data visualisation: chart-first analytics (owner review 1)

The owner asked for more visual ambition in Progress and for chart quality comparable to or better than Strava's. These principles apply to every chart in the roadmap:

1. **Chart first, figures second.** The trend is the headline. A scrub readout above the chart states the selected period's figure in display type. Supporting figures follow as a flat typographic strip with dividers, not a grid of cards.
2. **One surface, direct manipulation.** Tap or drag across the chart to select. The readout updates live, as a polite live region for screen readers, and the drill-down below lists the activities behind the selected bar, one tap each.
3. **Honest marks:**
   - bars for totals, a line for rates;
   - pace drawn so that higher is faster, and said so;
   - a baseline dot for "no runs" and a hollow ring for "unavailable";
   - "based on n of m" whenever only some sessions qualify;
   - recorded and estimated values always visually distinct.
4. **Accessible by construction.** Every bar or point is a separate selectable element with a spoken description. Colour is never the only cue. Charts grow taller at large text. Axis labels are minimal (top and bottom only), because the readout carries the exact value.
5. **No invented metrics.** Charts draw the existing weekly trend points, splits, heart-rate samples and strength records only.

**Better than a copy:**
- **Drill-down:** the readout plus the week's actual runs makes each bar explainable, and Strava's summary chart does not list the activities behind a week.
- **Duplicates:** copies of a run from other sources are noted ("also from 1 other source") rather than double counted.

**Implemented in Phase B for review** (`RunningTrendExplorer.kt`, Progress → Running and Overview):
- a Distance / Time / Pace connected button group;
- the scrub readout;
- the interactive weekly chart;
- the selected week's runs.

Strength progression now leads its tab with the estimated-1RM trend chart, and the figures follow as flat cells.

## 3b. Week strip clarity review (owner review 1)

The first version relied on dot styles alone (filled, ring, muted ring), so a new user could not reliably tell "missed" from "planned". Changes:
- a visible legend under the strip (Done ● · Planned ○ · Missed ⊘), drawn with the same shapes;
- a Run / Strength colour key when the week mixes disciplines;
- "missed" now has a diagonal slash, so it differs from "planned" by shape;
- the summary "n of m done" stays beside the title.

Screen readers already hear each day's full state, so the legend is hidden from them.

## 4. Representative journeys (synthetic data; checked against Phase B)

| Person | Onboarding → training → history → progress | Phase B effect | Still to address (later phases) |
| --- | --- | --- | --- |
| Beginner 5K runner (little history) | First-run choice → race goal → plan | Today shows the week (two or three rings), one hero and the run's figures. Progress opens on Running with honest empty states and "No runs recorded yet this week" | Planned-workout interval bar; an EmptyState pattern; a gentler plan header |
| Hybrid half-marathon + 3× strength | Hybrid choice → goal → gym days | The week strip mixes run (primary) and strength (tertiary) marks with done/planned/missed shapes. Progress opens on Overview with both heroes and both KPI sets | Calendar mark language; set table in the gym workout |
| Strength-only | Focus on strength → programme | Strength Today shows the week of gym sessions, a strength hero and figures. Progress opens on Strength training (KPIs, recent volume, 1RM, muscle workload); Running is one tab away | Set table with previous values; exercise progression chart first |

## 5. App-wide staged roadmap (each a focused PR with tests and real screenshots; none starts before approval)

Chart modernisation is explicit in phases V1–V4 below, and they run alongside the C phases that touch the same screens.

1. **Phase B (this draft PR):** design-system foundation, running Today, strength Today and
   Progress (running, strength, hybrid).
2. **C1 — Plan & Calendar:** WeekStrip and DateBadge in Plan; the shared mark language and a
   visible legend in Calendar week/month/list; planned-workout interval bar.
3. **C2 — Strength logging:** set table with previous values; collapsed prescription; rest-timer
   and warm-up controls restyled. The logging model is unchanged; run the gym persistence tests.
4. **C3 — Activity detail, splits & history:** hero figures, route, then splits as a bar chart;
   history rows with badges and filter chips.
5. **C4 — Exercise library, records & programmes:** progression chart first, recorded vs
   estimated styling, block cards with an overflow menu.
6. **C5 — Live recording polish:** type tokens and accents only. Recording logic is untouched;
   run the live, paused and large-text capture set.
7. **C6 — Recovery, weekly review, race day:** decision-first layouts and confidence labels.
8. **C7 — Dream routes, milestones, bouldering:** milestone moments, a badge grid and a grade
   ramp.
9. **C8 — Onboarding, settings, empty, loading and error states, iOS-only screens:** the
   EmptyState pattern and skeletons everywhere; iOS screens aligned with the shared components.

**Data-visualisation phases** (reusing `RunningTrendExplorer`'s chart, readout and drill-down pattern):

- **V1 — Running trends screen:** move the full Training trends screen (distance, time, pace, heart rate) onto the explorer, replacing `SharedWeeklyRunningChart`. Add the 12–52-week scale and month labels, and keep heart-rate "n of m runs" counts. Runs with C3.
- **V2 — Activity analysis:**
  - Splits become a horizontal pace-per-split chart, where the selected split shows its time, pace, heart rate and elevation, and partial splits are marked.
  - Heart rate over time becomes an area chart with recorded samples only, and gaps shown as gaps.
  - The route map and split chart share a selected split (tap a split to highlight its route segment), using existing route and split data.
- **V3 — Strength progression:**
  - Per-exercise estimated-1RM and heaviest-set charts on one axis: recorded as filled points, estimated as hollow, each with "from n sets".
  - Weekly working-sets chart per muscle group, reusing the existing attribution.
  - Session-volume history, labelled training volume.
  - Each point opens its workout.

  Runs with C2 and C4.
- **V4 — Hybrid and long-range:**
  - A combined week view with running time and strength sessions side by side, never summing distance with sets.
  - Year-in-review style monthly bars.
  - Bouldering grade-pyramid chart.

  Runs with C7.

Each phase reuses the Phase B components first, so visual changes propagate. Each runs focused
tests while developing and the full Android API 28/35/36 and iOS gates on its final head.

## 6. Phase B compromises and known limits

- **Week strip scope:** it shows the current Monday–Sunday week only; swiping between weeks
  belongs with Calendar (C1). Unplanned recorded sessions appear as done marks once the platform
  supplies local dates (Android and iOS both do).
- **Strength hero:** it counts recorded strength workouts since Monday (a count, not a new
  metric); working sets and records stay in the KPI grid.
- **No new motion and no icon dependency:** text chevrons remain until an icon decision is made.
- **Explorer and the full trends screen:** the explorer replaces the Progress chart only. The full Training trends screen still uses the earlier chart until V1.
- **Chart range:** the explorer shows the weeks of the selected period; for one year (52 bars) the bars become thin, and month labels come in V1.
- **Not yet done:** no human screen-reader pass or physical-device review.
