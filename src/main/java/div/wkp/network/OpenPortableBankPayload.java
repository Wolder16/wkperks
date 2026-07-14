package div.wkp.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record OpenPortableBankPayload() implements CustomPayload {
    public static final CustomPayload.Id<OpenPortableBankPayload> ID =
            new CustomPayload.Id<>(Identifier.of("wkperks", "open_portable_bank"));

    public static final PacketCodec<RegistryByteBuf, OpenPortableBankPayload> CODEC =
            new PacketCodec<>() {
                @Override
                public void encode(
                        RegistryByteBuf buf,
                        OpenPortableBankPayload payload
                ) {
                }

                @Override
                public OpenPortableBankPayload decode(RegistryByteBuf buf) {
                    return new OpenPortableBankPayload();
                }
            };

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
