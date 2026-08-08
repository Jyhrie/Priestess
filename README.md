# Priestess

A Minecraft Forge mod (MC 1.20.1 / Forge 47.4.10) adding **Terra** — a standalone
Overworld-like dimension with 22 custom biomes across a hand-authored map of Terra, and
jigsaw structures.

| | |
|---|---|
| Mod ID | `priestess` |
| Dimension | `priestess:terra` |
| Base package | `com.jyhrie.priestess` |
| Getting in | `/execute in priestess:terra run tp @s ~ ~ ~` — there is no portal yet |

---

## The one thing to understand first

**Almost nothing in this mod is registered at runtime.** The dimension, biomes,
noise settings and structures are *datapack JSON* that gets generated from the Java
in `world/dimension/` by a build task:

```
edit Java in world/dimension/  ──►  ./gradlew runData  ──►  src/generated/resources/**.json  ──►  loaded by the game
```

So the workflow for any worldgen change is:

1. Edit the Java.
2. Run `./gradlew runData`.
3. *Then* run `./gradlew runClient`.

If you skip step 2, the game loads the **old** JSON and your change silently does nothing.

Two rules that follow from this:

- **Never hand-edit anything under `src/generated/resources/`.** `runData` overwrites it.
- **Worldgen changes need a fresh world.** Already-generated chunks keep their old
  terrain and biomes forever. Delete the test world in `run/saves/` and make a new one.

Only items, blocks and the creative tab are registered in code at runtime (via
`DeferredRegister` in `Priestess.java`).

---

## Project layout

```
src/main/java/com/jyhrie/priestess/
├── Priestess.java                  main mod class, MOD_ID, runtime registration
├── block/ModBlocks.java            block registry (auto-registers BlockItems too)
├── item/
│   ├── ModItems.java               item registry
│   └── ModCreativeTabs.java        the "Priestess" creative tab
├── datagen/                        ← turns the Java below into JSON
│   ├── DataGenerators.java         wires up every provider
│   ├── ModBlockStateProvider.java  blockstates + block models
│   ├── ModItemModelProvider.java   item models
│   ├── ModLanguageProvider.java    en_us.json
│   ├── ModLootTableProvider.java   block drops
│   └── ModWorldGenProvider.java    registry bootstrap order for all worldgen
├── world/dimension/
│   ├── ModDimensions.java          dimension type + chunk generator wiring
│   ├── ModBiomes.java              biome definitions (colors, temperature, mobs)
│   ├── ModNoiseSettings.java       terrain shape + surface blocks
│   └── ModStructures.java          structure declarations
└── world/terra/                    ← the map: where everything actually is
    ├── TerraRegion.java            every region, its map colour, its 8 biomes
    ├── TerraSlot.java              the 8 terrain classes and their elevations
    ├── TerraMap.java               loads the PNGs, warps and samples them
    ├── TerraMapBiomeSource.java    the BiomeSource that reads the map
    ├── TerraElevationFunction.java exposes map elevation to the terrain splines
    └── TerraMapPreview.java        renders docs/terra_world_preview.png

src/main/resources/                 ← hand-authored assets (safe to edit)
├── assets/priestess/textures/block/*.png
├── data/priestess/structures/*.nbt
└── data/priestess/terra/
    ├── regions.png                 ← THE MAP. one flat colour per region
    └── elevation.png               ← greyscale height, 0 = abyss, 255 = peak

tools/generate_terra_map.py         regenerates those two PNGs from a layout
docs/terra_map_preview.png          shaded political map, for humans
docs/terra_world_preview.png        what the generator will actually produce

src/generated/resources/            ← GENERATED, do not edit
```

---

## Adding an item

1. **Register it** in `item/ModItems.java`:

   ```java
   public static final RegistryObject<Item> JOKLUM_CRYSTAL = ITEMS.register("joklum_crystal",
           () -> new Item(new Item.Properties()));
   ```

   (You'll need `import net.minecraftforge.registries.RegistryObject;` back.)

2. **Add the texture** at `src/main/resources/assets/priestess/textures/item/joklum_crystal.png`
   (16×16).

3. **Give it a model** in `datagen/ModItemModelProvider.java`:

   ```java
   basicItem(ModItems.JOKLUM_CRYSTAL.get());
   ```

4. **Name it** in `datagen/ModLanguageProvider.java`:

   ```java
   add(ModItems.JOKLUM_CRYSTAL.get(), "Joklum Crystal");
   ```

5. `./gradlew runData`

It appears in the Priestess creative tab automatically — the tab iterates
`ModItems.ITEMS`, so there is no list to keep in sync.

---

## Adding a block

Blocks are a little more work than items because a block needs a blockstate, a model,
a loot table, and a matching item.

1. **Register it** in `block/ModBlocks.java` using the `registerBlock` helper — it
   registers the `BlockItem` for you:

   ```java
   public static final RegistryObject<Block> KAZDEL_BASALT = registerBlock("kazdel_basalt",
           () -> new Block(BlockBehaviour.Properties.copy(Blocks.BASALT)));
   ```

   `BlockBehaviour.Properties.copy(...)` inherits hardness, tool, sound and blast
   resistance from a vanilla block. Pick a vanilla block that behaves the way you want.

   Use a specific block class when you want vanilla behaviour — e.g. `IBERIAN_SAND` is a
   `SandBlock` so it falls under gravity:

   ```java
   () -> new SandBlock(0xE6C280, BlockBehaviour.Properties.copy(Blocks.SAND))
   //                  ^ dust colour shown when it falls
   ```

2. **Add the texture** at `src/main/resources/assets/priestess/textures/block/kazdel_basalt.png`.

3. **Blockstate + model** in `datagen/ModBlockStateProvider.java`:

   ```java
   simpleBlockWithItem(ModBlocks.KAZDEL_BASALT.get(), cubeAll(ModBlocks.KAZDEL_BASALT.get()));
   ```

   `cubeAll` uses the same texture on all six faces and expects it at the path in step 2.

4. **Loot table** in `datagen/ModLootTableProvider.java` → `BlockLoot.generate()`:

   ```java
   this.dropSelf(ModBlocks.KAZDEL_BASALT.get());
   ```

   **This is not optional.** `getKnownBlocks()` reports every registered block, and
   datagen *fails* if a known block has no loot table. If you get a datagen error
   naming your new block, this is the step you missed.

5. **Name it** in `datagen/ModLanguageProvider.java`:

   ```java
   add(ModBlocks.KAZDEL_BASALT.get(), "Kazdel Basalt");
   ```

6. `./gradlew runData`

To then make terrain actually *use* the block, see
[Surface rules](docs/WORLDGEN.md#surface-rules).

---

## Worldgen

**Terra's geography is a hand-authored map, not climate noise.** Two PNGs in
`src/main/resources/data/priestess/terra/` decide where everything is: `regions.png`
(one flat colour per region) and `elevation.png` (greyscale height). A custom
`BiomeSource` reads the region, elevation picks one of eight terrain slots, and the pair
picks the biome. The same elevation drives the terrain height splines, which is what
keeps beaches at sea level instead of halfway up a mountain.

![Terra](docs/terra_map_preview.png)

### Scale, origin and height

| | Value | Defined in |
|---|---|---|
| **World width** | 131,072 blocks | `TerraMap.WORLD_WIDTH_BLOCKS` — **the scale knob** |
| **Map resolution** | 1024 × 640 px | `generate_terra_map.py:63`; Java reads it from the PNG |
| **Blocks per pixel** | 128 | derived: world width ÷ image width |
| **World size** | 131,072 × 81,920 blocks | derived: height follows the image aspect |
| **X / Z range** | −65,536 → +65,536 / −40,960 → +40,960 | derived |
| **Origin (0, 0)** | pixel (512, 320), in Kazimierz — this is spawn | derived: the map is centred |
| **Origin shift** | none | `ORIGIN_AT_BLOCK_X/Z`, `TerraMap.java:76` — paste a region's coordinates in to spawn there |
| **Lowest ground** | y 28 | first knot of `mapHeight`, `ModNoiseSettings.java:135` |
| **Highest ground** | y 244 base, **y 305 with relief** | last knot of `mapHeight` + `ruggedness` |
| **Sea level** | y 124 | `ModNoiseSettings.java:100` |
| **World floor / ceiling** | y −64 / y 320 | `NoiseSettings.create(-64, 384, …)` |
| **Off the N / S edge** | Infy Icefield / Foehn Hotlands, forever | the map's own top and bottom rows |
| **Off the E / W edge** | open ocean, forever | the map's own left and right columns |
| **Seed-dependent?** | No. Terra is the same in every world. | — |

Grey value in `elevation.png` becomes a world height like this:

```
grey 0-255 ──/255──► elevation 0..1 ──×2−1──► density −1..1 ──mapHeight──► terrainHeight
                                                                                │
                                                     surfaceY = 128 + 128 × terrainHeight
```

So a `mapHeight` knot reads `y = 128 + 128 × value`, and only ~15 blocks of headroom are
left above the tallest peaks. Details and the full "if you want to change X" table are in
[docs/WORLDGEN.md](docs/WORLDGEN.md#scale-origin-and-height).

**→ Full reference: [docs/WORLDGEN.md](docs/WORLDGEN.md)** — how the PNGs are read, the
terrain slot table with the grey values to paint, adding regions and biomes, surface
rules, terrain splines, and troubleshooting.

Quick pointers:

| I want to change… | Edit |
|---|---|
| Where a region physically sits | `data/priestess/terra/regions.png` |
| How high the ground is | `data/priestess/terra/elevation.png` |
| Which biome a region wears at a given height | `world/terra/TerraRegion.java` |
| Sky/fog/water colour, temperature, rain, mob spawns | `world/dimension/ModBiomes.java` |
| Which blocks the ground is made of | `ModNoiseSettings.createSurfaceRules()` |
| The whole map layout, from scratch | `tools/generate_terra_map.py` |

After any worldgen change: `./gradlew runData`, then check
`docs/terra_world_preview.png` and the datagen log — the log prints every region's share
of the world, a teleport coordinate for each, and shouts about any region that came out
unreachable. **Worldgen changes need a fresh world**; delete the test world in
`run/saves/`.

---

## Adding a structure

Structures are declared **once** in the CONFIGURATION BLOCK of `ModStructures.java`. The
three bootstrap methods derive the template pool, the structure and the structure set
from that single declaration, so you never touch them.

1. **Build it in-game** with structure blocks, save it, then copy the `.nbt` from
   `run/saves/<world>/generated/minecraft/structures/` to
   `src/main/resources/data/priestess/structures/my_structure.nbt`.

2. **Declare it** in the static block:

   ```java
   registerStructure(
           "kazdel_spire",                     // structure id -> priestess:kazdel_spire
           List.of("kazdel_spire_a",           // .nbt file names, chosen at random,
                   "kazdel_spire_b"),          //   equal weight
           ModBiomes.KAZDEL_CRAGS,             // biome it spawns in
           24,                                 // spacing: avg chunks between attempts
           8,                                  // separation: min chunks apart (< spacing!)
           78341265,                           // salt: any int, UNIQUE per structure
           UniformHeight.of(VerticalAnchor.absolute(-12), VerticalAnchor.absolute(-4)),
           true                                // project onto the heightmap
   );
   ```

3. `./gradlew runData`

Notes on the fields:

- **`spacing` / `separation`** control density. `separation` **must** be less than
  `spacing` or the game throws on load. Lower spacing = more common. The existing ice
  spike uses `spacing = 1, separation = 0`, which is "attempt in every chunk" — very
  dense, good for scatter decoration, bad for landmarks.
- **`salt`** must be unique across structures. Two structures sharing a salt will try to
  generate at exactly the same chunk coordinates.
- **`heightProvider`** is the vertical offset applied *relative to* the heightmap when
  `projectToHeightmap` is true. The ice spike's `-12 … -4` sinks it into the ground so
  its base isn't left floating.
- **NBT naming**: the entry `"kazdel_spire_a"` resolves to
  `data/priestess/structures/kazdel_spire_a.nbt`. A typo here produces a missing-template
  error at worldgen time, not at datagen time.
- The structure targets exactly one biome. For several biomes, change
  `HolderSet.direct(biomes.getOrThrow(data.targetBiome()))` in `bootstrapStructures` to
  take a list.

---

## Testing

```bash
./gradlew runData      # regenerate JSON  — after ANY worldgen change
./gradlew runClient    # launch the game
```

Useful in-game commands (creative + cheats):

```
/execute in priestess:terra run tp @s ~ ~ ~     enter the dimension
/execute in minecraft:overworld run tp @s ~ ~ ~ leave it
/locate biome priestess:infy_icefields          find a biome
/place structure priestess:infy_ice_spike       force-place a structure here
```

If a change doesn't show up, check in this order:

1. Did you run `runData`? Look at the file under `src/generated/resources/` and confirm
   your change is in the JSON.
2. Are you in a **new** world? Old chunks never regenerate.
3. For structures, is the `.nbt` filename exactly right?

## Build

```bash
./gradlew build        # -> build/libs/priestess-1.0.0.jar
```

Mod metadata (id, name, version, description) lives in `gradle.properties`;
`mods.toml` reads it via `${...}` substitution, so edit `gradle.properties`, not
`mods.toml`.
