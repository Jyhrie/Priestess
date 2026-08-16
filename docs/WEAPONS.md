# Weapons

Everything a weapon requires lives in `weapons/`. Build one by **copying
`weapons/item/TemplateWeaponItem.java`** and working through the nine steps below; none of them
touches `Priestess.java`.

Steps 1–9 produce a registered weapon with a name, tooltip, model and working left click.
Abilities follow in [part two](#abilities).

**Reference implementations:**

| Weapon | Demonstrates |
|---|---|
| **Aegir Greatspear** | the pattern to follow. Main-hand only; a projectile that stops at the first entity, an ability that outlives the click |
| **Laevatain** | charging, two charged abilities on one button, independent per-ability cooldowns |
| **Devil's Devastation** | a projectile fan. Ported — see [LETHALITY WEAPONS.md](LETHALITY%20WEAPONS.md) |

> Devil's Devastation and Laevatain scan `InteractionHand.values()`, while `WeaponSwingEvents`
> gates on the main hand alone, so holding both and swinging fires both. **New weapons are
> main-hand only.** See `ERRORS.md` § A1.

---

## Contents

**Getting it in game**

1. [Copy the template](#1-copy-the-template)
2. [Rename and assign the class variables](#2-rename-and-assign-the-class-variables)
3. [Declare its stats](#3-declare-its-stats)
4. [Register the item](#4-register-the-item)
5. [Supply two textures](#5-supply-two-textures)
6. [Call its model builder](#6-call-its-model-builder)
7. [Add the name and tooltip](#7-add-the-name-and-tooltip)
8. [Wire the left-click swing](#8-wire-the-left-click-swing)
9. [Generate and run](#9-generate-and-run)

**Abilities**

- [Which hook fires when](#which-hook-fires-when)
- [Left click: on hit](#left-click-on-hit)
- [Right click](#right-click)
- [Shift + right click](#shift--right-click)
- [Hold right click (charged)](#hold-right-click-charged)
- [Cooldowns and durability](#cooldowns-and-durability)
- [Hitting an area](#hitting-an-area)
- [Projectiles](#projectiles)
- [Animated VFX meshes](#animated-vfx-meshes)
- [When it does not work](#when-it-does-not-work)

---

# Getting it in game

## 1. Copy the template

```
weapons/item/TemplateWeaponItem.java  →  weapons/item/YourWeaponItem.java
```

That file carries the constructor, rarity, tooltip, on-hit hook, swing entry point and — as a
nested `Model` class — **its own item model**.

The template is itself registered and holdable (`/give @s priestess:template_weapon`), so the
starting point can be inspected before anything is changed and the scaffolding cannot silently
rot.

**Do not extend `SwordItem` directly.** `ConfiguredSwordItem` is what makes a weapon's numbers
answer to `config/priestess/weapon.toml`; `SwordItem` freezes them at registration. See
[STATS.md](STATS.md).

## 2. Rename and assign the class variables

**Rename**, so nothing in the copy still refers to the template:

| Identifier | Becomes |
|---|---|
| the class and its constructor | `YourWeaponItem` |
| `Model.NAME` | `"your_weapon"` — **must match** the registry name in step 4 |
| the `tooltip.priestess.template.*` keys | `tooltip.priestess.your_weapon.*` |
| `sweep(...)` | the name of the left-click ability |

**Assign the class variables.** They are declared at the top of each class so that configuring a
weapon means editing a block of constants rather than hunting through method bodies:

| Variable | In | Decides |
|---|---|---|
| `TIER` | the item | durability, enchantability, repair material. `WeaponTiers.DEMONIC` unless a new tier is warranted |
| `STATS` | the item | the config block supplying damage and swing speed — `WeaponStats.YOUR_WEAPON`, added in step 3 |
| `PROPERTIES` | the item | vanilla `Item.Properties`: stack size, fire resistance |
| `RARITY` | the item | the name's colour. `WeaponRarities.CALAMITOUS` for a rarity above vanilla's four |
| `EXAMPLE_STAT` | the item | placeholder for ability constants — ranges, arcs, durations. Rename or delete |
| `NAME` | `Model` | the registry name the model is generated under |
| `THIRD_*`, `FIRST_*` | `Model` | the held pose in third and first person (step 6) |

Leave `WeaponTiers.DEMONIC`'s `attackDamageBonus` at zero and put all damage in the config, so one
number governs it. Cooldowns, ranges and ability geometry are deliberately **not** configurable
and belong here as constants.

## 3. Declare its stats

In `config/WeaponStats`, alongside the template's block:

```java
public static final Stats.Weapon YOUR_WEAPON;
...
builder.comment("Your Weapon — one line describing it.").push("your_weapon");
YOUR_WEAPON = weapon(builder, 15.0, -2.4);   // attackDamage, attackSpeed
builder.pop();
```

- `attackDamage` is added to the player's base of 1, so the tooltip shows this plus one.
- `attackSpeed` is an offset from 4.0, so `-2.4` leaves 1.6 swings a second.

Ability damage is declared here too, as a **fraction** of `attackDamage`:

```java
YOUR_ABILITY_FRACTION = builder
        .comment("Right click, the primary ability.")
        .defineInRange("abilityDamageFraction", 1.2, 0.0, Stats.FRACTION_LIMIT);
```

Read it against the stack's *real* damage, so enchantments carry through:

```java
float damage = WeaponText.itemAttackDamage(stack)
        * WeaponStats.YOUR_ABILITY_FRACTION.get().floatValue();
```

`attackDamage` moves the weapon and everything it throws together; the fractions determine only
the shape of the kit. A flat value is right only where there is no wielded item to scale against —
Aegir's Maelstrom, whose vortex outlives the swing that opened it.

## 4. Register the item

```java
// weapons/ModWeapons.java
public static final RegistryObject<Item> YOUR_WEAPON =
        ITEMS.register("your_weapon", YourWeaponItem::new);
```

`ModCreativeTabs` already iterates `ModWeapons.ITEMS`, so the tab needs no wiring.

## 5. Supply two textures

In `src/main/resources/assets/priestess/textures/item/`:

| File | Size | Seen |
|---|---|---|
| `your_weapon.png` | 64×64 | in hand |
| `your_weapon_gui.png` | 16×16 | inventory, hotbar, ground |

Both are required and **datagen fails outright if either is absent.** Two sprites because reducing
the 64×64 to slot size keeps every detail and renders none of them legibly.

For a placeholder, add an entry to `BIG_WEAPONS` in `tools/generate_placeholder_art.py` rather
than drawing a PNG by hand — and remove it once real art lands, or a later run overwrites the real
thing.

## 6. Call its model builder

```java
// datagen/ModItemModelProvider.registerModels()
YourWeaponItem.Model.build(this);
```

That is the entire datagen side; the model itself is already in the copy made in step 1, and its
numbers tune without affecting any other weapon.

**The call is not optional, and declaring the builder in the item class does not replace it.**
`getBuilder(name)` registers into *that provider instance's* `generatedModels` map, and Forge
emits only what that map holds once `registerModels()` returns — so a builder nothing calls is
never discovered. The failure is **silent**: a missing-model cube and no error.

> `bigWeapon(...)` and `chargedWeapon(...)` are **legacy**, retained for the three weapons that
> predate this. They must not be called for anything new: they hold every weapon at one angle and
> one scale, which suited three greatswords and is wrong for a spear or a dagger.

For an item that reads at slot size, delete the nested `Model` class and call
`basicItem(ModWeapons.YOUR_WEAPON.get())` instead.

## 7. Add the name and tooltip

```java
// datagen/ModLanguageProvider
add(ModWeapons.YOUR_WEAPON.get(), "Your Weapon");
add("tooltip.priestess.your_weapon.flavour", "§8« One line of flavour. »");
add("tooltip.priestess.your_weapon.left", "§6Ability Name §7— Left Click");
add("tooltip.priestess.your_weapon.left_detail", "§e -What it does.");
```

Every key `appendHoverText` requests must exist or the tooltip renders the raw key.
`tooltip.priestess.hold_shift` already exists and should be reused.

Convention: `§6` heading, `§e` detail, `§8` flavour, cooldown as `§8(3s)` ending a heading. For an
animated gradient name, override `getName` and use `WeaponText.gradient(...)`, as
`DevilsDevastationItem` does. See [TOOLTIPS.md](TOOLTIPS.md) and [RARITIES.md](RARITIES.md).

## 8. Wire the left-click swing

**Only for a left click that fires when the swing hits air.** One that matters only when it
connects is `hurtEnemy` and needs no plumbing. Vanilla sends the server nothing on a missed swing,
which is why this behaviour alone needs a packet.

**a.** `weapons/client/WeaponSwingEvents.trySendSwing` — add the item to the check:

```java
if (held.getItem() == ModWeapons.DEVILS_DEVASTATION.get()
        || ...
        || held.getItem() == ModWeapons.YOUR_WEAPON.get()) {
```

**b.** `weapons/network/SwingSlashC2S.dispatch` — one line:

```java
YourWeaponItem.sweep(player.level(), player);
```

**c.** In the item, that method verifies it is the item held and returns otherwise, which is why
`dispatch` needs no lookup table:

```java
public static void sweep(Level level, Player user) {
    ItemStack stack = user.getMainHandItem();
    if (stack.getItem() != ModWeapons.YOUR_WEAPON.get()) return;
    if (user.getCooldowns().isOnCooldown(stack.getItem())) return;
    // Already on the server. Spawn entities and deal damage here.
}
```

The packet **carries no payload**: the client is not trusted to declare which weapon swung or how
hard, so the server re-reads the held stack.

## 9. Generate and run

```
gradlew runData && gradlew runClient
```

Then `/give @s priestess:your_weapon`. Nothing under `src/generated/resources/` may be hand-edited
— `runData` overwrites it, and prunes any file no provider claims.

---

# Abilities

## Which hook fires when

**Most weapon defects are the wrong row.**

| Player action | Hook | Runs on | Custom packet? |
|---|---|---|---|
| Left click, **connects** | `Item.hurtEnemy` | server | no |
| Left click, **hits nothing** | `PlayerInteractEvent.LeftClickEmpty` | **client only** | **yes** — step 8 |
| Left click a **block** | `PlayerInteractEvent.LeftClickBlock` | both | no |
| Right click **air** | `Item.use` | **both** | no |
| Right click a **block** | `Item.useOn` (falls through to `use` on `PASS`) | both | no |
| Right click an **entity** | `Item.interactLivingEntity` | both | no |
| **Shift** + right click | `Item.use`, gated on `isShiftKeyDown()` | both | no |
| **Hold** right click | `use` → `getUseDuration` → `releaseUsing` | both | no |

Two rows warrant emphasis:

- **`Item.use` runs on both sides.** The client calls it *and* sends a packet causing the server to
  call it, so every unguarded side effect occurs twice.
- **Swinging at air is the sole exception** — no vanilla packet exists, hence step 8.

## Left click: on hit

Server-side, so effects apply directly. **`super.hurtEnemy` must always be called** — it is what
spends durability.

```java
@Override
public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
    target.setSecondsOnFire(8);
    // Clearing hurt-immunity lets a follow-up land instead of being eaten by i-frames.
    target.invulnerableTime = 0;
    return super.hurtEnemy(stack, target, attacker);
}
```

## Right click

```java
@Override
public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
    ItemStack stack = player.getItemInHand(hand);

    if (player.getCooldowns().isOnCooldown(stack.getItem())) {
        return InteractionResultHolder.fail(stack);
    }
    // Both sides: the server enforces it, the client needs it to draw the sweep.
    player.getCooldowns().addCooldown(stack.getItem(), 60);

    if (!level.isClientSide()) {
        spawnSomething(level, player);                                  // server: entities,
        stack.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(hand)); // damage, randomness
    } else {
        level.playLocalSound(player.getX(), player.getY(), player.getZ(),
                SoundEvents.EVOKER_CAST_SPELL, SoundSource.PLAYERS, 1.0F, 1.0F, false);
    }

    return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
}
```

1. **Guard every side effect.** An entity spawned outside `!level.isClientSide()` is a client-only
   ghost that disappears at the next sync.
2. **Never roll randomness on both sides.** The player is shown one result and given another. Roll
   server-side and transmit the outcome if the client needs it.
3. **Return the correct result.** `sidedSuccess` swings the arm, `fail` does nothing, `pass` falls
   through.

**A click landing on a block reaches `useOn`, not `use`.** Return `InteractionResult.PASS` from
`useOn` to let it fall through.

## Shift + right click

No separate hook: `use` with a branch, and **shift must be tested first** or the normal branch
consumes it.

```java
if (player.isShiftKeyDown()) {
    return alternateAbility(level, player, stack);
}
return primaryAbility(level, player, stack);
```

- **`isShiftKeyDown()` is the sneak *key*, not the sneak *state*.** `isCrouching()` concerns the
  hitbox and is false when a player sneaks beneath a slab.
- **Shift-right-click on a block never reaches `use`** — holding shift is what suppresses block
  interaction, so it routes to `useOn`. To work while aimed at a block, branch in both and
  delegate to one method.

For a mode switch, store the mode in stack NBT so it survives relogs.

## Hold right click (charged)

`use` begins the draw, `releaseUsing` fires it. `remaining` counts **down**.

```java
@Override public UseAnim getUseAnimation(ItemStack stack) { return UseAnim.BOW; }
@Override public int getUseDuration(ItemStack stack) { return 72000; }   // "until released"

@Override
public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
    player.startUsingItem(hand);
    return InteractionResultHolder.consume(player.getItemInHand(hand));
}

@Override
public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int remaining) {
    if (!(entity instanceof Player player) || level.isClientSide()) return;
    float charge = Math.min((getUseDuration(stack) - remaining) / (float) CHARGE_TICKS, 1.0F);
    if (charge < 0.25F) return;      // released too early
    // ... fire at strength `charge`
}
```

**`UseAnim.BOW` does not itself produce a visible wind-up** — a bow appears to bend because its
*model swaps*. Uncomment the charged block in `Model` and register the `pulling` and `pull`
predicates in `WeaponsClient.clientSetup`; each is inert without the other.

### Two charged abilities on one button

`releaseUsing` does not know which ability began and **must not try to determine it** — the player
may release shift mid-draw, and a Twilight charge would fire Molten Giant. Record the choice as
the draw starts:

```java
// use() — decide once, and record it
stack.getOrCreateTag().putInt(TAG_CHARGING, player.isShiftKeyDown() ? TWILIGHT : MOLTEN_GIANT);
player.startUsingItem(hand);

// releaseUsing() — read it, and clear it regardless of what follows
int which = stack.getOrCreateTag().getInt(TAG_CHARGING);
stack.getOrCreateTag().putInt(TAG_CHARGING, NONE);
```

Clear it **unconditionally**, including on a draw too short to fire, or a stale value waits for the
next release to act upon.

## Cooldowns and durability

```java
player.getCooldowns().addCooldown(stack.getItem(), 60);   // ticks — set on BOTH sides
player.getCooldowns().isOnCooldown(stack.getItem());
```

To match the weapon's swing rate rather than a fixed value, divide 20 by
`Attributes.ATTACK_SPEED`.

### More than one cooldown on one weapon

**`ItemCooldowns` is keyed by `Item`, so it holds one timer per weapon** — a ten-second ability
would lock out a one-second one. Additional timers belong in stack NBT as game-time stamps:

```java
private static boolean ready(ItemStack stack, Level level, String tag) {
    return level.getGameTime() >= stack.getOrCreateTag().getLong(tag);
}

private static void startCooldown(ItemStack stack, Level level, String tag, int ticks) {
    stack.getOrCreateTag().putLong(tag, level.getGameTime() + ticks);
}
```

Store the *ready-at* time, not a countdown: nothing has to tick it down and it survives the item
sitting in a chest. Stack tags sync on their own and `getGameTime` agrees on both sides, so no
packet is needed. The vanilla cooldown is then left to whichever ability the hotbar sweep honestly
represents — normally the basic attack, as in Laevatain.

**Durability**: `super.hurtEnemy` spends it on melee. An ability spends it explicitly, server-side
only, with `stack.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(hand))`.

## Hitting an area

**`AABB` is always axis-aligned**, so it is never the intended shape alone: a box around a 5-block
reach swells to its diagonal when the player faces north-east. Gather with an `AABB`, then discard
the corners with an exact test.

**A cone** — compare against the cosine of the half-angle rather than an `acos` per candidate,
since the dot product of two unit vectors already *is* that cosine:

```java
Vec3 toTarget = candidate.getBoundingBox().getCenter().subtract(user.getEyePosition());
double distance = toTarget.length();
if (distance > range) continue;
if (toTarget.scale(1.0 / distance).dot(user.getLookAngle())
        < Math.cos(Math.toRadians(arcDegrees * 0.5))) continue;
```

**An oriented box** — rewrite the offset into the player's own frame and test each axis:

```java
Vec3 forward = user.getLookAngle();
Vec3 right = forward.cross(new Vec3(0, 1, 0));
right = right.lengthSqr() < 1.0E-6 ? new Vec3(1, 0, 0) : right.normalize();  // straight up/down
Vec3 up = right.cross(forward).normalize();

Vec3 offset = candidate.getBoundingBox().getCenter().subtract(origin);
boolean inside = offset.dot(forward) >= 0 && offset.dot(forward) <= length
        && Math.abs(offset.dot(right)) <= halfWidth
        && Math.abs(offset.dot(up)) <= halfHeight;
```

Both are in `LaevatainItem`, as `targetsInCone` and `fireLine`. Two further points:

- **Stop at terrain**, or the area reaches through walls. One `level.clip` down the centre line,
  shortened to the hit, is generally sufficient.
- **Draw the shape in particles.** An AOE the player cannot see is one they cannot aim, and the
  outer edge is the part worth drawing.

## Projectiles

**1. The entity** — `weapons/entity/YourProjectile.java`. One that flies straight, sweeps and
decays should **extend `DevilsProjectile`** and costs about 30 lines. From scratch:

```java
public class YourProjectile extends AbstractHurtingProjectile implements GeoEntity {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private float damage;

    public YourProjectile(EntityType<? extends YourProjectile> type, Level level) {
        super(type, level);
    }

    public YourProjectile(Level level, double x, double y, double z, float damage) {
        this(ModWeapons.YOUR_PROJECTILE.get(), level);
        this.setPos(x, y, z);
        this.damage = damage;
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (!level().isClientSide() && result.getEntity() instanceof LivingEntity target) {
            target.hurt(level().damageSources().indirectMagic(this, getOwner()), damage);
            discard();
        }
    }

    @Override protected void defineSynchedData() { }
    @Override public boolean isPickable() { return false; }
    @Override public boolean isOnFire() { return false; }   // or it renders burning

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);   // required, or it is invisible
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 0, s -> PlayState.CONTINUE));
    }

    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
}
```

- **`getAddEntityPacket` is not optional.** Without it the entity is invisible to every client.
- **Overriding `tick()` without `super.tick()` means owning movement entirely** — nothing writes
  yaw or pitch, so the renderer must derive facing from velocity, as `DevilsProjectileRenderer`
  does.
- **Fields are not synced.** `damage` exists only on the server; anything the client needs takes
  `defineSynchedData` and an `EntityDataAccessor`.

**2. The entity type** — in `ModWeapons`. `MobCategory.MISC` because it is not a mob and must never
count against a spawn cap; `updateInterval(1)` because fast movers stutter at the default of 3.

```java
public static final RegistryObject<EntityType<YourProjectile>> YOUR_PROJECTILE =
        ENTITY_TYPES.register("your_projectile", () -> EntityType.Builder
                .<YourProjectile>of(YourProjectile::new, MobCategory.MISC)
                .sized(0.5F, 0.5F)
                .clientTrackingRange(8)
                .updateInterval(1)
                .build("your_projectile"));
```

**3. Spawning**, from server-side code:

```java
YourProjectile shot = new YourProjectile(level, user.getX(), user.getY() + 0.25, user.getZ(), damage);
shot.setDeltaMovement(user.getLookAngle());
shot.shootFromRotation(user, user.getXRot(), user.getYRot() + yawOffset, 0.0F, speed, inaccuracy);
shot.setOwner(user);
level.addFreshEntity(shot);
```

`setOwner` is how the projectile avoids the shooter and how kills are credited. The apparently
redundant `setDeltaMovement` prevents a first tick at zero velocity, which renders the projectile
briefly parked at the player's feet. For a **fan**, vary `yawOffset` per shot; see
`DevilsDevastationItem.spawnFan`.

**4. Model and renderer.** A `GeoModel` returning `geo/entity/your_projectile.geo.json` and
`textures/entity/your_projectile.png`, `getAnimationResource` returning `null` for static geometry,
then one line in `WeaponsClient.registerRenderers`:

```java
event.registerEntityRenderer(ModWeapons.YOUR_PROJECTILE.get(), context ->
        new DevilsProjectileRenderer<>(context, new YourProjectileModel()));
```

`DevilsProjectileRenderer` supplies velocity-derived facing, translucency and full-bright
rendering; a plain `GeoEntityRenderer` suffices for one that moves normally. A projectile whose
trail *is* the visual takes `InvisibleEntityRenderer` and needs no assets — see `AegirTide`. An
unbound entity type logs an error and falls back to a missing-model cube.

## Animated VFX meshes

Particles suit a line or an outline. An ability that calls for a *shape* calls for geometry: a
short-lived entity that plays one GeckoLib clip and removes itself. `WeaponVfx` is that entity.
**A new effect is one registration line and three assets, with no Java at all**, because
`WeaponVfxModel` derives every path from the registry name.

```java
public static final RegistryObject<EntityType<WeaponVfx>> MY_EFFECT =
        ENTITY_TYPES.register("my_effect", () -> vfx("my_effect", 1.0F, 2.0F));
```

```
assets/priestess/geo/entity/my_effect.geo.json         geometry
assets/priestess/textures/entity/my_effect.png         texture
assets/priestess/animations/my_effect.animation.json   keyframes
```

Spawn it from the server side of the ability, **after the damage has resolved**:

```java
WeaponVfx.spawn(level, ModWeapons.MY_EFFECT.get(), position, yaw, pitch, lifetimeTicks);
```

**Keep the visual and the hit separate.** Resolving damage first and spawning the mesh purely to be
looked at is what lets a visual be retimed without touching balance, and makes a dropped packet
cost a flourish rather than a hit.

### Five common failures

1. **Bone names are the contract.** The bone in the `.geo.json` and the bone keyframed in the
   `.animation.json` must match. GeckoLib logs *nothing* otherwise; the mesh does not move.
2. **The clip must be named `play`.** One `RawAnimation` drives every VFX and requests that name.
3. **Lifetime must equal `animation_length` × 20.** The JSON is in seconds, the entity counts
   ticks. Too short and the mesh vanishes mid-swing; too long and it hangs on its final frame.
4. **Transition length 0.** The default eases in over several ticks, which on an eight-tick effect
   is most of the animation spent blending.
5. **The texture sheet must match the model.** `tools/generate_placeholder_models.py` prints the
   sheet size it packed the UVs onto; the PNG must be exactly that.

`WeaponVfx` calls `super.tick()` because `baseTick` advances `tickCount`, which drives GeckoLib's
animation clock. Omitting it renders the mesh frozen on frame zero.

---

## When it does not work

| Symptom | Cause |
|---|---|
| Item absent from the creative tab | not registered in `ModWeapons` |
| Missing-model cube in hand | no `Model.build(this)` call, or `Model.NAME` does not match the registry name |
| `runData` fails on a missing file | a sprite is absent or misnamed |
| Tooltip shows raw `tooltip.priestess.…` keys | key not added to `ModLanguageProvider` |
| Ability fires twice, or particles double | side effect not guarded by `!level.isClientSide()` in `use` |
| Nothing happens on right click | returned `fail`/`pass` rather than `sidedSuccess`, or a block consumed the click and `useOn` is required |
| Shift-right-click never triggers | aimed at a block, which routes to `useOn` |
| Arm does not swing | returned `consume` or `fail`; use `sidedSuccess` |
| Swing at air does nothing, hitting a mob works | missing the `WeaponSwingEvents` clause or the `SwingSlashC2S.dispatch` line |
| No visible wind-up while charging | model overrides or the `pulling`/`pull` predicates missing; both are required |
| Both weapons fire when one is swung | the legacy `InteractionHand.values()` loop; use the main hand only |
| Projectile invisible | `getAddEntityPacket` missing, or a wrong geo/texture path |
| Projectile appears then vanishes | spawned client-side |
| Projectile flies sideways facing forward | `tick()` overridden without `super.tick()`, so nothing sets yaw or pitch |
| Projectile stutters | `updateInterval` left at the default; set `1` |
| Hotbar icon is black | `gui_light` not set on the outer `separate_transforms` model |
| Inventory icon over-detailed | no `_gui` sprite, so the 64×64 one is being reduced |
| Cooldown sweep does not draw | cooldown set server-side only |
| Client and server disagree | randomness rolled on both sides |
| No durability loss on hit | `hurtEnemy` overridden without calling `super.hurtEnemy` |
| A config edit has no effect | extended `SwordItem` rather than `ConfiguredSwordItem` |

Test right-click abilities **on a LAN world or dedicated server** where possible. A single-player
world runs an integrated server, which conceals much of the side-confusion above.
