#!/usr/bin/env python3
"""Keep source provenance complete as shared production source sets are introduced."""

import contextlib
import hashlib
import io
from pathlib import Path
import runpy
import unittest

ROOT = Path(__file__).resolve().parent.parent


class ScreenshotSourceCoverageTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        output = io.StringIO()
        with contextlib.redirect_stdout(output):
            cls.source = runpy.run_path(str(ROOT / "scripts/screenshot-source.py"))
        cls.paths = set(cls.source["paths"])
        cls.fingerprint = output.getvalue().strip()

    def test_all_actual_shared_production_source_sets_are_covered(self):
        for module in ROOT.glob("shared*"):
            if not module.is_dir():
                continue
            self.assertIn(module / "build.gradle", self.paths)
            for source_set in (module / "src").iterdir():
                if source_set.name.endswith("Main") or source_set.name == "nativeInterop":
                    for path in source_set.rglob("*"):
                        if path.is_file():
                            self.assertIn(path, self.paths, str(path.relative_to(ROOT)))

    def test_test_fixtures_and_documentation_are_not_production_inputs(self):
        self.assertNotIn(ROOT / "CONTINUE_CODEX.md", self.paths)
        self.assertIn(ROOT / "scripts/screenshot-source.py", self.paths)
        self.assertIn(ROOT / "app/src/androidTest/java/org/racedaydreams/coach/CoachingScreenshotTest.kt", self.paths)
        for path in self.paths:
            self.assertTrue(path.is_file())
            self.assertFalse(any(part in ("commonTest", "jvmTest", "iosTest") for part in path.parts))

    def test_fingerprint_binds_names_and_exact_source_bytes_without_duplicate_inputs(self):
        self.assertEqual(len(self.paths), len(self.source["paths"]))
        digest = hashlib.sha256()
        for path in sorted(self.paths):
            digest.update(str(path.relative_to(ROOT)).encode() + b"\0" + path.read_bytes() + b"\0")
        self.assertEqual(digest.hexdigest(), self.fingerprint)


if __name__ == "__main__":
    unittest.main(verbosity=2)
