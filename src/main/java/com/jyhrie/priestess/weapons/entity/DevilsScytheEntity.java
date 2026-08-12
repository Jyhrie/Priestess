package com.jyhrie.priestess.weapons.entity;

import com.jyhrie.priestess.weapons.ModWeapons;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

/**
 * The wide arm of Devil's Devastation's fan: three of these go out per swing, one down the
 * crosshair and one 25° to either side.
 *
 * <p>Carries half the sword's attack damage each and the longer of the two burns. All of the
 * behaviour is {@link DevilsProjectile}; this class is the entity type, the burn, and nothing
 * else.
 */
public class DevilsScytheEntity extends DevilsProjectile {

    /** Seconds of vanilla fire on a target this sweeps over. */
    private static final int BURN_SECONDS = 20;

    public DevilsScytheEntity(EntityType<? extends DevilsScytheEntity> type, Level level) {
        super(type, level);
    }

    public DevilsScytheEntity(Level level, double x, double y, double z, float damage) {
        super(ModWeapons.DEVILS_SCYTHE.get(), level, x, y, z, damage);
    }

    @Override
    protected void applyOnHit(LivingEntity target) {
        // Lethality also stacks Terramity's Nyxium Fire here, at 300 ticks / amplifier 2.
        // Terramity is not a dependency of this mod, so that half is stubbed — see
        // docs/LETHALITY WEAPONS.md, "Terramity", for the one-line reinstatement.
        //
        // target.addEffect(new MobEffectInstance(
        //         TerramityModMobEffects.NYXIUM_FIRE.get(), 300, 2), this.getOwner());
        target.setSecondsOnFire(BURN_SECONDS);
    }
}
