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
//  ברירת המחדל למטה משתמשת במידה הפיזית הנמדדת (151x61x11) כי היא
//  שמרנית יותר (טולרנס בטוח יותר למארז נגרר-על - slide-on).
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
PHONE_LENGTH    = 151;   // אורך כולל (ציר Y)
PHONE_WIDTH     = 61;    // רוחב כולל (ציר X)
PHONE_THICKNESS = 11;    // עובי כולל (ציר Z)
PHONE_CORNER_R  = 6;     // רדיוס פינות משוער - לא מאושר, יש לאמת מול המכשיר

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
BACK_OUTER_T = PHONE_THICKNESS + FIT_CLEARANCE + WALL; // מכסה את הגב + חצי מהעובי בערך

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

SCREEN_TOP_MARGIN = 8;   // מרווח מקצה עליון של הטלפון עד תחילת המסך (אזור אוזניה/מצלמה קדמית) - הערכה
SCREEN_GAP_TO_KEYS = 4;  // מרווח בין תחתית המסך לתחילת אזור המקלדת - הערכה

// מרכז המסך במערכת קואורדינטות שמקורה במרכז גוף הטלפון (0,0)
screen_center_y = PHONE_LENGTH/2 - SCREEN_TOP_MARGIN - SCREEN_H/2;

// =====================================================================
// 4. פריסת מקלדת (T9 + ניווט) - נתוני מקום, לא נתונים ממדידה בפועל
// =====================================================================
KEY_W = 13;      // רוחב כפתור מספרי
KEY_H = 9;       // גובה כפתור מספרי
KEY_GAP_X = 3.5;
KEY_GAP_Y = 3;
KEY_R = 1.5;     // רדיוס פינות כפתור

NUMPAD_ROWS = [["1","2","3"], ["4","5","6"], ["7","8","9"], ["*","0","#"]];

numpad_w = 3*KEY_W + 2*KEY_GAP_X;
numpad_h = 4*KEY_H + 3*KEY_GAP_Y;

// שורת ניווט מעל המקלדת המספרית: [שם, dx ממרכז, רוחב, גובה, "round"/"rect"]
NAV_ROW_H = 11;
NAV_ROW = [
    ["*/A", -numpad_w/2 + 6.5, 13, NAV_ROW_H, "rect"],  // מקש קיצור/שפה (הסמל בפינה שמאל-עליונה)
    ["OK",   0,                15, 15,        "round"], // כפתור עיגול מרכזי - ניווט/אישור
    ["BACK", numpad_w/2 - 12.5, 13, NAV_ROW_H, "rect"],  // חזרה
    ["PWR",  numpad_w/2 - 12.5, 13, NAV_ROW_H, "rect"],  // הפעלה/ניתוק - ממוקם מתחת ל-BACK בפועל, לכן מוזז ב-Y בלולאה למטה
];

// תחתית אזור המקלדת (Y) - מבוסס על השארת שוליים סבירים מתחתית הטלפון
keys_bottom_margin = 8;
numpad_bottom_y = -PHONE_LENGTH/2 + keys_bottom_margin;
numpad_center_y = numpad_bottom_y + numpad_h/2;
nav_row_center_y = numpad_center_y + numpad_h/2 + KEY_GAP_Y + NAV_ROW_H/2;

// =====================================================================
// 5. פתחי חיבור/כפתורים צדדיים - הערכת מיקום, יש לאמת
// =====================================================================
USB_C_W = 9;
USB_C_H = 3.2;
USB_C_CENTER_X = 0; // מרכז הטלפון, בקצה התחתון (Y שלילי מקסימלי)

SIM_TRAY_W = 2;
SIM_TRAY_H = 12;
SIM_TRAY_CENTER_Y = 10; // מהמרכז, בצד שמאל של הטלפון (X שלילי)

VOL_BTN_W = 2;
VOL_BTN_H = 14;
VOL_BTN_CENTER_Y = 15; // בצד ימין של הטלפון (X חיובי)

HAS_HEADPHONE_JACK = false; // לא אושר קיומו במכשיר זה - הפעילו רק אם יש בפועל
AUDIO_JACK_D = 6;
AUDIO_JACK_CENTER_X = PHONE_WIDTH/4;

REAR_CAM_D = 9;
REAR_CAM_CENTER_X = 0;
REAR_CAM_CENTER_Y = PHONE_LENGTH/2 - 14;

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

        // פתח מצלמה אחורית
        translate([REAR_CAM_CENTER_X, REAR_CAM_CENTER_Y, -0.5])
            cylinder(d = REAR_CAM_D + 2*FIT_CLEARANCE, h = WALL + 1);

        // רשת רמקול (מספר חריצים)
        for (i = [-1.5:1:1.5])
            translate([SPEAKER_CENTER_X + i * (SPEAKER_W/4), SPEAKER_CENTER_Y, -0.5])
                cube([SPEAKER_W/4 - 0.6, SPEAKER_H, WALL + 1], center = true);

        // פתח USB-C בקצה תחתון
        translate([USB_C_CENTER_X, -BACK_OUTER_L/2, BACK_OUTER_T/2])
            cube([USB_C_W + 2*FIT_CLEARANCE, WALL*3, USB_C_H + 2*FIT_CLEARANCE], center = true);

        // מגש SIM/microSD בצד שמאל
        translate([-BACK_OUTER_W/2, SIM_TRAY_CENTER_Y, BACK_OUTER_T/2])
            cube([WALL*3, SIM_TRAY_H, SIM_TRAY_W + 2*FIT_CLEARANCE], center = true);

        // כפתורי ווליום בצד ימין (פתח מעבר - הכפתור עצמו נשאר על הטלפון)
        translate([BACK_OUTER_W/2, VOL_BTN_CENTER_Y, BACK_OUTER_T/2])
            cube([WALL*3, VOL_BTN_H, VOL_BTN_W + 2*FIT_CLEARANCE], center = true);

        // ג'ק אוזניות 3.5 מ"מ - רק אם אושר קיומו במכשיר
        if (HAS_HEADPHONE_JACK)
            translate([AUDIO_JACK_CENTER_X, BACK_OUTER_L/2, BACK_OUTER_T/2])
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

        // חלונות שורת ניווט
        for (k = NAV_ROW) {
            label = k[0]; dx = k[1]; kw = k[2]; kh = k[3]; shape = k[4];
            ky = (label == "PWR") ? nav_row_center_y - NAV_ROW_H - KEY_GAP_Y : nav_row_center_y;
            translate([dx, ky, -LIP_DEPTH - 1])
                linear_extrude(height = FRAME_H + LIP_DEPTH + 2)
                    if (shape == "round")
                        circle(d = kw + 2*FIT_CLEARANCE);
                    else
                        rounded_rect(kw + 2*FIT_CLEARANCE, kh + 2*FIT_CLEARANCE, KEY_R);
        }

        // חלונות מקלדת מספרית
        for (row = [0:3], col = [0:2]) {
            kx = (col - 1) * (KEY_W + KEY_GAP_X);
            ky = numpad_center_y + (1.5 - row) * (KEY_H + KEY_GAP_Y);
            translate([kx, ky, -LIP_DEPTH - 1])
                linear_extrude(height = FRAME_H + LIP_DEPTH + 2)
                    rounded_rect(KEY_W + 2*FIT_CLEARANCE, KEY_H + 2*FIT_CLEARANCE, KEY_R);
        }
    }
}

// =====================================================================
// 8. כפתור בודד (keycap) - "פלנג'" עליון שנשען על מסגרת המקלדת + פין
//    תחתון קצר שמעביר לחיצה לכפתור המקורי (הכיפה/גומי) של הטלפון עצמו
// =====================================================================
module keycap(w, h, label, shape = "rect", plunger_h = 2.4, flange_h = 1.0, nub_h = 1.0) {
    flange_margin = 1.6; // כמה הפלנג' רחב יותר מהפתח, כדי לשבת על השפה

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

    all_keys = concat(
        [for (k = NAV_ROW) [k[0], k[2], k[3], k[4]]],
        [for (row = [0:3], col = [0:2]) [NUMPAD_ROWS[row][col], KEY_W, KEY_H, "rect"]]
    );

    for (i = [0:len(all_keys)-1]) {
        k = all_keys[i];
        cx = (i % cols) * (KEY_W + spacing) * 1.4;
        cy = -floor(i / cols) * (KEY_H + spacing) * 1.6;
        translate([cx, cy, 0])
            keycap(k[1], k[2], k[0], k[3]);
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
}
