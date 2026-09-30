// fos-overlay.js: everything drawn over the phone in one SVG: the blueprint line, the dimension lines, the hand,
// the struck-out icons and the closing mark. Lines are 3D polylines in SCAD mm, projected every frame with the same
// camera as the 3D body, so the drawing and the solid are always the same object.
window.FOS = window.FOS || {};
(() => {
  'use strict';
  const DEG = Math.PI / 180;

  // ------------------------------------------------------------------ polylines in SCAD mm
  // rounded rect, counter-clockwise, starting at the top centre. rT / rB: top / bottom corner radii
  function rrPts(x0, y0, x1, y1, rT, rB, z = 0, n = 14) {
    const P = [], cx = (x0 + x1) / 2, arc = (ox, oy, r, a0, a1) => { for (let i = 1; i <= n; i++) { const a = (a0 + (a1 - a0) * i / n) * DEG; P.push([ox + r * Math.cos(a), oy + r * Math.sin(a), z]); } };
    P.push([cx, y1, z]); P.push([x0 + rT, y1, z]);
    arc(x0 + rT, y1 - rT, rT, 90, 180); P.push([x0, y0 + rB, z]);
    arc(x0 + rB, y0 + rB, rB, 180, 270); P.push([x1 - rB, y0, z]);
    arc(x1 - rB, y0 + rB, rB, 270, 360); P.push([x1, y1 - rT, z]);
    arc(x1 - rT, y1 - rT, rT, 0, 90); P.push([cx, y1, z]);
    return P;
  }
  const circPts = (cx, cy, r, z = 0, n = 48, a0 = 90) => Array.from({ length: n + 1 }, (_, i) => { const a = (a0 + 360 * i / n) * DEG; return [cx + r * Math.cos(a), cy + r * Math.sin(a), z]; });
  const keyPts = (k, z) => {
    const [x, y, w, h] = k, r = Math.min(h / 2 - 0.01, 0.8 * h / 2);
    return rrPts(x - w / 2, y - h / 2, x + w / 2, y + h / 2, r, r, z, 8);
  };
  const cumLen = P => { const c = [0]; for (let i = 1; i < P.length; i++) c.push(c[i - 1] + Math.hypot(P[i][0] - P[i - 1][0], P[i][1] - P[i - 1][1], (P[i][2] || 0) - (P[i - 1][2] || 0))); return c; };
  // the part of a polyline from length a*L to b*L (both 0..1)
  function cut(P, c, a, b) {
    const L = c[c.length - 1], A = a * L, B = b * L, out = [];
    if (B <= A) return out;
    const at = (s) => { let i = 1; while (i < c.length - 1 && c[i] < s) i++; const u = (s - c[i - 1]) / Math.max(1e-9, c[i] - c[i - 1]); return P[i - 1].map((v, j) => v + (P[i][j] - v) * u); };
    out.push(at(A));
    for (let i = 0; i < P.length; i++) if (c[i] > A && c[i] < B) out.push(P[i]);
    out.push(at(B));
    return out;
  }
  const dOf = (S, close = false) => S.length < 2 ? '' : 'M' + S.map(p => p[0].toFixed(1) + ' ' + p[1].toFixed(1)).join('L') + (close ? 'Z' : '');
  // centripetal Catmull-Rom through 2D points, sampled
  function spline(pts, per = 8, closed = false) {
    const P = closed ? [pts[pts.length - 1], ...pts, pts[0], pts[1]] : [pts[0], ...pts, pts[pts.length - 1]];
    const out = [];
    for (let i = 1; i < P.length - 2; i++) {
      const [p0, p1, p2, p3] = [P[i - 1], P[i], P[i + 1], P[i + 2]];
      const d = (a, b) => Math.max(1e-4, Math.pow(Math.hypot(b[0] - a[0], b[1] - a[1]), 0.5));
      const t0 = 0, t1 = t0 + d(p0, p1), t2 = t1 + d(p1, p2), t3 = t2 + d(p2, p3);
      for (let s = 0; s < per; s++) {
        const t = t1 + (t2 - t1) * s / per;
        const L = (a, b, ta, tb) => a.map((v, j) => (tb - t) / (tb - ta) * v + (t - ta) / (tb - ta) * b[j]);
        const A1 = L(p0, p1, t0, t1), A2 = L(p1, p2, t1, t2), A3 = L(p2, p3, t2, t3);
        const B1 = L(A1, A2, t0, t2), B2 = L(A2, A3, t1, t3);
        out.push(L(B1, B2, t1, t2));
      }
    }
    out.push(closed ? pts[0] : pts[pts.length - 1]);
    return out;
  }
  function hull(pts) {   // Andrew's monotone chain, screen points
    const p = pts.slice().sort((a, b) => a[0] - b[0] || a[1] - b[1]);
    const cr = (o, a, b) => (a[0] - o[0]) * (b[1] - o[1]) - (a[1] - o[1]) * (b[0] - o[0]);
    const lo = [], up = [];
    for (const q of p) { while (lo.length >= 2 && cr(lo[lo.length - 2], lo[lo.length - 1], q) <= 0) lo.pop(); lo.push(q); }
    for (let i = p.length - 1; i >= 0; i--) { const q = p[i]; while (up.length >= 2 && cr(up[up.length - 2], up[up.length - 1], q) <= 0) up.pop(); up.push(q); }
    return lo.slice(0, -1).concat(up.slice(0, -1));
  }
  FOS.geo = { rrPts, circPts, cumLen, cut, dOf, spline, hull };

  FOS.buildOverlay = (parent, ph) => {
    const D = FOS.D, TL = FOS.TL, LOGO = FOS.LOGO;
    // colour copy: the drawing is cyan, the measurements orange, the struck-out icons each in a system colour
    const A = FOS.T.accents, LINE = A[1], DIM = A[2], FBC = [A[1], A[2], A[4], A[3]];
    const E = (t, t0, wn = 18) => S(t - t0, wn, 1);
    const sm = x => x <= 0 ? 0 : x >= 1 ? 1 : x * x * (3 - 2 * x);
    const svg = svgEl('svg', parent, { width: W, height: H, viewBox: `0 0 ${W} ${H}` });
    svg.style.cssText = 'position:absolute;left:0;top:0;overflow:visible';
    const defs = svgEl('defs', svg);
    const proj = P => P.map(p => ph.project(p[0], p[1], p[2] || 0));

    // ================================================================ blueprint lines (front)
    const KZ = 0.85;
    const lines = [];
    const addLine = (P, t0, t1, o = {}) => { const c = cumLen(P); const el = svgEl('path', svg, { fill: 'none', stroke: LINE, 'stroke-width': o.w || 1.7, 'stroke-linecap': 'round', 'stroke-linejoin': 'round' }); lines.push({ P, c, t0, t1, el, pen: o.pen, a: o.a ?? 0.92, side: o.side || 1 }); return lines[lines.length - 1]; };
    addLine(rrPts(0, 0, D.BODY_W, D.BODY_L, D.CORNER_R, D.CORNER_R, 0, 20), TL.outline[0], TL.outline[1], { pen: true, w: 2 });
    addLine(rrPts(D.SIDE_WALL, D.WIN_Y0, D.BODY_W - D.SIDE_WALL, D.WIN_Y1, D.WIN_R_TOP, D.WIN_R_BOT, 0.02, 14), TL.win[0], TL.win[1], { pen: true });
    addLine(circPts(D.PUNCH[0], D.PUNCH[1], D.PUNCH_D / 2, 0.03, 32), TL.punch[0], TL.punch[1] - 0.05, { w: 1.4 });
    addLine(rrPts(D.EARPIECE[0] - 5, D.EARPIECE[1] - 0.4, D.EARPIECE[0] + 5, D.EARPIECE[1] + 0.4, 0.39, 0.39, 0.02, 4), TL.punch[0] + 0.08, TL.punch[1], { w: 1.2, a: 0.7 });
    const [kx0, ky0, kx1, ky1] = D.KP;
    addLine(rrPts(kx0 - 0.8, ky0 - 0.8, kx1 + 0.8, ky1 + 0.8, 3, 3, 0.02, 8), TL.nav[0], TL.nav[0] + 0.5, { w: 1.5, a: 0.8 });
    const nk = D.navKeys, nt = (i) => TL.nav[0] + 0.12 + i * 0.1;
    addLine(circPts(nk[0][0], nk[0][1], D.RING_D / 2, KZ, 56), nt(0), nt(0) + 0.35);
    addLine(circPts(nk[0][0], nk[0][1], D.RING_HOLE / 2, KZ, 40), nt(1), nt(1) + 0.3, { w: 1.4, a: 0.7 });
    addLine(circPts(nk[1][0], nk[1][1], nk[1][2] / 2, KZ, 40), nt(1) + 0.08, nt(1) + 0.35);
    nk.slice(2).forEach((k, i) => addLine(keyPts(k, KZ), nt(2 + i), nt(2 + i) + 0.32));
    D.digitKeys.forEach((k, i) => addLine(keyPts(k, KZ), TL.keypad[0] + i * 0.075, TL.keypad[0] + i * 0.075 + 0.3));
    const pen = svgEl('circle', svg, { r: 3.4, fill: '#fff' });
    // digits on the blueprint keys
    const digitLabels = D.digitKeys.map(k => { const e = svgEl('text', svg, { 'font-family': 'Heebo', 'font-weight': 500, fill: LINE, 'text-anchor': 'middle', 'dominant-baseline': 'central' }); e.textContent = k[5]; return { e, k }; });
    // flat volume: the faces take a faint white before the material arrives
    const faceFill = svgEl('path', svg, { fill: LINE, stroke: 'none' });
    svg.insertBefore(faceFill, svg.firstChild);
    const keyFills = D.frontKeys.map(k => { const e = svgEl('path', svg, { fill: LINE }); svg.insertBefore(e, faceFill.nextSibling); return { e, P: k[4] === 'ring' || k[4] === 'round' ? circPts(k[0], k[1], k[2] / 2, KZ, 40) : keyPts(k, KZ) }; });
    const bodyPts = rrPts(0, 0, D.BODY_W, D.BODY_L, D.CORNER_R, D.CORNER_R, 0, 16);

    // back view lines for the closing turn
    const backLines = [
      rrPts(0, 0, D.BODY_W, D.BODY_L, D.CORNER_R, D.CORNER_R, -D.BODY_T, 20),
      circPts(D.CAM_XY[0], D.CAM_XY[1], 4.6, -D.BODY_T - 0.3, 48), circPts(D.CAM_XY[0], D.CAM_XY[1], 3.0, -D.BODY_T - 0.3, 40),
      circPts(D.FLASH_XY[0], D.FLASH_XY[1], 1.5, -D.BODY_T, 28),
      FOS.infinityPoints(D.INF.w / 2, 160).map(([x, y]) => [D.INF.x + x, D.INF.y + y, -D.BODY_T - 0.1]),
    ].map(P => ({ P, c: cumLen(P), el: svgEl('path', svg, { fill: 'none', stroke: LINE, 'stroke-width': 1.7, 'stroke-linecap': 'round', 'stroke-linejoin': 'round' }) }));

    // ================================================================ dimension lines (screen space)
    const dimG = svgEl('g', svg);
    function dimSet(label, fontSize = 24) {
      const g = svgEl('g', dimG);
      const ext1 = svgEl('line', g, { stroke: DIM, 'stroke-width': 1.2 }), ext2 = svgEl('line', g, { stroke: DIM, 'stroke-width': 1.2 });
      const main = svgEl('line', g, { stroke: DIM, 'stroke-width': 1.3 });
      const t1 = svgEl('line', g, { stroke: DIM, 'stroke-width': 1.6 }), t2 = svgEl('line', g, { stroke: DIM, 'stroke-width': 1.6 });
      const bg = svgEl('rect', g, { fill: '#000' });
      const tx = svgEl('text', g, { 'font-family': "'JetBrains Mono'", 'font-size': fontSize, 'font-weight': 500, fill: DIM, 'text-anchor': 'middle', 'dominant-baseline': 'central', direction: 'ltr' });
      tx.textContent = label;
      return { g, ext1, ext2, main, t1, t2, tx, bg, label };
    }
    // a, b: screen points; n: unit offset direction; off: distance of the dimension line; k: 0..1 build; o: opacity
    function drawDim(d, a, b, n, off, k, o, inside = true) {
      if (o <= 0.003) { d.g.style.visibility = 'hidden'; return; }
      d.g.style.visibility = 'inherit'; d.g.style.opacity = o.toFixed(3);
      const ke = sm(seg(k, 0, 0.45)), km = sm(seg(k, 0.25, 0.8)), kl = sm(seg(k, 0.6, 1));
      const ea = [a[0] + n[0] * 8, a[1] + n[1] * 8], eb = [b[0] + n[0] * 8, b[1] + n[1] * 8];
      const len = (off + 12) * ke;
      const set = (el, p, q) => { el.setAttribute('x1', p[0].toFixed(1)); el.setAttribute('y1', p[1].toFixed(1)); el.setAttribute('x2', q[0].toFixed(1)); el.setAttribute('y2', q[1].toFixed(1)); };
      set(d.ext1, ea, [ea[0] + n[0] * len, ea[1] + n[1] * len]); set(d.ext2, eb, [eb[0] + n[0] * len, eb[1] + n[1] * len]);
      const pa = [a[0] + n[0] * (8 + off), a[1] + n[1] * (8 + off)], pb = [b[0] + n[0] * (8 + off), b[1] + n[1] * (8 + off)];
      const mid = [(pa[0] + pb[0]) / 2, (pa[1] + pb[1]) / 2];
      set(d.main, [mid[0] + (pa[0] - mid[0]) * km, mid[1] + (pa[1] - mid[1]) * km], [mid[0] + (pb[0] - mid[0]) * km, mid[1] + (pb[1] - mid[1]) * km]);
      const u = [pb[0] - pa[0], pb[1] - pa[1]], ul = Math.hypot(u[0], u[1]) || 1, uu = [u[0] / ul, u[1] / ul];
      const tn = inside ? [-uu[1], uu[0]] : n;
      const tick = (p) => { const s = 7 * km; return [[p[0] - (uu[0] + tn[0]) * s, p[1] - (uu[1] + tn[1]) * s], [p[0] + (uu[0] + tn[0]) * s, p[1] + (uu[1] + tn[1]) * s]]; };
      const [ta1, ta2] = tick(pa), [tb1, tb2] = tick(pb); set(d.t1, ta1, ta2); set(d.t2, tb1, tb2);
      let ang = Math.atan2(uu[1], uu[0]) / DEG; if (ang > 90) ang -= 180; if (ang < -90) ang += 180;
      const lp = inside ? mid : [mid[0] + n[0] * 22, mid[1] + n[1] * 22];
      d.tx.setAttribute('transform', `translate(${lp[0].toFixed(1)} ${lp[1].toFixed(1)}) rotate(${ang.toFixed(2)})`);
      d.tx.setAttribute('x', 0); d.tx.setAttribute('y', 0); d.tx.style.opacity = kl.toFixed(3);
      const bw = d.label.length * 15 + 18;
      d.bg.setAttribute('transform', d.tx.getAttribute('transform'));
      d.bg.setAttribute('x', -bw / 2); d.bg.setAttribute('y', -16); d.bg.setAttribute('width', bw); d.bg.setAttribute('height', 32);
      d.bg.style.opacity = inside ? kl.toFixed(3) : '0';
    }
    const dW = dimSet('53'), dH = dimSet('145.3'), dS = dimSet('3.5″'), dT = dimSet('8');

    // ================================================================ the hand (right hand, drawn in the phone's front plane)
    const handG = svgEl('g', svg);
    const mask = svgEl('mask', defs, { id: 'palmMask', maskUnits: 'userSpaceOnUse', x: -200, y: -200, width: W + 400, height: H + 400 });
    svgEl('rect', mask, { x: -200, y: -200, width: W + 400, height: H + 400, fill: '#fff' });
    const maskPhone = svgEl('path', mask, { fill: '#000' });
    const maskThumb = svgEl('path', mask, { fill: '#000' });
    const palmG = svgEl('g', handG, { mask: 'url(#palmMask)' });
    const palmFill = svgEl('path', palmG, { fill: '#fff', 'fill-opacity': 0.055 });
    const palmLine = svgEl('path', palmG, { fill: 'none', stroke: '#fff', 'stroke-width': 1.9, 'stroke-linecap': 'round', 'stroke-linejoin': 'round' });
    const palmDetail = svgEl('path', palmG, { fill: 'none', stroke: '#fff', 'stroke-width': 1.3, 'stroke-linecap': 'round', 'stroke-opacity': 0.4 });
    const thumbFill = svgEl('path', handG, { fill: '#fff', 'fill-opacity': 0.07 });
    const thumbLine = svgEl('path', handG, { fill: 'none', stroke: '#fff', 'stroke-width': 1.9, 'stroke-linecap': 'round', 'stroke-linejoin': 'round' });
    const thumbDetail = svgEl('path', handG, { fill: 'none', stroke: '#fff', 'stroke-width': 1.2, 'stroke-linecap': 'round', 'stroke-opacity': 0.34 });
    // palm outline, clockwise from the outer side of the thumb's base (SCAD mm, the phone's front plane)
    const TB = [62.4, 19.2], TH = 9.6, dir0 = [Math.cos(117 * DEG), Math.sin(117 * DEG)], nL = [-dir0[1], dir0[0]];
    const BLp = [TB[0] + TH * nL[0], TB[1] + TH * nL[1]], BRp = [TB[0] - TH * nL[0], TB[1] - TH * nL[1]];
    const palmKeys = [BRp, [76.5, 17], [80, 5], [80.8, -9], [80.4, -24], [82, -52], [86, -110], [89, -210],
      [30, -210], [27, -110], [24.5, -52], [19, -19], [8.5, -7.5], [1.5, -2.5],
      [-2.5, 2.5], [-6.4, 8], [-6.2, 13], [-2.6, 17.2], [-1.2, 18.6],
      [-3.4, 20.4], [-7.8, 26], [-7.9, 31], [-3.8, 35.3], [-1.4, 36.8],
      [-3.6, 38.6], [-8.6, 44.2], [-8.8, 49.4], [-4.3, 53.6], [-1.4, 55.1],
      [-3.4, 57], [-7.6, 62], [-7.4, 67], [-2.8, 70.6], [2.5, 71.8],
      [20, 68], [40, 52], [51, 31], BLp];
    const palmBase = spline(palmKeys, 7, true);
    const isFinger = palmBase.map(p => p[0] < 0.5);
    // creases between the fingertips (short lines from each notch toward the phone)
    const creases = [[[-1.2, 18.6], [-3.6, 18.4]], [[-1.4, 36.8], [-4.4, 36.6]], [[-1.4, 55.1], [-4.6, 55]]];
    // the thumb: a tapered curve from its base (at the phone's right edge) to the key. It leaves the palm along a fixed
    // direction and arrives along the reach, so it bends by itself; the whole hand slides a little toward far keys.
    const T0 = [30, 38];
    const reachShift = tgt => [(tgt[0] - T0[0]) * 0.22, (tgt[1] - T0[1]) * 0.3];
    function thumbShape(T, press) {
      const B = TB, dx = T[0] - B[0], dy = T[1] - B[1], Dd = Math.hypot(dx, dy), ux = dx / Dd, uy = dy / Dd;
      const P1 = [B[0] + dir0[0] * 0.42 * Dd, B[1] + dir0[1] * 0.42 * Dd], P2 = [T[0] - ux * 0.3 * Dd, T[1] - uy * 0.3 * Dd];
      const N = 34, cl = [];
      for (let i = 0; i <= N; i++) {
        const s = i / N, a = (1 - s) ** 3, b = 3 * (1 - s) ** 2 * s, c = 3 * (1 - s) * s * s, d = s ** 3;
        cl.push([a * B[0] + b * P1[0] + c * P2[0] + d * T[0], a * B[1] + b * P1[1] + c * P2[1] + d * T[1]]);
      }
      const halfW = s => (TH + (7.7 - TH) * Math.min(1, s / 0.8) + 0.45 * Math.exp(-(((s - 0.45) / 0.12) ** 2))) * (1 + 0.05 * press * s);
      const left = [], right = [];
      cl.forEach((q, i) => {
        const n1 = cl[Math.min(N, i + 1)], o = cl[Math.max(0, i - 1)];
        const tx = n1[0] - o[0], ty = n1[1] - o[1], tl = Math.hypot(tx, ty) || 1;
        const nx = -ty / tl, ny = tx / tl, w = i === 0 ? TH : halfW(i / N);
        left.push([q[0] + nx * w, q[1] + ny * w]); right.push([q[0] - nx * w, q[1] - ny * w]);
      });
      const e = cl[N], f = cl[N - 2], tx = e[0] - f[0], ty = e[1] - f[1], tl = Math.hypot(tx, ty), ex = tx / tl, ey = ty / tl, w = halfW(1);
      const tipPts = [];
      for (let i = 1; i < 18; i++) { const a = Math.PI * i / 18; tipPts.push([e[0] - ey * w * Math.cos(a) + ex * w * 1.05 * Math.sin(a), e[1] + ex * w * Math.cos(a) + ey * w * 1.05 * Math.sin(a)]); }
      const outline = [...left, ...tipPts, ...right.slice().reverse()];
      // the nail: an oval along the thumb, close to the tip (the viewer sees the back of the thumb)
      const nail = [], nc = [e[0] + ex * 0.4, e[1] + ey * 0.4], along = 4.8, across = 0.46 * w;
      for (let i = 0; i <= 28; i++) { const a = Math.PI * 2 * i / 28; nail.push([nc[0] + ex * along * Math.cos(a) - ey * across * Math.sin(a), nc[1] + ey * along * Math.cos(a) + ex * across * Math.sin(a)]); }
      // the joint: a short crease across the thumb
      const j = Math.round(N * 0.5), kc = cl[j], k2 = cl[j + 1], kx = k2[0] - kc[0], ky = k2[1] - kc[1], kL = Math.hypot(kx, ky), jw = 0.62 * halfW(0.5);
      const crease = [[kc[0] - ky / kL * jw, kc[1] + kx / kL * jw], [kc[0] + kx / kL * 1.4, kc[1] + ky / kL * 1.4], [kc[0] + ky / kL * jw, kc[1] - kx / kL * jw]];
      return { outline, nail, crease };
    }
    // where the thumb goes for each key (the pad lands on the key; the tip is a little beyond it)
    const KC = {}; D.frontKeys.forEach(k => KC[k[5]] = [k[0], k[1]]);
    const R = D.RING_D / 2 - 2.6;
    const target = key => ({ left: [KC.dpad[0] - R, KC.dpad[1]], right: [KC.dpad[0] + R, KC.dpad[1]], up: [KC.dpad[0], KC.dpad[1] + R], down: [KC.dpad[0], KC.dpad[1] - R], ok: KC.ok })[key] || KC[key];
    const REST = [35, 32];
    const tipTrack = track(REST, TL.presses.map(([tp, key]) => { const p = target(key); return [tp - 0.2, p, 24, 1]; }));
    const pressAt = t => TL.presses.reduce((m, [tp]) => Math.max(m, S(t - tp, 45, 1) - S(t - tp - 0.12, 30, 1)), 0);

    // ================================================================ struck-out icons (scene 9)
    const forbid = svgEl('g', svg);
    const FB = [['public', 'דפדפן'], ['storefront', 'חנות אפליקציות'], ['groups', 'רשתות חברתיות'], ['play_circle', 'וידאו']];
    const fbPos = [[580, 420], [300, 420], [580, 700], [300, 700]];
    const fbItems = FB.map(([icon, name], i) => {
      const g = svgEl('g', forbid);
      const ig = svgEl('g', g, { transform: `translate(${fbPos[i][0] - 60} ${fbPos[i][1] - 60}) scale(5)` });
      for (const part of FOS.ICONS[icon].split('|')) svgEl('path', ig, { d: part, fill: 'none', stroke: FBC[i], 'stroke-width': 0.85, 'stroke-linecap': 'round', 'stroke-linejoin': 'round' });
      const strike = svgEl('line', g, { stroke: FOS.T.danger, 'stroke-width': 4, 'stroke-linecap': 'round' });
      return { g, strike, x: fbPos[i][0], y: fbPos[i][1], name };
    });

    // ================================================================ the closing mark: the ftr logo, drawn by the same line
    const LS = 2.25, logoW = LOGO.w * LS, logoH = LOGO.h * LS, LX = W / 2 - logoW / 2, LY = 392 - logoH / 2;
    const logoG = svgEl('g', svg);
    const logoArt = svgEl('g', logoG, { transform: `translate(${LX} ${LY}) scale(${LS})` });
    const logoPaths = LOGO.strokes.map(d => svgEl('path', logoArt, { d, fill: 'none', stroke: LOGO.color, 'stroke-width': LOGO.stroke, 'stroke-linecap': 'butt' }));
    const logoPen = LOGO.strokes.map(d => svgEl('path', logoArt, { d, fill: 'none', stroke: '#fff', 'stroke-width': 2.4 / LS, 'stroke-linecap': 'round' }));
    const logoLens = logoPaths.map(p => p.getTotalLength()), logoTotal = logoLens.reduce((a, b) => a + b, 0);
    const logoDot = svgEl('circle', svg, { r: 3.6, fill: '#fff' });

    // ================================================================ update
    const facingK = s => clamp((ph.facing(s) - 0.05) * 3, 0, 1);
    return {
      update(t) {
        const front = facingK(1), back = facingK(-1);
        // ---------------- blueprint lines: drawn, then fade as the solid arrives
        const linesA = t < TL.linesOut[1] ? 1 - sm(seg(t, TL.linesOut[0], TL.linesOut[1])) : 0;
        let penP = null;
        for (const L of lines) {
          const k = sm(seg(t, L.t0, L.t1));
          if (k <= 0 || linesA <= 0) { L.el.style.visibility = 'hidden'; continue; }
          L.el.style.visibility = 'inherit';
          const Sg = proj(cut(L.P, L.c, 0, k));
          L.el.setAttribute('d', dOf(Sg));
          L.el.style.opacity = (L.a * linesA * front).toFixed(3);
          if (L.pen && k < 1) penP = Sg[Sg.length - 1];
        }
        if (penP && linesA > 0) { pen.setAttribute('cx', penP[0].toFixed(1)); pen.setAttribute('cy', penP[1].toFixed(1)); pen.style.visibility = 'inherit'; }
        else if (t >= TL.pen && t < TL.outline[0]) { const p = ph.project(D.CX, D.BODY_L, 0); pen.setAttribute('cx', p[0].toFixed(1)); pen.setAttribute('cy', p[1].toFixed(1)); pen.style.visibility = 'inherit'; pen.style.opacity = E(t, TL.pen, 30).toFixed(3); }
        else pen.style.visibility = 'hidden';
        if (penP) pen.style.opacity = '1';
        const lk = sm(seg(t, TL.labels[0], TL.labels[1])) * linesA;
        digitLabels.forEach(({ e, k }, i) => {
          if (lk <= 0) { e.style.visibility = 'hidden'; return; }
          const dx = /[2-9]/.test(k[5]) ? -0.2 * k[2] : 0;   // where the key prints its digit (SCAD key_label2d)
          const p = ph.project(k[0] + dx, k[1] + k[3] * 0.03, KZ), q = ph.project(k[0] + dx, k[1] + k[3] * 0.55, KZ);
          e.style.visibility = 'inherit'; e.setAttribute('x', p[0].toFixed(1)); e.setAttribute('y', p[1].toFixed(1));
          e.setAttribute('font-size', (Math.abs(p[1] - q[1]) * 1.0).toFixed(1));
          e.style.opacity = (0.75 * sm(seg(t, TL.labels[0] + i * 0.03, TL.labels[0] + i * 0.03 + 0.25)) * linesA).toFixed(3);
        });
        // ---------------- the faint white faces: volume before the material
        const fillA = sm(seg(t, TL.fill[0], TL.fill[1])) * (1 - sm(seg(t, TL.solid[0] + 0.2, TL.solid[1])));
        if (fillA > 0.003) {
          faceFill.setAttribute('d', dOf(proj(bodyPts), true)); faceFill.style.opacity = (0.06 * fillA).toFixed(3); faceFill.style.visibility = 'inherit';
          keyFills.forEach(({ e, P }) => { e.setAttribute('d', dOf(proj(P), true)); e.style.opacity = (0.1 * fillA).toFixed(3); e.style.visibility = 'inherit'; });
        } else { faceFill.style.visibility = 'hidden'; keyFills.forEach(({ e }) => e.style.visibility = 'hidden'); }
        // ---------------- the back, as lines, at the end: the material leaves, the lines stay, then they go
        const bIn = sm(seg(t, TL.dissolve[0], TL.dissolve[1] - 0.3)), bOff = sm(seg(t, TL.linesOff[0], TL.linesOff[1]));
        backLines.forEach(L => {
          if (bIn <= 0 || bOff >= 1) { L.el.style.visibility = 'hidden'; return; }
          L.el.style.visibility = 'inherit';
          L.el.setAttribute('d', dOf(proj(cut(L.P, L.c, bOff, 1))));
          L.el.style.opacity = (0.9 * bIn * Math.max(back, 0.25)).toFixed(3);
        });

        // ---------------- dimension lines
        if (t < TL.dims[2][1] + 0.5) {
          const pTL = ph.project(0, D.BODY_L, 0), pTR = ph.project(D.BODY_W, D.BODY_L, 0), pBL = ph.project(0, 0, 0);
          const ow = [(TL.dims[0][1] > t ? 1 : 1 - sm(seg(t, TL.dims[0][1], TL.dims[0][1] + 0.35)))];
          drawDim(dW, pTL, pTR, [0, -1], 30, seg(t, TL.dims[0][0], TL.dims[0][0] + 0.75), (t >= TL.dims[0][0] ? 1 : 0) * ow[0] * linesA, false);
          drawDim(dH, pBL, pTL, [-1, 0], 34, seg(t, TL.dims[1][0], TL.dims[1][0] + 0.75), (t >= TL.dims[1][0] ? 1 : 0) * (1 - sm(seg(t, TL.dims[1][1], TL.dims[1][1] + 0.35))) * linesA, false);
          const a0 = ph.project(D.ACT_X0, D.ACT_Y0, 0), a1 = ph.project(D.ACT_X1, D.ACT_Y1, 0);
          const dx = a1[0] - a0[0], dy = a1[1] - a0[1], dl = Math.hypot(dx, dy);
          drawDim(dS, a0, a1, [dy / dl, -dx / dl].map(v => v * 0), 0, seg(t, TL.dims[2][0], TL.dims[2][0] + 0.75), (t >= TL.dims[2][0] ? 1 : 0) * (1 - sm(seg(t, TL.dims[2][1], TL.dims[2][1] + 0.35))) * linesA, true);
        } else { dW.g.style.visibility = dH.g.style.visibility = dS.g.style.visibility = 'hidden'; }
        if (t >= TL.dim8[0] && t < TL.dim8[1] + 0.5) {
          const yy = D.BODY_L - 2, a = ph.project(D.BODY_W, yy, 0), b = ph.project(D.BODY_W, yy, -D.BODY_T);
          const top = ph.project(D.BODY_W, D.BODY_L, -D.BODY_T / 2);
          const A = [a[0], top[1]], B = [b[0], top[1]];
          drawDim(dT, A[0] < B[0] ? A : B, A[0] < B[0] ? B : A, [0, -1], 26, seg(t, TL.dim8[0], TL.dim8[0] + 0.7), 1 - sm(seg(t, TL.dim8[1], TL.dim8[1] + 0.35)), false);
        } else dT.g.style.visibility = 'hidden';

        // ---------------- the hand
        const hin = E(t, TL.handIn[0], 5.2), hout = E(t, TL.handOut[0], 5.5);
        const handOn = t >= TL.handIn[0] && t < TL.handOut[1] + 0.6;
        if (!handOn) handG.style.visibility = 'hidden';
        else {
          handG.style.visibility = 'inherit';
          const tip = tipTrack(t), pr = pressAt(t), sh = reachShift(tip);
          const off = [48 * (1 - hin) + 40 * hout + sh[0], -95 * (1 - hin) - 90 * hout + sh[1]];
          const grip = E(t, TL.grip, 10);
          const H3 = p => [p[0] + off[0], p[1] + off[1], 0.9];
          // palm: fingers close a little on the grip
          const palm = palmBase.map((p, i) => isFinger[i] ? [p[0] * (1 - 0.12 * grip), p[1]] : p);
          const Pp = proj(palm.map(H3));
          const draw = sm(seg(t, TL.handIn[0] + 0.1, TL.handIn[1])) * (1 - sm(seg(t, TL.handOut[0], TL.handOut[1] - 0.2)));
          const cP = cumLen(Pp.map(p => [p[0], p[1], 0]));
          // the contour draws on from the wrist (index 7 of the keys ~ the bottom) both ways; simplest: along the path
          palmLine.setAttribute('d', dOf(cut(Pp, cP, 0, draw)));
          palmFill.setAttribute('d', dOf(Pp, true));
          palmFill.setAttribute('fill-opacity', (0.055 * sm(seg(draw, 0.6, 1))).toFixed(3));
          palmDetail.setAttribute('d', creases.map(c => dOf(proj(c.map(p => H3([p[0] * (1 - 0.12 * grip), p[1]]))))).join(' '));
          palmDetail.style.opacity = sm(seg(draw, 0.8, 1)).toFixed(3);
          // thumb
          const th = thumbShape([tip[0] - sh[0], tip[1] - sh[1]], pr);
          const Tp = proj(th.outline.map(H3));
          const cT = cumLen(Tp.map(p => [p[0], p[1], 0]));
          thumbLine.setAttribute('d', dOf(cut(Tp, cT, 0, draw)));
          thumbFill.setAttribute('d', dOf(Tp, true));
          thumbFill.setAttribute('fill-opacity', ((0.07 + 0.07 * pr) * sm(seg(draw, 0.6, 1))).toFixed(3));   // contact: the pad lights a little
          thumbDetail.setAttribute('d', dOf(proj(th.nail.map(H3)), true) + ' ' + dOf(proj(th.crease.map(H3))));
          thumbDetail.style.opacity = sm(seg(draw, 0.8, 1)).toFixed(3);
          // mask: the phone's silhouette and the thumb hide the palm behind them
          const sil = hull(proj(bodyPts).concat(proj(bodyPts.map(p => [p[0], p[1], -D.BODY_T]))));
          maskPhone.setAttribute('d', dOf(sil, true));
          maskThumb.setAttribute('d', dOf(Tp, true));
        }

        // ---------------- struck-out icons
        if (t >= TL.forbid[0] && t < TL.forbidOut + 0.8) {
          forbid.style.visibility = 'inherit';
          const out = E(t, TL.forbidOut, 12);
          fbItems.forEach((f, i) => {
            const r = rise(t, TL.forbid[i], 30), ks = t < TL.strikes[i] ? 0 : 1 - Math.pow(2, -10 * Math.min(1, (t - TL.strikes[i]) / 0.26));
            f.g.style.opacity = (r.o * (1 - 0.62 * sm(seg(t, TL.strikes[i] + 0.15, TL.strikes[i] + 0.5))) * (1 - out)).toFixed(3);
            f.g.setAttribute('transform', `translate(0 ${(r.y - 20 * out).toFixed(2)})`);
            const a = [f.x + 76, f.y - 76], b = [f.x - 76, f.y + 76];
            f.strike.setAttribute('x1', a[0]); f.strike.setAttribute('y1', a[1]);
            f.strike.setAttribute('x2', (a[0] + (b[0] - a[0]) * ks).toFixed(1)); f.strike.setAttribute('y2', (a[1] + (b[1] - a[1]) * ks).toFixed(1));
            f.strike.style.visibility = ks > 0.001 ? 'inherit' : 'hidden';
          });
        } else forbid.style.visibility = 'hidden';

        // ---------------- the closing mark
        if (t >= TL.logoEnd[0]) {
          logoG.style.visibility = 'inherit';
          const lag = 26;   // the red follows the white line by this much (logo units)
          const s0 = sm(seg(t, TL.logoEnd[0], TL.logoEnd[1])) * (logoTotal + lag);
          let left = s0, penAt = null;
          logoPaths.forEach((p, i) => {
            const Lx = logoLens[i], dPen = clamp(left, 0, Lx), dRed = clamp(left - lag, 0, Lx);
            logoPen[i].setAttribute('stroke-dasharray', `${dPen.toFixed(2)} ${Lx + 1}`);
            logoPen[i].style.opacity = (1 - sm(seg(t, TL.logoEnd[1] - 0.1, TL.logoEnd[1] + 0.25))).toFixed(3);
            p.setAttribute('stroke-dasharray', `${dRed.toFixed(2)} ${Lx + 1}`);
            if (dPen > 0 && dPen < Lx) penAt = [logoPen[i], dPen];
            left -= Lx;
          });
          const k = s0 < logoTotal ? 0 : 1;
          const land = 1 + 0.045 * (1 - E(t, TL.logoEnd[1] - 0.15, 10));
          logoG.setAttribute('transform', `translate(${W / 2} 392) scale(${land.toFixed(4)}) translate(${-W / 2} -392)`);
          if (penAt && k < 1) { const q = penAt[0].getPointAtLength(penAt[1]); logoDot.setAttribute('cx', (W / 2 + (LX + q.x * LS - W / 2) * land).toFixed(1)); logoDot.setAttribute('cy', (392 + (LY + q.y * LS - 392) * land).toFixed(1)); logoDot.style.visibility = 'inherit'; }
          else logoDot.style.visibility = 'hidden';
        } else { logoG.style.visibility = 'hidden'; logoDot.style.visibility = 'hidden'; }
      },
    };
  };
})();
