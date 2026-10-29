# =====================================================================
#  גוף חלופי ל-Qin F22 Pro - body_04 "כחול קפסולה" (רטרו נוקיה 3310)
#
#  הרעיון: להחזיר את תחושת הנוקיה 3310 - אבן חלקה שנכנסת ליד.
#    - גב מעוגל ככל האפשר: קצה גב אליפטי (רדיוס אופקי 5.0, אנכי 2.6; עיגול
#      מעגלי מלא היה פותח את הדופן מעל רצפת האמבט) ופינות R8 במבט מלמעלה.
#    - כחול כהה (נייבי) לכל הגוף. מקשים בכחול-אפור בהיר, טבעת חצים כחולה
#      עמוקה, ו-OK ומקש העוצמה בירוק-ליים של תאורת מסך נוקיה.
#    - מקשי מספרים אובליים מלאים (כמו בנוקיה), מקשי ניווט ריבועיים-מעוגלים,
#      טבעת חצים עגולה עם OK עגול (במקום הריבועים של v2). בלי frets.
#    - קצה חזית עדין R1.0, שיפוע גדול יותר בשפת המקשים, מקשים בולטים 1.0.
#    - גב: מרקם אחיזה של חריצים אופקיים רדודים (0.35) בצורת אליפסה, לוגו
#      FutureOS שקוע קטן מעל, בלי שקע לאצבע.
#    - דפנות: שורות חריצי אחיזה אנכיים (0.4) בשני הצדדים בחלק התחתון.
#  לא השתנה: מידות חיצוניות, כל הפתחים, חלון המסך והמדף, הסלפי, מרכזי המקשים,
#  WALL/BACK/FRONT/SKIRT, KEY_NUB_H. פלט: out/bodies/body_04/
#
#  (הכותרת של v2:)
#  (הכותרת המקורית של v1:)
#  גוף חלופי ל-Qin F22 Pro - Blender (bpy), פרמטרי ורץ מחדש מאפס
#
#  זה לא כיסוי שנלבש על הטלפון. זה גוף שמחליף את הגוף המקורי: פותחים את
#  ה-F22 Pro ומעבירים את החלקים הפנימיים (לוח, מסך, סוללה, ממברנת המקשים)
#  לגוף הזה. המידות החיצוניות והמיקומים זהים למקור (147x57.5x9), העיצוב שונה:
#  פינות מעוגלות יותר וקצוות מעוגלים בחזית ובגב, מקשי מספרים בצורת גלולה,
#  ו-D-pad מרובע: מסגרת חצים כהה עם OK מרובע במרכזה (לפי הסקיצה של המשתמש).
#
#  הרצה:  blender -b -P qin_f22_pro_case.py
#
#  חלקים להדפסה:
#    back_housing   - אמבט אחורי: גב + דפנות, כל הפתחים בצדדים ובקצוות, מצלמה.
#    front_housing  - חזית: חלון מסך עם מדף לזכוכית, חריץ אוזנייה, חלון לכל
#                     מקש, ותקע היקפי שנכנס לאמבט (לחיצה, או טיפת דבק).
#    keycaps        - 15 מקשים, טבעת חצים, OK ומקש עוצמה צדדי. לכל מקש פלנג'
#                   מתחת לחזית ופין שלוחץ על הכיפה בממברנה המקורית. לטבעת
#                   החצים 4 פינים (למעלה/למטה/ימין/שמאל), וה-OK נתפס מתחתיה.
#  התא של # נשאר חלון פתוח בצורת גלולה: מצלמת הסלפי יושבת מתחתיו.
#
#  !!! מה לא ידוע עד שמפרקים את הטלפון (ערכים מסומנים "לאמת בפירוק"):
#    - עובי הלוח, המסך והסוללה והגבהים שלהם בתוך הגוף
#    - המרחק מתחתית החזית לכיפות הממברנה (KEY_NUB_H)
#    - נקודות עיגון מקוריות (ברגים, תפסים) - כאן אין ברגים, רק תקע היקפי
#    - מיקום הרמקול, חיישן הקרבה והאור, ונורית
#  לכן מדפיסים קודם את front_housing בלבד ובודקים מול החלקים האמיתיים.
#
#  מקור המידות: תמונות עם כרטיס בגודל כרטיס אשראי כסקאלה (2026-10-05), ±1 מ"מ.
#  צירים: X = רוחב (חיובי = ימין במבט מהחזית), Y = אורך (חיובי = למעלה),
#  Z = עובי (0 = הגב, החזית ב-Z=BODY_T). יחידות: מ"מ.
# =====================================================================

import bpy
import bmesh
import json
import math
import os
from mathutils import Vector
from mathutils.bvhtree import BVHTree

# ---------------------------------------------------------------------
# 1. הגוף המקורי (נמדד)
# ---------------------------------------------------------------------
BODY_L = 147.0
BODY_W = 57.5
BODY_T = 9.0
BODY_R = 8.0          # רדיוס פינות במבט מלמעלה (במקור ~5). יותר מזה כבר נכנס
                      # למקום שבו כנראה יושבות פינות המסך והלוח המקוריים
BACK_EDGE_R = 5.0     # קצה הגב: רחב, נוגע בכף היד
FRONT_EDGE_R = 1.0
BACK_EDGE_Z = 2.6     # גובה קשת הגב (אליפסה: אופקי BACK_EDGE_R, אנכי BACK_EDGE_Z)    # קצה החזית: חד ומדויק
REVEAL_D = 0.35       # עומק חריץ הצל בקו החיבור
REVEAL_H = 0.2        # גובה הפס הישר בתחתית החריץ (קו החיתוך עובר בו)

# מסך: אזור מואר 49.9x74.8, ממורכז לרוחב, 9.2 מ"מ מלמעלה
SCREEN_W = 49.9
SCREEN_H = 74.8
SCREEN_FROM_TOP = 9.2
SCREEN_GAP_TO_KEYS = 1.9

KEY_ROW_H = [8.8, 10.0, 9.9, 9.6, 9.6, 8.9]   # מלמעלה למטה
KEY_COL_W = [18.8, 17.7, 19.2]                # משמאל לימין
KEY_LABELS = [
    ["", "DPAD", ""],    # מקש רך שמאלי | D-pad | חזור
    ["", "DPAD", ""],    # חיוג         | D-pad | ניתוק
    ["1", "2", "3"],
    ["4", "5", "6"],
    ["7", "8", "9"],
    ["*", "0", "#"],
]
SELFIE = dict(from_left=49.8, from_top=138.0, d=6.5)

# ---------------------------------------------------------------------
# 2. מבנה הגוף
# ---------------------------------------------------------------------
WALL = 1.4            # עובי דופן
BACK = 1.2            # עובי הגב
FRONT = 1.2           # עובי החזית
SKIRT_H = 1.5         # עומק התקע של החזית בתוך האמבט
SKIRT_T = 0.8         # עובי התקע
SKIRT_GAP = 0.15      # מרווח בין התקע לדופן (לחיצה)

SCREEN_LEDGE_MARGIN = 0.4   # חלון המסך גדול מהאזור המואר רק בזה
GLASS_LEDGE_MARGIN = 1.4    # מדף בצד הפנימי של החזית: זכוכית המסך נשענת עליו מבפנים
GLASS_LEDGE_DEPTH = 0.6     # עומק המדף (נשאר 0.6 מ"מ של חזית מעליו)

EARPIECE = dict(from_top=4.6, w=12.0, h=1.4)   # חריץ אוזנייה מעל המסך (לאמת בפירוק)

# צורות המקשים (לפי הסקיצה). המרכזים נשארים במרכזי התאים שנמדדו, מעל הכיפות.
NUM_KEY = (14.0, 7.4)       # מקש מספר: גלולה, רוחב x גובה (כולם באותו רוחב)
NAV_KEY = (14.0, 7.4)       # מקשי ניווט בצדדים: ריבוע מעוגל, בגודל מקש מספר
KEY_TOP_CHAMFER = 0.5      # שיפוע בשפה העליונה של כל מקש
FRET_W = 0.8                # פס בין שורות המספרים
FRET_H = 0.35
LOGO = dict(text="FutureOS", size=4.6, from_top=62.0, depth=0.3)
FINGER = dict(from_top=64.0, w=17.0, l=26.0, depth=0.3, slope=1.5)  # שקע לאצבע המורה
NAV_R = 2.2
DPAD_OUT = 16.0             # טבעת החצים: ריבוע מעוגל
DPAD_R = 8.0
OK_SIZE = 9.5               # OK: ריבוע מעוגל בתוך הטבעת
OK_R = 4.75
DPAD_NUB_OFFSET = 6.4       # מרחק הפינים של החצים מהמרכז (הקוטר המקורי 14.7)
KEY_WINDOW_CLEAR = 0.35
FLANGE_H = 0.8
FLANGE_MARGIN = 1.2         # הפלנג' רחב מהמקש בזה (סה"כ)
KEY_PROTRUDE = 1.0          # כמה המקש בולט מעל החזית
KEY_NUB_H = 0.8             # פין מתחת לפלנג' עד הממברנה - לאמת בפירוק
LABEL_DEPTH = 0.4

# ---------------------------------------------------------------------
# 3. פתחים בדפנות ובגב (מרכזים במחצית העובי, כמו במקור)
# ---------------------------------------------------------------------
def from_top(d):
    return BODY_L / 2 - d

PORT_Z = BODY_T / 2
USB = dict(x=0.0, w=9.2, h=3.6)                 # USB-C 8.34x2.56 + מרווח
MIC = dict(x=-8.0, d=1.6)
# חלון כהה מוארך בקצה העליון: כנראה IR; גבוה מספיק גם לשקע 3.5 (חור ~4 מ"מ)
TOP_WINDOW = dict(x=15.5, w=11.0, h=4.4)
SIM = dict(y=from_top(40.0), l=30.0, h=3.6)     # צד שמאל, 25-55 מלמעלה
VOLUME = dict(y=from_top(43.0), l=21.0, h=3.2)  # צד ימין, 33-53 מלמעלה
REAR_CAM = dict(x=-1.5, y=from_top(25.3), w=10.7, l=21.2)

# ---------------------------------------------------------------------
# 4. נגזרות
# ---------------------------------------------------------------------
IN_W = BODY_W - 2 * WALL
IN_L = BODY_L - 2 * WALL
IN_R = max(BODY_R - WALL, 0.5)
SPLIT_Z = BODY_T - FRONT          # קו החיבור בין האמבט לחזית
SKIRT_OUT_W = IN_W - 2 * SKIRT_GAP
SKIRT_OUT_L = IN_L - 2 * SKIRT_GAP
SKIRT_IN_W = SKIRT_OUT_W - 2 * SKIRT_T
SKIRT_IN_L = SKIRT_OUT_L - 2 * SKIRT_T

screen_cy = from_top(SCREEN_FROM_TOP) - SCREEN_H / 2
keypad_top = from_top(SCREEN_FROM_TOP + SCREEN_H + SCREEN_GAP_TO_KEYS)
keypad_x0 = -KEY_COL_W[1] / 2 - KEY_COL_W[0]
selfie_x = SELFIE["from_left"] - BODY_W / 2
selfie_y = from_top(SELFIE["from_top"])

# העמודות הקיצוניות מוצרות מבחוץ כך שהפלנג' לא ייתקל בתקע של החזית.
KEY_LIMIT_X = SKIRT_IN_W / 2 - FLANGE_MARGIN / 2 - KEY_WINDOW_CLEAR - 0.2


def cell_cy(r):
    return keypad_top - sum(KEY_ROW_H[:r]) - KEY_ROW_H[r] / 2


def cell_cx(c):
    return keypad_x0 + sum(KEY_COL_W[:c]) + KEY_COL_W[c] / 2


dpad_y = keypad_top - KEY_ROW_H[0]
dpad_hole = OK_SIZE + 2 * KEY_WINDOW_CLEAR

# [תווית, x, y, רוחב, גובה, רדיוס, סוג]: key / open / dpad / ok
KEYS = []
for r in range(6):
    for c in range(3):
        lab = KEY_LABELS[r][c]
        if lab == "DPAD":
            continue
        w, h = NAV_KEY if r < 2 else NUM_KEY
        # רוחב קבוע; המרכז זז פנימה רק כמה שצריך (הפין עדיין על הכיפה)
        x = min(max(cell_cx(c), -KEY_LIMIT_X + w / 2), KEY_LIMIT_X - w / 2)
        rad = NAV_R if r < 2 else h / 2
        KEYS.append([lab, x, cell_cy(r), w, h, rad, "open" if lab == "#" else "key"])
KEYS.append(["", 0.0, dpad_y, DPAD_OUT, DPAD_OUT, DPAD_R, "dpad"])
KEYS.append(["", 0.0, dpad_y, OK_SIZE, OK_SIZE, OK_R, "ok"])

HERE = os.path.dirname(os.path.abspath(__file__)) if "__file__" in globals() and __file__ else os.getcwd()
OUT = os.path.join(HERE, "..", "out", "bodies", "body_04")
os.makedirs(OUT, exist_ok=True)
ARC_SEGS = 12


# =====================================================================
# כלי גיאומטריה
# =====================================================================
def rr_ring(w, l, r, z, segs=ARC_SEGS):
    r = min(r, w / 2, l / 2)
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
    """גוף סגור מסדרת טבעות עם אותו מספר נקודות, מלמטה למעלה."""
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


def rounded_body(name):
    """הגוף החיצוני המלא: צד שטוח, קצה גב רחב, קצה חזית חד, וחריץ צל בקו החיבור."""
    prof = []                                   # (inset, z) מלמטה למעלה
    n, R = 8, BACK_EDGE_R
    for i in range(n + 1):                      # קצה הגב
        t = math.radians(90 * i / n)
        prof.append((BACK_EDGE_R * (1 - math.sin(t)), BACK_EDGE_Z * (1 - math.cos(t))))
    # חריץ צל: שיפוע 45° פנימה, פס ישר שקו החיתוך עובר בו, ושיפוע החוצה
    prof += [(0.0, SPLIT_Z - REVEAL_H / 2 - REVEAL_D),
             (REVEAL_D, SPLIT_Z - REVEAL_H / 2),
             (REVEAL_D, SPLIT_Z + REVEAL_H / 2)]
    n, R = 4, FRONT_EDGE_R
    for i in range(n, -1, -1):                  # קצה החזית
        t = math.radians(90 * i / n)
        prof.append((R - R * math.sin(t), BODY_T - (R - R * math.cos(t))))
    rings = [rr_ring(BODY_W - 2 * a, BODY_L - 2 * a, BODY_R - a, z) for a, z in prof]
    return loft_rings(name, rings)


def stadium_cutter(name, length, height, depth, loc, axis):
    """פתח אצטדיון שחוצה דופן. axis='Y' לקצוות עליון/תחתון, 'X' לצדדים."""
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
        if o is not None:
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


def text_cutter(text, size, z_mid, depth):
    cu = bpy.data.curves.new("label", "FONT")
    cu.body = text
    cu.size = size
    cu.align_x = "CENTER"
    cu.align_y = "CENTER"
    cu.extrude = depth
    tmp = bpy.data.objects.new("label_tmp", cu)
    bpy.context.scene.collection.objects.link(tmp)
    dg = bpy.context.evaluated_depsgraph_get()
    mesh = bpy.data.meshes.new_from_object(tmp.evaluated_get(dg))
    bpy.data.objects.remove(tmp, do_unlink=True)
    bpy.data.curves.remove(cu)
    obj = bpy.data.objects.new(f"label_{text}", mesh)
    bpy.context.scene.collection.objects.link(obj)
    obj.location = (0, 0, z_mid)
    return obj


def clear_scene():
    bpy.ops.wm.read_factory_settings(use_empty=True)
    s = bpy.context.scene
    s.unit_settings.system = "METRIC"
    s.unit_settings.scale_length = 0.001
    s.unit_settings.length_unit = "MILLIMETERS"


def chamfer_cutter(name, w, l, r, z_face, inward, c, loc=(0, 0, 0)):
    """שיפוע 45° בשפת חלון. z_face = המשטח, inward = +1 אם החומר מעל המשטח (גב), -1 אם מתחתיו (חזית)."""
    rings = [rr_ring(w + 2 * c + 2, l + 2 * c + 2, r + c + 1, z_face - inward * 1.0),
             rr_ring(w + 2 * c, l + 2 * c, r + c, z_face),
             rr_ring(w, l, max(r, 0.3), z_face + inward * c)]
    if inward < 0:
        rings.reverse()
    return loft_rings(name, rings, loc)


def finger_dimple():
    """שקע רדוד בגב, איפה שהאצבע המורה נחה כשמקלידים באגודל (כמו ב-Pixel)."""
    w, l, d, s = FINGER["w"], FINGER["l"], FINGER["depth"], FINGER["slope"]
    rings = [rr_ring(w + 2, l + 2, (w + 2) / 2, -1.0),
             rr_ring(w, l, w / 2, 0.0),
             rr_ring(w - 2 * s, l - 2 * s, (w - 2 * s) / 2, d)]
    return loft_rings("finger_dimple", rings, (0, from_top(FINGER["from_top"]), 0))


def mirrored_text_cutter(text, size, depth, loc):
    """טקסט שקוע בגב: הפוך במראה כדי שיהיה קריא כשמסתכלים על הגב."""
    obj = text_cutter(text, size, 0.0, depth)
    obj.rotation_euler = (0, math.radians(180), 0)
    obj.location = loc
    return obj


def fret_bars():
    return []   # body_04: בלי frets (נוקיה: מקשים אובליים חלקים)


def _fret_bars_v2():
    """פסים בולטים בין שורות המספרים (BlackBerry): מוצאים שורה בלי להסתכל."""
    bars = []
    x_half = KEY_LIMIT_X + 0.4
    for r in range(1, 5):
        y = (cell_cy(r) + cell_cy(r + 1)) / 2
        bars.append(rr_prism(f"fret{r}", 2 * x_half, FRET_W, FRET_W / 2, BODY_T - 0.05, BODY_T + FRET_H,
                             loc=(0, y, 0)))
    return bars


# =====================================================================
# החלקים
# =====================================================================
def port_cutters():
    """הפתחים בדפנות. חותכים גם את התקע של החזית, שעובר מאחורי הדופן."""
    depth = 2 * (WALL + SKIRT_GAP + SKIRT_T) + 1
    return [
        stadium_cutter("usb", USB["w"], USB["h"], depth, (USB["x"], -BODY_L / 2, PORT_Z), "Y"),
        stadium_cutter("mic", MIC["d"], MIC["d"], depth, (MIC["x"], -BODY_L / 2, PORT_Z), "Y"),
        stadium_cutter("top_window", TOP_WINDOW["w"], TOP_WINDOW["h"], depth,
                       (TOP_WINDOW["x"], BODY_L / 2, PORT_Z), "Y"),
        stadium_cutter("sim", SIM["l"], SIM["h"], depth, (-BODY_W / 2, SIM["y"], PORT_Z), "X"),
        stadium_cutter("volume", VOLUME["l"], VOLUME["h"], depth, (BODY_W / 2, VOLUME["y"], PORT_Z), "X"),
    ]


GROOVE_DEPTH = 0.35      # עומק חריצי הגב (מקסימום מותר 0.4)
GROOVE_W = 0.9
GROOVE_PITCH = 2.6
GRIP_DEPTH = 0.4         # חריצי אחיזה בדפנות
GRIP_PITCH = 3.0


def back_grooves():
    """חריצים אופקיים רדודים בגב, בצורת אליפסה: מרקם אחיזה בלי לגעת במצלמה ובלוגו."""
    cuts = []
    yc, semi, xmax = -33.0, 26.0, 19.0
    y = -8.0
    i = 0
    while y >= -58.0:
        k = 1 - ((y - yc) / semi) ** 2
        half = xmax * math.sqrt(k) if k > 0 else 0.0
        if half >= 3.0:
            cuts.append(rr_prism(f"groove{i}", 2 * half, GROOVE_W, GROOVE_W / 2,
                                 -1.0, GROOVE_DEPTH, loc=(0, y, 0)))
        y -= GROOVE_PITCH
        i += 1
    return cuts


def side_grips():
    """חריצי אחיזה אנכיים בדפנות הצד, בחלק התחתון (הפתחים נמצאים למעלה)."""
    cuts = []
    depth = 2.0
    for side in (-1, 1):
        for i in range(11):
            y = -18.0 - GRIP_PITCH * i
            x = side * (BODY_W / 2 + depth / 2 - GRIP_DEPTH)
            cuts.append(stadium_cutter(f"grip{side}_{i}", 1.0, 3.6, depth, (x, y, PORT_Z), "X"))
    return cuts


def build_back_housing():
    tub = rounded_body("back_housing")
    boolean(tub, [rr_prism("below_split", BODY_W + 10, BODY_L + 10, 1, -1, SPLIT_Z)], "INTERSECT")
    cutters = port_cutters() + [
        rr_prism("cavity", IN_W, IN_L, IN_R, BACK, SPLIT_Z + 1),
        rr_prism("rear_cam", REAR_CAM["w"], REAR_CAM["l"], REAR_CAM["w"] / 2, -1, BACK + 1,
                 loc=(REAR_CAM["x"], REAR_CAM["y"], 0)),
        chamfer_cutter("rear_cam_chamfer", REAR_CAM["w"], REAR_CAM["l"], REAR_CAM["w"] / 2, 0.0, +1, 0.6,
                       loc=(REAR_CAM["x"], REAR_CAM["y"], 0)),
        mirrored_text_cutter(LOGO["text"], LOGO["size"], LOGO["depth"], (0, from_top(LOGO["from_top"]), 0)),
    ]
    cutters += back_grooves() + side_grips()
    boolean(tub, cutters, "DIFFERENCE")
    clean_degenerate(tub)
    return tub


def build_front_housing():
    plate = rounded_body("front_housing")
    boolean(plate, [rr_prism("above_split", BODY_W + 10, BODY_L + 10, 1, SPLIT_Z, BODY_T + 1)], "INTERSECT")
    skirt = rr_prism("skirt", SKIRT_OUT_W, SKIRT_OUT_L, max(IN_R - SKIRT_GAP, 0.5), SPLIT_Z - SKIRT_H, SPLIT_Z + 0.01)
    skirt_hole = rr_prism("skirt_hole", SKIRT_IN_W, SKIRT_IN_L, max(IN_R - SKIRT_GAP - SKIRT_T, 0.3),
                          SPLIT_Z - SKIRT_H - 1, SPLIT_Z)
    boolean(skirt, [skirt_hole], "DIFFERENCE")
    boolean(plate, [skirt] + fret_bars(), "UNION")

    cutters = port_cutters()
    cutters.append(chamfer_cutter("screen_chamfer", SCREEN_W + 2 * SCREEN_LEDGE_MARGIN,
                                  SCREEN_H + 2 * SCREEN_LEDGE_MARGIN, 0.8, BODY_T, -1, 0.5, loc=(0, screen_cy, 0)))
    # חלון המסך (המסגרת יושבת על שולי הזכוכית) + מדף מבפנים שהזכוכית נשענת עליו
    cutters.append(rr_prism("screen_window", SCREEN_W + 2 * SCREEN_LEDGE_MARGIN, SCREEN_H + 2 * SCREEN_LEDGE_MARGIN,
                            0.8, SPLIT_Z - 1, BODY_T + 1, loc=(0, screen_cy, 0)))
    cutters.append(rr_prism("glass_ledge", SCREEN_W + 2 * GLASS_LEDGE_MARGIN, SCREEN_H + 2 * GLASS_LEDGE_MARGIN,
                            1.2, SPLIT_Z - 1, SPLIT_Z + GLASS_LEDGE_DEPTH, loc=(0, screen_cy, 0)))
    cutters.append(rr_prism("earpiece", EARPIECE["w"], EARPIECE["h"], EARPIECE["h"] / 2, SPLIT_Z - 1, BODY_T + 1,
                            loc=(0, from_top(EARPIECE["from_top"]), 0)))
    for lab, x, y, w, h, r, kind in KEYS:
        if kind in ("ok", "open"):
            continue        # ה-OK בולט דרך החלון של טבעת החצים; ב-# יש רק חלון עגול לסלפי
        c = KEY_WINDOW_CLEAR
        cutters.append(rr_prism(f"win_{kind}_{lab}", w + 2 * c, h + 2 * c, r + c,
                                SPLIT_Z - 1, BODY_T + 1, loc=(x, y, 0)))
    # שיפוע מעל עדשת הסלפי (בתוך החלון של #)
    cutters.append(rr_frustum("selfie_cone", SELFIE["d"] + 1, SELFIE["d"] + 1, (SELFIE["d"] + 1) / 2, SPLIT_Z - 0.01,
                              SELFIE["d"] + 1 + 2 * FRONT, SELFIE["d"] + 1 + 2 * FRONT,
                              (SELFIE["d"] + 1) / 2 + FRONT, BODY_T + 0.01, loc=(selfie_x, selfie_y, 0)))
    boolean(plate, cutters, "DIFFERENCE")
    clean_degenerate(plate)
    return plate


def chamfered_cap(name, w, h, r, z0, z1, c=KEY_TOP_CHAMFER):
    return loft_rings(name, [rr_ring(w, h, r, z0), rr_ring(w, h, r, z1 - c),
                             rr_ring(w - 2 * c, h - 2 * c, max(r - c, 0.3), z1)])


def key_heights():
    z_flange = KEY_NUB_H
    return z_flange, z_flange + FLANGE_H, KEY_NUB_H + FLANGE_H + FRONT + KEY_PROTRUDE


def build_keycap(lab, w, h, r):
    """מקש במקור (0,0); תחתית הפין ב-z=0 (על הכיפה), הפלנג' נוגע בתחתית החזית."""
    zf, zb, zt = key_heights()
    cap = chamfered_cap(f"key_{lab or 'nav'}", w, h, r, zb - 0.01, zt)
    flange = rr_prism("flange", w + FLANGE_MARGIN, h + FLANGE_MARGIN, r + FLANGE_MARGIN / 2, zf - 0.01, zb)
    nub = rr_prism("nub", 3.0, 3.0, 1.5, 0.0, zf)
    boolean(cap, [flange, nub], "UNION")
    if lab:
        boolean(cap, [text_cutter(lab, h * 0.6, zt, LABEL_DEPTH)], "DIFFERENCE")
    clean_degenerate(cap)
    return cap


def build_dpad_ring():
    """טבעת החצים: ריבוע מעוגל עם חור ל-OK, 4 פינים מתחת לכיווני החצים."""
    zf, zb, zt = key_heights()
    ring = chamfered_cap("dpad_ring", DPAD_OUT, DPAD_OUT, DPAD_R, zb - 0.01, zt)
    flange = rr_prism("flange", DPAD_OUT + FLANGE_MARGIN, DPAD_OUT + FLANGE_MARGIN, DPAD_R + FLANGE_MARGIN / 2,
                      zf - 0.01, zb)
    nubs = [rr_prism(f"nub{i}", 1.6, 1.6, 0.8, 0.0, zf, loc=(dx * DPAD_NUB_OFFSET, dy * DPAD_NUB_OFFSET, 0))
            for i, (dx, dy) in enumerate(((1, 0), (-1, 0), (0, 1), (0, -1)))]
    boolean(ring, [flange] + nubs, "UNION")
    boolean(ring, [rr_prism("ok_hole", dpad_hole, dpad_hole, OK_R + KEY_WINDOW_CLEAR, -1, zt + 1)], "DIFFERENCE")
    clean_degenerate(ring)
    return ring


def build_ok_key():
    """OK: הפלנג' שלו יושב ישר על הכיפה ונתפס מתחת לטבעת החצים."""
    zf, zb, zt = key_heights()
    ok = chamfered_cap("ok_key", OK_SIZE, OK_SIZE, OK_R, FLANGE_H - 0.01, zt)
    flange = rr_prism("flange", dpad_hole + 0.5, dpad_hole + 0.5, OK_R + 0.6, 0.0, FLANGE_H)
    boolean(ok, [flange], "UNION")
    clean_degenerate(ok)
    return ok


def build_volume_rocker():
    """מקש עוצמה. בנוי שוכב (להדפסה): Z מקומי = כלפי חוץ מהדופן."""
    ln, ht = VOLUME["l"] - 0.6, VOLUME["h"] - 0.6
    body = rr_prism("volume_rocker", ht, ln, ht / 2, -0.01, WALL + 0.6)
    flange = rr_prism("rocker_flange", ht + 1.4, ln + 2.0, ht / 2 + 0.7, -0.6, 0.0)
    nubs = [rr_prism(f"rocker_nub{i}", 1.6, 1.6, 0.8, -1.4, -0.59, loc=(0, s * ln / 4, 0)) for i, s in ((0, -1), (1, 1))]
    boolean(body, [flange] + nubs, "UNION")
    # חריץ באמצע: מרגישים איפה נגמר "למעלה" ומתחיל "למטה"
    boolean(body, [rr_prism("rocker_notch", ht + 1, 1.0, 0.3, WALL + 0.3, WALL + 1.0)], "DIFFERENCE")
    clean_degenerate(body)
    return body


def build_keys_in_place():
    caps = []
    z_nub = SPLIT_Z - FLANGE_H - KEY_NUB_H
    for lab, x, y, w, h, r, kind in KEYS:
        if kind == "key":
            cap = build_keycap(lab, w, h, r)
        elif kind == "dpad":
            cap = build_dpad_ring()
        elif kind == "ok":
            cap = build_ok_key()
        else:
            continue
        cap.location = (x, y, z_nub)
        caps.append(cap)
    rocker = build_volume_rocker()
    rocker.rotation_euler = (0, math.radians(90), 0)
    rocker.location = (IN_W / 2, VOLUME["y"], PORT_Z)
    caps.append(rocker)
    return caps


def print_plate(parts):
    """עותק של כל המקשים פרוסים שטוח על משטח הדפסה, מאוחדים לאובייקט אחד."""
    copies = []
    cols, step_x, step_y = 5, max(KEY_COL_W) + 3, max(KEY_ROW_H) + 4
    for i, p in enumerate(parts):
        c = p.copy()
        c.data = p.data.copy()
        bpy.context.scene.collection.objects.link(c)
        c.rotation_euler = (0, 0, 0)
        if c.name.startswith("volume_rocker"):
            # ארוך מתא ברשת: בשורה נפרדת מתחת; הפינים שלו יורדים עד z=-1.4
            c.location = (BODY_W + 20, BODY_L / 2 - 10 - (len(parts) // cols + 1) * step_y - 6, 1.4)
        else:
            c.location = (BODY_W + 20 + (i % cols) * step_x, BODY_L / 2 - 10 - (i // cols) * step_y, 0)
        copies.append(c)
    for o in bpy.context.scene.objects:
        if o is not None:
            o.select_set(o in copies)
    bpy.context.view_layer.objects.active = copies[0]
    bpy.ops.object.join()
    plate = bpy.context.view_layer.objects.active
    plate.name = "keycaps_print_plate"
    return plate


def build_internals_preview():
    """מסך דמה (לתמונות בלבד)."""
    scr = rr_prism("screen_preview", SCREEN_W + 3, SCREEN_H + 3, 1.0, SPLIT_Z - 1.6, SPLIT_Z - 0.05, loc=(0, screen_cy, 0))
    lit = rr_prism("screen_lit_preview", SCREEN_W, SCREEN_H, 0.6, SPLIT_Z - 0.05, SPLIT_Z, loc=(0, screen_cy, 0))
    lens = rr_prism("selfie_preview", SELFIE["d"], SELFIE["d"], SELFIE["d"] / 2, SPLIT_Z - 1.6, SPLIT_Z - 0.3,
                    loc=(selfie_x, selfie_y, 0))
    return [scr, lit, lens]


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
    zero = sum(1 for f in bm.faces if f.calc_area() < 1e-8)
    tree = BVHTree.FromBMesh(bm)
    overlaps = 0
    for a, b in tree.overlap(tree):
        if a < b and not (set(bm.faces[a].verts) & set(bm.faces[b].verts)):
            overlaps += 1
    cos = [v.co for v in bm.verts]
    lo = Vector((min(c.x for c in cos), min(c.y for c in cos), min(c.z for c in cos)))
    hi = Vector((max(c.x for c in cos), max(c.y for c in cos), max(c.z for c in cos)))
    res = dict(object=obj.name, faces=len(bm.faces), non_manifold_edges=nm_edges,
               non_manifold_verts=nm_verts, zero_area_faces=zero, self_intersections=overlaps,
               volume_mm3=round(bm.calc_volume(signed=True), 1),
               size_mm=[round(c, 2) for c in (hi - lo)])
    res["ok"] = nm_edges == 0 and nm_verts == 0 and overlaps == 0 and res["volume_mm3"] > 0
    bm.free()
    return res


def check_design():
    issues = []
    for name, h in (("USB", USB["h"]), ("top_window", TOP_WINDOW["h"]), ("SIM", SIM["h"]), ("volume", VOLUME["h"])):
        m = min(PORT_Z - h / 2 - BACK, SPLIT_Z - (PORT_Z + h / 2))
        if m < 0.5:
            issues.append(f"פתח {name} קרוב מדי לגב או לחזית ({m:.2f} מ\"מ)")
    if BODY_W / 2 - FRONT_EDGE_R - (SCREEN_W / 2 + SCREEN_LEDGE_MARGIN + 0.5) < 0.8:
        issues.append("החזית ליד חלון המסך דקה מדי")
    ok_flange_half = (dpad_hole + 0.5) / 2
    if DPAD_NUB_OFFSET - 0.8 - ok_flange_half < 0.2:
        issues.append("פיני החצים נוגעים בפלנג' של ה-OK")
    keys = [k for k in KEYS if k[6] in ("key", "open", "dpad")]
    for i, a in enumerate(keys):
        for b in keys[i + 1:]:
            gx = abs(a[1] - b[1]) - (a[3] + b[3]) / 2 - FLANGE_MARGIN
            gy = abs(a[2] - b[2]) - (a[4] + b[4]) / 2 - FLANGE_MARGIN
            if max(gx, gy) < 0.3:
                issues.append(f"הפלנג'ים של {a[0] or a[6]} ו-{b[0] or b[6]} קרובים מדי")
    return issues


# =====================================================================
# ייצוא ורינדור
# =====================================================================
def export_stl(obj, path):
    select_only(obj)
    if hasattr(bpy.ops.wm, "stl_export"):
        bpy.ops.wm.stl_export(filepath=path, export_selected_objects=True, ascii_format=False)
    else:
        bpy.ops.export_mesh.stl(filepath=path, use_selection=True)


def material(name, rgba, rough=0.6):
    m = bpy.data.materials.new(name)
    m.diffuse_color = rgba
    m.roughness = rough
    return m


def set_mat(obj, mat):
    """הבוליאנים משאירים חריץ חומר ריק שהפאות מצביעות עליו - מחליפים את כולם בחומר אחד."""
    obj.data.materials.clear()
    obj.data.materials.append(mat)
    for poly in obj.data.polygons:
        poly.material_index = 0


def render_views(back, front, keys, preview, plate):
    scene = bpy.context.scene
    scene.render.engine = "BLENDER_WORKBENCH"
    scene.display.shading.light = "STUDIO"
    scene.display.shading.color_type = "MATERIAL"
    scene.display.shading.show_cavity = True
    scene.display.shading.show_shadows = True
    if scene.world is None:
        scene.world = bpy.data.worlds.new("World")
    scene.display.shading.cavity_type = "BOTH"
    scene.display.shading.show_specular_highlight = True
    scene.world.color = (0.70, 0.74, 0.80)
    scene.render.resolution_x = 1000
    scene.render.resolution_y = 1000

    # שחור מט + כתום אחד על מה שלוחצים (OK, עוצמה). מקשי מספרים בצבע עצם.
    m_body = material("body_blue", (0.015, 0.05, 0.26, 1), 0.45)
    set_mat(back, m_body)
    set_mat(front, m_body)
    m_key = material("keys_bluegrey", (0.70, 0.78, 0.88, 1), 0.55)
    m_dpad = material("dpad_ring", (0.03, 0.10, 0.40, 1), 0.4)
    m_orange = material("accent_lime", (0.60, 0.80, 0.15, 1), 0.5)
    for k in keys:
        if k.name.startswith("dpad_ring"):
            set_mat(k, m_dpad)
        elif k.name.startswith(("ok_key", "volume_rocker")):
            set_mat(k, m_orange)
        else:
            set_mat(k, m_key)
    set_mat(plate, m_key)
    m_dark = material("internals", (0.01, 0.01, 0.012, 1), 0.2)
    for o in preview:
        set_mat(o, m_dark)
    set_mat(preview[1], material("screen", (0.20, 0.30, 0.14, 1), 0.05))

    target = bpy.data.objects.new("look_at", None)
    scene.collection.objects.link(target)
    cam = bpy.data.objects.new("cam", bpy.data.cameras.new("cam"))
    cam.data.lens = 60
    cam.data.clip_start = 1
    cam.data.clip_end = 5000
    scene.collection.objects.link(cam)
    scene.camera = cam
    t = cam.constraints.new("TRACK_TO")
    t.target = target
    t.track_axis = "TRACK_NEGATIVE_Z"
    t.up_axis = "UP_Y"

    plate.hide_render = True
    base = {o: o.location.copy() for o in [front] + keys + preview}
    views = [
        ("front_straight", (0, -30, 265), (0, -6, 5), 0.0),
        ("front", (60, -140, 320), (0, -5, 5), 0.0),
        ("keypad_closeup", (40, -170, 110), (0, -40, 6), 0.0),
        ("back_and_ports", (-170, 230, -250), (0, 0, 4), 0.0),
        ("exploded", (230, -250, 250), (0, 0, 14), 16.0),
    ]
    paths = []
    for name, loc, look, lift in views:
        for o, p in base.items():
            if o is front:
                k = 1.0
            elif o.name.startswith("volume_rocker"):
                k = 0.0      # המקש הצדדי נשאר בדופן
            else:
                k = 1.8 if o in keys else 0.5
            o.location = p + Vector((0, 0, lift * k))
        cam.location = loc
        target.location = look
        scene.render.filepath = os.path.join(OUT, f"render_{name}.png")
        bpy.ops.render.render(write_still=True)
        paths.append(scene.render.filepath)
    for o, p in base.items():
        o.location = p
    plate.hide_render = False
    return paths


# =====================================================================
def main():
    clear_scene()
    back = build_back_housing()
    front = build_front_housing()
    keys = build_keys_in_place()
    plate = print_plate(keys)
    preview = build_internals_preview()

    report = dict(
        dims=dict(outer=[BODY_W, BODY_L, BODY_T], split_z=round(SPLIT_Z, 2),
                  key_limit_x=round(KEY_LIMIT_X, 2)),
        back_housing=check_mesh(back),
        front_housing=check_mesh(front),
        keycaps=check_mesh(plate),
        design_issues=check_design(),
    )
    stl = {
        "back_housing": os.path.join(OUT, "qin_f22_pro_back_housing.stl"),
        "front_housing": os.path.join(OUT, "qin_f22_pro_front_housing.stl"),
        "keycaps": os.path.join(OUT, "qin_f22_pro_keycaps.stl"),
    }
    export_stl(back, stl["back_housing"])
    export_stl(front, stl["front_housing"])
    export_stl(plate, stl["keycaps"])
    report["stl"] = stl
    report["renders"] = render_views(back, front, keys, preview, plate)

    blend = os.path.join(OUT, "qin_f22_pro_housing.blend")
    bpy.ops.wm.save_as_mainfile(filepath=blend)
    report["blend"] = blend
    with open(os.path.join(OUT, "report.json"), "w", encoding="utf-8") as f:
        json.dump(report, f, ensure_ascii=False, indent=2)
    print(json.dumps(report, ensure_ascii=False, indent=2))
    if not all(report[k]["ok"] for k in ("back_housing", "front_housing", "keycaps")):
        print("!!! רשת לא תקינה - לבדוק את report.json לפני הדפסה")


main()
