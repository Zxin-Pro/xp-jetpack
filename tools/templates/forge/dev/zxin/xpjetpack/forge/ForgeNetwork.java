package dev.zxin.xpjetpack.forge;

import dev.zxin.xpjetpack.Bridge;
import dev.zxin.xpjetpack.XpJetpack;
import dev.zxin.xpjetpack.client.PacketHandlers;
import dev.zxin.xpjetpack.logic.PacketCodec;
import dev.zxin.xpjetpack.network.InputPacket;
import dev.zxin.xpjetpack.network.StatePacket;
import net.minecraftforge.network.PacketDistributor;

//#if MC>=12002
// Forge 1.20.2 起：SimpleChannel 移到 net.minecraftforge.network，旧 newSimpleChannel 被移除
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.SimpleChannel;
//#else
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
//#endif

/** Forge 网络。1.20.1 用旧 SimpleChannel API；1.20.2+ 用 ChannelBuilder + messageBuilder。 */
public final class ForgeNetwork {

    private static final String PROTOCOL = "1";

    //#if MC>=12002
    public static final SimpleChannel CHANNEL = ChannelBuilder
            .named(XpJetpack.id("main"))
            .networkProtocolVersion(1)
            .clientAcceptedVersions((status, version) -> true)
            .serverAcceptedVersions((status, version) -> true)
            .simpleChannel();
    //#else
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            XpJetpack.id("main"), () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);
    //#endif

    private ForgeNetwork() {
    }

    public static void register() {
        //#if MC>=12002
        CHANNEL.messageBuilder(InputPacket.class, 0)
                .encoder(InputPacket::encode)
                .decoder(InputPacket::decode)
                .consumerMainThread((message, context) ->
                        PacketCodec.onServerInput(context.getSender(), message))
                .add();

        CHANNEL.messageBuilder(StatePacket.class, 1)
                .encoder(StatePacket::encode)
                .decoder(StatePacket::decode)
                .consumerMainThread((message, context) -> PacketHandlers.onState(message))
                .add();

        // 注意参数顺序：Channel#send(message, target)
        Bridge.bindServerToClient((player, packet) ->
                CHANNEL.send(packet, PacketDistributor.PLAYER.with(player)));
        //#else
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
        //#endif
    }
}
