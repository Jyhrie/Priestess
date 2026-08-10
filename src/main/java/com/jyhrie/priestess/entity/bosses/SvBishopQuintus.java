package com.jyhrie.priestess.entity.bosses;

import com.jyhrie.priestess.damage.ModDamageTypes;
import com.jyhrie.priestess.entity.BossMonster;
import com.jyhrie.priestess.entity.projectiles.ArtsBeam;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * Sal Viento Bishop Quintus — the thing the town is built around now. It does not move.
 *
 * <h2>Fully immobile, and what that costs</h2>
 * Zero movement speed, no navigation goals, no melee goal, knockback immunity by attribute,
 * and a no-op {@code push} — nothing in the world relocates it, including a piston, a
 * current, or the blast trying to kill it. Unlike {@code DvAwaken} it keeps gravity: it is
 * built like architecture rather than hung in the air, so it should sit on the seabed and
 * not float above a hole in it.
 *
 * <p>An immobile boss has one hard requirement: <b>it must be able to reach you</b>. A boss
 * that cannot move and cannot shoot is scenery you are allowed to hit, which is the gap
 * {@code DvAwaken} is currently sitting in and openly documents. So unlike Awaken this ships
 * with its attack — a hitscan {@code ArtsBeam} on a cooldown, the same one both other bosses
 * use. Cover is the counterplay, which is what its arena has to be built to provide.
 *
 * <h2>Why it turns</h2>
 * {@link #facePlayerSlowly()} is lifted from {@code DvAwaken} and for the same reason:
 * vanilla's rotation does not ease for a mob that never moves. {@code BodyRotationControl}
 * only runs its body-follows-head step while {@code isMoving()}, and this never is, so the
 * stationary path waits for a 15-degree drift and then closes the whole gap in one tick —
 * which on something two and a half blocks across reads as a snap. Owning the rotation is
 * the only way to get a constant rate.
 *
 * <p>The turn is also the fight's clock. It is slow on purpose: getting behind Quintus is
 * the only thing resembling a dodge against a beam that has already landed by the time it
 * is drawn, so how fast it comes round <em>is</em> the difficulty dial.
 */
public class SvBishopQuintus extends BossMonster implements GeoEntity {

    /**
     * Degrees of yaw per tick, so 20x this is degrees per second. At 1.8 a half turn takes
     * five seconds — slower than {@code DvAwaken}, because unlike Awaken this one is
     * shooting at whatever it faces and the window behind it has to be worth running for.
     */
    private static final float TURN_DEGREES_PER_TICK = 1.8F;

    private static final int BEAM_COOLDOWN_TICKS = 50;
    private static final double BEAM_RANGE = 28.0;
    private static final float BEAM_DAMAGE = 8.0F;

    /** How wide a cone in front of it counts as "facing you". Beyond this it holds fire. */
    private static final float FIRING_ARC_DEGREES = 40.0F;

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

    private int beamCooldown;

    public SvBishopQuintus(EntityType<? extends SvBishopQuintus> type, Level level) {
        super(type, level, BossEvent.BossBarColor.BLUE, BossEvent.BossBarOverlay.PROGRESS);
        this.xpReward = 400;
    }

    public static AttributeSupplier.Builder attributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 400.0)
                // Zero, and nothing to use it if it were not. It stands where it is put.
                .add(Attributes.MOVEMENT_SPEED, 0.0)
                // Zero: it has no melee goal, and a boss that chips you for standing inside
                // it is a boss with an attack nobody designed. The beam carries the fight.
                .add(Attributes.ATTACK_DAMAGE, 0.0)
                .add(Attributes.FOLLOW_RANGE, 48.0)
                .add(Attributes.ARMOR, 10.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
    }

    /** Target selectors only. Nothing moves it and nothing swings. */
    @Override
    protected void registerGoals() {
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();

        LivingEntity target = this.getTarget();
        if (target == null || !target.isAlive()) {
            return;
        }

        facePlayerSlowly(target);

        if (beamCooldown > 0) {
            beamCooldown--;
        }
        fireBeam(target);
    }

    /**
     * Self-guarding, like Jesselton's {@code castArts}: it returns unless the cooldown is up,
     * the target is in range, there is line of sight, and the target is inside the firing
     * arc. The arc is the part that makes the slow turn matter — step round the side and it
     * has to come about before it can shoot you again.
     */
    private void fireBeam(LivingEntity target) {
        if (beamCooldown > 0 || this.distanceToSqr(target) > BEAM_RANGE * BEAM_RANGE) {
            return;
        }
        if (!this.hasLineOfSight(target) || !isWithinFiringArc(target)) {
            return;
        }
        beamCooldown = BEAM_COOLDOWN_TICKS;

        ArtsBeam.fire(this, target, ModDamageTypes.ORIGINIUM_ACID, BEAM_DAMAGE, ParticleTypes.BUBBLE_POP);
        this.playSound(SoundEvents.CONDUIT_ATTACK_TARGET, 2.0F, 0.7F);
    }

    private boolean isWithinFiringArc(LivingEntity target) {
        double dx = target.getX() - this.getX();
        double dz = target.getZ() - this.getZ();
        float wanted = (float) (Mth.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0F;
        return Math.abs(Mth.degreesDifference(this.getYRot(), wanted)) <= FIRING_ARC_DEGREES;
    }

    /**
     * Turns to face its target at a fixed rate. Head, body and entity yaw are all set to the
     * same value, which is also what stops {@code BodyRotationControl} interfering — it still
     * runs, but with no gap between head and body there is nothing for it to close.
     */
    private void facePlayerSlowly(LivingEntity target) {
        double dx = target.getX() - this.getX();
        double dz = target.getZ() - this.getZ();
        float wanted = (float) (Mth.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0F;
        float yaw = Mth.approachDegrees(this.getYRot(), wanted, TURN_DEGREES_PER_TICK);

        this.setYRot(yaw);
        this.setYBodyRot(yaw);
        this.setYHeadRot(yaw);
    }

    /** Nothing moves it: not a piston, not a current, not the blast trying to kill it. */
    @Override
    public void push(double x, double y, double z) {
    }

    /** No controllers: the model has no animations yet. */
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.CONDUIT_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ELDER_GUARDIAN_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ELDER_GUARDIAN_DEATH;
    }
}
