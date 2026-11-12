// =====================================================================
//  מארז + מקלדת (keycaps) לטלפון Qin F22 Pro ("קין F22 פרו")
//  קובץ OpenSCAD פרמטרי מלא - כל המידות מוגדרות כמשתנים בראש הקובץ.
//
//  *** חשוב - קרא לפני הדפסה ***
//  המידות הכלליות של גוף הטלפון והמסך מבוססות על מקורות חיצוניים
//  (ראו "מקורות" למטה) ותואמות לרזולוציית המסך 640x960 המוגדרת גם
//  ב-CLAUDE.md של הפרויקט. עם זאת, פריסת הכפתורים המדויקת (מיקומי
//  XY, גדלים, ומרווחים) אינה מבוססת על מדידה אמיתית של המכשיר -
//  היא הערכה סבירה של פריסת מקלדת T9 סטנדרטית. לפני הדפסה:
//    1. מדדו את גוף הטלפון בקליבר ועדכנו PHONE_LENGTH/WIDTH/THICKNESS.
//    2. מדדו את מיקום וגודל כל כפתור בפועל ועדכנו את המערך KEY_LAYOUT.
//    3. הדפיסו טסט חתוך (test slice) של המסגרת הקדמית בלבד ובדקו
//       יישור מול הטלפון האמיתי לפני הדפסת המארז המלא.
//
//  מקורות:
//    - github.com/shuuryou/f22pro (מדידה פיזית בפועל: 151x61x11 מ"מ,
//      מסך 3.54" 640x960 - תואם ל-CLAUDE.md)
//    - qinphone.com (מפרט יצרן רשמי: 147x58x9 מ"מ, 116 גרם)
//    - gsmchoice.com (מפרט משני, תואם בערך למפרט היצרן)
//  עדכון 2026-10-05: כל המידות (גוף, מסך, רשת המקלדת, מצלמת הסלפי
//  שליד #, פתחים) נמדדו מתמונות של המכשיר עם כרטיס בגודל כרטיס אשראי
//  כסקאלה. הן יוצאות קרובות למפרט היצרן (147x58x9). הדיוק ±1 מ"מ, ולכן
//  חובה להדפיס קודם חתך ניסיון.
//
//  איך להשתמש (משתנה PART למטה, או ב-Customizer של OpenSCAD):
//    "back_shell"   - חלק אחורי (תא) - להדפיס כמות: 1
//    "front_frame"  - מסגרת קדמית עם חלון מסך + חלונות מקלדת - להדפיס כמות: 1
//    "keycaps"      - כל הכפתורים הבודדים פרוסים למשטח הדפסה - להדפיס כמות: 1 סט
//    "assembly"     - תצוגה מקדימה בלבד (הכל מורכב יחד) - לא להדפסה!
// =====================================================================

$fn = 64;

part = "assembly"; // [back_shell:חלק אחורי, front_frame:מסגרת קדמית, keycaps:כפתורים, assembly:תצוגה מקדימה]

// =====================================================================
// 1. מידות גוף הטלפון (מ"מ) - ראו הערת מקורות למעלה
// =====================================================================
// עודכן 2026-10-05 לפי תמונות עם כרטיס בגודל כרטיס אשראי (85.6x54) כסקאלה,
// בלי קליבר - דיוק של כ-±1 מ"מ. המידות תואמות למפרט היצרן (147x58x9);
// המידה הקודמת (151x61x11) הייתה כנראה עם כיסוי.
PHONE_LENGTH    = 147;   // אורך כולל (ציר Y)
PHONE_WIDTH     = 57.5;  // רוחב כולל (ציר X)
PHONE_THICKNESS = 9.0;   // עובי כולל (ציר Z)
PHONE_CORNER_R  = 5;     // רדיוס פינות (מבט מלמעלה)

// טולרנס - כמה "אוויר" להוסיף סביב הטלפון כדי שהמארז ייכנס בלי להידחק
FIT_CLEARANCE = 0.35;

// =====================================================================
// 2. מידות מארז
// =====================================================================
WALL       = 1.6;   // עובי דופן כללי
FRAME_H    = 2.2;   // גובה המסגרת הקדמית (השפה שעוטפת את קצה המסך)
LIP_DEPTH  = 1.4;   // עומק ה"תקע" של המסגרת הקדמית לתוך התא האחורי (snap/friction fit)
LIP_GAP    = 0.25;  // טולרנס נוסף בין התקע לתא, כדי שיכנס בלחיצה סבירה

BACK_OUTER_L = PHONE_LENGTH + 2 * FIT_CLEARANCE + 2 * WALL;
BACK_OUTER_W = PHONE_WIDTH  + 2 * FIT_CLEARANCE + 2 * WALL;
// הדפנות עולות מעל פני הטלפון בגובה LIP_DEPTH, כדי שהתקע של המסגרת הקדמית
// ייכנס ביניהן וישב על פני הטלפון (לפני כן התקע נחתך לתוך גוף הטלפון).
BACK_OUTER_T = WALL + PHONE_THICKNESS + FIT_CLEARANCE + LIP_DEPTH;
PORT_Z = WALL + PHONE_THICKNESS/2; // גובה מרכז הדופן של הטלפון בתוך התא

FRONT_OUTER_L = BACK_OUTER_L;
FRONT_OUTER_W = BACK_OUTER_W;

// =====================================================================
// 3. מסך - מחושב אוטומטית מתוך יחס 640x960 פיקסלים ואלכסון 3.54"
//    (89.9 מ"מ). ערכי SCREEN_TOP_MARGIN ו-SCREEN_GAP הם הערכה בלבד.
// =====================================================================
SCREEN_DIAG_MM   = 3.54 * 25.4;
SCREEN_RATIO_W   = 640;
SCREEN_RATIO_H   = 960;
SCREEN_K         = SCREEN_DIAG_MM / sqrt(SCREEN_RATIO_W*SCREEN_RATIO_W + SCREEN_RATIO_H*SCREEN_RATIO_H);
SCREEN_W         = SCREEN_RATIO_W * SCREEN_K / 10; // ~49.9 מ"מ
SCREEN_H         = SCREEN_RATIO_H * SCREEN_K / 10; // ~74.8 מ"מ
SCREEN_CUT_MARGIN = 1.0; // "שיחרור" נוסף בכל כיוון כדי לא לחפוף לזכוכית עצמה

SCREEN_TOP_MARGIN = 9.2; // מהקצה העליון של הטלפון עד תחילת האזור המואר - נמדד מתמונה
SCREEN_GAP_TO_KEYS = 1.9; // מתחתית האזור המואר עד שורת המקשים הראשונה - נמדד מתמונה
// המסך ממורכז לרוחב (3.6 מ"מ משני הצדדים)

// מרכז המסך במערכת קואורדינטות שמקורה במרכז גוף הטלפון (0,0)
screen_center_y = PHONE_LENGTH/2 - SCREEN_TOP_MARGIN - SCREEN_H/2;

// =====================================================================
// 4. פריסת מקלדת - נמדד מתמונה (2026-10-05), ±1 מ"מ
//    המקלדת המקורית היא רשת של 6 שורות x 3 עמודות שתופסת את כל רוחב
//    החזית, עם קווי חלוקה דקים בין המקשים. שתי השורות העליונות בעמודה
//    האמצעית הן ה-D-pad העגול (עם OK במרכזו). מצלמת הסלפי יושבת בתוך
//    התא של מקש #, בפינה הימנית-תחתונה.
// =====================================================================
KEY_ROW_H = [8.8, 10.0, 9.9, 9.6, 9.6, 8.9]; // גובה כל שורה, מלמעלה למטה
KEY_COL_W = [18.8, 17.7, 19.2];              // רוחב כל עמודה, משמאל לימין
KEY_GAP   = 2.0;  // רווח בין חלונות המקשים במסגרת (משאיר דופן מודפסת של ~1.3 מ"מ)
KEY_R     = 1.5;  // רדיוס פינות כפתור

// תוויות: "" = מקש פונקציה בלי חריטה, "DPAD" ו-"#" = חלון פתוח בלי כפתור מודפס
KEY_LABELS = [
    ["",  "DPAD", ""],   // מקש רך שמאלי | D-pad | חזור
    ["",  "DPAD", ""],   // חיוג         | D-pad | ניתוק
    ["1", "2",    "3"],
    ["4", "5",    "6"],
    ["7", "8",    "9"],
    ["*", "0",    "#"]
];

DPAD_D = 14.7;   // קוטר חיצוני של ה-D-pad (OK במרכז: 10.6)
// ה-D-pad נשאר חשוף (חלון עגול פתוח): כפתור מודפס אחד לא יכול להעביר 4 כיוונים + OK

FRONT_CAM_CUT_D     = 7.5;   // העדשה ~6.5 מ"מ + שוליים כדי שהמסגרת לא תיכנס לתמונה
FRONT_CAM_FROM_TOP  = 138;   // מרכז מצלמת הסלפי מהקצה העליון
FRONT_CAM_FROM_LEFT = 49.8;  // מרכז מצלמת הסלפי מהקצה השמאלי

// מידות הכפתור המודפס (keycap): פין שנוגע במקש המקורי, פלנג' שנתפס מתחת
// לעור העליון של המסגרת, וגוף שבולט החוצה דרך החלון.
KEYCAP_NUB_H     = 1.0;
KEYCAP_FLANGE_H  = 1.0;
KEYCAP_PLUNGER_H = 2.4;
KEYCAP_FLANGE_MARGIN = 1.2; // כמה הפלנג' רחב מהחלון - מוגבל כי בין המקשים יש רק 2 מ"מ
KEY_TRAVEL       = 0.5;     // מרווח אנכי לפלנג' בתוך הכיס

function sum_to(v, n) = n <= 0 ? 0 : v[n-1] + sum_to(v, n-1);

keypad_top_y = PHONE_LENGTH/2 - SCREEN_TOP_MARGIN - SCREEN_H - SCREEN_GAP_TO_KEYS;
keypad_x0    = -KEY_COL_W[1]/2 - KEY_COL_W[0]; // העמודה האמצעית ממורכזת
function cell_cy(r) = keypad_top_y - sum_to(KEY_ROW_H, r) - KEY_ROW_H[r]/2;
function cell_cx(c) = keypad_x0 + sum_to(KEY_COL_W, c) + KEY_COL_W[c]/2;

dpad_y      = keypad_top_y - KEY_ROW_H[0]; // מרכז ה-D-pad = הקו שבין שתי השורות העליונות
front_cam_x = FRONT_CAM_FROM_LEFT - PHONE_WIDTH/2;
front_cam_y = PHONE_LENGTH/2 - FRONT_CAM_FROM_TOP;

// התא של # נשאר חלון פתוח (בלי כפתור מודפס): מצלמת הסלפי יושבת בו, ומסגרת
// בעובי 3.6 מ"מ מעל העדשה עם חור צר הייתה מסתירה חלק גדול משדה הראייה.
// לוחצים על # המקורי ישירות דרך החלון.
// רשימת המקשים: [תווית, מרכז X, מרכז Y, רוחב, גובה, צורה]
//   "rect" = כפתור מודפס, "openrect" / "open" = חלון פתוח מלבני / עגול
KEYS = concat(
    [for (r = [0:5], c = [0:2])
        if (KEY_LABELS[r][c] != "DPAD" && KEY_LABELS[r][c] != "#")
            [KEY_LABELS[r][c], cell_cx(c), cell_cy(r), KEY_COL_W[c] - KEY_GAP, KEY_ROW_H[r] - KEY_GAP, "rect"]],
    [["", cell_cx(2), cell_cy(5), KEY_COL_W[2] - KEY_GAP, KEY_ROW_H[5] - KEY_GAP, "openrect"]],
    [["", 0, dpad_y, DPAD_D, DPAD_D, "open"]]
);

// =====================================================================
// 5. פתחי חיבור וכפתורי צד - נמדד מתמונות (2026-10-05), ±1.5 מ"מ
//    X חיובי = צד ימין במבט מהחזית
// =====================================================================
USB_C_W = 9;     // שקע USB-C (8.34x2.56 תקני) + מרווח
USB_C_H = 3.2;
USB_C_CENTER_X = 0; // ממורכז בקצה התחתון

MIC_D = 2;
MIC_CENTER_X = -8;  // חור המיקרופון בקצה התחתון, כ-8 מ"מ משמאל ל-USB-C

// מגש SIM בצד שמאל (אושר ע"י המשתמש). בתמונת הצד נראה חור שחרור (pinhole)
// כ-40 מ"מ מהקצה העליון, אבל לא ברור לאיזה כיוון המגש נמשך ממנו, ולכן
// הפתח ארוך (25-55 מ"מ מלמעלה) ומכסה את שתי האפשרויות. אין כפתורים בצד שמאל.
HAS_SIM_TRAY = true;
SIM_TRAY_W = 4;     // גובה הפתח (בכיוון העובי)
SIM_TRAY_H = 30;    // אורך הפתח לאורך הצד
SIM_TRAY_CENTER_Y = PHONE_LENGTH/2 - 40;

VOL_BTN_W = 2.5;
VOL_BTN_H = 21;                          // מקש עוצמה כפול, כ-20 מ"מ
VOL_BTN_CENTER_Y = PHONE_LENGTH/2 - 43;  // צד ימין, 33-53 מ"מ מהקצה העליון

// חלון כהה מוארך (צורת אצטדיון, ~10x3.7 מ"מ) בקצה העליון, עם נקודה כהה בצד
// השמאלי שלו. נראה כמו חלון IR ולא כמו שקע אוזניות (שהוא חור עגול ועמוק),
// אבל זה לא ודאי - לכן הפתח גבוה מספיק (6.5 מ"מ) גם לגוף של תקע 3.5 מ"מ.
TOP_WINDOW_W = 12;
TOP_WINDOW_H = 6.5;
TOP_WINDOW_CENTER_X = 15.5; // מימין למרכז

HAS_HEADPHONE_JACK = false; // לא נראה בתמונות - הפעילו רק אם יש בפועל
AUDIO_JACK_D = 6;
AUDIO_JACK_CENTER_X = PHONE_WIDTH/4;

// מודול המצלמה האחורית: קפסולה אנכית, עדשה למעלה ופלאש מתחת
REAR_CAM_W = 10;
REAR_CAM_H = 20.5;
REAR_CAM_CENTER_X = -1.5; // כמעט ממורכז (במבט מהחזית)
REAR_CAM_CENTER_Y = PHONE_LENGTH/2 - 25.3;

SPEAKER_W = 14;
SPEAKER_H = 3;
SPEAKER_CENTER_X = 0;
SPEAKER_CENTER_Y = -PHONE_LENGTH/2 + 6;

// =====================================================================
// מודולים גיאומטריים בסיסיים
// =====================================================================

// מלבן עם פינות מעוגלות (2D), ממורכז סביב הראשית
module rounded_rect(w, h, r) {
    r2 = min(r, min(w, h) / 2 - 0.01);
    hull() {
        for (sx = [-1, 1], sy = [-1, 1])
            translate([sx * (w/2 - r2), sy * (h/2 - r2)])
                circle(r = r2);
    }
}

// קופסה עם פינות מעוגלות (3D) - שכבת 2D מוחלקת לגובה נתון
module rounded_box(w, h, t, r) {
    linear_extrude(height = t)
        rounded_rect(w, h, r);
}

// =====================================================================
// 6. חלק אחורי (תא) - נגרר על הטלפון מלמטה, פתוח כלפי הקדמה
// =====================================================================
module back_shell() {
    difference() {
        rounded_box(BACK_OUTER_W, BACK_OUTER_L, BACK_OUTER_T, PHONE_CORNER_R + WALL);

        // חלל פנימי לטלפון - פתוח כלפי מעלה (Z+)
        translate([0, 0, WALL])
            rounded_box(
                BACK_OUTER_W - 2*WALL,
                BACK_OUTER_L - 2*WALL,
                BACK_OUTER_T, // גבוה מדי בכוונה, ייחתך ע"י גבול הגוף החיצוני
                max(PHONE_CORNER_R - WALL, 0.5)
            );

        // פתח מודול מצלמה אחורית (עדשה + פלאש)
        translate([REAR_CAM_CENTER_X, REAR_CAM_CENTER_Y, -0.5])
            linear_extrude(height = WALL + 1)
                rounded_rect(REAR_CAM_W + 2*FIT_CLEARANCE, REAR_CAM_H + 2*FIT_CLEARANCE, REAR_CAM_W/2);

        // רשת רמקול (מספר חריצים)
        for (i = [-1.5:1:1.5])
            translate([SPEAKER_CENTER_X + i * (SPEAKER_W/4), SPEAKER_CENTER_Y, -0.5])
                cube([SPEAKER_W/4 - 0.6, SPEAKER_H, WALL + 1], center = true);

        // פתח USB-C בקצה תחתון
        translate([USB_C_CENTER_X, -BACK_OUTER_L/2, PORT_Z])
            cube([USB_C_W + 2*FIT_CLEARANCE, WALL*3, USB_C_H + 2*FIT_CLEARANCE], center = true);

        // חור מיקרופון בקצה תחתון
        translate([MIC_CENTER_X, -BACK_OUTER_L/2, PORT_Z])
            rotate([90, 0, 0])
                cylinder(d = MIC_D + 2*FIT_CLEARANCE, h = WALL*3, center = true);

        // חלון כהה בקצה העליון (IR)
        translate([TOP_WINDOW_CENTER_X, BACK_OUTER_L/2, PORT_Z])
            rotate([90, 0, 0])
                linear_extrude(height = WALL*3, center = true)
                    rounded_rect(TOP_WINDOW_W + 2*FIT_CLEARANCE, TOP_WINDOW_H + 2*FIT_CLEARANCE, TOP_WINDOW_H/2);

        // מגש SIM/microSD בצד שמאל - רק אם אושר מיקומו
        if (HAS_SIM_TRAY)
            translate([-BACK_OUTER_W/2, SIM_TRAY_CENTER_Y, PORT_Z])
                cube([WALL*3, SIM_TRAY_H, SIM_TRAY_W + 2*FIT_CLEARANCE], center = true);

        // כפתורי ווליום בצד ימין (פתח מעבר - הכפתור עצמו נשאר על הטלפון)
        translate([BACK_OUTER_W/2, VOL_BTN_CENTER_Y, PORT_Z])
            cube([WALL*3, VOL_BTN_H, VOL_BTN_W + 2*FIT_CLEARANCE], center = true);

        // ג'ק אוזניות 3.5 מ"מ - רק אם אושר קיומו במכשיר
        if (HAS_HEADPHONE_JACK)
            translate([AUDIO_JACK_CENTER_X, BACK_OUTER_L/2, PORT_Z])
                rotate([90, 0, 0])
                    cylinder(d = AUDIO_JACK_D + 2*FIT_CLEARANCE, h = WALL*3, center = true);
    }
}

// =====================================================================
// 7. מסגרת קדמית - חלון מסך + חלונות למקשים, עם "תקע" תחתון שנכנס לתא האחורי
// =====================================================================
module front_frame() {
    difference() {
        union() {
            // גוף המסגרת העליון (הנראה מבחוץ)
            rounded_box(FRONT_OUTER_W, FRONT_OUTER_L, FRAME_H, PHONE_CORNER_R + WALL);

            // תקע תחתון שנכנס לתוך פתח התא האחורי (friction fit)
            translate([0, 0, -LIP_DEPTH])
                rounded_box(
                    BACK_OUTER_W - 2*WALL - LIP_GAP,
                    BACK_OUTER_L - 2*WALL - LIP_GAP,
                    LIP_DEPTH + 0.01,
                    max(PHONE_CORNER_R - WALL, 0.5)
                );
        }

        // חלון מסך
        translate([0, screen_center_y, -LIP_DEPTH - 1])
            linear_extrude(height = FRAME_H + LIP_DEPTH + 2)
                rounded_rect(SCREEN_W + 2*SCREEN_CUT_MARGIN, SCREEN_H + 2*SCREEN_CUT_MARGIN, 2);

        // חלונות המקשים (כולל חלון עגול פתוח ל-D-pad)
        for (k = KEYS) {
            kw = k[3]; kh = k[4]; shape = k[5];
            translate([k[1], k[2], -LIP_DEPTH - 1])
                linear_extrude(height = FRAME_H + LIP_DEPTH + 2)
                    if (shape == "open" || shape == "round")
                        circle(d = kw + 2*FIT_CLEARANCE);
                    else
                        rounded_rect(kw + 2*FIT_CLEARANCE, kh + 2*FIT_CLEARANCE, KEY_R);
        }

        // כיס לפלנג' של כל כפתור, בצד התחתון של המסגרת (נשאר עור עליון של
        // LIP_DEPTH + FRAME_H - pocket_h). הכיסים של מקשים סמוכים מתחברים - זה בסדר.
        pocket_h = KEYCAP_NUB_H + KEYCAP_FLANGE_H + KEY_TRAVEL;
        for (k = KEYS)
            if (k[5] == "rect")
                translate([k[1], k[2], -LIP_DEPTH - 1])
                    linear_extrude(height = pocket_h + 1)
                        rounded_rect(k[3] + KEYCAP_FLANGE_MARGIN + 2*FIT_CLEARANCE,
                                     k[4] + KEYCAP_FLANGE_MARGIN + 2*FIT_CLEARANCE, KEY_R);

        // פתח מצלמת הסלפי (בתוך אזור המקלדת, ליד #)
        translate([front_cam_x, front_cam_y, -LIP_DEPTH - 1])
            cylinder(d = FRONT_CAM_CUT_D, h = FRAME_H + LIP_DEPTH + 2);
    }
}

// =====================================================================
// 8. כפתור בודד (keycap) - "פלנג'" עליון שנשען על מסגרת המקלדת + פין
//    תחתון קצר שמעביר לחיצה לכפתור המקורי (הכיפה/גומי) של הטלפון עצמו
// =====================================================================
module keycap(w, h, label, shape = "rect", plunger_h = KEYCAP_PLUNGER_H, flange_h = KEYCAP_FLANGE_H, nub_h = KEYCAP_NUB_H) {
    flange_margin = KEYCAP_FLANGE_MARGIN;

    union() {
        // פלנג' עליון (נשען על שפת החלון במסגרת הקדמית)
        if (shape == "round") {
            cylinder(d = w + flange_margin, h = flange_h);
        } else {
            linear_extrude(height = flange_h)
                rounded_rect(w + flange_margin, h + flange_margin, KEY_R);
        }

        // גוף המקש שעובר דרך חלון המסגרת
        translate([0, 0, flange_h])
            if (shape == "round")
                cylinder(d = w, h = plunger_h);
            else
                linear_extrude(height = plunger_h)
                    rounded_rect(w, h, KEY_R);

        // פין קטן בתחתית שנוגע בכפתור המקורי של הטלפון
        translate([0, 0, -nub_h])
            if (shape == "round")
                cylinder(d = w * 0.5, h = nub_h);
            else
                linear_extrude(height = nub_h)
                    rounded_rect(w * 0.5, h * 0.5, 1);

        // תווית חרוטה על פני הכפתור
        if (len(label) > 0)
            translate([0, 0, flange_h + plunger_h - 0.4])
                linear_extrude(height = 0.5)
                    text(label, size = min(w, h) * 0.55, halign = "center", valign = "center", font = "Arial:style=Bold");
    }
}

// כל הכפתורים פרוסים זה ליד זה על משטח הדפסה (לא במיקומם הסופי) - לפריט "keycaps"
module keycaps_plate() {
    spacing = 4;
    cols = 4;

    all_keys = [for (k = KEYS) if (k[5] == "rect") k]; // D-pad ו-# נשארים חשופים, בלי כפתור
    cell_w = max(KEY_COL_W) + spacing;
    cell_h = max(KEY_ROW_H) + spacing;

    for (i = [0:len(all_keys)-1]) {
        k = all_keys[i];
        cx = (i % cols) * cell_w;
        cy = -floor(i / cols) * cell_h;
        translate([cx, cy, 0])
            keycap(k[3], k[4], k[0], k[5]);
    }
}

// =====================================================================
// 9. בחירת פלט לפי משתנה PART
// =====================================================================
if (part == "back_shell") {
    back_shell();
} else if (part == "front_frame") {
    front_frame();
} else if (part == "keycaps") {
    keycaps_plate();
} else { // "assembly" - לתצוגה מקדימה בלבד, לא לחיתוך STL להדפסה!
    color("SlateGray") back_shell();
    color("LightSteelBlue", 0.85)
        translate([0, 0, BACK_OUTER_T - LIP_DEPTH])
            front_frame();
    // הכפתורים במקומם: הפין נוגע בפני הטלפון, בתחתית התקע של המסגרת
    color("Gainsboro")
        for (k = KEYS)
            if (k[5] == "rect")
                translate([k[1], k[2], BACK_OUTER_T - 2*LIP_DEPTH + KEYCAP_NUB_H])
                    keycap(k[3], k[4], k[0], k[5]);
}
