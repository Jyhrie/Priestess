package com.jyhrie.priestess.entity;

import com.jyhrie.priestess.damage.ModDamageTypes;
import com.jyhrie.priestess.item.ModItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
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
 * Jesselton's Shadow — what is left of the mercenary who tried to take Mansfield and got
 * locked in it instead. The first boss of the Columbia chapter.
 *
 * <h2>Two phases, and why they are different in kind</h2>
 * The prison hands out riot gear on the way in, so the fight has to answer the question
 * "was that armour worth carrying". Phase one says yes: spectral iron arts, heavy but
 * ordinary kinetic damage, and every point of armour you looted subtracts from it.
 *
 * <p>At half health he stops caring. Phase two is {@code priestess:void_arts}, which sits in
 * the {@code minecraft:bypasses_armor} tag, and he starts pulling the dead inmates out of
 * their cells to swarm you. The armour that carried the first half of the fight does
 * nothing for the second, and the answer has to be movement instead.
 *
 * <p>He is knockback-immune by attribute rather than by a special case in {@code hurt} —
 * he is a ghost, not a heavy, and the difference the player notices is that a shield bash
 * moves everything in the cell block except him.
 */
public class JesseltonsShadow extends BossMonster {

    /** Below this fraction of max health he is in phase two, and never goes back. */
    private static final float PHASE_TWO_AT = 0.5F;

    private static final int RANGED_COOLDOWN_TICKS = 45;
    private static final double RANGED_RANGE = 24.0;
    private static final float PHASE_ONE_DAMAGE = 9.0F;
    private static final float PHASE_TWO_DAMAGE = 7.0F;

    private static final int SUMMON_COOLDOWN_TICKS = 160;
    private static final int SUMMONS_PER_WAVE = 3;
    /** Hard cap on live adds, so a slow fight cannot silt the arena up. */
    private static final int MAX_LIVE_SHADOWS = 8;
    private static final double SUMMON_SEARCH_RADIUS = 24.0;

    /** How far he will chase before the prison pulls him back. */
    private static final int HOME_RADIUS = 40;

    private int rangedCooldown;
    private int summonCooldown = SUMMON_COOLDOWN_TICKS;
    private boolean announcedPhaseTwo;

    public JesseltonsShadow(EntityType<? extends JesseltonsShadow> type, Level level) {
        super(type, level, BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.PROGRESS);
        this.xpReward = 250;
    }

    public static AttributeSupplier.Builder attributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 220.0)
                .add(Attributes.MOVEMENT_SPEED, 0.28)
                .add(Attributes.ATTACK_DAMAGE, 8.0)
                .add(Attributes.FOLLOW_RANGE, 48.0)
                // Some armour, but not much: phase one is meant to be survivable in gear and
                // phase two is meant to ignore gear, so his own armour is not where the
                // difficulty lives.
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
        // super drives the boss bar off barProgress() — health, in his case.
        super.customServerAiStep();

        // Anchored to wherever the prison generated him, on his first server tick. Structures
        // place entities directly rather than through finalizeSpawn, so there is no spawn hook
        // to do this in — and without it he can be walked out of the dungeon and lost.
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
        if (summonCooldown > 0) {
            summonCooldown--;
        }

        castArts(target);

        if (isPhaseTwo() && summonCooldown <= 0) {
            summonCooldown = SUMMON_COOLDOWN_TICKS;
            summonShadows();
        }
    }

    /**
     * The arts themselves. Both phases use the same hitscan beam and differ only in the
     * damage type, which is the entire design: the attack looks the same and stops being
     * something armour can answer.
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
            ArtsBeam.fire(this, target, ModDamageTypes.VOID_ARTS, PHASE_TWO_DAMAGE, ParticleTypes.SCULK_SOUL);
        } else {
            ArtsBeam.fire(this, target, ModDamageTypes.SPECTRAL_ARTS, PHASE_ONE_DAMAGE, ParticleTypes.CRIT);
        }
        this.playSound(SoundEvents.EVOKER_CAST_SPELL, 1.0F, isPhaseTwo() ? 0.6F : 1.0F);
    }

    private void enterPhaseTwo() {
        announcedPhaseTwo = true;
        bossEvent.setColor(BossEvent.BossBarColor.RED);
        // Fires the first wave immediately rather than after another cooldown, so the phase
        // change is something that happens rather than something you notice later.
        summonCooldown = SUMMON_COOLDOWN_TICKS;
        summonShadows();
        this.playSound(SoundEvents.WITHER_SPAWN, 2.0F, 1.4F);
    }

    private void summonShadows() {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        long live = serverLevel.getEntitiesOfClass(ImprisonedShadow.class,
                this.getBoundingBox().inflate(SUMMON_SEARCH_RADIUS)).size();
        if (live >= MAX_LIVE_SHADOWS) {
            return;
        }

        for (int i = 0; i < SUMMONS_PER_WAVE && live + i < MAX_LIVE_SHADOWS; i++) {
            ImprisonedShadow shadow = ModEntities.IMPRISONED_SHADOW.get().create(serverLevel);
            if (shadow == null) {
                return;
            }
            // A ring around him rather than on top of him: adds that spawn inside the boss's
            // hitbox are free damage for anyone already swinging at it.
            double angle = this.random.nextDouble() * Math.PI * 2.0;
            double distance = 2.0 + this.random.nextDouble() * 3.0;
            shadow.moveTo(this.getX() + Math.cos(angle) * distance,
                    this.getY(),
                    this.getZ() + Math.sin(angle) * distance,
                    this.random.nextFloat() * 360.0F, 0.0F);
            shadow.finalizeSpawn(serverLevel, serverLevel.getCurrentDifficultyAt(shadow.blockPosition()),
                    MobSpawnType.MOB_SUMMONED, null, null);
            shadow.setTarget(this.getTarget());
            serverLevel.addFreshEntity(shadow);
            serverLevel.sendParticles(ParticleTypes.SOUL, shadow.getX(), shadow.getY() + 1.0, shadow.getZ(),
                    12, 0.3, 0.5, 0.3, 0.02);
        }
    }

    /**
     * The Master Key is dropped here rather than from a loot table because it is the gate
     * on the rest of the chapter: exactly one, every time, regardless of Looting, difficulty
     * or whether the kill rolled anything else.
     */
    @Override
    protected void dropCustomDeathLoot(DamageSource source, int looting, boolean recentlyHitByPlayer) {
        super.dropCustomDeathLoot(source, looting, recentlyHitByPlayer);
        this.spawnAtLocation(new ItemStack(ModItems.MANSFIELD_MASTER_KEY.get()));
    }

    // The boss bar, the despawn rules and the push rules are all BossMonster's.

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
