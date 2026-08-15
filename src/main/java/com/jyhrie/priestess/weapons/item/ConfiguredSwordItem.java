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
 * <h2>Why the constructor's numbers cannot simply be the config's</h2>
 * {@link SwordItem} builds an immutable {@code Multimap} of attribute modifiers <b>once</b>, in
 * its constructor, and every item in the game is constructed during registration — early enough
 * in mod loading to be racing the config file. Even if it won that race, the map would then be
 * fixed for the process's lifetime, so editing the config would take a restart. So the compiled
 * defaults still go to {@code super} and still decide what the item is without a config, and
 * this overrides the <em>getter</em> so the map is built fresh from the config each time it is
 * asked for.
 *
 * <p>Rebuilt per call rather than cached, deliberately. It is two small objects, and the callers
 * are equipment changes and tooltip rendering rather than anything per-tick; a cache would have
 * to be invalidated whenever the config file changed on disk, and getting that wrong is a weapon
 * quietly displaying the wrong number. Not caching is also what lets an edit take effect live.
 *
 * <h2>What this drags along with it</h2>
 * Every ability in this package that scales off {@code WeaponText.itemAttackDamage} reads the
 * stack's attribute modifiers, so it reads this, so it follows the config too. That is the whole
 * reason ability damage is configured as a fraction: {@code attackDamage} moves the weapon and
 * everything it throws together, and the fractions only decide the shape.
 */
public abstract class ConfiguredSwordItem extends SwordItem {

    private final Stats.Weapon stats;

    protected ConfiguredSwordItem(Tier tier, Stats.Weapon stats, Properties properties) {
        super(tier, stats.defaultAttackDamage(), stats.defaultAttackSpeed(), properties);
        this.stats = stats;
    }

    /**
     * Main hand only, which is where {@code SwordItem} puts its own modifiers — the off hand
     * carries none in vanilla and none here, so a sword held there is a sword that does not
     * change the numbers on your arm.
     */
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
