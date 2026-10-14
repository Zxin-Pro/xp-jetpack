package dev.zxin.xpjetpack.forge;

import dev.zxin.xpjetpack.Bridge;
import dev.zxin.xpjetpack.client.JetpackClientLogic;

/** 仅客户端：绑定 C2S 发送器。 */
public final class ForgeClientSetup {

    private ForgeClientSetup() {
    }

    public static void init() {

        Bridge.bindClientToServer(packet -> ForgeNetwork.CHANNEL.sendToServer(packet));

        JetpackClientLogic.reset();
    }
}
