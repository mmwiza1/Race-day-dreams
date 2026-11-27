# Race Day Dreams UI design standards

These standards apply to every new screen and meaningful change to an existing screen. Use the app’s [Material 3 foundation](MATERIAL3_UI.md) and compare with an existing polished flow such as Exercise Library search or Programme suggestion.

## Design and interaction

- Give each screen one clear purpose. Start with the information needed to act. Put values and units in scannable summaries, with supporting explanations close to the decisions they inform.
- Make the primary action obvious. Give secondary actions less weight. Use Material 3 radio controls for exclusive choices, chips for compact filters, menus for occasional options, and dialogs for irreversible actions. Avoid columns of equal-weight text actions.
- Group related content with shared `CoachSpace` tokens, typography and shapes. Keep related spacing compact. Use intrinsic heights and add cards only where they clarify grouping. Keep core actions within reach on compact phones.
- Use theme-derived surface, content and accent roles. Show selection through text, a checkmark or a radio indicator as well as colour. Show what was saved or what is currently in effect.
- Place destructive actions away from routine choices. Confirm them in a dialog that names what will be removed and offers a clear way to cancel.
- Keep controls at least 48dp, with readable contrast, meaningful labels, and correct selection and disclosure semantics. Allow wrapping and natural scrolling at 200% text. Review light and dark themes and check focus order where relevant.

## Visual acceptance checklist

- [ ] Open the actual rendered screen at a compact width in light, dark and enlarged text.
- [ ] Inspect the default screen and relevant selected, expanded, error, empty and confirmation states.
- [ ] Check hierarchy, spacing, density, typography, alignment, contrast, clipping and action prominence. Scroll through the whole flow and verify that every control remains reachable.
- [ ] Inspect before-and-after captures when changing an existing layout and compare the result with a polished app screen. Passing tests and screenshot generation support this review. Visual sign-off requires inspecting the rendered result.
- [ ] Record the captures reviewed, the focused tests run and any remaining visual defects or unverified conditions in the implementation report or handoff.

Use the existing build and screenshot tools for the affected screens. This checklist adds no CI workflow.
