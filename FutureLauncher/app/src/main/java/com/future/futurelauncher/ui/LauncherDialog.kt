package com.future.futurelauncher.ui

sealed class LauncherDialog {
    object None : LauncherDialog()
    data class AppOptions(val item: LauncherItem) : LauncherDialog()
    data class EmptySlotOptions(val pageIndex: Int, val itemIndex: Int) : LauncherDialog()
    data class FolderView(val folder: LauncherItem.Folder) : LauncherDialog()
    object LauncherSettings : LauncherDialog()
    object Widgets : LauncherDialog()
    object AppList : LauncherDialog()
}
