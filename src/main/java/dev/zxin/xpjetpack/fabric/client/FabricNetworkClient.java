package dev.zxin.xpjetpack.fabric.client;

import dev.zxin.xpjetpack.Bridge;
import dev.zxin.xpjetpack.client.PacketHandlers;
import dev.zxin.xpjetpack.logic.PacketCodec;

import dev.zxin.xpjetpack.network.InputPayload;
import dev.zxin.xpjetpack.network.StatePayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

/** Fabric 客户端网络。只允许在客户端类加载。 */
public final class FabricNetworkClient {

    private FabricNetworkClient() {
    }

    public static void register() {

        ClientPlayNetworking.registerGlobalReceiver(StatePayload.TYPE, (payload, context) ->
                context.client().execute(() -> PacketHandlers.onState(PacketCodec.fromPayload(payload))));

        Bridge.bindClientToServer(packet -> ClientPlayNetworking.send(PacketCodec.toPayload(packet)));

    }
}
