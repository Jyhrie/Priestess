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
 * Terra's geography, loaded from three PNGs in the mod jar.
 *
 * <pre>
 *     data/priestess/terra/regions.png     one flat colour per region
 *     data/priestess/terra/elevation.png   greyscale, 0 = abyss, 255 = peak
 *     data/priestess/terra/relief.png      greyscale, 0 = flat, 255 = broken crag
 * </pre>
 *
 * <p>Terra has a real geography that a multi-noise biome source cannot express — it would
 * say "Iberia is wherever it is cold and coastal", giving infinitely many Iberias and no
 * mountain range that crosses a border. This class trades infinite variety for one correct
 * world. Elevation and relief are separate maps so that a high plateau and a rugged lowland
 * are both paintable.
 *
 * <p>Loading is straight off the classpath rather than through the {@code ResourceManager},
 * because worldgen runs on many threads and starts before a datapack-backed lookup is
 * convenient to reach. The cost is that a datapack cannot override the map.
 *
 * <p>A pixel covers many blocks, so two things stop the world being a grid of squares:
 * every lookup is domain-warped by noise at two scales, and elevation is sampled
 * bilinearly. Regions are nearest-neighbour — a region is categorical — but use the
 * <em>same</em> warp as elevation, which keeps the region map's coastline on top of the
 * elevation map's.
 */
public final class TerraMap {

    private static final Logger LOG = LogUtils.getLogger();

    /**
     * How wide Terra is, in blocks — the scale knob. Blocks-per-pixel is derived
     * ({@code WORLD_WIDTH_BLOCKS / regions.png width}), so map resolution and world size
     * are independent choices and repainting at a higher resolution costs only memory.
     * Only the width is given; height follows the image's aspect ratio, so Terra can never
     * be stretched.
     *
     * <p><b>Changing this moves {@link #ORIGIN_AT_BLOCK_X}</b>, which is written in
     * unshifted block coordinates. Halve the world and every block coordinate halves with
     * it, so an origin left alone points at a different pixel and spawn relocates.
     */
    public static final int WORLD_WIDTH_BLOCKS = 32_768;

    /**
     * Origin shift: the point on the map that should become block (0, 0), given in the
     * <em>unshifted</em> block coordinates it sits at today. Zero means the centre of the
     * map. Read a coordinate out of the region report {@code runData} prints and put it
     * here to make players arrive somewhere else.
     *
     * <p>It shifts the whole world rather than moving a region relative to its neighbours,
     * and it is applied on read, so the PNGs never need regenerating. The generator knows
     * nothing about it.
     *
     * <p>Set to Columbia, where the chapter starts. Being block coordinates, these are only
     * meaningful at one {@link #WORLD_WIDTH_BLOCKS} — rescale the world and rescale these,
     * or take a fresh pair out of the region report.
     */
    public static final int ORIGIN_AT_BLOCK_X = -5_120;
    public static final int ORIGIN_AT_BLOCK_Z = -1_536;

    private static final String REGIONS_PATH = "/data/priestess/terra/regions.png";
    private static final String ELEVATION_PATH = "/data/priestess/terra/elevation.png";
    private static final String RELIEF_PATH = "/data/priestess/terra/relief.png";

    /**
     * What relief.png falls back to when missing: about ±15 blocks, ordinary rolling
     * country. A missing relief map warns rather than crashing, because it is the one of
     * the three a world is perfectly playable without.
     */
    private static final int DEFAULT_RELIEF_GREY = 80;

    /**
     * The blocks-per-pixel the warp constants were tuned at. Everything is relative to
     * this so the warp keeps the same size <em>in map pixels</em> at any scale; otherwise a
     * warp tuned to nudge borders two pixels starts throwing them eight on a smaller world,
     * shredding regions only a few pixels across. Change {@link #WORLD_WIDTH_BLOCKS}
     * instead and let the ratio do the work.
     */
    private static final double TUNED_AT_BLOCKS_PER_PIXEL = 128.0;

    // At the tuned scale: +/-210 blocks (1.6 px) broad, +/-46 (0.4 px) fine. The broad term
    // moves whole coastlines; the fine term breaks up pixel edges at walking scale.
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

        // Resolved once per distinct colour, not once per pixel: a 1024x640 map is 655k
        // pixels but only a couple of dozen colours.
        Map<Integer, TerraRegion> resolved = new HashMap<>();
        int unknownColours = 0;

        // Read off the raster, NOT through getRGB. A greyscale PNG decodes to a linear grey
        // colour space and getRGB converts it to sRGB on the way out, silently applying a
        // gamma curve — elevation 0.16 came back as 0.44, generating deep ocean as hills.
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

        // Fixed seeds: the map is the same in every world, so the warp must not depend on
        // the world seed either.
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

    /**
     * Warped pixel-space x for a block position.
     *
     * <p>The origin shift is applied first, to the position used for the warp lookup too.
     * That is what makes the shift a pure translation — warping the unshifted position
     * would leave the warp field standing still while the map slid under it, reshaping
     * every coastline.
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
     * <p>Past the edge of the map the lookup clamps to the nearest edge pixel, so the world
     * continues with whatever is painted there — which is why the map paints its own
     * frontiers. Clamping rather than returning a fixed frontier region also keeps the edge
     * clean: the lookup is domain-warped, so positions near the edge would otherwise flip
     * back and forth across it and shimmer.
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
     * Normalised relief in [0,1] at a block position, bilinearly interpolated. Bilinear
     * matters more here than for elevation: a hard step in relief stops the hills mid-slope,
     * which reads as a seam.
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
     * Both channels use the same warp, which keeps a relief edge sitting exactly on the
     * coastline it was painted against.
     */
    private double sample(byte[] channel, double blockX, double blockZ) {
        double px = pixelX(blockX, blockZ);
        double pz = pixelZ(blockX, blockZ);

        // Sample at pixel centres, so interpolation is symmetric about each pixel rather
        // than biased half a pixel north-west.
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
