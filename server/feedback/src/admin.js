// דשבורד (API) של המשתמש: רשימת אשכולות, החלטות ידניות, סימון "תוקן בגרסה X", הרצת מיון.
// מוגן ב-Cloudflare Access: ה-Worker מאמת בעצמו את ה-JWT שמגיע ב-Cf-Access-Jwt-Assertion,
// ולא סומך על כותרת אימייל (אותה אפשר לזייף אם Access לא מוגדר על הנתיב).

import { HttpError, json, readJson, timingSafeEqual } from "./util.js";
import { runTriage, promote, markFixed } from "./triage.js";

const VERSION_RE = /^[\w.\-+ ]{1,40}$/;
let jwksCache = { at: 0, keys: [] };

function b64urlDecode(s) {
  const bin = atob(s.replace(/-/g, "+").replace(/_/g, "/") + "===".slice((s.length + 3) % 4));
  return Uint8Array.from(bin, (c) => c.charCodeAt(0));
}

async function accessKeys(env) {
  if (Date.now() - jwksCache.at < 10 * 60 * 1000 && jwksCache.keys.length) return jwksCache.keys;
  const res = await fetch(`https://${env.ACCESS_TEAM_DOMAIN}/cdn-cgi/access/certs`);
  if (!res.ok) throw new HttpError(503, "access_certs_unavailable");
  jwksCache = { at: Date.now(), keys: (await res.json()).keys || [] };
  return jwksCache.keys;
}

export function resetJwksCache() {
  jwksCache = { at: 0, keys: [] };
}

/** מחזיר את אימייל המנהל, או זורק 401/403. */
export async function requireAdmin(request, env) {
  // מצב פיתוח מקומי בלבד: טוקן קבוע. ENVIRONMENT=local מוגדר רק ב-.dev.vars, לא ב-wrangler.toml
  if (env.ENVIRONMENT === "local" && env.LOCAL_ADMIN_TOKEN) {
    const given = request.headers.get("x-admin-token") || "";
    if (timingSafeEqual(given, env.LOCAL_ADMIN_TOKEN)) return "local-admin";
  }
  if (!env.ACCESS_TEAM_DOMAIN || !env.ACCESS_AUD || !env.ADMIN_EMAIL) throw new HttpError(503, "admin_disabled");

  const jwt = request.headers.get("cf-access-jwt-assertion") || "";
  const parts = jwt.split(".");
  if (parts.length !== 3) throw new HttpError(401, "unauthorized");
  let header;
  let payload;
  try {
    header = JSON.parse(new TextDecoder().decode(b64urlDecode(parts[0])));
    payload = JSON.parse(new TextDecoder().decode(b64urlDecode(parts[1])));
  } catch {
    throw new HttpError(401, "unauthorized");
  }
  if (header.alg !== "RS256") throw new HttpError(401, "unauthorized");
  const jwk = (await accessKeys(env)).find((k) => k.kid === header.kid);
  if (!jwk) throw new HttpError(401, "unauthorized");

  const key = await crypto.subtle.importKey("jwk", jwk, { name: "RSASSA-PKCS1-v1_5", hash: "SHA-256" }, false, ["verify"]);
  const ok = await crypto.subtle.verify(
    "RSASSA-PKCS1-v1_5",
    key,
    b64urlDecode(parts[2]),
    new TextEncoder().encode(`${parts[0]}.${parts[1]}`),
  );
  const aud = Array.isArray(payload.aud) ? payload.aud : [payload.aud];
  const nowSec = Math.floor(Date.now() / 1000);
  if (!ok || !aud.includes(env.ACCESS_AUD) || !(payload.exp > nowSec)) throw new HttpError(401, "unauthorized");
  if (String(payload.email || "").toLowerCase() !== env.ADMIN_EMAIL.toLowerCase()) throw new HttpError(403, "forbidden");
  return payload.email;
}

export async function handleAdmin(request, env, path) {
  await requireAdmin(request, env);
  const method = request.method;

  if (method === "GET" && path === "/admin/clusters") {
    const state = new URL(request.url).searchParams.get("state");
    const stmt = state
      ? env.DB.prepare(
          "SELECT id, app_pkg, title_he, severity, state, members, gh_issue, fixed_in, updated_at FROM clusters WHERE state = ?1 ORDER BY severity, updated_at DESC LIMIT 200",
        ).bind(state)
      : env.DB.prepare(
          "SELECT id, app_pkg, title_he, severity, state, members, gh_issue, fixed_in, updated_at FROM clusters ORDER BY severity, updated_at DESC LIMIT 200",
        );
    return json({ clusters: (await stmt.all()).results });
  }

  let m = /^\/admin\/clusters\/([0-9a-f-]{36})$/.exec(path);
  if (method === "GET" && m) {
    const cluster = await env.DB.prepare("SELECT * FROM clusters WHERE id = ?1").bind(m[1]).first();
    if (!cluster) throw new HttpError(404, "not_found");
    delete cluster.embedding;
    const { results } = await env.DB.prepare(
      "SELECT id, type, text, app_version, build, status, severity, decision, log_key, created_at FROM feedback WHERE cluster_id = ?1 ORDER BY created_at DESC",
    )
      .bind(m[1])
      .all();
    return json({ cluster, feedback: results });
  }

  m = /^\/admin\/feedback\/([0-9a-f-]{36})\/log$/.exec(path);
  if (method === "GET" && m) {
    const row = await env.DB.prepare("SELECT log_key FROM feedback WHERE id = ?1").bind(m[1]).first();
    const obj = row?.log_key ? await env.BLOBS.get(row.log_key) : null;
    if (!obj) throw new HttpError(404, "not_found");
    return new Response(await obj.text(), { headers: { "content-type": "text/plain; charset=utf-8", "cache-control": "no-store" } });
  }

  // החלטה ידנית: deep (פותח issue ושולח "בטיפול") או wontfix
  m = /^\/admin\/clusters\/([0-9a-f-]{36})\/decision$/.exec(path);
  if (method === "POST" && m) {
    const body = await readJson(request, 256);
    const cluster = await env.DB.prepare("SELECT * FROM clusters WHERE id = ?1").bind(m[1]).first();
    if (!cluster) throw new HttpError(404, "not_found");
    if (body.state === "deep") {
      const summary = {};
      await promote(env, cluster, console, summary);
      return json({ ok: true, issue_opened: Boolean(summary.issues) });
    }
    if (body.state === "wontfix") {
      await env.DB.prepare("UPDATE clusters SET state = 'wontfix' WHERE id = ?1").bind(m[1]).run();
      return json({ ok: true });
    }
    throw new HttpError(400, "bad_state");
  }

  // "תוקן בגרסה X": רק אחרי שהגרסה באמת יצאה (שלב 4 יחבר את זה ל-boot_ok אוטומטית)
  m = /^\/admin\/clusters\/([0-9a-f-]{36})\/fixed$/.exec(path);
  if (method === "POST" && m) {
    const body = await readJson(request, 256);
    if (typeof body.version !== "string" || !VERSION_RE.test(body.version)) throw new HttpError(400, "bad_version");
    const notified = await markFixed(env, m[1], body.version);
    return json({ ok: true, notified });
  }

  if (method === "POST" && path === "/admin/triage") {
    return json(await runTriage(env));
  }

  throw new HttpError(404, "not_found");
}
