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
 * Failure — the attempt that did not take, still walking.
 *
 * <p>The weakest of the three Medium-bearers and the one you meet in numbers: fast, brittle,
 * no armour at all. It is the mob that makes carrying a Medium feel cheap, which is the job
 * it has to do for the other two to feel like anything.
 *
 * <p>Hunched and lopsided in the model — one arm dragging, one stunted — so that at a glance
 * across a room you can tell it from the {@link DvReplica} standing next to it without reading
 * a health bar. See {@code geo/entity/dv_failure.geo.json}.
 */
public class DvFailure extends GeoMonster {

    public DvFailure(EntityType<? extends DvFailure> type, Level level) {
        super(type, level);
    }

    /** Defaults only; {@code EntityStats} overwrites all six from {@code MobStats} on join. */
    public static AttributeSupplier.Builder attributes() {
        return Monster.createMonsterAttributes()
                // Three hits from a stone sword: a tax on standing still, not a fight.
                .add(Attributes.MAX_HEALTH, 24.0)
                // Faster than a walking player, slower than a sprinting one, so you can leave.
                .add(Attributes.MOVEMENT_SPEED, 0.30)
                .add(Attributes.ATTACK_DAMAGE, 4.0)
                .add(Attributes.FOLLOW_RANGE, 24.0)
                .add(Attributes.ARMOR, 0.0)
                // Shoving a wall of these apart is the counterplay to their only real threat.
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.0);
    }

    /**
     * Dropped in code rather than from a loot table; there are no entity loot tables in the
     * mod at all, so what a mob leaves behind is decided in the file you tune it in. The cost
     * is honouring Looting by hand.
     */
    @Override
    protected void dropCustomDeathLoot(DamageSource source, int looting, boolean recentlyHitByPlayer) {
        super.dropCustomDeathLoot(source, looting, recentlyHitByPlayer);
        this.spawnAtLocation(new ItemStack(ModItems.MEDIUM.get(), 1 + this.random.nextInt(1 + looting)));
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.ZOMBIE_VILLAGER_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ZOMBIE_VILLAGER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ZOMBIE_VILLAGER_DEATH;
    }
}
