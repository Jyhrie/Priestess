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
 * <p>It cannot be done where the attributes are declared: an {@code AttributeSupplier} is built
 * once during mod loading, which races config load and then freezes for the process. So the
 * {@code attributes()} methods keep their numbers as the honest answer to "what is this mob
 * without a config", and this overwrites them per entity afterwards. The cost is two copies of
 * every number, and the trap worth naming: <b>changing a number in an {@code attributes()}
 * method alone will not change the game.</b>
 *
 * <p>{@code EntityJoinLevelEvent} is the one point every entity passes through — spawn egg,
 * {@code /summon}, a structure placing a boss directly (which never calls
 * {@code finalizeSpawn}), or a chunk loaded off disk — and all of them arrive <em>after</em>
 * NBT is read. That is what makes the config authoritative rather than advisory.
 *
 * <p>Anything applied as a <em>modifier</em> rather than a base value survives untouched, which
 * is why {@code SvTheFirstToTalk} enrages with one: a mob's runtime state has to outlive this.
 *
 * <p>To add a mob: a {@code Block} in whichever config class matches its tier, and a line in
 * {@link #blocks()}. A mob with no entry keeps whatever its {@code attributes()} method gave
 * it, so forgetting costs a missing config section and nothing else.
 */
@Mod.EventBusSubscriber(modid = Priestess.MOD_ID)
public final class EntityStats {

    /**
     * Built on first use rather than in a static initialiser, because the {@code EntityType}
     * keys come from {@code DeferredRegister} suppliers that cannot resolve until registration
     * has run.
     */
    private static Map<EntityType<?>, Stats.Block> blocks;

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        // Server only: it syncs the attributes the client needs when tracking starts.
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof LivingEntity living)) {
            return;
        }

        Stats.Block block = blocks().get(living.getType());
        if (block != null) {
            // No config-loaded check: the three files load independently, so the question is
            // per-Block, and applyTo answers it for its own file.
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
