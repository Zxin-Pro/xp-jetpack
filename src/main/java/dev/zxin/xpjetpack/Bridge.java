package dev.zxin.xpjetpack;

import dev.zxin.xpjetpack.network.InputPacket;
import dev.zxin.xpjetpack.network.StatePacket;
import net.minecraft.server.level.ServerPlayer;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * 平台桥：把加载器相关的能力（配置读写、发包、日志）注入到共用代码里。
 * 加载器初始化时必须调用 {@link #bind} 完成注入。
 */
public final class Bridge {

    private static volatile Supplier<dev.zxin.xpjetpack.config.JetpackConfig> configLoader =
            dev.zxin.xpjetpack.config.JetpackConfig::defaults;
    private static volatile dev.zxin.xpjetpack.config.JetpackConfig config =
            dev.zxin.xpjetpack.config.JetpackConfig.defaults();

    private static volatile Consumer<InputPacket> toServer = packet -> {
    };
    private static volatile BiConsumer<ServerPlayer, StatePacket> toClient = (player, packet) -> {
    };
    private static volatile Consumer<String> logger = message -> System.out.println("[XP Jetpack] " + message);

    private Bridge() {
    }

    public static void bind(Supplier<dev.zxin.xpjetpack.config.JetpackConfig> loader,
                            Consumer<InputPacket> clientToServer,
                            BiConsumer<ServerPlayer, StatePacket> serverToClient,
                            Consumer<String> log) {
        configLoader = loader;
        toServer = clientToServer;
        toClient = serverToClient;
        logger = log;
        reload();
    }

    public static void bindConfig(Supplier<dev.zxin.xpjetpack.config.JetpackConfig> loader) {
        configLoader = loader;
        reload();
    }

    /** 服务端侧绑定（S2C 发送器）。 */
    public static void bindServerToClient(BiConsumer<ServerPlayer, StatePacket> serverToClient) {
        toClient = serverToClient;
    }

    /** 客户端侧绑定（C2S 发送器）。只能在客户端调用。 */
    public static void bindClientToServer(Consumer<InputPacket> clientToServer) {
        toServer = clientToServer;
    }

    public static void bindLogger(Consumer<String> log) {
        logger = log;
    }

    /** 重新从加载器配置读取并校验。 */
    public static void reload() {
        dev.zxin.xpjetpack.config.JetpackConfig loaded = configLoader.get();
        if (loaded == null) {
            loaded = dev.zxin.xpjetpack.config.JetpackConfig.defaults();
        }
        loaded.clamp();
        config = loaded;
        info("配置已加载: ticksPerPoint=" + loaded.xpTicksPerPoint
                + " elytraMult=" + loaded.xpElytraMultiplier
                + " vMax=" + loaded.verticalMaxSpeed
                + " elytraMax=" + loaded.elytraMaxSpeed);
    }

    public static dev.zxin.xpjetpack.config.JetpackConfig config() {
        return config;
    }

    public static void sendToServer(InputPacket packet) {
        toServer.accept(packet);
    }

    public static void sendToClient(ServerPlayer player, StatePacket packet) {
        toClient.accept(player, packet);
    }

    public static void info(String message) {
        logger.accept(message);
    }
}
