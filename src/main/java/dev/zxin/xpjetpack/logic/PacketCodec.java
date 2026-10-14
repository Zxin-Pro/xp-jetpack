package dev.zxin.xpjetpack.logic;

import dev.zxin.xpjetpack.network.InputPacket;
import dev.zxin.xpjetpack.network.StatePacket;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/** 把两种线格式统一成 DTO，供加载器薄封装调用。 */
public final class PacketCodec {

    private PacketCodec() {
    }

    // ---- 1.20.5+ 原版载荷（服务端 / 客户端共用） ----

    // ---- 旧版裸缓冲区 ----

    public static void write(InputPacket packet, FriendlyByteBuf buf) {
        InputPacket.encode(packet, buf);
    }

    public static InputPacket readInput(FriendlyByteBuf buf) {
        return InputPacket.decode(buf);
    }

    public static void write(StatePacket packet, FriendlyByteBuf buf) {
        StatePacket.encode(packet, buf);
    }

    public static StatePacket readState(FriendlyByteBuf buf) {
        return StatePacket.decode(buf);
    }

    // ---- 统一入口 ----

    public static void onServerInput(ServerPlayer player, InputPacket packet) {
        if (player == null || packet == null) {
            return;
        }
        MinecraftServer server = player.level().getServer();
        if (server == null) {
            return;
        }
        ServerNetworkHandler.onInput(server, player, packet);
    }
}
