package com.jyhrie.priestess.oripathy;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;

/**
 * {@code /oripathy} — the only way to read or change the infection, since it is deliberately
 * invisible to the player. Op-only (permission level 2); it is an admin and testing tool,
 * not something a survival player is meant to consult.
 *
 * <pre>
 * /oripathy get [target]            defaults to yourself
 * /oripathy set &lt;targets&gt; &lt;value&gt;   1..10000
 * /oripathy add &lt;targets&gt; &lt;amount&gt;  negative to treat; result is clamped
 * </pre>
 */
public class OripathyCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("oripathy")
                .requires(source -> source.hasPermission(Commands.LEVEL_GAMEMASTERS))

                .then(Commands.literal("get")
                        .executes(context -> get(context.getSource(), context.getSource().getPlayerOrException()))
                        .then(Commands.argument("target", EntityArgument.player())
                                .executes(context -> get(context.getSource(),
                                        EntityArgument.getPlayer(context, "target")))))

                .then(Commands.literal("set")
                        .then(Commands.argument("targets", EntityArgument.players())
                                .then(Commands.argument("value",
                                                IntegerArgumentType.integer(Oripathy.MIN, Oripathy.MAX))
                                        .executes(context -> set(context.getSource(),
                                                EntityArgument.getPlayers(context, "targets"),
                                                IntegerArgumentType.getInteger(context, "value"))))))

                .then(Commands.literal("add")
                        .then(Commands.argument("targets", EntityArgument.players())
                                .then(Commands.argument("amount",
                                                IntegerArgumentType.integer(-Oripathy.MAX, Oripathy.MAX))
                                        .executes(context -> add(context.getSource(),
                                                EntityArgument.getPlayers(context, "targets"),
                                                IntegerArgumentType.getInteger(context, "amount")))))));
    }

    private static int get(CommandSourceStack source, ServerPlayer target) {
        int value = Oripathy.of(target);
        source.sendSuccess(() -> Component.literal(
                target.getName().getString() + " has " + value + " oripathy (" + stage(value) + ")"), false);
        return value;
    }

    private static int set(CommandSourceStack source, Collection<ServerPlayer> targets, int value) {
        targets.forEach(target -> Oripathy.set(target, value));
        source.sendSuccess(() -> Component.literal(
                "Set oripathy to " + value + " for " + describe(targets)), true);
        return targets.size();
    }

    private static int add(CommandSourceStack source, Collection<ServerPlayer> targets, int amount) {
        targets.forEach(target -> Oripathy.add(target, amount));
        source.sendSuccess(() -> Component.literal(
                (amount < 0 ? "Removed " + -amount + " oripathy from " : "Added " + amount + " oripathy to ")
                        + describe(targets)), true);
        return targets.size();
    }

    /** Named so an op can tell at a glance which threshold a value falls under. */
    private static String stage(int value) {
        if (value >= Oripathy.LETHAL) return "terminal";
        if (value >= Oripathy.BLINDNESS) return "blindness, weakness II, slowness II";
        if (value >= Oripathy.WEAKNESS) return "weakness II, slowness II";
        if (value >= Oripathy.SLOWNESS) return "slowness II";
        return "asymptomatic";
    }

    private static String describe(Collection<ServerPlayer> targets) {
        return targets.size() == 1
                ? targets.iterator().next().getName().getString()
                : targets.size() + " players";
    }

    private OripathyCommand() {
    }
}
