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

/**
 * Block tags — including the ones that <em>are</em> a mechanic.
 *
 * <h2>{@code sealed_by/<dungeon>} and {@code minecraft:wither_immune}</h2>
 * Derived, not declared. Every registered block implementing {@link DungeonSealed} is put in
 * its own dungeon's tag and in vanilla's wither tag, so the dungeon named in the block's
 * constructor is the single source of truth and there is no list here to fall out of step with
 * {@code ModBlocks}. See {@link Dungeon#sealedBlocks()}, {@code DungeonLockdown} and
 * {@code docs/DUNGEON_BLOCKS.md}.
 *
 * <p>They are additive datapack files like any other, so a pack can gate its own blocks behind
 * a Priestess dungeon without touching this mod.
 *
 * <h2>{@code mineable/*} and the tool tier</h2>
 * <b>Not</b> derived, because these describe the material and not the gate — a sealed block is
 * not necessarily stone, and guessing would be wrong the first time one isn't. Listed per build
 * set below.
 *
 * <p>Not decoration either. These blocks copy deepslate, which sets
 * {@code requiresCorrectToolForDrops}, and a block with that flag and no {@code mineable} tag
 * is mineable by nothing at all — slow no-tool speed, and then no drop whatever you hit it
 * with.
 */
public class ModBlockTagsProvider extends BlockTagsProvider {

    public ModBlockTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider,
                                @Nullable ExistingFileHelper existingFileHelper) {
        super(output, lookupProvider, Priestess.MOD_ID, existingFileHelper);
    }

    /**
     * Pickaxe, iron tier. Both gated build sets are deepslate-grade masonry, so they share it —
     * anything lower would let a player who has just opened a gate strip the place with the
     * stone pickaxe they walked in with.
     */
    private static final List<RegistryObject<Block>> IRON_PICKAXE = List.of(
            ModBlocks.RHINE_LAB_ARTS_LAB_CHISELED_WALL,
            ModBlocks.RHINE_LAB_ARTS_LAB_PLATED_WALL,
            ModBlocks.RHINE_LAB_ARTS_LAB_CONCRETE_WALL,
            ModBlocks.RHINE_LAB_ARTS_LAB_TILE,
            ModBlocks.RHINE_LAB_ARTS_LAB_PILLAR,
            ModBlocks.SAL_VIENTO_CATACOMBS_STONE,
            ModBlocks.SAL_VIENTO_CATACOMBS_OVERGROWN_STONE);

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        for (RegistryObject<Block> entry : ModBlocks.BLOCKS.getEntries()) {
            if (entry.get() instanceof DungeonSealed sealed) {
                tag(sealed.sealedBy().sealedBlocks()).add(entry.get());
                // The wither deletes what it flies into — no player, no explosion to resist.
                // The other ways past a gate are closed in DungeonSealed.seal; this one is a
                // tag because that is where vanilla looks.
                tag(BlockTags.WITHER_IMMUNE).add(entry.get());
            }
        }

        for (RegistryObject<Block> entry : IRON_PICKAXE) {
            tag(BlockTags.MINEABLE_WITH_PICKAXE).add(entry.get());
            tag(BlockTags.NEEDS_IRON_TOOL).add(entry.get());
        }

        // What a DecorativePipeBlock joins up with. A pipe missing from this tag still places
        // and still looks like a pipe — it simply never connects to anything, which is the
        // first thing to check when a run of them sits in a row as separate stubs.
        tag(DecorativePipeBlock.PIPES)
                .add(ModBlocks.SAL_VIENTO_CATACOMBS_PIPE.get());

        // Copper-grade, so stone is enough. Pipes are dressing, not a gate.
        tag(BlockTags.MINEABLE_WITH_PICKAXE).add(ModBlocks.SAL_VIENTO_CATACOMBS_PIPE.get());
        tag(BlockTags.NEEDS_STONE_TOOL).add(ModBlocks.SAL_VIENTO_CATACOMBS_PIPE.get());
    }

    @Override
    public String getName() {
        return "Block Tags: " + Priestess.MOD_ID;
    }
}
