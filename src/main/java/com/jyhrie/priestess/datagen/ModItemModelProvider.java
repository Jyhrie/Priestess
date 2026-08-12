package com.jyhrie.priestess.datagen;

import com.jyhrie.priestess.Priestess;
import com.jyhrie.priestess.item.ModItems;
import com.jyhrie.priestess.weapons.ModWeapons;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraftforge.client.model.generators.ItemModelBuilder;
import net.minecraftforge.client.model.generators.ItemModelProvider;
import net.minecraftforge.client.model.generators.loaders.SeparateTransformsModelBuilder;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.registries.RegistryObject;

public class ModItemModelProvider extends ItemModelProvider {

    public ModItemModelProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, Priestess.MOD_ID, existingFileHelper);
    }

    @Override
    protected void registerModels() {
        // Standalone items only. Block models come from ModBlockStateProvider's
        // simpleBlockWithItem(). See "Adding an item" in README.md.

        basicItem(ModItems.MANSFIELD_MASTER_KEY.get());
        basicItem(ModItems.DOROTHYS_NEURAL_PROCESSOR.get());
        basicItem(ModItems.BLUEPRINT_ORIGINIUM_REFINEMENT.get());
        basicItem(ModItems.TARNISHED_DOG_TAGS.get());
        basicItem(ModItems.CORRUPTED_NEURAL_SHARD.get());
        basicItem(ModItems.MEDIUM.get());
        basicItem(ModItems.DREAMLAND.get());

        // Ported weapons — see docs/LETHALITY WEAPONS.md.
        //
        // Not basicItem(). These use two sprites and have to be held like a sword rather than
        // like a potato, neither of which item/generated does. See bigWeapon below.
        bigWeapon(ModWeapons.DEVILS_DEVASTATION);

        // Spawn eggs are the one item that needs no texture: the vanilla template tints two
        // greyscale layers from the colours passed to ForgeSpawnEggItem.
        spawnEgg(ModItems.ORIGINIUM_SLUG_SPAWN_EGG);
        spawnEgg(ModItems.DV_FAILURE_SPAWN_EGG);
        spawnEgg(ModItems.DV_REPLICA_SPAWN_EGG);
        spawnEgg(ModItems.DV_BIONIC_SPAWN_EGG);
        spawnEgg(ModItems.MB_IMPRISONED_PUGILIST_SPAWN_EGG);
        spawnEgg(ModItems.MB_IMPRISONED_RECIDIVIST_SPAWN_EGG);
        spawnEgg(ModItems.MB_IMPRISONED_SNIPER_SPAWN_EGG);
        spawnEgg(ModItems.MB_JESSELTON_WILLIAMS_SPAWN_EGG);
        spawnEgg(ModItems.SV_RUNNER_SPAWN_EGG);
        spawnEgg(ModItems.SV_SPITTER_SPAWN_EGG);
        spawnEgg(ModItems.SV_REAPER_SPAWN_EGG);
        spawnEgg(ModItems.SV_CRAWLER_SPAWN_EGG);
        spawnEgg(ModItems.SV_PIERCER_SPAWN_EGG);
        spawnEgg(ModItems.SV_THE_FIRST_TO_TALK_SPAWN_EGG);
        spawnEgg(ModItems.SV_BISHOP_QUINTUS_SPAWN_EGG);
        spawnEgg(ModItems.DV_AWAKEN_SPAWN_EGG);
    }

    private void spawnEgg(RegistryObject<? extends net.minecraft.world.item.Item> egg) {
        withExistingParent(egg.getId().getPath(), new ResourceLocation("item/template_spawn_egg"));
    }

    /**
     * A ported weapon whose blade sprite is bigger than an inventory slot.
     *
     * <p>These want two different things from one item, and vanilla's item model cannot do
     * both at once:
     *
     * <ul>
     *   <li><b>In hand</b> a 64×64 sprite, scaled up and angled so it reads as a greatsword.
     *       {@code item/generated} holds a sprite flat and upright like a carrot, so the parent
     *       has to be {@code item/handheld} and the transforms below have to be spelled out.</li>
     *   <li><b>In the inventory</b> a hand-drawn 16×16 sprite. Letting the GUI shrink the 64×64
     *       one instead is what makes the icon look like a photograph of a sword: the detail is
     *       all still there and none of it survives at slot size.</li>
     * </ul>
     *
     * <p>Forge's {@code separate_transforms} loader is what allows one item to answer with a
     * different model per perspective. {@code base} is used everywhere not listed; {@code GUI}
     * and {@code GROUND} — the two places the item is seen small and flat — get the small
     * sprite instead.
     *
     * <p>Requires two textures, both in {@code src/main/resources}:
     * {@code item/<name>.png} at 64×64 and {@code item/<name>_gui.png} at 16×16.
     *
     * <p>The transform numbers are Lethality's, carried over as tuned. They are the awkward
     * part of this model and there is nothing to derive them from — a greatsword that sits
     * correctly in the hand is somebody's afternoon in-game, not a formula.
     */
    private void bigWeapon(RegistryObject<? extends net.minecraft.world.item.Item> weapon) {
        String name = weapon.getId().getPath();
        ResourceLocation heldSprite = modLoc("item/" + name);
        ResourceLocation guiSprite = modLoc("item/" + name + "_gui");

        ItemModelBuilder small = nested()
                .parent(getExistingFile(mcLoc("item/generated")))
                // Belt and braces with the outer guiLight below — see the note there.
                .guiLight(BlockModel.GuiLight.FRONT)
                .texture("layer0", guiSprite);

        getBuilder(name)
                // Load-bearing, and the reason this is not inherited: gui_light resolves from
                // the OUTER model, and a separate_transforms model has no parent to inherit it
                // from. minecraft:item/generated sets front, but that is two levels down inside
                // a perspective and never consulted. Left unset the outer model defaults to
                // side, which lights a flat sprite as if from the edge and renders the hotbar
                // icon almost black.
                .guiLight(BlockModel.GuiLight.FRONT)
                .customLoader(SeparateTransformsModelBuilder::begin)
                .base(nested()
                        .parent(getExistingFile(mcLoc("item/handheld")))
                        .texture("layer0", heldSprite)
                        .transforms()
                        // Third person: held out from the body, well clear of the shoulder.
                        // The +20 on Y is what keeps a blade this long off the ground.
                        .transform(ItemDisplayContext.THIRD_PERSON_RIGHT_HAND)
                                .rotation(0, -90, 55).translation(0, 20, -1).scale(2.9F, 2.9F, 1.0F)
                                .end()
                        .transform(ItemDisplayContext.THIRD_PERSON_LEFT_HAND)
                                .rotation(0, 90, -55).translation(0, 20, -1).scale(2.9F, 2.9F, 1.0F)
                                .end()
                        // First person: smaller and flatter, or the blade fills the screen.
                        .transform(ItemDisplayContext.FIRST_PERSON_RIGHT_HAND)
                                .rotation(-15, -90, 25).translation(1.13F, 7.2F, 1.13F)
                                .scale(1.7F, 1.7F, 0.85F)
                                .end()
                        .transform(ItemDisplayContext.FIRST_PERSON_LEFT_HAND)
                                .rotation(-15, 90, -25).translation(1.13F, 7.2F, 1.13F)
                                .scale(1.7F, 1.7F, 0.85F)
                                .end()
                        .end())
                .perspective(ItemDisplayContext.GUI, small)
                .perspective(ItemDisplayContext.GROUND, small)
                .end();
    }
}
