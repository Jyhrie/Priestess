package com.jyhrie.priestess.item;

import com.jyhrie.priestess.Priestess;
import com.jyhrie.priestess.block.ModBlocks;
import com.jyhrie.priestess.weapons.ModWeapons;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Priestess.MOD_ID);

    public static final RegistryObject<CreativeModeTab> PRIESTESS_TAB = CREATIVE_MODE_TABS.register("priestess_tab",
            () -> CreativeModeTab.builder()
                    .icon(() -> new ItemStack(ModBlocks.IBERIAN_SAND.get()))
                    // Everything in ModItems.ITEMS shows up automatically, in registration
                    // order — that includes the BlockItems ModBlocks registers for us.
                    // ModWeapons keeps its own register so the weapons package stays
                    // self-contained, so it has to be asked for separately.
                    .displayItems((parameters, output) -> {
                        ModItems.ITEMS.getEntries().forEach(item -> output.accept(item.get()));
                        ModWeapons.ITEMS.getEntries().forEach(item -> output.accept(item.get()));
                    })
                    .title(Component.translatable("creativetab.priestess_tab"))
                    .build());

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }
}
