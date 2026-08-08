package com.jyhrie.priestess.datagen;

import com.jyhrie.priestess.Priestess;
import net.minecraft.data.PackOutput;
import net.minecraftforge.client.model.generators.ItemModelProvider;
import net.minecraftforge.common.data.ExistingFileHelper;

public class ModItemModelProvider extends ItemModelProvider {
    public ModItemModelProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, Priestess.MOD_ID, existingFileHelper);
    }

    @Override
    protected void registerModels() {
        // Standalone items only. Block models come from ModBlockStateProvider's
        // simpleBlockWithItem(). See "Adding an item" in README.md.
    }
}
