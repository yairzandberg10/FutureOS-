# FutureOS - גיבוי ושחזור של המערכת (סעיפים 923, 955, 927, 928)
#   $(call inherit-product, vendor/futureos/config/futureos_backup.mk)
#
# מה AOSP נותן בלי GMS:
#   * BackupManagerService + SettingsBackupAgent (גיבוי הגדרות המערכת, 955) קיימים ב-framework.
#   * LocalTransport: transport מקומי, מיועד לבדיקות (לא לשימוש משתמש קצה).
#   * אין transport לענן ואין transport לכרטיס/דיסק חיצוני. זה החלק שחסר (FutureBackup, ר' STATUS.md).
# מה הקובץ הזה עושה: מפעיל את מסלול הגיבוי והשחזור בפריימוורק, כך שכשיש transport (שלנו)
# אפשר לחבר אותו בלי לגעת בשאר המערכת.

PRODUCT_PACKAGES += \
    LocalTransport \
    BackupRestoreConfirmation

# ברירת מחדל: גיבוי מופעל (def_backup_enabled) ו-transport ריק (def_backup_transport) עד שיש FutureBackup,
# כדי שלא ייווצר אשליה של גיבוי שבפועל לא נשמר באף מקום. ר' overlay-src/.../SettingsProvider/res/values/defaults.xml
PRODUCT_PACKAGE_OVERLAYS += vendor/futureos/overlay-src

# כללי גיבוי לאפליקציות FutureOS (תבנית להעתקה לכל אפליקציה): apps-template/res/xml/
