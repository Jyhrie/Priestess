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
        basicItem(ModItems.MEDIUM.get());
        basicItem(ModItems.DREAMLAND.get());

        // Spawn eggs are the one item that needs no texture: the vanilla template tints two
        // greyscale layers from the colours passed to ForgeSpawnEggItem.
        spawnEgg(ModItems.ORIGINIUM_SLUG_SPAWN_EGG);
        spawnEgg(ModItems.DV_FAILURE_SPAWN_EGG);
        spawnEgg(ModItems.DV_REPLICA_SPAWN_EGG);
        spawnEgg(ModItems.DV_BIONIC_SPAWN_EGG);
        spawnEgg(ModItems.MB_IMPRISONED_PUGILIST_SPAWN_EGG);
        spawnEgg(ModItems.MB_IMPRISONED_RECIDIVIST_SPAWN_EGG);
        spawnEgg(ModItems.MB_IMPRISONED_SNIPER_SPAWN_EGG);
        spawnEgg(ModItems.MB_JESSELTON_WILLIAMS_SPAWN_EGG);
        spawnEgg(ModItems.SV_RUNNER_SPAWN_EGG);
        spawnEgg(ModItems.SV_SPITTER_SPAWN_EGG);
        spawnEgg(ModItems.SV_REAPER_SPAWN_EGG);
        spawnEgg(ModItems.SV_CRAWLER_SPAWN_EGG);
        spawnEgg(ModItems.SV_PIERCER_SPAWN_EGG);
        spawnEgg(ModItems.SV_THE_FIRST_TO_TALK_SPAWN_EGG);
        spawnEgg(ModItems.SV_BISHOP_QUINTUS_SPAWN_EGG);
        spawnEgg(ModItems.DV_AWAKEN_SPAWN_EGG);
    }

    private void spawnEgg(RegistryObject<? extends net.minecraft.world.item.Item> egg) {
        withExistingParent(egg.getId().getPath(), new ResourceLocation("item/template_spawn_egg"));
    }
}
