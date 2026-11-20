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
        self.assertIn('needs: [build, guidance-completeness, formatting, apple]', publisher)
        self.assertIn('group: release-publication', publisher)
        self.assertIn('cancel-in-progress: false', publisher)
        self.assertIn('live', publisher)
        self.assertIn('development-assets.py --prepare', publisher)
        self.assertIn('development-assets.py --verify', publisher)
        self.assertIn('gh release download development', publisher)
        self.assertNotIn('--method DELETE', publisher)
        for flag in ('--prepare-ios', '--verify-ios', 'experimental-ios-device-unsigned',
                     'ios-simulator-universal-unsigned', 'latest-build-info'):
            self.assertIn(flag, publisher)
        apple = workflow.split('\n  apple:', 1)[1].split('\n  publish-test-release:', 1)[0]
        self.assertIn('uses: ./.github/workflows/multiplatform.yml', apple)
        self.assertIn('Permanent tags/releases are never overwritten', publisher)


    def ios_source(self, kind, commit='a' * 40, version='0.13.0', build='20', payload=b'ios-zip\x00' * 64):
        directory = self.root / ('ci-' + kind)
        directory.mkdir(exist_ok=True)
        name = {'ios-simulator': 'RaceDayDreams-simulator-universal.zip',
                'iphoneos-unsigned': 'RaceDayDreams-iphoneos-unsigned.zip'}[kind]
        (directory / name).write_bytes(payload)
        (directory / (name + '.sha256')).write_text(f'{assets.digest(directory / name)}  {name}\n')
        (directory / 'source-commit.txt').write_text(commit + '\n')
        (directory / 'bundle-version.txt').write_text(version + '\n')
        (directory / 'bundle-build.txt').write_text(build + '\n')
        return directory

    def test_ios_pairs_are_versioned_aliased_and_byte_identical(self):
        for kind in assets.IOS_KINDS:
            with self.subTest(kind=kind):
                expected = assets.prepare_ios(self.ios_source(kind), '0.13.0-test1', 20, 'a' * 40, kind, self.output)
                versioned, _, alias, _ = assets.ios_names('0.13.0-test1', kind)
                self.assertEqual(versioned, f'Race-Day-Dreams-0.13.0-test1-{kind}.zip')
                self.assertEqual(alias, f'Race-Day-Dreams-latest-{kind}.zip')
                self.assertEqual(assets.verify_ios(self.output, '0.13.0-test1', kind, expected)[2], alias)

    def test_ios_source_rejects_wrong_commit_version_build_or_checksum(self):
        cases = {
            'commit': dict(commit='b' * 40),
            'version': dict(version='0.12.3'),
            'build': dict(build='19'),
        }
        for label, override in cases.items():
            with self.subTest(label=label), self.assertRaises(ValueError):
                assets.prepare_ios(self.ios_source('iphoneos-unsigned', **override), '0.13.0-test1', 20,
                                   'a' * 40, 'iphoneos-unsigned', self.output)
        source = self.ios_source('ios-simulator')
        (source / 'RaceDayDreams-simulator-universal.zip').write_bytes(b'tampered')
        with self.assertRaises(ValueError):
            assets.prepare_ios(source, '0.13.0-test1', 20, 'a' * 40, 'ios-simulator', self.output)

    def test_corrupt_published_ios_alias_is_rejected(self):
        expected = assets.prepare_ios(self.ios_source('ios-simulator'), '0.13.0-test1', 20, 'a' * 40,
                                      'ios-simulator', self.output)
        (self.output / 'Race-Day-Dreams-latest-ios-simulator.zip').write_bytes(b'older build')
        with self.assertRaises(ValueError):
            assets.verify_ios(self.output, '0.13.0-test1', 'ios-simulator', expected)

    def test_build_info_ties_every_binary_to_one_commit_and_labels_unsigned(self):
        text = assets.build_info('0.13.0-test1', 20, 'c' * 40,
                                 'https://github.com/mmwiza1/Race-day-dreams/actions/runs/7',
                                 {'android-apk': '1' * 64, 'ios-simulator': '2' * 64, 'iphoneos-unsigned': '3' * 64})
        self.assertIn('Source commit: ' + 'c' * 40, text)
        self.assertIn('iOS 0.13.0 / build 20', text)
        self.assertEqual(text.count('UNSIGNED'), 2)
        with self.assertRaises(ValueError):
            assets.build_info('0.13.0-test1', 20, 'c' * 40, 'https://example.com/run',
                              {'android-apk': '1' * 64, 'ios-simulator': '2' * 64, 'iphoneos-unsigned': '3' * 64})


SIGNED_OUTPUT = """Verifies
Verified using v1 scheme (JAR signing): false
Verified using v2 scheme (APK Signature Scheme v2): false
Verified using v3 scheme (APK Signature Scheme v3): true
Number of signers: 1
Signer #1 certificate DN: CN=Example
Signer #1 certificate SHA-256 digest: {cert}
"""


class AndroidSigningAndUpdateManifestTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.cert = '8f' * 32

    def zip(self, name, entries):
        import zipfile
        path = self.root / name
        with zipfile.ZipFile(path, 'w') as archive:
            for entry, data in entries.items():
                archive.writestr(entry, data)
        return path

    def test_only_one_verified_v2_or_v3_signer_is_accepted(self):
        self.assertEqual(self.cert, assets.certificate_sha256(SIGNED_OUTPUT.format(cert=self.cert)))
        v2 = SIGNED_OUTPUT.format(cert=self.cert).replace('v2): false', 'v2): true').replace('v3): true', 'v3): false')
        self.assertEqual(self.cert, assets.certificate_sha256(v2))
        for bad in (
            SIGNED_OUTPUT.format(cert=self.cert).replace('Verifies\n', 'DOES NOT VERIFY\n'),
            SIGNED_OUTPUT.format(cert=self.cert).replace('v3): true', 'v3): false'),
            SIGNED_OUTPUT.format(cert=self.cert) + 'Signer #2 certificate SHA-256 digest: ' + 'a' * 64 + '\n',
            SIGNED_OUTPUT.format(cert='not-a-digest'),
        ):
            with self.assertRaises(ValueError):
                assets.certificate_sha256(bad)

    def test_resigning_may_change_only_signature_entries(self):
        files = {'AndroidManifest.xml': b'm', 'classes.dex': b'd', 'res/raw/a.bin': b'a'}
        verified = self.zip('verified.apk', {**files, 'META-INF/CERT.RSA': b'debug', 'META-INF/CERT.SF': b's'})
        resigned = self.zip('signed.apk', {**files, 'META-INF/RELEASE.RSA': b'release', 'META-INF/MANIFEST.MF': b'x'})
        self.assertTrue(assets.same_content(verified, resigned))
        tampered = self.zip('tampered.apk', {**files, 'classes.dex': b'evil'})
        self.assertFalse(assets.same_content(verified, tampered))
        extra = self.zip('extra.apk', {**files, 'META-INF/services/x': b'new'})
        self.assertFalse(assets.same_content(verified, extra))

    def test_committed_fingerprint_must_be_one_digest(self):
        pin = self.root / 'pin'
        self.assertIsNone(assets.read_pin(pin))
        pin.write_text(self.cert.upper() + '\n')
        self.assertEqual(self.cert, assets.read_pin(pin))
        for bad in ('', 'abc', self.cert + self.cert, self.cert + ' x'):
            pin.write_text(bad)
            with self.assertRaises(ValueError):
                assets.read_pin(pin)

    def test_manifest_matches_the_app_codec_fields_and_describes_the_versioned_apk(self):
        import json
        apk = self.root / 'Race-Day-Dreams-0.14.0-test1.apk'
        apk.write_bytes(b'apk' * 1000)
        text = assets.update_manifest(apk, '0.14.0-test1', 22, 'c' * 40, self.cert)
        data = json.loads(text)
        codec = (ROOT / 'sharedStorage/src/commonMain/kotlin/org/racedaydreams/storage/AppUpdateManifestJson.kt').read_text()
        fields = codec[codec.index('private val FIELDS'):codec.index(')', codec.index('setOf('))]
        import re
        self.assertEqual(set(re.findall(r'"([A-Za-z0-9]+)"', fields)), set(data))
        self.assertEqual(assets.RELEASE_DOWNLOAD_PREFIX + apk.name, data['apkUrl'])
        self.assertEqual(assets.digest(apk), data['apkSha256'])
        self.assertEqual(1, data['schema'])
        self.assertTrue(data['notes'].startswith('• '))
        self.assertLessEqual(len(data['notes']), assets.MAX_NOTES)
        self.assertNotIn('`', data['notes'])
        self.assertEqual(data, assets.verify_update_manifest(text, apk, self.cert))
        with self.assertRaises(ValueError):
            assets.verify_update_manifest(text, apk, 'a' * 64)
        apk.write_bytes(b'different')
        with self.assertRaises(ValueError):
            assets.verify_update_manifest(text, apk, self.cert)
        alias = self.root / assets.ALIAS
        alias.write_bytes(b'x')
        with self.assertRaises(ValueError):
            assets.update_manifest(alias, '0.14.0-test1', 22, 'c' * 40, self.cert)

    def test_identity_check_reports_only_the_public_fingerprint_for_unchanged_contents(self):
        import subprocess, sys
        files = {'AndroidManifest.xml': b'm', 'classes.dex': b'd'}
        verified = self.zip('verified.apk', {**files, 'META-INF/CERT.RSA': b'debug'})
        scratch = self.zip('scratch.apk', {**files, 'META-INF/RELEASE.RSA': b'release'})
        output = self.root / 'verify.txt'
        output.write_text(SIGNED_OUTPUT.format(cert=self.cert))
        script = str(ROOT / 'scripts/development-assets.py')
        def run(apk):
            return subprocess.run([sys.executable, '-I', script, '--version', '0.0.0-test0', '--directory', str(self.root),
                                   '--identity-check', str(apk), '--verified-apk', str(verified),
                                   '--apksigner-output', str(output)], capture_output=True, text=True)
        ok = run(scratch)
        self.assertEqual(0, ok.returncode, ok.stderr)
        self.assertEqual(self.cert, ok.stdout.strip())
        tampered = self.zip('tampered.apk', {**files, 'classes.dex': b'evil'})
        self.assertNotEqual(0, run(tampered).returncode)

    def test_a_lower_version_code_is_never_announced(self):
        live = '{"versionCode": 23}'
        assets.check_not_downgrade(None, 1)
        assets.check_not_downgrade(live, 23)
        assets.check_not_downgrade(live, 24)
        with self.assertRaises(ValueError):
            assets.check_not_downgrade(live, 22)

    def test_build_info_names_the_release_certificate_when_signed(self):
        digests = {'android-apk': '1' * 64, 'ios-simulator': '2' * 64, 'iphoneos-unsigned': '3' * 64}
        run = 'https://github.com/mmwiza1/Race-day-dreams/actions/runs/7'
        self.assertIn('no in-place updates', assets.build_info('0.14.0-test1', 22, 'c' * 40, run, digests))
        signed = assets.build_info('0.14.0-test1', 22, 'c' * 40, run, digests, self.cert)
        self.assertIn('release key, certificate SHA-256 ' + self.cert, signed)
        with self.assertRaises(ValueError):
            assets.build_info('0.14.0-test1', 22, 'c' * 40, run, digests, 'debug')


if __name__ == '__main__':
    unittest.main(verbosity=2)
