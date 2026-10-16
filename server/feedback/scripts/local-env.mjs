// סביבה מקומית בלי Cloudflare ובלי npm install: D1 מעל node:sqlite, R2 מעל Map או תיקייה, Cache מעל Map.
// משמש את הבדיקות (node --test), את dev-server.mjs ואת triage-local.mjs. דורש Node 22.5 ומעלה.

import { DatabaseSync } from "node:sqlite";
import { readFileSync, mkdirSync, writeFileSync, existsSync, rmSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";

const ROOT = join(dirname(fileURLToPath(import.meta.url)), "..");

const clean = (args) => args.map((a) => (a === undefined ? null : typeof a === "boolean" ? Number(a) : a));

class Stmt {
  constructor(db, sql, args = []) {
    this.db = db;
    this.sql = sql;
    this.args = args;
  }
  bind(...args) {
    return new Stmt(this.db, this.sql, clean(args));
  }
  async first(column) {
    const row = this.db.prepare(this.sql).get(...this.args);
    if (!row) return null;
    const plain = { ...row };
    return column ? plain[column] : plain;
  }
  async all() {
    return { success: true, results: this.db.prepare(this.sql).all(...this.args).map((r) => ({ ...r })) };
  }
  async run() {
    const info = this.db.prepare(this.sql).run(...this.args);
    return { success: true, meta: { changes: Number(info.changes) } };
  }
}

export class LocalD1 {
  constructor(path = ":memory:") {
    this.db = new DatabaseSync(path);
    this.db.exec("PRAGMA foreign_keys = ON");
    this.db.exec(readFileSync(join(ROOT, "schema.sql"), "utf8"));
  }
  prepare(sql) {
    return new Stmt(this.db, sql);
  }
  async batch(stmts) {
    this.db.exec("BEGIN");
    try {
      const out = [];
      for (const s of stmts) out.push(await s.run());
      this.db.exec("COMMIT");
      return out;
    } catch (e) {
      this.db.exec("ROLLBACK");
      throw e;
    }
  }
}

export class LocalR2 {
  constructor(dir = null) {
    this.dir = dir;
    this.map = new Map();
    if (dir) mkdirSync(dir, { recursive: true });
  }
  file(key) {
    return join(this.dir, key.replace(/[^\w.-]/g, "_"));
  }
  async put(key, value) {
    if (this.dir) writeFileSync(this.file(key), value);
    else this.map.set(key, String(value));
  }
  async get(key) {
    const text = this.dir ? (existsSync(this.file(key)) ? readFileSync(this.file(key), "utf8") : null) : this.map.get(key);
    return text == null ? null : { text: async () => text };
  }
  async delete(key) {
    if (this.dir) rmSync(this.file(key), { force: true });
    else this.map.delete(key);
  }
}

export class LocalCache {
  constructor() {
    this.map = new Map();
  }
  async match(req) {
    const hit = this.map.get(req.url);
    return hit ? hit.clone() : undefined;
  }
  async put(req, res) {
    this.map.set(req.url, res);
  }
}

/** env מלא לבדיקות. overrides דורס משתני סביבה (מפתחות, ספים). */
export function makeEnv(overrides = {}, { dbPath, blobDir } = {}) {
  return {
    DB: new LocalD1(dbPath),
    BLOBS: new LocalR2(blobDir),
    FEEDBACK_PER_DAY: "10",
    ...overrides,
  };
}

/** קורא .dev.vars (KEY=VALUE בשורות), כמו ש-wrangler dev עושה. */
export function loadDevVars(path = join(ROOT, ".dev.vars")) {
  if (!existsSync(path)) return {};
  const out = {};
  for (const line of readFileSync(path, "utf8").split(/\r?\n/)) {
    const m = /^\s*([A-Z0-9_]+)\s*=\s*"?(.*?)"?\s*$/.exec(line);
    if (m && !line.trim().startsWith("#")) out[m[1]] = m[2];
  }
  return out;
}

export { ROOT };
