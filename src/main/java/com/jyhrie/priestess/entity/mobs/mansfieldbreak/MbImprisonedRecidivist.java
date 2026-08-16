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
 * Imprisoned Recidivist — the one who has been inside longest and is worst for it.
 *
 * <p>The {@link MbImprisonedPugilist} scaled up in every direction: half again as tall, twice
 * the health, twice the damage, and heavy enough that hitting it does not reliably buy you
 * the step backwards you were counting on. Everything else about it is the same mob, which is
 * deliberate — it should read as "that one, but worse", not as a different fight.
 *
 * <p>The knockback resistance is the part that actually changes how it plays. A Pugilist can
 * be walked backwards indefinitely; this cannot, so a corridor with one in it has to be
 * fought through rather than kited out of. It is slower than the other two to pay for that.
 */
public class MbImprisonedRecidivist extends GeoMonster {

    public MbImprisonedRecidivist(EntityType<? extends MbImprisonedRecidivist> type, Level level) {
        super(type, level);
        this.xpReward = 12;
    }

    /** Defaults only; {@code EntityStats} overwrites all six from {@code MobStats} on join. */
    public static AttributeSupplier.Builder attributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 45.0)
                // Slower than a walking player, so the threat is meeting one indoors.
                .add(Attributes.MOVEMENT_SPEED, 0.21)
                .add(Attributes.ATTACK_DAMAGE, 8.0)
                .add(Attributes.FOLLOW_RANGE, 32.0)
                .add(Attributes.ARMOR, 4.0)
                // Not 1.0 — that is for bosses. It staggers, it just does not stagger enough
                // to be walked backwards down a cell block.
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.5);
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

    /** Deeper than the other two, so a Recidivist is audible before it is visible. */
    @Override
    public float getVoicePitch() {
        return 0.7F;
    }
}
