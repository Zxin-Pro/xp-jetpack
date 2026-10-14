package dev.zxin.xpjetpack.util;

import dev.zxin.xpjetpack.feature.FeatureConfig;
import dev.zxin.xpjetpack.feature.FeatureState;
import dev.zxin.xpjetpack.feature.GlobalConfig;
import net.minecraft.world.entity.player.Player;

/**
 * ★ 统一经验消耗接口 ★ —— 喷气与全部生存功能共用。
 *
 * <p>约定：
 * <ul>
 *   <li>不足即停：付不起直接返回 false，且不扣任何经验</li>
 *   <li>只扣经验点（{@code totalExperience}），不碰经验等级</li>
 *   <li>小数累积：秒级扣费先攒零头（{@code st.accumulator}），凑满 1 点才真正扣，不丢精度</li>
 *   <li>创造模式免责由 {@code global.creativeFree} 控制</li>
 * </ul>
 *
 * <p>注意：喷气走自己的 {@code JetpackConfig.creativeInfinite}/{@code burnPerTick}，
 * <b>不经过本类</b>，因此喷气行为完全不受本类影响。
 */
public final class XpCost {

    private XpCost() {
    }

    /** 玩家当前可用经验点。 */
    public static int available(Player player) {
        if (player == null) {
            return 0;
        }
        return Math.max(0, player.totalExperience);
    }

    /** 创造模式是否免责。 */
    public static boolean isFree(Player player) {
        return player != null && player.isCreative() && GlobalConfig.creativeFree();
    }

    /** 是否付得起。 */
    public static boolean canAfford(Player player, double amount) {
        if (isFree(player)) {
            return true;
        }
        return amount <= 0.0D || available(player) >= 1;
    }

    /**
     * 按「点」付费（护盾等即时消费）。
     * 不足返回 false 且不扣任何经验。
     */
    public static boolean pay(Player player, FeatureState st, double amount) {
        if (amount <= 0.0D) {
            return true;
        }
        if (isFree(player)) {
            return true;
        }
        if (st == null) {
            return strip(player, (int) Math.ceil(amount)) > 0;
        }
        st.accumulator += amount;
        if (st.accumulator < 1.0D) {
            // 零头不足 1 点，本次不扣，视为成功
            return true;
        }
        int points = (int) Math.floor(st.accumulator);
        if (strip(player, points) > 0) {
            st.accumulator -= points;
            return true;
        }
        st.accumulator = 0.0D;
        return false;
    }

    /**
     * 按「点/秒」持续付费（呼吸/防火等），内部按 20 tick = 1 秒折算。
     *
     * @param perSecond 每秒消耗的经验点
     * @return false = 经验不足，调用方应停用效果并提示
     */
    public static boolean payPerSecond(Player player, FeatureState st, double perSecond) {
        if (perSecond <= 0.0D) {
            return true;
        }
        if (isFree(player)) {
            return true;
        }
        if (st == null) {
            return available(player) >= 1;
        }
        st.accumulator += perSecond / 20.0D;
        if (st.accumulator < 1.0D) {
            return true;
        }
        int points = (int) Math.floor(st.accumulator);
        if (strip(player, points) > 0) {
            st.accumulator -= points;
            return true;
        }
        st.accumulator = 0.0D;
        return false;
    }

    /**
     * 直接扣点（喷气专用：保留「起喷先付 1 点」的行为）。
     *
     * @return 实际扣掉的点数
     */
    public static int consume(Player player, int amount) {
        return strip(player, amount);
    }

    private static int strip(Player player, int amount) {
        if (player == null || amount <= 0) {
            return 0;
        }
        int have = available(player);
        int take = Math.min(have, amount);
        if (take <= 0) {
            return 0;
        }
        player.giveExperiencePoints(-take);
        return take;
    }

    /** 当前配置下，剩余经验还能支撑多少秒的 perSecond 消耗。 */
    public static double remainingSeconds(Player player, double perSecond) {
        if (perSecond <= 0.0D || isFree(player)) {
            return Double.POSITIVE_INFINITY;
        }
        return available(player) / perSecond;
    }

    /** tick 数 → 秒。 */
    public static double seconds(int ticks) {
        return ticks / 20.0D;
    }

    /** 配置层是否已声明全局键（用于降级判断）。 */
    public static boolean hasGlobal(String path) {
        return FeatureConfig.has(path);
    }
}
