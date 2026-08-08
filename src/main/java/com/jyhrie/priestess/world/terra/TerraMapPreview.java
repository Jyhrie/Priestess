package com.jyhrie.priestess.world.terra;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;

/**
 * Renders what the world will <em>actually</em> look like, by asking the real
 * {@link TerraMap} rather than by re-reading the source PNGs.
 *
 * <pre>
 *     gradlew runData   ->  docs/terra_world_preview.png
 * </pre>
 *
 * <p>This exists because the source PNGs are not the world. Between them and the ground
 * you stand on sit a two-scale domain warp and a bilinear filter, and those can quietly
 * ruin a layout — a warp strong enough to look organic is also strong enough to tear a
 * small nation apart or push a coastline over a mountain range. Repaint the map, run
 * datagen, look at the preview. It samples through the same code path the chunk
 * generator uses, so what you see is what generates.
 *
 * <p>Left panel: region colours, shaded by terrain slot. Right panel: the terrain slot on
 * its own, which is what actually decides the biome within a region.
 *
 * <p>It runs as part of datagen rather than as a standalone {@code main} for an annoying
 * but immovable reason: {@link TerraRegion} holds {@code ResourceKey}s, touching one
 * initialises {@code Registries}, and that throws unless the game has bootstrapped —
 * which under Forge cannot be done outside the mod launcher. The datagen JVM is already
 * a bootstrapped Forge environment, so the render is free there.
 */
public final class TerraMapPreview {

    /** Target width of each panel, in output pixels. The sampling rate follows from it. */
    private static final int PANEL_WIDTH = 512;

    private static final Map<TerraSlot, Integer> SLOT_COLOURS = new EnumMap<>(TerraSlot.class);

    static {
        SLOT_COLOURS.put(TerraSlot.DEEP_SEA, 0x0B2545);
        SLOT_COLOURS.put(TerraSlot.SEA, 0x1D4E7A);
        SLOT_COLOURS.put(TerraSlot.SHORE, 0xE8DCA0);
        SLOT_COLOURS.put(TerraSlot.LOWLAND, 0x7FA85C);
        SLOT_COLOURS.put(TerraSlot.FLATS, 0x9CB86A);
        SLOT_COLOURS.put(TerraSlot.MIDLAND, 0xB0A070);
        SLOT_COLOURS.put(TerraSlot.HILLS, 0x9A7F5E);
        SLOT_COLOURS.put(TerraSlot.MOUNTAIN, 0xF0F0F5);
    }

    /** Renders the preview to {@code file} and returns a short human-readable report. */
    public static String render(File file) throws Exception {
        TerraMap map = TerraMap.get();
        StringBuilder report = new StringBuilder();

        // Derived from the world size rather than fixed, so the preview stays the same
        // physical size on screen whatever scale the world is built at. A hardcoded
        // blocks-per-pixel renders a quarter-size world as a 128px thumbnail.
        int blocksPerPixel = Math.max(1, map.widthInBlocks() / PANEL_WIDTH);
        int width = map.widthInBlocks() / blocksPerPixel;
        int height = map.heightInBlocks() / blocksPerPixel;
        // Follow the origin shift, so the preview always frames the whole map and the
        // coordinates it prints are the ones players will actually stand on.
        int originX = -map.widthInBlocks() / 2 - TerraMap.ORIGIN_AT_BLOCK_X;
        int originZ = -map.heightInBlocks() / 2 - TerraMap.ORIGIN_AT_BLOCK_Z;

        BufferedImage out = new BufferedImage(width * 2 + 8, height, BufferedImage.TYPE_INT_RGB);
        int[] slotTally = new int[TerraSlot.COUNT];
        int[] regionTally = new int[TerraRegion.VALUES.length];

        for (int py = 0; py < height; py++) {
            for (int px = 0; px < width; px++) {
                int blockX = originX + px * blocksPerPixel;
                int blockZ = originZ + py * blocksPerPixel;

                TerraRegion region = map.regionAt(blockX, blockZ);
                TerraSlot slot = TerraSlot.of(map.elevationAt(blockX, blockZ));
                slotTally[slot.ordinal()]++;
                regionTally[region.ordinal()]++;

                // Left: the political map, darkened at low ground and lightened high.
                double shade = 0.55 + 0.75 * (slot.ordinal() / (double) (TerraSlot.COUNT - 1));
                out.setRGB(px, py, scale(region.colour, shade));

                // Right: terrain slots alone.
                out.setRGB(px + width + 8, py, SLOT_COLOURS.get(slot));
            }
        }

        file.getAbsoluteFile().getParentFile().mkdirs();
        ImageIO.write(out, "png", file);

        int total = width * height;
        report.append(String.format("Terra preview -> %s (%dx%d px, %d blocks/px)%n",
                file.getPath(), out.getWidth(), out.getHeight(), blocksPerPixel));

        report.append("  terrain slots:");
        for (TerraSlot slot : TerraSlot.VALUES) {
            report.append(String.format(" %s=%.1f%%", slot, 100.0 * slotTally[slot.ordinal()] / total));
        }
        report.append('\n');

        // A region with no pixels is almost always a mistake — a seed drowned by an
        // inland sea, or a colour in TerraRegion that is not in regions.png.
        StringBuilder unreachable = new StringBuilder();
        for (TerraRegion region : TerraRegion.VALUES) {
            if (regionTally[region.ordinal()] == 0) {
                unreachable.append(unreachable.length() == 0 ? "" : ", ").append(region);
            }
        }
        if (unreachable.length() > 0) {
            report.append("  UNREACHABLE REGIONS: ").append(unreachable).append('\n');
        }

        // A representative coordinate per region, so "where is Kjerag" has an answer you
        // can paste into /tp. Take the centroid of the region's samples, then snap to the
        // nearest sample actually inside it — a centroid alone lands outside anything
        // crescent-shaped, and half these regions wrap around a coastline.
        long[] sumX = new long[TerraRegion.VALUES.length];
        long[] sumZ = new long[TerraRegion.VALUES.length];
        for (int py = 0; py < height; py++) {
            for (int px = 0; px < width; px++) {
                int blockX = originX + px * blocksPerPixel;
                int blockZ = originZ + py * blocksPerPixel;
                int r = map.regionAt(blockX, blockZ).ordinal();
                sumX[r] += blockX;
                sumZ[r] += blockZ;
            }
        }

        int[] bestX = new int[TerraRegion.VALUES.length];
        int[] bestZ = new int[TerraRegion.VALUES.length];
        long[] bestDistance = new long[TerraRegion.VALUES.length];
        Arrays.fill(bestDistance, Long.MAX_VALUE);
        for (int py = 0; py < height; py++) {
            for (int px = 0; px < width; px++) {
                int blockX = originX + px * blocksPerPixel;
                int blockZ = originZ + py * blocksPerPixel;
                int r = map.regionAt(blockX, blockZ).ordinal();
                if (regionTally[r] == 0) {
                    continue;
                }
                long dx = blockX - sumX[r] / regionTally[r];
                long dz = blockZ - sumZ[r] / regionTally[r];
                long d = dx * dx + dz * dz;
                if (d < bestDistance[r]) {
                    bestDistance[r] = d;
                    bestX[r] = blockX;
                    bestZ[r] = blockZ;
                }
            }
        }

        TerraRegion[] byShare = TerraRegion.VALUES.clone();
        Arrays.sort(byShare, (a, b) -> regionTally[b.ordinal()] - regionTally[a.ordinal()]);
        report.append(String.format("  %-14s %7s  %-9s  %s%n", "region", "share", "slot", "go here"));
        for (TerraRegion region : byShare) {
            int r = region.ordinal();
            if (regionTally[r] == 0) {
                report.append(String.format("  %-14s %7s%n", region, "---"));
                continue;
            }
            TerraSlot slot = TerraSlot.of(map.elevationAt(bestX[r], bestZ[r]));
            report.append(String.format("  %-14s %6.2f%%  %-9s  /execute in priestess:terra run tp @s %d ~ %d%n",
                    region, 100.0 * regionTally[r] / total, slot, bestX[r], bestZ[r]));
        }
        return report.toString();
    }

    private static int scale(int rgb, double factor) {
        int r = Math.min(255, (int) (((rgb >> 16) & 0xFF) * factor));
        int g = Math.min(255, (int) (((rgb >> 8) & 0xFF) * factor));
        int b = Math.min(255, (int) ((rgb & 0xFF) * factor));
        return (r << 16) | (g << 8) | b;
    }

    private TerraMapPreview() {
    }
}
