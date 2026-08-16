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
 * <p>Mining is client-predicted — the client runs its own destroy timer and removes the block
 * without waiting for permission — so a server-only {@link DungeonLockdown} showed the block
 * cracking, shattering and reappearing. Refusing on the server makes the rule true; refusing
 * on the client makes it look true.
 *
 * <p>One packet per player: the lockdown switch and a bit per dungeon. With the block tags,
 * which already ship with the datapack, that is everything the rule needs. It is also why the
 * lockdown gates blocks rather than an area — structure starts live in server chunk data that
 * is never sent, so the client could not answer "is this position inside a dungeon" at any
 * price.
 *
 * <p>Sent on login, respawn, dimension change and every write in {@link DungeonProgress}, so
 * {@code lockdown.enabled} is read at those moments and not after: turning it off mid-session
 * frees the server at once but leaves the client refusing until the next one.
 */
@Mod.EventBusSubscriber(modid = Priestess.MOD_ID)
public final class DungeonSync {

    private static final String PROTOCOL = "1";

    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(Priestess.MOD_ID, "dungeon_progress"),
            () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);

    /**
     * What the client last believed. Static rather than hung off the local player, which is
     * rebuilt on respawn and on a dimension change; this has to outlive both.
     */
    private static volatile Set<Dungeon> clientCleared = EnumSet.noneOf(Dungeon.class);

    private static volatile boolean clientLockdownEnabled = true;

    /** Called from the mod constructor purely to force this class — and its channel — to load. */
    public static void register() {
        CHANNEL.registerMessage(0, Payload.class, Payload::write, Payload::read, Payload::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
    }

    /** Whether the client should behave as though the lockdown is on at all. */
    public static boolean clientLockdownEnabled() {
        return clientLockdownEnabled;
    }

    /** Whether the client believes this player has cleared {@code dungeon}. */
    public static boolean clientHasCleared(Dungeon dungeon) {
        return clientCleared.contains(dungeon);
    }

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
