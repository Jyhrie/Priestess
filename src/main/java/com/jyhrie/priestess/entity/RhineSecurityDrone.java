package com.jyhrie.priestess.entity;

import com.jyhrie.priestess.damage.ModDamageTypes;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomFlyingGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.EnumSet;

/**
 * Rhine Security Drone — the tower's automated perimeter, still running on a charter nobody
 * is left to revoke.
 *
 * <p>Airborne, fragile, and it never lands. What it does is strip armour: the beam deals
 * very little damage and a great deal of durability, so a player who ignores the things
 * buzzing overhead arrives at the next {@link RoguePowerArmour} in gear that is about to
 * break. That is the whole pairing — one mob demands armour, the other removes it, and
 * neither is dangerous on its own.
 *
 * <h2>Flight</h2>
 * It is a {@link Monster} with a flying move control and navigation rather than a
 * {@code FlyingMob}, which is how vanilla's Bee does it. {@code FlyingMob} is built for
 * things that drift like a Ghast; a drone needs to path around a ruined skyscraper, and
 * pathing is what {@code PathfinderMob} brings. Gravity is off, so the move control's
 * vertical component is the only thing deciding its height.
 */
public class RhineSecurityDrone extends Monster {

    private static final int LASER_COOLDOWN_TICKS = 40;
    private static final double LASER_RANGE = 20.0;
    /** Low. The drone is a durability tax, not a damage source. */
    private static final float LASER_DAMAGE = 2.0F;
    /** Armour durability burned per beam, spread over every worn piece. */
    private static final float ARMOUR_DRAIN = 12.0F;

    /** How far out it tries to sit from its target. Inside its range, outside melee. */
    private static final double PREFERRED_RANGE = 8.0;

    private int laserCooldown;

    public RhineSecurityDrone(EntityType<? extends RhineSecurityDrone> type, Level level) {
        super(type, level);
        this.moveControl = new FlyingMoveControl(this, 20, true);
        // Nothing holds it up but the move control, which is what lets it hover motionless
        // instead of sagging between navigation ticks.
        this.setNoGravity(true);
    }

    public static AttributeSupplier.Builder attributes() {
        return Monster.createMonsterAttributes()
                // Frail: two hits from anything. They are meant to be swatted, and to have
                // already cost you something by the time you get round to it.
                .add(Attributes.MAX_HEALTH, 16.0)
                .add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.FLYING_SPEED, 0.5)
                .add(Attributes.ATTACK_DAMAGE, 2.0)
                .add(Attributes.FOLLOW_RANGE, 32.0)
                .add(Attributes.ARMOR, 4.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.0);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        FlyingPathNavigation navigation = new FlyingPathNavigation(this, level);
        navigation.setCanOpenDoors(false);
        navigation.setCanFloat(false);
        navigation.setCanPassDoors(true);
        return navigation;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new HoldRangeGoal());
        this.goalSelector.addGoal(2, new WaterAvoidingRandomFlyingGoal(this, 1.0));
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 16.0F));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (laserCooldown > 0) {
            laserCooldown--;
        }

        LivingEntity target = this.getTarget();
        if (target == null || !target.isAlive() || laserCooldown > 0) {
            return;
        }
        if (this.distanceToSqr(target) > LASER_RANGE * LASER_RANGE || !this.hasLineOfSight(target)) {
            return;
        }
        laserCooldown = LASER_COOLDOWN_TICKS;
        fireLaser(target);
    }

    /**
     * The damage is almost incidental; the durability is the attack.
     *
     * <p>The armour is burned whether or not the damage landed, because the two have
     * different reasons to fail — a player still in hurt-immunity from the last drone took
     * no health, but the beam still hit their plate, and a drone swarm that could be walled
     * off by invulnerability frames would not be a swarm.
     */
    private void fireLaser(LivingEntity target) {
        ArtsBeam.fire(this, target, ModDamageTypes.RHINE_LASER, LASER_DAMAGE, ParticleTypes.END_ROD);

        if (target instanceof Player player) {
            player.getInventory().hurtArmor(
                    ModDamageTypes.source(this.level(), ModDamageTypes.RHINE_LASER, this),
                    ARMOUR_DRAIN, Inventory.ALL_ARMOR_SLOTS);
        }
        this.playSound(SoundEvents.BEACON_POWER_SELECT, 1.0F, 1.8F);
    }

    // ── It does not land, and it does not fall ────────────────────────────────

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    protected void checkFallDamage(double y, boolean onGround, net.minecraft.world.level.block.state.BlockState state,
                                   net.minecraft.core.BlockPos pos) {
    }

    @Override
    public boolean isFlapping() {
        return true;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.BEACON_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ITEM_BREAK;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.GENERIC_EXPLODE;
    }

    /**
     * Fly to a point roughly {@link #PREFERRED_RANGE} from the target and stay there.
     *
     * <p>Deliberately not a chase: a drone that closed to melee would be trivially punched
     * out of the air, and one that fled to maximum range would be unkillable without a bow.
     * Holding a fixed ring keeps it inside the player's reach if they commit to it and out
     * of it if they are busy with something on the ground, which is the trade the mob is
     * there to offer.
     */
    private class HoldRangeGoal extends Goal {

        /** Re-plan on a timer; a new path every tick makes it jitter and costs a lot. */
        private static final int REPLAN_INTERVAL = 20;

        private int replanIn;

        HoldRangeGoal() {
            this.setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = RhineSecurityDrone.this.getTarget();
            return target != null && target.isAlive();
        }

        @Override
        public void start() {
            replanIn = 0;
        }

        @Override
        public void stop() {
            RhineSecurityDrone.this.getNavigation().stop();
        }

        @Override
        public void tick() {
            LivingEntity target = RhineSecurityDrone.this.getTarget();
            if (target == null || replanIn-- > 0) {
                return;
            }
            replanIn = REPLAN_INTERVAL;

            // A point on the ring nearest to where the drone already is, lifted a little, so
            // it settles above the player rather than in their face.
            double dx = RhineSecurityDrone.this.getX() - target.getX();
            double dz = RhineSecurityDrone.this.getZ() - target.getZ();
            double flat = Math.sqrt(dx * dx + dz * dz);
            if (flat < 1.0E-4) {
                // Directly overhead: pick any direction rather than dividing by nothing.
                dx = 1.0;
                dz = 0.0;
                flat = 1.0;
            }
            double scale = PREFERRED_RANGE / flat;
            RhineSecurityDrone.this.getNavigation().moveTo(
                    target.getX() + dx * scale,
                    target.getY() + 3.0,
                    target.getZ() + dz * scale,
                    1.0);
        }
    }
}
