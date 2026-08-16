package com.jyhrie.priestess.block;

import com.jyhrie.priestess.Priestess;
import com.jyhrie.priestess.block.boss_summons.DorothysTerminalBlock;
import com.jyhrie.priestess.block.boss_summons.JesseltonProjectorBlock;
import com.jyhrie.priestess.item.ModItems;
import com.jyhrie.priestess.progression.Dungeon;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FlowerBlock;
import net.minecraft.world.level.block.FlowerPotBlock;
import net.minecraft.world.level.block.PinkPetalsBlock;
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

    // Neither plant is placed by worldgen and neither is compostable yet.

    /**
     * A small flower — everything that makes it one comes from {@link FlowerBlock} and from
     * copying POPPY, including the random XZ offset that stops a meadow of them sitting on a
     * visible grid.
     *
     * <p>The effect is passed as a supplier because Forge deprecated taking it directly: an
     * effect is a registry entry, and holding one at block-construction time is holding it
     * before the registry is necessarily filled.
     */
    public static final RegistryObject<Block> WHITEFLOWER = registerBlock("whiteflower",
            () -> new FlowerBlock(() -> MobEffects.REGENERATION, 7,
                    BlockBehaviour.Properties.copy(Blocks.POPPY)));

    /**
     * Fallen whiteflower petals. All the behaviour is {@link PinkPetalsBlock}, which hardcodes
     * nothing pink and so serves any petal.
     */
    public static final RegistryObject<Block> WHITEFLOWER_PETALS = registerBlock("whiteflower_petals",
            () -> new PinkPetalsBlock(BlockBehaviour.Properties.copy(Blocks.PINK_PETALS)));

    /**
     * Registered straight into {@code BLOCKS} rather than through {@link #registerBlock},
     * because it must <em>not</em> have a BlockItem: a potted plant is made by using the flower
     * on a pot.
     *
     * <p>Forge's three-argument constructor is the whole of "the vanilla pot accepts this
     * flower" — passing the empty pot registers the plant into that pot's content map. It
     * resolves {@code WHITEFLOWER} while it runs, which is safe only because that block is
     * declared above and so registered first.
     */
    public static final RegistryObject<Block> POTTED_WHITEFLOWER = BLOCKS.register("potted_whiteflower",
            () -> new FlowerPotBlock(() -> (FlowerPotBlock) Blocks.FLOWER_POT, WHITEFLOWER,
                    BlockBehaviour.Properties.copy(Blocks.POTTED_POPPY)));

    // LODESTONE: blast-resistant enough that a creeper cannot delete a fight you were about to
    // have. See BossSummonerBlock.
    //
    // noOcclusion() is load-bearing. These are drawn by a block entity renderer with an
    // INVISIBLE baked model, but occlusion is decided separately from rendering: left
    // occluding, a neighbour culls the face it shares with the altar, and because the altar
    // draws nothing in that plane you see straight through the wall. Chests do this too.

    public static final RegistryObject<Block> JESSELTON_PROJECTOR = registerBlock("jesselton_projector",
            () -> new JesseltonProjectorBlock(BlockBehaviour.Properties.copy(Blocks.LODESTONE)
                    .lightLevel(BossSummonerBlock::glow)
                    .noOcclusion()));

    public static final RegistryObject<Block> DOROTHYS_TERMINAL = registerBlock("dorothys_terminal",
            () -> new DorothysTerminalBlock(BlockBehaviour.Properties.copy(Blocks.LODESTONE)
                    .lightLevel(BossSummonerBlock::glow)
                    .noOcclusion()));

    // Blocks that cannot be mined until their dungeon is cleared. Everything about the gate —
    // the tag, the blast and piston immunity, the wither tag — comes from SealedBlock and the
    // Dungeon passed to it; the properties below describe only the material. See
    // docs/DUNGEON_BLOCKS.md.

    /**
     * Gated behind <b>Dorothy's Vision</b>, which chapter order decides rather than the
     * building the blocks are named after — a dungeon gating its own build set would be a
     * locked door with the key behind it.
     *
     * <p>Shared by all five, so the gate cannot be undercut by one of them being softer.
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

    public static final RegistryObject<Block> RHINE_LAB_ARTS_LAB_PILLAR =
            registerBlock("rhine_lab_arts_lab_pillar",
                    () -> new SealedPillarBlock(Dungeon.DOROTHYS_VISION, artsLab()));

    /**
     * Gated behind <b>Under Tides</b>. DEEPSLATE_BRICKS rather than the Arts Lab's tiles: the
     * same tier, so neither build set is the cheap way into the other, but a masonry sound and
     * a coarser look.
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

    // Never gated: the lockdown holds the walls that *are* the gate.
    //
    // Every pipe joins every other pipe and every vent, whatever the material — that is the
    // priestess:pipes and priestess:pipe_attachments tags doing the work, not any code here.
    // A vent needs no behaviour, because a pipe decides its own connections by looking outward.

    /** Half the pipe's thickness, so 0.25 is an eight-pixel pipe. Shared, so the sets match. */
    private static final float PIPE_APOTHEM = 0.25F;

    /**
     * {@code noOcclusion} is not optional on a block thinner than its own cube — without it the
     * game culls the faces of whatever the pipe touches and leaves holes in the wall behind it.
     */
    private static RegistryObject<Block> pipe(String name, Block material) {
        return registerBlock(name, () -> new DecorativePipeBlock(PIPE_APOTHEM,
                BlockBehaviour.Properties.copy(material).noOcclusion()));
    }

    /** A plain cube. All it needs is to be in the attachment tag, which is datagen's job. */
    private static RegistryObject<Block> vent(String name, Block material) {
        return registerBlock(name, () -> new Block(BlockBehaviour.Properties.copy(material)));
    }

    public static final RegistryObject<Block> SAL_VIENTO_CATACOMBS_PIPE =
            pipe("sal_viento_catacombs_pipe", Blocks.COPPER_BLOCK);

    public static final RegistryObject<Block> RMA70_12_DECORATIVE_PIPE =
            pipe("rma70_12_decorative_pipe", Blocks.COPPER_BLOCK);
    public static final RegistryObject<Block> RMA70_12_DECORATIVE_VENT =
            vent("rma70_12_decorative_vent", Blocks.COPPER_BLOCK);

    // Iron-grade against -12's copper, so the pair are told apart by sound and mining time as
    // well as by colour.
    public static final RegistryObject<Block> RMA70_24_DECORATIVE_PIPE =
            pipe("rma70_24_decorative_pipe", Blocks.IRON_BLOCK);
    public static final RegistryObject<Block> RMA70_24_DECORATIVE_VENT =
            vent("rma70_24_decorative_vent", Blocks.IRON_BLOCK);

    public static final RegistryObject<Block> D32_STEEL_DECORATIVE_PIPE =
            pipe("d32_steel_decorative_pipe", Blocks.IRON_BLOCK);
    public static final RegistryObject<Block> D32_STEEL_DECORATIVE_VENT =
            vent("d32_steel_decorative_vent", Blocks.IRON_BLOCK);

    // The only set carrying a light level, and a dim one: it should catch the eye in an unlit
    // corridor without lighting the room.
    public static final RegistryObject<Block> IRIDESCENT_ALLOY_DECORATIVE_PIPE =
            registerBlock("iridescent_alloy_decorative_pipe",
                    () -> new DecorativePipeBlock(PIPE_APOTHEM,
                            BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK)
                                    .noOcclusion().lightLevel(state -> 4)));
    public static final RegistryObject<Block> IRIDESCENT_ALLOY_DECORATIVE_VENT =
            registerBlock("iridescent_alloy_decorative_vent",
                    () -> new Block(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK)
                            .lightLevel(state -> 4)));

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