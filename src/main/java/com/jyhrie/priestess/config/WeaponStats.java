package com.jyhrie.priestess.config;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * The three weapons — {@code config/priestess/weapon.toml}.
 *
 * <p>The one of the four that is not a creature, so it does not take {@link Stats}'s six
 * attribute keys. Each weapon table takes two:
 *
 * <ul>
 *   <li>{@code attackDamage} — added to the player's own base 1, so the tooltip shows this
 *       plus one.</li>
 *   <li>{@code attackSpeed} — an offset from the player's base of 4.0 attacks a second, so it
 *       is negative. It also sets the rate the left-click ability fires at, which is one
 *       swing's worth of ticks.</li>
 * </ul>
 *
 * <p><b>Ability damage is mostly a fraction of {@code attackDamage}</b>, and of the stack's
 * <em>real</em> attack damage rather than the number here — so Sharpness and any other modifier
 * on the weapon raises what it throws too. Raise {@code attackDamage} to make a whole weapon hit
 * harder; change a fraction to change the shape of its kit. The one exception is the Maelstrom,
 * which is flat: the vortex outlives the swing that opened it.
 *
 * <p>This file is the one piece of a weapon living outside the sealed {@code weapons/} package,
 * because the four balance configs belong together more than this belongs beside the item
 * classes. Nothing here imports {@code weapons/}, so the compartment still holds in the
 * direction that matters — deleting the folder means deleting this file too.
 */
public final class WeaponStats {

    public static final ForgeConfigSpec SPEC;

    public static final Stats.Weapon TEMPLATE_WEAPON_STATS;
    public static final Stats.Weapon DEVILS_DEVASTATION;
    public static final Stats.Weapon LAEVATAIN;
    public static final Stats.Weapon AEGIR_GREATSPEAR;

    /** Devil's Devastation: what each of the five projectiles carries, as a fraction. */
    public static final ForgeConfigSpec.DoubleValue DEVILS_PROJECTILE_FRACTION;
    /** Flat bonus a pitchfork carries over a scythe, added after the fraction. */
    public static final ForgeConfigSpec.DoubleValue DEVILS_PITCHFORK_BONUS;

    public static final ForgeConfigSpec.DoubleValue LAEVATAIN_SWEEP_FRACTION;
    public static final ForgeConfigSpec.DoubleValue LAEVATAIN_MOLTEN_GIANT_FRACTION;
    public static final ForgeConfigSpec.DoubleValue LAEVATAIN_TWILIGHT_FRACTION;

    public static final ForgeConfigSpec.DoubleValue AEGIR_TIDE_FRACTION;
    public static final ForgeConfigSpec.DoubleValue AEGIR_UNDERTOW_FRACTION;
    /** Maelstrom grinds for a flat amount rather than a fraction — the vortex is not the spear. */
    public static final ForgeConfigSpec.DoubleValue AEGIR_MAELSTROM_DAMAGE_PER_SECOND;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.comment(
                "The three weapons, and what their abilities hit for.",
                "",
                "  attackDamage   added to the player's own base 1, so the number the tooltip",
                "                 shows is this plus one.",
                "  attackSpeed    an offset from the player's base of 4.0 attacks a second, so it",
                "                 is negative. -2.0 leaves 2.0 attacks a second; -3.0 leaves 1.0.",
                "                 It also sets the rate the left-click ability fires at, which is",
                "                 one swing's worth of ticks.",
                "",
                "ABILITY DAMAGE IS MOSTLY A FRACTION OF attackDamage, and of the stack's real",
                "attack damage rather than the number here — so Sharpness and any other modifier",
                "on the weapon raises what it throws too. Raise attackDamage to make a whole",
                "weapon hit harder; change a fraction to change the shape of its kit.",
                "",
                "Changes here take effect immediately, with no restart: the modifiers are rebuilt",
                "from this file every time anything asks a weapon what it does.")
               .push("weapon");

        builder.comment("Template weapon — scaffolding, not a weapon anyone is meant to hold.",
                        "Kept alongside TemplateWeaponItem so a new weapon starts from a block",
                        "that already parses rather than from memory. Copy both, rename both.")
               .push("template_weapon");
        TEMPLATE_WEAPON_STATS = weapon(builder, 1.0, -1.0);
        builder.pop();

        builder.comment("Devil's Devastation — throws a fan of five on every swing: three scythes",
                        "and two pitchforks. Ported from Lethality.")
               .push("devils_devastation");
        DEVILS_DEVASTATION = weapon(builder, 15.0, -2.0);
        DEVILS_PROJECTILE_FRACTION = builder
                .comment("What one projectile in the fan carries. Five of them land, so the fan's",
                         "total is well over a single swing even at half.")
                .defineInRange("projectileDamageFraction", 0.5, 0.0, Stats.FRACTION_LIMIT);
        DEVILS_PITCHFORK_BONUS = builder
                .comment("Flat bonus a pitchfork carries over a scythe, added after the fraction.")
                .defineInRange("pitchforkDamageBonus", 2.0, 0.0, Stats.DAMAGE_LIMIT);
        builder.pop();

        builder.comment("Laevatain — Surtr's greatsword. Three fire abilities on the three click",
                        "inputs. The slowest weapon here; the damage and the sweep pay for it.")
               .push("laevatain");

        LAEVATAIN = weapon(builder, 18.0, -1.6);
        LAEVATAIN_SWEEP_FRACTION = builder
                .comment("Left click, the sweep. Keep this at or below 1.0: a mob the player",
                         "actually connected with is inside its hurt-immunity window when the",
                         "sweep resolves, and vanilla ignores a second hit that is not larger",
                         "than the one still on the clock. Above 1.0 the aimed target starts",
                         "taking both, which is not what the ability is for.")
                .defineInRange("sweepDamageFraction", 0.75, 0.0, Stats.FRACTION_LIMIT);
        LAEVATAIN_MOLTEN_GIANT_FRACTION = builder
                .comment("Right click, the line. Scaled by how far the cast was charged.")
                .defineInRange("moltenGiantDamageFraction", 1.5, 0.0, Stats.FRACTION_LIMIT);
        LAEVATAIN_TWILIGHT_FRACTION = builder
                .comment("Shift + right click, the cone. Also scaled by charge.")
                .defineInRange("twilightDamageFraction", 1.25, 0.0, Stats.FRACTION_LIMIT);
        builder.pop();

        builder.comment("Aegir Greatspear — a reach weapon whose left click throws the lance.")
               .push("aegir_greatspear");
        AEGIR_GREATSPEAR = weapon(builder, 15.0, -2.8);
        AEGIR_TIDE_FRACTION = builder
                .comment("Left click, the thrown lance.")
                .defineInRange("tideDamageFraction", 0.9, 0.0, Stats.FRACTION_LIMIT);
        AEGIR_UNDERTOW_FRACTION = builder
                .comment("Right click, the 5x5x5 pull in front.")
                .defineInRange("undertowDamageFraction", 1.2, 0.0, Stats.FRACTION_LIMIT);
        AEGIR_MAELSTROM_DAMAGE_PER_SECOND = builder
                .comment("Shift + right click, the whirlpool. Flat rather than a fraction: the",
                         "vortex is a thing left standing in the world for eight seconds and not",
                         "the spear swinging. Applied once a second, so this number is literally",
                         "what the tooltip quotes.")
                .defineInRange("maelstromDamagePerSecond", 5.0, 0.0, Stats.DAMAGE_LIMIT);
        builder.pop();

        builder.pop();   // weapon

        SPEC = builder.build();
    }

    public static boolean isLoaded() {
        return SPEC.isLoaded();
    }

    private static Stats.Weapon weapon(ForgeConfigSpec.Builder builder, double damage, double speed) {
        return Stats.weapon(builder, WeaponStats::isLoaded, damage, speed);
    }

    private WeaponStats() {
    }
}
