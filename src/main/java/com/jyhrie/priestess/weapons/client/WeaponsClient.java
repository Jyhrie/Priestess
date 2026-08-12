package com.jyhrie.priestess.weapons.client;

import com.jyhrie.priestess.Priestess;
import com.jyhrie.priestess.weapons.ModWeapons;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Binds renderers for everything the weapons package throws.
 *
 * <p>The mod's own {@code client/PriestessClient} does the same job for the mob roster. These
 * are kept apart on purpose: this compartment is meant to be deletable, and a renderer
 * registration sitting in a file outside it would be a compile error the moment the folder
 * went.
 *
 * <p>{@code value = Dist.CLIENT} is load-bearing, not decorative — every class touched here
 * reaches into client-only rendering, and a dedicated server that classloads them dies.
 */
@Mod.EventBusSubscriber(modid = Priestess.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class WeaponsClient {

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModWeapons.DEVILS_SCYTHE.get(), context ->
                new DevilsProjectileRenderer<>(context, new DevilsScytheModel()));
        event.registerEntityRenderer(ModWeapons.DEVILS_PITCHFORK.get(), context ->
                new DevilsProjectileRenderer<>(context, new DevilsPitchforkModel()));
    }

    private WeaponsClient() {
    }
}
