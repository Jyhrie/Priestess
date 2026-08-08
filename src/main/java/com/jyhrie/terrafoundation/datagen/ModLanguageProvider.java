package com.jyhrie.terrafoundation.datagen;

import com.jyhrie.terrafoundation.TerraFoundation;
import com.jyhrie.terrafoundation.block.ModBlocks;
import com.jyhrie.terrafoundation.item.ModItems;
import net.minecraft.data.PackOutput;
import net.minecraftforge.common.data.LanguageProvider;

public class ModLanguageProvider extends LanguageProvider {
    public ModLanguageProvider(PackOutput output) {
        super(output, TerraFoundation.MOD_ID, "en_us");
    }

    @Override
    protected void addTranslations() {
        add("creativetab.terra_foundation_tab", "Terra Foundation");
        add(ModBlocks.IBERIAN_SAND.get(), "Iberian Sand");
        add(ModBlocks.IBERIAN_SANDSTONE.get(), "Iberian Sandstone");
    }
}