package com.jyhrie.priestess.block;

import com.jyhrie.priestess.progression.Dungeon;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;

/**
 * {@link SealedBlock} for a block that also needs an axis — see {@link DungeonSealed}.
 *
 * <p>Identical in every way that matters; it exists only because {@link RotatedPillarBlock} is
 * the superclass a pillar has to have, and a class can only have one. A sealed slab or stair
 * would need the same treatment.
 */
public class SealedPillarBlock extends RotatedPillarBlock implements DungeonSealed {

    private final Dungeon sealedBy;

    public SealedPillarBlock(Dungeon sealedBy, BlockBehaviour.Properties properties) {
        super(DungeonSealed.seal(properties));
        this.sealedBy = sealedBy;
    }

    @Override
    public Dungeon sealedBy() {
        return sealedBy;
    }
}
