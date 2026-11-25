package com.future.wallpapers

import android.content.Context
import com.future.sharednav.widget.FutureContentWidget
import com.future.sharednav.widget.WidgetContent

/** כמה רקעים יש בקטלוג, ומאיפה הם נטענו (מובנה או מהענן). */
class WallpapersCountWidgetProvider : FutureContentWidget() {
    override fun content(context: Context): WidgetContent {
        val cached = WallpaperCatalog.cached(context)
        val all = cached.ifEmpty { WallpaperCatalog.builtIn }
        val categories = all.map { it.category }.distinct().size
        return WidgetContent(
            value = "${all.size} רקעים",
            subtitle = "$categories קטגוריות · ${if (cached.isEmpty()) "מובנה" else "מהענן"}",
        )
    }
}

/** הקטגוריה הגדולה ביותר בקטלוג, וכמה רקעים בה. */
class WallpapersCategoryWidgetProvider : FutureContentWidget() {
    override fun content(context: Context): WidgetContent {
        val all = WallpaperCatalog.cached(context).ifEmpty { WallpaperCatalog.builtIn }
        val top = all.groupingBy { it.category }.eachCount().maxByOrNull { it.value }
            ?: return WidgetContent(value = "אין רקעים", subtitle = "הקטלוג ריק")
        return WidgetContent(
            value = "${top.key} · ${top.value}",
            subtitle = "הקטגוריה הגדולה ביותר",
        )
    }
}
