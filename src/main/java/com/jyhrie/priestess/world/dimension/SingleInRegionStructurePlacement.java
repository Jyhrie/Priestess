package com.jyhrie.priestess.world.dimension;

import com.jyhrie.priestess.world.terra.TerraAnchors;
import com.jyhrie.priestess.world.terra.TerraRegion;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacementType;

import java.util.Optional;

/**
 * A structure placement that yields <em>exactly one</em> chunk in the whole world, somewhere
 * inside a named Terra region.
 *
 * <p>Neither vanilla placement can express this. {@code random_spread} spells "one per
 * region" as a spacing, and a spacing large enough to cover Terra still straddles four cells
 * at the origin, producing four prisons. {@code concentric_rings} takes a count but places by
 * radius from the world centre and looks only 112 blocks for a preferred biome, so on a
 * hand-authored map it lands wherever it lands — usually the sea.
 *
 * <p>This asks the map instead: {@link TerraAnchors} picks a spot inside the region from the
 * world seed, and this placement reports that one chunk and no other. Two saves put the
 * dungeon in two different places; no save puts it outside the region.
 *
 * <p>The structure still has to pass its own biome check when that chunk generates.
 * Candidates come from the region's interior so it cannot fail, but if a region is painted
 * narrow enough for the {@code TerraAnchors} fallback to kick in, the worst case is
 * <em>zero</em> of that structure, never two — the right failure direction for a progression
 * gate, since a duplicate Master Key is a broken chapter.
 *
 * <p>{@link #index} and {@link #total} are how several dungeons sharing one region stay out
 * of each other's way: they ask {@code TerraAnchors} for the same {@code total} positions and
 * each takes its own.
 *
 * <p>Registered as a placement <em>type</em> in code (see {@code Priestess}), because a codec
 * has to be in the registry before the structure-set JSON naming it can be read back.
 */
public class SingleInRegionStructurePlacement extends StructurePlacement {

    private static final Codec<TerraRegion> REGION_CODEC = Codec.STRING.comapFlatMap(
            name -> {
                try {
                    return DataResult.success(TerraRegion.valueOf(name));
                } catch (IllegalArgumentException e) {
                    return DataResult.error(() -> "Unknown Terra region: " + name);
                }
            },
            TerraRegion::name);

    public static final Codec<SingleInRegionStructurePlacement> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                            REGION_CODEC.fieldOf("region").forGetter(placement -> placement.region),
                            Codec.intRange(0, 255).fieldOf("index").forGetter(placement -> placement.index),
                            Codec.intRange(1, 256).fieldOf("total").forGetter(placement -> placement.total))
                    .apply(instance, SingleInRegionStructurePlacement::new));

    private final TerraRegion region;
    /** Which of the region's {@link #total} positions this structure takes. */
    private final int index;
    /** How many structures share this region, so every one of them asks for the same list. */
    private final int total;

    /** The seed an anchor was resolved for, and the anchor. Always replaced as a pair. */
    private record Resolved(long seed, ChunkPos anchor) {}

    /**
     * The resolved chunk, remembered so the common case is an int comparison — worldgen calls
     * {@code isPlacementChunk} once per chunk per structure set. The seed is kept with it
     * because one placement instance is shared by every world loaded from the same datapack.
     *
     * <p>One volatile field holding both is the whole reason {@link Resolved} exists: with
     * separate seed and anchor fields a reader can see the new seed next to the old anchor
     * and return the wrong chunk. Threads racing on this reference compute the same answer,
     * so no lock is needed beyond that.
     */
    private volatile Resolved resolved;

    public SingleInRegionStructurePlacement(TerraRegion region, int index, int total) {
        // All five inherited knobs are deliberately inert. Frequency 1 in particular: the
        // reducer is a dice roll per candidate chunk and there is exactly one candidate, so
        // rolling it would mean the dungeon sometimes does not exist. Separation is
        // TerraAnchors' job, which works in blocks across a region.
        super(Vec3i.ZERO, FrequencyReductionMethod.DEFAULT, 1.0F, 0, Optional.empty());
        this.region = region;
        this.index = index;
        this.total = total;
    }

    @Override
    protected boolean isPlacementChunk(ChunkGeneratorStructureState state, int x, int z) {
        ChunkPos anchor = anchorFor(state.getLevelSeed());
        return anchor.x == x && anchor.z == z;
    }

    /**
     * The one chunk this placement will ever accept, for a given world seed.
     *
     * <p>Public because {@link AnchorReport} needs the answer without a chunk to ask about:
     * {@link #isPlacementChunk} can only confirm a guess, and guessing is the problem.
     */
    public ChunkPos anchorFor(long seed) {
        Resolved cached = resolved;
        if (cached != null && cached.seed() == seed) {
            return cached.anchor();
        }
        ChunkPos anchor = TerraAnchors.forWorld(seed, region, total).get(index);
        resolved = new Resolved(seed, anchor);
        return anchor;
    }

    @Override
    public StructurePlacementType<?> type() {
        return ModStructurePlacements.SINGLE_IN_REGION.get();
    }
}
