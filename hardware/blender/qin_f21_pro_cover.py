# =====================================================================
#  מגן ל-Qin F21 Pro - אותה שפה עיצובית כמו F22 Pro v2
#
#  טוען את qin_f22_pro_cover_ultimate_v2.py ודורס רק את מידות המכשיר.
#  מקור המידות וחוסר הוודאות: hardware/F21_PRO_DIMENSIONS.md
#
#  !!! המידות החיצוניות (131.7 × 53.5 × 9.5) מגיעות ממקורות מקוונים. המיקום של המצלמה,
#      מקש ההפעלה, ה-SIM והמיקרופונים לא תועד באף מקור אמין, ולכן הפתחים כאן מכוונים
#      לכסות טווח רחב (מגן פחות מוגן, אבל לא חוסם). כשהיחידות מגיעות: מודדים,
#      מעדכנים את הבלוק "פתחים" ומדפיסים קודם את qin_f21_pro_fit_test.stl.
#
#  הרצה:  blender -b -P qin_f21_pro_cover.py      פלט: out/f21_pro/
# =====================================================================

import os

HERE = os.path.dirname(os.path.abspath(__file__)) if "__file__" in globals() and __file__ else os.getcwd()
_NO_MAIN = True
exec(compile(open(os.path.join(HERE, "qin_f22_pro_cover_ultimate_v2.py"), encoding="utf-8").read(),
             "qin_f22_pro_cover_ultimate_v2.py", "exec"), globals())

# ---------------------------------------------------------------------
# המכשיר
# ---------------------------------------------------------------------
DEVICE_NAME = "qin_f21_pro"
OUT = os.path.join(HERE, "out", "f21_pro")
PHONE_L = 131.7
PHONE_W = 53.5
PHONE_T = 9.5
PHONE_R = 6.0            # לא נמדד - F21 נראה מעוגל יותר מ-F22
PHONE_EDGE = 2.0         # לא נמדד
LIP_IN = 1.5             # מצלמת הסלפי בשוליים התחתונים, מתחת למקלדת: שפה צרה יותר
SCREEN_W, SCREEN_H, SCREEN_FROM_TOP = 43.2, 57.6, 9.0   # 2.8" 3:4 - לרינדור בלבד

# ---------------------------------------------------------------------
# פתחים - הערכה רחבה (ר' F21_PRO_DIMENSIONS.md)
# ---------------------------------------------------------------------
USB = dict(x=0.0, w=22.0, h=6.0)                 # USB-C + מיקרופון/רמקול לידו, בתחתית
MIC = dict(x=8.0, d=2.6)                         # בתוך חלון ה-USB
TOP_WINDOW = dict(x=0.0, w=10.0, h=5.0)          # מיקרופון עליון אפשרי
SIM = dict(y=from_top(45.0), l=36.0, h=5.0)      # צד שמאל, פתוח: לפי ביקורת יש חור מיקרופון ליד המגש
VOLUME = dict(y=from_top(38.0), l=30.0, h=5.0)   # צד ימין: מקש ההפעלה. אין מקשי עוצמה ב-F21
REAR_CAM = dict(x=0.0, y=from_top(14.0), w=30.0, l=18.0)   # חלון רחב: מצלמה + פלאש, מיקום לא ידוע
SIDE_RIB = dict(w=1.0, depth=0.5, pitch=2.5, y0=-40.0, y1=0.0, z_margin=2.2)
FINGER = dict(y=-2.0, w=18.0, l=24.0, depth=0.5)
LUG = dict(x=-17.0, size=10.0, hole=3.2)

# SIM ומקש הפעלה: פתוחים (cover_* כבוי), כי המיקום לא מאומת
D = dict(key="v1", name="F21 Pro", wall=1.6, back=1.4, color=(0.10, 0.11, 0.12, 1))


def accent_faces(cover, g, D):
    """כתום: מסגרת המצלמה, ושפת החלון של מקש ההפעלה."""
    for p in cover.data.polygons:
        c = p.center
        on_ridge = (c.z < -0.05 and abs(c.x - REAR_CAM["x"]) < REAR_CAM["w"] / 2 + 4.5
                    and abs(c.y - REAR_CAM["y"]) < REAR_CAM["l"] / 2 + 4.5)
        on_key = (c.x > g["out_w"] / 2 - D["wall"] - 0.05 and abs(c.y - VOLUME["y"]) < VOLUME["l"] / 2 + 0.3
                  and abs(c.z - g["port_z"]) < VOLUME["h"] / 2 + 0.3)
        if on_ridge or on_key:
            p.material_index = 1


def phone_proxy(g):
    """F21 Pro לרינדור: גוף, מסך 2.8", 4 שורות מקשים, סלפי בשוליים התחתונים."""
    body = rounded_block("ph_phone", PHONE_W, PHONE_L, PHONE_R, PHONE_T, PHONE_EDGE, z0=g["phone_z"])
    top = g["phone_z"] + PHONE_T
    scr = rr_prism("ph_screen", SCREEN_W, SCREEN_H, 0.6, top - 0.1, top + 0.02,
                   loc=(0, from_top(SCREEN_FROM_TOP) - SCREEN_H / 2, 0))
    keys = []
    y0 = from_top(SCREEN_FROM_TOP + SCREEN_H + 4.0)
    for r in range(6):
        y = y0 - 4.0 - r * 8.6
        for x in (-16.0, 0.0, 16.0):
            if r < 2 and x == 0.0:
                continue
            keys.append(rr_prism("ph_key", 12.5, 6.2, 3.1, top - 0.1, top + 0.5, loc=(x, y, 0)))
    keys.append(rr_prism("ph_dpad", 13.0, 13.0, 3.5, top - 0.1, top + 0.5, loc=(0, y0 - 8.3, 0)))
    lens = rr_prism("ph_selfie", 3.0, 3.0, 1.5, top - 0.1, top + 0.03, loc=(0, -PHONE_L / 2 + 3.0, 0))
    return body, scr, keys, [lens]


run(D, extra_report=dict(uncertain=["PHONE_R", "PHONE_EDGE", "REAR_CAM", "SIM", "power key (VOLUME)",
                                    "MIC", "TOP_WINDOW", "selfie position"]))
