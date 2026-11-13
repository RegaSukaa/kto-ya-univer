# -*- coding: utf-8 -*-
"""Проверяет, что наборы строк ru и en совпадают один-в-один."""
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
RU = ROOT / "android/app/src/main/res/values/strings.xml"
EN = ROOT / "android/app/src/main/res/values-en/strings.xml"
RE = re.compile(r'<string name="([^"]+)"')


def keys(path: Path) -> set[str]:
    return set(RE.findall(path.read_text(encoding="utf-8")))


def main() -> int:
    ru, en = keys(RU), keys(EN)
    ok = True
    missing_en = sorted(ru - en)
    missing_ru = sorted(en - ru)
    if missing_en:
        ok = False
        print("Нет в values-en:", ", ".join(missing_en))
    if missing_ru:
        ok = False
        print("Нет в values:", ", ".join(missing_ru))
    print(f"ru: {len(ru)} строк, en: {len(en)} строк")
    print("OK" if ok else "FAIL")
    return 0 if ok else 1


if __name__ == "__main__":
    sys.exit(main())
