package com.jyhrie.priestess.entity;

import com.jyhrie.priestess.Priestess;
import com.jyhrie.priestess.config.BossStats;
import com.jyhrie.priestess.config.MinibossStats;
import com.jyhrie.priestess.config.MobStats;
import com.jyhrie.priestess.config.Stats;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;

/**
 * Puts the balance configs' numbers onto every mob and boss in the mod, as it joins the world.
 *
 * <p>Three files feed this — {@link BossStats}, {@link MinibossStats} and {@link MobStats},
 * under {@code config/priestess/}. They are separate configs because they are tuned at separate
 * times, but they are one mechanism from here: each hands over a {@link Stats.Block}, and a
 * Block knows both how to write itself onto an entity and whether its own file has been read.
 *
 * <h2>Why it is not done where the attributes are declared</h2>
 * Because it cannot be. An {@code AttributeSupplier} is built <b>once</b> during mod loading,
 * from the {@code attributes()} method on each mob class, and handed to the entity type. Reading
 * the config there would be a race against config loading — and, worse, would bake the answer in
 * for the process's lifetime, so editing a file would need a restart to take.
 *
 * <p>So the {@code attributes()} methods keep their numbers and stay the honest answer to "what
 * is this mob without a config", and this overwrites the six of them per entity afterwards. Two
 * copies of every number is the cost, and it is worth naming: <b>changing a number in an
 * {@code attributes()} method alone will not change the game</b>, because this runs after it.
 * The defaults in the config classes are the ones that decide anything.
 *
 * <h2>Why {@code EntityJoinLevelEvent}</h2>
 * It is the one point every entity passes through, however it got there. A spawn egg, a
 * {@code /summon}, a structure placing a boss directly — which never calls {@code finalizeSpawn},
 * the trap {@code MbJesseltonWilliams} documents against — and a chunk being loaded off disk all
 * arrive here, and all of them arrive <em>after</em> NBT has been read. That last part is what
 * makes the config authoritative rather than advisory: an attribute base value saved with a mob
 * would otherwise win, and editing a file would do nothing for any mob that already exists.
 *
 * <p>The exception is anything applied as a <em>modifier</em> rather than a base value, which
 * survives untouched. That is deliberate, and it is why {@code SvTheFirstToTalk} enrages with a
 * modifier: a mob's own runtime state has to outlive this.
 *
 * <h2>Adding a mob</h2>
 * A {@code Block} in whichever of the three config classes matches its tier, and a line in
 * {@link #blocks()}. A mob with no entry here simply keeps whatever its {@code attributes()}
 * method gave it, which is a working mob and not a broken one — so forgetting costs a missing
 * config section and nothing else.
 */
@Mod.EventBusSubscriber(modid = Priestess.MOD_ID)
public final class EntityStats {

    /**
     * Built on first use rather than in a static initialiser, because the {@code EntityType}
     * keys come out of {@code DeferredRegister} suppliers that cannot be resolved until
     * registration has run.
     */
    private static Map<EntityType<?>, Stats.Block> blocks;

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        // Server only. Attributes are the server's to decide, and it syncs the ones the client
        // needs on its own the moment the entity starts being tracked.
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof LivingEntity living)) {
            return;
        }

        Stats.Block block = blocks().get(living.getType());
        if (block != null) {
            // No config-loaded check here: the three files load independently, so the question
            // is per-Block rather than global, and applyTo answers it for its own file.
            block.applyTo(living);
        }
    }

    private static Map<EntityType<?>, Stats.Block> blocks() {
        if (blocks != null) {
            return blocks;
        }

        Map<EntityType<?>, Stats.Block> map = new HashMap<>();

        map.put(ModEntities.DV_AWAKEN.get(), BossStats.DV_AWAKEN);
        map.put(ModEntities.MB_JESSELTON_WILLIAMS.get(), BossStats.MB_JESSELTON_WILLIAMS);
        map.put(ModEntities.SV_BISHOP_QUINTUS.get(), BossStats.SV_BISHOP_QUINTUS);

        map.put(ModEntities.SV_THE_FIRST_TO_TALK.get(), MinibossStats.SV_THE_FIRST_TO_TALK);

        map.put(ModEntities.ORIGINIUM_SLUG.get(), MobStats.ORIGINIUM_SLUG);

        map.put(ModEntities.DV_FAILURE.get(), MobStats.DV_FAILURE);
        map.put(ModEntities.DV_REPLICA.get(), MobStats.DV_REPLICA);
        map.put(ModEntities.DV_BIONIC.get(), MobStats.DV_BIONIC);

        map.put(ModEntities.MB_IMPRISONED_PUGILIST.get(), MobStats.MB_IMPRISONED_PUGILIST);
        map.put(ModEntities.MB_IMPRISONED_RECIDIVIST.get(), MobStats.MB_IMPRISONED_RECIDIVIST);
        map.put(ModEntities.MB_IMPRISONED_SNIPER.get(), MobStats.MB_IMPRISONED_SNIPER);

        map.put(ModEntities.SV_CRAWLER.get(), MobStats.SV_CRAWLER);
        map.put(ModEntities.SV_RUNNER.get(), MobStats.SV_RUNNER);
        map.put(ModEntities.SV_PIERCER.get(), MobStats.SV_PIERCER);
        map.put(ModEntities.SV_REAPER.get(), MobStats.SV_REAPER);
        map.put(ModEntities.SV_SPITTER.get(), MobStats.SV_SPITTER);

        blocks = map;
        return blocks;
    }

    private EntityStats() {
    }
}
