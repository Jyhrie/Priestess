package com.jyhrie.priestess.block;

import com.jyhrie.priestess.progression.Dungeon;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.PushReaction;

/**
 * A block that cannot be mined until its dungeon is cleared.
 *
 * <p>Implemented by {@link SealedBlock} and {@link SealedPillarBlock}, which is where the
 * behaviour actually comes from — this interface exists because Java has one superclass and a
 * sealed block may need to be a pillar, a slab or anything else. Anything implementing it is
 * picked up automatically by {@code ModBlockTagsProvider}, which is what puts it in the
 * {@code sealed_by/<dungeon>} tag the lockdown reads. <b>Declaring the dungeon on the block is
 * the whole registration</b>; there is no second list to keep in step.
 *
 * <p>See {@code docs/DUNGEON_BLOCKS.md}.
 */
public interface DungeonSealed {

    /** The dungeon that has to be cleared before this block can be mined. */
    Dungeon sealedBy();

    /**
     * The three immunities every sealed block gets, applied by the base classes so that no
     * block can be gated and left with a way around the gate.
     *
     * <p>Mining is not the only way a block leaves the world. Bedrock's blast resistance and
     * {@link PushReaction#BLOCK} close explosions and pistons here; the wither is closed by
     * {@code minecraft:wither_immune}, which the tags provider adds for the same reason it
     * adds {@code sealed_by} — vanilla looks in a tag for that one.
     *
     * <p>Unlike the mining gate these never lift. An explosion, a piston and a wither skull
     * all arrive without a player, so there is nobody whose progress could be consulted.
     */
    static BlockBehaviour.Properties seal(BlockBehaviour.Properties properties) {
        return properties
                .explosionResistance(3600000.0F)
                .pushReaction(PushReaction.BLOCK);
    }
}
