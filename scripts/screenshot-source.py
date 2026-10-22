#!/usr/bin/env python3
"""Fingerprint code that can change canonical screenshots; no user/device data is read."""
from pathlib import Path
import hashlib
root=Path(__file__).resolve().parent.parent
paths=[]
for directory in ('app/src/main','domain/src/main'):
    paths.extend(p for p in (root/directory).rglob('*') if p.is_file())
paths.extend(root/p for p in ('app/src/androidTest/java/org/racedaydreams/coach/CoachingScreenshotTest.kt','app/build.gradle','scripts/screenshot-source.py'))
hash=hashlib.sha256()
for path in sorted(paths):
    hash.update(str(path.relative_to(root)).encode()+b'\0'+path.read_bytes()+b'\0')
print(hash.hexdigest())
