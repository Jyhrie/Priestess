package com.jyhrie.priestess.datagen;

import com.jyhrie.priestess.Priestess;
import com.jyhrie.priestess.block.ModBlocks;
import com.jyhrie.priestess.item.ModItems;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.data.loot.packs.VanillaBlockLoot;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.advancements.critereon.StatePropertiesPredicate;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.PinkPetalsBlock;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.entries.LootPoolSingletonContainer;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.predicates.LootItemBlockStatePropertyCondition;
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
            this.dropSelf(ModBlocks.WHITEFLOWER.get());
            // A pot drops the pot and the plant as two separate items, which is the whole of
            // what dropPottedContents writes — and it reads the flower back off the block, so
            // there is nothing here to keep in step with ModBlocks.
            this.dropPottedContents(ModBlocks.POTTED_WHITEFLOWER.get());
            this.add(ModBlocks.WHITEFLOWER_PETALS.get(), petals(ModBlocks.WHITEFLOWER_PETALS.get()));
            // The altars drop themselves armed, whatever state they were broken in — the
            // block entity's boss goes with the block, so a spent altar picked up and put
            // back down is a fresh one. That is the forgiving reading, and the alternative
            // is an item that silently remembers a fight you already won.
            this.dropSelf(ModBlocks.JESSELTON_PROJECTOR.get());
            this.dropSelf(ModBlocks.DOROTHYS_TERMINAL.get());
            // Gated blocks drop themselves like any other decorative block. The gate is not in
            // the loot table — a sealed block's break is cancelled outright, so there is no
            // drop to suppress, and once the dungeon is cleared the blocks are ordinary
            // building material a player is meant to be able to keep.
            this.dropSelf(ModBlocks.RHINE_LAB_ARTS_LAB_CHISELED_WALL.get());
            this.dropSelf(ModBlocks.RHINE_LAB_ARTS_LAB_PLATED_WALL.get());
            this.dropSelf(ModBlocks.RHINE_LAB_ARTS_LAB_CONCRETE_WALL.get());
            this.dropSelf(ModBlocks.RHINE_LAB_ARTS_LAB_TILE.get());
            this.dropSelf(ModBlocks.RHINE_LAB_ARTS_LAB_PILLAR.get());
            this.dropSelf(ModBlocks.SAL_VIENTO_CATACOMBS_STONE.get());
            this.dropSelf(ModBlocks.SAL_VIENTO_CATACOMBS_OVERGROWN_STONE.get());
            this.dropSelf(ModBlocks.SAL_VIENTO_CATACOMBS_PIPE.get());
            this.dropSelf(ModBlocks.RMA70_12_DECORATIVE_PIPE.get());
            this.dropSelf(ModBlocks.RMA70_12_DECORATIVE_VENT.get());
            this.dropSelf(ModBlocks.RMA70_24_DECORATIVE_PIPE.get());
            this.dropSelf(ModBlocks.RMA70_24_DECORATIVE_VENT.get());
            this.dropSelf(ModBlocks.D32_STEEL_DECORATIVE_PIPE.get());
            this.dropSelf(ModBlocks.D32_STEEL_DECORATIVE_VENT.get());
            this.dropSelf(ModBlocks.IRIDESCENT_ALLOY_DECORATIVE_PIPE.get());
            this.dropSelf(ModBlocks.IRIDESCENT_ALLOY_DECORATIVE_VENT.get());
        }

        /**
         * Petals drop one item per petal in the block, so breaking a block of four gives four
         * back and the ground cover is not a way to delete items.
         *
         * <p>One entry carrying four conditional {@code set_count}s rather than four entries,
         * which is the shape of vanilla's own pink petals table: the entry is always the same
         * item, and only the count depends on the state. The explosion decay is what makes a
         * blown-up patch drop less, and it has to wrap the finished table rather than the
         * entry — it is a table-level function in vanilla's version too.
         */
        private LootTable.Builder petals(Block block) {
            LootPoolSingletonContainer.Builder<?> entry = LootItem.lootTableItem(block);
            for (int amount = PinkPetalsBlock.MIN_FLOWERS; amount <= PinkPetalsBlock.MAX_FLOWERS; amount++) {
                entry = entry.apply(SetItemCountFunction.setCount(ConstantValue.exactly(amount))
                        .when(LootItemBlockStatePropertyCondition.hasBlockStateProperties(block)
                                .setProperties(StatePropertiesPredicate.Builder.properties()
                                        .hasProperty(PinkPetalsBlock.AMOUNT, amount))));
            }
            return this.applyExplosionDecay(block,
                    LootTable.lootTable().withPool(LootPool.lootPool().add(entry)));
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
