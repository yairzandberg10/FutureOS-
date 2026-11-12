# =====================================================================
#  המגן ה"אולטימטיבי" ל-Qin F22 Pro - Blender (bpy), פרמטרי
#
#  משלב את מה שחוזר בכל המקורות במחקר (אוקטובר 2026) במגן אחד:
#    1. פינות עם כיסי אוויר אטומים   - "crumple zone": הפינה נוחתת ראשונה,
#                                       החללים קורסים ומאטים את הבלימה.
#    2. שפה מוגבהת 1.2 מ"מ מעל החזית  - המקורות: ≥1 מ"מ למסך.
#    3. מסגרת מצלמה בולטת 1.2 מ"מ     - המקורות: ≥1.5 מ"מ מעל העדשה
#                                       (גב 1.4 + מסגרת = ~2.6 מעל גב הטלפון).
#    4. צלעות אחיזה בצדדים, באזור כף היד - "grippy not sticky", לא נתקע בכיס.
#    5. חריצים רוחביים בגב           - אחיזה + פיזור חום, מרווח סביב המצלמה.
#    6. מקש עוצמה מכוסה עם בליטה ו-SIM מכוסה - אבק/חול (כמו Nokia 800 Tough).
#    7. אוזן לרצועת יד בתחתית         - מניעת נפילה עדיפה על שרידות בנפילה.
#    8. החזית פתוחה כולה               - מסך, מקלדת, ומצלמת הסלפי שליד #.
#
#  חומר: TPU ‏95A (מודפס בכל מדפסת). להזרקה: אותה גיאומטריה ב-TPU ‏90A.
#  הרצה:  blender -b -P qin_f22_pro_cover_ultimate.py
#  תלוי ב-qin_f22_pro_cover_collection.py (כלי הגיאומטריה והמידות).
# =====================================================================

import bpy
import json
import math
import os

HERE = os.path.dirname(os.path.abspath(__file__)) if "__file__" in globals() and __file__ else os.getcwd()
_src = open(os.path.join(HERE, "qin_f22_pro_cover_collection.py"), encoding="utf-8").read()
_src = _src.replace("\nmain()\n", "\n")
exec(compile(_src, "qin_f22_pro_cover_collection.py", "exec"), globals())

# ---------------------------------------------------------------------
# פרמטרים
# ---------------------------------------------------------------------
# !!! רדיוס פינות הטלפון במבט מלמעלה - לא נמדד. מדפיסים קודם את
#     qin_f22_pro_fit_test.stl (מסגרת בלי גב, ~16 גרם), מלבישים, ומתקנים כאן.
PHONE_R = 5.0
LIP_RAISE = 1.2                       # דורס את 0.8 של הקולקציה
OUT = os.path.join(HERE, "out", "ultimate")
os.makedirs(OUT, exist_ok=True)

D = dict(key="ultimate", name="Ultimate", wall=1.6, back=1.4, color=(0.10, 0.11, 0.12, 1),
         cover_volume=True, cover_sim=True)
ACCENT = (0.95, 0.42, 0.08, 1)        # כתום אות - לרינדור מסגרת המצלמה בלבד

CORNER_GROW = 1.8                     # עיבוי הפינה כלפי חוץ
CORNER_SPAN = 8.0                     # כמה מעבר לרדיוס הפינה העיבוי ממשיך
POCKET = dict(d=1.6, angles=(18, 45, 72), z_margin=1.6)
CAM_RIDGE_H = 1.2
SIDE_RIB = dict(w=1.0, depth=0.5, pitch=2.5, y0=-46.0, y1=12.0, z_margin=2.2)
BACK_GROOVE = dict(w=1.2, depth=0.6, pitch=4.0, margin=7.0, cam_gap=5.5)
LUG = dict(x=-17.0, size=10.0, hole=3.2)


def corner_blocks(g):
    pw = 2 * (g["out_r"] + CORNER_GROW) + CORNER_SPAN
    blocks = []
    for sx in (-1, 1):
        for sy in (-1, 1):
            cx = sx * (g["out_w"] / 2 + CORNER_GROW - pw / 2)
            cy = sy * (g["out_l"] / 2 + CORNER_GROW - pw / 2)
            blocks.append(rounded_block(f"corner_{sx}_{sy}", pw, pw, g["out_r"] + CORNER_GROW, g["out_t"],
                                        CASE_EDGE + 0.6, loc=(cx, cy, 0)))
    return blocks


def air_pockets(g):
    """שלושה חללים אנכיים אטומים בכל פינה, באמצע עובי הדופן המעובה."""
    r_in = g["cav_r"]
    r_mid = r_in + (D["wall"] + CORNER_GROW) / 2
    z0 = POCKET["z_margin"]
    z1 = g["out_t"] - POCKET["z_margin"] - LIP_RAISE
    out = []
    for sx in (-1, 1):
        for sy in (-1, 1):
            ccx = sx * (g["cav_w"] / 2 - r_in)
            ccy = sy * (g["cav_l"] / 2 - r_in)
            for a in POCKET["angles"]:
                t = math.radians(a)
                x = ccx + sx * r_mid * math.cos(t)
                y = ccy + sy * r_mid * math.sin(t)
                out.append(rr_prism(f"pocket_{sx}_{sy}_{a}", POCKET["d"], POCKET["d"], POCKET["d"] / 2, z0, z1,
                                    loc=(x, y, 0)))
    return out


def side_ribs(g):
    cut = []
    z0, z1 = SIDE_RIB["z_margin"], g["out_t"] - SIDE_RIB["z_margin"]
    y = SIDE_RIB["y0"]
    while y <= SIDE_RIB["y1"]:
        for sx in (-1, 1):
            cut.append(rr_prism(f"rib_{sx}_{y:.1f}", 2 * SIDE_RIB["depth"], SIDE_RIB["w"], SIDE_RIB["w"] / 2 - 0.01,
                                z0, z1, loc=(sx * g["out_w"] / 2, y, 0)))
        y += SIDE_RIB["pitch"]
    return cut


def back_grooves(g):
    G = BACK_GROOVE
    cut = []
    cam_y0 = REAR_CAM["y"] - REAR_CAM["l"] / 2 - G["cam_gap"]
    cam_x0 = REAR_CAM["x"] - REAR_CAM["w"] / 2 - G["cam_gap"]
    cam_x1 = REAR_CAM["x"] + REAR_CAM["w"] / 2 + G["cam_gap"]
    lug_clear = -g["out_l"] / 2 + G["margin"] + 2
    y = lug_clear
    while y < g["out_l"] / 2 - G["margin"]:
        if y < cam_y0:
            spans = [(-g["out_w"] / 2 + G["margin"], g["out_w"] / 2 - G["margin"])]
        else:
            spans = [(-g["out_w"] / 2 + G["margin"], cam_x0), (cam_x1, g["out_w"] / 2 - G["margin"])]
        for a, b in spans:
            if b - a > 4:
                cut.append(rr_prism(f"groove_{len(cut)}", b - a, G["w"], G["w"] / 2, -1, G["depth"],
                                    loc=((a + b) / 2, y, 0)))
        y += G["pitch"]
    return cut


def build_ultimate():
    g = geometry(D)
    shell = rounded_block("cover_ultimate", g["out_w"], g["out_l"], g["out_r"], g["out_t"], CASE_EDGE)

    rw, rl = REAR_CAM["w"] + 5, REAR_CAM["l"] + 5
    ridge = rr_frustum("cam_ridge", rw + 1.6, rl + 1.6, rw / 2 + 0.8, 0.5, rw, rl, rw / 2, -CAM_RIDGE_H,
                       loc=(REAR_CAM["x"], REAR_CAM["y"], 0))
    bump = stadium_cutter("vol_bump", VOLUME["l"] - 3.0, VOLUME["h"] - 2.4, 2 * VOL_BUMP,
                          (g["out_w"] / 2, VOLUME["y"], g["port_z"]), "X")
    lug_t = min(6.0, g["out_t"] - 2)
    lug = rounded_block("lug", LUG["size"], LUG["size"], 3.0, lug_t, 1.2, z0=(g["out_t"] - lug_t) / 2,
                        loc=(LUG["x"], -g["out_l"] / 2 - 1.5, 0))
    boolean(shell, corner_blocks(g) + [ridge, bump, lug], "UNION")

    cutters = [
        rounded_block("cavity", g["cav_w"], g["cav_l"], g["cav_r"], g["cav_t"], PHONE_EDGE + CLEAR, z0=g["back"]),
        rr_prism("front_open", PHONE_W - 2 * LIP_IN, PHONE_L - 2 * LIP_IN, max(PHONE_R - LIP_IN, 0.8),
                 g["back"] + CLEAR + PHONE_T - 0.6, g["out_t"] + 5),
        rr_frustum("rear_cam", REAR_CAM["w"] + 2 * g["back"] + 2, REAR_CAM["l"] + 2 * g["back"] + 2,
                   REAR_CAM["w"] / 2 + g["back"] + 1, -1.0 - CAM_RIDGE_H,
                   REAR_CAM["w"], REAR_CAM["l"], REAR_CAM["w"] / 2, g["back"] + 0.5,
                   loc=(REAR_CAM["x"], REAR_CAM["y"], 0)),
        stadium_cutter("lug_hole", LUG["hole"], LUG["hole"], 14.0,
                       (LUG["x"], -g["out_l"] / 2 - 3.4, g["out_t"] / 2), "X"),
    ] + port_cutters(D, g) + air_pockets(g) + side_ribs(g) + back_grooves(g)
    boolean(shell, cutters, "DIFFERENCE")
    clean_degenerate(shell)
    return shell, g


def build_fit_test():
    """מסגרת היקפית בלבד: אותן פינות, שפה וחלל כמו במגן, בלי גב, בלי פתחים ובלי טקסטורה."""
    g = geometry(D)
    ring = rounded_block("fit_test", g["out_w"], g["out_l"], g["out_r"], g["out_t"], CASE_EDGE)
    boolean(ring, corner_blocks(g), "UNION")
    rim = 3.0
    boolean(ring, [
        rounded_block("cavity", g["cav_w"], g["cav_l"], g["cav_r"], g["cav_t"], PHONE_EDGE + CLEAR, z0=g["back"]),
        rr_prism("front_open", PHONE_W - 2 * LIP_IN, PHONE_L - 2 * LIP_IN, max(PHONE_R - LIP_IN, 0.8),
                 g["back"] + CLEAR + PHONE_T - 0.6, g["out_t"] + 5),
        rr_prism("back_open", g["cav_w"] - 2 * rim, g["cav_l"] - 2 * rim, max(g["cav_r"] - rim, 2.0), -1, g["back"] + 1),
    ], "DIFFERENCE")
    clean_degenerate(ring)
    return ring


def section_copy(obj, z):
    """עותק חתוך בגובה z, כדי להראות את כיסי האוויר."""
    cp = obj.copy()
    cp.data = obj.data.copy()
    cp.name = "section"
    bpy.context.scene.collection.objects.link(cp)
    box = rr_prism("section_cut", 400, 400, 1, z, 100)
    boolean(cp, [box], "DIFFERENCE")
    return cp


def main():
    clear_scene()
    cover, g = build_ultimate()
    info = check_mesh(cover)
    stl = os.path.join(OUT, "qin_f22_pro_cover_ultimate.stl")
    export_stl(cover, stl)
    info.update(name=D["name"], wall=D["wall"], back=D["back"], lip_raise=LIP_RAISE,
                cam_ridge=CAM_RIDGE_H, pockets=4 * len(POCKET["angles"]), stl=stl)

    fit = build_fit_test()
    fit_info = check_mesh(fit)
    fit_stl = os.path.join(OUT, "qin_f22_pro_fit_test.stl")
    export_stl(fit, fit_stl)
    fit_info["stl"] = fit_stl
    info["fit_test"] = fit_info
    bpy.data.objects.remove(fit, do_unlink=True)

    m_tpu = material("tpu", D["color"], 0.8)
    m_acc = material("accent", ACCENT, 0.6)
    set_mat(cover, m_tpu)
    # צביעת מסגרת המצלמה והבליטה של מקש העוצמה בכתום (רינדור בלבד)
    cover.data.materials.append(m_acc)
    for p in cover.data.polygons:
        c = p.center
        on_ridge = (c.z < -0.05 and abs(c.x - REAR_CAM["x"]) < REAR_CAM["w"] / 2 + 4.5
                    and abs(c.y - REAR_CAM["y"]) < REAR_CAM["l"] / 2 + 4.5)
        on_bump = c.x > g["out_w"] / 2 + 0.05 and abs(c.y - VOLUME["y"]) < VOLUME["l"] / 2
        if on_ridge or on_bump:
            p.material_index = 1

    body, scr, keys, lens = build_phone_proxy(g, "ph")
    set_mat(body, material("phone", (0.16, 0.17, 0.19, 1), 0.3))
    set_mat(scr, material("screen", (0.25, 0.47, 0.33, 1), 0.1))
    set_mat(lens, material("lens", (0.02, 0.02, 0.03, 1), 0.1))
    mk = material("keys", (0.78, 0.79, 0.78, 1))
    for k in keys:
        set_mat(k, mk)
    phone = [body, scr, lens] + keys

    sec = section_copy(cover, g["port_z"])
    sec.location.x = 90
    sec.hide_render = True

    scene, cam, target = setup_render()
    shots = []
    shots.append(render(scene, cam, target, "front", (120, -210, 260), (0, -5, 5), (900, 1030)))
    for o in phone:
        o.hide_render = True
    shots.append(render(scene, cam, target, "back", (130, 190, -230), (0, 0, 5), (900, 1030)))
    shots.append(render(scene, cam, target, "side_grip", (230, -60, 40), (0, -20, 6), (1000, 690)))
    cover.hide_render = True
    sec.hide_render = False
    cx, cy = 90 + g["out_w"] / 2 - 8, -g["out_l"] / 2 + 8
    shots.append(render(scene, cam, target, "corner_section", (cx + 30, cy - 35, 70), (cx, cy, 4), (900, 710)))
    info["renders"] = shots

    bpy.data.objects.remove(sec, do_unlink=True)
    cover.hide_render = False
    for o in phone:
        o.hide_render = False
    blend = os.path.join(OUT, "qin_f22_pro_cover_ultimate.blend")
    bpy.ops.wm.save_as_mainfile(filepath=blend)
    info["blend"] = blend
    with open(os.path.join(OUT, "report.json"), "w", encoding="utf-8") as f:
        json.dump(info, f, ensure_ascii=False, indent=2)
    print(json.dumps(info, ensure_ascii=False, indent=2))
    if not (info["ok"] and fit_info["ok"]):
        print("!!! רשת לא תקינה")


main()
