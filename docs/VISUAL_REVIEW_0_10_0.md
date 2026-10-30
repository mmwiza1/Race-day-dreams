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

The follow-up implements these fixes and a chronological-preview regression. Its regenerated
images have NOT yet been reviewed; the existing0eb56d6 images do not prove those fixes.

## Remaining acceptance

Editable templates/generated previews, future regeneration, permanent substitution controls
and progression proposals are not finished or pictured here. Final0.10 acceptance requires
those workflows and their actual captures. Galaxy S24/TalkBack/physical use remain unperformed.
