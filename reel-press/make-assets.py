"""Prepares assets/ and fonts/ for the reel. Run once: python3 make-assets.py

  * fonts/      Heebo (text), Rubik (headings) and Roboto Mono (numbers): exactly the three faces the design system
                names in tokens/typography.css, fetched from Google Fonts as woff2 (Hebrew + Latin subsets) so the
                page renders with no network.
  * assets/icons/   the real launcher icons of the apps, copied from each app's own res/ folder and only scaled.
                    Nothing is redrawn, recoloured or cropped: the icons stay as they are in the repo.
  * assets/fos-icons.json   the "Future Glyphs" path table lifted from components/core/FosIcon.jsx
  * assets/ds-tokens.css    the design system's tokens (colors, spacing, shape, focus, type, motion, elevation), concatenated
"""
import json, re, subprocess, sys
from pathlib import Path

import requests
from PIL import Image

ROOT = Path(__file__).resolve().parent
REPO = ROOT.parent
DS = REPO / "design" / "FutureOS Design System"

# ------------------------------------------------------------------ fonts
UA = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Safari/537.36"
FAMILIES = [
    ("Heebo", "wght@300;400;500;600;700;800;900"),
    ("Rubik", "wght@400;500;600;700;800"),
    ("Roboto+Mono", "wght@300;400;500"),
]


def fetch_fonts():
    out = ROOT / "fonts"
    out.mkdir(exist_ok=True)
    css_all = []
    for fam, axis in FAMILIES:
        css = requests.get(f"https://fonts.googleapis.com/css2?family={fam}:{axis}&display=block",
                           headers={"User-Agent": UA}, timeout=60).text
        # blocks look like:  /* hebrew */ @font-face { ... src: url(...) ...; unicode-range: ...; }
        for m in re.finditer(r"/\*\s*([\w-]+)\s*\*/\s*(@font-face\s*\{.*?\})", css, re.S):
            subset, block = m.group(1), m.group(2)
            if subset not in ("hebrew", "latin"):
                continue
            url = re.search(r"url\((https://[^)]+)\)", block).group(1)
            weight = re.search(r"font-weight:\s*(\d+)", block).group(1)
            name = f"{fam.lower().replace('+', '-')}-{subset}-{weight}.woff2"
            path = out / name
            if not path.exists():
                path.write_bytes(requests.get(url, timeout=60).content)
            block = re.sub(r"url\(https://[^)]+\)", f"url({name})", block)
            block = re.sub(r"font-family:\s*'[^']+'", f"font-family: '{fam.replace('+', ' ')}'", block)
            css_all.append(f"/* {fam} {subset} {weight} */\n{block}")
    (out / "fonts.css").write_text("\n".join(css_all) + "\n", encoding="utf-8")
    print("fonts:", len(css_all), "faces")


# ------------------------------------------------------------------ app icons
# (file name in assets/icons, app folder, source)
ICONS = [
    ("dialer", "dialer"), ("messages", "Messages"), ("contact", "Contact"), ("calendar", "Calendar"),
    ("clock", "Clock"), ("camera", "Camera"), ("gallery", "Gallery"), ("music", "Music"),
    ("navigation", "Navigation"), ("settings", "Settings"), ("notes", "notes"), ("tasks", "Tasks"),
    ("files", "Files"), ("calculator", "Calculator"), ("fitness", "Fitness"), ("sfarim", "Sfarim"),
    ("bluetooth", "Bluetooth"), ("terminal", "Terminal"), ("remote", "Remote"), ("tools", "Tools"),
    ("flashlight", "Flashlight"), ("guide", "Guide"), ("frixa", "Frixa"), ("assistant", "Assistant"),
    ("keyboard", "Keyboard"),
]


def adaptive(main):
    """Foreground over the background's fill, cropped to the inner 72 of the 108 dp (like AdaptiveIconDrawable)."""
    fg = Image.open(main / "res" / "mipmap-xxxhdpi" / "ic_launcher_foreground.webp").convert("RGBA")
    xml = (main / "res" / "drawable" / "ic_launcher_background.xml").read_text(encoding="utf-8")
    h = re.search(r'fillColor="#([0-9A-Fa-f]{6,8})"', xml).group(1)[-6:]
    bg = Image.new("RGBA", fg.size, tuple(int(h[i:i + 2], 16) for i in (0, 2, 4)) + (255,))
    w = fg.size[0]
    c = w / 6
    return Image.alpha_composite(bg, fg).crop((round(c), round(c), round(w - c), round(w - c)))


def copy_icons():
    out = ROOT / "assets" / "icons"
    out.mkdir(parents=True, exist_ok=True)
    n = 0
    for name, folder in ICONS:
        main = REPO / folder / "app" / "src" / "main"
        play = main / "ic_launcher-playstore.png"
        fg = main / "res" / "mipmap-xxxhdpi" / "ic_launcher_foreground.webp"
        if play.exists():
            im = Image.open(play).convert("RGBA")
        elif fg.exists():
            # No play-store image (Camera, Calculator): build the icon the way the launcher does, the adaptive
            # foreground over its background, cut to the 72 dp viewport. The legacy ic_launcher.webp has its own
            # padding and would show up smaller than the rest of the grid.
            im = adaptive(main)
        else:
            print("no icon for", folder)
            continue
        if im.width > 256:
            im = im.resize((256, 256), Image.LANCZOS)   # scaling only
        im.save(out / f"{name}.png", optimize=True)
        n += 1
    print("icons:", n)


# ------------------------------------------------------------------ FosIcon paths
def fos_icons():
    src = (DS / "components" / "core" / "FosIcon.jsx").read_text(encoding="utf-8")
    head = src.split("export function FosIcon")[0]
    head = re.sub(r"^import .*$", "", head, flags=re.M)
    head = head.replace("const P = {", "const P = {", 1)
    js = head + "\nprocess.stdout.write(JSON.stringify(P));"
    res = subprocess.run(["node", "-e", js], capture_output=True, text=True)
    if res.returncode:
        sys.exit(res.stderr)
    data = json.loads(res.stdout)
    (ROOT / "assets" / "fos-icons.json").write_text(json.dumps(data, ensure_ascii=False), encoding="utf-8")
    (ROOT / "assets" / "fos-icons.js").write_text("window.FOS_ICONS = " + json.dumps(data, ensure_ascii=False) + ";\n", encoding="utf-8")
    print("glyphs:", len(data))


# ------------------------------------------------------------------ tokens
def tokens():
    parts = []
    for f in ("colors", "spacing", "shape", "focus", "typography", "motion", "elevation"):
        parts.append((DS / "tokens" / f"{f}.css").read_text(encoding="utf-8"))
    (ROOT / "assets" / "ds-tokens.css").write_text("\n".join(parts), encoding="utf-8")
    print("tokens ok")


if __name__ == "__main__":
    (ROOT / "assets").mkdir(exist_ok=True)
    fetch_fonts()
    copy_icons()
    fos_icons()
    tokens()
