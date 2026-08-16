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
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.UUID;

/**
 * The memory behind a {@link BossSummonerBlock}: which boss it let out, and whether that
 * boss is still walking around.
 *
 * <p>It polls once a second rather than listening for the death, because a death hook only
 * catches deaths: a boss that was {@code /kill}ed, world-edited away, or lost with a rolled-back
 * chunk never dies as far as an event is concerned, and each of those would brick the altar
 * forever. "Is it there" has no failure mode that survives the next second.
 *
 * <p>{@link ServerLevel#getEntity} only sees loaded entities, so a boss in an unloaded chunk
 * reads as gone — hence {@value #MISSES_BEFORE_REARM} consecutive misses before re-arming.
 * What that does not cover is the altar ticking while the boss's chunk stays unloaded for
 * longer. It does not arise today because both bosses spawn on their altar and neither can
 * leave it; if a later boss roams further, record its position alongside the UUID and only
 * count a miss when that position is loaded.
 */
public class BossSummonerBlockEntity extends BlockEntity implements GeoBlockEntity {

    /** One second between checks. The answer cannot change faster than a boss can die. */
    private static final int CHECK_INTERVAL_TICKS = 20;

    /** Consecutive empty answers before the altar is re-armed. See the class notes. */
    private static final int MISSES_BEFORE_REARM = 5;

    private static final String NBT_BOSS = "summoned_boss";

    /** The boss let out of this altar, or null if it is armed and nothing is out. */
    private UUID bossId;

    private int sinceCheck;
    private int misses;

    /**
     * Client-side rendering only; the server builds one too and never looks at it, which is
     * how GeckoLib is designed. The altar is a {@link GeoBlockEntity} because it is drawn by a
     * block entity renderer rather than a baked model — see {@code docs/BOSS_SPAWNERS.md}.
     */
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

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
        // Re-arming detaches the ticker (see BossSummonerBlock.getTicker), so this has to be
        // last — nothing after it would run.
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

    /**
     * No animations, and a controller anyway — the core's spin is written as a bone rotation
     * by the renderer rather than keyframed, but GeckoLib still wants a controller registered.
     *
     * <p>Real clips mean writing
     * {@code assets/priestess/animations/block/boss_summoner.animation.json}, pointing
     * {@code BossSummonerModel.getAnimationResource} at it, and triggering them from here.
     */
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 0, state -> PlayState.CONTINUE));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
