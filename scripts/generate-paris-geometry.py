#!/usr/bin/env python3
"""Reproduce annual Paris2026 geometry; preserve the immutable legacy Olympic drawing."""
import json
import math
import sys
from pathlib import Path

data = json.loads(Path('docs/dream-route-data/paris-2026.json').read_text())
points = data['geometry']
if len(points) != 1960 or data['edition'] != 2026 or data['licence'] != 'ODbL-1.0':
    raise SystemExit('Unexpected Paris source revision/count/licence')
if any(not (48.7 < lat < 49 and 2 < lon < 2.5) for lat,lon in points):
    raise SystemExit('Paris coordinates outside documented geographic bounds')
latitude = sum(p[0] for p in points)/len(points)
xs = [lon*math.cos(math.radians(latitude)) for lat,lon in points]
ys = [-lat for lat,lon in points]
scale = 9/max(max(xs)-min(xs),max(ys)-min(ys))
xoff = (10-(max(xs)-min(xs))*scale)/2
yoff = (10-(max(ys)-min(ys))*scale)/2
vertices = [((x-min(xs))*scale+xoff,(y-min(ys))*scale+yoff) for x,y in zip(xs,ys)]
source = '''package org.racedaydreams.domain.support;
import java.util.*;

/** Geographic DATA: ODbL-1.0, (c) OpenStreetMap contributors, via Geofabrik.
 * Annual Schneider Electric Marathon de Paris2026 street-itinerary reconstruction.
 * Original wrapper: MIT. All selected OSM positions retained; no invented coordinates.
 * Sources, node/way/version evidence, factual itinerary and limitations:
 * docs/dream-route-data/paris-2026.json and paris-2026-osm-evidence.json.
 * Approximate road/junction line, not a certified racing line or navigation.
 */
final class Paris2026Geometry {
    private Paris2026Geometry() {}
    static DreamRoutes.Route route() {return new DreamRoutes.Route("paris-2026-geo-v1","Paris · Marathon de Paris 2026",Arrays.asList(
'''
source += ',\n'.join('        new DreamRoutes.Vertex(%.8f,%.8f)' % p for p in vertices)
source += '\n    ));}\n}\n'
target = Path('domain/src/main/java/org/racedaydreams/domain/support/Paris2026Geometry.java')
if '--check' in sys.argv:
    if target.read_text() != source:
        raise SystemExit('Paris projection differs from retained geographic source positions')
    print('Paris geometry projection matches1960 retained source positions')
else:
    target.write_text(source)

# Previously saved Olympic assignments must remain byte-for-byte reproducible.
import runpy
runpy.run_path('scripts/generate-paris-legacy-geometry.py', run_name='__main__')
