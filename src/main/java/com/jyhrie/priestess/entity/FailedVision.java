package com.jyhrie.priestess.entity;

import com.jyhrie.priestess.damage.ModDamageTypes;
import com.jyhrie.priestess.item.ModItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * The Failed Vision — three hundred years of an Originium neural network left running with
 * nobody to answer it. The second boss of the Columbia chapter, and the one that is a puzzle
 * before it is a fight.
 *
 * <h2>The neural nodes</h2>
 * It cannot move and it cannot be shot down. While it still has nodes, ordinary damage does
 * <em>nothing at all</em> to the core — the hit is refused outright, not reduced — and the
 * only thing that takes a node off it is an explosion or fire. That is the mechanic
 * described in the GDD as "explosives or chemical throwers", expressed in terms a modded
 * pack will already have: TNT, a rocket, a flamethrower, a grenade from any tech mod, all
 * of them arrive as an explosion or as fire damage.
 *
 * <p>The point is to stop the player from treating a stationary boss as a free damage
 * check. Melee is useless until the nodes are gone, and it spends the whole time you are
 * finding explosives spawning Franks and burning you down with Arts lasers.
 *
 * <p>Nodes are synched to the client so the boss bar can count them; see
 * {@link #DATA_NODES}. They are saved, so leaving and coming back does not reset the puzzle.
 */
public class FailedVision extends BossMonster {

    /** How many neural nodes it starts with. Each one takes one explosion or fire hit. */
    public static final int TOTAL_NODES = 6;

    private static final EntityDataAccessor<Integer> DATA_NODES =
            SynchedEntityData.defineId(FailedVision.class, EntityDataSerializers.INT);

    private static final String NBT_NODES = "neural_nodes";

    private static final int LASER_COOLDOWN_TICKS = 60;
    private static final double LASER_RANGE = 28.0;
    private static final float LASER_DAMAGE = 8.0F;

    private static final int SPAWN_COOLDOWN_TICKS = 100;
    private static final int FRANKS_PER_WAVE = 2;
    private static final int MAX_LIVE_FRANKS = 10;
    private static final double ARENA_RADIUS = 24.0;

    private int laserCooldown;
    private int spawnCooldown = SPAWN_COOLDOWN_TICKS;

    public FailedVision(EntityType<? extends FailedVision> type, Level level) {
        // Six notches, one per neural node. The bar switches to a plain progress bar when
        // the last one comes off — see barProgress().
        super(type, level, BossEvent.BossBarColor.GREEN, BossEvent.BossBarOverlay.NOTCHED_6);
        this.xpReward = 400;
    }

    public static AttributeSupplier.Builder attributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 400.0)
                // Zero, not small. It is a mass of flesh grown into the floor of the test
                // chamber; the arena is the fight, and it must never follow you out of it.
                .add(Attributes.MOVEMENT_SPEED, 0.0)
                .add(Attributes.ATTACK_DAMAGE, 0.0)
                .add(Attributes.FOLLOW_RANGE, 48.0)
                .add(Attributes.ARMOR, 4.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
    }

    @Override
    protected void registerGoals() {
        // No movement goals at all — not even a stroll. Everything it does is in
        // customServerAiStep, because a pathfinding goal on a mob with zero speed just
        // burns the navigator every tick for nothing.
        this.goalSelector.addGoal(1, new LookAtPlayerGoal(this, Player.class, 32.0F));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_NODES, TOTAL_NODES);
    }

    public int getNodes() {
        return this.entityData.get(DATA_NODES);
    }

    private void setNodes(int nodes) {
        this.entityData.set(DATA_NODES, Math.max(0, nodes));
    }

    /** The core is only reachable once every node is off it. */
    public boolean isCoreExposed() {
        return getNodes() <= 0;
    }

    /**
     * The whole boss, in one method.
     *
     * <p>While nodes remain, damage is <em>refused</em> unless it is an explosion or fire.
     * Refusing rather than reducing is deliberate: a player who chips it for 0.5 a swing
     * concludes the boss is a damage sponge and keeps swinging. A player whose hits visibly
     * do nothing goes looking for the reason, which is the intended experience.
     */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        // Out-of-world damage and /kill always land, or an unkillable boss can wedge a world.
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return super.hurt(source, amount);
        }
        if (isCoreExposed()) {
            return super.hurt(source, amount);
        }

        boolean breaksNode = source.is(DamageTypeTags.IS_EXPLOSION) || source.is(DamageTypeTags.IS_FIRE);
        if (!breaksNode) {
            deflect();
            return false;
        }

        setNodes(getNodes() - 1);
        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.EXPLOSION,
                    this.getX(), this.getY() + 1.5, this.getZ(), 8, 1.2, 1.0, 1.2, 0.0);
            serverLevel.playSound(null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.SCULK_SHRIEKER_SHRIEK, this.getSoundSource(), 2.0F, 0.7F);
        }
        if (isCoreExposed()) {
            exposeCore();
        }
        // The blow spent itself on the node. It does not also come off the core, or the
        // node phase would just be a slower health bar.
        return false;
    }

    /**
     * The bar stops counting nodes and starts counting health. Only the colour and the
     * overlay are set here — the value itself comes from {@link #barProgress()} on the next
     * tick, so there is exactly one place that decides what the bar reads.
     */
    private void exposeCore() {
        bossEvent.setColor(BossEvent.BossBarColor.RED);
        bossEvent.setOverlay(BossEvent.BossBarOverlay.PROGRESS);
    }

    private void deflect() {
        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.ENCHANTED_HIT,
                    this.getX(), this.getY() + 1.5, this.getZ(), 10, 1.0, 1.0, 1.0, 0.0);
            serverLevel.playSound(null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.SHIELD_BLOCK, this.getSoundSource(), 1.0F, 0.5F);
        }
    }

    /**
     * The bar counts nodes while they last, then switches to counting health.
     *
     * <p>This is the whole reason {@link BossMonster#barProgress()} is a hook rather than a
     * hardcoded health fraction: the Failed Vision's bar is answering a different question
     * for the first half of the fight.
     */
    @Override
    protected float barProgress() {
        return isCoreExposed() ? super.barProgress() : getNodes() / (float) TOTAL_NODES;
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();

        if (laserCooldown > 0) {
            laserCooldown--;
        }
        if (spawnCooldown > 0) {
            spawnCooldown--;
        }

        if (spawnCooldown <= 0) {
            spawnCooldown = SPAWN_COOLDOWN_TICKS;
            spawnFranks();
        }

        LivingEntity target = this.getTarget();
        if (target == null || !target.isAlive() || laserCooldown > 0) {
            return;
        }
        if (this.distanceToSqr(target) > LASER_RANGE * LASER_RANGE || !this.hasLineOfSight(target)) {
            return;
        }
        laserCooldown = LASER_COOLDOWN_TICKS;
        // Armour-piercing, like Jesselton's second phase: you cannot out-gear a laser, you
        // have to break line of sight, which is what the pillars in the chamber are for.
        ArtsBeam.fire(this, target, ModDamageTypes.VOID_ARTS, LASER_DAMAGE, ParticleTypes.GLOW);
        this.playSound(SoundEvents.BEACON_ACTIVATE, 2.0F, 1.6F);
    }

    private void spawnFranks() {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        int live = serverLevel.getEntitiesOfClass(Frank.class,
                this.getBoundingBox().inflate(ARENA_RADIUS)).size();
        if (live >= MAX_LIVE_FRANKS) {
            return;
        }

        for (int i = 0; i < FRANKS_PER_WAVE && live + i < MAX_LIVE_FRANKS; i++) {
            Frank frank = ModEntities.FRANK.get().create(serverLevel);
            if (frank == null) {
                return;
            }
            double angle = this.random.nextDouble() * Math.PI * 2.0;
            double distance = 3.0 + this.random.nextDouble() * 4.0;
            frank.moveTo(this.getX() + Math.cos(angle) * distance,
                    this.getY(),
                    this.getZ() + Math.sin(angle) * distance,
                    this.random.nextFloat() * 360.0F, 0.0F);
            frank.finalizeSpawn(serverLevel, serverLevel.getCurrentDifficultyAt(frank.blockPosition()),
                    MobSpawnType.MOB_SUMMONED, null, null);
            frank.setTarget(this.getTarget());
            serverLevel.addFreshEntity(frank);
        }
    }

    /** Guaranteed, for the same reason Jesselton's key is: it gates the rest of the chapter. */
    @Override
    protected void dropCustomDeathLoot(DamageSource source, int looting, boolean recentlyHitByPlayer) {
        super.dropCustomDeathLoot(source, looting, recentlyHitByPlayer);
        this.spawnAtLocation(new ItemStack(ModItems.DOROTHYS_NEURAL_PROCESSOR.get()));
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt(NBT_NODES, getNodes());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        // An absent key means a boss saved before nodes existed, or a /summon — either way
        // a full set is the right answer, not zero.
        setNodes(tag.contains(NBT_NODES) ? tag.getInt(NBT_NODES) : TOTAL_NODES);
        if (isCoreExposed()) {
            exposeCore();
        }
    }

    // ── It is furniture ───────────────────────────────────────────────────────
    // The bar, the despawn rules and isPushable are BossMonster's. This one is not:
    // Jesselton can legitimately be blown across a room, and this cannot.

    /** Rooted in the floor: nothing about it should slide, drift or get carried by a piston. */
    @Override
    public void push(double x, double y, double z) {
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.SCULK_CATALYST_BLOOM;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.SLIME_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.WARDEN_DEATH;
    }
}
