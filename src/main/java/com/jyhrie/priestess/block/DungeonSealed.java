package com.jyhrie.priestess.block;

import com.jyhrie.priestess.progression.Dungeon;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.PushReaction;

/**
 * A block that cannot be mined until its dungeon is cleared.
 *
 * <p>An interface because Java has one superclass and a sealed block may need to be a pillar,
 * a slab or anything else; the behaviour lives in {@link SealedBlock} and
 * {@link SealedPillarBlock}. Anything implementing it is picked up automatically by
 * {@code ModBlockTagsProvider} and put in the {@code sealed_by/<dungeon>} tag the lockdown
 * reads, so <b>declaring the dungeon on the block is the whole registration</b>.
 *
 * <p>See {@code docs/DUNGEON_BLOCKS.md}.
 */
public interface DungeonSealed {

    /** The dungeon that has to be cleared before this block can be mined. */
    Dungeon sealedBy();

    /**
     * Mining is not the only way a block leaves the world. Bedrock's blast resistance and
     * {@link PushReaction#BLOCK} close explosions and pistons here; the wither is closed by
     * {@code minecraft:wither_immune}, which the tags provider adds because vanilla looks in a
     * tag for that one.
     *
     * <p>Unlike the mining gate these never lift: an explosion, a piston and a wither skull
     * all arrive without a player, so there is nobody whose progress could be consulted.
     */
    static BlockBehaviour.Properties seal(BlockBehaviour.Properties properties) {
        return properties
                .explosionResistance(3600000.0F)
                .pushReaction(PushReaction.BLOCK);
    }
}
