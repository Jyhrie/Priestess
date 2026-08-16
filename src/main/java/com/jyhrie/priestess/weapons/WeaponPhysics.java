package com.jyhrie.priestess.weapons;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Movement a weapon does to something that is not the player holding it.
 *
 * <p>At the package root rather than on the item, because two of Aegir Greatspear's three
 * abilities pull from an entity — putting it on the item would make the projectile and the
 * whirlpool import the weapon that threw them.
 */
public final class WeaponPhysics {

    /**
     * Drags {@code target} toward a point.
     *
     * <p><b>{@code hurtMarked} is the load-bearing line.</b> {@code ServerEntity} only pushes a
     * velocity packet for an entity whose {@code hurtMarked} flag is set, so without it the
     * server moves the mob while the client keeps rendering it where it was, until the next
     * position sync snaps it across the room.
     *
     * <p>Adds to the existing velocity rather than replacing it, so a pull does not cancel a
     * mob's fall or knockback.
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
