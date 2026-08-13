go# Lethality weapons

Weapons ported in from [Lethality](https://github.com/) into `com.jyhrie.priestess.weapons`,
and the record of what changed on the way across.

**Read the "What changed" table before editing anything in `weapons/`.** Several behaviours in
there are deliberately stubbed rather than missing, and without this file they look like bugs
worth fixing.

---

## Contents

- [What is ported](#what-is-ported)
- [Where everything lives](#where-everything-lives)
- [What changed on the way in](#what-changed-on-the-way-in)
- [Terramity](#terramity)
- [Better Combat](#better-combat)
- [How a swing becomes projectiles](#how-a-swing-becomes-projectiles)
- [Adding another weapon](#adding-another-weapon)
- [Known gaps](#known-gaps)
- [Checking it worked](#checking-it-worked)

---

## What is ported

One weapon: **Devil's Devastation**.

A greatsword that throws a fan of five projectiles on every swing — three scythes and two
pitchforks. 15 base damage at -2.0 swing speed, so roughly 16 a hit once every second. Each
projectile carries half the sword's attack damage; the two pitchforks carry that plus 2 and fly
tighter to the crosshair, so the fan rewards aiming without punishing a wild swing.

The projectiles pierce. Each one damages a given target at most once, but five of them go out
per swing, so a target caught centrally takes several hits from one click. They pass through
terrain, have no timeout and no collision — they fly straight for ten ticks, decay, spray a
burst of particles and vanish, all inside about a second.

`hurtEnemy` also clears the target's `invulnerableTime`. That is what lets the fan land on
something the melee swing just hit rather than being eaten by i-frames, and it is most of why
the weapon bursts as hard as it does.

### Why this one passed the gate

The rule was: port it only if it does not extend out of Terramity.

`DevilsDevastationItem extends SwordItem` — vanilla. It inherits nothing from Terramity, so it
passed. It *referenced* Terramity in three places, all of which are handled below; none of them
were inheritance, and none of them survive in the ported code as a compile dependency. **The
build has no new dependencies** — `build.gradle` is untouched, and GeckoLib, which the
projectiles need, was already there for the boss models.

---

## Where everything lives

`weapons/` is a sealed compartment. Exactly three references reach into it from outside:

| File | Line |
|---|---|
| `Priestess.java` | `ModWeapons.register(modEventBus)` and `WeaponNetwork.register()` |
| `item/ModCreativeTabs.java` | one `forEach` over `ModWeapons.ITEMS` |
| `datagen/ModItemModelProvider.java`, `datagen/ModLanguageProvider.java` | one entry each |

Delete the folder and those references and the mod still builds. That is the point — none of
this is Columbia's, and it should stay easy to rip back out.

```
weapons/
├── ModWeapons.java              registry: the item, the two entity types
├── WeaponTiers.java             DEMONIC — durability, mining level, repair
├── WeaponRarities.java          CALAMITOUS — a rarity above vanilla's four
├── WeaponText.java              the animated gradient, and attack-damage readback
├── item/
│   └── DevilsDevastationItem.java
├── entity/
│   ├── DevilsProjectile.java    the shared body: the sweep, the decay, the burst
│   ├── DevilsScytheEntity.java  entity type + burn duration, nothing else
│   └── DevilsPitchforkEntity.java
├── client/                      all Dist.CLIENT, none server-safe
│   ├── DevilsProjectileRenderer.java   one renderer, both projectiles
│   ├── DevilsScytheModel.java
│   ├── DevilsPitchforkModel.java
│   ├── WeaponsClient.java       binds the renderers
│   └── WeaponSwingEvents.java   sees the swing, sends the packet
└── network/
    ├── WeaponNetwork.java       the package's own SimpleChannel
    └── SwingSlashC2S.java       the wire
```

Two things were merged that Lethality keeps apart. `DevilsScytheEntity` and
`DevilsPitchforkEntity` were 190-line files differing in four numbers, and are now a shared
`DevilsProjectile` plus two ~30-line subclasses; their two renderers are likewise one generic
class. This matches how `entity/` already works in this mod — behaviour in a base class,
per-mob numbers in the subclass.

**Assets** went to the mod's normal locations, keyed off the registry names:

```
assets/priestess/geo/entity/devils_scythe.geo.json
assets/priestess/geo/entity/devils_pitchfork.geo.json
assets/priestess/textures/entity/devils_scythe.png
assets/priestess/textures/entity/devils_pitchfork.png
assets/priestess/textures/item/devils_devastation.png       64×64, used in hand
assets/priestess/textures/item/devils_devastation_gui.png   16×16, used in the inventory
```

### The item model is not a `basicItem`

Devil's Devastation needs two different things from one item, and `item/generated` — what
`basicItem` produces — does neither:

| Perspective | Model | Sprite |
|---|---|---|
| in hand, first and third person | `item/handheld` + tuned transforms | 64×64 |
| GUI, ground | `item/generated` | 16×16 |

`item/generated` holds a sprite flat and upright, like a carrot, so a greatsword on it reads as
an item rather than a weapon; and letting the GUI shrink the 64×64 sprite makes the inventory
icon look like a photograph of a sword, because all the detail is still there and none of it
survives at slot size.

Forge's `forge:separate_transforms` loader is what lets one item answer with a different model
per perspective. It is built in `ModItemModelProvider.bigWeapon`, which is the datagen
equivalent of the hand-written model Lethality ships — same structure, same transform numbers.
**Adding another oversized weapon is one `bigWeapon(...)` call plus the two sprites**, named
`item/<name>.png` and `item/<name>_gui.png`.

The transform numbers are Lethality's, carried over as tuned. There is nothing to derive them
from — a greatsword that sits correctly in the hand is somebody's afternoon in-game, not a
formula.

#### `gui_light` has to be set on the outer model

If the hotbar icon comes out almost black, this is why.

`gui_light` resolves from the **outer** model, and a `separate_transforms` model has no parent
to inherit it from. `minecraft:item/generated` does set `"gui_light": "front"` — but that sits
two levels down inside a perspective and is never consulted. Left unset, the outer model falls
back to the spec default of `side`, which lights a flat sprite as though from its edge.

`bigWeapon` sets it explicitly at the top level, which is what Lethality's hand-written model
does and for the same reason. **Anything else built on `separate_transforms` needs the same
line.**

---

## What changed on the way in

Everything in this table is a deliberate substitution, not an oversight.

| # | Lethality | Here | Why |
|---|---|---|---|
| 1 | `TerramityModMobEffects.NYXIUM_FIRE`, 3 call sites | commented out, vanilla fire kept | Terramity is not a dependency — see [below](#terramity) |
| 2 | Better Combat cooldown bypass | commented out, cooldown always applies | not integrated yet, by request — see [below](#better-combat) |
| 3 | `ModParticles.FORBIDDEN_GLINT` | `ParticleTypes.SOUL_FIRE_FLAME` | a custom particle is a registry, a client factory and a texture sheet, for set dressing |
| 4 | sound `terramity:crescent_moonblade_wave` | same lookup, null-checked, falls back to `PLAYER_ATTACK_SWEEP` | soft dependency. Lethality passes the null straight to `playSound` and crashes without Terramity; this comes back for free if Terramity is ever added |
| 5 | font `lethality:homicide` on the rarity line | default font, same palette | no font asset came across, and a missing font silently falls back anyway |
| 6 | tier tag `ModTags.Blocks.ANCIENT_WEAPON` | `BlockTags.NEEDS_DIAMOND_TOOL` | it is a sword; the tag decides what it mines, which is cobwebs |
| 7 | tier repair = Bladecrest Oathsword | netherite ingot | the Oathsword is a Lethality item that did not come across. **Swap this** when there is a Columbia material that should repair it |
| 8 | rarity id `calamitous` | `priestess_calamitous` | `Rarity.create` is a global extensible enum; an unprefixed id would collide with Lethality itself if both are installed |
| 9 | tooltip key `hold_ctrl`, code checks Shift | key and text both say Shift | Lethality's prompt asked for the wrong key — a typo against its own code |
| 10 | `DistExecutor.unsafeCallWhenOn(Dist.CLIENT, …)` for the gradient clock | dist checked, server falls back to wall-clock | the original returns `null` server-side and NPEs on unboxing. Latent in Lethality because `getName` rarely runs server-side — but "rarely" includes death messages |
| 11 | `detonate` particle burst ungated | client-side only | `addParticle` is a no-op on a server; this just skips 50 wasted iterations. No visible change |

Items 10 and 11 are fixes rather than substitutions. Everything else is a swap you may want to
undo once the corresponding thing exists in this mod.

---

## Terramity

**This is the substantive loss.** Lethality's Devil's Devastation applies Terramity's
**Nyxium Fire** in three places, and it is the weapon's real damage curve:

| Where | Duration | Amplifier |
|---|---|---|
| `DevilsDevastationItem.hurtEnemy` | 200 ticks | stacks +1 per hit, capped at 9 |
| `DevilsScytheEntity` on sweep | 300 ticks | 2 |
| `DevilsPitchforkEntity` on sweep | 150 ticks | 1 |

The melee stack is the interesting one: every hit raises the amplifier, so the weapon ramps on a
single target rather than dealing flat damage. **None of that is in the port.** What remains is
`setSecondsOnFire` — vanilla fire, 10 seconds melee, 20 from a scythe, 10 from a pitchfork —
which reads similarly and does far less.

The calls are left in place as comments at all three sites, so reinstating is uncommenting.

### To reinstate with Terramity

1. Add Terramity to `build.gradle` `dependencies` and a `[[dependencies.priestess]]` block to
   `META-INF/mods.toml`.
2. Uncomment the three blocks (search `TerramityModMobEffects`).
3. Restore the import `net.mcreator.terramity.init.TerramityModMobEffects` in each file.

Item 4 in the table — the sound — needs nothing; it comes back on its own.

### To reinstate with a Columbia effect instead

Probably the better call, since it costs no dependency. Register a burn in
`effect/ModEffects.java` and substitute it at the same three sites. Note that `ModEffects`
currently holds only `InertMobEffect`s — effects that are read by other code rather than doing
anything themselves — so a stacking burn would need real tick behaviour, which is new work
rather than a swap.

---

## Better Combat

Not integrated, by request.

Lethality checks `ModList.get().isLoaded("bettercombat")` and, when it is present, skips its own
cooldown entirely so Better Combat's attack timing owns the rate limit. Here **the cooldown
always applies**: one swing's worth of ticks, derived from the player's `ATTACK_SPEED`
attribute, so the fan fires at the rate the sword swings rather than as fast as the player can
click.

To restore, guard the cooldown block in `DevilsDevastationItem.fireFan` — the comment there
gives the exact line:

```java
if (!ModList.get().isLoaded("bettercombat")) {
    // ... the existing cooldown block
}
```

---

## How a swing becomes projectiles

A swing is known only to the client; only the server may spawn entities. So there is a wire,
and it is not optional — a weapon that spawns its projectiles client-side spawns them nowhere.

```
left click
  → WeaponSwingEvents          (client) two events, see below
  → SwingSlashC2S              (wire) empty packet
  → DevilsDevastationItem.fireFan  (server) re-reads the held stack, spawns five
```

**Two client events, because neither sees every swing.** `LeftClickEmpty` fires when the click
hits nothing; `AttackEntityEvent` when it hits a mob. Minecraft routes a left-click down one
path or the other and never both, so listening to just one means the weapon silently stops
throwing whenever the player happens to connect — or happens to miss. Clicking a *block* is the
deliberate gap: that is a mining swing, and a greatsword should not spray projectiles at a wall.

**The packet carries no payload.** The client is not trusted to say which weapon or how hard —
the server re-reads the held stack and decides. A modified client can at most ask to swing
something it is already holding, at the cadence its own cooldown allows.

---

## Adding another weapon

**→ Full how-to, including right-click abilities and projectiles:
[WEAPONS.md](WEAPONS.md)**. The short version, for a weapon shaped like this one:

1. **The item** in `weapons/item/`, extending `SwordItem` (or whatever fits). Take damage and
   speed as constructor arguments to `super`; leave `WeaponTiers`' `attackDamageBonus` at zero.
2. **Register** it in `ModWeapons.ITEMS`. The creative tab picks it up with no further wiring.
   Register any projectile entity types in `ModWeapons.ENTITY_TYPES` alongside.
3. **If it throws something on swing**, add a clause to `WeaponSwingEvents.trySendSwing` and a
   line to `SwingSlashC2S.dispatch`. Each weapon's own fire method is responsible for checking
   that it is the thing being held and no-opping otherwise, so there is no dispatch table to
   maintain.
4. **Datagen**: in `ModItemModelProvider`, `bigWeapon(...)` if the sprite is oversized and it
   should be held like a weapon — see [the item model](#the-item-model-is-not-a-basicitem) —
   or `basicItem(...)` if a flat 16×16 icon is genuinely all it needs. Then a name and any
   tooltip keys in `ModLanguageProvider`, and `gradlew runData`.

Renderers for new projectiles go in `WeaponsClient`. If the projectile behaves like the two
here, `DevilsProjectileRenderer` is generic and needs only a new `GeoModel`.

---

## Known gaps

- **No recipe.** Lethality ships one; it names Lethality materials, so it did not come across.
  The item is creative-tab and `/give` only, which is how every other item in this mod currently
  works.
- **No loot table.** Nothing drops it.
- **No emissive overlay.** Lethality ships `devils_devastation_emissive.png` (64×64) alongside
  the other two sprites. Nothing in the ported model references it — an emissive layer needs
  either a shader mod or a second render pass, and neither exists here. The GUI sprite *did*
  come across; see [the item model](#the-item-model-is-not-a-basicitem).
- **The sweep is not cheap.** Each projectile runs 30 AABB queries a tick, so a full swing is
  around 150 for about a second. Survivable for something this short-lived, and the reason these
  are given a hard, short life — but it is the first thing to look at if swinging one tanks the
  tick rate.
- **`DevilsProjectile` does not call `super.tick()`.** It inherits `AbstractHurtingProjectile`
  for the spawn packet and owner tracking, not for movement, and steps its own position. This is
  also why the renderer derives facing from velocity: nothing ever writes the entity's yaw and
  pitch.

---

## Checking it worked

```
gradlew compileJava        # the port compiles with no new dependencies
gradlew runData            # regenerates the item model and en_us.json
gradlew runClient
```

In game:

1. `/give @s priestess:devils_devastation` — or find it in the Priestess creative tab, after the
   spawn eggs.
2. The name should shimmer through white → fire → blood → violet → magenta and back, in bold.
3. **The inventory icon should be the crisp 16×16 sprite at full brightness** — not a shrunken
   64×64 one, and not darkened (if it looks black, see `gui_light` above). Hold it:
   it should sit angled in the hand like a greatsword, not flat and upright like a torch. Drop
   it on the ground and it should go back to the small sprite.
4. Hover and hold **Shift**: two `On Swing` lines and two `On Hit` lines.
5. Swing at open air. Five translucent, full-bright projectiles should fan out, tumble to a stop
   within about a second and burst into soul-fire particles.
6. Swing at a mob. It should catch fire, and the fan should land on it as well as the melee hit —
   that is the `invulnerableTime` reset doing its job.
7. Swing again immediately. Nothing should fire until the cooldown is up; that is item 2 in the
   table, and it is expected.

If the projectiles are invisible, the geo or texture paths are wrong — check the six asset paths
listed above. If they fly but do nothing, the server half is not running: check that
`WeaponNetwork.register()` is still called in `Priestess`.
