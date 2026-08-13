package com.jyhrie.priestess.datagen;

import com.jyhrie.priestess.Priestess;
import com.jyhrie.priestess.item.ModItems;
import com.jyhrie.priestess.item.ModTags;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.ItemTagsProvider;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

/**
 * Item tags. Right now this exists for one reason: <b>it is how an item declares which Curios
 * slot it goes in.</b> The item is listed in {@code curios:module}, and the "tag" validator on
 * the Module slot does the rest.
 *
 * <p>This is the step that actually makes a curio wearable, and the one that fails quietly —
 * skip it and the item registers, renders, and stacks in the inventory forever.
 *
 * <p>Takes the block tag provider's contents because {@code ItemTagsProvider} can copy a block
 * tag into an item tag, and it has to be able to resolve one to do so. Nothing copies one yet.
 */
public class ModItemTagsProvider extends ItemTagsProvider {

    public ModItemTagsProvider(PackOutput output,
                               CompletableFuture<HolderLookup.Provider> lookupProvider,
                               CompletableFuture<TagsProvider.TagLookup<Block>> blockTags,
                               @Nullable ExistingFileHelper existingFileHelper) {
        super(output, lookupProvider, blockTags, Priestess.MOD_ID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        tag(ModTags.Items.CURIOS_MODULE)
                .add(ModItems.TEMPLATE.get());
    }
}
