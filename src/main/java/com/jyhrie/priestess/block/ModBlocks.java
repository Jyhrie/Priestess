package com.jyhrie.priestess.block;

import com.jyhrie.priestess.Priestess;
import com.jyhrie.priestess.item.ModItems;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SandBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.PushReaction;
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

    // ── Rhine Lab: the Arts Lab build set ─────────────────────────────────────
    // The five blocks the Arts Lab is made of, and the first set to be gated by block type
    // rather than by position: every one of them is in the priestess:sealed_by/dorothys_vision
    // tag, so none of them can be broken by a player who has not finished Dorothy's Vision.
    // See DungeonLockdown and ModBlockTagsProvider.
    //
    // DEEPSLATE_TILES is the base: pickaxe-only and hard enough that digging one out is a
    // decision, without being so slow that a cleared lab is tedious to renovate.
    //
    // Everything after that base exists because a gate is only as strong as the cheapest way
    // around it, and mining is not the only way a block leaves the world. The lockdown refuses
    // the pickaxe; these refuse the three things that would otherwise move a wall nobody is
    // allowed to mine — see the class comment on DungeonLockdown for the pickaxe half.

    /**
     * Shared by all five, so the gate cannot be undercut by one of them being softer.
     *
     * <p>The three immunities, and what each closes:
     * <ul>
     *   <li><b>Explosions</b> — bedrock's resistance rather than obsidian's. TNT is trivially
     *       available long before Dorothy's Vision is, and a creeper wandering into the lab
     *       should not be able to open it by accident. At this value no explosion opens it,
     *       vanilla or otherwise, rather than merely no explosion vanilla can produce.</li>
     *   <li><b>Pistons</b> — {@link PushReaction#BLOCK}, so a wall cannot be shoved aside
     *       instead of broken. This also stops a piston head extending into one, which is what
     *       makes it a wall rather than a block that happens not to move.</li>
     *   <li><b>The wither</b> — via {@link net.minecraft.tags.BlockTags#WITHER_IMMUNE} in
     *       {@code ModBlockTagsProvider}, because that is where vanilla looks. A wither eats
     *       through anything below bedrock and would otherwise be a portable dungeon key.</li>
     * </ul>
     *
     * <p>All three are unconditional — they do not lift when the dungeon is cleared, unlike
     * the mining gate. None of the three has a player to ask: an explosion, a piston and a
     * wither skull all arrive without one, and per-player progress has no answer for "may
     * <em>this TNT</em> break it". Unconditional is also the more honest reading of what these
     * blocks are: lab plating that a piston was never going to move.
     */
    private static BlockBehaviour.Properties artsLab() {
        return BlockBehaviour.Properties.copy(Blocks.DEEPSLATE_TILES)
                .explosionResistance(3600000.0F)
                .pushReaction(PushReaction.BLOCK);
    }

    public static final RegistryObject<Block> RHINE_LAB_ARTS_LAB_CHISELED_WALL =
            registerBlock("rhine_lab_arts_lab_chiseled_wall", () -> new Block(artsLab()));

    public static final RegistryObject<Block> RHINE_LAB_ARTS_LAB_PLATED_WALL =
            registerBlock("rhine_lab_arts_lab_plated_wall", () -> new Block(artsLab()));

    public static final RegistryObject<Block> RHINE_LAB_ARTS_LAB_CONCRETE_WALL =
            registerBlock("rhine_lab_arts_lab_concrete_wall", () -> new Block(artsLab()));

    public static final RegistryObject<Block> RHINE_LAB_ARTS_LAB_TILE =
            registerBlock("rhine_lab_arts_lab_tile", () -> new Block(artsLab()));

    // A RotatedPillarBlock rather than a plain one: a pillar that cannot be laid on its side
    // is a pillar you have to build the room around, and the axis state costs nothing.
    public static final RegistryObject<Block> RHINE_LAB_ARTS_LAB_PILLAR =
            registerBlock("rhine_lab_arts_lab_pillar", () -> new RotatedPillarBlock(artsLab()));

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