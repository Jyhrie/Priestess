package com.jyhrie.priestess.datagen;

import com.jyhrie.priestess.Priestess;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.data.ExistingFileHelper;
import top.theillusivec4.curios.api.CuriosDataProvider;

import java.util.concurrent.CompletableFuture;

/**
 * Writes Curios' own datapack files, under {@code data/priestess/curios/}.
 *
 * <p>Curios ships definitions for its standard slot types but deliberately assigns none of
 * them to any entity: until some mod asks for a slot, that slot does not exist in the GUI at
 * all. This provider is that request. It is also where the Module slot itself is defined,
 * since Curios has no slot type resembling one.
 *
 * <p>Note we ask for nothing but Module. The mod uses none of Curios' built-in slots, so
 * naming them here would put empty ring and belt slots in the GUI of a player who has no
 * business seeing them.
 */
public class ModCuriosDataProvider extends CuriosDataProvider {

    public ModCuriosDataProvider(PackOutput output, ExistingFileHelper fileHelper,
                                 CompletableFuture<HolderLookup.Provider> registries) {
        super(Priestess.MOD_ID, output, fileHelper, registries);
    }

    @Override
    public void generate(HolderLookup.Provider registries, ExistingFileHelper fileHelper) {
        // The Module slot. "order" is its left-to-right position in the GUI and only matters
        // relative to other slots — 100 leaves room on both sides for later ones without
        // renumbering this. The "curios:tag" validator is what makes the slot accept exactly
        // the items in the curios:module item tag; without it the slot takes anything at all,
        // dirt included, and that failure looks like a slot that "works".
        this.createSlot("module")
                .order(100)
                .icon(new ResourceLocation(Priestess.MOD_ID, "slot/empty_module_slot"))
                .addValidator(new ResourceLocation("curios", "tag"));

        // Grant it to players. The file name is arbitrary — only the contents matter — and
        // addEntities(...) in place of addPlayer() is how a slot would be given to a mob.
        this.createEntities("module_wearers")
                .addPlayer()
                .addSlots("module");
    }
}
