#!/usr/bin/env python3
"""Regenerate the bundled web emoji font subset.

The web client bundles a subset of Noto Color Emoji covering exactly the emoji the app
uses (suits, bot avatars, badges, ...) so they render from the first frame with no
tofu flash. When a NEW emoji is added to the app, rerun this script — until then the
new glyph falls back to Compose's on-demand Noto download (brief tofu flash, web only).

Usage:
    pip3 install --user fonttools   # once
    python3 docs/web/subset_emoji.py

Scans commonMain sources + strings.xml for emoji codepoints, downloads the full
Noto Color Emoji (cached in /tmp), and writes the subset to
composeApp/src/wasmJsMain/composeResources/font/noto_color_emoji.ttf.
"""

import pathlib
import subprocess
import sys
import urllib.request

REPO = pathlib.Path(__file__).resolve().parents[2]
SCAN_ROOTS = [REPO / "composeApp/src/commonMain", REPO / "shared/src/commonMain"]
OUTPUT = REPO / "composeApp/src/wasmJsMain/composeResources/font/noto_color_emoji.ttf"
FULL_FONT_URL = "https://github.com/googlefonts/noto-emoji/raw/main/fonts/NotoColorEmoji.ttf"
FULL_FONT_CACHE = pathlib.Path("/tmp/NotoColorEmoji-full.ttf")

# Variation selector + ZWJ are always included so emoji-presentation sequences resolve.
ALWAYS = {0xFE0F, 0x200D, 0x20E3}


def collect_codepoints():
    chars = set(ALWAYS)
    for root in SCAN_ROOTS:
        for f in list(root.rglob("*.kt")) + list(root.rglob("*.xml")):
            for ch in f.read_text(encoding="utf-8"):
                if ord(ch) >= 0x2190:
                    chars.add(ord(ch))
    return sorted(chars)


def main():
    codepoints = collect_codepoints()
    print(f"{len(codepoints)} codepoints: " + "".join(chr(c) for c in codepoints if c >= 0x2600))

    if not FULL_FONT_CACHE.exists():
        print(f"downloading full Noto Color Emoji (~11 MB) to {FULL_FONT_CACHE} ...")
        urllib.request.urlretrieve(FULL_FONT_URL, FULL_FONT_CACHE)

    unicodes = ",".join(f"U+{c:04X}" for c in codepoints)
    subprocess.run(
        [
            sys.executable,
            "-m",
            "fontTools.subset",
            str(FULL_FONT_CACHE),
            f"--unicodes={unicodes}",
            f"--output-file={OUTPUT}",
        ],
        check=True,
    )
    print(f"wrote {OUTPUT} ({OUTPUT.stat().st_size:,} bytes)")


if __name__ == "__main__":
    main()
