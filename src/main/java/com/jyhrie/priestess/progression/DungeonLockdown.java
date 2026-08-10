package com.jyhrie.priestess.progression;

import com.jyhrie.priestess.Priestess;
import com.jyhrie.priestess.PriestessConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.Nullable;

/**
 * You cannot dig your way through a dungeon you have not finished.
 *
 * <h2>The rule</h2>
 * Inside a dungeon that is not cleared, breaking a block is refused — <em>unless the player
 * put that block there</em>. Placing is untouched. So the dungeon is a fixed shape you have
 * to solve, while everything a player brings with them still behaves: scaffold up, bridge a
 * gap, wall a corridor behind you, then take it all back down again.
 *
 * <p>Refused rather than merely slowed. Mining Fatigue is a tax on patience — it makes
 * digging through a wall take four minutes instead of four seconds, which is worse than
 * either allowing it or forbidding it. This cancels the break outright and says why.
 *
 * <h2>Both halves are needed</h2>
 * <ul>
 *   <li>{@link BlockEvent.BreakEvent} is the authority. Cancelling it is what actually
 *       refuses the break, and Forge makes the server re-send the block so the client's
 *       optimistic removal is undone.</li>
 *   <li>{@link PlayerEvent.BreakSpeed} set to zero stops the server's own destroy progress
 *       ever reaching completion. Without it the block is still refused, but only after the
 *       player has spent the full mining time on it — the refusal should be immediate.</li>
 * </ul>
 *
 * <p>Neither is client-side, so a player mining a sealed block still sees crack particles
 * for as long as they hold the button; the block simply never goes. Making the cracks stop
 * too means telling the client which positions are sealed, which is a sync packet and a
 * whole invalidation problem for a cosmetic gain. The action-bar line is the cheaper answer
 * to the same confusion.
 */
@Mod.EventBusSubscriber(modid = Priestess.MOD_ID)
public final class DungeonLockdown {

    /** Ticks between repeats of the refusal message, per player. Stops a held click spamming. */
    private static final int MESSAGE_COOLDOWN_TICKS = 40;

    /**
     * {@code /dungeon}. Registered here rather than in its own subscriber class for the same
     * reason {@code OripathyEvents} registers {@code /oripathy}: one Forge-bus class per
     * feature, and the command is part of this feature.
     */
    @SubscribeEvent
    public static void registerCommands(net.minecraftforge.event.RegisterCommandsEvent event) {
        DungeonCommand.register(event.getDispatcher());
    }

    @SubscribeEvent
    public static void onBreak(BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)) {
            return;
        }
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        Dungeon sealed = sealedDungeonAt(player, level, event.getPos());
        if (sealed == null) {
            // Not sealed — but it may have been player-placed scaffolding inside a dungeon
            // that is now cleared, or simply anywhere at all. Either way the set should stop
            // tracking a block that no longer exists.
            PlacedBlocks.get(level).forget(event.getPos());
            return;
        }

        event.setCanceled(true);
        refuse(player, sealed);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        // Server only: PlacedBlocks and the progress record are both server-side, so the
        // client cannot answer this question and must not try.
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        BlockPos pos = event.getPosition().orElse(null);
        if (pos == null) {
            return;
        }
        if (sealedDungeonAt(player, level, pos) != null) {
            event.setNewSpeed(0.0F);
        }
    }

    /**
     * Records anything placed inside a sealed dungeon so that it stays mineable.
     *
     * <p>Only inside a sealed one: outside, there is nothing to be exempt from, and
     * recording every block a player has ever placed in the world would be an unbounded set
     * for no gain.
     */
    @SubscribeEvent
    public static void onPlace(BlockEvent.EntityPlaceEvent event) {
        if (!PriestessConfig.LOCKDOWN_ENABLED.get()) {
            return;
        }
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        for (Dungeon dungeon : Dungeon.sealable()) {
            if (dungeon.contains(level, event.getPos()) && !DungeonProgress.isCleared(player, dungeon)) {
                PlacedBlocks.get(level).record(event.getPos());
                return;
            }
        }
    }

    // ── Clearing ──────────────────────────────────────────────────────────────

    /** A boss dying clears the dungeon it belongs to. */
    @SubscribeEvent
    public static void onBossDeath(LivingDeathEvent event) {
        if (!(event.getEntity().level() instanceof ServerLevel level)) {
            return;
        }
        for (Dungeon dungeon : Dungeon.values()) {
            if (dungeon.isClearedBy(event.getEntity().getType())) {
                DungeonProgress.markCleared(level, event.getEntity().blockPosition(), dungeon);
                return;
            }
        }
    }

    /**
     * Picking up the right item clears a dungeon that has no boss in it.
     *
     * <p>Rhine Lab is the only one, and this is why: it ends in a chest rather than a fight,
     * so there is nothing to kill and the Blueprint coming off the floor is the only moment
     * that unambiguously means "done".
     */
    @SubscribeEvent
    public static void onItemPickup(PlayerEvent.ItemPickupEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        for (Dungeon dungeon : Dungeon.values()) {
            if (dungeon.isClearedByPickingUp(event.getStack().getItem())) {
                DungeonProgress.markCleared(level, player.blockPosition(), dungeon);
                return;
            }
        }
    }

    // ── Internals ─────────────────────────────────────────────────────────────

    /**
     * The dungeon that forbids breaking at {@code pos}, or null if the break is allowed.
     *
     * <p>Player-placed blocks return null even inside a sealed dungeon — that check comes
     * first among the reasons to allow, because it is the whole exemption.
     */
    @Nullable
    private static Dungeon sealedDungeonAt(Player player, ServerLevel level, BlockPos pos) {
        if (!PriestessConfig.LOCKDOWN_ENABLED.get()) {
            return null;
        }
        // Creative is the build mode; a lockdown that stops an operator fixing a dungeon is
        // a lockdown that gets turned off entirely.
        if (player.isCreative()) {
            return null;
        }
        if (PlacedBlocks.get(level).isPlayerPlaced(pos)) {
            return null;
        }
        for (Dungeon dungeon : Dungeon.sealable()) {
            if (dungeon.contains(level, pos) && !DungeonProgress.isCleared(player, dungeon)) {
                return dungeon;
            }
        }
        return null;
    }

    private static void refuse(ServerPlayer player, Dungeon dungeon) {
        if (player.tickCount - lastMessageTick(player) < MESSAGE_COOLDOWN_TICKS) {
            return;
        }
        player.getPersistentData().putInt(LAST_MESSAGE_KEY, player.tickCount);
        player.displayClientMessage(Component.translatable("message.priestess.dungeon.sealed",
                Component.translatable("dungeon.priestess." + dungeon.getSerializedName())), true);
    }

    /** Not persisted on purpose — a message cooldown that survives a relog is a bug, not a feature. */
    private static final String LAST_MESSAGE_KEY = "priestess:last_seal_message";

    private static int lastMessageTick(ServerPlayer player) {
        return player.getPersistentData().getInt(LAST_MESSAGE_KEY);
    }

    private DungeonLockdown() {
    }
}
