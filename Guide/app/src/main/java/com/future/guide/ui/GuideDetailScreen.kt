package com.future.guide.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.future.guide.data.GuideApp
import com.future.sharednav.theme.FutureTheme
import kotlinx.coroutines.launch

@Composable
fun GuideDetailScreen(app: GuideApp, theme: FutureTheme, onBack: () -> Unit) {
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    val contentFocusRequester = remember { FocusRequester() }
    LaunchedEffect(app.id) { contentFocusRequester.requestFocus() }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Box(modifier = Modifier.fillMaxSize().background(theme.backgroundColor)) {
            Column(modifier = Modifier.fillMaxSize()) {
                GuideHeader(title = app.name, theme = theme, onBack = onBack)
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .focusable()
                        .focusRequester(contentFocusRequester)
                        .onKeyEvent { event ->
                            if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                            when (event.nativeKeyEvent.keyCode) {
                                android.view.KeyEvent.KEYCODE_DPAD_DOWN -> {
                                    if (scrollState.value >= scrollState.maxValue) {
                                        false
                                    } else {
                                        scope.launch { scrollState.animateScrollBy(220f) }; true
                                    }
                                }
                                android.view.KeyEvent.KEYCODE_DPAD_UP -> {
                                    if (scrollState.value <= 0) {
                                        false
                                    } else {
                                        scope.launch { scrollState.animateScrollBy(-220f) }; true
                                    }
                                }
                                else -> false
                            }
                        }
                        .verticalScroll(scrollState)
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 24.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(theme.accentColor.copy(alpha = 0.18f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(app.icon, contentDescription = null, tint = theme.accentColor, modifier = Modifier.size(26.dp))
                        }
                        Column(modifier = Modifier.padding(start = 14.dp)) {
                            Text(app.name, color = theme.textColor, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Text(app.subtitle, color = theme.textColor.copy(alpha = 0.6f), fontSize = 13.sp)
                        }
                    }

                    GuideSectionTitle("איך משתמשים", theme = theme)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        app.steps.forEachIndexed { index, step ->
                            GuideStepRow(number = index + 1, text = step, theme = theme)
                        }
                    }

                    if (app.tips.isNotEmpty()) {
                        GuideSectionTitle("טיפים", theme = theme)
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            app.tips.forEach { tip -> GuideTip(tip, theme = theme) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GuideStepRow(number: Int, text: String, theme: FutureTheme) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(theme.accentColor.copy(alpha = 0.22f)),
            contentAlignment = Alignment.Center
        ) {
            Text("$number", color = theme.accentColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Text(
            text,
            color = theme.textColor.copy(alpha = 0.9f),
            fontSize = 14.sp,
            lineHeight = 20.sp,
            modifier = Modifier.padding(start = 12.dp).weight(1f, fill = true)
        )
    }
}
