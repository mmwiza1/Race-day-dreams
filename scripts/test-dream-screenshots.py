#!/usr/bin/env python3
"""Fault tests for bounded capture validation; fixtures are not app screenshot evidence."""
import importlib.util
from pathlib import Path
import struct
import tempfile
import unittest
import zlib

SPEC = importlib.util.spec_from_file_location("dream_screenshots", Path(__file__).with_name("verify-dream-screenshots.py"))
captures = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(captures)


def chunk(kind, payload):
    return struct.pack(">I", len(payload)) + kind + payload + struct.pack(">I", zlib.crc32(kind + payload) & 0xffffffff)


def png(width=320, height=640, pixels=None):
    header = chunk(b"IHDR", struct.pack(">IIBBBBB", width, height, 8, 6, 0, 0, 0))
    raw = pixels if pixels is not None else (b"\0" + b"\0\x44\x88\xff" * width) * height
    return captures.PNG_SIGNATURE + header + chunk(b"IDAT", zlib.compress(raw)) + chunk(b"IEND", b"")


class CaptureValidationTest(unittest.TestCase):
    def test_catalogue_expansion_has_every_display_mode(self):
        import re
        guide_source = (Path(__file__).resolve().parent.parent / 'domain/src/main/java/org/racedaydreams/domain/strength/ExpandedExerciseGuides.java').read_text()
        identities = set(re.findall(r'add\(\s*out,\s*"([^"]+)"', guide_source))
        self.assertEqual(identities, set(captures.CATALOGUE_EXPANSION_IDS))
        expected = {f'exercise-{identity}-howto{suffix}.png' for identity in identities for suffix in ('', '-dark', '-large-font')}
        self.assertEqual(expected, set(captures.CATALOGUE_EXPANSION_REQUIRED))
        self.assertTrue(expected.issubset(captures.REQUIRED))

    def test_progression_and_history_require_three_actual_modes(self):
        self.assertEqual(len(captures.PROGRAMME_REVIEW_REQUIRED), 36)
        for scene in ('progression', 'history', 'progression-details', 'progression-instructions', 'progression-weight', 'progression-controls', 'progression-context', 'progression-evidence', 'progression-unchanged', 'progression-targets', 'progression-safeguards', 'progression-evidence-actions'):
            for suffix in ('', '-dark', '-large-font'):
                self.assertIn(f'strength-programme-{scene}{suffix}.png', captures.REQUIRED)

    def test_storage_requires_real_policy_preview_and_confirmation_in_three_modes(self):
        expected={f'{scene}{suffix}.png' for scene in ('storage-management','storage-retention','storage-preview','storage-confirmation','storage-confirmation-details') for suffix in ('','-dark','-large-font')}
        self.assertEqual(expected,set(captures.STORAGE_REQUIRED))
        self.assertTrue(expected.issubset(captures.REQUIRED))

    def test_required_five_cities_and_two_accessibility_modes_have_both_panels(self):
        self.assertEqual(14, len(captures.DREAM_REQUIRED))
        self.assertEqual(77, len(captures.OTHER_REQUIRED) + len(captures.DREAM_REQUIRED))
        self.assertEqual(296, len(captures.REQUIRED))
        self.assertEqual(len(captures.REQUIRED), len(set(captures.REQUIRED)))
        self.assertEqual(63, len(captures.OTHER_REQUIRED))
        for city in ("london", "berlin", "paris", "tokyo", "new-york", "dark", "large-font"):
            self.assertIn(f"dream-route-{city}.png", captures.REQUIRED)
            self.assertIn(f"dream-route-{city}-landmarks.png", captures.REQUIRED)

    def test_strength_first_home_requires_all_three_real_capture_modes(self):
        self.assertEqual({"strength-home.png", "strength-home-dark.png", "strength-home-large-font.png"},
                         set(captures.STRENGTH_REQUIRED))
        for name in captures.STRENGTH_REQUIRED:
            self.assertIn(name, captures.REQUIRED)

    def test_strength_intelligence_requires_all_three_modes_and_evidence_panels(self):
        self.assertEqual(18, len(captures.STRENGTH_INTELLIGENCE_REQUIRED))
        for name in captures.STRENGTH_INTELLIGENCE_REQUIRED:
            self.assertIn(name, captures.REQUIRED)

    def test_gym_preparation_requires_real_light_dark_and_large_views(self):
        self.assertEqual(15, len(captures.GYM_TOOLS_REQUIRED))
        for name in captures.GYM_TOOLS_REQUIRED:
            self.assertIn(name, captures.REQUIRED)

    def test_gym_now_requires_hub_builder_actuals_completion_and_accessibility_captures(self):
        self.assertEqual({"exercise-excluded.png", "gym-association-review.png", "gym-association-saved.png", "exercise-browse.png", "gym-equipment-alternatives.png", "gym-now.png", "gym-workout-builder.png", "gym-workout.png",
                          "gym-workout-saved.png", "gym-workout-dark.png", "gym-workout-large-font.png"},
                         set(captures.GYM_REQUIRED))
        for name in captures.GYM_REQUIRED:
            self.assertIn(name, captures.REQUIRED)

    def test_programme_builder_requires_real_preview_and_accessibility_captures(self):
        self.assertEqual({
            "strength-programme-builder.png", "strength-programme-builder-dark.png",
            "strength-programme-builder-large-font.png", "strength-programme-preview.png",
            "strength-programme-preview-dark.png", "strength-programme-preview-large-font.png",
            "strength-custom-exercise.png",
        }, set(captures.PROGRAMMING_REQUIRED))
        for name in captures.PROGRAMMING_REQUIRED:
            self.assertIn(name, captures.REQUIRED)

    def test_templates_require_real_configuration_dated_previews_and_accessibility(self):
        self.assertEqual(12, len(captures.TEMPLATE_REQUIRED))
        for scene in ("strength-templates", "strength-generated-preview", "strength-generated-workout", "strength-template-settings"):
            for suffix in ("", "-dark", "-large-font"):
                self.assertIn(f"{scene}{suffix}.png", captures.REQUIRED)

    def test_future_review_requires_both_real_screens_in_every_accessibility_mode(self):
        self.assertEqual(6, len(captures.REVISION_REQUIRED))
        for scene in ("strength-future-preview", "strength-future-settings"):
            for suffix in ("", "-dark", "-large-font"):
                self.assertIn(f"{scene}{suffix}.png", captures.REQUIRED)

    def test_programme_replacement_requires_real_dialog_in_all_accessibility_modes(self):
        self.assertEqual(3, len(captures.PROGRAMME_SWAP_REQUIRED))
        for suffix in ("", "-dark", "-large-font"):
            self.assertIn(f"strength-programme-alternatives{suffix}.png", captures.REQUIRED)

    def test_complete_capture_set_validates_without_requiring_equal_api_hashes(self):
        with tempfile.TemporaryDirectory() as temp:
            directory = Path(temp)
            for index, name in enumerate(captures.REQUIRED):
                (directory / name).write_bytes(png(320 + index))
            result = captures.validate_captures(directory)
            self.assertEqual(len(captures.REQUIRED), len(result))
            self.assertEqual((320 + len(captures.REQUIRED) - 1, 640), result[captures.REQUIRED[-1]])

    def test_missing_or_symlink_capture_rejected(self):
        with tempfile.TemporaryDirectory() as temp:
            directory = Path(temp)
            for name in captures.REQUIRED:
                (directory / name).write_bytes(png())
            missing = directory / captures.REQUIRED[-1]
            missing.unlink()
            with self.assertRaisesRegex(ValueError, "missing real capture"):
                captures.validate_captures(directory)
            missing.symlink_to(directory / captures.REQUIRED[0])
            with self.assertRaisesRegex(ValueError, "missing real capture"):
                captures.validate_captures(directory)

    def test_bad_signature_truncation_checksum_and_trailing_bytes_rejected(self):
        good = png()
        bad_crc = good[:40] + bytes([good[40] ^ 1]) + good[41:]
        for data in (b"", b"not png", good[:20], good[:-1], bad_crc, good + b"extra"):
            with self.subTest(size=len(data)), self.assertRaises(ValueError):
                captures.png_dimensions(data)

    def test_impossible_dimensions_and_pixel_streams_rejected(self):
        for data in (png(319), png(height=639), png(1601), png(height=3201),
                     png(pixels=b"short"), png(pixels=b"\0" * (640 * 1281 + 1)),
                     png(pixels=(b"\5" + b"\0" * 1280) * 640)):
            with self.subTest(size=len(data)), self.assertRaises(ValueError):
                captures.png_dimensions(data)

    def test_duplicate_header_or_missing_image_data_rejected(self):
        good = png()
        header = good[8:33]
        for data in (good[:33] + header + good[33:],
                     captures.PNG_SIGNATURE + header + chunk(b"IEND", b""),
                     captures.PNG_SIGNATURE + header + chunk(b"IDAT", b"invalid-zlib") + chunk(b"IEND", b"")):
            with self.subTest(size=len(data)), self.assertRaises(ValueError):
                captures.png_dimensions(data)


if __name__ == "__main__":
    unittest.main(verbosity=2)
