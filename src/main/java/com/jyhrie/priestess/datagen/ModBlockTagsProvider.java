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
 * wherever it stands — see {@link Dungeon#sealedBlocks()} and {@code DungeonLockdown}. Nothing
 * else declares the gate: add a line here, run {@code runData}, done. They are additive
 * datapack files, so a pack can gate its own blocks behind a Priestess dungeon too.
 *
 * <h2>{@code minecraft:wither_immune}</h2>
 * Vanilla's own tag. One of the three non-pickaxe ways out of the world that the Arts Lab set
 * refuses; unlike {@code sealed_by} it never lifts. See {@code ModBlocks.artsLab}.
 *
 * <h2>{@code mineable/*}</h2>
 * Not decoration. These blocks copy {@code DEEPSLATE_TILES}, which sets
 * {@code requiresCorrectToolForDrops}, and a block with that flag and no {@code mineable} tag
 * is mineable by nothing at all — slow no-tool speed, and then no drop whatever you hit it
 * with.
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

            // The wither deletes what it flies into — no player, no explosion to resist. The
            // other two ways past the gate live in ModBlocks.artsLab; this one is a tag
            // because that is where vanilla looks.
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
