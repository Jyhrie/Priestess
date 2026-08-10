package com.jyhrie.priestess.datagen;

import com.jyhrie.priestess.Priestess;
import com.jyhrie.priestess.block.ModBlocks;
import com.jyhrie.priestess.item.ModItems;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.data.loot.packs.VanillaBlockLoot;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;
import net.minecraftforge.registries.RegistryObject;

import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;

public class ModLootTableProvider {

    /**
     * The chest at the top of Rhine Lab. Referenced by name from the structure NBT, which is
     * why the id is a constant rather than something derived — the .nbt the build script
     * writes has this string baked into it.
     */
    public static final ResourceLocation RHINE_DIRECTORS_OFFICE =
            new ResourceLocation(Priestess.MOD_ID, "chests/rhine_directors_office");

    public static LootTableProvider create(PackOutput output) {
        return new LootTableProvider(output, Set.of(), List.of(
                new LootTableProvider.SubProviderEntry(BlockLoot::new, LootContextParamSets.BLOCK),
                new LootTableProvider.SubProviderEntry(ChestLoot::new, LootContextParamSets.CHEST)
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
            // The altars drop themselves armed, whatever state they were broken in — the
            // block entity's boss goes with the block, so a spent altar picked up and put
            // back down is a fresh one. That is the forgiving reading, and the alternative
            // is an item that silently remembers a fight you already won.
            this.dropSelf(ModBlocks.JESSELTONS_EFFIGY.get());
            this.dropSelf(ModBlocks.DOROTHYS_TERMINAL.get());
        }

        // Every block registered by the mod must get a table in generate(), or datagen fails.
        @Override
        protected Iterable<Block> getKnownBlocks() {
            return ModBlocks.BLOCKS.getEntries().stream().map(RegistryObject::get).toList();
        }
    }

    /**
     * Chest loot for the dungeons.
     *
     * <p>Only one table, and only because the chapter has to be able to end. Mansfield and
     * Dorothy's Vision point their chests at vanilla dungeon and stronghold tables: their
     * loot in the GDD is Columbian circuitry and riot gear, which do not exist yet, and
     * inventing stand-in items for them would be inventing content rather than scaffolding
     * it.
     *
     * <p>The Blueprint is different. It is the gate on the next chapter, so it has to come
     * from somewhere, and until there is a Rhine Lab Archival Mainframe to reboot it comes
     * out of a chest in the Director's Office. When the mainframe exists, this pool is what
     * gets deleted.
     */
    public static class ChestLoot implements net.minecraft.data.loot.LootTableSubProvider {
        @Override
        public void generate(BiConsumer<ResourceLocation, LootTable.Builder> output) {
            output.accept(RHINE_DIRECTORS_OFFICE, LootTable.lootTable()
                    // Its own pool with exactly one roll, so the blueprint is never competing
                    // with the flavour loot below for a slot.
                    .withPool(LootPool.lootPool()
                            .setRolls(ConstantValue.exactly(1.0F))
                            .add(LootItem.lootTableItem(ModItems.BLUEPRINT_ORIGINIUM_REFINEMENT.get())))
                    .withPool(LootPool.lootPool()
                            .setRolls(UniformGenerator.between(3.0F, 5.0F))
                            .add(LootItem.lootTableItem(Items.IRON_INGOT)
                                    .setWeight(6)
                                    .apply(SetItemCountFunction.setCount(UniformGenerator.between(2.0F, 6.0F))))
                            .add(LootItem.lootTableItem(Items.REDSTONE)
                                    .setWeight(6)
                                    .apply(SetItemCountFunction.setCount(UniformGenerator.between(4.0F, 12.0F))))
                            .add(LootItem.lootTableItem(Items.AMETHYST_SHARD)
                                    .setWeight(4)
                                    .apply(SetItemCountFunction.setCount(UniformGenerator.between(2.0F, 5.0F))))
                            .add(LootItem.lootTableItem(Items.DIAMOND)
                                    .setWeight(2)
                                    .apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 3.0F))))));
        }
    }
}
