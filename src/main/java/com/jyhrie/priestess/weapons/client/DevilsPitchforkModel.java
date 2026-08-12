package com.jyhrie.priestess.weapons.client;

import com.jyhrie.priestess.Priestess;
import com.jyhrie.priestess.weapons.entity.DevilsPitchforkEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

/** Geometry and texture for the pitchfork projectile. See {@link DevilsScytheModel}. */
public class DevilsPitchforkModel extends GeoModel<DevilsPitchforkEntity> {

    private static final ResourceLocation MODEL =
            new ResourceLocation(Priestess.MOD_ID, "geo/entity/devils_pitchfork.geo.json");

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(Priestess.MOD_ID, "textures/entity/devils_pitchfork.png");

    @Override
    public ResourceLocation getModelResource(DevilsPitchforkEntity entity) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(DevilsPitchforkEntity entity) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(DevilsPitchforkEntity entity) {
        return null;
    }
}
