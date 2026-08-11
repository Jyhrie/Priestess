package com.jyhrie.priestess.progression;

import com.jyhrie.priestess.Priestess;
import com.jyhrie.priestess.PriestessConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.Nullable;

/**
 * You cannot dig your way through a dungeon you have not finished.
 *
 * <h2>Two rules, either of which refuses a break</h2>
 * <ol>
 *   <li><b>By place</b> — every block inside the structure of an uncleared dungeon.</li>
 *   <li><b>By block</b> — every block in an uncleared dungeon's
 *       {@linkplain Dungeon#sealedBlocks() tag}, wherever in the world it stands.</li>
 * </ol>
 *
 * The second exists because the first cannot express "this wall is the gate": a structure
 * seals an <em>area</em>, so it catches the generator's backfill too and stops dead at the
 * structure's edge. Tagging the build set gates exactly the dungeon's own blocks, follows them
 * wherever a later build puts them, and is a datapack file — so growing a gate is JSON, not
 * code. The tag check runs first because it is a lookup on a blockstate already in hand.
 *
 * <p>Placing is untouched, and neither rule asks who put a block there. That costs something:
 * a block placed inside a sealed dungeon cannot be taken back until the dungeon is cleared.
 * The alternative is tracking every position a player has built on, which is persistent state
 * to keep correct against creepers, pistons and water.
 *
 * <h2>Three events</h2>
 * <ul>
 *   <li>{@link PlayerInteractEvent.LeftClickBlock} — the refusal. Cancelling stops the dig
 *       before it starts, and it is the only one that reliably fires for a sealed block, so
 *       the message lives here.</li>
 *   <li>{@link PlayerEvent.BreakSpeed} — zero, to hold a dig already in flight.</li>
 *   <li>{@link BlockEvent.BreakEvent} — the authority and backstop for anything that breaks a
 *       block on a player's behalf.</li>
 * </ul>
 *
 * <p>All three run on <b>both sides</b>. Mining is client-predicted, so a server-only refusal
 * looks like the block breaking and then being put back; the client has to refuse too. It can
 * only recognise the block rule — block tags it has, the cleared set arrives on
 * {@link DungeonSync}. The area rule stays server-only; see {@code DungeonSync} for why.
 */
@Mod.EventBusSubscriber(modid = Priestess.MOD_ID)
public final class DungeonLockdown {

    /** Ticks between repeats of the refusal message, per player. Stops a held click spamming. */
    private static final int MESSAGE_COOLDOWN_TICKS = 40;

    /** Not persisted on purpose — a message cooldown that survives a relog is a bug. */
    private static final String LAST_MESSAGE_KEY = "priestess:last_seal_message";

    /** One Forge-bus class per feature, and {@code /dungeon} is part of this one. */
    @SubscribeEvent
    public static void registerCommands(net.minecraftforge.event.RegisterCommandsEvent event) {
        DungeonCommand.register(event.getDispatcher());
    }

    /**
     * {@code START} only: the server fires this for every action in a dig including the
     * release, and cancelling a release would strand a dig that was already under way when the
     * block became sealed. Fires once per press on the server but every tick of the hold on
     * the client, hence the message cooldown and the silent client half.
     */
    @SubscribeEvent
    public static void onLeftClick(PlayerInteractEvent.LeftClickBlock event) {
        if (event.getAction() != PlayerInteractEvent.LeftClickBlock.Action.START) {
            return;
        }
        Player player = event.getEntity();

        if (player.level() instanceof ServerLevel level) {
            Seal sealed = sealAt(player, level, event.getPos());
            if (sealed == null) {
                return;
            }
            event.setCanceled(true);
            if (player instanceof ServerPlayer serverPlayer) {
                refuse(serverPlayer, sealed);
            }
            return;
        }

        if (clientSealsBlock(player, event.getLevel().getBlockState(event.getPos()))) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onBreak(BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)) {
            return;
        }
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        Seal sealed = sealAt(player, level, event.getPos());
        if (sealed == null) {
            return;
        }

        event.setCanceled(true);
        refuse(player, sealed);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        Player player = event.getEntity();

        if (player.level() instanceof ServerLevel level) {
            BlockPos pos = event.getPosition().orElse(null);
            if (pos != null && sealAt(player, level, pos) != null) {
                event.setNewSpeed(0.0F);
            }
            return;
        }

        if (clientSealsBlock(player, event.getState())) {
            event.setNewSpeed(0.0F);
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
     * Picking up the right item clears a dungeon with no boss in it. Rhine Lab is the only
     * one — it ends in a chest, so the Blueprint leaving the floor is the only moment that
     * unambiguously means "done".
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

    /** Which dungeon refused a break, and under which of the two rules — the message differs. */
    private record Seal(Dungeon dungeon, boolean byBlockType) {
    }

    /** The seal that forbids breaking at {@code pos}, or null if the break is allowed. */
    @Nullable
    private static Seal sealAt(Player player, ServerLevel level, BlockPos pos) {
        if (!PriestessConfig.LOCKDOWN_ENABLED.get()) {
            return null;
        }
        // Creative is the build mode; a lockdown that stops an operator fixing a dungeon is
        // a lockdown that gets turned off entirely.
        if (player.isCreative()) {
            return null;
        }

        Dungeon byBlock = sealingBlock(player, level.getBlockState(pos));
        if (byBlock != null) {
            return new Seal(byBlock, true);
        }

        for (Dungeon dungeon : Dungeon.sealable()) {
            if (dungeon.contains(level, pos) && !DungeonProgress.isCleared(player, dungeon)) {
                return new Seal(dungeon, false);
            }
        }
        return null;
    }

    /**
     * The block rule, from synced state rather than the save. A client not yet told anything
     * starts from "nothing cleared" and so errs towards refusing; the server decides anyway.
     */
    private static boolean clientSealsBlock(Player player, BlockState state) {
        if (!DungeonSync.clientLockdownEnabled() || player.isCreative()) {
            return false;
        }
        for (Dungeon dungeon : Dungeon.values()) {
            if (dungeon.seals(state) && !DungeonSync.clientHasCleared(dungeon)) {
                return true;
            }
        }
        return false;
    }

    /**
     * The uncleared dungeon whose tag {@code state} is in, or null.
     *
     * <p>Every dungeon rather than {@link Dungeon#sealable()}: this rule needs no structure,
     * so a dungeon that has not been given one yet still gates its blocks.
     */
    @Nullable
    private static Dungeon sealingBlock(Player player, BlockState state) {
        for (Dungeon dungeon : Dungeon.values()) {
            if (dungeon.seals(state) && !DungeonProgress.isCleared(player, dungeon)) {
                return dungeon;
            }
        }
        return null;
    }

    private static void refuse(ServerPlayer player, Seal seal) {
        if (player.tickCount - lastMessageTick(player) < MESSAGE_COOLDOWN_TICKS) {
            return;
        }
        player.getPersistentData().putInt(LAST_MESSAGE_KEY, player.tickCount);
        // "will not let you dig" is about a place. A gated block can be anywhere — including
        // one a player carried home — so it gets a line that is about the block instead.
        String key = seal.byBlockType()
                ? "message.priestess.dungeon.sealed_block"
                : "message.priestess.dungeon.sealed";
        player.displayClientMessage(Component.translatable(key,
                Component.translatable("dungeon.priestess." + seal.dungeon().getSerializedName())), true);
    }

    private static int lastMessageTick(ServerPlayer player) {
        return player.getPersistentData().getInt(LAST_MESSAGE_KEY);
    }

    private DungeonLockdown() {
    }
}
