"""python3 sheet2.py out.png cols tile_w a.png b.png ...   a grid of small tiles (9:16) for reviewing a whole timeline at a glance"""
import sys
from PIL import Image, ImageDraw
out, cols, tw, files = sys.argv[1], int(sys.argv[2]), int(sys.argv[3]), sys.argv[4:]
th = tw * 16 // 9
rows = (len(files) + cols - 1) // cols
sheet = Image.new("RGB", (cols * (tw + 4), rows * (th + 4)), (70, 70, 70))
d = ImageDraw.Draw(sheet)
for i, f in enumerate(files):
    im = Image.open(f).convert("RGB").resize((tw, th), Image.LANCZOS)
    x, y = (i % cols) * (tw + 4), (i // cols) * (th + 4)
    sheet.paste(im, (x, y))
    d.text((x + 6, y + 4), f.split("t-")[-1].replace(".png", ""), fill=(255, 220, 0))
sheet.save(out)
