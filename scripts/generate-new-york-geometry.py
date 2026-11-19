#!/usr/bin/env python3
"""Reproduce ODbL New York course data; uses the existing pinned Kotlin formatter, not an application dependency."""
import json
import math
import sys
from pathlib import Path

data = json.loads(Path('docs/dream-route-data/new-york-2026.json').read_text())
points = data['geometry']
if len(points) != 371 or data['frame'] != {'crs': 'EPSG:26918', 'anchor': [579000, 4495000]}:
    raise SystemExit('Unexpected New York source frame or vertex count')
if any(not (0 < x < 13000 and 0 < y < 25000) for x, y in points):
    raise SystemExit('New York coordinates outside retained source bounds')
length = sum(math.hypot(b[0]-a[0], b[1]-a[1]) for a,b in zip(points,points[1:]))
if abs(length-data['retainedPolylineMetres']) > .001:
    raise SystemExit('New York retained centreline length changed')
xs = [p[0] for p in points]
ys = [-p[1] for p in points]
scale = 9 / max(max(xs)-min(xs), max(ys)-min(ys))
xoff = (10-(max(xs)-min(xs))*scale)/2
yoff = (10-(max(ys)-min(ys))*scale)/2
vertices = [((x-min(xs))*scale+xoff, (y-min(ys))*scale+yoff) for x,y in zip(xs,ys)]
from dream_kotlin import geometry_source, SHARED
source = geometry_source('NewYork2026Geometry', "new-york-2026-geo-v1", "New York", vertices, '/** Geometry DATA: ODbL-1.0, (c) OpenStreetMap contributors; prepared by MARATHON TRIP.\n * Original application wrapper code: MIT. Do not relicense the coordinate database.\n * Source, retained projected coordinates, licence and transformation:\n * docs/dream-route-data/new-york-2026.json, NEW_YORK_SOURCE_NOTICE.txt, ODbL-1.0.txt.\n * All371 delivered vertices retained, uniformly scaled, north-up EPSG:26918.\n * Approximate2026 road centreline, not official/certified geometry or navigation.\n * The42,195m equivalent axis is separate from the42.67km retained centreline.\n */')
target = SHARED / 'NewYork2026Geometry.kt'
if '--check' in sys.argv:
    if target.read_text() != source:
        raise SystemExit('New York projection differs from retained geographic source positions')
    print('New York geometry projection matches371 retained source positions')
else:
    target.write_text(source)
