# Third-party software and engineering references

Original Race Day Dreams code is MIT. Dependency licences remain their own
licences; this inventory does not relicense them. No GPL source or commercial
training schedules are incorporated.

## Incorporated runtime/build/test dependencies

Direct dependencies are pinned below. Transitive versions are the actual versions
resolved by CI for this build, whose runtime graph is printed in the job logs.
AndroidX transitive modules are maintained by the same upstream project under
Apache-2.0. Review that graph and this inventory when versions change.

| Project / artifact | Version | Licence | Purpose / source / notices |
| --- | --- | --- | --- |
| Kotlin / Android and Compose compiler plugins, standard library | 2.0.21 | Apache-2.0 | Android integration language; [source](https://github.com/JetBrains/kotlin/tree/v2.0.21). Licence retained in assets. |
| kotlinx.coroutines Android/core | 1.9.0 | Apache-2.0 | Off-main IO, structured concurrency and StateFlow; [source](https://github.com/Kotlin/kotlinx.coroutines/tree/1.9.0). Licence retained in assets. |
| AndroidX Compose BOM | 2024.12.01 | Apache-2.0 | Pins Compose UI/runtime/foundation/material libraries; [AndroidX](https://android.googlesource.com/platform/frameworks/support/). |
| Compose UI, Foundation, Material 3, previews/tooling | BOM-managed | Apache-2.0 | Accessible declarative UI; AndroidX copyright/licence notices below. Tooling is debug-only. |
| AndroidX Activity Compose | 1.9.3 | Apache-2.0 | Activity and result/permission integration; AndroidX. |
| AndroidX Navigation Compose | 2.8.5 | Apache-2.0 | Local navigation; AndroidX. |
| AndroidX Lifecycle runtime/viewmodel Compose | 2.8.7 | Apache-2.0 | Lifecycle-aware presentation; AndroidX. |
| AndroidX Health Connect client | 1.1.0 | Apache-2.0 | Optional local health-provider reads; [source](https://android.googlesource.com/platform/frameworks/support/+/androidx-main/health/connect/). |
| AndroidX transitive Core, Collection, SavedState, Annotation, ProfileInstaller and related modules | Resolved by pinned graph | Apache-2.0 | Support libraries from AndroidX; no analytics or network service. |
| Protocol Buffers code used by Health Connect client | Resolved/relocated upstream component | BSD-3-Clause | Serialization in AndroidX Health Connect. [Source](https://github.com/protocolbuffers/protobuf); BSD licence retained in assets. |
| Guava (`com.google.guava:guava`) | 31.1-android | Apache-2.0 | Health Connect / AndroidX async interoperability; [source and licence](https://github.com/google/guava/tree/v31.1). Attribution in `transitive-NOTICES.txt`; Apache terms bundled. |
| Guava failureaccess (`com.google.guava:failureaccess`) | 1.0.1 | Apache-2.0 | Internal future interoperability; [component](https://github.com/google/guava/blob/v31.1/futures/failureaccess/pom.xml). Guava notice and Apache terms bundled. |
| Guava listenablefuture (`com.google.guava:listenablefuture`) | 9999.0-empty-to-avoid-conflict-with-guava | Apache-2.0 | [Empty conflict-avoidance artifact](https://github.com/google/guava/blob/v31.1/futures/listenablefuture9999/pom.xml); implementation comes from Guava. Apache terms bundled. |
| JSR305 annotations (`com.google.code.findbugs:jsr305`) | 3.0.2 | BSD-3-Clause | Nullability annotations; [FindBugs licence inventory](https://github.com/findbugsproject/findbugs/blob/master/findbugs/licenses/LICENSE-jsr305.txt), [full expert-group notice retained by Google](https://github.com/google/j2objc/blob/1.3/jsr305/LICENSE). Full notice bundled in `transitive-NOTICES.txt`. |
| Checker Framework qualifiers (`org.checkerframework:checker-qual`) | 3.12.0 | MIT | Type-qualifier annotations; [exact component licence](https://github.com/typetools/checker-framework/blob/checker-framework-3.12.0/checker-qual/LICENSE.txt). This MIT-only annotations component does not include the GPL-licensed compiler. Full notice bundled in `transitive-NOTICES.txt`. |
| Error Prone annotations (`com.google.errorprone:error_prone_annotations`) | 2.11.0 | Apache-2.0 | Static-analysis annotations; [component declaration](https://github.com/google/error-prone/blob/v2.11.0/annotations/pom.xml). Author notice and Apache terms bundled. |
| J2ObjC annotations (`com.google.j2objc:j2objc-annotations`) | 1.3 | Apache-2.0 | Guava's cross-platform annotations; [source](https://github.com/google/j2objc/tree/master/annotations), [licence](https://github.com/google/j2objc/blob/master/LICENSE). Author notice and Apache terms bundled; no Objective-C runtime incorporated. |
| JetBrains annotations (`org.jetbrains:annotations`) | 23.0.0 | Apache-2.0 | Kotlin/JVM nullability and contract annotations; [exact licence](https://github.com/JetBrains/java-annotations/blob/23.0.0/LICENSE.txt). JetBrains notice and Apache terms bundled. |
| Gradle wrapper/distribution | 8.11.1 | Apache-2.0 plus included-component notices | Build only; [upstream notices](third_party/gradle-LICENSE.txt). |
| Android Gradle Plugin | 8.9.2 | Apache-2.0 | Build only; Android Open Source Project. |
| Android SDK/build tools | Platform 36 / tools 35.0.0 | Package-specific SDK terms | Developer toolchain; not application code. |
| AndroidX Test runner/core/ext JUnit | 1.6.2 / 1.6.1 / 1.2.1 | Apache-2.0 | Instrumentation tests only; [source](https://github.com/android/android-test). |
| AndroidX Espresso core | 3.6.1 | Apache-2.0 | Native View interoperability tests during incremental Compose migration; AndroidX Test. |
| Compose UI test JUnit4/test manifest | BOM-managed | Apache-2.0 | Instrumentation/debug support only. |
| JUnit 4 and Hamcrest (test transitive) | Resolved by AndroidX Test | EPL-1.0 / BSD-3-Clause | Test APK only; [JUnit](https://github.com/junit-team/junit4), [Hamcrest](https://github.com/hamcrest/JavaHamcrest). Gradle notices retain corresponding test-component notices. |

Apache-2.0 terms and upstream Kotlin/coroutines notices, the protobuf BSD notice,
and `transitive-NOTICES.txt` (including JSR305 BSD and checker-qual MIT terms) are
bundled under app assets and accessible from Settings offline. AndroidX works are
Copyright The Android Open Source Project / The Android Open Source Project
authors and are licensed under Apache License 2.0. No AndroidX, Kotlin or
Health Connect source files were copied into this project's MIT source.
Dependencies are linked through their published artifacts.

## CI-only actions

These actions execute in GitHub Actions and are not bundled in the application.
Their licence files were checked at the configured major-version refs.

| Action / source | Ref | Licence | Purpose |
| --- | --- | --- | --- |
| [actions/checkout](https://github.com/actions/checkout) | v5 | MIT | Repository checkout |
| [actions/setup-java](https://github.com/actions/setup-java) | v5 | MIT | CI JDK |
| [android-actions/setup-android](https://github.com/android-actions/setup-android) | v3 | MIT | Android SDK setup |
| [gradle/actions](https://github.com/gradle/actions) | v4 | MIT | Gradle setup/cache |
| [reactivecircus/android-emulator-runner](https://github.com/ReactiveCircus/android-emulator-runner) | v2 | Apache-2.0 | Emulator device tests |
| [actions/upload-artifact](https://github.com/actions/upload-artifact) | v4 | MIT | APK/test-report artifacts |

## Engineering references, no incorporated source

| Reference | Review / decision |
| --- | --- |
| [Google Now in Android](https://github.com/android/nowinandroid) | Architecture Learning Journey consulted for unidirectional data flow; its complexity and source are not copied. Apache-2.0. |
| [Google Android Health Samples](https://github.com/android/health-samples) | Health Connect manager and manifest API patterns consulted. Original adapter implementation; no copied sample code/assets. Apache-2.0. |
| [OpenTracks](https://github.com/OpenTracksApp/OpenTracks) | Approved engineering direction for future offline recording; no recorder source incorporated. Apache-2.0. |
| [android-gpx-parser](https://github.com/ticofab/android-gpx-parser) | Apache-2.0; README recommends 2.3.1/JitPack, declares unmaintained status and a Joda-Time Android dependency. Runtime adoption deferred pending compatibility/maintenance review. |
| [JPX](https://github.com/jenetics/jpx) | Apache-2.0; current branch requires a newer Java toolchain than this app. An older Java-17 release still needs Android XML/API verification. No dependency included. |
| [Vico](https://github.com/patrykandpatrick/vico) | Apache-2.0 candidate evaluated against current small chart scope. Accessible native bars/text meet this increment; no Vico dependency included. |
| [MapLibre](https://github.com/maplibre/maplibre-native) | Future optional open map stack; not included. Verify exact component/version licence before adoption. |
| TrainingPlanKit, TLR, Zoned, Tempo | Approved conceptual references for future testing/analytics/portability/shoe UX. No source or assets incorporated; exact selected versions/notices require review before reuse. |
| RunnerUp, FitoTrack, C2K | GPL reference/ideas only. No source incorporated. |

The NHS published Couch to 5K schedule has not been copied or implemented here.
Its current schedule and applicable reuse terms must be verified before that
programme is added. No NHS app source, audio, UI, branding or proprietary assets
are used, and no NHS endorsement is implied.

## 0.3.0-test1 additions

No new runtime, build or test dependency was incorporated. Scheduling, strength
analytics and scoring are original MIT application code. Android's existing
`UiAutomation.takeScreenshot()` and the existing AndroidX Compose instrumentation
tools capture actual screens; no image-generation or screenshot-library runtime
is shipped. Capture fixtures are in the test APK only.

Choco (BSD-3-Clause) and Timefold (Apache-2.0) hard/soft constraint concepts were
considered; neither library, source nor prescription data is incorporated. For a
seven-day search space, original bounded enumeration avoids Android compatibility,
APK size, Java-runtime and maintenance costs. No TrainingPlanKit, TLR, Open-Coach,
Long Run Hybrid Coach or other reference-project source/schedule data was copied.
Scientific/formula references are documented in TRAINING_ENGINE.md; arithmetic
and product heuristics are not presented as independently validated coaching.


## 0.4 increment

No runtime, build or screenshot dependencies added. Charts, scheduling, review,
durability, reconciliation and mass conversions are original project code.
No proprietary provider SDK is incorporated. Direct-provider scaffolding is removed
in 0.5; Health Connect remains supported. No new runtime dependencies were added.

## Original 0.5 assets and engineering

The exercise catalogue, technique copy and schematic Compose drawings are original
Race Day Dreams work licensed with this MIT repository. No exercise photography,
commercial schedules or GPL/AGPL source/assets were incorporated. Scientific papers
are references, not copied programme tables: see docs/STRENGTH_PROGRAMMING.md.

## Native run increment — engineering references

OpenTracksApp/OpenTracks (Apache-2.0), services/TrackRecordingService.java and recording
manager separation, reviewed 23 October 2026: foreground ownership, sensor lifecycle,
notification and bounded recording context informed the original RDD implementation.
No OpenTracks source/assets were copied or materially adapted. Existing platform Android
LocationManager, Bluetooth SIG Heart Rate profile and Android XmlPullParser are used;
no parser/sensor runtime dependency added. Official AndroidX Health Connect API source
(Metadata/Device, Apache-2.0) was consulted for record identifiers/write metadata.
No GPL/AGPL source, Garmin FIT SDK, proprietary provider SDK or map library was added.

## Prompt 2 original content

No runtime dependency, external route dataset, official marathon graphic, GPL code or
photograph is added. The 14 supportive movement descriptions/line figures, 13 routines,
execution/cue/prediction rules and five city-inspired Dream Route schematics are original
Race Day Dreams work under this repository's MIT licence. The drawings are not actual
or official surveyed marathon courses. No source geometry was copied. Platform Android
TextToSpeech/AudioFocusRequest/VibrationEffect are used directly; no OpenTracks audio
source was adapted. Existing Apache-compatible engineering references remain references.

## 0.8 original assets and research

No runtime/build dependency added. Semantic surfaces, technique figures, offline route
rendering, landmarks and v2 Dream drawing anchors are original MIT project work.
No organiser map, GPX, screenshot, logo, typography or map tiles were incorporated.
Factual course research/reuse assessment is recorded in docs/DREAM_ROUTES.md. A public
GPX download is not treated as a redistribution licence.

## 0.8.1 artwork checkpoint

New RDL, Bench Press, Walking Lunge and Ankle Rocks WebPs are original generated
illustrations created for this project from original movement descriptions. They
are bundled offline and contributed under the project MIT licence. No third-party
exercise photographs/assets or organiser graphics were copied. Owner references
are development acceptance documents, separate from production assets. Coverage
and review are tracked in docs/exercise-artwork/README.md; full174 is not yet met.

The Berlin2014 geographic **research candidate** in docs/dream-route-data derives
only coordinates from alexdjulin/running-events-map's participant GPX, under MIT
(copyright2023 Alexandre Donciu-Julin). The original licence/source blob and
transformations are retained there. It is not yet a bundled/live Dream Route.
