#!/usr/bin/env python3
"""
Generates a starting relief.png from regions.png.

    tools/generate_relief_map.py  ->  src/main/resources/data/priestess/terra/relief.png

WHAT RELIEF IS

    A third greyscale map alongside elevation.png. elevation.png says how HIGH the ground
    is; relief.png says how much it RISES AND FALLS once it gets there. They are separate
    so that a high plateau and a broken lowland are both paintable — relief used to be
    derived from elevation, which made brightening a mountain make it bumpier as well as
    taller, with no way to opt out.

    Grey reads directly as blocks, via TerraReliefFunction.MAX_RELIEF_BLOCKS = 48:

        grey   0  ->   0 blocks   dead flat: salt pan, paddy, ice shelf
        grey  43  ->   8 blocks   open and walkable — steppe
        grey  80  ->  15 blocks   ordinary rolling country
        grey 128  ->  24 blocks   hill country, visible spurs and gullies
        grey 180  ->  34 blocks   mountain
        grey 255  ->  48 blocks   broken crag, ledges and climbing

    That is the HEIGHT of a spur, not its spacing. Spurs land ~128 blocks apart whatever
    this map says; that is rangeScale in ModNoiseSettings.

WHY THIS SCRIPT EXISTS

    Only to give you a sensible first draft, once. After that, relief.png is the source of
    truth and you should paint it by hand — that is the entire point of it being a map.
    Re-running this OVERWRITES your painting, so it refuses unless you pass --force.

    The draft assigns one value per region and then blurs across the borders, because a
    hard step in relief is a place where the hills stop mid-slope and it reads as a seam.
    Airbrush freely on top; gradients survive into the world (TerraMap samples relief
    bilinearly, same as elevation).

    Needs numpy and nothing else — PNG encoding is done here with zlib, matching
    generate_terra_map.py.
"""
import argparse
import os
import struct
import sys
import zlib

import numpy as np

HERE = os.path.dirname(os.path.abspath(__file__))
TERRA = os.path.join(HERE, "..", "src", "main", "resources", "data", "priestess", "terra")
REGIONS = os.path.join(TERRA, "regions.png")
RELIEF = os.path.join(TERRA, "relief.png")

MAX_RELIEF_BLOCKS = 48.0  # must match TerraReliefFunction.MAX_RELIEF_BLOCKS

# Region colour -> how many blocks that place rises and falls locally.
# Only a starting point; paint over it.
RELIEF_BLOCKS = {
    0x000000: 4,    # OCEAN         sea floor, damped to nothing at the shore anyway
    0xFFFFFF: 6,    # INFY          ice shelf, flat
    0x98CAFF: 11,   # SAMI          snowfields with some roll
    0x6B0A0A: 15,   # URSUS_COLD    taiga
    0xFFC532: 8,    # URSUS_DRY     steppe: open, that is the whole character
    0xFF3232: 15,   # URSUS_WARM    mixed forest
    0x165A74: 34,   # KJERAG        summit country
    0x558496: 38,   # MOUNT_KARLAN  the massif itself, most broken thing on the map
    0x4EFF61: 7,    # KAZIMIERZ     the plains — should read as steppe at any height
    0x837CFF: 15,   # COLUMBIA
    0x2E1AFF: 12,   # IBERIA_LAND   sour heath, low hills
    0xFF9D00: 20,   # YAN           hill country
    0x9E5252: 18,   # HIGASHI_COLD  cedar hills
    0x760006: 9,    # HIGASHI_WARM  terraced paddy, worked flat
    0x2D0000: 30,   # KAZDEL        crags
    0xD1FF00: 15,   # TEMPORARY     unpainted: ordinary rolling country
}

BLUR_PIXELS = 12  # border softening radius; 12 px at 16 blocks/px is ~190 blocks


def read_png(path):
    with open(path, "rb") as f:
        data = f.read()
    assert data[:8] == b"\x89PNG\r\n\x1a\n", f"{path} is not a png"
    pos, idat, palette = 8, b"", None
    width = height = depth = ctype = None
    while pos < len(data):
        (length,) = struct.unpack(">I", data[pos:pos + 4])
        tag = data[pos + 4:pos + 8]
        chunk = data[pos + 8:pos + 8 + length]
        if tag == b"IHDR":
            width, height, depth, ctype, _, _, interlace = struct.unpack(">IIBBBBB", chunk)
            assert interlace == 0, "interlaced png not supported"
        elif tag == b"PLTE":
            palette = np.frombuffer(chunk, dtype=np.uint8).reshape(-1, 3)
        elif tag == b"IDAT":
            idat += chunk
        elif tag == b"IEND":
            break
        pos += 12 + length

    assert depth == 8, f"bit depth {depth} not supported"
    channels = {0: 1, 2: 3, 3: 1, 4: 2, 6: 4}[ctype]
    raw = zlib.decompress(idat)
    stride = width * channels
    out = np.zeros((height, stride), dtype=np.uint8)
    prev = np.zeros(stride, dtype=np.uint8)
    p = 0
    for y in range(height):
        ftype = raw[p]
        p += 1
        line = np.frombuffer(raw[p:p + stride], dtype=np.uint8).astype(np.int32).copy()
        p += stride
        if ftype == 1:
            for i in range(channels, stride):
                line[i] = (line[i] + line[i - channels]) & 0xFF
        elif ftype == 2:
            line = (line + prev) & 0xFF
        elif ftype == 3:
            for i in range(stride):
                left = line[i - channels] if i >= channels else 0
                line[i] = (line[i] + ((left + int(prev[i])) >> 1)) & 0xFF
        elif ftype == 4:
            for i in range(stride):
                a = int(line[i - channels]) if i >= channels else 0
                b = int(prev[i])
                c = int(prev[i - channels]) if i >= channels else 0
                pp = a + b - c
                pa, pb, pc = abs(pp - a), abs(pp - b), abs(pp - c)
                pr = a if (pa <= pb and pa <= pc) else (b if pb <= pc else c)
                line[i] = (line[i] + pr) & 0xFF
        prev = line.astype(np.uint8)
        out[y] = prev
    img = out.reshape(height, width, channels)
    if ctype == 3:
        img = palette[img[:, :, 0]]
    return img


def write_grey_png(path, grey):
    height, width = grey.shape
    raw = b"".join(b"\x00" + grey[y].tobytes() for y in range(height))

    def chunk(tag, payload):
        return (struct.pack(">I", len(payload)) + tag + payload
                + struct.pack(">I", zlib.crc32(tag + payload) & 0xFFFFFFFF))

    with open(path, "wb") as f:
        f.write(b"\x89PNG\r\n\x1a\n")
        f.write(chunk(b"IHDR", struct.pack(">IIBBBBB", width, height, 8, 0, 0, 0, 0)))
        f.write(chunk(b"IDAT", zlib.compress(raw, 9)))
        f.write(chunk(b"IEND", b""))


def box_blur(a, radius):
    """Two passes of a separable box blur — smooth enough for a border, and no scipy."""
    for _ in range(2):
        pad = np.pad(a, ((0, 0), (radius, radius)), mode="edge")
        cum = np.cumsum(pad, axis=1)
        cum = np.pad(cum, ((0, 0), (1, 0)), mode="constant")
        a = (cum[:, 2 * radius + 1:] - cum[:, :-(2 * radius + 1)]) / (2 * radius + 1)
        a = a.T
        pad = np.pad(a, ((0, 0), (radius, radius)), mode="edge")
        cum = np.cumsum(pad, axis=1)
        cum = np.pad(cum, ((0, 0), (1, 0)), mode="constant")
        a = (cum[:, 2 * radius + 1:] - cum[:, :-(2 * radius + 1)]) / (2 * radius + 1)
        a = a.T
    return a


def main():
    ap = argparse.ArgumentParser(description=__doc__,
                                 formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--force", action="store_true",
                    help="overwrite an existing relief.png (throws away your painting)")
    args = ap.parse_args()

    if os.path.exists(RELIEF) and not args.force:
        sys.exit(f"{RELIEF} already exists.\n"
                 f"It is a hand-painted map now — re-running this would throw your work "
                 f"away. Pass --force if you really mean to start over.")

    reg = read_png(REGIONS)[:, :, :3].astype(int)
    height, width = reg.shape[:2]
    packed = (reg[:, :, 0] << 16) | (reg[:, :, 1] << 8) | reg[:, :, 2]

    blocks = np.zeros((height, width), dtype=float)
    unknown = {}
    known = np.zeros((height, width), dtype=bool)
    for colour, value in RELIEF_BLOCKS.items():
        hit = packed == colour
        blocks[hit] = value
        known |= hit
    if not known.all():
        for colour in np.unique(packed[~known]):
            unknown[int(colour)] = int((packed == colour).sum())
        blocks[~known] = RELIEF_BLOCKS[0xD1FF00]

    blocks = box_blur(blocks, BLUR_PIXELS)
    grey = np.clip(np.round(blocks / MAX_RELIEF_BLOCKS * 255.0), 0, 255).astype(np.uint8)
    write_grey_png(RELIEF, grey)

    print(f"wrote {RELIEF}  ({width}x{height})")
    print(f"  relief {blocks.min():.1f}..{blocks.max():.1f} blocks "
          f"(grey {grey.min()}..{grey.max()}), border blur {BLUR_PIXELS} px")
    if unknown:
        print(f"  {len(unknown)} colour(s) not in RELIEF_BLOCKS, defaulted to "
              f"{RELIEF_BLOCKS[0xD1FF00]} blocks:")
        for colour, count in sorted(unknown.items(), key=lambda kv: -kv[1])[:10]:
            print(f"    #{colour:06X}  {count} px")


if __name__ == "__main__":
    main()
