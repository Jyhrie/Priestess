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
 * <p>A position resolves in two lookups: which {@link TerraRegion} the pixel belongs to,
 * and which {@link TerraSlot} its elevation falls into. The pair indexes a flat table of
 * region x slot biomes. The {@link Climate.Sampler} handed in by the chunk generator is
 * ignored entirely — there is no climate model here any more, the map <em>is</em> the
 * model.
 *
 * <h2>The table is serialised, the layout is not</h2>
 * The codec writes out the biomes as one flat list in {@code TerraRegion.values()} x
 * {@code TerraSlot.values()} order. That keeps the dimension JSON self-describing and
 * datapack-overridable without needing to encode the enum names, at the cost that
 * reordering either enum invalidates an existing dimension JSON — so re-run
 * {@code gradlew runData} after any such reorder. The size check in the constructor is
 * what stops that mistake being silent.
 */
public class TerraMapBiomeSource extends BiomeSource {

    public static final Codec<TerraMapBiomeSource> CODEC = RecordCodecBuilder.create(
            instance -> instance.group(
                    RegistryFixedCodec.create(Registries.BIOME).listOf()
                            .fieldOf("biomes")
                            .forGetter(source -> source.table)
            ).apply(instance, TerraMapBiomeSource::new));

    private static final int EXPECTED = TerraRegion.VALUES.length * TerraSlot.COUNT;

    private final List<Holder<Biome>> table;

    public TerraMapBiomeSource(List<Holder<Biome>> table) {
        if (table.size() != EXPECTED) {
            throw new IllegalArgumentException(String.format(
                    "Terra map biome table has %d entries, expected %d (%d regions x %d slots). "
                            + "Re-run `gradlew runData` after changing TerraRegion or TerraSlot.",
                    table.size(), EXPECTED, TerraRegion.VALUES.length, TerraSlot.COUNT));
        }
        this.table = List.copyOf(table);
    }

    /** Builds the table from {@link TerraRegion}, for datagen. */
    public static TerraMapBiomeSource create(HolderGetter<Biome> biomes) {
        List<Holder<Biome>> table = new ArrayList<>(EXPECTED);
        for (TerraRegion region : TerraRegion.VALUES) {
            for (TerraSlot slot : TerraSlot.VALUES) {
                table.add(biomes.getOrThrow(region.biome(slot)));
            }
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

        TerraMap map = TerraMap.get();
        TerraRegion region = map.regionAt(blockX, blockZ);
        TerraSlot slot = TerraSlot.of(map.elevationAt(blockX, blockZ));

        return table.get(region.ordinal() * TerraSlot.COUNT + slot.ordinal());
    }
}
