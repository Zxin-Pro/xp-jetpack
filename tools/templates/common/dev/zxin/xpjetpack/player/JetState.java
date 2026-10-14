package dev.zxin.xpjetpack.player;

import dev.zxin.xpjetpack.feature.FeatureState;
import dev.zxin.xpjetpack.feature.FeatureStates;

import java.util.Map;

/** 单个玩家的喷气状态。服务端权威，客户端只持镜像。 */
public final class JetState {

    /** 喷气开关（/fly 控制）。 */
    public boolean enabled;
    /** 服务端视角：最近一次收到的输入。 */
    public boolean thrusting;
    /**
     * 当前这颗经验点还剩多少（1.0 → 0.0）。
     * 归零时扣 1 点经验并重置为 1.0；没经验可用时归零并停喷。
     */
    public double burn = 1.0D;
    /** 玩家当前可用经验点（服务端同步给客户端用于显示与预测）。 */
    public int xpPoints;
    /** 距上次收到输入包过了多少 tick。 */
    public int inputTicks = 1000;
    /** 是否已经提示过“经验耗尽”。 */
    public boolean exhaustedNotified;

    /**
     * 生存套件各功能的玩家状态：功能 id → 状态。
     * 随本对象一起落盘；旧存档读到 null 时按功能默认值惰性创建。
     */
    public Map<String, FeatureState> features = FeatureState.newMap();

    /** 上一 tick 是否在燃烧经验（用于判断“刚起喷”要立刻扣 1 点）。 */
    public transient boolean burning;

    // 同步去重用（不落盘）
    public transient double lastSyncedBurn = -1.0D;
    public transient int lastSyncedXp = -1;
    public transient boolean lastSyncedEnabled = false;
    public transient boolean lastSyncedThrust = false;

    public JetState() {
    }

    /** 读盘后的自愈。 */
    public void clampSelf() {
        if (Double.isNaN(burn) || Double.isInfinite(burn) || burn < 0.0D) {
            burn = 0.0D;
        }
        if (burn > 1.0D) {
            burn = 1.0D;
        }
        if (xpPoints < 0) {
            xpPoints = 0;
        }
        if (inputTicks < 0) {
            inputTicks = 1000;
        }
        burning = false;
        FeatureStates.clampAll(this);
    }

    public void setEnabled(boolean value) {
        enabled = value;
        if (!value) {
            thrusting = false;
            burning = false;
            inputTicks = 1000;
        }
    }

    /** 起喷时立刻付掉 1 点经验的“预付款”，返回是否成功。 */
    public boolean startBurn() {
        burn = 1.0D;
        burning = true;
        return true;
    }
}
