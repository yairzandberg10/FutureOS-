// סינון פרטים אישיים מטקסט משוב ומלוגים. רץ גם במכשיר (בקוטלין) וגם כאן, לפני שמירה ולפני Gemini.
// עדיף לסנן יותר מדי מאשר פחות: מספר גרסה שנמחק בטעות לא מזיק, מספר טלפון שדלף כן.

const RULES = [
  // מיילים
  [/[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}/g, "[email]"],
  // IMEI / ICCID / IMSI: רצף של 14-20 ספרות
  [/\b\d{14,20}\b/g, "[id]"],
  // טלפונים: +972 / 05x / מספרים בינלאומיים, עם או בלי מפרידים
  [/(?:\+|00)\d{1,3}[\s-]?\d{1,4}(?:[\s-]?\d{2,4}){2,4}/g, "[phone]"],
  [/\b0(?:5\d|[2-4]|[89]|7\d)[\s-]?\d{3}[\s-]?\d{4}\b/g, "[phone]"],
  // קואורדינטות: lat,lng עם 4 ספרות אחרי הנקודה לפחות
  [/-?\d{1,3}\.\d{4,}\s*,\s*-?\d{1,3}\.\d{4,}/g, "[location]"],
  // כתובות IP
  [/\b(?:\d{1,3}\.){3}\d{1,3}\b/g, "[ip]"],
  // MAC (בלוטות', Wi-Fi)
  [/\b(?:[0-9A-Fa-f]{2}[:-]){5}[0-9A-Fa-f]{2}\b/g, "[mac]"],
  // טוקנים ומפתחות שמופיעים בלוג כ-key=value
  [/\b(token|apikey|api_key|key|password|secret|authorization|bearer)\b(\s*[:=]\s*|\s+)[^\s,;"']+/gi, "$1$2[secret]"],
];

export function redact(text) {
  if (typeof text !== "string" || text.length === 0) return "";
  let out = text;
  for (const [pattern, replacement] of RULES) out = out.replace(pattern, replacement);
  return out;
}
