#!/usr/bin/env python3
"""Generate the placeholder textures for the Columbia chapter.

Everything this writes is a stand-in. The mobs and the three dungeons exist so the
chapter can be walked end to end; none of them are drawn yet. Rather than ship a pile
of missing-texture magenta, this paints each one a flat identifying colour with a
little grain and a few accent bands, so that in-game you can tell a shadow from a drone
at a glance and still be in no doubt that neither is finished.

    python tools/generate_placeholder_art.py

It is idempotent and seeded, so re-running it produces byte-identical files. Delete a
file and re-run to get it back; overwrite one with real art and *remove its entry here*
so a later run cannot clobber it.

Pure standard library on purpose — the repo's other tools need Pillow, this one is run
rarely enough that not adding a dependency is worth more than the convenience.
"""

import os
import random
import struct
import zlib

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "priestess", "textures")


def write_png(path, width, height, pixels):
    """pixels: flat bytearray of RGBA, width*height*4."""
    raw = bytearray()
    for y in range(height):
        raw.append(0)  # filter type 0 (None) — these are tiny, filtering buys nothing
        raw.extend(pixels[y * width * 4:(y + 1) * width * 4])

    def chunk(tag, data):
        return (struct.pack(">I", len(data)) + tag + data
                + struct.pack(">I", zlib.crc32(tag + data) & 0xFFFFFFFF))

    blob = b"\x89PNG\r\n\x1a\n"
    blob += chunk(b"IHDR", struct.pack(">IIBBBBB", width, height, 8, 6, 0, 0, 0))
    blob += chunk(b"IDAT", zlib.compress(bytes(raw), 9))
    blob += chunk(b"IEND", b"")

    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "wb") as handle:
        handle.write(blob)
    print("  %s  (%dx%d)" % (os.path.relpath(path, ROOT).replace("\\", "/"), width, height))


def shade(colour, factor):
    return tuple(max(0, min(255, int(c * factor))) for c in colour)


def noisy_field(width, height, base, accent, seed, grain=14, stripe=0):
    """A flat colour with per-pixel grain, optionally banded with an accent colour.

    The grain is what stops a placeholder reading as a solid untextured block at
    distance; the bands give each mob one asymmetric feature so you can see which way
    it is facing while it is being tuned.
    """
    rng = random.Random(seed)
    pixels = bytearray(width * height * 4)
    for y in range(height):
        for x in range(width):
            colour = base
            if stripe and (y // stripe) % 3 == 1:
                colour = accent
            jitter = rng.randint(-grain, grain)
            r, g, b = shade(colour, 1.0 + jitter / 255.0)
            i = (y * width + x) * 4
            pixels[i] = r
            pixels[i + 1] = g
            pixels[i + 2] = b
            pixels[i + 3] = 255
    return pixels


def entity_texture(name, width, height, base, accent, seed, stripe=0):
    pixels = noisy_field(width, height, base, accent, seed, stripe=stripe)
    write_png(os.path.join(ASSETS, "entity", "%s.png" % name), width, height, pixels)


def block_texture(name, base, accent, seed, frame=True):
    """A 16x16 block tile: grainy `base`, optionally with an `accent` border and centre mark.

    The frame is what separates an armed altar from a spent one at a glance — the spent
    variant is the same colours with the frame dropped and the whole tile darkened, so the
    pair reads as one block in two states rather than as two blocks.
    """
    width = height = 16
    rng = random.Random(seed)
    pixels = bytearray(width * height * 4)
    for y in range(height):
        for x in range(width):
            edge = x == 0 or y == 0 or x == width - 1 or y == height - 1
            centre = 6 <= x <= 9 and 6 <= y <= 9
            colour = accent if (frame and (edge or centre)) else base
            jitter = rng.randint(-14, 14)
            r, g, b = shade(colour, 1.0 + jitter / 255.0)
            i = (y * width + x) * 4
            pixels[i] = r
            pixels[i + 1] = g
            pixels[i + 2] = b
            pixels[i + 3] = 255
    write_png(os.path.join(ASSETS, "block", "%s.png" % name), width, height, pixels)


def item_texture(name, base, accent, seed, glyph):
    """A 16x16 item: a rounded slab of `base` with `glyph` stamped on in `accent`.

    glyph is a list of 16 strings of 16 characters; '#' is accent, '.' is base, ' ' is
    transparent. It is easier to read as ASCII than as coordinates, and these are only
    ever going to be eyeballed.
    """
    width = height = 16
    rng = random.Random(seed)
    pixels = bytearray(width * height * 4)
    for y in range(height):
        row = glyph[y]
        for x in range(width):
            cell = row[x] if x < len(row) else " "
            i = (y * width + x) * 4
            if cell == " ":
                continue
            colour = accent if cell == "#" else base
            jitter = rng.randint(-10, 10)
            r, g, b = shade(colour, 1.0 + jitter / 255.0)
            pixels[i] = r
            pixels[i + 1] = g
            pixels[i + 2] = b
            pixels[i + 3] = 255
    write_png(os.path.join(ASSETS, "item", "%s.png" % name), width, height, pixels)


# ── The roster ────────────────────────────────────────────────────────────────
# Colours are chosen so the three dungeons read as three palettes: Mansfield is
# spectral violet, Dorothy's Vision is diseased pink-green, Rhine Lab is clean white
# and blue. The slug is Originium cyan against wasteland grey.

ENTITIES = [
    # name,                    w,   h,  base colour, accent colour, seed, stripe
    ("originium_slug",         64,  32, (0x4A, 0x5A, 0x66), (0x5F, 0xC8, 0xE8), 101, 6),
    ("jesselton_williams",     64,  64, (0x3A, 0x30, 0x4E), (0xB8, 0x26, 0x2E), 103, 8),
    # The three Medium-bearers, one palette per rung so they read as a set at a glance:
    # Failure is going-off green, Replica is clean bone and cold blue, Bionic is gunmetal
    # and hazard orange. These are box-UV placeholders for the geo models in
    # geo/entity/{failure,replica,bionic}.geo.json — a real Blockbench export replaces both
    # the .geo.json and the texture together, and the entry here goes with them.
    ("failure",                64,  64, (0x3A, 0x4A, 0x3E), (0xA8, 0xC0, 0x60), 105, 7),
    ("replica",                64,  64, (0xC8, 0xC4, 0xBC), (0x5F, 0x8C, 0xE8), 106, 9),
    ("bionic",                 64,  64, (0x3E, 0x46, 0x52), (0xE8, 0x7A, 0x2E), 107, 5),
    # NOT "awaken". Its texture is a real 128x128 export that belongs to the Blockbench
    # model in geo/entity/awaken.geo.json, and the UVs only line up with that file. This
    # script overwrites by name, so putting it back here would destroy hand-made art the
    # next time anyone regenerates placeholders.
    #
    # "frank" and "failed_vision" were removed with their entities.
]

KEY = [
    "                ",
    "      ####      ",
    "     #....#     ",
    "     #.##.#     ",
    "     #....#     ",
    "      ####      ",
    "       ##       ",
    "       ##       ",
    "       ##       ",
    "       ###      ",
    "       ##       ",
    "       ###      ",
    "       ##       ",
    "       ##       ",
    "                ",
    "                ",
]

PROCESSOR = [
    "                ",
    "   #  #  #  #   ",
    "  ############  ",
    "  #..........#  ",
    " ##.########.## ",
    "  #.#......#.#  ",
    "  #.#.####.#.#  ",
    "  #.#.#..#.#.#  ",
    "  #.#.####.#.#  ",
    "  #.#......#.#  ",
    " ##.########.## ",
    "  #..........#  ",
    "  ############  ",
    "   #  #  #  #   ",
    "                ",
    "                ",
]

BLUEPRINT = [
    "                ",
    " ############## ",
    " #............# ",
    " #.##########.# ",
    " #.#........#.# ",
    " #.#.######.#.# ",
    " #.#.#....#.#.# ",
    " #.#.#.##.#.#.# ",
    " #.#.#....#.#.# ",
    " #.#.######.#.# ",
    " #.#........#.# ",
    " #.##########.# ",
    " #............# ",
    " ############## ",
    "                ",
    "                ",
]

DOG_TAGS = [
    "                ",
    "         ##     ",
    "        #..#    ",
    "       #..#     ",
    "      #..#      ",
    "     #..#       ",
    "    ####        ",
    "   #....#       ",
    "   #.##.#       ",
    "   #....#       ",
    "  #....#        ",
    "  #.##.#        ",
    "  #....#        ",
    "   ####         ",
    "                ",
    "                ",
]

SHARD = [
    "                ",
    "       ##       ",
    "      #..#      ",
    "     #....#     ",
    "     #.##.#     ",
    "    #..##..#    ",
    "    #.#..#.#    ",
    "   #..#..#..#   ",
    "   #.#....#.#   ",
    "    #..##..#    ",
    "    #.####.#    ",
    "     #....#     ",
    "      #..#      ",
    "       ##       ",
    "                ",
    "                ",
]

MEDIUM = [
    "                ",
    "       ##       ",
    "      #..#      ",
    "     #.##.#     ",
    "    #.#..#.#    ",
    "   #.#.##.#.#   ",
    "   #.#.##.#.#   ",
    "   #.#.##.#.#   ",
    "   #.#.##.#.#   ",
    "   #.#.##.#.#   ",
    "    #.#..#.#    ",
    "     #.##.#     ",
    "      #..#      ",
    "       ##       ",
    "                ",
    "                ",
]

DREAMLAND = [
    "                ",
    "   #        #   ",
    "  #.#  ##  #.#  ",
    "   #  #..#  #   ",
    "      #..#      ",
    "   ####..####   ",
    "  #..........#  ",
    " #............# ",
    " #............# ",
    "  #..........#  ",
    "   #........#   ",
    "    #......#    ",
    "     #....#     ",
    "      #..#      ",
    "       ##       ",
    "                ",
]

ITEMS = [
    # name,                            base,               accent,              seed, glyph
    ("mansfield_master_key",           (0x4A, 0x42, 0x38), (0xD8, 0xC0, 0x78), 201, KEY),
    ("dorothys_neural_processor",      (0x1E, 0x30, 0x2A), (0x5F, 0xE8, 0xA8), 202, PROCESSOR),
    ("blueprint_originium_refinement", (0x14, 0x2E, 0x54), (0x8C, 0xC8, 0xF0), 203, BLUEPRINT),
    ("tarnished_dog_tags",             (0x3A, 0x38, 0x30), (0x9A, 0x8E, 0x6A), 204, DOG_TAGS),
    ("corrupted_neural_shard",         (0x24, 0x18, 0x2E), (0xC8, 0x5F, 0xA8), 205, SHARD),
    ("medium",                         (0x24, 0x30, 0x3A), (0x8C, 0xD8, 0xE8), 206, MEDIUM),
    ("dreamland",                      (0x2A, 0x24, 0x40), (0xC8, 0xA8, 0xF0), 207, DREAMLAND),
]

# ── Boss altars ───────────────────────────────────────────────────────────────
# Each is a pair: armed, then spent. Spent is the same base darkened with the frame
# dropped, so the two read as one block in two states.

BLOCKS = [
    # name,                     base,               accent,              seed, frame
    ("jesselton_projector",       (0x2A, 0x24, 0x38), (0xB8, 0x26, 0x2E), 301, True),
    ("jesselton_projector_spent", (0x1A, 0x16, 0x22), (0x1A, 0x16, 0x22), 302, False),
    ("dorothys_terminal",       (0x1E, 0x30, 0x2A), (0x5F, 0xE8, 0xA8), 303, True),
    ("dorothys_terminal_spent", (0x13, 0x1E, 0x1A), (0x13, 0x1E, 0x1A), 304, False),
]


def main():
    print("entity textures ->")
    for name, width, height, base, accent, seed, stripe in ENTITIES:
        entity_texture(name, width, height, base, accent, seed, stripe)
    print("item textures ->")
    for name, base, accent, seed, glyph in ITEMS:
        item_texture(name, base, accent, seed, glyph)
    print("block textures ->")
    for name, base, accent, seed, frame in BLOCKS:
        block_texture(name, base, accent, seed, frame)


if __name__ == "__main__":
    main()
