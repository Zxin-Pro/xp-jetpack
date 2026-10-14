package dev.zxin.xpjetpack.client;

import dev.zxin.xpjetpack.network.StatePacket;

/** 客户端收包落点。只允许在客户端类加载。 */
public final class PacketHandlers {

    private PacketHandlers() {
    }

    public static void onState(StatePacket packet) {
        ClientState.INSTANCE.apply(packet);
    }

    public static void onStatePayload(Object payload) {
        if (payload instanceof StatePacket) {
            ClientState.INSTANCE.apply((StatePacket) payload);
        }
    }
}
