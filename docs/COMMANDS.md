# Commands

Every command this mod adds, and the vanilla ones you actually need to test it with.

Both mod commands are **op-only (permission level 2, `LEVEL_GAMEMASTERS`)** and both exist for
the same reason: the mechanics they touch are deliberately invisible or one-way, so without a
command there is no way to see what state a world is in, and no way to put it back.

For the mechanics themselves see [the README](../README.md#oripathy) and
[the dungeon code](../README.md#the-dungeon-code).

---

## Contents

- [`/oripathy`](#oripathy) — read and change the infection
- [`/dungeon`](#dungeon) — read and rewrite dungeon clear flags
  - [What `targets` means depends on the config](#what-targets-means-depends-on-the-config)
  - [`/dungeon placed`](#dungeon-placed)
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
/dungeon placed count                    placed-block exemptions in this dimension
/dungeon placed clear                    drop them all
```

This is purely a testing tool, and the mechanics need one. Both the
[lockdown](../README.md#the-dungeon-code) and the flight ban key off a flag that gameplay only
ever sets in *one* direction — killing the boss clears it, and nothing ever un-clears it. Without
`/dungeon seal` the only way to re-test a dungeon is to shut the world down and hand-edit
`data/priestess_dungeon_progress.dat`.

The dungeon names are the **enum constants lowercased, not the structure IDs** — they are what
ends up in save data, so they stay stable across a structure rename:

| Argument | Structure | Cleared by | Clearing it unlocks flight in |
|---|---|---|---|
| `mansfield_break` | `priestess:mansfield_state_prison` | killing Jesselton Williams | — |
| `dorothys_vision` | `priestess:dorothys_vision` | killing "Awaken" | — |
| `rhine_lab` | `priestess:rhine_lab_hq` | **picking up** the Originium Refinement Blueprint | `priestess:columbia` |
| `under_tides` | *none yet* | killing Bishop Quintus | — |
| `all` | | every one of the above at once | |

Every literal here is built in a loop from `Dungeon.values()`, so a new dungeon gets
tab-completion and validation for free without touching `DungeonCommand`.

`list` prints the storage mode first, then a line per dungeon:

```
Dungeon progress for Jyhrie — storage: shared (world-wide)
  mansfield_break: cleared
  dorothys_vision: SEALED
  rhine_lab: SEALED
  under_tides: cleared (nothing seals it — see Dungeon.java)
```

**`under_tides` reading as cleared is not a bug.** It has no structure declared yet, so it has
no physical extent, so there is nothing to seal — the lockdown skips it silently. The note is
printed for exactly this reason.

### What `targets` means depends on the config

`lockdown.sharedProgress` in `serverconfig/priestess-server.toml` decides where progress lives,
and it changes what `targets` does:

| `sharedProgress` | Where the record lives | What `targets` does |
|---|---|---|
| `true` *(default)* | one world-wide `SavedData` | **accepted and ignored** — the command says so rather than letting you believe you changed one person's progress |
| `false` | each player's Forge-persisted tag, kept through death | whose record to write; defaults to the sender |

`list` prints which mode is live, because guessing wrong is the obvious way to waste ten
minutes wondering why nothing happened.

### `/dungeon placed`

Every block a player puts down inside a sealed dungeon is recorded so they can take it back
out again — that is what makes the lockdown a rule about the dungeon rather than about the
player's hands. Those exemptions are stored **per dimension**.

- `count` — how many the current dimension holds. Handy for confirming the set actually
  shrinks when you break your own scaffolding back down.
- `clear` — drops them all, so the dungeon behaves as though nobody had ever built in it.
  That is the state you want back between test runs.

Both read `source.getLevel()`, so from the console — or from anywhere that is not Terra —
you'll be looking at the wrong dimension's set. Wrap it:
`/execute in priestess:terra run dungeon placed count`.

---

## Gotchas

| Symptom | Cause |
|---|---|
| "Nothing happened when I set another player's progress" | `sharedProgress=true`. There is one record; run `/dungeon list` and read the storage line. |
| `/dungeon list` or `/dungeon clear mansfield_break` errors from the console | Both fall back to *the sender* when no target is given, and the console is not a player. Name a target, or use `/execute as <player> run …`. |
| `/dungeon placed count` says 0 in a dungeon you just built in | You are in the wrong dimension. `/execute in priestess:terra run …`. |
| Oripathy never rises, symptoms never appear | Creative and spectator are exempt from ambient gain *and* from symptoms. `/gamemode survival`. |
| Blocks in a cleared dungeon still won't break | Progress is per player and this player hasn't cleared it, or `lockdown.enabled=false`. |
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
| Spawn eggs | `<entity>_spawn_egg` for every mob in [BOSSES.md](BOSSES.md) — e.g. `mb_jesselton_williams_spawn_egg`, `dv_awaken_spawn_egg`, `sv_bishop_quintus_spawn_egg` |

---

## Test recipes

**The lockdown, start to finish.** Nothing here needs a new world.

```
/gamemode survival
/execute in priestess:terra run tp @s ~ ~ ~
/dungeon seal mansfield_break                    put it back to uncleared
/dungeon placed clear                            forget last run's scaffolding
                                                 → mine a dungeon block: refused
                                                 → place a block, mine it back: works
/execute in priestess:terra run dungeon placed count
/dungeon clear mansfield_break                   → the same block now mines
```

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
