#!/usr/bin/env python3
"""Offline artifact-pair and publisher-contract tests; never uploads files."""
import importlib.util
from pathlib import Path
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[1]
spec = importlib.util.spec_from_file_location('assets', Path(__file__).with_name('development-assets.py'))
assets = importlib.util.module_from_spec(spec)
spec.loader.exec_module(assets)


class DevelopmentAssetsTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.source = self.root / 'input.apk'
        self.source.write_bytes(b'unit-test-binary\x00\xff' * 32)
        self.output = self.root / 'output'
        self.version = '0.12.3-test1'

    def test_pair_has_identical_bytes_and_checksum_names(self):
        names = assets.prepare(self.source, self.version, self.output)
        self.assertEqual(4, len(names))
        self.assertEqual(self.source.read_bytes(), (self.output / assets.ALIAS).read_bytes())
        self.assertEqual(names, assets.verify(self.output, self.version, assets.digest(self.source)))

    def test_refresh_keeps_unrelated_and_previous_version_files(self):
        assets.prepare(self.source, '0.12.2-test1', self.output)
        note = self.output / 'owner-file.txt'
        note.write_text('keep')
        self.source.write_bytes(b'new-test-build')
        assets.prepare(self.source, self.version, self.output)
        self.assertEqual('keep', note.read_text())
        self.assertTrue((self.output / 'Race-Day-Dreams-0.12.2-test1.apk').exists())

    def test_corrupt_alias_or_checksum_is_rejected(self):
        for name in (assets.ALIAS, assets.ALIAS + '.sha256'):
            with self.subTest(name=name):
                assets.prepare(self.source, self.version, self.output)
                (self.output / name).write_bytes(b'wrong')
                with self.assertRaises(ValueError):
                    assets.verify(self.output, self.version, assets.digest(self.source))

    def test_missing_empty_and_unsafe_sources_are_rejected(self):
        for version in ('../bad', '0.12.3', '0.12.3-test1\n', '0.12.3-test1;false'):
            with self.assertRaises(ValueError):
                assets.prepare(self.source, version, self.output)
        self.source.write_bytes(b'')
        with self.assertRaises(ValueError):
            assets.prepare(self.source, self.version, self.output)

    def test_future_version_uses_same_alias_and_own_versioned_name(self):
        names = assets.prepare(self.source, '0.13.0-test2', self.output)
        self.assertEqual(assets.ALIAS, names[2])
        self.assertEqual('Race-Day-Dreams-0.13.0-test2.apk', names[0])

    def test_bootstrap_rejects_failing_or_unrelated_sources(self):
        import copy
        source = 'a' * 40
        body = f'## Build information\n- Version: 0.12.2-test1 / versionCode 18\n- Commit: `{source}`\nhttps://github.com/mmwiza1/Race-day-dreams/actions/runs/123'
        release = {'body': body, 'tag_name': 'development', 'draft': False, 'prerelease': True}
        run = {'head_sha': source, 'id': 123, 'status': 'completed', 'conclusion': 'success', 'event': 'push', 'head_branch': 'main'}
        tag = {'object': {'sha': source}}
        spec = importlib.util.spec_from_file_location('bootstrap', ROOT / 'scripts/bootstrap-development-alias.py')
        module = importlib.util.module_from_spec(spec)
        spec.loader.exec_module(module)
        self.assertEqual(('0.12.2-test1', source, 123), module.identity(release, run, tag, 'mmwiza1/Race-day-dreams'))
        release['body'] = 'Earlier PR evidence: https://github.com/mmwiza1/Race-day-dreams/actions/runs/999\n\n' + release['body']
        self.assertEqual(('0.12.2-test1', source, 123), module.identity(release, run, tag, 'mmwiza1/Race-day-dreams'))
        for field, value in [('conclusion','failure'), ('status','in_progress'), ('event','pull_request'), ('head_branch','feature'), ('head_sha','b'*40)]:
            bad = copy.deepcopy(run)
            bad[field] = value
            with self.subTest(field=field), self.assertRaises(ValueError):
                module.identity(release, bad, tag, 'mmwiza1/Race-day-dreams')

    def test_publisher_keeps_gate_serialisation_and_verifies_downloaded_pairs(self):
        workflow = (ROOT / '.github/workflows/android.yml').read_text()
        publisher = workflow.split('  publish-test-release:', 1)[1]
        self.assertIn('needs: [build, guidance-completeness, formatting]', publisher)
        self.assertIn('group: release-publication', publisher)
        self.assertIn('cancel-in-progress: false', publisher)
        self.assertIn('live', publisher)
        self.assertIn('development-assets.py --prepare', publisher)
        self.assertIn('development-assets.py --verify', publisher)
        self.assertIn('gh release download development', publisher)
        self.assertNotIn('--method DELETE', publisher)
        self.assertIn('Permanent tags/releases are never overwritten', publisher)


if __name__ == '__main__':
    unittest.main(verbosity=2)
