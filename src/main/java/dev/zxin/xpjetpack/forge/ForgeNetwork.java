package dev.zxin.xpjetpack.forge;

import dev.zxin.xpjetpack.Bridge;
import dev.zxin.xpjetpack.XpJetpack;
import dev.zxin.xpjetpack.client.PacketHandlers;
import dev.zxin.xpjetpack.logic.PacketCodec;
import dev.zxin.xpjetpack.network.InputPacket;
import dev.zxin.xpjetpack.network.StatePacket;
import net.minecraftforge.network.PacketDistributor;

import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

/** Forge 网络。1.20.1 用旧 SimpleChannel API；1.20.2+ 用 ChannelBuilder + messageBuilder。 */
public final class ForgeNetwork {

    private static final String PROTOCOL = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            XpJetpack.id("main"), () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);

    private ForgeNetwork() {
    }

    public static void register() {

        CHANNEL.registerMessage(0, InputPacket.class, InputPacket::encode, InputPacket::decode,
                (message, context) -> {
                    context.get().enqueueWork(() ->
                            PacketCodec.onServerInput(context.get().getSender(), message));
                    context.get().setPacketHandled(true);
                });

        CHANNEL.registerMessage(1, StatePacket.class, StatePacket::encode, StatePacket::decode,
                (message, context) -> {
                    context.get().enqueueWork(() -> PacketHandlers.onState(message));
                    context.get().setPacketHandled(true);
                });

        Bridge.bindServerToClient((player, packet) ->
                CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet));

    }
}
