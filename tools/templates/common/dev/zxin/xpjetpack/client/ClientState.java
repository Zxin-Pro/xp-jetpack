package dev.zxin.xpjetpack.client;

import dev.zxin.xpjetpack.config.JetpackConfig;
import dev.zxin.xpjetpack.network.StatePacket;

/** 客户端镜像状态（服务端同步来的权威值 + 本地预测值）。 */
public final class ClientState {

    public static final ClientState INSTANCE = new ClientState();

    public boolean enabled;
    /** 当前经验点剩余比例。 */
    public double burn = 1.0D;
    /** 玩家可用经验点（服务端同步）。 */
    public int xpPoints;
    public double ticksPerPoint = 50.0D;
    public double elytraMultiplier = 2.0D;
    public double verticalAcceleration = 0.08D;
    public double verticalMaxSpeed = 0.5D;
    public double elytraThrustPerTick = 0.015D;
    public double elytraMaxSpeed = 1.5D;
    public double soundVolume = 0.6D;
    public boolean elytraOnly;
    public boolean particles = true;
    public boolean sounds = true;

    private ClientState() {
    }

    /** 本地是否还有燃料（预测用）。 */
    public boolean hasFuel() {
        return xpPoints > 0 || burn > 0.0D;
    }

    public void apply(StatePacket packet) {
        if (packet == null) {
            return;
        }
        this.burn = packet.burn;
        this.xpPoints = packet.xpPoints;
        this.ticksPerPoint = packet.ticksPerPoint;
        this.elytraMultiplier = packet.elytraMultiplier;
        this.verticalAcceleration = packet.verticalAcceleration;
        this.verticalMaxSpeed = packet.verticalMaxSpeed;
        this.elytraThrustPerTick = packet.elytraThrustPerTick;
        this.elytraMaxSpeed = packet.elytraMaxSpeed;
        this.soundVolume = packet.soundVolume;
        this.enabled = packet.enabled;
        this.elytraOnly = packet.elytraOnly;
        this.particles = packet.particles;
        this.sounds = packet.sounds;
    }

    /** 把同步来的参数装进一份配置，供物理函数使用。 */
    public JetpackConfig config() {
        JetpackConfig cfg = JetpackConfig.defaults();
        cfg.xpTicksPerPoint = ticksPerPoint;
        cfg.xpElytraMultiplier = elytraMultiplier;
        cfg.verticalAcceleration = verticalAcceleration;
        cfg.verticalMaxSpeed = verticalMaxSpeed;
        cfg.elytraThrustPerTick = elytraThrustPerTick;
        cfg.elytraMaxSpeed = elytraMaxSpeed;
        cfg.soundVolume = soundVolume;
        cfg.elytraOnly = elytraOnly;
        cfg.particles = particles;
        cfg.sounds = sounds;
        return cfg;
    }

    public void reset() {
        enabled = false;
        burn = 1.0D;
        xpPoints = 0;
    }
}
