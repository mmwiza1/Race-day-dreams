# Race Day Dreams UI design standards

Apply these standards to every new screen and meaningful UI change. Use the existing [Material 3 foundation](MATERIAL3_UI.md) and the [owner-approved visual benchmark](RACE_DAY_DREAMS_DESIGN_BENCHMARK.md). The six mockups establish a quality bar for hierarchy, density and interaction. Adapt their patterns to the app’s identity, content and platform conventions.

## Mandatory design principles

| Principle | Apply it in production |
| --- | --- |
| Activity first | Put the athlete’s current task, progress and next action at the top. Set logging should feel like doing a workout. Live recording should be readable in motion. |
| Immediate visual hierarchy | Make the activity, key values with units, current state and primary action obvious at a glance. Give supporting context and secondary actions less weight. |
| Efficient repeated interactions | Keep related inputs together, minimise typing, preserve unfinished input and offer explicit reuse of earlier values. Show whether a set is suggested, edited, completed or saved. |
| Progressive disclosure | Keep essentials visible. Give optional effort metrics, notes, filters, advanced preferences and technical evidence clear labels and accessible disclosure controls. |
| High visual quality | Use the shared `CoachSpace` rhythm, typography and shapes. Align values, labels and actions. Prefer intrinsic heights and purposeful surface grouping. Keep cards and elevation restrained, and avoid long stacks of equal-weight controls. |
| Material 3 and Material You | Reuse shared components and semantic colour pairs. Preserve the brand, navigation destinations and icons, Dream Routes, dynamic and branded themes, and user-customisable discipline colours. Use text, checkmarks or radio indicators as well as colour to show state. |
| Accessibility and responsive layouts | Keep interactive targets at least 48dp and use readable contrast, meaningful names, focus order and state semantics. Reflow rows at 200% text. Check compact and expanded layouts, scrolling with the keyboard open, light and dark themes, and TalkBack or VoiceOver where available. |
| Functional and data integrity | Preserve workout history, recorded evidence, dates, plan links, run states, duplicate choices, backups and platform features. Keep destructive actions separate and confirm them by naming the affected item and offering cancellation. Communicate saving, success and errors, and preserve draft input when an action fails. |

Use Material radio controls for exclusive choices, chips for compact filters, menus for occasional actions and dialogs where confirmation is needed. Prefer the app’s polished flows, such as Exercise Library search and Programme suggestion, when choosing shared patterns.

## Screen patterns

| Area | Preferred presentation and behaviour |
| --- | --- |
| Today / Home | Show the next activity and its start or review action first. Summarise supported readiness or plan context and provide a way to explore it. |
| Running | Lead with distance, elapsed time and pace, with recording, pause and GPS states clearly visible. Show route or workout progress when useful and supported by actual evidence. Keep pause or resume easy to reach and protect Finish with confirmation. Preserve the recording lifecycle through view switches, interruptions and recovery. |
| Strength / Gym Now | Pair the exercise heading with previous performance. Group exercise-appropriate inputs in editable set rows and reflow them at large text sizes. Keep the next set action and finish or save distinct. Separate prior values, prescriptions and performed sets. Make editing and navigating among exercises clear. |
| Bouldering | Lead with grade, attempts and outcome. Make the next logging action clear and distinguish attempts, sends and flashes with text or semantics. Present recent climbs and session context in scannable rows. |
| Progress | Start with a trend or comparison, with date range, units and meaningful empty states. Offer drill-down to records and sources. Distinguish recorded, estimated and unavailable values, and provide a readable alternative to chart-only information. |
| Workout generation | Start with goal, equipment and focus. Keep optional preferences collapsed initially. Provide a full exercise preview, compatible alternatives and one prominent start action once the athlete has reviewed the workout. |
| Exercise Library | Keep search and common filters easy to reach. Use compact rows with meaningful labels and existing licensed thumbnails or neutral placeholders. Provide clear access to technique, history and other available details. |
| Settings | Use predictable preference groups, show current values and expose clear default or reset actions for discipline colours. Keep data management and destructive actions distinct from routine preferences. |
| Diagnostics | Lead with a readable status and useful event or error summary. Keep technical details and redacted JSON review and export available on request. Distinguish unknown status from a verified result. |

## Mandatory design acceptance questions

Answer each question for the actual rendered workflow. Record failed and unverified criteria with the review evidence.

1. **Task clarity** — Can a new user identify what to do without instructions?
2. **Visual hierarchy** — Is the most important information immediately apparent?
3. **Interaction efficiency** — Are repeated actions straightforward without redundant entry?
4. **Progressive disclosure** — Is technical complexity hidden until useful?
5. **Consistency** — Does it feel like the same application across disciplines?
6. **Visual quality** — Does the actual rendered result look polished and intentional?
7. **Accessibility** — Can users complete the workflow at enlarged text, with assistive technology and on compact screens?
8. **Functional integrity** — Are actions, persistence and error handling reliable?

## Visual acceptance checklist

- [ ] Open the actual rendered screen at compact width in light, dark and 200% text. Check expanded layouts, custom or dynamic colours, keyboard and focus behaviour where relevant.
- [ ] Inspect the default and relevant editing, selected, expanded, completed, error, empty and confirmation states. Scroll through the whole flow and verify controls remain reachable.
- [ ] Check hierarchy, spacing, density, typography, alignment, contrast, clipping and action prominence. Review before-and-after captures for an existing layout change and name the benchmark patterns used or adapted.
- [ ] Run focused functional and accessibility checks. Record the captures inspected, results, visual defects and unverified conditions. Visual sign-off requires inspecting the rendered result.

Use the existing build and screenshot tools for affected screens. This checklist adds no CI workflow.
