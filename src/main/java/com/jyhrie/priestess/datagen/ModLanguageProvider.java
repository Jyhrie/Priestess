package com.jyhrie.priestess.datagen;

import com.jyhrie.priestess.Priestess;
import com.jyhrie.priestess.block.ModBlocks;
import com.jyhrie.priestess.entity.ModEntities;
import com.jyhrie.priestess.item.ModItems;
import net.minecraft.data.PackOutput;
import net.minecraftforge.common.data.LanguageProvider;

public class ModLanguageProvider extends LanguageProvider {
    public ModLanguageProvider(PackOutput output) {
        super(output, Priestess.MOD_ID, "en_us");
    }

    @Override
    protected void addTranslations() {
        add("creativetab.priestess_tab", "Priestess");

        // Death messages. The key comes from the DamageType's message id, not from its
        // registry name. Every type needs a plain form and, where a mob can cause it, a
        // ".player" form — vanilla falls back to the plain one, so a missing .player is a
        // sentence that never names the ghost that killed you.
        add("death.attack.oripathy", "%1$s was crystallised by Oripathy");
        add("death.attack.spectral_arts", "%1$s was cut down by spectral Arts");
        add("death.attack.spectral_arts.player", "%1$s was cut down by %2$s");
        add("death.attack.void_arts", "%1$s was unmade by Void Arts");
        add("death.attack.void_arts.player", "%1$s was unmade by %2$s");
        add("death.attack.originium_acid", "%1$s dissolved in Originium acid");
        add("death.attack.originium_acid.player", "%1$s was dissolved by %2$s");
        add("death.attack.rhine_laser", "%1$s was cooked by a Rhine security beam");
        add("death.attack.rhine_laser.player", "%1$s was cooked by %2$s");

        add("effect.priestess.open_wounds", "Open Wounds");
        add("effect.priestess.acute_oripathy", "Acute Oripathy");

        // ── Mobs ──────────────────────────────────────────────────────────────
        add(ModEntities.ORIGINIUM_SLUG.get(), "Originium Slug");
        add(ModEntities.JESSELTON_WILLIAMS.get(), "Jesselton Williams");
        // The quotes are part of the name, not punctuation around it — it is called
        // "Awaken", quotation marks and all, everywhere it is written.
        add(ModEntities.AWAKEN.get(), "\"Awaken\"");

        // ── Items ─────────────────────────────────────────────────────────────
        add(ModItems.MANSFIELD_MASTER_KEY.get(), "Mansfield Master Key");
        add(ModItems.DOROTHYS_NEURAL_PROCESSOR.get(), "Dorothy's Neural Processor");
        add(ModItems.BLUEPRINT_ORIGINIUM_REFINEMENT.get(), "Blueprint: Originium Refinement");
        add(ModItems.TARNISHED_DOG_TAGS.get(), "Tarnished Dog Tags");
        add(ModItems.CORRUPTED_NEURAL_SHARD.get(), "Corrupted Neural Shard");

        add(ModItems.ORIGINIUM_SLUG_SPAWN_EGG.get(), "Originium Slug Spawn Egg");
        add(ModItems.JESSELTON_WILLIAMS_SPAWN_EGG.get(), "Jesselton Williams Spawn Egg");
        add(ModItems.AWAKEN_SPAWN_EGG.get(), "\"Awaken\" Spawn Egg");

        // ── Blocks ────────────────────────────────────────────────────────────
        add(ModBlocks.IBERIAN_SAND.get(), "Iberian Sand");
        add(ModBlocks.IBERIAN_SANDSTONE.get(), "Iberian Sandstone");
        add(ModBlocks.SIESTA_SAND.get(), "Siesta Sand");
        add(ModBlocks.BLACK_ICE.get(), "Black Ice");
        add(ModBlocks.PALE_BEACH_SAND.get(), "Pale Beach Sand");
        add(ModBlocks.DEAD_SEABED.get(), "Dead Seabed");
        add(ModBlocks.PERMAFROST.get(), "Permafrost");
        add(ModBlocks.JESSELTON_PROJECTOR.get(), "Jesselton's Projector");
        add(ModBlocks.DOROTHYS_TERMINAL.get(), "Dorothy's Terminal");

        // ── Boss summoners ────────────────────────────────────────────────────
        // Action-bar lines, so a refused summon says why without opening the chat log.
        // %s is the boss for "spent" and the required item for "wrong_item".
        add("message.priestess.summoner.spent", "%s is already abroad.");
        add("message.priestess.summoner.wrong_item", "It wants %s.");
        add("message.priestess.summoner.no_room", "There is not enough room here.");
    }
}
