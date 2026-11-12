#!/usr/bin/env python3
"""Add aliases ONLY for the already published, successful main build. Never publishes a PR APK."""
from pathlib import Path
import hashlib
import importlib.util
import json
import os
import re
import shutil
import stat
import subprocess
import zipfile

spec = importlib.util.spec_from_file_location('assets', Path(__file__).with_name('development-assets.py'))
assets = importlib.util.module_from_spec(spec)
spec.loader.exec_module(assets)


def identity(release, run, tag, repository):
    if not re.fullmatch(r'[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+', repository):
        raise ValueError('Repository required')
    body = release['body'].rsplit('## Build information', 1)[-1]
    version = re.search(r'^- Version: (\d+\.\d+\.\d+-test\d+) / versionCode (\d+)\s*$', body, re.M)
    source = re.search(r'^- Commit: `([0-9a-f]{40})`\s*$', body, re.M)
    evidence = re.search(r'https://github.com/' + re.escape(repository) + r'/actions/runs/(\d+)', body)
    if not version or not source or not evidence:
        raise ValueError('Published build identity is incomplete')
    sha = source.group(1)
    if release['tag_name'] != 'development' or not release['prerelease'] or release['draft']:
        raise ValueError('Only the rolling test release is eligible')
    if (run['head_sha'] != sha or str(run['id']) != evidence.group(1) or
            run['status'] != 'completed' or run['conclusion'] != 'success' or
            run['event'] != 'push' or run['head_branch'] != 'main' or tag['object']['sha'] != sha):
        raise ValueError('Release must match a successful main workflow and development tag')
    assets.names(version.group(1))
    return version.group(1), sha, int(evidence.group(1))


def main():
    repository = os.environ['GH_REPO']
    root = Path('build/alias-bootstrap')
    root.mkdir(parents=True, exist_ok=True)

    def api(path):
        return json.loads(subprocess.check_output(['gh', 'api', f'repos/{repository}/{path}'], text=True))

    release = api('releases/tags/development')
    match = re.search(r'https://github.com/' + re.escape(repository) + r'/actions/runs/(\d+)', release['body'].rsplit('## Build information', 1)[-1])
    if not match:
        raise ValueError('Missing workflow evidence')
    run = api('actions/runs/' + match.group(1))
    version, source, run_id = identity(release, run, api('git/ref/tags/development'), repository)
    artifact = next(a for a in api(f'actions/runs/{run_id}/artifacts')['artifacts']
                    if a['name'] == f'Race-Day-Dreams-{version}-debug-api-35')
    if artifact['expired'] or artifact['workflow_run']['head_sha'] != source:
        raise ValueError('Verified main APK artifact is unavailable')
    archive = root / 'verified-main.zip'
    with archive.open('wb') as out:
        subprocess.run(['gh', 'api', f'repos/{repository}/actions/artifacts/{artifact["id"]}/zip'], stdout=out, check=True)
    if archive.stat().st_size != artifact['size_in_bytes'] or 'sha256:' + assets.digest(archive) != artifact['digest']:
        raise ValueError('Main artifact ZIP mismatch')
    with zipfile.ZipFile(archive) as zipped:
        entries = zipped.infolist()
        if len(entries) != 1 or not entries[0].filename.endswith(f'Race-Day-Dreams-{version}-debug.apk') or entries[0].file_size > 100 * 1024 * 1024 or stat.S_ISLNK(entries[0].external_attr >> 16):
            raise ValueError('Unexpected main APK archive')
        with zipped.open(entries[0]) as src, (root / 'ci.apk').open('wb') as out:
            shutil.copyfileobj(src, out)
    expected = assets.digest(root / 'ci.apk')
    apk = f'Race-Day-Dreams-{version}.apk'
    public = root / 'public'
    subprocess.run(['gh', 'release', 'download', 'development', '--repo', repository,
                    '--pattern', apk, '--pattern', apk + '.sha256', '--dir', str(public), '--clobber'], check=True)
    if assets.digest(public / apk) != expected or (public / (apk + '.sha256')).read_text() != f'{expected}  {apk}\n':
        raise ValueError('Public APK differs from the successful main artifact')
    identity(api('releases/tags/development'), run, api('git/ref/tags/development'), repository)
    prepared = root / 'prepared'
    assets.prepare(public / apk, version, prepared)
    subprocess.run(['gh', 'release', 'upload', 'development', '--repo', repository,
                    str(prepared / assets.ALIAS), str(prepared / (assets.ALIAS + '.sha256')), '--clobber'], check=True)
    subprocess.run(['gh', 'release', 'download', 'development', '--repo', repository,
                    '--pattern', apk, '--pattern', apk + '.sha256', '--pattern', assets.ALIAS,
                    '--pattern', assets.ALIAS + '.sha256', '--dir', str(public), '--clobber'], check=True)
    assets.verify(public, version, expected)
    identity(api('releases/tags/development'), run, api('git/ref/tags/development'), repository)
    result = {'version': version, 'source': source, 'verified_run': run_id,
              'artifact': artifact['id'], 'sha256': expected, 'bytes': (public / apk).stat().st_size}
    (root / 'verification.json').write_text(json.dumps(result, indent=2) + '\n')
    print('BOOTSTRAP VERIFIED ' + json.dumps(result))


if __name__ == '__main__':
    main()
