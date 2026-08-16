package com.jyhrie.priestess.datagen;

import com.jyhrie.priestess.Priestess;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.concurrent.CompletableFuture;

@Mod.EventBusSubscriber(modid = Priestess.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class DataGenerators {

    @SubscribeEvent
    public static void gatherData(GatherDataEvent event) {
        DataGenerator generator = event.getGenerator();
        PackOutput packOutput = generator.getPackOutput();
        CompletableFuture<HolderLookup.Provider> lookupProvider = event.getLookupProvider();
        ExistingFileHelper existingFileHelper = event.getExistingFileHelper();

        // Server data -> src/generated/resources/data/priestess/
        generator.addProvider(event.includeServer(), ModLootTableProvider.create(packOutput));

        ModWorldGenProvider worldGen = new ModWorldGenProvider(packOutput, lookupProvider);
        generator.addProvider(event.includeServer(), worldGen);

        // Fed from the worldgen provider's registries, NOT from the event's. Damage types
        // are a datapack registry this mod writes itself, so the vanilla lookup has never
        // heard of priestess:void_arts — and a tag provider that cannot resolve an entry it
        // is asked to tag fails datagen outright rather than skipping it.
        generator.addProvider(event.includeServer(),
                new ModDamageTypeTagsProvider(packOutput, worldGen.getRegistryProvider(), existingFileHelper));

        // In a variable because the item tags provider below needs its contents to resolve any
        // tag copied from a block tag.
        ModBlockTagsProvider blockTags =
                new ModBlockTagsProvider(packOutput, lookupProvider, existingFileHelper);
        generator.addProvider(event.includeServer(), blockTags);

        generator.addProvider(event.includeServer(), new ModItemTagsProvider(
                packOutput, lookupProvider, blockTags.contentsGetter(), existingFileHelper));

        // Without this the Module slot does not appear in the GUI. See docs/CURIOS.md.
        generator.addProvider(event.includeServer(),
                new ModCuriosDataProvider(packOutput, existingFileHelper, lookupProvider));

        // Not a datapack file — writes docs/terra_world_preview.png.
        generator.addProvider(event.includeServer(), new ModTerraPreviewProvider(packOutput));

        // Client data -> src/generated/resources/assets/priestess/
        generator.addProvider(event.includeClient(), new ModBlockStateProvider(packOutput, existingFileHelper));
        generator.addProvider(event.includeClient(), new ModItemModelProvider(packOutput, existingFileHelper));
        generator.addProvider(event.includeClient(), new ModLanguageProvider(packOutput));
    }
}