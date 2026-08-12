package com.jyhrie.priestess.weapons.client;

import com.jyhrie.priestess.Priestess;
import com.jyhrie.priestess.weapons.entity.DevilsScytheEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

/**
 * Geometry and texture for the scythe projectile.
 *
 * <p>A plain {@link GeoModel} rather than {@code DefaultedEntityGeoModel} — the defaulted one
 * derives an animation path too and warns when it is missing, and this has no animation by
 * design. Returning {@code null} from {@link #getAnimationResource} is the supported way to
 * say "static geometry".
 */
public class DevilsScytheModel extends GeoModel<DevilsScytheEntity> {

    private static final ResourceLocation MODEL =
            new ResourceLocation(Priestess.MOD_ID, "geo/entity/devils_scythe.geo.json");

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(Priestess.MOD_ID, "textures/entity/devils_scythe.png");

    @Override
    public ResourceLocation getModelResource(DevilsScytheEntity entity) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(DevilsScytheEntity entity) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(DevilsScytheEntity entity) {
        return null;
    }
}
