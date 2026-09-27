# איך מגיעים למכשיר FutureOS אמיתי - מה עושים, בפשטות

לא מתכננים PCB בעצמך. **משלמים למפעל (ODM) שמתכנן ומייצר את כל הטלפון**: הלוח, התוכנה הבסיסית, הגוף וההסמכות. אתה נותן להם מפרט ומקבל טלפון עובד.

הכול מוכן בתיקייה הזו. מה שנשאר לך זה לשלוח ולבחור.

## הצעדים

1. **שולחים את `RFQ_FutureOS_Regular.md`** (בקשה להצעת מחיר, באנגלית) ל-3 עד 5 מפעלים. נוסח המייל נמצא למטה.
2. **את מי שואלים:**
   - **Duoqin** (היצרן של Qin F22 Pro). הם כבר מייצרים כמעט את המכשיר הזה, ולכן הם המועמד הכי טוב. יוצרים קשר דרך האתר הרשמי שלהם.
   - **Alibaba** ו-**Global Sources**: מחפשים `4G keypad android phone ODM` ובוחרים ספקים עם תג "Verified Manufacturer" ושנות ניסיון.
   - לא פונים למפעלי ענק (Huaqin, Wingtech, Longcheer). הם לא עובדים בכמויות של 3,000 יחידות.
3. **משווים הצעות** לפי 3 דברים: עלות חד-פעמית (NRE), מחיר ליחידה, ו**האם הם מסכימים לתת root וקוד קרנל**. בלי הסעיף האחרון המערכת לא תעבוד (FutureUI, Settings ו-Terminal צריכים `su`).
4. **מזמינים דוגמאות (EVT)** ובודקים אותן עם כל 30 האפליקציות.
5. **הסמכות בישראל**: אישור סוג ממשרד התקשורת ובדיקות VoLTE מול המפעילים. המפעל מספק דוחות, ויועץ רגולציה ישראלי מגיש.

## לכמה כסף ולכמה זמן להתכונן
- **חד-פעמי:** כ-$150K עד $400K לדגם Regular. אם המפעל משתמש בפלטפורמה קיימת שלו, זה יכול לרדת הרבה.
- **ליחידה:** כ-$90.
- **זמן:** 9 עד 15 חודשים עד ייצור.

## המייל לשליחה (להעתיק כמו שהוא)

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
