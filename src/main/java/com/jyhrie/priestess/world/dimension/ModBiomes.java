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

import java.util.function.Consumer;

/**
 * One biome per zone painted in {@code data/priestess/terra/regions.png}.
 *
 * <h2>The map is the whole placement model</h2>
 * A colour on the map <em>is</em> a biome. There is no climate noise, no elevation banding,
 * and no rule that quietly substitutes something else — paint a zone and that zone is what
 * generates, from the seabed to the summit. If you want a beach, an ocean or a snowline,
 * you paint it as its own colour.
 *
 * <p>That is a deliberate trade. The previous model gave every zone eight biomes indexed by
 * elevation, which meant painting Infy also got you Ægir shelf on its coast and Kjerag on
 * its peaks — biomes you never asked for appearing on ground you never zoned. This costs
 * detail per zone and buys back the property that the map is literally what you get.
 *
 * <p>Elevation still matters, it just no longer picks biomes. {@code elevation.png} drives
 * the {@code mapHeight} spline in {@link ModNoiseSettings}, so terrain still rises and falls
 * exactly as painted; and the surface rules there key off height <em>within</em> a biome, so
 * a zone can still go bare rock above the treeline without that being a different biome.
 *
 * <h2>Adding a zone</h2>
 * Three edits, in this order, or the game will not start:
 * <ol>
 *   <li>register the biome here,</li>
 *   <li>add the colour and this key to {@link com.jyhrie.priestess.world.terra.TerraRegion},</li>
 *   <li>give it a surface rule in {@link ModNoiseSettings}, or it generates as bare stone.</li>
 * </ol>
 * Then re-run {@code gradlew runData}.
 */
public class ModBiomes {

    // ── Keys ──────────────────────────────────────────────────────────────────
    // One per colour in regions.png. Nothing else belongs in this list — a biome with no
    // zone painted for it is a biome that cannot generate.

    /** Everything outside the continent. Split into named seas once they are painted. */
    public static final ResourceKey<Biome> OCEAN = createKey("ocean");

    public static final ResourceKey<Biome> INFY_ICEFIELD = createKey("infy_icefield");
    public static final ResourceKey<Biome> SAMI = createKey("sami");

    public static final ResourceKey<Biome> URSUS_COLD = createKey("ursus_cold");
    public static final ResourceKey<Biome> URSUS_DRY = createKey("ursus_dry");
    public static final ResourceKey<Biome> URSUS_WARM = createKey("ursus_warm");

    public static final ResourceKey<Biome> KJERAG = createKey("kjerag");
    public static final ResourceKey<Biome> MOUNT_KARLAN = createKey("mount_karlan");

    public static final ResourceKey<Biome> KAZIMIERZ = createKey("kazimierz");
    public static final ResourceKey<Biome> COLUMBIA = createKey("columbia");
    public static final ResourceKey<Biome> IBERIA_LAND = createKey("iberia_land");

    public static final ResourceKey<Biome> YAN = createKey("yan");
    public static final ResourceKey<Biome> HIGASHI_COLD = createKey("higashi_cold");
    public static final ResourceKey<Biome> HIGASHI_WARM = createKey("higashi_warm");

    public static final ResourceKey<Biome> KAZDEL = createKey("kazdel");

    /** Not a place. Ground that has not been zoned yet, in the same acid yellow as the map. */
    public static final ResourceKey<Biome> TEMPORARY_LAYER = createKey("temporary_layer");

    private static ResourceKey<Biome> createKey(String name) {
        return ResourceKey.create(Registries.BIOME, new ResourceLocation(Priestess.MOD_ID, name));
    }

    /** Everything that decides how a zone reads on screen. */
    private record Palette(int sky, int fog, int water, int waterFog, int grass, int foliage) {}

    // ── Zone palettes ─────────────────────────────────────────────────────────

    /** The open sea: cold, deep and unlit, until the named oceans get painted in. */
    private static final Palette P_OCEAN       = new Palette(0x3A5A7C, 0x50708C, 0x1C3A54, 0x0E1E30, 0x6E8474, 0x627866);

    /** Infy: harsh, white, the far north. */
    private static final Palette P_INFY        = new Palette(0xC6E2F0, 0xE0F0F8, 0x2E6A8C, 0x1A3E52, 0xCFE0E0, 0xC4D6D6);
    /** Sami: pale northern sky with a cold cast. */
    private static final Palette P_SAMI        = new Palette(0xA8C4E0, 0xC8DCEC, 0x3D6E8C, 0x223F52, 0xC0CFC0, 0xB4C4B4);

    /** Northern Ursus: black pine and old snow, the last green before the ice. */
    private static final Palette P_URSUS_COLD  = new Palette(0x9CB4C8, 0xB8C8D4, 0x3A6E80, 0x203E4A, 0x6E8C64, 0x5C7A54);
    /** The Ursine steppe: cropped, strawy grass and nothing to break the wind. */
    private static final Palette P_URSUS_DRY   = new Palette(0xA8BCC8, 0xC4D0D4, 0x486E7C, 0x283E46, 0x9C9C64, 0x8C8C58);
    /** Southern Ursus: birch gets in among the pine and the whole place lightens. */
    private static final Palette P_URSUS_WARM  = new Palette(0x9CB8CC, 0xB8CCD8, 0x3E7488, 0x22424E, 0x6E9C50, 0x5E8C44);

    /** Kjerag: alpine white, thin air, glare off the snowfields. */
    private static final Palette P_KJERAG      = new Palette(0xC8DCF0, 0xE4F0F8, 0x3468A0, 0x1E3C5E, 0xB8C8C0, 0xACBCB4);
    /** Mount Karlan: Kjerag with the colour bleached out of it. Nothing grows this high. */
    private static final Palette P_KARLAN      = new Palette(0xDCEEFA, 0xF2FAFF, 0x2C5E96, 0x18345A, 0xAEC0BC, 0xA2B4B0);

    /** Kazimierz: open grassland under a big clean sky. */
    private static final Palette P_KAZIMIERZ   = new Palette(0x8FC0E4, 0xC8DCEC, 0x3E86B4, 0x244E68, 0x8CBB63, 0x7CAB55);
    /** Columbia: deep close forest on the hill flanks. */
    private static final Palette P_COLUMBIA    = new Palette(0x86A8C4, 0xAEC4D0, 0x3A7288, 0x20404E, 0x5E8C4A, 0x4E7C3E);
    /** Iberia: overcast coast and sour heath — the Deep Sea took the warmth with it. */
    private static final Palette P_IBERIA      = new Palette(0x8CA8C0, 0xB8CCD8, 0x2E6E94, 0x1A3E56, 0x849464, 0x748456);

    /** Yan: misted blue-green mountains. */
    private static final Palette P_YAN         = new Palette(0x7FA8C4, 0xA8C0CC, 0x3E7A8C, 0x244852, 0x6E9C6A, 0x5E8C5A);
    /** Northern Higashi: cedar under snow, the light gone flat and grey. */
    private static final Palette P_HIGASHI_COLD = new Palette(0x8CA8C0, 0xB0C4D0, 0x3A6E86, 0x203E4C, 0x5E7C5E, 0x4E6C50);
    /** Southern Higashi: paddy green under a humid haze. */
    private static final Palette P_HIGASHI_WARM = new Palette(0x9CC4DC, 0xC8DCE4, 0x4E96A8, 0x2C5662, 0x8CBC58, 0x7CAC4A);

    /** Kazdel: scorched, blood-dark, nothing green left. */
    private static final Palette P_KAZDEL      = new Palette(0x4A2B2B, 0x5C3535, 0x3A2020, 0x241414, 0x6B4A3A, 0x5A3E30);

    /**
     * The placeholder. Deliberately hideous, in the same acid yellow the zone is painted in
     * {@code regions.png}, so that ground you have not assigned yet announces itself from a
     * distance instead of being mistaken for a design decision.
     */
    private static final Palette P_TEMPORARY   = new Palette(0xD1FF00, 0xE4FF66, 0xD1FF00, 0xA0C000, 0xD1FF00, 0xD1FF00);

    // ── Bootstrap ─────────────────────────────────────────────────────────────
    public static void bootstrap(BootstapContext<Biome> context) {
        // Grouped coldest to hottest. The grouping carries no mechanical weight any more —
        // the map places things, not a climate wedge — but temperature still decides whether
        // water freezes and whether precipitation falls as snow, so it has to agree with the
        // company a zone keeps. Below 0.15 freezes.

        //                                          precip   temp   downfall  palette
        context.register(OCEAN,         biome(context, true,  0.4F,  0.5F,  P_OCEAN));

        // ── The north ─────────────────────────────────────────────────────────
        context.register(INFY_ICEFIELD, biome(context, true, -0.7F,  0.5F,  P_INFY));
        context.register(SAMI,          biome(context, true, -0.3F,  0.5F,  P_SAMI));

        context.register(URSUS_COLD,    biome(context, true, -0.1F,  0.7F,  P_URSUS_COLD));
        // Above freezing, and dry: that low downfall is what makes it steppe, not forest.
        context.register(URSUS_DRY,     biome(context, true,  0.3F,  0.2F,  P_URSUS_DRY));
        context.register(URSUS_WARM,    biome(context, true,  0.4F,  0.7F,  P_URSUS_WARM));

        // ── The range ─────────────────────────────────────────────────────────
        context.register(KJERAG,        biome(context, true, -0.6F,  0.5F,  P_KJERAG));
        // Colder than the slopes below it, so the summit keeps snow when Kjerag thaws.
        context.register(MOUNT_KARLAN,  biome(context, true, -0.9F,  0.4F,  P_KARLAN));

        // ── The heartland and the west ────────────────────────────────────────
        context.register(KAZIMIERZ,     biome(context, true,  0.6F,  0.4F,  P_KAZIMIERZ));
        // Columbia used to be the one biome with anything living in it — Originium Slugs,
        // two to four at a time, at weight 40. That went when the slug was stripped back to
        // a bare mob, so no biome in Terra has a natural spawn any more and this one takes
        // the same no-spawns overload as every other. The two-argument form is still there
        // and this is the call to give a Consumer back to when the wastes are repopulated.
        context.register(COLUMBIA,      biome(context, true,  0.45F, 0.8F,  P_COLUMBIA));
        context.register(IBERIA_LAND,   biome(context, true,  0.25F, 0.5F,  P_IBERIA));

        // ── The east ──────────────────────────────────────────────────────────
        context.register(YAN,           biome(context, true,  0.1F,  0.7F,  P_YAN));
        context.register(HIGASHI_COLD,  biome(context, true,  0.0F,  0.8F,  P_HIGASHI_COLD));
        context.register(HIGASHI_WARM,  biome(context, true,  0.85F, 0.9F,  P_HIGASHI_WARM));

        // ── The south ─────────────────────────────────────────────────────────
        context.register(KAZDEL,        biome(context, false, 1.6F,  0.0F,  P_KAZDEL));

        // ── Not a climate ─────────────────────────────────────────────────────
        context.register(TEMPORARY_LAYER, biome(context, false, 0.7F, 0.5F, P_TEMPORARY));
    }

    /**
     * A biome with no features and no mob spawns — Terra is deliberately bare for now.
     * To populate one, add placed features to {@code generationBuilder} and spawner data
     * to {@code spawnBuilder}.
     *
     * <p>For mobs specifically, use the overload below and see {@code docs/SPAWNING.md}: a
     * spawner entry here is only half of a natural spawn, and the other half is a
     * {@code SpawnPlacements} rule in {@code ModEntities}. Neither works alone and neither
     * errors when the other is missing.
     *
     * @param temperature visual/behavioural temperature: below 0.15 water freezes and snow
     *                    falls instead of rain. Nothing places the biome by this any more —
     *                    the map does that — so it is purely how the zone behaves once you
     *                    are standing in it.
     */
    private static Biome biome(BootstapContext<Biome> context, boolean hasPrecipitation,
                               float temperature, float downfall, Palette palette) {
        return biome(context, hasPrecipitation, temperature, downfall, palette, spawns -> {});
    }

    /** As above, with a chance to populate the biome. */
    private static Biome biome(BootstapContext<Biome> context, boolean hasPrecipitation,
                               float temperature, float downfall, Palette palette,
                               Consumer<MobSpawnSettings.Builder> spawns) {
        var placedFeatures = context.lookup(Registries.PLACED_FEATURE);
        var worldCarvers = context.lookup(Registries.CONFIGURED_CARVER);

        BiomeGenerationSettings.Builder generationBuilder = new BiomeGenerationSettings.Builder(placedFeatures, worldCarvers);
        MobSpawnSettings.Builder spawnBuilder = new MobSpawnSettings.Builder();
        spawns.accept(spawnBuilder);

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
