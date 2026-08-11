package com.jyhrie.priestess.datagen;

import com.jyhrie.priestess.Priestess;
import com.jyhrie.priestess.block.BossSummonerBlock;
import com.jyhrie.priestess.block.ModBlocks;
import net.minecraft.data.PackOutput;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
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

        // The Arts Lab build set. Four plain cubes and a pillar; the pillar takes the
        // _top/_side pair and gets a model per axis from axisBlock.
        simpleBlockWithItem(ModBlocks.RHINE_LAB_ARTS_LAB_CHISELED_WALL.get(),
                cubeAll(ModBlocks.RHINE_LAB_ARTS_LAB_CHISELED_WALL.get()));
        simpleBlockWithItem(ModBlocks.RHINE_LAB_ARTS_LAB_PLATED_WALL.get(),
                cubeAll(ModBlocks.RHINE_LAB_ARTS_LAB_PLATED_WALL.get()));
        simpleBlockWithItem(ModBlocks.RHINE_LAB_ARTS_LAB_CONCRETE_WALL.get(),
                cubeAll(ModBlocks.RHINE_LAB_ARTS_LAB_CONCRETE_WALL.get()));
        simpleBlockWithItem(ModBlocks.RHINE_LAB_ARTS_LAB_TILE.get(),
                cubeAll(ModBlocks.RHINE_LAB_ARTS_LAB_TILE.get()));
        pillar(ModBlocks.RHINE_LAB_ARTS_LAB_PILLAR);

        // The Sal Viento catacombs set. Plain cubes; nothing here knows they are gated.
        simpleBlockWithItem(ModBlocks.SAL_VIENTO_CATACOMBS_STONE.get(),
                cubeAll(ModBlocks.SAL_VIENTO_CATACOMBS_STONE.get()));
        simpleBlockWithItem(ModBlocks.SAL_VIENTO_CATACOMBS_OVERGROWN_STONE.get(),
                cubeAll(ModBlocks.SAL_VIENTO_CATACOMBS_OVERGROWN_STONE.get()));
    }

    /** A {@code RotatedPillarBlock}: one model, three axes, plus the upright inventory model. */
    private void pillar(RegistryObject<Block> block) {
        String name = block.getId().getPath();
        RotatedPillarBlock pillar = (RotatedPillarBlock) block.get();
        axisBlock(pillar, modLoc("block/" + name + "_side"), modLoc("block/" + name + "_top"));
        simpleBlockItem(pillar, models().getExistingFile(modLoc("block/" + name)));
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