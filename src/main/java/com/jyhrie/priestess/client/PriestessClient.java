package com.jyhrie.priestess.client;

import com.jyhrie.priestess.Priestess;
import com.jyhrie.priestess.block.entity.ModBlockEntities;
import com.jyhrie.priestess.entity.ModEntities;
import com.jyhrie.priestess.entity.bosses.MbJesseltonWilliams;
import com.jyhrie.priestess.entity.mobs.dorothysvision.DvBionic;
import com.jyhrie.priestess.entity.mobs.dorothysvision.DvFailure;
import com.jyhrie.priestess.entity.mobs.OriginiumSlug;
import com.jyhrie.priestess.entity.mobs.dorothysvision.DvReplica;
import com.jyhrie.priestess.entity.mobs.mansfieldbreak.MbImprisonedPugilist;
import com.jyhrie.priestess.entity.mobs.mansfieldbreak.MbImprisonedRecidivist;
import com.jyhrie.priestess.entity.mobs.mansfieldbreak.MbImprisonedSniper;
import com.jyhrie.priestess.entity.mobs.undertides.SvCrawler;
import com.jyhrie.priestess.entity.mobs.undertides.SvPiercer;
import com.jyhrie.priestess.entity.mobs.undertides.SvReaper;
import com.jyhrie.priestess.entity.mobs.undertides.SvRunner;
import com.jyhrie.priestess.entity.mobs.undertides.SvSpitter;
import com.jyhrie.priestess.entity.bosses.SvBishopQuintus;
import com.jyhrie.priestess.entity.minibosses.SvTheFirstToTalk;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Everything the client needs to draw the mob roster, which is currently sixteen mobs on
 * three different paths — one hand-built mesh, one vanilla mesh, fourteen GeckoLib models.
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
 *   <li><b>Everything else</b> — the three Medium-bearers, the three Imprisoned and the
 *       seven of Under Tides — GeckoLib, on the shared {@link PriestessGeoRenderer}. Their models are placeholder cube humanoids
 *       waiting to be redrawn in Blockbench; nothing here has to change when they are.</li>
 *   <li><b>"Awaken"</b> — GeckoLib too, but owning its own renderer; see
 *       {@link DvAwakenRenderer} for the two reasons it cannot share.</li>
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

        event.registerEntityRenderer(ModEntities.MB_JESSELTON_WILLIAMS.get(), context ->
                new PriestessMobRenderer<MbJesseltonWilliams>(context,
                        new HumanoidModel<>(context.bakeLayer(ModelLayers.ZOMBIE)),
                        0.7F, texture("mb_jesselton_williams"), 1.15F));

        // GeckoLib draws from its own model hierarchy, which MobRenderer cannot host, so
        // everything below is on the parallel shared renderer instead. Geometry and texture
        // come from resource paths derived from the name, not from arguments here.
        event.registerEntityRenderer(ModEntities.DV_FAILURE.get(), context ->
                new PriestessGeoRenderer<DvFailure>(context, "dv_failure", 0.4F));
        event.registerEntityRenderer(ModEntities.DV_REPLICA.get(), context ->
                new PriestessGeoRenderer<DvReplica>(context, "dv_replica", 0.4F));
        event.registerEntityRenderer(ModEntities.DV_BIONIC.get(), context ->
                new PriestessGeoRenderer<DvBionic>(context, "dv_bionic", 0.6F));

        event.registerEntityRenderer(ModEntities.MB_IMPRISONED_PUGILIST.get(), context ->
                new PriestessGeoRenderer<MbImprisonedPugilist>(context, "mb_imprisoned_pugilist", 0.5F));
        event.registerEntityRenderer(ModEntities.MB_IMPRISONED_RECIDIVIST.get(), context ->
                new PriestessGeoRenderer<MbImprisonedRecidivist>(context, "mb_imprisoned_recidivist", 0.7F));
        // Its bow is not drawn — GeckoLib renders no held items unless the renderer asks, and
        // this one does not. See MbImprisonedSniper's javadoc.
        event.registerEntityRenderer(ModEntities.MB_IMPRISONED_SNIPER.get(), context ->
                new PriestessGeoRenderer<MbImprisonedSniper>(context, "mb_imprisoned_sniper", 0.4F));

        event.registerEntityRenderer(ModEntities.SV_RUNNER.get(), context ->
                new PriestessGeoRenderer<SvRunner>(context, "sv_runner", 0.4F));
        event.registerEntityRenderer(ModEntities.SV_SPITTER.get(), context ->
                new PriestessGeoRenderer<SvSpitter>(context, "sv_spitter", 0.5F));
        event.registerEntityRenderer(ModEntities.SV_REAPER.get(), context ->
                new PriestessGeoRenderer<SvReaper>(context, "sv_reaper", 0.5F));
        event.registerEntityRenderer(ModEntities.SV_CRAWLER.get(), context ->
                new PriestessGeoRenderer<SvCrawler>(context, "sv_crawler", 0.5F));
        event.registerEntityRenderer(ModEntities.SV_PIERCER.get(), context ->
                new PriestessGeoRenderer<SvPiercer>(context, "sv_piercer", 0.4F));
        event.registerEntityRenderer(ModEntities.SV_THE_FIRST_TO_TALK.get(), context ->
                new PriestessGeoRenderer<SvTheFirstToTalk>(context, "sv_the_first_to_talk", 0.8F));
        // Immobile and four blocks tall, but still on the shared renderer: it is drawn at 1:1
        // like everything else, so unlike DvAwaken there is no scale constant to keep in step
        // with its hitbox.
        event.registerEntityRenderer(ModEntities.SV_BISHOP_QUINTUS.get(), context ->
                new PriestessGeoRenderer<SvBishopQuintus>(context, "sv_bishop_quintus", 1.6F));

        // "Awaken" is the exception even among those: its scale is tied to a hitbox constant
        // that has to be documented beside it, and its model has no bone named "head".
        event.registerEntityRenderer(ModEntities.DV_AWAKEN.get(), DvAwakenRenderer::new);

        // The boss altars. A block, not a mob, so this is a block entity renderer — the blocks
        // themselves are RenderShape.INVISIBLE and everything you see comes from here. Bound to
        // the shared block entity type, so one registration covers both altars and
        // BossSummonerModel tells them apart. See docs/BOSS_SPAWNERS.md.
        event.registerBlockEntityRenderer(ModBlockEntities.BOSS_SUMMONER.get(),
                context -> new BossSummonerRenderer());
    }

    private static ResourceLocation texture(String name) {
        return new ResourceLocation(Priestess.MOD_ID, "textures/entity/" + name + ".png");
    }

    private PriestessClient() {
    }
}
