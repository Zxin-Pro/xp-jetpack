package dev.zxin.xpjetpack.config;

/**
 * 模组配置（纯 POJO，各加载器负责把平台的配置系统映射到这里）。
 *
 * <p>核心模型：<b>喷气直接消耗玩家经验点</b>，没有中间资源。
 * 每烧掉 1 点经验可以喷 {@link #xpTicksPerPoint} 个 tick；鞘翅模式按
 * {@link #xpElytraMultiplier} 倍速率消耗。
 */
public final class JetpackConfig {

    // ---- 经验消耗 ----
    /** 直立模式：1 点经验可喷气多少 tick。 */
    public double xpTicksPerPoint = 50.0D;
    /** 鞘翅模式消耗倍率（2.0 = 每 25 tick 烧 1 点）。 */
    public double xpElytraMultiplier = 2.0D;
    /** 是否要求经验等级大于 0 才能起飞。 */
    public boolean xpRequireLevel = false;

    // ---- 喷气行为 ----
    /** /fly 需要的权限等级。 */
    public int commandPermissionLevel = 0;
    /** 只能鞘翅模式使用。 */
    public boolean elytraOnly = false;
    /** 创造模式不消耗经验。 */
    public boolean creativeInfinite = true;
    /** 喷气时清除摔落伤害。 */
    public boolean preventFallDamage = true;
    /** 规避原版「悬浮太久」踢人。 */
    public boolean preventFlyingKick = true;

    // ---- 推力 ----
    public double verticalAcceleration = 0.08D;
    public double verticalMaxSpeed = 0.5D;
    public double verticalDamping = 0.9D;
    public double elytraThrustPerTick = 0.015D;
    public double elytraMaxSpeed = 1.5D;
    public boolean elytraRequiresFallFlying = true;

    // ---- 输入 / 网络 ----
    /** 输入包失联判定阈值（tick）。 */
    public int inputTimeoutTicks = 20;
    /** 状态同步周期（tick）。 */
    public int syncIntervalTicks = 40;

    // ---- 特效 ----
    public boolean particles = true;
    public boolean sounds = true;
    public double soundVolume = 0.6D;

    // ---- HUD ----
    public boolean hudEnabled = true;
    public boolean hudAlwaysVisible = false;
    public int hudOffsetY = 45;

    public static JetpackConfig defaults() {
        return new JetpackConfig();
    }

    public void clamp() {
        if (xpTicksPerPoint < 1.0D) {
            xpTicksPerPoint = 1.0D;
        }
        if (xpTicksPerPoint > 100000.0D) {
            xpTicksPerPoint = 100000.0D;
        }
        if (xpElytraMultiplier < 0.01D) {
            xpElytraMultiplier = 0.01D;
        }
        if (xpElytraMultiplier > 100.0D) {
            xpElytraMultiplier = 100.0D;
        }
        if (commandPermissionLevel < 0) {
            commandPermissionLevel = 0;
        }
        if (commandPermissionLevel > 4) {
            commandPermissionLevel = 4;
        }
        if (verticalAcceleration < 0.0D) {
            verticalAcceleration = 0.0D;
        }
        if (verticalMaxSpeed < 0.0D) {
            verticalMaxSpeed = 0.0D;
        }
        if (verticalDamping < 0.0D) {
            verticalDamping = 0.0D;
        }
        if (verticalDamping > 1.0D) {
            verticalDamping = 1.0D;
        }
        if (elytraThrustPerTick < 0.0D) {
            elytraThrustPerTick = 0.0D;
        }
        if (elytraMaxSpeed < 0.0D) {
            elytraMaxSpeed = 0.0D;
        }
        if (inputTimeoutTicks < 1) {
            inputTimeoutTicks = 1;
        }
        if (inputTimeoutTicks > 200) {
            inputTimeoutTicks = 200;
        }
        if (syncIntervalTicks < 0) {
            syncIntervalTicks = 0;
        }
        if (syncIntervalTicks > 200) {
            syncIntervalTicks = 200;
        }
        if (soundVolume < 0.0D) {
            soundVolume = 0.0D;
        }
        if (soundVolume > 1.0D) {
            soundVolume = 1.0D;
        }
        if (hudOffsetY < 10) {
            hudOffsetY = 10;
        }
        if (hudOffsetY > 400) {
            hudOffsetY = 400;
        }
    }

    public JetpackConfig copy() {
        JetpackConfig c = new JetpackConfig();
        c.xpTicksPerPoint = xpTicksPerPoint;
        c.xpElytraMultiplier = xpElytraMultiplier;
        c.xpRequireLevel = xpRequireLevel;
        c.commandPermissionLevel = commandPermissionLevel;
        c.elytraOnly = elytraOnly;
        c.creativeInfinite = creativeInfinite;
        c.preventFallDamage = preventFallDamage;
        c.preventFlyingKick = preventFlyingKick;
        c.verticalAcceleration = verticalAcceleration;
        c.verticalMaxSpeed = verticalMaxSpeed;
        c.verticalDamping = verticalDamping;
        c.elytraThrustPerTick = elytraThrustPerTick;
        c.elytraMaxSpeed = elytraMaxSpeed;
        c.elytraRequiresFallFlying = elytraRequiresFallFlying;
        c.inputTimeoutTicks = inputTimeoutTicks;
        c.syncIntervalTicks = syncIntervalTicks;
        c.particles = particles;
        c.sounds = sounds;
        c.soundVolume = soundVolume;
        c.hudEnabled = hudEnabled;
        c.hudAlwaysVisible = hudAlwaysVisible;
        c.hudOffsetY = hudOffsetY;
        return c;
    }

    /** 单 tick 消耗的经验点比例（0..1）。 */
    public double burnPerTick(boolean elytra) {
        double perPoint = Math.max(1.0D, xpTicksPerPoint);
        double rate = 1.0D / perPoint;
        if (elytra) {
            rate *= Math.max(0.01D, xpElytraMultiplier);
        }
        return rate;
    }
}
