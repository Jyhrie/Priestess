package com.jyhrie.priestess.weapons.network;

import com.jyhrie.priestess.weapons.item.DevilsDevastationItem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * "The player swung a weapon that throws something."
 *
 * <p>Carries no payload at all, deliberately. The client is not trusted to say <em>which</em>
 * weapon or how hard — the server re-reads the held stack and decides, so a modified client
 * can at most ask to swing something it is already holding, at the cadence its own cooldown
 * allows.
 *
 * <p>An empty packet still needs the encoder, the {@link FriendlyByteBuf} constructor and the
 * no-arg constructor: {@code SimpleChannel} wires all three by method reference and does not
 * care that two of them do nothing.
 */
public class SwingSlashC2S {

    public SwingSlashC2S() {
    }

    /** Decoder. No payload to read. */
    public SwingSlashC2S(FriendlyByteBuf buf) {
    }

    /** Encoder. No payload to write. */
    public void toBytes(FriendlyByteBuf buf) {
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            // Null when the sender has disconnected between sending and the work running.
            if (player != null) {
                dispatch(player);
            }
        });
        context.setPacketHandled(true);
    }

    /**
     * Offers the swing to every weapon that responds to one. Each is responsible for checking
     * that it is the thing being held and no-opping otherwise, so adding a weapon here is one
     * line and needs no dispatch table.
     */
    private static void dispatch(ServerPlayer player) {
        DevilsDevastationItem.fireFan(player.level(), player);
    }
}
