package com.jyhrie.priestess.world.terra;

import com.mojang.serialization.MapCodec;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;

/**
 * Exposes the Terra map's elevation channel to the density-function graph, so terrain
 * height is driven by the same data as biome choice.
 *
 * <p>Biomes from a map plus terrain from noise would put Iberian beaches halfway up a
 * mountain, so both have to read the same elevation: this function and
 * {@link TerraMapBiomeSource} both call {@link TerraMap#elevationAt(double, double)}.
 *
 * <p>Output is the map's [0,1] elevation rescaled to [-1,1], the range every spline in the
 * density-function graph expects. The band edges in {@code ModNoiseSettings} are
 * {@link TerraSlot}'s, doubled and shifted to match.
 *
 * <p>Purely two-dimensional, so wrap it in {@code flatCache} at the point of use — the
 * router does — and it is sampled once per column instead of once per cell.
 */
public record TerraElevationFunction() implements DensityFunction.SimpleFunction {

    public static final TerraElevationFunction INSTANCE = new TerraElevationFunction();

    public static final KeyDispatchDataCodec<TerraElevationFunction> CODEC =
            KeyDispatchDataCodec.of(MapCodec.unit(INSTANCE));

    @Override
    public double compute(FunctionContext context) {
        return TerraMap.get().elevationAt(context.blockX(), context.blockZ()) * 2.0 - 1.0;
    }

    @Override
    public double minValue() {
        return -1.0;
    }

    @Override
    public double maxValue() {
        return 1.0;
    }

    @Override
    public KeyDispatchDataCodec<? extends DensityFunction> codec() {
        return CODEC;
    }
}
