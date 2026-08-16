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
import com.jyhrie.priestess.weapons.item.TemplateWeaponItem;
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
 * <p>This package is a <em>sealed compartment</em>: nothing outside {@code weapons/} imports
 * anything inside it except {@link Priestess}, which calls {@link #register} once, and
 * {@code ModCreativeTabs}. That is why this file carries its own {@link DeferredRegister}s
 * rather than adding to {@code ModItems} and {@code ModEntities} — one shared register that
 * half the mod knows about is how a compartment stops being one.
 *
 * <p>The compartment is still a compile boundary but is <b>no longer disposable</b>: it began
 * as a home for weapons ported from Lethality, and Laevatain and the Greatspear are original
 * content that live here because the scaffolding does. Check {@code item/} before deleting.
 *
 * <p><b>{@code docs/LETHALITY WEAPONS.md} records what changed on the way in.</b> Read it
 * before touching the ported weapons — several behaviours are deliberately stubbed rather
 * than missing.
 */
public final class ModWeapons {

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, Priestess.MOD_ID);

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, Priestess.MOD_ID);

    // Registered on purpose: a template you cannot hold in game is a template that rots. The
    // registry name MUST match TemplateWeaponItem.Model.NAME, or the item generates no model
    // and renders as a missing-texture cube.
    public static final RegistryObject<Item> TEMPLATE_WEAPON =
            ITEMS.register("template_weapon", TemplateWeaponItem::new);

    public static final RegistryObject<Item> DEVILS_DEVASTATION =
            ITEMS.register("devils_devastation", DevilsDevastationItem::new);

    public static final RegistryObject<Item> LAEVATAIN =
            ITEMS.register("laevatain", LaevatainItem::new);

    public static final RegistryObject<Item> AEGIR_GREATSPEAR =
            ITEMS.register("aegir_greatspear", AegirGreatspearItem::new);

    // MISC so nothing counts these against a spawn cap. updateInterval(1) rather than the
    // default 3 because they move fast and die within a second, so at the default the client
    // sees about six position updates across a projectile's whole life and it stutters.

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

    // All one class, differing only in their assets, which WeaponVfxModel finds from the
    // registry names below. None deal damage — the ability has resolved by the time one is
    // spawned. clientTrackingRange matters here: a slash you cannot see from eight blocks away
    // is a bug.

    public static final RegistryObject<EntityType<WeaponVfx>> LAEVATAIN_SLASH =
            ENTITY_TYPES.register("laevatain_slash", () -> vfx("laevatain_slash", 3.0F, 1.0F));

    public static final RegistryObject<EntityType<WeaponVfx>> LAEVATAIN_STAB =
            ENTITY_TYPES.register("laevatain_stab", () -> vfx("laevatain_stab", 1.0F, 1.0F));

    public static final RegistryObject<EntityType<WeaponVfx>> LAEVATAIN_ERUPTION =
            ENTITY_TYPES.register("laevatain_eruption", () -> vfx("laevatain_eruption", 1.0F, 2.0F));

    // The thrown lance has no model — its trail is all you see, so it is bound to
    // InvisibleEntityRenderer and needs no geo, texture or animation.

    public static final RegistryObject<EntityType<AegirTide>> AEGIR_TIDE =
            ENTITY_TYPES.register("aegir_tide", () -> EntityType.Builder
                    .<AegirTide>of(AegirTide::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F)
                    .clientTrackingRange(8)
                    .updateInterval(1)
                    .noSummon()
                    .build("aegir_tide"));

    /**
     * Sized to the pull radius rather than the mesh, so the hitbox and the ability's reach are
     * the same six blocks. It never collides, so a box this large costs nothing.
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
