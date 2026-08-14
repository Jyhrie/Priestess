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

    /**
     * Wires up the two predicates that make Laevatain <em>look</em> like it is being drawn.
     *
     * <p>{@code UseAnim.BOW} on its own does less than it sounds like: it gives the player the
     * bow-holding arm pose in third person and, in first person, essentially nothing. The
     * reason a real bow visibly bends is that <b>its item model swaps</b>, through overrides on
     * these two predicates — so a weapon without them draws for a second while looking
     * completely idle in the hand.
     *
     * <ul>
     *   <li>{@code pulling} — 1 while this exact stack is the one being used, else 0.</li>
     *   <li>{@code pull} — 0 to 1 across {@link LaevatainItem#CHARGE_TICKS}.</li>
     * </ul>
     *
     * <p>The overrides that consume them are built in {@code ModItemModelProvider.chargedWeapon}.
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

        // Laevatain's three VFX. One renderer and one model for all of them — the model reads
        // the entity type's registry name to find its assets, so a fourth effect adds no code.
        event.registerEntityRenderer(ModWeapons.LAEVATAIN_SLASH.get(), WeaponVfxRenderer::new);
        event.registerEntityRenderer(ModWeapons.LAEVATAIN_STAB.get(), WeaponVfxRenderer::new);
        event.registerEntityRenderer(ModWeapons.LAEVATAIN_ERUPTION.get(), WeaponVfxRenderer::new);
    }

    private WeaponsClient() {
    }
}
