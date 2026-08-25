package com.future.futureui.utils

object FutureUIActions {
    const val ACTION_SHOW_CONTROL_CENTER = "com.future.futureui.ACTION_SHOW_CONTROL_CENTER"
    const val ACTION_SHOW_NOTIFICATION_CENTER = "com.future.futureui.ACTION_SHOW_NOTIFICATION_CENTER"
    // נשלח על ידי כל מסך overlay כשהוא נפתח, כדי ששורת המצב הקבועה תישאר תמיד מעל כולם
    const val ACTION_BRING_STATUS_BAR_FRONT = "com.future.futureui.ACTION_BRING_STATUS_BAR_FRONT"
    // מקש Options/Menu הפיזי תמיד נחסם ברמת המערכת (StatusBarAccessibilityService
    // צורך אותו כדי לזהות לחיצה ארוכה ל"אפליקציות אחרונות") - אף אפליקציה בחזית
    // לא יכולה לקבל אותו ישירות. במקום זאת, לחיצה קצרה (לא ארוכה) על אותו מקש
    // משודרת גלובלית באמצעות שידור זה, כדי שהאפליקציה שבחזית תוכל להאזין ולפתוח
    // תפריט/פעולה משלה - בלי לפגוע בלחיצה הארוכה הקיימת.
    const val ACTION_OPTIONS_SHORT_PRESS = "com.future.futureui.ACTION_OPTIONS_SHORT_PRESS"
    // נשלח מ-CallService של dialer כששיחה נכנסת מתחילה/מפסיקה לצלצל, כדי שמסך הנעילה
    // המותאם-אישית (LockScreenAccessibilityService) יידע לפנות את עצמו זמנית - אחרת
    // הוא היה נשאר החלון הממוקד/העליון ביותר גם מעל מסך השיחה שנפתח מתחתיו.
    const val ACTION_CALL_RINGING = "com.future.futureui.ACTION_CALL_RINGING"
    const val ACTION_CALL_ENDED = "com.future.futureui.ACTION_CALL_ENDED"
}
