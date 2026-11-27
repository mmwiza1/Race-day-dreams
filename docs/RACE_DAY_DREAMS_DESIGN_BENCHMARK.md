# Race Day Dreams — Visual Design Benchmark

**Status:** Owner-approved visual reference concepts, 27 November 2026.\
**Purpose:** Provide Codex, Claude, and future design reviews with a *consistent fitness-app quality bar* built on Race Day Dreams' existing Material 3 / Material You foundation.

## Reference images

| File | Screen concept | Design cues |
|---|---|---|
| [01_running_dark_navy_lime.png](design-reference/01_running_dark_navy_lime.png) | Live running, direction A | Hero distance metric; glanceable elapsed time and pace; useful route map; powerful pause/finish controls; dark navy atmosphere |
| [02_strength_dark_lime.png](design-reference/02_strength_dark_lime.png) | Strength set logger, direction A | Exercise name and previous performance together; dense, legible set table; visible completion state; distinct save action |
| [03_workout_builder_light.png](design-reference/03_workout_builder_light.png) | Gym workout generator | Goal and equipment first; visual exercise preview; clear substitutions; primary start action; advanced options tucked away |
| [04_running_dark_green_purple.png](design-reference/04_running_dark_green_purple.png) | Live running, direction B | Focused live stats with excellent contrast; restrained atmospheric colour; navigation and recording controls visible |
| [05_bouldering_dark_violet.png](design-reference/05_bouldering_dark_violet.png) | Bouldering session | Grade and attempts prominent; success/failure easy to scan; recent climbs and clear log action |
| [06_strength_dark_violet.png](design-reference/06_strength_dark_violet.png) | Strength set logger, direction B | Legible set-history workflow; one exercise focus; previous/current comparison; clear next-step controls |

These are conceptual visual references, **not screenshots of the current application**, pixel-perfect implementation blueprints, or authorised changes to navigation, core features, brand iconography, or theme defaults. Some mockups deliberately explore different palettes, and one is in light mode. The final product should use the established discipline-colour settings and adapt faithfully to both dark and light modes.

## Mandatory app-wide outcomes

1. **Activity-first**: A run looks like a recording experience; a strength workout looks like a workout, not a form; climbing presents grade, attempts, outcomes; analytics prioritises trends and insights.
2. **Immediate hierarchy**: Within a glance, a new user can identify the activity, key metrics, progress/selection state, and primary next action. Use visual emphasis deliberately, not uniformly.
3. **Repeat-task efficiency**: Reuse prior set values where explicitly requested, minimise typing, place related weight/reps controls together, and retain the user's progress and intent. Avoid repeated generic buttons and redundant explanatory text.
4. **Progressive disclosure**: Beginners see essential tasks first. Optional effort metrics (RPE/RIR), notes, advanced preferences and technical diagnostics remain easy to find without dominating primary workflows.
5. **High visual craft**: Balanced density, polished typography, consistent alignment, purposeful surfaces, legible values and units, restrained elevation, cohesive iconography. Avoid huge empty cards, long text-button stacks, excessive whitespace and bland web-form layouts.
6. **Material You identity**: Retain shared Material 3 tokens, existing discipline colours and user customisation, established bottom-navigation icons, Dream Routes and platform conventions. Do not arbitrarily copy the reference images' icons or exact accent colours.
7. **Accessible and responsive**: Appropriate labels and contrast, minimum 48dp touch targets, TalkBack/VoiceOver semantics, 200% text, compact devices, keyboard-open states, and both dark/light themes. Reflow compact tables where required; never solve density by shrinking touch targets or clipping text.
8. **Data and behavioural integrity**: Visual revisions must preserve workout history, recorded evidence, backups, dates, plan links, run states, duplicate choices and platform-specific functionality.
9. **Actual render acceptance**: Inspect on-device or emulator captures of default, editing, selected, completed, error, empty and confirmation states where relevant. A passing screenshot test alone is not visual sign-off.

## App-wide screen patterns

- **Today/Home:** next action and key readiness first, short supporting context, easy drill-down.
- **Live Running:** distance/pace/time, trustworthy recording state, map if useful, prominent pause/finish, safe handling of interruptions.
- **Strength:** exercise header + previous performance, editable set rows, clear add-set feedback, accessible finish/save.
- **Workout Generation:** goal/equipment/focus, generated exercise preview and alternatives, one obvious start action; advanced preferences collapsed by default.
- **Bouldering:** grade, attempts, outcome and recent sessions are immediately understandable.
- **Progress/Analytics:** trend, comparison, metric context and drill-down, not oversized empty tiles.
- **Exercise Library:** search, filters, clear technique content and well-structured details.
- **Settings:** predictable grouped preferences and colour controls. Conventional settings design is appropriate here.
- **Diagnostics:** human-readable issue and event summary above optional technical detail and redacted export.

## Design acceptance rubric

A reviewer must answer all eight questions from the owner's accepted quality philosophy:

| Criterion | Acceptance question |
|---|---|
| Task clarity | Can a new user identify what to do without instructions? |
| Visual hierarchy | Is the most important information immediately apparent? |
| Interaction efficiency | Are repeated actions straightforward without redundant entry? |
| Progressive disclosure | Is technical complexity hidden until useful? |
| Consistency | Does it feel like the same application across disciplines? |
| Visual quality | Does the actual rendered result look polished and intentional? |
| Accessibility | Can users complete the workflow at enlarged text, with assistive technology and on compact screens? |
| Functional integrity | Are actions, persistence and error handling reliable? |

For future changes, compare **actual before/after renders with these reference images**, state which patterns were carried over and explain where platform constraints justified a different approach. Don't blindly optimise for visual similarity at the expense of usability.

## Integration guidance (for a future owner-authorised change)

Place the reference images under an appropriate `docs/design-reference/` directory and link this benchmark document from `docs/UI_DESIGN_STANDARDS.md` and `AGENTS.md`, without changing runtime assets. Do this separately from an in-flight release if the owner wishes to avoid CI churn. When asking Codex or Claude to design a screen, provide the source image files or repository paths and require a before/after visual comparison.
