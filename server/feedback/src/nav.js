// פרוקסי הניווט: מחליף את פונקציות ה-Firebase של Navigation (החלטה #28).
// מפתחות HERE ו-SIRI נשמרים כסודות של ה-Worker ולא יוצאים למכשיר.
// התשובות חוזרות כמו שהן: האפליקציה כבר מפענחת בדיוק את הצורות האלה (HereModels.kt, SiriXml).

import { HttpError, readJson, takeQuota, clientNet, HOUR, DAY } from "./util.js";

const HERE_ROUTES_URL = "https://router.hereapi.com/v8/routes";
const HERE_TRANSIT_URL = "https://transit.router.hereapi.com/v8/routes";
const HERE_DEPARTURES_URL = "https://transit.hereapi.com/v8/departures";
// זמן ISO עם הפרש אזור, למשל 2026-11-16T08:30:00+02:00
const ISO_TIME_RE = /^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}(:\d{2})?(Z|[+-]\d{2}:\d{2})$/;
const ID_RE = /^[0-9A-Za-z_-]{1,32}$/;
const SIRI_CACHE_SECONDS = 20;

// מכסה יומית לכל הצי (HERE בתשלום לפי שימוש), כמו ב-Firebase. ניתן לשנות ב-env.
const DEFAULT_DAILY = { here: 5000, siri: 20000 };

function validLat(v) {
  return typeof v === "number" && Number.isFinite(v) && v >= -90 && v <= 90;
}
function validLng(v) {
  return typeof v === "number" && Number.isFinite(v) && v >= -180 && v <= 180;
}

/**
 * מגבלות לפני המכסה של הצי: לכל מכשיר ביום, ולכל רשת בשעה. בלעדיהן מכשירים מזויפים
 * (הרישום פתוח) מכלים את המכסה היומית של כולם תוך כמה שעות.
 */
async function abuseLimits(env, request, device, service, perDeviceDay, perNetHour) {
  if (!(await takeQuota(env.DB, `${service}_day`, device.id, perDeviceDay, DAY))) throw new HttpError(429, "rate_limited");
  if (!(await takeQuota(env.DB, `${service}_net`, clientNet(request), perNetHour, HOUR))) throw new HttpError(429, "rate_limited");
}

async function fleetQuota(env, service) {
  const limit = Number(env[`DAILY_LIMIT_${service.toUpperCase()}`] || DEFAULT_DAILY[service]);
  if (!(await takeQuota(env.DB, "fleet", service, limit, DAY))) throw new HttpError(429, "daily_limit");
}

function escapeXml(value) {
  return value
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&apos;");
}

/** POST /nav/route  {from:{lat,lng}, to:{lat,lng}, mode:"car"} -> JSON של HERE routing v8 */
export async function route(request, env, device) {
  if (!env.HERE_API_KEY) throw new HttpError(503, "nav_disabled");
  const body = await readJson(request, 1024);
  const { from, to } = body;
  if (!from || !to || !validLat(from.lat) || !validLng(from.lng) || !validLat(to.lat) || !validLng(to.lng)) {
    throw new HttpError(400, "bad_coordinates");
  }
  if (body.mode != null && body.mode !== "car") throw new HttpError(400, "bad_mode");
  // אפשרויות בסגנון Waze: הימנעות מכבישי אגרה (כביש 6), ושעת יציאה עתידית ("מתי לצאת")
  if (body.avoidTolls != null && typeof body.avoidTolls !== "boolean") throw new HttpError(400, "bad_options");
  const departAt = body.departAt ?? null;
  if (departAt !== null && (typeof departAt !== "string" || !ISO_TIME_RE.test(departAt) || Number.isNaN(Date.parse(departAt)))) {
    throw new HttpError(400, "bad_options");
  }

  if (!(await takeQuota(env.DB, "route", device.id, Number(env.ROUTE_PER_HOUR || 60), HOUR))) {
    throw new HttpError(429, "rate_limited");
  }
  await abuseLimits(env, request, device, "route", Number(env.ROUTE_PER_DAY || 200), Number(env.ROUTE_PER_NET_HOUR || 240));
  await fleetQuota(env, "here");

  const url = new URL(HERE_ROUTES_URL);
  url.searchParams.set("origin", `${from.lat},${from.lng}`);
  url.searchParams.set("destination", `${to.lat},${to.lng}`);
  url.searchParams.set("transportMode", "car");
  url.searchParams.set("routingMode", "fast");
  // אותם פרמטרים כמו בפונקציית ה-Firebase, כי זו הצורה ש-HereModels.kt מפענח היום
  url.searchParams.set("return", "polyline,summary,travelSummary,turnByTurnActions");
  url.searchParams.set("lang", "he");
  url.searchParams.set("traffic[mode]", "default");
  if (body.avoidTolls === true) url.searchParams.set("avoid[features]", "tollRoad");
  if (departAt !== null) url.searchParams.set("departureTime", departAt);
  url.searchParams.set("apikey", env.HERE_API_KEY);

  const upstream = await fetch(url.toString());
  const text = await upstream.text();
  if (!upstream.ok) {
    // לא מעבירים את גוף השגיאה: עלול להכיל את ה-URL עם המפתח
    console.warn("here failed", upstream.status);
    throw new HttpError(502, "upstream_failed");
  }
  return new Response(text, { headers: { "content-type": "application/json; charset=utf-8", "cache-control": "no-store" } });
}

/**
 * POST /nav/transit  {from:{lat,lng}, to:{lat,lng}} -> JSON של HERE Public Transit v8.
 * החישוב קורה כאן, והמכשיר מקבל רק את המסלולים (כמה KB) במקום להוריד את כל נתוני ה-GTFS (~130MB).
 */
export async function transit(request, env, device) {
  if (!env.HERE_API_KEY) throw new HttpError(503, "nav_disabled");
  const body = await readJson(request, 1024);
  const { from, to } = body;
  if (!from || !to || !validLat(from.lat) || !validLng(from.lng) || !validLat(to.lat) || !validLng(to.lng)) {
    throw new HttpError(400, "bad_coordinates");
  }
  if (!(await takeQuota(env.DB, "transit", device.id, Number(env.ROUTE_PER_HOUR || 60), HOUR))) {
    throw new HttpError(429, "rate_limited");
  }
  await abuseLimits(env, request, device, "transit", Number(env.ROUTE_PER_DAY || 200), Number(env.ROUTE_PER_NET_HOUR || 240));
  await fleetQuota(env, "here");

  const url = new URL(HERE_TRANSIT_URL);
  url.searchParams.set("origin", `${from.lat},${from.lng}`);
  url.searchParams.set("destination", `${to.lat},${to.lng}`);
  url.searchParams.set("return", "intermediate,travelSummary");
  url.searchParams.set("alternatives", "3");
  url.searchParams.set("lang", "he");
  url.searchParams.set("apikey", env.HERE_API_KEY);

  const upstream = await fetch(url.toString());
  const text = await upstream.text();
  if (!upstream.ok) {
    console.warn("here transit failed", upstream.status);
    throw new HttpError(502, "upstream_failed");
  }
  return new Response(text, { headers: { "content-type": "application/json; charset=utf-8", "cache-control": "no-store" } });
}

/**
 * POST /nav/departures  {lat, lng} -> JSON של HERE Public Transit "Next Departures" (boards[]).
 * תחנות קרובות וזמני היציאה הבאים מכל אחת (בסגנון Moovit), בלי שהמכשיר יחזיק נתוני GTFS.
 */
export async function departures(request, env, device) {
  if (!env.HERE_API_KEY) throw new HttpError(503, "nav_disabled");
  const body = await readJson(request, 256);
  if (!validLat(body.lat) || !validLng(body.lng)) throw new HttpError(400, "bad_coordinates");
  if (!(await takeQuota(env.DB, "departures", device.id, Number(env.DEPARTURES_PER_HOUR || 120), HOUR))) {
    throw new HttpError(429, "rate_limited");
  }
  await abuseLimits(env, request, device, "departures", Number(env.DEPARTURES_PER_DAY || 600), Number(env.DEPARTURES_PER_NET_HOUR || 480));
  await fleetQuota(env, "here");

  const url = new URL(HERE_DEPARTURES_URL);
  // ";" חייב להישאר לא מקודד: HERE מפרש in={lat},{lng};r={radius}
  url.search = `in=${body.lat},${body.lng};r=600&maxPlaces=6&maxPerBoard=4&lang=he&apiKey=${encodeURIComponent(env.HERE_API_KEY)}`;

  const upstream = await fetch(url.toString());
  const text = await upstream.text();
  if (!upstream.ok) {
    console.warn("here departures failed", upstream.status);
    throw new HttpError(502, "upstream_failed");
  }
  return new Response(text, { headers: { "content-type": "application/json; charset=utf-8", "cache-control": "no-store" } });
}

/** POST /nav/siri/stop{stopId, lineRef?} -> XML של SIRI StopMonitoring */
export async function siriStop(request, env, device, cache = globalThis.caches?.default) {
  if (!env.SIRI_BASE_URL || !env.SIRI_API_KEY) throw new HttpError(503, "transit_disabled");
  const body = await readJson(request, 512);
  const stopId = body.stopId;
  const lineRef = body.lineRef ?? null;
  if (typeof stopId !== "string" || !ID_RE.test(stopId)) throw new HttpError(400, "bad_stop_id");
  if (lineRef !== null && (typeof lineRef !== "string" || !ID_RE.test(lineRef))) throw new HttpError(400, "bad_line_ref");

  // cache לפי תחנה+קו: כמה מכשירים באותה תחנה = קריאה אחת ל-SIRI כל 20 שניות
  const cacheKey = new Request(`https://cache.futureos.internal/siri/${stopId}/${lineRef ?? "-"}`);
  if (cache) {
    const hit = await cache.match(cacheKey);
    if (hit) return hit;
  }

  if (!(await takeQuota(env.DB, "siri", device.id, Number(env.SIRI_PER_HOUR || 240), HOUR))) {
    throw new HttpError(429, "rate_limited");
  }
  await abuseLimits(env, request, device, "siri", Number(env.SIRI_PER_DAY || 1500), Number(env.SIRI_PER_NET_HOUR || 960));
  await fleetQuota(env, "siri");

  const timestamp = new Date().toISOString();
  const lineRefXml = lineRef ? `<LineRef>${escapeXml(lineRef)}</LineRef>` : "";
  const xmlRequest = `<?xml version="1.0" encoding="UTF-8"?>
<Siri xmlns="http://www.siri.org.uk/siri" version="2.0">
  <ServiceRequest>
    <RequestTimestamp>${timestamp}</RequestTimestamp>
    <RequestorRef>${escapeXml(env.SIRI_API_KEY)}</RequestorRef>
    <StopMonitoringRequest version="2.0">
      <RequestTimestamp>${timestamp}</RequestTimestamp>
      <MonitoringRef>${escapeXml(stopId)}</MonitoringRef>
      ${lineRefXml}
      <MaximumStopVisits>20</MaximumStopVisits>
    </StopMonitoringRequest>
  </ServiceRequest>
</Siri>`;

  const upstream = await fetch(env.SIRI_BASE_URL, {
    method: "POST",
    headers: { "content-type": "text/xml; charset=utf-8" },
    body: xmlRequest,
  });
  const xml = await upstream.text();
  if (!upstream.ok) {
    console.warn("siri failed", upstream.status);
    throw new HttpError(502, "upstream_failed");
  }
  const response = new Response(xml, {
    headers: { "content-type": "text/xml; charset=utf-8", "cache-control": `public, max-age=${SIRI_CACHE_SECONDS}` },
  });
  if (cache) await cache.put(cacheKey, response.clone());
  return response;
}
