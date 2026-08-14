#!/usr/bin/env python3
"""Generate placeholder GeckoLib models (.geo.json) from a compact bone spec.

Every mob in the mod that is not yet drawn needs a model, and a model needs box-UV
coordinates that fit on the texture sheet without overlapping. Computing those by hand
is the error-prone part — a clash is invisible in the JSON and only shows up in-game as
one limb wearing another limb's pixels — so this does it.

    python tools/generate_placeholder_models.py

Idempotent: the packer is deterministic, so re-running produces byte-identical files.

## What a spec looks like

A bone is (name, parent, pivot, rotation, cubes) and a cube is (origin, size). Sizes are
in Blockbench units, 16 to a block, with y=0 at the model's feet. That is the whole
vocabulary — these are placeholders, so there are no per-cube rotations, no mirroring
and no inflation.

## Why the UVs are packed rather than authored

Box UV lays a cube out as a cross: a (2*(w+d)) x (h+d) rectangle. The packer below is a
plain shelf packer — sort by height, fill rows left to right, start a new row when the
current one is full — which is far from optimal but is trivially correct and leaves the
sheet readable. If a model does not fit on 64x64 it is retried at 128x128 rather than
silently overflowing.

## Scope

**New placeholders only.** The six models that predate this script
(dv_failure, dv_replica, dv_bionic and the three mb_imprisoned_*) were hand-authored and
are not listed here; they are already valid and regenerating them would churn files for
nothing. They can be migrated by writing their bones into ROSTER below.

**Never add dv_awaken.** That is a real Blockbench export whose UVs belong to a hand-made
128x128 texture, exactly as generate_placeholder_art.py says of its texture.

## Blocks as well as mobs

BLOCK_ROSTER writes into geo/block/ instead of geo/entity/, and is otherwise identical —
a GeckoLib block model is the same format as a mob's. The one difference is the coordinate
frame: GeoBlockRenderer puts the model origin at the block's *centre* on the floor, so a
block model runs x and z from -8 to +8 and y from 0 to 16, and anything outside that box
hangs into the neighbouring block. See docs/BOSS_SPAWNERS.md.
"""

import json
import math
import os

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
GEO = os.path.join(ROOT, "src", "main", "resources", "assets", "priestess", "geo", "entity")
GEO_BLOCK = os.path.join(ROOT, "src", "main", "resources", "assets", "priestess", "geo", "block")


def footprint(size):
    """Box-UV footprint of a cube: the cross is 2*(w+d) wide and h+d tall."""
    w, h, d = (int(math.ceil(v)) for v in size)
    return 2 * (w + d), h + d


def pack(cubes, sheet):
    """Shelf-pack every cube's UV cross into a `sheet`x`sheet` texture.

    Returns a list of (u, v) origins in the same order as `cubes`, or None if they do not
    all fit. Sorting by height first is what keeps the shelves from wasting space on a
    roster where a 10x10x10 head shares a sheet with a 2x12x2 arm.
    """
    order = sorted(range(len(cubes)), key=lambda i: -footprint(cubes[i][1])[1])
    placed = [None] * len(cubes)
    x = y = shelf_height = 0

    for i in order:
        w, h = footprint(cubes[i][1])
        if w > sheet:
            return None
        if x + w > sheet:            # shelf full, start the next one
            x = 0
            y += shelf_height
            shelf_height = 0
        if y + h > sheet:
            return None
        placed[i] = (x, y)
        x += w + 2                   # a 2px gutter, so bleeding at low mip levels is obvious
        shelf_height = max(shelf_height, h + 2)

    return placed


def build(name, bones, bounds):
    """Turn a bone spec into a .geo.json dict, choosing the smallest sheet that fits."""
    cubes = [c for bone in bones for c in bone[4]]

    for sheet in (64, 128, 256):
        uvs = pack(cubes, sheet)
        if uvs is not None:
            break
    else:
        raise SystemExit("%s: does not fit on a 256x256 sheet" % name)

    width, height, offset = bounds
    out_bones = []
    cursor = 0
    for bone_name, parent, pivot, rotation, bone_cubes in bones:
        entry = {"name": bone_name}
        if parent:
            entry["parent"] = parent
        entry["pivot"] = pivot
        if rotation:
            entry["rotation"] = rotation
        entry["cubes"] = [
            {"origin": origin, "size": size, "uv": list(uvs[cursor + n])}
            for n, (origin, size) in enumerate(bone_cubes)
        ]
        cursor += len(bone_cubes)
        out_bones.append(entry)

    return sheet, {
        "format_version": "1.12.0",
        "minecraft:geometry": [{
            "description": {
                "identifier": "geometry.%s" % name,
                "texture_width": sheet,
                "texture_height": sheet,
                "visible_bounds_width": width,
                "visible_bounds_height": height,
                "visible_bounds_offset": [0, offset, 0],
            },
            "bones": out_bones,
        }],
    }


# ── Body plans ────────────────────────────────────────────────────────────────
# Each entry is (id, visible_bounds, bones). A bone is
# (name, parent, pivot, rotation_or_None, [(origin, size), ...]).
#
# Every one of these has a bone literally named "head", because PriestessGeoRenderer
# turns on GeckoLib head tracking and that looks the bone up by name. Keep it when you
# redraw these in Blockbench.

def humanoid(body, head, arm, leg, arm_rot=None, body_rot=None, head_y=None):
    """The shared five-bone plan the Sal Viento roster is variations on.

    body/head/arm/leg are (size, y_bottom); the rest is derived so that limbs hang off
    the right corners without every entry restating the same arithmetic.
    """
    (bw, bh, bd), by = body
    (hw, hh, hd), hy = head
    (aw, ah, ad), ay = arm
    (lw, lh, ld), ly = leg
    hy = head_y if head_y is not None else hy

    return [
        ("body", None, [0, by, 0], body_rot,
         [([-bw / 2, by, -bd / 2], [bw, bh, bd])]),
        ("head", "body", [0, hy, 0], None,
         [([-hw / 2, hy, -hd / 2], [hw, hh, hd])]),
        ("arm_right", "body", [-bw / 2, ay + ah, 0], arm_rot,
         [([-bw / 2 - aw, ay, -ad / 2], [aw, ah, ad])]),
        ("arm_left", "body", [bw / 2, ay + ah, 0], arm_rot,
         [([bw / 2, ay, -ad / 2], [aw, ah, ad])]),
        ("leg_right", None, [-lw / 2, ly + lh, 0], None,
         [([-lw, ly, -ld / 2], [lw, lh, ld])]),
        ("leg_left", None, [lw / 2, ly + lh, 0], None,
         [([0, ly, -ld / 2], [lw, lh, ld])]),
    ]


ROSTER = [
    # Runner — leggy and pitched forward, so it reads as mid-sprint standing still.
    ("sv_runner", (2, 2.5, 1),
     humanoid(body=((6, 9, 4), 16), head=((6, 5, 7), 25), arm=((3, 9, 3), 16),
              leg=((3, 16, 3), 0), arm_rot=[-30, 0, 0], body_rot=[22, 0, 0])),

    # Spitter — nearly all head, on stubs. The maw is the silhouette.
    ("sv_spitter", (2, 2, 0.75),
     humanoid(body=((9, 7, 7), 5), head=((11, 9, 11), 12), arm=((3, 6, 3), 5),
              leg=((4, 5, 4), 0), arm_rot=[-15, 0, 0])),

    # Reaper — tall, thin, arms nearly to the floor.
    ("sv_reaper", (2.5, 3, 1.25),
     humanoid(body=((7, 14, 4), 18), head=((6, 6, 6), 32), arm=((3, 20, 3), 13),
              leg=((4, 18, 4), 0), arm_rot=[-8, 0, 0])),

    # Crawler — no legs to speak of; a long flat body close to the ground.
    ("sv_crawler", (2, 1, 0.4),
     [("body", None, [0, 2, 0], None, [([-5, 2, -8], [10, 5, 16])]),
      ("head", "body", [0, 2, -8], None, [([-4, 1, -13], [8, 6, 5])]),
      ("leg_right", None, [-5, 2, -4], [0, 0, 35], [([-9, 0, -5], [4, 3, 3])]),
      ("leg_left", None, [5, 2, -4], [0, 0, -35], [([5, 0, -5], [4, 3, 3])]),
      ("leg_right_back", None, [-5, 2, 4], [0, 0, 35], [([-9, 0, 3], [4, 3, 3])]),
      ("leg_left_back", None, [5, 2, 4], [0, 0, -35], [([5, 0, 3], [4, 3, 3])])]),

    # Piercer — narrow, with a spike where a face should be.
    ("sv_piercer", (2, 2.5, 1),
     humanoid(body=((6, 11, 4), 14), head=((5, 5, 5), 25), arm=((3, 11, 3), 13),
              leg=((3, 14, 3), 0)) +
     [("spike", "head", [0, 27, -2], None, [([-1, 26, -9], [2, 2, 7])])]),

    # The First to Talk — a big humanoid carrying a head far too large for it.
    ("sv_the_first_to_talk", (3, 3, 1.25),
     humanoid(body=((14, 16, 8), 18), head=((13, 12, 13), 34), arm=((5, 17, 5), 15),
              leg=((6, 18, 6), 0), arm_rot=[-12, 0, 0])),

    # Bishop Quintus — immobile, so it is built like architecture: a wide base, a column,
    # and a crown that overhangs it. No legs and no arms; nothing on it is meant to move.
    ("sv_bishop_quintus", (5, 4.5, 2),
     [("body", None, [0, 0, 0], None,
       [([-18, 0, -18], [36, 10, 36]),      # base
        ([-13, 10, -13], [26, 30, 26])]),   # column
      ("head", "body", [0, 40, 0], None,
       [([-16, 40, -16], [32, 14, 32])]),   # crown
      ("fin_right", "body", [-13, 22, 0], [0, 0, 18],
       [([-21, 18, -4], [8, 22, 8])]),
      ("fin_left", "body", [13, 22, 0], [0, 0, -18],
       [([13, 18, -4], [8, 22, 8])])]),
]


# ── Block models ──────────────────────────────────────────────────────────────
# Same format, different coordinate frame: the origin is the block's centre at floor
# level, so these run -8..8 on x and z and 0..16 on y. Anything outside that box hangs
# into the neighbouring block, which for an altar standing on its own is fine and in a
# corridor is not — so these stay inside it.

def altar(core_size, prong):
    """The shared boss-altar plan: a plinth, a waist, a rim, a floating core, four prongs.

    Deliberately built like a lectern rather than like a machine — the silhouette has to
    read as "put something here" from across a room, which means a wide flat base and one
    thing suspended above it. The core is its own root bone rather than a child of the
    rim so the renderer can spin it without dragging the prongs round with it.
    """
    half = core_size / 2.0
    return [
        # Plinth. Full 16-wide footprint, so the block still looks like it occupies its
        # square from directly above.
        ("base", None, [0, 0, 0], None,
         [([-8, 0, -8], [16, 2, 16])]),
        ("pillar", "base", [0, 2, 0], None,
         [([-5, 2, -5], [10, 6, 10])]),
        ("rim", "base", [0, 8, 0], None,
         [([-7, 8, -7], [14, 2, 14])]),
        # Pivot at the core's own centre, so setRotY spins it in place.
        ("core", None, [0, 10 + half, 0], None,
         [([-half, 10, -half], [core_size, core_size, core_size])]),
        # Splayed outward, so the rim reads as holding the core rather than fencing it.
        ("prong_nw", "rim", [-5.5, 10, -5.5], [0, 0, 12],
         [([-6.5, 10, -6.5], prong)]),
        ("prong_ne", "rim", [5.5, 10, -5.5], [0, 0, -12],
         [([3.5, 10, -6.5], prong)]),
        ("prong_sw", "rim", [-5.5, 10, 5.5], [0, 0, 12],
         [([-6.5, 10, 3.5], prong)]),
        ("prong_se", "rim", [5.5, 10, 5.5], [0, 0, -12],
         [([3.5, 10, 3.5], prong)]),
    ]


# ── Weapon VFX ────────────────────────────────────────────────────────────────
# Laevatain's three abilities, as geometry rather than particles. These are not creatures and
# do not follow the mob conventions above: there is no "head" bone (nothing tracks anything),
# and the model is built around the origin in the orientation the entity is spawned facing,
# because WeaponVfxRenderer rotates the whole thing by the entity's yaw and pitch.
#
# Each has exactly one bone, named for the file, and that bone name is what the matching
# animations/<name>.animation.json keyframes. Renaming a bone here silently stops the
# animation — GeckoLib logs nothing for keyframes that address a bone which is not there.

# laevatain_slash is NOT here, and must not be added. It is a single zero-thickness plane with
# per-face UV so the crescent can be drawn on the texture with transparency rather than built
# out of blocks — this generator only knows box UV, which a flat cube cannot use. It is
# hand-authored alongside its texture in generate_placeholder_art.py.

# The stab: a tapered spike running 80 units — five blocks — straight down +Z, which is the
# length of Molten Giant's box, so the mesh and the hitbox agree.
STAB = [
    ("stab", None, [0, 0, 0], None,
     [([-3, -3, 0], [6, 6, 26]),
      ([-2, -2, 26], [4, 4, 28]),
      ([-1, -1, 54], [2, 2, 26])]),
]

# The eruption: a central column with four smaller ones around it, all standing on y=0 so the
# bone can be scaled up from the floor rather than grown from its middle.
ERUPTION = [
    ("eruption", None, [0, 0, 0], None,
     [([-3, 0, -3], [6, 24, 6]),
      ([-11, 0, -2], [4, 16, 4]),
      ([7, 0, -2], [4, 16, 4]),
      ([-2, 0, -11], [4, 16, 4]),
      ([-2, 0, 7], [4, 16, 4])]),
]

VFX_ROSTER = [
    ("laevatain_stab", (6, 1, 0), STAB),
    ("laevatain_eruption", (2, 2, 1), ERUPTION),
]


BLOCK_ROSTER = [
    # One plan, both altars. They differ by texture rather than by geometry, exactly as the
    # 16x16 cube tiles they replace did — giving one its own silhouette is a matter of
    # adding an entry here and overriding BossSummonerBlock.modelName().
    ("boss_summoner", (1.5, 1.5, 0.5), altar(core_size=6, prong=[3, 5, 3])),
]


def emit(directory, roster):
    os.makedirs(directory, exist_ok=True)
    for name, bounds, bones in roster:
        sheet, model = build(name, bones, bounds)
        path = os.path.join(directory, "%s.geo.json" % name)
        with open(path, "w", encoding="utf-8") as handle:
            json.dump(model, handle, indent="\t")
            handle.write("\n")
        cubes = sum(len(b[4]) for b in bones)
        print("  %-24s %d bones, %d cubes, %dx%d"
              % (os.path.basename(path), len(bones), cubes, sheet, sheet))


def main():
    print("entity models ->")
    emit(GEO, ROSTER)
    print("weapon vfx models ->")
    emit(GEO, VFX_ROSTER)
    print("block models ->")
    emit(GEO_BLOCK, BLOCK_ROSTER)


if __name__ == "__main__":
    main()
