package com.jyhrie.priestess;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * The mod's server config — {@code serverconfig/priestess-server.toml} inside a world save.
 *
 * <p><b>SERVER, not COMMON.</b> Every value here decides what the server permits, so it has
 * to travel with the world rather than with the installation: a shared world and a
 * single-player world of the same pack should be able to disagree about whether progression
 * is shared, and a client must never be the one holding the answer.
 */
public final class PriestessConfig {

    public static final ForgeConfigSpec SPEC;

    // ── Dungeon lockdown ──────────────────────────────────────────────────────

    public static final ForgeConfigSpec.BooleanValue LOCKDOWN_ENABLED;
    public static final ForgeConfigSpec.BooleanValue SHARED_PROGRESS;

    // ── Flight restriction ────────────────────────────────────────────────────

    public static final ForgeConfigSpec.BooleanValue FLIGHT_BAN_ENABLED;
    public static final ForgeConfigSpec.BooleanValue FLIGHT_BAN_EXEMPTS_CREATIVE;
    public static final ForgeConfigSpec.BooleanValue FLIGHT_BAN_STOPS_ELYTRA;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.comment(
                "Dungeon lockdown: while a dungeon is uncleared, its rooms cannot be mined.",
                "Blocks a player placed themselves are always exempt, so scaffolding and",
                "bridging still work — the rule is 'you cannot dig through the dungeon', not",
                "'you cannot build in it'.")
               .push("lockdown");

        LOCKDOWN_ENABLED = builder
                .comment("Master switch. False turns the whole mechanic off.")
                .define("enabled", true);

        SHARED_PROGRESS = builder
                .comment("true  — one world-wide record. Anyone clearing a dungeon opens it for everyone.",
                         "false — every player carries their own record, saved with the player and",
                         "        kept through death. A dungeon a player has not personally cleared",
                         "        stays sealed for them even on a server where someone else has.")
                .define("sharedProgress", true);

        builder.pop();

        builder.comment(
                "Flight restriction: whole biomes refuse flight until the dungeon that gates",
                "them has been cleared. See the note on jetpacks in FlightRestriction.java —",
                "this covers everything that flies through the vanilla ability flags, which is",
                "most but not all of it.")
               .push("flight");

        FLIGHT_BAN_ENABLED = builder
                .comment("Master switch. False turns the whole mechanic off.")
                .define("enabled", true);

        FLIGHT_BAN_EXEMPTS_CREATIVE = builder
                .comment("true  — players in Creative keep their flight in restricted biomes.",
                         "false — Creative is grounded too.",
                         "",
                         "Defaults to true, which is a build-and-test convenience rather than a",
                         "design position: grounding yourself in Creative while laying out a dungeon",
                         "is miserable. Set it false for the strict reading. Spectator is always",
                         "exempt either way and is the escape hatch when this is false.")
                .define("exemptCreative", true);

        FLIGHT_BAN_STOPS_ELYTRA = builder
                .comment("Also cancel elytra gliding in restricted biomes.",
                         "On by default because an elytra is otherwise the largest hole in this:",
                         "it does not touch the ability flags at all, so nothing else here stops it.")
                .define("stopsElytra", true);

        builder.pop();

        SPEC = builder.build();
    }

    private PriestessConfig() {
    }
}
