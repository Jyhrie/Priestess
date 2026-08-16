package com.jyhrie.priestess.progression;

import com.jyhrie.priestess.PriestessConfig;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashSet;
import java.util.Set;

/**
 * Which dungeons are cleared — for the world, or for one player, depending on the config.
 *
 * <p>Both storages exist at all times and only one is consulted, so flipping
 * {@code sharedProgress} back and forth loses nothing. Nothing migrates between them, because
 * "everyone has cleared it" and "this player has cleared it" are not the same fact.
 *
 * <p><b>Shared</b> is a {@link SavedData} on the <em>Overworld's</em> storage, which makes it
 * unambiguously world-wide rather than per-dimension. <b>Per player</b> is the player's Forge
 * persistent tag, which survives death and respawn for free and needs no capability, provider
 * or clone handler for a set of short strings.
 */
public final class DungeonProgress extends SavedData {

    private static final String SAVE_NAME = "priestess_dungeon_progress";
    /** Key inside the player's Forge-persisted tag. Namespaced — that tag is shared. */
    private static final String PLAYER_KEY = "priestess:cleared_dungeons";

    private final Set<String> cleared = new HashSet<>();

    /**
     * Whether {@code player} may treat {@code dungeon} as done.
     *
     * <p>A dungeon that <em>nothing clears</em> always reads as cleared, so a half-declared
     * dungeon fails open rather than sealing its {@linkplain Dungeon#sealedBlocks() blocks}
     * with no way to ever unseal them.
     */
    public static boolean isCleared(Player player, Dungeon dungeon) {
        if (!dungeon.hasClearCondition()) {
            return true;
        }
        if (PriestessConfig.SHARED_PROGRESS.get()) {
            MinecraftServer server = player.getServer();
            return server != null && shared(server).cleared.contains(dungeon.getSerializedName());
        }
        return personal(player).contains(dungeon.getSerializedName());
    }

    /**
     * Marks {@code dungeon} cleared, for the world or for the players who earned it.
     *
     * <p>In per-player mode this credits everyone within {@link #CREDIT_RADIUS}, not just
     * whoever landed the kill — otherwise two thirds of a co-op group walk back out into a
     * sealed dungeon.
     */
    public static void markCleared(ServerLevel level, net.minecraft.core.BlockPos where, Dungeon dungeon) {
        if (PriestessConfig.SHARED_PROGRESS.get()) {
            DungeonProgress data = shared(level.getServer());
            if (data.cleared.add(dungeon.getSerializedName())) {
                data.setDirty();
                DungeonSync.sendToAll(level.getServer());
                announce(level.getServer().getPlayerList().getPlayers(), dungeon);
            }
            return;
        }

        for (ServerPlayer player : level.getPlayers(p ->
                p.blockPosition().closerThan(where, CREDIT_RADIUS))) {
            if (addPersonal(player, dungeon)) {
                DungeonSync.sendTo(player);
                announce(java.util.List.of(player), dungeon);
            }
        }
    }

    /** How far from a clearing event a player has to be to be credited, in per-player mode. */
    private static final double CREDIT_RADIUS = 64.0;

    // Separate from markCleared, which is the gameplay path and carries the radius credit and
    // the announcement. These set exactly what they are told and say nothing, because an
    // operator undoing a flag should not tell the whole server a dungeon just opened.

    /**
     * Sets or clears the flag in whichever storage the config is currently using.
     *
     * @param player the player whose record to write in per-player mode; ignored in shared
     *               mode, where there is only one record
     * @return whether anything changed
     */
    public static boolean set(ServerPlayer player, Dungeon dungeon, boolean cleared) {
        if (PriestessConfig.SHARED_PROGRESS.get()) {
            DungeonProgress data = shared(player.server);
            boolean changed = cleared
                    ? data.cleared.add(dungeon.getSerializedName())
                    : data.cleared.remove(dungeon.getSerializedName());
            if (changed) {
                data.setDirty();
                DungeonSync.sendToAll(player.server);
            }
            return changed;
        }
        boolean changed = setPersonal(player, dungeon, cleared);
        if (changed) {
            DungeonSync.sendTo(player);
        }
        return changed;
    }

    /** Every dungeon {@code player} counts as having finished, for {@code /dungeon list}. */
    public static Set<Dungeon> clearedFor(Player player) {
        Set<Dungeon> out = new HashSet<>();
        for (Dungeon dungeon : Dungeon.values()) {
            if (isCleared(player, dungeon)) {
                out.add(dungeon);
            }
        }
        return out;
    }

    /** Which storage is live right now. The command prints this so the answer is never ambiguous. */
    public static boolean isShared() {
        return PriestessConfig.SHARED_PROGRESS.get();
    }

    private static DungeonProgress shared(MinecraftServer server) {
        ServerLevel overworld = server.getLevel(Level.OVERWORLD);
        if (overworld == null) {
            // Cannot happen on a running server, but a throwaway beats a crash in a
            // block-break handler.
            return new DungeonProgress();
        }
        return overworld.getDataStorage()
                .computeIfAbsent(DungeonProgress::load, DungeonProgress::new, SAVE_NAME);
    }

    private static DungeonProgress load(CompoundTag tag) {
        DungeonProgress data = new DungeonProgress();
        ListTag list = tag.getList("Cleared", Tag.TAG_STRING);
        for (int i = 0; i < list.size(); i++) {
            data.cleared.add(list.getString(i));
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (String id : cleared) {
            list.add(StringTag.valueOf(id));
        }
        tag.put("Cleared", list);
        return tag;
    }

    private static Set<String> personal(Player player) {
        CompoundTag persisted = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        ListTag list = persisted.getList(PLAYER_KEY, Tag.TAG_STRING);
        Set<String> out = new HashSet<>();
        for (int i = 0; i < list.size(); i++) {
            out.add(list.getString(i));
        }
        return out;
    }

    private static boolean addPersonal(Player player, Dungeon dungeon) {
        return setPersonal(player, dungeon, true);
    }

    private static boolean setPersonal(Player player, Dungeon dungeon, boolean cleared) {
        Set<String> current = personal(player);
        boolean changed = cleared
                ? current.add(dungeon.getSerializedName())
                : current.remove(dungeon.getSerializedName());
        if (!changed) {
            return false;
        }
        ListTag list = new ListTag();
        for (String id : current) {
            list.add(StringTag.valueOf(id));
        }
        // getCompound returns a copy when the key is absent, so the sub-tag has to be put
        // back explicitly or the first write to a fresh player is silently dropped.
        CompoundTag root = player.getPersistentData();
        CompoundTag persisted = root.getCompound(Player.PERSISTED_NBT_TAG);
        persisted.put(PLAYER_KEY, list);
        root.put(Player.PERSISTED_NBT_TAG, persisted);
        return true;
    }

    private static void announce(Iterable<ServerPlayer> to, Dungeon dungeon) {
        Component message = Component.translatable("message.priestess.dungeon.cleared",
                Component.translatable("dungeon.priestess." + dungeon.getSerializedName()));
        for (ServerPlayer player : to) {
            player.displayClientMessage(message, false);
        }
    }
}
