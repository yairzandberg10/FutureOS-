#!/usr/bin/env bash
# Exports every solid of the FutureOS Regular (shell, keys, glass, the whole board stack) as its own STL into stl/.
#   ./export-parts.sh            all parts, 4 in parallel
#   MODEL=pro ./export-parts.sh  another model of the family (files are named <model>_*.stl)
# Needs OpenSCAD 2021.01+ (apt-get install openscad). Icons of the key legends need Windows' "Segoe MDL2 Assets",
# so the legends are taken from ../print/<model>_markings.stl, not re-exported here.
set -euo pipefail
cd "$(dirname "$0")"
MODEL="${MODEL:-regular}"
mkdir -p stl
export MODEL
jobs=()
for p in front_shell back_shell keypad side_keys glass display_area punch lcd battery pcba side_fpc; do jobs+=("parts.scad:$p"); done
for p in mb_pcb mb_black mb_silver mb_gray mb_dimgray mb_teal mb_darkred mb_cams mb_lens mb_flash mb_als mb_label \
         kb_pcb kb_gold kb_domes kb_leds kb_usb kb_jack kb_spk kb_mic kb_sim batt_body batt_pcm batt_fpc fpcs antenna back_final; do jobs+=("parts_stack.scad:$p"); done
run() {
  f="${1%%:*}"; p="${1##*:}"
  if [ "$f" = "parts.scad" ]; then extra=(-D 'PART="none"' -D "PART_OUT=\"$p\""); else extra=(-D 'PART="none"' -D 'MB_PART="none"' -D "OUT=\"$p\""); fi
  openscad -D "MODEL=\"$MODEL\"" "${extra[@]}" -o "stl/${MODEL}_$p.stl" "$f" > "/tmp/os_${MODEL}_$p.log" 2>&1 \
    && echo "ok   $p" || { echo "FAIL $p"; tail -3 "/tmp/os_${MODEL}_$p.log"; }
}
export -f run; export MODEL
printf '%s\n' "${jobs[@]}" | xargs -P 4 -I{} bash -c 'run {}'
