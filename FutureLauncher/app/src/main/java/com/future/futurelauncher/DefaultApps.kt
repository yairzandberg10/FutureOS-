package com.future.futurelauncher

/**
 * חבילות FutureOS המובנות (ראו הטבלה בקובץ ה-README הראשי של המאגר) - המקור
 * היחיד לאמת לגבי "אפליקציית ברירת מחדל" בכל המערכת. כשהגבלת מסך הבית
 * מופעלת (LauncherViewModel.restrictToDefaultApps), רק חבילות מהרשימה הזו
 * ניתנות להוספה כסמל או כווידג'ט למסך הבית, והן היחידות שנכנסות אוטומטית
 * לפריסת ברירת המחדל (מכשיר חדש / אחרי איפוס פריסה).
 *
 * com.alert.meserhadash (פיקוד העורף) הוא היוצא מן הכלל היחיד - לא אפליקציית
 * FutureOS, אבל התרעות חירום הן קריטיות לבטיחות ולכן נכללות באותה רשימת היתר
 * בכל מקום שבו נבדקת ההגבלה.
 */
object DefaultApps {
    val PACKAGES: Set<String> = setOf(
        "com.future.futureui",
        "com.future.futurelauncher",
        "com.future.settings",
        "com.future.dialer",
        "com.future.messages",
        "com.future.notes",
        "com.future.calendar",
        "com.future.contact",
        "com.future.files",
        "com.future.keyboard",
        "com.future.gallery",
        "com.future.music",
        "com.future.sfarim",
        "com.future.terminal",
        "com.future.tools",
        "com.future.guide",
        "com.future.fitness",
        "com.future.navigation",
        "com.future.calculator",
        "com.future.clock",
        "com.future.camera",
        "com.future.acremote",
        "com.alert.meserhadash"
    )

    fun isDefault(packageName: String): Boolean = packageName in PACKAGES
}

/**
 * חבילות "למפתחים בלבד" - טרמינל, הלאנצ'ר עצמו, ו-SystemUI (FutureUI) לא אמורות
 * להיות נגישות/מוצעות למשתמש רגיל דרך רשת האפליקציות או דיאלוג "הוספת אפליקציה"
 * (ראו LauncherViewModel.loadData ו-LauncherDialogs), אלא אם מצב מפתח מופעל
 * (Settings.Global.DEVELOPMENT_SETTINGS_ENABLED - ראו SystemInteractor.isDeveloperModeEnabled
 * ב-Settings, קריא מכל אפליקציה בלי הרשאה מיוחדת).
 */
object DeveloperApps {
    val PACKAGES: Set<String> = setOf(
        "com.future.terminal",
        "com.future.futurelauncher",
        "com.future.futureui"
    )

    fun isDeveloperOnly(packageName: String): Boolean = packageName in PACKAGES
}
