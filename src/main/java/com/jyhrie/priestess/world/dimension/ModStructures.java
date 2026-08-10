package com.jyhrie.priestess.world.dimension;

import com.jyhrie.priestess.Priestess;
import com.jyhrie.priestess.world.terra.TerraAnchors;
import com.jyhrie.priestess.world.terra.TerraRegion;
import com.mojang.datafixers.util.Pair;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstapContext;
import net.minecraft.data.worldgen.Pools;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.heightproviders.HeightProvider;
import net.minecraft.world.level.levelgen.heightproviders.UniformHeight;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.TerrainAdjustment;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadType;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.structures.JigsawStructure;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * Every structure is declared once in the CONFIGURATION BLOCK below; the three bootstrap
 * methods then derive the template pool, the structure and the structure set from that
 * single declaration. See "Adding a structure" in README.md.
 *
 * <h2>Two kinds of structure</h2>
 * <dl>
 *   <dt><b>Scattered</b> — {@link #registerScattered}</dt>
 *   <dd>The ordinary kind. A spacing and a separation lay a jittered grid over the world
 *       and the structure appears all over the region. Ice spikes, camps, ruins: anything
 *       that is scenery.</dd>
 *
 *   <dt><b>Unique</b> — {@link #registerUnique}</dt>
 *   <dd>Exactly one in the entire world, anywhere inside a named region. There is no
 *       coordinate in this file and none in the generated JSON either: the JSON names a
 *       region, and {@link TerraAnchors} picks a spot in it from the world seed when the
 *       world is created. So every save puts the dungeon somewhere different, and every
 *       save puts it in the right biome.
 *       <p>This is what Movement I's three Columbia dungeons use. A progression gate that
 *       generated twice would hand out two Mansfield Master Keys and two Neural Processors,
 *       which is not a rarer version of the chapter — it is a different one.</dd>
 * </dl>
 *
 * <h2>Giving a dungeon its own region</h2>
 * The three Columbia dungeons currently share {@link TerraRegion#COLUMBIA}, which is why
 * each {@code registerUnique} call names a biome <em>and</em> a region. When a dungeon gets
 * a region painted for it — a Mansfield Prison region, a Rhine Lab region — the migration is
 * those two arguments on one line, plus the usual three edits any new zone needs
 * ({@code ModBiomes}, {@code TerraRegion}, a surface rule). Nothing here or in
 * {@link TerraAnchors} has to change: a region holding one structure instead of three is
 * simply {@code total = 1}, and {@code TerraAnchors} already relaxes its interior test for
 * regions too small to have one.
 */
public class ModStructures {

    /** How a structure decides which chunks it may appear in. */
    private sealed interface Placement {

        /** The usual jittered grid. {@code separation} must be less than {@code spacing}. */
        record Scattered(int spacing, int separation, int salt) implements Placement {}

        /** One per world, somewhere inside {@code region}. See {@link TerraAnchors}. */
        record Unique(TerraRegion region) implements Placement {}
    }

    private record StructureData(
            String structureId,
            List<String> nbtVariants,
            ResourceKey<Biome> targetBiome,
            Placement placement,
            HeightProvider heightProvider,
            boolean projectToHeightmap
    ) {}

    private static final List<StructureData> STRUCTURES_REGISTRY = new ArrayList<>();

    // =========================================================================
    // CONFIGURATION BLOCK — add new structures here
    // =========================================================================
    static {
        registerScattered(
                "infy_ice_spike",
                List.of("ice_spike_medium_normal"),   // one entry per .nbt variant; picked at random
                ModBiomes.INFY_ICEFIELD,
                1,                                     // spacing (chunks)
                0,                                     // separation (chunks, must be < spacing)
                14325892,                              // salt — must be unique per structure
                UniformHeight.of(VerticalAnchor.absolute(-12), VerticalAnchor.absolute(-4)),
                true                                   // project onto the heightmap
        );

        // ── Movement I — Columbia: Those who Take the Future ──────────────────
        // Three dungeons, one of each, somewhere in Columbia and at least a kilometre apart
        // — TerraAnchors handles both. Where in Columbia is the world seed's business, so
        // the order below is only the order they are dealt positions, not a route.

        // Sunk one block so the hull's floor course is buried rather than perched.
        registerUnique(
                "mansfield_state_prison",
                List.of("mansfield_state_prison"),
                ModBiomes.COLUMBIA,
                TerraRegion.COLUMBIA,
                UniformHeight.of(VerticalAnchor.absolute(-2), VerticalAnchor.absolute(-1)),
                true
        );

        // Dropped thirty blocks, which puts the lab properly underground and leaves the top
        // of the .nbt's own access shaft standing a course or two proud of the terrain. The
        // shaft is part of the structure for exactly this reason — see the build script.
        registerUnique(
                "dorothys_vision",
                List.of("dorothys_vision"),
                ModBiomes.COLUMBIA,
                TerraRegion.COLUMBIA,
                UniformHeight.of(VerticalAnchor.absolute(-30), VerticalAnchor.absolute(-29)),
                true
        );

        registerUnique(
                "rhine_lab_hq",
                List.of("rhine_lab_hq"),
                ModBiomes.COLUMBIA,
                TerraRegion.COLUMBIA,
                UniformHeight.of(VerticalAnchor.absolute(-2), VerticalAnchor.absolute(-1)),
                true
        );
    }
    // =========================================================================

    private static void registerScattered(String structureId, List<String> nbtVariants, ResourceKey<Biome> biome,
                                          int spacing, int separation, int salt,
                                          HeightProvider heightProvider, boolean projectToHeightmap) {
        STRUCTURES_REGISTRY.add(new StructureData(structureId, nbtVariants, biome,
                new Placement.Scattered(spacing, separation, salt), heightProvider, projectToHeightmap));
    }

    /**
     * @param biome  the biome the structure must land in, checked when the chunk generates
     * @param region the region the anchor is searched for in. In practice always the region
     *               that owns {@code biome} — they are two spellings of the same place, one
     *               for the generator and one for the map — but they are separate arguments
     *               because it is the <em>biome</em> that can veto placement and the
     *               <em>region</em> that decides where to look, and conflating them would
     *               hide a mismatch rather than let it be seen.
     */
    private static void registerUnique(String structureId, List<String> nbtVariants, ResourceKey<Biome> biome,
                                       TerraRegion region,
                                       HeightProvider heightProvider, boolean projectToHeightmap) {
        STRUCTURES_REGISTRY.add(new StructureData(structureId, nbtVariants, biome,
                new Placement.Unique(region), heightProvider, projectToHeightmap));
    }

    // --- Key derivation: one structureId maps to exactly these three keys ---

    private static ResourceKey<StructureTemplatePool> poolKey(String structureId) {
        return ResourceKey.create(Registries.TEMPLATE_POOL, new ResourceLocation(Priestess.MOD_ID, structureId + "_pool"));
    }

    private static ResourceKey<Structure> structureKey(String structureId) {
        return ResourceKey.create(Registries.STRUCTURE, new ResourceLocation(Priestess.MOD_ID, structureId));
    }

    private static ResourceKey<StructureSet> setKey(String structureId) {
        return ResourceKey.create(Registries.STRUCTURE_SET, new ResourceLocation(Priestess.MOD_ID, structureId + "_set"));
    }

    // --- Dynamic datagen builders ---

    public static void bootstrapPools(BootstapContext<StructureTemplatePool> context) {
        var emptyPool = context.lookup(Registries.TEMPLATE_POOL).getOrThrow(Pools.EMPTY);

        for (StructureData data : STRUCTURES_REGISTRY) {
            List<Pair<Function<StructureTemplatePool.Projection, ? extends StructurePoolElement>, Integer>> poolElements = new ArrayList<>();

            for (String nbtName : data.nbtVariants()) {
                poolElements.add(Pair.of(
                        StructurePoolElement.single(Priestess.MOD_ID + ":" + nbtName),
                        1   // weight
                ));
            }

            context.register(poolKey(data.structureId()), new StructureTemplatePool(
                    emptyPool,
                    poolElements,
                    StructureTemplatePool.Projection.RIGID
            ));
        }
    }

    public static void bootstrapStructures(BootstapContext<Structure> context) {
        var biomes = context.lookup(Registries.BIOME);
        var pools = context.lookup(Registries.TEMPLATE_POOL);

        for (StructureData data : STRUCTURES_REGISTRY) {
            context.register(structureKey(data.structureId()), new JigsawStructure(
                    new Structure.StructureSettings(
                            HolderSet.direct(biomes.getOrThrow(data.targetBiome())),
                            Map.of(),
                            GenerationStep.Decoration.SURFACE_STRUCTURES,
                            TerrainAdjustment.NONE
                    ),
                    pools.getOrThrow(poolKey(data.structureId())),
                    Optional.empty(),
                    1,                                          // max depth
                    data.heightProvider(),
                    data.projectToHeightmap(),
                    Optional.of(Heightmap.Types.WORLD_SURFACE_WG),
                    80                                          // max distance from center
            ));
        }
    }

    public static void bootstrapSets(BootstapContext<StructureSet> context) {
        var structures = context.lookup(Registries.STRUCTURE);
        Map<TerraRegion, Integer> perRegion = uniquesPerRegion();
        Map<TerraRegion, Integer> nextIndex = new EnumMap<>(TerraRegion.class);

        for (StructureData data : STRUCTURES_REGISTRY) {
            context.register(setKey(data.structureId()), new StructureSet(
                    structures.getOrThrow(structureKey(data.structureId())),
                    placementFor(data, perRegion, nextIndex)
            ));
        }
    }

    /**
     * How many unique structures each region has to find room for. Every one of them is
     * written into the JSON with the same {@code total}, which is what makes them ask
     * {@link TerraAnchors} the same question at runtime and therefore get answers that were
     * spread against each other rather than drawn independently.
     */
    private static Map<TerraRegion, Integer> uniquesPerRegion() {
        Map<TerraRegion, Integer> counts = new EnumMap<>(TerraRegion.class);
        for (StructureData data : STRUCTURES_REGISTRY) {
            if (data.placement() instanceof Placement.Unique unique) {
                counts.merge(unique.region(), 1, Integer::sum);
            }
        }
        return counts;
    }

    private static StructurePlacement placementFor(StructureData data,
                                                   Map<TerraRegion, Integer> perRegion,
                                                   Map<TerraRegion, Integer> nextIndex) {
        if (data.placement() instanceof Placement.Scattered scattered) {
            return new RandomSpreadStructurePlacement(
                    scattered.spacing(),
                    scattered.separation(),
                    RandomSpreadType.LINEAR,
                    scattered.salt()
            );
        }
        TerraRegion region = ((Placement.Unique) data.placement()).region();
        // Indices are handed out in declaration order, so re-ordering the configuration
        // block above re-shuffles which dungeon lands where in an existing seed. That is
        // fine, and worth knowing: these positions are stable per world, not per file.
        int index = nextIndex.merge(region, 1, Integer::sum) - 1;
        return new SingleInRegionStructurePlacement(region, index, perRegion.get(region));
    }
}
