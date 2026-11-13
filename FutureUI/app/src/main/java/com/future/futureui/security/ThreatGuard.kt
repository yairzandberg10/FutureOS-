package com.future.futureui.security

import android.content.Context
import android.util.Log
import org.json.JSONArray

/**
 * המדיניות של ההגנה: מה נחסם אוטומטית, מה המשתמש אישר, ומה חיכה שם עוד לפני שההגנה הופעלה.
 *
 *  - חסימה אוטומטית (ברירת מחדל: פועלת): יכולת מסוכנת שאפליקציה זרה מקבלת מעכשיו נחסמת
 *    מיד, והמשתמש מקבל התראה. ביטול בלחיצה אחת במרכז האבטחה בהגדרות.
 *  - נקודת פתיחה (baseline): מה שכבר היה פעיל כשההגנה הופעלה לראשונה - למשל משגר, שעון
 *    חכם או קישור לטלפון שהמשתמש הפעיל בעצמו - לא נחסם לבד. הוא מחכה להחלטה של המשתמש.
 *  - אפליקציה שנמצאה במאגר הנוזקות: כל היכולות שלה נחסמות, גם אם היו שם מההתחלה.
 *  - מאושר (allowed): המשתמש החליט שזה בסדר. לא נחסם, לא מתריע ולא מוריד את רמת האבטחה.
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

    private val lock = Any()

    const val STATUS_OPEN = "open"
    const val STATUS_ALLOWED = "allowed"
    const val STATUS_BLOCKED = "blocked"

    private val APP_OPS_KINDS = setOf(ThreatScanner.Kind.OVERLAY, ThreatScanner.Kind.INSTALLER)

    data class Outcome(
        /** ממצאים חדשים שהמשתמש לא אישר (כולל אלה שנחסמו). */
        val fresh: List<ThreatScanner.Finding>,
        val blocked: List<ThreatScanner.Finding>,
        /** ההפעלה הראשונה: מה שכבר היה פעיל, מחכה להחלטה. */
        val baseline: List<ThreatScanner.Finding>,
    )

    private fun prefs(context: Context) = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun autoBlock(context: Context): Boolean = prefs(context).getBoolean(KEY_AUTO, true)

    fun setAutoBlock(context: Context, on: Boolean) = synchronized(lock) {
        prefs(context).edit().putBoolean(KEY_AUTO, on).commit()
    }

    fun allowed(context: Context): Set<String> = prefs(context).getStringSet(KEY_ALLOWED, emptySet()).orEmpty().toSet()

    fun blocked(context: Context): Map<String, ThreatBlocker.Entry> = synchronized(lock) { readBlocked(context) }

    fun status(allowed: Set<String>, f: ThreatScanner.Finding): String =
        if (f.key in allowed) STATUS_ALLOWED else STATUS_OPEN

    /** השומר ברקע: מחליט מה לחסום אחרי כל סריקה. */
    fun evaluate(context: Context, result: ThreatScanner.Result, withAppOps: Boolean): Outcome = synchronized(lock) {
        val p = prefs(context)
        // סריקה חלקית (מקור שלא נקרא) לא משנה כלום: אחרת ממצא שלא נקרא "נעלם", יוצא מנקודת
        // הפתיחה, ובסריקה המלאה הבאה נחסם כאילו הוא חדש - למשל SMS של אפליקציית ההודעות
        if (result.unchecked.isNotEmpty()) return@synchronized Outcome(emptyList(), emptyList(), emptyList())
        val allowed = allowed(context)
        val current = result.findings.map { it.key }.toSet()
        val prevSeen = p.getStringSet(KEY_SEEN, emptySet()).orEmpty().toSet()
        // כשחלון צף/התקנה לא נבדקו בפועל (סריקה בלי root, או ש-su נכשל) - הם לא "נעלמו"
        val carry = if (result.appOpsChecked) emptySet() else prevSeen.filter { kindOf(it) in APP_OPS_KINDS }.toSet()
        val seen = current + carry

        if (!p.getBoolean(KEY_BASELINE_READY, false)) {
            p.edit()
                .putStringSet(KEY_BASELINE, current)
                .putStringSet(KEY_SEEN, seen)
                .putBoolean(KEY_BASELINE_READY, true)
                .putBoolean(KEY_BASELINE_APPOPS, result.appOpsChecked)
                .commit()
            return@synchronized Outcome(emptyList(), emptyList(), result.findings.filter { it.key !in allowed })
        }

        var baselineAll = p.getStringSet(KEY_BASELINE, emptySet()).orEmpty().toSet()
        var prevSeenAll = prevSeen
        // נקודת הפתיחה נקבעה בלי בדיקת חלון צף/התקנה: הבדיקה המלאה הראשונה משלימה אותה ולא חוסמת
        if (!p.getBoolean(KEY_BASELINE_APPOPS, false) && result.appOpsChecked) {
            val appOps = current.filter { kindOf(it) in APP_OPS_KINDS }
            baselineAll = baselineAll + appOps
            prevSeenAll = prevSeenAll + appOps
            p.edit().putBoolean(KEY_BASELINE_APPOPS, true).commit()
        }

        // ממצא מנקודת הפתיחה שנעלם וחזר - נחשב חדש
        val baseline = baselineAll.filter { it in seen }.toSet()
        val fresh = result.findings.filter { it.key !in prevSeenAll && it.key !in allowed }
        val malware = result.findings.filter { it.kind == ThreatScanner.Kind.KNOWN_MALWARE }.map { it.target }.toSet()
        val blockedMap = readBlocked(context).toMutableMap()
        val blockedNow = ArrayList<ThreatScanner.Finding>()
        // בלי חותמים שלנו אי אפשר לדעת מה זר - לא חוסמים כלום (ThreatBlocker בודק גם הוא)
        if (autoBlock(context) && "signatures" !in result.unchecked) {
            for (f in result.findings) {
                if (!f.kind.blockable || f.key in allowed) continue
                val isNew = f in fresh && f.key !in baseline
                if (!isNew && f.target !in malware) continue
                val entry = ThreatBlocker.block(context, f) ?: continue
                // חסימה חוזרת (היכולת הופעלה שוב) שומרת את מה שצריך כדי להחזיר את המצב המקורי
                val old = blockedMap[f.key]
                blockedMap[f.key] = if (old == null) entry else entry.copy(
                    components = (old.components + entry.components).distinct(),
                    prior = old.prior.ifEmpty { entry.prior },
                )
                blockedNow += f
            }
        }
        val seenAfter = seen - blockedNow.map { it.key }.toSet()
        p.edit()
            .putStringSet(KEY_SEEN, seenAfter)
            .putStringSet(KEY_BASELINE, baseline)
            .putString(KEY_BLOCKED, writeBlocked(blockedMap))
            .commit()
        Outcome(fresh, blockedNow, emptyList())
    }

    /** מההגדרות: חסום עכשיו. הממצא מגיע מסריקה טרייה כדי שהרכיבים יהיו מדויקים. */
    fun block(context: Context, key: String): Boolean = synchronized(lock) {
        val kind = kindOf(key) ?: return@synchronized false
        if (!kind.blockable) return@synchronized false
        val scan = ThreatScanner.scan(context, withAppOps = kind in APP_OPS_KINDS)
        if ("signatures" in scan.unchecked) return@synchronized false
        val f = scan.findings.firstOrNull { it.key == key } ?: return@synchronized false
        val entry = ThreatBlocker.block(context, f) ?: return@synchronized false
        val map = readBlocked(context).toMutableMap()
        map[key] = entry
        val p = prefs(context)
        p.edit()
            .putString(KEY_BLOCKED, writeBlocked(map))
            .putStringSet(KEY_ALLOWED, allowed(context) - key)
            .putStringSet(KEY_SEEN, p.getStringSet(KEY_SEEN, emptySet()).orEmpty() - key)
            .commit()
        true
    }

    /** מההגדרות: אשר. אם זה נחסם - מחזיר את היכולת בדיוק כמו שהייתה. */
    fun allow(context: Context, key: String): Boolean = synchronized(lock) {
        if (kindOf(key) == null) return@synchronized false
        val map = readBlocked(context).toMutableMap()
        val entry = map[key]
        if (entry != null) {
            if (!ThreatBlocker.restore(context, entry)) return@synchronized false
            map.remove(key)
        }
        val p = prefs(context)
        p.edit()
            .putString(KEY_BLOCKED, writeBlocked(map))
            .putStringSet(KEY_ALLOWED, allowed(context) + key)
            // היכולת חוזרת - שהשומר לא יתריע עליה כ"חדשה"
            .putStringSet(KEY_SEEN, p.getStringSet(KEY_SEEN, emptySet()).orEmpty() + key)
            .commit()
        true
    }

    /** חסימות של אפליקציות שהוסרו מאז - אין מה להחזיר. */
    fun pruneUninstalled(context: Context) = synchronized(lock) {
        val pm = context.packageManager
        val map = readBlocked(context)
        val keep = map.filterValues { e ->
            e.kind == ThreatScanner.Kind.PROXY || runCatching { pm.getApplicationInfo(e.target, 0) }.isSuccess
        }
        if (keep.size != map.size) prefs(context).edit().putString(KEY_BLOCKED, writeBlocked(keep)).commit()
    }

    private fun kindOf(key: String): ThreatScanner.Kind? =
        runCatching { ThreatScanner.Kind.valueOf(key.substringBefore(':')) }.getOrNull()

    private fun readBlocked(context: Context): Map<String, ThreatBlocker.Entry> = runCatching {
        val arr = JSONArray(prefs(context).getString(KEY_BLOCKED, "[]") ?: "[]")
        (0 until arr.length()).mapNotNull { ThreatBlocker.Entry.fromJson(arr.getJSONObject(it)) }.associateBy { it.key }
    }.onFailure { Log.w(TAG, "blocked list", it) }.getOrDefault(emptyMap())

    private fun writeBlocked(map: Map<String, ThreatBlocker.Entry>): String =
        JSONArray().apply { map.values.forEach { put(it.toJson()) } }.toString()
}
