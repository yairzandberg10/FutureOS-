# איך מגיעים למכשיר FutureOS אמיתי - מה עושים, בפשטות

> **ל-10 יחידות זה לא המסלול.** מפעלים (ODM) עובדים מ-3,000 יחידות ומעלה. ל-10 יחידות: [PROTOTYPE_10_UNITS.md](../PROTOTYPE_10_UNITS.md). הקבצים כאן נשארים לשלב של ייצור המוני. שים לב: מידות הגוף בהם (10 מ"מ) הן של הגרסה הקודמת, לפני ההקטנה ל-8 מ"מ.

לא מתכננים PCB בעצמך. **משלמים למפעל (ODM) שמתכנן ומייצר את כל הטלפון**: הלוח, התוכנה הבסיסית, הגוף וההסמכות. אתה נותן להם מפרט ומקבל טלפון עובד.

הכול מוכן בתיקייה הזו. מה שנשאר לך זה לשלוח ולבחור.

## הצעדים

1. **שולחים את ה-RFQ** (בקשה להצעת מחיר, באנגלית) ל-3 עד 5 מפעלים. נוסחי המייל נמצאים למטה.
   | דגם | קובץ | יעד ליחידה | הזמנה ראשונה |
   |---|---|---|---|
   | **Regular** ‏3.5" (מתחילים ממנו) | `RFQ_FutureOS_Regular.md` | $90 (₪335) | 3,000 |
   | **Mini** ‏2.8" | `RFQ_FutureOS_Mini.md` | $70 (₪260), עם 4GB ‏$77 | 5,000 |
   | **Pro** ‏4.5" ריבועי | `RFQ_FutureOS_Pro.md` | $122 (₪450), 5G בנפרד | 3,000-5,000 |

   ב-Mini המסך בהזמנה אישית (אין פאנל 2:3 מדף), ולכן ההזמנה הראשונה גדולה יותר. ב-Pro יש פאנל ריבועי קיים. אפשר לשלוח לאותו מפעל את שלושת הקבצים ביחד.
2. **את מי שואלים:**
   - **Duoqin** (היצרן של Qin F22 Pro). הם כבר מייצרים כמעט את המכשיר הזה, ולכן הם המועמד הכי טוב. יוצרים קשר דרך האתר הרשמי שלהם.
   - **Alibaba** ו-**Global Sources**: מחפשים `4G keypad android phone ODM` ובוחרים ספקים עם תג "Verified Manufacturer" ושנות ניסיון.
   - לא פונים למפעלי ענק (Huaqin, Wingtech, Longcheer). הם לא עובדים בכמויות של 3,000 יחידות.
3. **משווים הצעות** בטבלה `quote_comparison.csv` (נפתחת ב-Excel), לפי 3 דברים עיקריים: עלות חד-פעמית (NRE), מחיר ליחידה, ו**האם הם מסכימים לתת root וקוד קרנל**. בלי הסעיף האחרון המערכת לא תעבוד (FutureUI, Settings ו-Terminal צריכים `su`).
4. **מזמינים דוגמאות (EVT)** ובודקים אותן עם כל 30 האפליקציות.
5. **הסמכות בישראל**: אישור סוג ממשרד התקשורת ובדיקות VoLTE מול המפעילים. המפעל מספק דוחות, ויועץ רגולציה ישראלי מגיש.

## לכמה כסף ולכמה זמן להתכונן
| | Regular | Mini | Pro |
|---|---|---|---|
| **חד-פעמי (NRE)** | $150K-400K | $200K-450K (כולל פיתוח מסך) | $200K-450K |
| **ליחידה** | ~$90 | ~$70 | ~$122 |
| **זמן עד ייצור** | 9-15 חודשים | 12-18 חודשים | 12-18 חודשים |

אם המפעל משתמש בפלטפורמה קיימת שלו, ה-NRE יכול לרדת הרבה. הסדר המומלץ: Regular, אחריו Mini, ואחריו Pro.

## המיילים לשליחה (להעתיק כמו שהם)

### מייל ל-Regular

> **Subject:** RFQ - 4G keypad Android phone, ODM, 3,000-10,000 units
>
> Hello,
>
> We are developing a 4G Android keypad phone (3.5" 640×960, no touchscreen, Helio G85 class) for the Israeli market. The software is complete. We are looking for an ODM partner for the hardware, BSP and mass production.
>
> The attached RFQ has the full specification. Key points: VoLTE with Israeli operators, root access and kernel source for our own AOSP build, first order of 3,000 units, target unit price around USD 90.
>
> Please send your quotation (NRE + unit price + schedule), and tell us whether you already have a keypad phone platform we could reuse.
>
> Best regards,
> [השם שלך]

### מייל ל-Mini

> **Subject:** RFQ - small 4G keypad Android phone (2.8"), ODM, 5,000-10,000 units
>
> Hello,
>
> We are developing a compact 4G Android keypad phone (2.8" 480×720, no touchscreen, Unisoc T606 class) for the Israeli market. The software is complete. We are looking for an ODM partner for the hardware, BSP, custom display sourcing and mass production.
>
> The attached RFQ has the full specification. Key points: VoLTE with Israeli operators, root access and kernel source for our own AOSP build, first order of 5,000 units, target unit price around USD 70.
>
> Please send your quotation (NRE + unit price + schedule), and tell us whether you already have a small keypad phone platform or a 2:3 panel we could reuse.
>
> Best regards,
> [השם שלך]

### מייל ל-Pro

> **Subject:** RFQ - 4G square-screen keypad Android phone (4.5"), ODM, 3,000-10,000 units
>
> Hello,
>
> We are developing a 4G Android keypad phone (4.5" 1440×1440 square screen, no touchscreen, full numeric keypad, Helio G99 class) for the Israeli market. The software is complete. We are looking for an ODM partner for the hardware, BSP, display sourcing and mass production.
>
> The attached RFQ has the full specification. Key points: VoLTE with Israeli operators, root access and kernel source for our own AOSP build, first order of 3,000-5,000 units, target unit price around USD 122 (please also quote a 5G variant).
>
> Please send your quotation (NRE + unit price + schedule), and tell us whether you have worked with square 1440×1440 panels before.
>
> Best regards,
> [השם שלך]
