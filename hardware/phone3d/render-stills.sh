#!/usr/bin/env bash
# All the still renders, one after the other (Cycles uses every core). SAMPLES=110 is a clean image after the denoiser; ~3 minutes per image on 4 cores.
set -uo pipefail
cd "$(dirname "$0")"
SAMPLES="${SAMPLES:-110}"
SHOTS="${SHOTS:-hero exploded_wide exploded open_back som electronics side keys front back teardown}"
mkdir -p out
for s in $SHOTS; do
  echo "== $s"; python3 render_scene.py still "$s" "out/futureos_regular_$s.png" --samples "$SAMPLES" 2>&1 | grep -E "Traceback|Error:|Saved" || true
done
echo "all done"
