# =====================================================================
#  גוף חלופי ל-Qin F22 Pro - גרסה 3: "גרפיט"
#
#  v2 נראה כמו אב-טיפוס: מקשים קטנים שמפוזרים על משטח שחור גדול, פסים שנראים
#  כמו שריטות, חור ריק במקום #, ותמונות Workbench אפורות. v3 בונה שפה אחת:
#    - גוש מקשים אחד: כל המקשים באותו גודל (15.8 × 7.9) ובאותו רדיוס, רווח קבוע
#      1.6 מ"מ ביניהם, והמקלדת ברוחב המסך בדיוק. כמו Nokia 8000 / Punkt.
#    - שני בלוקים: אשכול הניווט (2 שורות + טבעת חצים) ובלוק המספרים, עם רווח
#      קצת גדול יותר ביניהם. אין פסים ואין "חורים" בגריד.
#    - #: אריח זכוכית שחור קבוע עם חלון עגול לסלפי, כדי שהגריד יישאר שלם.
#    - קצה יהלום: שיפוע 45° של 0.7 מ"מ בחזית שתופס אור, צד שטוח, גב R3.2.
#    - צבע: גוף גרפיט סאטן, מקשים שחורים, ספרות לבנות (Bahnschrift), כתום רק
#      על OK ועל מקש העוצמה.
#    - רינדור Cycles עם תאורת סטודיו ומסך דלוק (צילום מסך אמיתי של FutureOS).
#
#  לא השתנו: המידות החיצוניות (147 × 57.5 × 9), כל הפתחים, התקע ההיקפי וכל
#  הערכים ש"לאמת בפירוק". מרכזי המקשים זזים עד ~1.1 מ"מ מהכיפות כדי ליישר את
#  הגריד; הפין של כל מקש נשאר בדיוק מעל הכיפה (NUB offset).
#
#  הרצה:  blender -b -P qin_f22_pro_case_v3.py      פלט: out/body_v3/
#
#  חלקים להדפסה:
#    back_housing   - אמבט אחורי: גב + דפנות, כל הפתחים, מצלמה, לוגו, שקע לאצבע.
#    front_housing  - חזית: חלון מסך עם מדף לזכוכית, חריץ אוזנייה, חלון לכל
#                     מקש ולאריח, ותקע היקפי שנכנס לאמבט.
#    keycaps        - 14 מקשים, טבעת חצים, OK, אריח # ומקש עוצמה. הספרות
#                     חרוטות 0.4 מ"מ - ממלאים בצבע לבן (או החלפת פילמנט).
#
#  !!! לאמת בפירוק (כמו ב-v2): עובי הלוח/מסך/סוללה, KEY_NUB_H, נקודות עיגון,
#      רמקול/חיישנים. ובנוסף: שדה הראייה של הסלפי דרך האריח (חלון Ø6.9,
#      ~2.6 מ"מ מעל העדשה).
#  צירים: X = רוחב (ימין +), Y = אורך (למעלה +), Z = עובי (0 = גב). מ"מ.
# =====================================================================

import bpy
import bmesh
import json
import math
import os
from mathutils import Matrix, Vector
from mathutils.bvhtree import BVHTree

# ---------------------------------------------------------------------
# 1. הגוף המקורי (נמדד)
# ---------------------------------------------------------------------
BODY_L = 147.0
BODY_W = 57.5
BODY_T = 9.0
BODY_R = 7.5          # פינות עגולות יותר (במקור ~5)
BACK_EDGE_R = 3.5     # קצה הגב: עגול ורך ביד
FRONT_EDGE_R = 1.5    # קצה החזית

SCREEN_W = 49.9
SCREEN_H = 74.8
SCREEN_FROM_TOP = 9.2
SCREEN_GAP_TO_KEYS = 1.9

KEY_ROW_H = [8.8, 10.0, 9.9, 9.6, 9.6, 8.9]   # תאי הכיפות המקוריים, מלמעלה למטה
KEY_COL_W = [18.8, 17.7, 19.2]
KEY_LABELS = [
    ["\ue700", "DPAD", "\ue72b"],   # תפריט | D-pad | חזור   (Segoe Fluent Icons)
    ["\ue717", "DPAD", "\ue778"],   # חיוג  | D-pad | ניתוק
    ["1", "2", "3"],
    ["4", "5", "6"],
    ["7", "8", "9"],
    ["*", "0", "#"],
]
SELFIE = dict(from_left=49.8, from_top=138.0, d=6.5)

# ---------------------------------------------------------------------
# 2. מבנה הגוף
# ---------------------------------------------------------------------
WALL = 1.4
BACK = 1.2
FRONT = 1.2
SKIRT_H = 1.5
SKIRT_T = 0.8
SKIRT_GAP = float(os.environ.get("V3_SKIRT_GAP", "0.15"))   # #44: גרסאות סובלנות 0.10/0.15/0.20
SKIRT_GAP_NOMINAL = 0.15

SCREEN_LEDGE_MARGIN = 0.4
# 22.10 (המשתמש): "שלא יהיה כלום בלמעלה רק המדף למסך". אין מסגרת מעל המסך, אין חריץ אוזנייה ואין שיפוע:
# מודול המסך (הזכוכית) יושב בשקע פתוח, בגובה פני החזית, על מדף (רצפת המגש). מדביקים בסרט דו-צדדי כמו בטלפונים.
SCREEN_POCKET_R = 3.0
SCREEN_POCKET_TOP_MARGIN = 1.4   # הזכוכית מעל האזור המואר (כמו המדף הקודם) - לאמת בפירוק
SCREEN_POCKET_SIDE_MARGIN = 1.4
GLASS_LEDGE_MARGIN = 1.4
GLASS_LEDGE_DEPTH = 0.6
EARPIECE = dict(from_top=4.6, w=12.0, h=1.2)

# מקשי גלולה לפי הסקיצה של המשתמש (Frame 1.png, 6.10): גלולות ~2:1, רווח 2 מ"מ בין העמודות
KEY_GAP_X = 2.8             # 25.10 ערב: ריווח גדול יותר בשני הצירים (אופקי 2.8, אנכי ~2.3)
NAV_W, NAV_H, NAV_R = 12.4, 7.4, 1.2   # 30.10 (המשתמש): מקשי הניווט מלבנים, לא ריבועים
KEY_H = 7.2
HASH_WEB = 1.2              # גשר החזית בין חלון # לחלון הסלפי
HASH_NUB_D = 2.4            # המקש # צר: פין קטן יותר כדי שייכנס לחלל שלו
KEY_TOP_CHAMFER = 0.8       # רדיוס הקצה העליון: המקש מעוגל ורך, לא "קופסה"
OK_SIZE = 9.4               # OK עגול
DPAD_NUB_OFFSET = 6.4
SELFIE_HOLE = 6.9           # (לא בשימוש: האריח הוסר)
SELFIE_WIN = 7.3            # 22.10: אין מקש # בחומרה. במקומו רק חלון עגול לעדשת הסלפי (Ø6.5 + 0.4 מכל צד)
KEY_WINDOW_CLEAR = 0.4     # TPU: מרווח קצת גדול יותר מ-PLA
FLANGE_H = 0.8
FLANGE_MARGIN = 1.2
KEY_PROTRUDE = 0.6
KEY_NUB_H = 0.8             # לאמת בפירוק
LABEL_DEPTH = 0.6          # חריטה עמוקה: ב-TPU שקוף למחצה הספרה דקה יותר ולכן מוארת יותר
KEY_TOP_SKIN = 1.0          # המקש חלול מלמטה: נשאר 1.0 מ"מ למעלה (0.4 מתחת לספרה) - לתאורה האחורית

# מגש למסך בכל החלק העליון: הקירות הם התקע ההיקפי שיורד לעומק המודול (כל 4 הצדדים) + קיר תחתון
# מעל המקלדת. הרצפה (screen_tray_floor, ‏PLA) היא חלק נפרד: נכנסת מאחור אחרי המודול ונדבקת.
SCREEN_MODULE_T = 2.4       # עובי מודול המסך (זכוכית + LCD) - לאמת בפירוק
SCREEN_RECESS = 0.8         # 30.10 (המשתמש): המסך שקוע - הזכוכית 0.8 מ"מ מתחת לפני החזית, על מדף
RETAINER_T = 0.6            # עובי רצפת המגש
TRAY_WALL = 0.7
CAP_HOLE = dict(x=-10.0, w=12.0, l=5.0, from_top=0.0)   # 25.10: חור לקבלים בקצה העליון של המדף - מיקום ומידה לאמת בפירוק
CABLE_HOLE = dict(x=15.5, w=13.0, l=6.0, from_top=0.0)   # חור לחוטים של המסך, למעלה מימין (פתוח לקצה הרצפה)
FLOOR_TOP_MARGIN = 1.0      # הרצפה ממשיכה 1 מ"מ מעל הזכוכית; מעליה פתוח - שם האוזנייה, ה-IR והשקע
FLOOR_RIBS = dict(x=(-18.0, 18.0), w=1.0, h=0.4)   # #19: צלעות קשיחות מתחת לרצפת המגש - לוודא מול הלוח בפירוק
AIR_CHANNELS_X = (-16.0, -6.0, 4.0)   # #26: תעלות בקיר העליון של המגש, מהלוח אל חריץ התפר העליון
BATTERY_RIBS = dict(y=(-55.0, -38.0, -21.0), l=40.0, w=0.8, h=0.5)   # #28: מרווח אוויר מתחת לסוללה - לוודא בפירוק
SOC_ZONE = None            # #25: dict(x=, y=, w=, l=) כשיודעים איפה המעבד: גב 1.6 במקום 1.2 שם
BACK_SOC = 1.6
LANYARD = dict(x=-23.5, slot_w=1.8, slot_h=1.2, post_d=1.6, post_in=2.2)   # #38: שרוך דרך התפר, סביב עמוד פנימי
USB_CHAMFER = 0.4          # #40: כניסת USB-C עם שיפוע שמנחה את התקע
REG_LABEL = dict(text="FutureOS  ·  MODEL F22P  ·  CE", size=1.6, from_top=139.5, depth=0.15)   # #33
KEY_DISH = 0.2             # #1: מקשי המספרים קעורים 0.2 מ"מ
KEY_DISH_R = 105.0
BOTTOM_ROW_DROP = 0.4      # #5: שורת * 0 # יורדת 0.4 מ"מ (עוד מרווח נשימה; 0.4 כדי שהסלפי עוד נכנס בחלון)

# קירור בלי חורים גלויים: חריץ דק בקו החיבור בקצה התחתון ובקצה העליון. נראה כמו התפר עצמו;
# האוויר נכנס למטה ויוצא למעלה (ארובה). PLA מתרכך ב-~55°C.
SEAM_VENT_H = 0.45
SEAM_VENTS = [(-BODY_L / 2, -20.0, 20.0), (BODY_L / 2, -20.0, 8.0)]   # (y, x0, x1)
CAM_RING = 1.6               # רוחב טבעת המצלמה
CAM_RAISE = 0.6
LOGO = dict(text="FutureOS", size=4.0, from_top=124.0, depth=0.3)
FINGER = dict(from_top=64.0, w=17.0, l=26.0, depth=0.45, slope=1.5)

FONT_TEXT = os.environ.get("V3_FONT", "C:/Windows/Fonts/seguisb.ttf")
FONT_ICON = "C:/Windows/Fonts/SegoeIcons.ttf"

# ---------------------------------------------------------------------
# 3. פתחים
# ---------------------------------------------------------------------
def from_top(d):
    return BODY_L / 2 - d

PORT_Z = BODY_T / 2
USB = dict(x=0.0, w=9.2, h=3.6)
MIC = dict(x=-8.0, d=1.6)
TOP_WINDOW = dict(x=15.5, w=11.0, h=4.4)
SIM = dict(y=from_top(40.0), l=30.0, h=3.6)
VOLUME = dict(y=from_top(43.0), l=21.0, h=3.2)
REAR_CAM = dict(x=-1.5, y=from_top(25.3), w=10.7, l=21.2)

# ---------------------------------------------------------------------
# 4. נגזרות וגריד המקשים
# ---------------------------------------------------------------------
IN_W = BODY_W - 2 * WALL
IN_L = BODY_L - 2 * WALL
IN_R = max(BODY_R - WALL, 0.5)
SPLIT_Z = BODY_T - FRONT
SKIRT_OUT_W = IN_W - 2 * SKIRT_GAP
SKIRT_OUT_L = IN_L - 2 * SKIRT_GAP
SKIRT_IN_W = SKIRT_OUT_W - 2 * SKIRT_T
SKIRT_IN_L = SKIRT_OUT_L - 2 * SKIRT_T

# 30.10 (המשתמש): בלי מסגרת מעל המסך - השקע נמשך עד הקיר העליון של המגש
SCREEN_POCKET_TOP_MARGIN = SKIRT_IN_L / 2 - 0.3 - from_top(SCREEN_FROM_TOP)
screen_cy = from_top(SCREEN_FROM_TOP) - SCREEN_H / 2
keypad_top = from_top(SCREEN_FROM_TOP + SCREEN_H + SCREEN_GAP_TO_KEYS)
keypad_x0 = -KEY_COL_W[1] / 2 - KEY_COL_W[0]
selfie_x = SELFIE["from_left"] - BODY_W / 2
selfie_y = from_top(SELFIE["from_top"])

# קבוע לפי הסובלנות הנומינלית: גרסאות הסובלנות לא משנות את המקשים
KEY_LIMIT_X = (IN_W - 2 * SKIRT_GAP_NOMINAL - 2 * SKIRT_T) / 2 - FLANGE_MARGIN / 2 - KEY_WINDOW_CLEAR - 0.2


def cell_cy(r):
    return keypad_top - sum(KEY_ROW_H[:r]) - KEY_ROW_H[r] / 2


def cell_cx(c):
    return keypad_x0 + sum(KEY_COL_W[:c]) + KEY_COL_W[c] / 2


# עמודות: 3 מקשים ברוחב שווה, רווח KEY_GAP, הקצה החיצוני ב-KEY_LIMIT_X
KEY_W = (KEY_LIMIT_X - KEY_GAP_X) / 1.5
COL_X = [-(KEY_W + KEY_GAP_X), 0.0, KEY_W + KEY_GAP_X]
KEY_R = KEY_H / 2           # גלולה
# בלוק המספרים: 4 שורות בפסיעה שווה, ממורכז על הכיפות
PITCH = (cell_cy(2) - cell_cy(5)) / 3
KEY_GAP = PITCH - KEY_H     # רווח אנכי
num_mid = (cell_cy(2) + cell_cy(5)) / 2
ROW_Y = {r: num_mid + (3.5 - r) * PITCH for r in range(2, 6)}
ROW_Y[5] -= BOTTOM_ROW_DROP
# אשכול הניווט: טבעת החצים ממורכזת על כיפת ה-D-pad, שני המקשים בכל צד מיושרים אליה
dpad_y = keypad_top - KEY_ROW_H[0]
DPAD_H = 2 * KEY_H + KEY_GAP
ROW_Y[0] = dpad_y + (NAV_H + KEY_GAP) / 2
ROW_Y[1] = dpad_y - (NAV_H + KEY_GAP) / 2
DPAD_D = 17.3                # טבעת החצים: עיגול סימטרי, גדול מרוחב העמודה
# #2: מקשי הניווט ברוחב מלא, והצד הפנימי שלהם עוטף את טבעת החצים בקשת (רווח קבוע KEY_GAP_X)
NAV_LABELS = {KEY_LABELS[0][0], KEY_LABELS[0][2], KEY_LABELS[1][0], KEY_LABELS[1][2]}
DPAD_R = DPAD_D / 2
OK_R = OK_SIZE / 2
dpad_hole = OK_SIZE + 2 * KEY_WINDOW_CLEAR
# קשת מקש # סביב חלון הסלפי: (מקש, פלנג', חלון בחזית, שיפוע חלון)
HASH_ARC_R = SELFIE_WIN / 2 + HASH_WEB + KEY_WINDOW_CLEAR
HASH_ARC = (HASH_ARC_R, SELFIE_WIN / 2 + 0.95, HASH_ARC_R - KEY_WINDOW_CLEAR, HASH_ARC_R - KEY_WINDOW_CLEAR - 0.3)

# [תווית, x, y, רוחב, גובה, רדיוס, סוג, (dx, dy) של הפין מול מרכז המקש]
KEYS = []
for r in range(6):
    for c in range(3):
        lab = KEY_LABELS[r][c]
        if lab == "DPAD":
            continue
        x, y = COL_X[c], ROW_Y[r]
        kw = KEY_W
        kh, kr = KEY_H, KEY_R
        if r < 2:           # מקשי הניווט: כמעט ריבועיים, ממורכזים על העמודה (מתחתם בדיוק מקשי המספרים)
            kw, kh, kr = NAV_W, NAV_H, NAV_R
        nub = (cell_cx(c) - x, cell_cy(r) - y)
        # הפין לא יוצא מתחת למקש
        nub = (max(min(nub[0], kw / 2 - 2.0), -(kw / 2 - 2.0)), nub[1])
        if lab == "#":
            # בטלפון המקורי # יושב משמאל לעדשת הסלפי (תמונה 5.10). חלון עגול לעדשה, ומקש # צר
            # שהצד הימני שלו קשת סביב החלון. הכיפה של # כנראה משמאל לעדשה - לאמת בפירוק.
            sw = SELFIE_WIN - 2 * KEY_WINDOW_CLEAR
            KEYS.append(["selfie", selfie_x, selfie_y, sw, sw, sw / 2, "open", (0, 0)])
            hx0 = x - kw / 2
            reach = kh / 2 + abs(y - selfie_y)
            hx1 = selfie_x - math.sqrt(max(HASH_ARC_R ** 2 - reach ** 2, 0.0))
            hw, hx = hx1 - hx0, (hx0 + hx1) / 2
            # הפין 0.8 שמאלה ובקוטר 2.4 (HASH_NUB_D): לא משיק לאף דופן של החלל (משיק = צלעות פתוחות)
            KEYS.append([lab, hx, y, hw, kh, kr, "key", (-0.8, cell_cy(r) - y)])
            continue
        KEYS.append([lab, x, y, kw, kh, kr, "key", nub])
KEYS.append(["", 0.0, dpad_y, DPAD_D, DPAD_D, DPAD_R, "dpad", (0, 0)])
KEYS.append(["", 0.0, dpad_y, OK_SIZE, OK_SIZE, OK_R, "ok", (0, 0)])

HERE = os.path.dirname(os.path.abspath(__file__)) if "__file__" in globals() and __file__ else os.getcwd()
OUT = os.path.join(HERE, "out", "body_v3")
os.makedirs(OUT, exist_ok=True)
SCREEN_IMG = os.path.normpath(os.path.join(HERE, "..", "..", "Sfarim", "tools", "output", "v3_home5.png"))
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


def rounded_body(name):
    """מסגרת שטוחה (כמו iPhone / Galaxy): צד ישר, עיגול קטן בגב ובחזית."""
    prof = []
    n, R = 8, BACK_EDGE_R
    for i in range(n + 1):
        t = math.radians(90 * i / n)
        prof.append((R - R * math.sin(t), R - R * math.cos(t)))
    n, R = 6, FRONT_EDGE_R
    for i in range(n, -1, -1):
        t = math.radians(90 * i / n)
        prof.append((R - R * math.sin(t), BODY_T - (R - R * math.cos(t))))
    rings = [rr_ring(BODY_W - 2 * a, BODY_L - 2 * a, BODY_R - a, z, segs=16) for a, z in prof]
    return loft_rings(name, rings)


def stadium_cutter(name, length, height, depth, loc, axis):
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
    others = [o for o in others if o is not None and len(o.data.vertices) > 0]
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


def self_fix(obj):
    """פותר חפיפות פנימיות ברשת: בוליאני EXACT עם use_self מול קובייה רחוקה (לא מסיר כלום מהחלק)."""
    far = rr_prism("far", 0.1, 0.1, 0.02, 500, 500.1, loc=(500, 500, 0))
    mod = obj.modifiers.new("selffix", "BOOLEAN")
    mod.operation = "DIFFERENCE"
    mod.solver = "EXACT"
    mod.use_self = True
    mod.object = far
    select_only(obj)
    bpy.ops.object.modifier_apply(modifier=mod.name)
    bpy.data.objects.remove(far, do_unlink=True)
    # קודקודים שנשארו במרחק של מיקרונים זה מזה (שאריות של חיתוך השקע) מתאחדים
    bm = bmesh.new()
    bm.from_mesh(obj.data)
    bmesh.ops.remove_doubles(bm, verts=bm.verts, dist=0.003)
    bmesh.ops.dissolve_degenerate(bm, dist=0.003, edges=bm.edges)
    bm.to_mesh(obj.data)
    bm.free()


def clean_degenerate(obj):
    bm = bmesh.new()
    bm.from_mesh(obj.data)
    bmesh.ops.dissolve_degenerate(bm, dist=1e-4, edges=bm.edges)
    bm.to_mesh(obj.data)
    bm.free()


_fonts = {}


def font(path):
    if path not in _fonts:
        try:
            _fonts[path] = bpy.data.fonts.load(path, check_existing=True)
        except Exception:
            _fonts[path] = None
    return _fonts[path]


def text_mesh(text, size, z_mid, half_depth, font_path=FONT_TEXT, name=None, spacing=1.0):
    """טקסט כרשת סגורה, ממורכז ב-(0,0), בעובי 2*half_depth סביב z_mid."""
    cu = bpy.data.curves.new("label", "FONT")
    cu.body = text
    cu.size = size
    cu.align_x = "CENTER"
    cu.align_y = "CENTER"
    cu.extrude = half_depth
    cu.space_character = spacing
    f = font(font_path)
    if f is not None:
        cu.font = f
    tmp = bpy.data.objects.new("label_tmp", cu)
    bpy.context.scene.collection.objects.link(tmp)
    dg = bpy.context.evaluated_depsgraph_get()
    mesh = bpy.data.meshes.new_from_object(tmp.evaluated_get(dg))
    bpy.data.objects.remove(tmp, do_unlink=True)
    bpy.data.curves.remove(cu)
    bm = bmesh.new()
    bm.from_mesh(mesh)
    bmesh.ops.remove_doubles(bm, verts=bm.verts, dist=1e-4)
    bmesh.ops.recalc_face_normals(bm, faces=bm.faces)
    bm.to_mesh(mesh)
    bm.free()
    obj = bpy.data.objects.new(name or f"label_{text}", mesh)
    bpy.context.scene.collection.objects.link(obj)
    obj.location = (0, 0, z_mid)
    return obj


def is_icon(lab):
    return len(lab) == 1 and ord(lab) >= 0xE000


def legend_size(lab):
    if is_icon(lab):
        return 4.4
    if lab == "*":
        return 7.4
    return 5.8


def clear_scene():
    bpy.ops.wm.read_factory_settings(use_empty=True)
    s = bpy.context.scene
    s.unit_settings.system = "METRIC"
    s.unit_settings.scale_length = 0.001
    s.unit_settings.length_unit = "MILLIMETERS"


def chamfer_cutter(name, w, l, r, z_face, inward, c, loc=(0, 0, 0)):
    rings = [rr_ring(w + 2 * c + 2, l + 2 * c + 2, r + c + 1, z_face - inward * 1.0),
             rr_ring(w + 2 * c, l + 2 * c, r + c, z_face),
             rr_ring(w, l, max(r, 0.3), z_face + inward * c)]
    if inward < 0:
        rings.reverse()
    return loft_rings(name, rings, loc)


def finger_dimple():
    w, l, d, s = FINGER["w"], FINGER["l"], FINGER["depth"], FINGER["slope"]
    rings = [rr_ring(w + 2, l + 2, (w + 2) / 2, -1.0),
             rr_ring(w, l, w / 2, 0.0),
             rr_ring(w - 2 * s, l - 2 * s, (w - 2 * s) / 2, d)]
    return loft_rings("finger_dimple", rings, (0, from_top(FINGER["from_top"]), 0))


def mirrored_text_cutter(text, size, depth, loc):
    obj = text_mesh(text, size, 0.0, depth)
    obj.rotation_euler = (0, math.radians(180), 0)
    obj.location = loc
    return obj


# =====================================================================
# החלקים
# =====================================================================
def port_cutters():
    depth = 2 * (WALL + SKIRT_GAP + SKIRT_T) + 1
    return [
        stadium_cutter("usb", USB["w"], USB["h"], depth, (USB["x"], -BODY_L / 2, PORT_Z), "Y"),
        stadium_cutter("mic", MIC["d"], MIC["d"], depth, (MIC["x"], -BODY_L / 2, PORT_Z), "Y"),
        stadium_cutter("top_window", TOP_WINDOW["w"], TOP_WINDOW["h"], depth,
                       (TOP_WINDOW["x"], BODY_L / 2, PORT_Z), "Y"),
        stadium_cutter("sim", SIM["l"], SIM["h"], depth, (-BODY_W / 2, SIM["y"], PORT_Z), "X"),
        stadium_cutter("volume", VOLUME["l"], VOLUME["h"], depth, (BODY_W / 2, VOLUME["y"], PORT_Z), "X"),
        usb_chamfer(),
    ]


def usb_chamfer():
    """#40: שיפוע 45° סביב פתח ה-USB-C מבחוץ."""
    w, h, c = USB["w"], USB["h"], USB_CHAMFER
    obj = loft_rings("usb_chamfer", [rr_ring(w, h, h / 2, -0.4), rr_ring(w + 2 * c, h + 2 * c, h / 2 + c, 0.0),
                                      rr_ring(w + 2 * c + 2, h + 2 * c + 2, h / 2 + c + 1, 1.0)])
    obj.rotation_euler = (math.radians(90), 0, 0)
    obj.location = (USB["x"], -BODY_L / 2, PORT_Z)
    return obj


def build_back_housing():
    tub = rounded_body("back_housing")
    boolean(tub, [rr_prism("below_split", BODY_W + 10, BODY_L + 10, 1, -1, SPLIT_Z)], "INTERSECT")
    # אי מצלמה: טבעת בולטת 0.6 מ"מ סביב העדשה, כמו ב-iPhone / Galaxy
    isl_w, isl_l = REAR_CAM["w"] + CAM_RING * 2, REAR_CAM["l"] + CAM_RING * 2
    island = loft_rings("cam_island", [rr_ring(isl_w, isl_l, isl_w / 2, 0.5),
                                       rr_ring(isl_w, isl_l, isl_w / 2, -CAM_RAISE + 0.25),
                                       rr_ring(isl_w - 0.5, isl_l - 0.5, isl_w / 2 - 0.25, -CAM_RAISE)],
                        loc=(REAR_CAM["x"], REAR_CAM["y"], 0))
    boolean(tub, [island], "UNION")
    cutters = port_cutters() + seam_vents(False) + [
        rr_prism("cavity", IN_W, IN_L, IN_R, BACK, SPLIT_Z + 1),
        rr_prism("rear_cam", REAR_CAM["w"], REAR_CAM["l"], REAR_CAM["w"] / 2, -2, BACK + 1,
                 loc=(REAR_CAM["x"], REAR_CAM["y"], 0)),
        mirrored_text_cutter(LOGO["text"], LOGO["size"], LOGO["depth"], (0, from_top(LOGO["from_top"]), 0)),
    ]
    boolean(tub, cutters, "DIFFERENCE")
    adds = [rr_prism(f"bat_rib{i}", BATTERY_RIBS["l"], BATTERY_RIBS["w"], BATTERY_RIBS["w"] / 2, BACK - 0.01,
                     BACK + BATTERY_RIBS["h"], loc=(0, by, 0)) for i, by in enumerate(BATTERY_RIBS["y"])]
    # עמוד לשרוך בפינה התחתונה-שמאלית, מאחורי החריץ בתפר
    ly = -IN_L / 2 + LANYARD["post_in"]
    adds.append(rr_prism("lanyard_post", LANYARD["post_d"], LANYARD["post_d"], LANYARD["post_d"] / 2, BACK - 0.01,
                         SPLIT_Z - SKIRT_H - 0.3, loc=(LANYARD["x"], ly, 0)))
    if SOC_ZONE:
        adds.append(rr_prism("soc_pad", SOC_ZONE["w"], SOC_ZONE["l"], 1.0, BACK - 0.01, BACK_SOC,
                             loc=(SOC_ZONE["x"], SOC_ZONE["y"], 0)))
    boolean(tub, adds, "UNION")
    clean_degenerate(tub)
    # התווית אחרונה ובלי clean_degenerate: האותיות הקטנות מתפרקות בניקוי ומשאירות צלעות פתוחות
    if not FIT_ONLY:    # בבדיקת ההתאמה החיתוך עובר דרך האותיות ומשאיר צלעות פתוחות
        boolean(tub, [mirrored_text_cutter(REG_LABEL["text"], REG_LABEL["size"], REG_LABEL["depth"],
                                           (0, from_top(REG_LABEL["from_top"]), 0))], "DIFFERENCE")
    return tub


def screen_pocket():
    """השקע הפתוח לזכוכית: (רוחב, y תחתון, y עליון)."""
    win_bot = screen_cy - SCREEN_H / 2 - SCREEN_LEDGE_MARGIN
    return (SCREEN_W + 2 * SCREEN_POCKET_SIDE_MARGIN, win_bot - 0.15,
            from_top(SCREEN_FROM_TOP) + SCREEN_POCKET_TOP_MARGIN)


def tray_geom():
    """גבולות המגש: (y_תחתון_פנימי, y_עליון_פנימי, עומק מתחת לקו החיבור).
    הזכוכית בגובה החזית, ולכן המדף (הרצפה) גבוה: BODY_T - מודול - רצפה."""
    _, y_bot, _ = screen_pocket()
    floor_bottom = BODY_T - SCREEN_RECESS - SCREEN_MODULE_T - RETAINER_T
    return y_bot, SKIRT_IN_L / 2, SPLIT_Z - floor_bottom


def tray_shape(name, w, y0, y1, r_top, z0, z1):
    """מלבן מ-y0 עד y1: פינות עליונות ברדיוס r_top (כמו הגוף), תחתונות כמעט חדות."""
    a = rr_prism(name, w, y1 - y0 - 6, r_top, z0, z1, loc=(0, (y0 + 6 + y1) / 2, 0))
    b = rr_prism(name + "_b", w, 12, 0.4, z0, z1, loc=(0, y0 + 6, 0))
    boolean(a, [b], "UNION")
    return a


def build_tray_walls():
    """קירות המגש בכל 4 הצדדים בעומק המודול + הרצפה: התקע ההיקפי יורד עמוק, וקיר תחתון מעל המקלדת."""
    y0, y1, depth = tray_geom()
    box = tray_shape("tray", SKIRT_OUT_W, y0 - TRAY_WALL, SKIRT_OUT_L / 2, max(IN_R - SKIRT_GAP, 0.5),
                     SPLIT_Z - depth, SPLIT_Z + 0.01)
    inner = tray_shape("tray_in", SKIRT_IN_W, y0, y1, max(IN_R - SKIRT_GAP - SKIRT_T, 0.3),
                       SPLIT_Z - depth - 1, SPLIT_Z + 0.02)
    boolean(box, [inner], "DIFFERENCE")
    return box


def build_screen_tray_floor():
    """רצפת המגש (PLA): לוח מלא שנכנס מאחור מתחת למודול ונדבק לקירות. חור חוטים למעלה מימין."""
    y0, _, depth = tray_geom()
    g = 0.15
    y1 = min(screen_pocket()[2] + FLOOR_TOP_MARGIN + 0.15, SKIRT_IN_L / 2)
    z1 = BODY_T - SCREEN_RECESS - SCREEN_MODULE_T
    # מלבן אחד (tray_shape איחד שני חלקים עם דפנות באותו מישור והשאיר צלעות פתוחות)
    floor = rr_prism("screen_tray_floor", SKIRT_IN_W - 2 * g, (y1 - g) - (y0 + g), 0.8, z1 - RETAINER_T, z1,
                     loc=(0, (y0 + y1) / 2, 0))
    ch = CABLE_HOLE
    cy = y1 - g - ch["from_top"] - ch["l"] / 2
    ribs_len = (y1 - y0) - 14
    boolean(floor, [rr_prism(f"floor_rib{i}", FLOOR_RIBS["w"], ribs_len, FLOOR_RIBS["w"] / 2,
                             z1 - RETAINER_T - FLOOR_RIBS["h"], z1 - RETAINER_T + 0.01, loc=(rx, (y0 + y1) / 2 - 2, 0))
                    for i, rx in enumerate(FLOOR_RIBS["x"])], "UNION")
    cp = CAP_HOLE
    cpy = y1 - g - cp["from_top"] - cp["l"] / 2
    boolean(floor, [rr_prism("cable_hole", ch["w"], ch["l"] + 2, 2.0, z1 - RETAINER_T - 1, z1 + 1,
                             loc=(ch["x"], cy + 1, 0)),
                    rr_prism("cap_hole", cp["w"], cp["l"] + 2, 2.0, z1 - RETAINER_T - 1, z1 + 1,
                             loc=(cp["x"], cpy + 1, 0))], "DIFFERENCE")
    clean_degenerate(floor)
    return floor


def seam_vents(for_front):
    """חריץ אוורור בקו החיבור (נראה כמו התפר). בגב חותך את שפת הדופן, בחזית את התקע שמאחוריה."""
    out = []
    for i, (y, x0, x1) in enumerate(SEAM_VENTS):
        d = 2 * (WALL + SKIRT_GAP + SKIRT_T) + 1
        z1 = SPLIT_Z if for_front else SPLIT_Z + 1
        out.append(rr_prism(f"seam_vent{i}", x1 - x0, d, 0.2, SPLIT_Z - SEAM_VENT_H, z1, loc=((x0 + x1) / 2, y, 0)))
    d = 2 * (WALL + SKIRT_GAP + SKIRT_T) + 1
    lz1 = SPLIT_Z if for_front else SPLIT_Z + 1
    out.append(rr_prism("lanyard_slot", LANYARD["slot_w"], d, 0.4, SPLIT_Z - LANYARD["slot_h"], lz1,
                        loc=(LANYARD["x"], -BODY_L / 2, 0)))
    if for_front:   # #26: תעלות בקיר העליון של המגש, מתחת לרצפה ועד חריץ התפר
        y0, y1, depth = tray_geom()
        for i, ax in enumerate(AIR_CHANNELS_X):
            out.append(rr_prism(f"air_ch{i}", 4.0, 3.0, 0.5, SPLIT_Z - depth - 0.5, SPLIT_Z - 0.2,
                                loc=(ax, y1 + 0.4, 0)))
    return out


def build_front_housing():
    plate = rounded_body("front_housing")
    boolean(plate, [rr_prism("above_split", BODY_W + 10, BODY_L + 10, 1, SPLIT_Z, BODY_T + 1)], "INTERSECT")
    skirt = rr_prism("skirt", SKIRT_OUT_W, SKIRT_OUT_L, max(IN_R - SKIRT_GAP, 0.5), SPLIT_Z - SKIRT_H, SPLIT_Z + 0.01)
    skirt_hole = rr_prism("skirt_hole", SKIRT_IN_W, SKIRT_IN_L, max(IN_R - SKIRT_GAP - SKIRT_T, 0.3),
                          SPLIT_Z - SKIRT_H - 1, SPLIT_Z)
    boolean(skirt, [skirt_hole], "DIFFERENCE")
    boolean(plate, [skirt, build_tray_walls()], "UNION")

    cutters = port_cutters() + seam_vents(True)
    # שקע פתוח לזכוכית בגובה החזית. בלי מסגרת מעל המסך, בלי שיפוע ובלי חריץ אוזנייה:
    # הצליל של האוזנייה יוצא בחריץ התפר העליון (SEAM_VENTS, x -20..+8), שעובר בדיוק מעליה.
    pw, py0, py1 = screen_pocket()
    cutters.append(rr_prism("screen_pocket", pw, py1 - py0, SCREEN_POCKET_R, SPLIT_Z - 3, BODY_T + 1,
                            loc=(0, (py0 + py1) / 2, 0)))
    for lab, x, y, w, h, r, kind, _ in KEYS:
        if kind == "ok":
            continue        # ה-OK בולט דרך החלון של טבעת החצים
        c = KEY_WINDOW_CLEAR
        win = rr_prism(f"win_{kind}", w + 2 * c, h + 2 * c, r + c, SPLIT_Z - 1, BODY_T + 1, loc=(x, y, 0))
        # שיפוע 0.3 בשפת החלון: הגשרים בין המקשים נראים כמו מסגרת מדויקת ולא כמו חיתוך
        wch = chamfer_cutter(f"win_ch_{kind}", w + 2 * c, h + 2 * c, r + c, BODY_T, -1, 0.3, loc=(x, y, 0))
        if lab == "#":      # החלון של # נעצר בקשת, ומשאיר גשר HASH_WEB לחלון הסלפי
            _, _, r_win, r_wch = HASH_ARC
            boolean(win, [rr_prism("arc_w", 2 * r_win, 2 * r_win, r_win, SPLIT_Z - 2, BODY_T + 2,
                                   loc=(selfie_x - x, selfie_y - y, 0))], "DIFFERENCE")
            boolean(wch, [rr_prism("arc_wc", 2 * r_wch, 2 * r_wch, r_wch, SPLIT_Z - 2, BODY_T + 2,
                                   loc=(selfie_x - x, selfie_y - y, 0))], "DIFFERENCE")
        cutters += [win, wch]
    boolean(plate, cutters, "DIFFERENCE")
    clean_degenerate(plate)
    return plate


def chamfered_cap(name, w, h, r, z0, z1, c=KEY_TOP_CHAMFER):
    """מקש עם קצה עליון מעוגל ברדיוס c (רבע עיגול), לא שיפוע חד."""
    rings = [rr_ring(w, h, r, z0)]
    for i in range(7):
        t = math.radians(90 * i / 6)
        a = c * (1 - math.cos(t))
        rings.append(rr_ring(w - 2 * a, h - 2 * a, max(r - a, 0.3), z1 - c + c * math.sin(t)))
    return loft_rings(name, rings)


def key_heights():
    z_flange = KEY_NUB_H
    return z_flange, z_flange + FLANGE_H, KEY_NUB_H + FLANGE_H + FRONT + KEY_PROTRUDE


# אותיות לפי המקלדת הישראלית: עברית מעל, לועזית מתחת
KEY_LETTERS = {"2": ("דהו", "ABC"), "3": ("אבג", "DEF"), "4": ("מנ", "GHI"), "5": ("יכל", "JKL"),
               "6": ("זחט", "MNO"), "7": ("רשת", "PQRS"), "8": ("צק", "TUV"), "9": ("סעפ", "WXYZ")}


def legend_parts(lab):
    """[(טקסט, גודל, dx, dy, גופן)]. העברית הפוכה כי Blender כותב משמאל לימין (נבדק).
    #12: כל הספרות ממורכזות על אותו טור (x=-3.3), כך שהן מיושרות כמו ספרות טבלאיות."""
    if is_icon(lab):
        return [(lab, 4.3, 0.0, 0.0, FONT_ICON)]
    if lab in KEY_LETTERS:
        he, en = KEY_LETTERS[lab]
        return [(lab, 5.8, -3.3, 0.0, FONT_TEXT),
                (he[::-1], 2.7, 3.2, 1.15, FONT_TEXT),
                (en, 2.2, 3.2, -1.35, FONT_TEXT)]
    if lab == "1":      # #9: דואר קולי
        return [(lab, 5.8, -3.3, 0.0, FONT_TEXT), ("voicemail", 1.0, 3.2, 0.0, "GEO")]
    if lab == "0":      # #8: רווח
        return [(lab, 5.8, -3.3, 0.0, FONT_TEXT), ("space", 1.0, 3.2, -0.2, "GEO")]
    if lab == "*":      # #10: + לחיוג בינלאומי
        return [(lab, 7.4, -3.3, 0.0, FONT_TEXT), ("+", 3.6, 3.2, 0.1, FONT_TEXT)]
    if lab == "#":      # ממורכז על החלק הרחב של המקש, הרחק מהקשת
        return [(lab, 5.2, -0.9, 0.0, FONT_TEXT)]
    return [(lab, 5.8, 0.0, 0.0, FONT_TEXT)]


def poly_prism(name, pts, z0, z1):
    """מנסרה ממצולע סגור (נגד כיוון השעון)."""
    mesh = bpy.data.meshes.new(name)
    bm = bmesh.new()
    bot = [bm.verts.new((x, y, z0)) for x, y in pts]
    top = [bm.verts.new((x, y, z1)) for x, y in pts]
    bm.faces.new(list(reversed(bot)))
    bm.faces.new(top)
    n = len(pts)
    for i in range(n):
        j = (i + 1) % n
        bm.faces.new((bot[i], bot[j], top[j], top[i]))
    bmesh.ops.recalc_face_normals(bm, faces=bm.faces)
    bm.to_mesh(mesh)
    bm.free()
    obj = bpy.data.objects.new(name, mesh)
    bpy.context.scene.collection.objects.link(obj)
    return obj


def geo_glyph(kind, z0, z1, name):
    """סמלים שאין בגופן, כקווים בעובי 0.4 (מודפסים בפיה 0.2)."""
    if kind == "space":     # צורת U אחת (בלי איחוד של פסים: האיחוד השאיר חפיפות זעירות)
        pts = [(-1.3, -0.8), (1.3, -0.8), (1.3, 0.3), (0.9, 0.3), (0.9, -0.4), (-0.9, -0.4), (-0.9, 0.3), (-1.3, 0.3)]
        return poly_prism(name, pts, z0, z1)
    else:   # voicemail: שתי טבעות ופס מתחת
        parts = []
        for i, sx in enumerate((-1.05, 1.05)):
            ring = rr_prism(f"{name}c{i}", 1.8, 1.8, 0.9, z0, z1, loc=(sx, 0.1, 0))
            boolean(ring, [rr_prism(f"{name}h{i}", 1.0, 1.0, 0.5, z0 - 1, z1 + 1, loc=(sx, 0.1, 0))], "DIFFERENCE")
            parts.append(ring)
        parts.append(rr_prism(name + "bar", 2.1, 0.4, 0.1, z0, z1, loc=(0, -0.55, 0)))
    base = parts[0]
    boolean(base, parts[1:], "UNION")
    base.name = name
    return base


def legend_meshes(lab, z0, z1, name):
    objs = []
    for i, (t, size, dx, dy, fp) in enumerate(legend_parts(lab)):
        if fp == "GEO":
            o = geo_glyph(t, z0, z1, f"{name}{i}")
        else:
            # אותיות קטנות: ריווח 1.12 כדי שהאותיות לא ייגעו זו בזו (ל ו-כ נגעו)
            o = text_mesh(t, size, (z0 + z1) / 2, (z1 - z0) / 2, fp, name=f"{name}{i}",
                          spacing=1.12 if size < 3 else 1.0)
        if fp == FONT_ICON:     # גופן האייקונים לא ממורכז לפי תיבת הגבולות שלו: ממרכזים לפי הרשת עצמה
            xs = [v.co.x for v in o.data.vertices]
            ys = [v.co.y for v in o.data.vertices]
            o.data.transform(Matrix.Translation((-(min(xs) + max(xs)) / 2, -(min(ys) + max(ys)) / 2, 0)))
        o.location.x, o.location.y = o.location.x + dx, o.location.y + dy
        if lab in NAV_LABELS:   # האייקון זז קצת החוצה, הרחק מהקשת
            o.location.x += legend_nav_shift
        objs.append(o)
    return objs


legend_nav_shift = 0.0


def dish_cutter(zt, name, u=160):
    """#1: כדור גדול שחותך שקע של KEY_DISH במרכז המקש."""
    mesh = bpy.data.meshes.new(name)
    bm = bmesh.new()
    bmesh.ops.create_uvsphere(bm, u_segments=u, v_segments=u // 2, radius=KEY_DISH_R)
    bm.to_mesh(mesh)
    bm.free()
    obj = bpy.data.objects.new(name, mesh)
    bpy.context.scene.collection.objects.link(obj)
    obj.location = (0, 0, zt + KEY_DISH_R - KEY_DISH)
    return obj


def _unused():
    objs = []
    for o in objs:
        objs.append(o)
    return objs


def nav_arc_radii():
    """רדיוסי הקשת של מקשי הניווט סביב טבעת החצים: (מקש, פלנג', חלון, שיפוע חלון)."""
    r_cap = DPAD_R + KEY_GAP_X
    return r_cap, DPAD_R + FLANGE_MARGIN / 2 + 0.4, r_cap - KEY_WINDOW_CLEAR, r_cap - KEY_WINDOW_CLEAR - 0.3


def build_keycap(lab, w, h, r, nub_xy, wrap=None):
    """מקש במקור (0,0); תחתית הפין ב-z=0 (על הכיפה), הפלנג' נוגע בתחתית החזית.
    wrap = (dx, dy) של מרכז העיגול שהמקש עוטף (מקש # סביב חלון הסלפי)."""
    global legend_nav_shift
    zf, zb, zt = key_heights()
    cap = chamfered_cap(f"key_{ord(lab[0]):x}", w, h, r, zb - 0.01, zt)
    flange = rr_prism("flange", w + FLANGE_MARGIN, h + FLANGE_MARGIN, r + FLANGE_MARGIN / 2, zf - 0.01, zb)
    # הפין חודר 0.2 לתוך הפלנג': חפיפה של 0.01 בלבד השאירה צלעות פתוחות במקש # (שם הפין חוצה את דופן החלל)
    nd = HASH_NUB_D if wrap else 3.0
    nub = rr_prism("nub", nd, nd, nd / 2, 0.0, zf + 0.2, loc=(nub_xy[0], nub_xy[1], 0))
    adds = [flange, nub]
    boolean(cap, adds, "UNION")
    legend_nav_shift = 0.0
    if wrap:
        r_cap, r_fl, _, _ = HASH_ARC
        boolean(cap, [rr_prism("arc_cap", 2 * r_cap, 2 * r_cap, r_cap, zb - 0.02, zt + 1, loc=(wrap[0], wrap[1], 0)),
                      rr_prism("arc_fl", 2 * r_fl, 2 * r_fl, r_fl, -1, zb + 0.01, loc=(wrap[0], wrap[1], 0))],
                "DIFFERENCE")
    # חלול מלמטה: חלל לאור של הלדים, שיוצא רק דרך הספרות השקופות
    pw, ph = w - 2.4, h - 2.4
    pocket = rr_prism("light_pocket", pw, ph, max(min(r - 1.2, ph / 2), 0.3), zf - 1, zt - KEY_TOP_SKIN)
    if wrap:            # החלל לא פורץ את הדופן הקשתית: דופן 0.9
        rp = HASH_ARC[0] + 0.9
        boolean(pocket, [rr_prism("arc_pocket", 2 * rp, 2 * rp, rp, zf - 2, zt + 1, loc=(wrap[0], wrap[1], 0))],
                "DIFFERENCE")
    boolean(cap, [pocket], "DIFFERENCE")
    boolean(cap, [rr_prism("nub_post", nd, nd, nd / 2, 0.0, zt - KEY_TOP_SKIN + 0.01, loc=(nub_xy[0], nub_xy[1], 0))],
            "UNION")
    dished = not is_icon(lab)
    if dished:
        boolean(cap, [dish_cutter(zt, "dish")], "DIFFERENCE")
    # המקש אטום (TPU שחור); הספרות והאותיות הן חורים דרך כל העליון שמתמלאים ב-TPU שקוף (הדפסה דו-חומרית)
    boolean(cap, legend_meshes(lab, zt - KEY_TOP_SKIN - 0.1, zt + 0.5, "legend_cut"), "DIFFERENCE")
    clean_degenerate(cap)
    for ins in legend_meshes(lab, zt - KEY_TOP_SKIN, zt, f"legend_{ord(lab[0]):x}_"):
        if dished:      # המילוי השקוף בגובה פני השקע
            boolean(ins, [dish_cutter(zt, "dish_i", u=224)], "DIFFERENCE")   # 224: ב-160 נשארו שבבים של מיקרון ב-6
            self_fix(ins)
        ins.parent = cap
    return cap


def build_selfie_tile(x, y, w, h, r):
    """אריח #: מקש קבוע (בלי פין) עם חלון עגול לעדשת הסלפי. מודבק בפלנג' מתחת לחזית."""
    zf, zb, zt = key_heights()
    tile = chamfered_cap("selfie_tile", w, h, r, zb - 0.01, zt)
    flange = rr_prism("flange", w + FLANGE_MARGIN, h + FLANGE_MARGIN, r + FLANGE_MARGIN / 2, zf, zb)
    boolean(tile, [flange], "UNION")
    lx, ly = selfie_x - x, selfie_y - y
    boolean(tile, [rr_prism("selfie_hole", SELFIE_HOLE, SELFIE_HOLE, SELFIE_HOLE / 2, zf - 1, zt + 1, loc=(lx, ly, 0)),
                   chamfer_cutter("selfie_ch", SELFIE_HOLE, SELFIE_HOLE, SELFIE_HOLE / 2, zt, -1, 0.3, loc=(lx, ly, 0))],
            "DIFFERENCE")
    clean_degenerate(tile)
    return tile


def build_dpad_ring():
    zf, zb, zt = key_heights()
    ring = chamfered_cap("dpad_ring", DPAD_D, DPAD_D, DPAD_R, zb - 0.01, zt)
    flange = rr_prism("flange", DPAD_D + FLANGE_MARGIN, DPAD_D + FLANGE_MARGIN, DPAD_R + FLANGE_MARGIN / 2,
                      zf - 0.01, zb)
    nubs = [rr_prism(f"nub{i}", 1.6, 1.6, 0.8, 0.0, zf, loc=(dx * DPAD_NUB_OFFSET, dy * DPAD_NUB_OFFSET, 0))
            for i, (dx, dy) in enumerate(((1, 0), (-1, 0), (0, 1), (0, -1)))]
    boolean(ring, [flange] + nubs, "UNION")
    boolean(ring, [rr_prism("ok_hole", dpad_hole, dpad_hole, OK_R + KEY_WINDOW_CLEAR, -1, zt + 1),
                   chamfer_cutter("ok_hole_ch", dpad_hole, dpad_hole, OK_R + KEY_WINDOW_CLEAR, zt, -1, 0.3)],
            "DIFFERENCE")
    clean_degenerate(ring)
    return ring


def build_ok_key():
    zf, zb, zt = key_heights()
    ok = chamfered_cap("ok_key", OK_SIZE, OK_SIZE, OK_R, FLANGE_H - 0.01, zt)
    flange = rr_prism("flange", dpad_hole + 0.5, dpad_hole + 0.5, OK_R + 0.6, 0.0, FLANGE_H)
    boolean(ok, [flange], "UNION")
    clean_degenerate(ok)
    return ok


def build_volume_rocker():
    ln, ht = VOLUME["l"] - 0.6, VOLUME["h"] - 0.6
    body = rr_prism("volume_rocker", ht, ln, ht / 2, -0.01, WALL + 0.6)
    flange = rr_prism("rocker_flange", ht + 1.4, ln + 2.0, ht / 2 + 0.7, -0.6, 0.0)
    nubs = [rr_prism(f"rocker_nub{i}", 1.6, 1.6, 0.8, -1.4, -0.59, loc=(0, s * ln / 4, 0)) for i, s in ((0, -1), (1, 1))]
    boolean(body, [flange] + nubs, "UNION")
    boolean(body, [rr_prism("rocker_notch", ht + 1, 1.0, 0.3, WALL + 0.3, WALL + 1.0)], "DIFFERENCE")
    clean_degenerate(body)
    return body


def build_keys_in_place():
    caps = []
    z_nub = SPLIT_Z - FLANGE_H - KEY_NUB_H
    only = FIT_KEYS if FIT_ONLY else None
    for lab, x, y, w, h, r, kind, nub in KEYS:
        if only is not None and lab not in only:
            continue
        if kind == "key":
            wrap = (selfie_x - x, selfie_y - y) if lab == "#" else None
            cap = build_keycap(lab, w, h, r, nub, wrap)
        elif kind == "open":
            continue        # #: חלון גלולה פתוח מעל מצלמת הסלפי
        elif kind == "dpad":
            cap = build_dpad_ring()
        else:
            cap = build_ok_key()
        cap.location = (x, y, z_nub)
        caps.append(cap)
    rocker = build_volume_rocker()
    rocker.rotation_euler = (0, math.radians(90), 0)
    rocker.location = (IN_W / 2, VOLUME["y"], PORT_Z)
    caps.append(rocker)
    return caps


def print_plate(parts):
    """שני קבצים באותן קואורדינטות: המקשים (TPU שחור) והספרות (TPU שקוף) - טוענים יחד ב-slicer דו-חומרי."""
    copies, legends = [], []
    cols, step_x, step_y = 5, KEY_W + 5, DPAD_D + 4
    for i, p in enumerate(parts):
        c = p.copy()
        c.data = p.data.copy()
        c.parent = None
        bpy.context.scene.collection.objects.link(c)
        c.rotation_euler = (0, 0, 0)
        if c.name.startswith("volume_rocker"):
            c.location = (BODY_W + 20, BODY_L / 2 - 10 - (len(parts) // cols + 1) * step_y - 6, 1.4)
        else:
            c.location = (BODY_W + 20 + (i % cols) * step_x, BODY_L / 2 - 10 - (i // cols) * step_y, 0)
        copies.append(c)
        for ch in p.children:
            lc = ch.copy()
            lc.data = ch.data.copy()
            lc.parent = None
            bpy.context.scene.collection.objects.link(lc)
            lc.location = c.location + ch.location
            legends.append(lc)

    def join(objs, name):
        for o in bpy.context.scene.objects:
            if o is not None:
                o.select_set(o in objs)
        bpy.context.view_layer.objects.active = objs[0]
        bpy.ops.object.join()
        out = bpy.context.view_layer.objects.active
        out.name = name
        return out

    def triangulate(obj):
        # BEAUTY: המשולשים שה-STL היה מחלק בעצמו מהמצולעים הקעורים באותיות יצאו חופפים (שבב 0.0004 מ"מ² ב-5)
        bm = bmesh.new()
        bm.from_mesh(obj.data)
        bmesh.ops.triangulate(bm, faces=bm.faces, quad_method="BEAUTY", ngon_method="BEAUTY")
        bm.to_mesh(obj.data)
        bm.free()

    plate = join(copies, "keycaps_print_plate")
    triangulate(plate)
    global LEGEND_PLATE
    LEGEND_PLATE = join(legends, "keycaps_legends_clear")
    triangulate(LEGEND_PLATE)
    return plate


def build_internals_preview():
    """מסך דלוק, פאנל מאחוריו ועדשת סלפי (לתמונות בלבד)."""
    pw, py0, py1 = screen_pocket()
    panel = rr_prism("screen_glass", pw - 0.2, py1 - py0 - 0.2, SCREEN_POCKET_R - 0.1,
                     BODY_T - SCREEN_RECESS - SCREEN_MODULE_T, BODY_T - SCREEN_RECESS - 0.004,
                     loc=(0, (py0 + py1) / 2, 0))
    bpy.ops.mesh.primitive_plane_add(size=1, location=(0, screen_cy, BODY_T - SCREEN_RECESS - 0.002))
    lit = bpy.context.active_object
    lit.name = "screen_lit"
    lit.scale = (SCREEN_W, SCREEN_H, 1)
    lens = rr_prism("selfie_lens", SELFIE["d"], SELFIE["d"], SELFIE["d"] / 2, SPLIT_Z - 1.6, SPLIT_Z - 0.3,
                    loc=(selfie_x, selfie_y, 0))
    glass = rr_prism("selfie_glass", 2.4, 2.4, 1.2, SPLIT_Z - 0.3, SPLIT_Z - 0.25, loc=(selfie_x, selfie_y, 0))
    cam_lens = rr_prism("rear_lens", REAR_CAM["w"] - 1, REAR_CAM["l"] - 1, (REAR_CAM["w"] - 1) / 2, BACK + 0.2, BACK + 0.6,
                        loc=(REAR_CAM["x"], REAR_CAM["y"], 0))
    k = next(k for k in KEYS if k[6] == "open")
    module = rr_prism("selfie_module", k[3] + 2.5, k[4] + 2.5, (k[4] + 2.5) / 2, SPLIT_Z - 1.8, SPLIT_Z - 0.7,
                      loc=(k[1], k[2], 0))
    # תאורה אחורית: לוח לדים מתחת למקלדת (לתמונות בלבד)
    kb_top = max(k[2] + k[4] / 2 for k in KEYS if k[6] in ("key", "dpad"))
    kb_bot = min(k[2] - k[4] / 2 for k in KEYS if k[6] in ("key", "dpad"))
    backlight = rr_prism("backlight", 2 * KEY_LIMIT_X, kb_top - kb_bot, 1.0,
                         SPLIT_Z - FLANGE_H - KEY_NUB_H - 0.4, SPLIT_Z - FLANGE_H - KEY_NUB_H - 0.3,
                         loc=(0, (kb_top + kb_bot) / 2, 0))
    # מגש SIM לתמונות בלבד: להב שטוח עם שני שקעי nano-SIM, יושב בחריץ בצד שמאל
    tx0 = -BODY_W / 2 + 0.1
    tray = rr_prism("sim_tray", 14.0, SIM["l"] - 1.0, 0.8, PORT_Z - 1.0, PORT_Z + 1.0, loc=(tx0 + 7.0, SIM["y"], 0))
    boolean(tray, [rr_prism(f"sim_rec{i}", 12.4, 8.9, 1.2, PORT_Z + 0.3, PORT_Z + 1.2,
                            loc=(tx0 + 8.3, SIM["y"] + s * 6.4, 0)) for i, s in ((0, -1), (1, 1))], "DIFFERENCE")
    chips = [rr_prism(f"sim_chip{i}", 12.0, 8.5, 1.1, PORT_Z + 0.3, PORT_Z + 0.5,
                      loc=(tx0 + 8.3, SIM["y"] + s * 6.4, 0)) for i, s in ((0, -1), (1, 1))]
    return dict(panel=panel, lit=lit, lens=lens, glass=glass, rear=cam_lens, module=module, backlight=backlight,
                sim_tray=tray, sim_chip0=chips[0], sim_chip1=chips[1])


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
    ok_flange_half = (dpad_hole + 0.5) / 2
    if DPAD_NUB_OFFSET - 0.8 - ok_flange_half < 0.2:
        issues.append("פיני החצים נוגעים בפלנג' של ה-OK")
    keys = [k for k in KEYS if k[6] in ("key", "open", "dpad")]
    r_cap, r_fl, r_win, _ = nav_arc_radii()
    if r_fl - (DPAD_R + FLANGE_MARGIN / 2) < 0.3:
        issues.append("הפלנג' של מקשי הניווט קרוב מדי לפלנג' של טבעת החצים")
    if r_win - (DPAD_R + KEY_WINDOW_CLEAR) < 0.9:
        issues.append(f"הגשר בין טבעת החצים למקשי הניווט דק ({r_win - DPAD_R - KEY_WINDOW_CLEAR:.2f})")
    for i, a in enumerate(keys):
        for b in keys[i + 1:]:
            if {a[0], b[0]} == {"#", "selfie"}:
                continue    # נבדק רדיאלית למטה
            gx = abs(a[1] - b[1]) - (a[3] + b[3]) / 2 - FLANGE_MARGIN
            gy = abs(a[2] - b[2]) - (a[4] + b[4]) / 2 - FLANGE_MARGIN
            if max(gx, gy) < 0.3:
                issues.append(f"הפלנג'ים של {a[0] or a[6]} ו-{b[0] or b[6]} קרובים מדי")
    top_key = max(k[2] + k[4] / 2 for k in keys) + KEY_WINDOW_CLEAR
    scr_bot = screen_pocket()[1]
    if scr_bot - top_key < 0.9:
        issues.append(f"הגשר בין שקע המסך למקשים דק ({scr_bot - top_key:.2f})")
    y0, _, _ = tray_geom()
    nav_top = max(k[2] + k[4] / 2 for k in KEYS if k[6] in ("key", "dpad")) + FLANGE_MARGIN / 2
    if (y0 - TRAY_WALL) - nav_top < 0.1:
        issues.append(f"הקיר התחתון של המגש נוגע בפלנג'ים של שורת הניווט ({(y0 - TRAY_WALL) - nav_top:.2f})")
    r_cap, r_fl, r_win, _ = HASH_ARC
    if r_win - SELFIE_WIN / 2 < 1.0:
        issues.append(f"הגשר בין חלון # לחלון הסלפי דק ({r_win - SELFIE_WIN / 2:.2f})")
    if r_fl - (SELFIE_WIN / 2 - KEY_WINDOW_CLEAR + 2.5 / 2) < 0.0:
        issues.append("הפלנג' של # נכנס לאזור מודול הסלפי")
    h = next(k for k in KEYS if k[0] == "#")
    if h[3] < 6.0 or abs(h[7][0]) > h[3] / 2 - 2.0:
        issues.append(f"מקש # צר מדי ({h[3]:.2f}) או שהפין יוצא ממנו")
    if lens_margin() < 0.3:
        issues.append(f"עדשת הסלפי לא נכנסת בחלון של # ({lens_margin():.2f})")
    return issues


def grid_report():
    return dict(key=[round(KEY_W, 2), round(KEY_H, 2)], gap=KEY_GAP, pitch=round(PITCH, 3),
                keypad_width=round(2 * KEY_LIMIT_X, 2), screen_pocket=[round(v, 2) for v in screen_pocket()],
                max_nub_offset=round(max(math.hypot(*k[7]) for k in KEYS), 2),
                selfie_lens_margin=round(lens_margin(), 2))


def lens_margin():
    """כמה מרווח יש בין עדשת הסלפי לשפת החלון העגול שלה."""
    k = next(k for k in KEYS if k[6] == "open")
    rr = (k[3] + 2 * KEY_WINDOW_CLEAR) / 2
    return rr - math.hypot(selfie_x - k[1], selfie_y - k[2]) - SELFIE["d"] / 2


# =====================================================================
# ייצוא ורינדור
# =====================================================================
def export_stl(obj, path):
    select_only(obj)
    if hasattr(bpy.ops.wm, "stl_export"):
        bpy.ops.wm.stl_export(filepath=path, export_selected_objects=True, ascii_format=False)
    else:
        bpy.ops.export_mesh.stl(filepath=path, use_selection=True)


def pmat(name, rgb, rough, coat=0.0, metal=0.0, image=None, emit=0.0):
    m = bpy.data.materials.new(name)
    m.diffuse_color = (*rgb, 1)
    try:
        m.use_nodes = True
    except Exception:
        pass
    nt = m.node_tree
    bsdf = next(n for n in nt.nodes if n.type == "BSDF_PRINCIPLED")
    bsdf.inputs["Base Color"].default_value = (*rgb, 1)
    bsdf.inputs["Roughness"].default_value = rough
    bsdf.inputs["Metallic"].default_value = metal
    if coat:
        bsdf.inputs["Coat Weight"].default_value = coat
        bsdf.inputs["Coat Roughness"].default_value = 0.08
    if image and os.path.exists(image):
        tex = nt.nodes.new("ShaderNodeTexImage")
        tex.image = bpy.data.images.load(image)
        nt.links.new(tex.outputs["Color"], bsdf.inputs["Emission Color"])
        bsdf.inputs["Emission Strength"].default_value = emit
    return m


def set_mat(obj, mat):
    obj.data.materials.clear()
    obj.data.materials.append(mat)
    for poly in obj.data.polygons:
        poly.material_index = 0


def set_mats_by_normal(obj, mat_flat, mat_side, flat_sign):
    """פאות שפונות לחזית/לגב מקבלות mat_flat, המסגרת (הצדדים) mat_side."""
    obj.data.materials.clear()
    obj.data.materials.append(mat_flat)
    obj.data.materials.append(mat_side)
    for poly in obj.data.polygons:
        poly.material_index = 0 if poly.normal.z * flat_sign > 0.6 else 1


def area_light(name, loc, size, power, target, color=(1, 1, 1)):
    ld = bpy.data.lights.new(name, "AREA")
    ld.shape = "DISK"
    ld.size = size
    ld.energy = power
    ld.color = color
    lo = bpy.data.objects.new(name, ld)
    bpy.context.scene.collection.objects.link(lo)
    lo.location = loc
    t = lo.constraints.new("TRACK_TO")
    t.target = target
    t.track_axis = "TRACK_NEGATIVE_Z"
    t.up_axis = "UP_Y"
    return lo


def render_views(back, front, keys, preview, plate):
    scene = bpy.context.scene
    scene.render.engine = "CYCLES"
    scene.cycles.device = "CPU"
    scene.cycles.samples = int(os.environ.get("V3_SAMPLES", "48"))
    scene.cycles.use_denoising = True
    scene.cycles.max_bounces = 6
    scene.render.resolution_x = int(os.environ.get("V3_RES", "1000"))
    scene.render.resolution_y = scene.render.resolution_x
    scene.view_settings.view_transform = "AgX"
    try:
        scene.view_settings.look = "AgX - Medium High Contrast"
    except Exception:
        pass
    if scene.world is None:
        scene.world = bpy.data.worlds.new("World")
    scene.world.color = (0.02, 0.02, 0.022)
    try:
        scene.world.use_nodes = True
        bg = next(n for n in scene.world.node_tree.nodes if n.type == "BACKGROUND")
        bg.inputs["Color"].default_value = (0.32, 0.32, 0.33, 1)
        bg.inputs["Strength"].default_value = 1.0
    except Exception:
        pass

    # שחור מלא, כמו iPhone / Galaxy: חזית זכוכית שחורה מבריקה, מסגרת מתכת שחורה,
    # גב זכוכית חלבית שחורה, מקשים שחורים מבריקים. הלבן היחיד: הספרות.
    m_front = pmat("front_gloss", (0.006, 0.006, 0.007), 0.06, coat=1.0)
    m_frame = pmat("frame_black_titanium", (0.035, 0.035, 0.038), 0.22, metal=1.0)
    m_back = pmat("back_frosted", (0.012, 0.012, 0.013), 0.5, coat=0.2)
    # TPU שחור אטום; רק הספרות והאותיות (TPU שקוף) מוארות מהלדים שמתחת
    m_key = pmat("key_tpu_black", (0.012, 0.012, 0.013), 0.5, coat=0.05)
    m_backlight = pmat("backlight", (0, 0, 0), 1.0)
    _bb = next(n for n in m_backlight.node_tree.nodes if n.type == "BSDF_PRINCIPLED")
    _bb.inputs["Emission Color"].default_value = (1.0, 0.98, 0.95, 1)
    _bb.inputs["Emission Strength"].default_value = float(os.environ.get("V3_BACKLIGHT", "1.5"))
    m_legend = pmat("legend_white", (0.92, 0.92, 0.90), 0.5)
    # תאורת מקלדת לבנה (כמו בטלפונים עם מקשים מוארים)
    _lb = next(n for n in m_legend.node_tree.nodes if n.type == "BSDF_PRINCIPLED")
    _lb.inputs["Emission Color"].default_value = (1.0, 1.0, 1.0, 1)
    _lb.inputs["Emission Strength"].default_value = 4.0
    m_dark = pmat("internals", (0.005, 0.005, 0.006), 0.4)
    m_screen = pmat("screen", (0.0, 0.0, 0.0), 0.04, coat=1.0, image=SCREEN_IMG, emit=0.9)
    m_lens = pmat("lens", (0.002, 0.004, 0.012), 0.02, coat=1.0, metal=0.0)
    m_floor = pmat("floor", (0.62, 0.62, 0.63), 0.6)

    set_mats_by_normal(back, m_back, m_frame, flat_sign=-1)
    isl_w, isl_l = REAR_CAM["w"] + CAM_RING * 2 + 0.2, REAR_CAM["l"] + CAM_RING * 2 + 0.2
    for poly in back.data.polygons:
        c = poly.center
        dx, dy = abs(c.x - REAR_CAM["x"]), abs(c.y - REAR_CAM["y"])
        if c.z < 0.05 and dx < isl_w / 2 and dy < isl_l / 2:
            poly.material_index = 1
    set_mats_by_normal(front, m_front, m_frame, flat_sign=+1)
    for k in keys:
        if k.name.startswith("volume_rocker"):
            set_mat(k, m_frame)
        else:
            set_mat(k, m_key)
        for ch in k.children:
            set_mat(ch, m_legend)
    set_mat(preview["panel"], m_front)     # זכוכית המסך: שחור מבריק, בגובה החזית
    set_mat(preview["lit"], m_screen)
    set_mat(preview["lens"], m_lens)
    set_mat(preview["glass"], m_lens)
    set_mat(preview["rear"], m_lens)
    set_mat(preview["module"], m_dark)
    set_mat(preview["retainer"], m_back)
    set_mat(preview["backlight"], m_backlight)
    m_tray = pmat("sim_tray_metal", (0.55, 0.55, 0.57), 0.3, metal=1.0)
    m_gold = pmat("sim_gold", (0.9, 0.68, 0.2), 0.25, metal=1.0)
    set_mat(preview["sim_tray"], m_tray)
    set_mat(preview["sim_chip0"], m_gold)
    set_mat(preview["sim_chip1"], m_gold)
    plate.hide_render = True

    # כל הטלפון תחת Empty אחד: כך הופכים אותו לצילום הגב
    root = bpy.data.objects.new("phone_root", None)
    scene.collection.objects.link(root)
    parts = [back, front] + keys + list(preview.values())
    for o in parts:
        o.parent = root

    bpy.ops.mesh.primitive_plane_add(size=4000, location=(0, 0, -0.05))
    floor = bpy.context.active_object
    set_mat(floor, m_floor)

    target = bpy.data.objects.new("look_at", None)
    scene.collection.objects.link(target)
    origin = bpy.data.objects.new("light_target", None)
    scene.collection.objects.link(origin)
    # 1 יחידה = 1 מ' בעיני Cycles, ולכן ההספקים גדולים
    area_light("key", (-220, 160, 420), 320, 7.0e5, origin)
    area_light("fill", (300, -260, 260), 420, 1.6e5, origin, (0.92, 0.95, 1.0))
    area_light("rim", (60, 420, 160), 160, 3.0e5, origin, (1.0, 0.93, 0.85))
    area_light("strip", (-320, -120, 120), 60, 1.2e5, origin)

    cam = bpy.data.objects.new("cam", bpy.data.cameras.new("cam"))
    cam.data.lens = 70
    cam.data.clip_start = 1
    cam.data.clip_end = 6000
    scene.collection.objects.link(cam)
    scene.camera = cam
    t = cam.constraints.new("TRACK_TO")
    t.target = target
    t.track_axis = "TRACK_NEGATIVE_Z"
    t.up_axis = "UP_Y"

    base = {o: o.location.copy() for o in [front] + keys + [preview["retainer"], preview["backlight"],
                                                           preview["lit"], preview["panel"], preview["lens"],
                                                           preview["glass"]]}
    sim_parts = [preview["sim_tray"], preview["sim_chip0"], preview["sim_chip1"]]
    sim_base = {o: o.location.copy() for o in sim_parts}
    # (שם, מצלמה, מבט, הרמה לפיצוץ, הפוך לגב, סיבוב Z)
    views = [
        ("hero", (95, -175, 300), (0, -4, 4), 0.0, False, math.radians(-14)),
        ("front_straight", (0, -20, 330), (0, -3, 5), 0.0, False, 0.0),
        ("keypad_closeup", (55, -165, 120), (0, -42, 6), 0.0, False, 0.0),
        ("back", (-80, -170, 300), (0, 0, 4), 0.0, True, math.radians(12)),
        ("side", (240, -60, 70), (0, 0, 6), 0.0, False, math.radians(-20)),
        ("exploded", (240, -260, 260), (0, 0, 16), 16.0, False, 0.0),
        ("sim_closed", (-125, 8, 38), (-28.75, SIM["y"] - 2, 4.5), 0.0, False, 0.0),
        ("sim_open", (-125, 8, 38), (-28.75, SIM["y"] - 2, 4.5), 0.0, False, 0.0),
        ("tray_floor", (0, 0, 260), (0, 20, 0), 0.0, False, 0.0),
    ]
    only = os.environ.get("V3_VIEWS")
    paths = []
    for name, loc, look, lift, flip, rz in views:
        if only and name not in only.split(","):
            continue
        for o, p in base.items():
            if o is front:
                k = 1.0
            elif o.name.startswith("volume_rocker"):
                k = 0.0
            else:
                k = 1.8 if o in keys else 0.5
            o.location = p + Vector((0, 0, lift * k))
        preview["backlight"].hide_render = True
        for o, p in sim_base.items():
            o.location = p + (Vector((-16.0, 0, 0)) if name == "sim_open" else Vector((0, 0, 0)))
        floor_only = name == "tray_floor"
        for o in [back, front, plate] + keys + [ch for k2 in keys for ch in k2.children]                 + [v for k2, v in preview.items() if k2 != "retainer"]:
            o.hide_render = floor_only or o is plate
        preview["backlight"].hide_render = True
        preview["retainer"].location = Vector((0, 0, 0))
        root.rotation_euler = (0, math.pi if flip else 0, rz)
        root.location = (0, 0, BODY_T if flip else 0)
        cam.location = loc
        target.location = look
        scene.render.filepath = os.path.join(OUT, f"render_{name}.png")
        bpy.ops.render.render(write_still=True)
        paths.append(scene.render.filepath)
    for o, p in list(base.items()) + list(sim_base.items()):
        o.location = p
    root.rotation_euler = (0, 0, 0)
    root.location = (0, 0, 0)
    plate.hide_render = False
    return paths


# =====================================================================
FIT_ONLY = os.environ.get("V3_FIT_ONLY") == "1"
# 25.10: כל הרצועה התחתונה (שתי הפינות, USB, מיקרופון, # וחלון הסלפי) - בודקים גם את הכיפה של # ואת העדשה
FIT_KEYS = {"7", "8", "9", "*", "0", "#"}
FIT_BOX = dict(x0=-BODY_W / 2 - 1, x1=BODY_W / 2 + 1, y0=-BODY_L / 2 - 1, y1=-49.0)


def fit_test(back, front, keys):
    """#43: פינה תחתונה-שמאלית בלבד (חזית + גב + 3 מקשים) להדפסה מהירה. #44: גרסה לכל SKIRT_GAP."""
    tag = f"gap{int(round(SKIRT_GAP * 100)):03d}"
    out = {}
    b = FIT_BOX
    for name, obj in (("front", front), ("back", back)):
        c = obj.copy()
        c.data = obj.data.copy()
        bpy.context.scene.collection.objects.link(c)
        box = rr_prism("fit_box", b["x1"] - b["x0"], b["y1"] - b["y0"], 0.5, -5, BODY_T + 5,
                       loc=((b["x0"] + b["x1"]) / 2, (b["y0"] + b["y1"]) / 2, 0))
        boolean(c, [box], "INTERSECT")
        c.name = f"fit_{name}_{tag}"
        path = os.path.join(OUT, f"fit_test_{name}_{tag}.stl")
        export_stl(c, path)
        out[name] = dict(stl=path, mesh=check_mesh(c))
        bpy.data.objects.remove(c, do_unlink=True)
    plate = print_plate([k for k in keys if not k.name.startswith(("volume", "dpad", "ok"))])
    out["keys"] = os.path.join(OUT, f"fit_test_keys_{tag}.stl")
    export_stl(plate, out["keys"])
    out["keys_mesh"] = check_mesh(plate)
    out["legends"] = os.path.join(OUT, f"fit_test_keys_legends_clear_{tag}.stl")
    export_stl(LEGEND_PLATE, out["legends"])
    out["legends_mesh"] = check_mesh(LEGEND_PLATE)
    return out


def main():
    clear_scene()
    if FIT_ONLY:
        back = build_back_housing()
        front = build_front_housing()
        keys = build_keys_in_place()
        res = fit_test(back, front, keys)
        with open(os.path.join(OUT, f"fit_test_gap{int(round(SKIRT_GAP * 100)):03d}.json"), "w", encoding="utf-8") as f:
            json.dump(res, f, ensure_ascii=False, indent=2)
        print(json.dumps(res, ensure_ascii=False))
        return
    back = build_back_housing()
    front = build_front_housing()
    keys = build_keys_in_place()
    plate = print_plate(keys)
    preview = build_internals_preview()
    retainer = build_screen_tray_floor()

    report = dict(
        dims=dict(outer=[BODY_W, BODY_L, BODY_T], split_z=round(SPLIT_Z, 2)),
        grid=grid_report(),
        back_housing=check_mesh(back),
        front_housing=check_mesh(front),
        screen_tray_floor=check_mesh(retainer),
        keycaps=check_mesh(plate),
        keycaps_legends_clear=check_mesh(LEGEND_PLATE),
        design_issues=check_design(),
    )
    stl = {
        "back_housing": os.path.join(OUT, "qin_f22_pro_back_housing.stl"),
        "front_housing": os.path.join(OUT, "qin_f22_pro_front_housing.stl"),
        "screen_tray_floor": os.path.join(OUT, "qin_f22_pro_screen_tray_floor.stl"),
        "keycaps": os.path.join(OUT, "qin_f22_pro_keycaps.stl"),
    }
    export_stl(back, stl["back_housing"])
    export_stl(front, stl["front_housing"])
    export_stl(retainer, stl["screen_tray_floor"])
    preview["retainer"] = retainer
    export_stl(plate, stl["keycaps"])
    stl["keycaps_legends_clear"] = os.path.join(OUT, "qin_f22_pro_keycaps_legends_clear.stl")
    export_stl(LEGEND_PLATE, stl["keycaps_legends_clear"])
    LEGEND_PLATE.hide_render = True
    report["stl"] = stl
    print(json.dumps(report, ensure_ascii=False, indent=2))
    if os.environ.get("V3_NO_RENDER") != "1":
        report["renders"] = render_views(back, front, keys, preview, plate)

    blend = os.path.join(OUT, "qin_f22_pro_housing.blend")
    bpy.ops.wm.save_as_mainfile(filepath=blend)
    report["blend"] = blend
    with open(os.path.join(OUT, "report.json"), "w", encoding="utf-8") as f:
        json.dump(report, f, ensure_ascii=False, indent=2)
    if not all(report[k]["ok"] for k in ("back_housing", "front_housing", "screen_tray_floor", "keycaps")):
        print("!!! רשת לא תקינה - לבדוק את report.json לפני הדפסה")


main()
