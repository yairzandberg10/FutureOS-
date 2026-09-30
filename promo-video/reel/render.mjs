// Renders the FutureOS reel (1080x1920, 30 fps) with the Remotion Node API.
//
//   node reel/render.mjs still 100 300 500 [--guides]   # PNG stills -> out/stills
//   node reel/render.mjs video                          # MP4 -> exports/futureos-reel-1080x1920.mp4
//
// BROWSER=/path/to/chrome-headless-shell overrides the browser (needed where Remotion cannot download its own).
import { bundle } from "@remotion/bundler";
import { renderMedia, renderStill, selectComposition } from "@remotion/renderer";
import { existsSync, mkdirSync, readdirSync } from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");
const [mode, ...rest] = process.argv.slice(2);
const guides = rest.includes("--guides");
const frames = rest.filter((a) => /^\d+$/.test(a)).map(Number);

const findShell = () => {
  if (process.env.BROWSER) return process.env.BROWSER;
  const base = "/opt/pw-browsers";
  if (!existsSync(base)) return undefined;
  const dir = readdirSync(base).find((d) => d.startsWith("chromium_headless_shell"));
  const bin = dir && path.join(base, dir, "chrome-linux", "headless_shell");
  return bin && existsSync(bin) ? bin : undefined;
};
const browserExecutable = findShell();

const serveUrl = await bundle({ entryPoint: path.join(root, "src/reel/index.ts") });
const inputProps = { showGuides: guides };
const composition = await selectComposition({ serveUrl, id: "FutureOSReel", inputProps, browserExecutable });

if (mode === "still") {
  const dir = path.join(root, "out/stills");
  mkdirSync(dir, { recursive: true });
  for (const frame of frames) {
    const output = path.join(dir, `f${String(frame).padStart(4, "0")}${guides ? "-guides" : ""}.png`);
    await renderStill({ composition, serveUrl, frame, output, inputProps, browserExecutable, imageFormat: "png" });
    console.log("still", output);
  }
} else if (mode === "video") {
  mkdirSync(path.join(root, "exports"), { recursive: true });
  const outputLocation = path.join(root, "exports/futureos-reel-1080x1920.mp4");
  let last = -1;
  await renderMedia({
    composition,
    serveUrl,
    inputProps,
    browserExecutable,
    codec: "h264",
    crf: 16,
    imageFormat: "png",
    pixelFormat: "yuv420p",
    colorSpace: "bt709",
    audioCodec: "aac",
    audioBitrate: "192k",
    x264Preset: "slow",
    concurrency: 4,
    outputLocation,
    onProgress: ({ progress }) => {
      const pct = Math.floor(progress * 10);
      if (pct !== last) {
        last = pct;
        console.log(`render ${pct * 10}%`);
      }
    },
  });
  console.log("video", outputLocation);
} else {
  console.error("usage: node reel/render.mjs still <frames…> [--guides] | video");
  process.exit(1);
}
