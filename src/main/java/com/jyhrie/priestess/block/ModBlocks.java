package com.jyhrie.priestess.block;

import com.jyhrie.priestess.Priestess;
import com.jyhrie.priestess.item.ModItems;
import com.jyhrie.priestess.progression.Dungeon;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SandBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Supplier;

public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, Priestess.MOD_ID);

    // Registered as a SandBlock so it falls down automatically!
    public static final RegistryObject<Block> IBERIAN_SAND = registerBlock("iberian_sand",
            () -> new SandBlock(0xE6C280, BlockBehaviour.Properties.copy(Blocks.SAND)));

    public static final RegistryObject<Block> IBERIAN_SANDSTONE = registerBlock("iberian_sandstone",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.SANDSTONE)));

    // Siesta is volcanic, so its sand is ash rather than beach gold — the dust colour is
    // what you see kicked up when you walk on it and when it falls.
    public static final RegistryObject<Block> SIESTA_SAND = registerBlock("siesta_sand",
            () -> new SandBlock(0x6B5B4E, BlockBehaviour.Properties.copy(Blocks.SAND)));

    // Copies PACKED_ICE, not ICE: packed ice keeps the friction but does not melt near a
    // light source, which is what you want for terrain that has to survive worldgen.
    public static final RegistryObject<Block> BLACK_ICE = registerBlock("black_ice",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.PACKED_ICE)));

    // Dossoles' side of the water, against Siesta's volcanic ash. Near-white dust colour
    // so the two beaches read as different places from across a bay.
    public static final RegistryObject<Block> PALE_BEACH_SAND = registerBlock("pale_beach_sand",
            () -> new SandBlock(0xEDE3CB, BlockBehaviour.Properties.copy(Blocks.SAND)));

    // The Sea of Silence floor. CALCITE rather than a sediment: what Aegir left behind is
    // a bleached crust of dead coral, so it should be brittle and chalky, not soft.
    public static final RegistryObject<Block> DEAD_SEABED = registerBlock("dead_seabed",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.CALCITE)));

    // Frozen ground, not ice. PACKED_MUD is the right analogue: still a shovel block, but
    // meaningfully harder than dirt.
    public static final RegistryObject<Block> PERMAFROST = registerBlock("permafrost",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.PACKED_MUD)));

    // ── Boss summoners ────────────────────────────────────────────────────────
    // One altar per boss. Right-click with the matching catalyst and the boss stands up out
    // of it; it stays spent until that boss is dead. See BossSummonerBlock.
    //
    // Copied from LODESTONE: stone-like, needs a pickaxe, and blast-resistant enough that a
    // creeper cannot delete a fight you were about to have. They light while armed, which is
    // the only way to see the state from more than a few blocks away.

    public static final RegistryObject<Block> JESSELTON_PROJECTOR = registerBlock("jesselton_projector",
            () -> new JesseltonProjectorBlock(BlockBehaviour.Properties.copy(Blocks.LODESTONE)
                    .lightLevel(BossSummonerBlock::glow)));

    public static final RegistryObject<Block> DOROTHYS_TERMINAL = registerBlock("dorothys_terminal",
            () -> new DorothysTerminalBlock(BlockBehaviour.Properties.copy(Blocks.LODESTONE)
                    .lightLevel(BossSummonerBlock::glow)));

    // ── Dungeon-gated build sets ──────────────────────────────────────────────
    // Blocks that cannot be mined until their dungeon is cleared. Everything about the gate
    // comes from SealedBlock/SealedPillarBlock and the Dungeon passed to them — the tag, the
    // blast and piston immunity, and the wither tag are all derived from that one argument.
    // The properties below describe only the material. See docs/DUNGEON_BLOCKS.md.

    /**
     * Rhine Lab's Arts Lab wing, gated behind <b>Dorothy's Vision</b> — chapter order decides
     * that, not the building the blocks are named after. A dungeon gating its own build set
     * would be a locked door with the key behind it.
     *
     * <p>DEEPSLATE_TILES is the base: pickaxe-only and hard enough that digging one out is a
     * decision, without being so slow that a cleared lab is tedious to renovate. Shared by all
     * five, so the gate cannot be undercut by one of them being softer.
     */
    private static BlockBehaviour.Properties artsLab() {
        return BlockBehaviour.Properties.copy(Blocks.DEEPSLATE_TILES);
    }

    public static final RegistryObject<Block> RHINE_LAB_ARTS_LAB_CHISELED_WALL =
            registerBlock("rhine_lab_arts_lab_chiseled_wall",
                    () -> new SealedBlock(Dungeon.DOROTHYS_VISION, artsLab()));

    public static final RegistryObject<Block> RHINE_LAB_ARTS_LAB_PLATED_WALL =
            registerBlock("rhine_lab_arts_lab_plated_wall",
                    () -> new SealedBlock(Dungeon.DOROTHYS_VISION, artsLab()));

    public static final RegistryObject<Block> RHINE_LAB_ARTS_LAB_CONCRETE_WALL =
            registerBlock("rhine_lab_arts_lab_concrete_wall",
                    () -> new SealedBlock(Dungeon.DOROTHYS_VISION, artsLab()));

    public static final RegistryObject<Block> RHINE_LAB_ARTS_LAB_TILE =
            registerBlock("rhine_lab_arts_lab_tile",
                    () -> new SealedBlock(Dungeon.DOROTHYS_VISION, artsLab()));

    // A pillar rather than a plain block: one that cannot be laid on its side is a pillar you
    // have to build the room around, and the axis state costs nothing.
    public static final RegistryObject<Block> RHINE_LAB_ARTS_LAB_PILLAR =
            registerBlock("rhine_lab_arts_lab_pillar",
                    () -> new SealedPillarBlock(Dungeon.DOROTHYS_VISION, artsLab()));

    /**
     * The Sal Viento catacombs, gated behind <b>Under Tides</b> — the dungeon Bishop Quintus
     * ends, which is the only thing down there that clears anything.
     *
     * <p>DEEPSLATE_BRICKS rather than the Arts Lab's tiles: same tier, so neither build set is
     * the cheap way into the other, but a masonry sound and a coarser look. Both catacombs
     * blocks share it — the overgrown one is the same stone with something living on it, not
     * a softer stone.
     */
    private static BlockBehaviour.Properties catacombs() {
        return BlockBehaviour.Properties.copy(Blocks.DEEPSLATE_BRICKS);
    }

    public static final RegistryObject<Block> SAL_VIENTO_CATACOMBS_STONE =
            registerBlock("sal_viento_catacombs_stone",
                    () -> new SealedBlock(Dungeon.UNDER_TIDES, catacombs()));

    public static final RegistryObject<Block> SAL_VIENTO_CATACOMBS_OVERGROWN_STONE =
            registerBlock("sal_viento_catacombs_overgrown_stone",
                    () -> new SealedBlock(Dungeon.UNDER_TIDES, catacombs()));

    // ── Decoration ────────────────────────────────────────────────────────────

    /**
     * Catacomb plumbing. Deliberately <b>not</b> a {@link SealedBlock}: the lockdown gates the
     * walls that are the gate, and dressing a dungeon with pipes a player cannot take down
     * would put fittings behind a boss for no reason.
     *
     * <p>{@code noOcclusion} is not optional on a block thinner than its cube — without it the
     * game culls the faces of whatever the pipe touches and leaves holes in the wall behind it.
     * COPPER for the sound: it is the one vanilla metal block that does not read as machinery.
     */
    public static final RegistryObject<Block> SAL_VIENTO_CATACOMBS_PIPE =
            registerBlock("sal_viento_catacombs_pipe",
                    () -> new DecorativePipeBlock(0.25F,
                            BlockBehaviour.Properties.copy(Blocks.COPPER_BLOCK).noOcclusion()));

    private static <T extends Block> RegistryObject<T> registerBlock(String name, Supplier<T> block) {
        RegistryObject<T> toReturn = BLOCKS.register(name, block);
        registerBlockItem(name, toReturn);
        return toReturn;
    }

    private static <T extends Block> void registerBlockItem(String name, RegistryObject<T> block) {
        ModItems.ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
    }

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}