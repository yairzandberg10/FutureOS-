package com.android.sistemui.utils

/** מצב משותף בין שירותי הנגישות של SystemUI (כולם רצים באותו תהליך). */
object SystemUIState {
    // עודכן ע"י StatusBarAccessibilityService מתוך TYPE_WINDOW_STATE_CHANGED - צריך
    // כדי לדעת אם מסך הבית (FutureLauncher) הוא האפליקציה בחזית כרגע, למשל כדי להחליט
    // אם שיחה נכנסת צריכה לפתוח את מסך השיחה במסך מלא (ר' SystemUIActions.ACTION_LAUNCH_CALL_UI).
    @Volatile
    var foregroundPackage: String? = null
}
