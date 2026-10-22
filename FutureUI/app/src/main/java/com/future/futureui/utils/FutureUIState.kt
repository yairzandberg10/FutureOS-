package com.future.futureui.utils

import androidx.compose.runtime.mutableStateOf

/** מצב משותף בין שירותי הנגישות של FutureUI (כולם רצים באותו תהליך). */
object FutureUIState {
    // עודכן ע"י StatusBarAccessibilityService מתוך TYPE_WINDOW_STATE_CHANGED - צריך
    // כדי לדעת אם מסך הבית (FutureLauncher) הוא האפליקציה בחזית כרגע, למשל כדי להחליט
    // אם שיחה נכנסת צריכה לפתוח את מסך השיחה במסך מלא (ר' FutureUIActions.ACTION_LAUNCH_CALL_UI).
    @Volatile
    var foregroundPackage: String? = null

    // מסך הנעילה מוצג (ר' LockScreenController). מרכז הבקרה/ההתראות לא נפתחים
    // והתראות קופצות לא מוצגות כל עוד הוא true.
    // מצב Compose ולא שדה רגיל: שורת המצב קוראת אותו כדי לדעת אם היא אטומה (N-01)
    private val lockedState = mutableStateOf(false)
    var isLocked: Boolean
        get() = lockedState.value
        set(value) { lockedState.value = value }

    // חלונות מסך מלא שמכסים את שורת המערכת בעצמם (רקע שקוף בכוונה). בזמן שאחד
    // מהם פתוח שורת המצב של FutureUI לא אטומה, כדי לא לצייר פס מעל הרקע שלהם.
    val controlCenterOpen = mutableStateOf(false)
    val notificationCenterOpen = mutableStateOf(false)
    val recentsOpen = mutableStateOf(false)
    val powerMenuOpen = mutableStateOf(false)

    // המכשיר נעול - גם כשמסך הנעילה פונה זמנית לשיחה או לשעון מעורר (ואז isLocked
    // false כדי שהמקשים יגיעו אליהם). מה שחושף תוכן - מרכז ההתראות, מסך האחרונות
    // וצילומי המסך שלו - לא נפתח כל עוד זה true.
    @Volatile
    var isSecured: Boolean = false
}
