# 0.11 visual review — in progress

## Verified gym checkpoint

Source `a26150dbcdbe7f1abef65b8a7680c1bbc4b0a1c3`, workflow42073702201, passed448tests on each
API28/35/36 and164required actual-build captures per API. Canonical bot commit
`8a86d56186d9b571494a158fcd967eef47df499c` preserves the raw API35 images and source metadata.
Artifact13439525824 ZIP SHA256:
`f4f7ca650996ec6034db35099eb6e3e118a85f442c4274ac3c19eb0845a4a8a5`.
The archive was downloaded, hashed and extracted without editing the images.

All nine new timer/warm-up/superset images were opened directly, across light, dark and200%font.
Text wraps and the controls remain readable; dark surfaces preserve foreground contrast. Large
text scrolls naturally rather than forcing a narrow grid. The semantic pastel cards and navy/blue
hierarchy belong to the existing0.10.1 family. These are real production screens, not design mocks.

Review found issues to refine before final acceptance:

- Paused rest showed both Resume and a prominent new Start. The next source hides new Start
  while an existing timer is running/paused/interrupted and gives wrapped controls vertical gaps.
- Warm-up safeguards dominated the200%view. Instructions remain primary, with full justification
  behind a second functional “Why this preparation?” disclosure.
- Superset context did not display the full movement order and offered navigation to the already
  selected first movement. The next source lists the order and offers navigation only when needed.
- A fully skipped group must not be described as completed working sets; its actuals/targets remain
  distinct. Domain assertions cover this and rest after a skipped final movement.

These refinements are NOT visually signed off yet. They need regenerated final-source captures.

## Strength intelligence and remaining release

Analytics, recorded-versus-estimated records, milestone recognition/evidence and deep gym settings
have actual production capture definitions in all three modes. The next required set is188PNGs:
155baseline +15gym preparation/settings/evidence +18strength intelligence. Existing captures are
retained; original references remain separate under docs/design-reference and are not replaced.

M2 current-source Android execution and screenshot inspection remain pending. Recovery/body/
retention/catalogue milestones and their UI have not reached visual acceptance. A passing image
integrity check alone cannot establish design quality. Inspect every new screen and meaningful
scroll/disclosure state against the approved plain-English0.10.1 direction before completion.
No Galaxy S24 physical or human TalkBack acceptance is claimed.
