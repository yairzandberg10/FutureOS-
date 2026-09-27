#!/usr/bin/env bash
# FutureOS Keyboard Prototype - full rebuild: schematic -> ERC -> PCB -> DRC -> fab files.
# Edit tools/design.py, then run: ./build.sh
set -euo pipefail
cd "$(dirname "$0")"
KICAD_BIN="${KICAD_BIN:-C:/Users/$USERNAME/AppData/Local/Programs/KiCad/10.0/bin}"
PY="$KICAD_BIN/python.exe"
CLI="$KICAD_BIN/kicad-cli.exe"
P=keyboard_prototype

(cd tools && "$PY" gen_sch.py)
"$CLI" sch erc --exit-code-violations -o erc.rpt $P.kicad_sch
"$CLI" sch export netlist -o $P.net $P.kicad_sch
"$CLI" sch export pdf -o ${P}_schematic.pdf $P.kicad_sch

if [ "${SKIP_ROUTE:-0}" != 1 ]; then (cd tools && "$PY" -u gen_pcb.py); fi
"$CLI" pcb drc --schematic-parity --severity-all -o drc.rpt $P.kicad_pcb
"$CLI" pcb drc --schematic-parity --severity-error --exit-code-violations -o drc_errors.rpt $P.kicad_pcb

rm -rf fab && mkdir -p fab/gerbers
"$CLI" pcb export gerbers -o fab/gerbers/ \
  -l "F.Cu,B.Cu,F.Paste,B.Paste,F.SilkS,B.SilkS,F.Mask,B.Mask,Edge.Cuts" \
  --use-drill-file-origin --subtract-soldermask $P.kicad_pcb
"$CLI" pcb export drill -o fab/gerbers/ --format excellon --excellon-separate-th \
  --generate-map --map-format gerberx2 $P.kicad_pcb
"$CLI" pcb export pos -o fab/pos_raw.csv --format csv --units mm --side both $P.kicad_pcb
(cd tools && "$PY" make_fab.py)
(cd fab/gerbers && "$PY" -c "import shutil; shutil.make_archive('../${P}_gerbers', 'zip', '.')")

mkdir -p render
"$CLI" pcb export step -o fab/$P.step --subst-models --force $P.kicad_pcb || true
for side in top bottom; do
  "$CLI" pcb render -o render/pcb_$side.png --side $side --width 1600 --height 1800 \
    --quality high --background opaque $P.kicad_pcb
done
"$CLI" pcb render -o render/pcb_3d.png --rotate "-40,0,-25" --width 1800 --height 1400 \
  --quality high --background opaque --perspective $P.kicad_pcb
echo "done: fab/${P}_gerbers.zip, fab/BOM_JLCPCB.csv, fab/CPL_JLCPCB.csv, render/*.png"
