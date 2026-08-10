package com.jyhrie.priestess.datagen;

import com.jyhrie.priestess.Priestess;
import com.jyhrie.priestess.item.ModItems;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.model.generators.ItemModelProvider;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.registries.RegistryObject;

public class ModItemModelProvider extends ItemModelProvider {

    public ModItemModelProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, Priestess.MOD_ID, existingFileHelper);
    }

    @Override
    protected void registerModels() {
        // Standalone items only. Block models come from ModBlockStateProvider's
        // simpleBlockWithItem(). See "Adding an item" in README.md.

        basicItem(ModItems.MANSFIELD_MASTER_KEY.get());
        basicItem(ModItems.DOROTHYS_NEURAL_PROCESSOR.get());
        basicItem(ModItems.BLUEPRINT_ORIGINIUM_REFINEMENT.get());
        basicItem(ModItems.TARNISHED_DOG_TAGS.get());
        basicItem(ModItems.CORRUPTED_NEURAL_SHARD.get());

        // Spawn eggs are the one item that needs no texture: the vanilla template tints two
        // greyscale layers from the colours passed to ForgeSpawnEggItem.
        spawnEgg(ModItems.ORIGINIUM_SLUG_SPAWN_EGG);
        spawnEgg(ModItems.JESSELTON_WILLIAMS_SPAWN_EGG);
        spawnEgg(ModItems.AWAKEN_SPAWN_EGG);
    }

    private void spawnEgg(RegistryObject<? extends net.minecraft.world.item.Item> egg) {
        withExistingParent(egg.getId().getPath(), new ResourceLocation("item/template_spawn_egg"));
    }
}
