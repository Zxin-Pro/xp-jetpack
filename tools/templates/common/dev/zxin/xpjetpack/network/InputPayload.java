//@only MC>=12005
package dev.zxin.xpjetpack.network;

import dev.zxin.xpjetpack.XpJetpack;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** 1.20.5+ 原版自定义载荷包装。 */
public record InputPayload(boolean thrusting) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<InputPayload> TYPE =
            new CustomPacketPayload.Type<InputPayload>(XpJetpack.id("input"));

    public static final StreamCodec<RegistryFriendlyByteBuf, InputPayload> STREAM_CODEC = StreamCodec.of(
            (RegistryFriendlyByteBuf buf, InputPayload payload) -> buf.writeBoolean(payload.thrusting()),
            (RegistryFriendlyByteBuf buf) -> new InputPayload(buf.readBoolean()));

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
