package com.jyhrie.priestess.datagen;

import com.jyhrie.priestess.Priestess;
import com.jyhrie.priestess.block.BossSummonerBlock;
import com.jyhrie.priestess.block.DecorativePipeBlock;
import com.jyhrie.priestess.block.ModBlocks;
import net.minecraft.core.Direction;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraftforge.client.model.generators.BlockStateProvider;
import net.minecraftforge.client.model.generators.ModelFile;
import net.minecraftforge.client.model.generators.MultiPartBlockStateBuilder;
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

        // Pipes and vents. A vent is an ordinary cube — only the pipe has connection state.
        pipe(ModBlocks.SAL_VIENTO_CATACOMBS_PIPE);

        pipe(ModBlocks.RMA70_12_DECORATIVE_PIPE);
        simpleBlockWithItem(ModBlocks.RMA70_12_DECORATIVE_VENT.get(),
                cubeAll(ModBlocks.RMA70_12_DECORATIVE_VENT.get()));

        pipe(ModBlocks.RMA70_24_DECORATIVE_PIPE);
        simpleBlockWithItem(ModBlocks.RMA70_24_DECORATIVE_VENT.get(),
                cubeAll(ModBlocks.RMA70_24_DECORATIVE_VENT.get()));

        pipe(ModBlocks.D32_STEEL_DECORATIVE_PIPE);
        simpleBlockWithItem(ModBlocks.D32_STEEL_DECORATIVE_VENT.get(),
                cubeAll(ModBlocks.D32_STEEL_DECORATIVE_VENT.get()));

        pipe(ModBlocks.IRIDESCENT_ALLOY_DECORATIVE_PIPE);
        simpleBlockWithItem(ModBlocks.IRIDESCENT_ALLOY_DECORATIVE_VENT.get(),
                cubeAll(ModBlocks.IRIDESCENT_ALLOY_DECORATIVE_VENT.get()));
    }

    /**
     * A {@link DecorativePipeBlock}: a centre cube, plus one arm model placed once per
     * connected side.
     *
     * <p><b>Multipart, not a variant per state.</b> Six booleans is sixty-four combinations,
     * and a variant map would need all sixty-four written out. Multipart instead states each
     * part's condition independently and lets the game assemble whichever apply, so the whole
     * block is seven parts and adding a seventh connection would be one more.
     *
     * <p>One arm model, rotated. It is authored pointing north, so {@code rotationY} covers the
     * other three horizontals and {@code rotationX} tips it up and down — 270 is up because a
     * positive X rotation tips north towards the floor.
     *
     * <p>Core and arm share one 4..12 cross-section, so a straight run is a single unbroken
     * rectangular box rather than a string of hubs and stubs, and the model agrees exactly with
     * the {@code VoxelShape} an apothem of {@code 0.25F} produces.
     */
    private void pipe(RegistryObject<Block> block) {
        String name = block.getId().getPath();
        ResourceLocation texture = modLoc("block/" + name);

        // 4..12 on every axis, which is exactly the VoxelShape an apothem of 0.25 produces —
        // the hub you see and the hub you bump into are the same box.
        ModelFile core = models().withExistingParent(name + "_core", mcLoc("block/block"))
                .texture("particle", texture).texture("pipe", texture)
                .element().from(4, 4, 4).to(12, 12, 12)
                .allFaces((direction, face) -> face.texture("#pipe")).end();

        // The same 4..12 cross-section as the core, butted against it rather than overlapping.
        // That is what makes a run of pipe read as one unbroken rectangular box: the arm's
        // sides are in the same planes as the core's and pick up straight where they stop, so
        // there is no step and no collar at a joint. Default UVs come from the element's own
        // coordinates, so the texture carries across the seam as well.
        //
        // No south face. It would be exactly coincident with the core's north face and the two
        // would z-fight; omitting it leaves the core's face alone in that plane, hidden behind
        // the arm where nothing can see it.
        ModelFile arm = models().withExistingParent(name + "_arm", mcLoc("block/block"))
                .texture("particle", texture).texture("pipe", texture)
                .element().from(4, 4, 0).to(12, 12, 4)
                // Culled against a solid neighbour: where a pipe enters a wall, this face is
                // inside the wall.
                .face(Direction.NORTH).texture("#pipe").cullface(Direction.NORTH).end()
                .face(Direction.EAST).texture("#pipe").cullface(null).end()
                .face(Direction.WEST).texture("#pipe").cullface(null).end()
                .face(Direction.UP).texture("#pipe").cullface(null).end()
                .face(Direction.DOWN).texture("#pipe").cullface(null).end()
                .end();

        MultiPartBlockStateBuilder builder = getMultipartBuilder(block.get());
        builder.part().modelFile(core).addModel().end();
        arm(builder, arm, PipeBlock.NORTH, 0, 0);
        arm(builder, arm, PipeBlock.EAST, 0, 90);
        arm(builder, arm, PipeBlock.SOUTH, 0, 180);
        arm(builder, arm, PipeBlock.WEST, 0, 270);
        arm(builder, arm, PipeBlock.UP, 270, 0);
        arm(builder, arm, PipeBlock.DOWN, 90, 0);

        // Held and dropped as a straight north–south length — one box, since that is what a
        // connected run now looks like. The bare core on its own reads as a small cube rather
        // than as a pipe.
        simpleBlockItem(block.get(), models().withExistingParent(name + "_inventory", mcLoc("block/block"))
                .texture("particle", texture).texture("pipe", texture)
                .element().from(4, 4, 0).to(12, 12, 16)
                .allFaces((direction, face) -> face.texture("#pipe")).end());
    }

    private void arm(MultiPartBlockStateBuilder builder, ModelFile arm,
                     BooleanProperty side, int rotationX, int rotationY) {
        builder.part()
                .modelFile(arm).rotationX(rotationX).rotationY(rotationY).uvLock(false).addModel()
                .condition(side, true)
                .end();
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