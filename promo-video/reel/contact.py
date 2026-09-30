#!/usr/bin/env python3
"""Contact sheet of the stills in out/stills: python3 reel/contact.py out/sheet.png f0030 f0048 ..."""
import sys
from pathlib import Path
from PIL import Image, ImageDraw

root = Path(__file__).resolve().parent.parent
out, names = Path(sys.argv[1]), sys.argv[2:]
w, h, cols = 360, 640, 4
rows = (len(names) + cols - 1) // cols
sheet = Image.new("RGB", (cols * w, rows * (h + 26)), (30, 30, 30))
d = ImageDraw.Draw(sheet)
for i, n in enumerate(names):
    im = Image.open(root / "out/stills" / f"{n}.png").convert("RGB").resize((w, h), Image.LANCZOS)
    x, y = (i % cols) * w, (i // cols) * (h + 26)
    sheet.paste(im, (x, y + 26))
    d.text((x + 6, y + 6), n, fill=(255, 255, 255))
sheet.save(out)
