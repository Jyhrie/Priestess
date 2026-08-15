package com.jyhrie.priestess.config;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * The minibosses — {@code config/priestess/miniboss.toml}.
 *
 * <p>Its own file rather than a section of {@link BossStats}, for the same reason
 * {@code minibosses/} is its own package: a miniboss is a statement about where a fight sits in
 * a movement. It has the bar and the permanence of a boss at a third of the scale, and it gates
 * nothing. Keeping the two apart means tuning the fight that ends a dungeon never scrolls past
 * the fight that sits halfway down it.
 *
 * <p>One entry today. That is fine — the file is cheap, and the alternative is moving The First
 * to Talk out of {@link BossStats} later, once there are three of them and the mixing has
 * already caused a mistake.
 *
 * <p>See {@link Stats} for the six keys, the bounds, and how any of this reaches a live entity.
 */
public final class MinibossStats {

    public static final ForgeConfigSpec SPEC;

    public static final Stats.Block SV_THE_FIRST_TO_TALK;

    /**
     * The First to Talk's speed below half health. Applied as an {@code AttributeModifier} over
     * the configured base rather than as a base write, because {@code EntityStats} rewrites base
     * movement speed every time the mob joins the world and would undo it.
     */
    public static final ForgeConfigSpec.DoubleValue FIRST_TO_TALK_ENRAGED_SPEED;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.comment(Stats.header(
                       "The minibosses — a bar and no despawning, at a third of a boss's scale."))
               .push("miniboss");

        builder.comment("The First to Talk — Under Tides. Knockback resistance is deliberately",
                        "short of 1: full immunity is what marks the two real bosses out, and",
                        "spending it here flattens the difference.")
               .push("sv_the_first_to_talk");
        SV_THE_FIRST_TO_TALK = Stats.attributes(builder, MinibossStats::isLoaded,
                120.0, 0.26, 9.0, 40.0, 4.0, 0.7);
        FIRST_TO_TALK_ENRAGED_SPEED = builder
                .comment("Movement speed below half health. Keep it under a sprinting player, or",
                         "enraging removes the option of leaving rather than shortening the fight.")
                .defineInRange("enragedMovementSpeed", 0.32, 0.0, Stats.SPEED_LIMIT);
        builder.pop();

        builder.pop();   // miniboss

        SPEC = builder.build();
    }

    public static boolean isLoaded() {
        return SPEC.isLoaded();
    }

    private MinibossStats() {
    }
}
