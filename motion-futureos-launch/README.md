# סרטון ההשקה של FutureOS

סרטון של כ-66 שניות, ‏1920×1080, ‏60fps, שכל פריים בו מחושב בקוד (HTML + three.js), מצולם ב-Chrome ומחובר ב-ffmpeg עם motion blur.

## מה יש כאן

| קובץ | מה בו |
|---|---|
| `fos-data.js` | המידות של דגם Regular מתוך `hardware/openscad/futureos_family.scad`, הטוקנים והאייקונים מהדיזיין סיסטם, הלוגו כווקטור, הטקסטים שעל המסכים |
| `fos-timeline.js` | לוח הזמנים של כל הסרט: כל לחיצה, כל מעבר, כל כיתוב |
| `fos-phone.js` | הגוף בתלת ממד (three.js), נבנה מהמספרים של ה-SCAD |
| `fos-ui.js` | המסכים (640×960), מסגרת המיקוד והמעברים |
| `fos-overlay.js` | קווי השרטוט, קווי המידה, היד, האייקונים שנמחקים, הלוגו בסוף |
| `scene.js` | התנועה של הטלפון והמצלמה, הכיתובים, הסאונד |
| `bed.py` | פס הקול: מסונתז מאפס (120 BPM), לא נלקח משום מקום. יוצר את `bed.wav` |

## פקודות

```bash
python bed.py                 # פס הקול
python render.py check        # בדיקה אוטומטית של הטקסט והתנועה
python render.py preview      # טיוטה בחצי גודל: out/preview.mp4
python render.py full         # הסרטון הסופי: out/final.mp4
```

הפייתון הוא זה של `~/.motion-studio/venv`. אם הדפדפן של Playwright לא מותקן, `render.py` משתמש ב-Chrome שמותקן במחשב.

## להחליף את המוזיקה

שמים את השיר בתיקייה ומשנים ב-`index.html` את `music` ל-`{ file: 'song.mp3', start: <הביט הראשון>, gain: 0 }`.
`python audio.py beat song.mp3` מודד את הקצב. כל הלחיצות יושבות על גריד של רבע ביט ב-120 BPM, אז שיר בקצב אחר צריך הזזה של הזמנים ב-`fos-timeline.js`.
