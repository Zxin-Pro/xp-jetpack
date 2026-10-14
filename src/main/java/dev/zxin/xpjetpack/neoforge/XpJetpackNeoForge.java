package dev.zxin.xpjetpack.neoforge;

import dev.zxin.xpjetpack.Bridge;
import dev.zxin.xpjetpack.XpJetpack;
import dev.zxin.xpjetpack.command.FlyCommand;
import dev.zxin.xpjetpack.command.SurvivalCommand;
import dev.zxin.xpjetpack.logic.JetpackServerLogic;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import net.neoforged.fml.loading.FMLEnvironment;

@Mod(XpJetpack.MOD_ID)
public final class XpJetpackNeoForge {

    public XpJetpackNeoForge(IEventBus modBus, ModContainer container) {
        Bridge.bindLogger(message -> org.slf4j.LoggerFactory.getLogger("XP Jetpack").info(message));
        Bridge.bindConfig(NeoConfig::load);

        container.registerConfig(ModConfig.Type.COMMON, NeoConfig.SPEC);

        modBus.addListener(NeoNetwork::register);
        modBus.addListener((ModConfigEvent.Loading event) -> Bridge.reload());
        modBus.addListener((ModConfigEvent.Reloading event) -> Bridge.reload());

        // 生存套件：事件桥（只做接线，新增功能无需改这里）
        NeoFeatureEvents.register();

        NeoForge.EVENT_BUS.addListener((RegisterCommandsEvent event) -> {
            FlyCommand.register(event.getDispatcher());
            SurvivalCommand.register(event.getDispatcher());
        });
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post event) -> JetpackServerLogic.tick(event.getServer()));

        if (FMLEnvironment.dist.isClient()) {
            NeoClientSetup.init();
        }

        Bridge.info("XP Jetpack 已加载 (NeoForge)");
    }
}
