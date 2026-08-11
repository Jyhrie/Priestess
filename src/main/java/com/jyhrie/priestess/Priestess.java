package com.jyhrie.priestess;

import com.jyhrie.priestess.block.ModBlocks;
import com.jyhrie.priestess.block.entity.ModBlockEntities;
import com.jyhrie.priestess.effect.ModEffects;
import com.jyhrie.priestess.entity.ModEntities;
import com.jyhrie.priestess.item.ModCreativeTabs;
import com.jyhrie.priestess.item.ModItems;
import com.jyhrie.priestess.oripathy.OripathyEvents;
import com.jyhrie.priestess.progression.DungeonSync;
import com.jyhrie.priestess.world.dimension.AnchorReport;
import com.jyhrie.priestess.world.dimension.ModStructurePlacements;
import com.jyhrie.priestess.world.terra.TerraElevationFunction;
import com.jyhrie.priestess.world.terra.TerraMapBiomeSource;
import com.jyhrie.priestess.world.terra.TerraReliefFunction;
import com.mojang.serialization.Codec;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;

@Mod(Priestess.MOD_ID)
public class Priestess {

    public static final String MOD_ID = "priestess";

    // The dimension, biomes, noise settings and structures are datapack JSON generated
    // by `gradlew runData` — see README.md. These two are the exception: a codec has to
    // exist in the registry *before* that JSON can be read back, so the worldgen types
    // Terra defines are registered in code even though everything using them is data.
    private static final DeferredRegister<Codec<? extends BiomeSource>> BIOME_SOURCES =
            DeferredRegister.create(Registries.BIOME_SOURCE, MOD_ID);

    private static final DeferredRegister<Codec<? extends DensityFunction>> DENSITY_FUNCTIONS =
            DeferredRegister.create(Registries.DENSITY_FUNCTION_TYPE, MOD_ID);

    static {
        BIOME_SOURCES.register("terra_map", () -> TerraMapBiomeSource.CODEC);
        // The registry stores a plain Codec, while DensityFunction.codec() hands back a
        // KeyDispatchDataCodec, so unwrap it — the same thing vanilla's
        // DensityFunctions.register does.
        DENSITY_FUNCTIONS.register("terra_elevation",
                () -> TerraElevationFunction.CODEC.codec());
        DENSITY_FUNCTIONS.register("terra_relief",
                () -> TerraReliefFunction.CODEC.codec());
    }

    public Priestess() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        ModItems.register(modEventBus);
        ModBlocks.register(modEventBus);
        ModBlockEntities.register(modEventBus);
        ModCreativeTabs.register(modEventBus);
        ModEffects.register(modEventBus);
        ModEntities.register(modEventBus);

        BIOME_SOURCES.register(modEventBus);
        DENSITY_FUNCTIONS.register(modEventBus);
        // Same reason as the two above: the structure-set JSON names a placement type, and
        // the codec for it has to be in the registry before that JSON can be parsed.
        ModStructurePlacements.register(modEventBus);

        // Oripathy: the capability type is a mod-bus registration, everything that acts on
        // it (attaching it to players, symptoms, the /oripathy command) is a Forge-bus one.
        modEventBus.addListener(OripathyEvents::registerCapabilities);
        MinecraftForge.EVENT_BUS.register(OripathyEvents.class);

        // Reports where this world put its one-per-world dungeons, to operators on login.
        MinecraftForge.EVENT_BUS.register(AnchorReport.class);

        // Progression. Both mechanics read the config, so it has to exist before either can
        // run; the handlers themselves are @Mod.EventBusSubscriber and register themselves.
        // SERVER type, so it lives in the world save rather than the installation — see
        // PriestessConfig.
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, PriestessConfig.SPEC);

        // The lockdown's client half. Without this the client predicts a break the server is
        // going to refuse, and a sealed block shatters and comes back rather than not moving.
        DungeonSync.register();
    }

}
