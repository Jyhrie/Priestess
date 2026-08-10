package com.jyhrie.priestess.entity.mobs.mansfieldbreak;

import com.jyhrie.priestess.entity.GeoMonster;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RangedBowAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

/**
 * Imprisoned Sniper — the inmate who got to the armoury first. A skeleton in behaviour.
 *
 * <p>The only mob in the mod that shoots something you can dodge. Everything else ranged goes
 * through {@code ArtsBeam}, which is hitscan and lands the instant it fires; this one throws a
 * real vanilla {@link Arrow}, with travel time, an arc, and inaccuracy that tightens with
 * difficulty. That is the whole reason it exists — Mansfield needs one threat that punishes
 * standing in the open rather than one that punishes being in line of sight.
 *
 * <h2>Why it does not extend the shared goal set</h2>
 * {@link GeoMonster#registerGoals} gives every mob a {@code MeleeAttackGoal}, and a sniper
 * that walks into melee is not a sniper. This overrides the method outright, which is the
 * escape hatch {@code GeoMonster} documents for exactly this case — no flag on the base class,
 * no {@code isRanged()} branch.
 *
 * <p>{@link RangedBowAttackGoal} is what makes it read as a skeleton rather than as a turret:
 * it strafes, it backs off when you close, and it holds fire while it has no line of sight.
 * That goal is also why the mob carries a real bow — it refuses to run otherwise, checking
 * {@code isHolding(BowItem)} on every tick.
 *
 * <h2>The bow is invisible, and that is a known gap</h2>
 * GeckoLib does not draw held items unless the renderer asks it to, and
 * {@code PriestessGeoRenderer} does not. So the mob is holding a bow that the player cannot
 * see. Harmless while the model is a placeholder cube humanoid — there is nowhere sensible to
 * put a bow on it anyway — but when the real model arrives it wants a bone for the weapon and
 * a {@code GeoItemLayer} on the renderer to fill it.
 */
public class MbImprisonedSniper extends GeoMonster implements RangedAttackMob {

    /**
     * Damage before the velocity multiplier, which {@code shoot} applies on top. A vanilla
     * arrow is 2.0; this is a little meaner because there are meant to be few of these and
     * they are meant to be the reason you take cover.
     */
    private static final double ARROW_DAMAGE = 3.0;

    /** Ticks between shots. Vanilla's skeleton uses 20 on Hard and 40 otherwise; this is flat. */
    private static final int SHOOT_INTERVAL_TICKS = 30;

    /** How far it will open fire from, in blocks. */
    private static final float SHOOT_RANGE = 16.0F;

    public MbImprisonedSniper(EntityType<? extends MbImprisonedSniper> type, Level level) {
        super(type, level);

        // Equipped here rather than in finalizeSpawn on purpose. Structures place entities
        // directly and never call finalizeSpawn — the same trap MbJesseltonWilliams documents
        // for his home position — so a sniper generated inside Mansfield would come out
        // unarmed and, because RangedBowAttackGoal needs the bow, entirely inert.
        this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
        // Otherwise every one of these is a free bow.
        this.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
    }

    public static AttributeSupplier.Builder attributes() {
        return Monster.createMonsterAttributes()
                // A skeleton's 20. It is not meant to survive being reached.
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.MOVEMENT_SPEED, 0.25)
                // Only used if something ever gives it a melee goal — arrows carry their own
                // damage, from ARROW_DAMAGE above, and never read this.
                .add(Attributes.ATTACK_DAMAGE, 2.0)
                // Further than it will shoot from, so it notices you before it engages.
                .add(Attributes.FOLLOW_RANGE, 32.0)
                .add(Attributes.ARMOR, 0.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.0);
    }

    /**
     * Replaces {@link GeoMonster}'s melee set entirely. Same shape otherwise — float, engage,
     * wander, look about — with the bow goal where the melee goal was.
     */
    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new RangedBowAttackGoal<>(this, 1.0, SHOOT_INTERVAL_TICKS, SHOOT_RANGE));
        this.goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 0.8));
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 16.0F));
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    /**
     * Vanilla's skeleton shot, near enough verbatim.
     *
     * <p>The {@code + horizontal * 0.2} is the arc: it aims above the target by a fifth of the
     * horizontal distance so the arrow drops onto them rather than falling short. The last
     * argument to {@code shoot} is inaccuracy in degrees — 10 on Easy, 6 on Normal, 2 on Hard,
     * which is how a skeleton gets deadlier with difficulty without dealing more damage.
     */
    @Override
    public void performRangedAttack(LivingEntity target, float velocity) {
        Arrow arrow = new Arrow(this.level(), this);
        arrow.setBaseDamage(ARROW_DAMAGE);

        double dx = target.getX() - this.getX();
        double dy = target.getY(0.3333333333333333) - arrow.getY();
        double dz = target.getZ() - this.getZ();
        double horizontal = Math.sqrt(dx * dx + dz * dz);

        arrow.shoot(dx, dy + horizontal * 0.2, dz, 1.6F,
                14 - this.level().getDifficulty().getId() * 4);

        this.playSound(SoundEvents.SKELETON_SHOOT, 1.0F,
                1.0F / (this.getRandom().nextFloat() * 0.4F + 0.8F));
        this.level().addFreshEntity(arrow);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.SKELETON_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.SKELETON_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.SKELETON_DEATH;
    }
}
