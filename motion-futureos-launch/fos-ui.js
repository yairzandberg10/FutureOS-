// fos-ui.js: the phone's screens, 640 x 960 device px, built only from the design system
// (colours, sizes, radii, Future Glyphs). One focus frame moves between everything: it is the only thing that
// travels from screen to screen, and every change of screen starts inside it.
window.FOS = window.FOS || {};
(() => {
  'use strict';
  const NS = 'http://www.w3.org/2000/svg';

  function mk(parent, css = {}, cls = 'ui') {
    const d = document.createElement('div'); d.className = cls;
    d.style.position = 'absolute';
    if (css.left == null && css.right == null) d.style.left = '0';   // anchored by whichever side the caller gives
    if (css.top == null && css.bottom == null) d.style.top = '0';
    Object.assign(d.style, css); parent.appendChild(d); return d;
  }
  // a line of UI text: align r = right edge on x, c = centre, l = left edge; vertically centred on y
  function lab(parent, s, x, y, o = {}) {
    const T = FOS.T;
    const d = mk(parent, {
      left: x + 'px', top: y + 'px', fontFamily: o.font || T.font, fontSize: (o.size || 32) + 'px', fontWeight: o.weight || 400,
      color: o.color || '#fff', opacity: o.alpha ?? 1, direction: o.dir || 'rtl', lineHeight: 1.3, letterSpacing: (o.ls || 0) + 'px',
      transform: `translate(${o.align === 'l' ? 0 : o.align === 'c' ? -50 : -100}%, -50%)`, whiteSpace: 'nowrap',
    });
    d.textContent = s; return d;
  }
  // a Future Glyph centred on (cx, cy)
  function ico(parent, name, cx, cy, size, color = '#fff', o = {}) {
    const s = document.createElementNS(NS, 'svg');
    s.setAttribute('viewBox', '0 0 24 24'); s.setAttribute('width', size); s.setAttribute('height', size);
    s.style.cssText = `position:absolute;left:${cx - size / 2}px;top:${cy - size / 2}px;overflow:visible`;
    const sw = o.stroke || 1.6;
    for (const part of FOS.ICONS[name].split('|')) {
      if (part.startsWith('d:')) {
        const [x, y] = part.slice(2).split(' ');
        const c = document.createElementNS(NS, 'circle'); c.setAttribute('cx', x); c.setAttribute('cy', y); c.setAttribute('r', 1.2);
        c.setAttribute('fill', color); s.appendChild(c);
      } else {
        const p = document.createElementNS(NS, 'path'); p.setAttribute('d', part);
        p.setAttribute('fill', o.fill && part.trim().endsWith('Z') ? color : 'none');
        p.setAttribute('stroke', color); p.setAttribute('stroke-width', sw); p.setAttribute('stroke-linecap', 'round'); p.setAttribute('stroke-linejoin', 'round');
        s.appendChild(p);
      }
    }
    if (o.alpha != null) s.style.opacity = o.alpha;
    parent.appendChild(s); return s;
  }
  const svgEl2 = (tag, parent, attrs = {}) => { const e = document.createElementNS(NS, tag); for (const k in attrs) e.setAttribute(k, attrs[k]); parent.appendChild(e); return e; };
  FOS.uiHelpers = { mk, lab, ico, svgEl2 };

  const hexA = (hex, a) => { const n = parseInt(hex.slice(1), 16); return `rgba(${n >> 16},${(n >> 8) & 255},${n & 255},${a})`; };
  const mixHex = (a, b, k) => {
    const A = parseInt(a.slice(1), 16), B = parseInt(b.slice(1), 16);
    const c = i => Math.round(((A >> i) & 255) + (((B >> i) & 255) - ((A >> i) & 255)) * k);
    return `rgb(${c(16)},${c(8)},${c(0)})`;
  };
  FOS.hexA = hexA; FOS.mixHex = mixHex;

  FOS.buildUI = (parent) => {
    const T = FOS.T, TL = FOS.TL, C = FOS.COPY, LOGO = FOS.LOGO, U = FOS.U;
    const FULL = [0, 0, 640, 960, U.CORNER_TOP, U.CORNER_TOP, 0, 0];
    const root = mk(parent, { width: '640px', height: '960px', transformOrigin: '0 0', overflow: 'hidden', background: '#000',
      borderRadius: `${U.CORNER_TOP}px ${U.CORNER_TOP}px 0 0` }, 'a');
    const layer = () => mk(root, { left: 0, top: 0, width: '640px', height: '960px', transformOrigin: '320px 480px' });
    const E = (t, t0, wn = 18) => S(t - t0, wn, 1);           // critically damped: expo-out without overshoot
    const ramp = (t, a, b) => E_s(seg(t, a, b));
    const E_s = x => x <= 0 ? 0 : x >= 1 ? 1 : x * x * (3 - 2 * x);
    const lerpR = (a, b, k) => a.map((v, i) => v + (b[i] - v) * k);
    const clipOf = r => `inset(${r[1].toFixed(1)}px ${(640 - r[0] - r[2]).toFixed(1)}px ${(960 - r[1] - r[3]).toFixed(1)}px ${r[0].toFixed(1)}px round ${r[4].toFixed(1)}px ${r[5].toFixed(1)}px ${r[6].toFixed(1)}px ${r[7].toFixed(1)}px)`;

    // accent over time: white, then the four the user picks on the settings screen
    const accentIdx = t => TL.colors.reduce((n, [, ok], i) => t >= ok ? i + 1 : n, 0);
    const accentAt = t => T.accents[accentIdx(t)];

    // ---------------------------------------------------------------- top bar (ScreenTopBar)
    function topBar(p, title, { back = false, menu = true } = {}) {
      const g = mk(p, { left: 0, top: 0, width: '640px', height: '120px' });
      let x = 608;
      if (back) {
        mk(g, { left: 608 - 72 + 'px', top: '24px', width: '72px', height: '72px', borderRadius: '50%', background: hexA('#FFFFFF', 0.08) });
        ico(g, 'arrow_forward', 608 - 36, 60, 36, '#fff');
        x = 608 - 72 - 16;
      }
      lab(g, title, x, 60, { font: T.display, size: T.size.screenTitle, weight: 700 });
      if (menu) {
        mk(g, { left: '32px', top: '24px', width: '72px', height: '72px', borderRadius: '50%', background: hexA('#FFFFFF', 0.08) });
        ico(g, 'more_vert', 68, 60, 36, '#fff');
      }
      return g;
    }

    // ================================================================= BOOT: the ftr mark draws itself
    const boot = layer();
    const bootSvg = svgEl2('svg', boot, { width: 640, height: 960, viewBox: '0 0 640 960' });
    const LS = 1.72, LX = 320 - LOGO.w * LS / 2, LY = 452 - LOGO.h * LS / 2;
    const bootG = svgEl2('g', bootSvg, { transform: `translate(${LX} ${LY}) scale(${LS})` });
    const bootStrokes = LOGO.strokes.map(d => svgEl2('path', bootG, { d, fill: 'none', stroke: LOGO.color, 'stroke-width': LOGO.stroke, 'stroke-linecap': 'butt' }));
    const bootLens = bootStrokes.map(p => p.getTotalLength());
    const bootTotal = bootLens.reduce((a, b) => a + b, 0);
    const bootPen = svgEl2('circle', bootSvg, { r: 3.2, fill: '#fff' });

    // ================================================================= HOME
    const home = layer();
    const homeClock = lab(home, C.time, 320, 176, { align: 'c', size: 112, weight: 300, ls: -1 });
    const homeDate = lab(home, C.date, 320, 258, { align: 'c', size: T.size.summary, alpha: 0.6 });
    const cell = i => { const r = Math.floor(i / 4), c = i % 4; return [32 + (3 - c) * 144, 336 + r * 178]; };
    const cellRect = i => { const [x, y] = cell(i); return [x + 6, y + 2, 132, 170, 32, 32, 32, 32]; };
    const homeCells = C.apps.map(([name, icon], i) => {
      const [x, y] = cell(i);
      const g = mk(home, { left: x + 'px', top: y + 'px', width: '144px', height: '178px', transformOrigin: '72px 70px' });
      ico(g, icon, 72, 66, 60, '#fff', { stroke: 1.6 });
      const l = lab(g, name, 72, 136, { align: 'c', size: 23, alpha: 0.86 });
      return { g, l, cx: x + 72, cy: y + 70 };
    });

    // ================================================================= LOCK
    const lock = layer();
    const lockIcon = ico(lock, 'lock', 320, 150, 44, '#fff', { alpha: 0.8 });
    const lockOpen = ico(lock, 'lock_open', 320, 150, 44, '#fff', { alpha: 0 });
    const lockTop = mk(lock, { left: 0, top: 0, width: '640px', height: '960px' });
    lab(lockTop, C.time, 320, 290, { align: 'c', size: 176, weight: 200, ls: -3 });
    lab(lockTop, C.date, 320, 402, { align: 'c', size: 30, weight: 500, alpha: 0.9 });
    const pinHint = lab(lock, 'הקלד קוד', 320, 640, { align: 'c', size: T.size.summary, alpha: 0.7 });
    const dots = [0, 1, 2, 3].map(i => {
      const x = 320 + (1.5 - i) * 56;   // first digit on the right
      const ring = mk(lock, { left: x - 13 + 'px', top: '707px', width: '26px', height: '26px', borderRadius: '50%', border: `2px solid ${hexA('#FFFFFF', 0.6)}`, boxSizing: 'border-box' });
      const fillD = mk(lock, { left: x - 13 + 'px', top: '707px', width: '26px', height: '26px', borderRadius: '50%', background: '#fff', transformOrigin: '13px 13px' });
      return { x, ring, fillD };
    });
    const faceWrap = mk(lock, { left: 0, top: 0, width: '640px', height: '960px' });
    const faceIcon = ico(faceWrap, 'face', 320, 540, 128, '#fff', { stroke: 1.3 });
    const scanLine = mk(faceWrap, { left: '236px', top: '476px', width: '168px', height: '3px', borderRadius: '2px', background: '#fff' });
    const faceText = lab(faceWrap, 'מחפש את הפנים שלך', 320, 700, { align: 'c', size: 28, alpha: 0.8 });
    const knownWrap = mk(faceWrap, { left: 0, top: 0, width: '640px', height: '960px' });
    ico(knownWrap, 'check', 256, 700, 36, '#fff', { stroke: 2 });
    lab(knownWrap, 'זוהית', 288, 700, { align: 'l', size: 30, weight: 500 });
    // the ring around the front camera, drawn as the scan runs
    const ringSvg = svgEl2('svg', lock, { width: 640, height: 960, viewBox: '0 0 640 960' });
    ringSvg.style.cssText = 'position:absolute;left:0;top:0;overflow:visible';
    const ringR = 44, ringC = 2 * Math.PI * ringR;
    const ringBase = svgEl2('circle', ringSvg, { cx: U.PUNCH[0], cy: U.PUNCH[1], r: ringR, fill: 'none', stroke: hexA('#FFFFFF', 0.18), 'stroke-width': 3 });
    const ringArc = svgEl2('circle', ringSvg, { cx: U.PUNCH[0], cy: U.PUNCH[1], r: ringR, fill: 'none', stroke: '#fff', 'stroke-width': 3,
      'stroke-linecap': 'round', transform: `rotate(-90 ${U.PUNCH[0]} ${U.PUNCH[1]})` });

    // ================================================================= MESSAGES (thread list)
    const msgs = layer();
    topBar(msgs, 'הודעות');
    const rowY = i => 132 + i * 154;
    C.threads.forEach(([name, last, unread], i) => {
      const y = rowY(i);
      lab(msgs, name, 584, y + 46, { size: T.size.title, weight: 500 });
      lab(msgs, last, 584, y + 88, { size: T.size.summary, alpha: 0.6 });
      if (unread) {
        const b = mk(msgs, { left: 56 + 'px', top: y + 47 + 'px', width: '36px', height: '36px', borderRadius: '18px', background: '#fff' });
        b.dataset.accent = 'bg';
        const n = lab(b, String(unread), 18, 18, { align: 'c', size: T.size.badge, weight: 700, color: '#000', dir: 'ltr' });
      }
    });
    const rowRect = i => [32, rowY(i), 576, 130, 40, 40, 40, 40];

    // ================================================================= CHAT (a thread, DS message-compose template)
    const chat = layer();
    topBar(chat, 'דני כהן', { back: true });
    const chatArea = mk(chat, { left: 0, top: '120px', width: '640px', height: '700px', overflow: 'hidden' });
    const bubble = (s, side, o = {}) => {
      const d = mk(chatArea, {
        maxWidth: '440px', padding: '22px 28px', fontFamily: T.font, fontSize: '30px', lineHeight: 1.35, direction: 'rtl',
        borderRadius: side === 'in' ? '40px 40px 12px 40px' : '40px 40px 40px 12px',
        background: side === 'in' ? T.surface : '#fff', color: side === 'in' ? '#fff' : '#000', whiteSpace: 'nowrap',
        top: 0, [side === 'in' ? 'right' : 'left']: '32px', transformOrigin: side === 'in' ? '100% 100%' : '0% 100%',
      });
      d.textContent = s; if (side === 'out') d.dataset.accent = 'bg';
      return d;
    };
    const bIn1 = bubble('אתה בדרך?', 'in');
    const bOut = bubble('יוצא עוד רגע', 'out');
    const sent = mk(chatArea, { right: 'auto', left: '38px', top: 0, fontFamily: T.font, fontSize: '22px', fontWeight: 500, color: '#fff', direction: 'rtl', display: 'flex', alignItems: 'center', gap: '10px' });
    { const s = document.createElement('span'); s.textContent = 'נשלח · ' + C.time; sent.appendChild(s);
      const ic = ico(sent, 'check', 0, 0, 26, '#fff', { stroke: 2 }); ic.style.position = 'relative'; ic.style.left = ic.style.top = '0'; }
    // the reply: text and a location inside one incoming bubble
    const bIn2 = mk(chatArea, { right: '32px', top: 0, width: '420px', borderRadius: '40px 40px 12px 40px', background: T.surface, overflow: 'hidden', transformOrigin: '100% 100%' });
    lab(bIn2, 'שלחתי לך את הכתובת', 392, 44, { size: 30 });
    const miniMap = svgEl2('svg', bIn2, { width: 380, height: 150, viewBox: '0 0 380 150' });
    miniMap.style.cssText = 'position:absolute;left:20px;top:84px;border-radius:24px;background:#000';
    for (const [x1, y1, x2, y2, w] of [[-10, 40, 390, 10, 5], [-10, 120, 390, 85, 5], [60, -10, 110, 160, 4], [200, -10, 240, 160, 7], [300, -10, 330, 160, 4], [-10, 150, 390, 60, 3]])
      svgEl2('line', miniMap, { x1, y1, x2, y2, stroke: hexA('#FFFFFF', 0.16), 'stroke-width': w, 'stroke-linecap': 'round' });
    ico(bIn2, 'location_on', 245, 146, 52, '#fff', { stroke: 1.8 });
    lab(bIn2, 'הרצל 12', 392, 268, { size: 30, weight: 500 });
    lab(bIn2, 'מיקום', 392, 310, { size: 24, alpha: 0.6 });
    bIn2.style.height = '340px';
    // compose bar
    const field = mk(chat, { left: '144px', top: '836px', width: '464px', height: '96px', borderRadius: '48px', background: T.surface, boxSizing: 'border-box', border: `2px solid ${hexA('#FFFFFF', 0.14)}` });
    ico(field, 'add', 464 - 56, 48, 40, '#fff', { alpha: 0.4 });
    ico(field, 'mic', 56, 48, 40, '#fff', { alpha: 0.4 });
    const draft = lab(field, 'יוצא עוד רגע', 464 - 96, 48, { size: 30 });
    const placeholder = lab(field, 'הודעה', 464 - 96, 48, { size: 30, alpha: 0.4 });
    const caret = mk(field, { left: 464 - 96 - 176 + 'px', top: '31px', width: '3px', height: '34px', borderRadius: '2px', background: '#fff' });
    caret.dataset.accent = 'bg';
    const typing = mk(chatArea, { right: '32px', top: 0, width: '118px', height: '70px', borderRadius: '40px 40px 12px 40px', background: T.surface, transformOrigin: '100% 100%' });
    const tdots = [0, 1, 2].map(i => mk(typing, { left: 30 + i * 22 + 'px', top: '29px', width: '13px', height: '13px', borderRadius: '7px', background: '#fff' }));
    const sendBtn = mk(chat, { left: '32px', top: '836px', width: '96px', height: '96px', borderRadius: '50%', background: '#fff' });
    sendBtn.dataset.accent = 'bg';
    ico(sendBtn, 'send', 48, 48, 46, '#000', { stroke: 1.8 });
    const sendRect = [24, 828, 112, 112, 56, 56, 56, 56];

    // ================================================================= NAVIGATION (map, route, the time to go)
    const nav = layer();
    const mapWrap = mk(nav, { left: 0, top: 0, width: '640px', height: '960px', transformOrigin: '320px 560px' });
    const map = svgEl2('svg', mapWrap, { width: 640, height: 960, viewBox: '0 0 640 960' });
    const th = -14 * Math.PI / 180, O = [300, 500];
    const G = (i, j) => [O[0] + i * 118 * Math.cos(th) - j * 132 * Math.sin(th), O[1] + i * 118 * Math.sin(th) + j * 132 * Math.cos(th)];
    for (let i = -5; i <= 5; i++) { const a = G(i, -8), b = G(i, 8); svgEl2('line', map, { x1: a[0], y1: a[1], x2: b[0], y2: b[1], stroke: hexA('#FFFFFF', i % 2 ? 0.08 : 0.12), 'stroke-width': i % 2 ? 5 : 8, 'stroke-linecap': 'round' }); }
    for (let j = -8; j <= 8; j++) { const a = G(-6, j), b = G(6, j); svgEl2('line', map, { x1: a[0], y1: a[1], x2: b[0], y2: b[1], stroke: hexA('#FFFFFF', j % 3 ? 0.08 : 0.13), 'stroke-width': j % 3 ? 5 : 9, 'stroke-linecap': 'round' }); }
    svgEl2('path', map, { d: 'M-40 900 C 160 760, 260 640, 700 180', fill: 'none', stroke: hexA('#FFFFFF', 0.14), 'stroke-width': 14, 'stroke-linecap': 'round' });
    svgEl2('path', map, { d: 'M-60 300 C 120 360, 380 180, 700 320', fill: 'none', stroke: hexA('#FFFFFF', 0.06), 'stroke-width': 46, 'stroke-linecap': 'round' });
    const rp = [G(0, 1.35), G(0, 0), G(1, 0), G(1, -2)];   // starts above the card, ends on הרצל
    const routeD = 'M' + rp.map(p => p.map(v => v.toFixed(1)).join(' ')).join('L');
    const route = svgEl2('path', map, { d: routeD, fill: 'none', stroke: '#fff', 'stroke-width': 11, 'stroke-linecap': 'round', 'stroke-linejoin': 'round' });
    route.dataset.accent = 'stroke';
    const routeLen = route.getTotalLength();
    const startDot = svgEl2('circle', map, { cx: rp[0][0], cy: rp[0][1], r: 13, fill: '#fff' });
    const startRing = svgEl2('circle', map, { cx: rp[0][0], cy: rp[0][1], r: 26, fill: hexA('#FFFFFF', 0.18) });
    const pinG = mk(mapWrap, { left: rp[3][0] - 34 + 'px', top: rp[3][1] - 62 + 'px', width: '68px', height: '68px', transformOrigin: '34px 62px' });
    ico(pinG, 'location_on', 34, 34, 68, '#fff', { stroke: 1.8 });
    const streetLabel = (s, p, ang) => { const l = lab(mapWrap, s, p[0], p[1], { align: 'c', size: 22, alpha: 0.45 }); l.style.transform += ` rotate(${ang}deg)`; return l; };
    streetLabel('הרצל', lerpR(G(1, -1), G(2, -1), 0.5).map((v, i) => v + (i ? -16 : 0)), -14);
    streetLabel('יפו', G(-2, 2.6), 76);
    const navCard = mk(nav, { left: '24px', top: '760px', width: '592px', height: '176px', borderRadius: '44px', background: T.surface });
    lab(navCard, '12 דק׳', 552, 58, { size: 56, weight: 700 });
    lab(navCard, '3.4 ק״מ · הרצל 12', 552, 118, { size: 28, alpha: 0.6 });
    const goBtn = mk(navCard, { left: '40px', top: '40px', width: '96px', height: '96px', borderRadius: '50%', background: '#fff' });
    goBtn.dataset.accent = 'bg';
    ico(goBtn, 'navigation', 48, 48, 44, '#000', { stroke: 1.8 });
    const goRect = [24 + 32, 760 + 32, 112, 112, 56, 56, 56, 56];

    // ================================================================= LIBRARY: בלכתך בדרך
    const lib = layer();
    topBar(lib, 'בלכתך בדרך');
    const cont = mk(lib, { left: '32px', top: '132px', width: '576px', height: '196px', borderRadius: '44px', background: T.surface });
    lab(cont, 'המשך קריאה', 536, 46, { size: T.size.summary, alpha: 0.6 });
    lab(cont, 'פרקי אבות', 536, 98, { size: 40, weight: 700, font: T.display });
    lab(cont, 'פרק א, משנה א', 536, 148, { size: T.size.summary, alpha: 0.6 });
    const contIcon = ico(cont, 'book_open', 92, 98, 72, '#fff', { stroke: 1.5 }); contIcon.dataset.accent = 'stroke';
    const contRect = [32, 132, 576, 196, 44, 44, 44, 44];
    lab(lib, 'הספרייה', 560, 380, { font: T.display, size: T.size.summary, weight: 700, alpha: 0.55, ls: 2 });
    const libCard = mk(lib, { left: '32px', top: '412px', width: '576px', height: `${4 * 108 + 12}px`, borderRadius: '44px', background: T.surface });
    [['תנ״ך', '24 ספרים', 'menu_book'], ['משנה', '63 מסכתות', 'auto_stories'], ['תלמוד בבלי', '37 מסכתות', 'library_books'], ['הלכה', 'משנה תורה, שולחן ערוך', 'book']].forEach(([title, sum, icon], i) => {
      const y = 6 + i * 108;
      if (i) mk(libCard, { left: '32px', top: y + 'px', width: '512px', height: '2px', background: hexA('#FFFFFF', 0.12) });
      const ic = ico(libCard, icon, 576 - 32 - 22, y + 54, 44, '#fff'); ic.dataset.accent = 'stroke';
      lab(libCard, title, 576 - 32 - 44 - 32, y + 38, { size: T.size.title, weight: 600 });
      lab(libCard, sum, 576 - 32 - 44 - 32, y + 76, { size: T.size.summary, alpha: 0.6 });
      ico(libCard, 'chevron_left', 32 + 18, y + 54, 36, '#fff', { alpha: 0.3 });
    });

    // ================================================================= READER: פרקי אבות, pages turned with the keys
    const reader = layer();
    topBar(reader, 'פרקי אבות', { back: true, menu: false });
    const pages = C.avotPages.map((ids, p) => {
      const pg = mk(reader, { left: '32px', top: '140px', width: '576px', height: '760px' });
      let y = 0;
      if (p === 0) { lab(pg, 'פרק ראשון', 288, 26, { align: 'c', size: 26, alpha: 0.55, font: T.display, weight: 700, ls: 2 }); y = 70; }
      for (const id of ids) {
        const [n, s] = C.avot[id];
        const b = mk(pg, { right: 0, top: y + 'px', width: '576px' });
        const tag = mk(b, { right: 0, top: '6px', width: '40px', height: '40px', borderRadius: '20px', background: hexA('#FFFFFF', 0.1), fontFamily: T.font, fontSize: '24px', fontWeight: 700, color: '#fff', textAlign: 'center', lineHeight: '40px' });
        tag.textContent = n;
        const para = mk(b, { right: '0', top: '0', width: '576px', fontFamily: T.font, fontSize: '33px', lineHeight: 1.62, color: hexA('#FFFFFF', 0.93), direction: 'rtl', textAlign: 'right', whiteSpace: 'normal', textIndent: '54px' });
        para.textContent = s;
        y += para.getBoundingClientRect().height + 26;
      }
      return pg;
    });
    const pageDots = [0, 1, 2].map(i => mk(reader, { left: 320 + (1 - i) * 24 - 6 + 'px', top: '922px', width: '12px', height: '12px', borderRadius: '6px', background: '#fff' }));

    // ================================================================= ASSISTANT: עוזרי
    const asst = layer();
    topBar(asst, 'עוזרי', { menu: false });
    const micG = mk(asst, { left: '220px', top: '230px', width: '200px', height: '200px', transformOrigin: '100px 100px' });
    mk(micG, { left: 0, top: 0, width: '200px', height: '200px', borderRadius: '50%', border: `3px solid ${hexA('#FFFFFF', 0.22)}`, boxSizing: 'border-box' });
    const micIc = ico(micG, 'mic', 100, 100, 88, '#fff', { stroke: 1.5 }); micIc.dataset.accent = 'stroke';
    const listen = lab(asst, 'מקשיב', 320, 470, { align: 'c', size: T.size.summary, alpha: 0.6 });
    const bars = Array.from({ length: 15 }, (_, i) => { const b = mk(asst, { left: 320 + (i - 7) * 22 - 5 + 'px', top: '530px', width: '10px', height: '10px', borderRadius: '5px', background: '#fff' }); b.dataset.accent = 'bg'; return b; });
    const words = C.assistant.split(' ');
    const wordsBox = mk(asst, { left: '50px', top: '600px', width: '540px', height: '150px', direction: 'rtl', textAlign: 'center', whiteSpace: 'normal', fontFamily: T.font, fontSize: '40px', fontWeight: 500, lineHeight: 1.35, color: '#fff' });
    const wordEls = words.map((w, i) => { const s = document.createElement('span'); s.textContent = w + (i < words.length - 1 ? ' ' : ''); s.style.display = 'inline-block'; s.style.whiteSpace = 'pre'; wordsBox.appendChild(s); return s; });
    const savedRow = mk(asst, { left: 0, top: '800px', width: '640px', height: '60px' });
    const svIc = ico(savedRow, 'check', 214, 30, 36, '#fff', { stroke: 2 }); svIc.dataset.accent = 'stroke';
    const svL = lab(savedRow, 'התזכורת נשמרה', 246, 30, { align: 'l', size: 28, weight: 500 });

    // ================================================================= SETTINGS: צבע הדגשה (two copies: the wave reveals the new accent)
    const setRowRect = i => [32, 144 + i * 110, 576, 108, i === 0 ? 44 : 0, i === 0 ? 44 : 0, i === 4 ? 44 : 0, i === 4 ? 44 : 0];
    function accentScreen() {
      const s = layer();
      topBar(s, 'צבע הדגשה', { menu: false });
      const card = mk(s, { left: '32px', top: '138px', width: '576px', height: `${5 * 110 + 12}px`, borderRadius: '44px', background: T.surface });
      const checks = [];
      T.accents.forEach((c, i) => {
        const y = 6 + i * 110;
        if (i) mk(card, { left: '32px', top: y - 1 + 'px', width: '512px', height: '2px', background: hexA('#FFFFFF', 0.12) });
        ico(card, 'circle', 576 - 32 - 22, y + 54, 44, c, { stroke: 2 });
        lab(card, T.accentNames[i], 576 - 32 - 44 - 32, y + 54, { size: T.size.title, weight: 600 });
        const ck = ico(card, 'check', 32 + 22, y + 54, 44, '#fff', { stroke: 2 }); ck.dataset.accent = 'stroke';
        checks.push(ck);
      });
      lab(s, 'תצוגה מקדימה', 560, 748, { font: T.display, size: T.size.summary, weight: 700, alpha: 0.55, ls: 2 });
      const card2 = mk(s, { left: '32px', top: '780px', width: '576px', height: '120px', borderRadius: '44px', background: T.surface });
      const vi = ico(card2, 'volume_up', 576 - 32 - 22, 60, 44, '#fff'); vi.dataset.accent = 'stroke';
      lab(card2, 'צלילי מקשים', 576 - 32 - 44 - 32, 42, { size: T.size.title, weight: 600 });
      lab(card2, 'מופעל', 576 - 32 - 44 - 32, 82, { size: T.size.summary, alpha: 0.6 });
      const sw = mk(card2, { left: '40px', top: '30px', width: '104px', height: '60px', borderRadius: '30px', background: '#fff', boxSizing: 'border-box' });
      sw.dataset.accent = 'bg';
      mk(sw, { left: '12px', top: '12px', width: '36px', height: '36px', borderRadius: '18px', background: '#000' });
      return { s, checks };
    }
    const accA = accentScreen(), accB = accentScreen();
    // recolour everything marked data-accent (idempotent: the result depends only on the colour)
    const paint = (layerEl, color) => {
      if (layerEl._painted === color) return;
      layerEl._painted = color;
      layerEl.querySelectorAll('[data-accent]').forEach(e => {
        const tag = e.tagName.toLowerCase();
        if (e.dataset.accent === 'bg') e.style.background = color;
        else if (tag === 'svg') e.querySelectorAll('path,circle').forEach(p => {
          if (p.getAttribute('stroke') && p.getAttribute('stroke') !== 'none') p.setAttribute('stroke', color);
          if (tag === 'svg' && p.tagName.toLowerCase() === 'circle' && p.getAttribute('fill') !== 'none') p.setAttribute('fill', color);
        });
        else e.setAttribute('stroke', color);
      });
    };

    // ================================================================= the focus frame and the camera hole
    const frame = mk(root, { left: 0, top: 0, boxSizing: 'border-box', borderStyle: 'solid' });
    const punch = mk(root, { left: U.PUNCH[0] - U.PUNCH_R + 'px', top: U.PUNCH[1] - U.PUNCH_R + 'px', width: 2 * U.PUNCH_R + 'px', height: 2 * U.PUNCH_R + 'px', borderRadius: '50%', background: '#000', boxShadow: '0 0 0 2px #0a0a0a' });

    // ---------------------------------------------------------------- the frame's path through the film
    // styles: [border px, background alpha, background is text colour (1) or accent (0)]
    const ROW = [3, 0.14, 0], CARD = [4, 0.06, 1], RING = [4, 0, 0], NONE = [0, 0, 0];
    const FK = [], SK = [];
    const move = (t, r, st, wn = 24) => { FK.push([t, r, wn, 1]); if (st) SK.push([t, st, 30, 1]); };
    const open = (t, r, st) => { FK.push([t, FULL, 16, 1]); SK.push([t, NONE, 34, 1]); FK.push([t + 0.27, r, 20, 1]); SK.push([t + 0.27, st, 22, 1]); };
    move(TL.okBoot, cellRect(0), ROW, 15);
    const H = TL.homeMoves, path = [1, 2, 6, 5, 9, 5, 1];
    H.forEach(([t], i) => move(t, cellRect(path[i])));
    open(TL.openMsgs, rowRect(0), ROW);
    open(TL.openChat, sendRect, RING);
    move(TL.focusLoc, null, CARD);
    open(TL.openNav, goRect, RING);
    move(TL.backNav, cellRect(4), ROW, 18); move(TL.toLib, cellRect(5));
    open(TL.openLib, contRect, CARD);
    open(TL.openReader, FULL, NONE);
    move(TL.backAsst, cellRect(6), ROW, 18); move(TL.toSettings, cellRect(7));
    open(TL.openSettings, setRowRect(0), CARD);
    TL.colors.forEach(([d], i) => move(d, setRowRect(i + 1)));
    move(TL.backSettings, cellRect(7), ROW, 18);
    // ---------------------------------------------------------------- chat layout over time (a stack aligned to the bottom)
    // bubble 85 tall, status 34, the reply 340; gaps 8 (bubble to its status), 20 (between bubbles), 24 (above the reply)
    const chatStack = t => {
      const bottom = 690;   // inside chatArea (700 tall)
      const sentK = E(t, TL.send + 0.06, 20), replyK = E(t, TL.reply, 18);
      const hOut = (85 + 8 + 34) * sentK, hRep = (340 + 24) * replyK;
      const yRep = bottom - 340 + (1 - replyK) * 30;
      const ySent = bottom - hRep - 34;
      const yOut = ySent - 8 - 85;
      const yIn1 = bottom - hRep - hOut - 20 * sentK - 85;
      return { yIn1, yOut, ySent, yRep };
    };
    // the location bubble in screen coordinates, once the reply has settled
    const LOC = [640 - 32 - 420 - 6, 120 + chatStack(1e6).yRep - 6, 432, 352, 46, 46, 18, 46];
    const f0 = [320, 470, 0, 0, 30, 30, 30, 30];
    const frameTrack = track(f0, FK.map(k => [k[0], k[1] || LOC, k[2], k[3]]));
    const styleTrack = track(NONE, SK.map(k => [k[0], k[1], k[2], k[3]]));

    return {
      root, FULL,
      accentAt,
      update(t, fx) {
        const acc = accentAt(t);
        // ---------------- which screen shows
        const on = (w, a, b) => t >= a && t < b;
        const all = { boot, home, lock, msgs, chat, nav, lib, reader, asst, accA: accA.s, accB: accB.s };
        const st = {};
        for (const k in all) st[k] = { o: 0 };
        // boot
        if (t >= TL.screenOn && t < TL.okBoot + 0.6) st.boot = { o: 1 - E(t, TL.okBoot, 20), s: 1 - 0.06 * E(t, TL.okBoot, 20) };
        // home: several visits
        const homeWin = [[TL.okBoot, TL.lockEnd, 'boot', 'lock'], [TL.unlock, TL.openMsgs, 'unlock', 'open'], [TL.backNav, TL.openLib, 'back', 'open'],
          [TL.backAsst, TL.openSettings, 'back', 'open'], [TL.backSettings, 1e9, 'back', 'none']];
        const openOut = (t0) => ({ o: 1 - E(t, t0, 26), blur: 8 * E(t, t0, 26) });
        const backIn = (t0) => ({ o: E(t, t0 + 0.06, 18), s: 1.04 - 0.04 * E(t, t0 + 0.06, 18) });
        const backOut = (t0) => ({ o: 1 - E(t, t0, 22), s: 1 - 0.08 * E(t, t0, 22) });
        const openIn = (t0, from) => ({ o: 1, clip: clipOf(lerpR(from, FULL, E(t, t0, 16))), done: E(t, t0, 16) > 0.995 });
        for (const [a, b, kin, kout] of homeWin) {
          if (t < a || t >= b + 0.6) continue;
          let s = { o: 1 };
          if (kin === 'back') s = backIn(a);
          if (kin === 'unlock') s = { o: E(t, a + 0.1, 16), s: 1.05 - 0.05 * E(t, a + 0.1, 16) };
          if (t >= b) s = kout === 'open' ? openOut(b) : kout === 'lock' ? { o: 1 - E(t, b, 30) } : s;
          st.home = s;
        }
        // lock screen
        if (t >= TL.lockEnd + 0.3 && t < TL.unlock + 0.6) st.lock = t < TL.unlock ? { o: E(t, TL.lockEnd + 0.36, 16) } : { o: 1 - E(t, TL.unlock, 18), y: -60 * E(t, TL.unlock, 18) };
        // app screens opened from the frame
        const seq = [
          ['msgs', TL.openMsgs, cellRect(1), TL.openChat, 'open'],
          ['chat', TL.openChat, rowRect(0), TL.openNav, 'open'],
          ['nav', TL.openNav, LOC, TL.backNav, 'back'],
          ['lib', TL.openLib, cellRect(5), TL.openReader, 'open'],
          ['reader', TL.openReader, contRect, TL.asst[1] + 0.05, 'asst'],
          ['accA', TL.openSettings, cellRect(7), TL.backSettings, 'back'],
          ['accB', TL.openSettings, cellRect(7), TL.backSettings, 'back'],
        ];
        for (const [k, a, from, b, out] of seq) {
          if (t < a || t >= b + 0.6) continue;
          let s = openIn(a, from);
          if (t >= b) s = out === 'open' ? openOut(b) : out === 'back' ? backOut(b) : { o: 1 - E(t, b, 24), s: 1 - 0.04 * E(t, b, 24), blur: 6 * E(t, b, 24) };
          st[k] = s;
        }
        // assistant: grows out of the centre after the double press
        const ta = TL.asst[1] + 0.05;
        if (t >= ta && t < TL.backAsst + 0.6) st.asst = t < TL.backAsst ? { o: E(t, ta + 0.05, 18), s: 0.94 + 0.06 * E(t, ta + 0.05, 16) } : backOut(TL.backAsst);
        // apply
        for (const k in all) {
          const s = st[k], el = all[k];
          if (!s || s.o <= 0.002) { el.style.visibility = 'hidden'; continue; }
          el.style.visibility = 'inherit';
          el.style.opacity = s.o >= 0.999 ? '1' : s.o.toFixed(3);
          el.style.transform = (s.s && Math.abs(s.s - 1) > 1e-4) || s.y ? `translateY(${(s.y || 0).toFixed(2)}px) scale(${(s.s || 1).toFixed(4)})` : 'none';
          el.style.filter = s.blur > 0.05 ? `blur(${s.blur.toFixed(2)}px)` : 'none';
          el.style.clipPath = s.clip && !s.done ? s.clip : 'none';
        }

        // ---------------- boot: the logo writes itself, a small white pen leads the red line
        if (st.boot.o > 0.002) {
          const k = E_s(seg(t, TL.logo[0], TL.logo[1]));
          let left = k * bootTotal, penAt = null;
          bootStrokes.forEach((p, i) => {
            const L = bootLens[i], d = clamp(left, 0, L); left -= L;
            p.setAttribute('stroke-dasharray', `${d.toFixed(2)} ${L + 1}`);
            if (d > 0 && d < L) penAt = [p, d];
          });
          if (penAt && k < 1) {
            const pt = penAt[0].getPointAtLength(penAt[1]);
            bootPen.setAttribute('cx', (LX + pt.x * LS).toFixed(2)); bootPen.setAttribute('cy', (LY + pt.y * LS).toFixed(2)); bootPen.style.opacity = 1;
          } else bootPen.style.opacity = 0;
        }
        // ---------------- home: assembles around the first frame, then rests
        if (st.home.o > 0.002) {
          homeCells.forEach((c, i) => {
            const d = Math.hypot(c.cx - 320, c.cy - 470) / 700;
            const k = t < TL.okBoot + 3 ? E(t, TL.okBoot + 0.06 + d * 0.28, 16) : 1;
            c.g.style.opacity = k.toFixed(3); c.g.style.transform = `scale(${(0.86 + 0.14 * k).toFixed(4)})`;
          });
          const kc = t < TL.okBoot + 3 ? E(t, TL.okBoot + 0.02, 14) : 1;
          homeClock.style.opacity = homeDate.style.opacity = kc.toFixed(3);
          homeDate.style.opacity = (0.6 * kc).toFixed(3);
        }
        // ---------------- lock: dots fill, merge into the ring round the camera, the face scan, recognised
        if (st.lock.o > 0.002) {
          TL.pin.forEach(([tp], i) => {
            // a dot fills on its key; after the fourth, all four fly up into the ring round the front camera
            const d = dots[i], k = E(t, tp + 0.02, 30), m = E(t, TL.merge + i * 0.03, 13);
            d.fillD.style.transform = `translate(${((U.PUNCH[0] - d.x) * m).toFixed(2)}px, ${((U.PUNCH[1] - 720) * m).toFixed(2)}px) scale(${(k * (1 - 0.55 * m)).toFixed(4)})`;
            d.fillD.style.opacity = k > 0.01 ? (1 - E(t, TL.merge + 0.22, 20)).toFixed(3) : '0';
            d.ring.style.opacity = (1 - E(t, TL.merge - 0.05, 22)).toFixed(3);
          });
          pinHint.style.opacity = (0.7 * (1 - E(t, TL.merge, 18))).toFixed(3);
          const fin = E(t, TL.merge + 0.12, 16), known = E(t, TL.known, 20);
          faceWrap.style.opacity = fin.toFixed(3);
          const sc = seg(t, TL.scan[0], TL.scan[1]);
          const scanY = 476 + 128 * (0.5 - 0.5 * Math.cos(sc * Math.PI * 2 * 1.0));
          scanLine.style.transform = `translateY(${(scanY - 476).toFixed(2)}px)`;
          scanLine.style.opacity = (0.55 * (sc > 0 && sc < 1 ? 1 : 0) * (1 - known)).toFixed(3);
          faceText.style.opacity = (0.8 * (1 - E(t, TL.known - 0.1, 34))).toFixed(3);
          knownWrap.style.opacity = known.toFixed(3);
          faceIcon.style.opacity = (0.85 - 0.25 * known).toFixed(3);
          const arc = E_s(seg(t, TL.scan[0], TL.scan[1] + 0.05));
          ringArc.setAttribute('stroke-dasharray', `${(arc * ringC).toFixed(2)} ${ringC + 1}`);
          ringSvg.style.opacity = (fin * (1 - E(t, TL.unlock, 22))).toFixed(3);
          lockIcon.style.opacity = (0.8 * (1 - known)).toFixed(3);
          lockOpen.style.opacity = (0.8 * known).toFixed(3);
          lockTop.style.opacity = '1';
        }
        // ---------------- chat: send, the reply arrives
        if (st.chat.o > 0.002) {
          const cs = chatStack(t);
          const sK = E(t, TL.send, 22), rK = E(t, TL.reply, 18);
          bIn1.style.transform = `translateY(${(cs.yIn1).toFixed(2)}px)`;
          bOut.style.transform = `translateY(${(cs.yOut).toFixed(2)}px) scale(${(0.6 + 0.4 * sK).toFixed(4)})`;
          bOut.style.opacity = sK > 0.01 ? Math.min(1, sK * 1.6).toFixed(3) : '0';
          sent.style.transform = `translateY(${cs.ySent.toFixed(2)}px)`; sent.style.opacity = E(t, TL.send + 0.3, 20).toFixed(3);
          bIn2.style.transform = `translateY(${cs.yRep.toFixed(2)}px) scale(${(0.85 + 0.15 * rK).toFixed(4)})`; bIn2.style.opacity = rK.toFixed(3);
          const dK = E(t, TL.send, 30);
          draft.style.opacity = (1 - dK).toFixed(3); draft.style.transform = `translate(-100%, -50%) translateX(${(-80 * dK).toFixed(2)}px)`;
          placeholder.style.opacity = (0.4 * E(t, TL.send + 0.12, 20)).toFixed(3);
          caret.style.opacity = t < TL.send && Math.floor((t - TL.openChat) / 0.5) % 2 === 0 ? '1' : '0';   // the field's caret blinks until the send
          const ty = E(t, TL.send + 0.45, 20) * (1 - E(t, TL.reply - 0.06, 30));
          typing.style.opacity = ty.toFixed(3);
          typing.style.transform = `translateY(${(cs.yRep + 340 - 70 - 20 * 0).toFixed(2)}px) scale(${(0.85 + 0.15 * ty).toFixed(4)})`;
          tdots.forEach((d, i) => { d.style.opacity = (0.35 + 0.65 * Math.max(0, Math.sin((t * 5.2 - i * 0.55) * Math.PI))).toFixed(3); });
        }
        // ---------------- navigation: the route stretches, the card rises
        if (st.nav.o > 0.002) {
          const k = E_s(seg(t, TL.route[0], TL.route[1]));
          route.setAttribute('stroke-dasharray', `${(k * routeLen).toFixed(2)} ${routeLen + 1}`);
          const pk = E(t, TL.route[1] - 0.1, 22);
          pinG.style.opacity = pk.toFixed(3); pinG.style.transform = `translateY(${(-18 * (1 - pk)).toFixed(2)}px)`;
          const ck = E(t, TL.navCard, 16);
          navCard.style.opacity = ck.toFixed(3); navCard.style.transform = `translateY(${(60 * (1 - ck)).toFixed(2)}px)`;
          const u = t - TL.openNav;   // a slow, constant pan: the map is never still
          mapWrap.style.transform = `translate(${(-7 * u).toFixed(2)}px, ${(9 * u).toFixed(2)}px) scale(${(1.1 - 0.1 * E(t, TL.openNav, 5) + 0.006 * u).toFixed(4)})`;
          const pulse = (t * 1.3) % 1;
          startRing.setAttribute('r', (16 + 22 * pulse).toFixed(2)); startRing.setAttribute('fill-opacity', (0.35 * (1 - pulse)).toFixed(3));
        }
        // ---------------- reader: pages turn; the next page comes in from the left (RTL forward)
        if (st.reader.o > 0.002) {
          const [p1, p2] = TL.pages;
          const idx = t >= p2 ? 2 : t >= p1 ? 1 : 0;
          pages.forEach((pg, i) => {
            let o = 0, x = 0;
            const tin = i === 0 ? -1 : TL.pages[i - 1], tout = i < 2 ? TL.pages[i] : 1e9;
            if (t >= tin && t < tout + 0.4) {
              const ki = i === 0 ? 1 : E(t, tin + 0.04, 20), ko = t >= tout ? E(t, tout, 26) : 0;
              o = ki * (1 - ko); x = -70 * (1 - ki) + 90 * ko;
            }
            pg.style.opacity = o.toFixed(3); pg.style.transform = `translateX(${x.toFixed(2)}px)`;
            pg.style.visibility = o > 0.002 ? 'inherit' : 'hidden';
          });
          pageDots.forEach((d, i) => { d.style.opacity = i === idx ? '1' : '0.25'; });
        }
        // ---------------- assistant: the wave listens, the words land one by one, then saved
        if (st.asst.o > 0.002) {
          const tw = TL.words, n = wordEls.length;
          const lsn = E(t, TL.asst[1] + 0.2, 10) * (1 - E(t, TL.saved - 0.1, 10));
          bars.forEach((b, i) => {
            const env = 0.35 + 0.65 * Math.abs(vnoise(t * 7.5 + i * 0.37, 5)) * (0.6 + 0.4 * Math.sin(t * 9 + i));
            const h = 10 + 96 * lsn * env * (1 - Math.abs(i - 7) / 10);
            b.style.height = h.toFixed(2) + 'px'; b.style.top = (560 - h / 2).toFixed(2) + 'px';
          });
          listen.style.opacity = (0.6 * lsn).toFixed(3);
          wordEls.forEach((w, i) => {
            const t0 = tw[0] + (tw[1] - tw[0]) * i / Math.max(1, n - 1);
            const r = rise(t, t0, 16);
            w.style.opacity = r.o.toFixed(3); w.style.transform = `translateY(${r.y.toFixed(2)}px)`;
          });
          const sk = E(t, TL.saved, 18);
          savedRow.style.opacity = sk.toFixed(3); savedRow.style.transform = `translateY(${(14 * (1 - sk)).toFixed(2)}px)`;
          micG.style.transform = `scale(${(1 + 0.04 * lsn * Math.sin(t * 6)).toFixed(4)})`;
        }
        // ---------------- settings: the wave of the new accent, from the focus frame outwards
        if (st.accA.o > 0.002) {
          // i colours picked so far. A shows the one before, B the current one inside a circle that grows from the
          // selected row (the wave); when the circle has covered the screen, both show the current colour.
          const i = accentIdx(t), cur = T.accents[i];
          const waveT = i > 0 ? TL.colors[i - 1][1] : null;
          paint(accA.s, i > 0 ? T.accents[i - 1] : cur); paint(accB.s, cur);
          accA.checks.forEach((c, j) => c.style.opacity = j === Math.max(0, i - 1) ? '1' : '0');
          accB.checks.forEach((c, j) => c.style.opacity = j === i ? '1' : '0');
          if (waveT != null) {
            const r = [...setRowRect(i)], cx = r[0] + r[2] / 2, cy = r[1] + r[3] / 2;
            const rad = 1150 * E(t, waveT, 7.5);
            accB.s.style.clipPath = rad < 1100 ? `circle(${rad.toFixed(1)}px at ${cx}px ${cy}px)` : 'none';
          } else {
            accB.s.style.clipPath = 'none';
          }
        }

        // ---------------- the frame
        const fr = frameTrack(t), sy = styleTrack(t);
        const fVis = t < TL.okBoot ? 0 : (t >= TL.lockEnd && t < TL.unlock + 0.15) ? 0 : 1;
        const fIn = t < TL.okBoot + 1 ? E(t, TL.okBoot, 20) : 1;
        const fOut = (t >= TL.lockEnd && t < TL.lockEnd + 0.3) ? 1 - E(t, TL.lockEnd, 30) : 1;
        const fBack = t >= TL.unlock && t < TL.unlock + 1 ? E(t, TL.unlock + 0.15, 18) : 1;
        let fo = (t >= TL.lockEnd && t < TL.unlock + 0.15) ? 0 : fIn;
        if (t >= TL.lockEnd && t < TL.lockEnd + 0.3) fo = fOut;
        if (t >= TL.unlock + 0.15 && t < TL.unlock + 1) fo = fBack;
        // on the accent screen the focused row previews its own colour (DS: each row sets its own accent)
        let fc = acc;
        if (t >= TL.openSettings && t < TL.backSettings) {
          const row = TL.colors.reduce((n, [d], i) => t >= d ? i + 1 : n, 0);
          fc = T.accents[row];
        }
        const bw = Math.max(0, sy[0]), bgA = Math.max(0, sy[1]);
        if (fo <= 0.002 || fr[2] < 1) { frame.style.visibility = 'hidden'; }
        else {
          frame.style.visibility = 'inherit';
          frame.style.left = fr[0].toFixed(2) + 'px'; frame.style.top = fr[1].toFixed(2) + 'px';
          frame.style.width = fr[2].toFixed(2) + 'px'; frame.style.height = fr[3].toFixed(2) + 'px';
          frame.style.borderRadius = fr.slice(4).map(v => Math.max(0, v).toFixed(1) + 'px').join(' ');
          frame.style.borderWidth = bw.toFixed(2) + 'px';
          frame.style.borderColor = fc;
          frame.style.background = sy[2] > 0.5 ? hexA('#FFFFFF', bgA) : hexA(fc.startsWith('#') ? fc : '#FFFFFF', bgA);
          frame.style.opacity = fo.toFixed(3);
        }
        // everything that is accent coloured outside the settings screen follows the current accent
        if (t < TL.openSettings || t >= TL.backSettings) for (const k of ['msgs', 'chat', 'nav', 'lib', 'asst']) if (st[k].o > 0) paint(all[k], acc);
        return { frameRect: fr, frameOpacity: fo };
      },
    };
  };
})();
