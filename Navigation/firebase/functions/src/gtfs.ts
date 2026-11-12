/**
 * Turns the national GTFS feed into one small bundle per region.
 *
 * The feed (gtfs.mot.gov.il, ~130MB zipped, the whole country, refreshed
 * nightly) used to be downloaded and parsed by every device. Here it is
 * downloaded once, filtered down to a bounding box, and written out as
 * gzipped NDJSON - one JSON object per line, so the device can parse it with
 * a fixed amount of memory (see TransitBundleSource.kt).
 *
 * The filter chain is the same one the app used locally, and in the same
 * order, because each step needs the ids the previous one kept:
 * stops (inside the box) -> stop_times (only those stops) -> trips (only
 * those stop_times) -> routes/calendar (only what those trips reference).
 */
import * as fs from "fs";
import * as os from "os";
import * as path from "path";
import * as readline from "readline";
import * as zlib from "zlib";
import { Readable } from "stream";
import { pipeline } from "stream/promises";
import * as admin from "firebase-admin";
import * as logger from "firebase-functions/logger";
import unzipper from "unzipper";

export interface Region {
  id: string;
  minLat: number;
  maxLat: number;
  minLon: number;
  maxLon: number;
}

/**
 * Greater Tel Aviv, the same box the app shipped with (GtfsConfig.kt).
 * Adding a region here is all it takes to cover another area - the app picks
 * whichever bundle covers the box it asks for.
 */
export const REGIONS: Region[] = [
  { id: "tel-aviv", minLat: 31.95, maxLat: 32.25, minLon: 34.7, maxLon: 34.9 },
];

const FEED_URL = "https://gtfs.mot.gov.il/gtfsfiles/israel-public-transportation.zip";

export interface BundleSummary {
  region: string;
  version: string;
  storagePath: string;
  stops: number;
  routes: number;
  trips: number;
  stopTimes: number;
  calendar: number;
  bytes: number;
}

/**
 * GTFS files are plain CSV but fields may be quoted and contain commas
 * (Hebrew stop names do, often). Small hand-rolled splitter rather than a
 * dependency, same as the app's own GtfsCsv.
 */
function splitCsvLine(line: string): string[] {
  const fields: string[] = [];
  let current = "";
  let inQuotes = false;
  for (let i = 0; i < line.length; i++) {
    const char = line[i];
    if (inQuotes) {
      if (char === '"') {
        if (line[i + 1] === '"') {
          current += '"';
          i++;
        } else {
          inQuotes = false;
        }
      } else {
        current += char;
      }
    } else if (char === '"') {
      inQuotes = true;
    } else if (char === ",") {
      fields.push(current);
      current = "";
    } else if (char !== "\r") {
      current += char;
    }
  }
  fields.push(current);
  return fields;
}

/** "25:10:00" is legal GTFS (a trip past midnight) and must stay > 86400. */
function gtfsTimeToSeconds(value: string): number | null {
  const parts = value.trim().split(":");
  if (parts.length < 2) return null;
  const hours = Number(parts[0]);
  const minutes = Number(parts[1]);
  const seconds = parts.length > 2 ? Number(parts[2]) : 0;
  if (Number.isNaN(hours) || Number.isNaN(minutes) || Number.isNaN(seconds)) return null;
  return hours * 3600 + minutes * 60 + seconds;
}

async function downloadFeed(destination: string): Promise<void> {
  const response = await fetch(FEED_URL);
  if (!response.ok || !response.body) {
    throw new Error(`GTFS download failed: ${response.status}`);
  }
  await pipeline(
    Readable.fromWeb(response.body as Parameters<typeof Readable.fromWeb>[0]),
    fs.createWriteStream(destination),
  );
}

/**
 * Streams one file out of the zip, row by row, as {column: value} objects
 * keyed by the header line - so extra or reordered columns in the feed do
 * not break anything.
 */
async function forEachCsvRow(
  zipPath: string,
  entryName: string,
  onRow: (row: Record<string, string>) => void,
): Promise<void> {
  const directory = await unzipper.Open.file(zipPath);
  const entry = directory.files.find((file) => file.path === entryName);
  if (!entry) {
    throw new Error(`${entryName} is missing from the GTFS archive`);
  }

  const reader = readline.createInterface({ input: entry.stream(), crlfDelay: Infinity });
  let header: string[] | null = null;
  for await (const line of reader) {
    if (line.length === 0) continue;
    if (header === null) {
      // The feed is UTF-8 with a BOM on some files.
      header = splitCsvLine(line.replace(/^﻿/, ""));
      continue;
    }
    const values = splitCsvLine(line);
    const row: Record<string, string> = {};
    for (let i = 0; i < header.length; i++) {
      row[header[i]] = values[i] ?? "";
    }
    onRow(row);
  }
}

/** Buffered NDJSON writer - one JSON object per line, gzipped on the way out. */
class BundleWriter {
  private readonly gzip = zlib.createGzip({ level: 6 });
  private readonly done: Promise<void>;
  private buffer: string[] = [];

  constructor(outputPath: string) {
    this.done = pipeline(this.gzip, fs.createWriteStream(outputPath));
  }

  write(value: Record<string, unknown>): void {
    this.buffer.push(JSON.stringify(value));
    if (this.buffer.length >= 2000) {
      this.flush();
    }
  }

  private flush(): void {
    if (this.buffer.length === 0) return;
    this.gzip.write(this.buffer.join("\n") + "\n");
    this.buffer = [];
  }

  async close(): Promise<void> {
    this.flush();
    this.gzip.end();
    await this.done;
  }
}

export async function buildRegionBundle(region: Region): Promise<BundleSummary> {
  const workDir = fs.mkdtempSync(path.join(os.tmpdir(), "gtfs-"));
  const zipPath = path.join(workDir, "feed.zip");
  const bundlePath = path.join(workDir, "bundle.ndjson.gz");

  try {
    logger.info("downloading the national GTFS feed", { region: region.id });
    await downloadFeed(zipPath);

    const writer = new BundleWriter(bundlePath);
    const keptStops = new Set<string>();
    const keptTrips = new Set<string>();
    const keptRoutes = new Set<string>();
    const keptServices = new Set<string>();
    let stopTimeCount = 0;
    let routeCount = 0;
    let calendarCount = 0;
    let tripCount = 0;

    // 1. stops inside the box
    await forEachCsvRow(zipPath, "stops.txt", (row) => {
      const lat = Number(row["stop_lat"]);
      const lon = Number(row["stop_lon"]);
      const id = row["stop_id"];
      if (!id || Number.isNaN(lat) || Number.isNaN(lon)) return;
      if (lat < region.minLat || lat > region.maxLat || lon < region.minLon || lon > region.maxLon) return;
      keptStops.add(id);
      writer.write({ t: "stop", id, name: row["stop_name"] ?? "", lat, lon });
    });

    // 2. stop_times for those stops (this is the big one, ~10M rows nationally)
    await forEachCsvRow(zipPath, "stop_times.txt", (row) => {
      const stopId = row["stop_id"];
      if (!stopId || !keptStops.has(stopId)) return;
      const tripId = row["trip_id"];
      if (!tripId) return;
      const arrival = gtfsTimeToSeconds(row["arrival_time"] ?? "");
      if (arrival === null) return;
      const departure = gtfsTimeToSeconds(row["departure_time"] ?? "") ?? arrival;
      keptTrips.add(tripId);
      stopTimeCount++;
      writer.write({
        t: "time",
        trip: tripId,
        stop: stopId,
        arr: arrival,
        dep: departure,
        seq: Number(row["stop_sequence"] ?? "0") || 0,
      });
    });

    // 3. trips that actually stop at one of those stops
    await forEachCsvRow(zipPath, "trips.txt", (row) => {
      const tripId = row["trip_id"];
      if (!tripId || !keptTrips.has(tripId)) return;
      const routeId = row["route_id"];
      const serviceId = row["service_id"];
      if (!routeId || !serviceId) return;
      keptRoutes.add(routeId);
      keptServices.add(serviceId);
      tripCount++;
      writer.write({
        t: "trip",
        id: tripId,
        route: routeId,
        service: serviceId,
        headsign: row["trip_headsign"] ?? "",
      });
    });

    // 4. routes referenced by a kept trip
    await forEachCsvRow(zipPath, "routes.txt", (row) => {
      const routeId = row["route_id"];
      if (!routeId || !keptRoutes.has(routeId)) return;
      routeCount++;
      writer.write({
        t: "route",
        id: routeId,
        short: row["route_short_name"] ?? "",
        long: row["route_long_name"] ?? "",
        type: Number(row["route_type"] ?? "3") || 3,
      });
    });

    // 5. calendar for services referenced by a kept trip
    await forEachCsvRow(zipPath, "calendar.txt", (row) => {
      const serviceId = row["service_id"];
      if (!serviceId || !keptServices.has(serviceId)) return;
      const days = [
        row["monday"], row["tuesday"], row["wednesday"], row["thursday"],
        row["friday"], row["saturday"], row["sunday"],
      ].map((value) => (value === "1" ? "1" : "0")).join("");
      calendarCount++;
      writer.write({
        t: "cal",
        service: serviceId,
        days,
        start: Number(row["start_date"] ?? "0") || 0,
        end: Number(row["end_date"] ?? "99991231") || 99991231,
      });
    });

    await writer.close();

    const version = new Date().toISOString().slice(0, 10).replace(/-/g, "");
    const storagePath = `transit/${region.id}-${version}.ndjson.gz`;
    const bucket = admin.storage().bucket();
    await bucket.upload(bundlePath, {
      destination: storagePath,
      metadata: { contentType: "application/gzip", cacheControl: "public, max-age=3600" },
    });

    const bytes = fs.statSync(bundlePath).size;
    const bundleDoc = admin.firestore().collection("transit_bundles").doc(region.id);

    // Yesterday's bundle is dead weight in storage the moment this one is
    // published - nothing ever asks for an older version.
    const previousPath = (await bundleDoc.get()).data()?.storagePath as string | undefined;
    if (previousPath && previousPath !== storagePath) {
      await bucket.file(previousPath).delete({ ignoreNotFound: true });
    }

    await bundleDoc.set({
      storagePath,
      version,
      minLat: region.minLat,
      maxLat: region.maxLat,
      minLon: region.minLon,
      maxLon: region.maxLon,
      stopCount: keptStops.size,
      stopTimeCount,
      bytes,
      updatedAt: Date.now(),
    });

    return {
      region: region.id,
      version,
      storagePath,
      stops: keptStops.size,
      routes: routeCount,
      trips: tripCount,
      stopTimes: stopTimeCount,
      calendar: calendarCount,
      bytes,
    };
  } finally {
    fs.rmSync(workDir, { recursive: true, force: true });
  }
}
