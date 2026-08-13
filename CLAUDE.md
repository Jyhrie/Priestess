# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

A Minecraft **Forge 1.20.1** mod (Forge 47.4.10, Java 17) adding **Terra** — a standalone
Overworld-like dimension with 15 biomes on a hand-authored map, plus dungeons, bosses, and
progression gating.

| | |
|---|---|
| Mod ID | `priestess` |
| Dimension | `priestess:terra` |
| Base package | `com.jyhrie.priestess` |
| Sole dependency | GeckoLib 4.8.4 (boss animation) |
| Entering the dimension | `/execute in priestess:terra run tp @s ~ ~ ~` — no portal exists |

## Commands

```bash
./gradlew runData      # regenerate datapack JSON — after ANY worldgen/datagen change
./gradlew runClient    # launch the game
./gradlew build        # -> build/libs/priestess-1.0.0.jar
```

From PowerShell (the default shell here) use `.\gradlew.bat`; `./gradlew` works in the Bash tool.

**There are no automated tests.** `src/test/` is empty and no gametests are registered, despite
the `gameTestServer` run config existing in `build.gradle`. Verification is manual and in-game —
see the Testing section of `README.md` for command recipes.

## The critical workflow rule

**Almost nothing is registered at runtime.** The dimension, biomes, noise settings, structures,
damage types, block/item models, loot tables and lang are *datapack JSON generated from Java*:

```
edit Java  ──►  ./gradlew runData  ──►  src/generated/resources/**.json  ──►  loaded by the game
```

Consequences that cause silent, confusing failures:

- **Skipping `runData` means the game loads the old JSON** and the change does nothing.
- **Never hand-edit `src/generated/resources/`** — `runData` overwrites it.
- **Worldgen changes need a fresh world.** Existing chunks keep their old terrain forever;
  delete the test world in `run/saves/`.
- **A new block with no loot table fails datagen.** `getKnownBlocks()` reports every registered
  block, so `ModLootTableProvider` must cover it.

What *is* registered in code lives in `Priestess.java`: items, blocks, block entities, creative
tab, effects, entity types, the two worldgen codecs, the structure placement type, the Oripathy
capability, and the `DungeonSync` channel. The codecs must be registered in code because the
generated JSON can't be parsed until its codec exists in the registry.

## Architecture

Full per-directory map is in `README.md` § Project layout. The parts that need several files
to understand:

**Terra's geography is painted, not noised.** Three PNGs in
`src/main/resources/data/priestess/terra/` decide everything: `regions.png` (flat colour per
region), `elevation.png` (height), `relief.png` (ruggedness). `TerraMapBiomeSource` reads the
region, elevation picks one of eight terrain slots, and the pair picks the biome; the same
elevation drives the height splines via `TerraElevationFunction`/`TerraReliefFunction`.
Terra is **seed-independent** — identical in every world. Elevation and relief are deliberately
separate axes.

**`Dungeon.java` is the progression hub.** One enum constant per dungeon holds all three facts:
its structure, what clears it (boss kill or item pickup), and which biomes it ungrounds. Both
gating mechanics iterate that enum:
- *Lockdown* — a block in `priestess:sealed_by/<dungeon>` can't be mined until that dungeon is
  cleared. Blocks join the tag by extending `SealedBlock` and naming their dungeon;
  `ModBlockTagsProvider` derives the tag. Enforced on **both sides** (`DungeonSync` pushes the
  cleared set to clients) because mining is client-predicted — a server-only refusal makes
  blocks shatter and reappear.
- *Flight ban* — biomes refuse flight until their dungeon is cleared, by clearing vanilla
  `mayfly`/`flying` each tick.

Both are configured in `serverconfig/priestess-server.toml` (`PriestessConfig`, SERVER type, so
it lives in the world save). `lockdown.sharedProgress` switches between per-player and
world-wide storage; both always exist and only one is read.

**Mobs are prefixed by dungeon code**, and the code is *not* always the package initials:

| Dorothy's Vision | Mansfield Break | Under Tides (Sal Viento) | no dungeon |
|---|---|---|---|
| `dv_` | `mb_` | `sv_` | unprefixed |

So `DvFailure.java` → `dv_failure` → displayed as just "Failure". **Display names never carry
the prefix.** Each mob class's javadoc opens with its in-game name — when editing these, rename
identifiers freely but leave the prose alone. The authoritative table is in `README.md`
§ The dungeon code.

**`weapons/` is a self-contained, removable subsystem** ported in from Lethality. Its only
references from outside the folder are two lines in `Priestess.java` and one in
`ModCreativeTabs` — keep it that way.

**Oripathy** is a per-player infection capability (`oripathy/`), never shown in any UI. Creative
and spectator are exempt from gain and symptoms, so testing requires `/gamemode survival`.

**Content is scaffolding.** Mobs use placeholder cube models, dungeons are Python-generated
(`tools/`), and **nothing spawns naturally** — everything is reached via spawn egg or structure
placement.

## Conventions

- Mod metadata (id, name, version, description) lives in **`gradle.properties`**; `mods.toml`
  reads it via `${...}` substitution. Edit `gradle.properties`, never `mods.toml`.
- Comments in this codebase explain **why**, often at length, especially where a subtle
  Forge/Minecraft behaviour forced the design (see the mixin refmap block in `build.gradle`, or
  the codec notes in `Priestess.java`). Match that density and register rather than stripping
  it back to terse one-liners.
- GeckoLib is `implementation` + `fg.deobf()`, and dev runs remap its mixin refmap via two
  properties in `build.gradle` — without them the client dies before the main menu.

## Where to look

`README.md` is the primary reference (720 lines) and holds step-by-step recipes for adding an
item, a block, or a structure. Beyond it:

| Topic | Doc |
|---|---|
| Map, terrain, biomes, surface rules | `docs/WORLDGEN.md` |
| Every command + test recipes | `docs/COMMANDS.md` |
| Dungeon-gated blocks, adding a dungeon | `docs/DUNGEON_BLOCKS.md` |
| Storyline, chapter by chapter | `docs/SCORE_MOVEMENTS.md` |
| Per-boss design and fights | `docs/BOSSES.md` |
| Summoning altars, GeckoLib block models | `docs/BOSS_SPAWNERS.md` |
| Why nothing spawns, and how to change it | `docs/SPAWNING.md` |
| Weapons: click hooks, abilities, projectiles | `docs/WEAPONS.md` |
| Flowers/litter: cutout models, petal states | `docs/FLOWERS_LITTER.md` |
| Tooltips, rarities | `docs/TOOLTIPS.md`, `docs/RARITIES.md` |

---

**Keep this file current.** When something here stops being true — the workflow changes, a
subsystem is added or removed, a convention shifts, tests finally appear — update this file as
part of that same change rather than leaving it to drift.
