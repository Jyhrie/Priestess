package com.jyhrie.terrafoundation.item;

import com.jyhrie.terrafoundation.TerraFoundation;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, TerraFoundation.MOD_ID);

    // Register standalone items here
//    public static final RegistryObject<Item> JOKLUM_CRYSTAL = ITEMS.register("joklum_crystal",
//            () -> new Item(new Item.Properties()));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}