#!/usr/bin/env python3
"""Reproduce the bundled projection from retained source vertices, never invent coordinates.
Run from repository root. Uses the existing pinned Kotlin formatter; no application/runtime dependency.
"""
import json,math
from pathlib import Path
j=json.loads(Path('docs/dream-route-data/berlin-2014-candidate.json').read_text()); pts=j['geometry'];lat0=sum(x[0] for x in pts)/len(pts);xs=[x[1]*math.cos(math.radians(lat0)) for x in pts];ys=[-x[0] for x in pts];scale=9/max(max(xs)-min(xs),max(ys)-min(ys));xoff=(10-(max(xs)-min(xs))*scale)/2;yoff=(10-(max(ys)-min(ys))*scale)/2
vs=[((x-min(xs))*scale+xoff,(y-min(ys))*scale+yoff) for x,y in zip(xs,ys)]
from dream_kotlin import geometry_source, SHARED
s = geometry_source('Berlin2014Geometry', "berlin-2014-geo-v1", "Berlin", vs, '/** Geographic coordinates adapted from MIT-licensed participant GPX, copyright2023\n * Alexandre Donciu-Julin. Full notice: docs/dream-route-data/BERLIN_SOURCE_LICENCE.txt.\n * Berlin2014 approximate historical GPS trace, not certified/current official geometry.\n * Only position retained; local equirectangular projection, north up, uniform scale.\n * The marathon-equivalent progress axis is separate from the43,008.3m measured trace.\n */')
target = SHARED / 'Berlin2014Geometry.kt'
import sys
if '--check' in sys.argv:
    if target.read_text()!=s: raise SystemExit('Berlin projection differs from retained coordinate evidence')
    print('Berlin geometry projection matches191 retained source vertices')
else: target.write_text(s)
