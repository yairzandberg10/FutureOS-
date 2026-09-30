// fos-phone.js: the Regular body in three.js, built from the SCAD numbers in fos-data.js.
// Geometry lives in SCAD axes (mm): X right, Y up, Z out of the front face (front face z = 0, back z = -8).
// Nothing here animates by itself: seek() sets the pose, the key depths and the camera, then calls render().
window.FOS = window.FOS || {};
(() => {
  'use strict';
  const TAU = Math.PI * 2, DEG = Math.PI / 180;

  // rounded rectangle outline, counter-clockwise, per-point outward normals. r: radius (same on all corners)
  function rrOutline(x0, y0, x1, y1, r, segs = 14) {
    const cs = [[x1 - r, y1 - r, 0], [x0 + r, y1 - r, 90], [x0 + r, y0 + r, 180], [x1 - r, y0 + r, 270]];
    const out = [];
    for (const [cx, cy, a0] of cs) for (let i = 0; i <= segs; i++) {
      const a = (a0 + 90 * i / segs) * DEG;
      out.push({ x: cx + r * Math.cos(a), y: cy + r * Math.sin(a), nx: Math.cos(a), ny: Math.sin(a) });
    }
    return out;
  }
  // rounded rect with one radius for the top corners and one for the bottom ones, as a THREE.Shape
  function rrShape(x0, y0, x1, y1, rTop, rBot) {
    const s = new THREE.Shape();
    s.moveTo(x0 + rBot, y0);
    s.lineTo(x1 - rBot, y0); s.absarc(x1 - rBot, y0 + rBot, rBot, -Math.PI / 2, 0, false);
    s.lineTo(x1, y1 - rTop); s.absarc(x1 - rTop, y1 - rTop, rTop, 0, Math.PI / 2, false);
    s.lineTo(x0 + rTop, y1); s.absarc(x0 + rTop, y1 - rTop, rTop, Math.PI / 2, Math.PI, false);
    s.lineTo(x0, y0 + rBot); s.absarc(x0 + rBot, y0 + rBot, rBot, Math.PI, Math.PI * 1.5, false);
    return s;
  }
  FOS.rrShape = rrShape;

  // the body: a rounded slab with a 0.6 mm front fillet and a 1.5 mm back fillet (SCAD sbox), swept as one mesh
  function bodyGeometry(D) {
    const W = D.BODY_W, L = D.BODY_L, R = D.CORNER_R, T = D.BODY_T, rf = D.FILLET_FRONT, rb = D.FILLET_BACK;
    const ol = rrOutline(0, 0, W, L, R, 18);
    const prof = [], FS = 10;
    for (let i = 0; i <= FS; i++) {   // back fillet: back face -> wall
      const a = (Math.PI / 2) * (1 - i / FS);
      prof.push({ d: rb - rb * Math.cos(a), z: -T + rb - rb * Math.sin(a), on: Math.cos(a), nz: -Math.sin(a) });
    }
    for (let i = 0; i <= FS; i++) {   // front fillet: wall -> front face
      const a = (Math.PI / 2) * (i / FS);
      prof.push({ d: rf - rf * Math.cos(a), z: -rf + rf * Math.sin(a), on: Math.cos(a), nz: Math.sin(a) });
    }
    const pos = [], nor = [], idx = [];
    const n = ol.length;
    for (const p of prof) for (const o of ol) {
      pos.push(o.x - p.d * o.nx, o.y - p.d * o.ny, p.z);
      nor.push(o.nx * p.on, o.ny * p.on, p.nz);
    }
    for (let i = 0; i < prof.length - 1; i++) for (let j = 0; j < n; j++) {
      const a = i * n + j, b = i * n + (j + 1) % n, c = (i + 1) * n + j, d = (i + 1) * n + (j + 1) % n;
      idx.push(a, b, d, a, d, c);
    }
    // caps: the same outline inset by each fillet, so the edges meet the sweep exactly
    const cap = (inset, z, nz) => {
      const pts = ol.map(o => new THREE.Vector2(o.x - inset * o.nx, o.y - inset * o.ny));
      const tris = THREE.ShapeUtils.triangulateShape(pts, []);
      const base = pos.length / 3;
      for (const p of pts) { pos.push(p.x, p.y, z); nor.push(0, 0, nz); }
      for (const t of tris) nz > 0 ? idx.push(base + t[0], base + t[1], base + t[2]) : idx.push(base + t[0], base + t[2], base + t[1]);
    };
    cap(rb, -T, -1); cap(rf, 0, 1);
    const g = new THREE.BufferGeometry();
    g.setAttribute('position', new THREE.Float32BufferAttribute(pos, 3));
    g.setAttribute('normal', new THREE.Float32BufferAttribute(nor, 3));
    g.setIndex(idx);
    return g;
  }

  // key cap: extruded key outline with a soft rounded top (SCAD key_dome), bottom at z = 0, top at z = h
  function keyGeometry(k, h = 0.85) {
    const [, , w, kh, shape] = k, bs = 0.5, bt = 0.3, depth = Math.max(0.05, h - 2 * bt);
    let s;
    if (shape === 'rect') {
      const r = Math.max(0.25, 0.8 * kh / 2 - bs);
      s = rrShape(-w / 2 + bs, -kh / 2 + bs, w / 2 - bs, kh / 2 - bs, r, r);
    } else if (shape === 'round') {
      s = new THREE.Shape(); s.absarc(0, 0, w / 2 - bs, 0, TAU, false);
    } else {   // ring: the D-pad, with the hole for OK
      s = new THREE.Shape(); s.absarc(0, 0, w / 2 - bs, 0, TAU, false);
      const hole = new THREE.Path(); hole.absarc(0, 0, FOS.D.RING_HOLE / 2 + bs, 0, TAU, true); s.holes.push(hole);
    }
    const g = new THREE.ExtrudeGeometry(s, { depth, bevelEnabled: true, bevelThickness: bt, bevelSize: bs, bevelSegments: 5, curveSegments: 28 });
    g.translate(0, 0, bt);
    return g;
  }

  // printed key markings (SCAD key_label2d), drawn once into a canvas. White ink, Heebo like the rest of the film.
  function keyLabelTexture(k) {
    const [, , w, h, , name] = k, PX = 40;   // px per mm
    const cw = Math.round(w * PX), ch = Math.round(h * PX);
    const c = document.createElement('canvas'); c.width = cw; c.height = ch;
    const g = c.getContext('2d');
    g.fillStyle = '#ffffff'; g.strokeStyle = '#ffffff'; g.textAlign = 'center'; g.textBaseline = 'middle';
    const ts = Math.min(w, h);
    const icon = { softL: 'dehaze', call: 'call', softR: 'undo', end: 'call_end' }[name];
    const LATIN = ['', 'ABC', 'DEF', 'GHI', 'JKL', 'MNO', 'PQRS', 'TUV', 'WXYZ'];
    const HEB = ['', 'דהו', 'אבג', 'מנ', 'יכל', 'זחט', 'רשת', 'צק', 'סעפ'];
    if (name === 'dpad') return null;
    if (name === 'ok') {
      g.font = `700 ${ts * 0.3 * PX}px Heebo`; g.fillText('OK', cw / 2, ch / 2 + ts * 0.02 * PX);
    } else if (icon) {
      const size = ts * 0.46 * PX, sc = size / 24;
      g.save(); g.translate(cw / 2 - size / 2, ch / 2 - size / 2); g.scale(sc, sc);
      g.lineWidth = 2.1; g.lineCap = 'round'; g.lineJoin = 'round';
      for (const part of FOS.ICONS[icon].split('|')) {
        if (part.startsWith('d:')) { const [x, y] = part.slice(2).split(' ').map(Number); g.beginPath(); g.arc(x, y, 1.3, 0, TAU); g.fill(); }
        else g.stroke(new Path2D(part));
      }
      g.restore();
    } else if (name >= '2' && name <= '9') {
      const i = name.charCodeAt(0) - 49;
      g.font = `700 ${h * 0.52 * PX}px Heebo`; g.fillText(name, cw / 2 - w * 0.2 * PX, ch / 2 + h * 0.03 * PX);
      g.font = `500 ${h * 0.21 * PX}px Heebo`;
      g.direction = 'rtl'; g.fillText(HEB[i], cw / 2 + w * 0.17 * PX, ch / 2 - h * 0.19 * PX);
      g.direction = 'ltr'; g.fillText(LATIN[i], cw / 2 + w * 0.17 * PX, ch / 2 + h * 0.2 * PX);
    } else {
      g.font = `700 ${h * 0.52 * PX}px Heebo`; g.fillText(name, cw / 2, ch / 2 + h * (name === '*' ? 0.12 : 0.03) * PX);
    }
    const tex = new THREE.CanvasTexture(c);
    tex.colorSpace = THREE.SRGBColorSpace; tex.anisotropy = 8;
    return tex;
  }

  // Bernoulli lemniscate: the infinity mark
  function infinityCurve(a, n = 240) {
    const pts = [];
    for (let i = 0; i < n; i++) {
      const s = i / n * TAU, d = 1 + Math.sin(s) ** 2;
      pts.push(new THREE.Vector3(a * Math.cos(s) / d, a * Math.sin(s) * Math.cos(s) / d, 0));
    }
    return new THREE.CatmullRomCurve3(pts, true, 'centripetal');
  }
  FOS.infinityPoints = (a, n = 240) => {
    const out = [];
    for (let i = 0; i <= n; i++) { const s = i / n * TAU, d = 1 + Math.sin(s) ** 2; out.push([a * Math.cos(s) / d, a * Math.sin(s) * Math.cos(s) / d]); }
    return out;
  };

  FOS.buildPhone = (parent, W, H) => {
    const D = FOS.D;
    const canvas = document.createElement('canvas');
    canvas.style.cssText = `position:absolute;left:0;top:0;width:${W}px;height:${H}px`;
    parent.appendChild(canvas);
    const renderer = new THREE.WebGLRenderer({ canvas, antialias: true, alpha: false, preserveDrawingBuffer: true, powerPreference: 'high-performance' });
    renderer.setPixelRatio(window.devicePixelRatio || 1);
    renderer.setSize(W, H, false);
    renderer.setClearColor(0x000000, 1);
    renderer.toneMapping = THREE.ACESFilmicToneMapping;
    renderer.toneMappingExposure = 1.2;
    renderer.outputColorSpace = THREE.SRGBColorSpace;

    const scene = new THREE.Scene();
    const camera = new THREE.PerspectiveCamera(18, W / H, 20, 4000);

    // studio: soft boxes baked into an environment map (reflections) + two direct lights (shape)
    const env = new THREE.Scene();
    env.background = new THREE.Color(0x000000);
    const softbox = (w, h, p, k) => {
      const m = new THREE.Mesh(new THREE.PlaneGeometry(w, h), new THREE.MeshBasicMaterial({ color: new THREE.Color(k, k, k), side: THREE.DoubleSide }));
      m.position.set(...p); m.lookAt(0, 0, 0); env.add(m);
    };
    softbox(20, 1.1, [0, 8, 7], 0.9);   // overhead strip: a band of light that travels across the glass as it tilts
    softbox(4, 3, [-5, 6, 6], 2.2);      // key: high, front left (small: a defined highlight, not a grey wash)
    softbox(1.6, 14, [-10, 0, 3], 3.2);  // strip left, a little in front: the line along the left edge
    softbox(1.6, 14, [10, 0, 2], 3.8);   // strip right
    softbox(1.4, 12, [9, 1, -5], 3.0);   // rim strips behind: the walls in 3/4 and profile
    softbox(1.4, 12, [-9, 0, -5], 2.2);
    softbox(14, 1.2, [0, 9, 0], 2.2);    // top strip: a line of light along the top edges
    softbox(12, 4, [2, -8, 4], 0.2);     // floor bounce
    const pmrem = new THREE.PMREMGenerator(renderer);
    scene.environment = pmrem.fromScene(env, 0.03).texture;
    const key = new THREE.DirectionalLight(0xffffff, 1.6); key.position.set(-260, 380, 420); scene.add(key);
    const rim = new THREE.DirectionalLight(0xffffff, 2.2); rim.position.set(420, 140, -300); scene.add(rim);
    const rim2 = new THREE.DirectionalLight(0xffffff, 1.2); rim2.position.set(-420, 60, -260); scene.add(rim2);

    const root = new THREE.Group(); root.rotation.order = 'YXZ'; scene.add(root);
    const body = new THREE.Group(); body.position.set(-D.CX, -D.BODY_L / 2, D.BODY_T / 2); root.add(body);

    const M = {
      body: new THREE.MeshPhysicalMaterial({ color: D.BODY_COLOR, roughness: 0.6, metalness: 0, sheen: 0.12, sheenRoughness: 0.6, sheenColor: new THREE.Color(0x9a9aa2) }),
      glass: new THREE.MeshPhysicalMaterial({ color: 0x020203, roughness: 0.08, metalness: 0, clearcoat: 0.5, clearcoatRoughness: 0.05, reflectivity: 0.3 }),
      keys: new THREE.MeshPhysicalMaterial({ color: D.KEYPAD_COLOR, roughness: 0.5, metalness: 0, sheen: 0.35, sheenRoughness: 0.5, sheenColor: new THREE.Color(0xa0a0a8) }),
      gap: new THREE.MeshBasicMaterial({ color: 0x020202 }),
      black: new THREE.MeshStandardMaterial({ color: 0x010101, roughness: 0.25 }),
      lens: new THREE.MeshPhysicalMaterial({ color: 0x050608, roughness: 0.04, clearcoat: 1, clearcoatRoughness: 0.02 }),
      flash: new THREE.MeshStandardMaterial({ color: 0xcfcac0, roughness: 0.35, emissive: 0x141312 }),
      ring: new THREE.MeshPhysicalMaterial({ color: 0x222226, roughness: 0.28, metalness: 0.2, clearcoat: 0.6 }),
      inf: new THREE.MeshPhysicalMaterial({ color: 0x8c8c92, roughness: 0.14, metalness: 1.0 }),
    };
    const meshes = [];
    const add = (geo, mat, parentGroup = body) => { const m = new THREE.Mesh(geo, mat); parentGroup.add(m); meshes.push(m); return m; };

    add(bodyGeometry(D), M.body);
    // glass: the window, edge to edge, flush with the front
    const glass = add(new THREE.ShapeGeometry(rrShape(D.SIDE_WALL, D.WIN_Y0, D.BODY_W - D.SIDE_WALL, D.WIN_Y1, D.WIN_R_TOP, D.WIN_R_BOT), 24), M.glass);
    glass.position.z = 0.02;
    // front camera hole and the earpiece slot
    const punch = add(new THREE.CircleGeometry(D.PUNCH_D / 2, 40), M.black); punch.position.set(D.PUNCH[0], D.PUNCH[1], 0.03);
    const ear = add(new THREE.ShapeGeometry(rrShape(-5, -0.4, 5, 0.4, 0.39, 0.39), 8), M.gap); ear.position.set(D.EARPIECE[0], D.EARPIECE[1], 0.021);
    // keypad: one membrane in one opening (SCAD keypad_outline), a dark gap around it
    const [kx0, ky0, kx1, ky1] = D.KP, m0 = D.KP_MARGIN;
    const gapM = add(new THREE.ShapeGeometry(rrShape(kx0 - m0 - D.GAP, ky0 - m0 - D.GAP, kx1 + m0 + D.GAP, ky1 + m0 + D.GAP, 3 + D.GAP, 3 + D.GAP), 12), M.gap);
    gapM.position.z = 0.012;
    const mem = add(new THREE.ShapeGeometry(rrShape(kx0 - m0, ky0 - m0, kx1 + m0, ky1 + m0, 3, 3), 12), M.keys);
    mem.position.z = 0.016;

    // keys: a group per key so a press can move it
    const keys = {};
    for (const k of D.frontKeys) {
      const g = new THREE.Group(); g.position.set(k[0], k[1], 0.016); body.add(g);
      const cap = add(keyGeometry(k), M.keys, g);
      const tex = keyLabelTexture(k);
      if (tex) {
        const lm = new THREE.MeshBasicMaterial({ map: tex, transparent: true, depthWrite: false, color: 0xd9d9d9, toneMapped: false });
        const decal = new THREE.Mesh(new THREE.PlaneGeometry(k[2], k[3]), lm); decal.position.z = 0.855; g.add(decal);
        cap.userData.decal = decal;
      }
      keys[k[5]] = { g, k, cap };
    }
    // side keys: vol+, vol-, power on the right wall
    for (const [y, l, kind] of D.sideKeys) {
      const r = D.VOL_H / 2, s = new THREE.Shape();
      s.absarc(0, (l - D.VOL_H) / 2, r, 0, Math.PI, false); s.absarc(0, -(l - D.VOL_H) / 2, r, Math.PI, TAU, false);
      const geo = new THREE.ExtrudeGeometry(s, { depth: 0.35, bevelEnabled: true, bevelThickness: 0.25, bevelSize: 0.22, bevelSegments: 4, curveSegments: 20 });
      geo.rotateY(Math.PI / 2);
      const m = add(geo, M.keys); m.position.set(D.BODY_W, y, D.VOL_Z);   // outer face 0.6 mm proud of the wall
      keys[kind] = { g: m, k: null };
    }
    // back: camera ring, lens, flash, the infinity mark
    const bump = new THREE.LatheGeometry([[3.0, -0.02], [3.0, 0.32], [3.25, 0.44], [4.25, 0.44], [4.6, 0.0]].map(p => new THREE.Vector2(p[0], p[1])), 64);
    bump.rotateX(-Math.PI / 2);
    const bm = add(bump, M.ring); bm.position.set(D.CAM_XY[0], D.CAM_XY[1], -D.BODY_T);
    const lensTex = (() => {
      const c = document.createElement('canvas'); c.width = c.height = 256; const g = c.getContext('2d');
      g.fillStyle = '#050608'; g.fillRect(0, 0, 256, 256);
      g.strokeStyle = 'rgba(255,255,255,0.10)'; g.lineWidth = 3; g.beginPath(); g.arc(128, 128, 96, 0, TAU); g.stroke();
      g.strokeStyle = 'rgba(255,255,255,0.06)'; g.beginPath(); g.arc(128, 128, 60, 0, TAU); g.stroke();
      g.fillStyle = '#000'; g.beginPath(); g.arc(128, 128, 44, 0, TAU); g.fill();
      const t = new THREE.CanvasTexture(c); t.colorSpace = THREE.SRGBColorSpace; return t;
    })();
    M.lens.map = lensTex;
    const lens = add(new THREE.CircleGeometry(3.0, 48), M.lens); lens.position.set(D.CAM_XY[0], D.CAM_XY[1], -D.BODY_T - 0.12); lens.rotation.y = Math.PI;
    const flash = add(new THREE.CircleGeometry(1.5, 32), M.flash); flash.position.set(D.FLASH_XY[0], D.FLASH_XY[1], -D.BODY_T - 0.01); flash.rotation.y = Math.PI;
    const inf = add(new THREE.TubeGeometry(infinityCurve(D.INF.w / 2), 420, D.INF.stroke / 2, 14, true), M.inf);
    inf.scale.set(1, 1, 0.16); inf.position.set(D.INF.x, D.INF.y, -D.BODY_T - 0.02);

    // ------------------------------------------------------------------ API
    const v = new THREE.Vector3();
    const api = {
      canvas, renderer, scene, camera, root, body, keys, M,
      // pose of the phone in world mm (centre of the body) and degrees
      pose(p) {
        root.position.set(p.x || 0, p.y || 0, p.z || 0);
        root.rotation.set((p.pitch || 0) * DEG, (p.yaw || 0) * DEG, (p.roll || 0) * DEG, 'YXZ');
      },
      // camera straight down -Z at (tx, ty, dist), looking at (tx, ty, 0)
      cam(c) {
        camera.fov = c.fov || 18; camera.position.set(c.tx || 0, c.ty || 0, c.dist);
        camera.lookAt(c.tx || 0, c.ty || 0, 0); camera.updateProjectionMatrix();
      },
      update() { root.updateMatrixWorld(true); camera.updateMatrixWorld(true); },
      // a point in SCAD mm -> screen px (x, y) and depth; call update() first
      project(x, y, z = 0) {
        v.set(x, y, z); body.localToWorld(v); v.project(camera);
        return [(v.x + 1) / 2 * W, (1 - v.y) / 2 * H, v.z];
      },
      // how much the front (sign +1) or the back (-1) faces the camera: 1 straight on, 0 edge on
      facing(sign = 1) {
        const n = new THREE.Vector3(0, 0, sign).applyQuaternion(root.quaternion).normalize();
        const c = new THREE.Vector3().setFromMatrixPosition(root.matrixWorld);
        const toCam = camera.position.clone().sub(c).normalize();
        return n.dot(toCam);
      },
      // key press: depth in mm (0..0.5), D-pad tilt as a direction [dx, dy] with magnitude 0..1
      press(depths, tilt) {
        for (const name in keys) {
          const K = keys[name];
          if (!K.k) continue;
          const d = depths[name] || 0;
          K.g.position.z = 0.016 - d;
          if (K.cap.userData.decal) K.cap.userData.decal.material.opacity = 1 - 0.55 * clamp(d / 0.7, 0, 1);   // the key drops out of the light
          if (name === 'dpad') {
            const [tx, ty] = tilt || [0, 0];
            K.g.rotation.set(-ty * 5 * DEG, tx * 5 * DEG, 0);   // the pressed side of the rocker goes down
          }
        }
      },
      render() { renderer.render(scene, camera); },
    };
    return api;
  };
})();
