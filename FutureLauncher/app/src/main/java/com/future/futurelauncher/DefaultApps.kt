package com.future.futurelauncher

/**
 * חבילות FutureOS המובנות (ראו הטבלה בקובץ ה-README הראשי של המאגר) - המקור
 * היחיד לאמת לגבי "אפליקציית ברירת מחדל" בכל המערכת. כשהגבלת מסך הבית
 * מופעלת (LauncherViewModel.restrictToDefaultApps), רק חבילות מהרשימה הזו
 * ניתנות להוספה כסמל או כווידג'ט למסך הבית.
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
        "com.future.guide"
    )

    fun isDefault(packageName: String): Boolean = packageName in PACKAGES
}
