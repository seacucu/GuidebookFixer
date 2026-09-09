#!/usr/bin/env python3
"""Draws the mod's icon: a correction tape.

The metaphor is the whole mod in one object. A guidebook says something that
stopped being true when a modpack changed the recipes underneath it, and this
mod paints over that line and writes the truth on top.

The art lives here as a character map rather than as an opaque PNG so it can be
read and edited in a diff. Run this to regenerate both sizes:

    python art/icon.py

    src/main/resources/assets/guidebookfixer/textures/gui/correction_tape.png
        16x16, drawn in the title bar of a recipe box this mod filled in
    src/main/resources/logo.png
        128x128, the mod list entry
"""

import os

from PIL import Image

#  .  transparent      o  outline        b  body
#  h  body highlight   w  tape / spools  *  sparkle
ART = """
................
..oooooooo......
.obbbbbbbbo...*.
.obhwwbbwwbo....
.obhwwbbwwbo....
.obhwwbbwwbo....
.obbbbbbbbbo....
.obbbbbbbbbo....
..obbbbbbbo.....
...obbbbbo......
....obbbo.......
.....obo........
.....owwwwwwwwwo
.....owwwwwwwwwo
.....ooooooooooo
................
"""

PALETTE = {
    "o": (59, 36, 85, 255),      # outline, dark violet
    "b": (122, 79, 166, 255),    # body
    "h": (169, 139, 208, 255),   # highlight down the left edge
    "w": (242, 239, 247, 255),   # the tape itself, and the spool windows
    "*": (255, 233, 163, 255),   # a spark, so it reads as more than stationery
    ".": (0, 0, 0, 0),
}

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ICON = os.path.join(ROOT, "src", "main", "resources", "assets", "guidebookfixer",
                    "textures", "gui", "correction_tape.png")
LOGO = os.path.join(ROOT, "src", "main", "resources", "logo.png")


def main():
    rows = [r for r in ART.strip("\n").split("\n")]
    assert len(rows) == 16, f"要 16 列，有 {len(rows)}"
    for i, r in enumerate(rows):
        assert len(r) == 16, f"第 {i} 列要 16 格，有 {len(r)}"

    img = Image.new("RGBA", (16, 16))
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            img.putpixel((x, y), PALETTE[ch])

    os.makedirs(os.path.dirname(ICON), exist_ok=True)
    img.save(ICON)
    # 最近鄰放大，保住像素邊緣
    img.resize((128, 128), Image.NEAREST).save(LOGO)
    print(f"{os.path.relpath(ICON, ROOT)}  16x16")
    print(f"{os.path.relpath(LOGO, ROOT)}  128x128")
    print()
    for row in rows:
        print("  " + row.replace(".", " ").replace("o", "#").replace("b", "@")
                       .replace("h", "%").replace("w", "="))


if __name__ == "__main__":
    main()
