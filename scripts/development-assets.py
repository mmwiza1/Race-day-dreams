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
IOS_KINDS = ('ios-simulator', 'iphoneos-unsigned')
BUILD_INFO_ALIAS = 'Race-Day-Dreams-latest-build-info.txt'


def ios_names(version, kind):
    names(version)  # same strict development/test version contract as Android
    if kind not in IOS_KINDS:
        raise ValueError('Unknown iOS artifact kind')
    versioned = f'Race-Day-Dreams-{version}-{kind}.zip'
    alias = f'Race-Day-Dreams-latest-{kind}.zip'
    return (versioned, versioned + '.sha256', alias, alias + '.sha256')


def bundle_version(version):
    """Info.plist CFBundleShortVersionString for a development/test versionName."""
    names(version)
    return version.split('-', 1)[0]


def check_ios_source(source, version, code, commit, kind):
    """Validate one CI iOS artifact directory: zip, checksum, commit and bundle version files."""
    source = Path(source)
    zip_name = {'ios-simulator': 'RaceDayDreams-simulator-universal.zip',
                'iphoneos-unsigned': 'RaceDayDreams-iphoneos-unsigned.zip'}[kind]
    archive = source / zip_name
    if archive.is_symlink() or not archive.is_file() or archive.stat().st_size == 0:
        raise ValueError(f'Missing or empty {kind} archive')
    expected = digest(archive)
    recorded = (source / (zip_name + '.sha256')).read_text().split()
    if not recorded or recorded[0] != expected:
        raise ValueError(f'{kind} checksum does not match its archive')
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


def build_info(version, code, commit, run_url, digests):
    names(version)
    if not re.fullmatch(r'[0-9a-f]{40}', commit):
        raise ValueError('Full commit SHA required')
    if not re.fullmatch(r'https://github.com/[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+/actions/runs/\d+', run_url):
        raise ValueError('Actions evidence link required')
    lines = [
        'Race Day Dreams development/test build information',
        f'Version: {version} / versionCode {code} (iOS {bundle_version(version)} / build {code})',
        f'Source commit: {commit}',
        f'Verified CI run: {run_url}',
        'All binaries below were built and verified from this one commit in this one run.',
        '',
    ]
    labels = {
        'android-apk': 'Android APK (debug-signed test build)',
        'ios-simulator': 'iOS Simulator app, universal arm64 + x86_64 (UNSIGNED developer/test build)',
        'iphoneos-unsigned': 'iPhone arm64 app (UNSIGNED; requires your own signing and provisioning)',
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
    parser.add_argument('--kind', choices=IOS_KINDS)
    parser.add_argument('--version-code')
    parser.add_argument('--commit')
    args = parser.parse_args()
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
