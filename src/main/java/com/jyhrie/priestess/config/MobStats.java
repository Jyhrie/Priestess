package com.jyhrie.priestess.config;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * The trash mobs — {@code config/priestess/mob.toml}.
 *
 * <p>Twelve of them, grouped by dungeon in the order the chapter meets them, which is also the
 * order they appear in the file. This is the one of the four that is tuned as a <em>set</em>: a
 * dungeon's difficulty is the shape of its whole roster, not any single entry, so the file is
 * meant to be read top to bottom.
 *
 * <p>See {@link Stats} for the six keys, the bounds, and how any of this reaches a live entity.
 */
public final class MobStats {

    public static final ForgeConfigSpec SPEC;

    public static final Stats.Block ORIGINIUM_SLUG;

    public static final Stats.Block DV_FAILURE;
    public static final Stats.Block DV_REPLICA;
    public static final Stats.Block DV_BIONIC;

    public static final Stats.Block MB_IMPRISONED_PUGILIST;
    public static final Stats.Block MB_IMPRISONED_RECIDIVIST;
    public static final Stats.Block MB_IMPRISONED_SNIPER;

    public static final Stats.Block SV_CRAWLER;
    public static final Stats.Block SV_RUNNER;
    public static final Stats.Block SV_PIERCER;
    public static final Stats.Block SV_REAPER;
    public static final Stats.Block SV_SPITTER;

    /** The Sniper's arrow, before the velocity multiplier {@code shoot} applies on top. */
    public static final ForgeConfigSpec.DoubleValue SNIPER_ARROW_DAMAGE;
    public static final ForgeConfigSpec.DoubleValue SPITTER_SPIT_DAMAGE;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.comment(Stats.header(
                       "The trash mobs, grouped by dungeon in the order the chapter meets them."))
               .push("mob");

        builder.comment("Originium Slug — vermin, and the only mob here belonging to no dungeon.")
               .push("originium_slug");
        ORIGINIUM_SLUG = block(builder, 8.0, 0.32, 2.0, 24.0, 0.0, 0.0);
        builder.pop();

        // ── Dorothy's Vision ──────────────────────────────────────────────────

        builder.push("dv_failure");
        DV_FAILURE = block(builder, 24.0, 0.30, 4.0, 24.0, 0.0, 0.0);
        builder.pop();

        builder.push("dv_replica");
        DV_REPLICA = block(builder, 34.0, 0.26, 6.0, 32.0, 2.0, 0.0);
        builder.pop();

        builder.push("dv_bionic");
        DV_BIONIC = block(builder, 60.0, 0.21, 9.0, 32.0, 8.0, 0.6);
        builder.pop();

        // ── Mansfield Break ───────────────────────────────────────────────────

        builder.push("mb_imprisoned_pugilist");
        MB_IMPRISONED_PUGILIST = block(builder, 22.0, 0.25, 4.0, 32.0, 1.0, 0.0);
        builder.pop();

        builder.push("mb_imprisoned_recidivist");
        MB_IMPRISONED_RECIDIVIST = block(builder, 45.0, 0.21, 8.0, 32.0, 4.0, 0.5);
        builder.pop();

        builder.comment("Imprisoned Sniper. It shoots and never closes, so attackDamage below is",
                        "unused unless something gives it a melee goal — arrowDamage is the one",
                        "that matters. followRange is deliberately longer than its 16-block firing",
                        "range, so it notices you before it engages.")
               .push("mb_imprisoned_sniper");
        MB_IMPRISONED_SNIPER = block(builder, 20.0, 0.25, 2.0, 32.0, 0.0, 0.0);
        SNIPER_ARROW_DAMAGE = builder
                .comment("Arrow damage before the velocity multiplier the shot applies on top.",
                         "A vanilla arrow is 2.0.")
                .defineInRange("arrowDamage", 3.0, 0.0, Stats.DAMAGE_LIMIT);
        builder.pop();

        // ── Under Tides ───────────────────────────────────────────────────────

        builder.push("sv_crawler");
        SV_CRAWLER = block(builder, 12.0, 0.31, 3.0, 20.0, 1.0, 0.0);
        builder.pop();

        builder.push("sv_runner");
        SV_RUNNER = block(builder, 18.0, 0.34, 4.0, 32.0, 0.0, 0.0);
        builder.pop();

        builder.push("sv_piercer");
        SV_PIERCER = block(builder, 16.0, 0.28, 11.0, 32.0, 0.0, 0.0);
        builder.pop();

        builder.push("sv_reaper");
        SV_REAPER = block(builder, 40.0, 0.22, 10.0, 32.0, 3.0, 0.4);
        builder.pop();

        builder.comment("Spitter. Like the Sniper it has no melee goal, so attackDamage is held",
                        "non-zero only so that anything which later gives it one does not find a",
                        "mob that cannot hurt anybody. spitDamage is the real attack.")
               .push("sv_spitter");
        SV_SPITTER = block(builder, 20.0, 0.20, 2.0, 28.0, 2.0, 0.0);
        SPITTER_SPIT_DAMAGE = builder
                .comment("Hitscan, so there is nothing to dodge — cover is the counterplay.")
                .defineInRange("spitDamage", 5.0, 0.0, Stats.DAMAGE_LIMIT);
        builder.pop();

        builder.pop();   // mob

        SPEC = builder.build();
    }

    public static boolean isLoaded() {
        return SPEC.isLoaded();
    }

    private static Stats.Block block(ForgeConfigSpec.Builder builder,
                                     double health, double speed, double damage,
                                     double range, double armour, double knockbackResistance) {
        return Stats.attributes(builder, MobStats::isLoaded,
                health, speed, damage, range, armour, knockbackResistance);
    }

    private MobStats() {
    }
}
