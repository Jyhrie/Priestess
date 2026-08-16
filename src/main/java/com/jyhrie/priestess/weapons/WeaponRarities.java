package com.jyhrie.priestess.weapons;

import net.minecraft.ChatFormatting;
import net.minecraft.world.item.Rarity;

/**
 * Rarity tiers above vanilla's four, for the ported weapons.
 *
 * <p>{@link Rarity#create} is a Forge extensible-enum call that runs at classload rather than
 * through a {@code DeferredRegister}, so this class must be touched before any item asks for
 * the value — which it is, since only the items reference {@code CALAMITOUS}.
 *
 * <p>The colour is the vanilla tooltip fallback. Weapons that override {@code getName} paint
 * their own gradient, so in practice this gold only shows on the rarity line.
 */
public final class WeaponRarities {

    /** Above {@code EPIC}. Lethality's top tier, kept under its own name. */
    public static final Rarity CALAMITOUS = Rarity.create("priestess_calamitous", ChatFormatting.GOLD);

    private WeaponRarities() {
    }
}
