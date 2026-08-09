package com.jyhrie.priestess.oripathy;

import com.jyhrie.priestess.damage.ModDamageTypes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Everything that makes {@link Oripathy} actually happen: attaching it to players,
 * carrying it through death, and turning the number into symptoms.
 *
 * <p>Registered from {@code Priestess}' constructor rather than by annotation, to keep
 * runtime wiring in one place. {@link Oripathy#registerCapability} goes on the mod bus,
 * the {@code @SubscribeEvent} methods here go on the Forge bus.
 */
public class OripathyEvents {

    /** Symptoms are re-evaluated once a second; there is nothing here worth 20 Hz. */
    private static final int CHECK_INTERVAL_TICKS = 20;
    /**
     * Long enough that the one-second check never lets an effect lapse, short enough that
     * curing yourself stops the symptoms within ten seconds rather than instantly — the
     * effects are refreshed, not held, so we never strip a potion the player drank.
     */
    private static final int EFFECT_DURATION_TICKS = 200;
    /** Only re-apply once an effect has burned past halfway, to avoid a packet a second. */
    private static final int REFRESH_BELOW_TICKS = 100;

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        Oripathy.registerCapability(event);
    }

    @SubscribeEvent
    public static void attachCapability(AttachCapabilitiesEvent<Entity> event) {
        if (!(event.getObject() instanceof Player)) {
            return;
        }
        event.addCapability(OripathyProvider.ID, new OripathyProvider());
    }

    /**
     * Respawning — after death, and on the way back out of the End — builds a brand new
     * Player, so the infection has to be copied across by hand or dying would cure it.
     *
     * <p>Ordinary dimension travel keeps the same Player and needs nothing here; it does
     * still invalidate and revive its capabilities, which is why {@link OripathyProvider}
     * must not hand out a handle it can never take back.
     */
    @SubscribeEvent
    public static void copyOnClone(PlayerEvent.Clone event) {
        Player original = event.getOriginal();
        // The old player's capabilities are already invalidated by the time this fires.
        original.reviveCaps();
        int carried = Oripathy.of(original);
        original.invalidateCaps();

        // Dying of oripathy is the one thing that lowers it: you come back at AFTER_DEATH
        // instead of at the lethal threshold, which would kill you again on the next tick.
        boolean diedOfIt = event.isWasDeath() && carried >= Oripathy.LETHAL;
        Oripathy.set(event.getEntity(), diedOfIt ? Oripathy.AFTER_DEATH : carried);
    }

    @SubscribeEvent
    public static void applySymptoms(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Player player = event.player;
        // Server-side only: the value never leaves the server, and effects sync themselves.
        // Creative and spectator are exempt, so building a world is not done at Slowness II.
        if (player.level().isClientSide() || player.isCreative() || player.isSpectator()) {
            return;
        }
        if (player.tickCount % CHECK_INTERVAL_TICKS != 0) {
            return;
        }

        int oripathy = Oripathy.of(player);

        if (oripathy >= Oripathy.LETHAL) {
            // Float.MAX_VALUE is what vanilla's /kill uses. Nothing is reset here: the
            // respawn value is applied in copyOnClone, so a totem or a cancelled death
            // leaves the player still terminal, and still dying, which is the point.
            player.hurt(ModDamageTypes.source(player.level(), ModDamageTypes.ORIPATHY), Float.MAX_VALUE);
            return;
        }

        // Cumulative: each stage keeps everything the stage below it gave you.
        if (oripathy >= Oripathy.SLOWNESS) {
            symptom(player, MobEffects.MOVEMENT_SLOWDOWN, 1);
        }
        if (oripathy >= Oripathy.WEAKNESS) {
            symptom(player, MobEffects.WEAKNESS, 1);
        }
        if (oripathy >= Oripathy.BLINDNESS) {
            symptom(player, MobEffects.BLINDNESS, 0);
        }
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        OripathyCommand.register(event.getDispatcher());
    }

    /**
     * Tops the effect back up unless the player already has it at least this strong with
     * time to spare — so a stronger potion is never downgraded and a longer one is never
     * cut short.
     */
    private static void symptom(Player player, MobEffect effect, int amplifier) {
        MobEffectInstance active = player.getEffect(effect);
        if (active != null && (active.getAmplifier() > amplifier
                || (active.getAmplifier() == amplifier && active.getDuration() > REFRESH_BELOW_TICKS))) {
            return;
        }
        // Icon shown, particles off: the player should be able to tell they are slowed
        // without trailing symptom particles everywhere they walk.
        player.addEffect(new MobEffectInstance(effect, EFFECT_DURATION_TICKS, amplifier, false, false, true));
    }
}
