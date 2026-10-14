package dev.zxin.xpjetpack.fabric.client;

import dev.zxin.xpjetpack.Bridge;
import dev.zxin.xpjetpack.client.PacketHandlers;
import dev.zxin.xpjetpack.logic.PacketCodec;

//#if MC>=12005
import dev.zxin.xpjetpack.network.InputPayload;
import dev.zxin.xpjetpack.network.StatePayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
//#else
import dev.zxin.xpjetpack.fabric.FabricNetworkServer;
import dev.zxin.xpjetpack.network.StatePacket;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.network.FriendlyByteBuf;
//#endif

/** Fabric 客户端网络。只允许在客户端类加载。 */
public final class FabricNetworkClient {

    private FabricNetworkClient() {
    }

    public static void register() {
        //#if MC>=12005
        ClientPlayNetworking.registerGlobalReceiver(StatePayload.TYPE, (payload, context) ->
                context.client().execute(() -> PacketHandlers.onState(PacketCodec.fromPayload(payload))));

        Bridge.bindClientToServer(packet -> ClientPlayNetworking.send(PacketCodec.toPayload(packet)));
        //#else
        ClientPlayNetworking.registerGlobalReceiver(FabricNetworkServer.STATE_ID, (client, handler, buf, sender) -> {
            StatePacket message = PacketCodec.readState(buf);
            client.execute(() -> PacketHandlers.onState(message));
        });

        Bridge.bindClientToServer(packet -> {
            FriendlyByteBuf buf = PacketByteBufs.create();
            PacketCodec.write(packet, buf);
            ClientPlayNetworking.send(FabricNetworkServer.INPUT_ID, buf);
        });
        //#endif
    }
}
