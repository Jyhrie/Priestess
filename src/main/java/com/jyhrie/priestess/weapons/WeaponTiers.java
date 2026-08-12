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
 * <p>A {@link Tier} carries durability, mining level, enchantability and the repair
 * ingredient. It does <em>not</em> carry attack damage for these weapons — every one of them
 * passes its own damage and swing speed to the {@code SwordItem} constructor and leaves the
 * tier's {@code attackDamageBonus} at zero, so changing numbers here will not change how hard
 * anything hits.
 *
 * <p>{@link TierSortingRegistry} is what makes a modded tier sort against vanilla's, so that
 * "needs better than diamond" checks resolve correctly. Sorting after {@code NETHERITE} with
 * nothing after it means these sit at the top of the ladder.
 */
public final class WeaponTiers {

    /**
     * Lethality's demonic tier.
     *
     * <p>Two values were changed on the way in, because the originals named things that do
     * not exist in this mod:
     * <ul>
     *   <li><b>Mineable tag</b> — {@code BlockTags.NEEDS_DIAMOND_TOOL} in place of Lethality's
     *       custom {@code ANCIENT_WEAPON} tag. It is a sword; the tag decides what it can mine,
     *       which is cobwebs and not much else, and is not worth a bespoke tag file.</li>
     *   <li><b>Repair ingredient</b> — a netherite ingot in place of the Bladecrest Oathsword,
     *       a Lethality item that did not come across. Swap this the moment there is a
     *       Columbia material that ought to repair it.</li>
     * </ul>
     */
    public static final Tier DEMONIC = TierSortingRegistry.registerTier(
            new ForgeTier(5, 1850, 0.0F, 0.0F, 25,
                    BlockTags.NEEDS_DIAMOND_TOOL, () -> Ingredient.of(Items.NETHERITE_INGOT)),
            new ResourceLocation(Priestess.MOD_ID, "demonic"), List.of(Tiers.NETHERITE), List.of());

    private WeaponTiers() {
    }
}
