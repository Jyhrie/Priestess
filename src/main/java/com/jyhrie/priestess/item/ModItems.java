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

    // ── Movement I keys (Columbia) ────────────────────────────────────────────
    // The three things the chapter is actually about. Each is a one-off: stack size 1, and
    // dropped by code rather than by a loot table so the roll can never come up empty (see
    // MbJesseltonWilliams.dropCustomDeathLoot). They do nothing on their own yet — nothing
    // consumes them, because the machinery that would is a later chapter's problem — but
    // they exist, they are named, and they are obtainable, which is what makes the
    // progression something you can walk through rather than something written down.

    /** Drops from Jesselton Williams. Chapter 2's proof of completion. */
    public static final RegistryObject<Item> MANSFIELD_MASTER_KEY = ITEMS.register("mansfield_master_key",
            () -> new Item(new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));

    /** Drops from the Failed Vision. Chapter 3's proof of completion. */
    public static final RegistryObject<Item> DOROTHYS_NEURAL_PROCESSOR = ITEMS.register("dorothys_neural_processor",
            () -> new Item(new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));

    /**
     * The end of Movement I and the gate on the next: the recipe for refining
     * raw Originium safely, and with it the road to Ursus. Found at the top of Rhine Lab.
     */
    public static final RegistryObject<Item> BLUEPRINT_ORIGINIUM_REFINEMENT =
            ITEMS.register("blueprint_originium_refinement",
                    () -> new Item(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));

    // ── Medium and Dreamland ──────────────────────────────────────────────────
    // The pair the next stretch of progression is built out of, and the first two items in
    // the mod that are not one-offs by fiat. Neither does anything yet — nothing consumes
    // them, the same as the chapter keys above — but both are obtainable, which is the bar.

    /**
     * Drops from Failure, Replica and Bionic. Stackable, because it is a material rather
     * than a token: three mobs pay it out and there is no story in holding exactly one.
     */
    public static final RegistryObject<Item> MEDIUM = ITEMS.register("medium",
            () -> new Item(new Item.Properties().rarity(Rarity.UNCOMMON)));

    /**
     * Drops from "Awaken". A one-off like the chapter keys — stack size 1, dropped in code
     * so the roll can never come up empty (see {@code DvAwaken.dropCustomDeathLoot}).
     */
    public static final RegistryObject<Item> DREAMLAND = ITEMS.register("dreamland",
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

    /** This world's Jesselton's, taken off him when they locked him in. Aims the projector. */
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

    // The three Medium-bearers have no home in the world yet, so for them the egg is not a
    // test tool — it is the only way to meet one at all.
    public static final RegistryObject<Item> DV_FAILURE_SPAWN_EGG =
            ITEMS.register("dv_failure_spawn_egg",
                    () -> new ForgeSpawnEggItem(ModEntities.DV_FAILURE, 0x3A4A3E, 0xA8C060, new Item.Properties()));

    public static final RegistryObject<Item> DV_REPLICA_SPAWN_EGG =
            ITEMS.register("dv_replica_spawn_egg",
                    () -> new ForgeSpawnEggItem(ModEntities.DV_REPLICA, 0xC8C4BC, 0x5F8CE8, new Item.Properties()));

    public static final RegistryObject<Item> DV_BIONIC_SPAWN_EGG =
            ITEMS.register("dv_bionic_spawn_egg",
                    () -> new ForgeSpawnEggItem(ModEntities.DV_BIONIC, 0x3E4652, 0xE87A2E, new Item.Properties()));

    // Mansfield's inmates. Prison denim over three shades of the same idea, with the Sniper
    // marked out in bowstring pale so you can tell which egg is the ranged one.
    public static final RegistryObject<Item> MB_IMPRISONED_PUGILIST_SPAWN_EGG =
            ITEMS.register("mb_imprisoned_pugilist_spawn_egg",
                    () -> new ForgeSpawnEggItem(ModEntities.MB_IMPRISONED_PUGILIST, 0x3E4A5C, 0x8C9AA8, new Item.Properties()));

    public static final RegistryObject<Item> MB_IMPRISONED_RECIDIVIST_SPAWN_EGG =
            ITEMS.register("mb_imprisoned_recidivist_spawn_egg",
                    () -> new ForgeSpawnEggItem(ModEntities.MB_IMPRISONED_RECIDIVIST, 0x2E3846, 0xB8562E, new Item.Properties()));

    public static final RegistryObject<Item> MB_IMPRISONED_SNIPER_SPAWN_EGG =
            ITEMS.register("mb_imprisoned_sniper_spawn_egg",
                    () -> new ForgeSpawnEggItem(ModEntities.MB_IMPRISONED_SNIPER, 0x4A5668, 0xD8CFA8, new Item.Properties()));

    public static final RegistryObject<Item> MB_JESSELTON_WILLIAMS_SPAWN_EGG =
            ITEMS.register("mb_jesselton_williams_spawn_egg",
                    () -> new ForgeSpawnEggItem(ModEntities.MB_JESSELTON_WILLIAMS, 0x2A2438, 0xB8262E, new Item.Properties()));

    // Sal Viento. Abyssal blues and greens with a bioluminescent spot, so the whole dungeon's
    // eggs read as one shelf; the two big ones take gold and violet to stand out from the
    // five trash mobs above them.
    public static final RegistryObject<Item> SV_RUNNER_SPAWN_EGG =
            ITEMS.register("sv_runner_spawn_egg",
                    () -> new ForgeSpawnEggItem(ModEntities.SV_RUNNER, 0x1E4A50, 0x4AD8C8, new Item.Properties()));

    public static final RegistryObject<Item> SV_SPITTER_SPAWN_EGG =
            ITEMS.register("sv_spitter_spawn_egg",
                    () -> new ForgeSpawnEggItem(ModEntities.SV_SPITTER, 0x2E4A36, 0xA8E850, new Item.Properties()));

    public static final RegistryObject<Item> SV_REAPER_SPAWN_EGG =
            ITEMS.register("sv_reaper_spawn_egg",
                    () -> new ForgeSpawnEggItem(ModEntities.SV_REAPER, 0x182238, 0xD8D0B8, new Item.Properties()));

    public static final RegistryObject<Item> SV_CRAWLER_SPAWN_EGG =
            ITEMS.register("sv_crawler_spawn_egg",
                    () -> new ForgeSpawnEggItem(ModEntities.SV_CRAWLER, 0x3A3E2E, 0xC87A3A, new Item.Properties()));

    public static final RegistryObject<Item> SV_PIERCER_SPAWN_EGG =
            ITEMS.register("sv_piercer_spawn_egg",
                    () -> new ForgeSpawnEggItem(ModEntities.SV_PIERCER, 0x364452, 0xBCE0F0, new Item.Properties()));

    public static final RegistryObject<Item> SV_THE_FIRST_TO_TALK_SPAWN_EGG =
            ITEMS.register("sv_the_first_to_talk_spawn_egg",
                    () -> new ForgeSpawnEggItem(ModEntities.SV_THE_FIRST_TO_TALK, 0xB8AEA0, 0x5A2E78, new Item.Properties()));

    public static final RegistryObject<Item> SV_BISHOP_QUINTUS_SPAWN_EGG =
            ITEMS.register("sv_bishop_quintus_spawn_egg",
                    () -> new ForgeSpawnEggItem(ModEntities.SV_BISHOP_QUINTUS, 0x142844, 0xE0C870, new Item.Properties()));

    /** Dorothy's Terminal summons "Awaken" in place; this is the way to meet one anywhere else. */
    public static final RegistryObject<Item> DV_AWAKEN_SPAWN_EGG =
            ITEMS.register("dv_awaken_spawn_egg",
                    () -> new ForgeSpawnEggItem(ModEntities.DV_AWAKEN, 0xE0E4EC, 0x3A6FD8, new Item.Properties()));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
