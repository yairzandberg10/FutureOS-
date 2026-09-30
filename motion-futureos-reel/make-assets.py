"""make-assets.py: the images the colour film loads, rebuilt from the repo.

  python make-assets.py

assets/icons/<app>.png  each app's own launcher icon, exactly as the launcher draws it: the adaptive icon's
                        foreground over its background, cut to the 72 dp viewport (the inner two thirds of the
                        108 dp layers, like AdaptiveIconDrawable). The launcher rounds the corners (28%); the
                        page does that in CSS. The artwork itself is not touched.
assets/wallpaper.png    640 x 960, the home and lock screen wallpaper: soft fields of the design system's
                        accents over a dark base, with a little grain so the video encoder does not band it.
"""
from pathlib import Path
import re

import numpy as np
from PIL import Image

HERE = Path(__file__).resolve().parent
REPO = HERE.parent
OUT = HERE / "assets"

# folder in the repo -> file name the page asks for
APPS = {
    "dialer": "dialer", "Messages": "messages", "Contact": "contact", "Camera": "camera",
    "Navigation": "navigation", "Sfarim": "sfarim", "Assistant": "assistant", "Settings": "settings",
    "Clock": "clock", "Calendar": "calendar", "Music": "music", "Gallery": "gallery",
    "Calculator": "calculator", "notes": "notes", "Fitness": "fitness", "Files": "files",
}
SIZE = 192   # 2x the 96 px (48 dp) the launcher shows


def background_color(app_dir):
    """The adaptive icon background's fill (the first path of drawable/ic_launcher_background.xml)."""
    f = app_dir / "app/src/main/res/drawable/ic_launcher_background.xml"
    m = re.search(r'fillColor="#([0-9A-Fa-f]{6,8})"', f.read_text(encoding="utf-8"))
    h = m.group(1)[-6:]
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4)) + (255,)


def icon(folder):
    app_dir = REPO / folder
    fg = Image.open(app_dir / "app/src/main/res/mipmap-xxxhdpi/ic_launcher_foreground.webp").convert("RGBA")
    bg = Image.new("RGBA", fg.size, background_color(app_dir))
    comp = Image.alpha_composite(bg, fg)
    w = fg.size[0]
    c = w / 6   # 18 of 108 dp on each side
    return comp.crop((round(c), round(c), round(w - c), round(w - c))).resize((SIZE, SIZE), Image.LANCZOS)


def wallpaper(w=640, h=960, seed=7):
    """Soft colour fields, mixed in linear light, then a little grain."""
    def lin(hexs):
        v = np.array([int(hexs[i:i + 2], 16) for i in (1, 3, 5)], dtype=np.float64) / 255
        return np.where(v <= 0.04045, v / 12.92, ((v + 0.055) / 1.055) ** 2.4)
    y, x = np.mgrid[0:h, 0:w].astype(np.float64)
    img = np.zeros((h, w, 3)) + lin("#05060B")
    # (colour, centre x, centre y, radius x, radius y, strength)
    fields = [
        ("#64D2FF", 320, -60, 520, 360, 0.9),    # cyan, across the top
        ("#BF5AF2", 0, 560, 360, 420, 0.85),     # purple, the middle on the left
        ("#FF9F0A", 640, 980, 420, 360, 1.0),    # orange, bottom right
    ]
    for col, cx, cy, rx, ry, k in fields:
        d2 = ((x - cx) / rx) ** 2 + ((y - cy) / ry) ** 2
        img += lin(col)[None, None, :] * (k * np.exp(-1.6 * d2))[..., None]
    img = img / (1 + img * 0.35)   # soft shoulder: bright overlaps stay colour, not white
    srgb = np.where(img <= 0.0031308, img * 12.92, 1.055 * np.power(np.clip(img, 0, None), 1 / 2.4) - 0.055)
    rng = np.random.default_rng(seed)
    srgb = srgb * 255 + rng.uniform(-2.2, 2.2, srgb.shape)
    return Image.fromarray(np.clip(np.round(srgb), 0, 255).astype(np.uint8), "RGB")


def main():
    (OUT / "icons").mkdir(parents=True, exist_ok=True)
    for folder, name in APPS.items():
        icon(folder).save(OUT / "icons" / f"{name}.png", optimize=True)
        print("icon", name)
    wallpaper().save(OUT / "wallpaper.png", optimize=True)
    print("wallpaper")


if __name__ == "__main__":
    main()
