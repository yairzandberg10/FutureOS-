#!/usr/bin/env bash
# FutureOS - צילום מדדי ביצועים ו-soak (961, 970-974, 921, 922)
# קורא בלבד (dumpsys / top / df). שימוש: ./perf_snapshot.sh [outdir]  (ADB=adb כברירת מחדל)
# להשוואת soak (921/922): להריץ בהתחלה, אחרי 24 שעות ואחרי 7 ימים, ולהשוות את הקבצים.
set -u
ADB="${ADB:-adb}"; OUT="${1:-./perf-$(date +%Y%m%d-%H%M%S)}"; mkdir -p "$OUT"
run() { local name="$1"; shift; "$ADB" shell "$@" > "$OUT/$name.txt" 2>&1 || echo "WARN: $name failed" >&2; }

run build        getprop
run uptime       uptime
run cpu_top      top -b -n 1 -m 15            # 970 מדדי מעבד
run cpu_info     dumpsys cpuinfo
run mem_meminfo  dumpsys meminfo              # 971 זיכרון + 922 דליפות (להשוות PSS של system_server ו-FutureUI בין צילומים)
run mem_proc     cat /proc/meminfo
run disk_df      df -h                         # 972 דיסק
run disk_stats   dumpsys diskstats
run net_stats    dumpsys netstats             # 973 רשת
run net_conn     dumpsys connectivity
run gpu_sf       dumpsys SurfaceFlinger        # 974 GPU (אין API אחיד ל-Mali; gfxinfo לכל אפליקציה)
for pkg in com.future.futureui com.future.futurelauncher com.future.settings; do
  run "gfx_$pkg" dumpsys gfxinfo "$pkg"
done
run battery      dumpsys battery
run thermal      dumpsys thermalservice
run dropbox      dumpsys dropbox
run logd_stats   logcat -g

echo "saved to $OUT"
