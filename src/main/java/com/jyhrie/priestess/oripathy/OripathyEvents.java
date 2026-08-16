package com.jyhrie.priestess.oripathy;

import com.jyhrie.priestess.damage.ModDamageTypes;
import com.jyhrie.priestess.effect.ModEffects;
import com.jyhrie.priestess.world.dimension.ModDimensions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Everything that makes {@link Oripathy} actually happen: attaching it to players, carrying it
 * through death, dosing and draining an Acute Oripathy flare-up, handing out the ambient dose
 * to anyone standing on Terra, and turning the number into symptoms.
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

    /**
     * Ambient exposure, to everyone standing on Terra. A placeholder for the real passive
     * model: a flat drip with no regard for biome, depth or what you are wearing. It goes
     * through {@code Oripathy.add} rather than {@code infect}, because breathing the place in
     * is not a wound.
     */
    private static final int PASSIVE_INTERVAL_TICKS = 300;
    private static final int PASSIVE_GAIN = 1;

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
     * Respawning builds a brand new Player, so the infection has to be copied across by hand
     * or dying would cure it. Ordinary dimension travel keeps the same Player but still
     * invalidates and revives its capabilities, which is why {@link OripathyProvider} must not
     * hand out a handle it can never take back.
     */
    @SubscribeEvent
    public static void copyOnClone(PlayerEvent.Clone event) {
        Player original = event.getOriginal();
        // The old player's capabilities are already invalidated by the time this fires.
        original.reviveCaps();
        int carried = Oripathy.of(original);
        // The whole state, not just the number: a flare-up part way through paying itself
        // back has to survive, or the player keeps a dose that was meant to be temporary.
        CompoundTag state = Oripathy.snapshot(original);
        original.invalidateCaps();

        Player respawned = event.getEntity();
        Oripathy.restore(respawned, state);

        if (event.isWasDeath()) {
            // Death ends the flare-up; whatever it still owed is written off.
            Oripathy.cancelRelief(respawned);
            // AFTER_DEATH rather than the lethal threshold, which would kill you again next
            // tick.
            if (carried >= Oripathy.LETHAL) {
                Oripathy.set(respawned, Oripathy.AFTER_DEATH);
            }
        }
    }

    /**
     * A flare-up of Acute Oripathy drives a whole dose in the moment it lands, then hands most
     * of it back over the effect's own duration — 1000 up front at level III, 900 of it gone
     * again by the time the effect wears off, 100 of it yours forever.
     *
     * <p>The dose is a wound, so Open Wounds adds its bonus on top; only the dose itself is
     * refunded, which means Open Wounds makes the permanent residue bigger, not the peak.
     *
     * <p>Only a <i>new</i> flare-up doses you. A re-application at the same level or a weaker
     * one layered under a stronger one refreshes the timer and nothing else — otherwise
     * anything that reapplied the effect every tick would pour in 20 doses a second.
     */
    @SubscribeEvent
    public static void doseOnAcuteOripathy(MobEffectEvent.Added event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide()) {
            return;
        }
        MobEffectInstance added = event.getEffectInstance();
        if (added.getEffect() != ModEffects.ACUTE_ORIPATHY.get()) {
            return;
        }
        MobEffectInstance previous = event.getOldEffectInstance();
        if (previous != null && previous.getAmplifier() >= added.getAmplifier()) {
            return;
        }

        int dose = ModEffects.acuteDose(added.getAmplifier());
        Oripathy.infect(player, dose);
        // Paid out over exactly as long as the effect lasts, so the fall you feel and the icon
        // you can see run out together. A longer /effect duration means a slower fall.
        Oripathy.scheduleRelief(player, ModEffects.acuteRelief(dose), added.getDuration());
    }

    @SubscribeEvent
    public static void playerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Player player = event.player;
        // Server-side only: the value never leaves the server, and effects sync themselves.
        if (player.level().isClientSide()) {
            return;
        }

        // Ahead of the creative check on purpose: relief already promised is always paid off,
        // whatever mode the player switches to part way through. Otherwise one /gamemode
        // during a flare-up would freeze the dose in place permanently.
        Oripathy.tickRelief(player);

        // Creative and spectator are exempt from the rest, so building a world is not done at
        // Slowness II and does not slowly infect you either.
        if (player.isCreative() || player.isSpectator()) {
            return;
        }

        if (player.tickCount % PASSIVE_INTERVAL_TICKS == 0
                && player.level().dimension().equals(ModDimensions.TERRA_KEY)) {
            Oripathy.add(player, PASSIVE_GAIN);
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
