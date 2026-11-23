# W4c — Recovery, race day, bouldering and supporting screens

Presentation only. No coaching, recovery, milestone, PB, protein, health, bouldering grading or analytics,
or persistence logic changed. Every string and test tag that existing tests rely on is kept.

## What looked dated

- **Race day:** a plain heading with three loose text lines, then stacked tinted cards: goal, capability and
  one grey card per pacing stage. The race itself had no identity, and the pacing strategy read as
  unrelated boxes rather than a sequence.
- **Recovery & training balance:** a plain heading, then a pink strength-tinted card and a blue card with
  small titles inside. The guidance headline had the same weight as its supporting text.
- **Bouldering:** this screen mirrored the old Strength layout: a plain heading, a filled button, an
  outlined secondary button, period chips, a loose KPI grid and tinted cards. The grade bars used the
  running (primary) colour.
- **Milestones, body & protein, health context, weekly review, movement support:** plain headings and
  equal-weight tinted cards, with titles inside the cards.

## What changed

| Screen | Now |
| --- | --- |
| Race day | **Hero:** an expressive running-colour `HeroSurface` with a flag glyph. It shows the race name as a display title, the countdown and date, and the goal and target average as large `StatRow` figures. **Estimated capability:** a teal, glyph-labelled section with the range as a strong headline; the disclaimer is kept. **Pacing strategy:** a labelled panel. The hilly-course option is a clean row, and the stages form a **numbered timeline** (`StageTimeline`): accent discs joined by a rail, the km range as a label, the pace as the figure and the guidance below. "Prepare / start race" is the single accent action, and "Race warm-up" and "Add or view official result" are quiet `ActionRow`s. **Race week:** the checklist, taper sessions and preparation are labelled sections, and checklist items are clean 48 dp rows (`CheckRow`). |
| Recovery & training balance | **Header:** a compact `ScreenHeader` in the recovery (secondary) colour. **Guidance:** a borderless panel with a bold headline, "Here's what to do" in the recovery accent, and full-width actions: preview in the recovery accent, keep as outlined. "Why this guidance?" and "Running & gym schedule" are glyph-labelled sections; the disclaimers are kept as quiet notes. |
| Bouldering home | **Hero:** the V3 Strength composition. A strength (tertiary) `ScreenHero` titled "Bouldering", with "Start bouldering session" (or "Resume session") as the primary action and "Log a past session" as the secondary. The unsaved-session or loading state is the hero's supporting line. **Sections:** "Your climbing" (label, period chips, KPI panel), then "Grade distribution", "Hardest send by week", "Recent sessions" (with "See all" trailing) and "Grades", each as a plum label above a borderless panel. Grade and progression bars now use the plum strength hue instead of the running primary. |
| Bouldering session, review, detail, history | **Live session:** the timer sits in a strength hero. Problems, notes and KPIs are labelled panels. |
| Recorded milestones | **Header:** a compact header. **Milestones:** each is a panel led by a decorative discipline badge (run, dumbbell or calendar) with a bold "✓ title" and the recognition date in the discipline accent. The celebration setting is a labelled section. |
| Weekly review | **Header:** a compact header (Android and iOS hosts). **Your week:** a labelled section with a bold state headline, the week's distance as a strong figure in the distance hue, and the completion line kept verbatim. **Recommendation:** a labelled section. |
| Body & protein | **Header:** a compact header. "General daily protein" and "Your measurements" are labelled sections, with the protein range and the latest height as strong headlines; source and disclaimer lines are kept as quiet notes. |
| Health context | **Header:** a compact header (Android). The trend cards are borderless panels with a bold metric title and value, and "Consider an easier option" is a labelled section with an outlined preview button. |
| Movement support | **Header:** a compact recovery header, then a "Routines" label above borderless routine panels, each with a recovery-accent start button. |

## New internal components (`W4cComposition.kt`, candidates for promotion)

- **`StageTimeline` / `TimelineStage`:** numbered stages joined by a rail. Each stage is read as one item, and the numbers and rail are hidden from screen readers.
- **`CheckRow`:** a full-width, at least 48 dp checkbox row with the `Checkbox` role.
- **`QuietNote`:** receding supporting text for disclaimers and captions.
- **`DisciplineBadge`:** a decorative discipline glyph on the discipline's container colour.

## Compromises

- `ChartHue` has no recovery hue. Recovery-type sections (guidance, capability, protein, milestones) use
  **teal**, the time and balance hue. The discipline colour (secondary) carries recovery in headers and
  actions.
- Bouldering's hero uses the TREND glyph, because the frozen `Glyph` set has no climbing glyph.
- The race hero carries no action. "Prepare / start race" follows the hilly-course option, because the
  start uses that choice. Placing the action in the hero would invite starting before choosing.
- "No planned sessions remain before the race." is shown when the race-week taper list is empty. Before,
  that section rendered with a title and nothing under it.

## Deferred / remaining inconsistencies

- **Running PBs and records** (`running-pbs`, `running-records-*`) are still on their earlier layout. They
  share code with the W4b records screens, so they should follow that workstream's records treatment.
- **Dream routes:** untouched by design. Their artwork, basemaps, route data, readiness and publication
  logic are unchanged, and their surrounding chrome is already distinctive.
- **Routine player:** `SharedRoutineContent` (mobility and warm-up execution) is unchanged. Its
  illustration-led layout and step progress already fit its purpose.
- **Weight graph:** the Android `WeightSection` keeps its plain heading.
- **iOS health context:** the iOS health-context host and the iOS milestone and race hosts were not edited
  beyond the shared content they render.
- **Captures:** no screenshots were regenerated locally (there is no emulator here). The coordinator's
  screenshot cycle produces the after-state for `race-day`, `training-recovery*`, `training-milestones*`,
  `bouldering-*`, `body-*`, `health-*`, `weekly-review` and `mobility*`.
