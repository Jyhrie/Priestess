package com.jyhrie.priestess.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * A {@link MobEffect} that does nothing by itself.
 *
 * <p>{@code MobEffect}'s constructor is protected, so even an effect whose entire meaning
 * lives somewhere else needs a subclass to exist. Both of Terra's effects are like that:
 * Open Wounds is a number other code reads when it infects you, and Acute Oripathy does its
 * work in {@code OripathyEvents} the moment it is applied. Neither has anything to do on a
 * tick, so neither overrides {@code applyEffectTick}.
 */
public class InertMobEffect extends MobEffect {

    public InertMobEffect(MobEffectCategory category, int color) {
        super(category, color);
    }
}
