# ERRORS.md

Audit of the whole project, 2026-08-14. Every item is a checkbox — tick the ones you want fixed.

Nothing here has been changed. Findings are grouped by kind and ordered by severity within each
group. Where I could not verify something without running the game, it says so.

**Verified clean, for the record:** all 107 lang keys resolve (no missing `tooltip.*` /
`message.*` / `dungeon.*`); every registered block has a loot table; the three Laevatain VFX
have matching geo bone names, clip names (`play`), and `animation_length` × 20 == the tick
constants in `LaevatainItem`; both weapon sprites exist at the required 64×64 + 16×16; no
`TODO`/`FIXME`, no `printStackTrace`, no empty catch blocks, no unused private methods.

---

## A. Correctness bugs

### A1. An off-hand weapon fires when you swing the *other* weapon in your main hand
Off-hand firing is not wanted — confirmed as intended design. The code does not implement that
intent: it implements off-hand firing, but gates it on the main hand, and the two halves
disagree in a way that leaks.

Three pieces:

```java
// WeaponSwingEvents.java:53 — client sends the packet only for the MAIN hand
ItemStack held = player.getMainHandItem();
if (held.getItem() == ModWeapons.DEVILS_DEVASTATION.get()
        || held.getItem() == ModWeapons.LAEVATAIN.get()) { ... }

// SwingSlashC2S.java:53 — server then offers the swing to BOTH weapons, unconditionally
DevilsDevastationItem.fireFan(player.level(), player);
LaevatainItem.sweep(player.level(), player);

// DevilsDevastationItem.java:216 and LaevatainItem.java:254 — each scans BOTH hands
for (InteractionHand hand : InteractionHand.values()) {
    ItemStack stack = user.getItemInHand(hand);
    if (stack.getItem() != ModWeapons.DEVILS_DEVASTATION.get()) continue;
```

The main-hand check decides only *whether a swing happens at all* — it does not decide *which
hand acts*. So with **Laevatain in the main hand and Devil's Devastation in the off hand**, one
left-click passes the main-hand gate, and `fireFan` then finds Devil's Devastation in the off
hand and throws its five-projectile fan. You get the sweep *and* the fan from one swing. It
works in the other pairing too (Devil's Devastation main, Laevatain off → sweep also fires).

Holding a weapon in the off hand with an *empty* main hand correctly does nothing, which is why
this hides: it only shows when both weapons are equipped at once.

Fix: replace the `InteractionHand.values()` loop in both `fireFan` and `sweep` with a main-hand
read, and correct the two javadocs that promise the opposite — *"Checks both hands because the
sword works off-hand"* (`LaevatainItem.java:251`, `DevilsDevastationItem.java:212`). That also
deletes the now-dead off-hand branch in each.

- [ ] Fix

### A2. Sealed-block refusal message goes silent for minutes after a relog — `DungeonLockdown.java:181`

```java
if (player.tickCount - lastMessageTick(player) < MESSAGE_COOLDOWN_TICKS) return;
player.getPersistentData().putInt(LAST_MESSAGE_KEY, player.tickCount);
```

The comment at `DungeonLockdown.java:58` says *"Not persisted on purpose — a message cooldown
that survives a relog is a bug."* That is what was intended, but it is not what happens: the
`ForgeData` root returned by `getPersistentData()` **is** written to the player's `.dat`.
`tickCount`, meanwhile, resets to 0 on relog.

So after playing for N ticks and relogging, `0 - N` is hugely negative, always `< 40`, and the
refusal message is suppressed until `tickCount` climbs back past the stored value — up to the
length of the previous session. The block still correctly refuses to break; the player just
gets no explanation. Fix: use a static in-memory `Map<UUID, Integer>`, matching what
`FlightRestriction.GROUNDED` already does.

- [ ] Fix

### A3. Dorothy's Neural Processor is unobtainable, and its javadoc says otherwise — `ModItems.java:32`

```java
/** Drops from the Failed Vision. Chapter 3's proof of completion. */
public static final RegistryObject<Item> DOROTHYS_NEURAL_PROCESSOR = ...
```

The Failed Vision was removed (`ModEntities.java:284` records that "Awaken" inherited Dorothy's
Vision from it). Nothing calls `spawnAtLocation` with this item — the only three
`dropCustomDeathLoot` overrides drop `DREAMLAND` (DvAwaken), `MANSFIELD_MASTER_KEY`
(MbJesseltonWilliams) and `MEDIUM` (the three Medium-bearers). `docs/SCORE_MOVEMENTS.md:109`
already acknowledges the item is unobtainable, so the gap is known — but the Java comment
still claims it drops, which is the misleading part.

Two separate fixes: correct the comment, and/or give `DvAwaken` the second drop.

- [ ] Fix comment only
- [ ] Also make it obtainable

### A4. `colorFromGradient` throws on an empty palette — `WeaponText.java:67`

```java
if (rgbColors.length < 2) {
    return rgbToInt(rgbColors[0]);   // AIOOBE when length == 0
}
```

The `length < 2` guard admits `length == 0` and then indexes `[0]`. Currently unreachable —
`gradient` returns early on `numColors == 0` before calling this, and both call sites pass
literal palettes — so this is latent, not live. Worth closing since it is a public method.

- [ ] Fix

### A5. `DungeonProgress.shared()` silently discards writes — `DungeonProgress.java:152`

```java
if (overworld == null) {
    return new DungeonProgress();   // throwaway
}
```

The comment says a throwaway beats a crash in a block-break handler, which is right for the
**read** path. But `markCleared` and `set` also go through `shared()`, so on the write path
this silently drops a dungeon-clear — the player kills the boss and nothing is recorded. Given
the comment concedes it "cannot happen on a running server", the low-risk fix is to log a
warning so it is not silent if it ever does.

- [ ] Fix

### A6. Laevatain swings 3× faster than its javadoc says — `LaevatainItem.java:73-78`

```java
/**
 * Offset from the player's base attack speed of 4.0, so the final value is 0.8333 attacks a
 * second — one swing per 1.2 seconds. ...
 */
private static final float ATTACK_SPEED = -1.6F;
```

`SwordItem` applies this as an **ADDITION** modifier to the player's base attack speed of 4.0,
so the real final value is `4.0 - 1.6 =` **2.4 attacks/second — one swing per 0.42 s**, not
0.8333 and 1.2 s. To get the documented rate the constant would have to be about `-3.17`.

This propagates: `sweep` computes its cooldown as `(int)(20.0F / attackSpeed)` and comments
*"At this weapon's attack speed that is 24 ticks — the 1.2 seconds the sword advertises"*
(`LaevatainItem.java:263-265`). It is actually **8 ticks**. The tooltip's "1.2 s" is wrong by
the same factor.

The code is self-consistent — the cooldown does track the real swing rate — so nothing is
broken; the number is just three times what every comment and the tooltip claim. Decide which
is right: change `ATTACK_SPEED` to `-3.17F` to match the design, or correct the three comments
and the tooltip to say 0.42 s.

*(Found while setting Aegir Greatspear's swing rate off the same formula. That weapon's
`-2.8F` → 1.2 attacks/s → 16 ticks is documented correctly.)*

- [ ] Fix the constant (weapon becomes slower, as documented)
- [ ] Fix the comments + tooltip (weapon stays fast)

---

## B. Broken references and stale comments

### B1. `docs/LETHALITY WEAPONS.md` does not exist — referenced 11 times
The file is gone (only `docs/WEAPONS.md` exists), but it is still cited as the authority in:

| File | Line |
|---|---|
| `Priestess.java` | 68 |
| `weapons/ModWeapons.java` | 53 |
| `weapons/item/DevilsDevastationItem.java` | 35, 189 |
| `weapons/entity/DevilsScytheEntity.java` | 33 |
| `weapons/entity/DevilsPitchforkEntity.java` | 32 |
| `datagen/ModItemModelProvider.java` | 39 |
| `datagen/ModLanguageProvider.java` | 96 |
| `docs/TOOLTIPS.md` | 317 (markdown link) |
| `docs/WEAPONS.md` | 9, 723 (markdown links) |

`ModWeapons.java:53` is the worst of them — *"Read it before touching any of this — several
behaviours here are deliberately stubbed rather than missing, and the doc is the list."* That
list is currently unreachable. The three doc links are also broken links a reader will click.

Either restore the file or repoint all 11 at `docs/WEAPONS.md`.

- [ ] Fix

### B2. `Priestess.java:66` describes the weapons package as entirely ported

```java
// Weapons ported in from other mods. Self-contained in weapons/ — these two lines and
// one in ModCreativeTabs are every reference to it from outside the folder, which is
// what keeps it removable. See docs/LETHALITY WEAPONS.md.
```

`ModWeapons`' own javadoc (lines 26–31) and `CLAUDE.md` both say the opposite now: Laevatain is
original and the folder is no longer disposable. This comment is the one place still telling a
reader it is safe to delete.

- [ ] Fix

### B3. The Failed Vision is described as a live entity in 5 files
`BossSummonerBlockEntity.java:47`, `ModDamageTypes.java:30`, `ModDamageTypeTagsProvider.java:37`,
`BossMonster.java:22` all discuss its behaviour in the present tense. It no longer exists.
Low priority — prose only, no code depends on it — but it misleads anyone reading those classes
to understand the boss framework.

- [ ] Fix

---

## C. Naming and convention inconsistencies

### C1. Inline fully-qualified class names instead of imports — 9 files
Everywhere else the codebase imports cleanly. These do not:

| File | Line | Type written out |
|---|---|---|
| `weapons/item/LaevatainItem.java` | 600 | `net.minecraft.sounds.SoundEvent` |
| `weapons/item/DevilsDevastationItem.java` | 269 | `...projectile.AbstractHurtingProjectile` |
| `progression/DungeonProgress.java` | 79, 94 | `net.minecraft.core.BlockPos`, `java.util.List` |
| `progression/DungeonLockdown.java` | 63 | `net.minecraftforge.event.RegisterCommandsEvent` |
| `progression/Dungeon.java` | 84 | `java.util.Locale` |
| `damage/ModDamageTypes.java` | 70 | `net.minecraft.world.entity.Entity` |
| `datagen/ModItemModelProvider.java` | 70, 101, 164 | `net.minecraft.world.item.Item` |
| `datagen/ModLootTableProvider.java` | 133 | `net.minecraft.data.loot.LootTableSubProvider` |
| `entity/minibosses/SvTheFirstToTalk.java` | 122, 128 | `net.minecraft.nbt.CompoundTag` |

- [ ] Fix

### C2. `final` + private constructor applied inconsistently
The `weapons/` package is uniform: every non-instantiable class is `public final` with a private
constructor. The rest of the mod is not. Static-only holders that are **neither** `final` nor
have a private constructor:

`ModBlocks`, `ModItems`, `ModCreativeTabs`, `ModDamageTypes`, `ModBiomes`, `ModDimensions`,
`ModNoiseSettings`, `ModStructures`, `DataGenerators`, `ModLootTableProvider`, `OripathyEvents`,
`Oripathy`, `AnchorReport`

And the reverse — a private constructor but no `final`: `ModEntities`, `ModBlockEntities`,
`ModEffects`, `PriestessClient`. Also `weapons/network/SwingSlashC2S` is the one class in
`weapons/` that is not `final` (it does need a public constructor, so only `final` applies).

- [ ] Fix

### C3. `CREDIT_RADIUS` declared 9 lines after its only use — `DungeonProgress.java:100`
Used at line 91, declared at line 100, in the middle of the file. Every other constant in the
codebase is at the top of its class.

- [ ] Fix

### C4. `ModEntities` import block and attribute registration are both out of order
Imports (`ModEntities.java:4–19`): `mobs.OriginiumSlug` sits between `dorothysvision.DvFailure`
and `dorothysvision.DvReplica`; `bosses.SvBishopQuintus` and `minibosses.SvTheFirstToTalk` trail
after the `mobs.undertides.*` block while the other two `bosses.*` imports lead the file.

`registerAttributes` (lines 308–323) puts `DV_AWAKEN` in the middle, though it is declared last.

- [ ] Fix

### C5. `Dungeon.getSerializedName()` does not implement `StringRepresentable`
`Dungeon.java:83` defines exactly that method signature but the enum declares no interface.
Implementing `StringRepresentable` makes the contract explicit and lets the enum be used
directly by codecs later.

- [ ] Fix

### C6. `LaevatainItem.flame()` is indirection for a constant — `LaevatainItem.java:588`

```java
private static ParticleOptions flame() {
    return ParticleTypes.FLAME;
}
```

One call site, no logic. Should be a constant or inlined.

- [ ] Fix

### C7. `SWEEP_BURN_SECONDS` also drives the melee burn — `LaevatainItem.java:238`
`hurtEnemy` (the ordinary melee hit, not the sweep) applies `SWEEP_BURN_SECONDS`. The value is
probably right; the name says it belongs to a different ability. Rename to something like
`BURN_SECONDS`, or split the two.

- [ ] Fix

---

## D. Duplication

### D1. `sweep` and `fireFan` are the same 20 lines twice
`LaevatainItem.java:253-296` and `DevilsDevastationItem.java:215-243` share: the
`InteractionHand.values()` loop, the item-identity `continue`, the cooldown check, the
`20.0F / attackSpeed` cooldown calculation, the `!isClientSide` guard, and the sound call.
Only the payload differs. A shared helper in `weapons/` — after A1, something like
`WeaponSwing.mainHandHolding(user, item)` returning the stack or empty — would collapse both.

**Do A1 first.** It rewrites the loop that is the bulk of what is duplicated here, so fixing
this one first means writing the shared helper twice.

- [ ] Fix

### D2. `LaevatainItem.playSound` and `DevilsDevastationItem.playSwingSound` are near-identical
Both do `Mth.nextFloat` pitch → `playSound` server / `playLocalSound` client. Same candidate
helper as D1.

Related: both are only ever reached from the swing packet, which is server-only, so the
`playLocalSound` branch in each is dead code.

- [ ] Fix

### D3. Eleven mob classes repeat the same three sound overrides
Every `GeoMonster` subclass overrides `getAmbientSound` / `getHurtSound` / `getDeathSound` with
nothing but a different `SoundEvents` constant — ~15 lines × 11 classes. Could be three
constructor arguments or an abstract `SoundSet` on `GeoMonster`.

This one is a judgement call: the current shape is also how vanilla writes mobs, and it keeps
each mob's file self-contained and readable, which the codebase clearly values. Listed for
completeness rather than recommended.

- [ ] Fix

### D4. `releaseUsing`'s two switch branches are identical — `LaevatainItem.java:376-392`

```java
case CHARGING_MOLTEN_GIANT -> {
    float charge = Math.min((float) heldTicks / CHARGE_TICKS, 1.0F);
    if (charge >= MOLTEN_GIANT_MIN_CHARGE) { castMoltenGiant(...); }
}
case CHARGING_TWILIGHT -> {
    float charge = Math.min((float) heldTicks / CHARGE_TICKS, 1.0F);
    if (charge >= TWILIGHT_MIN_CHARGE) { castTwilight(...); }
}
```

`MOLTEN_GIANT_MIN_CHARGE` and `TWILIGHT_MIN_CHARGE` are both `0.25F`. Hoist the `charge`
computation above the switch; consider one `MIN_CHARGE` constant unless they are meant to
diverge later.

- [ ] Fix

---

## E. File structure and repo hygiene

### E1. 58 build-output files tracked under `run-data/` — 262 KB
`.gitignore` already lists `*.log.gz` and `run-data/logs/*.log`, but **gitignore does not apply
to files already tracked**, so they keep being committed. Seven of them show as modified in the
current working tree, which means every `runData` run dirties the diff:

```
run-data/logs/*.log.gz      (56 files)
run-data/logs/debug.log     (103 KB, modified)
run-data/logs/latest.log    (modified)
run-data/config/fml.toml
```

Fix: `git rm -r --cached run-data/` and commit. The gitignore rules then start working.

- [ ] Fix

### E2. Two dead PNGs ship inside the mod jar — 144 KB
`src/main/resources/data/priestess/terra/elevation_old.png` (131 KB) and `regions_old.png`
(13 KB). Grepped `src/`, `tools/`, `docs/`, `README.md` — **referenced nowhere**. Because they
sit under `src/main/resources` they are packaged into the built jar as dead weight.

- [ ] Delete

### E3. Forge MDK boilerplate left in the repo root
`README.txt` (2 KB, "Source installation information for modders"), `CREDITS.txt` (3 KB, Forge's
own credits), `changelog.txt` (74 KB, Forge's changelog). None are about this project, and
`README.txt` sits directly beside the real `README.md`, which is confusing at a glance.

Note `.gitignore` already has `forge*changelog.txt` — but the file is named `changelog.txt`, so
the pattern misses it.

- [ ] Delete

### E4. `mods.toml` opens with "This is an example mods.toml file"
`src/main/resources/META-INF/mods.toml:1`. The file is real and correct — it is the MDK's
explanatory comment block that was never trimmed. Given `CLAUDE.md` tells contributors never to
edit this file, a header saying it is an example is actively misleading. Trimming the ~30 lines
of MDK tutorial comments while keeping the project's own (the GeckoLib and Curios dependency
notes are genuinely good) would help.

- [ ] Fix

### E5. `tools/__pycache__/` is not gitignored
It exists on disk (`generate_relief_map.cpython-312.pyc`) and is currently untracked, but
nothing stops it being committed — `.gitignore` has no `__pycache__` or `*.pyc` entry.

- [ ] Fix

### E6. `gameTestServer` run config exists with zero tests
`build.gradle:112` defines the run config, and its own comment at line 110 warns *"the server
will crash when no gametests are provided."* `src/test/` contains 0 files and no gametests are
registered anywhere, so `./gradlew runGameTestServer` crashes.

`CLAUDE.md` already documents this, so it is a known state rather than a surprise. Either
delete the run config or add a first gametest.

- [ ] Delete config
- [ ] Add a first test

---

## Summary

| Group | Count | Of which functional |
|---|---|---|
| A. Correctness | 6 | 6 |
| B. Broken references | 3 | 0 (all prose) |
| C. Conventions | 7 | 0 |
| D. Duplication | 4 | 0 |
| E. Repo hygiene | 6 | 0 |

**If you only fix three:** A1 (one swing fires both weapons when you hold two), A2 (the lockdown
stops explaining itself after a relog), E1 (every `runData` run dirties the git diff).

**Ordering note:** A1 before D1 — A1 rewrites the code D1 wants to deduplicate.

**Cheapest wins:** E2 and E3 are pure deletions. B1 is a find-and-replace across 11 sites.
