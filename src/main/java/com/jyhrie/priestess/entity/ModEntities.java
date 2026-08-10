package com.jyhrie.priestess.entity;

import com.jyhrie.priestess.Priestess;
import com.jyhrie.priestess.entity.bosses.Awaken;
import com.jyhrie.priestess.entity.bosses.JesseltonWilliams;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
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
 *   <tr><td>Originium Slug</td><td>the open wastes</td><td>attrition, and a tax on leaving
 *       machines running unguarded</td></tr>
 *   <tr><td>Jesselton Williams</td><td>Mansfield</td><td>boss 1 — drops the Master Key</td></tr>
 *   <tr><td>Awaken</td><td>Dorothy's Vision</td><td>boss 2 — summoned by the terminal, no
 *       behaviour yet</td></tr>
 * </table>
 *
 * <h2>Adding a mob</h2>
 * <ol>
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
        event.put(JESSELTON_WILLIAMS.get(), JesseltonWilliams.attributes().build());
        event.put(AWAKEN.get(), Awaken.attributes().build());
    }

    /**
     * Only the slug spawns naturally; everything else is placed by a structure, because a
     * boss's adds wandering the wastes would give away the dungeon that owns them.
     *
     * <p>Deliberately <em>not</em> {@code Monster.checkMonsterSpawnRules} — that gates on
     * darkness, and Columbia's slugs are a daylight problem. What is kept is the
     * "not on the ceiling of a cave, not underwater" part.
     */
    @SubscribeEvent
    public static void registerSpawnPlacements(FMLCommonSetupEvent event) {
        // Spawn placement state is not thread safe and setup runs in parallel.
        event.enqueueWork(() -> SpawnPlacements.register(ORIGINIUM_SLUG.get(),
                SpawnPlacements.Type.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Monster::checkAnyLightMonsterSpawnRules));
    }

    public static void register(IEventBus eventBus) {
        ENTITY_TYPES.register(eventBus);
    }

    private ModEntities() {
    }
}
