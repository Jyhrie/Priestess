package com.jyhrie.priestess.client;

import com.jyhrie.priestess.Priestess;
import com.jyhrie.priestess.entity.ModEntities;
import com.jyhrie.priestess.entity.OriginiumSlug;
import com.jyhrie.priestess.entity.bosses.JesseltonWilliams;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Everything the client needs to draw the Columbia chapter, which is currently three mobs
 * on three different paths — one hand-built mesh, one vanilla mesh, one GeckoLib model.
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
 *   <li><b>"Awaken"</b> — GeckoLib, from a Blockbench model, owning its own renderer; see
 *       {@link AwakenRenderer}.</li>
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

        // The only mob not on the shared renderer. GeckoLib draws from its own model
        // hierarchy, which MobRenderer cannot host, so this one owns its renderer and gets
        // its geometry and texture from resource paths rather than from arguments here.
        event.registerEntityRenderer(ModEntities.AWAKEN.get(), AwakenRenderer::new);
    }

    private static ResourceLocation texture(String name) {
        return new ResourceLocation(Priestess.MOD_ID, "textures/entity/" + name + ".png");
    }

    private PriestessClient() {
    }
}
