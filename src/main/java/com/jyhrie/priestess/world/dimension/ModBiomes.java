package com.jyhrie.priestess.world.dimension;

import com.jyhrie.priestess.Priestess;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.AmbientMoodSettings;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.biome.BiomeSpecialEffects;
import net.minecraft.world.level.biome.MobSpawnSettings;

/**
 * The 22 regions of Terra.
 *
 * Each biome is defined by a {@link Palette} (what it looks like) plus a temperature,
 * downfall and precipitation flag (how it behaves). Where each one physically appears
 * is decided separately, by the climate parameters in
 * {@link ModDimensions#bootstrapStem}; what the ground is made of is decided in
 * {@link ModNoiseSettings}.
 *
 * <p>The regions are grouped into five climate wedges, coldest to hottest. Every biome
 * in a wedge borders only its own neighbours in that wedge, so walking inland from an
 * Iberian beach takes you through Iberian water, never across the map into Siesta.
 * Keep that grouping in mind when adding a region: it is the whole reason the wedge
 * table in {@link ModDimensions} is shaped the way it is.
 */
public class ModBiomes {

    // ── Keys ──────────────────────────────────────────────────────────────────
    public static final ResourceKey<Biome> BARRENLANDS = createKey("barrenlands");

    public static final ResourceKey<Biome> AEGIR_DEPTHS = createKey("aegir_depths");
    public static final ResourceKey<Biome> BOLIVAR_DEPTHS = createKey("bolivar_depths");
    public static final ResourceKey<Biome> SEA_OF_SILENCE = createKey("sea_of_silence");
    public static final ResourceKey<Biome> SIESTA_SEA = createKey("siesta_sea");

    public static final ResourceKey<Biome> AEGIR_SHELF = createKey("aegir_shelf");
    public static final ResourceKey<Biome> IBERIAN_SHORES = createKey("iberian_shores");
    public static final ResourceKey<Biome> SIRACUSAN_COAST = createKey("siracusan_coast");
    public static final ResourceKey<Biome> DOSSOLES_BEACHES = createKey("dossoles_beaches");

    public static final ResourceKey<Biome> INFY_ICEFIELDS = createKey("infy_icefields");
    public static final ResourceKey<Biome> SAMI_SNOWFIELDS = createKey("sami_snowfields");
    public static final ResourceKey<Biome> KJERAG_SLOPES = createKey("kjerag_slopes");

    public static final ResourceKey<Biome> FOEHN_HOTLANDS = createKey("foehn_hotlands");
    public static final ResourceKey<Biome> KAZDEL_CRAGS = createKey("kazdel_crags");
    public static final ResourceKey<Biome> SARGON_DUNES = createKey("sargon_dunes");

    public static final ResourceKey<Biome> YANESE_PEAKS = createKey("yanese_peaks");
    public static final ResourceKey<Biome> HIGASHI_HIGHLANDS = createKey("higashi_highlands");

    // The neutral middle of the map: the green, liveable belt the nations fight over.
    public static final ResourceKey<Biome> URSUS_TAIGA = createKey("ursus_taiga");
    public static final ResourceKey<Biome> KAZIMIERZ_PLAINS = createKey("kazimierz_plains");
    public static final ResourceKey<Biome> VICTORIAN_MOORS = createKey("victorian_moors");
    public static final ResourceKey<Biome> LEITHANIEN_WOODS = createKey("leithanien_woods");
    public static final ResourceKey<Biome> BOLIVAR_MIRE = createKey("bolivar_mire");

    private static ResourceKey<Biome> createKey(String name) {
        return ResourceKey.create(Registries.BIOME, new ResourceLocation(Priestess.MOD_ID, name));
    }

    /** Everything that decides how a region reads on screen. */
    private record Palette(int sky, int fog, int water, int waterFog, int grass, int foliage) {}

    // ── Region palettes ───────────────────────────────────────────────────────
    // Dust and ash dominate inland Terra; the seas run from resort-blue in the south
    // to dead grey in the Sea of Silence to near-black in the Aegir trenches.

    /** The wastes between the cities: dust haze, sour water, dead scrub. */
    private static final Palette P_BARRENLANDS = new Palette(0xB0A48C, 0xC9BCA0, 0x6E7A5E, 0x4A5240, 0x9C8F6B, 0x8E8460);
    /** Kazdel: scorched, blood-dark, nothing green left. */
    private static final Palette P_KAZDEL      = new Palette(0x4A2B2B, 0x5C3535, 0x3A2020, 0x241414, 0x6B4A3A, 0x5A3E30);
    /** Arid highlands baked by the foehn wind. */
    private static final Palette P_FOEHN       = new Palette(0xD98E4A, 0xE0A868, 0x8C6A3A, 0x5A4424, 0xB09050, 0xA07C40);

    /** Sami: pale northern sky with a cold cast. */
    private static final Palette P_SAMI        = new Palette(0xA8C4E0, 0xC8DCEC, 0x3D6E8C, 0x223F52, 0xC0CFC0, 0xB4C4B4);
    /** Infy: harsher, whiter, further north still. */
    private static final Palette P_INFY        = new Palette(0xC6E2F0, 0xE0F0F8, 0x2E6A8C, 0x1A3E52, 0xCFE0E0, 0xC4D6D6);

    /** Yan: misted blue-green mountains. */
    private static final Palette P_YANESE      = new Palette(0x7FA8C4, 0xA8C0CC, 0x3E7A8C, 0x244852, 0x6E9C6A, 0x5E8C5A);
    /** Higashi: softer, wetter highlands. */
    private static final Palette P_HIGASHI     = new Palette(0x8FB8D4, 0xB8CEDC, 0x4A8CA0, 0x2A525E, 0x7CA85E, 0x6C9850);

    /** Kjerag: alpine white, thin air, glare off the snowfields. */
    private static final Palette P_KJERAG      = new Palette(0xC8DCF0, 0xE4F0F8, 0x3468A0, 0x1E3C5E, 0xB8C8C0, 0xACBCB4);

    /** Iberia: overcast Atlantic coast — the Deep Sea took the warmth with it. */
    private static final Palette P_IBERIAN     = new Palette(0x8CA8C0, 0xB8CCD8, 0x2E6E94, 0x1A3E56, 0x849464, 0x748456);
    /** Ægir's shelf: an ice lid over water nobody has seen the bottom of. */
    private static final Palette P_AEGIR_SHELF = new Palette(0xB4D0E4, 0xD4E6F0, 0x1C3A54, 0x0E1E30, 0x8FA8A0, 0x819A92);
    /** Siracusa: limestone headlands over a bright, deep-blue sea. */
    private static final Palette P_SIRACUSA    = new Palette(0x9CD0EC, 0xDCEAF0, 0x2FA0C8, 0x1A5E78, 0xA8B96A, 0x98A85C);
    /** Dossoles: tropical resort blue. */
    private static final Palette P_DOSSOLES    = new Palette(0x7FD0F0, 0xCCEEF8, 0x24B8D8, 0x146E80, 0x8FC46A, 0x7FB45A);
    /** Siesta: warm, shallow, holiday water. */
    private static final Palette P_SIESTA      = new Palette(0x86CCEC, 0xC4E8F4, 0x2AA8CC, 0x186476, 0x8AB86A, 0x7AA85A);

    /** The Sea of Silence: colour drained out of it. Iberia abandoned this water. */
    private static final Palette P_SILENCE     = new Palette(0x6E7C80, 0x8A9698, 0x3A5254, 0x1E2E30, 0x7A8478, 0x6E7868);
    /** Bolivar's drowned coast: murky, silted, war-fouled. */
    private static final Palette P_BOLIVAR     = new Palette(0x5A6A70, 0x74848A, 0x2E4448, 0x18282C, 0x74806C, 0x68745E);
    /** Aegir: the abyss. Almost no light gets down here. */
    private static final Palette P_AEGIR       = new Palette(0x2A3A52, 0x3A4A62, 0x0E1A2E, 0x060C18, 0x54604E, 0x4A5646);

    // The neutral belt. These read green and ordinary on purpose — they are the ground
    // the extremes are measured against, and the buffer that stops Infy touching Foehn.

    /** Ursus: black pine and old snow, the last green before the ice. */
    private static final Palette P_URSUS       = new Palette(0x9CB4C8, 0xB8C8D4, 0x3A6E80, 0x203E4A, 0x6E8C64, 0x5C7A54);
    /** Kazimierz: open grassland under a big clean sky. */
    private static final Palette P_KAZIMIERZ   = new Palette(0x8FC0E4, 0xC8DCEC, 0x3E86B4, 0x244E68, 0x8CBB63, 0x7CAB55);
    /** Victoria: heather moor, weathered and grey-green. */
    private static final Palette P_VICTORIA    = new Palette(0x9EB0C0, 0xC0CCD4, 0x486E80, 0x28404A, 0x86976A, 0x76875C);
    /** Leithanien: deep, close forest on the hill flanks. */
    private static final Palette P_LEITHANIEN  = new Palette(0x86A8C4, 0xAEC4D0, 0x3A7288, 0x20404E, 0x5E8C4A, 0x4E7C3E);
    /** Bolivar's inland mire: warm, silted, half water. */
    private static final Palette P_BOLIVAR_MIRE = new Palette(0x8CA894, 0xAEBCA8, 0x50663E, 0x2C3A22, 0x6E8C48, 0x5E7C3C);
    /** Sargon: pale dune sea between the beaches and the hotlands. */
    private static final Palette P_SARGON      = new Palette(0xE0C48C, 0xEEDCB0, 0x9C8A50, 0x5E5230, 0xC4B070, 0xB4A064);

    // ── Bootstrap ─────────────────────────────────────────────────────────────
    public static void bootstrap(BootstapContext<Biome> context) {
        // Grouped by the climate wedge each region belongs to, coldest first, because
        // the visual temperature below has to agree with where ModDimensions puts it —
        // a "cold" Iberia with temperature 0.8F would still grow palm-tree colours.

        // ── Frigid ────────────────────────────────────────────────────────────
        //                                            precip  temp  downfall  palette
        context.register(AEGIR_DEPTHS,      biome(context, true,  0.2F, 0.5F,  P_AEGIR));
        context.register(AEGIR_SHELF,       biome(context, true, -0.4F, 0.6F,  P_AEGIR_SHELF));
        context.register(INFY_ICEFIELDS,    biome(context, true, -0.7F, 0.5F,  P_INFY));
        context.register(KJERAG_SLOPES,     biome(context, true, -0.6F, 0.5F,  P_KJERAG));

        // ── Cold: Iberia and the north ────────────────────────────────────────
        context.register(SEA_OF_SILENCE,    biome(context, false, 0.3F, 0.1F,  P_SILENCE));
        context.register(IBERIAN_SHORES,    biome(context, true,  0.3F, 0.6F,  P_IBERIAN));
        context.register(URSUS_TAIGA,       biome(context, true, -0.1F, 0.7F,  P_URSUS));
        context.register(SAMI_SNOWFIELDS,   biome(context, true, -0.3F, 0.5F,  P_SAMI));
        context.register(YANESE_PEAKS,      biome(context, true,  0.1F, 0.7F,  P_YANESE));

        // ── Temperate: the neutral belt ───────────────────────────────────────
        context.register(KAZIMIERZ_PLAINS,  biome(context, true,  0.6F, 0.4F,  P_KAZIMIERZ));
        context.register(VICTORIAN_MOORS,   biome(context, true,  0.5F, 0.7F,  P_VICTORIA));
        context.register(LEITHANIEN_WOODS,  biome(context, true,  0.45F, 0.8F, P_LEITHANIEN));

        // ── Warm ──────────────────────────────────────────────────────────────
        context.register(BOLIVAR_DEPTHS,    biome(context, true,  0.8F, 0.6F,  P_BOLIVAR));
        context.register(SIRACUSAN_COAST,   biome(context, true,  0.9F, 0.4F,  P_SIRACUSA));
        context.register(BOLIVAR_MIRE,      biome(context, true,  0.9F, 0.9F,  P_BOLIVAR_MIRE));
        context.register(BARRENLANDS,       biome(context, false, 1.1F, 0.05F, P_BARRENLANDS));
        context.register(HIGASHI_HIGHLANDS, biome(context, true,  0.7F, 0.8F,  P_HIGASHI));

        // ── Hot ───────────────────────────────────────────────────────────────
        context.register(SIESTA_SEA,        biome(context, true,  0.9F, 0.6F,  P_SIESTA));
        context.register(DOSSOLES_BEACHES,  biome(context, true,  1.0F, 0.5F,  P_DOSSOLES));
        context.register(SARGON_DUNES,      biome(context, false, 2.0F, 0.0F,  P_SARGON));
        context.register(FOEHN_HOTLANDS,    biome(context, false, 2.0F, 0.0F,  P_FOEHN));
        context.register(KAZDEL_CRAGS,      biome(context, false, 1.6F, 0.0F,  P_KAZDEL));
    }

    /**
     * A biome with no features and no mob spawns — Terra is deliberately bare for now.
     * To populate one, add placed features to {@code generationBuilder} and spawner data
     * to {@code spawnBuilder}.
     *
     * @param temperature visual/behavioural temperature: below 0.15 water freezes and
     *                    snow falls instead of rain. This is NOT the climate temperature
     *                    used to place the biome — that lives in ModDimensions.
     */
    private static Biome biome(BootstapContext<Biome> context, boolean hasPrecipitation,
                               float temperature, float downfall, Palette palette) {
        var placedFeatures = context.lookup(Registries.PLACED_FEATURE);
        var worldCarvers = context.lookup(Registries.CONFIGURED_CARVER);

        BiomeGenerationSettings.Builder generationBuilder = new BiomeGenerationSettings.Builder(placedFeatures, worldCarvers);
        MobSpawnSettings.Builder spawnBuilder = new MobSpawnSettings.Builder();

        return new Biome.BiomeBuilder()
                .hasPrecipitation(hasPrecipitation)
                .temperature(temperature)
                .downfall(downfall)
                .specialEffects(new BiomeSpecialEffects.Builder()
                        .skyColor(palette.sky())
                        .fogColor(palette.fog())
                        .waterColor(palette.water())
                        .waterFogColor(palette.waterFog())
                        .grassColorOverride(palette.grass())
                        .foliageColorOverride(palette.foliage())
                        .ambientMoodSound(AmbientMoodSettings.LEGACY_CAVE_SETTINGS)
                        .build())
                .mobSpawnSettings(spawnBuilder.build())
                .generationSettings(generationBuilder.build())
                .build();
    }
}
