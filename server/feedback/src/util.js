// עזרים משותפים: תשובות JSON, טוקנים, hash, השוואה בזמן קבוע, ומונים לחלונות זמן.

export class HttpError extends Error {
  constructor(status, code, message = code) {
    super(message);
    this.status = status;
    this.code = code;
  }
}

export function json(body, status = 200, headers = {}) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "content-type": "application/json; charset=utf-8", "cache-control": "no-store", ...headers },
  });
}

export function errorResponse(err) {
  if (err instanceof HttpError) return json({ error: err.code }, err.status);
  // שגיאה לא צפויה: לא מחזירים פרטים ללקוח (עלולים להכיל חלקים מבקשות או סודות)
  console.error("unexpected", err && err.stack ? err.stack : String(err));
  return json({ error: "internal" }, 500);
}

/** קורא גוף JSON עם תקרת גודל. גוף גדול מדי או לא תקין = 400/413. */
export async function readJson(request, maxBytes) {
  const declared = Number(request.headers.get("content-length") || 0);
  if (declared > maxBytes) throw new HttpError(413, "too_large");
  const buf = await request.arrayBuffer();
  if (buf.byteLength > maxBytes) throw new HttpError(413, "too_large");
  try {
    const value = JSON.parse(new TextDecoder().decode(buf));
    if (value === null || typeof value !== "object" || Array.isArray(value)) throw new Error("not an object");
    return value;
  } catch {
    throw new HttpError(400, "bad_json");
  }
}

export function now() {
  return Date.now();
}

export function newId() {
  return crypto.randomUUID();
}

export function base64url(bytes) {
  let s = "";
  for (const b of bytes) s += String.fromCharCode(b);
  return btoa(s).replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/, "");
}

export function newToken() {
  return base64url(crypto.getRandomValues(new Uint8Array(32)));
}

export async function sha256Hex(text) {
  const digest = await crypto.subtle.digest("SHA-256", new TextEncoder().encode(text));
  return [...new Uint8Array(digest)].map((b) => b.toString(16).padStart(2, "0")).join("");
}

/** השוואת מחרוזות באורך זהה בלי לדלוף זמן לפי מיקום ההבדל הראשון. */
export function timingSafeEqual(a, b) {
  if (typeof a !== "string" || typeof b !== "string" || a.length !== b.length) return false;
  let diff = 0;
  for (let i = 0; i < a.length; i++) diff |= a.charCodeAt(i) ^ b.charCodeAt(i);
  return diff === 0;
}

/**
 * מגדיל מונה של חלון זמן ומחזיר true אם עדיין בתוך המגבלה.
 * windowMs קובע את גודל החלון (שעה, יום). חלונות שפגו נמחקים מדי פעם.
 */
export async function takeQuota(db, kind, subject, limit, windowMs) {
  const t = now();
  const windowStart = Math.floor(t / windowMs) * windowMs;
  const key = `${kind}:${subject}:${windowStart}`;
  const row = await db
    .prepare(
      `INSERT INTO counters (key, count, expires_at) VALUES (?1, 1, ?2)
       ON CONFLICT(key) DO UPDATE SET count = count + 1
       RETURNING count`,
    )
    .bind(key, windowStart + windowMs)
    .first();
  if (Math.random() < 0.01) {
    await db.prepare("DELETE FROM counters WHERE expires_at < ?1").bind(t).run();
  }
  return row.count <= limit;
}

export const HOUR = 60 * 60 * 1000;
export const DAY = 24 * HOUR;
