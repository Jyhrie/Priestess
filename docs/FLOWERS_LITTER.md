# Flowers and ground litter

How to add a plant: a small flower that stands up out of the ground, and the fallen-petal
ground cover that goes with it.

Everything here is vanilla block classes doing vanilla work. There is no `PriestessFlower`
base class and there should not be one — nothing about a flower on Terra behaves differently
from a flower in the overworld, and a subclass that only calls `super` is a file to maintain
for no behaviour. What this file is actually about is the four places a plant is *not* like the
cube blocks in [DUNGEON_BLOCKS.md](DUNGEON_BLOCKS.md): its texture has holes in it, its item is
a flat sprite, its model paths are foldered, and its loot depends on its state.

---

## Contents

- [The short version](#the-short-version)
- [Adding a flower](#adding-a-flower)
- [Adding a litter block](#adding-a-litter-block)
- [Where the files go](#where-the-files-go)
- [What you get for free, and what you do not](#what-you-get-for-free-and-what-you-do-not)
- [Checking it worked](#checking-it-worked)
- [It looks wrong](#it-looks-wrong)

---

## The short version

Three registrations per flower, and every one of them is a vanilla class:

```java
public static final RegistryObject<Block> WHITEFLOWER = registerBlock("whiteflower",
        () -> new FlowerBlock(() -> MobEffects.REGENERATION, 7,
                BlockBehaviour.Properties.copy(Blocks.POPPY)));

public static final RegistryObject<Block> WHITEFLOWER_PETALS = registerBlock("whiteflower_petals",
        () -> new PinkPetalsBlock(BlockBehaviour.Properties.copy(Blocks.PINK_PETALS)));

public static final RegistryObject<Block> POTTED_WHITEFLOWER = BLOCKS.register("potted_whiteflower",
        () -> new FlowerPotBlock(() -> (FlowerPotBlock) Blocks.FLOWER_POT, WHITEFLOWER,
                BlockBehaviour.Properties.copy(Blocks.POTTED_POPPY)));
```

| Class | Gives you |
|---|---|
| `FlowerBlock` | a small flower: instant break, needs something to root in, suspicious stew ingredient |
| `PinkPetalsBlock` | ground litter: 1–4 petals per block, rotates to the placer, bone-mealable |
| `FlowerPotBlock` | the potted version — Forge's 3-arg constructor, see below |

**`PinkPetalsBlock` hardcodes nothing pink.** It is the cherry-petals class and it serves any
petal, so litter needs no subclass either. Minecraft 1.20.1 has no *leaf litter* block — that
arrived in 1.21.5 — and this is the same mechanic under an earlier name.

Two things about those three lines that are not obvious:

- **The effect is a supplier.** Forge deprecated `FlowerBlock(MobEffect, …)`. An effect is a
  registry entry, and holding one while blocks are still being constructed is holding it before
  the registry is necessarily filled.
- **The pot is registered through `BLOCKS.register`, not `registerBlock`.** It must *not* get a
  `BlockItem`. You make a potted plant by using the flower on a pot; an item for it would be a
  second way to obtain one that vanilla does not offer and that no recipe or loot table would
  explain. Forge's three-argument constructor is the whole of "the vanilla pot accepts this
  flower" — passing the empty pot registers the plant into that pot's content map, so there is
  nothing to add on the vanilla side. It resolves the flower while it runs, which is safe only
  because the flower is declared **above** it and so is registered first.

---

## Adding a flower

Worked through with **Whiteflower**, which is in the mod.

### 1. Register it — `block/ModBlocks.java`

As above. `Properties.copy(Blocks.POPPY)` is doing more than hardness: it carries the random XZ
offset that stops a meadow of flowers sitting on a visible grid, so that never has to be asked
for by hand.

The two numbers are the **suspicious stew** effect and its duration in seconds. Pick from
vanilla's flowers rather than inventing — Whiteflower takes the oxeye daisy's Regeneration, and
seven seconds is a bowl of stew's worth rather than a potion's.

### 2. Texture — `tools/generate_placeholder_art.py`

One 16×16 tile with **transparency**, at
`assets/priestess/textures/block/flowers/whiteflower.png`.

Neither `block_texture` nor `patterned_block` can draw one — both write an opaque pixel
everywhere. Plants use `plant_texture`, which paints the ASCII glyphs the item sprites use into
`textures/block/<folder>/`:

```python
PLANTS = [
    # folder,    name,          base,               accent,             seed, glyph
    ("flowers", "whiteflower", WHITEFLOWER_PETAL, WHITEFLOWER_GREEN,  351, WHITEFLOWER),
]
```

where the glyph is 16 rows of 16 characters — `#` accent, `.` base, space transparent:

```python
WHITEFLOWER = [
    "                ",
    "      ....      ",
    "     ......     ",
    "    ...##...    ",
    ...
    "       ##       ",
]
```

Draw the whole plant, bloom to stem, touching the bottom edge. The cross model is two of these
quads at right angles, so the tile has to read as a plant from any side.

Then `python tools/generate_placeholder_art.py`. It is seeded and idempotent — re-running never
disturbs existing art. Overwrite the PNG with real art and **remove its `PLANTS` entry** so a
later run cannot clobber it.

### 3. Blockstate, models and the item — `datagen/ModBlockStateProvider.java`

One call does the flower, the pot and the item model:

```java
flower(ModBlocks.WHITEFLOWER, ModBlocks.POTTED_WHITEFLOWER);
```

Two things that helper exists to get right:

**`renderType` is not optional.** Since 1.19 a model's render layer is a field on the model
rather than something registered in client code, and a model that names none is drawn as solid
geometry — which fills every transparent pixel of the tile with black. An undeclared flower is
a black square with a flower inside it. Cutout rather than translucent: these tiles are opaque
or empty per pixel with nothing in between, and cutout does not pay for depth sorting.

**The item is a flat sprite, not the block model.** `simpleBlockWithItem` would hand the
inventory the cross model, and two quads crossing at right angles read as a smear when a slot
shows them head-on. Vanilla points flower items at `item/generated` with the *block* tile as
`layer0`, which is what `flower()` does — so a flower needs **no item texture of its own**.

### 4. Loot tables — `datagen/ModLootTableProvider.java`

```java
this.dropSelf(ModBlocks.WHITEFLOWER.get());
this.dropPottedContents(ModBlocks.POTTED_WHITEFLOWER.get());
```

`dropPottedContents` writes the pot and the plant as two separate drops, and it reads the plant
back off the block — so there is nothing here to keep in step with `ModBlocks`.

**Neither is optional.** `getKnownBlocks()` reports every registered block and datagen *fails*
if one has no table. The potted block counts, even though it has no item.

### 5. Tags — `datagen/ModBlockTagsProvider.java`

```java
tag(BlockTags.SMALL_FLOWERS).add(ModBlocks.WHITEFLOWER.get());
tag(BlockTags.FLOWER_POTS).add(ModBlocks.POTTED_WHITEFLOWER.get());
```

`#small_flowers` is what vanilla builds `#flowers` and `#sword_efficient` out of, so joining
that one tag is what makes bees pollinate the flower and a sword cut it down. Do not list those
two by hand for a small flower; you would be duplicating an inclusion vanilla already has.

`#flower_pots` is how the pot's own break and pick behaviour finds the full pots.

### 6. Name it — `datagen/ModLanguageProvider.java`

```java
add(ModBlocks.WHITEFLOWER.get(), "Whiteflower");
add(ModBlocks.POTTED_WHITEFLOWER.get(), "Potted Whiteflower");
```

No item ever carries the potted name, but vanilla names its potted blocks too, and a missing
key is what a debug screen shows you.

### 7. `./gradlew runData`

It appears in the Priestess creative tab automatically — the tab iterates `ModItems.ITEMS`, and
`registerBlock` registered the `BlockItem`. The pot does not appear, by design.

---

## Adding a litter block

Same seven steps, with three differences.

### The tile is four tiles

The `block/flowerbed_N` parents each draw **only the Nth petal**, in its own 8×8 quadrant of
the texture — layer 1 takes the top-left, layer 4 the top-right. So all four quadrants of the
16×16 tile have to carry their own scatter, and the scatter has to be placed differently in
each, or a full block of petals comes out as a grid of four identical clumps.

A litter block needs a **second** texture for the stems, and those parents sample a 1×3 strip
of it at `x=0, y=4..7` and nothing else. Everything outside that strip is deliberately empty:
it is a colour swatch with a shape, not a drawing.

```python
PLANTS = [
    ("litter",  "whiteflower_petals",      WHITEFLOWER_PETAL, WHITEFLOWER_SHADE, 352, WHITEFLOWER_PETALS),
    ("litter",  "whiteflower_petals_stem", WHITEFLOWER_GREEN, WHITEFLOWER_GREEN, 353, WHITEFLOWER_PETALS_STEM),
]
```

Unlike a flower, litter **does** want its own item sprite, added to `ITEMS` as usual: the block
tile is a scatter seen from directly above, which says nothing at slot size.

### The blockstate is sixteen multipart cases

```java
petals(ModBlocks.WHITEFLOWER_PETALS);
```

Four models × four horizontal facings, which is vanilla's own arrangement rather than a choice.
Because the layer models stack — a block holding three petals applies layers 1, 2 *and* 3 —
each part's condition is **"amount is N or more"**, not "amount is N". Condition on the exact
amount and a three-petal block shows the third petal alone, floating in its quadrant.

The parents are authored facing north while `Direction.toYRot` measures from south, hence the
`+ 180`: north has to come out as no rotation at all.

Those parents also carry `tintindex 1` on the stem faces, which vanilla uses to grass-tint pink
petal stems. Nothing here registers a colour provider, so the tint resolves to white and the
stem renders in its own colours — which is the intent. A whiteflower stem should not change hue
with the biome it fell in. Register a `RegisterColorHandlersEvent.Block` handler only if you
want a plant that follows the grass.

### The loot depends on the state

Litter must drop **one item per petal**, or a block of four is a way to delete three:

```java
this.add(ModBlocks.WHITEFLOWER_PETALS.get(), petals(ModBlocks.WHITEFLOWER_PETALS.get()));
```

The `petals(...)` helper in `ModLootTableProvider` builds one entry carrying four conditional
`set_count`s, which is the shape of vanilla's own pink petals table: the item is always the
same, only the count depends on the state. `dropSelf` would give you one petal back out of four.

Tag it `#sword_efficient` **by name**:

```java
tag(BlockTags.SWORD_EFFICIENT).add(ModBlocks.WHITEFLOWER_PETALS.get());
```

Litter is not a small flower and inherits none of that tag's contents through `#small_flowers`.
Vanilla lists pink petals there by name for the same reason.

---

## Where the files go

Plants are foldered by whether they stand up or have fallen down:

```
src/main/resources/assets/priestess/textures/
├── block/flowers/whiteflower.png                    the standing plant, one tile
├── block/litter/whiteflower_petals.png              four 8x8 quadrants of scatter
├── block/litter/whiteflower_petals_stem.png         a 1x3 strip at x=0, y=4..7
└── item/whiteflower_petals.png                      litter only; a flower reuses its block tile

src/generated/resources/assets/priestess/models/
├── block/flowers/whiteflower.json                   parent block/cross
├── block/flowers/potted_whiteflower.json            parent block/flower_pot_cross
└── block/litter/whiteflower_petals_1..4.json        parent block/flowerbed_1..4
```

**The `block/` in a model name is load-bearing.** A model name containing a slash is taken as a
complete path and the provider's own `block` folder is *not* prepended — pass
`"flowers/whiteflower"` and the file lands in `models/flowers/`, outside the tree every other
block model in the mod lives in. Hence the `FLOWERS` and `LITTER` constants at the top of
`ModBlockStateProvider`, which spell out `block/flowers/` and `block/litter/` and are used for
the texture paths as well, so a model and its texture cannot drift apart.

Blockstates and item models stay at their default paths. Those are keyed by registry name and
cannot be foldered.

---

## What you get for free, and what you do not

| | Where from |
|---|---|
| ✅ Instant break, no collision, plant sound | `Properties.copy(POPPY / PINK_PETALS)` |
| ✅ Random XZ offset, so a meadow is not a grid | copied with the properties |
| ✅ Needs a block to root in, breaks when it loses it | `BushBlock`, via `FlowerBlock` |
| ✅ Suspicious stew ingredient | `FlowerBlock`, from the two constructor arguments |
| ✅ 1–4 petals, facing, bone meal, stacking placement | `PinkPetalsBlock` |
| ✅ Accepted by the vanilla flower pot | Forge's 3-arg `FlowerPotBlock` constructor |
| ✅ Bees pollinate it, swords cut it | `#small_flowers` (litter: `#sword_efficient`) |
| ✅ A creative tab entry | `registerBlock`, via `ModItems.ITEMS` |
| ❌ Anything in the world to find | no worldgen feature — `/give` or the tab |
| ❌ Compostable | needs a `ComposterBlock.COMPOSTABLES` entry in common setup |
| ❌ Grass-tinted stems | no colour handler registered, on purpose |
| ❌ A biome that wants it | see [WORLDGEN.md](WORLDGEN.md#surface-rules) |

**Nothing places these in the world yet.** Whiteflower and its petals are obtainable, named and
correct, which is the bar the rest of the mod's scaffolding is held to — a vegetation feature is
the next thing, not a missing part of this one.

**Plants root in vanilla ground.** `BushBlock.mayPlaceOn` accepts dirt, grass and farmland, so
none of Terra's own terrain blocks — Permafrost, the sands, Dead Seabed — will hold a flower
until something says they can. That is a deliberate gap and not a bug in the flower.

---

## Checking it worked

```
/give @s priestess:whiteflower
/give @s priestess:whiteflower_petals 8
                                          → place the flower: bloom and stem, no black square
                                          → place petals, click the same block 3 more times:
                                            four petals appear one at a time
                                          → break it: 4 petals back, not 1
                                          → bone meal a 1-petal block: it becomes 2
/setblock ~ ~ ~ flower_pot                → use the flower on it: potted, and the flower is gone
                                          → break the pot: pot and flower drop separately
```

A correct flower is **not** a black square, reads as a plant from every angle, and wobbles off
centre from its neighbours. A correct litter block turns with you as you place it.

---

## It looks wrong

| Symptom | Cause |
|---|---|
| Black square with the plant inside it | No `renderType` on the model. The single most common plant mistake — see step 3. |
| Missing-texture chequerboard | The texture is not where the model says. Check `block/flowers/…` against the `FLOWERS` constant, and that you ran the art tool. |
| The inventory icon is a smeared cross | The item model is the block model. A flower's item is `item/generated` over the block tile. |
| Model JSONs appear in `models/flowers/` | The `block/` prefix is missing from the model *name*. See [Where the files go](#where-the-files-go). |
| `runData` fails naming the new block | No loot table. The potted block needs one too. |
| Petals show one floating petal, not a pile | The multipart conditions are exact-amount instead of "N or more". |
| Petals drop 1 when you break a pile of 4 | `dropSelf` instead of the state-conditional table. |
| Petals never stack past one | The block is not a `PinkPetalsBlock`; stacking placement is `canBeReplaced` on that class. |
| The flower will not place on Terra's terrain | Expected. `BushBlock` accepts vanilla ground only. |
| Nothing appears in the creative tab | The pot is meant to be absent. For the others, check `registerBlock` was used rather than `BLOCKS.register`. |
