package com.future.guide.data

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.automirrored.rounded.Message
import androidx.compose.material.icons.automirrored.rounded.Notes
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.Contacts
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Navigation
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SettingsRemote
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material.icons.rounded.Build
import androidx.compose.ui.graphics.vector.ImageVector

/** תוכן המדריך לכל אפליקציות FutureOS. סטטי בקוד - אין תלות ברשת או במאגר נתונים. */
data class GuideApp(
    val id: String,
    val icon: ImageVector,
    val name: String,
    val subtitle: String,
    val steps: List<String>,
    val tips: List<String> = emptyList()
)

val GUIDE_APPS = listOf(
    GuideApp(
        id = "system",
        icon = Icons.Rounded.PhoneAndroid,
        name = "מסך נעילה ופס עליון",
        subtitle = "FutureUI - השלד של המערכת",
        steps = listOf(
            "פס העליון מציג שעה, חיבור סלולרי וסוללה, ונשלט ע\"י המקש העליון בכל מסך.",
            "ממסך הנעילה, הקש בכל מקש מספרי כדי להעיר את המכשיר, ולאחר מכן אשר עם מקש הבחירה.",
            "מרכז הבקרה (עוצמת קול, פנס, מצב טיסה) נפתח עם צירוף המקשים הייעודי של המכשיר.",
            "מרכז ההתראות מציג הודעות שהתקבלו מכל האפליקציות במקום אחד."
        ),
        tips = listOf("FutureUI רץ ברקע כל הזמן כשירות נגישות - אין צורך לפתוח אותו ישירות.")
    ),
    GuideApp(
        id = "launcher",
        icon = Icons.Rounded.Apps,
        name = "FutureLauncher",
        subtitle = "מסך הבית",
        steps = listOf(
            "השתמשו בחצי הניווט (D-pad) כדי לעבור בין סמלי האפליקציות.",
            "לחיצה על מקש הבחירה פותחת את האפליקציה המסומנת.",
            "לכל אפליקציה ניתן להקצות קיצור מספרי - החזקת המקש המספרי בלחיצה ארוכה פותחת אותה ישירות ממסך הבית.",
            "מקש החזרה תמיד מחזיר למסך הבית מכל אפליקציה פתוחה."
        )
    ),
    GuideApp(
        id = "dialer",
        icon = Icons.Rounded.Call,
        name = "טלפון",
        subtitle = "חיוג ושיחות",
        steps = listOf(
            "הקישו את המספר על המקלדת המספרית ולחצו על מקש השיחה הירוק כדי לחייג.",
            "גללו עם ה-D-pad אל 'אנשי קשר' או 'היסטוריית שיחות' כדי לבחור מספר קיים במקום להקליד.",
            "בשיחה נכנסת, מקש הבחירה עונה ומקש החזרה/אדום מנתק.",
            "במהלך שיחה ניתן לעבור לרמקול או להשתיק דרך תפריט השיחה הפעילה."
        ),
        tips = listOf("החזקה ארוכה על ספרה בהיסטוריה פותחת אפשרויות מהירות: חיוג חוזר, הודעה, הוספה לאנשי קשר.")
    ),
    GuideApp(
        id = "messages",
        icon = Icons.AutoMirrored.Rounded.Message,
        name = "הודעות",
        subtitle = "הודעות SMS",
        steps = listOf(
            "מרשימת השיחות, בחרו שיחה קיימת או פתחו שיחה חדשה כדי להתחיל להקליד.",
            "הקלדת הטקסט נעשית עם מקלדת ה-T9 החזויה - לחיצות חוזרות על מקש בוחרות בין האותיות שעליו.",
            "מקש הבחירה שולח את ההודעה; מקש החזרה חוזר לרשימת השיחות.",
            "התראות על הודעות חדשות מופיעות במרכז ההתראות של FutureUI."
        )
    ),
    GuideApp(
        id = "notes",
        icon = Icons.AutoMirrored.Rounded.Notes,
        name = "פתקים",
        subtitle = "רשימת פתקים",
        steps = listOf(
            "ברשימת הפתקים, מקש הבחירה על פתק קיים פותח אותו לעריכה.",
            "כדי ליצור פתק חדש, נווטו לכפתור ההוספה ולחצו בחירה.",
            "מקש החזרה שומר את הפתק; אפשר גם לשמור בכפתור השמירה שבראש המסך.",
            "נעיצת פתק (או ביטול נעיצה) נעשית מתפריט האפשרויות - ברשימה על הפתק הממוקד, או בתוך הפתק הפתוח. פתק נעוץ מסומן בסיכה.",
            "מחיקת פתק מתבצעת מתוך תפריט האפשרויות של הפתק הפתוח."
        )
    ),
    GuideApp(
        id = "calendar",
        icon = Icons.Rounded.CalendarMonth,
        name = "לוח שנה",
        subtitle = "לוח עברי, דף יומי וזמנים",
        steps = listOf(
            "לוח החודש מציג תאריך עברי ולועזי יחד - נווטו בין הימים עם ה-D-pad.",
            "בחירת יום מציגה את זמני היום (הנץ, שקיעה וכו') המחושבים לפי המיקום שהוגדר במכשיר.",
            "מסך 'דף יומי' מציג את הדף הנלמד היום במחזור הדף היומי.",
            "ניתן להוסיף אירועים אישיים ליום נבחר דרך תפריט ההוספה."
        ),
        tips = listOf("זמני היום מבוססים על מיקום המכשיר - ודאו ששירותי המיקום פעילים לדיוק מרבי.")
    ),
    GuideApp(
        id = "contact",
        icon = Icons.Rounded.Contacts,
        name = "אנשי קשר",
        subtitle = "ניהול אנשי קשר",
        steps = listOf(
            "גללו את הרשימה עם ה-D-pad, או הקישו את ספרות השם במקלדת T9 (מקש 2 = א/ב/ג וכן הלאה) כדי לסנן את הרשימה בזמן אמת.",
            "מקש הבחירה על איש קשר פותח את פרטיו - משם ניתן לחייג או לשלוח הודעה ישירות.",
            "ליצירת איש קשר חדש, נווטו לכפתור ההוספה בראש הרשימה.",
            "עריכה ומחיקה של איש קשר קיימים זמינות דרך תפריט הפרטים שלו."
        )
    ),
    GuideApp(
        id = "files",
        icon = Icons.Rounded.Folder,
        name = "קבצים",
        subtitle = "דפדפן קבצים",
        steps = listOf(
            "נווטו בין תיקיות עם מקש הבחירה כדי להיכנס פנימה, ומקש החזרה כדי לצאת תיקייה אחת אחורה.",
            "החזקה ארוכה על קובץ פותחת תפריט פעולות: העתקה, העברה, מחיקה ושיתוף.",
            "בחירת קובץ תומך פותחת אותו באפליקציה המתאימה (למשל תמונה בגלריה, שיר במוזיקה)."
        )
    ),
    GuideApp(
        id = "keyboard",
        icon = Icons.Rounded.Keyboard,
        name = "מקלדת",
        subtitle = "קלט טקסט T9 חזוי",
        steps = listOf(
            "בכל שדה טקסט במערכת, המקלדת המספרית הופכת אוטומטית לקלט T9: כל מקש מייצג כמה אותיות.",
            "הקלידו את הרצף המספרי של המילה - המקלדת מנחשת את המילה הסבירה ביותר.",
            "מקש # בדרך כלל מחליף בין הצעות מילה כשיש כמה אפשרויות תואמות.",
            "לחיצה ארוכה על מקש מציגה תפריט לבחירת אות ספציפית ידנית, לשמות או מילים לא נפוצות."
        )
    ),
    GuideApp(
        id = "gallery",
        icon = Icons.Rounded.PhotoLibrary,
        name = "גלריה",
        subtitle = "תמונות וסרטונים",
        steps = listOf(
            "רשת התמונות נגללת עם ה-D-pad; מקש הבחירה פותח תמונה או סרטון במסך מלא.",
            "בתצוגה המלאה, ימין/שמאל עוברים לתמונה הבאה/קודמת באלבום.",
            "תפריט האפשרויות בתצוגה המלאה מאפשר מחיקה, שיתוף והגדרה כתמונת רקע."
        )
    ),
    GuideApp(
        id = "music",
        icon = Icons.Rounded.MusicNote,
        name = "מוזיקה",
        subtitle = "נגן מוזיקה מקומי",
        steps = listOf(
            "רשימת השירים המקומית נטענת אוטומטית מזיכרון המכשיר - בחרו שיר עם מקש הבחירה כדי לנגן.",
            "בזמן ניגון, מקשי הניווט משמשים להשהיה/המשך, מעבר לשיר הבא/קודם ושליטה בעוצמה.",
            "ניתן ליצור ולערוך רשימות השמעה דרך תפריט הספרייה."
        )
    ),
    GuideApp(
        id = "sfarim",
        icon = Icons.AutoMirrored.Rounded.MenuBook,
        name = "בלכתך בדרך",
        subtitle = "ספריית טקסטים תורניים",
        steps = listOf(
            "חפשו ספר או מסכת ברשימה הראשית, או השתמשו בחיפוש כדי לקפוץ ישירות למקור.",
            "בתוך הטקסט, ה-D-pad למעלה/למטה גולל בין השורות והעמודים.",
            "מקש הבחירה על מונח בטקסט (במידה וקיים קישור) פותח פירוש או מקור קשור."
        ),
        tips = listOf("מאגר הטקסטים גדול (ספריית ספריא) ונטען מקומית מהמכשיר - אין צורך בחיבור לאינטרנט לשימוש רגיל.")
    ),
    GuideApp(
        id = "terminal",
        icon = Icons.Rounded.Terminal,
        name = "טרמינל",
        subtitle = "שורת פקודה עם הרשאות root",
        steps = listOf(
            "הקלידו פקודת שורת פקודה (shell) בשדה הקלט ולחצו על מקש הבחירה כדי להריץ אותה.",
            "הפלט מוצג מעל שדה הקלט; גללו למעלה כדי לראות פקודות והיסטוריה קודמות.",
            "לאפליקציה יש הרשאות root - יש להשתמש בזהירות, במיוחד בפקודות שמוחקות או משנות קבצי מערכת."
        ),
        tips = listOf("מיועד למשתמשים מנוסים בלבד - פקודה שגויה יכולה לפגוע בפעולת המכשיר.")
    ),
    GuideApp(
        id = "tools",
        icon = Icons.Rounded.Build,
        name = "כלים",
        subtitle = "ממיר יחידות, סורק QR, פתקים מהירים וכלים קטנים נוספים",
        steps = listOf(
            "ממסך הבית של האפליקציה, בחרו את הכלי הרצוי מהרשימה עם ה-D-pad ולחצו OK.",
            "המחשבון, שעון העצר והפנס עברו לאפליקציות עצמאיות משלהם ואינם חלק מכלים יותר.",
            "בסורק ה-QR, כוונו את המצלמה לקוד והתוצאה תוצג אוטומטית.",
            "מקש החזרה בכל כלי חוזר לרשימת הכלים הראשית."
        )
    ),
    GuideApp(
        id = "tasks",
        icon = Icons.Rounded.Checklist,
        name = "משימות",
        subtitle = "רשימת מטלות",
        steps = listOf(
            "גללו בין המשימות עם ה-D-pad; מקש הבחירה פותח משימה לעריכה.",
            "בכפתור ההוספה שבראש המסך יוצרים משימה חדשה - כותרת, הערות ודרגת עדיפות.",
            "שדה החיפוש שבראש הרשימה מסנן לפי כותרת בזמן הקלדה.",
            "מקש החזרה שומר את המשימה הפתוחה ויוצא; משימה ריקה לגמרי לא נשמרת."
        ),
        tips = listOf("צבע הנקודה לצד כל משימה מציין את דרגת העדיפות שלה.")
    ),
    GuideApp(
        id = "bluetooth",
        icon = Icons.Rounded.Bluetooth,
        name = "בלוטות'",
        subtitle = "חיבור אוזניות ומכשירים",
        steps = listOf(
            "מקש הבחירה על מתג ההפעלה מדליק ומכבה את הבלוטות'.",
            "לאחר ההדלקה מתחיל סריקה אוטומטית - המכשירים שנמצאו מופיעים ברשימה מתחת.",
            "מקש הבחירה על מכשיר ברשימה מתחיל התאמה (pairing) איתו.",
            "מכשיר שכבר מותאם מופיע בנפרד, ואפשר להתנתק ממנו או להסיר אותו דרך אותו מקש."
        )
    ),
    GuideApp(
        id = "navigation",
        icon = Icons.Rounded.Navigation,
        name = "ניווט",
        subtitle = "מפות, מסלול ותחבורה ציבורית",
        steps = listOf(
            "הקלידו יעד בשדה החיפוש; מקש למטה מעביר מהשדה לרשימת התוצאות.",
            "מקש הבחירה על תוצאה מציג את המסלול אליה ואת זמן ההגעה המשוער.",
            "מקומות שמורים נגישים ממסך נפרד, כדי לא להקליד כתובת קבועה שוב ושוב.",
            "מקש החזרה מבטל ניווט פעיל וחוזר למסך הראשי."
        ),
        tips = listOf("הניווט דורש חיבור לאינטרנט ומיקום פעיל - בלעדיהם אין חישוב מסלול.")
    ),
    GuideApp(
        id = "camera",
        icon = Icons.Rounded.PhotoCamera,
        name = "מצלמה",
        subtitle = "צילום תמונות ווידאו",
        steps = listOf(
            "מקש הבחירה מצלם תמונה.",
            "מקשי החצים מחליפים בין מצב תמונה למצב וידאו ובין המצלמה הקדמית לאחורית.",
            "התמונות נשמרות בגלריה ונגישות משם מיד אחרי הצילום.",
            "מקש החזרה סוגר את המצלמה."
        )
    ),
    GuideApp(
        id = "fitness",
        icon = Icons.Rounded.FitnessCenter,
        name = "כושר",
        subtitle = "צעדים, אימונים ודופק",
        steps = listOf(
            "המסך הראשי מציג את הצעדים של היום ואת ההתקדמות מול היעד.",
            "בונה האימונים מאפשר להרכיב אימון מתרגילים ולשמור אותו לשימוש חוזר.",
            "במסך ההגדרות מזינים גיל, משקל ויעד יומי - הם בסיס חישוב הקלוריות.",
            "חיישן דופק חיצוני בבלוטות' מתחבר דרך אותו מסך הגדרות."
        )
    ),
    GuideApp(
        id = "remote",
        icon = Icons.Rounded.SettingsRemote,
        name = "שלט רחוק",
        subtitle = "שליטה במזגן ובמכשירי אינפרא-אדום",
        steps = listOf(
            "הוסיפו מכשיר חדש ותנו לו שם - לדוגמה \"מזגן סלון\".",
            "לכל מכשיר מוסיפים כפתורים; כל כפתור שומר קוד אינפרא-אדום (NEC או רצף גולמי).",
            "מקש הבחירה על כפתור שולח את הקוד דרך משדר האינפרא-אדום של המכשיר.",
            "מקש החזרה חוזר מרשימת הכפתורים לרשימת המכשירים."
        ),
        tips = listOf("נדרש משדר אינפרא-אדום מובנה במכשיר - בלעדיו הכפתורים לא ישדרו דבר.")
    ),
    GuideApp(
        id = "assistant",
        icon = Icons.Rounded.Mic,
        name = "עוזר קולי",
        subtitle = "זיהוי דיבור והקראה מקומיים",
        steps = listOf(
            "מקש הבחירה מתחיל האזנה; דברו ואז המתינו לסיום הזיהוי.",
            "הטקסט שזוהה מוצג על המסך, ואפשר להקריא אותו בחזרה.",
            "העיבוד מתבצע במכשיר עצמו - אין צורך בחיבור לאינטרנט.",
            "מקש החזרה עוצר האזנה פעילה."
        ),
        tips = listOf("נדרשת הרשאת מיקרופון; בלעדיה ההאזנה לא תתחיל.")
    ),
    GuideApp(
        id = "settings",
        icon = Icons.Rounded.Settings,
        name = "הגדרות",
        subtitle = "הגדרות מערכת",
        steps = listOf(
            "נווטו בין קטגוריות ההגדרות (רשת, צליל, תצוגה, אבטחה ועוד) עם ה-D-pad.",
            "מצב כהה/בהיר וצבע ההדגשה שנקבעים כאן משפיעים על כל אפליקציות FutureOS, כולל אפליקציית המדריך הזו.",
            "שינויים נשמרים מיידית - אין צורך בכפתור 'שמור' נפרד ברוב המסכים."
        )
    )
)

fun findGuideApp(id: String): GuideApp? = GUIDE_APPS.firstOrNull { it.id == id }
