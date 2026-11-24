package com.future.assistant.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WakeWordTest {
    @Test
    fun wakePhrases() {
        assertEquals("", WakeWord.match("היי פיוטר"))
        assertEquals("", WakeWord.match("הי, פיוצ'ר!"))
        assertEquals("", WakeWord.match("פיוטר"))
        assertEquals("", WakeWord.match("עוזר"))
        assertEquals("", WakeWord.match("היי עוזר"))
        assertEquals("", WakeWord.match("Hey Future"))
        assertEquals("", WakeWord.match("היי פיטר"))
        assertEquals("מה השעה", WakeWord.match("היי פיוטר, מה השעה?"))
        assertEquals("תפתח את המצלמה", WakeWord.match("עוזר תפתח את המצלמה"))
        // שיבושים אמיתיים מהמכשיר
        assertEquals("", WakeWord.match("היף יוצא."))
        assertEquals("", WakeWord.match("הייפ יוטר"))
        assertEquals("מה נשמע", WakeWord.match("היפיוטר מה נשמע"))
    }

    @Test
    fun ordinarySpeechDoesNotWake() {
        assertNull(WakeWord.match("זה עוזר לי מאוד"))
        assertNull(WakeWord.match("עוזר לי"))
        assertNull(WakeWord.match("פיטר הלך הביתה"))
        assertNull(WakeWord.match("מה השעה"))
        assertNull(WakeWord.match("הוא עוזר"))
        assertNull(WakeWord.match("היום יוצא לטיול"))
        assertNull(WakeWord.match("היא יוצאת"))
        assertNull(WakeWord.match("היפה והחיה"))
        assertNull(WakeWord.match(""))
    }
}
