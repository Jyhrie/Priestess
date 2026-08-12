package com.jyhrie.priestess.weapons.network;

import com.jyhrie.priestess.Priestess;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

/**
 * The weapons package's own network channel.
 *
 * <p>Separate from anything else the mod might send, because this compartment is meant to be
 * removable — a channel registered here disappears with the folder. {@code DungeonSync} has
 * its own arrangements and the two never meet.
 *
 * <p>Protocol version is pinned but both accept-predicates return true, so a client and server
 * that disagree still connect. That is the right call for a cosmetic-plus-projectiles channel:
 * the failure mode of a mismatch is a swing that does not throw anything, not a corrupt world,
 * and refusing the connection outright would be worse.
 */
public final class WeaponNetwork {

    private static final String PROTOCOL_VERSION = "1.0";

    private static SimpleChannel channel;

    private static int nextId;

    public static void register() {
        channel = NetworkRegistry.ChannelBuilder
                .named(new ResourceLocation(Priestess.MOD_ID, "weapons"))
                .networkProtocolVersion(() -> PROTOCOL_VERSION)
                .clientAcceptedVersions(v -> true)
                .serverAcceptedVersions(v -> true)
                .simpleChannel();

        channel.messageBuilder(SwingSlashC2S.class, nextId++, NetworkDirection.PLAY_TO_SERVER)
                .decoder(SwingSlashC2S::new)
                .encoder(SwingSlashC2S::toBytes)
                .consumerMainThread(SwingSlashC2S::handle)
                .add();
    }

    public static <T> void sendToServer(T message) {
        channel.sendToServer(message);
    }

    private WeaponNetwork() {
    }
}
