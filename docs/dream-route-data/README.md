# Dream Route source data

These are bundled visualisation datasets, not user GPS, training evidence, certified
race courses or navigation. The application uses a separate 42,195 m equivalent axis.
No raw trace length, course landmark or city geometry becomes an actual run metric.

## Retained geographic datasets

| Dataset | Retained positions | Licence | Exact provenance |
| --- | ---: | --- | --- |
| London historical line | 164 | MIT | [london-historical.json](london-historical.json) |
| Berlin participant trace, 2014 | 191 | MIT | [berlin-2014-candidate.json](berlin-2014-candidate.json) |
| Tokyo participant trace, 2026 | 91 | CC0-1.0 | [tokyo-2026.json](tokyo-2026.json) |
| New York road centreline, 2026 context | 371 | ODbL-1.0 | [new-york-2026.json](new-york-2026.json) |
| Annual Marathon de Paris,2026 |1960|ODbL-1.0|[paris-2026.json](paris-2026.json)|

Every JSON includes its source URL/revision or commit, checked date, licence
assessment, source checksum/blob identity, retained coordinate sequence and limitations.
Original notices ship in the APK and are readable in Settings. ODbL databases and
derived coordinate arrays retain ODbL separately from the original MIT application.

### London

Source: Georgios Karamanis' `gkaramanis/tidytuesday`, week 17 of 2023, commit
`f56d74b9e21891c67a9f616b2ccfc7bbcba74a74`, GeoJSON blob
`f187e1367cd1bfd8c1444d84cc2ccccf41f77e4b`. All 164 positions of the named full-course
feature are retained. Alternative starts and zero-altitude values are excluded.
This is an already simplified historical geographic drawing, **not a participant
trace**. The edition and original survey accuracy are unspecified; publication in
2023 is not a claimed race edition. Its approximate 42.78 km line has a largest
source segment of about 1.41 km. See [the exact MIT notice](LONDON_SOURCE_LICENCE.txt).

### Berlin and Tokyo

Berlin adapts position-only evidence from Alexandre Donciu-Julin's MIT-licensed
`alexdjulin/running-events-map` 2014 participant GPX. Approximate measured source
length is 43.01 km. See [the original notice](BERLIN_SOURCE_LICENCE.txt).

Tokyo adapts `kekomat/marathon-gpx-dataset`, commit
`5bff087d82353ec64df84873bbc1a246f8a25aae`, source blob
`c5baee9ad663325e99042b03614ff6af514ebd57`, explicitly dedicated under CC0.
Its 10,872 positions are reduced to 91 retained original vertices at 25 m tolerance;
source indices are recorded. Approximate source length is 42.75 km. See
[the full CC0 notice](TOKYO_SOURCE_LICENCE.txt).

No identity, timestamps, biometric samples or fabricated elevation are retained in
these visual datasets. Approximate historical GPS evidence is not a current certified
course, even when the event name is recognisable.

### New York

MARATHON TRIP explicitly offers its course line as ODbL data in its machine-readable
credits, data page and terms. Only the delivered line's XY arrays and frame are
extracted, retaining all 371 vertices. The original source frame is EPSG:26918,
with metre offsets from E 579000 / N 4495000. No source application code, films,
pictures, buildings, elevation or marketing artwork is adapted.

The publisher reports 42.706 km for its unsimplified line; the delivered rounded,
1 m-simplified line measures about 42.673 km. Both remain separate from equivalent
progress and certified race distance. See [source attribution](NEW_YORK_SOURCE_NOTICE.txt)
and [the full ODbL text](ODbL-1.0.txt). The retained database and its transformation
are offered in this repository under ODbL, with contributor attribution in the app.

### Paris — annual Marathon de Paris,2026

Factual itinerary: [police order2026-00388,3April2026](https://cdn.paris.fr/paris/2026/04/07/arrete-marathon-2026-joRm.pdf),
articles16,18–20,22. Start/finish junctions corroborated from the [ASO2026runner guide](https://storage-aso.lequipe.fr/ASO/publicEvents_mar/semp26-guidedumarathonien-fr.pdf).
These are factual street/junction references, not a traced map stroke or copied artwork.
The latest published annual course at the24October2026check is2026;2027is unpublished.

Geometry: [Geofabrik Île-de-France22October2026 OSMextract](https://download.geofabrik.de/europe/france/ile-de-france-261022.osm.pbf),
© OpenStreetMap contributors, ODbL-1.0. The [Geofabrik licence offer](https://www.geofabrik.de/data/download.html#license) expressly permits any purpose with OSM acknowledgement and ODbL for derivative databases (checked24October2026). Original RDD ordered selection of actual OSM
road/pedestrian nodes follows Champs-Élysées/Marignan, Concorde, Vendôme/Opéra loop,
Louvre, Bastille/Nation, Picpus/Daumesnil, ChâteauVincennes/Pyramide/Ferme/Tourelle/
Pesage/Gravelle, Charenton/Daumesnil/Bastille, Seine/Tuileries/Concorde/Alma tunnels,
Kennedy/Versailles/Exelmans/Molitor, Auteuil/Hippodrome/Passy, Doumer/Trocadéro,
VictorHugo/HubertGermain and AvenueFoch near RueSpontini.

[Retained evidence](paris-2026-osm-evidence.json) contains550source ways with OSM IDs,
versions/tags/nodes,1960ordered node IDs and itinerary legs. Every consecutive position
is an existing OSM way edge. No synthetic geographic points or resampling. The data
and projected array remain ODbL. SHA256s for the raw extract, factual PDFs and retained
evidence are in [paris-2026.json](paris-2026.json). Reproduction is offline and tests
verify node/edge identity, annual-course corridors, edition, hashes and projection.
Development-only pyosmium read the PBF; no library code/dependency ships in the app.

Accuracy: approximate street/junction-lane reconstruction using October geography,
not surveyed April lane placement/certified racing line. Measured source line43.22km
is explicitly separate from42.195km equivalent progress. No elevation is inferred.
The visual is not official or suitable for navigation. No organiser map pixels/stroke,
logos, proprietary tiles, unlicensed GPX or GPL implementation are redistributed.

Investigated alternatives: ontempo explicitly offers OSM-redrawn ODbL venue data,
but its unversioned local Paris frame/edition could not be established, so none is
incorporated. Third-party downloadable annual GPXs without redistribution grants,
Mavr's mixed data terms and organiser map artwork were not ingested. Global Overpass
endpoints were unavailable/406/timed out; Swiss regional results do not establish
Paris coverage. The full Geofabrik extract allowed independent geographic construction.

Legacy [Paris2024Olympic data](paris-2024-olympic.json), its328positions, ID, projection,
context and [notice](PARIS_SOURCE_NOTICE.txt) remain unchanged solely to read previously
stored assignments. Never use it as an annual-course fallback or reroll old runs.

## City context and original artwork

[city-context.json](city-context.json) retains twelve Wikidata P625 landmark positions
under CC0, three New York projected anchors under the explicit ODbL offer, and original
Natural Earth public-domain Thames/Seine centreline vertices. Claim IDs, URLs, frozen
Natural Earth commit/hash and checked date are included. No river endpoints are
bridged and no streets are invented. River width is symbolic, not surveyed banks.

The city vignettes are original RDD Canvas work. Position data do not include copied
building images. Geographic anchors use the same frozen projection as their route;
legacy drawing-unit anchors are never silently reused in the new coordinate frame.

## Reproduction and compatibility

Run all five `scripts/generate-*-geometry.py --check` commands and
`scripts/generate-dream-context.py --check` from the repository root. They use retained
source data, need no network and regenerate the exact Java drawing coordinates.
Projections use a uniform north-up scale, without invented/densified positions.

The old unversioned and `*-v2` drawing IDs remain readable. Stable city preferences
select a new revision only for a future assignment; existing assignments never reroll.
All five new/legacy pairs have complete-backup instrumentation coverage. Geometry
and context are bundled resources, so no schema or user-backup bump was needed.

## Research exclusions

Public downloadability is not a redistribution grant. Unlicensed GPX candidates from
funmaps, datanews/ny-marathon and xavierchia/runwithfriends were not ingested. GPL
source was not copied. Mavr's OSM footer does not establish rights to its entire
mixed-source Paris line; its terms reserve associated data, so it was excluded.

London OSM relation 2086183/version 642 was examined, but naive ordered concatenation
would create 23 discontinuities/about 5.3 km of artificial connectors. It was not
bundled. No official map image, logo, proprietary tile or uncertain geometry is
substituted merely to make the visual look complete.

## Offline geographic context (all five cities)

The `basemap-*.json` databases contain factual road lines and water/green-space
polygons from OpenFreeMap's frozen `planet/20261018_080001_pt` dataset, OpenMapTiles
schema, OpenStreetMap data (ODbL-1.0). Each records the exact tile URLs/SHA256s,
checked date, source bounds, retained latitude/longitude, licence and simplification.
These data are separately ODbL, not relicensed MIT with the original app renderer.
No map images, styles, proprietary tiles, fonts, icons or source code are copied.

OpenFreeMap explicitly allows bounded offline use under https://openfreemap.org/tos/.
Development preparation used67tiles/10,342,383bytes/3parallelrequests and an app-specific
User-Agent. The OSMF vector endpoint has a different policy prohibiting offline bulk
use; its research download is excluded. The Android app never requests either service.

`prepare-dream-basemaps.py --cache /path/to/source-cache` can reconstruct the retained
inputs from that existing cache. Only an explicit `--fetch` allows bounded development
network retrieval. Review changes; this is not an automatic regeneration of stable
user assignments. The original MVT wire-format decoder is development-only.
`generate-dream-basemaps.py --check` reproduces the compact bundled `.rddmap` files
entirely offline from the committed retained data. No source tiles ship in the APK.

Projection uses each unchanged course frame. NYC context converts WGS84 through
UTM18; metre-scale WGS84/NAD83 datum uncertainty is acceptable only for this decorative
overview. No converted context becomes user distance, GPS or analysis evidence.
Road fragments below roughly120m are omitted; RDP retains original positions with
0.00007degree tolerance. This is incomplete overview context, not navigation or a
claim that historical course editions match the2026street snapshot exactly.

The parser bounds bytes/features/positions, rejects nonfinite/mismatched data and
requires closed polygons. Domain tests load all five shipped files. Android tests
also parse every packaged extract on API28/35/36. Three cached unit-space Canvas
paths are prepared off-main, so redraw does not iterate/decode all source features.
Landmark labels remain separate accessible text. Attribution is visible on every
geographic card and full data/ODbL notices are available offline in Settings.

The final context extent covers the fitted5:4 course-plus-landmark viewport with4%
margin, rather than only a narrow course bounding box. This corrects abrupt extract
edges seen during actual-build review.67source tiles/10,342,383bytes produce235,998
retained positions across five compact extracts (4,012,113bytes before APK compression).
The app parser remains bounded:3MB,20,000features and150,000positions per extract.
The largest is NYC,108,706positions, within these limits; redraw still uses three
cached paths. The Canvas aspect is stable across widths rather than a fixed height.
