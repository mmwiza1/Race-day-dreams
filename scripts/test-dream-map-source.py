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


if __name__ == '__main__':
    unittest.main()
