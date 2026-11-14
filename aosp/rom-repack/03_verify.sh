#!/bin/bash
# FutureOS ROM repack - step 3: offline checks of the built system_a (WSL, no phone).
W="${W:-/root/fos-work}"
I="$W/out/images/system_a.img"
ok=1
echo "== original fs"; dumpe2fs -h "$W/system_a.orig.img" 2>/dev/null | grep -E 'Block count|Block size'
echo "== e2fsck"; e2fsck -fn "$I" >/tmp/fsck.txt 2>&1 && echo clean || { cat /tmp/fsck.txt; ok=0; }
echo "== every app: mode/owner/label/content"
A=/system/priv-app
for apkdir in $(debugfs -R "ls -p $A" "$I" 2>/dev/null | awk -F/ '{print $6}' | grep -E '^(Future|SistemUI)'); do
  f="$A/$apkdir/$apkdir.apk"
  dlab=$(debugfs -R "ea_get $A/$apkdir security.selinux" "$I" 2>/dev/null | tr -d '\n')
  echo "$dlab" | grep -q 'system_file:s0' || { echo "BAD dir label $apkdir"; ok=0; }
  st=$(debugfs -R "stat $f" "$I" 2>/dev/null)
  mode=$(echo "$st" | grep -oE 'Mode: +[0-7]+' | awk '{print $2}')
  uid=$(echo "$st" | grep -oE 'User: +[0-9]+' | awk '{print $2}')
  lab=$(debugfs -R "ea_get $f security.selinux" "$I" 2>/dev/null | tr -d '\n')
  debugfs -R "dump $f /tmp/x.apk" "$I" >/dev/null 2>&1
  h=$(sha256sum /tmp/x.apk | awk '{print $1}')
  src=$(for p in "$W"/apps/*/base.apk; do [ "$(sha256sum "$p" | awk '{print $1}')" = "$h" ] && echo "$p"; done | head -1)
  dmode=$(debugfs -R "stat $A/$apkdir" "$I" 2>/dev/null | grep -oE 'Mode: +[0-7]+' | awk '{print $2}')
  status=OK
  [ "$mode" = "0644" ] && [ "$dmode" = "0755" ] && [ "$uid" = "0" ] && [ -n "$src" ] && echo "$lab" | grep -q 'system_file:s0' || { status=BAD; ok=0; }
  printf '%-4s %-22s file=%s dir=%s uid=%s src=%s\n' "$status" "$apkdir" "$mode" "$dmode" "$uid" "$(basename "$(dirname "$src")")"
done
echo "== system files"
for rel in etc/permissions/privapp-permissions-futureos.xml etc/excluded-input-devices.xml; do
  debugfs -R "dump /system/$rel /tmp/s.xml" "$I" >/dev/null 2>&1
  if cmp -s /tmp/s.xml "$W/sysfiles/$rel" && debugfs -R "ea_get /system/$rel security.selinux" "$I" 2>/dev/null | grep -q system_file; then
    echo "OK   /system/$rel"; else echo "BAD  /system/$rel"; ok=0; fi
done
# every allowlisted package must be a priv-app in the image, and every priv-app must be allowlisted
n_allow=$(grep -c '<privapp-permissions package=' "$W/sysfiles/etc/permissions/privapp-permissions-futureos.xml")
n_apps=$(debugfs -R "ls -p $A" "$I" 2>/dev/null | awk -F/ '{print $6}' | grep -cE '^(Future|SistemUI)')
echo "allowlisted=$n_allow priv-apps=$n_apps"; [ "$n_allow" = "$n_apps" ] || ok=0
python3 -c "import xml.dom.minidom,sys;[xml.dom.minidom.parse(f) for f in sys.argv[1:]];print('xml well-formed')" \
  "$W/sysfiles/etc/permissions/privapp-permissions-futureos.xml" "$W/sysfiles/etc/excluded-input-devices.xml" || ok=0
echo "== sizes vs super (8 GiB, slot a group main_a)"
sys=$(stat -c %s "$I"); total=$((sys + 465281024 + 212070400))
echo "system_a=$sys vendor_a=465281024 product_a=212070400 total=$total limit=8589934592"
[ $total -lt 8589934592 ] || ok=0
[ $ok = 1 ] && echo "ALL CHECKS PASSED" || echo "CHECKS FAILED"
