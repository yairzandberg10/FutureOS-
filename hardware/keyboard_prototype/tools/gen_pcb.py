"""Generate keyboard_prototype.kicad_pcb from the schematic netlist + design.py.

1. reads keyboard_prototype.net (kicad-cli sch export netlist) -> parts & nets
2. places every footprint (front: switches/diodes/LEDs, back: XIAO/FPC/JP1)
3. routes all nets on 2 layers with a grid maze router (rip-up & retry)
4. saves the board; DRC + fab outputs are done by build.sh via kicad-cli
"""
import heapq
import math
import os
import random
import sys

import numpy as np
import pcbnew

from design import *
from sexpr import find, first, parse

OX, OY = 100.0, 100.0          # KiCad position of board bottom-left corner... (see kxy)
RES = 0.25                     # router grid (mm)
TRACK_W = 0.25
CLEAR = 0.2
VIA_D, VIA_DRILL = 0.6, 0.3
EDGE_CLEAR = 0.3
R_TRACK = CLEAR + TRACK_W / 2 + 0.02   # centre-line keep-away from foreign copper
R_VIA = CLEAR + VIA_D / 2 + 0.05
MM = pcbnew.FromMM


def kxy(x, y):
    """board coords (front view, Y up) -> KiCad mm (Y down)."""
    return OX + x, OY + BOARD_H - y


def V(x, y):
    return pcbnew.VECTOR2I(MM(x), MM(y))


# ---------------------------------------------------------------------------
# netlist
# ---------------------------------------------------------------------------
def read_netlist(path):
    t = parse(open(path, encoding="utf-8").read())
    comps = {}
    for c in find(first(t, "components"), "comp"):
        ref = first(c, "ref")[1]
        fields = {}
        for fld in find(first(c, "fields") or [], "field"):
            name = first(fld, "name")[1]
            if name not in ("Footprint", "Datasheet", "Description") and len(fld) > 2:
                fields[name] = fld[2]
        props = {first(pr, "name")[1] for pr in find(c, "property")}
        comps[ref] = {
            "value": first(c, "value")[1],
            "footprint": first(c, "footprint")[1],
            "uuid": first(c, "tstamps")[1] if first(c, "tstamps") else "",
            "fields": fields,
            "no_bom": "exclude_from_bom" in props,
        }
    pin_net = {}
    for n in find(first(t, "nets"), "net"):
        name = first(n, "name")[1]
        for node in find(n, "node"):
            pin_net[(first(node, "ref")[1], first(node, "pin")[1])] = name
    return comps, pin_net


# ---------------------------------------------------------------------------
# placement: ref -> (x, y, rot, side)   (board coords, rot in degrees CCW)
# ---------------------------------------------------------------------------
def placements():
    P = {}
    diode_at = {"LEFT": (CX - 11.4, NAV_Y), "RIGHT": (CX + 11.4, NAV_Y),
                "UP": (CX - 5.2, NAV_Y + DPAD_R), "OK": (CX + 5.2, NAV_Y + DPAD_R)}
    for i, (lab, x, y, rot, r, c, code) in enumerate(KEYS, start=1):
        P["SW%d" % i] = (x, y, rot, "F")
        dx, dy = diode_at.get(lab, (x + 5.2, y))
        P["D%d" % i] = (dx, dy, 90, "F")
    # backlight: LED + resistor pairs in the gaps between digit columns
    led_pos = [(CX - KEY_P / 2, y) for y in (35.25, 25.75, 16.25, 6.75)] + \
              [(CX + KEY_P / 2, y) for y in (35.25, 25.75, 16.25, 6.75)]
    for i, (x, y) in enumerate(led_pos, start=1):
        P["LED%d" % i] = (x, y - 1.3, 0, "F")
        P["R%d" % i] = (x, y + 1.3, 0, "F")
    P["Q1"] = (CX - 9.0, 40.4, 0, "F")
    P["R9"] = (CX - 4.9, 39.4, 0, "F")
    # back side
    P["U1"] = (CX, 11.0, 0, "B")       # XIAO: pad-field centre; USB toward bottom edge
    P["J1"] = (CX, 31.0, 0, "B")       # FPC: back side, centre (mainboard sits behind the keypad)
    P["JP1"] = (CX + 12.5, 22.0, 0, "B")
    for i, (x, y) in enumerate(HOLES, start=1):
        P["H%d" % i] = (x, y, 0, "F")
    return P


def lib_path(lib):
    if lib == LOCAL_FP_LIB:
        return os.path.join(PROJ_DIR, LOCAL_FP_LIB + ".pretty")
    return FPS + lib + ".pretty"


def pads_centre(fp):
    xs = [p.GetPosition().x for p in fp.Pads() if p.GetNumber() not in ("15", "16", "17", "18", "19", "20")]
    ys = [p.GetPosition().y for p in fp.Pads() if p.GetNumber() not in ("15", "16", "17", "18", "19", "20")]
    return (min(xs) + max(xs)) / 2, (min(ys) + max(ys)) / 2


def build_board(comps, pin_net):
    board = pcbnew.BOARD()
    ds = board.GetDesignSettings()
    ds.SetBoardThickness(MM(BOARD_T))
    ds.m_CopperEdgeClearance = MM(EDGE_CLEAR)
    ds.m_MinClearance = MM(CLEAR)
    ds.m_TrackMinWidth = MM(0.2)
    ds.m_ViasMinSize = MM(VIA_D)
    ds.m_MinThroughDrill = MM(VIA_DRILL)
    nc = ds.m_NetSettings.GetDefaultNetclass()
    nc.SetClearance(MM(CLEAR))
    nc.SetTrackWidth(MM(TRACK_W))
    nc.SetViaDiameter(MM(VIA_D))
    nc.SetViaDrill(MM(VIA_DRILL))

    nets = {}
    for name in sorted(set(pin_net.values())):
        ni = pcbnew.NETINFO_ITEM(board, name)
        board.Add(ni)
        nets[name] = ni

    P = placements()
    fps = {}
    for ref, c in comps.items():
        lib, name = c["footprint"].split(":")
        fp = pcbnew.FootprintLoad(lib_path(lib), name)
        if fp is None:
            sys.exit("footprint not found: " + c["footprint"])
        fp.SetFPID(pcbnew.LIB_ID(lib, name))
        fp.SetReference(ref)
        fp.SetValue(c["value"])
        if c["uuid"]:
            fp.SetPath(pcbnew.KIID_PATH("/" + c["uuid"]))
        board.Add(fp)
        x, y, rot, side = P[ref]
        kx, ky = kxy(x, y)
        fp.SetPosition(V(0, 0))
        fp.SetOrientationDegrees(rot)
        if side == "B":
            fp.Flip(fp.GetPosition(), pcbnew.FLIP_DIRECTION_LEFT_RIGHT)
            if ref == "U1":
                # USB end (pads 1/14) must face the board's bottom edge (+Y in KiCad)
                pad = {p.GetNumber(): p for p in fp.Pads()}
                if pad["1"].GetPosition().y < pad["7"].GetPosition().y:
                    fp.SetOrientationDegrees(fp.GetOrientationDegrees() + 180)
                pad = {p.GetNumber(): p for p in fp.Pads()}
                assert pad["1"].GetPosition().y > pad["7"].GetPosition().y, "XIAO USB not at bottom edge"
            if ref == "J1":
                fp.SetOrientationDegrees(180)
        # move so the pad field centre lands on (kx, ky)
        cx, cy = pads_centre(fp) if ref.startswith(("U", "J")) else (fp.GetPosition().x, fp.GetPosition().y)
        fp.Move(V(kx, ky) - pcbnew.VECTOR2I(int(cx), int(cy)))
        for k, v in c["fields"].items():
            fp.SetField(k, v)
            fp.GetField(k).SetVisible(False)
        fp.SetExcludedFromBOM(c["no_bom"])
        if ref.startswith(("SW", "D", "LED", "R", "Q", "H")):
            fp.Reference().SetVisible(False)
        if ref == "U1":
            fp.Reference().SetTextSize(V(1.0, 1.0))
            fp.Reference().SetTextThickness(MM(0.15))
        for pad in fp.Pads():
            net = pin_net.get((ref, pad.GetNumber()))
            if net in nets:
                pad.SetNet(nets[net])
        fps[ref] = fp

    # diode orientation: anode (pad 2) toward its switch's pad 2
    for i in range(1, len(KEYS) + 1):
        d, sw = fps["D%d" % i], fps["SW%d" % i]
        a = [p for p in d.Pads() if p.GetNumber() == "2"][0].GetPosition()
        s2 = [p for p in sw.Pads() if p.GetNumber() == "2"]
        best = min(math.hypot(p.GetPosition().x - a.x, p.GetPosition().y - a.y) for p in s2)
        k = [p for p in d.Pads() if p.GetNumber() == "1"][0].GetPosition()
        other = min(math.hypot(p.GetPosition().x - k.x, p.GetPosition().y - k.y) for p in s2)
        if other < best:
            d.SetOrientationDegrees(d.GetOrientationDegrees() + 180)

    # outline: rounded rectangle on Edge.Cuts
    W, H, R = BOARD_W, BOARD_H, BOARD_R
    x0, y0 = kxy(0, H)
    x1, y1 = kxy(W, 0)

    def seg(a, b):
        s = pcbnew.PCB_SHAPE(board)
        s.SetShape(pcbnew.SHAPE_T_SEGMENT)
        s.SetStart(V(*a))
        s.SetEnd(V(*b))
        s.SetLayer(pcbnew.Edge_Cuts)
        s.SetWidth(MM(0.1))
        board.Add(s)

    def arc(c, a, b):
        s = pcbnew.PCB_SHAPE(board)
        s.SetShape(pcbnew.SHAPE_T_ARC)
        m = ((a[0] + b[0]) / 2 - c[0], (a[1] + b[1]) / 2 - c[1])
        L = math.hypot(*m)
        mid = (c[0] + m[0] / L * R, c[1] + m[1] / L * R)
        s.SetArcGeometry(V(*a), V(*mid), V(*b))
        s.SetLayer(pcbnew.Edge_Cuts)
        s.SetWidth(MM(0.1))
        board.Add(s)

    seg((x0 + R, y0), (x1 - R, y0))
    seg((x1, y0 + R), (x1, y1 - R))
    seg((x1 - R, y1), (x0 + R, y1))
    seg((x0, y1 - R), (x0, y0 + R))
    arc((x0 + R, y0 + R), (x0, y0 + R), (x0 + R, y0))
    arc((x1 - R, y0 + R), (x1 - R, y0), (x1, y0 + R))
    arc((x1 - R, y1 - R), (x1, y1 - R), (x1 - R, y1))
    arc((x0 + R, y1 - R), (x0 + R, y1), (x0, y1 - R))

    # silkscreen
    def txt(s, x, y, layer, size=1.0, mirror=False):
        t = pcbnew.PCB_TEXT(board)
        t.SetText(s)
        t.SetPosition(V(*kxy(x, y)))
        t.SetLayer(layer)
        t.SetTextSize(V(size, size))
        t.SetTextThickness(MM(size * 0.15))
        if mirror:
            t.SetMirrored(True)
        board.Add(t)

    txt("FutureOS Keyboard Prototype rev A", CX, 44.0, pcbnew.B_SilkS, 1.2, True)
    txt("XIAO RP2040 - prototype only", CX, 41.5, pcbnew.B_SilkS, 0.9, True)
    txt("USB-C", CX + 12.5, 3.0, pcbnew.B_SilkS, 0.8, True)
    txt("J1: FPC to mainboard", CX, 38.5, pcbnew.B_SilkS, 0.8, True)
    txt("JP1 closed = LEDs from USB", CX + 14.0, 25.0, pcbnew.B_SilkS, 0.8, True)
    return board, fps, nets


# ---------------------------------------------------------------------------
# grid maze router
# ---------------------------------------------------------------------------
class Grid:
    def __init__(self, board, fps, nets):
        self.nx = int(round(BOARD_W / RES)) + 1
        self.ny = int(round(BOARD_H / RES)) + 1
        xs = OX + np.arange(self.nx) * RES
        ys = OY + np.arange(self.ny) * RES
        self.X, self.Y = np.meshgrid(xs, ys, indexing="ij")
        self.net_id = {n: i + 1 for i, n in enumerate(sorted(nets))}
        self.id_net = {v: k for k, v in self.net_id.items()}
        # owner maps per layer: 0 free, -1 blocked, n = only net n
        self.tmap = np.zeros((2, self.nx, self.ny), dtype=np.int32)
        self.vmap = np.zeros((2, self.nx, self.ny), dtype=np.int32)
        self.pads = []          # (layers, rect, net_id)
        self._edge()
        for fp in fps.values():
            for pad in fp.Pads():
                bb = pad.GetBoundingBox()
                rect = (pcbnew.ToMM(bb.GetLeft()), pcbnew.ToMM(bb.GetTop()),
                        pcbnew.ToMM(bb.GetRight()), pcbnew.ToMM(bb.GetBottom()))
                layers = [li for li, L in enumerate((pcbnew.F_Cu, pcbnew.B_Cu)) if pad.IsOnLayer(L)]
                nid = self.net_id.get(pad.GetNetname(), -1) if pad.GetNetname() else -1
                if pad.GetAttribute() == pcbnew.PAD_ATTRIB_NPTH:
                    r = pcbnew.ToMM(pad.GetDrillSize().x) / 2
                    c = ((rect[0] + rect[2]) / 2, (rect[1] + rect[3]) / 2)
                    for li in (0, 1):
                        self._mark_circle(li, c, r, -1)
                    continue
                self.pads.append((layers, rect, nid, pad))
                for li in layers:
                    self._mark_rect(li, rect, nid)

    # -- marking ------------------------------------------------------------
    def _apply(self, li, mask_t, mask_v, nid):
        for m, mask in ((self.tmap, mask_t), (self.vmap, mask_v)):
            cur = m[li]
            if nid == -1:
                cur[mask] = -1
            else:
                sel = mask & (cur == 0)
                cur[sel] = nid
                cur[mask & (cur != nid)] = -1

    def _dist_rect(self, rect):
        dx = np.maximum(np.maximum(rect[0] - self.X, self.X - rect[2]), 0)
        dy = np.maximum(np.maximum(rect[1] - self.Y, self.Y - rect[3]), 0)
        return np.hypot(dx, dy)

    def _mark_rect(self, li, rect, nid):
        d = self._dist_rect(rect)
        self._apply(li, d < R_TRACK, d < R_VIA, nid)

    def _mark_circle(self, li, c, r, nid):
        d = np.hypot(self.X - c[0], self.Y - c[1]) - r
        self._apply(li, d < R_TRACK, d < R_VIA, nid)

    def _mark_seg(self, li, a, b, half, nid):
        ax, ay = a
        bx, by = b
        vx, vy = bx - ax, by - ay
        L2 = vx * vx + vy * vy or 1e-9
        t = np.clip(((self.X - ax) * vx + (self.Y - ay) * vy) / L2, 0, 1)
        d = np.hypot(self.X - (ax + t * vx), self.Y - (ay + t * vy)) - half
        self._apply(li, d < R_TRACK, d < R_VIA, nid)

    def _edge(self):
        # distance to the inside of the rounded-rect outline
        x0, y0 = OX, OY
        x1, y1 = OX + BOARD_W, OY + BOARD_H
        R = BOARD_R
        cx = np.clip(self.X, x0 + R, x1 - R)
        cy = np.clip(self.Y, y0 + R, y1 - R)
        inside_d = R - np.hypot(self.X - cx, self.Y - cy)  # distance to edge
        lim = EDGE_CLEAR + TRACK_W / 2 + 0.05
        bad = inside_d < lim
        self.tmap[:, bad] = -1
        badv = inside_d < EDGE_CLEAR + VIA_D / 2 + 0.05
        self.vmap[:, badv] = -1

    # -- routing --------------------------------------------------------------
    def cell(self, x, y):
        return int(round((x - OX) / RES)), int(round((y - OY) / RES))

    def pad_cells(self, rect, layers):
        i0 = max(0, int(math.ceil((rect[0] - OX) / RES - 1e-6)))
        i1 = min(self.nx - 1, int(math.floor((rect[2] - OX) / RES + 1e-6)))
        j0 = max(0, int(math.ceil((rect[1] - OY) / RES - 1e-6)))
        j1 = min(self.ny - 1, int(math.floor((rect[3] - OY) / RES + 1e-6)))
        cells = [(li, i, j) for li in layers for i in range(i0, i1 + 1) for j in range(j0, j1 + 1)]
        if not cells:  # tiny pad: nearest grid point
            ci, cj = self.cell((rect[0] + rect[2]) / 2, (rect[1] + rect[3]) / 2)
            cells = [(li, ci, cj) for li in layers]
        return cells

    def search(self, sources, targets, nid, margin=None):
        """A* from any source cell to any target cell, optionally inside a window."""
        nx, ny = self.nx, self.ny
        allc = [(i, j) for _, i, j in sources] + [(i, j) for _, i, j in targets]
        if margin is None:
            wi0, wj0, wi1, wj1 = 0, 0, nx - 1, ny - 1
        else:
            m = int(margin / RES)
            wi0 = max(0, min(c[0] for c in allc) - m)
            wj0 = max(0, min(c[1] for c in allc) - m)
            wi1 = min(nx - 1, max(c[0] for c in allc) + m)
            wj1 = min(ny - 1, max(c[1] for c in allc) + m)
        tgt = np.zeros((2, nx, ny), dtype=bool)
        for li, i, j in targets:
            tgt[li, i, j] = True
        tpts = np.array([(i, j) for _, i, j in targets])
        tmin0, tmin1 = int(tpts[:, 0].min()), int(tpts[:, 1].min())
        tmax0, tmax1 = int(tpts[:, 0].max()), int(tpts[:, 1].max())
        tm = self.tmap
        vm = self.vmap
        okt = (tm == 0) | (tm == nid) | tgt
        okv = ((vm[0] == 0) | (vm[0] == nid)) & ((vm[1] == 0) | (vm[1] == nid))
        okt_l = okt.tolist()
        okv_l = okv.tolist()
        tgt_l = tgt.tolist()
        dist = {}
        prev = {}
        pq = []
        push = heapq.heappush
        pop = heapq.heappop
        for s in sources:
            dist[s] = 0.0
            prev[s] = None
            push(pq, (0.0, 0.0, s))
        steps = ((1, 0, 1.0), (-1, 0, 1.0), (0, 1, 1.0), (0, -1, 1.0),
                 (1, 1, 1.414), (1, -1, 1.414), (-1, 1, 1.414), (-1, -1, 1.414))
        while pq:
            f, g, u = pop(pq)
            if g > dist[u]:
                continue
            li, i, j = u
            if tgt_l[li][i][j]:
                path = []
                while u is not None:
                    path.append(u)
                    u = prev[u]
                return path[::-1]
            row = okt_l[li]
            for di, dj, c in steps:
                a = i + di
                b = j + dj
                if a < wi0 or a > wi1 or b < wj0 or b > wj1:
                    continue
                if not row[a][b]:
                    continue
                if di and dj and not (row[a][j] and row[i][b]):
                    continue
                if di and dj:
                    cost = c * 1.15
                elif (dj == 0) == (li == 0):
                    cost = 1.0
                else:
                    cost = 1.7
                ng = g + cost
                v = (li, a, b)
                if ng < dist.get(v, 1e18):
                    dist[v] = ng
                    prev[v] = u
                    dx = tmin0 - a if a < tmin0 else (a - tmax0 if a > tmax0 else 0)
                    dy = tmin1 - b if b < tmin1 else (b - tmax1 if b > tmax1 else 0)
                    push(pq, (ng + 0.95 * (dx + dy), ng, v))
            if okv_l[i][j]:
                v = (1 - li, i, j)
                ng = g + 8.0
                if okt_l[1 - li][i][j] and ng < dist.get(v, 1e18):
                    dist[v] = ng
                    prev[v] = u
                    dx = tmin0 - i if i < tmin0 else (i - tmax0 if i > tmax0 else 0)
                    dy = tmin1 - j if j < tmin1 else (j - tmax1 if j > tmax1 else 0)
                    push(pq, (ng + 0.95 * (dx + dy), ng, v))
        return None

    def commit(self, path, nid):
        """Turn a cell path into segments/vias; mark them in the maps."""
        segs, vias = [], []
        pts = [(li, OX + i * RES, OY + j * RES) for li, i, j in path]
        start = pts[0]
        for k in range(1, len(pts)):
            p, q = pts[k - 1], pts[k]
            if p[0] != q[0]:
                if (start[1], start[2]) != (p[1], p[2]):
                    segs.append((start[0], (start[1], start[2]), (p[1], p[2])))
                vias.append((p[1], p[2]))
                start = q
                continue
            if k + 1 < len(pts):
                r = pts[k + 1]
                d1 = (round((q[1] - p[1]) / RES), round((q[2] - p[2]) / RES))
                d2 = (round((r[1] - q[1]) / RES), round((r[2] - q[2]) / RES))
                if r[0] == q[0] and d1 == d2:
                    continue
            segs.append((start[0], (start[1], start[2]), (q[1], q[2])))
            start = q
        for li, a, b in segs:
            self._mark_seg(li, a, b, TRACK_W / 2, nid)
        for v in vias:
            for li in (0, 1):
                self._mark_circle(li, v, VIA_D / 2, nid)
        return segs, vias


def route_all(board, fps, nets, order_seed=0, first_nets=()):
    grid = Grid(board, fps, nets)
    # group pads per net
    net_pads = {}
    for layers, rect, nid, pad in grid.pads:
        if nid > 0 and not grid.id_net[nid].startswith("unconnected-"):
            net_pads.setdefault(nid, []).append((layers, rect))
    local = [n for n in net_pads if grid.id_net[n].startswith(("Net-", "/Net-"))]
    named = [n for n in net_pads if n not in local]
    rnd = random.Random(order_seed)
    if order_seed:
        rnd.shuffle(named)
    firsts = [grid.net_id[n] for n in first_nets if n in grid.net_id]
    named = [n for n in firsts if n in named] + [n for n in named if n not in firsts]
    order = sorted(local, key=lambda n: len(net_pads[n])) + named
    result, failed = [], []
    # fine-pitch FPC: reserve a straight fan-out stub per pin before anything else,
    # V-shaped lengths so neighbouring traces can peel off without crossing.
    stubs = {}
    stub_meta = []
    j1 = fps["J1"]
    bb = j1.GetBoundingBox(False)
    body_cy = pcbnew.ToMM(bb.GetCenter().y)
    j1_pads = sorted([p for p in j1.Pads() if p.GetNetname() and p.GetNumber() != "MP"],
                     key=lambda p: p.GetPosition().x)
    n = len(j1_pads)
    for k, pad in enumerate(j1_pads):
        nid = grid.net_id[pad.GetNetname()]
        pb = pad.GetBoundingBox()
        x = round(pcbnew.ToMM(pad.GetPosition().x) / RES) * RES
        y0 = pcbnew.ToMM(pad.GetPosition().y)
        half = pcbnew.ToMM(pb.GetHeight()) / 2
        sgn = 1 if y0 > body_cy else -1
        length = 0.75 + 0.5 * (min(k, n - 1 - k))
        y1 = round((y0 + sgn * (half + length)) / RES) * RES
        seg = (1, (x, y0), (x, y1))
        grid._mark_seg(1, seg[1], seg[2], TRACK_W / 2, nid)
        result.append((nid, [seg], []))
        stub_meta.append((len(result) - 1, nid, len(stubs.get(nid, [])), x, y0, half, sgn))
        cells = set()
        j_lo, j_hi = sorted((grid.cell(x, y0)[1], grid.cell(x, y1)[1]))
        ci = grid.cell(x, y0)[0]
        for j in range(j_lo, j_hi + 1):
            cells.add((1, ci, j))
        stubs.setdefault(nid, []).append((pad, cells))
    def rect_of(pad):
        r = pad.GetBoundingBox()
        return (pcbnew.ToMM(r.GetLeft()), pcbnew.ToMM(r.GetTop()),
                pcbnew.ToMM(r.GetRight()), pcbnew.ToMM(r.GetBottom()))

    def key(rect):
        return tuple(round(v, 3) for v in rect)

    trees, pending = {}, {}
    stub_hits = {}
    for nid in order:
        pads = list(net_pads[nid])
        if nid in stubs:
            tree = set()
            done = set()
            for pad, cells in stubs[nid][:1]:
                tree |= cells
                tree |= set(grid.pad_cells(rect_of(pad), [1]))
                done.add(key(rect_of(pad)))
            trees[nid] = tree
            pending[nid] = [p for p in pads if key(p[1]) not in done]
        else:
            trees[nid] = set(grid.pad_cells(pads[0][1], pads[0][0]))
            pending[nid] = pads[1:]

    def connect(nid, p):
        tree = trees[nid]
        src = grid.pad_cells(p[1], p[0])
        if set(src) & tree:
            tree |= set(src)
            return True
        path = grid.search(src, list(tree), nid, margin=6)
        if path is None:
            path = grid.search(src, list(tree), nid)
        if path is None:
            failed.append((grid.id_net[nid], p[1]))
            return False
        segs, vias = grid.commit(path, nid)
        result.append((nid, segs, vias))
        for end in (path[0], path[-1]):
            for k2, (pad2, cells2) in enumerate(stubs.get(nid, [])):
                if end in cells2:
                    stub_hits.setdefault((nid, k2), []).append(end)
        tree |= set(path)
        tree |= set(src)
        return True

    j1_rects = {key(rect_of(p)) for p in j1.Pads()}
    # phase 1: FPC -> back-side partner pad (XIAO / JP1) bus, shortest first
    j1c = pcbnew.ToMM(j1.GetPosition().x), pcbnew.ToMM(j1.GetPosition().y)
    bus = []
    for nid in stubs:
        # extra FPC pads of the same net: seed from their own stub cells too
        for pad2, cells2 in stubs[nid][1:]:
            pass
        backs = [p for p in pending[nid] if p[0] == [1] and not (
            j1_rects and key(p[1]) in j1_rects)]
        if backs:
            p = min(backs, key=lambda q: math.hypot((q[1][0] + q[1][2]) / 2 - j1c[0], (q[1][1] + q[1][3]) / 2 - j1c[1]))
            bus.append((math.hypot((p[1][0] + p[1][2]) / 2 - j1c[0], (p[1][1] + p[1][3]) / 2 - j1c[1]), nid, p))
    for _, nid, p in sorted(bus, key=lambda t: t[0]):
        pending[nid].remove(p)
        connect(nid, p)

    # phase 2: everything else, net by net
    for nid in order:
        while pending[nid]:
            tree = trees[nid]
            sample = list(tree)[::5] or list(tree)

            def dist_to_tree(p):
                ci, cj = grid.cell((p[1][0] + p[1][2]) / 2, (p[1][1] + p[1][3]) / 2)
                return min(abs(ci - i) + abs(cj - j) for _, i, j in sample)
            pending[nid].sort(key=dist_to_tree)
            connect(nid, pending[nid].pop(0))
    # trim each FPC stub to the farthest point another track actually joins it
    drop = []
    for idx, nid, k, x, y0, half, sgn in stub_meta:
        hits = stub_hits.get((nid, k), [])
        reach = max([abs(OY + j * RES - y0) for _, _, j in hits] or [0.0])
        if reach <= half + 1e-6:
            drop.append(idx)
        else:
            result[idx] = (nid, [(1, (x, y0), (x, y0 + sgn * reach))], [])
    result = [r for i, r in enumerate(result) if i not in set(drop)]
    return grid, result, failed


def add_copper(board, nets, grid, result):
    for nid, segs, vias in result:
        ni = nets[grid.id_net[nid]]
        for li, a, b in segs:
            t = pcbnew.PCB_TRACK(board)
            t.SetStart(V(*a))
            t.SetEnd(V(*b))
            t.SetWidth(MM(TRACK_W))
            t.SetLayer(pcbnew.F_Cu if li == 0 else pcbnew.B_Cu)
            t.SetNet(ni)
            board.Add(t)
        for x, y in vias:
            v = pcbnew.PCB_VIA(board)
            v.SetPosition(V(x, y))
            v.SetViaType(pcbnew.VIATYPE_THROUGH)
            v.SetLayerPair(pcbnew.F_Cu, pcbnew.B_Cu)
            v.SetDrill(MM(VIA_DRILL))
            try:
                v.SetWidth(MM(VIA_D))
            except TypeError:
                v.SetWidth(pcbnew.F_Cu, MM(VIA_D))
            v.SetNet(ni)
            board.Add(v)


def main():
    comps, pin_net = read_netlist(os.path.join(PROJ_DIR, PROJECT + ".net"))
    best = None
    failed = []
    for attempt in range(int(os.environ.get("ATTEMPTS", "12"))):
        board, fps, nets = build_board(comps, pin_net)
        prio = list(dict.fromkeys([f[0] for f in failed])) if attempt else []
        if attempt and best:
            prio = list(dict.fromkeys(prio + [f[0] for f in best[3]]))
        grid, result, failed = route_all(board, fps, nets, order_seed=attempt if attempt > 3 else 0,
                                         first_nets=prio)
        sys.stdout.flush()
        print("attempt %d: %d connections routed, %d failed %s" %
              (attempt, len(result), len(failed), [f[0] for f in failed][:8]))
        if best is None or len(failed) < len(best[3]):
            best = (board, nets, (grid, result), failed)
        if not failed:
            break
    board, nets, (grid, result), failed = best
    add_copper(board, nets, grid, result)
    out = os.path.join(PROJ_DIR, PROJECT + ".kicad_pcb")
    pcbnew.SaveBoard(out, board)
    print("saved", out, "failed:", failed)


if __name__ == "__main__":
    main()
