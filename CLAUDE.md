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
| Dependencies | GeckoLib 4.8.4 (boss animation), Curios 5.14.1 (accessory slots) |
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

**Every combat number is config, and nothing reads it where it is declared.** `config/` holds
four **COMMON** configs — `config/priestess/boss.toml`, `miniboss.toml`, `mob.toml` and
`weapon.toml`, one class each in `com.jyhrie.priestess.config`, split because the four are tuned
at different times by different questions. Unlike the SERVER file above they belong to the
installation, so one edit retunes every world. Between them: the six attributes for all sixteen
creatures, the three weapons' damage and swing speed, and every ability damage. `Stats` holds
what they share — the `Block`/`Weapon` types, the bounds, and the act of writing to an entity.

The timing is the whole design: attribute suppliers are built once during mod loading and item
modifiers once during registration, both racing config load and both frozen for the process
afterwards, so

- the `attributes()` methods and the `SwordItem` constructor keep the compiled numbers as
  **defaults**, and
- `entity/EntityStats` writes the configured values over them on `EntityJoinLevelEvent` —
  *after* NBT load, which is what makes the config authoritative rather than advisory — while
  `weapons/item/ConfiguredSwordItem` builds its modifier map from the config per call.

Because both read fresh, an edit to the toml takes effect **without a restart** — weapons at
once, mobs the next time they join a level. Other consequences: **editing a number in an
`attributes()` method alone does nothing**; only base values are written, so runtime *modifiers*
survive (which is why the miniboss enrages with one); a mob in an already-loaded chunk keeps its
old numbers until that chunk cycles; and because weapon abilities scale off
`WeaponText.itemAttackDamage`, raising a weapon's `attackDamage` raises everything it throws.
COMMON is not synced, so a client with a divergent file sees wrong tooltips while the server
still deals its own numbers. Cooldowns, ranges and geometry are deliberately *not* configurable.
See `docs/STATS.md`.

**Mobs are prefixed by dungeon code**, and the code is *not* always the package initials:

| Dorothy's Vision | Mansfield Break | Under Tides (Sal Viento) | no dungeon |
|---|---|---|---|
| `dv_` | `mb_` | `sv_` | unprefixed |

So `DvFailure.java` → `dv_failure` → displayed as just "Failure". **Display names never carry
the prefix.** Each mob class's javadoc opens with its in-game name — when editing these, rename
identifiers freely but leave the prose alone. The authoritative table is in `README.md`
§ The dungeon code.

**`weapons/` is a self-contained subsystem** — its only references from outside the folder are
two lines in `Priestess.java` and one in `ModCreativeTabs`. Keep it that way. It was originally
all ported Lethality content and therefore disposable; **it no longer is** — Laevatain is
original, so the compartment is still a compile boundary but deleting it now costs real content.
New weapons go here regardless, because the scaffolding (`WeaponTiers`, `WeaponText`,
`ConfiguredSwordItem`, the swing packet) lives here — extend `ConfiguredSwordItem`, never
`SwordItem` directly, or the weapon's numbers stop answering to the config. Note `ItemCooldowns` holds one timer per *item*, so a multi-ability weapon
keeps its extra cooldowns as game-time stamps in stack NBT. See `docs/WEAPONS.md`.

**Ability visuals are entities, not particles.** `WeaponVfx` is a short-lived, damageless
`GeoEntity` that plays one GeckoLib clip and discards itself; `WeaponVfxModel` derives geo,
texture and animation paths from the entity type's registry name, so a new effect is one
registration line plus three assets and no new Java. These are the **only animated models in
the mod** — every mob and block model is static geometry with an empty controller. The clip is
always named `play`, the bone name must match between `.geo.json` and `.animation.json` (a
mismatch fails silently), and the entity's lifetime in ticks must equal the JSON's
`animation_length` in seconds × 20.

**Oripathy** is a per-player infection capability (`oripathy/`), never shown in any UI. Creative
and spectator are exempt from gain and symptoms, so testing requires `/gamemode survival`.

**Curios accessories are three files, two of which fail silently.** The mod adds one slot,
`module`, holding one item, `Template`. Which slot an item fits is *not* decided in Java — it
comes from an item tag in the **`curios` namespace, not `priestess`** (`ModTags.Items`
wraps this). Registering the item is not enough: without the `ModItemTagsProvider` entry it is
equippable nowhere, and without `ModCuriosDataProvider` the slot never appears in the GUI.
Neither omission logs anything. See `docs/CURIOS.md`.

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
  properties in `build.gradle` — without them the client dies before the main menu. Curios ships
  mixins too and relies on those same two properties; it compiles against its slim `:api`
  classifier with the full artifact pulled at runtime.
- Placeholder textures are generated, not drawn: `tools/generate_placeholder_art.py` is pure
  stdlib, seeded and idempotent. Add an entry there rather than hand-making a PNG, and remove
  an entry once real art replaces it so a re-run cannot clobber the real thing.
- Adding a weapon touches seven files in five packages — the item class, `config/WeaponStats`,
  `ModWeapons`, `ModLanguageProvider`, `ModItemModelProvider`, the placeholder-art table, and
  both ends of the swing packet. Missing one usually fails *silently* rather than at compile
  time: a weapon with no name, no texture, or one that swings and never fires. `docs/WEAPONS.md`
  is the checklist; work through it rather than from memory.

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
| The balance config: mob, boss, weapon and ability numbers | `docs/STATS.md` |
| Summoning altars, GeckoLib block models | `docs/BOSS_SPAWNERS.md` |
| Why nothing spawns, and how to change it | `docs/SPAWNING.md` |
| Weapons: click hooks, abilities, projectiles | `docs/WEAPONS.md` |
| Curios: the Module slot, adding a module | `docs/CURIOS.md` |
| Flowers/litter: cutout models, petal states | `docs/FLOWERS_LITTER.md` |
| Tooltips, rarities | `docs/TOOLTIPS.md`, `docs/RARITIES.md` |

---

**Keep this file current.** When something here stops being true — the workflow changes, a
subsystem is added or removed, a convention shifts, tests finally appear — update this file as
part of that same change rather than leaving it to drift.
