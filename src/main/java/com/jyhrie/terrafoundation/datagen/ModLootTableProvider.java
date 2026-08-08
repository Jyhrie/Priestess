package com.jyhrie.terrafoundation.datagen;

import com.jyhrie.terrafoundation.block.ModBlocks;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.data.loot.packs.VanillaBlockLoot;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;

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
        }

        @Override
        protected Iterable<Block> getKnownBlocks() {
            return ModBlocks.BLOCKS.getEntries().stream().map(net.minecraftforge.registries.RegistryObject::get)::iterator;
        }
    }
}