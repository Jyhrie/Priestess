package com.jyhrie.priestess.weapons.entity;

import com.jyhrie.priestess.weapons.ModWeapons;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

/**
 * The tight arm of Devil's Devastation's fan: two of these, 12.5° to either side of the
 * crosshair, inside the scythes.
 *
 * <p>Hits for two more than a scythe but burns for half as long — the pitchforks are the part
 * of the fan that rewards actually aiming, so they carry the damage and the scythes carry the
 * area. All of the behaviour is {@link DevilsProjectile}.
 */
public class DevilsPitchforkEntity extends DevilsProjectile {

    /** Seconds of vanilla fire on a target this sweeps over. Half the scythe's. */
    private static final int BURN_SECONDS = 10;

    public DevilsPitchforkEntity(EntityType<? extends DevilsPitchforkEntity> type, Level level) {
        super(type, level);
    }

    public DevilsPitchforkEntity(Level level, double x, double y, double z, float damage) {
        super(ModWeapons.DEVILS_PITCHFORK.get(), level, x, y, z, damage);
    }

    @Override
    protected void applyOnHit(LivingEntity target) {
        // As DevilsScytheEntity: Lethality stacks Nyxium Fire here too, at 150 ticks /
        // amplifier 1. Stubbed for the same reason — see docs/LETHALITY WEAPONS.md.
        //
        // target.addEffect(new MobEffectInstance(
        //         TerramityModMobEffects.NYXIUM_FIRE.get(), 150, 1), this.getOwner());
        target.setSecondsOnFire(BURN_SECONDS);
    }
}
