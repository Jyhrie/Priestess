package com.jyhrie.priestess.block.boss_summons;

import com.jyhrie.priestess.block.BossSummonerBlock;
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
 * The effigy the inmates built of the mercenary who was locked in with them, which turned
 * out to be aimable. Right-click it holding {@link ModItems#TARNISHED_DOG_TAGS} and it
 * projects {@link com.jyhrie.priestess.entity.bosses.MbJesseltonWilliams} — the Jesselton of
 * the assimilated universe, the one who took Mansfield — into the cell block with you.
 *
 * <p>The tags are this world's Jesselton, and they are what the effigy aims along: a likeness
 * and a name are enough to find the same man in a version of events where he won.
 *
 * <p>He is 0.7 x 2.2 and walks, so the inherited clearance check — his own hitbox, one block
 * up — is all this needs. If the altar is buried, he simply steps out. Contrast
 * {@link DorothysTerminalBlock}, whose boss cannot.
 */
public class JesseltonProjectorBlock extends BossSummonerBlock {

    public JesseltonProjectorBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected EntityType<? extends Mob> boss() {
        return ModEntities.MB_JESSELTON_WILLIAMS.get();
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
