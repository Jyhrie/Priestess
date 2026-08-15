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
 * Crawler — knee-high, and never alone.
 *
 * <p>Sal Viento's answer to the Originium Slug: individually beneath notice, dangerous only
 * in the numbers it comes in. The point of it is the hitbox rather than the numbers — at
 * 0.7 blocks tall it sits under a normal swing arc, so a player fighting anything else in
 * the room is not incidentally clearing these too.
 *
 * <p>Four splayed legs and a long flat body, deliberately unlike the humanoid silhouette
 * the rest of the roster shares. See {@code geo/entity/sv_crawler.geo.json}.
 */
public class SvCrawler extends GeoMonster {

    public SvCrawler(EntityType<? extends SvCrawler> type, Level level) {
        super(type, level);
    }

    /**
     * Defaults only. {@code EntityStats} overwrites all six of these from
     * {@code config/priestess/mob.toml} as it joins the world, so editing a number
     * here alone changes nothing — change it in {@code MobStats} too.
     */
    public static AttributeSupplier.Builder attributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 12.0)
                .add(Attributes.MOVEMENT_SPEED, 0.31)
                .add(Attributes.ATTACK_DAMAGE, 3.0)
                // Short. It notices you late, which is what stops a whole room of them
                // arriving at once.
                .add(Attributes.FOLLOW_RANGE, 20.0)
                .add(Attributes.ARMOR, 1.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.0);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.SILVERFISH_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.SILVERFISH_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.SILVERFISH_DEATH;
    }
}
