# Worldgen

Everything about how Terra is generated: what decides where you are, how the map PNGs
are read, and how to add regions and biomes.

For items, blocks and structures see the main [README](../README.md).

---

## Contents

- [The model](#the-model) — why there is no climate noise
- [Scale, origin and height](#scale-origin-and-height) — **the constants, and where they live**
- [How the PNGs are read](#how-the-pngs-are-read) — coordinates, colours, warping
- [Terrain slots](#terrain-slots) — elevation to terrain class
- [Adding a biome](#adding-a-biome)
- [Adding a region](#adding-a-region)
- [Worked example: terra incognita](#worked-example-terra-incognita-north-of-infy)
- [Surface rules](#surface-rules)
- [Terrain shape](#terrain-shape)
- [Verifying and troubleshooting](#verifying-and-troubleshooting)

---

## The model

**There is no climate model in this mod.** Biomes are not chosen by temperature and
humidity noise. They are read off a hand-authored map.

```
data/priestess/terra/regions.png  ──┐
                                    ├─► TerraMapBiomeSource ─ (region, slot) ─► biome
data/priestess/terra/elevation.png ─┤
                                    └─► TerraElevationFunction ─► terrain height splines
```

Both consumers read the **same** elevation. That is the whole reason it works: a
map-chosen biome with a noise-chosen height would put Iberian beaches halfway up a
mountain.

Why a map at all — Terra has a real geography. Ægir is in the ocean south of Iberia and
at war with it. The Foehn Hotlands are south of Sargon, the Infy Icefield is north of
Sami and Ursus, and both border terra incognita. A mountain range runs from northern
Kazimierz through the basin that forms Kjerag to the Sargonian desert. Multi-noise
cannot express any of that. It says "Iberia is wherever it is cold and coastal", which
gives you infinitely many Iberias and no range that crosses a border.

The trade is that **Terra is finite and identical in every world** — see
[Scale, origin and height](#scale-origin-and-height) for the numbers.

### The files

| File | What it is |
|---|---|
| `src/main/resources/data/priestess/terra/regions.png` | **The map.** One flat colour per region |
| `src/main/resources/data/priestess/terra/elevation.png` | Greyscale height, 0 = abyss, 255 = peak |
| `world/terra/TerraRegion.java` | Every region: its map colour and its eight biomes |
| `world/terra/TerraSlot.java` | The eight terrain classes and their elevation thresholds |
| `world/terra/TerraMap.java` | Loads, warps and samples the PNGs |
| `world/terra/TerraMapBiomeSource.java` | The `BiomeSource` |
| `world/terra/TerraElevationFunction.java` | Feeds elevation to the terrain splines |
| `world/dimension/ModBiomes.java` | What each biome looks like |
| `world/dimension/ModNoiseSettings.java` | Terrain splines + surface blocks |
| `tools/generate_terra_map.py` | Regenerates both PNGs from a described layout |

---

## Scale, origin and height

Three numbers define how big Terra is, where its centre is, and how high the ground
goes. This section is the one place they are written down together.

### The numbers

| | Value | Defined in |
|---|---|---|
| **World width** | 131,072 blocks | `TerraMap.WORLD_WIDTH_BLOCKS` — **the scale knob** |
| **Map resolution** | 1024 × 640 px | `generate_terra_map.py:63`; Java reads it from the PNG |
| **Blocks per pixel** | 128 | *derived:* world width ÷ image width |
| **World size** | 131,072 × 81,920 blocks | *derived:* height follows the image's aspect |
| **X range** | −65,536 → +65,536 | *derived:* centred on origin |
| **Z range** | −40,960 → +40,960 | *derived:* centred on origin |
| **Origin (0, 0)** | pixel (512, 320), in Kazimierz | *derived:* the map is centred |
| **Origin shift** | none (0, 0) | `TerraMap.java:76` — moves spawn anywhere |
| **Lowest ground** | y 28 (y 21 with detail) | first knot of `mapHeight`, `ModNoiseSettings.java:135` |
| **Highest ground** | y 244 base, **y 305 actual** | last knot of `mapHeight`, plus `ruggedness` |
| **Sea level** | y 124 | `ModNoiseSettings.java:100` |
| **World floor / ceiling** | y −64 / y 320 | `NoiseSettings.create(-64, 384, …)`, `ModNoiseSettings.java:91` |
| **Seed-dependent?** | No — fixed constants throughout | — |

### Scale

**Resolution and world size are independent.** Set them separately; blocks-per-pixel is
derived and you never touch it.

```java
// TerraMap.java — how big the world is
public static final int WORLD_WIDTH_BLOCKS = 131_072;
```
```python
# tools/generate_terra_map.py — how detailed the map is
W, H = 1024, 640
```

```
blocksPerPixel = WORLD_WIDTH_BLOCKS / regions.png width
worldHeight    = image height × blocksPerPixel
```

Only the *width* is configured. Height follows the image's aspect ratio, so a square
image gives a square world and Terra can never be stretched by asking for a shape the
map doesn't have.

| regions.png | `WORLD_WIDTH_BLOCKS` | blocks/px | Resulting world |
|---|---|---|---|
| 1024 × 640 | 131,072 | 128 | 131,072 × 81,920 *(current)* |
| 1024 × 640 | 20,480 | 20 | 20,480 × 12,800 |
| 2048 × 1280 | 131,072 | 64 | 131,072 × 81,920 — same world, 4× the detail |
| **4096 × 4096** | **20,480** | **5** | **20,480 × 20,480** |
| 4096 × 4096 | 40,960 | 10 | 40,960 × 40,960 |

So to go from a 1024 map to a 4096 one while keeping a ~20k world: repaint at 4096 ×
4096, set `WORLD_WIDTH_BLOCKS = 20_480`, done. Nothing else changes.

#### What follows automatically

These used to be hardcoded to 128 blocks/px and would have quietly broken at any other
scale. They are all derived now:

| Derived | Why it has to be |
|---|---|
| The domain warp (`TerraMap`) | Fixed in blocks, it would be ±2 px at 128 blocks/px but ±13 px at 5 — Laterano is ~4 px across and would simply cease to exist |
| Ridge/erosion `xz_scale` (`ModNoiseSettings`) | A range's spurs are a map-scale feature; a quarter-size world otherwise gets ranges built from full-size mountains |
| Preview sampling (`TerraMapPreview`) | Otherwise a small world renders as a 128 px thumbnail |

Verified: at `WORLD_WIDTH_BLOCKS = 20_480` the terrain-slot shares come out identical to
the last decimal, every region stays reachable, and every coordinate scales by exactly
128/20. The world is the same world, just smaller.

The **detail noise deliberately does not scale** — that is surface texture, and a
boulder is a boulder whatever size the continent is.

#### The Python constant is cosmetic

`generate_terra_map.py` also has a `WORLD_WIDTH_BLOCKS`, but the generator works
entirely in pixel space: it is used for the printed summary and nothing else. The PNGs
come out byte-identical whatever it says. Keep it in step with the Java so the summary
doesn't lie to you, but getting it wrong cannot produce a wrong world.

#### Cost of a bigger map

The two arrays are one byte per pixel each, held for the life of the JVM:

| Resolution | Retained | Transient peak while decoding |
|---|---|---|
| 1024 × 640 | 1.25 MiB | ~4 MB |
| 2048 × 1280 | 5 MiB | ~17 MB |
| 4096 × 4096 | 33 MiB | ~120 MB |

4096² is fine, just be aware it's ~17M pixels to walk on first load — expect a few
hundred milliseconds of startup, once.

### Origin

The map is centred on the world origin, so:

```
blockX = (pixelX - 512) * 128        pixelX = blockX / 128 + 512
blockZ = (pixelY - 320) * 128        pixelY = blockZ / 128 + 320
```

Block (0, 0) is pixel (512, 320) — the middle of the image, currently in **Kazimierz**,
which is why that is the spawn region. The centring is not hardcoded to 512/320; it is
`width * 0.5`, so the map re-centres itself if you repaint at a different resolution.

### Moving the origin

To put spawn somewhere else, set the origin shift in `TerraMap.java:76`:

```java
public static final int ORIGIN_AT_BLOCK_X = 0;
public static final int ORIGIN_AT_BLOCK_Z = 0;
```

These take **the coordinates the place currently has**, and make that place the new
(0, 0). So the workflow is: run `./gradlew runData`, copy a coordinate out of the region
report, paste it in. To spawn in Iberia, which the report puts at `-12544, 25344`:

```java
public static final int ORIGIN_AT_BLOCK_X = -12544;
public static final int ORIGIN_AT_BLOCK_Z = 25344;
```

Re-run `runData` and the report now reads `IBERIA … tp @s 0 ~ 0`, with every other
region moved by the same vector — Kazimierz goes from `3072, -1792` to
`15616, -27136`.

It is a **pure translation**. The map, its domain warp and its terrain all move together,
so the world is the same world addressed through different coordinates; the terrain-slot
shares come out identical to the last decimal. It does not move a region relative to its
neighbours — for that, repaint the map.

Two consequences worth knowing:

- **The map's block extent moves with it.** It becomes
  `[-width/2 - shift, +width/2 - shift]`, so a large shift puts the frontier much closer
  on one side than the other. With the Iberia shift above, the eastern ocean edge is at
  x ≈ +78,000 while the western one is at x ≈ −53,000.
- **No regeneration needed.** The shift is applied when the PNGs are *read*, so the
  generator knows nothing about it and the PNGs do not change. `runData` still needs
  re-running to refresh the preview and the printed coordinates.

With a shift applied, the coordinate conversion picks up one term:

```
blockX = (pixelX - width/2)  * 128 - ORIGIN_AT_BLOCK_X
blockZ = (pixelY - height/2) * 128 - ORIGIN_AT_BLOCK_Z
```

There is no separate "spawn point" setting. Whatever is at pixel (512, 320) is where
players arrive.

### Height

The grey value in `elevation.png` becomes a world y through this chain:

```
grey 0-255 ──/255──► elevation 0..1 ──×2−1──► density −1..1 ──mapHeight──► terrainHeight
                                     TerraElevationFunction.java:34                │
                                                                                   ▼
                                                    surfaceY = 128 + 128 × terrainHeight
```

**`mapHeight` (`ModNoiseSettings.java:135`) is the min/max elevation mapping.** Its first
and last knots *are* the floor and ceiling of the world's terrain:

```java
DensityFunction mapHeight = spline(mapElevation,
        -1.00f, -0.781f,  // y  28   grey 0    ← lowest ground in the world
        -0.68f, -0.516f,  // y  62   grey 41
        ...
         0.72f,  0.531f,  // y 196   grey 219
         1.00f,  0.906f); // y 244   grey 255  ← highest, before relief is added
```

To read a knot: `y = 128 + 128 × value`. To write one: `value = (y − 128) / 128`.

The `128 + 128 ×` comes from `yClampedGradient(-64, 320, 1.5, -1.5)` at
`ModNoiseSettings.java:190`, which falls by exactly 1/128 per block. **Change that
gradient and every spline value in the file changes meaning** — it is not a knob to turn
casually.

> **Base height is not final height.** `mapHeight` is only the first term. `ruggedness`
> (`ModNoiseSettings.java:151`) adds local relief on top, worth ~60 blocks in the
> mountains, and the detail noise adds ±13 more. So peaks reach **y 305**, not the y 244
> the last knot implies, and the world ceiling is y 320.
>
> **There are only ~15 blocks of headroom.** Raising the top of `mapHeight` will clip
> terrain flat against the build limit. To get taller mountains you have to raise the
> world height in `NoiseSettings.create` *and* re-derive the gradient, which changes the
> formula above.

### If you want to…

| Goal | Edit |
|---|---|
| Make the world bigger or smaller | `WORLD_WIDTH_BLOCKS` in `TerraMap.java` — nothing else |
| Add map detail without resizing the world | Repaint the PNGs at a higher resolution — no code change at all |
| Both at once (e.g. 4096 px map, 20k world) | Repaint at 4096², set `WORLD_WIDTH_BLOCKS = 20_480` |
| Move spawn to another nation | `ORIGIN_AT_BLOCK_X/Z` in `TerraMap.java:76` — paste the region's current coordinates in |
| Deepen the oceans | First knots of `mapHeight` |
| Raise the mountains | Last knots of `mapHeight` — **but check headroom first** |
| Change sea level | `sea_level` at `ModNoiseSettings.java:100`, and move the `mapHeight` knot currently pinned to y 124 |
| Change which grey means "shore" | `TerraSlot.java` **and** the matching `mapHeight` knot **and** the generator |
| Flatten or roughen terrain generally | `ruggedness` and `detailAmount` in `ModNoiseSettings` |

---

## How the PNGs are read

### Pixel ↔ block coordinates

The map is centred on the origin.

```
blockX = (pixelX - 512) * 128        pixelX = blockX / 128 + 512
blockZ = (pixelY - 320) * 128        pixelY = blockZ / 128 + 320
```

So pixel (0, 0) is the north-west corner at block (−65,536, −40,960), and one pixel is a
128 × 128 block square.

### Regions: exact colour match

Each pixel of `regions.png` is looked up as a 24-bit RGB integer against the `colour`
field of every `TerraRegion`. The lookup is memoised per distinct colour, so a
655k-pixel map costs 28 comparisons, not 655k.

- **Exact match wins.** Use a pencil tool, not a brush.
- **No match** snaps to the nearest colour by squared RGB distance and logs a warning.
  This is a safety net for anti-aliased edges, not a feature to rely on.
- **Two regions sharing a colour** is a hard error thrown at class-init.

### Elevation: read from the raster, not `getRGB`

```java
elevation[i] = (byte) elevationRaster.getSample(x, y, 0);
```

This matters. A greyscale PNG decodes into a **linear** grey colour space, and
`BufferedImage.getRGB` converts that to sRGB on the way out, silently applying a gamma
curve. When this code used `getRGB`, elevation 0.16 came back as 0.44 and the deep ocean
generated as hill country. `getSample` returns the byte that is actually in the file.

If you ever add a third channel, read it the same way.

### Warping and interpolation

A pixel is 128 blocks across, so sampled raw the world would be a grid of enormous
squares. Three things in `TerraMap` prevent that:

| | |
|---|---|
| **Two-scale domain warp** | Every lookup is displaced by noise: ±210 blocks at a ~1400-block wavelength, plus ±46 blocks at ~260. Borders wander across the pixel grid instead of following it. |
| **Bilinear elevation** | Height is continuous, so ground slopes between pixels rather than stepping. Sampled at pixel *centres*, so interpolation isn't biased half a pixel north-west. |
| **Shared warp** | Regions are nearest-neighbour — you cannot average Iberia and Victoria — but they use the *same* warped position as elevation. That is what keeps the coastline in `regions.png` sitting on top of the coastline in `elevation.png`. |

Total warp displacement is at most **256 blocks, or exactly 2 pixels**. Remember that
number; it decides how thin a feature can be before the warp tears it apart.

### Off the edge of the map: clamping

Past the map edge, both lookups **clamp to the nearest edge pixel**. The world simply
continues with whatever is painted on that edge, forever.

> **This is the single most important rule for editing the map.**
> Whatever is on row 0 is what you get if you keep walking north, for ever. Same for the
> bottom row going south, and the left and right columns going west and east.

That is why the map paints its own frontiers: the Infy Icefield runs off the top edge and
the Foehn Hotlands off the bottom, while the left and right edges are ocean.

Clamping is also why there is no special case for out-of-bounds. An earlier version
returned a fixed region ("north of the map is always Infy"), which looks correct but is
not: because the lookup is warped, positions near the edge flip back and forth across it
and the boundary came out as a shimmering two-pixel band.

---

## Terrain slots

Within a region, elevation picks one of eight slots, and `(region, slot)` picks the
biome. Thresholds live in `TerraSlot.java`.

| Slot | Elevation | **Grey value** | Base y | **Actual surface y** | Share of world |
|---|---|---|---|---|---|
| `DEEP_SEA` | 0.00–0.16 | 0–40 | 28–62 | 21–68 | 26.2% |
| `SEA` | 0.16–0.34 | 41–86 | 62–116 | 56–120 | 11.2% |
| `SHORE` | 0.34–0.40 | 87–101 | 116–131 | 112–134 | 3.1% |
| `LOWLAND` | 0.40–0.48 | 102–122 | 131–137 | 129–144 | 10.0% |
| `FLATS` | 0.48–0.62 | 123–158 | 137–150 | 133–162 | 40.5% |
| `MIDLAND` | 0.62–0.74 | 159–188 | 150–168 | 144–192 | 3.8% |
| `HILLS` | 0.74–0.86 | 189–219 | 168–196 | 157–238 | 2.1% |
| `MOUNTAIN` | 0.86–1.00 | 220–255 | 196–244 | **179–305** | 3.1% |

**The grey column is what you type into an image editor.** The waterline is grey **94**,
inside `SHORE`, so a shore is half surf and half dry sand. Anything below 94 is
underwater; anything above is dry.

**The two height columns are different things and it matters.** "Base y" is what the
`mapHeight` spline alone produces, and it is what the slot thresholds are aligned to.
Ridge relief and detail noise are then added on top, which in the mountains is worth
another ~60 blocks. Peaks reach **y 305**, and the world ceiling is y 320 — so there is
only about 15 blocks of headroom left. Raise the top of `mapHeight` and terrain will
start clipping flat against the build limit.

> These edges are a three-way contract between `generate_terra_map.py`, `TerraSlot.java`
> and the `mapHeight` spline in `ModNoiseSettings`. **Move one and you must move all
> three**, or you get beaches halfway up a mountain.

---

## Adding a biome

A biome is what a place *looks and is made of*. A region is *where it is*. These are
separate; adding a biome does not put it anywhere until you reference it from a region.

**1. Declare a key** in `ModBiomes.java`:

```java
public static final ResourceKey<Biome> ASHEN_FLATS = createKey("ashen_flats");
```

**2. Add a palette and register it** in `ModBiomes.bootstrap()`:

```java
private static final Palette P_ASHEN =
        new Palette(0x9A8F84, 0xB0A69A, 0x5A5A50, 0x38382F, 0x8A8270, 0x7C7462);
//                  sky       fog       water     waterFog  grass     foliage
...
context.register(ASHEN_FLATS, biome(context, false, 1.4F, 0.1F, P_ASHEN));
//                                     precip  temp  downfall
```

`temperature` is **visual/behavioural only** — below `0.15` snow falls instead of rain and
water freezes, `2.0` is desert-hot. Since the climate model is gone, nothing places
biomes by it. It only has to agree with wherever you put the biome on the map.

**3. Give it a surface** in `ModNoiseSettings.createSurfaceRules()`, or it generates as
bare stone. See [Surface rules](#surface-rules).

**4. Reference it from a region** — see below. A biome nothing references is dead code.

**5. `./gradlew runData`.**

---

## Adding a region

A region is one row in the `TerraRegion` enum: a map colour, then its eight biomes in
`TerraSlot` order.

**1. Add the enum constant.** Pick a colour no other region uses.

```java
ASHEN_WASTE(0x8C8478,
        // deep sea                sea                        shore
        ModBiomes.BOLIVAR_DEPTHS,  ModBiomes.SEA_OF_SILENCE,  ModBiomes.IBERIAN_SHORES,
        // lowland                 flats                      midland
        ModBiomes.ASHEN_FLATS,     ModBiomes.ASHEN_FLATS,     ModBiomes.BARRENLANDS,
        // hills                   mountain
        ModBiomes.KAZDEL_CRAGS,    ModBiomes.KAZDEL_CRAGS),
```

All eight slots are required — the constructor throws if you give it a different number,
so you cannot silently shift a region's biomes by one. Fill sea slots on a landlocked
region and land slots on an ocean anyway: they are usually unreachable, but they keep the
map correct if you later repaint a coastline.

**2. Paint the colour into `regions.png`.**

**3. Make `elevation.png` agree.** Don't paint an ocean region over grey 200.

**4. `./gradlew runData`,** then look at `docs/terra_world_preview.png` and the log.

### Things that will bite you

| Symptom | Cause |
|---|---|
| `UNREACHABLE` in the datagen log | The colour is in `TerraRegion` but not in the PNG — or a seed drowned in an inland sea |
| "colour #XXXXXX matches no region" warning | Anti-aliased painting, or a typo in the `colour` field |
| Hard error at startup about a shared colour | Two regions with the same `colour` |
| Region appears speckled or torn | The painted area is thinner than the ~2 px warp |
| Biomes disagree with terrain | `regions.png` and `elevation.png` don't agree |
| Existing world unchanged | Worldgen changes need a **fresh world**; delete `run/saves/` |

> **Reordering the `TerraRegion` or `TerraSlot` enums invalidates an existing
> `terra.json`.** The biome table is serialised as a flat list in enum order. `runData`
> regenerates it; the size check in `TerraMapBiomeSource` is what stops the mistake being
> silent. Appending a region at the end is always safe.

> **Watch the seams.** Where two regions meet, their tables should agree at the shared
> slot, or the border is a hard line — palm sand against pack ice. The old climate model
> enforced this structurally; a hand-authored map cannot, because the whole point is that
> you decide what goes next to what.

---

## Worked example: terra incognita north of Infy

Goal: an unknown, unmapped waste **beyond** the Infy Icefield, which is exactly what canon
puts there — the icefield "acts as a border between Terra's civilized world and terra
incognita".

The one thing that makes this different from any other region: **it has to be the thing
painted on row 0**, because row 0 is what repeats forever northward. Infy currently
occupies row 0, so Infy has to move down to make room.

### 1. The biome

In `ModBiomes.java`:

```java
public static final ResourceKey<Biome> TERRA_INCOGNITA = createKey("terra_incognita");

/** Past the icefield. No survey, no colour, no horizon. */
private static final Palette P_UNKNOWN =
        new Palette(0xD8DEE4, 0xE8ECF0, 0x2A3E50, 0x16222E, 0x9AA4A8, 0x8E989C);
...
context.register(TERRA_INCOGNITA, biome(context, true, -0.8F, 0.4F, P_UNKNOWN));
```

Surface rules in `ModNoiseSettings.createSurfaceRules()`:

```java
SurfaceRules.ifTrue(SurfaceRules.isBiome(ModBiomes.TERRA_INCOGNITA),
        SurfaceRules.sequence(
                SurfaceRules.ifTrue(floor0, SurfaceRules.sequence(
                        SurfaceRules.ifTrue(patchHigh, snow),
                        SurfaceRules.ifTrue(patchMid,  packedIce),
                        SurfaceRules.ifTrue(patchLow,  blueIce)
                )),
                SurfaceRules.ifTrue(floor4, packedIce),
                SurfaceRules.ifTrue(floor8, blueIce)
        )
),
```

### 2. The region

In `TerraRegion.java`, appended after `INFY`:

```java
/**
 * Beyond the Infy Icefield. Painted on row 0 of the map, which is what makes it
 * endless: TerraMap clamps out-of-bounds lookups to the nearest edge pixel.
 */
UNKNOWN_NORTH(0xF4FAFF,
        ModBiomes.AEGIR_DEPTHS,     ModBiomes.SEA_OF_SILENCE,   ModBiomes.AEGIR_SHELF,
        ModBiomes.TERRA_INCOGNITA,  ModBiomes.TERRA_INCOGNITA,  ModBiomes.TERRA_INCOGNITA,
        ModBiomes.TERRA_INCOGNITA,  ModBiomes.KJERAG_SLOPES),
```

`0xF4FAFF` is close to Infy's `0xEAF6FF` on screen but distinct as an exact value — fine
for the code, awkward if you ever hand-edit. Pick something visually distinct if you plan
to paint by hand.

### 3. Put it on the map

**Option A — hand-paint.** Simplest for an edge band. Open `regions.png`, fill rows 0
down to about row 40 with `#F4FAFF`, then in `elevation.png` fill the same rows with grey
**135** (mid-`FLATS`). Feather the boundary between the two regions by hand if you want it
organic.

You need at least ~4 rows to beat the 2-pixel runtime warp, but 30–50 rows (≈4,000–6,400
blocks deep) makes it feel like a region rather than a stripe.

**Option B — regenerate.** In `tools/generate_terra_map.py`:

```python
# Terra incognita, beyond the icefield. Depth in pixels from the top edge.
#
# This must comfortably exceed the generator's own coast warp (±54 px) or the band will
# not reach row 0 everywhere, and the top of the world comes out as a broken fringe.
UNKNOWN_DEPTH = 96
UNKNOWN_COLOUR = 0xF4FAFF
```

Push Infy down so it is no longer the top thing:

```python
INFY_LENS = (600, 60, 430, 130)     # was (600, -36, 430, 118)
```

In `build()`, right after the Infy lines:

```python
    # Painted last and reaching row 0, so it is what continues north forever.
    unknown = coast_y < UNKNOWN_DEPTH + 22 * value_noise((H, W), 6, seed=71, octaves=3)
    land |= unknown
```

and in the compose step, after `paint(infy, INFY_COLOUR)`:

```python
    paint(unknown, UNKNOWN_COLOUR)
```

Return `unknown` alongside `infy` if you want it in the printed stats.

> **The warp trap.** The generator warps by up to ±54 px, so a band whose threshold is
> only 30 px will cover *part* of row 0 and leave gaps in the rest. Any band that must
> reach an edge needs a threshold larger than the warp amplitude. This is the same class
> of bug as the shimmering world edge described above — it is easy to hit and obvious in
> the preview.

### 4. Verify

```
python tools/generate_terra_map.py     # only if you used Option B
./gradlew runData
```

Check the log for `UNKNOWN_NORTH` with a non-zero share and a coordinate, then open
`docs/terra_world_preview.png` and confirm the band spans the whole top edge with Infy
below it. Test in game with a **fresh world**:

```
/execute in priestess:terra run tp @s 0 200 -40000
```

---

## Surface rules

`ModNoiseSettings.createSurfaceRules()` returns one big ordered sequence. **First
matching rule wins:**

1. bedrock floor (must stay first),
2. one branch per biome,
3. the global deepslate transition (must stay last — it's the fallback).

```java
SurfaceRules.ifTrue(SurfaceRules.isBiome(ModBiomes.ASHEN_FLATS),
        SurfaceRules.sequence(
                SurfaceRules.ifTrue(floor0, ash),        // depth 0 (the top block)
                SurfaceRules.ifTrue(floor3, coarseDirt), // depths 1-3
                SurfaceRules.ifTrue(floor8, basalt)      // depths 4-8
        )
),
```

`floorN` is `stoneDepthCheck(N, …)`, which matches **every depth from 0 through N** — a
maximum, not an exact depth. So order them shallowest-first, or a deep rule swallows the
shallow ones. `floor0` followed by `floor4` with the same block is redundant.

For a mottled surface, wrap in the `patchHigh` / `patchMid` / `patchLow` noise conditions.
For altitude variation, wrap in `aboveSnowline` / `aboveTreeline` — and put the altitude
branch **before** the biome's normal rules, or they match first and the snowline never
fires.

To use a mod block, add a shorthand at the top of the method:

```java
var ash = SurfaceRules.state(ModBlocks.ASH_BLOCK.get().defaultBlockState());
```

---

## Terrain shape

The one formula worth memorising:

```
surfaceY = 128 + 128 * terrainHeight
```

The y-gradient falls by exactly 1/128 per block. Every spline in `ModNoiseSettings` is
written in those units with the resulting y in a comment on each knot. To tune: decide the
height you want, divide by 128, write that number down.

```
terrainHeight = mapHeight(mapElevation)      // base elevation, straight off the map
              + ruggedness(mapElevation)     // how much local relief belongs here
                * reliefVariation(erosion)   // varied along a range
                * ridgeShape(ridges)         // where the individual spurs run
```

| Spline | Driven by | Does what |
|---|---|---|
| `mapHeight` | map elevation | base elevation; knots sit on the `TerraSlot` edges |
| `ruggedness` | map elevation | ±1 block on a shore, ±38 in the mountains |
| `reliefVariation` | erosion noise | stops two mountains on one range being identical |
| `ridgeShape` | ridge noise | folded so peaks land at \|ridges\|≈0.65 |

`mapElevation` is **not noise** — it is `TerraElevationFunction` reading `elevation.png`,
rescaled from the PNG's [0,1] to the [−1,1] the splines expect. There is no
continentalness, temperature or vegetation noise left in the mod. Noise only supplies
detail finer than the map's 128-block resolution.

A 3D `detail` noise is added to the density itself, producing cliffs and overhangs rather
than a smooth height field. Its `yScale` (0.2) is deliberately below its `xzScale` (0.5),
keeping it vertically coherent — raise `yScale` and you get swiss cheese. Its amplitude is
`detailAmount(mapElevation)`, so roughness follows terrain class: ±13 blocks on a crag
face, ±1 on a shore.

**Footgun:** spline knots are in noise units, not blocks. `-0.26f, -0.031f` means "at map
elevation 0.37, put the surface at y = 128 + 128×(−0.031) = 124".

`NoiseSettings.create(-64, 384, 1, 2)` sets world height; `sea_level` is `124`.
`aquifers_enabled` and `ore_veins_enabled` are `false` — ores are expected from GregTech.

---

## Verifying and troubleshooting

`./gradlew runData` regenerates `docs/terra_world_preview.png` and prints a report.

**The preview is rendered by asking the real `TerraMap`**, not by re-reading the PNGs, so
it shows what will actually generate — warp, bilinear filtering and all. Always look at it
after editing the map. A warp strong enough to look organic is also strong enough to tear
a small nation apart.

The report gives, per region, its share of the world and a coordinate you can paste
straight into chat:

```
  region           share  slot       go here
  BARRENLANDS      8.11%  FLATS      /execute in priestess:terra run tp @s 4096 ~ 5632
  INFY             7.52%  FLATS      /execute in priestess:terra run tp @s 14080 ~ -36608
  FOEHN            4.83%  FLATS      /execute in priestess:terra run tp @s 34048 ~ 32256
```

Those coordinates are the region's centroid snapped to a point actually inside it.
Substitute a real y — `~` keeps your current height, which in a fresh dimension is usually
inside rock or above the void. The slot column tells you roughly where the ground is; for
`FLATS`, y 160 is a safe drop.

**Worldgen changes always need a fresh world.** Already-generated chunks keep their
terrain and biomes forever. Delete the test world in `run/saves/` and make a new one.
