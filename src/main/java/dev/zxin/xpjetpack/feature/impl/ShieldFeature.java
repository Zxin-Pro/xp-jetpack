package dev.zxin.xpjetpack.feature.impl;

import dev.zxin.xpjetpack.feature.ConfigFacade;
import dev.zxin.xpjetpack.feature.DamageInfo;
import dev.zxin.xpjetpack.feature.Feature;
import dev.zxin.xpjetpack.feature.FeatureConfig;
import dev.zxin.xpjetpack.feature.FeatureContext;
import dev.zxin.xpjetpack.feature.FeatureState;
import dev.zxin.xpjetpack.util.FeatureNotifier;
import dev.zxin.xpjetpack.util.XpCost;

import net.minecraft.network.chat.Component;

/**
 * 经验护盾：受到伤害时按比例抵挡，用经验支付。
 *
 * <p>默认抵挡 50% 伤害，每抵挡 1 点伤害消耗 4 点经验（均可配置）。
 * <b>无任何音效</b>，提示走聊天栏，且默认按 40 tick 窗口合并，避免刷屏。
 */
public final class ShieldFeature implements Feature {

    @Override
    public String id() {
        return "shield";
    }

    @Override
    public Component displayName() {
        return Component.translatable("xpjetpack.feature.shield");
    }

    @Override
    public String commandName() {
        return "shield";
    }

    @Override
    public void declareConfig(ConfigFacade cfg) {
        cfg.bool("shield.enabled", "是否开启经验护盾（自动运行）", true);
        cfg.dbl("shield.blockRatio", "抵挡比例（0~1，默认 0.5 即抵挡一半伤害）", 0.5D, 0.0D, 1.0D);
        cfg.dbl("shield.costPerDamage", "每抵挡 1 点伤害消耗的经验点", 4.0D, 0.0D, 1000.0D);
        cfg.str("shield.notifyMode", "提示频率：every（逐条）/ merged（合并）/ broken（仅失效时）", "merged");
        cfg.bool("shield.soundEnabled", "音效开关（本模组固定关闭，不播放任何音效）", false);
    }

    @Override
    public void onDamage(FeatureContext ctx, DamageInfo info) {
        if (info.cancelled || info.amount <= 0.0D) {
            return;
        }
        double ratio = FeatureConfig.dbl("shield.blockRatio", 0.5D);
        if (ratio <= 0.0D) {
            return;
        }

        double blocked = info.amount * ratio;
        double cost = blocked * FeatureConfig.dbl("shield.costPerDamage", 4.0D);

        FeatureState st = ctx.featureState(this);
        if (!XpCost.pay(ctx.player(), st, cost)) {
            FeatureNotifier.notifyExhausted(ctx.player(), st, "经验护盾");
            return;
        }

        if (info.postApplied) {
            // 伤害已结算（Fabric 侧只能事后回调）：按抵挡量回补生命
            ctx.player().heal((float) blocked);
        } else {
            // 伤害尚未结算：直接削掉被抵挡的部分；剩 0 时由加载器侧取消事件
            info.amount = Math.max(0.0D, info.amount - blocked);
        }

        FeatureNotifier.notifyShield(ctx.player(), st,
                FeatureConfig.str("shield.notifyMode", "merged"),
                String.format("经验护盾抵挡了 %.1f 点伤害，消耗 %.0f 经验", blocked, cost));
    }

    @Override
    public void onServerTick(FeatureContext ctx) {
        FeatureState st = ctx.featureState(this);
        String mode = FeatureConfig.str("shield.notifyMode", "merged");
        // merged 模式下把窗口内累积的抵挡汇总成一条
        FeatureNotifier.flushShield(ctx.player(), st, mode);
        FeatureNotifier.markShieldTick(ctx.player(), st);
    }

    @Override
    public Component statusLine(FeatureContext ctx) {
        double ratio = FeatureConfig.dbl("shield.blockRatio", 0.5D);
        double cost = FeatureConfig.dbl("shield.costPerDamage", 4.0D);
        return Component.literal(String.format("抵挡 %.0f%% · 每点伤害 %s 经验", ratio * 100.0D, trim(cost)));
    }

    private static String trim(double value) {
        if (value == Math.floor(value) && !Double.isInfinite(value)) {
            return String.valueOf((long) value);
        }
        return String.valueOf(value);
    }
}
