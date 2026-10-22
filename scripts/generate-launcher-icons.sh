#!/usr/bin/env bash
# Deterministic resizing/masking only; never redraws the approved illustration.
set -euo pipefail
cd "$(dirname "$0")/.."
work=$(mktemp -d)
trap 'rm -rf "$work"' EXIT
magick artwork/launcher/approved.jpg -crop 1000x1000+140+130 +repage \
  -alpha on -fuzz 24% -fill none \
  -draw 'color 0,0 floodfill' -draw 'color 999,0 floodfill' \
  -draw 'color 0,999 floodfill' -draw 'color 999,999 floodfill' "$work/art.png"
# 66dp of the 108dp adaptive canvas: essential details fit the safe circle.
for item in mdpi:108:66:48 hdpi:162:99:72 xhdpi:216:132:96 xxhdpi:324:198:144 xxxhdpi:432:264:192; do
  IFS=: read -r density canvas artwork legacy <<< "$item"
  dest="app/src/main/res/mipmap-$density"
  mkdir -p "$dest"
  magick "$work/art.png" -resize "${artwork}x${artwork}" \
    -gravity center -background none -extent "${canvas}x${canvas}" -strip "$dest/ic_launcher_foreground.png"
  # Legacy launchers get a complete icon, with the same visible scale as adaptive.
  size=$((legacy * 11 / 12))
  magick "$work/art.png" -resize "${size}x${size}" -gravity center \
    -background '#0843A0' -extent "${legacy}x${legacy}" "$work/square.png"
  magick -size "${legacy}x${legacy}" xc:none -fill white \
    -draw "roundrectangle 0,0,$((legacy-1)),$((legacy-1)),$((legacy/5)),$((legacy/5))" "$work/mask.png"
  magick "$work/square.png" "$work/mask.png" -compose DstIn -composite -strip "$dest/ic_launcher.png"
  magick -size "${legacy}x${legacy}" xc:none -fill white \
    -draw "circle $((legacy/2)),$((legacy/2)) $((legacy/2)),0" "$work/mask.png"
  magick "$work/square.png" "$work/mask.png" -compose DstIn -composite -strip "$dest/ic_launcher_round.png"
done
