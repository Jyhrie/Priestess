package com.jyhrie.priestess.progression;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * {@code /dungeon} — read and rewrite dungeon clear flags. Op-only (permission level 2).
 *
 * <p>Purely a testing tool, and the mechanics need one: both the lockdown and the flight ban
 * key off a flag that gameplay only ever sets in <em>one</em> direction. Without this the
 * only way to re-seal a dungeon is to shut the world down and edit
 * {@code data/priestess_dungeon_progress.dat} by hand, which makes the two features
 * effectively untestable.
 *
 * <pre>
 * /dungeon list [target]                   what is cleared, and which storage is live
 * /dungeon clear &lt;dungeon|all&gt; [targets]   mark cleared
 * /dungeon seal  &lt;dungeon|all&gt; [targets]   mark uncleared
 * /dungeon placed count                    how many placed-block exemptions this dimension holds
 * /dungeon placed clear                    drop them all
 * </pre>
 *
 * <h2>What {@code targets} means depends on the config</h2>
 * In per-player mode it is whose record to write, defaulting to the sender. In shared mode
 * there is only one record, so targets are accepted and ignored — and the command says so
 * rather than letting an operator believe they changed one person's progress. Which mode is
 * live is printed by {@code list}, because guessing wrong is the obvious way to waste ten
 * minutes wondering why nothing happened.
 *
 * <h2>Why the dungeon argument is a literal per dungeon</h2>
 * Built in a loop from {@link Dungeon#values()}, so a new dungeon needs no change here and
 * gets tab-completion and validation for free. A string argument would need a suggestion
 * provider and its own error handling to reach the same place.
 */
public final class DungeonCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("dungeon")
                .requires(source -> source.hasPermission(Commands.LEVEL_GAMEMASTERS));

        root.then(Commands.literal("list")
                .executes(context -> list(context.getSource(), context.getSource().getPlayerOrException()))
                .then(Commands.argument("target", EntityArgument.player())
                        .executes(context -> list(context.getSource(),
                                EntityArgument.getPlayer(context, "target")))));

        root.then(setNode("clear", true));
        root.then(setNode("seal", false));

        root.then(Commands.literal("placed")
                .then(Commands.literal("count")
                        .executes(context -> placedCount(context.getSource())))
                .then(Commands.literal("clear")
                        .executes(context -> placedClear(context.getSource()))));

        dispatcher.register(root);
    }

    /** One subtree — {@code clear} or {@code seal} — with a literal per dungeon plus "all". */
    private static LiteralArgumentBuilder<CommandSourceStack> setNode(String name, boolean cleared) {
        LiteralArgumentBuilder<CommandSourceStack> node = Commands.literal(name);

        for (Dungeon dungeon : Dungeon.values()) {
            node.then(Commands.literal(dungeon.getSerializedName())
                    .executes(context -> apply(context.getSource(),
                            List.of(context.getSource().getPlayerOrException()), List.of(dungeon), cleared))
                    .then(Commands.argument("targets", EntityArgument.players())
                            .executes(context -> apply(context.getSource(),
                                    EntityArgument.getPlayers(context, "targets"), List.of(dungeon), cleared))));
        }

        node.then(Commands.literal("all")
                .executes(context -> apply(context.getSource(),
                        List.of(context.getSource().getPlayerOrException()), List.of(Dungeon.values()), cleared))
                .then(Commands.argument("targets", EntityArgument.players())
                        .executes(context -> apply(context.getSource(),
                                EntityArgument.getPlayers(context, "targets"), List.of(Dungeon.values()), cleared))));

        return node;
    }

    private static int apply(CommandSourceStack source, Collection<ServerPlayer> targets,
                             List<Dungeon> dungeons, boolean cleared) {
        int changed = 0;
        for (ServerPlayer target : targets) {
            for (Dungeon dungeon : dungeons) {
                if (DungeonProgress.set(target, dungeon, cleared)) {
                    changed++;
                }
            }
            if (DungeonProgress.isShared()) {
                // One record — writing it once per target would just count the same change
                // several times and read as though something more had happened.
                break;
            }
        }

        String what = dungeons.size() == 1 ? dungeons.get(0).getSerializedName() : "all dungeons";
        String who = DungeonProgress.isShared()
                ? "the world (shared progress is on, so targets were ignored)"
                : describe(targets);
        int total = changed;
        source.sendSuccess(() -> Component.literal(
                (cleared ? "Cleared " : "Sealed ") + what + " for " + who
                        + " (" + total + " flag" + (total == 1 ? "" : "s") + " changed)"), true);
        return changed;
    }

    private static int list(CommandSourceStack source, ServerPlayer target) {
        Set<Dungeon> cleared = DungeonProgress.clearedFor(target);

        String mode = DungeonProgress.isShared() ? "shared (world-wide)" : "per player";
        String body = List.of(Dungeon.values()).stream()
                .map(dungeon -> {
                    String mark = cleared.contains(dungeon) ? "cleared" : "SEALED";
                    // A dungeon that cannot be sealed reads as cleared for a reason that has
                    // nothing to do with progress, and saying so saves a bug report.
                    String why = dungeon.canBeSealed() ? "" : " (nothing seals it — see Dungeon.java)";
                    return "  " + dungeon.getSerializedName() + ": " + mark + why;
                })
                .collect(Collectors.joining("\n"));

        source.sendSuccess(() -> Component.literal(
                "Dungeon progress for " + target.getName().getString()
                        + " — storage: " + mode + "\n" + body), false);
        return cleared.size();
    }

    private static int placedCount(CommandSourceStack source) {
        ServerLevel level = source.getLevel();
        int count = PlacedBlocks.get(level).size();
        source.sendSuccess(() -> Component.literal(
                count + " placed-block exemption" + (count == 1 ? "" : "s") + " in "
                        + level.dimension().location()), false);
        return count;
    }

    private static int placedClear(CommandSourceStack source) {
        ServerLevel level = source.getLevel();
        int dropped = PlacedBlocks.get(level).forgetAll();
        source.sendSuccess(() -> Component.literal(
                "Dropped " + dropped + " placed-block exemption" + (dropped == 1 ? "" : "s")
                        + " in " + level.dimension().location()), true);
        return dropped;
    }

    private static String describe(Collection<ServerPlayer> targets) {
        return targets.size() == 1
                ? targets.iterator().next().getName().getString()
                : targets.size() + " players";
    }

    private DungeonCommand() {
    }
}
