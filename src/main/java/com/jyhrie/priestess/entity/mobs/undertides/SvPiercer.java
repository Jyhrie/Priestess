package com.jyhrie.priestess.entity.mobs.undertides;

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
 * Piercer — a spike with just enough body behind it to carry the spike.
 *
 * <p>A glass cannon: it hits harder than the {@link SvReaper} and dies to almost anything,
 * which makes it the mob you have to notice rather than the mob you have to fight. Sixteen
 * health is two hits with an iron sword; eleven damage is most of an unarmoured player's
 * bar.
 *
 * <h2>It does not actually pierce yet</h2>
 * The name promises armour-piercing and this delivers raw damage instead — armour reduces
 * it like anything else. Making it literal is a damage type in the
 * {@code minecraft:bypasses_armor} tag, exactly as {@code void_arts} is for Jesselton's
 * second phase, plus an override of {@code doHurtTarget} to use it. That was left out
 * deliberately rather than forgotten: a new damage type comes with a death message and a
 * name for the thing doing the piercing, and naming it is a design call rather than a
 * wiring one. Until then this is a mob that hits hard, not a mob that ignores your gear.
 *
 * <p>Note that the fix is not free balance-wise. Eleven armour-piercing damage would be the
 * hardest hit in the mod by a wide margin; the number should come down when the tag
 * goes on.
 */
public class SvPiercer extends GeoMonster {

    public SvPiercer(EntityType<? extends SvPiercer> type, Level level) {
        super(type, level);
    }

    /**
     * Defaults only. {@code EntityStats} overwrites all six of these from
     * {@code config/priestess/mob.toml} as it joins the world, so editing a number
     * here alone changes nothing — change it in {@code MobStats} too.
     */
    public static AttributeSupplier.Builder attributes() {
        return Monster.createMonsterAttributes()
                // Deliberately frail. See the class note — this is meant to be a threat you
                // answer by killing it first, not one you out-tank.
                .add(Attributes.MAX_HEALTH, 16.0)
                .add(Attributes.MOVEMENT_SPEED, 0.28)
                .add(Attributes.ATTACK_DAMAGE, 11.0)
                .add(Attributes.FOLLOW_RANGE, 32.0)
                .add(Attributes.ARMOR, 0.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.0);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.GUARDIAN_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.GUARDIAN_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.GUARDIAN_DEATH;
    }
}
