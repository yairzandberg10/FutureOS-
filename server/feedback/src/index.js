// נקודת הכניסה של ה-Worker: ניתוב בקשות, ו-Cron למיון המשובים.
// מבנה: /v1/* למכשירים (טוקן מכשיר), /admin/* למשתמש (Cloudflare Access).

import { HttpError, json, errorResponse } from "./util.js";
import { register, authenticate, submitFeedback, listFeedback, inbox, deleteMe } from "./devices.js";
import { route, siriStop } from "./nav.js";
import { handleAdmin } from "./admin.js";
import { runTriage } from "./triage.js";

export async function handle(request, env) {
  const url = new URL(request.url);
  const path = url.pathname.replace(/\/+$/, "") || "/";
  const method = request.method;

  if (path === "/v1/health" && method === "GET") return json({ ok: true });
  if (path === "/v1/register" && method === "POST") return register(request, env);

  if (path.startsWith("/admin/")) return handleAdmin(request, env, path);

  if (path.startsWith("/v1/")) {
    const device = await authenticate(request, env);
    switch (`${method} ${path}`) {
      case "POST /v1/feedback":
        return submitFeedback(request, env, device);
      case "GET /v1/feedback":
        return listFeedback(env, device);
      case "GET /v1/inbox":
        return inbox(request, env, device);
      case "DELETE /v1/me":
        return deleteMe(env, device);
      case "POST /v1/nav/route":
        return route(request, env, device);
      case "POST /v1/nav/siri/stop":
        return siriStop(request, env, device);
    }
  }
  throw new HttpError(404, "not_found");
}

export default {
  async fetch(request, env) {
    try {
      return await handle(request, env);
    } catch (err) {
      return errorResponse(err);
    }
  },

  async scheduled(_event, env, ctx) {
    ctx.waitUntil(runTriage(env).then((s) => console.log("triage", JSON.stringify(s))));
  },
};
