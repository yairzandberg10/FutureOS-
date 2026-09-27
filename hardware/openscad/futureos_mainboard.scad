// =====================================================================
//  לוח אם ל-FutureOS Mini / Regular / Pro - מודל מכני פרמטרי
//  מפרט מלא: hardware/MAINBOARD_SPEC.md
//
//  ארכיטקטורה (כמו בטלפונים דקים): שני לוחות ו-FPC ביניהם.
//    MB  - לוח ראשי למעלה, מאחורי החלק העליון של המסך: SoC + זיכרון, PMIC,
//          RF, Wi-Fi/BT/GNSS, חיישנים, מגש SIM, מצלמות, רמקול שיחה, IR.
//    KB  - לוח מקלדת למטה: כיפות המקשים מלפנים, ומאחור USB-C, 3.5 מ"מ,
//          רמקול, מיקרופון ומנוע רטט.
//    הסוללה בין שני הלוחות, מאחורי המקלדת והחלק התחתון של המסך.
//
//  מה זה כן: קווי מתאר של הלוחות, מיקום ונפח של כל רכיב גדול, חורי הברגה,
//  פדים של כיפות המקשים, אזורי אנטנה, ופתחים שהגוף צריך. בסיס ל-RFQ ול-ODM.
//  מה זה לא: שרטוט חשמלי או layout. לוח עם SoC של MediaTek/Unisoc מתכננים
//  רק מתוך ה-reference design של היצרן (תחת NDA) - זו עבודת ה-ODM.
//
//  המודל משתמש במידות הגוף מ-futureos_family.scad (לא משנה אותו).
//  MB_PART: stack (הכול בתוך הגוף), boards (הלוחות בלבד), mb, kb,
//           outline_mb / outline_kb (דו-ממדי, לייצוא DXF ל-KiCad Edge.Cuts)
//  MODEL: mini / regular / pro (כמו בקובץ המשפחה)
// =====================================================================

include <futureos_family.scad>
PART = "none";       // משתיק את הרינדור של קובץ המשפחה
MB_PART = "stack";   // [stack, boards, mb, kb, outline_mb, outline_kb]
SHOW_BACK = false;   // stack: הגב מוסתר כדי לראות את הלוחות

// ---------------------------------------------------------------------
// מערכת צירים: כמו ה-assembly של המשפחה. X לרוחב, Y מלמטה למעלה,
// Z=0 פני החזית והגוף לכיוון Z שלילי.
// ---------------------------------------------------------------------
IN_X0 = WALL + 0.2;
IN_X1 = BODY_W - WALL - 0.2;
IN_Y0 = WALL + 0.2;
IN_Y1 = BODY_L - WALL - 0.2;
Z_BACK_IN = -BODY_T + WALL;               // פני הדופן האחורית מבפנים

Z_SCR_BACK = -(LCD_SINK + LCD_T);         // גב מודול המסך
// מקובץ המשפחה (מקשי הצד נגזרים ממנו), ולא גבוה יותר ממה ש-KB מאפשר (Mini: נמוך ב-0.05)
Z_BATT_TOP = min(BATT_TOP_Z, -(FRONT_T + FLANGE_T + 0.4) - 0.6 - 0.6);
Z_BATT_BOT = Z_BATT_TOP - BATT[2];

PCB_T = 0.8;                              // 8-10 שכבות HDI, 0.8 מ"מ
SHIELD_H = 1.2;                           // מגני RF / פח מעל SoC

// --- KB: לוח מקלדת ---
// מקש -> ממברנת TPU עם בליטה (actuator) -> כיפת מתכת 0.3 -> פד זהב על KB
// כיפה בגובה 0.3 -> הקודקוד 0.1 מתחת לשפת הממברנה (FRONT_T + FLANGE_T)
KB_T   = 0.6;                              // 4 שכבות
KB_TOP = -(FRONT_T + FLANGE_T + 0.4);
KB_BOT = KB_TOP - KB_T;
STRIP_L = 16;                              // פס תחתון מאחורי KB: USB-C, 3.5 מ"מ, רמקול, מיקרופון, רטט
KB_Y0 = IN_Y0;
KB_Y1 = Y_SCR - 0.4;                       // מעל זה מתחיל מודול המסך באותו גובה Z
KB_X0 = IN_X0;
KB_X1 = IN_X1;

// --- סוללה: מאחורי KB ומאחורי החלק התחתון של המסך ---
BATT_Y0 = IN_Y0 + STRIP_L + 0.3;
BATT_Y1 = BATT_Y0 + BATT[1];
BATT_X0 = CX - BATT[0] / 2;

// --- MB: לוח ראשי ---
MB_TOP = Z_SCR_BACK - 0.3;
MB_BOT = MB_TOP - PCB_T;
MB_Y0 = BATT_Y1 + 0.8;
MB_Y1 = IN_Y1;
MB_X0 = IN_X0;
MB_X1 = BODY_W - WALL - VOL_IN - 0.3;      // מפנה את ה-FPC של מקשי הווליום בדופן הימנית
MB_W = MB_X1 - MB_X0;
MB_L = MB_Y1 - MB_Y0;

// ---------------------------------------------------------------------
// רכיבים לפי דגם: [mini, regular, pro]
// ---------------------------------------------------------------------
HAS_FRONT_CAM = MODEL != "mini";
HAS_IR        = MODEL != "mini";
HAS_BARO      = MODEL != "mini";
HAS_NFC       = MODEL == "pro";

RCAM     = sel([[6.5, 6.5, 3.8], [8.5, 8.5, 4.5], [8.5, 8.5, 4.6]]); // מצלמה אחורית - מודולים דקים (גוף של 10 מ"מ)
FCAM     = sel([[0, 0, 0], [4.5, 4.5, 2.9], [5.5, 5.5, 3.3]]);       // מצלמה קדמית (punch-hole)
SPK      = sel([[12, 10, 3.0], [15, 11, 3.5], [18, 13, 3.5]]);      // רמקול (box)
TRAY     = [11.8, 27, 2.9];                      // מגש 3 כרטיסים מוערם: 2 nano-SIM + microSD
RCV      = [12, 6, 2.2];                         // רמקול שיחה
JACK     = [6.0, 14.5, 4.5];                     // 3.5 מ"מ, פרופיל נמוך
USBC     = [8.94, 7.35, 3.2];                    // USB-C mid-mount 16 pin
MOTOR_D  = 8;                                    // מנוע רטט coin/LRA 0820
MOTOR_H  = 2.5;
DOME_D   = sel([5, 5, 6]);
SCREW_D  = 1.6;                                  // M1.4

// מיקומים (מרכזי רכיבים, XY). צד: "back" = בצד האחורי של הלוח
CAM_XY   = [BODY_W - 11, BODY_L - 12];           // אותו מקום כמו החור בגב (קובץ המשפחה)
FLASH_XY = [BODY_W - 11, BODY_L - 22];
PUNCH_XY = [CX, BODY_L - TOP_BAND - LCD_SIDE - PUNCH_D];
ALS_XY   = [PUNCH_XY[0] + (HAS_FRONT_CAM ? FCAM[0] / 2 + 1.8 : 0), PUNCH_XY[1]]; // "גלולה" אחת במסך עם המצלמה

// ---------------------------------------------------------------------
// בדיקות - נכשלות אם משהו לא נכנס
// ---------------------------------------------------------------------
KB_UNDER_CLEAR   = KB_BOT - Z_BATT_TOP;          // מקום לרכיבים פסיביים בגב KB מעל הסוללה
RCAM_TOP = MB_TOP + 0.2;                         // sink mount: המודול עובר דרך הלוח, 0.1 מתחת לגב המסך
RCAM_BACK_CLEAR  = (RCAM_TOP - RCAM[2]) - Z_BACK_IN; // העדשה צריכה להיות קרובה לחלון שבגב
TRAY_BACK_CLEAR  = (MB_BOT - TRAY[2]) - Z_BACK_IN;
FPC_GAP          = Z_SCR_BACK - Z_BATT_TOP;      // מעבר ה-FPC של המסך והראשי בין המסך לסוללה
STRIP_BACK_CLEAR = (KB_BOT - JACK[2]) - Z_BACK_IN;

echo(str("MAINBOARD ", MODEL,
    ":  MB = ", MB_W, " x ", MB_L, " mm (", round(MB_W * MB_L), " mm2)",
    "  KB = ", KB_X1 - KB_X0, " x ", KB_Y1 - KB_Y0,
    "  battery y = ", BATT_Y0, " .. ", BATT_Y1));
echo(str("clearances:  KB->battery ", KB_UNDER_CLEAR,
    "  rear cam->back ", RCAM_BACK_CLEAR,
    "  SIM tray->back ", TRAY_BACK_CLEAR,
    "  jack->back ", STRIP_BACK_CLEAR,
    "  FPC gap screen/battery ", FPC_GAP));
echo(str("shell openings:  USB-C center z = ", KB_BOT - USBC[2] / 2,
    " (family back shell hole center z = ", -(BODY_T + FRONT_T) / 2, ")"));

assert(KB_UNDER_CLEAR >= 0.6, "אין מקום לפסיביים (0201) בין KB לסוללה");
assert(RCAM_BACK_CLEAR >= 0.1, "המצלמה האחורית עוברת את הדופן האחורית");
assert(TRAY_BACK_CLEAR >= 0.3, "מגש ה-SIM עובר את הדופן האחורית");
assert(STRIP_BACK_CLEAR >= 0.3, "שקע 3.5 מ\"מ עובר את הדופן האחורית");
assert(FPC_GAP >= 0.5, "אין מעבר ל-FPC בין המסך לסוללה");
assert(MB_L >= 36, "הלוח הראשי קצר מדי - להקטין את הסוללה");
assert(BATT_X0 >= IN_X0, "הסוללה רחבה מהגוף");
assert(Z_BATT_BOT - Z_BACK_IN >= 0.2, "הסוללה נכנסת בדופן האחורית");

// ---------------------------------------------------------------------
// עזרים
// ---------------------------------------------------------------------
// תיבה ממורכזת ב-XY, מ-z0 כלפי מטה בגובה h
module part_down(xy, s, z0, h) {
    translate([xy[0] - s[0] / 2, xy[1] - s[1] / 2, z0 - h]) cube([s[0], s[1], h]);
}

module label(t, xy, z, size = 1.6) {
    color("White") translate([xy[0], xy[1], z]) linear_extrude(0.05)
        text(t, size = size, font = "Arial:style=Bold", halign = "center", valign = "center");
}

// ---------------------------------------------------------------------
// MB - לוח ראשי
// ---------------------------------------------------------------------
// פריסה בתוך MB, בקואורדינטות יחסיות ללוח (x מהקצה השמאלי, y מהקצה התחתון - צד הסוללה).
// כל רכיב: [שם, x0, y0, רוחב, אורך, גובה], כולם בצד האחורי של הלוח (הצד הקדמי צמוד למסך).
// שורה תחתונה: מחברי B2B, לשם מגיעים ה-FPC. אחריה מגש SIM + PMIC + Wi-Fi, באמצע SoC + RF,
// ולמעלה רמקול שיחה, חיישנים ו-IR. המצלמות קבועות מהגוף, והפריסה נבנתה סביבן.
//   SOC  = SoC + זיכרון תחת מגן אחד    RF = transceiver + PA + FEM + מסננים
//   WIFI = Wi-Fi/BT/GNSS combo (נשאר גם בוריאנט בלי Wi-Fi - ר' המפרט)   TRAY = 2 nano-SIM + microSD מוערם
//   CAMR / CAMF = מחברי B2B של FPC המצלמות
MB_LAYOUT = sel([
  [ // mini
    ["LCD",  1.0,  0.6,  9, 3.2, 0.9], ["MAIN", 11.5, 0.6, 8, 3.2, 0.9], ["BATT", 21.5, 0.6, 6, 3.2, 0.9],
    ["TRAY", -0.5, 4.5, TRAY[1], TRAY[0], TRAY[2]],
    ["PMIC", 27.5, 4.5, 10, 9, SHIELD_H],
    ["SOC",  0.5, 17.3, 20, 14, SHIELD_H],
    ["RF",   21.5, 16.6, 16, 7.4, SHIELD_H],
    ["WIFI", 7.5, 31.8, 8, 6, 1.0],
    ["RCV",  17.5, 32.0, 12, 6, RCV[2]],
    ["IMU",  1.0, 32.0, 2.5, 3.0, 0.9], ["MAG", 1.0, 35.8, 1.6, 1.6, 0.6],
    ["CAMR", 22.0, 25.5, 5, 3.2, 0.9]
  ],
  [ // regular
    ["LCD",  2.0,  0.6,  9, 3.2, 0.9], ["MAIN", 13.0, 0.6, 8, 3.2, 0.9], ["BATT", 22.0, 0.6, 6, 3.2, 0.9],
    ["TRAY", -0.5, 4.5, TRAY[1], TRAY[0], TRAY[2]],
    ["PMIC", 28.0, 4.5, 12, 10, SHIELD_H],
    ["WIFI", 40.8, 4.5, 7, 8, 1.0],
    ["SOC",  0.5, 17.3, 24, 16, SHIELD_H],
    ["RF",   27.0, 14.9, 20, 12, SHIELD_H],
    ["RCV",  9.0, 35.0, 12, 6, RCV[2]],
    ["IMU",  1.0, 34.3, 2.5, 3.0, 0.9], ["MAG", 1.0, 39.6, 1.6, 1.6, 0.6],
    ["BARO", 5.0, 39.6, 2.0, 2.0, 0.8], ["IR", 36.5, 39.3, 3.0, 2.5, 1.8],
    ["CAMR", 29.5, 28.8, 6, 3.2, 0.9], ["CAMF", 29.5, 33.0, 5, 2.5, 0.9]
  ],
  [ // pro
    ["LCD",  2.0,  0.6,  9, 3.2, 0.9], ["MAIN", 13.0, 0.6, 8, 3.2, 0.9], ["BATT", 38.0, 0.6, 6, 3.2, 0.9],
    ["TRAY", -0.5, 4.5, TRAY[1], TRAY[0], TRAY[2]],
    ["PMIC", 28.0, 4.5, 13, 11, SHIELD_H],
    ["WIFI", 42.5, 4.5, 8, 8, 1.0], ["NFC", 52.0, 4.5, 3.0, 3.0, 0.6],
    ["SOC",  0.5, 17.3, 26, 17, SHIELD_H],
    ["RF",   28.0, 17.0, 22, 13, SHIELD_H],
    ["RCV",  22.0, 52.0, 12, 6, RCV[2]],
    ["IMU",  1.0, 52.0, 2.5, 3.0, 0.9], ["MAG", 1.0, 57.0, 1.6, 1.6, 0.6],
    ["BARO", 5.0, 57.0, 2.0, 2.0, 0.8], ["IR", 10.0, 57.5, 3.0, 2.5, 1.8],
    ["CAMR", 57.0, 46.0, 6, 3.2, 0.9], ["CAMF", 46.0, 46.0, 5, 2.5, 0.9]
  ]
]);

function mb_part(n) = [for (p = MB_LAYOUT) if (p[0] == n) p][0];
function mb_center(n) = let (p = mb_part(n)) [MB_X0 + p[1] + p[3] / 2, MB_Y0 + p[2] + p[4] / 2];
TRAY_XY = mb_center("TRAY");

// ברגים (M1.4): בשורת המחברים ובפינות העליונות
MB_SCREWS = sel([
    [[MB_X0 + 30.5, MB_Y0 + 2.2], [MB_X0 + 37.0, MB_Y0 + 36.0]],
    [[MB_X0 + 32.0, MB_Y0 + 2.2], [MB_X0 + 46.8, MB_Y0 + 40.0], [MB_X0 + 33.5, MB_Y0 + 40.2]],
    [[MB_X0 + 30.0, MB_Y0 + 2.2], [MB_X0 + 60.0, MB_Y0 + 2.2], [MB_X0 + 78.0, MB_Y0 + 58.5]]
]);

// בדיקת התנגשויות בתוך MB: כל זוג רכיבים, מול שתי המצלמות, יציאה מהלוח, וברגים
function rect_of(p) = [MB_X0 + p[1], MB_Y0 + p[2], MB_X0 + p[1] + p[3], MB_Y0 + p[2] + p[4]];
function overlap(a, b) = a[0] < b[2] && b[0] < a[2] && a[1] < b[3] && b[1] < a[3];
function grow(r, g) = [r[0] - g, r[1] - g, r[2] + g, r[3] + g];
function pt(s) = [s[0], s[1], s[0], s[1]];
CAM_RECT  = [CAM_XY[0] - RCAM[0] / 2, CAM_XY[1] - RCAM[1] / 2, CAM_XY[0] + RCAM[0] / 2, CAM_XY[1] + RCAM[1] / 2];
FCAM_RECT = [PUNCH_XY[0] - FCAM[0] / 2, PUNCH_XY[1] - FCAM[1] / 2, PUNCH_XY[0] + FCAM[0] / 2, PUNCH_XY[1] + FCAM[1] / 2];
MB_CLASHES = concat(
    [for (i = [0 : len(MB_LAYOUT) - 1], j = [0 : len(MB_LAYOUT) - 1])
        if (i < j && overlap(grow(rect_of(MB_LAYOUT[i]), 0.2), rect_of(MB_LAYOUT[j])))
            str(MB_LAYOUT[i][0], "/", MB_LAYOUT[j][0])],
    [for (p = MB_LAYOUT) if (overlap(grow(rect_of(p), 0.3), CAM_RECT)) str(p[0], "/rear cam")],
    [for (p = MB_LAYOUT) if (HAS_FRONT_CAM && overlap(grow(rect_of(p), 0.3), FCAM_RECT)) str(p[0], "/front cam")],
    [for (p = MB_LAYOUT) if (p[0] != "TRAY" && (rect_of(p)[0] < MB_X0 || rect_of(p)[2] > MB_X1 || rect_of(p)[3] > MB_Y1)) str(p[0], " off board")],
    [for (s = MB_SCREWS, p = MB_LAYOUT) if (overlap(grow(rect_of(p), 1.8), pt(s))) str("screw/", p[0])],
    [for (s = MB_SCREWS) if (overlap(grow(CAM_RECT, 1.8), pt(s)) || (HAS_FRONT_CAM && overlap(grow(FCAM_RECT, 1.8), pt(s)))) "screw/camera"],
    [for (s = MB_SCREWS) if (s[0] < MB_X0 + 1.4 || s[0] > MB_X1 - 1.4 || s[1] > MB_Y1 - 1.4) "screw off board"]
);
echo(str("MB clashes: ", len(MB_CLASHES) == 0 ? "none" : MB_CLASHES));
assert(len(MB_CLASHES) == 0, "רכיבים חופפים בלוח הראשי - ר' MB clashes");

PART_COLOR = [["LCD", "Black"], ["MAIN", "Black"], ["BATT", "Black"], ["TRAY", "DarkGray"],
              ["PMIC", "Silver"], ["WIFI", "Silver"], ["SOC", "Silver"], ["RF", "Silver"],
              ["RCV", "DimGray"], ["IMU", "Teal"], ["MAG", "Teal"], ["BARO", "Teal"],
              ["IR", "DarkRed"], ["NFC", "Teal"], ["CAMR", "Black"], ["CAMF", "Black"]];
function part_color(n) = [for (c = PART_COLOR) if (c[0] == n) c[1]][0];

module mb_pcb() {
    difference() {
        translate([MB_X0, MB_Y0, MB_BOT]) rbox(MB_W, MB_L, PCB_T, 1.5);
        // מצלמה קדמית עוברת דרך הלוח
        if (HAS_FRONT_CAM)
            translate([PUNCH_XY[0] - FCAM[0] / 2 - 0.3, PUNCH_XY[1] - FCAM[1] / 2 - 0.3, MB_BOT - 0.01])
                cube([FCAM[0] + 0.6, FCAM[1] + 0.6, PCB_T + 0.02]);
        // המצלמה האחורית: חור בלוח כדי שהמודול לא יוסיף עובי (sink mount)
        translate([CAM_XY[0] - RCAM[0] / 2 - 0.3, CAM_XY[1] - RCAM[1] / 2 - 0.3, MB_BOT - 0.01])
            cube([RCAM[0] + 0.6, RCAM[1] + 0.6, PCB_T + 0.02]);
        for (s = MB_SCREWS) translate([s[0], s[1], MB_BOT - 0.01]) cylinder(d = SCREW_D, h = PCB_T + 0.02);
    }
}

module mb_parts() {
    for (p = MB_LAYOUT) color(part_color(p[0]))
        translate([MB_X0 + p[1], MB_Y0 + p[2], MB_BOT - p[5]]) cube([p[3], p[4], p[5]]);
    // מצלמה אחורית (sink - עוברת דרך הלוח) + פלאש על FPC צמוד לגב
    color("#333") translate([CAM_XY[0] - RCAM[0] / 2, CAM_XY[1] - RCAM[1] / 2, RCAM_TOP - RCAM[2]]) cube([RCAM[0], RCAM[1], RCAM[2]]);
    color("#111") translate([CAM_XY[0], CAM_XY[1], RCAM_TOP - RCAM[2] - 0.05]) cylinder(d = RCAM[0] * 0.6, h = 0.05);
    color("Yellow") translate([FLASH_XY[0] - 1.5, FLASH_XY[1] - 1.5, Z_BACK_IN]) cube([3, 3, 0.8]);
    // מצלמה קדמית: עדשה מתחת לזכוכית, גוף עובר דרך המסך והלוח
    if (HAS_FRONT_CAM)
        color("#333") translate([PUNCH_XY[0] - FCAM[0] / 2, PUNCH_XY[1] - FCAM[1] / 2, -0.6 - FCAM[2]]) cube(FCAM);
    // ALS + קרבה מתחת לחור במסך, על FPC משלו
    color("Purple") translate([ALS_XY[0] - 1, ALS_XY[1] - 1, -0.6 - 1.0]) cube([2, 2, 1.0]);
}

// ---------------------------------------------------------------------
// KB - לוח מקלדת
// ---------------------------------------------------------------------

USB_XY   = [CX, KB_Y0 + USBC[1] / 2 - 0.4];
MOTOR_XY = [CX, KB_Y0 + USBC[1] + 0.5 + MOTOR_D / 2];
JACK_XY  = [KB_X0 + 1 + JACK[0] / 2, KB_Y0 + JACK[1] / 2 - 0.4];
SPK_XY   = [KB_X1 - 0.5 - SPK[0] / 2, KB_Y0 + 0.5 + SPK[1] / 2];
MIC_XY   = [CX + USBC[0] / 2 + 2.5, KB_Y0 + 2];
KB_B2B   = [mb_center("MAIN")[0], KB_Y1 - 2.5];
// ברגים רק בפס התחתון - מעליו הסוללה. הקצה העליון של KB נתפס מתחת לצלע בחזית
KB_SCREWS = [[(JACK_XY[0] + JACK[0] / 2 + USB_XY[0] - USBC[0] / 2) / 2, KB_Y0 + 13],
             [(USB_XY[0] + USBC[0] / 2 + SPK_XY[0] - SPK[0] / 2) / 2, KB_Y0 + 13]];

// פדי כיפות: D-pad = 4 כיפות בכיוונים + OK במרכז, כל השאר כיפה אחת במרכז המקש
function dome_points() = concat(
    [for (k = FRONT_KEYS) if (k[5] != "dpad") [k[0], k[1]]],
    [for (k = FRONT_KEYS) if (k[5] == "dpad") for (a = [0, 90, 180, 270])
        [k[0] + cos(a) * k[2] * 0.36, k[1] + sin(a) * k[2] * 0.36]]
);

module kb_pcb() {
    difference() {
        translate([KB_X0, KB_Y0, KB_BOT]) rbox(KB_X1 - KB_X0, KB_Y1 - KB_Y0, KB_T, 1.5);
        for (s = KB_SCREWS) translate([s[0], s[1], KB_BOT - 0.01]) cylinder(d = SCREW_D, h = KB_T + 0.02);
    }
}

module kb_parts() {
    // פדי זהב + כיפות מתכת
    for (p = dome_points()) {
        color("Gold") translate([p[0], p[1], KB_TOP]) cylinder(d = DOME_D, h = 0.02);
        color("Silver", 0.7) translate([p[0], p[1], KB_TOP]) scale([1, 1, 0.3 / (DOME_D / 2)]) sphere(d = DOME_D);
    }
    // תאורת מקשים: LED צד בין השורות (סמלי)
    color("White") for (p = dome_points()) translate([p[0] + DOME_D / 2 + 0.6, p[1] - 0.4, KB_TOP]) cube([0.8, 0.8, 0.4]);
    // פס תחתון בגב
    color("Silver")  part_down(USB_XY, USBC, KB_BOT, USBC[2]);
    color("DimGray") part_down(JACK_XY, JACK, KB_BOT, JACK[2]);
    color("#444")    part_down(SPK_XY, SPK, KB_BOT, SPK[2]);
    color("Black")   part_down(MIC_XY, [3.5, 2.65], KB_BOT, 1.0);
    color("Gray")    translate([MOTOR_XY[0], MOTOR_XY[1], KB_BOT - MOTOR_H]) cylinder(d = MOTOR_D, h = MOTOR_H);
    color("Black")   part_down(KB_B2B, [8, 3.2], KB_BOT, 0.9);
}

// ---------------------------------------------------------------------
// סוללה, FPC, אנטנות
// ---------------------------------------------------------------------
module battery() {
    color("Orange", 0.85) translate([BATT_X0, BATT_Y0, Z_BATT_BOT]) cube(BATT);
    // PCM + זנב FPC לכיוון MB
    color("Green") translate([mb_center("BATT")[0] - 6, BATT_Y1 - 3, Z_BATT_BOT]) cube([12, 3, BATT[2]]);
    color("Gold", 0.9) translate([mb_center("BATT")[0] - 3, BATT_Y1 - 1, MB_BOT - 0.95]) cube([6, MB_Y0 - BATT_Y1 + 3, 0.15]);
}

// שני FPC עוברים בין גב המסך לסוללה: המסך (מתקפל בתחתית המסך) והראשי (KB <-> MB)
module fpcs() {
    z = Z_BATT_TOP + 0.1;
    color("Gold", 0.9) {
        translate([mb_center("LCD")[0] - 4.5, Y_SCR - 1, z]) cube([9, MB_Y0 - Y_SCR + 3, 0.12]);          // LCD
        translate([mb_center("MAIN")[0] - 4, KB_Y1 - 4, z]) cube([8, MB_Y0 - KB_Y1 + 7, 0.12]);            // MAIN
    }
}

// אזורי אנטנה: בלי מתכת, בלי נחושת במשטחים. LTE ראשית למטה, DRX/GNSS/Wi-Fi למעלה
ANT_H = 6;
module antenna_zones() {
    color("Cyan", 0.18) {
        translate([IN_X0, IN_Y0, Z_BACK_IN]) cube([IN_X1 - IN_X0, ANT_H, -Z_BACK_IN - FRONT_T]);
        translate([IN_X0, IN_Y1 - ANT_H, Z_BACK_IN]) cube([IN_X1 - IN_X0, ANT_H, -Z_BACK_IN - FRONT_T]);
    }
    if (HAS_NFC) color("Cyan", 0.5)
        translate([CX - 20, BATT_Y0 + BATT[1] / 2 - 15, Z_BACK_IN]) cube([40, 30, 0.3]);    // סליל NFC על גב הסוללה
}

// ---------------------------------------------------------------------
// פתחים שהגוף צריך (בקובץ המשפחה עדיין לא כולם קיימים) - מסומנים באדום
// ---------------------------------------------------------------------
module shell_openings() {
    color("Red", 0.6) {
        // USB-C - במרכז הגובה של המחבר
        translate([USB_XY[0] - 4.5, -0.1, KB_BOT - USBC[2] / 2 - 1.75]) cube([9, WALL + 0.3, 3.5]);
        // 3.5 מ"מ
        translate([JACK_XY[0], -0.1, KB_BOT - JACK[2] / 2]) rotate([-90, 0, 0]) cylinder(d = 3.8, h = WALL + 0.3);
        // מיקרופון
        translate([MIC_XY[0], -0.1, KB_BOT - 0.5]) rotate([-90, 0, 0]) cylinder(d = 0.8, h = WALL + 0.3);
        // רמקול - גריל בדופן התחתונה
        for (i = [-2 : 2]) translate([SPK_XY[0] + i * 2, -0.1, KB_BOT - SPK[2] / 2]) rotate([-90, 0, 0]) cylinder(d = 1.0, h = WALL + 0.3);
        // מגש SIM - בדופן השמאלית
        translate([-0.1, TRAY_XY[1] - TRAY[0] / 2, MB_BOT - TRAY[2]]) cube([WALL + 0.3, TRAY[0], TRAY[2]]);
        // IR - בדופן העליונה
        if (HAS_IR) translate([mb_center("IR")[0], BODY_L - WALL - 0.2, MB_BOT - 0.9]) rotate([-90, 0, 0]) cylinder(d = 2.2, h = WALL + 0.3);
        // ALS/קרבה: ליד המצלמה הקדמית (חור "גלולה" אחד במסך), ב-Mini חור קטן לבד
        translate([ALS_XY[0], ALS_XY[1], -0.6]) cylinder(d = 2.2, h = 0.6);
    }
}

// ---------------------------------------------------------------------
// הרכבה
// ---------------------------------------------------------------------
module boards() {
    color("DarkGreen") mb_pcb();
    mb_parts();
    color("DarkGreen") kb_pcb();
    kb_parts();
    label("MB", [MB_X0 + MB_W / 2, MB_Y0 + MB_L / 2], MB_TOP + 0.01, 3);
    battery();
    fpcs();
}

module stack() {
    // הגוף של המשפחה, בלי ה-keepouts הישנים (שם הלוח והסוללה חופפים)
    color(BODY_COLOR, 0.25) translate([0, 0, -FRONT_T]) front_shell();
    if (SHOW_BACK) color(BODY_COLOR, 0.25) translate([0, 0, -BODY_T]) back_shell();
    color("SteelBlue", 0.5) translate([CX - SCR_OUT_W / 2, Y_SCR, Z_SCR_BACK]) cube([SCR_OUT_W, SCR_OUT_H, LCD_T]);
    translate([0, 0, -FRONT_T]) color("Gold", 0.8)
        translate([BODY_W - WALL - VOL_IN, VOL_Y[1] - VOL_L / 2 - VOL_FLANGE, VOL_Z + FRONT_T - VOL_H / 2 - VOL_FLANGE])
            cube([VOL_FPC_T, VOL_Y[0] - VOL_Y[1] + VOL_L + 2 * VOL_FLANGE, VOL_H + 2 * VOL_FLANGE]);
    boards();
    antenna_zones();
    shell_openings();
}

if (MB_PART == "stack")           stack();
else if (MB_PART == "boards")     boards();
else if (MB_PART == "mb")         { color("DarkGreen") mb_pcb(); mb_parts(); }
else if (MB_PART == "kb")         { color("DarkGreen") kb_pcb(); kb_parts(); }
else if (MB_PART == "outline_mb") projection() mb_pcb();
else if (MB_PART == "outline_kb") projection() kb_pcb();
