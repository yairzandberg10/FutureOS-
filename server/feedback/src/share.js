// שיתוף נסיעה (בסגנון Waze): המכשיר פותח שיתוף, מעדכן מיקום ושעת הגעה בזמן הנסיעה,
// ומי שקיבל את הקישור רואה דף פשוט בלי התחברות. השיתוף פג מעצמו אחרי 3 שעות או בסיום.

import { HttpError, readJson, takeQuota, clientNet, now, json, HOUR, DAY, base64url } from "./util.js";

const SHARE_TTL_MS = 3 * HOUR;
const ID_RE = /^[A-Za-z0-9_-]{12,32}$/;
const MAX_LABEL = 80;

const validLat = (v) => typeof v === "number" && Number.isFinite(v) && v >= -90 && v <= 90;
const validLng = (v) => typeof v === "number" && Number.isFinite(v) && v >= -180 && v <= 180;
const validEta = (v) => typeof v === "number" && Number.isFinite(v) && v > now() - HOUR && v < now() + DAY;

function escapeHtml(value) {
  return String(value)
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&#39;");
}

function checkPosition(body) {
  if (!validLat(body.lat) || !validLng(body.lng)) throw new HttpError(400, "bad_coordinates");
  if (!validEta(body.etaMs)) throw new HttpError(400, "bad_eta");
}

/** POST /nav/share  {lat, lng, etaMs, label?} -> {id, url} */
export async function createShare(request, env, device) {
  const body = await readJson(request, 512);
  checkPosition(body);
  const label = typeof body.label === "string" ? body.label.trim().slice(0, MAX_LABEL) : "";
  if (!(await takeQuota(env.DB, "share_day", device.id, Number(env.SHARE_PER_DAY || 20), DAY))) {
    throw new HttpError(429, "rate_limited");
  }
  if (!(await takeQuota(env.DB, "share_net", clientNet(request), Number(env.SHARE_PER_NET_HOUR || 30), HOUR))) {
    throw new HttpError(429, "rate_limited");
  }
  // מזהה לא ניתן לניחוש: מי שאין לו את הקישור לא יכול למצוא את הנסיעה
  const id = base64url(crypto.getRandomValues(new Uint8Array(12)));
  const t = now();
  await env.DB.prepare(
    `INSERT INTO trip_shares (id, device_id, label, lat, lng, eta_ms, updated_at, expires_at)
     VALUES (?1, ?2, ?3, ?4, ?5, ?6, ?7, ?8)`,
  )
    .bind(id, device.id, label, body.lat, body.lng, body.etaMs, t, t + SHARE_TTL_MS)
    .run();
  if (Math.random() < 0.05) await env.DB.prepare("DELETE FROM trip_shares WHERE expires_at < ?1").bind(t).run();
  const base = new URL(request.url).origin;
  return json({ id, url: `${base}/s/${id}` }, 201);
}

/** POST /nav/share/update  {id, lat, lng, etaMs, ended?} - רק המכשיר שפתח את השיתוף */
export async function updateShare(request, env, device) {
  const body = await readJson(request, 512);
  if (typeof body.id !== "string" || !ID_RE.test(body.id)) throw new HttpError(400, "bad_id");
  const ended = body.ended === true;
  if (!ended) checkPosition(body);
  if (!(await takeQuota(env.DB, "share_update", device.id, Number(env.SHARE_UPDATES_PER_HOUR || 240), HOUR))) {
    throw new HttpError(429, "rate_limited");
  }
  const t = now();
  const result = ended
    ? await env.DB.prepare("UPDATE trip_shares SET ended = 1, updated_at = ?3, expires_at = MIN(expires_at, ?4) WHERE id = ?1 AND device_id = ?2")
        .bind(body.id, device.id, t, t + HOUR)
        .run()
    : await env.DB.prepare(
        "UPDATE trip_shares SET lat = ?3, lng = ?4, eta_ms = ?5, updated_at = ?6 WHERE id = ?1 AND device_id = ?2 AND ended = 0 AND expires_at > ?6",
      )
        .bind(body.id, device.id, body.lat, body.lng, body.etaMs, t)
        .run();
  if (!result.meta.changes) throw new HttpError(404, "not_found");
  return json({ ok: true });
}

/** GET /s/:id - דף ציבורי לצופה בקישור. בלי התחברות, בלי מזהה המכשיר. */
export async function viewShare(env, id) {
  if (!ID_RE.test(id)) throw new HttpError(404, "not_found");
  const row = await env.DB.prepare("SELECT label, lat, lng, eta_ms, updated_at, expires_at, ended FROM trip_shares WHERE id = ?1")
    .bind(id)
    .first();
  const page = (title, inner) =>
    new Response(
      `<!doctype html><html lang="he" dir="rtl"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">` +
        `<meta name="robots" content="noindex"><meta http-equiv="refresh" content="60"><title>${escapeHtml(title)}</title>` +
        `<style>body{font-family:system-ui,sans-serif;margin:0;padding:24px 16px;background:#0b0b0c;color:#f2f2f2}` +
        `main{max-width:480px;margin:auto}h1{font-size:22px}p{font-size:17px;line-height:1.5}a{color:#9ecbff}</style></head>` +
        `<body><main>${inner}</main></body></html>`,
      { headers: { "content-type": "text/html; charset=utf-8", "cache-control": "no-store", "x-robots-tag": "noindex" } },
    );
  if (!row || row.expires_at <= now()) return page("השיתוף הסתיים", "<h1>השיתוף הסתיים</h1><p>הנסיעה כבר לא משותפת.</p>");

  const fmt = (ms) => new Intl.DateTimeFormat("he-IL", { timeZone: "Asia/Jerusalem", hour: "2-digit", minute: "2-digit" }).format(new Date(ms));
  const dest = row.label ? ` ל${escapeHtml(row.label)}` : "";
  if (row.ended) {
    return page("הנסיעה הסתיימה", `<h1>הנסיעה${dest} הסתיימה</h1><p>עודכן לאחרונה ב-${fmt(row.updated_at)}.</p>`);
  }
  const minutes = Math.max(0, Math.round((row.eta_ms - now()) / 60000));
  const map = `https://www.openstreetmap.org/?mlat=${row.lat}&mlon=${row.lng}#map=15/${row.lat}/${row.lng}`;
  return page(
    "בדרך",
    `<h1>בדרך${dest}</h1>` +
      `<p>הגעה משוערת: <b>${fmt(row.eta_ms)}</b> (בעוד ${minutes} דק׳)</p>` +
      `<p>עודכן ב-${fmt(row.updated_at)}. הדף מתרענן כל דקה.</p>` +
      `<p><a href="${escapeHtml(map)}" rel="noopener noreferrer">המיקום האחרון על המפה</a></p>`,
  );
}
