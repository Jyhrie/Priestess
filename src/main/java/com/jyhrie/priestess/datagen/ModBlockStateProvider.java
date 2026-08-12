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
     * Where the plant models and their textures live: what stands up out of the ground, and
     * what has fallen onto it.
     *
     * <p>The {@code block/} is spelled out and load-bearing. A model name containing a slash is
     * taken as a complete path and the provider's own {@code block} folder is <em>not</em>
     * prepended, so {@code "flowers/whiteflower"} would write to {@code models/flowers/} and
     * sit outside the tree every other block model in the mod is in.
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

        // Plants. Both tiles are mostly transparent, which is what the cutout render type in
        // each of these models is for — see flower().
        flower(ModBlocks.WHITEFLOWER, ModBlocks.POTTED_WHITEFLOWER);
        petals(ModBlocks.WHITEFLOWER_PETALS);

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
     * A small flower and its potted version, both from the one tile in {@code block/flowers/}.
     *
     * <p><b>{@code renderType} is not optional.</b> Since 1.19 a model's render layer is a
     * field on the model rather than something registered in client code, and a model that
     * names none is drawn as solid geometry — which fills every transparent pixel of the tile
     * with black, so an undeclared flower comes out as a black square with a flower inside it.
     * Cutout rather than translucent: these tiles are opaque or empty per pixel with nothing in
     * between, and cutout does not pay for sorting.
     *
     * <p><b>The item is a flat sprite, not the block model.</b> {@code simpleBlockWithItem}
     * would hand the inventory the cross model, and two quads crossing at right angles read as
     * a smear when a slot shows them head-on. Vanilla points flower items at
     * {@code item/generated} with the block tile as {@code layer0}, which is what this does —
     * so the flower needs no item texture of its own.
     */
    private void flower(RegistryObject<Block> flower, RegistryObject<Block> potted) {
        String name = flower.getId().getPath();
        ResourceLocation texture = modLoc(FLOWERS + name);

        simpleBlock(flower.get(), models().cross(FLOWERS + name, texture).renderType(CUTOUT));

        // The pot is vanilla's model; all it wants is the plant to put in it. Its own name has
        // to stay potted_<flower> to match the block, but the model can live with the flower.
        simpleBlock(potted.get(), models().singleTexture(FLOWERS + potted.getId().getPath(),
                mcLoc("block/flower_pot_cross"), "plant", texture).renderType(CUTOUT));

        itemModels().withExistingParent(name, mcLoc("item/generated")).texture("layer0", texture);
    }

    /**
     * A {@link PinkPetalsBlock}: ground cover that holds one to four petals, laid down facing
     * the way the player was standing.
     *
     * <p>Four models and sixteen multipart cases, which is vanilla's own arrangement for pink
     * petals rather than a choice. Each of vanilla's {@code block/flowerbed_N} parents draws
     * <em>only</em> the Nth petal, in its own 8x8 quadrant of the tile, so the models stack:
     * a block holding three petals applies layers 1, 2 <em>and</em> 3. That is why each part's
     * condition is "amount is N or more" rather than "amount is N" — a condition per exact
     * amount would show the third petal and nothing else.
     *
     * <p>Times four horizontal facings, because the whole patch turns with the player who laid
     * it. The parent models are authored facing north and {@code Direction.toYRot} measures
     * from south, hence the 180 — north has to come out as no rotation at all.
     *
     * <p>The stem faces in those parents carry {@code tintindex 1}, which vanilla uses to
     * grass-tint pink petal stems. Nothing here registers a colour provider, so the tint
     * resolves to white and the stem renders in its own colours — which is the intent: a
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
        // directly above, which says nothing at slot size. See PETAL_HANDFUL in
        // tools/generate_placeholder_art.py.
        itemModels().withExistingParent(name, mcLoc("item/generated"))
                .texture("layer0", modLoc("item/" + name));
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
     * A boss altar. What you see in the world is a GeckoLib model drawn by
     * {@code BossSummonerRenderer}, so the blockstate models here are <em>not</em> the altar —
     * the block is {@link net.minecraft.world.level.block.RenderShape#INVISIBLE} and none of
     * this is rendered as geometry.
     *
     * <p>They still have to exist, for two things a block entity renderer does not provide:
     *
     * <ul>
     *   <li><b>Particles.</b> Break and landing particles take their texture from the model the
     *       blockstate points at. Without one they come out as missing-texture chequerboard.
     *       Hence a model with a {@code particle} texture and no elements — no faces to draw,
     *       one texture to sample.</li>
     *   <li><b>The item.</b> An item model is a separate thing from a block model and is not
     *       affected by the render shape, so this stays an ordinary cube. It has to: an altar
     *       that was invisible in the inventory would be unplaceable in practice.</li>
     * </ul>
     *
     * <p>Two particle models rather than one, keyed on {@link BossSummonerBlock#ARMED}, so a
     * spent altar breaks in its own darker colours. That is also what keeps the blockstate
     * honest about having two states, which the renderer reads to pick its texture.
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

        // The armed tile, as a plain cube. This is the only place the 16x16 altar textures are
        // still drawn as geometry.
        simpleBlockItem(block.get(), models().cubeAll(name + "_inventory", modLoc("block/" + name)));
    }

    /** A model with a particle texture and nothing else — see {@link #summoner}. */
    private ModelFile particlesOnly(String name, ResourceLocation texture) {
        return models().getBuilder(name).texture("particle", texture);
    }
}