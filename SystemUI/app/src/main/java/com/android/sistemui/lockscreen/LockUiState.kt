package com.android.sistemui.lockscreen

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import com.android.sistemui.lockscreen.face.FaceStatus
import com.android.sistemui.lockscreen.logic.LockWallpaper

enum class LockMode { MAIN, PIN, EDIT }

/** התראה כפי שמוצגת במסך הנעילה - רק מה שצריך, בלי להחזיק את ה-StatusBarNotification. */
class LockNotification(
    val key: String,
    val packageName: String,
    val appName: String,
    val title: String,
    val text: String,
    val time: Long,
    val icon: ImageBitmap?,
)

/** הרכיב שבמיקוד במצב העריכה (כמו One UI: כל רכיב במסגרת, OK פותח את הלוח שלו). */
enum class EditTarget { CLOCK, WIDGETS, LEFT_SHORTCUT, WALLPAPER, RIGHT_SHORTCUT }

/** הלוח שעולה מלמטה בעריכה. */
enum class EditPanel { NONE, CLOCK, WIDGETS, SHORTCUTS, WALLPAPER }

/** כל מה שהמסך מצייר. רק [LockScreenController] כותב; ה-UI רק קורא. */
class LockUiState {
    var mode by mutableStateOf(LockMode.MAIN)
    var unlocking by mutableStateOf(false)

    // אבטחה
    var hasPin by mutableStateOf(false)
    var pinLength by mutableIntStateOf(4)
    var pinEntered by mutableIntStateOf(0)
    var pinError by mutableStateOf(false)
    var pinErrorTick by mutableIntStateOf(0)
    var lockoutSeconds by mutableIntStateOf(0)
    var lockoutTotal by mutableIntStateOf(0)
    var pinReason by mutableStateOf<String?>(null)
    var faceStatus by mutableStateOf<FaceStatus?>(null)
    var faceEnabled by mutableStateOf(false)
    var authenticated by mutableStateOf(false)

    // התראות
    val notifications = mutableStateListOf<LockNotification>()
    var focusedNotification by mutableIntStateOf(-1)
    var notificationPrivacy by mutableIntStateOf(1)

    // התאמה אישית
    var clockStyle by mutableIntStateOf(0)
    var clockColor by mutableIntStateOf(0)
    var background by mutableIntStateOf(0)
    var widgets by mutableStateOf(listOf("battery", "alarm", "hebdate"))
    var leftShortcut by mutableStateOf("flashlight")
    var rightShortcut by mutableStateOf("camera")
    var ownerMessage by mutableStateOf("")
    var wallpaper by mutableStateOf<ImageBitmap?>(null)
    var deviceWallpaper by mutableStateOf<ImageBitmap?>(null)
    var wallpaperId by mutableStateOf("")
    var flashlightOn by mutableStateOf(false)

    // עריכה
    var editTarget by mutableStateOf(EditTarget.CLOCK)
    var editPanel by mutableStateOf(EditPanel.NONE)
    /** השורה שבמיקוד בתוך הלוח (שעון: 0 סגנון 1 צבע; טפט: 0 קטגוריות 1 רשת 2 טשטוש; קיצורים: 0 צד 1 רשת). */
    var panelRow by mutableIntStateOf(0)
    /** הפריט שבמיקוד ברשימה / ברשת של הלוח. */
    var panelIndex by mutableIntStateOf(0)
    /** 0 הקיצור השמאלי (חזור), 1 הימני (תפריט). */
    var shortcutSide by mutableIntStateOf(0)
    val wallpaperCatalog = mutableStateListOf<LockWallpaper>()
    var wallpaperCategory by mutableIntStateOf(0)
    var wallpaperLoading by mutableStateOf<String?>(null)

    // נתוני ווידג'טים
    var batteryPercent by mutableIntStateOf(0)
    var charging by mutableStateOf(false)
    var nextAlarm by mutableStateOf<String?>(null)
    var mediaTitle by mutableStateOf<String?>(null)

    /** הקטגוריות של רשת הרקעים: "הכל" ואז לפי הסדר בקטלוג. */
    val wallpaperCategories: List<String>
        get() = listOf("הכל") + wallpaperCatalog.map { it.category }.distinct()

    /** המשבצות ברשת הרקעים: "" (הטפט של המכשיר) ואז הקטלוג המסונן. */
    val wallpaperTiles: List<String>
        get() {
            val cat = wallpaperCategories.getOrNull(wallpaperCategory)
            val list = if (wallpaperCategory == 0 || cat == null) wallpaperCatalog else wallpaperCatalog.filter { it.category == cat }
            return listOf("") + list.map { it.id }
        }
}
