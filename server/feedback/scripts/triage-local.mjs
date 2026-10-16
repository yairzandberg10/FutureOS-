// מריץ סבב מיון אחד על מסד הנתונים של dev-server (.local/dev.sqlite), או על דוגמאות אם המסד ריק.
// שימוש:  node scripts/triage-local.mjs [--demo]
// בלי GEMINI_API_KEY ב-.dev.vars הסיווג וה-embeddings מגיעים מ-mock. בלי GITHUB_TOKEN לא נפתח issue.

import { join } from "node:path";
import { mkdirSync } from "node:fs";
import { runTriage } from "../src/triage.js";
import { makeEnv, loadDevVars, ROOT } from "./local-env.mjs";

const demo = process.argv.includes("--demo");
const dataDir = join(ROOT, ".local");
mkdirSync(dataDir, { recursive: true });
const env = demo
  ? makeEnv({ ...loadDevVars(), GITHUB_TOKEN: "" })
  : makeEnv(loadDevVars(), { dbPath: join(dataDir, "dev.sqlite"), blobDir: join(dataDir, "blobs") });

if (demo) {
  const t = Date.now();
  await env.DB.prepare("INSERT INTO devices (id, token_hash, created_at, last_seen) VALUES ('d1', 'x', ?1, ?1), ('d2', 'x', ?1, ?1), ('d3', 'x', ?1, ?1)").bind(t).run();
  const samples = [
    ["d1", "bug", "המצלמה קורסת כשלוחצים על מקש 5 לצילום", "com.future.camera"],
    ["d2", "bug", "המצלמה נסגרת כשלוחצים 5 לצילום תמונה", "com.future.camera"],
    ["d3", "idea", "אשמח לאפשרות טיימר בצילום", "com.future.camera"],
    ["d1", "bug", "לא", "com.future.music"],
  ];
  for (const [i, [dev, type, text, pkg]] of samples.entries()) {
    await env.DB.prepare("INSERT INTO feedback (id, device_id, client_id, type, text, app_pkg, created_at) VALUES (?1, ?2, ?3, ?4, ?5, ?6, ?7)")
      .bind(`f${i}`, dev, `c${i}xxxxxxx`, type, text, pkg, t + i)
      .run();
  }
}

const summary = await runTriage(env);
console.log("summary:", summary);
const { results } = await env.DB.prepare("SELECT title_he, app_pkg, state, severity, members FROM clusters").all();
console.table(results);
const inbox = await env.DB.prepare("SELECT device_id, kind, text_he FROM inbox").all();
console.table(inbox.results);
