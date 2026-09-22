package com.future.frixa.ui

import com.future.sharednav.theme.FutureTypography
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.future.frixa.data.Recipe
import com.future.sharednav.components.ScreenScaffold
import com.future.sharednav.theme.FutureTheme
import kotlinx.coroutines.launch

@Composable
fun RecipeDetailScreen(recipe: Recipe, theme: FutureTheme, onBack: () -> Unit) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    ScreenScaffold(
        backgroundColor = theme.backgroundColor,
        title = recipe.title,
        textColor = theme.textColor,
        accentColor = theme.accentColor,
        onBack = onBack,
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .focusRequester(focusRequester)
                .focusable()
                .onKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                    when (event.key) {
                        Key.DirectionDown -> { scope.launch { listState.animateScrollBy(220f) }; true }
                        Key.DirectionUp -> { scope.launch { listState.animateScrollBy(-220f) }; true }
                        else -> false
                    }
                },
        ) {
            item {
                Row(modifier = Modifier.padding(bottom = 16.dp)) {
                    Text(recipe.category, color = theme.accentColor, fontSize = FutureTypography.summary, fontWeight = FontWeight.SemiBold)
                    Text(" · ${recipe.minutes} דקות", color = theme.textColor.copy(alpha = 0.6f), fontSize = FutureTypography.summary)
                }
                Text("מצרכים", color = theme.textColor, fontSize = FutureTypography.bodyLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
            }
            items(recipe.ingredients) { ingredient ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(6.dp).background(theme.accentColor, CircleShape))
                    Text(ingredient, color = theme.textColor, fontSize = FutureTypography.body, modifier = Modifier.padding(start = 10.dp))
                }
            }
            item {
                Spacer(modifier = Modifier.height(20.dp))
                Text("הוראות הכנה", color = theme.textColor, fontSize = FutureTypography.bodyLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
            }
            itemsIndexed(recipe.steps) { index, step ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.Top) {
                    Box(
                        modifier = Modifier.size(22.dp).background(theme.accentColor.copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("${index + 1}", color = theme.accentColor, fontSize = FutureTypography.label, fontWeight = FontWeight.Bold)
                    }
                    Text(step, color = theme.textColor, fontSize = FutureTypography.body, modifier = Modifier.padding(start = 10.dp))
                }
            }
            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

private fun <T> androidx.compose.foundation.lazy.LazyListScope.itemsIndexed(
    list: List<T>,
    content: @Composable (Int, T) -> Unit,
) {
    items(list.size) { i -> content(i, list[i]) }
}
