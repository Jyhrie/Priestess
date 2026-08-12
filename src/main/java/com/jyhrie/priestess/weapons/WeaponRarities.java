package com.jyhrie.priestess.weapons;

import net.minecraft.ChatFormatting;
import net.minecraft.world.item.Rarity;

/**
 * Rarity tiers above vanilla's four, for the ported weapons.
 *
 * <p>{@link Rarity#create} is a Forge extensible-enum call, and it runs at classload of this
 * class rather than through a {@code DeferredRegister}. That is fine — rarities are not a
 * registry — but it does mean this class must be touched before any item asks for the value,
 * which it is, because the only things that reference {@code CALAMITOUS} are the items
 * themselves.
 *
 * <p>The colour here is the fallback that the vanilla tooltip path uses. Devil's Devastation
 * overrides {@code getName} and paints its own animated gradient, so in practice you only see
 * this gold on the rarity line rather than on the item name.
 */
public final class WeaponRarities {

    /** Above {@code EPIC}. Lethality's top tier, kept under its own name. */
    public static final Rarity CALAMITOUS = Rarity.create("priestess_calamitous", ChatFormatting.GOLD);

    private WeaponRarities() {
    }
}
