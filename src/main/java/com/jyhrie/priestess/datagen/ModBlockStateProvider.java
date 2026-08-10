package com.jyhrie.priestess.datagen;

import com.jyhrie.priestess.Priestess;
import com.jyhrie.priestess.block.BossSummonerBlock;
import com.jyhrie.priestess.block.ModBlocks;
import net.minecraft.data.PackOutput;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.client.model.generators.BlockStateProvider;
import net.minecraftforge.client.model.generators.ModelFile;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.registries.RegistryObject;

public class ModBlockStateProvider extends BlockStateProvider {
    public ModBlockStateProvider(PackOutput output, ExistingFileHelper exFileHelper) {
        super(output, Priestess.MOD_ID, exFileHelper);
    }

    @Override
    protected void registerStatesAndModels() {
        simpleBlockWithItem(ModBlocks.IBERIAN_SAND.get(), cubeAll(ModBlocks.IBERIAN_SAND.get()));
        simpleBlockWithItem(ModBlocks.IBERIAN_SANDSTONE.get(), cubeAll(ModBlocks.IBERIAN_SANDSTONE.get()));
        simpleBlockWithItem(ModBlocks.SIESTA_SAND.get(), cubeAll(ModBlocks.SIESTA_SAND.get()));
        simpleBlockWithItem(ModBlocks.BLACK_ICE.get(), cubeAll(ModBlocks.BLACK_ICE.get()));
        simpleBlockWithItem(ModBlocks.PALE_BEACH_SAND.get(), cubeAll(ModBlocks.PALE_BEACH_SAND.get()));
        simpleBlockWithItem(ModBlocks.DEAD_SEABED.get(), cubeAll(ModBlocks.DEAD_SEABED.get()));
        simpleBlockWithItem(ModBlocks.PERMAFROST.get(), cubeAll(ModBlocks.PERMAFROST.get()));

        summoner(ModBlocks.JESSELTON_PROJECTOR);
        summoner(ModBlocks.DOROTHYS_TERMINAL);
    }

    /**
     * A boss altar: two cube models, picked by {@link BossSummonerBlock#ARMED}, so a spent
     * altar is visibly spent. The inventory model is the armed one — that is what you are
     * holding when you place it.
     */
    private void summoner(RegistryObject<Block> block) {
        String name = block.getId().getPath();
        ModelFile armed = models().cubeAll(name, modLoc("block/" + name));
        ModelFile spent = models().cubeAll(name + "_spent", modLoc("block/" + name + "_spent"));

        getVariantBuilder(block.get())
                .partialState().with(BossSummonerBlock.ARMED, true)
                .modelForState().modelFile(armed).addModel()
                .partialState().with(BossSummonerBlock.ARMED, false)
                .modelForState().modelFile(spent).addModel();

        simpleBlockItem(block.get(), armed);
    }
}