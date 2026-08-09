package com.jyhrie.priestess.world.terra;

import com.jyhrie.priestess.world.dimension.ModBiomes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;

import java.util.HashMap;
import java.util.Map;

/**
 * Every zone of Terra: one colour in {@code data/priestess/terra/regions.png}, one biome.
 *
 * <h2>The colour is the contract</h2>
 * The {@code colour} field must exactly match the colour that zone is painted in. Use an
 * editor with a pencil tool rather than a brush, because anti-aliased edges produce colours
 * that are in no zone. ({@link TerraMap} tolerates that by snapping unknown colours to the
 * nearest known one and logging a warning, but a hard-edged map is what you want — and a
 * clean load reports zero unrecognised colours, which is the check worth watching.)
 *
 * <h2>One colour, one biome, no exceptions</h2>
 * This table used to give each zone eight biomes indexed by elevation, so a zone's coast,
 * flats and peaks were different biomes. That is gone. The map defines where the biomes
 * are, full stop: what you paint is what generates, at every height.
 *
 * <p>The consequence to keep in mind is that a zone no longer produces a coastline on its
 * own. Paint a nation across a bay and the water in that bay is the nation's biome, not
 * ocean. Coasts, beaches and named seas become their own colours when you paint them.
 *
 * <p>Elevation has not stopped mattering — it still drives terrain height through the
 * {@code mapHeight} spline, and the surface rules in {@code ModNoiseSettings} still key off
 * height within a zone, so a zone can go bare rock above its treeline without that being a
 * separate biome. What elevation no longer does is choose the biome.
 *
 * <h2>Adding a zone</h2>
 * Register the biome in {@link ModBiomes}, add the row here, give it a surface rule in
 * {@code ModNoiseSettings}, then re-run {@code gradlew runData}. A zone painted on the map
 * with no row here is a warning at load and a nearest-colour guess in the world; a row here
 * with no paint on the map is dead weight in the serialised table.
 */
public enum TerraRegion {

    /**
     * Everything outside the continent, painted flat black — 72% of the map, and 95% of
     * that is below the waterline.
     *
     * <p>It needs a row of its own even though it is "just" the sea. Black is not a colour
     * any nation is painted in, so without one {@link #byNearestColour} hands the entire
     * world ocean to whichever zone happens to be darkest.
     */
    OCEAN(0x000000, ModBiomes.OCEAN),

    // ── The north ─────────────────────────────────────────────────────────────
    /** The Infy Icefield: the frontier north of Sami and Ursus, and terra incognita past it. */
    INFY(0xFFFFFF, ModBiomes.INFY_ICEFIELD),
    SAMI(0x98CAFF, ModBiomes.SAMI),

    // ── Ursus, split three ways ───────────────────────────────────────────────
    /** Northern Ursus, against the ice. */
    URSUS_COLD(0x6B0A0A, ModBiomes.URSUS_COLD),
    /** The Ursine steppe: cold, open, and too dry for forest. */
    URSUS_DRY(0xFFC532, ModBiomes.URSUS_DRY),
    /** Southern Ursus, where birch gets in among the pine. */
    URSUS_WARM(0xFF3232, ModBiomes.URSUS_WARM),

    // ── The range ─────────────────────────────────────────────────────────────
    // One massif painted as two zones. The elevation map says which is which: Kjerag sits
    // almost entirely above 0.86 while Mount Karlan wraps around it lower down, so Karlan
    // is the body of the mountain and Kjerag is the summit country on top of it.

    /** The high basin at the top of the range from northern Kazimierz to Sargon. */
    KJERAG(0x165A74, ModBiomes.KJERAG),
    /** Mount Karlan itself: the flanks Kjerag sits on, and the long climb up to it. */
    MOUNT_KARLAN(0x558496, ModBiomes.MOUNT_KARLAN),

    // ── The heartland and the west ────────────────────────────────────────────
    KAZIMIERZ(0x4EFF61, ModBiomes.KAZIMIERZ),
    COLUMBIA(0x837CFF, ModBiomes.COLUMBIA),
    /** Cold, and dead along the coast — Ægir saw to that. Inland it is sour heath. */
    IBERIA_LAND(0x2E1AFF, ModBiomes.IBERIA_LAND),

    // ── The east ──────────────────────────────────────────────────────────────
    YAN(0xFF9D00, ModBiomes.YAN),
    /** Northern Higashi: cedar under snow. */
    HIGASHI_COLD(0x9E5252, ModBiomes.HIGASHI_COLD),
    /** Southern Higashi: terraced paddy and wet heat. */
    HIGASHI_WARM(0x760006, ModBiomes.HIGASHI_WARM),

    // ── The south ─────────────────────────────────────────────────────────────
    /** The Sarkaz homeland: fought over until nothing green was left. */
    KAZDEL(0x2D0000, ModBiomes.KAZDEL),

    // ── Unpainted ─────────────────────────────────────────────────────────────
    /**
     * Ground no nation has been painted onto yet. It wears a deliberately garish biome so
     * that unfinished map reads as unfinished from inside the world, and it is also where
     * {@link #byNearestColour} sends a colour it does not recognise — the right answer for
     * both cases that can arise from, a zone you have not got to yet and a soft-brush
     * artefact on a border.
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

    // ── Colour lookup ─────────────────────────────────────────────────────────
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
