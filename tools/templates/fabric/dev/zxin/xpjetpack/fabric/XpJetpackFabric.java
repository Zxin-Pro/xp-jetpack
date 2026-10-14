package dev.zxin.xpjetpack.fabric;

import dev.zxin.xpjetpack.Bridge;
import dev.zxin.xpjetpack.XpJetpack;
import dev.zxin.xpjetpack.command.FlyCommand;
import dev.zxin.xpjetpack.command.SurvivalCommand;
import dev.zxin.xpjetpack.logic.JetpackServerLogic;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import org.slf4j.LoggerFactory;

/** Fabric 入口。 */
public final class XpJetpackFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        Bridge.bindLogger(message -> LoggerFactory.getLogger("XP Jetpack").info(message));
        Bridge.bindConfig(FabricConfig::load);
        FabricConfig.loadFromDisk();
        Bridge.reload();

        FabricNetworkServer.register();

        // 生存套件：事件桥（只做接线，新增功能无需改这里）
        FabricFeatureEvents.register();

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            FlyCommand.register(dispatcher);
            SurvivalCommand.register(dispatcher);
        });

        ServerTickEvents.END_SERVER_TICK.register(JetpackServerLogic::tick);

        Bridge.info("XP Jetpack 已加载 (Fabric)");
    }
}
