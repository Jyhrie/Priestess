package com.jyhrie.priestess.entity;

import com.jyhrie.priestess.Priestess;
import com.jyhrie.priestess.entity.bosses.Awaken;
import com.jyhrie.priestess.entity.bosses.JesseltonWilliams;
import com.jyhrie.priestess.entity.mobs.Bionic;
import com.jyhrie.priestess.entity.mobs.Failure;
import com.jyhrie.priestess.entity.mobs.OriginiumSlug;
import com.jyhrie.priestess.entity.mobs.Replica;
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
 * Every mob the Columbia chapter puts in the world.
 *
 * <p>Unlike the dimension and its biomes, entities are <em>runtime</em> registrations — a
 * mob is code, not data, so there is no {@code runData} step for anything in this package.
 * What does still go through datagen is the biome spawn list ({@code ModBiomes}), the
 * language file, and the loot tables, so a new mob here is not finished until those three
 * know about it.
 *
 * <h2>Who belongs to which dungeon</h2>
 * <table border="1">
 *   <caption>The Columbia roster</caption>
 *   <tr><th>Mob</th><th>Where</th><th>What it is for</th></tr>
 *   <tr><td>Originium Slug</td><td>nowhere yet</td><td>a blank mob — every mechanic it had
 *       has been cut, pending replacements</td></tr>
 *   <tr><td>Failure</td><td>nowhere yet</td><td>melee — the swarm; drops Medium</td></tr>
 *   <tr><td>Replica</td><td>nowhere yet</td><td>melee — the baseline; drops Medium</td></tr>
 *   <tr><td>Bionic</td><td>nowhere yet</td><td>melee — the wall; drops Medium</td></tr>
 *   <tr><td>Jesselton Williams</td><td>Mansfield</td><td>boss 1 — drops the Master Key</td></tr>
 *   <tr><td>Awaken</td><td>Dorothy's Vision</td><td>boss 2 — summoned by the terminal, no
 *       attacks yet; drops Dreamland</td></tr>
 * </table>
 *
 * <p>"nowhere yet" is literal, and it is now true of four of the six: <b>nothing in this mod
 * spawns naturally any more.</b> The slug was the last one that did. Everything except the
 * two bosses — which a structure places — is reachable only through its spawn egg, and the
 * three Medium-bearers are waiting on a decision about which dungeon they belong to rather
 * than on code.
 *
 * <h2>How this package is laid out</h2>
 * The root holds only the registry and the base classes — {@link BossMonster} and
 * {@link GeoMonster}. Everything that <em>is</em> a mob lives one level down:
 * <ul>
 *   <li>{@code mobs/} — the trash mobs: the slug and the three Medium-bearers.</li>
 *   <li>{@code bosses/} — the two bosses.</li>
 *   <li>{@code projectiles/} — {@code ArtsBeam}, which is a static helper rather than an
 *       entity but is the mod's whole ranged-attack story, and the place a real projectile
 *       entity would go when one exists.</li>
 * </ul>
 * Packages are invisible to the game: a mob's registry name is the string passed to
 * {@code ENTITY_TYPES.register} below, and its assets are keyed off that name, not off where
 * the class sits. Moving a class between these folders is a rename-and-recompile, nothing
 * more.
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
    public static final RegistryObject<EntityType<Failure>> FAILURE =
            ENTITY_TYPES.register("failure", () -> EntityType.Builder
                    .of(Failure::new, MobCategory.MONSTER)
                    .sized(0.8F, 1.6F)
                    .clientTrackingRange(8)
                    .build("failure"));

    /** A plain two-block humanoid; the baseline the other two are read against. */
    public static final RegistryObject<EntityType<Replica>> REPLICA =
            ENTITY_TYPES.register("replica", () -> EntityType.Builder
                    .of(Replica::new, MobCategory.MONSTER)
                    .sized(0.7F, 1.95F)
                    .clientTrackingRange(8)
                    .build("replica"));

    /** Wide and tall enough to block a doorway, which is most of what it is for. */
    public static final RegistryObject<EntityType<Bionic>> BIONIC =
            ENTITY_TYPES.register("bionic", () -> EntityType.Builder
                    .of(Bionic::new, MobCategory.MONSTER)
                    .sized(0.9F, 2.1F)
                    .clientTrackingRange(10)
                    .build("bionic"));

    // ── Mansfield State Prison ────────────────────────────────────────────────

    public static final RegistryObject<EntityType<JesseltonWilliams>> JESSELTON_WILLIAMS =
            ENTITY_TYPES.register("jesselton_williams", () -> EntityType.Builder
                    .of(JesseltonWilliams::new, MobCategory.MONSTER)
                    .sized(0.7F, 2.2F)
                    .fireImmune()
                    // A boss the player is meant to kite around a cell block has to stay
                    // rendered from further away than the mob that shares its arena.
                    .clientTrackingRange(16)
                    .build("jesselton_williams"));

    // ── Dorothy's Vision ──────────────────────────────────────────────────────

    /**
     * "Awaken". A floating boss with no behaviour yet — see {@link Awaken}. It inherited
     * Dorothy's Vision when the Failed Vision was removed, so
     * {@code DorothysTerminalBlock} is what summons it; the spawn egg still works.
     *
     * <p>6.75 is the model's own width at the 3x {@code AwakenRenderer.SCALE} draws it at,
     * rather than the placeholder box scaled up: the box is sized to the geometry so that
     * what you can see and what you can hit are the same shape. The two constants have to
     * move together — rescale the renderer and this stops being true.
     *
     * <p>Wide enough to matter for placement. It cannot be pushed out of a wall it overlaps
     * ({@code setNoGravity} plus a no-op {@code push}), so it wants an open arena rather
     * than a corridor.
     */
    public static final RegistryObject<EntityType<Awaken>> AWAKEN =
            ENTITY_TYPES.register("awaken", () -> EntityType.Builder
                    .of(Awaken::new, MobCategory.MONSTER)
                    .sized(6.75F, 6.75F)
                    .fireImmune()
                    .clientTrackingRange(16)
                    .build("awaken"));

    // ── Attributes ────────────────────────────────────────────────────────────

    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(ORIGINIUM_SLUG.get(), OriginiumSlug.attributes().build());
        event.put(FAILURE.get(), Failure.attributes().build());
        event.put(REPLICA.get(), Replica.attributes().build());
        event.put(BIONIC.get(), Bionic.attributes().build());
        event.put(JESSELTON_WILLIAMS.get(), JesseltonWilliams.attributes().build());
        event.put(AWAKEN.get(), Awaken.attributes().build());
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
