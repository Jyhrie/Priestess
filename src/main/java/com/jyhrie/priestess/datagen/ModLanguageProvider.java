package com.jyhrie.priestess.datagen;

import com.jyhrie.priestess.Priestess;
import com.jyhrie.priestess.block.ModBlocks;
import net.minecraft.data.PackOutput;
import net.minecraftforge.common.data.LanguageProvider;

public class ModLanguageProvider extends LanguageProvider {
    public ModLanguageProvider(PackOutput output) {
        super(output, Priestess.MOD_ID, "en_us");
    }

    @Override
    protected void addTranslations() {
        add("creativetab.priestess_tab", "Priestess");

        // Death message for the priestess:oripathy damage type. The key comes from the
        // DamageType's message id, not from its registry name.
        add("death.attack.oripathy", "%1$s was crystallised by Oripathy");

        add("effect.priestess.open_wounds", "Open Wounds");
        add("effect.priestess.acute_oripathy", "Acute Oripathy");

        add(ModBlocks.IBERIAN_SAND.get(), "Iberian Sand");
        add(ModBlocks.IBERIAN_SANDSTONE.get(), "Iberian Sandstone");
        add(ModBlocks.SIESTA_SAND.get(), "Siesta Sand");
        add(ModBlocks.BLACK_ICE.get(), "Black Ice");
        add(ModBlocks.PALE_BEACH_SAND.get(), "Pale Beach Sand");
        add(ModBlocks.DEAD_SEABED.get(), "Dead Seabed");
        add(ModBlocks.PERMAFROST.get(), "Permafrost");
    }
}