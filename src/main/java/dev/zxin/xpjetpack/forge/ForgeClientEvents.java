package dev.zxin.xpjetpack.forge;

import dev.zxin.xpjetpack.XpJetpack;
import dev.zxin.xpjetpack.client.JetpackClientLogic;
import dev.zxin.xpjetpack.client.hud.JetpackHud;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Forge 客户端事件：tick 驱动 + HUD 渲染。 */
@Mod.EventBusSubscriber(modid = XpJetpack.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ForgeClientEvents {

    private ForgeClientEvents() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        JetpackClientLogic.tick();
    }

    /** 1.20.1–1.20.4：整屏 GUI 渲染结束后绘制压缩空气条。 */
    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        JetpackHud.render(event.getGuiGraphics());
    }
}
