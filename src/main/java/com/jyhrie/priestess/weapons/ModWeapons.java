package com.jyhrie.priestess.weapons;

import com.jyhrie.priestess.Priestess;
import com.jyhrie.priestess.weapons.entity.DevilsPitchforkEntity;
import com.jyhrie.priestess.weapons.entity.DevilsScytheEntity;
import com.jyhrie.priestess.weapons.item.DevilsDevastationItem;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Weapons ported in from other mods, and everything they need to exist.
 *
 * <p>This package is deliberately a <em>sealed compartment</em>. Nothing outside
 * {@code weapons/} imports anything inside it except {@link Priestess}, which calls
 * {@link #register} once, and {@code ModCreativeTabs}, which asks it for a list of items to
 * show. Delete the folder and those two references and the mod still builds — which is the
 * point, because none of this is Columbia's and it should stay easy to rip back out.
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

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
        ENTITY_TYPES.register(eventBus);
    }

    private ModWeapons() {
    }
}
