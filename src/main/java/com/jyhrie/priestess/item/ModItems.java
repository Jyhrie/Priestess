package com.jyhrie.priestess.item;

import com.jyhrie.priestess.Priestess;
import com.jyhrie.priestess.entity.ModEntities;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, Priestess.MOD_ID);

    // Standalone items go here; ModBlocks also registers its BlockItems into this
    // same DeferredRegister. See "Adding an item" in README.md.

    // ── Columbia chapter keys ─────────────────────────────────────────────────
    // The three things the chapter is actually about. Each is a one-off: stack size 1, and
    // dropped by code rather than by a loot table so the roll can never come up empty (see
    // JesseltonsShadow.dropCustomDeathLoot). They do nothing on their own yet — nothing
    // consumes them, because the machinery that would is a later chapter's problem — but
    // they exist, they are named, and they are obtainable, which is what makes the
    // progression something you can walk through rather than something written down.

    /** Drops from Jesselton's Shadow. Chapter 2's proof of completion. */
    public static final RegistryObject<Item> MANSFIELD_MASTER_KEY = ITEMS.register("mansfield_master_key",
            () -> new Item(new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));

    /** Drops from the Failed Vision. Chapter 3's proof of completion. */
    public static final RegistryObject<Item> DOROTHYS_NEURAL_PROCESSOR = ITEMS.register("dorothys_neural_processor",
            () -> new Item(new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));

    /**
     * The end of the Columbia chapter and the gate on the next one: the recipe for refining
     * raw Originium safely, and with it the road to Ursus. Found at the top of Rhine Lab.
     */
    public static final RegistryObject<Item> BLUEPRINT_ORIGINIUM_REFINEMENT =
            ITEMS.register("blueprint_originium_refinement",
                    () -> new Item(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));

    // ── Summoning catalysts ───────────────────────────────────────────────────
    // The things a boss altar wants. One is consumed per summon, which is what stops an
    // altar being an infinite boss farm: the altar re-arms itself when the boss dies, so
    // the only limit on fighting a boss twice is finding another of these.
    //
    // Deliberately NOT the items the bosses drop. Gating Jesselton behind the Master Key he
    // himself drops is a lock whose key is inside it; these are separate, findable things.
    // Nothing places them in the world yet — that is the dungeons' job and they are still
    // Python-generated placeholders — so for now they are a creative-tab item and a
    // /give, which is exactly what the spawn eggs above are for too.

    /** Jesselton's, taken off him when they locked him in. Wakes his effigy. */
    public static final RegistryObject<Item> TARNISHED_DOG_TAGS = ITEMS.register("tarnished_dog_tags",
            () -> new Item(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));

    /** A splinter of the network that is still faintly running. Wakes Dorothy's terminal. */
    public static final RegistryObject<Item> CORRUPTED_NEURAL_SHARD = ITEMS.register("corrupted_neural_shard",
            () -> new Item(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));

    // ── Spawn eggs ────────────────────────────────────────────────────────────
    // Every mob gets one. They are a test tool first — a boss that can only be seen by
    // walking to the one prison in the world is a boss you cannot tune — and they cost
    // nothing, because the egg model is a vanilla template and needs no texture.
    //
    // The two colours are the shell and the spots, in that order.

    public static final RegistryObject<Item> ORIGINIUM_SLUG_SPAWN_EGG =
            ITEMS.register("originium_slug_spawn_egg",
                    () -> new ForgeSpawnEggItem(ModEntities.ORIGINIUM_SLUG, 0x4A5A66, 0x5FC8E8, new Item.Properties()));

    public static final RegistryObject<Item> IMPRISONED_SHADOW_SPAWN_EGG =
            ITEMS.register("imprisoned_shadow_spawn_egg",
                    () -> new ForgeSpawnEggItem(ModEntities.IMPRISONED_SHADOW, 0x1E1B29, 0x6B5FA8, new Item.Properties()));

    public static final RegistryObject<Item> JESSELTONS_SHADOW_SPAWN_EGG =
            ITEMS.register("jesseltons_shadow_spawn_egg",
                    () -> new ForgeSpawnEggItem(ModEntities.JESSELTONS_SHADOW, 0x2A2438, 0xB8262E, new Item.Properties()));

    public static final RegistryObject<Item> FRANK_SPAWN_EGG =
            ITEMS.register("frank_spawn_egg",
                    () -> new ForgeSpawnEggItem(ModEntities.FRANK, 0x7A8C5E, 0xC85FA8, new Item.Properties()));

    public static final RegistryObject<Item> FAILED_VISION_SPAWN_EGG =
            ITEMS.register("failed_vision_spawn_egg",
                    () -> new ForgeSpawnEggItem(ModEntities.FAILED_VISION, 0x8C3A52, 0x5FC8E8, new Item.Properties()));

    public static final RegistryObject<Item> ROGUE_POWER_ARMOUR_SPAWN_EGG =
            ITEMS.register("rogue_power_armour_spawn_egg",
                    () -> new ForgeSpawnEggItem(ModEntities.ROGUE_POWER_ARMOUR, 0x5A6068, 0xE8B84A, new Item.Properties()));

    public static final RegistryObject<Item> RHINE_SECURITY_DRONE_SPAWN_EGG =
            ITEMS.register("rhine_security_drone_spawn_egg",
                    () -> new ForgeSpawnEggItem(ModEntities.RHINE_SECURITY_DRONE, 0xD8DCE0, 0x2E86C8, new Item.Properties()));

    /** "Awaken" has no dungeon and no summoner yet, so this is the only way to meet one. */
    public static final RegistryObject<Item> AWAKEN_SPAWN_EGG =
            ITEMS.register("awaken_spawn_egg",
                    () -> new ForgeSpawnEggItem(ModEntities.AWAKEN, 0xE0E4EC, 0x3A6FD8, new Item.Properties()));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
