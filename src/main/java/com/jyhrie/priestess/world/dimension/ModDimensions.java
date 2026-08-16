package com.jyhrie.priestess.world.dimension;

import com.jyhrie.priestess.Priestess;
import com.jyhrie.priestess.world.terra.TerraMapBiomeSource;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;

import java.util.OptionalLong;

public class ModDimensions {

    /** Runtime key. There is no portal yet — {@code /execute in priestess:terra run tp @s ~ ~ ~}. */
    public static final ResourceKey<Level> TERRA_KEY = ResourceKey.create(
            Registries.DIMENSION,
            new ResourceLocation(Priestess.MOD_ID, "terra")
    );

    public static final ResourceKey<LevelStem> TERRA_STEM_KEY = ResourceKey.create(
            Registries.LEVEL_STEM,
            new ResourceLocation(Priestess.MOD_ID, "terra")
    );

    public static final ResourceKey<DimensionType> TERRA_TYPE = ResourceKey.create(
            Registries.DIMENSION_TYPE,
            new ResourceLocation(Priestess.MOD_ID, "terra_type")
    );

    public static void bootstrapType(BootstapContext<DimensionType> context) {
        context.register(TERRA_TYPE, new DimensionType(
                OptionalLong.empty(), // fixedTime
                true, // hasSkylight
                false, // hasCeiling
                false, // ultraWarm
                true, // natural
                1.0, // coordinateScale
                true, // bedWorks
                false, // respawnAnchorWorks
                -64, // minY
                384, // height
                384, // logicalHeight
                BlockTags.INFINIBURN_OVERWORLD, // infiniburn
                BuiltinDimensionTypes.OVERWORLD_EFFECTS, // effectsLocation
                0.0f, // ambientLight
                new DimensionType.MonsterSettings(false, true, UniformInt.of(0, 7), 0)
        ));
    }

    // Biomes come from the painted map, not a climate model — see TerraMap. This file only
    // wires the pieces together. The trade: Terra is finite and identical in every world.

    public static void bootstrapStem(BootstapContext<LevelStem> context) {
        HolderGetter<Biome> biomes = context.lookup(Registries.BIOME);
        HolderGetter<DimensionType> dimTypes = context.lookup(Registries.DIMENSION_TYPE);
        HolderGetter<NoiseGeneratorSettings> noiseGenSettings = context.lookup(Registries.NOISE_SETTINGS);

        NoiseBasedChunkGenerator chunkGenerator = new NoiseBasedChunkGenerator(
                TerraMapBiomeSource.create(biomes),
                noiseGenSettings.getOrThrow(ModNoiseSettings.TERRA_SETTINGS)
        );

        LevelStem stem = new LevelStem(dimTypes.getOrThrow(TERRA_TYPE), chunkGenerator);
        context.register(TERRA_STEM_KEY, stem);
    }
}
