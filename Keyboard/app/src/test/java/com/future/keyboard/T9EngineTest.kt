package com.future.keyboard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class T9EngineTest {

    @Test
    fun `digitsFor converts an English word to its key sequence`() {
        // t=8 (tuv), h=4 (ghi), e=3 (def)
        assertEquals("843", T9Engine.digitsFor("the", T9Engine.Language.ENGLISH))
    }

    @Test
    fun `digitsFor converts a Hebrew word to its key sequence`() {
        // נ=4, כ=5, ו=2, ן=4 (final nun shares a key with the regular letter)
        assertEquals("4524", T9Engine.digitsFor("נכון", T9Engine.Language.HEBREW))
    }

    @Test
    fun `digitsFor returns empty string for a character with no mapped key`() {
        assertEquals("", T9Engine.digitsFor("hi!", T9Engine.Language.ENGLISH))
    }

    @Test
    fun `lettersFor returns the letters printed on a given digit key`() {
        val engine = T9Engine(T9Engine.Language.ENGLISH)
        assertEquals("ghi", engine.lettersFor('4'))
        assertEquals("", engine.lettersFor('1'))
    }

    @Test
    fun `candidatesFor returns the most common dictionary word first for a digit sequence`() {
        val engine = T9Engine(T9Engine.Language.ENGLISH)
        val candidates = engine.candidatesFor("843")
        assertTrue("expected 'the' among candidates for 843, got $candidates", candidates.isNotEmpty())
        assertEquals("the", candidates.first())
    }

    @Test
    fun `candidatesFor returns an empty list for an unmatched digit sequence`() {
        val engine = T9Engine(T9Engine.Language.ENGLISH)
        assertEquals(emptyList<String>(), engine.candidatesFor("99999999"))
    }

    @Test
    fun `candidatesFor returns an empty list for empty input`() {
        val engine = T9Engine(T9Engine.Language.ENGLISH)
        assertEquals(emptyList<String>(), engine.candidatesFor(""))
    }

    @Test
    fun `candidatesFor never returns duplicate words for a Hebrew digit sequence with a repeated dictionary entry`() {
        // "נכון" (correct) is listed twice in the raw Hebrew word list (regression
        // guard for the duplicate-dictionary-entry bug - see T9Engine's .distinct()).
        val engine = T9Engine(T9Engine.Language.HEBREW)
        val candidates = engine.candidatesFor("4524")
        assertEquals(1, candidates.count { it == "נכון" })
    }

    @Test
    fun `candidatesFor never returns duplicate words for another repeated Hebrew dictionary entry`() {
        // "שם" (there/name) is also listed twice in the raw Hebrew word list.
        val engine = T9Engine(T9Engine.Language.HEBREW)
        val candidates = engine.candidatesFor("74")
        assertEquals(1, candidates.count { it == "שם" })
    }

    @Test
    fun `candidatesFor orders words by dictionary popularity without frequency data`() {
        val engine = T9Engine(T9Engine.Language.ENGLISH)
        // "of" (m/n/o + d/e/f = 63) ranks earlier in the dictionary than "me" (also 63).
        assertEquals(listOf("of", "me"), engine.candidatesFor("63"))
    }

    @Test
    fun `candidatesFor orders words by learned frequency when provided`() {
        val engine = T9Engine(T9Engine.Language.ENGLISH)
        // "me" ranks after "of" in the dictionary, but a high learned frequency
        // should move it ahead - the whole point of the wordFrequency parameter.
        val reordered = engine.candidatesFor("63") { word -> if (word == "me") 100 else 0 }
        assertEquals(listOf("me", "of"), reordered)
    }
}
