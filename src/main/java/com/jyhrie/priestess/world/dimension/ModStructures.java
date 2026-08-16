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
 * <p>{@link #registerScattered} is the ordinary kind: a jittered grid, for scenery.
 * {@link #registerUnique} is exactly one in the entire world, anywhere inside a named region
 * — no coordinate appears in this file or the generated JSON, because {@link TerraAnchors}
 * picks a spot from the world seed at world creation. Progression gates need it: a dungeon
 * that generated twice would hand out two Master Keys, which is a different chapter rather
 * than a rarer one.
 *
 * <p>The Columbia dungeons currently share {@link TerraRegion#COLUMBIA}, which is why each
 * {@code registerUnique} call names a biome <em>and</em> a region. Giving one its own region
 * later is those two arguments plus the usual three edits any new zone needs; nothing here or
 * in {@link TerraAnchors} has to change.
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

    // Add new structures here.
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

        // Sunk one block so the hull's floor course is buried rather than perched.
        registerUnique(
                "mansfield_state_prison",
                List.of("mansfield_state_prison"),
                ModBiomes.COLUMBIA,
                TerraRegion.COLUMBIA,
                UniformHeight.of(VerticalAnchor.absolute(-2), VerticalAnchor.absolute(-1)),
                true
        );

        // Dropped thirty blocks to put the lab underground, leaving the top of the .nbt's own
        // access shaft standing a course or two proud of the terrain.
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

    private static void registerScattered(String structureId, List<String> nbtVariants, ResourceKey<Biome> biome,
                                          int spacing, int separation, int salt,
                                          HeightProvider heightProvider, boolean projectToHeightmap) {
        STRUCTURES_REGISTRY.add(new StructureData(structureId, nbtVariants, biome,
                new Placement.Scattered(spacing, separation, salt), heightProvider, projectToHeightmap));
    }

    /**
     * @param biome  the biome the structure must land in, checked when the chunk generates
     * @param region the region the anchor is searched for in. In practice always the region
     *               that owns {@code biome}, but kept separate because it is the biome that
     *               can veto placement and the region that decides where to look — conflating
     *               them would hide a mismatch.
     */
    private static void registerUnique(String structureId, List<String> nbtVariants, ResourceKey<Biome> biome,
                                       TerraRegion region,
                                       HeightProvider heightProvider, boolean projectToHeightmap) {
        STRUCTURES_REGISTRY.add(new StructureData(structureId, nbtVariants, biome,
                new Placement.Unique(region), heightProvider, projectToHeightmap));
    }

    private static ResourceKey<StructureTemplatePool> poolKey(String structureId) {
        return ResourceKey.create(Registries.TEMPLATE_POOL, new ResourceLocation(Priestess.MOD_ID, structureId + "_pool"));
    }

    private static ResourceKey<Structure> structureKey(String structureId) {
        return ResourceKey.create(Registries.STRUCTURE, new ResourceLocation(Priestess.MOD_ID, structureId));
    }

    private static ResourceKey<StructureSet> setKey(String structureId) {
        return ResourceKey.create(Registries.STRUCTURE_SET, new ResourceLocation(Priestess.MOD_ID, structureId + "_set"));
    }

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
     * How many unique structures each region has to find room for. They all go into the JSON
     * with the same {@code total}, which is what makes them ask {@link TerraAnchors} the same
     * question and get answers spread against each other rather than drawn independently.
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
        // Handed out in declaration order, so re-ordering the block above re-shuffles which
        // dungeon lands where in an existing seed: positions are stable per world, not per file.
        int index = nextIndex.merge(region, 1, Integer::sum) - 1;
        return new SingleInRegionStructurePlacement(region, index, perRegion.get(region));
    }
}
