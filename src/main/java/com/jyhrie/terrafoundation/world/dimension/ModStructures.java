package com.jyhrie.terrafoundation.world.dimension;

import com.jyhrie.terrafoundation.TerraFoundation;
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
import net.minecraft.world.level.levelgen.heightproviders.ConstantHeight;
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

public class ModStructures {

    // Updated record to hold a list of NBT variant names instead of just one ID string
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
    // CONFIGURATION BLOCK
    // =========================================================================
    static {
        // Your Ice Spike now bundles all 3 variants into a single spawning structure pool!
        registerStructure(
                "infy_ice_spike",
                List.of("ice_spike_medium_normal"),
                ModBiomes.INFY_ICEFIELDS,
                1,
                0,
                14325892,
                UniformHeight.of(VerticalAnchor.absolute(-12), VerticalAnchor.absolute(-4)),
                true
        );

        // In the future, if a structure only has 1 NBT file, you just do: List.of("my_single_file")
    }

    private static void registerStructure(String structureId, List<String> nbtVariants, ResourceKey<Biome> biome,
                                          int spacing, int separation, int salt,
                                          HeightProvider heightProvider, boolean projectToHeightmap) {
        STRUCTURES_REGISTRY.add(new StructureData(structureId, nbtVariants, biome, spacing, separation, salt, heightProvider, projectToHeightmap));
    }
    // =========================================================================


    // --- DYNAMIC DATAGEN BUILDERS ---

    public static void bootstrapPools(BootstapContext<StructureTemplatePool> context) { //
        var emptyPool = context.lookup(Registries.TEMPLATE_POOL).getOrThrow(Pools.EMPTY); //

        for (StructureData data : STRUCTURES_REGISTRY) { //
            ResourceKey<StructureTemplatePool> poolKey = ResourceKey.create(Registries.TEMPLATE_POOL, new ResourceLocation(TerraFoundation.MOD_ID, data.structureId() + "_pool")); //

            List<Pair<java.util.function.Function<StructureTemplatePool.Projection, ? extends StructurePoolElement>, Integer>> poolElements = new ArrayList<>(); //

            for (String nbtName : data.nbtVariants()) { //
                poolElements.add(Pair.of( //
                        StructurePoolElement.single(TerraFoundation.MOD_ID + ":" + nbtName), //
                        1 //
                ));
            }

            context.register(poolKey, new StructureTemplatePool( //
                    emptyPool, //
                    poolElements, //
                    StructureTemplatePool.Projection.RIGID //
            ));
        }
    }

    public static void bootstrapStructures(BootstapContext<Structure> context) { //
        var biomes = context.lookup(Registries.BIOME); //
        var pools = context.lookup(Registries.TEMPLATE_POOL); //

        for (StructureData data : STRUCTURES_REGISTRY) { //
            ResourceKey<Structure> structureKey = ResourceKey.create(Registries.STRUCTURE, new ResourceLocation(TerraFoundation.MOD_ID, data.structureId())); //
            ResourceKey<StructureTemplatePool> poolKey = ResourceKey.create(Registries.TEMPLATE_POOL, new ResourceLocation(TerraFoundation.MOD_ID, data.structureId() + "_pool")); //

            context.register(structureKey, new JigsawStructure( //
                    new Structure.StructureSettings( //
                            HolderSet.direct(biomes.getOrThrow(data.targetBiome())), //
                            Map.of(), //
                            GenerationStep.Decoration.SURFACE_STRUCTURES, //
                            TerrainAdjustment.NONE //
                    ),
                    pools.getOrThrow(poolKey), //
                    Optional.empty(), //
                    1, //
                    data.heightProvider(),       // 2. Pulled cleanly out of the record profile setup
                    data.projectToHeightmap(),   // 3. Pulled cleanly out of the record profile setup
                    Optional.of(Heightmap.Types.WORLD_SURFACE_WG), //
                    80 //
            ));
        }
    }

    public static void bootstrapSets(BootstapContext<StructureSet> context) {
        var structures = context.lookup(Registries.STRUCTURE);

        for (StructureData data : STRUCTURES_REGISTRY) {
            ResourceKey<StructureSet> setKey = ResourceKey.create(Registries.STRUCTURE_SET, new ResourceLocation(TerraFoundation.MOD_ID, data.structureId() + "_set"));
            ResourceKey<Structure> structureKey = ResourceKey.create(Registries.STRUCTURE, new ResourceLocation(TerraFoundation.MOD_ID, data.structureId()));

            context.register(setKey, new StructureSet(
                    structures.getOrThrow(structureKey),
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