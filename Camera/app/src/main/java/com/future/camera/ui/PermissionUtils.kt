package com.future.camera.ui

import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

/** מבקש הרשאת runtime בודדת (מצלמה) ומחזיר האם היא מאושרת כרגע. */
@Composable
fun rememberRuntimePermission(permission: String): MutableState<Boolean> {
    val context = LocalContext.current
    val granted = remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED)
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { result ->
        granted.value = result
    }
    LaunchedEffect(Unit) {
        if (!granted.value) launcher.launch(permission)
    }
    return granted
}

/** כמו rememberRuntimePermission, אבל לא מבקש מיד ב-LaunchedEffect - נועד
 * להרשאת מיקרופון שנחוצה רק במעבר למצב וידאו, לא בכניסה הרגילה לאפליקציה
 * (בקשת הרשאה לא-קשורה מיד עם הכניסה למסך היא חוויה גרועה וגם מבלבלת -
 * "למה מצלמה צריכה מיקרופון?" - אם המשתמש אף פעם לא נכנס למצב וידאו). קריאה
 * ל-request() בזמן אמת (ר' onEnterVideoMode) מפעילה את הבקשה בפועל. */
@Composable
fun rememberLazyRuntimePermission(permission: String): Pair<MutableState<Boolean>, () -> Unit> {
    val context = LocalContext.current
    val granted = remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED)
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { result ->
        granted.value = result
    }
    return granted to { if (!granted.value) launcher.launch(permission) }
}
