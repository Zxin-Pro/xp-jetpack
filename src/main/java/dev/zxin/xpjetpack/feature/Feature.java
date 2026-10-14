package dev.zxin.xpjetpack.feature;

import net.minecraft.network.chat.Component;

/**
 * 功能模块接口 —— <b>可扩展架构的核心</b>。
 *
 * <p>新增一个功能 = 实现本接口 + 在 {@link Features#bootstrap()} 里注册一行。
 * 配置、指令、事件分发、状态存储、语言文件全部自动接入，无需改动核心逻辑。
 *
 * <p>约束：实现类中<b>禁止</b>出现加载器专属 import（forge / neoforge / fabric）。
 */
public interface Feature {

    /** 唯一 id：小写英文，用作配置段名 / 独立指令名 / 语言键前缀 / 状态键。 */
    String id();

    /** 显示名（/xps help 与 status 使用）。 */
    Component displayName();

    /** 一句话说明。 */
    default Component description() {
        return Component.empty();
    }

    /** 独立指令根名；返回 null 表示不注册独立指令。 */
    default String commandName() {
        return id();
    }

    /** 默认是否开启（本套件约定：全部默认 true）。 */
    default boolean defaultEnabled() {
        return true;
    }

    /** 声明本功能的配置键 —— 自动注册进三套配置系统。 */
    default void declareConfig(ConfigFacade cfg) {
    }

    /** 每 tick 调用（功能自行判断前置条件）。 */
    default void onServerTick(FeatureContext ctx) {
    }

    /** 玩家受伤（可改 {@code info.amount} 或取消）。 */
    default void onDamage(FeatureContext ctx, DamageInfo info) {
    }

    /** 玩家挖掉方块。 */
    default void onBlockBreak(FeatureContext ctx, BlockBreakInfo info) {
    }

    /** /xps status 的一行简报。 */
    default Component statusLine(FeatureContext ctx) {
        FeatureState st = ctx.featureState(this);
        return Component.literal(id() + ": " + (st.enabled ? "开" : "关"));
    }

    /** 被启用时的回调（默认清掉缺料提示标记）。 */
    default void onEnable(FeatureContext ctx) {
        ctx.featureState(this).exhaustedNotified = false;
    }

    /** 被关闭时的回调。 */
    default void onDisable(FeatureContext ctx) {
        ctx.featureState(this).exhaustedNotified = false;
    }
}
