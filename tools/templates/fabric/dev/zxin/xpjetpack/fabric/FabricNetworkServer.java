package dev.zxin.xpjetpack.fabric;

import dev.zxin.xpjetpack.Bridge;
import dev.zxin.xpjetpack.logic.PacketCodec;

//#if MC>=12005
import dev.zxin.xpjetpack.network.InputPayload;
import dev.zxin.xpjetpack.network.StatePayload;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
//#else
import dev.zxin.xpjetpack.XpJetpack;
import dev.zxin.xpjetpack.network.InputPacket;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
//#endif

/** Fabric 服务端网络。 */
public final class FabricNetworkServer {

    //#if MC<12005
    public static final ResourceLocation INPUT_ID = XpJetpack.id("input");
    public static final ResourceLocation STATE_ID = XpJetpack.id("state");
    //#endif

    private FabricNetworkServer() {
    }

    public static void register() {
        //#if MC>=12005
        //#if MC>=26000
        // Fabric API 26.x（networking-api-v1 6.x）把 playC2S/playS2C 改名为 serverboundPlay/clientboundPlay
        PayloadTypeRegistry.serverboundPlay().register(InputPayload.TYPE, InputPayload.STREAM_CODEC);
        PayloadTypeRegistry.clientboundPlay().register(StatePayload.TYPE, StatePayload.STREAM_CODEC);
        //#else
        PayloadTypeRegistry.playC2S().register(InputPayload.TYPE, InputPayload.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(StatePayload.TYPE, StatePayload.STREAM_CODEC);
        //#endif

        ServerPlayNetworking.registerGlobalReceiver(InputPayload.TYPE, (payload, context) ->
                context.server().execute(() ->
                        PacketCodec.onServerInput(context.player(), PacketCodec.fromPayload(payload))));

        Bridge.bindServerToClient((player, packet) ->
                ServerPlayNetworking.send(player, PacketCodec.toPayload(packet)));
        //#else
        ServerPlayNetworking.registerGlobalReceiver(INPUT_ID, (server, player, handler, buf, sender) -> {
            InputPacket message = PacketCodec.readInput(buf);
            server.execute(() -> PacketCodec.onServerInput(player, message));
        });

        Bridge.bindServerToClient((player, packet) -> {
            FriendlyByteBuf buf = PacketByteBufs.create();
            PacketCodec.write(packet, buf);
            ServerPlayNetworking.send(player, STATE_ID, buf);
        });
        //#endif
    }
}
