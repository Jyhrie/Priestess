package com.jyhrie.priestess.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.energy.IEnergyStorage;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Finding and draining "machines" without knowing what a machine is.
 *
 * <p>This mod is built to drop into a larger tech pack and must not name a single one of
 * them. The only thing it asks of a block is that it exposes Forge's
 * {@link IEnergyStorage} capability — which every tech mod worth slotting in does, because
 * it is the interoperability contract they already agreed on. So "a machine" here means
 * "a block entity that stores energy", and the Originium Slug's whole reason to exist works
 * against a generator from any mod, or against none at all if the pack has none.
 *
 * <h2>Why it walks chunks rather than block positions</h2>
 * The obvious implementation — three nested loops over a cube of {@link BlockPos} calling
 * {@code getBlockEntity} — is 2,000-odd hash lookups for a radius of six, which is a lot of
 * work to usually find nothing. A chunk already keeps a map of exactly the block entities
 * in it, and there are at most four chunks in range, so asking the chunks is a couple of
 * dozen iterations instead.
 */
public final class Machines {

    /**
     * The nearest energy-storing block entity within {@code radius} blocks that currently
     * holds a charge, or null.
     *
     * <p>Empty machines are skipped on purpose: a slug that homes in on a drained battery
     * and then sits on it forever looks broken, and the fantasy is that they follow the
     * field, which an empty machine is not producing.
     */
    @Nullable
    public static BlockPos findCharged(Level level, BlockPos around, double radius) {
        BlockPos best = null;
        double bestDistance = radius * radius;

        for (BlockEntity blockEntity : inRange(level, around, radius)) {
            IEnergyStorage energy = energyOf(blockEntity);
            if (energy == null || energy.getEnergyStored() <= 0) {
                continue;
            }
            double distance = blockEntity.getBlockPos().distSqr(around);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = blockEntity.getBlockPos();
            }
        }
        return best;
    }

    /**
     * Pulls up to {@code amount} energy out of every machine in range and throws it away.
     *
     * @return the total drained, so the caller can tell whether anything happened and only
     *         play the effect if it did.
     */
    public static int drainAround(Level level, BlockPos around, double radius, int amount) {
        int drained = 0;
        for (BlockEntity blockEntity : inRange(level, around, radius)) {
            IEnergyStorage energy = energyOf(blockEntity);
            if (energy == null) {
                continue;
            }
            // simulate = false: this is theft, not a query. canExtract() is deliberately not
            // consulted — a machine refusing extraction is refusing a pipe, and the slug is
            // not a pipe. extractEnergy still returns what it allowed, so a machine that
            // genuinely cannot give any up simply gives up none.
            drained += energy.extractEnergy(amount, false);
        }
        return drained;
    }

    /** Whether there is anything worth draining in range — cheaper than a full scan result. */
    public static boolean anyCharged(Level level, BlockPos around, double radius) {
        return findCharged(level, around, radius) != null;
    }

    @Nullable
    private static IEnergyStorage energyOf(BlockEntity blockEntity) {
        if (blockEntity.isRemoved()) {
            return null;
        }
        // No side: a machine that only exposes energy on a particular face is still a
        // machine, but resolving every face here would be six capability lookups per block
        // entity per scan. The null side is the one every mod also answers.
        return blockEntity.getCapability(ForgeCapabilities.ENERGY).resolve().orElse(null);
    }

    /**
     * Every loaded block entity within {@code radius}. Chunks that are not loaded are
     * skipped rather than loaded — a mob must never drag chunks in.
     */
    private static List<BlockEntity> inRange(Level level, BlockPos around, double radius) {
        List<BlockEntity> found = new ArrayList<>();
        int minChunkX = SectionPos.blockToSectionCoord(around.getX() - (int) radius);
        int maxChunkX = SectionPos.blockToSectionCoord(around.getX() + (int) radius);
        int minChunkZ = SectionPos.blockToSectionCoord(around.getZ() - (int) radius);
        int maxChunkZ = SectionPos.blockToSectionCoord(around.getZ() + (int) radius);
        double radiusSqr = radius * radius;

        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                ChunkAccess chunk = level.getChunk(chunkX, chunkZ, ChunkStatus.FULL, false);
                if (!(chunk instanceof net.minecraft.world.level.chunk.LevelChunk levelChunk)) {
                    continue;
                }
                for (Map.Entry<BlockPos, BlockEntity> entry : levelChunk.getBlockEntities().entrySet()) {
                    if (entry.getKey().distSqr(around) <= radiusSqr) {
                        found.add(entry.getValue());
                    }
                }
            }
        }
        return found;
    }

    private Machines() {
    }
}
