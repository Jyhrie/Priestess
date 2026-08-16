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
 * <p>The surface sits where {@code finalDensity} crosses zero. The y gradient falls by
 * 1/128 per block, so the formula to tune against is:
 * <pre>
 *     surfaceY = 128 + 128 * terrainHeight
 *
 *     terrainHeight = mapHeight(mapElevation)                   // elevation.png
 *                   + mapRelief * shoreDamping(mapElevation)    // relief.png
 *                     * reliefVariation(erosion)
 *                     * ridgeShape(ridges)
 * </pre>
 *
 * <p>Elevation and relief are separate painted maps on purpose: how high the ground is and
 * how much it rises and falls are independent axes. There is no continentalness noise —
 * noise only supplies detail below the map's 16-block resolution. Every knot in
 * {@code mapHeight} sits on a {@link com.jyhrie.priestess.world.terra.TerraSlot} band edge,
 * which is what keeps a place's height and its chosen biome in agreement.
 */
public class ModNoiseSettings {

    public static final ResourceKey<NoiseGeneratorSettings> TERRA_SETTINGS = ResourceKey.create(
            Registries.NOISE_SETTINGS,
            new ResourceLocation(Priestess.MOD_ID, "settings")
    );

    /**
     * The world width {@code rangeScale} was tuned at. Spur size is derived from this and
     * {@link TerraMap#WORLD_WIDTH_BLOCKS}, deliberately <b>not</b> from blocks-per-pixel:
     * repainting the map at a finer resolution is not a rescale of the world and must not
     * change how big a mountain is.
     */
    private static final double TUNED_AT_WORLD_WIDTH_BLOCKS = 65_536.0;

    public static final ResourceKey<NormalNoise.NoiseParameters> EROSION       = noiseKey("erosion");
    public static final ResourceKey<NormalNoise.NoiseParameters> RIDGES        = noiseKey("ridges");
    public static final ResourceKey<NormalNoise.NoiseParameters> DETAIL        = noiseKey("detail");
    public static final ResourceKey<NormalNoise.NoiseParameters> SURFACE_PATCH = noiseKey("surface_patch");

    private static ResourceKey<NormalNoise.NoiseParameters> noiseKey(String name) {
        return ResourceKey.create(Registries.NOISE, new ResourceLocation(Priestess.MOD_ID, name));
    }

    /** firstOctave n gives a base wavelength of 2^-n blocks; the range noises are sampled at
     *  xz_scale 0.25, so their effective wavelengths are four times these. */
    public static void bootstrapNoise(BootstapContext<NormalNoise.NoiseParameters> ctx) {
        ctx.register(RIDGES,      new NormalNoise.NoiseParameters(-7,  List.of(1.0, 2.0, 1.0, 0.0, 0.0, 0.0)));
        ctx.register(EROSION,     new NormalNoise.NoiseParameters(-9,  List.of(1.0, 1.0, 0.0, 1.0, 1.0)));
        ctx.register(DETAIL,      new NormalNoise.NoiseParameters(-7,  List.of(1.0, 0.6, 0.3, 0.15)));
        ctx.register(SURFACE_PATCH, new NormalNoise.NoiseParameters(-4, List.of(1.0, 1.0, 1.0)));
    }

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

        // The same function TerraMapBiomeSource reads, which is why height and biome agree.
        // Range is [-1,1], being the PNG's [0,1] doubled and shifted.
        DensityFunction mapElevation = DensityFunctions.flatCache(TerraElevationFunction.INSTANCE);

        // Spurs are a feature of the world measured in blocks, so their scale follows the
        // world's size — see TUNED_AT_WORLD_WIDTH_BLOCKS. Baked into settings.json by
        // runData; the game never re-derives it.
        double rangeScale = 0.25 * (TUNED_AT_WORLD_WIDTH_BLOCKS / TerraMap.WORLD_WIDTH_BLOCKS);
        DensityFunction ridges = DensityFunctions.flatCache(
                DensityFunctions.noise(noises.getOrThrow(RIDGES), rangeScale, 0.0));
        DensityFunction erosion = DensityFunctions.flatCache(
                DensityFunctions.noise(noises.getOrThrow(EROSION), rangeScale, 0.0));

        DensityFunction mapHeight = spline(mapElevation,
                -1.00f, -0.781f,  // y  28  the abyss
                -0.68f, -0.516f,  // y  62  DEEP_SEA / SEA
                -0.32f, -0.094f,  // y 116  SEA / SHORE
                -0.26f, -0.031f,  // y 124  sea level, inside the shore band
                -0.20f,  0.023f,  // y 131  SHORE / LOWLAND
                -0.04f,  0.070f,  // y 137  LOWLAND / FLATS
                 0.24f,  0.172f,  // y 150  FLATS / MIDLAND
                 0.48f,  0.313f,  // y 168  MIDLAND / HILLS
                 0.72f,  0.531f,  // y 196  HILLS / MOUNTAIN
                 1.00f,  0.906f); // y 244  the highest peaks

        DensityFunction mapRelief = DensityFunctions.flatCache(TerraReliefFunction.INSTANCE);

        // The one thing relief takes from elevation: a painted crag running into the sea
        // would break the waterline into one-block islands and potholes, so relief is damped
        // to nothing across the shore band and comes fully in once the ground is inland.
        DensityFunction shoreDamping = spline(mapElevation,
                -1.00f, 0.00f,
                -0.32f, 0.00f,   // SEA / SHORE
                -0.20f, 0.00f,   // SHORE / LOWLAND
                -0.04f, 1.00f,   // LOWLAND / FLATS
                 1.00f, 1.00f);

        DensityFunction ruggedness = DensityFunctions.mul(mapRelief, shoreDamping);

        // Folded so peaks land on |ridges| ~ 0.65, giving long chains with basins between
        // them instead of isolated lumps.
        DensityFunction ridgeShape = spline(ridges,
                -1.00f,  0.00f,
                -0.65f,  1.00f,   // ridge line
                 0.00f, -0.20f,   // basin
                 0.65f,  1.00f,   // ridge line
                 1.00f,  0.00f);

        // So two mountains on the same mapped ridge are not the same mountain.
        DensityFunction reliefVariation = spline(erosion,
                -1.00f, 1.25f,
                 0.00f, 1.00f,
                 1.00f, 0.72f);

        DensityFunction terrainHeight = DensityFunctions.cache2d(
                DensityFunctions.add(
                        mapHeight,
                        DensityFunctions.mul(
                                DensityFunctions.mul(ruggedness, reliefVariation), ridgeShape)
                )
        );

        // Zero at the surface, positive underground. Feeds both terrain and the biome
        // sampler's depth channel.
        DensityFunction depth = DensityFunctions.add(
                DensityFunctions.yClampedGradient(-64, 320, 1.5, -1.5),
                terrainHeight
        );

        // On land the detail amplitude follows the painted relief map, so one grey value
        // controls both a place's hills and how broken their faces are. The fraction puts
        // grey 255 on the +/-13 blocks of crag face this was tuned to.
        DensityFunction landDetail = DensityFunctions.mul(
                DensityFunctions.mul(mapRelief, shoreDamping),
                DensityFunctions.constant(0.28));

        // Underwater stays keyed to elevation — nobody paints the sea floor. Zero everywhere
        // landDetail is non-zero, so the two never fight.
        DensityFunction waterDetail = spline(mapElevation,
                -1.00f, 0.055f,   // +/- 7 blocks: broken sea floor
                -0.32f, 0.030f,   // SEA / SHORE
                -0.20f, 0.008f,   // shore: nearly nothing
                -0.04f, 0.000f,   // on land the relief map takes over
                 1.00f, 0.000f);

        DensityFunction detailAmount = DensityFunctions.add(waterDetail, landDetail);

        // yScale below xzScale keeps it vertically coherent, carving cliffs and ledges
        // rather than swiss cheese.
        DensityFunction detail = DensityFunctions.mul(
                detailAmount,
                DensityFunctions.noise(noises.getOrThrow(DETAIL), 0.5, 0.2)
        );

        DensityFunction finalDensity = DensityFunctions.interpolated(
                DensityFunctions.add(depth, detail)
        );

        DensityFunction zero = DensityFunctions.zero();

        // temperature and vegetation stay wired to zero rather than deleted because
        // NoiseRouter requires them and TerraMapBiomeSource ignores the sampler entirely.
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
     * Builds a cubic spline over {@code coordinate} from flat (location, value) pairs. Every
     * knot gets a zero derivative, which keeps the curve monotone between knots and stops it
     * overshooting outside the range you wrote down.
     */
    private static DensityFunction spline(DensityFunction coordinate, float... locationValuePairs) {
        CubicSpline.Builder<DensityFunctions.Spline.Point, DensityFunctions.Spline.Coordinate> builder =
                CubicSpline.builder(new DensityFunctions.Spline.Coordinate(Holder.direct(coordinate)));
        for (int i = 0; i < locationValuePairs.length; i += 2) {
            builder.addPoint(locationValuePairs[i], locationValuePairs[i + 1], 0.0f);
        }
        return DensityFunctions.spline(builder.build());
    }

    private static SurfaceRules.RuleSource createSurfaceRules() {
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

        var iberianSand      = SurfaceRules.state(ModBlocks.IBERIAN_SAND.get().defaultBlockState());
        var iberianSandstone = SurfaceRules.state(ModBlocks.IBERIAN_SANDSTONE.get().defaultBlockState());
        var permafrost       = SurfaceRules.state(ModBlocks.PERMAFROST.get().defaultBlockState());
        var blackIce         = SurfaceRules.state(ModBlocks.BLACK_ICE.get().defaultBlockState());

        // Held for zones that are not painted yet — the arid south, Victoria's moors, and the
        // coasts that get their own colours later. Delete only if you drop the zone too.
        var siestaSand       = SurfaceRules.state(ModBlocks.SIESTA_SAND.get().defaultBlockState());
        var paleBeachSand    = SurfaceRules.state(ModBlocks.PALE_BEACH_SAND.get().defaultBlockState());
        var deadSeabed       = SurfaceRules.state(ModBlocks.DEAD_SEABED.get().defaultBlockState());

        var patchHigh = SurfaceRules.noiseCondition(SURFACE_PATCH,  0.4,  1.0);
        var patchMid  = SurfaceRules.noiseCondition(SURFACE_PATCH,  0.1,  0.4);
        var patchLow  = SurfaceRules.noiseCondition(SURFACE_PATCH, -0.2,  0.1);

        // stoneDepthCheck(N) matches EVERY depth from 0..N, so order these shallowest-first
        // inside a sequence.
        var floor0 = SurfaceRules.stoneDepthCheck(0, false, 0, CaveSurface.FLOOR);
        var floor1 = SurfaceRules.stoneDepthCheck(1, false, 0, CaveSurface.FLOOR);
        var floor2 = SurfaceRules.stoneDepthCheck(2, false, 0, CaveSurface.FLOOR);
        var floor3 = SurfaceRules.stoneDepthCheck(3, false, 0, CaveSurface.FLOOR);
        var floor4 = SurfaceRules.stoneDepthCheck(4, false, 0, CaveSurface.FLOOR);
        var floor8 = SurfaceRules.stoneDepthCheck(8, false, 0, CaveSurface.FLOOR);

        var aboveSnowline = SurfaceRules.yBlockCheck(VerticalAnchor.absolute(185), 0);
        var aboveTreeline = SurfaceRules.yBlockCheck(VerticalAnchor.absolute(165), 0);

        // One rule per zone, so this list is exactly TerraRegion's list. A zone with no rule
        // generates as bare stone — the failure mode to look for if a new zone comes out grey.
        // Because a zone spans every elevation it is painted over, the snowline/treeline
        // checks do the work the old eight biome slots used to: elevation varies the surface
        // within a zone, it no longer changes which zone you are in.
        return SurfaceRules.sequence(

                // Must stay first.
                SurfaceRules.ifTrue(
                        SurfaceRules.verticalGradient("minecraft:bedrock_floor",
                                VerticalAnchor.aboveBottom(0), VerticalAnchor.aboveBottom(5)),
                        bedrock
                ),

                // Mostly deep sea floor, but it also covers the islands the elevation map
                // lifts above the waterline, so it cannot be gravel alone.
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

                // Ursus is one biome per climate, and the surface is what carries the split:
                // frozen duff north, dry cropped ground in the steppe, forest floor south.
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

                // Kjerag is alpine rather than polar — snow over stone, not snow over ice.
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

                // Karlan is the mountain's body: more exposed rock, less standing snow, black
                // ice where the glaciers have scoured it.
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

                // Iberia keeps its own sand: a third of the zone is coast and vanilla desert
                // sand is the wrong colour for it.
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

                // Loud on purpose: unzoned ground should look wrong from a distance, not pass
                // for a design decision you forgot you made.
                SurfaceRules.ifTrue(SurfaceRules.isBiome(ModBiomes.TEMPORARY_LAYER),
                        SurfaceRules.sequence(
                                SurfaceRules.ifTrue(floor0, yellowTerra),
                                SurfaceRules.ifTrue(floor4, coarseDirt)
                        )
                ),

                // Must stay last.
                SurfaceRules.ifTrue(
                        SurfaceRules.verticalGradient("minecraft:deepslate",
                                VerticalAnchor.absolute(0), VerticalAnchor.absolute(8)),
                        deepslate
                )
        );
    }
}
