#!/bin/bash
# FutureOS ROM repack - step 4a (WSL root, no phone): map every APK/APEX in system/product/vendor to its
# signing certificate, so we know exactly what the Duoqin platform key signs.
#   wsl -d Ubuntu -u root -- bash 04_scan_certs.sh
set -uo pipefail
W="${W:-/root/fos-work}"
OUT="$W/certs"; mkdir -p "$OUT"
for p in system product vendor; do mkdir -p "/mnt/fos_$p"; mountpoint -q "/mnt/fos_$p" || true; done
mountpoint -q /mnt/fos_system  || mount -o loop,ro "$W/system_a.orig.img" /mnt/fos_system
mountpoint -q /mnt/fos_product || mount -o loop,ro "$W/product_a.img"     /mnt/fos_product
mountpoint -q /mnt/fos_vendor  || mount -o loop,ro "$W/vendor_a.img"      /mnt/fos_vendor

TSV="$OUT/apk-certs.tsv"; : > "$TSV"
find /mnt/fos_system /mnt/fos_product /mnt/fos_vendor -type f \( -name '*.apk' -o -name '*.apex' -o -name '*.capex' \) | sort |
while read -r f; do
  case "$f" in
    *.apk)
      c=$(java -jar "$W/apksigner.jar" verify --print-certs "$f" 2>/dev/null </dev/null | grep -m1 'certificate SHA-256 digest' | awk '{print $NF}')
      dn=$(java -jar "$W/apksigner.jar" verify --print-certs "$f" 2>/dev/null </dev/null | grep -m1 'certificate DN' | sed 's/.*DN: //')
      su=$(unzip -p "$f" AndroidManifest.xml 2>/dev/null | tr -d '\0' | grep -ao 'android\.uid\.[a-z]*' | head -1)
      printf '%s\t%s\t%s\t%s\n' "${c:-UNSIGNED}" "$f" "${su:--}" "$dn" >> "$TSV" ;;
    *) printf '%s\t%s\t%s\t%s\n' "APEX" "$f" "-" "-" >> "$TSV" ;;
  esac
done
echo "== certificates by count"
cut -f1,4 "$TSV" | sort | uniq -c | sort -rn
PLAT=$(java -jar "$W/apksigner.jar" verify --print-certs /mnt/fos_system/system/framework/framework-res.apk </dev/null | grep -m1 'SHA-256' | awk '{print $NF}')
echo "== platform cert (framework-res): $PLAT"
awk -F'\t' -v p="$PLAT" '$1==p{print $2"\t"$3}' "$TSV" > "$OUT/platform-signed.txt"
echo "platform-signed APKs: $(wc -l < "$OUT/platform-signed.txt")"
echo "== mac_permissions files"
find /mnt/fos_system /mnt/fos_product /mnt/fos_vendor -name '*mac_permissions*.xml' | while read -r m; do
  echo "$m: $(grep -o 'signature="[0-9a-f]*"' "$m" | sort -u | while read -r s; do echo -n "${s:11:12}… "; done)"
done
echo "== apex payload types"; ls -la /mnt/fos_system/system/apex | head -50
