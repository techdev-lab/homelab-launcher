"""General wallpapers: ten calm, neutral 1920x1080 scenes (no text, no people, nothing household).

For a TV that is not yours: a parent's living room, a guest room, a waiting room. Every scene is drawn from
code, so there is no photo licence to worry about and the set can be rebuilt byte for byte.

Needs Pillow and numpy. Usage: python gen-general.py <output-dir>
Scenes 01-06 are the first set (general-v1); 07-10 were added in general-v2. Seeds are fixed per scene, so
adding scenes never changes the earlier ones.
"""
import sys
from pathlib import Path

import numpy as np
from PIL import Image, ImageFilter

W, H = 1920, 1080


def lerp(a, b, t):
    return a + (b - a) * t


def vgrad(stops):
    """Vertical gradient from [(pos, (r, g, b)), ...] to an HxWx3 float array."""
    y = np.linspace(0, 1, H)[:, None]
    out = np.zeros((H, W, 3))
    for c in range(3):
        out[:, :, c] = np.interp(y, [s[0] for s in stops], [s[1][c] for s in stops])
    return out


def glow(img, cx, cy, radius, color, strength):
    yy, xx = np.mgrid[0:H, 0:W]
    d = np.sqrt((xx - cx) ** 2 + (yy - cy) ** 2) / radius
    a = np.clip(1 - d, 0, 1) ** 2 * strength
    for c in range(3):
        img[:, :, c] = lerp(img[:, :, c], color[c], a)
    return img


def ridge(rng, base, amp, seed_phase):
    x = np.linspace(0, 1, W)
    y = np.zeros(W)
    for k, f in enumerate((1.3, 2.9, 6.1, 12.7)):
        y += np.sin(x * f * np.pi * 2 + rng.uniform(0, 6.28) + seed_phase) * amp / (k + 1.4)
    return base + y


def fill_below(img, curve, color, fog_to=None, fog=0.0):
    yy = np.arange(H)[:, None]
    mask = (yy >= curve[None, :]).astype(float)
    soft = np.clip((yy - curve[None, :]) / 3.0, 0, 1)
    mask = np.minimum(mask, soft)
    col = np.array(color, dtype=float)
    layer = np.zeros_like(img) + col
    if fog_to is not None:
        t = np.clip((yy - curve[None, :]) / 380.0, 0, 1) * fog
        layer = lerp(layer, np.array(fog_to, dtype=float), t[:, :, None])
    return img * (1 - mask[:, :, None]) + layer * mask[:, :, None]


def finish(img, rng, vig=0.5, grain=2.2):
    yy, xx = np.mgrid[0:H, 0:W]
    d = np.sqrt(((xx - W / 2) / (W / 2)) ** 2 + ((yy - H / 2) / (H / 2)) ** 2)
    img = img * (1 - np.clip(d - 0.55, 0, 1)[:, :, None] * vig)
    img = img + rng.normal(0, grain, (H, W, 1))
    return Image.fromarray(np.clip(img, 0, 255).astype("uint8"))


def dusk(rng):
    img = vgrad([(0, (10, 20, 36)), (0.55, (34, 62, 88)), (0.82, (88, 118, 134)), (1, (30, 46, 60))])
    img = glow(img, W * 0.5, H * 0.78, 760, (150, 170, 176), 0.28)
    return finish(img, rng)


def ridges(rng):
    img = vgrad([(0, (14, 26, 42)), (0.6, (58, 84, 106)), (1, (96, 120, 136))])
    img = glow(img, W * 0.68, H * 0.38, 620, (180, 196, 204), 0.18)
    palette = [(78, 102, 122), (60, 84, 104), (44, 66, 86), (30, 48, 66), (20, 34, 48)]
    fog = (96, 120, 136)
    for i, col in enumerate(palette):
        curve = ridge(rng, H * (0.50 + i * 0.09), 38 - i * 4, i)
        img = fill_below(img, curve, col, fog_to=fog, fog=0.55 - i * 0.1)
    return finish(img, rng)


def water(rng):
    horizon = int(H * 0.58)
    img = np.zeros((H, W, 3))
    img[:horizon] = vgrad([(0, (8, 16, 30)), (1, (62, 88, 108))])[:horizon]
    img[horizon:] = vgrad([(0, (46, 70, 90)), (1, (8, 16, 26))])[horizon:]
    img = glow(img, W * 0.5, horizon - 40, 520, (200, 212, 214), 0.30)
    yy = np.arange(H)[:, None]
    ripple = np.sin(yy * 0.35 + rng.uniform(0, 6)) * np.sin(np.linspace(0, 40, W)[None, :] + yy * 0.02)
    band = np.clip((yy - horizon) / 300, 0, 1) * np.clip(1 - (yy - horizon) / 440, 0, 1)
    img += (ripple * band * 9)[:, :, None]
    return finish(img, rng, vig=0.55)


def dunes(rng):
    img = vgrad([(0, (28, 34, 52)), (0.45, (92, 88, 100)), (0.7, (178, 140, 120)), (1, (120, 90, 80))])
    img = glow(img, W * 0.3, H * 0.62, 640, (230, 190, 160), 0.22)
    palette = [(150, 112, 96), (118, 86, 78), (86, 62, 62), (56, 42, 48)]
    for i, col in enumerate(palette):
        curve = ridge(rng, H * (0.62 + i * 0.1), 30 - i * 4, 10 + i)
        img = fill_below(img, curve, col, fog_to=(178, 140, 120), fog=0.35 - i * 0.07)
    return finish(img, rng)


def ribbons(rng):
    base = vgrad([(0, (4, 8, 18)), (1, (10, 20, 34))])
    layer = np.zeros((H, W, 3))
    x = np.linspace(0, 1, W)
    yy = np.arange(H)[:, None]
    for k, col in enumerate([(40, 150, 130), (50, 110, 170), (70, 150, 150)]):
        centre = H * (0.30 + k * 0.1) + np.sin(x * (2 + k) * np.pi + rng.uniform(0, 6)) * 90
        width = 70 + k * 25
        a = np.exp(-(((yy - centre[None, :]) / width) ** 2)) * (0.55 - k * 0.1)
        for c in range(3):
            layer[:, :, c] += a * col[c]
    img = base + layer
    pil = Image.fromarray(np.clip(img, 0, 255).astype("uint8")).filter(ImageFilter.GaussianBlur(26))
    return finish(np.asarray(pil).astype(float), rng, vig=0.6)


def circles(rng):
    img = vgrad([(0, (24, 26, 32)), (1, (12, 14, 18))])
    yy, xx = np.mgrid[0:H, 0:W]
    for cx, cy, r, col, s in [(W * 0.25, H * 0.7, 520, (70, 84, 104), 0.35), (W * 0.72, H * 0.35, 420, (84, 74, 98), 0.28),
                             (W * 0.55, H * 0.85, 300, (60, 96, 100), 0.22)]:
        d = np.sqrt((xx - cx) ** 2 + (yy - cy) ** 2)
        a = np.clip(1 - np.abs(d - r) / 90, 0, 1) ** 2 * s + np.clip((r * 0.9 - d) / r, 0, 1) * 0.05
        for c in range(3):
            img[:, :, c] = lerp(img[:, :, c], col[c], np.clip(a, 0, 1))
    pil = Image.fromarray(np.clip(img, 0, 255).astype("uint8")).filter(ImageFilter.GaussianBlur(10))
    return finish(np.asarray(pil).astype(float), rng, vig=0.5)


def stars(rng):
    """A clear night over low dunes: a few hundred faint stars, brighter towards the top."""
    img = vgrad([(0, (4, 8, 20)), (0.62, (18, 28, 50)), (1, (30, 34, 48))])
    img = glow(img, W * 0.78, H * 0.70, 700, (60, 70, 96), 0.25)
    n = 420
    xs = rng.uniform(0, W, n).astype(int)
    ys = (rng.beta(1.2, 2.6, n) * H * 0.7).astype(int)
    for x, y, b in zip(xs, ys, rng.uniform(70, 200, n)):
        r = 1 if b < 170 else 2
        img[max(0, y - r):y + r, max(0, x - r):x + r] += b * (1 - y / (H * 0.8))
    pil = Image.fromarray(np.clip(img, 0, 255).astype("uint8")).filter(ImageFilter.GaussianBlur(0.8))
    img = np.asarray(pil).astype(float)
    for i, col in enumerate([(26, 28, 40), (16, 18, 28)]):
        curve = ridge(rng, H * (0.80 + i * 0.08), 22 - i * 6, 20 + i)
        img = fill_below(img, curve, col)
    return finish(img, rng, vig=0.55, grain=1.6)


def aurora(rng):
    """Soft green and teal curtains, heavily blurred so they read as light, not shapes."""
    base = vgrad([(0, (4, 10, 20)), (1, (8, 18, 28))])
    layer = np.zeros((H, W, 3))
    x = np.linspace(0, 1, W)
    yy = np.arange(H)[:, None]
    for k, col in enumerate([(60, 170, 120), (40, 140, 150), (90, 120, 170)]):
        top = H * (0.18 + k * 0.08) + np.sin(x * (1.5 + k * 0.7) * np.pi + rng.uniform(0, 6)) * 70
        height = 260 - k * 40
        inside = np.clip((yy - top[None, :]) / height, 0, 1)
        a = inside * np.clip(1 - inside, 0, 1) * 4 * (0.42 - k * 0.1)
        a *= (0.75 + 0.25 * np.sin(x * 7 + k * 2)[None, :])
        for c in range(3):
            layer[:, :, c] += a * col[c]
    img = base + layer
    pil = Image.fromarray(np.clip(img, 0, 255).astype("uint8")).filter(ImageFilter.GaussianBlur(22))
    img = np.asarray(pil).astype(float)
    img = fill_below(img, ridge(rng, H * 0.86, 18, 30), (6, 12, 18))
    return finish(img, rng, vig=0.6)


def dawn(rng):
    """Sea at first light: a warm band on the horizon fading into a cool sky and dark water."""
    horizon = int(H * 0.62)
    img = np.zeros((H, W, 3))
    img[:horizon] = vgrad([(0, (22, 30, 52)), (0.7, (120, 110, 128)), (1, (214, 160, 126))])[:horizon]
    img[horizon:] = vgrad([(0, (96, 84, 92)), (1, (14, 20, 30))])[horizon:]
    img = glow(img, W * 0.42, horizon, 460, (244, 196, 150), 0.32)
    yy = np.arange(H)[:, None]
    shimmer = np.sin(yy * 0.6 + rng.uniform(0, 6)) * np.sin(np.linspace(0, 60, W)[None, :] + yy * 0.05)
    xx = np.arange(W)[None, :]
    path = np.exp(-(((xx - W * 0.42) / 220) ** 2)) * np.clip((yy - horizon) / 40, 0, 1) * np.clip(
        1 - (yy - horizon) / 400, 0, 1)
    img += (shimmer * path * 22)[:, :, None]
    return finish(img, rng, vig=0.5)


def geometry(rng):
    """A faint eight-point star lattice on deep teal: pattern you notice only when you look for it."""
    img = vgrad([(0, (12, 26, 32)), (1, (6, 14, 18))])
    yy, xx = np.mgrid[0:H, 0:W].astype(float)
    cell = 180.0
    u = (xx % cell) / cell - 0.5
    v = (yy % cell) / cell - 0.5
    square = np.maximum(np.abs(u), np.abs(v))
    diamond = (np.abs(u) + np.abs(v)) / 1.4142
    star = np.minimum(square, diamond)
    line = np.clip(1 - np.abs(star - 0.30) / 0.012, 0, 1)
    fade = np.clip(1 - np.sqrt(((xx - W * 0.5) / W) ** 2 + ((yy - H * 0.45) / H) ** 2) * 1.6, 0, 1)
    for c, col in enumerate((120, 170, 160)):
        img[:, :, c] = lerp(img[:, :, c], col, line * fade * 0.16)
    img = glow(img, W * 0.5, H * 0.45, 820, (40, 70, 76), 0.20)
    pil = Image.fromarray(np.clip(img, 0, 255).astype("uint8")).filter(ImageFilter.GaussianBlur(1.2))
    return finish(np.asarray(pil).astype(float), rng, vig=0.55)


SCENES = [("01-dusk-glow", dusk), ("02-misty-ridges", ridges), ("03-still-water", water),
          ("04-soft-dunes", dunes), ("05-quiet-ribbons", ribbons), ("06-soft-circles", circles),
          ("07-night-stars", stars), ("08-aurora-veil", aurora), ("09-sea-dawn", dawn),
          ("10-quiet-geometry", geometry)]


def main():
    out = Path(sys.argv[1])
    out.mkdir(parents=True, exist_ok=True)
    for i, (name, fn) in enumerate(SCENES):
        rng = np.random.default_rng(7000 + i)
        fn(rng).save(out / f"{name}.jpg", quality=88, optimize=True)
        print(name)


if __name__ == "__main__":
    main()
