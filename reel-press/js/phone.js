/* The phone in world units: 1 unit = 1 screen pixel (640x960 at the origin). Regular model: 62 x 156 mm, screen 49.3 x 74 mm,
   so 12.98 px per mm. Key positions come from hardware/keyboard_prototype/SPEC.md (17 mm column pitch, 9.5 mm row pitch). */
const MM = 640 / 49.3;
const BODY_BOTTOM = 1895;
const kx = (xmm) => 320 + (xmm - 28) * MM;
const ky = (ymm) => BODY_BOTTOM - (ymm + 5) * MM;

const LEGENDS = { d1: '', d2: 'אבג ABC', d3: 'דהו DEF', d4: 'זחט GHI', d5: 'יכל JKL', d6: 'מנס MNO', d7: 'עפצ PQRS', d8: 'קרש TUV', d9: 'ת WXYZ', star: '', d0: '+', pound: '' };
const DIGIT_LABEL = { star: '*', pound: '#' };
const PHONE_KEYS = [];
[['d1', 11, 35.25], ['d2', 28, 35.25], ['d3', 45, 35.25], ['d4', 11, 25.75], ['d5', 28, 25.75], ['d6', 45, 25.75],
 ['d7', 11, 16.25], ['d8', 28, 16.25], ['d9', 45, 16.25], ['star', 11, 6.75], ['d0', 28, 6.75], ['pound', 45, 6.75]]
  .forEach(([id, x, y]) => PHONE_KEYS.push({ id, x: kx(x), y: ky(y), w: 196, h: 100, type: 'digit' }));
PHONE_KEYS.push({ id: 'soft_l', x: kx(11), y: ky(55), w: 176, h: 72, type: 'icon', icon: 'dehaze' });
PHONE_KEYS.push({ id: 'soft_r', x: kx(45), y: ky(55), w: 176, h: 72, type: 'icon', icon: 'arrow_back' });
PHONE_KEYS.push({ id: 'call', x: kx(11), y: ky(45), w: 176, h: 84, type: 'icon', icon: 'call', color: 'var(--fos-success)' });
PHONE_KEYS.push({ id: 'end', x: kx(45), y: ky(45), w: 176, h: 84, type: 'icon', icon: 'call_end', color: 'var(--fos-danger)' });
const DPAD = { x: kx(28), y: ky(50), R: 128, r: 50 };

const pressTimes = {};
KEYS.forEach((k) => (pressTimes[k.k] = pressTimes[k.k] || []).push(k.t));
function pressOf(id, t) {
  const a = pressTimes[id]; if (!a) return 0;
  let p = 0;
  for (let i = 0; i < a.length; i++) { if (a[i] > t) break; p = Math.max(p, pressAmt(t, a[i])); }
  return p;
}

function wedgePath(cx, cy, a0, a1, r0, r1) {
  const P = (a, r) => [cx + r * Math.cos(a), cy + r * Math.sin(a)].map((v) => v.toFixed(2));
  const [x0, y0] = P(a0, r1), [x1, y1] = P(a1, r1), [x2, y2] = P(a1, r0), [x3, y3] = P(a0, r0);
  return `M${x0} ${y0}A${r1} ${r1} 0 0 1 ${x1} ${y1}L${x2} ${y2}A${r0} ${r0} 0 0 0 ${x3} ${y3}Z`;
}
/* D-pad as SVG markup; pr = {up,down,left,right,ok} in 0..1. Centered on (0,0) in a viewBox of +-(R+8). */
function dpadSVG(pr, size) {
  const R = DPAD.R, r = DPAD.r, B = R + 8, D = Math.PI / 4;
  const dirs = [['up', -Math.PI / 2, 'keyboard_arrow_up'], ['right', 0, 'keyboard_arrow_right'], ['down', Math.PI / 2, 'keyboard_arrow_down'], ['left', Math.PI, 'keyboard_arrow_left']];
  let s = `<svg width="${size}" height="${size}" viewBox="${-B} ${-B} ${2 * B} ${2 * B}" style="display:block">`;
  s += `<circle r="${R}" fill="#2C2C2E"/>`;
  for (const [id, a] of dirs) {
    const p = pr[id] || 0;
    s += `<path d="${wedgePath(0, 0, a - D + 0.02, a + D - 0.02, r + 3, R)}" fill="var(--fos-accent)" opacity="${p}"/>`;
  }
  for (let i = 0; i < 4; i++) { const a = D + i * Math.PI / 2; s += `<line x1="${(r * Math.cos(a)).toFixed(1)}" y1="${(r * Math.sin(a)).toFixed(1)}" x2="${(R * Math.cos(a)).toFixed(1)}" y2="${(R * Math.sin(a)).toFixed(1)}" stroke="#0C0C0E" stroke-width="7"/>`; }
  for (const [id, a, icon] of dirs) {
    const p = pr[id] || 0, d = (R + r) / 2 + 2;
    const col = `color-mix(in srgb, #000 ${p * 100}%, rgba(255,255,255,0.6))`;
    s += `<g transform="translate(${(d * Math.cos(a) - 20).toFixed(1)} ${(d * Math.sin(a) - 20).toFixed(1)})" style="color:${col}">${ic(icon, 40, col, 1, 0, 2.2).replace('<svg ', '<svg x="0" y="0" ')}</g>`;
  }
  const po = pr.ok || 0;
  s += `<circle r="${r}" fill="#3A3A3C"/><circle r="${r}" fill="var(--fos-accent)" opacity="${po}"/>`;
  s += `<text y="10" text-anchor="middle" font-family="Heebo" font-weight="700" font-size="30" fill="color-mix(in srgb, #000 ${po * 100}%, #fff)">OK</text>`;
  return s + '</svg>';
}

function buildPhone() {
  const w = document.getElementById('world');
  let h = `<div id="body"></div><div id="bezel"></div>`;
  h += `<div class="abs" style="left:250px;top:-58px;width:140px;height:12px;border-radius:6px;background:#1C1C1E"></div>`;
  for (const k of PHONE_KEYS) {
    const inner = k.type === 'digit'
      ? `<div class="kp"></div><div class="kd">${DIGIT_LABEL[k.id] || k.id.slice(1)}</div>${LEGENDS[k.id] ? `<div class="kl">${LEGENDS[k.id]}</div>` : ''}`
      : `<div class="kp"></div><div class="ki" style="color:${k.color || 'rgba(255,255,255,0.7)'}">${ic(k.icon, 40, 'currentColor', 1, 0, 2)}</div>`;
    h += `<div class="key" id="k-${k.id}" style="left:${k.x - k.w / 2}px;top:${k.y - k.h / 2}px;width:${k.w}px;height:${k.h}px;border-radius:${k.type === 'digit' ? 30 : 34}px">${inner}</div>`;
  }
  h += `<div id="dpad" class="abs" style="left:${DPAD.x - DPAD.R - 8}px;top:${DPAD.y - DPAD.R - 8}px"></div>`;
  h += `<div id="screen"></div>`;
  w.innerHTML = h;
  window.$keys = PHONE_KEYS.map((k) => document.getElementById('k-' + k.id));
  window.$dpad = document.getElementById('dpad');
  window.$screen = document.getElementById('screen');
}

function updatePhone(t) {
  PHONE_KEYS.forEach((k, i) => { const p = pressOf(k.id, t); $keys[i].style.setProperty('--p', p.toFixed(3)); if (k.type === 'icon') $keys[i].lastChild.style.color = `color-mix(in srgb, #000 ${(p * 100).toFixed(0)}%, ${k.color || 'rgba(255,255,255,0.7)'})`; });
  $dpad.innerHTML = dpadSVG({ up: pressOf('up', t), down: pressOf('down', t), left: pressOf('left', t), right: pressOf('right', t), ok: pressOf('ok', t) }, 2 * (DPAD.R + 8));
}
