# Weapons

How to build a weapon with click behaviours — on-hit effects, right-click abilities,
shift-right-click alternates, and projectiles.

The worked example throughout is **Devil's Devastation** in `weapons/`. For what that weapon is
and where it came from, see [LETHALITY WEAPONS.md](LETHALITY%20WEAPONS.md); this file is the
how-to.

> **Scope note.** Devil's Devastation implements the left-click half only — on-hit and
> swing-at-air. The right-click and shift-right-click sections below are the pattern to follow,
> not a description of existing code. Everything in them is vanilla Forge API that needs no new
> plumbing, and the file each snippet belongs in is named.

---

## Contents

- [Which hook fires when](#which-hook-fires-when)
- [Files you need](#files-you-need)
- [Step 1 — the item class](#step-1--the-item-class)
- [Step 2 — register it](#step-2--register-it)
- [Left click: on hit](#left-click-on-hit)
- [Left click: swing at air](#left-click-swing-at-air)
- [Right click](#right-click)
- [Shift + right click](#shift--right-click)
- [Hold right click (charged)](#hold-right-click-charged)
- [Making a projectile](#making-a-projectile)
- [Cooldowns and durability](#cooldowns-and-durability)
- [The name and tooltip](#the-name-and-tooltip)
- [Datagen and assets](#datagen-and-assets)
- [When it does not work](#when-it-does-not-work)

---

## Which hook fires when

**This table is the whole thing.** Almost every weapon bug is picking the wrong row.

| Player does | Hook to override | Runs on | Custom packet? |
|---|---|---|---|
| Left click, **connects with a mob** | `Item.hurtEnemy` | server | no |
| Left click, **hits nothing** | *nothing vanilla* — `PlayerInteractEvent.LeftClickEmpty` | **client only** | **yes** |
| Left click a **block** | `PlayerInteractEvent.LeftClickBlock` | both | no |
| Right click **air** | `Item.use` | **both** | no |
| Right click a **block** | `Item.useOn` (falls through to `use` if it returns `PASS`) | both | no |
| Right click an **entity** | `Item.interactLivingEntity` | both | no |
| **Shift** + right click | `Item.use`, gated on `player.isShiftKeyDown()` | both | no |
| **Hold** right click | `use` → `getUseDuration` → `releaseUsing` | both | no |

Two rows deserve emphasis:

**`Item.use` runs on both sides.** The client calls it and *also* sends a packet that makes the
server call it. So every side effect has to be guarded, or it happens twice — once as a client
prediction and once for real. That is the single most common weapon bug.

**Swinging at air is the odd one out.** Vanilla sends the server nothing at all — there is no
"I swung and missed" packet — so it is the only behaviour here that needs plumbing of its own.
See [that section](#left-click-swing-at-air).

---

## Files you need

For a weapon with click behaviours but **no projectile**:

| File | Why |
|---|---|
| `weapons/item/YourWeaponItem.java` | new — the item and all its behaviour |
| `weapons/ModWeapons.java` | edit — one `ITEMS.register` line |
| `datagen/ModItemModelProvider.java` | edit — one `bigWeapon(...)` or `basicItem(...)` line |
| `datagen/ModLanguageProvider.java` | edit — name + tooltip keys |
| `assets/priestess/textures/item/your_weapon.png` | new — 64×64 if using `bigWeapon` |
| `assets/priestess/textures/item/your_weapon_gui.png` | new — 16×16, only for `bigWeapon` |

Add for **swing-at-air**:

| File | Why |
|---|---|
| `weapons/client/WeaponSwingEvents.java` | edit — one clause |
| `weapons/network/SwingSlashC2S.java` | edit — one dispatch line |

Add for a **projectile**:

| File | Why |
|---|---|
| `weapons/entity/YourProjectile.java` | new — the entity |
| `weapons/ModWeapons.java` | edit — an `ENTITY_TYPES.register` block |
| `weapons/client/YourProjectileModel.java` | new — geo + texture paths |
| `weapons/client/WeaponsClient.java` | edit — one `registerEntityRenderer` line |
| `assets/priestess/geo/entity/your_projectile.geo.json` | new |
| `assets/priestess/textures/entity/your_projectile.png` | new |

Nothing else. **You never touch `Priestess.java`** — `ModWeapons.register` and
`WeaponNetwork.register` are already called, and everything new hangs off those.

---

## Step 1 — the item class

`weapons/item/YourWeaponItem.java`. Extend `SwordItem` for a melee weapon; the tier supplies
durability and repair, and the two numbers supply damage and speed.

```java
public class YourWeaponItem extends SwordItem {

    private static final int ATTACK_DAMAGE = 12;
    private static final float ATTACK_SPEED = -2.4F;   // negative = slower than a fist

    public YourWeaponItem() {
        super(WeaponTiers.DEMONIC, ATTACK_DAMAGE, ATTACK_SPEED, new Properties());
    }
}
```

`ATTACK_SPEED` is an offset from 4.0, so `-2.4` means 1.6 swings a second. Leave
`WeaponTiers.DEMONIC`'s `attackDamageBonus` at zero and put all the damage here, so one number
governs it.

Want a rarity above vanilla's four? Override `getRarity` and return
`WeaponRarities.CALAMITOUS`.

---

## Step 2 — register it

One line in `weapons/ModWeapons.java`:

```java
public static final RegistryObject<Item> YOUR_WEAPON =
        ITEMS.register("your_weapon", YourWeaponItem::new);
```

The creative tab picks it up with no further wiring — `ModCreativeTabs` already iterates
`ModWeapons.ITEMS`.

---

## Left click: on hit

`Item.hurtEnemy` fires when your weapon damages a living entity. It runs **server-side**
(`Player.attack` guards the call), so you can apply effects and spawn things directly.

```java
@Override
public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
    target.setSecondsOnFire(8);
    target.addEffect(new MobEffectInstance(MobEffects.WITHER, 100, 1), attacker);

    // Clearing hurt-immunity lets a follow-up (a projectile, a second hit in the same tick)
    // land instead of being eaten by i-frames. Devil's Devastation relies on this.
    target.invulnerableTime = 0;

    return super.hurtEnemy(stack, target, attacker);
}
```

**Always call `super.hurtEnemy`** — it is what spends durability.

To scale off the weapon's real damage rather than a constant, use
`WeaponText.itemAttackDamage(stack)`. It reads the stack's own attribute modifiers plus
Sharpness, so an enchanted copy hits proportionally harder.

---

## Left click: swing at air

Vanilla tells the server nothing when you swing and miss, so this needs a packet. The plumbing
already exists — you are adding two lines to it, not building it.

**1. In `weapons/client/WeaponSwingEvents.java`**, add your item to the check:

```java
Item item = held.getItem();
if (item == ModWeapons.DEVILS_DEVASTATION.get() || item == ModWeapons.YOUR_WEAPON.get()) {
    WeaponNetwork.sendToServer(new SwingSlashC2S());
}
```

**2. In `weapons/network/SwingSlashC2S.java`**, add a dispatch line:

```java
private static void dispatch(ServerPlayer player) {
    DevilsDevastationItem.fireFan(player.level(), player);
    YourWeaponItem.onSwing(player.level(), player);   // ← yours
}
```

**3. In your item**, a static method that checks it is actually being held and no-ops otherwise:

```java
public static void onSwing(Level level, Player user) {
    for (InteractionHand hand : InteractionHand.values()) {
        ItemStack stack = user.getItemInHand(hand);
        if (stack.getItem() != ModWeapons.YOUR_WEAPON.get()) {
            continue;
        }
        if (user.getCooldowns().isOnCooldown(stack.getItem())) {
            return;
        }
        // ... your effect. Already on the server; entities can be spawned here.
    }
}
```

Every weapon self-checks, which is why `dispatch` needs no lookup table.

**Why the packet carries nothing:** the client is not trusted to say which weapon or how hard.
The server re-reads the held stack and decides, so a modified client can at most ask to swing
what it is already holding.

> The two client events in `WeaponSwingEvents` — `LeftClickEmpty` and `AttackEntityEvent` — are
> both needed. Minecraft routes a left-click down one path or the other and never both, so
> listening to only one means the weapon stops firing whenever the player happens to connect,
> or happens to miss.

---

## Right click

Override `Item.use`. **It runs on both sides**, so the shape below is the one to copy:

```java
@Override
public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
    ItemStack stack = player.getItemInHand(hand);

    if (player.getCooldowns().isOnCooldown(stack.getItem())) {
        return InteractionResultHolder.fail(stack);
    }

    // Cooldown on both sides: the server enforces it, the client needs it to draw the sweep.
    player.getCooldowns().addCooldown(stack.getItem(), 60);

    if (!level.isClientSide()) {
        // Server only. Spawn entities, deal damage, apply effects, roll randomness here.
        spawnSomething(level, player);
        stack.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(hand));
    } else {
        // Client only. Particles and sounds the player should hear instantly.
        level.playLocalSound(player.getX(), player.getY(), player.getZ(),
                SoundEvents.EVOKER_CAST_SPELL, SoundSource.PLAYERS, 1.0F, 1.0F, false);
    }

    // sidedSuccess makes the arm swing on the client and reports success on the server.
    return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
}
```

**The three rules:**

1. **Guard every side effect.** Spawning an entity outside `!level.isClientSide()` creates a
   ghost that exists only on the client and vanishes on the next sync.
2. **Never roll randomness on both sides.** Client and server will disagree, and the player
   sees one result and gets another. Roll on the server, and send the outcome if the client
   needs it.
3. **Return the right result.** `sidedSuccess` swings the arm; `fail` does nothing at all;
   `pass` lets the click fall through to whatever is behind it.

### Right click a block instead

`use` does **not** fire when the click lands on a block — `useOn` does:

```java
@Override
public InteractionResult useOn(UseOnContext context) {
    BlockPos pos = context.getClickedPos();
    Level level = context.getLevel();
    if (!level.isClientSide()) {
        // ... your effect at pos
    }
    return InteractionResult.sidedSuccess(level.isClientSide());
}
```

Return `InteractionResult.PASS` from `useOn` if you want the click to fall through to `use`.

---

## Shift + right click

There is no separate hook. It is `use` with a branch, and **order matters** — check shift
first, or the normal branch swallows it:

```java
@Override
public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
    ItemStack stack = player.getItemInHand(hand);

    if (player.isShiftKeyDown()) {
        return alternateAbility(level, player, stack);
    }
    return primaryAbility(level, player, stack);
}
```

Two things that trip people up:

- **`isShiftKeyDown()` is the sneak *key*, not the sneak *state*.** Use it here.
  `isCrouching()` is about the hitbox and is false when a player sneaks under a slab, which is
  not what you mean.
- **Shift-right-click on a block never reaches `use`.** Holding shift is exactly what
  suppresses block interaction, so the click goes to `useOn` instead. If your alternate should
  work while pointed at a block, put the shift branch in `useOn` too — or in both, calling one
  shared method.

A common three-way split:

| Input | Hook | Typical use |
|---|---|---|
| left click | `hurtEnemy` / swing packet | the basic attack |
| right click | `use` | the ability |
| shift + right click | `use`, shift branch | mode switch, self-buff, or the ability aimed differently |

For a mode switch, store the mode on the stack's NBT so it survives relogs:

```java
CompoundTag tag = stack.getOrCreateTag();
tag.putInt("Mode", (tag.getInt("Mode") + 1) % 3);
if (!level.isClientSide()) {
    player.displayClientMessage(Component.literal("Mode " + tag.getInt("Mode")), true);
}
```

---

## Hold right click (charged)

Four overrides. `use` starts it, and `releaseUsing` fires when the button comes up:

```java
@Override
public UseAnim getUseAnimation(ItemStack stack) {
    return UseAnim.BOW;          // how the arms are drawn
}

@Override
public int getUseDuration(ItemStack stack) {
    return 72000;                // effectively "until released"
}

@Override
public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
    player.startUsingItem(hand);
    return InteractionResultHolder.consume(player.getItemInHand(hand));
}

@Override
public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int remaining) {
    if (!(entity instanceof Player player) || level.isClientSide()) {
        return;
    }
    int heldTicks = getUseDuration(stack) - remaining;
    float charge = Math.min(heldTicks / 20.0F, 1.0F);   // 1 second to full
    if (charge < 0.25F) {
        return;                  // released too early, nothing happens
    }
    // ... fire at strength `charge`
}
```

`remaining` counts **down**, which is why the charge is `duration - remaining`.

---

## Making a projectile

### 1. The entity class

`weapons/entity/YourProjectile.java`. If it behaves like the Devil's Devastation pair — flies
straight, sweeps for targets, decays — **extend `DevilsProjectile` instead** and you get all of
it for about 30 lines. Otherwise, from scratch:

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

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
```

Three things that will bite you:

- **`getAddEntityPacket` is not optional.** Without it the entity exists server-side and is
  invisible to every client.
- **If you override `tick()` and do not call `super.tick()`**, you own movement entirely —
  nothing writes yaw or pitch, so the renderer has to derive facing from velocity. That is
  exactly what `DevilsProjectileRenderer` does and why.
- **Fields are not synced.** `damage` above exists only on the server. Anything the client must
  know needs `defineSynchedData` and an `EntityDataAccessor`.

### 2. Register the entity type

In `weapons/ModWeapons.java`:

```java
public static final RegistryObject<EntityType<YourProjectile>> YOUR_PROJECTILE =
        ENTITY_TYPES.register("your_projectile", () -> EntityType.Builder
                .<YourProjectile>of(YourProjectile::new, MobCategory.MISC)
                .sized(0.5F, 0.5F)
                .clientTrackingRange(8)
                .updateInterval(1)      // fast movers stutter at the default of 3
                .build("your_projectile"));
```

`MobCategory.MISC` — it is not a mob and must never count against a spawn cap.

### 3. Spawn it

From server-side code (`hurtEnemy`, the swing dispatch, or the `!isClientSide` branch of `use`):

```java
YourProjectile shot = new YourProjectile(level, user.getX(), user.getY() + 0.25, user.getZ(), damage);
shot.setDeltaMovement(user.getLookAngle());
shot.shootFromRotation(user, user.getXRot(), user.getYRot() + yawOffset, 0.0F, speed, inaccuracy);
shot.setOwner(user);
level.addFreshEntity(shot);
```

`setOwner` matters: it is how the projectile avoids hitting the shooter and how kills get
credited. The seemingly redundant `setDeltaMovement` before `shootFromRotation` stops the first
tick running with zero velocity, which otherwise renders the projectile briefly parked on the
player's feet.

For a **fan**, vary `yawOffset` per shot — see `DevilsDevastationItem.spawnFan`, which does
0° and ±25° for one projectile type and ±12.5° for another.

### 4. Renderer and model

`weapons/client/YourProjectileModel.java`:

```java
public class YourProjectileModel extends GeoModel<YourProjectile> {
    @Override public ResourceLocation getModelResource(YourProjectile e) {
        return new ResourceLocation(Priestess.MOD_ID, "geo/entity/your_projectile.geo.json");
    }
    @Override public ResourceLocation getTextureResource(YourProjectile e) {
        return new ResourceLocation(Priestess.MOD_ID, "textures/entity/your_projectile.png");
    }
    @Override public ResourceLocation getAnimationResource(YourProjectile e) {
        return null;    // static geometry
    }
}
```

Then one line in `weapons/client/WeaponsClient.java`:

```java
event.registerEntityRenderer(ModWeapons.YOUR_PROJECTILE.get(), context ->
        new DevilsProjectileRenderer<>(context, new YourProjectileModel()));
```

`DevilsProjectileRenderer` is generic and reusable if your projectile extends
`DevilsProjectile`. It gives velocity-derived facing, translucency and full-bright rendering.
If yours moves normally, a plain `GeoEntityRenderer` is enough.

---

## Cooldowns and durability

**Cooldown** — the grey sweep over the hotbar icon. Set it on **both** sides or the sweep does
not draw:

```java
player.getCooldowns().addCooldown(stack.getItem(), 60);       // ticks
player.getCooldowns().isOnCooldown(stack.getItem());
```

To match the weapon's own swing rate rather than a fixed number:

```java
AttributeInstance attr = player.getAttribute(Attributes.ATTACK_SPEED);
float speed = attr != null ? (float) attr.getValue() : 4.0F;
player.getCooldowns().addCooldown(stack.getItem(), (int) (20.0F / speed));
```

**Durability** — `super.hurtEnemy` already spends it on melee. For an ability, spend it
yourself, server-side only:

```java
stack.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(hand));
```

---

## The name and tooltip

**→ Full detail: [TOOLTIPS.md](TOOLTIPS.md) and [RARITIES.md](RARITIES.md).** The short version:

For a plain name, do nothing — `ModLanguageProvider` supplies it.

For the animated gradient, override `getName` and use `WeaponText.gradient(text, speed, spread,
colours...)`; each `int[]` is one RGB stop and the palette cycles along the text. See
`DevilsDevastationItem.getName`.

For a tooltip with a shift-to-expand section:

```java
@Override
public void appendHoverText(ItemStack stack, @Nullable Level level,
                            List<Component> tooltip, TooltipFlag flag) {
    tooltip.add(Component.translatable("tooltip.priestess.your_weapon.flavour"));
    if (Screen.hasShiftDown()) {
        tooltip.add(Component.translatable("tooltip.priestess.your_weapon.detail"));
    } else {
        tooltip.add(Component.translatable("tooltip.priestess.hold_shift"));
    }
    super.appendHoverText(stack, level, tooltip, flag);
}
```

`tooltip.priestess.hold_shift` already exists; reuse it. Add the rest to `ModLanguageProvider`.

---

## Datagen and assets

One line each in two providers, then regenerate:

```java
// ModItemModelProvider
bigWeapon(ModWeapons.YOUR_WEAPON);      // 64×64 blade, held like a sword — needs _gui sprite too
basicItem(ModWeapons.YOUR_WEAPON.get()); // or this, for a plain 16×16 icon

// ModLanguageProvider
add(ModWeapons.YOUR_WEAPON.get(), "Your Weapon");
add("tooltip.priestess.your_weapon.flavour", "§8« ... »");
```

```
gradlew runData
```

`bigWeapon` needs **both** sprites — `your_weapon.png` at 64×64 and `your_weapon_gui.png` at
16×16 — and handles the `separate_transforms` model, the handheld transforms and the
`gui_light` fix. See [LETHALITY WEAPONS.md](LETHALITY%20WEAPONS.md#the-item-model-is-not-a-basicitem).

Never edit anything under `src/generated/resources/`.

---

## When it does not work

| Symptom | Cause |
|---|---|
| Ability fires twice, or particles double up | side effect not guarded by `!level.isClientSide()` in `use` |
| Nothing happens on right click | returned `fail`/`pass` instead of `sidedSuccess`, or a block ate the click and you need `useOn` |
| Shift-right-click never triggers | pointed at a block — that goes to `useOn`, not `use` |
| Arm does not swing | returned `consume` or `fail`; use `sidedSuccess` |
| Swing at air does nothing, hitting a mob works | forgot the clause in `WeaponSwingEvents` or the line in `SwingSlashC2S.dispatch` |
| Projectile invisible | missing `getAddEntityPacket`, or wrong geo/texture path |
| Projectile appears then vanishes | spawned client-side — move it inside `!level.isClientSide()` |
| Projectile flies sideways facing forward | you override `tick()` without `super.tick()`, so nothing sets yaw/pitch — derive facing from velocity in the renderer |
| Projectile stutters | `updateInterval` left at the default; set `1` |
| Hotbar icon is black | `gui_light` not set on the outer `separate_transforms` model |
| Inventory icon looks over-detailed | no `_gui` sprite, so the 64×64 one is being shrunk |
| Cooldown sweep does not draw | cooldown set server-side only; set it on both |
| Client and server disagree on the result | randomness rolled on both sides; roll server-side only |
| Weapon has no durability loss | overrode `hurtEnemy` without calling `super.hurtEnemy` |

### Checking it in game

```
gradlew runData && gradlew runClient
```

Then `/give @s priestess:your_weapon` and work down the table above. Test right-click abilities
**in multiplayer or on an open LAN world** if you can — a single-player world runs an integrated
server, which hides a good half of the side-confusion bugs listed here.
