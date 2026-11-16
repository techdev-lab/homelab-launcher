#!/usr/bin/env python3
"""Make the digits-only clock font of the Scenes look (docs/BATCH_S_SCENES.md).

The source is Antonio (SIL Open Font License 1.1) from the official google/fonts repository, file
ofl/antonio/Antonio[wght].ttf at the commit named in NOTICE.md. The script fixes the weight at 300 (Light) and keeps only
the digits 0 to 9 and the colon, so the font in the APK is a few KB. The output is launcher/app/src/main/res/font/antonio_digits.ttf.

    python scenes/make-clock-font.py path/to/Antonio[wght].ttf

Needs Python 3 and fontTools (pip install fonttools). It is not part of the Android build: the output is committed.
"""
import argparse
import hashlib
import sys
from pathlib import Path

from fontTools import subset
from fontTools.ttLib import TTFont
from fontTools.varLib import instancer

OUT = Path(__file__).resolve().parent.parent / "launcher" / "app" / "src" / "main" / "res" / "font" / "antonio_digits.ttf"
WEIGHT = 300
TEXT = "0123456789:"


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("source", type=Path, help="Antonio[wght].ttf from google/fonts")
    args = ap.parse_args()
    print("source sha256", hashlib.sha256(args.source.read_bytes()).hexdigest())
    font = TTFont(args.source)
    font = instancer.instantiateVariableFont(font, {"wght": WEIGHT})
    options = subset.Options()
    options.layout_features = []
    options.name_IDs = [0, 1, 2, 3, 4, 5, 6, 13, 14]   # keep the copyright and licence names
    options.notdef_outline = True
    options.hinting = False
    subsetter = subset.Subsetter(options)
    subsetter.populate(text=TEXT)
    subsetter.subset(font)
    font.save(OUT)
    print(f"{OUT.name}: {OUT.stat().st_size} bytes, sha256 {hashlib.sha256(OUT.read_bytes()).hexdigest()}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
