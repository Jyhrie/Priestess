package com.jyhrie.priestess.progression;

import com.jyhrie.priestess.Priestess;
import com.jyhrie.priestess.PriestessConfig;
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
 * You cannot mine a block that belongs to a dungeon you have not finished.
 *
 * <p>One rule: a break is refused if the block is in an uncleared dungeon's
 * {@linkplain Dungeon#sealedBlocks() tag}, wherever it stands — <b>a dungeon has no sealed
 * area</b>. Sealing the structure was tried and removed, because gating a <em>region</em>
 * catches the generator's backfill and every pipe and fitting inside the build. Tagging the
 * build set gates exactly the walls, follows them wherever a later build puts them, and is a
 * datapack file.
 *
 * <p>Placing is untouched and the rule does not ask who put a block there, so a gated block a
 * player places is one they cannot take back until the dungeon is cleared. The alternative is
 * tracking every position a player has built on, against creepers, pistons and water.
 *
 * <p>Three events:
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
 * answer the rule in full — block tags ship with the datapack, and the cleared set arrives on
 * {@link DungeonSync}.
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
        Dungeon sealed = sealing(player, event.getLevel().getBlockState(event.getPos()));
        if (sealed == null) {
            return;
        }
        event.setCanceled(true);
        if (player instanceof ServerPlayer serverPlayer) {
            refuse(serverPlayer, sealed);
        }
    }

    @SubscribeEvent
    public static void onBreak(BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)) {
            return;
        }
        Dungeon sealed = sealing(player, event.getState());
        if (sealed == null) {
            return;
        }
        event.setCanceled(true);
        refuse(player, sealed);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        if (sealing(event.getEntity(), event.getState()) != null) {
            event.setNewSpeed(0.0F);
        }
    }

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
     * Picking up the right item clears a dungeon with no boss in it — Rhine Lab ends in a
     * chest, so the Blueprint leaving the floor is the only unambiguous "done".
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

    /**
     * The uncleared dungeon whose tag {@code state} is in, or null if the break is allowed.
     *
     * <p>Runs on both sides and differs only in where the two server-owned answers come from:
     * the save on the server, {@link DungeonSync} on the client. A client not yet told anything
     * starts from "nothing cleared" and so errs towards refusing; the server decides anyway.
     */
    @Nullable
    private static Dungeon sealing(Player player, BlockState state) {
        // Creative is the build mode; a lockdown that stops an operator fixing a dungeon is
        // a lockdown that gets turned off entirely.
        if (player.isCreative()) {
            return null;
        }
        boolean server = !player.level().isClientSide();
        if (!(server ? PriestessConfig.LOCKDOWN_ENABLED.get() : DungeonSync.clientLockdownEnabled())) {
            return null;
        }
        for (Dungeon dungeon : Dungeon.values()) {
            if (!dungeon.seals(state)) {
                continue;
            }
            boolean cleared = server
                    ? DungeonProgress.isCleared(player, dungeon)
                    : DungeonSync.clientHasCleared(dungeon);
            if (!cleared) {
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
        player.displayClientMessage(Component.translatable("message.priestess.dungeon.sealed_block",
                Component.translatable("dungeon.priestess." + dungeon.getSerializedName())), true);
    }

    private static int lastMessageTick(ServerPlayer player) {
        return player.getPersistentData().getInt(LAST_MESSAGE_KEY);
    }

    private DungeonLockdown() {
    }
}
