# Qin F21 Pro: מידות למגן (16.10.2026)

אין דף מפרט רשמי עם שרטוט. המידות נאספו מאתרי מפרט ומביקורות, ונבדקו זו מול זו. **כשהיחידות מגיעות (3 יחידות בהזמנה), צריך למדוד עם קליבר ולעדכן את `blender/qin_f21_pro_cover.py`.**

## ודאי (כמה מקורות מסכימים)
| | ערך | מקורות |
|---|---|---|
| גובה × רוחב × עובי | **131.7 × 53.5 × 9.5 מ"מ** (בחנות Qin מעוגל ל-132 × 53 × 9.5) | DevicesFAQ, UL Benchmarks, qinphone.com, kaveet field notes |
| משקל | 100-102 גרם | אותם מקורות |
| מסך | 2.8", ‏480×640 (3:4), מגע | כולם |
| מצלמות | 5MP אחורית עם AF ופלאש LED, 2MP קדמית | DroidAfrica, kaveet |
| **מצלמה קדמית** | **בשוליים התחתונים, מתחת למקלדת** (לא למעלה) | ביקורת JTech / binboupan |
| **מקשי עוצמה** | **אין** בצד. העוצמה נשלטת בלחיצה ארוכה על 8 ו-0 | Digital Nomad Hardware, JTech |
| מקש הפעלה | **בצד ימין** | DevicesFAQ |
| טעינה | USB-C, ‏7.5W. **אין שקע 3.5 מ"מ** | DroidAfrica, kaveet |
| SIM | מגש Nano בצד, עם **חור מיקרופון סמוך שקל לבלבל עם חור ההוצאה** | ביקורת JTech, manuals.plus |
| מיקרופונים / רמקול | 2 מיקרופונים (ראשי + סינון רעשים), רמקול מונו | kaveet |
| מקלדת | 21 מקשים: 0-9, ‏*, ‏#, שיחה, הפעלה/ניתוק, חזרה, 4 כיוונים + אישור, מקש "ינשוף" | AlikornSause notes |

## לא ודאי (הערכה במגן, עם פתחים רחבים)
| | מה במגן | למה לא ודאי |
|---|---|---|
| רדיוס פינות | `PHONE_R = 6.0`, ‏`PHONE_EDGE = 2.0` | לא נמדד באף מקור |
| מצלמה + פלאש אחוריים | חלון **30 × 18 מ"מ**, במרכז, 14 מ"מ מלמעלה | אין מקור עם מיקום. החלון רחב כדי לכסות מיקום משמאל עד מרכז |
| מקש הפעלה (צד ימין) | חלון פתוח **30 מ"מ**, 38 מ"מ מלמעלה | הצד ידוע, הגובה לא. **פתוח ולא ממברנה**, כדי לא לחסום אם המיקום שגוי |
| SIM + מיקרופון (צד) | חלון פתוח **36 מ"מ** בצד **שמאל**, 45 מ"מ מלמעלה | הצד לא מתועד. אם הוא בימין: להזיז ל-`VOLUME`/`SIM` |
| תחתית | חלון **22 מ"מ** סביב USB-C (כולל מיקרופון/רמקול אפשריים) | מיקום המיקרופון והרמקול לא ידוע |
| חלק עליון | חלון 10 מ"מ במרכז | אולי מיקרופון סינון רעשים |
| מצלמת סלפי | השפה הקדמית נכנסת רק 1.5 מ"מ (ב-F22: ‏1.8) | הגובה המדויק מעל הקצה לא ידוע |

## מה למדוד כשהיחידות מגיעות
1. רדיוס הפינות (שבלונת רדיוס, או צילום מלמעלה ליד סרגל).
2. מרכז העדשה האחורית והפלאש: מרחק מהקצה העליון ומהשמאלי, וגודל.
3. מקש ההפעלה: מרחק מלמעלה, אורך, ובליטה.
4. מגש SIM וחור המיקרופון: איזה צד, מרחק מלמעלה, אורך.
5. בתחתית: USB-C, מיקרופון, רמקול (מרחק מהמרכז).
6. מצלמת סלפי: מרחק מהקצה התחתון.
אחרי המדידה: לעדכן את הבלוק "פתחים", להקטין את החלונות לגודל אמיתי + 1.5 מ"מ, ולשקול ממברנה על מקש ההפעלה (כמו ב-F22).

## מקורות
- [DevicesFAQ – Qin F21 Pro specs](https://www.devicesfaq.com/en/characteristics/xiaomi-qin-f21-pro)
- [DevicesFAQ – restart (power button side)](https://www.devicesfaq.com/en/restart/xiaomi-qin-f21-pro)
- [UL Benchmarks – Qin F21 Pro](https://benchmarks.ul.com/hardware/phone/Qin+F21+Pro+review)
- [qinphone.com – F21 Pro](https://qinphone.com/products/qin-f21-pro-smart-keypad-phone-compact-2-8-inch-touchscreen-android-11-4g-lte-single-camera-google-play-support-ideal-backup-work-phone-porcelain-white-iron-grey)
- [Field Notes – hardware variants](https://kaveet.github.io/dumbphones/qin-f21-pro/hardware-variants.html)
- [DroidAfrica – Qin F21 Pro](https://droidafrica.net/gadget/qin-f21-pro/)
- [JTech Forums – F21 Pro review](https://jtechforums.org/t/qin-f21-pro-us-modified-review/706)
- [Digital Nomad Hardware – F21 Pro](https://digitalnomadhardware.com/xiaomi-qin-f21-pro-smartphone/)
- [AlikornSause – Notes on Qin F21 Pro](https://github.com/AlikornSause/Notes-on-QIN-F21-PRO)
