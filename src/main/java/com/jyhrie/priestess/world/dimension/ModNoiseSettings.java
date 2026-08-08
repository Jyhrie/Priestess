package com.jyhrie.priestess.world.dimension;

import com.jyhrie.priestess.Priestess;
import com.jyhrie.priestess.block.ModBlocks;
import com.jyhrie.priestess.world.terra.TerraElevationFunction;
import com.jyhrie.priestess.world.terra.TerraMap;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.CubicSpline;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.*;
import net.minecraft.world.level.levelgen.placement.CaveSurface;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

import java.util.List;

/**
 * Terrain shape and surface composition for Terra.
 *
 * <h2>The height equation</h2>
 * The surface sits where {@code finalDensity} crosses zero, i.e. where
 * <pre>
 *     yClampedGradient(-64, 320, 1.5, -1.5) + terrainHeight == 0
 * </pre>
 * That gradient falls by exactly 1/128 per block, which gives the one formula
 * worth memorising when tuning this file:
 * <pre>
 *     surfaceY = 128 + 128 * terrainHeight
 * </pre>
 * So a terrainHeight of +0.5 puts the surface at y=192 and -0.5 puts it at y=64.
 * Every spline below is written in those units with the resulting y in a comment.
 *
 * <h2>How terrainHeight is built</h2>
 * <pre>
 *     terrainHeight = mapHeight(mapElevation)                   // base elevation, from the map
 *                   + ruggedness(mapElevation)                  // how much local relief
 *                     * reliefVariation(erosion)                // varied along a range
 *                     * ridgeShape(ridges)                      // where the spurs run
 * </pre>
 * plus a 3D detail noise added to the density itself, which is what produces
 * cliffs, ledges and overhangs rather than a smooth height field.
 *
 * <h2>The map, not noise</h2>
 * {@code mapElevation} is {@link com.jyhrie.priestess.world.terra.TerraElevationFunction},
 * reading {@code data/priestess/terra/elevation.png}. There is no continentalness noise:
 * where the land is, is authored. Noise only supplies detail below the map's 128-block
 * resolution. Every knot in {@code mapHeight} sits on a
 * {@link com.jyhrie.priestess.world.terra.TerraSlot} band edge, which is what keeps the
 * height of a place and the biome chosen for it in agreement.
 */
public class ModNoiseSettings {

    public static final ResourceKey<NoiseGeneratorSettings> TERRA_SETTINGS = ResourceKey.create(
            Registries.NOISE_SETTINGS,
            new ResourceLocation(Priestess.MOD_ID, "settings")
    );

    // ── Noise keys ────────────────────────────────────────────────────────────
    // There is no continentalness, temperature or vegetation noise any more. Where a
    // place is and what it is like are read off the Terra map; noise now only supplies
    // local detail that the map is too coarse to carry.
    public static final ResourceKey<NormalNoise.NoiseParameters> EROSION       = noiseKey("erosion");
    public static final ResourceKey<NormalNoise.NoiseParameters> RIDGES        = noiseKey("ridges");
    public static final ResourceKey<NormalNoise.NoiseParameters> DETAIL        = noiseKey("detail");
    public static final ResourceKey<NormalNoise.NoiseParameters> SURFACE_PATCH = noiseKey("surface_patch");

    private static ResourceKey<NormalNoise.NoiseParameters> noiseKey(String name) {
        return ResourceKey.create(Registries.NOISE, new ResourceLocation(Priestess.MOD_ID, name));
    }

    // ── Noise bootstrap ───────────────────────────────────────────────────────
    // firstOctave n => base wavelength of 2^-n blocks. The two range noises are sampled
    // at xz_scale 0.25, so their effective wavelengths are four times these.
    public static void bootstrapNoise(BootstapContext<NormalNoise.NoiseParameters> ctx) {
        // Ridges: folded by ridgeShape() into mountain chains rather than blobs. The map
        // says where a range runs; this says what its individual spurs look like.
        ctx.register(RIDGES,      new NormalNoise.NoiseParameters(-7,  List.of(1.0, 2.0, 1.0, 0.0, 0.0, 0.0)));
        // Erosion: varies how sharp the relief is from one part of a range to the next.
        ctx.register(EROSION,     new NormalNoise.NoiseParameters(-9,  List.of(1.0, 1.0, 0.0, 1.0, 1.0)));
        // Detail: the 3D noise that turns a height field into actual terrain.
        ctx.register(DETAIL,      new NormalNoise.NoiseParameters(-7,  List.of(1.0, 0.6, 0.3, 0.15)));
        // Surface patch: ~16-block blotches for the mottled wasteland surfaces.
        ctx.register(SURFACE_PATCH, new NormalNoise.NoiseParameters(-4, List.of(1.0, 1.0, 1.0)));
    }

    // ── Settings bootstrap ────────────────────────────────────────────────────
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

    // =========================================================================
    // TERRAIN
    // =========================================================================

    private static NoiseRouter createNoiseRouter(BootstapContext<NoiseGeneratorSettings> context) {
        HolderGetter<NormalNoise.NoiseParameters> noises = context.lookup(Registries.NOISE);

        // --- The map ---------------------------------------------------------
        // Elevation comes from data/priestess/terra/elevation.png, not from noise. This
        // is the same function TerraMapBiomeSource reads, which is the whole reason the
        // two agree: a map-chosen biome and a noise-chosen height would put Iberian
        // beaches halfway up a mountain.
        //
        // Range is [-1,1], being the PNG's [0,1] doubled and shifted. Every knot below is
        // written in that space, with the matching TerraSlot band named in the comment.
        DensityFunction mapElevation = DensityFunctions.flatCache(TerraElevationFunction.INSTANCE);

        // Local variety still comes from noise — the map says where the mountains are,
        // not what any individual ridge looks like.
        //
        // The scale follows TerraMap.BLOCKS_PER_PIXEL. A range's spurs are a map-scale
        // feature, so if the world is rescaled they have to rescale with it, or a
        // quarter-size world gets ranges built out of full-size mountains. (The detail
        // noise further down does NOT scale — that is surface texture, and a boulder is
        // a boulder whatever size the continent is.)
        // Loads the map, which datagen does anyway for the preview. At runtime this value
        // is already baked into settings.json, so the game never re-derives it.
        double rangeScale = 0.25 * (128.0 / TerraMap.get().blocksPerPixel());
        DensityFunction ridges = DensityFunctions.flatCache(
                DensityFunctions.noise(noises.getOrThrow(RIDGES), rangeScale, 0.0));
        DensityFunction erosion = DensityFunctions.flatCache(
                DensityFunctions.noise(noises.getOrThrow(EROSION), rangeScale, 0.0));

        // --- Base elevation from the map -------------------------------------
        // Knots sit exactly on the TerraSlot band edges, so a point the biome source
        // calls "shore" generates at a height that actually is one.
        DensityFunction mapHeight = spline(mapElevation,
                -1.00f, -0.781f,  // y  28  0.00  the abyss
                -0.68f, -0.516f,  // y  62  0.16  DEEP_SEA / SEA
                -0.32f, -0.094f,  // y 116  0.34  SEA / SHORE
                -0.26f, -0.031f,  // y 124  0.37  SEA LEVEL, inside the shore band
                -0.20f,  0.023f,  // y 131  0.40  SHORE / LOWLAND
                -0.04f,  0.070f,  // y 137  0.48  LOWLAND / FLATS
                 0.24f,  0.172f,  // y 150  0.62  FLATS / MIDLAND
                 0.48f,  0.313f,  // y 168  0.74  MIDLAND / HILLS
                 0.72f,  0.531f,  // y 196  0.86  HILLS / MOUNTAIN
                 1.00f,  0.906f); // y 244  1.00  the highest peaks

        // --- How much local relief sits on top of that ------------------------
        // Near zero at sea and on the shore, so coastlines stay clean; large in the
        // mountains, where the ridge noise is what turns a smooth mapped dome into an
        // actual range with spurs and valleys.
        DensityFunction ruggedness = spline(mapElevation,
                -1.00f, 0.000f,
                -0.32f, 0.004f,   // sea
                -0.20f, 0.010f,   // shore: +/- 1 block
                -0.04f, 0.022f,   // lowland
                 0.24f, 0.048f,   // flats: +/- 6 blocks, still walkable
                 0.48f, 0.105f,   // midland
                 0.72f, 0.195f,   // hills
                 1.00f, 0.300f);  // mountain: +/- 38 blocks

        // --- Where the ranges actually run -----------------------------------
        // Folded so peaks land on |ridges| ~ 0.65, giving long chains with basins
        // between them, instead of isolated lumps.
        DensityFunction ridgeShape = spline(ridges,
                -1.00f,  0.00f,
                -0.65f,  1.00f,   // ridge line
                 0.00f, -0.20f,   // basin
                 0.65f,  1.00f,   // ridge line
                 1.00f,  0.00f);

        // A little erosion noise on top, so two mountains on the same mapped ridge are
        // not the same mountain.
        DensityFunction reliefVariation = spline(erosion,
                -1.00f, 1.25f,
                 0.00f, 1.00f,
                 1.00f, 0.72f);

        // terrainHeight is purely 2D, so cache it per column.
        DensityFunction terrainHeight = DensityFunctions.cache2d(
                DensityFunctions.add(
                        mapHeight,
                        DensityFunctions.mul(
                                DensityFunctions.mul(ruggedness, reliefVariation), ridgeShape)
                )
        );

        // depth == 0 at the surface, positive underground. Used both for terrain and
        // for the biome sampler's "depth" channel.
        DensityFunction depth = DensityFunctions.add(
                DensityFunctions.yClampedGradient(-64, 320, 1.5, -1.5),
                terrainHeight
        );

        // --- The 3D detail noise ---------------------------------------------
        // yScale below xzScale keeps it vertically coherent, so it carves cliffs and
        // ledges rather than swiss cheese.
        //
        // Its amplitude is no longer a constant. A flat 0.08 everywhere meant the wastes
        // and the beaches got the same +/-10 blocks of churn as the crags, which is what
        // made low ground look chewed rather than smooth. Now the roughness follows the
        // relief class, so terrain that is supposed to be walkable actually is.
        // Both the roughness and the shore damping now key off the map elevation, so a
        // beach on the map is a smooth beach in the world.
        DensityFunction detailAmount = spline(mapElevation,
                -1.00f, 0.055f,   // +/- 7 blocks: broken sea floor
                -0.32f, 0.030f,
                -0.20f, 0.012f,   // shore: nearly nothing, so the waterline is an edge
                                  // and not a scatter of one-block islands and potholes
                -0.04f, 0.026f,
                 0.24f, 0.034f,   // flats: +/- 4 blocks, still walkable
                 0.48f, 0.058f,
                 0.72f, 0.082f,
                 1.00f, 0.100f);  // +/- 13 blocks: broken crag faces

        DensityFunction detail = DensityFunctions.mul(
                detailAmount,
                DensityFunctions.noise(noises.getOrThrow(DETAIL), 0.5, 0.2)
        );

        DensityFunction finalDensity = DensityFunctions.interpolated(
                DensityFunctions.add(depth, detail)
        );

        DensityFunction zero = DensityFunctions.zero();

        // temperature and vegetation feed the Climate.Sampler, and nothing reads it any
        // more: TerraMapBiomeSource ignores the sampler it is handed, because the map
        // decides what a place is. They stay wired to zero rather than deleted because
        // NoiseRouter requires them, and a constant is honest about them being unused.
        return new NoiseRouter(
                /* barrier */                        zero,
                /* fluidLevelFloodedness */          DensityFunctions.constant(-1.0),
                /* fluidLevelSpread */               zero,
                /* lava */                           zero,
                /* temperature */                    zero,
                /* vegetation */                     zero,
                /* continents */                     mapElevation,
                /* erosion */                        erosion,
                /* depth */                          depth,
                /* ridges */                         ridges,
                /* initialDensityWithoutJaggedness */ depth,
                /* finalDensity */                   finalDensity,
                /* veinToggle */                     zero,
                /* veinRidged */                     zero,
                /* veinGap */                        zero
        );
    }

    /**
     * Builds a cubic spline over {@code coordinate} from flat (location, value) pairs.
     * Every knot gets a zero derivative, which keeps the curve monotone between knots
     * and stops it overshooting outside the range you wrote down.
     */
    private static DensityFunction spline(DensityFunction coordinate, float... locationValuePairs) {
        CubicSpline.Builder<DensityFunctions.Spline.Point, DensityFunctions.Spline.Coordinate> builder =
                CubicSpline.builder(new DensityFunctions.Spline.Coordinate(Holder.direct(coordinate)));
        for (int i = 0; i < locationValuePairs.length; i += 2) {
            builder.addPoint(locationValuePairs[i], locationValuePairs[i + 1], 0.0f);
        }
        return DensityFunctions.spline(builder.build());
    }

    // =========================================================================
    // SURFACE
    // =========================================================================

    private static SurfaceRules.RuleSource createSurfaceRules() {
        // Block shorthands
        var bedrock       = SurfaceRules.state(Blocks.BEDROCK.defaultBlockState());
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
        var tuff          = SurfaceRules.state(Blocks.TUFF.defaultBlockState());
        var calcite       = SurfaceRules.state(Blocks.CALCITE.defaultBlockState());
        var podzol        = SurfaceRules.state(Blocks.PODZOL.defaultBlockState());
        var moss          = SurfaceRules.state(Blocks.MOSS_BLOCK.defaultBlockState());
        var mud           = SurfaceRules.state(Blocks.MUD.defaultBlockState());
        var clay          = SurfaceRules.state(Blocks.CLAY.defaultBlockState());
        var andesite      = SurfaceRules.state(Blocks.ANDESITE.defaultBlockState());
        var smoothSand    = SurfaceRules.state(Blocks.SMOOTH_SANDSTONE.defaultBlockState());
        var mossyCobble   = SurfaceRules.state(Blocks.MOSSY_COBBLESTONE.defaultBlockState());

        // The mod's own blocks, so Iberia looks like Iberia and not like vanilla desert.
        var iberianSand      = SurfaceRules.state(ModBlocks.IBERIAN_SAND.get().defaultBlockState());
        var iberianSandstone = SurfaceRules.state(ModBlocks.IBERIAN_SANDSTONE.get().defaultBlockState());

        // Noise threshold conditions for surface_patch (~16-block blotches)
        var patchHigh = SurfaceRules.noiseCondition(SURFACE_PATCH,  0.4,  1.0);
        var patchMid  = SurfaceRules.noiseCondition(SURFACE_PATCH,  0.1,  0.4);
        var patchLow  = SurfaceRules.noiseCondition(SURFACE_PATCH, -0.2,  0.1);

        // Floor depth conditions. stoneDepthCheck(N) matches EVERY depth from 0..N,
        // so order these shallowest-first inside a sequence.
        var floor0 = SurfaceRules.stoneDepthCheck(0, false, 0, CaveSurface.FLOOR);
        var floor1 = SurfaceRules.stoneDepthCheck(1, false, 0, CaveSurface.FLOOR);
        var floor2 = SurfaceRules.stoneDepthCheck(2, false, 0, CaveSurface.FLOOR);
        var floor3 = SurfaceRules.stoneDepthCheck(3, false, 0, CaveSurface.FLOOR);
        var floor4 = SurfaceRules.stoneDepthCheck(4, false, 0, CaveSurface.FLOOR);
        var floor8 = SurfaceRules.stoneDepthCheck(8, false, 0, CaveSurface.FLOOR);

        // Now that peaks actually reach y~240, high ground gets a snowline.
        var aboveSnowline = SurfaceRules.yBlockCheck(VerticalAnchor.absolute(185), 0);
        var aboveTreeline = SurfaceRules.yBlockCheck(VerticalAnchor.absolute(165), 0);

        return SurfaceRules.sequence(

                // 1. Bedrock floor — must stay first.
                SurfaceRules.ifTrue(
                        SurfaceRules.verticalGradient("minecraft:bedrock_floor",
                                VerticalAnchor.aboveBottom(0), VerticalAnchor.aboveBottom(5)),
                        bedrock
                ),

                // ── Seas ──────────────────────────────────────────────────────
                // floor4 already covers depth 0, so one rule per biome is enough.
                SurfaceRules.ifTrue(SurfaceRules.isBiome(ModBiomes.SEA_OF_SILENCE),
                        SurfaceRules.ifTrue(floor4, gravel)),

                SurfaceRules.ifTrue(SurfaceRules.isBiome(ModBiomes.AEGIR_DEPTHS),
                        SurfaceRules.ifTrue(floor4, deepslate)),

                SurfaceRules.ifTrue(SurfaceRules.isBiome(ModBiomes.SIESTA_SEA),
                        SurfaceRules.ifTrue(floor4, basalt)),

                SurfaceRules.ifTrue(SurfaceRules.isBiome(ModBiomes.BOLIVAR_DEPTHS),
                        SurfaceRules.ifTrue(floor4, blackstone)),

                // ── Ice ───────────────────────────────────────────────────────
                SurfaceRules.ifTrue(SurfaceRules.isBiome(ModBiomes.SAMI_SNOWFIELDS),
                        SurfaceRules.sequence(
                                SurfaceRules.ifTrue(floor0, snow),
                                SurfaceRules.ifTrue(floor3, ice),
                                SurfaceRules.ifTrue(floor8, packedIce)
                        )
                ),

                SurfaceRules.ifTrue(SurfaceRules.isBiome(ModBiomes.INFY_ICEFIELDS),
                        SurfaceRules.sequence(
                                SurfaceRules.ifTrue(floor0, snow),
                                SurfaceRules.ifTrue(floor3, packedIce),
                                SurfaceRules.ifTrue(floor8, blueIce)
                        )
                ),

                // Ægir's shelf is the frozen half of the shore band, so it is part beach
                // and part sea floor — the patch noise mixes drifted snow with the gravel
                // showing through where the ice has been scoured off.
                SurfaceRules.ifTrue(SurfaceRules.isBiome(ModBiomes.AEGIR_SHELF),
                        SurfaceRules.sequence(
                                SurfaceRules.ifTrue(floor0, SurfaceRules.sequence(
                                        SurfaceRules.ifTrue(patchHigh, snow),
                                        SurfaceRules.ifTrue(patchMid,  packedIce),
                                        SurfaceRules.ifTrue(patchLow,  gravel)
                                )),
                                SurfaceRules.ifTrue(floor3, packedIce),
                                SurfaceRules.ifTrue(floor8, gravel)
                        )
                ),

                // Kjerag: alpine rather than polar — snow over stone, not snow over ice.
                SurfaceRules.ifTrue(SurfaceRules.isBiome(ModBiomes.KJERAG_SLOPES),
                        SurfaceRules.sequence(
                                SurfaceRules.ifTrue(aboveSnowline, SurfaceRules.sequence(
                                        SurfaceRules.ifTrue(floor0, snow),
                                        SurfaceRules.ifTrue(floor4, packedIce)
                                )),
                                SurfaceRules.ifTrue(floor0, snow),
                                SurfaceRules.ifTrue(floor2, calcite),
                                SurfaceRules.ifTrue(floor8, andesite)
                        )
                ),

                // ── Arid / blasted ────────────────────────────────────────────
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

                // Sargon: the dune sea that lets Dossoles' beaches become Foehn's hotlands
                // gradually, instead of sand meeting red rock at a line.
                SurfaceRules.ifTrue(SurfaceRules.isBiome(ModBiomes.SARGON_DUNES),
                        SurfaceRules.sequence(
                                SurfaceRules.ifTrue(floor0, SurfaceRules.sequence(
                                        SurfaceRules.ifTrue(patchHigh, smoothSand),
                                        SurfaceRules.ifTrue(patchMid,  sand),
                                        SurfaceRules.ifTrue(patchLow,  sand)
                                )),
                                SurfaceRules.ifTrue(floor3, sand),
                                SurfaceRules.ifTrue(floor8, sandstone)
                        )
                ),

                // Kazdel: exposed crag faces go bare basalt, lower slopes stay scree.
                SurfaceRules.ifTrue(SurfaceRules.isBiome(ModBiomes.KAZDEL_CRAGS),
                        SurfaceRules.sequence(
                                SurfaceRules.ifTrue(aboveTreeline,
                                        SurfaceRules.ifTrue(floor2, blackstone)),
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

                // ── Coasts ────────────────────────────────────────────────────
                SurfaceRules.ifTrue(SurfaceRules.isBiome(ModBiomes.IBERIAN_SHORES),
                        SurfaceRules.sequence(
                                SurfaceRules.ifTrue(floor3, iberianSand),
                                SurfaceRules.ifTrue(floor4, iberianSandstone)
                        )
                ),

                SurfaceRules.ifTrue(SurfaceRules.isBiome(ModBiomes.DOSSOLES_BEACHES),
                        SurfaceRules.sequence(
                                SurfaceRules.ifTrue(floor3, sand),
                                SurfaceRules.ifTrue(floor4, sandstone)
                        )
                ),

                // Siracusa: pale limestone headlands broken by pockets of sand.
                SurfaceRules.ifTrue(SurfaceRules.isBiome(ModBiomes.SIRACUSAN_COAST),
                        SurfaceRules.sequence(
                                SurfaceRules.ifTrue(floor0, SurfaceRules.sequence(
                                        SurfaceRules.ifTrue(patchHigh, calcite),
                                        SurfaceRules.ifTrue(patchMid,  sand),
                                        SurfaceRules.ifTrue(patchLow,  gravel)
                                )),
                                SurfaceRules.ifTrue(floor3, sand),
                                SurfaceRules.ifTrue(floor8, sandstone)
                        )
                ),

                // ── The neutral belt ──────────────────────────────────────────
                // Ordinary green ground. These exist so the extremes have something to
                // fade through, so they are deliberately the least dramatic surfaces here.

                SurfaceRules.ifTrue(SurfaceRules.isBiome(ModBiomes.URSUS_TAIGA),
                        SurfaceRules.sequence(
                                SurfaceRules.ifTrue(floor0, SurfaceRules.sequence(
                                        SurfaceRules.ifTrue(patchHigh, snow),
                                        SurfaceRules.ifTrue(patchMid,  podzol),
                                        SurfaceRules.ifTrue(patchLow,  coarseDirt)
                                )),
                                SurfaceRules.ifTrue(floor4, dirt)
                        )
                ),

                SurfaceRules.ifTrue(SurfaceRules.isBiome(ModBiomes.KAZIMIERZ_PLAINS),
                        SurfaceRules.sequence(
                                SurfaceRules.ifTrue(floor0, grass),
                                SurfaceRules.ifTrue(floor4, dirt)
                        )
                ),

                SurfaceRules.ifTrue(SurfaceRules.isBiome(ModBiomes.VICTORIAN_MOORS),
                        SurfaceRules.sequence(
                                SurfaceRules.ifTrue(floor0, SurfaceRules.sequence(
                                        SurfaceRules.ifTrue(patchHigh, mossyCobble),
                                        SurfaceRules.ifTrue(patchMid,  grass),
                                        SurfaceRules.ifTrue(patchLow,  coarseDirt)
                                )),
                                SurfaceRules.ifTrue(floor3, dirt),
                                SurfaceRules.ifTrue(floor8, rootedDirt)
                        )
                ),

                SurfaceRules.ifTrue(SurfaceRules.isBiome(ModBiomes.LEITHANIEN_WOODS),
                        SurfaceRules.sequence(
                                SurfaceRules.ifTrue(floor0, SurfaceRules.sequence(
                                        SurfaceRules.ifTrue(patchHigh, podzol),
                                        SurfaceRules.ifTrue(patchMid,  grass),
                                        SurfaceRules.ifTrue(patchLow,  moss)
                                )),
                                SurfaceRules.ifTrue(floor3, dirt),
                                SurfaceRules.ifTrue(floor8, rootedDirt)
                        )
                ),

                SurfaceRules.ifTrue(SurfaceRules.isBiome(ModBiomes.BOLIVAR_MIRE),
                        SurfaceRules.sequence(
                                SurfaceRules.ifTrue(floor0, SurfaceRules.sequence(
                                        SurfaceRules.ifTrue(patchHigh, moss),
                                        SurfaceRules.ifTrue(patchMid,  mud),
                                        SurfaceRules.ifTrue(patchLow,  coarseDirt)
                                )),
                                SurfaceRules.ifTrue(floor2, clay),
                                SurfaceRules.ifTrue(floor8, dirt)
                        )
                ),

                // ── Mountains ─────────────────────────────────────────────────
                // Snowcap, then bare rock, then grass on the lower slopes.
                SurfaceRules.ifTrue(SurfaceRules.isBiome(ModBiomes.YANESE_PEAKS),
                        SurfaceRules.sequence(
                                SurfaceRules.ifTrue(aboveSnowline, SurfaceRules.sequence(
                                        SurfaceRules.ifTrue(floor0, snow),
                                        SurfaceRules.ifTrue(floor4, calcite)
                                )),
                                SurfaceRules.ifTrue(aboveTreeline,
                                        SurfaceRules.ifTrue(floor2, tuff)),
                                SurfaceRules.ifTrue(floor0, grass),
                                SurfaceRules.ifTrue(floor4, dirt)
                        )
                ),

                SurfaceRules.ifTrue(SurfaceRules.isBiome(ModBiomes.HIGASHI_HIGHLANDS),
                        SurfaceRules.sequence(
                                SurfaceRules.ifTrue(aboveSnowline, SurfaceRules.sequence(
                                        SurfaceRules.ifTrue(floor0, snow),
                                        SurfaceRules.ifTrue(floor4, tuff)
                                )),
                                SurfaceRules.ifTrue(floor0, grass),
                                SurfaceRules.ifTrue(floor4, dirt)
                        )
                ),

                // ── The wastes ────────────────────────────────────────────────
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

                // 2. Global deepslate transition — must stay last.
                SurfaceRules.ifTrue(
                        SurfaceRules.verticalGradient("minecraft:deepslate",
                                VerticalAnchor.absolute(0), VerticalAnchor.absolute(8)),
                        deepslate
                )
        );
    }
}
