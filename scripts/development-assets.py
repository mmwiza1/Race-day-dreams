#!/usr/bin/env python3
"""Prepare/verify versioned and rolling APK and unsigned iOS pairs; no publishing or network calls."""
from pathlib import Path
import argparse
import hashlib
import re
import shutil

ALIAS = 'Race-Day-Dreams-latest-test.apk'


def names(version):
    if not re.fullmatch(r'\d+\.\d+\.\d+-test\d+', version):
        raise ValueError('A development/test version is required')
    apk = f'Race-Day-Dreams-{version}.apk'
    return (apk, apk + '.sha256', ALIAS, ALIAS + '.sha256')


def digest(path):
    value = hashlib.sha256()
    with Path(path).open('rb') as stream:
        for block in iter(lambda: stream.read(1024 * 1024), b''):
            value.update(block)
    return value.hexdigest()


def prepare(source, version, destination):
    source, destination = Path(source), Path(destination)
    files = names(version)
    if not source.is_file() or source.stat().st_size == 0 or source.is_symlink():
        raise ValueError('A non-empty verified APK file is required')
    expected = digest(source)
    destination.mkdir(parents=True, exist_ok=True)
    for name in (files[0], files[2]):
        target = destination / name
        if target.resolve() != source.resolve():
            shutil.copyfile(source, target)
        (destination / (name + '.sha256')).write_text(f'{expected}  {name}\n')
    verify(destination, version, expected)
    return files


def verify(directory, version, expected):
    if not re.fullmatch(r'[0-9a-f]{64}', expected):
        raise ValueError('Expected SHA-256 is required')
    directory = Path(directory)
    files = names(version)
    for name in (files[0], files[2]):
        apk = directory / name
        checksum = directory / (name + '.sha256')
        if apk.is_symlink() or checksum.is_symlink() or not apk.is_file() or apk.stat().st_size == 0:
            raise ValueError('Missing or unsafe APK/checksum pair')
        if digest(apk) != expected or checksum.read_text() != f'{expected}  {name}\n':
            raise ValueError('Published APK or checksum does not match the verified build')
    return files


# Unsigned iOS developer/test builds. Not signed, not App Store/TestFlight packages.
IOS_KINDS = ('ios-simulator', 'iphoneos-unsigned', 'ipa')
# CI artifact file for each kind, and the published suffix after "Race-Day-Dreams-<version|latest>-".
# The .ipa is the same unsigned iPhone app in the standard Payload/ layout, for re-signing tools.
IOS_SOURCES = {'ios-simulator': 'RaceDayDreams-simulator-universal.zip',
               'iphoneos-unsigned': 'RaceDayDreams-iphoneos-unsigned.zip',
               'ipa': 'RaceDayDreams-unsigned.ipa'}
IOS_SUFFIXES = {'ios-simulator': 'ios-simulator.zip', 'iphoneos-unsigned': 'iphoneos-unsigned.zip',
                'ipa': 'unsigned.ipa'}
BUILD_INFO_ALIAS = 'Race-Day-Dreams-latest-build-info.txt'


def ios_names(version, kind):
    names(version)  # same strict development/test version contract as Android
    if kind not in IOS_KINDS:
        raise ValueError('Unknown iOS artifact kind')
    versioned = f'Race-Day-Dreams-{version}-{IOS_SUFFIXES[kind]}'
    alias = f'Race-Day-Dreams-latest-{IOS_SUFFIXES[kind]}'
    return (versioned, versioned + '.sha256', alias, alias + '.sha256')


def bundle_version(version):
    """Info.plist CFBundleShortVersionString for a development/test versionName."""
    names(version)
    return version.split('-', 1)[0]


def check_ios_source(source, version, code, commit, kind):
    """Validate one CI iOS artifact directory: zip, checksum, commit and bundle version files."""
    source = Path(source)
    zip_name = IOS_SOURCES[kind]
    archive = source / zip_name
    if archive.is_symlink() or not archive.is_file() or archive.stat().st_size == 0:
        raise ValueError(f'Missing or empty {kind} archive')
    expected = digest(archive)
    recorded = (source / (zip_name + '.sha256')).read_text().split()
    if not recorded or recorded[0] != expected:
        raise ValueError(f'{kind} checksum does not match its archive')
    if kind == 'ipa':
        check_ipa(archive)
    if not re.fullmatch(r'[0-9a-f]{40}', commit):
        raise ValueError('Full commit SHA required')
    fields = {
        'source-commit.txt': commit,
        'bundle-version.txt': bundle_version(version),
        'bundle-build.txt': str(code),
    }
    for name, value in fields.items():
        if (source / name).read_text().strip() != value:
            raise ValueError(f'{kind} {name} does not match the verified build')
    return archive, expected


def check_ipa(archive):
    """An .ipa must hold exactly one app at Payload/RaceDayDreams.app with its executable and plist."""
    import zipfile
    with zipfile.ZipFile(archive) as ipa:
        names = set(ipa.namelist())
    if any(not n.startswith('Payload/RaceDayDreams.app/') and n != 'Payload/' for n in names):
        raise ValueError('The .ipa must contain only Payload/RaceDayDreams.app')
    for required in ('Payload/RaceDayDreams.app/RaceDayDreams', 'Payload/RaceDayDreams.app/Info.plist'):
        if required not in names:
            raise ValueError(f'The .ipa is missing {required}')


def prepare_ios(source, version, code, commit, kind, destination):
    archive, expected = check_ios_source(source, version, code, commit, kind)
    destination = Path(destination)
    destination.mkdir(parents=True, exist_ok=True)
    files = ios_names(version, kind)
    for name in (files[0], files[2]):
        shutil.copyfile(archive, destination / name)
        (destination / (name + '.sha256')).write_text(f'{expected}  {name}\n')
    verify_ios(destination, version, kind, expected)
    return expected


def verify_ios(directory, version, kind, expected):
    if not re.fullmatch(r'[0-9a-f]{64}', expected):
        raise ValueError('Expected SHA-256 is required')
    directory = Path(directory)
    files = ios_names(version, kind)
    for name in (files[0], files[2]):
        archive, checksum = directory / name, directory / (name + '.sha256')
        if archive.is_symlink() or checksum.is_symlink() or not archive.is_file() or archive.stat().st_size == 0:
            raise ValueError('Missing or unsafe iOS archive/checksum pair')
        if digest(archive) != expected or checksum.read_text() != f'{expected}  {name}\n':
            raise ValueError('Published iOS archive or checksum does not match the verified build')
    return files


# ---- Consistent Android signing and the in-app update manifest -----------------------------------
# The verified API35 test APK is re-signed for distribution with the project's release key, held only
# in GitHub Actions secrets. Its contents stay byte-identical; only the signature changes.

UPDATE_MANIFEST = 'Race-Day-Dreams-latest-update.json'
RELEASE_DOWNLOAD_PREFIX = 'https://github.com/mmwiza1/Race-day-dreams/releases/download/development/'
PACKAGE_NAME = 'org.racedaydreams'
SIGNATURE_ENTRY = re.compile(r'META-INF/([^/]+\.(SF|RSA|DSA|EC)|MANIFEST\.MF)$')
MAX_NOTES = 4000


def read_pin(path):
    """The committed public certificate fingerprint, or None until stable signing is set up."""
    path = Path(path)
    if not path.exists():
        return None
    value = path.read_text().strip().lower()
    if not re.fullmatch(r'[0-9a-f]{64}', value):
        raise ValueError(f'{path} must contain exactly one SHA-256 hex digest')
    return value


def certificate_sha256(apksigner_output):
    """Parse `apksigner verify --print-certs -v`; exactly one verified v2+ signer is accepted."""
    text = apksigner_output
    if not re.search(r'^Verifies$', text, re.M):
        raise ValueError('APK signature does not verify')
    # minSdk 28 (Android 9) apps are signed with APK Signature Scheme v3; v2 is also accepted.
    if not re.search(r'^Verified using v[23] scheme \(APK Signature Scheme v[23]\): true$', text, re.M):
        raise ValueError('APK Signature Scheme v2 or v3 is required')
    # Any signer line ("Signer #1 ...", or per-SDK-range v3 signers "Signer (minSdkVersion=..) ...")
    # must name the same single certificate; source stamps and other keys are not signers.
    digests = set(re.findall(r'^Signer\b[^\n]*? certificate SHA-256 digest: ([0-9a-f]{64})\s*$', text, re.M))
    count = re.search(r'^Number of signers: (\d+)\s*$', text, re.M)
    if len(digests) != 1 or (count and count.group(1) != '1'):
        public = '\n'.join(line for line in text.splitlines()
                           if line.startswith(('Verified', 'Number of signers', 'Signer', 'WARNING')))
        raise ValueError('Exactly one APK signer is required. apksigner reported (public data only):\n' + public)
    return digests.pop()


def same_content(verified, signed):
    """True when both APKs hold the same files, ignoring only JAR signature entries."""
    import zipfile

    def entries(path):
        with zipfile.ZipFile(path) as archive:
            return {(i.filename, i.CRC, i.file_size) for i in archive.infolist()
                    if not SIGNATURE_ENTRY.fullmatch(i.filename)}
    return entries(verified) == entries(signed)


def whats_new(version):
    """The maintained release note's "What's new" bullets as plain text for the app."""
    names(version)
    numbers = version.split('-', 1)[0].split('.')
    note = Path(__file__).resolve().parents[1] / ('docs/RELEASE_' + '_'.join(numbers) + '.md')
    text = note.read_text()
    match = re.search(r"^## What's new\n(.*?)(?=^## )", text, re.M | re.S)
    if not match:
        raise ValueError("The release note has no What's new section")
    bullets, current = [], None
    for line in match.group(1).splitlines():
        if line.startswith('- '):
            current = line[2:].strip()
            bullets.append(current)
        elif line.startswith('  ') and current is not None:
            bullets[-1] += ' ' + line.strip()
    bullets = ['• ' + re.sub(r'[`*_]', '', b) for b in bullets]
    out = ''
    for bullet in bullets:
        candidate = (out + '\n' if out else '') + bullet
        if len(candidate) > MAX_NOTES - 60:
            return out + '\n• More in the full release notes.'
        out = candidate
    return out


def update_manifest(apk, version, code, commit, certificate, min_sdk=28):
    """The manifest the app reads (AppUpdateManifestJson schema 1). Keys are sorted for stable diffs."""
    import json
    apk = Path(apk)
    names(version)
    if not re.fullmatch(r'[0-9a-f]{40}', commit):
        raise ValueError('Full commit SHA required')
    if not re.fullmatch(r'[0-9a-f]{64}', certificate):
        raise ValueError('Certificate SHA-256 required')
    code = int(code)
    if code < 1:
        raise ValueError('versionCode must be positive')
    apk_name = f'Race-Day-Dreams-{version}.apk'
    if apk.name != apk_name or apk.is_symlink() or not apk.is_file():
        raise ValueError('The versioned APK is required')
    return json.dumps({
        'schema': 1,
        'packageName': PACKAGE_NAME,
        'versionName': version,
        'versionCode': code,
        'minSdk': int(min_sdk),
        'apkName': apk_name,
        'apkUrl': RELEASE_DOWNLOAD_PREFIX + apk_name,
        'apkBytes': apk.stat().st_size,
        'apkSha256': digest(apk),
        'certificateSha256': certificate,
        'commit': commit,
        'notes': whats_new(version),
    }, sort_keys=True, ensure_ascii=False, indent=1) + '\n'


def check_not_downgrade(live_manifest_text, new_code):
    """Refuse to announce a lower versionCode than the live manifest; equal codes are never offered."""
    import json
    if live_manifest_text is None:
        return
    live = json.loads(live_manifest_text)
    if int(new_code) < int(live['versionCode']):
        raise ValueError(f"versionCode {new_code} is lower than the published {live['versionCode']}")


def verify_update_manifest(manifest_text, apk, certificate):
    """The published manifest must describe exactly the published APK and certificate."""
    import json
    data = json.loads(manifest_text)
    apk = Path(apk)
    if (data.get('apkName') != apk.name or data.get('apkSha256') != digest(apk)
            or data.get('apkBytes') != apk.stat().st_size or data.get('certificateSha256') != certificate
            or data.get('apkUrl') != RELEASE_DOWNLOAD_PREFIX + apk.name or data.get('schema') != 1
            or data.get('packageName') != PACKAGE_NAME):
        raise ValueError('Published update manifest does not match the published APK')
    return data


def build_info(version, code, commit, run_url, digests, android_certificate=None):
    names(version)
    if not re.fullmatch(r'[0-9a-f]{40}', commit):
        raise ValueError('Full commit SHA required')
    if not re.fullmatch(r'https://github.com/[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+/actions/runs/\d+', run_url):
        raise ValueError('Actions evidence link required')
    if android_certificate is not None and not re.fullmatch(r'[0-9a-f]{64}', android_certificate):
        raise ValueError('Android certificate SHA-256 required')
    lines = [
        'Race Day Dreams development/test build information',
        f'Version: {version} / versionCode {code} (iOS {bundle_version(version)} / build {code})',
        f'Source commit: {commit}',
        f'Verified CI run: {run_url}',
        'All binaries below were built and verified from this one commit in this one run.',
        '',
    ]
    labels = {
        'android-apk': ('Android APK (debug-signed test build; no in-place updates)' if android_certificate is None
                        else 'Android APK (signed with the Race Day Dreams release key, certificate SHA-256 '
                        f'{android_certificate}; contents identical to the verified test build)'),
        'ios-simulator': 'iOS Simulator app, universal arm64 + x86_64 (UNSIGNED developer/test build)',
        'iphoneos-unsigned': 'iPhone arm64 app (UNSIGNED; requires your own signing and provisioning)',
        'ipa': 'iPhone .ipa, the same app (UNSIGNED; re-sign with your own Apple ID before installing)',
    }
    for key in ('android-apk',) + IOS_KINDS:
        value = digests[key]
        if not re.fullmatch(r'[0-9a-f]{64}', value):
            raise ValueError('SHA-256 required for ' + key)
        lines.append(f'{labels[key]}: SHA-256 {value}')
    return '\n'.join(lines) + '\n'


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('--version', required=True)
    parser.add_argument('--directory', required=True, type=Path)
    mode = parser.add_mutually_exclusive_group(required=True)
    mode.add_argument('--prepare', type=Path)
    mode.add_argument('--verify', action='store_true')
    parser.add_argument('--expected-sha')
    mode.add_argument('--prepare-ios', type=Path, help='CI artifact directory for --kind')
    mode.add_argument('--verify-ios', action='store_true')
    mode.add_argument('--ios-names', action='store_true', help='print the versioned and latest names for --kind')
    mode.add_argument('--signed-check', type=Path, help='re-signed APK to compare with --verified-apk')
    mode.add_argument('--identity-check', type=Path, help='scratch-signed APK; prints its public fingerprint')
    mode.add_argument('--update-manifest', type=Path, help='versioned APK to describe')
    parser.add_argument('--verified-apk', type=Path)
    parser.add_argument('--apksigner-output', type=Path)
    parser.add_argument('--pin', type=Path)
    parser.add_argument('--live-manifest', type=Path)
    parser.add_argument('--kind', choices=IOS_KINDS)
    parser.add_argument('--version-code')
    parser.add_argument('--commit')
    args = parser.parse_args()
    if args.ios_names:
        print(*ios_names(args.version, args.kind)[0::2])
        raise SystemExit(0)
    if args.identity_check:
        actual = certificate_sha256(args.apksigner_output.read_text())
        if not same_content(args.verified_apk, args.identity_check):
            raise SystemExit('Re-signing changed APK contents')
        print(actual)
        raise SystemExit(0)
    if args.signed_check:
        pin = read_pin(args.pin)
        if pin is None:
            raise SystemExit('A committed certificate fingerprint is required to sign updates')
        actual = certificate_sha256(args.apksigner_output.read_text())
        if actual != pin:
            raise SystemExit(f'Signed APK certificate {actual} does not match the committed {pin}')
        if not same_content(args.verified_apk, args.signed_check):
            raise SystemExit('Re-signing changed APK contents')
        print(f'Release-signed APK verified: certificate {actual}; contents identical to the verified build')
        raise SystemExit(0)
    if args.update_manifest:
        pin = read_pin(args.pin)
        live = args.live_manifest.read_text() if args.live_manifest and args.live_manifest.exists() else None
        check_not_downgrade(live, args.version_code)
        (args.directory / UPDATE_MANIFEST).write_text(
            update_manifest(args.update_manifest, args.version, args.version_code, args.commit, pin))
        print(f'Wrote {UPDATE_MANIFEST} for {args.update_manifest.name}')
        raise SystemExit(0)
    if args.prepare_ios:
        value = prepare_ios(args.prepare_ios, args.version, args.version_code or '', args.commit or '',
                            args.kind, args.directory)
        print(f'Verified {args.kind} archive {value} from commit {args.commit}')
    elif args.verify_ios:
        print('Verified iOS pairs: ' + ', '.join(
            verify_ios(args.directory, args.version, args.kind, args.expected_sha or '')))
    else:
        result = (prepare(args.prepare, args.version, args.directory) if args.prepare else
                  verify(args.directory, args.version, args.expected_sha or ''))
        print('Verified matching versioned/rolling APK and checksum pairs: ' + ', '.join(result))
