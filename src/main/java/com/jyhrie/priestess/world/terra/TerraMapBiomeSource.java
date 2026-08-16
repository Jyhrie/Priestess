package com.jyhrie.priestess.world.terra;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.QuartPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.RegistryFixedCodec;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Picks biomes from Terra's map rather than from climate noise.
 *
 * <p>A position resolves in one lookup: which {@link TerraRegion} the pixel belongs to. That
 * zone names one biome and that is the answer, at every height. The {@link Climate.Sampler}
 * handed in by the chunk generator is ignored entirely — the map <em>is</em> the model.
 * Elevation only shapes terrain height, through {@code ModNoiseSettings}.
 *
 * <p>The codec writes the biomes as one flat list in {@code TerraRegion.values()} order,
 * which keeps the dimension JSON datapack-overridable without encoding the enum names — at
 * the cost that reordering the enum invalidates an existing dimension JSON. Re-run
 * {@code gradlew runData} after any such reorder; the constructor's size check is what stops
 * that mistake being silent.
 */
public class TerraMapBiomeSource extends BiomeSource {

    public static final Codec<TerraMapBiomeSource> CODEC = RecordCodecBuilder.create(
            instance -> instance.group(
                    RegistryFixedCodec.create(Registries.BIOME).listOf()
                            .fieldOf("biomes")
                            .forGetter(source -> source.table)
            ).apply(instance, TerraMapBiomeSource::new));

    private static final int EXPECTED = TerraRegion.VALUES.length;

    private final List<Holder<Biome>> table;

    public TerraMapBiomeSource(List<Holder<Biome>> table) {
        if (table.size() != EXPECTED) {
            throw new IllegalArgumentException(String.format(
                    "Terra map biome table has %d entries, expected %d (one per zone). "
                            + "Re-run `gradlew runData` after changing TerraRegion.",
                    table.size(), EXPECTED));
        }
        this.table = List.copyOf(table);
    }

    /** Builds the table from {@link TerraRegion}, for datagen. */
    public static TerraMapBiomeSource create(HolderGetter<Biome> biomes) {
        List<Holder<Biome>> table = new ArrayList<>(EXPECTED);
        for (TerraRegion region : TerraRegion.VALUES) {
            table.add(biomes.getOrThrow(region.biome()));
        }
        return new TerraMapBiomeSource(table);
    }

    @Override
    protected Codec<? extends BiomeSource> codec() {
        return CODEC;
    }

    @Override
    protected Stream<Holder<Biome>> collectPossibleBiomes() {
        return table.stream().distinct();
    }

    @Override
    public Holder<Biome> getNoiseBiome(int quartX, int quartY, int quartZ, Climate.Sampler ignored) {
        int blockX = QuartPos.toBlock(quartX);
        int blockZ = QuartPos.toBlock(quartZ);

        return table.get(TerraMap.get().regionAt(blockX, blockZ).ordinal());
    }
}
