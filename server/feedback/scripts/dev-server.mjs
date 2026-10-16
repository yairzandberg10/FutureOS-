// שרת פיתוח מקומי בלי wrangler: מריץ את אותו קוד Worker מעל node:http, עם D1/R2 מקומיים בתיקייה .local/.
// שימוש:  node scripts/dev-server.mjs [port]      (ברירת מחדל 8787)
// מהטלפון: adb reverse tcp:8787 tcp:8787, ואז FUTURE_API_BASE=http://127.0.0.1:8787/v1 בבניית בדיקה.
// משתני סביבה נקראים מ-.dev.vars (ראו .dev.vars.example). בלי GEMINI_API_KEY המיון רץ על mock.

import { createServer } from "node:http";
import { join } from "node:path";
import { mkdirSync } from "node:fs";
import worker from "../src/index.js";
import { makeEnv, loadDevVars, LocalCache, ROOT } from "./local-env.mjs";

const port = Number(process.argv[2] || 8787);
const dataDir = join(ROOT, ".local");
mkdirSync(dataDir, { recursive: true });

globalThis.caches ??= { default: new LocalCache() };
const env = makeEnv({ ENVIRONMENT: "local", ...loadDevVars() }, { dbPath: join(dataDir, "dev.sqlite"), blobDir: join(dataDir, "blobs") });

createServer(async (req, res) => {
  const chunks = [];
  for await (const c of req) chunks.push(c);
  const body = chunks.length ? Buffer.concat(chunks) : undefined;
  const request = new Request(`http://${req.headers.host}${req.url}`, {
    method: req.method,
    headers: { ...req.headers, "cf-connecting-ip": req.socket.remoteAddress },
    body: ["GET", "HEAD"].includes(req.method) ? undefined : body,
  });
  const response = await worker.fetch(request, env);
  res.writeHead(response.status, Object.fromEntries(response.headers));
  res.end(Buffer.from(await response.arrayBuffer()));
  console.log(req.method, req.url, response.status);
}).listen(port, "127.0.0.1", () => {
  console.log(`FutureOS feedback dev server: http://127.0.0.1:${port}/v1  (mock Gemini: ${!env.GEMINI_API_KEY})`);
});
