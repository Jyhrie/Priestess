package com.jyhrie.priestess.datagen;

import com.jyhrie.priestess.Priestess;
import com.jyhrie.priestess.damage.ModDamageTypes;
import com.jyhrie.priestess.world.dimension.ModDimensions;
import com.jyhrie.priestess.world.dimension.ModNoiseSettings;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraftforge.common.data.DatapackBuiltinEntriesProvider;
import com.jyhrie.priestess.world.dimension.ModBiomes;
import com.jyhrie.priestess.world.dimension.ModStructures;

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
            .add(Registries.NOISE_SETTINGS, ModNoiseSettings::bootstrap)

            // Not worldgen, but damage types are datapack registries too and this is the
            // one provider that writes them.
            .add(Registries.DAMAGE_TYPE, ModDamageTypes::bootstrap);

    public ModWorldGenProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries, BUILDER, Set.of(Priestess.MOD_ID));
    }
}