# Mob spawning

How to make a specific biome spawn a specific mob.

**Nothing in this mod spawns naturally right now.** Columbia used to spawn Originium Slugs and
that was removed along with the slug's mechanics, so every mob today is reached through a
spawn egg or placed by a structure. This document is how to put it back.

For the biomes themselves see [WORLDGEN.md](WORLDGEN.md); for the mobs see
[BOSSES.md](BOSSES.md) and `entity/ModEntities.java`.

---

## Contents

- [The one thing to get right](#the-one-thing-to-get-right) — it takes two edits, not one
- [The recipe](#the-recipe)
- [Part 1 — the placement rule](#part-1--the-placement-rule)
- [Part 2 — the biome entry](#part-2--the-biome-entry)
- [Weight and pack size](#weight-and-pack-size)
- [Mob categories](#mob-categories)
- [Which biome is where](#which-biome-is-where)
- [What needs regenerating](#what-needs-regenerating)
- [Worked example](#worked-example-put-the-slug-back-in-columbia)
- [It isn't spawning](#it-isnt-spawning)
- [The alternatives](#the-alternatives)

---

## The one thing to get right

A natural spawn is **two independent registrations in two different files**, and each one is
silently useless without the other:

| Half | File | Answers |
|---|---|---|
| `SpawnPlacements.register(...)` | `entity/ModEntities.java` | *Can it physically stand here?* — ground vs water, which heightmap, light and difficulty gates |
| `spawnBuilder.addSpawn(...)` | `world/dimension/ModBiomes.java` | *Should this biome produce it at all?* — and how often, and in what pack size |

Neither errors if the other is missing. A mob with a placement rule and no biome entry is
never picked as a candidate. A mob with a biome entry and no placement rule is picked and then
fails placement every time. Both look identical from in-game: nothing spawns, no log line.

This is the single most common reason "I added the spawn and nothing happened".

> Vanilla entity types come with their own placement rules already registered, so adding a
> zombie to a biome only needs the second half. Anything from this mod needs both.

---

## The recipe

1. Register the `EntityType` in `ModEntities` (see *Adding a mob* in that file's javadoc).
2. Add a `SpawnPlacements.register(...)` call in a `FMLCommonSetupEvent` handler in
   `ModEntities`. **There is currently no such method** — see
   [Part 1](#part-1--the-placement-rule) for the one to recreate.
3. Write a spawns `Consumer` in `ModBiomes` and pass it to the biome's `biome(...)` call.
4. `./gradlew runData`.
5. Restart the game and reload the world. A *new* world is not required — see
   [What needs regenerating](#what-needs-regenerating).

---

## Part 1 — the placement rule

Placement state is not thread safe and mod setup runs in parallel, so the call **must** be
inside `event.enqueueWork`. This method was deleted when the slug was stripped back; recreate
it in `ModEntities` exactly like this:

```java
@SubscribeEvent
public static void registerSpawnPlacements(FMLCommonSetupEvent event) {
    // Spawn placement state is not thread safe and setup runs in parallel.
    event.enqueueWork(() -> SpawnPlacements.register(ORIGINIUM_SLUG.get(),
            SpawnPlacements.Type.ON_GROUND,
            Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
            Monster::checkAnyLightMonsterSpawnRules));
}
```

You will need these imports back: `SpawnPlacements`, `Heightmap`,
`net.minecraft.world.entity.monster.Monster`, `FMLCommonSetupEvent`.

### The four arguments

**`SpawnPlacements.Type`** — what it needs underfoot.

| Value | For |
|---|---|
| `ON_GROUND` | anything that walks. The block below must be a valid spawn surface. |
| `IN_WATER` | fish, squid, drowned |
| `IN_LAVA` | striders |
| `NO_RESTRICTIONS` | fliers and anything that supplies its own rule entirely |

**`Heightmap.Types`** — which surface the game measures to find a candidate Y.
`MOTION_BLOCKING_NO_LEAVES` is the right default for a surface mob: it ignores leaves, so mobs
do not spawn on treetops. `WORLD_SURFACE` includes them. `OCEAN_FLOOR` is for things that want
the seabed.

**The predicate** — the gate that runs at each candidate position. The choice that matters:

| Predicate | Gates on |
|---|---|
| `Monster::checkMonsterSpawnRules` | **darkness** + difficulty + a valid block below |
| `Monster::checkAnyLightMonsterSpawnRules` | difficulty + a valid block below — **no light check** |

Terra's surface mobs use `checkAnyLightMonsterSpawnRules` deliberately. The wastes are meant to
be a *daylight* problem: a mob that only appears at night reads as a vanilla monster, and the
point of Columbia is that the open ground is hostile at noon. Both refuse to spawn on Peaceful.

You can also pass your own lambda for a bespoke rule — "only above y 120", "only on
Iberian Sand" — with the same signature.

---

## Part 2 — the biome entry

`ModBiomes` already has the machinery. The `biome(...)` helper has two overloads:

```java
// no spawns — what every biome currently uses
private static Biome biome(context, hasPrecipitation, temperature, downfall, palette)

// with spawns
private static Biome biome(context, hasPrecipitation, temperature, downfall, palette,
                           Consumer<MobSpawnSettings.Builder> spawns)
```

So populating a biome is: write a `private static void` that takes a
`MobSpawnSettings.Builder`, and hand it to that biome's registration as a method reference.

```java
private static void columbiaSpawns(MobSpawnSettings.Builder spawnBuilder) {
    spawnBuilder.addSpawn(MobCategory.MONSTER,
            new MobSpawnSettings.SpawnerData(ModEntities.ORIGINIUM_SLUG.get(), 40, 2, 4));
}
```

```java
context.register(COLUMBIA, biome(context, true, 0.45F, 0.8F, P_COLUMBIA,
                                 ModBiomes::columbiaSpawns));
```

Several mobs in one biome is several `addSpawn` calls in the same method. Several biomes
sharing a population is one method referenced from several `context.register` calls — there is
no reason to duplicate it.

`SpawnerData` is `(EntityType<?> type, int weight, int minCount, int maxCount)`.

Two other `MobSpawnSettings.Builder` methods you will rarely want:

- `creatureGenerationProbability(float)` — the chance of the one-off world-generation pass that
  places passive animals when a chunk is first generated. Irrelevant to monsters.
- `addMobCharge(EntityType, double energyBudget, double charge)` — vanilla's density damping,
  used for things like the soul-sand-valley skeleton cap. Leave it alone unless a mob is
  genuinely overrunning a biome.

---

## Weight and pack size

```java
new MobSpawnSettings.SpawnerData(ModEntities.ORIGINIUM_SLUG.get(), 40, 2, 4)
//                                                                 ^   ^  ^
//                                                            weight  min max
```

**Weight is relative, and only against other entries in the same `MobCategory` in the same
biome.** It is not a percentage and not a rate. One entry at weight 40 and one entry at weight
40 is a 50/50 split; a single entry at weight 40 behaves identically to a single entry at
weight 1, because it wins every roll either way. Changing the weight of the only monster in a
biome does nothing at all — this surprises people.

**Pack size is what actually sets density.** `min`/`max` is how many attempt to spawn per
successful roll, as a group in one place. Two to four is constant low pressure; eight to twelve
is a swarm that will wall off a corridor.

To make a biome *busier*, raise the pack size or add more entries — not the weight.

---

## Mob categories

The category decides which population cap the mob is counted against, whether it despawns, and
which of the game's separate spawn passes it rides. Values below are read out of
`net.minecraft.world.entity.MobCategory` for 1.20.1:

| Category | Cap | Friendly | Persistent | No-despawn radius |
|---|---:|:---:|:---:|---:|
| `MONSTER` | 70 | no | no | 128 |
| `CREATURE` | 10 | yes | yes | 128 |
| `AMBIENT` | 15 | yes | no | 128 |
| `AXOLOTLS` | 5 | yes | no | 128 |
| `UNDERGROUND_WATER_CREATURE` | 5 | yes | no | 128 |
| `WATER_CREATURE` | 5 | yes | no | 128 |
| `WATER_AMBIENT` | 20 | yes | no | 64 |
| `MISC` | −1 | yes | yes | 128 |

**The cap is not per world.** `NaturalSpawner` scales it by how much of the world is currently
loaded — `cap × spawnableChunks / 289`, where 289 is `17²`, the chunk area around one player.
So "70 monsters" means roughly 70 within one player's spawn area, and a second player elsewhere
gets their own budget.

Two consequences worth knowing:

- **The cap is shared across every monster in the dimension.** Adding a second monster to
  Columbia does not double the population, it splits the existing budget.
- `MONSTER` mobs despawn beyond 128 blocks. That is usually what you want; a mob that must
  persist wants `setPersistenceRequired()` on the entity, not a different category.

Everything this mod registers is `MobCategory.MONSTER`, set in the `EntityType.Builder` chain
in `ModEntities`. That is a property of the *entity type*, not of the spawn entry — the
category in `addSpawn` must match the one the type was registered with, or the entry sits in a
list the spawner never consults for it.

---

## Which biome is where

Terra's biomes are placed by a **hand-painted map**, not by climate. So "make X spawn in the
southern desert" is really "find which biome the southern desert is painted as, and populate
that biome". `regions.png` is the authority; `TerraRegion.java` maps colour to biome key.

The registered biomes, from `ModBiomes`:

```
OCEAN            INFY_ICEFIELD    SAMI             URSUS_COLD
URSUS_DRY        URSUS_WARM       KJERAG           MOUNT_KARLAN
KAZIMIERZ        COLUMBIA         IBERIA_LAND      YAN
HIGASHI_COLD     HIGASHI_WARM     KAZDEL           TEMPORARY_LAYER
```

`TEMPORARY_LAYER` is unzoned ground and deliberately hideous — do not populate it. `COLUMBIA`
is where the player spawns and where the first chapter happens, so it is the one that usually
matters.

To find a biome in-game: `/locate biome priestess:columbia`, or F3 and read the biome line.

---

## What needs regenerating

`./gradlew runData` — **always**. Biome definitions are datapack JSON generated from the Java,
so an edit to `ModBiomes` that is not followed by `runData` changes nothing at all. Confirm it
landed:

```bash
python -c "import json;print(json.load(open('src/generated/resources/data/priestess/worldgen/biome/columbia.json'))['spawners']['monster'])"
```

An empty `[]` there means the edit did not take.

**A fresh world is not required for spawn-list changes**, which is the exception to the blanket
rule in the main README. A chunk stores *which biome* it is; the biome's spawner list lives in
the datapack and is re-read when the world loads. So terrain and biome placement are baked into
old chunks forever, but spawn lists are not — restart the game, load the existing world, and
the new list is live.

> This is the one claim in this document derived from how the worldgen registries load rather
> than confirmed in-game. If a spawn refuses to appear in an old save and works in a new one,
> trust the save and correct this line.

---

## Worked example: put the slug back in Columbia

The exact code that was removed, in both files.

**`entity/ModEntities.java`** — recreate the deleted method and its four imports:

```java
@SubscribeEvent
public static void registerSpawnPlacements(FMLCommonSetupEvent event) {
    event.enqueueWork(() -> SpawnPlacements.register(ORIGINIUM_SLUG.get(),
            SpawnPlacements.Type.ON_GROUND,
            Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
            Monster::checkAnyLightMonsterSpawnRules));
}
```

**`world/dimension/ModBiomes.java`** — add the method back and re-point the registration:

```java
private static void columbiaSpawns(MobSpawnSettings.Builder spawnBuilder) {
    spawnBuilder.addSpawn(MobCategory.MONSTER,
            new MobSpawnSettings.SpawnerData(ModEntities.ORIGINIUM_SLUG.get(), 40, 2, 4));
}
```

```java
context.register(COLUMBIA, biome(context, true, 0.45F, 0.8F, P_COLUMBIA,
                                 ModBiomes::columbiaSpawns));
```

Restore the `MobCategory` and `ModEntities` imports in `ModBiomes`, then `./gradlew runData`.

---

## It isn't spawning

Work down this list in order — it is sorted by how often each one is the answer.

1. **Did you do both halves?** See [the top](#the-one-thing-to-get-right). This is the answer
   most of the time.
2. **Did you run `runData`?** Check the generated `columbia.json` as above.
3. **Are you in the right biome?** F3, or `/locate biome`. Terra is 65,536 blocks across and
   biome edges follow a painted map, not anything you can eyeball.
4. **Are you in Survival?** Peaceful blocks every monster rule, and both `Monster::check…`
   predicates test difficulty first.
5. **Is the mob cap already full?** 70 monsters is shared across the whole loaded area. If
   something else is flooding the dimension, nothing new gets a slot.
6. **Are you standing too close?** Vanilla will not spawn a monster within 24 blocks of a
   player. Fly out and come back.
7. **Does the category match?** The `MobCategory` in `addSpawn` must equal the one passed to
   `EntityType.Builder.of(...)`.
8. **Does the placement rule pass where you are standing?** `ON_GROUND` plus
   `MOTION_BLOCKING_NO_LEAVES` will refuse foliage, water and anything the block below reports
   as an invalid spawn surface.

To prove the mob itself is fine before blaming the spawn wiring, use its egg:

```
/give @s priestess:originium_slug_spawn_egg
```

If the egg works and the natural spawn does not, the problem is in one of the two halves — not
in the mob.

---

## The alternatives

Natural spawning is one of three ways a mob reaches a player, and often the wrong one.

**Structure placement.** Both bosses are placed by the dungeon that owns them, baked into the
`.nbt`. This is right for anything that belongs to a place: a lab's guards found wandering the
open wasteland give away a lab the player has not found yet. It is also the answer for the
three Medium-bearers once their dungeon is decided.

**Spawn eggs.** Every mob has one, and they cost nothing — the model is a vanilla template.
This is the testing path and, for anything not yet placed, currently the only path.

**Biome modifiers, for biomes you do not own.** Everything above works because this mod defines
its own biomes and can put whatever it likes in them. To add a mob to a *vanilla* biome, or one
from another mod, you cannot edit its definition — you write a Forge **biome modifier** JSON
(`data/priestess/forge/biome_modifier/`) using `forge:add_spawns`. Nothing in this mod needs
that yet, since Terra is self-contained.
