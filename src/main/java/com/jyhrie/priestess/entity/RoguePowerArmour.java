package com.jyhrie.priestess.entity;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
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
 * Rogue Columbian Power Armour — a suit still walking its patrol three hundred years after
 * the man inside it stopped needing it. The bruiser of Rhine Lab HQ.
 *
 * <h2>Physical resistance, and why it is not just armour</h2>
 * The armour attribute alone would be answered by an enchanted weapon and a large enough
 * number. Instead, kinetic damage is cut by {@link #PHYSICAL_RESISTANCE} <em>on top of</em>
 * armour, while anything that bypasses armour — Arts, magic, the void damage the rest of
 * the chapter taught you about — comes through at full strength.
 *
 * <p>That inversion is the point of putting them in the last dungeon. The Rhine tower is
 * where the two lessons of the chapter get used against each other: these want to be hit
 * with something that ignores armour, and the {@link RhineSecurityDrone}s overhead are busy
 * removing yours.
 */
public class RoguePowerArmour extends Monster {

    /** Fraction of ordinary kinetic damage the plating turns away, before armour. */
    private static final float PHYSICAL_RESISTANCE = 0.5F;

    public RoguePowerArmour(EntityType<? extends RoguePowerArmour> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder attributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 120.0)
                // Slow. It never catches a player who keeps moving, and it is not supposed
                // to — it is a thing you have to get past, not a thing that hunts you.
                .add(Attributes.MOVEMENT_SPEED, 0.19)
                .add(Attributes.ATTACK_DAMAGE, 12.0)
                .add(Attributes.ATTACK_KNOCKBACK, 2.0)
                .add(Attributes.FOLLOW_RANGE, 32.0)
                .add(Attributes.ARMOR, 18.0)
                .add(Attributes.ARMOR_TOUGHNESS, 8.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.9);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.0, false));
        this.goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 0.6));
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 16.0F));
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    /**
     * Kinetic damage is halved; anything in {@code bypasses_armor} is not touched. Applied
     * here rather than through a damage-reduction attribute because there is no vanilla
     * attribute that distinguishes the two, and the distinction is the entire mob.
     */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!source.is(DamageTypeTags.BYPASSES_ARMOR) && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            amount *= 1.0F - PHYSICAL_RESISTANCE;
        }
        return super.hurt(source, amount);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.IRON_GOLEM_STEP;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.IRON_GOLEM_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.IRON_GOLEM_DEATH;
    }

    @Override
    protected void playStepSound(net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
        this.playSound(SoundEvents.IRON_GOLEM_STEP, 1.0F, 0.8F);
    }
}
