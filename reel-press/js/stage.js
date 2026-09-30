/* The frame: camera, captions, the opening noise and the dialog, and seek(t) which draws everything for one instant. */
const FULL = { s: 0.62, ox: 341.6, oy: 640 };
const SCREEN = { s: 1.02, ox: 213.6, oy: 592 };
const lerpCam = (a, b, p) => ({ s: lerp(a.s, b.s, p), ox: lerp(a.ox, b.ox, p), oy: lerp(a.oy, b.oy, p) });
const PUNCH = [12, 14, 16, 18, 20, 22, 24, 26, 28, 30, 32, 34, 36, 40, 44, 45, 46, 47, 48, 56];

function camAt(t) {
  let c;
  if (t < 6.0) c = { s: FULL.s, ox: FULL.ox, oy: 2400 };
  else if (t < 7.4) c = { s: FULL.s, ox: FULL.ox, oy: lerp(2000, FULL.oy, out3((t - 6.0) / 1.4)) };
  else if (t < 8.75) c = FULL;
  else if (t < 9.4) c = lerpCam(FULL, SCREEN, inout((t - 8.75) / 0.65));
  else if (t < 52.0) c = SCREEN;
  else if (t < 53.0) c = lerpCam(SCREEN, FULL, inout((t - 52.0) / 1.0));
  else c = FULL;
  // a small punch on every scene start: the phone answers the OK key
  let k = 0;
  for (const p of PUNCH) if (t >= p) k = Math.max(k, Math.exp(-(t - p) * 14));
  const drop = (t >= 12 && t < 36) || (t >= 44 && t < 54);
  const s = c.s * (1 + 0.018 * k + (drop ? 0.006 * beatEnv(t) : 0));
  return { s, ox: c.ox + (c.s - s) * 320, oy: c.oy + (c.s - s) * 480 };
}

/* ------------------------------------------------------------ captions */
const TOUR = [
  ['keys', 12, 'חיוג. כל ספרה, מקש אחד.', 12.2, 13.5],
  ['keys', 14, 'הקלדה. T9 בעברית, כמו שהכרת.', 14.1, 15.8],
  ['pill', 16, 'בלוטוס', 'מתחברים בלחיצה.'], ['pill', 18, 'תרגום', 'במכשיר עצמו. גם בלי אינטרנט.'],
  ['pill', 20, 'שעון', 'מעורר, טיימר וסטופר.'], ['pill', 22, 'לוח שנה', 'לוח עברי, דף יומי וזמני היום.'],
  ['pill', 24, 'מוזיקה', 'הפס קול של הדרך.'], ['pill', 26, 'כושר', 'דופק בזמן אמת, מהחיישן.'],
  ['pill', 28, 'טרמינל', 'כן, אמיתי. כן, עם root.'], ['pill', 30, 'ספרים', '6,211 ספרים. גם בלי אינטרנט.'],
  ['pill', 32, 'שלט', 'מזגן, מאוורר, מערכת שמע.'], ['pill', 34, 'עוזר קולי', 'לחיצה כפולה על OK.'],
];
const CAPS = [
  { a: -0.4, b: 2.75, kind: 'plain', top: 270, size: 92, lines: ['כמה פעמים נגעת', 'בטלפון היום?'], stagger: 0.04 },
  { a: 5.0, b: 6.0, kind: 'plain', top: 610, size: 200, lines: ['בלי מסך', 'מגע.'], dim: (t) => (t >= 5.5 ? 0.3 : 1) },
  { a: 5.5, b: 6.0, kind: 'plain', top: 1130, size: 124, lines: ['רק מקשים.'], accent: true },
  { a: 6.5, b: 8.85, kind: 'pill', top: 268, text: 'FutureOS', ring: 6.5, fill: 7.0, sub: 'טלפון שחוזר להיות כלי.', subAt: 7.3, out: 8.55 },
  { a: 9.5, b: 12.0, kind: 'plain', top: 258, size: 112, lines: ['מעל 25 אפליקציות.'], sub: 'כל אחת בנויה למקשים.' },
  ...TOUR.map(([kind, a, x, y, z]) => (kind === 'keys'
    ? { a, b: a + 2, kind, top: 262, sub: x, win: [y, z] }
    : { a, b: a + 2, kind, top: 268, text: x, ring: a, fill: a + 0.05, sub: y })),
  { a: 36.0, b: 40.0, kind: 'plain', top: 258, size: 132, lines: ['בצבע שלך.'], sub: 'צבע אחד. אתה בוחר.', accentWord: 'שלך.' },
  { a: 40.0, b: 41.5, kind: 'plain', top: 258, size: 150, lines: ['כהה.'], sub: 'מצב כהה, כברירת מחדל.' },
  { a: 41.5, b: 43.0, kind: 'plain', top: 258, size: 150, lines: ['או בהיר.'], sub: 'אותה מערכת. אותם מקשים.' },
  { a: 43.0, b: 44.0, kind: 'plain', top: 258, size: 150, lines: ['קריא.'], sub: 'תמיד.' },
  { a: 44.0, b: 45.0, kind: 'plain', top: 258, size: 150, lines: ['בלי נגיעות.'] },
  { a: 45.0, b: 46.0, kind: 'plain', top: 258, size: 150, lines: ['בלי גלילה.'] },
  { a: 46.0, b: 47.0, kind: 'plain', top: 258, size: 150, lines: ['בלי רעש.'] },
  { a: 47.0, b: 48.0, kind: 'plain', top: 258, size: 150, lines: ['רק מקשים.'], accent: true },
  { a: 48.0, b: 52.0, kind: 'plain', top: 258, size: 128, lines: ['כל אפליקציה.'], sub: 'מקש אחד.' },
  { a: 52.0, b: 54.0, kind: 'plain', top: 258, size: 128, lines: ['אתה. והמקשים.'] },
  { a: 54.0, b: 60.01, kind: 'pill', top: 268, text: 'FutureOS', ring: 54.0, fill: 56.0, sub: 'טלפון שחוזר להיות כלי.', subAt: 56.3, sub2: 'הלינק בביו.', sub2At: 57.6 },
];

const wordSpans = (line, t, a, size, stagger = 0.06, colorFn) => line.split(' ').map((w, i) => {
  const p = out3((t - a - i * stagger) / 0.26);
  const c = colorFn ? colorFn(w) : '';
  return `<span style="display:inline-block;opacity:${p};transform:translateY(${(1 - p) * 34}px) scale(${1 + (1 - p) * 0.1});${c}">${w}</span>`;
}).join(' ');

function captionHTML(t, tf) {
  let out = '';
  for (const c of CAPS) {
    if (t < c.a || t >= c.b) continue;
    const dim = c.dim ? c.dim(t) : 1;
    let h = '';
    if (c.kind === 'plain') {
      const cf = (w) => (c.accent || (c.accentWord && w === c.accentWord)) ? 'color:var(--fos-accent);' : '';
      const lines = c.lines.map((l, i) => `<div class="big" style="font-size:${c.size}px;line-height:1.02">${wordSpans(l, tf, c.a + i * 0.08, c.size, c.stagger || 0.06, cf)}</div>`).join('');
      const sub = c.sub ? `<div class="sub" style="opacity:${out3((tf - c.a - 0.18) / 0.25)}">${c.sub}</div>` : '';
      h = lines + sub;
    } else if (c.kind === 'pill') {
      const pe = out3((tf - c.a) / 0.2);
      const sel = inv(c.fill, c.fill + 0.14, tf);
      const press = t >= 55.95 ? pressOf('ok', t) : 0;
      const fade = c.out ? 1 - inv(c.out, c.out + 0.4, tf) : 1;
      const sc = (0.9 + 0.1 * pe) * (1 - 0.02 * press);
      h = `<div style="opacity:${pe * fade};transform:translateY(${(1 - fade) * -70}px) scale(${sc})"><span class="pill" style="background:color-mix(in srgb, var(--fos-accent) ${sel * 100}%, transparent);color:color-mix(in srgb, #000 ${sel * 100}%, #fff)">${c.text}</span></div>`;
      if (c.sub) h += `<div class="sub" style="opacity:${out3((tf - (c.subAt || c.a + 0.2)) / 0.25) * fade};margin-top:26px">${c.sub}</div>`;
      if (c.sub2) h += `<div class="sub" style="opacity:${out3((tf - c.sub2At) / 0.3)};margin-top:14px;color:var(--fos-accent);font-weight:700">${c.sub2}</div>`;
    } else if (c.kind === 'keys') {
      const typed = keysIn(c.win[0] - 0.05, c.win[1] + 0.05, /^(d\d|star|pound)$/).filter((k) => k.t <= t);
      let seq = typed;
      if (c.a === 14) { seq = typed.filter((k) => { const w = TL.t9.find((x) => k.t >= x.t0 - 0.01 && k.t < x.commit); return w && t < w.commit; }); }
      const caps = seq.map((k, i) => `<span class="kc${i === seq.length - 1 && t - k.t < 0.3 ? ' on' : ''}" style="transform:scale(${1 + 0.16 * Math.exp(-(t - k.t) * 16)})">${k.k === 'star' ? '*' : k.k === 'pound' ? '#' : k.k.slice(1)}</span>`).join('');
      h = `<div style="height:118px;display:flex;justify-content:center;align-items:center;direction:ltr">${caps}</div><div class="sub" style="margin-top:20px;opacity:${out3((t - c.a) / 0.2)}">${c.sub}</div>`;
    }
    out += `<div style="position:absolute;left:0;top:${c.top}px;width:1080px;opacity:${dim}">${h}</div>`;
  }
  return out;
}

/* ------------------------------------------------------------ 0 - 3: the noise */
const NOISE = [
  ['הודעות', 'chat', '12 הודעות חדשות', 'נראה אותך בקבוצה?'], ['חדשות', 'notifications', 'מבזק', 'עוד עדכון שלא ביקשת'],
  ['רשת חברתית', 'forum', 'מישהו הגיב', 'על התמונה שלך'], ['משחק', 'sports_esports', 'האנרגיה מלאה', 'חזור לשחק'],
  ['קניות', 'storefront', 'המבצע נגמר', 'בעוד שעה'], ['עדכון', 'download', 'יש גרסה חדשה', 'שוב'],
  ['דואר', 'chat', '47 הודעות', 'שלא נקראו'], ['אפליקציה', 'notifications', 'התגעגענו אליך', 'חזור אלינו'],
  ['צעדים', 'directions_walk', 'עוד 400 צעדים', 'ליעד היומי'], ['שיחות', 'call_missed', '5 שיחות', 'שלא נענו'],
  ['הודעות', 'chat', 'עוד הודעה', 'ועוד אחת'], ['רשת חברתית', 'forum', 'סיפור חדש', 'שכחת לראות'],
  ['חדשות', 'notifications', 'מבזק', 'ושוב מבזק'], ['משחק', 'sports_esports', 'קיבלת מתנה', 'קחי אותה'],
  ['קניות', 'storefront', 'הסל שלך מחכה', 'רק עוד יום'], ['עדכון', 'download', 'עדכון זמין', 'שוב'],
  ['דואר', 'chat', '48 הודעות', 'שלא נקראו'], ['הודעות', 'chat', 'הודעה חדשה', 'עכשיו'],
  ['אפליקציה', 'notifications', 'אל תפספס', 'עכשיו'], ['משחק', 'sports_esports', 'עוד שלב', 'עכשיו'],
];
const PINGS = TL.sfx.filter((s) => s.s === 'ping').map((s) => s.t);
function noiseHTML(t, tf) {
  if (t >= 2.75) return '';
  const amp = 1 + 12 * inv(1.4, 2.7, t);
  const fr = Math.floor(t * FPS);
  const dx = (hash(fr) - 0.5) * 2 * amp, dy = (hash(fr + 99) - 0.5) * 2 * amp;
  let h = `<div class="abs" style="left:0;top:0;width:1080px;height:1920px;transform:translate(${dx}px,${dy}px)">`;
  PINGS.forEach((pt, i) => {
    if (t < pt) return;
    const age = tf - pt + (i === 0 ? 0.14 : 0), p = out3(age / 0.13);
    const x = 540 + (i % 2 ? 1 : -1) * (20 + hash(i) * 70);
    const y = 545 + i * 56 + (hash(i + 7) - 0.5) * 30;
    const r = (hash(i + 3) - 0.5) * 5;
    const [app, icon, title, body] = NOISE[i];
    h += `<div class="abs" style="left:${x - 300}px;top:${y}px;transform-origin:50% 50%;transform:translateY(${(1 - p) * -40}px) rotate(${r}deg) scale(${1.55 * (0.82 + 0.18 * p)});opacity:${clamp(age / 0.05)};z-index:${i}">
      ${HeadsUp({ app, title, body, icon })}</div>`;
  });
  // the unread counter
  if (t >= 0.9) {
    const n = Math.min(99, 7 + Math.round(92 * inv(0.9, 2.5, t) * inv(0.9, 2.5, t)));
    const p = out3((tf - 0.9) / 0.2);
    h += `<div class="abs" style="left:410px;top:1180px;width:260px;height:260px;border-radius:999px;background:var(--fos-danger);color:#000;display:flex;align-items:center;justify-content:center;font-size:128px;direction:ltr;font-weight:800;transform:scale(${p * (1 + 0.05 * Math.sin(t * 40))}) rotate(-6deg);z-index:99;font-family:var(--fos-font)">${n}${n >= 99 ? '+' : ''}</div>`;
  }
  return h + '</div>';
}

/* ------------------------------------------------------------ 3 - 5: the question */
function dialogHTML(t, tf) {
  if (t < 3.0 || t >= 5.05) return '';
  const inP = std(clamp((tf - 3.0) / 0.25)), outP = std(clamp((tf - 4.5) / 0.3));
  const op = inP * (1 - outP);
  const ft = tween(t, 4.0);
  const press = pressOf('ok', t);
  const dlg = ConfirmDialog({ message: 'למחוק את מסך המגע?', confirm: 'מחק', cancel: 'ביטול', focus: t >= 4.0 ? 'confirm' : 'cancel', ft: t >= 4.0 ? ft : 1, scrim: 0, scale: (0.9 + 0.1 * inP) * (1 - 0.04 * outP) * (1 - 0.01 * press) });
  const dp = dpadSVG({ left: pressOf('left', t), ok: pressOf('ok', t) }, 300);
  return `<div class="abs" style="left:0;top:0;width:1080px;height:1920px;opacity:${op}">
    <div class="abs" style="left:220px;top:-60px;width:640px;height:960px;transform:scale(1.6875);transform-origin:50% 0;direction:rtl">${dlg}</div>
    <div class="abs" style="left:390px;top:1290px">${dp}</div></div>`;
}

/* ------------------------------------------------------------ seek */
let _last = '';
function seek(t, tf = t) {
  document.documentElement.style.setProperty('--fos-accent', accentAt(t));
  const scr = $screen;
  const light = isLight(t);
  if (scr.classList.contains('fos-light') !== light) scr.classList.toggle('fos-light', light);
  const c = camAt(tf);
  document.getElementById('world').style.transform = `translate(${c.ox}px,${c.oy}px) scale(${c.s})`;
  const html = t >= 6.0 ? screenAt(t) : '';
  scr.innerHTML = html + scr.dataset.punch;
  updatePhone(t, tf, t >= 6.0);
  document.getElementById('cap').innerHTML = captionHTML(t, tf);
  document.getElementById('fx').innerHTML = noiseHTML(t, tf) + dialogHTML(t, tf);
  window.REPORT = { t };
}
window.seek = seek;
