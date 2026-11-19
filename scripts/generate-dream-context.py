#!/usr/bin/env python3
"""Project independently sourced city context in each frozen course's exact frame."""
import json
import math
import sys
from pathlib import Path

root = Path('docs/dream-route-data')
data = json.loads((root/'city-context.json').read_text())
files = {'london-historical-geo-v1':'london-historical.json',
         'berlin-2014-geo-v1':'berlin-2014-candidate.json',
         'tokyo-2026-geo-v1':'tokyo-2026.json',
         'paris-2024-olympic-geo-v1':'paris-2024-olympic.json',
         'paris-2026-geo-v1':'paris-2026.json',
         'new-york-2026-geo-v1':'new-york-2026.json'}

def projectors(route_id):
    route = json.loads((root/files[route_id]).read_text())
    points = route['geometry']
    if 'frame' in route:
        xs,ys = [p[0] for p in points],[-p[1] for p in points]
        raw = lambda a,b:(a,-b)
    else:
        cosine = math.cos(math.radians(sum(p[0] for p in points)/len(points)))
        xs,ys = [p[1]*cosine for p in points],[-p[0] for p in points]
        raw = lambda lat,lon:(lon*cosine,-lat)
    scale = 9/max(max(xs)-min(xs),max(ys)-min(ys))
    xoff,yoff = (10-(max(xs)-min(xs))*scale)/2,(10-(max(ys)-min(ys))*scale)/2
    def project(a,b):
        x,y = raw(a,b)
        return (x-min(xs))*scale+xoff,(y-min(ys))*scale+yoff
    return project

from dream_kotlin import format_source, SHARED

def vertex(p):
    return 'DreamRoutes.Vertex(%.8f,%.8f)' % p

source = 'package org.racedaydreams.domain.support\n\nimport kotlin.jvm.JvmStatic\nimport org.racedaydreams.domain.coaching.Immutable\n\n' + '/** Reproducible geographic context, not a street map or navigation.\n * Wikidata P625 landmarks: CC0. Natural Earth river centrelines: public domain.\n * NYC projected landmark DATA: ODbL-1.0, (c) OpenStreetMap contributors,\n * prepared by MARATHON TRIP. Coordinate data retain their own licences.\n * Original wrapper code/illustrations: MIT. Exact sources/positions/transform:\n * docs/dream-route-data/city-context.json, generate-dream-context.py.\n */' + """
object DreamCartography {
    data class River(private val label:String?,private var vertices:List<DreamRoutes.Vertex>?) {
        init {vertices=Immutable.copyList(vertices ?: throw NullPointerException())}
        fun label():String?=label
        fun vertices():List<DreamRoutes.Vertex> = vertices!!
        override fun toString():String="River[label=$label, vertices=$vertices]"
    }
    @JvmStatic fun landmarks(routeId:String):List<DreamRoutes.Landmark> = when(routeId){
"""
for route_id,context in data['cities'].items():
    project = projectors(route_id)
    assert len(context['landmarks'])==3
    rows=[]
    for l in context['landmarks']:
        a,b = (l['x'],l['y']) if 'x' in l else (l['latitude'],l['longitude'])
        x,y=project(a,b)
        if not math.isfinite(x+y) or not (-1<x<11 and -1<y<11):
            raise SystemExit('Landmark outside researched city frame: '+l['label'])
        rows.append('DreamRoutes.Landmark(%s,%.8f,%.8f,%s)' % (json.dumps(l['label'],ensure_ascii=False),x,y,json.dumps(l['shape'])))
    source+='        '+json.dumps(route_id)+' -> Immutable.list('+',\n                '.join(rows)+')\n'
source+='        else -> Immutable.list()\n    }\n    @JvmStatic fun rivers(routeId:String):List<River> = when(routeId){\n'
for route_id,context in data['cities'].items():
    project=projectors(route_id)
    rows=[]
    for river in context['rivers']:
        points=[project(*p) for p in river['geometry']]
        assert len(points)>1
        rows.append('River('+json.dumps(river['label'])+',Immutable.list('+',\n                '.join(vertex(p) for p in points)+'))')
    if rows:source+='        '+json.dumps(route_id)+' -> Immutable.list('+',\n                '.join(rows)+')\n'
source+='        else -> Immutable.list()\n    }\n}\n'
source=format_source('DreamCartography',source)
target=SHARED/'DreamCartography.kt'
if '--check' in sys.argv:
    if target.read_text()!=source:
        raise SystemExit('City context differs from retained source coordinates')
    print(str(sum(len(c['landmarks']) for c in data['cities'].values()))+' sourced landmark anchors and river projections reproduce exactly')
else:
    target.write_text(source)
