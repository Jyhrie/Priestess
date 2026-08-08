package com.jyhrie.terrafoundation.world.dimension;

import com.jyhrie.terrafoundation.TerraFoundation;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.AmbientMoodSettings;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.biome.BiomeSpecialEffects;
import net.minecraft.world.level.biome.MobSpawnSettings;

public class ModBiomes {

    // 1. Define the Keys for all 13 Biomes
    public static final ResourceKey<Biome> BARRENLANDS = createKey("barrenlands");

    public static final ResourceKey<Biome> AEGIR_DEPTHS = createKey("aegir_depths");
    public static final ResourceKey<Biome> BOLIVAR_DEPTHS = createKey("bolivar_depths");
    public static final ResourceKey<Biome> SEA_OF_SILENCE = createKey("sea_of_silence");
    public static final ResourceKey<Biome> SIESTA_SEA = createKey("siesta_sea");

    public static final ResourceKey<Biome> IBERIAN_SHORES = createKey("iberian_shores");
    public static final ResourceKey<Biome> DOSSOLES_BEACHES = createKey("dossoles_beaches");

    public static final ResourceKey<Biome> INFY_ICEFIELDS = createKey("infy_icefields");
    public static final ResourceKey<Biome> SAMI_SNOWFIELDS = createKey("sami_snowfields");

    public static final ResourceKey<Biome> FOEHN_HOTLANDS = createKey("foehn_hotlands");
    public static final ResourceKey<Biome> KAZDEL_CRAGS = createKey("kazdel_crags");

    public static final ResourceKey<Biome> YANESE_PEAKS = createKey("yanese_peaks");
    public static final ResourceKey<Biome> HIGASHI_HIGHLANDS = createKey("higashi_highlands");


    // Helper method to create keys cleanly
    private static ResourceKey<Biome> createKey(String name) {
        return ResourceKey.create(Registries.BIOME, new ResourceLocation(TerraFoundation.MOD_ID, name));
    }

    // 2. Bootstrap Context (This runs during runData)
    public static void bootstrap(BootstapContext<Biome> context) {

        // Register all biomes using a template helper method
        // You can tweak temperatures, downfall, and water colors here!
        // Parameters: context, hasPrecipitation, temperature, downfall, waterColor

        context.register(BARRENLANDS, blankBiome(context, false, 2.0F, 0.0F, 4159204));

        context.register(AEGIR_DEPTHS, blankBiome(context, true, 0.5F, 0.5F, 4159204));
        context.register(BOLIVAR_DEPTHS, blankBiome(context, true, 0.5F, 0.5F, 4159204));
        context.register(SEA_OF_SILENCE, blankBiome(context, true, 0.5F, 0.5F, 4159204));
        context.register(SIESTA_SEA, blankBiome(context, true, 0.5F, 0.5F, 4159204));

        context.register(IBERIAN_SHORES, blankBiome(context, true, 0.8F, 0.4F, 4159204));
        context.register(DOSSOLES_BEACHES, blankBiome(context, true, 0.8F, 0.4F, 4159204));

        context.register(INFY_ICEFIELDS, blankBiome(context, true, -0.5F, 0.5F, 4159204));
        context.register(SAMI_SNOWFIELDS, blankBiome(context, true, -0.5F, 0.5F, 4159204));

        context.register(FOEHN_HOTLANDS, blankBiome(context, false, 2.0F, 0.0F, 4159204));
        context.register(KAZDEL_CRAGS, blankBiome(context, false, 2.0F, 0.0F, 4159204));

        context.register(YANESE_PEAKS, blankBiome(context, true, 0.2F, 0.8F, 4159204));
        context.register(HIGASHI_HIGHLANDS, blankBiome(context, true, 0.2F, 0.8F, 4159204));
    }

    // 3. The Blueprint for a blank biome (no trees/ores yet, just terrain/color)
    private static Biome blankBiome(BootstapContext<Biome> context, boolean hasPrecipitation, float temperature, float downfall, int waterColor) {
        // We look up features and carvers so we can add caves and trees later
        var placedFeatures = context.lookup(Registries.PLACED_FEATURE);
        var worldCarvers = context.lookup(Registries.CONFIGURED_CARVER);

        BiomeGenerationSettings.Builder generationBuilder = new BiomeGenerationSettings.Builder(placedFeatures, worldCarvers);
        MobSpawnSettings.Builder spawnBuilder = new MobSpawnSettings.Builder();

        return new Biome.BiomeBuilder()
                .hasPrecipitation(hasPrecipitation)
                .temperature(temperature)
                .downfall(downfall)
                .specialEffects((new BiomeSpecialEffects.Builder())
                        .waterColor(waterColor)
                        .waterFogColor(329011)
                        .skyColor(8103167)
                        .fogColor(12638463)
                        .ambientMoodSound(AmbientMoodSettings.LEGACY_CAVE_SETTINGS)
                        .build())
                .mobSpawnSettings(spawnBuilder.build())
                .generationSettings(generationBuilder.build())
                .build();
    }
}