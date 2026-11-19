#!/usr/bin/env python3
"""Prepare/verify versioned and rolling APK pairs; no publishing or network calls."""
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


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('--version', required=True)
    parser.add_argument('--directory', required=True, type=Path)
    mode = parser.add_mutually_exclusive_group(required=True)
    mode.add_argument('--prepare', type=Path)
    mode.add_argument('--verify', action='store_true')
    parser.add_argument('--expected-sha')
    args = parser.parse_args()
    result = (prepare(args.prepare, args.version, args.directory) if args.prepare else
              verify(args.directory, args.version, args.expected_sha or ''))
    print('Verified matching versioned/rolling APK and checksum pairs: ' + ', '.join(result))
