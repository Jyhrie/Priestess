package com.jyhrie.priestess.entity.projectiles;

import com.jyhrie.priestess.damage.ModDamageTypes;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

/**
 * A hitscan Arts attack: it lands the instant it is fired and draws itself as a line of
 * particles.
 *
 * <p>Every ranged attacker in Columbia uses this rather than a projectile entity, and that
 * is a deliberate call. A projectile would need its own entity type, its own renderer and
 * its own network tracking, three files each, for something the player experiences as
 * "the ghost pointed at me and I took damage". Arts in Arknights are not arrows; they
 * arrive. The cost is that there is nothing to dodge once it is fired, so the counterplay
 * has to be line of sight and cover — which is why every caller checks
 * {@link Mob#hasLineOfSight} first and why the dungeons are built with pillars in them.
 *
 * <p>Damage is attributed to the mob, so kills are credited and the death message names it.
 */
public final class ArtsBeam {

    /** Particles drawn per block of beam. Enough to read as a line, few enough to be cheap. */
    private static final double PARTICLES_PER_BLOCK = 2.0;

    /**
     * Fires from {@code source}'s eyes at {@code target}'s chest.
     *
     * @return whether the target actually took damage — false if it was invulnerable or
     *         still in hurt-immunity from a previous hit, which callers use to decide
     *         whether to spend the attack cooldown.
     */
    public static boolean fire(Mob source, LivingEntity target, ResourceKey<DamageType> damageType,
                              float damage, ParticleOptions particle) {
        Vec3 from = source.getEyePosition();
        Vec3 to = target.position().add(0.0, target.getBbHeight() * 0.5, 0.0);

        if (source.level() instanceof ServerLevel serverLevel) {
            draw(serverLevel, from, to, particle);
        }
        return target.hurt(ModDamageTypes.source(source.level(), damageType, source), damage);
    }

    private static void draw(ServerLevel level, Vec3 from, Vec3 to, ParticleOptions particle) {
        Vec3 along = to.subtract(from);
        int steps = Math.max(1, (int) (along.length() * PARTICLES_PER_BLOCK));
        Vec3 step = along.scale(1.0 / steps);
        Vec3 at = from;
        for (int i = 0; i <= steps; i++) {
            // Count 1 with zero speed and zero spread: the particle has to sit exactly on
            // the line, or a wide beam reads as a cloud and stops pointing at anything.
            level.sendParticles(particle, at.x, at.y, at.z, 1, 0.0, 0.0, 0.0, 0.0);
            at = at.add(step);
        }
    }

    private ArtsBeam() {
    }
}
