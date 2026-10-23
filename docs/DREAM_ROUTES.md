# Current geographic checkpoint — three cities

New London assignments use `london-historical-geo-v1`: all164positions of the
historical course line published in2023 in Georgios Karamanis' MIT-licensed
TidyTuesday repository. The source does not specify the race edition or survey
accuracy; the app explicitly says so. This is an already simplified geographic
line, **not** a participant recording, current certified course or official map.
Only the named full-course feature is retained, without its zero-altitude values
or alternative starts. Approximate observed length42.78km is distinct from the
42.195km equivalent-progress axis. Source commit/blob/licence and all positions are
in `dream-route-data/london-historical.json`; the original MIT notice is bundled and
readable in Settings. Reproduce with `generate-london-geometry.py --check`.

Berlin2014 and Tokyo2026 geographic traces remain unchanged. Paris and New York
still need safely redistributable geographic replacements. Rich original five-city
presentation and actual screenshot/reference fidelity remain unfinished.

All legacy geometry IDs and assignments remain valid. City preferences stay stable;
only future London assignments select the new revision. New and old London complete
backup round trips have instrumentation coverage. Equivalent start/finish boundaries
now return the exact retained endpoints, avoiding floating-point finish drift.

London OSM relation2086183/version642 was investigated but rejected for this import:
its ordered way list has23discontinuities. No false straight-line bridges from that
candidate are bundled. No organiser artwork, map tiles or runtime network dependency
has been introduced.

## Earlier geographic checkpoints (historical)

# 0.8.1 geographic upgrade checkpoint

New Berlin assignments use `berlin-2014-geo-v1`:191 original retained vertices from
an MIT-licensed participant GPS trace. Source/edition/licence/transformation and the
reproducible offline projection are in [dream-route-data](dream-route-data/README.md).
This is approximate2014 participant evidence, not current/certified course geometry.
It carries no timestamps, HR or athlete identity. The marathon-equivalent axis is
separate from its observed43.01km geographic trace length.

The stable `berlin-v2` city preference selects the new revision only for future
assignments. All stored v1/v2 assignments continue using their exact old geometries.
Complete-backup tests cover old and new assignments plus the unchanged city setting.
The full source MIT notice ships in the APK and can be opened from Settings.

The highlighted polyline retains every intervening source vertex; it no longer
samples straight chords across corners. Rendering caches that section independently
of recomposition. Duplicate zero-length vertices do not produce NaN positions.
The section finish uses a checkered marker rather than the old schematic runner.

London, Paris, Tokyo and New York remain legacy schematics pending licence-safe,
geographically meaningful replacements. **The full five-city design gate has not
passed.** The historical documentation below describes those retained v2 assets.

# Dream Routes v2 provenance

Checked 23 October 2026. These are original, independently drawn **stylised course-inspired
visuals**, not surveyed routes, official race maps or navigation. No organiser GPX,
GeoJSON, map artwork, logos, typography, tiles or images are bundled. Public availability
of a download is not a reuse licence; no clear redistribution grant was established.

| Geometry ID | City / factual edition reference | Factual source reviewed | Original drawing cues |
| --- | --- | --- | --- |
| london-v2 | London / 2026 course context | https://www.londonmarathonevents.co.uk/london-marathon/course | southeast start, westward section, bridge, east loop and central finish; Thames/bridge/palace icons |
| berlin-v2 | Berlin / current 2026 course page | https://www.bmw-berlin-marathon.com/en/your-race/course/ | central start/finish and broad city loop; gate, park, skyline icons |
| paris-v2 | Paris / 2026 | https://www.schneiderelectricparismarathon.com/en/information/route-and-profiles | eastward leg, riverward return and western loop; tower/river/arch icons |
| tokyo-v2 | Tokyo / 2027 published course context | https://www.marathon.tokyo/en/about/course/ | west-to-central approach, eastern excursions and southern return; skyline/gate/tower icons |
| new-york-v2 | New York / 2026 | https://prod.nyrr.org/tcsnycmarathon/the-course | southern bridge approach, northward borough progression and park finish cue; bridge/skyline/park icons |

Edition references identify the factual pages, **not accuracy certification of the
geometry**. NYRR documents a 2026 Bronx adjustment; this drawing does not promise those
street-level details. Berlin offers GPX but its legal notice protects website content:
https://www.bmw-berlin-marathon.com/impressum . No GPX was ingested or traced.
The drawings are original MIT project assets in `DreamRoutes.java`, in normalized
0–10 drawing units. Segment lengths normalize to a **42,195 m equivalent**, not real
geographic distance. Landmarks are illustrative cues, not precise waypoint coordinates.
All have 28–34 independent anchors, 5 km progress markers and original Canvas symbols.

The old `london`, `berlin`, `paris`, `tokyo`, `new-york` geometries are retained unchanged
for old assignments. New assignments use `*-v2`; existing assignments never reroll.
Preferences affect future assignments only. Live selection is persisted with recording
origin and transferred to the finished activity. The live secondary panel consumes
accepted recorder distance; it does not replace navigation or the actual route.

A future exact open dataset needs a separately documented licence/edition assessment,
a new geometry ID and preserved old geometries. Do not silently replace these assets.

## Tokyo geographic upgrade

New Tokyo assignments now use tokyo-2026-geo-v1, a separately versioned approximate
participant trace from an explicit CC0-1.0 dataset. It retains91 original position
vertices without timestamps or health data. Source hashes, original point indices,
simplification, limitations and reproduction are in dream-route-data/README.md and
tokyo-2026.json. Old Tokyo assignments and city preferences remain stable. Berlin
and Tokyo now have geographic revisions; London/Paris/New York and richer original
city presentation remain unfinished. Do not call this the completed0.8.1visual gate.
