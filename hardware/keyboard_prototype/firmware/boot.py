# Runs once at power-up, before USB is set up.
# Fixed USB VID/PID so Android picks up Vendor_1209_Product_0001.kl.
# 0x1209 = pid.codes open-source VID; PID 0x0001 is from its range reserved
# for private testing - replace with an allocated PID before any release.
import supervisor
import usb_hid

supervisor.set_usb_identification("FutureOS", "Keyboard Prototype", 0x1209, 0x0001)
usb_hid.enable((usb_hid.Device.KEYBOARD,))
