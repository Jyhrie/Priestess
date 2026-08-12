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

    // ── Plants ────────────────────────────────────────────────────────────────
    // The first flora in the mod. Both of these are vanilla block classes doing vanilla work:
    // there is nothing about a flower that Terra needs to do differently, and a subclass that
    // only calls super is a file to maintain for no behaviour.
    //
    // Neither is placed by worldgen and neither is compostable yet. Both are additions on top
    // of a flower that exists, is obtainable and behaves correctly — see docs/WORLDGEN.md for
    // where a vegetation feature would go.

    /**
     * A small flower. Everything that makes it one comes from {@link FlowerBlock} and from
     * copying POPPY: it breaks instantly and in one hit, it needs a block it can root in, it
     * is a suspicious stew ingredient, and it takes the random XZ offset that stops a meadow
     * of them sitting on a visible grid — {@code copy} carries that offset over with the rest
     * of the material, so it does not have to be asked for.
     *
     * <p>Regeneration is the oxeye daisy's effect, chosen because a white flower that mends
     * something reads right and because no progression hangs on it. Seven seconds is a bowl
     * of stew's worth rather than a potion's. It is passed as a supplier because Forge
     * deprecated taking the effect directly — an effect is a registry entry, and holding one
     * at block-construction time is holding it before the registry is necessarily filled.
     */
    public static final RegistryObject<Block> WHITEFLOWER = registerBlock("whiteflower",
            () -> new FlowerBlock(() -> MobEffects.REGENERATION, 7,
                    BlockBehaviour.Properties.copy(Blocks.POPPY)));

    /**
     * Fallen whiteflower petals — the ground cover, in the mould of vanilla's pink petals.
     * One to four petals per block, facing whichever way the player was, and bone meal adds
     * one more; all of that is {@link PinkPetalsBlock}, which hardcodes nothing pink and so
     * serves any petal.
     *
     * <p>The layer of grass and stone underneath is what the litter is dressing, so it copies
     * PINK_PETALS: no collision, instant break, and the plant sound.
     */
    public static final RegistryObject<Block> WHITEFLOWER_PETALS = registerBlock("whiteflower_petals",
            () -> new PinkPetalsBlock(BlockBehaviour.Properties.copy(Blocks.PINK_PETALS)));

    /**
     * The potted flower. Registered straight into {@code BLOCKS} rather than through
     * {@link #registerBlock} because it must <em>not</em> have a BlockItem: a potted plant is
     * made by using the flower on a pot, and an item for it would be a second way to get one
     * that vanilla does not offer and that no recipe or loot table would explain.
     *
     * <p>Forge's three-argument constructor is the whole of "the vanilla pot accepts this
     * flower" — passing the empty pot registers the plant into that pot's content map, so
     * there is nothing to add on the vanilla side. It resolves {@code WHITEFLOWER} while it
     * runs, which is safe only because that block is declared above this one and so is
     * registered first.
     */
    public static final RegistryObject<Block> POTTED_WHITEFLOWER = BLOCKS.register("potted_whiteflower",
            () -> new FlowerPotBlock(() -> (FlowerPotBlock) Blocks.FLOWER_POT, WHITEFLOWER,
                    BlockBehaviour.Properties.copy(Blocks.POTTED_POPPY)));

    // ── Boss summoners ────────────────────────────────────────────────────────
    // One altar per boss. Right-click with the matching catalyst and the boss stands up out
    // of it; it stays spent until that boss is dead. See BossSummonerBlock.
    //
    // Copied from LODESTONE: stone-like, needs a pickaxe, and blast-resistant enough that a
    // creeper cannot delete a fight you were about to have. They light while armed, which is
    // the only way to see the state from more than a few blocks away.

    // noOcclusion() is load-bearing, not tidiness. These are drawn by a block entity renderer
    // and their baked model is RenderShape.INVISIBLE, but occlusion is decided separately from
    // rendering: left occluding, a neighbouring block culls the face it shares with the altar,
    // and because the altar draws nothing in that plane you see straight through the wall. Every
    // BER-rendered vanilla block — chests included — does this for the same reason.

    public static final RegistryObject<Block> JESSELTON_PROJECTOR = registerBlock("jesselton_projector",
            () -> new JesseltonProjectorBlock(BlockBehaviour.Properties.copy(Blocks.LODESTONE)
                    .lightLevel(BossSummonerBlock::glow)
                    .noOcclusion()));

    public static final RegistryObject<Block> DOROTHYS_TERMINAL = registerBlock("dorothys_terminal",
            () -> new DorothysTerminalBlock(BlockBehaviour.Properties.copy(Blocks.LODESTONE)
                    .lightLevel(BossSummonerBlock::glow)
                    .noOcclusion()));

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

    // ── Decoration: pipes and vents ───────────────────────────────────────────
    // Never gated. The lockdown holds the walls that *are* the gate; dressing a dungeon with
    // fittings a player cannot take down would put plumbing behind a boss for no reason.
    //
    // Every pipe joins every other pipe and every vent, whatever the material — that is one
    // priestess:pipes tag and one priestess:pipe_attachments tag doing the work, not any code
    // here. A vent is an ordinary full cube; it needs no behaviour, because a pipe decides its
    // own connections by looking outward and a vent never looks back.

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

    // Sal Viento's catacombs: the plumbing that runs through the Under Tides masonry.
    public static final RegistryObject<Block> SAL_VIENTO_CATACOMBS_PIPE =
            pipe("sal_viento_catacombs_pipe", Blocks.COPPER_BLOCK);

    // RMA70-12 — the lighter reagent line. Copper-grade, so it reads as the softer of the two.
    public static final RegistryObject<Block> RMA70_12_DECORATIVE_PIPE =
            pipe("rma70_12_decorative_pipe", Blocks.COPPER_BLOCK);
    public static final RegistryObject<Block> RMA70_12_DECORATIVE_VENT =
            vent("rma70_12_decorative_vent", Blocks.COPPER_BLOCK);

    // RMA70-24 — the heavier one. Iron-grade against -12's copper, so the pair are told apart
    // by sound and by mining time as well as by colour.
    public static final RegistryObject<Block> RMA70_24_DECORATIVE_PIPE =
            pipe("rma70_24_decorative_pipe", Blocks.IRON_BLOCK);
    public static final RegistryObject<Block> RMA70_24_DECORATIVE_VENT =
            vent("rma70_24_decorative_vent", Blocks.IRON_BLOCK);

    // D32 Steel — structural, the heaviest of the four.
    public static final RegistryObject<Block> D32_STEEL_DECORATIVE_PIPE =
            pipe("d32_steel_decorative_pipe", Blocks.IRON_BLOCK);
    public static final RegistryObject<Block> D32_STEEL_DECORATIVE_VENT =
            vent("d32_steel_decorative_vent", Blocks.IRON_BLOCK);

    // Iridescent Alloy — the refined one, and the only set that carries a light level. A dim
    // one: it should catch the eye in an unlit corridor without lighting the room.
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