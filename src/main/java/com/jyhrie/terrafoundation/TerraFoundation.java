package com.jyhrie.terrafoundation;

import com.jyhrie.terrafoundation.block.ModBlocks;
import com.jyhrie.terrafoundation.item.ModCreativeTabs;
import com.jyhrie.terrafoundation.item.ModItems;
import com.jyhrie.terrafoundation.world.dimension.ModDimensions;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.ForgeRegistries;

@Mod(TerraFoundation.MOD_ID)
public class TerraFoundation {

    public static final String MOD_ID = "terrafoundation";

    public TerraFoundation() {
        // Register our custom dimensions
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModItems.register(modEventBus);
        ModBlocks.register(modEventBus);
        ModDimensions.register();
        ModCreativeTabs.register(modEventBus);
    }

}