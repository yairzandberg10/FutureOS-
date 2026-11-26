package com.future.systemui.fui.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import org.json.JSONArray
import java.security.KeyStore
import java.security.MessageDigest
import javax.crypto.KeyGenerator
import javax.crypto.Mac
import javax.crypto.SecretKey

/**
 * המדיניות של ההגנה: מה נחסם אוטומטית, מה המשתמש אישר, ומה חיכה שם עוד לפני שההגנה הופעלה.
 *
 *  - חסימה אוטומטית (ברירת מחדל: פועלת): יכולת מסוכנת שאפליקציה זרה מקבלת מעכשיו נחסמת
 *    מיד, והמשתמש מקבל התראה. ביטול בלחיצה אחת במרכז האבטחה בהגדרות.
 *  - נקודת פתיחה (baseline): מה שכבר היה פעיל כשההגנה הופעלה לראשונה לא נחסם לבד.
 *  - נוזקה ידועה: האפליקציה מושהית (גם אם הייתה שם מההתחלה) וכל היכולות שלה נחסמות.
 *  - שילוב חשוד חדש (למשל אפליקציה שהותקנה ממקור לא מוכר, מוסתרת ושולטת במסך): מושהית
 *    לפני ההפעלה הראשונה, עד שהמשתמש מאשר.
 *  - מאושר (allowed): המשתמש החליט שזה בסדר. לא נחסם, לא מתריע ולא מוריד את רמת האבטחה.
 *
 * הגנה מפני שינוי (anti-tampering): כל המצב חתום ב-HMAC במפתח של Android Keystore שלא יוצא
 * מהמכשיר. מי שעורך את קובץ ההעדפות מבחוץ (למשל תהליך root במכשיר ה-QA) לא יכול לחשב חתימה
 * תקינה. חתימה שלא מתאימה = האישורים ונקודת הפתיחה נמחקים (fail closed), נרשם אירוע ביומן
 * והמשתמש מקבל התראה. ב-AOSP המצב יעבור לשירות מערכת - ראו docs/security-protection.md.
 *
 * כל הקריאה והכתיבה תחת נעילה אחת, כי גם השומר ברקע וגם ההגדרות (דרך הספק) משנים את המצב.
 */
object ThreatGuard {
    private const val TAG = "ThreatGuard"
    private const val PREFS = "threat_watch"
    private const val KEY_SEEN = "seen"
    private const val KEY_AUTO = "auto_block"
    private const val KEY_BASELINE_READY = "baseline_ready_v2"
    private const val KEY_BASELINE = "baseline"
    private const val KEY_BASELINE_APPOPS = "baseline_appops_v2"
    private const val KEY_ALLOWED = "allowed"
    private const val KEY_BLOCKED = "blocked_json"
    private const val KEY_MAC = "state_mac"
    private const val MAC_ALIAS = "futureos_threat_state_v1"

    private val lock = Any()

    const val STATUS_OPEN = "open"
    const val STATUS_ALLOWED = "allowed"
    const val STATUS_BLOCKED = "blocked"

    private val APP_OPS_KINDS = setOf(ThreatScanner.Kind.OVERLAY, ThreatScanner.Kind.INSTALLER)

    data class State(
        val seen: Set<String> = emptySet(),
        val baseline: Set<String> = emptySet(),
        val baselineReady: Boolean = false,
        val baselineAppOps: Boolean = false,
        val allowed: Set<String> = emptySet(),
        val blocked: Map<String, ThreatBlocker.Entry> = emptyMap(),
        val autoBlock: Boolean = true,
    ) {
        fun status(key: String): String = when (key) {
            in blocked -> STATUS_BLOCKED
            in allowed -> STATUS_ALLOWED
            else -> STATUS_OPEN
        }
    }

    data class Outcome(
        /** ממצאים חדשים שהמשתמש לא אישר (כולל אלה שנחסמו). */
        val fresh: List<ThreatScanner.Finding>,
        val blocked: List<ThreatScanner.Finding>,
        /** ההפעלה הראשונה: מה שכבר היה פעיל, מחכה להחלטה. */
        val baseline: List<ThreatScanner.Finding>,
        /** המצב השמור שונה מבחוץ (נמחק ונבנה מחדש). */
        val tampered: Boolean = false,
    )

    private fun prefs(context: Context) = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    // ---- מצב חתום ----

    @Volatile private var lastTamper = false

    /** קורא ובודק חתימה. מצב ששונה מבחוץ מתאפס (בלי האישורים ובלי נקודת הפתיחה). */
    private fun load(context: Context): State {
        val p = prefs(context)
        val state = State(
            seen = p.getStringSet(KEY_SEEN, emptySet()).orEmpty().toSet(),
            baseline = p.getStringSet(KEY_BASELINE, emptySet()).orEmpty().toSet(),
            baselineReady = p.getBoolean(KEY_BASELINE_READY, false),
            baselineAppOps = p.getBoolean(KEY_BASELINE_APPOPS, false),
            allowed = p.getStringSet(KEY_ALLOWED, emptySet()).orEmpty().toSet(),
            blocked = parseBlocked(p.getString(KEY_BLOCKED, "[]")),
            autoBlock = p.getBoolean(KEY_AUTO, true),
        )
        val stored = p.getString(KEY_MAC, null)
        val expected = mac(state) ?: return state // אין Keystore: לא חוסמים את ההגנה עצמה
        if (stored == null) {
            // שדרוג מהגרסה שלפני החתימה: נחתם פעם אחת כמו שהוא
            save(context, state)
            return state
        }
        if (MessageDigest.isEqual(stored.toByteArray(), expected.toByteArray())) return state
        Log.w(TAG, "protection state was modified outside FutureUI - resetting decisions")
        lastTamper = true
        // את החסימות משאירים (בלעדיהן אי אפשר להחזיר), ושחרור עדיין בודק כל רכיב מחדש
        val reset = State(blocked = state.blocked, autoBlock = true)
        save(context, reset)
        SecurityAudit.record(context, SecurityAudit.Type.TAMPER)
        return reset
    }

    private fun save(context: Context, s: State) {
        prefs(context).edit()
            .putStringSet(KEY_SEEN, s.seen)
            .putStringSet(KEY_BASELINE, s.baseline)
            .putBoolean(KEY_BASELINE_READY, s.baselineReady)
            .putBoolean(KEY_BASELINE_APPOPS, s.baselineAppOps)
            .putStringSet(KEY_ALLOWED, s.allowed)
            .putString(KEY_BLOCKED, writeBlocked(s.blocked))
            .putBoolean(KEY_AUTO, s.autoBlock)
            .putString(KEY_MAC, mac(s))
            .commit()
    }

    private fun canonical(s: State): String = buildString {
        append("v1|seen=").append(s.seen.sorted().joinToString(","))
        append("|baseline=").append(s.baseline.sorted().joinToString(","))
        append("|ready=").append(s.baselineReady).append("|appops=").append(s.baselineAppOps)
        append("|allowed=").append(s.allowed.sorted().joinToString(","))
        append("|blocked=").append(writeBlocked(s.blocked.toSortedMap()))
        append("|auto=").append(s.autoBlock)
    }

    private fun mac(s: State): String? = runCatching {
        val m = Mac.getInstance("HmacSHA256")
        m.init(macKey())
        Base64.encodeToString(m.doFinal(canonical(s).toByteArray(Charsets.UTF_8)), Base64.NO_WRAP)
    }.onFailure { Log.w(TAG, "state mac", it) }.getOrNull()

    private fun macKey(): SecretKey {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (ks.getKey(MAC_ALIAS, null) as? SecretKey)?.let { return it }
        val gen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_HMAC_SHA256, "AndroidKeyStore")
        gen.init(KeyGenParameterSpec.Builder(MAC_ALIAS, KeyProperties.PURPOSE_SIGN).build())
        return gen.generateKey()
    }

    // ---- קריאה מבחוץ ----

    fun snapshot(context: Context): State = synchronized(lock) { load(context) }

    /** האם זוהה שינוי מבחוץ מאז הפעם הקודמת ששאלו (פעם אחת לכל אירוע). */
    fun takeTamper(): Boolean = synchronized(lock) { lastTamper.also { lastTamper = false } }

    fun setAutoBlock(context: Context, on: Boolean): Boolean = synchronized(lock) {
        save(context, load(context).copy(autoBlock = on))
        true
    }

    // ---- השומר ברקע ----

    fun evaluate(context: Context, result: ThreatScanner.Result): Outcome = synchronized(lock) {
        val state = load(context)
        val tampered = takeTamperLocked()
        // סריקה חלקית (מקור שלא נקרא) לא משנה כלום: אחרת ממצא שלא נקרא "נעלם", יוצא מנקודת
        // הפתיחה, ובסריקה המלאה הבאה נחסם כאילו הוא חדש
        if (result.unchecked.isNotEmpty()) return@synchronized Outcome(emptyList(), emptyList(), emptyList(), tampered)
        val allowed = state.allowed
        val current = result.findings.map { it.key }.toSet()
        // כשחלון צף/התקנה לא נבדקו בפועל (סריקה בלי root, או ש-su נכשל) - הם לא "נעלמו"
        val carry = if (result.appOpsChecked) emptySet() else state.seen.filter { kindOf(it) in APP_OPS_KINDS }.toSet()
        val seen = current + carry

        if (!state.baselineReady) {
            save(context, state.copy(baseline = current, seen = seen, baselineReady = true, baselineAppOps = result.appOpsChecked))
            val pending = result.findings.filter { it.key !in allowed }
            SecurityAudit.record(context, SecurityAudit.Type.BASELINE, detail = pending.size.toString())
            return@synchronized Outcome(emptyList(), emptyList(), pending, tampered)
        }

        var baselineAll = state.baseline
        var prevSeen = state.seen
        var baselineAppOps = state.baselineAppOps
        // נקודת הפתיחה נקבעה בלי בדיקת חלון צף/התקנה: הבדיקה המלאה הראשונה משלימה אותה ולא חוסמת
        if (!baselineAppOps && result.appOpsChecked) {
            val appOps = current.filter { kindOf(it) in APP_OPS_KINDS }
            baselineAll = baselineAll + appOps
            prevSeen = prevSeen + appOps
            baselineAppOps = true
        }

        // ממצא מנקודת הפתיחה שנעלם וחזר - נחשב חדש
        val baseline = baselineAll.filter { it in seen }.toSet()
        val fresh = result.findings.filter { it.key !in prevSeen && it.key !in allowed }
        val malware = result.findings.filter { it.kind == ThreatScanner.Kind.KNOWN_MALWARE }.map { it.target }.toSet()
        val blockedMap = state.blocked.toMutableMap()
        val blockedNow = ArrayList<ThreatScanner.Finding>()
        if (state.autoBlock) {
            for (f in result.findings) {
                if (f.key in allowed) continue
                val isNew = f in fresh && f.key !in baseline
                val act = when {
                    f.kind == ThreatScanner.Kind.KNOWN_MALWARE -> f.key !in blockedMap
                    f.kind == ThreatScanner.Kind.SUSPICIOUS -> isNew && f.key !in blockedMap
                    f.kind.blockable -> isNew || f.target in malware
                    else -> false
                }
                if (!act) continue
                val entry = ThreatBlocker.block(context, f) ?: continue
                // חסימה חוזרת (היכולת הופעלה שוב) שומרת את מה שצריך כדי להחזיר את המצב המקורי
                val old = blockedMap[f.key]
                blockedMap[f.key] = if (old == null) entry else entry.copy(
                    components = (old.components + entry.components).distinct(),
                    prior = old.prior.ifEmpty { entry.prior },
                )
                blockedNow += f
                val type = if (f.kind in ThreatBlocker.HOLD_KINDS) SecurityAudit.Type.HOLD else SecurityAudit.Type.AUTO_BLOCK
                SecurityAudit.record(context, type, f.kind.name, f.target, f.label, priorText(entry))
            }
        }
        fresh.filter { it !in blockedNow }.forEach { f ->
            val type = if (f.kind == ThreatScanner.Kind.SIDELOADED) SecurityAudit.Type.SIDELOAD else SecurityAudit.Type.NEW
            SecurityAudit.record(context, type, f.kind.name, f.target, f.label, f.detail)
        }
        // יכולת שבוטלה נעלמת מהסריקה; אפליקציה מושהית נשארת בה - ולכן נשארת "נראתה"
        val seenAfter = seen - blockedNow.filter { it.kind !in ThreatBlocker.HOLD_KINDS }.map { it.key }.toSet()
        save(context, state.copy(seen = seenAfter, baseline = baseline, baselineAppOps = baselineAppOps, blocked = blockedMap))
        Outcome(fresh, blockedNow, emptyList(), tampered)
    }

    private fun takeTamperLocked(): Boolean = lastTamper.also { lastTamper = false }

    // ---- פעולות מההגדרות ----

    /** חסום עכשיו. הממצא מגיע מסריקה טרייה כדי שהרכיבים יהיו מדויקים. */
    fun block(context: Context, key: String): Boolean = synchronized(lock) {
        val kind = kindOf(key) ?: return@synchronized false
        if (!kind.blockable && kind !in ThreatBlocker.HOLD_KINDS) return@synchronized false
        val state = load(context)
        val scan = ThreatScanner.scan(context, withAppOps = kind in APP_OPS_KINDS)
        if ("signatures" in scan.unchecked) return@synchronized false
        val f = scan.findings.firstOrNull { it.key == key } ?: return@synchronized false
        val entry = ThreatBlocker.block(context, f) ?: return@synchronized false
        val seen = if (kind in ThreatBlocker.HOLD_KINDS) state.seen else state.seen - key
        save(context, state.copy(blocked = state.blocked + (key to entry), allowed = state.allowed - key, seen = seen))
        SecurityAudit.record(context, SecurityAudit.Type.BLOCK, f.kind.name, f.target, f.label, priorText(entry))
        true
    }

    /** אשר. אם זה נחסם - מחזיר את היכולת (או מבטל את ההשהיה) בדיוק כמו שהיה. */
    fun allow(context: Context, key: String): Boolean = synchronized(lock) {
        val kind = kindOf(key) ?: return@synchronized false
        val state = load(context)
        val entry = state.blocked[key]
        if (entry != null && !ThreatBlocker.restore(context, entry)) return@synchronized false
        // היכולת חוזרת - שהשומר לא יתריע עליה כ"חדשה"
        save(context, state.copy(blocked = state.blocked - key, allowed = state.allowed + key, seen = state.seen + key))
        SecurityAudit.record(
            context,
            if (entry != null) SecurityAudit.Type.RESTORE else SecurityAudit.Type.ALLOW,
            kind.name, key.substringAfter(':'), entry?.label.orEmpty(),
        )
        true
    }

    /** חסימות של אפליקציות שהוסרו מאז - אין מה להחזיר. */
    fun pruneUninstalled(context: Context) = synchronized(lock) {
        val pm = context.packageManager
        val state = load(context)
        val keep = state.blocked.filterValues { e ->
            e.kind == ThreatScanner.Kind.PROXY || runCatching { pm.getApplicationInfo(e.target, 0) }.isSuccess
        }
        if (keep.size != state.blocked.size) save(context, state.copy(blocked = keep))
    }

    /** "מה היה לפני" לרשומה ביומן. */
    private fun priorText(e: ThreatBlocker.Entry): String = when {
        e.kind in ThreatBlocker.HOLD_KINDS -> "האפליקציה הייתה פעילה"
        e.components.isNotEmpty() -> e.components.joinToString(", ") { it.substringAfterLast('/').substringAfterLast('.') }
        e.prior.isNotEmpty() -> e.prior.joinToString(", ") { it.substringAfterLast('.') }
        else -> ""
    }

    private fun kindOf(key: String): ThreatScanner.Kind? =
        runCatching { ThreatScanner.Kind.valueOf(key.substringBefore(':')) }.getOrNull()

    private fun parseBlocked(json: String?): Map<String, ThreatBlocker.Entry> = runCatching {
        val arr = JSONArray(json ?: "[]")
        (0 until arr.length()).mapNotNull { ThreatBlocker.Entry.fromJson(arr.getJSONObject(it)) }.associateBy { it.key }
    }.onFailure { Log.w(TAG, "blocked list", it) }.getOrDefault(emptyMap())

    private fun writeBlocked(map: Map<String, ThreatBlocker.Entry>): String =
        JSONArray().apply { map.values.forEach { put(it.toJson()) } }.toString()
}
