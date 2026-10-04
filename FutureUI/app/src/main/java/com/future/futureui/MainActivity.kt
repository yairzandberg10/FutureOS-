package com.future.futureui

import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

/**
 * FutureUI הוא ממשק המערכת - אין לו מסך משלו ואין בו הגדרות (הכול באפליקציית
 * ההגדרות, "ממשק המערכת"). ה-Activity הזו רק מבקשת את הרשאות זמן הריצה
 * שהשירותים צריכים ולא יכולים לבקש בעצמם (שירות נגישות לא מציג דיאלוג
 * הרשאה), ונסגרת. בלי READ_PHONE_STATE שורת המצב זורקת SecurityException בכל
 * בדיקת שיחה ואייקון השיחה לא מוצג; CAMERA - זיהוי הפנים במסך הנעילה.
 */
class MainActivity : ComponentActivity() {
    override fun dispatchTouchEvent(ev: android.view.MotionEvent): Boolean = true

    private val request = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { finish() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val missing = listOf(
            android.Manifest.permission.READ_PHONE_STATE,
            android.Manifest.permission.CAMERA,
        ).filter { ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED }
        if (missing.isEmpty()) finish() else request.launch(missing.toTypedArray())
    }
}
