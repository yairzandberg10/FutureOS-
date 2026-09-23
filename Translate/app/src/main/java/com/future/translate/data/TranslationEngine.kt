package com.future.translate.data

import com.google.android.gms.tasks.Task
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.languageid.LanguageIdentificationOptions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.TranslateRemoteModel
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * תרגום על המכשיר עם ML Kit. לכל שפה יש מודל (כ-30MB) שיורד פעם אחת; משם
 * התרגום עובד גם בלי רשת. אנגלית היא שפת הציר של המנוע, ולכן היא חלק מכל
 * זוג שפות.
 */
class TranslationEngine {

    private val models = RemoteModelManager.getInstance()
    private val identifier = LanguageIdentification.getClient(
        LanguageIdentificationOptions.Builder().setConfidenceThreshold(0.4f).build()
    )
    private val translators = mutableMapOf<String, Translator>()

    /** מה המשתמש צריך לדעת בזמן שהתרגום רץ. */
    enum class Phase { Downloading, Translating }

    /** קוד השפה של [text], או null כשלא זוהתה שפה שאפשר לתרגם ממנה. */
    suspend fun identify(text: String): String? {
        val tag = identifier.identifyLanguage(text).await()
        if (tag == "und") return null
        return Languages.normalize(tag)
    }

    suspend fun downloadedLanguages(): Set<String> =
        models.getDownloadedModels(TranslateRemoteModel::class.java).await()
            .map { it.language }
            .toSet()

    suspend fun download(code: String, wifiOnly: Boolean) {
        models.download(model(code), conditions(wifiOnly)).await()
    }

    suspend fun delete(code: String) {
        models.deleteDownloadedModel(model(code)).await()
    }

    /**
     * מתרגם [text]. אם אחד המודלים חסר הוא יורד קודם, ו-[onPhase] מודיע על
     * כך כדי שהמסך יגיד "מוריד את השפה" ולא רק "מתרגם".
     */
    suspend fun translate(
        text: String,
        from: String,
        to: String,
        wifiOnly: Boolean,
        onPhase: (Phase) -> Unit,
    ): String {
        if (from == to) return text
        val translator = translatorFor(from, to)
        val downloaded = downloadedLanguages()
        if (from !in downloaded || to !in downloaded) {
            onPhase(Phase.Downloading)
            translator.downloadModelIfNeeded(conditions(wifiOnly)).await()
        }
        onPhase(Phase.Translating)
        return translator.translate(text).await()
    }

    fun close() {
        translators.values.forEach { it.close() }
        translators.clear()
        identifier.close()
    }

    private fun translatorFor(from: String, to: String): Translator {
        val key = "$from>$to"
        return translators.getOrPut(key) {
            Translation.getClient(
                TranslatorOptions.Builder()
                    .setSourceLanguage(requireNotNull(TranslateLanguage.fromLanguageTag(from)))
                    .setTargetLanguage(requireNotNull(TranslateLanguage.fromLanguageTag(to)))
                    .build()
            )
        }
    }

    private fun model(code: String): TranslateRemoteModel =
        TranslateRemoteModel.Builder(requireNotNull(TranslateLanguage.fromLanguageTag(code))).build()

    private fun conditions(wifiOnly: Boolean): DownloadConditions =
        DownloadConditions.Builder().apply { if (wifiOnly) requireWifi() }.build()
}

/** Task של Play Services כ-suspend, בלי תלות נוספת רק בשביל זה. */
private suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { cont ->
    addOnSuccessListener { cont.resume(it) }
    addOnFailureListener { cont.resumeWithException(it) }
    addOnCanceledListener { cont.cancel() }
}
