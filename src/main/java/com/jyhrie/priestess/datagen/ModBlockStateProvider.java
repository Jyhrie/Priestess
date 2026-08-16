package com.jyhrie.priestess.datagen;

import com.jyhrie.priestess.Priestess;
import com.jyhrie.priestess.block.BossSummonerBlock;
import com.jyhrie.priestess.block.DecorativePipeBlock;
import com.jyhrie.priestess.block.ModBlocks;
import net.minecraft.core.Direction;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.PinkPetalsBlock;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraftforge.client.model.generators.BlockStateProvider;
import net.minecraftforge.client.model.generators.ModelFile;
import net.minecraftforge.client.model.generators.MultiPartBlockStateBuilder;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.registries.RegistryObject;

import java.util.stream.IntStream;

public class ModBlockStateProvider extends BlockStateProvider {

    /** The render layer every partly transparent plant model needs. See {@link #flower}. */
    private static final String CUTOUT = "minecraft:cutout";

    /**
     * The {@code block/} prefix is load-bearing: a model name containing a slash is taken as a
     * complete path and the provider's own {@code block} folder is <em>not</em> prepended, so
     * {@code "flowers/whiteflower"} would write to {@code models/flowers/}.
     */
    private static final String FLOWERS = "block/flowers/";
    private static final String LITTER = "block/litter/";

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

        flower(ModBlocks.WHITEFLOWER, ModBlocks.POTTED_WHITEFLOWER);
        petals(ModBlocks.WHITEFLOWER_PETALS);

        summoner(ModBlocks.JESSELTON_PROJECTOR);
        summoner(ModBlocks.DOROTHYS_TERMINAL);

        simpleBlockWithItem(ModBlocks.RHINE_LAB_ARTS_LAB_CHISELED_WALL.get(),
                cubeAll(ModBlocks.RHINE_LAB_ARTS_LAB_CHISELED_WALL.get()));
        simpleBlockWithItem(ModBlocks.RHINE_LAB_ARTS_LAB_PLATED_WALL.get(),
                cubeAll(ModBlocks.RHINE_LAB_ARTS_LAB_PLATED_WALL.get()));
        simpleBlockWithItem(ModBlocks.RHINE_LAB_ARTS_LAB_CONCRETE_WALL.get(),
                cubeAll(ModBlocks.RHINE_LAB_ARTS_LAB_CONCRETE_WALL.get()));
        simpleBlockWithItem(ModBlocks.RHINE_LAB_ARTS_LAB_TILE.get(),
                cubeAll(ModBlocks.RHINE_LAB_ARTS_LAB_TILE.get()));
        pillar(ModBlocks.RHINE_LAB_ARTS_LAB_PILLAR);

        simpleBlockWithItem(ModBlocks.SAL_VIENTO_CATACOMBS_STONE.get(),
                cubeAll(ModBlocks.SAL_VIENTO_CATACOMBS_STONE.get()));
        simpleBlockWithItem(ModBlocks.SAL_VIENTO_CATACOMBS_OVERGROWN_STONE.get(),
                cubeAll(ModBlocks.SAL_VIENTO_CATACOMBS_OVERGROWN_STONE.get()));

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
     * A small flower and its potted version, both from the one tile in {@code block/flowers/}.
     *
     * <p><b>{@code renderType} is not optional.</b> Since 1.19 the render layer is a field on
     * the model, and a model that names none is drawn as solid geometry — so an undeclared
     * flower comes out as a black square with a flower inside it. Cutout rather than
     * translucent, since these tiles are opaque or empty per pixel and cutout skips sorting.
     *
     * <p><b>The item is a flat sprite, not the block model</b>, because two quads crossing at
     * right angles read as a smear in a slot. It uses the block tile as {@code layer0}, so the
     * flower needs no item texture of its own.
     */
    private void flower(RegistryObject<Block> flower, RegistryObject<Block> potted) {
        String name = flower.getId().getPath();
        ResourceLocation texture = modLoc(FLOWERS + name);

        simpleBlock(flower.get(), models().cross(FLOWERS + name, texture).renderType(CUTOUT));

        // Vanilla's model; all it wants is the plant to put in it.
        simpleBlock(potted.get(), models().singleTexture(FLOWERS + potted.getId().getPath(),
                mcLoc("block/flower_pot_cross"), "plant", texture).renderType(CUTOUT));

        itemModels().withExistingParent(name, mcLoc("item/generated")).texture("layer0", texture);
    }

    /**
     * A {@link PinkPetalsBlock}: ground cover that holds one to four petals, laid down facing
     * the way the player was standing.
     *
     * <p>Four models and sixteen multipart cases, which is vanilla's own arrangement. Each
     * {@code block/flowerbed_N} parent draws <em>only</em> the Nth petal, so the models stack
     * and each part's condition has to be "amount is N or more" — a condition per exact amount
     * would show the third petal and nothing else.
     *
     * <p>Times four facings. The parents are authored facing north while
     * {@code Direction.toYRot} measures from south, hence the 180.
     *
     * <p>The stem faces carry {@code tintindex 1}, which vanilla grass-tints. Nothing here
     * registers a colour provider, so the tint resolves to white — intentionally, since a
     * whiteflower stem should not change hue with the biome it fell in.
     */
    private void petals(RegistryObject<Block> block) {
        String name = block.getId().getPath();
        ResourceLocation petals = modLoc(LITTER + name);
        ResourceLocation stem = modLoc(LITTER + name + "_stem");

        MultiPartBlockStateBuilder builder = getMultipartBuilder(block.get());
        for (int layer = PinkPetalsBlock.MIN_FLOWERS; layer <= PinkPetalsBlock.MAX_FLOWERS; layer++) {
            ModelFile model = models()
                    .withExistingParent(LITTER + name + "_" + layer, mcLoc("block/flowerbed_" + layer))
                    .texture("flowerbed", petals)
                    .texture("stem", stem)
                    .renderType(CUTOUT);

            Integer[] thisManyOrMore = IntStream.rangeClosed(layer, PinkPetalsBlock.MAX_FLOWERS)
                    .boxed().toArray(Integer[]::new);

            for (Direction facing : Direction.Plane.HORIZONTAL) {
                builder.part()
                        .modelFile(model)
                        .rotationY(((int) facing.toYRot() + 180) % 360)
                        .addModel()
                        .condition(PinkPetalsBlock.FACING, facing)
                        .condition(PinkPetalsBlock.AMOUNT, thisManyOrMore)
                        .end();
            }
        }

        // Its own sprite, unlike the flower's: the block tile is a scatter of petals seen from
        // directly above, which says nothing at slot size.
        itemModels().withExistingParent(name, mcLoc("item/generated"))
                .texture("layer0", modLoc("item/" + name));
    }

    /**
     * A {@link DecorativePipeBlock}: a centre cube, plus one arm model placed once per
     * connected side.
     *
     * <p><b>Multipart, not a variant per state</b>: six booleans is sixty-four combinations, and
     * a variant map would need all sixty-four written out.
     *
     * <p>One arm model, authored pointing north, rotated into place — 270 is up because a
     * positive X rotation tips north towards the floor.
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

        // The same cross-section as the core, butted against it rather than overlapping, so a
        // run reads as one unbroken box with no step or collar at a joint.
        //
        // No south face: it would be exactly coincident with the core's north face and the two
        // would z-fight.
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

        // Held as a straight length: the bare core alone reads as a small cube, not a pipe.
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
     * A boss altar. What you see in the world is a GeckoLib model drawn by
     * {@code BossSummonerRenderer}, so the blockstate models here are <em>not</em> the altar —
     * the block is {@link net.minecraft.world.level.block.RenderShape#INVISIBLE} and none of
     * this is rendered as geometry.
     *
     * <p>They still have to exist for two things a block entity renderer does not provide.
     * Break and landing <b>particles</b> take their texture from the model the blockstate
     * points at, hence a model with a {@code particle} texture and no elements. And the
     * <b>item</b> model is a separate thing unaffected by the render shape, so it stays an
     * ordinary cube — an altar invisible in the inventory would be unplaceable.
     *
     * <p>Two particle models keyed on {@link BossSummonerBlock#ARMED}, so a spent altar breaks
     * in its own darker colours and the blockstate stays honest about having two states.
     */
    private void summoner(RegistryObject<Block> block) {
        String name = block.getId().getPath();

        ModelFile armedParticles = particlesOnly(name + "_particles", modLoc("block/" + name));
        ModelFile spentParticles = particlesOnly(name + "_spent_particles",
                modLoc("block/" + name + "_spent"));

        getVariantBuilder(block.get())
                .partialState().with(BossSummonerBlock.ARMED, true)
                .modelForState().modelFile(armedParticles).addModel()
                .partialState().with(BossSummonerBlock.ARMED, false)
                .modelForState().modelFile(spentParticles).addModel();

        simpleBlockItem(block.get(), models().cubeAll(name + "_inventory", modLoc("block/" + name)));
    }

    /** A model with a particle texture and nothing else — see {@link #summoner}. */
    private ModelFile particlesOnly(String name, ResourceLocation texture) {
        return models().getBuilder(name).texture("particle", texture);
    }
}