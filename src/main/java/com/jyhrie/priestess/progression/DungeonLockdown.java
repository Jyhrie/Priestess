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
 * <h2>The rules</h2>
 * There are two, and a break is refused if either says so. Placing is untouched by both, so a
 * dungeon is a fixed shape you have to solve while everything a player brings with them still
 * goes down where they want it.
 *
 * <ol>
 *   <li><b>By place.</b> Every block inside the structure of an uncleared dungeon.</li>
 *   <li><b>By block.</b> Every block in an uncleared dungeon's
 *       {@linkplain Dungeon#sealedBlocks() tag}, wherever in the world it stands.</li>
 * </ol>
 *
 * <p>The second rule exists because the first one cannot express "this wall is the gate". A
 * structure seals an <em>area</em>, so it seals the floor you are standing on and the stone
 * the generator put behind the wall along with it; it also stops dead at the structure's
 * bounding pieces, which means a corridor extended by a later build is not covered. Tagging
 * the build set instead gates exactly the blocks the dungeon is made of, follows them
 * wherever the structure grows, and — the part that matters at scale — is a datapack file.
 * Adding a hundred more Rhine Lab blocks to the gate is a hundred lines of JSON and no code.
 *
 * <p>The tag check runs first because it is a tag lookup on a blockstate that is already in
 * hand, where the area check is a structure lookup; the common case of neither applying
 * should pay the cheap test first.
 *
 * <p>Neither rule asks who put the block there. That is deliberate and it has a cost: a block
 * a player places inside a sealed dungeon — scaffolding, a bridge, a wall across a corridor —
 * is theirs to place and not theirs to take back until the dungeon is cleared, and the same
 * goes for a gated block handed out by an operator and set down in someone's floor. The
 * alternative is tracking every position a player has built on, which is persistent state
 * that has to be kept correct against creepers, pistons and water for a case that only
 * inconveniences the player who chose to build in a dungeon they had not finished.
 *
 * <p>Refused rather than merely slowed. Mining Fatigue is a tax on patience — it makes
 * digging through a wall take four minutes instead of four seconds, which is worse than
 * either allowing it or forbidding it. This cancels the break outright and says why.
 *
 * <h2>Three events, because one is not enough</h2>
 * <ul>
 *   <li>{@link PlayerInteractEvent.LeftClickBlock} is where the refusal happens. Cancelling it
 *       stops the dig before it starts — on the server it re-sends the block and never enters
 *       the destroying state, and on the client it stops the crack overlay and the digging
 *       sound ever beginning. It is also the only one of the three that reliably fires for a
 *       sealed block, which makes it the right place to say why.</li>
 *   <li>{@link PlayerEvent.BreakSpeed} set to zero holds a dig already in flight at zero
 *       progress — a block that becomes sealed under a player's hands, or any path that starts
 *       destroying without a left click.</li>
 *   <li>{@link BlockEvent.BreakEvent} is the authority and the backstop. Nothing in normal play
 *       should reach it now, but anything that breaks a block on a player's behalf does, and
 *       cancelling there is what makes the rule true rather than merely well presented.</li>
 * </ul>
 *
 * <h2>Both sides run this</h2>
 * Mining is client-predicted: the client keeps its own destroy timer and, when that timer
 * finishes, removes the block and throws the break particles without waiting for permission.
 * A server-only refusal therefore looks like the block breaking and then being put back. The
 * client has to refuse too, and it can only refuse what it can recognise — block tags, which
 * it has, against a cleared set that {@link DungeonSync} sends it. The area rule cannot follow
 * it there and stays server-only; see {@code DungeonSync} for why.
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

    /**
     * The one that actually stops the player, on both sides — see the class note.
     *
     * <p>{@code START} only. The server fires this for every action in a dig including the
     * release, and cancelling a release would leave a dig that was already under way when the
     * block became sealed stuck in the server's destroying state; refusing to begin is the
     * whole job. It still fires once per press on the server and every tick of the hold on the
     * client, which is why the message sits behind {@link #MESSAGE_COOLDOWN_TICKS} and the
     * client half says nothing at all.
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

    /**
     * Runs on <em>both</em> sides, and they answer different amounts of the question.
     *
     * <p>The server answers all of it and is the authority. The client answers only the block
     * rule, from what {@link DungeonSync} told it — but that is what stops the crack animation
     * ever starting, so it is the half the player actually sees. See {@code DungeonSync} for
     * why the area rule cannot follow it there.
     */
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
     * The block rule, answered from synced state instead of from the save.
     *
     * <p>Reads nothing the client does not have: block tags ship with the datapack, the
     * cleared set and the config switch arrive on {@link DungeonSync}'s channel, and creative
     * is a flag on the local player. A client that has somehow not been told anything yet
     * starts from "nothing cleared", so it errs towards refusing — and the server, which is
     * the one that decides, is never wrong either way.
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

    /** Not persisted on purpose — a message cooldown that survives a relog is a bug, not a feature. */
    private static final String LAST_MESSAGE_KEY = "priestess:last_seal_message";

    private static int lastMessageTick(ServerPlayer player) {
        return player.getPersistentData().getInt(LAST_MESSAGE_KEY);
    }

    private DungeonLockdown() {
    }
}
