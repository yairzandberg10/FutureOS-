package com.future.sharednav.actions

import com.future.sharednav.systemui.SystemUiTarget

/**
 * קבועי ה-Broadcast המשותפים בין אפליקציית ה-System UI הפעילה (ר' [SystemUiTarget])
 * לשאר אפליקציות המערכת. עד כה הקובץ הזה היה קיים רק בתוך FutureUI
 * (utils/FutureUIActions.kt) וכל אפליקציה אחרת ששולחת/מאזינה לאחת
 * מהפעולות האלה (בעיקר dialer, מול מסך הנעילה ושורת המצב) שכפלה את אותה
 * מחרוזת ידנית בקוד שלה במקום לייבא קבוע משותף - סיכון קלאסי לטעות
 * הקלדה ששוברת תקשורת בין שירותים בלי שגיאת קומפילציה שתתפוס את זה.
 */
object FutureUIActions {
    const val ACTION_SHOW_CONTROL_CENTER = "${SystemUiTarget.PACKAGE}.ACTION_SHOW_CONTROL_CENTER"
    const val ACTION_SHOW_NOTIFICATION_CENTER = "${SystemUiTarget.PACKAGE}.ACTION_SHOW_NOTIFICATION_CENTER"

    /** נשלח על ידי כל מסך overlay כשהוא נפתח, כדי ששורת המצב הקבועה תישאר תמיד מעל כולם. */
    const val ACTION_BRING_STATUS_BAR_FRONT = "${SystemUiTarget.PACKAGE}.ACTION_BRING_STATUS_BAR_FRONT"

    /**
     * מקש Options/Menu הפיזי תמיד נחסם ברמת המערכת (StatusBarAccessibilityService
     * צורך אותו כדי לזהות לחיצה ארוכה ל"אפליקציות אחרונות") - אף אפליקציה בחזית
     * לא יכולה לקבל אותו ישירות. במקום זאת, לחיצה קצרה (לא ארוכה) על אותו מקש
     * משודרת גלובלית באמצעות שידור זה, כדי שהאפליקציה שבחזית תוכל להאזין ולפתוח
     * תפריט/פעולה משלה - בלי לפגוע בלחיצה הארוכה הקיימת.
     */
    const val ACTION_OPTIONS_SHORT_PRESS = "${SystemUiTarget.PACKAGE}.ACTION_OPTIONS_SHORT_PRESS"

    /**
     * בדיוק אותו סיפור כמו ACTION_OPTIONS_SHORT_PRESS, עבור * ו-#:
     * ControlCenterAccessibilityService ו-NotificationCenterAccessibilityService
     * צורכים את שני המקשים האלה ברמת המערכת (החזקה ארוכה פותחת מרכז בקרה /
     * מרכז התראות), ומחזירים true גם ללחיצה הקצרה - כך שאף אפליקציה בחזית לא
     * יכולה לקבל אותם. הלחיצה הקצרה משודרת כאן במקום, וכך מקלדת ה-T9 יכולה
     * לפתוח את תפריט הפיסוק (*) ולהחליף שפת הקלדה (#) בלי לגעת בהחזקה הארוכה.
     */
    const val ACTION_STAR_SHORT_PRESS = "${SystemUiTarget.PACKAGE}.ACTION_STAR_SHORT_PRESS"
    const val ACTION_POUND_SHORT_PRESS = "${SystemUiTarget.PACKAGE}.ACTION_POUND_SHORT_PRESS"

    /**
     * נשלח מ-CallService של dialer כששיחה נכנסת מתחילה/מפסיקה לצלצל, כדי שמסך הנעילה
     * המותאם-אישית (LockScreenAccessibilityService) יידע לפנות את עצמו זמנית - אחרת
     * הוא היה נשאר החלון הממוקד/העליון ביותר גם מעל מסך השיחה שנפתח מתחתיו.
     */
    const val ACTION_CALL_RINGING = "${SystemUiTarget.PACKAGE}.ACTION_CALL_RINGING"
    const val ACTION_CALL_ENDED = "${SystemUiTarget.PACKAGE}.ACTION_CALL_ENDED"

    /**
     * נשלחים מ-LockScreenAccessibilityService אל dialer כשמקש CALL/ENDCALL הפיזי נלחץ תוך
     * כדי שיחה מצלצלת - נחוצים כי מקש גלובלי לא בהכרח מגיע ל-onKeyDown של dialer כשהוא
     * אינו האפליקציה בחזית (למשל כשרק הבאנר heads-up מוצג מעל אפליקציה אחרת).
     */
    const val ACTION_ANSWER_CALL = "com.future.dialer.ACTION_ANSWER_CALL"
    const val ACTION_REJECT_CALL = "com.future.dialer.ACTION_REJECT_CALL"

    /**
     * נשלח מ-LockScreenAccessibilityService אל dialer כששיחה מתחילה לצלצל בזמן שמסך
     * הבית (FutureLauncher) הוא האפליקציה בחזית - ה-fullScreenIntent הרגיל של ההתראה
     * לא מופעל אוטומטית ע"י המערכת כשהמסך דלוק ולא נעול (רק כשהוא כבוי/נעול), אז
     * זו הדרך היחידה לפתוח את מסך השיחה במסך מלא גם מעל מסך הבית.
     */
    const val ACTION_LAUNCH_CALL_UI = "com.future.dialer.ACTION_LAUNCH_CALL_UI"
}
