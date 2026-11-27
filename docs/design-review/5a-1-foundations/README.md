# Phase 5A.1 · Shared visual foundations

The redesign uses a neutral activity heading, a small discipline marker and a more
pronounced title. A shared action component distinguishes the main task, supporting
actions and navigation. Tabular numerals are available for aligned activity metrics.
The application still supplies its Material You and user-selected discipline palettes.

| View | Before · published 0.17.0-test1 | Target · approved concepts | After · Android API 28 |
| --- | --- | --- | --- |
| Exercise heading, light | [Previous historical set flow](../../screenshots/strength-log-previous.png) | [Strength reference A](../../design-reference/02_strength_dark_lime.png) | [Heading and supporting action](after/exercise-header-light.png) |
| Exercise heading, dark | [Published dark Gym Now set flow](../../screenshots/gym-workout-dark.png) | [Strength reference B](../../design-reference/06_strength_dark_violet.png) | [Heading and supporting action](after/exercise-header-dark.png) |

Both After images are unmodified captures of the historical logger on a compact API 28
device using synthetic sets. The published dark gallery provides a related Gym Now view.
Its exercise, viewport and state differ. The foundation checkpoint demonstrates hierarchy
and accent placement. Structured measurement editing belongs to 5A.2.

No calculation, navigation, colour preference or storage code changed in this checkpoint.
The owner review gate follows 5A.2.

Implementation source: `76a2af5045ba965a644f1ac7d9bfedd8fe412b90`. Android debug build and two focused API 28
logging runs passed, one in each theme. The earlier form still needs its structural
remediation in 5A.2.
