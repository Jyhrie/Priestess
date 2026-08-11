package com.jyhrie.priestess.block;

import com.jyhrie.priestess.progression.Dungeon;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;

/**
 * The base class for a block gated behind a dungeon — see {@link DungeonSealed}.
 *
 * <p>Pass the dungeon and ordinary properties; the immunities are applied for you. The
 * properties should describe the <em>material</em> (what it is made of, what tool it takes)
 * and say nothing about the gate.
 *
 * <pre>
 * new SealedBlock(Dungeon.UNDER_TIDES, BlockBehaviour.Properties.copy(Blocks.DEEPSLATE_BRICKS))
 * </pre>
 */
public class SealedBlock extends Block implements DungeonSealed {

    private final Dungeon sealedBy;

    public SealedBlock(Dungeon sealedBy, BlockBehaviour.Properties properties) {
        super(DungeonSealed.seal(properties));
        this.sealedBy = sealedBy;
    }

    @Override
    public Dungeon sealedBy() {
        return sealedBy;
    }
}
