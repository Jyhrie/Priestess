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
 * <p><b>This is a skeleton on purpose.</b> It has a health bar, a hitbox, a shape and no
 * behaviour: it hovers where it is put, watches whoever is nearest, and takes damage until
 * it dies. Attacks come later. Everything needed to hold one is already here, so adding them
 * is a matter of filling in {@link #customServerAiStep} and nothing else.
 *
 * <p>It draws through GeckoLib rather than a hand-built mesh — see {@code DvAwakenRenderer} —
 * so the silhouette lives in {@code geo/entity/dv_awaken.geo.json} and is edited in Blockbench
 * instead of in Java. The animation side is wired but empty; see {@link #registerControllers}.
 *
 * <h2>What is already wired up for those attacks</h2>
 * <ul>
 *   <li><b>A target.</b> The target selectors below run, so {@link #getTarget()} returns the
 *       player it should be shooting at from the moment there is something to shoot with.
 *       They cost almost nothing while nothing acts on them.</li>
 *   <li><b>No melee goal.</b> Having a target and having a way to hurt it are separate; with
 *       no {@code MeleeAttackGoal} and {@code ATTACK_DAMAGE 0} it cannot touch you even
 *       standing inside it.</li>
 *   <li><b>{@link ArtsBeam}.</b> The hitscan helper both other bosses use. One call in the
 *       tick, guarded by a cooldown and {@code hasLineOfSight}, is a working ranged attack.</li>
 * </ul>
 *
 * <h2>Why it floats</h2>
 * {@code setNoGravity(true)} in the constructor rather than a hover goal or a movement
 * control. It has {@code MOVEMENT_SPEED 0} and no navigation, so there is nothing to fight
 * with gravity in the first place — turning gravity off is the whole of the behaviour, and
 * it means it stays exactly where it was summoned instead of settling onto the floor. It
 * hangs dead still: the old placeholder's bob came from the hand-built model that GeckoLib
 * replaced, and the natural home for it now is an idle animation, not the renderer.
 */
public class DvAwaken extends BossMonster implements GeoEntity {

    /**
     * Degrees of yaw per tick, so 20x this is degrees per second. At 2.5 a half-turn takes
     * about three and a half seconds, which is slow enough to read as mass rather than as
     * lag. This is the dial to turn if it still feels wrong; it is the only thing that
     * decides how fast it comes round.
     */
    private static final float TURN_DEGREES_PER_TICK = 2.5F;

    /**
     * Per-entity animation state. {@code createInstanceCache} rather than the singleton
     * variant because every Awaken in the world needs its own playhead — the singleton cache
     * is for items and blocks, where one shared state is the point.
     */
    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

    public DvAwaken(EntityType<? extends DvAwaken> type, Level level) {
        super(type, level, BossEvent.BossBarColor.BLUE, BossEvent.BossBarOverlay.PROGRESS);
        this.xpReward = 300;
        // Set here rather than in a tick: it is a property of what this is, not something
        // that has to be re-asserted, and it saves and reloads with the entity for free.
        this.setNoGravity(true);
    }

    public static AttributeSupplier.Builder attributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 300.0)
                // Zero, and no navigation goals to use it if it were not. It hangs where it
                // is put.
                .add(Attributes.MOVEMENT_SPEED, 0.0)
                // Zero until there is an attack to give it. A boss that can chip you by
                // being stood in is a boss with an attack nobody designed.
                .add(Attributes.ATTACK_DAMAGE, 0.0)
                .add(Attributes.FOLLOW_RANGE, 48.0)
                .add(Attributes.ARMOR, 8.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
    }

    /**
     * Two target selectors and nothing else. No movement, no attack, and deliberately no
     * {@code LookAtPlayerGoal} — see {@link #facePlayerSlowly}.
     *
     * <p>Turning to watch you is not decoration: a shape that tracks you is the only cue
     * that it is awake at all, and without it the placeholder is indistinguishable from a
     * block. It just cannot be vanilla's look goal that does it.
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
     * Turns to face its target at a fixed rate, instead of letting vanilla do it.
     *
     * <h2>Why not {@code LookAtPlayerGoal}</h2>
     * Because for a mob that never moves, vanilla's rotation does not ease — it lurches.
     * {@code BodyRotationControl} only runs its body-follows-head step when
     * {@code isMoving()}, and this thing never is; the stationary path instead waits until
     * the head has drifted more than 15 degrees and then calls
     * {@code yBodyRot = Mth.rotateIfNecessary(yBodyRot, yHeadRot, getMaxHeadYRot())}, which
     * closes the whole gap in a single tick. The result is a body that sits still and then
     * jumps, which at six and three quarter blocks across is what reads as a snap.
     *
     * <p>Turning {@code getMaxHeadYRot()} down does not fix that. The 15-degree gate still
     * fires in steps, so a smaller cap buys a stutter of little jerks instead of one big
     * one. The only way to get a constant rate is to own the rotation.
     *
     * <p>So: head, body and entity yaw are all set to the same value every tick. Keeping
     * them equal is also what stops {@code BodyRotationControl} interfering — it still runs,
     * but with no gap between head and body there is nothing for it to close.
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
     * Dreamland, dropped in code rather than from a loot table — the same reason Jesselton's
     * Master Key is: exactly one, every time, regardless of Looting, difficulty or whether
     * the kill rolled anything else.
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
     * No controllers, because the model has no animations yet — it renders as a static pose.
     *
     * <p>Leaving this empty is deliberate and safe: GeckoLib only reads
     * {@code animations/entity/dv_awaken.animation.json} when a controller asks for a clip by
     * name, so the missing file costs nothing until there is something in it. Add the file
     * and a {@code controllers.add(new AnimationController<>(...))} here together.
     */
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }
}
