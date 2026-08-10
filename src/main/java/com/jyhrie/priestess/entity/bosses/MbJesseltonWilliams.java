package com.jyhrie.priestess.entity.bosses;

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
 * <p>The distinction matters for how the fight reads. Nothing here died in Mansfield: the
 * projector reaches sideways rather than backwards, and what it pulls through is a living
 * man who won, wearing the confidence of a version of events the player never got. The
 * dog tags that wake the effigy are this world's Jesselton, and they are the anchor it
 * aims with.
 *
 * <h2>Two phases, and why they are different in kind</h2>
 * The prison hands out riot gear on the way in, so the fight has to answer the question
 * "was that armour worth carrying". Phase one says yes: iron arts thrown across the cell
 * block, heavy but ordinary kinetic damage, and every point of armour you looted subtracts
 * from it.
 *
 * <p>At half health he stops caring. Phase two is {@code priestess:void_arts}, which sits in
 * the {@code minecraft:bypasses_armor} tag, so the armour that carried the first half of the
 * fight does nothing for the second. He used to pull the dead inmates out of their cells to
 * swarm you as well; that went with the Imprisoned Shadow, and phase two is currently the
 * damage-type change on its own.
 *
 * <p>He is knockback-immune by attribute rather than by a special case in {@code hurt} —
 * what stands in the room is a projection with no mass to shove, and the difference the
 * player notices is that a shield bash moves everything in the cell block except him.
 */
public class MbJesseltonWilliams extends BossMonster {

    /** Below this fraction of max health he is in phase two, and never goes back. */
    private static final float PHASE_TWO_AT = 0.5F;

    private static final int RANGED_COOLDOWN_TICKS = 45;
    private static final double RANGED_RANGE = 24.0;
    private static final float PHASE_ONE_DAMAGE = 9.0F;
    private static final float PHASE_TWO_DAMAGE = 7.0F;

    /** How far he will chase before the prison pulls him back. */
    private static final int HOME_RADIUS = 40;

    private int rangedCooldown;
    private boolean announcedPhaseTwo;

    public MbJesseltonWilliams(EntityType<? extends MbJesseltonWilliams> type, Level level) {
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

        castArts(target);
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

    /**
     * The turn, which is now a damage-type change and a sound and nothing else.
     *
     * <p>It used to also pull the dead inmates out of their cells; that half went with the
     * Imprisoned Shadow. What is left still works as a phase — the beam stops being
     * something armour answers — but it is thinner than it was, and the swarm is the obvious
     * place to put whatever replaces it.
     */
    private void enterPhaseTwo() {
        announcedPhaseTwo = true;
        bossEvent.setColor(BossEvent.BossBarColor.RED);
        this.playSound(SoundEvents.WITHER_SPAWN, 2.0F, 1.4F);
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
