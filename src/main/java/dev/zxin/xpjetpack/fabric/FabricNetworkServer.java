package dev.zxin.xpjetpack.fabric;

import dev.zxin.xpjetpack.Bridge;
import dev.zxin.xpjetpack.logic.PacketCodec;

import dev.zxin.xpjetpack.network.InputPayload;
import dev.zxin.xpjetpack.network.StatePayload;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

/** Fabric 服务端网络。 */
public final class FabricNetworkServer {

    private FabricNetworkServer() {
    }

    public static void register() {

        // Fabric API 26.x（networking-api-v1 6.x）把 playC2S/playS2C 改名为 serverboundPlay/clientboundPlay
        PayloadTypeRegistry.serverboundPlay().register(InputPayload.TYPE, InputPayload.STREAM_CODEC);
        PayloadTypeRegistry.clientboundPlay().register(StatePayload.TYPE, StatePayload.STREAM_CODEC);

        ServerPlayNetworking.registerGlobalReceiver(InputPayload.TYPE, (payload, context) ->
                context.server().execute(() ->
                        PacketCodec.onServerInput(context.player(), PacketCodec.fromPayload(payload))));

        Bridge.bindServerToClient((player, packet) ->
                ServerPlayNetworking.send(player, PacketCodec.toPayload(packet)));

    }
}
