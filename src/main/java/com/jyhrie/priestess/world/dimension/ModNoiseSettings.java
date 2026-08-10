package com.jyhrie.priestess.world.dimension;

import com.jyhrie.priestess.Priestess;
import com.jyhrie.priestess.block.ModBlocks;
import com.jyhrie.priestess.world.terra.TerraElevationFunction;
import com.jyhrie.priestess.world.terra.TerraMap;
import com.jyhrie.priestess.world.terra.TerraReliefFunction;
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
 *     terrainHeight = mapHeight(mapElevation)                   // base height, from elevation.png
 *                   + mapRelief * shoreDamping(mapElevation)    // how much local relief, from relief.png
 *                     * reliefVariation(erosion)                // varied along a range
 *                     * ridgeShape(ridges)                      // where the spurs run
 * </pre>
 *
 * <p>Two painted maps, two jobs: {@code elevation.png} decides how high the ground is and
 * {@code relief.png} decides how much it rises and falls once it is there. They are
 * independent on purpose — relief used to be a spline over elevation, which made
 * brightening a mountain quietly make it bumpier as well as taller and left a high
 * plateau unpaintable.
 * plus a 3D detail noise added to the density itself, which is what produces
 * cliffs, ledges and overhangs rather than a smooth height field.
 *
 * <h2>The map, not noise</h2>
 * {@code mapElevation} is {@link com.jyhrie.priestess.world.terra.TerraElevationFunction},
 * reading {@code data/priestess/terra/elevation.png}. There is no continentalness noise:
 * where the land is, is authored. Noise only supplies detail below the map's 16-block
 * resolution. Every knot in {@code mapHeight} sits on a
 * {@link com.jyhrie.priestess.world.terra.TerraSlot} band edge, which is what keeps the
 * height of a place and the biome chosen for it in agreement.
 */
public class ModNoiseSettings {

    public static final ResourceKey<NoiseGeneratorSettings> TERRA_SETTINGS = ResourceKey.create(
            Registries.NOISE_SETTINGS,
            new ResourceLocation(Priestess.MOD_ID, "settings")
    );

    /**
     * The world width {@code rangeScale} was tuned at. Spur size is derived from this and
     * {@link TerraMap#WORLD_WIDTH_BLOCKS} — deliberately <b>not</b> from blocks-per-pixel.
     *
     * <p>A mountain spur is a feature of the world, measured in blocks. Blocks-per-pixel
     * moves for two unrelated reasons — resizing the world, or repainting the map at a
     * different resolution — and only the first should change how big a mountain is.
     * Keying off it conflated the two: repainting Terra at 4092px instead of 1024px took
     * the map to 16 blocks/px, drove {@code rangeScale} to 2.0, and made every ridge
     * octave eight times too fine. Kjerag came out as 16-block gravel with ±28 blocks of
     * relief on it, on a slope that only climbs 23 blocks in 865.
     */
    private static final double TUNED_AT_WORLD_WIDTH_BLOCKS = 65_536.0;

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
        // The scale follows the world's size in blocks. A range's spurs are a feature of
        // the world, so if the world is rescaled they have to rescale with it, or a
        // quarter-size world gets ranges built out of full-size mountains. Repainting the
        // map at a finer resolution is NOT a rescale and must not touch them — see
        // TUNED_AT_WORLD_WIDTH_BLOCKS. (The detail noise further down does not scale at
        // all — that is surface texture, and a boulder is a boulder whatever size the
        // continent is.)
        //
        // At today's 32,768-block world this is 0.5, putting the ridge octaves at
        // 256 / 128 / 64 blocks. ridgeShape folds that, so spurs land ~64 blocks apart —
        // half what they were at 65,536, which is the point: the continent halved, so its
        // mountains did too, and a range still has the same number of spurs across it.
        // This value is baked into settings.json by runData; the game never re-derives it.
        double rangeScale = 0.25 * (TUNED_AT_WORLD_WIDTH_BLOCKS / TerraMap.WORLD_WIDTH_BLOCKS);
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
        // Painted, in data/priestess/terra/relief.png — grey 0 is dead flat and grey 255
        // is a broken crag. This used to be a spline over mapElevation, which meant
        // brightening the elevation map made a place bumpier as well as taller and put a
        // high plateau or a rugged lowland out of reach. The two are separate maps now.
        DensityFunction mapRelief = DensityFunctions.flatCache(TerraReliefFunction.INSTANCE);

        // The one thing relief still takes from elevation, and only near the water: a
        // painted crag running into the sea would break the waterline into a scatter of
        // one-block islands and potholes, so relief is damped to nothing below the shore
        // and comes fully in by the time the ground is properly inland. Above that it is
        // flat, so height genuinely does not affect ruggedness any more.
        DensityFunction shoreDamping = spline(mapElevation,
                -1.00f, 0.00f,
                -0.32f, 0.00f,   // SEA / SHORE
                -0.20f, 0.00f,   // SHORE / LOWLAND — the whole shore band stays clean
                -0.04f, 1.00f,   // LOWLAND / FLATS — full relief from here on
                 1.00f, 1.00f);

        DensityFunction ruggedness = DensityFunctions.mul(mapRelief, shoreDamping);

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
        // made low ground look chewed rather than smooth.
        //
        // On land it follows the painted relief map, so one grey value controls both the
        // shape of a place's hills and how broken their faces are — there is no second
        // map to keep in step. The fraction is what makes grey 255 land on the +/-13
        // blocks of crag face this was tuned to, and grey 80 on the +/-4 of walkable
        // flats it used to give.
        DensityFunction landDetail = DensityFunctions.mul(
                DensityFunctions.mul(mapRelief, shoreDamping),
                DensityFunctions.constant(0.28));

        // Underwater is the exception, and stays keyed to elevation: the relief map is
        // about land, nobody paints the sea floor, and the shore still has to be smooth
        // or the waterline becomes a scatter of one-block islands and potholes. This term
        // is zero everywhere landDetail is non-zero, so the two never fight.
        DensityFunction waterDetail = spline(mapElevation,
                -1.00f, 0.055f,   // +/- 7 blocks: broken sea floor
                -0.32f, 0.030f,   // SEA / SHORE
                -0.20f, 0.008f,   // shore: nearly nothing
                -0.04f, 0.000f,   // on land the relief map takes over
                 1.00f, 0.000f);

        DensityFunction detailAmount = DensityFunctions.add(waterDetail, landDetail);

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
        var yellowTerra   = SurfaceRules.state(Blocks.YELLOW_TERRACOTTA.defaultBlockState());

        // The mod's own blocks, so Iberia looks like Iberia and not like vanilla desert.
        var iberianSand      = SurfaceRules.state(ModBlocks.IBERIAN_SAND.get().defaultBlockState());
        var iberianSandstone = SurfaceRules.state(ModBlocks.IBERIAN_SANDSTONE.get().defaultBlockState());
        var permafrost       = SurfaceRules.state(ModBlocks.PERMAFROST.get().defaultBlockState());
        var blackIce         = SurfaceRules.state(ModBlocks.BLACK_ICE.get().defaultBlockState());

        // Kept for zones that are not painted yet — the arid south (redSand, redSandstone,
        // orangeTerra, smoothSand, sandstone), Victoria's moors (mossyCobble, cobblestone),
        // and the coasts and seabeds that get their own colours later (siestaSand,
        // paleBeachSand, deadSeabed). Unused today; delete only if you drop the zone too.
        var siestaSand       = SurfaceRules.state(ModBlocks.SIESTA_SAND.get().defaultBlockState());
        var paleBeachSand    = SurfaceRules.state(ModBlocks.PALE_BEACH_SAND.get().defaultBlockState());
        var deadSeabed       = SurfaceRules.state(ModBlocks.DEAD_SEABED.get().defaultBlockState());

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

                // ══ One rule per zone ═════════════════════════════════════════
                // Biomes come from the painted map now, one colour to one biome, so this
                // list is exactly TerraRegion's list. A zone with no rule here generates as
                // bare stone — that is the failure mode to look for if a new zone comes out
                // grey.
                //
                // Since a zone spans every elevation it is painted over, the height checks
                // below (aboveSnowline / aboveTreeline) are doing the work the old eight
                // biome slots used to do. That is the intended division: elevation varies
                // the *surface* within a zone, it no longer changes which zone you are in.

                // ── The sea ───────────────────────────────────────────────────
                // 95% of this zone is deep sea floor, but it also covers the islands the
                // elevation map lifts above the waterline, so it cannot be gravel alone.
                SurfaceRules.ifTrue(SurfaceRules.isBiome(ModBiomes.OCEAN),
                        SurfaceRules.sequence(
                                SurfaceRules.ifTrue(floor0, SurfaceRules.sequence(
                                        SurfaceRules.ifTrue(patchHigh, sand),
                                        SurfaceRules.ifTrue(patchMid,  gravel),
                                        SurfaceRules.ifTrue(patchLow,  clay)
                                )),
                                SurfaceRules.ifTrue(floor4, gravel),
                                SurfaceRules.ifTrue(floor8, deepslate)
                        )
                ),

                // ── The north ─────────────────────────────────────────────────
                SurfaceRules.ifTrue(SurfaceRules.isBiome(ModBiomes.INFY_ICEFIELD),
                        SurfaceRules.sequence(
                                SurfaceRules.ifTrue(floor0, snow),
                                SurfaceRules.ifTrue(floor3, packedIce),
                                SurfaceRules.ifTrue(floor8, blueIce)
                        )
                ),

                SurfaceRules.ifTrue(SurfaceRules.isBiome(ModBiomes.SAMI),
                        SurfaceRules.sequence(
                                SurfaceRules.ifTrue(floor0, snow),
                                SurfaceRules.ifTrue(floor3, ice),
                                SurfaceRules.ifTrue(floor8, packedIce)
                        )
                ),

                // ── Ursus, three ways ─────────────────────────────────────────
                // The empire's three climates differ here more than anywhere: frozen duff
                // in the north, dry cropped ground in the steppe, ordinary forest floor in
                // the south. This is what carries the split now that they are one biome each.
                SurfaceRules.ifTrue(SurfaceRules.isBiome(ModBiomes.URSUS_COLD),
                        SurfaceRules.sequence(
                                SurfaceRules.ifTrue(aboveTreeline,
                                        SurfaceRules.ifTrue(floor0, snow)),
                                SurfaceRules.ifTrue(floor0, SurfaceRules.sequence(
                                        SurfaceRules.ifTrue(patchHigh, snow),
                                        SurfaceRules.ifTrue(patchMid,  podzol),
                                        SurfaceRules.ifTrue(patchLow,  coarseDirt)
                                )),
                                SurfaceRules.ifTrue(floor2, permafrost),
                                SurfaceRules.ifTrue(floor8, dirt)
                        )
                ),

                SurfaceRules.ifTrue(SurfaceRules.isBiome(ModBiomes.URSUS_DRY),
                        SurfaceRules.sequence(
                                SurfaceRules.ifTrue(floor0, SurfaceRules.sequence(
                                        SurfaceRules.ifTrue(patchHigh, coarseDirt),
                                        SurfaceRules.ifTrue(patchMid,  grass),
                                        SurfaceRules.ifTrue(patchLow,  coarseDirt)
                                )),
                                SurfaceRules.ifTrue(floor3, dirt),
                                SurfaceRules.ifTrue(floor8, rootedDirt)
                        )
                ),

                SurfaceRules.ifTrue(SurfaceRules.isBiome(ModBiomes.URSUS_WARM),
                        SurfaceRules.sequence(
                                SurfaceRules.ifTrue(floor0, SurfaceRules.sequence(
                                        SurfaceRules.ifTrue(patchHigh, podzol),
                                        SurfaceRules.ifTrue(patchMid,  grass),
                                        SurfaceRules.ifTrue(patchLow,  coarseDirt)
                                )),
                                SurfaceRules.ifTrue(floor3, dirt),
                                SurfaceRules.ifTrue(floor8, rootedDirt)
                        )
                ),

                // ── The range ─────────────────────────────────────────────────
                // Kjerag: alpine rather than polar — snow over stone, not snow over ice.
                SurfaceRules.ifTrue(SurfaceRules.isBiome(ModBiomes.KJERAG),
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

                // Karlan is the mountain's body: more exposed rock, less standing snow,
                // and black ice where the glaciers have scoured it.
                SurfaceRules.ifTrue(SurfaceRules.isBiome(ModBiomes.MOUNT_KARLAN),
                        SurfaceRules.sequence(
                                SurfaceRules.ifTrue(aboveSnowline, SurfaceRules.sequence(
                                        SurfaceRules.ifTrue(floor0, snow),
                                        SurfaceRules.ifTrue(floor4, blackIce)
                                )),
                                SurfaceRules.ifTrue(floor0, SurfaceRules.sequence(
                                        SurfaceRules.ifTrue(patchHigh, snow),
                                        SurfaceRules.ifTrue(patchMid,  andesite),
                                        SurfaceRules.ifTrue(patchLow,  tuff)
                                )),
                                SurfaceRules.ifTrue(floor4, andesite)
                        )
                ),

                // ── The heartland and the west ────────────────────────────────
                SurfaceRules.ifTrue(SurfaceRules.isBiome(ModBiomes.KAZIMIERZ),
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

                SurfaceRules.ifTrue(SurfaceRules.isBiome(ModBiomes.COLUMBIA),
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

                // Iberia keeps its own sand: a third of this zone is coast, and vanilla
                // desert sand is the wrong colour for it.
                SurfaceRules.ifTrue(SurfaceRules.isBiome(ModBiomes.IBERIA_LAND),
                        SurfaceRules.sequence(
                                SurfaceRules.ifTrue(floor0, SurfaceRules.sequence(
                                        SurfaceRules.ifTrue(patchHigh, iberianSand),
                                        SurfaceRules.ifTrue(patchMid,  coarseDirt),
                                        SurfaceRules.ifTrue(patchLow,  grass)
                                )),
                                SurfaceRules.ifTrue(floor3, iberianSand),
                                SurfaceRules.ifTrue(floor8, iberianSandstone)
                        )
                ),

                // ── The east ──────────────────────────────────────────────────
                SurfaceRules.ifTrue(SurfaceRules.isBiome(ModBiomes.YAN),
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

                SurfaceRules.ifTrue(SurfaceRules.isBiome(ModBiomes.HIGASHI_COLD),
                        SurfaceRules.sequence(
                                SurfaceRules.ifTrue(aboveSnowline,
                                        SurfaceRules.ifTrue(floor0, snow)),
                                SurfaceRules.ifTrue(floor0, SurfaceRules.sequence(
                                        SurfaceRules.ifTrue(patchHigh, snow),
                                        SurfaceRules.ifTrue(patchMid,  podzol),
                                        SurfaceRules.ifTrue(patchLow,  grass)
                                )),
                                SurfaceRules.ifTrue(floor3, dirt),
                                SurfaceRules.ifTrue(floor8, rootedDirt)
                        )
                ),

                // Paddy country: wet ground, clay under it, mud in the low pockets.
                SurfaceRules.ifTrue(SurfaceRules.isBiome(ModBiomes.HIGASHI_WARM),
                        SurfaceRules.sequence(
                                SurfaceRules.ifTrue(floor0, SurfaceRules.sequence(
                                        SurfaceRules.ifTrue(patchHigh, grass),
                                        SurfaceRules.ifTrue(patchMid,  mud),
                                        SurfaceRules.ifTrue(patchLow,  moss)
                                )),
                                SurfaceRules.ifTrue(floor2, clay),
                                SurfaceRules.ifTrue(floor8, dirt)
                        )
                ),

                // ── The south ─────────────────────────────────────────────────
                // Kazdel: exposed crag faces go bare basalt, lower slopes stay scree.
                SurfaceRules.ifTrue(SurfaceRules.isBiome(ModBiomes.KAZDEL),
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

                // ── Unpainted ─────────────────────────────────────────────────
                // Loud on purpose. Unzoned ground should look wrong from a distance, not
                // pass for a design decision you forgot you made.
                SurfaceRules.ifTrue(SurfaceRules.isBiome(ModBiomes.TEMPORARY_LAYER),
                        SurfaceRules.sequence(
                                SurfaceRules.ifTrue(floor0, yellowTerra),
                                SurfaceRules.ifTrue(floor4, coarseDirt)
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
