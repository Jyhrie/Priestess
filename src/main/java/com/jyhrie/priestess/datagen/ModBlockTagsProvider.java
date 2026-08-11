package com.jyhrie.priestess.datagen;

import com.jyhrie.priestess.Priestess;
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

/**
 * Block tags — including the ones that <em>are</em> a mechanic.
 *
 * <h2>{@code sealed_by/<dungeon>}</h2>
 * A block in one of these cannot be broken by a player who has not cleared that dungeon,
 * wherever in the world it stands. See {@link Dungeon#sealedBlocks()} and
 * {@code DungeonLockdown}. Nothing else declares the gate: to put a block behind a dungeon,
 * add it here and run {@code runData}. To take it out again, delete the line. There is no
 * code path to update, which is the point of doing it with a tag — a dungeon's build set is
 * going to be tens of blocks, not five.
 *
 * <p>The tags are additive datapack files like any other, so a pack maintainer can gate their
 * own blocks behind a Priestess dungeon without touching this mod.
 *
 * <h2>{@code minecraft:wither_immune}</h2>
 * Vanilla's own tag, one of the three ways the Arts Lab set refuses to leave the world by
 * something other than a pickaxe. Unlike {@code sealed_by}, it does not lift when the dungeon
 * is cleared — see {@code ModBlocks.artsLab} for why all three are unconditional.
 *
 * <h2>{@code mineable/*}</h2>
 * These blocks copy {@code DEEPSLATE_TILES}, which sets {@code requiresCorrectToolForDrops}.
 * A block with that flag and no {@code mineable} tag is minable by nothing at all — it takes
 * the slow no-tool speed and then drops nothing whatever you hit it with. The tag is what
 * makes the pickaxe count, so it is not optional decoration; it is the difference between a
 * block that works and one that looks broken.
 */
public class ModBlockTagsProvider extends BlockTagsProvider {

    public ModBlockTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider,
                                @Nullable ExistingFileHelper existingFileHelper) {
        super(output, lookupProvider, Priestess.MOD_ID, existingFileHelper);
    }

    /** The Arts Lab build set: gated together, mined the same way. */
    private static final List<RegistryObject<Block>> ARTS_LAB = List.of(
            ModBlocks.RHINE_LAB_ARTS_LAB_CHISELED_WALL,
            ModBlocks.RHINE_LAB_ARTS_LAB_PLATED_WALL,
            ModBlocks.RHINE_LAB_ARTS_LAB_CONCRETE_WALL,
            ModBlocks.RHINE_LAB_ARTS_LAB_TILE,
            ModBlocks.RHINE_LAB_ARTS_LAB_PILLAR);

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        for (RegistryObject<Block> entry : ARTS_LAB) {
            Block block = entry.get();

            // The gate. The Arts Lab is sealed by Dorothy's Vision and not by Rhine Lab
            // itself — chapter order decides this, not the building the blocks are named
            // after, and a dungeon gating its own blocks would be a locked door with the key
            // behind it.
            tag(Dungeon.DOROTHYS_VISION.sealedBlocks()).add(block);

            // The wither is the one thing that eats through a block without a player behind
            // it and without an explosion to resist — it just deletes what it flies into. The
            // other two ways past the gate are refused in the block's own properties (see
            // ModBlocks.artsLab); this one is a tag because that is where vanilla looks.
            tag(BlockTags.WITHER_IMMUNE).add(block);

            tag(BlockTags.MINEABLE_WITH_PICKAXE).add(block);
            // Iron, matching the deepslate tier they are built from. Anything lower would let
            // a player who has just opened the gate strip the lab with the stone pickaxe they
            // walked in with.
            tag(BlockTags.NEEDS_IRON_TOOL).add(block);
        }
    }

    @Override
    public String getName() {
        return "Block Tags: " + Priestess.MOD_ID;
    }
}
