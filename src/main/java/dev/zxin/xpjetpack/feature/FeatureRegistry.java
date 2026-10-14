package dev.zxin.xpjetpack.feature;

import dev.zxin.xpjetpack.Bridge;
import dev.zxin.xpjetpack.util.XpCost;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import dev.zxin.xpjetpack.player.JetState;
import dev.zxin.xpjetpack.player.PlayerStore;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * 功能注册表 —— 一切分发的中枢。
 *
 * <p>新增功能只需在 {@link Features#bootstrap()} 注册一次；
 * 配置声明、指令树、事件分发、状态存储全部由本表驱动。
 */
public final class FeatureRegistry {

    private static final Map<String, Feature> FEATURES = new LinkedHashMap<String, Feature>();
    private static boolean bootstrapped;

    private FeatureRegistry() {
    }

    /** 注册一个功能（按 id 幂等）。 */
    public static synchronized void register(Feature feature) {
        if (feature == null || feature.id() == null || feature.id().isEmpty()) {
            return;
        }
        if (FEATURES.containsKey(feature.id())) {
            Bridge.info("功能重复注册，已忽略: " + feature.id());
            return;
        }
        FEATURES.put(feature.id(), feature);
    }

    /** 启动引导：由加载器在初始化时调用一次。 */
    public static synchronized void ensureBootstrapped() {
        if (bootstrapped) {
            return;
        }
        bootstrapped = true;
        Features.bootstrap();
        Bridge.info("已注册功能 " + FEATURES.size() + " 个: " + String.join(", ", FEATURES.keySet()));
    }

    public static Collection<Feature> all() {
        return Collections.unmodifiableCollection(FEATURES.values());
    }

    public static List<Feature> list() {
        return new ArrayList<Feature>(FEATURES.values());
    }

    public static Feature get(String id) {
        return FEATURES.get(id);
    }

    public static boolean isEmpty() {
        return FEATURES.isEmpty();
    }

    // ------------------------------------------------------------------
    // 配置声明
    // ------------------------------------------------------------------

    /** 让每个功能声明自己的配置键。 */
    public static void declareAllConfig(ConfigFacade cfg) {
        ensureBootstrapped();
        GlobalConfig.declare(cfg);
        for (Feature feature : FEATURES.values()) {
            try {
                feature.declareConfig(cfg);
            } catch (Throwable t) {
                Bridge.info("功能 " + feature.id() + " 声明配置失败: " + t);
            }
        }
    }

    // ------------------------------------------------------------------
    // 事件分发
    // ------------------------------------------------------------------

    /** 是否应当运行：总开关 + 功能级 enabled + 玩家状态 + 自动重开。 */
    public static boolean isRunnable(FeatureContext ctx, Feature feature) {
        if (!FeatureConfig.bool(feature.id() + ".enabled", feature.defaultEnabled())) {
            return false;
        }
        FeatureState st = ctx.featureState(feature);
        if (!st.enabled && GlobalConfig.autoReenable()) {
            boolean blockedByManual = st.manualDisabled && GlobalConfig.respectManualToggle();
            if (!blockedByManual && XpCost.available(ctx.player()) >= 1) {
                st.enabled = true;
                st.exhaustedNotified = false;
                ctx.touch();
            }
        }
        return st.enabled;
    }

    /** 每 tick 分发。 */
    public static void tick(FeatureContext ctx) {
        ensureBootstrapped();
        for (Feature feature : FEATURES.values()) {
            try {
                if (!isRunnable(ctx, feature)) {
                    continue;
                }
                feature.onServerTick(ctx);
            } catch (Throwable t) {
                Bridge.info("功能 " + feature.id() + " tick 异常: " + t);
            }
        }
    }

    /** 伤害分发。返回是否被功能修改过。 */
    /**
     * 为某个玩家构造上下文 —— <b>加载器事件钩子专用</b>。
     *
     * <p>有了它，三个加载器的事件代码都只需要两三行，不必各自重复
     * 「取 server / 取 store / 取 state / 取配置」这套样板。
     */
    public static FeatureContext contextFor(ServerPlayer player) {
        MinecraftServer server = player.level().getServer();
        if (server == null) {
            return null;
        }
        PlayerStore store = PlayerStore.get(server);
        JetState state = store.get(player.getUUID());
        return new FeatureContext(player, state, store, Bridge.config(), server.getTickCount());
    }

    /** 加载器钩子：伤害事件（返回 true 表示功能改动过伤害值）。 */
    public static boolean damage(ServerPlayer player, DamageInfo info) {
        FeatureContext ctx = contextFor(player);
        if (ctx == null) {
            return false;
        }
        return damage(ctx, info);
    }

    /** 加载器钩子：挖掘事件。 */
    public static void blockBreak(ServerPlayer player, BlockBreakInfo info) {
        FeatureContext ctx = contextFor(player);
        if (ctx == null) {
            return;
        }
        blockBreak(ctx, info);
    }

    public static boolean damage(FeatureContext ctx, DamageInfo info) {
        ensureBootstrapped();
        boolean touched = false;
        for (Feature feature : FEATURES.values()) {
            try {
                if (!isRunnable(ctx, feature)) {
                    continue;
                }
                double before = info.amount;
                boolean cancelledBefore = info.cancelled;
                feature.onDamage(ctx, info);
                if (before != info.amount || cancelledBefore != info.cancelled) {
                    touched = true;
                }
            } catch (Throwable t) {
                Bridge.info("功能 " + feature.id() + " 伤害处理异常: " + t);
            }
        }
        return touched;
    }

    /** 挖掘分发。 */
    public static void blockBreak(FeatureContext ctx, BlockBreakInfo info) {
        ensureBootstrapped();
        for (Feature feature : FEATURES.values()) {
            try {
                if (!isRunnable(ctx, feature)) {
                    continue;
                }
                feature.onBlockBreak(ctx, info);
            } catch (Throwable t) {
                Bridge.info("功能 " + feature.id() + " 挖掘处理异常: " + t);
            }
        }
    }
}
