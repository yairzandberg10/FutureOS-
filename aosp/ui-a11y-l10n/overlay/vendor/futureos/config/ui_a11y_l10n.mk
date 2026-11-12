# FutureOS: קטגוריות 16-20 (תצוגה, ממשק, נגישות, עברית/RTL, שמע).
# הטמעה: ב-vendor/futureos/config/futureos.mk להוסיף:
#   $(call inherit-product, vendor/futureos/config/ui_a11y_l10n.mk)

# --- 19: לוקליזציה. הראשון ברשימה הוא ברירת המחדל (ro.product.locale) ---
PRODUCT_LOCALES := he_IL en_US ar_EG ru_RU

# 739/740 אזור זמן ברירת מחדל. הנתונים עצמם מגיעים מ-APEX com.android.tzdata (עדכון דרך mainline).
PRODUCT_SYSTEM_DEFAULT_PROPERTIES += \
    persist.sys.timezone=Asia/Jerusalem \
    persist.sys.language=he \
    persist.sys.country=IL

# 668/669/16: ממשק. אין hwui debug, ואין blur (מסך 640x960 בלי GPU חזק).
PRODUCT_PROPERTY_OVERRIDES += \
    ro.surface_flinger.supports_background_blur=0 \
    ro.sf.blurs_are_expensive=1

# --- RRO ---
PRODUCT_PACKAGES += \
    FutureOSFrameworkOverlay \
    FutureOSSettingsProviderOverlay

# --- 734: גופן Heebo (גופן הדיזיין סיסטם). מוסיפים את הקבצים ל-fonts/ ראו README ---
# PRODUCT_COPY_FILES += \
#     vendor/futureos/fonts/fonts_customization.xml:$(TARGET_COPY_OUT_PRODUCT)/etc/fonts_customization.xml \
#     vendor/futureos/fonts/Heebo-Regular.ttf:$(TARGET_COPY_OUT_PRODUCT)/fonts/Heebo-Regular.ttf \
#     vendor/futureos/fonts/Heebo-Bold.ttf:$(TARGET_COPY_OUT_PRODUCT)/fonts/Heebo-Bold.ttf
