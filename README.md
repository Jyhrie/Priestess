# Priestess

A Minecraft Forge mod (MC 1.20.1 / Forge 47.4.10) adding **Terra** — a standalone
Overworld-like dimension with 13 custom biomes, custom noise-based terrain, and
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
└── world/dimension/
    ├── ModDimensions.java          dimension type + which biome goes where
    ├── ModBiomes.java              biome definitions (colors, temperature, mobs)
    ├── ModNoiseSettings.java       terrain shape + surface blocks
    └── ModStructures.java          structure declarations

src/main/resources/                 ← hand-authored assets (safe to edit)
├── assets/priestess/textures/block/*.png
└── data/priestess/structures/*.nbt

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

To then make terrain actually *use* the block, see "Surface rules" below.

---

## Biomes

A biome in this mod is three separate concerns, in three different files. Changing how
a biome *looks* is a different file from changing *where* it appears.

| I want to change… | Edit |
|---|---|
| Sky/fog/water colour, temperature, rain, mob spawns | `ModBiomes.java` |
| Where on the map the biome appears | `ModDimensions.bootstrapStem()` |
| Which blocks the ground is made of | `ModNoiseSettings.createSurfaceRules()` |

### Tuning an existing biome's settings

In `ModBiomes.bootstrap()` every biome is one line through the `blankBiome` helper:

```java
//                                    context, hasPrecipitation, temperature, downfall, waterColor
context.register(YANESE_PEAKS, blankBiome(context, true,  0.2F, 0.8F, 4159204));
```

- `hasPrecipitation` — whether weather falls here at all.
- `temperature` — **visual/behavioural** temperature: below `0.15` snow falls instead of
  rain and water freezes; `2.0` is desert-hot. This is *not* the same number as the
  climate `temperature` used for biome placement (below). They are independent.
- `downfall` — affects foliage/grass tint dryness.
- `waterColor` — packed RGB int, e.g. `0x3F76E4` = `4159204`.

For sky/fog colours, mood sounds or music, edit the `blankBiome` helper itself — it
builds the `BiomeSpecialEffects`. Right now every biome shares one sky colour
(`8103167`) and fog colour (`12638463`). If you want per-biome skies, add parameters to
`blankBiome` the same way `waterColor` is threaded through.

Mob spawns and features (trees, ores, flowers) are deliberately empty — `spawnBuilder`
and `generationBuilder` are built with nothing added, which is why Terra is bare. To add
ore or vegetation, populate `generationBuilder` with placed features; to add mobs, add
`spawnBuilder.addSpawn(MobCategory.MONSTER, new MobSpawnSettings.SpawnerData(...))`.

### Adding a new biome

1. **Declare a key** in `ModBiomes.java`:

   ```java
   public static final ResourceKey<Biome> ASHEN_FLATS = createKey("ashen_flats");
   ```

2. **Register its settings** in `ModBiomes.bootstrap()`:

   ```java
   context.register(ASHEN_FLATS, blankBiome(context, false, 1.4F, 0.1F, 4159204));
   ```

3. **Place it on the climate map** in `ModDimensions.bootstrapStem()`, in the
   `parameters` list:

   ```java
   Pair.of(Climate.parameters(0.4f, 0.0f, 0.35f, 0.0f, 0.0f, 0.0f, 0.0f),
           biomeRegistry.getOrThrow(ModBiomes.ASHEN_FLATS)),
   ```

   The seven arguments are, **in order**:

   | # | Parameter | Range | What it means here |
   |---|---|---|---|
   | 1 | `temperature` | −1…1 | hot vs cold |
   | 2 | `humidity` | −1…1 | **inert — see warning** |
   | 3 | `continentalness` | −1…1 | ocean floor (−1) → deep inland (1) |
   | 4 | `erosion` | −1…1 | low = mountainous, high = flat |
   | 5 | `depth` | −1…1 | surface (0) vs underground (1) |
   | 6 | `weirdness` | −1…1 | **inert — see warning** |
   | 7 | `offset` | 0…1 | a flat penalty; higher = harder to pick |

   The game picks whichever biome's parameter point is *nearest* to the terrain's actual
   noise values, so these are targets to aim at, not boundaries.

   > **Warning — humidity and weirdness do nothing in Terra.** In
   > `ModNoiseSettings.createNoiseRouter()` the `vegetation` (humidity) and `ridges`
   > (weirdness) channels are hardwired to `DensityFunctions.zero()`. The terrain
   > therefore always reports humidity = 0 and weirdness = 0. Setting a non-zero value
   > for either on a biome only ever *increases* its distance from every real point,
   > making it less likely to be chosen — never more. Leave both at `0.0f` unless you
   > first wire real noise into those two channels of the `NoiseRouter`.
   >
   > Only **temperature**, **continentalness**, **erosion** and **depth** currently carry
   > signal. Existing biomes are separated almost entirely on temperature and
   > continentalness.

4. **Give it a surface** (next section) — otherwise it generates as bare stone.

5. **Register it in the dimension is automatic**, but the biome JSON only appears if
   `ModBiomes::bootstrap` runs, which `ModWorldGenProvider.BUILDER` already wires up.
   Just run `./gradlew runData`.

### Surface rules — what the ground is made of

`ModNoiseSettings.createSurfaceRules()` returns one big ordered sequence. **First
matching rule wins**, so order matters:

1. bedrock floor (must stay first),
2. one branch per biome,
3. the global deepslate transition (must stay last — it's the fallback).

A branch looks like:

```java
SurfaceRules.ifTrue(SurfaceRules.isBiome(ModBiomes.ASHEN_FLATS),
        SurfaceRules.sequence(
                SurfaceRules.ifTrue(floor0, ash),        // depth 0        (the top block)
                SurfaceRules.ifTrue(floor3, coarseDirt), // depths 1–3
                SurfaceRules.ifTrue(floor8, basalt)      // depths 4–8
        )
),
```

The `floorN` helpers at the top of the method are `stoneDepthCheck(N, …)`, which matches
**every depth from 0 through N** — it is a *maximum*, not an exact depth. So:

- Order them shallowest-first, or a deep rule will swallow the shallow ones.
- `floor0` followed by `floor4` with the *same* block is redundant; `floor4` alone does it.
- You don't need a `floor0` rule at all if the next rule uses the block you want on top.

To make the surface patchy rather than uniform, wrap it in a noise condition. The
`patchHigh` / `patchMid` / `patchLow` helpers slice the `surface_patch` noise into three
bands, as Barrenlands, Foehn Hotlands and Kazdel Crags do:

```java
SurfaceRules.ifTrue(floor0, SurfaceRules.sequence(
        SurfaceRules.ifTrue(patchHigh, rootedDirt),
        SurfaceRules.ifTrue(patchMid,  cobblestone),
        SurfaceRules.ifTrue(patchLow,  coarseDirt)
)),
```

To use **your own mod block**, add a shorthand next to the others at the top of the
method:

```java
var kazdelBasalt = SurfaceRules.state(ModBlocks.KAZDEL_BASALT.get().defaultBlockState());
```

### Terrain shape

Height, cliffs and continent shape come from `createNoiseRouter()` and the noise
parameters in `bootstrapNoise()`. `NoiseSettings.create(-64, 384, 1, 2)` sets the world
height, and `sea_level` is `124` in `bootstrap()`. This is the most delicate part of the
mod — change one spline point at a time and regenerate, because the interaction between
`continents`, `erosion` and the y-gradient is not intuitive.

Note that `aquifers_enabled` and `ore_veins_enabled` are both `false`, so there are no
underground lakes or vanilla ore veins in Terra.

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
