#!/usr/bin/env python3
"""Offline GitHub showcase contract. Public HTTP/download bytes are verified separately."""
from pathlib import Path
import re
import unittest
from urllib.parse import urlsplit, unquote

ROOT = Path(__file__).resolve().parents[1]
DOWNLOAD = 'https://github.com/mmwiza1/Race-day-dreams/releases/download/development/Race-Day-Dreams-latest-test.apk'
GROUPS = ('Running Coach', 'Strength and Gym', 'Progress and Performance', 'Health and Recovery', 'Privacy and Data Ownership')


def validate(text, root):
    images = re.findall(r'<img\s+[^>]*src="([^"]+)"[^>]*alt="([^"]+)"[^>]*>', text)
    if len(images) < 12:
        raise ValueError('Feature showcase requires real screenshots')
    for path, label in images:
        if not path.startswith('docs/screenshots/') or not (root / path).is_file() or len(label) < 15:
            raise ValueError('Missing screenshot or descriptive alternative')
        if f'href="{path}"' not in text:
            raise ValueError('Screenshot must link to full-resolution original')
    for line in text.splitlines():
        if line.count('<img ') > 2:
            raise ValueError('At most two screenshots per row')
    for index, group in enumerate(GROUPS):
        section = text.split('### ' + group + '\n', 1)[1].split('\n### ', 1)[0].split('\n## ', 1)[0]
        if '<img ' not in section or DOWNLOAD not in section:
            raise ValueError('Each feature needs screenshots and an adjacent direct download')
    if DOWNLOAD not in text[:2200] or DOWNLOAD + '.sha256' not in text[:2200]:
        raise ValueError('Primary APK/checksum links must be prominent')
    if re.search(r'releases/download/development/Race-Day-Dreams-\d', text):
        raise ValueError('README must not hard-code a rolling version')
    for target in re.findall(r'(?<!!)\[[^\]]+\]\(([^)]+)\)', text):
        uri = urlsplit(target)
        if uri.scheme or target.startswith('#'):
            continue
        if not (root / unquote(uri.path)).exists():
            raise ValueError('Broken local documentation link: ' + target)
    if 'synthetic demonstration records' not in text or 'docs/screenshots)' not in text:
        raise ValueError('Fixture disclosure and full gallery link are required')


class ShowcaseTest(unittest.TestCase):
    def setUp(self):
        self.text = (ROOT / 'README.md').read_text()

    def test_readme_has_real_paths_mobile_rows_and_adjacent_downloads(self):
        validate(self.text, ROOT)

    def test_missing_image_is_rejected(self):
        with self.assertRaises(ValueError):
            validate(self.text.replace('home-material-brand-light.png', 'missing-image.png'), ROOT)

    def test_versioned_rolling_link_is_rejected(self):
        with self.assertRaises(ValueError):
            validate(self.text.replace(DOWNLOAD, DOWNLOAD.replace('latest-test', '0.12.3-test1')), ROOT)

    def test_missing_group_download_is_rejected(self):
        begin = self.text.index('### Privacy and Data Ownership')
        end = self.text.index('## Installation', begin)
        bad = self.text[:begin] + self.text[begin:end].replace(DOWNLOAD, 'https://example.org') + self.text[end:]
        with self.assertRaises(ValueError):
            validate(bad, ROOT)

    def test_minimum_android_matches_app_and_no_unsupported_web_features(self):
        self.assertRegex((ROOT / 'app/build.gradle').read_text(), r'minSdk\s+28')
        self.assertIn('Android 9 (API 28)', self.text)
        self.assertIn('docs/README_0_12_2_ARCHIVE.md', self.text)
        self.assertNotRegex(self.text.lower(), r'<script|<style|style=|iframe')


if __name__ == '__main__':
    unittest.main(verbosity=2)
