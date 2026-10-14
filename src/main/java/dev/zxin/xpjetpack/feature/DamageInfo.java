package dev.zxin.xpjetpack.feature;

/**
 * 伤害信息：加载器事件 → 通用结构。
 * 功能可改 {@link #amount}（0 表示完全抵挡）或 {@link #cancelled}。
 */
public final class DamageInfo {

    /** 原始伤害值。 */
    public double amount;
    /** 伤害来源标识（如 "lava" / "inFire" / "fall" / 类名），仅用于展示与配置。 */
    public String sourceId;
    /** 是否为火焰/岩浆类伤害。 */
    public boolean fire;
    /** 是否已取消。 */
    public boolean cancelled;

    /**
     * 伤害是否已经生效后才回调（Fabric 侧只能事后回补）。
     * false = 还能改 {@link #amount}（Forge/NeoForge 的伤害前事件）；
     * true  = 伤害已扣除，功能应按最终伤害自行回补。
     */
    public boolean postApplied;

    public DamageInfo(double amount, String sourceId, boolean fire) {
        this.amount = amount;
        this.sourceId = sourceId == null ? "" : sourceId;
        this.fire = fire;
    }
}
