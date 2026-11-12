# =====================================================================
#  מגן אולטימטיבי v2 ל-Qin F22 Pro - Blender (bpy), פרמטרי
#
#  המגן המשולב (qin_f22_pro_cover_ultimate.py) + העקרונות מ-docs/design/body-research.md:
#    1. צד שטוח וקצה מעוגל: הקצה האחורי ברדיוס 3.2 מ"מ (היה 2.0) - כף היד נוגעת בו.
#       הקצה הקדמי 1.6 מ"מ.
#    2. אין קצה חד במסלול האגודל: שיפוע פנימי 0.8 מ"מ בשפה שמעל החזית.
#    4. שקע רדוד לאצבע המורה בגב (0.5 מ"מ), חריצי הגב רק מחוץ לו.
#  כל השאר (פינות עם כיסי אוויר, שפה 1.2, מסגרת מצלמה, צלעות, מקש מכוסה, אוזן) - כמו במשולב.
#  הקובץ הקיים לא נדרס. פלט: out/ultimate_v2/.
#
#  הרצה:  blender -b -P qin_f22_pro_cover_ultimate_v2.py
#  qin_f21_pro_cover.py טוען את הקובץ הזה (עם _NO_MAIN) ודורס את מידות המכשיר.
# =====================================================================

import bpy
import json
import math
import os

HERE = os.path.dirname(os.path.abspath(__file__)) if "__file__" in globals() and __file__ else os.getcwd()
_src = open(os.path.join(HERE, "qin_f22_pro_cover_ultimate.py"), encoding="utf-8").read()
_src = _src.replace("\nmain()\n", "\n")
exec(compile(_src, "qin_f22_pro_cover_ultimate.py", "exec"), globals())

# ---------------------------------------------------------------------
# פרמטרים חדשים ב-v2
# ---------------------------------------------------------------------
BACK_EDGE = 3.2          # עיגול הקצה האחורי (כף היד)
FRONT_EDGE = 1.6         # עיגול הקצה הקדמי
LIP_CHAMFER = 0.8        # שיפוע פנימי בשפה הקדמית
FINGER = dict(y=8.0, w=20.0, l=26.0, depth=0.5)   # שקע לאצבע המורה (מרכז y, מידות)
DEVICE_NAME = "qin_f22_pro"
OUT = os.path.join(HERE, "out", "ultimate_v2")


def rounded_block2(name, w, l, r, t, e_bot, e_top, z0=0.0, loc=(0, 0, 0), n=6):
    """כמו rounded_block, אבל עם רדיוס שונה לקצה התחתון (גב) ולעליון (חזית)."""
    rings = []
    for i in range(n + 1):
        a = math.radians(90 * i / n)
        inset, z = e_bot - e_bot * math.sin(a), e_bot - e_bot * math.cos(a)
        rings.append(rr_ring(w - 2 * inset, l - 2 * inset, r - inset, z0 + z))
    for i in range(n, -1, -1):
        a = math.radians(90 * i / n)
        inset, z = e_top - e_top * math.sin(a), t - (e_top - e_top * math.cos(a))
        rings.append(rr_ring(w - 2 * inset, l - 2 * inset, r - inset, z0 + z))
    return loft_rings(name, rings, loc)


def corner_blocks(g):
    pw = 2 * (g["out_r"] + CORNER_GROW) + CORNER_SPAN
    blocks = []
    for sx in (-1, 1):
        for sy in (-1, 1):
            cx = sx * (g["out_w"] / 2 + CORNER_GROW - pw / 2)
            cy = sy * (g["out_l"] / 2 + CORNER_GROW - pw / 2)
            blocks.append(rounded_block2(f"corner_{sx}_{sy}", pw, pw, g["out_r"] + CORNER_GROW, g["out_t"],
                                         BACK_EDGE + 0.6, FRONT_EDGE + 0.4, loc=(cx, cy, 0)))
    return blocks


def finger_rest():
    F = FINGER
    return rr_prism("finger_rest", F["w"], F["l"], F["w"] / 2, -1, F["depth"], loc=(0, F["y"], 0))


def back_grooves(g):
    """חריצים רוחביים, מחוץ למצלמה ולשקע האצבע."""
    G = BACK_GROOVE
    cut = []
    cam_y0 = REAR_CAM["y"] - REAR_CAM["l"] / 2 - G["cam_gap"]
    cam = (REAR_CAM["x"] - REAR_CAM["w"] / 2 - G["cam_gap"], REAR_CAM["x"] + REAR_CAM["w"] / 2 + G["cam_gap"])
    gap = 2.5
    fy0, fy1 = FINGER["y"] - FINGER["l"] / 2 - gap, FINGER["y"] + FINGER["l"] / 2 + gap
    fx = (-FINGER["w"] / 2 - gap, FINGER["w"] / 2 + gap)
    x0, x1 = -g["out_w"] / 2 + G["margin"], g["out_w"] / 2 - G["margin"]
    y = -g["out_l"] / 2 + G["margin"] + 2
    while y < g["out_l"] / 2 - G["margin"]:
        holes = []
        if y >= cam_y0:
            holes.append(cam)
        if fy0 <= y <= fy1:
            holes.append(fx)
        spans, a = [], x0
        for h0, h1 in sorted(holes):
            spans.append((a, h0))
            a = max(a, h1)
        spans.append((a, x1))
        for a, b in spans:
            if b - a > 4:
                cut.append(rr_prism(f"groove_{len(cut)}", b - a, G["w"], G["w"] / 2, -1, G["depth"],
                                    loc=((a + b) / 2, y, 0)))
        y += G["pitch"]
    return cut


def front_cutters(g):
    """החלון מעל החזית + שיפוע פנימי בקצה העליון של השפה."""
    w0, l0, r0 = PHONE_W - 2 * LIP_IN, PHONE_L - 2 * LIP_IN, max(PHONE_R - LIP_IN, 0.8)
    c = LIP_CHAMFER
    return [
        rr_prism("front_open", w0, l0, r0, g["back"] + CLEAR + PHONE_T - 0.6, g["out_t"] + 5),
        rr_frustum("lip_chamfer", w0, l0, r0, g["out_t"] - c,
                   w0 + 2 * (c + 0.4), l0 + 2 * (c + 0.4), r0 + c + 0.4, g["out_t"] + 0.4),
    ]


def side_key_cutters(D, g):
    return port_cutters(D, g)


def build_v2(D):
    g = geometry(D)
    shell = rounded_block2(f"cover_{DEVICE_NAME}", g["out_w"], g["out_l"], g["out_r"], g["out_t"], BACK_EDGE, FRONT_EDGE)
    rw, rl = REAR_CAM["w"] + 5, REAR_CAM["l"] + 5
    adds = corner_blocks(g) + [
        rr_frustum("cam_ridge", rw + 1.6, rl + 1.6, rw / 2 + 0.8, 0.5, rw, rl, rw / 2, -CAM_RIDGE_H,
                   loc=(REAR_CAM["x"], REAR_CAM["y"], 0)),
    ]
    if D.get("cover_volume"):
        adds.append(stadium_cutter("vol_bump", VOLUME["l"] - 3.0, VOLUME["h"] - 2.4, 2 * VOL_BUMP,
                                   (g["out_w"] / 2, VOLUME["y"], g["port_z"]), "X"))
    lug_t = min(6.0, g["out_t"] - 2)
    adds.append(rounded_block("lug", LUG["size"], LUG["size"], 3.0, lug_t, 1.2, z0=(g["out_t"] - lug_t) / 2,
                              loc=(LUG["x"], -g["out_l"] / 2 - 1.5, 0)))
    boolean(shell, adds, "UNION")

    cutters = [
        rounded_block("cavity", g["cav_w"], g["cav_l"], g["cav_r"], g["cav_t"], PHONE_EDGE + CLEAR, z0=g["back"]),
        rr_frustum("rear_cam", REAR_CAM["w"] + 2 * g["back"] + 2, REAR_CAM["l"] + 2 * g["back"] + 2,
                   REAR_CAM["w"] / 2 + g["back"] + 1, -1.0 - CAM_RIDGE_H,
                   REAR_CAM["w"], REAR_CAM["l"], REAR_CAM["w"] / 2, g["back"] + 0.5,
                   loc=(REAR_CAM["x"], REAR_CAM["y"], 0)),
        stadium_cutter("lug_hole", LUG["hole"], LUG["hole"], 14.0,
                       (LUG["x"], -g["out_l"] / 2 - 3.4, g["out_t"] / 2), "X"),
        finger_rest(),
    ] + front_cutters(g) + side_key_cutters(D, g) + air_pockets(g) + side_ribs(g) + back_grooves(g)
    boolean(shell, cutters, "DIFFERENCE")
    clean_degenerate(shell)
    return shell, g


def build_fit_test_v2(D):
    g = geometry(D)
    ring = rounded_block2("fit_test", g["out_w"], g["out_l"], g["out_r"], g["out_t"], BACK_EDGE, FRONT_EDGE)
    boolean(ring, corner_blocks(g), "UNION")
    rim = 3.0
    boolean(ring, [
        rounded_block("cavity", g["cav_w"], g["cav_l"], g["cav_r"], g["cav_t"], PHONE_EDGE + CLEAR, z0=g["back"]),
        rr_prism("back_open", g["cav_w"] - 2 * rim, g["cav_l"] - 2 * rim, max(g["cav_r"] - rim, 2.0), -1, g["back"] + 1),
    ] + front_cutters(g), "DIFFERENCE")
    clean_degenerate(ring)
    return ring


def phone_proxy(g):
    body, scr, keys, lens = build_phone_proxy(g, "ph")
    return body, scr, keys, [lens]


def accent_faces(cover, g, D):
    """מסגרת המצלמה + הבליטה על המקש הצדדי בכתום (רינדור בלבד)."""
    for p in cover.data.polygons:
        c = p.center
        on_ridge = (c.z < -0.05 and abs(c.x - REAR_CAM["x"]) < REAR_CAM["w"] / 2 + 4.5
                    and abs(c.y - REAR_CAM["y"]) < REAR_CAM["l"] / 2 + 4.5)
        on_bump = D.get("cover_volume") and c.x > g["out_w"] / 2 + 0.05 and abs(c.y - VOLUME["y"]) < VOLUME["l"] / 2
        if on_ridge or on_bump:
            p.material_index = 1


def run(D, extra_report=None):
    os.makedirs(OUT, exist_ok=True)
    clear_scene()
    cover, g = build_v2(D)
    info = check_mesh(cover)
    stl = os.path.join(OUT, f"{DEVICE_NAME}_cover_{D['key']}.stl")
    export_stl(cover, stl)
    info.update(device=DEVICE_NAME, phone=[PHONE_W, PHONE_L, PHONE_T], phone_r=PHONE_R, wall=D["wall"],
                back=D["back"], lip_raise=LIP_RAISE, back_edge=BACK_EDGE, lip_chamfer=LIP_CHAMFER, stl=stl)
    if extra_report:
        info.update(extra_report)

    fit = build_fit_test_v2(D)
    fit_info = check_mesh(fit)
    fit_stl = os.path.join(OUT, f"{DEVICE_NAME}_fit_test.stl")
    export_stl(fit, fit_stl)
    fit_info["stl"] = fit_stl
    info["fit_test"] = fit_info
    bpy.data.objects.remove(fit, do_unlink=True)

    set_mat(cover, material("tpu", D["color"], 0.8))
    cover.data.materials.append(material("accent", ACCENT, 0.6))
    accent_faces(cover, g, D)

    body, scr, keys, lenses = phone_proxy(g)
    set_mat(body, material("phone", (0.16, 0.17, 0.19, 1), 0.3))
    set_mat(scr, material("screen", (0.25, 0.47, 0.33, 1), 0.1))
    m_lens = material("lens", (0.02, 0.02, 0.03, 1), 0.1)
    for o in lenses:
        set_mat(o, m_lens)
    mk = material("keys", (0.78, 0.79, 0.78, 1))
    for k in keys:
        set_mat(k, mk)
    phone = [body, scr] + lenses + keys

    sec = section_copy(cover, g["port_z"])
    sec.location.x = 90
    sec.hide_render = True

    scene, cam, target = setup_render()
    L = g["out_l"]
    shots = [render(scene, cam, target, "front", (120, -210 * L / 158, 260 * L / 158), (0, -5, 5), (900, 1030))]
    for o in phone:
        o.hide_render = True
    shots.append(render(scene, cam, target, "back", (130, 190 * L / 158, -230 * L / 158), (0, 0, 5), (900, 1030)))
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
    blend = os.path.join(OUT, f"{DEVICE_NAME}_cover.blend")
    bpy.ops.wm.save_as_mainfile(filepath=blend)
    info["blend"] = blend
    with open(os.path.join(OUT, "report.json"), "w", encoding="utf-8") as f:
        json.dump(info, f, ensure_ascii=False, indent=2)
    print(json.dumps(info, ensure_ascii=False, indent=2))
    if not (info["ok"] and fit_info["ok"]):
        print("!!! רשת לא תקינה")


if not globals().get("_NO_MAIN"):
    run(dict(D, key="ultimate_v2", name="Ultimate v2"))
