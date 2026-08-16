package com.jyhrie.priestess.entity.mobs.dorothysvision;

import com.jyhrie.priestess.entity.GeoMonster;
import com.jyhrie.priestess.item.ModItems;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Replica — the attempt that took, and came out wrong in the way a copy does.
 *
 * <p>The middle of the three Medium-bearers and the baseline the other two are read against:
 * a plain melee mob with plain numbers, roughly a zombie that hits harder. Nothing about it
 * is interesting on purpose — {@link DvFailure} is the swarm and {@link DvBionic} is the wall,
 * and neither of those means anything without something ordinary standing between them.
 *
 * <p>The model is the clean, symmetric, entirely unremarkable humanoid of the set, which is
 * the whole of the character it has: it is the one that looks like it worked. See
 * {@code geo/entity/dv_replica.geo.json}.
 */
public class DvReplica extends GeoMonster {

    public DvReplica(EntityType<? extends DvReplica> type, Level level) {
        super(type, level);
    }

    /** Defaults only; {@code EntityStats} overwrites all six from {@code MobStats} on join. */
    public static AttributeSupplier.Builder attributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 34.0)
                .add(Attributes.MOVEMENT_SPEED, 0.26)
                .add(Attributes.ATTACK_DAMAGE, 6.0)
                .add(Attributes.FOLLOW_RANGE, 32.0)
                // Enough that unarmoured is a bad idea and armoured is fine. The Medium is
                // not meant to be gated behind gear, only behind bothering.
                .add(Attributes.ARMOR, 2.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.0);
    }

    /** One Medium, plus Looting. See {@link DvFailure#dropCustomDeathLoot} for why it is here. */
    @Override
    protected void dropCustomDeathLoot(DamageSource source, int looting, boolean recentlyHitByPlayer) {
        super.dropCustomDeathLoot(source, looting, recentlyHitByPlayer);
        this.spawnAtLocation(new ItemStack(ModItems.MEDIUM.get(), 1 + this.random.nextInt(1 + looting)));
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.ALLAY_AMBIENT_WITHOUT_ITEM;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ALLAY_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ALLAY_DEATH;
    }
}
