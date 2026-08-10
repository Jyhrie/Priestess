package com.jyhrie.priestess.world.dimension;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Tells an operator, on login, where this world's one-per-world structures actually are.
 *
 * <h2>Why this exists</h2>
 * A {@link SingleInRegionStructurePlacement} anchor is derived from the world seed, so it is
 * in a different place in every save and written down nowhere. {@code /locate} does find it,
 * but only within its own hundred-chunk search radius, and an anchor can be several thousand
 * blocks away — so in practice the coordinates existed only in the server log, which is a
 * poor place to keep something you need while you are standing in the game.
 *
 * <h2>Read from the world, not from {@link ModStructures}</h2>
 * The sets come from {@link ChunkGeneratorStructureState#possibleStructureSets()} — what this
 * dimension's generator actually loaded — rather than from the configuration block that
 * generated the JSON. Those agree today, and this way they cannot quietly stop agreeing: a
 * datapack that overrides a structure set, or a stale {@code src/generated} tree that was not
 * re-run through {@code runData}, is reported as it is rather than as it was declared.
 *
 * <h2>Operators only</h2>
 * Gated on permission level {@value #REQUIRED_PERMISSION_LEVEL}. The seeded placement exists
 * so that finding Mansfield is something a player does rather than something a wiki tells
 * them; handing every player three coordinates on login would spend exactly the thing
 * {@code TerraAnchors} was written to create. This is a builder's tool.
 */
public class AnchorReport {

    /** The usual bar for gamemaster commands — {@code /locate} and {@code /tp} sit here too. */
    private static final int REQUIRED_PERMISSION_LEVEL = 2;

    /** One structure's anchor, resolved and ready to print. */
    private record Anchor(String name, int blockX, int blockY, int blockZ) {}

    /**
     * Operators, and anyone already in creative.
     *
     * <p>Creative is here because permission level alone gets the common case wrong: a
     * single-player world made without cheats gives even its host level 0, so a builder
     * testing the mod in their own world would be told nothing. Someone in creative can
     * already fly to the coordinate and spawn what is in the chest — there is no secret left
     * for the permission check to be protecting.
     */
    private static boolean maySee(ServerPlayer player) {
        return player.hasPermissions(REQUIRED_PERMISSION_LEVEL) || player.isCreative();
    }

    @SubscribeEvent
    public static void announceOnLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !maySee(player)) {
            return;
        }
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }
        ServerLevel terra = server.getLevel(ModDimensions.TERRA_KEY);
        if (terra == null) {
            // Terra failed to load — a broken datapack, most likely. That is loud elsewhere.
            return;
        }

        List<Anchor> anchors;
        try {
            anchors = resolve(terra);
        } catch (IllegalStateException e) {
            // TerraAnchors throws when a region has no ground at all. Worth saying here,
            // because the alternative is a login that silently reports nothing and a player
            // who concludes the feature is broken rather than the map.
            player.sendSystemMessage(Component.literal("Terra anchors unavailable: " + e.getMessage())
                    .withStyle(ChatFormatting.RED));
            return;
        }
        if (anchors.isEmpty()) {
            return;
        }

        player.sendSystemMessage(Component.literal("Terra anchors (seed " + terra.getSeed() + "):")
                .withStyle(ChatFormatting.GOLD));
        for (Anchor anchor : anchors) {
            player.sendSystemMessage(line(anchor));
        }
    }

    /**
     * Every unique structure in Terra, with the ground height at its anchor.
     *
     * <p>The scan behind {@link com.jyhrie.priestess.world.terra.TerraAnchors} is cached per
     * region and has almost always been run already — the structures resolved their own
     * anchors when Terra's spawn chunks generated. A login that does trigger it pays for one
     * pass over the map, which is the same cost world creation pays and is not worth
     * deferring off-thread to avoid.
     */
    private static List<Anchor> resolve(ServerLevel terra) {
        ChunkGeneratorStructureState state = terra.getChunkSource().getGeneratorState();
        long seed = state.getLevelSeed();

        List<Anchor> anchors = new ArrayList<>();
        for (Holder<StructureSet> holder : state.possibleStructureSets()) {
            StructureSet set = holder.value();
            if (!(set.placement() instanceof SingleInRegionStructurePlacement placement)) {
                continue;
            }
            ChunkPos anchor = placement.anchorFor(seed);
            int blockX = anchor.getMiddleBlockX();
            int blockZ = anchor.getMiddleBlockZ();
            // The generator's own height sample rather than a heightmap lookup, so this does
            // not force the chunk to generate — and so the Y quoted is the one the structure
            // was placed against, WORLD_SURFACE_WG being what ModStructures projects onto.
            int blockY = terra.getChunkSource().getGenerator().getBaseHeight(
                    blockX, blockZ, Heightmap.Types.WORLD_SURFACE_WG, terra, state.randomState());
            anchors.add(new Anchor(nameOf(set), blockX, blockY, blockZ));
        }
        // Registry iteration order is not the declaration order in ModStructures and is not
        // worth relying on either way; alphabetical at least does not move between runs.
        anchors.sort(Comparator.comparing(Anchor::name));
        return anchors;
    }

    /** A {@code single_in_region} set holds exactly one structure, so the first entry is it. */
    private static String nameOf(StructureSet set) {
        return set.structures().stream()
                .findFirst()
                .flatMap(entry -> entry.structure().unwrapKey())
                .map(ResourceKey::location)
                .map(ResourceLocation::getPath)
                .orElse("unknown");
    }

    /**
     * SUGGEST_COMMAND rather than RUN_COMMAND: a misclick in the chat log should not teleport
     * someone out of what they were building. The command lands in the chat box and waits.
     */
    private static Component line(Anchor anchor) {
        String command = String.format("/execute in %s run tp @s %d %d %d",
                ModDimensions.TERRA_KEY.location(), anchor.blockX(), anchor.blockY() + 1, anchor.blockZ());
        Component coordinates = Component
                .literal(String.format("[%d, %d, %d]", anchor.blockX(), anchor.blockY(), anchor.blockZ()))
                .withStyle(style -> style
                        .withColor(ChatFormatting.AQUA)
                        .withUnderlined(true)
                        .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, command))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                Component.literal(command))));
        return Component.literal("  " + anchor.name() + " ")
                .withStyle(ChatFormatting.GRAY)
                .append(coordinates);
    }
}
