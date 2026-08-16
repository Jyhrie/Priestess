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
 * <p>The one animated entity here that is not a {@link WeaponVfx}, because that class promises
 * to deal no damage and to appear only after its ability has resolved. This <em>is</em> the
 * ability, ticking, so it is its own entity and borrows only the renderer.
 *
 * <p>The damage interval is tied to the interval the damage is quoted in, so the tooltip's
 * "5 damage a second" is literally what the code does.
 *
 * <p>The usual "lifetime in ticks == {@code animation_length} × 20" rule does not apply: this
 * clip is a two-second spin marked {@code "loop": true}, so lengthening the vortex is a change
 * to {@link #LIFETIME_TICKS} alone.
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

    /** Fixed at one second, because the config quotes the damage per second. */
    private static final int DAMAGE_INTERVAL_TICKS = 20;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    /** For the damage source, so a kill is credited to the player. Not persisted. */
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

    /** {@code super.tick()} advances {@code tickCount}, which is GeckoLib's animation clock. */
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

            // The query above is a cube; this makes the reach a sphere.
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

    @Override
    protected void defineSynchedData() {
        // Position rides the spawn packet; the pull and the damage are the server's business.
    }

    /**
     * Nothing to save. Reloading into a half-finished vortex with no caster is worse than
     * reloading into none.
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

    /** Transition length 0: GeckoLib's default eases into a clip over several ticks. */
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
