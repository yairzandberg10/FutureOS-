package com.future.settings.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.future.settings.ui.components.SettingDivider
import com.future.settings.ui.components.SettingHeader
import com.future.settings.ui.components.SettingItem
import com.future.settings.ui.components.SettingSwitch
import com.future.settings.ui.components.SettingsCard
import com.future.settings.ui.theme.ThemeConfig
import com.future.settings.utils.SysScreen
import com.future.settings.utils.SysSetting
import com.future.settings.viewmodel.SettingsViewModel

/**
 * מסך אחד לכל SysScreen של ExtraSystemSettings - כל ההגדרות המתקדמות מוצגות
 * באותה צורה: מתג כבוי/פועל, או שורה שכל לחיצת OK מעבירה אותה לערך הבא.
 */
@Composable
fun ExtraSettingsScreen(navController: NavController, theme: ThemeConfig, viewModel: SettingsViewModel, screen: SysScreen) {
    LaunchedEffect(screen.route) { viewModel.loadExtraSettings(screen) }

    Box(modifier = Modifier.fillMaxSize().background(theme.backgroundColor)) {
        Column {
            SmallHeader(screen.title, theme) { navController.popBackStack() }
            LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
                screen.sections.forEach { section ->
                    item { SettingHeader(section.title, theme) }
                    item {
                        SettingsCard(theme) {
                            section.settings.forEachIndexed { index, setting ->
                                ExtraSettingRow(setting, theme, viewModel)
                                if (index < section.settings.lastIndex) SettingDivider(theme)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExtraSettingRow(setting: SysSetting, theme: ThemeConfig, viewModel: SettingsViewModel) {
    val value = viewModel.extraValues[setting.id] ?: setting.defaultValue
    if (setting.isSwitch) {
        val checked = value == SysSetting.ON
        SettingSwitch(
            setting.title,
            setting.summary.ifBlank { null },
            checked,
            { viewModel.setExtraSetting(setting, if (checked) SysSetting.OFF else SysSetting.ON) },
            theme
        )
    } else {
        SettingItem(setting.title, setting.labelFor(value), null, theme, showChevron = false) {
            viewModel.setExtraSetting(setting, setting.next(value))
        }
    }
}
