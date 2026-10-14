package dev.zxin.xpjetpack.physics;

import dev.zxin.xpjetpack.config.JetpackConfig;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * 喷气物理。客户端（预测）与服务端（权威）调用同一份实现。
 * 逻辑参考 Create Jetpack / FlightLib 的手感（加速度 + 速度上限 + 松手保留惯性），
 * 代码为独立实现，未复制其源码。
 */
public final class JetpackPhysics {

    private JetpackPhysics() {
    }

    /** 直立模式：持续向上加速，夹在上限内；超速时按阻尼回落。 */
    public static void applyUprightThrust(Player player, JetpackConfig cfg) {
        Vec3 motion = player.getDeltaMovement();
        double y = motion.y;

        if (y > cfg.verticalMaxSpeed) {
            y *= cfg.verticalDamping;
        }
        if (y < cfg.verticalMaxSpeed) {
            y = Math.min(y + cfg.verticalAcceleration, cfg.verticalMaxSpeed);
        }
        player.setDeltaMovement(motion.x, y, motion.z);
    }

    /** 鞘翅模式：沿视线方向施加推力，并按硬上限夹紧总速度。 */
    public static void applyElytraThrust(Player player, JetpackConfig cfg) {
        Vec3 look = player.getLookAngle();
        Vec3 motion = player.getDeltaMovement().add(look.scale(cfg.elytraThrustPerTick));

        double speed = motion.length();
        if (speed > cfg.elytraMaxSpeed && speed > 1.0E-6D) {
            motion = motion.scale(cfg.elytraMaxSpeed / speed);
        }
        player.setDeltaMovement(motion);
    }

    /** 直立模式服务端准入判定。 */
    public static boolean canThrustUpright(Player player, JetpackConfig cfg) {
        if (cfg.elytraOnly) {
            return false;
        }
        if (player.isFallFlying()) {
            return false;
        }
        return !player.onGround();
    }

    /** 鞘翅模式服务端准入判定。 */
    public static boolean canThrustElytra(Player player, JetpackConfig cfg) {
        if (cfg.elytraRequiresFallFlying) {
            return player.isFallFlying();
        }
        return true;
    }
}
