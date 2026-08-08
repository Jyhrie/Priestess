package com.jyhrie.terrafoundation.world.dimension;

import com.jyhrie.terrafoundation.TerraFoundation;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.data.worldgen.BootstapContext;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.FixedBiomeSource;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import java.util.OptionalLong;
import com.mojang.datafixers.util.Pair;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import net.minecraft.core.Holder;
import java.util.List;

public class ModDimensions {

    // Runtime Key (Used for teleporting)
    public static final ResourceKey<Level> TERRA_KEY = ResourceKey.create(
            Registries.DIMENSION,
            new ResourceLocation(TerraFoundation.MOD_ID, "terra")
    );

    // Datagen Key (Used to generate the JSON)
    public static final ResourceKey<LevelStem> TERRA_STEM_KEY = ResourceKey.create(
            Registries.LEVEL_STEM,
            new ResourceLocation(TerraFoundation.MOD_ID, "terra")
    );

    // Dimension Type Key
    public static final ResourceKey<DimensionType> TERRA_TYPE = ResourceKey.create(
            Registries.DIMENSION_TYPE,
            new ResourceLocation(TerraFoundation.MOD_ID, "terra_type")
    );

    public static void register() {
        System.out.println("Registering ModDimensions for " + TerraFoundation.MOD_ID);
    }

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

    public static void bootstrapStem(BootstapContext<LevelStem> context) {
        HolderGetter<Biome> biomeRegistry = context.lookup(Registries.BIOME);
        HolderGetter<DimensionType> dimTypes = context.lookup(Registries.DIMENSION_TYPE);
        HolderGetter<NoiseGeneratorSettings> noiseGenSettings = context.lookup(Registries.NOISE_SETTINGS);

        List<Pair<Climate.ParameterPoint, net.minecraft.core.Holder<Biome>>> parameters = List.of(
                // Neutral Biome
                Pair.of(Climate.parameters(0.0f, 0.0f, 0.3f, 0.0f, 0.0f, 0.0f, 0.0f), biomeRegistry.getOrThrow(ModBiomes.BARRENLANDS)),

                // Sea Biomes
                Pair.of(Climate.parameters(-0.2f, 0.0f, -0.5f, 0.0f, 0.0f, 0.0f, 0.0f), biomeRegistry.getOrThrow(ModBiomes.AEGIR_DEPTHS)),
                Pair.of(Climate.parameters(0.2f,  0.0f, -0.5f, 0.0f, 0.0f, 0.0f, 0.0f), biomeRegistry.getOrThrow(ModBiomes.BOLIVAR_DEPTHS)),
                Pair.of(Climate.parameters(-0.2f, 0.0f, -0.2f, 0.0f, 0.0f, 0.0f, 0.0f), biomeRegistry.getOrThrow(ModBiomes.SEA_OF_SILENCE)),
                Pair.of(Climate.parameters(0.2f,  0.0f, -0.2f, 0.0f, 0.0f, 0.0f, 0.0f), biomeRegistry.getOrThrow(ModBiomes.SIESTA_SEA)),

                // Beaches
                Pair.of(Climate.parameters(-0.2f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f), biomeRegistry.getOrThrow(ModBiomes.IBERIAN_SHORES)),
                Pair.of(Climate.parameters(0.2f,  0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f), biomeRegistry.getOrThrow(ModBiomes.DOSSOLES_BEACHES)),

                // Cold Biomes
                Pair.of(Climate.parameters(-0.2f,  0.0f, 0.33f, 0.0f, 0.0f, 0.0f, 0.0f), biomeRegistry.getOrThrow(ModBiomes.INFY_ICEFIELDS)),
                Pair.of(Climate.parameters(-0.17f, 0.0f, 0.25f, 0.0f, 0.0f, 0.0f, 0.0f), biomeRegistry.getOrThrow(ModBiomes.SAMI_SNOWFIELDS)),

                // Hot Biomes
                Pair.of(Climate.parameters(0.45f, 0.0f, 0.25f, 0.0f, 0.0f, 0.0f, 0.0f), biomeRegistry.getOrThrow(ModBiomes.FOEHN_HOTLANDS)),
                Pair.of(Climate.parameters(0.35f, 0.0f, 0.33f, 0.0f, 0.0f, 0.0f, 0.0f), biomeRegistry.getOrThrow(ModBiomes.KAZDEL_CRAGS)),

                // Mountains
                Pair.of(Climate.parameters(0.2f,  0.0f, 0.7f, -0.4f, 0.0f, 0.0f, 0.0f), biomeRegistry.getOrThrow(ModBiomes.YANESE_PEAKS)),
                Pair.of(Climate.parameters(-0.2f, 0.0f, 0.7f, -0.4f, 0.0f, 0.0f, 0.0f), biomeRegistry.getOrThrow(ModBiomes.HIGASHI_HIGHLANDS))
        );

        // Create the source using the standardized .create() factory wrapper
        MultiNoiseBiomeSource biomeSource = MultiNoiseBiomeSource.createFromList(
                new Climate.ParameterList<>(parameters)
        );

        // Build Chunk Generator
        NoiseBasedChunkGenerator chunkGenerator = new NoiseBasedChunkGenerator(
                biomeSource,
                noiseGenSettings.getOrThrow(ModNoiseSettings.TERRA_SETTINGS)
        );

        LevelStem stem = new LevelStem(dimTypes.getOrThrow(TERRA_TYPE), chunkGenerator);
        context.register(TERRA_STEM_KEY, stem);
    }
}