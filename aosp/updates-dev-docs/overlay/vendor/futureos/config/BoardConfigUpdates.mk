# FutureOS - משתני Board ל-OTA, AVB ומערכות קבצים (סעיפים 815, 816, 810, 944)
# מצורף מתוך device/<vendor>/f22pro/BoardConfig.mk:
#   include vendor/futureos/config/BoardConfigUpdates.mk
#
# כל הערכים כאן הם שלד. הערכים הסופיים תלויים בתוצאות M0 (דאמפ המחיצות של המכשיר).

FUTUREOS_OTA_MODE ?= none

# --- A/B ---------------------------------------------------------------------
ifneq ($(filter ab vab,$(FUTUREOS_OTA_MODE)),)
  AB_OTA_UPDATER := true
  # הרשימה חייבת להתאים למחיצות האמיתיות ב-super / by-name. לא לנחש: להשלים מ-M0.
  AB_OTA_PARTITIONS := \
      boot \
      system \
      system_ext \
      product \
      vendor \
      vbmeta \
      vbmeta_system
  # TODO(M0): להוסיף dtbo, vendor_boot, odm, lk, preloader אם קיימים ב-scatter ואם ה-OTA אמור לכלול אותם.
endif

ifeq ($(FUTUREOS_OTA_MODE),vab)
  BOARD_SUPER_PARTITION_GROUPS := futureos_dynamic_partitions
  # TODO(M0): BOARD_FUTUREOS_DYNAMIC_PARTITIONS_SIZE = גודל super פחות 4MB
  #           BOARD_FUTUREOS_DYNAMIC_PARTITIONS_PARTITION_LIST := system system_ext product vendor odm
endif

# --- AVB / Verified Boot (810, 816) ------------------------------------------
# המפתח עצמו מוגדר בזמן חתימת ה-release (make_release.sh), לא כאן.
BOARD_AVB_ENABLE := true
BOARD_AVB_ALGORITHM := SHA256_RSA4096

# Rollback index. מעלים אותו רק בגרסה שמתקנת בעיית אבטחה (aosp-port-plan סעיף 4.3).
# מרגע שהעלו, אי אפשר לחזור לבילד הקודם בשום דרך. ההחלטה: המשתמש. עד אז 0.
FUTUREOS_AVB_ROLLBACK_INDEX ?= 0
BOARD_AVB_ROLLBACK_INDEX := $(FUTUREOS_AVB_ROLLBACK_INDEX)
BOARD_AVB_VBMETA_SYSTEM := system product system_ext
BOARD_AVB_VBMETA_SYSTEM_ALGORITHM := SHA256_RSA4096
BOARD_AVB_VBMETA_SYSTEM_ROLLBACK_INDEX := $(FUTUREOS_AVB_ROLLBACK_INDEX)
BOARD_AVB_VBMETA_SYSTEM_ROLLBACK_INDEX_LOCATION := 2

# --- מערכת קבצים ו-journaling (944) -------------------------------------------
# לא משנים כאן את סוג מערכת הקבצים של userdata. הוא נקבע לפי ה-fstab של היצרן (ext4 או f2fs,
# שניהם עם journaling / checkpoint). Virtual A/B דורש checkpoint ב-fstab של userdata:
#   fstab.<soc>:  /data ... fileencryption=...,checkpoint=fs   (f2fs)  או  checkpoint=block (ext4)
# אימות אחרי צריבה: tools/verify_updates_dev.sh בודק את סוג ה-mount.
