package com.jyhrie.priestess.entity;

import com.jyhrie.priestess.damage.ModDamageTypes;
import com.jyhrie.priestess.oripathy.Oripathy;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

/**
 * Originium Slug — vermin, and the reason you cannot leave a base running unattended.
 *
 * <p>Weak and fast on their own. What makes them a problem is that they are attracted to
 * electromagnetic fields: any block that stores energy pulls them in from across the camp,
 * and once they are on it they eat the charge. Kill one next to the machine it was chewing
 * and it bursts, which corrodes whatever is standing nearby — including you.
 *
 * <p>"A machine" is defined entirely by Forge's energy capability and never by a mod name;
 * see {@link Machines} for why. In a pack with no tech mod in it the slug degrades cleanly
 * into an ordinary weak melee mob, which is the correct behaviour rather than a bug.
 */
public class OriginiumSlug extends Monster {

    /** How far the field reaches. Comfortably further than a slug will wander on its own. */
    public static final double MACHINE_SENSE_RADIUS = 12.0;
    /** How close it has to get before it starts feeding. */
    private static final double FEEDING_RANGE_SQR = 2.5 * 2.5;
    /** Energy eaten per second while feeding, per machine in range. */
    private static final int DRAIN_PER_SECOND = 200;

    /** Radius of the acid burst when one dies. */
    private static final double BURST_RADIUS = 3.0;
    private static final float BURST_DAMAGE = 4.0F;
    /** What the burst takes out of any machine it catches — the sting in killing them close in. */
    private static final int BURST_MACHINE_DRAIN = 5_000;
    /** Acid in an open wound. Small; the danger is standing in a nest of them, not one slug. */
    private static final int BURST_ORIPATHY = 25;

    public OriginiumSlug(EntityType<? extends OriginiumSlug> type, Level level) {
        super(type, level);
        // Vermin, not a hazard to navigate around: a slug that made players path around it
        // would be a much bigger deal than one worth 8 health.
        this.setPathfindingMalus(net.minecraft.world.level.pathfinder.BlockPathTypes.WATER, 8.0F);
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
        // Priority 2: below a player it can actually reach, above idle wandering. A slug
        // that ignored the person hitting it in favour of a battery would just be silly.
        this.goalSelector.addGoal(2, new FeedOnMachineGoal());
        this.goalSelector.addGoal(3, new RandomStrollGoal(this, 0.8));
        this.goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 8.0F));

        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getAmbientSound() {
        return SoundEvents.SILVERFISH_AMBIENT;
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.SLIME_HURT_SMALL;
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getDeathSound() {
        return SoundEvents.SLIME_SQUISH;
    }

    /**
     * The burst. Killing a slug is never free — it is only cheap when you do it away from
     * anything you care about, which is the entire mechanic.
     *
     * <p>It fires from {@code die} rather than from the death sound or a loot condition so
     * that it happens exactly once, on the server, however the slug was killed: melee, fall
     * damage, a turret, or another slug's burst chaining into it.
     */
    @Override
    public void die(DamageSource cause) {
        if (!this.level().isClientSide()) {
            burst();
        }
        super.die(cause);
    }

    private void burst() {
        Level level = this.level();
        BlockPos here = this.blockPosition();

        Machines.drainAround(level, here, BURST_RADIUS, BURST_MACHINE_DRAIN);

        for (LivingEntity caught : level.getEntitiesOfClass(LivingEntity.class,
                this.getBoundingBox().inflate(BURST_RADIUS))) {
            if (caught == this) {
                continue;
            }
            caught.hurt(ModDamageTypes.source(level, ModDamageTypes.ORIGINIUM_ACID, this), BURST_DAMAGE);
            // Acid, in a wound: this goes through infect, so Open Wounds bites on it.
            if (caught instanceof Player player) {
                Oripathy.infect(player, BURST_ORIPATHY);
            }
        }

        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.SNEEZE,
                    this.getX(), this.getY() + 0.2, this.getZ(), 40, 0.6, 0.3, 0.6, 0.05);
            serverLevel.playSound(null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.GENERIC_EXTINGUISH_FIRE, this.getSoundSource(), 1.0F, 0.6F);
        }
    }

    /**
     * Walk to the nearest charged machine and eat it.
     *
     * <p>The target is re-checked on a timer rather than every tick because finding it means
     * walking the block entities of up to four chunks; see {@link Machines}. The timer is
     * also what stops a slug flickering between two machines the same distance away.
     */
    private class FeedOnMachineGoal extends Goal {

        /** Ticks between searches while it has nothing to feed on. */
        private static final int SEARCH_INTERVAL = 40;

        @Nullable
        private BlockPos machine;
        private int cooldown;
        private int drainTimer;

        FeedOnMachineGoal() {
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (OriginiumSlug.this.getTarget() != null) {
                return false;
            }
            if (cooldown-- > 0) {
                return false;
            }
            cooldown = SEARCH_INTERVAL;
            machine = Machines.findCharged(OriginiumSlug.this.level(),
                    OriginiumSlug.this.blockPosition(), MACHINE_SENSE_RADIUS);
            return machine != null;
        }

        @Override
        public boolean canContinueToUse() {
            // Dropped the moment a player shows up, and the moment the machine runs dry —
            // a slug parked on an empty block is the failure mode this guards against.
            return machine != null
                    && OriginiumSlug.this.getTarget() == null
                    && Machines.anyCharged(OriginiumSlug.this.level(), machine, 2.0);
        }

        @Override
        public void start() {
            drainTimer = 0;
            moveToMachine();
        }

        @Override
        public void stop() {
            machine = null;
            OriginiumSlug.this.getNavigation().stop();
        }

        @Override
        public void tick() {
            if (machine == null) {
                return;
            }
            OriginiumSlug.this.getLookControl().setLookAt(
                    machine.getX() + 0.5, machine.getY() + 0.5, machine.getZ() + 0.5);

            if (OriginiumSlug.this.blockPosition().distSqr(machine) > FEEDING_RANGE_SQR) {
                if (OriginiumSlug.this.getNavigation().isDone()) {
                    moveToMachine();
                }
                return;
            }

            // Close enough. Drain once a second so the numbers a player sees on a machine's
            // gauge tick down visibly instead of vanishing in one frame.
            if (++drainTimer >= 20) {
                drainTimer = 0;
                int drained = Machines.drainAround(OriginiumSlug.this.level(), machine, 2.0, DRAIN_PER_SECOND);
                if (drained > 0 && OriginiumSlug.this.level() instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                            machine.getX() + 0.5, machine.getY() + 1.0, machine.getZ() + 0.5,
                            6, 0.3, 0.3, 0.3, 0.02);
                }
            }
        }

        private void moveToMachine() {
            if (machine != null) {
                OriginiumSlug.this.getNavigation().moveTo(
                        machine.getX() + 0.5, machine.getY(), machine.getZ() + 0.5, 1.0);
            }
        }
    }
}
