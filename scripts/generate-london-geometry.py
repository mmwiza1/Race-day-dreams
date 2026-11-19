#!/usr/bin/env python3
"""Reproduce the north-up London projection from unchanged retained source positions."""
import json
import math
import sys
from pathlib import Path

data = json.loads(Path('docs/dream-route-data/london-historical.json').read_text())
points = data['geometry']
if len(points) != data['sourcePointCount'] or data['edition'] is not None:
    raise SystemExit('Historical London source count/unspecified edition changed')
if any(not (51.4 < lat < 51.6 and -.2 < lon < .1) for lat, lon in points):
    raise SystemExit('London coordinates outside researched bounds')
latitude = sum(p[0] for p in points) / len(points)
xs = [lon * math.cos(math.radians(latitude)) for lat, lon in points]
ys = [-lat for lat, lon in points]
scale = 9 / max(max(xs) - min(xs), max(ys) - min(ys))
xoff = (10 - (max(xs) - min(xs)) * scale) / 2
yoff = (10 - (max(ys) - min(ys)) * scale) / 2
vertices = [((x-min(xs))*scale+xoff, (y-min(ys))*scale+yoff) for x,y in zip(xs,ys)]
from dream_kotlin import geometry_source, SHARED
source = geometry_source('LondonHistoricalGeometry', "london-historical-geo-v1", "London", vertices, '/** Historical course line published2023 by Georgios Karamanis, MIT.\n * Source/licence: docs/dream-route-data/london-historical.json and LONDON_SOURCE_LICENCE.txt.\n * All164 original source positions retained. Edition/survey accuracy unspecified.\n * Already simplified geometry; not an official current course or navigation.\n * Observed42.78km source line is separate from the marathon-equivalent axis.\n */')
target = SHARED / 'LondonHistoricalGeometry.kt'
if '--check' in sys.argv:
    if target.read_text() != source:
        raise SystemExit('London projection differs from retained geographic source positions')
    print('London geometry projection matches164 retained source positions')
else:
    target.write_text(source)
