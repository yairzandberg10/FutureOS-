package com.future.remote.data

import android.content.Context
import android.hardware.ConsumerIrManager

/** עטיפה דקה סביב ConsumerIrManager - לא קורסת במכשירים בלי משדר אינפרא אדום. */
class IrTransmitter(context: Context) {
    private val manager = context.getSystemService(Context.CONSUMER_IR_SERVICE) as? ConsumerIrManager

    val isAvailable: Boolean
        get() = try {
            manager?.hasIrEmitter() == true
        } catch (e: Exception) {
            false
        }

    fun transmit(carrierFrequencyHz: Int, pattern: IntArray): Boolean {
        return try {
            manager?.transmit(carrierFrequencyHz, pattern)
            true
        } catch (e: Exception) {
            false
        }
    }
}
