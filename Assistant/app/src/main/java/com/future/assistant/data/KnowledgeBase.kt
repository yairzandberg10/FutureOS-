package com.future.assistant.data

import android.content.Context

/**
 * מאגר ידע מקומי - אלפי פקודות שהתשובות שלהן יושבות בקבצי TSV תחת
 * assets/knowledge (בירות, יסודות, מילון עברית-אנגלית, הפכים, ברכות, תנ"ך,
 * פרשות, חיות, כוכבי לכת, אישים ושאלות כלליות). בלי אינטרנט.
 *
 * *** איך מוסיפים ידע ***
 * מוסיפים שורה לקובץ המתאים ב-assets/knowledge - בלי לגעת בקוד. השדות
 * מופרדים בטאב, שמות נוספים לאותו דבר מופרדים ב-|, ושורה שמתחילה ב-#
 * היא הערה. שאלה כללית חדשה = שורה ב-trivia.tsv: ניסוחים|מופרדים<TAB>תשובה.
 *
 * כל קטגוריה מופעלת רק כשיש במשפט "מילת רמז" שלה (למשל "בירה", "באנגלית",
 * "מברכים") וגם ישות מוכרת מהטבלה - אחרת ממשיכים לקטגוריה הבאה, ובסוף
 * לפקודות הרגילות של CommandProcessor.
 */
object KnowledgeBase {
    @Volatile private var loaded: Data? = null

    /** טעינה מראש ברקע, כדי שהשאלה הראשונה לא תחכה לקריאת הקבצים. */
    fun preload(context: Context) {
        data(context)
    }

    fun answer(context: Context, text: String): CommandResult? = answer(data(context), text)

    fun commandCount(context: Context): Int = data(context).commandCount

    private fun data(context: Context): Data =
        loaded ?: synchronized(this) {
            loaded ?: load { name ->
                context.assets.open("knowledge/$name").bufferedReader().use { it.readText() }
            }.also { loaded = it }
        }

    // ---------------------------------------------------------------------
    // נרמול טקסט - Whisper מחזיר פיסוק וגרשיים, והטבלאות כתובות בכל מיני צורות.
    // ---------------------------------------------------------------------

    private val NIKUD = Regex("[\\u0591-\\u05C7]")
    private val PUNCTUATION = Regex("[\"'׳״`?!.,:;()\\[\\]]")
    private val DASHES = Regex("[-־–]")
    private val SPACES = Regex("\\s+")

    fun normalize(s: String): String = s
        .replace(NIKUD, "")
        .replace(PUNCTUATION, "")
        .replace(DASHES, " ")
        .replace(SPACES, " ")
        .trim()

    private fun words(s: String) = s.split(" ").filter { it.isNotBlank() }

    // ---------------------------------------------------------------------
    // טבלאות
    // ---------------------------------------------------------------------

    class Country(val name: String, val capital: String, val continent: String)
    class Element(val number: Int, val name: String, val symbol: String)
    class Word(val hebrew: String, val english: String, val spoken: String)
    class Blessing(val food: String, val first: String, val last: String)
    class Book(val name: String, val chapters: Int, val section: String)
    class Parasha(val name: String, val book: String, val order: Int)
    class Animal(val name: String, val sound: String, val baby: String, val group: String)
    class Planet(val name: String, val order: Int, val moons: Int, val fact: String)
    class Person(val name: String, val about: String)
    class Trivia(val phrasings: List<String>, val answer: String)

    class Data(
        val countries: Map<String, Country>,
        val capitals: Map<String, List<Country>>,
        val elements: Map<String, Element>,
        val elementsByNumber: Map<Int, Element>,
        val dictionary: Map<String, Word>,
        val opposites: Map<String, List<String>>,
        val blessings: Map<String, Blessing>,
        val books: Map<String, Book>,
        val parashot: Map<String, Parasha>,
        val parashaList: List<Parasha>,
        val animals: Map<String, Animal>,
        val planets: Map<String, Planet>,
        val people: Map<String, Person>,
        val trivia: List<Trivia>,
        val commandCount: Int,
    )

    private fun rows(text: String, columns: Int): List<List<String>> =
        text.lineSequence()
            .map { it.trimEnd('\r') }
            .filter { it.isNotBlank() && !it.startsWith("#") }
            .map { line -> line.split("\t").map { it.trim() } }
            .filter { it.size >= columns }
            .toList()

    private fun names(field: String) = field.split("|").map { normalize(it) }.filter { it.isNotEmpty() }

    /** ממפה כל שם (כולל שמות נוספים) לערך - הראשון שמופיע בקובץ מנצח. */
    private fun <T> index(rows: List<List<String>>, nameColumn: Int, make: (List<String>) -> T): Map<String, T> {
        val map = LinkedHashMap<String, T>()
        rows.forEach { row ->
            val value = make(row)
            names(row[nameColumn]).forEach { map.putIfAbsent(it, value) }
        }
        return map
    }

    fun load(read: (String) -> String): Data {
        val countryRows = rows(read("capitals.tsv"), 3)
        val countries = index(countryRows, 0) { Country(it[0].substringBefore("|"), it[1].substringBefore("|"), it[2]) }
        val capitals = LinkedHashMap<String, MutableList<Country>>()
        countryRows.forEach { row ->
            val country = countries.getValue(names(row[0]).first())
            names(row[1]).forEach { capitals.getOrPut(it) { mutableListOf() }.add(country) }
        }

        val elementRows = rows(read("elements.tsv"), 3)
        val elements = index(elementRows, 1) { Element(it[0].toInt(), it[1].substringBefore("|"), it[2]) }
        val elementsByNumber = elements.values.associateBy { it.number }

        val dictionaryRows = rows(read("dictionary.tsv"), 3)
        val dictionary = index(dictionaryRows, 0) { Word(it[0].substringBefore("|"), it[1], it[2]) }

        val opposites = LinkedHashMap<String, MutableList<String>>()
        rows(read("opposites.tsv"), 2).forEach { (a, b) ->
            opposites.getOrPut(normalize(a)) { mutableListOf() }.let { if (b !in it) it += b }
            opposites.getOrPut(normalize(b)) { mutableListOf() }.let { if (a !in it) it += a }
        }

        val blessingRows = rows(read("blessings.tsv"), 3)
        val blessings = index(blessingRows, 0) { Blessing(it[0].substringBefore("|"), it[1], it[2]) }

        val bookRows = rows(read("tanakh.tsv"), 3)
        val books = index(bookRows, 0) { Book(it[0].substringBefore("|"), it[1].toInt(), it[2]) }

        val parashaRows = rows(read("parashot.tsv"), 2)
        val parashaList = parashaRows.mapIndexed { i, row -> Parasha(row[0].substringBefore("|"), row[1], i + 1) }
        val parashot = LinkedHashMap<String, Parasha>()
        parashaRows.forEachIndexed { i, row -> names(row[0]).forEach { parashot.putIfAbsent(it, parashaList[i]) } }

        val animalRows = rows(read("animals.tsv"), 4)
        val animals = index(animalRows, 0) { Animal(it[0].substringBefore("|"), it[1], it[2], it[3]) }

        val planetRows = rows(read("planets.tsv"), 4)
        val planets = index(planetRows, 0) { Planet(it[0].substringBefore("|"), it[1].toInt(), it[2].toInt(), it[3]) }

        val peopleRows = rows(read("people.tsv"), 2)
        val people = index(peopleRows, 0) { Person(it[0].substringBefore("|"), it[1]) }

        val trivia = rows(read("trivia.tsv"), 2).map { Trivia(names(it[0]), it[1]) }

        // ספירה שמרנית: כל שאלה נפרדת שיש לה תשובה משלה = פקודה אחת.
        val count = countryRows.size * 2 + capitals.size +
            elementRows.size * 3 +
            dictionaryRows.size * 2 +
            opposites.size +
            blessingRows.size * 2 +
            bookRows.size * 2 +
            parashaRows.size * 3 +
            animalRows.sumOf { row -> listOf(row[1], row[2], row[3]).count { it != "-" } } +
            planetRows.size * 3 +
            peopleRows.size +
            trivia.size

        return Data(
            countries, capitals, elements, elementsByNumber, dictionary, opposites, blessings,
            books, parashot, parashaList, animals, planets, people, trivia, count,
        )
    }

    // ---------------------------------------------------------------------
    // חיפוש ישות בתוך משפט
    // ---------------------------------------------------------------------

    private const val PREFIX_LETTERS = "הבלמושכ"
    private const val MAX_PHRASE_WORDS = 4

    /** השם עצמו, ואז עם עד שתי אותיות שימוש (ה/ב/ל/מ/ו/ש/כ) מוסרות מההתחלה. */
    private fun variants(phrase: String): List<String> {
        val out = mutableListOf(phrase)
        var s = phrase
        repeat(2) {
            if (s.length >= 3 && s[0] in PREFIX_LETTERS) { s = s.substring(1); out += s }
        }
        return out
    }

    /** מוצא את הישות הארוכה ביותר מהטבלה שמופיעה במשפט כמילים שלמות. */
    private fun <T> find(map: Map<String, T>, words: List<String>): T? {
        var bestKey: String? = null
        var best: T? = null
        for (i in words.indices) {
            for (n in minOf(MAX_PHRASE_WORDS, words.size - i) downTo 1) {
                val phrase = words.subList(i, i + n).joinToString(" ")
                val key = variants(phrase).firstOrNull { map.containsKey(it) } ?: continue
                if (bestKey == null || key.length > bestKey.length) { bestKey = key; best = map[key] }
                break
            }
        }
        return best
    }

    private fun String.hasAny(cues: List<String>) = cues.any { contains(it) }

    // ---------------------------------------------------------------------
    // תשובות
    // ---------------------------------------------------------------------

    fun answer(data: Data, rawText: String): CommandResult? {
        val text = normalize(rawText)
        if (text.isEmpty()) return null
        val all = words(text)
        return spelling(data, text, all)
            ?: translation(data, text, all)
            ?: opposite(data, text, all)
            ?: blessing(data, text, all)
            ?: element(data, text, all)
            ?: capital(data, text, all)
            ?: continent(data, text, all)
            ?: capitalReverse(data, text, all)
            ?: book(data, text, all)
            ?: parasha(data, text, all)
            ?: animal(data, text, all)
            ?: planet(data, text, all)
            ?: gematria(text, all)
            ?: person(data, text, all)
            ?: trivia(data, text)
    }

    // --- מילון -----------------------------------------------------------

    private val DICTIONARY_FRAME = setOf(
        "איך", "אומרים", "אומר", "באנגלית", "אנגלית", "מה", "זה", "תרגם", "תתרגם", "תרגמי", "תרגום", "את",
        "המילה", "מילה", "של", "לי", "בבקשה", "תגיד", "תגידי", "כותבים", "תאיית", "תאייתי", "איות", "מאייתים",
        "לאנגלית", "האנגלית", "התרגום", "נקרא", "קוראים", "ל",
    )
    private val DICTIONARY_CORE = setOf("איך", "אומרים", "באנגלית", "לאנגלית", "תרגם", "תתרגם", "תרגמי", "תרגום", "התרגום", "את", "המילה")

    private fun dictionaryWord(data: Data, all: List<String>): Word? {
        val stripped = all.filter { it !in DICTIONARY_FRAME }
        return find(data.dictionary, stripped.ifEmpty { all.filter { it !in DICTIONARY_CORE } })
    }

    private fun spelling(data: Data, text: String, all: List<String>): CommandResult? {
        if (!text.hasAny(listOf("איך כותבים", "תאיית", "איות", "מאייתים", "איך מאיית"))) return null
        if (!text.hasAny(listOf("אנגלית", "תאיית", "מאייתים", "איות"))) return null
        val word = dictionaryWord(data, all) ?: return null
        val letters = word.english.uppercase().filter { it.isLetter() }
        return CommandResult(
            "${word.hebrew} באנגלית: ${word.english}\n${letters.toList().joinToString("-")}",
            speech = "${word.hebrew} באנגלית זה ${word.spoken}. באותיות: ${letters.map { LETTER_NAMES[it] ?: "" }.joinToString(", ")}",
        )
    }

    private fun translation(data: Data, text: String, all: List<String>): CommandResult? {
        if (!text.hasAny(listOf("באנגלית", "לאנגלית", "תרגם", "תתרגם", "תרגמי", "תרגום"))) return null
        val word = dictionaryWord(data, all) ?: return null
        return CommandResult(
            "${word.hebrew} באנגלית: ${word.english}",
            speech = "${word.hebrew} באנגלית זה ${word.spoken}",
        )
    }

    // --- הפכים -----------------------------------------------------------

    private val OPPOSITE_FRAME = setOf(
        "מה", "ההפך", "הפך", "ההיפך", "היפך", "הפוך", "ניגוד", "הניגוד", "של", "המילה", "מילה", "זה", "תגיד", "לי",
        "ההפוך", "המילה", "הוא", "היא",
    )

    private fun opposite(data: Data, text: String, all: List<String>): CommandResult? {
        if (!text.hasAny(listOf("הפך", "היפך", "הפוך", "ניגוד"))) return null
        val words = all.filter { it !in OPPOSITE_FRAME }
        var bestKey: String? = null
        for (i in words.indices) for (n in minOf(3, words.size - i) downTo 1) {
            val phrase = words.subList(i, i + n).joinToString(" ")
            val key = variants(phrase).firstOrNull { data.opposites.containsKey(it) } ?: continue
            if (bestKey == null || key.length > bestKey.length) bestKey = key
            break
        }
        val key = bestKey ?: return null
        return CommandResult("ההפך של $key הוא ${data.opposites.getValue(key).joinToString(" או ")}")
    }

    // --- ברכות -----------------------------------------------------------

    private val FIRST_BLESSING = mapOf(
        "H" to "המוציא לחם מן הארץ", "M" to "בורא מיני מזונות", "G" to "בורא פרי הגפן",
        "E" to "בורא פרי העץ", "A" to "בורא פרי האדמה", "S" to "שהכל נהיה בדברו",
    )
    private val LAST_BLESSING = mapOf(
        "BM" to "ברכת המזון", "ME" to "ברכה מעין שלוש, על המחיה", "GF" to "ברכה מעין שלוש, על הגפן",
        "EZ" to "ברכה מעין שלוש, על העץ", "N" to "בורא נפשות",
    )

    private fun blessing(data: Data, text: String, all: List<String>): CommandResult? {
        if (!text.hasAny(listOf("מברכים", "מברך", "ברכה", "לברך", "מברכת"))) return null
        val food = find(data.blessings, all) ?: return null
        val isLast = text.hasAny(listOf("אחרונה", "אחרי", "לאחר", "בסוף"))
        return if (isLast) {
            CommandResult("אחרי ${food.food} מברכים ${LAST_BLESSING[food.last]}")
        } else {
            CommandResult("על ${food.food} מברכים ${FIRST_BLESSING[food.first]}")
        }
    }

    // --- יסודות ----------------------------------------------------------

    private fun element(data: Data, text: String, all: List<String>): CommandResult? {
        val symbolCue = text.contains("סמל")
        val numberCue = text.contains("אטומי")
        val elementCue = text.contains("יסוד")
        if (!symbolCue && !numberCue && !elementCue) return null
        val element = find(data.elements, all)
        if (element == null) {
            if (!elementCue) return null
            val n = Regex("\\d+").find(text)?.value?.toIntOrNull()
                ?: HebrewNumbers.findNumbers(text, 1).firstOrNull() ?: return null
            val byNumber = data.elementsByNumber[n] ?: return null
            return CommandResult("היסוד שהמספר האטומי שלו $n הוא ${byNumber.name}, והסמל שלו ${byNumber.symbol}",
                speech = "היסוד שהמספר האטומי שלו $n הוא ${byNumber.name}, והסמל שלו ${spell(byNumber.symbol)}")
        }
        val symbolText = "הסמל של ${element.name} הוא ${element.symbol}"
        val symbolSpeech = "הסמל של ${element.name} הוא ${spell(element.symbol)}"
        val numberText = "המספר האטומי של ${element.name} הוא ${element.number}"
        return when {
            symbolCue && !numberCue -> CommandResult(symbolText, speech = symbolSpeech)
            numberCue && !symbolCue -> CommandResult(numberText)
            else -> CommandResult("$numberText, ו$symbolText", speech = "$numberText, ו$symbolSpeech")
        }
    }

    // --- מדינות ----------------------------------------------------------

    private fun capital(data: Data, text: String, all: List<String>): CommandResult? {
        if (!text.contains("בירה")) return null
        val country = find(data.countries, all) ?: return null
        return CommandResult("עיר הבירה של ${country.name} היא ${country.capital}")
    }

    private fun continent(data: Data, text: String, all: List<String>): CommandResult? {
        if (!text.contains("יבשת")) return null
        val country = find(data.countries, all) ?: return null
        return CommandResult("${country.name} נמצאת ב${country.continent}")
    }

    private fun capitalReverse(data: Data, text: String, all: List<String>): CommandResult? {
        if (!text.hasAny(listOf("איזו מדינה", "איזה מדינה", "איפה נמצאת", "איפה נמצא", "איפה זה", "בירה של", "באיזו ארץ"))) return null
        val countries = find(data.capitals, all) ?: return null
        val city = countries.first().capital
        return CommandResult("$city היא עיר הבירה של ${countries.joinToString(" ושל ") { it.name }}")
    }

    // --- תנ"ך ופרשות -----------------------------------------------------

    private val SECTION_PHRASE = mapOf("תורה" to "בתורה", "נביאים" to "בנביאים", "כתובים" to "בכתובים")

    private fun book(data: Data, text: String, all: List<String>): CommandResult? {
        val chaptersCue = text.contains("פרקים")
        val sectionCue = text.hasAny(listOf("באיזה חלק", "איזה חלק", "לאיזה חלק", "שייך", "נביאים או", "כתובים או"))
        if (!chaptersCue && !sectionCue) return null
        val book = find(data.books, all) ?: return null
        return if (chaptersCue) CommandResult("בספר ${book.name} יש ${book.chapters} פרקים")
        else CommandResult("ספר ${book.name} נמצא ${SECTION_PHRASE[book.section] ?: book.section}")
    }

    private val PARASHA_FRAME = setOf(
        "מה", "איזו", "איזה", "פרשה", "פרשת", "הפרשה", "פרשות", "באה", "בא אחרי", "אחרי", "לפני", "שאחרי", "שלפני",
        "באיזה", "ספר", "נמצאת", "היא", "של", "לי", "תגיד", "הבאה", "הקודמת", "קודמת", "אחר", "בספר",
    )

    private fun parasha(data: Data, text: String, all: List<String>): CommandResult? {
        if (!text.hasAny(listOf("פרשת", "פרשה", "פרשות"))) return null
        val parasha = find(data.parashot, all.filter { it !in PARASHA_FRAME }) ?: return null
        val list = data.parashaList
        return when {
            text.hasAny(listOf("אחרי", "הבאה")) -> {
                val next = list.getOrNull(parasha.order)
                if (next == null) CommandResult("וזאת הברכה היא הפרשה האחרונה בתורה, ואחריה מתחילים שוב מבראשית")
                else CommandResult("אחרי פרשת ${parasha.name} באה פרשת ${next.name}")
            }
            text.hasAny(listOf("לפני", "הקודמת", "קודמת")) -> {
                val previous = list.getOrNull(parasha.order - 2)
                if (previous == null) CommandResult("בראשית היא הפרשה הראשונה בתורה")
                else CommandResult("לפני פרשת ${parasha.name} באה פרשת ${previous.name}")
            }
            else -> {
                val inBook = list.filter { it.book == parasha.book }.indexOf(parasha) + 1
                CommandResult("פרשת ${parasha.name} היא הפרשה ה-$inBook בספר ${parasha.book}, והפרשה ה-${parasha.order} בתורה")
            }
        }
    }

    // --- חיות ------------------------------------------------------------

    private val ANIMAL_BABY_CUES = listOf("גור", "תינוק", "ילד של", "צאצא", "הבן של", "לילד", "לתינוק", "גורים", "לצאצא")
    private val ANIMAL_GROUP_CUES = listOf("יונק", "מחלקה", "זוחל", "דו חיים", "איזה סוג חיה", "לאיזו קבוצה", "חרק", "איזה סוג")
    private val ANIMAL_SOUND_CUES = listOf("עושה", "קול", "אומר", "אומרת", "נשמע", "משמיע", "משמיעה", "צליל")

    private fun animal(data: Data, text: String, all: List<String>): CommandResult? {
        val baby = text.hasAny(ANIMAL_BABY_CUES)
        val group = text.hasAny(ANIMAL_GROUP_CUES)
        val sound = text.hasAny(ANIMAL_SOUND_CUES)
        if (!baby && !group && !sound) return null
        val animal = find(data.animals, all.filter { it != "גור" && it != "הגור" }) ?: return null
        return when {
            baby -> if (animal.baby == "-") CommandResult("אין שם מיוחד לצאצא של ${animal.name}")
                    else CommandResult("הצאצא של ${animal.name} נקרא ${animal.baby}")
            group -> CommandResult("${animal.name} שייך ל${animal.group}")
            else -> CommandResult("${animal.name}: ${animal.sound}")
        }
    }

    // --- כוכבי לכת -------------------------------------------------------

    private val ORDINALS = listOf("הראשון", "השני", "השלישי", "הרביעי", "החמישי", "השישי", "השביעי", "השמיני")

    private fun planet(data: Data, text: String, all: List<String>): CommandResult? {
        val moonsCue = text.hasAny(listOf("ירחים", "כמה ירח", "יש ירח"))
        val orderCue = text.hasAny(listOf("מהשמש", "במקום", "איזה מקום", "באיזה מקום"))
        val factCue = text.hasAny(listOf("ספר לי על", "תספר לי על", "עובדה על", "מה אתה יודע על", "מה זה", "תספר על"))
        if (!moonsCue && !orderCue && !factCue) return null
        val planet = find(data.planets, all) ?: return null
        return when {
            moonsCue -> when (planet.moons) {
                0 -> CommandResult("ל${planet.name} אין ירחים")
                1 -> CommandResult("ל${planet.name} יש ירח אחד")
                else -> CommandResult("ל${planet.name} יש בערך ${planet.moons} ירחים ידועים")
            }
            orderCue -> CommandResult("${planet.name} הוא כוכב הלכת ${ORDINALS[planet.order - 1]} מהשמש")
            else -> CommandResult("${planet.name}: ${planet.fact}")
        }
    }

    // --- גימטריה ---------------------------------------------------------

    private val GEMATRIA = mapOf(
        'א' to 1, 'ב' to 2, 'ג' to 3, 'ד' to 4, 'ה' to 5, 'ו' to 6, 'ז' to 7, 'ח' to 8, 'ט' to 9,
        'י' to 10, 'כ' to 20, 'ך' to 20, 'ל' to 30, 'מ' to 40, 'ם' to 40, 'נ' to 50, 'ן' to 50,
        'ס' to 60, 'ע' to 70, 'פ' to 80, 'ף' to 80, 'צ' to 90, 'ץ' to 90, 'ק' to 100, 'ר' to 200,
        'ש' to 300, 'ת' to 400,
    )
    private val GEMATRIA_FRAME = setOf(
        "מה", "הגימטריה", "גימטריה", "בגימטריה", "של", "המילה", "מילה", "כמה", "יוצא", "יוצאת", "זה", "שווה",
        "את", "תחשב", "חשב", "תחשבי", "לי", "המספר", "השם", "הביטוי", "בבקשה", "היא", "הוא",
    )

    private fun gematria(text: String, all: List<String>): CommandResult? {
        if (!text.contains("גימטריה")) return null
        val target = all.filter { it !in GEMATRIA_FRAME }.joinToString(" ")
        val value = target.sumOf { GEMATRIA[it] ?: 0 }
        if (target.isBlank() || value == 0) return null
        return CommandResult("הגימטריה של $target היא $value")
    }

    // --- אישים -----------------------------------------------------------

    private fun person(data: Data, text: String, all: List<String>): CommandResult? {
        if (!text.hasAny(listOf("מי היה", "מי הייתה", "מי היתה", "מי זה", "מי זאת", "מי היו", "ספר לי על", "תספר לי על", "מה אתה יודע על", "מי הוא", "מי היא"))) return null
        val person = find(data.people, all) ?: return null
        return CommandResult("${person.name}: ${person.about}")
    }

    // --- שאלות כלליות ----------------------------------------------------

    private fun trivia(data: Data, text: String): CommandResult? {
        val padded = " $text "
        var best: Trivia? = null
        var bestLength = 0
        data.trivia.forEach { t ->
            t.phrasings.forEach { p ->
                if (p.length > bestLength && p.contains(' ') && padded.contains(" $p ")) { best = t; bestLength = p.length }
            }
        }
        return best?.let { CommandResult(it.answer) }
    }

    // --- עזר: שמות אותיות לועזיות לקול ------------------------------------

    private val LETTER_NAMES = mapOf(
        'A' to "איי", 'B' to "בי", 'C' to "סי", 'D' to "די", 'E' to "אי", 'F' to "אף", 'G' to "ג'י",
        'H' to "אייץ'", 'I' to "אַיי", 'J' to "ג'יי", 'K' to "קיי", 'L' to "אל", 'M' to "אם", 'N' to "אן",
        'O' to "או", 'P' to "פי", 'Q' to "קיו", 'R' to "אר", 'S' to "אס", 'T' to "טי", 'U' to "יו",
        'V' to "וי", 'W' to "דאבל יו", 'X' to "אקס", 'Y' to "וואי", 'Z' to "זי",
    )

    private fun spell(latin: String) = latin.uppercase().map { LETTER_NAMES[it] ?: "" }.joinToString(" ")
}
