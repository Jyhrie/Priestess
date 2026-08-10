package com.jyhrie.priestess.progression;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * Every block a player put down inside a sealed dungeon, so that they can take it back out.
 *
 * <p>This is what makes the lockdown a rule about <em>the dungeon</em> rather than a rule
 * about the player's hands. Scaffolding, bridging, walling off a corridor and pillaring up
 * all still work, and all still come back down — you simply cannot dig into anything that
 * was there before you.
 *
 * <h2>Why this stays small</h2>
 * Positions are only recorded when the placement happens inside a dungeon that is currently
 * sealed, and they are dropped again the moment the block is broken. So the set holds
 * scaffolding in progress, not a history of the world: a player who tidies up leaves nothing
 * behind, and one who does not leaves a few hundred longs. It is bounded by what people
 * actually build inside four rooms, which is why a flat {@link LongSet} is enough and there
 * is no per-chunk indexing.
 *
 * <p>Per dimension, unlike {@link DungeonProgress}: a position only means anything alongside
 * the level it is in, and Terra's scaffolding has no business being loaded when someone is
 * in the Overworld.
 *
 * <h2>The one thing it does not survive</h2>
 * A block placed and then destroyed by something that is not a player — a creeper, a piston,
 * water — leaves its position in the set. The next block to occupy that position is then
 * mineable when it should not be. That is a genuine hole, it needs an explosion and a
 * rebuild to hit, and closing it properly means listening to every way a block can vanish;
 * the cheap 90% fix, if it ever matters, is to drop the entry whenever the position is
 * observed to be air.
 */
public final class PlacedBlocks extends SavedData {

    private static final String SAVE_NAME = "priestess_placed_blocks";

    private final LongSet positions = new LongOpenHashSet();

    public static PlacedBlocks get(ServerLevel level) {
        return level.getDataStorage()
                .computeIfAbsent(PlacedBlocks::load, PlacedBlocks::new, SAVE_NAME);
    }

    /** @return true if this position was not already recorded. */
    public boolean record(BlockPos pos) {
        if (positions.add(pos.asLong())) {
            setDirty();
            return true;
        }
        return false;
    }

    public boolean isPlayerPlaced(BlockPos pos) {
        return positions.contains(pos.asLong());
    }

    /** Called when the block comes back out, so the set tracks what is standing, not what was. */
    public void forget(BlockPos pos) {
        if (positions.remove(pos.asLong())) {
            setDirty();
        }
    }

    public int size() {
        return positions.size();
    }

    /**
     * Drops every exemption in this dimension. A testing tool — it makes the dungeon behave
     * as though nobody had ever built in it, which is the state you want back between runs.
     *
     * @return how many were dropped
     */
    public int forgetAll() {
        int had = positions.size();
        if (had > 0) {
            positions.clear();
            setDirty();
        }
        return had;
    }

    private static PlacedBlocks load(CompoundTag tag) {
        PlacedBlocks data = new PlacedBlocks();
        // The array constructor rather than LongOpenHashSet.of(...): the factory mirrors
        // Set.of and throws on a duplicate element. Nothing this class writes can contain
        // one, but a hand-edited or half-written save should not take the world down.
        data.positions.addAll(new LongOpenHashSet(tag.getLongArray("Positions")));
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putLongArray("Positions", positions.toLongArray());
        return tag;
    }
}
