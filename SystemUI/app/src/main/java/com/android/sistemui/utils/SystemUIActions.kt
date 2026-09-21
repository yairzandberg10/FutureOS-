package com.android.sistemui.utils

/**
 * הועבר למודול המשותף (com.future.sharednav.actions.FutureUIActions - השם שם
 * לא שונה בכוונה, ר' SystemUiTarget) כחלק מאיחוד קבועי ה-Broadcast הגלובליים -
 * קובץ זה נשאר כ-alias בלבד כדי לא לשבור import קיים בתוך SystemUI עצמו;
 * מקור האמת היחיד מעכשיו הוא המודול המשותף. אפליקציות אחרות (בעיקר dialer)
 * שהיו משכפלות את המחרוזות האלה ידנית עוברות לייבא ישירות מ-
 * com.future.sharednav.actions.FutureUIActions.
 *
 * (typealias לא מספיק כאן כי הוא לא מאפשר גישה ל-const val של האובייקט
 * דרך השם המחודש - צריך property בפועל שמצביע על ה-singleton המשותף.)
 */
val SystemUIActions = com.future.sharednav.actions.FutureUIActions
