package com.future.frixa.ui
import androidx.compose.material.icons.rounded.Restaurant

import com.future.sharednav.icons.FutureIcons
import com.future.sharednav.theme.readableAccentColor
import com.future.sharednav.theme.FutureShapes
import com.future.sharednav.theme.mutedTextColor
import com.future.sharednav.theme.FutureMotion

import com.future.sharednav.theme.FutureTypography
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.future.frixa.data.Recipe
import com.future.frixa.data.RecipeCatalog
import com.future.sharednav.components.KeypadLazyColumn
import com.future.sharednav.nav.rememberFocusListState
import com.future.sharednav.theme.FutureDimens
import com.future.sharednav.theme.FutureTheme

@Composable
fun RecipesScreen(theme: FutureTheme, onOpenRecipe: (Int) -> Unit) {
    val recipes = RecipeCatalog.all
    val focusState = rememberFocusListState(recipes.size)

    KeypadLazyColumn(
        items = recipes,
        focusState = focusState,
        onSelect = { _, recipe -> onOpenRecipe(recipe.id) },
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        key = { _, recipe -> recipe.id },
    ) { _, recipe, isFocused ->
        RecipeRow(recipe = recipe, theme = theme, isFocused = isFocused)
    }
}

/** הפוקוס כאן מגיע מבחוץ (FocusListState של KeypadLazyColumn, לא פוקוס
 * Compose אמיתי על השורה) - לכן עיצוב ידני לפי isFocused ולא FocusableItem
 * (שמניח שהוא זה שמחזיק אינטראקציה/פוקוס אמיתיים על עצמו). */
@Composable
private fun RecipeRow(recipe: Recipe, theme: FutureTheme, isFocused: Boolean) {
    val shape = FutureShapes.row
    val bgColor by animateColorAsState(
        if (isFocused) theme.readableAccentColor.copy(alpha = 0.14f) else theme.surfaceColor,
        FutureMotion.focusColorSpec,
        label = "recipeRowBg",
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp)
            .clip(shape)
            .background(bgColor)
            .then(if (isFocused) Modifier.border(FutureDimens.focusBorderItem, theme.readableAccentColor, shape) else Modifier)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.Restaurant, contentDescription = null, tint = theme.mutedTextColor)
        Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(recipe.title, color = theme.textColor, fontSize = FutureTypography.bodyLarge, fontWeight = FontWeight.Bold)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(recipe.category, color = theme.textColor.copy(alpha = 0.6f), fontSize = FutureTypography.label)
                Text(" · ", color = theme.textColor.copy(alpha = 0.4f), fontSize = FutureTypography.label)
                Icon(
                    FutureIcons.Schedule,
                    contentDescription = null,
                    tint = theme.textColor.copy(alpha = 0.5f),
                    modifier = Modifier.padding(end = 2.dp),
                )
                Text("${recipe.minutes} דק'", color = theme.textColor.copy(alpha = 0.6f), fontSize = FutureTypography.label)
            }
        }
        Icon(FutureIcons.AutoMirrored.KeyboardArrowLeft, contentDescription = null, tint = theme.textColor.copy(alpha = 0.4f))
    }
}
