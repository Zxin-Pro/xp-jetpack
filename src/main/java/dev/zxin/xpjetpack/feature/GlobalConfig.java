package dev.zxin.xpjetpack.feature;

/**
 * 全局配置键声明（{@code global.*}）。
 *
 * <p>这些键服务于整个生存套件，与喷气自身的配置<b>互不影响</b>：
 * 喷气读 {@code JetpackConfig}，本套件读 {@link FeatureConfig}。
 */
public final class GlobalConfig {

    /** 每次伤害抵挡消耗的经验点（护盾默认值）。 */
    public static final String XP_PER_DAMAGE = "global.experiencePerDamage";
    /** 每秒效果消耗的经验点（呼吸/防火默认值）。 */
    public static final String XP_PER_SECOND = "global.experiencePerSecond";
    /** 创造模式是否免消耗。 */
    public static final String CREATIVE_FREE = "global.creativeFree";
    /** 经验恢复后是否自动重新启用功能。 */
    public static final String AUTO_REENABLE = "global.autoReenable";
    /** 玩家手动关闭后，是否拒绝自动重开。 */
    public static final String RESPECT_MANUAL_TOGGLE = "global.respectManualToggle";
    /** 是否发送聊天栏提示。 */
    public static final String CHAT_NOTIFY = "global.chatNotify";

    private GlobalConfig() {
    }

    /** 由加载器在声明阶段调用。 */
    public static void declare(ConfigFacade cfg) {
        cfg.dbl(XP_PER_DAMAGE, "每次伤害抵挡消耗的经验点（护盾默认）", 4.0D, 0.0D, 500.0D);
        cfg.dbl(XP_PER_SECOND, "每秒持续效果消耗的经验点（呼吸/防火默认）", 2.0D, 0.0D, 500.0D);
        cfg.bool(CREATIVE_FREE, "创造模式不消耗经验", true);
        cfg.bool(AUTO_REENABLE, "经验恢复后自动重新启用功能", true);
        cfg.bool(RESPECT_MANUAL_TOGGLE, "玩家手动关闭的功能不自动重开", true);
        cfg.bool(CHAT_NOTIFY, "是否在聊天栏发送提示", true);
    }

    // ------------------------------------------------------------------
    // 读取便捷方法
    // ------------------------------------------------------------------

    public static double xpPerDamage() {
        return FeatureConfig.dbl(XP_PER_DAMAGE, 4.0D);
    }

    public static double xpPerSecond() {
        return FeatureConfig.dbl(XP_PER_SECOND, 2.0D);
    }

    public static boolean creativeFree() {
        return FeatureConfig.bool(CREATIVE_FREE, true);
    }

    public static boolean autoReenable() {
        return FeatureConfig.bool(AUTO_REENABLE, true);
    }

    public static boolean respectManualToggle() {
        return FeatureConfig.bool(RESPECT_MANUAL_TOGGLE, true);
    }

    public static boolean chatNotify() {
        return FeatureConfig.bool(CHAT_NOTIFY, true);
    }
}
