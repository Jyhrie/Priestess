package com.jyhrie.priestess.progression;

import com.jyhrie.priestess.Priestess;
import com.jyhrie.priestess.PriestessConfig;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Tells the client which dungeons it has cleared, so a sealed block does not <em>look</em>
 * mineable.
 *
 * <p>Mining is client-predicted: the client runs its own destroy timer and, on completion,
 * removes the block and throws the particles without waiting for permission. A server-only
 * {@link DungeonLockdown} therefore showed the block cracking, shattering and reappearing.
 * Refusing on the server makes the rule true; refusing on the client makes it look true.
 *
 * <p>One packet per player: the lockdown switch, and a bit per dungeon. Together with the
 * block tags, which already ship to the client with the datapack, that is everything the rule
 * needs — so the client can answer it in full and a sealed block never moves. This is why the
 * lockdown gates blocks rather than an area: structure starts live in server chunk data that
 * is never sent, so "is this position inside a dungeon" is a question the client could not
 * have answered at any price.
 *
 * <p>Sent on login, respawn and dimension change, and on every write in
 * {@link DungeonProgress} — which is what makes {@code /dungeon seal} land on a player who is
 * standing there. {@code lockdown.enabled} is therefore read at those moments and not after:
 * turning it off mid-session frees the server at once but leaves the client refusing until the
 * next of them. Not worth a config-reload listener for a switch flipped between test runs.
 */
@Mod.EventBusSubscriber(modid = Priestess.MOD_ID)
public final class DungeonSync {

    private static final String PROTOCOL = "1";

    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(Priestess.MOD_ID, "dungeon_progress"),
            () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);

    /**
     * What the client last believed. Static rather than hung off the local player, which is
     * rebuilt on respawn and on a dimension change; this has to outlive both. Never read on a
     * dedicated server — every path to it has already established it is on a client.
     */
    private static volatile Set<Dungeon> clientCleared = EnumSet.noneOf(Dungeon.class);

    private static volatile boolean clientLockdownEnabled = true;

    /** Called from the mod constructor purely to force this class — and its channel — to load. */
    public static void register() {
        CHANNEL.registerMessage(0, Payload.class, Payload::write, Payload::read, Payload::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
    }

    // ── The client's side of the answer ───────────────────────────────────────

    /** Whether the client should behave as though the lockdown is on at all. */
    public static boolean clientLockdownEnabled() {
        return clientLockdownEnabled;
    }

    /** Whether the client believes this player has cleared {@code dungeon}. */
    public static boolean clientHasCleared(Dungeon dungeon) {
        return clientCleared.contains(dungeon);
    }

    // ── Sending ───────────────────────────────────────────────────────────────

    public static void sendTo(ServerPlayer player) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), Payload.of(player));
    }

    /** For shared progress, where one write changes what every player is allowed to break. */
    public static void sendToAll(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            sendTo(player);
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            sendTo(player);
        }
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            sendTo(player);
        }
    }

    @SubscribeEvent
    public static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            sendTo(player);
        }
    }

    // ── The packet ────────────────────────────────────────────────────────────

    /**
     * The lockdown switch and one bit per dungeon. Bit positions are {@link Dungeon} ordinals
     * — the one place here allowed to depend on declaration order, since a packet never
     * outlives a connection and both ends run the same jar.
     */
    public record Payload(boolean lockdownEnabled, int clearedMask) {

        private static Payload of(ServerPlayer player) {
            int mask = 0;
            for (Dungeon dungeon : Dungeon.values()) {
                if (DungeonProgress.isCleared(player, dungeon)) {
                    mask |= 1 << dungeon.ordinal();
                }
            }
            return new Payload(PriestessConfig.LOCKDOWN_ENABLED.get(), mask);
        }

        private void write(FriendlyByteBuf buffer) {
            buffer.writeBoolean(lockdownEnabled);
            buffer.writeVarInt(clearedMask);
        }

        private static Payload read(FriendlyByteBuf buffer) {
            return new Payload(buffer.readBoolean(), buffer.readVarInt());
        }

        private void handle(Supplier<NetworkEvent.Context> context) {
            context.get().enqueueWork(() -> {
                Set<Dungeon> cleared = EnumSet.noneOf(Dungeon.class);
                for (Dungeon dungeon : Dungeon.values()) {
                    if ((clearedMask & (1 << dungeon.ordinal())) != 0) {
                        cleared.add(dungeon);
                    }
                }
                clientCleared = cleared;
                clientLockdownEnabled = lockdownEnabled;
            });
            context.get().setPacketHandled(true);
        }
    }

    private DungeonSync() {
    }
}
