#!/usr/bin/env python3
"""Audit bundled Dream artwork against its reviewed provenance, without dependencies.

The WebP check validates the container and encoded dimensions; exact SHA-256 values
bind each asset to the images decoded and reviewed during development. This is not
a substitute for Android decoding or the actual-build visual acceptance gate.
"""
import hashlib
import json
from pathlib import Path
import re
import struct

ASSET_DIRECTORY = "app/src/main/assets/dream-art"
MANIFEST_PATH = "docs/dream-artwork-0.8.2.json"
MAX_ASSET_BYTES = 256 * 1024
MAX_TOTAL_BYTES = 2 * 1024 * 1024
LANDMARKS = {
    "london": (("tower-bridge", "Tower Bridge"),
               ("buckingham-palace", "Buckingham Palace"), ("london-eye", "London Eye")),
    "berlin": (("brandenburg-gate", "Brandenburg Gate"),
               ("reichstag", "Reichstag"), ("victory-column", "Victory Column")),
    "paris": (("eiffel-tower", "Eiffel Tower"),
              ("vincennes", "Château de Vincennes"), ("louvre", "Louvre")),
    "tokyo": (("tokyo-tower", "Tokyo Tower"),
              ("tokyo-station", "Tokyo Station"), ("senso-ji", "Sensō-ji")),
    "new-york": (("verrazzano", "Verrazzano Bridge"),
                 ("queensboro", "Queensboro Bridge"), ("central-park", "Central Park finish")),
}
CLEAN_POSTER_START = {"london": 512, "berlin": 522, "paris": 531,
                      "tokyo": 549, "new-york": 528}
CITY_TITLES = {"london": "London", "berlin": "Berlin", "paris": "Paris",
               "tokyo": "Tokyo", "new-york": "New York"}
SHA256 = re.compile(r"[0-9a-f]{64}\Z")
PLATE = re.compile(r"exec-[0-9a-f]{8}(?:-[0-9a-f]{4}){3}-[0-9a-f]{12}\.png\Z")


def require(condition, message):
    if not condition:
        raise ValueError(message)


def webp_dimensions(data):
    """Read one non-animated WebP's dimensions and validate RIFF chunk bounds."""
    require(12 <= len(data) <= MAX_ASSET_BYTES, "WebP size is outside the asset bound")
    require(data[:4] == b"RIFF" and data[8:12] == b"WEBP", "Not a WebP RIFF container")
    require(struct.unpack_from("<I", data, 4)[0] + 8 == len(data), "WebP RIFF length mismatch")
    offset = 12
    encoded = None
    canvas = None
    while offset < len(data):
        require(offset + 8 <= len(data), "Truncated WebP chunk header")
        kind = data[offset:offset + 4]
        length = struct.unpack_from("<I", data, offset + 4)[0]
        start = offset + 8
        end = start + length
        padded_end = end + length % 2
        require(end <= len(data) and padded_end <= len(data), "Truncated WebP chunk payload")
        payload = data[start:end]
        require(kind not in (b"ANIM", b"ANMF"), "Animated artwork is not supported")
        if kind == b"VP8X":
            require(canvas is None and length == 10, "Invalid WebP extended header")
            require(not payload[0] & 2, "Animated artwork is not supported")
            canvas = (int.from_bytes(payload[4:7], "little") + 1,
                      int.from_bytes(payload[7:10], "little") + 1)
        elif kind in (b"VP8 ", b"VP8L"):
            require(encoded is None, "Multiple encoded images in one artwork")
            if kind == b"VP8 ":
                require(length >= 10 and payload[0] & 1 == 0
                        and payload[3:6] == b"\x9d\x01\x2a", "Invalid WebP keyframe header")
                encoded = (struct.unpack_from("<H", payload, 6)[0] & 0x3fff,
                           struct.unpack_from("<H", payload, 8)[0] & 0x3fff)
            else:
                require(length >= 5 and payload[0] == 0x2f, "Invalid lossless WebP header")
                packed = int.from_bytes(payload[1:5], "little")
                require(packed >> 29 == 0, "Unsupported lossless WebP version")
                encoded = ((packed & 0x3fff) + 1, ((packed >> 14) & 0x3fff) + 1)
        offset = padded_end
    require(encoded is not None, "WebP has no encoded image")
    require(all(0 < value <= 1200 for value in encoded), "WebP dimensions outside display bounds")
    require(canvas is None or canvas == encoded, "WebP canvas/frame dimensions disagree")
    return encoded


def expected_assets():
    expected = {}
    for city, landmarks in LANDMARKS.items():
        expected[f"{ASSET_DIRECTORY}/{city}-hero.webp"] = (
            city, "hero", CITY_TITLES[city] + " city panorama", (1200, 400), [0, 0, 1536, 512])
        start = CLEAN_POSTER_START[city]
        side = 1024 - start
        for index, (slug, label) in enumerate(landmarks):
            left = index * 512 + (512 - side) // 2
            expected[f"{ASSET_DIRECTORY}/{city}-{slug}.webp"] = (
                city, "landmark", label, (400, 400), [left, start, left + side, 1024])
    return expected


def validate_manifest(root, manifest):
    """Validate complete coverage, reviewed metadata, dimensions, size and hashes."""
    root = Path(root).resolve()
    require(manifest.get("version") == 1 and manifest.get("release") == "0.8.2-test1",
            "Unsupported Dream artwork manifest identity")
    for field in ("created", "ownership", "purpose", "sourceOfLandmarkLabels", "processing"):
        require(isinstance(manifest.get(field), str) and manifest[field].strip(),
                "Missing Dream artwork provenance field: " + field)
    expected = expected_assets()
    assets = manifest.get("assets")
    require(isinstance(assets, list) and len(assets) == len(expected)
            and manifest.get("assetCount") == len(expected), "Dream artwork must cover exactly 20 assets")
    packaged = root / ASSET_DIRECTORY
    actual = {path.relative_to(root).as_posix() for path in packaged.rglob("*") if path.is_file()}
    require(actual == set(expected), "Packaged Dream artwork does not match five complete cities")
    require(not any(path.is_symlink() for path in packaged.rglob("*")), "Symlinked artwork is not allowed")
    seen = set()
    plates = {}
    total = 0
    for entry in assets:
        require(isinstance(entry, dict), "Artwork entry must be an object")
        name = entry.get("file")
        require(isinstance(name, str) and name in expected and name not in seen,
                "Unexpected, duplicate or unsafe Dream artwork path: " + str(name))
        seen.add(name)
        city, kind, label, dimensions, crop = expected[name]
        require((entry.get("city"), entry.get("kind"), entry.get("label")) == (city, kind, label),
                "Artwork label/city mismatch: " + name)
        require((entry.get("width"), entry.get("height")) == dimensions,
                "Artwork dimensions metadata mismatch: " + name)
        require(entry.get("sourceWidth") == 1536 and entry.get("sourceHeight") == 1024
                and entry.get("sourceCrop") == crop, "Artwork reviewed crop mismatch: " + name)
        require(entry.get("encoding") == {"format": "WebP", "quality": 85,
                                          "method": 6, "resample": "Lanczos"},
                "Artwork encoding provenance mismatch: " + name)
        require(entry.get("licence") == "MIT project contribution", "Missing original artwork licence: " + name)
        for field in ("creation", "accessibility", "labelSource"):
            require(isinstance(entry.get(field), str) and entry[field].strip(),
                    "Missing artwork " + field + ": " + name)
        require("Original Race Day Dreams" in entry["creation"], "Missing artwork creation provenance: " + name)
        if kind == "hero":
            require("non-geographic" in entry["accessibility"] and "Decorative" in entry["accessibility"],
                    "Hero must be identified as decorative and non-geographic: " + name)
        require(isinstance(entry.get("sourcePlate"), str) and PLATE.fullmatch(entry["sourcePlate"]),
                "Invalid artwork source plate: " + name)
        require(isinstance(entry.get("sourcePlateSha256"), str) and SHA256.fullmatch(entry["sourcePlateSha256"]),
                "Invalid artwork source hash: " + name)
        source = (entry["sourcePlate"], entry["sourcePlateSha256"])
        require(city not in plates or plates[city] == source, "Inconsistent source plate for city: " + city)
        plates[city] = source
        require(isinstance(entry.get("sha256"), str) and SHA256.fullmatch(entry["sha256"]),
                "Invalid packaged artwork hash: " + name)
        path = root / name
        require(path.resolve().parent == packaged.resolve(), "Artwork escaped asset directory")
        require(0 < path.stat().st_size <= MAX_ASSET_BYTES,
                "Artwork file exceeds read bound: " + name)
        data = path.read_bytes()
        require(len(data) == entry.get("bytes") and 0 < len(data) <= MAX_ASSET_BYTES,
                "Artwork size mismatch: " + name)
        require(hashlib.sha256(data).hexdigest() == entry["sha256"], "Artwork SHA-256 mismatch: " + name)
        require(webp_dimensions(data) == dimensions, "Encoded artwork dimensions mismatch: " + name)
        total += len(data)
    require(seen == set(expected), "Missing Dream artwork provenance")
    require(len(set(plates.values())) == len(LANDMARKS), "Cities must not share a source panorama")
    require(total == manifest.get("packagedBytes") and total <= MAX_TOTAL_BYTES,
            "Dream artwork package total mismatch or size budget exceeded")
    return len(seen), total


def main():
    root = Path(__file__).resolve().parent.parent
    try:
        manifest = json.loads((root / MANIFEST_PATH).read_text(encoding="utf-8"))
        count, total = validate_manifest(root, manifest)
    except (OSError, ValueError, TypeError, KeyError) as error:
        raise SystemExit("Dream artwork audit failed: " + str(error)) from error
    print(f"Dream artwork verified: {count} original packaged assets, {len(LANDMARKS)} cities, {total:,} bytes")


if __name__ == "__main__":
    main()
