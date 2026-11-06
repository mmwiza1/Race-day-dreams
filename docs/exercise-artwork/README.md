# Original instructional artwork — current catalogue

## Current coverage

All **220/220 canonical exercises** have movement-specific illustrated guidance.
All **14/14 supporting-routine movements** have artwork. The strict catalogue audit
passes; no canonical exercise uses a schematic or a generic unrelated-pose fallback.

There are **223 original local WebP assets**, totalling **11,634,030 bytes** before APK
packaging. Each is a 1200×800 sequence encoded at quality 84. Only the displayed
asset is decoded. Single-frame holds, two-frame movements and three-frame technical
lifts retain meaningful captions and complete written/screen-reader instructions.
The exact byte sizes, SHA256 checksums and creation/licence records are in
`asset-provenance.json`. Adding an exercise without a guide fails domain tests and
the strict packaged-file audit. `missing-canonical-ids.txt` is currently empty.

## 0.11 catalogue checkpoint

Forty-six complete additions cover seated hip adductor/abductor machines, cable hip adduction/
abduction, cable glute kickback, seated dumbbell calf raise, wall/knee push-ups, chair
sit-to-stand and side-lying hip abduction. Further additions cover straight-arm shoulder-blade
control, short/long-lever Copenhagen holds, bear plank, heel taps, single-leg glute bridge,
Pallof hold and overhead dumbbell carry. Eight further additions cover inner-thigh floor work,
scapular pulling, prone Y raise, cable external rotation and four Smith-machine setups. Each has its own original movement-specific artwork,
explicit measurement/equipment/role metadata, readable instructions and accessibility copy.
Copenhagen drafts were corrected to put the elbow and entire forearm on the mat;
static holds use a single useful frame. The first cable-adduction draft was rejected and corrected: the cuff must be on the near
leg so inward movement works against the cable, not with its pull. No rejected draft ships.
Wall/knee pushing alternatives explain support/leverage rather than invented added load.

Further additions cover floor presses, unilateral machines, hip control, supported squatting,
reverse Nordic, reverse-hyper and 45-degree benches, crawling, shoulder taps, hip lifts,
one-arm carries and distinct inward/outward shoulder rotations. Specific equipment requirements
prevent unrelated machines or supports being assumed available. New bodyweight distance tasks
record actual metres; timed holds remain separate from repetition tasks. Each addition has
complete guidance, original art and a stable identity; the original174 definitions are retained.

Bundled additions total **2,435,370 bytes** over the prior177assets. The selective catalogue
now reaches220 complete entries. Verified sourceee760bf passes508tests/359captures EACH API28/35/36, including all46additions
in light/dark/200%. The API35CI APK is55,250,287bytes: +2,500,202/+4.7397%versus pre-catalogue
M5API35, and+6.2923%versus0.10.1 CI. The final200%tail views were directly reviewed; see
VISUAL_REVIEW_0_11.md. Asset completeness alone does not replace that visual review.

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
fidelity. The174-guide source passed strict completeness, Android build/lint and the supported
API28/35/36 matrix in workflow40027830067. Fresh London sourceb56b118 then passed
200tests perAPI and63actual captures in40030105708. RDL, Bench, Walking Lunge and
Ankle Rocks actual screens were compared with the untouched owner references: the
filled-athlete, blue-panel, mint-tip/pink-mistake family is present; guidance remains
scrollable on narrow screens. Final whole-product acceptance and physical movement/
TalkBack checks are separate and remain unclaimed.
