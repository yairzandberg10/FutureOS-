/* The phone: the real FutureOS Regular, drawn from the repo's own 3D model (assets/phone-model.js, built by make-model.py from
   the STLs of hardware/phone3d and hardware/print) with three.js, into a canvas that lives in the world.
   World units = screen px: the model's display area is the 640x960 screen at the origin, so the UI (the DOM #screen) is laid
   exactly onto the model's display, and follows it through a projective transform when the phone turns. */
const PM = window.PHONE_MODEL.meta;
const CANVAS = { x0: -200, y0: -220, x1: 840, y1: 2080, pr: 1.0 };   // the world rectangle the canvas covers, and its pixel ratio
const CAM_D = 5200;                                                   // camera distance (px): a long lens, little distortion
const PIVOT = { x: 320, y: (PM.body.y0 + PM.body.y1) / 2, z: -4 * PM.mm };   // the middle of the body (y up, model units)
const KEY_IDS = ['d1', 'd2', 'd3', 'd4', 'd5', 'd6', 'd7', 'd8', 'd9', 'star', 'd0', 'pound', 'soft_l', 'soft_r', 'call', 'end', 'ok'];
const COL = { body: '#1c1c1e', key: '#2a2a2d', legend: '#f5f5f5', glass: '#020203' };   // the SCAD's colours

/* the D-pad of the confirm dialog (3-5 s), a 2D drawing that floats in the frame, not the phone */
const DPAD = { R: 128, r: 50 };

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

/* ------------------------------------------------------------ the 3D phone */
let R3 = null;

function geometryOf(part) {
  const b64 = (s) => Uint8Array.from(atob(s), (c) => c.charCodeAt(0)).buffer;
  const g = new THREE.BufferGeometry();
  g.setAttribute('position', new THREE.BufferAttribute(new Float32Array(b64(part.p)), 3));
  g.setAttribute('normal', new THREE.BufferAttribute(new Int8Array(b64(part.n)), 3, true));
  return g;
}

function buildPhone() {
  const w = document.getElementById('world');
  const cw = CANVAS.x1 - CANVAS.x0, ch = CANVAS.y1 - CANVAS.y0;
  w.innerHTML = `<canvas id="phone3d" class="abs" style="left:${CANVAS.x0}px;top:${CANVAS.y0}px;width:${cw}px;height:${ch}px"></canvas><div id="screen"></div>`;
  window.$screen = document.getElementById('screen');
  $screen.style.clipPath = screenClip();
  $screen.dataset.punch = punchHTML();

  const canvas = document.getElementById('phone3d');
  const renderer = new THREE.WebGLRenderer({ canvas, antialias: true, alpha: true, preserveDrawingBuffer: true });
  renderer.setPixelRatio(CANVAS.pr);
  renderer.setSize(cw, ch, false);
  renderer.outputColorSpace = THREE.SRGBColorSpace;
  renderer.toneMapping = THREE.NoToneMapping;
  renderer.setClearColor(0x000000, 0);

  const scene = new THREE.Scene();
  // studio lights: a soft key above left, a cool rim from the right, a warm one from the left, and a little ambient. (No
  // environment map: the renders run on a software GPU, where image-based lighting costs ~5x a frame.)
  scene.add(new THREE.HemisphereLight(0xffffff, 0x18181a, 1.1));
  const key = new THREE.DirectionalLight(0xffffff, 2.2); key.position.set(-0.6, 0.8, 1.0); scene.add(key);
  const rim = new THREE.DirectionalLight(0xdfe8ff, 2.4); rim.position.set(1.0, 0.25, -0.2); scene.add(rim);
  const warm = new THREE.DirectionalLight(0xffeedd, 1.2); warm.position.set(-1.0, -0.2, -0.1); scene.add(warm);

  // the camera: a perspective whose z = 0 plane lands exactly on the canvas's world rectangle
  const cam = new THREE.PerspectiveCamera();
  cam.position.set(PIVOT.x, PIVOT.y, CAM_D);
  const near = 200, far = CAM_D + 3000, k = near / CAM_D;
  cam.projectionMatrix.makePerspective((CANVAS.x0 - PIVOT.x) * k, (CANVAS.x1 - PIVOT.x) * k, (-CANVAS.y0 - PIVOT.y) * k, (-CANVAS.y1 - PIVOT.y) * k, near, far);
  cam.projectionMatrixInverse.copy(cam.projectionMatrix).invert();
  cam.updateMatrixWorld();

  const phone = new THREE.Group();                 // turns about the middle of the body
  phone.position.set(PIVOT.x, PIVOT.y, PIVOT.z);
  const model = new THREE.Group();
  model.position.set(-PIVOT.x, -PIVOT.y, -PIVOT.z);
  phone.add(model); scene.add(phone);

  const mats = {
    body: new THREE.MeshStandardMaterial({ color: COL.body, roughness: 0.62, metalness: 0 }),
    glass: new THREE.MeshStandardMaterial({ color: COL.glass, roughness: 0.06, metalness: 0 }),
  };
  const keyMat = () => new THREE.MeshStandardMaterial({ color: COL.key, roughness: 0.42, metalness: 0 });
  // the legends are a skin inside the key's top surface: pulled forward in depth so they never fight with the key around them
  const legMat = () => new THREE.MeshStandardMaterial({ color: COL.legend, roughness: 0.5, metalness: 0, polygonOffset: true, polygonOffsetFactor: -2, polygonOffsetUnits: -8 });
  const keys = {}, legends = {};
  let ring = null;
  const P = window.PHONE_MODEL.parts;
  for (const [name, part] of Object.entries(P)) {
    const g = geometryOf(part);
    let m;
    if (name.startsWith('key:')) {
      const id = name.slice(4);
      m = new THREE.Mesh(g, keyMat());
      if (id === 'ring') ring = m; else keys[id] = m;
    } else if (name.startsWith('legend:')) {
      m = new THREE.Mesh(g, legMat());
      legends[name.slice(7)] = m;
    } else {
      m = new THREE.Mesh(g, part.mat === 'key' ? keyMat() : mats[part.mat]);
    }
    model.add(m);
  }

  // the D-pad ring is one piece: a pressed direction lights its quarter (the same wedge the system draws)
  const ringU = { uPr: { value: new THREE.Vector4() }, uAcc: { value: new THREE.Color() }, uC: { value: new THREE.Vector2(PM.keys.ring.x, PM.keys.ring.y) } };
  ring.material.onBeforeCompile = (sh) => {
    Object.assign(sh.uniforms, ringU);
    sh.vertexShader = sh.vertexShader.replace('#include <common>', '#include <common>\nvarying vec2 vLocal;')
      .replace('#include <begin_vertex>', '#include <begin_vertex>\nvLocal = position.xy;');
    sh.fragmentShader = sh.fragmentShader.replace('#include <common>', '#include <common>\nvarying vec2 vLocal;\nuniform vec4 uPr;\nuniform vec3 uAcc;\nuniform vec2 uC;\nfloat ringW;')
      .replace('#include <color_fragment>', `#include <color_fragment>
        vec2 d = vLocal - uC;
        float a = atan(d.y, d.x);
        float q = 0.03;
        float wr = smoothstep(-0.7854 - q, -0.7854 + q, a) * (1.0 - smoothstep(0.7854 - q, 0.7854 + q, a));
        float wu = smoothstep(0.7854 - q, 0.7854 + q, a) * (1.0 - smoothstep(2.3562 - q, 2.3562 + q, a));
        float wd = smoothstep(-2.3562 - q, -2.3562 + q, a) * (1.0 - smoothstep(-0.7854 - q, -0.7854 + q, a));
        float wl = 1.0 - wr - wu - wd;
        ringW = clamp(dot(vec4(wu, wr, wd, wl), uPr), 0.0, 1.0);
        diffuseColor.rgb = mix(diffuseColor.rgb, uAcc, ringW);`)
      .replace('#include <emissivemap_fragment>', '#include <emissivemap_fragment>\ntotalEmissiveRadiance += uAcc * ringW * 0.8;');
  };

  R3 = { renderer, scene, cam, phone, keys, legends, ringU, last: '' };
}

/* the screen as seen through the glass window: the window's rounded top corners cut the display's top corners */
function screenClip() {
  const w = PM.window, r = w.rTop;
  const cx0 = w.x0 + r, cx1 = w.x1 - r, cy = w.y0 + r;         // centres of the top corner arcs, in screen px
  const dy = Math.sqrt(Math.max(0, r * r - cx0 * cx0));          // where each arc meets the display's side edge (x = 0 / 640)
  const dx = Math.sqrt(Math.max(0, r * r - cy * cy));            // where it meets the top edge (y = 0)
  const f = (v) => v.toFixed(2);
  return `path('M${f(cx0 - dx)} 0L${f(cx1 + dx)} 0A${f(r)} ${f(r)} 0 0 1 640 ${f(cy - dy)}L640 960L0 960L0 ${f(cy - dy)}A${f(r)} ${f(r)} 0 0 1 ${f(cx0 - dx)} 0Z')`;
}
/* the front camera's punch hole, drawn over the UI (it is a hole in the display) */
function punchHTML() {
  const p = PM.punch;
  return `<div class="punch" style="left:${(p.x - p.d / 2).toFixed(2)}px;top:${(p.y - p.d / 2).toFixed(2)}px;width:${p.d.toFixed(2)}px;height:${p.d.toFixed(2)}px"></div>`;
}

/* how the phone is turned at t (radians): it rises turned and settles square before the camera goes into the screen, and turns
   a little again for the ending, so the body reads as the object it is */
function phoneTurn(t) {
  let rx = 0, ry = 0;
  if (t < 8.4) { const p = std(inv(6.0, 8.4, t)); rx = lerp(0.42, 0, p); ry = lerp(-0.62, 0, p); }
  if (t >= 52.4) { const p = std(inv(52.4, 54.4, t)); rx = lerp(0, 0.1, p) + 0.015 * Math.sin((t - 52.4) * 0.9); ry = lerp(0, -0.3, p) + 0.05 * Math.sin((t - 52.4) * 0.6); }
  return { rx, ry };
}

/* 2D homography: the 640 x 960 box -> the quad p (tl, tr, br, bl), as a CSS matrix3d */
function quadMatrix(p, w = 640, h = 960) {
  const [[x0, y0], [x1, y1], [x2, y2], [x3, y3]] = p;
  const dx1 = x1 - x2, dx2 = x3 - x2, dx3 = x0 - x1 + x2 - x3, dy1 = y1 - y2, dy2 = y3 - y2, dy3 = y0 - y1 + y2 - y3;
  const den = dx1 * dy2 - dx2 * dy1;
  const g = (dx3 * dy2 - dx2 * dy3) / den, hh = (dx1 * dy3 - dx3 * dy1) / den;
  const a = x1 - x0 + g * x1, b = x3 - x0 + hh * x3, d = y1 - y0 + g * y1, e = y3 - y0 + hh * y3;
  const m = [a / w, d / w, 0, g / w, b / h, e / h, 0, hh / h, 0, 0, 1, 0, x0, y0, 0, 1];
  return `matrix3d(${m.map((v) => +v.toFixed(9)).join(',')})`;
}

const _v = [];
function updatePhone(t, tf = t, visible = true) {
  const cv = document.getElementById('phone3d');
  cv.style.visibility = visible ? 'visible' : 'hidden';
  if (!visible) return;
  const { rx, ry } = phoneTurn(tf);
  const acc = getComputedStyle(document.documentElement).getPropertyValue('--fos-accent').trim() || '#FFFFFF';
  const pr = KEY_IDS.map((id) => pressOf(id, t));
  const dir = ['up', 'right', 'down', 'left'].map((id) => pressOf(id, t));
  const state = [rx.toFixed(5), ry.toFixed(5), acc, ...pr.map((v) => v.toFixed(3)), ...dir.map((v) => v.toFixed(3))].join('|');

  R3.phone.rotation.set(rx, ry, 0);
  R3.phone.updateMatrixWorld(true);
  // the screen follows the display area of the model (a plane just under the glass, z = -0.02 mm)
  const z = -0.02 * PM.mm;
  const corners = [[0, 0], [640, 0], [640, -960], [0, -960]].map(([x, y]) => {
    const v = (_v[0] = _v[0] || new THREE.Vector3()).set(x, y, z);
    R3.phone.children[0].localToWorld(v);
    v.project(R3.cam);
    return [CANVAS.x0 + (v.x + 1) / 2 * (CANVAS.x1 - CANVAS.x0), CANVAS.y0 + (1 - v.y) / 2 * (CANVAS.y1 - CANVAS.y0)];
  });
  $screen.style.transform = quadMatrix(corners);

  if (state === R3.last) return;       // nothing moved: the canvas keeps the last frame
  R3.last = state;
  const white = new THREE.Color('#ffffff'), black = new THREE.Color('#000000'), accC = new THREE.Color(acc);
  const base = new THREE.Color(COL.key), leg = new THREE.Color(COL.legend);
  KEY_IDS.forEach((id, i) => {
    const m = R3.keys[id]; if (!m) return;
    const p = pr[i], fill = id === 'ok' ? accC : white;     // a pressed key fills white (OK: the accent), its legend turns black
    m.position.z = -p * 0.35 * PM.mm;
    m.material.color.copy(base).lerp(fill, p);
    m.material.emissive.copy(fill).multiplyScalar(0.85 * p);
    const l = R3.legends[id];
    if (l) { l.position.z = m.position.z; l.material.color.copy(leg).lerp(black, p); }
  });
  R3.ringU.uPr.value.set(dir[0], dir[1], dir[2], dir[3]);
  R3.ringU.uAcc.value.copy(accC);
  R3.renderer.render(R3.scene, R3.cam);
}
