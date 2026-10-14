package dev.zxin.xpjetpack.forge;

import dev.zxin.xpjetpack.Bridge;
import dev.zxin.xpjetpack.XpJetpack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.slf4j.LoggerFactory;

/** Forge 入口（1.20.1 / 1.20.2 / 1.20.4）。 */
@Mod(XpJetpack.MOD_ID)
public final class XpJetpackForge {

    public XpJetpackForge() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();

        Bridge.bindLogger(message -> LoggerFactory.getLogger("XP Jetpack").info(message));
        Bridge.bindConfig(ForgeConfig::load);
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, ForgeConfig.SPEC);

        // 直接注册（Forge 的 IEventBus#addListener 需要 Consumer，本方法无参，不能写成方法引用）
        ForgeNetwork.register();

        if (FMLEnvironment.dist.isClient()) {
            ForgeClientSetup.init();
        }
        Bridge.info("XP Jetpack 已加载 (Forge)");
    }
}
