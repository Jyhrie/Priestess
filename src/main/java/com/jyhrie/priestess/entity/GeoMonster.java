package com.jyhrie.priestess.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
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
 * An ordinary melee monster that draws through GeckoLib instead of a vanilla mesh.
 *
 * <p>Same test for what belongs here as {@link BossMonster}: only the parts that are
 * <em>mechanical</em> — true of any mob of this shape, with no design content to get wrong.
 * That is the GeckoLib plumbing and the stock "walk up to the player and hit them" goal set.
 * Everything a subclass actually is — how much health, how hard it hits, what it leaves
 * behind, what it sounds like — stays in the subclass.
 *
 * <p>Three mobs share it today ({@code DvFailure}, {@code DvReplica}, {@code DvBionic}) and they
 * differ in nothing but numbers, which is exactly the case a base class is for. A fourth that
 * needs a goal these do not have should override {@link #registerGoals} rather than growing a
 * flag here.
 *
 * <h2>What a subclass still owes the mod</h2>
 * The GeckoLib half of a mob is three resource paths derived from its registry name, and
 * {@code PriestessGeoRenderer} is what derives them — so a subclass is not finished until
 * there is a {@code geo/entity/<name>.geo.json} and a {@code textures/entity/<name>.png} to
 * go with it. The animation file is the one that can wait: GeckoLib resolves it lazily and
 * {@link #registerControllers} asks for no clips, so a static pose costs nothing until
 * someone animates it.
 */
public abstract class GeoMonster extends Monster implements GeoEntity {

    /**
     * Per-entity animation state. {@code createInstanceCache} rather than the singleton
     * variant for the same reason as {@code DvAwaken}: every mob in the world needs its own
     * playhead, and the singleton cache is for items and blocks where one shared state is
     * the point.
     */
    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

    protected GeoMonster(EntityType<? extends GeoMonster> type, Level level) {
        super(type, level);
    }

    /**
     * Entirely stock, and in the order every vanilla melee monster uses it: don't drown,
     * chase and hit, otherwise wander and look about.
     *
     * <p>{@code MeleeAttackGoal(this, 1.0, false)} — the {@code false} is "keep pathing to a
     * target you cannot currently see", which is what stops one of these giving up the moment
     * you step behind a pillar.
     */
    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.0, false));
        this.goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 0.8));
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 12.0F));
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    /**
     * No controllers, because none of the models have animations yet — they render as a
     * static pose, exactly as {@code DvAwaken} does.
     *
     * <p>Safe to leave empty: GeckoLib only reads
     * {@code animations/entity/<name>.animation.json} when a controller asks for a clip by
     * name, so the missing file costs nothing. Add the file and a controller together, in the
     * subclass that owns the model.
     */
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }
}
