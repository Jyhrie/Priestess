package com.jyhrie.priestess.world.dimension;

import com.jyhrie.priestess.Priestess;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacementType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

/**
 * The one structure placement type this mod adds.
 *
 * <p>Like the Terra biome source and the two density functions, this is a <em>runtime</em>
 * registration standing behind <em>datapack</em> content: the structure sets that use it are
 * JSON written by {@code runData}, and that JSON cannot be parsed until a codec is sitting
 * in this registry under the name it references. Registering it in code is not an exception
 * to "worldgen is data" — it is what makes the data loadable.
 *
 * @see SingleInRegionStructurePlacement
 */
public class ModStructurePlacements {

    public static final DeferredRegister<StructurePlacementType<?>> STRUCTURE_PLACEMENTS =
            DeferredRegister.create(Registries.STRUCTURE_PLACEMENT, Priestess.MOD_ID);

    /**
     * {@code "type": "priestess:single_in_region"} in a structure set — one structure, one
     * world, somewhere inside one named region of the Terra map.
     */
    public static final RegistryObject<StructurePlacementType<SingleInRegionStructurePlacement>> SINGLE_IN_REGION =
            STRUCTURE_PLACEMENTS.register("single_in_region",
                    () -> () -> SingleInRegionStructurePlacement.CODEC);

    public static void register(IEventBus eventBus) {
        STRUCTURE_PLACEMENTS.register(eventBus);
    }

    private ModStructurePlacements() {
    }
}
