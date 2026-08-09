package com.jyhrie.priestess.world.terra;

import com.mojang.serialization.MapCodec;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;

/**
 * Exposes the Terra map's relief channel to the density-function graph, so how rugged a
 * place is can be painted rather than derived.
 *
 * <p>This is the counterpart to {@link TerraElevationFunction}, and the two are
 * deliberately independent. Relief used to be a spline over elevation, which tied the two
 * together in a way you could not paint your way out of: anywhere you made higher, you
 * also made bumpier, so a high plateau or a broken lowland simply could not exist. Now
 * {@code elevation.png} says how high the ground is and {@code relief.png} says how much
 * it rises and falls once it is there.
 *
 * <p>Output is the map's [0,1] grey scaled to a density amplitude, where
 * {@code surfaceY = 128 + 128 * terrainHeight} — so the value is
 * {@code grey/255 * MAX_RELIEF_BLOCKS / 128}, and a painted grey reads directly as a
 * number of blocks:
 *
 * <pre>
 *     grey   0  ->   0 blocks   dead flat: salt pan, paddy, ice shelf
 *     grey  43  ->   8 blocks   open and walkable — steppe
 *     grey  80  ->  15 blocks   ordinary rolling country
 *     grey 128  ->  24 blocks   hill country, visible spurs and gullies
 *     grey 180  ->  34 blocks   mountain
 *     grey 255  ->  48 blocks   broken crag, ledges and climbing
 * </pre>
 *
 * <p>That is the height of a spur, not its spacing — spurs land about 128 blocks apart
 * whatever this says, which is {@code rangeScale} in {@code ModNoiseSettings}. Two things
 * scale the number at runtime, so treat it as a centre rather than a cap:
 * {@code reliefVariation} multiplies it by 0.72–1.25 along a range, and it is damped to
 * zero at the waterline so coastlines stay clean however rugged the land behind them is.
 *
 * <p>Purely two-dimensional, so wrap it in {@code flatCache} at the point of use — the
 * router does.
 */
public record TerraReliefFunction() implements DensityFunction.SimpleFunction {

    public static final TerraReliefFunction INSTANCE = new TerraReliefFunction();

    public static final KeyDispatchDataCodec<TerraReliefFunction> CODEC =
            KeyDispatchDataCodec.of(MapCodec.unit(INSTANCE));

    /**
     * What a fully white relief pixel is worth, in blocks. The single calibration knob for
     * the whole map: raise it and every painted value gets proportionally more dramatic
     * without you repainting anything.
     */
    public static final double MAX_RELIEF_BLOCKS = 48.0;

    /** Blocks to density, given {@code surfaceY = 128 + 128 * terrainHeight}. */
    private static final double BLOCKS_TO_DENSITY = 1.0 / 128.0;

    @Override
    public double compute(FunctionContext context) {
        return TerraMap.get().reliefAt(context.blockX(), context.blockZ())
                * MAX_RELIEF_BLOCKS * BLOCKS_TO_DENSITY;
    }

    @Override
    public double minValue() {
        return 0.0;
    }

    @Override
    public double maxValue() {
        return MAX_RELIEF_BLOCKS * BLOCKS_TO_DENSITY;
    }

    @Override
    public KeyDispatchDataCodec<? extends DensityFunction> codec() {
        return CODEC;
    }
}
