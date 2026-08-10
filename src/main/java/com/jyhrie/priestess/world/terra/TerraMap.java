package com.jyhrie.priestess.world.terra;

import com.mojang.logging.LogUtils;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.XoroshiroRandomSource;
import net.minecraft.world.level.levelgen.synth.ImprovedNoise;
import org.slf4j.Logger;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Terra's geography, loaded from two PNGs in the mod jar.
 *
 * <pre>
 *     data/priestess/terra/regions.png     one flat colour per region
 *     data/priestess/terra/elevation.png   greyscale, 0 = abyss, 255 = peak
 *     data/priestess/terra/relief.png      greyscale, 0 = flat, 255 = broken crag
 * </pre>
 *
 * <p>Elevation and relief are deliberately two maps and not one. Elevation says how high
 * the ground is; relief says how much it rises and falls once it gets there. Deriving the
 * second from the first — which is what this used to do — means a high plateau and a
 * rugged lowland are both unpaintable, and it makes brightening a mountain on the
 * elevation map quietly make it bumpier as well as taller.
 *
 * <h2>Why a fixed map at all</h2>
 * Terra has a real geography — Ægir is south of Iberia, the Foehn Hotlands are south of
 * Sargon, a mountain range runs from northern Kazimierz through Kjerag to the Sargonian
 * desert. A multi-noise biome source cannot express any of that: it says "Iberia is
 * wherever it is cold and coastal", so you get infinitely many Iberias and no ranges that
 * cross a border. This class trades infinite variety for one correct world.
 *
 * <h2>Loading</h2>
 * Straight off the classpath, not through the {@code ResourceManager}. Worldgen runs on
 * many threads and starts before a datapack-backed lookup would be convenient to reach;
 * the jar is always there. The cost is that a datapack cannot override the map — repaint
 * the PNGs and rebuild instead.
 *
 * <h2>Smoothing</h2>
 * A pixel covers many blocks, so sampled raw the world would be a grid of enormous
 * squares. Two things prevent that:
 * <ul>
 *   <li>every lookup is <em>domain-warped</em> by noise, at two scales, so borders wander
 *       across the pixel grid instead of following it;</li>
 *   <li>elevation is sampled <em>bilinearly</em>, so terrain height is continuous and the
 *       ground slopes between pixels rather than stepping.</li>
 * </ul>
 * Regions are sampled nearest-neighbour — a region is categorical, you cannot average
 * Iberia and Victoria — but they use the <em>same</em> warp as elevation, which is what
 * keeps a coastline in the region map on top of the coastline in the elevation map.
 */
public final class TerraMap {

    private static final Logger LOG = LogUtils.getLogger();

    /**
     * How wide Terra is, in blocks. <b>This is the scale knob — set the world size you
     * want and everything else follows.</b>
     *
     * <p>Blocks-per-pixel is <em>derived</em>, not configured:
     * {@code blocksPerPixel = WORLD_WIDTH_BLOCKS / regions.png width}. So the map's
     * resolution and the world's size are independent choices:
     *
     * <table border="1">
     *   <caption>Examples</caption>
     *   <tr><th>regions.png</th><th>WORLD_WIDTH_BLOCKS</th><th>blocks/px</th><th>World</th></tr>
     *   <tr><td>1024 x 640</td><td>131,072</td><td>128</td><td>131,072 x 81,920</td></tr>
     *   <tr><td>4092 x 4092</td><td>32,768</td><td>8</td><td>32,768 x 32,768 &mdash; today</td></tr>
     *   <tr><td>4092 x 4092</td><td>65,536</td><td>16</td><td>65,536 x 65,536</td></tr>
     *   <tr><td>2048 x 1280</td><td>131,072</td><td>64</td><td>131,072 x 81,920</td></tr>
     * </table>
     *
     * <p><b>Changing this moves {@link #ORIGIN_AT_BLOCK_X}.</b> The origin is written in
     * unshifted block coordinates, which are a function of this constant — halve the world
     * and every block coordinate on the map halves with it, so an origin left alone now
     * points at a different pixel and spawn quietly relocates. Scale the two together.
     *
     * <p>Only the <em>width</em> is given. Height comes from the image's aspect ratio, so
     * a square image gives a square world and Terra can never be accidentally stretched
     * by asking for a world shape the map does not have.
     *
     * <p>Repainting at a higher resolution therefore costs nothing but memory and detail:
     * the world stays the same size, each pixel just covers less ground. Note the memory
     * though — the two arrays are one byte per pixel each, so 1024x640 is 1.25 MiB but
     * 4096x4096 is 33 MiB, with a transient spike of maybe four times that while the
     * images decode.
     */
    public static final int WORLD_WIDTH_BLOCKS = 32_768;

    /**
     * Origin shift: the point on the map that should become block (0, 0), given in the
     * <em>unshifted</em> block coordinates it sits at today. Zero means "the centre of
     * the map", which is where Kazimierz is.
     *
     * <p>This is the knob for "spawn somewhere else". Read a coordinate out of the
     * region report that {@code runData} prints — say Iberia at {@code -12544, 25344} —
     * put those two numbers here, and Iberia is where players now arrive.
     *
     * <p>It shifts the whole world, it does not move a region relative to its
     * neighbours: the map, its warp and its terrain all translate together, so the world
     * is the same world seen through different coordinates. What does change is the
     * block extent of the map, which becomes
     * {@code [-width/2 - shift, +width/2 - shift]} — so with a large shift the far edge
     * is nearer the origin on one side and further on the other.
     *
     * <p>The generator knows nothing about this. It writes pixels; the shift is applied
     * when they are read, so you do not need to regenerate the PNGs after changing it.
     *
     * <p>Set to Columbia, because that is where the chapter starts: the player's ship comes
     * down in the Columbian wastes, and block (0, 0) should be the ground they land on. It
     * was the centre of the map — Kazimierz — until Movement I existed to arrive
     * in. These two numbers came out of the region report {@code runData} prints; move them
     * to another row of that table and the whole world is addressed from there instead.
     *
     * <p>They are block coordinates, so they are only meaningful at one
     * {@link #WORLD_WIDTH_BLOCKS}: these were {@code -10_240, -3_072} at the old
     * 65,536-block width and were halved with it, which lands on the same map pixel and
     * therefore the same square yard of Columbia. Rescale the world and rescale these, or
     * take a fresh pair out of the region report.
     */
    public static final int ORIGIN_AT_BLOCK_X = -5_120;
    public static final int ORIGIN_AT_BLOCK_Z = -1_536;

    private static final String REGIONS_PATH = "/data/priestess/terra/regions.png";
    private static final String ELEVATION_PATH = "/data/priestess/terra/elevation.png";
    private static final String RELIEF_PATH = "/data/priestess/terra/relief.png";

    /**
     * What relief.png falls back to when it is missing: grey 80, about ±15 blocks — the
     * "ordinary rolling country" value. A missing relief map is a warning and a uniformly
     * rolling world, not a crash, because it is the one of the three that a world can be
     * perfectly playable without.
     */
    private static final int DEFAULT_RELIEF_GREY = 80;

    /**
     * The blocks-per-pixel the warp constants were tuned at. Everything is expressed
     * relative to this, so the warp keeps the same size <em>in map pixels</em> whatever
     * scale you run at.
     *
     * <p>Without it the warp is a fixed distance in blocks, which is only correct at one
     * scale. Shrink the world to a quarter and a warp tuned to nudge borders by two
     * pixels starts throwing them eight, which shreds any region only a few pixels
     * across — Laterano and Dossoles are exactly that small. Do not change this constant;
     * change {@link #WORLD_WIDTH_BLOCKS} and let the ratio do the work.
     */
    private static final double TUNED_AT_BLOCKS_PER_PIXEL = 128.0;

    // Warp strength, at the tuned scale: +/-210 blocks (1.6 px) broad, +/-46 (0.4 px)
    // fine. The broad term moves whole coastlines around; the fine term breaks up what
    // is left of the pixel edges at walking scale. Scaled per-instance in the
    // constructor, once blocksPerPixel is known.
    private static final double TUNED_BROAD_WARP_BLOCKS = 210.0;
    private static final double TUNED_BROAD_WARP_WAVELENGTH = 1400.0;
    private static final double TUNED_FINE_WARP_BLOCKS = 46.0;
    private static final double TUNED_FINE_WARP_WAVELENGTH = 260.0;

    private static volatile TerraMap instance;

    private final int width;
    private final int height;
    private final byte[] regions;      // index into TerraRegion.VALUES
    private final byte[] elevation;    // 0..255, unsigned
    private final byte[] relief;       // 0..255, unsigned

    // Derived from WORLD_WIDTH_BLOCKS and the image size, so they cannot be set to
    // something inconsistent with each other.
    private final double blocksPerPixel;
    private final double broadWarpBlocks;
    private final double broadWarpScale;
    private final double fineWarpBlocks;
    private final double fineWarpScale;

    private final ImprovedNoise warpX;
    private final ImprovedNoise warpZ;
    private final ImprovedNoise warpFineX;
    private final ImprovedNoise warpFineZ;

    private TerraMap(BufferedImage regionImage, BufferedImage elevationImage, BufferedImage reliefImage) {
        this.width = regionImage.getWidth();
        this.height = regionImage.getHeight();

        if (elevationImage.getWidth() != width || elevationImage.getHeight() != height) {
            throw new IllegalStateException(String.format(
                    "regions.png is %dx%d but elevation.png is %dx%d — they must match",
                    width, height, elevationImage.getWidth(), elevationImage.getHeight()));
        }
        if (reliefImage != null
                && (reliefImage.getWidth() != width || reliefImage.getHeight() != height)) {
            throw new IllegalStateException(String.format(
                    "regions.png is %dx%d but relief.png is %dx%d — they must match",
                    width, height, reliefImage.getWidth(), reliefImage.getHeight()));
        }

        this.blocksPerPixel = WORLD_WIDTH_BLOCKS / (double) width;

        double warpScale = blocksPerPixel / TUNED_AT_BLOCKS_PER_PIXEL;
        this.broadWarpBlocks = TUNED_BROAD_WARP_BLOCKS * warpScale;
        this.broadWarpScale = 1.0 / (TUNED_BROAD_WARP_WAVELENGTH * warpScale);
        this.fineWarpBlocks = TUNED_FINE_WARP_BLOCKS * warpScale;
        this.fineWarpScale = 1.0 / (TUNED_FINE_WARP_WAVELENGTH * warpScale);

        this.regions = new byte[width * height];
        this.elevation = new byte[width * height];
        this.relief = new byte[width * height];

        if (reliefImage == null) {
            java.util.Arrays.fill(this.relief, (byte) DEFAULT_RELIEF_GREY);
            LOG.warn("{} is missing — every zone gets a uniform ~{} blocks of relief. "
                            + "Paint one to control ruggedness per place.",
                    RELIEF_PATH, DEFAULT_RELIEF_GREY * 48 / 255);
        }

        // Colour -> region is resolved once per distinct colour, not once per pixel: a
        // 1024x640 map is 655k pixels but only a couple of dozen colours.
        Map<Integer, TerraRegion> resolved = new HashMap<>();
        int unknownColours = 0;

        // Elevation is read straight off the raster, NOT through getRGB. A greyscale PNG
        // decodes to a linear grey colour space, and getRGB converts that to sRGB on the
        // way out — which silently applies a gamma curve. Elevation 0.16 came back as
        // 0.44, so the deep ocean generated as hill country. getSample returns the byte
        // that is actually in the file.
        var elevationRaster = elevationImage.getRaster();
        var reliefRaster = reliefImage == null ? null : reliefImage.getRaster();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = regionImage.getRGB(x, y) & 0xFFFFFF;
                TerraRegion region = resolved.get(rgb);
                if (region == null) {
                    region = TerraRegion.byExactColour(rgb);
                    if (region == null) {
                        region = TerraRegion.byNearestColour(rgb);
                        unknownColours++;
                        LOG.warn("regions.png colour #{} matches no region; snapping to {}",
                                String.format("%06X", rgb), region);
                    }
                    resolved.put(rgb, region);
                }
                int i = y * width + x;
                regions[i] = (byte) region.ordinal();
                elevation[i] = (byte) elevationRaster.getSample(x, y, 0);
                if (reliefRaster != null) {
                    relief[i] = (byte) reliefRaster.getSample(x, y, 0);
                }
            }
        }

        // Fixed seeds. The map is deliberately the same in every world — that is the
        // point of it — so the warp must not depend on the world seed either.
        this.warpX = new ImprovedNoise(new XoroshiroRandomSource(0x7E44A1L));
        this.warpZ = new ImprovedNoise(new XoroshiroRandomSource(0x1C93F5L));
        this.warpFineX = new ImprovedNoise(new XoroshiroRandomSource(0x5AB20DL));
        this.warpFineZ = new ImprovedNoise(new XoroshiroRandomSource(0x2F8C61L));

        LOG.info("Loaded Terra: {}x{} px at {} blocks/px = {}x{} blocks, {} colours{}",
                width, height, blocksPerPixel,
                widthInBlocks(), heightInBlocks(), resolved.size(),
                unknownColours > 0 ? ", " + unknownColours + " unrecognised" : "");
    }

    public static TerraMap get() {
        TerraMap local = instance;
        if (local == null) {
            synchronized (TerraMap.class) {
                local = instance;
                if (local == null) {
                    instance = local = load();
                }
            }
        }
        return local;
    }

    private static TerraMap load() {
        try (InputStream regionStream = TerraMap.class.getResourceAsStream(REGIONS_PATH);
             InputStream elevationStream = TerraMap.class.getResourceAsStream(ELEVATION_PATH);
             InputStream reliefStream = TerraMap.class.getResourceAsStream(RELIEF_PATH)) {
            if (regionStream == null || elevationStream == null) {
                throw new IOException("missing " + REGIONS_PATH + " or " + ELEVATION_PATH);
            }
            // relief.png is optional — see DEFAULT_RELIEF_GREY.
            return new TerraMap(ImageIO.read(regionStream), ImageIO.read(elevationStream),
                    reliefStream == null ? null : ImageIO.read(reliefStream));
        } catch (IOException e) {
            throw new IllegalStateException("Could not load the Terra map from the mod jar", e);
        }
    }

    // ── Sampling ──────────────────────────────────────────────────────────────

    /**
     * Warped pixel-space x for a block position.
     *
     * <p>The origin shift is applied first, to the position used for <em>everything</em>
     * including the warp lookup. That is what makes the shift a pure translation: warp
     * the shifted position and the noise pattern travels with the map, so the world is
     * identical, just addressed differently. Warping the unshifted position instead
     * would leave the warp field standing still while the map slid under it, quietly
     * reshaping every coastline.
     */
    private double pixelX(double blockX, double blockZ) {
        double mapX = blockX + ORIGIN_AT_BLOCK_X;
        double mapZ = blockZ + ORIGIN_AT_BLOCK_Z;
        double warp = warpX.noise(mapX * broadWarpScale, 0.0, mapZ * broadWarpScale)
                * broadWarpBlocks
                + warpFineX.noise(mapX * fineWarpScale, 0.0, mapZ * fineWarpScale)
                * fineWarpBlocks;
        return (mapX + warp) / blocksPerPixel + width * 0.5;
    }

    /** Warped pixel-space y for a block position. See {@link #pixelX}. */
    private double pixelZ(double blockX, double blockZ) {
        double mapX = blockX + ORIGIN_AT_BLOCK_X;
        double mapZ = blockZ + ORIGIN_AT_BLOCK_Z;
        double warp = warpZ.noise(mapX * broadWarpScale, 0.0, mapZ * broadWarpScale)
                * broadWarpBlocks
                + warpFineZ.noise(mapX * fineWarpScale, 0.0, mapZ * fineWarpScale)
                * fineWarpBlocks;
        return (mapZ + warp) / blocksPerPixel + height * 0.5;
    }

    /**
     * The region at a block position.
     *
     * <p>Past the edge of the map the lookup clamps to the nearest edge pixel, so the
     * world simply continues with whatever is painted on that edge. That is why the map
     * paints its own frontiers: the Infy Icefield runs off the top and the Foehn Hotlands
     * off the bottom, which canon calls "borders between Terra's civilized world and
     * terra incognita", while the left and right edges are ocean.
     *
     * <p>Clamping rather than special-casing also keeps the world edge clean. Returning a
     * fixed frontier region for out-of-bounds looks correct but is not: the lookup is
     * domain-warped, so positions near the edge flip back and forth across it and the
     * boundary comes out as a shimmering two-pixel band.
     */
    public TerraRegion regionAt(int blockX, int blockZ) {
        int px = clampX(Mth.floor(pixelX(blockX, blockZ)));
        int pz = clampZ(Mth.floor(pixelZ(blockX, blockZ)));
        return TerraRegion.VALUES[regions[pz * width + px] & 0xFF];
    }

    /** Normalised elevation in [0,1] at a block position, bilinearly interpolated. */
    public double elevationAt(double blockX, double blockZ) {
        return sample(elevation, blockX, blockZ);
    }

    /**
     * Normalised relief in [0,1] at a block position, bilinearly interpolated.
     *
     * <p>Bilinear rather than nearest-neighbour matters more here than it does for
     * elevation: a hard step in relief is a place where the hills stop mid-slope, which
     * reads as a seam. Painted gradients stay gradients all the way into the world.
     */
    public double reliefAt(double blockX, double blockZ) {
        return sample(relief, blockX, blockZ);
    }

    /** The terrain class at a block position. */
    public TerraSlot slotAt(int blockX, int blockZ) {
        return TerraSlot.of(elevationAt(blockX, blockZ));
    }

    /**
     * Bilinear lookup into one of the greyscale channels, through the shared domain warp.
     * Both channels use the same warp, which is what keeps a relief edge sitting exactly
     * on the coastline it was painted against.
     */
    private double sample(byte[] channel, double blockX, double blockZ) {
        double px = pixelX(blockX, blockZ);
        double pz = pixelZ(blockX, blockZ);

        // Sample at pixel centres, so the interpolation is symmetric about each pixel
        // rather than biased half a pixel north-west.
        double fx = px - 0.5;
        double fz = pz - 0.5;
        int x0 = Mth.floor(fx);
        int z0 = Mth.floor(fz);
        double tx = fx - x0;
        double tz = fz - z0;

        int x1 = clampX(x0 + 1);
        int z1 = clampZ(z0 + 1);
        x0 = clampX(x0);
        z0 = clampZ(z0);

        double e00 = raw(channel, x0, z0);
        double e10 = raw(channel, x1, z0);
        double e01 = raw(channel, x0, z1);
        double e11 = raw(channel, x1, z1);

        return Mth.lerp(tz, Mth.lerp(tx, e00, e10), Mth.lerp(tx, e01, e11));
    }

    private double raw(byte[] channel, int x, int z) {
        return (channel[z * width + x] & 0xFF) / 255.0;
    }

    private int clampX(int x) {
        return x < 0 ? 0 : Math.min(x, width - 1);
    }

    private int clampZ(int z) {
        return z < 0 ? 0 : Math.min(z, height - 1);
    }

    /** Blocks covered by one map pixel. Derived; see {@link #WORLD_WIDTH_BLOCKS}. */
    public double blocksPerPixel() {
        return blocksPerPixel;
    }

    public int widthInBlocks() {
        return WORLD_WIDTH_BLOCKS;
    }

    /** Follows the image's aspect ratio, so Terra is never stretched. */
    public int heightInBlocks() {
        return (int) Math.round(height * blocksPerPixel);
    }
}
