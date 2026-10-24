#!/usr/bin/env python3
"""Offline Dream artwork coverage/provenance and bounded WebP-header fault tests."""
import copy
import importlib.util
import json
from pathlib import Path
import struct
import tempfile
import unittest

SPEC = importlib.util.spec_from_file_location("dream_artwork", Path(__file__).with_name("verify-dream-artwork.py"))
artwork = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(artwork)
ROOT = Path(__file__).resolve().parent.parent


def chunk(kind, payload):
    return kind + struct.pack("<I", len(payload)) + payload + b"\0" * (len(payload) % 2)


def container(*chunks):
    payload = b"WEBP" + b"".join(chunks)
    return b"RIFF" + struct.pack("<I", len(payload)) + payload


def keyframe(width=400, height=400):
    # Header-only fixture tests the structural reader, not a complete WebP codec.
    return chunk(b"VP8 ", b"\0\0\0\x9d\x01\x2a" + struct.pack("<HH", width, height))


class WebpHeaderTest(unittest.TestCase):
    def test_vp8_dimensions(self):
        self.assertEqual((1200, 400), artwork.webp_dimensions(container(keyframe(1200, 400))))

    def test_lossless_dimensions(self):
        packed = (400 - 1) | ((400 - 1) << 14)
        data = container(chunk(b"VP8L", b"\x2f" + struct.pack("<I", packed)))
        self.assertEqual((400, 400), artwork.webp_dimensions(data))

    def test_extended_canvas_dimensions(self):
        extended = b"\0\0\0\0" + (399).to_bytes(3, "little") * 2
        self.assertEqual((400, 400), artwork.webp_dimensions(container(chunk(b"VP8X", extended), keyframe())))

    def test_rejects_bad_container_and_truncation(self):
        valid = container(keyframe())
        for data in (b"", valid[:11], valid[:-1], valid + b"\0", b"NOPE" + valid[4:],
                     valid[:8] + b"JPEG" + valid[12:], container(b"VP8 "),
                     container(b"VP8 " + struct.pack("<I", 100) + b"\0")):
            with self.subTest(data=data[:16]), self.assertRaises(ValueError):
                artwork.webp_dimensions(data)

    def test_rejects_animation(self):
        extended = b"\x02\0\0\0" + (399).to_bytes(3, "little") * 2
        for data in (container(chunk(b"VP8X", extended), keyframe()),
                     container(chunk(b"ANIM", b"\0" * 6), keyframe()),
                     container(chunk(b"ANMF", b"\0" * 16), keyframe())):
            with self.subTest(data=data[:24]), self.assertRaises(ValueError):
                artwork.webp_dimensions(data)

    def test_rejects_missing_duplicate_invalid_or_unbounded_frames(self):
        mismatch = b"\0\0\0\0" + (500).to_bytes(3, "little") * 2
        for data in (container(chunk(b"ALPH", b"\0")), container(keyframe(), keyframe()),
                     container(keyframe(0, 400)), container(keyframe(1201, 400)),
                     container(chunk(b"VP8 ", b"\0" * 10)),
                     container(chunk(b"VP8L", b"\x2f\xff\xff\xff\xff")),
                     container(chunk(b"VP8X", mismatch), keyframe()),
                     container(chunk(b"VP8X", b"\0"), keyframe()),
                     container(keyframe(), chunk(b"JUNK", b"\0" * artwork.MAX_ASSET_BYTES))):
            with self.subTest(length=len(data)), self.assertRaises(ValueError):
                artwork.webp_dimensions(data)

    def test_odd_chunks_require_padding(self):
        data = container(chunk(b"JUNK", b"a"), keyframe())
        self.assertEqual((400, 400), artwork.webp_dimensions(data))
        invalid = container(b"JUNK" + struct.pack("<I", 1) + b"a")
        with self.assertRaises(ValueError):
            artwork.webp_dimensions(invalid)


class ManifestTest(unittest.TestCase):
    def setUp(self):
        self.manifest = json.loads((ROOT / artwork.MANIFEST_PATH).read_text(encoding="utf-8"))

    def test_all_five_cities_have_one_hero_and_three_reviewed_landmarks(self):
        count, total = artwork.validate_manifest(ROOT, self.manifest)
        self.assertEqual(20, count)
        self.assertEqual(self.manifest["packagedBytes"], total)
        self.assertLessEqual(total, artwork.MAX_TOTAL_BYTES)
        for city in artwork.LANDMARKS:
            entries = [entry for entry in self.manifest["assets"] if entry["city"] == city]
            self.assertEqual(["hero", "landmark", "landmark", "landmark"], [entry["kind"] for entry in entries])

    def test_every_asset_has_valid_bounded_encoded_dimensions(self):
        for entry in self.manifest["assets"]:
            with self.subTest(file=entry["file"]):
                self.assertEqual((entry["width"], entry["height"]),
                                 artwork.webp_dimensions((ROOT / entry["file"]).read_bytes()))

    def test_rejects_unsupported_or_incomplete_manifest(self):
        for field, value in (("version", 2), ("release", "future"), ("assetCount", 19),
                             ("packagedBytes", self.manifest["packagedBytes"] + 1),
                             ("ownership", ""), ("purpose", "")):
            bad = copy.deepcopy(self.manifest)
            bad[field] = value
            with self.subTest(field=field), self.assertRaises(ValueError):
                artwork.validate_manifest(ROOT, bad)
        bad = copy.deepcopy(self.manifest)
        bad["assets"].pop()
        with self.assertRaises(ValueError):
            artwork.validate_manifest(ROOT, bad)

    def test_rejects_unsafe_duplicate_and_unreviewed_entries(self):
        for field, value in (("file", "../london-hero.webp"), ("city", "missing-city"),
                             ("file", "app/src/main/assets/dream-art/placeholder.webp"),
                             ("label", "Wrong landmark"), ("width", 2400), ("height", 800),
                             ("bytes", 1), ("sha256", "0" * 64), ("licence", "unknown"),
                             ("creation", ""), ("accessibility", ""), ("labelSource", ""),
                             ("sourcePlate", "unreviewed.png"), ("sourcePlateSha256", "missing"),
                             ("sourceCrop", [0, 0, 1536, 1024])):
            bad = copy.deepcopy(self.manifest)
            bad["assets"][0][field] = value
            with self.subTest(field=field, value=value), self.assertRaises(ValueError):
                artwork.validate_manifest(ROOT, bad)
        bad = copy.deepcopy(self.manifest)
        bad["assets"][1] = bad["assets"][0]
        with self.assertRaises(ValueError):
            artwork.validate_manifest(ROOT, bad)

    def test_rejects_missing_packaged_files(self):
        with tempfile.TemporaryDirectory() as directory, self.assertRaises(ValueError):
            artwork.validate_manifest(directory, self.manifest)

    def test_source_plates_consistent_within_each_city(self):
        bad = copy.deepcopy(self.manifest)
        bad["assets"][1]["sourcePlateSha256"] = "a" * 64
        with self.assertRaises(ValueError):
            artwork.validate_manifest(ROOT, bad)

    def test_hero_is_explicitly_decorative_not_geographic_evidence(self):
        bad = copy.deepcopy(self.manifest)
        bad["assets"][0]["accessibility"] = "London skyline"
        with self.assertRaises(ValueError):
            artwork.validate_manifest(ROOT, bad)


if __name__ == "__main__":
    unittest.main(verbosity=2)
