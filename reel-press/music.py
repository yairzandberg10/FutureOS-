"""The sound of the reel: an original track plus every key click, synthesised from scratch with numpy. No samples, no libraries of
music: every note is computed here, so the file is ours.

    python3 music.py            -> out/audio.wav (48 kHz stereo, 60 s) and out/audio-report.txt

Timing comes from js/cues.js (through node), the same file the picture reads, so a key on screen is a click in the mix.
120 BPM, A minor (Am - F - C - G). The plan, in bars of 2 s:
   1-2   the noise: twenty pings, out of tune on purpose, faster and faster; then one beat of nothing
   2-3   the question: a low boom, a pad, a few keys
   4-6   the phone rises: a soft pulse grows into a build
   7-18  the tour: four on the floor, a rolling bass, plucks, a lead that comes in at bar 11
   19-22 in your colour: the drums drop out, a music box plays the accent picks, a riser and a snare roll
   23-27 the second drop
   28-30 the keypad is played top to bottom, one big chord on "FutureOS", and a tail
"""
import json
import subprocess
from pathlib import Path

import numpy as np
from scipy import signal
from scipy.io import wavfile

ROOT = Path(__file__).resolve().parent
SR = 48000
DUR = 60.0
N = int(SR * DUR)
BPM = 120
BEAT = 60 / BPM
BAR = 4 * BEAT
RNG = np.random.default_rng(20260930)

TL = json.loads(subprocess.run(["node", "-e", "console.log(JSON.stringify(require('./js/cues.js')))"], cwd=ROOT,
                               capture_output=True, text=True, check=True).stdout)

# ------------------------------------------------------------------ helpers
mtof = lambda m: 440.0 * 2 ** ((m - 69) / 12)
tt = lambda n: np.arange(n) / SR
db = lambda x: 10 ** (x / 20)


def sos_lp(fc, order=2): return signal.butter(order, min(fc, SR * 0.45) / (SR / 2), "low", output="sos")
def sos_hp(fc, order=2): return signal.butter(order, fc / (SR / 2), "high", output="sos")
def sos_bp(lo, hi, order=2): return signal.butter(order, [lo / (SR / 2), min(hi, SR * 0.45) / (SR / 2)], "band", output="sos")
def lp(x, fc, order=2): return signal.sosfilt(sos_lp(fc, order), x)
def hp(x, fc, order=2): return signal.sosfilt(sos_hp(fc, order), x)
def bp(x, lo, hi, order=2): return signal.sosfilt(sos_bp(lo, hi, order), x)


def fade(x, a=0.002, r=0.004):
    n = len(x); na, nr = min(int(a * SR), n), min(int(r * SR), n)
    if na: x[:na] *= np.linspace(0, 1, na)
    if nr: x[-nr:] *= np.linspace(1, 0, nr)
    return x


def saw(f, n, ph=0.0):
    """band-limited saw (polyBLEP); f may be a number or an array"""
    dt = np.broadcast_to(np.asarray(f, dtype=np.float64) / SR, (n,))
    p = (ph + np.cumsum(dt)) % 1.0
    y = 2 * p - 1
    m = p < dt; x = p[m] / dt[m]; y[m] -= x + x - x * x - 1
    m = p > 1 - dt; x = (p[m] - 1) / dt[m]; y[m] -= x * x + x + x + 1
    return y


def sine(f, n, ph=0.0):
    f = np.broadcast_to(np.asarray(f, dtype=np.float64), (n,))
    return np.sin(2 * np.pi * (ph + np.cumsum(f) / SR))


def env_ad(n, a, tau):
    t = tt(n); e = np.exp(-t / tau)
    na = max(1, int(a * SR)); e[:na] *= np.linspace(0, 1, na)
    return e


def env_sus(n, a, r, dur):
    """attack, sustain for dur seconds, release r"""
    e = np.ones(n); na = max(1, int(a * SR)); e[:na] = np.linspace(0, 1, na)
    nd = int(dur * SR)
    if nd < n:
        nr = n - nd; e[nd:] *= np.linspace(1, 0, nr) ** 2
    return e


# ------------------------------------------------------------------ buses
BUS = {k: np.zeros((2, N), dtype=np.float32) for k in ["drums", "bass", "pad", "arp", "lead", "bells", "keys", "fx"]}
SEND = np.zeros((2, N), dtype=np.float32)      # into the reverb
KICKS = []                                      # times used for the side-chain


def put(bus, sig, t0, gain=1.0, pan=0.0, send=0.0):
    if isinstance(sig, tuple):                  # stereo pair
        l, r = sig
    else:
        g = np.pi / 4 * (pan + 1); l, r = sig * np.cos(g), sig * np.sin(g)
    i = int(round(t0 * SR))
    if i >= N or i + len(l) <= 0: return
    a = max(0, -i); b = min(len(l), N - i)
    BUS[bus][0, i + a:i + b] += (l[a:b] * gain).astype(np.float32)
    BUS[bus][1, i + a:i + b] += (r[a:b] * gain).astype(np.float32)
    if send:
        SEND[0, i + a:i + b] += (l[a:b] * gain * send).astype(np.float32)
        SEND[1, i + a:i + b] += (r[a:b] * gain * send).astype(np.float32)


# ------------------------------------------------------------------ instruments
def kick(vel=1.0):
    n = int(0.5 * SR); t = tt(n)
    f = 44 + 150 * np.exp(-t / 0.026)
    body = np.sin(2 * np.pi * np.cumsum(f) / SR) * np.exp(-t / 0.19)
    click = hp(RNG.standard_normal(n), 2500) * np.exp(-t / 0.0025) * 0.22
    return fade((body + click) * vel, 0.0005, 0.02)


def clap(vel=1.0):
    n = int(0.34 * SR); t = tt(n)
    e = sum(np.exp(-np.clip(t - o, 0, None) / 0.009) * (t >= o) for o in (0, 0.011, 0.022)) * 0.7 + np.exp(-t / 0.085) * (t >= 0.022) * 0.9
    return fade(bp(RNG.standard_normal(n), 1000, 4200) * e * vel * 2.2, 0.0005, 0.02)


def hat(open_=False, vel=1.0):
    n = int((0.42 if open_ else 0.09) * SR); t = tt(n)
    x = hp(RNG.standard_normal(n), 7000) * np.exp(-t / (0.11 if open_ else 0.016))
    return fade(x * vel, 0.0005, 0.01)


def shaker(vel=1.0):
    n = int(0.12 * SR); t = tt(n)
    return fade(bp(RNG.standard_normal(n), 4500, 10000) * env_ad(n, 0.012, 0.035) * vel, 0.001, 0.02)


def bass_note(m, dur, vel=1.0):
    f = mtof(m); n = int((dur + 0.06) * SR); t = tt(n)
    sub = np.sin(2 * np.pi * f * t)
    body = lp(saw(f, n), 240) * 0.9 + lp(saw(f * 2, n), 500) * 0.25 + lp(saw(f, n), 1400) * 0.22
    e = (0.55 + 0.45 * np.exp(-t / 0.09)) * env_sus(n, 0.004, 0.05, dur)
    return (sub * 0.9 + body) * e * vel


def pluck(m, dur, vel=1.0, bright=1.0):
    f = mtof(m); n = int(max(dur, 0.25) * SR * 1.3); t = tt(n)
    x = (saw(f * 2 ** (-6 / 1200), n) + saw(f * 2 ** (6 / 1200), n, 0.3)) * 0.5
    e = np.exp(-t / 0.075)
    y = lp(x, 6500 * bright) * e + lp(x, 900) * (1 - e)
    return fade(y * env_ad(n, 0.002, 0.14) * vel, 0.001, 0.02)


def pad_chord(ms, dur, bright=1.0):
    n = int((dur + 0.9) * SR); l = np.zeros(n); r = np.zeros(n)
    for m in ms:
        f = mtof(m)
        for k, d in enumerate((-11, -4, 3, 10)):
            s = saw(f * 2 ** (d / 1200), n, RNG.random())
            (l if k % 2 == 0 else r)[:] += s
    e = env_sus(n, 0.45, 0.9, dur)
    return lp(l, 1100 + 1500 * bright, 2) * e / len(ms) * 0.55, lp(r, 1100 + 1500 * bright, 2) * e / len(ms) * 0.55


def lead_note(m, dur, vel=1.0, oct_up=False):
    f = mtof(m) * (2 if oct_up else 1); n = int((dur + 0.12) * SR); t = tt(n)
    vib = 1 + 0.004 * np.sin(2 * np.pi * 5.6 * t) * np.clip((t - 0.12) / 0.2, 0, 1)
    x = saw(f * vib * 2 ** (-7 / 1200), n) * 0.6 + saw(f * vib * 2 ** (7 / 1200), n, 0.4) * 0.6 + np.sign(np.sin(2 * np.pi * f * vib * t)) * 0.18
    return lp(x, 3400, 2) * env_sus(n, 0.01, 0.1, dur) * vel * 0.55


def bell(f, dur=0.6, vel=1.0, bright=1.0):
    n = int(dur * SR * 1.6); t = tt(n)
    mod = np.sin(2 * np.pi * f * 3.5 * t) * 2.2 * bright * np.exp(-t / 0.07)
    car = np.sin(2 * np.pi * f * t + mod) * np.exp(-t / (dur / 2.4))
    p2 = np.sin(2 * np.pi * f * 2.005 * t) * 0.28 * np.exp(-t / (dur / 4))
    return fade((car + p2) * vel, 0.001, 0.04)


def tick(f=1800, vel=1.0, dur=0.05):
    n = int(dur * SR); t = tt(n)
    return fade((hp(RNG.standard_normal(n), 2500) * np.exp(-t / 0.004) * 0.5 + np.sin(2 * np.pi * f * t) * np.exp(-t / (dur / 3))) * vel, 0.0003, 0.01)


def noise_sweep(dur, f0, f1, rising=True, peak=0.5, power=2.0, q=1.6):
    """band-limited noise whose centre moves from f0 to f1; loudness follows a rising (or falling) curve"""
    n = int(dur * SR); x = RNG.standard_normal(n); y = np.zeros(n); B = 256; zi = None
    for i in range(0, n, B):
        p = i / n; fc = f0 * (f1 / f0) ** p
        sos = signal.butter(2, [fc / q / (SR / 2), min(fc * q, SR * 0.45) / (SR / 2)], "band", output="sos")
        if zi is None: zi = np.zeros((sos.shape[0], 2))
        y[i:i + B], zi = signal.sosfilt(sos, x[i:i + B], zi=zi)   # the state is carried across blocks, so the sweep stays smooth
    a = np.linspace(0, 1, n) ** power
    return y * (a if rising else a[::-1]) * peak


def boom(vel=1.0, dur=2.4):
    n = int(dur * SR); t = tt(n)
    f = 36 + 70 * np.exp(-t / 0.22)
    body = np.sin(2 * np.pi * np.cumsum(f) / SR) * np.exp(-t / 0.95)
    rumble = lp(RNG.standard_normal(n), 160) * np.exp(-t / 0.4) * 0.8
    return fade((body + rumble) * vel, 0.001, 0.05)


def crash(vel=1.0, dur=2.6):
    n = int(dur * SR); t = tt(n)
    return fade(hp(RNG.standard_normal(n), 3200) * np.exp(-t / 0.75) * vel * 0.5, 0.001, 0.05)


def ping(f, vel=1.0):
    n = int(0.3 * SR); t = tt(n)
    x = np.sin(2 * np.pi * f * t) * np.exp(-t / 0.055) + 0.4 * np.sin(2 * np.pi * f * 2.0 * t) * np.exp(-t / 0.03)
    return fade(x * vel, 0.0005, 0.02)


# ------------------------------------------------------------------ the score
CH = [dict(name="Am", root=33, up=[57, 60, 64, 69]), dict(name="F", root=29, up=[57, 60, 65, 69]),
      dict(name="C", root=36, up=[55, 60, 64, 67]), dict(name="G", root=31, up=[55, 59, 62, 67])]
chord = lambda bar: CH[(bar - 7) % 4]
bar_t = lambda b: (b - 1) * BAR
PENT = {"d1": 69, "d2": 72, "d3": 74, "d4": 76, "d5": 79, "d6": 81, "d7": 84, "d8": 86, "d9": 88, "star": 91, "d0": 93, "pound": 96}

# ---- pings of the noise: on purpose not in tune with anything (chromatic, jumping)
PING_NOTES = [81, 85, 89, 83, 87, 80, 86, 82, 90, 84, 88, 81, 85, 91, 83, 89, 87, 92, 84, 90]
for s in [x for x in TL["sfx"] if x["s"] == "ping"]:
    i = s["i"]
    put("fx", ping(mtof(PING_NOTES[i]), 1.0), s["t"], gain=1.5, pan=RNG.uniform(-0.7, 0.7), send=0.05)
    put("fx", tick(900 + 40 * i, 0.9, 0.03), s["t"], gain=0.9)

# a drone under the noise: a tritone that climbs and tightens, then is cut
n_ = int(2.75 * SR); tm_ = tt(n_)
drone = (saw(mtof(45), n_) + saw(mtof(51) * 1.003, n_)) * 0.5
drone = np.concatenate([lp(drone[i:i + 4096], 250 + 2600 * (i / n_) ** 2) for i in range(0, n_, 4096)]) if False else drone
sw_ = np.zeros(n_); zi_ = None
for i in range(0, n_, 512):
    sos_ = sos_lp(200 + 2400 * (i / n_) ** 2)
    if zi_ is None: zi_ = np.zeros((sos_.shape[0], 2))
    sw_[i:i + 512], zi_ = signal.sosfilt(sos_, drone[i:i + 512], zi=zi_)
put("fx", sw_ * (0.15 + 0.85 * (tm_ / 2.75) ** 1.5) * 0.75, 0.0, send=0.05)

# ---- drums
def kick_at(t, v=1.0, bus_gain=1.0):
    put("drums", kick(v), t, gain=bus_gain); KICKS.append((t, v))

def four(b0, b1, v0=1.0, v1=1.0):
    n = int((b1 - b0) * 4 + 0.5)
    for k in range(n):
        kick_at(bar_t(b0) + k * BEAT, v0 + (v1 - v0) * k / max(1, n - 1))

# build: a soft pulse from 4.0, the kick from bar 4
for k in range(4):
    put("drums", kick(0.35), 4.0 + k * BEAT, gain=0.7)             # heartbeat under the dialog
four(4, 5, 0.5, 0.6); four(5, 6, 0.6, 0.75); four(6, 7, 0.75, 0.95)
four(7, 19)                                                          # the first drop
four(23, 28)                                                         # the second drop
# the last bar of the tour and of the drop: kick on 1 and 3 only, then the chord
for k in (0, 2): kick_at(bar_t(28) + k * BEAT * 2, 0.9)

for b in list(range(7, 19)) + list(range(23, 28)):
    t0 = bar_t(b)
    for k in (1, 3): put("drums", clap(0.75 if b < 23 else 0.9), t0 + k * BEAT)
    for k in range(8):                                               # closed hats on the 8ths, open on the off-beats
        if k % 2: put("drums", hat(True, 0.30), t0 + k * BEAT / 2, gain=0.6, pan=0.25)
        else: put("drums", hat(False, 0.22 if k % 4 else 0.28), t0 + k * BEAT / 2, gain=0.55, pan=-0.2)
    if b >= 11:
        for k in range(16):                                          # 16th shaker from bar 11
            if k % 2: put("drums", shaker(0.35 + 0.1 * (k % 4 == 3)), t0 + k * BEAT / 4, gain=0.5, pan=0.4 if k % 4 == 1 else -0.4)
for b in (5, 6):
    t0 = bar_t(b)
    for k in range(8): put("drums", hat(False, 0.16 + 0.05 * (b == 6)), t0 + k * BEAT / 2, gain=0.5)

def roll(t_start, t_end, steps, v0, v1):
    for k, t in enumerate(np.linspace(t_start, t_end, steps, endpoint=False)):
        put("drums", clap(v0 + (v1 - v0) * k / steps), t, gain=0.8)

roll(bar_t(6) + BEAT * 2, bar_t(7), 8, 0.35, 0.9)                    # build into the drop
roll(bar_t(18) + BEAT * 2, bar_t(19), 8, 0.4, 0.8)                   # fill before the breakdown
# the big snare roll into the second drop: eighths, sixteenths, thirty-seconds
seg = [(42.0, 43.0, 4), (43.0, 43.75, 6), (43.75, 44.0, 6)]
tot = sum(s[2] for s in seg); idx = 0
for a, b_, st in seg:
    for t in np.linspace(a, b_, st, endpoint=False):
        put("drums", clap(0.25 + 0.75 * idx / tot), t, gain=0.85); idx += 1
put("drums", crash(1.0), bar_t(7), gain=0.7); put("drums", crash(0.7), bar_t(15), gain=0.5)
put("drums", crash(1.15), bar_t(23), gain=0.9); put("drums", crash(0.8), bar_t(27), gain=0.6)

# ---- bass: rolling eighths on the root, an octave up on two of them; sub only in the last bars
for b in list(range(7, 19)) + list(range(23, 28)):
    c = chord(b); t0 = bar_t(b)
    for k in range(8):
        m = c["root"] + (12 if k in (3, 6) else 0)
        put("bass", bass_note(m, BEAT / 2 * 0.9, 1.0 if k % 2 == 0 else 0.8), t0 + k * BEAT / 2)
for b in (5, 6):                                                     # a filtered pulse in the build
    c = chord(b); t0 = bar_t(b)
    for k in range(4): put("bass", bass_note(c["root"], BEAT * 0.8, 0.6 + 0.1 * (b == 6)), t0 + k * BEAT)
for b in (28,):
    put("bass", bass_note(33, BAR * 0.9, 0.9), bar_t(b))
put("bass", boom(0.9, 3.0), 56.0, gain=0.8)

# ---- pads
PADS = [(3.0, 6.0, CH[0], 0.6, 0.55)]
for b in range(4, 31):
    if b in (29, 30): continue
    PADS.append((bar_t(b), bar_t(b + 1), chord(b), 0.5 if b < 7 else 1.0 if not 19 <= b < 23 else 1.35, 1.0))
PADS.append((56.0, 60.0, dict(up=[57, 60, 64, 71]), 1.4, 1.1))
for t0, t1, c, br, g in PADS:
    put("pad", pad_chord(c["up"], t1 - t0, br), t0, gain=0.5 * g, send=0.35)
put("pad", pad_chord([45, 57, 64], 3.0, 1.0), 3.0, gain=0.45, send=0.4)

# ---- plucks: sixteenths, up and down the chord
ARP = [0, 1, 2, 3, 2, 1, 0, 1, 2, 3, 2, 1, 0, 2, 1, 3]
for b in list(range(5, 19)) + list(range(23, 28)):
    c = chord(b); t0 = bar_t(b)
    step = 2 if b < 7 else 1                                         # eighths in the build, sixteenths after
    for k in range(0, 16, step):
        m = c["up"][ARP[k]] + 12
        v = 0.55 * (1.0 if k % 4 == 0 else 0.6) * (0.5 if b < 7 else 1.0)
        put("arp", pluck(m, BEAT / 4, v, 0.8 + 0.2 * min(1, (b - 4) / 4)), t0 + k * BEAT / 4, pan=(-0.35, 0.35)[k % 2], send=0.2)

# ---- the lead, four bars at a time
LEAD = {0: [(0, 76, 3), (3, 74, 1), (4, 72, 2), (6, 69, 2)], 1: [(0, 72, 3), (3, 69, 1), (4, 65, 2), (6, 69, 2)],
        2: [(0, 76, 3), (3, 79, 1), (4, 76, 2), (6, 72, 2)], 3: [(0, 74, 3), (3, 71, 1), (4, 74, 2), (6, 79, 2)]}
for b in list(range(11, 19)) + list(range(23, 28)):
    idx = (b - 7) % 4; t0 = bar_t(b)
    for st, m, ln in LEAD[idx]:
        d = ln * BEAT / 2 * 0.95
        oct_ = b >= 23
        put("lead", lead_note(m, d, 0.9, oct_up=False), t0 + st * BEAT / 2, gain=0.55, pan=-0.1, send=0.3)
        if oct_: put("lead", lead_note(m + 7, d, 0.5), t0 + st * BEAT / 2, gain=0.35, pan=0.2, send=0.3)

# ---- bells: sparse in the intro, a counter line in the second half of the first drop
for tb, m in ((3.6, 76), (4.6, 81), (5.3, 79), (5.75, 76)):
    put("bells", bell(mtof(m), 1.4, 0.35), tb, pan=0.3, send=0.6)
for b in range(15, 19):
    c = chord(b)
    put("bells", bell(mtof(c["up"][2] + 24), 0.8, 0.28), bar_t(b) + BEAT * 3.5, pan=0.5, send=0.5)
    put("bells", bell(mtof(c["up"][0] + 24), 0.8, 0.22), bar_t(b) + BEAT * 1.5, pan=-0.5, send=0.5)

# ---- the breakdown (bars 19-22): a music box, then a riser
for b in range(19, 23):
    c = chord(b); t0 = bar_t(b)
    for k in range(8):
        m = c["up"][(0, 1, 2, 3, 2, 1, 2, 3)[k]] + 24
        put("bells", bell(mtof(m), 0.55, 0.24 if k % 2 == 0 else 0.16, 0.7), t0 + k * BEAT / 2, pan=(-0.4, 0.4)[k % 2], send=0.5)
    put("bass", bass_note(c["root"] + 12, BAR * 0.95, 0.5), t0)
put("fx", noise_sweep(4.0, 300, 9000, True, 0.30, 2.4), 40.0, send=0.12)
put("fx", sine(np.linspace(180, 1900, int(4 * SR)) ** 1.0, int(4 * SR)) * np.linspace(0, 1, int(4 * SR)) ** 3 * 0.05, 40.0)
put("fx", noise_sweep(1.5, 5000, 400, False, 0.5, 2.0), 36.0, send=0.2)      # a down-sweep as the drums leave
put("fx", noise_sweep(2.0, 200, 5000, True, 0.35, 2.4), bar_t(6) + BEAT * 0, send=0.1)   # the build before the first drop

# ---- the ending: the key ripple gets a bed of harmony, and one large chord
for k, m in enumerate((57, 60, 64, 69, 72, 76)):
    put("bells", bell(mtof(m + 12), 1.0, 0.25), 56.0 + k * 0.045, pan=(-0.5, 0.5)[k % 2], send=0.55)
put("bells", bell(mtof(93), 2.2, 0.3), 56.0, pan=0.0, send=0.7)
put("drums", crash(1.2), 56.0, gain=0.8)
kick_at(56.0, 1.0)
for t in (56.0,): put("fx", boom(1.0, 3.0), t, gain=0.7)

# ------------------------------------------------------------------ the sounds of the picture (cues)
DPAD = {"up": 88, "down": 81, "left": 84, "right": 86}
for k in TL["keys"]:
    t, key = k["t"], k["k"]
    if key in DPAD:
        put("keys", bell(mtof(DPAD[key]), 0.22, 0.40, 0.5), t, pan=(-0.25 if key == "left" else 0.25 if key == "right" else 0), send=0.25)
        put("keys", tick(1500, 0.30), t, gain=0.5)
    elif key == "ok":
        n = int(0.25 * SR); x = (np.sin(2 * np.pi * 196 * tt(n)) * np.exp(-tt(n) / 0.09) + 0.6 * np.sin(2 * np.pi * 294 * tt(n)) * np.exp(-tt(n) / 0.07))
        put("keys", fade(x, 0.0005, 0.03) * 0.55, t, send=0.15)
        put("keys", tick(1100, 0.5), t, gain=0.6)
    elif key in PENT:
        put("keys", bell(mtof(PENT[key]), 0.34, 0.5, 0.9), t, pan=np.clip((PENT[key] - 84) / 24, -0.6, 0.6), send=0.35)
        put("keys", tick(2200, 0.25), t, gain=0.4)
    elif key == "call":
        put("keys", bell(mtof(79), 0.3, 0.5), t, send=0.3); put("keys", bell(mtof(84), 0.5, 0.5), t + 0.06, send=0.3)
    elif key in ("soft_l", "soft_r", "end"):
        put("keys", bell(mtof(72), 0.2, 0.35, 0.4), t, send=0.2); put("keys", tick(900, 0.4), t, gain=0.5)

for s in TL["sfx"]:
    t, kind = s["t"], s["s"]
    if kind == "buzz":
        n = int(s["dur"] * SR); tm = tt(n)
        put("fx", lp(saw(82, n) * (0.55 + 0.45 * np.sign(np.sin(2 * np.pi * 26 * tm))), 500) * 0.5, t, gain=0.7)
    elif kind == "boom":
        put("fx", boom(1.0), t, gain=1.0, send=0.35)
    elif kind == "wipe":
        n = int(0.55 * SR); tm = tt(n)
        put("fx", noise_sweep(0.55, 7000, 250, False, 0.6, 1.2) , t, send=0.25)
        put("fx", fade(np.sin(2 * np.pi * np.cumsum(900 * np.exp(-tm / 0.15) + 90) / SR) * np.exp(-tm / 0.2), 0.001, 0.03) * 0.4, t)
    elif kind == "hit":
        p = s["p"]
        if p in (0, 1):
            put("fx", boom(0.8 if p == 0 else 0.55, 1.0), t, send=0.3); put("fx", crash(0.4 if p == 0 else 0.3, 1.0), t, gain=0.5)
        elif p == 2:
            put("fx", boom(0.7, 1.4), t, send=0.3); put("bells", bell(mtof(81), 1.6, 0.5), t, send=0.6); put("bells", bell(mtof(88), 1.6, 0.35), t, send=0.6)
        elif p == 3:
            put("fx", boom(1.1, 2.4), t, send=0.3); put("fx", crash(1.0), t, gain=0.7)
        elif p == 4:
            put("fx", crash(1.0), t, gain=0.6)
    elif kind == "rise":
        put("fx", noise_sweep(s["dur"] + 0.1, 250, 7000, True, 0.5, 2.0), t, send=0.2)
        n = int((s["dur"] + 0.1) * SR)
        put("fx", sine(np.linspace(150, 1200, n), n) * np.linspace(0, 1, n) ** 2 * 0.09, t)
    elif kind == "push":
        d = s["dur"]
        put("fx", noise_sweep(d + 0.2, 500, 6000 if not s.get("back") else 1500, True, 0.4, 1.0) * np.hanning(int((d + 0.2) * SR)) * 2.0, t, send=0.2)
    elif kind == "ring":
        put("keys", bell(mtof(88), 0.35, 0.35), t, send=0.3); put("keys", bell(mtof(93), 0.5, 0.35), t + 0.12, send=0.3)
    elif kind == "connect":
        put("keys", bell(mtof(88), 0.4, 0.5), t, send=0.4); put("keys", bell(mtof(91), 0.7, 0.5), t + 0.09, send=0.4)
    elif kind == "type":
        put("keys", tick(1800 + (s.get("i", 0) * 137) % 900, 0.22), t, gain=0.5)
    elif kind == "translate":
        for j, m in enumerate((93, 96, 100)): put("keys", bell(mtof(m), 0.35, 0.35), t + j * 0.07, send=0.4)
    elif kind == "switch":
        put("keys", tick(2400 if s["on"] else 1300, 0.6), t, gain=0.6)
        put("keys", bell(mtof(84 if s["on"] else 72), 0.25, 0.35, 0.5), t + 0.01, send=0.25)
    elif kind == "count":
        put("keys", tick(1000 + 170 * s["i"], 0.3), t, gain=0.5)
    elif kind == "ir":
        for j in range(3): put("keys", tick(2800, 0.5, 0.02), t + j * 0.035, gain=0.5)
    elif kind == "listen":
        put("keys", bell(mtof(86), 0.35, 0.5), t, send=0.4); put("keys", bell(mtof(93), 0.6, 0.5), t + 0.1, send=0.4)
    elif kind == "chime":
        m = (84, 88, 91, 93, 96)[s["n"]]
        put("bells", bell(mtof(m), 1.0, 0.5, 0.9), t, pan=(-0.3, 0.3, -0.15, 0.15, 0)[s["n"]], send=0.55)
    elif kind == "theme":
        if s["light"]:
            for j, m in enumerate((88, 93)): put("bells", bell(mtof(m), 0.8, 0.4), t + j * 0.05, send=0.5)
        else:
            put("bells", bell(mtof(57), 0.7, 0.5, 0.4), t, send=0.4)
    elif kind == "tail":
        put("bells", bell(mtof(88), 2.0, 0.35), t, send=0.7); put("bells", bell(mtof(93), 2.0, 0.28), t + 0.08, send=0.7)

# ------------------------------------------------------------------ mix
def reverb_ir(rt=2.0, pre=0.018):
    n = int(SR * rt * 1.25); t = tt(n)
    ir = np.stack([RNG.standard_normal(n), RNG.standard_normal(n)])
    ir *= np.exp(-6.9 * t / rt)
    ir = np.stack([lp(hp(ir[c], 180), 7500) for c in range(2)])
    ir[:, :int(pre * SR)] = 0
    ir /= np.sqrt((ir ** 2).sum(axis=1, keepdims=True))
    return ir


def duck_curve(depth, tau=0.16, atk=0.004):
    d = np.ones(N, dtype=np.float64)
    for tk, v in KICKS:
        i = int(tk * SR); n = min(int(0.6 * SR), N - i)
        if n <= 0: continue
        t = np.arange(n) / SR
        g = 1 - depth * min(1, v) * np.exp(-t / tau)
        d[i:i + n] = np.minimum(d[i:i + n], g)
    return d.astype(np.float32)


duck = {k: duck_curve(dp) for k, dp in (("bass", 0.7), ("pad", 0.6), ("arp", 0.3), ("lead", 0.2))}
GAIN = dict(drums=db(-3), bass=db(-4), pad=db(-9), arp=db(-8), lead=db(-9), bells=db(-8), keys=db(-6), fx=db(-6))
mix = np.zeros((2, N), dtype=np.float64)
for k, b in BUS.items():
    x = b.astype(np.float64)
    if k == 'bass': x = np.tanh(1.5 * x) / np.tanh(1.5)     # a little saturation so the bass is heard on a phone speaker
    if k in duck: x = x * duck[k]
    mix += x * GAIN[k]

ir = reverb_ir()
wet = np.stack([signal.fftconvolve(SEND[c].astype(np.float64), ir[c])[:N] for c in range(2)])
mix += wet * db(-7)

# a moment of nothing before the dialog (2.75 - 3.0), a short fade at the very end
gate = np.ones(N)
a0, a1 = int(2.75 * SR), int(3.0 * SR)
gate[a0:a1] = 0.0
gate[a0 - int(0.004 * SR):a0] = np.linspace(1, 0, int(0.004 * SR))
gate[a1 - int(0.002 * SR):a1] = np.linspace(0, 1, int(0.002 * SR))
mix *= gate
mix[:, -int(0.6 * SR):] *= np.linspace(1, 0, int(0.6 * SR)) ** 1.5

mix = np.stack([signal.sosfilt(sos_hp(28), mix[c]) for c in range(2)])
peak0 = np.abs(mix).max()
mix = np.tanh(mix / (peak0 * 0.5)) * 0.5 / np.tanh(1)      # soft limiter: taming the peaks, not the body
mix *= 0.84 / np.abs(mix).max()
out = (mix.T * 32767).astype(np.int16)
(ROOT / "out").mkdir(exist_ok=True)
wavfile.write(ROOT / "out" / "audio.wav", SR, out)

# ------------------------------------------------------------------ a report, in place of listening
lines = [f"samples {N}, {DUR:.1f} s, peak {np.abs(mix).max():.3f}, clipped samples: {int((np.abs(out) >= 32767).sum())}"]
rms = [20 * np.log10(np.sqrt((mix[:, int(s * SR):int((s + 1) * SR)] ** 2).mean()) + 1e-9) for s in range(int(DUR))]
lines.append("RMS dBFS per second:")
for i in range(0, int(DUR), 10): lines.append(f"  {i:2d}-{i + 9:2d}: " + " ".join(f"{v:6.1f}" for v in rms[i:i + 10]))
spec = np.abs(np.fft.rfft(mix.mean(axis=0))) ** 2
fr = np.fft.rfftfreq(N, 1 / SR)
tot = spec.sum()
for lo, hi in ((0, 120), (120, 500), (500, 2000), (2000, 6000), (6000, 24000)):
    lines.append(f"  energy {lo:5d}-{hi:5d} Hz: {100 * spec[(fr >= lo) & (fr < hi)].sum() / tot:5.1f} %")
(ROOT / "out" / "audio-report.txt").write_text("\n".join(lines) + "\n")
print("\n".join(lines))
