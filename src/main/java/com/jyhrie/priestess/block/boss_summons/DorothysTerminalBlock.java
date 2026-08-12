package com.jyhrie.priestess.block;

import com.jyhrie.priestess.entity.ModEntities;
import com.jyhrie.priestess.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.Item;
import net.minecraft.world.phys.AABB;

/**
 * A dead Rhine test-chamber console. Right-click it holding
 * {@link ModItems#CORRUPTED_NEURAL_SHARD} and the thing in the pit wakes up with you.
 *
 * <p>Unlike {@link JesseltonProjectorBlock} this one is fussy about space, and has to be.
 * "Awaken" is 6.75 blocks on a side with {@code MOVEMENT_SPEED 0}, no gravity and a no-op
 * {@code push} — it cannot walk, fall or be shoved out of a bad spawn, so a summon into a
 * corridor leaves a boss permanently inside a wall. {@link #clearanceFor} therefore checks
 * a box a whole block wider than the hitbox on each side, which is the difference between
 * "it fits" and "you can fight around it", and the arena is the fight.
 *
 * <p>That is a demanding check now: 6.75 plus a block each side is an 8.75-block span, and
 * the {@code dorothys_vision} boss pit is nine blocks of clear floor. It fits, but only
 * about centred — expect this altar to refuse most rooms that are not purpose-built.
 */
public class DorothysTerminalBlock extends BossSummonerBlock {

    /** Breathing room around the 6.75-block hitbox, in blocks, per side. */
    private static final double ELBOW_ROOM = 1.0;

    public DorothysTerminalBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected EntityType<? extends Mob> boss() {
        return ModEntities.DV_AWAKEN.get();
    }

    @Override
    protected Item catalyst() {
        return ModItems.CORRUPTED_NEURAL_SHARD.get();
    }

    /**
     * Wider than the hitbox on the horizontal axes only. Inflating the vertical too would
     * demand a five-block ceiling for a three-block boss and refuse most rooms it belongs in.
     */
    @Override
    protected AABB clearanceFor(BlockPos pos) {
        return super.clearanceFor(pos).inflate(ELBOW_ROOM, 0.0, ELBOW_ROOM);
    }

    @Override
    protected ParticleOptions summonParticle() {
        return ParticleTypes.SCULK_SOUL;
    }

    @Override
    protected SoundEvent summonSound() {
        return SoundEvents.WARDEN_EMERGE;
    }
}
