package com.jyhrie.priestess.client;

import com.jyhrie.priestess.Priestess;
import com.jyhrie.priestess.entity.ModEntities;
import com.jyhrie.priestess.entity.bosses.JesseltonWilliams;
import com.jyhrie.priestess.entity.mobs.Bionic;
import com.jyhrie.priestess.entity.mobs.Failure;
import com.jyhrie.priestess.entity.mobs.OriginiumSlug;
import com.jyhrie.priestess.entity.mobs.Replica;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Everything the client needs to draw the Columbia chapter, which is currently six mobs on
 * three different paths — one hand-built mesh, one vanilla mesh, four GeckoLib models.
 *
 * <p>Client-only, and enforced by {@code value = Dist.CLIENT} rather than by care — a
 * dedicated server that classloads {@code HumanoidModel} crashes, and the annotation is
 * what makes that impossible rather than unlikely.
 *
 * <h2>The three paths</h2>
 * <ul>
 *   <li><b>Originium Slug</b> — a hand-built mesh from {@link PriestessModelLayers}.</li>
 *   <li><b>Jesselton Williams</b> — {@link ModelLayers#ZOMBIE}, which is not a zombie but
 *       {@code HumanoidModel}'s own mesh under a name vanilla happened to register it with.
 *       He walks, swings and turns his head for free, and all this mod supplies is a 64×64
 *       texture in the standard skin layout.</li>
 *   <li><b>Failure, Replica, Bionic</b> — GeckoLib, on the shared
 *       {@link PriestessGeoRenderer}. Their models are placeholder cube humanoids waiting to
 *       be redrawn in Blockbench; nothing here has to change when they are.</li>
 *   <li><b>"Awaken"</b> — GeckoLib too, but owning its own renderer; see
 *       {@link AwakenRenderer} for the two reasons it cannot share.</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = Priestess.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class PriestessClient {

    @SubscribeEvent
    public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(PriestessModelLayers.ORIGINIUM_SLUG, PriestessModelLayers::slug);
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.ORIGINIUM_SLUG.get(), context ->
                new PriestessMobRenderer<OriginiumSlug>(context,
                        new PriestessEntityModel<>(context.bakeLayer(PriestessModelLayers.ORIGINIUM_SLUG)),
                        0.4F, texture("originium_slug")));

        event.registerEntityRenderer(ModEntities.JESSELTON_WILLIAMS.get(), context ->
                new PriestessMobRenderer<JesseltonWilliams>(context,
                        new HumanoidModel<>(context.bakeLayer(ModelLayers.ZOMBIE)),
                        0.7F, texture("jesselton_williams"), 1.15F));

        // GeckoLib draws from its own model hierarchy, which MobRenderer cannot host, so
        // everything below is on the parallel shared renderer instead. Geometry and texture
        // come from resource paths derived from the name, not from arguments here.
        event.registerEntityRenderer(ModEntities.FAILURE.get(), context ->
                new PriestessGeoRenderer<Failure>(context, "failure", 0.4F));
        event.registerEntityRenderer(ModEntities.REPLICA.get(), context ->
                new PriestessGeoRenderer<Replica>(context, "replica", 0.4F));
        event.registerEntityRenderer(ModEntities.BIONIC.get(), context ->
                new PriestessGeoRenderer<Bionic>(context, "bionic", 0.6F));

        // "Awaken" is the exception even among those: its scale is tied to a hitbox constant
        // that has to be documented beside it, and its model has no bone named "head".
        event.registerEntityRenderer(ModEntities.AWAKEN.get(), AwakenRenderer::new);
    }

    private static ResourceLocation texture(String name) {
        return new ResourceLocation(Priestess.MOD_ID, "textures/entity/" + name + ".png");
    }

    private PriestessClient() {
    }
}
