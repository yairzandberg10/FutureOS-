# aosp/tools: שלב M0 (גיבוי, dump וחילוץ blobs)

שלב M0 מ-[docs/aosp-port-plan.md](../../docs/aosp-port-plan.md). **כל הסקריפטים כאן קוראים בלבד.** אין בהם write, erase, unlock או flash, ו-`mtk_read` ב-`common.sh` חוסם כל פקודת mtkclient שאינה קריאה.

| שלב | סקריפט | טלפון? | מה |
|---|---|---|---|
| - | [CHECKLIST.md](CHECKLIST.md) | המשתמש, בהגדרות | גרסת קושחה, OEM unlocking, IMEI |
| 0 | `00_device_info.sh` | ADB | props: גרסה, A/B, dynamic partitions, AVB, OEM unlock, VoLTE, רשימת מחיצות |
| 1 | `01_backup_critical.sh <copy2>` | BROM | **חובה ראשון:** nvram, nvdata, nvcfg, persist, proinfo, protect1/2, md1img, seccfg, lk, preloader... + בדיקה שאינם כולם אפסים + SHA256 + עותק שני |
| 2 | `02_dump_partitions.sh` | BROM | כל המחיצות חוץ מ-userdata/cache/metadata (בערך 8-12GB) |
| 3 | `03_extract_blobs.sh <dump>` | לא | lpunpack, חילוץ erofs/ext4 בלי mount, build.prop, vbmeta, טיוטת `proprietary-files.txt` |

## הכנה
- **מקום:** `OUT` (ברירת מחדל `/d/futureos-m0`). לא על C.
- **mtkclient:** `git clone https://github.com/bkerler/mtkclient ~/mtkclient && pip install -r ~/mtkclient/requirements.txt`. ב-Windows צריך UsbDk, וב-WSL2 צריך `usbipd`. אפשר לשנות מיקום עם `MTK="python3 /path/mtk.py"`.
- **BROM:** מכבים את הטלפון, מריצים את הסקריפט, ומחברים USB תוך לחיצה על שני מקשי העוצמה.
- **phone.lock:** כל סקריפט שמתחבר לטלפון דורש `--i-hold-the-phone-lock`, ומציג מי מחזיק את ה-lock עכשיו.

## אזהרות
- **קושחה 1.1.0 ומעלה:** לפי Hovatek, Qin חסמו בגרסאות האלה את פתיחת ה-bootloader. **לא לעשות downgrade ולא format** (מקרה מתועד של טלפון שנהרס). M0 רק קורא, ולכן אפשר לבצע אותו בכל גרסה. השאלה אם אפשר לפתוח את ה-bootloader מחכה לתוצאות של `00` ולבדיקה ב-mtkclient (`seccfg`).
- המחיצות nvram, nvdata ו-md1img הן היחידות שאי אפשר לשחזר. בלעדיהן הטלפון נשאר בלי IMEI ובלי רשת.
- ה-blobs של MediaTek **לא** עולים לריפו ציבורי.
