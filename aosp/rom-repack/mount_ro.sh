#!/bin/bash
# Mount the stock images read-only at /mnt/fos_{system,product,vendor} (WSL root). Idempotent.
W="${W:-/root/fos-work}"
command -v unzip >/dev/null || apt-get install -y unzip >/dev/null 2>&1
for p in system product vendor; do
  mkdir -p "/mnt/fos_$p"
  img="$W/${p}_a.img"; [ $p = system ] && img="$W/system_a.orig.img"
  mountpoint -q "/mnt/fos_$p" || mount -o loop,ro "$img" "/mnt/fos_$p"
done
mount | grep fos_
