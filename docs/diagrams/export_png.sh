#!/usr/bin/env bash
set -euo pipefail

# Rasterise every SVG in exports/ to a PNG at twice its size, for slides and printed material.
# Needs only a Chrome or Chromium binary; no project dependency is added.
#   CHROME=/path/to/chrome docs/diagrams/export_png.sh [output directory]
task_here="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
task_out="${1:-$task_here/exports/png}"
task_chrome="${CHROME:-}"
if [ -z "$task_chrome" ]; then
  for task_candidate in \
    "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome" \
    "/Applications/Chromium.app/Contents/MacOS/Chromium" \
    "$(command -v google-chrome || true)" "$(command -v chromium || true)" "$(command -v chromium-browser || true)"; do
    if [ -n "$task_candidate" ] && [ -x "$task_candidate" ]; then
      task_chrome="$task_candidate"
      break
    fi
  done
fi
if [ -z "$task_chrome" ]; then
  printf 'Set CHROME to a Chrome or Chromium binary.\n' >&2
  exit 1
fi
mkdir -p "$task_out"
for task_svg in "$task_here"/exports/*.svg; do
  task_size="$(sed -n 's/.*<svg[^>]* width="\([0-9]*\)" height="\([0-9]*\)".*/\1,\2/p' "$task_svg" | head -n 1)"
  task_png="$task_out/$(basename "${task_svg%.svg}").png"
  "$task_chrome" --headless --disable-gpu --hide-scrollbars --force-device-scale-factor=2 \
    --window-size="$task_size" --screenshot="$task_png" "file://$task_svg" >/dev/null 2>&1
  printf '%s\n' "$task_png"
done
