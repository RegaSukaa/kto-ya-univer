# -*- coding: utf-8 -*-
"""Генерирует 12 фото-заглушек для банка персонажей (assets/characters/*.png).

Запуск:  py tools/gen_placeholders.py
Требует Pillow. Ничего не скачивает: шрифты берутся из системы.
"""
import json, os, sys
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parent.parent
ASSETS = ROOT / "android" / "app" / "src" / "main" / "assets"
CHAR_DIR = ASSETS / "characters"
SIZE = 1024
FONT_CANDIDATES = [
    r"C:\Windows\Fonts\arialbd.ttf",
    r"C:\Windows\Fonts\segoeuib.ttf",
    r"C:\Windows\Fonts\DejaVuSans-Bold.ttf",
    "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf",
]

FRIEND = ((37, 99, 235), (30, 64, 175))
TEACHER = ((220, 38, 38), (153, 27, 27))
OTHER = ((51, 65, 85), (30, 41, 59))


def font(size):
    for p in FONT_CANDIDATES:
        if os.path.exists(p):
            return ImageFont.truetype(p, size)
    return ImageFont.load_default()


def gradient(size, top, bottom):
    img = Image.new("RGB", (size, size))
    d = ImageDraw.Draw(img)
    for y in range(size):
        t = y / max(1, size - 1)
        col = tuple(int(top[i] + (bottom[i] - top[i]) * t) for i in range(3))
        d.line([(0, y), (size, y)], fill=col)
    return img


def circle_photo(initials, colors, kind):
    img = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    grad = gradient(SIZE, *colors).convert("RGBA")
    mask = Image.new("L", (SIZE, SIZE), 0)
    ImageDraw.Draw(mask).ellipse([62, 40, 962, 940], fill=255)
    img.paste(grad, (0, 0), mask)

    d = ImageDraw.Draw(img)
    # нейтральная рожица — 25% прозрачности поверх инициалов
    overlay = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    od = ImageDraw.Draw(overlay)
    od.ellipse([372, 300, 412, 340], fill=(255, 255, 255, 64))
    od.ellipse([612, 300, 652, 340], fill=(255, 255, 255, 64))
    od.arc([392, 430, 632, 620], start=20, end=160, fill=(255, 255, 255, 64), width=14)
    img = Image.alpha_composite(img, overlay)

    d = ImageDraw.Draw(img)
    f = font(320)
    box = d.textbbox((0, 0), initials, font=f)
    tw, th = box[2] - box[0], box[3] - box[1]
    # тень под инициалами, чтобы читалось на градиенте
    d.text(((SIZE - tw) / 2 - box[0] + 6, 210 - box[1] + 8), initials, font=f, fill=(0, 0, 0, 90))
    d.text(((SIZE - tw) / 2 - box[0], 210 - box[1]), initials, font=f, fill=(255, 255, 255, 235))

    f2 = font(56)
    label = "PLACEHOLDER"
    b2 = d.textbbox((0, 0), label, font=f2)
    d.text(((SIZE - (b2[2] - b2[0])) / 2, 800), label, font=f2, fill=(255, 255, 255, 115))
    return img


def main():
    CHAR_DIR.mkdir(parents=True, exist_ok=True)
    manifest = json.loads((ASSETS / "characters.json").read_text(encoding="utf-8"))
    count = 0
    for c in manifest["characters"]:
        colors = {"friend": FRIEND, "teacher": TEACHER}.get(c["kind"], OTHER)
        img = circle_photo(c["initials"], colors, c["kind"])
        out = ASSETS / c["photo"]
        out.parent.mkdir(parents=True, exist_ok=True)
        img.save(out, "PNG")
        count += 1
    print(f"generated {count} placeholders in {CHAR_DIR}")


if __name__ == "__main__":
    main()
