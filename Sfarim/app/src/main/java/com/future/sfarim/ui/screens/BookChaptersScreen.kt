package com.future.sfarim.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.future.sfarim.data.BookChapter
import com.future.sfarim.data.LibraryBook
import com.future.sfarim.ui.components.FocusableItem
import com.future.sfarim.ui.components.ScreenTopBar
import com.future.sfarim.ui.digitForKey
import com.future.sfarim.ui.theme.FutureTheme

@Composable
fun BookChaptersScreen(
    book: LibraryBook,
    chapters: List<BookChapter>,
    theme: FutureTheme,
    onBack: () -> Unit,
    onOpenChapter: (Int) -> Unit,
) {
    var jumpBuffer by remember { mutableStateOf("") }
    val validTopIndices = remember(chapters) { chapters.map { it.topIndex }.toSet() }
    val firstFocusRequester = remember(chapters.firstOrNull()) { FocusRequester() }
    LaunchedEffect(chapters.firstOrNull()) {
        if (chapters.isNotEmpty()) firstFocusRequester.requestFocus()
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(theme.backgroundColor)
                .onKeyEvent { event ->
                    val digit = digitForKey(event.key)
                    when {
                        event.type != KeyEventType.KeyDown -> false
                        digit != null -> {
                            jumpBuffer = (jumpBuffer + digit).takeLast(4)
                            true
                        }
                        event.nativeKeyEvent.keyCode == android.view.KeyEvent.KEYCODE_POUND -> {
                            val target = jumpBuffer.toIntOrNull()
                            jumpBuffer = ""
                            if (target != null && target in validTopIndices) onOpenChapter(target)
                            true
                        }
                        event.nativeKeyEvent.keyCode == android.view.KeyEvent.KEYCODE_STAR -> {
                            jumpBuffer = ""
                            true
                        }
                        else -> false
                    }
                },
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                ScreenTopBar(title = book.displayTitle, theme = theme, onBack = onBack)
                if (jumpBuffer.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.End,
                    ) {
                        Box(
                            modifier = Modifier
                                .background(theme.accentColor.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                        ) {
                            Text("עבור אל $jumpBuffer  (# לאישור, * לניקוי)", color = theme.accentColor, fontSize = 13.sp)
                        }
                    }
                }
                if (chapters.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("אין תוכן זמין", color = theme.textColor.copy(alpha = 0.5f), fontSize = 15.sp)
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp),
                    ) {
                        itemsIndexed(chapters, key = { _, it -> it.topIndex }) { index, chapter ->
                            FocusableItem(
                                onClick = { onOpenChapter(chapter.topIndex) },
                                theme = theme,
                                modifier = Modifier.fillMaxWidth(),
                                focusRequester = if (index == 0) firstFocusRequester else null,
                            ) { _ ->
                                Text(
                                    chapter.label,
                                    color = theme.textColor,
                                    fontSize = 16.sp,
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 14.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
