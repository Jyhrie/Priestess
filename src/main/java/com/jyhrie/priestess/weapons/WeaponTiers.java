package com.jyhrie.priestess.weapons;

import com.jyhrie.priestess.Priestess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.common.ForgeTier;
import net.minecraftforge.common.TierSortingRegistry;

import java.util.List;

/**
 * Tool tiers for the ported weapons.
 *
 * <p>A {@link Tier} carries durability, mining level, enchantability and the repair ingredient
 * — <em>not</em> attack damage. Every weapon here leaves {@code attackDamageBonus} at zero and
 * takes its damage from {@code config/priestess/weapon.toml}, because a bonus set here would
 * be the one part of a weapon's damage the config could not reach.
 *
 * <p>{@link TierSortingRegistry} is what makes a modded tier sort against vanilla's, so
 * "needs better than diamond" checks resolve correctly.
 */
public final class WeaponTiers {

    /**
     * Lethality's demonic tier. Two values changed on the way in because the originals named
     * things this mod does not have: the mineable tag (was a custom {@code ANCIENT_WEAPON}
     * tag) and the repair ingredient (was the Bladecrest Oathsword — swap it once there is a
     * Columbia material that ought to repair this).
     */
    public static final Tier DEMONIC = TierSortingRegistry.registerTier(
            new ForgeTier(5, 1850, 0.0F, 0.0F, 25,
                    BlockTags.NEEDS_DIAMOND_TOOL, () -> Ingredient.of(Items.NETHERITE_INGOT)),
            new ResourceLocation(Priestess.MOD_ID, "demonic"), List.of(Tiers.NETHERITE), List.of());

    private WeaponTiers() {
    }
}
