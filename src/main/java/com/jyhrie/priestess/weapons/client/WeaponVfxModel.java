package com.jyhrie.priestess.weapons.client;

import com.jyhrie.priestess.Priestess;
import com.jyhrie.priestess.weapons.entity.AegirWhirlpool;
import com.jyhrie.priestess.weapons.entity.WeaponVfx;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.registries.ForgeRegistries;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.model.GeoModel;

/**
 * Geometry, texture and animation for an animated ability effect, all three derived from the
 * entity type's registry name.
 *
 * <p>So {@code priestess:laevatain_slash} reads {@code geo/entity/laevatain_slash.geo.json},
 * {@code textures/entity/laevatain_slash.png} and
 * {@code animations/laevatain_slash.animation.json}. One class covers all of them, and a new
 * effect needs no class at all — just the three files under the same name.
 *
 * <p>Generic over the entity rather than fixed to {@link WeaponVfx}, because
 * {@link AegirWhirlpool} is an animated effect that deliberately is <em>not</em> one. The
 * lookup needs nothing beyond the entity type, so both use this unchanged.
 *
 * <p>Unlike the mob and projectile models this returns a real animation path rather than null:
 * these are the only animated models in the mod.
 */
public class WeaponVfxModel<T extends Entity & GeoEntity> extends GeoModel<T> {

    /** Only reachable if an entity type is somehow unregistered, which would be a bug elsewhere. */
    private static final String FALLBACK = "laevatain_slash";

    @Override
    public ResourceLocation getModelResource(T effect) {
        return new ResourceLocation(Priestess.MOD_ID, "geo/entity/" + name(effect) + ".geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(T effect) {
        return new ResourceLocation(Priestess.MOD_ID, "textures/entity/" + name(effect) + ".png");
    }

    @Override
    public ResourceLocation getAnimationResource(T effect) {
        return new ResourceLocation(Priestess.MOD_ID, "animations/" + name(effect) + ".animation.json");
    }

    private static String name(Entity effect) {
        ResourceLocation key = ForgeRegistries.ENTITY_TYPES.getKey(effect.getType());
        return key != null ? key.getPath() : FALLBACK;
    }
}
