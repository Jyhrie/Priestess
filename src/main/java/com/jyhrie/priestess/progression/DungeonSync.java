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
 * Tells the client which dungeons it has cleared, so that a sealed block does not <em>look</em>
 * mineable.
 *
 * <h2>Why this has to exist</h2>
 * Mining in Minecraft is client-predicted. The client runs its own copy of the destroy timer,
 * and when that timer completes it removes the block, plays the break sound and throws the
 * break particles <em>without waiting to be told it may</em> — the server's job is only to
 * disagree afterwards, which it does by re-sending the block.
 *
 * <p>{@link DungeonLockdown} used to run entirely on the server, so that disagreement is
 * exactly what a player saw: the block cracked all the way, shattered, and then reappeared.
 * Refusing on the server is what makes the rule true; refusing on the client too is what makes
 * it <em>look</em> true. Both are needed, and the client can only refuse what it knows, which
 * is what this channel is for.
 *
 * <h2>What is sent, and what still cannot be</h2>
 * One packet per player: the lockdown's on/off switch, and a bit per dungeon saying whether
 * that player has cleared it. That is everything the <em>block</em> rule needs — the tag
 * itself is already on the client, because block tags are synced with the rest of the
 * datapack — so a gated block is inert in the client's hands from the first tick.
 *
 * <p>It is <b>not</b> enough for the area rule. "Is this position inside a dungeon" is a
 * structure lookup, and structure starts live in server chunk data that is never sent; the
 * client has no way to answer it and no small amount of data would let it. So breaking the
 * plain stone floor of an uncleared dungeon still cracks and pops back, while breaking one of
 * its Rhine Lab blocks does not. Closing that too means streaming the piece bounding boxes of
 * whichever dungeon the player is standing in, which is a real feature rather than a fix —
 * the block rule is the one a build set is gated with, and it is the one that had to stop
 * lying.
 *
 * <h2>When it is sent</h2>
 * On login, on respawn and on a dimension change, because all three either create the client
 * player or are cheap moments to be sure; and on every write in {@link DungeonProgress}, which
 * is what makes {@code /dungeon seal} take effect on a player who is standing there rather
 * than at their next login.
 *
 * <p>Which means {@code lockdown.enabled} is read at those moments and not after. Turning it
 * off in {@code priestess-server.toml} mid-session frees the server immediately but leaves the
 * client still refusing to start a dig until the next of them — a relog, or any {@code
 * /dungeon} write. Nothing is inconsistent for long and nothing is lost; it is simply not
 * worth a config-reload listener and an off-thread send for a switch that is flipped between
 * test runs.
 */
@Mod.EventBusSubscriber(modid = Priestess.MOD_ID)
public final class DungeonSync {

    private static final String PROTOCOL = "1";

    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(Priestess.MOD_ID, "dungeon_progress"),
            () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);

    /**
     * What the client last believed. Static rather than hung off the player because the local
     * player is thrown away and rebuilt on respawn and on a dimension change, and this should
     * survive both — the server re-sends on those anyway, but a set that empties itself for a
     * tick is a set that un-seals a block for a tick.
     *
     * <p>Never read on a dedicated server: every path that reads it has already established
     * it is on a client. In single player the client and the integrated server share this JVM
     * and therefore this field, which is harmless — there is exactly one player to describe.
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
     * The lockdown switch and one bit per dungeon.
     *
     * <p>Bit positions are {@link Dungeon} ordinals, which is the one place in this feature
     * that is allowed to depend on declaration order: unlike the save data, this never
     * outlives a connection, and both ends of a connection are running the same jar. Forge
     * refuses the connection outright if they are not.
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
