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

    /**
     * שולח ומחזיר אם השליחה הצליחה. חוסם עד סוף השידור (עשרות עד מאות ms) -
     * לקרוא מחוץ ל-main thread. בלי משדר מחזיר false (קודם: `manager?.transmit`
     * החזיר null בשקט והפונקציה דיווחה הצלחה).
     */
    fun transmit(carrierFrequencyHz: Int, pattern: IntArray): Boolean {
        val m = manager ?: return false
        return try {
            m.transmit(carrierFrequencyHz, pattern)
            true
        } catch (e: Exception) {
            false
        }
    }
}
