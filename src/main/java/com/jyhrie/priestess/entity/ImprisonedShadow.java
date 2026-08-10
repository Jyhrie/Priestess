package com.jyhrie.priestess.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
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
import net.minecraft.world.level.Level;

/**
 * The inmates who died locked in their cells, summoned back out of them by
 * {@link JesseltonsShadow} once he drops into his second phase.
 *
 * <p>They exist to take the player's attention off the boss, so they are built to be
 * annoying rather than dangerous: fast, fragile, and there are always more. What keeps
 * that from turning into an unwinnable room is {@link #LIFETIME_TICKS} — every shadow
 * fades on its own after a minute whether or not you killed it, so a long fight cannot
 * silt up with adds and a player who kites well is rewarded for it.
 *
 * <p>They are not gated on the boss being alive. Killing Jesselton mid-summon should leave
 * you with the ones already out to deal with; the arena going instantly quiet would rob
 * the kill of its ending.
 */
public class ImprisonedShadow extends Monster {

    /** One minute. Long enough to be a problem, short enough to never be a wall. */
    private static final int LIFETIME_TICKS = 1200;

    private static final String NBT_AGE = "shadow_age";

    private int shadowAge;

    public ImprisonedShadow(EntityType<? extends ImprisonedShadow> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder attributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 14.0)
                .add(Attributes.MOVEMENT_SPEED, 0.34)
                .add(Attributes.ATTACK_DAMAGE, 4.0)
                .add(Attributes.FOLLOW_RANGE, 32.0)
                // Spectral, like the thing that summoned them: they do not get shoved.
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.1, true));
        this.goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 0.8));
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 16.0F));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide() && ++shadowAge > LIFETIME_TICKS) {
            // discard, not kill: a shadow that faded out was never killed by anybody, so it
            // should not drop loot, credit a kill, or play a death animation.
            this.discard();
        }
    }

    /**
     * The age is saved rather than reset on load, or a player could park a summoned add in
     * an unloaded chunk and come back to find it immortal.
     */
    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt(NBT_AGE, shadowAge);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        shadowAge = tag.getInt(NBT_AGE);
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
        return SoundEvents.VEX_DEATH;
    }
}
