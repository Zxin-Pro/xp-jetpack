package dev.zxin.xpjetpack.logic;

import net.minecraft.server.level.ServerPlayer;

import java.lang.reflect.Field;

/**
 * 清零原版「悬浮太久（Floating is not enabled on this server）」计数器。
 *
 * 用反射而不是 Mixin / AccessTransformer / AccessWidener：
 *  · 22 个版本 × 3 个加载器的构建配置差异太大，AT/AW 任一处失效就直接编译失败；
 *  · 反射在任何加载器、任何版本上都能编译，且不会因为目标字段被官方改名而在加载期崩溃。
 *
 * 字段名候选覆盖：Mojang 官方名（开发环境 / NeoForge / Fabric 运行时）与 Forge 生产环境的 SRG 名。
 */
public final class AntiKick {

    private static Field field;
    private static boolean resolved;
    private static boolean unavailable;

    private AntiKick() {
    }

    public static void reset(ServerPlayer player) {
        if (unavailable || player == null) {
            return;
        }
        try {
            Field target = resolve(player);
            if (target != null) {
                target.setInt(player.connection, 0);
            }
        } catch (Throwable t) {
            unavailable = true;
        }
    }

    private static Field resolve(ServerPlayer player) {
        if (resolved) {
            return field;
        }
        resolved = true;
        String[] candidates = {"aboveGroundTickCount", "f_9788_", "field_9894_"};
        Class<?> type = player.connection.getClass();
        while (type != null && field == null) {
            for (String name : candidates) {
                try {
                    Field candidate = type.getDeclaredField(name);
                    candidate.setAccessible(true);
                    field = candidate;
                    break;
                } catch (Throwable ignored) {
                    // 换下一个候选名
                }
            }
            type = type.getSuperclass();
        }
        return field;
    }
}
