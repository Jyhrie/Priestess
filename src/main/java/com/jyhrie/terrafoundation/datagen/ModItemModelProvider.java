package com.jyhrie.terrafoundation.datagen;

import com.jyhrie.terrafoundation.TerraFoundation;
import com.jyhrie.terrafoundation.item.ModItems;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.model.generators.ItemModelProvider;
import net.minecraftforge.common.data.ExistingFileHelper;

public class ModItemModelProvider extends ItemModelProvider {
    public ModItemModelProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, TerraFoundation.MOD_ID, existingFileHelper);
    }

    @Override
    protected void registerModels() {
//        withExistingParent(ModItems.JOKLUM_CRYSTAL.getId().getPath(), new ResourceLocation("item/generated"))
//                .texture("layer0", new ResourceLocation(TerraFoundation.MOD_ID, "item/" + ModItems.JOKLUM_CRYSTAL.getId().getPath()));
    }
}