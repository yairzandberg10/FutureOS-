package com.future.futurelauncher.ui

sealed class LauncherDialog {
    object None : LauncherDialog()
    data class AppOptions(val item: LauncherItem) : LauncherDialog()
    data class EmptySlotOptions(val pageIndex: Int, val itemIndex: Int) : LauncherDialog()
    data class FolderView(val folder: LauncherItem.Folder) : LauncherDialog()
    object LauncherSettings : LauncherDialog()
    object Widgets : LauncherDialog()
    object AppList : LauncherDialog()
    /** אישור לפני הסרה ממסך הבית - הפח מחק מיד, בלי אישור ובלי ביטול. */
    data class ConfirmRemoveItem(val item: LauncherItem) : LauncherDialog()
    /** אישור לפני מחיקת עמוד עם תוכן - הפח במצב עריכה מחק עמוד שלם ב-OK אחד. */
    data class ConfirmRemovePage(val pageIndex: Int) : LauncherDialog()
}
