package com.future.tools.ui

sealed class ToolRoute {
    object Home : ToolRoute()
    object Calculator : ToolRoute()
    object Flashlight : ToolRoute()
    object Stopwatch : ToolRoute()
    object Timer : ToolRoute()
    object UnitConverter : ToolRoute()
    object Compass : ToolRoute()
    object Level : ToolRoute()

    // מדידה וחיישנים
    object NoiseMeter : ToolRoute()
    object LuxMeter : ToolRoute()
    object AngleRuler : ToolRoute()

    // מחשבונים וממירים
    object TipSplitCalculator : ToolRoute()
    object QuickFinanceCalculator : ToolRoute()
    object TimeZoneConverter : ToolRoute()

    // פרודוקטיביות
    object QrScanner : ToolRoute()
    object Pomodoro : ToolRoute()
    object PasswordGenerator : ToolRoute()
    object QuickNotes : ToolRoute()

    // כלי עזר אקראיים ופנאי
    object CoinDice : ToolRoute()
    object RandomPicker : ToolRoute()
    object RandomNumber : ToolRoute()

    // שדרוגי AI
    object TextScanner : ToolRoute()
    object VoiceTranscribe : ToolRoute()
}
