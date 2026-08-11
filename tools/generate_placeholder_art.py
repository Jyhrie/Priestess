#!/usr/bin/env python3
"""Generate the placeholder mob, item and block textures.

Everything this writes is a stand-in. The mobs and the dungeons exist so a score movement
can be walked end to end; none of them are drawn yet. Rather than ship a pile
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


def arts_lab_pattern(kind, x, y):
    """True where the accent colour goes, for the Rhine Lab Arts Lab build set.

    Predicates rather than ASCII masks like the items use: these five are variations on one
    another, and a set that has to stay a set is easier to keep coherent as six lines of
    arithmetic than as six hand-drawn grids that drift apart the moment one is edited.
    """
    edge = x in (0, 15) or y in (0, 15)
    if kind == "chiseled":
        frame = (x in (3, 12) and 3 <= y <= 12) or (y in (3, 12) and 3 <= x <= 12)
        core = 6 <= x <= 9 and 6 <= y <= 9 and (x + y) % 2 == 0
        return edge or frame or core
    if kind == "plated":
        seam = y in (7, 8)
        rivets = x in (2, 13) and y in (2, 5, 10, 13)
        return edge or seam or rivets
    if kind == "concrete":
        # The plain one of the set — no structure at all, just a scatter of aggregate, so it
        # is the block you can put a hundred of in a wall without it reading as a pattern.
        return (x * 7 + y * 13) % 37 == 0
    if kind == "tile":
        return x in (0, 7, 8, 15) or y in (0, 7, 8, 15)
    if kind == "pillar_side":
        return x in (0, 1, 7, 8, 14, 15)
    if kind == "pillar_top":
        ring = max(abs(x - 7.5), abs(y - 7.5))
        return edge or 3.5 < ring < 5.5
    if kind == "catacombs":
        # Running bond masonry: a course every four rows, with the head joints of one course
        # offset half a brick from the next. Reads as stacked blockwork rather than as tiles,
        # which is what tells the catacombs apart from the Arts Lab at a glance.
        course = y % 4 == 0
        head = x % 8 == (0 if (y // 4) % 2 == 0 else 4)
        return course or head
    if kind == "catacombs_overgrown":
        # The same bond with growth on it. The moss is deterministic per pixel rather than
        # random so the two tiles line up as the same stone, and it hangs from the courses
        # because that is where water sits.
        course = y % 4 == 0
        head = x % 8 == (0 if (y // 4) % 2 == 0 else 4)
        moss = (x * 5 + y * 11) % 17 < 6 and (y % 4) < 2
        return course or head or moss
    raise ValueError("unknown pattern %r" % kind)


def patterned_block(name, base, accent, seed, kind):
    """A 16x16 tile whose accent pixels come from `kind`. Same grain as the other blocks."""
    width = height = 16
    rng = random.Random(seed)
    pixels = bytearray(width * height * 4)
    for y in range(height):
        for x in range(width):
            colour = accent if arts_lab_pattern(kind, x, y) else base
            jitter = rng.randint(-10, 10)
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
# One palette per dungeon, so a screenshot says where you are: Mansfield is prison denim,
# Dorothy's Vision is diseased green, Under Tides is abyssal blue-green, Rhine Lab is clean
# white and blue. The slug belongs to none of them — Originium cyan on wasteland grey.

ENTITIES = [
    # name,                    w,   h,  base colour, accent colour, seed, stripe
    ("originium_slug",         64,  32, (0x4A, 0x5A, 0x66), (0x5F, 0xC8, 0xE8), 101, 6),
    ("mb_jesselton_williams",  64,  64, (0x3A, 0x30, 0x4E), (0xB8, 0x26, 0x2E), 103, 8),
    # The three Medium-bearers, one palette per rung so they read as a set at a glance:
    # Failure is going-off green, Replica is clean bone and cold blue, Bionic is gunmetal
    # and hazard orange. These are box-UV placeholders for the geo models in
    # geo/entity/dv_{failure,replica,bionic}.geo.json — a real Blockbench export replaces both
    # the .geo.json and the texture together, and the entry here goes with them.
    ("dv_failure",             64,  64, (0x3A, 0x4A, 0x3E), (0xA8, 0xC0, 0x60), 105, 7),
    ("dv_replica",             64,  64, (0xC8, 0xC4, 0xBC), (0x5F, 0x8C, 0xE8), 106, 9),
    ("dv_bionic",              64,  64, (0x3E, 0x46, 0x52), (0xE8, 0x7A, 0x2E), 107, 5),
    # Mansfield's inmates. One prison-denim base across all three so they read as a block,
    # with the accent doing the telling apart: grey for the plain one, rust for the big one,
    # bowstring pale for the one that shoots. Box-UV placeholders for the geo models in
    # geo/entity/mb_imprisoned_*.geo.json — real art replaces both files and this entry.
    ("mb_imprisoned_pugilist", 64,  64, (0x3E, 0x4A, 0x5C), (0x8C, 0x9A, 0xA8), 108, 8),
    ("mb_imprisoned_recidivist", 64,  64, (0x2E, 0x38, 0x46), (0xB8, 0x56, 0x2E), 109, 6),
    ("mb_imprisoned_sniper",   64,  64, (0x4A, 0x56, 0x68), (0xD8, 0xCF, 0xA8), 110, 10),
    # Under Tides (sv_ is for Sal Viento, the town). Abyssal blue-green throughout with bioluminescent accents, so the whole
    # dungeon reads cold and wet against Mansfield's denim and Dorothy's sickly greens.
    # Sizes must match the texture_width/height in the matching geo file, which
    # generate_placeholder_models.py picks by packing — 64 for the five trash mobs, 128 for
    # the miniboss, 256 for Quintus.
    ("sv_runner",              64,  64, (0x1E, 0x4A, 0x50), (0x4A, 0xD8, 0xC8), 111, 6),
    ("sv_spitter",             64,  64, (0x2E, 0x4A, 0x36), (0xA8, 0xE8, 0x50), 112, 7),
    ("sv_reaper",              64,  64, (0x18, 0x22, 0x38), (0xD8, 0xD0, 0xB8), 113, 9),
    ("sv_crawler",             64,  64, (0x3A, 0x3E, 0x2E), (0xC8, 0x7A, 0x3A), 114, 5),
    ("sv_piercer",             64,  64, (0x36, 0x44, 0x52), (0xBC, 0xE0, 0xF0), 115, 8),
    ("sv_the_first_to_talk",  128, 128, (0xB8, 0xAE, 0xA0), (0x5A, 0x2E, 0x78), 116, 14),
    ("sv_bishop_quintus",     256, 256, (0x14, 0x28, 0x44), (0xE0, 0xC8, 0x70), 117, 24),
    # NOT "dv_awaken". Its texture is a real 128x128 export that belongs to the Blockbench
    # model in geo/entity/dv_awaken.geo.json, and the UVs only line up with that file. This
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

# ── Rhine Lab, Arts Lab wing ──────────────────────────────────────────────────
# Rhine's palette is clean white and cold blue, against Mansfield's denim and Dorothy's
# sickly greens — a screenshot of a corridor should say which building it is. One base
# across all five so they read as one build set, with the pattern doing the telling apart;
# the pillar is two tiles because a RotatedPillarBlock needs a top as well as a side.
#
# These are the blocks the lockdown gates behind Dorothy's Vision, which is worth knowing
# while looking at them: they are load-bearing for progression, not only decoration.

ARTS_LAB_PANEL = (0xC6, 0xCD, 0xD4)
ARTS_LAB_BLUE = (0x3E, 0x7A, 0xB8)
ARTS_LAB_CONCRETE = (0x9A, 0xA2, 0xA8)

ARTS_LAB_BLOCKS = [
    # name,                                 base,               accent,             seed, pattern
    ("rhine_lab_arts_lab_chiseled_wall",  ARTS_LAB_PANEL,    ARTS_LAB_BLUE,      311, "chiseled"),
    ("rhine_lab_arts_lab_plated_wall",    (0xAE, 0xB6, 0xBE), (0x5A, 0x6A, 0x78), 312, "plated"),
    ("rhine_lab_arts_lab_concrete_wall",  ARTS_LAB_CONCRETE, (0x7A, 0x82, 0x88),  313, "concrete"),
    ("rhine_lab_arts_lab_tile",           (0xD8, 0xDE, 0xE4), (0x8C, 0xA8, 0xC0), 314, "tile"),
    ("rhine_lab_arts_lab_pillar_side",    ARTS_LAB_PANEL,    ARTS_LAB_BLUE,      315, "pillar_side"),
    ("rhine_lab_arts_lab_pillar_top",     ARTS_LAB_PANEL,    ARTS_LAB_BLUE,      316, "pillar_top"),
]

# ── Sal Viento, the catacombs ─────────────────────────────────────────────────
# Under Tides' build set, gated behind Bishop Quintus. Drowned grey-green masonry against
# Rhine's clean white — a corridor screenshot should say which movement you are in. The
# overgrown variant is the same stone and the same bond with growth on it, so the two read
# as one wall in two states rather than as two different stones.

CATACOMBS_STONE = (0x6E, 0x74, 0x70)
CATACOMBS_JOINT = (0x44, 0x4B, 0x49)
CATACOMBS_MOSS = (0x4E, 0x6B, 0x45)

SAL_VIENTO_BLOCKS = [
    # name,                                    base,             accent,           seed, pattern
    ("sal_viento_catacombs_stone",           CATACOMBS_STONE, CATACOMBS_JOINT,   321, "catacombs"),
    ("sal_viento_catacombs_overgrown_stone", CATACOMBS_STONE, CATACOMBS_MOSS,    322, "catacombs_overgrown"),
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
    for name, base, accent, seed, kind in ARTS_LAB_BLOCKS + SAL_VIENTO_BLOCKS:
        patterned_block(name, base, accent, seed, kind)


if __name__ == "__main__":
    main()
