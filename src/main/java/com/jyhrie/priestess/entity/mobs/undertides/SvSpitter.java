package com.jyhrie.priestess.entity.mobs.undertides;

import com.jyhrie.priestess.config.MobStats;
import com.jyhrie.priestess.damage.ModDamageTypes;
import com.jyhrie.priestess.entity.GeoMonster;
import com.jyhrie.priestess.entity.projectiles.ArtsBeam;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * Spitter — mostly maw, and the only thing in Sal Viento that hurts you from across a room.
 *
 * <p>Vanilla's {@link RangedAttackGoal} handles positioning and timing, and {@code ArtsBeam}
 * lands the hit — hitscan, so the counterplay is cover rather than dodging. It overrides
 * {@link GeoMonster#registerGoals} rather than extending it, because the shared set opens with
 * a {@code MeleeAttackGoal}.
 *
 * <p><b>The damage type is a placeholder.</b> The spit lands as
 * {@code priestess:originium_acid}, whose death message says "dissolved in Originium acid" —
 * wrong for something from under the sea. It was picked because it already exists, is tagged
 * the way a corrosive cloud should be, and had no other user. A Sal Viento damage type is what
 * it wants.
 */
public class SvSpitter extends GeoMonster implements RangedAttackMob {

    /** Ticks between spits, picked at random in this band so a group does not fire in unison. */
    private static final int SPIT_INTERVAL_MIN = 40;
    private static final int SPIT_INTERVAL_MAX = 70;
    /** How far it will open up from, in blocks. Shorter than the Sniper's 16. */
    private static final float SPIT_RANGE = 12.0F;

    public SvSpitter(EntityType<? extends SvSpitter> type, Level level) {
        super(type, level);
    }

    /** Defaults only; {@code EntityStats} overwrites all six from {@code MobStats} on join. */
    public static AttributeSupplier.Builder attributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                // Slow, so it stays reachable: a fast mob that outranges you has no answer.
                .add(Attributes.MOVEMENT_SPEED, 0.20)
                // Unused — it has no melee goal — but kept non-zero in case something adds one.
                .add(Attributes.ATTACK_DAMAGE, 2.0)
                .add(Attributes.FOLLOW_RANGE, 28.0)
                .add(Attributes.ARMOR, 2.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.0);
    }

    /** {@link GeoMonster}'s set with the melee goal swapped for a ranged one. */
    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new RangedAttackGoal(this, 1.0,
                SPIT_INTERVAL_MIN, SPIT_INTERVAL_MAX, SPIT_RANGE));
        this.goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 0.8));
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 14.0F));
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    /** {@code velocity} is ignored, because a hitscan beam has no flight to scale. */
    @Override
    public void performRangedAttack(LivingEntity target, float velocity) {
        ArtsBeam.fire(this, target, ModDamageTypes.ORIGINIUM_ACID,
                MobStats.SPITTER_SPIT_DAMAGE.get().floatValue(), ParticleTypes.ITEM_SLIME);
        this.playSound(SoundEvents.LLAMA_SPIT, 1.0F,
                0.8F + this.getRandom().nextFloat() * 0.2F);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.GUARDIAN_AMBIENT_LAND;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.GUARDIAN_HURT_LAND;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.GUARDIAN_DEATH_LAND;
    }
}
