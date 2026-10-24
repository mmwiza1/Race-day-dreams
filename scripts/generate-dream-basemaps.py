#!/usr/bin/env python3
"""Offline reproduction of original-style basemap DATA, never map tiles/artwork."""
import json
import math
import runpy
import sys
from pathlib import Path

root = Path(__file__).resolve().parent.parent
projection = runpy.run_path(str(root/'scripts/dream-projection.py'))
courses = {json.loads(p.read_text())['id']: p for p in (root/'docs/dream-route-data').glob('*.json')
           if 'geometry' in json.loads(p.read_text()) and 'id' in json.loads(p.read_text())}
courses['berlin-2014-geo-v1'] = root/'docs/dream-route-data/berlin-2014-candidate.json'
count = vertices = 0
for path in sorted((root/'docs/dream-route-data').glob('basemap-*.json')):
    data = json.loads(path.read_text())
    route = json.loads(courses[data['routeId']].read_text())
    project = projection['geographic_frame'](route)
    assert data['format'] == 1 and data['licence'] == 'ODbL-1.0'
    assert 0 < len(data['sourceTiles']) < 100
    assert all(t['url'].startswith('https://tiles.openfreemap.org/planet/20261018_080001_pt/')
               and len(t['sha256']) == 64 for t in data['sourceTiles'])
    assert 0 < len(data['features']) <= 20000
    lines = ['RDD-MAP 1', data['routeId']]
    for feature in data['features']:
        kind = feature['kind']
        assert kind in ('road', 'water', 'green')
        paths = []
        for points in feature['paths']:
            assert len(points) >= (2 if kind == 'road' else 4)
            if kind != 'road':
                assert points[0] == points[-1]
            projected = [project(*p) for p in points]
            assert all(math.isfinite(x+y) and abs(x) < 100 and abs(y) < 100 for x, y in projected)
            paths.append(' '.join('%.5f,%.5f' % p for p in projected))
            vertices += len(points)
        lines.append(kind+' '+'|'.join(paths))
    output = '\n'.join(lines)+'\n'
    assert len(output.encode()) <= 3_000_000
    target = root/'app/src/main/assets/dream-basemaps'/(data['routeId']+'.rddmap')
    if '--check' in sys.argv:
        assert target.read_text() == output, 'Basemap differs from retained ODbL data: '+str(target)
    else:
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_text(output)
    count += 1
assert count == 6, 'Five current course frames and the retained legacy Paris frame require an extract'
print(f'{count} original-style geographic basemaps reproduce exactly; {vertices} retained positions')
