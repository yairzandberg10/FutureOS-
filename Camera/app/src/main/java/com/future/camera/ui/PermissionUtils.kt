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
