#!/usr/bin/env python3
"""Reproduce the bundled projection from retained source vertices, never invent coordinates.
Run from repository root. No downloads or third-party dependencies.
"""
import json,math
from pathlib import Path
j=json.loads(Path('docs/dream-route-data/berlin-2014-candidate.json').read_text()); pts=j['geometry'];lat0=sum(x[0] for x in pts)/len(pts);xs=[x[1]*math.cos(math.radians(lat0)) for x in pts];ys=[-x[0] for x in pts];scale=9/max(max(xs)-min(xs),max(ys)-min(ys));xoff=(10-(max(xs)-min(xs))*scale)/2;yoff=(10-(max(ys)-min(ys))*scale)/2
vs=[((x-min(xs))*scale+xoff,(y-min(ys))*scale+yoff) for x,y in zip(xs,ys)]
s='''package org.racedaydreams.domain.support;
import java.util.*;

/** Geographic coordinates adapted from MIT-licensed participant GPX, copyright2023
 * Alexandre Donciu-Julin. Full notice: docs/dream-route-data/BERLIN_SOURCE_LICENCE.txt.
 * Berlin2014 approximate historical GPS trace, not certified/current official geometry.
 * Only position retained; local equirectangular projection, north up, uniform scale.
 * The marathon-equivalent progress axis is separate from the43,008.3m measured trace.
 */
final class Berlin2014Geometry {
    private Berlin2014Geometry() {}
    static DreamRoutes.Route route() {return new DreamRoutes.Route("berlin-2014-geo-v1","Berlin",Arrays.asList(
'''
s+=',\n'.join('        new DreamRoutes.Vertex(%.8f,%.8f)'%p for p in vs)+'\n    ));}\n}\n';target=Path('domain/src/main/java/org/racedaydreams/domain/support/Berlin2014Geometry.java')
import sys
if '--check' in sys.argv:
    if target.read_text()!=s: raise SystemExit('Berlin projection differs from retained coordinate evidence')
    print('Berlin geometry projection matches191 retained source vertices')
else: target.write_text(s)
