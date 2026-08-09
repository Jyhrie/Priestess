package com.jyhrie.priestess.oripathy;

import com.jyhrie.priestess.effect.ModEffects;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;

/**
 * Oripathy — a per-player infection level, in the spirit of Thaumcraft's warp.
 *
 * <p>The number itself is never shown to the player: there is no HUD and no chat message.
 * You find out how infected you are by noticing that you have started to limp. The only
 * ways to read it are {@code /oripathy get} and the static accessors here.
 *
 * <p>Stored as a capability on the Player, so it saves with the player file and survives
 * logout. {@link OripathyEvents} handles carrying it through death and dimension changes.
 *
 * <p>Almost nothing raises oripathy on its own yet — this is the substrate. There are two
 * ways in, and which one you pick matters:
 *
 * <ul>
 *   <li>{@link #infect(Player, int)} — a <b>wound</b>. Something got in. Open Wounds makes
 *       every one of these worse. This is what content that infects people should call.
 *   <li>{@link #add(Player, int)} — the raw number, unmodified. For ambient exposure, for
 *       treatment (a negative delta), for the command, and for the acute drain — none of
 *       which are wounds, and none of which Open Wounds should touch.
 * </ul>
 */
public class Oripathy {

    public static final Capability<Oripathy> CAPABILITY = CapabilityManager.get(new CapabilityToken<>() {});

    /** Everyone on Terra carries a trace of it. There is no such thing as a zero. */
    public static final int MIN = 1;
    public static final int MAX = 10_000;

    /** Slowness II from here up. */
    public static final int SLOWNESS = 5_000;
    /** ...and Weakness II on top of it. */
    public static final int WEAKNESS = 7_500;
    /** ...and Blindness on top of both. */
    public static final int BLINDNESS = 9_000;
    /** Terminal. Equal to MAX — the scale tops out at death, it does not run past it. */
    public static final int LETHAL = MAX;

    /**
     * Where a player who died *of oripathy* comes back. Below {@link #WEAKNESS}, so they
     * respawn limping but able to fight and see — one stage of relapse away from the
     * crystals taking them again, rather than instantly back at the lethal threshold.
     */
    public static final int AFTER_DEATH = 7_000;

    private static final String NBT_KEY = "oripathy";
    private static final String NBT_RELIEF_OWED = "relief_owed";
    private static final String NBT_RELIEF_TICKS = "relief_ticks";

    private int value = MIN;

    /** Oripathy an acute flare-up has still to hand back. See {@link #tickRelief()}. */
    private int reliefOwed;
    /** Ticks left to hand it back over. Zero whenever nothing is owed. */
    private int reliefTicks;

    public int get() {
        return value;
    }

    public void set(int value) {
        this.value = Mth.clamp(value, MIN, MAX);
    }

    public void add(int delta) {
        set(value + delta);
    }

    public int reliefOwed() {
        return reliefOwed;
    }

    /**
     * Promises {@code amount} back over the next {@code ticks} ticks. Stacks: a second
     * flare-up on top of the first adds to what is owed and re-times the payout to its own
     * duration, so the drain always finishes when the effect does.
     */
    public void scheduleRelief(int amount, int ticks) {
        if (amount <= 0) {
            return;
        }
        reliefOwed += amount;
        // A zero or negative duration would divide by zero below; pay it all off next tick.
        reliefTicks = Math.max(1, ticks);
    }

    public void cancelRelief() {
        reliefOwed = 0;
        reliefTicks = 0;
    }

    /**
     * Hands back one tick's worth. Called every tick, so the fall after a flare-up is
     * visibly gradual rather than a single step.
     *
     * <p>The step is divided against the ticks <i>remaining</i>, not the original duration.
     * That makes it self-correcting: whatever integer division loses on one tick is picked up
     * by the next, and the final tick pays off the remainder exactly, so a 225-over-100-ticks
     * payout lands on 225 rather than on 200.
     */
    public void tickRelief() {
        if (reliefOwed <= 0 || reliefTicks <= 0) {
            cancelRelief();
            return;
        }
        int step = reliefTicks == 1 ? reliefOwed : reliefOwed / reliefTicks;
        reliefTicks--;
        reliefOwed -= step;
        // Clamped like any other change: relief cannot take a player below MIN.
        add(-step);
        if (reliefOwed <= 0 || reliefTicks <= 0) {
            cancelRelief();
        }
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putInt(NBT_KEY, value);
        // Written unconditionally so a log-out mid-flare-up still owes the same amount on
        // log-in; getInt returns 0 for an absent key, which is exactly "nothing owed".
        tag.putInt(NBT_RELIEF_OWED, reliefOwed);
        tag.putInt(NBT_RELIEF_TICKS, reliefTicks);
        return tag;
    }

    public void load(CompoundTag tag) {
        // Routed through set() so an absent or hand-edited key still lands inside the range.
        set(tag.getInt(NBT_KEY));
        reliefOwed = Math.max(0, tag.getInt(NBT_RELIEF_OWED));
        reliefTicks = Math.max(0, tag.getInt(NBT_RELIEF_TICKS));
        if (reliefOwed == 0 || reliefTicks == 0) {
            cancelRelief();
        }
    }

    // --- Static access. Use these rather than touching the capability directly. ---

    public static int of(Player player) {
        return player.getCapability(CAPABILITY).map(Oripathy::get).orElse(MIN);
    }

    public static void set(Player player, int value) {
        player.getCapability(CAPABILITY).ifPresent(oripathy -> oripathy.set(value));
    }

    /**
     * The raw number, unmodified — ambient exposure, treatment, the command, the acute drain.
     * Negative deltas are treatment; the result is clamped to [MIN, MAX] either way.
     *
     * <p>For something that <i>infected</i> the player, use {@link #infect(Player, int)}.
     */
    public static void add(Player player, int delta) {
        player.getCapability(CAPABILITY).ifPresent(oripathy -> oripathy.add(delta));
    }

    /**
     * A wound: oripathy driven in from outside, by a mob, a block, a hit. Open Wounds adds
     * a flat bonus on top of every one of these — that is the whole point of it — so every
     * source that infects a player should come through here rather than through
     * {@link #add(Player, int)}.
     *
     * <p>The bonus is not part of the amount the caller asked for, which matters for Acute
     * Oripathy: the dose drains back out, the Open Wounds bonus on top of it does not.
     *
     * @param amount the wound's own dose, before Open Wounds. Non-positive amounts do nothing;
     *               a wound that heals you is not a thing, and negatives here would be
     *               <i>increased</i> by Open Wounds, which is backwards.
     */
    public static void infect(Player player, int amount) {
        if (amount <= 0) {
            return;
        }
        add(player, amount + ModEffects.openWoundsBonus(player));
    }

    /** See {@link #scheduleRelief(int, int)}. */
    public static void scheduleRelief(Player player, int amount, int ticks) {
        player.getCapability(CAPABILITY).ifPresent(oripathy -> oripathy.scheduleRelief(amount, ticks));
    }

    public static void tickRelief(Player player) {
        player.getCapability(CAPABILITY).ifPresent(Oripathy::tickRelief);
    }

    public static void cancelRelief(Player player) {
        player.getCapability(CAPABILITY).ifPresent(Oripathy::cancelRelief);
    }

    public static int reliefOwed(Player player) {
        return player.getCapability(CAPABILITY).map(Oripathy::reliefOwed).orElse(0);
    }

    /**
     * The whole state as NBT — the value and any relief still owed. Used by
     * {@link OripathyEvents#copyOnClone} to move everything onto a rebuilt Player, so that
     * dying mid-flare-up cannot strand a payout and leave the dose permanent.
     */
    static CompoundTag snapshot(Player player) {
        return player.getCapability(CAPABILITY).map(Oripathy::save).orElseGet(CompoundTag::new);
    }

    static void restore(Player player, CompoundTag tag) {
        player.getCapability(CAPABILITY).ifPresent(oripathy -> oripathy.load(tag));
    }

    static void registerCapability(RegisterCapabilitiesEvent event) {
        event.register(Oripathy.class);
    }
}
