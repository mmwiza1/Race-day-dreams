# Licences and attribution

Original Race Day Dreams code and documentation are [MIT](LICENSE). Incorporated
library licences remain their own licences. See [THIRD_PARTY.md](THIRD_PARTY.md)
for versions, purposes, sources and notices. The inventory, Apache-2.0 terms and
upstream Kotlin/coroutines/protobuf licence notices are bundled in app assets
for offline access. Gradle build-tool notices are retained separately.

No commercial training schedules, proprietary assets, GPL source, analytics SDKs
or external service branding are incorporated. Engineering references are not
claimed as dependencies or copied implementations. Exact dependency versions and
required notices must be reviewed whenever the resolved graph changes.

## NHS Couch to 5K

The published NHS programme remains a future implementation. Before adding its
schedule structure, verify the current source and reuse/attribution terms. Public
availability alone does not establish a copying licence. Record the source/version
and required attribution in the app and this repository. Do not copy NHS app code,
UI, audio, branding or proprietary assets, and do not imply affiliation.

Reference: https://www.nhs.uk/live-well/exercise/get-running-with-couch-to-5k/

## Training methodology

Future 10K, half-marathon, marathon and aerobic plans must be original. General
training principles and permissive engineering references do not authorize
copying commercial or book-derived schedule catalogues. Test fixtures are clearly
synthetic and must not be presented as operational coaching plans.

Native recording, health-context rules, GPX adapter and archive implementation are original
Race Day Dreams code (MIT). OpenTracks (Apache-2.0) was an engineering reference, with no
source/assets copied. Android's bundled XML parser supplies XML parsing; no additional
library licence or proprietary FIT/Samsung binary is incorporated. See THIRD_PARTY.md.

Supportive movement drawings and the five Dream Route schematics introduced in Prompt2
are original MIT Race Day Dreams work. The route drawings are abstract city-inspired
marathon equivalents, not official courses or navigation. No event branding, third-party
route geometry, commercial illustrations or GPL source was incorporated. Platform
audio/TTS/vibration APIs add no bundled third-party dependency.

## 0.8 assets

Original three-frame technique and Dream landmark/drawing code remains MIT project
artwork. No external visual asset or proprietary course file was ingested. See
docs/DREAM_ROUTES.md for per-city factual references and explicit geometry limitations.
Dependencies/notices remain unchanged.

0.8.1's new bundled exercise illustrations are original generated project assets,
contributed under MIT; see docs/exercise-artwork/README.md for creation and review.
The five owner-provided design references remain separate development references,
not production assets or claimed actual screenshots. No new runtime library is bundled. Berlin2014 participant coordinates are now
projected for new Berlin assignments, with a new immutable geometry ID. The MIT
source, transformation and original Alexandre Donciu-Julin notice are recorded in
docs/dream-route-data and bundled in assets/licences/berlin-2014-MIT.txt.

## Tokyo2026 position dataset (0.8.1 checkpoint)

Approximate participant coordinates adapted from kekomat/marathon-gpx-dataset,
explicitly dedicated under CC0-1.0. Source https://github.com/kekomat/marathon-gpx-dataset
at commit5bff087d82353ec64df84873bbc1a246f8a25aae. Only simplified original positions
are bundled; no biometric data, timestamps or identity. Not certified/official course
geometry. See docs/dream-route-data/tokyo-2026.json and TOKYO_SOURCE_LICENCE.txt.
Full CC0 text is bundled in assets/licences/tokyo-2026-CC0.txt and readable in Settings.
No source implementation, Garmin SDK, remote tiles or runtime download was added.

## London historical coordinate line (0.8.1 checkpoint)

Georgios Karamanis' MIT-licensed TidyTuesday GeoJSON contributes164retained
positions. Source publication2023; race edition and survey accuracy unspecified.
No visual artwork or source implementation copied. The exact copyright/licence
notice is preserved in docs/dream-route-data/LONDON_SOURCE_LICENCE.txt and
assets/licences/london-historical-MIT.txt. See london-historical.json for frozen
source commit/blob and transformation. No current/official-course claim is made.

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

### Offline geographic context

OpenFreeMap © OpenMapTiles · © OpenStreetMap contributors · ODbL-1.0.
All five Dream Route contexts retain selected factual road/water/green-space DATA
from the frozen20261018OpenFreeMap dataset, not downloaded artwork/styles or code.
ODbL retained/projected databases are offered in docs/dream-route-data and app
assets/dream-basemaps; source URLs/checksums and transformations are recorded per
city. The provider explicitly permits the bounded development offline extract;
see THIRD_PARTY.md and bundled licences/dream-basemaps-NOTICE.txt. Original reader,
projection and styling remain MIT. Nothing is fetched by the app at runtime.

## 0.8.2 original Dream Routes artwork

Five city heroes and fifteen landmark-poster WebPs are original generated illustrations
created for Race Day Dreams from original architectural descriptions, contributed under
the project MIT licence. No tourism photograph, organiser artwork, logo, sponsor identity
or third-party illustration was copied. Decorative hero compositions do not represent
geographic landmark positions. Exact source-plate hashes, processing/crops and asset
hashes are recorded in docs/dream-artwork-0.8.2.json. Existing geographic datasets, ODbL
notices and source licences are unchanged; maps remain coordinate/vector rendered.
No runtime/build dependency or permission was added.
