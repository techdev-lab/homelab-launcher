"""Repository hygiene checks that run in CI and locally: python3 scripts/ci/check_hygiene.py

1. Arabic text is allowed only where it belongs: the parallel *.ar.md docs, the bilingual README.md,
   localization data (res/values-ar, assets/names_ar.json, assets/clubs.json, the language
   names in res/values/language_names.xml), the Arabic store listing (fastlane/metadata/android/ar/),
   the issue and pull request templates (they are shown to the people who write to the project, so
   they carry both languages) and the city names in config/. An English doc may also carry the link to its Arabic twin in its
   first lines (the language switch), so a reader who cannot read English can find the Arabic one.
   Code, comments, scripts, English docs and CI files must be English.
2. Files that must never be tracked: Gradle local.properties, signing keys, ADB keys, .env files.
"""
import os
import subprocess
import sys

# Unicode blocks: Arabic, Arabic Supplement, Arabic Presentation Forms-A and -B (as code points, so this
# file itself contains no Arabic characters).
ARABIC_RANGES = ((0x0600, 0x06FF), (0x0750, 0x077F), (0xFB50, 0xFDFF), (0xFE70, 0xFEFF))

ARABIC_ALLOWED_PREFIXES = ("launcher/app/src/main/res/values-ar/", "config/", "fastlane/metadata/android/ar/",
                           ".github/ISSUE_TEMPLATE/")
# The language switch of an English doc: a link to its Arabic twin within the first lines.
SWITCH_LINE_LIMIT = 5
ARABIC_ALLOWED_FILES = {"README.md", ".github/PULL_REQUEST_TEMPLATE.md", "launcher/app/src/main/assets/names_ar.json",
                        "launcher/app/src/main/assets/clubs.json",
                        "launcher/app/src/main/res/values/language_names.xml"}

FORBIDDEN_NAMES = {"local.properties", "adbkey", "adbkey.pub", ".env"}
FORBIDDEN_SUFFIXES = (".keystore", ".jks")


def has_arabic(line):
    return any(lo <= ord(ch) <= hi for ch in line for lo, hi in ARABIC_RANGES)


def arabic_violations(path, text):
    """Return the line numbers in `text` that hold Arabic where `path` may not have it."""
    if path.endswith(".ar.md") or path in ARABIC_ALLOWED_FILES or path.startswith(ARABIC_ALLOWED_PREFIXES):
        return []
    return [number for number, line in enumerate(text.splitlines(), 1)
            if has_arabic(line) and not (number <= SWITCH_LINE_LIMIT and ".ar.md)" in line)]


def is_forbidden(path):
    name = os.path.basename(path)
    return name in FORBIDDEN_NAMES or name.endswith(FORBIDDEN_SUFFIXES)


def tracked_files():
    out = subprocess.run(["git", "ls-files", "-z"], check=True, capture_output=True).stdout
    return [p for p in out.decode("utf-8").split("\0") if p]


def main():
    problems = []
    for path in tracked_files():
        if is_forbidden(path):
            problems.append(f"{path}: this file must not be tracked")
        try:
            with open(path, encoding="utf-8") as handle:
                text = handle.read()
        except UnicodeDecodeError:
            continue  # binary file (image, font, jar): nothing to scan
        for number in arabic_violations(path, text):
            problems.append(f"{path}:{number}: Arabic text outside the allowed places")
    for problem in problems:
        print(problem)
    if problems:
        print(f"\n{len(problems)} problem(s) found.")
        return 1
    print("Hygiene checks passed.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
