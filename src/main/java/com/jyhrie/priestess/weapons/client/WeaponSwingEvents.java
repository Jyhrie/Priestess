package com.jyhrie.priestess.weapons.client;

import com.jyhrie.priestess.Priestess;
import com.jyhrie.priestess.weapons.ModWeapons;
import com.jyhrie.priestess.weapons.network.SwingSlashC2S;
import com.jyhrie.priestess.weapons.network.WeaponNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Turns a left-click into a swing packet.
 *
 * <p>Two events, because neither alone sees every swing:
 * {@link PlayerInteractEvent.LeftClickEmpty} fires when the click hits nothing and
 * {@link AttackEntityEvent} when it hits a mob, and Minecraft routes a left-click down one
 * path or the other, never both. Clicking a <em>block</em> is the deliberate gap — that is a
 * mining swing.
 *
 * <p>The held-item check is only an optimisation, to avoid a packet on every punch. The server
 * re-checks what is held and is the authority; see {@link SwingSlashC2S}.
 */
@Mod.EventBusSubscriber(modid = Priestess.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class WeaponSwingEvents {

    @SubscribeEvent
    public static void onLeftClickEmpty(PlayerInteractEvent.LeftClickEmpty event) {
        trySendSwing(Minecraft.getInstance().player);
    }

    @SubscribeEvent
    public static void onAttackEntity(AttackEntityEvent event) {
        // Fires on both sides; only the client half may send to the server.
        if (event.getEntity().level().isClientSide()) {
            trySendSwing(Minecraft.getInstance().player);
        }
    }

    /** Add a weapon to the package by adding it to the check below. */
    private static void trySendSwing(LocalPlayer player) {
        if (player == null) {
            return;
        }
        ItemStack held = player.getMainHandItem();
        if (held.getItem() == ModWeapons.DEVILS_DEVASTATION.get()
                || held.getItem() == ModWeapons.LAEVATAIN.get()
                || held.getItem() == ModWeapons.AEGIR_GREATSPEAR.get()) {
            WeaponNetwork.sendToServer(new SwingSlashC2S());
        }
    }

    private WeaponSwingEvents() {
    }
}
