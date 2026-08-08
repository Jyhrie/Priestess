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

    // Runtime key — use this to teleport into the dimension once a portal/item exists.
    // Nothing references it yet; today the only way in is
    //   /execute in priestess:terra run tp @s ~ ~ ~
    public static final ResourceKey<Level> TERRA_KEY = ResourceKey.create(
            Registries.DIMENSION,
            new ResourceLocation(Priestess.MOD_ID, "terra")
    );

    // Datagen Key (Used to generate the JSON)
    public static final ResourceKey<LevelStem> TERRA_STEM_KEY = ResourceKey.create(
            Registries.LEVEL_STEM,
            new ResourceLocation(Priestess.MOD_ID, "terra")
    );

    // Dimension Type Key
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

    // =========================================================================
    // BIOME PLACEMENT
    // =========================================================================
    // There is no climate model here. Terra has a real geography — Aegir south of
    // Iberia, the Foehn Hotlands south of Sargon, a mountain range from northern
    // Kazimierz through Kjerag to the Sargonian desert — and a multi-noise biome source
    // cannot express any of it. Multi-noise says "Iberia is wherever it is cold and
    // coastal", which gives you infinitely many Iberias and no range that crosses a
    // border.
    //
    // So biomes come from two PNGs in the mod jar instead. Everything about where a
    // region sits lives in:
    //
    //     src/main/resources/data/priestess/terra/regions.png    which region
    //     src/main/resources/data/priestess/terra/elevation.png  how high
    //     world/terra/TerraRegion.java                           region x slot -> biome
    //
    // Repaint the PNGs to move a nation. Edit TerraRegion to change what a nation is
    // made of. Neither is in this file, and that is the point — this file just wires
    // the pieces together.
    //
    // The trade this makes: Terra is now finite, 131,072 x 81,920 blocks, and identical
    // in every world. Walk off the north or south edge and you get the Infy Icefield or
    // the Foehn Hotlands forever, which is what canon puts beyond those frontiers; walk
    // off the east or west edge and you get open ocean.

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
