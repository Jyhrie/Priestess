package com.jyhrie.priestess.oripathy;

import com.jyhrie.priestess.Priestess;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.Nullable;

/**
 * Holds one player's {@link Oripathy} and hands it out through the capability system.
 * Attached to every Player in {@link OripathyEvents#attachCapability}.
 */
public class OripathyProvider implements ICapabilitySerializable<CompoundTag> {

    public static final ResourceLocation ID = new ResourceLocation(Priestess.MOD_ID, "oripathy");

    private final Oripathy oripathy = new Oripathy();

    /**
     * Deliberately never invalidated. Forge only ever calls {@code invalidateCaps()} on a
     * player — it has no matching hook to hand back to a provider on {@code reviveCaps()},
     * so a {@link LazyOptional} killed once is empty forever. That would silently turn
     * every read into {@link Oripathy#MIN} and every write into a no-op the moment the
     * player died or changed dimension, which is exactly when {@link OripathyEvents#copyOnClone}
     * needs to read the value back out. Nothing caches this handle, so there is nothing
     * that needs telling when the player goes away.
     */
    private final LazyOptional<Oripathy> handle = LazyOptional.of(() -> oripathy);

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> capability, @Nullable Direction side) {
        return capability == Oripathy.CAPABILITY ? handle.cast() : LazyOptional.empty();
    }

    @Override
    public CompoundTag serializeNBT() {
        return oripathy.save();
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        oripathy.load(tag);
    }
}
