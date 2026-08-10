package com.jyhrie.priestess.entity.bosses;

import com.jyhrie.priestess.entity.ArtsBeam;
import com.jyhrie.priestess.entity.BossMonster;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
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
 * <p>It draws through GeckoLib rather than a hand-built mesh — see {@code AwakenRenderer} —
 * so the silhouette lives in {@code geo/entity/awaken.geo.json} and is edited in Blockbench
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
public class Awaken extends BossMonster implements GeoEntity {

    /**
     * Per-entity animation state. {@code createInstanceCache} rather than the singleton
     * variant because every Awaken in the world needs its own playhead — the singleton cache
     * is for items and blocks, where one shared state is the point.
     */
    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

    public Awaken(EntityType<? extends Awaken> type, Level level) {
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
     * A look goal and two target selectors. No movement, no attack.
     *
     * <p>The look goal is not decoration — a cube that tracks you is the only cue that it is
     * awake at all, and without it the placeholder is indistinguishable from a block.
     */
    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new LookAtPlayerGoal(this, Player.class, 32.0F));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    /** Nothing yet. {@code super} keeps the boss bar in step with its health. */
    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
    }

    /** Nothing moves it: not a piston, not water, not the blast that is trying to kill it. */
    @Override
    public void push(double x, double y, double z) {
    }

    /** It is already off the ground on purpose; falling is not a thing that happens to it. */
    @Override
    public boolean causeFallDamage(float distance, float multiplier,
                                   net.minecraft.world.damagesource.DamageSource source) {
        return false;
    }

    /**
     * No controllers, because the model has no animations yet — it renders as a static pose.
     *
     * <p>Leaving this empty is deliberate and safe: GeckoLib only reads
     * {@code animations/entity/awaken.animation.json} when a controller asks for a clip by
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
