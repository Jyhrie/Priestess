package com.jyhrie.priestess.entity.mobs.undertides;

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
 * <h2>How it shoots</h2>
 * Two halves borrowed from opposite ends of the mod. Vanilla's {@link RangedAttackGoal}
 * handles the positioning and the timing — close to within range, stop, fire on an interval,
 * chase again if you leave — and {@code ArtsBeam} lands the hit. Nothing new was written for
 * either half, and no projectile entity exists, which is the same trade
 * {@code ArtsBeam} argues for at length: hitscan, so the counterplay is cover rather than
 * dodging.
 *
 * <p>This is why it overrides {@link GeoMonster#registerGoals} rather than extending it —
 * the shared set opens with a {@code MeleeAttackGoal}, and a mob that walks into melee has
 * no use for a spit. Same escape hatch {@code MbImprisonedSniper} takes, and the same reason.
 *
 * <h2>It borrows Originium acid</h2>
 * The spit lands as {@code priestess:originium_acid}, which is a placeholder and reads as
 * one: that damage type belongs to a dead Originium Slug, not to something from under the
 * sea, and its death message says so ("dissolved in Originium acid"). It was picked because
 * it already exists, is tagged the way a corrosive cloud should be — no impact, bypasses
 * shields — and had no user at all after the slug's burst was cut. A Sal Viento damage type
 * is what it eventually wants; naming one is a design call, so it was left alone.
 */
public class SvSpitter extends GeoMonster implements RangedAttackMob {

    private static final float SPIT_DAMAGE = 5.0F;
    /** Ticks between spits, picked at random in this band so a group does not fire in unison. */
    private static final int SPIT_INTERVAL_MIN = 40;
    private static final int SPIT_INTERVAL_MAX = 70;
    /** How far it will open up from, in blocks. Shorter than the Sniper's 16. */
    private static final float SPIT_RANGE = 12.0F;

    public SvSpitter(EntityType<? extends SvSpitter> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder attributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                // Slow. It is meant to be reachable — a fast mob that outranges you is a mob
                // with no answer.
                .add(Attributes.MOVEMENT_SPEED, 0.20)
                // Never used: it has no melee goal. Kept non-zero only so that anything which
                // later gives it one does not find a mob that cannot hurt anybody.
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

    /**
     * Called by {@link RangedAttackGoal} when its interval is up and the target is in range
     * and visible. {@code velocity} is how far into the band the shot fell; it is ignored,
     * because a hitscan beam has no flight to scale.
     */
    @Override
    public void performRangedAttack(LivingEntity target, float velocity) {
        ArtsBeam.fire(this, target, ModDamageTypes.ORIGINIUM_ACID, SPIT_DAMAGE, ParticleTypes.ITEM_SLIME);
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
