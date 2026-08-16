package com.jyhrie.priestess.world.terra;

/**
 * The eight terrain classes a point on the map can fall into, chosen purely by the
 * elevation channel of {@code elevation.png}.
 *
 * <p>These thresholds are a contract between {@code tools/generate_terra_map.py}, this enum,
 * and the {@code mapHeight} spline in {@code ModNoiseSettings}. Move an edge here and you
 * must move it in the other two, or you get beaches halfway up a mountain.
 *
 * <p>The waterline is at elevation 0.37, inside {@link #SHORE}, because a shore is half surf
 * and half dry sand.
 *
 * <p>Base y is what {@code mapHeight} alone gives, and is what these thresholds are aligned
 * to; ridge relief and detail noise then add up to another 60 blocks in the mountains. Watch
 * that if you raise the top of the spline — the world ceiling is y 320.
 */
public enum TerraSlot {

    //                    base y      actual surface y
    //                    (mapHeight) (after ridge relief and detail noise)
    DEEP_SEA(0.00f),   //   28 -  62     21 -  68
    SEA(0.16f),        //   62 - 116     56 - 120
    SHORE(0.34f),      //  116 - 131    112 - 134   waterline at y 124
    LOWLAND(0.40f),    //  131 - 137    129 - 144
    FLATS(0.48f),      //  137 - 150    133 - 162
    MIDLAND(0.62f),    //  150 - 168    144 - 192
    HILLS(0.74f),      //  168 - 196    157 - 238
    MOUNTAIN(0.86f);   //  196 - 244    179 - 305

    /** Elevation at or above which this slot begins. */
    public final float floor;

    public static final TerraSlot[] VALUES = values();
    public static final int COUNT = VALUES.length;

    TerraSlot(float floor) {
        this.floor = floor;
    }

    /** The slot a normalised elevation in [0,1] falls into. */
    public static TerraSlot of(double elevation) {
        for (int i = COUNT - 1; i > 0; i--) {
            if (elevation >= VALUES[i].floor) {
                return VALUES[i];
            }
        }
        return DEEP_SEA;
    }
}
