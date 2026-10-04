package com.future.tools

import android.content.ComponentName
import android.content.pm.PackageManager
import com.future.tools.data.ToolShortcuts
import android.content.Context
import com.future.sharednav.widget.FutureContentWidget
import com.future.sharednav.widget.WidgetContent

/** כמה כלים יש, וכמה מהם נעוצים כאפליקציה נפרדת במסך הבית. */
class ToolsWidgetProvider : FutureContentWidget() {
    override fun content(context: Context): WidgetContent {
        val pm = context.packageManager
        val pinned = ToolShortcuts.ALIAS_BY_ROUTE.values.count { alias ->
            runCatching { pm.getComponentEnabledSetting(ComponentName(context.packageName, alias)) }.getOrNull() ==
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        }
        return WidgetContent(
            value = "${ToolShortcuts.ALIAS_BY_ROUTE.size} כלים",
            subtitle = if (pinned == 0) "אין כלים נעוצים במסך הבית" else "$pinned נעוצים במסך הבית",
        )
    }
}
