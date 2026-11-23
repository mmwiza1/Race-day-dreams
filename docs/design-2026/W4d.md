# W4d — Exercise library and technique visual modernisation

This workstream aligns the exercise library, the exercise detail tabs, technique guidance and
the gym workout builder with the approved expressive language (DESIGN_2026 §3f/§3g). These
screens were refined recently, so this is an alignment pass, not a redesign. Catalogue content,
how-to artwork and guidance, exclusions, favourites, search and filter behaviour, and persistence
are unchanged.

## What looked dated

- **Library:** the title was a plain headline with a bare "+" beside it. Results were outlined
  cards with 1 dp borders, and the result count was a small title with no section identity.
- **Refine search:** the body-area, movement and equipment chips floated as loose rows with no
  grouping.
- **Exercise detail:** the name was a plain headline with a star. Overview, history and PB entries
  were the generic tinted `CoachCard`, so PB figures did not lead. The custom-exercise "Save
  exercise name" was a second filled button competing with "Favourite".
- **How to do it:** the steps title sat inside a tinted card, unlike the rest of the app's
  labelled sections.
- **Workout builder:** a plain heading, tinted strength cards for each exercise, and a neutral
  tinted preview card.

## What changed

- **Library (`SharedExerciseBrowseScreen`):**
  - A compact `ScreenHeader` in the strength colour. The eyebrow is "Strength exercises", the
    title "Exercise Library" (or "Excluded exercises") is the single heading, and the
    "Add custom exercise" button is its trailing control (48 dp, spoken label unchanged).
  - The result summary ("A few useful starting points" / "N matching exercises") is a
    `SectionLabel` with a plum list glyph. It is still the accessibility heading.
  - Exercise rows are borderless 24 dp cards, led by the existing thumbnail and ending in the
    shared chevron glyph. Status ("Favourite · In your programme …") is in the strength accent.
  - The Refine search chips and the Excluded explanation sit in borderless `Panel`s.
  - "Explore all exercises" is a strength-tonal button instead of an outlined one.
- **Shared controls (`ExerciseLibraryControls`):** `LibraryOpenIcon` now draws the shared
  `Glyph.CHEVRON` (decorative, hidden from screen readers), so library rows match the
  expressive rows elsewhere.
- **Exercise detail (`SharedExerciseDetail`):**
  - A compact strength `ScreenHeader`. The eyebrow is "Strength exercise", the exercise name is
    the heading, and the favourite star is the trailing 48 dp control (content descriptions
    unchanged).
  - Overview details, history sessions and PB records are borderless `Panel`s. PB results lead in
    bold strength accent; provenance and date are secondary.
  - "Save exercise name" is an outlined button, so "Favourite" is the screen's one filled action.
- **Technique (`SharedTechniqueContent`):**
  - "How to do it" is a `SectionLabel` outside a borderless panel of the guide's own setup and
    movement steps.
  - Primary muscles and alternatives share one borderless panel.
  - The meaning-bearing tints of the Key cues (mint) and Common mistakes (caution pink) notes are
    unchanged.
- **Workout builder (`SharedGymWorkoutBuilderScreen`):**
  - A compact strength `ScreenHeader` titled "Build a workout".
  - The exercise summary is a `SectionLabel`, and each exercise is a borderless `Panel` with its
    order and name in the strength accent.
  - "Your workout preview" is an `ExpressiveSection`.
  - "Start this workout" remains the single filled action.

## Functional changes

None. Every text, test tag and content description that tests rely on is preserved. Each title
still has exactly one heading: "Exercise Library", "Excluded exercises", the exercise name and
"Build a workout". "How to do it" still appears twice on the technique tab (tab label and section
label), as before.

## Compromises and deferred items

- **Strength templates list ("Templates & generation"):** this lives in
  `SharedStrengthGenerationScreen.kt`, which W4b owns, so it is deferred to W4b or a follow-up.
- **Custom exercise form (`SharedCustomStrengthExerciseScreen`):** not changed, to keep this PR
  reviewable. It is still a plain form. A follow-up could add the same compact header.
- **Slot editor dialog** inside the builder, and the builder's empty-state `SemanticCard`: left
  as they are.
- **Library search field:** keeps its outlined M3 text field, because the outline is the focus
  affordance.
- **Unclassified custom-exercise dialog:** keeps its `AlertDialog` composition.
- **`CoachSectionTabs`:** in the frozen `CoachDesign.kt`, so it is unchanged.

## Remaining inconsistencies

- The GuidanceNote tints (mint and pink) predate the expressive palette. They carry meaning (cue
  versus caution) and are shared with other guidance screens, so a palette change belongs to the
  coordinator.
- History rows on the exercise detail tab still list sets as plain lines rather than a compact
  set table.
