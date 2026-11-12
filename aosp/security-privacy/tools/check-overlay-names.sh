#!/usr/bin/env bash
# Verify that every resource name overridden by the overlay exists in the synced AOSP tree.
# Usage: check-overlay-names.sh <AOSP_ROOT> [overlay_root]
# Read-only. Run before the first build; a missing name makes aapt2 fail.
set -u
AOSP="${1:?usage: $0 <AOSP_ROOT> [overlay_root]}"
OV="${2:-$(cd "$(dirname "$0")/.." && pwd)/overlay/device/futureos/qin/overlay}"
bad=0
while IFS= read -r f; do
  rel="${f#$OV/}"
  target="$AOSP/$rel"
  if [ ! -f "$target" ]; then
    echo "MISSING FILE  $rel (module not in tree or built as APEX/prebuilt: use an RRO)"; bad=1; continue
  fi
  for n in $(grep -o 'name="[^"]*"' "$f" | sed 's/name="\(.*\)"/\1/'); do
    if grep -q "name=\"$n\"" "$target"; then echo "ok            $rel :: $n"
    else echo "MISSING NAME  $rel :: $n"; bad=1; fi
  done
done < <(find "$OV" -name '*.xml')
exit $bad
