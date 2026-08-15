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

    /**
     * Defaults only. {@code EntityStats} overwrites all six of these from
     * {@code config/priestess/mob.toml} as it joins the world, so editing a number
     * here alone changes nothing — change it in {@code MobStats} too.
     */
    public static AttributeSupplier.Builder attributes() {
        return Monster.createMonsterAttributes()
                // Dies to a stone sword in three hits. It is a tax on standing still, not a
                // fight, and the danger is only ever how many of them there are.
                .add(Attributes.MAX_HEALTH, 24.0)
                // Faster than a player walking, slower than one sprinting — the same deal the
                // slug offers, so you can always leave.
                .add(Attributes.MOVEMENT_SPEED, 0.30)
                .add(Attributes.ATTACK_DAMAGE, 4.0)
                .add(Attributes.FOLLOW_RANGE, 24.0)
                .add(Attributes.ARMOR, 0.0)
                // Knocks back freely. Being able to shove a wall of these apart is the
                // counterplay to their only real threat.
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.0);
    }

    /**
     * The Medium, dropped in code rather than from a loot table.
     *
     * <p>There are no entity loot tables in the mod at all — the bosses drop their keys the
     * same way, for the same reason: what a mob leaves behind is a fact about the mob, and
     * putting it here means it is decided in the file you are already reading when you tune
     * the mob. Looting is honoured by hand, which is the cost of that.
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
