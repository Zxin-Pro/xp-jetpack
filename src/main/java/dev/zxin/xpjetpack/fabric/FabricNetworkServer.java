package dev.zxin.xpjetpack.fabric;

import dev.zxin.xpjetpack.Bridge;
import dev.zxin.xpjetpack.logic.PacketCodec;

import dev.zxin.xpjetpack.XpJetpack;
import dev.zxin.xpjetpack.network.InputPacket;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

/** Fabric 服务端网络。 */
public final class FabricNetworkServer {

    public static final ResourceLocation INPUT_ID = XpJetpack.id("input");
    public static final ResourceLocation STATE_ID = XpJetpack.id("state");

    private FabricNetworkServer() {
    }

    public static void register() {

        ServerPlayNetworking.registerGlobalReceiver(INPUT_ID, (server, player, handler, buf, sender) -> {
            InputPacket message = PacketCodec.readInput(buf);
            server.execute(() -> PacketCodec.onServerInput(player, message));
        });

        Bridge.bindServerToClient((player, packet) -> {
            FriendlyByteBuf buf = PacketByteBufs.create();
            PacketCodec.write(packet, buf);
            ServerPlayNetworking.send(player, STATE_ID, buf);
        });

    }
}
