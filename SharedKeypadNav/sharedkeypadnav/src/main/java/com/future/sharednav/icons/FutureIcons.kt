package com.future.sharednav.icons

import androidx.compose.ui.graphics.vector.ImageVector

/**
 * "Future Glyphs" - סט האייקונים של ה-Design System
 * (design/FutureOS Design System/components/core/FosIcon.jsx), מומר ל-ImageVector.
 * רשת 24, קו 1.6, קצוות ומפרקים מעוגלים. השמות זהים לשמות Material Symbols
 * שהיו בשימוש (Icons.Rounded.Home -> FutureIcons.Home), כך ששם שלא צויר בסט
 * נשאר ב-Icons.Rounded ולא נשבר שום מסך.
 *
 * הקובץ נוצר אוטומטית מ-FosIcon.jsx (SharedKeypadNav/tools/genicons.js) - לשינוי
 * אייקון יש לעדכן את ה-DS ולהריץ מחדש את המחולל, לא לערוך כאן ידנית.
 */
object FutureIcons {
    val Home: ImageVector by lazy { futureGlyph("Home", listOf("M3.8 11.2 12 4.2l8.2 7v7.4a1.8 1.8 0 0 1-1.8 1.8H5.6a1.8 1.8 0 0 1-1.8-1.8Z", "M9.4 20.4v-5.2h5.2v5.2")) }
    val Search: ImageVector by lazy { futureGlyph("Search", listOf("M10.6 4.6a6 6 0 1 0 0 12 6 6 0 0 0 0-12Z", "M15 15l4.6 4.6")) }
    val SearchOff: ImageVector by lazy { futureGlyph("SearchOff", listOf("M10.6 4.6a6 6 0 1 0 0 12 6 6 0 0 0 0-12Z", "M15 15l4.6 4.6", "M7.6 13.6 13.6 7.6")) }
    val Settings: ImageVector by lazy { futureGlyph("Settings", listOf("M4 7h9.5", "M17.5 7h2.5", "M4 12h3.5", "M11.5 12h8.5", "M4 17h9.5", "M17.5 17h2.5", "M15.5 4.9a2.1 2.1 0 1 0 0 4.2 2.1 2.1 0 0 0 0-4.2Z", "M9.5 9.9a2.1 2.1 0 1 0 0 4.2 2.1 2.1 0 0 0 0-4.2Z", "M15.5 14.9a2.1 2.1 0 1 0 0 4.2 2.1 2.1 0 0 0 0-4.2Z")) }
    val ArrowBack: ImageVector by lazy { futureGlyph("ArrowBack", listOf("M19 12H5.4", "M11 5.6 4.6 12l6.4 6.4")) }
    val ArrowForward: ImageVector by lazy { futureGlyph("ArrowForward", listOf("M5 12h13.6", "M13 5.6 19.4 12 13 18.4")) }
    val KeyboardArrowLeft: ImageVector by lazy { futureGlyph("KeyboardArrowLeft", listOf("M14.6 5.8 8.4 12l6.2 6.2")) }
    val ChevronLeft: ImageVector by lazy { futureGlyph("ChevronLeft", listOf("M14.6 5.8 8.4 12l6.2 6.2")) }
    val ChevronRight: ImageVector by lazy { futureGlyph("ChevronRight", listOf("M9.4 5.8 15.6 12l-6.2 6.2")) }
    val Circle: ImageVector by lazy { futureGlyph("Circle", listOf("M12 5a7 7 0 1 0 0 14 7 7 0 0 0 0-14Z")) }
    val KeyboardArrowRight: ImageVector by lazy { futureGlyph("KeyboardArrowRight", listOf("M9.4 5.8 15.6 12l-6.2 6.2")) }
    val KeyboardArrowDown: ImageVector by lazy { futureGlyph("KeyboardArrowDown", listOf("M5.8 9.4 12 15.6l6.2-6.2")) }
    val KeyboardArrowUp: ImageVector by lazy { futureGlyph("KeyboardArrowUp", listOf("M5.8 14.6 12 8.4l6.2 6.2")) }
    val Check: ImageVector by lazy { futureGlyph("Check", listOf("M4.8 12.4 9.6 17.2 19.2 6.8")) }
    val Close: ImageVector by lazy { futureGlyph("Close", listOf("M6 6l12 12", "M18 6 6 18")) }
    val Add: ImageVector by lazy { futureGlyph("Add", listOf("M12 4.8v14.4", "M4.8 12h14.4")) }
    val MoreVert: ImageVector by lazy { futureGlyph("MoreVert", listOf("d:12 4.9", "d:12 12", "d:12 19.1")) }
    val Refresh: ImageVector by lazy { futureGlyph("Refresh", listOf("M19.2 9.4A7.6 7.6 0 0 0 5.4 8.4", "M19.6 4.4v5h-5", "M4.8 14.6a7.6 7.6 0 0 0 13.8 1", "M4.4 19.6v-5h5")) }
    val RestartAlt: ImageVector by lazy { futureGlyph("RestartAlt", listOf("M8.8 6.4A7.6 7.6 0 1 0 12 4.4", "M8.6 6.6 12 3.9v5.2")) }
    val Edit: ImageVector by lazy { futureGlyph("Edit", listOf("M4.6 19.4h3.3L18.7 8.6a1.7 1.7 0 0 0 0-2.4l-.9-.9a1.7 1.7 0 0 0-2.4 0L4.6 16.1Z", "M14.4 6.9l2.7 2.7")) }
    val Delete: ImageVector by lazy { futureGlyph("Delete", listOf("M5.4 7h13.2", "M9.6 7V5.4A1.4 1.4 0 0 1 11 4h2a1.4 1.4 0 0 1 1.4 1.4V7", "M6.9 7l.9 11.2A1.7 1.7 0 0 0 9.5 19.8h5a1.7 1.7 0 0 0 1.7-1.6L17.1 7", "M10.6 10.4v6", "M13.4 10.4v6")) }
    val Info: ImageVector by lazy { futureGlyph("Info", listOf("M12 4a8 8 0 1 0 0 16 8 8 0 0 0 0-16Z", "M12 11.2v5.4", "d:12 8.1")) }
    val Help: ImageVector by lazy { futureGlyph("Help", listOf("M12 4a8 8 0 1 0 0 16 8 8 0 0 0 0-16Z", "M9.5 9.7a2.6 2.6 0 1 1 3 2.6v1.4", "d:12 17")) }
    val Star: ImageVector by lazy { futureGlyph("Star", listOf("M12 4.2l2.4 5 5.5.8-4 3.9.9 5.5-4.8-2.6-4.8 2.6.9-5.5-4-3.9 5.5-.8Z"), solid = true) }
    val StarBorder: ImageVector by lazy { futureGlyph("StarBorder", listOf("M12 4.2l2.4 5 5.5.8-4 3.9.9 5.5-4.8-2.6-4.8 2.6.9-5.5-4-3.9 5.5-.8Z")) }
    val Bookmark: ImageVector by lazy { futureGlyph("Bookmark", listOf("M6.6 4.6h10.8v15.2L12 15.8l-5.4 4Z"), solid = true) }
    val BookmarkBorder: ImageVector by lazy { futureGlyph("BookmarkBorder", listOf("M6.6 4.6h10.8v15.2L12 15.8l-5.4 4Z")) }
    val Call: ImageVector by lazy { futureGlyph("Call", listOf("M5.6 4.2h3.2l1.6 4-2 1.4a10.6 10.6 0 0 0 6 6l1.4-2 4 1.6v3.2a1.6 1.6 0 0 1-1.6 1.6A15.4 15.4 0 0 1 4 5.8a1.6 1.6 0 0 1 1.6-1.6Z")) }
    val CallEnd: ImageVector by lazy { futureGlyph("CallEnd", listOf("M3.4 13.4a17 17 0 0 1 17.2 0l-1.4 2.9-4-.6-.5-2.3a11 11 0 0 0-5.4 0l-.5 2.3-4 .6Z")) }
    val CallReceived: ImageVector by lazy { futureGlyph("CallReceived", listOf("M18.8 5.2 8.4 15.6", "M8.4 9.6v6h6")) }
    val CallMade: ImageVector by lazy { futureGlyph("CallMade", listOf("M5.2 18.8 15.6 8.4", "M15.6 14.4v-6h-6")) }
    val CallMissed: ImageVector by lazy { futureGlyph("CallMissed", listOf("M19.6 7 12 14.6 4.4 7", "M9.4 7H4.4v5")) }
    val RingVolume: ImageVector by lazy { futureGlyph("RingVolume", listOf("M8.6 8.9a1 1 0 0 0-1.4 0L5.6 10.5a2.2 2.2 0 0 0-.6 2.1 14.5 14.5 0 0 0 8.4 8.4 2.2 2.2 0 0 0 2.1-.6l1.6-1.6a1 1 0 0 0 0-1.4l-2.4-2.4a1 1 0 0 0-1.4 0l-1.1 1.1a11 11 0 0 1-3.8-3.8l1.1-1.1a1 1 0 0 0 0-1.4Z", "M12 2.4v2.8", "M4.9 4.4 6.9 6.4", "M19.1 4.4 17.1 6.4")) }
    val Dialpad: ImageVector by lazy { futureGlyph("Dialpad", listOf("d:7 5", "d:12 5", "d:17 5", "d:7 10", "d:12 10", "d:17 10", "d:7 15", "d:12 15", "d:17 15", "d:12 20")) }
    val Contacts: ImageVector by lazy { futureGlyph("Contacts", listOf("M6.4 3.6h11.2a2 2 0 0 1 2 2v12.8a2 2 0 0 1-2 2H6.4a2 2 0 0 1-2-2V5.6a2 2 0 0 1 2-2Z", "M12 8.2a2.4 2.4 0 1 0 0 4.8 2.4 2.4 0 0 0 0-4.8Z", "M8.2 17.4a4 4 0 0 1 7.6 0", "M2.6 8h3.8", "M2.6 12h3.8", "M2.6 16h3.8")) }
    val Person: ImageVector by lazy { futureGlyph("Person", listOf("M12 4.2a3.7 3.7 0 1 0 0 7.4 3.7 3.7 0 0 0 0-7.4Z", "M5.4 19.8c0-3.7 2.9-6.2 6.6-6.2s6.6 2.5 6.6 6.2")) }
    val Mic: ImageVector by lazy { futureGlyph("Mic", listOf("M12 3.4a2.9 2.9 0 0 0-2.9 2.9v4.8a2.9 2.9 0 0 0 5.8 0V6.3A2.9 2.9 0 0 0 12 3.4Z", "M5.6 11.1a6.4 6.4 0 0 0 12.8 0", "M12 17.5v3.1", "M8.8 20.6h6.4")) }
    val MicOff: ImageVector by lazy { futureGlyph("MicOff", listOf("M12 3.4a2.9 2.9 0 0 0-2.9 2.9v4.8a2.9 2.9 0 0 0 5.8 0V6.3A2.9 2.9 0 0 0 12 3.4Z", "M5.6 11.1a6.4 6.4 0 0 0 12.8 0", "M12 17.5v3.1", "M4.4 4.4l15.2 15.2")) }
    val VolumeUp: ImageVector by lazy { futureGlyph("VolumeUp", listOf("M4 9.4h3.4L12 5.6v12.8L7.4 14.6H4Z", "M15.3 9.4a3.7 3.7 0 0 1 0 5.2", "M17.9 6.8a7.4 7.4 0 0 1 0 10.4")) }
    val VolumeOff: ImageVector by lazy { futureGlyph("VolumeOff", listOf("M4 9.4h3.4L12 5.6v12.8L7.4 14.6H4Z", "M15.6 9.6l4.8 4.8", "M20.4 9.6l-4.8 4.8")) }
    val Pause: ImageVector by lazy { futureGlyph("Pause", listOf("M8.6 4.8v14.4", "M15.4 4.8v14.4")) }
    val PlayArrow: ImageVector by lazy { futureGlyph("PlayArrow", listOf("M8.2 5.2 18.4 12 8.2 18.8Z")) }
    val Link: ImageVector by lazy { futureGlyph("Link", listOf("M9.9 14.1a4 4 0 0 1 0-5.6l2.9-2.9a4 4 0 0 1 5.6 5.6l-1.2 1.2", "M14.1 9.9a4 4 0 0 1 0 5.6l-2.9 2.9a4 4 0 0 1-5.6-5.6l1.2-1.2")) }
    val LinkOff: ImageVector by lazy { futureGlyph("LinkOff", listOf("M9.9 14.1a4 4 0 0 1 0-5.6l1.6-1.6", "M14.1 9.9a4 4 0 0 1 0 5.6l-1.6 1.6", "M4.4 4.4l15.2 15.2")) }
    val Chat: ImageVector by lazy { futureGlyph("Chat", listOf("M4.4 6.9A2.4 2.4 0 0 1 6.8 4.5h10.4a2.4 2.4 0 0 1 2.4 2.4v7a2.4 2.4 0 0 1-2.4 2.4h-6.1l-4.5 3.4v-3.4A2.4 2.4 0 0 1 4.4 13.9Z")) }
    val Forum: ImageVector by lazy { futureGlyph("Forum", listOf("M3.6 5.4a1.9 1.9 0 0 1 1.9-1.9h8.2a1.9 1.9 0 0 1 1.9 1.9v4.4a1.9 1.9 0 0 1-1.9 1.9H7.4L3.6 14.4Z", "M8.4 14.2v1.2a1.9 1.9 0 0 0 1.9 1.9h6.3l3.8 2.8v-2.9a1.9 1.9 0 0 0 .9-1.6v-3.9a1.9 1.9 0 0 0-1.9-1.9h-1.6")) }
    val Bluetooth: ImageVector by lazy { futureGlyph("Bluetooth", listOf("M8.2 7.8 15.8 15.4 12 18.8V5.2l3.8 3.4-7.6 7.6")) }
    val BluetoothDisabled: ImageVector by lazy { futureGlyph("BluetoothDisabled", listOf("M8.2 7.8 15.8 15.4 12 18.8V5.2l3.8 3.4-7.6 7.6", "M4.4 4.4l15.2 15.2")) }
    val Headphones: ImageVector by lazy { futureGlyph("Headphones", listOf("M4.6 14.4v-2a7.4 7.4 0 0 1 14.8 0v2", "M3.6 15.4a1.8 1.8 0 0 1 1.8-1.8h1.6v6.2H5.4a1.8 1.8 0 0 1-1.8-1.8Z", "M20.4 15.4a1.8 1.8 0 0 0-1.8-1.8H17v6.2h1.6a1.8 1.8 0 0 0 1.8-1.8Z")) }
    val Speaker: ImageVector by lazy { futureGlyph("Speaker", listOf("M5.4 5.4a2 2 0 0 1 2-2h9.2a2 2 0 0 1 2 2v13.2a2 2 0 0 1-2 2H7.4a2 2 0 0 1-2-2Z", "M12 10.6a3.4 3.4 0 1 0 0 6.8 3.4 3.4 0 0 0 0-6.8Z", "d:12 6.8")) }
    val Watch: ImageVector by lazy { futureGlyph("Watch", listOf("M6.6 12a5.4 5.4 0 1 0 10.8 0 5.4 5.4 0 0 0-10.8 0Z", "M9 7.2 9.4 3.4h5.2L15 7.2", "M9 16.8l.4 3.8h5.2l.4-3.8", "M12 9.6V12l1.8 1.2")) }
    val DirectionsCar: ImageVector by lazy { futureGlyph("DirectionsCar", listOf("M4.2 16.4v-3.1l1.8-4.5a2 2 0 0 1 1.9-1.3h8.2a2 2 0 0 1 1.9 1.3l1.8 4.5v3.1Z", "M4.2 13.3h15.6", "d:7.6 15.6", "d:16.4 15.6", "M5.4 16.4v2.2h2.6v-2.2", "M16 16.4v2.2h2.6v-2.2")) }
    val FitnessCenter: ImageVector by lazy { futureGlyph("FitnessCenter", listOf("M4 10.4v3.2", "M6.8 7.8v8.4", "M17.2 7.8v8.4", "M20 10.4v3.2", "M6.8 12h10.4")) }
    val TrendingUp: ImageVector by lazy { futureGlyph("TrendingUp", listOf("M4.2 16.2 9.2 11.2l3.4 3.4L19.8 7.4", "M14.6 7.4h5.2v5.2")) }
    val Folder: ImageVector by lazy { futureGlyph("Folder", listOf("M3.6 7a2 2 0 0 1 2-2h3.6l2.1 2.4h7.1a2 2 0 0 1 2 2v7.6a2 2 0 0 1-2 2H5.6a2 2 0 0 1-2-2Z")) }
    val FolderOff: ImageVector by lazy { futureGlyph("FolderOff", listOf("M3.6 7a2 2 0 0 1 2-2h3.6l2.1 2.4h7.1a2 2 0 0 1 2 2v7.6a2 2 0 0 1-2 2H5.6a2 2 0 0 1-2-2Z", "M4.4 4.4l15.2 15.2")) }
    val Archive: ImageVector by lazy { futureGlyph("Archive", listOf("M3.6 6.2a1.6 1.6 0 0 1 1.6-1.6h13.6a1.6 1.6 0 0 1 1.6 1.6v1.4a1.6 1.6 0 0 1-1.6 1.6H5.2a1.6 1.6 0 0 1-1.6-1.6Z", "M5 9.2v8.8a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2V9.2", "M12 11.6v5.2", "M9.6 14.4 12 16.8l2.4-2.4")) }
    val Unarchive: ImageVector by lazy { futureGlyph("Unarchive", listOf("M3.6 6.2a1.6 1.6 0 0 1 1.6-1.6h13.6a1.6 1.6 0 0 1 1.6 1.6v1.4a1.6 1.6 0 0 1-1.6 1.6H5.2a1.6 1.6 0 0 1-1.6-1.6Z", "M5 9.2v8.8a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2V9.2", "M12 16.8v-5.2", "M9.6 14 12 11.6l2.4 2.4")) }
    val Image: ImageVector by lazy { futureGlyph("Image", listOf("M3.8 6.6a2.2 2.2 0 0 1 2.2-2.2h12a2.2 2.2 0 0 1 2.2 2.2v10.8a2.2 2.2 0 0 1-2.2 2.2H6a2.2 2.2 0 0 1-2.2-2.2Z", "d:8.6 9.2", "M4.4 16.8 9.2 12l3.4 3.4 3-3 4.2 4.2")) }
    val Description: ImageVector by lazy { futureGlyph("Description", listOf("M6.8 3.8h6.4L18.4 9v10.8a1.6 1.6 0 0 1-1.6 1.6H6.8a1.6 1.6 0 0 1-1.6-1.6V5.4a1.6 1.6 0 0 1 1.6-1.6Z", "M13.2 3.8V9h5.2", "M8.4 13.2h7.2", "M8.4 16.6h4.6")) }
    val MusicNote: ImageVector by lazy { futureGlyph("MusicNote", listOf("M9.8 17.2V7.2l8.4-2.2v10", "M7 14.4a2.8 2.8 0 1 0 0 5.6 2.8 2.8 0 0 0 0-5.6Z", "M15.4 12.2a2.8 2.8 0 1 0 0 5.6 2.8 2.8 0 0 0 0-5.6Z")) }
    val GraphicEq: ImageVector by lazy { futureGlyph("GraphicEq", listOf("M4.4 10v4", "M8.2 6.6v10.8", "M12 3.8v16.4", "M15.8 6.6v10.8", "M19.6 10v4")) }
    val Download: ImageVector by lazy { futureGlyph("Download", listOf("M12 4v10.4", "M7.8 10.4 12 14.6l4.2-4.2", "M5 19.4h14")) }
    val Downloading: ImageVector by lazy { futureGlyph("Downloading", listOf("M12 4v8.4", "M8.6 9.4 12 12.8l3.4-3.4", "M5.4 15.6a7.4 7.4 0 0 0 13.2 0", "M19.4 19.6H4.6")) }
    val Share: ImageVector by lazy { futureGlyph("Share", listOf("M17.4 3.4a2.6 2.6 0 1 0 0 5.2 2.6 2.6 0 0 0 0-5.2Z", "M6.6 9.4a2.6 2.6 0 1 0 0 5.2 2.6 2.6 0 0 0 0-5.2Z", "M17.4 15.4a2.6 2.6 0 1 0 0 5.2 2.6 2.6 0 0 0 0-5.2Z", "M8.9 10.7 15.1 7.3", "M8.9 13.3 15.1 16.7")) }
    val ContentCopy: ImageVector by lazy { futureGlyph("ContentCopy", listOf("M9 8.6a2 2 0 0 1 2-2h7.4a2 2 0 0 1 2 2V17a2 2 0 0 1-2 2H11a2 2 0 0 1-2-2Z", "M15.4 5.4V4.8a1.4 1.4 0 0 0-1.4-1.4H5a1.4 1.4 0 0 0-1.4 1.4v9.2A1.4 1.4 0 0 0 5 15.4h.6")) }
    val History: ImageVector by lazy { futureGlyph("History", listOf("M4.4 12a7.6 7.6 0 1 0 2.3-5.4", "M4 6.2v4.6h4.6", "M12 7.8v4.4l3.1 1.8")) }
    val Translate: ImageVector by lazy { futureGlyph("Translate", listOf("M3.6 6.4h8.8", "M8 4.2v2.2", "M10.4 6.4c0 4.2-3.4 8.2-6.8 9.8", "M5.8 10.8c1.4 3 3.8 4.8 6 5.6", "M12.6 20.4l4.3-10.2 4.3 10.2", "M14.3 16.6h5.2")) }
    val Visibility: ImageVector by lazy { futureGlyph("Visibility", listOf("M2.6 12S6.4 6.2 12 6.2 21.4 12 21.4 12 17.6 17.8 12 17.8 2.6 12 2.6 12Z", "M12 9.2a2.8 2.8 0 1 0 0 5.6 2.8 2.8 0 0 0 0-5.6Z")) }
    val VisibilityOff: ImageVector by lazy { futureGlyph("VisibilityOff", listOf("M2.6 12S6.4 6.2 12 6.2 21.4 12 21.4 12 17.6 17.8 12 17.8 2.6 12 2.6 12Z", "M4.4 4.4l15.2 15.2")) }
    val DarkMode: ImageVector by lazy { futureGlyph("DarkMode", listOf("M19.8 14.6A8.4 8.4 0 0 1 9.4 4.2a8.4 8.4 0 1 0 10.4 10.4Z")) }
    val CalendarToday: ImageVector by lazy { futureGlyph("CalendarToday", listOf("M3.8 7.4a2.2 2.2 0 0 1 2.2-2.2h12a2.2 2.2 0 0 1 2.2 2.2v10.4a2.2 2.2 0 0 1-2.2 2.2H6a2.2 2.2 0 0 1-2.2-2.2Z", "M3.8 10h16.4", "M8.4 3.4v3.6", "M15.6 3.4v3.6")) }
    val Alarm: ImageVector by lazy { futureGlyph("Alarm", listOf("M12 6a7 7 0 1 0 0 14 7 7 0 0 0 0-14Z", "M12 9.4V13l2.6 1.8", "M4.6 5.6 7.6 3", "M19.4 5.6 16.4 3")) }
    val Schedule: ImageVector by lazy { futureGlyph("Schedule", listOf("M12 4a8 8 0 1 0 0 16 8 8 0 0 0 0-16Z", "M12 7.4V12l3.2 1.9")) }
    val Timer: ImageVector by lazy { futureGlyph("Timer", listOf("M12 6.6a7 7 0 1 0 0 14 7 7 0 0 0 0-14Z", "M9.4 3.4h5.2", "M12 9.6v4")) }
    val Camera: ImageVector by lazy { futureGlyph("Camera", listOf("M3.8 8.6a2.1 2.1 0 0 1 2.1-2.1h2.3l1.3-2h5l1.3 2h2.3a2.1 2.1 0 0 1 2.1 2.1v8.3a2.1 2.1 0 0 1-2.1 2.1H5.9a2.1 2.1 0 0 1-2.1-2.1Z", "M12 9.2a3.4 3.4 0 1 0 0 6.8 3.4 3.4 0 0 0 0-6.8Z")) }
    val Wifi: ImageVector by lazy { futureGlyph("Wifi", listOf("M2.6 9.4a14 14 0 0 1 18.8 0", "M6 12.8a9 9 0 0 1 12 0", "d:12 17.4")) }
    val Lock: ImageVector by lazy { futureGlyph("Lock", listOf("M5 12.6a2.2 2.2 0 0 1 2.2-2.2h9.6a2.2 2.2 0 0 1 2.2 2.2v5a2.2 2.2 0 0 1-2.2 2.2H7.2A2.2 2.2 0 0 1 5 17.6Z", "M8.6 10.4V8.2a3.4 3.4 0 0 1 6.8 0v2.2", "d:12 15.1")) }
    val Notifications: ImageVector by lazy { futureGlyph("Notifications", listOf("M6.6 16.4V11a5.4 5.4 0 1 1 10.8 0v5.4l1.4 2.2H5.2Z", "M10 19.6a2 2 0 0 0 4 0")) }
    val Apps: ImageVector by lazy { futureGlyph("Apps", listOf("M5.2 4.6h3.4v3.4H5.2Z", "M10.3 4.6h3.4v3.4h-3.4Z", "M15.4 4.6h3.4v3.4h-3.4Z", "M5.2 10.3h3.4v3.4H5.2Z", "M10.3 10.3h3.4v3.4h-3.4Z", "M15.4 10.3h3.4v3.4h-3.4Z", "M5.2 16h3.4v3.4H5.2Z", "M10.3 16h3.4v3.4h-3.4Z", "M15.4 16h3.4v3.4h-3.4Z")) }
    val SwapHoriz: ImageVector by lazy { futureGlyph("SwapHoriz", listOf("M4.2 9.2h13.2", "M14.4 6.2 17.4 9.2l-3 3", "M19.8 14.8H6.6", "M9.6 11.8l-3 3 3 3")) }
    val ArrowUpward: ImageVector by lazy { futureGlyph("ArrowUpward", listOf("M12 19.4V5.2", "M6.6 10.6 12 5.2l5.4 5.4")) }
    val ArrowDownward: ImageVector by lazy { futureGlyph("ArrowDownward", listOf("M12 4.6v14.2", "M6.6 13.4 12 18.8l5.4-5.4")) }
    val PersonAdd: ImageVector by lazy { futureGlyph("PersonAdd", listOf("M10 4.6a3.6 3.6 0 1 0 0 7.2 3.6 3.6 0 0 0 0-7.2Z", "M3.6 19.4a6.4 6.4 0 0 1 12.8 0", "M18.6 13.4v5.2", "M16 16h5.2")) }
    val AirplanemodeActive: ImageVector by lazy { futureGlyph("AirplanemodeActive", listOf("M12 3.4a1.3 1.3 0 0 1 1.3 1.3v4.6l7.1 4.1v2.2l-7.1-2.2v4l2.4 1.8v1.4L12 19.6l-3.7 1v-1.4l2.4-1.8v-4L3.6 15.4v-2.2l7.1-4.1V4.7A1.3 1.3 0 0 1 12 3.4Z")) }
    val Brightness6: ImageVector by lazy { futureGlyph("Brightness6", listOf("M12 7.4a4.6 4.6 0 1 0 0 9.2 4.6 4.6 0 0 0 0-9.2Z", "M12 2.6v2.2", "M12 19.2v2.2", "M2.6 12h2.2", "M19.2 12h2.2", "M5.3 5.3 6.9 6.9", "M17.1 17.1l1.6 1.6", "M18.7 5.3l-1.6 1.6", "M6.9 17.1l-1.6 1.6")) }
    val BrightnessAuto: ImageVector by lazy { futureGlyph("BrightnessAuto", listOf("M12 6.8a5.2 5.2 0 1 0 0 10.4 5.2 5.2 0 0 0 0-10.4Z", "M12 2.6v1.8", "M12 19.6v1.8", "M2.6 12h1.8", "M19.6 12h1.8", "M5.3 5.3 6.6 6.6", "M17.4 17.4l1.3 1.3", "M18.7 5.3l-1.3 1.3", "M6.6 17.4l-1.3 1.3", "M10.2 14.4 12 9.6l1.8 4.8", "M10.8 12.9h2.4")) }
    val FormatSize: ImageVector by lazy { futureGlyph("FormatSize", listOf("M3.2 17.4 7.4 6.6l4.2 10.8", "M4.6 14.2h5.6", "M14.4 17.4 17.2 10.4l2.8 7", "M15.4 15.4h3.6")) }
    val Badge: ImageVector by lazy { futureGlyph("Badge", listOf("M3.8 7.4a2 2 0 0 1 2-2h12.4a2 2 0 0 1 2 2v9.2a2 2 0 0 1-2 2H5.8a2 2 0 0 1-2-2Z", "M9.4 3.6h5.2v3.8H9.4Z", "M9 11.2a1.8 1.8 0 1 0 0 3.6 1.8 1.8 0 0 0 0-3.6Z", "M14.2 11.8h3.6", "M14.2 15h3.6")) }
    val Devices: ImageVector by lazy { futureGlyph("Devices", listOf("M3.2 6.4a1.6 1.6 0 0 1 1.6-1.6h10.8a1.6 1.6 0 0 1 1.6 1.6v1.4", "M3.2 8.6v6.2a1.6 1.6 0 0 0 1.6 1.6h6.6", "M6 19.4h4", "M15.2 10.4a1.4 1.4 0 0 1 1.4-1.4h3.4a1.4 1.4 0 0 1 1.4 1.4v7.6a1.4 1.4 0 0 1-1.4 1.4h-3.4a1.4 1.4 0 0 1-1.4-1.4Z")) }
    val LaptopMac: ImageVector by lazy { futureGlyph("LaptopMac", listOf("M5.4 6.4a1.6 1.6 0 0 1 1.6-1.6h10a1.6 1.6 0 0 1 1.6 1.6v8.2H5.4Z", "M2.8 17.6h18.4", "M10.4 17.6h3.2")) }
    val Smartphone: ImageVector by lazy { futureGlyph("Smartphone", listOf("M7 4.6a1.8 1.8 0 0 1 1.8-1.8h6.4A1.8 1.8 0 0 1 17 4.6v14.8a1.8 1.8 0 0 1-1.8 1.8H8.8A1.8 1.8 0 0 1 7 19.4Z", "M10.4 5.8h3.2", "d:12 18.2")) }
    val Keyboard: ImageVector by lazy { futureGlyph("Keyboard", listOf("M2.8 7.6a1.6 1.6 0 0 1 1.6-1.6h15.2a1.6 1.6 0 0 1 1.6 1.6v8.8a1.6 1.6 0 0 1-1.6 1.6H4.4a1.6 1.6 0 0 1-1.6-1.6Z", "M8 14.8h8", "d:6 9.6", "d:9.4 9.6", "d:12.8 9.6", "d:16.2 9.6", "d:18 12.4", "d:6 12.4")) }
    val Public: ImageVector by lazy { futureGlyph("Public", listOf("M12 4a8 8 0 1 0 0 16 8 8 0 0 0 0-16Z", "M4 12h16", "M12 4c2.2 2.2 3.4 5 3.4 8s-1.2 5.8-3.4 8c-2.2-2.2-3.4-5-3.4-8S9.8 6.2 12 4Z")) }
    val Language: ImageVector by lazy { futureGlyph("Language", listOf("M12 4a8 8 0 1 0 0 16 8 8 0 0 0 0-16Z", "M4.4 9.4h15.2", "M4.4 14.6h15.2", "M12 4c2 2.4 3 5 3 8s-1 5.6-3 8c-2-2.4-3-5-3-8s1-5.6 3-8Z")) }
    val HourglassEmpty: ImageVector by lazy { futureGlyph("HourglassEmpty", listOf("M7 3.6h10", "M7 20.4h10", "M7.6 3.6v3.2L12 12l-4.4 5.2v3.2", "M16.4 3.6v3.2L12 12l4.4 5.2v3.2")) }
    val Palette: ImageVector by lazy { futureGlyph("Palette", listOf("M12 4a8 8 0 0 0 0 16 1.8 1.8 0 0 0 1.8-1.8c0-.5-.2-.9-.5-1.2a1.8 1.8 0 0 1 1.3-3h1.9A4.5 4.5 0 0 0 21 9.5C21 6.4 16.9 4 12 4Z", "d:7.6 12", "d:9.4 8.2", "d:14.2 7.8", "d:17.2 10.6")) }
    val Animation: ImageVector by lazy { futureGlyph("Animation", listOf("M9 8.4a5.6 5.6 0 1 0 0 11.2 5.6 5.6 0 0 0 0-11.2Z", "M11.6 6.2h4.2a2 2 0 0 1 2 2v4.2", "M14.2 3.8h4a2 2 0 0 1 2 2v4")) }
    val MarkEmailRead: ImageVector by lazy { futureGlyph("MarkEmailRead", listOf("M3.6 7.6a1.8 1.8 0 0 1 1.8-1.8h10.2a1.8 1.8 0 0 1 1.8 1.8v3", "M3.6 8.2 10.5 13l3.3-2.3", "M3.6 8.2v8a1.8 1.8 0 0 0 1.8 1.8h6.2", "M14.4 17.2l2 2 4.2-4.6")) }
    val DevicesOther: ImageVector by lazy { futureGlyph("DevicesOther", listOf("M3.2 6.4a1.6 1.6 0 0 1 1.6-1.6h10.8a1.6 1.6 0 0 1 1.6 1.6v1.4", "M3.2 8.6v6.2a1.6 1.6 0 0 0 1.6 1.6h6.6", "M6 19.4h4", "M15.2 10.4a1.4 1.4 0 0 1 1.4-1.4h3.4a1.4 1.4 0 0 1 1.4 1.4v7.6a1.4 1.4 0 0 1-1.4 1.4h-3.4a1.4 1.4 0 0 1-1.4-1.4Z")) }
    val Send: ImageVector by lazy { futureGlyph("Send", listOf("M20.4 4 3.6 12l16.8 8-3.2-8Z", "M17.2 12H6.4")) }
    val Error: ImageVector by lazy { futureGlyph("Error", listOf("M12 4a8 8 0 1 0 0 16 8 8 0 0 0 0-16Z", "M12 7.8v5.4", "d:12 16.4")) }
    val Remove: ImageVector by lazy { futureGlyph("Remove", listOf("M4.8 12h14.4")) }
    val Save: ImageVector by lazy { futureGlyph("Save", listOf("M5 4.4h11l3.6 3.6v11.6H5Z", "M8.4 4.4v4.4h6.4V4.4", "M8.4 19.6v-5.4h7.2v5.4")) }
    val Cancel: ImageVector by lazy { futureGlyph("Cancel", listOf("M12 4a8 8 0 1 0 0 16 8 8 0 0 0 0-16Z", "M9 9l6 6", "M15 9l-6 6")) }
    val Block: ImageVector by lazy { futureGlyph("Block", listOf("M12 4a8 8 0 1 0 0 16 8 8 0 0 0 0-16Z", "M6.4 6.4l11.2 11.2")) }
    val Tune: ImageVector by lazy { futureGlyph("Tune", listOf("M4 7h8", "M16 7h4", "M4 17h4", "M12 17h8", "M14 4.6v4.8", "M10 14.6v4.8")) }
    val Sort: ImageVector by lazy { futureGlyph("Sort", listOf("M4 7h16", "M4 12h11", "M4 17h6")) }
    val ViewList: ImageVector by lazy { futureGlyph("ViewList", listOf("M8.4 6.6h11.2", "M8.4 12h11.2", "M8.4 17.4h11.2", "d:4.8 6.6", "d:4.8 12", "d:4.8 17.4")) }
    val GridView: ImageVector by lazy { futureGlyph("GridView", listOf("M4.6 4.6h5.8v5.8H4.6Z", "M13.6 4.6h5.8v5.8h-5.8Z", "M4.6 13.6h5.8v5.8H4.6Z", "M13.6 13.6h5.8v5.8h-5.8Z")) }
    val Dashboard: ImageVector by lazy { futureGlyph("Dashboard", listOf("M4.4 4.4h6.4v8.2H4.4Z", "M13.2 4.4h6.4v4.4h-6.4Z", "M13.2 11.2h6.4v8.4h-6.4Z", "M4.4 15h6.4v4.6H4.4Z")) }
    val PushPin: ImageVector by lazy { futureGlyph("PushPin", listOf("M9 3.8h6", "M10 3.8v5.4l-3.2 3.6v1.6h10.4v-1.6L14 9.2V3.8", "M12 14.4v5.8")) }
    val Pin: ImageVector by lazy { futureGlyph("Pin", listOf("M3.6 7.6a1.8 1.8 0 0 1 1.8-1.8h13.2a1.8 1.8 0 0 1 1.8 1.8v8.8a1.8 1.8 0 0 1-1.8 1.8H5.4a1.8 1.8 0 0 1-1.8-1.8Z", "d:8 12", "d:12 12", "d:16 12")) }
    val AttachFile: ImageVector by lazy { futureGlyph("AttachFile", listOf("M16.4 7.4v8.4a4.4 4.4 0 0 1-8.8 0V6.6a2.8 2.8 0 0 1 5.6 0v8.8a1.2 1.2 0 0 1-2.4 0V7.8")) }
    val ContentPaste: ImageVector by lazy { futureGlyph("ContentPaste", listOf("M6.6 5.4H6a1.6 1.6 0 0 0-1.6 1.6v12.2A1.6 1.6 0 0 0 6 20.8h12a1.6 1.6 0 0 0 1.6-1.6V7a1.6 1.6 0 0 0-1.6-1.6h-.6", "M8.8 3.4h6.4v3.6H8.8Z")) }
    val Code: ImageVector by lazy { futureGlyph("Code", listOf("M8.6 7 3.6 12l5 5", "M15.4 7l5 5-5 5")) }
    val Terminal: ImageVector by lazy { futureGlyph("Terminal", listOf("M3.6 7a2 2 0 0 1 2-2h12.8a2 2 0 0 1 2 2v10a2 2 0 0 1-2 2H5.6a2 2 0 0 1-2-2Z", "M7.4 9.4l2.8 2.6-2.8 2.6", "M12.4 15h4.2")) }
    val LockOpen: ImageVector by lazy { futureGlyph("LockOpen", listOf("M5 12.6a2.2 2.2 0 0 1 2.2-2.2h9.6a2.2 2.2 0 0 1 2.2 2.2v5a2.2 2.2 0 0 1-2.2 2.2H7.2A2.2 2.2 0 0 1 5 17.6Z", "M8.6 10.4V8.2a3.4 3.4 0 0 1 6.6-1.2", "d:12 15.1")) }
    val Security: ImageVector by lazy { futureGlyph("Security", listOf("M12 3.6 5 6.4v5.2c0 4.2 3 7.6 7 8.8 4-1.2 7-4.6 7-8.8V6.4Z")) }
    val PrivacyTip: ImageVector by lazy { futureGlyph("PrivacyTip", listOf("M12 3.6 5 6.4v5.2c0 4.2 3 7.6 7 8.8 4-1.2 7-4.6 7-8.8V6.4Z", "M12 11v4.4", "d:12 8.2")) }
    val Build: ImageVector by lazy { futureGlyph("Build", listOf("M14.6 4.2a4.2 4.2 0 0 0-4.4 5.6l-5.8 5.8a1.8 1.8 0 0 0 2.6 2.6l5.8-5.8a4.2 4.2 0 0 0 5.6-4.4l-2.6 2.6-2.4-.4-.4-2.4Z")) }
    val Handyman: ImageVector by lazy { futureGlyph("Handyman", listOf("M5 19l8.4-8.4", "M10.4 5.6 13.6 3l5.4 5.4-2.6 2.6-2.2-.6-1-1-.6-2.2Z")) }
    val Widgets: ImageVector by lazy { futureGlyph("Widgets", listOf("M4.4 4.4h6v6h-6Z", "M4.4 13.6h6v6h-6Z", "M13.6 13.6h6v6h-6Z", "M16.6 3.4l3.9 3.9-3.9 3.9-3.9-3.9Z")) }
    val Wallpaper: ImageVector by lazy { futureGlyph("Wallpaper", listOf("M4 9V6a2 2 0 0 1 2-2h3", "M15 4h3a2 2 0 0 1 2 2v3", "M20 15v3a2 2 0 0 1-2 2h-3", "M9 20H6a2 2 0 0 1-2-2v-3", "M7.4 16.4l3-3.6 2.2 2.4 1.8-2 2.2 3.2Z", "d:15.4 8.6")) }
    val Favorite: ImageVector by lazy { futureGlyph("Favorite", listOf("M12 19.4s-7.6-4.6-7.6-10a4.2 4.2 0 0 1 7.6-2.5 4.2 4.2 0 0 1 7.6 2.5c0 5.4-7.6 10-7.6 10Z"), solid = true) }
    val FavoriteBorder: ImageVector by lazy { futureGlyph("FavoriteBorder", listOf("M12 19.4s-7.6-4.6-7.6-10a4.2 4.2 0 0 1 7.6-2.5 4.2 4.2 0 0 1 7.6 2.5c0 5.4-7.6 10-7.6 10Z")) }
    val LocationOn: ImageVector by lazy { futureGlyph("LocationOn", listOf("M12 20.8s-6.2-5.6-6.2-10.6a6.2 6.2 0 0 1 12.4 0c0 5-6.2 10.6-6.2 10.6Z", "M12 7.8a2.4 2.4 0 1 0 0 4.8 2.4 2.4 0 0 0 0-4.8Z")) }
    val MyLocation: ImageVector by lazy { futureGlyph("MyLocation", listOf("M12 5.6a6.4 6.4 0 1 0 0 12.8 6.4 6.4 0 0 0 0-12.8Z", "M12 2.8v2.8", "M12 18.4v2.8", "M2.8 12h2.8", "M18.4 12h2.8", "d:12 12")) }
    val Navigation: ImageVector by lazy { futureGlyph("Navigation", listOf("M12 3.6 18.6 19.6 12 16.2 5.4 19.6Z")) }
    val Explore: ImageVector by lazy { futureGlyph("Explore", listOf("M12 4a8 8 0 1 0 0 16 8 8 0 0 0 0-16Z", "M15.4 8.6l-2 4.8-4.8 2 2-4.8Z")) }
    val Route: ImageVector by lazy { futureGlyph("Route", listOf("M6.4 3.8a2.2 2.2 0 1 0 0 4.4 2.2 2.2 0 0 0 0-4.4Z", "M17.6 15.8a2.2 2.2 0 1 0 0 4.4 2.2 2.2 0 0 0 0-4.4Z", "M8.6 6h7.6a2.8 2.8 0 0 1 0 5.6H7.8a2.8 2.8 0 0 0 0 5.6h7.6")) }
    val Work: ImageVector by lazy { futureGlyph("Work", listOf("M3.8 9a2 2 0 0 1 2-2h12.4a2 2 0 0 1 2 2v9a2 2 0 0 1-2 2H5.8a2 2 0 0 1-2-2Z", "M9 7V5.4A1.4 1.4 0 0 1 10.4 4h3.2A1.4 1.4 0 0 1 15 5.4V7", "M3.8 12.6h16.4")) }
    val PowerSettingsNew: ImageVector by lazy { futureGlyph("PowerSettingsNew", listOf("M12 3.6v7.6", "M7.4 6.4a7 7 0 1 0 9.2 0")) }
    val BatteryFull: ImageVector by lazy { futureGlyph("BatteryFull", listOf("M6 7h9.8a2.6 2.6 0 0 1 2.6 2.6v4.8a2.6 2.6 0 0 1-2.6 2.6H6a2.6 2.6 0 0 1-2.6-2.6V9.6A2.6 2.6 0 0 1 6 7Z", "M20.8 10.4v3.2", "M7 9.4h8a1 1 0 0 1 1 1v3.2a1 1 0 0 1-1 1H7a1 1 0 0 1-1-1v-3.2a1 1 0 0 1 1-1Z")) }
    val BatteryChargingFull: ImageVector by lazy { futureGlyph("BatteryChargingFull", listOf("M6 7h9.8a2.6 2.6 0 0 1 2.6 2.6v4.8a2.6 2.6 0 0 1-2.6 2.6H6a2.6 2.6 0 0 1-2.6-2.6V9.6A2.6 2.6 0 0 1 6 7Z", "M20.8 10.4v3.2", "M11.8 9 9.6 12.2h3.4l-2 2.8")) }
    val BatterySaver: ImageVector by lazy { futureGlyph("BatterySaver", listOf("M6 7h9.8a2.6 2.6 0 0 1 2.6 2.6v4.8a2.6 2.6 0 0 1-2.6 2.6H6a2.6 2.6 0 0 1-2.6-2.6V9.6A2.6 2.6 0 0 1 6 7Z", "M20.8 10.4v3.2", "M10.9 9.8v4.4", "M8.7 12h4.4")) }
    val SignalCellularAlt: ImageVector by lazy { futureGlyph("SignalCellularAlt", listOf("M5.4 19v-4", "M10 19v-7.4", "M14.6 19V8", "M19.2 19V4.6")) }
    val SimCard: ImageVector by lazy { futureGlyph("SimCard", listOf("M7.8 3.6h6.8l3.6 3.6v12a1.6 1.6 0 0 1-1.6 1.6H7.8a1.6 1.6 0 0 1-1.6-1.6V5.2a1.6 1.6 0 0 1 1.6-1.6Z", "M9 11h6v6.4H9Z", "M12 11v6.4")) }
    val Vibration: ImageVector by lazy { futureGlyph("Vibration", listOf("M8.6 5h6.8v14H8.6Z", "M5.4 8.4v7.2", "M18.6 8.4v7.2", "M2.8 10.2v3.6", "M21.2 10.2v3.6")) }
    val DoNotDisturbOn: ImageVector by lazy { futureGlyph("DoNotDisturbOn", listOf("M12 4a8 8 0 1 0 0 16 8 8 0 0 0 0-16Z", "M8 12h8")) }
    val NotificationsOff: ImageVector by lazy { futureGlyph("NotificationsOff", listOf("M6.6 16.4V11a5.4 5.4 0 1 1 10.8 0v5.4l1.4 2.2H5.2Z", "M10 19.6a2 2 0 0 0 4 0", "M4.4 4.4l15.2 15.2")) }
    val Nightlight: ImageVector by lazy { futureGlyph("Nightlight", listOf("M15.6 4.4a8 8 0 1 0 0 15.2 8 8 0 0 1 0-15.2Z")) }
    val ScreenRotation: ImageVector by lazy { futureGlyph("ScreenRotation", listOf("M9.4 4.2l10.4 10.4-5.2 5.2L4.2 9.4Z", "M3.4 15.4a8.6 8.6 0 0 0 5.2 5.2", "M20.6 8.6a8.6 8.6 0 0 0-5.2-5.2")) }
    val Storage: ImageVector by lazy { futureGlyph("Storage", listOf("M4 5.4h16V9H4Z", "M4 10.2h16v3.6H4Z", "M4 15h16v3.6H4Z", "d:6.8 7.2", "d:6.8 12", "d:6.8 16.8")) }
    val Memory: ImageVector by lazy { futureGlyph("Memory", listOf("M7 7h10v10H7Z", "M10 10h4v4h-4Z", "M9.4 4v3", "M14.6 4v3", "M9.4 17v3", "M14.6 17v3", "M4 9.4h3", "M4 14.6h3", "M17 9.4h3", "M17 14.6h3")) }
    val Speed: ImageVector by lazy { futureGlyph("Speed", listOf("M4.8 17.6a8 8 0 1 1 14.4 0", "M12 14.2l4-5", "d:12 14.2")) }
    val Sensors: ImageVector by lazy { futureGlyph("Sensors", listOf("d:12 12", "M8.6 8.6a4.8 4.8 0 0 0 0 6.8", "M15.4 8.6a4.8 4.8 0 0 1 0 6.8", "M5.8 5.8a8.8 8.8 0 0 0 0 12.4", "M18.2 5.8a8.8 8.8 0 0 1 0 12.4")) }
    val Accessibility: ImageVector by lazy { futureGlyph("Accessibility", listOf("d:12 4.6", "M5 8.4l7 1.4 7-1.4", "M12 9.8V14", "M9 20l3-6 3 6")) }
    val Autorenew: ImageVector by lazy { futureGlyph("Autorenew", listOf("M18.6 9.4A7 7 0 0 0 5.4 10", "M18.6 4.8v4.6H14", "M5.4 14.6a7 7 0 0 0 13.2.6", "M5.4 19.2v-4.6H10")) }
    val Sos: ImageVector by lazy { futureGlyph("Sos", listOf("M7.2 8.6H4.4a1.6 1.6 0 0 0 0 3.2h1.2a1.6 1.6 0 0 1 0 3.2H2.8", "M12 8.6a3.2 3.2 0 1 0 0 6.4 3.2 3.2 0 0 0 0-6.4Z", "M21.2 8.6h-2.8a1.6 1.6 0 0 0 0 3.2h1.2a1.6 1.6 0 0 1 0 3.2h-2.8")) }
    val SkipNext: ImageVector by lazy { futureGlyph("SkipNext", listOf("M6 6l8.4 6L6 18Z", "M18 6v12")) }
    val SkipPrevious: ImageVector by lazy { futureGlyph("SkipPrevious", listOf("M18 6l-8.4 6L18 18Z", "M6 6v12")) }
    val Stop: ImageVector by lazy { futureGlyph("Stop", listOf("M6.6 6.6h10.8v10.8H6.6Z")) }
    val StopCircle: ImageVector by lazy { futureGlyph("StopCircle", listOf("M12 4a8 8 0 1 0 0 16 8 8 0 0 0 0-16Z", "M9.4 9.4h5.2v5.2H9.4Z")) }
    val PlayCircle: ImageVector by lazy { futureGlyph("PlayCircle", listOf("M12 4a8 8 0 1 0 0 16 8 8 0 0 0 0-16Z", "M10 8.6 15.4 12 10 15.4Z")) }
    val Repeat: ImageVector by lazy { futureGlyph("Repeat", listOf("M4.6 11V9.4a2 2 0 0 1 2-2h12.2", "M16 4.4l3 3-3 3", "M19.4 13v1.6a2 2 0 0 1-2 2H5.2", "M8 19.6l-3-3 3-3")) }
    val RepeatOne: ImageVector by lazy { futureGlyph("RepeatOne", listOf("M4.6 11V9.4a2 2 0 0 1 2-2h12.2", "M16 4.4l3 3-3 3", "M19.4 13v1.6a2 2 0 0 1-2 2H5.2", "M8 19.6l-3-3 3-3", "M11.4 10.6l1.2-.8v4.6")) }
    val Shuffle: ImageVector by lazy { futureGlyph("Shuffle", listOf("M4 7h3.4l9.2 10H20", "M4 17h3.4l2.4-2.6", "M14.2 9.6 16.6 7H20", "M17.6 4l2.8 3-2.8 3", "M17.6 14l2.8 3-2.8 3")) }
    val Equalizer: ImageVector by lazy { futureGlyph("Equalizer", listOf("M6 19.4v-8", "M10 19.4V4.6", "M14 19.4v-11", "M18 19.4v-5")) }
    val QueueMusic: ImageVector by lazy { futureGlyph("QueueMusic", listOf("M4 6.4h11", "M4 11h11", "M4 15.6h6", "M18.6 6v10.6", "M18.6 6H21", "M16.4 14.4a2.2 2.2 0 1 0 0 4.4 2.2 2.2 0 0 0 0-4.4Z")) }
    val PlaylistAdd: ImageVector by lazy { futureGlyph("PlaylistAdd", listOf("M4 6.4h11", "M4 11h11", "M4 15.6h7", "M17.6 12.4v7.2", "M14 16h7.2")) }
    val Album: ImageVector by lazy { futureGlyph("Album", listOf("M12 4a8 8 0 1 0 0 16 8 8 0 0 0 0-16Z", "M12 9.8a2.2 2.2 0 1 0 0 4.4 2.2 2.2 0 0 0 0-4.4Z")) }
    val VolumeDown: ImageVector by lazy { futureGlyph("VolumeDown", listOf("M5.4 9.4h3.4l4.6-3.8v12.8l-4.6-3.8H5.4Z", "M16.7 9.4a3.7 3.7 0 0 1 0 5.2")) }
    val Videocam: ImageVector by lazy { futureGlyph("Videocam", listOf("M3.4 7.6a1.8 1.8 0 0 1 1.8-1.8h8.6a1.8 1.8 0 0 1 1.8 1.8v8.8a1.8 1.8 0 0 1-1.8 1.8H5.2a1.8 1.8 0 0 1-1.8-1.8Z", "M15.6 10.4l5-3v9.2l-5-3")) }
    val Movie: ImageVector by lazy { futureGlyph("Movie", listOf("M3.8 6a1.6 1.6 0 0 1 1.6-1.6h13.2A1.6 1.6 0 0 1 20.2 6v12a1.6 1.6 0 0 1-1.6 1.6H5.4A1.6 1.6 0 0 1 3.8 18Z", "M3.8 9h16.4", "M8 4.4 9.6 9", "M13 4.4 14.6 9")) }
    val RecordVoiceOver: ImageVector by lazy { futureGlyph("RecordVoiceOver", listOf("M9.4 5a3.4 3.4 0 1 0 0 6.8 3.4 3.4 0 0 0 0-6.8Z", "M3.4 19.6a6 6 0 0 1 12 0", "M16.4 6.4a3.6 3.6 0 0 1 0 4", "M19 4a7.2 7.2 0 0 1 0 8.8")) }
    val FlashOn: ImageVector by lazy { futureGlyph("FlashOn", listOf("M13.4 3.4 6.4 13h5l-1 7.6 7.2-10h-5.2Z")) }
    val FlashOff: ImageVector by lazy { futureGlyph("FlashOff", listOf("M13.4 3.4 6.4 13h5l-1 7.6 7.2-10h-5.2Z", "M4.4 4.4l15.2 15.2")) }
    val FlashAuto: ImageVector by lazy { futureGlyph("FlashAuto", listOf("M10.4 3.4 4.4 12h4.4L8 19.6 14 11H9.6Z", "M14.6 19.6l2.6-7 2.6 7", "M15.4 17.4H19")) }
    val FlashlightOn: ImageVector by lazy { futureGlyph("FlashlightOn", listOf("M7.4 3.6h9.2v3.2l-2 3.4v10.2H9.4V10.2l-2-3.4Z", "M7.4 6.8h9.2", "d:12 13.6")) }
    val FlashlightOff: ImageVector by lazy { futureGlyph("FlashlightOff", listOf("M7.4 3.6h9.2v3.2l-2 3.4v10.2H9.4V10.2l-2-3.4Z", "M7.4 6.8h9.2", "M4.4 4.4l15.2 15.2")) }
    val GridOn: ImageVector by lazy { futureGlyph("GridOn", listOf("M4.4 4.4h15.2v15.2H4.4Z", "M9.5 4.4v15.2", "M14.5 4.4v15.2", "M4.4 9.5h15.2", "M4.4 14.5h15.2")) }
    val HighQuality: ImageVector by lazy { futureGlyph("HighQuality", listOf("M3.4 6.6a1.8 1.8 0 0 1 1.8-1.8h13.6a1.8 1.8 0 0 1 1.8 1.8v10.8a1.8 1.8 0 0 1-1.8 1.8H5.2a1.8 1.8 0 0 1-1.8-1.8Z", "M6.6 9v6", "M10 9v6", "M6.6 12H10", "M13.4 9h3.4v6h-3.4Z", "M16 14l1.4 1.6")) }
    val AspectRatio: ImageVector by lazy { futureGlyph("AspectRatio", listOf("M3.4 6.6a1.8 1.8 0 0 1 1.8-1.8h13.6a1.8 1.8 0 0 1 1.8 1.8v10.8a1.8 1.8 0 0 1-1.8 1.8H5.2a1.8 1.8 0 0 1-1.8-1.8Z", "M6.6 11V8.4h3", "M17.4 13v2.6h-3")) }
    val Crop: ImageVector by lazy { futureGlyph("Crop", listOf("M6.6 3.4v12.2a1.8 1.8 0 0 0 1.8 1.8h12.2", "M3.4 6.6h12.2a1.8 1.8 0 0 1 1.8 1.8v12.2")) }
    val RotateLeft: ImageVector by lazy { futureGlyph("RotateLeft", listOf("M5.6 12.6a6.8 6.8 0 1 0 2-4.8", "M4.2 4.4V9h4.6")) }
    val Flip: ImageVector by lazy { futureGlyph("Flip", listOf("M12 3.4v17.2", "M9 6.4 3.6 17.6H9Z", "M15 6.4l5.4 11.2H15Z")) }
    val AutoAwesome: ImageVector by lazy { futureGlyph("AutoAwesome", listOf("M10 4.6l1.6 4.4 4.4 1.6-4.4 1.6-1.6 4.4-1.6-4.4L4 10.6l4.4-1.6Z", "M17.6 13.6l.8 2.2 2.2.8-2.2.8-.8 2.2-.8-2.2-2.2-.8 2.2-.8Z", "M17.4 3.6v3.2", "M15.8 5.2H19")) }
    val FilterVintage: ImageVector by lazy { futureGlyph("FilterVintage", listOf("M12 9.6a2.4 2.4 0 1 0 0 4.8 2.4 2.4 0 0 0 0-4.8Z", "M12 9.6c-2-3-1.4-5.4 0-6.2 1.4.8 2 3.2 0 6.2Z", "M12 14.4c2 3 1.4 5.4 0 6.2-1.4-.8-2-3.2 0-6.2Z", "M9.6 12c-3 2-5.4 1.4-6.2 0 .8-1.4 3.2-2 6.2 0Z", "M14.4 12c3-2 5.4-1.4 6.2 0-.8 1.4-3.2 2-6.2 0Z")) }
    val NoPhotography: ImageVector by lazy { futureGlyph("NoPhotography", listOf("M3.8 8.6a2.1 2.1 0 0 1 2.1-2.1h2.3l1.3-2h5l1.3 2h2.3a2.1 2.1 0 0 1 2.1 2.1v8.3a2.1 2.1 0 0 1-2.1 2.1H5.9a2.1 2.1 0 0 1-2.1-2.1Z", "M12 9.2a3.4 3.4 0 1 0 0 6.8 3.4 3.4 0 0 0 0-6.8Z", "M4.4 4.4l15.2 15.2")) }
    val AddAPhoto: ImageVector by lazy { futureGlyph("AddAPhoto", listOf("M3.8 8.6a2.1 2.1 0 0 1 2.1-2.1h2.3l1.3-2h5l1.3 2h2.3a2.1 2.1 0 0 1 2.1 2.1v8.3a2.1 2.1 0 0 1-2.1 2.1H5.9a2.1 2.1 0 0 1-2.1-2.1Z", "M12 9.2a3.4 3.4 0 1 0 0 6.8 3.4 3.4 0 0 0 0-6.8Z", "M19.4 1.8v3.6", "M17.6 3.6h3.6")) }
    val CreateNewFolder: ImageVector by lazy { futureGlyph("CreateNewFolder", listOf("M3.6 7a2 2 0 0 1 2-2h3.6l2.1 2.4h7.1a2 2 0 0 1 2 2v7.6a2 2 0 0 1-2 2H5.6a2 2 0 0 1-2-2Z", "M12 10.4v5.2", "M9.4 13h5.2")) }
    val DriveFileMove: ImageVector by lazy { futureGlyph("DriveFileMove", listOf("M3.6 7a2 2 0 0 1 2-2h3.6l2.1 2.4h7.1a2 2 0 0 1 2 2v7.6a2 2 0 0 1-2 2H5.6a2 2 0 0 1-2-2Z", "M8.4 13h6.4", "M12.4 10.4 15 13l-2.6 2.6")) }
    val DriveFileRenameOutline: ImageVector by lazy { futureGlyph("DriveFileRenameOutline", listOf("M4.4 19.6h3L17.8 9.2l-3-3L4.4 16.6Z", "M13 8l3 3", "M12 19.6h7.6")) }
    val PictureAsPdf: ImageVector by lazy { futureGlyph("PictureAsPdf", listOf("M6.8 3.8h6.4L18.4 9v10.8a1.6 1.6 0 0 1-1.6 1.6H6.8a1.6 1.6 0 0 1-1.6-1.6V5.4a1.6 1.6 0 0 1 1.6-1.6Z", "M13.2 3.8V9h5.2", "M8.4 13v4.4", "M8.4 13h1.4a1.2 1.2 0 0 1 0 2.4H8.4", "M12.2 13v4.4h.8a1.6 1.6 0 0 0 1.6-1.6v-1.2a1.6 1.6 0 0 0-1.6-1.6Z")) }
    val DownloadDone: ImageVector by lazy { futureGlyph("DownloadDone", listOf("M5.6 11.4l4.4 4.4 8.4-8.8", "M5 19.4h14")) }
    val Android: ImageVector by lazy { futureGlyph("Android", listOf("M4.6 17.4a7.4 7.4 0 0 1 14.8 0Z", "M8 8.4 6.4 5.8", "M16 8.4l1.6-2.6", "d:9.2 13.8", "d:14.8 13.8")) }
    val Book: ImageVector by lazy { futureGlyph("Book", listOf("M6 4.4h11.6v15.2H7.4A1.4 1.4 0 0 1 6 18.2Z", "M6 17.4A1.4 1.4 0 0 1 7.4 16h10.2", "M10 4.4v6l1.8-1.2 1.8 1.2v-6")) }
    val MenuBook: ImageVector by lazy { futureGlyph("MenuBook", listOf("M12 6.8c-2-1.6-4.8-2-8-1.6v12.6c3.2-.4 6 0 8 1.6 2-1.6 4.8-2 8-1.6V5.2c-3.2-.4-6 0-8 1.6Z", "M12 6.8v12.6")) }
    val LibraryBooks: ImageVector by lazy { futureGlyph("LibraryBooks", listOf("M7.6 3.8h11a1.6 1.6 0 0 1 1.6 1.6v11a1.6 1.6 0 0 1-1.6 1.6h-11A1.6 1.6 0 0 1 6 16.4v-11a1.6 1.6 0 0 1 1.6-1.6Z", "M3.4 7.4v11.4a1.8 1.8 0 0 0 1.8 1.8h11.4", "M9.6 8H16", "M9.6 11H16", "M9.6 14h4")) }
    val BookShelf: ImageVector by lazy { futureGlyph("BookShelf", listOf("M3 20h18", "M4.6 4.6h3v15.4h-3Z", "M8.6 6.6h3v13.4h-3Z", "M12.8 5.6l2.8-.8 3.6 14.6-2.8.8Z", "M4.6 8h3", "M8.6 9.6h3")) }
    val BookOpen: ImageVector by lazy { futureGlyph("BookOpen", listOf("M12 18.6c-2.4-1.6-5.4-1.8-8.6-1.2V6.6c3.2-.6 6.2-.4 8.6 1.2 2.4-1.6 5.4-1.8 8.6-1.2v10.8c-3.2-.6-6.2-.4-8.6 1.2Z", "M12 7.8v10.8", "M3.4 19.8c3.2-.6 6.2-.2 8.6 1.2 2.4-1.4 5.4-1.8 8.6-1.2")) }
    val AutoStories: ImageVector by lazy { futureGlyph("AutoStories", listOf("M3.6 6.2 11 8.6v11.2l-7.4-2.4Z", "M11 8.6l6.6-4.8v11.4L11 19.8", "M20.4 7v11.6")) }
    val TextIncrease: ImageVector by lazy { futureGlyph("TextIncrease", listOf("M3 18.4 7.6 6.6l4.6 11.8", "M4.6 14.4h6", "M15 12h6", "M18 9v6")) }
    val TextDecrease: ImageVector by lazy { futureGlyph("TextDecrease", listOf("M3 18.4 7.6 6.6l4.6 11.8", "M4.6 14.4h6", "M15 12h6")) }
    val Spellcheck: ImageVector by lazy { futureGlyph("Spellcheck", listOf("M3.4 13.4 6.8 4.6l3.4 8.8", "M4.6 10.4H9", "M11.8 15.2l3.2 3.2 6-6.4")) }
    val WbSunny: ImageVector by lazy { futureGlyph("WbSunny", listOf("M12 7.6a4.4 4.4 0 1 0 0 8.8 4.4 4.4 0 0 0 0-8.8Z", "M12 2.6v2", "M12 19.4v2", "M2.6 12h2", "M19.4 12h2", "M5.4 5.4l1.4 1.4", "M17.2 17.2l1.4 1.4", "M18.6 5.4l-1.4 1.4", "M6.8 17.2l-1.4 1.4")) }
    val WbCloudy: ImageVector by lazy { futureGlyph("WbCloudy", listOf("M8.6 4.6a3.6 3.6 0 0 0-2.8 5.9", "M8.6 2.2V3", "M3 8.2h.8", "M4.6 4.2l.6.6", "M8.6 19.4a3.6 3.6 0 0 1-.4-7.2 5 5 0 0 1 9.6 1.1 3 3 0 0 1-.2 6.1Z")) }
    val WbTwilight: ImageVector by lazy { futureGlyph("WbTwilight", listOf("M5.6 16.4a6.4 6.4 0 0 1 12.8 0", "M3 19.4h18", "M12 4.6v2.6", "M4.6 8.6l1.8 1.8", "M19.4 8.6l-1.8 1.8")) }
    val Cloud: ImageVector by lazy { futureGlyph("Cloud", listOf("M7.2 18.4a4 4 0 0 1-.4-8 5.6 5.6 0 0 1 10.8 1.2 3.4 3.4 0 0 1-.4 6.8Z")) }
    val Umbrella: ImageVector by lazy { futureGlyph("Umbrella", listOf("M3.4 12a8.6 8.6 0 0 1 17.2 0Z", "M12 12v6.2a2 2 0 0 1-4 0", "M12 2.6v.8")) }
    val AcUnit: ImageVector by lazy { futureGlyph("AcUnit", listOf("M12 3v18", "M4.2 7.5l15.6 9", "M4.2 16.5l15.6-9", "M9.8 4.4 12 6.4l2.2-2", "M9.8 19.6l2.2-2 2.2 2")) }
    val Thermostat: ImageVector by lazy { futureGlyph("Thermostat", listOf("M9.8 5.6a2.2 2.2 0 0 1 4.4 0v8.2a4 4 0 1 1-4.4 0Z", "M12 9.4v7")) }
    val Grain: ImageVector by lazy { futureGlyph("Grain", listOf("d:6 6", "d:12 8", "d:9 12.4", "d:15.4 12", "d:6 17", "d:18 6", "d:12 17.6", "d:18 17")) }
    val Dehaze: ImageVector by lazy { futureGlyph("Dehaze", listOf("M4 7h16", "M4 12h16", "M4 17h16")) }
    val DirectionsRun: ImageVector by lazy { futureGlyph("DirectionsRun", listOf("M15.2 3.4a1.6 1.6 0 1 0 0 3.2 1.6 1.6 0 0 0 0-3.2Z", "M6.4 9.6l3.4-2.2h3.6l2 3.4 3 1", "M12.6 7.4 10 13.4l3.6 2.4-1 4.6", "M10 13.4 8 17H4.4")) }
    val DirectionsWalk: ImageVector by lazy { futureGlyph("DirectionsWalk", listOf("M13.2 3.4a1.6 1.6 0 1 0 0 3.2 1.6 1.6 0 0 0 0-3.2Z", "M12.4 8.4 10.6 14l2.8 2.6.6 4", "M10.6 14l-1.8 6.4", "M12.4 8.4l-3.4 2v3", "M12.4 8.4l1.6 3 2.6 1")) }
    val DirectionsBike: ImageVector by lazy { futureGlyph("DirectionsBike", listOf("M6 12.6a3.6 3.6 0 1 0 0 7.2 3.6 3.6 0 0 0 0-7.2Z", "M18 12.6a3.6 3.6 0 1 0 0 7.2 3.6 3.6 0 0 0 0-7.2Z", "M6 16.2 10 10l3 3.6h5", "M10 10h4.4", "M15.6 3.6a1.6 1.6 0 1 0 0 3.2 1.6 1.6 0 0 0 0-3.2Z", "M18 16.2l-3.2-6.8")) }
    val Pool: ImageVector by lazy { futureGlyph("Pool", listOf("M3 18c1.5-1.2 3-1.2 4.5 0s3 1.2 4.5 0 3-1.2 4.5 0 3 1.2 4.5 0", "M7.6 14.2 13 8.6l-2.6-2.4-3 .8", "M13 8.6l4 5.6", "M17 5.4a1.6 1.6 0 1 0 0 3.2 1.6 1.6 0 0 0 0-3.2Z")) }
    val DirectionsBoat: ImageVector by lazy { futureGlyph("DirectionsBoat", listOf("M3.6 15.2 12 12.4l8.4 2.8-2 4.4H5.6Z", "M6.4 14.2V8.6h11.2v5.6", "M12 5v3.6", "M9.6 5h4.8", "M12 12.4v7.2")) }
    val Terrain: ImageVector by lazy { futureGlyph("Terrain", listOf("M2.8 18.6 9 8.6l3.6 5.6 2.4-3.4 6.2 7.8Z")) }
    val Spa: ImageVector by lazy { futureGlyph("Spa", listOf("M12 19.4c-4.6 0-8-2.8-8.4-7 3.6 0 6.8 2 8.4 7Z", "M12 19.4c4.6 0 8-2.8 8.4-7-3.6 0-6.8 2-8.4 7Z", "M12 19.4c-2.4-2.6-2.4-9.6 0-14 2.4 4.4 2.4 11.4 0 14Z")) }
    val Whatshot: ImageVector by lazy { futureGlyph("Whatshot", listOf("M12 20.4a6 6 0 0 0 6-6c0-3.6-2.8-5.6-3.6-10.2-2.8 1.6-4.4 4.2-4.2 7-1.2-.6-1.8-1.8-2-3-1.6 1.6-2.2 3.8-2.2 6.2a6 6 0 0 0 6 6Z")) }
    val LocalFireDepartment: ImageVector by lazy { futureGlyph("LocalFireDepartment", listOf("M12 20.4a6 6 0 0 0 6-6c0-3.6-2.8-5.6-3.6-10.2-2.8 1.6-4.4 4.2-4.2 7-1.2-.6-1.8-1.8-2-3-1.6 1.6-2.2 3.8-2.2 6.2a6 6 0 0 0 6 6Z", "M12 20.4a2.6 2.6 0 0 1-2.6-2.6c0-1.6 1.4-2.4 2.6-4.2 1.2 1.8 2.6 2.6 2.6 4.2a2.6 2.6 0 0 1-2.6 2.6Z")) }
    val EmojiEvents: ImageVector by lazy { futureGlyph("EmojiEvents", listOf("M7.4 4.2h9.2v5.4a4.6 4.6 0 0 1-9.2 0Z", "M7.4 6H4.6v1.6a3 3 0 0 0 3 3", "M16.6 6h2.8v1.6a3 3 0 0 1-3 3", "M12 14.2v3.2", "M9.6 17.4h4.8V20H9.6Z")) }
    val MilitaryTech: ImageVector by lazy { futureGlyph("MilitaryTech", listOf("M7.6 3.4l2.8 6.4", "M16.4 3.4l-2.8 6.4", "M7.6 3.4h8.8", "M12 9.8a4.8 4.8 0 1 0 0 9.6 4.8 4.8 0 0 0 0-9.6Z", "M12 12.2l.9 1.8 2 .3-1.4 1.4.3 2-1.8-.9-1.8.9.3-2-1.4-1.4 2-.3Z")) }
    val VideogameAsset: ImageVector by lazy { futureGlyph("VideogameAsset", listOf("M3.4 9a2 2 0 0 1 2-2h13.2a2 2 0 0 1 2 2v6a2 2 0 0 1-2 2H5.4a2 2 0 0 1-2-2Z", "M8 10v4", "M6 12h4", "d:15.2 11", "d:17.4 13.2")) }
    val Calculate: ImageVector by lazy { futureGlyph("Calculate", listOf("M5 5.4a1.6 1.6 0 0 1 1.6-1.6h10.8A1.6 1.6 0 0 1 19 5.4v13.2a1.6 1.6 0 0 1-1.6 1.6H6.6A1.6 1.6 0 0 1 5 18.6Z", "M8 7.4h8v3H8Z", "d:8.6 13.6", "d:12 13.6", "d:15.4 13.6", "d:8.6 17", "d:12 17", "d:15.4 17")) }
    val Functions: ImageVector by lazy { futureGlyph("Functions", listOf("M17.4 4.6H6.6l6 7.4-6 7.4h10.8")) }
    val Percent: ImageVector by lazy { futureGlyph("Percent", listOf("M18.4 5.6 5.6 18.4", "M7.6 5a2.4 2.4 0 1 0 0 4.8 2.4 2.4 0 0 0 0-4.8Z", "M16.4 14.2a2.4 2.4 0 1 0 0 4.8 2.4 2.4 0 0 0 0-4.8Z")) }
    val Numbers: ImageVector by lazy { futureGlyph("Numbers", listOf("M9.6 4 7.6 20", "M16.4 4l-2 16", "M4.6 9h15.2", "M3.8 15H19")) }
    val Straighten: ImageVector by lazy { futureGlyph("Straighten", listOf("M2.8 8.4h18.4v7.2H2.8Z", "M6.4 8.4v3", "M10 8.4v4.2", "M13.6 8.4v3", "M17.2 8.4v4.2")) }
    val Architecture: ImageVector by lazy { futureGlyph("Architecture", listOf("M12 3.4a1.8 1.8 0 1 0 0 3.6 1.8 1.8 0 0 0 0-3.6Z", "M11.2 6.8 5.6 20.4", "M12.8 6.8l5.6 13.6", "M7.2 16.2a9 9 0 0 0 9.6 0")) }
    val SwapVert: ImageVector by lazy { futureGlyph("SwapVert", listOf("M8.6 18.8V5.6", "M5.4 8.6l3.2-3.2 3.2 3.2", "M15.4 5.2v13.2", "M12.2 15.4l3.2 3.2 3.2-3.2")) }
    val QrCodeScanner: ImageVector by lazy { futureGlyph("QrCodeScanner", listOf("M4 8.4V6a2 2 0 0 1 2-2h2.4", "M15.6 4H18a2 2 0 0 1 2 2v2.4", "M20 15.6V18a2 2 0 0 1-2 2h-2.4", "M8.4 20H6a2 2 0 0 1-2-2v-2.4", "M7.6 7.6h3.2v3.2H7.6Z", "M13.2 7.6h3.2v3.2h-3.2Z", "M7.6 13.2h3.2v3.2H7.6Z", "M13.2 13.2h3.2v3.2h-3.2Z")) }
    val DocumentScanner: ImageVector by lazy { futureGlyph("DocumentScanner", listOf("M4 8.4V6a2 2 0 0 1 2-2h2.4", "M15.6 4H18a2 2 0 0 1 2 2v2.4", "M20 15.6V18a2 2 0 0 1-2 2h-2.4", "M8.4 20H6a2 2 0 0 1-2-2v-2.4", "M3.6 12h16.8", "M8 8h8", "M8 16h5")) }
    val Receipt: ImageVector by lazy { futureGlyph("Receipt", listOf("M5.6 3.6v17l2.1-1.4 2.1 1.4 2.2-1.4 2.2 1.4 2.1-1.4 2.1 1.4v-17l-2.1 1.4-2.1-1.4-2.2 1.4-2.2-1.4-2.1 1.4Z", "M8.8 9h6.4", "M8.8 12.4h6.4", "M8.8 15.8h4")) }
    val Casino: ImageVector by lazy { futureGlyph("Casino", listOf("M4.4 6.4a2 2 0 0 1 2-2h11.2a2 2 0 0 1 2 2v11.2a2 2 0 0 1-2 2H6.4a2 2 0 0 1-2-2Z", "d:8.4 8.4", "d:15.6 8.4", "d:12 12", "d:8.4 15.6", "d:15.6 15.6")) }
    val VpnKey: ImageVector by lazy { futureGlyph("VpnKey", listOf("M7.4 8.2a3.8 3.8 0 1 0 0 7.6 3.8 3.8 0 0 0 0-7.6Z", "M11.2 12h9.4v3", "M17.4 12v2.4")) }
    val LocalCafe: ImageVector by lazy { futureGlyph("LocalCafe", listOf("M4.6 8.4h11.2v6.4a4 4 0 0 1-4 4H8.6a4 4 0 0 1-4-4Z", "M15.8 9.6h1.8a2.2 2.2 0 0 1 0 4.4h-1.8", "M3.6 21h14", "M8 3.4v2.4", "M11.6 3.4v2.4")) }
    val Checklist: ImageVector by lazy { futureGlyph("Checklist", listOf("M3.6 6.6l1.6 1.6L8 5.2", "M3.6 14.6l1.6 1.6L8 13.2", "M11.4 6.8h9", "M11.4 14.8h9")) }
    val RadioButtonChecked: ImageVector by lazy { futureGlyph("RadioButtonChecked", listOf("M12 4a8 8 0 1 0 0 16 8 8 0 0 0 0-16Z", "M12 8.6a3.4 3.4 0 1 0 0 6.8 3.4 3.4 0 0 0 0-6.8Z")) }
    val RadioButtonUnchecked: ImageVector by lazy { futureGlyph("RadioButtonUnchecked", listOf("M12 4a8 8 0 1 0 0 16 8 8 0 0 0 0-16Z")) }
    val Email: ImageVector by lazy { futureGlyph("Email", listOf("M3.4 7a1.8 1.8 0 0 1 1.8-1.8h13.6A1.8 1.8 0 0 1 20.6 7v10a1.8 1.8 0 0 1-1.8 1.8H5.2A1.8 1.8 0 0 1 3.4 17Z", "M3.8 7.2 12 13l8.2-5.8")) }
    val Business: ImageVector by lazy { futureGlyph("Business", listOf("M3.6 20.2V4.6H12v15.6", "M12 8.8h8.4v11.4", "M2.6 20.2h18.8", "M6.4 7.8h2.8", "M6.4 11.2h2.8", "M6.4 14.6h2.8", "M15 12h2.4", "M15 15.4h2.4")) }
    val Groups: ImageVector by lazy { futureGlyph("Groups", listOf("M12 5.4a2.8 2.8 0 1 0 0 5.6 2.8 2.8 0 0 0 0-5.6Z", "M6.6 18.6a5.4 5.4 0 0 1 10.8 0", "M5.6 8.2a2 2 0 1 0 0 4 2 2 0 0 0 0-4Z", "M18.4 8.2a2 2 0 1 0 0 4 2 2 0 0 0 0-4Z", "M2.4 17.4A3.6 3.6 0 0 1 6 14.2", "M21.6 17.4a3.6 3.6 0 0 0-3.6-3.2")) }
    val ContactPhone: ImageVector by lazy { futureGlyph("ContactPhone", listOf("M2.8 6.4a1.8 1.8 0 0 1 1.8-1.8h14.8a1.8 1.8 0 0 1 1.8 1.8v11.2a1.8 1.8 0 0 1-1.8 1.8H4.6a1.8 1.8 0 0 1-1.8-1.8Z", "M8.6 8.4a2 2 0 1 0 0 4 2 2 0 0 0 0-4Z", "M5.4 16.4a3.4 3.4 0 0 1 6.4 0", "M14.4 9h3.8", "M14.4 12h3.8", "M14.4 15h2.4")) }
    val Face: ImageVector by lazy { futureGlyph("Face", listOf("M12 4a8 8 0 1 0 0 16 8 8 0 0 0 0-16Z", "d:9.2 10.6", "d:14.8 10.6", "M9 14.4a4 4 0 0 0 6 0")) }
    val Mouse: ImageVector by lazy { futureGlyph("Mouse", listOf("M6.4 9.4a5.6 5.6 0 0 1 11.2 0v5.2a5.6 5.6 0 0 1-11.2 0Z", "M12 3.8v4.6")) }
    val Hub: ImageVector by lazy { futureGlyph("Hub", listOf("M12 9.6a2.4 2.4 0 1 0 0 4.8 2.4 2.4 0 0 0 0-4.8Z", "M12 3.4a1.8 1.8 0 1 0 0 3.6 1.8 1.8 0 0 0 0-3.6Z", "M4.8 15.6a1.8 1.8 0 1 0 0 3.6 1.8 1.8 0 0 0 0-3.6Z", "M19.2 15.6a1.8 1.8 0 1 0 0 3.6 1.8 1.8 0 0 0 0-3.6Z", "M12 7v2.6", "M10 13.4l-3.8 2.8", "M14 13.4l3.8 2.8")) }
    val Bolt: ImageVector by lazy { futureGlyph("Bolt", listOf("M13 3.4 6.6 13.2h5.2L11 20.6l6.4-9.8h-5.2Z")) }
    val Restaurant: ImageVector by lazy { futureGlyph("Restaurant", listOf("M6.4 3.6v5.6a2 2 0 0 0 4 0V3.6", "M8.4 3.6v16.8", "M17.4 20.4V3.6c-2.2 1-3.4 3.6-3.4 7.6h3.4")) }
    val Storefront: ImageVector by lazy { futureGlyph("Storefront", listOf("M4.2 8.4l1.4-4h12.8l1.4 4v1a2.6 2.6 0 0 1-5.2 0 2.6 2.6 0 0 1-5.2 0 2.6 2.6 0 0 1-5.2 0Z", "M5.4 12v7.6h13.2V12", "M10 19.6v-4.4h4v4.4")) }
    val SportsEsports: ImageVector by lazy { futureGlyph("SportsEsports", listOf("M7.4 6.8h9.2a4 4 0 0 1 3.9 3.2l1 5.2a2.4 2.4 0 0 1-4.2 2l-2-2.4H8.7l-2 2.4a2.4 2.4 0 0 1-4.2-2l1-5.2a4 4 0 0 1 3.9-3.2Z", "M8 9.6v3.2", "M6.4 11.2h3.2", "d:15.6 10.2", "d:17.4 12.2")) }
    val Celebration: ImageVector by lazy { futureGlyph("Celebration", listOf("M4 20l4.6-12.4 7.8 7.8Z", "M13.6 4.4c.8 1.6.4 3-1 4", "M19.6 10.4c-1.6-.8-3-.4-4 1", "d:17 5", "d:20 7.4", "d:10.4 3.6")) }
    val CleaningServices: ImageVector by lazy { futureGlyph("CleaningServices", listOf("M10.6 3.4h2.8v7.2h-2.8Z", "M5.6 10.6h12.8l1.2 10H4.4Z", "M9 20.6v-3.4", "M12 20.6v-3.4", "M15 20.6v-3.4")) }
    val Gavel: ImageVector by lazy { futureGlyph("Gavel", listOf("M8.2 7.4l4.4-4.4 5.2 5.2-4.4 4.4Z", "M10.6 10.4l-6 6a1.4 1.4 0 0 0 2 2l6-6", "M12.4 20.4h8")) }
    val HealthAndSafety: ImageVector by lazy { futureGlyph("HealthAndSafety", listOf("M12 3.6 5 6.4v5.2c0 4.2 3 7.6 7 8.8 4-1.2 7-4.6 7-8.8V6.4Z", "M12 9v5.6", "M9.2 11.8h5.6")) }
    val Troubleshoot: ImageVector by lazy { futureGlyph("Troubleshoot", listOf("M10 3.8a6.2 6.2 0 1 0 0 12.4 6.2 6.2 0 0 0 0-12.4Z", "M14.4 14.4l5.8 5.8", "M6 10.4h1.8L9 8.2l1.8 4 1.2-1.8H14")) }
    val SettingsRemote: ImageVector by lazy { futureGlyph("SettingsRemote", listOf("M8.4 7.6h7.2a1 1 0 0 1 1 1v11.8H7.4V8.6a1 1 0 0 1 1-1Z", "M12 10.2a1.4 1.4 0 1 0 0 2.8 1.4 1.4 0 0 0 0-2.8Z", "M9 4.6a4.4 4.4 0 0 1 6 0", "M6.6 2.4a7.6 7.6 0 0 1 10.8 0")) }
    val Air: ImageVector by lazy { futureGlyph("Air", listOf("M3.4 8.8h10.4a2.6 2.6 0 1 0-2.6-2.6", "M3.4 12.4h15a2.6 2.6 0 1 1-2.6 2.6", "M3.4 16h7")) }
    val FiberManualRecord: ImageVector by lazy { futureGlyph("FiberManualRecord", listOf("M12 6.4a5.6 5.6 0 1 0 0 11.2 5.6 5.6 0 0 0 0-11.2Z"), solid = true) }

    /** הגרסאות שמתהפכות ב-RTL, במקום Icons.AutoMirrored.Rounded.* */
    object AutoMirrored {
        val ArrowBack: ImageVector by lazy { futureGlyph("AutoMirrored.ArrowBack", listOf("M19 12H5.4", "M11 5.6 4.6 12l6.4 6.4"), autoMirror = true) }
        val ArrowForward: ImageVector by lazy { futureGlyph("AutoMirrored.ArrowForward", listOf("M5 12h13.6", "M13 5.6 19.4 12 13 18.4"), autoMirror = true) }
        val KeyboardArrowLeft: ImageVector by lazy { futureGlyph("AutoMirrored.KeyboardArrowLeft", listOf("M14.6 5.8 8.4 12l6.2 6.2"), autoMirror = true) }
        val KeyboardArrowRight: ImageVector by lazy { futureGlyph("AutoMirrored.KeyboardArrowRight", listOf("M9.4 5.8 15.6 12l-6.2 6.2"), autoMirror = true) }
        val Help: ImageVector by lazy { futureGlyph("AutoMirrored.Help", listOf("M12 4a8 8 0 1 0 0 16 8 8 0 0 0 0-16Z", "M9.5 9.7a2.6 2.6 0 1 1 3 2.6v1.4", "d:12 17"), autoMirror = true) }
        val CallReceived: ImageVector by lazy { futureGlyph("AutoMirrored.CallReceived", listOf("M18.8 5.2 8.4 15.6", "M8.4 9.6v6h6"), autoMirror = true) }
        val CallMade: ImageVector by lazy { futureGlyph("AutoMirrored.CallMade", listOf("M5.2 18.8 15.6 8.4", "M15.6 14.4v-6h-6"), autoMirror = true) }
        val CallMissed: ImageVector by lazy { futureGlyph("AutoMirrored.CallMissed", listOf("M19.6 7 12 14.6 4.4 7", "M9.4 7H4.4v5"), autoMirror = true) }
        val VolumeUp: ImageVector by lazy { futureGlyph("AutoMirrored.VolumeUp", listOf("M4 9.4h3.4L12 5.6v12.8L7.4 14.6H4Z", "M15.3 9.4a3.7 3.7 0 0 1 0 5.2", "M17.9 6.8a7.4 7.4 0 0 1 0 10.4"), autoMirror = true) }
        val VolumeOff: ImageVector by lazy { futureGlyph("AutoMirrored.VolumeOff", listOf("M4 9.4h3.4L12 5.6v12.8L7.4 14.6H4Z", "M15.6 9.6l4.8 4.8", "M20.4 9.6l-4.8 4.8"), autoMirror = true) }
        val Chat: ImageVector by lazy { futureGlyph("AutoMirrored.Chat", listOf("M4.4 6.9A2.4 2.4 0 0 1 6.8 4.5h10.4a2.4 2.4 0 0 1 2.4 2.4v7a2.4 2.4 0 0 1-2.4 2.4h-6.1l-4.5 3.4v-3.4A2.4 2.4 0 0 1 4.4 13.9Z"), autoMirror = true) }
        val TrendingUp: ImageVector by lazy { futureGlyph("AutoMirrored.TrendingUp", listOf("M4.2 16.2 9.2 11.2l3.4 3.4L19.8 7.4", "M14.6 7.4h5.2v5.2"), autoMirror = true) }
        val Send: ImageVector by lazy { futureGlyph("AutoMirrored.Send", listOf("M20.4 4 3.6 12l16.8 8-3.2-8Z", "M17.2 12H6.4"), autoMirror = true) }
        val Sort: ImageVector by lazy { futureGlyph("AutoMirrored.Sort", listOf("M4 7h16", "M4 12h11", "M4 17h6"), autoMirror = true) }
        val ViewList: ImageVector by lazy { futureGlyph("AutoMirrored.ViewList", listOf("M8.4 6.6h11.2", "M8.4 12h11.2", "M8.4 17.4h11.2", "d:4.8 6.6", "d:4.8 12", "d:4.8 17.4"), autoMirror = true) }
        val QueueMusic: ImageVector by lazy { futureGlyph("AutoMirrored.QueueMusic", listOf("M4 6.4h11", "M4 11h11", "M4 15.6h6", "M18.6 6v10.6", "M18.6 6H21", "M16.4 14.4a2.2 2.2 0 1 0 0 4.4 2.2 2.2 0 0 0 0-4.4Z"), autoMirror = true) }
        val PlaylistAdd: ImageVector by lazy { futureGlyph("AutoMirrored.PlaylistAdd", listOf("M4 6.4h11", "M4 11h11", "M4 15.6h7", "M17.6 12.4v7.2", "M14 16h7.2"), autoMirror = true) }
        val VolumeDown: ImageVector by lazy { futureGlyph("AutoMirrored.VolumeDown", listOf("M5.4 9.4h3.4l4.6-3.8v12.8l-4.6-3.8H5.4Z", "M16.7 9.4a3.7 3.7 0 0 1 0 5.2"), autoMirror = true) }
        val MenuBook: ImageVector by lazy { futureGlyph("AutoMirrored.MenuBook", listOf("M12 6.8c-2-1.6-4.8-2-8-1.6v12.6c3.2-.4 6 0 8 1.6 2-1.6 4.8-2 8-1.6V5.2c-3.2-.4-6 0-8 1.6Z", "M12 6.8v12.6"), autoMirror = true) }
        val LibraryBooks: ImageVector by lazy { futureGlyph("AutoMirrored.LibraryBooks", listOf("M7.6 3.8h11a1.6 1.6 0 0 1 1.6 1.6v11a1.6 1.6 0 0 1-1.6 1.6h-11A1.6 1.6 0 0 1 6 16.4v-11a1.6 1.6 0 0 1 1.6-1.6Z", "M3.4 7.4v11.4a1.8 1.8 0 0 0 1.8 1.8h11.4", "M9.6 8H16", "M9.6 11H16", "M9.6 14h4"), autoMirror = true) }
        val DirectionsRun: ImageVector by lazy { futureGlyph("AutoMirrored.DirectionsRun", listOf("M15.2 3.4a1.6 1.6 0 1 0 0 3.2 1.6 1.6 0 0 0 0-3.2Z", "M6.4 9.6l3.4-2.2h3.6l2 3.4 3 1", "M12.6 7.4 10 13.4l3.6 2.4-1 4.6", "M10 13.4 8 17H4.4"), autoMirror = true) }
        val DirectionsWalk: ImageVector by lazy { futureGlyph("AutoMirrored.DirectionsWalk", listOf("M13.2 3.4a1.6 1.6 0 1 0 0 3.2 1.6 1.6 0 0 0 0-3.2Z", "M12.4 8.4 10.6 14l2.8 2.6.6 4", "M10.6 14l-1.8 6.4", "M12.4 8.4l-3.4 2v3", "M12.4 8.4l1.6 3 2.6 1"), autoMirror = true) }
    }
}
