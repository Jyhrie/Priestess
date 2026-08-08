package com.jyhrie.priestess.world.terra;

import com.jyhrie.priestess.world.dimension.ModBiomes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;

import java.util.HashMap;
import java.util.Map;

/**
 * Every region of Terra, and which biome each one wears at each terrain class.
 *
 * <h2>The colour is a contract</h2>
 * The {@code colour} field must exactly match the colour that region is painted in
 * {@code data/priestess/terra/regions.png}. {@code tools/generate_terra_map.py} prints
 * the full list when it runs; if you repaint the map by hand, use an editor with a
 * pencil tool rather than a brush, because anti-aliased edges produce colours that are
 * in no region. ({@link TerraMap} tolerates that by snapping unknown colours to the
 * nearest known one, but a hard-edged map is what you want.)
 *
 * <h2>Reading the table</h2>
 * Each row is one region and its eight slots, in {@link TerraSlot} order:
 * <pre>
 *     deep sea, sea, shore, lowland, flats, midland, hills, mountain
 * </pre>
 * Ocean regions still declare land slots and land regions still declare sea slots. They
 * are usually unreachable — an ocean region has no pixels above the waterline — but they
 * have to be filled, and filling them sensibly means the map stays correct if you later
 * repaint a coastline and hand some land to a region that never had any.
 *
 * <p>Where a region borders another, their tables should agree at the shared slot, or
 * you get a hard seam. The old climate-wedge model enforced that structurally; here it is
 * on you, because the whole point of a hand-authored map is that you place things.
 */
public enum TerraRegion {

    // ── Open water ────────────────────────────────────────────────────────────
    /** The ocean south of Iberia, where Ægir lies. The two are at war. */
    AEGIR(0x0E1A2E,
            ModBiomes.AEGIR_DEPTHS, ModBiomes.SEA_OF_SILENCE, ModBiomes.IBERIAN_SHORES,
            ModBiomes.BARRENLANDS, ModBiomes.BARRENLANDS, ModBiomes.VICTORIAN_MOORS,
            ModBiomes.YANESE_PEAKS, ModBiomes.YANESE_PEAKS),

    /** Everything else: west of Bolívar, east of Yan and Higashi. */
    OPEN_OCEAN(0x1D4E7A,
            ModBiomes.BOLIVAR_DEPTHS, ModBiomes.BOLIVAR_DEPTHS, ModBiomes.DOSSOLES_BEACHES,
            ModBiomes.KAZIMIERZ_PLAINS, ModBiomes.KAZIMIERZ_PLAINS, ModBiomes.VICTORIAN_MOORS,
            ModBiomes.LEITHANIEN_WOODS, ModBiomes.YANESE_PEAKS),

    /** The great inland sea. Canon gives it Mediterranean biomes, so it runs warm. */
    CLARISIDE(0x39B9D6,
            ModBiomes.SIESTA_SEA, ModBiomes.SIESTA_SEA, ModBiomes.DOSSOLES_BEACHES,
            ModBiomes.SIRACUSAN_COAST, ModBiomes.KAZIMIERZ_PLAINS, ModBiomes.VICTORIAN_MOORS,
            ModBiomes.HIGASHI_HIGHLANDS, ModBiomes.HIGASHI_HIGHLANDS),

    /** Northern Bolívar's sea, which feeds Dossoles' artificial one. */
    NORTE_SEA(0x49C4C0,
            ModBiomes.SIESTA_SEA, ModBiomes.SIESTA_SEA, ModBiomes.DOSSOLES_BEACHES,
            ModBiomes.BOLIVAR_MIRE, ModBiomes.BOLIVAR_MIRE, ModBiomes.VICTORIAN_MOORS,
            ModBiomes.HIGASHI_HIGHLANDS, ModBiomes.HIGASHI_HIGHLANDS),

    // ── The north ─────────────────────────────────────────────────────────────
    /** The Infy Icefield: the frontier north of Sami and Ursus, and terra incognita past it. */
    INFY(0xEAF6FF,
            ModBiomes.AEGIR_DEPTHS, ModBiomes.SEA_OF_SILENCE, ModBiomes.AEGIR_SHELF,
            ModBiomes.INFY_ICEFIELDS, ModBiomes.INFY_ICEFIELDS, ModBiomes.INFY_ICEFIELDS,
            ModBiomes.KJERAG_SLOPES, ModBiomes.KJERAG_SLOPES),

    SAMI(0xBFE3F2,
            ModBiomes.AEGIR_DEPTHS, ModBiomes.SEA_OF_SILENCE, ModBiomes.AEGIR_SHELF,
            ModBiomes.URSUS_TAIGA, ModBiomes.SAMI_SNOWFIELDS, ModBiomes.SAMI_SNOWFIELDS,
            ModBiomes.SAMI_SNOWFIELDS, ModBiomes.KJERAG_SLOPES),

    URSUS(0x7A93B5,
            ModBiomes.AEGIR_DEPTHS, ModBiomes.SEA_OF_SILENCE, ModBiomes.AEGIR_SHELF,
            ModBiomes.URSUS_TAIGA, ModBiomes.URSUS_TAIGA, ModBiomes.SAMI_SNOWFIELDS,
            ModBiomes.LEITHANIEN_WOODS, ModBiomes.KJERAG_SLOPES),

    /** The mountain basin on the range running from northern Kazimierz to Sargon. */
    KJERAG(0xE4EEF5,
            ModBiomes.AEGIR_DEPTHS, ModBiomes.SEA_OF_SILENCE, ModBiomes.AEGIR_SHELF,
            ModBiomes.KJERAG_SLOPES, ModBiomes.KJERAG_SLOPES, ModBiomes.KJERAG_SLOPES,
            ModBiomes.KJERAG_SLOPES, ModBiomes.KJERAG_SLOPES),

    // ── The heartland ─────────────────────────────────────────────────────────
    KAZIMIERZ(0x9CC46A,
            ModBiomes.BOLIVAR_DEPTHS, ModBiomes.SIESTA_SEA, ModBiomes.DOSSOLES_BEACHES,
            ModBiomes.KAZIMIERZ_PLAINS, ModBiomes.KAZIMIERZ_PLAINS, ModBiomes.KAZIMIERZ_PLAINS,
            ModBiomes.VICTORIAN_MOORS, ModBiomes.KJERAG_SLOPES),

    /** Central Terra, and canon's most fertile ground. */
    VICTORIA(0x6FA86B,
            ModBiomes.BOLIVAR_DEPTHS, ModBiomes.SIESTA_SEA, ModBiomes.SIRACUSAN_COAST,
            ModBiomes.KAZIMIERZ_PLAINS, ModBiomes.KAZIMIERZ_PLAINS, ModBiomes.VICTORIAN_MOORS,
            ModBiomes.LEITHANIEN_WOODS, ModBiomes.YANESE_PEAKS),

    /** Gaul disintegrated after the War of the Four Nations. What is left is waste. */
    GAUL(0x9C9478,
            ModBiomes.BOLIVAR_DEPTHS, ModBiomes.SEA_OF_SILENCE, ModBiomes.IBERIAN_SHORES,
            ModBiomes.BARRENLANDS, ModBiomes.BARRENLANDS, ModBiomes.BARRENLANDS,
            ModBiomes.VICTORIAN_MOORS, ModBiomes.YANESE_PEAKS),

    LEITHANIEN(0x4E7C55,
            ModBiomes.BOLIVAR_DEPTHS, ModBiomes.SIESTA_SEA, ModBiomes.SIRACUSAN_COAST,
            ModBiomes.LEITHANIEN_WOODS, ModBiomes.LEITHANIEN_WOODS, ModBiomes.LEITHANIEN_WOODS,
            ModBiomes.LEITHANIEN_WOODS, ModBiomes.YANESE_PEAKS),

    // ── The Clariside rim ─────────────────────────────────────────────────────
    /** Cold, and dead along the coast — Ægir saw to that. Inland it is barrenlands. */
    IBERIA(0x8FA0B0,
            ModBiomes.AEGIR_DEPTHS, ModBiomes.SEA_OF_SILENCE, ModBiomes.IBERIAN_SHORES,
            ModBiomes.IBERIAN_SHORES, ModBiomes.BARRENLANDS, ModBiomes.BARRENLANDS,
            ModBiomes.VICTORIAN_MOORS, ModBiomes.YANESE_PEAKS),

    /** A volcanic peninsula in the Clariside, so its high ground is crag, not meadow. */
    SIESTA(0xF0C86A,
            ModBiomes.SIESTA_SEA, ModBiomes.SIESTA_SEA, ModBiomes.DOSSOLES_BEACHES,
            ModBiomes.SIRACUSAN_COAST, ModBiomes.SIRACUSAN_COAST, ModBiomes.KAZDEL_CRAGS,
            ModBiomes.KAZDEL_CRAGS, ModBiomes.KAZDEL_CRAGS),

    ACAHUALLA(0x7FA05A,
            ModBiomes.SIESTA_SEA, ModBiomes.SIESTA_SEA, ModBiomes.DOSSOLES_BEACHES,
            ModBiomes.BOLIVAR_MIRE, ModBiomes.BOLIVAR_MIRE, ModBiomes.LEITHANIEN_WOODS,
            ModBiomes.HIGASHI_HIGHLANDS, ModBiomes.HIGASHI_HIGHLANDS),

    LATERANO(0xF2E9C4,
            ModBiomes.SIESTA_SEA, ModBiomes.SIESTA_SEA, ModBiomes.SIRACUSAN_COAST,
            ModBiomes.KAZIMIERZ_PLAINS, ModBiomes.KAZIMIERZ_PLAINS, ModBiomes.VICTORIAN_MOORS,
            ModBiomes.LEITHANIEN_WOODS, ModBiomes.YANESE_PEAKS),

    SIRACUSA(0xD9A25C,
            ModBiomes.SIESTA_SEA, ModBiomes.SIESTA_SEA, ModBiomes.SIRACUSAN_COAST,
            ModBiomes.SIRACUSAN_COAST, ModBiomes.KAZIMIERZ_PLAINS, ModBiomes.VICTORIAN_MOORS,
            ModBiomes.HIGASHI_HIGHLANDS, ModBiomes.HIGASHI_HIGHLANDS),

    MINOS(0xC9B98A,
            ModBiomes.SIESTA_SEA, ModBiomes.SIESTA_SEA, ModBiomes.SIRACUSAN_COAST,
            ModBiomes.KAZIMIERZ_PLAINS, ModBiomes.BARRENLANDS, ModBiomes.VICTORIAN_MOORS,
            ModBiomes.HIGASHI_HIGHLANDS, ModBiomes.YANESE_PEAKS),

    // ── The south ─────────────────────────────────────────────────────────────
    /** The Sarkaz homeland: fought over until nothing green was left. */
    KAZDEL(0x7A3A3A,
            ModBiomes.BOLIVAR_DEPTHS, ModBiomes.SEA_OF_SILENCE, ModBiomes.IBERIAN_SHORES,
            ModBiomes.BARRENLANDS, ModBiomes.BARRENLANDS, ModBiomes.KAZDEL_CRAGS,
            ModBiomes.KAZDEL_CRAGS, ModBiomes.KAZDEL_CRAGS),

    SARGON(0xE0B060,
            ModBiomes.BOLIVAR_DEPTHS, ModBiomes.SIESTA_SEA, ModBiomes.DOSSOLES_BEACHES,
            ModBiomes.SARGON_DUNES, ModBiomes.SARGON_DUNES, ModBiomes.FOEHN_HOTLANDS,
            ModBiomes.KAZDEL_CRAGS, ModBiomes.KAZDEL_CRAGS),

    /** The Foehn Hotlands: Sargon's southern frontier, and terra incognita past it. */
    FOEHN(0xD9743A,
            ModBiomes.BOLIVAR_DEPTHS, ModBiomes.SIESTA_SEA, ModBiomes.DOSSOLES_BEACHES,
            ModBiomes.SARGON_DUNES, ModBiomes.FOEHN_HOTLANDS, ModBiomes.FOEHN_HOTLANDS,
            ModBiomes.KAZDEL_CRAGS, ModBiomes.KAZDEL_CRAGS),

    // ── The east ──────────────────────────────────────────────────────────────
    YAN(0x8FB8A0,
            ModBiomes.BOLIVAR_DEPTHS, ModBiomes.SIESTA_SEA, ModBiomes.SIRACUSAN_COAST,
            ModBiomes.KAZIMIERZ_PLAINS, ModBiomes.KAZIMIERZ_PLAINS, ModBiomes.HIGASHI_HIGHLANDS,
            ModBiomes.HIGASHI_HIGHLANDS, ModBiomes.YANESE_PEAKS),

    HIGASHI(0xA8CBB4,
            ModBiomes.BOLIVAR_DEPTHS, ModBiomes.SIESTA_SEA, ModBiomes.DOSSOLES_BEACHES,
            ModBiomes.HIGASHI_HIGHLANDS, ModBiomes.HIGASHI_HIGHLANDS, ModBiomes.HIGASHI_HIGHLANDS,
            ModBiomes.HIGASHI_HIGHLANDS, ModBiomes.YANESE_PEAKS),

    /** Mining country. Its hills are quarried rock rather than pasture. */
    RIM_BILLITON(0x8A7F6E,
            ModBiomes.BOLIVAR_DEPTHS, ModBiomes.SIESTA_SEA, ModBiomes.DOSSOLES_BEACHES,
            ModBiomes.BARRENLANDS, ModBiomes.BARRENLANDS, ModBiomes.KAZDEL_CRAGS,
            ModBiomes.KAZDEL_CRAGS, ModBiomes.KAZDEL_CRAGS),

    // ── The west ──────────────────────────────────────────────────────────────
    COLUMBIA(0xB4C8D8,
            ModBiomes.BOLIVAR_DEPTHS, ModBiomes.BOLIVAR_DEPTHS, ModBiomes.DOSSOLES_BEACHES,
            ModBiomes.KAZIMIERZ_PLAINS, ModBiomes.KAZIMIERZ_PLAINS, ModBiomes.LEITHANIEN_WOODS,
            ModBiomes.LEITHANIEN_WOODS, ModBiomes.YANESE_PEAKS),

    BOLIVAR(0x6E8A5E,
            ModBiomes.BOLIVAR_DEPTHS, ModBiomes.BOLIVAR_DEPTHS, ModBiomes.DOSSOLES_BEACHES,
            ModBiomes.BOLIVAR_MIRE, ModBiomes.BOLIVAR_MIRE, ModBiomes.BARRENLANDS,
            ModBiomes.HIGASHI_HIGHLANDS, ModBiomes.YANESE_PEAKS),

    /** Bolívar's resort capital, and its enormous artificial sea. */
    DOSSOLES(0x6FD8E0,
            ModBiomes.SIESTA_SEA, ModBiomes.SIESTA_SEA, ModBiomes.DOSSOLES_BEACHES,
            ModBiomes.DOSSOLES_BEACHES, ModBiomes.BOLIVAR_MIRE, ModBiomes.BOLIVAR_MIRE,
            ModBiomes.HIGASHI_HIGHLANDS, ModBiomes.HIGASHI_HIGHLANDS),

    // ── Unclaimed ─────────────────────────────────────────────────────────────
    /** Canon's own word for the territory no nation holds. The wastes between cities. */
    BARRENLANDS(0xA89A80,
            ModBiomes.BOLIVAR_DEPTHS, ModBiomes.SEA_OF_SILENCE, ModBiomes.IBERIAN_SHORES,
            ModBiomes.BARRENLANDS, ModBiomes.BARRENLANDS, ModBiomes.BARRENLANDS,
            ModBiomes.VICTORIAN_MOORS, ModBiomes.YANESE_PEAKS);

    public static final TerraRegion[] VALUES = values();

    /** RGB as painted in regions.png. */
    public final int colour;
    private final ResourceKey<Biome>[] slots;

    @SafeVarargs
    TerraRegion(int colour, ResourceKey<Biome>... slots) {
        if (slots.length != TerraSlot.COUNT) {
            throw new IllegalArgumentException(
                    name() + " declares " + slots.length + " slots, expected " + TerraSlot.COUNT);
        }
        this.colour = colour;
        this.slots = slots;
    }

    public ResourceKey<Biome> biome(TerraSlot slot) {
        return slots[slot.ordinal()];
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
     * The region whose colour is closest in RGB. Used only for pixels that are not an
     * exact match, which in practice means someone hand-edited the map with a soft brush.
     */
    public static TerraRegion byNearestColour(int rgb) {
        int r = (rgb >> 16) & 0xFF, g = (rgb >> 8) & 0xFF, b = rgb & 0xFF;
        TerraRegion best = BARRENLANDS;
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
