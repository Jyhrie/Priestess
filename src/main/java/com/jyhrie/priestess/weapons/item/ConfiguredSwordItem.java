package com.jyhrie.priestess.weapons.item;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.jyhrie.priestess.config.Stats;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;

/**
 * A sword whose damage and swing speed come from {@code config/priestess/weapon.toml} rather
 * than from the numbers it was constructed with.
 *
 * <p>The constructor's numbers cannot be the config's: {@link SwordItem} builds its modifier
 * {@code Multimap} once, during registration, which races config load and is then frozen for
 * the process. So the compiled defaults still go to {@code super} — they are what the item is
 * without a config — and this overrides the <em>getter</em> to rebuild from the config on each
 * call, which is also what lets an edit take effect without a restart. Not cached, because a
 * stale cache is a weapon displaying the wrong number.
 *
 * <p>Abilities that scale off {@code WeaponText.itemAttackDamage} read the stack's modifiers,
 * so they follow the config too. That is why ability damage is configured as a fraction:
 * {@code attackDamage} moves the weapon and everything it throws together.
 */
public abstract class ConfiguredSwordItem extends SwordItem {

    private final Stats.Weapon stats;

    protected ConfiguredSwordItem(Tier tier, Stats.Weapon stats, Properties properties) {
        super(tier, stats.defaultAttackDamage(), stats.defaultAttackSpeed(), properties);
        this.stats = stats;
    }

    /** Main hand only, which is where {@code SwordItem} puts its own modifiers. */
    @Override
    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(EquipmentSlot slot,
                                                                       ItemStack stack) {
        if (slot != EquipmentSlot.MAINHAND) {
            return super.getAttributeModifiers(slot, stack);
        }
        // The same two UUIDs vanilla uses, so these replace the base modifiers rather than
        // stacking with them — an item is allowed one modifier per UUID per attribute.
        return ImmutableMultimap.of(
                Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_UUID,
                        "Weapon modifier", stats.attackDamage(),
                        AttributeModifier.Operation.ADDITION),
                Attributes.ATTACK_SPEED, new AttributeModifier(BASE_ATTACK_SPEED_UUID,
                        "Weapon modifier", stats.attackSpeed(),
                        AttributeModifier.Operation.ADDITION));
    }
}
