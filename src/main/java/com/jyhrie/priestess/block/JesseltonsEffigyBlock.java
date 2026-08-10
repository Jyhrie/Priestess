package com.jyhrie.priestess.block;

import com.jyhrie.priestess.entity.ModEntities;
import com.jyhrie.priestess.item.ModItems;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.Item;

/**
 * The effigy the inmates built of the mercenary who was locked in with them. Right-click it
 * holding {@link ModItems#TARNISHED_DOG_TAGS} and Jesselton's Shadow comes up out of it.
 *
 * <p>He is 0.7 x 2.2 and walks, so the inherited clearance check — his own hitbox, one block
 * up — is all this needs. If the altar is buried, he simply steps out.
 */
public class JesseltonsEffigyBlock extends BossSummonerBlock {

    public JesseltonsEffigyBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected EntityType<? extends Mob> boss() {
        return ModEntities.JESSELTONS_SHADOW.get();
    }

    @Override
    protected Item catalyst() {
        return ModItems.TARNISHED_DOG_TAGS.get();
    }

    @Override
    protected ParticleOptions summonParticle() {
        return ParticleTypes.SOUL;
    }

    /** The same cue his phase change uses, so the fight opens and turns on one sound. */
    @Override
    protected SoundEvent summonSound() {
        return SoundEvents.WITHER_SPAWN;
    }
}
