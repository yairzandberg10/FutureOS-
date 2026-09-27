// =====================================================================
//  משפחת מכשירי FutureOS - Mini (2.8") / Regular (3.5") / Pro (4.5" ריבועי)
//  מודל מעטפת פרמטרי ל-OpenSCAD. מפרט מלא: hardware/MODELS_SPEC.md
//
//  מה זה כן: מודל אריזה (packaging) - מידות חיצוניות, חלון מסך, פריסת
//  מקלדת, ונפחי keep-out לרכיבים הגדולים (מודול מסך, לוח, סוללה).
//  משמש לבדיקת ארגונומיה, הדפסת דמה (dummy) להחזקה ביד, ובסיס ל-RFQ מול ODM.
//  מה זה לא: CAD לייצור. קבצי STEP לתבניות הזרקה מייצר ה-ODM מהמודל הזה.
//
//  הקנבס של ה-UI הוא 320x480dp (FutureDimens.screenWidth/Height), לכן Mini
//  ו-Regular ביחס 2:3 בדיוק. Pro הוא bar עם מסך רחב יותר, ריבועי (1:1):
//  480x480dp - אותו גובה, רוחב גדול ב-160dp.
//
//  שימוש: לבחור MODEL ו-PART (או דרך ה-Customizer). F5 לתצוגה, F6+STL להדפסה.
//  הקונסולה מדפיסה (echo) את המידות החיצוניות שחושבו.
// =====================================================================

$fn = 48;

MODEL = "regular"; // [mini, regular, pro]
PART  = "assembly"; // [assembly, front_shell, back_shell, keypad, markings, side_keys, keepouts]

// ---------------------------------------------------------------------
// טבלת פרמטרים לפי דגם: [mini, regular, pro]
// ---------------------------------------------------------------------
function sel(v) = MODEL == "mini" ? v[0] : MODEL == "regular" ? v[1] : v[2];

DIAG_IN     = sel([2.8, 3.5, 4.5]);
SCR_AR      = sel([2 / 3, 2 / 3, 1]);          // רוחב/גובה: 2:3, ו-Pro ריבועי
SCR_W       = DIAG_IN * 25.4 * SCR_AR / sqrt(1 + SCR_AR * SCR_AR); // רוחב אזור פעיל
SCR_H       = DIAG_IN * 25.4 / sqrt(1 + SCR_AR * SCR_AR);          // גובה אזור פעיל
LCD_SIDE    = 0.8;   // שוליים של מודול המסך בצדדים ולמעלה (מודול narrow-bezel)
LCD_BOTTOM  = 3.5;   // שוליים למטה (FPC / IC של המסך)
LCD_T       = sel([2.6, 2.8, 3.2]);            // עובי מודול מסך כולל זכוכית

TOP_BAND    = sel([2.5, 2.5, 3]); // פס דק מעל הזכוכית: חריץ רמקול שיחה בלבד (מצלמה קדמית = punch-hole במסך)
NAV_GAP     = 1.5;                // מרווח בין תחתית הזכוכית לאזור הניווט
NAV_H       = sel([16, 20, 20]);  // אזור D-pad + מקשי soft + שיחה/ניתוק
ROW_P       = sel([8, 9.5, 10]);  // פסיעת שורות במקלדת הספרות
KEY_P       = sel([14, 17, 21]);  // פסיעת עמודות
BOTTOM_BAND = 5;                  // מיקרופון, USB-C
SIDE_WALL   = 1.0;                // מסגרת סביב הזכוכית - מסך מקצה לקצה
CORNER_R    = sel([6, 7, 8]);
FILLET_BACK  = sel([2.5, 2.0, 3]); // עיגול קצה אחורי של הגוף (Regular: 2.0 - בגוף של 10 מ"מ מקשי הווליום יורדים נמוך יותר)
FILLET_FRONT = 0.6;                // עיגול קצה קדמי (סביב הזכוכית)
PUNCH_D      = 3.2;                // חור מצלמה קדמית בתוך המסך
BODY_T      = sel([12, 10, 12.5]);   // Regular: 10 מ"מ מקסימום
FRONT_T     = 1.4;                 // עובי דופן פנים
MIDFRAME    = 0.6;                 // לוחית תמיכה + דבק בין תחתית מודול המסך לסוללה
LCD_SINK    = 0.35;                // הזכוכית שקועה מתחת לפני החזית
WALL        = 1.4;

// Regular: 47 ולא 48 - מפנה מקום למקשי הווליום בדופן הימנית (ר' VOL_* למטה)
BATT        = sel([[36, 62, 4.0], [47, 83, 4.5], [66, 74, 4.7]]); // Li-Po, מ"מ (1500/3000/4000mAh)
BOARD       = sel([[40, 55, 1.0], [48, 70, 1.0], [76, 58, 1.0]]); // PCBA (ללא מגנים) - צר מספיק לגוף מקצה לקצה
BOARD_Z     = sel([3.2, 3.5, 3.5]); // גובה רכיבים + מגני RF

DIGIT_ROWS  = 4;

// מקשי ווליום: שני מקשים נפרדים (Vol+ למעלה, Vol- למטה) בדופן הימנית,
// כמו ב-F22 Pro. נלחצים על כיפות מתכת על FPC צד שמודבק לדופן מבפנים.
VOL_L       = sel([9, 10, 11]);  // אורך כל מקש (לאורך הגוף)
VOL_H       = 3.0;               // גובה המקש (בעובי הגוף)
VOL_SPACING = 1.6;               // מרווח בין Vol+ ל-Vol-
VOL_FROM_TOP = sel([30, 36, 38]); // מרכז הזוג, מ"מ מהקצה העליון - בהישג האצבע כשמחזיקים ביד
VOL_PROUD   = 0.6;               // כמה המקש בולט מהדופן
VOL_FLANGE  = 0.8;               // שפה פנימית סביב המקש שמונעת ממנו ליפול החוצה
VOL_FLANGE_T = 0.4;
VOL_PLUNGER = 0.2;               // בליטה קטנה מאחורי המקש שלוחצת על הכיפה
VOL_FPC_T   = 0.7;               // כיפה 0.3 + FPC 0.12 + stiffener פלדה 0.2 + דבק
VOL_NUB     = 1.0;               // נקודה מורגשת על Vol+ - לזיהוי בלי להסתכל

// ---------------------------------------------------------------------
// פריסה אנכית (מלמטה למעלה, Y=0 בתחתית הגוף)
// ---------------------------------------------------------------------
DIGITS_H   = DIGIT_ROWS * ROW_P;
SCR_OUT_W  = SCR_W + 2 * LCD_SIDE;
SCR_OUT_H  = SCR_H + LCD_SIDE + LCD_BOTTOM;

Y_DIGITS   = BOTTOM_BAND;
Y_NAV      = Y_DIGITS + DIGITS_H;
Y_SCR      = Y_NAV + NAV_H + NAV_GAP;
BODY_L     = Y_SCR + SCR_OUT_H + TOP_BAND;
BODY_W     = max(SCR_OUT_W, 3 * KEY_P) + 2 * SIDE_WALL;

echo(str("MODEL=", MODEL,
         "  body W x L x T = ", BODY_W, " x ", BODY_L, " x ", BODY_T, " mm",
         "  active area = ", SCR_W, " x ", SCR_H));

// מקשי ווליום: Z במערכת ההרכבה (0 = פני החזית), בגובה אמצע הסוללה - מתחת
// למודול המסך ומעל עיגול הקצה האחורי. Y: [Vol+, Vol-]
BATT_TOP_Z = -(LCD_SINK + LCD_T + MIDFRAME);  // הסוללה יושבת ישר מאחורי מודול המסך
VOL_Z  = BATT_TOP_Z - BATT[2] / 2;
VOL_Y  = [BODY_L - VOL_FROM_TOP + (VOL_L + VOL_SPACING) / 2,
          BODY_L - VOL_FROM_TOP - (VOL_L + VOL_SPACING) / 2];
VOL_IN = VOL_FLANGE_T + VOL_PLUNGER + VOL_FPC_T; // עומק המכלול מתחת לדופן הפנימית
VOL_BATT_CLEAR = (BODY_W - WALL - VOL_IN) - (BODY_W / 2 + BATT[0] / 2);

echo(str("side keys: Vol+ y=", VOL_Y[0], "  Vol- y=", VOL_Y[1], "  z=", VOL_Z,
         "  battery clearance = ", VOL_BATT_CLEAR, " mm"));
assert(VOL_BATT_CLEAR >= 0.25, "מקשי הווליום נכנסים לסוללה - להקטין את BATT[0]");
assert(VOL_Z - VOL_H / 2 > -BODY_T + FILLET_BACK, "מקשי הווליום יושבים על עיגול הקצה האחורי");
assert(VOL_Z + VOL_H / 2 + VOL_FLANGE < -LCD_T - LCD_SINK, "מקשי הווליום נכנסים למודול המסך");
BATT_BACK_CLEAR = (BATT_TOP_Z - BATT[2]) - (-BODY_T + WALL);
echo(str("battery to back wall clearance = ", BATT_BACK_CLEAR, " mm"));
assert(BATT_BACK_CLEAR >= 0.2, "הסוללה נכנסת בדופן האחורית - להגדיל את BODY_T או להקטין את BATT[2]");

// ---------------------------------------------------------------------
// גיאומטריה בסיסית
// ---------------------------------------------------------------------
module rbox(w, l, t, r) {
    hull() for (x = [r, w - r], y = [r, l - r])
        translate([x, y, 0]) cylinder(r = r, h = t);
}

// כמו rbox אבל עם קצוות מעוגלים (fillet) למטה (fb) ו/או למעלה (ft)
module sbox(w, l, t, r, fb = 0, ft = 0) {
    hull() for (x = [r, w - r], y = [r, l - r]) translate([x, y, 0]) {
        if (fb > 0) translate([0, 0, fb]) rotate_extrude() translate([r - fb, 0]) circle(r = fb);
        else cylinder(r = r, h = 0.01);
        if (ft > 0) translate([0, 0, t - ft]) rotate_extrude() translate([r - ft, 0]) circle(r = ft);
        else translate([0, 0, t - 0.01]) cylinder(r = r, h = 0.01);
    }
}

CX = BODY_W / 2;

// מיקומי מקשים: [x, y, w, h, צורה, תווית]
function digit_keys() = [
    for (row = [0 : DIGIT_ROWS - 1], col = [0 : 2])
        [CX + (col - 1) * KEY_P, Y_DIGITS + (DIGIT_ROWS - 1 - row + 0.5) * ROW_P,
         KEY_P - 2.4, ROW_P - 1.6, "rect",
         ["1","2","3","4","5","6","7","8","9","*","0","#"][row * 3 + col]]
];

RING_D = NAV_H - 2;
SIDE_KEY_W = KEY_P - 2.4;
SIDE_KEY_H = (NAV_H - 3) / 2;

function nav_keys() = [
    [CX, Y_NAV + NAV_H / 2, RING_D, RING_D, "ring", "dpad"],
    [CX, Y_NAV + NAV_H / 2, RING_D * 0.42, RING_D * 0.42, "round", "ok"],
    [CX - KEY_P, Y_NAV + NAV_H * 0.75, SIDE_KEY_W, SIDE_KEY_H, "rect", "softL"],
    [CX - KEY_P, Y_NAV + NAV_H * 0.25, SIDE_KEY_W, SIDE_KEY_H, "rect", "call"],
    [CX + KEY_P, Y_NAV + NAV_H * 0.75, SIDE_KEY_W, SIDE_KEY_H, "rect", "softR"],
    [CX + KEY_P, Y_NAV + NAV_H * 0.25, SIDE_KEY_W, SIDE_KEY_H, "rect", "end"]
];

module key_shape(k, grow = 0, h = 1, r = 1.2) {
    translate([k[0], k[1], 0])
    if (k[4] == "rect")
        translate([-(k[2] + grow) / 2, -(k[3] + grow) / 2, 0])
            rbox(k[2] + grow, k[3] + grow, h, min(r, (k[3] + grow) / 2 - 0.01));
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
FRONT_KEYS = concat(nav_keys(), digit_keys());
GAP = 0.25; // מרווח בין מקש לחור

// חלון הזכוכית: כל רוחב החזית פחות מסגרת SIDE_WALL, מעל אזור הניווט ועד הפס העליון.
// הפינות העליונות עוקבות אחרי פינות הגוף כך שהמסך נראה מקצה לקצה.
WIN_W = BODY_W - 2 * SIDE_WALL;
WIN_L = BODY_L - TOP_BAND - Y_SCR;
WIN_R_TOP = max(CORNER_R - SIDE_WALL, 1);
WIN_R_BOT = 2;

module screen_window(t = FRONT_T + 0.02) {
    hull() {
        for (x = [SIDE_WALL + WIN_R_BOT, BODY_W - SIDE_WALL - WIN_R_BOT])
            translate([x, Y_SCR + WIN_R_BOT, 0]) cylinder(r = WIN_R_BOT, h = t);
        for (x = [SIDE_WALL + WIN_R_TOP, BODY_W - SIDE_WALL - WIN_R_TOP])
            translate([x, BODY_L - TOP_BAND - WIN_R_TOP, 0]) cylinder(r = WIN_R_TOP, h = t);
    }
}

module front_shell() {
    difference() {
        sbox(BODY_W, BODY_L, FRONT_T, CORNER_R, 0, FILLET_FRONT);
        translate([0, 0, -0.01]) screen_window();
        // פתח אחד לכל המקלדת - ממברנת TPU אחת במקום חור לכל מקש
        translate([0, 0, -0.01]) keypad_outline(FRONT_KEYS, KP_MARGIN + GAP, FRONT_T + 0.02);
        // חריץ רמקול שיחה בפס העליון הדק
        translate([CX - 5, BODY_L - TOP_BAND / 2 - 0.4, -0.01]) rbox(10, 0.8, FRONT_T + 0.02, 0.39);
    }
}

module back_shell() {
    t = BODY_T - FRONT_T;
    difference() {
        sbox(BODY_W, BODY_L, t, CORNER_R, FILLET_BACK, 0);
        translate([WALL, WALL, WALL]) rbox(BODY_W - 2 * WALL, BODY_L - 2 * WALL, t, CORNER_R - WALL);
        // USB-C בתחתית
        translate([CX - 4.5, -0.01, t / 2 - 1.75]) cube([9, WALL + 0.02, 3.5]);
        // מצלמה אחורית + פלאש
        translate([BODY_W - 11, BODY_L - 12, -0.01]) cylinder(d = sel([7, 8, 11]), h = WALL + 0.02);
        translate([BODY_W - 11, BODY_L - 22, -0.01]) cylinder(d = 3, h = WALL + 0.02);
        // חריצי מקשי ווליום בדופן הימנית
        for (y = VOL_Y) translate([BODY_W - WALL - 0.01, y, VOL_Z + BODY_T])
            pill_x(VOL_L + 2 * GAP, VOL_H + 2 * GAP, WALL + 0.02);
    }
}

// ---------------------------------------------------------------------
// מקשי ווליום
// ---------------------------------------------------------------------
// גלולה (צורת מקש צד) שהציר שלה לאורך X, מ-x=0 עד x=len, ממורכזת ב-Y וב-Z
module pill_x(l, h, len) {
    hull() for (y = [-(l - h) / 2, (l - h) / 2])
        translate([0, y, 0]) rotate([0, 90, 0]) cylinder(d = h, h = len);
}

// מקש צד במערכת מקומית: x=0 הוא פני הדופן הפנימיים, X חיובי החוצה
module side_key(nub = false, plunger = true) {
    tip = WALL + VOL_PROUD;
    translate([-VOL_FLANGE_T, 0, 0]) pill_x(VOL_L + 2 * VOL_FLANGE, VOL_H + 2 * VOL_FLANGE, VOL_FLANGE_T);
    pill_x(VOL_L, VOL_H, tip - 0.3);
    hull() { // קצה חיצוני מעוגל
        translate([tip - 0.31, 0, 0]) pill_x(VOL_L, VOL_H, 0.01);
        translate([tip - 0.01, 0, 0]) pill_x(VOL_L - 0.6, VOL_H - 0.6, 0.01);
    }
    if (plunger)
        translate([-VOL_FLANGE_T - VOL_PLUNGER, 0, 0]) rotate([0, 90, 0]) cylinder(d = 1.5, h = VOL_PLUNGER + 0.01);
    if (nub) translate([tip - 0.05, 0, 0]) scale([0.5, 1, 1]) sphere(d = VOL_NUB);
}

// במקום, במערכת ההרכבה
module side_keys_placed() {
    for (i = [0, 1]) translate([BODY_W - WALL, VOL_Y[i], VOL_Z]) side_key(i == 0);
}

// להדפסה: השפה על המגש, הקצה כלפי מעלה. השמאלי הוא Vol+ (עם הנקודה).
// בלי הבליטה האחורית - בדמה מודפסת אין כיפות, והיא רק הייתה מרימה את השפה מהמגש
module side_keys_print() {
    for (i = [0, 1]) translate([i * (VOL_H + 2 * VOL_FLANGE + 3), 0, VOL_FLANGE_T])
        rotate([0, -90, 0]) side_key(i == 0, plunger = false);
}

// ---------------------------------------------------------------------
// מקלדת: ממברנת TPU אחת עם מקשים נמוכים ומעוגלים + סימונים לבנים
// שמודפסים כגוף נפרד (Keypad_TPU + Keypad_Markings_TPU, הדפסה דו-צבעית)
// ---------------------------------------------------------------------
KP_MARGIN  = 0.8;   // שוליים של הממברנה סביב המקשים
KP_RECESS  = 0.3;   // הממברנה שקועה מעט מתחת לפני החזית
KEY_RISE   = 1.1;   // גובה המקש מעל הממברנה
MARK_DEPTH = 0.4;   // עומק הסימון (שקוע בתוך המקש, ממולא בגוף הסימונים)
FLANGE     = 1.2;   // שפה מתחת לחזית שמחזיקה את הממברנה (נחתכת לפי החלל הפנימי)
FLANGE_T   = 0.6;

LATIN  = ["", "ABC", "DEF", "GHI", "JKL", "MNO", "PQRS", "TUV", "WXYZ"];
HEBREW = ["", "אבג", "דהו", "זחט", "יכל", "מנס", "עפצ", "קרש", "ת"];

function kp_x0(ks) = min([for (k = ks) k[0] - k[2] / 2]);
function kp_x1(ks) = max([for (k = ks) k[0] + k[2] / 2]);
function kp_y0(ks) = min([for (k = ks) k[1] - k[3] / 2]);
function kp_y1(ks) = max([for (k = ks) k[1] + k[3] / 2]);

module keypad_outline(ks, m, h) {
    translate([kp_x0(ks) - m, kp_y0(ks) - m, 0])
        rbox(kp_x1(ks) - kp_x0(ks) + 2 * m, kp_y1(ks) - kp_y0(ks) + 2 * m, h, 3);
}

// גוף מקש מעוגל: פינות כמעט חצי-עיגול וראש כיפתי (שכבות מוקטנות בהדרגה בתוך hull)
KEY_ROUND = 0.8;   // 0..1 - חלק מחצי גובה המקש שמשמש רדיוס פינה
DOME = [[0, 0], [0.45, 0.1], [0.75, 0.35], [0.92, 0.7], [1, 1.2]]; // [גובה יחסי, הקטנה במ"מ]

module key_dome(k) {
    r = KEY_ROUND * k[3] / 2;
    hull() for (d = DOME)
        translate([0, 0, d[0] * (KEY_RISE - 0.05)]) key_shape(k, -d[1], 0.05, r);
}

module key_body(k) {
    if (k[4] == "ring")
        difference() {
            key_dome([k[0], k[1], k[2], k[3], "round"]);
            translate([k[0], k[1], -0.01]) cylinder(d = k[2] * 0.42 + 1.2, h = KEY_RISE + 0.02);
        }
    else key_dome(k);
}

// אייקונים מהפונט המובנה של Windows (Segoe MDL2 Assets), מעובים מעט כדי שיהיו ניתנים להדפסה
ICON_FONT = "Segoe MDL2 Assets";
ICONS = [["softL", 59136], ["call", 59159], ["softR", 59303], ["end", 59368]]; // תפריט, שיחה, חזרה, הפעלה

module icon2d(code, size) {
    offset(r = 0.15) text(chr(code), size = size, font = ICON_FONT, halign = "center", valign = "center");
}

// סימון דו-ממדי של מקש, במרכז (0,0) של המקש
module key_label2d(k) {
    n = k[5]; w = k[2]; h = k[3];
    ts = min(w, h);
    ic = [for (c = ICONS) if (c[0] == n) c[1]];
    if (n == "dpad") {
        // בלי חצים - טבעת נקייה
    } else if (n == "ok") {
        text("OK", size = ts * 0.28, font = "Arial:style=Bold", halign = "center", valign = "center");
    } else if (len(ic) > 0) {
        icon2d(ic[0], ts * 0.55);
    } else {
        if (n >= "2" && n <= "9") {
            i = ord(n) - 49;
            translate([-w * 0.2, 0])
                text(n, size = h * 0.5, font = "Arial:style=Bold", halign = "center", valign = "center");
            translate([w * 0.17, h * 0.19])
                text(HEBREW[i], size = h * 0.2, font = "Arial", halign = "center", valign = "center",
                     direction = "rtl", script = "hebrew", language = "he");
            translate([w * 0.17, -h * 0.19])
                text(LATIN[i], size = h * 0.2, font = "Arial", halign = "center", valign = "center");
        } else {
            text(n, size = h * 0.5, font = "Arial:style=Bold", halign = "center", valign = "center");
        }
    }
}

// הסימון הוא "עור" בעובי MARK_DEPTH מתחת לפני הכיפה - עוקב אחרי הקימור, לא בולט החוצה
module key_marks(k) {
    intersection() {
        difference() {
            key_body(k);
            translate([0, 0, -MARK_DEPTH]) key_body(k);
        }
        translate([k[0], k[1], -0.01]) linear_extrude(height = KEY_RISE + 0.02) key_label2d(k);
    }
}

// הממברנה + המקשים. מערכת צירים: Z=0 הוא גב החזית, Z=FRONT_T הוא פני החזית
module keypad(ks) {
    top = FRONT_T - KP_RECESS;
    intersection() {
        translate([0, 0, -FLANGE_T]) keypad_outline(ks, KP_MARGIN + FLANGE, FLANGE_T);
        translate([WALL + 0.2, WALL + 0.2, -FLANGE_T - 0.01])
            rbox(BODY_W - 2 * WALL - 0.4, BODY_L - 2 * WALL - 0.4, FLANGE_T + 0.02, CORNER_R - WALL);
    }
    keypad_outline(ks, KP_MARGIN, top);
    // כל מקש מחושב כגוף סגור אחד (render) - אחרת עץ ה-CSG מתפוצץ ב-F5
    for (k = ks) translate([0, 0, top - 0.01])
        render() difference() { key_body(k); key_marks(k); }
}

module keypad_markings(ks) {
    for (k = ks) translate([0, 0, FRONT_T - KP_RECESS - 0.01]) render() key_marks(k);
}

KEYPAD_COLOR = "#2a2a2d";
BODY_COLOR   = "#1c1c1e";

module keepouts() {
    inner_z = FRONT_T;
    // מודול המסך
    color("SteelBlue", 0.8)
        translate([CX - SCR_OUT_W / 2, Y_SCR, -LCD_T + FRONT_T - LCD_SINK]) cube([SCR_OUT_W, SCR_OUT_H, LCD_T]); // מתחת לזכוכית
    // סוללה - מאחורי המסך
    color("Orange", 0.8)
        translate([CX - BATT[0] / 2, BODY_L - TOP_BAND - BATT[1], BATT_TOP_Z + FRONT_T - BATT[2]])
            cube(BATT);
    // PCBA - מאחורי אזור המקלדת
    color("ForestGreen", 0.8)
        translate([CX - BOARD[0] / 2, BOTTOM_BAND - 1, -BOARD_Z - 1.5]) cube([BOARD[0], BOARD[1], BOARD_Z]);
    // FPC צד של מקשי הווליום (כיפות + stiffener), מודבק לדופן הימנית מבפנים
    color("Gold", 0.8)
        translate([BODY_W - WALL - VOL_IN, VOL_Y[1] - VOL_L / 2 - VOL_FLANGE, VOL_Z + FRONT_T - VOL_H / 2 - VOL_FLANGE])
            cube([VOL_FPC_T, VOL_Y[0] - VOL_Y[1] + VOL_L + 2 * VOL_FLANGE, VOL_H + 2 * VOL_FLANGE]);
}

// ---------------------------------------------------------------------
// הרכבה
// ---------------------------------------------------------------------
module assembly() {
    // המערכת: Z=0 הוא הפנים הקדמיים, הגוף נבנה כלפי Z שלילי
    color(BODY_COLOR) translate([0, 0, -FRONT_T]) front_shell();
    translate([0, 0, -FRONT_T]) {
        color(KEYPAD_COLOR) keypad(FRONT_KEYS);
        color("WhiteSmoke") keypad_markings(FRONT_KEYS);
    }
    // זכוכית מקצה לקצה + אזור פעיל + punch-hole
    color("Black") translate([0, 0, -0.35]) screen_window(0.3);
    color("#0d1422") translate([CX - SCR_W / 2, Y_SCR + LCD_BOTTOM, -0.04]) cube([SCR_W, SCR_H, 0.02]);
    color("Black") translate([CX, BODY_L - TOP_BAND - LCD_SIDE - PUNCH_D, -0.02]) cylinder(d = PUNCH_D, h = 0.04);
    translate([0, 0, -FRONT_T]) keepouts();
    color(BODY_COLOR) translate([0, 0, -BODY_T]) back_shell();
    color(KEYPAD_COLOR) side_keys_placed();
}

if (PART == "assembly")         assembly();
else if (PART == "front_shell") front_shell();
else if (PART == "back_shell")  back_shell();
else if (PART == "keypad")      keypad(FRONT_KEYS);
else if (PART == "markings")    keypad_markings(FRONT_KEYS);
else if (PART == "side_keys")   side_keys_print();
else if (PART == "keepouts")    keepouts();
