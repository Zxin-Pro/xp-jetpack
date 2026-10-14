package dev.zxin.xpjetpack.network;

import dev.zxin.xpjetpack.XpJetpack;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** 1.20.5+ 原版自定义载荷包装。 */
public record StatePayload(double burn, int xpPoints, double ticksPerPoint, double elytraMultiplier,
                           double verticalAcceleration, double verticalMaxSpeed,
                           double elytraThrustPerTick, double elytraMaxSpeed, double soundVolume,
                           boolean enabled, boolean elytraOnly, boolean particles, boolean sounds)
        implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<StatePayload> TYPE =
            new CustomPacketPayload.Type<StatePayload>(XpJetpack.id("state"));

    public static final StreamCodec<RegistryFriendlyByteBuf, StatePayload> STREAM_CODEC = StreamCodec.of(
            (RegistryFriendlyByteBuf buf, StatePayload payload) -> StatePacket.encode(payload.toPacket(), buf),
            (RegistryFriendlyByteBuf buf) -> fromPacket(StatePacket.decode(buf)));

    public static StatePayload fromPacket(StatePacket packet) {
        return new StatePayload(packet.burn, packet.xpPoints, packet.ticksPerPoint,
                packet.elytraMultiplier, packet.verticalAcceleration, packet.verticalMaxSpeed,
                packet.elytraThrustPerTick, packet.elytraMaxSpeed, packet.soundVolume,
                packet.enabled, packet.elytraOnly, packet.particles, packet.sounds);
    }

    public StatePacket toPacket() {
        return new StatePacket(burn, xpPoints, ticksPerPoint, elytraMultiplier,
                verticalAcceleration, verticalMaxSpeed, elytraThrustPerTick, elytraMaxSpeed,
                soundVolume, enabled, elytraOnly, particles, sounds);
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
