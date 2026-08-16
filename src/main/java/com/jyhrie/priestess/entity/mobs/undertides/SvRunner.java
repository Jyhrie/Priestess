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
 * Runner — the one that reaches you first, and the reason Sal Viento is fought moving.
 *
 * <p>The fastest mob in the mod, and the only one quicker than a sprinting player. That is
 * the whole design: everything else in Under Tides can be walked away from, and this cannot,
 * so it converts "leave" into "leave <em>now</em>, having dealt with this". It is made of
 * paper to pay for it — two hits from anything.
 *
 * <p>Pitched forward in the model so a stationary one still reads as mid-sprint. See
 * {@code geo/entity/sv_runner.geo.json}.
 */
public class SvRunner extends GeoMonster {

    public SvRunner(EntityType<? extends SvRunner> type, Level level) {
        super(type, level);
    }

    /** Defaults only; {@code EntityStats} overwrites all six from {@code MobStats} on join. */
    public static AttributeSupplier.Builder attributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 18.0)
                // A sprinting player is about 0.33. This is the one mob deliberately above
                // that line — see the class note.
                .add(Attributes.MOVEMENT_SPEED, 0.34)
                .add(Attributes.ATTACK_DAMAGE, 4.0)
                .add(Attributes.FOLLOW_RANGE, 32.0)
                .add(Attributes.ARMOR, 0.0)
                // Knocks back easily, which is the counterplay: it closes fast and it is
                // trivially interrupted once you turn around.
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.0);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.DROWNED_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.DROWNED_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.DROWNED_DEATH;
    }

    /** Higher than the rest of the roster, so a Runner is audible as the thing rushing you. */
    @Override
    public float getVoicePitch() {
        return 1.3F;
    }
}
