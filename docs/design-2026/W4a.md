# W4a — Workout history and activity details

Presentation-only modernisation of recorded workout history, the run/activity detail, its splits
and the post-run sections, following `docs/DESIGN_2026.md` §3f and §3g.

## What was dated

- **Run detail:** a plain "Run detail" heading and date line, then a large primary-tinted summary
  block with small labels, so the figures did not lead. Heart rate, route, route story,
  measurements, comparison and feedback were all tinted cards of the same weight. The long-run
  durability card was nested inside the measurements card.
- **Splits:** each split was a separate full card with a grey bar. Nothing named the fastest or
  slowest split.
- **History (Calendar → List):** a flat list of identical cards. Strength and runs differed only by
  text ("Strength ✓"), with no discipline cue.
- **Workout history (with filters):** a flat list of cards with the date as a small label.

## What changed

- **Run detail header:** `ScreenHeader` in the running colour, with the start date and time as the
  eyebrow and "Run detail" or "Assessment run" as the heading.
- **Headline figures:** "Recorded summary" is a glyph-labelled section. Its borderless panel leads
  with elapsed time (display size, time hue), then distance (distance hue) and elapsed average pace
  (pace hue) side by side. The figures stack at 150% text and above. Every figure is a merged
  label-and-value node. Moving time and the elapsed-time explanation are kept word for word.
- **Sections:** each section is a `SectionLabel` (glyph disc in its measure hue, bold heading)
  above a borderless `Panel`. There are no nested cards.
  - Recorded heart rate: coral.
  - Your recorded route and Route story: blue.
  - Source and measurements: teal. This is a new heading for the previously untitled card.
  - Long-run durability: violet, now its own section.
  - Run Analysis / Run Score and feedback: violet.
  - Distance splits: violet.
  - Recorded laps / intervals: teal.
  - Planned and actual: blue.
  - Shoe and your notes: blue.

  Every disclaimer and explanation is unchanged.
- **Splits:** one panel holds the splits as rows (a readable table). Each row has the split number,
  a "Fastest" or "Slowest" tag in words, the recorded distance, time and pace, and a violet pace bar
  (strong for the selected and fastest rows), then the sample-based extras. The selected row is
  tinted and labelled "Selected". Fastest and slowest are named only among complete splits, and
  only when there are at least two. The pace values, the bar scaling and the selection behaviour
  are unchanged.
- **Comparison:** "Compare this run" is a quiet, full-width disclosure row with a running glyph
  disc and an Expanded/Collapsed state. The comparison panel shows current and previous as labelled
  columns with a pace-hue rule. Their texts are unchanged.
- **Feedback:** a borderless panel with bold sub-headings.
- **History (Calendar → List):**
  - "Strength history" and "Recorded runs" are glyph-labelled sections, with runs grouped under
    date headings (screen-reader headings).
  - Rows are borderless, with a discipline glyph disc (running RUN, strength DUMBBELL) and a
    chevron. Each row is one ≥64 dp target.
  - Run rows show readable figures with the measure named in words ("7.2 km distance",
    "0:30:00 elapsed", "4:10/km elapsed average pace"), then the source and the plain status
    "Completed · tap for details".
  - Strength rows read "Strength workout", "50 min · 1 exercise" (pluralised correctly), the
    exercise names and "Completed".
- **Workout history (filters):**
  - A `ScreenHeader` that takes the colour of the selected kind (strength → strength colour,
    otherwise running).
  - Rows are grouped under month headings. Rows are borderless and discipline-glyphed.
  - The All/Running/Strength chips, search and date filters, run type, "Clear filters", paging and
    their semantics are unchanged.
- **iOS run detail:** the shared sections are no longer wrapped in an outer card, so there are no
  nested panels.

## New components (`W4aComposition.kt`, candidates for promotion)

- `DisciplineGlyphDisc`
- `MeasureFigure` and `MeasureFigureRow`
- `FigureTag`
- `RecordedWorkoutRow` and `RecordedWorkoutSummaryRow`
- `HistoryDateLabel`
- `RowDetail`

They are public, not `internal`, because the Android run-detail and workout-history adapters live
in the `app` module.

## Compromises

- **Workout history groups by month, not by day.** Each row keeps its own date. The existing
  instrumented test (`Release05UiTest`) finds a workout by its date inside the clickable row, and
  day groups would repeat the date in every row.
- **Hues for non-measure sections:** `SectionLabel` requires a `ChartHue`, so source, notes and
  planned/actual use blue (running/route) or teal. Feedback and durability use violet, because they
  describe pace execution.
- **No heart glyph:** the frozen `Glyph` set has no heart, so heart rate uses the trend glyph in
  coral.
- **Strength workout detail:** `gym-workout-detail` is a planned programme session in the Gym and
  programme screens (another workstream), not recorded history, so it is unchanged here.
- **iOS:** the detail keeps its "Recorded run" heading. iOS was not compiled locally.

## Remaining inconsistencies

- `StructuredRunSection`, `DreamRouteSection`, `SpontaneousAssociation` and `AdjustmentCard` keep
  their existing cards. Dream routes are deliberately distinctive.
- The live recorded route (`SharedLiveRecordedRoute`) and the live-run screens are untouched.
- The "Activity calendar" title stays a plain heading under the Calendar chips, which the Calendar
  workstream owns.
