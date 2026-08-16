package com.jyhrie.priestess.config;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * The three bosses — {@code config/priestess/boss.toml}.
 *
 * <p>See {@link Stats} for the six keys, the bounds, and how any of this reaches a live entity.
 *
 * <p><b>COMMON</b>, so it belongs to the installation rather than a world and one edit retunes
 * every save. The cost is that it is not synced — on a server each side reads its own copy and
 * only the server's decides anything. See {@code docs/STATS.md}.
 */
public final class BossStats {

    public static final ForgeConfigSpec SPEC;

    public static final Stats.Block DV_AWAKEN;
    public static final Stats.Block MB_JESSELTON_WILLIAMS;
    public static final Stats.Block SV_BISHOP_QUINTUS;

    /** Jesselton's beam, phase one: ordinary kinetic damage that armour answers. */
    public static final ForgeConfigSpec.DoubleValue JESSELTON_PHASE_ONE_DAMAGE;
    /** Jesselton's beam, phase two: void arts, which bypass armour entirely. */
    public static final ForgeConfigSpec.DoubleValue JESSELTON_PHASE_TWO_DAMAGE;
    public static final ForgeConfigSpec.DoubleValue QUINTUS_BEAM_DAMAGE;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.comment(Stats.header("The three bosses — the fights that end a chapter."))
               .push("boss");

        builder.comment("Awaken — Dorothy's Vision. A skeleton on purpose: it hangs in the air,",
                        "watches you, and has no attack at all yet. Hence the two zeroes.")
               .push("dv_awaken");
        DV_AWAKEN = block(builder, 300.0, 0.0, 0.0, 48.0, 8.0, 1.0);
        builder.pop();

        builder.comment("Jesselton Williams — Mansfield Break. Two phases off one beam: below",
                        "half health it changes damage type to void arts, which bypass armour,",
                        "and stops being something looted riot gear answers. That is why phase",
                        "two is allowed to be the smaller number.")
               .push("mb_jesselton_williams");
        MB_JESSELTON_WILLIAMS = block(builder, 220.0, 0.28, 8.0, 48.0, 6.0, 1.0);
        JESSELTON_PHASE_ONE_DAMAGE = builder
                .comment("Beam damage above half health. Kinetic — armour subtracts from it.")
                .defineInRange("artsPhaseOneDamage", 9.0, 0.0, Stats.DAMAGE_LIMIT);
        JESSELTON_PHASE_TWO_DAMAGE = builder
                .comment("Beam damage below half health. Bypasses armour, so it lands in full.")
                .defineInRange("artsPhaseTwoDamage", 7.0, 0.0, Stats.DAMAGE_LIMIT);
        builder.pop();

        builder.comment("Bishop Quintus — Under Tides. Immobile, and its beam is the whole fight;",
                        "the melee number is unused because it has no melee goal.")
               .push("sv_bishop_quintus");
        SV_BISHOP_QUINTUS = block(builder, 400.0, 0.0, 0.0, 48.0, 10.0, 1.0);
        QUINTUS_BEAM_DAMAGE = builder
                .comment("Beam damage. Originium acid — no impact, and shields do not stop it.")
                .defineInRange("beamDamage", 8.0, 0.0, Stats.DAMAGE_LIMIT);
        builder.pop();

        builder.pop();   // boss

        SPEC = builder.build();
    }

    public static boolean isLoaded() {
        return SPEC.isLoaded();
    }

    private static Stats.Block block(ForgeConfigSpec.Builder builder,
                                     double health, double speed, double damage,
                                     double range, double armour, double knockbackResistance) {
        return Stats.attributes(builder, BossStats::isLoaded,
                health, speed, damage, range, armour, knockbackResistance);
    }

    private BossStats() {
    }
}
