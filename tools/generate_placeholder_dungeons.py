#!/usr/bin/env python3
"""Write the three Columbia dungeons as structure NBT.

    python tools/generate_placeholder_dungeons.py

    -> src/main/resources/data/priestess/structures/mansfield_state_prison.nbt
                                                  /dorothys_vision.nbt
                                                  /rhine_lab_hq.nbt

These are placeholders in the strict sense: they have the right *shape*, the right
population and the right loot in the right room, and no craft whatsoever. Mansfield is a
long dark cell block with a warden's office at the end of it; Dorothy's Vision is a buried
chamber with test cells around a boss pit; Rhine Lab is a tower you climb. That is enough
to walk the chapter end to end and find out whether the pacing works, which is the only
question a placeholder is allowed to answer.

When somebody builds the real thing in-game with structure blocks, drop the exported .nbt
over the file this writes, delete that dungeon's entry from BUILDERS below, and nothing
else in the mod has to change — the declarations in ModStructures.java name the file, not
the contents.

Why generated rather than hand-built: the alternative to a script is three files nobody
can diff, review or adjust by two blocks without opening Minecraft. This way the layout is
readable, and "make the prison longer" is one number.

Pure standard library — it writes gzipped NBT directly, which is a couple of dozen lines
and saves a dependency.
"""

import gzip
import os
import struct

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
OUT_DIR = os.path.join(ROOT, "src", "main", "resources", "data", "priestess", "structures")

# 1.20.1. Written into the file so the game does not run its data fixers over it.
DATA_VERSION = 3465


# ── Minimal NBT writer ────────────────────────────────────────────────────────
# Only the six tag types a structure file needs. Values are tagged by Python type where
# that is unambiguous and by a wrapper class where it is not (Int vs Double vs Byte).

class Byte(int):
    pass


class Int(int):
    pass


class Double(float):
    pass


def _write_string(out, value):
    encoded = value.encode("utf-8")
    out += struct.pack(">H", len(encoded))
    out += encoded


def _tag_id(value):
    if isinstance(value, Byte):
        return 1
    if isinstance(value, Int):
        return 3
    if isinstance(value, Double):
        return 6
    if isinstance(value, str):
        return 8
    if isinstance(value, list):
        return 9
    if isinstance(value, dict):
        return 10
    raise TypeError("no NBT tag for %r (%s) — wrap it in Int/Double/Byte"
                    % (value, type(value).__name__))


def _write_payload(out, value):
    if isinstance(value, Byte):
        out += struct.pack(">b", int(value))
    elif isinstance(value, Int):
        out += struct.pack(">i", int(value))
    elif isinstance(value, Double):
        out += struct.pack(">d", float(value))
    elif isinstance(value, str):
        _write_string(out, value)
    elif isinstance(value, list):
        # An empty list is TAG_End-typed; a populated one must be homogeneous, which is a
        # rule the format enforces and a mistake worth catching here rather than at load.
        element_type = _tag_id(value[0]) if value else 0
        for element in value:
            if _tag_id(element) != element_type:
                raise TypeError("NBT lists must be homogeneous, found %r" % (element,))
        out += struct.pack(">bi", element_type, len(value))
        for element in value:
            _write_payload(out, element)
    elif isinstance(value, dict):
        for key, element in value.items():
            out += struct.pack(">b", _tag_id(element))
            _write_string(out, key)
            _write_payload(out, element)
        out += b"\x00"
    else:
        raise TypeError("no NBT payload writer for %r" % (value,))


def write_nbt(path, root):
    out = bytearray()
    out += struct.pack(">b", 10)
    _write_string(out, "")
    _write_payload(out, root)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    # mtime=0 so re-running produces a byte-identical file and does not churn git.
    with gzip.GzipFile(path, "wb", compresslevel=9, mtime=0) as handle:
        handle.write(bytes(out))
    print("  %s  (%d bytes)" % (os.path.relpath(path, ROOT).replace("\\", "/"),
                                os.path.getsize(path)))


# ── A voxel canvas ────────────────────────────────────────────────────────────

class Structure:
    """A box of blocks that knows how to serialise itself as a structure template.

    Positions not written are left as they were, which for a structure placed on terrain
    means "the hillside stays". That is almost never what you want inside a building, so
    every builder below explicitly hollows its interior with air.
    """

    def __init__(self, width, height, depth):
        self.width = width
        self.height = height
        self.depth = depth
        self.blocks = {}
        self.entities = []
        self.palette = []
        self.palette_index = {}

    def state(self, name, properties=None):
        key = (name, tuple(sorted((properties or {}).items())))
        if key not in self.palette_index:
            self.palette_index[key] = len(self.palette)
            entry = {"Name": name}
            if properties:
                entry["Properties"] = dict(properties)
            self.palette.append(entry)
        return self.palette_index[key]

    def set(self, x, y, z, name, properties=None, nbt=None):
        if not (0 <= x < self.width and 0 <= y < self.height and 0 <= z < self.depth):
            return
        self.blocks[(x, y, z)] = (self.state(name, properties), nbt)

    def fill(self, x0, y0, z0, x1, y1, z1, name, properties=None):
        for x in range(x0, x1 + 1):
            for y in range(y0, y1 + 1):
                for z in range(z0, z1 + 1):
                    self.set(x, y, z, name, properties)

    def box(self, x0, y0, z0, x1, y1, z1, name, properties=None):
        """Shell only — the six faces, nothing inside."""
        for x in range(x0, x1 + 1):
            for y in range(y0, y1 + 1):
                for z in range(z0, z1 + 1):
                    on_face = (x in (x0, x1)) or (y in (y0, y1)) or (z in (z0, z1))
                    if on_face:
                        self.set(x, y, z, name, properties)

    def chest(self, x, y, z, facing, loot_table):
        self.set(x, y, z, "minecraft:chest", {"facing": facing, "type": "single",
                                              "waterlogged": "false"},
                 {"id": "minecraft:chest", "LootTable": loot_table})

    def mob(self, x, y, z, entity_id, persistent=True):
        nbt = {"id": entity_id,
               "Motion": [Double(0.0), Double(0.0), Double(0.0)],
               "Rotation": [Double(0.0), Double(0.0)]}
        if persistent:
            # Without this the tower's garrison quietly despawns before anyone climbs it.
            nbt["PersistenceRequired"] = Byte(1)
        self.entities.append({
            "blockPos": [Int(x), Int(y), Int(z)],
            "pos": [Double(x + 0.5), Double(y), Double(z + 0.5)],
            "nbt": nbt,
        })

    def to_nbt(self):
        blocks = []
        for (x, y, z), (state, nbt) in sorted(self.blocks.items()):
            entry = {"state": Int(state), "pos": [Int(x), Int(y), Int(z)]}
            if nbt:
                converted = dict(nbt)
                entry["nbt"] = converted
            blocks.append(entry)
        return {
            "DataVersion": Int(DATA_VERSION),
            "size": [Int(self.width), Int(self.height), Int(self.depth)],
            "palette": self.palette,
            "blocks": blocks,
            "entities": self.entities,
        }


# Vanilla loot, so the chests are not empty while the mod has nothing of its own to put in
# them. The one exception is the Director's Office, which holds the chapter's payoff.
DUNGEON_LOOT = "minecraft:chests/simple_dungeon"
LAB_LOOT = "minecraft:chests/stronghold_library"
RHINE_LOOT = "minecraft:chests/stronghold_corridor"
DIRECTORS_OFFICE_LOOT = "priestess:chests/rhine_directors_office"


# ── Mansfield State Prison ────────────────────────────────────────────────────

def mansfield():
    """A crashed mobile penitentiary: one long corridor, cells down both sides, and the
    warden's office sealed at the far end with Jesselton in it.

    Deliberately unlit. The GDD calls for pitch black and claustrophobic, and a placeholder
    that is convenient to walk through would be answering a different question than the one
    the dungeon is for.
    """
    width, height, depth = 15, 8, 45
    s = Structure(width, height, depth)

    # Hull, then hollow it out.
    s.box(0, 0, 0, width - 1, height - 1, depth - 1, "minecraft:deepslate_bricks")
    s.fill(1, 1, 1, width - 2, height - 2, depth - 2, "minecraft:air")
    s.fill(1, 0, 1, width - 2, 0, depth - 2, "minecraft:polished_deepslate")

    # Age it. Every ninth block on the hull goes cracked, which is enough to stop the walls
    # reading as one flat texture without needing noise.
    for z in range(0, depth, 3):
        for y in range(1, height - 1, 2):
            s.set(0, y, z, "minecraft:cracked_deepslate_bricks")
            s.set(width - 1, y, z, "minecraft:cracked_deepslate_bricks")

    # Cell blocks: 3x3 cells down both walls, barred onto the corridor.
    bars_ew = {"north": "false", "south": "false", "east": "true", "west": "true",
               "waterlogged": "false"}
    for z in range(2, depth - 12, 5):
        for side in (0, 1):
            x_wall = 1 if side == 0 else width - 2
            x_bars = 4 if side == 0 else width - 5
            # Cell interior
            s.fill(min(x_wall, x_bars), 1, z, max(x_wall, x_bars), 3, z + 2, "minecraft:air")
            # Divider between this cell and the next
            s.fill(min(x_wall, x_bars), 1, z + 3, max(x_wall, x_bars), height - 2, z + 3,
                   "minecraft:deepslate_bricks")
            # The bars, with a gap at head height for a door that has long since gone
            for y in range(1, 4):
                for dz in range(0, 3):
                    s.set(x_bars, y, z + dz, "minecraft:iron_bars", bars_ew)
            s.set(x_bars, 1, z + 1, "minecraft:air")
            s.set(x_bars, 2, z + 1, "minecraft:air")

    # Warden's office: the last five metres, behind a wall, with the boss in it.
    office_z = depth - 9
    s.fill(1, 1, office_z, width - 2, height - 2, office_z, "minecraft:deepslate_tiles")
    s.fill(6, 1, office_z, 8, 3, office_z, "minecraft:air")
    s.fill(1, 0, office_z + 1, width - 2, 0, depth - 2, "minecraft:polished_deepslate")
    s.chest(2, 1, depth - 3, "south", DUNGEON_LOOT)
    s.chest(width - 3, 1, depth - 3, "south", DUNGEON_LOOT)
    s.mob(width // 2, 1, depth - 4, "priestess:jesseltons_shadow")

    # A way in at the near end, at ground level.
    s.fill(6, 1, 0, 8, 3, 0, "minecraft:air")

    return s


# ── Dorothy's Vision (Pioneer Labs) ───────────────────────────────────────────

def dorothys_vision():
    """A buried lab: a ring of glass test chambers around a central pit with the Failed
    Vision rooted in the floor of it.

    Lit, unlike Mansfield — flickering neon is the brief, and a lab you cannot see the
    walls of is just Mansfield again.

    The top two thirds of the box is nothing but the access shaft. The lab is placed deep
    enough to be genuinely buried, which means it needs to carry its own way down with it:
    blocks a structure does not write are left as whatever the hillside already was, so a
    shaft that is not in the .nbt is a shaft full of stone.
    """
    width, depth = 33, 33
    lab_height = 12
    height = 32
    s = Structure(width, height, depth)

    s.box(0, 0, 0, width - 1, lab_height - 1, depth - 1, "minecraft:tuff")
    s.fill(1, 1, 1, width - 2, lab_height - 2, depth - 2, "minecraft:air")
    s.fill(1, 0, 1, width - 2, 0, depth - 2, "minecraft:polished_blackstone_bricks")

    # Overgrowth: the synthetic flesh that took the place over.
    for x in range(2, width - 2, 4):
        for z in range(2, depth - 2, 4):
            s.set(x, 0, z, "minecraft:sculk")
            s.set(x + 1, 0, z + 1, "minecraft:sculk")
    # ...and the Originium coming through the floor with it.
    for x in range(4, width - 4, 7):
        for z in range(4, depth - 4, 7):
            s.set(x, 1, z, "minecraft:amethyst_cluster",
                  {"facing": "up", "waterlogged": "false"})

    # Four test chambers in the corners, glass onto the floor so you can see in.
    for corner_x, corner_z in ((1, 1), (width - 8, 1), (1, depth - 8), (width - 8, depth - 8)):
        s.box(corner_x, 1, corner_z, corner_x + 6, 5, corner_z + 6,
              "minecraft:light_blue_stained_glass")
        s.fill(corner_x + 1, 1, corner_z + 1, corner_x + 5, 4, corner_z + 5, "minecraft:air")
        s.set(corner_x + 3, 1, corner_z, "minecraft:air")
        s.set(corner_x + 3, 2, corner_z, "minecraft:air")
        s.set(corner_x + 3, 5, corner_z + 3, "minecraft:glowstone")
        # Two per chamber; the boss adds more once the fight starts.
        s.mob(corner_x + 3, 1, corner_z + 3, "priestess:frank")

    # The boss pit, dropped two blocks so the mass reads as grown into the floor.
    centre = width // 2
    s.fill(centre - 4, 0, centre - 4, centre + 4, 1, centre + 4, "minecraft:air")
    s.fill(centre - 5, 0, centre - 5, centre + 5, 0, centre + 5, "minecraft:sculk")
    s.mob(centre, 1, centre, "priestess:failed_vision")

    # Neon strip down the middle of the ceiling.
    for z in range(2, depth - 2, 2):
        s.set(centre, lab_height - 2, z, "minecraft:sea_lantern")

    s.chest(2, 1, centre, "east", LAB_LOOT)
    s.chest(width - 3, 1, centre, "west", LAB_LOOT)

    # The shaft: a 3x3 tuff chimney from the lab ceiling to the top of the box, with a
    # ladder up one wall. The structure is placed so the last course clears the terrain,
    # which is the only thing on the surface that says the lab is down there at all.
    shaft_z = 3
    s.fill(centre - 1, lab_height - 1, shaft_z - 1, centre + 1, height - 1, shaft_z + 1,
           "minecraft:tuff")
    s.fill(centre, 1, shaft_z, centre, height - 1, shaft_z, "minecraft:air")
    # The ladder sits in the shaft column itself, not in the wall — a ladder's `facing` is
    # the way it points, so it hangs off the block behind it, one step north.
    for y in range(1, height):
        s.set(centre, y, shaft_z, "minecraft:ladder",
              {"facing": "south", "waterlogged": "false"})

    return s


# ── Rhine Lab Headquarters ────────────────────────────────────────────────────

def rhine_lab_hq():
    """A tower with eight floors and a ladder up the spine, garrisoned all the way up, and
    the Director's Office at the top holding the blueprint.

    No boss. The GDD is explicit that the last dungeon is a climb rather than a fight, and
    what stands in the way is the pairing of armoured bruisers with drones that strip your
    armour off — so the population is what makes this dungeon, not a room at the top.
    """
    width, depth = 17, 17
    floors = 8
    floor_height = 6
    height = floors * floor_height + 1
    s = Structure(width, height, depth)

    s.box(0, 0, 0, width - 1, height - 1, depth - 1, "minecraft:smooth_quartz")
    s.fill(1, 1, 1, width - 2, height - 2, depth - 2, "minecraft:air")

    for floor in range(floors):
        base = floor * floor_height

        # Slab floor for every storey above the ground one.
        if floor > 0:
            s.fill(1, base, 1, width - 2, base, depth - 2, "minecraft:quartz_block")

        # Window band, one course below the ceiling, on all four walls.
        window_y = base + 3
        for i in range(2, width - 2):
            s.set(i, window_y, 0, "minecraft:light_blue_stained_glass")
            s.set(i, window_y, depth - 1, "minecraft:light_blue_stained_glass")
            s.set(0, window_y, i, "minecraft:light_blue_stained_glass")
            s.set(width - 1, window_y, i, "minecraft:light_blue_stained_glass")

        # Structural columns, so a floor plate is not a featureless square.
        for cx, cz in ((4, 4), (width - 5, 4), (4, depth - 5), (width - 5, depth - 5)):
            s.fill(cx, base + 1, cz, cx, base + floor_height - 1, cz, "minecraft:iron_block")

        s.set(width // 2, base + floor_height - 1, width // 2, "minecraft:sea_lantern")

        # The spine: a hole through each floor plate with a ladder in it.
        if floor > 0:
            s.fill(width - 4, base, depth - 4, width - 3, base, depth - 3, "minecraft:air")
        for y in range(base + 1, base + floor_height + 1):
            s.set(width - 4, y, depth - 4, "minecraft:ladder",
                  {"facing": "west", "waterlogged": "false"})

        # Garrison. Armour on the floor, drones in the air above it — the two halves of the
        # dungeon's one idea, on every storey.
        if floor < floors - 1:
            s.mob(3, base + 1, 3, "priestess:rogue_power_armour")
            if floor % 2 == 0:
                s.mob(width - 4, base + 3, 4, "priestess:rhine_security_drone")
            if floor % 3 == 0:
                s.chest(2, base + 1, depth - 3, "north", RHINE_LOOT)

    # Ground floor way in.
    s.fill(width // 2 - 1, 1, 0, width // 2 + 1, 3, 0, "minecraft:air")

    # The Director's Office: the top storey, and the only chest in the chapter that matters.
    top = (floors - 1) * floor_height
    s.fill(2, top + 1, 2, width - 3, top + 1, depth - 3, "minecraft:polished_deepslate")
    s.chest(width // 2, top + 2, 3, "south", DIRECTORS_OFFICE_LOOT)
    s.mob(width // 2 - 3, top + 2, depth // 2, "priestess:rogue_power_armour")
    s.mob(width // 2 + 3, top + 4, depth // 2, "priestess:rhine_security_drone")

    return s


BUILDERS = {
    "mansfield_state_prison": mansfield,
    "dorothys_vision": dorothys_vision,
    "rhine_lab_hq": rhine_lab_hq,
}


def main():
    print("dungeon placeholders ->")
    for name, builder in BUILDERS.items():
        structure = builder()
        write_nbt(os.path.join(OUT_DIR, "%s.nbt" % name), structure.to_nbt())
        print("      %d x %d x %d, %d blocks, %d entities, %d palette entries"
              % (structure.width, structure.height, structure.depth,
                 len(structure.blocks), len(structure.entities), len(structure.palette)))


if __name__ == "__main__":
    main()
