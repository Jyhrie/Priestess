package com.jyhrie.priestess.item;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurio;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import java.util.UUID;

/**
 * Template — the first Module, and the one every later Module is meant to be a copy of.
 *
 * <p>It wears correctly and does nothing, which is deliberate on both counts. Doing nothing
 * keeps it the same kind of thing as the rest of the mod's items: named, obtainable, and
 * waiting on the chapter that gives it a purpose. Wearing correctly is the part worth copying,
 * because none of it is guessable — see docs/CURIOS.md.
 *
 * <p><b>Which slot this fits is not decided here.</b> It comes from the {@code curios:module}
 * item tag in {@link com.jyhrie.priestess.datagen.ModItemTagsProvider}. A curio class with no
 * tag entry is an ordinary inventory item that cannot be equipped anywhere, and nothing warns
 * you about it.
 *
 * <p>{@link ICurioItem} gives every method a default, so implementing it costs nothing and you
 * override only what you want. The hooks worth knowing about:
 *
 * <table border="1">
 *   <caption>ICurioItem hooks</caption>
 *   <tr><td>{@code getAttributeModifiers}</td><td>attributes applied while worn</td></tr>
 *   <tr><td>{@code canEquipFromUse}</td><td>right-click in hand to equip</td></tr>
 *   <tr><td>{@code getEquipSound}</td><td>sound on equip</td></tr>
 *   <tr><td>{@code curioTick}</td><td>every tick while worn</td></tr>
 *   <tr><td>{@code onEquip} / {@code onUnequip}</td><td>one-shot effects</td></tr>
 * </table>
 */
public class TemplateModuleItem extends Item implements ICurioItem {

    public TemplateModuleItem(Properties properties) {
        super(properties);
    }

    /**
     * Attributes applied while worn. Empty, because Template is a shape rather than a piece of
     * balance — a real Module fills this in, e.g.
     *
     * <pre>{@code
     * modifiers.put(Attributes.ARMOR,
     *         new AttributeModifier(uuid, "Module armor", 2.0D, AttributeModifier.Operation.ADDITION));
     * }</pre>
     *
     * <p><b>Use the {@code uuid} passed in, never a constant of your own.</b> Curios hands out
     * a UUID that is unique per slot, which is what lets the same accessory worn in two slots
     * apply twice. A constant makes the second copy silently replace the first, and the
     * symptom — "my two rings only count once" — points nowhere near the cause.
     */
    @Override
    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(SlotContext slotContext,
                                                                       UUID uuid, ItemStack stack) {
        return LinkedHashMultimap.create();
    }

    /** Right-click the item in hand to equip it, rather than having to open the Curios GUI. */
    @Override
    public boolean canEquipFromUse(SlotContext slotContext, ItemStack stack) {
        return true;
    }

    @Override
    public ICurio.SoundInfo getEquipSound(SlotContext slotContext, ItemStack stack) {
        return new ICurio.SoundInfo(SoundEvents.ARMOR_EQUIP_GENERIC, 1.0F, 1.0F);
    }
}
