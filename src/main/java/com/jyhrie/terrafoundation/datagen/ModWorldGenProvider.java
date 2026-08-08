package com.jyhrie.terrafoundation.datagen;

import com.jyhrie.terrafoundation.TerraFoundation;
import com.jyhrie.terrafoundation.world.dimension.ModDimensions;
import com.jyhrie.terrafoundation.world.dimension.ModNoiseSettings;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraftforge.common.data.DatapackBuiltinEntriesProvider;
import com.jyhrie.terrafoundation.world.dimension.ModBiomes;
import com.jyhrie.terrafoundation.world.dimension.ModStructures;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class ModWorldGenProvider extends DatapackBuiltinEntriesProvider {

    public static final RegistrySetBuilder BUILDER = new RegistrySetBuilder()
            .add(Registries.BIOME, ModBiomes::bootstrap)

            .add(Registries.TEMPLATE_POOL, ModStructures::bootstrapPools)
            .add(Registries.STRUCTURE, ModStructures::bootstrapStructures)
            .add(Registries.STRUCTURE_SET, ModStructures::bootstrapSets)

            .add(Registries.DIMENSION_TYPE, ModDimensions::bootstrapType)
            .add(Registries.LEVEL_STEM, ModDimensions::bootstrapStem)

            .add(Registries.NOISE, ModNoiseSettings::bootstrapNoise)
            .add(Registries.NOISE_SETTINGS, ModNoiseSettings::bootstrap);

    public ModWorldGenProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries, BUILDER, Set.of(TerraFoundation.MOD_ID));
    }
}