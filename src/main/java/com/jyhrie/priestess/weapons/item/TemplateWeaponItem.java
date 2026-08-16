package com.jyhrie.priestess.weapons.item;

import com.jyhrie.priestess.config.Stats;
import com.jyhrie.priestess.config.WeaponStats;
import com.jyhrie.priestess.weapons.ModWeapons;
import com.jyhrie.priestess.weapons.WeaponText;
import com.jyhrie.priestess.weapons.WeaponTiers;
import com.jyhrie.priestess.weapons.entity.WeaponVfx;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.model.generators.ItemModelBuilder;
import net.minecraftforge.client.model.generators.ItemModelProvider;
import net.minecraftforge.client.model.generators.loaders.SeparateTransformsModelBuilder;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class TemplateWeaponItem extends ConfiguredSwordItem{

    private static final double EXAMPLE_STAT = 5.0;

    private static final Tier TIER = WeaponTiers.DEMONIC;
    private static final Stats.Weapon STATS = WeaponStats.TEMPLATE_WEAPON_STATS;
    private static final Properties PROPERTIES = new Properties();

    private static final Rarity RARITY = Rarity.EPIC;

    public TemplateWeaponItem() {
        super(TIER, STATS, PROPERTIES);
    }

    @Override
    public Rarity getRarity(ItemStack stack) {
        return RARITY;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal(" "));
        tooltip.add(Component.translatable("tooltip.priestess.template.flavour"));
        tooltip.add(Component.literal(" "));

        if (Screen.hasShiftDown()) {
            tooltip.add(Component.translatable("tooltip.priestess.template.left"));
            tooltip.add(Component.translatable("tooltip.priestess.template.left_detail"));
            tooltip.add(Component.literal(" "));
            tooltip.add(Component.translatable("tooltip.priestess.template.right"));
            tooltip.add(Component.translatable("tooltip.priestess.template.right_detail"));
        } else {
            tooltip.add(Component.translatable("tooltip.priestess.hold_shift"));
        }

        super.appendHoverText(stack, level, tooltip, flag);
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        // APPLY ON ENEMY HIT EFFECTS HERE
        return super.hurtEnemy(stack, target, attacker);
    }

    // IMPORTANT: register the left click in weapons/client/WeaponSwingEvents AND add a
    // dispatch line in weapons/network/SwingSlashC2S. Vanilla sends the server nothing when a
    // swing hits air, so without both this is never called and the ability silently does
    // nothing. See docs/WEAPONS.md § 8.
    //
    // Main hand only: WeaponSwingEvents gates on the main hand, so a weapon that scans both
    // hands fires while sitting in the off hand as another weapon is swung. See ERRORS.md § A1.
    public static void sweep(Level level, Player user) {

//        ItemStack stack = user.getMainHandItem();
//        if (stack.getItem() != ModWeapons.TEMPLATE_WEAPON.get()) {
//            return;
//        }
//        if (user.getCooldowns().isOnCooldown(stack.getItem())) {
//            return;
//        }
//        // Already on the server. Spawn entities and deal damage here.
    }

    public static final class Model {

        /** The registry name. Must match the {@code ITEMS.register} string in ModWeapons. */
        private static final String NAME = "template_weapon";

        // A greatsword's numbers, as a starting point to tune from in-game — there is nothing
        // to derive them from. rotation is [pitch, yaw, roll] in degrees, translation is in
        // 1/16ths of a block, scale is per-axis. First person always wants to be smaller, or
        // the blade fills the screen.

        private static final float THIRD_ROLL = 55.0F;
        private static final float THIRD_LIFT = 20.0F;   // keeps a long blade off the ground
        private static final float THIRD_SCALE = 2.9F;

        private static final float FIRST_PITCH = -15.0F;
        private static final float FIRST_ROLL = 25.0F;
        private static final float FIRST_LIFT = 7.2F;
        private static final float FIRST_SCALE = 1.7F;

        public static void build(ItemModelProvider provider) {
            ResourceLocation held = provider.modLoc("item/" + NAME);
            ResourceLocation gui = provider.modLoc("item/" + NAME + "_gui");

            ItemModelBuilder small = provider.nested()
                    .parent(provider.getExistingFile(provider.mcLoc("item/generated")))
                    // Belt and braces with the outer guiLight below — see the note there.
                    .guiLight(BlockModel.GuiLight.FRONT)
                    .texture("layer0", gui);

            provider.getBuilder(NAME)
                    // Load-bearing: gui_light resolves from the OUTER model, and a
                    // separate_transforms model has no parent to inherit it from. Left unset it
                    // defaults to side, which lights a flat sprite from its edge and renders the
                    // hotbar icon almost black.
                    .guiLight(BlockModel.GuiLight.FRONT)
                    .customLoader(SeparateTransformsModelBuilder::begin)
                    .base(inHand(provider, held))
                    // base covers everywhere not listed here; these two are where the item is
                    // seen small and flat.
                    .perspective(ItemDisplayContext.GUI, small)
                    .perspective(ItemDisplayContext.GROUND, small)
                    .end();

            // If an ability of yours charges, uncomment this. UseAnim.BOW alone does almost
            // nothing in first person — a real bow appears to bend because its MODEL SWAPS,
            // through overrides on the pulling and pull predicates. Register those two in
            // WeaponsClient.clientSetup first.
            //
            // ORDER MATTERS: vanilla takes the LAST override whose predicates all pass, so the
            // deeper threshold must be declared second or it can never win.
            //
            // ItemModelBuilder early = windUp(provider, NAME + "_pulling_0", held, 45.0F, 2.0F);
            // ItemModelBuilder full  = windUp(provider, NAME + "_pulling_1", held, 70.0F, 4.0F);
            //
            // provider.getBuilder(NAME)
            //         .override().predicate(new ResourceLocation("pulling"), 1.0F)
            //                 .model(early).end()
            //         .override().predicate(new ResourceLocation("pulling"), 1.0F)
            //                 .predicate(new ResourceLocation("pull"), 0.65F)
            //                 .model(full).end();
        }

        /** The held pose: the 64×64 sprite, angled and scaled for both hands in both views. */
        private static ItemModelBuilder inHand(ItemModelProvider provider, ResourceLocation sprite) {
            return pose(provider.nested(), provider, sprite, 0.0F, 0.0F);
        }

        /**
         * One wind-up pose for a charged ability. Only used by the commented block above;
         * delete it along with that block if nothing here charges.
         */
        @SuppressWarnings("unused")
        private static ItemModelBuilder windUp(ItemModelProvider provider, String name,
                                               ResourceLocation sprite,
                                               float extraRoll, float lift) {
            return pose(provider.getBuilder(name)
                                .guiLight(BlockModel.GuiLight.FRONT),
                        provider, sprite, extraRoll, lift);
        }

        /**
         * Shared by the held pose and every wind-up pose, so a charge continues from where the
         * idle pose left off rather than snapping to an unrelated angle. Left-hand transforms
         * are the right-hand ones mirrored: yaw and roll negate, nothing else does.
         */
        private static ItemModelBuilder pose(ItemModelBuilder builder, ItemModelProvider provider,
                                             ResourceLocation sprite,
                                             float extraRoll, float lift) {
            return builder
                    .parent(provider.getExistingFile(provider.mcLoc("item/handheld")))
                    .texture("layer0", sprite)
                    .transforms()
                    .transform(ItemDisplayContext.THIRD_PERSON_RIGHT_HAND)
                            .rotation(0, -90, THIRD_ROLL + extraRoll)
                            .translation(0, THIRD_LIFT + lift, -1)
                            .scale(THIRD_SCALE, THIRD_SCALE, 1.0F)
                            .end()
                    .transform(ItemDisplayContext.THIRD_PERSON_LEFT_HAND)
                            .rotation(0, 90, -THIRD_ROLL - extraRoll)
                            .translation(0, THIRD_LIFT + lift, -1)
                            .scale(THIRD_SCALE, THIRD_SCALE, 1.0F)
                            .end()
                    .transform(ItemDisplayContext.FIRST_PERSON_RIGHT_HAND)
                            .rotation(FIRST_PITCH, -90, FIRST_ROLL + extraRoll)
                            .translation(1.13F, FIRST_LIFT + lift, 1.13F)
                            .scale(FIRST_SCALE, FIRST_SCALE, 0.85F)
                            .end()
                    .transform(ItemDisplayContext.FIRST_PERSON_LEFT_HAND)
                            .rotation(FIRST_PITCH, 90, -FIRST_ROLL - extraRoll)
                            .translation(1.13F, FIRST_LIFT + lift, 1.13F)
                            .scale(FIRST_SCALE, FIRST_SCALE, 0.85F)
                            .end()
                    .end();
        }

        private Model() {
        }
    }
}
