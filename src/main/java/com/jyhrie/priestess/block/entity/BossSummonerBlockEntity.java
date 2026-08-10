package com.jyhrie.priestess.block.entity;

import com.jyhrie.priestess.block.BossSummonerBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.UUID;

/**
 * The memory behind a {@link BossSummonerBlock}: which boss it let out, and whether that
 * boss is still walking around.
 *
 * <h2>How it knows the fight is over</h2>
 * It polls. Once a second it asks the level for the entity it summoned, and when the answer
 * has been "nothing" {@value #MISSES_BEFORE_REARM} times running it re-arms the altar.
 *
 * <p>Polling rather than listening for the death: a death hook only catches deaths. A boss
 * that was {@code /kill}ed, removed by a world edit, or lost with a chunk that got rolled
 * back never dies as far as an event is concerned, and every one of those would leave an
 * altar bricked forever with no way to fix it short of breaking the block. Asking "is it
 * there" instead of "did it die" cannot get stuck, because the question has no failure mode
 * that survives the next second.
 *
 * <h2>The grace period, and the one case it does not cover</h2>
 * {@link ServerLevel#getEntity} only sees loaded entities, so a boss in an unloaded chunk
 * reads as gone. That is what {@value #MISSES_BEFORE_REARM} consecutive misses is for — it
 * costs five seconds after a real kill, which is if anything an improvement, and it rides
 * out a chunk that is briefly between owners.
 *
 * <p>What it cannot cover is the altar ticking while the boss's chunk stays unloaded for
 * longer than that. In practice this does not arise: both bosses spawn on top of their altar
 * and neither can leave it — the Failed Vision cannot move at all and Jesselton is leashed to
 * 40 blocks — so anything close enough to keep the altar ticking is keeping the boss loaded
 * too. If a later boss roams further, this is the assumption that breaks, and the fix is to
 * record the boss's position alongside its UUID and only count a miss when that position is
 * loaded.
 */
public class BossSummonerBlockEntity extends BlockEntity {

    /** One second between checks. The answer cannot change faster than a boss can die. */
    private static final int CHECK_INTERVAL_TICKS = 20;

    /** Consecutive empty answers before the altar is re-armed. See the class notes. */
    private static final int MISSES_BEFORE_REARM = 5;

    private static final String NBT_BOSS = "summoned_boss";

    /** The boss let out of this altar, or null if it is armed and nothing is out. */
    private UUID bossId;

    private int sinceCheck;
    private int misses;

    public BossSummonerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BOSS_SUMMONER.get(), pos, state);
    }

    /** Called by the block the moment it summons. */
    public void watch(UUID boss) {
        this.bossId = boss;
        this.misses = 0;
        this.sinceCheck = 0;
        setChanged();
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state,
                                  BossSummonerBlockEntity summoner) {
        if (++summoner.sinceCheck < CHECK_INTERVAL_TICKS) {
            return;
        }
        summoner.sinceCheck = 0;

        if (summoner.bossAlive((ServerLevel) level)) {
            summoner.misses = 0;
            return;
        }
        if (++summoner.misses < MISSES_BEFORE_REARM) {
            return;
        }
        summoner.rearm(level, pos, state);
    }

    private boolean bossAlive(ServerLevel level) {
        if (bossId == null) {
            return false;
        }
        Entity boss = level.getEntity(bossId);
        return boss != null && boss.isAlive();
    }

    private void rearm(Level level, BlockPos pos, BlockState state) {
        bossId = null;
        misses = 0;
        setChanged();
        // Re-arming is a state change, which detaches the ticker — see
        // BossSummonerBlock.getTicker. This is the last thing that happens here for that
        // reason: nothing after it would run.
        level.setBlock(pos, state.setValue(BossSummonerBlock.ARMED, true), Block.UPDATE_ALL);
        level.playSound(null, pos, SoundEvents.RESPAWN_ANCHOR_CHARGE, SoundSource.BLOCKS, 1.0F, 1.4F);
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                    pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 20, 0.3, 0.3, 0.3, 0.02);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (bossId != null) {
            tag.putUUID(NBT_BOSS, bossId);
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        // Absent means armed. A spent altar always writes its boss, so a missing key is
        // either a fresh block or one saved while armed — both want null.
        bossId = tag.hasUUID(NBT_BOSS) ? tag.getUUID(NBT_BOSS) : null;
    }
}
