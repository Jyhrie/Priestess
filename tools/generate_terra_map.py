#!/usr/bin/env python3
"""
Generates the two PNGs that define Terra's geography.

    tools/generate_terra_map.py  ->  src/main/resources/data/priestess/terra/
                                         regions.png     political map, one colour per region
                                         elevation.png   greyscale height, 0 = abyss, 255 = peak
                                     docs/
                                         terra_map_preview.png   shaded preview, for humans

Run it with `python tools/generate_terra_map.py`. It needs numpy and nothing else —
PNG encoding is done here with zlib so the tool has no image-library dependency.

WHY THIS EXISTS
    The PNGs are the source of truth for where everything is, and they are checked in.
    You do not have to run this script ever again: open the PNGs in any image editor and
    repaint them. This script only exists so the *initial* layout is reproducible and so
    there is a written record of which canon fact put each region where.

    If you do repaint by hand, the only hard rules are:
      - a colour in regions.png must exactly match a colour in TerraRegion.java
      - elevation.png must be the same size as regions.png
      - the two must agree: don't paint ocean-blue over an elevation of 200

CANON THIS LAYOUT IS BUILT FROM
    - Terra is a supercontinent. The oceans "encompass the supercontinent from the west
      of Bolivar, the south of Iberia and Sargon, to the east of Rim Billiton, Yan, and
      Higashi."
    - "The Foehn Hotlands south of mainland Sargon and the Infy Icefield north of Sami
      and Ursus" border terra incognita.
    - The Sea of Clariside is "located between southern Victoria, northern Iberia, the
      Siestan peninsula, and the Acahuallan region" and makes Mediterranean biomes.
    - The Norte Sea is "in northern Bolivar".
    - "A mountain range runs from northern Kazimierz, passing through the mountain basin
      that forms Kjerag, to the endless Sargonian desert."
    - Aegir lies in the ocean south of Iberia; the two are at war, which is why Iberia's
      coastal cities are dead.
    - Victoria is central Terra and the most fertile.
    - The traditional heartland was Victoria, Gaul, Leithanien, Iberia and Laterano.
    - "Barrenlands" is the canon term for unclaimed territory, which is what Gaul became.
"""

import os
import struct
import zlib

import numpy as np

# ── Canvas ────────────────────────────────────────────────────────────────────
# W and H are the RESOLUTION of the map: how many pixels of detail you are authoring.
# They have nothing to do with how big the world is.
#
# World size is set by WORLD_WIDTH_BLOCKS in TerraMap.java, which derives blocks-per-pixel
# as WORLD_WIDTH_BLOCKS / W. The two are independent: raise W and H for a more detailed
# map at the same world size, raise WORLD_WIDTH_BLOCKS for a bigger world at the same
# detail.
#
# Everything in this file works in pixel space, so WORLD_WIDTH_BLOCKS below is used for
# the printed summary and nothing else — the PNGs come out identical whatever it says.
# Keep it in step with the Java anyway, so the summary does not lie to you.
W, H = 1024, 640
WORLD_WIDTH_BLOCKS = 131_072        # mirror of TerraMap.WORLD_WIDTH_BLOCKS
BLOCKS_PER_PIXEL = WORLD_WIDTH_BLOCKS / W

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(HERE)
OUT_DATA = os.path.join(ROOT, "src", "main", "resources", "data", "priestess", "terra")
OUT_DOCS = os.path.join(ROOT, "docs")

# ── Regions ───────────────────────────────────────────────────────────────────
# name -> (colour, seed points, elevation bias)
#
# The colour is the contract with TerraRegion.java. The seed points drive a warped
# Voronoi assignment, so borders come out organic rather than geometric; give a region
# several seeds to stretch it. Elevation bias nudges the whole region up or down after
# the coast/mountain terms are applied.
# name -> (colour, seeds, reach, elevation bias)
#
# "Reach" is how far a nation's claim extends from its seeds, in pixels. Land goes to
# whichever nation minimises (distance - reach); where every nation scores above zero,
# nobody has claimed it and it becomes barrenlands. That is what the canon term means —
# unclaimed territory between nations — and it beats giving barrenlands its own seeds,
# which made it compete on equal terms and swallow the map.
LAND = {
    # ── The north: Sami and Ursus, with the Infy Icefield beyond them ──
    "SAMI":         (0xBFE3F2, [(430, 118), (500, 132)],              72, +0.02),
    "URSUS":        (0x7A93B5, [(640, 145), (722, 176), (585, 192)],  96, +0.04),

    # ── The heartland: Victoria, Gaul, Leithanien, Iberia, Laterano ──
    "VICTORIA":     (0x6FA86B, [(345, 300), (372, 346)],              58, +0.00),
    "GAUL":         (0x9C9478, [(268, 332)],                          42, +0.00),
    "LEITHANIEN":   (0x4E7C55, [(432, 318)],                          48, +0.03),
    "KAZIMIERZ":    (0x9CC46A, [(520, 278), (542, 322)],              62, +0.01),
    "KJERAG":       (0xE4EEF5, [(614, 326)],                          30, +0.13),

    # ── The Clariside rim ──
    # Every seed here must sit on the *rim*, not in the water. The first draft put
    # Siesta, Laterano and Siracusa inside the Clariside ellipse, which drowned them.
    "SIESTA":       (0xF0C86A, [(420, 456)],                          26, +0.05),
    "ACAHUALLA":    (0x7FA05A, [(302, 470)],                          42, -0.01),
    "LATERANO":     (0xF2E9C4, [(522, 480)],                          22, +0.01),
    "SIRACUSA":     (0xD9A25C, [(562, 462)],                          46, +0.02),
    "MINOS":        (0xC9B98A, [(590, 510)],                          40, +0.02),

    # ── The south ──
    "IBERIA":       (0x8FA0B0, [(392, 500), (442, 524)],              62, -0.01),
    "KAZDEL":       (0x7A3A3A, [(650, 478)],                          46, +0.06),
    "SARGON":       (0xE0B060, [(748, 440), (802, 482), (712, 502)],  88, -0.02),
    "FOEHN":        (0xD9743A, [(700, 592), (822, 588)],              70, -0.03),

    # ── The east ──
    "YAN":          (0x8FB8A0, [(852, 288), (868, 352)],              76, +0.03),
    "HIGASHI":      (0xA8CBB4, [(954, 300)],                          44, +0.02),
    "RIM_BILLITON": (0x8A7F6E, [(868, 470)],                          44, +0.04),

    # ── The west ──
    "COLUMBIA":     (0xB4C8D8, [(192, 248), (210, 302)],              74, +0.01),
    "BOLIVAR":      (0x6E8A5E, [(188, 444), (202, 502)],              78, -0.01),
    "DOSSOLES":     (0x6FD8E0, [(172, 374)],                          26, -0.04),

    # The fallback. No seeds and no reach — it is simply what is left over.
    "BARRENLANDS":  (0xA89A80, [],                                     0, +0.00),
}

# How far past its reach a nation still holds ground before the land counts as
# unclaimed, and how much coast a nation always keeps regardless.
#
# The slack alone is not enough. With slack 0 the barrenlands formed one unbroken ring
# around the entire continent, because every coastline is further from a capital than
# anywhere inland — which left Iberia, a seafaring nation, landlocked. Raising the slack
# until that stopped left barely any barrenlands at all. So the rule is two-part:
# unclaimed ground becomes barrenlands only in the *interior*. Nations always hold their
# own coast, and the wastes sit between them where they belong.
BARRENLANDS_SLACK = 4.0
COASTLINE_ALWAYS_CLAIMED = 15   # pixels inland, ~1900 blocks

WATER = {
    "AEGIR":        (0x0E1A2E, +0.00),   # the ocean south of Iberia
    "OPEN_OCEAN":   (0x1D4E7A, +0.00),   # west of Bolivar, east of Yan
    "CLARISIDE":    (0x39B9D6, +0.00),   # the inland sea
    "NORTE_SEA":    (0x49C4C0, +0.00),   # northern Bolivar
}

INFY_COLOUR = 0xEAF6FF   # the Infy Icefield, north of Sami and Ursus

ALL_COLOURS = {n: c for n, (c, _, _, _) in LAND.items()}
ALL_COLOURS.update({n: c for n, (c, _) in WATER.items()})
ALL_COLOURS["INFY"] = INFY_COLOUR

# ── Continent outline ─────────────────────────────────────────────────────────
# Union of ellipses. Metaballs would give softer coasts but ellipses plus the noise
# warp below are already organic enough, and they are far easier to reason about when
# you want to move a landmass.
CONTINENT = [
    (186, 336, 104, 188),   # the western wing: Columbia over Bolivar
    (198, 470,  92, 122),   # southern Bolivar
    (298, 342,  76, 104),   # the isthmus joining it to the heartland
    (420, 330, 176, 164),   # the heartland
    (392, 486, 104,  88),   # Iberia, reaching south to the ocean
    (620, 168, 254, 106),   # the north: Sami and Ursus
    (742, 470, 168, 118),   # Sargon
    (778, 606, 156,  86),   # the Foehn frontier, running off the bottom edge
    (852, 322, 106, 158),   # the east: Yan and Rim Billiton
    (958, 300,  44,  64),   # Higashi, all but an island
    (676, 296, 116, 108),   # the barrenlands bridging north to Sargon
]

# Inland seas, carved back out of the land: (cx, cy, rx, ry, region)
INLAND_SEAS = [
    (424, 432, 118, 44, "CLARISIDE"),
    (200, 344,  54, 29, "NORTE_SEA"),
]

# Land added back *after* the seas are carved, so it can jut into one.
# "The Sea of Clariside is located between southern Victoria, northern Iberia, the
#  Siestan peninsula, and the Acahuallan region" — so Siesta has to reach into it.
PENINSULAS = [
    (420, 452, 30, 30),   # the Siestan peninsula
]

# Aegir's water: the ocean south of Iberia. An ellipse rather than a rectangle, warped
# like everything else, so it does not read as a box drawn over the sea.
AEGIR_ELLIPSE = (418, 588, 214, 124)

# The Infy Icefield, as a lens across the north that runs off the top edge of the map.
#
# Running it off the edge is the point. TerraMap clamps an out-of-bounds lookup to the
# nearest edge pixel, so whatever is painted on row 0 is what you get forever if you keep
# walking north — and canon puts the icefield and then terra incognita up there, not
# ocean. Painting the frontier into the map is also what stops the world edge from
# shimmering: the first draft special-cased out-of-bounds to "always Infy", and because
# the lookup is domain-warped, the top of the world came out as a two-pixel band
# flickering between icefield and open sea.
INFY_LENS = (600, -36, 430, 118)

# Mountain ranges as polylines: (points, half-width in px, height added)
RANGES = [
    # "from northern Kazimierz, passing through the mountain basin that forms Kjerag,
    #  to the endless Sargonian desert" — the one range canon actually routes for us.
    ([(536, 236), (572, 282), (614, 326), (662, 378), (722, 424), (772, 452)], 42, 0.60),
    ([(600, 118), (682, 150), (752, 192)], 34, 0.40),   # the Ursus north
    ([(838, 248), (864, 320), (880, 392)], 32, 0.48),   # the Yanese spine
    ([(140, 248), (152, 350), (168, 458), (182, 520)], 28, 0.52),  # the western cordillera
    ([(358, 488), (420, 508)], 24, 0.32),               # the Iberian hills
    ([(950, 268), (964, 332)], 20, 0.36),               # Higashi
    ([(858, 448), (884, 486)], 22, 0.38),               # Rim Billiton's mining ridges
]

# ── Elevation bands ───────────────────────────────────────────────────────────
# Normalised elevation, and the terrain slot each range maps to. These are mirrored
# exactly in TerraMap.java — change both together or biomes will disagree with terrain.
#   0.00-0.16  deep sea      0.48-0.62  flats
#   0.16-0.34  sea           0.62-0.74  midland
#   0.34-0.40  shore         0.74-0.86  hills
#   0.40-0.48  lowland       0.86-1.00  mountain
# The waterline sits at 0.37, inside the shore band, so a shore is half surf and half
# dry sand exactly as it was under the old climate model.
SHORE_LO, WATERLINE, LAND_LO = 0.34, 0.37, 0.40


# ── PNG encoding (stdlib only) ────────────────────────────────────────────────
def write_png(path, array, greyscale=False):
    """array is (H,W) uint8 for greyscale, or (H,W,3) uint8 for colour."""
    h, w = array.shape[0], array.shape[1]
    colour_type, channels = (0, 1) if greyscale else (2, 3)
    flat = array.reshape(h, w * channels)
    # Each scanline is prefixed with filter type 0 (None).
    raw = b"".join(b"\x00" + flat[y].tobytes() for y in range(h))

    def chunk(tag, data):
        body = tag + data
        return struct.pack(">I", len(data)) + body + struct.pack(">I", zlib.crc32(body))

    png = b"\x89PNG\r\n\x1a\n"
    png += chunk(b"IHDR", struct.pack(">IIBBBBB", w, h, 8, colour_type, 0, 0, 0))
    png += chunk(b"IDAT", zlib.compress(raw, 9))
    png += chunk(b"IEND", b"")
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "wb") as f:
        f.write(png)
    print(f"  wrote {os.path.relpath(path, ROOT)}  ({w}x{h}, {len(png)/1024:.0f} KB)")


# ── Helpers ───────────────────────────────────────────────────────────────────
def value_noise(shape, cells, seed, octaves=3):
    """Smooth tileable-ish value noise in [-1,1], built by upsampling random grids."""
    rng = np.random.default_rng(seed)
    h, w = shape
    total = np.zeros((h, w), dtype=np.float64)
    amp, norm = 1.0, 0.0
    for o in range(octaves):
        cy, cx = max(2, cells << o), max(2, cells << o)
        grid = rng.random((cy + 1, cx + 1)) * 2 - 1
        yi = np.linspace(0, cy, h)
        xi = np.linspace(0, cx, w)
        y0 = np.floor(yi).astype(int).clip(0, cy - 1)
        x0 = np.floor(xi).astype(int).clip(0, cx - 1)
        fy = (yi - y0)[:, None]
        fx = (xi - x0)[None, :]
        sy = fy * fy * (3 - 2 * fy)
        sx = fx * fx * (3 - 2 * fx)
        g00 = grid[np.ix_(y0, x0)]
        g01 = grid[np.ix_(y0, x0 + 1)]
        g10 = grid[np.ix_(y0 + 1, x0)]
        g11 = grid[np.ix_(y0 + 1, x0 + 1)]
        total += amp * ((g00 * (1 - sx) + g01 * sx) * (1 - sy)
                        + (g10 * (1 - sx) + g11 * sx) * sy)
        norm += amp
        amp *= 0.5
    return total / norm


def ellipse_mask(cx, cy, rx, ry, yy, xx):
    return ((xx - cx) / rx) ** 2 + ((yy - cy) / ry) ** 2 <= 1.0


def distance_to(mask):
    """Chebyshev-ish distance transform by iterative dilation. Exact enough here and
    keeps the dependency list at numpy."""
    dist = np.full(mask.shape, np.inf)
    dist[mask] = 0.0
    cur = mask.copy()
    for d in range(1, 200):
        grown = cur.copy()
        grown[1:, :] |= cur[:-1, :]
        grown[:-1, :] |= cur[1:, :]
        grown[:, 1:] |= cur[:, :-1]
        grown[:, :-1] |= cur[:, 1:]
        new = grown & ~cur
        if not new.any():
            break
        dist[new] = d
        cur = grown
    return dist


def blur(a, radius, passes=3):
    """Separable box blur. Used on the elevation bias field: a per-region constant
    applied raw would put a vertical cliff on every national border, which is exactly
    the artefact the first draft of this map had."""
    out = a.astype(np.float64)
    k = 2 * radius + 1
    for _ in range(passes):
        pad = np.pad(out, ((0, 0), (radius, radius)), mode="edge")
        c = np.cumsum(pad, axis=1)
        out = (c[:, k - 1:] - np.concatenate(
            [np.zeros((out.shape[0], 1)), c[:, :-k]], axis=1)) / k
        pad = np.pad(out, ((radius, radius), (0, 0)), mode="edge")
        c = np.cumsum(pad, axis=0)
        out = (c[k - 1:, :] - np.concatenate(
            [np.zeros((1, out.shape[1])), c[:-k, :]], axis=0)) / k
    return out


def polyline_distance(points, yy, xx):
    """Distance from every pixel to a polyline, as the min over its segments."""
    best = np.full(yy.shape, np.inf)
    for (x1, y1), (x2, y2) in zip(points, points[1:]):
        dx, dy = x2 - x1, y2 - y1
        length2 = dx * dx + dy * dy
        t = np.clip(((xx - x1) * dx + (yy - y1) * dy) / length2, 0.0, 1.0)
        px, py = x1 + t * dx, y1 + t * dy
        best = np.minimum(best, np.hypot(xx - px, yy - py))
    return best


# ── Build ─────────────────────────────────────────────────────────────────────
def build():
    yy, xx = np.mgrid[0:H, 0:W].astype(np.float64)

    # Two independent warp fields. The coast one is broad and strong, so the continent
    # outline stops looking like the union of ellipses it actually is. The border one is
    # finer, so national borders wander at a scale you notice while walking them.
    cwx = value_noise((H, W), 3, seed=17, octaves=5) * 54.0
    cwy = value_noise((H, W), 3, seed=93, octaves=5) * 54.0
    coast_x, coast_y = xx + cwx, yy + cwy

    bwx = coast_x + value_noise((H, W), 9, seed=204, octaves=4) * 26.0
    bwy = coast_y + value_noise((H, W), 9, seed=311, octaves=4) * 26.0

    # 1. Land mask -------------------------------------------------------------
    land = np.zeros((H, W), dtype=bool)
    for cx, cy, rx, ry in CONTINENT:
        land |= ellipse_mask(cx, cy, rx, ry, coast_y, coast_x)

    # The northern frontier is land, and it reaches the top edge of the map.
    infy = ellipse_mask(*INFY_LENS, coast_y, coast_x)
    land |= infy

    # 2. Inland seas, cut back out of the land ---------------------------------
    sea_id = np.full((H, W), -1, dtype=np.int32)
    water_names = list(WATER.keys())
    for cx, cy, rx, ry, name in INLAND_SEAS:
        m = ellipse_mask(cx, cy, rx, ry, coast_y, coast_x)
        land &= ~m
        sea_id[m] = water_names.index(name)

    for cx, cy, rx, ry in PENINSULAS:
        m = ellipse_mask(cx, cy, rx, ry, coast_y, coast_x)
        land |= m
        sea_id[m] = -1

    # Distances to the shoreline. Needed by both the region step and the elevation step.
    inland_d = distance_to(~land)          # 0 at the shoreline, grows inland
    offshore_d = distance_to(land)         # 0 at the shoreline, grows out to sea

    # 3. Region assignment on land ---------------------------------------------
    # Nations claim outward from their seeds up to their reach; whatever no nation
    # reaches is unclaimed, and unclaimed ground is the barrenlands.
    land_names = list(LAND.keys())
    barren = land_names.index("BARRENLANDS")
    best = np.full((H, W), np.inf)
    region = np.full((H, W), barren, dtype=np.int32)
    for i, name in enumerate(land_names):
        _, seeds, reach, _ = LAND[name]
        if not seeds:
            continue
        d = np.full((H, W), np.inf)
        for sx, sy in seeds:
            d = np.minimum(d, np.hypot(bwx - sx, bwy - sy))
        score = d - reach
        closer = score < best
        best = np.where(closer, score, best)
        region = np.where(closer, i, region)
    unclaimed = (best > BARRENLANDS_SLACK) & (inland_d > COASTLINE_ALWAYS_CLAIMED)
    region = np.where(unclaimed, barren, region)

    # 4. Ocean regions ---------------------------------------------------------
    ocean = ~land & (sea_id < 0)
    acx, acy, arx, ary = AEGIR_ELLIPSE
    aegir = ocean & ellipse_mask(acx, acy, arx, ary, coast_y, coast_x)
    sea_id[ocean] = water_names.index("OPEN_OCEAN")
    sea_id[aegir] = water_names.index("AEGIR")

    # 5. Elevation -------------------------------------------------------------
    elev = np.zeros((H, W))
    # Land climbs from the shore band into the flats band.
    elev = np.where(land, 0.370 + 0.205 * np.tanh(inland_d / 40.0), elev)
    # Water falls from the shore band into the abyss.
    elev = np.where(~land, 0.360 - 0.310 * np.tanh(offshore_d / 30.0), elev)
    # Inland seas are shallow — they never reach abyssal depth.
    shallow = (sea_id == water_names.index("CLARISIDE"))
    shallow |= (sea_id == water_names.index("NORTE_SEA"))
    elev = np.where(shallow, np.clip(elev, 0.235, 0.360), elev)

    # Mountain ranges. Only raise land: a range must not build a wall out at sea.
    for points, halfwidth, height in RANGES:
        d = polyline_distance(points, bwy, bwx)
        ridge = np.clip(1.0 - d / halfwidth, 0.0, 1.0) ** 1.6
        elev += land * ridge * height

    # Per-region bias, blurred so national borders are gradients and not cliffs.
    bias = np.zeros((H, W))
    for i, name in enumerate(land_names):
        bias += (region == i) * LAND[name][3]
    elev += land * blur(bias, 9)

    # Roughness, so nothing is a perfectly smooth dome.
    elev += land * value_noise((H, W), 24, seed=41, octaves=4) * 0.030
    elev = np.clip(elev, 0.0, 1.0)

    # Whatever the terms above did, water must stay below the waterline and land above
    # it — otherwise a biome painted "ocean" generates as dry ground, or vice versa.
    # Clamp to the WATERLINE, not to LAND_LO: clamping land up to 0.40 would push every
    # coast straight past the shore band and leave the world with no beaches at all.
    elev = np.where(land, np.maximum(elev, WATERLINE + 0.002), elev)
    elev = np.where(~land, np.minimum(elev, WATERLINE - 0.002), elev)

    # 6. Compose the region raster --------------------------------------------
    out_region = np.zeros((H, W, 3), dtype=np.uint8)

    def paint(mask, colour):
        out_region[mask] = [(colour >> 16) & 255, (colour >> 8) & 255, colour & 255]

    for i, name in enumerate(water_names):
        paint(sea_id == i, WATER[name][0])
    for i, name in enumerate(land_names):
        paint(land & (region == i), LAND[name][0])
    paint(infy, INFY_COLOUR)

    out_elev = (elev * 255.0 + 0.5).astype(np.uint8)
    return out_region, out_elev, land, infy, region, land_names


def preview(region_rgb, elev):
    """Hillshaded political map, for the README and for eyeballing the layout."""
    e = elev.astype(np.float64) / 255.0
    gy, gx = np.gradient(e)
    shade = np.clip(0.5 + 5.0 * (gx * 0.7 + gy * 0.7), 0.15, 1.35)
    out = region_rgb.astype(np.float64) * shade[..., None]
    return np.clip(out, 0, 255).astype(np.uint8)


SLOTS = [("deep sea", 0.00, 0.16), ("sea", 0.16, 0.34), ("shore", 0.34, 0.40),
         ("lowland", 0.40, 0.48), ("flats", 0.48, 0.62), ("midland", 0.62, 0.74),
         ("hills", 0.74, 0.86), ("mountain", 0.86, 1.01)]

if __name__ == "__main__":
    print("Generating Terra...")
    regions, elevation, land, infy, region_idx, land_names = build()

    write_png(os.path.join(OUT_DATA, "regions.png"), regions)
    write_png(os.path.join(OUT_DATA, "elevation.png"), elevation, greyscale=True)
    write_png(os.path.join(OUT_DOCS, "terra_map_preview.png"), preview(regions, elevation))

    e = elevation / 255.0
    print(f"\n  resolution   {W} x {H} px  ({BLOCKS_PER_PIXEL:g} blocks/px)")
    print(f"  world size   {round(W * BLOCKS_PER_PIXEL):,} x {round(H * BLOCKS_PER_PIXEL):,} blocks")
    print(f"  land         {100 * land.mean():.1f}%    Infy frontier {100 * infy.mean():.1f}%")

    print("\n  Terrain slots, share of the whole map:")
    for name, lo, hi in SLOTS:
        print(f"    {name:9s} {100 * np.mean((e >= lo) & (e < hi)):5.1f}%")

    print("\n  Share of land per region:")
    on_land = land & ~infy
    rows = [(100 * np.mean(on_land & (region_idx == i)) / on_land.mean(), n)
            for i, n in enumerate(land_names)]
    for share, name in sorted(rows, reverse=True):
        print(f"    {name:14s} {share:5.1f}%")
