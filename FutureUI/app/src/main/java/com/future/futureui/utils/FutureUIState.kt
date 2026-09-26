package com.future.futureui.utils

/** מצב משותף בין שירותי הנגישות של FutureUI (כולם רצים באותו תהליך). */
object FutureUIState {
    // עודכן ע"י StatusBarAccessibilityService מתוך TYPE_WINDOW_STATE_CHANGED - צריך
    // כדי לדעת אם מסך הבית (FutureLauncher) הוא האפליקציה בחזית כרגע, למשל כדי להחליט
    // אם שיחה נכנסת צריכה לפתוח את מסך השיחה במסך מלא (ר' FutureUIActions.ACTION_LAUNCH_CALL_UI).
    @Volatile
    var foregroundPackage: String? = null

    // מסך הנעילה מוצג (ר' LockScreenController). מרכז הבקרה/ההתראות לא נפתחים
    // והתראות קופצות לא מוצגות כל עוד הוא true.
    @Volatile
    var isLocked: Boolean = false
}
