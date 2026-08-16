package com.jyhrie.priestess.progression;

import com.jyhrie.priestess.Priestess;
import com.jyhrie.priestess.PriestessConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.level.biome.Biome;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Whole biomes refuse flight until the dungeon gating them has been cleared.
 *
 * <p>The mechanic exists to stop a player flying over a movement rather than walking through
 * it. Columbia is grounded until Rhine Lab is done, which is the end of Movement I — so the
 * sky is something the chapter hands you, not something you arrive with.
 *
 * <p><b>Read this before trusting it.</b> It works by clearing {@link Abilities#mayfly} and
 * {@link Abilities#flying} every tick, which catches vanilla creative flight and most modded
 * flight — jetpacks, rings, curios — because granting the vanilla ability is far less work
 * than writing a movement controller.
 *
 * <p>It does <b>not</b> catch anything that moves the player directly instead of asking
 * permission. There is no general way to: the only signal is "this player is going up and
 * should not be", which is also what a jump, a bubble column and a boat on ice look like.
 * Elytra are the other hole and are closed separately, since gliding is not a flag either.
 *
 * <p>Leaving a restricted biome hands flight back only to the extent the game mode grants it.
 * A player whose flight came from a mod may have to re-equip it, which is unavoidable without
 * knowing who granted it — and is why {@link #GROUNDED} exists rather than blind re-granting.
 */
@Mod.EventBusSubscriber(modid = Priestess.MOD_ID)
public final class FlightRestriction {

    /**
     * Who we took flight from, so it is handed back only to players we took it from. Without
     * this, walking out of Columbia would grant flight to anyone who never had it.
     *
     * <p>In-memory only. A player who logs out grounded and back in outside the biome simply
     * has whatever their game mode gives them, which is correct.
     */
    private static final Set<UUID> GROUNDED = new HashSet<>();

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        if (!(event.player instanceof ServerPlayer player)) {
            return;
        }
        if (!PriestessConfig.FLIGHT_BAN_ENABLED.get()) {
            release(player);
            return;
        }
        // Spectators pass through the world rather than playing it.
        if (player.isSpectator()) {
            return;
        }
        if (player.isCreative() && PriestessConfig.FLIGHT_BAN_EXEMPTS_CREATIVE.get()) {
            return;
        }

        if (isRestricted(player)) {
            ground(player);
        } else {
            release(player);
        }
    }

    /** Nothing should stay in the set for a player who is no longer here. */
    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        GROUNDED.remove(event.getEntity().getUUID());
    }

    /**
     * Checked every tick rather than on a timer: a biome lookup on a loaded chunk is a palette
     * read, and caching it would buy a stale answer at exactly the moment the player crosses
     * the border.
     */
    private static boolean isRestricted(ServerPlayer player) {
        ResourceKey<Biome> here = player.level().getBiome(player.blockPosition())
                .unwrapKey().orElse(null);
        if (here == null) {
            return false;
        }
        for (Dungeon dungeon : Dungeon.values()) {
            if (dungeon.unlocksFlightIn().contains(here) && !DungeonProgress.isCleared(player, dungeon)) {
                return true;
            }
        }
        return false;
    }

    private static void ground(ServerPlayer player) {
        Abilities abilities = player.getAbilities();
        boolean changed = false;

        if (abilities.mayfly) {
            abilities.mayfly = false;
            changed = true;
        }
        if (abilities.flying) {
            abilities.flying = false;
            changed = true;
        }

        if (changed) {
            player.onUpdateAbilities();
            if (GROUNDED.add(player.getUUID())) {
                player.displayClientMessage(
                        Component.translatable("message.priestess.flight.grounded"), true);
            }
        } else {
            GROUNDED.add(player.getUUID());
        }

        if (player.isFallFlying() && PriestessConfig.FLIGHT_BAN_STOPS_ELYTRA.get()) {
            player.stopFallFlying();
        }
    }

    /**
     * Hands back only what the game mode itself grants, and only to someone we grounded.
     * A mod-granted flight is not re-granted here — see the class note.
     */
    private static void release(ServerPlayer player) {
        if (!GROUNDED.remove(player.getUUID())) {
            return;
        }
        boolean gameModeGrantsFlight = player.isCreative() || player.isSpectator();
        if (gameModeGrantsFlight && !player.getAbilities().mayfly) {
            player.getAbilities().mayfly = true;
            player.onUpdateAbilities();
        }
    }

    private FlightRestriction() {
    }
}
