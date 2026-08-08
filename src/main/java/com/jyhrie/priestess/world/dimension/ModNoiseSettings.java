package com.jyhrie.priestess.world.dimension;

import com.jyhrie.priestess.Priestess;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.*;
import net.minecraft.world.level.levelgen.placement.CaveSurface;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import net.minecraft.util.CubicSpline;


import java.util.List;

public class ModNoiseSettings {

    public static final ResourceKey<NoiseGeneratorSettings> TERRA_SETTINGS = ResourceKey.create(
            Registries.NOISE_SETTINGS,
            new ResourceLocation(Priestess.MOD_ID, "settings")
    );

    // ── Noise Keys ────────────────────────────────────────────────────────────
    public static final ResourceKey<NormalNoise.NoiseParameters> CONTINENT = noiseKey("continent");
    public static final ResourceKey<NormalNoise.NoiseParameters> EROSION   = noiseKey("erosion");
    public static final ResourceKey<NormalNoise.NoiseParameters> DEPTH     = noiseKey("depth");
    public static final ResourceKey<NormalNoise.NoiseParameters> TEMPERATURE  = noiseKey("temperature");
    public static final ResourceKey<NormalNoise.NoiseParameters> SURFACE_PATCH = noiseKey("surface_patch");

    private static ResourceKey<NormalNoise.NoiseParameters> noiseKey(String name) {
        return ResourceKey.create(Registries.NOISE, new ResourceLocation(Priestess.MOD_ID, name));
    }

    // ── Noise Bootstrap ───────────────────────────────────────────────────────
    // Call this from your RegistrySetBuilder alongside bootstrapSettings()
    public static void bootstrapNoise(BootstapContext<NormalNoise.NoiseParameters> ctx) {
        ctx.register(CONTINENT,     new NormalNoise.NoiseParameters(-10, List.of(1.0, 1.0, 2.0, 2.0, 2.0)));
        ctx.register(EROSION,       new NormalNoise.NoiseParameters(-8,  List.of(1.0, 0.3, 0.5)));
        ctx.register(DEPTH,         new NormalNoise.NoiseParameters(-8,  List.of(1.0, 0.25, 0.15)));
        ctx.register(TEMPERATURE,   new NormalNoise.NoiseParameters(-12, List.of(1.0, 0.2)));
        ctx.register(SURFACE_PATCH, new NormalNoise.NoiseParameters(-1,  List.of(1.0, 1.0, 1.0)));
    }

    // ── Settings Bootstrap ────────────────────────────────────────────────────
    public static void bootstrap(BootstapContext<NoiseGeneratorSettings> context) {
        NoiseSettings noiseSettings = NoiseSettings.create(-64, 384, 1, 2);

        context.register(TERRA_SETTINGS, new NoiseGeneratorSettings(
                noiseSettings,
                Blocks.STONE.defaultBlockState(),
                Blocks.WATER.defaultBlockState(),
                createNoiseRouter(context),
                createSurfaceRules(),
                List.of(),
                124,    // sea_level
                false,  // disable_mob_generation
                false,  // aquifers_enabled
                false,  // ore_veins_enabled
                false   // legacy_random_source
        ));
    }

    private static NoiseRouter createNoiseRouter(BootstapContext<NoiseGeneratorSettings> context) {
        HolderGetter<NormalNoise.NoiseParameters> noises = context.lookup(Registries.NOISE);

        // ---------------------------------------------------------------------
        // Raw noises (Matching your json configuration fields)
        // ---------------------------------------------------------------------

        Holder<DensityFunction> continentNoise = Holder.direct(
                DensityFunctions.noise(noises.getOrThrow(CONTINENT), 1.0, 0.0)
        );

        Holder<DensityFunction> erosionNoise = Holder.direct(
                DensityFunctions.noise(noises.getOrThrow(EROSION), 1.0, 0.0)
        );
        DensityFunction depthNoise     = DensityFunctions.noise(noises.getOrThrow(DEPTH), 1.0, 0.0);
        DensityFunction temperatureNoise = DensityFunctions.noise(noises.getOrThrow(TEMPERATURE), 1.0, 0.0);

        // ---------------------------------------------------------------------
        // Continents: add(noise(continent), 0.11)
        // ---------------------------------------------------------------------
        DensityFunction continents = DensityFunctions.add(
                continentNoise.value(),
                DensityFunctions.constant(0.11)
        );

        // ---------------------------------------------------------------------
        // y gradient: yClampedGradient(-64, 320, 1.5, -1.5)
        // ---------------------------------------------------------------------
        DensityFunction yGradient = DensityFunctions.yClampedGradient(-64, 320, 1.5, -1.5);

        // ---------------------------------------------------------------------
        // Below ground spline
        // ---------------------------------------------------------------------
        DensityFunction belowGround = DensityFunctions.spline(
                CubicSpline.builder(new DensityFunctions.Spline.Coordinate(continentNoise))
                        .addPoint(-1.0f, -1.0f, 1.0f)
                        .addPoint( 0.0f,  0.0f, 0.0f)
                        .addPoint( 1.0f,  0.0f, 0.0f)
                        .build()
        );

        // ---------------------------------------------------------------------
        // Erosion spline
        // ---------------------------------------------------------------------
        DensityFunction erosionSpline = DensityFunctions.spline(
                CubicSpline.builder(new DensityFunctions.Spline.Coordinate(erosionNoise))
                        .addPoint(-1.0f, 1.0f, 0.0f)
                        .addPoint( 0.0f, 0.0f, 0.0f)
                        .addPoint( 1.0f, 0.0f, 0.0f)
                        .build()
        );

        // ---------------------------------------------------------------------
        // Continent spline #2
        // ---------------------------------------------------------------------
        DensityFunction continentSpline2 = DensityFunctions.spline(
                CubicSpline.builder(new DensityFunctions.Spline.Coordinate(continentNoise))
                        .addPoint(-1.0f, -1.0f, 0.0f)
                        .addPoint( 0.0f,  0.0f, 0.0f)
                        .addPoint( 0.5f,  1.0f, 0.0f)
                        .build()
        );

        // ---------------------------------------------------------------------
        // Multiply splines: mul(erosionSpline, continentSpline2)
        // ---------------------------------------------------------------------
        DensityFunction aboveGroundCoord = DensityFunctions.mul(
                erosionSpline,
                continentSpline2
        );

        // ---------------------------------------------------------------------
        // Above ground spline
        // ---------------------------------------------------------------------
        DensityFunction aboveGround = DensityFunctions.spline(
                CubicSpline.builder(new DensityFunctions.Spline.Coordinate(Holder.direct(aboveGroundCoord)))
                        .addPoint(-1.0f,    -1.0f,    0.0f)
                        .addPoint( 0.0f,    -1.0f,    0.0f)
                        .addPoint( 0.00001f, 0.00001f, 1.0f)
                        .addPoint( 1.0f,     1.0f,    1.0f)
                        .build()
        );

        // ---------------------------------------------------------------------
        // final density: add(y_gradient, max(belowGround, aboveGround))
        // ---------------------------------------------------------------------
        DensityFunction finalDensity = DensityFunctions.add(
                yGradient,
                DensityFunctions.max(
                        belowGround,
                        aboveGround
                )
        );

        DensityFunction zero = DensityFunctions.zero();

        return new NoiseRouter(
                /* barrier */                         zero,
                /* fluidLevelFloodedness */          DensityFunctions.constant(-1.0),
                /* fluidLevelSpread */               zero,
                /* lava */                           zero,
                /* temperature */                    temperatureNoise,
                /* vegetation */                     zero,
                /* continents */                     continents,
                /* erosion */                        erosionNoise.value(),
                /* depth */                          depthNoise,
                /* ridges */                         zero,
                /* initialDensityWithoutJaggedness */ zero,
                /* finalDensity */                   finalDensity,
                /* veinToggle */                     zero,
                /* veinRidged */                     zero,
                /* veinGap */                        zero
        );
    }

    // ── Surface Rules ─────────────────────────────────────────────────────────
    private static SurfaceRules.RuleSource createSurfaceRules() {
        // Block shorthands
        var bedrock       = SurfaceRules.state(Blocks.BEDROCK.defaultBlockState());
        var stone         = SurfaceRules.state(Blocks.STONE.defaultBlockState());
        var deepslate     = SurfaceRules.state(Blocks.DEEPSLATE.defaultBlockState());
        var snow          = SurfaceRules.state(Blocks.SNOW_BLOCK.defaultBlockState());
        var ice           = SurfaceRules.state(Blocks.ICE.defaultBlockState());
        var packedIce     = SurfaceRules.state(Blocks.PACKED_ICE.defaultBlockState());
        var blueIce       = SurfaceRules.state(Blocks.BLUE_ICE.defaultBlockState());
        var gravel        = SurfaceRules.state(Blocks.GRAVEL.defaultBlockState());
        var basalt        = SurfaceRules.state(Blocks.BASALT.defaultBlockState());
        var smoothBasalt  = SurfaceRules.state(Blocks.SMOOTH_BASALT.defaultBlockState());
        var blackstone    = SurfaceRules.state(Blocks.BLACKSTONE.defaultBlockState());
        var sand          = SurfaceRules.state(Blocks.SAND.defaultBlockState());
        var sandstone     = SurfaceRules.state(Blocks.SANDSTONE.defaultBlockState());
        var redSand       = SurfaceRules.state(Blocks.RED_SAND.defaultBlockState());
        var redSandstone  = SurfaceRules.state(Blocks.RED_SANDSTONE.defaultBlockState());
        var orangeTerra   = SurfaceRules.state(Blocks.ORANGE_TERRACOTTA.defaultBlockState());
        var grass         = SurfaceRules.state(Blocks.GRASS_BLOCK.defaultBlockState());
        var dirt          = SurfaceRules.state(Blocks.DIRT.defaultBlockState());
        var coarseDirt    = SurfaceRules.state(Blocks.COARSE_DIRT.defaultBlockState());
        var rootedDirt    = SurfaceRules.state(Blocks.ROOTED_DIRT.defaultBlockState());
        var cobblestone   = SurfaceRules.state(Blocks.COBBLESTONE.defaultBlockState());
        var cobDeepslate  = SurfaceRules.state(Blocks.COBBLED_DEEPSLATE.defaultBlockState());

        // Noise threshold conditions for surface_patch
        var patchHigh = SurfaceRules.noiseCondition(SURFACE_PATCH,  0.4,  1.0);
        var patchMid  = SurfaceRules.noiseCondition(SURFACE_PATCH,  0.1,  0.4);
        var patchLow  = SurfaceRules.noiseCondition(SURFACE_PATCH, -0.2,  0.1);

        // Floor depth condition helpers
        // stoneDepthCheck(offset, addSurfaceDepth, secondaryDepthRange, surface)
        var floor0 = SurfaceRules.stoneDepthCheck(0, false, 0, CaveSurface.FLOOR);
        var floor1 = SurfaceRules.stoneDepthCheck(1, false, 0, CaveSurface.FLOOR);
        var floor2 = SurfaceRules.stoneDepthCheck(2, false, 0, CaveSurface.FLOOR);
        var floor3 = SurfaceRules.stoneDepthCheck(3, false, 0, CaveSurface.FLOOR);
        var floor4 = SurfaceRules.stoneDepthCheck(4, false, 0, CaveSurface.FLOOR);
        var floor8 = SurfaceRules.stoneDepthCheck(8, false, 0, CaveSurface.FLOOR);

        return SurfaceRules.sequence(

                // 1. Bedrock floor
                SurfaceRules.ifTrue(
                        SurfaceRules.verticalGradient("minecraft:bedrock_floor",
                                VerticalAnchor.aboveBottom(0), VerticalAnchor.aboveBottom(5)),
                        bedrock
                ),

                // Sami Snowfields: snow / ice / packed_ice
                SurfaceRules.ifTrue(SurfaceRules.isBiome(ModBiomes.SAMI_SNOWFIELDS),
                        SurfaceRules.sequence(
                                SurfaceRules.ifTrue(floor0, snow),
                                SurfaceRules.ifTrue(floor3, ice),
                                SurfaceRules.ifTrue(floor8, packedIce)
                        )
                ),

                // Sea beds: a single block down to depth 4. floor4 already covers depth 0,
                // so one rule per biome is enough.
                SurfaceRules.ifTrue(SurfaceRules.isBiome(ModBiomes.SEA_OF_SILENCE),
                        SurfaceRules.ifTrue(floor4, gravel)),

                SurfaceRules.ifTrue(SurfaceRules.isBiome(ModBiomes.AEGIR_DEPTHS),
                        SurfaceRules.ifTrue(floor4, deepslate)),

                SurfaceRules.ifTrue(SurfaceRules.isBiome(ModBiomes.SIESTA_SEA),
                        SurfaceRules.ifTrue(floor4, basalt)),

                SurfaceRules.ifTrue(SurfaceRules.isBiome(ModBiomes.BOLIVAR_DEPTHS),
                        SurfaceRules.ifTrue(floor4, blackstone)),

                // Infy Icefields: snow / packed_ice / blue_ice
                SurfaceRules.ifTrue(SurfaceRules.isBiome(ModBiomes.INFY_ICEFIELDS),
                        SurfaceRules.sequence(
                                SurfaceRules.ifTrue(floor0, snow),
                                SurfaceRules.ifTrue(floor3, packedIce),
                                SurfaceRules.ifTrue(floor8, blueIce)
                        )
                ),

                // Foehn Hotlands: noise-patched red sand/sandstone surface, subsurface layers
                SurfaceRules.ifTrue(SurfaceRules.isBiome(ModBiomes.FOEHN_HOTLANDS),
                        SurfaceRules.sequence(
                                SurfaceRules.ifTrue(floor0, SurfaceRules.sequence(
                                        SurfaceRules.ifTrue(patchHigh, redSandstone),
                                        SurfaceRules.ifTrue(patchMid,  redSand),
                                        SurfaceRules.ifTrue(patchLow,  redSand)
                                )),
                                SurfaceRules.ifTrue(floor1, redSand),
                                SurfaceRules.ifTrue(floor2, redSandstone),
                                SurfaceRules.ifTrue(floor3, orangeTerra)
                        )
                ),

                // Kazdel Crags: noise-patched basalt/deepslate surface, subsurface layers
                SurfaceRules.ifTrue(SurfaceRules.isBiome(ModBiomes.KAZDEL_CRAGS),
                        SurfaceRules.sequence(
                                SurfaceRules.ifTrue(floor0, SurfaceRules.sequence(
                                        SurfaceRules.ifTrue(patchHigh, smoothBasalt),
                                        SurfaceRules.ifTrue(patchMid,  basalt),
                                        SurfaceRules.ifTrue(patchLow,  deepslate)
                                )),
                                SurfaceRules.ifTrue(floor1, cobDeepslate),
                                SurfaceRules.ifTrue(floor2, deepslate),
                                SurfaceRules.ifTrue(floor3, basalt)
                        )
                ),

                // Iberian Shores: sand + sandstone
                SurfaceRules.ifTrue(SurfaceRules.isBiome(ModBiomes.IBERIAN_SHORES),
                        SurfaceRules.sequence(
                                SurfaceRules.ifTrue(floor3, sand),
                                SurfaceRules.ifTrue(floor4, sandstone)
                        )
                ),

                // Dossoles Beaches: sand + sandstone
                SurfaceRules.ifTrue(SurfaceRules.isBiome(ModBiomes.DOSSOLES_BEACHES),
                        SurfaceRules.sequence(
                                SurfaceRules.ifTrue(floor3, sand),
                                SurfaceRules.ifTrue(floor4, sandstone)
                        )
                ),

                // Yanese Peaks: grass + dirt
                SurfaceRules.ifTrue(SurfaceRules.isBiome(ModBiomes.YANESE_PEAKS),
                        SurfaceRules.sequence(
                                SurfaceRules.ifTrue(floor0, grass),
                                SurfaceRules.ifTrue(floor4, dirt)
                        )
                ),

                // Higashi Highlands: grass + dirt
                SurfaceRules.ifTrue(SurfaceRules.isBiome(ModBiomes.HIGASHI_HIGHLANDS),
                        SurfaceRules.sequence(
                                SurfaceRules.ifTrue(floor0, grass),
                                SurfaceRules.ifTrue(floor4, dirt)
                        )
                ),

                // Barrenlands: noise-patched rooted dirt/cobblestone/coarse dirt, 3 dirt subsurface layers
                SurfaceRules.ifTrue(SurfaceRules.isBiome(ModBiomes.BARRENLANDS),
                        SurfaceRules.sequence(
                                SurfaceRules.ifTrue(floor0, SurfaceRules.sequence(
                                        SurfaceRules.ifTrue(patchHigh, rootedDirt),
                                        SurfaceRules.ifTrue(patchMid,  cobblestone),
                                        SurfaceRules.ifTrue(patchLow,  coarseDirt)
                                )),
                                SurfaceRules.ifTrue(floor3, dirt)
                        )
                ),

                // 2. Global deepslate transition
                SurfaceRules.ifTrue(
                        SurfaceRules.verticalGradient("minecraft:deepslate",
                                VerticalAnchor.absolute(0), VerticalAnchor.absolute(8)),
                        deepslate
                )
        );
    }
}