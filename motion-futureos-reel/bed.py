"""bed.py: an original, synthesised sound bed for the Instagram reel (numpy only, nothing sampled or downloaded).
120 BPM, 4/4, beat 0 at t = 0, so every key press on the film's quarter-beat grid lands on the pulse.
It is a placeholder for a track the user will bring: CONFIG.music points at bed.wav; swap the file to replace it.

  python bed.py            writes bed.wav (48 kHz stereo) next to this file
Sections follow reel-edit.js (reel seconds): the drawing (pad and a breath), the drop on the solid at 3.0, the screen wakes
at 5.8, the lock (half-time pulse) 10.0-12.5, the full pulse to 26.0, the turn (a riser) 26.0-28.0, the mark (a hit and a
chord) at 28.15."""
import sys, wave
from pathlib import Path
import numpy as np

SR = 48000
BPM = 120.0
BEAT = 60.0 / BPM
DUR = 33.5
N = int(DUR * SR)
rng = np.random.default_rng(7)
t_all = np.arange(N) / SR


def lp(x, fc, order=2):
    X = np.fft.rfft(x); f = np.fft.rfftfreq(len(x), 1 / SR) + 1e-9
    return np.fft.irfft(X / np.sqrt(1 + (f / fc) ** (2 * order)), len(x))


def hp(x, fc, order=2):
    X = np.fft.rfft(x); f = np.fft.rfftfreq(len(x), 1 / SR) + 1e-9
    return np.fft.irfft(X / np.sqrt(1 + (fc / f) ** (2 * order)), len(x))


def env_seg(points):
    """piecewise-linear envelope over the whole film: [(t, v), ...]"""
    ts, vs = zip(*points)
    return np.interp(t_all, ts, vs)


def place(bus, x, t, g=1.0):
    i = int(round(t * SR))
    if i >= len(bus): return
    j = min(len(bus), i + len(x)); bus[i:j] += g * x[:j - i]


def saw(f, d, harm=18, detune=0.0):
    t = np.arange(int(d * SR)) / SR
    out = np.zeros_like(t)
    for h in range(1, harm + 1):
        if f * h > 9000: break
        out += np.sin(2 * np.pi * f * h * (1 + detune) * t + h * 0.7) / h
    return out


# ---------------------------------------------------------------- elements
def kick():
    d = 0.42; t = np.arange(int(d * SR)) / SR
    f = 44 + 110 * np.exp(-t * 38)
    body = np.sin(2 * np.pi * np.cumsum(f) / SR) * np.exp(-t * 7.5) * np.clip(t / 0.002, 0, 1)
    click = hp(lp(rng.standard_normal(len(t)), 3500), 900) * np.exp(-t * 260) * 0.25
    return np.tanh(1.4 * (body + click)) * 0.9


def hat(v=1.0):
    d = 0.06; t = np.arange(int(d * SR)) / SR
    x = hp(rng.standard_normal(len(t)), 7500, 3) * np.exp(-t * 95)
    return x / (np.abs(x).max() + 1e-9) * v


def pluck(f, d=0.34):
    t = np.arange(int(d * SR)) / SR
    x = saw(f, d, harm=10) + 0.5 * saw(f * 0.5, d, harm=6)
    # the filter closes after the attack: a soft, rounded pluck
    lo = lp(x, 400) * np.exp(-t * 9); hi = lp(x, 1300) * np.exp(-t * 30)
    return (0.8 * lo + 0.35 * hi) * np.clip(t / 0.004, 0, 1)


def pad_chord(freqs, d):
    x = np.zeros(int(d * SR))
    for f in freqs:
        for dt in (-0.0042, 0.0, 0.0047):
            x += saw(f, d, harm=14, detune=dt) / 3
    return x / len(freqs)


# ---------------------------------------------------------------- the score
# the section times come from the cut list in reel-edit.js, so the sound follows a change to the edit
import re
CUTS = [tuple(float(v) for v in m) for m in re.findall(r'\[(\d+\.?\d*), (\d+\.?\d*), (\d+\.?\d*), (\d+\.?\d*)\]', (Path(__file__).resolve().parent / 'reel-edit.js').read_text(encoding='utf-8'))]


def reel_time(tau):
    """reel time of master time tau (the first cut that shows it)"""
    for r0, r1, m0, v in CUTS:
        if m0 <= tau < m0 + (r1 - r0) * v:
            return r0 + (tau - m0) / v
    raise ValueError(tau)


def cut_of(tau):
    return next((r0, r1) for r0, r1, m0, v in CUTS if m0 <= tau < m0 + (r1 - r0) * v)


DROP = CUTS[1][0]                 # the body fills in: the pulse starts
WAKE = reel_time(14.15)           # the screen wakes
LOCK0, LOCK1 = cut_of(23.5)       # the PIN and the face: half-time
TURN0 = cut_of(57.3)[0]           # the phone turns: a riser
MARK = reel_time(61.65)           # the mark draws itself: a hit and a chord


def build():
    L = np.zeros(N); R = np.zeros(N)
    # chords per bar (2 s) from the drop: Am9, Fmaj7, Cmaj7, G6 (A minor, cool and open)
    CH = [[110.0, 164.81, 196.0, 246.94, 261.63], [87.31, 130.81, 164.81, 220.0], [130.81, 164.81, 196.0, 246.94], [98.0, 146.83, 164.81, 246.94]]
    ROOT = [55.0, 43.65, 65.41, 49.0]
    bar = 4 * BEAT

    # --- pad: an open fifth under the drawing, then one chord per bar from the drop; one low-passed track
    pad = np.zeros(N + int(SR))
    place_chord = lambda c, t0: place(pad, (lambda seg: seg * np.minimum(np.minimum(np.linspace(0, 1, len(seg)) * len(seg) / (0.3 * SR), 1), np.minimum((len(seg) - np.arange(len(seg))) / (0.3 * SR), 1)))(pad_chord(c, bar + 0.3)), t0)
    place_chord([110.0, 164.81, 220.0], 0.0)
    place_chord([110.0, 164.81, 220.0], bar - 0.0)
    b0 = 0
    t0 = DROP
    while t0 < MARK:
        place_chord(CH[b0 % 4], t0); b0 += 1; t0 += bar
    pad = pad[:N]
    pad_dark = lp(pad, 420, 2); pad_open = lp(pad, 1500, 2)
    openness = env_seg([(0, 0), (2.4, 0.2), (DROP + 0.4, 0.6), (LOCK0, 0.5), (LOCK1, 0.7), (TURN0, 0.7), (MARK - 0.2, 0.3), (MARK, 1.0), (MARK + 0.3, 0.6), (DUR, 0.6)])
    padlvl = env_seg([(0, 0), (1.0, 0.6), (DROP - 0.05, 0.75), (DROP + 0.1, 1.0), (TURN0, 0.9), (MARK - 0.1, 1.0), (MARK, 0.6), (MARK + 0.6, 1.0), (DUR - 1.6, 0.8), (DUR, 0)])
    pad = (pad_dark * (1 - openness) + pad_open * openness) * padlvl

    # --- the final chord: A major add9 on the mark, warm, long
    fin = pad_chord([110.0, 138.59, 164.81, 246.94, 277.18], DUR - MARK)
    tf = np.arange(len(fin)) / SR
    fin = lp(fin, 1300) * np.clip(tf / 0.6, 0, 1) * np.clip((DUR - MARK - tf) / 1.8, 0, 1)
    finb = np.zeros(N); place(finb, fin, MARK)
    pad = pad * env_seg([(0, 1), (MARK - 0.1, 1), (MARK + 0.6, 0.25), (DUR, 0.2)]) + 0.9 * finb

    # --- pulse: kick. four on the floor from the drop, half-time on the lock, off for the turn
    kk = kick(); kb = np.zeros(N)
    for i in range(int(DUR / BEAT) + 1):
        tb = i * BEAT
        if DROP <= tb < LOCK0: place(kb, kk, tb, 1.0 if i % 4 == 2 else 0.85)
        elif LOCK0 <= tb < LOCK1 and i % 2 == 0: place(kb, kk, tb, 0.8)
        elif LOCK1 <= tb < TURN0: place(kb, kk, tb, 1.0 if i % 4 == 1 else 0.85)
        elif TURN0 <= tb < MARK - 0.3 and i % 2 == 0: place(kb, kk, tb, 0.55 + 0.1 * ((tb - TURN0) / 2))
    # the drop and the mark: one deep hit each
    for (tm, gm, d) in ((DROP, 0.75, 1.1), (MARK, 1.1, 1.6)):
        th = np.arange(int(d * SR)) / SR
        boom = np.sin(2 * np.pi * np.cumsum(62 * np.exp(-th * 1.3) + 30) / SR) * np.exp(-th * 2.2) * np.clip(th / 0.004, 0, 1)
        place(kb, gm * np.tanh(1.8 * boom), tm)

    # --- bass: soft plucks on the eighths under the demo, following the chords
    bb = np.zeros(N)
    for i in range(int(DUR / (BEAT / 2)) + 1):
        tb = i * BEAT / 2
        if WAKE - 0.3 <= tb < TURN0 and not (LOCK0 <= tb < LOCK1):
            b = int((tb - DROP) // bar); f = ROOT[b % 4]
            v = 1.0 if i % 2 == 0 else 0.55
            if i % 8 == 5: f *= 2   # a small lift at the end of each bar
            place(bb, pluck(f), tb, v)
    bb *= env_seg([(0, 0), (WAKE - 0.3, 0), (WAKE + 0.5, 0.9), (TURN0 - 0.3, 1.0), (TURN0, 0), (DUR, 0)])

    # --- hats: off beats from the screen waking, the only pulse left in the lock
    hb = np.zeros(N)
    for i in range(int(DUR / (BEAT / 2)) + 1):
        tb = i * BEAT / 2
        off = i % 2 == 1
        if DROP <= tb < WAKE - 0.3 and off: place(hb, hat(0.45), tb)
        elif WAKE - 0.3 <= tb < TURN0: place(hb, hat(0.9 if off else 0.35), tb)
    hb *= env_seg([(0, 1), (TURN0 - 0.2, 1), (TURN0 + 0.3, 0.6), (MARK - 0.3, 0.6), (MARK - 0.1, 0), (DUR, 0)])

    # --- air: a breath under the drawing, a riser into the drop, a riser into the mark, a shimmer when the screen wakes
    air = np.zeros(N)
    nz = rng.standard_normal(N)
    breath = lp(hp(nz, 300), 1800) * env_seg([(0, 0), (0.4, 0.08), (2.0, 0.12), (DROP, 0.3), (DROP + 0.5, 0.04), (DUR, 0)])
    air += breath
    for (a, b, g) in [(1.4, DROP, 0.4), (TURN0, MARK, 0.55)]:
        n = int((b - a) * SR); tt = np.arange(n) / SR; u = tt / (b - a)
        x = np.zeros(n)
        for c in (400, 900, 1800, 3500, 6000):   # a noise band that climbs
            x += lp(hp(rng.standard_normal(n), c * 0.7), c * 1.4) * np.exp(-((u - np.log(c / 400) / np.log(15)) / 0.28) ** 2)
        x += 0.25 * np.sin(2 * np.pi * np.cumsum(110 * 4 ** u) / SR)
        place(air, g * x * u ** 1.6 * (1 - np.clip((u - 0.97) / 0.03, 0, 1)), a)
    n = int(1.8 * SR); tt = np.arange(n) / SR
    shim = sum(np.sin(2 * np.pi * f * tt) for f in (880.0, 1318.5, 1975.5)) * np.clip(tt / 0.25, 0, 1) * np.exp(-tt * 1.6) / 3
    place(air, 0.22 * shim, WAKE)

    # --- mix and a gentle "pump": the pad and bass duck a little on each kick
    duck = np.ones(N)
    for i in range(int(DUR / BEAT) + 1):
        tb = i * BEAT
        if DROP <= tb < TURN0 and not (LOCK0 <= tb < LOCK1 and i % 2):
            a = int(tb * SR); n = min(int(0.3 * SR), N - a)
            if n > 0: duck[a:a + n] = np.minimum(duck[a:a + n], 1 - 0.28 * np.exp(-np.arange(n) / SR * 11))
    mono = 0.26 * pad * duck + 0.55 * kb + 0.16 * bb * duck + 0.30 * hb + 0.12 * air
    side = 0.07 * lp(np.roll(pad, int(0.011 * SR)), 1500) + 0.08 * np.roll(hb, int(0.004 * SR))
    L = mono + side; R = mono - side
    st = np.stack([L, R], 1)
    st /= np.abs(st).max() / 0.7
    return st


def write(path, st):
    x = np.clip(st, -1, 1)
    with wave.open(str(path), 'wb') as w:
        w.setnchannels(2); w.setsampwidth(2); w.setframerate(SR); w.writeframes((x * 32767).astype('<i2').tobytes())


if __name__ == '__main__':
    out = Path(__file__).resolve().parent / 'bed.wav'
    st = build(); write(out, st)
    print(f'bed {out}: {DUR}s, {BPM:.0f} BPM, peak {20 * np.log10(np.abs(st).max()):.1f} dBFS')
