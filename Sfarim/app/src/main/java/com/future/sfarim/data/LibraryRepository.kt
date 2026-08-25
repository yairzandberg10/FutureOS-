package com.future.sfarim.data

import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import org.json.JSONArray
import com.future.sfarim.util.HebrewNumerals
import com.future.sfarim.util.stripHtmlTags

/** שכבת גישה ל-sefaria.db - שאילתות SQL גולמיות (בלי Room), הכל read-heavy
 * מלבד bookmarks/reading_progress. כל הפעולות סינכרוניות - קרא מ-Dispatchers.IO. */
class LibraryRepository(private val db: SQLiteDatabase) {

    private fun parseIntList(json: String): List<Int> {
        val arr = JSONArray(json)
        return (0 until arr.length()).map { arr.getInt(it) }
    }

    private fun parseStringList(json: String): List<String> {
        val arr = JSONArray(json)
        return (0 until arr.length()).map { arr.getString(it) }
    }

    fun getChildCategories(parentId: Long?): List<LibraryCategory> {
        val cursor = if (parentId == null) {
            db.rawQuery(
                "SELECT id, parent_id, name_en, name_he, sort_order FROM categories WHERE parent_id IS NULL ORDER BY sort_order",
                null,
            )
        } else {
            db.rawQuery(
                "SELECT id, parent_id, name_en, name_he, sort_order FROM categories WHERE parent_id = ? ORDER BY sort_order",
                arrayOf(parentId.toString()),
            )
        }
        return cursor.use { c ->
            buildList {
                while (c.moveToNext()) {
                    add(
                        LibraryCategory(
                            id = c.getLong(0),
                            parentId = if (c.isNull(1)) null else c.getLong(1),
                            nameEn = c.getString(2),
                            nameHe = c.getString(3),
                            sortOrder = c.getInt(4),
                        )
                    )
                }
            }
        }
    }

    private val bookColumns = "id, category_id, title_en, title_he, section_names, sort_order, root_category"

    fun getBooksInCategory(categoryId: Long): List<LibraryBook> {
        val cursor = db.rawQuery(
            "SELECT $bookColumns FROM books WHERE category_id = ? ORDER BY sort_order",
            arrayOf(categoryId.toString()),
        )
        return cursor.use { c -> buildList { while (c.moveToNext()) add(bookFromCursor(c)) } }
    }

    fun getBook(bookId: Long): LibraryBook? {
        val cursor = db.rawQuery(
            "SELECT $bookColumns FROM books WHERE id = ?",
            arrayOf(bookId.toString()),
        )
        return cursor.use { c -> if (c.moveToFirst()) bookFromCursor(c) else null }
    }

    private fun bookFromCursor(c: Cursor): LibraryBook = LibraryBook(
        id = c.getLong(0),
        categoryId = if (c.isNull(1)) null else c.getLong(1),
        titleEn = c.getString(2),
        titleHe = c.getString(3),
        sectionNames = parseStringList(c.getString(4)),
        sortOrder = c.getInt(5),
        rootCategory = c.getString(6),
    )

    /** עצמאי (לא תלוי באובייקט LibraryBook שנטען אסינכרונית בנפרד) - שולף את
     * section_names בעצמו, כדי שלא תהיה שרשרת תלות בין שתי טעינות אסינכרוניות. */
    fun getChapters(bookId: Long): List<BookChapter> {
        val sectionNames = db.rawQuery("SELECT section_names FROM books WHERE id = ?", arrayOf(bookId.toString()))
            .use { c -> if (c.moveToFirst()) parseStringList(c.getString(0)) else emptyList() }
        val cursor = db.rawQuery(
            "SELECT DISTINCT top_index FROM segments WHERE book_id = ? ORDER BY top_index",
            arrayOf(bookId.toString()),
        )
        return cursor.use { c ->
            buildList {
                while (c.moveToNext()) {
                    val topIndex = c.getInt(0)
                    add(BookChapter(bookId, topIndex, HebrewNumerals.chapterLabel(sectionNames, topIndex)))
                }
            }
        }
    }

    fun getSegments(bookId: Long, topIndex: Int): List<LibrarySegment> {
        val cursor = db.rawQuery(
            "SELECT id, book_id, top_index, path, depth, ref_display, text_he, sort_order " +
                "FROM segments WHERE book_id = ? AND top_index = ? ORDER BY sort_order",
            arrayOf(bookId.toString(), topIndex.toString()),
        )
        return cursor.use { c -> buildList { while (c.moveToNext()) add(segmentFromCursor(c)) } }
    }

    fun getSegment(segmentId: Long): LibrarySegment? {
        val cursor = db.rawQuery(
            "SELECT id, book_id, top_index, path, depth, ref_display, text_he, sort_order FROM segments WHERE id = ?",
            arrayOf(segmentId.toString()),
        )
        return cursor.use { c -> if (c.moveToFirst()) segmentFromCursor(c) else null }
    }

    private fun segmentFromCursor(c: Cursor): LibrarySegment = LibrarySegment(
        id = c.getLong(0),
        bookId = c.getLong(1),
        topIndex = c.getInt(2),
        path = parseIntList(c.getString(3)),
        depth = c.getInt(4),
        refDisplay = c.getString(5),
        textHe = c.getString(6),
        sortOrder = c.getInt(7),
    )

    /** מפרשים על פסוק/עמוד מסוים - בלי גרף קישורים נפרד: ספרי פירוש בספריא
     * ("X על Y") משקפים את מבנה ה-path של הספר הבסיסי + רמה אחת נוספת למספור
     * הביאורים, אז אפשר למצוא אותם ע"י התאמת קידומת path בין שני הספרים. */
    fun getCommentaries(baseBook: LibraryBook, pathPrefix: List<Int>): List<CommentaryEntry> {
        if (pathPrefix.isEmpty()) return emptyList()
        val baseSection0 = baseBook.sectionNames.firstOrNull() ?: return emptyList()

        data class Candidate(val id: Long, val title: String, val sectionNames: List<String>)
        val candidates = db.rawQuery(
            "SELECT id, COALESCE(title_he, title_en), section_names FROM books WHERE title_en LIKE ? AND id != ?",
            arrayOf("% on ${baseBook.titleEn}", baseBook.id.toString()),
        ).use { c ->
            buildList {
                while (c.moveToNext()) {
                    val sn = parseStringList(c.getString(2))
                    if (sn.firstOrNull() == baseSection0) {
                        add(Candidate(c.getLong(0), c.getString(1), sn))
                    }
                }
            }
        }

        return buildList {
            for (cand in candidates) {
                val matchDepth = (cand.sectionNames.size - 1).coerceIn(1, pathPrefix.size)
                val likePattern = "[" + pathPrefix.take(matchDepth).joinToString(", ") + ",%"
                db.rawQuery(
                    "SELECT id, top_index, text_he FROM segments WHERE book_id = ? AND path LIKE ? ORDER BY sort_order LIMIT 1",
                    arrayOf(cand.id.toString(), likePattern),
                ).use { c ->
                    if (c.moveToFirst()) {
                        val preview = stripHtmlTags(c.getString(2)).take(80)
                        add(CommentaryEntry(cand.id, cand.title, c.getInt(1), c.getLong(0), preview))
                    }
                }
            }
        }
    }

    /** הופך קלט חופשי לשאילתת FTS5 בטוחה: מסיר תווים מיוחדים של תחביר FTS5,
     * ומוסיף * לכל מילה (חיפוש-לפי-קידומת, מתאים לחיפוש "תוך כדי הקלדה"). */
    private fun buildFtsQuery(userInput: String): String? {
        val cleaned = userInput.replace(Regex("[^\\p{L}\\p{N}\\s]"), " ")
        val tokens = cleaned.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
        if (tokens.isEmpty()) return null
        return tokens.joinToString(" ") { "${it}*" }
    }

    fun searchBooks(query: String, limit: Int = 40): List<BookSearchEntry> {
        val ftsQuery = buildFtsQuery(query) ?: return emptyList()
        val cursor = db.rawQuery(
            "SELECT b.id, b.title_en, b.title_he FROM search_fts f " +
                "JOIN books b ON b.id = f.rowid " +
                "WHERE search_fts MATCH ? ORDER BY rank LIMIT ?",
            arrayOf(ftsQuery, limit.toString()),
        )
        return cursor.use { c ->
            buildList {
                while (c.moveToNext()) {
                    add(BookSearchEntry(c.getLong(0), c.getString(1), c.getString(2)))
                }
            }
        }
    }

    /** חיפוש מלא בתוך גוף הטקסט של כל הפסוקים/הקטעים בספרייה. */
    fun searchSegments(query: String, limit: Int = 40): List<SegmentSearchResult> {
        val ftsQuery = buildFtsQuery(query) ?: return emptyList()
        val cursor = db.rawQuery(
            "SELECT s.id, s.book_id, s.top_index, COALESCE(b.title_he, b.title_en), s.ref_display, " +
                "snippet(segments_fts, 0, '', '', '…', 12) " +
                "FROM segments_fts f " +
                "JOIN segments s ON s.id = f.rowid " +
                "JOIN books b ON b.id = s.book_id " +
                "WHERE segments_fts MATCH ? ORDER BY rank LIMIT ?",
            arrayOf(ftsQuery, limit.toString()),
        )
        return cursor.use { c ->
            buildList {
                while (c.moveToNext()) {
                    add(
                        SegmentSearchResult(
                            segmentId = c.getLong(0),
                            bookId = c.getLong(1),
                            topIndex = c.getInt(2),
                            bookTitle = c.getString(3),
                            refDisplay = c.getString(4),
                            snippet = c.getString(5),
                        )
                    )
                }
            }
        }
    }

    fun addBookmark(bookId: Long, segmentId: Long, note: String?, createdAt: Long) {
        db.execSQL(
            "INSERT INTO bookmarks (book_id, segment_id, note, created_at) VALUES (?, ?, ?, ?)",
            arrayOf<Any?>(bookId, segmentId, note, createdAt),
        )
    }

    fun removeBookmark(bookmarkId: Long) {
        db.execSQL("DELETE FROM bookmarks WHERE id = ?", arrayOf(bookmarkId))
    }

    fun removeBookmarkBySegment(segmentId: Long) {
        db.execSQL("DELETE FROM bookmarks WHERE segment_id = ?", arrayOf(segmentId))
    }

    fun getBookmarkedSegmentIds(bookId: Long): Set<Long> {
        val cursor = db.rawQuery("SELECT segment_id FROM bookmarks WHERE book_id = ?", arrayOf(bookId.toString()))
        return cursor.use { c -> buildSet { while (c.moveToNext()) add(c.getLong(0)) } }
    }

    fun getBookmarks(): List<LibraryBookmark> {
        val cursor = db.rawQuery(
            "SELECT bm.id, bm.book_id, bm.segment_id, bm.note, bm.created_at, " +
                "COALESCE(b.title_he, b.title_en), s.ref_display " +
                "FROM bookmarks bm " +
                "JOIN books b ON b.id = bm.book_id " +
                "JOIN segments s ON s.id = bm.segment_id " +
                "ORDER BY bm.created_at DESC",
            null,
        )
        return cursor.use { c ->
            buildList {
                while (c.moveToNext()) {
                    add(
                        LibraryBookmark(
                            id = c.getLong(0),
                            bookId = c.getLong(1),
                            segmentId = c.getLong(2),
                            note = c.getString(3),
                            createdAt = c.getLong(4),
                            bookTitle = c.getString(5),
                            segmentRef = c.getString(6),
                        )
                    )
                }
            }
        }
    }

    fun setReadingProgress(bookId: Long, segmentId: Long, updatedAt: Long) {
        db.execSQL(
            "INSERT OR REPLACE INTO reading_progress (book_id, segment_id, updated_at) VALUES (?, ?, ?)",
            arrayOf(bookId, segmentId, updatedAt),
        )
    }

    /** רשומת ההתקדמות האחרונה בכל הספרייה - ל"המשך קריאה" במסך הבית. */
    fun getLatestReadingProgress(): ReadingProgressEntry? {
        val cursor = db.rawQuery(
            "SELECT rp.book_id, rp.segment_id, rp.updated_at, " +
                "COALESCE(b.title_he, b.title_en), s.ref_display " +
                "FROM reading_progress rp " +
                "JOIN books b ON b.id = rp.book_id " +
                "JOIN segments s ON s.id = rp.segment_id " +
                "ORDER BY rp.updated_at DESC LIMIT 1",
            null,
        )
        return cursor.use { c ->
            if (c.moveToFirst()) {
                ReadingProgressEntry(
                    bookId = c.getLong(0),
                    segmentId = c.getLong(1),
                    updatedAt = c.getLong(2),
                    bookTitle = c.getString(3),
                    segmentRef = c.getString(4),
                )
            } else null
        }
    }
}
