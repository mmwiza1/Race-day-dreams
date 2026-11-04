# Strength Programming 0.10 — actual-build review in progress

## Source and captures reviewed

Source0eb56d887861be69d178ca3cc40dbc1500895a6b passed workflow41058603236:
387instrumentation cases on each API28,35,36, full build/lint and98canonical captures.
Canonical bot3deee3e585afa5184b48c7415c06b833271002af preserves that source's images.
All imported image blobs were checked against GitHub blob SHA; no images were edited.

The following actual production-navigation captures were opened: strength-programme-builder,
strength-programme-preview, both dark and200% variants, and strength-custom-exercise.
These are compiled app screens with typed test-only draft data, not mock interfaces.
The existing design references remain separate and unchanged.

## Findings and follow-up

Light/dark text and sage cards remain legible; large text wraps and scrolls rather than
shrinking. Four issues require the current follow-up and another real capture review:

- Expanded calculation assumptions displace useful targets. Move programme trade-offs and
  block emphasis into the existing optional disclosures; keep the full explanations.
- Recurring manual drafts retain weekday-grouped storage order. Render dated previews and
  current-programme details chronologically, preserving the stored objects and actuals.
- Section switches retain an old scroll position. Reset section scroll in production and
  assert the review heading is immediately visible, including200% text. Capture that real
  initial screen rather than aligning away part of a control.
- The custom entry capture leaves the keyboard over the form. Close the keyboard through
  the real Android test interaction before capture; keep the production form unchanged.

## Refined source reviewed

Source1dc34c2665ced02a1a8f5e5f33f3b81abe30ec8f passed workflow41063159777:
**388cases/API28,35,36**, build/lint/all supporting gates and98captures. Canonical
bot2c7a59976634fda88b96e49425da6fc23f2eb9a6 preserves source fingerprint
e6a0a1b0cb613ece151a6006cae97206f42391bf092b42b8daaf19c3afc047a6.

All seven refined images were opened. The normal preview now shows30October then2November
before6November; the explanations remain available through disclosures. All200% contextual
navigation labels are visible at entry, with natural scrolling. Dark text/cards remain
legible. The custom entry image now shows its selectors and notes without a covering
keyboard. These findings accept the scoped manual-builder/custom-entry refinements;
they do not certify unfinished template/generated/future-edit workflows or physical use.

## Remaining acceptance

Editable templates/generated previews, future regeneration, permanent substitution controls
and progression proposals are not finished or pictured here. Final0.10 acceptance requires
those workflows and their actual captures. Galaxy S24/TalkBack/physical use remain unperformed.

## Latest inventory follow-up — b38e075

Workflow41068497047 passed390cases per supported API and98capture integrity checks.
Canonical a855c08 preserves sourceb38e075. The latest browser, normal programme preview
and200% preview were reopened; the scoped refinements remain readable and chronological.
Two additional real browser cases verify authored inventory ownership/refresh and custom
metadata filters. These are actual compiled controls, not a rendered mockup. No S24/TalkBack
physical claim or acceptance of unfinished template/future-edit screens is made.

## Templates and concrete previews — observed source98366d7 (4 November 2026)

Workflow41889775036 passed405tests on each supported API and published110canonical
captures asb98885cced69650bcc6ff919a5b2d160fe183b68. All12new template/configuration/
dated-preview/workout images were opened individually in light, dark and actual200%text.
The semantic sage/cream cards and navy/blue hierarchy are consistent with retained screens.
No overlapping controls, clipped labels or undersized text were observed in these captures;
200%content wraps and scrolls naturally. The programme preview's long introductory copy
pushed blocks below the first viewport at200%; the next source shortens introductory and
saved notices and capitalises displayed block headings. Those refinements and new future
controls require fresh captures before visual acceptance. No image was edited.

This is an emulator capture review, not physical Galaxy S24 or human TalkBack testing.
The full0.10 release remains unfinished (permanent swaps, progression and final quality
review remain). The six future-preview/settings captures are specified but not yet reviewed.

## Future-review captures — observed source8f1d1aa

All6future preview/settings images from canonical8cba4a1 were opened individually in
light/dark/200% text after workflow41899315529 passed413cases per API. Wrapping/scrolling
and sage/cream hierarchy remain readable, with no control overlap observed. Original-start
text was dim when disabled in dark mode; the next source keeps it read-only and normally
legible, retaining the model guard and a no-edit semantics assertion. Replacement review
has3new required scenes that must be captured and opened before acceptance. No physical
S24/TalkBack pass is claimed.

## Replacement, progression and history — source4fb19b8 reviewed

Workflow41908273369 passed426cases per supported API and125captures. Canonicalf79c3c5
preserves source fingerprint cbf3a7ecd0837bf72e481ed8fc7d00b9d89e074a9606e40b858a8adc57cc4538.
All9new images were opened individually, plus future-settings dark/200%. Replacement
choices and read-only history have legible light/dark contrast, natural wrapping and
scrolling, without overlapping controls. Original-start text is now legible in dark mode.

The progression AlertDialog at200% failed visual review despite green screenshot tests:
long explanation pushed the numeric optional target below the first viewport and wrapped
footer actions were too crowded. The next source uses a full-screen scrollable review,
proposal first, optional evidence disclosure and flowing controls. Stronger captures must
show the actual82kg proposal at entry in all modes and exercise real scrolling to the save
control. That refinement requires fresh build captures and inspection. No bitmap was edited.
All physical Galaxy S24/human TalkBack checks remain unperformed.

## Refined progression — source1a7c683 accepted in scoped visual review

Workflow41912323900 passed426cases EACH API28/35/36 and125captures. Canonicalacf7a228
was adopted byteexact with current app hashc93d9cf6a62147599a59ca1e94c23dcd57ead85e8e1e762f30863f4610275514.
All3refined progression images were opened individually. The optional82kg reference now
appears immediately in the planned semantic card, ahead of current target and optional
evidence disclosures. Light/dark contrast is readable. At actual200%text, labels wrap
naturally, Keep current target remains accessible, and the save control flows in the
scrolling content instead of sharing a cramped dialog footer. The capture explicitly
scrolls to/asserts save then returns to the real entry position. No image was edited.

This accepts the scoped refinement that previously failed visual review. Prior unchanged
replacement/history scenes and future settings were reviewed above. Full0.10software/gate
work is complete; subsequent metadata-only CI still must be checked before PR readiness.
Physical Galaxy S24/human TalkBack verification remains unperformed.
