package dev.zxin.xpjetpack.util;

import dev.zxin.xpjetpack.config.JetpackConfig;
import net.minecraft.world.entity.player.Player;

/**
 * 经验读写。只使用跨版本稳定的字段与方法：
 * {@code totalExperience} 是玩家总经验点，{@code giveExperiencePoints} 正负皆可。
 */
public final class XpUtil {

    private XpUtil() {
    }

    /** 玩家当前可用经验点。 */
    public static int available(Player player) {
        return Math.max(0, player.totalExperience);
    }

    /** 是否还有燃料。 */
    public static boolean hasFuel(Player player, JetpackConfig cfg) {
        if (available(player) < 1) {
            return false;
        }
        if (cfg.xpRequireLevel && player.experienceLevel <= 0 && player.totalExperience < 1) {
            return false;
        }
        return true;
    }

    /** 消耗经验点，返回实际消耗数量。 */
    public static int consume(Player player, int amount) {
        if (amount <= 0) {
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

    /** 把 tick 数换算成可读时长（秒）。 */
    public static double seconds(int ticks) {
        return ticks / 20.0D;
    }
}
