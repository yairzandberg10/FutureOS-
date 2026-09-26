// =====================================================================
//  משפחת מכשירי FutureOS - Mini (2.8") / Regular (3.5") / Pro (5")
//  מודל מעטפת פרמטרי ל-OpenSCAD. מפרט מלא: hardware/MODELS_SPEC.md
//
//  מה זה כן: מודל אריזה (packaging) - מידות חיצוניות, חלון מסך, פריסת
//  מקלדת, ונפחי keep-out לרכיבים הגדולים (מודול מסך, לוח, סוללה).
//  משמש לבדיקת ארגונומיה, הדפסת דמה (dummy) להחזקה ביד, ובסיס ל-RFQ מול ODM.
//  מה זה לא: CAD לייצור. קבצי STEP לתבניות הזרקה מייצר ה-ODM מהמודל הזה.
//
//  הקנבס של ה-UI הוא 320x480dp בכל הדגמים (FutureDimens.screenWidth/Height),
//  לכן כל המסכים ביחס 2:3 בדיוק.
//
//  שימוש: לבחור MODEL ו-PART (או דרך ה-Customizer). F5 לתצוגה, F6+STL להדפסה.
//  הקונסולה מדפיסה (echo) את המידות החיצוניות שחושבו.
// =====================================================================

$fn = 48;

MODEL = "regular"; // [mini, regular, pro]
PART  = "assembly"; // [assembly, front_shell, back_shell, keycaps, keepouts]
PRO_FORM = "slider"; // [slider, bar] - Pro כ-bar יוצא כ-190 מ"מ אורך, ר' המפרט
SLIDE_OPEN = 1;      // 0 = סגור, 1 = פתוח (Pro slider בלבד)

// ---------------------------------------------------------------------
// טבלת פרמטרים לפי דגם: [mini, regular, pro]
// ---------------------------------------------------------------------
function sel(v) = MODEL == "mini" ? v[0] : MODEL == "regular" ? v[1] : v[2];

DIAG_IN     = sel([2.8, 3.5, 5.0]);
SCR_W       = DIAG_IN * 25.4 * 2 / sqrt(13);   // רוחב אזור פעיל (2:3)
SCR_H       = DIAG_IN * 25.4 * 3 / sqrt(13);   // גובה אזור פעיל
LCD_SIDE    = 1.5;   // שוליים של מודול המסך בצדדים ולמעלה
LCD_BOTTOM  = 3.5;   // שוליים למטה (FPC / IC של המסך)
LCD_T       = sel([2.6, 2.8, 3.2]);            // עובי מודול מסך כולל זכוכית

TOP_BAND    = sel([7, 8, 9]);     // רמקול שיחה, מצלמה קדמית, חיישן אור/קרבה
HOME_H      = sel([5, 6, 6]);     // שורת מקש Home מתחת למסך
NAV_H       = sel([16, 20, 20]);  // אזור D-pad + מקשי soft + שיחה/ניתוק
ROW_P       = sel([8, 9.5, 10]);  // פסיעת שורות במקלדת הספרות
KEY_P       = sel([14, 17, 21]);  // פסיעת עמודות
BOTTOM_BAND = 5;                  // מיקרופון, USB-C
SIDE_WALL   = sel([4, 4.5, 4]);
CORNER_R    = sel([6, 7, 8]);
BODY_T      = sel([12, 12, 16.5]); // Pro slider: שתי יחידות
FRONT_T     = 1.4;                 // עובי דופן פנים
WALL        = 1.4;

BATT        = sel([[36, 62, 4.0], [48, 83, 4.5], [60, 85, 4.7]]); // Li-Po, מ"מ (1500/3000/4000mAh)
BOARD       = sel([[44, 50, 1.0], [54, 62, 1.0], [70, 58, 1.0]]); // PCBA (ללא מגנים)
BOARD_Z     = sel([3.2, 3.5, 3.5]); // גובה רכיבים + מגני RF

DIGIT_ROWS  = 4;
IS_SLIDER   = MODEL == "pro" && PRO_FORM == "slider";

// ---------------------------------------------------------------------
// פריסה אנכית (מלמטה למעלה, Y=0 בתחתית הגוף)
// ---------------------------------------------------------------------
DIGITS_H   = DIGIT_ROWS * ROW_P;
SCR_OUT_W  = SCR_W + 2 * LCD_SIDE;
SCR_OUT_H  = SCR_H + LCD_SIDE + LCD_BOTTOM;

// ב-slider הספרות יושבות על המזחלת ולכן לא נכנסות לאורך היחידה העליונה
Y_DIGITS   = BOTTOM_BAND;
Y_NAV      = IS_SLIDER ? BOTTOM_BAND : Y_DIGITS + DIGITS_H;
Y_HOME     = Y_NAV + NAV_H;
Y_SCR      = Y_HOME + HOME_H;
BODY_L     = Y_SCR + SCR_OUT_H + TOP_BAND;
BODY_W     = max(SCR_OUT_W, 3 * KEY_P) + 2 * SIDE_WALL;

SLED_T     = 6.0;                  // Pro slider: עובי המזחלת
TOP_T      = IS_SLIDER ? BODY_T - SLED_T : BODY_T;
SLED_L     = DIGITS_H + BOTTOM_BAND + 6;
SLIDE_TRAVEL = DIGITS_H + 2;

echo(str("MODEL=", MODEL, IS_SLIDER ? " (slider)" : "",
         "  body W x L x T = ", BODY_W, " x ", BODY_L, " x ", BODY_T, " mm",
         IS_SLIDER ? str("  open L = ", BODY_L + SLIDE_TRAVEL) : "",
         "  active area = ", SCR_W, " x ", SCR_H));

// ---------------------------------------------------------------------
// גיאומטריה בסיסית
// ---------------------------------------------------------------------
module rbox(w, l, t, r) {
    hull() for (x = [r, w - r], y = [r, l - r])
        translate([x, y, 0]) cylinder(r = r, h = t);
}

CX = BODY_W / 2;

// מיקומי מקשים: [x, y, w, h, צורה, תווית]
function digit_keys() = [
    for (row = [0 : DIGIT_ROWS - 1], col = [0 : 2])
        [CX + (col - 1) * KEY_P, Y_DIGITS + (DIGIT_ROWS - 1 - row + 0.5) * ROW_P,
         KEY_P - 1.4, ROW_P - 1.4, "rect",
         ["1","2","3","4","5","6","7","8","9","*","0","#"][row * 3 + col]]
];

RING_D = NAV_H - 2;
SIDE_KEY_W = KEY_P - 1.6;
SIDE_KEY_H = (NAV_H - 3) / 2;

function nav_keys() = [
    [CX, Y_NAV + NAV_H / 2, RING_D, RING_D, "ring", "dpad"],
    [CX, Y_NAV + NAV_H / 2, RING_D * 0.42, RING_D * 0.42, "round", "ok"],
    [CX - KEY_P, Y_NAV + NAV_H * 0.75, SIDE_KEY_W, SIDE_KEY_H, "rect", "softL"],
    [CX - KEY_P, Y_NAV + NAV_H * 0.25, SIDE_KEY_W, SIDE_KEY_H, "rect", "call"],
    [CX + KEY_P, Y_NAV + NAV_H * 0.75, SIDE_KEY_W, SIDE_KEY_H, "rect", "softR"],
    [CX + KEY_P, Y_NAV + NAV_H * 0.25, SIDE_KEY_W, SIDE_KEY_H, "rect", "end"],
    [CX, Y_HOME + HOME_H / 2, KEY_P * 0.8, HOME_H - 2, "rect", "home"]
];

module key_shape(k, grow = 0, h = 1) {
    translate([k[0], k[1], 0])
    if (k[4] == "rect")
        translate([-(k[2] + grow) / 2, -(k[3] + grow) / 2, 0])
            rbox(k[2] + grow, k[3] + grow, h, min(1.2, (k[3] + grow) / 2 - 0.01));
    else if (k[4] == "round")
        cylinder(d = k[2] + grow, h = h);
    else // ring: טבעת D-pad (החור של OK מוחסר בנפרד)
        difference() {
            cylinder(d = k[2] + grow, h = h);
            translate([0, 0, -0.01]) cylinder(d = k[2] * 0.42 + 1.2 - grow, h = h + 0.02);
        }
}

// ---------------------------------------------------------------------
// חלקים
// ---------------------------------------------------------------------
FRONT_KEYS = IS_SLIDER ? nav_keys() : concat(nav_keys(), digit_keys());
GAP = 0.25; // מרווח בין מקש לחור

module screen_window() {
    translate([CX - SCR_W / 2 - 0.3, Y_SCR + LCD_BOTTOM - 0.3, -0.01])
        rbox(SCR_W + 0.6, SCR_H + 0.6, FRONT_T + 0.02, 0.8);
}

module front_shell() {
    difference() {
        rbox(BODY_W, BODY_L, FRONT_T, CORNER_R);
        screen_window();
        for (k = FRONT_KEYS) translate([0, 0, -0.01]) key_shape(k, 2 * GAP, FRONT_T + 0.02);
        // רמקול שיחה
        translate([CX - 6, BODY_L - TOP_BAND / 2 - 0.6, -0.01]) rbox(12, 1.2, FRONT_T + 0.02, 0.59);
    }
}

module back_shell() {
    t = TOP_T - FRONT_T;
    difference() {
        rbox(BODY_W, BODY_L, t, CORNER_R);
        translate([WALL, WALL, WALL]) rbox(BODY_W - 2 * WALL, BODY_L - 2 * WALL, t, CORNER_R - WALL);
        // USB-C בתחתית
        translate([CX - 4.5, -0.01, t / 2 - 1.75]) cube([9, WALL + 0.02, 3.5]);
        // מצלמה אחורית + פלאש
        translate([BODY_W - SIDE_WALL - 8, BODY_L - TOP_BAND - 10, -0.01]) cylinder(d = sel([7, 8, 11]), h = WALL + 0.02);
        translate([BODY_W - SIDE_WALL - 8, BODY_L - TOP_BAND - 20, -0.01]) cylinder(d = 3, h = WALL + 0.02);
    }
}

module keycaps(layout_flat = false) {
    for (i = [0 : len(FRONT_KEYS) - 1]) {
        k = FRONT_KEYS[i];
        translate(layout_flat ? [0, 0, 0] : [0, 0, FRONT_T - 0.4])
            key_shape(k, 0, 1.2);
    }
}

module sled() {
    // Pro slider: מזחלת הספרות, יושבת מתחת ליחידה העליונה
    sled_keys = [
        for (row = [0 : DIGIT_ROWS - 1], col = [0 : 2])
            [CX + (col - 1) * KEY_P, BOTTOM_BAND + (DIGIT_ROWS - 1 - row + 0.5) * ROW_P,
             KEY_P - 1.4, ROW_P - 1.4, "rect", ""]
    ];
    difference() {
        rbox(BODY_W, SLED_L, SLED_T, CORNER_R);
        for (k = sled_keys) translate([0, 0, SLED_T - 0.8]) key_shape(k, 2 * GAP, 1);
    }
    color("DimGray") for (k = sled_keys) translate([0, 0, SLED_T - 0.8]) key_shape(k, 0, 1.3);
}

module keepouts() {
    inner_z = FRONT_T;
    // מודול המסך
    color("SteelBlue", 0.8)
        translate([CX - SCR_OUT_W / 2, Y_SCR, -LCD_T + FRONT_T]) cube([SCR_OUT_W, SCR_OUT_H, LCD_T]);
    // סוללה - מאחורי המסך
    color("Orange", 0.8)
        translate([CX - BATT[0] / 2, BODY_L - TOP_BAND - BATT[1], -LCD_T - BATT[2] - 0.3])
            cube(BATT);
    // PCBA - מאחורי אזור המקלדת (bar) / מאחורי ה-nav (slider)
    color("ForestGreen", 0.8)
        translate([CX - BOARD[0] / 2, BOTTOM_BAND - 1, -BOARD_Z - 1.5]) cube([BOARD[0], BOARD[1], BOARD_Z]);
}

// ---------------------------------------------------------------------
// הרכבה
// ---------------------------------------------------------------------
module assembly() {
    // המערכת: Z=0 הוא הפנים הקדמיים, הגוף נבנה כלפי Z שלילי
    color("WhiteSmoke") translate([0, 0, -FRONT_T]) front_shell();
    color("DimGray") translate([0, 0, -FRONT_T]) keycaps();
    color("Black", 0.9) translate([CX - SCR_W / 2, Y_SCR + LCD_BOTTOM, -0.2]) cube([SCR_W, SCR_H, 0.1]);
    translate([0, 0, -FRONT_T]) keepouts();
    color("Gainsboro", 0.35) translate([0, 0, -TOP_T]) back_shell();
    if (IS_SLIDER)
        translate([0, -SLIDE_OPEN * SLIDE_TRAVEL, -TOP_T - SLED_T]) sled();
}

if (PART == "assembly")         assembly();
else if (PART == "front_shell") front_shell();
else if (PART == "back_shell")  back_shell();
else if (PART == "keycaps")     keycaps(true);
else if (PART == "keepouts")    keepouts();
