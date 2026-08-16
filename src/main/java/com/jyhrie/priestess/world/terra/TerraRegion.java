package com.jyhrie.priestess.world.terra;

import com.jyhrie.priestess.world.dimension.ModBiomes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;

import java.util.HashMap;
import java.util.Map;

/**
 * Every zone of Terra: one colour in {@code data/priestess/terra/regions.png}, one biome.
 *
 * <p>The {@code colour} field must exactly match the colour that zone is painted in. Paint
 * with a pencil tool rather than a brush — anti-aliased edges produce colours that are in no
 * zone. A clean load reports zero unrecognised colours.
 *
 * <p>One colour, one biome, at every height: what you paint is what generates. So a zone
 * does not produce a coastline on its own — paint a nation across a bay and the water in
 * that bay is the nation's biome. Elevation still drives terrain height and the surface
 * rules key off height within a zone; what it no longer does is choose the biome.
 *
 * <p>To add a zone: register the biome in {@link ModBiomes}, add the row here, give it a
 * surface rule in {@code ModNoiseSettings}, then re-run {@code gradlew runData}.
 */
public enum TerraRegion {

    /**
     * Everything outside the continent, painted flat black. It needs a row of its own even
     * though it is "just" the sea: black is not a colour any nation is painted in, so
     * without one {@link #byNearestColour} hands the world ocean to whichever zone happens
     * to be darkest.
     */
    OCEAN(0x000000, ModBiomes.OCEAN),

    /** The Infy Icefield: the frontier north of Sami and Ursus, and terra incognita past it. */
    INFY(0xFFFFFF, ModBiomes.INFY_ICEFIELD),
    SAMI(0x98CAFF, ModBiomes.SAMI),

    /** Northern Ursus, against the ice. */
    URSUS_COLD(0x6B0A0A, ModBiomes.URSUS_COLD),
    /** The Ursine steppe: cold, open, and too dry for forest. */
    URSUS_DRY(0xFFC532, ModBiomes.URSUS_DRY),
    /** Southern Ursus, where birch gets in among the pine. */
    URSUS_WARM(0xFF3232, ModBiomes.URSUS_WARM),

    // One massif painted as two zones, told apart by the elevation map: Kjerag sits almost
    // entirely above 0.86 and Karlan wraps around it lower down.

    /** The high basin at the top of the range from northern Kazimierz to Sargon. */
    KJERAG(0x165A74, ModBiomes.KJERAG),
    /** Mount Karlan itself: the flanks Kjerag sits on, and the long climb up to it. */
    MOUNT_KARLAN(0x558496, ModBiomes.MOUNT_KARLAN),

    KAZIMIERZ(0x4EFF61, ModBiomes.KAZIMIERZ),
    COLUMBIA(0x837CFF, ModBiomes.COLUMBIA),
    /** Cold, and dead along the coast — Ægir saw to that. Inland it is sour heath. */
    IBERIA_LAND(0x2E1AFF, ModBiomes.IBERIA_LAND),

    YAN(0xFF9D00, ModBiomes.YAN),
    /** Northern Higashi: cedar under snow. */
    HIGASHI_COLD(0x9E5252, ModBiomes.HIGASHI_COLD),
    /** Southern Higashi: terraced paddy and wet heat. */
    HIGASHI_WARM(0x760006, ModBiomes.HIGASHI_WARM),

    /** The Sarkaz homeland: fought over until nothing green was left. */
    KAZDEL(0x2D0000, ModBiomes.KAZDEL),

    /**
     * Ground no nation has been painted onto yet, wearing a garish biome so that unfinished
     * map reads as unfinished from inside the world. Also where {@link #byNearestColour}
     * sends a colour it does not recognise.
     */
    TEMPORARY(0xD1FF00, ModBiomes.TEMPORARY_LAYER);

    public static final TerraRegion[] VALUES = values();

    /** RGB as painted in regions.png. */
    public final int colour;
    private final ResourceKey<Biome> biome;

    TerraRegion(int colour, ResourceKey<Biome> biome) {
        this.colour = colour;
        this.biome = biome;
    }

    /** The biome this zone generates, everywhere within it. */
    public ResourceKey<Biome> biome() {
        return biome;
    }

    private static final Map<Integer, TerraRegion> BY_COLOUR = new HashMap<>();

    static {
        for (TerraRegion region : VALUES) {
            TerraRegion clash = BY_COLOUR.put(region.colour, region);
            if (clash != null) {
                throw new IllegalStateException(String.format(
                        "%s and %s share map colour #%06X", clash, region, region.colour));
            }
        }
    }

    /** Exact colour match, or null. {@link TerraMap} handles the inexact case. */
    public static TerraRegion byExactColour(int rgb) {
        return BY_COLOUR.get(rgb);
    }

    /**
     * The zone whose colour is closest in RGB. Used only for pixels that are not an exact
     * match, which in practice means someone hand-edited the map with a soft brush.
     */
    public static TerraRegion byNearestColour(int rgb) {
        int r = (rgb >> 16) & 0xFF, g = (rgb >> 8) & 0xFF, b = rgb & 0xFF;
        TerraRegion best = TEMPORARY;
        int bestDistance = Integer.MAX_VALUE;
        for (TerraRegion region : VALUES) {
            int dr = ((region.colour >> 16) & 0xFF) - r;
            int dg = ((region.colour >> 8) & 0xFF) - g;
            int db = (region.colour & 0xFF) - b;
            int distance = dr * dr + dg * dg + db * db;
            if (distance < bestDistance) {
                bestDistance = distance;
                best = region;
            }
        }
        return best;
    }
}
