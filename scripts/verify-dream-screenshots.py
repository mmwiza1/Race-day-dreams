#!/usr/bin/env python3
"""Validate all canonical captures, including Dream panels; not visual fidelity or API hashes."""
import argparse
from pathlib import Path
import struct
import sys
import zlib

CITIES = ("london", "berlin", "paris", "tokyo", "new-york", "dark", "large-font")
DREAM_REQUIRED = tuple(f"dream-route-{city}{suffix}.png" for city in CITIES for suffix in ("", "-landmarks"))
# Preserve every established whole-app capture when adding the seven lower Dream panels.
OTHER_REQUIRED = (
    'activity-detail.png',
    'body-weight.png',
    'calendar-month.png',
    'calendar-week.png',
    'data-sources.png',
    'dream-route.png',
    'exercise-ankle-rocks.png',
    'exercise-arnold-press-howto.png',
    'exercise-bench-howto.png',
    'exercise-bulgarian-howto.png',
    'exercise-bulgarian-split-squat.png',
    'exercise-cable-crunch-howto.png',
    'exercise-cable-row-howto.png',
    'exercise-clean-jerk-howto.png',
    'exercise-farmers-carry-howto.png',
    'exercise-front-squat-howto.png',
    'exercise-hack-squat-howto.png',
    'exercise-lat-pulldown-howto.png',
    'exercise-leg-press-howto.png',
    'exercise-plank-howto.png',
    'exercise-rdl-howto.png',
    'exercise-rdl.png',
    'exercise-search.png',
    'exercise-seated-leg-curl-howto.png',
    'exercise-static-stretch.png',
    'exercise-walking-lunge.png',
    'exercise-weighted-wall-sit-howto.png',
    'gym-programme-setup.png',
    'gym-running-support.png',
    'gym-strength-first.png',
    'gym-workout-detail.png',
    'health-context.png',
    'history-filters.png',
    'history.png',
    'home-dark.png',
    'home-large-font.png',
    'home.png',
    'live-coach.png',
    'mobility-dark.png',
    'mobility.png',
    'onboarding.png',
    'plan-dark.png',
    'plan-large-font.png',
    'planned-workout.png',
    'post-run-route.png',
    'progress.png',
    'race-day.png',
    'record-run.png',
    'run-comparison.png',
    'run-feedback.png',
    'run-now.png',
    'running-pbs.png',
    'settings.png',
    'shoes.png',
    'strength-log-distance.png',
    'strength-log-previous.png',
    'strength-log-timed.png',
    'strength-pbs.png',
    'strength.png',
    'training-plan.png',
    'training-trends.png',
    'warm-up.png',
    'weekly-review.png',
)
REQUIRED = tuple(sorted(OTHER_REQUIRED + DREAM_REQUIRED))
PNG_SIGNATURE = b"\x89PNG\r\n\x1a\n"
MAX_FILE_BYTES = 16 * 1024 * 1024
MAX_DECODED_BYTES = 42 * 1024 * 1024


def png_dimensions(data):
    """Bounded PNG structure/CRC/inflate checks for non-interlaced Android captures."""
    if not 57 <= len(data) <= MAX_FILE_BYTES or not data.startswith(PNG_SIGNATURE):
        raise ValueError("invalid or unbounded PNG file")
    offset = 8
    dimensions = None
    idat = bytearray()
    idat_ended = False
    saw_palette = False
    saw_end = False
    while offset < len(data):
        if offset + 12 > len(data):
            raise ValueError("truncated PNG chunk")
        length = struct.unpack_from(">I", data, offset)[0]
        kind = data[offset + 4:offset + 8]
        end = offset + 12 + length
        if end > len(data) or not all(65 <= c <= 90 or 97 <= c <= 122 for c in kind):
            raise ValueError("invalid PNG chunk")
        payload = data[offset + 8:end - 4]
        actual_crc = struct.unpack_from(">I", data, end - 4)[0]
        if zlib.crc32(kind + payload) & 0xffffffff != actual_crc:
            raise ValueError("PNG chunk checksum mismatch")
        if dimensions is None and kind != b"IHDR":
            raise ValueError("PNG must start with IHDR")
        if kind == b"IHDR":
            if dimensions is not None or length != 13:
                raise ValueError("invalid or duplicate PNG header")
            width, height, depth, colour, compression, filtering, interlace = struct.unpack(">IIBBBBB", payload)
            depths = {0: (1, 2, 4, 8, 16), 2: (8, 16), 3: (1, 2, 4, 8), 4: (8, 16), 6: (8, 16)}
            if not 320 <= width <= 1600 or not 640 <= height <= 3200:
                raise ValueError("PNG must have bounded phone-screen dimensions")
            if depth not in depths.get(colour, ()) or compression or filtering or interlace:
                raise ValueError("unsupported PNG format for Android capture")
            channels = {0: 1, 2: 3, 3: 1, 4: 2, 6: 4}[colour]
            row_bytes = (width * channels * depth + 7) // 8
            decoded_size = height * (row_bytes + 1)
            if decoded_size > MAX_DECODED_BYTES:
                raise ValueError("PNG decoded size is unbounded")
            dimensions = (width, height)
        elif kind == b"PLTE":
            if saw_palette or idat or not 3 <= length <= 768 or length % 3:
                raise ValueError("invalid PNG palette")
            saw_palette = True
        elif kind == b"IDAT":
            if idat_ended or (colour == 3 and not saw_palette):
                raise ValueError("invalid PNG image-data order")
            idat.extend(payload)
        elif kind == b"IEND":
            if length or not idat or end != len(data):
                raise ValueError("invalid PNG end or trailing bytes")
            saw_end = True
            offset = end
            break
        elif kind[0] & 32 == 0:
            raise ValueError("unknown critical PNG chunk")
        if kind != b"IDAT" and idat:
            idat_ended = True
        offset = end
    if not saw_end:
        raise ValueError("PNG missing complete image data/end")
    try:
        decoder = zlib.decompressobj()
        pixels = decoder.decompress(idat, decoded_size + 1)
        if len(pixels) != decoded_size or not decoder.eof or decoder.unused_data or decoder.unconsumed_tail:
            raise ValueError("invalid or unbounded PNG pixel stream")
    except zlib.error as error:
        raise ValueError("corrupt PNG pixel stream") from error
    if any(pixels[row * (row_bytes + 1)] > 4 for row in range(height)):
        raise ValueError("invalid PNG scanline filter")
    return dimensions


def validate_captures(directory):
    directory = Path(directory)
    if not directory.is_dir():
        raise ValueError(f"screenshot directory does not exist: {directory}")
    dimensions = {}
    for name in REQUIRED:
        path = directory / name
        if not path.is_file() or path.is_symlink():
            raise ValueError(f"missing real capture: {name}")
        if path.stat().st_size > MAX_FILE_BYTES:
            raise ValueError(f"unbounded capture: {name}")
        try:
            dimensions[name] = png_dimensions(path.read_bytes())
        except ValueError as error:
            raise ValueError(f"{name}: {error}") from error
    return dimensions


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("directory", type=Path, help="actual-build PNG directory")
    args = parser.parse_args()
    try:
        dimensions = validate_captures(args.directory)
    except (OSError, ValueError) as error:
        print(f"Dream screenshot verification failed: {error}", file=sys.stderr)
        return 1
    sizes = ", ".join(f"{width}x{height}" for width, height in sorted(set(dimensions.values())))
    print(f"Canonical screenshots verified: {len(dimensions)} required, including {len(DREAM_REQUIRED)} Dream top/landmark panels; dimensions {sizes}")
    print("File integrity is not design-reference acceptance; inspect real captures separately.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
