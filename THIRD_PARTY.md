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

The catalogue-wide exercise and routine WebPs are original generated
illustrations created for this project from original movement descriptions. They
are bundled offline and contributed under the project MIT licence. No third-party
exercise photographs/assets or organiser graphics were copied. Owner references
are development acceptance documents, separate from production assets. Coverage
and review are tracked in docs/exercise-artwork/README.md; all174 canonical exercises
and14 routine movements have illustrated guidance. Actual-build design acceptance
is a separate release gate and is not implied by the completeness test.

The Berlin2014 geographic dataset in docs/dream-route-data derives
only coordinates from alexdjulin/running-events-map's participant GPX, under MIT
(copyright2023 Alexandre Donciu-Julin). The original licence/source blob and
transformations are retained there. New Berlin assignments use its separately versioned
`berlin-2014-geo-v1` projection; historical assignments keep their old geometry.
The full source MIT notice is bundled in assets/licences/berlin-2014-MIT.txt
and readable in Settings. No provider metadata or health samples are included.

## Tokyo2026 position dataset (0.8.1 checkpoint)

Approximate participant coordinates adapted from kekomat/marathon-gpx-dataset,
explicitly dedicated under CC0-1.0. Source https://github.com/kekomat/marathon-gpx-dataset
at commit5bff087d82353ec64df84873bbc1a246f8a25aae. Only simplified original positions
are bundled; no biometric data, timestamps or identity. Not certified/official course
geometry. See docs/dream-route-data/tokyo-2026.json and TOKYO_SOURCE_LICENCE.txt.
Full CC0 text is bundled in assets/licences/tokyo-2026-CC0.txt and readable in Settings.
No source implementation, Garmin SDK, remote tiles or runtime download was added.

## London historical coordinate line (0.8.1 checkpoint)

The London historical route line adapts164positions from Georgios Karamanis'
MIT-licensed `gkaramanis/tidytuesday` GeoJSON, frozen at
f56d74b9e21891c67a9f616b2ccfc7bbcba74a74. No R source, map image or organiser artwork
is copied. Source publication2023 is not a claimed event edition; survey accuracy
is unspecified. Full original notice ships in
assets/licences/london-historical-MIT.txt and is readable in Settings. Complete
source/blob/transform/limitations are in docs/dream-route-data/london-historical.json.

## New York2026 geographic course data (ODbL-1.0)

New assignments use `new-york-2026-geo-v1`,371 retained XY positions from
MARATHON TRIP's explicitly offered ODbL course-line data. © OpenStreetMap
contributors; course-line preparation by MARATHON TRIP. The documented source
EPSG:26918 frame/anchor is preserved; uniform north-up scaling adds no invented
coordinates. The approximate42.67km retained centreline is separate from the
42.195km equivalent-progress axis. Not official/certified geometry or navigation.

The source, SHA256, transformation and complete retained coordinate database are
in `docs/dream-route-data/new-york-2026.json`. That data and its generated coordinate
array remain ODbL-1.0; original application code remains MIT. Source attribution and
full ODbL text are bundled and readable in Settings. No third-party application
code, tiles, buildings, films, photographs, elevation or marketing art are copied.
Old geometry IDs/preferences/assignments remain unchanged. The source's2026 context
is documented without claiming survey accuracy or verification of every start variant.

## Annual Marathon de Paris2026 geographic data (ODbL-1.0)

New Paris assignments use the annual **Schneider Electric Marathon de Paris2026**,
not the Olympic event. RDD reconstructs the statutory street itinerary from actual
OpenStreetMap nodes, via Geofabrik's22October2026 Île-de-France extract. Police
order2026-00388 articles16,18–20,22 establish the annual route; the ASO2026 guide
corroborates start/finish junctions. No organiser artwork/stroke is copied.

All1960selected positions and550source way IDs/versions, node order, factual sources,
SHA256s and transformations are offered in `docs/dream-route-data/paris-2026.json`
and `paris-2026-osm-evidence.json`. This coordinate database and its derived drawing
remain ODbL-1.0, separately from MIT app code. Full licence/notice is bundled.
Approximate road/pedestrian and junction-lane choices are not a certified racing
line:43.22km source geometry is separate from42.195km equivalent progress.
No source software, proprietary tiles, organiser logos/branding or elevation copied.

The prior `paris-2024-olympic-geo-v1` remains immutable ONLY for existing saved
assignments, with its Thomas Krakow/Running-tomy Commons ODbL notice retained.
It is no longer eligible for new Paris assignments and is never relabelled annual.

## Original city context (0.8.1)

Landmark anchors use twelve verified Wikidata P625 positions (CC0-1.0), plus three
New York anchors from the explicitly ODbL-offered MARATHON TRIP course data. Thames
and Seine centrelines retain original Natural Earth10m source positions (public
domain). These are symbolic-width centrelines, not surveyed river banks/street maps.
Exact sources, statement IDs, frozen Natural Earth commit/hash, licences, retained
coordinates and limitations: docs/dream-route-data/city-context.json. Reproduce all
projections offline with scripts/generate-dream-context.py --check. The NYC anchor
database retains ODbL; app wrapper and original Canvas illustrations remain MIT.

Miniature landmark illustrations are original RDD Canvas work. No building images,
organiser maps, remote tiles or source application code copied. Source links:
https://www.wikidata.org/wiki/Wikidata:Licensing
https://www.naturalearthdata.com/about/terms-of-use/

## Offline geographic context data (0.8.1)

All five geographic Dream Routes now use original RDD styling of a bounded ODbL
extract from OpenFreeMap's frozen `planet/20261018_080001_pt` OpenMapTiles-schema
OpenStreetMap data. Visible credit: **OpenFreeMap © OpenMapTiles · © OpenStreetMap
contributors · ODbL**. The database remains ODbL; original reader/projection/renderer
code remains MIT. No third-party code, images, map styles, icons, fonts, SDK or
runtime network client is incorporated. No runtime dependency/permission added.

The provider explicitly permits bounded offline downloads at
https://openfreemap.org/tos/ . Development used67tiles/10,342,383bytes/3parallel
requests with an identifying User-Agent, below its3000tile/50MB/4request limits.
The OSMF vector service's different no-offline policy was reviewed and respected;
its research tile is excluded from all retained data. No proprietary tiles used.

Machine-readable retained coordinates, exact frozen URLs/SHA256s, bounds and
simplification notes: `docs/dream-route-data/basemap-*.json`. These derived
geographic databases and the projected `assets/dream-basemaps/*.rddmap` are offered
under ODbL. Full licence and attribution notice are bundled/readable in Settings.
The app ships compact data only, not source tile archives. Original lightweight
MVT wire/geometry decoder is a development tool, not copied software or a new app
dependency. Context is decorative/approximate, never used as run distance evidence.
