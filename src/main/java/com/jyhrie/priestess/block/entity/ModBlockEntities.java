package com.jyhrie.priestess.block.entity;

import com.jyhrie.priestess.Priestess;
import com.jyhrie.priestess.block.ModBlocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Block entities. There is one, and it is shared by every boss summoner.
 *
 * <p>One type covering both blocks rather than one each: the block entity stores a UUID and
 * a countdown and nothing else, so which boss it belongs to is already answered by the block
 * it sits under. A type per summoner would be two registrations that differ in no way.
 */
public class ModBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, Priestess.MOD_ID);

    public static final RegistryObject<BlockEntityType<BossSummonerBlockEntity>> BOSS_SUMMONER =
            BLOCK_ENTITIES.register("boss_summoner", () -> BlockEntityType.Builder
                    .of(BossSummonerBlockEntity::new,
                            ModBlocks.JESSELTON_PROJECTOR.get(),
                            ModBlocks.DOROTHYS_TERMINAL.get())
                    // The datafixer type. Null is what every mod passes and what vanilla
                    // tolerates; there is no schema to migrate against outside Mojang's own.
                    .build(null));

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }

    private ModBlockEntities() {
    }
}
