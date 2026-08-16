package com.jyhrie.priestess.entity;

import com.jyhrie.priestess.Priestess;
import com.jyhrie.priestess.entity.bosses.DvAwaken;
import com.jyhrie.priestess.entity.bosses.MbJesseltonWilliams;
import com.jyhrie.priestess.entity.mobs.dorothysvision.DvBionic;
import com.jyhrie.priestess.entity.mobs.dorothysvision.DvFailure;
import com.jyhrie.priestess.entity.mobs.OriginiumSlug;
import com.jyhrie.priestess.entity.mobs.dorothysvision.DvReplica;
import com.jyhrie.priestess.entity.mobs.mansfieldbreak.MbImprisonedPugilist;
import com.jyhrie.priestess.entity.mobs.mansfieldbreak.MbImprisonedRecidivist;
import com.jyhrie.priestess.entity.mobs.mansfieldbreak.MbImprisonedSniper;
import com.jyhrie.priestess.entity.mobs.undertides.SvCrawler;
import com.jyhrie.priestess.entity.mobs.undertides.SvPiercer;
import com.jyhrie.priestess.entity.mobs.undertides.SvReaper;
import com.jyhrie.priestess.entity.mobs.undertides.SvRunner;
import com.jyhrie.priestess.entity.mobs.undertides.SvSpitter;
import com.jyhrie.priestess.entity.bosses.SvBishopQuintus;
import com.jyhrie.priestess.entity.minibosses.SvTheFirstToTalk;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Every mob in the mod, across every score movement.
 *
 * <p>What each of them is <em>for</em> lives in {@code docs/SCORE_MOVEMENTS.md}; this file
 * is the registry and the wiring.
 *
 * <p>Entities are <em>runtime</em> registrations, so there is no {@code runData} step for
 * anything in this package. What does go through datagen is the biome spawn list, the language
 * file and the loot tables, so a new mob is not finished until those three know about it.
 *
 * <p><b>Nothing in this mod spawns naturally.</b> Every mob is reached through its spawn egg,
 * except the bosses a structure places. See {@code docs/SPAWNING.md}.
 *
 * <p>The root holds only the registry and the base classes, {@link BossMonster} and
 * {@link GeoMonster}; mobs live one level down in {@code mobs/} (a package per dungeon),
 * {@code minibosses/}, {@code bosses/} and {@code projectiles/}. Packages are invisible to the
 * game — a mob's registry name is the string passed to {@code ENTITY_TYPES.register} below and
 * its assets key off that, so moving a class between folders is a rename-and-recompile.
 *
 * <p>That is why the grouping is also spelled out in the names: {@code dv_}, {@code mb_},
 * {@code sv_}, or none. <b>The table in the README is the authority for which code is
 * whose</b> — it is not always the package's initials. The prefix never appears in display
 * names, so rename identifiers freely and leave the prose alone.
 *
 * <p>Adding a mob:
 * <ol>
 *   <li>write the class in {@code mobs/} or {@code bosses/}, extending {@link GeoMonster} or
 *       {@link BossMonster} as appropriate,</li>
 *   <li>register the {@link EntityType} here,</li>
 *   <li>add its attributes to {@link #registerAttributes},</li>
 *   <li>if it should spawn naturally, add a {@link SpawnPlacements} rule in
 *       {@link #registerSpawnPlacements} <em>and</em> a spawner entry in {@code ModBiomes},</li>
 *   <li>bind a renderer in {@code client/PriestessClient},</li>
 *   <li>name it in {@code ModLanguageProvider} and give it a loot table in
 *       {@code ModLootTableProvider}.</li>
 * </ol>
 */
@Mod.EventBusSubscriber(modid = Priestess.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModEntities {

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, Priestess.MOD_ID);

    /** Low and long, so it reads as vermin rather than as a threat you should stop for. */
    public static final RegistryObject<EntityType<OriginiumSlug>> ORIGINIUM_SLUG =
            ENTITY_TYPES.register("originium_slug", () -> EntityType.Builder
                    .of(OriginiumSlug::new, MobCategory.MONSTER)
                    .sized(0.9F, 0.5F)
                    .clientTrackingRange(8)
                    .build("originium_slug"));

    // The Medium-bearers: three melee mobs differing only in numbers and silhouette. Hitbox
    // widths are the model's shoulders, not its arm span — arms hanging outside the box is how
    // every vanilla humanoid works, and boxing them in makes a mob impossible to walk past.

    /** Short and hunched — the shortest hitbox of the three, and the fastest. */
    public static final RegistryObject<EntityType<DvFailure>> DV_FAILURE =
            ENTITY_TYPES.register("dv_failure", () -> EntityType.Builder
                    .of(DvFailure::new, MobCategory.MONSTER)
                    .sized(0.8F, 1.6F)
                    .clientTrackingRange(8)
                    .build("dv_failure"));

    /** A plain two-block humanoid; the baseline the other two are read against. */
    public static final RegistryObject<EntityType<DvReplica>> DV_REPLICA =
            ENTITY_TYPES.register("dv_replica", () -> EntityType.Builder
                    .of(DvReplica::new, MobCategory.MONSTER)
                    .sized(0.7F, 1.95F)
                    .clientTrackingRange(8)
                    .build("dv_replica"));

    /** Wide and tall enough to block a doorway, which is most of what it is for. */
    public static final RegistryObject<EntityType<DvBionic>> DV_BIONIC =
            ENTITY_TYPES.register("dv_bionic", () -> EntityType.Builder
                    .of(DvBionic::new, MobCategory.MONSTER)
                    .sized(0.9F, 2.1F)
                    .clientTrackingRange(10)
                    .build("dv_bionic"));

    // Mansfield State Prison: three rungs of one ladder — a body, a bigger body, and the one
    // that shoots. The Sniper is the only mob in the mod throwing a projectile the player can
    // dodge; everything else ranged is ArtsBeam, which is hitscan.

    /** A zombie by another name; the body that fills a corridor. */
    public static final RegistryObject<EntityType<MbImprisonedPugilist>> MB_IMPRISONED_PUGILIST =
            ENTITY_TYPES.register("mb_imprisoned_pugilist", () -> EntityType.Builder
                    .of(MbImprisonedPugilist::new, MobCategory.MONSTER)
                    .sized(0.7F, 1.95F)
                    .clientTrackingRange(8)
                    .build("mb_imprisoned_pugilist"));

    /** The same mob half again as large, and heavy enough that it cannot be walked backwards. */
    public static final RegistryObject<EntityType<MbImprisonedRecidivist>> MB_IMPRISONED_RECIDIVIST =
            ENTITY_TYPES.register("mb_imprisoned_recidivist", () -> EntityType.Builder
                    .of(MbImprisonedRecidivist::new, MobCategory.MONSTER)
                    .sized(1.0F, 2.5F)
                    .clientTrackingRange(12)
                    .build("mb_imprisoned_recidivist"));

    /** Thin, skeleton-proportioned, and the reason to stay behind something. */
    public static final RegistryObject<EntityType<MbImprisonedSniper>> MB_IMPRISONED_SNIPER =
            ENTITY_TYPES.register("mb_imprisoned_sniper", () -> EntityType.Builder
                    .of(MbImprisonedSniper::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.99F)
                    .clientTrackingRange(12)
                    .build("mb_imprisoned_sniper"));

    public static final RegistryObject<EntityType<MbJesseltonWilliams>> MB_JESSELTON_WILLIAMS =
            ENTITY_TYPES.register("mb_jesselton_williams", () -> EntityType.Builder
                    .of(MbJesseltonWilliams::new, MobCategory.MONSTER)
                    .sized(0.7F, 2.2F)
                    .fireImmune()
                    .clientTrackingRange(16)
                    .build("mb_jesselton_williams"));

    // Sal Viento's five trash mobs, built as a rock-paper-scissors rather than a ladder: no two
    // are answered the same way.
    //
    //   Runner    outruns you                          kill it or be caught
    //   Crawler   comes in numbers, low                clear it or be nibbled
    //   Reaper    hits hardest, slow                   walk away and come back
    //   Piercer   hits nearly as hard, dies instantly  notice it first
    //   Spitter   outranges everything                 break line of sight

    /** Faster than a sprinting player — the only mob in the mod that is. */
    public static final RegistryObject<EntityType<SvRunner>> SV_RUNNER =
            ENTITY_TYPES.register("sv_runner", () -> EntityType.Builder
                    .of(SvRunner::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.8F)
                    .clientTrackingRange(8)
                    .build("sv_runner"));

    /** Squat and wide-mouthed; the hitbox is the maw. */
    public static final RegistryObject<EntityType<SvSpitter>> SV_SPITTER =
            ENTITY_TYPES.register("sv_spitter", () -> EntityType.Builder
                    .of(SvSpitter::new, MobCategory.MONSTER)
                    .sized(0.9F, 1.4F)
                    .clientTrackingRange(10)
                    .build("sv_spitter"));

    /** Tall enough to be seen over the rest of a room, which is the warning. */
    public static final RegistryObject<EntityType<SvReaper>> SV_REAPER =
            ENTITY_TYPES.register("sv_reaper", () -> EntityType.Builder
                    .of(SvReaper::new, MobCategory.MONSTER)
                    .sized(0.8F, 2.4F)
                    .clientTrackingRange(10)
                    .build("sv_reaper"));

    /**
     * Knee-high on purpose: it sits under a normal swing arc, so clearing a room of these is
     * something you choose to do rather than something that happens incidentally.
     */
    public static final RegistryObject<EntityType<SvCrawler>> SV_CRAWLER =
            ENTITY_TYPES.register("sv_crawler", () -> EntityType.Builder
                    .of(SvCrawler::new, MobCategory.MONSTER)
                    .sized(1.0F, 0.7F)
                    .clientTrackingRange(8)
                    .build("sv_crawler"));

    /** Narrow, so it hides behind anything — which is most of what makes it dangerous. */
    public static final RegistryObject<EntityType<SvPiercer>> SV_PIERCER =
            ENTITY_TYPES.register("sv_piercer", () -> EntityType.Builder
                    .of(SvPiercer::new, MobCategory.MONSTER)
                    .sized(0.7F, 1.9F)
                    .clientTrackingRange(8)
                    .build("sv_piercer"));

    /** Miniboss. Bigger than anything else that walks down there, and it has a bar. */
    public static final RegistryObject<EntityType<SvTheFirstToTalk>> SV_THE_FIRST_TO_TALK =
            ENTITY_TYPES.register("sv_the_first_to_talk", () -> EntityType.Builder
                    .of(SvTheFirstToTalk::new, MobCategory.MONSTER)
                    .sized(1.2F, 2.6F)
                    .clientTrackingRange(16)
                    .build("sv_the_first_to_talk"));

    /**
     * The box is sized to the model's own geometry rather than scaled up by the renderer, so
     * what you can see and what you can hit are the same shape. Placement matters: it cannot
     * be pushed out of a wall it overlaps, so it wants an open arena.
     */
    public static final RegistryObject<EntityType<SvBishopQuintus>> SV_BISHOP_QUINTUS =
            ENTITY_TYPES.register("sv_bishop_quintus", () -> EntityType.Builder
                    .of(SvBishopQuintus::new, MobCategory.MONSTER)
                    .sized(2.6F, 4.0F)
                    .fireImmune()
                    .clientTrackingRange(24)
                    .build("sv_bishop_quintus"));

    /**
     * "Awaken", summoned by {@code DorothysTerminalBlock}. The width is the model's own at the
     * 3x {@code DvAwakenRenderer.SCALE} draws it, so the two constants have to move together —
     * rescale the renderer and the hitbox stops matching what you can see.
     *
     * <p>It cannot be pushed out of a wall it overlaps, so it wants an open arena.
     */
    public static final RegistryObject<EntityType<DvAwaken>> DV_AWAKEN =
            ENTITY_TYPES.register("dv_awaken", () -> EntityType.Builder
                    .of(DvAwaken::new, MobCategory.MONSTER)
                    .sized(6.75F, 6.75F)
                    .fireImmune()
                    .clientTrackingRange(16)
                    .build("dv_awaken"));

    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(ORIGINIUM_SLUG.get(), OriginiumSlug.attributes().build());
        event.put(DV_FAILURE.get(), DvFailure.attributes().build());
        event.put(DV_REPLICA.get(), DvReplica.attributes().build());
        event.put(DV_BIONIC.get(), DvBionic.attributes().build());
        event.put(MB_IMPRISONED_PUGILIST.get(), MbImprisonedPugilist.attributes().build());
        event.put(MB_IMPRISONED_RECIDIVIST.get(), MbImprisonedRecidivist.attributes().build());
        event.put(MB_IMPRISONED_SNIPER.get(), MbImprisonedSniper.attributes().build());
        event.put(MB_JESSELTON_WILLIAMS.get(), MbJesseltonWilliams.attributes().build());
        event.put(DV_AWAKEN.get(), DvAwaken.attributes().build());
        event.put(SV_RUNNER.get(), SvRunner.attributes().build());
        event.put(SV_SPITTER.get(), SvSpitter.attributes().build());
        event.put(SV_REAPER.get(), SvReaper.attributes().build());
        event.put(SV_CRAWLER.get(), SvCrawler.attributes().build());
        event.put(SV_PIERCER.get(), SvPiercer.attributes().build());
        event.put(SV_THE_FIRST_TO_TALK.get(), SvTheFirstToTalk.attributes().build());
        event.put(SV_BISHOP_QUINTUS.get(), SvBishopQuintus.attributes().build());
    }

    // There are no spawn placements, so there is no FMLCommonSetupEvent handler here at all.
    // Putting one back is two halves that must both be present: a SpawnPlacements.register call
    // subscribed to FMLCommonSetupEvent (wrapped in event.enqueueWork — placement state is not
    // thread safe and setup runs in parallel), *and* a spawner entry in ModBiomes. Either alone
    // does nothing. docs/SPAWNING.md walks through both.

    public static void register(IEventBus eventBus) {
        ENTITY_TYPES.register(eventBus);
    }

    private ModEntities() {
    }
}
