/* Deterministic renderer: every frame is seek(t) plus a screenshot.
     node render.js stills 5 12.5 ...        out/stills/t-XX.XX.png
     node render.js frames [part parts]      out/frames/f_00001.png ...  (30 fps, 1080x1920)
   Playwright and Chromium are the ones already on the machine (NODE_PATH points at the global modules). */
const path = require('path');
const fs = require('fs');
const { chromium } = require('playwright');

const ROOT = __dirname;
const FPS = 30, DUR = 60;
const mode = process.argv[2] || 'stills';

async function open() {
  const browser = await chromium.launch({ args: ['--font-render-hinting=none', '--disable-lcd-text', '--force-color-profile=srgb'] });
  const page = await browser.newPage({ viewport: { width: 1080, height: 1920 }, deviceScaleFactor: 1 });
  page.on('pageerror', (e) => console.error('PAGE ERROR', e.message));
  page.on('console', (m) => { if (m.type() === 'error' || m.type() === 'warning') console.error('console', m.text()); });
  await page.goto('file://' + path.join(ROOT, 'index.html'));
  await page.evaluate(() => window.READY);
  return { browser, page };
}
async function shot(page, t, file, tf = t) {
  // t is what the frame shows (key presses, typing, captions appearing); tf is the sub-frame time, used only by smooth motion
  await page.evaluate(([t, tf]) => { window.seek(t, tf); return Promise.all([...document.images].map((i) => i.decode().catch(() => 0))); }, [t, tf]);
  await page.screenshot({ path: file, type: 'png' });
}

(async () => {
  const { browser, page } = await open();
  if (mode === 'stills') {
    const dir = path.join(ROOT, 'out', 'stills'); fs.mkdirSync(dir, { recursive: true });
    for (const a of process.argv.slice(3)) { const t = parseFloat(a); await shot(page, t, path.join(dir, `t-${t.toFixed(3).padStart(6, '0')}.png`)); console.log('still', t); }
  } else if (mode === 'frames') {
    // 30 fps with motion blur: SUB sub-frames per output frame, spread over a 180 degree shutter (1/60 s) and averaged later by ffmpeg
    const part = parseInt(process.argv[3] || '0', 10), parts = parseInt(process.argv[4] || '1', 10);
    const SUB = parseInt(process.env.SUB || '6', 10);
    const from = parseFloat(process.env.T0 || '0'), to = parseFloat(process.env.T1 || String(DUR));
    const dir = process.env.FRAMES || path.join(ROOT, 'out', 'frames'); fs.mkdirSync(dir, { recursive: true });
    const n0 = Math.round(from * FPS), n1 = Math.round(to * FPS);
    const t0 = Date.now();
    for (let n = n0 + part; n < n1; n += parts) {
      for (let k = 0; k < SUB; k++) {
        const t = Math.min(DUR - 1e-3, n / FPS + 1 / 60);                       // the middle of the frame
        const tf = Math.max(0, Math.min(DUR - 1e-3, t + (SUB === 1 ? 0 : (k - (SUB - 1) / 2) / SUB / 60)));
        await shot(page, t, path.join(dir, `s_${String(n * SUB + k).padStart(6, '0')}.png`), tf);
      }
      if ((n - n0) % (parts * 60) === part) console.log(`part ${part}: frame ${n} of ${n1} (${((Date.now() - t0) / 1000).toFixed(0)} s)`);
    }
  }
  await browser.close();
})();
