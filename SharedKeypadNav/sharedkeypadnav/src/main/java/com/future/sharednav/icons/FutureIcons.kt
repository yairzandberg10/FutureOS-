package com.future.sharednav.icons

import androidx.compose.ui.graphics.vector.ImageVector

/**
 * "Future Glyphs" - סט האייקונים של ה-Design System
 * (design/FutureOS Design System/components/core/FosIcon.jsx), מומר ל-ImageVector.
 * רשת 24, קו 1.6, קצוות ומפרקים מעוגלים. השמות זהים לשמות Material Symbols
 * שהיו בשימוש (Icons.Rounded.Home -> FutureIcons.Home), כך ששם שלא צויר בסט
 * נשאר ב-Icons.Rounded ולא נשבר שום מסך.
 *
 * הקובץ נוצר אוטומטית מ-FosIcon.jsx - לשינוי אייקון יש לעדכן את ה-DS ולהריץ
 * מחדש את המחולל, לא לערוך כאן ידנית.
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
    val Bookmark: ImageVector by lazy { futureGlyph("Bookmark", listOf("M6.6 4.6h10.8v15.2L12 15.8l-5.4 4Z"), solid = true) }
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
    }
}
