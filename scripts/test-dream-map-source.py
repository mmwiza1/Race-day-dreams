#!/usr/bin/env python3
"""Independent format/fault fixtures for development-only geographic preparation."""
import math
from pathlib import Path
import runpy
import unittest

root = Path(__file__).resolve().parent
decoder = runpy.run_path(str(root/'dream-map-source.py'))
projection = runpy.run_path(str(root/'dream-projection.py'))


class SourceFormatTest(unittest.TestCase):
    def test_geometry_relative_positions_and_closed_ring(self):
        # Move(2,3), Line(5,3), Line(5,1), Close. MVT ZigZag deltas.
        self.assertEqual(decoder['geometry'](bytes([9,4,6,18,6,0,0,3,15])),
                         [[(2,3),(5,3),(5,1),(2,3)]])

    def test_geometry_disjoint_paths_are_not_bridged(self):
        self.assertEqual(decoder['geometry'](bytes([9,2,2,10,2,0,9,8,8,10,2,0])),
                         [[(1,1),(2,1)],[(6,5),(7,5)]])

    def test_wire_truncation_rejected(self):
        for malformed in (b'\x0a\x05ab', b'\x09ab', b'\x0dxy', b'\x80'):
            with self.assertRaises(ValueError):
                list(decoder['fields'](malformed))

    def test_integer_overflow_rejected(self):
        with self.assertRaises(ValueError):
            decoder['varint'](b'\x80'*11, 0)

    def test_unsupported_geometry_command_rejected(self):
        with self.assertRaises(ValueError):
            decoder['geometry'](b'\x0b')

    def test_equator_central_meridian_reference(self):
        x,y = projection['utm18'](0,-75)
        self.assertEqual(x,500000)
        self.assertEqual(y,0)

    def test_40_north_central_meridian_reference(self):
        x,y = projection['utm18'](40,-75)
        self.assertEqual(x,500000)
        self.assertAlmostEqual(y,4427757.218,places=2)

    def test_nyc_round_trip_preserves_metre_scale_context(self):
        for lat,lon in ((40.6,-74.05),(40.78,-73.97),(40.85,-73.92)):
            x,y = projection['utm18'](lat,lon)
            restored = projection['inverse18'](x,y)
            self.assertAlmostEqual(lat,restored[0],places=7)
            self.assertAlmostEqual(lon,restored[1],places=7)

    def test_web_mercator_tile_reference(self):
        self.assertEqual(decoder['lonlat'](0,0,0,4096,(2048,2048)),[0,0])

    def test_fitted_viewport_inverse_matches_each_frozen_course_frame(self):
        import json
        sources=('london-historical.json','berlin-2014-candidate.json','tokyo-2026.json',
                 'paris-2024-olympic.json','paris-2026.json','new-york-2026.json')
        for source in sources:
            route=json.loads((root.parent/'docs/dream-route-data'/source).read_text())
            project=projection['geographic_frame'](route)
            for x,y in ((-.5,-.5),(5,5),(10.5,10.5)):
                lat,lon=projection['inverse_course_frame'](route,x,y)
                restored=project(lat,lon)
                self.assertAlmostEqual(x,restored[0],places=5)
                self.assertAlmostEqual(y,restored[1],places=5)


    def test_annual_paris_geometry_is_only_adjacent_retained_osm_nodes(self):
        import json, hashlib
        data_root=root.parent/'docs/dream-route-data'
        route=json.loads((data_root/'paris-2026.json').read_text())
        raw=(data_root/route['evidenceFile']).read_bytes()
        self.assertEqual(hashlib.sha256(raw).hexdigest(),route['evidenceSha256'])
        proof=json.loads(raw)
        self.assertEqual(route['edition'],2026)
        self.assertIn('annual race',route['event'])
        self.assertEqual(route['licence'],proof['licence'])
        self.assertEqual(route['sourceSha256'],proof['extractSha256'])
        self.assertIn('geofabrik.de',proof['extractUrl'])
        nodes={}; edges=set()
        for way in proof['ways']:
            self.assertGreater(way['version'],0)
            for ident,lat,lon in way['nodes']: nodes[ident]=[lat,lon]
            for a,b in zip(way['nodes'],way['nodes'][1:]): edges.add(tuple(sorted((a[0],b[0]))))
        ids=proof['orderedNodeIds']
        self.assertEqual(route['geometry'],[nodes[n] for n in ids])
        self.assertEqual(len(ids),route['sourcePointCount'])
        for a,b in zip(ids,ids[1:]): self.assertIn(tuple(sorted((a,b))),edges)
        # Exact statutory street names distinguish this from the Olympic Versailles course.
        names={w['tags'].get('name','') for w in proof['ways']}
        for expected in ('Route de la Pyramide','Route du Pesage','Avenue de Gravelle',
                         'Rue du Faubourg Saint-Antoine','Tunnel des Tuileries','Avenue Hubert Germain'):
            self.assertIn(expected,names)
        self.assertTrue(all(lon>2.23 for lat,lon in route['geometry']))
        self.assertTrue(42000<route['measuredMetres']<44000)
        for url in route['factualSources']: self.assertTrue(url['url'].startswith('https://'))
        self.assertEqual(len(route['factualSources'][0]['sha256']),64)
        self.assertIn('not the certified racing line',route['simplification'])


if __name__ == '__main__':
    unittest.main()
