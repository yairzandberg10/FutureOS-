# FutureOS - יומנים, דוחות קריסה ואבחון (סעיפים 939, 940, 963, 964, 966, 961, 970-974)
#   $(call inherit-product, vendor/futureos/config/futureos_logging.mk)
#
# הערות:
# * logd הוא ring buffer: סבב היומנים (964) בנוי בו. כאן רק מגדילים את הגודל.
# * ברירת המחדל באנדרואיד היא 256KB למאגר (64KB ב-low_ram). במכשיר עם 4GB RAM אפשר 1MB.
# * logpersistd (שמירת logcat לדיסק) עובד רק ב-userdebug/eng, לא ב-user. לכן ב-user build
#   שומרים עקבות דרך DropBox + tombstones + (אם הקרנל תומך) pstore. ר' docs/developer/logging-diagnostics.md

# גודל מאגרי logcat. לשינוי בזמן ריצה (רק eng/userdebug): logcat -G 2M
PRODUCT_SYSTEM_PROPERTIES += \
    ro.logd.size=1M

# מקסימום tombstones שנשמרים ב-/data/tombstones (ברירת מחדל 32).
# 20 מספיק לדוחות קריסה ולא תופס מקום מיותר ב-64GB.
PRODUCT_SYSTEM_PROPERTIES += \
    tombstoned.max_tombstone_count=20

# שליחת דוח קריסה מתבצעת רק באישור המשתמש (סעיף 514), ר' docs/update-feedback-design.md סעיף 3.

# perfetto / traced כבר מופעלים כברירת מחדל ב-user build (persist.traced.enable=1). אין צורך בהגדרה.
# זה הכלי לצפייה במדדי CPU/זיכרון/GPU (970-974).

# DropBox (דוחות קריסה של אפליקציות ושל system_server): ההגדרות הן Settings.Global ולא properties.
# הן נקבעות בהפעלה הראשונה על ידי FutureUI (WRITE_SECURE_SETTINGS), ר' docs/developer/logging-diagnostics.md:
#   dropbox_max_files=500  dropbox_age_seconds=1209600 (14 יום)  dropbox_quota_kb=10240
