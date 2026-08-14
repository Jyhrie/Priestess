package com.jyhrie.priestess.weapons.client;

import com.jyhrie.priestess.Priestess;
import com.jyhrie.priestess.weapons.entity.WeaponVfx;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;
import software.bernie.geckolib.model.GeoModel;

/**
 * Geometry, texture and animation for every {@link WeaponVfx}, derived from the entity type's
 * registry name.
 *
 * <p>So {@code priestess:laevatain_slash} reads {@code geo/entity/laevatain_slash.geo.json},
 * {@code textures/entity/laevatain_slash.png} and
 * {@code animations/laevatain_slash.animation.json}. One class covers all of them, and a new
 * effect needs no class at all — just the three files under the same name.
 *
 * <p>Unlike the mob and projectile models this returns a <em>real</em> animation path rather
 * than null. These are the first animated models in the mod; everything else is static
 * geometry with an empty controller.
 */
public class WeaponVfxModel extends GeoModel<WeaponVfx> {

    /** Only reachable if an entity type is somehow unregistered, which would be a bug elsewhere. */
    private static final String FALLBACK = "laevatain_slash";

    @Override
    public ResourceLocation getModelResource(WeaponVfx vfx) {
        return new ResourceLocation(Priestess.MOD_ID, "geo/entity/" + name(vfx) + ".geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(WeaponVfx vfx) {
        return new ResourceLocation(Priestess.MOD_ID, "textures/entity/" + name(vfx) + ".png");
    }

    @Override
    public ResourceLocation getAnimationResource(WeaponVfx vfx) {
        return new ResourceLocation(Priestess.MOD_ID, "animations/" + name(vfx) + ".animation.json");
    }

    private static String name(WeaponVfx vfx) {
        ResourceLocation key = ForgeRegistries.ENTITY_TYPES.getKey(vfx.getType());
        return key != null ? key.getPath() : FALLBACK;
    }
}
