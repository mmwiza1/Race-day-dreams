#!/usr/bin/env python3
"""Render maintained user-facing notes plus verified build identity. No user data/logs."""
from pathlib import Path
import argparse
import re


def render(root, commit, run_url, development=False, tag=None):
    build=(root/'app/build.gradle').read_text()
    name=re.search(r"versionName\s+'([^']+)'",build).group(1)
    code=re.search(r'versionCode\s+(\d+)',build).group(1)
    if not re.fullmatch(r'[a-zA-Z0-9.-]+',name):raise ValueError('Unsafe version name')
    if not re.fullmatch(r'[0-9a-f]{40}',commit):raise ValueError('Full commit SHA required')
    if not re.fullmatch(r'https://github.com/[^/]+/[^/]+/actions/runs/\d+',run_url):raise ValueError('Actions evidence link required')
    if not development and tag!='v'+name:raise ValueError('Tag must match the built app version')
    release=re.match(r'(\d+)\.(\d+)\.(\d+)',name)
    source=root/('docs/RELEASE_'+'_'.join(release.groups())+'.md')
    notes=source.read_text()
    required=("What's new",'Improvements','Fixes','Known limitations')
    for section in required:
        if not re.search(r'^## '+re.escape(section)+r'\s*$',notes,re.M):raise ValueError('Missing human-readable section: '+section)
    prefix=('**Latest Development Build — pre-release.** This rolling download tracks verified main; it is not a permanent release.\n\n' if development else '**Test pre-release.** Not a Play Store/production package.\n\n')
    return prefix+notes+f'''\n\n## Downloads

Install `Race-Day-Dreams-{name}.apk` from this release. The SHA-256 companion file
checks download integrity. Export a complete local backup before installing.
This APK is a **debug-signed test build**, not production-signed. Debug keys may
differ between clean CI runners, so an upgrade may require backup/reinstall/restore.
The app itself requires no account, Internet, analytics or subscription.

## Build information

- Version: {name} / versionCode {code}
- Commit: `{commit}`
- [Verified CI run]({run_url}): domain/schema checks, Android build/lint,
  API 28/35/36 instrumentation and authentic screenshot capture completed before publication.
- Physical Galaxy S24 acceptance remains a separate human checklist in TEST_BUILD.md;
  emulator success does not establish outdoor GNSS, BLE, audio or battery behaviour.
'''

if __name__=='__main__':
    parser=argparse.ArgumentParser();parser.add_argument('--commit',required=True);parser.add_argument('--run-url',required=True);parser.add_argument('--tag');parser.add_argument('--development',action='store_true');parser.add_argument('--output',type=Path,required=True)
    args=parser.parse_args()
    output=render(Path(__file__).resolve().parents[1],args.commit,args.run_url,args.development,args.tag)
    args.output.write_text(output)
