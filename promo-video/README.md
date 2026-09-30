# Remotion video

<p align="center">
  <a href="https://github.com/remotion-dev/logo">
    <picture>
      <source media="(prefers-color-scheme: dark)" srcset="https://github.com/remotion-dev/logo/raw/main/animated-logo-banner-dark.apng">
      <img alt="Animated Remotion Logo" src="https://github.com/remotion-dev/logo/raw/main/animated-logo-banner-light.gif">
    </picture>
  </a>
</p>

Welcome to your Remotion project!

## Commands

**Install Dependencies**

```console
npm i
```

**Start Preview**

```console
npm run dev
```

**Render video**

```console
npx remotion render
```

**Upgrade Remotion**

```console
npx remotion upgrade
```

## Docs

Get started with Remotion by reading the [fundamentals page](https://www.remotion.dev/docs/the-fundamentals).

## Help

We provide help on our [Discord server](https://discord.gg/6VzzNDwUwV).

## Issues

Found an issue with Remotion? [File an issue here](https://github.com/remotion-dev/remotion/issues/new).

## License

Note that for some entities a company license is needed. [Read the terms here](https://github.com/remotion-dev/remotion/blob/main/LICENSE.md).

---

# ריל לאינסטגרם (1080×1920, 9:16)

סרטון פרומו אנכי של 26 שניות, ‏30fps, עם פס קול. הקומפוזיציה `FutureOSReel`, בתיקייה `src/reel/`. הסרטון המוכן: [`exports/futureos-reel-1080x1920.mp4`](exports/futureos-reel-1080x1920.mp4), וכרטיס כריכה: [`exports/futureos-reel-cover.png`](exports/futureos-reel-cover.png).

**התסריט:** "מסך מגע?" נחצה, ואז "נחזור למקשים." ← הטלפון עולה ← חצים ו-OK במסך הבית ← הקלדת "אני בדרך" ב-T9 ← לחיצה כפולה על OK פותחת את העוזר הקולי ← קיר האייקונים "הכל כאן." ← הלוגו של FutureOS.

- **אותם רכיבים כמו הפרומו הרחב:** `Phone`, המסכים והאייקונים של האפליקציות (בלי שינוי), צבע ההדגשה `#64D2FF`, גופן Heebo. הטלפון הוא הדגם מ-`hardware/openscad`, והמסכים ב-640×960 המקורי.
- **שוליים של אינסטגרם:** הכיתובים יושבים בין 250px מלמעלה ל-340px מלמטה, ו-64px מהצדדים. `showGuides` מצייר את האזורים האלה.
- **הקצב:** 120 BPM, פעימה כל 15 פריימים. כל לחיצה שמאירה מקש בתמונה מקבלת "קליק" בפס הקול, באותו פריים בדיוק. הלחיצות מוגדרות פעם אחת ב-`src/reel/timeline.json`, והאנימציה והסאונד קוראים ממנו.
- **פס הקול** מסונתז מאפס ב-`reel/make-audio.py`: בלי דגימות ובלי שום דבר שהורד.
- **הגופן** נטען מ-`public/fonts` (רישיון OFL), כך שהרינדור לא תלוי ברשת.

```bash
npm ci
python3 reel/make-audio.py                     # פס הקול: public/reel/bed.m4a (צריך numpy ו-scipy)
npm run dev                                    # Studio: הקומפוזיציה FutureOSReel
node reel/render.mjs still 150 300 --guides    # פריימים לבדיקה, עם השוליים: out/stills
node reel/render.mjs video                     # exports/futureos-reel-1080x1920.mp4
```

הייצוא: H.264 (High), ‏yuv420p, צבע BT.709, ‏AAC 192k סטריאו ב-48kHz, ‏CRF 16. `BROWSER=/path/to/chrome-headless-shell` מחליף את הדפדפן כשל-Remotion אין הורדה.
