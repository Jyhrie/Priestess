package com.jyhrie.priestess.world.dimension;

import com.jyhrie.priestess.Priestess;
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
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.structures.JigsawStructure;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * Every structure is declared once in the CONFIGURATION BLOCK below; the three
 * bootstrap methods then derive the template pool, the structure and the structure
 * set from that single declaration. See "Adding a structure" in README.md.
 */
public class ModStructures {

    private record StructureData(
            String structureId,
            List<String> nbtVariants,
            ResourceKey<Biome> targetBiome,
            int spacing,
            int separation,
            int salt,
            HeightProvider heightProvider,
            boolean projectToHeightmap
    ) {}

    private static final List<StructureData> STRUCTURES_REGISTRY = new ArrayList<>();

    // =========================================================================
    // CONFIGURATION BLOCK — add new structures here
    // =========================================================================
    static {
        registerStructure(
                "infy_ice_spike",
                List.of("ice_spike_medium_normal"),   // one entry per .nbt variant; picked at random
                ModBiomes.INFY_ICEFIELDS,
                1,                                     // spacing (chunks)
                0,                                     // separation (chunks, must be < spacing)
                14325892,                              // salt — must be unique per structure
                UniformHeight.of(VerticalAnchor.absolute(-12), VerticalAnchor.absolute(-4)),
                true                                   // project onto the heightmap
        );
    }
    // =========================================================================

    private static void registerStructure(String structureId, List<String> nbtVariants, ResourceKey<Biome> biome,
                                          int spacing, int separation, int salt,
                                          HeightProvider heightProvider, boolean projectToHeightmap) {
        STRUCTURES_REGISTRY.add(new StructureData(structureId, nbtVariants, biome, spacing, separation, salt, heightProvider, projectToHeightmap));
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

        for (StructureData data : STRUCTURES_REGISTRY) {
            context.register(setKey(data.structureId()), new StructureSet(
                    structures.getOrThrow(structureKey(data.structureId())),
                    new RandomSpreadStructurePlacement(
                            data.spacing(),
                            data.separation(),
                            RandomSpreadType.LINEAR,
                            data.salt()
                    )
            ));
        }
    }
}
