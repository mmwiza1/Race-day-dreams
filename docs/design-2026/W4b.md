# W4b — Gym, exercise library and records visual modernisation

Presentation only. Programme generation and progression, prescriptions, rest timer, workout
execution and recording, the exercise catalogue and how-to artwork are unchanged.

## What looked dated

- **Gym Now:**
  - A pink `SemanticCard` hero that differed from the Strength tab's `ScreenHero`.
  - Four identical outlined buttons stacked as cards.
  - "Favourite workouts" and "Repeat previous workout" were bare text links.
- **Programme overview** (both authored and generated programmes):
  - A plain heading.
  - Each block was a pink tinted card.
  - Sessions were long single-line text links ("date · title · about N min") or outlined cards.
- **Generated programme preview** ("Review every block") and the builder's programme preview: the same pink block cards.
- **Strength records:**
  - A pink card per exercise and a grey card per estimate.
  - Labels, figures and dates all at similar weight.

## What changed

- **Gym Now overview:**
  - The overview is now a strength `HeroSurface` (tertiary, dumbbell glyph, "Gym Now" eyebrow) with "Train your way" as the display title.
  - The single strength-accent primary action is "Start open workout" (`GymStartAction`, which can be disabled, unlike the frozen `HeroPrimaryAction`).
  - The other choices are glyph-led option tiles (`GymOptionTile`) under two labelled sections, "Choose a workout" and "Saved and recent". Favourites and Repeat previous are now tiles too, rather than bare links.
  - Saving, unfinished-workout and error states still render above the hero, because they block starting.
- **Gym Now sub-sections:**
  - Each sub-section opens with a compact `ScreenHeader` (eyebrow "Gym Now", title = section).
  - Planned sessions sit under a programme `SectionLabel` (the programme name, with the version line as its caption). They are listed as readable rows in one borderless `Panel`: the date as a plum eyebrow, the title, the exercise count, the exercise names, the expected time, and a tonal "Start this session".
  - Favourites and recent workouts are panels with tonal start actions.
  - Generated and quick suggestions are a labelled section with divided exercise rows. The start action is the strength accent.
- **Authored programme overview:**
  - A `ScreenHeader`, then the revision and the date range.
  - Each block has a `SectionLabel` header (block or phase name; dates and weeks as the caption).
  - The block's sessions are rows in a borderless panel ("N exercises · about N min"), and each whole row opens the session.
- **Generated gym programme:**
  - A `ScreenHeader` ("Gym programme"), a goal `SectionLabel` with the version and dates.
  - An "Upcoming sessions" panel of rows with exercise counts. The exact "date · title" text is kept, because journeys select it.
  - "Your gym preferences" is now a section label.
- **Programme previews:** the block cards became `SectionLabel` plus `Panel`. The exact session line text is kept, because tests select it.
- **Strength records:**
  - A `ScreenHeader` ("Personal bests" / "Strength records").
  - Each exercise is a labelled section ("Recorded performance" caption), and its best figures lead in bold plum.
  - Estimates sit in a separate outlined, untinted panel, so they read as calculations rather than recorded evidence.
  - Progress bars in the measured history use the plum chart hue.

## New internal components (`strength/GymComposition.kt`, candidates for promotion)

- `GymGlyphDisc`: a plum glyph disc.
- `GymStartAction`: the strength-accent start button, with an enabled state.
- `GymRowAction`: a tonal tertiary row action.
- `GymOptionTile` and `GymOptionTiles`: glyph-led choice tiles, at least 72 dp tall.
- `GymSessionRow` and `GymSessionLink`: dated session rows.
- `GymTextLink`: a fixed-text row link.
- `exerciseCount`.

The app-only screens (`GymProgrammeScreens.kt`, `StrengthRecordsScreen.kt`) reuse the public shared components and one private row helper, because `internal` is not visible across modules.

## Accessibility

- Glyph discs and chevrons are decorative and hidden from screen readers.
- Section titles and hero titles are headings.
- Option tiles are single merged touch targets labelled by their text, at least 72 dp tall. Session rows are at least 56 dp and row actions at least 48 dp.
- Disabled tiles are dimmed and report as not enabled.

## Compromises and remaining inconsistencies

- **Exercise library** (`SharedExerciseBrowseScreen`, `SharedExerciseDetail`, `ExerciseLibraryControls`, technique screens): not changed in this PR, to keep it reviewable. It still needs section labels or panels and chip consistency.
- **Workout builder, programme builder setup, progression sheet, history sheet, templates list, rest timer, warm-up, superset and equipment-alternative sheets:** not restyled. Their `SemanticCard` blocks remain.
- **Strength workout detail screens** (`SharedAuthoredProgrammeWorkout`, `GymSessionScreen`): left to W4a.
- **Generated programme setup:** the chips are unchanged.
- **Unfinished-workout and pending-save warning cards:** they keep `SemanticCard`, because they carry status rather than decoration.
