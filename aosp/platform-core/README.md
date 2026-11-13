# platform-core: הטמעה

overlay של device tree לקטגוריות 1-9 ו-15 מהצ'קליסט (אתחול, ליבה, תזמון, זיכרון, תהליכים, קבצים, אחסון, דרייברים, רשת, חשמל). עץ AOSP לא קיים במחשב הזה, ולכן שום דבר כאן לא נבנה או נבדק. אין נגיעה במכשיר.

## הטמעה בעץ (LineageOS 19.1)
1. להעתיק את `overlay/device/qin/f22pro/` ל-`device/<vendor>/<device>/` ולתקן את `PC_PATH` ב-`platform_core.mk` לשם האמיתי.
2. ב-`device.mk`: `$(call inherit-product, device/.../platform_core.mk)`. ב-`BoardConfig.mk`: `include device/.../BoardConfigPlatformCore.mk`.
3. קרנל (אחרי קבלת המקור): `merge_config.sh -m <defconfig> kernel/futureos_core.fragment`. ל-build אבחון להוסיף גם `futureos_core_debug.fragment`.
4. `fstab.mt6768.futureos_core.fragment` ו-`mtk-kpd.futureos-extra.kl` הם לשילוב ידני בקבצי היצרן, לא להעתקה.
5. sepolicy הוא נקודת פתיחה בלבד: להריץ במכשיר ולהשלים עם audit2allow.

## ידני לפני שזה שימושי
- למלא את ערכי TBD ב-`power_profile.xml` מה-blobs של היצרן.
- למצוא node טעינה ב-sysfs ולהגדיר `FUTUREOS_CHARGE_CTL_PATH` (סעיף 568).
- לגלות scancodes של המקלדת (312), נתיבי controller ל-SD ו-USB (234), וטופולוגיית ליבות (cpuset).
- צומת ramoops ב-DT של היצרן (26), ומפתח AVB משלנו (7, 8).
- שמות משאבים שאינם קיימים בעץ נוספים בשקט ואינם מזיקים, אבל גם לא עושים כלום: לאמת במכשיר.

טבלת ההחלטות לכל סעיף: [STATUS.md](STATUS.md). מדיניות: [docs/PLATFORM_CORE_POLICY.md](docs/PLATFORM_CORE_POLICY.md).
