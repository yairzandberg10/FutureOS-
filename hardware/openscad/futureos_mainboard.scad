// =====================================================================
//  לוח אם ל-FutureOS Mini / Regular / Pro - מודל מכני פרמטרי
//  מפרט מלא: hardware/MAINBOARD_SPEC.md
//
//  מהדורת אב-טיפוס (10 יחידות): במקום SoC על הלוח, מודול Android מוכן (SoM,
//  Quectel) שמולחם על לוח נושא (carrier) של 4 שכבות. את לוח הנושא אפשר לתכנן
//  ב-KiCad ולהזמין עם הרכבה ב-JLCPCB/PCBWay - בלי reference design תחת NDA.
//
//  ארכיטקטורה: שני לוחות ו-FPC ביניהם.
//    MB  - לוח נושא למעלה, מאחורי החלק העליון של המסך: ה-SoM, מחברי FPC,
//          ובשורה העליונה מצלמות, רמקול שיחה, רטט, חיישנים, IR.
//    KB  - לוח מקלדת למטה: כיפות המקשים מלפנים, ומאחור בפס התחתון USB-C,
//          3.5 מ"מ, שקע nano-SIM (ו-microSD ב-Pro), רמקול ומיקרופון.
//    הסוללה בין שני הלוחות. ה-SIM נגיש בהסרת הגב (אין מגש - חוסך מנגנון ועובי).
//
//  מה זה כן: קווי מתאר של הלוחות, מיקום ונפח של כל רכיב גדול, חורי הברגה,
//  פדים של כיפות המקשים, אזורי אנטנה, ופתחים בגוף.
//  מה זה לא: שרטוט חשמלי. את החיבורים של ה-SoM לוקחים מ-Hardware Design של Quectel.
//
//  המודל משתמש במידות הגוף מ-futureos_family.scad (לא משנה אותו).
//  MB_PART: stack (הכול בתוך הגוף), boards (הלוחות בלבד), mb, kb,
//           outline_mb / outline_kb (דו-ממדי, לייצוא DXF ל-KiCad Edge.Cuts),
//           back_shell (הגב עם כל הפתחים - להדפסה)
//  MODEL: mini / regular / pro (כמו בקובץ המשפחה)
// =====================================================================

include <futureos_family.scad>
PART = "none";       // משתיק את הרינדור של קובץ המשפחה
MB_PART = "stack";   // [stack, boards, mb, kb, outline_mb, outline_kb, back_shell]
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
// מקובץ המשפחה (מקשי הצד נגזרים ממנו), ולא גבוה יותר ממה ש-KB מאפשר.
// בגב KB מעל הסוללה אין רכיבים (הכול בפס התחתון), רק 0.2 מרווח
Z_BATT_TOP = min(BATT_TOP_Z, -(FRONT_T + FLANGE_T + 0.4) - 0.6 - 0.2);
Z_BATT_BOT = Z_BATT_TOP - BATT[2];

PCB_T = 0.8;                              // לוח נושא 4 שכבות (דק יותר מתעקם בהלחמת מודול LCC של 40 מ"מ)

// --- KB: לוח מקלדת ---
// מקש -> ממברנת TPU עם בליטה (actuator) -> כיפת מתכת 0.3 -> פד זהב על KB
// כיפה בגובה 0.3 -> הקודקוד 0.1 מתחת לשפת הממברנה (FRONT_T + FLANGE_T)
KB_T   = 0.6;                              // 4 שכבות
KB_TOP = -(FRONT_T + FLANGE_T + 0.4);
KB_BOT = KB_TOP - KB_T;
STRIP_L = 17;                              // פס תחתון מאחורי KB: USB-C, 3.5 מ"מ, SIM, רמקול, מיקרופון
KB_Y0 = IN_Y0;
KB_Y1 = Y_SCR - 0.4;                       // מעל זה מתחיל מודול המסך באותו גובה Z
KB_X0 = IN_X0;
KB_X1 = IN_X1;

// --- סוללה: מאחורי KB ומאחורי החלק התחתון של המסך ---
BATT_Y0 = IN_Y0 + STRIP_L + 0.3;
BATT_Y1 = BATT_Y0 + BATT[1];
BATT_X0 = CX - BATT[0] / 2;

// --- MB: לוח ראשי ---
MB_TOP = Z_SCR_BACK - 0.15;               // קצף 0.15 בין גב המסך ללוח
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
HAS_JACK      = MODEL != "mini";          // Mini: אודיו דרך USB-C - אין רוחב לשקע בפס התחתון
HAS_SD        = MODEL == "pro";           // Mini/Regular: אחסון פנימי בלבד (אין רוחב בפס)

// SoM: Mini - Quectel SC200E (4 ליבות, 40.5x40.5x2.85)
//      Regular/Pro - Quectel SC680A (8 ליבות, 43x44x2.85)
SOM      = sel([[40.5, 40.5, 2.85], [43, 44, 2.85], [43, 44, 2.85]]);
RCAM     = sel([[6.5, 6.5, 3.5], [8.5, 8.5, 4.4], [8.5, 8.5, 4.6]]); // מצלמה אחורית: Mini 5MP FF, האחרים AF
FCAM     = sel([[0, 0, 0], [4.5, 4.5, 2.9], [5.5, 5.5, 3.3]]);       // מצלמה קדמית (punch-hole)
SPK      = sel([[12, 10, 3.0], [15, 11, 3.0], [18, 13, 3.0]]);      // רמקול (box)
SIM      = [12.5, 14, 1.4];                      // שקע nano-SIM push-push
SD       = [11.5, 14.5, 1.5];                    // שקע microSD push-push
RCV      = [12, 6, 2.2];                         // רמקול שיחה
JACK     = [6.0, 14.5, 4.2];                     // 3.5 מ"מ, mid-mount בפרופיל נמוך (4.2 מעל הלוח)
USBC     = [8.94, 7.35, 3.2];                    // USB-C mid-mount 16 pin
MOTOR    = 7;                                    // מנוע רטט coin בקוטר 7 (בשורה העליונה של MB)
DOME_D   = sel([5, 5, 6]);
SCREW_D  = 1.6;                                  // M1.4

// מיקומים (מרכזי רכיבים, XY). צד: "back" = בצד האחורי של הלוח
// CAM_XY / FLASH_XY - מקובץ המשפחה (שם נחתכים החורים בגב)
PUNCH_XY = [CX, BODY_L - TOP_BAND - LCD_SIDE - PUNCH_D];
ALS_XY   = [PUNCH_XY[0] + (HAS_FRONT_CAM ? FCAM[0] / 2 + 1.8 : 0), PUNCH_XY[1]]; // "גלולה" אחת במסך עם המצלמה

// ---------------------------------------------------------------------
// בדיקות - נכשלות אם משהו לא נכנס
// ---------------------------------------------------------------------
KB_UNDER_CLEAR   = KB_BOT - Z_BATT_TOP;          // בגב KB מעל הסוללה אין רכיבים
RCAM_TOP = Z_SCR_BACK - 0.1;                     // sink mount: המודול עובר דרך חור בלוח, 0.1 מתחת לגב המסך
// העדשה עד החלון בגב (זכוכית 0.5), כולל הטבעת הבולטת CAM_BUMP
RCAM_BACK_CLEAR  = (RCAM_TOP - RCAM[2]) - (-BODY_T - CAM_BUMP + 0.5);
SOM_BACK_CLEAR   = (MB_BOT - SOM[2]) - Z_BACK_IN;
FPC_GAP          = Z_SCR_BACK - Z_BATT_TOP;      // מעבר ה-FPC של המסך והראשי בין המסך לסוללה
STRIP_BACK_CLEAR = (KB_BOT - (HAS_JACK ? JACK[2] : USBC[2])) - Z_BACK_IN;
BATT_MAH = round(BATT[0] * BATT[1] * BATT[2] * 0.15 / 50) * 50; // ~150mAh לסמ"ק (תא מדף / חלק חילוף)

echo(str("MAINBOARD ", MODEL,
    ":  MB = ", MB_W, " x ", MB_L, " mm (", round(MB_W * MB_L), " mm2)",
    "  KB = ", KB_X1 - KB_X0, " x ", KB_Y1 - KB_Y0,
    "  battery = ", BATT[0], " x ", BATT[1], " x ", BATT[2], " (~", BATT_MAH, " mAh)",
    "  y = ", BATT_Y0, " .. ", BATT_Y1));
echo(str("clearances:  KB->battery ", KB_UNDER_CLEAR,
    "  SoM->back ", SOM_BACK_CLEAR,
    "  rear cam->window ", RCAM_BACK_CLEAR,
    "  jack/USB->back ", STRIP_BACK_CLEAR,
    "  FPC gap screen/battery ", FPC_GAP));

assert(KB_UNDER_CLEAR >= 0.2 - 1e-6, "KB נוגע בסוללה");
assert(SOM_BACK_CLEAR >= 0.2 - 1e-6, "ה-SoM עובר את הדופן האחורית");
assert(RCAM_BACK_CLEAR >= 0.1, "המצלמה האחורית עוברת את החלון בגב - להגדיל את CAM_BUMP");
assert(STRIP_BACK_CLEAR >= 0.2 - 1e-6, "שקע האוזניות / USB-C עוברים את הדופן האחורית");
assert(FPC_GAP >= 0.35, "אין מעבר ל-FPC בין המסך לסוללה");
assert(BATT_X0 >= IN_X0, "הסוללה רחבה מהגוף");
assert(Z_BATT_BOT - Z_BACK_IN >= 0.2 - 1e-6, "הסוללה נכנסת בדופן האחורית");

// ---------------------------------------------------------------------
// עזרים
// ---------------------------------------------------------------------
// תיבה ממורכזת ב-XY, מ-z0 כלפי מטה בגובה h
module part_down(xy, s, z0, h) {
    translate([xy[0] - s[0] / 2, xy[1] - s[1] / 2, z0 - h]) cube([s[0], s[1], h]);
}

// back = true: נקרא כשמסתכלים מהגב
module label(t, xy, z, size = 1.6, back = false) {
    color("White") translate([xy[0], xy[1], z]) mirror([back ? 1 : 0, 0, 0]) linear_extrude(0.05)
        text(t, size = size, font = "Arial:style=Bold", halign = "center", valign = "center");
}

// ---------------------------------------------------------------------
// MB - לוח ראשי
// ---------------------------------------------------------------------
// פריסה בתוך MB, בקואורדינטות יחסיות ללוח (x מהקצה השמאלי, y מהקצה התחתון - צד הסוללה).
// כל רכיב: [שם, x0, y0, רוחב, אורך, גובה], כולם בצד האחורי של הלוח (הצד הקדמי צמוד למסך).
// ה-SoM בתחתית הלוח. מחברי FPC לידו (Regular/Pro) או מתחתיו (Mini). בשורה העליונה, מעל
// ה-SoM: מצלמות, רמקול שיחה, רטט, חיישנים ו-IR. המצלמות קבועות מהגוף, והפריסה נבנתה סביבן.
// המגנטומטר רחוק מהמגנטים (רטט, רמקול שיחה, רמקול).
//   LCD/MAIN/BATT = מחברי B2B ל-FPC   CAMR / CAMF = מחברי FPC של המצלמות
MB_LAYOUT = sel([
  [ // mini
    ["LCD",  1.0,  0.6,  9, 3.2, 0.9], ["MAIN", 11.5, 0.6, 8, 3.2, 0.9], ["BATT", 21.5, 0.6, 6, 3.2, 0.9],
    ["SOM",  0.5,  4.3, SOM[0], SOM[1], SOM[2]],
    ["MOTOR", 3.6, 46.1, MOTOR, MOTOR, 2.0],
    ["IMU",  11.0, 46.0, 2.5, 3.0, 0.9],
    ["RCV",  14.0, 46.8, RCV[0], RCV[1], RCV[2]],
    ["CAMR", 26.4, 49.0, 5, 3.2, 0.9],
    ["MAG",  40.2, 47.0, 1.6, 1.6, 0.6]
  ],
  [ // regular
    ["SOM",  0.5,  0.6, SOM[0], SOM[1], SOM[2]],
    ["LCD",  45.0, 0.6, 3.2, 9, 0.9], ["MAIN", 45.0, 10.0, 3.2, 8, 0.9], ["BATT", 45.0, 18.8, 3.2, 6, 0.9],
    ["CAMR", 45.0, 29.3, 3.2, 6, 0.9],
    ["MAG",  45.0, 35.8, 1.6, 1.6, 0.6], ["IMU", 45.0, 38.0, 2.5, 3.0, 0.9], ["BARO", 45.0, 41.5, 2.0, 2.0, 0.8],
    ["MOTOR", 0.8, 46.4, MOTOR, MOTOR, 2.0],
    ["RCV",  8.4,  46.9, RCV[0], RCV[1], RCV[2]],
    ["CAMF", 28.2, 46.0, 5, 2.5, 0.9],
    ["IR",   28.2, 51.5, 3.0, 2.5, 1.8]
  ],
  [ // pro
    ["SOM",  0.5,  0.6, SOM[0], SOM[1], SOM[2]],
    ["LCD",  45.0, 0.6, 9, 3.2, 0.9], ["MAIN", 55.0, 0.6, 8, 3.2, 0.9], ["BATT", 64.0, 0.6, 6, 3.2, 0.9],
    ["NFC",  71.0, 0.6, 3.0, 3.0, 0.6],
    ["MOTOR", 45.0, 6.0, MOTOR, MOTOR, 2.0],
    ["IMU",  45.0, 15.0, 2.5, 3.0, 0.9], ["BARO", 49.0, 15.0, 2.0, 2.0, 0.8],
    ["MAG",  77.0, 20.0, 1.6, 1.6, 0.6],
    ["CAMF", 45.0, 41.0, 5, 2.5, 0.9], ["CAMR", 66.0, 40.0, 6, 3.2, 0.9],
    ["RCV",  25.0, 47.0, RCV[0], RCV[1], RCV[2]],
    ["IR",   50.0, 51.5, 3.0, 2.5, 1.8]
  ]
]);

function mb_part(n) = [for (p = MB_LAYOUT) if (p[0] == n) p][0];
function mb_center(n) = let (p = mb_part(n)) [MB_X0 + p[1] + p[3] / 2, MB_Y0 + p[2] + p[4] / 2];

// ברגים (M1.4), ביחס ללוח
MB_SCREWS = [for (s = sel([
    [[30.5, 2.2], [1.6, 49.5]],
    [[46.6, 27.5], [35.5, 50.0]],
    [[2.5, 50.0], [60.0, 50.0], [79.0, 3.0], [79.0, 38.0]]
])) [MB_X0 + s[0], MB_Y0 + s[1]]];

// בדיקת התנגשויות בתוך MB: כל זוג רכיבים, מול שתי המצלמות, יציאה מהלוח, וברגים
function rect_of(p) = [MB_X0 + p[1], MB_Y0 + p[2], MB_X0 + p[1] + p[3], MB_Y0 + p[2] + p[4]];
function overlap(a, b) = a[0] < b[2] && b[0] < a[2] && a[1] < b[3] && b[1] < a[3];
function grow(r, g) = [r[0] - g, r[1] - g, r[2] + g, r[3] + g];
function pt(s) = [s[0], s[1], s[0], s[1]];
CAM_RECT  = [CAM_XY[0] - RCAM[0] / 2, CAM_XY[1] - RCAM[1] / 2, CAM_XY[0] + RCAM[0] / 2, CAM_XY[1] + RCAM[1] / 2];
FLASH_RECT = [FLASH_XY[0] - 1.5, FLASH_XY[1] - 1.5, FLASH_XY[0] + 1.5, FLASH_XY[1] + 1.5];
FCAM_RECT = [PUNCH_XY[0] - FCAM[0] / 2, PUNCH_XY[1] - FCAM[1] / 2, PUNCH_XY[0] + FCAM[0] / 2, PUNCH_XY[1] + FCAM[1] / 2];
MB_CLASHES = concat(
    [for (i = [0 : len(MB_LAYOUT) - 1], j = [0 : len(MB_LAYOUT) - 1])
        if (i < j && overlap(grow(rect_of(MB_LAYOUT[i]), 0.2), rect_of(MB_LAYOUT[j])))
            str(MB_LAYOUT[i][0], "/", MB_LAYOUT[j][0])],
    [for (p = MB_LAYOUT) if (overlap(grow(rect_of(p), 0.3), CAM_RECT)) str(p[0], "/rear cam")],
    [for (p = MB_LAYOUT) if (HAS_FRONT_CAM && overlap(grow(rect_of(p), 0.3), FCAM_RECT)) str(p[0], "/front cam")],
    [for (p = MB_LAYOUT) if (rect_of(p)[0] < MB_X0 || rect_of(p)[1] < MB_Y0 || rect_of(p)[2] > MB_X1 || rect_of(p)[3] > MB_Y1) str(p[0], " off board")],
    // הפלאש צמוד לגב: רכיב גבוה מתחתיו לא נכנס
    [for (p = MB_LAYOUT) if (MB_BOT - p[5] < Z_BACK_IN + 1.0 && overlap(grow(rect_of(p), 0.3), FLASH_RECT)) str(p[0], "/flash")],
    [for (s = MB_SCREWS, p = MB_LAYOUT) if (overlap(grow(rect_of(p), 1.8), pt(s))) str("screw/", p[0])],
    [for (s = MB_SCREWS) if (overlap(grow(CAM_RECT, 1.8), pt(s)) || (HAS_FRONT_CAM && overlap(grow(FCAM_RECT, 1.8), pt(s)))) "screw/camera"],
    [for (s = MB_SCREWS) if (s[0] < MB_X0 + 1.4 || s[0] > MB_X1 - 1.4 || s[1] < MB_Y0 + 1.4 || s[1] > MB_Y1 - 1.4) "screw off board"]
);
echo(str("MB clashes: ", len(MB_CLASHES) == 0 ? "none" : MB_CLASHES));
assert(len(MB_CLASHES) == 0, "רכיבים חופפים בלוח הראשי - ר' MB clashes");

PART_COLOR = [["LCD", "Black"], ["MAIN", "Black"], ["BATT", "Black"], ["SOM", "Silver"], ["MOTOR", "Gray"],
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
        if (p[0] == "MOTOR") translate([MB_X0 + p[1] + p[3] / 2, MB_Y0 + p[2] + p[4] / 2, MB_BOT - p[5]]) cylinder(d = p[3], h = p[5]);
        else translate([MB_X0 + p[1], MB_Y0 + p[2], MB_BOT - p[5]]) cube([p[3], p[4], p[5]]);
    label(MODEL == "mini" ? "SC200E" : "SC680A", mb_center("SOM"), MB_BOT - SOM[2] - 0.06, 3, back = true);
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

// פס תחתון בגב KB: [שם, מרכז x, מרכז y, רוחב, אורך, גובה] במערכת הגוף.
// USB-C ו-3.5 מ"מ בולטים 0.4 לתוך הדופן התחתונה, כדי שיגיעו לפתח.
USB_XY   = [CX, KB_Y0 + USBC[1] / 2 - 0.4];
JACK_XY  = [KB_X0 + 1 + JACK[0] / 2, KB_Y0 + JACK[1] / 2 - 0.4];
SPK_XY   = [KB_X1 - 0.5 - SPK[0] / 2, KB_Y0 + 0.5 + SPK[1] / 2];
MIC_XY   = [CX + USBC[0] / 2 + 2.25, KB_Y0 + 2];
// SIM: Mini - בקצה השמאלי; Regular - בין השקע ל-USB; Pro - אחרי השקע, microSD מימין ל-USB
SIM_X0   = sel([KB_X0 + 1.0, JACK_XY[0] + JACK[0] / 2 + 0.7, JACK_XY[0] + JACK[0] / 2 + 3.8]);
SIM_XY   = [SIM_X0 + SIM[0] / 2, KB_Y0 + 0.5 + SIM[1] / 2];
SD_XY    = [MIC_XY[0] + 1.75 + 1.4 + SD[0] / 2, KB_Y0 + 0.5 + SD[1] / 2];
KB_STRIP = concat(
    [["USB", USB_XY, USBC], ["SPK", SPK_XY, SPK], ["MIC", MIC_XY, [3.5, 2.65, 1.0]], ["SIM", SIM_XY, SIM]],
    HAS_JACK ? [["JACK", JACK_XY, JACK]] : [],
    HAS_SD ? [["SD", SD_XY, SD]] : []);
// ה-FPC הראשי מולחם ל-KB (hot-bar), לא מחבר: מעל הסוללה אין גובה למחבר B2B
KB_B2B   = [mb_center("MAIN")[0], KB_Y1 - 2.5];
// ברגים רק בפס התחתון - מעליו הסוללה. הקצה העליון של KB נתפס מתחת לצלע בחזית
KB_SCREWS = sel([
    [[(SIM_XY[0] + SIM[0] / 2 + USB_XY[0] - USBC[0] / 2) / 2, KB_Y0 + 9], [MIC_XY[0], KB_Y0 + 10]],
    [[(USB_XY[0] + USBC[0] / 2 + SPK_XY[0] - SPK[0] / 2) / 2, KB_Y0 + 10]],
    [[(JACK_XY[0] + JACK[0] / 2 + SIM_X0) / 2, KB_Y0 + 9], [(SIM_X0 + SIM[0] + USB_XY[0] - USBC[0] / 2) / 2, KB_Y0 + 9]]
]);
function strip_rect(p) = [p[1][0] - p[2][0] / 2, p[1][1] - p[2][1] / 2, p[1][0] + p[2][0] / 2, p[1][1] + p[2][1] / 2];
KB_CLASHES = concat(
    [for (i = [0 : len(KB_STRIP) - 1], j = [0 : len(KB_STRIP) - 1])
        if (i < j && overlap(grow(strip_rect(KB_STRIP[i]), 0.2), strip_rect(KB_STRIP[j])))
            str(KB_STRIP[i][0], "/", KB_STRIP[j][0])],
    [for (p = KB_STRIP) if (strip_rect(p)[0] < KB_X0 || strip_rect(p)[2] > KB_X1 || strip_rect(p)[3] > KB_Y0 + STRIP_L) str(p[0], " off strip")],
    [for (s = KB_SCREWS, p = KB_STRIP) if (overlap(grow(strip_rect(p), 1.8), pt(s))) str("screw/", p[0])],
    [for (s = KB_SCREWS) if (s[1] > KB_Y0 + STRIP_L - 1.4) "screw under battery"]
);
echo(str("KB clashes: ", len(KB_CLASHES) == 0 ? "none" : KB_CLASHES));
assert(len(KB_CLASHES) == 0, "רכיבים חופפים בפס התחתון של KB - ר' KB clashes");

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
    STRIP_COLOR = [["USB", "Silver"], ["JACK", "DimGray"], ["SPK", "#444"], ["MIC", "Black"], ["SIM", "Goldenrod"], ["SD", "Goldenrod"]];
    for (p = KB_STRIP) color([for (c = STRIP_COLOR) if (c[0] == p[0]) c[1]][0]) part_down(p[1], p[2], KB_BOT, p[2][2]);
    color("Gold")    part_down(KB_B2B, [8, 3.2], KB_BOT, 0.15);   // פדים ל-FPC מולחם
}

// ---------------------------------------------------------------------
// סוללה, FPC, אנטנות
// ---------------------------------------------------------------------
module battery() {
    color("Orange", 0.85) translate([BATT_X0, BATT_Y0, Z_BATT_BOT]) cube(BATT);
    // PCM + זנב FPC לכיוון MB
    pcm_x = min(mb_center("BATT")[0], BATT_X0 + BATT[0] - 6);
    color("Green") translate([pcm_x - 6, BATT_Y1 - 3, Z_BATT_BOT]) cube([12, 3, BATT[2]]);
    color("Gold", 0.9) translate([pcm_x - 3, BATT_Y1 - 1, MB_BOT - 0.95]) cube([6, MB_Y0 - BATT_Y1 + 3, 0.15]);
}

// שני FPC עוברים בין גב המסך לסוללה: המסך (מתקפל בתחתית המסך) והראשי (KB <-> MB)
module fpcs() {
    z = Z_BATT_TOP + 0.1;
    color("Gold", 0.9) {
        translate([min(mb_center("LCD")[0], MB_X1 - 4.5) - 4.5, Y_SCR - 1, z]) cube([9, MB_Y0 - Y_SCR + 3, 0.12]);   // LCD
        translate([min(mb_center("MAIN")[0], MB_X1 - 4) - 4, KB_Y1 - 4, z]) cube([8, MB_Y0 - KB_Y1 + 7, 0.12]);     // MAIN
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
        // USB-C: הפתח כבר בקובץ המשפחה (USB_Z)
        // 3.5 מ"מ
        if (HAS_JACK) translate([JACK_XY[0], -0.1, KB_BOT - JACK[2] / 2]) rotate([-90, 0, 0]) cylinder(d = 3.8, h = WALL + 0.3);
        // מיקרופון
        translate([MIC_XY[0], -0.1, KB_BOT - 0.5]) rotate([-90, 0, 0]) cylinder(d = 0.8, h = WALL + 0.3);
        // רמקול - גריל בדופן התחתונה
        for (i = [-2 : 2]) translate([SPK_XY[0] + i * 2, -0.1, KB_BOT - SPK[2] / 2]) rotate([-90, 0, 0]) cylinder(d = 1.0, h = WALL + 0.3);
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

// הגב להדפסה: הגב של המשפחה פחות הפתחים (שקע, מיקרופון, רמקול, IR). הצד הפנימי למעלה
module back_shell_final() {
    difference() {
        back_shell();
        translate([0, 0, BODY_T]) shell_openings();
    }
}

if (MB_PART == "stack")           stack();
else if (MB_PART == "back_shell") back_shell_final();
else if (MB_PART == "boards")     boards();
else if (MB_PART == "mb")         { color("DarkGreen") mb_pcb(); mb_parts(); }
else if (MB_PART == "kb")         { color("DarkGreen") kb_pcb(); kb_parts(); }
else if (MB_PART == "outline_mb") projection() mb_pcb();
else if (MB_PART == "outline_kb") projection() kb_pcb();
