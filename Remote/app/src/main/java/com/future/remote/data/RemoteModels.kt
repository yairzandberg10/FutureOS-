package com.future.remote.data

import java.util.UUID

enum class DeviceCategory(val label: String) {
    AC("מזגן"),
    FAN("מאוורר"),
    AUDIO("מערכת שמע"),
    CUSTOM("מותאם אישית")
}

/** מצב הקידוד של כפתור: NEC (כתובת+פקודה בהקסדצימלי - הפורמט הכי נפוץ
 * שיצרנים מפרסמים לו קודים) או Raw (תבנית פולסים גולמית לכל פרוטוקול אחר). */
enum class ButtonEncoding { NEC, RAW }

data class RemoteButton(
    val id: String = UUID.randomUUID().toString(),
    val label: String,
    val encoding: ButtonEncoding,
    // NEC
    val necAddress: Int = 0,
    val necCommand: Int = 0,
    // RAW
    val rawFrequencyHz: Int = NecEncoder.CARRIER_FREQUENCY_HZ,
    val rawPattern: String = ""
) {
    fun toTransmission(): Pair<Int, IntArray>? {
        return when (encoding) {
            ButtonEncoding.NEC -> NecEncoder.CARRIER_FREQUENCY_HZ to NecEncoder.encode(necAddress, necCommand)
            ButtonEncoding.RAW -> {
                val values = rawPattern.split(",", " ")
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                    .mapNotNull { it.toIntOrNull() }
                if (values.isEmpty()) null else rawFrequencyHz to values.toIntArray()
            }
        }
    }
}

data class RemoteDevice(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val category: DeviceCategory,
    val buttons: List<RemoteButton> = emptyList()
)
