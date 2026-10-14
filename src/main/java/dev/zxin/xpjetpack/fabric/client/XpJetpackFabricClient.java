package dev.zxin.xpjetpack.fabric.client;

import dev.zxin.xpjetpack.Bridge;
import dev.zxin.xpjetpack.XpJetpack;
import dev.zxin.xpjetpack.client.JetpackClientLogic;
import dev.zxin.xpjetpack.client.hud.JetpackHud;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;

/** Fabric 客户端入口。同包直接访问 FabricNetworkClient。 */
public final class XpJetpackFabricClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        FabricNetworkClient.register();
        ClientTickEvents.END_CLIENT_TICK.register(client -> JetpackClientLogic.tick());

        // 1.21.6+ 起 HudRenderCallback 被 HudElementRegistry 取代（26.x 用 extractRenderState）
        HudElementRegistry.addLast(XpJetpack.id("hud"), (graphics, deltaTracker) -> JetpackHud.render(graphics));

    }
}
