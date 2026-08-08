package com.jyhrie.priestess.item;

import com.jyhrie.priestess.Priestess;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, Priestess.MOD_ID);

    // Standalone items go here; ModBlocks also registers its BlockItems into this
    // same DeferredRegister. See "Adding an item" in README.md.

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
