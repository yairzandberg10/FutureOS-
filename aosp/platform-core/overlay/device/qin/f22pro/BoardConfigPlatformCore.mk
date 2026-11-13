# FutureOS platform-core: BoardConfig. לכלול מתוך BoardConfig.mk של הטלפון.
# קרנל: למזג kernel/futureos_core.fragment ל-defconfig:
#   scripts/kconfig/merge_config.sh -m arch/arm64/configs/DEFCONFIG device/qin/f22pro/kernel/futureos_core.fragment
# עם קרנל prebuilt אי אפשר להחיל, והסעיפים נשארים אימות דרך /proc/config.gz.

# AVB (7, 8, 420): פעיל רק כשיש מפתח חתימה משלנו. בלי Root of Trust ב-preloader, mtkclient עוקף.
ifneq ($(FUTUREOS_AVB_KEY),)
BOARD_AVB_ENABLE := true
BOARD_AVB_ALGORITHM := SHA256_RSA4096
BOARD_AVB_KEY_PATH := $(FUTUREOS_AVB_KEY)
BOARD_AVB_ROLLBACK_INDEX := 0
endif

# שליטה בטעינה (568): Lineage health charging-control HAL. למלא רק אחרי שמוצאים node כתיב ב-sysfs.
# לאמת את שמות המשתנים מול hardware/lineage/interfaces/health בעץ.
ifneq ($(FUTUREOS_CHARGE_CTL_PATH),)
TARGET_HEALTH_CHARGING_CONTROL_CHARGING_PATH := $(FUTUREOS_CHARGE_CTL_PATH)
TARGET_HEALTH_CHARGING_CONTROL_CHARGING_ENABLED := 1
TARGET_HEALTH_CHARGING_CONTROL_CHARGING_DISABLED := 0
endif
# מועמדים ב-MediaTek: input_suspend, batt_slate_mode, charging_enabled, /proc/mtk_battery_cmd/current_cmd
# cmdline: לא נוגעים. אם הקרנל תומך אפשר להוסיף init_on_alloc=1 (149).
