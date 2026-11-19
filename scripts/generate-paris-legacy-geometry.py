#!/usr/bin/env python3
"""Reproduce the openly licensed Paris2024 geographic drawing without invented points."""
import json
import math
import sys
from pathlib import Path

data = json.loads(Path('docs/dream-route-data/paris-2024-olympic.json').read_text())
points = data['geometry']
if len(points) != 328 or data['sourceRevision'] != 931916019 or data['licence'] != 'ODbL-1.0':
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
from dream_kotlin import geometry_source, SHARED
source = geometry_source('Paris2024Geometry', "paris-2024-olympic-geo-v1", "Paris · 2024 Olympic course", vertices, '/** Geometry DATA: ODbL-1.0, Thomas Krakow / Running-tomy, via Wikimedia Commons.\n * Original application wrapper code: MIT; coordinate database remains ODbL.\n * Source revision931916019, retained positions, licence and transformation:\n * docs/dream-route-data/paris-2024-olympic.json, PARIS_SOURCE_NOTICE.txt, ODbL-1.0.txt.\n * All328 source vertices retained, uniform north-up projection.\n * Paris2024 Olympic course, NOT the annual Paris Marathon. Approximate community\n * geometry, not official/certified or navigation. No Olympic marks or artwork.\n */')
target = SHARED / 'Paris2024Geometry.kt'
if '--check' in sys.argv:
    if target.read_text() != source:
        raise SystemExit('Paris projection differs from retained geographic source positions')
    print('Paris geometry projection matches328 retained source positions')
else:
    target.write_text(source)
