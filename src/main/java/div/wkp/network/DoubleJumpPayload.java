package div.wkp.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record DoubleJumpPayload(boolean useTempJump) implements CustomPayload {
    public static final CustomPayload.Id<DoubleJumpPayload> ID =
            new CustomPayload.Id<>(Identifier.of("wkperks", "double_jump"));

    public static final PacketCodec<RegistryByteBuf, DoubleJumpPayload> CODEC =
            new PacketCodec<>() {
                @Override
                public void encode(RegistryByteBuf buf, DoubleJumpPayload payload) {
                    buf.writeBoolean(payload.useTempJump());
                }

                @Override
                public DoubleJumpPayload decode(RegistryByteBuf buf) {
                    return new DoubleJumpPayload(buf.readBoolean());
                }
            };

    @Override
    public Id<? extends CustomPayload> getId() { return ID; }
}