package dev.zxin.xpjetpack.feature;

import dev.zxin.xpjetpack.player.JetState;

import java.util.HashMap;
import java.util.Map;

/**
 * 单个玩家 × 单个功能的运行状态。随 {@link JetState} 一起落盘。
 */
public final class FeatureState {

    /** 当前是否生效。 */
    public boolean enabled = true;

    /**
     * 玩家是否手动关过。
     * 手动关闭后，即使经验恢复也不自动重开（见 global.respectManualToggle）。
     */
    public boolean manualDisabled;

    /** 缺料提示去重，避免刷屏。 */
    public boolean exhaustedNotified;

    /** 秒级扣费的零头（不足 1 点经验的累积量）。 */
    public double accumulator;

    /** 护盾：合并提示用（不落盘）。 */
    public transient double pendingDamage;
    public transient double pendingCost;
    public transient long lastNotifyTick;

    /** 熔炉：避免同一方块被重复处理（不落盘）。 */
    public transient long lastBlockTick = -1L;

    // 同步去重（保留位，供未来扩展）
    public transient boolean lastSynced;

    public FeatureState() {
    }

    /** 读盘后的自愈。 */
    public void clampSelf() {
        if (Double.isNaN(accumulator) || Double.isInfinite(accumulator) || accumulator < 0.0D) {
            accumulator = 0.0D;
        }
        if (accumulator > 64.0D) {
            accumulator = 64.0D;
        }
    }

    /** 由功能重置为「启用」。 */
    public void enable() {
        enabled = true;
        manualDisabled = false;
        exhaustedNotified = false;
    }

    /** 玩家手动关闭。 */
    public void disableManually() {
        enabled = false;
        manualDisabled = true;
        exhaustedNotified = false;
        accumulator = 0.0D;
    }

    /** 因缺料自动停用（不算手动关闭，经验恢复后可在 autoReenable 时重开）。 */
    public void disableByExhaustion() {
        enabled = false;
        accumulator = 0.0D;
    }

    /** 帧内可变状态复位。 */
    public void resetTransient() {
        pendingDamage = 0.0D;
        pendingCost = 0.0D;
    }

    /** JetState 里存放的容器类型别名。 */
    public static Map<String, FeatureState> newMap() {
        return new HashMap<String, FeatureState>();
    }
}
