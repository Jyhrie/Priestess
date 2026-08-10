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
 * <h2>How much of "flight" this actually covers — read this before trusting it</h2>
 * Everything here works by clearing {@link Abilities#mayfly} and {@link Abilities#flying}
 * every tick. That is the flag vanilla creative flight uses, and it is also what the large
 * majority of modded flight uses — jetpacks, flight rings, baubles, curios, most
 * armour-granted flight — because granting the vanilla ability is far less work than writing
 * a movement controller. Anything on that path is stopped, whatever mod it came from, with
 * no compatibility code and no mod list.
 *
 * <p><b>What is not stopped:</b> anything that moves the player directly instead of asking
 * permission — a jetpack that adds to {@code deltaMovement} while a key is held never touches
 * the ability flags and will keep working. There is no general way to catch that: the only
 * signal is "this player is going up and should not be", which is also what a jump, a bubble
 * column, a slime block and a boat on ice look like. Catching those properly means a
 * compatibility shim per mod, and that is a real cost to weigh rather than something to
 * pretend is already handled.
 *
 * <p>Elytra are the other hole, and are closed separately — gliding is not a flag either, so
 * {@code stopsElytra} cancels it explicitly. It is on by default because an elytra is
 * otherwise a complete bypass of the mechanic.
 *
 * <h2>Restoring what it took</h2>
 * Leaving a restricted biome hands flight back only to the extent that the game mode grants
 * it. A player whose flight came from a mod may have to re-equip whatever gave it, because
 * most such mods grant on an event rather than every tick, and there is nothing to ask. That
 * is unavoidable without knowing who granted it — and it is why {@link #GROUNDED} exists at
 * all rather than blindly re-granting.
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
        // Spectators are always exempt. They pass through the world rather than playing it,
        // and grounding one is the one case that is never what anybody wanted.
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
     * Whether the player is standing in a biome some uncleared dungeon has grounded.
     *
     * <p>Checked every tick rather than on a timer. A biome lookup on a loaded chunk is a
     * palette read, and the alternative — caching it — buys a rounding error's worth of work
     * in exchange for a stale answer at exactly the moment the player crosses the border.
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
