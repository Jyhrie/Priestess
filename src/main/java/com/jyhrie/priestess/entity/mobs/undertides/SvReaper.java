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
 * Reaper — tall, slow, and the hardest hit in Under Tides that can also take one back.
 *
 * <p>Sal Viento's heavy, and the counterweight to the {@link SvRunner}: one of them makes
 * standing still lethal, the other makes running past lethal. Ten damage is more than the Imprisoned
 * Recidivist deals, though the {@link SvPiercer} beside it hits harder still — the Reaper is
 * the one that hits hard <em>and</em> survives being hit back, which is what makes it the mob
 * you disengage from rather than the mob you race.
 *
 * <p>Arms nearly to the floor, which is the whole silhouette. See
 * {@code geo/entity/sv_reaper.geo.json}.
 */
public class SvReaper extends GeoMonster {

    public SvReaper(EntityType<? extends SvReaper> type, Level level) {
        super(type, level);
        this.xpReward = 10;
    }

    /**
     * Defaults only. {@code EntityStats} overwrites all six of these from
     * {@code config/priestess/mob.toml} as it joins the world, so editing a number
     * here alone changes nothing — change it in {@code MobStats} too.
     */
    public static AttributeSupplier.Builder attributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 40.0)
                // Slower than a walking player. Everything it does has to be walked into.
                .add(Attributes.MOVEMENT_SPEED, 0.22)
                .add(Attributes.ATTACK_DAMAGE, 10.0)
                .add(Attributes.FOLLOW_RANGE, 32.0)
                .add(Attributes.ARMOR, 3.0)
                // Staggers, but not enough to be walked backwards indefinitely.
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.4);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.WARDEN_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.DROWNED_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.DROWNED_DEATH;
    }

    /** Deep, so the thing worth backing away from announces itself. */
    @Override
    public float getVoicePitch() {
        return 0.6F;
    }
}
