package com.jyhrie.priestess.client;

import com.jyhrie.priestess.Priestess;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * {@link PriestessMobRenderer}'s opposite number for the GeckoLib mobs.
 *
 * <p>Same argument, different hierarchy. GeckoLib renders from its own {@code GeoModel}
 * through a class tree that never meets {@code MobRenderer}, so the two cannot be one class —
 * but within GeckoLib the mobs still differ in only three values, which name, how big and
 * how large a shadow, and those are arguments rather than files.
 *
 * <p>{@code DvAwakenRenderer} stays its own class and does not use this one: its scale is tied
 * to a hitbox constant that has to be documented next to it, and its model has no bone named
 * {@code head}, so it cannot take the head tracking this switches on.
 *
 * <h2>Where the files have to live</h2>
 * {@link DefaultedEntityGeoModel} derives all three resource paths from {@code name}, so
 * passing {@code "dv_replica"} means exactly:
 * <ul>
 *   <li>{@code assets/priestess/geo/entity/dv_replica.geo.json}</li>
 *   <li>{@code assets/priestess/textures/entity/dv_replica.png}</li>
 *   <li>{@code assets/priestess/animations/entity/dv_replica.animation.json}</li>
 * </ul>
 * The last does not exist for any of them yet and does not need to — GeckoLib resolves it
 * lazily, only when a controller asks for a clip by name, and {@code GeoMonster} registers
 * none.
 *
 * <h2>Head tracking</h2>
 * On, via the two-argument {@code DefaultedEntityGeoModel} constructor, which makes GeckoLib
 * look up a bone literally named {@code head} and point it wherever the mob is looking. Every
 * model this renders has one. A model without it renders with the head locked forward rather
 * than crashing, but that is a bug in the model, not a supported option — keep the bone
 * called {@code head}.
 */
public class PriestessGeoRenderer<T extends Mob & GeoEntity> extends GeoEntityRenderer<T> {

    public PriestessGeoRenderer(EntityRendererProvider.Context context, String name, float shadowRadius) {
        this(context, name, shadowRadius, 1.0F);
    }

    public PriestessGeoRenderer(EntityRendererProvider.Context context, String name,
                                float shadowRadius, float scale) {
        super(context, new DefaultedEntityGeoModel<>(new ResourceLocation(Priestess.MOD_ID, name), true));
        this.shadowRadius = shadowRadius;
        if (scale != 1.0F) {
            // Scales about the entity origin, which sits at the model's feet — these models
            // all run from y=0 upward — so growing one pushes it up rather than sinking it
            // into the floor.
            withScale(scale);
        }
    }
}
