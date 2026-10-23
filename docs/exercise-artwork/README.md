# Original instructional artwork — 0.8.1 work in progress

The new assets are original images created for Race Day Dreams in this development
session with OpenAI image generation from original written movement specifications.
No existing app artwork, photographs, web imagery, event graphics or reference JPEG
pixels were incorporated. The user-provided mockups guide layout/style, not assets.
The project distributes its contributed assets under MIT; no third-party artwork
licence is asserted or required. Generated artwork is not individual form assessment.

`drawable-nodpi/guide_*.webp` stores one 1200×800 combined movement sequence per guide,
encoded at quality84 from the original 1536×1024 output. Only the visible asset is
loaded; no catalogue-wide bitmap decoding. Four assets total approximately 152 KiB.
Text instructions/accessibility descriptions are authoritative and supplemental.

| Asset | Identity | Frames |
| --- | --- | --- |
| guide_romanian_deadlift | Romanian Deadlift | Standing / hip hinge |
| guide_bench_press | Barbell Bench Press | Controlled lowering / press |
| guide_walking_lunge | Walking Lunge (also bodyweight routine) | Standing / lunge |
| guide_ankle_rocks | Ankle rocks routine | Half-kneeling ankle movement |

These four were visually inspected for complete bodies, equipment, neutral posture,
recognisable human proportions and reference palette. Qualified movement review and
actual-build light/dark/large-text comparison remain required. **Coverage is not yet
174/174.** The old 25-guide system remains for other movements until individually
reviewed replacements exist. Do not count generic fallback drawings as accepted art.
No release may claim complete coverage from this checkpoint.

## Routine coverage checkpoint

All14 built-in routine movements now have reviewed original illustrations:
walk/jog (shared walking option), ankle rocks, ankle circles, calf raises, leg
swings, walking lunge, marching, controlled strides, glute bridge, hip-flexor,
seated glute, calf and supported quad stretches. Unsupported custom guidance
never inherits an unrelated pose. The old routine stick-figure renderer is removed.
The calf-stretch draft with a raised rear heel was rejected and replaced with
an explicitly corrected heel-down image. A duplicate marching pose was also
replaced with distinct standing/raised-knee phases before bundling.

The bodyweight Glute Bridge now has exact setup/movement text and shares its
appropriate mat illustration with routines. Canonical accepted artwork coverage
is **4/174**, with26 text guides in the older technique model. Strict coverage
still blocks release. `scripts/ExerciseGuidanceAudit.java` derives missing IDs
from the real catalogue, checks required copy/accessibility and actual local
assets; `--report` is only for this development branch. Before final delivery,
require strict mode in the normal build as well as release publication.

Packaged-resource instrumentation checks every built-in routine and every
registered canonical asset without decoding the entire catalogue into bitmaps.
It does not certify human form or imply unregistered exercises are complete.
