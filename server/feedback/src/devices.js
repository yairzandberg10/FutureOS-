// רישום מכשיר, אימות, משוב, תיבת הודעות ומחיקת מידע.
// כל הנתיבים כאן יחסיים ל-/v1 (המכשיר שומר את /v1 בתוך כתובת הבסיס).

import { HttpError, json, readJson, now, newId, newToken, sha256Hex, timingSafeEqual, takeQuota, HOUR, DAY } from "./util.js";
import { redact } from "./redact.js";

const MAX_TEXT = 4000;
const MAX_LOG = 64 * 1024;
const FEEDBACK_TYPES = new Set(["bug", "idea", "other"]);
const PKG_RE = /^[a-zA-Z][a-zA-Z0-9_]*(\.[a-zA-Z0-9_]+)+$/;
const SHORT_RE = /^[\w.\-+ ()]{0,64}$/;

function shortField(value, name) {
  if (value == null || value === "") return null;
  if (typeof value !== "string" || !SHORT_RE.test(value)) throw new HttpError(400, `bad_${name}`);
  return value;
}

/** POST /register  {model, build} -> {device_id, token} */
export async function register(request, env) {
  const ip = request.headers.get("cf-connecting-ip") || "unknown";
  if (!(await takeQuota(env.DB, "register", ip, 20, HOUR))) throw new HttpError(429, "rate_limited");

  const body = await readJson(request, 2048);
  const model = shortField(body.model, "model");
  const build = typeof body.build === "string" ? body.build.slice(0, 128) : null;

  const id = newId();
  const token = newToken();
  const t = now();
  await env.DB.prepare(
    "INSERT INTO devices (id, token_hash, model, build, created_at, last_seen) VALUES (?1, ?2, ?3, ?4, ?5, ?5)",
  )
    .bind(id, await sha256Hex(token), model, build, t)
    .run();
  return json({ device_id: id, token }, 201);
}

/** מאמת Authorization: Bearer + X-Device-Id. מחזיר את שורת המכשיר או זורק 401. */
export async function authenticate(request, env) {
  const auth = request.headers.get("authorization") || "";
  const deviceId = request.headers.get("x-device-id") || "";
  const match = /^Bearer ([A-Za-z0-9_-]{20,128})$/.exec(auth);
  if (!match || !/^[0-9a-f-]{36}$/.test(deviceId)) throw new HttpError(401, "unauthorized");

  const device = await env.DB.prepare("SELECT id, token_hash, last_seen FROM devices WHERE id = ?1").bind(deviceId).first();
  const hash = await sha256Hex(match[1]);
  if (!device || !timingSafeEqual(device.token_hash, hash)) throw new HttpError(401, "unauthorized");

  // last_seen מתעדכן לכל היותר פעם בשעה, כדי לא לכתוב ל-D1 בכל בקשה
  const t = now();
  if (t - device.last_seen > HOUR) {
    await env.DB.prepare("UPDATE devices SET last_seen = ?1 WHERE id = ?2").bind(t, device.id).run();
  }
  return device;
}

/**
 * POST /feedback
 * {client_id, type, text, app_pkg, app_version?, build?, log?}
 * log נשלח רק אם המשתמש השאיר אותו מסומן במסך התצוגה המקדימה.
 */
export async function submitFeedback(request, env, device) {
  const body = await readJson(request, MAX_TEXT * 4 + MAX_LOG * 2 + 4096);

  const clientId = body.client_id;
  if (typeof clientId !== "string" || !/^[0-9a-zA-Z-]{8,64}$/.test(clientId)) throw new HttpError(400, "bad_client_id");
  if (!FEEDBACK_TYPES.has(body.type)) throw new HttpError(400, "bad_type");
  if (typeof body.text !== "string" || body.text.trim().length === 0) throw new HttpError(400, "empty_text");
  if (body.text.length > MAX_TEXT) throw new HttpError(413, "text_too_long");
  if (typeof body.app_pkg !== "string" || !PKG_RE.test(body.app_pkg) || body.app_pkg.length > 128) {
    throw new HttpError(400, "bad_app_pkg");
  }
  if (body.log != null && (typeof body.log !== "string" || body.log.length > MAX_LOG)) throw new HttpError(413, "log_too_long");
  const appVersion = shortField(body.app_version, "app_version");
  const build = typeof body.build === "string" ? body.build.slice(0, 128) : null;

  // שליחה חוזרת של אותו משוב (רשת נפלה באמצע) מחזירה את הרשומה הקיימת ולא סופרת מכסה
  const existing = await env.DB.prepare("SELECT id, status FROM feedback WHERE device_id = ?1 AND client_id = ?2")
    .bind(device.id, clientId)
    .first();
  if (existing) return json({ id: existing.id, status: existing.status });

  if (!(await takeQuota(env.DB, "feedback", device.id, Number(env.FEEDBACK_PER_DAY || 10), DAY))) {
    throw new HttpError(429, "rate_limited");
  }

  const id = newId();
  let logKey = null;
  if (body.log && body.log.trim()) {
    logKey = `logs/${id}.txt`;
    await env.BLOBS.put(logKey, redact(body.log), { httpMetadata: { contentType: "text/plain; charset=utf-8" } });
  }
  await env.DB.prepare(
    `INSERT INTO feedback (id, device_id, client_id, type, text, app_pkg, app_version, build, log_key, created_at)
     VALUES (?1, ?2, ?3, ?4, ?5, ?6, ?7, ?8, ?9, ?10)`,
  )
    .bind(id, device.id, clientId, body.type, redact(body.text.trim()), body.app_pkg, appVersion, build, logKey, now())
    .run();
  return json({ id, status: "new" }, 201);
}

/** GET /feedback - המשובים של המכשיר והסטטוס שלהם (בלי הלוגים). */
export async function listFeedback(env, device) {
  const { results } = await env.DB.prepare(
    `SELECT f.id, f.client_id, f.type, f.app_pkg, f.status, f.created_at, c.fixed_in
       FROM feedback f LEFT JOIN clusters c ON c.id = f.cluster_id
      WHERE f.device_id = ?1 ORDER BY f.created_at DESC LIMIT 50`,
  )
    .bind(device.id)
    .all();
  return json({ feedback: results });
}

/**
 * GET /inbox - הודעות שעוד לא נמסרו ("בטיפול", "תוקן בגרסה X").
 * ההודעות מסומנות כנמסרו רק אחרי ?ack=<id>,<id>, כדי שהודעה לא תאבד אם התשובה לא הגיעה.
 */
export async function inbox(request, env, device) {
  const url = new URL(request.url);
  const ack = (url.searchParams.get("ack") || "").split(",").filter((s) => /^[0-9a-f-]{36}$/.test(s)).slice(0, 100);
  if (ack.length) {
    const placeholders = ack.map((_, i) => `?${i + 3}`).join(",");
    await env.DB.prepare(`UPDATE inbox SET delivered_at = ?1 WHERE device_id = ?2 AND id IN (${placeholders})`)
      .bind(now(), device.id, ...ack)
      .run();
  }
  const { results } = await env.DB.prepare(
    `SELECT id, feedback_id, kind, text_he, created_at FROM inbox
      WHERE device_id = ?1 AND delivered_at IS NULL ORDER BY created_at LIMIT 50`,
  )
    .bind(device.id)
    .all();
  return json({ messages: results });
}

/** DELETE /me - "מחק את המידע שלי": המכשיר, המשובים, הלוגים וההודעות. */
export async function deleteMe(env, device) {
  const { results } = await env.DB.prepare("SELECT log_key FROM feedback WHERE device_id = ?1 AND log_key IS NOT NULL")
    .bind(device.id)
    .all();
  for (const row of results) await env.BLOBS.delete(row.log_key);
  await env.DB.batch([
    env.DB.prepare("DELETE FROM inbox WHERE device_id = ?1").bind(device.id),
    env.DB.prepare("DELETE FROM feedback WHERE device_id = ?1").bind(device.id),
    env.DB.prepare("DELETE FROM devices WHERE id = ?1").bind(device.id),
  ]);
  return new Response(null, { status: 204 });
}
