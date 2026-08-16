package com.jyhrie.priestess.world.terra;

import com.mojang.logging.LogUtils;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Where a one-per-world structure goes: somewhere inside a named region, chosen from the
 * world seed. Every save puts Mansfield somewhere different, and every save puts it in
 * Columbia.
 *
 * <p>That is a deliberate exception to Terra's seed-independence — the continent is the same
 * in every world, but a landmark that is in the same field in everybody's world is a wiki
 * coordinate rather than a thing you find.
 *
 * <p>The candidate scan runs the whole map, once per region, the first time a structure asks
 * — so during world creation, not during play. Both caches are keyed on immutable data and
 * safe to hit from the several threads worldgen runs on.
 */
public final class TerraAnchors {

    private static final Logger LOG = LogUtils.getLogger();

    /**
     * Grid spacing for the candidate scan, in blocks. Sixteen chunks: fine enough that a
     * region the size of Columbia still offers hundreds of spots, coarse enough that
     * scanning Terra is tens of thousands of samples rather than millions.
     */
    private static final int SAMPLE_STEP = 256;

    /**
     * Slots at or above this count as dry land. {@link TerraSlot#SHORE} is excluded because
     * its band straddles the waterline, so half of it is surf.
     */
    private static final TerraSlot MINIMUM_LAND = TerraSlot.LOWLAND;

    /**
     * How far apart two structures in the same region have to be, in blocks. Far enough that
     * finding one tells you nothing about where the next is, and that their bounding boxes
     * cannot argue with each other.
     */
    private static final int MINIMUM_SEPARATION = 1_024;

    /** Give up on the separation rule rather than loop forever in a region too small for it. */
    private static final int PLACEMENT_ATTEMPTS = 64;

    private record Sample(int blockX, int blockZ) {}

    private record SeededKey(long seed, TerraRegion region, int count) {}

    private static final Map<TerraRegion, List<Sample>> CANDIDATES = new ConcurrentHashMap<>();
    private static final Map<SeededKey, List<ChunkPos>> CHOSEN = new ConcurrentHashMap<>();

    /**
     * {@code count} chunk positions inside {@code region}, chosen for this world and stable
     * for as long as it has this seed. The same arguments always return the same list, which
     * is what lets a {@code StructurePlacement} answer "is this my chunk?" consistently
     * across the many threads that ask it.
     *
     * @throws IllegalStateException if the region has no interior at all — a map problem that
     *         has to be loud, because the alternative is a chapter whose dungeons quietly do
     *         not exist.
     */
    public static List<ChunkPos> forWorld(long seed, TerraRegion region, int count) {
        return CHOSEN.computeIfAbsent(new SeededKey(seed, region, count), key -> choose(key));
    }

    private static List<ChunkPos> choose(SeededKey key) {
        List<Sample> candidates = CANDIDATES.computeIfAbsent(key.region(), TerraAnchors::scan);
        if (candidates.isEmpty()) {
            throw new IllegalStateException(String.format(
                    "%s is not painted anywhere in regions.png, so the %d structure(s) placed in "
                            + "it cannot generate. Either give the region some ground or point those "
                            + "structures at a region that has some.",
                    key.region(), key.count()));
        }

        // Mixed with the region so two regions do not draw the same sequence.
        RandomSource random = RandomSource.create(key.seed() ^ ((long) key.region().ordinal() << 40));

        List<Sample> chosen = new ArrayList<>(key.count());
        for (int i = 0; i < key.count(); i++) {
            chosen.add(pickApartFrom(candidates, chosen, random));
        }

        List<ChunkPos> anchors = new ArrayList<>(key.count());
        for (Sample sample : chosen) {
            anchors.add(new ChunkPos(sample.blockX() >> 4, sample.blockZ() >> 4));
        }
        // /locate only searches a hundred chunks or so and one of these can be thousands
        // away, so the server log is the only place the coordinates exist.
        LOG.info("Terra anchors in {} for seed {}: {} candidates, chose {}",
                key.region(), key.seed(), candidates.size(),
                anchors.stream().map(pos -> "(" + pos.getMiddleBlockX() + ", " + pos.getMiddleBlockZ() + ")").toList());
        return List.copyOf(anchors);
    }

    /**
     * A candidate at random, retried until it clears {@link #MINIMUM_SEPARATION} from
     * everything already picked. Rejection sampling beats filtering the list because almost
     * every draw succeeds first try. After {@link #PLACEMENT_ATTEMPTS} failures it takes
     * what it drew — in a cramped region, two dungeons too close together beats one that
     * never generates.
     */
    private static Sample pickApartFrom(List<Sample> candidates, List<Sample> taken, RandomSource random) {
        Sample fallback = candidates.get(random.nextInt(candidates.size()));
        for (int attempt = 0; attempt < PLACEMENT_ATTEMPTS; attempt++) {
            Sample sample = candidates.get(random.nextInt(candidates.size()));
            if (isClearOf(sample, taken)) {
                return sample;
            }
        }
        return fallback;
    }

    private static boolean isClearOf(Sample sample, List<Sample> taken) {
        long minimum = (long) MINIMUM_SEPARATION * MINIMUM_SEPARATION;
        for (Sample other : taken) {
            long dx = (long) sample.blockX() - other.blockX();
            long dz = (long) sample.blockZ() - other.blockZ();
            if (dx * dx + dz * dz < minimum) {
                return false;
            }
        }
        return true;
    }

    /**
     * Every spot in the region a structure could stand on, strictest first: interior land,
     * then any land, then any ground at all. The ladder exists because a region painted to
     * hold one dungeon can be narrower than two sample steps, at which point the interior
     * test rejects every pixel of it.
     */
    private static List<Sample> scan(TerraRegion region) {
        TerraMap map = TerraMap.get();
        int originX = -map.widthInBlocks() / 2 - TerraMap.ORIGIN_AT_BLOCK_X;
        int originZ = -map.heightInBlocks() / 2 - TerraMap.ORIGIN_AT_BLOCK_Z;

        List<Sample> interior = new ArrayList<>();
        List<Sample> land = new ArrayList<>();
        List<Sample> anywhere = new ArrayList<>();

        for (int blockZ = originZ; blockZ < originZ + map.heightInBlocks(); blockZ += SAMPLE_STEP) {
            for (int blockX = originX; blockX < originX + map.widthInBlocks(); blockX += SAMPLE_STEP) {
                if (map.regionAt(blockX, blockZ) != region) {
                    continue;
                }
                Sample sample = new Sample(blockX, blockZ);
                anywhere.add(sample);
                if (TerraSlot.of(map.elevationAt(blockX, blockZ)).ordinal() < MINIMUM_LAND.ordinal()) {
                    continue;
                }
                land.add(sample);
                if (isInterior(map, region, blockX, blockZ)) {
                    interior.add(sample);
                }
            }
        }

        if (!interior.isEmpty()) {
            return Collections.unmodifiableList(interior);
        }
        if (!land.isEmpty()) {
            LOG.warn("{} has no land far enough from its own border to be sure of the biome check "
                            + "({} land samples, none interior). Falling back to any dry land in it — "
                            + "if a structure anchored here fails to generate, the region is painted "
                            + "narrower than {} blocks and wants widening.",
                    region, land.size(), SAMPLE_STEP * 2);
            return Collections.unmodifiableList(land);
        }
        if (!anywhere.isEmpty()) {
            LOG.warn("{} is entirely at or below the shore band ({} samples, no dry land). "
                            + "Falling back to placing structures in it anyway; expect them underwater.",
                    region, anywhere.size());
        }
        return Collections.unmodifiableList(anywhere);
    }

    /**
     * In the region, on dry land, and surrounded on all eight sides by ground that is also
     * both. A structure whose chunk lands just outside the region fails its biome check and
     * simply does not appear, which looks like a bug rather than like a spot two hundred
     * blocks too close to the coast. One sample step out is comfortably more than the map's
     * domain warp can move a border.
     */
    private static boolean isInterior(TerraMap map, TerraRegion region, int blockX, int blockZ) {
        for (int dz = -1; dz <= 1; dz++) {
            for (int dx = -1; dx <= 1; dx++) {
                int x = blockX + dx * SAMPLE_STEP;
                int z = blockZ + dz * SAMPLE_STEP;
                if (map.regionAt(x, z) != region) {
                    return false;
                }
                if (TerraSlot.of(map.elevationAt(x, z)).ordinal() < MINIMUM_LAND.ordinal()) {
                    return false;
                }
            }
        }
        return true;
    }

    private TerraAnchors() {
    }
}
