package dev.zxin.xpjetpack.neoforge;

import dev.zxin.xpjetpack.Bridge;
import dev.zxin.xpjetpack.client.JetpackClientLogic;
import dev.zxin.xpjetpack.client.hud.JetpackHud;
import dev.zxin.xpjetpack.logic.PacketCodec;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.common.NeoForge;

//#if MC>=12105
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
//#else
import net.neoforged.neoforge.network.PacketDistributor;
//#endif

/** 仅客户端：绑定 C2S 发送器 + 客户端 tick + HUD。 */
public final class NeoClientSetup {

    private NeoClientSetup() {
    }

    public static void init() {
        //#if MC>=12105
        Bridge.bindClientToServer(packet -> ClientPacketDistributor.sendToServer(PacketCodec.toPayload(packet)));
        //#else
        Bridge.bindClientToServer(packet -> PacketDistributor.sendToServer(PacketCodec.toPayload(packet)));
        //#endif

        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post event) -> JetpackClientLogic.tick());

        // 26.x 起 getGuiGraphics() 返回 GuiGraphicsExtractor，JetpackHud 有对应重载
        NeoForge.EVENT_BUS.addListener((RenderGuiLayerEvent.Post event) -> JetpackHud.render(event.getGuiGraphics()));
    }
}
