package com.jyhrie.priestess.entity.bosses;

import com.jyhrie.priestess.entity.BossMonster;
import com.jyhrie.priestess.entity.projectiles.ArtsBeam;
import com.jyhrie.priestess.item.ModItems;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
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
 * "Awaken" — a shape that hangs in the air and does nothing at all.
 *
 * <p><b>A skeleton on purpose.</b> It has a health bar, a hitbox and no behaviour: it hovers,
 * watches whoever is nearest, and takes damage until it dies. Adding attacks is a matter of
 * filling in {@link #customServerAiStep} — the target selectors already run, so
 * {@link #getTarget()} is ready, and {@link ArtsBeam} is the hitscan helper the other bosses
 * use. There is no melee goal and {@code ATTACK_DAMAGE} is 0, so it cannot touch you even
 * standing inside it.
 *
 * <p>It floats via {@code setNoGravity(true)} rather than a hover goal: with
 * {@code MOVEMENT_SPEED 0} and no navigation there is nothing fighting gravity in the first
 * place, so turning it off is the whole behaviour and keeps it where it was summoned.
 */
public class DvAwaken extends BossMonster implements GeoEntity {

    /** Degrees of yaw per tick — the only thing deciding how fast it comes round. */
    private static final float TURN_DEGREES_PER_TICK = 2.5F;

    /**
     * {@code createInstanceCache} rather than the singleton variant, because every Awaken
     * needs its own playhead — the singleton cache is for items and blocks.
     */
    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

    public DvAwaken(EntityType<? extends DvAwaken> type, Level level) {
        super(type, level, BossEvent.BossBarColor.BLUE, BossEvent.BossBarOverlay.PROGRESS);
        this.xpReward = 300;
        // Here rather than in a tick, so it saves and reloads with the entity for free.
        this.setNoGravity(true);
    }

    /** Defaults only; {@code EntityStats} overwrites all six from {@code BossStats} on join. */
    public static AttributeSupplier.Builder attributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 300.0)
                .add(Attributes.MOVEMENT_SPEED, 0.0)
                // Zero until there is an attack to give it. A boss that chips you by being
                // stood in is a boss with an attack nobody designed.
                .add(Attributes.ATTACK_DAMAGE, 0.0)
                .add(Attributes.FOLLOW_RANGE, 48.0)
                .add(Attributes.ARMOR, 8.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
    }

    /**
     * Two target selectors and nothing else — no movement, no attack, and deliberately no
     * {@code LookAtPlayerGoal}; see {@link #facePlayerSlowly}.
     */
    @Override
    protected void registerGoals() {
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    /** {@code super} keeps the boss bar in step with its health. */
    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        facePlayerSlowly();
    }

    /**
     * Turns to face its target at a fixed rate, instead of letting vanilla do it. For a mob
     * that never moves vanilla's rotation lurches: {@code BodyRotationControl} only runs its
     * body-follows-head step when {@code isMoving()}, and the stationary path waits until the
     * head has drifted 15 degrees and then closes the whole gap in one tick. Lowering
     * {@code getMaxHeadYRot()} only trades one big jerk for a stutter of small ones.
     *
     * <p>So head, body and entity yaw are all set to the same value every tick — which also
     * leaves {@code BodyRotationControl} nothing to close.
     */
    private void facePlayerSlowly() {
        LivingEntity target = this.getTarget();
        if (target == null || !target.isAlive()) {
            return;
        }

        double dx = target.getX() - this.getX();
        double dz = target.getZ() - this.getZ();
        float wanted = (float) (Mth.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0F;
        float yaw = Mth.approachDegrees(this.getYRot(), wanted, TURN_DEGREES_PER_TICK);

        // All three, or the renderer and the body control disagree about which way it faces.
        this.setYRot(yaw);
        this.setYBodyRot(yaw);
        this.setYHeadRot(yaw);
    }

    /**
     * Dropped in code rather than from a loot table, so it is exactly one every time
     * regardless of Looting or difficulty.
     */
    @Override
    protected void dropCustomDeathLoot(DamageSource source, int looting, boolean recentlyHitByPlayer) {
        super.dropCustomDeathLoot(source, looting, recentlyHitByPlayer);
        this.spawnAtLocation(new ItemStack(ModItems.DREAMLAND.get()));
    }

    /** Nothing moves it: not a piston, not water, not the blast that is trying to kill it. */
    @Override
    public void push(double x, double y, double z) {
    }

    /** It is already off the ground on purpose; falling is not a thing that happens to it. */
    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    /**
     * Empty on purpose: GeckoLib only reads the animation file when a controller asks for a
     * clip by name, so the missing file costs nothing. Add the file and a controller together.
     */
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }
}
