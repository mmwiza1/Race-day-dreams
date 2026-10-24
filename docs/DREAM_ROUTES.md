# Dream Routes — 0.8.2-test1

Dream Routes show how a completed run compares with a section of a marathon-distance
course. They never claim that the runner physically visited the city. All geometry,
city illustrations and progress rendering work offline, without tiles or Internet
permission. They are not official race maps or suitable for navigation.

## Current geographic revisions

| City choice | Geometry ID | Source and limitation |
| --- | --- | --- |
| London | `london-historical-geo-v1` | 164 original positions from Georgios Karamanis' MIT-licensed historical GeoJSON, published in 2023. Race edition and survey accuracy are unspecified. |
| Berlin | `berlin-2014-geo-v1` | 191 retained positions from an MIT-licensed 2014 participant trace by Alexandre Donciu-Julin. Historical approximate GPS evidence. |
| Tokyo | `tokyo-2026-geo-v1` | 91 retained original positions from kekomat's explicitly CC0 2026 participant trace, simplified at 25 m. |
| New York | `new-york-2026-geo-v1` | All 371 delivered projected positions from MARATHON TRIP's explicitly ODbL-offered road-centreline data. Approximate 2026 context, not certified geometry. |
| Paris | `paris-2026-geo-v1` | Annual Marathon de Paris2026:1960retained OSM positions, reconstructed from the statutory street itinerary; ODbL via Geofabrik. Approximate road/junction line, not certified geometry. |

Paris represents the **annual Schneider Electric Marathon de Paris,12April2026**.
Police order2026-00388 provides the street itinerary; OpenStreetMap/Geofabrik provides
geography. The unpublished2027 course is not claimed. See the detailed source audit
in [dream-route-data](dream-route-data/README.md). Existing Olympic geometry remains
immutable solely for old stored assignments; it is never selected for a new run.
No organiser map artwork, logos, sponsor branding, photographs or third-party
application source were copied. Source hashes, revision IDs, notices and retained
coordinates are in [dream-route-data](dream-route-data/README.md).

## Progress and presentation

The saved course appears beneath an original atmospheric illustrated city hero.
The blue equivalent section dominates a quieter full-course line. Full-course Start
and Finish pins are distinct from the blue section-end rings: a saved offset is not
silently moved to the race start. Collision-aware five-kilometre and factual landmark
labels are subordinate to the highlighted section.

Progress shows completed distance, percentage, an accessible bar, and completed /
remaining / total summary. All arithmetic uses exactly42,195metres before display
rounding; over-marathon distance is explained separately. The equivalence sentence
never claims the runner physically visited the city. Preserved historical IDs keep
their own course/edition labels, including archived Olympic Paris assignments.

Five original bundled hero illustrations and fifteen landmark posters are decorative
app-owned art, separate from factual geographic data. Each current course uses its
three retained sourced landmark labels. AnnualParis2026 uses EiffelTower, Château de
Vincennes and Louvre; it does not borrow unrelated landmarks from the Olympic course.
Illustrated skyline positions are atmospheric, not geographic placement evidence.
Asset provenance, exact crops, sizes and SHA256s are in
[dream-artwork-0.8.2.json](dream-artwork-0.8.2.json). No organiser graphics/photos copied.

The42,195m equivalent-distance axis remains separate from measured source trace length.
Sections retain all intervening original vertices and interpolate their endpoints;
this presentation neither changes actualGPS nor calls approximate lines certified.

Retained landmark positions use fifteen WikidataP625statements(CC0) and three NewYork
ODbLanchors; Thames/Seine retain NaturalEarth public-domain centreline vertices.
The existing sourced ODbL road/water/green-space frame is unchanged. Tiny road fragments
may be omitted only from drawing to reduce visual noise; stored data remain intact.
No streets, park labels or geographic scenery are invented.

Only the displayed city's four bounded WebPs are decoded on an IOdispatcher, with an
8MiB cache. Course/context paths are prepared off-main and retained for at most3cities;
raw map coordinate objects are released after path preparation. No runtime image,
map-tile or geographic request occurs. At200%font posters and summary columns stack,
text wraps and content scrolls. Text/semantics provide colour-independent progress.
Source attribution and a compact About control remain readable offline.

## Stable assignments and compatibility

City preferences remain `london-v2`, `berlin-v2`, `paris-v2`, `tokyo-v2` and
`new-york-v2`. Only new assignments choose the current geometry revision. Changing
preferences never rerolls an existing activity. Every old unversioned/v2/Olympic geometry
remains readable, with its original stored offset and distance.

Complete-backup instrumentation covers old and new assignments for all five cities.
No schema or backup-format change was necessary for these bundled data revisions.
ODbL coordinate databases remain separately licensed from MIT application code;
full notices and the ODbL text are bundled and readable in Settings.

## Reproduce and verify

Run `scripts/generate-berlin-geometry.py`, `generate-tokyo-geometry.py`,
`generate-london-geometry.py`, `generate-new-york-geometry.py`,
`generate-paris-geometry.py` and `generate-dream-context.py` with `--check` from the
repository root. These checks need no network. Domain tests also cover bounds,
interpolation, full-polyline clipping, old IDs and immutable registry reuse.

Actual-build screenshot/reference acceptance is documented separately in
[VISUAL_REVIEW_0_8_2.md](VISUAL_REVIEW_0_8_2.md). A passing data test does not prove
visual fidelity, physical GNSS accuracy or a human accessibility check.

### Five-city offline context follow-up

Original RDD styling now draws sourced streets, green-space and water polygons behind
the preserved full course and equivalent section. Selected ODbL geographic data come
from a bounded, expressly permitted OpenFreeMap extract (not downloaded imagery or
styles). There is no map SDK, runtime tile client, Internet permission or added runtime
dependency. Exact source snapshot, per-tile checksums, retained positions, licensing
and transformations are recorded in dream-route-data/basemap-*.json.

The parser bounds input and verifies course-frame identity. Immutable data are loaded
on an IO dispatcher and paths prepared on a background dispatcher; the canvas draws
three cached context paths, retaining the route/highlight/markers on top. Existing
legacy frames use their original presentation. Actual-build five-city/dark/large-font captures are a separate visual acceptance gate;
see the current verification and visual-review documents for observed results.
