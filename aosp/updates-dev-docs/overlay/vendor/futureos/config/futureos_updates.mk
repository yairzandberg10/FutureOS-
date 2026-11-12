# FutureOS - עדכוני OTA, חתימה וחבילות (סעיפים 810, 815, 816, 817, 820, 935)
#
# שימוש: ב-device.mk של המכשיר (device/<vendor>/f22pro/device.mk):
#   FUTUREOS_OTA_MODE := vab        # vab | ab | none  (ברירת מחדל: none)
#   $(call inherit-product, vendor/futureos/config/futureos_updates.mk)
#
# FUTUREOS_OTA_MODE נקבע לפי תוצאת M0 (00_device_info.sh):
#   ro.virtual_ab.enabled=true      -> vab   (מועדף: rollback אוטומטי + snapshot, סעיפים 815/816/935)
#   ro.build.ab_update=true בלבד    -> ab
#   אין _a/_b ב-/dev/block/by-name  -> none  (RecoverySystem, בלי rollback אוטומטי)
# עד שיש dump אמיתי הערך הוא none, ושום דבר כאן לא משנה את מבנה המחיצות.

FUTUREOS_OTA_MODE ?= none

ifeq ($(FUTUREOS_OTA_MODE),vab)
  # Virtual A/B: חייב super partition (dynamic partitions) ומקום פנוי ב-/data לצילום (snapshot).
  $(call inherit-product, $(SRC_TARGET_DIR)/product/virtual_ab_ota.mk)
  FUTUREOS_AB := true
endif
ifeq ($(FUTUREOS_OTA_MODE),ab)
  FUTUREOS_AB := true
endif

ifeq ($(FUTUREOS_AB),true)
  # AB_OTA_UPDATER / AB_OTA_PARTITIONS מוגדרים ב-BoardConfigUpdates.mk (הם משתני Board).
  PRODUCT_PACKAGES += \
      update_engine \
      update_verifier \
      otapreopt_script \
      checkpoint_gc

  PRODUCT_PACKAGES_DEBUG += update_engine_client

  # אחרי התקנה ל-slot הלא פעיל: dexopt מראש (otapreopt) כדי שהאתחול הראשון לא יהיה איטי.
  AB_OTA_POSTINSTALL_CONFIG += \
      RUN_POSTINSTALL_system=true \
      POSTINSTALL_PATH_system=system/bin/otapreopt_script \
      FILESYSTEM_TYPE_system=ext4 \
      POSTINSTALL_OPTIONAL_system=true

  # update_verifier מסמן את ה-slot כתקין רק אחרי boot_completed. מכאן ה-"boot_ok" שהשרת מקבל
  # (docs/update-feedback-design.md סעיף 2.2).
  PRODUCT_PRODUCT_PROPERTIES += ro.futureos.ota.mode=$(FUTUREOS_OTA_MODE)
else
  # non-A/B: אין rollback אוטומטי. FutureUpdater משתמש ב-RecoverySystem.installPackage.
  PRODUCT_PRODUCT_PROPERTIES += ro.futureos.ota.mode=none
endif

# --- חתימה (810) -------------------------------------------------------------
# מפתחות האמת אף פעם לא בעץ. בבנייה רגילה נשארים test-keys. החתימה נעשית אחרי הבנייה
# עם tools/make_release.sh (sign_target_files_apks + ota_from_target_files).
# אם FUTUREOS_KEYS_DIR מוגדר (מחשב החתימה בלבד), הבנייה משתמשת בו ישירות:
ifneq ($(FUTUREOS_KEYS_DIR),)
  PRODUCT_DEFAULT_DEV_CERTIFICATE := $(FUTUREOS_KEYS_DIR)/releasekey
  PRODUCT_OTA_PUBLIC_KEYS := $(FUTUREOS_KEYS_DIR)/releasekey.x509.pem
endif

# הרשאות privileged ל-FutureUpdater (האפליקציה עצמה מחוץ ל-overlay הזה)
PRODUCT_COPY_FILES += \
    vendor/futureos/permissions/privapp-permissions-futureos-updater.xml:$(TARGET_COPY_OUT_SYSTEM_EXT)/etc/permissions/privapp-permissions-futureos-updater.xml

# הערות גרסה שמוצגות במכשיר (825)
PRODUCT_COPY_FILES += \
    vendor/futureos/release/release_notes_he.txt:$(TARGET_COPY_OUT_PRODUCT)/etc/futureos/release_notes_he.txt
