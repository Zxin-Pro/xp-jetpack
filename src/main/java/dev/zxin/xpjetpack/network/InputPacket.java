package dev.zxin.xpjetpack.network;

import net.minecraft.network.FriendlyByteBuf;

/** C2S：玩家是否正在按住喷气键。 */
public final class InputPacket {

    private final boolean thrusting;

    public InputPacket(boolean thrusting) {
        this.thrusting = thrusting;
    }

    public boolean thrusting() {
        return thrusting;
    }

    public static void encode(InputPacket message, FriendlyByteBuf buf) {
        buf.writeBoolean(message.thrusting);
    }

    public static InputPacket decode(FriendlyByteBuf buf) {
        return new InputPacket(buf.readBoolean());
    }
}
