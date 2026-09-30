/* The 640x960 screens, composed from the design-system components only (the way the system's own UI kits are).
   Each screen is a function of time: it reads the physical key presses in TL.keys and turns them into focus moves,
   toggles and typing, so what the phone shows is exactly what the keys did. */
const APPS = [['dialer', 'טלפון'], ['messages', 'הודעות'], ['contact', 'אנשי קשר'], ['calendar', 'לוח שנה'],
  ['clock', 'שעון'], ['camera', 'מצלמה'], ['gallery', 'גלריה'], ['music', 'מוזיקה'],
  ['navigation', 'ניווט'], ['settings', 'הגדרות'], ['notes', 'פתקים'], ['tasks', 'משימות'],
  ['files', 'קבצים'], ['calculator', 'מחשבון'], ['fitness', 'כושר'], ['sfarim', 'בלכתך בדרך']];

/* ---- focus on a grid or list, driven by the direction keys between a and b */
function navSim(t, a, b, start, cols, count, off = 0) {
  let i = start, prev = -1, t0 = -1e9;
  for (const k of keysIn(a, b, /^(left|right|up|down)$/)) {
    if (k.t > t) break;
    const col = (i + off) % cols; let n = i;
    if (k.k === 'left' && col < cols - 1 && i + 1 < count) n = i + 1;          // RTL: left is forward
    else if (k.k === 'right' && col > 0) n = i - 1;
    else if (k.k === 'down' && i + cols < count) n = i + cols;
    else if (k.k === 'up' && i - cols >= 0) n = i - cols;
    if (n !== i) { prev = i; i = n; t0 = k.t; }
  }
  return { i, prev, t0 };
}
const fa = (nav, idx, t) => nav.i === idx ? (nav.t0 < -1e8 ? 1 : tween(t, nav.t0)) : nav.prev === idx ? 1 - tween(t, nav.t0) : 0;
const okBetween = (t, a, b) => keysIn(a, b, /^ok$/).filter((k) => k.t <= t);
const hebNum = ['', 'א', 'ב', 'ג', 'ד', 'ה', 'ו', 'ז', 'ח', 'ט', 'י', 'יא', 'יב', 'יג', 'יד', 'טו', 'טז', 'יז', 'יח', 'יט', 'כ', 'כא', 'כב', 'כג', 'כד', 'כה', 'כו', 'כז', 'כח', 'כט', 'ל'];
const beatEnv = (t) => Math.exp(-((t % TL.BEAT) / TL.BEAT) * 5);   // 1 on every beat, decays

const center = (inner, extra = '') => `<div style="height:100%;display:flex;flex-direction:column;${extra}">${inner}</div>`;
const col = (gap) => `display:flex;flex-direction:column;gap:${gap}px`;

const SoftKeys = (options, ok, back) => `<div style="position:absolute;left:0;right:0;bottom:0">${SoftKeyBar(back, ok, options)}</div>`;

/* ============================================================ launcher */
function Launcher(t, a, b, start = 0, idle = false) {
  const nav = navSim(t, a, b, start, 4, 16);
  let cells = '';
  APPS.forEach(([id, name], i) => {
    const f = idle ? (i === nav.i ? 1 : 0) : fa(nav, i, t);
    cells += `<div class="li" style="--f:${f};flex-direction:column;justify-content:center;gap:12px;height:196px;padding:0;border-radius:32px">
      <img src="assets/icons/${id}.png" style="width:116px;height:116px;border-radius:30px;display:block"/>
      <div style="font-size:24px;font-weight:500;white-space:nowrap">${name}</div></div>`;
  });
  return `${StatusBar('08:30')}
    <div style="display:grid;grid-template-columns:repeat(4,1fr);gap:0 0;padding:24px 20px 0;direction:rtl">${cells}</div>
    <div style="position:absolute;left:0;right:0;bottom:36px;display:flex;justify-content:center;gap:16px">
      <div style="width:14px;height:14px;border-radius:9px;background:var(--fos-text)"></div>
      <div style="width:14px;height:14px;border-radius:9px;background:var(--fos-text-30)"></div>
      <div style="width:14px;height:14px;border-radius:9px;background:var(--fos-text-30)"></div></div>`;
}

/* ============================================================ 1  dialer + call */
const KEYPAD = [['1', ''], ['2', 'ABC'], ['3', 'DEF'], ['4', 'GHI'], ['5', 'JKL'], ['6', 'MNO'], ['7', 'PQRS'], ['8', 'TUV'], ['9', 'WXYZ'], ['*', ''], ['0', '+'], ['#', '']];
function Dialer(t) {
  const typed = keysIn(12.2, 13.5, /^d\d$/).filter((k) => k.t <= t);
  const digits = typed.map((k) => k.k.slice(1)).join('');
  const shown = digits.length > 3 ? digits.slice(0, 3) + '-' + digits.slice(3) : digits;
  const match = digits.length >= 10;
  const idmap = { '*': 'star', '#': 'pound' };
  const call = pressOf('call', t);
  const keys = KEYPAD.map(([d, s]) => {
    const p = pressOf(idmap[d] || 'd' + d, t);
    return `<div style="height:116px;border-radius:32px;background:color-mix(in srgb, var(--fos-calc-button-focused) ${p * 100}%, var(--fos-calc-button));${col(2)};align-items:center;justify-content:center;flex-direction:column">
      <span style="font-size:40px;font-weight:500">${d}</span>${s ? `<span style="font-size:20px;color:var(--fos-text-40);letter-spacing:1px">${s}</span>` : ''}</div>`;
  }).join('');
  return center(`${TopBar({ title: 'מקלדת' })}
    <div style="padding:0 32px 24px;min-height:150px;${col(6)};justify-content:center;align-items:center">
      <div style="font-size:68px;font-weight:300;letter-spacing:2px;direction:ltr;font-family:var(--fos-font-display);color:${digits ? 'var(--fos-text)' : 'var(--fos-text-30)'}">${shown || 'הקלד מספר'}</div>
      <div style="font-size:28px;color:var(--fos-accent);min-height:34px;opacity:${match ? 1 : 0}">מיכל לוי</div></div>
    <div style="flex:1;padding:0 48px;display:grid;direction:ltr;grid-template-columns:repeat(3,1fr);gap:16px;align-content:center">${keys}</div>
    <div style="padding:24px 48px 32px">${Button({ label: 'התקשר', variant: 'primary', f: digits ? Math.max(0.999 * (digits.length ? 1 : 0), call) : 0 })}</div>`, '');
}
function ActiveCall(t) {
  const lt = t - 13.62;
  const tiles = [['mic', 'השתק'], ['volume_up', 'רמקול'], ['pause', 'המתנה'], ['dialpad', 'מקלדת']].map(([i, l], k) =>
    `<div style="height:132px;border-radius:32px;background:var(--fos-glass);border:4px solid ${k === 0 ? 'var(--fos-accent)' : 'transparent'};${col(6)};align-items:center;justify-content:center">
      ${ic(i, 40, 'var(--fos-text-70)', 1, 0, 1.6)}<span style="font-size:26px;color:var(--fos-text-60)">${l}</span></div>`).join('');
  return center(`<div style="${col(8)};align-items:center;padding:72px 0 40px">${Avatar('מיכל לוי', 160)}
      <div style="font-size:40px;font-weight:700;font-family:var(--fos-font-display)">מיכל לוי</div>
      <div style="font-size:28px;color:${lt < 0.25 ? 'var(--fos-text-60)' : 'var(--fos-success)'};direction:ltr;font-variant-numeric:tabular-nums">${lt < 0.25 ? 'מתקשר' : '00:00'}</div></div>
    <div style="flex:1;padding:0 48px;display:grid;grid-template-columns:1fr 1fr;gap:16px;align-content:start">${tiles}</div>
    <div style="padding:24px 48px 40px">${Button({ label: 'סיים שיחה', variant: 'destructive' })}</div>`);
}

/* ============================================================ 2  messages, T9 */
function T9Screen(t) {
  let text = '', comp = null;
  for (const w of TL.t9) {
    if (t >= w.commit) { text += (text ? ' ' : '') + w.word; continue; }
    if (t >= w.t0) {
      const n = w.digits.filter((_, i) => t >= w.t0 + i * 0.125).length;
      comp = { w, n, list: w.cands[Math.max(0, n - 1)], seq: w.digits.slice(0, n) };
    }
  }
  const compText = comp ? comp.list[0] : '';
  const cands = comp ? comp.list : [];
  const seq = comp ? comp.seq.join(' ') : '';
  const chips = cands.map((c, i) => `<span style="flex:none;border-radius:24px;padding:${i ? 12 : 14}px ${i ? 26 : 30}px;font-size:${i ? 30 : 32}px;font-weight:${i ? 400 : 700};line-height:1.2;background:${i ? '#2C2C2E' : 'var(--fos-accent)'};color:${i ? '#fff' : '#000'}">${c}</span>`).join('');
  const caret = (Math.floor(t * 2) % 2 === 0) || comp ? 1 : 0;
  const key = (k, l) => `<span style="display:flex;align-items:center;gap:8px"><span style="min-width:40px;height:40px;border-radius:12px;background:#2C2C2E;color:#fff;font-size:22px;font-weight:500;display:flex;align-items:center;justify-content:center;padding:0 6px">${k}</span><span style="font-size:21px;color:#B0B0B0">${l}</span></span>`;
  return `${TopBar({ title: 'הודעה חדשה', back: true })}
    <div style="padding:0 32px;${col(16)}">
      <div style="direction:rtl;font-size:30px;background:var(--fos-idle-bg-field);border-radius:32px;padding:24px;display:flex;align-items:center;gap:16px;border:4px solid transparent">
        <span style="color:var(--fos-text-40)">אל</span><span style="font-weight:500">מיכל לוי</span></div>
      <div style="min-height:250px;background:var(--fos-idle-bg-field);border-radius:32px;padding:28px;border:4px solid var(--fos-accent);font-size:58px;line-height:1.3;font-weight:500">
        ${text ? text + ' ' : ''}${comp ? `<span style="border-bottom:5px solid var(--fos-accent)">${compText}</span>` : ''}<span style="display:inline-block;width:4px;height:64px;background:var(--fos-accent);vertical-align:-12px;margin-inline-start:4px;opacity:${caret}"></span></div></div>
    <div style="position:absolute;left:0;right:0;bottom:0;background:#1C1C1E;border-top:2px solid rgba(255,255,255,0.10);border-radius:32px 32px 0 0;padding:18px 0 22px;display:flex;flex-direction:column;gap:14px">
      <div style="display:flex;align-items:center;gap:12px;padding:0 32px">
        <span style="display:flex;align-items:center;background:rgba(255,255,255,0.16);border:2px solid #fff;border-radius:44px;padding:8px 20px"><span style="font-size:24px;font-weight:700;line-height:1">עב</span></span>
        <span style="font-size:22px;color:#B0B0B0;line-height:1">ניבוי</span><span style="flex-grow:1"></span>
        <span class="mono" style="font-size:22px;color:rgba(255,255,255,0.42);letter-spacing:3px">${seq}</span>
        ${cands.length ? `<span class="mono" style="font-size:22px;color:#B0B0B0;background:#2C2C2E;border-radius:16px;padding:4px 12px">1/${cands.length}</span>` : ''}</div>
      <div style="height:84px;display:flex;align-items:center;gap:16px;padding:0 32px;overflow:hidden">${chips}</div>
      <div style="display:flex;align-items:center;gap:18px;padding:14px 32px 0;border-top:2px solid rgba(255,255,255,0.10)">${key('#', 'שפה')}${key('*', 'פיסוק')}${key('0', 'רווח')}${key(ic('keyboard_arrow_left', 24, '#fff', 1, 0, 2), 'מועמדות')}</div></div>`;
}

/* ============================================================ 3  bluetooth */
function Bluetooth(t) {
  const nav = navSim(t, 16.0, 18.0, 0, 1, 4);
  const conn = t >= 17.0 ? (t >= 17.5 ? 2 : 1) : 0;
  const dev = [['אוזניות', 'headphones', 'מחובר · 80%', true], ['מערכת רכב', 'directions_car', conn === 2 ? 'מחובר' : conn === 1 ? 'מתחבר' : 'מותאם', conn === 2], ['רמקול סלון', 'speaker', 'מותאם', false]];
  const rows = dev.map(([n, i, s, on], k) => {
    const f = fa(nav, k + 2, t);
    return (k ? Divider() : '') + ListItem({ style: 'margin:4px 10px;height:114px', title: n, summary: s, f, trailing: `<div style="display:flex;align-items:center;gap:16px">${ic(i, 44, on ? 'var(--fos-accent)' : 'var(--fos-text-40)')}${ic('keyboard_arrow_left', 36, 'var(--fos-text-30)')}</div>` });
  }).join('');
  return `${TopBar({ title: 'בלוטוס', menu: true })}
    ${Card(`${SettingItem({ title: 'בלוטוס', summary: 'מופעל', icon: 'bluetooth', trailing: Switch(1), f: fa(nav, 0, t), pos: 'first' })}${Divider()}
      ${SettingItem({ title: 'שם המכשיר', summary: 'FutureOS', icon: 'badge', f: fa(nav, 1, t), pos: 'last' })}`)}
    ${SectionHeader('מכשירים מותאמים')}${Card(rows)}${SoftKeys('אפשרויות', 'בחר', 'חזרה')}`;
}

/* ============================================================ 4  translate */
function Translate(t) {
  const src = 'איפה תחנת הרכבת?', dst = 'Where is the train station?';
  const n = Math.round(src.length * inv(18.2, 19.0, t));
  const text = src.slice(0, n), done = t >= 19.05;
  const sub = t >= 19.5 ? 1 : 0, ft = tween(t, 19.5);
  const cap = (l, name) => `<div style="flex:1;min-width:0;min-height:96px;border-radius:999px;background:var(--fos-text-08);border:4px solid transparent;display:flex;flex-direction:column;align-items:center;justify-content:center;padding:8px 16px"><div style="font-size:26px;color:var(--fos-text-55);letter-spacing:2px">${l}</div><div style="font-size:34px;font-weight:500">${name}</div></div>`;
  const acts = [['volume_up', 'השמע'], ['content_copy', 'העתק'], ['share', 'שתף'], ['star', 'שמור']].map(([i, l], k) => {
    const f = k === 0 ? 1 - ft : k === 1 ? ft : 0;
    return `<div style="flex:1;display:flex;flex-direction:column;align-items:center;gap:4px;padding:8px 0;border-radius:24px;background:${cm('var(--fos-accent)', 20 * f)};border:4px solid ${cm('var(--fos-accent)', 100 * f)}">${ic(i, 40, 'var(--fos-accent)')}<div style="font-size:24px;color:var(--fos-text-60)">${l}</div></div>`;
  }).join('');
  const out = done ? Card(`<div style="padding:20px 24px;${col(16)}">
      <div style="font-size:26px;color:var(--fos-text-55);letter-spacing:2px">אנגלית</div>
      <div style="font-size:34px;font-weight:500;line-height:1.3;direction:ltr;text-align:left">${dst}</div>
      ${ProgressBar(clamp((t - 19.05) / 1.6))}</div>${Divider()}<div style="display:flex;padding:8px 20px;gap:12px">${acts}</div>`) : '';
  return `${TopBar({ title: 'תרגום', menu: true })}
    <div style="display:flex;align-items:center;gap:16px;padding:0 32px 20px">${cap('מ', 'עברית')}<div class="ibtn" style="--f:0">${ic('swap_horiz', 36, 'var(--fos-text)')}</div>${cap('אל', 'אנגלית')}</div>
    ${Card(`<div style="padding:20px 24px;${col(12)}">
      <div style="display:flex;justify-content:space-between"><div style="font-size:26px;color:var(--fos-text-55);letter-spacing:2px">עברית</div><div style="font-size:26px;color:var(--fos-text-40);direction:ltr">${text.length}/1000</div></div>
      <div style="background:var(--fos-idle-bg-field);border-radius:32px;border:4px solid ${done ? 'transparent' : 'var(--fos-accent)'};padding:20px;min-height:150px;font-size:32px;line-height:1.3">${text}${done ? '' : '<span style="display:inline-block;width:3px;height:34px;background:var(--fos-accent);vertical-align:-6px;margin-inline-start:6px"></span>'}</div></div>`)}${out}${SoftKeys('אפשרויות', done ? 'בחר' : 'תרגם', 'חזרה')}`;
}

/* ============================================================ 5  alarms */
function Alarms(t) {
  const nav = navSim(t, 20.0, 22.0, 0, 1, 5);
  const A = [['07:00', 'ימי חול', 1], ['08:30', 'שבת', 0], ['13:15', 'חד פעמי', 1], ['18:45', 'ימי חול', 0], ['22:30', 'חד פעמי', 0]];
  const on = [1, stepTween(t, [[0, 0], [20.5, 1]]), stepTween(t, [[0, 1], [21.0, 0]]), 0, 0];
  const rows = A.map(([time, days], i) => (i ? Divider() : '') + SettingItem({ title: `<span class="mono" style="font-weight:600">${time}</span>`, summary: days, icon: 'alarm', chevron: false, f: fa(nav, i, t), trailing: Switch(on[i]), pos: i === 0 ? 'first' : i === 4 ? 'last' : 'middle' })).join('');
  const snack = t >= 21.05 ? `<div style="position:absolute;left:0;right:0;bottom:80px;opacity:${inv(21.05, 21.2, t)};transform:translateY(${(1 - out3((t - 21.05) / 0.25)) * 40}px);margin:0 24px 24px;padding:24px 28px;border-radius:40px;background:var(--fos-surface);box-shadow:var(--fos-shadow-headsup);display:flex;align-items:center;gap:28px;direction:rtl">
    <div style="flex:1;font-size:30px">המעורר של 13:15 כבוי</div><div style="font-size:30px;font-weight:700;color:var(--fos-accent)">בטל</div></div>` : '';
  return `${TopBar({ title: 'מעורר', menu: true })}${Card(rows)}${snack}${SoftKeys('הוסף', 'החלף', 'חזרה')}`;
}

/* ============================================================ 6  calendar (Hebrew) */
function CalendarScreen(t) {
  const nav = navSim(t, 22.0, 24.0, 18, 7, 30, 6);                    // index = day - 1, starts on the 19th
  const sel = t >= 23.25 ? 21 : 18;                                  // the 22nd once OK is pressed
  const first = 6;                                                   // 1 Tishrei 5787 is a Saturday
  const days = ['א', 'ב', 'ג', 'ד', 'ה', 'ו', 'ש'];
  let cells = '';
  for (let i = 0; i < first; i++) cells += '<div></div>';
  for (let d = 1; d <= 30; d++) {
    const f = fa(nav, d - 1, t), s = d - 1 === sel ? 1 : 0;
    const g = d <= 19 ? 11 + d : d - 19;
    cells += `<div style="aspect-ratio:1;border-radius:999px;background:${s ? 'var(--fos-accent)' : 'transparent'};border:4px solid ${s ? 'transparent' : cm('var(--fos-accent)', 100 * f)};display:flex;flex-direction:column;align-items:center;justify-content:center;color:${s ? 'var(--fos-on-accent)' : 'var(--fos-text)'}">
      <span style="font-size:30px;font-weight:${s ? 700 : 500};line-height:1">${hebNum[d]}</span><span style="font-size:17px;line-height:1;margin-top:3px;color:${s ? 'rgba(0,0,0,0.6)' : 'var(--fos-text-40)'}">${g}</span></div>`;
  }
  const step = (i) => `<div style="width:72px;height:72px;border-radius:999px;background:var(--fos-text-08);display:flex;align-items:center;justify-content:center">${ic(i, 36, 'var(--fos-accent)')}</div>`;
  const detail = t >= 23.3 ? `<div style="opacity:${inv(23.3, 23.5, t)}">${Card(SettingItem({ title: 'שמיני עצרת', summary: 'כ״ב בתשרי · 3 באוקטובר', icon: 'celebration', chevron: false, pos: 'only' }))}</div>` : '';
  return `${TopBar({ title: 'לוח שנה', menu: true })}
    <div style="display:flex;align-items:center;justify-content:center;gap:32px;padding:0 32px 12px">${step('keyboard_arrow_left')}
      <div style="min-width:300px;text-align:center"><div style="font-size:34px;font-weight:500">תשרי תשפ״ז</div><div style="font-size:22px;color:var(--fos-text-50)">ספטמבר · אוקטובר 2026</div></div>${step('keyboard_arrow_right')}</div>
    <div style="padding:0 24px;${col(8)}">
      <div style="display:grid;grid-template-columns:repeat(7,1fr);gap:8px">${days.map((d) => `<div style="text-align:center;font-size:24px;color:var(--fos-text-50);letter-spacing:2px">${d}</div>`).join('')}</div>
      <div style="display:grid;grid-template-columns:repeat(7,1fr);gap:8px">${cells}</div></div>${detail}${SoftKeys('אפשרויות', 'בחר', 'חזרה')}`;
}

/* ============================================================ 7  music */
function MusicScreen(t) {
  const nav = navSim(t, 24.0, 26.0, 1, 3, 3);
  const vol = navSim(t, 25.4, 26.0, 0, 1, 2);
  const vf = vol.i === 1 ? tween(t, vol.t0) : 0;
  const next = t >= 25.0;
  const lt = next ? t - 25.0 : t - 24.0 + 72;
  const dur = next ? 214 : 220;
  const mm = (s) => `${Math.floor(s / 60)}:${String(Math.floor(s % 60)).padStart(2, '0')}`;
  const eq = beatEnv(t);
  const btn = (icon, k, big) => `<div style="height:132px;border-radius:32px;background:var(--fos-glass);border:4px solid ${cm('var(--fos-accent)', 100 * fa(nav, k, t) * (1 - vf))};display:flex;align-items:center;justify-content:center">${ic(icon, big ? 64 : 48, 'var(--fos-text)', big ? 1 : 0.75, big ? 1 : 0, 1.5)}</div>`;
  return `${TopBar({ title: 'מוזיקה', menu: true })}
    <div style="${col(8)};align-items:center;padding:16px 32px 24px">
      <div style="width:200px;height:200px;border-radius:999px;background:var(--fos-glass);display:flex;align-items:center;justify-content:center;transform:scale(${1 + 0.05 * eq})">${ic('graphic_eq', 96, 'var(--fos-accent)', 1, 0, 1.4)}</div>
      <div style="font-family:var(--fos-font-display);font-size:40px;font-weight:700;margin-top:16px">${next ? 'עיר בלילה' : 'בדרך'}</div>
      <div style="font-size:28px;color:var(--fos-text-60)">${next ? 'קולות מהעיר' : 'אלבום לדוגמה'}</div></div>
    <div style="padding:0 40px;${col(12)}">${ProgressBar(lt / dur, true)}
      <div class="mono" style="display:flex;justify-content:space-between;font-size:26px;color:var(--fos-text-60);direction:ltr"><span>${mm(lt)}</span><span>${mm(dur)}</span></div></div>
    <div style="display:grid;grid-template-columns:repeat(3,1fr);gap:16px;padding:28px 32px 20px">${btn('skip_previous', 0)}${btn('pause', 1, true)}${btn('skip_next', 2)}</div>
    ${Slider({ label: 'עוצמה', value: 0.6, f: vf })}${SoftKeys('אפשרויות', 'השהה', 'חזרה')}`;
}

/* ============================================================ 8  fitness */
function FitnessScreen(t) {
  const lt = t - 26.0;
  const bpm = Math.round(lerp(72, 131, out3(lt / 1.8)));
  const eq = beatEnv(t);
  const btnf = tween(t, 27.0), pressed = pressOf('ok', t);
  const nav = (items, sel) => `<div style="background:var(--fos-surface);border-radius:999px;margin:0 32px 32px;height:132px;display:flex;align-items:center;padding:0 16px">${items.map(([i, l], k) => `
    <div style="flex:${k === sel ? 1.4 : 1};display:flex;justify-content:center"><div style="height:92px;border-radius:999px;background:${k === sel ? 'var(--fos-accent)' : 'transparent'};display:flex;align-items:center;gap:16px;padding:${k === sel ? '0 28px' : '0'}">${ic(i, 40, k === sel ? 'var(--fos-on-accent)' : 'var(--fos-text)', k === sel ? 1 : 0.5, k === sel ? 1 : 0)}${k === sel ? `<span style="font-size:28px;font-weight:500;color:var(--fos-on-accent)">${l}</span>` : ''}</div></div>`).join('')}</div>`;
  return center(`${TopBar({ title: 'כושר', menu: true })}
    <div style="flex:1;${col(20)};align-items:center;justify-content:center;padding:0 32px">
      <div style="font-size:24px;letter-spacing:2px;color:var(--fos-text-60)">אימון פעיל</div>
      <div style="display:flex;align-items:center;gap:24px"><div style="transform:scale(${1 + 0.22 * eq})">${ic('favorite', 72, 'var(--fos-danger)', 1, 1, 1.6)}</div>
        <div class="mono" style="font-size:180px;font-weight:300;line-height:1">${bpm}</div></div>
      <div style="font-size:28px;color:var(--fos-text-60)">פעימות לדקה</div>
      <div style="display:flex;gap:16px;margin-top:8px">${Chip('קרדיו', 'selected')}${Chip('שריפת שומן')}${Chip('מאמץ')}</div>
      <div style="width:100%;padding:0 8px;margin-top:8px">${ProgressBar(lerp(0.2, 0.72, out3(lt / 1.8)))}</div>
      <div class="mono" style="font-size:40px;font-weight:300;color:var(--fos-text-70)">00:${String(12 + Math.floor(lt)).padStart(2, '0')}</div>
      <div style="display:flex;align-items:center;gap:12px;font-size:26px;color:var(--fos-success)">${ic('bluetooth', 28, 'var(--fos-success)')}חיישן דופק מחובר</div></div>
    <div style="padding:0 32px 24px">${Button({ label: t >= 27.25 ? 'המשך' : 'השהה', variant: 'primary', f: Math.max(btnf, pressed) })}</div>
    ${nav([['fitness_center', 'אימון'], ['history', 'היסטוריה'], ['trending_up', 'מדדים']], 0)}`);
}

/* ============================================================ 9  terminal */
function Terminal(t) {
  let lines = `<div style="color:var(--fos-text-50)">FutureOS 1.0</div><div style="color:var(--fos-text-50)">root shell</div><div>&nbsp;</div>`;
  const prompt = '<span style="color:var(--fos-accent)">#</span> ';
  for (const l of TL.term) {
    if (l.type) {
      const n = clamp(Math.floor((t - l.t0) / l.dt) + 1, 0, l.type.length);
      if (t >= l.t0) lines += `<div>${prompt}${l.type.slice(0, n)}</div>`;
    } else if (t >= l.t) lines += `<div style="color:var(--fos-output-text)">${l.out}</div>`;
  }
  const caret = `<span style="display:inline-block;width:16px;height:32px;background:var(--fos-accent);vertical-align:-6px;opacity:${Math.floor(t * 4) % 2 ? 0.2 : 1}"></span>`;
  const last = TL.term[TL.term.length - 1];
  const idle = t >= last.t;
  return `${TopBar({ title: 'טרמינל', menu: true })}
    <div class="mono" style="margin:8px 32px;padding:28px;height:760px;border-radius:32px;background:var(--fos-input-bar);font-size:30px;line-height:1.55;text-align:left;overflow:hidden">${lines}${idle ? `<div>${prompt}${caret}</div>` : ''}${!idle && !lines ? '' : ''}</div>`;
}

/* ============================================================ 10  books */
function Books(t) {
  const n = Math.round(6211 * out3(inv(30.1, 30.95, t)));
  const nav = navSim(t, 30.0, 32.0, -1, 1, 5);
  const cat = [['תנ״ך', 'בראשית · שמות · ויקרא'], ['משנה', 'סדר זרעים · סדר מועד'], ['תלמוד', 'בבלי · ירושלמי'], ['הלכה', 'שולחן ערוך · רמב״ם'], ['מדרש', 'בראשית רבה · שמות רבה']];
  const rows = cat.map(([a, b], i) => ListItem({ title: a, summary: b, f: nav.i === i ? tween(t, nav.t0) : nav.prev === i ? 1 - tween(t, nav.t0) : 0, trailing: ic('keyboard_arrow_left', 36, 'var(--fos-text-30)') })).join(`<div style="height:12px"></div>`);
  return `${TopBar({ title: 'בלכתך בדרך', menu: true })}
    <div style="${col(8)};align-items:center;padding:16px 0 28px"><div class="mono" style="font-size:96px;font-weight:300;line-height:1.05">${n.toLocaleString('en-US')}</div><div style="font-size:28px;color:var(--fos-text-60)">ספרים בספרייה</div></div>
    <div style="padding:0 32px">${rows}</div>${SoftKeys('חיפוש', 'פתח', 'חזרה')}`;
}

/* ============================================================ 11  remote */
function Remote(t) {
  if (t < 32.25) {
    const items = [['ac_unit', 'מזגן סלון', 'קירור · 26°'], ['air', 'מאוורר', 'כבוי'], ['speaker', 'מערכת שמע', 'כבוי']];
    return `${TopBar({ title: 'שלט', menu: true })}<div style="padding:0 32px;${col(24)}">${items.map(([i, a, b], k) => ListItem({ title: a, summary: b, f: k === 0 ? 1 : 0, trailing: ic(i, 44, k === 0 ? 'var(--fos-accent)' : 'var(--fos-text-40)') })).join('')}</div>${SoftKeys('אפשרויות', 'בחר', 'חזרה')}`;
  }
  const temp = 26 - countKeys(t, 32.5, /^down$/);
  const pulse = Math.max(pressOf('down', t), 0);
  const tiles = [['power_settings_new', 'הפעלה', 1], ['ac_unit', 'קירור', 1], ['air', 'מאוורר', 0], ['timer', 'טיימר', 0]].map(([i, l, on], k) =>
    `<div style="height:132px;border-radius:32px;background:${on ? 'var(--fos-accent-20)' : 'var(--fos-glass)'};border:4px solid transparent;${col(6)};align-items:center;justify-content:center">${ic(i, 40, on ? 'var(--fos-accent)' : 'var(--fos-text-70)', 1, 0, 1.3)}<span style="font-size:26px;color:var(--fos-text-60)">${l}</span></div>`).join('');
  return `${TopBar({ title: 'מזגן סלון', back: true })}
    <div style="height:820px;display:flex;flex-direction:column;justify-content:center;padding-bottom:60px"><div style="${col(10)};align-items:center;padding:0 0 44px">
      <div style="height:48px;display:flex;align-items:center;gap:12px;color:var(--fos-accent);opacity:${pulse}">${ic('sensors', 40, 'var(--fos-accent)')}</div>
      <div class="mono" style="font-size:150px;font-weight:300;line-height:1">${temp}°</div><div style="font-size:28px;color:var(--fos-text-60)">קירור</div></div>
    <div style="padding:0 40px;display:grid;grid-template-columns:1fr 1fr;gap:16px">${tiles}</div></div>${SoftKeys('', 'שלח', 'חזרה')}`;
}

/* ============================================================ 12  assistant */
function Assistant(t) {
  const lt = t - 34.25;
  const words = ['תזכיר', 'לי', 'להתקשר', 'לאמא'];
  const n = clamp(Math.floor((lt - 0.45) / 0.28) + 1, 0, words.length);
  const bars = [0, 1, 2, 3, 4, 5, 6, 7, 8].map((i) => {
    const h = 24 + 120 * (0.5 + 0.5 * Math.sin(t * 13 + i * 1.7)) * (0.35 + 0.65 * Math.sin(i / 8 * Math.PI)) * clamp(lt / 0.3);
    return `<div style="width:14px;height:${h}px;border-radius:7px;background:var(--fos-accent)"></div>`;
  }).join('');
  return center(`${TopBar({ title: 'עוזר קולי' })}
    <div style="flex:1;${col(36)};align-items:center;justify-content:center;padding:0 40px">
      <div style="width:240px;height:240px;border-radius:999px;background:var(--fos-accent-20);border:4px solid var(--fos-accent);display:flex;align-items:center;justify-content:center;transform:scale(${1 + 0.04 * beatEnv(t)})">${ic('mic', 116, 'var(--fos-accent)', 1, 0, 1.4)}</div>
      <div style="height:160px;display:flex;align-items:center;gap:12px">${bars}</div>
      <div style="font-size:28px;color:var(--fos-text-60)">${n < words.length ? 'מקשיב' : 'נשמע'}</div>
      <div style="min-height:56px;font-size:44px;font-weight:500;text-align:center;line-height:1.3">${words.slice(0, n).join(' ')}</div></div>`);
}

/* ============================================================ settings, accent, display */
const ACC = [['לבן', '#FFFFFF'], ['תכלת', '#64D2FF'], ['כתום', '#FF9F0A'], ['ירוק', '#30D158'], ['סגול', '#BF5AF2']];
function accentIndexAt(t) {
  if (t < 37.5) return 0; if (t < 38.0) return 1; if (t < 38.5) return 2; if (t < 39.0) return 3; if (t < 39.625) return 4; return 1;
}
function SettingsRoot(t, a, b, startFocus) {
  const nav = navSim(t, a, b, startFocus, 1, 6);
  const G = [['כללי', [['תצוגה', 'בהירות, גודל טקסט', 'brightness_6'], ['צלילים', 'עוצמת מדיה ורינגטון', 'volume_up'], ['צבע הדגשה', ACC[accentIndexAt(t)][0], 'palette']]],
             ['מערכת', [['שפה', 'עברית', 'language'], ['מקשים', 'קיצורי מקשים מספריים', 'keyboard'], ['אודות', 'FutureOS 1.0', 'info']]]];
  let i = 0, h = '';
  for (const [head, rows] of G) {
    h += SectionHeader(head) + Card(rows.map(([tt, ss, ii], k) => { const idx = i++; return (k ? Divider() : '') + SettingItem({ title: tt, summary: ss, icon: ii, f: fa(nav, idx, t), pos: k === 0 ? 'first' : k === rows.length - 1 ? 'last' : 'middle' }); }).join(''));
  }
  return `${TopBar({ title: 'הגדרות', back: true, menu: true })}${h}${SoftKeys('', 'בחר', 'חזרה')}`;
}
function AccentScreen(t) {
  const nav = navSim(t, 36.75, 40.0, 0, 1, 5);
  const selIdx = accentIndexAt(t);
  const rows = ACC.map(([name, val], i) => (i ? Divider() : '') + SettingItem({ title: name, icon: 'circle', chevron: false, f: fa(nav, i, t), iconColor: val, style: `--fos-accent:${val}`,
    trailing: selIdx === i ? ic('check', 44, 'var(--fos-accent)') : '<div style="width:44px"></div>', pos: i === 0 ? 'first' : i === 4 ? 'last' : 'middle' })).join('');
  return `${TopBar({ title: 'צבע הדגשה', back: true })}${Card(rows)}
    <div style="padding:40px 48px;font-size:26px;color:var(--fos-text-40);line-height:1.4">צבע ההדגשה הוא הצבע היחיד שהמשתמש בוחר. הוא מסמן פוקוס ובחירה בלבד.</div>${SoftKeys('', 'בחר', 'חזרה')}`;
}
function DisplayScreen(t) {
  const nav = navSim(t, 40.75, 44.0, 0, 1, 5);
  const dark = stepTween(t, [[0, 1], [41.5, 0], [43.0, 1]], 0.001);
  return `${TopBar({ title: 'תצוגה', back: true, menu: true })}
    <div style="margin-top:12px;${col(24)}">${Slider({ label: 'בהירות מסך', value: 0.72, f: fa(nav, 0, t) })}${Slider({ label: 'גודל טקסט', value: 0.5, f: fa(nav, 1, t) })}</div>
    ${SectionHeader('תצוגה')}
    ${Card(SettingItem({ title: 'מצב כהה', summary: dark > 0.5 ? 'מופעל' : 'כבוי', icon: 'dark_mode', trailing: Switch(dark), f: fa(nav, 2, t), pos: 'first' }) + Divider() +
      SettingItem({ title: 'בהירות אדפטיבית', summary: 'התאמה לתאורת הסביבה', icon: 'brightness_auto', trailing: Switch(1), f: fa(nav, 3, t), chevron: false, pos: 'middle' }) + Divider() +
      SettingItem({ title: 'אנימציות', summary: 'מופעל', icon: 'animation', trailing: Switch(1), f: fa(nav, 4, t), chevron: false, pos: 'last' }))}${SoftKeys('', 'החלף', 'חזרה')}`;
}

/* ============================================================ 44-48 the choices */
function Choices(t) {
  const nav = navSim(t, 44.0, 48.0, 0, 1, 4);
  const rows = [['מסך מגע', 'smartphone', [[0, 1], [44.0, 0]]], ['גלילה אינסופית', 'swap_vert', [[0, 1], [45.0, 0]]], ['התראות מציקות', 'notifications', [[0, 1], [46.0, 0]]], ['מקשים', 'keyboard', [[0, 0], [47.0, 1]]]];
  return `<div style="height:100%;display:flex;flex-direction:column;justify-content:center;padding-bottom:40px">${TopBar({ title: 'הבחירות שלי' })}
    ${Card(rows.map(([title, icon, steps], i) => {
      const p = stepTween(t, steps);
      return (i ? Divider() : '') + SettingItem({ title, summary: p > 0.5 ? 'מופעל' : 'כבוי', icon, trailing: Switch(p), f: fa(nav, i, t), chevron: false, pos: i === 0 ? 'first' : i === 3 ? 'last' : 'middle' });
    }).join(''))}</div>`;
}

/* ============================================================ router */
function screenAt(t) {
  if (t < 12) return Launcher(t, 9.5, 12.0, 0);
  if (t < 13.62) return Dialer(t);
  if (t < 14) return ActiveCall(t);
  if (t < 16) return T9Screen(t);
  if (t < 18) return Bluetooth(t);
  if (t < 20) return Translate(t);
  if (t < 22) return Alarms(t);
  if (t < 24) return CalendarScreen(t);
  if (t < 26) return MusicScreen(t);
  if (t < 28) return FitnessScreen(t);
  if (t < 30) return Terminal(t);
  if (t < 32) return Books(t);
  if (t < 34) return Remote(t);
  if (t < 34.25) return Launcher(t, 34.0, 34.25, 0, true);
  if (t < 36) return Assistant(t);
  if (t < 36.75) return SettingsRoot(t, 36.0, 36.75, 0);
  if (t < 40) return AccentScreen(t);
  if (t < 40.75) return SettingsRoot(t, 40.0, 40.75, 2);
  if (t < 44) return DisplayScreen(t);
  if (t < 48) return Choices(t);
  if (t < 52.5) return Launcher(t, 48.0, 52.5, 0);
  return Launcher(t, 60, 60, 0, true);
}
const isLight = (t) => t >= 41.5 && t < 43.0;
const accentAt = (t) => {
  const c = [[0, '#FFFFFF'], [37.5, '#64D2FF'], [38.0, '#FF9F0A'], [38.5, '#30D158'], [39.0, '#BF5AF2'], [39.625, '#64D2FF'], [54.0, '#FFFFFF']];
  let cur = c[0][1], prev = cur, t0 = -1e9;
  for (const [ts, v] of c) if (t >= ts) { prev = cur; cur = v; t0 = ts; }
  return mixHex(prev, cur, tween(t, t0, 0.2));
};
function mixHex(a, b, p) {
  const A = [1, 3, 5].map((i) => parseInt(a.slice(i, i + 2), 16)), B = [1, 3, 5].map((i) => parseInt(b.slice(i, i + 2), 16));
  return '#' + A.map((v, i) => Math.round(lerp(v, B[i], p)).toString(16).padStart(2, '0')).join('');
}
