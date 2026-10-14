package dev.zxin.xpjetpack.logic;

import dev.zxin.xpjetpack.network.InputPacket;
import dev.zxin.xpjetpack.player.JetState;
import dev.zxin.xpjetpack.player.PlayerStore;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/** 服务端收到输入包后的入口。所有判定都放在权威侧。 */
public final class ServerNetworkHandler {

    private ServerNetworkHandler() {
    }

    public static void onInput(MinecraftServer server, ServerPlayer player, InputPacket packet) {
        PlayerStore store = PlayerStore.get(server);
        JetState state = store.get(player.getUUID());
        state.thrusting = packet.thrusting();
        state.inputTicks = 0;
        if (!packet.thrusting()) {
            state.exhaustedNotified = false;
        }
        store.markDirty();
    }
}
