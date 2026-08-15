package com.jyhrie.priestess.weapons.entity;

import com.jyhrie.priestess.config.WeaponStats;
import com.jyhrie.priestess.weapons.ModWeapons;
import com.jyhrie.priestess.weapons.WeaponPhysics;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * Aegir Greatspear's third ability: a standing vortex that holds a room for eight seconds.
 *
 * <p><b>This is the one animated entity in the mod that is not a {@link WeaponVfx}</b>, and the
 * reason is the contract on that class — a {@code WeaponVfx} deals no damage and exists purely
 * to be looked at, because every ability that spawns one has already resolved by the time it
 * appears. This has not resolved: it is the ability, ticking, for a hundred and sixty ticks
 * after the click that made it. Subclassing {@code WeaponVfx} would have inherited the renderer
 * for free and quietly broken the promise its javadoc makes, so it is its own entity and
 * borrows only the renderer.
 *
 * <h2>What it does each tick</h2>
 * <ul>
 *   <li><b>Every tick</b> — drags everything alive inside {@link #RADIUS} toward its centre.
 *       The pull is per-tick and gentle rather than occasional and violent, which is what makes
 *       it read as water and not as a series of shoves.</li>
 *   <li><b>Every second</b> — {@link #DAMAGE_PER_SECOND} to everything still inside it. Tied to
 *       the same interval the damage is quoted in, so the tooltip's "5 damage a second" is
 *       literally what the code does.</li>
 * </ul>
 *
 * <h2>Why the animation loops, and why the tick/length rule does not apply here</h2>
 * Every other animated effect in the mod obeys "lifetime in ticks == {@code animation_length}
 * × 20", because each plays one clip once and must vanish on its last frame. This one lives far
 * longer than any sensible hand-keyframed clip: its animation is a <b>two-second spin marked
 * {@code "loop": true}</b>, played four times over. There is nothing to keep in step, and
 * lengthening the vortex is a change to {@link #LIFETIME_TICKS} alone.
 */
public class AegirWhirlpool extends Entity implements GeoEntity {

    /** Named "play" like every other effect clip, so the shared model can find it. */
    private static final RawAnimation SPIN = RawAnimation.begin().thenLoop("play");

    /** Eight seconds. */
    public static final int LIFETIME_TICKS = 160;

    /** How far out it reaches, in blocks. Also the radius the damage uses. */
    private static final double RADIUS = 6.0;

    /** Velocity added toward the centre, per tick, per caught mob. */
    private static final double PULL_STRENGTH = 0.12;

    /**
     * How often it grinds. Fixed at one second because the number it applies is <em>quoted</em>
     * per second — {@code maelstromDamagePerSecond} in the config — so the interval and the
     * units have to agree or the tooltip starts lying.
     */
    private static final int DAMAGE_INTERVAL_TICKS = 20;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    /**
     * Who cast it, for the damage source, so a kill is credited to the player and not to a
     * nameless entity. Not persisted — see the lifetime note on {@link #addAdditionalSaveData}.
     */
    @Nullable
    private LivingEntity owner;

    public AegirWhirlpool(EntityType<? extends AegirWhirlpool> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    /**
     * Opens one at a position. Server-side only — spawned on the client it is a ghost that
     * disappears on the next sync.
     */
    public static void spawn(Level level, Vec3 at, LivingEntity caster) {
        if (level.isClientSide()) {
            return;
        }
        AegirWhirlpool whirlpool = new AegirWhirlpool(ModWeapons.AEGIR_WHIRLPOOL.get(), level);
        whirlpool.moveTo(at.x, at.y, at.z, 0.0F, 0.0F);
        whirlpool.owner = caster;
        level.addFreshEntity(whirlpool);

        level.playSound(null, whirlpool.blockPosition(), SoundEvents.CONDUIT_ACTIVATE,
                SoundSource.PLAYERS, 1.2F, 0.7F);
    }

    /**
     * {@code super.tick()} is called for the same reason {@link WeaponVfx} calls it: it advances
     * {@code tickCount}, which is both GeckoLib's animation clock and the age this counts its
     * own life against.
     */
    @Override
    public void tick() {
        super.tick();

        if (this.level().isClientSide()) {
            swirl();
            return;
        }

        boolean damageTick = this.tickCount % DAMAGE_INTERVAL_TICKS == 0;
        Vec3 centre = this.position();

        for (LivingEntity caught : this.level().getEntitiesOfClass(LivingEntity.class,
                this.getBoundingBox().inflate(RADIUS),
                other -> other != owner && other.isAlive() && !other.isSpectator())) {

            // The bounding-box query above is a cube; this is what makes the reach a sphere,
            // so a mob in the corner of that cube is not caught by a vortex it is outside of.
            if (caught.position().distanceToSqr(centre) > RADIUS * RADIUS) {
                continue;
            }

            WeaponPhysics.pullTowards(caught, centre, PULL_STRENGTH);

            if (damageTick) {
                caught.hurt(this.damageSources().indirectMagic(this, owner),
                        WeaponStats.AEGIR_MAELSTROM_DAMAGE_PER_SECOND.get().floatValue());
            }
        }

        if (this.tickCount >= LIFETIME_TICKS) {
            this.discard();
        }
    }

    /** A ring of spray at the rim, so the edge of the pull is somewhere the player can see. */
    private void swirl() {
        double angle = this.tickCount * 0.4;
        for (int i = 0; i < 3; i++) {
            double at = angle + i * (Math.PI * 2.0 / 3.0);
            this.level().addParticle(ParticleTypes.BUBBLE_COLUMN_UP,
                    this.getX() + Math.cos(at) * RADIUS * 0.8,
                    this.getY() + 0.2,
                    this.getZ() + Math.sin(at) * RADIUS * 0.8,
                    0.0, 0.05, 0.0);
        }
    }

    // ── Plumbing ──────────────────────────────────────────────────────────────

    @Override
    protected void defineSynchedData() {
        // Nothing is synced. Position rides the spawn packet, and the client needs no more than
        // that — the pull and the damage are the server's business and the spin is on a clock
        // both sides already have.
    }

    /**
     * Nothing to save. One of these lives eight seconds, so the only way it could be written to
     * disk is a save landing mid-cast; reloading into a half-finished vortex with no caster is
     * worse than reloading into none.
     */
    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean canBeCollidedWith() {
        return false;
    }

    /** Large, brief, and popping out of view while it is still pulling you reads as a bug. */
    @Override
    public boolean shouldRender(double camX, double camY, double camZ) {
        return true;
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    /**
     * Transition length 0, as with {@link WeaponVfx}: GeckoLib's default eases into a clip over
     * several ticks, and a vortex that fades up to speed looks like it is buffering.
     */
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 0,
                state -> state.setAndContinue(SPIN)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    /** Unused, but {@link ServerLevel} entity queries expect a sane answer. */
    @Override
    public boolean isAttackable() {
        return false;
    }

    @Nullable
    public LivingEntity owner() {
        return owner;
    }

    /** Nothing pushes a vortex: not a piston, not a mob walking into it. */
    @Override
    public void push(Entity entity) {
    }
}
