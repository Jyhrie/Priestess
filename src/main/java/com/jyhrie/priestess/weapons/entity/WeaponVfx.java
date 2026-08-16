package com.jyhrie.priestess.weapons.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * A piece of animated geometry that exists to be looked at.
 *
 * <p>It deals no damage and moves nowhere: spawned at a position and a facing, it plays one
 * animation and removes itself. <b>Every ability that uses one has already resolved its damage
 * by the time this is spawned</b> — what hits and what you see are separate, so a dropped
 * packet costs a player nothing but a flourish.
 *
 * <p>One class serves every effect. They differ only in assets, and {@code WeaponVfxModel}
 * derives all three paths from the entity type's registry name, so adding one is a register
 * line plus three files. Every clip is named <b>{@code play}</b>, which is what lets a single
 * {@link RawAnimation} drive them all.
 *
 * <p>Lifetime is server-side only and never synced, but it must <em>match</em> the asset: the
 * value passed to {@link #spawn} is in ticks and {@code animation_length} is in seconds at 20
 * ticks each. Too short and the mesh vanishes mid-swing; too long and it hangs on its last
 * frame.
 */
public class WeaponVfx extends Entity implements GeoEntity {

    /** Every VFX animation file calls its one clip "play". See the class note. */
    private static final RawAnimation PLAY = RawAnimation.begin().thenPlay("play");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private int lifetime = 10;

    public WeaponVfx(EntityType<? extends WeaponVfx> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    /**
     * Places one in the world. Server-side only — spawned on the client it is a ghost.
     *
     * <p>{@code moveTo} rather than {@code setPos} plus {@code setYRot}: it writes the previous
     * rotation fields too, so the first frame renders at the angle asked for instead of
     * interpolating into it from due south.
     */
    public static void spawn(Level level, EntityType<WeaponVfx> type, Vec3 at,
                             float yaw, float pitch, int lifetimeTicks) {
        if (level.isClientSide()) {
            return;
        }
        WeaponVfx vfx = new WeaponVfx(type, level);
        vfx.moveTo(at.x, at.y, at.z, yaw, pitch);
        vfx.lifetime = lifetimeTicks;
        level.addFreshEntity(vfx);
    }

    /**
     * {@code super.tick()} advances {@code tickCount}, which GeckoLib drives the animation
     * clock off. Skip it and the mesh renders frozen on frame zero for its whole life.
     */
    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide() && this.tickCount >= this.lifetime) {
            this.discard();
        }
    }

    @Override
    protected void defineSynchedData() {
        // Position and rotation ride the spawn packet; lifetime is the server's business.
    }

    /** Nothing to save — one of these never survives long enough for a chunk to be written. */
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

    /** Always drawn: these are large, brief, and popping out of view mid-animation reads as a bug. */
    @Override
    public boolean shouldRender(double camX, double camY, double camZ) {
        return true;
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    /**
     * Transition length 0 is load-bearing: GeckoLib's default blends into a clip over several
     * ticks, which on an eight-tick effect is most of the animation spent easing in.
     */
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 0,
                state -> state.setAndContinue(PLAY)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
