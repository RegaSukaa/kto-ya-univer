# -*- coding: utf-8 -*-
"""Собирает иконки приложения из одного логотипа.

Запуск: py tools/make_icons.py [--source design/logo_source.png]
Делает adaptive foreground, legacy/round mipmap'ы всех плотностей и logo.png для UI.
"""
import argparse
from pathlib import Path
from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parent.parent
RES = ROOT / "android/app/src/main/res"
BG = (13, 14, 17)  # совпадает с фоном логотипа и темой приложения
DENSITIES = {"mdpi": 1.0, "hdpi": 1.5, "xhdpi": 2.0, "xxhdpi": 3.0, "xxxhdpi": 4.0}
LEGACY_DP = 48
ADAPTIVE_DP = 108


def art_bounds(img: Image.Image) -> tuple[int, int, int, int]:
    rgb = img.convert("RGB")
    px = rgb.load()
    w, h = rgb.size
    minx, miny, maxx, maxy = w, h, 0, 0
    for y in range(h):
        for x in range(w):
            r, g, b = px[x, y]
            if max(r, g, b) > 40:
                minx = min(minx, x); maxx = max(maxx, x)
                miny = min(miny, y); maxy = max(maxy, y)
    if maxx <= minx or maxy <= miny:
        return (0, 0, w, h)
    return (minx, miny, maxx + 1, maxy + 1)


ART: Image.Image | None = None


def make_tile(size: int, art_frac: float, rounded: float = 0.0) -> Image.Image:
    global ART
    if ART is None:
        img = Image.open(SRC).convert("RGB")
        ART = img.crop(art_bounds(img))
    art = ART
    scale = (size * art_frac) / max(art.width, art.height)
    scaled = art.resize((max(1, int(art.width * scale)), max(1, int(art.height * scale))), Image.LANCZOS)
    tile = Image.new("RGB", (size, size), BG)
    tile.paste(scaled, ((size - scaled.width) // 2, (size - scaled.height) // 2))
    out = tile.convert("RGBA")
    if rounded > 0:
        mask = Image.new("L", (size, size), 0)
        ImageDraw.Draw(mask).rounded_rectangle([0, 0, size - 1, size - 1], radius=int(size * rounded), fill=255)
        out.putalpha(mask)
    return out


def make_art(size: int, art_frac: float) -> Image.Image:
    """Логотип без фона: тёмная подложка вырезается, остаётся оранжевый контур и «?»."""
    global ART
    if ART is None:
        img0 = Image.open(SRC).convert("RGB")
        ART = img0.crop(art_bounds(img0))
    src = ART
    scale = (size * art_frac) / max(src.width, src.height)
    w, h = max(1, int(src.width * scale)), max(1, int(src.height * scale))
    small = src.resize((w, h), Image.LANCZOS)
    out = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    src_px = small.load()
    dst_px = out.load()
    for y in range(h):
        for x in range(w):
            r, g, b = src_px[x, y]
            a = max(r, g, b) / 255.0
            if a < 0.10:
                continue
            # un-premultiply over тёмный фон: возвращаем исходный цвет линии
            dst_px[x, y] = (
                min(255, int(r / a)),
                min(255, int(g / a)),
                min(255, int(b / a)),
                min(255, int(a * 255 * 1.15)),
            )
    canvas = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    canvas.alpha_composite(out, ((size - w) // 2, (size - h) // 2))
    return canvas


def save(img: Image.Image, path: Path) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    img.save(path, "PNG")


def main() -> None:
    global SRC
    ap = argparse.ArgumentParser()
    ap.add_argument("--source", default=str(ROOT / "design/logo_source.png"))
    SRC = Path(ap.parse_args().source)
    if not SRC.exists():
        raise SystemExit(f"нет файла логотипа: {SRC}")

    for name, factor in DENSITIES.items():
        legacy_px = int(LEGACY_DP * factor)
        adaptive_px = int(ADAPTIVE_DP * factor)
        save(make_tile(legacy_px, 0.66), RES / f"mipmap-{name}/ic_launcher.png")
        round_img = make_tile(legacy_px, 0.60).convert("RGBA")
        mask = Image.new("L", (legacy_px, legacy_px), 0)
        ImageDraw.Draw(mask).ellipse([0, 0, legacy_px - 1, legacy_px - 1], fill=255)
        round_img.putalpha(mask)
        save(round_img, RES / f"mipmap-{name}/ic_launcher_round.png")
        save(make_tile(adaptive_px, 0.60), RES / f"mipmap-{name}/ic_launcher_foreground.png")
        print(f"{name}: legacy {legacy_px}px, adaptive {adaptive_px}px")

    save(make_tile(512, 0.62, rounded=0.22), RES / "drawable-nodpi/logo.png")
    save(make_art(512, 0.78), RES / "drawable-nodpi/logo_art.png")
    print("drawable-nodpi/logo.png 512px + logo_art.png 512px")


if __name__ == "__main__":
    main()
