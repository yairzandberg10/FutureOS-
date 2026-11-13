# מדיניות ליבת הפלטפורמה של FutureOS

מסמך זה מכסה את סעיפים 120, 160, 200, 236, 239, 271. הכול מבוסס Android 12 (LineageOS 19.1) על MT6768 עם 4GB RAM.

## מדיניות תזמון (120)
- מתזמן CFS הוגן של הקרנל, עדיפויות לפי oom_adj ו-cpuctl של אנדרואיד.
- cpuset: top-app ו-foreground על כל 8 הליבות, system-background על ליבות 0-5 (A55), background על 0-3. ראו init.futureos_core.vendor.rc.
- ה-governor ו-DVFS נשארים בידי PerfService של MediaTek. אין לדרוס בלי מדידה.

## מדדי זיכרון (160)
- RSS: זיכרון פיזי של התהליך. PSS: RSS עם חלוקה הוגנת של דפים משותפים. זה המדד הנכון לצריכת אפליקציה.
- בדיקה: adb shell dumpsys meminfo, ו-/proc/PID/smaps_rollup.
- לחץ זיכרון: /proc/pressure/memory (PSI). lmkd מחליט על הריגה לפי PSI ו-swap פנוי (ro.lmk.* ב-platform_core.mk).
- zram: חצי מה-RAM, דחיסת lz4, swappiness 100.

## IPC (200)
- Binder: ממשק IPC מרכזי בין אפליקציות ושירותי מערכת. ההרשאות נאכפות לפי UID ו-SELinux.
- Unix domain sockets: ערוצי init ו-netd. Shared memory: ashmem או memfd. Futex: סנכרון ברמת bionic.
- מקור מלא: https://source.android.com/docs/core/architecture/hidl/binder-ipc

## מבנה תיקיות (239)
- /system, /system_ext, /product, /vendor: קריאה בלבד (dynamic partitions).
- /data: נתוני משתמש ואפליקציות, מוצפן FBE. /data/local/tmp מנוקה אחרי 7 ימים.
- /sdcard: אחסון משותף בנתיב FUSE. כרטיס SD ו-USB מותקנים תחת /storage/UUID.

## סל מחזור וגודל תיקיות (236, 271)
אלה יכולות של אפליקציית Files ולא של ה-overlay. MediaStore מספק trash ל-30 יום לתמונות, וידאו ואודיו בלבד. לקבצים אחרים נדרש סל משלה באפליקציה. גודל לפי תיקייה דורש סריקה באפליקציה.
