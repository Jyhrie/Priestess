package com.jyhrie.priestess.client;

import com.jyhrie.priestess.Priestess;
import com.jyhrie.priestess.entity.Awaken;
import com.jyhrie.priestess.entity.FailedVision;
import com.jyhrie.priestess.entity.Frank;
import com.jyhrie.priestess.entity.ImprisonedShadow;
import com.jyhrie.priestess.entity.JesseltonsShadow;
import com.jyhrie.priestess.entity.ModEntities;
import com.jyhrie.priestess.entity.OriginiumSlug;
import com.jyhrie.priestess.entity.RhineSecurityDrone;
import com.jyhrie.priestess.entity.RoguePowerArmour;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Everything the client needs to draw the Columbia chapter: the geometry for the three
 * non-humanoid mobs, and a renderer binding for all seven.
 *
 * <p>Client-only, and enforced by {@code value = Dist.CLIENT} rather than by care — a
 * dedicated server that classloads {@code HumanoidModel} crashes, and the annotation is
 * what makes that impossible rather than unlikely.
 *
 * <h2>Humanoids on vanilla's mesh</h2>
 * Four of the seven are people-shaped, and all four use {@link ModelLayers#ZOMBIE} — which
 * is not a zombie, it is exactly {@code HumanoidModel}'s own mesh under a name vanilla
 * happened to register it with. Doing that means the Franks and the shadows walk, swing
 * and turn their heads for free, and the only thing this mod supplies is a 64×64 texture
 * in the standard skin layout, which is the one layout every texture tool already speaks.
 */
@Mod.EventBusSubscriber(modid = Priestess.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class PriestessClient {

    @SubscribeEvent
    public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(PriestessModelLayers.ORIGINIUM_SLUG, PriestessModelLayers::slug);
        event.registerLayerDefinition(PriestessModelLayers.RHINE_SECURITY_DRONE, PriestessModelLayers::drone);
        event.registerLayerDefinition(PriestessModelLayers.FAILED_VISION, PriestessModelLayers::failedVision);
        event.registerLayerDefinition(PriestessModelLayers.AWAKEN, PriestessModelLayers::awaken);
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.ORIGINIUM_SLUG.get(), context ->
                new PriestessMobRenderer<OriginiumSlug>(context,
                        new PriestessEntityModel<>(context.bakeLayer(PriestessModelLayers.ORIGINIUM_SLUG)),
                        0.4F, texture("originium_slug")));

        event.registerEntityRenderer(ModEntities.IMPRISONED_SHADOW.get(), context ->
                new PriestessMobRenderer<ImprisonedShadow>(context,
                        new HumanoidModel<>(context.bakeLayer(ModelLayers.ZOMBIE)),
                        0.5F, texture("imprisoned_shadow")));

        // The boss is the same silhouette as his adds, one and a quarter times the size.
        // That is on purpose: the thing you are fighting should look like what it is
        // summoning, only worse.
        event.registerEntityRenderer(ModEntities.JESSELTONS_SHADOW.get(), context ->
                new PriestessMobRenderer<JesseltonsShadow>(context,
                        new HumanoidModel<>(context.bakeLayer(ModelLayers.ZOMBIE)),
                        0.7F, texture("jesseltons_shadow"), 1.15F));

        event.registerEntityRenderer(ModEntities.FRANK.get(), context ->
                new PriestessMobRenderer<Frank>(context,
                        new HumanoidModel<>(context.bakeLayer(ModelLayers.ZOMBIE)),
                        0.5F, texture("frank")));

        event.registerEntityRenderer(ModEntities.FAILED_VISION.get(), context ->
                new PriestessMobRenderer<FailedVision>(context,
                        new PriestessEntityModel<>(context.bakeLayer(PriestessModelLayers.FAILED_VISION)),
                        1.6F, texture("failed_vision")));

        event.registerEntityRenderer(ModEntities.ROGUE_POWER_ARMOUR.get(), context ->
                new PriestessMobRenderer<RoguePowerArmour>(context,
                        new HumanoidModel<>(context.bakeLayer(ModelLayers.ZOMBIE)),
                        0.6F, texture("rogue_power_armour"), 1.25F));

        // Bobs a full model unit, which at hover height reads as a machine holding station
        // rather than as a block someone left floating.
        event.registerEntityRenderer(ModEntities.RHINE_SECURITY_DRONE.get(), context ->
                new PriestessMobRenderer<RhineSecurityDrone>(context,
                        new PriestessEntityModel<>(context.bakeLayer(PriestessModelLayers.RHINE_SECURITY_DRONE), 1.0F),
                        0.3F, texture("rhine_security_drone")));

        // Bobs, because a cube with no animation at all reads as a block someone placed in
        // mid-air rather than as something holding itself up. The entity does not move; this
        // is entirely the model's doing.
        event.registerEntityRenderer(ModEntities.AWAKEN.get(), context ->
                new PriestessMobRenderer<Awaken>(context,
                        new PriestessEntityModel<>(context.bakeLayer(PriestessModelLayers.AWAKEN), 1.5F),
                        0.8F, texture("awaken")));
    }

    private static ResourceLocation texture(String name) {
        return new ResourceLocation(Priestess.MOD_ID, "textures/entity/" + name + ".png");
    }

    private PriestessClient() {
    }
}
