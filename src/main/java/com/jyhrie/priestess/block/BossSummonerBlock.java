package com.jyhrie.priestess.block;

import com.jyhrie.priestess.block.entity.BossSummonerBlockEntity;
import com.jyhrie.priestess.block.entity.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;

/**
 * A one-boss summoning altar: right-click it holding the right item and the boss stands up
 * out of it, and the altar stays spent until that boss is dead.
 *
 * <p>Concrete blocks are one subclass per boss — {@link JesseltonsEffigyBlock},
 * {@link DorothysTerminalBlock} — and each supplies four things: which boss, which item, and
 * the particle and sound it comes up with. Everything else, which is all of the fiddly
 * parts, lives here.
 *
 * <h2>Why subclasses rather than one parameterised block</h2>
 * The two bosses do not spawn the same way. Jesselton is 0.7 x 2.2 and walks; the Failed
 * Vision is 3 x 3 x 3 and is rooted where it lands, so it needs a great deal more clear
 * space and it needs that space checked before the item is taken off the player rather than
 * after. {@link #clearanceFor} is the hook, and having a class per boss means the answer to
 * "how much room does this one need" sits next to the boss it is about instead of in a
 * switch.
 *
 * <h2>{@link #ARMED}</h2>
 * One boolean, in the blockstate rather than only in the block entity, for two reasons: the
 * model changes with it, so the altar visibly reads as spent from across the room; and the
 * ticker is only attached while it is {@code false}, so an armed altar waiting to be used
 * costs nothing per tick.
 */
public abstract class BossSummonerBlock extends BaseEntityBlock {

    /** True when the altar is ready. False from the moment it summons until the boss dies. */
    public static final BooleanProperty ARMED = BooleanProperty.create("armed");

    /** How far up to look for somewhere the boss will fit, before giving up. */
    private static final int MAX_SPAWN_RISE = 3;

    protected BossSummonerBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(ARMED, true));
    }

    // ── What a subclass has to answer ─────────────────────────────────────────

    /** The boss this altar calls. */
    protected abstract EntityType<? extends Mob> boss();

    /** The item that has to be in hand. One is consumed per summon. */
    protected abstract Item catalyst();

    protected abstract ParticleOptions summonParticle();

    protected abstract SoundEvent summonSound();

    /**
     * The space the boss needs, as a box at a candidate spawn position. The default is the
     * entity type's own bounding box, which is right for anything that can walk out of a
     * tight spot; override for a boss that cannot.
     */
    protected AABB clearanceFor(BlockPos pos) {
        return boss().getAABB(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
    }

    // ── Interaction ───────────────────────────────────────────────────────────

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if (!state.getValue(ARMED)) {
            if (!level.isClientSide) {
                player.displayClientMessage(
                        Component.translatable("message.priestess.summoner.spent", boss().getDescription()), true);
            }
            // CONSUME, not PASS: the altar answered, it just said no. PASS would fall
            // through to placing whatever block is in the other hand against it.
            return InteractionResult.CONSUME;
        }

        ItemStack held = player.getItemInHand(hand);
        if (!held.is(catalyst())) {
            if (!level.isClientSide) {
                player.displayClientMessage(
                        Component.translatable("message.priestess.summoner.wrong_item",
                                catalyst().getDescription()), true);
            }
            return InteractionResult.CONSUME;
        }

        if (level.isClientSide) {
            // The client cannot know whether there is room, so it optimistically swings and
            // lets the server correct it. Every vanilla block that can fail does this.
            return InteractionResult.SUCCESS;
        }

        ServerLevel serverLevel = (ServerLevel) level;
        BlockPos spawnPos = findRoom(serverLevel, pos);
        if (spawnPos == null) {
            player.displayClientMessage(Component.translatable("message.priestess.summoner.no_room"), true);
            return InteractionResult.CONSUME;
        }

        Mob mob = summon(serverLevel, spawnPos, player);
        if (mob == null) {
            return InteractionResult.CONSUME;
        }

        // Only now is the item spent. Everything that can refuse has already refused, so
        // there is no path that eats the catalyst and produces nothing.
        if (!player.getAbilities().instabuild) {
            held.shrink(1);
        }

        level.setBlock(pos, state.setValue(ARMED, false), Block.UPDATE_ALL);
        if (level.getBlockEntity(pos) instanceof BossSummonerBlockEntity summoner) {
            summoner.watch(mob.getUUID());
        }
        return InteractionResult.CONSUME;
    }

    /**
     * The first position at or above the altar where the boss actually fits, or null.
     *
     * <p>Checked before anything is consumed. A boss summoned into a one-block cellar either
     * suffocates or is shoved through the wall, and both of those cost the player a catalyst
     * for a fight they did not get.
     */
    private BlockPos findRoom(ServerLevel level, BlockPos altar) {
        for (int rise = 1; rise <= MAX_SPAWN_RISE; rise++) {
            BlockPos candidate = altar.above(rise);
            if (level.noCollision(clearanceFor(candidate))) {
                return candidate;
            }
        }
        return null;
    }

    private Mob summon(ServerLevel level, BlockPos pos, Player player) {
        Mob mob = boss().create(level);
        if (mob == null) {
            return null;
        }
        // Facing whoever called it, which is the only orientation that reads as being
        // summoned rather than as having been standing there all along.
        mob.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5,
                Mth.wrapDegrees(player.getYRot() + 180.0F), 0.0F);
        mob.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.TRIGGERED, null, null);
        level.addFreshEntity(mob);

        level.sendParticles(summonParticle(), pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
                60, 0.8, 1.0, 0.8, 0.05);
        level.playSound(null, pos, summonSound(), SoundSource.HOSTILE, 2.0F, 1.0F);
        return mob;
    }

    // ── Block entity plumbing ─────────────────────────────────────────────────

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BossSummonerBlockEntity(pos, state);
    }

    /**
     * Only ticks while spent, and only on the server. An armed altar has nothing to watch,
     * and there may be a great many of them sitting in a creative build.
     */
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                  BlockEntityType<T> type) {
        if (level.isClientSide || state.getValue(ARMED)) {
            return null;
        }
        return createTickerHelper(type, ModBlockEntities.BOSS_SUMMONER.get(),
                BossSummonerBlockEntity::serverTick);
    }

    /** {@link BaseEntityBlock} renders nothing by default; this is an ordinary cube. */
    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ARMED);
    }

    /** Lit while armed, dark while spent — readable across a dark cell block. */
    public static int glow(BlockState state) {
        return state.getValue(ARMED) ? 8 : 0;
    }

    @Override
    public boolean isPathfindable(BlockState state, BlockGetter level, BlockPos pos,
                                  net.minecraft.world.level.pathfinder.PathComputationType type) {
        return false;
    }
}
