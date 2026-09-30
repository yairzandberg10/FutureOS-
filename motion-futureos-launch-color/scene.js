// scene.js: the FutureOS launch film. One continuous shot: the rig (phone pose + camera) is a set of springs over the
// timeline in fos-timeline.js; the 3D body, the screen, the lines, the hand and the captions all follow it.
(() => {
  'use strict';
  const TL = FOS.TL, D = FOS.D;
  const sm = x => x <= 0 ? 0 : x >= 1 ? 1 : x * x * (3 - 2 * x);
  const E = (t, t0, wn = 18) => S(t - t0, wn, 1);

  scene('main', 0, DUR, { bg: '#000' }, (root) => {
    const T = FOS.T, U = FOS.U, A = T.accents;
    // ---------------------------------------------------------------- layers
    const floodWrap = div(root, { left: 0, top: 0, width: W + 'px', height: H + 'px' });   // behind the phone: scene 8's colour
    const ph = FOS.buildPhone(root, W, H);
    const uiHost = div(root, { left: 0, top: 0, width: W + 'px', height: H + 'px' });
    const ui = FOS.buildUI(uiHost);
    const ov = FOS.buildOverlay(root, ph);
    const capG = div(root, { left: 0, top: 0, width: W + 'px', height: H + 'px' });

    // ---------------------------------------------------------------- the rig: pose [x, y, z, yaw, pitch, roll], camera [dist, tx, ty]
    const K = (t, v, wn) => [t, v, wn, 1];
    const pose = track([0, 0, 0, -4, -1.5, 0], [
      K(0.2, [0, 0, 0, 3, 1, 0], 0.55),
      K(TL.turn34, [0, 0, 0, -30, 7, 0], 3.4),
      K(TL.turnSide, [-4, 0, 0, -83, 5, 0], 3.4),
      K(9.2, [null, null, null, -93, 7, null], 0.9),
      K(TL.turnBack, [0, -34, 0, -14, -11, 5], 3.2),
      K(TL.raise, [0, 0, 0, -7, 5, 0], 3.6),
      K(TL.zoom, [0, 0, 0, -6, -2, 0], 3.2),
      K(31, [null, null, null, -3.5, -1, null], 1.2),
      K(36, [null, null, null, -7, -3, null], 1.2),
      K(41, [null, null, null, -4, -1.5, null], 1.2),
      K(46, [null, null, null, -6.5, -2.5, null], 1.2),
      K(TL.pullOut, [0, 0, 0, -16, 4, 0], 3.0),
      K(51.6, [null, null, null, -6, 2, -1.5], 0.55),
      K(55.6, [null, null, null, -9, 4, 0], 0.8),
      K(TL.spin, [0, 0, 0, -196, 5, 0], 1.7),
      K(TL.backHold, [null, null, null, -204, 7, null], 1.1),
    ]);
    // 9:16 (Instagram): the captions own the band at the top (under Instagram's own 250 px), so the phone sits in the
    // frame below it: a positive ty looks higher and lowers the phone. At 1920 px and 18 degrees, 1 mm at distance d
    // is 1920 / (0.3168 d) px.
    const cam = track([780, 0, 29.5], [
      K(0.1, [746, 0, 29.5], 0.9),          // the drawing: the whole phone under the caption, the 53 just above it
      K(TL.turnSide, [770, 0, 30], 3),
      K(9.2, [700, 0, 29], 1.1),            // the profile, tall in the frame, the 8 above it
      K(TL.turnBack, [720, 0, 12], 3),
      K(TL.raise, [560, 0, 14], 3.4),
      K(TL.screenOn, [520, 0, 20], 1.4),
      K(TL.zoom, [415, 0, 42.6], 3.6),      // the screen 1080 px tall under the caption, the D-pad at the bottom edge
      K(TL.pullBack, [700, 0, 29], 4.2),    // the lock: the whole phone, the keys for the code
      K(TL.merge, [450, 0, 45], 2.6),       // up to the screen: the dots fly into the ring round the camera
      K(TL.backIn, [415, 0, 42.6], 4.2),
      K(TL.pullOut, [800, -26.4, 25], 3.2), // the phone on the right, the struck-out list on the left
      K(55.5, [760, -25.1, 24], 1.0),
      K(TL.spin, [720, 0, 6], 1.5),         // centred for the turn, the captions are gone
      K(TL.backHold, [660, 0, 6], 1.3),
    ]);

    // ---------------------------------------------------------------- key presses on the body
    const DIR = { left: [-1, 0], right: [1, 0], up: [0, 1], down: [0, -1] };
    const pulse = (t, tp) => clamp(S(t - tp, 45, 1) - S(t - tp - 0.12, 30, 1), 0, 1);
    function keyState(t) {
      const depth = {}; let tilt = [0, 0];
      for (const [tp, key] of TL.presses) {
        if (t < tp - 0.01 || t > tp + 0.6) continue;
        const k = pulse(t, tp);
        if (DIR[key]) { tilt = [tilt[0] + DIR[key][0] * k, tilt[1] + DIR[key][1] * k]; depth.dpad = Math.max(depth.dpad || 0, 0.22 * k); }
        else depth[key] = Math.max(depth[key] || 0, 0.7 * k);
      }
      return { depth, tilt };
    }

    // ---------------------------------------------------------------- the screen: DOM mapped onto the glass (homography)
    function homography(src, dst) {
      const A = [], b = [];
      for (let i = 0; i < 4; i++) {
        const [u, v] = src[i], [x, y] = dst[i];
        A.push([u, v, 1, 0, 0, 0, -u * x, -v * x]); b.push(x);
        A.push([0, 0, 0, u, v, 1, -u * y, -v * y]); b.push(y);
      }
      for (let c = 0; c < 8; c++) {   // Gaussian elimination with partial pivoting
        let p = c; for (let r = c + 1; r < 8; r++) if (Math.abs(A[r][c]) > Math.abs(A[p][c])) p = r;
        [A[c], A[p]] = [A[p], A[c]]; [b[c], b[p]] = [b[p], b[c]];
        for (let r = c + 1; r < 8; r++) { const f = A[r][c] / A[c][c]; for (let k = c; k < 8; k++) A[r][k] -= f * A[c][k]; b[r] -= f * b[c]; }
      }
      const x = new Array(8);
      for (let r = 7; r >= 0; r--) { let s = b[r]; for (let k = r + 1; k < 8; k++) s -= A[r][k] * x[k]; x[r] = s / A[r][r]; }
      const [a, c, e, bb, d, f, g, h] = x;   // x' = (a u + c v + e) / w, y' = (bb u + d v + f) / w, w = g u + h v + 1
      return `matrix3d(${[a, bb, 0, g, c, d, 0, h, 0, 0, 1, 0, e, f, 0, 1].map(v => +v.toFixed(8)).join(',')})`;
    }
    const UIC = [[0, 0], [640, 0], [640, 960], [0, 960]];
    const SCRZ = 0.03;

    // ---------------------------------------------------------------- the colour flood (scene 8)
    // every pick on the accent screen sends its colour out of the phone and over the whole frame, one wave over the
    // other; the back key takes the colour home into the screen.
    const FLOODS = TL.colors.map(([, ok], k) => {
      const row = T.accent0 + k + 1;
      return { ok, row, el: div(floodWrap, { left: 0, top: 0, width: W + 'px', height: H + 'px', background: A[row] }) };
    });
    const RMAX = Math.hypot(W, H) * 1.15;
    const onScreen = (u, v) => ph.project(...U.toMM(u, v), SCRZ);
    const floodAt = (f, t) => { const [u, v] = ui.rowCenter(f.row), p = onScreen(u, v); return [p[0], p[1], RMAX * E(t, f.ok + 0.08, 5.2)]; };
    const collapseAt = t => { const p = onScreen(320, 480); return [p[0], p[1], RMAX * (1 - E(t, TL.backSettings, 6.5))]; };
    const circle = ([x, y, r]) => `circle(${Math.max(0, r).toFixed(1)}px at ${x.toFixed(1)}px ${y.toFixed(1)}px)`;
    const outside = ([x, y, r]) => {   // the frame minus a circle
      r = Math.max(0.01, r); const f = v => v.toFixed(1);
      return `path(evenodd, "M-10 -10H${W + 10}V${H + 10}H-10Z M${f(x - r)} ${f(y)}a${f(r)} ${f(r)} 0 1 0 ${f(2 * r)} 0a${f(r)} ${f(r)} 0 1 0 ${f(-2 * r)} 0Z")`;
    };

    // ---------------------------------------------------------------- captions (the top band, centred over the phone)
    // colour copy: the second line of each caption takes the colour of its scene
    const CX = W / 2, CAPS = [];
    function caption(lines, tIns, tOut, o = {}) {
      const y0 = lines.length === 1 ? [376] : [318, 434];
      lines.forEach((s, i) => {
        const g = txt(o.parent || capG, s, { x: CX, y: y0[i], size: 96, font: F.HEEBO, weight: 700, align: 'c', color: (o.colors || [])[i] || '#FFFFFF' });
        if (o.ghost) g.inner.className = 'txb';   // a colour twin of a line that is already there: the check reads the first one
        CAPS.push({ g, tIn: tIns[i], tOut });
      });
    }
    caption(['מה אם הטלפון', 'יחזור למקשים?'], [TL.cap1[0], TL.cap1[0] + 0.32], TL.cap1[1], { colors: [null, A[1]] });
    caption(['8 מ״מ.'], [TL.cap2[0]], TL.cap2[1], { colors: [A[T.accent0]] });   // the colour of the measurement line
    caption(['בלי מגע.', 'מיקוד מלא.'], [TL.cap4[0], TL.cap4[0] + 0.6], TL.cap4[1], { colors: [null, A[1]] });
    caption(['שלך.', 'רק שלך.'], [TL.cap5[0], TL.cap5[0] + 0.6], TL.cap5[1], { colors: [null, A[3]] });
    caption(['מחובר.', 'למה שחשוב.'], [TL.cap6[0], TL.cap6[0] + 0.6], TL.cap6[1], { colors: [null, A[2]] });
    caption(['ספרייה שלמה.', 'עוזר שמקשיב.'], [TL.cap7[0], TL.cap7b], TL.cap7[1], { colors: [null, A[4]] });
    // scene 8: white on black, black on the flood, each cut to its side of the wave
    const cap8W = div(capG, { left: 0, top: 0, width: W + 'px', height: H + 'px' });
    const cap8B = div(capG, { left: 0, top: 0, width: W + 'px', height: H + 'px' });
    caption(['בצבע שלך.'], [TL.cap8[0]], TL.cap8[1], { parent: cap8W });
    caption(['בצבע שלך.'], [TL.cap8[0]], TL.cap8[1], { parent: cap8B, colors: ['#000000'], ghost: true });
    caption(['כל מה שצריך.', 'שום דבר שלא.'], [TL.cap9[0], TL.cap9b], TL.cap9[1], { colors: [null, A[4]] });
    // labels under the struck-out icons (the icons' positions come from fos-overlay.js)
    const FBL = ['דפדפן', 'חנות אפליקציות', 'רשתות חברתיות', 'וידאו']
      .map((s, i) => ({ g: txt(capG, s, { x: FOS.FB_POS[i][0], y: FOS.FB_POS[i][1] + 122, size: 50, font: F.HEEBO, weight: 500, align: 'c', color: '#FFFFFF' }), i }));
    // the closing lockup: FutureOS letter by letter (Latin: left to right), the slogan word by word (Hebrew: right to left)
    const word = letters(capG, 'FutureOS', { x: W / 2, y: 1010, size: 150, font: F.HEEBO, weight: 700, align: 'c', dir: 'ltr', ls: -0.015, color: '#FFFFFF' });
    const WORDC = [A[1], A[2], A[3], A[4]];   // each letter lands in a system colour and cools to white
    const slogan = row(capG, ['כשר.', 'בלי', 'לרדת', 'ברמה.'], { x: W / 2, y: 1150, size: 68, font: F.HEEBO, weight: 500, color: '#FFFFFF', gap: 22 });
    const soon = txt(capG, 'בקרוב', { x: W / 2, y: 1250, size: 54, font: F.HEEBO, weight: 500, align: 'c', color: A[4] });
    const lockup = [word, soon, ...slogan.map(s => s.g)];

    // ---------------------------------------------------------------- sound: every press is a key click; the big moves breathe
    for (const [tp, key] of TL.presses) sfx(tp, 'dome', /^[0-9]$/.test(key) ? 0.85 : 0.75);
    [[TL.outline[1], 0.45], [TL.win[1], 0.35], [TL.nav[1], 0.4], [TL.keypad[1], 0.45]].forEach(([t, g]) => sfx(t, 'tick', g));
    sfx(TL.turn34 + 0.05, 'whoosh', 0.55); sfx(TL.dim8[0], 'tick', 0.45);
    sfx(TL.raise, 'swish', 0.35);
    sfx(TL.zoom, 'whoosh', 0.4); sfx(TL.pullBack, 'swish', 0.3); sfx(TL.known, 'success', 0.45);
    sfx(TL.send, 'swish', 0.5); sfx(TL.reply, 'pop', 0.45);
    TL.pages.forEach(t => sfx(t + 0.02, 'swish', 0.25)); sfx(TL.saved, 'ding', 0.35);
    TL.strikes.forEach(t => sfx(t, 'tick', 0.8));
    TL.colors.forEach(([, ok]) => sfx(ok + 0.06, 'whoosh', 0.32)); sfx(TL.backSettings + 0.04, 'swish', 0.3);   // the colour waves
    sfx(TL.pullOut, 'whoosh', 0.45);   // the turn and the mark are carried by the bed (riser, hit, chord)

    report('duration', DUR);

    // ================================================================ seek
    return t => {
      // rig
      const p = pose(t), c = cam(t);
      const br = sm(seg(t, 11, 13)) * (1 - sm(seg(t, 56.8, 57.6)));   // breathing: never a frozen frame, never a start or an end
      p[3] += br * 1.4 * Math.sin(0.37 * t + 2); p[4] += br * 0.9 * Math.sin(0.29 * t);
      c[0] += br * 7 * Math.sin(0.55 * t); c[2] += br * 1.4 * Math.sin(0.41 * t + 1);
      ph.pose({ x: p[0], y: p[1], z: p[2], yaw: p[3], pitch: p[4], roll: p[5] });
      ph.cam({ dist: c[0], tx: c[1], ty: c[2] });
      ph.update();
      const ks = keyState(t); ph.press(ks.depth, ks.tilt);
      ph.lamps(sm(seg(t, TL.spin + 0.3, TL.spin + 1.4)) * (1 - sm(seg(t, TL.dissolve[0], TL.dissolve[1]))));   // colour on the back
      // the solid: arrives after the drawing, leaves into lines at the end
      const solid = sm(seg(t, TL.solid[0], TL.solid[1])) * (1 - sm(seg(t, TL.dissolve[0], TL.dissolve[1])));
      if (solid > 0.002) { ph.canvas.style.visibility = 'inherit'; ph.canvas.style.opacity = solid.toFixed(3); ph.render(); }
      else ph.canvas.style.visibility = 'hidden';

      // screen
      const front = clamp((ph.facing(1) - 0.2) * 2.2, 0, 1);
      const onK = E(t, TL.screenOn, 14) * (1 - E(t, TL.screenOff, 16)) * front * solid;
      if (onK > 0.002) {
        uiHost.style.visibility = 'inherit';
        const dst = [[D.ACT_X0, D.ACT_Y1], [D.ACT_X1, D.ACT_Y1], [D.ACT_X1, D.ACT_Y0], [D.ACT_X0, D.ACT_Y0]].map(([x, y]) => ph.project(x, y, SCRZ));
        ui.root.style.transform = homography(UIC, dst);
        ui.root.style.opacity = onK.toFixed(3);
        ui.update(t);
      } else uiHost.style.visibility = 'hidden';

      // lines, dimensions, hand, icons, the mark
      ov.update(t);

      // the colour flood: every wave is a circle; the first one holds all the colour (the later ones start inside it)
      let held = null;
      if (t >= FLOODS[0].ok && t < TL.backSettings + 1.2) {
        floodWrap.style.visibility = 'inherit';
        FLOODS.forEach(f => {
          const c = t >= f.ok ? floodAt(f, t) : null;
          if (!c || c[2] < 0.5) { f.el.style.visibility = 'hidden'; return; }
          f.el.style.visibility = 'inherit'; f.el.style.clipPath = circle(c);
        });
        held = floodAt(FLOODS[0], t);
        if (t >= TL.backSettings) { held = collapseAt(t); floodWrap.style.clipPath = circle(held); }
        else floodWrap.style.clipPath = 'none';
      } else floodWrap.style.visibility = 'hidden';

      // captions
      for (const cp of CAPS) {
        if (t < cp.tIn || t > cp.tOut + 0.5) { tf(cp.g, { o: 0 }); continue; }
        const r = rise(t, cp.tIn, 34), out = E(t, cp.tOut, 24);
        tf(cp.g, { y: r.y - 16 * out, o: r.o * (1 - out), blur: 7 * out });
      }
      if (held && held[2] > 0.5) { cap8B.style.visibility = 'inherit'; cap8B.style.clipPath = circle(held); cap8W.style.clipPath = outside(held); }
      else { cap8B.style.visibility = 'hidden'; cap8W.style.clipPath = 'none'; }
      FBL.forEach(({ g, i }) => {
        if (t < TL.forbid[i] || t > TL.forbidOut + 0.8) { tf(g, { o: 0 }); return; }
        const r = rise(t, TL.forbid[i] + 0.05, 24), out = E(t, TL.forbidOut, 12);
        const dim = 1 - 0.62 * sm(seg(t, TL.strikes[i] + 0.15, TL.strikes[i] + 0.5));
        tf(g, { y: r.y - 20 * out, o: 0.85 * r.o * dim * (1 - out) });
      });
      // closing lockup; a very slow push keeps the last frames alive
      const drift = 1 + 0.012 * sm(seg(t, TL.word[0], DUR));
      const n = word.spans.length;
      word.spans.forEach((s, i) => {
        const ti = TL.word[0] + (TL.word[1] - TL.word[0]) * i / (n - 1), r = rise(t, ti, 26);
        s.style.opacity = r.o.toFixed(3); s.style.transform = `translateY(${r.y.toFixed(2)}px)`;
        s.style.color = FOS.mixHex(WORDC[i % 4], '#FFFFFF', E(t, ti + 0.3, 6));   // lands in colour, cools to white
      });
      tf(word, { s: drift, o: t >= TL.word[0] ? 1 : 0 });
      slogan.forEach((w, i) => { const r = rise(t, TL.slogan + i * 0.08, 26); tf(w.g, { y: r.y, o: r.o }); });
      { const r = rise(t, TL.soon, 22); tf(soon, { y: r.y, o: r.o }); }
    };
  });
})();
