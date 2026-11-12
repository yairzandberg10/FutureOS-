/**
 * Cloud Functions for the FutureOS Navigation app.
 *
 * Three call proxies (geocoding, driving routes, realtime transit) so the
 * device never holds an API key, plus one scheduled job that turns the
 * national GTFS feed into a small per-region bundle the device can download
 * instead of processing 130MB of CSV itself.
 *
 * Every callable requires an authenticated caller. The app signs in
 * anonymously at startup, which is enough for `request.auth` to be set and
 * lets firestore.rules/storage.rules require a signed-in user instead of
 * leaving the project open to the world.
 */
import { setGlobalOptions } from "firebase-functions/v2";
import { onCall, onRequest, HttpsError, CallableRequest } from "firebase-functions/v2/https";
import { onSchedule } from "firebase-functions/v2/scheduler";
import { defineSecret } from "firebase-functions/params";
import * as logger from "firebase-functions/logger";
import * as admin from "firebase-admin";

import { REGIONS, buildRegionBundle } from "./gtfs";

admin.initializeApp();

// Must match FIREBASE_FUNCTIONS_REGION in Navigation/app/build.gradle.kts,
// otherwise every call from the device comes back NOT_FOUND.
setGlobalOptions({ region: "europe-west1", maxInstances: 10 });

const HERE_API_KEY = defineSecret("HERE_API_KEY");
const SIRI_BASE_URL = defineSecret("SIRI_BASE_URL");
const SIRI_API_KEY = defineSecret("SIRI_API_KEY");
const ADMIN_TOKEN = defineSecret("ADMIN_TOKEN");

const NOMINATIM_URL = "https://nominatim.openstreetmap.org/search";
const NOMINATIM_USER_AGENT = "FutureOS-Navigation/1.0 (https://github.com/yairzandberg10/FutureOS-)";
const NOMINATIM_MIN_INTERVAL_MS = 1100;
const GEOCODE_CACHE_TTL_MS = 30 * 24 * 60 * 60 * 1000;

const HERE_ROUTES_URL = "https://router.hereapi.com/v8/routes";

function requireCaller(request: CallableRequest<unknown>): void {
  if (!request.auth) {
    throw new HttpsError("unauthenticated", "sign in first (the app signs in anonymously)");
  }
}

function sleep(ms: number): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

/**
 * Nominatim's usage policy allows at most one request per second from a
 * given client, and this project is now that single client for every device.
 * The gate is a transaction on one document: whoever gets there first
 * reserves the next slot and everyone else waits for theirs.
 */
async function waitForNominatimSlot(): Promise<void> {
  const ref = admin.firestore().collection("service_state").doc("nominatim");
  const waitMs = await admin.firestore().runTransaction(async (tx) => {
    const snapshot = await tx.get(ref);
    const now = Date.now();
    const nextAllowedAt = (snapshot.data()?.nextAllowedAt as number | undefined) ?? 0;
    const slot = Math.max(now, nextAllowedAt);
    tx.set(ref, { nextAllowedAt: slot + NOMINATIM_MIN_INTERVAL_MS }, { merge: true });
    return slot - now;
  });
  if (waitMs > 0) {
    await sleep(Math.min(waitMs, 10_000));
  }
}

interface GeocodePlace {
  label: string;
  lat: number;
  lon: number;
}

/**
 * Address search. The cache is shared by every device, so the same query
 * typed on a keypad over and over costs one upstream request in total
 * rather than one per device per retype.
 */
export const geocodeSearch = onCall(async (request: CallableRequest<{ query?: string }>) => {
  requireCaller(request);
  const query = (request.data?.query ?? "").trim();
  if (query.length === 0) {
    return { results: [] as GeocodePlace[] };
  }

  const cacheId = Buffer.from(query.toLowerCase()).toString("base64url").slice(0, 400);
  const cacheRef = admin.firestore().collection("geocode_cache").doc(cacheId);
  const cached = await cacheRef.get();
  const cachedAt = cached.data()?.cachedAt as number | undefined;
  if (cached.exists && cachedAt !== undefined && Date.now() - cachedAt < GEOCODE_CACHE_TTL_MS) {
    return { results: (cached.data()?.results ?? []) as GeocodePlace[] };
  }

  await waitForNominatimSlot();

  const url = new URL(NOMINATIM_URL);
  url.searchParams.set("q", query);
  url.searchParams.set("format", "jsonv2");
  url.searchParams.set("limit", "5");
  url.searchParams.set("accept-language", "he");

  const response = await fetch(url, { headers: { "User-Agent": NOMINATIM_USER_AGENT } });
  if (!response.ok) {
    throw new HttpsError("unavailable", `nominatim returned ${response.status}`);
  }
  const raw = (await response.json()) as Array<{ display_name?: string; lat?: string; lon?: string }>;

  const seen = new Set<string>();
  const results: GeocodePlace[] = [];
  for (const entry of raw) {
    const label = entry.display_name;
    const lat = Number(entry.lat);
    const lon = Number(entry.lon);
    if (!label || Number.isNaN(lat) || Number.isNaN(lon)) continue;
    // The app uses label+coordinates as a list key, and a duplicate key is a
    // crash there - so duplicates are dropped here, at the source.
    const key = `${label}|${lat}|${lon}`;
    if (seen.has(key)) continue;
    seen.add(key);
    results.push({ label, lat, lon });
  }

  await cacheRef.set({ query, results, cachedAt: Date.now() });
  return { results };
});

/**
 * Driving route with live traffic. Returns HERE's response body untouched:
 * the app already parses this exact shape (HereModels.kt), and one parser is
 * better than two that can drift apart.
 */
export const drivingRoute = onCall(
  { secrets: [HERE_API_KEY] },
  async (
    request: CallableRequest<{
      originLat?: number;
      originLon?: number;
      destinationLat?: number;
      destinationLon?: number;
    }>,
  ) => {
    requireCaller(request);
    const { originLat, originLon, destinationLat, destinationLon } = request.data ?? {};
    if (
      typeof originLat !== "number" || typeof originLon !== "number" ||
      typeof destinationLat !== "number" || typeof destinationLon !== "number"
    ) {
      throw new HttpsError("invalid-argument", "origin and destination coordinates are required");
    }

    const apiKey = HERE_API_KEY.value();
    if (!apiKey) {
      throw new HttpsError("failed-precondition", "HERE_API_KEY secret is not set");
    }

    const url = new URL(HERE_ROUTES_URL);
    url.searchParams.set("origin", `${originLat},${originLon}`);
    url.searchParams.set("destination", `${destinationLat},${destinationLon}`);
    url.searchParams.set("transportMode", "car");
    url.searchParams.set("routingMode", "fast");
    url.searchParams.set("return", "polyline,summary,travelSummary,turnByTurnActions");
    url.searchParams.set("lang", "he");
    // No departureTime on purpose: HERE uses "now" when it is omitted, which
    // is what makes the result account for live traffic.
    url.searchParams.set("traffic[mode]", "default");
    url.searchParams.set("apikey", apiKey);

    const response = await fetch(url);
    const body = await response.text();
    if (!response.ok) {
      logger.warn("HERE routing failed", { status: response.status, body: body.slice(0, 500) });
      throw new HttpsError("unavailable", `here returned ${response.status}`);
    }
    return { route: body };
  },
);

function escapeXml(value: string): string {
  return value
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&apos;");
}

/**
 * Realtime arrivals (SIRI StopMonitoring, Israeli Ministry of Transport).
 * The request body mirrors SiriXml.buildStopMonitoringRequest on the device,
 * and the raw XML response goes back for the app's existing parser.
 */
export const siriStopMonitoring = onCall(
  { secrets: [SIRI_BASE_URL, SIRI_API_KEY] },
  async (request: CallableRequest<{ stopId?: string; lineRef?: string | null }>) => {
    requireCaller(request);
    const stopId = request.data?.stopId;
    if (!stopId) {
      throw new HttpsError("invalid-argument", "stopId is required");
    }
    const baseUrl = SIRI_BASE_URL.value();
    const apiKey = SIRI_API_KEY.value();
    if (!baseUrl || !apiKey) {
      throw new HttpsError("failed-precondition", "SIRI_BASE_URL / SIRI_API_KEY secrets are not set");
    }

    const timestamp = new Date().toISOString();
    const lineRef = request.data?.lineRef;
    const lineRefXml = lineRef ? `<LineRef>${escapeXml(lineRef)}</LineRef>` : "";
    const body = `<?xml version="1.0" encoding="UTF-8"?>
<Siri xmlns="http://www.siri.org.uk/siri" version="2.0">
  <ServiceRequest>
    <RequestTimestamp>${timestamp}</RequestTimestamp>
    <RequestorRef>${escapeXml(apiKey)}</RequestorRef>
    <StopMonitoringRequest version="2.0">
      <RequestTimestamp>${timestamp}</RequestTimestamp>
      <MonitoringRef>${escapeXml(stopId)}</MonitoringRef>
      ${lineRefXml}
      <MaximumStopVisits>20</MaximumStopVisits>
    </StopMonitoringRequest>
  </ServiceRequest>
</Siri>`;

    const response = await fetch(baseUrl, {
      method: "POST",
      headers: { "Content-Type": "text/xml; charset=utf-8" },
      body,
    });
    const xml = await response.text();
    if (!response.ok) {
      logger.warn("SIRI request failed", { status: response.status });
      throw new HttpsError("unavailable", `siri returned ${response.status}`);
    }
    return { xml };
  },
);

/**
 * Rebuilds every region bundle once a night. The Ministry of Transport
 * refreshes the national feed nightly, and 03:30 local time is after that
 * and before anyone is travelling.
 */
export const refreshTransitBundle = onSchedule(
  {
    schedule: "30 3 * * *",
    timeZone: "Asia/Jerusalem",
    memory: "4GiB",
    timeoutSeconds: 1800,
  },
  async () => {
    for (const region of REGIONS) {
      try {
        const summary = await buildRegionBundle(region);
        logger.info("transit bundle rebuilt", summary);
      } catch (error) {
        logger.error("transit bundle failed", { region: region.id, error: String(error) });
      }
    }
  },
);

/**
 * Same job, on demand - the scheduled run only happens at night, and the
 * first bundle has to exist before the app can use it at all. Guarded by the
 * ADMIN_TOKEN secret because it is an unauthenticated HTTP endpoint.
 */
export const refreshTransitBundleNow = onRequest(
  {
    secrets: [ADMIN_TOKEN],
    memory: "4GiB",
    timeoutSeconds: 1800,
  },
  async (request, response) => {
    const token = request.query.token ?? request.get("x-admin-token");
    if (!ADMIN_TOKEN.value() || token !== ADMIN_TOKEN.value()) {
      response.status(403).send("forbidden");
      return;
    }
    const regionId = String(request.query.region ?? REGIONS[0].id);
    const region = REGIONS.find((candidate) => candidate.id === regionId);
    if (!region) {
      response.status(400).send(`unknown region: ${regionId}`);
      return;
    }
    try {
      const summary = await buildRegionBundle(region);
      response.status(200).json(summary);
    } catch (error) {
      logger.error("manual transit bundle failed", { region: region.id, error: String(error) });
      response.status(500).send(String(error));
    }
  },
);
