# Original instructional artwork — 0.8.1

## Current coverage

All **174/174 canonical exercises** have movement-specific illustrated guidance.
All **14/14 supporting-routine movements** have artwork. The strict catalogue audit
passes; no canonical exercise uses a schematic or a generic unrelated-pose fallback.

There are **177 original local WebP assets**, totalling **9,198,660 bytes** before APK
packaging. Each is a 1200×800 sequence encoded at quality 84. Only the displayed
asset is decoded. Single-frame holds, two-frame movements and three-frame technical
lifts retain meaningful captions and complete written/screen-reader instructions.
The exact byte sizes, SHA256 checksums and creation/licence records are in
`asset-provenance.json`. Adding an exercise without a guide fails domain tests and
the strict packaged-file audit. `missing-canonical-ids.txt` is currently empty.

## Authorship and review

These are original project images generated from original written movement
specifications with OpenAI image generation. No photographs, scraped exercise art,
organiser graphics or owner-reference JPEG pixels were incorporated. The supplied
mockups determine visual direction; they remain separate in `docs/design-reference`.
The project distributes the contributed artwork under MIT.

Every added sequence was inspected for movement, equipment and framing before
bundling. Incorrect drafts were corrected: low-bar shelf, belt-squat clearance,
weighted push-up vest, landmine orientation, block-pull clearance, standing tibialis
raise and cable/machine framing. Technical lifts distinguish hang/floor starts,
shallow/deep receiving positions and press/jerk finishes. Related named variants
share art only when the illustrated equipment and movement are appropriate; variant
instructions remain explicit. No load/repetition values are fabricated for holds or
carries. Text and illustration are general guidance, not individual form assessment.
Qualified movement review on a real device remains valuable and is not claimed.

## Maintainable implementation

- `TechniqueGuide` resolves canonical movement copy through small catalogue content
  classes; unknown/custom exercises remain unknown.
- `TechniqueIllustrations` and `CatalogueTechniqueIllustrations` explicitly map
  accepted IDs to assets, meaningful accessibility descriptions and frame captions.
- `GuidanceArtwork.kt` loads only the requested local resource.
- `TechniqueScreens.kt` provides one shared guide template. Cues/mistakes sit beside
  each other at normal text size and stack at larger font scales.
- No new database, backup format, runtime permission or dependency is needed.

## Validation commands

```sh
sh scripts/test-domain.sh
java -cp domain/build/offline-tests scripts/ExerciseGuidanceAudit.java
python3 scripts/verify-guidance-assets.py
```

The audit proves coverage and asset integrity. It does **not** prove final design
fidelity. New actual-build captures, light/dark/200% review against the untouched
owner references and current Android matrix verification remain required before
release. The previous 47-guide captures do not certify the expanded catalogue.
