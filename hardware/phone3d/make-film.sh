#!/usr/bin/env bash
# The 9 s teardown film: the closed phone turns once, comes apart one layer per second (back shell, battery, boards, LCD, front, keys), and snaps back.
# Timed to the reel's music: the segment from 12.0 s (the first drop, 120 BPM) is muxed in, so every step lands on a beat.
#   ./make-film.sh                 1080x1920, 24 fps, 12 samples per frame (about 20 s per frame on 4 cores)
#   SAMPLES=6 SCALE=0.5 ./make-film.sh   a quick draft
set -euo pipefail
cd "$(dirname "$0")"
FRAMES="${FRAMES:-${TMPDIR:-/tmp}/phone3d-frames}"
SAMPLES="${SAMPLES:-12}"
SCALE="${SCALE:-1.0}"
FF="$(python3 -c 'import imageio_ffmpeg; print(imageio_ffmpeg.get_ffmpeg_exe())')"
mkdir -p "$FRAMES" out
N=216
have=$(ls "$FRAMES" | wc -l)
if [ "$have" -lt "$N" ]; then                 # resumable: continue after the last finished frame
  python3 render_scene.py video teardown "$FRAMES" --samples "$SAMPLES" --scale "$SCALE" --from "$have" 2>&1 | grep -E "^frame|Traceback|Error:"
fi
"$FF" -y -framerate 24 -i "$FRAMES/f_%05d.png" -ss 12 -t 9 -i ../../reel-press/out/futureos-press-reel.mp4 \
  -map 0:v -map 1:a -af "afade=t=out:st=8.5:d=0.5" \
  -vf "scale=out_color_matrix=bt709:out_range=tv,format=yuv420p" -r 24 \
  -c:v libx264 -preset slow -crf 16 -profile:v high -colorspace bt709 -color_primaries bt709 -color_trc bt709 \
  -c:a aac -b:a 192k -movflags +faststart -shortest out/futureos_regular_teardown.mp4
echo "done: out/futureos_regular_teardown.mp4"
