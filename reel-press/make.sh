#!/usr/bin/env bash
# The whole pipeline: assets -> music -> frames (parallel, with motion blur) -> mp4.
#   ./make.sh            full render, out/futureos-press-reel.mp4
#   PREVIEW=1 ./make.sh  fast draft, no motion blur, out/preview.mp4
#   ENCODE_ONLY=1 ./make.sh  re-mix the music and re-encode from the frames already rendered
set -euo pipefail
cd "$(dirname "$0")"
export NODE_PATH="${NODE_PATH:-/opt/node22/lib/node_modules}"
FF="$(python3 -c 'import imageio_ffmpeg; print(imageio_ffmpeg.get_ffmpeg_exe())')"
FRAMES="${FRAMES:-${TMPDIR:-/tmp}/reel-press-frames}"
WORKERS="${WORKERS:-4}"
mkdir -p out "$FRAMES"
[ -f assets/ds-tokens.css ] || python3 make-assets.py
python3 music.py > /dev/null
if [ -n "${PREVIEW:-}" ]; then SUB=1; OUT=out/preview.mp4; CRF=24; else SUB="${SUB:-6}"; OUT=out/futureos-press-reel.mp4; CRF=17; fi
export FRAMES SUB
if [ -z "${ENCODE_ONLY:-}" ]; then      # ENCODE_ONLY=1: reuse the frames already in $FRAMES (e.g. after changing only the music)
  for i in $(seq 0 $((WORKERS - 1))); do node render.js frames "$i" "$WORKERS" & done
  wait
fi
if [ "$SUB" -gt 1 ]; then    # SUB sub-frames per frame, averaged: a 180 degree shutter
  VF="tmix=frames=$SUB,select='eq(mod(n\,$SUB)\,$((SUB - 1)))',setpts=N/(30*TB),scale=out_color_matrix=bt709:out_range=tv,format=yuv420p"; IN=$((30 * SUB))
else
  VF="scale=out_color_matrix=bt709:out_range=tv,format=yuv420p"; IN=30
fi
"$FF" -y -framerate "$IN" -i "$FRAMES/s_%06d.png" -i out/audio.wav -vf "$VF" -r 30 \
  -c:v libx264 -preset slow -crf "$CRF" -profile:v high -level 4.2 -colorspace bt709 -color_primaries bt709 -color_trc bt709 \
  -c:a aac -b:a 192k -ar 48000 -movflags +faststart -shortest "$OUT"
echo "done: $OUT"
