package com.jyhrie.priestess.datagen;

import com.jyhrie.priestess.block.ModBlocks;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.data.loot.packs.VanillaBlockLoot;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraftforge.registries.RegistryObject;

import java.util.List;
import java.util.Set;

public class ModLootTableProvider {
    public static LootTableProvider create(PackOutput output) {
        return new LootTableProvider(output, Set.of(), List.of(
                new LootTableProvider.SubProviderEntry(BlockLoot::new, LootContextParamSets.BLOCK)
        ));
    }

    public static class BlockLoot extends VanillaBlockLoot {
        @Override
        protected void generate() {
            this.dropSelf(ModBlocks.IBERIAN_SAND.get());
            this.dropSelf(ModBlocks.IBERIAN_SANDSTONE.get());
            this.dropSelf(ModBlocks.SIESTA_SAND.get());
            this.dropSelf(ModBlocks.BLACK_ICE.get());
            this.dropSelf(ModBlocks.PALE_BEACH_SAND.get());
            this.dropSelf(ModBlocks.DEAD_SEABED.get());
            this.dropSelf(ModBlocks.PERMAFROST.get());
        }

        // Every block registered by the mod must get a table in generate(), or datagen fails.
        @Override
        protected Iterable<Block> getKnownBlocks() {
            return ModBlocks.BLOCKS.getEntries().stream().map(RegistryObject::get).toList();
        }
    }
}