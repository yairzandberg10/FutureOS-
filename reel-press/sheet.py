"""python3 sheet.py out.png a.png b.png c.png d.png  -> 2x2 contact sheet (each tile 540x960) for quick review"""
import sys
from PIL import Image
out, files = sys.argv[1], sys.argv[2:]
cols = 2 if len(files) > 2 else len(files)
rows = (len(files) + cols - 1) // cols
sheet = Image.new("RGB", (cols * 540 + (cols - 1) * 6, rows * 960 + (rows - 1) * 6), (60, 60, 60))
for i, f in enumerate(files):
    im = Image.open(f).convert("RGB").resize((540, 960), Image.LANCZOS)
    sheet.paste(im, ((i % cols) * 546, (i // cols) * 966))
sheet.save(out)
