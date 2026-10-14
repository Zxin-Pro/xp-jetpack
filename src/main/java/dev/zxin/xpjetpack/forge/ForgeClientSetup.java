package dev.zxin.xpjetpack.forge;

import dev.zxin.xpjetpack.Bridge;
import dev.zxin.xpjetpack.client.JetpackClientLogic;

import net.minecraftforge.network.PacketDistributor;

/** 仅客户端：绑定 C2S 发送器。 */
public final class ForgeClientSetup {

    private ForgeClientSetup() {
    }

    public static void init() {

        // 1.20.2 起 SimpleChannel#sendToServer 被移除，改用 SERVER 目标点
        Bridge.bindClientToServer(packet ->
                ForgeNetwork.CHANNEL.send(packet, PacketDistributor.SERVER.noArg()));

        JetpackClientLogic.reset();
    }
}
