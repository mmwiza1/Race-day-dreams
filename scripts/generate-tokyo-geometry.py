#!/usr/bin/env python3
"""Reproduce the bundled projection from retained source vertices, never invent coordinates.
Run from repository root. No downloads or third-party dependencies.
"""
import json,math
from pathlib import Path
j=json.loads(Path('docs/dream-route-data/tokyo-2026.json').read_text()); pts=j['geometry'];lat0=sum(x[0] for x in pts)/len(pts);xs=[x[1]*math.cos(math.radians(lat0)) for x in pts];ys=[-x[0] for x in pts];scale=9/max(max(xs)-min(xs),max(ys)-min(ys));xoff=(10-(max(xs)-min(xs))*scale)/2;yoff=(10-(max(ys)-min(ys))*scale)/2
vs=[((x-min(xs))*scale+xoff,(y-min(ys))*scale+yoff) for x,y in zip(xs,ys)]
s='''package org.racedaydreams.domain.support;
import java.util.*;

/** Geographic coordinates from kekomat/marathon-gpx-dataset, CC0-1.0.
 * Full notice: docs/dream-route-data/TOKYO_SOURCE_LICENCE.txt.
 * Tokyo2026 approximate participant GPS trace, not certified/official geometry.
 * Position-only original vertices, simplified25m; north-up uniform projection.
 * Marathon-equivalent progress is separate from the42,746.8m observed trace.
 */
final class Tokyo2026Geometry {
    private Tokyo2026Geometry() {}
    static DreamRoutes.Route route() {return new DreamRoutes.Route("tokyo-2026-geo-v1","Tokyo",Arrays.asList(
'''
s+=',\n'.join('        new DreamRoutes.Vertex(%.8f,%.8f)'%p for p in vs)+'\n    ));}\n}\n';target=Path('domain/src/main/java/org/racedaydreams/domain/support/Tokyo2026Geometry.java')
import sys
if '--check' in sys.argv:
    if target.read_text()!=s: raise SystemExit('Tokyo projection differs from retained coordinate evidence')
    print('Tokyo geometry projection matches91 retained source vertices')
else: target.write_text(s)
