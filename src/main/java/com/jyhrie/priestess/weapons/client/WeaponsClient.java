package com.jyhrie.priestess.weapons.client;

import com.jyhrie.priestess.Priestess;
import com.jyhrie.priestess.weapons.ModWeapons;
import com.jyhrie.priestess.weapons.item.LaevatainItem;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/**
 * Binds renderers for everything the weapons package throws.
 *
 * <p>Kept apart from {@code client/PriestessClient} so the compartment stays deletable.
 *
 * <p>{@code value = Dist.CLIENT} is load-bearing: every class touched here reaches into
 * client-only rendering, and a dedicated server that classloads them dies.
 */
@Mod.EventBusSubscriber(modid = Priestess.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class WeaponsClient {

    /**
     * The two predicates that make Laevatain <em>look</em> like it is being drawn.
     * {@code UseAnim.BOW} alone gives almost nothing in first person — a real bow bends because
     * <b>its item model swaps</b>, through overrides on these two, so a weapon without them
     * draws for a second while looking idle in the hand.
     *
     * <ul>
     *   <li>{@code pulling} — 1 while this exact stack is the one being used, else 0.</li>
     *   <li>{@code pull} — 0 to 1 across {@link LaevatainItem#CHARGE_TICKS}.</li>
     * </ul>
     */
    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            Item laevatain = ModWeapons.LAEVATAIN.get();

            ItemProperties.register(laevatain, new ResourceLocation("pulling"),
                    (stack, level, entity, seed) ->
                            entity != null && entity.isUsingItem() && entity.getUseItem() == stack
                                    ? 1.0F : 0.0F);

            ItemProperties.register(laevatain, new ResourceLocation("pull"),
                    (stack, level, entity, seed) -> {
                        if (entity == null || entity.getUseItem() != stack) {
                            return 0.0F;
                        }
                        int held = stack.getUseDuration() - entity.getUseItemRemainingTicks();
                        return Math.min((float) held / LaevatainItem.CHARGE_TICKS, 1.0F);
                    });
        });
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModWeapons.DEVILS_SCYTHE.get(), context ->
                new DevilsProjectileRenderer<>(context, new DevilsScytheModel()));
        event.registerEntityRenderer(ModWeapons.DEVILS_PITCHFORK.get(), context ->
                new DevilsProjectileRenderer<>(context, new DevilsPitchforkModel()));

        // One renderer and model for all of them — the model reads the entity type's registry
        // name to find its assets, so a fourth effect adds no code.
        event.registerEntityRenderer(ModWeapons.LAEVATAIN_SLASH.get(), WeaponVfxRenderer::new);
        event.registerEntityRenderer(ModWeapons.LAEVATAIN_STAB.get(), WeaponVfxRenderer::new);
        event.registerEntityRenderer(ModWeapons.LAEVATAIN_ERUPTION.get(), WeaponVfxRenderer::new);

        // Bound to a renderer that draws nothing rather than left unbound: an entity type with
        // no renderer logs an error and falls back to a missing-model cube.
        event.registerEntityRenderer(ModWeapons.AEGIR_TIDE.get(), InvisibleEntityRenderer::new);
        event.registerEntityRenderer(ModWeapons.AEGIR_WHIRLPOOL.get(), WeaponVfxRenderer::new);
    }

    private WeaponsClient() {
    }
}
