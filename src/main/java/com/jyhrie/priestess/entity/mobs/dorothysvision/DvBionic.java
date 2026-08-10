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
 * Bionic — the attempt that was finished with hardware when the flesh ran out.
 *
 * <p>The heavy of the three Medium-bearers: slow, armoured, hits like a wall falling on you,
 * and mostly unmoved by being hit back. It is the one you cannot solve by walking backwards
 * swinging, because it does not stagger and it does not stop.
 *
 * <p>Bulkier than the other two and asymmetric with it — one oversized arm carrying most of
 * the mass. That silhouette is the warning: it is the only one of the three that reads as
 * "do not stand in front of this" before it has hit you. See {@code geo/entity/dv_bionic.geo.json}.
 */
public class DvBionic extends GeoMonster {

    public DvBionic(EntityType<? extends DvBionic> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder attributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 60.0)
                // Slower than a walking player, so it can always be disengaged from. Being
                // unable to knock it back is only fair if you can leave.
                .add(Attributes.MOVEMENT_SPEED, 0.21)
                .add(Attributes.ATTACK_DAMAGE, 9.0)
                .add(Attributes.FOLLOW_RANGE, 32.0)
                .add(Attributes.ARMOR, 8.0)
                // Not 1.0. A boss is immovable; this is heavy — you can still shove it, you
                // just cannot chain-stagger it the way you can a Failure.
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.6);
    }

    /**
     * Two Mediums, plus Looting — the only one of the three worth killing for the drop rather
     * than because it is in the way. See {@link DvFailure#dropCustomDeathLoot} for why the drop
     * is in code.
     */
    @Override
    protected void dropCustomDeathLoot(DamageSource source, int looting, boolean recentlyHitByPlayer) {
        super.dropCustomDeathLoot(source, looting, recentlyHitByPlayer);
        this.spawnAtLocation(new ItemStack(ModItems.MEDIUM.get(), 2 + this.random.nextInt(1 + looting)));
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.IRON_GOLEM_STEP;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.IRON_GOLEM_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.IRON_GOLEM_DEATH;
    }
}
