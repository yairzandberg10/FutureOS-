// בדיקות למנוע העדכונים. הרצה: node --test

import { test, beforeEach, afterEach } from "node:test";
import assert from "node:assert/strict";
import worker from "../src/index.js";
import { bucketOf, visibleChannels } from "../src/updates.js";
import { makeEnv } from "../scripts/local-env.mjs";

const BASE = "http://test.local";
const ADMIN = { "x-admin-token": "local-test-token" };
const realFetch = globalThis.fetch;
let env;
let publicKey;

beforeEach(async () => {
  const pair = await crypto.subtle.generateKey({ name: "ECDSA", namedCurve: "P-256" }, true, ["sign", "verify"]);
  publicKey = pair.publicKey;
  const pkcs8 = Buffer.from(await crypto.subtle.exportKey("pkcs8", pair.privateKey)).toString("base64");
  env = makeEnv({ ENVIRONMENT: "local", LOCAL_ADMIN_TOKEN: ADMIN["x-admin-token"], UPDATE_SIGNING_KEY: pkcs8 });
  globalThis.fetch = async () => {
    throw new Error("unexpected network call");
  };
});
afterEach(() => {
  globalThis.fetch = realFetch;
});

const call = (path, { method = "GET", body, headers = {}, raw } = {}) =>
  worker.fetch(
    new Request(BASE + path, {
      method,
      headers: raw ? headers : { "content-type": "application/json", ...headers },
      body: raw ?? (body === undefined ? undefined : JSON.stringify(body)),
    }),
    env,
  );

async function registerDevice() {
  const res = await call("/v1/register", { method: "POST", body: { model: "F22 Pro" } });
  const { device_id, token } = await res.json();
  return { device_id, headers: { authorization: `Bearer ${token}`, "x-device-id": device_id } };
}

const fakeApk = (n = 2048) => {
  const b = new Uint8Array(n);
  b.set([0x50, 0x4b, 0x03, 0x04]);
  for (let i = 4; i < n; i++) b[i] = i % 251;
  return b;
};

async function publish({ pkg = "com.future.camera", version_code = 20, channel = "stable", pct = 100, fixed_clusters } = {}) {
  const created = await call("/admin/releases", {
    method: "POST",
    headers: ADMIN,
    body: { pkg, version_code, version_name: `1.${version_code}`, channel, notes_he: "תיקון צילום", fixed_clusters },
  });
  assert.equal(created.status, 201);
  const { id } = await created.json();
  const up = await call(`/admin/releases/${id}/payload`, { method: "PUT", headers: ADMIN, raw: fakeApk() });
  assert.equal(up.status, 200);
  const roll = await call(`/admin/releases/${id}/rollout`, { method: "POST", headers: ADMIN, body: { pct } });
  assert.equal(roll.status, 200);
  return id;
}

async function check(dev, apps = [{ pkg: "com.future.camera", version_code: 10 }]) {
  const res = await call("/v1/update/check", { method: "POST", headers: dev.headers, body: { apps } });
  assert.equal(res.status, 200);
  return res.json();
}

async function verify(signed) {
  const sig = Buffer.from(signed.sig, "base64");
  return crypto.subtle.verify({ name: "ECDSA", hash: "SHA-256" }, publicKey, sig, new TextEncoder().encode(signed.manifest));
}

test("check returns a signed manifest bound to the device, and it verifies", async () => {
  const dev = await registerDevice();
  const id = await publish();
  const signed = await check(dev);
  assert.ok(await verify(signed));
  const m = JSON.parse(signed.manifest);
  assert.equal(m.device_id, dev.device_id);
  assert.ok(m.expires_at > m.issued_at);
  assert.equal(m.updates.length, 1);
  assert.equal(m.updates[0].release_id, id);
  assert.match(m.updates[0].sha256, /^[0-9a-f]{64}$/);
  assert.equal(m.updates[0].size, 2048);
  // מניפולציה במחרוזת שוברת את החתימה
  assert.equal(await verify({ ...signed, manifest: signed.manifest.replace("1.20", "1.99") }), false);
});

test("no update when installed is equal or newer, and only the highest version per package", async () => {
  const dev = await registerDevice();
  await publish({ version_code: 20 });
  await publish({ version_code: 30 });
  let m = JSON.parse((await check(dev, [{ pkg: "com.future.camera", version_code: 30 }])).manifest);
  assert.equal(m.updates.length, 0);
  m = JSON.parse((await check(dev)).manifest);
  assert.equal(m.updates.length, 1);
  assert.equal(m.updates[0].version_code, 30);
});

test("packages the device did not report are never offered", async () => {
  const dev = await registerDevice();
  await publish({ pkg: "com.future.music" });
  const m = JSON.parse((await check(dev)).manifest);
  assert.equal(m.updates.length, 0);
});

test("draft and paused releases are not offered; download of a paused release is 410", async () => {
  const dev = await registerDevice();
  const created = await call("/admin/releases", {
    method: "POST",
    headers: ADMIN,
    body: { pkg: "com.future.camera", version_code: 50, version_name: "1.50" },
  });
  const { id: draft } = await created.json();
  assert.equal(JSON.parse((await check(dev)).manifest).updates.length, 0, "draft");
  // פריסה בלי קובץ אסורה
  assert.equal((await call(`/admin/releases/${draft}/rollout`, { method: "POST", headers: ADMIN, body: { pct: 100 } })).status, 409);

  const id = await publish();
  assert.equal(JSON.parse((await check(dev)).manifest).updates.length, 1);
  assert.equal((await call(`/admin/releases/${id}/pause`, { method: "POST", headers: ADMIN })).status, 200);
  assert.equal(JSON.parse((await check(dev)).manifest).updates.length, 0);
  assert.equal((await call(`/v1/update/download/${id}`, { headers: dev.headers })).status, 410);
});

test("rollout percentage follows the stable per-device bucket", async () => {
  const devices = [];
  for (let i = 0; i < 12; i++) devices.push(await registerDevice());
  const id = await publish({ pct: 50 });
  for (const d of devices) {
    const offered = JSON.parse((await check(d)).manifest).updates.length === 1;
    assert.equal(offered, (await bucketOf(d.device_id, id)) < 50);
  }
  const b = await bucketOf("a", "b");
  assert.equal(b, await bucketOf("a", "b"));
  assert.ok(b >= 0 && b < 100);
});

test("channels: stable devices do not see beta or dev releases", async () => {
  const dev = await registerDevice();
  await publish({ channel: "beta" });
  assert.equal(JSON.parse((await check(dev)).manifest).updates.length, 0);
  assert.equal((await call(`/admin/devices/${dev.device_id}/channel`, { method: "POST", headers: ADMIN, body: { channel: "dev" } })).status, 200);
  assert.equal(JSON.parse((await check(dev)).manifest).updates.length, 1);
  assert.deepEqual(visibleChannels("dev"), ["dev", "beta", "stable"]);
  assert.deepEqual(visibleChannels("stable"), ["stable"]);
  assert.deepEqual(visibleChannels("weird"), ["stable"]);
});

test("download only for a release that was offered to this device, with size and hash", async () => {
  const a = await registerDevice();
  const b = await registerDevice();
  const id = await publish();
  assert.equal((await call(`/v1/update/download/${id}`, { headers: a.headers })).status, 404, "not offered yet");
  await check(a);
  const res = await call(`/v1/update/download/${id}`, { headers: a.headers });
  assert.equal(res.status, 200);
  const bytes = new Uint8Array(await res.arrayBuffer());
  assert.equal(bytes.length, 2048);
  assert.deepEqual(bytes, fakeApk());
  assert.equal((await call(`/v1/update/download/${id}`, { headers: b.headers })).status, 404, "other device");
  assert.equal((await call(`/v1/update/download/${id}`)).status, 401);
});

test("upload rejects non-zip and replacing the file of a published release", async () => {
  const created = await call("/admin/releases", {
    method: "POST",
    headers: ADMIN,
    body: { pkg: "com.future.camera", version_code: 7, version_name: "0.7" },
  });
  const { id } = await created.json();
  assert.equal((await call(`/admin/releases/${id}/payload`, { method: "PUT", headers: ADMIN, raw: new Uint8Array([1, 2, 3, 4]) })).status, 400);
  const pub = await publish({ version_code: 8 });
  assert.equal((await call(`/admin/releases/${pub}/payload`, { method: "PUT", headers: ADMIN, raw: fakeApk() })).status, 409);
});

test("release validation and duplicate versions", async () => {
  const bad = [
    { pkg: "org.evil.app", version_code: 1, version_name: "1" },
    { pkg: "com.future.camera", version_code: 0, version_name: "1" },
    { pkg: "com.future.camera", version_code: 1, version_name: "<script>" },
    { pkg: "com.future.camera", version_code: 1, version_name: "1", channel: "nightly" },
    { pkg: "com.future.camera", version_code: 1, version_name: "1", fixed_clusters: ["x"] },
  ];
  for (const body of bad) assert.equal((await call("/admin/releases", { method: "POST", headers: ADMIN, body })).status, 400, JSON.stringify(body));
  const ok = { pkg: "com.future.camera", version_code: 3, version_name: "3" };
  assert.equal((await call("/admin/releases", { method: "POST", headers: ADMIN, body: ok })).status, 201);
  assert.equal((await call("/admin/releases", { method: "POST", headers: ADMIN, body: ok })).status, 409);
  // בלי טוקן מנהל
  assert.equal((await call("/admin/releases", { method: "POST", body: ok })).status, 503);
});

test("installed event sends 'fixed in X' only to this device's feedback in the fixed cluster", async () => {
  const a = await registerDevice();
  const b = await registerDevice();
  const clusterId = crypto.randomUUID();
  const t = Date.now();
  await env.DB.prepare(
    "INSERT INTO clusters (id, app_pkg, title_he, embedding, created_at, updated_at, state) VALUES (?1, 'com.future.camera', 'קריסה', '[]', ?2, ?2, 'deep')",
  )
    .bind(clusterId, t)
    .run();
  for (const d of [a, b]) {
    await env.DB.prepare(
      "INSERT INTO feedback (id, device_id, client_id, type, text, app_pkg, status, cluster_id, created_at) VALUES (?1, ?2, ?3, 'bug', 'x', 'com.future.camera', 'in_progress', ?4, ?5)",
    )
      .bind(crypto.randomUUID(), d.device_id, crypto.randomUUID(), clusterId, t)
      .run();
  }
  const id = await publish({ fixed_clusters: [clusterId] });
  await check(a);
  const res = await call("/v1/update/event", { method: "POST", headers: a.headers, body: { release_id: id, event: "installed" } });
  assert.equal((await res.json()).notified, 1);

  const inboxA = await (await call("/v1/inbox", { headers: a.headers })).json();
  assert.equal(inboxA.messages.length, 1);
  assert.equal(inboxA.messages[0].kind, "fixed");
  assert.match(inboxA.messages[0].text_he, /1\.20/);
  const inboxB = await (await call("/v1/inbox", { headers: b.headers })).json();
  assert.equal(inboxB.messages.length, 0, "b has not installed yet");
  const cluster = await env.DB.prepare("SELECT state, fixed_in FROM clusters WHERE id = ?1").bind(clusterId).first();
  assert.equal(cluster.state, "fixed");
  assert.equal(cluster.fixed_in, "1.20");
});

test("events are only accepted for offered releases and validated", async () => {
  const dev = await registerDevice();
  const id = await publish();
  assert.equal((await call("/v1/update/event", { method: "POST", headers: dev.headers, body: { release_id: id, event: "installed" } })).status, 404);
  await check(dev);
  assert.equal((await call("/v1/update/event", { method: "POST", headers: dev.headers, body: { release_id: id, event: "offered" } })).status, 400);
  assert.equal((await call("/v1/update/event", { method: "POST", headers: dev.headers, body: { release_id: id, event: "failed", error: "a\nb" } })).status, 400);
  assert.equal((await call("/v1/update/event", { method: "POST", headers: dev.headers, body: { release_id: id, event: "failed", error: "STATUS_FAILURE_CONFLICT" } })).status, 200);
});

test("a device that just failed is not offered the same release again for a day", async () => {
  const dev = await registerDevice();
  const id = await publish();
  await check(dev);
  await call("/v1/update/event", { method: "POST", headers: dev.headers, body: { release_id: id, event: "failed", error: "verify" } });
  assert.equal(JSON.parse((await check(dev)).manifest).updates.length, 0);
});

test("rollout pauses automatically when installs fail", async () => {
  const id = await publish();
  const devices = [];
  for (let i = 0; i < 4; i++) devices.push(await registerDevice());
  for (const d of devices) await check(d);
  await call("/v1/update/event", { method: "POST", headers: devices[0].headers, body: { release_id: id, event: "installed" } });
  for (const d of devices.slice(1)) {
    await call("/v1/update/event", { method: "POST", headers: d.headers, body: { release_id: id, event: "failed", error: "STATUS_FAILURE" } });
  }
  const list = await (await call("/admin/releases", { headers: ADMIN })).json();
  const r = list.releases.find((x) => x.id === id);
  assert.equal(r.state, "paused");
  assert.match(r.pause_reason, /^auto/);
  assert.equal(r.failed, 3);
  assert.equal(r.installed, 1);
});

test("update check is 503 without a signing key, and validates the app list", async () => {
  const dev = await registerDevice();
  assert.equal((await call("/v1/update/check", { method: "POST", headers: dev.headers, body: { apps: [{ pkg: "x", version_code: 1 }] } })).status, 400);
  assert.equal((await call("/v1/update/check", { method: "POST", headers: dev.headers, body: { apps: "nope" } })).status, 400);
  delete env.UPDATE_SIGNING_KEY;
  assert.equal((await call("/v1/update/check", { method: "POST", headers: dev.headers, body: { apps: [] } })).status, 503);
});

test("ota releases are offered only to devices that declare ota support", async () => {
  const dev = await registerDevice();
  const created = await call("/admin/releases", {
    method: "POST",
    headers: ADMIN,
    body: { kind: "ota", pkg: "com.future.system", version_code: 2, version_name: "FutureOS 1.1" },
  });
  const { id } = await created.json();
  await call(`/admin/releases/${id}/payload`, { method: "PUT", headers: ADMIN, raw: fakeApk() });
  await call(`/admin/releases/${id}/rollout`, { method: "POST", headers: ADMIN, body: { pct: 100 } });
  const apps = [{ pkg: "com.future.system", version_code: 1 }];
  let res = await call("/v1/update/check", { method: "POST", headers: dev.headers, body: { apps } });
  assert.equal(JSON.parse((await res.json()).manifest).updates.length, 0);
  res = await call("/v1/update/check", { method: "POST", headers: dev.headers, body: { apps, kinds: ["apk", "ota"] } });
  assert.equal(JSON.parse((await res.json()).manifest).updates[0].kind, "ota");
});

test("DELETE /v1/me also removes update events", async () => {
  const dev = await registerDevice();
  await publish();
  await check(dev);
  assert.equal((await call("/v1/me", { method: "DELETE", headers: dev.headers })).status, 204);
  const row = await env.DB.prepare("SELECT COUNT(*) AS n FROM update_events WHERE device_id = ?1").bind(dev.device_id).first();
  assert.equal(row.n, 0);
});
