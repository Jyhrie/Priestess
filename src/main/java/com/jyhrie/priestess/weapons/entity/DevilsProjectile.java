package com.jyhrie.priestess.weapons.entity;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractHurtingProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * The shared body of Devil's Devastation's two projectiles.
 *
 * <p>In Lethality the scythe and the pitchfork are two 190-line files that differ in four
 * numbers. They are one class here for the same reason {@code GeoMonster} exists: the
 * behaviour is the interesting part and it should have exactly one copy.
 *
 * <h2>How it hits</h2>
 * Not through {@code onHitEntity} — that fires once, on one target, and these are meant to
 * cut through a crowd. Instead every tick it walks a 200° arc of thirty sample points on a
 * one-block radius around itself and damages everything near each one. The arc is oriented by
 * the projectile's own yaw and pitch, so it sweeps as a blade rather than a sphere.
 *
 * <p>{@link #alreadyHit} is what makes it a <em>pierce</em> rather than a blender: a target
 * is damaged once by a given projectile and then ignored for the rest of its life, however
 * many sample points sweep over it. Five projectiles per swing still means a target can take
 * up to five hits from one swing, which is the intended burst.
 *
 * <p>The sweep costs thirty AABB queries per tick per projectile, so a full swing is around a
 * hundred and fifty. That is survivable for something that lives under a second, and it is
 * the reason these projectiles are given a hard, short life rather than a long flight.
 *
 * <h2>How it dies</h2>
 * There is no collision and no timeout. It flies straight for ten ticks, then decays its
 * velocity by 20% a tick; crossing one speed threshold sprays the burst of particles and
 * crossing a lower one discards it. Terrain is not consulted at all — these pass through
 * walls, which is deliberate.
 *
 * <p>Note this never calls {@code super.tick()}. {@link AbstractHurtingProjectile} is
 * inherited for its spawn packet and owner tracking, not its movement; position is stepped by
 * hand below.
 */
public abstract class DevilsProjectile extends AbstractHurtingProjectile implements GeoEntity {

    /** Sample points along the sweep. Thirty reads as a continuous edge at a one-block radius. */
    private static final int SWEEP_POINTS = 30;

    /** Degrees of arc the sweep covers. Wider than a semicircle, so it wraps slightly behind. */
    private static final double SWEEP_ARC_DEGREES = 200.0;

    private static final double SWEEP_RADIUS = 1.0;

    /** Half-extent of the damage box at each sample point. */
    private static final double SWEEP_BOX_INFLATE = 0.5;

    /** Ticks of straight flight before it starts to decay. */
    private static final int TICKS_BEFORE_DECAY = 10;

    /** Velocity retained per tick once decaying. */
    private static final double DECAY_FACTOR = 0.8;

    /** Speed² below which the burst of particles fires. */
    private static final double DETONATE_SPEED_SQR = 0.3;

    /** Speed² below which the entity is removed. Must be under {@link #DETONATE_SPEED_SQR}. */
    private static final double DISCARD_SPEED_SQR = 0.18;

    /**
     * The trail and burst particle.
     *
     * <p>Substituted. Lethality uses its own {@code forbidden_glint}, which is a registered
     * particle type with a client factory and a texture sheet behind it — three more files and
     * a registry for something that is set dressing. Soul fire flame is the closest vanilla
     * read: dark, cold-burning, and it does not look like a torch.
     */
    private static final ParticleOptions TRAIL_PARTICLE = ParticleTypes.SOUL_FIRE_FLAME;

    /** Particles in the death burst, distributed over a sphere. */
    private static final int DETONATE_POINTS = 50;

    private static final float DETONATE_SPEED = 0.35F;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    /** Targets this projectile has already damaged. See the pierce note in the class javadoc. */
    private final Set<UUID> alreadyHit = new HashSet<>();

    private float damage;
    private int age;
    private boolean decaying;

    protected DevilsProjectile(EntityType<? extends DevilsProjectile> type, Level level) {
        super(type, level);
    }

    protected DevilsProjectile(EntityType<? extends DevilsProjectile> type, Level level,
                               double x, double y, double z, float damage) {
        this(type, level);
        this.setPos(x, y, z);
        this.damage = damage;
    }

    /** What lands on a target this projectile sweeps over, beyond the damage itself. */
    protected abstract void applyOnHit(LivingEntity target);

    @Override
    public void tick() {
        Level level = this.level();

        if (!level.isClientSide()) {
            sweepForTargets(level);
        } else {
            level.addParticle(TRAIL_PARTICLE,
                    this.getX(), this.getY() + 0.9, this.getZ(),
                    (this.random.nextDouble() - 0.5) * 0.1,
                    (this.random.nextDouble() - 0.5) * 0.1,
                    (this.random.nextDouble() - 0.5) * 0.1);
        }

        Vec3 motion = this.getDeltaMovement();
        this.setPos(this.getX() + motion.x, this.getY() + motion.y, this.getZ() + motion.z);

        age++;
        if (!decaying && age >= TICKS_BEFORE_DECAY) {
            decaying = true;
        }

        if (decaying) {
            Vec3 slowed = this.getDeltaMovement().scale(DECAY_FACTOR);
            this.setDeltaMovement(slowed);

            double speedSqr = slowed.lengthSqr();
            // Client-side only: addParticle does nothing on a server, and both sides run this
            // same deterministic decay, so the client reaches the threshold on its own.
            if (speedSqr < DETONATE_SPEED_SQR && level.isClientSide()) {
                detonate(level, this.getX() + 0.2, this.getY() + 0.9, this.getZ());
            }
            if (speedSqr < DISCARD_SPEED_SQR) {
                this.discard();
            }
        }
    }

    /**
     * Damages everything along the swept arc that this projectile has not hit yet.
     *
     * <p>The three unit-vector components below rotate a circle in the projectile's local
     * plane out into world space by its yaw and pitch. The {@code +90} offsets and the sign
     * flips are what stand the circle up edge-on to the direction of travel — read as a
     * rotation matrix rather than as trigonometry with meaning of its own.
     */
    private void sweepForTargets(Level level) {
        double yaw = Math.toRadians(this.getYRot() + 90.0);
        double pitch = Math.toRadians(-1.0 * (this.getXRot() + 90.0));

        for (int i = 0; i < SWEEP_POINTS; i++) {
            double angle = Math.toRadians(i * (SWEEP_ARC_DEGREES / SWEEP_POINTS));
            double sinAngle = Math.sin(angle);
            double cosAngle = Math.cos(angle);

            double vx = -(sinAngle * Math.sin(pitch) * Math.cos(yaw) + cosAngle * Math.sin(yaw));
            double vy = sinAngle * Math.cos(pitch);
            double vz = -(sinAngle * Math.sin(pitch) * Math.sin(yaw)) + cosAngle * Math.cos(yaw);

            double x = this.getX() + SWEEP_RADIUS * vx;
            double y = this.getY() + SWEEP_RADIUS * vy;
            double z = this.getZ() + SWEEP_RADIUS * vz;

            AABB box = new AABB(x, y + 1.6, z, x, y + 1.6, z).inflate(SWEEP_BOX_INFLATE);
            List<LivingEntity> found =
                    level.getEntitiesOfClass(LivingEntity.class, box, e -> e != this.getOwner());

            for (LivingEntity target : found) {
                if (alreadyHit.add(target.getUUID())) {
                    target.hurt(level.damageSources().indirectMagic(this, this.getOwner()), damage);
                    applyOnHit(target);
                }
            }
        }
    }

    /** A roughly even sphere of particles, spaced by the golden angle. */
    private void detonate(Level level, double x, double y, double z) {
        double phi = Math.PI * (3.0 - Math.sqrt(5.0));
        for (int i = 0; i < DETONATE_POINTS; i++) {
            double vy = 1 - ((double) i / (DETONATE_POINTS - 1)) * 2;
            double radius = Math.sqrt(1 - vy * vy);
            double theta = phi * i;
            level.addParticle(TRAIL_PARTICLE, x, y, z,
                    Math.cos(theta) * radius * DETONATE_SPEED,
                    vy * DETONATE_SPEED,
                    Math.sin(theta) * radius * DETONATE_SPEED);
        }
    }

    // ── Plumbing ──────────────────────────────────────────────────────────────

    @Override
    protected void defineSynchedData() {
        // Nothing is synced. Damage is server-only and position comes from entity tracking.
    }

    /** Always rendered: it is small, short-lived, and popping out of view mid-flight reads as a bug. */
    @Override
    public boolean shouldRender(double camX, double camY, double camZ) {
        return true;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean canBeCollidedWith() {
        return false;
    }

    /** {@link AbstractHurtingProjectile} draws itself burning by default; these are not on fire. */
    @Override
    public boolean isOnFire() {
        return false;
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    // GeckoLib wants a controller registered even when there is no animation to play. These
    // models are static geometry, so the controller exists and does nothing.
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 0, state -> PlayState.CONTINUE));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
