package dev.zxin.xpjetpack.feature.impl;

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
 * 防火：在火焰/岩浆中自动消耗经验免疫火焰伤害。
 *
 * <p>通过隐藏的「抗火」效果实现免疫（与原版抗火药水同源），
 * 可选在着火时立即灭火。
 *
 * <p>跨版本：{@code isOnFire()} / {@code isInLava()} / {@code clearFire()} 在所有目标版本一致。
 */
public final class FireproofFeature implements Feature {

    private static final int REFRESH_TICKS = 40;

    @Override
    public String id() {
        return "fireproof";
    }

    @Override
    public Component displayName() {
        return Component.translatable("xpjetpack.feature.fireproof");
    }

    @Override
    public Component description() {
        return Component.translatable("xpjetpack.feature.fireproof.desc");
    }

    @Override
    public void declareConfig(ConfigFacade cfg) {
        cfg.bool("fireproof.enabled", "是否启用「防火」功能", true);
        cfg.dbl("fireproof.costPerSecond", "处于火焰/岩浆中每秒消耗的经验点", 3.0D, 0.0D, 200.0D);
        cfg.bool("fireproof.immunityLava", "是否免疫岩浆伤害", true);
        cfg.bool("fireproof.immunityBurn", "是否免疫着火伤害", true);
        cfg.bool("fireproof.extinguish", "着火时立即灭火", true);
    }

    @Override
    public void onServerTick(FeatureContext ctx) {
        ServerPlayer player = ctx.player();
        FeatureState st = ctx.featureState(this);

        boolean lava = player.isInLava();
        boolean burning = player.isOnFire();
        boolean lavaOk = FeatureConfig.bool("fireproof.immunityLava", true);
        boolean burnOk = FeatureConfig.bool("fireproof.immunityBurn", true);

        boolean need = (lava && lavaOk) || (burning && burnOk);
        if (!need) {
            st.exhaustedNotified = false;
            st.accumulator = 0.0D;
            return;
        }

        double perSecond = FeatureConfig.dbl("fireproof.costPerSecond", 3.0D);
        if (!XpCost.payPerSecond(player, st, perSecond)) {
            FeatureNotifier.notifyExhausted(player, st, "防火");
            return;
        }

        st.exhaustedNotified = false;
        player.addEffect(new MobEffectInstance(
                MobEffects.FIRE_RESISTANCE, REFRESH_TICKS, 0, false, false, false));

        if (FeatureConfig.bool("fireproof.extinguish", true) && burning && burnOk) {
            player.clearFire();
        }
        ctx.touch();
    }

    @Override
    public Component statusLine(FeatureContext ctx) {
        FeatureState st = ctx.featureState(this);
        ServerPlayer player = ctx.player();
        String state = st.enabled ? "开" : "关";
        String now = (player.isInLava() || player.isOnFire()) ? "生效中" : "待机";
        return Component.literal(String.format("防火：%s · %.1f 点/秒 · %s",
                state, FeatureConfig.dbl("fireproof.costPerSecond", 3.0D), now));
    }
}
