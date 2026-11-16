#!/usr/bin/env python3
"""Re-encode the Scenes pictures for the APK (docs/BATCH_S_SCENES.md).

scenes/master/ holds the source pictures byte for byte as published by couch-tv (MIT, see NOTICE.md). This script writes
scenes/<id>/bg.webp, glass.webp and (for scenes that have one) cut.webp, and scenes/manifest.json with the SHA-256 of
every master, the size of every output and the first non-transparent row of every cut-out. Only the files under
scenes/<id>/ go into the APK (launcher/app/build.gradle).

    python scenes/to-scenes.py            convert everything and rewrite the manifest
    python scenes/to-scenes.py --check    verify masters, outputs and manifest; change nothing (exit 1 on a problem)

Settings are the constants below. Needs Python 3 and Pillow built with WebP support.
"""
import argparse
import hashlib
import json
import sys
from pathlib import Path

from PIL import Image, features

HERE = Path(__file__).resolve().parent
MASTER = HERE / "master"

# Scene id -> whether it has a cut-out. The order is the fixed order of the look (SceneList.java).
SCENES = {"night": True, "fog": True, "golden": True, "dawn": True, "water": False}

BG_QUALITY = 80      # background, WebP
CUT_QUALITY = 75     # cut-out, WebP with alpha
GLASS_QUALITY = 80   # blurred copy, WebP
GLASS_SIZE = (480, 270)
PICTURE_SIZE = (1920, 1080)
MANIFEST = HERE / "manifest.json"


def sha256(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def sources(scene):
    """The master files of a scene: {role: path}."""
    files = {"bg": MASTER / f"scene-{scene}.jpg", "glass": MASTER / f"scene-{scene}-glass.jpg"}
    if SCENES[scene]:
        files["cut"] = MASTER / f"scene-{scene}-fg.webp"
    return files


def first_opaque_row(path):
    """Top row of the first pixel that is not fully transparent, read back from the written file."""
    with Image.open(path) as im:
        alpha = im.convert("RGBA").split()[3]
        box = alpha.point(lambda v: 255 if v > 0 else 0).getbbox()
    return box[1] if box else PICTURE_SIZE[1]


def build():
    manifest = {"source": "couch-tv 31b8f26f445aa023ff1df3cda660740a30a8a8a5", "scenes": {}}
    total = 0
    for scene in SCENES:
        out_dir = HERE / scene
        out_dir.mkdir(exist_ok=True)
        entry = {"masters": {}, "files": {}}
        for role, src in sources(scene).items():
            entry["masters"][src.name] = sha256(src)
            dst = out_dir / f"{role}.webp"
            with Image.open(src) as im:
                if role == "bg":
                    im.convert("RGB").save(dst, "WEBP", quality=BG_QUALITY, method=6)
                elif role == "glass":
                    im.convert("RGB").resize(GLASS_SIZE, Image.LANCZOS).save(
                        dst, "WEBP", quality=GLASS_QUALITY, method=6)
                else:
                    im.convert("RGBA").save(dst, "WEBP", quality=CUT_QUALITY, method=6, alpha_quality=100)
            entry["files"][f"{role}.webp"] = dst.stat().st_size
            total += dst.stat().st_size
            print(f"{scene}/{dst.name}: {src.stat().st_size} -> {dst.stat().st_size} bytes")
        if SCENES[scene]:
            entry["cut_first_row"] = first_opaque_row(out_dir / "cut.webp")
        manifest["scenes"][scene] = entry
    manifest["total_bytes"] = total
    MANIFEST.write_bytes((json.dumps(manifest, indent=2) + "\n").encode("utf-8"))   # LF on every OS
    print(f"total {total} bytes; manifest written")
    return 0


def check():
    problems = []
    if not MANIFEST.is_file():
        print("scenes/manifest.json is missing", file=sys.stderr)
        return 1
    manifest = json.loads(MANIFEST.read_text(encoding="utf-8"))
    total = 0
    for scene in SCENES:
        entry = manifest.get("scenes", {}).get(scene)
        if entry is None:
            problems.append(f"{scene}: not in the manifest")
            continue
        for role, src in sources(scene).items():
            if not src.is_file():
                problems.append(f"{scene}: master {src.name} is missing")
            elif entry["masters"].get(src.name) != sha256(src):
                problems.append(f"{scene}: master {src.name} differs from the manifest SHA-256")
            dst = HERE / scene / f"{role}.webp"
            if not dst.is_file():
                problems.append(f"{scene}/{role}.webp is missing")
                continue
            total += dst.stat().st_size
            if entry["files"].get(dst.name) != dst.stat().st_size:
                problems.append(f"{scene}/{dst.name}: size differs from the manifest")
            with Image.open(dst) as im:
                want = GLASS_SIZE if role == "glass" else PICTURE_SIZE
                if im.size != want:
                    problems.append(f"{scene}/{dst.name}: size {im.size}, expected {want}")
                if role == "cut" and "A" not in im.convert("RGBA").getbands():
                    problems.append(f"{scene}/cut.webp has no alpha")
        if not SCENES[scene] and (HERE / scene / "cut.webp").exists():
            problems.append(f"{scene}: has a cut.webp but no cut-out is expected")
        if SCENES[scene]:
            row = first_opaque_row(HERE / scene / "cut.webp") if (HERE / scene / "cut.webp").is_file() else None
            if row != entry.get("cut_first_row"):
                problems.append(f"{scene}: first opaque row {row} differs from the manifest")
    if manifest.get("total_bytes") != total:
        problems.append(f"total {total} differs from the manifest {manifest.get('total_bytes')}")
    for p in problems:
        print(p, file=sys.stderr)
    if not problems:
        print(f"scenes OK, {total} bytes")
    return 1 if problems else 0


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--check", action="store_true", help="verify only, change nothing")
    args = ap.parse_args()
    if not features.check("webp"):
        print("This Pillow build has no WebP support.", file=sys.stderr)
        return 2
    return check() if args.check else build()


if __name__ == "__main__":
    sys.exit(main())
