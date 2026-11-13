# -*- coding: utf-8 -*-
"""Генерирует WAV-звуки игры (assets/sounds/*.wav). Offline, только stdlib.

Запуск:  py tools/gen_tones.py
44.1 кГц, 16 бит, mono, attack/release 10 мс (без щелчков).
"""
import math, struct, wave
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
OUT = ROOT / "android" / "app" / "src" / "main" / "assets" / "sounds"
SR = 44100


def envelope(i, n, attack=0.01, release=0.01):
    a = int(SR * attack)
    r = int(SR * release)
    if i < a:
        return i / max(1, a)
    if i > n - r:
        return max(0.0, (n - i) / max(1, r))
    return 1.0


def tone(freq, ms, vol=0.8, sweep_to=None, harmonics=()):
    n = int(SR * ms / 1000)
    out = []
    phase = 0.0
    for i in range(n):
        f = freq if sweep_to is None else freq + (sweep_to - freq) * (i / max(1, n - 1))
        phase += 2 * math.pi * f / SR
        s = math.sin(phase)
        for h, amp in harmonics:
            s += amp * math.sin(phase * h)
        s *= envelope(i, n) * vol
        out.append(s)
    return out


def silence(ms):
    return [0.0] * int(SR * ms / 1000)


def write(name, samples):
    OUT.mkdir(parents=True, exist_ok=True)
    path = OUT / name
    peak = max(1e-6, max(abs(s) for s in samples))
    if peak > 0.99:
        samples = [s / peak * 0.95 for s in samples]
    with wave.open(str(path), "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(SR)
        w.writeframes(b"".join(struct.pack("<h", int(max(-1, min(1, s)) * 32767)) for s in samples))
    print("wrote", path.name, f"{len(samples)/SR:.2f}s")


def main():
    # Погнали: 880 x2, 1100, восходящий 300->1200
    write("go.wav", tone(880, 120) + tone(880, 120) + tone(1100, 120) + tone(300, 220, sweep_to=1200))
    write("tick.wav", tone(1000, 60, vol=0.9))
    write("tick2.wav", tone(800, 60, vol=0.9))
    write("tick3.wav", tone(600, 60, vol=0.95))
    write("time_up.wav", tone(900, 400, sweep_to=400))
    write("win.wav", tone(523, 400, harmonics=((1.25, 0.5), (1.5, 0.5))))
    write("skip.wav", tone(440, 400, harmonics=((1.19, 0.4), (1.5, 0.35))))
    write("click.wav", tone(2000, 25, vol=0.35))
    write("fanfare.wav", tone(523, 140) + tone(659, 140) + tone(784, 140))
    write("ding.wav", tone(1568, 120) + silence(60) + tone(2093, 160))


if __name__ == "__main__":
    main()
