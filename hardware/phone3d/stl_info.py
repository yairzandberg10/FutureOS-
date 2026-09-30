"""python3 stl_info.py a.stl b.stl ...   triangle count, volume and bounding box of STL files (binary or ASCII)"""
import re
import struct
import sys

import numpy as np


def load(p):
    b = open(p, 'rb').read()
    if b[:5] == b'solid' and b'facet' in b[:400]:
        return np.array(re.findall(rb"vertex\s+(\S+)\s+(\S+)\s+(\S+)", b), dtype=float).reshape(-1, 3, 3)
    n = struct.unpack('<I', b[80:84])[0]
    a = np.frombuffer(b[84:84 + n * 50], dtype=np.dtype([('n', '<f4', 3), ('v', '<f4', (3, 3)), ('a', '<u2')]))
    return a['v'].astype(float)


def vol(t):
    return np.einsum('ij,ij->i', t[:, 0], np.cross(t[:, 1], t[:, 2])).sum() / 6


if __name__ == '__main__':
    for p in sys.argv[1:]:
        t = load(p); v = t.reshape(-1, 3)
        print(f"{p:42s} tris {len(t):6d} vol {vol(t):9.1f} min {v.min(0).round(2)} max {v.max(0).round(2)}")
