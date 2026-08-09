package com.jyhrie.priestess.oripathy;

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
 * <p>Nothing raises oripathy on its own yet — this is the substrate. Content that infects
 * people calls {@link #add(Player, int)}.
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

    private int value = MIN;

    public int get() {
        return value;
    }

    public void set(int value) {
        this.value = Mth.clamp(value, MIN, MAX);
    }

    public void add(int delta) {
        set(value + delta);
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putInt(NBT_KEY, value);
        return tag;
    }

    public void load(CompoundTag tag) {
        // Routed through set() so an absent or hand-edited key still lands inside the range.
        set(tag.getInt(NBT_KEY));
    }

    // --- Static access. Use these rather than touching the capability directly. ---

    public static int of(Player player) {
        return player.getCapability(CAPABILITY).map(Oripathy::get).orElse(MIN);
    }

    public static void set(Player player, int value) {
        player.getCapability(CAPABILITY).ifPresent(oripathy -> oripathy.set(value));
    }

    /** Negative deltas are treatment; the result is clamped to [MIN, MAX] either way. */
    public static void add(Player player, int delta) {
        player.getCapability(CAPABILITY).ifPresent(oripathy -> oripathy.add(delta));
    }

    static void registerCapability(RegisterCapabilitiesEvent event) {
        event.register(Oripathy.class);
    }
}
