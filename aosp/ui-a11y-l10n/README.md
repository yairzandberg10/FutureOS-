# ui-a11y-l10n: overlay לקטגוריות 16-20

קבצים מוכנים להשלכה לעץ AOSP 12 (לא סונכרן כאן). הכל תחת `overlay/vendor/futureos/` ומשקף את מבנה `vendor/futureos/` מ-`docs/aosp-port-plan.md`.

| קובץ | תפקיד |
|-|-|
| `overlay/FutureOSFrameworkOverlay/` | RRO ל-`android`: Night Light, בהירות אוטומטית, מגבלת עוצמה |
| `overlay/FutureOSSettingsProviderOverlay/` | RRO ל-`com.android.providers.settings`: IME, קיצור נגישות, אנימציות |
| `config/ui_a11y_l10n.mk` | locales (עברית ראשונה), Asia/Jerusalem, מאפייני תצוגה, `PRODUCT_PACKAGES` |
| `fonts/fonts_customization.xml` | תבנית לגופן Heebo (קבצי ה-TTF לא כלולים) |

## הטמעה
1. להעתיק את `overlay/vendor/futureos/*` לתוך `vendor/futureos/` בעץ.
2. ב-`vendor/futureos/config/futureos.mk`: `$(call inherit-product, vendor/futureos/config/ui_a11y_l10n.mk)`.
3. **לאמת שמות משאבים לפני בנייה** (נכתבו מהזיכרון): `grep -n "def_input_method\|def_enabled_input_methods\|def_accessibility_shortcut\|def_high_text_contrast\|def_window_.*_scale\|def_screen_brightness_automatic\|def_sound_effects" frameworks/base/packages/SettingsProvider/res/values/defaults.xml`. שם שחסר נמחק מ-`defaults.xml`.
4. `m FutureOSFrameworkOverlay FutureOSSettingsProviderOverlay`.
5. ה-IME `com.future.keyboard/.KeyboardService` חייב להיות בבילד כ-priv-app, ו-`LatinIME` ב-`overrides` של מודול ה-Keyboard.
6. אחרי הבנייה, בדיקות ידניות (ר' STATUS.md, עמודה "מה נשאר ידני").

## מגבלות
- בהירות אוטומטית (`config_automatic_brightness_available`) מניחה חיישן אור וערכי lux לא מכוילים. אם אין חיישן, להשיב ל-`false`.
- קיצור הנגישות פעיל אבל בלי יעד. TalkBack אינו ב-AOSP; אפשר לבנות אותו מ-github.com/google/talkback ולבטל את ההערה ב-`defaults.xml`.
- אין שירות TTS מערכתי ברפו, ולכן סעיפים 710-711 תלויים בעטיפת Piper כ-`TextToSpeechService`.
- צפיפות המסך (`ro.sf.lcd_density`) ו-keylayout של המקשים שייכים ל-device tree ולא נכללו.