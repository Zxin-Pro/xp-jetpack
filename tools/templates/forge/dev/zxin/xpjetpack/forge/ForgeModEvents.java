package dev.zxin.xpjetpack.forge;

import dev.zxin.xpjetpack.Bridge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

/** Forge 模组总线事件：配置加载/重载。 */
@Mod.EventBusSubscriber(modid = dev.zxin.xpjetpack.XpJetpack.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ForgeModEvents {

    private ForgeModEvents() {
    }

    @SubscribeEvent
    public static void onConfigLoad(ModConfigEvent.Loading event) {
        Bridge.reload();
    }

    @SubscribeEvent
    public static void onConfigReload(ModConfigEvent.Reloading event) {
        Bridge.reload();
    }
}
