"""FutureOS Keyboard Prototype - single source of truth for the design.

Board coordinates: mm, origin = board bottom-left seen from the FRONT, Y up
(toward the screen). Values come from hardware/openscad/futureos_family.scad
with MODEL=regular (board bottom edge = 3 mm above the body bottom).
See ../SPEC.md for the approved spec.
"""
import os

KICAD = r"C:/Users/yairz/AppData/Local/Programs/KiCad/10.0"
SYMS = KICAD + "/share/kicad/symbols/"
FPS = KICAD + "/share/kicad/footprints/"
HERE = os.path.dirname(os.path.abspath(__file__))
PROJ_DIR = os.path.dirname(HERE)
PROJECT = "keyboard_prototype"
LOCAL_FP_LIB = "keyboard_prototype"  # project-local .pretty (XIAO footprint from Seeed)

# --- mechanics (from futureos_family.scad, MODEL=regular) -------------------
BOARD_W, BOARD_H, BOARD_R = 56.0, 66.0, 3.0
BOARD_T = 1.0
KEY_P, ROW_P = 17.0, 9.5          # column / digit-row pitch
DPAD_R = 7.0                      # D-pad switch radius from OK
CX = BOARD_W / 2
NAV_Y = 50.0                      # D-pad centre
HOLES = [(2.8, 2.5), (53.2, 2.5), (3.5, 62.5), (52.5, 62.5)]

# --- keys: (label, x, y, rot, row, col, android keycode) --------------------
_digit_rows = [("1", "2", "3"), ("4", "5", "6"), ("7", "8", "9"), ("*", "0", "#")]
_digit_y = [35.25, 25.75, 16.25, 6.75]
_codes = {"*": "STAR", "#": "POUND"}

KEYS = []
for r, (labels, y) in enumerate(zip(_digit_rows, _digit_y)):
    for c, lab in enumerate(labels):
        KEYS.append((lab, CX + (c - 1) * KEY_P, y, 0, r, c, "KEYCODE_" + _codes.get(lab, lab)))
KEYS += [
    ("SOFT_L", CX - KEY_P, 55.0, 0, 4, 0, "KEYCODE_MENU"),
    ("SOFT_R", CX + KEY_P, 55.0, 0, 4, 2, "KEYCODE_BACK"),
    ("CALL",   CX - KEY_P, 45.0, 0, 0, 3, "KEYCODE_CALL"),
    ("END",    CX + KEY_P, 45.0, 0, 1, 3, "KEYCODE_ENDCALL"),
    ("UP",     CX, NAV_Y + DPAD_R, 0, 2, 3, "KEYCODE_DPAD_UP"),
    ("DOWN",   CX, NAV_Y - DPAD_R, 0, 3, 3, "KEYCODE_DPAD_DOWN"),
    ("LEFT",   CX - DPAD_R, NAV_Y, 90, 0, 4, "KEYCODE_DPAD_LEFT"),
    ("RIGHT",  CX + DPAD_R, NAV_Y, 90, 1, 4, "KEYCODE_DPAD_RIGHT"),
    ("OK",     CX, NAV_Y, 0, 4, 1, "KEYCODE_DPAD_CENTER"),
]
assert len(KEYS) == 21
assert len({(k[4], k[5]) for k in KEYS}) == 21, "matrix position used twice"

# --- XIAO RP2040 pin map (pad numbers from Seeed XIAO-RP2040-SMD footprint) --
# pad: (XIAO name, net)
XIAO_PINS = {
    "1": ("D0", "ROW0"), "2": ("D1", "ROW1"), "3": ("D2", "ROW2"), "4": ("D3", "ROW3"),
    "5": ("D4", "ROW4"), "6": ("D5", "COL0"), "7": ("D6", "COL1"), "8": ("D7", "COL2"),
    "9": ("D8", "COL3"), "10": ("D9", "COL4"), "11": ("D10", "BL_EN"),
    "12": ("3V3_OUT", None), "13": ("GND", "GND"), "14": ("VBUS", "VBUS"),
    # pads under the module (SWD / battery) - not used, left unsoldered
    "15": ("SWDIO", None), "16": ("SWCLK", None), "17": ("EN", None),
    "18": ("GND", None), "19": ("VIN", None), "20": ("GND", None),
}
RP2040_GPIO = {"D0": 26, "D1": 27, "D2": 28, "D3": 29, "D4": 6, "D5": 7,
               "D6": 0, "D7": 1, "D8": 2, "D9": 4, "D10": 3}

# --- FPC J1 pinout (our definition; mainboard side TBD) ----------------------
FPC_PINS = ["GND", "ROW0", "ROW1", "ROW2", "ROW3", "ROW4",
            "COL0", "COL1", "COL2", "COL3", "COL4", "BL_EN", "LED_PWR", "GND"]

N_LEDS = 8
LED_R = "1k"
