package com.future.remote.data

/** מותגי מזגנים עם שלט מוכן מראש - לא צריך למצוא/להזין קודים בעצמך. */
enum class AcBrand(val label: String) {
    ELECTRA("אלקטרה (וגם AUX / Frigidaire / Centek / AEG / Electrolux / Delonghi)")
}

object AcPresets {
    private fun rawButton(label: String, pattern: IntArray): RemoteButton {
        return RemoteButton(
            label = label,
            encoding = ButtonEncoding.RAW,
            rawFrequencyHz = ElectraEncoder.CARRIER_FREQUENCY_HZ,
            rawPattern = pattern.joinToString(",")
        )
    }

    fun buildDevice(brand: AcBrand, name: String): RemoteDevice {
        val buttons = when (brand) {
            AcBrand.ELECTRA -> buildElectraButtons()
        }
        return RemoteDevice(name = name, category = DeviceCategory.AC, buttons = buttons)
    }

    private fun buildElectraButtons(): List<RemoteButton> {
        val buttons = mutableListOf<RemoteButton>()
        buttons.add(rawButton("כבה", ElectraEncoder.encode(power = false, mode = ElectraEncoder.Mode.COOL, fan = ElectraEncoder.FanSpeed.AUTO, tempCelsius = 24)))
        for (temp in listOf(18, 20, 22, 24, 26, 28)) {
            buttons.add(rawButton("קירור $temp°", ElectraEncoder.encode(power = true, mode = ElectraEncoder.Mode.COOL, fan = ElectraEncoder.FanSpeed.AUTO, tempCelsius = temp)))
        }
        for (temp in listOf(20, 22, 24)) {
            buttons.add(rawButton("חימום $temp°", ElectraEncoder.encode(power = true, mode = ElectraEncoder.Mode.HEAT, fan = ElectraEncoder.FanSpeed.AUTO, tempCelsius = temp)))
        }
        buttons.add(rawButton("מאוורר בלבד", ElectraEncoder.encode(power = true, mode = ElectraEncoder.Mode.FAN, fan = ElectraEncoder.FanSpeed.AUTO, tempCelsius = 24)))
        buttons.add(rawButton("אוטומטי", ElectraEncoder.encode(power = true, mode = ElectraEncoder.Mode.AUTO, fan = ElectraEncoder.FanSpeed.AUTO, tempCelsius = 24)))
        return buttons
    }
}
