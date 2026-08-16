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
 * <p><b>It does not actually pierce yet.</b> Armour reduces its damage like anything else.
 * Making it literal means a damage type in the {@code minecraft:bypasses_armor} tag plus an
 * override of {@code doHurtTarget} — left out because naming that damage type is a design
 * call. The attack damage should come down when the tag goes on; eleven armour-piercing would
 * be the hardest hit in the mod by a wide margin.
 */
public class SvPiercer extends GeoMonster {

    public SvPiercer(EntityType<? extends SvPiercer> type, Level level) {
        super(type, level);
    }

    /** Defaults only; {@code EntityStats} overwrites all six from {@code MobStats} on join. */
    public static AttributeSupplier.Builder attributes() {
        return Monster.createMonsterAttributes()
                // Frail on purpose: a threat you answer by killing it first, not out-tanking.
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
