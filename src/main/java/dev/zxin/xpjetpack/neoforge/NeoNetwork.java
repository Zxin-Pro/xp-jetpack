package dev.zxin.xpjetpack.neoforge;

import dev.zxin.xpjetpack.Bridge;
import dev.zxin.xpjetpack.client.PacketHandlers;
import dev.zxin.xpjetpack.logic.PacketCodec;
import dev.zxin.xpjetpack.network.InputPayload;
import dev.zxin.xpjetpack.network.StatePayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/** NeoForge 网络：原版 CustomPacketPayload + PayloadRegistrar。 */
public final class NeoNetwork {

    private NeoNetwork() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");

        registrar.playToServer(InputPayload.TYPE, InputPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    // 1.21+ 起 IPayloadContext#player() 返回 Player，需要自行收窄
                    if (context.player() instanceof ServerPlayer player) {
                        PacketCodec.onServerInput(player, PacketCodec.fromPayload(payload));
                    }
                }));

        registrar.playToClient(StatePayload.TYPE, StatePayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(
                        () -> PacketHandlers.onState(PacketCodec.fromPayload(payload))));

        Bridge.bindServerToClient((player, packet) ->
                PacketDistributor.sendToPlayer(player, PacketCodec.toPayload(packet)));
    }
}
