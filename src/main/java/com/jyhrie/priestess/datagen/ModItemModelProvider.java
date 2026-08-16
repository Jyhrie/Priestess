package com.jyhrie.priestess.datagen;

import com.jyhrie.priestess.Priestess;
import com.jyhrie.priestess.item.ModItems;
import com.jyhrie.priestess.weapons.ModWeapons;
import com.jyhrie.priestess.weapons.item.TemplateWeaponItem;
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

        // Curios draws the worn item from this same model, so there is nothing extra for it.
        basicItem(ModItems.TEMPLATE.get());

        // The model lives beside the item in TemplateWeaponItem.Model, but this call is still
        // required: a builder only reaches disk if something registers it during
        // registerModels(). See docs/WEAPONS.md § 6.
        TemplateWeaponItem.Model.build(this);

        // Not basicItem(): these use two sprites and have to be held like a sword rather than
        // like a potato, neither of which item/generated does.
        bigWeapon(ModWeapons.DEVILS_DEVASTATION);
        chargedWeapon(ModWeapons.LAEVATAIN);
        // Plain bigWeapon rather than chargedWeapon: none of the spear's abilities draw, so
        // there is no wind-up to model and the pulling/pull predicates would never fire.
        bigWeapon(ModWeapons.AEGIR_GREATSPEAR);

        // Spawn eggs need no texture: the vanilla template tints two greyscale layers from the
        // colours passed to ForgeSpawnEggItem.
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
     * A weapon whose blade sprite is bigger than an inventory slot. It wants a 64×64 sprite in
     * hand and a hand-drawn 16×16 one in the inventory, and vanilla's item model cannot do
     * both — letting the GUI shrink the large one gives an icon with all the detail and none
     * of it legible. Forge's {@code separate_transforms} loader answers with a different model
     * per perspective.
     *
     * <p>Requires {@code item/<name>.png} at 64×64 and {@code item/<name>_gui.png} at 16×16.
     *
     * <p>The transform numbers are Lethality's, carried over as tuned. There is nothing to
     * derive them from.
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
                // Load-bearing: gui_light resolves from the OUTER model, and a
                // separate_transforms model has no parent to inherit it from. Left unset it
                // defaults to side, which lights a flat sprite from the edge and renders the
                // hotbar icon almost black.
                .guiLight(BlockModel.GuiLight.FRONT)
                .customLoader(SeparateTransformsModelBuilder::begin)
                .base(nested()
                        .parent(getExistingFile(mcLoc("item/handheld")))
                        .texture("layer0", heldSprite)
                        .transforms()
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

    /**
     * A {@link #bigWeapon} that also visibly winds up while one of its abilities is charging.
     *
     * <p><b>{@code UseAnim.BOW} does not do this on its own</b> — a real bow appears to bend
     * because its <em>model swaps</em>, through overrides on the {@code pulling} and
     * {@code pull} predicates. Both wind-up models reuse the weapon's existing sprite, so a
     * charge costs no new art.
     *
     * <p><b>Override order matters</b>: vanilla takes the <em>last</em> entry whose predicates
     * all pass, so the deeper threshold has to be declared second. The predicates themselves
     * are registered in {@code WeaponsClient.clientSetup}.
     */
    private void chargedWeapon(RegistryObject<? extends net.minecraft.world.item.Item> weapon) {
        bigWeapon(weapon);

        String name = weapon.getId().getPath();
        ResourceLocation pulling = new ResourceLocation("pulling");
        ResourceLocation pull = new ResourceLocation("pull");

        // Drawn back a little, then a lot: the base transform rotated further round Z and
        // lifted, which reads as the blade being cocked.
        ItemModelBuilder early = windUp(name + "_pulling_0", modLoc("item/" + name), 45.0F, 2.0F);
        ItemModelBuilder full = windUp(name + "_pulling_1", modLoc("item/" + name), 70.0F, 4.0F);

        getBuilder(name)
                .override().predicate(pulling, 1.0F).model(early).end()
                .override().predicate(pulling, 1.0F).predicate(pull, 0.65F).model(full).end();
    }

    /** One wind-up pose: the held sprite, hauled back by {@code extraRoll} and lifted. */
    private ItemModelBuilder windUp(String name, ResourceLocation sprite,
                                    float extraRoll, float lift) {
        return getBuilder(name)
                .guiLight(BlockModel.GuiLight.FRONT)
                .parent(getExistingFile(mcLoc("item/handheld")))
                .texture("layer0", sprite)
                .transforms()
                .transform(ItemDisplayContext.THIRD_PERSON_RIGHT_HAND)
                        .rotation(0, -90, 55 + extraRoll).translation(0, 20 + lift, -1)
                        .scale(2.9F, 2.9F, 1.0F)
                        .end()
                .transform(ItemDisplayContext.THIRD_PERSON_LEFT_HAND)
                        .rotation(0, 90, -55 - extraRoll).translation(0, 20 + lift, -1)
                        .scale(2.9F, 2.9F, 1.0F)
                        .end()
                .transform(ItemDisplayContext.FIRST_PERSON_RIGHT_HAND)
                        .rotation(-15, -90, 25 + extraRoll)
                        .translation(1.13F, 7.2F + lift, 1.13F)
                        .scale(1.7F, 1.7F, 0.85F)
                        .end()
                .transform(ItemDisplayContext.FIRST_PERSON_LEFT_HAND)
                        .rotation(-15, 90, -25 - extraRoll)
                        .translation(1.13F, 7.2F + lift, 1.13F)
                        .scale(1.7F, 1.7F, 0.85F)
                        .end()
                .end();
    }
}
