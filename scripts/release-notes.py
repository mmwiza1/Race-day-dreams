#!/usr/bin/env python3
"""Render maintained user-facing notes plus verified build identity. No user data/logs."""
from pathlib import Path
import argparse
import re


def render(root, commit, run_url, development=False, tag=None):
    build = (root / 'app/build.gradle').read_text()
    name = re.search(r"versionName\s+'([^']+)'", build).group(1)
    code = re.search(r'versionCode\s+(\d+)', build).group(1)
    if not re.fullmatch(r'[a-zA-Z0-9.-]+', name):
        raise ValueError('Unsafe version name')
    if not re.fullmatch(r'[0-9a-f]{40}', commit):
        raise ValueError('Full commit SHA required')
    if not re.fullmatch(r'https://github.com/[^/]+/[^/]+/actions/runs/\d+', run_url):
        raise ValueError('Actions evidence link required')
    if not development and tag != 'v' + name:
        raise ValueError('Tag must match the built app version')

    release = re.match(r'(\d+)\.(\d+)\.(\d+)', name)
    source = root / ('docs/RELEASE_' + '_'.join(release.groups()) + '.md')
    notes = source.read_text().strip()

    expected_title = r'^# Race Day Dreams ' + re.escape(name) + r'\s*$'
    if not re.search(expected_title, notes, re.M):
        raise ValueError('Release title must match the built app version')

    required = ("What's new", 'Improvements', 'Fixes', 'Known limitations')
    for section in required:
        if not re.search(r'^## ' + re.escape(section) + r'\s*$', notes, re.M):
            raise ValueError('Missing human-readable section: ' + section)

    generated = ('Download', 'Installation and first launch', 'Build information')
    for section in generated:
        if re.search(r'^## ' + re.escape(section) + r'\s*$', notes, re.M):
            raise ValueError('Section is generated automatically: ' + section)

    intro, sections = re.compile(r'(?=^## )', re.M).split(notes, maxsplit=1)
    status = (
        '> **Latest Development Build — pre-release.** This rolling download tracks '
        'verified `main`; it is not a permanent release.'
        if development else
        '> **Test pre-release.** This is not a Play Store/production package.'
    )

    download = f'''## Download

- **APK:** `Race-Day-Dreams-{name}.apk`
- **SHA-256:** `Race-Day-Dreams-{name}.apk.sha256`
- **Build:** debug-signed Android test build

Open **Assets** at the bottom of this GitHub release to download the APK and its
checksum. The checksum file can be used to verify download integrity.
'''

    installation = f'''## Installation and first launch

1. Download `Race-Day-Dreams-{name}.apk` from **Assets**.
2. Open the APK. If Android blocks the install, allow **Install unknown apps** only
   for the browser or file manager you are using, then retry.
3. Existing testers should export a complete local backup before upgrading.
4. Install and open Race Day Dreams. No account, subscription, analytics or Internet
   connection is required by the app.
5. If Android reports a signing conflict, back up your data, uninstall the previous
   debug build, install this APK, then restore your backup.

CI debug-signing keys can differ between clean runners, so some test-build upgrades
may require the backup/reinstall/restore path above.
'''

    build_info = f'''## Build information

- Version: {name} / versionCode {code}
- Commit: `{commit}`
- [Verified CI run]({run_url}): domain/schema checks, Android build/lint,
  API 28/35/36 instrumentation and authentic screenshot capture completed before publication.
- Physical Galaxy S24 acceptance remains a separate human checklist in TEST_BUILD.md;
  emulator success does not establish outdoor GNSS, BLE, audio or battery behaviour.
'''

    return (
        intro.rstrip() + '\n\n' + status + '\n\n' +
        download.rstrip() + '\n\n' +
        installation.rstrip() + '\n\n' +
        sections.lstrip().rstrip() + '\n\n' +
        build_info.rstrip() + '\n'
    )


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('--commit', required=True)
    parser.add_argument('--run-url', required=True)
    parser.add_argument('--tag')
    parser.add_argument('--development', action='store_true')
    parser.add_argument('--output', type=Path, required=True)
    args = parser.parse_args()
    output = render(
        Path(__file__).resolve().parents[1],
        args.commit,
        args.run_url,
        args.development,
        args.tag,
    )
    args.output.write_text(output)
