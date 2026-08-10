package com.jyhrie.priestess.entity.mobs.mansfieldbreak;

import com.jyhrie.priestess.entity.GeoMonster;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

/**
 * Imprisoned Pugilist — an inmate who fights with his hands, and the plain one of the three.
 *
 * <p>A zombie in everything but name: same health, same reach, same speed, and the same
 * answer (back up and swing). It inherits {@link GeoMonster}'s goal set unchanged, so there
 * is genuinely nothing here but numbers and sounds — which is the point. Mansfield needs a
 * body to fill a corridor before it needs anything clever in one.
 *
 * <p>Slightly harder-hitting than a vanilla zombie and slightly faster, because a prison
 * break is a fight you are supposed to feel outnumbered in rather than one you are supposed
 * to lose to any single inmate.
 */
public class MbImprisonedPugilist extends GeoMonster {

    public MbImprisonedPugilist(EntityType<? extends MbImprisonedPugilist> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder attributes() {
        return Monster.createMonsterAttributes()
                // A zombie's 20, rounded up a little for the extra damage it deals.
                .add(Attributes.MAX_HEALTH, 22.0)
                // A zombie is 0.23. This is a shade quicker: an inmate who has just been let
                // out of a cell should not amble.
                .add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.ATTACK_DAMAGE, 4.0)
                .add(Attributes.FOLLOW_RANGE, 32.0)
                .add(Attributes.ARMOR, 1.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.0);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.ZOMBIE_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ZOMBIE_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ZOMBIE_DEATH;
    }
}
