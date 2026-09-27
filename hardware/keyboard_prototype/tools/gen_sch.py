"""Generate keyboard_prototype.kicad_sch (+ project + lib tables) from design.py.

Every connection is made with net labels placed exactly on pin end points,
so the netlist is fully determined by design.py.
"""
import os
import shutil
import uuid

from design import *
from sexpr import Sym, dump, find, first, load_symbol, symbol_pins

SEEED_DIR = os.path.join(HERE, "seeed")  # copied from Seeed OPL_Kicad_Library
ROOT_UUID = str(uuid.uuid5(uuid.NAMESPACE_URL, "futureos/keyboard_prototype/root"))

_uid_n = 0


def uid():
    global _uid_n
    _uid_n += 1
    return str(uuid.uuid5(uuid.NAMESPACE_URL, "futureos/kbp/%d" % _uid_n))


S = Sym
LIBS = {
    "Switch:SW_Push": (SYMS + "Switch.kicad_sym", "SW_Push"),
    "Diode:1N4148W": (SYMS + "Diode.kicad_sym", "1N4148W"),
    "Device:LED": (SYMS + "Device.kicad_sym", "LED"),
    "Device:R": (SYMS + "Device.kicad_sym", "R"),
    "Transistor_FET:AO3400A": (SYMS + "Transistor_FET.kicad_sym", "AO3400A"),
    "Jumper:SolderJumper_2_Open": (SYMS + "Jumper.kicad_sym", "SolderJumper_2_Open"),
    "Mechanical:MountingHole": (SYMS + "Mechanical.kicad_sym", "MountingHole"),
    "Connector_Generic_MountingPin:Conn_01x14_MountingPin":
        (SYMS + "Connector_Generic_MountingPin.kicad_sym", "Conn_01x14_MountingPin"),
    "power:GND": (SYMS + "power.kicad_sym", "GND"),
    "power:PWR_FLAG": (SYMS + "power.kicad_sym", "PWR_FLAG"),
    "Seeed_Studio_XIAO_Series:XIAO-RP2040-SMD":
        (os.path.join(SEEED_DIR, "Seeed_Studio_XIAO_Series.kicad_sym"), "XIAO-RP2040-SMD"),
}
_sym_cache = {}


def lib_symbol(lib_id):
    if lib_id not in _sym_cache:
        path, name = LIBS[lib_id]
        node = load_symbol(path, name)
        node = list(node)
        node[1] = lib_id
        _sym_cache[lib_id] = node
    return _sym_cache[lib_id]


def pin_pos(lib_id, x, y, rot, number):
    """Schematic coordinates of a pin end point (lib Y is up, sheet Y is down)."""
    for p in symbol_pins(lib_symbol(lib_id)):
        if p["number"] == number:
            px, py = p["x"], p["y"]
            r = rot % 360
            if r == 90:
                px, py = -py, px
            elif r == 180:
                px, py = -px, -py
            elif r == 270:
                px, py = py, -px
            return round(x + px, 2), round(y - py, 2), p
    raise KeyError((lib_id, number))


def pin_dir(lib_id, rot, number):
    """Direction (sheet coords) pointing AWAY from the symbol body at the pin end."""
    for p in symbol_pins(lib_symbol(lib_id)):
        if p["number"] == number:
            # lib pin angle points from pin end toward the body
            a = (p["rot"] + rot) % 360
            return {0: 180, 90: 270, 180: 0, 270: 90}[a]
    raise KeyError(number)


items = []
counters = {}


def eff(size=1.27, hide=False, justify=None):
    e = [S("effects"), [S("font"), [S("size"), size, size]]]
    if justify:
        e.append([S("justify")] + [S(j) for j in justify.split()])
    if hide:
        e.append([S("hide"), S("yes")])
    return e


def place(lib_id, ref, value, x, y, rot=0, footprint="", fields=None, show_value=True, in_bom=True, side=False):
    sym = lib_symbol(lib_id)
    pins = symbol_pins(sym)
    node = [S("symbol"), [S("lib_id"), lib_id], [S("at"), x, y, rot], [S("unit"), 1],
            [S("exclude_from_sim"), S("no")], [S("in_bom"), S("yes" if in_bom else "no")],
            [S("on_board"), S("yes")], [S("dnp"), S("no")], [S("uuid"), uid()]]
    is_power = ref.startswith("#")
    if side:  # vertical parts: fields to the right of the body
        dx = 5.08 if lib_id.startswith("Transistor") else 2.54
        rx, ry, vx, vy, j = x + dx, y - 1.27, x + dx, y + 1.27, "right" if rot == 90 else "left"
    else:
        rx, ry, vx, vy, j = x, y - 3.81, x, y + 3.81, None
    node.append([S("property"), "Reference", ref, [S("at"), rx, ry, rot if side else 0], eff(hide=is_power, justify=j)])
    node.append([S("property"), "Value", value, [S("at"), vx, vy, rot if side else 0], eff(hide=not show_value, justify=j)])
    node.append([S("property"), "Footprint", footprint, [S("at"), x, y, 0], eff(hide=True)])
    node.append([S("property"), "Datasheet", "", [S("at"), x, y, 0], eff(hide=True)])
    for k, v in (fields or {}).items():
        node.append([S("property"), k, v, [S("at"), x, y, 0], eff(hide=True)])
    for p in {p["number"] for p in pins}:
        node.append([S("pin"), p, [S("uuid"), uid()]])
    node.append([S("instances"), [S("project"), PROJECT,
                 [S("path"), "/" + ROOT_UUID, [S("reference"), ref], [S("unit"), 1]]]])
    items.append(node)
    return node


def next_ref(prefix):
    counters[prefix] = counters.get(prefix, 0) + 1
    return "%s%d" % (prefix, counters[prefix])


def label(net, x, y, direction):
    """direction = where the text goes: 0 right, 90 up, 180 left, 270 down."""
    just = {0: "left bottom", 180: "right bottom", 90: "left bottom", 270: "right bottom"}[direction]
    items.append([S("label"), net, [S("at"), x, y, direction],
                  eff(justify=just), [S("uuid"), uid()]])


def no_connect(x, y):
    items.append([S("no_connect"), [S("at"), x, y], [S("uuid"), uid()]])


def wire(x1, y1, x2, y2):
    items.append([S("wire"), [S("pts"), [S("xy"), x1, y1], [S("xy"), x2, y2]],
                  [S("stroke"), [S("width"), 0], [S("type"), S("default")]], [S("uuid"), uid()]])


def text(s, x, y, size=1.27):
    items.append([S("text"), s, [S("exclude_from_sim"), S("no")], [S("at"), x, y, 0],
                  eff(size=size, justify="left bottom"), [S("uuid"), uid()]])


def gnd(x, y):
    place("power:GND", next_ref("#PWR"), "GND", x, y, 0, show_value=False)


def connect(lib_id, x, y, rot, number, net):
    """Attach a net to a pin: label, GND symbol, or no-connect flag."""
    px, py, _ = pin_pos(lib_id, x, y, rot, number)
    if net is None:
        no_connect(px, py)
    elif net == "GND":
        d = pin_dir(lib_id, rot, number)
        if d == 270:  # pin points down: put the GND symbol right on it
            gnd(px, py)
        elif d in (0, 180):  # sideways GND symbol right on the pin
            place("power:GND", next_ref("#PWR"), "GND", px, py, 90 if d == 0 else 270, show_value=False)
        else:  # pin points up: short wire up, then GND pointing up
            wire(px, py, px, py - 2.54)
            place("power:GND", next_ref("#PWR"), "GND", px, py - 2.54, 180, show_value=False)
    else:
        label(net, px, py, pin_dir(lib_id, rot, number))


def g(v):
    """Snap to the 1.27 mm schematic grid."""
    return round(round(v / 1.27) * 1.27, 2)


# ---------------------------------------------------------------------------
FP_SW = "Button_Switch_SMD:SW_Push_1P1T_XKB_TS-1187A"
FP_D = "Diode_SMD:D_SOD-123"
FP_LED = "LED_SMD:LED_0603_1608Metric"
FP_R = "Resistor_SMD:R_0603_1608Metric"


def build():
    text("FutureOS Keyboard Prototype - key matrix 5x5 (COL -> switch -> diode -> ROW)", g(25.4), g(22.86), 2.0)
    text("Prototype only: XIAO RP2040 is a test controller, NOT part of the FutureOS Regular mainboard.", g(25.4), g(27.94))

    # --- matrix: one cell per key, laid out by row/col ---------------------
    x0, y0, cw, ch = 38.1, 45.72, 45.72, 22.86
    for i, (lab, _, _, _, r, c, code) in enumerate(KEYS, start=1):
        X, Y = g(x0 + c * cw), g(y0 + r * ch)
        sw = "SW%d" % i
        d = "D%d" % i
        place("Switch:SW_Push", sw, "TS-1187A", X, Y, 0, FP_SW,
              {"Key": lab, "Keycode": code})
        DX = g(X + 11.43)
        place("Diode:1N4148W", d, "1N4148W", DX, Y, 180, FP_D)
        # SW pin2 (X+5.08) -> diode anode (DX-3.81)
        wire(X + 5.08, Y, DX - 3.81, Y)
        connect("Switch:SW_Push", X, Y, 0, "1", "COL%d" % c)
        connect("Diode:1N4148W", DX, Y, 180, "1", "ROW%d" % r)
        text("[%s]  R%d C%d" % (lab, r, c), X - 5.08, Y - 6.35)

    # --- XIAO RP2040 (prototype controller, back side) ----------------------
    UX, UY = g(279.4), g(88.9)
    xid = "Seeed_Studio_XIAO_Series:XIAO-RP2040-SMD"
    place(xid, "U1", "XIAO RP2040", UX, UY, 0, LOCAL_FP_LIB + ":XIAO-RP2040-SMD",
          {"Note": "Hand-solder on BACK side. Prototype only."}, in_bom=False)
    for pad, (_, net) in XIAO_PINS.items():
        connect(xid, UX, UY, 0, pad, net)
    text("U1 = XIAO RP2040 (hand soldered, back side)", UX - 25.4, UY - 40.64)

    # --- FPC to future mainboard --------------------------------------------
    JX, JY = g(360.68), g(66.04)
    jid = "Connector_Generic_MountingPin:Conn_01x14_MountingPin"
    place(jid, "J1", "FH12-14S-0.5SH", JX, JY, 0,
          "Connector_FFC-FPC:Hirose_FH12-14S-0.5SH_1x14-1MP_P0.50mm_Horizontal")
    for n, net in enumerate(FPC_PINS, start=1):
        connect(jid, JX, JY, 0, str(n), net)
    connect(jid, JX, JY, 0, "MP", "GND")  # both mounting pads tied to GND
    text("J1: FPC to FutureOS Regular mainboard (pinout TBD on mainboard side).", JX - 20.32, JY - 25.4)
    text("Do NOT connect the FPC while the XIAO is fitted.", JX - 20.32, JY - 22.86)

    # --- backlight -----------------------------------------------------------
    bx0, by0 = 38.1, 175.26
    text("Backlight: %d x white 0603 LED, low-side switched by Q1 (BL_EN)" % N_LEDS, bx0 - 5.08, by0 - 20.32)
    for i in range(N_LEDS):
        X = g(bx0 + i * 15.24)
        rref, lref = "R%d" % (i + 1), "LED%d" % (i + 1)
        place("Device:R", rref, LED_R, X, by0, 0, FP_R, side=True)
        connect("Device:R", X, by0, 0, "1", "LED_PWR")
        LY = g(by0 + 10.16)
        place("Device:LED", lref, "White", X, LY, 90, FP_LED, side=True)
        wire(X, by0 + 3.81, X, LY - 3.81)
        connect("Device:LED", X, LY, 90, "1", "LED_K")

    qid = "Transistor_FET:AO3400A"
    QX, QY = g(177.8), g(182.88)
    place(qid, "Q1", "AO3400A", QX, QY, 0, "Package_TO_SOT_SMD:SOT-23", side=True)
    connect(qid, QX, QY, 0, "3", "LED_K")
    connect(qid, QX, QY, 0, "1", "BL_EN")
    connect(qid, QX, QY, 0, "2", "GND")

    RX = g(165.1)
    place("Device:R", "R%d" % (N_LEDS + 1), "100k", RX, g(193.04), 0, FP_R, side=True)
    connect("Device:R", RX, g(193.04), 0, "1", "BL_EN")
    connect("Device:R", RX, g(193.04), 0, "2", "GND")

    # --- LED power source jumper ---------------------------------------------
    jpid = "Jumper:SolderJumper_2_Open"
    JPX, JPY = g(228.6), g(180.34)
    place(jpid, "JP1", "LED_PWR<-VBUS", JPX, JPY, 0,
          "Jumper:SolderJumper-2_P1.3mm_Open_Pad1.0x1.5mm")
    connect(jpid, JPX, JPY, 0, "1", "VBUS")
    connect(jpid, JPX, JPY, 0, "2", "LED_PWR")
    text("Close JP1 when running from the XIAO (LEDs from USB 5V).", JPX - 12.7, JPY - 10.16)

    # --- power flags (ERC) ----------------------------------------------------
    for i, net in enumerate(["GND", "VBUS", "LED_PWR"]):
        FX, FY = g(254 + i * 17.78), g(205.74)
        place("power:PWR_FLAG", next_ref("#FLG"), "PWR_FLAG", FX, FY, 0, show_value=False)
        if net == "GND":
            gnd(FX, FY)
        else:
            label(net, FX, FY, 270)

    # --- mounting holes -------------------------------------------------------
    for i in range(len(HOLES)):
        place("Mechanical:MountingHole", "H%d" % (i + 1), "M2", g(330.2 + i * 12.7), g(193.04), 0,
              "MountingHole:MountingHole_2.2mm_M2", in_bom=False)


def write():
    build()
    sch = [S("kicad_sch"), [S("version"), S("20250114")], [S("generator"), "futureos_gen"],
           [S("generator_version"), "10.0"], [S("uuid"), ROOT_UUID], [S("paper"), "A3"],
           [S("title_block"), [S("title"), "FutureOS Keyboard Prototype"], [S("date"), "2026-09-27"],
            [S("rev"), "A"], [S("company"), "FutureOS"],
            [S("comment"), 1, "Generated by hardware/keyboard_prototype/tools/gen_sch.py - edit design.py, not this file"]],
           [S("lib_symbols")] + list(_sym_cache.values())]
    sch += items
    sch.append([S("sheet_instances"), [S("path"), "/", [S("page"), "1"]]])
    with open(os.path.join(PROJ_DIR, PROJECT + ".kicad_sch"), "w", encoding="utf-8") as f:
        f.write(dump(sch) + "\n")

    # project-local libraries (XIAO symbol + footprint from Seeed)
    pretty = os.path.join(PROJ_DIR, LOCAL_FP_LIB + ".pretty")
    os.makedirs(pretty, exist_ok=True)
    shutil.copy(os.path.join(SEEED_DIR, "XIAO-RP2040-SMD.kicad_mod"), pretty)
    shutil.copy(os.path.join(SEEED_DIR, "Seeed_Studio_XIAO_Series.kicad_sym"), PROJ_DIR)
    with open(os.path.join(PROJ_DIR, "fp-lib-table"), "w") as f:
        f.write('(fp_lib_table\n\t(version 7)\n\t(lib (name "%s")(type "KiCad")(uri "${KIPRJMOD}/%s.pretty")(options "")(descr "Project footprints"))\n)\n'
                % (LOCAL_FP_LIB, LOCAL_FP_LIB))
    with open(os.path.join(PROJ_DIR, "sym-lib-table"), "w") as f:
        f.write('(sym_lib_table\n\t(version 7)\n\t(lib (name "Seeed_Studio_XIAO_Series")(type "KiCad")(uri "${KIPRJMOD}/Seeed_Studio_XIAO_Series.kicad_sym")(options "")(descr "Seeed XIAO"))\n)\n')
    pro = os.path.join(PROJ_DIR, PROJECT + ".kicad_pro")
    if not os.path.exists(pro):
        with open(pro, "w") as f:
            f.write('{\n  "meta": {"filename": "%s.kicad_pro", "version": 3},\n  "board": {"design_settings": {"rules": {"min_clearance": 0.2, "min_track_width": 0.2, "min_via_diameter": 0.6, "min_through_hole_diameter": 0.3}}}\n}\n' % PROJECT)


if __name__ == "__main__":
    write()
    print("schematic written")
