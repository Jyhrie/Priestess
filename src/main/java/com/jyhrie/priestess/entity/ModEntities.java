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
 * <p>Unlike the dimension and its biomes, entities are <em>runtime</em> registrations — a
 * mob is code, not data, so there is no {@code runData} step for anything in this package.
 * What does still go through datagen is the biome spawn list ({@code ModBiomes}), the
 * language file, and the loot tables, so a new mob here is not finished until those three
 * know about it.
 *
 * <h2>Who belongs to which movement</h2>
 * <table border="1">
 *   <caption>The roster</caption>
 *   <tr><th>Mob</th><th>Movement</th><th>Dungeon</th><th>What it is for</th></tr>
 *
 *   <tr><td>Originium Slug</td><td><i>none</i></td><td><i>none</i></td><td>wasteland vermin.
 *       A blank mob — every mechanic it had has been cut, pending replacements</td></tr>
 *
 *   <tr><td>Failure</td><td>I — Columbia</td><td>Dorothy's Vision</td><td>melee — the swarm;
 *       drops Medium</td></tr>
 *   <tr><td>Replica</td><td>I — Columbia</td><td>Dorothy's Vision</td><td>melee — the
 *       baseline; drops Medium</td></tr>
 *   <tr><td>Bionic</td><td>I — Columbia</td><td>Dorothy's Vision</td><td>melee — the wall;
 *       drops Medium</td></tr>
 *   <tr><td>"Awaken"</td><td>I — Columbia</td><td>Dorothy's Vision</td><td><b>boss</b> —
 *       summoned by the terminal, no attacks yet; drops Dreamland</td></tr>
 *
 *   <tr><td>Imprisoned Pugilist</td><td>I — Columbia</td><td>Mansfield Break</td><td>melee —
 *       a zombie in all but name</td></tr>
 *   <tr><td>Imprisoned Recidivist</td><td>I — Columbia</td><td>Mansfield Break</td><td>melee
 *       — the same mob half again as large, and too heavy to kite</td></tr>
 *   <tr><td>Imprisoned Sniper</td><td>I — Columbia</td><td>Mansfield Break</td><td>ranged —
 *       real arrows, so the only shot in the mod you can dodge</td></tr>
 *   <tr><td>Jesselton Williams</td><td>I — Columbia</td><td>Mansfield Break</td><td><b>boss</b>
 *       — two phases; drops the Master Key</td></tr>
 *
 *   <tr><td>Runner</td><td>II — Ægir</td><td>Under Tides</td><td>melee — the only mob faster
 *       than a sprinting player</td></tr>
 *   <tr><td>Spitter</td><td>II — Ægir</td><td>Under Tides</td><td>ranged — hitscan spit on a
 *       vanilla ranged goal</td></tr>
 *   <tr><td>Reaper</td><td>II — Ægir</td><td>Under Tides</td><td>melee — hits hard and
 *       survives being hit back</td></tr>
 *   <tr><td>Crawler</td><td>II — Ægir</td><td>Under Tides</td><td>melee — knee-high, comes in
 *       numbers</td></tr>
 *   <tr><td>Piercer</td><td>II — Ægir</td><td>Under Tides</td><td>melee — glass cannon; see
 *       its javadoc, it does not literally pierce yet</td></tr>
 *   <tr><td>The First to Talk</td><td>II — Ægir</td><td>Under Tides</td><td><b>miniboss</b> —
 *       bar, melee, enrages at half health</td></tr>
 *   <tr><td>Sal Viento Bishop Quintus</td><td>II — Ægir</td><td>Under Tides</td><td><b>boss</b>
 *       — fully immobile, and unlike "Awaken" it ships with its attack</td></tr>
 * </table>
 *
 * <p><b>Nothing in this mod spawns naturally.</b> The slug was the last one that did, and its
 * spawn wiring went when its mechanics did. Every mob is reached through its spawn egg,
 * except the bosses a structure places — and the dungeons that would place the rest are still
 * Python-generated placeholders. See {@code docs/SPAWNING.md} for how to give a movement its
 * population back.
 *
 * <h2>How this package is laid out</h2>
 * The root holds only the registry and the base classes — {@link BossMonster} and
 * {@link GeoMonster}. Everything that <em>is</em> a mob lives one level down:
 * <ul>
 *   <li>{@code mobs/} — the trash mobs, in a package per dungeon:
 *       {@code mobs/dorothysvision/}, {@code mobs/mansfieldbreak/},
 *       {@code mobs/undertides/}. Anything belonging to no dungeon sits directly in
 *       {@code mobs/}, which today is the slug alone.</li>
 *   <li>{@code minibosses/} — a tier of its own: mobs that carry a boss bar and all the
 *       {@link BossMonster} machinery, but are a fight inside a dungeon rather than the end
 *       of one, and gate no progression item. One so far.</li>
 *   <li>{@code bosses/} — the three that end a dungeon.</li>
 *   <li>{@code projectiles/} — {@code ArtsBeam}, which is a static helper rather than an
 *       entity but is the mod's whole ranged-attack story, and the place a real projectile
 *       entity would go when one exists.</li>
 * </ul>
 * Packages are invisible to the game: a mob's registry name is the string passed to
 * {@code ENTITY_TYPES.register} below, and its assets are keyed off that name, not off where
 * the class sits. Moving a class between these folders is a rename-and-recompile, nothing
 * more.
 *
 * <h2>Everything carries a dungeon code</h2>
 * Packages are invisible to the game and invisible in most of an IDE's search results, so
 * the grouping has to be spelled out in the names themselves — otherwise {@code /summon}
 * autocomplete, the creative tab, the asset folders and the class list are all one
 * undifferentiated alphabetical run. Every mob belonging to a dungeon is prefixed with that
 * dungeon's code:
 * <table border="1">
 *   <caption>Prefixes</caption>
 *   <tr><th>Prefix</th><th>Dungeon</th><th>Members</th></tr>
 *   <tr><td>{@code dv_} / {@code Dv}</td><td>Dorothy's Vision</td><td>Failure, Replica,
 *       Bionic, "Awaken"</td></tr>
 *   <tr><td>{@code mb_} / {@code Mb}</td><td>Mansfield Break</td><td>the three Imprisoned,
 *       Jesselton Williams</td></tr>
 *   <tr><td>{@code sv_} / {@code Sv}</td><td>Under Tides</td><td>Runner, Spitter, Reaper,
 *       Crawler, Piercer, The First to Talk, Bishop Quintus</td></tr>
 *   <tr><td><i>none</i></td><td>—</td><td>the Originium Slug, which belongs to no
 *       dungeon</td></tr>
 * </table>
 *
 * <p>Two spellings of the same prefix, one per naming convention. {@code snake_case} for
 * anything the game reads — the entity id, the spawn egg id,
 * {@code geo/entity/<id>.geo.json}, {@code textures/entity/<id>.png} — and
 * {@code SCREAMING_SNAKE} for the constants in this file and {@code ModItems}, which track
 * their registry names. {@code PascalCase} for the classes and their files, so
 * {@code DvFailure} sits in {@code mobs/dorothysvision/}. The renderer follows the boss it
 * draws: {@code DvAwakenRenderer}.
 *
 * <p>The one place the prefix does <b>not</b> appear is the <b>display names</b>.
 * {@code ModLanguageProvider} still reads "Failure" and "Jesselton Williams", and the class
 * javadoc of each mob still opens with its in-game name. A player never sees {@code dv_} or
 * {@code Mb}. The prefix is a developer-facing filing system, not part of the fiction — so
 * when editing these files, rename identifiers freely and leave the prose alone.
 *
 * <h2>Adding a mob</h2>
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

    // ── The wastes ────────────────────────────────────────────────────────────

    /** Low and long, so it reads as vermin rather than as a threat you should stop for. */
    public static final RegistryObject<EntityType<OriginiumSlug>> ORIGINIUM_SLUG =
            ENTITY_TYPES.register("originium_slug", () -> EntityType.Builder
                    .of(OriginiumSlug::new, MobCategory.MONSTER)
                    .sized(0.9F, 0.5F)
                    .clientTrackingRange(8)
                    .build("originium_slug"));

    // ── The Medium-bearers ────────────────────────────────────────────────────
    // Three melee mobs that differ in nothing but numbers and silhouette, and the only
    // source of Medium. They share {@link GeoMonster}, which is the goal set and the
    // GeckoLib plumbing; what each of them is lives in its own class.
    //
    // They have no home yet. Nothing spawns them naturally and no structure places them,
    // so today they are a spawn egg — deliberately, because deciding which dungeon they
    // belong to is a design call rather than a wiring one. When that is settled it is a
    // SpawnPlacements rule and a ModBiomes spawner entry, or a line in a dungeon's NBT.
    //
    // Hitbox widths are the model's shoulders, not its arm span: arms hanging outside the
    // box is how every vanilla humanoid works, and boxing the arms in would make them
    // impossible to walk past in a corridor.

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

    // ── Mansfield State Prison ────────────────────────────────────────────────
    // The break: the inmates Jesselton let out, and the population of the dungeon he sits at
    // the end of. Three rungs of one ladder — a body, a bigger body, and the one that shoots.
    //
    // The Sniper is the only mob in the mod that throws a projectile the player can dodge.
    // Everything else ranged is ArtsBeam, which is hitscan, so cover is the counterplay
    // rather than movement; the Sniper is the exception that makes a cell block's pillars
    // worth using in the other direction.
    //
    // Like everything else here they have no spawn wiring. They are meant to be placed by
    // Mansfield's structure, and until that NBT has them in it they are spawn eggs.

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
                    // Taller than the others and worth seeing coming, so it is tracked from
                    // further out than the two mobs it shares a room with.
                    .clientTrackingRange(12)
                    .build("mb_imprisoned_recidivist"));

    /** Thin, skeleton-proportioned, and the reason to stay behind something. */
    public static final RegistryObject<EntityType<MbImprisonedSniper>> MB_IMPRISONED_SNIPER =
            ENTITY_TYPES.register("mb_imprisoned_sniper", () -> EntityType.Builder
                    .of(MbImprisonedSniper::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.99F)
                    // It opens fire at 16 blocks, so it has to still be rendered at 16.
                    .clientTrackingRange(12)
                    .build("mb_imprisoned_sniper"));

    public static final RegistryObject<EntityType<MbJesseltonWilliams>> MB_JESSELTON_WILLIAMS =
            ENTITY_TYPES.register("mb_jesselton_williams", () -> EntityType.Builder
                    .of(MbJesseltonWilliams::new, MobCategory.MONSTER)
                    .sized(0.7F, 2.2F)
                    .fireImmune()
                    // A boss the player is meant to kite around a cell block has to stay
                    // rendered from further away than the mob that shares its arena.
                    .clientTrackingRange(16)
                    .build("mb_jesselton_williams"));

    // ── Sal Viento ────────────────────────────────────────────────────────────
    // Under Tides. Five trash mobs built as a rock-paper-scissors rather than a ladder —
    // unlike Mansfield's three, which are the same mob at three sizes, no two of these are
    // answered the same way:
    //
    //   Runner    outruns you            kill it or be caught
    //   Crawler   comes in numbers, low  clear it or be nibbled
    //   Reaper    hits hardest, slow     walk away and come back
    //   Piercer   hits nearly as hard, dies instantly   notice it first
    //   Spitter   outranges everything   break line of sight
    //
    // Then a miniboss and a boss, in minibosses/ and bosses/ respectively.

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
                    // It opens fire at 12 blocks, so it must still be tracked at 12.
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
     * Knee-high on purpose: at 0.7 it sits under a normal swing arc, so clearing a room of
     * these is a thing you have to choose to do rather than something that happens while you
     * fight what is behind them.
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
     * The boss, and the second immobile one in the mod. The box is sized to the model's own
     * geometry rather than scaled up by the renderer — see {@code SvBishopQuintus} — so what
     * you can see and what you can hit are the same shape. It is wide enough that placement
     * matters: it cannot be pushed out of a wall it overlaps, so it wants an open arena.
     */
    public static final RegistryObject<EntityType<SvBishopQuintus>> SV_BISHOP_QUINTUS =
            ENTITY_TYPES.register("sv_bishop_quintus", () -> EntityType.Builder
                    .of(SvBishopQuintus::new, MobCategory.MONSTER)
                    .sized(2.6F, 4.0F)
                    .fireImmune()
                    // It shoots to 28 blocks; it has to stay rendered at least that far out.
                    .clientTrackingRange(24)
                    .build("sv_bishop_quintus"));

    // ── Dorothy's Vision ──────────────────────────────────────────────────────

    /**
     * "Awaken". A floating boss with no behaviour yet — see {@link DvAwaken}. It inherited
     * Dorothy's Vision when the Failed Vision was removed, so
     * {@code DorothysTerminalBlock} is what summons it; the spawn egg still works.
     *
     * <p>6.75 is the model's own width at the 3x {@code DvAwakenRenderer.SCALE} draws it at,
     * rather than the placeholder box scaled up: the box is sized to the geometry so that
     * what you can see and what you can hit are the same shape. The two constants have to
     * move together — rescale the renderer and this stops being true.
     *
     * <p>Wide enough to matter for placement. It cannot be pushed out of a wall it overlaps
     * ({@code setNoGravity} plus a no-op {@code push}), so it wants an open arena rather
     * than a corridor.
     */
    public static final RegistryObject<EntityType<DvAwaken>> DV_AWAKEN =
            ENTITY_TYPES.register("dv_awaken", () -> EntityType.Builder
                    .of(DvAwaken::new, MobCategory.MONSTER)
                    .sized(6.75F, 6.75F)
                    .fireImmune()
                    .clientTrackingRange(16)
                    .build("dv_awaken"));

    // ── Attributes ────────────────────────────────────────────────────────────

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

    // ── Spawn placements ──────────────────────────────────────────────────────
    // There are none, and so there is no FMLCommonSetupEvent handler here at all.
    //
    // Nothing in the mod spawns naturally any more. The slug was the last one that did — it
    // had an ON_GROUND rule with Monster::checkAnyLightMonsterSpawnRules, deliberately not
    // checkMonsterSpawnRules, because it was meant to be a daylight problem — and that went
    // when the slug was stripped back. Every mob is now placed by a structure or reached
    // through a spawn egg.
    //
    // Putting one back is two halves that must both be present: a SpawnPlacements.register
    // call in a method subscribed to FMLCommonSetupEvent (wrapped in event.enqueueWork —
    // placement state is not thread safe and setup runs in parallel), *and* a spawner entry
    // in ModBiomes. Either one alone does nothing.
    //
    // docs/SPAWNING.md walks through both, and has the exact code the slug used.

    public static void register(IEventBus eventBus) {
        ENTITY_TYPES.register(eventBus);
    }

    private ModEntities() {
    }
}
