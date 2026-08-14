package com.jyhrie.priestess.weapons;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Movement a weapon does to something that is not the player holding it.
 *
 * <p>Shared scaffolding, the same as {@link WeaponTiers} and {@link WeaponText}: it lives at the
 * package root because all three of Aegir Greatspear's abilities pull, and two of them do it
 * from an entity rather than from the item. Putting it on the item instead would make the
 * projectile and the whirlpool import the weapon that threw them.
 */
public final class WeaponPhysics {

    /**
     * Drags {@code target} toward a point.
     *
     * <p><b>{@code hurtMarked} is the load-bearing line.</b> A velocity written on the server is
     * not sent to clients on its own — {@code ServerEntity} only pushes a velocity packet for an
     * entity whose {@code hurtMarked} flag is set, which vanilla sets when something is knocked
     * back. Without it the server moves the mob and the client keeps rendering it where it was,
     * so the pull happens but nobody watching can see it until the next position sync snaps the
     * mob across the room.
     *
     * <p>This <em>adds</em> to the existing velocity rather than replacing it, so a pull does not
     * cancel a mob's fall or its knockback. That is what keeps repeated pulls feeling like a
     * current rather than like teleportation.
     *
     * @param strength blocks per tick of velocity added, before any resistance the mob has
     */
    public static void pullTowards(LivingEntity target, Vec3 destination, double strength) {
        Vec3 toDestination = destination.subtract(target.position());
        double distance = toDestination.length();
        // Dead centre: there is no direction to pull in, and normalising would divide by zero.
        if (distance < 1.0E-4) {
            return;
        }
        target.setDeltaMovement(target.getDeltaMovement().add(toDestination.scale(strength / distance)));
        target.hurtMarked = true;
    }

    private WeaponPhysics() {
    }
}
