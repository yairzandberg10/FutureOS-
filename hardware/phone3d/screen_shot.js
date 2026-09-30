/* Saves the real FutureOS screen (640x960 device pixels, drawn by the reel's page from the design-system components) as a PNG at 2x,
   to be used as the emissive texture of the display area in the 3D render.
     node screen_shot.js [t] [out.png]        t defaults to 55 (the launcher, white accent) */
const path = require('path');
const { chromium } = require('playwright');

(async () => {
  const t = parseFloat(process.argv[2] || '55');
  const out = process.argv[3] || path.join(__dirname, 'screen.png');
  const browser = await chromium.launch({ args: ['--font-render-hinting=none', '--force-color-profile=srgb'] });
  const page = await browser.newPage({ viewport: { width: 1080, height: 1920 }, deviceScaleFactor: 2 });
  await page.goto('file://' + path.join(__dirname, '..', '..', 'reel-press', 'index.html'));
  await page.evaluate(() => window.READY);
  // the screen alone: no camera, no phone body around it, 1:1 in its own 640x960 box
  await page.evaluate((t) => {
    window.seek(t);
    const s = document.getElementById('screen');
    document.getElementById('world').style.transform = 'translate(20px,20px) scale(1)';
    document.getElementById('cap').innerHTML = ''; document.getElementById('fx').innerHTML = '';
    s.style.transform = 'none'; s.style.clipPath = 'none';
    document.getElementById('phone3d').style.visibility = 'hidden';
    s.querySelectorAll('.punch').forEach((e) => e.remove());
    return Promise.all([...document.images].map((i) => i.decode().catch(() => 0)));
  }, t);
  await page.locator('#screen').screenshot({ path: out });
  await browser.close();
  console.log('saved', out);
})();
