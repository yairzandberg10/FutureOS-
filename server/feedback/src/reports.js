// דיווחי דרך של הקהילה (בסגנון Waze): משטרה, תאונה, מפגע, פקק, חסימה, מצלמה.
// דיווח חדש קרוב לקיים מאותו סוג מאשר אותו; "לא שם" חוזרים מפחיתים אותו עד שהוא נעלם.

import { HttpError, readJson, takeQuota, newId, now, json, HOUR, DAY } from "./util.js";

// כמה זמן דיווח חי בלי אישור נוסף (דקות), ובאיזה רדיוס (מטרים) דיווח חדש נחשב לאותו אירוע
export const TYPES = {
  police: { ttlMin: 60, mergeM: 300 },
  accident: { ttlMin: 90, mergeM: 200 },
  hazard: { ttlMin: 120, mergeM: 150 },
  traffic: { ttlMin: 45, mergeM: 500 },
  closure: { ttlMin: 360, mergeM: 200 },
  camera: { ttlMin: 60 * 24 * 30, mergeM: 100 },
};
const MAX_RADIUS_M = 5000;
const DEFAULT_RADIUS_M = 3000;
const MAX_RESULTS = 60;
const DENIES_TO_REMOVE = 3;
const EARTH_M = 6371000;

const validLat = (v) => typeof v === "number" && Number.isFinite(v) && v >= -90 && v <= 90;
const validLng = (v) => typeof v === "number" && Number.isFinite(v) && v >= -180 && v <= 180;

export function distanceM(aLat, aLng, bLat, bLng) {
  const rad = Math.PI / 180;
  const dLat = (bLat - aLat) * rad;
  const dLng = (bLng - aLng) * rad;
  const h = Math.sin(dLat / 2) ** 2 + Math.cos(aLat * rad) * Math.cos(bLat * rad) * Math.sin(dLng / 2) ** 2;
  return 2 * EARTH_M * Math.asin(Math.sqrt(h));
}

/** תיבה גסה סביב נקודה, לשאילתה על האינדקס; המרחק המדויק מסונן אחר כך. */
function box(lat, lng, radiusM) {
  const dLat = (radiusM / EARTH_M) * (180 / Math.PI);
  const dLng = dLat / Math.max(Math.cos((lat * Math.PI) / 180), 0.01);
  return { minLat: lat - dLat, maxLat: lat + dLat, minLng: lng - dLng, maxLng: lng + dLng };
}

async function nearby(db, lat, lng, radiusM, type) {
  const b = box(lat, lng, radiusM);
  const rows = await db
    .prepare(
      `SELECT id, type, lat, lng, device_id, confirms, denies, created_at FROM road_reports
       WHERE lat BETWEEN ?1 AND ?2 AND lng BETWEEN ?3 AND ?4 AND expires_at > ?5 ${type ? "AND type = ?6" : ""}`,
    )
    .bind(...[b.minLat, b.maxLat, b.minLng, b.maxLng, now(), ...(type ? [type] : [])])
    .all();
  return rows.results.filter((r) => distanceM(lat, lng, r.lat, r.lng) <= radiusM);
}

const byDistance = (lat, lng) => (a, b) => distanceM(lat, lng, a.lat, a.lng) - distanceM(lat, lng, b.lat, b.lng);

/** POST /nav/report  {type, lat, lng} -> {id, merged} */
export async function submitReport(request, env, device) {
  const body = await readJson(request, 512);
  const spec = typeof body.type === "string" ? TYPES[body.type] : undefined;
  if (!spec) throw new HttpError(400, "bad_type");
  if (!validLat(body.lat) || !validLng(body.lng)) throw new HttpError(400, "bad_coordinates");

  if (!(await takeQuota(env.DB, "report_hour", device.id, Number(env.REPORT_PER_HOUR || 20), HOUR))) {
    throw new HttpError(429, "rate_limited");
  }
  if (!(await takeQuota(env.DB, "report_day", device.id, Number(env.REPORT_PER_DAY || 100), DAY))) {
    throw new HttpError(429, "rate_limited");
  }

  const t = now();
  const close = (await nearby(env.DB, body.lat, body.lng, spec.mergeM, body.type)).sort(byDistance(body.lat, body.lng))[0];
  if (close) {
    // דיווח חוזר מאותו מכשיר (או מי שכבר הצביע) לא מנפח את הספירה
    const voted = await env.DB.prepare("SELECT 1 AS x FROM road_report_votes WHERE report_id = ?1 AND device_id = ?2")
      .bind(close.id, device.id)
      .first();
    if (!voted && close.device_id !== device.id) {
      await env.DB.batch([
        env.DB.prepare("UPDATE road_reports SET confirms = confirms + 1, updated_at = ?2, expires_at = ?3 WHERE id = ?1").bind(
          close.id,
          t,
          t + spec.ttlMin * 60_000,
        ),
        env.DB.prepare("INSERT INTO road_report_votes (report_id, device_id, up) VALUES (?1, ?2, 1)").bind(close.id, device.id),
      ]);
    }
    return json({ id: close.id, merged: true });
  }

  const id = newId();
  await env.DB.prepare(
    "INSERT INTO road_reports (id, type, lat, lng, device_id, created_at, updated_at, expires_at) VALUES (?1, ?2, ?3, ?4, ?5, ?6, ?6, ?7)",
  )
    .bind(id, body.type, body.lat, body.lng, device.id, t, t + spec.ttlMin * 60_000)
    .run();
  if (Math.random() < 0.02) await env.DB.prepare("DELETE FROM road_reports WHERE expires_at < ?1").bind(t - DAY).run();
  return json({ id, merged: false }, 201);
}

/** POST /nav/reports/nearby  {lat, lng, radius?} -> {reports:[{id,type,lat,lng,confirms,ageSeconds}]} */
export async function nearbyReports(request, env, device) {
  const body = await readJson(request, 256);
  if (!validLat(body.lat) || !validLng(body.lng)) throw new HttpError(400, "bad_coordinates");
  const radius = Math.min(Number(body.radius) > 0 ? Number(body.radius) : DEFAULT_RADIUS_M, MAX_RADIUS_M);
  if (!(await takeQuota(env.DB, "reports_read", device.id, Number(env.REPORTS_READ_PER_HOUR || 360), HOUR))) {
    throw new HttpError(429, "rate_limited");
  }
  const t = now();
  const rows = (await nearby(env.DB, body.lat, body.lng, radius))
    .sort(byDistance(body.lat, body.lng))
    .slice(0, MAX_RESULTS)
    .map((r) => ({
      id: r.id,
      type: r.type,
      lat: r.lat,
      lng: r.lng,
      confirms: r.confirms,
      ageSeconds: Math.max(0, Math.round((t - r.created_at) / 1000)),
    }));
  return json({ reports: rows });
}

/** POST /nav/report/vote  {id, up} - "עדיין שם" או "לא שם" */
export async function voteReport(request, env, device) {
  const body = await readJson(request, 256);
  if (typeof body.id !== "string" || typeof body.up !== "boolean") throw new HttpError(400, "bad_vote");
  if (!(await takeQuota(env.DB, "report_vote", device.id, Number(env.VOTE_PER_HOUR || 120), HOUR))) {
    throw new HttpError(429, "rate_limited");
  }
  const t = now();
  const report = await env.DB.prepare("SELECT id, type, expires_at FROM road_reports WHERE id = ?1").bind(body.id).first();
  if (!report || report.expires_at <= t) throw new HttpError(404, "not_found");
  const already = await env.DB.prepare("SELECT 1 AS x FROM road_report_votes WHERE report_id = ?1 AND device_id = ?2")
    .bind(report.id, device.id)
    .first();
  if (already) return json({ ok: true, counted: false });

  const stmts = [
    env.DB.prepare("INSERT INTO road_report_votes (report_id, device_id, up) VALUES (?1, ?2, ?3)").bind(report.id, device.id, body.up ? 1 : 0),
  ];
  if (body.up) {
    stmts.push(
      env.DB.prepare("UPDATE road_reports SET confirms = confirms + 1, updated_at = ?2, expires_at = ?3 WHERE id = ?1").bind(
        report.id,
        t,
        t + TYPES[report.type].ttlMin * 60_000,
      ),
    );
  } else {
    stmts.push(env.DB.prepare("UPDATE road_reports SET denies = denies + 1 WHERE id = ?1").bind(report.id));
    // מסירים כשלפחות 3 אמרו "לא שם" והם יותר מאלה שאמרו "עדיין שם"
    stmts.push(
      env.DB.prepare("UPDATE road_reports SET expires_at = ?2 WHERE id = ?1 AND denies >= ?3 AND denies > confirms").bind(report.id, t, DENIES_TO_REMOVE),
    );
  }
  await env.DB.batch(stmts);
  return json({ ok: true, counted: true });
}
