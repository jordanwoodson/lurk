"""Launcher icon resources from two-color logo artwork. Run through bin/app-icon.

The artwork is a square image: a logo in one flat color, centred on a flat background color.
Outputs (resource names match what Android Studio generates, which is what the apps ship):

  <flat_png>                                    the artwork flattened to its two colors
  <out>/drawable/ic_launcher_background.xml     the background color
  <out>/drawable/ic_launcher_foreground.xml     the logo traced to a vector, inside the 66dp safe zone
  <out>/mipmap-anydpi/ic_launcher[_round].xml   adaptive icons; the logo doubles as the themed layer
  <out>/mipmap-<dpi>/ic_launcher[_round].webp   legacy icons (only used below Android 8)
"""

import sys
from pathlib import Path

import numpy as np
import potrace
from PIL import Image, ImageDraw

# Android keeps everything within a 66dp circle of the 108dp adaptive canvas visible under any mask.
CANVAS_DP = 108
SAFE_RADIUS_DP = 32
VISIBLE_DP = 72
LEGACY_DP = 48
DENSITIES = {"mdpi": 1, "hdpi": 1.5, "xhdpi": 2, "xxhdpi": 3, "xxxhdpi": 4}

ADAPTIVE_ICON = """<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@drawable/ic_launcher_background" />
    <foreground android:drawable="@drawable/ic_launcher_foreground" />
    <monochrome android:drawable="@drawable/ic_launcher_foreground" />
</adaptive-icon>
"""


def hex_color(rgb):
    return "#%02X%02X%02X" % tuple(int(round(c)) for c in rgb)


def colors(px):
    """Background = median of the border; logo = median of the pixels furthest from it."""
    border = np.concatenate([px[:8].reshape(-1, 3), px[-8:].reshape(-1, 3),
                             px[:, :8].reshape(-1, 3), px[:, -8:].reshape(-1, 3)])
    bg = np.median(border, axis=0)
    dist = np.linalg.norm(px - bg, axis=2)
    fg = np.median(px[dist > dist.max() * 0.75], axis=0)
    return bg, fg


def flatten(px, bg, fg):
    """Each pixel's position on the background→logo ramp, with the grain at both ends removed."""
    d = fg - bg
    t = np.clip(((px - bg) @ d) / (d @ d), 0.0, 1.0)
    t[t < 0.06] = 0.0
    t[t > 0.94] = 1.0
    return t


def path_data(mask):
    # potracer treats False as ink, so trace the inverse of the logo mask.
    curves = potrace.Bitmap(~mask).trace(turdsize=10, alphamax=1.0, opticurve=True, opttolerance=0.2)
    h, w = mask.shape

    def num(v):
        return f"{v:.1f}".rstrip("0").rstrip(".")

    parts = []
    for curve in curves:
        p = curve.start_point
        if min(p.x, p.y) < 1 or p.x > w - 1 or p.y > h - 1:
            sys.exit("app_icon: the logo touches the image edge (or was traced inverted)")
        parts.append(f"M{num(p.x)},{num(p.y)}")
        for seg in curve.segments:
            e = seg.end_point
            if seg.is_corner:
                parts.append(f"L{num(seg.c.x)},{num(seg.c.y)}L{num(e.x)},{num(e.y)}")
            else:
                parts.append(f"C{num(seg.c1.x)},{num(seg.c1.y)} {num(seg.c2.x)},{num(seg.c2.y)} "
                             f"{num(e.x)},{num(e.y)}")
        parts.append("Z")
    return "".join(parts), len(curves)


def legacy_icon(adaptive, size, round_):
    """Crop the visible part of the adaptive canvas and mask it like a pre-Android 8 launcher icon."""
    v = adaptive.width
    crop = v * (1 - VISIBLE_DP / CANVAS_DP) / 2
    icon = adaptive.crop((round(crop), round(crop), round(v - crop), round(v - crop)))
    icon = icon.resize((size, size), Image.Resampling.LANCZOS).convert("RGBA")
    big = Image.new("L", (size * 4, size * 4), 0)
    draw = ImageDraw.Draw(big)
    if round_:
        draw.ellipse((0, 0, size * 4 - 1, size * 4 - 1), fill=255)
    else:
        draw.rounded_rectangle((0, 0, size * 4 - 1, size * 4 - 1), radius=size * 4 * 0.18, fill=255)
    icon.putalpha(big.resize((size, size), Image.Resampling.LANCZOS))
    return icon


def main():
    art, flat_png, out = sys.argv[1], Path(sys.argv[2]), Path(sys.argv[3])
    px = np.asarray(Image.open(art).convert("RGB"), dtype=np.float64)
    h, w, _ = px.shape
    if h != w:
        sys.exit(f"app_icon: artwork must be square, got {w}x{h}")

    bg, fg = colors(px)
    t = flatten(px, bg, fg)
    flat = Image.fromarray((bg + t[..., None] * (fg - bg)).round().astype(np.uint8), "RGB")
    flat_png.parent.mkdir(parents=True, exist_ok=True)
    flat.quantize(colors=64, dither=Image.Dither.NONE).save(flat_png, optimize=True)

    mask = t > 0.5
    d, curves = path_data(mask)

    # Scale so the logo pixel furthest from the image centre lands on the safe-zone radius.
    ys, xs = np.nonzero(mask)
    centre = (w - 1) / 2
    radius = np.sqrt((xs - centre) ** 2 + (ys - centre) ** 2).max()
    viewport = CANVAS_DP * radius / SAFE_RADIUS_DP
    shift = (viewport - w) / 2

    (out / "drawable").mkdir(parents=True, exist_ok=True)
    (out / "drawable/ic_launcher_background.xml").write_text(
        '<?xml version="1.0" encoding="utf-8"?>\n'
        '<shape xmlns:android="http://schemas.android.com/apk/res/android" android:shape="rectangle">\n'
        f'    <solid android:color="{hex_color(bg)}" />\n'
        "</shape>\n"
    )
    (out / "drawable/ic_launcher_foreground.xml").write_text(
        '<?xml version="1.0" encoding="utf-8"?>\n'
        "<!-- Generated by bin/app-icon. -->\n"
        '<vector xmlns:android="http://schemas.android.com/apk/res/android"\n'
        f'    android:width="{CANVAS_DP}dp"\n'
        f'    android:height="{CANVAS_DP}dp"\n'
        f'    android:viewportWidth="{viewport:.1f}"\n'
        f'    android:viewportHeight="{viewport:.1f}">\n'
        f'    <group android:translateX="{shift:.1f}" android:translateY="{shift:.1f}">\n'
        "        <path\n"
        f'            android:fillColor="{hex_color(fg)}"\n'
        '            android:fillType="evenOdd"\n'
        f'            android:pathData="{d}" />\n'
        "    </group>\n"
        "</vector>\n"
    )
    (out / "mipmap-anydpi").mkdir(parents=True, exist_ok=True)
    for name in ("ic_launcher.xml", "ic_launcher_round.xml"):
        (out / "mipmap-anydpi" / name).write_text(ADAPTIVE_ICON)

    side = round(viewport)
    adaptive = Image.new("RGB", (side, side), tuple(int(round(c)) for c in bg))
    adaptive.paste(flat, (round(shift), round(shift)))
    for dpi, scale in DENSITIES.items():
        size = round(LEGACY_DP * scale)
        (out / f"mipmap-{dpi}").mkdir(parents=True, exist_ok=True)
        for name, round_ in (("ic_launcher.webp", False), ("ic_launcher_round.webp", True)):
            legacy_icon(adaptive, size, round_).save(out / f"mipmap-{dpi}" / name, lossless=True, quality=100)

    print(f"background {hex_color(bg)}, logo {hex_color(fg)}, {curves} curves, {len(d)} chars of path data")


if __name__ == "__main__":
    main()
