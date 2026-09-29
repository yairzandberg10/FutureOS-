// =====================================================================
//  מארז ל-Qin F22 Pro בסגנון מעטפת FutureOS (futureos_family.scad)
//  שתי קליפות (קדמית + אחורית) שנסגרות סביב הטלפון, דופן 1 מ"מ, עיגולי קצה
//  כמו במודל המשפחה. כל מידות הטלפון נמצאות בבלוק אחד למטה: "מידות הטלפון".
//
//  *** המידות עדיין לא נמדדו על המכשיר ***
//  מקורות ברשת סותרים (151x61x11 לפי מדידת משתמש, 147x58x9 לפי יצרן,
//  162.5x76.8x8.8 לפי חנות). אין מידע ברשת על מיקום מקשים/כפתורים.
//  לכן ברירות המחדל הן הערכה. מדדו בקליבר, עדכנו את הבלוק, והדפיסו
//  קודם PART = "fit_test" (טבעת דקה של 3 מ"מ) לבדיקת התאמה.
//  הקונסולה מדפיסה (echo) את מידות המארז שחושבו.
//
//  מערכת צירים: X = רוחב, Y = אורך (0 = תחתית), Z = 0 בפני החזית של הטלפון,
//  הטלפון נמצא ב-Z שלילי (הגב ב-Z = -PH_T).
//  שימוש: PART = front_shell / back_shell / fit_test / assembly. F6 + STL להדפסה.
// =====================================================================

$fn = 48;

PART = "assembly"; // [assembly, front_shell, back_shell, fit_test]

// ---------------------------------------------------------------------
// מידות הטלפון (מ"מ) - למדוד בקליבר! ערכי ברירת מחדל = מדידת shuuryou/f22pro
// ---------------------------------------------------------------------
PH_L  = 151;    // אורך
PH_W  = 61;     // רוחב
PH_T  = 11;     // עובי במקום העבה ביותר (כולל בליטת מצלמה אם יש)
PH_R  = 6;      // רדיוס פינות במבט מלפנים
PH_EDGE_R = 1.5; // רדיוס עיגול קצה הגב

// מסך: אזור פעיל 640x960 באלכסון 3.54" נגזר אוטומטית; הזכוכית והמסגרת נמדדות
SCR_DIAG   = 3.54;
SCR_W      = SCR_DIAG * 25.4 * (2 / 3) / sqrt(1 + 4 / 9);   // ~49.9
SCR_H      = SCR_DIAG * 25.4 / sqrt(1 + 4 / 9);             // ~74.8
GLASS_W    = 57;     // רוחב הזכוכית / אזור המסך הגלוי כולל שוליים שחורים
GLASS_TOP  = 10;     // מרחק מהקצה העליון של הטלפון לתחילת הזכוכית (אוזניה + מצלמה קדמית)
GLASS_L    = 84;     // אורך הזכוכית
// מקלדת: מלבן שמכסה את כל המקשים (מתחת לזכוכית)
KP_W       = 51;     // רוחב אזור המקשים
KP_TOP_Y   = 54;     // גובה קצה עליון של אזור המקשים, מהתחתית
KP_BOT_Y   = 6;      // גובה קצה תחתון
KP_R       = 3;

// כפתורים בצד: [צד (-1 שמאל / +1 ימין), מרכז Y מהתחתית, אורך, גובה Z, מרכז Z ביחס לפני החזית (שלילי)]
SIDE_CUTS = [
    [ 1, 108, 26, 3.2, -PH_T / 2],   // ווליום +/- (ימין) - הערכה
    [ 1,  84, 10, 3.2, -PH_T / 2],   // הפעלה (ימין) - הערכה
    [-1, 100, 14, 3.2, -PH_T / 2]    // מגש SIM / מקשים (שמאל) - הערכה
];
USB_W = 10; USB_H = 4.0; USB_X = PH_W / 2; USB_Z = -PH_T / 2;   // USB-C בתחתית
MIC_W = 3; MIC_X = PH_W / 2 + 14;                                // חור מיקרופון בתחתית (הערכה)
JACK_ON = false; JACK_D = 6.5; JACK_X = PH_W / 2 - 14;           // שקע אוזניות בקצה העליון (אם יש)
CAM_D  = 12; CAM_XY = [PH_W - 13, PH_L - 12];                    // מצלמה אחורית (הערכה)
FLASH_D = 5; FLASH_XY = [CAM_XY[0] - 11, CAM_XY[1]];
SPK_W = 18; SPK_H = 6; SPK_XY = [PH_W / 2, 9];                   // רמקול אחורי

// ---------------------------------------------------------------------
// פרמטרי המארז (כמו futureos_family.scad)
// ---------------------------------------------------------------------
FIT     = 0.3;    // אוויר סביב הטלפון בכל כיוון
WALL    = 1.0;    // דופן וגב
FRONT_T = 1.0;    // עובי החזית
SIDE_WALL = 1.2;  // שפת החזית סביב הזכוכית
FILLET_BACK  = 1.5;
FILLET_FRONT = 0.6;
GAP = 0.25;       // מרווח לחורים
SNAP_LIP = 0.6;   // שפה קטנה בחזית שיושבת בתוך הגב (חיבור חיכוך)

BODY_W = PH_W + 2 * (FIT + WALL);
BODY_L = PH_L + 2 * (FIT + WALL);
BODY_T = PH_T + FIT + WALL + FRONT_T;  // גב + טלפון + חזית
CORNER = PH_R + FIT + WALL;
OFFS   = FIT + WALL;   // הסטת מערכת הטלפון לתוך מערכת המארז

echo(str("case W x L x T = ", BODY_W, " x ", BODY_L, " x ", BODY_T, " mm"));

module rbox(w, l, t, r) {
    hull() for (x = [r, w - r], y = [r, l - r]) translate([x, y, 0]) cylinder(r = r, h = t);
}
module sbox(w, l, t, r, fb = 0, ft = 0) {
    hull() for (x = [r, w - r], y = [r, l - r]) translate([x, y, 0]) {
        if (fb > 0) translate([0, 0, fb]) rotate_extrude() translate([r - fb, 0]) circle(r = fb);
        else cylinder(r = r, h = 0.01);
        if (ft > 0) translate([0, 0, t - ft]) rotate_extrude() translate([r - ft, 0]) circle(r = ft);
        else translate([0, 0, t - 0.01]) cylinder(r = r, h = 0.01);
    }
}
// מיקום בקואורדינטות הטלפון (X,Y מהפינה התחתונה-שמאלית של הטלפון)
module at_phone(x, y, z = 0) { translate([OFFS + x, OFFS + y, z]) children(); }

// ---------------------------------------------------------------------
// גב: קליפה שהטלפון נכנס אליה מלפנים
// ---------------------------------------------------------------------
module back_shell() {
    t = BODY_T - FRONT_T;                 // גובה הגב (הטלפון + גב)
    difference() {
        sbox(BODY_W, BODY_L, t, CORNER, FILLET_BACK, 0);
        // חלל הטלפון
        translate([WALL, WALL, WALL])
            rbox(BODY_W - 2 * WALL, BODY_L - 2 * WALL, t, CORNER - WALL);
        // מצלמה + פלאש + רמקול
        at_phone(CAM_XY[0], CAM_XY[1], -0.01) cylinder(d = CAM_D + 2 * GAP, h = WALL + 0.02);
        at_phone(FLASH_XY[0], FLASH_XY[1], -0.01) cylinder(d = FLASH_D + 2 * GAP, h = WALL + 0.02);
        for (i = [-2 : 2]) at_phone(SPK_XY[0] + i * SPK_W / 5, SPK_XY[1], -0.01)
            translate([-0.6, -SPK_H / 2, 0]) rbox(1.2, SPK_H, WALL + 0.02, 0.5);
        // USB-C + מיקרופון בתחתית
        translate([OFFS + USB_X - (USB_W + 2 * GAP) / 2, -0.01, WALL + PH_T + USB_Z + 0])
            translate([0, 0, -(USB_H + 2 * GAP) / 2 + 0]) cube([USB_W + 2 * GAP, WALL + 0.02, USB_H + 2 * GAP]);
        translate([OFFS + MIC_X, -0.01, WALL + PH_T + USB_Z]) rotate([-90, 0, 0]) cylinder(d = MIC_W, h = WALL + 0.02);
        // שקע אוזניות למעלה
        if (JACK_ON) translate([OFFS + JACK_X, BODY_L - WALL - 0.01, WALL + PH_T - PH_T / 2])
            rotate([-90, 0, 0]) cylinder(d = JACK_D + 2 * GAP, h = WALL + 0.02);
        // חריצי כפתורי צד
        for (c = SIDE_CUTS)
            translate([c[0] > 0 ? BODY_W - WALL - 0.01 : -0.01, OFFS + c[1], WALL + PH_T + c[4]])
                hull() for (y = [-(c[2] - c[3]) / 2, (c[2] - c[3]) / 2])
                    translate([0, y, 0]) rotate([0, 90, 0]) cylinder(d = c[3] + 2 * GAP, h = WALL + 0.02);
    }
}

// ---------------------------------------------------------------------
// חזית: מסגרת דקה עם חלון זכוכית + פתח מקלדת אחד. נסגרת על שפת הגב
// ---------------------------------------------------------------------
module front_shell() {
    difference() {
        union() {
            sbox(BODY_W, BODY_L, FRONT_T, CORNER, 0, FILLET_FRONT);
            // שפה פנימית שנכנסת לגב (חיבור חיכוך)
            translate([WALL + GAP, WALL + GAP, -SNAP_LIP])
                difference() {
                    rbox(BODY_W - 2 * (WALL + GAP), BODY_L - 2 * (WALL + GAP), SNAP_LIP + 0.01, CORNER - WALL - GAP);
                    translate([SIDE_WALL, SIDE_WALL, -0.01])
                        rbox(BODY_W - 2 * (WALL + GAP) - 2 * SIDE_WALL, BODY_L - 2 * (WALL + GAP) - 2 * SIDE_WALL,
                             SNAP_LIP + 0.03, max(CORNER - WALL - SIDE_WALL, 1));
                }
        }
        // חלון זכוכית (כל רוחב הזכוכית פחות SIDE_WALL, פינות עליונות עוקבות אחרי הגוף)
        translate([OFFS + (PH_W - GLASS_W) / 2 + SIDE_WALL - 1, OFFS + PH_L - GLASS_TOP - GLASS_L, -SNAP_LIP - 0.02])
            rbox(GLASS_W - 2 * SIDE_WALL + 2, GLASS_L, FRONT_T + SNAP_LIP + 0.04, 3);
        // פתח מקלדת
        translate([OFFS + (PH_W - KP_W) / 2 - GAP, OFFS + KP_BOT_Y - GAP, -SNAP_LIP - 0.02])
            rbox(KP_W + 2 * GAP, KP_TOP_Y - KP_BOT_Y + 2 * GAP, FRONT_T + SNAP_LIP + 0.04, KP_R);
        // חריץ אוזניה בפס העליון
        translate([BODY_W / 2 - 5, BODY_L - (GLASS_TOP + OFFS) / 2 - 0.4 + 0, -SNAP_LIP - 0.02])
            rbox(10, 0.8, FRONT_T + SNAP_LIP + 0.04, 0.39);
    }
}

// טבעת התאמה: פרוסה של 3 מ"מ מהגב - מדפיסים כדי לבדוק שהטלפון נכנס
module fit_test() {
    difference() {
        rbox(BODY_W, BODY_L, 3, CORNER);
        translate([WALL, WALL, -0.01]) rbox(BODY_W - 2 * WALL, BODY_L - 2 * WALL, 3.02, CORNER - WALL);
    }
}

module phone_ghost() {
    color("DimGray", 0.5) translate([OFFS, OFFS, WALL]) rbox(PH_W, PH_L, PH_T, PH_R);
}

if (PART == "front_shell") front_shell();
else if (PART == "back_shell") back_shell();
else if (PART == "fit_test") fit_test();
else {
    color("#1c1c1e") back_shell();
    color("#2a2a2d") translate([0, 0, BODY_T - FRONT_T]) front_shell();
    phone_ghost();
}
