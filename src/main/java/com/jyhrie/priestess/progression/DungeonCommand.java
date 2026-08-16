package com.jyhrie.priestess.progression;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * {@code /dungeon} — read and rewrite dungeon clear flags. Op-only (permission level 2).
 *
 * <p>A testing tool, and the mechanics need one: gameplay only ever sets the clear flag in
 * <em>one</em> direction, so without this the only way to re-seal a dungeon is to shut the
 * world down and edit {@code data/priestess_dungeon_progress.dat} by hand.
 *
 * <pre>
 * /dungeon list [target]                   what is cleared, and which storage is live
 * /dungeon clear &lt;dungeon|all&gt; [targets]   mark cleared
 * /dungeon seal  &lt;dungeon|all&gt; [targets]   mark uncleared
 * </pre>
 *
 * <p>{@code targets} depends on the config: in per-player mode it is whose record to write,
 * and in shared mode there is one record, so targets are accepted and ignored — and the
 * command says so rather than letting an operator believe otherwise. {@code list} prints which
 * mode is live.
 *
 * <p>The dungeon argument is a literal per dungeon, built in a loop from
 * {@link Dungeon#values()}, so a new dungeon gets tab-completion and validation for free.
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
                // One record: writing it per target would count the same change several times.
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
                .map(dungeon -> "  " + dungeon.getSerializedName() + ": "
                        + (cleared.contains(dungeon) ? "cleared" : "SEALED")
                        + " — " + gates(dungeon))
                .collect(Collectors.joining("\n"));

        source.sendSuccess(() -> Component.literal(
                "Dungeon progress for " + target.getName().getString()
                        + " — storage: " + mode + "\n" + body), false);
        return cleared.size();
    }

    /**
     * How many block types this dungeon's flag holds shut. Printed per line because "SEALED"
     * alone is not an answer: a dungeon with nothing tagged to it gates nothing, which from
     * the inside looks exactly like a broken lockdown.
     */
    private static String gates(Dungeon dungeon) {
        if (!dungeon.hasClearCondition()) {
            return "nothing clears it, so it always reads cleared (see Dungeon.java)";
        }
        int blocks = BuiltInRegistries.BLOCK.getTag(dungeon.sealedBlocks())
                .map(HolderSet::size)
                .orElse(0);
        return blocks == 0
                ? "nothing yet (no blocks tagged sealed_by/" + dungeon.getSerializedName() + ")"
                : blocks + " block type" + (blocks == 1 ? "" : "s");
    }

    private static String describe(Collection<ServerPlayer> targets) {
        return targets.size() == 1
                ? targets.iterator().next().getName().getString()
                : targets.size() + " players";
    }

    private DungeonCommand() {
    }
}
