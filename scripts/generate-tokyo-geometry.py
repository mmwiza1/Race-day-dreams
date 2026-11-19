#!/usr/bin/env python3
"""Reproduce the bundled projection from retained source vertices, never invent coordinates.
Run from repository root. Uses the existing pinned Kotlin formatter; no application/runtime dependency.
"""
import json,math
from pathlib import Path
j=json.loads(Path('docs/dream-route-data/tokyo-2026.json').read_text()); pts=j['geometry'];lat0=sum(x[0] for x in pts)/len(pts);xs=[x[1]*math.cos(math.radians(lat0)) for x in pts];ys=[-x[0] for x in pts];scale=9/max(max(xs)-min(xs),max(ys)-min(ys));xoff=(10-(max(xs)-min(xs))*scale)/2;yoff=(10-(max(ys)-min(ys))*scale)/2
vs=[((x-min(xs))*scale+xoff,(y-min(ys))*scale+yoff) for x,y in zip(xs,ys)]
from dream_kotlin import geometry_source, SHARED
s = geometry_source('Tokyo2026Geometry', "tokyo-2026-geo-v1", "Tokyo", vs, '/** Geographic coordinates from kekomat/marathon-gpx-dataset, CC0-1.0.\n * Full notice: docs/dream-route-data/TOKYO_SOURCE_LICENCE.txt.\n * Tokyo2026 approximate participant GPS trace, not certified/official geometry.\n * Position-only original vertices, simplified25m; north-up uniform projection.\n * Marathon-equivalent progress is separate from the42,746.8m observed trace.\n */')
target = SHARED / 'Tokyo2026Geometry.kt'
import sys
if '--check' in sys.argv:
    if target.read_text()!=s: raise SystemExit('Tokyo projection differs from retained coordinate evidence')
    print('Tokyo geometry projection matches91 retained source vertices')
else: target.write_text(s)
