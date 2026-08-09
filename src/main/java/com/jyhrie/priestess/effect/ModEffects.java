package com.jyhrie.priestess.effect;

import com.jyhrie.priestess.Priestess;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * The two oripathy-related potion effects, and the numbers behind them.
 *
 * <p>Neither effect does anything on its own — see {@link InertMobEffect}. What they mean
 * is implemented in the oripathy package:
 *
 * <ul>
 *   <li><b>Open Wounds</b> — every wound you take drives in more oripathy than it should.
 *       Read by {@code Oripathy.infect}, which is the only path it applies to; ambient
 *       exposure goes through {@code Oripathy.add} and is untouched.
 *   <li><b>Acute Oripathy</b> — a flare-up. Applying it dumps a whole dose of oripathy in
 *       at once, and most of that dose drains back out again over the effect's duration.
 *       Handled by {@code OripathyEvents.doseOnAcuteOripathy}.
 * </ul>
 *
 * <p>The level tables live here so all the tuning for both effects is in one file.
 */
public class ModEffects {

    public static final DeferredRegister<MobEffect> EFFECTS =
            DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, Priestess.MOD_ID);

    public static final RegistryObject<MobEffect> OPEN_WOUNDS =
            EFFECTS.register("open_wounds", () -> new InertMobEffect(MobEffectCategory.HARMFUL, 0xB8262E));

    public static final RegistryObject<MobEffect> ACUTE_ORIPATHY =
            EFFECTS.register("acute_oripathy", () -> new InertMobEffect(MobEffectCategory.HARMFUL, 0x5FC8E8));

    /** Extra oripathy per wound, for Open Wounds I / II / III. */
    private static final int[] OPEN_WOUNDS_BONUS = {15, 30, 50};

    /** The dose a flare-up of Acute Oripathy I / II / III drives in when it lands. */
    private static final int[] ACUTE_DOSE = {250, 500, 1000};

    /**
     * How much of an acute dose drains back out over the effect's duration. The rest is
     * permanent: a level III flare-up is +1000 now and +100 forever.
     */
    public static final int ACUTE_RELIEF_PERCENT = 90;

    /** 0 when the entity does not have Open Wounds, which is the common case. */
    public static int openWoundsBonus(LivingEntity entity) {
        MobEffectInstance active = entity.getEffect(OPEN_WOUNDS.get());
        return active == null ? 0 : level(OPEN_WOUNDS_BONUS, active.getAmplifier());
    }

    public static int acuteDose(int amplifier) {
        return level(ACUTE_DOSE, amplifier);
    }

    public static int acuteRelief(int dose) {
        return dose * ACUTE_RELIEF_PERCENT / 100;
    }

    /**
     * A level above the highest one the table defines is treated as that level rather than
     * extrapolated — {@code /effect give @s priestess:open_wounds 60 40} should not be worth
     * a thousand oripathy a hit.
     */
    private static int level(int[] table, int amplifier) {
        return table[Mth.clamp(amplifier, 0, table.length - 1)];
    }

    public static void register(IEventBus eventBus) {
        EFFECTS.register(eventBus);
    }

    private ModEffects() {
    }
}
