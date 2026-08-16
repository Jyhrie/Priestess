package com.jyhrie.priestess.weapons;

import com.google.common.collect.Multimap;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;

import java.util.Map;

/**
 * The text and numbers helpers a ported weapon expects to find.
 *
 * <p>Trimmed from Lethality's {@code ModUtils}: everything here is something a weapon in this
 * package actually calls.
 *
 * <p>The gradient paints a display name one character at a time, cycling the palette along the
 * string over time. It is per-character {@link Style} colours on a chain of literals — no
 * shader, just a component per letter — animated off the local player's tick count, which is a
 * <em>client</em> concept. See {@link #animationTick()}.
 */
public final class WeaponText {

    /**
     * The clock the gradient animates against. Lethality reads the player's tickCount through
     * {@code DistExecutor.unsafeCallWhenOn(Dist.CLIENT, ...)}, which returns null on a
     * dedicated server and throws on unboxing — latent there, but {@code getName} does run
     * server-side for death messages and anything that logs an item. Checking the dist first
     * and falling through to wall-clock time only has to not explode; nothing renders it.
     */
    private static int animationTick() {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.player != null) {
                return minecraft.player.tickCount;
            }
        }
        return (int) (System.currentTimeMillis() / 50L);
    }

    /**
     * Interpolates a colour at {@code percentage} along a list of RGB stops.
     *
     * @param percentage 0-100, clamped by the segment maths rather than up front
     */
    public static int colorFromGradient(int percentage, int[]... rgbColors) {
        if (rgbColors.length < 2) {
            return rgbToInt(rgbColors[0]);
        }

        float ratio = percentage / 100.0F;
        int totalSegments = rgbColors.length - 1;

        int segment = Math.min((int) (ratio * totalSegments), totalSegments - 1);
        float localRatio = (ratio * totalSegments) - segment;

        int[] from = rgbColors[segment];
        int[] to = rgbColors[segment + 1];

        int r = (int) ((1 - localRatio) * from[0] + localRatio * to[0]);
        int g = (int) ((1 - localRatio) * from[1] + localRatio * to[1]);
        int b = (int) ((1 - localRatio) * from[2] + localRatio * to[2]);

        return (r << 16) | (g << 8) | b;
    }

    /** {@link #gradient(Component, ResourceLocation, float, float, int[]...)} with no custom font. */
    public static MutableComponent gradient(Component text, float speed, float spreadMultiplier,
                                            int[]... rgbColors) {
        return gradient(text, null, speed, spreadMultiplier, rgbColors);
    }

    /**
     * Rebuilds {@code text} as one coloured component per character, sampling the palette at a
     * position that advances with {@link #animationTick()}.
     *
     * @param font             an optional font to style every character with, or {@code null}
     * @param speed            cycles per second — higher is faster
     * @param spreadMultiplier how much of the palette is visible across the string at once;
     *                         above 1 compresses the palette so it repeats along the text
     */
    public static MutableComponent gradient(Component text, ResourceLocation font, float speed,
                                            float spreadMultiplier, int[]... rgbColors) {
        MutableComponent result = Component.empty();
        String string = text.getString();
        int length = string.length();
        int numColors = rgbColors.length;

        if (numColors == 0 || length == 0) {
            return result;
        }

        // Repeat the first stop at the end so the cycle wraps without a visible seam.
        int[][] looped = new int[numColors + 1][3];
        System.arraycopy(rgbColors, 0, looped, 0, numColors);
        looped[numColors] = rgbColors[0];

        float period = 1 / speed;
        float ratio = (animationTick() % (period * 20)) / (period * 20);

        float spread = 1 / spreadMultiplier;
        int effectiveLength = (int) (length * spread);
        if (effectiveLength <= 0) {
            effectiveLength = 1;
        }

        for (int i = 0; i < length; i++) {
            float index = ((((float) i * spread) / length) + ratio) * effectiveLength;
            index = index % effectiveLength;
            int percentage = (int) ((index / effectiveLength) * 100);

            Style style = Style.EMPTY.withColor(colorFromGradient(percentage, looped));
            if (font != null) {
                style = style.withFont(font);
            }
            result = result.append(Component.literal(String.valueOf(string.charAt(i))).withStyle(style));
        }

        return result;
    }

    public static int rgbToInt(int[] rgb) {
        return (rgb[0] << 16) | (rgb[1] << 8) | rgb[2];
    }

    /**
     * The attack damage a stack actually deals, read back off its own attribute modifiers, so
     * an enchanted sword throws proportionally harder projectiles. The starting 1.0 is the
     * player's own base attack damage, which the modifiers are relative to.
     *
     * <p>{@link MobType#UNDEFINED} means Sharpness counts and Smite and Bane of Arthropods do
     * not — the projectile does not know yet what it will hit.
     */
    public static float itemAttackDamage(ItemStack weapon) {
        if (weapon.isEmpty()) {
            return 1.0F;
        }

        Multimap<Attribute, AttributeModifier> modifiers = weapon.getAttributeModifiers(EquipmentSlot.MAINHAND);
        double damage = 1.0;

        for (Map.Entry<Attribute, AttributeModifier> entry : modifiers.entries()) {
            if (entry.getKey().equals(Attributes.ATTACK_DAMAGE)) {
                damage += entry.getValue().getAmount();
            }
        }

        damage += EnchantmentHelper.getDamageBonus(weapon, MobType.UNDEFINED);
        return (float) damage;
    }

    private WeaponText() {
    }
}
