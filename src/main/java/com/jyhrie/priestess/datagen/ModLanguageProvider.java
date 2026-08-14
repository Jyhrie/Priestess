package com.jyhrie.priestess.datagen;

import com.jyhrie.priestess.Priestess;
import com.jyhrie.priestess.block.ModBlocks;
import com.jyhrie.priestess.entity.ModEntities;
import com.jyhrie.priestess.item.ModItems;
import com.jyhrie.priestess.weapons.ModWeapons;
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
        add(ModEntities.DV_FAILURE.get(), "Failure");
        add(ModEntities.DV_REPLICA.get(), "Replica");
        add(ModEntities.DV_BIONIC.get(), "Bionic");
        add(ModEntities.MB_IMPRISONED_PUGILIST.get(), "Imprisoned Pugilist");
        add(ModEntities.MB_IMPRISONED_RECIDIVIST.get(), "Imprisoned Recidivist");
        add(ModEntities.MB_IMPRISONED_SNIPER.get(), "Imprisoned Sniper");
        add(ModEntities.SV_RUNNER.get(), "Runner");
        add(ModEntities.SV_SPITTER.get(), "Spitter");
        add(ModEntities.SV_REAPER.get(), "Reaper");
        add(ModEntities.SV_CRAWLER.get(), "Crawler");
        add(ModEntities.SV_PIERCER.get(), "Piercer");
        add(ModEntities.SV_THE_FIRST_TO_TALK.get(), "The First to Talk");
        // The prefix already carries "Sal Viento" in the id; the display name spells it out,
        // because a player reading a boss bar has never seen the id.
        add(ModEntities.SV_BISHOP_QUINTUS.get(), "Sal Viento Bishop Quintus");
        add(ModEntities.MB_JESSELTON_WILLIAMS.get(), "Jesselton Williams");
        // The quotes are part of the name, not punctuation around it — it is called
        // "Awaken", quotation marks and all, everywhere it is written.
        add(ModEntities.DV_AWAKEN.get(), "\"Awaken\"");

        // ── Items ─────────────────────────────────────────────────────────────
        add(ModItems.MANSFIELD_MASTER_KEY.get(), "Mansfield Master Key");
        add(ModItems.DOROTHYS_NEURAL_PROCESSOR.get(), "Dorothy's Neural Processor");
        add(ModItems.BLUEPRINT_ORIGINIUM_REFINEMENT.get(), "Blueprint: Originium Refinement");
        add(ModItems.TARNISHED_DOG_TAGS.get(), "Tarnished Dog Tags");
        add(ModItems.CORRUPTED_NEURAL_SHARD.get(), "Corrupted Neural Shard");
        add(ModItems.MEDIUM.get(), "Medium");
        add(ModItems.DREAMLAND.get(), "Dreamland");
        add(ModItems.TEMPLATE.get(), "Template");

        // ── Modules ───────────────────────────────────────────────────────────
        // Curios ships these two keys for its own slot types but cannot know about ours.
        // "identifier" labels the slot in the GUI; "modifiers" is the header above the
        // attribute list in a worn item's tooltip. Missing either shows the raw key on screen.
        // Both live in the curios namespace, like the slot's item tag, because Curios is what
        // looks them up. See docs/CURIOS.md.
        add("curios.identifier.module", "Module");
        add("curios.modifiers.module", "When worn as a module:");

        add(ModItems.ORIGINIUM_SLUG_SPAWN_EGG.get(), "Originium Slug Spawn Egg");
        add(ModItems.DV_FAILURE_SPAWN_EGG.get(), "Failure Spawn Egg");
        add(ModItems.DV_REPLICA_SPAWN_EGG.get(), "Replica Spawn Egg");
        add(ModItems.DV_BIONIC_SPAWN_EGG.get(), "Bionic Spawn Egg");
        add(ModItems.MB_IMPRISONED_PUGILIST_SPAWN_EGG.get(), "Imprisoned Pugilist Spawn Egg");
        add(ModItems.MB_IMPRISONED_RECIDIVIST_SPAWN_EGG.get(), "Imprisoned Recidivist Spawn Egg");
        add(ModItems.MB_IMPRISONED_SNIPER_SPAWN_EGG.get(), "Imprisoned Sniper Spawn Egg");
        add(ModItems.MB_JESSELTON_WILLIAMS_SPAWN_EGG.get(), "Jesselton Williams Spawn Egg");
        add(ModItems.SV_RUNNER_SPAWN_EGG.get(), "Runner Spawn Egg");
        add(ModItems.SV_SPITTER_SPAWN_EGG.get(), "Spitter Spawn Egg");
        add(ModItems.SV_REAPER_SPAWN_EGG.get(), "Reaper Spawn Egg");
        add(ModItems.SV_CRAWLER_SPAWN_EGG.get(), "Crawler Spawn Egg");
        add(ModItems.SV_PIERCER_SPAWN_EGG.get(), "Piercer Spawn Egg");
        add(ModItems.SV_THE_FIRST_TO_TALK_SPAWN_EGG.get(), "The First to Talk Spawn Egg");
        add(ModItems.SV_BISHOP_QUINTUS_SPAWN_EGG.get(), "Sal Viento Bishop Quintus Spawn Egg");
        add(ModItems.DV_AWAKEN_SPAWN_EGG.get(), "\"Awaken\" Spawn Egg");

        // ── Weapons ───────────────────────────────────────────────────────────
        // Ported in — see docs/LETHALITY WEAPONS.md. The name is painted over by the item's
        // own animated gradient, so this string supplies the letters and never the colour.
        add(ModWeapons.DEVILS_DEVASTATION.get(), "Devil's Devastation");

        // §-codes are Lethality's own styling, kept as written. The prompt says Shift because
        // the item checks Shift; Lethality's said Ctrl, which was a typo against its own code.
        add("tooltip.priestess.hold_shift", "§7Hold §fShift§7 for details");
        add("tooltip.priestess.devils_devastation.flavour", "§8« ...And you shall burn them all. »");
        add("tooltip.priestess.devils_devastation.on_swing", "§6On Swing —");
        add("tooltip.priestess.devils_devastation.on_swing_detail",
                "§e -Throws three piercing scythes and two pitchforks that set targets alight.");
        add("tooltip.priestess.devils_devastation.on_hit", "§6On Hit —");
        add("tooltip.priestess.devils_devastation.on_hit_detail", "§e -Sets the target alight.");

        // Laevatain. Not ported — original, and the one weapon in the mod whose three click
        // inputs are three named abilities, so the tooltip names all three. The §-codes follow
        // the convention set above: §6 for the heading, §e for the detail, §8 for flavour.
        add(ModWeapons.LAEVATAIN.get(), "Laevatain");
        add("tooltip.priestess.laevatain.flavour", "§8« Everything burns. I only decide when. »");
        add("tooltip.priestess.laevatain.left", "§6Laevatain §7— Left Click");
        add("tooltip.priestess.laevatain.left_detail",
                "§e -A wide sweep in front, burning everything it catches.");
        add("tooltip.priestess.laevatain.right", "§6Molten Giant §7— Hold Right Click §8(3s)");
        add("tooltip.priestess.laevatain.right_detail",
                "§e -Charge, then erupt a 2x2x5 wall of flame ahead. The whole line burns at once.");
        add("tooltip.priestess.laevatain.shift_right", "§6Twilight §7— Hold Shift + Right Click §8(10s)");
        add("tooltip.priestess.laevatain.shift_right_detail",
                "§e -Charge, then a 60° cone 10 blocks deep. Fire erupts beneath every mob caught.");

        // Aegir Greatspear. Original, and the counterpart to Laevatain: three named abilities on
        // the same three inputs, all of which pull rather than burn. Same §-code convention.
        add(ModWeapons.AEGIR_GREATSPEAR.get(), "Aegir Greatspear");
        add("tooltip.priestess.aegir_greatspear.flavour", "§8« The sea does not chase. It waits, and takes. »");
        add("tooltip.priestess.aegir_greatspear.left", "§Waterless Parting of the Great Ocean §7— Left Click");
        add("tooltip.priestess.aegir_greatspear.left_detail",
                "§b -Throws a lance of water. Whatever it strikes is dragged toward you.");
        add("tooltip.priestess.aegir_greatspear.right", "§Waterless Grasp of the Raging Seas §7— Right Click §8(10s)");
        add("tooltip.priestess.aegir_greatspear.right_detail",
                "§b -A 5x5x5 surge ahead. Everything inside it is hauled to your feet.");
        add("tooltip.priestess.aegir_greatspear.shift_right", "§Waterless Dance of the Shattered Maelstrom §7— Shift + Right Click §8(30s)");
        add("tooltip.priestess.aegir_greatspear.shift_right_detail",
                "§b -Opens a whirlpool where you aim. It pulls for 8 seconds and grinds for 5 a second.");

        // ── Blocks ────────────────────────────────────────────────────────────
        add(ModBlocks.IBERIAN_SAND.get(), "Iberian Sand");
        add(ModBlocks.IBERIAN_SANDSTONE.get(), "Iberian Sandstone");
        add(ModBlocks.SIESTA_SAND.get(), "Siesta Sand");
        add(ModBlocks.BLACK_ICE.get(), "Black Ice");
        add(ModBlocks.PALE_BEACH_SAND.get(), "Pale Beach Sand");
        add(ModBlocks.DEAD_SEABED.get(), "Dead Seabed");
        add(ModBlocks.PERMAFROST.get(), "Permafrost");

        add(ModBlocks.WHITEFLOWER.get(), "Whiteflower");
        add(ModBlocks.WHITEFLOWER_PETALS.get(), "Whiteflower Petals");
        // No item ever carries this name — the potted block has no BlockItem — but vanilla
        // names its potted blocks too, and a missing key is what a debug screen shows you.
        add(ModBlocks.POTTED_WHITEFLOWER.get(), "Potted Whiteflower");

        add(ModBlocks.JESSELTON_PROJECTOR.get(), "Jesselton's Projector");
        add(ModBlocks.DOROTHYS_TERMINAL.get(), "Dorothy's Terminal");

        add(ModBlocks.RHINE_LAB_ARTS_LAB_CHISELED_WALL.get(), "Rhine Lab Arts Lab Chiseled Wall");
        add(ModBlocks.RHINE_LAB_ARTS_LAB_PLATED_WALL.get(), "Rhine Lab Arts Lab Plated Wall");
        add(ModBlocks.RHINE_LAB_ARTS_LAB_CONCRETE_WALL.get(), "Rhine Lab Arts Lab Concrete Wall");
        add(ModBlocks.RHINE_LAB_ARTS_LAB_TILE.get(), "Rhine Lab Arts Lab Tile");
        add(ModBlocks.RHINE_LAB_ARTS_LAB_PILLAR.get(), "Rhine Lab Arts Lab Pillar");

        add(ModBlocks.SAL_VIENTO_CATACOMBS_STONE.get(), "Sal Viento Catacombs Stone");
        add(ModBlocks.SAL_VIENTO_CATACOMBS_OVERGROWN_STONE.get(), "Sal Viento Catacombs Overgrown Stone");
        add(ModBlocks.SAL_VIENTO_CATACOMBS_PIPE.get(), "Sal Viento Catacombs Pipe");

        add(ModBlocks.RMA70_12_DECORATIVE_PIPE.get(), "RMA70-12 Decorative Pipe");
        add(ModBlocks.RMA70_12_DECORATIVE_VENT.get(), "RMA70-12 Decorative Vent");
        add(ModBlocks.RMA70_24_DECORATIVE_PIPE.get(), "RMA70-24 Decorative Pipe");
        add(ModBlocks.RMA70_24_DECORATIVE_VENT.get(), "RMA70-24 Decorative Vent");
        add(ModBlocks.D32_STEEL_DECORATIVE_PIPE.get(), "D32 Steel Decorative Pipe");
        add(ModBlocks.D32_STEEL_DECORATIVE_VENT.get(), "D32 Steel Decorative Vent");
        add(ModBlocks.IRIDESCENT_ALLOY_DECORATIVE_PIPE.get(), "Iridescent Alloy Decorative Pipe");
        add(ModBlocks.IRIDESCENT_ALLOY_DECORATIVE_VENT.get(), "Iridescent Alloy Decorative Vent");

        // ── Dungeons ──────────────────────────────────────────────────────────
        // Names for the dungeons themselves, used by the lockdown and clear messages. The
        // key is Dungeon.getSerializedName(), so these must track that enum.
        add("dungeon.priestess.mansfield_break", "Mansfield State Prison");
        add("dungeon.priestess.dorothys_vision", "Dorothy's Vision");
        add("dungeon.priestess.rhine_lab", "Rhine Lab HQ");
        add("dungeon.priestess.under_tides", "Under Tides");

        // Action-bar lines. %s is the dungeon. Phrased about the block rather than the place,
        // because a gated block can be standing anywhere — including a player's own basement.
        add("message.priestess.dungeon.sealed_block", "This will not give until %s is cleared.");
        add("message.priestess.dungeon.cleared", "%s is open.");
        add("message.priestess.flight.grounded", "Something here will not let you leave the ground.");

        // ── Boss summoners ────────────────────────────────────────────────────
        // Action-bar lines, so a refused summon says why without opening the chat log.
        // %s is the boss for "spent" and the required item for "wrong_item".
        add("message.priestess.summoner.spent", "%s is already abroad.");
        add("message.priestess.summoner.wrong_item", "It wants %s.");
        add("message.priestess.summoner.no_room", "There is not enough room here.");
    }
}
