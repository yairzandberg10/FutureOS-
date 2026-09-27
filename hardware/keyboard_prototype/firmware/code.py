# FutureOS Keyboard Prototype - CircuitPython firmware for Seeed XIAO RP2040
#
# 5x5 key matrix (21 keys) -> USB HID keyboard. No extra libraries needed:
# HID reports are sent raw through usb_hid.
#
# Matrix wiring (see ../SPEC.md): COL -> switch -> diode (anode) -> ROW
#   ROW0..ROW4 = D0..D4, COL0..COL4 = D5..D9, backlight enable = D10
#
# On Android the keys arrive as normal keyboard keys. The special keys
# (*, #, Call, End, Options, Back, OK) are sent as F1..F6 / Enter and
# remapped to the FutureOS keycodes by Vendor_1209_Product_0001.kl.

import time

import board
import keypad
import pwmio
import usb_hid

ROWS = (board.D0, board.D1, board.D2, board.D3, board.D4)
COLS = (board.D5, board.D6, board.D7, board.D8, board.D9)

# HID usage IDs (USB HID Usage Tables, Keyboard page 0x07)
K1, K2, K3, K4, K5, K6, K7, K8, K9, K0 = 0x1E, 0x1F, 0x20, 0x21, 0x22, 0x23, 0x24, 0x25, 0x26, 0x27
ENTER = 0x28
F1, F2, F3, F4, F5, F6 = 0x3A, 0x3B, 0x3C, 0x3D, 0x3E, 0x3F
RIGHT, LEFT, DOWN, UP = 0x4F, 0x50, 0x51, 0x52

#            COL0  COL1   COL2  COL3  COL4
KEYMAP = (
    (K1,   K2,    K3,   F3,   LEFT),   # ROW0: 1 2 3 CALL LEFT
    (K4,   K5,    K6,   F4,   RIGHT),  # ROW1: 4 5 6 END  RIGHT
    (K7,   K8,    K9,   UP,   None),   # ROW2: 7 8 9 UP
    (F1,   K0,    F2,   DOWN, None),   # ROW3: * 0 # DOWN
    (F5,   ENTER, F6,   None, None),   # ROW4: SOFT_L OK SOFT_R
)

BACKLIGHT_TIMEOUT = 8.0   # seconds after the last key press
BACKLIGHT_LEVEL = 0.6     # 0..1

matrix = keypad.KeyMatrix(row_pins=ROWS, column_pins=COLS, columns_to_anodes=True)
keyboard = next(d for d in usb_hid.devices if d.usage_page == 0x01 and d.usage == 0x06)
backlight = pwmio.PWMOut(board.D10, frequency=1000, duty_cycle=0)

pressed = []
report = bytearray(8)
last_activity = time.monotonic()


def send():
    report[0] = 0
    for i in range(6):
        report[2 + i] = pressed[i] if i < len(pressed) else 0
    try:
        keyboard.send_report(report)
    except OSError:
        pass  # USB not ready yet


def set_backlight(on):
    backlight.duty_cycle = int(65535 * BACKLIGHT_LEVEL) if on else 0


set_backlight(True)

while True:
    event = matrix.events.get()
    now = time.monotonic()
    if event:
        row, col = divmod(event.key_number, len(COLS))
        code = KEYMAP[row][col]
        if code is not None:
            if event.pressed and code not in pressed:
                pressed.append(code)
            elif event.released and code in pressed:
                pressed.remove(code)
            send()
        last_activity = now
        set_backlight(True)
    elif now - last_activity > BACKLIGHT_TIMEOUT:
        set_backlight(False)
    time.sleep(0.002)
