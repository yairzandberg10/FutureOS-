# FutureOS - build-time guards for security items. Add at the END of
# device/futureos/qin/BoardConfig.mk:
#   -include device/futureos/qin/BoardConfigSecurity.mk

# 365: SELinux must stay enforcing - never permissive on the kernel cmdline.
ifneq (,$(findstring androidboot.selinux=permissive,$(BOARD_KERNEL_CMDLINE)))
$(error BoardConfigSecurity: androidboot.selinux=permissive found in BOARD_KERNEL_CMDLINE (item 365))
endif
ifeq ($(SELINUX_IGNORE_NEVERALLOWS),true)
$(error BoardConfigSecurity: SELINUX_IGNORE_NEVERALLOWS must not be true (items 365/400))
endif

# 382/419/420/421: AVB + dm-verity on. Warning only: the Qin bootloader may stay
# unlocked/orange after the port (see aosp/tools/README.md), which breaks the chain.
ifneq ($(BOARD_AVB_ENABLE),true)
$(warning BoardConfigSecurity: BOARD_AVB_ENABLE is not true (items 382/419/420/421))
endif

# 455/459: file-based encryption is configured in fstab, not here.
# See fstab-encryption.snippet.txt next to this file.
