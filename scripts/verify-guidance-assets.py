#!/usr/bin/env python3
"""Verify every packaged original guide has a matching, exact provenance entry."""
import hashlib
import json
from pathlib import Path

root = Path(__file__).resolve().parent.parent
manifest = json.loads((root / 'docs/exercise-artwork/asset-provenance.json').read_text())
if manifest.get('version') != 1:
    raise SystemExit('Unsupported artwork provenance version')
expected = {p.relative_to(root).as_posix() for p in
            (root / 'app/src/main/res/drawable-nodpi').glob('guide_*.webp')}
seen = set()
total = 0
for entry in manifest['assets']:
    path = entry['file']
    if path not in expected or path in seen:
        raise SystemExit('Unpackaged or duplicate provenance entry: ' + path)
    seen.add(path)
    data = (root / path).read_bytes()
    if len(data) != entry['bytes'] or hashlib.sha256(data).hexdigest() != entry['sha256']:
        raise SystemExit('Artwork no longer matches reviewed provenance: ' + path)
    if not entry.get('creation') or entry.get('licence') != 'MIT project contribution':
        raise SystemExit('Missing original artwork attribution: ' + path)
    total += len(data)
if expected != seen:
    raise SystemExit('Missing artwork provenance: ' + ', '.join(sorted(expected - seen)))
print(f'Artwork provenance verified: {len(seen)} original packaged assets, {total:,} bytes')
