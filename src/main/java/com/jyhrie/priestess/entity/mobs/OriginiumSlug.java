package com.jyhrie.priestess.entity.mobs;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.BlockPathTypes;

/**
 * Originium Slug — vermin. Weak, fast, and currently nothing more than that.
 *
 * <h2>Stripped back on purpose</h2>
 * This mob used to carry two mechanics and now carries none:
 * <ul>
 *   <li><b>Machine-eating.</b> It was drawn to any block exposing Forge's energy capability,
 *       sat on it and ate the charge — a tax on leaving a base running unattended, and one
 *       that worked against every tech mod without naming one. Cut, along with the
 *       {@code Machines} helper that found and drained them.</li>
 *   <li><b>The death burst.</b> Killing one corroded everything within three blocks and
 *       infected players with Oripathy, so a tight nest chained. Cut.</li>
 * </ul>
 *
 * <p>What is left is a plain melee monster: it walks at you, it hits you, it dies. That is
 * deliberate — the mechanics are coming back in some form, and an empty mob is a cleaner
 * thing to hang them off than a half-removed one.
 *
 * <p>It also no longer spawns anywhere. There is no {@code SpawnPlacements} rule and no
 * spawner entry in {@code ModBiomes}, so like the three Medium-bearers it is a spawn egg and
 * nothing else until the wastes are given a population again.
 */
public class OriginiumSlug extends Monster {

    public OriginiumSlug(EntityType<? extends OriginiumSlug> type, Level level) {
        super(type, level);
        // Vermin, not a hazard to navigate around: a slug that made players path around it
        // would be a much bigger deal than one worth 8 health.
        this.setPathfindingMalus(BlockPathTypes.WATER, 8.0F);
    }

    public static AttributeSupplier.Builder attributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 8.0)
                // Faster than a player walking, slower than one sprinting: they catch you if
                // you stand still and swarm you if you dawdle, but you can always leave.
                .add(Attributes.MOVEMENT_SPEED, 0.32)
                .add(Attributes.ATTACK_DAMAGE, 2.0)
                .add(Attributes.FOLLOW_RANGE, 24.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.0, false));
        this.goalSelector.addGoal(2, new RandomStrollGoal(this, 0.8));
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 8.0F));

        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.SILVERFISH_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.SLIME_HURT_SMALL;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.SLIME_SQUISH;
    }
}
