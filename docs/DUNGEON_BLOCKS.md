# Dungeon-locked blocks

How to make a block that cannot be mined until a dungeon is cleared, and how to add a dungeon
for one to be locked behind.

For what the lockdown *is* and why it gates blocks rather than areas, see
[Progression](../README.md#progression) in the README. This file is the how.

---

## Contents

- [The short version](#the-short-version)
- [Locking a block behind an existing dungeon](#locking-a-block-behind-an-existing-dungeon)
- [Registering a new dungeon](#registering-a-new-dungeon)
- [What you get for free, and what you do not](#what-you-get-for-free-and-what-you-do-not)
- [Checking it worked](#checking-it-worked)
- [It isn't locking](#it-isnt-locking)

---

## The short version

**Extend `SealedBlock` and name a dungeon.** That is the gate:

```java
public static final RegistryObject<Block> SAL_VIENTO_CATACOMBS_STONE =
        registerBlock("sal_viento_catacombs_stone",
                () -> new SealedBlock(Dungeon.UNDER_TIDES, catacombs()));
```

The dungeon passed to the constructor is the single source of truth. `ModBlockTagsProvider`
scans every registered block for `DungeonSealed` and writes the `sealed_by/<dungeon>` tag from
what it finds, so there is no second list to keep in step — and `DungeonSealed.seal()` applies
the blast and piston immunity in the constructor, so a gated block cannot be registered with a
way around the gate still open.

| Class | Use for | Extends |
|---|---|---|
| `SealedBlock` | ordinary cubes | `Block` |
| `SealedPillarBlock` | anything with an axis | `RotatedPillarBlock` |
| `DungeonSealed` | the interface both implement — what the tags provider looks for | — |

Java allows one superclass, so a sealed slab or stair needs its own two-line subclass on the
same pattern. Implement `DungeonSealed`, pass properties through `DungeonSealed.seal()`, and
the tags provider will find it.

---

## Locking a block behind an existing dungeon

Worked through with **Sal Viento Catacombs Stone**, which is in the mod and gated behind Under
Tides — the dungeon Bishop Quintus ends.

### 1. Register it — `block/ModBlocks.java`

Properties describe the **material only**. Say nothing about the gate; the base class handles
that.

```java
/**
 * The Sal Viento catacombs, gated behind Under Tides.
 *
 * DEEPSLATE_BRICKS rather than the Arts Lab's tiles: same tier, so neither build set is the
 * cheap way into the other, but a masonry sound and a coarser look.
 */
private static BlockBehaviour.Properties catacombs() {
    return BlockBehaviour.Properties.copy(Blocks.DEEPSLATE_BRICKS);
}

public static final RegistryObject<Block> SAL_VIENTO_CATACOMBS_STONE =
        registerBlock("sal_viento_catacombs_stone",
                () -> new SealedBlock(Dungeon.UNDER_TIDES, catacombs()));

public static final RegistryObject<Block> SAL_VIENTO_CATACOMBS_OVERGROWN_STONE =
        registerBlock("sal_viento_catacombs_overgrown_stone",
                () -> new SealedBlock(Dungeon.UNDER_TIDES, catacombs()));
```

> **Which dungeon?** Not necessarily the one the blocks are named after. The Arts Lab blocks
> are Rhine Lab's, but they are gated behind **Dorothy's Vision**, because chapter order
> decides this — a dungeon gating its own build set is a locked door with the key behind it.
> Gate a build set behind the dungeon *before* the one it is used in.

A shared properties method per build set is the convention, so the gate cannot be undercut by
one block of a set being softer than the rest.

### 2. Texture

`src/main/resources/assets/priestess/textures/block/sal_viento_catacombs_stone.png`, 16×16.

For a placeholder, add it to `tools/generate_placeholder_art.py` rather than drawing one — the
generator is seeded, so re-running it never disturbs existing art:

```python
SAL_VIENTO_BLOCKS = [
    # name,                                    base,             accent,          seed, pattern
    ("sal_viento_catacombs_stone",           CATACOMBS_STONE, CATACOMBS_JOINT, 321, "catacombs"),
    ("sal_viento_catacombs_overgrown_stone", CATACOMBS_STONE, CATACOMBS_MOSS,  322, "catacombs_overgrown"),
]
```

with a branch in `arts_lab_pattern()` returning `True` where the accent goes. Then
`python tools/generate_placeholder_art.py`.

### 3. Blockstate and model — `datagen/ModBlockStateProvider.java`

```java
simpleBlockWithItem(ModBlocks.SAL_VIENTO_CATACOMBS_STONE.get(),
        cubeAll(ModBlocks.SAL_VIENTO_CATACOMBS_STONE.get()));
```

A `SealedPillarBlock` uses the `pillar(...)` helper instead and needs `_top` / `_side`
textures.

### 4. Loot table — `datagen/ModLootTableProvider.java`

```java
this.dropSelf(ModBlocks.SAL_VIENTO_CATACOMBS_STONE.get());
```

**Not optional.** `getKnownBlocks()` reports every registered block and datagen *fails* if one
has no table. If `runData` errors naming your new block, this is the step you missed.

The gate is deliberately **not** in the loot table. A sealed block's break is cancelled
outright, so there is no drop to suppress — and once the dungeon is cleared the block is
ordinary building material the player is meant to keep.

### 5. Tool tags — `datagen/ModBlockTagsProvider.java`

The one list you *do* still edit, because it describes the material rather than the gate:

```java
private static final List<RegistryObject<Block>> IRON_PICKAXE = List.of(
        ...
        ModBlocks.SAL_VIENTO_CATACOMBS_STONE,
        ModBlocks.SAL_VIENTO_CATACOMBS_OVERGROWN_STONE);
```

Deepslate-derived properties set `requiresCorrectToolForDrops`, and a block with that flag and
no `mineable` tag is mineable by **nothing at all** — slow no-tool speed, then no drop whatever
you hit it with. Skip this and the block will look broken in a way that has nothing to do with
the lockdown.

### 6. Name it — `datagen/ModLanguageProvider.java`

```java
add(ModBlocks.SAL_VIENTO_CATACOMBS_STONE.get(), "Sal Viento Catacombs Stone");
```

### 7. `./gradlew runData`

It appears in the Priestess creative tab automatically — the tab iterates `ModItems.ITEMS`, and
`registerBlock` registers the `BlockItem` for you.

---

## Registering a new dungeon

A dungeon's tag is **derived from its enum constant**, so there is no tag to register by hand
and no way to declare one that points at another dungeon's blocks. Adding a constant to
`progression/Dungeon.java` is the whole job:

```java
/** Sal Viento's catacombs. Cleared by Bishop Quintus. */
UNDER_TIDES(() -> ModEntities.SV_BISHOP_QUINTUS.get(), null, Set.of());
```

| Argument | Meaning |
|---|---|
| `boss` | killing this entity type clears the dungeon — or `null` |
| `clearedByPickingUp` | picking up this item clears it, for a dungeon that ends in a chest rather than a fight — or `null` |
| `unlocksFlightIn` | biomes that refuse flight until it is cleared; `Set.of()` for none |

That yields `priestess:sealed_by/under_tides` from `getSerializedName()`, which is the constant
lowercased. Everything else follows on its own: `DungeonLockdown` and `FlightRestriction`
iterate the enum, `DungeonProgress` keys its records off the same name, and `/dungeon` builds a
literal per constant so the new dungeon gets tab-completion and validation for free.

Two things to do by hand:

- **Name it** in `ModLanguageProvider`, keyed `dungeon.priestess.<serialized_name>`. This is
  what the refusal message says, so an unnamed dungeon shows a raw translation key.
- **Give it a clear condition.** A dungeon that nothing clears *always reads as cleared* and
  gates nothing — `DungeonProgress.isCleared` fails open on purpose, because a seal with no key
  is the one state a player cannot recover from.

The serialized name ends up in save data, so **renaming a constant orphans existing progress**.

---

## What you get for free, and what you do not

| | Where from |
|---|---|
| ✅ `sealed_by/<dungeon>` membership | derived from the constructor argument |
| ✅ `minecraft:wither_immune` | derived alongside it |
| ✅ Explosion-proof (bedrock resistance) | `DungeonSealed.seal()` |
| ✅ Piston-proof (`PushReaction.BLOCK`) | `DungeonSealed.seal()` |
| ✅ Client-side refusal — no crack, no progress | `DungeonSync` + the block tag |
| ✅ Creative bypass, `lockdown.enabled` switch | `DungeonLockdown` |
| ❌ `mineable/*` and tool tier | you list it |
| ❌ Loot table, model, name | you write them |

**The three immunities never lift.** Unlike mining, they do not care whether the dungeon is
cleared — an explosion, a piston and a wither skull all arrive without a player, so there is
nobody whose progress could be consulted. After clearing, you mine the block and place it where
you like, but you still cannot shove or blow it around.

**The lockdown does not ask who placed a block.** A gated block a player puts down is one they
cannot take back until they clear that dungeon.

---

## Checking it worked

```
/gamemode survival                                    creative is exempt
/give @s priestess:sal_viento_catacombs_stone
/dungeon seal under_tides                             put it back to uncleared
/dungeon list                                         "under_tides: SEALED — 2 block types"
                                                      → place it, try to mine it: refused
/dungeon clear under_tides                            → it mines, and drops itself
```

`/dungeon list` printing the block count is the fastest check that the tag actually generated —
`nothing yet (no blocks tagged sealed_by/…)` means the gate is empty, not broken.

A correct refusal is: **no crack overlay, no digging sound, no progress**, and the action-bar
line. Small hit particles and the arm swing still happen, exactly as when you punch bedrock —
those are the client's response to the click and are not mining progress.

---

## It isn't locking

| Symptom | Cause |
|---|---|
| `/dungeon list` says `nothing yet (no blocks tagged …)` | The block doesn't implement `DungeonSealed`, or you haven't run `runData` since it did. Check `data/priestess/tags/blocks/sealed_by/<dungeon>.json`. |
| The block mines normally | You're in creative, `lockdown.enabled=false`, or that dungeon has no clear condition and so always reads cleared. |
| It cracks fully, shatters, then reappears | The client wasn't told your cleared set — a `DungeonSync` failure, not a lockdown one. Relog. |
| Mines with a slow punch and drops nothing | Missing `mineable/*` tag. See step 5; this is not the lockdown. |
| `runData` fails naming the new block | No loot table. See step 4. |
| The refusal message shows a raw key | The dungeon has no `dungeon.priestess.<name>` entry in `ModLanguageProvider`. |
| It's sealed forever with no way to clear it | The dungeon's boss or pickup item is `null`. Give it a clear condition. |
