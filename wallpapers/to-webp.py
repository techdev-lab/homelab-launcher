#!/usr/bin/env python3
"""Re-encode the bundled wallpapers from JPEG to lossy WebP (docs/BATCH_L_LIGHT.md).

The JPEG files in homelab/, kids/ and serene/ are the masters. This script writes one .webp beside each of them, with the
same name and the same pixel size, and only the .webp files go into the APK (launcher/app/build.gradle). Run it again
after changing a picture or a quality.

    python wallpapers/to-webp.py                      convert every folder with the qualities below
    python wallpapers/to-webp.py --quality serene=90  override one folder for this run
    python wallpapers/to-webp.py --check              verify, change nothing (exit 1 on a problem)

To raise the quality of one folder for good, change its number in QUALITY and run the script again.
Needs Python 3 and Pillow built with WebP support (the same Pillow as gen-serene.py).
"""
import argparse
import sys
from pathlib import Path

from PIL import Image, features

HERE = Path(__file__).resolve().parent

# WebP quality per folder, 0 to 100. 80 is the starting point; raise a folder that shows banding on a TV.
QUALITY = {"homelab": 80, "kids": 80, "serene": 80}


def masters(folder):
    return sorted((HERE / folder).glob("*.jpg"))


def convert(folder, quality):
    total_in = total_out = 0
    for src in masters(folder):
        dst = src.with_suffix(".webp")
        with Image.open(src) as im:
            im = im.convert("RGB")
            im.save(dst, "WEBP", quality=quality, method=6)
        total_in += src.stat().st_size
        total_out += dst.stat().st_size
        print(f"{folder}/{dst.name}: {src.stat().st_size} -> {dst.stat().st_size} bytes")
    return total_in, total_out


def check(folder):
    """Problems for one folder: a master with no .webp, a different pixel size, a .webp with no master."""
    problems = []
    names = {p.stem for p in masters(folder)}
    if not names:
        problems.append(f"{folder}: no .jpg masters")
    for stem in sorted(names):
        jpg, webp = HERE / folder / f"{stem}.jpg", HERE / folder / f"{stem}.webp"
        if not webp.is_file():
            problems.append(f"{folder}/{stem}: no .webp")
            continue
        with Image.open(jpg) as a, Image.open(webp) as b:
            if a.size != b.size:
                problems.append(f"{folder}/{stem}: size {a.size} (jpg) vs {b.size} (webp)")
    for webp in sorted((HERE / folder).glob("*.webp")):
        if webp.stem not in names:
            problems.append(f"{folder}/{webp.name}: no .jpg master")
    return problems


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--check", action="store_true", help="verify only, change nothing")
    ap.add_argument("--quality", action="append", default=[], metavar="FOLDER=Q", help="override one folder")
    args = ap.parse_args()
    if not features.check("webp"):
        print("This Pillow build has no WebP support.", file=sys.stderr)
        return 2
    quality = dict(QUALITY)
    for item in args.quality:
        folder, _, q = item.partition("=")
        if folder not in quality or not q.isdigit() or not 0 <= int(q) <= 100:
            print(f"bad --quality {item!r}; use FOLDER=0..100 with a folder of {sorted(quality)}", file=sys.stderr)
            return 2
        quality[folder] = int(q)
    if args.check:
        problems = [p for folder in quality for p in check(folder)]
        for p in problems:
            print(p)
        print("ok" if not problems else f"{len(problems)} problem(s)")
        return 1 if problems else 0
    tin = tout = 0
    for folder, q in quality.items():
        a, b = convert(folder, q)
        tin, tout = tin + a, tout + b
    print(f"total: {tin} -> {tout} bytes")
    problems = [p for folder in quality for p in check(folder)]
    for p in problems:
        print(p)
    return 1 if problems else 0


if __name__ == "__main__":
    sys.exit(main())
