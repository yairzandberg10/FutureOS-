"""python3 spectro.py  -> out/spectrogram.png: a picture of out/audio.wav (time across, log frequency up) to check the structure without listening"""
import numpy as np
from PIL import Image
from scipy.io import wavfile
sr, x = wavfile.read("out/audio.wav"); x = x.astype(np.float32).mean(axis=1) / 32768
win, hop = 4096, 1024
w = np.hanning(win)
frames = (len(x) - win) // hop
S = np.abs(np.stack([np.fft.rfft(x[i * hop:i * hop + win] * w) for i in range(frames)], axis=1))
f = np.fft.rfftfreq(win, 1 / sr)
edges = np.geomspace(40, 16000, 260)
rows = np.stack([S[(f >= a) & (f < b)].mean(axis=0) if ((f >= a) & (f < b)).any() else np.zeros(frames) for a, b in zip(edges[:-1], edges[1:])])
img = 20 * np.log10(rows / rows.max() + 1e-9); img = np.clip((img + 78) / 66, 0, 1)
cols = np.array_split(np.arange(frames), 1500)
img = np.stack([img[:, c].max(axis=1) for c in cols], axis=1)[::-1]
rgb = np.stack([img ** 1.6, img ** 1.0 * 0.85, 0.35 + 0.65 * img ** 0.8], axis=-1)
im = Image.fromarray((rgb * 255).astype(np.uint8)).resize((1500, 520), Image.NEAREST)
# a marker every 4 s (2 bars)
for s in range(0, 61, 4):
    xx = int(s / 60 * 1499)
    im.paste((255, 255, 255), (xx, 0, xx + 1, 520 if s % 12 == 0 else 14))
im.save("out/spectrogram.png"); print("ok")
