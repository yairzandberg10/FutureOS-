/* Small toolkit: easing, key/focus bookkeeping and the design-system components as HTML builders.
   Every builder mirrors a component in design/FutureOS Design System/components (same tokens, same measurements) and takes
   a focus/state amount 0..1 instead of a boolean, so a focus move can be drawn as the 200 ms FastOutSlowIn tween the
   system uses. Nothing here keeps state: the page is a pure function of time. */
const W = 1080, H = 1920, FPS = 30;
const clamp = (x, a = 0, b = 1) => Math.min(b, Math.max(a, x));
const lerp = (a, b, t) => a + (b - a) * t;
const inv = (a, b, x) => clamp((x - a) / (b - a));

function bezier(x1, y1, x2, y2) {
  const cx = 3 * x1, bx = 3 * (x2 - x1) - cx, ax = 1 - cx - bx;
  const cy = 3 * y1, by = 3 * (y2 - y1) - cy, ay = 1 - cy - by;
  const X = (t) => ((ax * t + bx) * t + cx) * t, Y = (t) => ((ay * t + by) * t + cy) * t;
  return (x) => {
    if (x <= 0) return 0; if (x >= 1) return 1;
    let t = x;
    for (let i = 0; i < 8; i++) { const e = X(t) - x; const d = (3 * ax * t + 2 * bx) * t + cx; if (Math.abs(e) < 1e-5 || !d) break; t -= e / d; }
    return Y(clamp(t));
  };
}
const std = bezier(0.4, 0, 0.2, 1);       // FastOutSlowIn: the system default
const enter = bezier(0, 0, 0.2, 1);       // LinearOutSlowIn
const out3 = (t) => 1 - Math.pow(1 - clamp(t), 3);
const out4 = (t) => 1 - Math.pow(1 - clamp(t), 4);
const inout = (t) => { t = clamp(t); return t < 0.5 ? 4 * t * t * t : 1 - Math.pow(-2 * t + 2, 3) / 2; };
const hash = (n) => { let x = Math.sin(n * 127.1 + 311.7) * 43758.5453; return x - Math.floor(x); };

/* ---- keys: everything on screen that reacts to a key reads TL.keys */
const KEYS = TL.keys;
const keysIn = (a, b, re) => KEYS.filter((k) => k.t >= a && k.t < b && (!re || re.test(k.k)));
const countKeys = (t, a, re) => keysIn(a, t + 1e-6, re).length;
const lastKey = (t, a, re) => { const l = keysIn(a, t + 1e-6, re); return l.length ? l[l.length - 1] : null; };
/* how far a key is pressed at time t: 0..1..0 over ~0.2 s. */
function pressAmt(t, t0) {
  const d = t - t0; if (d < 0 || d > 0.24) return 0;
  return d < 0.035 ? d / 0.035 : d < 0.11 ? 1 : 1 - (d - 0.11) / 0.13;
}
/* the tween of a focus change: 0 -> 1 over 200 ms from t0 */
const tween = (t, t0, dur = 0.2) => std(clamp((t - t0) / dur));
/* a value that steps at given times and eases between: steps = [[t, value], ...] */
function stepTween(t, steps, dur = 0.2) {
  let cur = steps[0][1], prev = cur, t0 = -1e9;
  for (const [ts, v] of steps) { if (t >= ts) { prev = cur; cur = v; t0 = ts; } }
  return lerp(prev, cur, tween(t, t0, dur));
}
/* focus amount of item i given the list of focus changes [[t, index], ...] */
function focusAmt(t, changes, i, dur = 0.2) {
  let cur = changes[0][1], prev = -1, t0 = -1e9;
  for (const [ts, idx] of changes) { if (t >= ts) { prev = cur; cur = idx; t0 = ts; } }
  const p = tween(t, t0, dur);
  if (cur === i) return t0 < -1e8 ? 1 : (prev === i ? 1 : p);
  if (prev === i) return 1 - p;
  return 0;
}

/* ---- icons (FosIcon paths) */
const ICONS = window.FOS_ICONS || {};
function ic(name, size = 24, color = 'currentColor', opacity = 1, fill = 0, sw = 1.6) {
  const spec = ICONS[name]; if (!spec) return '';
  const solid = fill === 1;
  const parts = spec.split('|').map((p) => {
    if (p.startsWith('d:')) { const [cx, cy] = p.slice(2).split(' '); return `<circle cx="${cx}" cy="${cy}" r="1.2" fill="${color}" stroke="none"/>`; }
    return `<path d="${p}" fill="${solid && p.trim().endsWith('Z') ? color : 'none'}"/>`;
  }).join('');
  return `<svg viewBox="0 0 24 24" width="${size}" height="${size}" fill="none" stroke="${color}" stroke-width="${sw}" stroke-linecap="round" stroke-linejoin="round" style="opacity:${opacity};display:block;flex:0 0 auto">${parts}</svg>`;
}

/* ---- components. `f` is the focus amount 0..1 */
const cm = (c, pct) => `color-mix(in srgb, ${c} ${pct}%, transparent)`;

const TopBar = ({ title, back, menu, bf = 0, mf = 0 }) => `
  <div class="topbar">
    ${back ? `<div class="ibtn" style="--f:${bf}">${ic('arrow_forward', 36, 'var(--fos-text)')}</div>` : ''}
    <div class="tb-title">${title}</div>
    ${menu ? `<div class="ibtn" style="--f:${mf}">${ic('more_vert', 36, 'var(--fos-text)')}</div>` : ''}
  </div>`;

const ListItem = ({ title, summary, trailing = '', f = 0, style = '' }) => `
  <div class="li" style="--f:${f};${style}">
    <div class="li-t"><div class="li-title">${title}</div>${summary ? `<div class="li-sum">${summary}</div>` : ''}</div>${trailing}
  </div>`;

const Card = (inner, style = '') => `<div class="card" style="${style}">${inner}</div>`;
const Divider = () => `<div class="divider"></div>`;
const SectionHeader = (txt) => `<div class="sechead">${txt}</div>`;

const SettingItem = ({ title, summary, icon, trailing, chevron = true, f = 0, iconColor = 'var(--fos-accent)', style = '', pos = 'middle' }) => `
  <div class="si ${pos}" style="--f:${f};${style}">
    ${icon ? ic(icon, 44, iconColor) : ''}
    <div class="si-t"><div class="si-title">${title}</div>${summary ? `<div class="si-sum">${summary}</div>` : ''}</div>
    ${trailing || (chevron ? ic('chevron_left', 36, 'var(--fos-text)', 0.3) : '')}
  </div>`;

const Switch = (p) => {
  const size = lerp(28, 36, p), start = lerp(8, 56, p);
  return `<div class="sw" style="background:${cm('var(--fos-accent)', 100 * p)};border-color:color-mix(in srgb, var(--fos-accent) ${100 * p}%, var(--fos-text-30))">
    <div class="sw-th" style="width:${size}px;height:${size}px;margin-top:${-size / 2}px;right:${start}px;background:color-mix(in srgb, var(--fos-bg) ${100 * p}%, var(--fos-text-40))"></div></div>`;
};

const Slider = ({ label, value, f = 0 }) => `
  <div class="slider" style="--f:${f}">
    <div class="sl-l">${label}</div>
    <div class="sl-track"><div class="sl-fill" style="width:${clamp(value) * 100}%"></div></div>
  </div>`;

const Badge = (count, bg) => `<div class="badge" ${bg ? `style="background:${bg}"` : ''}>${count}</div>`;
const ProgressBar = (v, mini = false, style = '') => `<div class="pbar${mini ? ' mini' : ''}" style="${style}"><div style="width:${clamp(v) * 100}%"></div></div>`;

const Button = ({ label, variant = 'primary', f = 0, full = true, style = '' }) => `
  <div class="btn ${variant}" style="--f:${f};${full ? 'width:100%;' : ''}${style}">${label}</div>`;

const Chip = (label, state = 'idle') => `<div class="chip ${state}">${label}</div>`;

const EmptyState = ({ icon, title, sub }) => `
  <div class="empty">${ic(icon, 112, 'var(--fos-text)', 0.4, 0, 1.1)}<div class="e-t">${title}</div>${sub ? `<div class="e-s">${sub}</div>` : ''}</div>`;

const Avatar = (name, size = 88, icon) => `
  <div class="avatar" style="width:${size}px;height:${size}px">${icon ? ic(icon, Math.round(size * 0.44), 'var(--fos-text)') : `<span style="font-size:${Math.round(size * 0.3)}px">${name.trim().split(/\s+/).slice(0, 2).map((w) => w[0]).join('')}</span>`}</div>`;

const SoftKeyBar = (l, c, r) => `<div class="softbar"><div class="sb-side" style="text-align:right">${l || ''}</div><div class="sb-c">${c || ''}</div><div class="sb-side" style="text-align:left">${r || ''}</div></div>`;

const StatusBar = (time = '08:30') => `
  <div class="statusbar"><span class="sb-time">${time}</span><span class="sb-ic">${ic('signal_cellular_alt', 26, 'var(--fos-text)', 0.9)}${ic('wifi', 26, 'var(--fos-text)', 0.9)}${ic('battery_full', 30, 'var(--fos-text)', 0.9)}</span></div>`;

const ConfirmDialog = ({ message, confirm = 'מחק', cancel = 'ביטול', focus = 'cancel', ft = 1, scrim = 1, scale = 1 }) => {
  const fc = focus === 'confirm' ? ft : (1 - ft) * (focus === 'confirm' ? 1 : 0);
  return `
  <div class="scrim" style="background:rgba(0,0,0,${0.6 * scrim})">
    <div class="dialog" style="transform:scale(${1.02 * scale})">
      <div class="d-msg">${message}</div>
      <div class="d-btns">
        ${Button({ label: cancel, variant: 'secondary', f: focus === 'cancel' ? ft : 0, full: false })}
        ${Button({ label: confirm, variant: 'destructive', f: focus === 'confirm' ? ft : 0, full: false })}
      </div>
    </div>
  </div>`;
};

const HeadsUp = ({ app, title, body, icon = 'chat' }) => `
  <div class="hu"><div class="hu-i">${ic(icon, 36, '#FFFFFF')}</div>
    <div class="hu-t"><div class="hu-app">${app}</div><div class="hu-title">${title}</div>${body ? `<div class="hu-body">${body}</div>` : ''}</div></div>`;
