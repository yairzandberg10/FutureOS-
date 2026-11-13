# =====================================================================
#  קולקציית מגנים (כיסויים נלבשים) ל-Qin F22 Pro - Blender (bpy), פרמטרי
#
#  בניגוד ל-qin_f22_pro_case.py (גוף חלופי), כאן אלה מגנים שנלבשים מעל הטלפון
#  המקורי, להדפסה ב-TPU (95A). החזית פתוחה כולה (מסך + מקלדת + מצלמת הסלפי
#  שליד #), והשפה עוטפת את קצה החזית ובולטת מעליה כדי להגן על המסך.
#
#  הרצה:  blender -b -P qin_f22_pro_cover_collection.py
#
#  הדגמים:
#    slim     - דק: דופן 1.2, גב 1.0. מינימום נפח.
#    rugged   - מוגן: דופן 2.0, פינות מעובות, מסגרת סביב המצלמה,
#               מקש עוצמה מכוסה (ממברנה דקה + בליטה), SIM מכוסה.
#    grip     - אחיזה: חריצים רוחביים בגב.
#    lanyard  - רצועה: אוזן בתחתית עם חור לרצועת יד.
#    bumper   - באמפר: מסגרת בלבד, הגב פתוח (נשארת שפה של 5 מ"מ).
#    wallet   - ארנק: כיס לכרטיס אשראי בגב, פתוח בתחתית, עם חלון לאגודל.
#
#  מקור המידות: אותן מדידות כמו ב-qin_f22_pro_case.py (±1 מ"מ).
#  !!! לא ידוע: רדיוס הפינות ועיגול הקצה של הגוף המקורי (PHONE_R, PHONE_EDGE).
#      מדפיסים קודם slim ובודקים התאמה לפני השאר.
#  צירים: X = רוחב, Y = אורך (חיובי = למעלה), Z = עובי (0 = גב המגן). מ"מ.
# =====================================================================

import bpy
import bmesh
import json
import math
import os
from mathutils import Vector
from mathutils.bvhtree import BVHTree

# ---------------------------------------------------------------------
# 1. הטלפון המקורי
# ---------------------------------------------------------------------
PHONE_L = 147.0
PHONE_W = 57.5
PHONE_T = 9.0
PHONE_R = 5.0          # רדיוס פינות במבט מלמעלה - לאמת
PHONE_EDGE = 1.5       # עיגול קצה החזית/הגב - לאמת

SCREEN_W, SCREEN_H, SCREEN_FROM_TOP = 49.9, 74.8, 9.2
SELFIE = dict(from_left=49.8, from_top=138.0, d=6.5)


def from_top(d):
    return PHONE_L / 2 - d


# פתחים (מרכזים במחצית עובי הטלפון). במגן הפתחים גדולים יותר מהמקור,
# כדי שתקע USB עם ציפוי עבה ייכנס.
USB = dict(x=0.0, w=12.5, h=6.0)
MIC = dict(x=-8.0, d=2.6)
TOP_WINDOW = dict(x=15.5, w=13.0, h=6.0)
SIM = dict(y=from_top(40.0), l=32.0, h=5.0)       # צד שמאל
VOLUME = dict(y=from_top(43.0), l=23.5, h=5.0)    # צד ימין
REAR_CAM = dict(x=-1.5, y=from_top(25.3), w=10.7 + 3.0, l=21.2 + 3.0)

# ---------------------------------------------------------------------
# 2. מבנה המגן
# ---------------------------------------------------------------------
CLEAR = 0.2            # מרווח בין הטלפון למגן (TPU נמתח)
LIP_IN = 1.8           # כמה השפה נכנסת פנימה מעל החזית
LIP_RAISE = 0.8        # כמה השפה בולטת מעל החזית (הגנה כשהטלפון הפוך)
CASE_EDGE = 2.0        # עיגול הקצה החיצוני
ARC_SEGS = 12

# ---------------------------------------------------------------------
# 3. הדגמים
# ---------------------------------------------------------------------
DESIGNS = [
    dict(key="slim", name="Slim", wall=1.2, back=1.0, color=(0.08, 0.08, 0.09, 1)),
    dict(key="rugged", name="Rugged", wall=2.0, back=1.6, color=(0.85, 0.42, 0.10, 1),
         corners=True, cam_ridge=True, cover_volume=True, cover_sim=True),
    dict(key="grip", name="Grip", wall=1.5, back=1.4, color=(0.12, 0.30, 0.55, 1), grooves=True),
    dict(key="lanyard", name="Lanyard", wall=1.4, back=1.2, color=(0.20, 0.50, 0.32, 1), lug=True),
    dict(key="bumper", name="Bumper", wall=1.6, back=1.2, color=(0.75, 0.12, 0.18, 1), open_back=True),
    dict(key="wallet", name="Wallet", wall=1.4, back=0.8, color=(0.42, 0.30, 0.22, 1), card=True),
]

CARD = dict(w=54.0 + 1.0, l=85.6 + 1.0, t=0.76 + 0.4, skin=1.0)   # כרטיס ISO + מרווח
GROOVE = dict(w=1.2, depth=0.6, pitch=4.0, margin=7.0)
VOL_MEMBRANE = 0.8     # עובי הדופן שנשאר מעל מקש העוצמה ב-rugged
VOL_BUMP = 0.6

HERE = os.path.dirname(os.path.abspath(__file__)) if "__file__" in globals() and __file__ else os.getcwd()
OUT = os.path.join(HERE, "out", "covers")
os.makedirs(OUT, exist_ok=True)


# =====================================================================
# כלי גיאומטריה
# =====================================================================
def rr_ring(w, l, r, z, segs=ARC_SEGS):
    r = max(min(r, w / 2, l / 2), 0.05)
    cx, cy = w / 2 - r, l / 2 - r
    pts = []
    for (sx, sy, a0) in ((1, 1, 0), (-1, 1, 90), (-1, -1, 180), (1, -1, 270)):
        for i in range(segs + 1):
            a = math.radians(a0 + 90 * i / segs)
            p = (sx * cx + r * math.cos(a), sy * cy + r * math.sin(a), z)
            if not pts or math.dist(p, pts[-1]) > 1e-6:
                pts.append(p)
    if math.dist(pts[0], pts[-1]) <= 1e-6:
        pts.pop()
    return pts


def loft_rings(name, rings, loc=(0, 0, 0)):
    mesh = bpy.data.meshes.new(name)
    bm = bmesh.new()
    vr = [[bm.verts.new(p) for p in ring] for ring in rings]
    n = len(vr[0])
    bm.faces.new(list(reversed(vr[0])))
    bm.faces.new(vr[-1])
    for a, b in zip(vr[:-1], vr[1:]):
        for i in range(n):
            j = (i + 1) % n
            bm.faces.new((a[i], a[j], b[j], b[i]))
    bmesh.ops.recalc_face_normals(bm, faces=bm.faces)
    bm.to_mesh(mesh)
    bm.free()
    obj = bpy.data.objects.new(name, mesh)
    bpy.context.scene.collection.objects.link(obj)
    obj.location = loc
    return obj


def rr_prism(name, w, l, r, z0, z1, loc=(0, 0, 0)):
    return loft_rings(name, [rr_ring(w, l, r, z0), rr_ring(w, l, r, z1)], loc)


def rr_frustum(name, w0, l0, r0, z0, w1, l1, r1, z1, loc=(0, 0, 0)):
    return loft_rings(name, [rr_ring(w0, l0, r0, z0), rr_ring(w1, l1, r1, z1)], loc)


def rounded_block(name, w, l, r, t, edge, z0=0.0, loc=(0, 0, 0), n=6):
    """מלבן מעוגל בעובי t עם רבע עיגול ברדיוס edge בקצה התחתון והעליון."""
    rings = []
    for i in range(n + 1):
        a = math.radians(90 * i / n)
        inset, z = edge - edge * math.sin(a), edge - edge * math.cos(a)
        rings.append(rr_ring(w - 2 * inset, l - 2 * inset, r - inset, z0 + z))
    for i in range(n, -1, -1):
        a = math.radians(90 * i / n)
        inset, z = edge - edge * math.sin(a), t - (edge - edge * math.cos(a))
        rings.append(rr_ring(w - 2 * inset, l - 2 * inset, r - inset, z0 + z))
    return loft_rings(name, rings, loc)


def stadium_cutter(name, length, height, depth, loc, axis):
    """אצטדיון שחוצה דופן. axis='Y' לקצוות עליון/תחתון, 'X' לצדדים."""
    if axis == "Y":
        obj = rr_prism(name, length, height, height / 2, -depth / 2, depth / 2)
        obj.rotation_euler = (math.radians(90), 0, 0)
    else:
        obj = rr_prism(name, height, length, height / 2, -depth / 2, depth / 2)
        obj.rotation_euler = (0, math.radians(90), 0)
    obj.location = loc
    return obj


def select_only(obj):
    bpy.context.view_layer.objects.active = obj
    for o in bpy.context.scene.objects:
        o.select_set(o == obj)


def boolean(target, others, op):
    if not others:
        return
    coll = bpy.data.collections.new(f"_{op.lower()}_operands")
    bpy.context.scene.collection.children.link(coll)
    for o in others:
        for c in list(o.users_collection):
            c.objects.unlink(o)
        coll.objects.link(o)
    mod = target.modifiers.new(op, "BOOLEAN")
    mod.operation = op
    mod.solver = "EXACT"
    mod.operand_type = "COLLECTION"
    mod.collection = coll
    select_only(target)
    bpy.ops.object.modifier_apply(modifier=mod.name)
    for o in list(coll.objects):
        bpy.data.objects.remove(o, do_unlink=True)
    bpy.data.collections.remove(coll)


def clean_degenerate(obj):
    bm = bmesh.new()
    bm.from_mesh(obj.data)
    bmesh.ops.dissolve_degenerate(bm, dist=1e-4, edges=bm.edges)
    bm.to_mesh(obj.data)
    bm.free()


def clear_scene():
    bpy.ops.wm.read_factory_settings(use_empty=True)
    s = bpy.context.scene
    s.unit_settings.system = "METRIC"
    s.unit_settings.scale_length = 0.001
    s.unit_settings.length_unit = "MILLIMETERS"


# =====================================================================
# המגן
# =====================================================================
def geometry(d):
    """מידות נגזרות לדגם: חלל, גוף חיצוני, גובה מרכז הפתחים."""
    back = d["back"]
    if d.get("card"):
        back = d["back"] + CARD["t"] + CARD["skin"]
    cav_w, cav_l, cav_t = PHONE_W + 2 * CLEAR, PHONE_L + 2 * CLEAR, PHONE_T + 2 * CLEAR
    g = dict(
        back=back,
        cav_w=cav_w, cav_l=cav_l, cav_t=cav_t, cav_r=PHONE_R + CLEAR,
        out_w=cav_w + 2 * d["wall"], out_l=cav_l + 2 * d["wall"],
        out_r=PHONE_R + CLEAR + d["wall"],
        out_t=back + cav_t + LIP_RAISE,
        phone_z=back + CLEAR,
        port_z=back + CLEAR + PHONE_T / 2,
    )
    return g


def port_cutters(d, g):
    depth = 2 * d["wall"] + 2
    yb, yt = -g["cav_l"] / 2 - d["wall"] / 2, g["cav_l"] / 2 + d["wall"] / 2
    xs = g["cav_w"] / 2 + d["wall"] / 2
    z = g["port_z"]
    c = [
        stadium_cutter("usb", USB["w"], USB["h"], depth, (USB["x"], yb, z), "Y"),
        stadium_cutter("mic", MIC["d"], MIC["d"], depth, (MIC["x"], yb, z), "Y"),
        stadium_cutter("top_window", TOP_WINDOW["w"], TOP_WINDOW["h"], depth, (TOP_WINDOW["x"], yt, z), "Y"),
    ]
    if not d.get("cover_sim"):
        c.append(stadium_cutter("sim", SIM["l"], SIM["h"], depth, (-xs, SIM["y"], z), "X"))
    if d.get("cover_volume"):
        # ממברנה: מדללים את הדופן מבפנים ומשאירים VOL_MEMBRANE
        thin = d["wall"] - VOL_MEMBRANE
        x_in = g["cav_w"] / 2
        c.append(stadium_cutter("vol_pocket", VOLUME["l"] - 1.0, VOLUME["h"] - 1.0, 2 * thin,
                                (x_in, VOLUME["y"], z), "X"))
    else:
        c.append(stadium_cutter("volume", VOLUME["l"], VOLUME["h"], depth, (xs, VOLUME["y"], z), "X"))
    return c


def build_cover(d):
    g = geometry(d)
    shell = rounded_block(f"cover_{d['key']}", g["out_w"], g["out_l"], g["out_r"], g["out_t"], CASE_EDGE)
    adds = []

    if d.get("corners"):
        # פינות מעובות: 1.2 מ"מ החוצה בכל כיוון, לאורך 16 מ"מ מכל פינה
        grow = 1.2
        pw = 2 * (g["out_r"] + grow) + 6
        for sx in (-1, 1):
            for sy in (-1, 1):
                cx = sx * (g["out_w"] / 2 + grow - pw / 2)
                cy = sy * (g["out_l"] / 2 + grow - pw / 2)
                adds.append(rounded_block(f"corner_{sx}_{sy}", pw, pw, g["out_r"] + grow, g["out_t"], CASE_EDGE + 0.4,
                                          loc=(cx, cy, 0)))
    if d.get("cam_ridge"):
        rw, rl = REAR_CAM["w"] + 5, REAR_CAM["l"] + 5
        adds.append(rr_frustum("cam_ridge", rw + 1.2, rl + 1.2, rw / 2 + 0.6, 0.5, rw, rl, rw / 2, -0.8,
                               loc=(REAR_CAM["x"], REAR_CAM["y"], 0)))
    if d.get("cover_volume"):
        # בליטה מעל המקש, כדי למצוא אותו במגע
        bump = stadium_cutter("vol_bump", VOLUME["l"] - 3.0, VOLUME["h"] - 2.4, 2 * VOL_BUMP,
                              (g["out_w"] / 2, VOLUME["y"], g["port_z"]), "X")
        adds.append(bump)
    if d.get("lug"):
        # אוזן לרצועה מתחת לקצה התחתון, משמאל ל-USB; החור לרוחב (X)
        lug_t = min(6.0, g["out_t"] - 2)
        lug = rounded_block("lug", 10.0, 10.0, 3.0, lug_t, 1.2, z0=(g["out_t"] - lug_t) / 2,
                            loc=(-17.0, -g["out_l"] / 2 - 1.5, 0))
        adds.append(lug)
    if adds:
        boolean(shell, adds, "UNION")

    cutters = [
        # החלל של הטלפון: אותה צורה כמו הטלפון + מרווח
        rounded_block("cavity", g["cav_w"], g["cav_l"], g["cav_r"], g["cav_t"], PHONE_EDGE + CLEAR, z0=g["back"]),
        # מעל החזית: חלון פתוח שהשפה מקיפה
        rr_prism("front_open", PHONE_W - 2 * LIP_IN, PHONE_L - 2 * LIP_IN, max(PHONE_R - LIP_IN, 0.8),
                 g["back"] + CLEAR + PHONE_T - 0.6, g["out_t"] + 5),
        # מצלמה אחורית עם שיפוע כלפי חוץ
        rr_frustum("rear_cam", REAR_CAM["w"] + 2 * g["back"] + 2, REAR_CAM["l"] + 2 * g["back"] + 2,
                   REAR_CAM["w"] / 2 + g["back"] + 1, -1.0 - (1.2 if d.get("cam_ridge") else 0),
                   REAR_CAM["w"], REAR_CAM["l"], REAR_CAM["w"] / 2, g["back"] + 0.5,
                   loc=(REAR_CAM["x"], REAR_CAM["y"], 0)),
    ] + port_cutters(d, g)

    if d.get("lug"):
        cutters.append(stadium_cutter("lug_hole", 3.2, 3.2, 14.0, (-17.0, -g["out_l"] / 2 - 3.4, g["out_t"] / 2), "X"))
    if d.get("open_back"):
        rim = 5.0
        cutters.append(rr_prism("back_window", g["cav_w"] - 2 * rim, g["cav_l"] - 2 * rim, max(g["cav_r"] - rim, 3.0),
                                -1, g["back"] + 1))
    if d.get("grooves"):
        cam_y0 = REAR_CAM["y"] - REAR_CAM["l"] / 2 - 3
        y = -g["out_l"] / 2 + GROOVE["margin"]
        gl = g["out_w"] - 2 * GROOVE["margin"]
        while y < g["out_l"] / 2 - GROOVE["margin"]:
            if y < cam_y0:
                cutters.append(rr_prism(f"groove_{len(cutters)}", gl, GROOVE["w"], GROOVE["w"] / 2,
                                        -1, GROOVE["depth"], loc=(0, y, 0)))
            y += GROOVE["pitch"]
        # מעל המצלמה: חריצים קצרים בשני הצדדים שלה
        y = cam_y0 + GROOVE["pitch"]
        cam_x0 = REAR_CAM["x"] - REAR_CAM["w"] / 2 - 3
        cam_x1 = REAR_CAM["x"] + REAR_CAM["w"] / 2 + 3
        while y < g["out_l"] / 2 - GROOVE["margin"]:
            lx0, lx1 = -g["out_w"] / 2 + GROOVE["margin"], cam_x0
            rx0, rx1 = cam_x1, g["out_w"] / 2 - GROOVE["margin"]
            for (a, b) in ((lx0, lx1), (rx0, rx1)):
                if b - a > 4:
                    cutters.append(rr_prism(f"groove_{len(cutters)}", b - a, GROOVE["w"], GROOVE["w"] / 2,
                                            -1, GROOVE["depth"], loc=((a + b) / 2, y, 0)))
            y += GROOVE["pitch"]
    if d.get("card"):
        # כיס בין הגב החיצוני (CARD.skin) לגב הפנימי, פתוח בתחתית
        z0 = CARD["skin"]
        y_top = -g["cav_l"] / 2 + CARD["l"]
        cutters.append(rr_prism("card_pocket", CARD["w"], CARD["l"] + 10, 1.0, z0, z0 + CARD["t"],
                                loc=(0, y_top - (CARD["l"] + 10) / 2, 0)))
        # חלון אגודל בגב החיצוני לדחיפת הכרטיס החוצה
        cutters.append(rr_prism("card_thumb", 26.0, 16.0, 8.0, -1, z0 + 0.01,
                                loc=(0, -g["cav_l"] / 2 + 26.0, 0)))

    boolean(shell, cutters, "DIFFERENCE")
    clean_degenerate(shell)
    return shell, g


def build_phone_proxy(g, name):
    """הטלפון (לתמונות בלבד)."""
    body = rounded_block(f"{name}_phone", PHONE_W, PHONE_L, PHONE_R, PHONE_T, PHONE_EDGE, z0=g["phone_z"])
    top = g["phone_z"] + PHONE_T
    scr = rr_prism(f"{name}_screen", SCREEN_W, SCREEN_H, 0.6, top - 0.1, top + 0.02,
                   loc=(0, from_top(SCREEN_FROM_TOP) - SCREEN_H / 2, 0))
    keys = []
    rows = [from_top(SCREEN_FROM_TOP + SCREEN_H + 1.9) - 4.4 - i * 9.5 for i in range(6)]
    for r, y in enumerate(rows):
        for x in (-18.25, 0.0, 18.0):
            if r < 2 and x == 0.0:
                continue
            keys.append(rr_prism(f"{name}_key", 14.0, 7.0, 3.5 if r >= 2 else 2.0, top - 0.1, top + 0.5, loc=(x, y, 0)))
    dpad = rr_prism(f"{name}_dpad", 15.0, 15.0, 3.5, top - 0.1, top + 0.5, loc=(0, rows[0] - 4.8, 0))
    lens = rr_prism(f"{name}_selfie", SELFIE["d"], SELFIE["d"], SELFIE["d"] / 2, top - 0.1, top + 0.03,
                    loc=(SELFIE["from_left"] - PHONE_W / 2, from_top(SELFIE["from_top"]), 0))
    return body, scr, keys + [dpad], lens


# =====================================================================
# בדיקות
# =====================================================================
def check_mesh(obj):
    bm = bmesh.new()
    bm.from_mesh(obj.data)
    bm.transform(obj.matrix_world)
    bmesh.ops.triangulate(bm, faces=bm.faces)
    bm.faces.ensure_lookup_table()
    nm_edges = sum(1 for e in bm.edges if not e.is_manifold)
    nm_verts = sum(1 for v in bm.verts if not v.is_manifold)
    tree = BVHTree.FromBMesh(bm)
    overlaps = 0
    for a, b in tree.overlap(tree):
        if a < b and not (set(bm.faces[a].verts) & set(bm.faces[b].verts)):
            overlaps += 1
    cos = [v.co for v in bm.verts]
    lo = Vector((min(c.x for c in cos), min(c.y for c in cos), min(c.z for c in cos)))
    hi = Vector((max(c.x for c in cos), max(c.y for c in cos), max(c.z for c in cos)))
    vol = bm.calc_volume(signed=True)
    res = dict(faces=len(bm.faces), non_manifold_edges=nm_edges, non_manifold_verts=nm_verts,
               self_intersections=overlaps, volume_mm3=round(vol, 1),
               tpu_grams=round(vol / 1000 * 1.21, 1),
               size_mm=[round(c, 2) for c in (hi - lo)])
    res["ok"] = nm_edges == 0 and nm_verts == 0 and overlaps == 0 and vol > 0
    bm.free()
    return res


# =====================================================================
# ייצוא ורינדור
# =====================================================================
def export_stl(obj, path):
    select_only(obj)
    bpy.ops.wm.stl_export(filepath=path, export_selected_objects=True, ascii_format=False)


def material(name, rgba, rough=0.6):
    m = bpy.data.materials.new(name)
    m.diffuse_color = rgba
    m.roughness = rough
    return m


def set_mat(obj, mat):
    obj.data.materials.clear()
    obj.data.materials.append(mat)
    for poly in obj.data.polygons:
        poly.material_index = 0


def collection(name):
    c = bpy.data.collections.new(name)
    bpy.context.scene.collection.children.link(c)
    return c


def move_to(obj, coll):
    for c in list(obj.users_collection):
        c.objects.unlink(obj)
    coll.objects.link(obj)


def setup_render():
    scene = bpy.context.scene
    scene.render.engine = "BLENDER_WORKBENCH"
    scene.display.shading.light = "STUDIO"
    scene.display.shading.color_type = "MATERIAL"
    scene.display.shading.show_cavity = True
    scene.display.shading.show_shadows = True
    if scene.world is None:
        scene.world = bpy.data.worlds.new("World")
    scene.world.color = (0.86, 0.87, 0.89)
    scene.render.resolution_x = 2400
    scene.render.resolution_y = 1100
    target = bpy.data.objects.new("look_at", None)
    scene.collection.objects.link(target)
    cam = bpy.data.objects.new("cam", bpy.data.cameras.new("cam"))
    cam.data.lens = 50
    cam.data.clip_start = 1
    cam.data.clip_end = 5000
    scene.collection.objects.link(cam)
    scene.camera = cam
    t = cam.constraints.new("TRACK_TO")
    t.target = target
    t.track_axis = "TRACK_NEGATIVE_Z"
    t.up_axis = "UP_Y"
    return scene, cam, target


def render(scene, cam, target, name, loc, look, res=None):
    if res:
        scene.render.resolution_x, scene.render.resolution_y = res
    cam.location = loc
    target.location = look
    scene.render.filepath = os.path.join(OUT, f"render_{name}.png")
    bpy.ops.render.render(write_still=True)
    return scene.render.filepath


# =====================================================================
def main():
    clear_scene()
    spacing = 82.0
    x0 = -spacing * (len(DESIGNS) - 1) / 2
    m_phone = material("phone", (0.16, 0.17, 0.19, 1), 0.3)
    m_screen = material("screen", (0.25, 0.47, 0.33, 1), 0.1)
    m_key = material("phone_keys", (0.78, 0.79, 0.78, 1))
    m_lens = material("lens", (0.02, 0.02, 0.03, 1), 0.1)

    report = dict(phone=[PHONE_W, PHONE_L, PHONE_T], clear=CLEAR, covers={})
    covers, phones = [], []
    for i, d in enumerate(DESIGNS):
        cover, g = build_cover(d)
        info = check_mesh(cover)
        path = os.path.join(OUT, f"qin_f22_pro_cover_{d['key']}.stl")
        export_stl(cover, path)        # מיוצא כשהוא עדיין במקור, הגב על z=0
        info.update(name=d["name"], wall=d["wall"], back=round(g["back"], 2), stl=path)
        report["covers"][d["key"]] = info

        coll = collection(f"Cover {d['name']}")
        set_mat(cover, material(f"tpu_{d['key']}", d["color"], 0.75))
        move_to(cover, coll)
        body, scr, keys, lens = build_phone_proxy(g, d["key"])
        set_mat(body, m_phone)
        set_mat(scr, m_screen)
        set_mat(lens, m_lens)
        for k in keys:
            set_mat(k, m_key)
        parts = [body, scr, lens] + keys
        for o in parts:
            move_to(o, coll)
        x = x0 + i * spacing
        cover.location.x += x
        for o in parts:
            o.location.x += x
        covers.append(cover)
        phones.append(parts)

    # תמונות: מבט מהגב על כל הקולקציה, מבט מהחזית עם הטלפונים, ותקריב של כל דגם
    scene, cam, target = setup_render()
    renders = [
        render(scene, cam, target, "lineup_back", (0, 260, -680), (0, 0, 5)),
        render(scene, cam, target, "lineup_front", (0, -330, 650), (0, -5, 5)),
    ]
    for o in [p for ps in phones for p in ps]:
        o.hide_render = True
    renders.append(render(scene, cam, target, "lineup_covers_only", (0, -330, 650), (0, -5, 5)))
    for o in [p for ps in phones for p in ps]:
        o.hide_render = False
    for i, (d, cover) in enumerate(zip(DESIGNS, covers)):
        x = x0 + i * spacing
        for c in bpy.context.scene.collection.children:
            c.hide_render = c.name != f"Cover {d['name']}"
        renders.append(render(scene, cam, target, f"{d['key']}_back", (x + 150, 170, -200), (x, 0, 5), (1400, 1100)))
    for c in bpy.context.scene.collection.children:
        c.hide_render = False
    report["renders"] = renders

    blend = os.path.join(OUT, "qin_f22_pro_cover_collection.blend")
    bpy.ops.wm.save_as_mainfile(filepath=blend)
    report["blend"] = blend
    with open(os.path.join(OUT, "report.json"), "w", encoding="utf-8") as f:
        json.dump(report, f, ensure_ascii=False, indent=2)
    print(json.dumps(report, ensure_ascii=False, indent=2))
    bad = [k for k, v in report["covers"].items() if not v["ok"]]
    if bad:
        print("!!! רשת לא תקינה:", bad)


main()
