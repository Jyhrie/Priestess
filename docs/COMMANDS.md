# Commands

Every command this mod adds, and the vanilla ones you actually need to test it with.

Both mod commands are **op-only (permission level 2, `LEVEL_GAMEMASTERS`)** and both exist for
the same reason: the mechanics they touch are deliberately invisible or one-way, so without a
command there is no way to see what state a world is in, and no way to put it back.

For the mechanics themselves see [Oripathy](../README.md#oripathy) and
[Progression](../README.md#progression) in the README.

---

## Contents

- [`/oripathy`](#oripathy) — read and change the infection
- [`/dungeon`](#dungeon) — read and rewrite dungeon clear flags
  - [What `targets` means depends on the config](#what-targets-means-depends-on-the-config)
- [Gotchas](#gotchas) — read this before filing a bug against yourself
- [Vanilla commands worth knowing](#vanilla-commands-worth-knowing)
  - [IDs you will be typing](#ids-you-will-be-typing)
- [Test recipes](#test-recipes)
- [Adding a command](#adding-a-command)

---

## `/oripathy`

Oripathy is a hidden number on the player, from 1 to 10000. Nothing in the UI shows it, so
this command is the only way to read it.

```
/oripathy get [target]            defaults to yourself
/oripathy set <targets> <value>   1..10000
/oripathy add <targets> <amount>  negative to treat; result is clamped
```

`get` prints the value, the **stage name**, and — when there is one — how much a flare-up
still has to drain off:

```
Jyhrie has 7500 oripathy (weakness II, slowness II)
Jyhrie has 1900 oripathy (asymptomatic, 900 still draining off)
```

The stage is printed rather than left as a number so you can tell at a glance which threshold
a value falls under:

| Value | Stage printed | What the player gets |
|---|---|---|
| < 5000 | `asymptomatic` | nothing |
| ≥ 5000 | `slowness II` | Slowness II |
| ≥ 7500 | `weakness II, slowness II` | + Weakness II |
| ≥ 9000 | `blindness, weakness II, slowness II` | + Blindness |
| 10000 | `terminal` | death |

The "still draining off" line is the only way to observe Acute Oripathy working. That effect
adds a dose and then refunds most of it over its duration; the outstanding amount lives on the
capability, not on the effect, so it survives milk and logout — and it is completely invisible
without this. See [Death and respawn](../README.md#death-and-respawn) for what a death does to
it.

`set` takes 1..10000 and `add` takes -10000..10000, both enforced by Brigadier, so a bad value
is a red parse error rather than a silent clamp. The *result* of `add` is clamped, though:
`add @s -99999` is the reliable "cure them" and won't underflow.

---

## `/dungeon`

```
/dungeon list [target]                   what is cleared, and which storage is live
/dungeon clear <dungeon|all> [targets]   mark cleared
/dungeon seal  <dungeon|all> [targets]   mark uncleared
```

This is purely a testing tool, and the mechanics need one. The lockdown and the flight ban key
off a flag that gameplay only ever sets in *one* direction — killing the boss clears it, and
nothing ever un-clears it. Without `/dungeon seal` the only way to re-test a dungeon is to shut
the world down and hand-edit the save data.

**A dungeon flag gates one thing**: every block in `priestess:sealed_by/<dungeon>`, **wherever
in the world it stands**. A block joins that tag by extending `SealedBlock` and naming its
dungeon, and `ModBlockTagsProvider` derives the tag from that — see
[docs/DUNGEON_BLOCKS.md](DUNGEON_BLOCKS.md). Two sets are gated today: the Rhine Lab Arts Lab
set (5 blocks) behind `dorothys_vision`, and the Sal Viento Catacombs set (2) behind
`under_tides`.

**A dungeon does not seal an area.** Standing inside an uncleared dungeon does not stop you
mining, so its pipes, doors, lamps and other fittings all come out normally — only the tagged
blocks refuse. That is deliberate: sealing the structure also sealed the generator's backfill
and every piece of furniture in the build.

**A gate is only as strong as the cheapest way around it**, and mining is not the only way a
block leaves the world. The Arts Lab set is therefore also immune to the three things that
would otherwise move a wall nobody is allowed to mine. These are properties of the blocks, not
of your progress — unlike the mining gate, **they do not lift when the dungeon is cleared**,
because none of the three has a player to ask permission of:

| Vector | How it is refused | Declared in |
|---|---|---|
| Explosions (TNT, creepers, anything) | bedrock's blast resistance, `3600000` | `ModBlocks.artsLab()` |
| Pistons pushing or pulling | `PushReaction.BLOCK` | `ModBlocks.artsLab()` |
| The wither eating through | `#minecraft:wither_immune` | `ModBlockTagsProvider` → `data/minecraft/tags/blocks/wither_immune.json` |

The dungeon names are the **enum constants lowercased**, which is what ends up in save data:

| Argument | Cleared by | Clearing it unlocks flight in |
|---|---|---|
| `mansfield_break` | killing Jesselton Williams | — |
| `dorothys_vision` | killing "Awaken" | — |
| `rhine_lab` | **picking up** the Originium Refinement Blueprint | `priestess:columbia` |
| `under_tides` | killing Bishop Quintus | — |
| `all` | every one of the above at once | |

Every literal here is built in a loop from `Dungeon.values()`, so a new dungeon gets
tab-completion and validation for free without touching `DungeonCommand`.

`list` prints the storage mode first, then a line per dungeon — and after each flag, **how many
block types that flag actually holds shut**:

```
Dungeon progress for Jyhrie — storage: per player
  mansfield_break: cleared — nothing yet (no blocks tagged sealed_by/mansfield_break)
  dorothys_vision: SEALED — 5 block types
  rhine_lab: SEALED — nothing yet (no blocks tagged sealed_by/rhine_lab)
  under_tides: SEALED — 2 block types
```

A dungeon gating nothing is not a bug — the flag is real, nothing is simply tagged to it yet.
That is what `mansfield_break` and `rhine_lab` look like above. A dungeon that *nothing can
clear* is called out separately: it always reads as cleared, because a seal with no key is the
one state a player cannot recover from.

### What `targets` means depends on the config

`lockdown.sharedProgress` in `serverconfig/priestess-server.toml` decides where progress lives,
and it changes what `targets` does:

| `sharedProgress` | Where the record lives | What `targets` does |
|---|---|---|
| `false` *(default)* | each player's Forge-persisted tag, kept through death | whose record to write; defaults to the sender |
| `true` | one world-wide `SavedData` | **accepted and ignored** — the command says so rather than letting you believe you changed one person's progress |

> The config file is written once per world, so **a world that already exists keeps whatever
> it was created with**. Check with `/dungeon list` rather than assuming the default.

`list` prints which mode is live, because guessing wrong is the obvious way to waste ten
minutes wondering why nothing happened.

---

## Gotchas

| Symptom | Cause |
|---|---|
| "Nothing happened when I set another player's progress" | `sharedProgress=true`. There is one record; run `/dungeon list` and read the storage line. |
| `/dungeon list` or `/dungeon clear mansfield_break` errors from the console | Both fall back to *the sender* when no target is given, and the console is not a player. Name a target, or use `/execute as <player> run …`. |
| A gated block you placed yourself won't break | Working as intended — the lockdown does not ask who put a block there. Clear the dungeon, or use creative. |
| Oripathy never rises, symptoms never appear | Creative and spectator are exempt from ambient gain *and* from symptoms. `/gamemode survival`. |
| A gated block still won't break after clearing | Progress is per player and this player hasn't cleared it, or `lockdown.enabled=false`. |
| I'm standing in an uncleared dungeon and its floor/pipes/doors mine fine | Working as intended. The lockdown gates **tagged blocks**, not an area — only the declared build set refuses. |
| Turned `lockdown.enabled` off but a gated block still won't start mining | The client is told the switch on login, on respawn, on a dimension change and on any `/dungeon` write. Relog, or run one `/dungeon clear`/`seal`. |
| A Rhine Lab Arts Lab block won't break anywhere, even outside a dungeon | Working as intended — it is gated by block type, not by place. `/dungeon clear dorothys_vision` for that player. |
| Cleared `dorothys_vision`, but an Arts Lab block still won't move with a piston or blow up | Working as intended. The blast, piston and wither immunities are permanent properties of the blocks and never lift; only the *mining* gate is tied to progress. Mine it and place it where you want it. |
| A gated block cracks all the way, shatters, then reappears | The client has not been told what you have cleared, so it is predicting a break the server then refuses. That is the `DungeonSync` channel failing — relog and check the server log. |
| Still flying in Columbia after clearing Rhine Lab | `flight.exemptCreative=true` and you are in creative. Flight is also only re-checked on tick, not instantly on the command. |
| A boss altar won't fire again | It is spent until that boss dies. Re-arm it — see [below](#vanilla-commands-worth-knowing). |

---

## Vanilla commands worth knowing

```
/execute in priestess:terra run tp @s ~ ~ ~       enter the dimension
/execute in minecraft:overworld run tp @s ~ ~ ~   leave it
/locate biome priestess:infy_icefield             find a biome
/place structure priestess:mansfield_state_prison force-place a dungeon here
/effect give @s priestess:open_wounds 60 2        Open Wounds III for a minute
/effect give @s priestess:acute_oripathy 5 2      +1000, then -900 over those 5 s
```

Re-arming a spent boss altar is a `/setblock` with the blockstate, because `armed` lives in
the blockstate rather than only in the block entity (the model changes with it, and the ticker
is only attached while it is spent):

```
/setblock ~ ~ ~ priestess:jesselton_projector[armed=true] replace
```

Summoning a boss the intended way needs its catalyst in hand:

| Altar | Catalyst to hold | Boss |
|---|---|---|
| `priestess:jesselton_projector` | `priestess:tarnished_dog_tags` | Jesselton Williams |
| `priestess:dorothys_terminal` | `priestess:corrupted_neural_shard` | "Awaken" |

`/summon` works too, but skips the clearance check the altar does — "Awaken" is 6.75 blocks on
a side and cannot move once placed, so summoning it in a tight room gets you a boss stuck in a
wall.

### IDs you will be typing

All under the `priestess:` namespace.

| Kind | IDs |
|---|---|
| Dimension | `terra` |
| Structures | `mansfield_state_prison`, `dorothys_vision`, `rhine_lab_hq`, `infy_ice_spike` |
| Biomes | `columbia`, `infy_icefield`, `sami`, `ursus_cold`, `ursus_dry`, `ursus_warm`, `kjerag`, `mount_karlan`, `kazimierz`, `iberia_land`, `yan`, `higashi_cold`, `higashi_warm`, `kazdel`, `ocean` |
| Effects | `open_wounds`, `acute_oripathy` |
| Progression items | `mansfield_master_key`, `blueprint_originium_refinement`, `dorothys_neural_processor`, `tarnished_dog_tags`, `corrupted_neural_shard` |
| Gated blocks | `rhine_lab_arts_lab_chiseled_wall`, `..._plated_wall`, `..._concrete_wall`, `rhine_lab_arts_lab_tile`, `rhine_lab_arts_lab_pillar` — behind `dorothys_vision`; `sal_viento_catacombs_stone`, `sal_viento_catacombs_overgrown_stone` — behind `under_tides`. All blast-, piston- and wither-proof for good |
| Spawn eggs | `<entity>_spawn_egg` for every mob in [BOSSES.md](BOSSES.md) — e.g. `mb_jesselton_williams_spawn_egg`, `dv_awaken_spawn_egg`, `sv_bishop_quintus_spawn_egg` |

---

## Test recipes

**The lockdown, start to finish.** Gated blocks are refused wherever they stand, so this needs
no dungeon and no new world — put one on the ground in front of you and try.

```
/gamemode survival
/give @s priestess:rhine_lab_arts_lab_tile          you can place it…
/dungeon seal dorothys_vision                       …but not take it back up
/dungeon list                                       "dorothys_vision: SEALED — 5 block types"
/dungeon clear dorothys_vision                      → it mines, and drops itself
```

There is no exemption for placing it yourself — the rule is about the block, not about who put
it there — so the block you just set down is the block that refuses to come back up.

`under_tides` works the same way with `priestess:sal_viento_catacombs_stone`.
`mansfield_break` and `rhine_lab` gate nothing, though — their flags are real but no blocks are
tagged to them, so `/dungeon seal mansfield_break` will not make anything refuse. `/dungeon
list` prints the block count per dungeon, which is the fastest way to tell "gate not working"
from "gate is empty".

**What "refuses" should look like.** No crack overlay, no digging sound, no progress of any
kind: the block does not move, you get the action-bar line, and that is all. Small hit
particles and the arm swing still happen, exactly as they do when you punch bedrock — those
are the client's response to the click itself and are not mining progress. If you instead see
the block crack through all ten stages, shatter and reappear, the client has not been told
what you have cleared; that is the `DungeonSync` channel failing, not the lockdown.

**The other three ways in.** None of these care whether you have cleared anything, so test
them cleared — if they hold with the gate open they hold with it shut.

```
/dungeon clear dorothys_vision                      progress is irrelevant here
/setblock ~2 ~ ~ priestess:rhine_lab_arts_lab_tile
/summon tnt ~2 ~1 ~ {fuse:1s}                       → block survives
/give @s piston                                     → aim one at it: refuses to extend
/summon wither ~5 ~ ~                               → it chews the floor, not the tile
```

Sticky pistons are the case worth trying deliberately: `PushReaction.BLOCK` refuses the *pull*
as well as the push, so a block that a piston cannot shove is also one it cannot drag out of a
wall from the far side.

**Flight.** Columbia is grounded until Rhine Lab is done, which is the point of the mechanic.

```
/dungeon seal rhine_lab
/gamemode survival                               creative is exempt by default
                                                 → fly in Columbia: dropped
/dungeon clear rhine_lab                         → flight comes back
```

**Oripathy.** It is invisible, so bracket everything with `get`.

```
/gamemode survival
/oripathy set @s 1
/effect give @s priestess:acute_oripathy 5 2
/oripathy get                                    spam it and watch the number fall
```

---

## Adding a command

Commands are registered on Forge's `RegisterCommandsEvent`, from **the feature's own event
subscriber** rather than a shared command-registry class — `OripathyEvents` registers
`/oripathy`, `DungeonLockdown` registers `/dungeon`. One Forge-bus class per feature, and the
command is part of the feature.

```java
@SubscribeEvent
public static void registerCommands(RegisterCommandsEvent event) {
    MyCommand.register(event.getDispatcher());
}
```

Two conventions worth keeping:

- **Gate on `Commands.LEVEL_GAMEMASTERS`** in `.requires(...)`. Everything here rewrites state
  a survival player is supposed to earn.
- **Prefer a literal per value over a string argument.** Both mod commands build their literals
  from an enum's `values()`, which buys tab-completion and validation for free; a string
  argument needs a suggestion provider and its own error handling to reach the same place.
