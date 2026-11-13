# FutureOS - גרסה, מחזור חיים ותוכנות מותקנות מראש (סעיפים 826, 827, 828, 840, 986)
#   $(call inherit-product, vendor/futureos/config/futureos_lifecycle.mk)
# חשוב: לכלול אחרון ב-device.mk, אחרי כל ה-inherit-product (בגלל filter-out ב-840).

# --- זהות גרסה (986: מסך מידע מערכת; Settings קורא את המאפיינים האלה) ----------
FUTUREOS_VERSION ?= 0.0.0-dev
FUTUREOS_CHANNEL ?= dev        # dev | beta | stable (תואם ל-docs/update-feedback-design.md סעיף 2.4)

PRODUCT_PRODUCT_PROPERTIES += \
    ro.futureos.version=$(FUTUREOS_VERSION) \
    ro.futureos.channel=$(FUTUREOS_CHANNEL)

# מחזור חיים (826-828). שתי השורות הבאות במכוון מסומנות בהערה: תאריך סוף תמיכה הוא התחייבות
# שרק המשתמש יכול לתת (ר' docs/public/support-lifecycle.md). לא מפעילים לפני החלטה.
# PRODUCT_PRODUCT_PROPERTIES += ro.futureos.security_support_until=YYYY-MM-DD
# PRODUCT_PRODUCT_PROPERTIES += ro.futureos.eol_notice_days=180

# --- תוכנות מיותרות (840) ------------------------------------------------------
# AOSP/Lineage מתקינים אפליקציות שיש להן תחליף ב-FutureOS (aosp-port-plan סעיף 3ה).
# אין PRODUCT_PACKAGES_REMOVE ב-AOSP 12, לכן מסננים מהרשימה אחרי כל ה-inherit-product.
# השמות הם שמות מודולים (LOCAL_MODULE) ולא חבילות. לאמת מול העץ: m nothing ואז
#   build/soong/scripts/... או: grep -r "LOCAL_MODULE := <name>" packages/apps
FUTUREOS_REMOVED_MODULES := \
    Launcher3 Launcher3QuickStep Trebuchet TrebuchetQuickStep \
    Dialer Contacts Messaging messaging Camera2 Snap Gallery2 Calculator \
    DeskClock Music Calendar Etar Browser2 Jelly Updater Recorder \
    LineageParts ExactCalculator SimpleGallery WallpaperPicker \
    WallpaperPicker2 Eleven AudioFX

PRODUCT_PACKAGES := $(filter-out $(FUTUREOS_REMOVED_MODULES),$(PRODUCT_PACKAGES))

# הרשימה ה"מותרת" נשמרת ב-STATUS/README ומאומתת בעזרת tools/verify_updates_dev.sh:
# מפרט את כל החבילות ב-/system ו-/product ומשווה לרשימת ה-allowlist.
