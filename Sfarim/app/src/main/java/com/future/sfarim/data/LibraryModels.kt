package com.future.sfarim.data

/** קטגוריה בעץ הספרייה (למשל תנ"ך > תורה) - יכולה להכיל גם תת-קטגוריות וגם ספרים ישירות. */
data class LibraryCategory(
    val id: Long,
    val parentId: Long?,
    val nameEn: String,
    val nameHe: String?,
    val sortOrder: Int,
)

/** ספר (כותר) בתוך קטגוריה. sectionNames מגדיר את סכמת המספור (למשל פרק/פסוק). */
data class LibraryBook(
    val id: Long,
    val categoryId: Long?,
    val titleEn: String,
    val titleHe: String?,
    val sectionNames: List<String>,
    val sortOrder: Int,
    val rootCategory: String?,
) {
    val displayTitle: String get() = titleHe?.takeIf { it.isNotBlank() } ?: titleEn

    /** האם זה טקסט "בסיס" (תנ"ך/משנה עצמם, לא פירוש עליהם) - לפי המוסכמה של ספריא
     * שמוסיפה "Comment" כרמת מספור אחרונה לכל פירוש, בכל קורפוס. */
    private val isCommentary: Boolean get() = sectionNames.lastOrNull() == "Comment"

    /** מצב תצוגה: פסוק-בפסוק עם תוויות מספור (רק תנ"ך/משנה בסיסיים), או טקסט שוטף
     * ללא תוויות (הכל השאר - גמרא, פירושים, הלכה וכו'). */
    val isVerseStyle: Boolean get() = !isCommentary && (rootCategory == "Tanakh" || rootCategory == "Mishnah")

    /** המילה המתאימה לתיאור יחידת תוכן אחת בספר הזה, לשימוש בתפריט האופציות. */
    val contentLabel: String
        get() = when {
            rootCategory == "Tanakh" && !isCommentary -> "פסוק"
            rootCategory == "Mishnah" && !isCommentary -> "משנה"
            rootCategory == "Talmud" -> "עמוד"
            isCommentary -> "פירוש"
            else -> "קטע"
        }
}

/** פרק בתוך ספר - נגזר מ-DISTINCT top_index, לא שורה אמיתית בטבלה. */
data class BookChapter(
    val bookId: Long,
    val topIndex: Int,
    val label: String,
)

/** קטע טקסט בודד (פסוק/הלכה/שורה) - יחידת הקריאה הבסיסית. */
data class LibrarySegment(
    val id: Long,
    val bookId: Long,
    val topIndex: Int,
    val path: List<Int>,
    val depth: Int,
    val refDisplay: String,
    val textHe: String,
    val sortOrder: Int,
)

data class LibraryBookmark(
    val id: Long,
    val bookId: Long,
    val segmentId: Long,
    val note: String?,
    val createdAt: Long,
    val bookTitle: String,
    val segmentRef: String,
)

data class ReadingProgressEntry(
    val bookId: Long,
    val segmentId: Long,
    val updatedAt: Long,
    val bookTitle: String,
    val segmentRef: String,
)

/** תוצאת חיפוש ספר (לפי כותר) במסך החיפוש. */
data class BookSearchEntry(
    val id: Long,
    val titleEn: String,
    val titleHe: String?,
) {
    val displayTitle: String get() = titleHe?.takeIf { it.isNotBlank() } ?: titleEn
}

/** תוצאת חיפוש בתוך גוף הטקסט (חיפוש מלא) - כולל הקשר לניווט ישיר לפסוק. */
data class SegmentSearchResult(
    val segmentId: Long,
    val bookId: Long,
    val topIndex: Int,
    val bookTitle: String,
    val refDisplay: String,
    val snippet: String,
)

/** פירוש שנמצא על פסוק/עמוד נתון - נגזר מהתאמת path בין ספר הפירוש לספר הבסיס
 * (ללא צורך בגרף קישורים נפרד - ראו LibraryRepository.getCommentaries). */
data class CommentaryEntry(
    val bookId: Long,
    val bookTitle: String,
    val topIndex: Int,
    val segmentId: Long,
    val preview: String,
)
