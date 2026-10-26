#!/usr/bin/env python3
"""Offline release-note validation; never calls GitHub or publishes."""
import importlib.util
from pathlib import Path
import tempfile
import unittest

spec = importlib.util.spec_from_file_location(
    'release_notes', Path(__file__).with_name('release-notes.py')
)
notes = importlib.util.module_from_spec(spec)
spec.loader.exec_module(notes)


class ReleaseNotesTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        (self.root / 'app').mkdir()
        (self.root / 'docs').mkdir()
        (self.root / 'app/build.gradle').write_text(
            "versionName '0.8.1-test1'\nversionCode 10\n"
        )
        (self.root / 'docs/RELEASE_0_8_1.md').write_text(
            "# Race Day Dreams 0.8.1-test1\n\n"
            "A concise user-facing summary of this build.\n\n"
            "## What's new\nReadable features\n\n"
            "## Improvements\nRefinements\n\n## Fixes\nResolved bugs\n\n"
            "## Known limitations\nPhysical checks pending\n"
        )
        self.sha = 'a' * 40
        self.url = 'https://github.com/mmwiza1/Race-day-dreams/actions/runs/123'

    def test_permanent_release_retains_sections_and_honest_signing(self):
        result = notes.render(self.root, self.sha, self.url, tag='v0.8.1-test1')
        for text in (
            "What's new", 'Improvements', 'Fixes', 'Known limitations',
            'Download', 'Installation and first launch', 'Build information',
            'debug-signed', 'Galaxy S24', 'versionCode 10', self.sha,
        ):
            self.assertIn(text, result)
        self.assertNotIn('Latest Development Build', result)

    def test_rolling_release_is_clearly_development(self):
        self.assertIn(
            'Latest Development Build',
            notes.render(self.root, self.sha, self.url, development=True),
        )

    def test_public_sections_are_in_required_order(self):
        result = notes.render(self.root, self.sha, self.url, development=True)
        ordered = (
            '# Race Day Dreams 0.8.1-test1',
            '## Download',
            '## Installation and first launch',
            "## What's new",
            '## Improvements',
            '## Fixes',
            '## Known limitations',
            '## Build information',
        )
        positions = [result.index(section) for section in ordered]
        self.assertEqual(positions, sorted(positions))

    def test_tag_mismatch_is_rejected(self):
        for tag in ('v0.8.0-test1', 'main', 'development', 'v0.8.1', 'v0.8.1-test1;false'):
            with self.subTest(tag=tag), self.assertRaises(ValueError):
                notes.render(self.root, self.sha, self.url, tag=tag)

    def test_missing_human_sections_is_rejected(self):
        (self.root / 'docs/RELEASE_0_8_1.md').write_text(
            '# Race Day Dreams 0.8.1-test1\n\nraw commit messages'
        )
        with self.assertRaises(ValueError):
            notes.render(self.root, self.sha, self.url, tag='v0.8.1-test1')

    def test_wrong_release_title_is_rejected(self):
        path = self.root / 'docs/RELEASE_0_8_1.md'
        path.write_text(path.read_text().replace('0.8.1-test1', '0.8.0-test1', 1))
        with self.assertRaises(ValueError):
            notes.render(self.root, self.sha, self.url, tag='v0.8.1-test1')

    def test_generated_sections_cannot_be_duplicated_in_maintained_notes(self):
        path = self.root / 'docs/RELEASE_0_8_1.md'
        path.write_text(path.read_text() + '\n## Download\nDuplicate\n')
        with self.assertRaises(ValueError):
            notes.render(self.root, self.sha, self.url, tag='v0.8.1-test1')

    def test_next_patch_uses_its_own_maintained_notes_and_download_name(self):
        (self.root / 'app/build.gradle').write_text(
            "versionName '0.8.2-test1'\nversionCode 11\n"
        )
        current = (
            (self.root / 'docs/RELEASE_0_8_1.md').read_text()
            .replace('0.8.1-test1', '0.8.2-test1', 1)
            .replace('Readable features', 'Premium Dream Routes patch')
        )
        (self.root / 'docs/RELEASE_0_8_2.md').write_text(current)
        result = notes.render(self.root, self.sha, self.url, tag='v0.8.2-test1')
        for text in ('Premium Dream Routes patch', 'Race-Day-Dreams-0.8.2-test1.apk', 'versionCode 11'):
            self.assertIn(text, result)
        self.assertNotIn('Readable features', result)
        self.assertNotIn('Race-Day-Dreams-0.8.1-test1.apk', result)

    def test_incomplete_identity_or_untrusted_url_is_rejected(self):
        for sha, url in (
            ('short', self.url),
            (self.sha, 'https://example.org'),
            ('a' * 40 + '\n', self.url),
        ):
            with self.subTest(sha=sha, url=url), self.assertRaises(ValueError):
                notes.render(self.root, sha, url, tag='v0.8.1-test1')


if __name__ == '__main__':
    unittest.main(verbosity=2)
