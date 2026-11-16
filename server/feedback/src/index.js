// נקודת הכניסה של ה-Worker: ניתוב בקשות, ו-Cron למיון המשובים.
// מבנה: /v1/* למכשירים (טוקן מכשיר), /admin/* למשתמש (Cloudflare Access).
// /v1/update/* הוא מנוע העדכונים (updates.js), השאר משוב וניווט.

import { HttpError, json, errorResponse } from "./util.js";
import { register, authenticate, submitFeedback, listFeedback, inbox, deleteMe } from "./devices.js";
import { route, transit, departures, siriStop } from "./nav.js";
import { submitReport, nearbyReports, voteReport } from "./reports.js";
import { handleAdmin } from "./admin.js";
import { runTriage } from "./triage.js";
import { checkUpdates, download, reportEvent } from "./updates.js";

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
      case "POST /v1/nav/transit":
        return transit(request, env, device);
      case "POST /v1/nav/departures":
        return departures(request, env, device);
      case "POST /v1/nav/report":
        return submitReport(request, env, device);
      case "POST /v1/nav/reports/nearby":
        return nearbyReports(request, env, device);
      case "POST /v1/nav/report/vote":
        return voteReport(request, env, device);
      case "POST /v1/nav/siri/stop":
        return siriStop(request, env, device);
      case "POST /v1/update/check":
        return checkUpdates(request, env, device);
      case "POST /v1/update/event":
        return reportEvent(request, env, device);
    }
    const dl = /^\/v1\/update\/download\/([^/]+)$/.exec(path);
    if (dl && method === "GET") return download(env, device, dl[1]);
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
