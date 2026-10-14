package dev.zxin.xpjetpack.forge;

import dev.zxin.xpjetpack.Bridge;
import dev.zxin.xpjetpack.command.FlyCommand;
import dev.zxin.xpjetpack.command.SurvivalCommand;
import dev.zxin.xpjetpack.logic.JetpackServerLogic;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.ServerLifecycleHooks;

/** Forge 游戏总线事件。 */
@Mod.EventBusSubscriber(modid = dev.zxin.xpjetpack.XpJetpack.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ForgeEvents {

    private ForgeEvents() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        FlyCommand.register(event.getDispatcher());
        SurvivalCommand.register(event.getDispatcher());
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        JetpackServerLogic.tick(ServerLifecycleHooks.getCurrentServer());
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent event) {
        Bridge.info("玩家退出: " + event.getEntity().getName().getString());
    }
}
