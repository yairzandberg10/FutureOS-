"""JLCPCB assembly files: BOM (Comment, Designator, Footprint, LCSC) and CPL.

Only FRONT-side SMD parts are assembled by the factory. U1 (XIAO) and J1 (FPC,
back side, only needed for the future mainboard) are hand-soldered; JP1 is a
solder jumper (copper only); H1-H4 are holes.
"""
import csv
import os

from design import PROJ_DIR, PROJECT
from gen_pcb import read_netlist

# verified on lcsc.com product pages, 2026-09-27
LCSC = {
    ("TS-1187A", "SW_Push_1P1T_XKB_TS-1187A"): ("C318884", "XKB TS-1187A-B-A-B"),
    ("1N4148W", "D_SOD-123"): ("C81598", "1N4148W"),
    ("White", "LED_0603_1608Metric"): ("C2290", "KENTO KT-0603W"),
    ("1k", "R_0603_1608Metric"): ("C21190", "UNI-ROYAL 0603WAF1001T5E"),
    ("100k", "R_0603_1608Metric"): ("C25803", "UNI-ROYAL 0603WAF1003T5E"),
    ("AO3400A", "SOT-23"): ("C20917", "AOS AO3400A"),
}
NOT_ASSEMBLED = ("U", "J", "JP", "H")

OUT = os.path.join(PROJ_DIR, "fab")


def main():
    comps, _ = read_netlist(os.path.join(PROJ_DIR, PROJECT + ".net"))
    groups = {}
    for ref, c in comps.items():
        if ref.rstrip("0123456789") in NOT_ASSEMBLED:
            continue
        fp = c["footprint"].split(":")[1]
        key = (c["value"], fp)
        if key not in LCSC:
            raise SystemExit("no LCSC part for %s %s" % (ref, key))
        groups.setdefault(key, []).append(ref)

    def natural(r):
        p = r.rstrip("0123456789")
        return (p, int(r[len(p):]))

    with open(os.path.join(OUT, "BOM_JLCPCB.csv"), "w", newline="") as f:
        w = csv.writer(f)
        w.writerow(["Comment", "Designator", "Footprint", "LCSC", "MPN"])
        for (val, fp), refs in sorted(groups.items()):
            lcsc, mpn = LCSC[(val, fp)]
            w.writerow([val, ",".join(sorted(refs, key=natural)), fp, lcsc, mpn])

    # CPL from kicad-cli pos export
    assembled = {r for refs in groups.values() for r in refs}
    rows = list(csv.DictReader(open(os.path.join(OUT, "pos_raw.csv"), encoding="utf-8")))
    with open(os.path.join(OUT, "CPL_JLCPCB.csv"), "w", newline="") as f:
        w = csv.writer(f)
        w.writerow(["Designator", "Mid X", "Mid Y", "Layer", "Rotation"])
        for r in rows:
            if r["Ref"] in assembled:
                w.writerow([r["Ref"], r["PosX"] + "mm", r["PosY"] + "mm",
                            "Top" if r["Side"] == "top" else "Bottom", r["Rot"]])
    os.remove(os.path.join(OUT, "pos_raw.csv"))
    print("BOM groups:", {k[0]: len(v) for k, v in groups.items()})


if __name__ == "__main__":
    main()
