package com.jyhrie.terrafoundation.item;

import com.jyhrie.terrafoundation.TerraFoundation;
import com.jyhrie.terrafoundation.block.ModBlocks;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, TerraFoundation.MOD_ID);

    // ── FIX: Grab the icon dynamically by its registry location string ──
    public static final RegistryObject<CreativeModeTab> TERRA_FOUNDATION_TAB = CREATIVE_MODE_TABS.register("terra_foundation_tab",
            () -> CreativeModeTab.builder()
                    .icon(() -> new ItemStack(ModBlocks.IBERIAN_SAND.get()))
                    .displayItems((parameters, output) -> {
                        output.accept(ModBlocks.IBERIAN_SAND.get());
                        output.accept(ModBlocks.IBERIAN_SANDSTONE.get());
                    })
                    .title(Component.translatable("creativetab.terra_foundation_tab"))
                    .build());

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }
}