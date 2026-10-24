# Dream Routes — 0.8.1

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

The full course remains visible, with a contrasting equivalent section, a ring for
its start and a checkered finish marker. Five-kilometre ticks, numbered landmark
anchors, labels, percentage and equivalent distance provide non-colour information.
A small About this route control exposes the source and limitations.

The 42,195 m progress axis is an **equivalent-distance axis**, separate from measured
source trace length. Participant traces and simplified road centrelines can measure
longer or shorter. The app neither edits actual GPS nor calls those lengths certified.
Sections retain intervening original vertices; they do not cut chords across corners.
Exact start/finish boundaries and over-marathon handling are tested.

Eighteen retained landmark anchors (fifteen for current routes, three for legacy Paris) use sourced positions in each course's exact projection:
fifteen Wikidata P625 statements (CC0) and three New York ODbL anchors. The miniature
Canvas illustrations are original RDD work. Thames and Seine context retains original
Natural Earth public-domain centreline vertices. Their symbolic width does not claim
surveyed river banks. No streets or scenery are invented to fill blank space.

The renderer caches bounds and highlighted sections. The immutable geometry registry
is built once and reused for assignment, history and redraw lookups. Landmark labels
stack at large font sizes. Canvas accessibility describes the course, equivalent
section and numbered landmarks; written labels remain available independently.

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
[VISUAL_REVIEW_0_8_1.md](VISUAL_REVIEW_0_8_1.md). A passing data test does not prove
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
legacy frames use their original presentation. Actual-build five-city/dark/large-font
captures must still be reviewed after this change before closing the fidelity gate.
