# Bosses

How a Columbia boss is put together, walked through with **Jesselton Williams** — the first
one, and the one every other boss in the chapter is a variation on.

For the chapter as a whole see [Content and storyline](../README.md#content-and-storyline) in
the README, or [docs/SCORE_MOVEMENTS.md](SCORE_MOVEMENTS.md) for the full thing.

---

## Contents

- [The shared skeleton](#the-shared-skeleton) — what every boss inherits
- [Arts beams](#arts-beams) — the ranged attack all of them use
- [Jesselton Williams](#jesselton-williams)
  - [Numbers](#numbers)
  - [The two phases](#the-two-phases)
  - [The tick](#the-tick)
  - [Summoning — removed](#summoning--removed)
  - [Staying put](#staying-put)
  - [The drop](#the-drop)
- [Known rough edges](#known-rough-edges)

---

## The shared skeleton

Every boss in the chapter is a plain `Monster` subclass. There is no boss framework, no
phase state machine, no abstract base class — the logic lives in `customServerAiStep()` and
that is deliberate. Two bosses is not enough to know what a base class should factor out,
and a wrong abstraction costs more than the duplication.

What they all do have:

| Piece | What it does | Why |
|---|---|---|
| `ServerBossEvent bossEvent` | the purple/red bar at the top of the screen | Constructed with the entity's own translation key, so the bar name follows the lang file |
| `startSeenByPlayer` / `stopSeenByPlayer` | adds and removes players from the bar | Vanilla calls these on tracking changes; the bar is not automatic |
| `setCustomName` override | re-labels the bar when the entity is renamed | Otherwise a name tag renames the mob and not the bar |
| `removeWhenFarAway → false` | never despawns | "A boss that despawns because nobody stood near it is a boss you can lose" |
| `canChangeDimensions → false` | cannot be portalled out | |
| `isPushable → false` | cannot be shoved by mobs or pistons | |
| `KNOCKBACK_RESISTANCE 1.0` | cannot be knocked back | By **attribute**, not by a special case in `hurt` — so a shield bash visibly moves everything in the cell block except him |
| `dropCustomDeathLoot` override | the guaranteed progression item | Not a loot table — see [The drop](#the-drop) |

Note what is **not** there: no `addAdditionalSaveData` / `readAdditionalSaveData` on
Jesselton. The Failed Vision does override those, because its node count is a puzzle state
that has to survive a reload. Jesselton's phase is derived from health, which already
saves — mostly. See [Known rough edges](#known-rough-edges).

---

## Arts beams

`ArtsBeam` is a static helper, not an entity. Every ranged attack in Columbia goes through
it:

```java
ArtsBeam.fire(source, target, damageType, damage, particle);
```

It is **hitscan** — it lands the instant it is fired:

1. take `source.getEyePosition()` and the target's chest (`position + bbHeight * 0.5`)
2. draw a line of particles between them, 2 per block, count 1 with zero speed and zero
   spread so each particle sits exactly on the line
3. `target.hurt(...)` with a damage source attributed to the mob

No projectile entity, which would mean an entity type, a renderer and network tracking —
three files — for something the player experiences as *the ghost pointed at me and I took
damage*. Arts in Arknights are not arrows; they arrive.

The cost is that **there is nothing to dodge once it is fired**. The counterplay has to be
line of sight, which is why every caller checks `hasLineOfSight` first and why the dungeons
are built with pillars in them.

### The damage types

Both of Jesselton's attacks are the same beam. The only thing that changes is the damage
type, and that is the whole design:

| Type | Phase | Tag | Effect |
|---|---|---|---|
| `priestess:spectral_arts` | one | — | Ordinary kinetic. Every point of armour you looted subtracts from it. |
| `priestess:void_arts` | two | `minecraft:bypasses_armor` | Armour does nothing. |

Both scale `WHEN_CAUSED_BY_LIVING_NON_PLAYER`, like every vanilla mob attack, so Hard hurts
more than Easy. Declared in `ModDamageTypes`, written out as datapack JSON by `runData`.

---

## Jesselton Williams

`src/main/java/com/jyhrie/priestess/entity/bosses/MbJesseltonWilliams.java`

The mercenary who tried to take Mansfield and got locked in it instead. He lives in the
prison and gates the rest of the chapter behind the Mansfield Master Key.

### Numbers

```java
MAX_HEALTH            220.0     ARMOR                  6.0
MOVEMENT_SPEED        0.28      KNOCKBACK_RESISTANCE   1.0
ATTACK_DAMAGE         8.0       FOLLOW_RANGE          48.0
xpReward              250
```

**These six are defaults, not the numbers the game runs on.** They live in
`config/priestess/boss.toml` under `[boss.mb_jesselton_williams]`, along with the
two beam damages below, and `EntityStats` writes the configured values over the top of these as
he joins the world. Changing a number in `attributes()` alone changes nothing — see
`docs/STATS.md`. `xpReward` is not configured and is set in the constructor as normal.

His own armour is low on purpose. Phase one is meant to be survivable *in gear* and phase
two is meant to *ignore* gear — the difficulty lives in the damage types, not in how spongy
he is.

The goals are entirely stock: `FloatGoal`, `MeleeAttackGoal`, `WaterAvoidingRandomStrollGoal`,
`LookAtPlayerGoal`, plus `HurtByTargetGoal` and `NearestAttackableTargetGoal` on the target
selector. Everything that makes him a boss is in `customServerAiStep`, not in a custom goal.

### The two phases

```
       220 HP ─────────────────────── 110 HP ─────────────────────── 0
       │  PHASE ONE                   │  PHASE TWO
       │  spectral_arts, 9 damage     │  void_arts, 7 damage
       │  armour works                │  armour does nothing
       │  bar PURPLE                  │  bar RED
       │                              │  summons every 8s
```

```java
public boolean isPhaseTwo() {
    return this.getHealth() <= this.getMaxHealth() * PHASE_TWO_AT;   // 0.5
}
```

Phase two does **less** raw damage — 7 instead of 9 — and is much worse for you, because 7
armour-piercing beats 9 through riot gear. The attack looks identical. That is the point:
the player does not see a new attack, they see the same attack stop being something their
armour answers.

`announcedPhaseTwo` is a separate one-shot flag. It is not "am I in phase two" — that is
`isPhaseTwo()` — it is "have I already played the transition", so the bar recolour, the
Wither spawn sound and the free first wave happen once rather than every tick below half
health.

### The tick

`customServerAiStep()` runs every server tick and is the whole boss:

```
1. bossEvent.setProgress(health / maxHealth)
2. if no restriction yet → restrictTo(here, 40)      ← one-time anchor, see below
3. if isPhaseTwo() && !announcedPhaseTwo → enterPhaseTwo()
4. if no target (or dead) → return
5. decrement rangedCooldown, summonCooldown
6. castArts(target)
7. if isPhaseTwo() && summonCooldown <= 0 → summon a wave, reset cooldown
```

`castArts` self-guards rather than being gated by the caller — it returns immediately unless
the cooldown is up, the target is within 24 blocks, and there is line of sight. The cooldown
is 45 ticks, so a beam every 2.25 seconds at most.

Note: **the cooldown only ticks down while he has a target.** Lose aggro and it freezes
where it was, so breaking line of sight and running does not quietly bank a beam for when
you come back.

### Summoning — removed

Phase two used to also summon Imprisoned Shadows: 3 per wave every 160 ticks, capped at 8
alive, spawned on a ring 2–5 blocks out so they were never free damage inside his hitbox.

**That mob was cut, and the summon machinery went with it.** `enterPhaseTwo` is now a bar
colour, a sound and nothing else, and phase two is carried entirely by the damage-type
change described above. It still reads as a turn — the armour stops working — but it is
thinner than it was designed to be, and the swarm is the obvious slot for whatever replaces
it.

### Staying put

```java
if (!this.hasRestriction()) {
    this.restrictTo(this.blockPosition(), HOME_RADIUS);   // 40 blocks
}
```

This is in the tick rather than in a spawn hook because **structures place entities directly
rather than through `finalizeSpawn`**, so there is no spawn callback to hang it on. First
server tick, wherever the prison put him, becomes home. Without it he can be pulled out of
the dungeon and lost.

### The drop

```java
protected void dropCustomDeathLoot(DamageSource source, int looting, boolean recentlyHit) {
    super.dropCustomDeathLoot(source, looting, recentlyHit);
    this.spawnAtLocation(new ItemStack(ModItems.MANSFIELD_MASTER_KEY.get()));
}
```

Hardcoded, not a loot table. The Master Key gates the rest of the chapter, so it has to be
exactly one, every time — regardless of Looting, difficulty, or whether the kill rolled
anything else. "Awaken" drops **Dreamland** the same way and for the same reason, and so do
the three Medium-bearers — there are no entity loot tables in the mod at all.

---

## Known rough edges

Things that are true of the code as it stands, found while writing this up. None are
breaking the fight; all are worth knowing before touching it.

**Phase two is not actually latched.** The class javadoc says he enters phase two "and never
goes back", but `isPhaseTwo()` reads current health every call. Heal him above 110 — a
Regeneration splash, `/effect`, a healing mod — and his damage type reverts to
`spectral_arts` and summons stop, while the bar stays red because `announcedPhaseTwo` *is*
latched. Nothing in the chapter heals him today, so this is latent rather than live. Latching
the phase itself, or dropping the "never goes back" claim, would settle it.

**`announcedPhaseTwo` does not survive a reload.** Jesselton overrides no save data, so the
flag resets to `false` when the chunk unloads. Quit and rejoin mid-phase-two and the first
tick sees `isPhaseTwo() && !announcedPhaseTwo`, so `enterPhaseTwo()` fires again: another
Wither spawn sound and a **free extra wave of 3 adds**, up to the cap. The Failed Vision does
not have this problem because it persists its node count. A two-line
`addAdditionalSaveData`/`readAdditionalSaveData` pair would fix it.

**The beam's return value is discarded.** `ArtsBeam.fire` documents its boolean as "whether
the target actually took damage — which callers use to decide whether to spend the attack
cooldown", but `castArts` sets `rangedCooldown = RANGED_COOLDOWN_TICKS` *before* firing and
ignores the result. A beam absorbed by the target's hurt-immunity still costs the full 2.25
seconds. Either honour the contract or drop it from the javadoc.

**The summon cap counts a 24-block box, not the arena.** `SUMMON_SEARCH_RADIUS` is inflated
from his bounding box, so adds that chase a player out past 24 blocks stop counting toward
the cap of 8 and he summons more. Kiting far enough therefore grows the total population
rather than holding it. The 60-second fade is what stops this from running away.
