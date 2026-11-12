# FutureOS - security & privacy product overlay (checklist categories 10-13)
# Include from the device makefile:
#   $(call inherit-product, device/futureos/qin/futureos_security.mk)
# Not built or tested - the AOSP tree is not synced. See ../../../STATUS.md.

LOCAL_FUTUREOS_SEC := device/futureos/qin

# Resource overlays: clipboard toast (398/500), device name (509),
# Wi-Fi MAC randomization (499), Wi-Fi scan default (491/520).
DEVICE_PACKAGE_OVERLAYS += $(LOCAL_FUTUREOS_SEC)/overlay

# SELinux domain, file labels and property labels for the privacy service.
SYSTEM_EXT_PRIVATE_SEPOLICY_DIRS += $(LOCAL_FUTUREOS_SEC)/sepolicy/system_ext/private

# Init rc + first-boot privacy defaults script + shipped user docs.
PRODUCT_PACKAGES += \
    init.futureos.security.rc \
    futureos-privacy.sh \
    futureos_privacy_policy_he \
    futureos_data_collection_he \
    futureos_crypto_policy_he

# Contract properties for FutureOS apps (482, 484, 503, 514). All default OFF.
#   telemetry     - may the system/apps record usage telemetry at all
#   cloud_consent - user consented to sending data to any cloud service
#   crash_upload  - may crash reports leave the device (needs consent too)
# Any outgoing event must first be written to /data/misc/futureos/outbox (484)
# so the user can view exactly what would be / was sent.
PRODUCT_SYSTEM_EXT_PROPERTIES += \
    persist.sys.futureos.telemetry=0 \
    persist.sys.futureos.cloud_consent=0 \
    persist.sys.futureos.crash_upload=0

# Guard: privacy/security hardening assumes a user build.
ifneq ($(TARGET_BUILD_VARIANT),user)
$(warning futureos_security: TARGET_BUILD_VARIANT=$(TARGET_BUILD_VARIANT); items 362/363/397/512 are only valid for 'user')
endif

# Explicitly NOT included (items 485/486/487/488): no GMS, no ad SDKs, no
# partner/recommended app packages. Do not add them to PRODUCT_PACKAGES.
