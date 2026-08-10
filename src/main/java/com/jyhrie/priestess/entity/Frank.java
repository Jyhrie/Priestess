package com.jyhrie.priestess.entity;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * "The Franks" — the biomechanical remains of Dorothy's pioneer test subjects.
 *
 * <p>Frail and quick. They are not meant to kill you; they are meant to make the fight
 * unreadable. Every hit lands Mining Fatigue and Nausea, which is the dead hivemind still
 * trying to talk to a nervous system it no longer owns — you keep your health bar and lose
 * your ability to use the room.
 *
 * <p>The debuffs are short and refreshed rather than long and stacked. A single Frank
 * getting one hit in should be an inconvenience that clears before you reach the next
 * chamber; six of them at once should be the point at which you stop being able to fight
 * back, and that comes out of the refresh rate, not out of a big duration.
 */
public class Frank extends Monster {

    private static final int SENSORY_OVERLOAD_TICKS = 120;
    /** Mining Fatigue II — enough to matter for digging out, not enough to be unrecoverable. */
    private static final int MINING_FATIGUE_AMPLIFIER = 1;

    public Frank(EntityType<? extends Frank> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder attributes() {
        return Monster.createMonsterAttributes()
                // Frail on purpose: two hits from anything the player brought to Columbia.
                .add(Attributes.MAX_HEALTH, 12.0)
                // Faster than a sprinting player. You do not outrun a Frank, you kill it.
                .add(Attributes.MOVEMENT_SPEED, 0.36)
                .add(Attributes.ATTACK_DAMAGE, 3.0)
                .add(Attributes.FOLLOW_RANGE, 32.0)
                // Almost none: they should fly off you when hit, so a crowd can be broken up.
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.2, false));
        this.goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 0.9));
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 12.0F));
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    /**
     * The debuffs ride on a landed hit, not on the swing — {@code doHurtTarget} returns false
     * when the target blocked or was still invulnerable, and a blocked hit should not overload
     * anybody's senses.
     */
    @Override
    public boolean doHurtTarget(Entity target) {
        if (!super.doHurtTarget(target)) {
            return false;
        }
        if (target instanceof LivingEntity living) {
            living.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN,
                    SENSORY_OVERLOAD_TICKS, MINING_FATIGUE_AMPLIFIER, false, true, true), this);
            living.addEffect(new MobEffectInstance(MobEffects.CONFUSION,
                    SENSORY_OVERLOAD_TICKS, 0, false, true, true), this);
        }
        return true;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.ZOMBIE_VILLAGER_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ZOMBIE_VILLAGER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ZOMBIE_VILLAGER_DEATH;
    }
}
