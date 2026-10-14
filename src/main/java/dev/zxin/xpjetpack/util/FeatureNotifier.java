package dev.zxin.xpjetpack.util;

import dev.zxin.xpjetpack.Bridge;
import dev.zxin.xpjetpack.feature.FeatureState;
import dev.zxin.xpjetpack.feature.GlobalConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * 聊天栏提示工具 —— 受 {@code global.chatNotify} 总开关约束。
 *
 * <p><b>本模组不添加任何音效</b>，所有反馈一律走聊天栏。
 */
public final class FeatureNotifier {

    /** 合并提示窗口（tick）。 */
    private static final long MERGE_WINDOW = 40L;

    private FeatureNotifier() {
    }

    private static boolean allowed() {
        return GlobalConfig.chatNotify();
    }

    /** 普通提示。 */
    public static void notify(ServerPlayer player, Component message) {
        if (player == null || !allowed()) {
            return;
        }
        player.sendSystemMessage(message);
    }

    /** 提示一次（同状态去重，避免刷屏）。 */
    public static void notifyOnce(ServerPlayer player, FeatureState st, String key, Component message) {
        if (player == null || st == null || st.exhaustedNotified) {
            return;
        }
        st.exhaustedNotified = true;
        notify(player, message);
    }

    /** 成功提示（绿色）。 */
    public static void ok(ServerPlayer player, String text) {
        notify(player, Component.literal(text).withStyle(ChatFormatting.GREEN));
    }

    /** 警告提示（黄色）。 */
    public static void warn(ServerPlayer player, String text) {
        notify(player, Component.literal(text).withStyle(ChatFormatting.YELLOW));
    }

    /** 经验不足提示（红色，去重）。 */
    public static void notifyExhausted(ServerPlayer player, FeatureState st, String featureName) {
        notifyOnce(player, st, "no_xp", Component.literal(
                featureName + "：经验不足，已自动停用").withStyle(ChatFormatting.RED));
    }

    /**
     * 经验护盾专用提示。
     *
     * @param mode every（逐条）/ merged（窗口合并）/ broken（仅缺料时提示）
     * @param text 形如「经验护盾抵挡了 3.0 点伤害，消耗 5 经验」
     */
    public static void notifyShield(ServerPlayer player, FeatureState st, String mode, String text) {
        if (player == null || st == null) {
            return;
        }
        if (!allowed()) {
            return;
        }
        if ("every".equalsIgnoreCase(mode)) {
            player.sendSystemMessage(Component.literal(text).withStyle(ChatFormatting.AQUA));
            return;
        }
        if ("broken".equalsIgnoreCase(mode)) {
            // 只在「付不起」时提示，由调用方走 notifyExhausted
            return;
        }
        // merged：窗口内累积，窗口结束时汇总
        st.pendingDamage += extractDamage(text);
        st.pendingCost += extractCost(text);
        flushShield(player, st, mode);
    }

    /** 合并窗口到期时输出一次汇总。每 tick 由分发器调用。 */
    public static void flushShield(ServerPlayer player, FeatureState st, String mode) {
        if (player == null || st == null || !"merged".equalsIgnoreCase(mode)) {
            return;
        }
        if (st.pendingDamage <= 0.0D) {
            return;
        }
        long now = player.level().getGameTime();
        if (st.lastNotifyTick >= 0L && now - st.lastNotifyTick < MERGE_WINDOW) {
            return;
        }
        String merged = String.format("经验护盾抵挡了 %.1f 点伤害，消耗 %.0f 经验",
                st.pendingDamage, st.pendingCost);
        notify(player, Component.literal(merged).withStyle(ChatFormatting.AQUA));
        st.pendingDamage = 0.0D;
        st.pendingCost = 0.0D;
        st.lastNotifyTick = now;
    }

    /** 重置合并窗口的计时基准（护盾首次触发时调用）。 */
    public static void markShieldTick(ServerPlayer player, FeatureState st) {
        if (player == null || st == null) {
            return;
        }
        if (st.lastNotifyTick < 0L) {
            st.lastNotifyTick = player.level().getGameTime();
        }
    }

    private static double extractDamage(String text) {
        return parseAfter(text, "抵挡了", "点伤害");
    }

    private static double extractCost(String text) {
        return parseAfter(text, "消耗", "经验");
    }

    private static double parseAfter(String text, String from, String to) {
        if (text == null) {
            return 0.0D;
        }
        int a = text.indexOf(from);
        if (a < 0) {
            return 0.0D;
        }
        int b = text.indexOf(to, a + from.length());
        if (b < 0) {
            return 0.0D;
        }
        try {
            return Double.parseDouble(text.substring(a + from.length(), b).trim());
        } catch (NumberFormatException ignored) {
            return 0.0D;
        }
    }
}
