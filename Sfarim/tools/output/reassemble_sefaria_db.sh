#!/usr/bin/env bash
# GitHub blocks files over 100MB, so sefaria.db ships split into
# sefaria_db_parts/sefaria.db.part_NNNN. Run this once after cloning
# to reconstruct tools/output/sefaria.db from those parts.
set -euo pipefail
cd "$(dirname "$0")"
cat sefaria_db_parts/sefaria.db.part_* > sefaria.db
echo "Reassembled sefaria.db ($(du -h sefaria.db | cut -f1))"
