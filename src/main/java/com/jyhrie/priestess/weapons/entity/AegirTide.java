package com.jyhrie.priestess.weapons.entity;

import com.jyhrie.priestess.weapons.ModWeapons;
import com.jyhrie.priestess.weapons.WeaponPhysics;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractHurtingProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

/**
 * Aegir Greatspear's left-click throw: a lance of white water that hauls back whatever it hits.
 *
 * <p>It has <b>no model</b>, and that is the design rather than a gap. What the player sees is
 * the trail — a line of white drawn along the path it took this tick — so the ability reads as a
 * jet of water rather than as an object flying through the air, and there is no mesh whose
 * facing has to be kept honest against the direction of travel. Its renderer draws nothing; see
 * {@code InvisibleEntityRenderer}.
 *
 * <p>Unlike {@link DevilsProjectile} this hits <b>once and stops</b>. That projectile sweeps an
 * arc every tick and pierces a crowd, because five of them are thrown per swing and the burst is
 * the point. One of these is thrown per swing and it is a spear: it finds the first thing in its
 * way, pulls it in, and is done. Consequently it uses vanilla's own collision — the hit result
 * {@link AbstractHurtingProjectile} already computes — instead of hand-rolled sampling.
 *
 * <p>It also respects terrain, which {@code DevilsProjectile} deliberately does not. A pull that
 * drags a mob through a wall to your feet would make the ability an unanswerable opener; stopping
 * at the wall is what makes cover work against it.
 */
public class AegirTide extends AbstractHurtingProjectile {

    /**
     * How long it flies before giving up, in ticks.
     *
     * <p>{@link AbstractHurtingProjectile} has no timeout of its own — it flies until it hits
     * something or the chunk unloads — so without this a shot fired at open sky is an entity
     * that never goes away.
     */
    private static final int MAX_LIFE_TICKS = 40;

    /** Retained velocity per tick. 1.0 is a straight, undecaying line; vanilla's default is 0.95. */
    private static final float INERTIA = 1.0F;

    /** The trail, and the whole of what this thing looks like. */
    private static final ParticleOptions TRAIL_PARTICLE = ParticleTypes.END_ROD;

    /**
     * Particles per block travelled.
     *
     * <p>The base class already drops one particle a tick, which at this projectile's speed is
     * one every two blocks — a dotted line, not a lance. The trail is therefore drawn here
     * instead, interpolated across the step the projectile just took, so the line is continuous
     * at any speed.
     */
    private static final double TRAIL_PARTICLES_PER_BLOCK = 4.0;

    /** Velocity added to a struck target, toward the thrower. See {@link WeaponPhysics}. */
    private static final double PULL_STRENGTH = 0.55;

    private float damage;

    public AegirTide(EntityType<? extends AegirTide> type, Level level) {
        super(type, level);
    }

    public AegirTide(Level level, LivingEntity thrower, float damage) {
        super(ModWeapons.AEGIR_TIDE.get(), level);
        this.setOwner(thrower);
        this.setPos(thrower.getX(), thrower.getEyeY() - 0.2, thrower.getZ());
        this.damage = damage;
    }

    @Override
    public void tick() {
        Vec3 from = this.position();
        super.tick();

        // super.tick() may have discarded this on a hit; drawing a trail afterwards would put a
        // line of particles through whatever it stopped against.
        if (this.isRemoved()) {
            return;
        }

        if (this.level().isClientSide()) {
            drawTrail(from, this.position());
        } else if (this.tickCount > MAX_LIFE_TICKS) {
            this.discard();
        }
    }

    /** The line of white, laid down across the step just taken rather than at a single point. */
    private void drawTrail(Vec3 from, Vec3 to) {
        Vec3 step = to.subtract(from);
        int points = Math.max(1, (int) (step.length() * TRAIL_PARTICLES_PER_BLOCK));
        for (int i = 0; i < points; i++) {
            Vec3 at = from.add(step.scale((double) i / points));
            this.level().addParticle(TRAIL_PARTICLE, at.x, at.y, at.z, 0.0, 0.0, 0.0);
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult hit) {
        super.onHitEntity(hit);
        if (this.level().isClientSide()) {
            return;
        }

        Entity struck = hit.getEntity();
        Entity thrower = this.getOwner();

        if (struck instanceof LivingEntity target) {
            target.hurt(this.damageSources().indirectMagic(this, thrower), damage);
            // Pulled toward the thrower rather than toward this projectile: the point of the
            // ability is to bring something to you, and by the time it lands the projectile is
            // already wherever the target was standing.
            if (thrower != null) {
                WeaponPhysics.pullTowards(target, thrower.position(), PULL_STRENGTH);
            }
        }

        splash();
        this.discard();
    }

    /** Stops at terrain. See the class note on why this one does not fly through walls. */
    @Override
    protected void onHitBlock(BlockHitResult hit) {
        super.onHitBlock(hit);
        if (!this.level().isClientSide()) {
            splash();
            this.discard();
        }
    }

    private void splash() {
        this.level().playSound(null, this.blockPosition(), SoundEvents.PLAYER_SPLASH_HIGH_SPEED,
                SoundSource.PLAYERS, 0.8F, 1.4F);
    }

    // ── Plumbing ──────────────────────────────────────────────────────────────

    /**
     * The base class draws one of these a tick by itself. It is left as the trail particle
     * rather than switched off, because the two agree — one more white fleck on a white line
     * costs nothing and keeps the projectile visible on the tick it spawns.
     */
    @Override
    protected ParticleOptions getTrailParticle() {
        return TRAIL_PARTICLE;
    }

    @Override
    protected float getInertia() {
        return INERTIA;
    }

    /** It is water. {@link AbstractHurtingProjectile} sets its subclasses alight by default. */
    @Override
    protected boolean shouldBurn() {
        return false;
    }

    @Override
    public boolean isOnFire() {
        return false;
    }

    @Override
    protected void defineSynchedData() {
        // Nothing is synced. Damage is server-only and position comes from entity tracking.
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
