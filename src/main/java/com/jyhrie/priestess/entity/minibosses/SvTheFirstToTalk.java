package com.jyhrie.priestess.entity.minibosses;

import com.jyhrie.priestess.config.MinibossStats;
import com.jyhrie.priestess.entity.BossMonster;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
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

import java.util.UUID;

/**
 * The First to Talk — Sal Viento's miniboss, and the first thing down there that answers.
 *
 * <h2>What a miniboss is, in this mod</h2>
 * Mechanically it is a {@link BossMonster}: it has the bar, it never despawns, it cannot be
 * portalled out or shoved. What separates it from the two in {@code bosses/} is scale and
 * role — 120 health rather than 220 or 400, an ordinary melee fight rather than a
 * multi-phase one, and no progression item gated behind it. It is the fight that tells you
 * the dungeon has a floor beneath the one you are on, not the fight that ends it.
 *
 * <p>Hence its own package rather than a flag or a subclass. {@code minibosses/} is a
 * statement about where a mob sits in a movement, which is exactly what {@code bosses/}
 * and {@code mobs/} already are; adding a third tier costs one directory and no code.
 *
 * <h2>The one thing it does</h2>
 * At half health it speeds up, to whatever {@code enragedMovementSpeed} says in
 * {@code config/priestess/miniboss.toml}. That is a deliberately small trick. It is a readable turn
 * that needs no new damage type, no summons and no second attack, and it leaves the interesting
 * half of the fight unspent: a mob called The First to Talk should eventually do something with
 * its voice, and this is a placeholder standing in that slot rather than filling it.
 *
 * <p>It is applied as an {@link AttributeModifier} and not by writing the base value, which used
 * to be the argument here — nothing removes it later, so a plain write was simpler. It cannot be
 * one any more: {@code EntityStats} rewrites base movement speed from the config every time this
 * joins the world, so a base write would be silently undone the first time its chunk reloaded.
 * A modifier sits on top of that and survives it, and saves and reloads for free.
 */
public class SvTheFirstToTalk extends BossMonster implements GeoEntity {

    /** Below this fraction of max health it is enraged. */
    private static final float ENRAGE_AT = 0.5F;

    private static final double BASE_SPEED = 0.26;

    /**
     * Identifies the enrage's speed modifier, so the same one is never added twice and so it can
     * be found again after a reload. Any fixed UUID does; this one was generated for the purpose
     * and means nothing beyond being unique.
     */
    private static final UUID ENRAGE_SPEED_ID = UUID.fromString("6f2a1c84-9e33-4d1b-8a57-0c9d4f1e7b20");

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

    /** One-shot: "have I already played the turn", not "am I past the threshold". */
    private boolean enraged;

    public SvTheFirstToTalk(EntityType<? extends SvTheFirstToTalk> type, Level level) {
        super(type, level, BossEvent.BossBarColor.GREEN, BossEvent.BossBarOverlay.NOTCHED_6);
        this.xpReward = 120;
    }

    /**
     * Defaults only. {@code EntityStats} overwrites all six of these from
     * {@code config/priestess/miniboss.toml} as it joins the world, so editing a number
     * here alone changes nothing — change it in {@code MinibossStats} too.
     */
    public static AttributeSupplier.Builder attributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 120.0)
                .add(Attributes.MOVEMENT_SPEED, BASE_SPEED)
                .add(Attributes.ATTACK_DAMAGE, 9.0)
                .add(Attributes.FOLLOW_RANGE, 40.0)
                .add(Attributes.ARMOR, 4.0)
                // Not 1.0. A miniboss should still flinch — full immunity is what marks the
                // two real bosses out, and spending it here would flatten the difference.
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.7);
    }

    /** Stock melee. Everything that makes it a miniboss is the bar and the enrage. */
    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.0, true));
        this.goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 0.7));
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 24.0F));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    /** {@code super} keeps the bar in step with its health. */
    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();

        if (!enraged && this.getHealth() <= this.getMaxHealth() * ENRAGE_AT) {
            enrage();
        }
    }

    private void enrage() {
        enraged = true;

        AttributeInstance speed = this.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null && speed.getModifier(ENRAGE_SPEED_ID) == null) {
            // The difference from where it currently stands, so the total lands exactly on the
            // configured enraged speed rather than on base plus some fixed bonus. Taken at the
            // moment of enraging, which is the moment the number is meant to describe.
            double delta = MinibossStats.FIRST_TO_TALK_ENRAGED_SPEED.get() - speed.getBaseValue();
            speed.addPermanentModifier(new AttributeModifier(ENRAGE_SPEED_ID, "Enraged",
                    delta, AttributeModifier.Operation.ADDITION));
        }

        bossEvent.setColor(BossEvent.BossBarColor.RED);
        this.playSound(SoundEvents.WARDEN_ROAR, 2.0F, 1.2F);
    }

    /**
     * The enrage is derived from health, but the <em>flag</em> is not, so it has to be saved
     * or a reload mid-fight replays the roar and re-applies a speed that is already applied.
     *
     * <p>This is the bug {@code docs/BOSSES.md} records against Jesselton, fixed here rather
     * than repeated. His {@code announcedPhaseTwo} does not persist, so quitting and
     * rejoining below half health gives him a second transition for free.
     */
    @Override
    public void addAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Enraged", enraged);
    }

    @Override
    public void readAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        enraged = tag.getBoolean("Enraged");
        if (enraged) {
            // The speed modifier itself does save, but the bar colour does not — reapply so a
            // reloaded fight looks like the one you left.
            bossEvent.setColor(BossEvent.BossBarColor.RED);
        }
    }

    /** No controllers: the model has no animations yet. Same as every other GeckoLib mob here. */
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.WARDEN_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.WARDEN_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.WARDEN_DEATH;
    }
}
