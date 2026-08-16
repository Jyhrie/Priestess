package com.jyhrie.priestess.entity.bosses;

import com.jyhrie.priestess.config.BossStats;
import com.jyhrie.priestess.damage.ModDamageTypes;
import com.jyhrie.priestess.entity.BossMonster;
import com.jyhrie.priestess.entity.projectiles.ArtsBeam;
import com.jyhrie.priestess.item.ModItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Jesselton Williams — not a ghost, and not this world's Jesselton. He is the mercenary who
 * took Mansfield in the assimilated universe, projected into this one by the effigy standing
 * in his cell block. The first boss of Movement I, Columbia: Those who Take the Future.
 *
 * <p>Two phases, different in kind. The prison hands out riot gear on the way in, so phase one
 * answers "was that armour worth carrying" — ordinary kinetic damage every looted point
 * subtracts from. At half health he switches to {@code priestess:void_arts}, which sits in the
 * {@code minecraft:bypasses_armor} tag, so that armour does nothing for the second half.
 *
 * <p>Knockback immunity is an attribute rather than a special case in {@code hurt}.
 */
public class MbJesseltonWilliams extends BossMonster {

    /** Below this fraction of max health he is in phase two, and never goes back. */
    private static final float PHASE_TWO_AT = 0.5F;

    private static final int RANGED_COOLDOWN_TICKS = 45;
    private static final double RANGED_RANGE = 24.0;

    /** How far he will chase before the prison pulls him back. */
    private static final int HOME_RADIUS = 40;

    private int rangedCooldown;
    private boolean announcedPhaseTwo;

    public MbJesseltonWilliams(EntityType<? extends MbJesseltonWilliams> type, Level level) {
        super(type, level, BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.PROGRESS);
        this.xpReward = 250;
    }

    /** Defaults only; {@code EntityStats} overwrites all six from {@code BossStats} on join. */
    public static AttributeSupplier.Builder attributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 220.0)
                .add(Attributes.MOVEMENT_SPEED, 0.28)
                .add(Attributes.ATTACK_DAMAGE, 8.0)
                .add(Attributes.FOLLOW_RANGE, 48.0)
                .add(Attributes.ARMOR, 6.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.0, true));
        this.goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 0.7));
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 32.0F));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    public boolean isPhaseTwo() {
        return this.getHealth() <= this.getMaxHealth() * PHASE_TWO_AT;
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();

        // Anchored on his first server tick, because structures place entities directly rather
        // than through finalizeSpawn — so there is no spawn hook to do it in, and without this
        // he can be walked out of the dungeon and lost.
        if (!this.hasRestriction()) {
            this.restrictTo(this.blockPosition(), HOME_RADIUS);
        }

        if (isPhaseTwo() && !announcedPhaseTwo) {
            enterPhaseTwo();
        }

        LivingEntity target = this.getTarget();
        if (target == null || !target.isAlive()) {
            return;
        }

        if (rangedCooldown > 0) {
            rangedCooldown--;
        }

        castArts(target);
    }

    /**
     * Both phases use the same hitscan beam and differ only in damage type, which is the whole
     * design: the attack looks the same and stops being something armour can answer.
     */
    private void castArts(LivingEntity target) {
        if (rangedCooldown > 0 || this.distanceToSqr(target) > RANGED_RANGE * RANGED_RANGE) {
            return;
        }
        if (!this.hasLineOfSight(target)) {
            return;
        }
        rangedCooldown = RANGED_COOLDOWN_TICKS;

        if (isPhaseTwo()) {
            ArtsBeam.fire(this, target, ModDamageTypes.VOID_ARTS,
                    BossStats.JESSELTON_PHASE_TWO_DAMAGE.get().floatValue(), ParticleTypes.SCULK_SOUL);
        } else {
            ArtsBeam.fire(this, target, ModDamageTypes.SPECTRAL_ARTS,
                    BossStats.JESSELTON_PHASE_ONE_DAMAGE.get().floatValue(), ParticleTypes.CRIT);
        }
        this.playSound(SoundEvents.EVOKER_CAST_SPELL, 1.0F, isPhaseTwo() ? 0.6F : 1.0F);
    }

    /** A damage-type change and a sound. The inmate swarm that went with it is unreplaced. */
    private void enterPhaseTwo() {
        announcedPhaseTwo = true;
        bossEvent.setColor(BossEvent.BossBarColor.RED);
        this.playSound(SoundEvents.WITHER_SPAWN, 2.0F, 1.4F);
    }

    /**
     * Dropped in code rather than from a loot table because it gates the rest of the chapter:
     * exactly one, every time, regardless of Looting or difficulty.
     */
    @Override
    protected void dropCustomDeathLoot(DamageSource source, int looting, boolean recentlyHitByPlayer) {
        super.dropCustomDeathLoot(source, looting, recentlyHitByPlayer);
        this.spawnAtLocation(new ItemStack(ModItems.MANSFIELD_MASTER_KEY.get()));
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.VEX_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.VEX_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.WITHER_DEATH;
    }
}
