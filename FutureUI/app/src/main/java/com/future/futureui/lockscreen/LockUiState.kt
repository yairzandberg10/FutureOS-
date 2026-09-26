package com.future.futureui.lockscreen

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import com.future.futureui.lockscreen.face.FaceStatus

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

/** פריט אחד במצב העריכה (לחיצה ארוכה על Menu): חצי מעלה/מטה בוחרים, ימינה/שמאלה משנים. */
enum class EditSector(val label: String) {
    CLOCK_STYLE("סגנון שעון"),
    CLOCK_COLOR("צבע"),
    BACKGROUND("רקע"),
    WIDGET_1("ווידג'ט 1"),
    WIDGET_2("ווידג'ט 2"),
    WIDGET_3("ווידג'ט 3"),
    LEFT_SHORTCUT("קיצור שמאלי"),
    RIGHT_SHORTCUT("קיצור ימני"),
}

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
    var background by mutableIntStateOf(1)
    var widgets by mutableStateOf(listOf("battery", "alarm", "hebdate"))
    var leftShortcut by mutableStateOf("flashlight")
    var rightShortcut by mutableStateOf("camera")
    var ownerMessage by mutableStateOf("")
    var editSector by mutableStateOf(EditSector.CLOCK_STYLE)
    var wallpaper by mutableStateOf<ImageBitmap?>(null)
    var flashlightOn by mutableStateOf(false)

    // נתוני ווידג'טים
    var batteryPercent by mutableIntStateOf(0)
    var charging by mutableStateOf(false)
    var nextAlarm by mutableStateOf<String?>(null)
    var mediaTitle by mutableStateOf<String?>(null)
}
