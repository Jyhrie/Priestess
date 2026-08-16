package com.jyhrie.priestess.datagen;

import com.jyhrie.priestess.Priestess;
import com.jyhrie.priestess.block.DecorativePipeBlock;
import com.jyhrie.priestess.block.DungeonSealed;
import com.jyhrie.priestess.block.ModBlocks;
import com.jyhrie.priestess.progression.Dungeon;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.common.data.BlockTagsProvider;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.registries.RegistryObject;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

/**
 * Block tags — including the ones that <em>are</em> a mechanic.
 *
 * <p>{@code sealed_by/<dungeon>} and {@code minecraft:wither_immune} are derived, not declared:
 * every registered {@link DungeonSealed} block goes into its own dungeon's tag, so the dungeon
 * named in the block's constructor is the single source of truth. See
 * {@link Dungeon#sealedBlocks()} and {@code docs/DUNGEON_BLOCKS.md}. They are ordinary additive
 * datapack files, so a pack can gate its own blocks behind a Priestess dungeon.
 *
 * <p>{@code mineable/*} and the tool tier are <b>not</b> derived, because they describe the
 * material rather than the gate. They are also not decoration: these blocks copy deepslate,
 * which sets {@code requiresCorrectToolForDrops}, and a block with that flag and no
 * {@code mineable} tag is mineable by nothing at all.
 */
public class ModBlockTagsProvider extends BlockTagsProvider {

    public ModBlockTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider,
                                @Nullable ExistingFileHelper existingFileHelper) {
        super(output, lookupProvider, Priestess.MOD_ID, existingFileHelper);
    }

    /**
     * Pickaxe, iron tier — anything lower would let a player who has just opened a gate strip
     * the place with the stone pickaxe they walked in with.
     */
    private static final List<RegistryObject<Block>> IRON_PICKAXE = List.of(
            ModBlocks.RHINE_LAB_ARTS_LAB_CHISELED_WALL,
            ModBlocks.RHINE_LAB_ARTS_LAB_PLATED_WALL,
            ModBlocks.RHINE_LAB_ARTS_LAB_CONCRETE_WALL,
            ModBlocks.RHINE_LAB_ARTS_LAB_TILE,
            ModBlocks.RHINE_LAB_ARTS_LAB_PILLAR,
            ModBlocks.SAL_VIENTO_CATACOMBS_STONE,
            ModBlocks.SAL_VIENTO_CATACOMBS_OVERGROWN_STONE);

    /** Every pipe, of every material. They all join each other because they share one tag. */
    private static final List<RegistryObject<Block>> PIPES = List.of(
            ModBlocks.SAL_VIENTO_CATACOMBS_PIPE,
            ModBlocks.RMA70_12_DECORATIVE_PIPE,
            ModBlocks.RMA70_24_DECORATIVE_PIPE,
            ModBlocks.D32_STEEL_DECORATIVE_PIPE,
            ModBlocks.IRIDESCENT_ALLOY_DECORATIVE_PIPE);

    /** Every vent. Any pipe docks into any of these, whatever material either one is. */
    private static final List<RegistryObject<Block>> VENTS = List.of(
            ModBlocks.RMA70_12_DECORATIVE_VENT,
            ModBlocks.RMA70_24_DECORATIVE_VENT,
            ModBlocks.D32_STEEL_DECORATIVE_VENT,
            ModBlocks.IRIDESCENT_ALLOY_DECORATIVE_VENT);

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        for (RegistryObject<Block> entry : ModBlocks.BLOCKS.getEntries()) {
            if (entry.get() instanceof DungeonSealed sealed) {
                tag(sealed.sealedBy().sealedBlocks()).add(entry.get());
                // The other ways past a gate are closed in DungeonSealed.seal; this one is a
                // tag because that is where vanilla looks.
                tag(BlockTags.WITHER_IMMUNE).add(entry.get());
            }
        }

        // Three small tags are everything that makes a flower behave like one to the rest of
        // the game. #small_flowers is what vanilla's #flowers and #sword_efficient are built
        // out of, so joining it is what makes bees pollinate the whiteflower. The petals need
        // #sword_efficient directly, being ground cover rather than a small flower.
        tag(BlockTags.SMALL_FLOWERS).add(ModBlocks.WHITEFLOWER.get());
        tag(BlockTags.SWORD_EFFICIENT).add(ModBlocks.WHITEFLOWER_PETALS.get());
        // How the flower pot's own break and pick behaviour finds the full ones.
        tag(BlockTags.FLOWER_POTS).add(ModBlocks.POTTED_WHITEFLOWER.get());

        for (RegistryObject<Block> entry : IRON_PICKAXE) {
            tag(BlockTags.MINEABLE_WITH_PICKAXE).add(entry.get());
            tag(BlockTags.NEEDS_IRON_TOOL).add(entry.get());
        }

        // A pipe missing from this tag still places and still looks like a pipe, it simply
        // never connects — the first thing to check when a run sits as separate stubs.
        for (RegistryObject<Block> entry : PIPES) {
            tag(DecorativePipeBlock.PIPES).add(entry.get());
        }
        for (RegistryObject<Block> entry : VENTS) {
            tag(DecorativePipeBlock.PIPE_ATTACHMENTS).add(entry.get());
        }

        // Metal-grade, so stone is enough. Decoration, not a gate.
        for (RegistryObject<Block> entry : Stream.concat(PIPES.stream(), VENTS.stream()).toList()) {
            tag(BlockTags.MINEABLE_WITH_PICKAXE).add(entry.get());
            tag(BlockTags.NEEDS_STONE_TOOL).add(entry.get());
        }
    }

    @Override
    public String getName() {
        return "Block Tags: " + Priestess.MOD_ID;
    }
}
