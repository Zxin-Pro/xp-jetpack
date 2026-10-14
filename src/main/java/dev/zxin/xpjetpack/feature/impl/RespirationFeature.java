package dev.zxin.xpjetpack.feature.impl;

import dev.zxin.xpjetpack.Bridge;
import dev.zxin.xpjetpack.feature.ConfigFacade;
import dev.zxin.xpjetpack.feature.Feature;
import dev.zxin.xpjetpack.feature.FeatureConfig;
import dev.zxin.xpjetpack.feature.FeatureContext;
import dev.zxin.xpjetpack.feature.FeatureState;
import dev.zxin.xpjetpack.util.FeatureNotifier;
import dev.zxin.xpjetpack.util.XpCost;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

/**
 * 水下呼吸：在水下自动消耗经验维持呼吸。
 *
 * <p>只给「水下呼吸」效果，<b>不提供夜视</b>；效果隐藏、无粒子。
 * 经验不足即停用并提示。
 *
 * <p>跨版本：{@code isUnderWater()} / {@code MobEffectInstance} 6 参构造在
 * 1.20.1 → 26.3 全部版本签名一致，无需版本分叉。
 */
public final class RespirationFeature implements Feature {

    /** 效果续期时长（tick）。离开水后约 2 秒自然失效。 */
    private static final int REFRESH_TICKS = 40;

    @Override
    public String id() {
        return "respiration";
    }

    @Override
    public Component displayName() {
        return Component.translatable("xpjetpack.feature.respiration");
    }

    @Override
    public Component description() {
        return Component.translatable("xpjetpack.feature.respiration.desc");
    }

    @Override
    public void declareConfig(ConfigFacade cfg) {
        cfg.bool("respiration.enabled", "是否启用「水下呼吸」功能", true);
        cfg.dbl("respiration.costPerSecond", "水下每秒消耗的经验点", 2.0D, 0.0D, 200.0D);
    }

    @Override
    public void onServerTick(FeatureContext ctx) {
        ServerPlayer player = ctx.player();
        FeatureState st = ctx.featureState(this);

        // 前置条件：头部浸在水中（此时原版才会消耗氧气）
        if (!player.isUnderWater()) {
            st.exhaustedNotified = false;
            st.accumulator = 0.0D;
            return;
        }

        double perSecond = FeatureConfig.dbl("respiration.costPerSecond", 2.0D);
        if (!XpCost.payPerSecond(player, st, perSecond)) {
            FeatureNotifier.notifyExhausted(player, st, "水下呼吸");
            return;
        }

        st.exhaustedNotified = false;
        // 隐藏效果：ambient=false, visible=false, showIcon=false
        player.addEffect(new MobEffectInstance(
                MobEffects.WATER_BREATHING, REFRESH_TICKS, 0, false, false, false));
        ctx.touch();
    }

    @Override
    public Component statusLine(FeatureContext ctx) {
        FeatureState st = ctx.featureState(this);
        String state = st.enabled ? "开" : "关";
        return Component.literal(String.format("水下呼吸：%s · %.1f 点/秒 · %s",
                state,
                FeatureConfig.dbl("respiration.costPerSecond", 2.0D),
                ctx.player().isUnderWater() ? "生效中" : "待机"));
    }

    /** 供指令展示用。 */
    public static boolean enabledByConfig() {
        return FeatureConfig.bool("respiration.enabled", true);
    }

    @SuppressWarnings("unused")
    private static void log(String message) {
        Bridge.info(message);
    }
}
