package com.future.sharednav.actions

/**
 * קבועי ה-Broadcast המשותפים בין FutureUI לשאר אפליקציות המערכת. עד כה
 * הקובץ הזה היה קיים רק בתוך FutureUI (utils/FutureUIActions.kt) וכל
 * אפליקציה אחרת ששולחת/מאזינה לאחת מהפעולות האלה (בעיקר dialer, מול
 * מסך הנעילה ושורת המצב) שכפלה את אותה מחרוזת ידנית בקוד שלה במקום
 * לייבא קבוע משותף - סיכון קלאסי לטעות הקלדה ששוברת תקשורת בין
 * שירותים בלי שגיאת קומפילציה שתתפוס את זה.
 */
object FutureUIActions {
    const val ACTION_SHOW_CONTROL_CENTER = "com.future.futureui.ACTION_SHOW_CONTROL_CENTER"
    const val ACTION_SHOW_NOTIFICATION_CENTER = "com.future.futureui.ACTION_SHOW_NOTIFICATION_CENTER"

    /** נשלח על ידי כל מסך overlay כשהוא נפתח, כדי ששורת המצב הקבועה תישאר תמיד מעל כולם. */
    const val ACTION_BRING_STATUS_BAR_FRONT = "com.future.futureui.ACTION_BRING_STATUS_BAR_FRONT"

    /**
     * מקש Options/Menu הפיזי תמיד נחסם ברמת המערכת (StatusBarAccessibilityService
     * צורך אותו כדי לזהות לחיצה ארוכה ל"אפליקציות אחרונות") - אף אפליקציה בחזית
     * לא יכולה לקבל אותו ישירות. במקום זאת, לחיצה קצרה (לא ארוכה) על אותו מקש
     * משודרת גלובלית באמצעות שידור זה, כדי שהאפליקציה שבחזית תוכל להאזין ולפתוח
     * תפריט/פעולה משלה - בלי לפגוע בלחיצה הארוכה הקיימת.
     */
    const val ACTION_OPTIONS_SHORT_PRESS = "com.future.futureui.ACTION_OPTIONS_SHORT_PRESS"

    /**
     * נשלח מ-CallService של dialer כששיחה נכנסת מתחילה/מפסיקה לצלצל, כדי שמסך הנעילה
     * המותאם-אישית (LockScreenAccessibilityService) יידע לפנות את עצמו זמנית - אחרת
     * הוא היה נשאר החלון הממוקד/העליון ביותר גם מעל מסך השיחה שנפתח מתחתיו.
     */
    const val ACTION_CALL_RINGING = "com.future.futureui.ACTION_CALL_RINGING"
    const val ACTION_CALL_ENDED = "com.future.futureui.ACTION_CALL_ENDED"

    /**
     * נשלחים מ-LockScreenAccessibilityService אל dialer כשמקש CALL/ENDCALL הפיזי נלחץ תוך
     * כדי שיחה מצלצלת - נחוצים כי מקש גלובלי לא בהכרח מגיע ל-onKeyDown של dialer כשהוא
     * אינו האפליקציה בחזית (למשל כשרק הבאנר heads-up מוצג מעל אפליקציה אחרת).
     */
    const val ACTION_ANSWER_CALL = "com.future.dialer.ACTION_ANSWER_CALL"
    const val ACTION_REJECT_CALL = "com.future.dialer.ACTION_REJECT_CALL"
}
