package com.future.assistant.asr

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import org.json.JSONObject
import java.nio.LongBuffer
import java.text.Normalizer

/**
 * ReNikud: המרת טקסט עברי בלי ניקוד להגייה (IPA), עם הטעמה. זה מה שחסר
 * ל-Piper: קודם eSpeak קבע את ההגייה, והוא לא משחזר תנועות מטקסט בלי
 * ניקוד, ולכן מילים נשמעו משובשות. המודל מסווג כל אות לשלישייה
 * (עיצור, תנועה, הטעמה) לפי ההקשר.
 *
 * העברה ישירה של renikud_onnx (github.com/thewh1teagle/renikud, CC-BY-4.0).
 * כל המילונים (אוצר מילים, מחלקות, אילוצי עיצורים) שמורים במטא-דאטה של
 * קובץ ה-ONNX עצמו.
 */
class HebrewG2P(env: OrtEnvironment, modelPath: String, options: OrtSession.SessionOptions) {
    private val env = env
    private val session: OrtSession = env.createSession(modelPath, options)
    private val vocab: Map<String, Int>
    private val consonantVocab: Map<Int, String>
    private val vowelVocab: Map<Int, String>
    private val clsId: Long
    private val sepId: Long
    private val letterConstraints: Map<String, IntArray>
    private val gereshMap: Map<String, String>

    init {
        val meta = session.metadata.customMetadata
        vocab = JSONObject(meta.getValue("vocab")).toMap { it as Int }
        consonantVocab = JSONObject(meta.getValue("consonant_vocab")).toMap { it as String }.mapKeys { it.key.toInt() }
        vowelVocab = JSONObject(meta.getValue("vowel_vocab")).toMap { it as String }.mapKeys { it.key.toInt() }
        clsId = meta.getValue("cls_token_id").toLong()
        sepId = meta.getValue("sep_token_id").toLong()
        letterConstraints = JSONObject(meta.getValue("letter_consonant_constraints")).toMap { v ->
            val arr = v as org.json.JSONArray
            IntArray(arr.length()) { arr.getInt(it) }
        }
        gereshMap = meta["geresh_map"]?.let { json -> JSONObject(json).toMap { it as String } } ?: emptyMap()
    }

    fun phonemize(input: String): String {
        val text = Normalizer.normalize(normalizeGraphemes(input), Normalizer.Form.NFD)
        // תו לכל טוקן, בנקודות קוד (כמו האיטרציה של Python על מחרוזת).
        val chars = text.codePoints().toArray().map { String(Character.toChars(it)) }
        if (chars.isEmpty()) return ""
        val unkId = (vocab["[UNK]"] ?: 0).toLong()
        val seqLen = chars.size + 2
        val ids = LongArray(seqLen)
        ids[0] = clsId
        chars.forEachIndexed { i, c -> ids[i + 1] = (vocab[c]?.toLong() ?: unkId) }
        ids[seqLen - 1] = sepId
        val mask = LongArray(seqLen) { 1L }

        val shape = longArrayOf(1, seqLen.toLong())
        val consonantLogits: Array<FloatArray>
        val vowelLogits: Array<FloatArray>
        val stressLogits: Array<FloatArray>
        OnnxTensor.createTensor(env, LongBuffer.wrap(ids), shape).use { idsTensor ->
            OnnxTensor.createTensor(env, LongBuffer.wrap(mask), shape).use { maskTensor ->
                session.run(mapOf("input_ids" to idsTensor, "attention_mask" to maskTensor)).use { out ->
                    @Suppress("UNCHECKED_CAST")
                    fun logits(name: String) = (out.get(name).get().value as Array<Array<FloatArray>>)[0]
                    consonantLogits = logits("consonant_logits")
                    vowelLogits = logits("vowel_logits")
                    stressLogits = logits("stress_logits")
                }
            }
        }

        // טוקן i (בלי CLS) = תו i-1. בכל מילה ההטעמה הולכת לתו עם הציון
        // הגבוה ביותר - כולל סימני פיסוק שבתוך המילה, כמו במקור.
        val stressed = HashSet<Int>()
        var i = 0
        while (i < chars.size) {
            if (chars[i].isBlank()) { i++; continue }
            var best = i
            var j = i
            while (j < chars.size && !chars[j].isBlank()) {
                if (stressLogits[j + 1][1] > stressLogits[best + 1][1]) best = j
                j++
            }
            stressed.add(best)
            i = j
        }

        val result = StringBuilder()
        for ((idx, c) in chars.withIndex()) {
            if (!isHebrewLetter(c)) {
                if (c != "'" && c != "\"") result.append(c)
                continue
            }
            val tok = idx + 1
            val allowed = letterConstraints[c]
            var cid = argmax(consonantLogits[tok])
            if (allowed != null && cid !in allowed) cid = allowed.maxBy { consonantLogits[tok][it] }
            var consonant = consonantVocab[cid] ?: NONE
            if (c in gereshMap && idx + 1 < chars.size && chars[idx + 1] == "'") consonant = gereshMap.getValue(c)
            val vowel = vowelVocab[argmax(vowelLogits[tok])] ?: NONE
            val stress = idx in stressed

            val wordFinal = idx + 1 >= chars.size || !Character.isLetter(chars[idx + 1].codePointAt(0))
            if (c == "ח" && wordFinal && vowel == "a") {
                // פתח גנובה: התנועה נהגית לפני העיצור.
                if (stress) result.append(STRESS_MARK)
                result.append("aχ")
            } else {
                if (consonant != NONE) result.append(consonant)
                if (stress) result.append(STRESS_MARK)
                if (vowel != NONE) result.append(vowel)
            }
        }
        return result.toString()
    }

    fun close() = session.close()

    private companion object {
        const val NONE = "∅"
        const val STRESS_MARK = "ˈ"

        fun isHebrewLetter(c: String) = c.length == 1 && c[0] in 'א'..'ת'

        fun normalizeGraphemes(text: String) = text
            .replace(Regex("[׳'`´]"), "'")
            .replace(Regex("[״\"“”]"), "\"")

        fun argmax(v: FloatArray): Int {
            var best = 0
            for (k in 1 until v.size) if (v[k] > v[best]) best = k
            return best
        }

        fun <T> JSONObject.toMap(convert: (Any) -> T): Map<String, T> =
            keys().asSequence().associateWith { convert(get(it)) }
    }
}
