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
 * <p>Mechanically a {@link BossMonster} — the bar, no despawn, no portalling or shoving. What
 * separates it from the two in {@code bosses/} is scale and role: a smaller, single-phase
 * fight with no progression item gated behind it. Hence its own package, which is a statement
 * about where a mob sits in a movement rather than a flag or a subclass.
 *
 * <p>At half health it speeds up. Deliberately small — a readable turn needing no new damage
 * type, summons or second attack — and a placeholder for whatever a mob called The First to
 * Talk should eventually do with its voice.
 *
 * <p>The speed-up is an {@link AttributeModifier} rather than a base-value write, because
 * {@code EntityStats} rewrites base movement speed from the config every time this joins the
 * world; a base write would be undone the first time its chunk reloaded.
 */
public class SvTheFirstToTalk extends BossMonster implements GeoEntity {

    /** Below this fraction of max health it is enraged. */
    private static final float ENRAGE_AT = 0.5F;

    private static final double BASE_SPEED = 0.26;

    /**
     * Identifies the enrage's speed modifier, so it is never added twice and can be found again
     * after a reload. Any fixed UUID does.
     */
    private static final UUID ENRAGE_SPEED_ID = UUID.fromString("6f2a1c84-9e33-4d1b-8a57-0c9d4f1e7b20");

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

    /** One-shot: "have I already played the turn", not "am I past the threshold". */
    private boolean enraged;

    public SvTheFirstToTalk(EntityType<? extends SvTheFirstToTalk> type, Level level) {
        super(type, level, BossEvent.BossBarColor.GREEN, BossEvent.BossBarOverlay.NOTCHED_6);
        this.xpReward = 120;
    }

    /** Defaults only; {@code EntityStats} overwrites all six from {@code MinibossStats} on join. */
    public static AttributeSupplier.Builder attributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 120.0)
                .add(Attributes.MOVEMENT_SPEED, BASE_SPEED)
                .add(Attributes.ATTACK_DAMAGE, 9.0)
                .add(Attributes.FOLLOW_RANGE, 40.0)
                .add(Attributes.ARMOR, 4.0)
                // Not 1.0: a miniboss should still flinch. Full immunity is what marks the two
                // real bosses out.
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
            // The difference from where it stands, so the total lands exactly on the configured
            // enraged speed rather than on base plus a fixed bonus.
            double delta = MinibossStats.FIRST_TO_TALK_ENRAGED_SPEED.get() - speed.getBaseValue();
            speed.addPermanentModifier(new AttributeModifier(ENRAGE_SPEED_ID, "Enraged",
                    delta, AttributeModifier.Operation.ADDITION));
        }

        bossEvent.setColor(BossEvent.BossBarColor.RED);
        this.playSound(SoundEvents.WARDEN_ROAR, 2.0F, 1.2F);
    }

    /**
     * The enrage is derived from health but the <em>flag</em> is not, so it has to be saved or
     * a reload mid-fight replays the roar. This is the bug {@code docs/BOSSES.md} records
     * against Jesselton, fixed here rather than repeated.
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

    /** No controllers: the model has no animations yet. */
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
