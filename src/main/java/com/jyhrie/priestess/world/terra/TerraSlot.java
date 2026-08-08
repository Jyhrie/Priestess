package com.jyhrie.priestess.world.terra;

/**
 * The eight terrain classes a point on the map can fall into, chosen purely by the
 * elevation channel of {@code elevation.png}.
 *
 * <p>These thresholds are the contract between three things that must agree or the world
 * comes out wrong:
 * <ul>
 *   <li>{@code tools/generate_terra_map.py}, which paints elevations into those bands,</li>
 *   <li>this enum, which turns an elevation back into a terrain class for biome choice,</li>
 *   <li>the {@code mapHeight} spline in {@code ModNoiseSettings}, whose knots sit on these
 *       same edges so that the ground actually generates at the height the class implies.</li>
 * </ul>
 * Move an edge here and you must move it in the other two, or you get beaches halfway up
 * a mountain.
 *
 * <p>The waterline is at elevation 0.37, which is inside {@link #SHORE}. That is
 * deliberate: a shore is half surf and half dry sand.
 *
 * <p>Note the two height columns. The "base y" is what {@code mapHeight} alone puts the
 * surface at — that is what these thresholds are aligned to. The ground you actually
 * stand on then has ridge relief and detail noise added, which in the mountains is worth
 * another 60 blocks: the peaks reach y 305, not y 244. Keep that in mind if you raise the
 * top of the {@code mapHeight} spline, because the world ceiling is y 320.
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
        // Walk down from the top: the first floor we clear is the answer. Eight entries,
        // so a loop beats a branch tree for readability and costs nothing measurable.
        for (int i = COUNT - 1; i > 0; i--) {
            if (elevation >= VALUES[i].floor) {
                return VALUES[i];
            }
        }
        return DEEP_SEA;
    }
}
