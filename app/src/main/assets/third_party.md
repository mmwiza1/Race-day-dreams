# Third-party software and engineering references

Original Race Day Dreams code is MIT. Dependency licences remain their own
licences; this inventory does not relicense them. No GPL source or commercial
training schedules are incorporated.

## Incorporated runtime/build/test dependencies

Versions below are pinned direct dependencies. The resolved runtime graph is
printed by CI; AndroidX transitive modules are maintained by the same upstream
project under Apache-2.0. Review that graph and this inventory when versions change.

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
| Guava/listenablefuture and AndroidX concurrent adapters, when resolved | Resolved by pinned graph | Apache-2.0 | Async interoperability; [Guava](https://github.com/google/guava). |
| Gradle wrapper/distribution | 8.11.1 | Apache-2.0 plus included-component notices | Build only; [upstream notices](third_party/gradle-LICENSE.txt). |
| Android Gradle Plugin | 8.9.2 | Apache-2.0 | Build only; Android Open Source Project. |
| Android SDK/build tools | Platform 35 / tools 35.0.0 | Package-specific SDK terms | Developer toolchain; not application code. |
| AndroidX Test runner/core/ext JUnit | 1.6.2 / 1.6.1 / 1.2.1 | Apache-2.0 | Instrumentation tests only; [source](https://github.com/android/android-test). |
| Compose UI test JUnit4/test manifest | BOM-managed | Apache-2.0 | Instrumentation/debug support only. |
| JUnit 4 and Hamcrest (test transitive) | Resolved by AndroidX Test | EPL-1.0 / BSD-3-Clause | Test APK only; [JUnit](https://github.com/junit-team/junit4), [Hamcrest](https://github.com/hamcrest/JavaHamcrest). Gradle notices retain corresponding test-component notices. |

Apache-2.0 terms and upstream Kotlin/coroutines notices, plus the protobuf BSD
notice, are bundled under app assets for offline access. AndroidX works are
Copyright The Android Open Source Project / The Android Open Source Project
authors and are licensed under Apache License 2.0. No AndroidX, Kotlin or
Health Connect source files were copied into this project's MIT source.
Dependencies are linked through their published artifacts.

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
