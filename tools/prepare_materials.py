# -*- coding: utf-8 -*-
"""Импорт материалов (фото + голоса + люди.csv) в assets игры.

Запуск:
    py tools/prepare_materials.py
    py tools/prepare_materials.py --materials materials --assets android/app/src/main/assets

Что делает:
  фото/<папка>/*.jpg|png|webp → assets/characters/<id>/photo_N.png (≤1024 px, PNG)
  голоса/<slot>/*.m4a|wav      → assets/voices/<slot>/<имя файла>
  голоса/личные/<папка>_win.m4a → assets/sounds/<id>_win.m4a (и _skip)
  люди.csv                     → assets/characters.json (id, name, kind, tags, photos)
"""
import argparse
import csv
import json
import re
import shutil
import sys
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
TRANSLIT = {
    "а": "a", "б": "b", "в": "v", "г": "g", "д": "d", "е": "e", "ё": "e", "ж": "zh",
    "з": "z", "и": "i", "й": "y", "к": "k", "л": "l", "м": "m", "н": "n", "о": "o",
    "п": "p", "р": "r", "с": "s", "т": "t", "у": "u", "ф": "f", "х": "h", "ц": "ts",
    "ч": "ch", "ш": "sh", "щ": "sch", "ъ": "", "ы": "y", "ь": "", "э": "e", "ю": "yu",
    "я": "ya",
}
KIND_MAP = {
    "студент": "friend", "друг": "friend", "friend": "friend", "ученик": "friend",
    "препод": "teacher", "преподаватель": "teacher", "teacher": "teacher", "доцент": "teacher",
    "профессор": "teacher",
}
PHOTO_EXT = {".jpg", ".jpeg", ".png", ".webp"}
VOICE_EXT = {".m4a", ".wav", ".mp3"}


def slug(text: str) -> str:
    text = text.strip().lower()
    out = "".join(TRANSLIT.get(ch, ch) for ch in text)
    out = re.sub(r"[^a-z0-9]+", "_", out).strip("_")
    return out or "person"


def read_csv(path: Path):
    text = path.read_text(encoding="utf-8-sig")
    delimiter = ";" if text.count(";") >= text.count(",") else ","
    rows = []
    for row in csv.DictReader(text.splitlines(), delimiter=delimiter):
        clean = {(k or "").strip().lower(): (v or "").strip() for k, v in row.items()}
        if clean.get("папка") or clean.get("folder"):
            rows.append(clean)
    return rows


def copy_photos(src_dir: Path, dst_dir: Path) -> list[str]:
    dst_dir.mkdir(parents=True, exist_ok=True)
    photos = []
    files = sorted(p for p in src_dir.iterdir() if p.suffix.lower() in PHOTO_EXT)
    for i, src in enumerate(files, start=1):
        img = Image.open(src)
        img = img.convert("RGBA")
        long_side = max(img.size)
        if long_side > 1024:
            ratio = 1024 / long_side
            img = img.resize((max(1, int(img.width * ratio)), max(1, int(img.height * ratio))), Image.LANCZOS)
        out = dst_dir / f"photo_{i}.png"
        img.save(out, "PNG")
        photos.append(out)
    return photos


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--materials", default=str(ROOT / "materials"))
    ap.add_argument("--assets", default=str(ROOT / "android/app/src/main/assets"))
    args = ap.parse_args()

    mat = Path(args.materials)
    assets = Path(args.assets)
    csv_path = mat / "люди.csv"
    if not csv_path.exists():
        csv_path = mat / "people.csv"
    if not csv_path.exists():
        print(f"нет таблицы: {mat / 'люди.csv'}")
        return 1

    manifest_path = assets / "characters.json"
    manifest = json.loads(manifest_path.read_text(encoding="utf-8")) if manifest_path.exists() else {
        "format": "ktya-assets/1", "version": 1, "characters": []
    }
    by_id = {c["id"]: c for c in manifest["characters"]}
    imported_photos = 0
    imported_people = 0

    for row in read_csv(csv_path):
        folder = row.get("папка") or row.get("folder")
        name = row.get("имя") or row.get("name")
        kind_raw = (row.get("тип") or row.get("kind") or "студент").lower()
        kind = KIND_MAP.get(kind_raw)
        if not folder or not name:
            print(f"пропуск строки (нет папки/имени): {row}")
            continue
        if kind is None:
            print(f"неизвестный тип '{kind_raw}' у {name}, считаю студентом")
            kind = "friend"
        person_id = f"{kind}_{slug(folder)}"
        src_dir = mat / "фото" / folder
        if not src_dir.exists():
            src_dir = mat / "photos" / folder
        photos = copy_photos(src_dir, assets / "characters" / person_id) if src_dir.exists() else []
        if not photos:
            print(f"у {name} нет фото в {src_dir}")
            continue
        rel = [f"characters/{person_id}/{p.name}" for p in photos]
        tags = [row.get("тег1"), row.get("тег2"), row.get("тег3")]
        tags = [t for t in tags if t]
        initials = (row.get("инициалы") or "").strip().upper() or "".join(
            part[0] for part in name.split()[:2]
        ).upper()
        by_id[person_id] = {
            "id": person_id,
            "name": name,
            "kind": kind,
            "bgPreset": by_id.get(person_id, {}).get("bgPreset", 0 if kind == "friend" else 1),
            "initials": initials,
            "tags": tags,
            "photo": rel[0],
            "photos": rel,
            "sounds": {"win": None, "skip": None},
            "placeholder": False,
        }
        imported_people += 1
        imported_photos += len(photos)
        print(f"{person_id}: {name}, {len(photos)} фото")

    manifest["characters"] = sorted(by_id.values(), key=lambda c: c["id"])
    manifest_path.parent.mkdir(parents=True, exist_ok=True)
    manifest_path.write_text(json.dumps(manifest, ensure_ascii=False, indent=1), encoding="utf-8")

    voices = 0
    for slot in ["go", "guessed", "missed", "final"]:
        src = mat / "голоса" / slot
        if not src.exists():
            src = mat / "voices" / slot
        if not src.exists():
            continue
        dst = assets / "voices" / slot
        dst.mkdir(parents=True, exist_ok=True)
        for f in sorted(src.iterdir()):
            if f.suffix.lower() in VOICE_EXT:
                shutil.copy2(f, dst / f.name)
                voices += 1

    personal = mat / "голоса" / "личные"
    if not personal.exists():
        personal = mat / "voices" / "personal"
    if personal.exists():
        sounds = assets / "sounds"
        sounds.mkdir(parents=True, exist_ok=True)
        for f in sorted(personal.iterdir()):
            if f.suffix.lower() in VOICE_EXT:
                shutil.copy2(f, sounds / f.name)
                voices += 1

    print(f"\nИтого: людей {imported_people}, фото {imported_photos}, голосов {voices}")
    print(f"manifest: {manifest_path}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
