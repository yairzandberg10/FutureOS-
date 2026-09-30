#!/usr/bin/env python3
"""FutureOS reel soundtrack, synthesised from scratch: no samples, nothing downloaded.

Reads src/reel/timeline.json (fps, length, BPM and every key press) so the key clicks land on the
exact frames where the keys glow in the picture. 120 BPM at 30 fps = one beat every 15 frames.

    python3 reel/make-audio.py        # writes public/reel/bed.m4a (and reel/bed.wav for checking)

Needs numpy and scipy, plus `npx remotion ffmpeg` for the AAC encode.
"""
import json
import subprocess
from pathlib import Path

import numpy as np
from scipy.io import wavfile
from scipy.signal import butter, fftconvolve, sosfilt

HERE = Path(__file__).resolve().parent
ROOT = HERE.parent
TL = json.loads((ROOT / "src/reel/timeline.json").read_text())

SR = 48000
FPS = TL["fps"]
TOTAL = TL["total"]
BEAT = 60.0 / TL["bpm"]  # 0.5 s
N = int(round(SR * TOTAL / FPS))
rng = np.random.default_rng(7)


def t_of(frame):
    return frame / FPS


def midi(m):
    return 440.0 * 2 ** ((m - 69) / 12)


# ----------------------------------------------------------------------------- building blocks
def tt(n):
    return np.arange(n) / SR


def exp_env(n, tau):
    return np.exp(-tt(n) / tau)


def sos(kind, fc, order=2):
    return butter(order, fc, btype=kind, fs=SR, output="sos")


def lp(x, fc, order=2):
    return sosfilt(sos("low", fc, order), x)


def hp(x, fc, order=2):
    return sosfilt(sos("high", fc, order), x)


def bp(x, lo, hi, order=2):
    return sosfilt(butter(order, [lo, hi], btype="band", fs=SR, output="sos"), x)


def noise(n):
    return rng.standard_normal(n)


def saw(freq, n, phase=0.0):
    return 2.0 * (((tt(n) * freq) + phase) % 1.0) - 1.0


def sine(freq, n):
    return np.sin(2 * np.pi * freq * tt(n))


def fade_edges(x, a=0.003, r=0.01):
    na, nr = int(a * SR), int(r * SR)
    x = x.copy()
    if na:
        x[:na] *= np.linspace(0, 1, na)
    if nr:
        x[-nr:] *= np.linspace(1, 0, nr)
    return x


class Bus:
    def __init__(self):
        self.l = np.zeros(N)
        self.r = np.zeros(N)

    def add(self, sig, t, pan=0.0, gain=1.0):
        i = int(round(t * SR))
        if i >= N or i + len(sig) <= 0:
            return
        a = max(0, -i)
        b = min(len(sig), N - i)
        seg = sig[a:b] * gain
        # constant-power pan, -1 (left) .. 1 (right)
        ang = (pan + 1) * np.pi / 4
        self.l[i + a : i + b] += seg * np.cos(ang)
        self.r[i + a : i + b] += seg * np.sin(ang)

    def add_stereo(self, l, r, t, gain=1.0):
        i = int(round(t * SR))
        if i >= N:
            return
        b = min(len(l), N - i)
        self.l[i : i + b] += l[:b] * gain
        self.r[i : i + b] += r[:b] * gain


# ----------------------------------------------------------------------------- instruments
def kick():
    n = int(0.5 * SR)
    t = tt(n)
    freq = 46 + 130 * np.exp(-t / 0.028)
    body = np.sin(2 * np.pi * np.cumsum(freq) / SR) * np.exp(-t / 0.16)
    click = hp(noise(n), 2500) * np.exp(-t / 0.004) * 0.35
    return fade_edges(np.tanh(1.7 * body) + click, 0.0005, 0.02)


def clap():
    n = int(0.45 * SR)
    t = tt(n)
    nz = bp(noise(n), 1100, 7500)
    env = np.zeros(n)
    for off in (0.0, 0.011, 0.022):
        k = int(off * SR)
        env[k:] += np.exp(-tt(n - k) / 0.006)
    env += 0.55 * np.exp(-t / 0.11) * (t > 0.03)
    body = np.sin(2 * np.pi * 185 * t) * np.exp(-t / 0.05) * 0.25
    return fade_edges(nz * env * 0.6 + body, 0.0005, 0.03)


def hat(open_=False):
    n = int((0.32 if open_ else 0.09) * SR)
    e = exp_env(n, 0.09 if open_ else 0.022)
    return fade_edges(hp(noise(n), 7200) * e, 0.0003, 0.01)


def bass(freq, dur):
    n = int(dur * SR)
    t = tt(n)
    env = np.minimum(1, t / 0.006) * np.exp(-t / (dur * 0.9)) * np.minimum(1, (dur - t) / 0.03)
    x = lp(saw(freq, n) * 0.6 + np.sin(2 * np.pi * freq * t), 420, 2)
    return x * env


def pad_voice(freqs, dur, detune):
    n = int(dur * SR)
    t = tt(n)
    x = np.zeros(n)
    for f in freqs:
        for d in (-detune, detune):
            x += saw(f * 2 ** (d / 1200), n, phase=rng.random())
    x = lp(x / (len(freqs) * 2), 1500, 2)
    env = np.minimum(1, t / 0.45) * np.minimum(1, (dur - t) / 0.9)
    return x * np.clip(env, 0, 1)


def pluck(freq, dur=0.6):
    n = int(dur * SR)
    t = tt(n)
    cutoff = 600 + 4200 * np.exp(-t / 0.05)
    x = saw(freq, n) * 0.55 + np.sign(np.sin(2 * np.pi * freq * t)) * 0.3
    # cheap time-varying low-pass: blend a dark and a bright copy
    dark, bright = lp(x, 900), lp(x, 5200)
    mix = np.clip((cutoff - 600) / 4200, 0, 1)
    x = dark * (1 - mix) + bright * mix
    return fade_edges(x * np.exp(-t / 0.2), 0.001, 0.02)


def click(key):
    n = int(0.12 * SR)
    t = tt(n)
    if key == "ok":
        sweep = np.sin(2 * np.pi * np.cumsum(520 + 420 * np.minimum(1, t / 0.07)) / SR) * np.exp(-t / 0.06)
        thunk = np.sin(2 * np.pi * 120 * t) * np.exp(-t / 0.03)
        tick = hp(noise(n), 3000) * np.exp(-t / 0.003)
        return fade_edges(0.45 * sweep + 0.55 * thunk + 0.35 * tick, 0.0005, 0.02)
    if key in ("up", "down", "left", "right"):
        f = {"up": 2600, "down": 2000, "left": 2300, "right": 2300}[key]
    else:
        f = 1700 + 110 * (int(key) if key.isdigit() else 5)
    tick = np.sin(2 * np.pi * f * t) * np.exp(-t / 0.004)
    nz = hp(noise(n), 3500) * np.exp(-t / 0.0028)
    thunk = np.sin(2 * np.pi * 170 * t) * np.exp(-t / 0.022)
    return fade_edges(0.5 * tick + 0.4 * nz + 0.35 * thunk, 0.0002, 0.015)


def whoosh(dur, rising=True):
    n = int(dur * SR)
    t = tt(n) / dur
    x = noise(n)
    dark, bright = lp(x, 700), hp(x, 3200)
    mix = t if rising else 1 - t
    y = dark * (1 - mix) + bright * mix
    env = np.sin(np.pi * np.clip(t, 0, 1)) ** 1.6
    return fade_edges(y * env, 0.002, 0.02)


def riser(dur):
    n = int(dur * SR)
    t = tt(n) / dur
    nz = hp(noise(n), 1800) * t**2.2
    tone = np.sin(2 * np.pi * np.cumsum(180 * 2 ** (t * 3.4)) / SR) * t**2.6 * 0.35
    return fade_edges(nz * 0.8 + tone, 0.01, 0.002)


def impact(dur=1.6):
    n = int(dur * SR)
    t = tt(n)
    freq = 38 + 90 * np.exp(-t / 0.05)
    sub = np.sin(2 * np.pi * np.cumsum(freq) / SR) * np.exp(-t / 0.55)
    boom = lp(noise(n), 500) * np.exp(-t / 0.18) * 0.6
    return fade_edges(np.tanh(1.3 * (sub + boom)), 0.0005, 0.05)


def crash(dur=2.2):
    n = int(dur * SR)
    t = tt(n)
    l = hp(noise(n), 4800) * np.exp(-t / 0.55)
    r = hp(noise(n), 4800) * np.exp(-t / 0.55)
    return fade_edges(l, 0.0005, 0.05), fade_edges(r, 0.0005, 0.05)


def reverb(l, r, seconds=1.2, wet=0.25):
    n = int(seconds * SR)
    t = tt(n)
    ir_l = noise(n) * np.exp(-t / 0.32) * (1 - np.exp(-t / 0.012))
    ir_r = noise(n) * np.exp(-t / 0.32) * (1 - np.exp(-t / 0.012))
    ir_l, ir_r = lp(ir_l, 7000), lp(ir_r, 7000)
    ir_l /= np.sqrt(np.sum(ir_l**2))
    ir_r /= np.sqrt(np.sum(ir_r**2))
    return fftconvolve(l, ir_l)[:N] * wet, fftconvolve(r, ir_r)[:N] * wet


# ----------------------------------------------------------------------------- arrangement
drums, music, sfx, verb_in = Bus(), Bus(), Bus(), Bus()
kick_times = []

CHORDS = [  # Am, F, C, G (one per bar = 4 beats = 60 frames), starting at the drop on frame 75
    {"root": 33, "notes": [57, 60, 64, 69]},
    {"root": 29, "notes": [53, 57, 60, 65]},
    {"root": 36, "notes": [60, 64, 67, 72]},
    {"root": 31, "notes": [55, 59, 62, 67]},
]
DROP = 75
OUTRO = 665
ARP_PATTERN = [0, 1, 2, 3, 2, 1, 2, 1]
BAR = 4 * 15

K = kick()
C = clap()
H_CLOSED, H_OPEN = hat(False), hat(True)

# -- hook (0-75): a thump when the words land, a swipe on the strike, a riser into the drop
sfx.add(impact(0.9), t_of(6), gain=0.22)
sfx.add(hp(whoosh(0.22, True), 1500), t_of(33), gain=0.55)
sfx.add(click("ok"), t_of(34), gain=0.5)
sfx.add(impact(1.0), t_of(56), gain=0.45)
sfx.add(riser(t_of(DROP - 58)), t_of(58), gain=0.6)

# -- the drop
sfx.add(impact(1.8), t_of(DROP), gain=0.95)
cl, cr = crash()
sfx.add_stereo(cl, cr, t_of(DROP), gain=0.32)
sfx.add(whoosh(0.9, True), t_of(DROP + 3), gain=0.5)

BREAKDOWN = (450, 520)
for f in range(DROP, OUTRO, 15):
    t = t_of(f)
    in_breakdown = BREAKDOWN[0] <= f < BREAKDOWN[1]
    if not in_breakdown:
        drums.add(K, t, gain=0.8)
        kick_times.append(t)
    beat_n = (f - DROP) // 15
    if f >= DROP + 30 and not in_breakdown:
        drums.add(H_CLOSED, t + BEAT / 2, pan=0.25, gain=0.22)
        if f >= 285:
            drums.add(H_CLOSED, t, pan=-0.2, gain=0.1)
    if f >= 285 and beat_n % 2 == 1 and not in_breakdown:
        drums.add(C, t, gain=0.5)
    if f >= 565 and beat_n % 4 == 3:
        drums.add(H_OPEN, t + BEAT / 2, pan=0.3, gain=0.22)
    if in_breakdown and beat_n % 2 == 1:
        drums.add(H_CLOSED, t, pan=0.2, gain=0.12)

# -- snare roll into the app wall
for i, f in enumerate(np.arange(535, 565, 3.75)):
    drums.add(C, t_of(f), gain=0.16 + 0.03 * i)
sfx.add(riser(t_of(30)), t_of(535), gain=0.5)

# -- bass, pad and arp follow the chords
for bar_start in range(DROP, OUTRO, BAR):
    ch = CHORDS[((bar_start - DROP) // BAR) % 4]
    t0 = t_of(bar_start)
    root = midi(ch["root"])
    for off_beats, dur in ((0, 0.42), (1.5, 0.22), (2, 0.42), (3.5, 0.22)):
        if BREAKDOWN[0] <= bar_start < BREAKDOWN[1]:
            continue
        music.add(bass(root, dur), t0 + off_beats * BEAT, gain=0.62)
    # pad
    pad_l = pad_voice([midi(m) for m in ch["notes"][:3]], BAR / FPS + 0.5, 9)
    pad_r = pad_voice([midi(m) for m in ch["notes"][:3]], BAR / FPS + 0.5, 13)
    lvl = 0.30 if not (BREAKDOWN[0] <= bar_start < BREAKDOWN[1]) else 0.42
    music.add(pad_l, t0 - 0.05, pan=-0.45, gain=lvl)
    music.add(pad_r, t0 - 0.05, pan=0.45, gain=lvl)
    verb_in.add(pad_l, t0 - 0.05, pan=0, gain=0.15)
    # arp (8th notes), with a ping-pong echo
    if bar_start + BAR > 105:
        for k, idx in enumerate(ARP_PATTERN):
            f = bar_start + k * 7.5
            if f < 105:
                continue
            lvl_a = 0.15 if f < 170 else (0.27 if not (BREAKDOWN[0] <= f < BREAKDOWN[1]) else 0.33)
            note = midi(ch["notes"][idx] + 12)
            p = pluck(note)
            pan = -0.35 if k % 2 == 0 else 0.35
            music.add(p, t_of(f), pan=pan, gain=lvl_a)
            music.add(p, t_of(f) + 0.375, pan=-pan, gain=lvl_a * 0.38)
            verb_in.add(p, t_of(f), pan=0, gain=lvl_a * 0.6)

# -- key clicks: on the exact frames where the keys light up
for p in TL["presses"]:
    pan = {"left": -0.35, "right": 0.35}.get(p["key"], 0.0)
    sfx.add(click(p["key"]), t_of(p["at"]), pan=pan, gain=0.85)
    verb_in.add(click(p["key"]), t_of(p["at"]), gain=0.25)

# -- camera / screen transitions
for f, dur, rising in ((162, 0.45, True), (246, 0.4, True), (296, 0.5, False), (444, 0.5, False), (464, 0.45, True), (492, 0.6, True)):
    sfx.add(whoosh(dur, rising), t_of(f), gain=0.32)

# -- the phone and titles land
for f in (196, 240, 286, 300, 451, 459):
    sfx.add(impact(0.5), t_of(f), gain=0.16)

# -- app wall: an iris, then icons popping in
sfx.add(impact(1.6), t_of(565), gain=0.8)
cl, cr = crash(1.8)
sfx.add_stereo(cl, cr, t_of(565), gain=0.30)
sfx.add(whoosh(0.7, True), t_of(562), gain=0.5)
pent = [76, 79, 81, 84, 86, 88, 91, 93]
for i in range(22):
    f = 565 + 16 + i * 2.2 + 3
    sfx.add(pluck(midi(pent[i % len(pent)] + (12 if i > 8 else 0)), 0.35), t_of(f), pan=(-0.6 + 1.2 * (i % 5) / 4), gain=0.13)

# -- outro: resolve to C, draw the logo with rising plucks, two taps for the tagline
sfx.add(impact(2.2), t_of(OUTRO), gain=0.8)
cl, cr = crash(2.6)
sfx.add_stereo(cl, cr, t_of(OUTRO), gain=0.30)
sfx.add(whoosh(0.7, False), t_of(OUTRO - 3), gain=0.5)
outro_len = (TOTAL - OUTRO) / FPS
for notes, pan in (([48, 55, 64, 71], -0.4), ([50, 59, 67, 74], 0.4)):
    music.add(pad_voice([midi(m) for m in notes], outro_len + 0.3, 10), t_of(OUTRO), pan=pan, gain=0.42)
    verb_in.add(pad_voice([midi(m) for m in notes], outro_len + 0.3, 10), t_of(OUTRO), gain=0.2)
music.add(bass(midi(36), 2.6), t_of(OUTRO), gain=0.6)
for i, m in enumerate([76, 79, 81, 84, 86, 88]):
    f = OUTRO + 14 + i * 4 + 6
    sfx.add(pluck(midi(m), 0.9), t_of(f), pan=-0.5 + 0.2 * i, gain=0.22)
    verb_in.add(pluck(midi(m), 0.9), t_of(f), gain=0.2)
for f in (OUTRO + 50, OUTRO + 62):
    sfx.add(click("ok"), t_of(f), gain=0.5)
for i, f in enumerate(range(OUTRO + 15, TOTAL - 20, 30)):
    drums.add(K, t_of(f), gain=0.55 * max(0.0, 1 - i / 4.2))
    kick_times.append(t_of(f))

# ----------------------------------------------------------------------------- mix
duck = np.ones(N)
for kt in kick_times:
    i = int(kt * SR)
    m = min(int(0.3 * SR), N - i)
    if m > 0:
        duck[i : i + m] *= 1 - 0.5 * np.exp(-tt(m) / 0.11)

vl, vr = reverb(verb_in.l, verb_in.r)
L = drums.l + music.l * duck + sfx.l + vl
R = drums.r + music.r * duck + sfx.r + vr

L, R = hp(L, 28), hp(R, 28)
L, R = np.tanh(1.25 * L) / np.tanh(1.25), np.tanh(1.25 * R) / np.tanh(1.25)
peak = max(np.abs(L).max(), np.abs(R).max())
L, R = L * (0.9 / peak), R * (0.9 / peak)

fin, fout = int(0.004 * SR), int(0.7 * SR)
for ch in (L, R):
    ch[:fin] *= np.linspace(0, 1, fin)
    ch[-fout:] *= np.linspace(1, 0, fout) ** 1.5

rms = np.sqrt(np.mean(np.concatenate([L, R]) ** 2))
print(f"{N / SR:.2f} s, peak 0.90, rms {20 * np.log10(rms):.1f} dBFS")

wav = HERE / "bed.wav"
wavfile.write(wav, SR, (np.stack([L, R], axis=1) * 32767).astype(np.int16))
out = ROOT / "public/reel/bed.m4a"
out.parent.mkdir(parents=True, exist_ok=True)
subprocess.run(
    ["npx", "remotion", "ffmpeg", "-y", "-loglevel", "error", "-i", str(wav), "-c:a", "aac", "-b:a", "192k", "-f", "mp4", str(out)],
    cwd=ROOT,
    check=True,
)
print("wrote", out.relative_to(ROOT))
