package com.jyhrie.priestess.weapons;

import com.jyhrie.priestess.Priestess;
import com.jyhrie.priestess.weapons.entity.AegirTide;
import com.jyhrie.priestess.weapons.entity.AegirWhirlpool;
import com.jyhrie.priestess.weapons.entity.DevilsPitchforkEntity;
import com.jyhrie.priestess.weapons.entity.DevilsScytheEntity;
import com.jyhrie.priestess.weapons.entity.WeaponVfx;
import com.jyhrie.priestess.weapons.item.AegirGreatspearItem;
import com.jyhrie.priestess.weapons.item.DevilsDevastationItem;
import com.jyhrie.priestess.weapons.item.LaevatainItem;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Weapons, and everything they need to exist.
 *
 * <p>This package is deliberately a <em>sealed compartment</em>. Nothing outside
 * {@code weapons/} imports anything inside it except {@link Priestess}, which calls
 * {@link #register} once, and {@code ModCreativeTabs}, which asks it for a list of items to
 * show. Delete the folder and those two references and the mod still builds.
 *
 * <p><b>That isolation no longer means "all of this is disposable".</b> It was originally true
 * that nothing here was Columbia's — the package existed to hold weapons ported in from
 * Lethality, and being easy to rip back out was the whole point. Laevatain is original content
 * and lives here anyway, because the scaffolding a weapon needs is here and
 * {@code docs/WEAPONS.md} sends you here to build one. So the compartment still stands as a
 * compile-time boundary, but ripping it out now costs a weapon the mod actually owns. Check
 * what is in {@code item/} before deleting anything.
 *
 * <p>That isolation is why this file carries its own {@link DeferredRegister}s rather than
 * adding to {@code ModItems} and {@code ModEntities}. Two registers against the same Forge
 * registry is entirely normal; one shared register that half the mod has to know about is
 * how a compartment stops being one.
 *
 * <h2>What lives here</h2>
 * <ul>
 *   <li>{@code item/} — the weapon items themselves.</li>
 *   <li>{@code entity/} — the projectiles they throw. These are real entities, unlike
 *       {@code entity/projectiles/ArtsBeam}, because they travel, tumble and are meant to be
 *       dodged rather than arriving instantly.</li>
 *   <li>{@code client/} — GeckoLib models, renderers, and the swing detection. All of it is
 *       {@code Dist.CLIENT}, none of it is safe to classload on a dedicated server.</li>
 *   <li>{@code network/} — the client-to-server packet that turns a swing into projectiles.
 *       A swing is only known to the client, and only the server may spawn entities, so
 *       there has to be a wire between them.</li>
 *   <li>{@link WeaponTiers}, {@link WeaponRarities}, {@link WeaponText} — the shared
 *       scaffolding a ported weapon expects to find.</li>
 * </ul>
 *
 * <p><b>Provenance and what was changed on the way in: {@code docs/LETHALITY WEAPONS.md}.</b>
 * Read it before touching any of this — several behaviours here are deliberately stubbed
 * rather than missing, and the doc is the list.
 */
public final class ModWeapons {

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, Priestess.MOD_ID);

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, Priestess.MOD_ID);

    // ── Devil's Devastation ───────────────────────────────────────────────────
    // A greatsword that throws a fan of five projectiles on every swing: three scythes and
    // two pitchforks, the pitchforks tighter to the crosshair and hitting slightly harder.
    // Ported from Lethality; see the doc named above for what came across and what did not.

    public static final RegistryObject<Item> DEVILS_DEVASTATION =
            ITEMS.register("devils_devastation", DevilsDevastationItem::new);

    // ── Laevatain ─────────────────────────────────────────────────────────────
    // Surtr's greatsword. Three fire abilities on the three click inputs: a sweep on left
    // click, a charged line on right, a cone on shift-right. NOT ported — original content,
    // built to docs/WEAPONS.md. See the note on this class's isolation contract above.

    public static final RegistryObject<Item> LAEVATAIN =
            ITEMS.register("laevatain", LaevatainItem::new);

    // ── Aegir Greatspear ──────────────────────────────────────────────────────
    // Ægir's polearm. Three abilities that all pull: a thrown lance on left click, a 5x5x5 box
    // on right, an eight-second whirlpool on shift-right. NOT ported — original content, and
    // the first weapon here written main-hand only on purpose.

    public static final RegistryObject<Item> AEGIR_GREATSPEAR =
            ITEMS.register("aegir_greatspear", AegirGreatspearItem::new);

    // Both projectiles are MISC: they are not mobs, they carry no attributes, and nothing
    // should ever count them against a spawn cap.
    //
    // updateInterval(1) rather than the default 3. These move fast and die within about a
    // second, so at the default the client would see roughly six position updates across a
    // projectile's whole life and it would visibly stutter. The cost is three times the
    // tracking packets for an entity that barely exists, which is the right trade here and
    // would not be for anything long-lived.

    public static final RegistryObject<EntityType<DevilsScytheEntity>> DEVILS_SCYTHE =
            ENTITY_TYPES.register("devils_scythe", () -> EntityType.Builder
                    .<DevilsScytheEntity>of(DevilsScytheEntity::new, MobCategory.MISC)
                    .sized(1.0F, 1.0F)
                    .clientTrackingRange(8)
                    .updateInterval(1)
                    .build("devils_scythe"));

    public static final RegistryObject<EntityType<DevilsPitchforkEntity>> DEVILS_PITCHFORK =
            ENTITY_TYPES.register("devils_pitchfork", () -> EntityType.Builder
                    .<DevilsPitchforkEntity>of(DevilsPitchforkEntity::new, MobCategory.MISC)
                    .sized(1.0F, 1.0F)
                    .clientTrackingRange(8)
                    .updateInterval(1)
                    .build("devils_pitchfork"));

    // ── Laevatain's VFX ───────────────────────────────────────────────────────
    // Three pieces of animated geometry, all one class — they differ only in their assets, and
    // WeaponVfxModel finds those from the registry name below. None of them deal damage or
    // collide; the abilities have already resolved by the time one is spawned. See WeaponVfx.
    //
    // MISC for the same reason as the projectiles: not mobs, never a spawn-cap cost.
    // updateInterval is irrelevant to something that never moves, but clientTrackingRange is
    // not — a slash you cannot see because you walked eight blocks away is a bug.

    public static final RegistryObject<EntityType<WeaponVfx>> LAEVATAIN_SLASH =
            ENTITY_TYPES.register("laevatain_slash", () -> vfx("laevatain_slash", 3.0F, 1.0F));

    public static final RegistryObject<EntityType<WeaponVfx>> LAEVATAIN_STAB =
            ENTITY_TYPES.register("laevatain_stab", () -> vfx("laevatain_stab", 1.0F, 1.0F));

    public static final RegistryObject<EntityType<WeaponVfx>> LAEVATAIN_ERUPTION =
            ENTITY_TYPES.register("laevatain_eruption", () -> vfx("laevatain_eruption", 1.0F, 2.0F));

    // ── Aegir Greatspear's entities ───────────────────────────────────────────
    // The thrown lance has no model at all — its trail is the whole of what you see, so it is
    // bound to InvisibleEntityRenderer and needs no geo, texture or animation. The whirlpool
    // does have a mesh, and reuses the VFX model and renderer without being a WeaponVfx; see
    // AegirWhirlpool for why it cannot be one.

    public static final RegistryObject<EntityType<AegirTide>> AEGIR_TIDE =
            ENTITY_TYPES.register("aegir_tide", () -> EntityType.Builder
                    .<AegirTide>of(AegirTide::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F)
                    .clientTrackingRange(8)
                    // Same reasoning as the two above: it is fast and short-lived, so the
                    // default of 3 would let the client see it only a handful of times.
                    .updateInterval(1)
                    .noSummon()
                    .build("aegir_tide"));

    /**
     * Sized to the pull radius rather than to the mesh, so what the hitbox says and what the
     * ability actually reaches are the same six blocks. It never collides with anything, so a
     * box this large costs nothing but honesty in the debug view.
     */
    public static final RegistryObject<EntityType<AegirWhirlpool>> AEGIR_WHIRLPOOL =
            ENTITY_TYPES.register("aegir_whirlpool", () -> EntityType.Builder
                    .<AegirWhirlpool>of(AegirWhirlpool::new, MobCategory.MISC)
                    .sized(12.0F, 2.0F)
                    .clientTrackingRange(32)
                    .updateInterval(1)
                    .fireImmune()
                    .noSummon()
                    .build("aegir_whirlpool"));

    private static EntityType<WeaponVfx> vfx(String name, float width, float height) {
        return EntityType.Builder.<WeaponVfx>of(WeaponVfx::new, MobCategory.MISC)
                .sized(width, height)
                .clientTrackingRange(32)
                .updateInterval(1)
                .fireImmune()
                .noSummon()
                .build(name);
    }

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
        ENTITY_TYPES.register(eventBus);
    }

    private ModWeapons() {
    }
}
