# FutureOS platform-core: product makefile (LineageOS 19.1 / Android 12, MT6768).
# הטמעה: ב-device.mk:   $(call inherit-product, device/qin/f22pro/platform_core.mk)
#        ב-BoardConfig.mk: include device/qin/f22pro/BoardConfigPlatformCore.mk
# qin/f22pro הוא placeholder: להחליף לשם האמיתי של ה-device tree.

PC_PATH := device/qin/f22pro

# overlays: framework-res, SettingsProvider, Tethering, Wifi
DEVICE_PACKAGE_OVERLAYS += $(PC_PATH)/overlay

# זיכרון: 4GB, 320dpi (סעיפים 121-160)
$(call inherit-product, frameworks/native/build/phone-xhdpi-4096-dalvik-heap.mk)

# features (288, 304, 306, 344, 360). כפילות זהה ל-device.mk קיים אינה מזיקה.
# בכוונה אין android.hardware.nfc (אין NFC בחומרה).
PC_FEAT := frameworks/native/data/etc
PC_PERM := $(TARGET_COPY_OUT_VENDOR)/etc/permissions
PRODUCT_COPY_FILES += \
    $(PC_FEAT)/android.hardware.bluetooth.xml:$(PC_PERM)/android.hardware.bluetooth.xml \
    $(PC_FEAT)/android.hardware.bluetooth_le.xml:$(PC_PERM)/android.hardware.bluetooth_le.xml \
    $(PC_FEAT)/android.hardware.wifi.xml:$(PC_PERM)/android.hardware.wifi.xml \
    $(PC_FEAT)/android.hardware.wifi.direct.xml:$(PC_PERM)/android.hardware.wifi.direct.xml \
    $(PC_FEAT)/android.hardware.usb.host.xml:$(PC_PERM)/android.hardware.usb.host.xml \
    $(PC_FEAT)/android.hardware.usb.accessory.xml:$(PC_PERM)/android.hardware.usb.accessory.xml \
    $(PC_FEAT)/android.hardware.location.gps.xml:$(PC_PERM)/android.hardware.location.gps.xml \
    $(PC_FEAT)/android.hardware.sensor.accelerometer.xml:$(PC_PERM)/android.hardware.sensor.accelerometer.xml \
    $(PC_FEAT)/android.hardware.sensor.gyroscope.xml:$(PC_PERM)/android.hardware.sensor.gyroscope.xml \
    $(PC_FEAT)/android.hardware.sensor.light.xml:$(PC_PERM)/android.hardware.sensor.light.xml \
    $(PC_FEAT)/android.hardware.sensor.proximity.xml:$(PC_PERM)/android.hardware.sensor.proximity.xml \
    $(PC_FEAT)/android.hardware.sensor.stepcounter.xml:$(PC_PERM)/android.hardware.sensor.stepcounter.xml \
    $(PC_FEAT)/android.hardware.sensor.stepdetector.xml:$(PC_PERM)/android.hardware.sensor.stepdetector.xml

# init: vendor (קרנל, זיכרון, cpuset) ו-system_ext (ברירות מחדל ראשונות, ניקוי tmp)
PRODUCT_COPY_FILES += \
    $(PC_PATH)/rootdir/etc/init/init.futureos_core.vendor.rc:$(TARGET_COPY_OUT_VENDOR)/etc/init/init.futureos_core.rc \
    $(PC_PATH)/rootdir/etc/init/init.futureos_core.system.rc:$(TARGET_COPY_OUT_SYSTEM_EXT)/etc/init/init.futureos_core.rc \
    $(PC_PATH)/rootdir/bin/futureos_core_job.sh:$(TARGET_COPY_OUT_SYSTEM_EXT)/bin/futureos_core_job.sh

# sepolicy: נקודת התחלה, דורשת audit2allow במכשיר
BOARD_VENDOR_SEPOLICY_DIRS += $(PC_PATH)/sepolicy/vendor
SYSTEM_EXT_PRIVATE_SEPOLICY_DIRS += $(PC_PATH)/sepolicy/system_ext/private

# fsck / mkfs (220, 224). חבילה שלא קיימת בעץ: להסיר מהשורה.
PRODUCT_PACKAGES += e2fsck fsck.f2fs mkfs.f2fs fsck.exfat mkfs.exfat

# מאפייני system
PRODUCT_SYSTEM_DEFAULT_PROPERTIES += \
    ro.config.low_ram=false \
    net.tethering.noprovisioning=true \
    ro.lmk.use_psi=true \
    ro.lmk.low=1001 \
    ro.lmk.medium=800 \
    ro.lmk.critical=0 \
    ro.lmk.critical_upgrade=false \
    ro.lmk.upgrade_pressure=100 \
    ro.lmk.downgrade_pressure=100 \
    ro.lmk.kill_heaviest_task=true \
    ro.lmk.psi_partial_stall_ms=70 \
    ro.lmk.psi_complete_stall_ms=700 \
    ro.lmk.thrashing_limit=30 \
    ro.lmk.thrashing_limit_decay=50 \
    ro.lmk.swap_free_low_percentage=10 \
    persist.device_config.activity_manager_native_boot.use_freezer=true
# use_freezer דורש אימות במכשיר: adb shell dumpsys activity settings | grep -i freezer

# אופציונלי וכבוי (276): כרטיס SD כאחסון פנימי מוצפן. החלטת מוצר.
# PRODUCT_SYSTEM_DEFAULT_PROPERTIES += persist.fw.force_adoptable=true

PRODUCT_VENDOR_PROPERTIES += \
    ro.vold.primary_physical=false
