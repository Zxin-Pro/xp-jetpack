package dev.zxin.xpjetpack.network;

import dev.zxin.xpjetpack.config.JetpackConfig;
import net.minecraft.network.FriendlyByteBuf;

/** S2C：服务端权威状态 + 客户端预测所需的物理参数。 */
public final class StatePacket {

    /** 当前经验点剩余比例（1.0 → 0.0）。 */
    public final double burn;
    /** 玩家可用经验点总数。 */
    public final int xpPoints;
    /** 1 点经验可喷气 tick 数（直立）。 */
    public final double ticksPerPoint;
    /** 鞘翅消耗倍率。 */
    public final double elytraMultiplier;
    public final double verticalAcceleration;
    public final double verticalMaxSpeed;
    public final double elytraThrustPerTick;
    public final double elytraMaxSpeed;
    public final double soundVolume;
    public final boolean enabled;
    public final boolean elytraOnly;
    public final boolean particles;
    public final boolean sounds;

    public StatePacket(double burn, int xpPoints, double ticksPerPoint, double elytraMultiplier,
                       double verticalAcceleration, double verticalMaxSpeed,
                       double elytraThrustPerTick, double elytraMaxSpeed, double soundVolume,
                       boolean enabled, boolean elytraOnly, boolean particles, boolean sounds) {
        this.burn = burn;
        this.xpPoints = xpPoints;
        this.ticksPerPoint = ticksPerPoint;
        this.elytraMultiplier = elytraMultiplier;
        this.verticalAcceleration = verticalAcceleration;
        this.verticalMaxSpeed = verticalMaxSpeed;
        this.elytraThrustPerTick = elytraThrustPerTick;
        this.elytraMaxSpeed = elytraMaxSpeed;
        this.soundVolume = soundVolume;
        this.enabled = enabled;
        this.elytraOnly = elytraOnly;
        this.particles = particles;
        this.sounds = sounds;
    }

    public static StatePacket of(double burn, int xpPoints, boolean enabled, JetpackConfig cfg) {
        return new StatePacket(burn, xpPoints, cfg.xpTicksPerPoint, cfg.xpElytraMultiplier,
                cfg.verticalAcceleration, cfg.verticalMaxSpeed, cfg.elytraThrustPerTick,
                cfg.elytraMaxSpeed, cfg.soundVolume, enabled, cfg.elytraOnly,
                cfg.particles, cfg.sounds);
    }

    public static void encode(StatePacket message, FriendlyByteBuf buf) {
        buf.writeDouble(message.burn);
        buf.writeVarInt(message.xpPoints);
        buf.writeDouble(message.ticksPerPoint);
        buf.writeDouble(message.elytraMultiplier);
        buf.writeDouble(message.verticalAcceleration);
        buf.writeDouble(message.verticalMaxSpeed);
        buf.writeDouble(message.elytraThrustPerTick);
        buf.writeDouble(message.elytraMaxSpeed);
        buf.writeDouble(message.soundVolume);
        buf.writeBoolean(message.enabled);
        buf.writeBoolean(message.elytraOnly);
        buf.writeBoolean(message.particles);
        buf.writeBoolean(message.sounds);
    }

    public static StatePacket decode(FriendlyByteBuf buf) {
        return new StatePacket(
                buf.readDouble(), buf.readVarInt(), buf.readDouble(), buf.readDouble(),
                buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readDouble(),
                buf.readDouble(), buf.readBoolean(), buf.readBoolean(),
                buf.readBoolean(), buf.readBoolean());
    }
}
