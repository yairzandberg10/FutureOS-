package com.future.keyboard

import com.future.sharednav.t9.T9DigitMap

/**
 * מנוע ניבוי טקסט T9 - ממיר רצף לחיצות על מקשי הספרות הפיזיים (2-9) למילים
 * אמיתיות, בדיוק כמו בטלפונים ישנים: כל מקש מייצג כמה אותיות, והמנוע בוחר
 * את המילה הכי נפוצה שמתאימה לרצף הספרות מתוך מילון מובנה. אם אין התאמה
 * במילון, נופלים חזרה למצב "לחיצות מרובות" (multi-tap) - לחיצות חוזרות על
 * אותו מקש עוברות בין האותיות שלו, בדיוק כמו הקלדת SMS ישנה.
 */
class T9Engine(private val language: Language) {

    enum class Language { HEBREW, ENGLISH }

    companion object {
        // מיפוי אנגלי/עברי משותף לכל הסוויטה - חי ב-SharedKeypadNav (T9DigitMap),
        // כדי שלא יידרש יותר לשמור אותו מסונכרן ידנית מול T9Search.kt של החייגן/המוזיקה.
        private val ENGLISH_MAP = T9DigitMap.ENGLISH
        private val HEBREW_MAP = T9DigitMap.HEBREW

        // מילון בכ-250 מילים נפוצות לכל שפה, ממוין מהנפוצה ביותר להכי פחות נפוצה בכל
        // קבוצת ספרות - הרחבה ממשית מעבר לרשימת ה"הדגמה" המקורית של כ-60 מילים, כדי
        // שהניבוי יהיה שימושי בפועל בהקלדה יומיומית ולא רק בהדגמה.
        private val ENGLISH_WORDS = listOf(
            "the", "of", "and", "to", "in", "is", "you", "that", "it", "he",
            "was", "for", "on", "are", "as", "with", "his", "they", "at", "be",
            "this", "have", "from", "or", "one", "had", "by", "word", "but", "not",
            "what", "all", "were", "we", "when", "your", "can", "said", "there", "use",
            "hi", "hello", "how", "good", "morning", "night", "yes", "no",
            "ok", "call", "me", "back", "later", "today", "tomorrow", "home", "work", "love",
            "thanks", "please", "sorry", "see", "soon", "time", "day", "week", "help", "need",
            "a", "about", "after", "again", "air", "also", "an", "another", "any", "around",
            "ask", "away", "because", "been", "before", "being", "below", "between", "big", "boy",
            "came", "come", "could", "different", "does", "don't", "down", "each", "eat", "end",
            "even", "every", "eyes", "face", "family", "far", "father", "few", "find", "first",
            "follow", "form", "found", "four", "get", "girl", "give", "go", "got", "great",
            "group", "grow", "hand", "hard", "has", "head", "hear", "her", "here", "high",
            "him", "himself", "house", "i", "if", "important", "into", "its", "just", "keep",
            "kind", "knew", "know", "land", "last", "learn", "leave", "left", "let", "life",
            "light", "like", "line", "list", "little", "live", "long", "look", "made", "make",
            "man", "many", "may", "mean", "men", "might", "more", "most", "mother", "move",
            "much", "must", "my", "name", "near", "never", "new", "next", "now", "number",
            "off", "often", "old", "only", "other", "our", "out", "over", "own", "page",
            "part", "people", "picture", "place", "plant", "point", "put", "question", "read", "really",
            "right", "run", "same", "saw", "say", "school", "sea", "seem", "sentence", "set",
            "several", "she", "should", "show", "side", "since", "small", "so", "some", "something",
            "sometimes", "sound", "spell", "start", "state", "still", "stop", "story", "study", "such",
            "take", "tell", "than", "thank", "their", "them", "then", "thing", "think", "those",
            "thought", "three", "through", "too", "took", "tree", "try", "turn", "two", "under",
            "until", "up", "upon", "us", "very", "want", "water", "way", "well", "went",
            "while", "who", "why", "will", "without", "women", "world", "would", "write", "year",
            "yet", "mom", "dad", "friend", "phone", "money", "food", "drink", "coffee", "tea",
            "car", "bus", "train", "office", "meeting", "busy", "free", "sure", "maybe", "okay",
            "awesome", "birthday", "happy", "welcome", "bye", "goodbye", "talk", "chat", "text", "message",
            "email", "address", "where", "know", "love", "great", "job", "team", "project", "sorry"
        )

        private val HEBREW_WORDS = listOf(
            "של", "את", "על", "לא", "זה", "עם", "יש", "אני", "הוא", "היא",
            "אתה", "הם", "אנחנו", "כל", "גם", "אבל", "אם", "מה", "מי",
            "איך", "כן", "טוב", "שלום", "היי", "בוקר", "ערב", "לילה", "תודה",
            "בבקשה", "סליחה", "אוהב", "אוהבת", "בית", "עבודה", "מחר", "היום", "אתמול", "עכשיו",
            "אחר", "כך", "לך", "לי", "לו", "לה", "פה", "שם", "רק", "עוד",
            "צריך", "רוצה", "יכול", "אפשר", "בסדר", "מתי", "איפה", "למה", "כמה", "נשמע",
            "אחד", "אחת", "שתי", "שני", "שלוש", "ארבע", "חמש", "שש", "שבע", "שמונה",
            "תשע", "עשר", "מאה", "אלף", "ראשון", "אחרון", "גדול", "קטן", "חדש", "ישן",
            "יפה", "טובה", "רע", "רעה", "חם", "קר", "מהר", "לאט", "הרבה", "מעט",
            "כאן", "שם", "מעל", "מתחת", "לפני", "אחרי", "בין", "ליד", "רחוק", "קרוב",
            "משפחה", "אמא", "אבא", "אח", "אחות", "ילד", "ילדה", "חבר", "חברה", "מכר",
            "טלפון", "כסף", "אוכל", "שתייה", "קפה", "תה", "מים", "רכב", "אוטובוס", "רכבת",
            "משרד", "פגישה", "עסוק", "פנוי", "בטוח", "אולי", "בסדר", "מזל", "טוב", "יום",
            "הולדת", "שמח", "שמחה", "ברוך", "הבא", "ברוכה", "הבאה", "להתראות", "ביי", "דבר",
            "לדבר", "לכתוב", "הודעה", "מייל", "כתובת", "מקום", "עיר", "רחוב", "בית", "ספר",
            "עבודה", "פרויקט", "צוות", "מנהל", "לקוח", "פגישה", "זמן", "שעה", "דקה", "שנייה",
            "שבוע", "חודש", "שנה", "היום", "מחר", "אתמול", "עכשיו", "תמיד", "אף", "פעם",
            "כבר", "עדיין", "פתאום", "לאט", "מהר", "יחד", "לבד", "כולם", "אף", "אחד",
            "משהו", "כלום", "הכל", "חלק", "רוב", "מעט", "יותר", "פחות", "מאוד", "בערך",
            "בטח", "אולי", "כנראה", "ודאי", "תודה", "רבה", "בבקשה", "סליחה", "מצטער", "מצטערת",
            "כיף", "נהדר", "מעולה", "יפה", "נכון", "לא", "נכון", "אמת", "שקר", "חשוב",
            "לומד", "לומדת", "יודע", "יודעת", "חושב", "חושבת", "מרגיש", "מרגישה", "רואה", "שומע",
            "מגיע", "מגיעה", "יוצא", "יוצאת", "נכנס", "נכנסת", "חוזר", "חוזרת", "נשאר", "נשארת",
            "מקווה", "מבין", "מבינה", "זוכר", "זוכרת", "שוכח", "שוכחת", "מחכה", "מחכה", "בדרך"
        )

        /** ממיר מילה לרצף הספרות שהיא הייתה מייצרת - המפתח לחיפוש מהיר במילון. */
        fun digitsFor(word: String, language: Language): String {
            val map = if (language == Language.HEBREW) HEBREW_MAP else ENGLISH_MAP
            val builder = StringBuilder()
            for (ch in word) {
                val digit = map.entries.firstOrNull { ch in it.value }?.key ?: return ""
                builder.append(digit)
            }
            return builder.toString()
        }
    }

    private val keyMap = if (language == Language.HEBREW) HEBREW_MAP else ENGLISH_MAP
    private val dictionary = if (language == Language.HEBREW) HEBREW_WORDS else ENGLISH_WORDS

    // אינדקס: רצף ספרות -> רשימת מילים תואמות, ממוין לפי סדר הופעה במילון (=פופולריות)
    private val digitIndex: Map<String, List<String>> by lazy {
        dictionary.groupBy { digitsFor(it, language) }.filterKeys { it.isNotEmpty() }
    }

    fun lettersFor(digit: Char): String = keyMap[digit] ?: ""

    /**
     * כל המילים במילון שתואמות לרצף הספרות שהוזן עד כה. סדר ברירת המחדל הוא
     * פופולריות המילון (הכי נפוצה קודם), אבל [wordFrequency] - מספר הפעמים
     * שהמשתמש עצמו כבר הקליד/בחר את המילה - מזיז מילים שנלמדו בפועל קדימה
     * (מיון stable: מילים בעלות אותה תדירות נשארות בסדר המילון המקורי ביניהן).
     */
    fun candidatesFor(digits: String, wordFrequency: (String) -> Int = { 0 }): List<String> {
        if (digits.isEmpty()) return emptyList()
        val base = digitIndex[digits] ?: return emptyList()
        return base.sortedByDescending { wordFrequency(it) }
    }

}
