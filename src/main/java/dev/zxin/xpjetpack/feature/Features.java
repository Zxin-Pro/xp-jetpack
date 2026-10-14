package dev.zxin.xpjetpack.feature;

import dev.zxin.xpjetpack.feature.impl.AutoSmeltFeature;
import dev.zxin.xpjetpack.feature.impl.FireproofFeature;
import dev.zxin.xpjetpack.feature.impl.RespirationFeature;
import dev.zxin.xpjetpack.feature.impl.ShieldFeature;

/**
 * ★ 唯一的功能注册点 ★
 *
 * <p>新增功能：实现 {@link Feature} → 在下面加一行 {@code register(new XxxFeature())}。
 * 其余一切（配置/指令/事件/状态/语言）自动接入。
 */
public final class Features {

    private Features() {
    }

    /** 注册全部功能。由 {@link FeatureRegistry#ensureBootstrapped()} 调用。 */
    public static void bootstrap() {
        FeatureRegistry.register(new RespirationFeature());
        FeatureRegistry.register(new FireproofFeature());
        FeatureRegistry.register(new ShieldFeature());
        FeatureRegistry.register(new AutoSmeltFeature());
        // ← 未来新增功能在这里加一行即可
    }
}
