# Bosses

How a Columbia boss is put together, walked through with **Jesselton's Shadow** — the first
one, and the one every other boss in the chapter is a variation on.

For the chapter as a whole see the main [README](../README.md#the-columbia-chapter).

---

## Contents

- [The shared skeleton](#the-shared-skeleton) — what every boss inherits
- [Arts beams](#arts-beams) — the ranged attack all of them use
- [Jesselton's Shadow](#jesseltons-shadow)
  - [Numbers](#numbers)
  - [The two phases](#the-two-phases)
  - [The tick](#the-tick)
  - [Summoning](#summoning)
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

## Jesselton's Shadow

`src/main/java/com/jyhrie/priestess/entity/JesseltonsShadow.java`

The mercenary who tried to take Mansfield and got locked in it instead. He lives in the
prison and gates the rest of the chapter behind the Mansfield Master Key.

### Numbers

```java
MAX_HEALTH            220.0     ARMOR                  6.0
MOVEMENT_SPEED        0.28      KNOCKBACK_RESISTANCE   1.0
ATTACK_DAMAGE         8.0       FOLLOW_RANGE          48.0
xpReward              250
```

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

Note step 4: **cooldowns only tick down while he has a target.** Lose aggro and everything
freezes where it was, so breaking line of sight and running does not quietly bank a summon
wave for when you come back.

### Summoning

Phase two only, every 160 ticks (8 s), 3 per wave, hard cap of 8 alive:

```java
long live = serverLevel.getEntitiesOfClass(ImprisonedShadow.class,
        this.getBoundingBox().inflate(SUMMON_SEARCH_RADIUS)).size();   // 24 blocks
if (live >= MAX_LIVE_SHADOWS) return;                                  // 8
```

The adds spawn on a **ring** — a random angle, 2–5 blocks out — rather than on top of him.
Adds that spawn inside the boss's hitbox are free damage for anyone already swinging at it.

They go out through `finalizeSpawn` with `MobSpawnType.MOB_SUMMONED` and inherit his current
target, then get 12 soul particles so the wave reads as an event.

`ImprisonedShadow` is built to be *annoying rather than dangerous* — 14 HP, speed 0.34,
4 damage, knockback-immune. The thing that keeps a long fight from silting up is that each
one **fades after 1200 ticks (60 s) whether or not you killed it**, tracked by its own
`shadowAge` and saved to NBT. They are also not gated on the boss being alive: kill Jesselton
mid-summon and the ones already out stay, because an arena going instantly silent robs the
kill of its ending.

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
anything else. The Failed Vision drops Dorothy's Neural Processor the same way and for the
same reason.

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
