# Licences and attribution

## Original project code

Original Race Day Dreams code and documentation are distributed under the
[MIT licence](LICENSE). Contributors must have the right to contribute their
work under that licence. Do not commit secrets, proprietary assets or copied
commercial training schedules.

## Build tools and platform

| Component | Role | Licence/source |
| --- | --- | --- |
| Gradle 8.11.1 and wrapper | Build orchestration | Apache-2.0; [Gradle licence](https://github.com/gradle/gradle/blob/v8.11.1/LICENSE) |
| Android Gradle Plugin 8.9.2 | Android build plugin | Apache-2.0, Android Open Source Project |
| Android platform APIs | Runtime platform, not a bundled third-party library | Android Open Source Project/platform distribution terms |
| Android SDK/build tools | Developer toolchain | Their individual SDK package licence terms |

Phase 1 adds no third-party runtime library, analytics SDK, fonts, images or
training-plan assets. Java domain tests use no third-party test framework. The
Gradle wrapper's upstream licence and included-component notices are retained in
[third_party/gradle-LICENSE.txt](third_party/gradle-LICENSE.txt), copied from the
Gradle v8.11.1 source distribution. These cover build tooling, not runtime
dependencies of the application. This inventory must be updated when dependencies
change; build tool licensing does not replace review of a new library's transitive
dependencies.

## NHS Couch to 5K — future work

The published NHS Couch to 5K programme is the intended basis for future beginner
5K progression. The schedule has **not** been implemented or copied in Phase 1.

Reference: [NHS — Get running with Couch to 5K](https://www.nhs.uk/live-well/exercise/get-running-with-couch-to-5k/).

Before adding programme content, verify the current published schedule and the
reuse terms applying to that material. Record the source/version, retain
required attribution and licence notices, and show them in the app's About /
Licences area. Public availability alone does not establish permission to copy.

Do not copy the NHS app's source code, UI, audio, branding or proprietary assets.
Do not imply NHS affiliation or endorsement. Do not copy commercial plans from
Runna, Garmin, Hal Higdon, B.A.A., Nike or other providers.

Later 10K, half-marathon, marathon and aerobic plans must be original implementations
of documented general endurance-training principles, with tests and explanations.
