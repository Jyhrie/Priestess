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
 * <p>Same test for what belongs here as {@link BossMonster}: only the <em>mechanical</em>
 * parts, true of any mob of this shape. Everything a subclass actually is — health, damage,
 * drops, sounds — stays in the subclass, and one that needs a goal these do not have should
 * override {@link #registerGoals} rather than growing a flag here.
 *
 * <p>A subclass is not finished until there is a {@code geo/entity/<name>.geo.json} and a
 * {@code textures/entity/<name>.png}, which {@code PriestessGeoRenderer} derives from the
 * registry name. The animation file can wait — GeckoLib resolves it lazily.
 */
public abstract class GeoMonster extends Monster implements GeoEntity {

    /** Per-entity, because every mob needs its own playhead. */
    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

    protected GeoMonster(EntityType<? extends GeoMonster> type, Level level) {
        super(type, level);
    }

    /**
     * Entirely stock. The {@code false} on {@code MeleeAttackGoal} is "keep pathing to a
     * target you cannot currently see", which stops one of these giving up the moment you
     * step behind a pillar.
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
     * Empty on purpose: GeckoLib only reads the animation file when a controller asks for a
     * clip by name. Add the file and a controller together, in the subclass owning the model.
     */
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }
}
