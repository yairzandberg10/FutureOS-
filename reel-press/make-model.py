"""Builds assets/phone-model.js: the real FutureOS Regular, straight from the repo's 3D model, for the page to draw with three.js.

  python3 make-model.py

Nothing is redrawn. The solids are the STLs that hardware/phone3d/export-parts.sh exports from openscad/futureos_family.scad
(front shell, back, side keys, glass, keypad) plus the key legends from hardware/print/regular_markings.stl, in the same
assembly coordinates the Blender renders use (the legends move by -FRONT_T, like assembly()).

What this script adds:
  * the page's units: 1 unit = 1 screen pixel. The display area of the model (x 1.84..51.16, y 68.0..141.97 mm) lands exactly
    on the 640 x 960 screen at the origin, so the UI drawn in the DOM sits on the model's own display. Y is up (three.js).
  * the keypad split per key (by the key rectangles of futureos_family.scad), so each key can be pressed on its own; the
    membrane stays one piece. The legends are split the same way.
  * smooth normals with a 40 degree crease angle, like the smooth shading of the renders.
"""
import base64, json, math
from pathlib import Path

import numpy as np

HERE = Path(__file__).resolve().parent
HW = HERE.parent / "hardware"
STL = HW / "phone3d" / "stl"

# ------------------------------------------------------------------ the Regular, from futureos_family.scad
DIAG_IN, SCR_AR = 3.5, 2 / 3
SCR_W = DIAG_IN * 25.4 * SCR_AR / math.sqrt(1 + SCR_AR ** 2)
SCR_H = DIAG_IN * 25.4 / math.sqrt(1 + SCR_AR ** 2)
LCD_SIDE, LCD_BOTTOM, TOP_BAND, NAV_GAP, NAV_H, ROW_P, KEY_P, BOTTOM_BAND, SIDE_WALL = 0.8, 3.5, 2.5, 1.5, 20, 9.5, 17, 5, 1.0
Y_DIGITS = BOTTOM_BAND
Y_NAV = Y_DIGITS + 4 * ROW_P
Y_SCR = Y_NAV + NAV_H + NAV_GAP
BODY_L = Y_SCR + SCR_H + LCD_SIDE + LCD_BOTTOM + TOP_BAND
BODY_W = max(SCR_W + 2 * LCD_SIDE, 3 * KEY_P) + 2 * SIDE_WALL
CX = BODY_W / 2
RING_D = NAV_H - 2
SIDE_KEY_W, SIDE_KEY_H = KEY_P - 2.4, (NAV_H - 3) / 2
PUNCH_D = 3.2
CORNER_R = 7
FRONT_T = 1.0

# the display area (the plane the Blender scene maps the launcher onto)
DX0, DY1 = CX - SCR_W / 2, Y_SCR + LCD_BOTTOM + SCR_H
MM = 640 / SCR_W                                # px per mm: 12.98

# key id on the page -> (x, y, w, h, shape) in mm
KEYS = {}
for row in range(4):
    for col in range(3):
        n = ["1", "2", "3", "4", "5", "6", "7", "8", "9", "*", "0", "#"][row * 3 + col]
        kid = {"*": "star", "#": "pound"}.get(n, "d" + n)
        KEYS[kid] = (CX + (col - 1) * KEY_P, Y_DIGITS + (3 - row + 0.5) * ROW_P, KEY_P - 2.4, ROW_P - 1.6, "rect")
KEYS["ring"] = (CX, Y_NAV + NAV_H / 2, RING_D, RING_D, "ring")
KEYS["ok"] = (CX, Y_NAV + NAV_H / 2, RING_D * 0.42, RING_D * 0.42, "round")
KEYS["soft_l"] = (CX - KEY_P, Y_NAV + NAV_H * 0.75, SIDE_KEY_W, SIDE_KEY_H, "rect")
KEYS["call"] = (CX - KEY_P, Y_NAV + NAV_H * 0.25, SIDE_KEY_W, SIDE_KEY_H, "rect")
KEYS["soft_r"] = (CX + KEY_P, Y_NAV + NAV_H * 0.75, SIDE_KEY_W, SIDE_KEY_H, "rect")
KEYS["end"] = (CX + KEY_P, Y_NAV + NAV_H * 0.25, SIDE_KEY_W, SIDE_KEY_H, "rect")


def read_stl(path, dz=0.0):
    """ASCII or binary STL -> (n, 3, 3) float64 triangles in mm."""
    raw = path.read_bytes()
    if raw[:5] == b"solid" and b"facet" in raw[:400]:
        v = [l.split()[1:] for l in raw.decode("ascii", "replace").splitlines() if l.lstrip().startswith("vertex")]
        tri = np.array(v, dtype=np.float64).reshape(-1, 3, 3)
    else:
        n = int(np.frombuffer(raw, np.uint32, 1, 80)[0])
        rec = np.frombuffer(raw, np.dtype([("n", "<f4", 3), ("v", "<f4", (3, 3)), ("a", "<u2")]), n, 84)
        tri = rec["v"].astype(np.float64)
    tri[..., 2] += dz
    return tri


def key_of(c, grow=0.35):
    """The key whose rectangle holds the point c (x, y), or None."""
    x, y = c
    for kid in ("ok", "ring") + tuple(k for k in KEYS if k not in ("ok", "ring")):
        kx, ky, w, h, shape = KEYS[kid]
        if shape == "rect":
            if abs(x - kx) <= w / 2 + grow and abs(y - ky) <= h / 2 + grow:
                return kid
        else:
            r = math.hypot(x - kx, y - ky)
            if kid == "ok" and r <= w / 2 + grow:
                return kid
            if kid == "ring" and r <= w / 2 + grow:
                return kid
    return None


def smooth_normals(tri, crease=40):
    """Per-corner normals: the average of the faces around the vertex that are within the crease angle of this face."""
    fn = np.cross(tri[:, 1] - tri[:, 0], tri[:, 2] - tri[:, 0])
    area = np.linalg.norm(fn, axis=1, keepdims=True)
    fn = fn / np.maximum(area, 1e-12)
    pts = tri.reshape(-1, 3)
    key = np.round(pts * 1e4).astype(np.int64)
    _, vid = np.unique(key, axis=0, return_inverse=True)
    vid = vid.reshape(-1)
    nf = len(tri)
    face = np.repeat(np.arange(nf), 3)
    # for each vertex, the list of (face) that touch it
    order = np.argsort(vid, kind="stable")
    vs, fs = vid[order], face[order]
    starts = np.r_[0, np.flatnonzero(np.diff(vs)) + 1, len(vs)]
    cosc = math.cos(math.radians(crease))
    out = np.zeros((nf * 3, 3))
    wfn = fn * area            # area weighted
    for a, b in zip(starts[:-1], starts[1:]):
        faces = fs[a:b]
        corners = order[a:b]
        n = fn[faces]
        dots = n @ n.T
        acc = (dots >= cosc).astype(np.float64) @ wfn[faces]
        out[corners] = acc
    out /= np.maximum(np.linalg.norm(out, axis=1, keepdims=True), 1e-12)
    bad = ~np.isfinite(out).all(1) | (np.linalg.norm(out, axis=1) < 0.5)
    out[bad] = np.repeat(fn, 3, axis=0)[bad]
    return out.reshape(-1, 3, 3)


def to_px(tri):
    """mm (model) -> page units: x from the display's left edge, y up from the display's top edge, 1 unit = 1 screen px."""
    p = tri.copy()
    p[..., 0] = (p[..., 0] - DX0) * MM
    p[..., 1] = (p[..., 1] - DY1) * MM
    p[..., 2] = p[..., 2] * MM
    return p


def pack(tri, nrm):
    pos = to_px(tri).astype(np.float32).reshape(-1)
    n = np.clip(np.round(nrm.reshape(-1) * 127), -127, 127).astype(np.int8)
    return {"p": base64.b64encode(pos.tobytes()).decode(), "n": base64.b64encode(n.tobytes()).decode(), "count": len(tri) * 3}


def main():
    parts = {}
    # the body: what the front and the sides show
    for name, file, mat in [("front", "regular_front_shell.stl", "body"), ("back", "regular_back_final.stl", "body"),
                            ("side_keys", "regular_side_keys.stl", "key"), ("glass", "regular_glass.stl", "glass")]:
        tri = read_stl(STL / file)
        parts[name] = dict(pack(tri, smooth_normals(tri)), mat=mat)
        print(name, len(tri), "triangles")

    # the keypad: the membrane, and every key on its own
    tri = read_stl(STL / "regular_keypad.stl")
    cen = tri.mean(axis=1)
    top = tri[..., 2].max(axis=1)
    membrane_z = -0.3 + 0.02            # the membrane's top face is at FRONT_T - KP_RECESS, -FRONT_T in assembly
    groups = {}
    for i in range(len(tri)):
        k = key_of(cen[i, :2]) if top[i] > membrane_z else None
        groups.setdefault(k or "membrane", []).append(i)
    nrm = smooth_normals(tri)
    for k, idx in groups.items():
        idx = np.array(idx)
        parts["key:" + k if k != "membrane" else "membrane"] = dict(pack(tri[idx], nrm[idx]), mat="key")
    print("keypad", len(tri), "triangles,", len(groups) - 1, "keys")

    # the legends (print/regular_markings.stl), moved by -FRONT_T like assembly()
    tri = read_stl(HW / "print" / "regular_markings.stl", dz=-FRONT_T)
    cen = tri.mean(axis=1)
    nrm = smooth_normals(tri)
    lg = {}
    for i in range(len(tri)):
        k = key_of(cen[i, :2], grow=0.8) or "none"
        lg.setdefault(k, []).append(i)
    for k, idx in lg.items():
        idx = np.array(idx)
        parts["legend:" + k] = dict(pack(tri[idx], nrm[idx]), mat="legend")
    print("legends", len(tri), "triangles:", sorted(lg))

    meta = {
        "mm": MM,
        "body": {"w": BODY_W, "l": BODY_L, "x0": -DX0 * MM, "y0": -DY1 * MM, "x1": (BODY_W - DX0) * MM, "y1": (BODY_L - DY1) * MM},
        # key centres and sizes in model units (y up)
        "keys": {k: {"x": (v[0] - DX0) * MM, "y": (v[1] - DY1) * MM, "w": v[2] * MM, "h": v[3] * MM} for k, v in KEYS.items()},
        # the glass window of the front shell (WIN_* in the SCAD): the screen is seen through it, so its rounded top corners
        # cut the display's corners
        "window": {"x0": (SIDE_WALL - DX0) * MM, "x1": (BODY_W - SIDE_WALL - DX0) * MM, "y0": (DY1 - (BODY_L - TOP_BAND)) * MM,
                   "y1": (DY1 - Y_SCR) * MM, "rTop": max(CORNER_R - SIDE_WALL, 1) * MM, "rBot": 2 * MM},
        # the front camera's punch hole in the display, in screen px (y down)
        "punch": {"x": (CX - DX0) * MM, "y": (DY1 - (BODY_L - TOP_BAND - LCD_SIDE - PUNCH_D)) * MM, "d": PUNCH_D * MM},
    }
    js = "window.PHONE_MODEL = " + json.dumps({"meta": meta, "parts": parts}, separators=(",", ":")) + ";\n"
    out = HERE / "assets" / "phone-model.js"
    out.write_text(js)
    print(f"body {BODY_W:.2f} x {BODY_L:.2f} mm, {MM:.3f} px/mm -> {out.name} ({len(js) / 1e6:.1f} MB)")


if __name__ == "__main__":
    main()
