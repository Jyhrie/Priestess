# Stats — the balance config

Every combat number in the mod lives under `config/priestess/`, in four files:

| File | Holds | Declared in |
|---|---|---|
| `boss.toml` | the three bosses, and their beam damages | `config/BossStats.java` |
| `miniboss.toml` | the miniboss tier, and the enrage speed | `config/MinibossStats.java` |
| `mob.toml` | the twelve trash mobs, and the two ranged attacks | `config/MobStats.java` |
| `weapon.toml` | the three weapons, and every ability fraction | `config/WeaponStats.java` |

The three creature files share a shape: health, movement speed, melee damage, follow range,
armour and knockback resistance, plus extra keys for anything with a ranged or special attack.
`weapon.toml` is the odd one out and takes damage and swing speed instead.

**Four rather than one** because they are read at different times by different questions. Boss
health is tuned while designing a fight; mob health while pacing a dungeon; weapon damage against
both. One six-hundred-line file made every one of those edits a scroll past the other two.
`config/Stats.java` holds what they share — the `Block` and `Weapon` types, the bounds, and the
act of writing values onto a live entity — so the four files stay lists of numbers.

**Installation-wide, not per world** — edit once and every save, existing and future, uses it. In
a dev run that is `run/config/priestess/`; in a normal install, `.minecraft/config/priestess/`;
on a dedicated server, `config/priestess/` in the server directory. Forge creates the
subdirectory itself: `ConfigFileTypeHandler.setupConfigFile` calls
`Files.createDirectories(file.getParent())` before creating the file.

Applied by `entity/EntityStats.java` for creatures, and `weapons/item/ConfiguredSwordItem.java`
for weapons.

---

## COMMON, and the one thing it costs

Forge configs come in three types, and the choice is about *where the file lives*:

| Type | Lives in | Scope |
|---|---|---|
| COMMON | `config/` | the installation — all four of these |
| SERVER | `<world>/serverconfig/` | one world; synced to clients — `priestess-server.toml` |
| CLIENT | `config/` | the installation, client only |

Balance is a property of how the pack is tuned, not of a particular save, so it is **COMMON**.
`PriestessConfig` — lockdown and flight — is SERVER for the opposite reason and both are right:
it gates what a world *permits*, so it has to travel with the world.

The cost is that **a COMMON config is not synced**. On a multiplayer server each side reads its
own copy, so a client whose file disagrees with the server's will see:

- a wrong damage number on a weapon tooltip, and
- a wrong attack-cooldown timer, since the client computes the swing indicator from its own item
  modifiers.

Only the *display* is wrong. Mob stats, ability damage and every hit are resolved server-side off
the server's files — a client cannot buff itself by editing these. But it is a real way for a
player to be confused, and the answer is that a pack ships them to everybody. (This is the same
trade Cataclysm and most stat-config mods make.)

### Why not part of `priestess-server.toml`

Different question, different type. That file gates what a world *permits* and travels with the
save; these decide how hard things hit and belong to the install. Forge tracks configs by
filename, so a mod may register as many as it likes.

### Edits take effect live

`ForgeConfigSpec` re-reads a file changed on disk while the game is running, and nothing in this
system caches a value past that. So you can edit the toml with the game open:

- **weapons** update immediately — the modifier map is rebuilt per call;
- **abilities** update on the next cast;
- **mobs** update the next time they join a level, so unload the area and come back (`/kill` and
  re-spawn is quicker for a single test mob).

---

## The shape of it

`boss.toml`:

```toml
[boss]
  [boss.mb_jesselton_williams]
    maxHealth = 220.0
    movementSpeed = 0.28
    attackDamage = 8.0
    followRange = 48.0
    armour = 6.0
    knockbackResistance = 1.0
    artsPhaseOneDamage = 9.0
    artsPhaseTwoDamage = 7.0
```

`weapon.toml`:

```toml
[weapon]
  [weapon.laevatain]
    attackDamage = 18.0
    attackSpeed = -1.6
    sweepDamageFraction = 0.75
    moltenGiantDamageFraction = 1.5
    twilightDamageFraction = 1.25
```

Each file's top-level table is its own name, so a key's full path reads
`mob.sv_reaper.maxHealth`. Tables are named by **registry name**, so `[mob.sv_reaper]` is the mob
you spawn with `priestess:sv_reaper`. The six attribute keys are the same in every mob table; a mob with a
ranged or special attack gets extra keys for it, because those carry their own damage and never
read `attackDamage`.

The bounds are vanilla's own limits for each attribute — armour caps at 30, knockback resistance
at 1 — so nothing you set here can be silently clamped later by the attribute itself.

---

## Why nothing reads the config where the numbers are declared

Timing, in both cases, and it is the thing to understand before editing any of this.

An `AttributeSupplier` is built **once during mod loading**, from the `attributes()` method on
each mob class, and handed to the entity type. Item attribute modifiers are built earlier still,
in the `SwordItem` constructor, during registration. Two problems with reading the config there:

1. **It races config loading.** COMMON configs load somewhere in amongst mod loading, at a point
   no mod should be depending on the exact order of.
2. **Even winning that race is wrong.** Both are built once and kept for the process's lifetime,
   so the numbers would be frozen at startup and editing the file would need a full restart.

So the compiled numbers stay where they are, as the honest answer to "what is this without a
config", and the config is applied afterwards by two different mechanisms — both of which read it
fresh, which is what makes live edits work.

### Mobs — `EntityJoinLevelEvent`

`entity/EntityStats.java` writes the six base values onto each entity as it joins the world.
That event is the one point every entity passes through however it got there — spawn egg,
`/summon`, a structure placing a boss directly (which never calls `finalizeSpawn`), or a chunk
loading off disk — and, crucially, it fires **after** NBT has been read.

That last part is what makes the config authoritative rather than advisory. Attribute base values
save with a mob, so without it, editing the config would do nothing for any mob that already
exists in the world — which, with an installation-wide config that a player expects to just work
everywhere, would be the more surprising behaviour by far.

Two consequences:

- **A mob already standing in a loaded chunk keeps its old numbers** until that chunk unloads and
  comes back. Bosses are summoned fresh, so they pick up a change immediately; a dungeon full of
  mobs needs the area unloaded and revisited.
- **Anything applied as a *modifier* survives untouched**, because only base values are written.
  That is deliberate, and it is why `SvTheFirstToTalk` enrages with an `AttributeModifier`
  instead of writing the base speed as it used to — a mob's own runtime state has to outlive
  this.

Health is carried across as a **fraction**: a boss reloaded at a third of its health comes back
at a third of whatever the new maximum is. Not healed to full, which would make unloading a
chunk a way to reset a fight, and not left holding a number the new maximum cannot contain.

### Weapons — `getAttributeModifiers`

`ConfiguredSwordItem` still passes the compiled defaults to `SwordItem`'s constructor, and
overrides the *getter* so the modifier map is built fresh from the config each time it is asked
for. It is rebuilt per call rather than cached: it is two small objects, the callers are
equipment changes and tooltip rendering rather than anything per-tick, and a cache would have to
be invalidated whenever the file changed on disk — getting that wrong is a weapon quietly
displaying the wrong number. Not caching is also what lets an edit take effect without a restart.

---

## Ability damage follows weapon damage

Every weapon ability that is quoted as a **fraction** multiplies
`WeaponText.itemAttackDamage(stack)` — the stack's *real* attack damage, read back off its
attribute modifiers, Sharpness included. Those modifiers now come from the config, so:

- raising `attackDamage` raises the weapon **and everything it throws**, together;
- changing a fraction changes only the shape of the kit.

The exception is `maelstromDamagePerSecond`, which is flat. The whirlpool is a thing left
standing in the world for eight seconds rather than the spear swinging, and it grinds once a
second, so the number is literally what the tooltip quotes.

Mob and boss abilities are all flat, for the same reason: there is no wielded item to scale off.

---

## Adding a mob

1. Write the mob and its `attributes()` as usual — those numbers are still the defaults.
2. Add a `Block` constant in whichever of `BossStats` / `MinibossStats` / `MobStats` matches its
   tier, and one `builder.push(name)` / `block(...)` / `builder.pop()` group in that class's
   static block. Ability damage goes inside the same push, so it
   lands in the same table.
3. Add one line to `EntityStats.blocks()`.

A mob with no entry simply keeps whatever `attributes()` gave it — a working mob, not a broken
one. Forgetting step 3 costs a config section nobody reads and nothing else.

**Editing a number in an `attributes()` method alone will not change the game**, because
`EntityStats` runs after it. Change it in the config class too, or the default and the effective
value drift apart.

## Adding a weapon

Extend **`ConfiguredSwordItem`** rather than `SwordItem`, and pass it a `Stats.Weapon` from
`WeaponStats`:

```java
public class YourWeaponItem extends ConfiguredSwordItem {
    public YourWeaponItem() {
        super(WeaponTiers.DEMONIC, WeaponStats.YOUR_WEAPON, new Properties());
    }
}
```

The `Weapon` constant carries both the compiled defaults — which the constructor needs, since it
runs during registration — and the configured values. Add ability damages as
`DoubleValue` fields inside the same `builder.push` group.

---

## What is deliberately *not* in here

Cooldowns, ranges, arc angles, box dimensions, burn durations, projectile speeds, VFX lifetimes.
Those are **shape** rather than balance: they decide what an ability *is*, several of them are
load-bearing for something else (a VFX lifetime has to match its animation length, the whirlpool's
damage interval has to match the units its damage is quoted in), and moving them into a config
invites a pack to break an ability rather than tune it.

The pattern for promoting one is the same three lines as any other key, if that call is ever made.
