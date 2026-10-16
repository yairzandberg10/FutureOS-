// מנוע העדכונים בצד השרת: בדיקת עדכון, הורדה, אירועי התקנה, פריסה הדרגתית ועצירה אוטומטית.
// מסמך העיצוב: docs/update-feedback-design.md סעיפים 2.4-2.5.
//
// מה השרת לא עושה: הוא לא בונה ולא חותם על APK. הקובץ מגיע חתום במפתח ה-release שנמצא רק אצל המשתמש,
// והטלפון בודק בעצמו שהחתימה זהה לזו של האפליקציה המותקנת. שרת פרוץ יכול לכל היותר להציע גרסה
// שכבר חתומה במפתח שלנו, ולא להתקין קוד זר.
//
// ה-manifest שהטלפון מקבל חתום ב-ECDSA P-256 (UPDATE_SIGNING_KEY). המפתח הציבורי נמצא בתוך Settings,
// כך שתשובה מזויפת (גם אם TLS נפרץ באמצע) לא מתקבלת. ה-manifest קשור למכשיר (device_id) ופג אחרי שעה.

import { HttpError, json, readJson, now, newId, takeQuota, DAY } from "./util.js";
import { addInbox, msgFixed } from "./triage.js";

const PKG_RE = /^[a-zA-Z][a-zA-Z0-9_]*(\.[a-zA-Z0-9_]+)+$/;
const VERSION_NAME_RE = /^[\w.\-+ ()]{1,64}$/;
const ERROR_RE = /^[\w.\-: ]{1,64}$/;
const ID_RE = /^[0-9a-f-]{36}$/;
export const CHANNELS = ["dev", "beta", "stable"];
const KINDS = new Set(["apk", "ota"]);
const DEVICE_EVENTS = new Set(["downloaded", "installed", "failed"]);
const MANIFEST_TTL_MS = 60 * 60 * 1000;
const NEXT_CHECK_S = 6 * 60 * 60;
/** תקרת העלאה. ב-Workers החינמי גוף בקשה מוגבל ל-100MB. */
export const MAX_PAYLOAD = 90 * 1024 * 1024;

/** ערוץ המכשיר קובע אילו ערוצים הוא רואה: dev רואה הכול, stable רק stable. */
export function visibleChannels(deviceChannel) {
  const i = CHANNELS.indexOf(deviceChannel);
  return CHANNELS.slice(i < 0 ? CHANNELS.length - 1 : i);
}

/** דלי קבוע לכל (מכשיר, גרסה) בטווח 0-99. מכשיר זכאי אם הדלי שלו קטן מאחוז הפריסה. */
export async function bucketOf(deviceId, releaseId) {
  const digest = await crypto.subtle.digest("SHA-256", new TextEncoder().encode(`${deviceId}:${releaseId}`));
  return new DataView(digest).getUint32(0) % 100;
}

function hex(buf) {
  return [...new Uint8Array(buf)].map((b) => b.toString(16).padStart(2, "0")).join("");
}

function b64(bytes) {
  let s = "";
  for (const b of new Uint8Array(bytes)) s += String.fromCharCode(b);
  return btoa(s);
}

function b64decode(text) {
  return Uint8Array.from(atob(text.replace(/\s+/g, "")), (c) => c.charCodeAt(0));
}

let signingKeyCache = { raw: null, key: null };
async function signingKey(env) {
  const raw = env.UPDATE_SIGNING_KEY;
  if (!raw) return null;
  if (signingKeyCache.raw === raw) return signingKeyCache.key;
  const key = await crypto.subtle.importKey("pkcs8", b64decode(raw), { name: "ECDSA", namedCurve: "P-256" }, false, ["sign"]);
  signingKeyCache = { raw, key };
  return key;
}

/** חותם על מחרוזת ה-manifest. החתימה בפורמט r||s (64 בתים), base64. */
export async function signManifest(env, manifestText) {
  const key = await signingKey(env);
  if (!key) throw new HttpError(503, "updates_disabled");
  const sig = await crypto.subtle.sign({ name: "ECDSA", hash: "SHA-256" }, key, new TextEncoder().encode(manifestText));
  return b64(sig);
}

function parseApps(list) {
  if (!Array.isArray(list) || list.length > 100) throw new HttpError(400, "bad_apps");
  const apps = new Map();
  for (const a of list) {
    if (!a || typeof a.pkg !== "string" || !PKG_RE.test(a.pkg) || a.pkg.length > 128) throw new HttpError(400, "bad_apps");
    if (!Number.isSafeInteger(a.version_code) || a.version_code < 0) throw new HttpError(400, "bad_apps");
    apps.set(a.pkg, a.version_code);
  }
  return apps;
}

/**
 * POST /update/check  {apps:[{pkg, version_code}], kinds?:["apk"], build?}
 * -> {manifest: "<JSON כמחרוזת>", sig: "<base64>"}
 * הטלפון מאמת את החתימה על המחרוזת כמו שהיא, ורק אחר כך מפענח אותה.
 */
export async function checkUpdates(request, env, device) {
  const body = await readJson(request, 16 * 1024);
  const apps = parseApps(body.apps);
  const kinds = Array.isArray(body.kinds) ? body.kinds.filter((k) => KINDS.has(k)) : ["apk"];
  if (!kinds.length) kinds.push("apk");

  const row = await env.DB.prepare("SELECT channel FROM devices WHERE id = ?1").bind(device.id).first();
  const channels = visibleChannels(row?.channel || "stable");

  const updates = [];
  if (apps.size) {
    const pkgs = [...apps.keys()];
    const ph = (arr, from) => arr.map((_, i) => `?${i + from}`).join(",");
    const { results } = await env.DB.prepare(
      `SELECT * FROM releases
        WHERE state = 'active' AND blob_key IS NOT NULL
          AND kind IN (${ph(kinds, 1)})
          AND channel IN (${ph(channels, 1 + kinds.length)})
          AND pkg IN (${ph(pkgs, 1 + kinds.length + channels.length)})
        ORDER BY version_code DESC`,
    )
      .bind(...kinds, ...channels, ...pkgs)
      .all();

    // מכשיר שנכשל בהתקנה של גרסה ב-24 השעות האחרונות לא מקבל אותה שוב מיד (בלי לולאת כשלים)
    const { results: recentFails } = await env.DB.prepare(
      "SELECT release_id FROM update_events WHERE device_id = ?1 AND event = 'failed' AND at > ?2",
    )
      .bind(device.id, now() - DAY)
      .all();
    const failed = new Set(recentFails.map((r) => r.release_id));

    const picked = new Set();
    for (const r of results) {
      const key = `${r.kind}:${r.pkg}`;
      if (picked.has(key)) continue; // כבר נבחרה גרסה גבוהה יותר לאותה חבילה
      if (r.version_code <= apps.get(r.pkg)) continue;
      if (failed.has(r.id)) continue;
      if ((await bucketOf(device.id, r.id)) >= r.pct) continue;
      picked.add(key);
      updates.push({
        release_id: r.id,
        kind: r.kind,
        pkg: r.pkg,
        version_code: r.version_code,
        version_name: r.version_name,
        size: r.size,
        sha256: r.sha256,
        notes_he: r.notes_he,
        path: `update/download/${r.id}`,
      });
    }
    for (const u of updates) {
      await env.DB.prepare(
        "INSERT OR IGNORE INTO update_events (device_id, release_id, event, at) VALUES (?1, ?2, 'offered', ?3)",
      )
        .bind(device.id, u.release_id, now())
        .run();
    }
  }

  const t = now();
  const manifest = JSON.stringify({
    v: 1,
    device_id: device.id,
    issued_at: t,
    expires_at: t + MANIFEST_TTL_MS,
    next_check_s: NEXT_CHECK_S,
    updates,
  });
  return json({ manifest, sig: await signManifest(env, manifest) });
}

/** GET /update/download/<id> - רק גרסה שהוצעה למכשיר הזה, ורק כשהיא עדיין פעילה. */
export async function download(env, device, releaseId) {
  if (!ID_RE.test(releaseId)) throw new HttpError(404, "not_found");
  const release = await env.DB.prepare("SELECT * FROM releases WHERE id = ?1").bind(releaseId).first();
  const offered = await env.DB.prepare(
    "SELECT 1 FROM update_events WHERE device_id = ?1 AND release_id = ?2 AND event = 'offered'",
  )
    .bind(device.id, releaseId)
    .first();
  if (!release || !offered || !release.blob_key) throw new HttpError(404, "not_found");
  if (release.state !== "active") throw new HttpError(410, "paused");
  if (!(await takeQuota(env.DB, "download", device.id, Number(env.DOWNLOADS_PER_DAY || 30), DAY))) {
    throw new HttpError(429, "rate_limited");
  }
  const obj = await env.BLOBS.get(release.blob_key);
  if (!obj) throw new HttpError(404, "not_found");
  return new Response(obj.body, {
    headers: {
      "content-type": "application/vnd.android.package-archive",
      "content-length": String(release.size),
      "cache-control": "no-store",
      "x-content-sha256": release.sha256,
    },
  });
}

/** POST /update/event  {release_id, event: downloaded|installed|failed, error?} */
export async function reportEvent(request, env, device) {
  const body = await readJson(request, 1024);
  if (typeof body.release_id !== "string" || !ID_RE.test(body.release_id)) throw new HttpError(400, "bad_release_id");
  if (!DEVICE_EVENTS.has(body.event)) throw new HttpError(400, "bad_event");
  const error = body.error == null ? null : String(body.error);
  if (error != null && !ERROR_RE.test(error)) throw new HttpError(400, "bad_error");

  const release = await env.DB.prepare("SELECT * FROM releases WHERE id = ?1").bind(body.release_id).first();
  const offered = await env.DB.prepare(
    "SELECT 1 FROM update_events WHERE device_id = ?1 AND release_id = ?2 AND event = 'offered'",
  )
    .bind(device.id, body.release_id)
    .first();
  if (!release || !offered) throw new HttpError(404, "not_found");

  await env.DB.prepare(
    `INSERT INTO update_events (device_id, release_id, event, error, at) VALUES (?1, ?2, ?3, ?4, ?5)
     ON CONFLICT(device_id, release_id, event) DO UPDATE SET error = excluded.error, at = excluded.at`,
  )
    .bind(device.id, body.release_id, body.event, error, now())
    .run();

  let notified = 0;
  if (body.event === "installed") notified = await notifyFixedOnDevice(env, device, release);
  if (body.event === "failed") await maybeAutoPause(env, release);
  return json({ ok: true, notified });
}

/**
 * "תוקן בגרסה X" אוטומטית: רק כשהמכשיר הזה דיווח שהגרסה עם התיקון באמת הותקנה אצלו.
 * משובים של מכשירים אחרים באותו אשכול יקבלו את ההודעה כשהם יתקינו.
 */
async function notifyFixedOnDevice(env, device, release) {
  let clusters;
  try {
    clusters = JSON.parse(release.fixed_clusters || "[]").filter((c) => typeof c === "string" && ID_RE.test(c));
  } catch {
    clusters = [];
  }
  if (!clusters.length) return 0;
  const ph = clusters.map((_, i) => `?${i + 2}`).join(",");
  const { results } = await env.DB.prepare(
    `SELECT id, device_id FROM feedback
      WHERE device_id = ?1 AND cluster_id IN (${ph}) AND status NOT IN ('fixed', 'wontfix')`,
  )
    .bind(device.id, ...clusters)
    .all();
  for (const f of results) {
    await addInbox(env, f, "fixed", msgFixed(release.version_name));
    await env.DB.prepare("UPDATE feedback SET status = 'fixed' WHERE id = ?1").bind(f.id).run();
  }
  await env.DB.prepare(
    `UPDATE clusters SET state = 'fixed', fixed_in = COALESCE(fixed_in, ?1), updated_at = ?2
      WHERE id IN (${clusters.map((_, i) => `?${i + 3}`).join(",")}) AND state != 'fixed'`,
  )
    .bind(release.version_name, now(), ...clusters)
    .run();
  return results.length;
}

/**
 * עצירה אוטומטית: אם לפחות UPDATE_PAUSE_MIN_FAILS מכשירים נכשלו, ושיעור הכשלים מעל UPDATE_PAUSE_FAIL_RATE,
 * הפריסה נעצרת. חידוש רק ידני מהדשבורד.
 */
export async function maybeAutoPause(env, release) {
  if (release.state !== "active") return false;
  const counts = await env.DB.prepare(
    `SELECT SUM(event = 'failed') AS failed, SUM(event = 'installed') AS installed
       FROM update_events WHERE release_id = ?1`,
  )
    .bind(release.id)
    .first();
  const failed = Number(counts?.failed || 0);
  const installed = Number(counts?.installed || 0);
  const minFails = Number(env.UPDATE_PAUSE_MIN_FAILS || 3);
  const maxRate = Number(env.UPDATE_PAUSE_FAIL_RATE || 0.2);
  if (failed < minFails || failed / (failed + installed) <= maxRate) return false;
  await env.DB.prepare("UPDATE releases SET state = 'paused', pause_reason = ?1, updated_at = ?2 WHERE id = ?3")
    .bind(`auto: ${failed} failed / ${installed} installed`, now(), release.id)
    .run();
  console.log("rollout auto-paused", release.id, failed, installed);
  return true;
}

// ---------- דשבורד (נקרא מ-admin.js, אחרי requireAdmin) ----------

export async function adminListReleases(env) {
  const { results } = await env.DB.prepare(
    `SELECT r.id, r.kind, r.pkg, r.version_code, r.version_name, r.channel, r.size, r.sha256, r.pct, r.state,
            r.pause_reason, r.fixed_clusters, r.created_at, r.updated_at,
            (SELECT COUNT(*) FROM update_events e WHERE e.release_id = r.id AND e.event = 'offered') AS offered,
            (SELECT COUNT(*) FROM update_events e WHERE e.release_id = r.id AND e.event = 'installed') AS installed,
            (SELECT COUNT(*) FROM update_events e WHERE e.release_id = r.id AND e.event = 'failed') AS failed
       FROM releases r ORDER BY r.created_at DESC LIMIT 200`,
  ).all();
  return json({ releases: results });
}

/** POST /admin/releases  {kind?, pkg, version_code, version_name, channel?, notes_he?, fixed_clusters?} */
export async function adminCreateRelease(request, env) {
  const body = await readJson(request, 8 * 1024);
  const kind = body.kind ?? "apk";
  if (!KINDS.has(kind)) throw new HttpError(400, "bad_kind");
  if (typeof body.pkg !== "string" || !PKG_RE.test(body.pkg) || body.pkg.length > 128) throw new HttpError(400, "bad_pkg");
  if (kind === "apk" && !body.pkg.startsWith("com.future.")) throw new HttpError(400, "bad_pkg");
  if (!Number.isSafeInteger(body.version_code) || body.version_code <= 0) throw new HttpError(400, "bad_version_code");
  if (typeof body.version_name !== "string" || !VERSION_NAME_RE.test(body.version_name)) throw new HttpError(400, "bad_version_name");
  const channel = body.channel ?? "stable";
  if (!CHANNELS.includes(channel)) throw new HttpError(400, "bad_channel");
  const notes = body.notes_he == null ? "" : String(body.notes_he);
  if (notes.length > 2000) throw new HttpError(400, "bad_notes");
  const fixed = body.fixed_clusters ?? [];
  if (!Array.isArray(fixed) || fixed.length > 50 || !fixed.every((c) => typeof c === "string" && ID_RE.test(c))) {
    throw new HttpError(400, "bad_fixed_clusters");
  }

  const id = newId();
  const t = now();
  try {
    await env.DB.prepare(
      `INSERT INTO releases (id, kind, pkg, version_code, version_name, channel, notes_he, fixed_clusters, created_at, updated_at)
       VALUES (?1, ?2, ?3, ?4, ?5, ?6, ?7, ?8, ?9, ?9)`,
    )
      .bind(id, kind, body.pkg, body.version_code, body.version_name, channel, notes, JSON.stringify(fixed), t)
      .run();
  } catch (e) {
    if (String(e?.message || e).includes("UNIQUE")) throw new HttpError(409, "version_exists");
    throw e;
  }
  return json({ id, state: "draft" }, 201);
}

/** PUT /admin/releases/<id>/payload - גוף הבקשה הוא קובץ ה-APK. השרת מחשב בעצמו גודל ו-SHA-256. */
export async function adminUploadPayload(request, env, id) {
  const release = await env.DB.prepare("SELECT * FROM releases WHERE id = ?1").bind(id).first();
  if (!release) throw new HttpError(404, "not_found");
  if (release.state !== "draft") throw new HttpError(409, "not_draft"); // קובץ של גרסה שכבר יצאה לא מוחלף
  const declared = Number(request.headers.get("content-length") || 0);
  if (declared > MAX_PAYLOAD) throw new HttpError(413, "too_large");
  const buf = await request.arrayBuffer();
  if (buf.byteLength === 0) throw new HttpError(400, "empty");
  if (buf.byteLength > MAX_PAYLOAD) throw new HttpError(413, "too_large");
  const head = new Uint8Array(buf, 0, 4);
  // APK וחבילת OTA הם קובצי ZIP
  if (!(head[0] === 0x50 && head[1] === 0x4b && head[2] === 0x03 && head[3] === 0x04)) throw new HttpError(400, "not_zip");

  const sha256 = hex(await crypto.subtle.digest("SHA-256", buf));
  const key = `releases/${id}.${release.kind === "ota" ? "zip" : "apk"}`;
  await env.BLOBS.put(key, buf, { httpMetadata: { contentType: "application/vnd.android.package-archive" } });
  await env.DB.prepare("UPDATE releases SET blob_key = ?1, size = ?2, sha256 = ?3, updated_at = ?4 WHERE id = ?5")
    .bind(key, buf.byteLength, sha256, now(), id)
    .run();
  return json({ ok: true, size: buf.byteLength, sha256 });
}

/** POST /admin/releases/<id>/rollout {pct} - פרסום או הרחבה. רק אחרי שהקובץ הועלה. */
export async function adminRollout(request, env, id) {
  const body = await readJson(request, 256);
  if (!Number.isInteger(body.pct) || body.pct < 0 || body.pct > 100) throw new HttpError(400, "bad_pct");
  const release = await env.DB.prepare("SELECT * FROM releases WHERE id = ?1").bind(id).first();
  if (!release) throw new HttpError(404, "not_found");
  if (!release.blob_key) throw new HttpError(409, "no_payload");
  await env.DB.prepare("UPDATE releases SET pct = ?1, state = 'active', pause_reason = NULL, updated_at = ?2 WHERE id = ?3")
    .bind(body.pct, now(), id)
    .run();
  return json({ ok: true, pct: body.pct, state: "active" });
}

export async function adminPause(env, id) {
  const res = await env.DB.prepare("UPDATE releases SET state = 'paused', pause_reason = 'manual', updated_at = ?1 WHERE id = ?2")
    .bind(now(), id)
    .run();
  if (!res.meta?.changes) throw new HttpError(404, "not_found");
  return json({ ok: true, state: "paused" });
}

/** POST /admin/devices/<id>/channel {channel} - למשל הטלפון של המשתמש ב-dev, השאר ב-stable. */
export async function adminSetChannel(request, env, deviceId) {
  const body = await readJson(request, 256);
  if (!CHANNELS.includes(body.channel)) throw new HttpError(400, "bad_channel");
  const res = await env.DB.prepare("UPDATE devices SET channel = ?1 WHERE id = ?2").bind(body.channel, deviceId).run();
  if (!res.meta?.changes) throw new HttpError(404, "not_found");
  return json({ ok: true, channel: body.channel });
}
