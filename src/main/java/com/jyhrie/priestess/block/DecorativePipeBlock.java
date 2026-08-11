package com.jyhrie.priestess.block;

import com.jyhrie.priestess.Priestess;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;

/**
 * A pipe that is only a pipe to look at: it has no inventory, moves nothing, and does nothing
 * but join up with its neighbours and be thinner than a block.
 *
 * <p>Everything about the shape comes from vanilla's {@link PipeBlock}, which already owns the
 * six {@code north/east/south/west/up/down} booleans, the {@code apothem} that decides how
 * thick the pipe is, and a {@code getShape} that assembles the right {@code VoxelShape} for any
 * combination of the six. What it deliberately leaves to subclasses is <em>when</em> those
 * booleans are true, which is the only interesting part and is all this class adds.
 *
 * <h2>What it connects to</h2>
 * Anything in {@link #PIPES}, so every pipe in the mod joins every other pipe and a datapack
 * can add its own to the tag without touching code; and any <b>sturdy face</b>, so a pipe run
 * into a wall ends in a collar against it rather than stopping in mid-air. Drop the
 * {@code isFaceSturdy} half of {@link #connectsTo} if you would rather pipes ignored walls.
 *
 * <p>Connections are recomputed in {@link #updateShape}, which the game calls on the block next
 * to any change, so a pipe re-joins and re-parts itself as things are built and broken around
 * it. Nothing has to tick and nothing is stored beyond the six booleans already in the
 * blockstate.
 *
 * <h2>Three things that are easy to leave out</h2>
 * <ul>
 *   <li><b>{@code noOcclusion()}</b> in the block's properties — see {@code ModBlocks}. Without
 *       it the game treats the pipe as a solid cube for culling and neighbouring block faces
 *       vanish where they touch it.</li>
 *   <li><b>Waterlogging.</b> A block smaller than its cube that cannot hold water deletes the
 *       water it is placed in, which every player reads as a bug.</li>
 *   <li><b>{@link #rotate} and {@link #mirror}.</b> Terra's dungeons are jigsaw structures and
 *       are placed rotated; without these, a rotated pipe keeps the connections it was saved
 *       with and points the wrong way until something updates next to it.</li>
 * </ul>
 */
public class DecorativePipeBlock extends PipeBlock implements SimpleWaterloggedBlock {

    /**
     * Every block that counts as a pipe for the purpose of joining up — one tag across all
     * materials, so an RMA70 pipe meets a D32 Steel one without either knowing the other
     * exists. Populated in {@code ModBlockTagsProvider}; a pipe left out of it will never
     * connect to anything, which is the first thing to check when a run sits as loose stubs.
     */
    public static final TagKey<Block> PIPES =
            TagKey.create(Registries.BLOCK, new ResourceLocation(Priestess.MOD_ID, "pipes"));

    /**
     * Blocks that are not pipes but that a pipe will still dock into — the vents. Separate
     * from {@link #PIPES} because these are solid cubes and answering "yes, I am a pipe" for
     * them would be a lie the moment anything else asks.
     */
    public static final TagKey<Block> PIPE_ATTACHMENTS =
            TagKey.create(Registries.BLOCK, new ResourceLocation(Priestess.MOD_ID, "pipe_attachments"));

    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    /**
     * @param apothem half the pipe's thickness, in block units — {@code 0.25F} is an
     *                eight-pixel pipe. This is baked into the shapes at construction, so it is
     *                a constant per block rather than a blockstate.
     */
    public DecorativePipeBlock(float apothem, Properties properties) {
        super(apothem, properties);
        registerDefaultState(stateDefinition.any()
                .setValue(NORTH, false)
                .setValue(EAST, false)
                .setValue(SOUTH, false)
                .setValue(WEST, false)
                .setValue(UP, false)
                .setValue(DOWN, false)
                .setValue(WATERLOGGED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NORTH, EAST, SOUTH, WEST, UP, DOWN, WATERLOGGED);
    }

    // ── Connecting ────────────────────────────────────────────────────────────

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = defaultBlockState()
                .setValue(WATERLOGGED, level.getFluidState(pos).getType() == Fluids.WATER);

        for (Direction direction : Direction.values()) {
            state = state.setValue(PROPERTY_BY_DIRECTION.get(direction),
                    connectsTo(level.getBlockState(pos.relative(direction))));
        }
        return state;
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighbour,
                                  LevelAccessor level, BlockPos pos, BlockPos neighbourPos) {
        if (state.getValue(WATERLOGGED)) {
            level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
        return state.setValue(PROPERTY_BY_DIRECTION.get(direction), connectsTo(neighbour));
    }

    /**
     * Membership, not geometry. An earlier version also joined to any sturdy face, which meant
     * a pipe connected to every wall in the game and left nothing for a vent to be — the whole
     * point of an attachment is that it is the block a run is <em>supposed</em> to end at. Put
     * a block in one of the two tags to make pipes meet it.
     */
    private static boolean connectsTo(BlockState neighbour) {
        return neighbour.is(PIPES) || neighbour.is(PIPE_ATTACHMENTS);
    }

    // ── Water ─────────────────────────────────────────────────────────────────

    @Override
    public FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED)
                ? Fluids.WATER.getSource(false)
                : super.getFluidState(state);
    }

    // ── Rotation ──────────────────────────────────────────────────────────────
    // Only the four horizontal booleans move; up and down are unchanged by either operation.

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state
                .setValue(NORTH, state.getValue(PROPERTY_BY_DIRECTION.get(rotation.rotate(Direction.NORTH))))
                .setValue(EAST, state.getValue(PROPERTY_BY_DIRECTION.get(rotation.rotate(Direction.EAST))))
                .setValue(SOUTH, state.getValue(PROPERTY_BY_DIRECTION.get(rotation.rotate(Direction.SOUTH))))
                .setValue(WEST, state.getValue(PROPERTY_BY_DIRECTION.get(rotation.rotate(Direction.WEST))));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state
                .setValue(NORTH, state.getValue(PROPERTY_BY_DIRECTION.get(mirror.mirror(Direction.NORTH))))
                .setValue(EAST, state.getValue(PROPERTY_BY_DIRECTION.get(mirror.mirror(Direction.EAST))))
                .setValue(SOUTH, state.getValue(PROPERTY_BY_DIRECTION.get(mirror.mirror(Direction.SOUTH))))
                .setValue(WEST, state.getValue(PROPERTY_BY_DIRECTION.get(mirror.mirror(Direction.WEST))));
    }
}
