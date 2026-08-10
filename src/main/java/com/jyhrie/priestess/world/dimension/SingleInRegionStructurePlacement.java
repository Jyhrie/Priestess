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
 * <h2>Why vanilla cannot do this</h2>
 * The two placements the game ships with both answer a different question. {@code
 * random_spread} lays a grid over the world and jitters a structure inside every cell — "one
 * per region" is spelled as a spacing, so "exactly one, ever" is not expressible: a spacing
 * large enough to cover Terra still straddles four cells at the origin and can produce four
 * prisons. {@code concentric_rings} does take a count, but it places by radius from the
 * world centre at an angle it picks itself and then looks only 112 blocks for a preferred
 * biome, which on a hand-authored map means it lands wherever it lands — usually the sea.
 *
 * <p>What this does instead is ask the map. {@link TerraAnchors} knows where a region
 * physically is, because the same PNG that decides the biomes decides that; it picks a spot
 * inside it from the world seed, and this placement reports that one chunk and no other.
 *
 * <h2>One per world, anywhere in the biome</h2>
 * Both halves matter, and they come from different places:
 * <ul>
 *   <li><b>One</b> is this class. There is a single chunk it will ever say yes to.</li>
 *   <li><b>Anywhere in the biome</b> is {@link TerraAnchors}, which draws from every spot in
 *       the region the world seed could have picked. Two saves put the dungeon in two
 *       different places; no save puts it outside the region.</li>
 * </ul>
 *
 * <p>The structure still has to pass its own biome check when that chunk generates.
 * Candidates are drawn from the region's interior precisely so it cannot fail, but if a
 * region is painted so narrow that the fallback in {@code TerraAnchors} kicks in, the worst
 * case is <em>zero</em> of that structure, never two. That is the right failure direction
 * for a progression gate: a duplicate Master Key is a broken chapter, a missing dungeon is a
 * visible bug with a warning already in the log.
 *
 * <h2>Several structures in one region</h2>
 * {@link #index} and {@link #total} are how three dungeons sharing Columbia stay out of each
 * other's way — they ask {@code TerraAnchors} for the same {@code total} positions and each
 * takes its own. When each dungeon gets the region painted for it, {@code total} becomes 1
 * and this degrades to "the one spot in the one region" without anything changing.
 *
 * <p>Registered as a placement <em>type</em> in code (see {@code Priestess}), because a
 * codec has to be in the registry before the structure-set JSON that names it can be read
 * back — the same reason the Terra biome source and density functions are registered there.
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
     * The resolved chunk, remembered so the common case is an int comparison.
     *
     * <p>{@code isPlacementChunk} is called once per chunk per structure set for the whole
     * of worldgen, and the lookup behind it is a map hit on a record key — cheap, but not
     * cheap enough to want tens of thousands of times. The seed is kept with it because one
     * placement instance is shared by every world loaded from the same datapack, so the
     * cache has to notice when it is being asked about a different world.
     *
     * <p>One volatile field holding both, rather than two fields, and that is the whole
     * reason {@link Resolved} exists: with a seed field and an anchor field, a reader can
     * see the new seed next to the old anchor and confidently return the wrong chunk. Two
     * threads racing on this single reference compute the same answer and write the same
     * value, so no lock is needed beyond that.
     */
    private volatile Resolved resolved;

    public SingleInRegionStructurePlacement(TerraRegion region, int index, int total) {
        // The five inherited knobs are all deliberately inert:
        //   locateOffset ZERO      — /locate should point at the chunk, not next to it;
        //   DEFAULT + frequency 1  — the frequency reducer is a dice roll per candidate
        //                            chunk, and there is exactly one candidate, so rolling
        //                            it would mean the prison sometimes does not exist;
        //   salt 0                 — salt only feeds that dice roll, so it has nothing to do;
        //   no exclusion zone      — separation between these is TerraAnchors' job, and it
        //                            works in blocks across a region rather than in chunks
        //                            against one other structure set.
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
